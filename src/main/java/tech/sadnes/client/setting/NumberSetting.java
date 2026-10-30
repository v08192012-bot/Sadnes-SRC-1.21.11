package tech.sadnes.client.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.BindMode;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;

public final class NumberSetting extends ValueSetting<Float> {
    private static final float TRACK_HEIGHT = 2.0F;
    private static final float KNOB_SIZE = 5.0F;
    private static final float TRACK_HIT_HEIGHT = 8.0F;
    private static final float MIN_TRACK_WIDTH = 20.0F;
    private static final float MIN_HEIGHT = 24.0F;
    private static final float CHIP_GAP = 2.0F;
    private static final float TWO_LEVEL_HEIGHT = 22.0F;
    private final float min;
    private final float max;
    private final float step;
    private final Vector2f textPosition = new Vector2f();
    private final Map<ValueBind<Float>, Animation> bindValueAnimations = new IdentityHashMap<>();
    private final Map<ValueBind<Float>, Animation> bindDragAnimations = new IdentityHashMap<>();
    private final Map<ValueBind<Float>, TextFieldComponent> bindTextFields = new IdentityHashMap<>();
    private final Animation dragAnimation = new Animation();
    private TextFieldComponent textField;
    private Unit unit;
    private boolean dragging;
    private ValueBind<Float> draggingBind;
    private float cachedChipWidth = -1.0F;

    public NumberSetting(String string, float f, float f2, float f3, float f4) {
        this(string, f, f2, f3, f4, Unit.EMPTY);
    }

    public NumberSetting(String string, float f, float f2, float f3, float f4, Unit unit2) {
        super(string, f);
        this.min = Math.min(f2, f3);
        this.max = Math.max(f2, f3);
        this.step = Math.max(f4, 0.0F);
        this.unit = unit2 != null ? unit2 : Unit.EMPTY;
        this.setValue(Float.valueOf(f));
    }

    public NumberSetting(GroupSetting groupSetting, String string, float f, float f2, float f3, float f4) {
        this(groupSetting, string, f, f2, f3, f4, Unit.EMPTY);
    }

