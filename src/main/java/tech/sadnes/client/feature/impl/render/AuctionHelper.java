package tech.sadnes.client.feature.impl.render;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.CloseScreenEvent;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.HandledScreenRenderEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.SlotClickEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.auction.AuctionLoreUtil;
import tech.sadnes.client.server.auction.AuctionPriceParser;
import tech.sadnes.client.server.auction.SafeBuyLimitTracker;
import tech.sadnes.client.server.auction.SafeBuyLimitWidget;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;

@FeatureInfo(
    name = "AuctionHelper",
    description = "feature.impl.render.auctionhelper",
    displayName = "feature.impl.render.auctionhelper",
    category = Category.VISUALS,
    subcategory = Subcategory.HUD_HELPERS
)
public class AuctionHelper extends Feature {
    private static final Pattern AH_SEARCH_PATTERN = Pattern.compile("^(?:ah|auction)\\s+search\\s+(.+)$", 2);
    private static final Pattern AH_QUERY_PATTERN = Pattern.compile("^(?:ah|auction)\\s+(?!sell(?:\\s|$))(.+)$", 2);
    private static final Pattern AH_COMMAND_PATTERN = Pattern.compile("^(?:ah|auction)(?:\\s|$)", 2);
    private final AuctionPriceParser priceParser = new AuctionPriceParser();
    private Slot cheapestSlot;
    private Slot costEffectiveSlot;
    private final ColorSetting cheapestColor = new ColorSetting("auctionhelper.cheapestColor", -11796661);
    private final ColorSetting costEffectiveColor = new ColorSetting("auctionhelper.costEffectiveColor", -46261);
    private final KeybindSetting ahSearchBind = new KeybindSetting("auctionhelper.ahSearchBind");
    private final BooleanSetting safeBuy = new BooleanSetting("auctionhelper.safeBuy", false);
    private final NumberSetting safeBuyTolerance = new NumberSetting("auctionhelper.safeBuyTolerance", 4.0F, 0.0F, 15.0F, 1.0F, Unit.PERCENT)
        .setVisibleWhen(this.safeBuy::getValue);
    private final SafeBuyLimitTracker safeBuyTracker = new SafeBuyLimitTracker(() -> this.safeBuyTolerance.getValue().intValue());
    private final SafeBuyLimitWidget safeBuyOverlay = new SafeBuyLimitWidget(this.safeBuyTracker);
    private final BooleanSetting checkDurability = new BooleanSetting("auctionhelper.checkDurability", false);
    private final BooleanSetting checkEnchants = new BooleanSetting("auctionhelper.checkEnchants", false);
    private final BooleanSetting checkBalance = new BooleanSetting("auctionhelper.checkBalance", false);
    private final BooleanSetting checkFilters = new BooleanSetting("auctionhelper.checkFilters", false);
    private final MultiSetting armorChecks = new MultiSetting(
            "auctionhelper.armorChecks",
            BooleanSetting.of("protection", false),
            BooleanSetting.of("unbreaking", false),
            BooleanSetting.of("mending", false),
            BooleanSetting.of("noThorns", false)
        )
        .setVisibleWhen(this.checkFilters::getValue);
    private final NumberSetting protectionLevel = new NumberSetting("auctionhelper.protectionLevel", 4.0F, 1.0F, 5.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.armorChecks.has("protection") ? true : false);
    private final NumberSetting armorUnbreakingLevel = new NumberSetting("auctionhelper.armorUnbreakingLevel", 3.0F, 1.0F, 3.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.armorChecks.has("unbreaking") ? true : false);
    private final MultiSetting swordChecks = new MultiSetting(
            "auctionhelper.swordChecks", BooleanSetting.of("sharpness", false), BooleanSetting.of("unbreaking", false), BooleanSetting.of("mending", false)
        )
        .setVisibleWhen(this.checkFilters::getValue);
    private final NumberSetting sharpnessLevel = new NumberSetting("auctionhelper.sharpnessLevel", 5.0F, 1.0F, 7.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.swordChecks.has("sharpness") ? true : false);
    private final NumberSetting swordUnbreakingLevel = new NumberSetting("auctionhelper.swordUnbreakingLevel", 3.0F, 1.0F, 3.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.swordChecks.has("unbreaking") ? true : false);
    private final MultiSetting potionsChecks = new MultiSetting(
            "auctionhelper.potionsChecks",
            BooleanSetting.of("strength", false),
            BooleanSetting.of("speed", false),
            BooleanSetting.of("healthBoost", false),
            BooleanSetting.of("instantHealth", false),
            BooleanSetting.of("regeneration", false),
            BooleanSetting.of("poison", false),
            BooleanSetting.of("slowness", false),
            BooleanSetting.of("weakness", false),
            BooleanSetting.of("wither", false)
        )
        .setVisibleWhen(this.checkFilters::getValue);
    private final NumberSetting strengthLevel = new NumberSetting("auctionhelper.strengthLevel", 3.0F, 1.0F, 4.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.potionsChecks.has("strength") ? true : false);
    private final NumberSetting speedLevel = new NumberSetting("auctionhelper.speedLevel", 2.0F, 1.0F, 3.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.potionsChecks.has("speed") ? true : false);
    private final NumberSetting healthBoostLevel = new NumberSetting("auctionhelper.healthBoostLevel", 3.0F, 1.0F, 3.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.potionsChecks.has("healthBoost") ? true : false);
    private final NumberSetting instantHealthLevel = new NumberSetting("auctionhelper.instantHealthLevel", 2.0F, 1.0F, 2.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.potionsChecks.has("instantHealth") ? true : false);
    private final NumberSetting regenerationLevel = new NumberSetting("auctionhelper.regenerationLevel", 2.0F, 1.0F, 3.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.potionsChecks.has("regeneration") ? true : false);
    private final NumberSetting poisonLevel = new NumberSetting("auctionhelper.poisonLevel", 2.0F, 1.0F, 2.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.potionsChecks.has("poison") ? true : false);
    private final NumberSetting slownessLevel = new NumberSetting("auctionhelper.slownessLevel", 4.0F, 1.0F, 4.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.potionsChecks.has("slowness") ? true : false);
    private final NumberSetting weaknessLevel = new NumberSetting("auctionhelper.weaknessLevel", 3.0F, 1.0F, 3.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.potionsChecks.has("weakness") ? true : false);
    private final NumberSetting witherLevel = new NumberSetting("auctionhelper.witherLevel", 5.0F, 1.0F, 5.0F, 1.0F)
        .setVisibleWhen(() -> this.checkFilters.getValue() && this.potionsChecks.has("wither") ? true : false);
    private int cachedBalance = -1;

