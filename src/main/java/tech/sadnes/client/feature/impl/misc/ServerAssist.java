package tech.sadnes.client.feature.impl.misc;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.MouseLookEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Bind;
import tech.sadnes.client.feature.BindMode;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.feature.impl.misc.serverassist.HolyTrap;
import tech.sadnes.client.feature.impl.misc.serverassist.TrapEffect;
import tech.sadnes.client.feature.impl.misc.serverassist.TrapType;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.neuro.NeuroAim;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.server.DonateItem;
import tech.sadnes.client.server.DonateItemRegistry;
import tech.sadnes.client.server.FunTimeEventType;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.ServerType;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.ui.RadialMenu;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.ui.hud.WaypointRenderer;
import tech.sadnes.client.util.ActionSequence;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.SelfPearlUtil;
import tech.sadnes.client.util.rotation.RotationUtil;

@FeatureInfo(
    name = "ServerAssist",
    description = "feature.impl.misc.serverassist",
    displayName = "feature.impl.misc.serverassist",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SERVER
)
public class ServerAssist extends Feature {
    private final ModeSetting server = new ModeSetting("serverassist.server", new String[]{"auto", "funtime", "holyworld", "reallyworld"});
    private final MultiSetting funtimeUtils = new MultiSetting(
        "serverassist.funtimeUtils", BooleanSetting.of("auto_point", false), BooleanSetting.of("structures", false), BooleanSetting.of("event_delay", false)
    );
    private final MultiSetting pointEvents = new MultiSetting(
        "serverassist.pointEvents", BooleanSetting.of("summonable", true), BooleanSetting.of("regular", true)
    );
    private final MultiSetting holyUtils = new MultiSetting("serverassist.holyUtils", BooleanSetting.of("auto_loot", false), BooleanSetting.of("traps", false));
    private final GroupSetting use = new GroupSetting("serverassist.use", true);
    private final Map<String, BooleanSetting> wheelEnabledByServer = new LinkedHashMap<>();
    private final KeybindSetting wheelItemsBind = new KeybindSetting(this.use, "serverassist.wheel.items").setVisibleWhen(this::isWheelEnabled);
    private final KeybindSetting wheelPotionsBind = new KeybindSetting(this.use, "serverassist.wheel.potions")
        .setVisibleWhen(() -> this.isWheelEnabled() && this.isServer("funtime") ? true : false);
    private final BooleanSetting useFromBundle = new BooleanSetting("serverassist.use_from_bundle", false);
    private final Map<DonateItem, KeybindSetting> itemBinds = new LinkedHashMap<>();
    private final Map<DonateItem, BooleanSetting> wheelItemToggles = new LinkedHashMap<>();
    private final Map<DonateItem, BooleanSetting> syncHeadToggles = new LinkedHashMap<>();
    private final Map<DonateItem, BooleanSetting> smartAimToggles = new LinkedHashMap<>();
    private final RadialMenu itemWheel = new RadialMenu()
        .withDefaultLabel(() -> Language.translate("serverassist.wheel.title", "Выберите предмет"))
        .withHint(() -> Language.translate("serverassist.wheel.hint", "В сторону — использовать · в центр — отмена"));
    private final List<DonateItem> wheelEntries = new ArrayList<>();
    private final Map<DonateItem, ItemStack> wheelIconStacks = new EnumMap<>(DonateItem.class);
    private final StopWatch wheelOpenTimer = new StopWatch();
    private ValueBind<Integer> wheelOpenBind;
    private boolean wheelReleasedEarly;
    private final List<ServerAssist.PendingItemUse> pendingUses = new ArrayList<>();
    private final ModeSetting rebuffFirst = new ModeSetting(
        "serverassist.rebuff.first", new String[]{"POPPER", "HOLY_WATER", "RAGE", "PALADIN", "ASSASSIN", "RADIATION", "SLEEP"}
    );
    private final ModeSetting rebuffSecond = new ModeSetting(
        "serverassist.rebuff.second", new String[]{"POPPER", "HOLY_WATER", "RAGE", "PALADIN", "ASSASSIN", "RADIATION", "SLEEP"}
    );
    private final ActionSequence antiFlySequence = new ActionSequence();
    private final StopWatch autoLootTimer = new StopWatch();
    private final List<TrapType> trapTypes = new ArrayList<>();
    private final List<ServerAssist.DetectedTrap> detectedTraps = new ArrayList<>();
    private final List<HolyTrap> holyTraps = new ArrayList<>();
    private final List<BlockPos> pendingStructureBlocks = new ArrayList<>();
    private final StopWatch structureBatchTimer = new StopWatch();
    private final ActionSequence rebuffSequence = new ActionSequence();
    private String pendingEventName;
    private final StopWatch lastUseTimer = new StopWatch();
    private final Animation trapTimerAnimation = new Animation();
    private HolyTrap displayedTrap;
    private static final Pattern EVENT_HEADER_PATTERN = Pattern.compile("\\[\\d+] .*:");
    private static final Pattern EVENT_PREFIX_PATTERN = Pattern.compile("^\\[\\d+]\\s*");
    private static final Pattern COORDINATES_PATTERN = Pattern.compile("(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)");
    private static final Set<String> SUMMONABLE_EVENTS = Set.of("Аир-дроп", "Загадочный маяк");

