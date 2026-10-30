package tech.sadnes.client.ui.clickgui.component;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.math.StopWatch;

public class TextFieldComponent implements IRenderable, IMinecraft {
    private static final String NUMERIC_CHARS = "1234567890.,-";
    private static final List<TextFieldComponent> INSTANCES = new ArrayList<>();
    private String text = "";
    private final Vector2f position = new Vector2f();
    private boolean focused;
    private int cursor;
    private final MsdfFont font;
    private final float fontSize;
    private int color;
    private final String placeholder;
    private float width;
    private final boolean password;
    private final boolean numeric;
    private boolean replaceOnInput;
    private boolean multiline;
    private float lineSpacing = 1.5F;
    private float scrollOffset;
    private final List<Object[]> undoStack = new ArrayList<>();
    private final List<Object[]> redoStack = new ArrayList<>();
    private boolean applyingEdit;
    private int selectionAnchor = -1;
    private boolean dragging;
    private int clickCount;
    private float lastClickX;
    private float lastClickY;
    private final StopWatch clickTimer = new StopWatch();
    private final StopWatch cursorBlinkTimer = new StopWatch();

    public static boolean isAnyFocused() {
        return INSTANCES.stream().anyMatch(textFieldComponent -> textFieldComponent.focused);
    }

    public static void unfocusAll() {
        int i = INSTANCES.size();

        for (int i2 = 0; i2 < i; i2++) {
            INSTANCES.get(i2).unfocus();
        }
    }

    public TextFieldComponent setFocused(boolean flag) {
        if (this.focused == flag) {
            return this;
        }

        if (!flag) {
            this.unfocus();
            return this;
        }

        this.focused = true;
        int i = INSTANCES.size();

        for (int i2 = 0; i2 < i; i2++) {
            TextFieldComponent textFieldComponent = INSTANCES.get(i2);
            if (textFieldComponent != this) {
                textFieldComponent.unfocus();
            }
        }

        return this;
    }

    private void unfocus() {
        this.focused = false;
        this.selectionAnchor = -1;
        this.dragging = false;
    }

    public TextFieldComponent(MsdfFont msdfFont, float f, String string, boolean flag, boolean flag2) {
        this.font = msdfFont;
        this.fontSize = f;
        this.placeholder = string;
        this.password = flag;
        this.numeric = flag2;
        INSTANCES.add(this);
    }

    public void render(DrawContext drawContext, Vector2f vector2f, int i, float f) {
        this.position.set(vector2f);
        this.color = i;
        this.width = f;
        this.updateDragSelection(drawContext);
        this.clampCursor();
        String string = this.getDisplayText();
        int i2 = this.text.isEmpty() ? ColorUtil.darken(i, this.focused ? 0.8F : 0.6F) : ColorUtil.darken(i, this.focused ? 1.0F : 0.8F);
        if (this.multiline) {
            this.renderMultiLine(drawContext, string, i2);
        } else {
            this.renderSingleLine(drawContext, string, i2);
        }
    }

    private void renderSingleLine(DrawContext drawContext, String string, int i) {
        if (this.text.isEmpty()) {
            this.scrollOffset = 0.0F;
            new RenderUtil(drawContext, this.position.x, this.position.y, this.width, this.fontSize)
                .textTopLeft(this.getRenderFont(), Language.translate(this.placeholder), 0.0F, 0.0F, i, this.fontSize);
            if (this.focused) {
                this.renderCursor(drawContext, this.position.x, this.position.y);
            }
        } else {
            float f = this.getTextWidth(string.substring(0, this.cursor));
            float f2 = this.getTextWidth(string);
            if (f - this.scrollOffset > this.width) {
                this.scrollOffset = f - this.width;
            }

            if (f - this.scrollOffset < 0.0F) {
                this.scrollOffset = f;
            }

            float f3 = Math.max(0.0F, f2 - this.width);
            if (this.scrollOffset > f3) {
                this.scrollOffset = f3;
            }

            if (this.scrollOffset < 0.0F) {
                this.scrollOffset = 0.0F;
            }

            boolean flag = ScissorStack.push(drawContext, this.position.x, this.position.y, this.width, this.fontSize);
            if (this.hasSelection()) {
                float f4 = this.getTextWidth(string.substring(0, this.getSelectionStart()));
                float f5 = this.getTextWidth(string.substring(0, this.getSelectionEnd())) - f4;
                new RenderUtil(drawContext, this.position.x + f4 - this.scrollOffset, this.position.y, f5, this.fontSize)
                    .rect(ColorUtil.multiplyAlpha(this.color, 0.3F), 1.0F, Radius.zero());
            }

            new RenderUtil(drawContext, this.position.x - this.scrollOffset, this.position.y, f2, this.fontSize)
                .textTopLeft(this.getRenderFont(), string, 0.0F, 0.0F, i, this.fontSize);
            if (this.focused) {
                this.renderCursor(drawContext, this.position.x + f - this.scrollOffset, this.position.y);
            }

            if (flag) {
                ScissorStack.pop(drawContext);
            }
        }
    }

