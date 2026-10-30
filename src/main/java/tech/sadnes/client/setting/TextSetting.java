package tech.sadnes.client.setting;

import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.Side;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class TextSetting extends ValueSetting<String> {
    private static final float MIN_ROW_HEIGHT = 20.0F;
    private static final float FIELD_HEIGHT = 10.0F;
    private static final float POPUP_RADIUS = 10.0F;
    private static final float POPUP_PADDING = 4.0F;
    private static final float TEXT_PADDING_X = 5.0F;
    private static final float TEXT_PADDING_Y = 4.0F;
    private static final float EDITOR_WIDTH = 110.0F;
    private static final float MIN_POPUP_HEIGHT = 22.0F;
    private final Animation popupAnimation = new Animation();
    private final String placeholder;
    private int minLength;
    private int maxLength = Integer.MAX_VALUE;
    private TextFieldComponent textField;
    private boolean editing;
    private float rowHeight = 20.0F;
    private float popupX;
    private float popupY;
    private float popupWidth;
    private float popupHeight;

    public TextSetting(String string) {
        this(string, "", "");
    }

    public TextSetting(String string, String string2) {
        this(string, string2, "");
    }

    public TextSetting(String string, String string2, String string3) {
        super(string, string2 != null ? string2 : "");
        this.placeholder = string3 != null ? string3 : "";
        this.setValue(string2 != null ? string2 : "");
    }

    public TextSetting(GroupSetting groupSetting, String string) {
        this(groupSetting, string, "", "");
    }

    public TextSetting(GroupSetting groupSetting, String string, String string2) {
        this(groupSetting, string, string2, "");
    }

    public TextSetting(GroupSetting groupSetting, String string, String string2, String string3) {
        this(string, string2, string3);
        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public TextSetting setValue(String string) {
        super.setValue(string);
        if (this.textField != null && !this.textField.isFocused()) {
            this.syncFieldFromValue();
        }

        return this;
    }

    public TextSetting withMinLength(int i) {
        this.minLength = Math.max(0, i);
        return this;
    }

    public TextSetting withMaxLength(int i) {
        this.maxLength = Math.max(1, i);
        this.setValue(this.getValue());
        return this;
    }

    public TextSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public TextSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public TextSetting setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    protected String sanitizeValue(String string) {
        if (string == null) {
            return "";
        } else {
            return string.length() > this.maxLength ? string.substring(0, this.maxLength) : string;
        }
    }

    @Override
    public String formatBindValue(ValueBind<String> valueBind) {
        return valueBind != null && valueBind.getValue() != null ? (String)valueBind.getValue() : this.getValue();
    }

    public String getText() {
        return this.getValue();
    }

    public int getInt(int i) {
        try {
            String string = this.getValue().trim();
            return string.isEmpty() ? i : Integer.parseInt(string);
        } catch (NumberFormatException numberFormatException) {
            return i;
        }
    }

    @Override
    public float resolveHeight(float f) {
        this.rowHeight = Math.max(20.0F, f);
        return f;
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.position.x;
        float f3 = this.position.y;
        float f4 = this.size.x;
        float f5 = this.size.y;
        float f6 = Math.min(this.rowHeight, f5);
        this.drawLabel(drawContext, f2, f3, f4, f6, f4 / 2.0F - 6.0F, this.isHovered(i, i2, this.position, this.size));
        float f7 = f4 / 2.0F;
        float f8 = f2 + f4 - f7;
        float f9 = f3 + f6 / 2.0F - 5.0F;
        int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), this.getAlpha());
        String string = this.getPreviewText();
        GuiRenderUtil.drawSurface(drawContext, f8, f9, f7, 10.0F, this.getAlpha(), 3.0F)
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, string, 4.0F, 0.0F, f7 - 8.0F, i3, 5.5F, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        boolean flag2 = this.isLeftButton(click.button());
        boolean flag3 = this.isRightButton(click.button());
        if (!flag2 && !flag3) {
            return false;
        }

        boolean flag4 = this.isHovered(click.x(), click.y(), this.position.x, this.position.y, this.size.x, this.size.y);
        if (this.editing) {
            if (flag2 && this.isEditorHovered(click.x(), click.y())) {
                TextFieldComponent textFieldComponent = this.getTextField();
                textFieldComponent.mouseClicked(click.x(), click.y(), click.button());
                if (!textFieldComponent.isFocused()) {
                    textFieldComponent.setFocused(true);
                }

                textFieldComponent.setReplaceOnInput(false);
                return true;
            }

            if ((flag2 || flag3) && flag4) {
                this.closeEditor();
                return true;
            }

            this.closeEditor();
        }

        if (flag2 && flag4) {
            this.openEditor();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void renderOverlay(DrawContext drawContext, int i, int i2, float f) {
        this.popupAnimation.update();
        if (this.editing) {
            this.popupAnimation.animate(1.0, 0.3F, Easings.BACK_OUT);
        } else {
            this.popupAnimation.animate(0.0, 0.2F, Easings.QUAD_IN);
        }

        float f2 = this.popupAnimation.get();
        if (!(f2 <= 0.001F)) {
            TextFieldComponent textFieldComponent = this.getTextField();
            textFieldComponent.setWidth(110.0F);
            float f3 = Math.max(5.5F, textFieldComponent.getHeight());
            float f4 = f3 + 8.0F + 8.0F;
            this.popupHeight = Math.max(22.0F, f4);
            float f5 = this.size.x / 2.0F;
            float f6 = this.position.x + this.size.x - f5 / 2.0F;
            float f7 = this.position.y + Math.min(this.rowHeight, this.size.y) / 2.0F;
            this.popupX = GuiRenderUtil.clampToScreenX(f6 - this.popupWidth / 2.0F, this.popupWidth);
            this.popupY = GuiRenderUtil.clampToScreenY(f7 - this.popupHeight / 2.0F, this.popupHeight);
            float f8 = this.getAlpha() * Math.min(f2, 1.0F);
            float f9 = 0.85F + 0.15F * f2;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, this.popupX + this.popupWidth / 2.0F, this.popupY + this.popupHeight / 2.0F, f9);
            int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f8);
            GuiRenderUtil.drawPopup(drawContext, this.popupX, this.popupY, this.popupWidth, this.popupHeight, f8, 10.0F)
                .scissor(() -> this.drawEditor(drawContext, textFieldComponent, f8, i3));
            MatrixUtil.pop(drawContext);
        }
    }

    private void drawEditor(DrawContext drawContext, TextFieldComponent textFieldComponent, float f, int i) {
        float f2 = this.popupX + 4.0F;
        float f3 = this.popupY + 4.0F;
        float f4 = this.popupWidth - 8.0F;
        float f5 = this.popupHeight - 8.0F;
        Radius radius = Radius.all(6.6666665F);
        RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, f4, f5);
        renderUtil.rect(ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.05F), 0.3F * f, radius);
        renderUtil.expand(Side.ALL, -0.5F);
        renderUtil.shadow(0.5F, ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.5F), 0.05F * f, radius);
        renderUtil.expand(Side.ALL, 0.5F);
        Vector2f vector2f = new Vector2f(f2 + 5.0F + 2.0F, f3 + 4.0F);
        textFieldComponent.render(drawContext, vector2f, i, 110.0F);
    }

    @Override
    public boolean isOverlayOpen() {
        return this.editing || this.popupAnimation.get() > 0.001F;
    }

    @Override
    public boolean isTyping() {
        return this.editing && this.textField != null && this.textField.isFocused();
    }

    public boolean isOverEditor(double d, double d2) {
        return (this.editing || this.popupAnimation.get() > 0.001F) && this.isEditorHovered(d, d2);
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (this.editing && this.textField != null && this.textField.isFocused()) {
            int i = keyInput.key();
            if (i == 256) {
                this.closeEditor();
                return true;
            } else {
                this.textField.keyPressed(i);
                this.commitText();
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        if (this.editing && this.textField != null && this.textField.isFocused()) {
            this.textField.charTyped((char)charInput.codepoint());
            this.commitText();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void resize(int i, int i2) {
        this.editing = false;
        this.popupAnimation.forceValue(0.0);
        if (this.textField != null) {
            this.textField.setFocused(false);
            this.syncFieldFromValue();
            this.textField.setCursor(this.textField.getText().length());
        }
    }

    @Override
    public void removed() {
        this.editing = false;
        this.popupAnimation.forceValue(0.0);
        if (this.textField != null) {
            this.textField.setFocused(false);
        }
    }

    @Override
    public void close() {
        this.closeEditor();
    }

    private TextFieldComponent getTextField() {
        if (this.textField == null) {
            this.textField = new TextFieldComponent(Fonts.INTER_MEDIUM, 5.5F, this.placeholder, false, false);
            this.textField.setMultiline(true);
            String string = this.getValue();
            this.textField.setText(string != null ? string : "");
            this.textField.setCursor(this.textField.getText().length());
        }

        return this.textField;
    }

    private void openEditor() {
        this.popupWidth = 128.0F;
        this.editing = true;
        TextFieldComponent textFieldComponent = this.getTextField();
        textFieldComponent.setFocused(true);
        textFieldComponent.setReplaceOnInput(false);
        textFieldComponent.setCursor(textFieldComponent.getText().length());
    }

    private void closeEditor() {
        this.commitText();
        this.editing = false;
        if (this.textField != null) {
            this.textField.setFocused(false);
            this.syncFieldFromValue();
        }
    }

    private void commitText() {
        if (this.textField != null) {
            String string = this.sanitizeValue(this.textField.getText());
            if (!string.equals(this.textField.getText())) {
                this.textField.setText(string);
                if (this.textField.getCursor() > string.length()) {
                    this.textField.setCursor(string.length());
                }
            }

            if (string.length() >= this.minLength) {
                this.setValue(string);
            }
        }
    }

    private void syncFieldFromValue() {
        String string = this.getValue();
        this.textField.setText(string != null ? string : "");
        if (this.textField.getCursor() > this.textField.getText().length()) {
            this.textField.setCursor(this.textField.getText().length());
        }
    }

    private boolean isEditorHovered(double d, double d2) {
        return !(this.popupWidth <= 0.0F) && !(this.popupHeight <= 0.0F)
            ? this.isHovered(d, d2, this.popupX, this.popupY, this.popupWidth, this.popupHeight)
            : false;
    }

    private String getPreviewText() {
        String string = this.getValue();
        if (string != null && !string.isEmpty()) {
            int i = string.indexOf(10);
            if (i >= 0) {
                string = string.substring(0, i) + "…";
            }

            return string;
        } else {
            return Language.translate(this.placeholder);
        }
    }

    @Override
    protected float getControlWidth() {
        return Math.max(40.0F, Fonts.INTER_MEDIUM.getWidth(this.getPreviewText(), 5.5F) + 8.0F);
    }
}