    public static ServerAssist getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(ServerAssist.class);
    }

    public boolean isUsingItem() {
        return this.isEnabled() && !this.lastUseTimer.hasPassed(250.0);
    }

    public ServerAssist() {
        this.funtimeUtils.setVisibleWhen(() -> this.isServer("funtime"));
        this.pointEvents.setVisibleWhen(() -> this.isServer("funtime") && this.funtimeUtils.has("auto_point") ? true : false);
        this.holyUtils.setVisibleWhen(() -> this.isServer("holyworld"));

        for (ServerType serverType : ServerType.values()) {
            String string = serverType.getApiName();
            if (!this.wheelEnabledByServer.containsKey(string)) {
                BooleanSetting booleanSetting = new BooleanSetting(this.use, "serverassist.wheel.enabled", false);
                booleanSetting.setDisplayName("serverassist.wheel.enabled." + string);
                booleanSetting.setAliases("serverassist.wheel.enabled");
                booleanSetting.setVisibleWhen(() -> this.isServer(string));
                this.wheelEnabledByServer.put(string, booleanSetting);
            }
        }

        this.rebuffFirst.setVisibleWhen(() -> this.isServer("funtime"));
        this.rebuffSecond.setVisibleWhen(() -> this.isServer("funtime"));

        for (DonateItem donateItem : DonateItem.values()) {
            KeybindSetting keybindSetting = new KeybindSetting(this.use, donateItem.getDisplayName());
            keybindSetting.setDisplayName("serverassist.item." + donateItem.name());
            keybindSetting.setAliases(donateItem.getDisplayName(), "Бинд: " + donateItem.getDisplayName());
            keybindSetting.setVisibleWhen(() -> this.isServer(donateItem.getServer().getApiName()));
            this.itemBinds.put(donateItem, keybindSetting);
            BooleanSetting booleanSetting2 = new BooleanSetting(this.use, "serverassist.wheelitem." + donateItem.name(), false);
            booleanSetting2.setVisibleWhen(() -> false);
            this.wheelItemToggles.put(donateItem, booleanSetting2);
            BooleanSetting booleanSetting3 = new BooleanSetting(this.use, "serverassist.syncHead", false);
            booleanSetting3.setDisplayName("serverassist.synchead." + donateItem.name());
            booleanSetting3.setVisibleWhen(() -> false);
            this.syncHeadToggles.put(donateItem, booleanSetting3);
            if (donateItem.isPositivePotion()) {
                BooleanSetting booleanSetting4 = new BooleanSetting(this.use, "serverassist.smart_aim", true);
                booleanSetting4.setDisplayName("serverassist.smartaim." + donateItem.name());
                booleanSetting4.setVisibleWhen(() -> false);
                this.smartAimToggles.put(donateItem, booleanSetting4);
            }
        }

        this.registerTrapTypes();
    }

    public String getActiveServer() {
        if (!this.server.is("auto")) {
            return this.server.getValue();
        }

        for (ServerType serverType : ServerType.values()) {
            if (serverType.isCurrent()) {
                return serverType.getApiName();
            }
        }

        return "";
    }

    private boolean isServer(String string) {
        return this.getActiveServer().equals(string);
    }

    private boolean isWheelEnabled() {
        BooleanSetting booleanSetting = this.wheelEnabledByServer.get(this.getActiveServer());
        return booleanSetting != null && booleanSetting.getValue();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!this.antiFlySequence.isFinished()) {
            mc.options.sneakKey.setPressed(false);
        }

        this.antiFlySequence.clear();
        this.pendingStructureBlocks.clear();
        this.detectedTraps.clear();
        this.holyTraps.clear();
        this.displayedTrap = null;
        this.rebuffSequence.clear();
        this.itemWheel.closeImmediately();
        this.wheelEntries.clear();
        this.wheelOpenBind = null;
        this.wheelReleasedEarly = false;
        this.pendingUses.clear();
    }

    @EventHandler
    public void onKey(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.getScreen() == null) {
            this.handleInput(screenMouseClickEvent.getButton(), screenMouseClickEvent.isPress(), screenMouseClickEvent.isRelease());
        }
    }

    @EventHandler
    public void onMouseButton(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.getScreen() == null) {
            this.handleInput(screenKeyEvent.getKey(), screenKeyEvent.isPress(), screenKeyEvent.isRelease());
        }
    }

    private void handleInput(int i, boolean flag, boolean flag2) {
        boolean flag3;
        if (flag) {
            flag3 = this.handleItemBinds(i);
        } else {
            flag3 = false;
        }

        if (flag2) {
            this.flushPendingUsesForKey(i);
        }

        if (flag2 && this.itemWheel.isOpen()) {
            this.handleWheelInput(i, false, true);
        } else {
            if (!flag3 && this.isWheelEnabled()) {
                this.handleWheelInput(i, flag, flag2);
            }
        }
    }

    public void setSingleBind(KeybindSetting keybindSetting, int i, int[] ints) {
        if (keybindSetting != null) {
            keybindSetting.clearBinds();
            keybindSetting.setValue(Integer.valueOf(-1));
            if (i != -1) {
                ValueBind valueBind = new ValueBind<>(i, BindMode.HOLD, i);
                if (ints == null) {
                    valueBind.setModifiers(Bind.NO_MODIFIERS);
                    keybindSetting.addBind(valueBind);
                } else {
                    valueBind.setModifiers(ints);
                    keybindSetting.addBind(valueBind);
                }
            }
        }
    }

    public ValueBind<Integer> getPrimaryBind(KeybindSetting keybindSetting) {
        return keybindSetting != null && !keybindSetting.getBinds().isEmpty() ? keybindSetting.getBinds().get(0) : null;
    }

    private boolean handleItemBinds(int i) {
        int i2 = 0;

        for (Entry entry : this.itemBinds.entrySet()) {
            if (this.isServer(((DonateItem)entry.getKey()).getServer().getApiName())) {
                ValueBind valueBind = this.getPrimaryBind((KeybindSetting)entry.getValue());
                if (valueBind != null && valueBind.matches(i)) {
                    i2 = Math.max(i2, 1 + valueBind.getModifiers().length);
                }
            }
        }

        if (i2 == 0) {
            return false;
        }

        if (i2 > 1) {
            this.pendingUses.clear();
        }

        for (Entry entry2 : this.itemBinds.entrySet()) {
            DonateItem donateItem = (DonateItem)entry2.getKey();
            if (this.isServer(donateItem.getServer().getApiName())) {
                ValueBind valueBind2 = this.getPrimaryBind((KeybindSetting)entry2.getValue());
                if (valueBind2 != null && valueBind2.matches(i) && 1 + valueBind2.getModifiers().length == i2) {
                    if (i2 == 1 && this.isPartOfComboBind(i)) {
                        this.pendingUses.add(new ServerAssist.PendingItemUse(donateItem, i, new StopWatch()));
                    } else {
                        this.triggerItemUse(donateItem);
                    }
                }
            }
        }

        return i2 > 1;
    }

    private void triggerItemUse(DonateItem donateItem) {
        this.lastUseTimer.reset();
        this.useItem(donateItem);
    }

    private boolean isPartOfComboBind(int i) {
        for (Entry entry : this.itemBinds.entrySet()) {
            if (this.isServer(((DonateItem)entry.getKey()).getServer().getApiName())
                && this.bindUsesKey(this.getPrimaryBind((KeybindSetting)entry.getValue()), i)) {
                return true;
            }
        }

        return this.bindUsesKey(this.getPrimaryBind(this.wheelItemsBind), i) || this.bindUsesKey(this.getPrimaryBind(this.wheelPotionsBind), i);
    }

    private boolean bindUsesKey(ValueBind<Integer> valueBind, int i) {
        if (valueBind != null && valueBind.getModifiers().length != 0) {
            if (Key.matches(valueBind.getKey(), i)) {
                return true;
            }

            for (int i2 : valueBind.getModifiers()) {
                if (Key.matches(i2, i)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void flushPendingUsesForKey(int i) {
        for (int i2 = 0; i2 < this.pendingUses.size(); i2++) {
            ServerAssist.PendingItemUse pendingItemUse = this.pendingUses.get(i2);
            if (Key.matches(pendingItemUse.key(), i)) {
                this.pendingUses.remove(i2--);
                this.triggerItemUse(pendingItemUse.item());
            }
        }
    }

    private void flushExpiredPendingUses() {
        for (int i = 0; i < this.pendingUses.size(); i++) {
            ServerAssist.PendingItemUse pendingItemUse = this.pendingUses.get(i);
            if (pendingItemUse.watch().hasPassed(140.0)) {
                this.pendingUses.remove(i--);
                this.triggerItemUse(pendingItemUse.item());
            }
        }
    }

    private void handleWheelInput(int i, boolean flag, boolean flag2) {
        if (flag2) {
            if (this.itemWheel.isOpen() && this.isWheelBindKey(i)) {
                if (this.wheelOpenTimer.hasPassed(200.0)) {
                    this.closeWheelAndUse();
                } else {
                    this.wheelReleasedEarly = true;
                }
            }
        } else if (flag) {
            boolean flag3 = this.wheelItemsBind.matchesKey(i);
            boolean flag4;
            if (this.wheelPotionsBind.matchesKey(i) && this.isServer("funtime")) {
                flag4 = true;
            } else {
                flag4 = false;
                if (!flag3) {
                    return;
                }
            }

            if (this.itemWheel.isOpen()) {
                this.closeWheelAndUse();
            } else {
                this.openWheel(flag4);
            }
        }
    }

    private void openWheel(boolean flag) {
        if (!RadialMenu.isAnyOpen() && mc.player != null && mc.currentScreen == null) {
            String string = this.getActiveServer();
            this.wheelEntries.clear();

            for (DonateItem donateItem : DonateItem.values()) {
                if (donateItem.getServer().getApiName().equals(string) && donateItem.isPotion() == flag && this.wheelItemToggles.get(donateItem).getValue()) {
                    this.wheelEntries.add(donateItem);
                }
            }

            if (this.wheelEntries.isEmpty()) {
                NotificationUtil.info(this.getDisplayName(), Language.translate("serverassist.wheel.empty_hint", "Колесо пусто — добавь предметы в настройках"));
            } else {
                this.lastUseTimer.reset();
                this.wheelOpenTimer.reset();
                this.wheelOpenBind = this.getPrimaryBind(flag ? this.wheelPotionsBind : this.wheelItemsBind);
                this.wheelReleasedEarly = false;
                this.itemWheel.open();
            }
        }
    }

    private void closeWheelAndUse() {
        if (this.itemWheel.isOpen()) {
            int i = this.itemWheel.getSelectedIndex(this.wheelEntries.size());
            this.itemWheel.close();
            this.wheelOpenBind = null;
            this.wheelReleasedEarly = false;
            if (i >= 0 && mc.player != null) {
                this.lastUseTimer.reset();
                this.useItem(this.wheelEntries.get(i));
            }
        }
    }

    private boolean isWheelBindKey(int i) {
        if (this.wheelOpenBind == null) {
            return false;
        }

        if (Key.matches(this.wheelOpenBind.getKey(), i)) {
            return true;
        }

        for (int i2 : this.wheelOpenBind.getModifiers()) {
            if (Key.matches(i2, i)) {
                return true;
            }
        }

        return false;
    }

    private void updateWheelRelease() {
        if (this.itemWheel.isOpen() && !this.wheelReleasedEarly && this.wheelOpenBind != null) {
            if (this.wheelOpenTimer.hasPassed(200.0) && !this.wheelOpenBind.isHeld()) {
                this.closeWheelAndUse();
            }
        }
    }

    @EventHandler(priority = 250)
    public void onMouseMove(MouseLookEvent mouseLookEvent) {
        this.itemWheel.onMouseMove(mouseLookEvent);
    }

    @EventHandler
    public void onRender2D(Render2DEvent render2DEvent) {
        if (mc.currentScreen != null) {
            this.itemWheel.closeImmediately();
            this.wheelOpenBind = null;
            this.wheelReleasedEarly = false;
        }

        if (this.itemWheel.isVisible()) {
            ArrayList arrayList = new ArrayList(this.wheelEntries.size());

            for (DonateItem donateItem : this.wheelEntries) {
                boolean flag;
                int i;
                if (donateItem == DonateItem.BUFF) {
                    flag = true;
                    i = 0;
                } else {
                    flag = false;
                    i = this.getItemCount(donateItem);
                }

                int i2 = i;
                arrayList.add(
                    new RadialMenu.Entry(
                        this.wheelIconStacks.computeIfAbsent(donateItem, DonateItem::createStack),
                        donateItem.getDisplayName(),
                        flag ? null : String.valueOf(i2),
                        !flag && i2 <= 0
                    )
                );
            }

            this.itemWheel.render(render2DEvent.getContext(), arrayList);
        }
    }

    private void useItem(DonateItem donateItem) {
        if (donateItem == DonateItem.BUFF) {
            this.performRebuff();
        } else {
            Slot slot = this.findItemSlot(donateItem);
            if (slot == null) {
                if (this.isServer("funtime") && this.useFromBundle.getValue()) {
                    InventoryUtil.BundleSlot bundleSlot2 = this.findInBundle(donateItem);
                    if (bundleSlot2 != null) {
                        Slot slot2 = this.findEmptySlot();
                        if (slot2 != null) {
                            if (donateItem == DonateItem.ANTIFLY) {
                                NotificationUtil.error(
                                    this.getDisplayName(), Language.translate("serverassist.notify.antifly_bundle", "Anti-Fly нельзя юзать из мешка!")
                                );
                                return;
                            }

                            ThrowComponent.getInstance().useFromSlot(bundleSlot2.bundleSlot().id, slot2.id, bundleSlot2.indexInBundle(), this.getSmartAimRotation(donateItem));
                            return;
                        }

                        NotificationUtil.error(
                            this.getDisplayName(), Language.translate("serverassist.notify.no_empty_slot", "Нет пустого слота для предмета из мешочка!")
                        );
                        return;
                    }
                }

                NotificationUtil.error(
                    this.getDisplayName(), String.format(Language.translate("serverassist.notify.no_item", "Предмета %s нет"), donateItem.getDisplayName())
                );
            } else if (donateItem == DonateItem.ANTIFLY) {
                this.useAntiFly(slot);
            } else if (donateItem == DonateItem.EXPLOSIVE_STUFF) {
                this.useExplosiveStuff(slot);
            } else {
                Vector2f vector2f = this.getSmartAimRotation(donateItem);
                if (vector2f != null) {
                    if (this.syncHeadToggles.get(donateItem).getValue()) {
                        RotationComponent.rotateTo(new Rotation(vector2f.x, vector2f.y), 999.0F, 999.0F, 2, 6);
                    }

                    ThrowComponent.getInstance().useSlotWithRotation(slot, vector2f);
                } else if (this.syncHeadToggles.get(donateItem).getValue()) {
                    this.useWithHeadSync(slot);
                } else {
                    ThrowComponent.getInstance().useSlot(slot);
                }
            }
        }
    }

    private Vector2f getSmartAimRotation(DonateItem donateItem) {
        BooleanSetting booleanSetting = this.smartAimToggles.get(donateItem);
        return booleanSetting != null && booleanSetting.getValue() ? SelfPearlUtil.getRotationOrDefault() : null;
    }

    private void useWithHeadSync(Slot slot) {
        RotationComponent.rotateTo(new Rotation(Rotation.getCameraYaw(), Rotation.getCameraPitch()), 999.0F, 999.0F, 2, 6);
        ThrowComponent.getInstance().useSlot(slot);
    }

    private void useExplosiveStuff(Slot slot) {
        ThrowComponent throwComponent = ThrowComponent.getInstance();
        if (ServerManager.isSpookyTime()) {
            throwComponent.useSlotWithRotation(slot, new Vector2f(Rotation.getCameraYaw(), 89.0F));
        } else {
            RotationComponent.rotateTo(new Rotation(mc.player.getYaw(), 90.0F), 999.0F, 999.0F, 2, 6);
            throwComponent.useSlotWithRotation(slot, new Vector2f(mc.player.getYaw(), mc.player.getPitch()));
        }
    }

    private void useAntiFly(Slot slot) {
        if (this.antiFlySequence.isFinished()) {
            ThrowComponent throwComponent = ThrowComponent.getInstance();
            if (throwComponent.swapToHand(slot.id, Hand.OFF_HAND)) {
                this.antiFlySequence.clear().addTicked(0, () -> mc.options.sneakKey.setPressed(true)).addTicked(15, () -> {
                    mc.options.sneakKey.setPressed(false);
                    throwComponent.swapToHand(slot.id, Hand.OFF_HAND);
                });
            }
        }
    }

    private void performRebuff() {
        DonateItem donateItem = DonateItem.valueOf(this.rebuffFirst.getValue());
        DonateItem donateItem2 = DonateItem.valueOf(this.rebuffSecond.getValue());
        Slot slot = this.findItemSlot(donateItem);
        InventoryUtil.BundleSlot bundleSlot2 = slot == null && this.isServer("funtime") && this.useFromBundle.getValue() ? this.findInBundle(donateItem) : null;
        Slot slot2 = this.findItemSlot(DonateItem.AURA);
        InventoryUtil.BundleSlot bundleSlot3 = slot2 == null && this.isServer("funtime") && this.useFromBundle.getValue() ? this.findInBundle(DonateItem.AURA) : null;
        Slot slot3 = this.findItemSlot(donateItem2);
        InventoryUtil.BundleSlot bundleSlot4 = slot3 == null && this.isServer("funtime") && this.useFromBundle.getValue() ? this.findInBundle(donateItem2) : null;

        if (slot == null && bundleSlot2 == null) {
            NotificationUtil.error(
                this.getDisplayName(),
                String.format(Language.translate("serverassist.notify.no_first_potion", "Нет первого зелья для разбафа (%s)!"), donateItem.getDisplayName())
            );
        } else if (slot2 == null && bundleSlot3 == null) {
            NotificationUtil.error(this.getDisplayName(), Language.translate("serverassist.notify.no_aura", "Нет Божьей ауры для разбафа!"));
        } else if (slot3 == null && bundleSlot4 == null) {
            NotificationUtil.error(
                this.getDisplayName(),
                String.format(Language.translate("serverassist.notify.no_second_potion", "Нет второго зелья для разбафа (%s)!"), donateItem2.getDisplayName())
            );
        } else if ((bundleSlot2 != null || bundleSlot3 != null || bundleSlot4 != null) && this.findEmptySlot() == null) {
            NotificationUtil.error(this.getDisplayName(), Language.translate("serverassist.notify.no_empty_slot", "Нет пустого слота для предмета из мешочка!"));
        } else if (this.rebuffSequence.isFinished()) {
            this.rebuffSequence.clear().addDelayed(0, () -> {
                Vector2f vector2f = this.getSmartAimRotation(donateItem);
                if (slot != null) {
                    ThrowComponent.getInstance().useSlotWithRotation(slot, vector2f);
                } else {
                    Slot slot4 = this.findEmptySlot();
                    if (slot4 != null) {
                        ThrowComponent.getInstance().useFromSlot(bundleSlot2.bundleSlot().id, slot4.id, bundleSlot2.indexInBundle(), vector2f);
                    }
                }
            }).addDelayed(250, () -> {
                if (slot2 != null) {
                    ThrowComponent.getInstance().useSlot(slot2);
                } else {
                    Slot slot5 = this.findEmptySlot();
                    if (slot5 != null) {
                        ThrowComponent.getInstance().useFromSlot(bundleSlot3.bundleSlot().id, slot5.id, bundleSlot3.indexInBundle(), null);
                    }
                }
            }).addDelayed(350, () -> {
                Vector2f vector2f2 = this.getSmartAimRotation(donateItem2);
                if (vector2f2 == null) {
                    vector2f2 = new Vector2f(Rotation.getCameraYaw(), 89.0F);
                }

                if (slot3 != null) {
                    ThrowComponent.getInstance().useSlotWithRotation(slot3, vector2f2);
                } else {
                    Slot slot6 = this.findEmptySlot();
                    if (slot6 != null) {
                        ThrowComponent.getInstance().useFromSlot(bundleSlot4.bundleSlot().id, slot6.id, bundleSlot4.indexInBundle(), vector2f2);
                    }
                }
            });
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!ServerManager.isNullWorld()) {
            this.normalizeBinds();
            this.flushExpiredPendingUses();
            this.updateWheelRelease();
            this.antiFlySequence.tick();
            this.rebuffSequence.tick();
            this.updateTraps();
            if (this.isServer("holyworld") && this.holyUtils.has("auto_loot")) {
                this.autoLoot();
            }
        }
    }

    public void normalizeBinds() {
        for (KeybindSetting keybindSetting : this.itemBinds.values()) {
            this.normalizeBind(keybindSetting);
        }

        this.normalizeBind(this.wheelItemsBind);
        this.normalizeBind(this.wheelPotionsBind);
    }

    private void normalizeBind(KeybindSetting keybindSetting) {
        if (keybindSetting.getDefaultValue() != -1) {
            keybindSetting.setValue(Integer.valueOf(-1));
        }

        int i = keybindSetting.getBinds().size();
        if (i > 1) {
            ValueBind valueBind = keybindSetting.getBinds().get(0);
            this.setSingleBind(keybindSetting, valueBind.getKey(), valueBind.getModifiers());
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.pendingStructureBlocks.clear();
        this.detectedTraps.clear();
        this.holyTraps.clear();
        if (this.isServer("funtime") && this.funtimeUtils.has("event_delay")) {
            if ((ServerManager.isFunTime() || ServerManager.isSpookyTime()) && ServerManager.getAnarchyNumber() != -1) {
                mc.getNetworkHandler().sendChatCommand("event delay");
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && mc.player != null && mc.world != null) {
            Packet packet = packetEvent.getPacket();
            if (this.isServer("funtime")) {
                if (packet instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket && this.funtimeUtils.has("structures")) {
                    mc.execute(() -> chunkDeltaUpdateS2CPacket.visitUpdates((blockPos, blockState) -> {
                        if (!blockState.isAir()) {
                            this.pendingStructureBlocks.add(blockPos.toImmutable());
                            this.structureBatchTimer.reset();
                        }
                    }));
                } else {
                    if (packet instanceof GameMessageS2CPacket gameMessageS2CPacket && this.funtimeUtils.has("auto_point")) {
                        Text text = gameMessageS2CPacket.content();
                        mc.execute(() -> this.handleEventMessage(text));
                    }
                }
            } else {
                if (this.isServer("holyworld") && this.holyUtils.has("traps") && packet instanceof PlaySoundS2CPacket playSoundS2CPacket) {
                    mc.execute(() -> this.handleHolyTrapSound(playSoundS2CPacket));
                }
            }
        }
    }

    private void handleEventMessage(Text text) {
        String string = text.getString();
        String string2 = extractBetween(string, "|||   [", "]   ");
        if (string2 != null) {
            String[] strings = extractGpsCoordinates(text);
            if (strings == null) {
                strings = extractTextCoordinates(string);
            }

            if (strings != null) {
                this.addEventPoint(string2.trim(), strings);
            }
        } else {
            if (EVENT_HEADER_PATTERN.matcher(string).matches()) {
                this.pendingEventName = EVENT_PREFIX_PATTERN.matcher(string).replaceAll("").replace(":", "").trim();
            }

            if (string.contains("Координаты:") && this.pendingEventName != null) {
                String string3 = extractBetween(string, "[", "]");
                if (string3 != null) {
                    this.addEventPoint(this.pendingEventName, string3.split(" "));
                    this.pendingEventName = null;
                }
            }
        }
    }

    private static String[] extractGpsCoordinates(Text text2) {
        Optional<String> optionalString = text2.visit((style, string) -> {
            if (style.getClickEvent() instanceof RunCommand runCommand) {
                String string2;
                try {
                    string2 = runCommand.command();
                } catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }

                String string3 = string2;
                if (string3.startsWith("/gps ")) {
                    return Optional.of(string3.substring(5).trim());
                }
            }

            return Optional.empty();
        }, Style.EMPTY);
        return optionalString.<String[]>map(string4 -> string4.split(" ")).orElse(null);
    }

    private static String[] extractTextCoordinates(String string) {
        int i = string.indexOf("координатах");
        if (i < 0) {
            return null;
        }

        Matcher matcher2 = COORDINATES_PATTERN.matcher(string);
        return matcher2.find(i) ? new String[]{matcher2.group(1), matcher2.group(2), matcher2.group(3)} : null;
    }

    private void addEventPoint(String string, String[] strings) {
        boolean flag = SUMMONABLE_EVENTS.contains(string);
        if (this.pointEvents.has(flag ? "summonable" : "regular")) {
            if (strings.length >= 3) {
                try {
                    this.createEventWaypoint(string, Integer.parseInt(strings[0]), Integer.parseInt(strings[1]), Integer.parseInt(strings[2]));
                } catch (NumberFormatException numberFormatException) {
                }
            }
        }
    }

    private void createEventWaypoint(String string, int i, int i2, int i3) {
        FunTimeEventType funTimeEventType = FunTimeEventType.fromMessage(string);
        String string2 = funTimeEventType != null ? funTimeEventType.getDisplayName() : string;
        WaypointRenderer.addTimed(string2, funTimeEventType != null ? funTimeEventType.getIcon() : Icon.FLAG, ServerManager.getAnarchyNumber(), i, i2, i3);
        NotificationUtil.info(
            this.getDisplayName(), String.format(Language.translate("serverassist.notify.event", "Ивент: %s (%d %d %d)"), string2, i, i2, i3)
        );
    }

    private void updateTraps() {
        this.detectedTraps.removeIf(detectedTrap -> detectedTrap.watch().hasPassed((double)detectedTrap.trap().time()));
        this.holyTraps.removeIf(holyTrap -> holyTrap.getTimer().hasPassed((double)holyTrap.getDuration()));
        if (!this.pendingStructureBlocks.isEmpty() && this.structureBatchTimer.hasPassed(25.0)) {
            if (this.pendingStructureBlocks.size() > 50 && this.pendingStructureBlocks.size() < 600) {
                this.detectTrapStructures(this.pendingStructureBlocks);
            }

            this.pendingStructureBlocks.clear();
        }
    }

    private void detectTrapStructures(List<BlockPos> blockPoses) {
        for (BlockPos blockPos : blockPoses) {
            for (TrapType trapType : this.trapTypes) {
                int i = trapType.sizeX() - 1;
                int i2 = trapType.sizeY() - 1;
                int i3 = trapType.sizeZ() - 1;
                int[] ints = new int[]{0, -i};
                int[] ints2 = new int[]{0, -i2};
                int[] ints3 = new int[]{0, -i3};

                for (int i4 : ints) {
                    for (int i5 : ints2) {
                        for (int i6 : ints3) {
                            int i7 = blockPos.getX() + i4;
                            int i8 = blockPos.getY() + i5;
                            int i9 = blockPos.getZ() + i6;
                            int i10 = i7 + i;
                            int i11 = i8 + i2;
                            int i12 = i9 + i3;
                            if (trapType.validator().check(this::isBlockAt, i7, i8, i9, i10, i11, i12)) {
                                this.addDetectedTrap(trapType, trapType.calculateCenter(i7, i8, i9));
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isBlockAt(BlockPos blockPos, Block block) {
        return mc.world != null && mc.world.getBlockState(blockPos).getBlock() == block;
    }

    private void addDetectedTrap(TrapType trapType, BlockPos blockPos) {
        int i = blockPos.getX() - trapType.centerOffsetX();
        int i2 = blockPos.getY() - trapType.centerOffsetY();
        int i3 = blockPos.getZ() - trapType.centerOffsetZ();
        int i4 = i + trapType.sizeX() - 1;
        int i5 = i2 + trapType.sizeY() - 1;
        int i6 = i3 + trapType.sizeZ() - 1;
        int i7 = trapType.sizeX() * trapType.sizeY() * trapType.sizeZ();

        for (int i8 = 0; i8 < this.detectedTraps.size(); i8++) {
            ServerAssist.DetectedTrap detectedTrap = this.detectedTraps.get(i8);
            if (detectedTrap.trap() == trapType && detectedTrap.pos().equals(blockPos)) {
                return;
            }

            int i9 = detectedTrap.pos().getX() - detectedTrap.trap().centerOffsetX();
            int i10 = detectedTrap.pos().getY() - detectedTrap.trap().centerOffsetY();
            int i11 = detectedTrap.pos().getZ() - detectedTrap.trap().centerOffsetZ();
            int i12 = i9 + detectedTrap.trap().sizeX() - 1;
            int i13 = i10 + detectedTrap.trap().sizeY() - 1;
            int i14 = i11 + detectedTrap.trap().sizeZ() - 1;
            int i15 = detectedTrap.trap().sizeX() * detectedTrap.trap().sizeY() * detectedTrap.trap().sizeZ();
            if (i >= i9 && i4 <= i12 && i2 >= i10 && i5 <= i13 && i3 >= i11 && i6 <= i14 && i7 < i15) {
                return;
            }

            if (i9 >= i && i12 <= i4 && i10 >= i2 && i13 <= i5 && i11 >= i3 && i14 <= i6 && i15 < i7) {
                this.detectedTraps.remove(i8);
                i8--;
            }
        }

        trapType.effect().getTimer().reset();
        this.detectedTraps.add(new ServerAssist.DetectedTrap(trapType, blockPos, new StopWatch()));
    }

    private void autoLoot() {
        if (this.autoLootTimer.hasPassed(400.0)) {
            this.autoLootTimer.reset();

            for (MerchantEntity merchantEntity : mc.world.getEntitiesByClass(MerchantEntity.class, mc.player.getBoundingBox().expand(6.0), merchantEntity2 -> true)) {
                if (!merchantEntity.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty() || !merchantEntity.getEquippedStack(EquipmentSlot.OFFHAND).isEmpty()) {
                    Vec3d vec3d = merchantEntity.getBoundingBox().getCenter();
                    this.rotateTo(vec3d);
                    mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(merchantEntity, false, Hand.MAIN_HAND, vec3d));
                    mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(merchantEntity, false, Hand.MAIN_HAND));
                    break;
                }
            }
        }
    }

    private void rotateTo(Vec3d vec3d) {
        float[] floats = RotationUtil.getRotationArrayTo(vec3d);
        RotationComponent.rotateTo(new Rotation(floats[0], floats[1]), 180.0F, 90.0F, 2, 8);
    }

    public Slot findItemSlot(DonateItem donateItem) {
        return DonateItemRegistry.findSlot(donateItem.getServer().resolve(), donateItem);
    }

    private InventoryUtil.BundleSlot findInBundle(DonateItem donateItem) {
        return DonateItemRegistry.getDeepInfo(donateItem.getServer().resolve(), donateItem);
    }

    private Slot findEmptySlot() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                if (i < 9) {
                    return mc.player.currentScreenHandler.getSlot(i + 36);
                }

                return mc.player.currentScreenHandler.getSlot(i);
            }
        }

        return null;
    }

    public int getItemCount(DonateItem donateItem) {
        return DonateItemRegistry.countItems(donateItem.getServer().resolve(), donateItem);
    }

    private static String extractBetween(String string, String string2, String string3) {
        if (string != null && string2 != null && string3 != null) {
            int i = string.indexOf(string2);
            if (i < 0) {
                return null;
            }

            int i2 = i + string2.length();
            int i3 = string.indexOf(string3, i2);
            return i3 > i2 ? string.substring(i2, i3) : null;
        } else {
            return null;
        }
    }

    private void handleHolyTrapSound(PlaySoundS2CPacket playSoundS2CPacket) {
        String string = playSoundS2CPacket.getSound().value().id().getPath();
        if (string.equals("block.beacon.deactivate")) {
            this.holyTraps.add(new HolyTrap("serverassist.holytrap.stun", "Стан", Items.NETHER_STAR, playSoundS2CPacket.getX(), playSoundS2CPacket.getY(), playSoundS2CPacket.getZ(), 30.0, 15000L));
        } else {
            if (string.equals("entity.generic.explode")) {
                this.holyTraps
                    .add(
                        new HolyTrap(
                            "serverassist.holytrap.explosive", "Взрывная трапка", Items.PRISMARINE_SHARD, playSoundS2CPacket.getX(), playSoundS2CPacket.getY(), playSoundS2CPacket.getZ(), 30.0, 15000L
                        )
                    );
            }
        }
    }

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (!ServerManager.isNullWorld()) {
            if (renderEvent.isWorld()) {
                if (this.isServer("holyworld") && this.holyUtils.has("traps")) {
                    this.renderHolyTrapBoxes(renderEvent.getMatrices());
                    return;
                }
            } else if (renderEvent.isHudPre()) {
                this.renderTrapLabels(renderEvent.getContext());
            }
        }
    }

    private void renderTrapLabels(DrawContext drawContext) {
        String string = Language.translate("serverassist.seconds", "сек.");
        if (this.isServer("funtime") && this.funtimeUtils.has("structures")) {
            for (ServerAssist.DetectedTrap detectedTrap : this.detectedTraps) {
                TrapType trapType = detectedTrap.trap();
                double d = roundToTenth((trapType.time() - detectedTrap.watch().getElapsed()) / 1000.0);
                String string2 = trapType.displayName() + " - " + d + " " + string;
                String string3 = null;
                TrapEffect trapEffect = trapType.effect();
                if (trapEffect.getCooldown() > 0.0F) {
                    double d2 = roundToTenth((trapEffect.getCooldown() - (float)trapEffect.getTimer().getElapsed()) / 1000.0);
                    if (d2 > 0.0) {
                        string3 = trapEffect.getDisplayName() + " - " + d2 + " " + string;
                    }
                }

                this.renderWorldLabel(drawContext, Vec3d.ofCenter(detectedTrap.pos()), trapType.item(), string2, string3);
            }
        }

        if (this.isServer("holyworld") && this.holyUtils.has("traps")) {
            for (HolyTrap holyTrap : this.holyTraps) {
                double d3 = roundToTenth((holyTrap.getDuration() - holyTrap.getTimer().getElapsed()) / 1000.0);
                String string4 = holyTrap.getDisplayName() + " - " + d3 + " " + string;
                this.renderWorldLabel(drawContext, holyTrap.getBox().getCenter(), holyTrap.getItem(), string4, null);
            }

            this.renderTrapTimer(drawContext, string);
        }
    }

    private void renderTrapTimer(DrawContext drawContext, String string) {
        HolyTrap holyTrap = this.findActiveHolyTrap();
        if (holyTrap != null) {
            this.displayedTrap = holyTrap;
        }

        this.trapTimerAnimation.update();
        this.trapTimerAnimation.animate(holyTrap != null ? 1.0 : 0.0, 0.25, holyTrap != null ? Easings.QUAD_OUT : Easings.QUAD_IN, true);
        float f = this.trapTimerAnimation.get();
        if (!(f <= 0.01F) && this.displayedTrap != null) {
            MsdfFont msdfFont = Fonts.INTER_SEMIBOLD;
            if (msdfFont != null) {
                double d = Math.max(0.0, roundToTenth((this.displayedTrap.getDuration() - this.displayedTrap.getTimer().getElapsed()) / 1000.0));
                String string2 = d + " " + string;
                float f2 = 9.0F + 9.0F + 6.0F + msdfFont.getWidth(string2, 6.5F) + 9.0F;
                float f3 = (mc.getWindow().getScaledWidth() - f2) / 2.0F;
                float f4 = Math.max(HudPanelUtil.getTopOffset(), NeuroAim.INSTANCE.getIndicatorHeight() + 4.0F);
                float f5 = 4.0F + f4 - (1.0F - f) * 8.0F;
                RenderUtil renderUtil = HudPanelUtil.drawPanel(drawContext, f3, f5, f2, 17.0F, f, 17.0F / 2.4F);
                this.drawItemIconAlpha(drawContext, this.displayedTrap.getItem(), f3 + 9.0F, f5 + (17.0F - 9.0F) / 2.0F, 9.0F, f);
                renderUtil.textMiddleLeft(msdfFont, string2, 9.0F + 9.0F + 6.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f), 6.5F);
            }
        } else {
            if (f <= 0.01F) {
                this.displayedTrap = null;
            }
        }
    }

    private HolyTrap findActiveHolyTrap() {
        if (mc.player == null) {
            return null;
        }

        Box box = mc.player.getBoundingBox();
        HolyTrap holyTrap = null;
        long l = 0L;

        for (HolyTrap holyTrap2 : this.holyTraps) {
            if (holyTrap2.getBox().intersects(box)) {
                long l2 = holyTrap2.getDuration() - holyTrap2.getTimer().getElapsed();
                if (l2 > 0L && l2 > l) {
                    l = l2;
                    holyTrap = holyTrap2;
                }
            }
        }

        return holyTrap;
    }

    private void renderWorldLabel(DrawContext drawContext, Vec3d vec3d, Item item, String string, String string2) {
        Vector2f vector2f = ProjectionUtil.worldToScreen(vec3d.x, vec3d.y, vec3d.z);
        if (vector2f != null) {
            MsdfFont msdfFont = Fonts.INTER_MEDIUM;
            if (msdfFont != null) {
                float f = 3.0F + 9.0F + 2.0F + msdfFont.getWidth(string, 6.5F) + 3.0F;
                float f2 = 9.0F + 3.0F * 2.0F;
                float f3 = vector2f.x - f / 2.0F;
                float f4 = vector2f.y - f2 / 2.0F;
                int i = ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.05F);
                new RenderUtil(drawContext, f3, f4, f, f2)
                    .blur(1.0F, Radius.all(4.0F), RenderUtil.BlurQuality.LOW)
                    .rect(i, 0.67F, Radius.all(4.0F))
                    .textMiddleLeft(msdfFont, string, 3.0F + 9.0F + 2.0F, 0.0F, Theme.getTextColor(), 6.5F);
                this.drawItemIcon(drawContext, item, f3 + 3.0F, f4 + (f2 - 9.0F) / 2.0F, 9.0F);
                if (string2 != null) {
                    float f5 = msdfFont.getWidth(string2, 6.5F) + 3.0F * 2.0F;
                    float f6 = vector2f.x - f5 / 2.0F;
                    float f7 = f4 + f2 + 1.5F;
                    new RenderUtil(drawContext, f6, f7, f5, 6.5F + 3.0F * 2.0F)
                        .blur(1.0F, Radius.all(4.0F), RenderUtil.BlurQuality.LOW)
                        .rect(i, 0.67F, Radius.all(4.0F))
                        .textCenter(msdfFont, string2, Theme.getSecondaryText(), 6.5F);
                }
            }
        }
    }

    private void drawItemIcon(DrawContext drawContext, Item item, float f, float f2, float f3) {
        this.drawItemIconAlpha(drawContext, item, f, f2, f3, 1.0F);
    }

    private void drawItemIconAlpha(DrawContext drawContext, Item item, float f, float f2, float f3, float f4) {
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f, f2, f3 / 16.0F);
        GuiRenderUtil.drawItem(drawContext, new ItemStack(item), f, f2, f4);
        MatrixUtil.pop(drawContext);
    }

    private void renderHolyTrapBoxes(MatrixStack matrixStack) {
        if (!this.holyTraps.isEmpty()) {
            int i = ColorUtil.setAlpha(Theme.getPrimary(), 200);
            int i2 = ColorUtil.darken(ColorUtil.setAlpha(Theme.getPrimary(), 40), 0.5F);

            for (HolyTrap holyTrap : this.holyTraps) {
                Render3DUtil.drawDepthBox(matrixStack, holyTrap.getBox(), i, i2);
            }
        }
    }

    private static double roundToTenth(double d) {
        return Math.round(d * 10.0) / 10.0;
    }

    private void registerTrapTypes() {
        this.trapTypes
            .add(TrapType.corner(Items.NETHERITE_SCRAP, Blocks.COBBLESTONE, "serverassist.trap.regular", "Обычная трапка", TrapEffect.NONE, 15000L, 5, 2));
        this.trapTypes
            .add(TrapType.corner(Items.NETHERITE_SCRAP, Blocks.QUARTZ_BRICKS, "serverassist.trap.nightmare", "Кошмарная трапка", TrapEffect.NONE, 15000L, 5, 2));
        this.trapTypes
            .add(
                TrapType.corner(
                    Items.NETHERITE_SCRAP, Blocks.CHISELED_STONE_BRICKS, "serverassist.trap.sinister", "Зловещая трапка", TrapEffect.NONE, 15000L, 5, 2
                )
            );
        this.trapTypes
            .add(TrapType.corner(Items.NETHERITE_SCRAP, Blocks.DARK_PRISMARINE, "serverassist.trap.pearl", "Жемчужная трапка", TrapEffect.NONE, 15000L, 5, 2));
        this.trapTypes
            .add(
                TrapType.corner(
                    Items.NETHERITE_SCRAP, Blocks.SMOOTH_QUARTZ_SLAB, "serverassist.trap.icy", "Ледяная трапка", TrapEffect.REGENERATION, 15000L, 5, 2
                )
            );
        this.trapTypes
            .add(TrapType.corner(Items.NETHERITE_SCRAP, Blocks.RESPAWN_ANCHOR, "serverassist.trap.cursed", "Проклятая трапка", TrapEffect.NONE, 15000L, 5, 2));
        this.trapTypes
            .add(
                TrapType.corner(
                    Items.NETHERITE_SCRAP, Blocks.CHISELED_QUARTZ_BLOCK, "serverassist.trap.ghostly", "Призрачная трапка", TrapEffect.GLOW, 15000L, 5, 2
                )
            );
        this.trapTypes
            .add(
                TrapType.corner(
                    Items.NETHERITE_SCRAP, Blocks.DEAD_TUBE_CORAL_BLOCK, "serverassist.trap.inevitable", "Неизбежная трапка", TrapEffect.HORUS, 15000L, 5, 2
                )
            );
        this.trapTypes
            .add(
                TrapType.corner(
                    Items.NETHERITE_SCRAP, Blocks.COAL_BLOCK, "serverassist.trap.singular", "Сингулярная трапка", TrapEffect.BLINDNESS, 15000L, 5, 2
                )
            );
        this.trapTypes
            .add(
                TrapType.dual(
                    Items.NETHERITE_SCRAP,
                    Blocks.PURPUR_PILLAR,
                    Blocks.PURPUR_BLOCK,
                    "serverassist.trap.mystic",
                    "Мистическая трапка",
                    TrapEffect.PEARL,
                    15000L
                )
            );
        this.trapTypes
            .add(
                TrapType.dual(
                    Items.NETHERITE_SCRAP,
                    Blocks.MAGMA_BLOCK,
                    Blocks.RED_NETHER_BRICKS,
                    "serverassist.trap.deadly",
                    "Смертельная трапка",
                    TrapEffect.NONE,
                    15000L
                )
            );
        this.trapTypes
            .add(
                TrapType.dual(
                    Items.NETHERITE_SCRAP,
                    Blocks.CHISELED_NETHER_BRICKS,
                    Blocks.NETHER_BRICKS,
                    "serverassist.trap.insidious",
                    "Коварная трапка",
                    TrapEffect.NONE,
                    15000L
                )
            );
        this.trapTypes
            .add(
                TrapType.dual(
                    Items.NETHERITE_SCRAP,
                    Blocks.NETHERITE_BLOCK,
                    Blocks.POLISHED_BLACKSTONE_BRICKS,
                    "serverassist.trap.tormenting",
                    "Мучительная трапка",
                    TrapEffect.WITHERING,
                    15000L
                )
            );
        this.trapTypes
            .add(
                new TrapType(
                    Items.NETHERITE_SCRAP,
                    "serverassist.trap.brutal",
                    "Жестокая трапка",
                    TrapEffect.NONE,
                    15000L,
                    5,
                    5,
                    5,
                    2,
                    2,
                    2,
                    (blockMatcher, i, i2, i3, i4, i5, i6) -> TrapType.checkAllCorners(blockMatcher, Blocks.NETHERITE_BLOCK, i, i2, i3, i4, i5, i6)
                        && !blockMatcher.isValid(new BlockPos(i, i2 + 2, i3), Blocks.DIORITE_WALL)
                        && blockMatcher.isValid(new BlockPos(i + 2, i5, i3 + 2), Blocks.GILDED_BLACKSTONE)
                )
            );
        this.trapTypes
            .add(
                new TrapType(
                    Items.NETHERITE_SCRAP,
                    "serverassist.trap.royal",
                    "Царская трапка",
                    TrapEffect.RUSH,
                    15000L,
                    5,
                    5,
                    5,
                    2,
                    2,
                    2,
                    (blockMatcher2, i7, i8, i9, i10, i11, i12) -> TrapType.checkAllCorners(blockMatcher2, Blocks.NETHERITE_BLOCK, i7, i8, i9, i10, i11, i12)
                        && blockMatcher2.isValid(new BlockPos(i7, i8 + 2, i9), Blocks.DIORITE_WALL)
                        && blockMatcher2.isValid(new BlockPos(i7, i8 + 2, i12), Blocks.DIORITE_WALL)
                        && blockMatcher2.isValid(new BlockPos(i10, i8 + 2, i9), Blocks.DIORITE_WALL)
                        && blockMatcher2.isValid(new BlockPos(i10, i8 + 2, i12), Blocks.DIORITE_WALL)
                )
            );
        this.trapTypes
            .add(TrapType.corner(Items.DRAGON_HEAD, Blocks.LODESTONE, "serverassist.trap.dragon", "Драконья трапка", TrapEffect.HORUS_DRAGON, 30000L, 7, 3));
        this.trapTypes
            .add(TrapType.plast(Items.DRIED_KELP, Blocks.COBBLESTONE, "serverassist.plast.regular", "Обычный пласт", TrapEffect.NONE, 60000L, 5, 2, 5, 2, 0, 2));
        this.trapTypes
            .add(TrapType.plast(Items.DRIED_KELP, Blocks.COBBLESTONE, "serverassist.plast.regular", "Обычный пласт", TrapEffect.NONE, 20000L, 5, 5, 2, 2, 2, 0));
        this.trapTypes
            .add(TrapType.plast(Items.DRIED_KELP, Blocks.COBBLESTONE, "serverassist.plast.regular", "Обычный пласт", TrapEffect.NONE, 20000L, 2, 5, 5, 0, 2, 2));
        this.trapTypes
            .add(TrapType.plast(Items.DRIED_KELP, Blocks.LODESTONE, "serverassist.plast.dragon", "Драконий пласт", TrapEffect.NONE, 60000L, 7, 2, 7, 3, 0, 3));
        this.trapTypes
            .add(TrapType.plast(Items.DRIED_KELP, Blocks.LODESTONE, "serverassist.plast.dragon", "Драконий пласт", TrapEffect.NONE, 20000L, 2, 7, 7, 0, 3, 3));
        this.trapTypes
            .add(TrapType.plast(Items.DRIED_KELP, Blocks.LODESTONE, "serverassist.plast.dragon", "Драконий пласт", TrapEffect.NONE, 20000L, 7, 7, 2, 3, 3, 0));
    }

    public ModeSetting getServer() {
        return this.server;
    }

    public MultiSetting getFuntimeUtils() {
        return this.funtimeUtils;
    }

    public MultiSetting getPointEvents() {
        return this.pointEvents;
    }

    public MultiSetting getHolyUtils() {
        return this.holyUtils;
    }

    public GroupSetting getUse() {
        return this.use;
    }

    public Map<String, BooleanSetting> getWheelEnabledByServer() {
        return this.wheelEnabledByServer;
    }

    public KeybindSetting getWheelItemsBind() {
        return this.wheelItemsBind;
    }

    public KeybindSetting getWheelPotionsBind() {
        return this.wheelPotionsBind;
    }

    public BooleanSetting getUseFromBundle() {
        return this.useFromBundle;
    }

    public Map<DonateItem, KeybindSetting> getItemBinds() {
        return this.itemBinds;
    }

    public Map<DonateItem, BooleanSetting> getWheelItemToggles() {
        return this.wheelItemToggles;
    }

    public Map<DonateItem, BooleanSetting> getSyncHeadToggles() {
        return this.syncHeadToggles;
    }

    public Map<DonateItem, BooleanSetting> getSmartAimToggles() {
        return this.smartAimToggles;
    }

    public RadialMenu getItemWheel() {
        return this.itemWheel;
    }

    public List<DonateItem> getWheelEntries() {
        return this.wheelEntries;
    }

    public Map<DonateItem, ItemStack> getWheelIconStacks() {
        return this.wheelIconStacks;
    }

    public StopWatch getWheelOpenTimer() {
        return this.wheelOpenTimer;
    }

    public ValueBind<Integer> getWheelOpenBind() {
        return this.wheelOpenBind;
    }

    public boolean isWheelReleasedEarly() {
        return this.wheelReleasedEarly;
    }

    public List<ServerAssist.PendingItemUse> getPendingUses() {
        return this.pendingUses;
    }

    public ModeSetting getRebuffFirst() {
        return this.rebuffFirst;
    }

    public ModeSetting getRebuffSecond() {
        return this.rebuffSecond;
    }

    public ActionSequence getAntiFlySequence() {
        return this.antiFlySequence;
    }

    public StopWatch getAutoLootTimer() {
        return this.autoLootTimer;
    }

    public List<TrapType> getTrapTypes() {
        return this.trapTypes;
    }

    public List<ServerAssist.DetectedTrap> getDetectedTraps() {
        return this.detectedTraps;
    }

    public List<HolyTrap> getHolyTraps() {
        return this.holyTraps;
    }

    public List<BlockPos> getPendingStructureBlocks() {
        return this.pendingStructureBlocks;
    }

    public StopWatch getStructureBatchTimer() {
        return this.structureBatchTimer;
    }

    public ActionSequence getRebuffSequence() {
        return this.rebuffSequence;
    }

    public String getPendingEventName() {
        return this.pendingEventName;
    }

    public StopWatch getLastUseTimer() {
        return this.lastUseTimer;
    }

    public Animation getTrapTimerAnimation() {
        return this.trapTimerAnimation;
    }

    public HolyTrap getDisplayedTrap() {
        return this.displayedTrap;
    }

    record DetectedTrap(TrapType trap, BlockPos pos, StopWatch watch) {
    }

    record PendingItemUse(DonateItem item, int key, StopWatch watch) {
    }
}
