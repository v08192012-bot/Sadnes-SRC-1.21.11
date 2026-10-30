package tech.sadnes.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.impl.MouseLookEvent;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class RadialMenu {
    private static final float RADIUS = 92.0F;
    private static final float RING_RADIUS = 34.0F;
    private static final float DEAD_ZONE = 30.0F;
    private static final float GAP = 2.5F;
    private static final double SENSITIVITY = 0.67;
    private static final List<RadialMenu> INSTANCES = new ArrayList<>();
    private final Animation openAnimation = new Animation();
    private final List<Animation> slotAnimations = new ArrayList<>();
    private final Animation centerAnimation = new Animation();
    private final Animation labelAnimation = new Animation();
    private String currentLabel = "";
    private boolean opened;
    private double cursorX;
    private double cursorY;
    private Icon icon;
    private Supplier<String> defaultLabel = () -> "";
    private Supplier<String> hint;

    public RadialMenu() {
        INSTANCES.add(this);
    }

    public static boolean isAnyOpen() {
        int i = INSTANCES.size();

        for (int i2 = 0; i2 < i; i2++) {
            if (INSTANCES.get(i2).opened) {
                return true;
            }
        }

        return false;
    }

    public RadialMenu withIcon(Icon icon2) {
        this.icon = icon2;
        return this;
    }

    public RadialMenu withDefaultLabel(Supplier<String> supplier) {
        this.defaultLabel = supplier;
        return this;
    }

    public RadialMenu withHint(Supplier<String> supplier) {
        this.hint = supplier;
        return this;
    }

    public void open() {
        if (!this.opened) {
            this.opened = true;
            this.cursorX = 0.0;
            this.cursorY = 0.0;
            int i = this.slotAnimations.size();

            for (int i2 = 0; i2 < i; i2++) {
                this.slotAnimations.get(i2).forceValue(0.0);
            }

            this.centerAnimation.forceValue(0.0);
            this.labelAnimation.forceValue(0.0);
            this.currentLabel = "";
        }
    }

    public void close() {
        this.opened = false;
    }

    public void closeImmediately() {
        this.opened = false;
        this.openAnimation.forceValue(0.0);
    }

    public boolean isOpen() {
        return this.opened;
    }

    public boolean isVisible() {
        return this.opened || this.openAnimation.get() > 0.01;
    }

    public boolean onMouseMove(MouseLookEvent mouseLookEvent) {
        if (!this.opened) {
            return false;
        }

        this.cursorX = this.cursorX + mouseLookEvent.getDeltaX() * 0.67;
        this.cursorY = this.cursorY + mouseLookEvent.getDeltaY() * 0.67;
        double d = this.getCursorDistance();
        if (d > 86.0) {
            double d2 = 86.0 / d;
            this.cursorX *= d2;
            this.cursorY *= d2;
        }

        mouseLookEvent.cancel();
        return true;
    }

    public int getSelectedIndex(int i) {
        if (i > 0 && !this.isInDeadZone()) {
            double d = Math.toDegrees(Math.atan2(this.cursorY, this.cursorX)) + 90.0;
            double d2 = d % 360.0;
            if (d2 < 0.0) {
                d2 += 360.0;
            }

            double d3 = 360.0 / i;
            return (int)Math.round(d2 / d3) % i;
        } else {
            return -1;
        }
    }

    public boolean isInDeadZone() {
        return this.getCursorDistance() <= 30.0;
    }

    private double getCursorDistance() {
        return Math.hypot(this.cursorX, this.cursorY);
    }

    public void render(DrawContext drawContext, List<RadialMenu.Entry> entries) {
        this.openAnimation.update();
        this.openAnimation.animate(this.opened ? 1.0 : 0.0, this.opened ? 0.3 : 0.16, this.opened ? Easings.EXPO_OUT : Easings.QUAD_IN);
        float f = this.openAnimation.get();
        if (!(f <= 0.01F) && Fonts.INTER_MEDIUM != null) {
            float f2 = GuiScale.getScaledWidth() / 2.0F;
            float f3 = GuiScale.getScaledHeight() / 2.0F;
            int i = entries.size();
            int i2 = this.getSelectedIndex(i);
            this.updateAnimations(i, i2);
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f2, f3, 0.9F + 0.1F * f);
            this.renderRing(drawContext, f2, f3, i, f);
            this.renderItems(drawContext, f2, f3, entries, f);
            this.renderCenter(drawContext, f2, f3, f);
            this.renderLabel(drawContext, f2, f3, entries, i2, f);
            MatrixUtil.pop(drawContext);
        }
    }

    private void updateAnimations(int i, int i2) {
        while (this.slotAnimations.size() < i) {
            this.slotAnimations.add(new Animation());
        }

        int i3 = this.slotAnimations.size();

        for (int i4 = 0; i4 < i3; i4++) {
            Animation animation = this.slotAnimations.get(i4);
            animation.update();
            animation.animate(i4 == i2 ? 1.0 : 0.0, i4 == i2 ? 0.22 : 0.16, i4 == i2 ? Easings.BACK_OUT : Easings.QUAD_IN);
        }

        this.centerAnimation.update();
        Animation animation2 = this.centerAnimation;
        if (this.isInDeadZone()) {
            animation2.animate(1.0, 0.18, Easings.QUAD_OUT);
        } else {
            animation2.animate(0.0, 0.18, Easings.QUAD_OUT);
        }
    }

    private float getSlotProgress(int i) {
        return i >= 0 && i < this.slotAnimations.size() ? this.slotAnimations.get(i).get() : 0.0F;
    }

    private RenderUtil createRenderer(DrawContext drawContext, float f, float f2, float f3) {
        float f4 = 92.0F + f3;
        return new RenderUtil(drawContext, f - f4, f2 - f4, f4 * 2.0F, f4 * 2.0F);
    }

    private void renderRing(DrawContext drawContext, float f, float f2, int i, float f3) {
        int i2 = Theme.withAlpha(Theme.getBackground(), 0.55F);
        int i3 = Theme.withAlpha(Theme.getPrimary(), 0.85F);
        if (i <= 1) {
            float f4 = i == 1 ? Math.min(this.getSlotProgress(0), 1.0F) : 0.0F;
            this.createRenderer(drawContext, f, f2, 2.5F * f4).arcCapped(0.0F, 360.0F, 34.0F + 4.0F * f4, ColorUtil.interpolate(i2, i3, f4), f3);
        } else {
            float f5 = 360.0F / i;

            for (int i4 = 0; i4 < i; i4++) {
                float f6 = Math.min(this.getSlotProgress(i4), 1.0F);
                float f7 = i4 * f5 - f5 / 2.0F + 1.25F;
                float f8 = f5 - 2.5F;
                this.createRenderer(drawContext, f, f2, 2.5F * f6)
                    .arcCapped(f7, f8, 34.0F + 4.0F * f6, ColorUtil.interpolate(i2, i3, f6), f3);
            }
        }
    }

    private void renderItems(DrawContext drawContext, float f, float f2, List<RadialMenu.Entry> entries, float f3) {
        int i = entries.size();
        if (i != 0) {
            double d = 360.0 / i;

            for (int i2 = 0; i2 < i; i2++) {
                RadialMenu.Entry entry = (RadialMenu.Entry)entries.get(i2);
                float f4 = this.getSlotProgress(i2);
                float f5 = Math.min(f4, 1.0F);
                double d2 = Math.toRadians(-90.0 + i2 * d);
                float f6 = 75.0F + 1.5F * f5;
                float f7 = f + (float)Math.cos(d2) * f6;
                float f8 = f2 + (float)Math.sin(d2) * f6;
                float f9 = f3 * (entry.dimmed() ? 0.35F : 1.0F);
                MatrixUtil.push(drawContext);
                MatrixUtil.scale(drawContext, f7, f8, 1.0F + 0.18F * f4);
                GuiRenderUtil.drawItem(drawContext, entry.stack(), f7 - 8.0F, f8 - 8.0F, f9);
                if (entry.badge() != null) {
                    this.renderBadge(drawContext, entry.badge(), f7, f8, f9);
                }

                MatrixUtil.pop(drawContext);
            }
        }
    }

    private void renderBadge(DrawContext drawContext, String string, float f, float f2, float f3) {
        float f4 = Fonts.INTER_MEDIUM.getWidth(string, 5.0F) + 4.0F;
        new RenderUtil(drawContext, f + 9.0F - f4 / 2.0F, f2 + 5.5F, f4, 7.0F)
            .rect(Theme.withAlpha(Theme.getSurfaceDark(), 0.85F * f3), Radius.all(3.5F))
            .textCenter(Fonts.INTER_MEDIUM, string, Theme.withAlpha(Theme.getTextColor(), f3), 5.0F);
    }

    private void renderCenter(DrawContext drawContext, float f, float f2, float f3) {
        if (this.icon != null) {
            float f4 = this.centerAnimation.get();
            float f5 = 15.0F + 1.5F * f4;
            RenderUtil renderUtil = new RenderUtil(drawContext, f - f5, f2 - f5, f5 * 2.0F, f5 * 2.0F);
            if (f4 > 0.01F) {
                renderUtil.outline(9.0F, Theme.getPrimary(), 0.5F * f4 * f3, Radius.all(f5));
            }

            renderUtil.rect(Theme.withAlpha(Theme.getSurfaceDark(), 0.8F * f3), Radius.all(f5));
            int i = ColorUtil.interpolate(Theme.withAlpha(Theme.getSecondaryText(), f3), Theme.withAlpha(Theme.getPrimary(), f3), f4);
            renderUtil.icon(this.icon, f5 - 6.0F, f5 - 6.0F, 12.0F, i);
        }
    }

    private void renderLabel(DrawContext drawContext, float f, float f2, List<RadialMenu.Entry> entries, int i, float f3) {
        String string = i >= 0 ? ((RadialMenu.Entry)entries.get(i)).label() : this.defaultLabel.get();
        this.labelAnimation.update();
        if (!string.equals(this.currentLabel)) {
            this.currentLabel = string;
            this.labelAnimation.forceValue(0.0);
        }

        this.labelAnimation.animate(1.0, 0.2, Easings.QUAD_OUT);
        float f4 = this.labelAnimation.get();
        new RenderUtil(drawContext, f - 90.0F, f2 - 92.0F - 16.0F + (1.0F - f4) * 4.0F, 180.0F, 12.0F)
            .textCenter(Fonts.INTER_BOLD, string, Theme.withAlpha(Theme.getTextColor(), f3 * (0.25F + 0.75F * f4)), 9.0F);
        if (this.hint != null) {
            new RenderUtil(drawContext, f - 120.0F, f2 + 92.0F + 6.0F, 240.0F, 10.0F)
                .textCenter(Fonts.INTER_REGULAR, this.hint.get(), Theme.withAlpha(Theme.getSecondaryText(), 0.85F * f3), 6.5F);
        }
    }

    public record Entry(ItemStack stack, String label, String badge, boolean dimmed) {
        public Entry(ItemStack itemStack, String string) {
            this(itemStack, string, null, false);
        }
    }
}
