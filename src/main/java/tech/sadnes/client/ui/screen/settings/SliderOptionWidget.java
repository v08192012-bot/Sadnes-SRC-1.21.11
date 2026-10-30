package tech.sadnes.client.ui.screen.settings;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.ui.clickgui.component.OptionWidget;
import tech.sadnes.client.util.OptionsUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class SliderOptionWidget implements OptionWidget {
    public static final SliderOptionWidget.ValueFormatter PERCENT_FORMAT = f -> Math.round(f * 100.0F) + "%";
    public static final SliderOptionWidget.ValueFormatter INTEGER_FORMAT = f -> Integer.toString(Math.round(f));
    private static final float TRACK_HEIGHT = 3.0F;
    private static final float KNOB_SIZE = 7.0F;
    private static final float VALUE_WIDTH = 36.0F;
    private static final float VALUE_GAP = 6.0F;
    private final OptionsUtil.SliderOption option;
    private final float min;
    private final float max;
    private final float step;
    private final SliderOptionWidget.ValueFormatter formatter;
    private final Animation progressAnimation = new Animation();
    private final Animation dragAnimation = new Animation();
    private boolean dragging;
    private float x;
    private float y;
    private float height;
    private float trackWidth;

    public SliderOptionWidget(OptionsUtil.SliderOption sliderOption, float f, float f2, float f3, SliderOptionWidget.ValueFormatter valueFormatter) {
        this.option = sliderOption;
        this.min = f;
        this.max = f2;
        this.step = f3;
        this.formatter = valueFormatter;
    }

    private float getProgress() {
        float f = this.max - this.min;
        return f <= 0.0F ? 0.0F : clamp01((this.option.get() - this.min) / f);
    }

    @Override
    public void renderWidget(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2, float f5) {
        float f6 = Math.max(f3 - 36.0F - 6.0F, 10.0F);
        this.x = f;
        this.y = f2;
        this.height = f4;
        this.trackWidth = f6;
        float f7 = f2 + f4 / 2.0F - 1.5F;
        this.progressAnimation.update();
        this.progressAnimation.animate(this.getProgress(), 0.18, Easings.EXPO_OUT);
        this.dragAnimation.update();
        this.dragAnimation.animate(this.dragging ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f8 = this.progressAnimation.get();
        float f9 = this.dragAnimation.get();
        GuiRenderUtil.drawSurface(drawContext, f, f7, f6, 3.0F, f5, 1.5F);
        float f10 = Math.max(f6 * f8, 3.0F);
        new RenderUtil(drawContext, f, f7, f10, 3.0F).rect(Theme.withAlpha(Theme.getPrimary(), f5), Radius.all(1.5F));
        float f11 = 7.0F * (1.0F + 0.3F * f9);
        float f12 = 7.0F * (1.0F - 0.25F * f9);
        float f13 = f + f6 * f8 - f11 / 2.0F;
        float f14 = Math.max(f, Math.min(f13, f + f6 - f11));
        float f15 = f7 + 1.5F - f12 / 2.0F;
        new RenderUtil(drawContext, f14, f15, f11, f12).border(3.0F, Theme.getPrimary(), 0.35F * f5, Radius.all(f12 / 2.0F));
        new RenderUtil(drawContext, f14, f15, f11, f12).rect(Theme.withAlpha(ColorUtil.WHITE, f5), Radius.all(f12 / 2.0F));
        new RenderUtil(drawContext, f + f6 + 6.0F, f2, 36.0F, f4)
            .textMiddleRight(Fonts.INTER_SEMIBOLD, this.formatter.format(this.option.get()), 0.0F, 0.0F, Theme.withAlpha(Theme.getPrimary(), f5), 7.0F);
    }

    @Override
    public boolean mouseClicked(double d, double d2) {
        if (d >= this.x - 3.0F && d <= this.x + this.trackWidth + 3.0F && d2 >= this.y && d2 <= this.y + this.height) {
            this.dragging = true;
            this.setFromMouse(d);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseReleased(double d, double d2) {
        if (!this.dragging) {
            return false;
        }

        this.setFromMouse(d);
        return true;
    }

    @Override
    public void update() {
        if (this.dragging) {
            this.dragging = false;
            this.option.commit();
        }
    }

    private void setFromMouse(double d) {
        float f = clamp01((float)((d - this.x) / Math.max(this.trackWidth, 1.0E-4F)));
        float f2 = this.min + (this.max - this.min) * f;
        if (this.step > 0.0F) {
            f2 = Math.round(f2 / this.step) * this.step;
        }

        this.option.set(f2);
    }

    @Override
    public float getWidgetHeight() {
        return 116.0F;
    }

    @Override
    public void reset() {
        this.option.reset();
    }

    @Override
    public boolean isFocused() {
        return !this.option.isDefault();
    }

    @Override
    public boolean keyPressed(int i) {
        if (i == 0) {
            return false;
        }

        float f = this.step > 0.0F ? this.step : (this.max - this.min) / 20.0F;
        float f2 = this.option.get() + i * f;
        this.option.set(Math.max(this.min, Math.min(this.max, f2)));
        this.option.commit();
        return true;
    }

    @Override
    public String getText() {
        return Float.toString(this.option.get());
    }

    @Override
    public void setText(String string) {
        try {
            this.option.set(Float.parseFloat(string));
            this.option.commit();
        } catch (Exception exception) {
        }
    }

    private static float clamp01(float f) {
        if (f < 0.0F) {
            return 0.0F;
        } else {
            return f > 1.0F ? 1.0F : f;
        }
    }

    @FunctionalInterface
    public interface ValueFormatter {
        String format(float f);
    }
}
