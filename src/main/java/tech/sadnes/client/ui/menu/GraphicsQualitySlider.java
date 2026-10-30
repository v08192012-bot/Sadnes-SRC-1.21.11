package tech.sadnes.client.ui.menu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.config.ThemePrefs;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.IconHoverAnimator;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.SliderDrop;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class GraphicsQualitySlider implements IRenderable {
    private static final float SPRING_SNAP = 0.45F;
    private static final float HEIGHT = 15.0F;
    private static final float PHYSICS_STEP = 0.008333334F;
    private static final GraphicsQuality[] QUALITIES = GraphicsQuality.values();
    private final Animation hoverAnimation = new Animation();
    private final Animation pressAnimation = new Animation();
    private final Animation labelAnimation = new Animation();
    private final IconHoverAnimator iconAnimator = new IconHoverAnimator();
    private float position;
    private float velocity;
    private float target;
    private double dragValue;
    private double dragStart;
    private double scrollAccumulator;
    private boolean dragging;
    private boolean settled = true;
    private long lastNanos;
    private long waveTime;
    private long pulseTime;
    private float waveAt;
    private int currentLabel = -1;
    private int previousLabel = -1;
    private float labelDirection = 1.0F;
    private float x;
    private float y;
    private float width;
    private float trackCenterX;
    private float trackWidth;
    private float alpha;
    private float labelWidth;
    private String cachedLocale;

    public void render(DrawContext drawContext, int i, int i2, float f, float f2, float f3) {
        this.alpha = f3;
        long l = System.currentTimeMillis();
        this.updatePhysics(System.nanoTime(), l);
        if (Fonts.INTER_MEDIUM != null) {
            boolean flag;
            float f4;
            if (f < 360.0F) {
                flag = true;
                f4 = 32.0F;
            } else {
                flag = false;
                f4 = 44.0F;
            }

            float f5 = f4;
            float f6 = f5 + 13.5F;
            this.x = 12.0F;
            this.y = f2 - 12.0F - 15.0F;
            float f7 = this.x + 12.0F;
            this.trackCenterX = f7 + 6.75F;
            this.trackWidth = f5;
            if (!Language.getLocale().equals(this.cachedLocale)) {
                this.labelWidth = 0.0F;

                for (GraphicsQuality graphicsQuality : QUALITIES) {
                    this.labelWidth = Math.max(this.labelWidth, Fonts.INTER_MEDIUM.getWidth(graphicsQuality.getDisplayName(), 5.0F));
                }

                this.cachedLocale = Language.getLocale();
            }

            this.width = flag ? 12.0F + f6 + 3.0F : 12.0F + f6 + 1.5F + this.labelWidth + 6.0F;
            this.hoverAnimation.update();
            this.pressAnimation.update();
            this.labelAnimation.update();
            boolean flag2 = this.isHovered(i, i2, this.x, this.y, this.width, 15.0);
            if (!flag2) {
                this.scrollAccumulator = 0.0;
            }

            this.hoverAnimation.animate(flag2 ? 1.0 : 0.0, 0.18, Easings.QUAD_OUT);
            this.pressAnimation.animate(this.dragging ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
            int i3 = Math.round((this.dragging ? MathHelper.clamp(this.position, 0.0F, 1.0F) : this.target) * 2.0F);
            if (this.currentLabel < 0) {
                this.currentLabel = i3;
                this.labelAnimation.forceValue(1.0);
            } else if (i3 != this.currentLabel) {
                float f8 = this.labelAnimation.get();
                if (i3 == this.previousLabel) {
                    this.labelAnimation.forceValue(1.0 - f8);
                    this.previousLabel = this.currentLabel;
                } else {
                    float f9 = f8 * f8;
                    float f10 = (1.0F - f8) * (1.0F - f8);
                    if (this.previousLabel < 0 || f9 >= f10) {
                        this.previousLabel = this.currentLabel;
                    }

                    this.labelAnimation.forceValue(1.0 - Math.sqrt(Math.max(f9, f10)));
                }

                this.labelDirection = i3 > this.previousLabel ? 1.0F : -1.0F;
                this.currentLabel = i3;
                this.labelAnimation.animate(1.0, 0.22, Easings.QUAD_OUT);
            }

            if (!(f3 <= 0.004F)) {
                float f11 = this.hoverAnimation.get();
                float f12 = this.pressAnimation.get();
                Radius radius = Radius.all(7.5F);
                GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MIDDLE, true, this.x, this.y, this.width, 15.0F, f3 * 0.4F, 7.5F);
                RenderUtil renderUtil = new RenderUtil(drawContext, this.x, this.y, this.width, 15.0F);
                renderUtil.rect(Theme.withAlpha(ColorUtil.BLACK, (0.42F + 0.06F * f11 - 0.02F * f12) * f3), radius);
                renderUtil.shadow(1.0F, Theme.withAlpha(ColorUtil.WHITE, (0.06F + 0.04F * f11) * f3), radius);
                int i4 = ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), Math.max(f11, f12));
                this.iconAnimator.render(drawContext, Icon.WAND_SPARKLES, this.x + 7.5F - 3.75F, this.y + 7.5F - 3.75F, 7.5F, Theme.withAlpha(i4, f3), f11);
                SliderDrop sliderDrop = SliderDrop.builder()
                    .pos(this.position)
                    .velocity((float)Math.tanh(this.velocity / 3.0F))
                    .hover(f11)
                    .press(f12)
                    .pulseAge(Math.min(1.0F, (float)(l - this.pulseTime) / 450.0F))
                    .waveAge(Math.min(1.0F, (float)(l - this.waveTime) / 600.0F))
                    .waveAt(this.waveAt)
                    .build();
                new RenderUtil(drawContext, f7, this.y, f6, 15.0F).animatedEffect(Theme.getPrimary(), f3, sliderDrop, (float)(l % 1000000L) / 1000.0F);
                if (flag) {
                    float f13 = Math.max(f11, f12);
                    if (f13 > 0.004F) {
                        MenuTooltip.render(
                            drawContext, this.x, this.y - 11.0F + (1.0F - f13) * 5.0F, QUALITIES[this.currentLabel].getDisplayName(), f3 * f13, f13
                        );
                    }
                } else {
                    float f14 = this.labelAnimation.get();
                    float f15 = f7 + f6 + 1.5F;
                    new RenderUtil(drawContext, this.x, this.y + 1.5F, this.width, 12.0F)
                        .scissor(
                            () -> {
                                this.renderLabel(drawContext, this.currentLabel, f15, 7.0F * (1.0F - f14) * this.labelDirection, i4, f3 * f14 * f14);
                                if (this.previousLabel >= 0) {
                                    this.renderLabel(
                                        drawContext, this.previousLabel, f15, -7.0F * f14 * this.labelDirection, i4, f3 * (1.0F - f14) * (1.0F - f14)
                                    );
                                }
                            }
                        );
                }
            }
        }
    }

    private void renderLabel(DrawContext drawContext, int i, float f, float f2, int i2, float f3) {
        if (!(f3 <= 0.004F)) {
            new RenderUtil(drawContext, f, this.y - 0.5F, this.labelWidth, 15.0F)
                .textMiddleLeft(Fonts.INTER_MEDIUM, QUALITIES[i].getDisplayName(), 0.0F, f2, Theme.withAlpha(i2, f3), 5.0F);
        }
    }

    private void updatePhysics(long l, long l2) {
        float f = GraphicsQuality.getCurrent().ordinal() * 0.5F;
        if (this.lastNanos == 0L) {
            this.position = this.target = f;
            this.lastNanos = l;
        } else if (!this.dragging && this.settled && f != this.target) {
            this.target = f;
            this.settled = false;
        }

        float f2 = Math.min((float)(l - this.lastNanos) / 1.0E9F, 0.1F);
        this.lastNanos = l;
        if ((this.dragging || this.position != this.target || this.velocity != 0.0F) && f2 > 0.0F) {
            int i = Math.max(1, (int)Math.ceil(f2 / 0.008333334F));
            float f3 = f2 / i;
            float f4 = this.dragging ? rubberBand(applyDetents((float)this.dragValue)) : this.target;
            float f5 = this.dragging ? 22.0F : 15.0F;
            float f6 = this.dragging ? 0.9F : 0.5F;

            for (int i2 = 0; i2 < i; i2++) {
                this.velocity = this.velocity + (f5 * f5 * (f4 - this.position) - 2.0F * f6 * f5 * this.velocity) * f3;
                this.position = this.position + this.velocity * f3;
            }
        }

        if (!this.dragging && !this.settled && Math.abs(this.position - this.target) < 0.02F) {
            this.settled = true;
            this.waveTime = l2;
            this.waveAt = this.target;
        }

        if (!this.dragging && Math.abs(this.position - this.target) < 0.0015F && Math.abs(this.velocity) < 0.02F) {
            this.position = this.target;
            this.velocity = 0.0F;
        }
    }

    private static float applyDetents(float f) {
        float f2 = Math.round(MathHelper.clamp(f, 0.0F, 1.0F) * 2.0F) / 2.0F;
        float f3 = (f - f2) / 0.07F;
        return f + (f2 - f) * 0.35F * (float)Math.exp(-f3 * f3);
    }

    private static float rubberBand(float f) {
        if (f < 0.0F) {
            return -0.07F * (float)Math.tanh(-f / 0.07F);
        } else {
            return f > 1.0F ? 1.0F + 0.07F * (float)Math.tanh((f - 1.0F) / 0.07F) : f;
        }
    }

    public boolean mouseClicked(double d, double d2) {
        if (!(this.alpha <= 0.004F) && !(this.width <= 0.0F)) {
            if (!this.isHovered(d, d2, this.x, this.y, this.width, 15.0)) {
                return false;
            }

            long l = System.currentTimeMillis();
            if (d < this.x + 13.5F) {
                this.target = QUALITIES[Math.round(this.target * 2.0F)].next().ordinal() * 0.5F;
                this.settled = false;
                this.pulseTime = l;
                this.applyQuality();
                return true;
            }

            float f = this.trackCenterX - 6.75F;
            float f2 = this.trackWidth + 13.5F;
            if (d >= f - 3.0F && d <= f + f2 + 3.0F) {
                this.dragging = true;
                this.dragValue = this.dragStart = (d - this.trackCenterX) / this.trackWidth;
                this.pulseTime = l;
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean mouseDragged(double d) {
        if (!this.dragging) {
            return false;
        }

        this.dragValue = (d - this.trackCenterX) / this.trackWidth;
        return true;
    }

    public boolean mouseReleased() {
        if (!this.dragging) {
            return false;
        }

        this.dragging = false;
        boolean flag = Math.abs(this.dragValue - this.dragStart) * this.trackWidth < 2.0;
        float f = flag ? (float)this.dragValue : this.position + this.velocity * 0.1F;
        this.target = Math.round(MathHelper.clamp(f, 0.0F, 1.0F) * 2.0F) / 2.0F;
        this.settled = false;
        this.applyQuality();
        return true;
    }

    public boolean mouseScrolled(double d, double d2, double d3) {
        if (!(this.alpha <= 0.004F) && !this.dragging && d3 != 0.0 && this.isHovered(d, d2, this.x, this.y, this.width, 15.0)) {
            if (this.scrollAccumulator * d3 < 0.0) {
                this.scrollAccumulator = 0.0;
            }

            this.scrollAccumulator += d3;
            if (Math.abs(this.scrollAccumulator) < 1.0) {
                return true;
            }

            float f = MathHelper.clamp(this.target + (this.scrollAccumulator > 0.0 ? 0.5F : -0.5F), 0.0F, 1.0F);
            this.scrollAccumulator = 0.0;
            if (f != this.target) {
                this.target = f;
                this.settled = false;
                this.applyQuality();
            }

            return true;
        } else {
            return false;
        }
    }

    public void cancelDrag() {
        this.mouseReleased();
    }

    private void applyQuality() {
        GraphicsQuality graphicsQuality = QUALITIES[Math.round(this.target * 2.0F)];
        if (graphicsQuality != GraphicsQuality.getCurrent()) {
            GraphicsQuality.setCurrent(graphicsQuality);
            ThemePrefs.save();
        }
    }
}
