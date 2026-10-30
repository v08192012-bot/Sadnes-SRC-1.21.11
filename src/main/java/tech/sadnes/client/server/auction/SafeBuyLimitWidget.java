package tech.sadnes.client.server.auction;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.PixelRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class SafeBuyLimitWidget {
    private final SafeBuyLimitTracker safeBuy;
    private final Animation appearAnimation = new Animation();
    private final Animation expandAnimation = new Animation();
    private final Animation collapsedWidthAnimation = new Animation();
    private final Animation hoverAnimation = new Animation();
    private final Animation knobHoverAnimation = new Animation();
    private final Animation progressAnimation = new Animation();
    private final Animation dragAnimation = new Animation();
    private final Animation activeAnimation = new Animation();
    private int lastSessionId;
    private long hoverStartTime = -1L;
    private long keepExpandedUntil;
    private boolean expanded;
    private boolean pressed;

    public void render(DrawContext drawContext, List<Slot> slots, int i, int i2, int i3, int i4) {
        if (this.lastSessionId != this.safeBuy.getVersion()) {
            this.lastSessionId = this.safeBuy.getVersion();
            this.appearAnimation.forceValue(0.0);
            this.expandAnimation.forceValue(0.0);
            this.collapsedWidthAnimation.forceValue(0.0);
            this.hoverStartTime = -1L;
            this.keepExpandedUntil = 0L;
        }

        int i5 = this.highlightFilteredSlots(drawContext, slots, i, i2);
        this.updateAnimations(i, i2, i3, i4);
        this.renderPanel(drawContext, this.getBounds(i, i2), i5);
    }

    public boolean mouseClicked(double d, double d2, int i, int i2, int i3) {
        this.safeBuy.commitPreview();
        SafeBuyLimitWidget.Bounds bounds = this.getBounds(i2, i3);
        this.pressed = bounds.contains(d, d2);
        if (this.pressed && i == 0) {
            if (this.expandAnimation.get() < 0.9F) {
                this.hoverStartTime = System.currentTimeMillis() - 60L;
                return true;
            }

            if (this.isOverSlider(bounds, d2)) {
                this.safeBuy.setPreviewLimit(this.getPriceAt(d, bounds));
            }

            return true;
        } else {
            return this.pressed;
        }
    }

    public boolean mouseDragged(double d, int i, int i2) {
        if (this.pressed && this.safeBuy.isPreviewing()) {
            this.safeBuy.setPreviewLimit(this.getPriceAt(d, this.getBounds(i, i2)));
            return true;
        } else {
            return false;
        }
    }

    public boolean mouseReleased() {
        if (!this.pressed) {
            return false;
        }

        this.pressed = false;
        if (this.safeBuy.isPreviewing()) {
            this.safeBuy.commitPreview();
            this.keepExpandedUntil = System.currentTimeMillis() + 350L;
        }

        return true;
    }

    public boolean mouseScrolled(double d, double d2, double d3, int i, int i2) {
        if (d3 == 0.0 || this.safeBuy.isPreviewing() || !this.getBounds(i, i2).contains(d, d2)) {
            return false;
        } else if (d3 > 0.0) {
            this.safeBuy.stepLimit(true);
            this.keepExpandedUntil = System.currentTimeMillis() + 700L;
            return true;
        } else {
            this.safeBuy.stepLimit(false);
            this.keepExpandedUntil = System.currentTimeMillis() + 700L;
            return true;
        }
    }

    private int highlightFilteredSlots(DrawContext drawContext, List<Slot> slots, int i, int i2) {
        if (this.safeBuy.getEffectiveLimit() <= 0L) {
            return 0;
        }

        int i3 = Theme.withAlpha(Theme.getBackground(), 0.6F);
        int i4 = 0;
        int i5 = Math.min(45, slots.size() - 36);

        for (int i6 = 0; i6 < i5; i6++) {
            Slot slot = (Slot)slots.get(i6);
            if (this.safeBuy.isOverLimit(slot.getStack())) {
                PixelRenderUtil.drawRect(drawContext, i + slot.x, i2 + slot.y, 16.0F, 16.0F, i3);
                i4++;
            }
        }

        return i4;
    }

    private void updateAnimations(int i, int i2, int i3, int i4) {
        long l = System.currentTimeMillis();
        long l2 = this.safeBuy.getEffectiveLimit();
        boolean flag = l2 > 0L;
        boolean flag2 = this.safeBuy.isPreviewing();
        SafeBuyLimitWidget.Bounds bounds = this.getBounds(i, i2);
        if (bounds.grow(this.expanded ? 4.0F : 0.0F).contains(i3, i4)) {
            if (this.hoverStartTime < 0L) {
                this.hoverStartTime = l;
            }
        } else if (this.hoverStartTime >= 0L) {
            this.hoverStartTime = -1L;
            if (this.expanded) {
                this.keepExpandedUntil = Math.max(this.keepExpandedUntil, l + 350L);
            }
        }

        this.expanded = flag2 || this.hoverStartTime >= 0L && l - this.hoverStartTime >= 60L || l < this.keepExpandedUntil;
        boolean flag3;
        if (this.expandAnimation.get() > 0.9F && bounds.contains(i3, i4)) {
            flag3 = this.isOverSlider(bounds, i4);
        } else {
            flag3 = false;
        }

        this.appearAnimation.update();
        this.appearAnimation.animate(1.0, 0.22, Easings.CUBIC_OUT);
        this.collapsedWidthAnimation.update();
        this.collapsedWidthAnimation.animate(this.calcCollapsedWidth(flag ? formatShortPrice(l2) : this.getNoLimitText(), flag), 0.2, Easings.CUBIC_OUT);
        this.expandAnimation.update();
        this.expandAnimation.animate(this.expanded ? 1.0 : 0.0, this.expanded ? 0.22 : 0.2, Easings.CUBIC_OUT);
        this.hoverAnimation.update();
        this.hoverAnimation.animate(this.hoverStartTime < 0L && !flag2 ? 0.0 : 1.0, 0.15, Easings.QUAD_OUT);
        this.knobHoverAnimation.update();
        this.knobHoverAnimation.animate(flag3 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        this.progressAnimation.update();
        this.progressAnimation.animate(this.safeBuy.getLimitFraction(l2), 0.25, Easings.EXPO_OUT);
        this.dragAnimation.update();
        this.dragAnimation.animate(flag2 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        this.activeAnimation.update();
        if (flag) {
            this.activeAnimation.animate(1.0, 0.15, Easings.QUAD_OUT);
        } else {
            this.activeAnimation.animate(0.0, 0.15, Easings.QUAD_OUT);
        }
    }

    private void renderPanel(DrawContext drawContext, SafeBuyLimitWidget.Bounds bounds, int i) {
        long l = this.safeBuy.getEffectiveLimit();
        boolean flag = l > 0L;
        boolean flag2 = this.safeBuy.isPreviewing();
        float f = this.appearAnimation.get();
        float f2 = this.expandAnimation.get();
        float f3 = MathHelper.clamp((f2 - 0.35F) / 0.65F, 0.0F, 1.0F);
        float f4 = f * (1.0F - MathHelper.clamp(f2 / 0.4F, 0.0F, 1.0F));
        float f5 = f * f3;
        float f6 = 4.0F * (1.0F - f3);
        int i2 = flag ? Theme.getTextColor() : Theme.getSecondaryText();
        int i3 = Theme.withAlpha(Theme.getTextColor(), 0.5F);
        Radius radius = Radius.all(Math.round(MathHelper.lerp(f2, 10.0F, 8.0F) * 2.0F) / 2.0F);
        int i4 = ColorUtil.interpolate(Theme.getBase(), Theme.getWhite(), 0.06F * this.hoverAnimation.get());
        new RenderUtil(
                drawContext,
                MathUtil.roundToStep(bounds.x(), 0.5F),
                MathUtil.roundToStep(bounds.y(), 0.5F),
                MathUtil.roundToStep(bounds.w(), 0.5F),
                MathUtil.roundToStep(bounds.h(), 0.5F)
            )
            .blurGlowBorder(
                f, i4, 0.68F, Theme.getNearBackground(), 4.0F, Theme.getShadowStrength(f), Easings.QUAD_IN, radius, RenderUtil.BlurQuality.LOW, false
            )
            .shadow(0.5F, Theme.withAlpha(Theme.getMutedText(), (0.12F + 0.1F * this.hoverAnimation.get()) * f), radius);
        String string = Language.translate("auctionhelper.safeBuy.perUnitShort", "/шт");
        String string2 = Language.translate("auctionhelper.safeBuy.perUnit", "за шт.");
        String string3 = flag ? formatShortPrice(l) : this.getNoLimitText();
        String string4 = flag ? (bounds.compact() ? string3 : AuctionLoreUtil.formatNumber(l)) : this.getNoLimitText();
        String string5 = String.format(Language.translate("auctionhelper.safeBuy.filtered", "отсеяно %d"), i);
        float f7 = Fonts.INTER_SEMIBOLD.getWidth(string4, 7.0F);
        float f8 = flag && !bounds.compact() ? 4.0F + Fonts.INTER_REGULAR.getWidth(string2, 5.5F) : 0.0F;
        boolean flag3 = i > 0 && 24.0F + f7 + f8 + 8.0F + Fonts.INTER_REGULAR.getWidth(string5, 5.5F) <= bounds.w() - 8.0F;
        boolean flag4 = flag2 && this.safeBuy.getMarketPrice() > 0L;
        String string6 = flag4
            ? String.format(Language.translate("auctionhelper.safeBuy.market", "рынок %s"), AuctionLoreUtil.formatNumber(this.safeBuy.getMarketPrice()))
            : this.safeBuy.getDisplayName();
        float f9 = bounds.y() + (bounds.compact() ? 27 : 35);
        float f10 = bounds.w() - 22.0F;
        float f11 = bounds.x() + 11.0F + this.safeBuy.getLimitFraction(this.safeBuy.getMarketPrice()) * f10;
        float f12 = (6.0F + this.knobHoverAnimation.get()) * (1.0F + 0.3F * this.dragAnimation.get());
        float f13 = bounds.x() + 8.0F + f12 / 2.0F + (bounds.w() - 16.0F - f12) * this.progressAnimation.get();
        float f14 = MathHelper.clamp((Math.abs(f11 - f13) - (f12 / 2.0F + 0.5F)) / 4.0F, 0.0F, 1.0F);
        new RenderUtil(drawContext, bounds.x(), bounds.y(), bounds.w(), bounds.h())
            .scissor(
                () -> {
                    float f15 = MathHelper.lerp(f2, 4.0F, 8.0F);
                    this.renderShieldIcon(drawContext, bounds.x() + f15, bounds.y() + f15, flag, f);
                    if (f4 > 0.01F) {
                        new RenderUtil(drawContext, bounds.x() + 20.0F, bounds.y(), bounds.w(), 20.0F)
                            .textMiddleLeft(Fonts.INTER_SEMIBOLD, string3, 0.0F, 0.0F, ColorUtil.multiplyAlpha(i2, f4), 6.5F)
                            .textMiddleLeft(
                                Fonts.INTER_REGULAR,
                                flag ? string : "",
                                Fonts.INTER_SEMIBOLD.getWidth(string3, 6.5F) + 4.0F,
                                0.3F,
                                ColorUtil.multiplyAlpha(i3, f4),
                                5.5F
                            );
                    }

                    if (!(f5 <= 0.01F)) {
                        new RenderUtil(drawContext, bounds.x() + 24.0F + f6, bounds.y() + 8.0F, bounds.w() - 32.0F, 12.0F)
                            .textMiddleLeft(Fonts.INTER_SEMIBOLD, string4, 0.0F, 0.0F, ColorUtil.multiplyAlpha(i2, f5), 7.0F)
                            .textMiddleLeft(Fonts.INTER_REGULAR, f8 > 0.0F ? string2 : "", f7 + 4.0F, 0.45F, ColorUtil.multiplyAlpha(i3, f5), 5.5F);
                        if (flag3) {
                            new RenderUtil(drawContext, bounds.x() + 8.0F, bounds.y() + 8.0F, bounds.w() - 16.0F, 12.0F)
                                .textMiddleRight(Fonts.INTER_REGULAR, string5, 0.0F, 0.45F, ColorUtil.multiplyAlpha(i3, f5), 5.5F);
                        }

                        if (!bounds.compact()) {
                            int i5 = flag4 ? i3 : Theme.getSecondaryText();
                            new RenderUtil(drawContext, bounds.x() + 24.0F + f6, bounds.y() + 20.0F, bounds.w() - 32.0F, 8.0F)
                                .scrollingTextMiddle(Fonts.INTER_MEDIUM, string6, 0.0F, 0.0F, bounds.w() - 32.0F, ColorUtil.multiplyAlpha(i5, f5), 5.5F);
                        }

                        SettingWidgets.drawSlider(
                            drawContext,
                            bounds.x() + 8.0F,
                            f9,
                            bounds.w() - 16.0F,
                            2.0F,
                            this.progressAnimation.get(),
                            6.0F + this.knobHoverAnimation.get(),
                            this.dragAnimation.get(),
                            f5 * (!flag && !flag2 ? 0.5F : 1.0F)
                        );
                        if (this.safeBuy.getMarketPrice() > 0L) {
                            new RenderUtil(drawContext, f11 - 0.5F, f9 - 2.0F, 1.0F, 6.0F).rect(ColorUtil.multiplyAlpha(i3, f5 * f14), Radius.all(0.5F));
                        }
                    }
                }
            );
    }

    private void renderShieldIcon(DrawContext drawContext, float f, float f2, boolean flag, float f3) {
        int i = flag ? ColorUtil.multiplyAlpha(Theme.getPrimary(), 0.16F * f3) : ColorUtil.multiplyAlpha(Theme.getTextColor(), 0.06F * f3);
        int i2 = flag ? ColorUtil.multiplyAlpha(Theme.getPrimary(), f3) : ColorUtil.multiplyAlpha(Theme.getTextColor(), 0.5F * f3);
        new RenderUtil(drawContext, f + 2.0F, f2 + 2.0F, 8.0F, 8.0F).rect(i, Radius.all(4.0F));
        new RenderUtil(drawContext, f + 3.0F, f2 + 3.0F, 6.0F, 6.0F).icon(Icon.SHIELD, 0.0F, 0.0F, 6.0F, i2);
        new RenderUtil(drawContext, f, f2, 12.0F, 12.0F).circle(1.25F, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), 0.2F * f3));
        float f4 = 360.0F * this.progressAnimation.get();
        if (f4 > 0.5F && this.activeAnimation.get() > 0.01F) {
            new RenderUtil(drawContext, f, f2, 12.0F, 12.0F).arc(0.0F, f4, 1.25F, Theme.getPrimary(), this.activeAnimation.get() * f3);
        }
    }

    private long getPriceAt(double d, SafeBuyLimitWidget.Bounds bounds) {
        float f = bounds.w() - 22.0F;
        double d2 = d - bounds.x() - 11.0;
        long l = this.safeBuy.getMarketPrice();
        if (l > 0L) {
            double d3 = Math.abs(d2 - this.safeBuy.getLimitFraction(l) * f);
            if (bounds.compact()) {
                if (d3 <= 2) {
                    return l;
                }
            } else if (d3 <= 3) {
                return l;
            }
        }

        return this.safeBuy.fractionToLimit(d2 / f);
    }

    private boolean isOverSlider(SafeBuyLimitWidget.Bounds bounds, double d) {
        float f = bounds.y() + (bounds.compact() ? 27 : 35);
        return d >= f - 7.0F && d <= f + 9.0F;
    }

    private SafeBuyLimitWidget.Bounds getBounds(int i, int i2) {
        float f = i - 8;
        boolean flag = f < 100.0F;
        float f2 = this.expandAnimation.get();
        float f3 = MathHelper.lerp(f2, this.collapsedWidthAnimation.get(), MathHelper.clamp(f, 64.0F, 136.0F));
        float f4 = MathHelper.lerp(f2, 20.0F, flag ? 40.0F : 48.0F);
        return new SafeBuyLimitWidget.Bounds(i - 4 - f3, i2 + 10, f3, f4, flag);
    }

    private float calcCollapsedWidth(String string, boolean flag) {
        float f = flag ? 4.0F + Fonts.INTER_REGULAR.getWidth(Language.translate("auctionhelper.safeBuy.perUnitShort", "/шт"), 5.5F) : 0.0F;
        return 20.0F + Fonts.INTER_SEMIBOLD.getWidth(string, 6.5F) + f + 8.0F;
    }

    private String getNoLimitText() {
        return Language.translate("auctionhelper.safeBuy.noLimit", "Без лимита");
    }

    private static String formatShortPrice(long l) {
        if (l < 1000L) {
            return Long.toString(l);
        }

        boolean flag;
        double d;
        double d2;
        if (l >= 999950L) {
            flag = true;
            d = l;
            d2 = 100000.0;
        } else {
            flag = false;
            d = l;
            d2 = 100.0;
        }

        double d3 = Math.round(d / d2) / 10.0;
        String string = d3 == Math.rint(d3) ? Long.toString((long)d3) : String.format(Locale.US, "%.1f", d3);
        return flag
            ? String.format(Language.translate("auctionhelper.safeBuy.millions", "%sкк"), string)
            : String.format(Language.translate("auctionhelper.safeBuy.thousands", "%sк"), string);
    }

    public SafeBuyLimitWidget(SafeBuyLimitTracker safeBuyLimitTracker) {
        this.safeBuy = safeBuyLimitTracker;
    }

    record Bounds(float x, float y, float w, float h, boolean compact) {
        boolean contains(double d, double d2) {
            return d >= this.x && d <= this.x + this.w && d2 >= this.y && d2 <= this.y + this.h;
        }

        SafeBuyLimitWidget.Bounds grow(float f) {
            return new SafeBuyLimitWidget.Bounds(this.x - f, this.y - f, this.w + f * 2.0F, this.h + f * 2.0F, this.compact);
        }
    }
}
