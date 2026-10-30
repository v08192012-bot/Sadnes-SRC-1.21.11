package tech.sadnes.client.feature.impl.combat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.ScreenMouseScrollEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.event.impl.UseItemEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.donut.DonutRegion;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.ui.drag.DragManager;
import tech.sadnes.client.ui.hud.Draggable;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "CPVP Helper",
    description = "feature.impl.combat.cpvphelper",
    displayName = "feature.impl.combat.cpvphelper",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_TWEAKS
)
public final class CpvpHelper extends Feature implements Draggable {
    private static final float COUNTER_SIZE = 22.0F;
    private static final float COUNTER_TEXT_SIZE = 10.0F;
    private static final float RING_SIZE = 10.5F;
    private static final float NAME_TEXT_SIZE = 6.5F;
    private static final float POPS_TEXT_SIZE = 5.0F;
    private static final float TABLE_PADDING = 4.0F;
    private static final float DURABILITY_PER_BOTTLE = 14.0F;
    private static final float TABLE_SPACING = 3.0F;
    private static final float TABLE_INNER_PADDING = 4.0F;
    private static final float TABLE_HEADER_HEIGHT = 8.0F;
    private static final float TABLE_MIN_HEIGHT = 5.0F;
    private static final float TITLE_TEXT_SIZE = 6.0F;
    private static final ItemStack TOTEM_STACK = Items.TOTEM_OF_UNDYING.getDefaultStack();
    private static final ItemStack EXP_BOTTLE_STACK = Items.EXPERIENCE_BOTTLE.getDefaultStack();
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final MultiSetting options = new MultiSetting("cpvphelper.options", BooleanSetting.of("pops_table", true), BooleanSetting.of("totem_count", true));
    private final MultiSetting experience = new MultiSetting(
        "cpvphelper.experience", BooleanSetting.of("exp_count", true), BooleanSetting.of("exp_guard", true)
    );
    private final BooleanSetting onlyPastTargets = new BooleanSetting("cpvphelper.onlyPastTargets", false).setVisibleWhen(() -> this.options.has("pops_table"));
    private final NumberSetting scale = new NumberSetting("cpvphelper.scale", 100.0F, 50.0F, 300.0F, 5.0F, Unit.PERCENT).setVisibleWhen(() -> false);
    private final DragSetting totemDraggable;
    private final DragSetting tableDraggable;
    private final DragSetting expDraggable;
    private final Map<String, Identifier> dimmedTextureCache = new HashMap<>();
    private final ArrayDeque<Long> expThrowTimes = new ArrayDeque<>();
    private final Map<UUID, CpvpHelper.PopEntry> popEntries = new HashMap<>();
    private final Set<UUID> pastTargets = new HashSet<>();
    private final List<CpvpHelper.PopEntry> visibleEntries = new ArrayList<>();
    private final Animation scaleAnimation = new Animation();
    private final Animation tableAlphaAnimation = new Animation();
    private final Animation tableWidthAnimation = new Animation();
    private final Animation tableHeightAnimation = new Animation();
    private final boolean previewMode = false;
    private UUID lastTargetUuid;
    private boolean totemCounterVisible;
    private boolean expCounterVisible;
    private boolean tableVisible;
    private int totemCount;
    private int expNeeded;

