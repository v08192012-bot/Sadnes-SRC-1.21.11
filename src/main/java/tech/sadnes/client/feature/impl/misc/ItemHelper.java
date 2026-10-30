package tech.sadnes.client.feature.impl.misc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.HandledScreenRenderEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.PixelRenderUtil;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.network.PacketQueue;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "ItemHelper",
    description = "feature.impl.misc.itemhelper",
    displayName = "feature.impl.misc.itemhelper",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_INVENTORY
)
public final class ItemHelper extends Feature {
    private final MultiSetting checks = new MultiSetting("itemhelper.checks", BooleanSetting.of("highlight", false), BooleanSetting.of("keybind", false));
    private final MultiSetting itemType = new MultiSetting(
            "itemhelper.itemType",
            BooleanSetting.of("enchanted_apple", false),
            BooleanSetting.of("golden_apple", false),
            BooleanSetting.of("healing_potion", false),
            BooleanSetting.of("pearls", false),
            BooleanSetting.of("chorus", true)
        )
        .setVisibleWhen(() -> this.checks.isSelected("highlight"));
    private final Map<ItemHelper.BindableItem, KeybindSetting> binds = new LinkedHashMap<>();
    private boolean using;
    private Slot swappedSlot;
    private boolean holdingUse;
    private int useDelayTicks;
    private ItemHelper.BindableItem currentItem;
    private boolean pendingRestore;
    private int restoreDelayTicks;
    private int previousHotbarSlot = -1;

