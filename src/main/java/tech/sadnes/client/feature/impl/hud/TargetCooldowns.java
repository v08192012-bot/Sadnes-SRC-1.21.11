package tech.sadnes.client.feature.impl.hud;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.ItemCooldownTrackerComponent;
import tech.sadnes.client.feature.impl.combat.AimAssist;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.TriggerBot;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.server.CooldownItem;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.ui.hud.Draggable;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

@FeatureInfo(
    name = "TargetCooldowns",
    description = "feature.impl.hud.targetcooldowns",
    displayName = "feature.impl.hud.targetcooldowns",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_STATUS
)
public class TargetCooldowns extends Feature implements Draggable {
    private static final float ROW_HEIGHT = 17.0F;
    private static final float SPACING2 = 3.0F;
    private static final float RING_SIZE = 12.0F;
    private static final float RING_THICKNESS = 1.5F;
    private static final float ITEM_SIZE = 8.0F;
    private static final float RING_PADDING = 2.5F;
    private static final float TEXT_GAP = 4.5F;
    private static final float RIGHT_PADDING = 6.5F;
    private static final float VALUE_FONT_SIZE = 7.0F;
    private static final float UNIT_FONT_SIZE = 5.0F;
    private static final float UNIT_GAP = 0.5F;
    private static final long TARGET_KEEP_MS = 700L;
    private static final long SPIN_PERIOD_MS = 1600L;
    private final ModeSetting orientation = new ModeSetting("targetcooldowns.orientation", new String[]{"vertical", "horizontal"});
    private final ModeSetting durations = new ModeSetting("targetcooldowns.durations", new String[]{"auto", "custom"});
    private final NumberSetting apple = new NumberSetting("targetcooldowns.apple", 30.0F, 1.0F, 300.0F, 1.0F, Unit.SECOND)
        .setVisibleWhen(() -> this.durations.is("custom"));
    private final NumberSetting enchantedApple = new NumberSetting("targetcooldowns.enchantedApple", 150.0F, 1.0F, 600.0F, 5.0F, Unit.SECOND)
        .setVisibleWhen(() -> this.durations.is("custom"));
    private final NumberSetting healing = new NumberSetting("targetcooldowns.healing", 20.0F, 1.0F, 300.0F, 1.0F, Unit.SECOND)
        .setVisibleWhen(() -> this.durations.is("custom"));
    private final DragSetting draggable;
    private final Animation widthAnimation = new Animation();
    private final Map<CooldownItem, TargetCooldowns.CooldownEntry> entries = new EnumMap<>(CooldownItem.class);
    private LivingEntity lastTarget;
    private long lastTargetTime;

    public TargetCooldowns() {
        this.draggable = new DragSetting(this.getInfo().name(), 10.0F, 130.0F);
        this.draggable.setSize(70.0F, 17.0F);
        this.draggable.onInput((drawContext, i, i2, f) -> this.render(drawContext));
        this.draggable.onInput(this);
        this.draggable.setRenderCondition(() -> this.isEnabled() && !this.entries.isEmpty() ? true : false);
        DragComponent.register(this.draggable);
    }

    public DragSetting getDraggable() {
        return this.draggable;
    }

    public boolean isCustomDurations() {
        return this.durations.is("custom");
    }

    public long getCustomDurationMs(CooldownItem cooldownItem) {
        switch (cooldownItem) {
            case GOLDEN_APPLE:
                return (long)(this.apple.getValue() * 1000.0F);
            case ENCHANTED_GOLDEN_APPLE:
                return (long)(this.enchantedApple.getValue() * 1000.0F);
            case HEALING_POTION:
                float f = this.healing.getValue();
                return (long)(f * 1000.0F);
            default:
                throw new MatchException(null, null);
        }
    }

