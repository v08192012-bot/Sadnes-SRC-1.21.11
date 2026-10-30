package tech.sadnes.client.feature.impl.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.world.GameMode;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.ui.hud.Draggable;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

@FeatureInfo(
    name = "Hotbar",
    description = "feature.impl.hud.hotbar",
    displayName = "feature.impl.hud.hotbar",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_INVENTORY
)
public final class Hotbar extends Feature implements Draggable {
    private static final float SLOT_ITEM_SIZE = 16.0F;
    private static final float SLOT_SIZE = 20.0F;
    private static final float PADDING2 = 4.0F;
    private static final float SCREEN_MARGIN = 4.0F;
    private static final float OFFHAND_GAP = 4.0F;
    private static final float WINDOW_RADIUS = 8.0F;
    private static final float ICON_GAP = 2.0F;
    private static final float PANEL_WIDTH = 188.0F;
    private static final float OFFHAND_SIZE = 28.0F;
    private static final float STATUS_ROW_HEIGHT = 9.0F;
    private static final float XP_ROW_HEIGHT = 8.0F;
    private static final float ROW_GAP = 3.0F;
    private static final float ICON_SIZE = 7.0F;
    private static final float STATUS_HEIGHT = 23.0F;
    private static final int LOW_HEALTH_COLOR = ColorUtil.rgb(235, 75, 75);
    private static final int FULL_HEALTH_COLOR = ColorUtil.rgb(110, 215, 120);
    private static final int FOOD_COLOR = ColorUtil.rgb(196, 140, 60);
    private static final int ARMOR_COLOR = ColorUtil.rgb(176, 190, 205);
    private static final int AIR_COLOR = ColorUtil.rgb(120, 190, 235);
    private static final int XP_COLOR = ColorUtil.rgb(126, 220, 70);
    private static final Radius SLOT_RADIUS = Radius.all(3.0F);
    private static final Radius BAR_RADIUS = Radius.all(2.5F);
    private static final Radius XP_BAR_RADIUS = Radius.all(2.0F);
    private final BooleanSetting overlays = new BooleanSetting("hotbar.overlays", true);
    private final BooleanSetting status = new BooleanSetting("hotbar.status", true);
    private final BooleanSetting offhand = new BooleanSetting("hotbar.offhand", true);
    private final Animation showAnimation = new Animation();
    private final Animation selectedSlotAnimation = new Animation();
    private final Object[] slotCacheKeys = new Object[9];
    private final Object offhandCacheKey = new Object();

