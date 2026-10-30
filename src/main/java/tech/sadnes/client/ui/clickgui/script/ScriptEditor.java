package tech.sadnes.client.ui.clickgui.script;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptLinter;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.script.ScriptRuntimeSandbox;
import tech.sadnes.client.script.editor.FilePreview;
import tech.sadnes.client.script.editor.ScriptApiDocs;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.clickgui.component.ColorPickerPopup;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorTagUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class ScriptEditor implements IInteractable, IRenderable, IMinecraft {
    private static final float HEADER_HEIGHT = 26.0F;
    private static final float FOOTER_HEIGHT = 13.0F;
    private static final float BASE_LINE_HEIGHT = 9.5F;
    private static final float BASE_FONT_SIZE = 6.0F;
    private static final float PADDING = 8.0F;
    private static final float FILE_TREE_WIDTH = 128.0F;
    private static final int TAB_SIZE = 4;
    private static final int MAX_VISIBLE_SUGGESTIONS = 8;
    private static final float SUGGESTION_HEIGHT = 14.0F;
    private static final int STRING_COLOR = ColorUtil.rgb(152, 195, 121);
    private static final int NUMBER_COLOR = ColorUtil.rgb(209, 154, 102);
    private static final int ERROR_COLOR = ColorUtil.rgb(255, 96, 96);
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("0[xX]([0-9a-fA-F]{8}|[0-9a-fA-F]{6})");
    private static final Pattern DECLARATION_PATTERN = Pattern.compile("(?m)^\\s*(function|var|let|const)\\s+([A-Za-z_$][\\w$]*)");
    private final ClickGuiScreen clickGui;
    private final Animation openAnimation = new Animation();
    private Script script;
    private Path filePath;
    private boolean open;
    private final List<String> lines = new ArrayList<>();
    private int caretLine;
    private int caretColumn;
    private int selectionLine = -1;
    private int selectionColumn = -1;
    private boolean selectingWithMouse;
    private float scrollY;
    private float scrollX;
    private float zoom = 1.0F;
    private boolean modified;
    private long lastEditTime;
    private boolean lintPending;
    private ScriptRuntimeSandbox.SyntaxError syntaxError;
    private List<ScriptLinter.Diagnostic> diagnostics = List.of();
    private boolean[] inBlockComment = new boolean[0];
    private final Deque<ScriptEditor.UndoState> undoStack = new ArrayDeque<>();
    private final Deque<ScriptEditor.UndoState> redoStack = new ArrayDeque<>();
    private long lastUndoPushTime;
    private final ScriptFileTree fileTree = new ScriptFileTree(this::openFile, this::reloadCurrentScript, this::saveIfModified);
    private final ColorPickerPopup colorPicker = new ColorPickerPopup();
    private int colorLine = -1;
    private int colorStart;
    private int colorEnd;
    private int colorDigits;
    private boolean editingColor;
    private final FilePreview filePreview = new FilePreview();
    private boolean textFile = true;
    private final List<ScriptApiDocs.ApiEntry> suggestions = new ArrayList<>();
    private int selectedSuggestion;
    private int suggestionStart = -1;
    private float suggestionX;
    private float suggestionY;
    private float suggestionWidth;
    private final Animation suggestionAnimation = new Animation();
    private final Animation consoleHoverAnimation = new Animation();
    private final Animation saveHoverAnimation = new Animation();
    private final Animation folderHoverAnimation = new Animation();
    private final Animation closeHoverAnimation = new Animation();
    private final Animation modifiedAnimation = new Animation();
    private final List<ScriptEditor.ContextAction> contextActions = new ArrayList<>();
    private final List<float[]> contextActionBounds = new ArrayList<>();
    private final Animation contextMenuAnimation = new Animation();
    private boolean contextMenuOpen;
    private float contextMenuX;
    private float contextMenuY;
    private float contextMenuWidth;
    private float contextMenuHeight;
    private boolean draggingPreview;
    private float x;
    private float y;
    private float width;
    private float height;
    private float treeX;
    private float treeY;
    private float treeHeight;
    private float gutterWidth;
    private float textX;
    private float textY;
    private float textWidth;
    private float textHeight;
    private static final Set<String> JS_KEYWORDS = Set.of(
        "var",
        "let",
        "const",
        "function",
        "return",
        "if",
        "else",
        "for",
        "while",
        "do",
        "break",
        "continue",
        "new",
        "true",
        "false",
        "null",
        "undefined",
        "typeof",
        "instanceof",
        "in",
        "of",
        "switch",
        "case",
        "default",
        "try",
        "catch",
        "finally",
        "throw",
        "this",
        "delete",
        "void"
    );

    public ScriptEditor(ClickGuiScreen clickGuiScreen) {
        this.clickGui = clickGuiScreen;
    }

    public void openScript(Script script2) {
        if (!script2.isPackaged()) {
            this.script = script2;
            this.fileTree.setScript(script2);
            this.openFile(script2.getMainFile());
            this.open = true;
        }
    }

    private void openFile(Path path) {
        this.saveIfModified();
        this.filePath = path;
        this.fileTree.setSelectedPath(path);
        this.textFile = Files.isRegularFile(path) && FilePreview.isTextFile(path);
        if (!this.textFile) {
            this.filePreview.open(path);
        }

        this.lines.clear();

        try {
            for (String string : Files.readString(path).split("\n", -1)) {
                this.lines.add(string.replace("\r", ""));
            }
        } catch (Exception exception) {
            this.lines.add("");
        }

        if (this.lines.isEmpty()) {
            this.lines.add("");
        }

        this.caretLine = 0;
        this.caretColumn = 0;
        this.clearSelection();
        this.clearSuggestions();
        this.closeContextMenu();
        this.scrollY = 0.0F;
        this.scrollX = 0.0F;
        this.modified = false;
        this.syntaxError = null;
        this.lintPending = true;
        this.lastEditTime = 0L;
        this.undoStack.clear();
        this.redoStack.clear();
        this.computeBlockComments();
    }

    public void close() {
        if (this.open) {
            this.saveIfModified();
            this.clearSuggestions();
            this.closeContextMenu();
            this.open = false;
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean isVisible() {
        return this.open || this.openAnimation.get() > 0.001;
    }

    private void saveIfModified() {
        if (this.modified && this.filePath != null && Files.exists(this.filePath)) {
            this.save();
        }
    }

    private void save() {
        if (this.script != null && this.filePath != null && this.textFile) {
            try {
                Files.writeString(this.filePath, this.getText());
                this.modified = false;
                ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
                if (scriptManager != null) {
                    scriptManager.reloadScript(this.script);
                }

                NotificationUtil.success(
                    NotificationSources.scripts(), String.format(Language.translate("scripts.editor.saved", "%s сохранён"), this.script.getDisplayName())
                );
            } catch (Exception exception) {
                NotificationUtil.error(
                    NotificationSources.scripts(), Language.translate("scripts.editor.save_failed", "Не удалось сохранить") + ": " + exception.getMessage()
                );
            }
        }
    }

    private String getText() {
        return String.join("\n", this.lines);
    }

    private void reloadCurrentScript() {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null && this.script != null) {
            scriptManager.reloadScript(this.script);
        }
    }

    private float getLineHeight() {
        return 9.5F * this.zoom;
    }

    private float getFontSize() {
        return 6.0F * this.zoom;
    }

    public void render(DrawContext drawContext, int i, int i2, float f) {
        this.openAnimation.update();
        this.openAnimation.animate(this.open ? 1.0 : 0.0, this.open ? 0.25 : 0.18, this.open ? Easings.QUAD_OUT : Easings.QUAD_IN);
        float f2 = this.openAnimation.get();
        if (!(f2 <= 0.001F) && this.script != null) {
            float f3 = (float)Math.pow(this.clickGui.getOpenAnimation().get(), 2.0) * f2;
            this.width = Math.min(760.0F, GuiScale.getScaledWidth() * 0.85F);
            this.height = Math.min(450.0F, GuiScale.getScaledHeight() * 0.85F);
            this.x = GuiScale.getScaledWidth() / 2.0F - this.width / 2.0F;
            this.y = GuiScale.getScaledHeight() / 2.0F - this.height / 2.0F;
            this.updateLint();
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, this.x + this.width / 2.0F, this.y + this.height / 2.0F, 0.92F + 0.08F * f2);
            ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
            if (scriptManager == null || !scriptManager.renderEditorBackground(drawContext, this.x, this.y, this.width, this.height, 10.0F, f3)) {
                RenderUtil renderUtil = GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MAX, true, this.x, this.y, this.width, this.height, f3, 10.0F);
                GuiRenderUtil.applyOutline(renderUtil, f3 * 0.7F, 10.0F);
            }

            this.renderHeader(drawContext, i, i2, f3);
            this.treeX = this.x + 1.0F;
            this.treeY = this.y + 26.0F + 3.0F;
            this.treeHeight = this.y + this.height - 13.0F - 4.0F - this.treeY;
            this.fileTree.render(drawContext, this.treeX, this.treeY, 128.0F, this.treeHeight, i, i2, f3);
            this.gutterWidth = this.textFile ? Fonts.INTER_MEDIUM.getWidth(String.valueOf(this.lines.size()), this.getFontSize() - 0.5F) + 22.0F : 0.0F;
            this.textX = this.treeX + 128.0F + this.gutterWidth;
            this.textY = this.treeY;
            this.textWidth = this.x + this.width - this.textX - 8.0F;
            this.textHeight = this.treeHeight;
            this.clampScroll();
            if (this.textFile) {
                this.renderText(drawContext, f3);
                this.renderSuggestions(drawContext, f3);
                this.renderContextMenu(drawContext, i, i2, f3);
            } else {
                this.filePreview.render(drawContext, this.textX, this.textY, this.x + this.width - this.textX - 2.0F, this.textHeight, f3);
            }

            this.renderStatusBar(drawContext, f3);
            this.colorPicker.renderPicker(drawContext, i, i2, f3);
            MatrixUtil.pop(drawContext);
        }
    }

    private void renderHeader(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.x + 8.0F;
        float f3 = this.y + 13.0F - 14.0F / 2.0F;
        new RenderUtil(drawContext, f2, f3, 14.0F, 14.0F)
            .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.16F), f), Radius.smoothAll(5.0F))
            .icon(Icon.CODE, 14.0F / 2.0F - 4.0F, 14.0F / 2.0F - 4.0F, 8.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f));
        float f4 = f2 + 14.0F + 6.0F;
        String string = this.script.getDisplayName();
        String string2 = " / " + this.getRelativePath(this.filePath);
        float f5 = Fonts.INTER_SEMIBOLD.getWidth(string, 7.5F);
        new RenderUtil(drawContext, this.x, this.y, this.width, 26.0F)
            .textMiddleLeft(Fonts.INTER_SEMIBOLD, string, f4 - this.x, 0.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f), 7.5F)
            .textMiddleLeft(
                Fonts.INTER_REGULAR,
                string2,
                f4 - this.x + f5 + 2.0F,
                0.5F,
                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.6F), f),
                6.0F
            );
        this.modifiedAnimation.update();
        this.modifiedAnimation.animate(this.modified ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f6 = this.modifiedAnimation.get();
        if (f6 > 0.001F) {
            float f7 = f4 + f5 + 2.0F + Fonts.INTER_REGULAR.getWidth(string2, 6.0F) + 5.0F;
            new RenderUtil(drawContext, f7, this.y + 13.0F - 1.5F, 3.0F, 3.0F)
                .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), f6), f), Radius.all(1.5F));
        }

        this.renderHeaderButton(
            drawContext,
            Icon.RECTANGLE_LIST,
            this.getHeaderButtonX(3),
            this.consoleHoverAnimation,
            i,
            i2,
            f,
            this.clickGui.getScriptWindowManager().isWindowOpen(this.script)
        );
        this.renderHeaderButton(drawContext, Icon.DEVICE_FLOPPY, this.getHeaderButtonX(2), this.saveHoverAnimation, i, i2, f, this.modified);
        this.renderHeaderButton(drawContext, Icon.FOLDER, this.getHeaderButtonX(1), this.folderHoverAnimation, i, i2, f, false);
        this.renderHeaderButton(drawContext, Icon.CLOSE, this.getHeaderButtonX(0), this.closeHoverAnimation, i, i2, f, false);
        new RenderUtil(drawContext, this.x + 1.0F, this.y + 26.0F, this.width - 2.0F, 1.0F)
            .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getOutline(), 0.5F), f));
    }

    private float getHeaderButtonX(int i) {
        return this.x + this.width - 17.0F - i * 16.0F;
    }

    private boolean isHeaderButtonHovered(double d, double d2, int i) {
        float f = this.getHeaderButtonX(i);
        return this.isHovered(d, d2, f - 2.5F, this.y + 13.0F - 7.0F, 14.0, 14.0);
    }

    private void renderHeaderButton(DrawContext drawContext, Icon icon2, float f, Animation animation, int i, int i2, float f2, boolean flag) {
        animation.update();
        animation.animate(this.isHovered(i, i2, f - 2.5F, this.y + 13.0F - 7.0F, 14.0, 14.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f3 = animation.get();
        if (f3 > 0.001F) {
            GuiRenderUtil.drawHighlight(drawContext, f - 2.5F, this.y + 13.0F - 7.0F, 14.0F, 14.0F, f2 * f3, Radius.all(4.5F));
        }

        int i3 = flag ? Theme.getPrimary() : Theme.getSecondaryText();
        new RenderUtil(drawContext, f, this.y + 13.0F - 4.5F, 9.0F, 9.0F)
            .icon(icon2, 0.0F, 0.0F, 9.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(i3, Math.min(1.0F, (flag ? 0.85F : 0.6F) + 0.35F * f3)), f2));
    }

    private void renderText(DrawContext drawContext, float f) {
        if (ScissorStack.push(drawContext, this.treeX + 128.0F, this.textY, this.x + this.width - this.treeX - 128.0F - 1.0F, this.textHeight)) {
            try {
                MsdfFont msdfFont = Fonts.INTER_MEDIUM;
                float f2 = this.getLineHeight();
                float f3 = this.getFontSize();
                int i = Math.max(0, (int)(this.scrollY / f2));
                int i2 = Math.min(this.lines.size() - 1, (int)((this.scrollY + this.textHeight) / f2) + 1);
                int i3 = this.getErrorLine() - 1;
                byte b = -1;
                byte b2 = 0;
                byte b3 = -1;
                byte b4 = 0;
                if (this.hasSelection()) {
                    boolean flag = this.selectionLine < this.caretLine || this.selectionLine == this.caretLine && this.selectionColumn <= this.caretColumn;
                    b = (byte)(flag ? this.selectionLine : this.caretLine);
                    b2 = (byte)(flag ? this.selectionColumn : this.caretColumn);
                    b3 = (byte)(flag ? this.caretLine : this.selectionLine);
                    b4 = (byte)(flag ? this.caretColumn : this.selectionColumn);
                }

                float f4 = this.treeX + 128.0F;
                float f5 = this.x + this.width - f4 - 1.0F;

                for (int i4 = i; i4 <= i2; i4++) {
                    float f6 = this.textY + i4 * f2 - this.scrollY;
                    String string = this.lines.get(i4);
                    if (i4 == i3) {
                        new RenderUtil(drawContext, f4, f6 - 1.0F, f5, f2).rect(ColorUtil.multiplyAlpha(Theme.withAlpha(ERROR_COLOR, 0.12F), f));
                    } else if (i4 == this.caretLine) {
                        new RenderUtil(drawContext, f4, f6 - 1.0F, f5, f2).rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.05F), f));
                    }

                    if (b >= 0 && i4 >= b && i4 <= b3) {
                        byte b5 = i4 == b ? b2 : 0;
                        int i5 = i4 == b3 ? b4 : string.length();
                        if (i5 > b5 || i4 < b3) {
                            float f7 = this.textX - this.scrollX + msdfFont.getWidth(string.substring(0, Math.min(b5, string.length())), f3);
                            float f8 = this.textX - this.scrollX + msdfFont.getWidth(string.substring(0, Math.min(i5, string.length())), f3);
                            if (i4 < b3) {
                                f8 += 3.0F;
                            }

                            new RenderUtil(drawContext, f7, f6 - 1.0F, Math.max(2.0F, f8 - f7), f2)
                                .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.25F), f));
                        }
                    }

                    String string2 = String.valueOf(i4 + 1);
                    Fonts.INTER_REGULAR
                        .draw(
                            drawContext,
                            string2,
                            this.textX - 8.0F - Fonts.INTER_REGULAR.getWidth(string2, f3 - 0.5F),
                            f6,
                            ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), i4 == this.caretLine ? 0.8F : 0.35F), f),
                            f3 - 0.5F
                        );
                    this.renderColorSwatch(drawContext, string, f6, f);
                    this.renderHighlightedLine(drawContext, msdfFont, string, i4, this.textX - this.scrollX, f6, f);
                }

                this.renderCaret(drawContext, f);
            } finally {
                ScissorStack.pop(drawContext);
            }
        }
    }

    private void renderColorSwatch(DrawContext drawContext, String string, float f, float f2) {
        Matcher matcher2 = HEX_COLOR_PATTERN.matcher(string);
        if (matcher2.find()) {
            int i = parseHexColor(matcher2.group());
            float f3 = Math.min(7.0F, this.getLineHeight() - 2.5F);
            float f4 = this.textX - this.gutterWidth + 2.0F;
            float f5 = f + (this.getLineHeight() - f3) / 2.0F - 1.0F;
            new RenderUtil(drawContext, f4 - 0.7F, f5 - 0.7F, f3 + 1.4F, f3 + 1.4F)
                .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getOutline(), 0.7F), f2), Radius.all(2.0F));
            new RenderUtil(drawContext, f4, f5, f3, f3).rect(ColorUtil.multiplyAlpha(i, f2), Radius.all(1.6F));
        }
    }

    private static int parseHexColor(String string) {
        String string2 = string.substring(2);
        long l = Long.parseLong(string2, 16);
        return string2.length() == 6 ? (int)(l | 4278190080L) : (int)l;
    }

    private boolean openColorPicker(int i, float f) {
        if (i >= 0 && i < this.lines.size()) {
            Matcher matcher2 = HEX_COLOR_PATTERN.matcher(this.lines.get(i));
            if (!matcher2.find()) {
                return false;
            }

            this.colorLine = i;
            this.colorStart = matcher2.start();
            this.colorEnd = matcher2.end();
            this.colorDigits = matcher2.group().length() - 2;
            this.colorPicker
                .show(parseHexColor(matcher2.group()), this.colorDigits == 8, this.textX + 6.0F, f + this.getLineHeight(), this::applyPickedColor);
            return true;
        } else {
            return false;
        }
    }

    private void applyPickedColor(int i) {
        if (this.colorLine >= 0 && this.colorLine < this.lines.size()) {
            String string = this.lines.get(this.colorLine);
            if (this.colorEnd <= string.length()) {
                String string2 = this.colorDigits == 6 ? String.format("0x%06X", i & 16777215) : String.format("0x%08X", i);
                this.lines.set(this.colorLine, string.substring(0, this.colorStart) + string2 + string.substring(this.colorEnd));
                this.colorEnd = this.colorStart + string2.length();
                this.editingColor = true;
                this.onTextChanged();
                this.editingColor = false;
            }
        }
    }

    private void renderCaret(DrawContext drawContext, float f) {
        if (System.currentTimeMillis() / 500L % 2L == 0L) {
            String string = this.lines.get(this.caretLine);
            float f2 = this.textX
                - this.scrollX
                + Fonts.INTER_MEDIUM.getWidth(string.substring(0, Math.min(this.caretColumn, string.length())), this.getFontSize());
            float f3 = this.textY + this.caretLine * this.getLineHeight() - this.scrollY;
            new RenderUtil(drawContext, f2, f3 - 1.0F, 1.0F, this.getLineHeight() - 1.0F).rect(ColorUtil.multiplyAlpha(Theme.getTextColor(), f));
        }
    }

    private void renderStatusBar(DrawContext drawContext, float f) {
        float f2 = this.y + this.height - 13.0F - 2.0F;
        if (!this.textFile) {
            new RenderUtil(drawContext, this.x, f2, this.width, 13.0F)
                .icon(Icon.EYE, 8.0F, 3.0F, 7.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.7F), f))
                .textMiddleLeft(
                    Fonts.INTER_REGULAR,
                    Language.translate("scripts.editor.readonly", "Просмотр — файл не текстовый"),
                    18.0F,
                    0.0F,
                    ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.6F), f),
                    5.5F
                );
        } else {
            String string = "Ln "
                + (this.caretLine + 1)
                + ", Col "
                + (this.caretColumn + 1)
                + (this.zoom != 1.0F ? "   " + Math.round(this.zoom * 100.0F) + "%" : "");
            new RenderUtil(drawContext, this.x, f2, this.width, 13.0F)
                .textMiddleLeft(
                    Fonts.INTER_REGULAR,
                    string,
                    this.width - 8.0F - Fonts.INTER_REGULAR.getWidth(string, 5.5F),
                    0.0F,
                    ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.6F), f),
                    5.5F
                );
            if (!this.isJavaScript()) {
                String string2 = FilePreview.getExtension(this.filePath).toUpperCase(Locale.ROOT);
                new RenderUtil(drawContext, this.x, f2, this.width, 13.0F)
                    .textMiddleLeft(Fonts.INTER_REGULAR, string2, 8.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.5F), f), 5.5F);
            } else if (this.syntaxError != null || !this.diagnostics.isEmpty()) {
                boolean flag = this.syntaxError != null;
                int i = flag ? this.syntaxError.line() : this.diagnostics.get(0).line();
                String string3 = flag ? this.syntaxError.message() : this.diagnostics.get(0).message();
                String string4 = Language.translate("scripts.editor.error_line", "Строка")
                    + " "
                    + i
                    + ": "
                    + string3
                    + (!flag && this.diagnostics.size() > 1 ? "  (+" + (this.diagnostics.size() - 1) + ")" : "");
                new RenderUtil(drawContext, this.x, f2, this.width, 13.0F)
                    .icon(Icon.WARNING, 8.0F, 3.0F, 7.0F, ColorUtil.multiplyAlpha(ERROR_COLOR, f))
                    .textMiddleLeft(
                        Fonts.INTER_REGULAR,
                        this.trimToWidth(string4, this.width - 16.0F - 110.0F, 5.5F),
                        18.0F,
                        0.0F,
                        ColorUtil.multiplyAlpha(ERROR_COLOR, f),
                        5.5F
                    );
            } else if (this.lintPending) {
                new RenderUtil(drawContext, this.x, f2, this.width, 13.0F)
                    .textMiddleLeft(
                        Fonts.INTER_REGULAR,
                        Language.translate("scripts.editor.checking", "Проверка..."),
                        18.0F,
                        0.0F,
                        ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.5F), f),
                        5.5F
                    );
            } else {
                new RenderUtil(drawContext, this.x, f2, this.width, 13.0F)
                    .icon(Icon.CIRCLE_CHECK, 8.0F, 3.0F, 7.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(STRING_COLOR, 0.9F), f))
                    .textMiddleLeft(
                        Fonts.INTER_REGULAR,
                        Language.translate("scripts.editor.ok", "Синтаксис в порядке"),
                        18.0F,
                        0.0F,
                        ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.7F), f),
                        5.5F
                    );
            }
        }
    }

    private void updateLint() {
        if (this.lintPending && System.currentTimeMillis() - this.lastEditTime >= 400L) {
            this.lintPending = false;
            this.diagnostics = List.of();
            if (!this.isJavaScript()) {
                this.syntaxError = null;
            } else {
                String string = this.getText();
                this.syntaxError = ScriptRuntimeSandbox.checkSyntax(string, this.getRelativePath(this.filePath));
                if (this.syntaxError == null) {
                    this.diagnostics = ScriptLinter.lint(string, this.getRelativePath(this.filePath), this.collectIncludedNames());
                }
            }
        }
    }

    private Set<String> collectIncludedNames() {
        HashSet hashSet = new HashSet();
        if (this.script == null) {
            return hashSet;
        }

        for (Path path : this.script.listSourceFiles()) {
            if (!path.equals(this.filePath)) {
                try {
                    Matcher matcher2 = DECLARATION_PATTERN.matcher(Files.readString(path));

                    while (matcher2.find()) {
                        hashSet.add(matcher2.group(2));
                    }
                } catch (Exception exception) {
                }
            }
        }

        return hashSet;
    }

    private int getErrorLine() {
        if (this.syntaxError != null) {
            return this.syntaxError.line();
        } else {
            return this.diagnostics.isEmpty() ? 0 : this.diagnostics.get(0).line();
        }
    }

    private boolean isJavaScript() {
        return this.filePath != null && this.filePath.toString().toLowerCase(Locale.ROOT).endsWith(".js");
    }

    private String getRelativePath(Path path) {
        if (path == null) {
            return "";
        }

        try {
            return this.script.getAssetsRoot().relativize(path).toString().replace('\\', '/');
        } catch (Exception exception) {
            return path.getFileName().toString();
        }
    }

    private void renderSuggestions(DrawContext drawContext, float f) {
        this.suggestionAnimation.update();
        this.suggestionAnimation.animate(this.suggestions.isEmpty() ? 0.0 : 1.0, 0.16, Easings.QUAD_OUT);
        if (!this.suggestions.isEmpty()) {
            float f2 = this.suggestionAnimation.get();
            float f3 = f * Math.min(1.0F, f2);
            int i = Math.min(8, this.suggestions.size());
            int i2 = Math.max(0, Math.min(this.selectedSuggestion - i + 2, this.suggestions.size() - i));
            this.suggestionWidth = 90.0F;

            for (ScriptApiDocs.ApiEntry apiEntry : this.suggestions) {
                this.suggestionWidth = Math.max(this.suggestionWidth, Fonts.INTER_MEDIUM.getWidth(apiEntry.signature(), 7.5F) + 38.0F);
            }

            this.suggestionWidth = Math.min(this.suggestionWidth, this.width * 0.6F);
            float f4 = i * 14.0F + 4.0F;
            String string = this.lines.get(this.caretLine);
            float f5 = this.textX
                - this.scrollX
                + Fonts.INTER_MEDIUM.getWidth(string.substring(0, Math.min(this.suggestionStart, string.length())), this.getFontSize());
            this.suggestionX = Math.min(f5, this.x + this.width - this.suggestionWidth - 4.0F);
            float f6 = this.textY + (this.caretLine + 1) * this.getLineHeight() - this.scrollY + 1.0F;
            this.suggestionY = f6 + f4 > this.y + this.height - 13.0F
                ? this.textY + this.caretLine * this.getLineHeight() - this.scrollY - f4 - 1.0F
                : f6;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, this.suggestionX + this.suggestionWidth / 2.0F, this.suggestionY + f4 / 2.0F, 0.92F + 0.08F * f2);
            GuiRenderUtil.drawPopup(drawContext, this.suggestionX, this.suggestionY, this.suggestionWidth, f4, f3, 4.0F);

            for (int i3 = 0; i3 < i; i3++) {
                ScriptApiDocs.ApiEntry apiEntry2 = this.suggestions.get(i2 + i3);
                float f7 = this.suggestionY + 2.0F + i3 * 14.0F;
                boolean flag = i2 + i3 == this.selectedSuggestion;
                if (flag) {
                    new RenderUtil(drawContext, this.suggestionX + 2.0F, f7, this.suggestionWidth - 4.0F, 14.0F)
                        .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.22F), f3), Radius.all(3.0F));
                }

                int i4 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), flag ? 1.0F : 0.85F), f3);
                new RenderUtil(drawContext, this.suggestionX, f7, this.suggestionWidth, 14.0F)
                    .icon(
                        apiEntry2.function() ? Icon.RUN : Icon.PACKAGE,
                        5.0F,
                        3.5F,
                        7.0F,
                        ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), flag ? 0.9F : 0.5F), f3)
                    )
                    .textMiddleLeft(Fonts.INTER_MEDIUM, apiEntry2.signature(), 15.0F, 0.0F, i4, 7.5F);
                if (!apiEntry2.detail().isEmpty()) {
                    float f8 = Fonts.INTER_REGULAR.getWidth(apiEntry2.detail(), 7.0F);
                    if (15.0F + Fonts.INTER_MEDIUM.getWidth(apiEntry2.signature(), 7.5F) + f8 + 10.0F < this.suggestionWidth) {
                        new RenderUtil(drawContext, this.suggestionX, f7, this.suggestionWidth - 6.0F, 14.0F)
                            .textMiddleLeft(
                                Fonts.INTER_REGULAR,
                                apiEntry2.detail(),
                                this.suggestionWidth - 6.0F - f8,
                                0.0F,
                                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.5F), f3),
                                7.0F
                            );
                    }
                }
            }

            MatrixUtil.pop(drawContext);
        }
    }

    private void updateSuggestions() {
        boolean flag = !this.suggestions.isEmpty();
        this.suggestions.clear();
        this.selectedSuggestion = 0;
        this.suggestionStart = -1;
        if (this.isJavaScript()) {
            String string = this.lines.get(this.caretLine);
            int i = Math.min(this.caretColumn, string.length());
            int i2 = i;

            while (i2 > 0 && isIdentifierChar(string.charAt(i2 - 1))) {
                i2--;
            }

            String string2 = string.substring(i2, i);
            List list;
            if (i2 > 0 && string.charAt(i2 - 1) == '.') {
                int i3 = i2 - 1;
                int i4 = i3;

                while (i4 > 0 && isIdentifierChar(string.charAt(i4 - 1))) {
                    i4--;
                }

                String string3 = string.substring(i4, i3);
                if (string3.isEmpty()) {
                    return;
                }

                String string4 = ScriptApiDocs.inferType(string3, this.getText());
                list = string4.isEmpty() ? this.collectObjectMembers(string3, string2) : ScriptApiDocs.completeMembers(string4, string2);
            } else {
                if (string2.length() < 1) {
                    return;
                }

                list = new ArrayList<>(ScriptApiDocs.completeGlobals(string2));
                this.addLocalSuggestions(list, string2);
            }

            if (!list.isEmpty()) {
                this.suggestions.addAll(list.size() > 40 ? list.subList(0, 40) : list);
                this.suggestionStart = i2;
                if (!flag) {
                    this.suggestionAnimation.forceValue(0.0);
                }
            }
        }
    }

    private void addLocalSuggestions(List<ScriptApiDocs.ApiEntry> apiEntries, String string) {
        HashSet hashSet = new HashSet();

        for (ScriptApiDocs.ApiEntry apiEntry : apiEntries) {
            hashSet.add(apiEntry.name());
        }

        String string2 = string.toLowerCase(Locale.ROOT);
        Matcher matcher2 = DECLARATION_PATTERN.matcher(this.getText());

        while (matcher2.find()) {
            String string3 = matcher2.group(2);
            if (string3.toLowerCase(Locale.ROOT).startsWith(string2) && hashSet.add(string3)) {
                boolean flag = matcher2.group(1).equals("function");
                apiEntries.add(new ScriptApiDocs.ApiEntry(string3, flag ? string3 + "(…)" : string3, "", flag, ""));
            }
        }

        for (String string4 : this.collectIncludedNames()) {
            if (string4.toLowerCase(Locale.ROOT).startsWith(string2) && hashSet.add(string4)) {
                apiEntries.add(new ScriptApiDocs.ApiEntry(string4, string4, "include", false, ""));
            }
        }
    }

    private List<ScriptApiDocs.ApiEntry> collectObjectMembers(String string, String string2) {
        String string3 = this.getText();
        Matcher matcher2 = Pattern.compile("\\b" + Pattern.quote(string) + "\\s*=\\s*\\{").matcher(string3);
        if (!matcher2.find()) {
            return List.of();
        }

        ArrayList arrayList = new ArrayList();
        String string4 = string2.toLowerCase(Locale.ROOT);
        HashSet hashSet = new HashSet();
        int i = 0;
        int i2 = matcher2.end() - 1;

        for (int i3 = string3.length(); i2 < i3; i2++) {
            char c = string3.charAt(i2);
            switch (c) {
                case '(':
                case '[':
                case '{':
                    i++;
                    break;
                case ')':
                case ']':
                case '}':
                    if (--i == 0) {
                        return arrayList;
                    }
                    break;
                case ':':
                    if (i == 1) {
                        int i4 = i2;

                        while (i4 > 0 && Character.isWhitespace(string3.charAt(i4 - 1))) {
                            i4--;
                        }

                        int i5 = i4;

                        while (i5 > 0 && isIdentifierChar(string3.charAt(i5 - 1))) {
                            i5--;
                        }

                        if (i4 > i5) {
                            String string5 = string3.substring(i5, i4);
                            boolean flag = string3.substring(i2 + 1, Math.min(i3, i2 + 30)).trim().startsWith("function");
                            if (string5.toLowerCase(Locale.ROOT).startsWith(string4) && hashSet.add(string5)) {
                                arrayList.add(new ScriptApiDocs.ApiEntry(string5, flag ? string5 + "(…)" : string5, "", flag, ""));
                            }
                        }
                    }
            }
        }

        return arrayList;
    }

    private void closeColorPicker() {
        this.colorPicker.close();
        this.colorLine = -1;
    }

    private void clearSuggestions() {
        this.suggestions.clear();
        this.suggestionStart = -1;
    }

    private void applySuggestion() {
        if (!this.suggestions.isEmpty() && this.suggestionStart >= 0) {
            ScriptApiDocs.ApiEntry apiEntry = this.suggestions.get(Math.min(this.selectedSuggestion, this.suggestions.size() - 1));
            this.pushUndo();
            String string = this.lines.get(this.caretLine);
            String string2 = string.substring(0, this.suggestionStart);
            String string3 = string.substring(Math.min(this.caretColumn, string.length()));
            boolean flag;
            if (apiEntry.function()) {
                flag = !apiEntry.signature().contains("()");
            } else {
                flag = false;
            }

            boolean flag2 = string3.startsWith("(");
            String string4 = apiEntry.name() + (apiEntry.function() && !flag2 ? "()" : "");
            this.lines.set(this.caretLine, string2 + string4 + string3);
            int i = string2.length() + apiEntry.name().length();
            if (!apiEntry.function() || flag2) {
                this.caretColumn = i;
                this.clearSuggestions();
                this.onTextChanged();
            } else if (flag) {
                this.caretColumn = i + 1;
                this.clearSuggestions();
                this.onTextChanged();
            } else {
                this.caretColumn = i + 2;
                this.clearSuggestions();
                this.onTextChanged();
            }
        }
    }

    private static boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private void openContextMenu(float f, float f2) {
        this.contextActions.clear();
        boolean flag = this.hasSelection();
        if (flag) {
            this.contextActions
                .add(
                    new ScriptEditor.ContextAction(
                        Language.translate("scripts.editor.action.copy", "Копировать"), () -> mc.keyboard.setClipboard(this.getSelectedText())
                    )
                );
            this.contextActions.add(new ScriptEditor.ContextAction(Language.translate("scripts.editor.action.cut", "Вырезать"), () -> {
                mc.keyboard.setClipboard(this.getSelectedText());
                this.deleteSelectedText();
            }));
        }

        this.contextActions.add(new ScriptEditor.ContextAction(Language.translate("scripts.editor.action.paste", "Вставить"), this::pasteFromClipboard));
        this.contextActions.add(new ScriptEditor.ContextAction("//", this::toggleLineComment));
        if (flag) {
            this.contextActions
                .add(new ScriptEditor.ContextAction(Language.translate("scripts.editor.action.delete", "Удалить"), this::deleteSelectedText));
        } else {
            this.contextActions
                .add(new ScriptEditor.ContextAction(Language.translate("scripts.editor.action.select_all", "Выделить всё"), this::selectAll));
        }

        this.contextMenuOpen = true;
        this.contextMenuAnimation.forceValue(0.0);
        this.contextMenuX = f;
        this.contextMenuY = f2;
    }

    private void closeContextMenu() {
        this.contextMenuOpen = false;
    }

    private void renderContextMenu(DrawContext drawContext, int i, int i2, float f) {
        this.contextMenuAnimation.update();
        this.contextMenuAnimation
            .animate(this.contextMenuOpen ? 1.0 : 0.0, this.contextMenuOpen ? 0.2 : 0.12, this.contextMenuOpen ? Easings.BACK_OUT : Easings.QUAD_IN);
        float f2 = this.contextMenuAnimation.get();
        if (!(f2 <= 0.001F) && !this.contextActions.isEmpty()) {
            float f3 = 4.0F;
            this.contextActionBounds.clear();

            for (ScriptEditor.ContextAction contextAction : this.contextActions) {
                float f4 = Fonts.INTER_MEDIUM.getWidth(contextAction.label(), 6.0F) + 8.0F;
                this.contextActionBounds.add(new float[]{f3, f4});
                f3 += f4 + 3.0F;
            }

            this.contextMenuWidth = f3 + 4.0F - 3.0F;
            this.contextMenuHeight = 13.0F + 4.0F * 2.0F;
            this.contextMenuX = Math.clamp(this.contextMenuX, this.x + 4.0F, this.x + this.width - this.contextMenuWidth - 4.0F);
            this.contextMenuY = Math.clamp(this.contextMenuY, this.y + 26.0F + 2.0F, this.y + this.height - 13.0F - this.contextMenuHeight - 2.0F);
            float f5 = f * Math.min(1.0F, f2);
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, this.contextMenuX + this.contextMenuWidth / 2.0F, this.contextMenuY + this.contextMenuHeight / 2.0F, 0.88F + 0.12F * f2);
            GuiRenderUtil.drawPopup(drawContext, this.contextMenuX, this.contextMenuY, this.contextMenuWidth, this.contextMenuHeight, f5, 6.0F);
            int i3 = 0;

            for (int i4 = this.contextActions.size(); i3 < i4; i3++) {
                float[] floats = this.contextActionBounds.get(i3);
                float f6 = this.contextMenuX + floats[0];
                float f7 = this.contextMenuY + 4.0F;
                boolean flag = this.isHovered(i, i2, f6, f7, floats[1], 13.0F);
                if (flag) {
                    GuiRenderUtil.drawHighlight(drawContext, f6, f7, floats[1], 13.0F, f5, Radius.all(4.0F));
                }

                new RenderUtil(drawContext, f6, f7, floats[1], 13.0F)
                    .textCenter(
                        Fonts.INTER_MEDIUM,
                        this.contextActions.get(i3).label(),
                        0.0F,
                        0.0F,
                        ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), flag ? 1.0F : 0.75F), f5),
                        6.0F
                    );
            }

            MatrixUtil.pop(drawContext);
        } else {
            this.contextActionBounds.clear();
        }
    }

    private boolean handleContextMenuClick(double d, double d2) {
        if (!this.contextMenuOpen && this.contextMenuAnimation.get() <= 0.001) {
            return false;
        }

        if (!this.isHovered(d, d2, this.contextMenuX, this.contextMenuY, this.contextMenuWidth, this.contextMenuHeight)) {
            this.closeContextMenu();
            return false;
        }

        int i = 0;

        for (int i2 = Math.min(this.contextActions.size(), this.contextActionBounds.size()); i < i2; i++) {
            float[] floats = this.contextActionBounds.get(i);
            if (this.isHovered(d, d2, this.contextMenuX + floats[0], this.contextMenuY + 4.0F, floats[1], 13.0)) {
                ScriptEditor.ContextAction contextAction = this.contextActions.get(i);
                this.closeContextMenu();
                contextAction.run().run();
                return true;
            }
        }

        return true;
    }

    private void toggleLineComment() {
        int i = this.hasSelection() ? Math.min(this.selectionLine, this.caretLine) : this.caretLine;
        int i2 = this.hasSelection() ? Math.max(this.selectionLine, this.caretLine) : this.caretLine;
        this.pushUndo();
        boolean flag = true;
        int i3 = i;

        int i4;
        while (true) {
            if (i3 > i2) {
                i4 = i;
                break;
            }

            String string = this.lines.get(i3).trim();
            if (!string.isEmpty() && !string.startsWith("//")) {
                flag = false;
                i4 = i;
                break;
            }

            i3++;
        }

        for (; i4 <= i2; i4++) {
            String string2 = this.lines.get(i4);
            if (!string2.trim().isEmpty()) {
                int i5 = 0;

                while (i5 < string2.length() && string2.charAt(i5) == ' ') {
                    i5++;
                }

                this.lines
                    .set(
                        i4,
                        flag
                            ? string2.substring(0, i5) + string2.substring(i5 + (string2.startsWith("// ", i5) ? 3 : 2))
                            : string2.substring(0, i5) + "// " + string2.substring(i5)
                    );
            }
        }

        this.caretColumn = Math.min(this.caretColumn, this.lines.get(this.caretLine).length());
        if (this.selectionLine >= 0) {
            this.selectionColumn = Math.min(this.selectionColumn, this.lines.get(this.selectionLine).length());
        }

        this.onTextChanged();
    }

    private void pasteFromClipboard() {
        String string = mc.keyboard.getClipboard();
        if (string != null && !string.isEmpty()) {
            this.insertText(string);
        }
    }

    private void selectAll() {
        this.selectionLine = 0;
        this.selectionColumn = 0;
        this.caretLine = this.lines.size() - 1;
        this.caretColumn = this.lines.get(this.caretLine).length();
    }

    private void renderHighlightedLine(DrawContext drawContext, MsdfFont msdfFont, String string, int i, float f, float f2, float f3) {
        if (!string.isEmpty()) {
            float f4 = this.getFontSize();
            int i2 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.45F), f3);
            if (!this.isJavaScript()) {
                msdfFont.draw(drawContext, string, f, f2, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f3), f4);
            } else if (i < this.inBlockComment.length && this.inBlockComment[i] && !string.contains("*/")) {
                msdfFont.draw(drawContext, string, f, f2, i2, f4);
            } else {
                int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f3);
                int i4 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.85F), f3);
                int i5 = ColorUtil.multiplyAlpha(Theme.getPrimary(), f3);
                int i6 = ColorUtil.multiplyAlpha(STRING_COLOR, f3);
                int i7 = ColorUtil.multiplyAlpha(NUMBER_COLOR, f3);
                StringBuilder builder = new StringBuilder(string.length() + 64);
                int i8 = 0;
                int i9 = 0;
                boolean flag = i < this.inBlockComment.length && this.inBlockComment[i];
                int i10 = string.length();

                while (i9 < i10) {
                    int i11 = i9;
                    char c = string.charAt(i9);
                    int i12;
                    if (flag) {
                        int i13 = string.indexOf("*/", i9);
                        i9 = i13 < 0 ? i10 : i13 + 2;
                        if (i13 >= 0) {
                            flag = false;
                        }

                        i12 = i2;
                    } else {
                        label204: {
                            label172:
                            switch (c) {
                                case '"':
                                case '\'':
                                case '`':
                                    break;
                                case '/':
                                    if (i9 + 1 < i10 && string.charAt(i9 + 1) == '/') {
                                        i9 = i10;
                                        i12 = i2;
                                        break label204;
                                    }

                                    switch (c) {
                                        case '"':
                                        case '\'':
                                        case '`':
                                            break label172;
                                        case '/':
                                            if (i9 + 1 < i10 && string.charAt(i9 + 1) == '*') {
                                                int i14 = string.indexOf("*/", i9 + 2);
                                                i9 = i14 < 0 ? i10 : i14 + 2;
                                                if (i14 < 0) {
                                                    flag = true;
                                                }

                                                i12 = i2;
                                                break label204;
                                            }

                                            switch (c) {
                                                case '"':
                                                case '\'':
                                                case '`':
                                                    break label172;
                                            }
                                    }
                                default:
                                    if (Character.isDigit(c)) {
                                        while (i9 < i10 && (Character.isLetterOrDigit(string.charAt(i9)) || string.charAt(i9) == '.')) {
                                            i9++;
                                        }

                                        i12 = i7;
                                        break label204;
                                    }

                                    int i15;
                                    if (!Character.isLetter(c) && c != '_' && c != '$') {
                                        if (!Character.isWhitespace(c)) {
                                            while (
                                                i9 < i10
                                                    && !Character.isLetterOrDigit(string.charAt(i9))
                                                    && !Character.isWhitespace(string.charAt(i9))
                                                    && "\"'`/".indexOf(string.charAt(i9)) < 0
                                            ) {
                                                i9++;
                                            }

                                            if (i9 == i11) {
                                                i9++;
                                            }

                                            i12 = i4;
                                            break label204;
                                        }

                                        while (i9 < i10 && Character.isWhitespace(string.charAt(i9))) {
                                            i9++;
                                        }

                                        i15 = i8 == 0 ? i3 : i8;
                                    } else {
                                        while (i9 < i10 && isIdentifierChar(string.charAt(i9))) {
                                            i9++;
                                        }

                                        i15 = JS_KEYWORDS.contains(string.substring(i11, i9)) ? i5 : i3;
                                    }

                                    i12 = i15;
                                    break label204;
                            }

                            i9++;

                            while (i9 < i10 && (string.charAt(i9) != c || string.charAt(i9 - 1) == '\\')) {
                                i9++;
                            }

                            if (i9 < i10) {
                                i9++;
                            }

                            i12 = i6;
                        }
                    }

                    if (i12 != i8) {
                        builder.append(ColorTagUtil.colorTag(i12));
                        i8 = i12;
                    }

                    builder.append(string, i11, i9);
                }

                msdfFont.draw(drawContext, builder.toString(), f, f2, i3, f4);
            }
        }
    }

    private void computeBlockComments() {
        int i = this.lines.size();
        this.inBlockComment = new boolean[i];
        boolean flag = false;

        label54:
        for (int i2 = 0; i2 < i; i2++) {
            this.inBlockComment[i2] = flag;
            String string = this.lines.get(i2);
            int i3 = 0;
            int i4 = string.length();
            boolean flag2 = false;
            char c = 0;

            while (i3 < i4) {
                char c2 = string.charAt(i3);
                if (flag) {
                    if (c2 == '*' && i3 + 1 < i4 && string.charAt(i3 + 1) == '/') {
                        flag = false;
                        i3++;
                    }
                } else if (flag2) {
                    if (c2 == '\\') {
                        i3++;
                    } else if (c2 == c) {
                        flag2 = false;
                    }
                } else {
                    switch (c2) {
                        case '"':
                        case '\'':
                        case '`':
                            flag2 = true;
                            c = c2;
                            break;
                        case '/':
                            if (i3 + 1 < i4 && string.charAt(i3 + 1) == '/') {
                                continue label54;
                            }

                            if (c2 == '/' && i3 + 1 < i4 && string.charAt(i3 + 1) == '*') {
                                flag = true;
                                i3++;
                            }
                    }
                }

                i3++;
            }
        }
    }

    private void onTextChanged() {
        if (!this.editingColor) {
            this.closeColorPicker();
        }

        this.modified = true;
        this.lastEditTime = System.currentTimeMillis();
        this.lintPending = true;
        this.computeBlockComments();
        this.ensureCaretVisible();
    }

    private void pushUndo() {
        long l = System.currentTimeMillis();
        if (l - this.lastUndoPushTime >= 400L || this.undoStack.isEmpty()) {
            this.lastUndoPushTime = l;
            this.undoStack.push(new ScriptEditor.UndoState(this.getText(), this.caretLine, this.caretColumn));
            if (this.undoStack.size() > 100) {
                this.undoStack.removeLast();
            }

            this.redoStack.clear();
        }
    }

    private void restoreState(ScriptEditor.UndoState undoState) {
        this.lines.clear();

        for (String string : undoState.text().split("\n", -1)) {
            this.lines.add(string);
        }

        if (this.lines.isEmpty()) {
            this.lines.add("");
        }

        this.caretLine = Math.min(undoState.line(), this.lines.size() - 1);
        this.caretColumn = Math.min(undoState.col(), this.lines.get(this.caretLine).length());
        this.clearSelection();
        this.clearSuggestions();
        this.onTextChanged();
    }

    private void undo() {
        if (!this.undoStack.isEmpty()) {
            this.redoStack.push(new ScriptEditor.UndoState(this.getText(), this.caretLine, this.caretColumn));
            this.restoreState(this.undoStack.pop());
        }
    }

    private void redo() {
        if (!this.redoStack.isEmpty()) {
            this.undoStack.push(new ScriptEditor.UndoState(this.getText(), this.caretLine, this.caretColumn));
            this.lastUndoPushTime = System.currentTimeMillis();
            this.restoreState(this.redoStack.pop());
        }
    }

    private void insertText(String string) {
        this.pushUndo();
        this.deleteSelection();
        String[] strings = string.replace("\r", "").split("\n", -1);
        String string2 = this.lines.get(this.caretLine);
        String string3 = string2.substring(0, this.caretColumn);
        String string4 = string2.substring(this.caretColumn);
        if (strings.length == 1) {
            this.lines.set(this.caretLine, string3 + strings[0] + string4);
            this.caretColumn = this.caretColumn + strings[0].length();
            this.onTextChanged();
        } else {
            this.lines.set(this.caretLine, string3 + strings[0]);

            for (int i = 1; i < strings.length - 1; i++) {
                this.lines.add(this.caretLine + i, strings[i]);
            }

            this.lines.add(this.caretLine + strings.length - 1, strings[strings.length - 1] + string4);
            this.caretLine += strings.length - 1;
            this.caretColumn = strings[strings.length - 1].length();
            this.onTextChanged();
        }
    }

    private boolean hasSelection() {
        return this.selectionLine >= 0 && (this.selectionLine != this.caretLine || this.selectionColumn != this.caretColumn);
    }

    private void clearSelection() {
        this.selectionLine = -1;
        this.selectionColumn = -1;
    }

    private String getSelectedText() {
        if (!this.hasSelection()) {
            return "";
        }

        boolean flag = this.selectionLine < this.caretLine || this.selectionLine == this.caretLine && this.selectionColumn <= this.caretColumn;
        int i = flag ? this.selectionLine : this.caretLine;
        int i2 = flag ? this.selectionColumn : this.caretColumn;
        int i3 = flag ? this.caretLine : this.selectionLine;
        int i4 = flag ? this.caretColumn : this.selectionColumn;
        if (i == i3) {
            return this.lines.get(i).substring(i2, i4);
        }

        StringBuilder builder = new StringBuilder(this.lines.get(i).substring(i2));

        for (int i5 = i + 1; i5 < i3; i5++) {
            builder.append('\n').append(this.lines.get(i5));
        }

        return builder.append('\n').append(this.lines.get(i3), 0, i4).toString();
    }

    private void deleteSelection() {
        if (this.hasSelection()) {
            boolean flag = this.selectionLine < this.caretLine || this.selectionLine == this.caretLine && this.selectionColumn <= this.caretColumn;
            int i = flag ? this.selectionLine : this.caretLine;
            int i2 = flag ? this.selectionColumn : this.caretColumn;
            int i3 = flag ? this.caretLine : this.selectionLine;
            int i4 = flag ? this.caretColumn : this.selectionColumn;
            String string = this.lines.get(i).substring(0, i2) + this.lines.get(i3).substring(i4);

            for (int i5 = i3; i5 > i; i5--) {
                this.lines.remove(i5);
            }

            this.lines.set(i, string);
            this.caretLine = i;
            this.caretColumn = i2;
            this.clearSelection();
        }
    }

    private void deleteSelectedText() {
        this.pushUndo();
        this.deleteSelection();
        this.onTextChanged();
    }

    private void backspace() {
        if (this.hasSelection()) {
            this.deleteSelectedText();
        } else {
            this.pushUndo();
            if (this.caretColumn > 0) {
                String string = this.lines.get(this.caretLine);
                this.lines.set(this.caretLine, string.substring(0, this.caretColumn - 1) + string.substring(this.caretColumn));
                this.caretColumn--;
                this.onTextChanged();
            } else {
                if (this.caretLine > 0) {
                    String string2 = this.lines.get(this.caretLine - 1);
                    this.lines.set(this.caretLine - 1, string2 + this.lines.get(this.caretLine));
                    this.lines.remove(this.caretLine);
                    this.caretLine--;
                    this.caretColumn = string2.length();
                }

                this.onTextChanged();
            }
        }
    }

    private void deleteForward() {
        if (this.hasSelection()) {
            this.deleteSelectedText();
        } else {
            this.pushUndo();
            String string = this.lines.get(this.caretLine);
            if (this.caretColumn < string.length()) {
                this.lines.set(this.caretLine, string.substring(0, this.caretColumn) + string.substring(this.caretColumn + 1));
                this.onTextChanged();
            } else {
                if (this.caretLine < this.lines.size() - 1) {
                    this.lines.set(this.caretLine, string + this.lines.get(this.caretLine + 1));
                    this.lines.remove(this.caretLine + 1);
                }

                this.onTextChanged();
            }
        }
    }

    private void insertNewLine() {
        String string = this.lines.get(this.caretLine);
        int i = 0;
        int i2 = string.length();

        while (i < i2 && string.charAt(i) == ' ') {
            i++;
        }

        String string2 = string.substring(0, Math.min(i, this.caretColumn));
        if (this.caretColumn > 0 && this.caretColumn <= i2 && string.substring(0, this.caretColumn).trim().endsWith("{")) {
            string2 = string2 + " ".repeat(4);
        }

        this.insertText("\n" + string2);
    }

    private void moveCaret(int i, int i2, boolean flag) {
        if (flag && this.selectionLine < 0) {
            this.selectionLine = this.caretLine;
            this.selectionColumn = this.caretColumn;
        }

        if (!flag) {
            this.clearSelection();
        }

        this.caretLine = Math.clamp(i, 0, this.lines.size() - 1);
        this.caretColumn = Math.clamp(i2, 0, this.lines.get(this.caretLine).length());
        this.ensureCaretVisible();
    }

    private void ensureCaretVisible() {
        float f = this.getLineHeight();
        float f2 = this.caretLine * f;
        if (f2 < this.scrollY) {
            this.scrollY = f2;
        }

        if (f2 + f > this.scrollY + this.textHeight) {
            this.scrollY = f2 + f - this.textHeight;
        }

        String string = this.lines.get(this.caretLine);
        float f3 = Fonts.INTER_MEDIUM.getWidth(string.substring(0, Math.min(this.caretColumn, string.length())), this.getFontSize());
        if (f3 < this.scrollX) {
            this.scrollX = Math.max(0.0F, f3 - 20.0F);
        }

        if (f3 > this.scrollX + this.textWidth - 10.0F) {
            this.scrollX = f3 - this.textWidth + 20.0F;
        }

        this.clampScroll();
    }

    private void clampScroll() {
        float f = Math.max(0.0F, this.lines.size() * this.getLineHeight() - this.textHeight);
        this.scrollY = Math.clamp(this.scrollY, 0.0F, f);
        this.scrollX = Math.max(0.0F, this.scrollX);
    }

    private int getColumnAt(String string, float f) {
        if (f <= 0.0F) {
            return 0;
        }

        float f2 = this.getFontSize();
        int i = string.length();

        for (int i2 = 1; i2 <= i; i2++) {
            float f3 = Fonts.INTER_MEDIUM.getWidth(string.substring(0, i2), f2);
            if (f3 > f) {
                float f4 = Fonts.INTER_MEDIUM.getWidth(string.substring(0, i2 - 1), f2);
                if (f - f4 < f3 - f) {
                    return i2 - 1;
                }

                return i2;
            }
        }

        return i;
    }

    private void moveCaretToMouse(double d, double d2, boolean flag) {
        int i = (int)((d2 - this.textY + this.scrollY) / this.getLineHeight());
        int i2 = Math.clamp(i, 0, this.lines.size() - 1);
        int i3 = this.getColumnAt(this.lines.get(i2), (float)(d - this.textX + this.scrollX));
        this.moveCaret(i2, i3, flag);
    }

    public boolean keyPressed(KeyInput keyInput) {
        if (!this.open) {
            return false;
        }

        if (this.fileTree.keyPressed(keyInput)) {
            return true;
        }

        int i = keyInput.key();
        if (!this.textFile && i != 256) {
            return true;
        }

        boolean flag = (keyInput.modifiers() & 2) != 0;
        boolean flag2 = (keyInput.modifiers() & 1) != 0;
        if (!this.suggestions.isEmpty()) {
            switch (i) {
                case 256:
                    this.clearSuggestions();
                    return true;
                case 257:
                case 258:
                case 335:
                    this.applySuggestion();
                    return true;
                case 264:
                    this.selectedSuggestion = (this.selectedSuggestion + 1) % this.suggestions.size();
                    return true;
                case 265:
                    this.selectedSuggestion = (this.selectedSuggestion - 1 + this.suggestions.size()) % this.suggestions.size();
                    return true;
            }
        }

        if (i != 256 || !this.contextMenuOpen && !(this.contextMenuAnimation.get() > 0.001)) {
            this.closeContextMenu();
            if (i == 256 && this.colorPicker.isOpen()) {
                this.closeColorPicker();
                return true;
            }

            if (i == 256) {
                this.close();
                return true;
            }

            if (flag && i == 83) {
                this.save();
                return true;
            }

            if (flag && i == 32) {
                this.updateSuggestions();
                return true;
            }

            if (flag && i == 65) {
                this.selectAll();
                return true;
            }

            if (flag && i == 67) {
                if (this.hasSelection()) {
                    mc.keyboard.setClipboard(this.getSelectedText());
                }
            } else if (flag && i == 88) {
                if (this.hasSelection()) {
                    mc.keyboard.setClipboard(this.getSelectedText());
                    this.deleteSelectedText();
                    return true;
                }
            } else {
                if (flag && i == 86) {
                    this.pasteFromClipboard();
                    return true;
                }

                if (flag && i == 47) {
                    this.toggleLineComment();
                    return true;
                }

                if (flag && i == 90) {
                    this.undo();
                    return true;
                }

                if (flag && i == 89) {
                    this.redo();
                    return true;
                }

                switch (i) {
                    case 257:
                    case 335:
                        this.insertNewLine();
                        return true;
                    case 258:
                        this.insertText(" ".repeat(4));
                        return true;
                    case 259:
                        this.backspace();
                        this.updateSuggestions();
                        return true;
                    case 261:
                        this.deleteForward();
                        return true;
                    case 262:
                        if (this.caretColumn < this.lines.get(this.caretLine).length()) {
                            this.moveCaret(this.caretLine, this.caretColumn + 1, flag2);
                            this.clearSuggestions();
                            return true;
                        }

                        if (this.caretLine < this.lines.size() - 1) {
                            this.moveCaret(this.caretLine + 1, 0, flag2);
                        }

                        this.clearSuggestions();
                        return true;
                    case 263:
                        if (this.caretColumn > 0) {
                            this.moveCaret(this.caretLine, this.caretColumn - 1, flag2);
                            this.clearSuggestions();
                            return true;
                        }

                        if (this.caretLine > 0) {
                            this.moveCaret(this.caretLine - 1, this.lines.get(this.caretLine - 1).length(), flag2);
                        }

                        this.clearSuggestions();
                        return true;
                    case 264:
                        this.moveCaret(this.caretLine + 1, this.caretColumn, flag2);
                        return true;
                    case 265:
                        this.moveCaret(this.caretLine - 1, this.caretColumn, flag2);
                        return true;
                    case 266:
                        this.moveCaret(this.caretLine - (int)(this.textHeight / this.getLineHeight()), this.caretColumn, flag2);
                        return true;
                    case 267:
                        this.moveCaret(this.caretLine + (int)(this.textHeight / this.getLineHeight()), this.caretColumn, flag2);
                        return true;
                    case 268:
                        this.moveCaret(this.caretLine, 0, flag2);
                        return true;
                    case 269:
                        this.moveCaret(this.caretLine, this.lines.get(this.caretLine).length(), flag2);
                        return true;
                }
            }

            return true;
        } else {
            this.closeContextMenu();
            return true;
        }
    }

    public boolean charTyped(CharInput charInput) {
        if (!this.open) {
            return false;
        }

        if (this.fileTree.charTyped(charInput)) {
            return true;
        }

        if (!this.textFile) {
            return true;
        }

        this.closeContextMenu();
        if (charInput.codepoint() >= 32 && charInput.codepoint() != 127) {
            this.insertText(charInput.asString());
            char c = (char)charInput.codepoint();
            if (!isIdentifierChar(c) && c != '.') {
                this.clearSuggestions();
                return true;
            } else {
                this.updateSuggestions();
                return true;
            }
        } else {
            return true;
        }
    }

    public boolean mouseClicked(Click click, boolean flag) {
        if (!this.open) {
            return false;
        }

        double d = click.x();
        double d2 = click.y();
        if (this.colorPicker.isOpen()) {
            if (this.colorPicker.onMouseClick(click)) {
                return true;
            }

            this.closeColorPicker();
        }

        if ((this.fileTree.isContextMenuOpen() || this.fileTree.isEditing()) && this.fileTree.mouseClicked(click, flag)) {
            return true;
        }

        if (this.handleContextMenuClick(d, d2)) {
            return true;
        }

        if (!this.isHovered(d, d2, this.x, this.y, this.width, this.height)) {
            this.close();
            return true;
        }

        if (!this.suggestions.isEmpty()
            && this.isHovered(d, d2, this.suggestionX, this.suggestionY, this.suggestionWidth, this.suggestions.size() * 14.0F + 4.0F)) {
            int i = Math.min(8, this.suggestions.size());
            int i2 = Math.max(0, Math.min(this.selectedSuggestion - i + 2, this.suggestions.size() - i));
            int i3 = i2 + (int)((d2 - this.suggestionY - 2.0) / 14.0);
            if (i3 >= 0 && i3 < this.suggestions.size()) {
                this.selectedSuggestion = i3;
                this.applySuggestion();
            }
        } else {
            this.clearSuggestions();
            if (this.isHeaderButtonHovered(d, d2, 0)) {
                this.close();
                return true;
            }

            if (this.isHeaderButtonHovered(d, d2, 2)) {
                this.save();
                return true;
            }

            if (this.isHeaderButtonHovered(d, d2, 1)) {
                Util.getOperatingSystem().open(this.script.getAssetsRoot().toFile());
                return true;
            }

            if (this.isHeaderButtonHovered(d, d2, 3)) {
                this.clickGui.getScriptWindowManager().openWindow(this.script);
                return true;
            }

            if (this.fileTree.mouseClicked(click, flag)) {
                return true;
            }

            if (this.isRightButton(click.button())
                && this.textFile
                && this.isHovered(d, d2, this.textX - this.gutterWidth, this.textY, this.gutterWidth + this.textWidth, this.textHeight)) {
                this.openContextMenu((float)d - 10.0F, (float)d2 - 22.0F);
                return true;
            }

            if (!this.textFile
                && this.isLeftButton(click.button())
                && this.isHovered(d, d2, this.textX, this.textY, this.x + this.width - this.textX - 2.0F, this.textHeight)) {
                this.draggingPreview = true;
                return true;
            }

            if (this.textFile && this.isHovered(d, d2, this.textX - this.gutterWidth, this.textY, this.gutterWidth, this.textHeight)) {
                int i4 = (int)((d2 - this.textY + this.scrollY) / this.getLineHeight());
                if (this.openColorPicker(i4, this.textY + i4 * this.getLineHeight() - this.scrollY)) {
                    return true;
                }
            }

            if (this.textFile
                && this.isLeftButton(click.button())
                && this.isHovered(d, d2, this.textX - this.gutterWidth, this.textY, this.gutterWidth + this.textWidth, this.textHeight)) {
                this.moveCaretToMouse(d, d2, false);
                this.selectionLine = this.caretLine;
                this.selectionColumn = this.caretColumn;
                this.selectingWithMouse = true;
                return true;
            }
        }

        return true;
    }

    public boolean mouseDragged(Click click, double d, double d2) {
        if (!this.open) {
            return false;
        }

        if (this.colorPicker.onMouseDrag(click)) {
            return true;
        }

        if (this.fileTree.mouseDragged(click, d, d2)) {
            return true;
        }

        if (this.draggingPreview) {
            this.filePreview.panBy((float)d, (float)d2);
            return true;
        }

        if (!this.selectingWithMouse) {
            return true;
        }

        this.moveCaretToMouse(click.x(), click.y(), true);
        return true;
    }

    public boolean mouseReleased(Click click) {
        if (!this.open) {
            return false;
        }

        this.colorPicker.onMouseRelease();
        this.fileTree.mouseReleased(click);
        this.draggingPreview = false;
        boolean flag = this.selectingWithMouse;
        this.selectingWithMouse = false;
        if (this.selectionLine == this.caretLine && this.selectionColumn == this.caretColumn) {
            this.clearSelection();
        }

        if (flag && this.hasSelection()) {
            float f = this.textY + Math.min(this.selectionLine, this.caretLine) * this.getLineHeight() - this.scrollY;
            this.openContextMenu((float)click.x() - 12.0F, f - 23.0F);
        }

        return true;
    }

    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (!this.open) {
            return false;
        }

        if (this.isControlDown()) {
            if (this.textFile) {
                this.zoom = Math.clamp(this.zoom + (float)d4 * 0.1F, 0.6F, 2.5F);
                this.ensureCaretVisible();
                return true;
            } else {
                this.filePreview.zoomBy((float)d4 * 0.15F);
                return true;
            }
        } else {
            if (this.colorPicker.isOpen()) {
                this.closeColorPicker();
            }

            this.closeContextMenu();
            if (this.fileTree.mouseScrolled(d, d2, d4)) {
                return true;
            }

            if (!this.textFile) {
                this.filePreview.zoomBy((float)d4 * 0.15F);
                return true;
            }

            if (!this.suggestions.isEmpty()) {
                this.clearSuggestions();
            }

            if (Math.abs(d3) > 0.001) {
                this.scrollX = Math.max(0.0F, this.scrollX - (float)d3 * 20.0F);
            }

            this.scrollY = this.scrollY - (float)d4 * this.getLineHeight() * 3.0F;
            this.clampScroll();
            return true;
        }
    }

    private boolean isControlDown() {
        if (mc.getWindow() == null) {
            return false;
        }

        long l = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(l, 341) == 1 || GLFW.glfwGetKey(l, 345) == 1;
    }

    private String trimToWidth(String string, float f, float f2) {
        if (string == null) {
            return "";
        }

        if (!(f <= 0.0F) && !(Fonts.INTER_MEDIUM.getWidth(string, f2) <= f)) {
            StringBuilder builder = new StringBuilder();
            int i = 0;

            for (int i2 = string.length(); i < i2 && !(Fonts.INTER_MEDIUM.getWidth(builder.toString() + string.charAt(i) + "…", f2) > f); i++) {
                builder.append(string.charAt(i));
            }

            return builder + "…";
        } else {
            return string;
        }
    }

    record ContextAction(String label, Runnable run) {
    }

    record UndoState(String text, int line, int col) {
    }
}
