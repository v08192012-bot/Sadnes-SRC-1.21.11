package tech.sadnes.client.setting;

import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.BindMode;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.RoundedRectRenderer;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public class ColorSetting extends ValueSetting<Integer> {
    protected static final float SWATCH_SIZE = 10.0F;
    private static final float PICKER_SIZE = 85.0F;
    private static final float BAR_WIDTH = 4.0F;
    private static final float PADDING = 4.5F;
    private static final float BAR_GAP = 3.0F;
    private static final float SECTION_GAP = 6.0F;
    private static final float TAB_HEIGHT = 11.0F;
    private static final float ROW_GAP = 5.0F;
    private static final float INPUT_HEIGHT = 12.0F;
    private static final float PRESET_SIZE = 9.0F;
    private static final int CORNER_RADIUS = 6;
    private static final int CHANNEL_COUNT = 3;
    private static final ColorSetting.InputFormat[] FORMATS = ColorSetting.InputFormat.values();
    private static final int[] PRESET_COLORS = new int[]{
        ColorUtil.rgb(255, 255, 255),
        ColorUtil.rgb(235, 64, 52),
        ColorUtil.rgb(240, 140, 30),
        ColorUtil.rgb(245, 205, 40),
        ColorUtil.rgb(60, 200, 90),
        ColorUtil.rgb(40, 195, 205),
        ColorUtil.rgb(60, 120, 245),
        ColorUtil.rgb(150, 90, 230)
    };
    private final Animation popupAnimation = new Animation();
    private final Animation tabAnimation = new Animation();
    private final Animation inputFadeAnimation = new Animation();
    private final Animation hexFocusAnimation = new Animation();
    private final Animation copyHoverAnimation = new Animation();
    private final Animation[] tabHoverAnimations = new Animation[]{new Animation(), new Animation(), new Animation()};
    private final Animation[] channelFocusAnimations = new Animation[]{new Animation(), new Animation(), new Animation()};
    private final Animation[] presetHoverAnimations = new Animation[PRESET_COLORS.length];
    private final Animation[] presetSelectedAnimations = new Animation[PRESET_COLORS.length];
    private final boolean alphaEnabled;
    private boolean pickerOpen;
    private boolean draggingPicker;
    private boolean draggingHue;
    private boolean draggingAlpha;
    private float popupX;
    private float popupY;
    private float hue;
    private float saturation;
    private float brightness;
    private float alpha;
    private float lastHue;
    private ValueBind<Integer> openedBind;
    private ColorSetting.InputFormat inputFormat;
    private TextFieldComponent hexField;
    private final TextFieldComponent[] channelFields;
    private TextFieldComponent focusedField;

    public ColorSetting(String string, int i) {
        this(null, string, null, true, i);
    }

    public ColorSetting(String string) {
        this(null, string, null, true, -1);
    }

    public ColorSetting(GroupSetting groupSetting, String string, int i) {
        this(groupSetting, string, null, true, i);
    }

    public ColorSetting(GroupSetting groupSetting, String string) {
        this(groupSetting, string, null, true, -1);
    }

    public ColorSetting(String string, Icon icon, boolean flag, int i) {
        this(null, string, icon, flag, i);
    }

    public ColorSetting(GroupSetting groupSetting, String string, Icon icon, boolean flag, int i) {
        super(string, flag ? i : i | 0xFF000000);

        for (int i2 = 0; i2 < PRESET_COLORS.length; i2++) {
            this.presetHoverAnimations[i2] = new Animation();
            this.presetSelectedAnimations[i2] = new Animation();
        }

        this.inputFormat = ColorSetting.InputFormat.HEX;
        this.channelFields = new TextFieldComponent[3];
        this.alphaEnabled = flag;
        if (icon != null) {
            this.setIcon(icon);
        }

        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public ColorSetting setValue(Integer integer) {
        super.setValue(integer);
        return this;
    }

    public ColorSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public ColorSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public ColorSetting setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    public ColorSetting withBind(int i, BindMode bindMode, int i2) {
        super.mouseDragged(i, bindMode, i2);
        return this;
    }

    public ColorSetting addBind(ValueBind<Integer> valueBind) {
        super.addBind(valueBind);
        return this;
    }

    public int getRed() {
        return ColorUtil.red(this.getValue());
    }

    public int getGreen() {
        return ColorUtil.green(this.getValue());
    }

    public int getBlue() {
        return ColorUtil.blue(this.getValue());
    }

    public int getAlphaComponent() {
        return ColorUtil.alpha(this.getValue());
    }

    public float getAlphaFloat() {
        return this.getAlphaComponent() / 255.0F;
    }

    public float getHue() {
        return this.getHsb()[0];
    }

    public float getSaturation() {
        return this.getHsb()[1];
    }

    public float getBrightness() {
        return this.getHsb()[2];
    }

    public ColorSetting setHue(float f) {
        float[] floats = this.getHsb();
        return this.setHsba(f, floats[1], floats[2], this.getAlphaFloat());
    }

    public ColorSetting setSaturation(float f) {
        float[] floats = this.getHsb();
        return this.setHsba(floats[0], f, floats[2], this.getAlphaFloat());
    }

    public ColorSetting setBrightness(float f) {
        float[] floats = this.getHsb();
        return this.setHsba(floats[0], floats[1], f, this.getAlphaFloat());
    }

    public ColorSetting setAlphaFloat(float f) {
        return this.setValue(ColorUtil.setAlphaf(this.getValue(), f));
    }

    public ColorSetting setRgba(int i, int i2, int i3, int i4) {
        return this.setValue(ColorUtil.rgba(i, i2, i3, i4));
    }

    public ColorSetting setRgb(int i, int i2, int i3) {
        return this.setRgba(i, i2, i3, this.getAlphaComponent());
    }

    public ColorSetting setRgbKeepAlpha(int i) {
        return this.setValue(ColorUtil.replaceAlpha(i, this.getAlphaComponent()));
    }

    public ColorSetting setHsb(float f, float f2, float f3) {
        return this.setHsba(f, f2, f3, this.getAlphaFloat());
    }

    public ColorSetting setHsba(float f, float f2, float f3, float f4) {
        return this.setValue(ColorUtil.fromHsba(f, f2, f3, f4));
    }

    public String toHexString(boolean flag) {
        return flag ? String.format("#%08X", this.getValue()) : String.format("#%02X%02X%02X", this.getRed(), this.getGreen(), this.getBlue());
    }

    public ColorSetting setHex(String string) {
        Integer integer = parseHex(string);
        if (integer != null) {
            this.setValue(integer);
        }

        return this;
    }

    protected Integer sanitizeValue(Integer integer) {
        if (integer == null) {
            return -1;
        } else {
            return this.alphaEnabled ? integer : integer | 0xFF000000;
        }
    }

    @Override
    public String formatBindValue(ValueBind<Integer> valueBind) {
        int i = this.resolveBindColor(valueBind);
        return String.format("#%02X%02X%02X", ColorUtil.red(i), ColorUtil.green(i), ColorUtil.blue(i));
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        ValueBind valueBind = this.editingBind;
        float f2 = this.position.x;
        float f3 = this.position.y;
        float f4 = this.size.x;
        float f5 = this.size.y;
        boolean flag = valueBind != null;
        int i3 = flag ? this.resolveBindColor(valueBind) : this.getValue();
        if (!flag) {
            float f6 = f4 - 10.0F - 6.0F;
            this.drawLabel(drawContext, f2, f3, f4, f5, f6, this.isHovered(i, i2, this.position, this.size));
        }

        float f7 = f2 + f4 - 10.0F - (flag ? 4.0F : 0.0F);
        float f8 = f3 + f5 / 2.0F - 5.0F;
        SettingWidgets.drawColorSwatch(drawContext, f7, f8, 10.0F, 10.0F, i3, this.getAlpha());
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (!this.isLeftButton(click.button()) && !this.isRightButton(click.button())) {
            return false;
        } else if (this.pickerOpen) {
            return this.handlePickerClick(click);
        } else if (this.isHovered(click.x(), click.y(), this.position.x, this.position.y, this.size.x, this.size.y)) {
            this.openPicker();
            return true;
        } else {
            return false;
        }
    }

    protected final boolean isPickerOpen() {
        return this.pickerOpen;
    }

    protected final boolean handlePickerClick(Click click) {
        if (this.handlePopupClick(click, this.editingBind)) {
            return true;
        }

        this.pickerOpen = false;
        this.draggingPicker = this.draggingHue = this.draggingAlpha = false;
        this.unfocusFields();
        return true;
    }

    protected final void openPicker() {
        this.pickerOpen = true;
        this.openedBind = this.editingBind;
        this.syncPickerFromValue();
        this.resetPickerState();
    }

    protected final void syncPickerFromValue() {
        int i = this.editingBind != null ? this.resolveBindColor(this.editingBind) : this.getValue();
        float[] floats = ColorUtil.toHsb(i);
        if (floats[1] > 0.001F && floats[2] > 0.001F) {
            this.lastHue = floats[0];
        }

        this.hue = this.lastHue;
        this.saturation = floats[1];
        this.brightness = floats[2];
        this.alpha = this.alphaEnabled ? ColorUtil.alphaf(i) : 1.0F;
        this.draggingPicker = this.draggingHue = this.draggingAlpha = false;
        this.unfocusFields();
        this.syncInputFields();
    }

    protected float getExtraHeaderHeight() {
        return 0.0F;
    }

    protected void renderExtraHeader(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
    }

    protected boolean handleExtraHeaderClick(Click click, float f, float f2, float f3) {
        return false;
    }

    @Override
    public void renderOverlay(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.updatePopupAnimation();
        if (this.editingBind == null && !(f2 <= 0.001F)) {
            this.renderPicker(drawContext, i, i2, f, f2);
        }
    }

    @Override
    public boolean isOverlayOpen() {
        return this.editingBind == null && this.isPopupVisible();
    }

    @Override
    public boolean isTyping() {
        return this.pickerOpen && this.focusedField != null && this.focusedField.isFocused();
    }

    public boolean isMouseOverPicker(double d, double d2) {
        return this.isPopupVisible() && (this.draggingPicker || this.draggingHue || this.draggingAlpha || this.isInsidePopup(d, d2));
    }

    @Override
    public boolean isBindDropdownOpen() {
        return this.isPopupVisible();
    }

    @Override
    public void renderBindDropdown(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.updatePopupAnimation();
        if (!(f2 <= 0.001F)) {
            this.renderPicker(drawContext, i, i2, f, f2);
        }
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (!this.pickerOpen || !this.isLeftButton(click.button())) {
            return false;
        }

        if (!this.draggingPicker && !this.draggingHue && !this.draggingAlpha) {
            return false;
        }

        this.updateFromDrag(click.x(), click.y());
        return true;
    }

    @Override
    public boolean onMouseRelease(Click click) {
        this.draggingPicker = this.draggingHue = this.draggingAlpha = false;
        return this.pickerOpen;
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (!this.pickerOpen) {
            return false;
        }

        int i = keyInput.key();
        if (this.focusedField != null && this.focusedField.isFocused()) {
            switch (i) {
                case 256:
                case 257:
                case 335:
                    this.unfocusFields();
                    return true;
                default:
                    this.focusedField.keyPressed(i);
                    this.applyFocusedInput();
                    return true;
            }
        } else {
            if (i != 256) {
                return false;
            }

            this.close();
            return true;
        }
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        if (this.pickerOpen && this.focusedField != null && this.focusedField.isFocused()) {
            char c = (char)charInput.codepoint();
            if (this.focusedField == this.hexField) {
                if (isHexChar(c) && this.focusedField.getText().length() < 6) {
                    this.focusedField.charTyped(Character.toUpperCase(c));
                    this.applyHexInput();
                    return true;
                }
            } else if (c >= '0' && c <= '9' && this.focusedField.getText().length() < 3) {
                this.focusedField.charTyped(c);
                this.applyChannelInputs();
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() {
        this.pickerOpen = false;
        this.draggingPicker = this.draggingHue = this.draggingAlpha = false;
        this.openedBind = null;
        this.unfocusFields();
    }

    public void closeImmediately() {
        this.close();
        this.popupAnimation.forceValue(0.0);
    }

    private float[] getHsb() {
        return ColorUtil.toHsb(this.getValue());
    }

    private int resolveBindColor(ValueBind<Integer> valueBind) {
        return valueBind != null && valueBind.getValue() != null ? this.sanitizeValue((Integer)valueBind.getValue()) : -1;
    }

    private int getPickerColor() {
        return ColorUtil.fromHsba(this.hue, this.saturation, this.brightness, this.alpha);
    }

    private void renderPicker(DrawContext drawContext, int i, int i2, float f, float f2) {
        if (!(f2 <= 0.001F)) {
            int i3 = this.getPickerColor();
            float f3 = this.getPopupWidth();
            float f4 = this.getPopupHeight();
            this.popupX = GuiRenderUtil.placeBeside(this.position.x, this.position.x + this.size.x, f3, 4.0F);
            this.popupY = GuiRenderUtil.clampToScreenY(this.position.y + this.size.y / 2.0F - f4 / 2.0F, f4);
            float f5 = this.popupX + 4.5F;
            float f6 = f5 + 85.0F + 3.0F;
            float f7 = f6 + 4.0F + 3.0F;
            float f8 = this.getPickerY();
            float f9 = this.getAlpha() * Math.min(f2, 1.0F);
            int i4 = ColorUtil.fromHsb(this.hue, 1.0F, 1.0F);
            float f10 = 0.85F + 0.15F * f2;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, this.popupX + f3 / 2.0F, this.popupY + f4 / 2.0F, f10);
            GuiRenderUtil.drawPopup(drawContext, this.popupX, this.popupY, f3, f4, f9, 10.0F);
            this.renderExtraHeader(drawContext, f5, this.popupY + 4.5F, this.getContentWidth(), f9, i, i2);
            RoundedRectRenderer.draw(
                drawContext, new Matrix3x2f(drawContext.getMatrices()), f5, f8, 85.0F, 85.0F, Radius.all(6.0F), 0.0F, 0.0F, f9, i4, i4, i4, i4, 2
            );
            this.drawTexture(drawContext, Texture.COLOR_HUE, f6, f8, 4.0F, 85.0F, f9, Radius.all(2.0F));
            if (this.alphaEnabled) {
                this.drawTexture(drawContext, Texture.COLOR_ALPHA, f7, f8, 4.0F, 85.0F, f9, Radius.all(2.0F));
                new RenderUtil(drawContext, f7, f8, 4.0F, 85.0F)
                    .rect(
                        ColorUtil.setAlpha(i3, 0),
                        ColorUtil.setAlpha(i3, 255),
                        ColorUtil.setAlpha(i3, 0),
                        ColorUtil.setAlpha(i3, 255),
                        f9,
                        Radius.all(2.0F)
                    );
            }

            float f11 = f8 + this.hue * 85.0F - 1.0F;
            float f12 = f5 + this.saturation * 85.0F - 3.0F;
            float f13 = f8 + (1.0F - this.brightness) * 85.0F - 3.0F;
            int i5 = ColorUtil.multiplyAlpha(ColorUtil.WHITE, f9);
            new RenderUtil(drawContext, f6 - 1.0F, f11, 6.0F, 2.0F).rect(i5, Radius.all(1.0F));
            if (this.alphaEnabled) {
                float f14 = f8 + (1.0F - this.alpha) * 85.0F - 1.0F;
                new RenderUtil(drawContext, f7 - 1.0F, f14, 6.0F, 2.0F).rect(i5, Radius.all(1.0F));
            }

            new RenderUtil(drawContext, f12, f13, 6.0F, 6.0F).rect(i5, Radius.all(3.0F));
            this.renderControls(drawContext, f5, f8, i3, f9, i, i2);
            MatrixUtil.pop(drawContext);
        }
    }

    private void renderControls(DrawContext drawContext, float f, float f2, int i, float f3, int i2, int i3) {
        float f4 = this.getContentWidth();
        float f5 = f2 + 85.0F + 6.0F;
        float f6 = f5 + 11.0F + 5.0F;
        float f7 = f6 + 12.0F + 5.0F;
        this.renderFormatTabs(drawContext, f, f5, f4, f3, i2, i3);
        this.renderInputRow(drawContext, f, f6, f4, i, f3, i2, i3);
        this.renderPresets(drawContext, f, f7, f4, f3, i2, i3);
    }

    private void renderFormatTabs(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        this.tabAnimation.update();
        this.tabAnimation.animate(this.inputFormat.ordinal(), 0.28F, Easings.EXPO_OUT);
        GuiRenderUtil.drawSurface(drawContext, f, f2, f3, 11.0F, f4, 5.5F);
        float f5 = f3 / FORMATS.length;
        float f6 = f + this.tabAnimation.get() * f5;
        new RenderUtil(drawContext, f6 + 1.0F, f2 + 1.0F, f5 - 2.0F, 9.0F)
            .rect(ColorUtil.multiplyAlpha(Theme.getPrimary(), 0.85F * f4), Radius.all(5.5F - 1.0F));
        int i3 = ColorUtil.multiplyAlpha(contrastTextColor(Theme.getPrimary()), f4);

        for (int i4 = 0; i4 < FORMATS.length; i4++) {
            boolean flag = i4 == this.inputFormat.ordinal();
            boolean flag2 = this.isHovered(i, i2, f + i4 * f5, f2, f5, 11.0);
            this.tabHoverAnimations[i4].update();
            this.tabHoverAnimations[i4].animate(flag2 && !flag ? 1.0 : 0.0, 0.18F, Easings.QUAD_OUT);
            int i5 = ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), this.tabHoverAnimations[i4].get());
            int i6 = flag ? i3 : ColorUtil.multiplyAlpha(i5, f4);
            new RenderUtil(drawContext, f + i4 * f5, f2, f5, 11.0F).textCenter(Fonts.INTER_MEDIUM, FORMATS[i4].label, i6, 5.5F, false);
        }
    }

    private void renderInputRow(DrawContext drawContext, float f, float f2, float f3, int i, float f4, int i2, int i3) {
        this.inputFadeAnimation.update();
        this.inputFadeAnimation.animate(1.0, 0.25, Easings.QUART_OUT);
        float f5 = f4 * clamp01(this.inputFadeAnimation.get());
        this.syncInputFields();
        float f6 = f + f3 - 12.0F;
        float f7 = f3 - 12.0F - 3.0F;
        if (this.inputFormat == ColorSetting.InputFormat.HEX) {
            this.renderHexInput(drawContext, f, f2, f7, i, f5);
        } else {
            this.renderChannelInputs(drawContext, f, f2, f7, f5, i2, i3);
        }

        this.renderCopyButton(drawContext, f6, f2, f4, i2, i3);
    }

    private void renderHexInput(DrawContext drawContext, float f, float f2, float f3, int i, float f4) {
        GuiRenderUtil.drawSurface(drawContext, f, f2, f3, 12.0F, f4, 4.0F);
        this.hexFocusAnimation.update();
        this.hexFocusAnimation.animate(this.hexField != null && this.hexField.isFocused() ? 1.0 : 0.0, 0.2F, Easings.QUAD_OUT);
        if (this.hexFocusAnimation.get() > 0.001F) {
            new RenderUtil(drawContext, f, f2, f3, 12.0F)
                .shadow(0.6F, ColorUtil.multiplyAlpha(Theme.getPrimary(), this.hexFocusAnimation.get() * f4), Radius.all(4.0F));
        }

        float f5 = f + 2.5F;
        float f6 = f2 + 6.0F - 9.0F / 2.0F;
        SettingWidgets.drawColorSwatch(drawContext, f5, f6, 9.0F, 9.0F, i, f4);
        float f7 = f5 + 9.0F + 2.0F;
        new RenderUtil(drawContext, f7, f2, 6.0F, 12.0F)
            .textMiddleLeft(Fonts.INTER_MEDIUM, "#", 0.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f4), 5.5F);
        float f8 = f7 + Fonts.INTER_MEDIUM.getWidth("#", 5.5F) + 1.0F;
        float f9 = f + f3 - f8 - 3.0F;
        TextFieldComponent textFieldComponent = this.getHexField();
        textFieldComponent.setWidth(f9);
        float f10 = f2 + 6.0F - 2.75F;
        textFieldComponent.render(drawContext, new Vector2f(f8, f10), ColorUtil.multiplyAlpha(Theme.getTextColor(), f4), f9);
    }

    private void renderChannelInputs(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        String[] strings = this.getChannelLabels();
        float f5 = (f3 - 3.0F * 2.0F) / 3.0F;
        float f6 = f2 + 6.0F - 2.75F;

        for (int i3 = 0; i3 < 3; i3++) {
            float f7 = f + i3 * (f5 + 3.0F);
            GuiRenderUtil.drawSurface(drawContext, f7, f2, f5, 12.0F, f4, 4.0F);
            TextFieldComponent textFieldComponent = this.getChannelField(i3);
            this.channelFocusAnimations[i3].update();
            this.channelFocusAnimations[i3].animate(textFieldComponent.isFocused() ? 1.0 : 0.0, 0.2F, Easings.QUAD_OUT);
            if (this.channelFocusAnimations[i3].get() > 0.001F) {
                new RenderUtil(drawContext, f7, f2, f5, 12.0F)
                    .shadow(0.6F, ColorUtil.multiplyAlpha(Theme.getPrimary(), this.channelFocusAnimations[i3].get() * f4), Radius.all(4.0F));
            }

            float f8 = f7 + 3.0F;
            new RenderUtil(drawContext, f8, f2, 6.0F, 12.0F)
                .textMiddleLeft(Fonts.INTER_MEDIUM, strings[i3], 0.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f4), 5.5F);
            float f9 = f8 + Fonts.INTER_MEDIUM.getWidth(strings[i3], 5.5F) + 2.0F;
            float f10 = f7 + f5 - f9 - 3.0F;
            textFieldComponent.setWidth(f10);
            textFieldComponent.render(drawContext, new Vector2f(f9, f6), ColorUtil.multiplyAlpha(Theme.getTextColor(), f4), f10);
        }
    }

    private void renderCopyButton(DrawContext drawContext, float f, float f2, float f3, int i, int i2) {
        boolean flag = this.isHovered(i, i2, f, f2, 12.0, 12.0);
        this.copyHoverAnimation.update();
        this.copyHoverAnimation.animate(flag ? 1.0 : 0.0, 0.18F, Easings.QUAD_OUT);
        GuiRenderUtil.drawSurface(drawContext, f, f2, 12.0F, 12.0F, f3, 4.0F);
        if (this.copyHoverAnimation.get() > 0.001F) {
            new RenderUtil(drawContext, f, f2, 12.0F, 12.0F)
                .rect(ColorUtil.multiplyAlpha(Theme.getWhite(), 0.07F * this.copyHoverAnimation.get() * f3), Radius.all(4.0F));
        }

        float f4 = (12.0F - 7.0F) / 2.0F;
        int i3 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), this.copyHoverAnimation.get()), f3);
        new RenderUtil(drawContext, f, f2, 12.0F, 12.0F).icon(Icon.COPY, f4, f4, 7.0F, i3);
    }

    private void renderPresets(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        float f5 = PRESET_COLORS.length > 1 ? (f3 - PRESET_COLORS.length * 9.0F) / (PRESET_COLORS.length - 1) : 0.0F;
        Radius radius = Radius.all(3.0F);

        for (int i3 = 0; i3 < PRESET_COLORS.length; i3++) {
            float f6 = f + i3 * (9.0F + f5);
            boolean flag = this.isHovered(i, i2, f6, f2, 9.0, 9.0);
            this.presetHoverAnimations[i3].update();
            this.presetHoverAnimations[i3].animate(flag ? 1.0 : 0.0, 0.18F, Easings.QUAD_OUT);
            this.presetSelectedAnimations[i3].update();
            this.presetSelectedAnimations[i3].animate(this.matchesPickerColor(PRESET_COLORS[i3]) ? 1.0 : 0.0, 0.2F, Easings.QUAD_OUT);
            float f7 = this.presetHoverAnimations[i3].get();
            new RenderUtil(drawContext, f6 - f7, f2 - f7, 9.0F + f7 * 2.0F, 9.0F + f7 * 2.0F)
                .rect(ColorUtil.multiplyAlpha(PRESET_COLORS[i3], f4), radius)
                .shadow(0.5F, ColorUtil.multiplyAlpha(Theme.getOutline(), 0.4F * f4), radius);
            if (this.presetSelectedAnimations[i3].get() > 0.001F) {
                float f8 = this.presetSelectedAnimations[i3].get();
                new RenderUtil(drawContext, f6 - 1.0F, f2 - 1.0F, 11.0F, 11.0F)
                    .shadow(1.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f8 * f4), Radius.all(4.0F));
            }
        }
    }

    private boolean handlePopupClick(Click click, ValueBind<Integer> valueBind) {
        if (!this.isLeftButton(click.button())) {
            return this.isInsidePopup(click.x(), click.y());
        }

        float f = this.popupX + 4.5F;
        float f2 = f + 85.0F + 3.0F;
        float f3 = f2 + 4.0F + 3.0F;
        float f4 = this.getPickerY();
        if (this.handleExtraHeaderClick(click, f, this.popupY + 4.5F, this.getContentWidth())) {
            return true;
        }

        this.draggingPicker = this.isHovered(click.x(), click.y(), f, f4, 85.0, 85.0);
        this.draggingHue = !this.draggingPicker && this.isHovered(click.x(), click.y(), f2 - 1.0F, f4, 6.0, 85.0);
        this.draggingAlpha = this.alphaEnabled && !this.draggingPicker && !this.draggingHue && this.isHovered(click.x(), click.y(), f3 - 1.0F, f4, 6.0, 85.0);
        if (this.draggingPicker || this.draggingHue || this.draggingAlpha) {
            this.unfocusFields();
            this.updateFromDrag(click.x(), click.y());
            return true;
        }

        if (this.handleFormatClick(click)) {
            return true;
        }

        boolean flag = this.isInsidePopup(click.x(), click.y());
        if (flag) {
            this.unfocusFields();
        }

        return flag;
    }

    private boolean handleFormatClick(Click click) {
        float f = this.popupX + 4.5F;
        float f2 = this.getPickerY();
        float f3 = this.getContentWidth();
        float f4 = f2 + 85.0F + 6.0F;
        float f5 = f4 + 11.0F + 5.0F;
        float f6 = f5 + 12.0F + 5.0F;
        float f7 = f3 / FORMATS.length;
        if (this.isHovered(click.x(), click.y(), f, f4, f3, 11.0)) {
            int i = (int)((click.x() - f) / f7);
            if (i >= 0 && i < FORMATS.length) {
                this.setInputFormat(FORMATS[i]);
            }

            return true;
        } else {
            float f8 = f + f3 - 12.0F;
            if (this.isHovered(click.x(), click.y(), f8, f5, 12.0, 12.0)) {
                this.copyToClipboard();
                return true;
            } else {
                float f9 = f3 - 12.0F - 3.0F;
                if (this.editingBind == null && this.handleInputClick(click, f, f5, f9)) {
                    return true;
                } else {
                    int i2 = this.getPresetAt(click.x(), click.y(), f, f6, f3);
                    if (i2 >= 0) {
                        this.applyPreset(PRESET_COLORS[i2]);
                        this.unfocusFields();
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    private boolean handleInputClick(Click click, float f, float f2, float f3) {
        if (this.inputFormat == ColorSetting.InputFormat.HEX) {
            if (!this.isHovered(click.x(), click.y(), f, f2, f3, 12.0)) {
                return false;
            }

            this.focusField(this.getHexField());
            return true;
        } else {
            float f4 = (f3 - 3.0F * 2.0F) / 3.0F;

            for (int i = 0; i < 3; i++) {
                float f5 = f + i * (f4 + 3.0F);
                if (this.isHovered(click.x(), click.y(), f5, f2, f4, 12.0)) {
                    this.focusField(this.getChannelField(i));
                    return true;
                }
            }

            return false;
        }
    }

    private int getPresetAt(double d, double d2, float f, float f2, float f3) {
        float f4 = PRESET_COLORS.length > 1 ? (f3 - PRESET_COLORS.length * 9.0F) / (PRESET_COLORS.length - 1) : 0.0F;

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            float f5 = f + i * (9.0F + f4);
            if (this.isHovered(d, d2, f5, f2, 9.0, 9.0)) {
                return i;
            }
        }

        return -1;
    }

    private void setInputFormat(ColorSetting.InputFormat inputFormat2) {
        if (this.inputFormat != inputFormat2) {
            this.inputFormat = inputFormat2;
            this.unfocusFields();
            this.inputFadeAnimation.forceValue(0.0);
            this.syncInputFields();
        }
    }

    private void applyPreset(int i) {
        float[] floats = ColorUtil.toHsb(i);
        if (floats[1] > 0.001F && floats[2] > 0.001F) {
            this.lastHue = floats[0];
        }

        this.hue = floats[0];
        this.saturation = floats[1];
        this.brightness = floats[2];
        this.applyColor(this.getPickerColor());
    }

    private boolean matchesPickerColor(int i) {
        int i2 = this.getPickerColor();
        return ColorUtil.red(i2) == ColorUtil.red(i) && ColorUtil.green(i2) == ColorUtil.green(i) && ColorUtil.blue(i2) == ColorUtil.blue(i);
    }

    private void updateFromDrag(double d, double d2) {
        float f = this.popupX + 4.5F;
        float f2 = this.getPickerY();
        float f3 = this.hue;
        float f4 = this.saturation;
        float f5 = this.brightness;
        float f6 = this.alpha;
        if (this.draggingHue) {
            f3 = clamp01((float)((d2 - f2) / 85.0));
            this.hue = f3;
            this.lastHue = f3;
        }

        if (this.draggingAlpha) {
            f6 = 1.0F - clamp01((float)((d2 - f2) / 85.0));
        }

        if (this.draggingPicker) {
            f4 = clamp01((float)((d - f) / 85.0));
            f5 = 1.0F - clamp01((float)((d2 - f2) / 85.0));
            if (f4 > 0.001F && f5 > 0.001F) {
                this.lastHue = this.hue;
            }
        }

        this.hue = f3;
        this.saturation = f4;
        this.brightness = f5;
        this.alpha = f6;
        this.applyColor(ColorUtil.fromHsba(f3, f4, f5, f6));
    }

    private void applyColor(int i) {
        ValueBind valueBind = this.getActiveBind();
        if (valueBind != null) {
            super.setBindValue(valueBind, i);
        } else {
            this.setValue(i);
        }
    }

    private void applyFocusedInput() {
        if (this.focusedField == this.hexField) {
            this.applyHexInput();
        } else {
            this.applyChannelInputs();
        }
    }

    private void applyHexInput() {
        Integer integer = parseHex(this.hexField.getText());
        if (integer != null) {
            float[] floats = ColorUtil.toHsb(integer);
            if (floats[1] > 0.001F && floats[2] > 0.001F) {
                this.lastHue = floats[0];
            }

            this.hue = floats[0];
            this.saturation = floats[1];
            this.brightness = floats[2];
            this.applyColor(this.getPickerColor());
        }
    }

    private void applyChannelInputs() {
        if (this.inputFormat == ColorSetting.InputFormat.RGB) {
            int i = clamp(parseIntOrZero(this.getChannelField(0).getText()), 0, 255);
            int i2 = clamp(parseIntOrZero(this.getChannelField(1).getText()), 0, 255);
            int i3 = clamp(parseIntOrZero(this.getChannelField(2).getText()), 0, 255);
            float[] floats = ColorUtil.toHsb(ColorUtil.rgb(i, i2, i3));
            if (floats[1] > 0.001F && floats[2] > 0.001F) {
                this.lastHue = floats[0];
            }

            this.hue = floats[0];
            this.saturation = floats[1];
            this.brightness = floats[2];
            this.applyColor(this.getPickerColor());
        } else {
            if (this.inputFormat == ColorSetting.InputFormat.HSB) {
                float f = clamp(parseIntOrZero(this.getChannelField(0).getText()), 0, 360) / 360.0F;
                float f2 = clamp(parseIntOrZero(this.getChannelField(1).getText()), 0, 100) / 100.0F;
                float f3 = clamp(parseIntOrZero(this.getChannelField(2).getText()), 0, 100) / 100.0F;
                if (f2 > 0.001F && f3 > 0.001F) {
                    this.lastHue = f;
                }

                this.hue = f;
                this.saturation = f2;
                this.brightness = f3;
            }

            this.applyColor(this.getPickerColor());
        }
    }

    private void syncInputFields() {
        int i = this.getPickerColor();
        if (this.inputFormat == ColorSetting.InputFormat.HEX) {
            TextFieldComponent textFieldComponent = this.getHexField();
            if (!textFieldComponent.isFocused()) {
                this.setFieldText(textFieldComponent, String.format("%02X%02X%02X", ColorUtil.red(i), ColorUtil.green(i), ColorUtil.blue(i)));
            }
        } else if (this.inputFormat == ColorSetting.InputFormat.RGB) {
            this.setChannelText(0, ColorUtil.red(i));
            this.setChannelText(1, ColorUtil.green(i));
            this.setChannelText(2, ColorUtil.blue(i));
        } else {
            this.setChannelText(0, Math.round(this.hue * 360.0F));
            this.setChannelText(1, Math.round(this.saturation * 100.0F));
            this.setChannelText(2, Math.round(this.brightness * 100.0F));
        }
    }

    private void setChannelText(int i, int i2) {
        TextFieldComponent textFieldComponent = this.getChannelField(i);
        if (!textFieldComponent.isFocused()) {
            this.setFieldText(textFieldComponent, Integer.toString(i2));
        }
    }

    private void setFieldText(TextFieldComponent textFieldComponent, String string) {
        textFieldComponent.setText(string);
        if (textFieldComponent.getCursor() > string.length()) {
            textFieldComponent.setCursor(string.length());
        }
    }

    private String[] getChannelLabels() {
        return this.inputFormat == ColorSetting.InputFormat.RGB ? new String[]{"R", "G", "B"} : new String[]{"H", "S", "B"};
    }

    private TextFieldComponent getHexField() {
        if (this.hexField == null) {
            this.hexField = new TextFieldComponent(Fonts.INTER_MEDIUM, 5.5F, "", false, false);
        }

        return this.hexField;
    }

    private TextFieldComponent getChannelField(int i) {
        if (this.channelFields[i] == null) {
            this.channelFields[i] = new TextFieldComponent(Fonts.INTER_MEDIUM, 5.5F, "", false, true);
        }

        return this.channelFields[i];
    }

    private void focusField(TextFieldComponent textFieldComponent) {
        if (this.focusedField != null && this.focusedField != textFieldComponent) {
            this.focusedField.setFocused(false);
        }

        this.focusedField = textFieldComponent;
        if (textFieldComponent != null) {
            textFieldComponent.setFocused(true);
            textFieldComponent.setReplaceOnInput(false);
            textFieldComponent.setCursor(textFieldComponent.getText().length());
        }
    }

    private void unfocusFields() {
        if (this.focusedField != null) {
            this.focusedField.setFocused(false);
        }

        this.focusedField = null;
    }

    private void copyToClipboard() {
        int i = this.getPickerColor();
        MinecraftClient.getInstance().keyboard.setClipboard(String.format("#%02X%02X%02X", ColorUtil.red(i), ColorUtil.green(i), ColorUtil.blue(i)));
    }

    private void resetPickerState() {
        this.inputFormat = ColorSetting.InputFormat.HEX;
        this.tabAnimation.forceValue(0.0);
        this.inputFadeAnimation.forceValue(1.0);
        this.hexFocusAnimation.forceValue(0.0);
        this.copyHoverAnimation.forceValue(0.0);

        for (int i = 0; i < 3; i++) {
            this.tabHoverAnimations[i].forceValue(0.0);
            this.channelFocusAnimations[i].forceValue(0.0);
        }

        for (int i2 = 0; i2 < PRESET_COLORS.length; i2++) {
            this.presetHoverAnimations[i2].forceValue(0.0);
            this.presetSelectedAnimations[i2].forceValue(0.0);
        }

        this.unfocusFields();
        this.syncInputFields();
    }

    private void drawTexture(DrawContext drawContext, Texture texture, float f, float f2, float f3, float f4, float f5, Radius radius) {
        GuiRenderUtil.bindTexture(texture);
        new RenderUtil(drawContext, f, f2, f3, f4).frostedGlass(f5, radius);
    }

    private boolean isInsidePopup(double d, double d2) {
        return this.isHovered(d, d2, this.popupX, this.popupY, this.getPopupWidth(), this.getPopupHeight());
    }

    private float getPopupWidth() {
        return this.alphaEnabled ? 9.0F + 14.0F + 85.0F : 9.0F + 7.0F + 85.0F;
    }

    private float getPopupHeight() {
        return 9.0F + this.getExtraHeaderHeight() + 85.0F + 6.0F + 11.0F + 5.0F + 12.0F + 5.0F + 9.0F;
    }

    private float getPickerY() {
        return this.popupY + 4.5F + this.getExtraHeaderHeight();
    }

    private float getContentWidth() {
        return this.alphaEnabled ? 99.0F : 92.0F;
    }

    private ValueBind<Integer> getActiveBind() {
        return this.editingBind != null ? this.editingBind : this.openedBind;
    }

    private boolean isPopupVisible() {
        return this.pickerOpen || this.popupAnimation.get() > 0.001F;
    }

    private float updatePopupAnimation() {
        this.popupAnimation.update();
        if (this.pickerOpen) {
            this.popupAnimation.animate(1.0, 0.3F, Easings.BACK_OUT);
            return this.popupAnimation.get();
        } else {
            this.popupAnimation.animate(0.0, 0.2F, Easings.QUAD_IN);
            return this.popupAnimation.get();
        }
    }

    private static float clamp01(float f) {
        return Math.max(0.0F, Math.min(1.0F, f));
    }

    private static int clamp(int i, int i2, int i3) {
        return Math.max(i2, Math.min(i3, i));
    }

    private static int parseIntOrZero(String string) {
        if (string != null && !string.isEmpty()) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException numberFormatException) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private static boolean isHexChar(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    private static int contrastTextColor(int i) {
        float[] floats = ColorUtil.toHsb(i);
        return floats[2] > 0.65F ? ColorUtil.rgb(25, 25, 25) : ColorUtil.WHITE;
    }

    private static Integer parseHex(String string) {
        if (string == null) {
            return null;
        }

        String string2 = string.trim();
        if (string2.startsWith("#")) {
            string2 = string2.substring(1);
        }

        String string3 = string2.toUpperCase(Locale.ROOT);
        if (string3.length() != 6 && string3.length() != 8) {
            return null;
        }

        try {
            return ColorUtil.fromHex(string3);
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    @Override
    protected float getControlWidth() {
        return 10.0F;
    }

    enum InputFormat {
        HEX("HEX", 1),
        RGB("RGB", 3),
        HSB("HSB", 3);

        final String label;
        private final int fieldCount;

        InputFormat(String string, int i) {
            this.label = string;
            this.fieldCount = i;
        }
    }
}
