package tech.sadnes.client.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easing;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;

public final class RangeSetting extends ValueSetting<float[]> {
    private static final float TRACK_HEIGHT = 2.0F;
    private static final float KNOB_SIZE = 5.0F;
    private static final float HIT_HEIGHT = 8.0F;
    private static final float MIN_TRACK_WIDTH = 20.0F;
    private static final float MIN_HEIGHT = 24.0F;
    private static final float CHIP_GAP = 2.0F;
    private static final float TWO_LEVEL_MIN_HEIGHT = 22.0F;
    private final float min;
    private final float max;
    private final float step;
    private final Animation lowAnimation = new Animation();
    private final Animation highAnimation = new Animation();
    private final Animation lowDragAnimation = new Animation();
    private final Animation highDragAnimation = new Animation();
    private final Map<ValueBind<float[]>, Animation> bindLowAnimations = new IdentityHashMap<>();
    private final Map<ValueBind<float[]>, Animation> bindHighAnimations = new IdentityHashMap<>();
    private final Map<ValueBind<float[]>, Animation> bindLowDragAnimations = new IdentityHashMap<>();
    private final Map<ValueBind<float[]>, Animation> bindHighDragAnimations = new IdentityHashMap<>();
    private Unit unit;
    private boolean dragging;
    private boolean draggingLow;
    private ValueBind<float[]> draggingBind;
    private float cachedChipWidth = -1.0F;

    public RangeSetting(String string, float f, float f2, float f3, float f4, float f5) {
        this(string, f, f2, f3, f4, f5, Unit.EMPTY);
    }

    public RangeSetting(String string, float f, float f2, float f3, float f4, float f5, Unit unit2) {
        super(string, new float[]{f, f2});
        this.min = Math.min(f3, f4);
        this.max = Math.max(f3, f4);
        this.step = Math.max(f5, 0.0F);
        this.unit = unit2 != null ? unit2 : Unit.EMPTY;
        this.setValue(new float[]{f, f2});
    }

    public RangeSetting(GroupSetting groupSetting, String string, float f, float f2, float f3, float f4, float f5) {
        this(groupSetting, string, f, f2, f3, f4, f5, Unit.EMPTY);
    }