    @Override
    public void render(DrawContext drawContext) {
        this.draggable.setDraggable(this::isEnabled);
        if (this.isEnabled()) {
            this.updateEntries();
            if (!this.entries.isEmpty()) {
                float f = this.draggable.position2.x;
                float f2 = this.draggable.position2.y;
                boolean flag = this.orientation.is("horizontal");
                float f3 = 0.0F;

                for (TargetCooldowns.CooldownEntry cooldownEntry : this.entries.values()) {
                    f3 = Math.max(f3, this.getEntryWidth(cooldownEntry));
                }

                this.widthAnimation.update();
                this.widthAnimation.animate(Math.max(f3, 1.0F), 0.35, Easings.QUART_OUT);
                float f4 = this.widthAnimation.get();
                float f5 = (float)(System.currentTimeMillis() % 1600L) / 1600.0F;
                float f6 = 0.0F;

                for (TargetCooldowns.CooldownEntry cooldownEntry2 : this.entries.values()) {
                    float f7 = cooldownEntry2.animation.get();
                    if (f7 > 0.001F) {
                        this.renderEntry(drawContext, flag ? f + f6 : f, flag ? f2 : f2 + f6, f4, f7, cooldownEntry2, f5);
                    }

                    f6 += ((flag ? f4 : 17.0F) + 3.0F) * f7;
                }

                float f8 = Math.max(f6 - 3.0F, 1.0F);
                float f9 = flag ? f8 : f4;
                if (flag) {
                    this.draggable.size2.set(f9, 17.0F);
                } else {
                    this.draggable.size2.set(f9, f8);
                }
            }
        }
    }

    private void updateEntries() {
        ItemCooldownTrackerComponent itemCooldownTrackerComponent = ItemCooldownTrackerComponent.getInstance();
        Map<CooldownItem, Long> map = this.getCooldowns(itemCooldownTrackerComponent);
        Iterator iterator2 = this.entries.values().iterator();

        while (iterator2.hasNext()) {
            ((TargetCooldowns.CooldownEntry)iterator2.next()).active = false;
        }

        for (Entry<CooldownItem, Long> entry : map.entrySet()) {
            CooldownItem cooldownItem = (CooldownItem)entry.getKey();
            this.entries.computeIfAbsent(cooldownItem, TargetCooldowns.CooldownEntry::new).update((Long)entry.getValue(), itemCooldownTrackerComponent.getDurationMs(cooldownItem));
        }

        for (TargetCooldowns.CooldownEntry cooldownEntry : this.entries.values()) {
            cooldownEntry.animation.update();
            cooldownEntry.animation.animate(cooldownEntry.active ? 1.0 : 0.0, 0.28, Easings.QUART_OUT);
        }

        this.entries.values().removeIf(cooldownEntry2 -> !cooldownEntry2.active && cooldownEntry2.animation.get() <= 0.001F);
    }

    private Map<CooldownItem, Long> getCooldowns(ItemCooldownTrackerComponent itemCooldownTrackerComponent) {
        if (itemCooldownTrackerComponent == null) {
            return Map.of();
        } else if (!(mc.currentScreen instanceof ClickGuiScreen) && !(mc.currentScreen instanceof ChatScreen)) {
            return this.getTarget() instanceof PlayerEntity playerEntity ? itemCooldownTrackerComponent.getCooldowns(playerEntity) : Map.of();
        } else {
            return this.getPreviewCooldowns(itemCooldownTrackerComponent);
        }
    }

    private Map<CooldownItem, Long> getPreviewCooldowns(ItemCooldownTrackerComponent itemCooldownTrackerComponent) {
        EnumMap enumMap = new EnumMap<>(CooldownItem.class);
        enumMap.put(CooldownItem.ENCHANTED_GOLDEN_APPLE, (long)((float)itemCooldownTrackerComponent.getDurationMs(CooldownItem.ENCHANTED_GOLDEN_APPLE) * 0.62F));
        enumMap.put(CooldownItem.GOLDEN_APPLE, (long)((float)itemCooldownTrackerComponent.getDurationMs(CooldownItem.GOLDEN_APPLE) * 0.28F));
        return enumMap;
    }

    private LivingEntity getTarget() {
        LivingEntity livingEntity = this.findCombatTarget();
        if (livingEntity != null) {
            this.lastTarget = livingEntity;
            this.lastTargetTime = System.currentTimeMillis();
            return this.lastTarget;
        }

        if (this.lastTarget != null && (System.currentTimeMillis() - this.lastTargetTime > 700L || !this.lastTarget.isAlive() || this.lastTarget.isRemoved())) {
            this.lastTarget = null;
        }

        return this.lastTarget;
    }

    private LivingEntity findCombatTarget() {
        AttackAura attackAura = AttackAura.getInstance();
        if (attackAura != null && attackAura.isEnabled() && attackAura.getCurrentTarget() != null) {
            return attackAura.getCurrentTarget();
        }

        AimAssist aimAssist = AimAssist.getInstance();
        if (aimAssist != null && aimAssist.isEnabled() && aimAssist.getTarget() != null) {
            return aimAssist.getTarget();
        }

        TriggerBot triggerBot = TriggerBot.getInstance();
        return triggerBot != null && triggerBot.isEnabled() ? triggerBot.findTarget() : null;
    }