    public static ItemHelper getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(ItemHelper.class);
    }

    public ItemHelper() {
        this.binds.put(ItemHelper.BindableItem.CHORUS, this.createBind("itemhelper.bind.chorus"));
        this.binds.put(ItemHelper.BindableItem.INSTHEAL, this.createBind("itemhelper.bind.instheal"));
        this.binds.put(ItemHelper.BindableItem.SPEED, this.createBind("itemhelper.bind.speed"));
        this.binds.put(ItemHelper.BindableItem.STRENGTH, this.createBind("itemhelper.bind.strength"));
        this.binds.put(ItemHelper.BindableItem.GOLD, this.createBind("itemhelper.bind.gold"));
        this.binds.put(ItemHelper.BindableItem.ENCHGOLD, this.createBind("itemhelper.bind.enchgold"));
        this.binds.put(ItemHelper.BindableItem.GOLDENCARROT, this.createBind("itemhelper.bind.carrot"));
        this.binds.put(ItemHelper.BindableItem.MILK, this.createBind("itemhelper.bind.milk"));
        this.binds.put(ItemHelper.BindableItem.RIPTIDE, this.createBind("itemhelper.bind.riptide"));
    }

    private KeybindSetting createBind(String string) {
        return new KeybindSetting(string).setVisibleWhen(() -> this.checks.isSelected("keybind"));
    }

    @EventHandler
    public void onContainerRender(HandledScreenRenderEvent handledScreenRenderEvent) {
        if (this.checks.isSelected("highlight") && mc.player != null && mc.player.currentScreenHandler != null) {
            String string = mc.currentScreen != null ? mc.currentScreen.getTitle().getString().toLowerCase() : "";
            if (!string.contains("аукцион")) {
                if (string.contains("поиск")) {
                    return;
                }

                DrawContext drawContext = handledScreenRenderEvent.getContext();
                float f = (float)((Math.sin(System.currentTimeMillis() / 100.0) + 1.0) / 2.0);
                int i = ColorUtil.rgbWithAlphaf(255, 0, 0, f * 0.5F);

                for (Slot slot : mc.player.currentScreenHandler.slots) {
                    if (this.shouldHighlight(slot.getStack())) {
                        PixelRenderUtil.drawRect(drawContext, slot.x, slot.y, 16.0F, 16.0F, i);
                    }
                }
            }
        }
    }

    private boolean shouldHighlight(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }

        if (itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            return this.itemType.isSelected("enchanted_apple");
        }

        if (itemStack.isOf(Items.GOLDEN_APPLE)) {
            return this.itemType.isSelected("golden_apple");
        }

        if (itemStack.isOf(Items.ENDER_PEARL)) {
            return this.itemType.isSelected("pearls");
        }

        if (itemStack.isOf(Items.CHORUS_FRUIT)) {
            return this.itemType.isSelected("chorus");
        }

        if (itemStack.isOf(Items.POTION) && this.itemType.isSelected("healing_potion")) {
            PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
            if (potionContentsComponent == null) {
                return false;
            }

            for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
                if (statusEffectInstance.getEffectType().equals(StatusEffects.INSTANT_HEALTH)) {
                    return true;
                }
            }
        }

        return false;
    }

    @EventHandler
    public void onKey(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.getScreen() == null && this.checks.isSelected("keybind")) {
            this.handleBindInput(screenMouseClickEvent.getButton(), screenMouseClickEvent.isPress(), screenMouseClickEvent.isRelease());
        }
    }

    @EventHandler
    public void onMouseButton(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.getScreen() == null && this.checks.isSelected("keybind")) {
            this.handleBindInput(screenKeyEvent.getKey(), screenKeyEvent.isPress(), screenKeyEvent.isRelease());
        }
    }

    private void handleBindInput(int i, boolean flag, boolean flag2) {
        this.binds.forEach((bindableItem, keybindSetting) -> {
            if (keybindSetting.matchesKey(i)) {
                if (flag) {
                    this.startUsing(bindableItem);
                } else {
                    if (flag2) {
                        this.stopUsing();
                    }
                }
            }
        });
    }

    private void startUsing(ItemHelper.BindableItem bindableItem) {
        if (!this.using && mc.player != null) {
            Slot slot = bindableItem.potion ? InventoryUtil.findPotionSlotByEntry(bindableItem.effect) : InventoryUtil.findSlotMatching(itemStack -> this.matchesItem(bindableItem, itemStack));
            if (slot != null) {
                this.using = true;
                int i = InventoryUtil.toHotbarIndex(slot.id);
                if (i != -1) {
                    if (!this.pendingRestore) {
                        this.previousHotbarSlot = InventoryUtil.getSelectedSlot();
                    }

                    this.pendingRestore = false;
                    PacketQueue.queue(() -> InventoryUtil.setSelectedSlot(i));
                } else {
                    this.swappedSlot = slot;
                    PacketQueue.queue(() -> InventoryUtil.swapToHand(slot, Hand.MAIN_HAND));
                }

                this.currentItem = bindableItem;
                this.holdingUse = true;
                this.useDelayTicks = 1;
            }
        }
    }

    private void stopUsing() {
        if (this.using) {
            this.using = false;
            this.holdingUse = false;
            this.currentItem = null;
            mc.options.useKey.setPressed(false);
            if (this.swappedSlot != null) {
                Slot slot = this.swappedSlot;
                this.swappedSlot = null;
                PacketQueue.queue(() -> InventoryUtil.swapToHand(slot, Hand.MAIN_HAND));
            } else {
                if (this.previousHotbarSlot != -1) {
                    this.pendingRestore = true;
                    this.restoreDelayTicks = 1;
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.holdingUse && this.using) {
            if (this.useDelayTicks > 0) {
                this.useDelayTicks--;
            } else if (this.isHoldingItem(this.currentItem)) {
                mc.options.useKey.setPressed(true);
            }
        }

        if (this.pendingRestore) {
            if (this.restoreDelayTicks > 0) {
                this.restoreDelayTicks--;
                return;
            }

            int i = this.previousHotbarSlot;
            this.previousHotbarSlot = -1;
            this.pendingRestore = false;
            PacketQueue.queue(() -> InventoryUtil.setSelectedSlot(i));
        }
    }

    private boolean isHoldingItem(ItemHelper.BindableItem bindableItem) {
        return bindableItem != null && mc.player != null && this.matchesItem(bindableItem, mc.player.getMainHandStack());
    }

    private boolean matchesItem(ItemHelper.BindableItem bindableItem, ItemStack itemStack) {
        if (!itemStack.isOf(bindableItem.item)) {
            return false;
        }

        if (bindableItem.enchantment != null) {
            for (RegistryEntry registryEntry : itemStack.getEnchantments().getEnchantments()) {
                if (registryEntry.matchesKey(bindableItem.enchantment)) {
                    return true;
                }
            }
        } else {
            if (!bindableItem.potion) {
                return true;
            }

            PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
            if (potionContentsComponent == null) {
                return false;
            }

            for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
                if (statusEffectInstance.getEffectType().equals(bindableItem.effect)) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<ItemHelper.ItemBindInfo> getBindInfos() {
        ArrayList arrayList = new ArrayList(this.binds.size());
        this.binds.forEach((bindableItem, keybindSetting) -> arrayList.add(new ItemHelper.ItemBindInfo(keybindSetting, bindableItem.item, bindableItem.potion, bindableItem.effect)));
        return arrayList;
    }

    enum BindableItem {
        INSTHEAL(Items.POTION, true, StatusEffects.INSTANT_HEALTH, null),
        SPEED(Items.POTION, true, StatusEffects.SPEED, null),
        STRENGTH(Items.POTION, true, StatusEffects.STRENGTH, null),
        CHORUS(Items.CHORUS_FRUIT, false, null, null),
        GOLD(Items.GOLDEN_APPLE, false, null, null),
        ENCHGOLD(Items.ENCHANTED_GOLDEN_APPLE, false, null, null),
        GOLDENCARROT(Items.GOLDEN_CARROT, false, null, null),
        MILK(Items.MILK_BUCKET, false, null, null),
        RIPTIDE(Items.TRIDENT, false, null, Enchantments.RIPTIDE);

        final Item item;
        final boolean potion;
        final RegistryEntry<StatusEffect> effect;
        final RegistryKey<Enchantment> enchantment;

        public Item getItem() {
            return this.item;
        }

        public boolean isPotion() {
            return this.potion;
        }

        public RegistryEntry<StatusEffect> getEffect() {
            return this.effect;
        }

        public RegistryKey<Enchantment> getEnchantment() {
            return this.enchantment;
        }

        BindableItem(Item item2, boolean flag, RegistryEntry<StatusEffect> registryEntry, RegistryKey<Enchantment> registryKey) {
            this.item = item2;
            this.potion = flag;
            this.effect = registryEntry;
            this.enchantment = registryKey;
        }
    }

    public record ItemBindInfo(KeybindSetting bind, Item item, boolean potion, RegistryEntry<StatusEffect> effect) {
    }
}