    public static AuctionHelper getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(AuctionHelper.class);
    }

    @EventHandler
    public void onKeyPress(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.isPress() && screenMouseClickEvent.getScreen() == null) {
            if (this.ahSearchBind.matchesKey(screenMouseClickEvent.getButton())) {
                this.searchHeldItem();
            }
        }
    }

    @EventHandler
    public void onMousePress(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.isPress() && screenKeyEvent.getScreen() == null) {
            if (this.ahSearchBind.matchesKey(screenKeyEvent.getKey())) {
                this.searchHeldItem();
            }
        }
    }

    private void searchHeldItem() {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            ItemStack itemStack = mc.player.getMainHandStack();
            if (!itemStack.isEmpty()) {
                String string = ServerManager.isHolyWorld() ? itemStack.getItem().getName().getString() : itemStack.getName().getString();
                String string2 = Formatting.strip(string);
                if (string2 != null) {
                    String string3 = string2.replaceAll("[^a-zA-Zа-яА-ЯёЁ ]", "").trim();
                    if (!string3.isEmpty()) {
                        ClientPlayNetworkHandler clientPlayNetworkHandler = mc.getNetworkHandler();
                        if (ServerManager.isDonut()) {
                            clientPlayNetworkHandler.sendChatCommand("ah " + string3);
                        } else {
                            clientPlayNetworkHandler.sendChatCommand("ah search " + string3);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onScreenTick(WorldTickEvent worldTickEvent) {
        if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen && isAuctionScreen(genericContainerScreen)) {
            this.updateHighlights(genericContainerScreen);
        } else {
            this.cheapestSlot = null;
            this.costEffectiveSlot = null;
            if (mc.currentScreen instanceof HandledScreen && !(mc.currentScreen instanceof InventoryScreen)) {
                this.safeBuyTracker.touch();
            }
        }
    }

    @EventHandler
    public void onContainerRender(HandledScreenRenderEvent handledScreenRenderEvent) {
        if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen && isAuctionScreen(genericContainerScreen)) {
            DefaultedList defaultedList = genericContainerScreen.getScreenHandler().slots;
            Slot slot = defaultedList.contains(this.cheapestSlot) ? this.cheapestSlot : null;
            Slot slot2 = defaultedList.contains(this.costEffectiveSlot) ? this.costEffectiveSlot : null;
            if (slot != null && slot.equals(slot2)) {
                this.highlightSlot(handledScreenRenderEvent, slot, this.pulseColor(-14774017));
            } else {
                if (slot != null) {
                    this.highlightSlot(handledScreenRenderEvent, slot, this.pulseColor(this.cheapestColor.getValue()));
                }

                if (slot2 != null) {
                    this.highlightSlot(handledScreenRenderEvent, slot2, this.pulseColor(this.costEffectiveColor.getValue()));
                }
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isSend()) {
            Packet packet = packetEvent.getPacket();
            String string = packet instanceof CommandExecutionC2SPacket
                ? ((CommandExecutionC2SPacket)packet).command()
                : (packet instanceof ChatCommandSignedC2SPacket ? ((ChatCommandSignedC2SPacket)packet).command() : null);
            if (string != null) {
                int i = mc.player == null ? -1 : mc.player.currentScreenHandler.syncId;
                Matcher matcher2 = AH_SEARCH_PATTERN.matcher(string.trim());
                Matcher matcher3 = AH_QUERY_PATTERN.matcher(string.trim());
                if (matcher2.matches()) {
                    this.safeBuyTracker.setPendingSearch(matcher2.group(1), i);
                } else if (ServerManager.isDonut() && matcher3.matches()) {
                    this.safeBuyTracker.setPendingSearch(matcher3.group(1), i);
                } else {
                    if (AH_COMMAND_PATTERN.matcher(string.trim()).lookingAt()) {
                        this.safeBuyTracker.setPendingSearch("", i);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onScreenClose(CloseScreenEvent closeScreenEvent) {
        if (closeScreenEvent.getScreen() instanceof HandledScreen && !(closeScreenEvent.getScreen() instanceof InventoryScreen)) {
            this.safeBuyTracker.clear();
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.safeBuyTracker.clear();
    }

    @EventHandler
    public void onSlotClick(SlotClickEvent slotClickEvent) {
        if (this.safeBuy.getValue() && mc.currentScreen instanceof GenericContainerScreen genericContainerScreen && isAuctionScreen(genericContainerScreen)) {
            ScreenHandler screenHandler = genericContainerScreen.getScreenHandler();
            int i = slotClickEvent.getSlotId();
            if (slotClickEvent.getSyncId() == screenHandler.syncId && i >= 0 && i < Math.min(45, screenHandler.slots.size() - 36)) {
                ItemStack itemStack = screenHandler.getSlot(i).getStack();
                if (this.safeBuyTracker.isOverLimit(itemStack)) {
                    slotClickEvent.cancel();
                    String string = AuctionLoreUtil.formatNumber(this.priceParser.getPrice(itemStack) / itemStack.getCount());
                    String string2 = AuctionLoreUtil.formatNumber(this.safeBuyTracker.getEffectiveLimit());
                    NotificationUtil.notifyKeyed(
                        NotificationType.WARNING,
                        this.getDisplayName(),
                        String.format(Language.translate("auctionhelper.safeBuy.blocked", "Покупка отменена: %s за шт. дороже вашего лимита %s"), string, string2)
                    );
                }
            }
        }
    }

    public SafeBuyLimitWidget getSafeBuyOverlay(HandledScreen<?> handledScreen) {
        boolean flag;
        if (this.isEnabled() && this.safeBuy.getValue() && handledScreen instanceof GenericContainerScreen && isAuctionScreen((GenericContainerScreen)handledScreen)) {
            flag = this.safeBuyTracker.hasData();
        } else {
            flag = false;
        }

        return flag ? this.safeBuyOverlay : null;
    }

    private int pulseColor(int i) {
        float f = 0.4F + 0.6F * (float)Math.abs(Math.sin(System.currentTimeMillis() / 10.0 * Math.PI / 180.0));
        return ColorUtil.multiplyAlpha(i, f);
    }

    private Slot findCheapestSlot(List<Slot> slots) {
        return slots.stream()
            .filter(this::isEligibleSlot)
            .min(Comparator.comparingInt(slot2 -> this.priceParser.getPrice(slot2.getStack())))
            .orElse(null);
    }

    private Slot findCostEffectiveSlot(List<Slot> slots) {
        return slots.stream()
            .filter(this::isEligibleStackSlot)
            .min(Comparator.comparingInt(slot2 -> this.priceParser.getPrice(slot2.getStack()) / slot2.getStack().getCount()))
            .orElse(null);
    }

    private boolean isEligibleSlot(Slot slot) {
        if (slot == null || !slot.hasStack()) {
            return false;
        } else if (this.isUnavailableLot(slot.getStack())) {
            return false;
        } else {
            return !this.passesChecks(slot.getStack()) ? false : this.priceParser.getPrice(slot.getStack()) >= 0;
        }
    }

    private boolean isEligibleStackSlot(Slot slot) {
        return this.isEligibleSlot(slot) && slot.getStack().getCount() > 1;
    }

    private void updateHighlights(GenericContainerScreen genericContainerScreen) {
        DefaultedList defaultedList = genericContainerScreen.getScreenHandler().slots;
        this.safeBuyTracker.onContainerUpdate(genericContainerScreen.getScreenHandler().syncId, defaultedList);
        this.cachedBalance = this.checkBalance.getValue() ? this.getBalance() : -1;
        List list = defaultedList.subList(0, Math.max(0, Math.min(45, defaultedList.size() - 36)));
        this.cheapestSlot = this.findCheapestSlot(list);
        this.costEffectiveSlot = this.findCostEffectiveSlot(list);
    }

    private boolean passesChecks(ItemStack itemStack) {
        boolean flag;
        if (!this.checkDurability.getValue() && !this.checkEnchants.getValue() && !this.checkBalance.getValue() && !this.checkFilters.getValue()) {
            if (!this.safeBuy.getValue()) {
                return true;
            }

            flag = true;
        } else {
            flag = true;
        }

        if (this.checkDurability.getValue()) {
            flag = true & (!itemStack.isDamageable() || itemStack.getDamage() == 0);
        }

        if (this.checkEnchants.getValue()) {
            flag &= itemStack.hasEnchantments();
        }

        if (this.checkBalance.getValue()) {
            int i = this.priceParser.getPrice(itemStack);
            flag &= this.cachedBalance != -1 && i != -1 && i < this.cachedBalance;
        }

        if (this.checkFilters.getValue()) {
            flag &= this.passesFilters(itemStack);
        }

        if (this.safeBuy.getValue()) {
            if (!this.safeBuyTracker.isOverLimit(itemStack)) {
                return flag;
            }

            flag = false;
        }

        return flag;
    }

    private boolean passesFilters(ItemStack itemStack) {
        boolean flag;
        if (this.isArmor(itemStack) && this.armorChecks.hasAnySelected()) {
            flag = true;
            if (this.armorChecks.has("protection")) {
                flag = true & this.getEnchantLevel(itemStack, Enchantments.PROTECTION) >= this.protectionLevel.getValue().intValue();
            }

            if (this.armorChecks.has("unbreaking")) {
                flag &= this.getEnchantLevel(itemStack, Enchantments.UNBREAKING) >= this.armorUnbreakingLevel.getValue().intValue();
            }

            if (this.armorChecks.has("mending")) {
                flag &= this.getEnchantLevel(itemStack, Enchantments.MENDING) >= 1;
            }

            if (this.armorChecks.has("noThorns")) {
                if (this.getEnchantLevel(itemStack, Enchantments.THORNS) == 0) {
                    return flag;
                }

                flag = false;
            }
        } else if (this.isSword(itemStack) && this.swordChecks.hasAnySelected()) {
            flag = true;
            if (this.swordChecks.has("sharpness")) {
                flag = true & this.getEnchantLevel(itemStack, Enchantments.SHARPNESS) >= this.sharpnessLevel.getValue().intValue();
            }

            if (this.swordChecks.has("unbreaking")) {
                flag &= this.getEnchantLevel(itemStack, Enchantments.UNBREAKING) >= this.swordUnbreakingLevel.getValue().intValue();
            }

            if (this.swordChecks.has("mending")) {
                if (this.getEnchantLevel(itemStack, Enchantments.MENDING) >= 1) {
                    return flag;
                }

                return false;
            }
        } else {
            if (!this.isPotion(itemStack) || !this.potionsChecks.hasAnySelected()) {
                return true;
            }

            flag = true;
            if (this.potionsChecks.has("strength")) {
                flag = true & this.hasEffectLevel(itemStack, StatusEffects.STRENGTH, this.strengthLevel);
            }

            if (this.potionsChecks.has("speed")) {
                flag &= this.hasEffectLevel(itemStack, StatusEffects.SPEED, this.speedLevel);
            }

            if (this.potionsChecks.has("healthBoost")) {
                flag &= this.hasEffectLevel(itemStack, StatusEffects.HEALTH_BOOST, this.healthBoostLevel);
            }

            if (this.potionsChecks.has("instantHealth")) {
                flag &= this.hasEffectLevel(itemStack, StatusEffects.INSTANT_HEALTH, this.instantHealthLevel);
            }

            if (this.potionsChecks.has("regeneration")) {
                flag &= this.hasEffectLevel(itemStack, StatusEffects.REGENERATION, this.regenerationLevel);
            }

            if (this.potionsChecks.has("poison")) {
                flag &= this.hasEffectLevel(itemStack, StatusEffects.POISON, this.poisonLevel);
            }

            if (this.potionsChecks.has("slowness")) {
                flag &= this.hasEffectLevel(itemStack, StatusEffects.SLOWNESS, this.slownessLevel);
            }

            if (this.potionsChecks.has("weakness")) {
                flag &= this.hasEffectLevel(itemStack, StatusEffects.WEAKNESS, this.weaknessLevel);
            }

            if (this.potionsChecks.has("wither")) {
                return flag & this.hasEffectLevel(itemStack, StatusEffects.WITHER, this.witherLevel);
            }
        }

        return flag;
    }

    private boolean hasEffectLevel(ItemStack itemStack, RegistryEntry<StatusEffect> registryEntry, NumberSetting numberSetting) {
        int i = this.getEffectLevel(itemStack, registryEntry);
        return i == 0 || i >= numberSetting.getValue().intValue();
    }

    private boolean isArmor(ItemStack itemStack) {
        return itemStack.isIn(ItemTags.ARMOR_ENCHANTABLE);
    }

    private boolean isSword(ItemStack itemStack) {
        return itemStack.isIn(ItemTags.SWORDS);
    }

    private boolean isPotion(ItemStack itemStack) {
        return itemStack.isOf(Items.POTION) || itemStack.isOf(Items.SPLASH_POTION) || itemStack.isOf(Items.LINGERING_POTION);
    }

    private int getEnchantLevel(ItemStack itemStack, RegistryKey<Enchantment> registryKey) {
        if (mc.world == null) {
            return 0;
        }

        Reference reference = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(registryKey);
        return EnchantmentHelper.getLevel(reference, itemStack);
    }

    private int getEffectLevel(ItemStack itemStack, RegistryEntry<StatusEffect> registryEntry) {
        PotionContentsComponent potionContentsComponent = itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

        for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
            if (statusEffectInstance.getEffectType().equals(registryEntry)) {
                return statusEffectInstance.getAmplifier() + 1;
            }
        }

        return 0;
    }

    public int getBalance() {
        long l = ServerManager.getCoins();
        return l < 0L ? -1 : (int)Math.min(l, 2147483647L);
    }

    public static boolean isAuctionScreen(GenericContainerScreen genericContainerScreen) {
        String string = genericContainerScreen.getTitle().getString().toLowerCase(Locale.ROOT);
        return string.contains("аукцион") || string.contains("auction") || string.contains("поиск") || string.contains("☃") || string.contains("漢");
    }

    private boolean isUnavailableLot(ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            if (itemStack.isOf(Items.GRAY_DYE)) {
                return true;
            }

            String string = itemStack.getName().getString().toLowerCase(Locale.ROOT);
            if (!string.contains("не актуален")) {
                if (string.contains("убрано с продажи")) {
                    return true;
                }

                LoreComponent loreComponent = itemStack.getComponents().get(DataComponentTypes.LORE);
                if (loreComponent == null) {
                    return false;
                }

                Iterator iterator2 = loreComponent.lines().iterator();

                while (true) {
                    if (!iterator2.hasNext()) {
                        return false;
                    }

                    Text text = (Text)iterator2.next();
                    String string2 = text.getString().toLowerCase(Locale.ROOT);
                    if (string2.contains("не актуален")) {
                        break;
                    }

                    if (string2.contains("убрано с продажи")) {
                        return true;
                    }
                }
            }
        }

        return true;
    }

    private void highlightSlot(HandledScreenRenderEvent handledScreenRenderEvent, Slot slot, int i) {
        handledScreenRenderEvent.getContext().fill(slot.x, slot.y, slot.x + 16, slot.y + 16, i);
    }

    public AuctionPriceParser getPriceParser() {
        return this.priceParser;
    }

    public Slot getCheapestSlot() {
        return this.cheapestSlot;
    }

    public Slot getCostEffectiveSlot() {
        return this.costEffectiveSlot;
    }

    public ColorSetting getCheapestColor() {
        return this.cheapestColor;
    }

    public ColorSetting getCostEffectiveColor() {
        return this.costEffectiveColor;
    }

    public KeybindSetting getAhSearchBind() {
        return this.ahSearchBind;
    }

    public BooleanSetting getSafeBuy() {
        return this.safeBuy;
    }

    public NumberSetting getSafeBuyTolerance() {
        return this.safeBuyTolerance;
    }

    public BooleanSetting getCheckDurability() {
        return this.checkDurability;
    }

    public BooleanSetting getCheckEnchants() {
        return this.checkEnchants;
    }

    public BooleanSetting getCheckBalance() {
        return this.checkBalance;
    }

    public BooleanSetting getCheckFilters() {
        return this.checkFilters;
    }

    public MultiSetting getArmorChecks() {
        return this.armorChecks;
    }

    public NumberSetting getProtectionLevel() {
        return this.protectionLevel;
    }

    public NumberSetting getArmorUnbreakingLevel() {
        return this.armorUnbreakingLevel;
    }

    public MultiSetting getSwordChecks() {
        return this.swordChecks;
    }

    public NumberSetting getSharpnessLevel() {
        return this.sharpnessLevel;
    }

    public NumberSetting getSwordUnbreakingLevel() {
        return this.swordUnbreakingLevel;
    }

    public MultiSetting getPotionsChecks() {
        return this.potionsChecks;
    }

    public NumberSetting getStrengthLevel() {
        return this.strengthLevel;
    }

    public NumberSetting getSpeedLevel() {
        return this.speedLevel;
    }

    public NumberSetting getHealthBoostLevel() {
        return this.healthBoostLevel;
    }

    public NumberSetting getInstantHealthLevel() {
        return this.instantHealthLevel;
    }

    public NumberSetting getRegenerationLevel() {
        return this.regenerationLevel;
    }

    public NumberSetting getPoisonLevel() {
        return this.poisonLevel;
    }

    public NumberSetting getSlownessLevel() {
        return this.slownessLevel;
    }

    public NumberSetting getWeaknessLevel() {
        return this.weaknessLevel;
    }

    public NumberSetting getWitherLevel() {
        return this.witherLevel;
    }

    public int getCachedBalance() {
        return this.cachedBalance;
    }
}
