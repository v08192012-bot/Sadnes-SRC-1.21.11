package tech.sadnes.client.ui.clickgui.component;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.util.color.Theme;

public final class SmoothScrollbar {
    private static final float BAR_WIDTH = 2.0F;
    private static final float HIT_PADDING = 4.0F;
    private float targetScroll;
    private float scroll;
    private float minScroll;
    private final Animation scrollAnimation = new Animation();
    private final Animation visibilityAnimation = new Animation();
    private final Animation hoverAnimation = new Animation();
    private float barX;
    private float trackY;
    private float trackHeight;
    private float thumbY;
    private float thumbHeight;
    private boolean scrollable;
    private boolean dragging;
    private boolean hovered;
    private float dragStartY;
    private float dragStartScroll;
    private boolean autoHide = true;
    private long lastScrollTime;
    private float minThumbHeight = 18.0F;

    public SmoothScrollbar setAutoHide(boolean flag) {
        this.autoHide = flag;
        return this;
    }

    public SmoothScrollbar setMinThumbHeight(float f) {
        this.minThumbHeight = f;
        return this;
    }

    public void update(float f, float f2) {
        float f3 = this.minScroll;
        boolean flag = this.minScroll < 0.0F && this.targetScroll <= this.minScroll + 0.5F;
        this.minScroll = Math.min(0.0F, f2 - f);
        if (flag && this.minScroll < f3) {
            this.targetScroll = this.minScroll;
        } else {
            this.targetScroll = this.clamp(this.targetScroll, this.minScroll, 0.0F);
        }

        if (this.dragging) {
            this.scrollAnimation.forceValue(this.targetScroll);
        } else {
            this.scrollAnimation.update();
            this.scrollAnimation.animate(this.targetScroll, 0.35F, Easings.EXPO_OUT);
        }

        this.scroll = this.scrollAnimation.get();
        boolean flag2 = f > f2 + 0.5F || Math.abs(this.scroll - this.targetScroll) > 0.5F;
        boolean flag3 = flag2;
        if (this.autoHide) {
            flag3 = flag2 && (this.dragging || this.hovered || System.currentTimeMillis() - this.lastScrollTime < 800L);
        }

        this.visibilityAnimation.update();
        this.visibilityAnimation.animate(flag3 ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT, true);
        float f4 = this.dragging ? 1.0F : (this.hovered ? 0.55F : 0.0F);
        this.hoverAnimation.update();
        this.hoverAnimation.animate(f4, 0.18F, Easings.QUAD_OUT, true);
    }

    public void onScroll(double d) {
        if (this.minScroll >= 0.0F) {
            this.targetScroll = 0.0F;
        } else {
            float f = (float)(d * 22.0);
            this.targetScroll = this.clamp(this.targetScroll + f, this.minScroll, 0.0F);
            this.lastScrollTime = System.currentTimeMillis();
        }
    }

    public boolean mouseClicked(double d, double d2) {
        if (!this.scrollable || this.minScroll >= 0.0F) {
            return false;
        }

        if (!this.isOverBar(d, d2)) {
            return false;
        }

        this.lastScrollTime = System.currentTimeMillis();
        boolean flag = d2 >= this.thumbY - 4.0F && d2 <= this.thumbY + this.thumbHeight + 4.0F;
        if (flag) {
            this.dragging = true;
            this.dragStartY = (float)d2;
            this.dragStartScroll = this.targetScroll;
            return true;
        }

        float f = this.trackHeight - this.thumbHeight;
        if (f > 0.0F) {
            float f2 = (float)d2 - this.trackY - this.thumbHeight / 2.0F;
            float f3 = this.clamp(f2 / f, 0.0F, 1.0F);
            this.targetScroll = this.minScroll * f3;
            this.dragging = true;
            this.dragStartY = (float)d2;
            this.dragStartScroll = this.targetScroll;
        }

        return true;
    }

    public boolean mouseDragged(double d, double d2) {
        if (!this.dragging) {
            return false;
        }

        float f = this.trackHeight - this.thumbHeight;
        if (f <= 0.0F) {
            return true;
        }

        float f2 = (float)d2 - this.dragStartY;
        float f3 = this.minScroll / f;
        this.targetScroll = this.clamp(this.dragStartScroll + f2 * f3, this.minScroll, 0.0F);
        this.lastScrollTime = System.currentTimeMillis();
        return true;
    }

    public void mouseReleased() {
        this.dragging = false;
    }

    public void updateHover(double d, double d2) {
        if (this.scrollable && this.isOverBar(d, d2)) {
            this.hovered = true;
        } else {
            this.hovered = false;
        }
    }

    public boolean isDragging() {
        return this.dragging;
    }

    private boolean isOverBar(double d, double d2) {
        boolean flag = d >= this.barX - 4.0F && d <= this.barX + 2.0F + 4.0F;
        return !(d2 >= this.trackY) || !(d2 <= this.trackY + this.trackHeight) ? false : flag;
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        boolean flag = f4 > f3 + 0.5F || Math.abs(this.scroll - this.targetScroll) > 0.5F;
        this.scrollable = flag;
        this.barX = f;
        this.trackY = f2;
        this.trackHeight = f3;
        float f6 = Math.max(f4, f3 + Math.abs(this.scroll));
        float f7 = Math.min(f3, Math.max(this.minThumbHeight, f3 * (f3 / f6)));
        float f8 = this.minScroll == 0.0F ? (this.scroll < 0.0F ? 1.0F : 0.0F) : this.scroll / this.minScroll;
        float f9 = this.clamp(f8, 0.0F, 1.0F);
        float f10 = f2 + (f3 - f7) * f9;
        this.thumbY = f10;
        this.thumbHeight = f7;
        float f11 = f5 * this.visibilityAnimation.get();
        if (!(f11 <= 0.001F) && flag) {
            float f12 = this.hoverAnimation.get();
            float f13 = f12 * 0.6F;
            float f14 = f - f13 * 0.5F;
            float f15 = 2.0F + f13;
            float f16 = 1.5F + f13 * 0.25F;
            new RenderUtil(drawContext, f14, f10, f15, f7).rect(Theme.withAlpha(Theme.getTextColor(), (0.22F + 0.26F * f12) * f11), Radius.all(f16));
        }
    }

    public float getScroll() {
        return this.scroll;
    }

    public float getTargetScroll() {
        return this.targetScroll;
    }

    public void setScroll(float f) {
        this.targetScroll = f;
        this.scroll = f;
        this.scrollAnimation.forceValue(f);
    }

    public void reset() {
        this.targetScroll = 0.0F;
        this.scroll = 0.0F;
        this.minScroll = 0.0F;
        this.dragging = false;
        this.hovered = false;
        this.scrollAnimation.forceValue(0.0);
        this.visibilityAnimation.forceValue(0.0);
        this.hoverAnimation.forceValue(0.0);
    }

    private float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }
}