    public void mouseClicked(double d, double d2, int i) {
        this.setFocused(
            this.isLeftButton(i)
                && this.isHovered(d, d2, this.position.x, this.position.y, this.width, this.multiline ? this.getHeight() : this.fontSize)
        );
        if (this.focused) {
            boolean flag = !this.clickTimer.hasPassed(350.0) && Math.abs(d - this.lastClickX) <= 2.0 && Math.abs(d2 - this.lastClickY) <= 2.0;
            this.clickCount = flag ? this.clickCount % 3 + 1 : 1;
            this.clickTimer.reset();
            this.lastClickX = (float)d;
            this.lastClickY = (float)d2;
            int i2 = this.getIndexAt((float)d, (float)d2);
            switch (this.clickCount) {
                case 2:
                    this.selectWord(i2);
                    this.replaceOnInput = false;
                    this.cursorBlinkTimer.reset();
                    return;
                case 3:
                    this.selectLine(i2);
                    this.replaceOnInput = false;
                    this.cursorBlinkTimer.reset();
                    return;
                default:
                    if (this.isShiftDown()) {
                        if (this.selectionAnchor < 0) {
                            this.selectionAnchor = this.cursor;
                        }
                    } else {
                        this.selectionAnchor = i2;
                    }

                    this.cursor = i2;
                    this.dragging = true;
                    this.replaceOnInput = false;
                    this.cursorBlinkTimer.reset();
            }
        }
    }

