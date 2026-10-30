package tech.sadnes.client.ui;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class ContextMenu implements IInteractable, IRenderable {
    private static final float MIN_WIDTH = 82.0F;
    private static final float PADDING = 4.5F;
    private static final float ITEM_HEIGHT = 16.0F;
    private static final float SEPARATOR_GAP = 2.0F;
    private static final float ICON_SIZE = 8.5F;
    private static final int DANGER_TEXT_COLOR = ColorUtil.rgb(255, 86, 86);
    private static final int DANGER_ICON_COLOR = ColorUtil.rgb(255, 120, 120);
    private final Animation openAnimation = new Animation();
    private List<ContextMenu.MenuItem> items = List.of();
    private Animation[] hoverAnimations = new Animation[0];
    private boolean open;
    private float x;
    private float y;
    private float width;
    private float height;
    private int dangerStartIndex = -1;

    public void show(List<ContextMenu.MenuItem> menuItems, float f, float f2) {
        this.items = menuItems;
        this.open = true;
        this.openAnimation.forceValue(0.0);
        this.hoverAnimations = new Animation[menuItems.size()];
        this.dangerStartIndex = -1;
        this.width = 82.0F;
        int i = 0;

        for (int i2 = menuItems.size(); i < i2; i++) {
            ContextMenu.MenuItem menuItem = (ContextMenu.MenuItem)menuItems.get(i);
            this.hoverAnimations[i] = new Animation();
            if (menuItem.danger() && this.dangerStartIndex < 0 && i > 0) {
                this.dangerStartIndex = i;
            }

            this.width = Math.max(this.width, 16.0F + Fonts.INTER_MEDIUM.getWidth(menuItem.label(), 5.5F) + 4.0F + 9.0F);
        }

        this.height = 9.0F + menuItems.size() * 16.0F + (this.dangerStartIndex > 0 ? 2.0F : 0.0F);
        this.x = GuiRenderUtil.clampToScreenX(f - this.width, this.width);
        this.y = GuiRenderUtil.clampToScreenY(f2, this.height);
    }

    public void close() {
        this.open = false;
    }

    public boolean isOpen() {
        return this.open;
    }

    public void render(DrawContext drawContext, int i, int i2, float f) {
        this.openAnimation.update();
        if (this.open) {
            this.openAnimation.animate(1.0, 0.3F, Easings.BACK_OUT);
        } else {
            this.openAnimation.animate(0.0, 0.2F, Easings.QUAD_IN);
        }

        if (!this.items.isEmpty() && (this.open || !(this.openAnimation.get() <= 0.001))) {
            float f2 = this.openAnimation.get();
            float f3 = f * Math.min(f2, 1.0F);
            float f4 = this.x + 4.5F;
            float f5 = this.width - 9.0F;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, this.x + this.width / 2.0F, this.y + this.height / 2.0F, 0.85F + 0.15F * f2);
            GuiRenderUtil.drawPopup(drawContext, this.x, this.y, this.width, this.height, f3, 10.0F);
            int i3 = 0;

            for (int i4 = this.items.size(); i3 < i4; i3++) {
                ContextMenu.MenuItem menuItem = this.items.get(i3);
                float f6 = this.itemY(i3);
                if (i3 == this.dangerStartIndex) {
                    GuiRenderUtil.drawOverlay(drawContext, f4 + 4.0F, MathUtil.roundToStep(f6 - 1.0F - 0.25F, 0.5F), f5 - 8.0F, 0.5F, f3);
                }

                Animation animation = this.hoverAnimations[i3];
                animation.update();
                boolean flag;
                if (this.open) {
                    flag = this.isHovered(i, i2, f4, f6, f5, 16.0);
                } else {
                    flag = false;
                }

                if (flag) {
                    animation.animate(1.0, 0.25, Easings.EXPO_OUT);
                } else {
                    animation.animate(0.0, 0.2, Easings.QUAD_OUT);
                }

                float f7 = animation.get();
                GuiRenderUtil.drawHighlight(drawContext, f4, f6, f5, 16.0F, f3 * f7, Radius.all(4.0F));
                int i5 = menuItem.danger()
                    ? Theme.withAlpha(DANGER_TEXT_COLOR, f3)
                    : ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.85F + 0.15F * f7), f3);
                int i6 = menuItem.danger()
                    ? Theme.withAlpha(DANGER_ICON_COLOR, f3)
                    : ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getPrimary(), f7), f3);
                new RenderUtil(drawContext, f4, f6, f5, 16.0F)
                    .icon(menuItem.icon(), 3.0F, 3.75F, 8.5F, i6)
                    .textMiddleLeft(Fonts.INTER_MEDIUM, menuItem.label(), 16.0F, 0.0F, i5, 5.5F);
            }

            MatrixUtil.pop(drawContext);
        }
    }

    public boolean mouseClicked(double d, double d2) {
        if (!this.open) {
            return false;
        }

        int i = -1;
        int i2 = 0;

        for (int i3 = this.items.size(); i2 < i3; i2++) {
            if (this.isHovered(d, d2, this.x + 4.5F, this.itemY(i2), this.width - 9.0F, 16.0)) {
                i = i2;
                break;
            }
        }

        this.close();
        if (i >= 0) {
            this.items.get(i).action().run();
        }

        return true;
    }

    private float itemY(int i) {
        return this.dangerStartIndex > 0 && i >= this.dangerStartIndex ? this.y + 4.5F + i * 16.0F + 2.0F : this.y + 4.5F + i * 16.0F + 0.0F;
    }

    public record MenuItem(Icon icon, String label, boolean danger, Runnable action) {
        public static ContextMenu.MenuItem of(Icon icon2, String string, Runnable runnable) {
            return new ContextMenu.MenuItem(icon2, string, false, runnable);
        }

        public static ContextMenu.MenuItem ofDanger(Icon icon2, String string, Runnable runnable) {
            return new ContextMenu.MenuItem(icon2, string, true, runnable);
        }
    }
}