    public RangeSetting(GroupSetting groupSetting, String string, float f, float f2, float f3, float f4, float f5, Unit unit2) {
        this(string, f, f2, f3, f4, f5, unit2);
        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public float getMin() {
        return this.min;
    }

    public float getMax() {
        return this.max;
    }

    public float getStep() {
        return this.step;
    }

    public Unit getUnit() {
        return this.unit;
    }

    public float getLow() {
        return this.getValue()[0];
    }

    public float getHigh() {
        return this.getValue()[1];
    }

    public RangeSetting onInput(Unit unit2) {
        if (unit2 != null) {
            this.unit = unit2;
            this.cachedChipWidth = -1.0F;
            return this;
        } else {
            this.unit = Unit.EMPTY;
            this.cachedChipWidth = -1.0F;
            return this;
        }
    }

    public RangeSetting setValue(float[] floats) {
        super.setValue(floats);
        return this;
    }

    public RangeSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public RangeSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public String getDisplayValue() {
        float[] floats = this.getValue();
        return this.formatValue(floats[0]) + " – " + this.formatValue(floats[1]) + this.unit.getSuffix();
    }

    @Override
    public String formatBindValue(ValueBind<float[]> valueBind) {
        float[] floats = this.getBindRange(valueBind);
        return this.formatValue(floats[0]) + " – " + this.formatValue(floats[1]) + this.unit.getSuffix();
    }

    protected float[] sanitizeValue(float[] floats) {
        float f = floats != null && floats.length > 0 ? floats[0] : this.min;
        float f2 = floats != null && floats.length > 1 ? floats[1] : this.max;
        float f3 = this.snap(f);
        float f4 = this.snap(f2);
        if (f3 > f4) {
            float f5 = f3;
            f3 = f4;
            f4 = f5;
        }

        float f6 = this.getMinGap();
        if (f4 - f3 < f6) {
            f4 = Math.min(this.max, f3 + f6);
            f3 = Math.min(f3, f4 - f6);
        }

        return new float[]{f3, f4};
    }

    @Override
    public float resolveHeight(float f) {
        return Math.max(f, 24.0F);
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        ValueBind valueBind = this.editingBind;
        boolean flag = valueBind != null;
        float[] floats = flag ? this.getBindRange(valueBind) : this.getValue();
        RangeSetting.Layout layout = this.computeLayout();
        float f2;
        float f3;
        float f4;
        float f5;
        if (flag) {
            this.pruneBindAnimations();
            boolean flag2 = this.draggingBind == valueBind;
            f2 = this.animateTo(
                this.getBindAnimation(this.bindLowAnimations, valueBind, this.toProgress(floats[0])), this.toProgress(floats[0]), 0.25F, Easings.EXPO_OUT
            );
            f3 = this.animateTo(
                this.getBindAnimation(this.bindHighAnimations, valueBind, this.toProgress(floats[1])), this.toProgress(floats[1]), 0.25F, Easings.EXPO_OUT
            );
            f4 = this.animateTo(
                this.getBindAnimation(this.bindLowDragAnimations, valueBind, 0.0F), flag2 && this.draggingLow ? 1.0F : 0.0F, 0.15F, Easings.QUAD_OUT
            );
            f5 = this.animateTo(
                this.getBindAnimation(this.bindHighDragAnimations, valueBind, 0.0F), flag2 && !this.draggingLow ? 1.0F : 0.0F, 0.15F, Easings.QUAD_OUT
            );
        } else {
            f2 = this.animateTo(this.lowAnimation, this.toProgress(floats[0]), 0.25F, Easings.EXPO_OUT);
            f3 = this.animateTo(this.highAnimation, this.toProgress(floats[1]), 0.25F, Easings.EXPO_OUT);
            f4 = this.animateTo(this.lowDragAnimation, this.dragging && this.draggingLow ? 1.0F : 0.0F, 0.15F, Easings.QUAD_OUT);
            f5 = this.animateTo(this.highDragAnimation, this.dragging && !this.draggingLow ? 1.0F : 0.0F, 0.15F, Easings.QUAD_OUT);
            float f6 = layout.twoLevel() ? layout.chipX() - this.position.x - 4.0F : layout.trackX() - this.position.x - 3.0F;
            float f7 = layout.twoLevel() ? layout.chipY() : this.position.y;
            float f8 = layout.twoLevel() ? layout.chipH() : this.size.y;
            this.drawLabel(drawContext, this.position.x, f7, this.size.x, f8, f6, this.isHovered(i, i2, this.position, this.size));
        }

        SettingWidgets.drawSurfaceLabel(
            drawContext, layout.chipX(), layout.chipY(), layout.chipW(), layout.chipH(), flag ? this.formatBindValue(valueBind) : this.getDisplayValue(), 0.35F, this.getAlpha()
        );
        SettingWidgets.drawRangeSlider(drawContext, layout.trackX(), layout.trackY(), layout.trackW(), 2.0F, f2, f3, 5.0F, f4, f5, this.getAlpha());
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        ValueBind valueBind = this.editingBind;
        if (!this.isLeftButton(click.button())) {
            return false;
        } else {
            RangeSetting.Layout layout = this.computeLayout();
            float f = layout.trackY() - 3.0F;
            if (!this.isHovered(click.x(), click.y(), layout.trackX(), f, layout.trackW(), 8.0F)) {
                return false;
            } else {
                float[] floats = valueBind != null ? this.getBindRange(valueBind) : this.getValue();
                float f2 = Math.max(layout.trackW() - 5.0F, 1.0E-4F);
                float f3 = layout.trackX() + 2.5F + f2 * this.toProgress(floats[0]);
                float f4 = layout.trackX() + 2.5F + f2 * this.toProgress(floats[1]);
                float f5 = (float)Math.abs(click.x() - f3);
                float f6 = (float)Math.abs(click.x() - f4);
                this.draggingLow = f5 < f6 || f5 == f6 && click.x() < f3;
                if (valueBind != null) {
                    this.draggingBind = valueBind;
                    this.updateFromMouse(valueBind, click.x(), layout);
                    return true;
                } else {
                    this.dragging = true;
                    this.updateFromMouse(valueBind, click.x(), layout);
                    return true;
                }
            }
        }
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        ValueBind valueBind = this.editingBind;
        if (!this.isLeftButton(click.button())) {
            return false;
        }

        if (valueBind != null) {
            this.draggingBind = valueBind;
            this.updateFromMouse(valueBind, click.x(), this.computeLayout());
            return true;
        }

        if (!this.dragging) {
            return false;
        }

        this.updateFromMouse(null, click.x(), this.computeLayout());
        return true;
    }

    @Override
    public boolean onMouseRelease(Click click) {
        this.dragging = false;
        this.draggingBind = null;
        return false;
    }

    @Override
    public void close() {
        this.dragging = false;
        this.draggingBind = null;
    }

    private void updateFromMouse(ValueBind<float[]> valueBind, double d, RangeSetting.Layout layout) {
        float f = Math.max(layout.trackW() - 5.0F, 1.0E-4F);
        float f2 = (float)((d - layout.trackX() - 2.5) / f);
        float f3 = Math.max(0.0F, Math.min(1.0F, f2));
        float f4 = this.min + (this.max - this.min) * f3;
        float f5 = Math.max(this.getMinGap(), (this.max - this.min) * 5.0F / f);
        float[] floats = valueBind != null ? this.getBindRange(valueBind) : this.getValue();
        float[] floats2 = this.draggingLow ? new float[]{Math.min(f4, floats[1] - f5), floats[1]} : new float[]{floats[0], Math.max(f4, floats[0] + f5)};
        if (valueBind != null) {
            super.setBindValue(valueBind, floats2);
        } else {
            this.setValue(floats2);
        }
    }

    private float[] getBindRange(ValueBind<float[]> valueBind) {
        return valueBind != null && valueBind.getValue() != null ? this.sanitizeValue((float[])valueBind.getValue()) : new float[]{this.min, this.max};
    }

    private float getMinGap() {
        return this.step > 0.0F ? this.step : (this.max - this.min) * 0.01F;
    }

    private float snap(float f) {
        float f2 = Math.max(this.min, Math.min(this.max, f));
        if (this.step <= 0.0F) {
            return f2;
        }

        float f3 = Math.round((f2 - this.min) / this.step);
        return Math.max(this.min, Math.min(this.max, this.min + f3 * this.step));
    }

    private float toProgress(float f) {
        float f2 = (f - this.min) / Math.max(this.max - this.min, 1.0E-4F);
        return Math.max(0.0F, Math.min(1.0F, f2));
    }

    private float animateTo(Animation animation, float f, float f2, Easing easing) {
        animation.update();
        animation.animate(f, f2, easing);
        return animation.get();
    }

    private Animation getBindAnimation(Map<ValueBind<float[]>, Animation> map, ValueBind<float[]> valueBind, float f) {
        return map.computeIfAbsent(valueBind, valueBind2 -> {
            Animation animation = new Animation();
            animation.forceValue(f);
            return animation;
        });
    }

    private void pruneBindAnimations() {
        this.bindLowAnimations.keySet().removeIf(valueBind -> !this.getBinds().contains(valueBind));
        this.bindHighAnimations.keySet().removeIf(valueBind2 -> !this.getBinds().contains(valueBind2));
        this.bindLowDragAnimations.keySet().removeIf(valueBind3 -> !this.getBinds().contains(valueBind3));
        this.bindHighDragAnimations.keySet().removeIf(valueBind4 -> !this.getBinds().contains(valueBind4));
    }

    private float computeChipWidth() {
        if (this.cachedChipWidth < 0.0F) {
            String string = (this.formatValue(this.max) + " – " + this.formatValue(this.max)).replaceAll("\\d", "0");
            String string2 = this.unit.getSuffix();
            float f = this.measureText(string) + this.measureText(string2);
            if (string2.isEmpty()) {
                this.cachedChipWidth = f + 8.0F;
                return this.cachedChipWidth;
            }

            this.cachedChipWidth = f + 9.5F;
        }

        return this.cachedChipWidth;
    }

    private RangeSetting.Layout computeLayout() {
        boolean flag = this.editingBind != null;
        float f = this.position.x;
        float f2 = this.position.y;
        float f3 = this.size.x;
        float f4 = this.size.y;
        float f5 = this.computeChipWidth();
        if (!flag && f4 >= 22.0F) {
            float f6 = 10.0F + 2.0F + 5.0F;
            float f7 = f2 + (f4 - f6) / 2.0F;
            float f8 = f + f3 - f5;
            float f9 = f7 + 10.0F + 2.0F + 1.5F;
            return new RangeSetting.Layout(true, f8, f7, f5, 10.0F, f, f9, Math.max(20.0F, f3));
        } else {
            float f10 = f + f3 - f5 - (flag ? 4.0F : 0.0F);
            float f11 = f2 + f4 / 2.0F - 10.0F / 2.0F;
            float f12 = flag ? f + 4.0F : this.computeTrackStart(f, f10);
            float f13 = Math.max(12.0F, f10 - f12 - 6.0F);
            float f14 = f2 + f4 / 2.0F - 1.0F;
            return new RangeSetting.Layout(false, f10, f11, f5, 10.0F, f12, f14, f13);
        }
    }

    private float computeTrackStart(float f, float f2) {
        float f3 = f2 - 6.0F;
        float f4 = this.getFont().getWidth(Language.translate(this.getName()), 6.0F);
        float f5 = Math.max(0.0F, f3 - 8.0F - 20.0F - f);
        return f + Math.min(f4, f5) + 8.0F;
    }

    private MsdfFont getFont() {
        return Fonts.INTER_MEDIUM != null ? Fonts.INTER_MEDIUM : UI_FONT;
    }

    private float measureText(String string) {
        return string != null && !string.isEmpty() ? this.getFont().getWidth(string, 5.5F) : 0.0F;
    }

    private int getDecimalPlaces() {
        if (this.step <= 0.0F) {
            return -1;
        }

        BigDecimal bigDecimal = new BigDecimal(Float.toString(this.step)).stripTrailingZeros();
        return Math.max(0, bigDecimal.scale());
    }

    private String formatValue(float f) {
        int i = this.getDecimalPlaces();
        if (i < 0) {
            return f == (int)f ? Integer.toString((int)f) : Float.toString(f);
        } else {
            return new BigDecimal(Float.toString(f)).setScale(i, RoundingMode.HALF_UP).toPlainString();
        }
    }

    @Override
    protected float getControlWidth() {
        return this.computeChipWidth();
    }

    record Layout(boolean twoLevel, float chipX, float chipY, float chipW, float chipH, float trackX, float trackY, float trackW) {
    }
}