    private void renderEntry(DrawContext drawContext, float f, float f2, float f3, float f4, TargetCooldowns.CooldownEntry cooldownEntry, float f5) {
        float f6 = (float)Easings.EXPO_OUT.ease(f4);
        Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
        matrix3x2fStack.pushMatrix();
        matrix3x2fStack.translate(f, f2 + 8.5F);
        matrix3x2fStack.scale(f6, f6);
        matrix3x2fStack.translate(-f, -(f2 + 8.5F));
        HudPanelUtil.drawSmallPanel(drawContext, f, f2, f3, 17.0F, f4, 8.5F);
        HudPanelUtil.drawPanel(drawContext, f, f2, 17.0F, 17.0F, f4, 8.5F);
        float f7 = f + 2.5F;
        float f8 = f2 + 2.5F;
        this.renderRing(drawContext, f7, f8, f4, cooldownEntry, f5);
        this.renderItem(drawContext, f7, f8, f4, cooldownEntry);
        this.renderTime(drawContext, f + 2.5F + 12.0F + 4.5F, f2, f4, cooldownEntry);
        matrix3x2fStack.popMatrix();
    }

    private void renderRing(DrawContext drawContext, float f, float f2, float f3, TargetCooldowns.CooldownEntry cooldownEntry, float f4) {
        new RenderUtil(drawContext, f, f2, 12.0F, 12.0F).circle(1.5F, ColorUtil.setAlphaf(Theme.getSecondaryText(), f3 * 0.22F));
        float f5 = 360.0F * MathHelper.clamp(cooldownEntry.progress, 0.0F, 1.0F);
        if (!(f5 <= 0.5F)) {
            new RenderUtil(drawContext, f, f2, 12.0F, 12.0F).arc(0.0F, f5, 1.5F, ColorUtil.setAlphaf(Theme.getPrimary(), f3), 1.0F, f4, 0.85F);
        }
    }

    private void renderItem(DrawContext drawContext, float f, float f2, float f3, TargetCooldowns.CooldownEntry cooldownEntry) {
        float f4 = MathUtil.roundToStep(f + 2.0F, 0.5F);
        float f5 = MathUtil.roundToStep(f2 + 2.0F, 0.5F);
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f4, f5, 0.5F);
        GuiRenderUtil.drawItem(drawContext, cooldownEntry.type.getIcon(), f4, f5, f3);
        MatrixUtil.pop(drawContext);
    }

    private void renderTime(DrawContext drawContext, float f, float f2, float f3, TargetCooldowns.CooldownEntry cooldownEntry) {
        float f4 = HEADER_FONT.getWidth(cooldownEntry.timeText, 7.0F);
        new RenderUtil(drawContext, f, f2, f4, 17.0F).textMiddleLeft(HEADER_FONT, cooldownEntry.timeText, 0.0F, 0.0F, ColorUtil.setAlphaf(Theme.getTextColor(), f3), 7.0F);
        new RenderUtil(drawContext, f + f4 + 0.5F, f2, BODY_FONT.getWidth("s", 5.0F), 17.0F)
            .textMiddleLeft(BODY_FONT, "s", 0.0F, 0.0F, ColorUtil.setAlphaf(Theme.getSecondaryText(), f3 * 0.75F), 5.0F);
    }

    private float getEntryWidth(TargetCooldowns.CooldownEntry cooldownEntry) {
        float f = HEADER_FONT.getWidth(cooldownEntry.timeText, 7.0F) + 0.5F + BODY_FONT.getWidth("s", 5.0F);
        return 19.0F + f + 6.5F;
    }

    public static TargetCooldowns getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(TargetCooldowns.class);
    }

    static final class CooldownEntry {
        final CooldownItem type;
        final Animation animation = new Animation();
        float progress;
        String timeText = "";
        private int lastTenths = -1;
        boolean active;

        private CooldownEntry(CooldownItem cooldownItem) {
            this.type = cooldownItem;
        }

        void update(long l, long l2) {
            this.active = true;
            this.progress = l2 <= 0L ? 0.0F : (float)l / (float)l2;
            int i = (int)Math.ceil(l / 100.0);
            if (i != this.lastTenths) {
                this.lastTenths = i;
                if (i >= 100) {
                    this.timeText = String.valueOf((i + 9) / 10);
                } else {
                    this.timeText = i / 10 + "." + i % 10;
                }
            }
        }
    }
}