    public CpvpHelper() {
        this.totemDraggable = new DragSetting(
            this.getInfo().name(),
            () -> mc.getWindow() == null ? 0.0F : GuiScale.getScaledWidthInt() / 2.0F + 15.0F,
            () -> mc.getWindow() == null ? 0.0F : GuiScale.getScaledHeightInt() / 2.0F - 11.0F
        );
        if (mc.getWindow() == null) {
            this.totemDraggable.setPositionInitialized(false);
        }

        this.totemDraggable.setSize(22.0F, 22.0F);
        this.totemDraggable.onInput((drawContext, i, i2, f) -> this.renderTotemCounter(drawContext));
        this.totemDraggable.onInput(this);
        this.totemDraggable.setDraggable(() -> this.totemCounterVisible);
        this.totemDraggable.setRenderCondition(() -> this.totemCounterVisible);
        DragComponent.register(this.totemDraggable);
        this.tableDraggable = new DragSetting(
            this.getInfo().name() + " Table", () -> 6.0F, () -> mc.getWindow() == null ? 0.0F : GuiScale.getScaledHeightInt() / 2.0F - 20.0F
        );
        if (mc.getWindow() == null) {
            this.tableDraggable.setPositionInitialized(false);
        }

        this.tableDraggable.onInput((drawContext2, i3, i4, f2) -> this.renderPopsTable(drawContext2));
        this.tableDraggable.onInput(this);
        this.tableDraggable.setDraggable(() -> this.tableVisible);
        this.tableDraggable.setRenderCondition(() -> this.tableVisible);
        DragComponent.register(this.tableDraggable);
        this.expDraggable = new DragSetting(
            this.getInfo().name() + " Exp",
            () -> mc.getWindow() == null ? 0.0F : GuiScale.getScaledWidthInt() / 2.0F - 15.0F - 22.0F,
            () -> mc.getWindow() == null ? 0.0F : GuiScale.getScaledHeightInt() / 2.0F - 11.0F
        );
        if (mc.getWindow() == null) {
            this.expDraggable.setPositionInitialized(false);
        }

        this.expDraggable.setSize(22.0F, 22.0F);
        this.expDraggable.onInput((drawContext3, i5, i6, f3) -> this.renderExpCounter(drawContext3));
        this.expDraggable.onInput(this);
        this.expDraggable.setDraggable(() -> this.expCounterVisible);
        this.expDraggable.setRenderCondition(() -> this.expCounterVisible);
        DragComponent.register(this.expDraggable);
    }