    public Hotbar() {
        for (int i = 0; i < this.slotCacheKeys.length; i++) {
            this.slotCacheKeys[i] = new Object();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.showAnimation.forceValue(0.0);
        if (mc.player != null) {
            this.selectedSlotAnimation.forceValue(mc.player.getInventory().getSelectedSlot());
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent render2DEvent) {
        if (mc.player != null && !mc.options.hudHidden) {
            if (mc.interactionManager == null || mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
                this.render(render2DEvent.getContext());
            }
        }
    }

    @Override
    public void render(DrawContext drawContext) {
        this.showAnimation.update();
        this.showAnimation.animate(1.0, 0.2, Easings.QUART_OUT);
        float f = this.showAnimation.get();
        if (!(f <= 0.001F)) {
            boolean flag;
            float f2;
            if (this.status.getValue() && mc.interactionManager != null && mc.interactionManager.hasStatusBars()) {
                flag = true;
                f2 = 23.0F;
            } else {
                flag = false;
                f2 = 0.0F;
            }

            float f3 = f2;
            float f4 = 4.0F + f3 + 20.0F + 4.0F;
            float f5 = (GuiScale.getScaledWidth() - 188.0F) / 2.0F;
            float f6 = GuiScale.getScaledHeight() - f4 - 4.0F - Watermark.getBottomOffset();
            float f7 = f5 + 4.0F;
            float f8 = f6 + 4.0F + f3;
            int i = mc.player.getInventory().getSelectedSlot();
            this.selectedSlotAnimation.update();
            this.selectedSlotAnimation.animate(i, 0.22, Easings.QUART_OUT);
            HudPanelUtil.drawPanel(drawContext, f5, f6, 188.0F, f4, f, 8.0F);
            if (flag) {
                this.renderStatusRow(drawContext, f7, f6 + 4.0F, 180.0F, f);
            }

            for (int i2 = 0; i2 < 9; i2++) {
                this.renderSlotBackground(drawContext, f7 + i2 * 20.0F, f8, f);
            }

            this.renderSelectedSlot(drawContext, f7, f8, f);
            PlayerInventory playerInventory = mc.player.getInventory();

            for (int i3 = 0; i3 < 9; i3++) {
                this.renderSlotItem(drawContext, this.slotCacheKeys[i3], playerInventory.getStack(i3), f7 + i3 * 20.0F, f8, f);
            }

            this.renderOffhand(drawContext, f5, f6, f4, f);
        }
    }

    private void renderStatusRow(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = (f3 - 26.0F) / 2.0F;
        float f6 = ServerManager.getHealth(mc.player);
        float f7 = Math.max(1.0F, mc.player.getMaxHealth());
        float f8 = clamp01(f6 / f7);
        this.renderStatusBar(
            drawContext,
            f,
            f2,
            f5,
            Icon.HEART,
            ColorUtil.interpolate(LOW_HEALTH_COLOR, FULL_HEALTH_COLOR, f8),
            f8,
            Integer.toString((int)Math.ceil(f6)),
            f4,
            false
        );
        float f9 = f + f5 + 26.0F;
        if (mc.player.getVehicle() instanceof LivingEntity livingEntity) {
            float f10 = livingEntity.getHealth();
            float f11 = Math.max(1.0F, livingEntity.getMaxHealth());
            float f12 = clamp01(f10 / f11);
            this.renderStatusBar(
                drawContext,
                f9,
                f2,
                f5,
                Icon.HEART,
                ColorUtil.interpolate(LOW_HEALTH_COLOR, FULL_HEALTH_COLOR, f12),
                f12,
                Integer.toString((int)Math.ceil(f10)),
                f4,
                true
            );
        } else {
            int i = mc.player.getHungerManager().getFoodLevel();
            this.renderStatusBar(drawContext, f9, f2, f5, Icon.DRUMSTICK, FOOD_COLOR, i / 20.0F, Integer.toString(i), f4, true);
        }

        int i2 = mc.player.getAir();
        int i3 = mc.player.getMaxAir();
        int i4 = mc.player.getArmor();
        if (i2 < i3 && i3 > 0) {
            this.renderStatusValue(drawContext, f + f5, f2, 26.0F, Icon.DROPLET, AIR_COLOR, Math.round(i2 * 100.0F / i3) + "%", f4);
        } else if (i4 > 0) {
            this.renderStatusValue(drawContext, f + f5, f2, 26.0F, Icon.SHIELD, ARMOR_COLOR, Integer.toString(i4), f4);
        }

        if (mc.interactionManager != null && mc.interactionManager.hasExperienceBar()) {
            this.renderExperienceBar(drawContext, f, f2 + 9.0F + 3.0F, f3, f4);
        }
    }

    private void renderStatusBar(DrawContext drawContext, float f, float f2, float f3, Icon icon2, int i, float f4, String string, float f5, boolean flag) {
        float f6 = BODY_FONT.getWidth(string, 6.0F);
        float f7 = f2 + 1.0F;
        float f8 = f2 + 2.0F;
        float f9;
        float f10;
        float f11;
        float f12;
        if (flag) {
            f9 = f + f3 - 7.0F;
            f10 = f;
            f11 = f + f6 + 2.0F;
            f12 = f9 - 2.0F;
        } else {
            f9 = f;
            f10 = f + f3 - f6;
            f11 = f + 7.0F + 2.0F;
            f12 = f10 - 2.0F;
        }

        new RenderUtil(drawContext, f9, f7, 7.0F, 7.0F).icon(icon2, 0.0F, 0.0F, 7.0F, ColorUtil.setAlphaf(i, f5));
        if (f12 - f11 > 1.0F) {
            this.renderProgressBar(drawContext, f11, f8, f12 - f11, 5.0F, f4, i, f5, BAR_RADIUS);
        }

        new RenderUtil(drawContext, f10, f2, f6, 9.0F).textMiddleLeft(BODY_FONT, string, 0.0F, 0.0F, ColorUtil.setAlphaf(Theme.getTextColor(), f5), 6.0F);
    }

    private void renderStatusValue(DrawContext drawContext, float f, float f2, float f3, Icon icon2, int i, String string, float f4) {
        float f5 = BODY_FONT.getWidth(string, 6.0F);
        float f6 = f + (f3 - (9.0F + f5)) / 2.0F;
        new RenderUtil(drawContext, f6, f2 + 1.0F, 7.0F, 7.0F).icon(icon2, 0.0F, 0.0F, 7.0F, ColorUtil.setAlphaf(i, f4));
        new RenderUtil(drawContext, f6 + 7.0F + 2.0F, f2, f5, 9.0F).textMiddleLeft(BODY_FONT, string, 0.0F, 0.0F, ColorUtil.setAlphaf(Theme.getTextColor(), f4), 6.0F);
    }

    private void renderExperienceBar(DrawContext drawContext, float f, float f2, float f3, float f4) {
        int i = mc.player.experienceLevel;
        String string = Integer.toString(i);
        float f5 = i > 0 ? BODY_FONT.getWidth(string, 6.0F) : 0.0F;
        new RenderUtil(drawContext, f, f2 + 0.5F, 7.0F, 7.0F).icon(Icon.BOLT, 0.0F, 0.0F, 7.0F, ColorUtil.setAlphaf(XP_COLOR, f4));
        float f6 = f + 7.0F + 2.0F;
        float f7 = f + f3 - (i > 0 ? f5 + 2.0F : 0.0F);
        if (f7 - f6 > 1.0F) {
            this.renderProgressBar(drawContext, f6, f2 + 2.0F, f7 - f6, 4.0F, mc.player.experienceProgress, XP_COLOR, f4, XP_BAR_RADIUS);
        }

        if (i > 0) {
            new RenderUtil(drawContext, f + f3 - f5, f2, f5, 8.0F).textMiddleLeft(BODY_FONT, string, 0.0F, 0.0F, ColorUtil.setAlphaf(XP_COLOR, f4), 6.0F);
        }
    }

    private void renderProgressBar(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, int i, float f6, Radius radius) {
        new RenderUtil(drawContext, f, f2, f3, f4).rect(ColorUtil.multiplyAlpha(i, f6 * 0.18F), radius);
        float f7 = f3 * clamp01(f5);
        if (f7 > 0.5F) {
            new RenderUtil(drawContext, f, f2, f7, f4).rect(ColorUtil.multiplyAlpha(i, f6), radius);
        }
    }

    private void renderSlotBackground(DrawContext drawContext, float f, float f2, float f3) {
        new RenderUtil(drawContext, f + 1.0F, f2 + 1.0F, 18.0F, 18.0F).rect(Theme.getBase(), f3 * Theme.dim(0.12F), SLOT_RADIUS);
    }

    private void renderSelectedSlot(DrawContext drawContext, float f, float f2, float f3) {
        float f4 = f + this.selectedSlotAnimation.get() * 20.0F;
        new RenderUtil(drawContext, f4 + 1.0F, f2 + 1.0F, 18.0F, 18.0F)
            .rect(ColorUtil.setAlphaf(Theme.getPrimary(), f3 * 0.18F), SLOT_RADIUS)
            .shadow(1.0F, ColorUtil.setAlphaf(Theme.getPrimary(), f3 * 0.5F), SLOT_RADIUS);
    }

    private void renderOffhand(DrawContext drawContext, float f, float f2, float f3, float f4) {
        if (this.offhand.getValue()) {
            ItemStack itemStack = mc.player.getOffHandStack();
            if (!itemStack.isEmpty()) {
                float f5 = mc.player.getMainArm().getOpposite() == Arm.LEFT ? f - 4.0F - 28.0F : f + 188.0F + 4.0F;
                float f6 = f2 + f3 - 28.0F;
                GuiRenderUtil.window(drawContext, f5, f6, 28.0F, 28.0F, f4, 8.0F);
                this.renderSlotBackground(drawContext, f5 + 4.0F, f6 + 4.0F, f4);
                this.renderSlotItem(drawContext, this.offhandCacheKey, itemStack, f5 + 4.0F, f6 + 4.0F, f4);
            }
        }
    }

    private void renderSlotItem(DrawContext drawContext, Object object, ItemStack itemStack, float f, float f2, float f3) {
        if (!itemStack.isEmpty()) {
            float f4 = f + 2.0F;
            float f5 = f2 + 2.0F;
            GuiRenderUtil.drawCachedItem(drawContext, object, itemStack, f4, f5, f3);
            if (this.overlays.getValue()) {
                drawContext.drawStackOverlay(mc.textRenderer, itemStack, (int)f4, (int)f5);
            }
        }
    }

    private static float clamp01(float f) {
        return Math.max(0.0F, Math.min(1.0F, f));
    }

    public static Hotbar getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(Hotbar.class);
    }

    public static boolean isActive() {
        Hotbar hotbar = getInstance();
        return hotbar != null && hotbar.isEnabled();
    }

    public static boolean isStatusShown() {
        Hotbar hotbar = getInstance();
        return hotbar != null && hotbar.isEnabled() && hotbar.status.getValue();
    }
}