    public void keyPressed(int i) {
        if (this.focused) {
            this.clampCursor();
            boolean flag = this.isCtrlDown();
            boolean flag2 = this.isShiftDown();
            switch (i) {
                case 65:
                    if (flag) {
                        this.selectAll();
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }
                    break;
                case 67:
                    if (flag) {
                        this.copySelection();
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }
                    break;
                case 86:
                    if (!flag) {
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    String string = mc.keyboard.getClipboard();
                    if (string != null) {
                        if (string.isEmpty()) {
                            this.clampCursor();
                            this.cursorBlinkTimer.reset();
                            return;
                        }

                        this.insert(string);
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }
                    break;
                case 88:
                    if (!flag) {
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    this.copySelection();
                    if (this.deleteSelection()) {
                        this.replaceOnInput = false;
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }
                    break;
                case 89:
                    if (flag) {
                        this.redo();
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }
                    break;
                case 90:
                    if (!flag) {
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    if (flag2) {
                        this.redo();
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    this.undo();
                    this.clampCursor();
                    this.cursorBlinkTimer.reset();
                    return;
                case 257:
                case 335:
                    if (this.multiline) {
                        this.insert("\n");
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    this.setFocused(false);
                    break;
                case 259:
                    this.replaceOnInput = false;
                    if (this.deleteSelection()) {
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    if (this.cursor <= 0) {
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    this.pushUndo();
                    int i2 = flag ? this.prevWordBoundary(this.cursor) : this.text.offsetByCodePoints(this.cursor, -1);
                    this.text = new StringBuilder(this.text).delete(i2, this.cursor).toString();
                    this.cursor = i2;
                    this.clampCursor();
                    this.cursorBlinkTimer.reset();
                    return;
                case 261:
                    this.replaceOnInput = false;
                    if (this.deleteSelection()) {
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    if (this.cursor >= this.text.length()) {
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    this.pushUndo();
                    int i3 = flag ? this.nextWordBoundary(this.cursor) : this.text.offsetByCodePoints(this.cursor, 1);
                    this.text = new StringBuilder(this.text).delete(this.cursor, i3).toString();
                    this.clampCursor();
                    this.cursorBlinkTimer.reset();
                    return;
                case 262:
                    this.replaceOnInput = false;
                    if (!flag2 && this.hasSelection()) {
                        int i4 = this.getSelectionEnd();
                        this.selectionAnchor = -1;
                        if (flag) {
                            this.cursor = this.nextWordBoundary(i4);
                            this.clampCursor();
                            this.cursorBlinkTimer.reset();
                            return;
                        }

                        this.cursor = i4;
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    int i5 = flag
                        ? this.nextWordBoundary(this.cursor)
                        : (this.cursor < this.text.length() ? this.text.offsetByCodePoints(this.cursor, 1) : this.cursor);
                    this.moveCursor(i5, flag2);
                    this.clampCursor();
                    this.cursorBlinkTimer.reset();
                    return;
                case 263:
                    this.replaceOnInput = false;
                    if (!flag2 && this.hasSelection()) {
                        int i6 = this.getSelectionStart();
                        this.selectionAnchor = -1;
                        if (flag) {
                            this.cursor = this.prevWordBoundary(i6);
                            this.clampCursor();
                            this.cursorBlinkTimer.reset();
                            return;
                        }

                        this.cursor = i6;
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    int i7 = flag ? this.prevWordBoundary(this.cursor) : (this.cursor > 0 ? this.text.offsetByCodePoints(this.cursor, -1) : this.cursor);
                    this.moveCursor(i7, flag2);
                    this.clampCursor();
                    this.cursorBlinkTimer.reset();
                    return;
                case 264:
                    if (!this.multiline) {
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    this.replaceOnInput = false;
                    this.moveCursor(this.getVerticalCursorTarget(1), flag2);
                    this.clampCursor();
                    this.cursorBlinkTimer.reset();
                    return;
                case 265:
                    if (!this.multiline) {
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    this.replaceOnInput = false;
                    this.moveCursor(this.getVerticalCursorTarget(-1), flag2);
                    this.clampCursor();
                    this.cursorBlinkTimer.reset();
                    return;
                case 268:
                    this.replaceOnInput = false;
                    if (!flag && this.multiline) {
                        this.moveCursor(this.getLineBounds(this.cursor)[0], flag2);
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    this.moveCursor(0, flag2);
                    this.clampCursor();
                    this.cursorBlinkTimer.reset();
                    return;
                case 269:
                    this.replaceOnInput = false;
                    if (!flag && this.multiline) {
                        this.moveCursor(this.getLineBounds(this.cursor)[1], flag2);
                        this.clampCursor();
                        this.cursorBlinkTimer.reset();
                        return;
                    }

                    this.moveCursor(this.text.length(), flag2);
                    this.clampCursor();
                    this.cursorBlinkTimer.reset();
                    return;
            }

            this.clampCursor();
            this.cursorBlinkTimer.reset();
        }
    }

    public void charTyped(char c) {
        this.charTyped((int)c);
    }

    public void charTyped(int i) {
        if (this.focused) {
            this.clampCursor();
            if (i != 10 && i != 13) {
                if (Character.isValidCodePoint(i)) {
                    String string = new String(Character.toChars(i));
                    if (!this.numeric || "1234567890.,-".contains(string)) {
                        if (this.numeric || this.isAllowedChar(i)) {
                            this.insert(string);
                            this.cursorBlinkTimer.reset();
                        }
                    }
                }
            } else {
                if (this.multiline) {
                    this.insert("\n");
                }
            }
        }
    }

    public boolean hasSelection() {
        return this.selectionAnchor >= 0 && this.selectionAnchor != this.cursor;
    }

    public int getSelectionStart() {
        return this.hasSelection() ? Math.min(this.cursor, this.selectionAnchor) : this.cursor;
    }

    public int getSelectionEnd() {
        return this.hasSelection() ? Math.max(this.cursor, this.selectionAnchor) : this.cursor;
    }

    public String getSelectedText() {
        return this.hasSelection() ? this.text.substring(this.getSelectionStart(), this.getSelectionEnd()) : "";
    }

    public void selectAll() {
        this.selectionAnchor = 0;
        this.cursor = this.text.length();
        this.replaceOnInput = false;
    }

    public void clearSelection() {
        this.selectionAnchor = -1;
    }

    private void copySelection() {
        if (this.hasSelection() && !this.password) {
            mc.keyboard.setClipboard(this.text.substring(this.getSelectionStart(), this.getSelectionEnd()));
        }
    }

    private boolean deleteSelection() {
        if (!this.hasSelection()) {
            return false;
        }

        this.pushUndo();
        int i = this.getSelectionStart();
        this.text = new StringBuilder(this.text).delete(i, this.getSelectionEnd()).toString();
        this.cursor = i;
        this.selectionAnchor = -1;
        return true;
    }

    private void pushUndo() {
        if (!this.applyingEdit) {
            if (this.undoStack.size() >= 100) {
                this.undoStack.remove(0);
            }

            this.undoStack.add(new Object[]{this.text, this.cursor});
            this.redoStack.clear();
        }
    }

    private void undo() {
        if (!this.undoStack.isEmpty()) {
            this.redoStack.add(new Object[]{this.text, this.cursor});
            this.restoreState(this.undoStack.remove(this.undoStack.size() - 1));
        }
    }

    private void redo() {
        if (!this.redoStack.isEmpty()) {
            this.undoStack.add(new Object[]{this.text, this.cursor});
            this.restoreState(this.redoStack.remove(this.redoStack.size() - 1));
        }
    }

    private void restoreState(Object[] objects) {
        this.text = (String)objects[0];
        this.cursor = (Integer)objects[1];
        this.selectionAnchor = -1;
        this.replaceOnInput = false;
        this.clampCursor();
        this.cursorBlinkTimer.reset();
    }

    private void moveCursor(int i, boolean flag) {
        if (flag) {
            if (this.selectionAnchor < 0) {
                this.selectionAnchor = this.cursor;
            }
        } else {
            this.selectionAnchor = -1;
        }

        this.cursor = Math.max(0, Math.min(i, this.text.length()));
    }

    private void selectWord(int i) {
        int i2 = this.text.length();
        if (i2 != 0) {
            int i3 = i >= i2 ? this.text.offsetByCodePoints(i2, -1) : i;
            int i4 = this.getCharClass(this.text.codePointAt(i3));
            int i5 = i3;
            int i6 = this.text.offsetByCodePoints(i3, 1);

            while (i5 > 0 && this.getCharClass(this.text.codePointBefore(i5)) == i4) {
                i5 = this.text.offsetByCodePoints(i5, -1);
            }

            while (i6 < i2 && this.getCharClass(this.text.codePointAt(i6)) == i4) {
                i6 = this.text.offsetByCodePoints(i6, 1);
            }

            this.selectionAnchor = i5;
            this.cursor = i6;
        }
    }

    private void selectLine(int i) {
        this.selectionAnchor = this.text.lastIndexOf(10, Math.max(0, i - 1)) + 1;
        int i2 = this.text.indexOf(10, i);
        if (i2 < 0) {
            this.cursor = this.text.length();
        } else {
            this.cursor = i2;
        }
    }

    private void clampCursor() {
        int i = this.text.length();
        if (this.cursor < 0) {
            this.cursor = 0;
        }

        if (this.cursor > i) {
            this.cursor = i;
        }

        if (this.selectionAnchor > i) {
            this.selectionAnchor = i;
        }

        if (!this.dragging && this.selectionAnchor == this.cursor) {
            this.selectionAnchor = -1;
        }
    }

    private int getCharClass(int i) {
        if (Character.isWhitespace(i)) {
            return 0;
        } else {
            return !Character.isLetterOrDigit(i) && i != 95 ? 2 : 1;
        }
    }

    private int nextWordBoundary(int i) {
        int i2 = this.text.length();
        if (i >= i2) {
            return i2;
        }

        int i3 = i;
        int i4 = this.getCharClass(this.text.codePointAt(i));

        while (i3 < i2 && this.getCharClass(this.text.codePointAt(i3)) == i4) {
            i3 = this.text.offsetByCodePoints(i3, 1);
        }

        while (i3 < i2 && Character.isWhitespace(this.text.codePointAt(i3))) {
            i3 = this.text.offsetByCodePoints(i3, 1);
        }

        return i3;
    }

    private int prevWordBoundary(int i) {
        int i2 = i;

        while (i2 > 0 && Character.isWhitespace(this.text.codePointBefore(i2))) {
            i2 = this.text.offsetByCodePoints(i2, -1);
        }

        if (i2 <= 0) {
            return 0;
        }

        int i3 = this.getCharClass(this.text.codePointBefore(i2));

        while (i2 > 0 && this.getCharClass(this.text.codePointBefore(i2)) == i3) {
            i2 = this.text.offsetByCodePoints(i2, -1);
        }

        return i2;
    }

    private void updateDragSelection(DrawContext drawContext) {
        if (this.dragging) {
            if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1) {
                this.dragging = false;
            } else {
                Vector2f vector2f = this.getMousePosition(drawContext);
                if (vector2f != null) {
                    int i = this.getIndexAt(vector2f.x, vector2f.y);
                    if (i != this.cursor) {
                        this.cursor = i;
                        this.cursorBlinkTimer.reset();
                    }
                }
            }
        }
    }

    private Vector2f getMousePosition(DrawContext drawContext) {
        Window window = mc.getWindow();
        if (window.getWidth() != 0 && window.getHeight() != 0) {
            double d = window.getScaleFactor();
            double d2 = d <= 0.0 ? window.getScaledWidth() : window.getFramebufferWidth() / d;
            double d3 = d <= 0.0 ? window.getScaledHeight() : window.getFramebufferHeight() / d;
            float f = (float)(mc.mouse.getX() * d2 / window.getWidth());
            float f2 = (float)(mc.mouse.getY() * d3 / window.getHeight());
            return new Matrix3x2f(drawContext.getMatrices()).invert().transformPosition(f, f2, new Vector2f());
        } else {
            return null;
        }
    }

    private int getIndexAt(float f, float f2) {
        String string = this.getDisplayText();
        if (string.isEmpty()) {
            return 0;
        }

        if (!this.multiline) {
            return this.getIndexAtX(string, f - this.position.x + this.scrollOffset);
        }

        List list = this.wrapLines(string, this.width);
        int i = (int)((f2 - this.position.y) / (this.fontSize + this.lineSpacing));
        if (i < 0) {
            i = 0;
        }

        int i2 = list.size() - 1;
        if (i > i2) {
            i = i2;
        }

        int[] ints = (int[])list.get(i);
        return ints[0] + this.getIndexAtX(string.substring(ints[0], ints[1]), f - this.position.x);
    }

    private int getIndexAtX(String string, float f) {
        if (f <= 0.0F) {
            return 0;
        }

        float f2 = 0.0F;
        int i = 0;
        int i2 = string.length();

        while (i < i2) {
            int i3 = string.offsetByCodePoints(i, 1);
            float f3 = this.getTextWidth(string.substring(i, i3));
            if (f < f2 + f3 / 2.0F) {
                return i;
            }

            f2 += f3;
            i = i3;
        }

        return i2;
    }

    private void insert(String string) {
        this.pushUndo();
        this.applyingEdit = true;

        try {
            if (this.replaceOnInput) {
                this.text = "";
                this.cursor = 0;
                this.selectionAnchor = -1;
                this.replaceOnInput = false;
            } else {
                this.deleteSelection();
            }

            String string2 = this.sanitize(string);
            if (this.numeric) {
                string2 = this.filterNumeric(string2);
            }

            if (!string2.isEmpty()) {
                this.text = new StringBuilder(this.text).insert(this.cursor, string2).toString();
                this.cursor = this.cursor + string2.length();
                return;
            }
        } finally {
            this.applyingEdit = false;
        }
    }

    private String sanitize(String string) {
        StringBuilder builder = new StringBuilder(string.length());
        int i = string.length();

        for (int i2 = 0; i2 < i; i2++) {
            char c = string.charAt(i2);
            switch (c) {
                case '\t':
                    builder.append(' ');
                    break;
                case '\n':
                    builder.append((char)(this.multiline ? '\n' : ' '));
                case '\r':
                    break;
                default:
                    if (!Character.isISOControl(c)) {
                        builder.append(c);
                    }
            }
        }

        return builder.toString();
    }

    private MsdfFont getRenderFont() {
        return Fonts.INTER_MEDIUM != null ? Fonts.INTER_MEDIUM : this.font;
    }

    private float getTextWidth(String string) {
        if (string != null && !string.isEmpty()) {
            MsdfFont msdfFont = this.getRenderFont();
            return msdfFont != null ? msdfFont.getWidth(string, this.fontSize) : mc.textRenderer.getWidth(string);
        } else {
            return 0.0F;
        }
    }

    private String getDisplayText() {
        if (this.password && !this.text.isEmpty()) {
            int i = this.text.length();
            StringBuilder builder = new StringBuilder(i);

            for (int i2 = 0; i2 < i; i2++) {
                builder.append((char)(this.text.charAt(i2) == '\n' ? '\n' : '*'));
            }

            return builder.toString();
        } else {
            return this.text;
        }
    }

    private String filterNumeric(String string) {
        StringBuilder builder = new StringBuilder();
        boolean flag = this.text.indexOf(46) >= 0 || this.text.indexOf(44) >= 0;
        boolean flag2 = this.text.indexOf(45) >= 0;
        int i = string.length();

        for (int i2 = 0; i2 < i; i2++) {
            char c = string.charAt(i2);
            if (c >= '0' && c <= '9') {
                builder.append(c);
            } else {
                switch (c) {
                    case ',':
                    case '.':
                        if (!flag) {
                            builder.append('.');
                            flag = true;
                            break;
                        } else if (c != '-') {
                            break;
                        }
                    case '-':
                        if (!flag2 && this.cursor == 0 && builder.isEmpty()) {
                            builder.append(c);
                            flag2 = true;
                        }
                }
            }
        }

        return builder.toString();
    }

    private boolean isAllowedChar(int i) {
        if (!Character.isISOControl(i)) {
            switch (i) {
                case 9:
                case 10:
                case 13:
                    break;
                default:
                    return true;
            }
        }

        return false;
    }

    private boolean isCtrlDown() {
        return InputUtil.isKeyPressed(mc.getWindow(), 341) || InputUtil.isKeyPressed(mc.getWindow(), 345);
    }

    private boolean isShiftDown() {
        return InputUtil.isKeyPressed(mc.getWindow(), 340) || InputUtil.isKeyPressed(mc.getWindow(), 344);
    }

    public boolean isEmpty() {
        return this.text.isEmpty();
    }

    public float getHeight() {
        return !this.multiline ? this.fontSize : Math.max(1, this.wrapLines(this.getDisplayText(), this.width).size()) * (this.fontSize + this.lineSpacing);
    }

    private void renderMultiLine(DrawContext drawContext, String string, int i) {
        MsdfFont msdfFont = this.getRenderFont();
        float f = this.fontSize + this.lineSpacing;
        if (this.text.isEmpty()) {
            new RenderUtil(drawContext, this.position.x, this.position.y, this.width, this.fontSize)
                .textTopLeft(msdfFont, Language.translate(this.placeholder), 0.0F, 0.0F, i, this.fontSize);
            if (this.focused) {
                this.renderCursor(drawContext, this.position.x, this.position.y);
            }
        } else {
            List list = this.wrapLines(string, this.width);
            int i2 = this.hasSelection() ? this.getSelectionStart() : -1;
            int i3 = this.hasSelection() ? this.getSelectionEnd() : -1;
            int i4 = list.size();

            for (int i5 = 0; i5 < i4; i5++) {
                int[] ints = (int[])list.get(i5);
                String string2 = string.substring(ints[0], ints[1]);
                float f2 = this.position.y + i5 * f;
                if (i2 >= 0 && i2 <= ints[1] && i3 >= ints[0]) {
                    float f3 = this.getTextWidth(string2.substring(0, Math.max(i2, ints[0]) - ints[0]));
                    float f4 = this.getTextWidth(string2.substring(0, Math.min(i3, ints[1]) - ints[0]));
                    if (i3 > ints[1]) {
                        f4 += 2.0F;
                    }

                    if (f4 > f3) {
                        new RenderUtil(drawContext, this.position.x + f3, f2, f4 - f3, this.fontSize)
                            .rect(ColorUtil.multiplyAlpha(this.color, 0.3F), 1.0F, Radius.zero());
                    }
                }

                new RenderUtil(drawContext, this.position.x, f2, this.width, this.fontSize).textTopLeft(msdfFont, string2, 0.0F, 0.0F, i, this.fontSize);
            }

            if (this.focused) {
                int i6 = this.getLineIndex(this.cursor, string, list);
                int[] ints2 = (int[])list.get(i6);
                int i7 = Math.max(0, Math.min(this.cursor - ints2[0], ints2[1] - ints2[0]));
                this.renderCursor(drawContext, this.position.x + this.getTextWidth(string.substring(ints2[0], ints2[0] + i7)), this.position.y + i6 * f);
            }
        }
    }

    private void renderCursor(DrawContext drawContext, float f, float f2) {
        float f3 = this.cursorBlinkTimer.hasPassed(500.0) ? (float)((Math.sin(System.currentTimeMillis() / 250.0) + 1.0) * 0.5) : 1.0F;
        new RenderUtil(drawContext, f, f2, 0.5F, this.fontSize).rect(ColorUtil.multiplyAlpha(this.color, f3), 1.0F, Radius.zero());
    }

    private List<int[]> wrapLines(String string, float f) {
        ArrayList arrayList = new ArrayList();
        int i = string.length();
        if (i == 0) {
            arrayList.add(new int[]{0, 0});
            return arrayList;
        }

        MsdfFont msdfFont = this.getRenderFont();
        int i2 = 0;
        float f2 = 0.0F;
        int i3 = 0;

        while (i3 < i) {
            char c = string.charAt(i3);
            if (c == '\n') {
                arrayList.add(new int[]{i2, i3});
                i2 = i3 + 1;
                f2 = 0.0F;
                i3++;
            } else {
                float f3 = msdfFont != null ? msdfFont.getWidth(String.valueOf(c), this.fontSize) : 0.0F;
                if (f > 0.0F && f2 + f3 > f && i3 > i2) {
                    arrayList.add(new int[]{i2, i3});
                    i2 = i3;
                    f2 = 0.0F;
                }

                f2 += f3;
                i3++;
            }
        }

        arrayList.add(new int[]{i2, i});
        return arrayList;
    }

    private int getLineIndex(int i, String string, List<int[]> intses) {
        int i2 = intses.size();

        for (int i3 = 0; i3 < i2; i3++) {
            int[] ints = (int[])intses.get(i3);
            if (i < ints[1]) {
                return i3;
            }

            boolean flag = ints[1] < string.length() && string.charAt(ints[1]) == '\n';
            if (i == ints[1] && (i3 == i2 - 1 || flag)) {
                return i3;
            }
        }

        return i2 - 1;
    }

    private int[] getLineBounds(int i) {
        String string = this.getDisplayText();
        List list = this.wrapLines(string, this.width);
        return (int[])list.get(this.getLineIndex(i, string, list));
    }

    private int getVerticalCursorTarget(int i) {
        String string = this.getDisplayText();
        List list = this.wrapLines(string, this.width);
        int i2 = this.getLineIndex(this.cursor, string, list);
        int[] ints = (int[])list.get(i2);
        int i3 = Math.max(0, Math.min(this.cursor - ints[0], ints[1] - ints[0]));
        int i4 = i2 + i;
        if (i4 >= 0 && i4 < list.size()) {
            int[] ints2 = (int[])list.get(i4);
            return ints2[0] + Math.min(i3, ints2[1] - ints2[0]);
        } else {
            return this.cursor;
        }
    }

    public String getText() {
        return this.text;
    }

    public Vector2f getPosition() {
        return this.position;
    }

    public boolean isFocused() {
        return this.focused;
    }

    public int getCursor() {
        return this.cursor;
    }

    public MsdfFont getFont() {
        return this.font;
    }

    public float getFontSize() {
        return this.fontSize;
    }

    public int getColor() {
        return this.color;
    }

    public String getPlaceholder() {
        return this.placeholder;
    }

    public float getWidth() {
        return this.width;
    }

    public boolean isPassword() {
        return this.password;
    }

    public boolean isNumeric() {
        return this.numeric;
    }

    public boolean isReplaceOnInput() {
        return this.replaceOnInput;
    }

    public boolean isMultiline() {
        return this.multiline;
    }

    public float getLineSpacing() {
        return this.lineSpacing;
    }

    public float getScrollOffset() {
        return this.scrollOffset;
    }

    public List<Object[]> getUndoStack() {
        return this.undoStack;
    }

    public List<Object[]> getRedoStack() {
        return this.redoStack;
    }

    public boolean isApplyingEdit() {
        return this.applyingEdit;
    }

    public int getSelectionAnchor() {
        return this.selectionAnchor;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public int getClickCount() {
        return this.clickCount;
    }

    public float getLastClickX() {
        return this.lastClickX;
    }

    public float getLastClickY() {
        return this.lastClickY;
    }

    public StopWatch getClickTimer() {
        return this.clickTimer;
    }

    public StopWatch getCursorBlinkTimer() {
        return this.cursorBlinkTimer;
    }

    public TextFieldComponent setText(String string) {
        this.text = string;
        return this;
    }

    public TextFieldComponent setCursor(int i) {
        this.cursor = i;
        return this;
    }

    public TextFieldComponent setColor(int i) {
        this.color = i;
        return this;
    }

    public TextFieldComponent setWidth(float f) {
        this.width = f;
        return this;
    }

    public TextFieldComponent setReplaceOnInput(boolean flag) {
        this.replaceOnInput = flag;
        return this;
    }

    public TextFieldComponent setMultiline(boolean flag) {
        this.multiline = flag;
        return this;
    }

    public TextFieldComponent setLineSpacing(float f) {
        this.lineSpacing = f;
        return this;
    }

    public TextFieldComponent setScrollOffset(float f) {
        this.scrollOffset = f;
        return this;
    }

    public TextFieldComponent setApplyingEdit(boolean flag) {
        this.applyingEdit = flag;
        return this;
    }

    public TextFieldComponent setSelectionAnchor(int i) {
        this.selectionAnchor = i;
        return this;
    }

    public TextFieldComponent setDragging(boolean flag) {
        this.dragging = flag;
        return this;
    }

    public TextFieldComponent setClickCount(int i) {
        this.clickCount = i;
        return this;
    }

    public TextFieldComponent setLastClickX(float f) {
        this.lastClickX = f;
        return this;
    }

    public TextFieldComponent setLastClickY(float f) {
        this.lastClickY = f;
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof TextFieldComponent textFieldComponent)) {
            return false;
        } else {
            if (!textFieldComponent.canEqual(this)) {
                return false;
            }

            if (this.isFocused() != textFieldComponent.isFocused()) {
                return false;
            }

            if (this.getCursor() != textFieldComponent.getCursor()) {
                return false;
            }

            if (Float.compare(this.getFontSize(), textFieldComponent.getFontSize()) != 0) {
                return false;
            }

            if (this.getColor() != textFieldComponent.getColor()) {
                return false;
            }

            if (Float.compare(this.getWidth(), textFieldComponent.getWidth()) != 0) {
                return false;
            }

            if (this.isPassword() != textFieldComponent.isPassword()) {
                return false;
            }

            if (this.isNumeric() != textFieldComponent.isNumeric()) {
                return false;
            }

            if (this.isReplaceOnInput() != textFieldComponent.isReplaceOnInput()) {
                return false;
            }

            if (this.isMultiline() != textFieldComponent.isMultiline()) {
                return false;
            }

            if (Float.compare(this.getLineSpacing(), textFieldComponent.getLineSpacing()) != 0) {
                return false;
            }

            if (Float.compare(this.getScrollOffset(), textFieldComponent.getScrollOffset()) != 0) {
                return false;
            }

            if (this.isApplyingEdit() != textFieldComponent.isApplyingEdit()) {
                return false;
            }

            if (this.getSelectionAnchor() != textFieldComponent.getSelectionAnchor()) {
                return false;
            }

            if (this.isDragging() != textFieldComponent.isDragging()) {
                return false;
            }

            if (this.getClickCount() != textFieldComponent.getClickCount()) {
                return false;
            }

            if (Float.compare(this.getLastClickX(), textFieldComponent.getLastClickX()) != 0) {
                return false;
            }

            if (Float.compare(this.getLastClickY(), textFieldComponent.getLastClickY()) != 0) {
                return false;
            }

            String string = this.getText();
            String string2 = textFieldComponent.getText();
            if (string == null) {
                if (string2 != null) {
                    return false;
                }
            } else if (!string.equals(string2)) {
                return false;
            }

            Vector2f vector2f = this.getPosition();
            Vector2f vector2f2 = textFieldComponent.getPosition();
            if (vector2f == null) {
                if (vector2f2 != null) {
                    return false;
                }
            } else if (!vector2f.equals(vector2f2)) {
                return false;
            }

            MsdfFont msdfFont = this.getFont();
            MsdfFont msdfFont2 = textFieldComponent.getFont();
            if (msdfFont == null) {
                if (msdfFont2 != null) {
                    return false;
                }
            } else if (!msdfFont.equals(msdfFont2)) {
                return false;
            }

            String string3 = this.getPlaceholder();
            String string4 = textFieldComponent.getPlaceholder();
            if (string3 == null) {
                if (string4 != null) {
                    return false;
                }
            } else if (!string3.equals(string4)) {
                return false;
            }

            List list = this.getUndoStack();
            List list2 = textFieldComponent.getUndoStack();
            if (list == null) {
                if (list2 != null) {
                    return false;
                }
            } else if (!list.equals(list2)) {
                return false;
            }

            List list3 = this.getRedoStack();
            List list4 = textFieldComponent.getRedoStack();
            if (list3 == null) {
                if (list4 != null) {
                    return false;
                }
            } else if (!list3.equals(list4)) {
                return false;
            }

            StopWatch stopWatch = this.getClickTimer();
            StopWatch stopWatch2 = textFieldComponent.getClickTimer();
            if (stopWatch == null) {
                if (stopWatch2 != null) {
                    return false;
                }
            } else if (!stopWatch.equals(stopWatch2)) {
                return false;
            }

            StopWatch stopWatch3 = this.getCursorBlinkTimer();
            StopWatch stopWatch4 = textFieldComponent.getCursorBlinkTimer();
            if (stopWatch3 == null) {
                if (stopWatch4 != null) {
                    return false;
                }
            } else if (!stopWatch3.equals(stopWatch4)) {
                return false;
            }

            return true;
        }
    }

    protected boolean canEqual(Object object) {
        return object instanceof TextFieldComponent;
    }

    @Override
    public int hashCode() {
        int i = (
                    (
                                (
                                            (
                                                        (
                                                                    (
                                                                                (
                                                                                            (
                                                                                                        (
                                                                                                                    (
                                                                                                                                (
                                                                                                                                            (
                                                                                                                                                        (
                                                                                                                                                                    (
                                                                                                                                                                                (
                                                                                                                                                                                            (
                                                                                                                                                                                                        59
                                                                                                                                                                                                            + (
                                                                                                                                                                                                                this.isFocused()
                                                                                                                                                                                                                    ? 79
                                                                                                                                                                                                                    : 97
                                                                                                                                                                                                            )
                                                                                                                                                                                                    )
                                                                                                                                                                                                    * 59
                                                                                                                                                                                                + this.getCursor()
                                                                                                                                                                                        )
                                                                                                                                                                                        * 59
                                                                                                                                                                                    + Float.floatToIntBits(
                                                                                                                                                                                        this.getFontSize()
                                                                                                                                                                                    )
                                                                                                                                                                            )
                                                                                                                                                                            * 59
                                                                                                                                                                        + this.getColor()
                                                                                                                                                                )
                                                                                                                                                                * 59
                                                                                                                                                            + Float.floatToIntBits(
                                                                                                                                                                this.getWidth()
                                                                                                                                                            )
                                                                                                                                                    )
                                                                                                                                                    * 59
                                                                                                                                                + (
                                                                                                                                                    this.isPassword()
                                                                                                                                                        ? 79
                                                                                                                                                        : 97
                                                                                                                                                )
                                                                                                                                        )
                                                                                                                                        * 59
                                                                                                                                    + (
                                                                                                                                        this.isNumeric()
                                                                                                                                            ? 79
                                                                                                                                            : 97
                                                                                                                                    )
                                                                                                                            )
                                                                                                                            * 59
                                                                                                                        + (this.isReplaceOnInput() ? 79 : 97)
                                                                                                                )
                                                                                                                * 59
                                                                                                            + (this.isMultiline() ? 79 : 97)
                                                                                                    )
                                                                                                    * 59
                                                                                                + Float.floatToIntBits(this.getLineSpacing())
                                                                                        )
                                                                                        * 59
                                                                                    + Float.floatToIntBits(this.getScrollOffset())
                                                                            )
                                                                            * 59
                                                                        + (this.isApplyingEdit() ? 79 : 97)
                                                                )
                                                                * 59
                                                            + this.getSelectionAnchor()
                                                    )
                                                    * 59
                                                + (this.isDragging() ? 79 : 97)
                                        )
                                        * 59
                                    + this.getClickCount()
                            )
                            * 59
                        + Float.floatToIntBits(this.getLastClickX())
                )
                * 59
            + Float.floatToIntBits(this.getLastClickY());
        String string = this.getText();
        int i2 = i * 59 + (string == null ? 43 : string.hashCode());
        Vector2f vector2f = this.getPosition();
        int i3 = i2 * 59 + (vector2f == null ? 43 : vector2f.hashCode());
        MsdfFont msdfFont = this.getFont();
        int i4 = i3 * 59 + (msdfFont == null ? 43 : msdfFont.hashCode());
        String string2 = this.getPlaceholder();
        int i5 = i4 * 59 + (string2 == null ? 43 : string2.hashCode());
        List list = this.getUndoStack();
        int i6 = i5 * 59 + (list == null ? 43 : list.hashCode());
        List list2 = this.getRedoStack();
        int i7 = i6 * 59 + (list2 == null ? 43 : list2.hashCode());
        StopWatch stopWatch = this.getClickTimer();
        int i8 = i7 * 59 + (stopWatch == null ? 43 : stopWatch.hashCode());
        StopWatch stopWatch2 = this.getCursorBlinkTimer();
        return stopWatch2 == null ? i8 * 59 + 43 : i8 * 59 + stopWatch2.hashCode();
    }

    @Override
    public String toString() {
        return "TextBox(text="
            + this.getText()
            + ", position="
            + this.getPosition()
            + ", selected="
            + this.isFocused()
            + ", cursor="
            + this.getCursor()
            + ", font="
            + this.getFont()
            + ", fontSize="
            + this.getFontSize()
            + ", color="
            + this.getColor()
            + ", emptyText="
            + this.getPlaceholder()
            + ", width="
            + this.getWidth()
            + ", hideCharacters="
            + this.isPassword()
            + ", onlyNumbers="
            + this.isNumeric()
            + ", replaceOnType="
            + this.isReplaceOnInput()
            + ", multiLine="
            + this.isMultiline()
            + ", lineGap="
            + this.getLineSpacing()
            + ", scrollX="
            + this.getScrollOffset()
            + ", undoStack="
            + this.getUndoStack()
            + ", redoStack="
            + this.getRedoStack()
            + ", undoGuard="
            + this.isApplyingEdit()
            + ", selectionAnchor="
            + this.getSelectionAnchor()
            + ", dragging="
            + this.isDragging()
            + ", clickCount="
            + this.getClickCount()
            + ", lastClickX="
            + this.getLastClickX()
            + ", lastClickY="
            + this.getLastClickY()
            + ", clickTimer="
            + this.getClickTimer()
            + ", caretBlink="
            + this.getCursorBlinkTimer()
            + ")";
    }
}