    public NumberSetting(GroupSetting groupSetting, String string, float f, float f2, float f3, float f4, Unit unit2) {
        this(string, f, f2, f3, f4, unit2);
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

    public NumberSetting onInput(Unit unit2) {
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

    public NumberSetting setValue(Float floatValue) {
        super.setValue(floatValue);
        return this;
    }

    public NumberSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public NumberSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public NumberSetting setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    public NumberSetting bind(int i, BindMode bindMode, float f) {
        super.mouseDragged(i, bindMode, f);
        return this;
    }

    public NumberSetting addBind(ValueBind<Float> valueBind) {
        super.addBind(valueBind);
        return this;
    }

    public String getFormattedValue() {
        return this.format(this.getValue()) + this.unit.getSuffix();
    }

    protected Float sanitizeValue(Float floatValue) {
        if (floatValue == null) {
            return this.min;
        }

        float f = Math.max(this.min, Math.min(this.max, floatValue));
        if (this.step <= 0.0F) {
            return f;
        }

        float f2 = Math.round((f - this.min) / this.step);
        float f3 = this.min + f2 * this.step;
        return Math.max(this.min, Math.min(this.max, f3));
    }

    @Override
    public String formatBindValue(ValueBind<Float> valueBind) {
        return this.format(this.getBindValue(valueBind)) + this.unit.getSuffix();
    }

    @Override
    public float resolveHeight(float f) {
        return Math.max(f, 24.0F);
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        ValueBind valueBind = this.editingBind;
        boolean flag = valueBind != null;
        float f2 = flag ? this.getBindValue(valueBind) : this.getValue();
        float f3 = this.normalize(f2);
        NumberSetting.Layout layout = this.computeLayout();
        TextFieldComponent textFieldComponent = this.getTextField(valueBind);
        if (!textFieldComponent.isFocused()) {
            this.syncTextField(textFieldComponent, f2);
        }

        String string = flag ? this.formatBindValue(valueBind) : this.getFormattedValue();
        float f4;
        float f5;
        if (flag) {
            this.cleanupBindState();
            f4 = this.animateBindValue(valueBind, f3);
            f5 = this.animateDrag(this.bindDragAnimations.computeIfAbsent(valueBind, valueBind2 -> new Animation()), this.draggingBind == valueBind);
        } else {
            this.valueAnimation.update();
            this.valueAnimation.animate(f3, 0.25, Easings.EXPO_OUT);
            f4 = this.valueAnimation.get();
            f5 = this.animateDrag(this.dragAnimation, this.dragging);
        }

        if (!flag) {
            float f6 = layout.twoLevel() ? layout.chipX() - this.position.x - 4.0F : layout.trackX() - this.position.x - 3.0F;
            float f7 = layout.twoLevel() ? layout.chipY() : this.position.y;
            float f8 = layout.twoLevel() ? layout.chipH() : this.size.y;
            this.drawLabel(drawContext, this.position.x, f7, this.size.x, f8, f6, this.isHovered(i, i2, this.position, this.size));
        }

        SettingWidgets.drawSurfaceLabel(drawContext, layout.chipX(), layout.chipY(), layout.chipW(), layout.chipH(), textFieldComponent.isFocused() ? "" : string, 0.35F, this.getAlpha());
        if (textFieldComponent.isFocused()) {
            this.renderTextField(drawContext, textFieldComponent, layout.chipX(), layout.chipY(), layout.chipW(), layout.chipH());
        }

        SettingWidgets.drawSlider(drawContext, layout.trackX(), layout.trackY(), layout.trackW(), 2.0F, f4, 5.0F, f5, this.getAlpha());
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        ValueBind valueBind = this.editingBind;
        if (!this.isLeftButton(click.button())) {
            return false;
        }

        boolean flag2 = valueBind != null;
        float f = flag2 ? this.getBindValue(valueBind) : this.getValue();
        NumberSetting.Layout layout = this.computeLayout();
        TextFieldComponent textFieldComponent = this.getTextField(valueBind);
        boolean flag3 = textFieldComponent.isFocused();
        if (this.isHovered(click.x(), click.y(), layout.chipX(), layout.chipY(), layout.chipW(), layout.chipH())) {
            if (!flag3) {
                this.syncTextField(textFieldComponent, f);
            }

            textFieldComponent.setFocused(true);
            if (!flag3) {
                textFieldComponent.setReplaceOnInput(true);
                this.dragging = false;
                return true;
            } else {
                textFieldComponent.setReplaceOnInput(false);
                this.dragging = false;
                return true;
            }
        } else {
            if (flag3) {
                this.applyTextInput(valueBind, textFieldComponent);
                textFieldComponent.setFocused(false);
                textFieldComponent.setReplaceOnInput(false);
                this.syncTextField(textFieldComponent, flag2 ? this.getBindValue(valueBind) : this.getValue());
            }

            float f2 = layout.trackY() - 3.0F;
            if (!this.isHovered(click.x(), click.y(), layout.trackX(), f2, layout.trackW(), 8.0F)) {
                return false;
            } else if (flag2) {
                this.draggingBind = valueBind;
                this.setFromMouse(valueBind, click.x(), layout);
                return true;
            } else {
                this.dragging = true;
                this.setFromMouse(valueBind, click.x(), layout);
                return true;
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
            this.setFromMouse(valueBind, click.x(), this.computeLayout());
            return true;
        }

        if (!this.dragging) {
            return false;
        }

        this.setFromMouse(null, click.x(), this.computeLayout());
        return true;
    }

    @Override
    public boolean onMouseRelease(Click click) {
        this.dragging = false;
        this.draggingBind = null;
        return false;
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        ValueBind valueBind = this.editingBind;
        TextFieldComponent textFieldComponent = this.getTextField(valueBind);
        if (!textFieldComponent.isFocused()) {
            return false;
        }

        if (keyInput.key() == 256) {
            textFieldComponent.setFocused(false);
            textFieldComponent.setReplaceOnInput(false);
            if (valueBind != null) {
                this.syncTextField(textFieldComponent, this.getBindValue(valueBind));
                return true;
            } else {
                this.syncTextField(textFieldComponent, this.getValue());
                return true;
            }
        } else if (keyInput.key() != 257 && keyInput.key() != 335) {
            textFieldComponent.keyPressed(keyInput.key());
            return true;
        } else {
            this.applyTextInput(valueBind, textFieldComponent);
            textFieldComponent.setFocused(false);
            textFieldComponent.setReplaceOnInput(false);
            if (valueBind != null) {
                this.syncTextField(textFieldComponent, this.getBindValue(valueBind));
                return true;
            } else {
                this.syncTextField(textFieldComponent, this.getValue());
                return true;
            }
        }
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        ValueBind valueBind = this.editingBind;
        TextFieldComponent textFieldComponent = this.getTextField(valueBind);
        if (!textFieldComponent.isFocused()) {
            return false;
        }

        textFieldComponent.charTyped((char)charInput.codepoint());
        return true;
    }

    @Override
    public void close() {
        this.dragging = false;
        this.draggingBind = null;
        if (this.textField != null) {
            this.textField.setFocused(false);
            this.textField.setReplaceOnInput(false);
        }

        for (TextFieldComponent textFieldComponent : this.bindTextFields.values()) {
            textFieldComponent.setFocused(false);
            textFieldComponent.setReplaceOnInput(false);
        }
    }

    @Override
    public boolean isTyping() {
        if (this.textField != null && this.textField.isFocused()) {
            return true;
        }

        Iterator iterator2 = this.bindTextFields.values().iterator();

        while (iterator2.hasNext()) {
            if (((TextFieldComponent)iterator2.next()).isFocused()) {
                return true;
            }
        }

        return false;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    private float getChipWidth() {
        if (this.cachedChipWidth < 0.0F) {
            String string = this.format(this.max);
            String string2 = this.format(this.min);
            String string3 = string.length() >= string2.length() ? string : string2;
            String string4 = string3.replaceAll("\\d", "0");
            String string5 = this.unit.getSuffix();
            float f = this.getTextWidth(string4) + this.getTextWidth(string5);
            if (string5.isEmpty()) {
                this.cachedChipWidth = f + 8.0F;
                return this.cachedChipWidth;
            }

            this.cachedChipWidth = f + 9.5F;
        }

        return this.cachedChipWidth;
    }

    private TextFieldComponent getTextField(ValueBind<Float> valueBind) {
        if (valueBind == null) {
            if (this.textField == null) {
                this.textField = new TextFieldComponent(this.getFont(), 5.5F, "", false, true);
            }

            return this.textField;
        } else {
            return this.bindTextFields.computeIfAbsent(valueBind, valueBind2 -> new TextFieldComponent(this.getFont(), 5.5F, "", false, true));
        }
    }

    private void renderTextField(DrawContext drawContext, TextFieldComponent textFieldComponent, float f, float f2, float f3, float f4) {
        String string = this.unit.getSuffix();
        MsdfFont msdfFont = this.getFont();
        float f5 = string.isEmpty() ? 0.0F : this.getTextWidth(string);
        float f6 = string.isEmpty() ? 0.0F : 1.5F;
        float f7 = Math.max(4.0F, f3 - 8.0F - f5 - f6);
        this.textPosition.set(f + 4.0F, f2 + f4 / 2.0F - 2.75F);
        textFieldComponent.render(drawContext, this.textPosition, this.getSecondaryColor(), f7);
        if (!string.isEmpty()) {
            new RenderUtil(drawContext, f, f2, f3, f4).textMiddleLeft(msdfFont, string, 4.0F + f7 + f6, 0.0F, this.getSecondaryColor(), 6.5F);
        }
    }

    private MsdfFont getFont() {
        return Fonts.INTER_MEDIUM != null ? Fonts.INTER_MEDIUM : UI_FONT;
    }

    private float getTextWidth(String string) {
        if (string != null && !string.isEmpty()) {
            MsdfFont msdfFont = this.getFont();
            return msdfFont != null ? msdfFont.getWidth(string, 5.5F) : string.length() * 5.5F * 0.5F;
        } else {
            return 0.0F;
        }
    }

    private void syncTextField(TextFieldComponent textFieldComponent, float f) {
        String string = this.format(f);
        if (!string.equals(textFieldComponent.getText())) {
            textFieldComponent.setText(string);
            textFieldComponent.setCursor(string.length());
        }
    }

    private void applyTextInput(ValueBind<Float> valueBind, TextFieldComponent textFieldComponent) {
        String string = textFieldComponent.getText().trim().replace(',', '.');
        if (!string.isEmpty() && !string.equals("-") && !string.equals(".") && !string.equals("-.")) {
            try {
                float f = Float.parseFloat(string);
                if (!Float.isFinite(f)) {
                    return;
                }

                if (valueBind != null) {
                    super.setBindValue(valueBind, f);
                } else {
                    this.setValue(Float.valueOf(f));
                }
            } catch (NumberFormatException numberFormatException) {
            }
        }
    }

    private float getTrackStart(boolean flag, float f, float f2) {
        if (flag) {
            return f + 4.0F;
        }

        float f3 = f2 - 6.0F;
        MsdfFont msdfFont = this.getFont();
        float f4 = msdfFont != null ? msdfFont.getWidth(this.getName(), 6.0F) : 0.0F;
        float f5 = Math.max(0.0F, f3 - 8.0F - 20.0F - f);
        float f6 = Math.min(f4, f5);
        return f + f6 + 8.0F;
    }

    private NumberSetting.Layout computeLayout() {
        boolean flag = this.editingBind != null;
        float f = this.position.x;
        float f2 = this.position.y;
        float f3 = this.size.x;
        float f4 = this.size.y;
        float f5 = this.getChipWidth();
        if (!flag && f4 >= 22.0F) {
            float f6 = 10.0F + 2.0F + 5.0F;
            float f7 = f2 + (f4 - f6) / 2.0F;
            float f8 = f + f3 - f5;
            float f9 = Math.max(20.0F, f3);
            float f10 = f7 + 10.0F + 2.0F + 1.5F;
            return new NumberSetting.Layout(true, f8, f7, f5, 10.0F, f, f10, f9);
        } else {
            float f11 = f + f3 - f5 - (flag ? 4.0F : 0.0F);
            float f12 = f2 + f4 / 2.0F - 10.0F / 2.0F;
            float f13 = this.getTrackStart(flag, f, f11);
            float f14 = Math.max(12.0F, f11 - f13 - 6.0F);
            float f15 = f2 + f4 / 2.0F - 1.0F;
            return new NumberSetting.Layout(false, f11, f12, f5, 10.0F, f13, f15, f14);
        }
    }

    private void setFromMouse(ValueBind<Float> valueBind, double d, NumberSetting.Layout layout) {
        float f = Math.max(layout.trackW() - 5.0F, 1.0E-4F);
        float f2 = (float)((d - layout.trackX() - 2.5) / f);
        float f3 = Math.max(0.0F, Math.min(1.0F, f2));
        float f4 = this.min + (this.max - this.min) * f3;
        if (valueBind != null) {
            super.setBindValue(valueBind, f4);
        } else {
            this.setValue(Float.valueOf(f4));
        }
    }

    private float normalize(float f) {
        float f2 = (f - this.min) / Math.max(this.max - this.min, 1.0E-4F);
        return Math.max(0.0F, Math.min(1.0F, f2));
    }

    private float animateBindValue(ValueBind<Float> valueBind, float f) {
        if (valueBind == null) {
            return f;
        }

        Animation animation = this.bindValueAnimations.computeIfAbsent(valueBind, valueBind2 -> {
            Animation animation2 = new Animation();
            animation2.forceValue(f);
            return animation2;
        });
        animation.update();
        animation.animate(f, 0.25, Easings.EXPO_OUT);
        return animation.get();
    }

    private float animateDrag(Animation animation, boolean flag) {
        animation.update();
        if (flag) {
            animation.animate(1.0, 0.15F, Easings.QUAD_OUT);
            return animation.get();
        } else {
            animation.animate(0.0, 0.15F, Easings.QUAD_OUT);
            return animation.get();
        }
    }

    private void cleanupBindState() {
        this.bindValueAnimations.keySet().removeIf(valueBind -> !this.getBinds().contains(valueBind));
        this.bindDragAnimations.keySet().removeIf(valueBind2 -> !this.getBinds().contains(valueBind2));
        this.bindTextFields.keySet().removeIf(valueBind3 -> !this.getBinds().contains(valueBind3));
    }

    private int getDecimalPlaces() {
        if (this.step <= 0.0F) {
            return -1;
        }

        BigDecimal bigDecimal = new BigDecimal(Float.toString(this.step)).stripTrailingZeros();
        return Math.max(0, bigDecimal.scale());
    }

    private String format(float f) {
        float f2 = this.sanitizeValue(f);
        int i = this.getDecimalPlaces();
        if (i < 0) {
            return f2 == (int)f2 ? Integer.toString((int)f2) : Float.toString(f2);
        }

        BigDecimal bigDecimal = new BigDecimal(Float.toString(f2)).setScale(i, RoundingMode.HALF_UP);
        return bigDecimal.toPlainString();
    }

    private float getBindValue(ValueBind<Float> valueBind) {
        return valueBind != null && valueBind.getValue() != null ? this.sanitizeValue((Float)valueBind.getValue()) : this.min;
    }

    @Override
    protected float getControlWidth() {
        return this.getChipWidth();
    }

    record Layout(boolean twoLevel, float chipX, float chipY, float chipW, float chipH, float trackX, float trackY, float trackW) {
    }
}