    public static CpvpHelper getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(CpvpHelper.class);
    }

    public DragSetting getTotemDraggable() {
        return this.totemDraggable;
    }

    @Override
    public void onEnable() {
        this.popEntries.clear();
        this.pastTargets.clear();
        this.lastTargetUuid = null;
        super.onEnable();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.expThrowTimes.clear();
        this.popEntries.clear();
        this.pastTargets.clear();
        this.lastTargetUuid = null;
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player != null && mc.world != null) {
            this.totemCount = InventoryUtil.countItem(Items.TOTEM_OF_UNDYING);
            this.expNeeded = this.computeBottlesNeeded();
            long l = System.currentTimeMillis();

            for (Entry entry : this.popEntries.entrySet()) {
                PlayerEntity playerEntity = mc.world.getPlayerByUuid((UUID)entry.getKey());
                CpvpHelper.PopEntry popEntry = (CpvpHelper.PopEntry)entry.getValue();
                boolean flag = playerEntity != null;
                if (popEntry.present && !flag) {
                    popEntry.lastSeenTime = l;
                }

                popEntry.present = flag;
                if (flag) {
                    popEntry.setPosition(playerEntity.getX(), playerEntity.getZ());
                }
            }

            this.popEntries.values().removeIf(popEntry2 -> !popEntry2.present && l - popEntry2.lastSeenTime > 5000L);
            if (this.options.has("pops_table")) {
                if (!this.onlyPastTargets.getValue()) {
                    return;
                }

                AttackAura attackAura = AttackAura.getInstance();
                if (attackAura != null && attackAura.isEnabled()) {
                    this.rememberTarget(attackAura.getCurrentTarget());
                }

                AutoCrystal autoCrystal = AutoCrystal.getInstance();
                if (autoCrystal != null && autoCrystal.isEnabled()) {
                    this.rememberTarget(autoCrystal.getActiveTarget());
                }

                TriggerBot triggerBot = TriggerBot.getInstance();
                if (triggerBot != null && triggerBot.isEnabled()) {
                    this.rememberTarget(triggerBot.findTarget());
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onMouseScroll(ScreenMouseScrollEvent screenMouseScrollEvent) {
        float f = (float)screenMouseScrollEvent.getMouseX();
        float f2 = (float)screenMouseScrollEvent.getMouseY();
        boolean flag;
        if (this.totemCounterVisible) {
            flag = this.isHovered(this.totemDraggable, f, f2);
        } else {
            flag = false;
        }

        boolean flag2;
        if (this.expCounterVisible) {
            flag2 = this.isHovered(this.expDraggable, f, f2);
        } else {
            flag2 = false;
        }

        boolean flag3;
        label35: {
            boolean flag4;
            if (mc.currentScreen instanceof ChatScreen) {
                if (!flag) {
                    flag3 = flag2;
                    break label35;
                }

                flag4 = true;
            } else {
                flag4 = false;
            }

            flag3 = flag4;
        }

        if (flag3 && screenMouseScrollEvent.getVerticalAmount() != 0.0) {
            float f3 = screenMouseScrollEvent.getVerticalAmount() > 0.0 ? 10.0F : -10.0F;
            this.scale.setValue(Float.valueOf(MathHelper.clamp(this.scale.getValue() + f3, this.scale.getMin(), this.scale.getMax())));
            screenMouseScrollEvent.cancel();
        }
    }

    @EventHandler
    public void onUseItem(UseItemEvent useItemEvent) {
        if (useItemEvent.getStage() == UseItemEvent.Stage.START && useItemEvent.getHand() != null && mc.player != null && this.experience.has("exp_guard")) {
            if (mc.player.getStackInHand(useItemEvent.getHand()).isOf(Items.EXPERIENCE_BOTTLE)) {
                long l = System.currentTimeMillis();

                while (!this.expThrowTimes.isEmpty() && l - this.expThrowTimes.peekFirst() > 1500L) {
                    this.expThrowTimes.pollFirst();
                }

                boolean flag = this.computeBottlesNeededWithOtherHand(useItemEvent.getHand()) <= this.expThrowTimes.size();
                if (flag) {
                    useItemEvent.cancel();
                } else {
                    this.expThrowTimes.addLast(l);
                }
            }
        }
    }

    private boolean isHovered(DragSetting dragSetting, float f, float f2) {
        return f >= dragSetting.position2.x && f <= dragSetting.position2.x + dragSetting.size2.x && f2 >= dragSetting.position2.y && f2 <= dragSetting.position2.y + dragSetting.size2.y;
    }

    private int computeBottlesNeeded() {
        return MathHelper.ceil(this.getArmorMendingDamage() / 14.0F);
    }

    private int computeBottlesNeededWithOtherHand(Hand hand) {
        ItemStack itemStack = mc.player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
        int i = isDamagedWithMending(itemStack) ? itemStack.getDamage() : 0;
        return MathHelper.ceil((this.getArmorMendingDamage() + i) / 14.0F);
    }

    private int getArmorMendingDamage() {
        int i = 0;

        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            ItemStack itemStack = mc.player.getEquippedStack(equipmentSlot);
            if (isDamagedWithMending(itemStack)) {
                i += itemStack.getDamage();
            }
        }

        return i;
    }

    private static boolean isDamagedWithMending(ItemStack itemStack) {
        return itemStack.isDamaged() && EnchantmentHelper.hasAnyEnchantmentsWith(itemStack, EnchantmentEffectComponentTypes.REPAIR_WITH_XP);
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && packetEvent.getPacket() instanceof EntityStatusS2CPacket entityStatusS2CPacket) {
            boolean flag = entityStatusS2CPacket.getStatus() == 35;
            if (entityStatusS2CPacket.getStatus() == 3 || flag) {
                mc.execute(() -> this.handleEntityStatus(entityStatusS2CPacket, flag));
            }
        }
    }

    private void handleEntityStatus(EntityStatusS2CPacket entityStatusS2CPacket, boolean flag) {
        if (mc.world != null && mc.player != null && this.options.has("pops_table") && entityStatusS2CPacket.getEntity(mc.world) instanceof PlayerEntity playerEntity) {
            if (playerEntity == mc.player) {
                return;
            }

            UUID uuid = playerEntity.getUuid();
            if (!flag) {
                this.popEntries.remove(uuid);
                return;
            }

            String string = playerEntity.getGameProfile().name();
            boolean flag2 = Sadnes.getInstance().getFriendManager().isFriend(string);
            if (this.onlyPastTargets.getValue() && !this.pastTargets.contains(uuid)) {
                return;
            }

            if (!flag2) {
                CpvpHelper.PopEntry popEntry = this.popEntries.computeIfAbsent(uuid, uuid2 -> new CpvpHelper.PopEntry(string));
                popEntry.pops++;
                popEntry.setPosition(playerEntity.getX(), playerEntity.getZ());
                return;
            }
        }
    }

    private void addPreviewEntries() {
        String[] strings = new String[]{"elytrabobik", "yeekhira", "fuflik"};

        for (int i = 0; i < 3; i++) {
            UUID uuid = new UUID(0L, i);
            if (!this.popEntries.containsKey(uuid)) {
                CpvpHelper.PopEntry popEntry = new CpvpHelper.PopEntry(strings[i]);
                popEntry.pops = 12 - i * 4;
                popEntry.setPosition(mc.player.getX() + 30.0 - i * 40, mc.player.getZ() + i * 40);
                this.popEntries.put(uuid, popEntry);
            }
        }
    }

    private void rememberTarget(LivingEntity livingEntity) {
        if (livingEntity instanceof PlayerEntity playerEntity) {
            this.pastTargets.add(playerEntity.getUuid());
            this.lastTargetUuid = playerEntity.getUuid();
        }
    }

    @Override
    public void render(DrawContext drawContext) {
        this.renderTotemCounter(drawContext);
        this.renderExpCounter(drawContext);
        this.renderPopsTable(drawContext);
    }

    private void renderTotemCounter(DrawContext drawContext) {
        this.totemCounterVisible = this.isEnabled() && this.options.has("totem_count") && mc.player != null && !mc.options.hudHidden;
        if (this.totemCounterVisible) {
            this.renderItemCounter(drawContext, this.totemDraggable, TOTEM_STACK, "totem_of_undying", this.totemCount);
        }
    }

    private void renderExpCounter(DrawContext drawContext) {
        boolean flag = DragManager.isEditScreen(mc.currentScreen);
        this.expCounterVisible = this.isEnabled()
            && this.experience.has("exp_count")
            && mc.player != null
            && !mc.options.hudHidden
            && (this.expNeeded > 0 || flag);
        if (this.expCounterVisible) {
            this.renderItemCounter(drawContext, this.expDraggable, EXP_BOTTLE_STACK, "experience_bottle", this.expNeeded);
        }
    }

    private void renderItemCounter(DrawContext drawContext, DragSetting dragSetting, ItemStack itemStack, String string, int i) {
        if (!dragSetting.isPositionInitialized()) {
            dragSetting.resetToDefault();
        }

        float f = dragSetting.position2.x;
        float f2 = dragSetting.position2.y;
        if (this.scaleAnimation.get() <= 0.0F) {
            this.scaleAnimation.forceValue(this.scale.getValue() / 100.0F);
        }

        this.scaleAnimation.update();
        this.scaleAnimation.animate(this.scale.getValue() / 100.0F, 0.25, Easings.QUART_OUT);
        float f3 = this.scaleAnimation.get();
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f, f2, f3);
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f, f2, 1.375F);
        Identifier identifier = this.getDimmedTexture(string);
        if (identifier != null) {
            drawContext.getMatrices().translate(f, f2);
            drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, identifier, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        } else {
            GuiRenderUtil.drawItem(drawContext, itemStack, f, f2, 1.0F);
        }

        MatrixUtil.pop(drawContext);
        new RenderUtil(drawContext, f, f2, 22.0F, 22.0F).textCenter(BODY_FONT, Integer.toString(i), Theme.getTextColor(), 10.0F, true);
        MatrixUtil.pop(drawContext);
        dragSetting.setSize(22.0F * f3, 22.0F * f3);
    }

    private Identifier getDimmedTexture(String string) {
        if (this.dimmedTextureCache.containsKey(string)) {
            return this.dimmedTextureCache.get(string);
        }

        Identifier identifier = Identifier.of("sadnes", "cpvphelper/dim_" + string);

        try (InputStream inputStream = mc.getResourceManager().open(Identifier.ofVanilla("textures/item/" + string + ".png"))) {
            NativeImage nativeImage = NativeImage.read(inputStream);
            int i = nativeImage.getWidth();
            int i2 = nativeImage.getHeight();

            for (int i3 = 0; i3 < i2; i3++) {
                for (int i4 = 0; i4 < i; i4++) {
                    int i5 = nativeImage.getColorArgb(i4, i3);
                    int i6 = ColorUtil.red(i5);
                    int i7 = ColorUtil.green(i5);
                    int i8 = ColorUtil.blue(i5);
                    float f = i6 * 0.299F + i7 * 0.587F + i8 * 0.114F;
                    int i9 = Math.round((f + (i6 - f) * 0.4F) * 0.6F);
                    int i10 = Math.round((f + (i7 - f) * 0.4F) * 0.6F);
                    int i11 = Math.round((f + (i8 - f) * 0.4F) * 0.6F);
                    nativeImage.setColorArgb(i4, i3, ColorUtil.rgba(i9, i10, i11, ColorUtil.alpha(i5)));
                }
            }

            mc.getTextureManager().registerTexture(identifier, new NativeImageBackedTexture(identifier::toString, nativeImage));
        } catch (IOException ioException) {
            identifier = null;
        }

        this.dimmedTextureCache.put(string, identifier);
        return identifier;
    }

    private void renderPopsTable(DrawContext drawContext) {
        this.tableVisible = false;
        if (this.isEnabled() && this.options.has("pops_table") && mc.player != null && !mc.options.hudHidden) {
            if (!this.tableDraggable.isPositionInitialized()) {
                this.tableDraggable.resetToDefault();
            }

            this.visibleEntries.clear();
            if (this.onlyPastTargets.getValue()) {
                if (this.lastTargetUuid != null) {
                    CpvpHelper.PopEntry popEntry = this.popEntries.get(this.lastTargetUuid);
                    if (popEntry != null) {
                        this.visibleEntries.add(popEntry);
                    }
                }
            } else {
                this.visibleEntries.addAll(this.popEntries.values());
                this.visibleEntries.sort(Comparator.<CpvpHelper.PopEntry>comparingInt(popEntry2 -> popEntry2.pops).reversed());
                if (this.visibleEntries.size() > 3) {
                    this.visibleEntries.subList(3, this.visibleEntries.size()).clear();
                }
            }

            boolean flag = DragManager.isEditScreen(mc.currentScreen);
            boolean flag2 = ServerManager.isDonut();
            String string = Language.translate("cpvphelper.table.title", "Попы тотемов");
            String string2 = flag2 ? Language.translateOption("cpvphelper.region", DonutRegion.getCurrent().getDisplayName()) : "";
            float f = flag2 ? 13.0F : 0.0F;
            float f2 = flag2 ? 9.0F + BODY_FONT.getWidth(string2, 6.0F) : 0.0F;
            float f3 = 0.0F;
            float f4 = 8.0F + f - 3.0F;

            for (CpvpHelper.PopEntry popEntry3 : this.visibleEntries) {
                f3 = Math.max(f3, Math.max(BODY_FONT.getWidth(popEntry3.name, 6.5F), BODY_FONT.getWidth(formatPops(popEntry3), 5.0F)));
                popEntry3.appearAnimation.update();
                popEntry3.appearAnimation.animate(1.0, 0.4, Easings.QUART_OUT);
                f4 += 17.0F * popEntry3.appearAnimation.get();
            }

            float f5 = this.visibleEntries.isEmpty() ? BODY_FONT.getWidth(string, 6.0F) : 14.5F + f3;
            float f6 = 8.0F + Math.max(f5, f2);
            if (this.visibleEntries.isEmpty()) {
                f4 = 8.0F + f + 10.0F;
            }

            this.tableAlphaAnimation.update();
            this.tableAlphaAnimation.animate(this.visibleEntries.isEmpty() && !flag ? 0.0 : 1.0, 0.25, Easings.QUART_OUT);
            if (this.tableWidthAnimation.get() <= 0.0F) {
                this.tableWidthAnimation.forceValue(f6);
            }

            this.tableWidthAnimation.update();
            this.tableWidthAnimation.animate(f6, 0.4, Easings.QUART_OUT);
            if (this.tableHeightAnimation.get() <= 0.0F) {
                this.tableHeightAnimation.forceValue(f4);
            }

            this.tableHeightAnimation.update();
            this.tableHeightAnimation.animate(f4, 0.4, Easings.QUART_OUT);
            float f7 = this.tableWidthAnimation.get();
            float f8 = this.tableHeightAnimation.get();
            this.tableDraggable.setSize(f7, f8);
            float f9 = this.tableAlphaAnimation.get();
            this.tableVisible = f9 > 0.001F;
            if (this.tableVisible) {
                float f10 = this.tableDraggable.position2.x;
                float f11 = this.tableDraggable.position2.y;
                HudPanelUtil.drawPanel(drawContext, f10, f11, f7, f8, f9, 4.5F);
                if (ScissorStack.push(drawContext, f10, f11, f7, f8)) {
                    try {
                        if (flag2) {
                            int i = Theme.withAlpha(Theme.getSecondaryText(), f9);
                            RenderUtil renderUtil = new RenderUtil(drawContext, f10 + 4.0F, f11 + 4.0F, f7, 8.0F);
                            renderUtil.icon(Icon.GLOBE, 0.0F, 1.0F, 6.0F, i);
                            renderUtil.textMiddleLeft(BODY_FONT, string2, 9.0F, 0.0F, i, 6.0F);
                            new RenderUtil(drawContext, f10 + 4.0F, f11 + 4.0F + 8.0F + 2.5F - 0.25F, f7 - 8.0F, 0.5F)
                                .rect(Theme.withAlpha(Theme.getSecondaryText(), f9 * 0.15F), Radius.zero());
                        }

                        float f12 = f11 + 4.0F + f;
                        if (!this.visibleEntries.isEmpty()) {
                            this.renderTableEntries(drawContext, f10, f12, f7, f9, f3);
                            return;
                        }

                        new RenderUtil(drawContext, f10 + 4.0F, f12, f7, 10.0F)
                            .textMiddleLeft(BODY_FONT, string, 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), f9 * 0.6F), 6.0F);
                    } finally {
                        ScissorStack.pop(drawContext);
                    }
                }
            }
        }
    }

    private void renderTableEntries(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        Vec3d vec3d = mc.gameRenderer.getCamera().getCameraPos();
        double d = Math.toRadians(mc.gameRenderer.getCamera().getYaw());
        double d2 = Math.cos(d);
        double d3 = Math.sin(d);
        long l = System.currentTimeMillis();

        for (CpvpHelper.PopEntry popEntry : this.visibleEntries) {
            float f6 = popEntry.present ? 0.0F : MathHelper.clamp((float)(l - popEntry.lastSeenTime) / 5000.0F, 0.0F, 1.0F);
            float f7 = popEntry.appearAnimation.get() * f4 * (1.0F - 0.6F * f6);
            double d4 = popEntry.x - vec3d.x;
            double d5 = popEntry.z - vec3d.z;
            float f8 = (float)Math.toDegrees(Math.atan2(-(d5 * d2 - d4 * d3), -(d4 * d2 + d5 * d3)));
            new RenderUtil(drawContext, f + 4.0F, f2 + 1.75F, 10.5F, 10.5F).progressRing(270.0F - f8, 55.0F, Theme.getPrimary(), f7);
            new RenderUtil(drawContext, f, f2, f3, 7.0F + 1.0F).textMiddleLeft(BODY_FONT, popEntry.name, 18.5F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f7), 6.5F);
            new RenderUtil(drawContext, f, f2 + 7.0F + 1.0F, f3, 7.0F - 1.0F)
                .textMiddleLeft(BODY_FONT, formatPops(popEntry), 18.5F, 0.0F, Theme.withAlpha(Theme.getPrimary(), f7), 5.0F);
            if (!popEntry.present) {
                new RenderUtil(drawContext, f + 18.5F, f2 + 14.0F, f5 * (1.0F - f6), 1.0F)
                    .rect(Theme.withAlpha(Theme.getPrimary(), popEntry.appearAnimation.get() * f4), Radius.all(0.5F));
            }

            f2 += 17.0F * popEntry.appearAnimation.get();
        }
    }

    private static String formatPops(CpvpHelper.PopEntry popEntry) {
        return "x" + popEntry.pops;
    }

    static final class PopEntry {
        final Animation appearAnimation = new Animation();
        final String name;
        int pops;
        boolean present = true;
        long lastSeenTime;
        double x;
        double z;

        PopEntry(String string) {
            this.name = string;
        }

        void setPosition(double d, double d2) {
            this.x = d;
            this.z = d2;
        }
    }
}
