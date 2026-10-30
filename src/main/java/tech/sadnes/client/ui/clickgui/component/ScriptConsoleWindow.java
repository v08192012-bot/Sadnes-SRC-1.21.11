package tech.sadnes.client.ui.clickgui.component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptConsole;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class ScriptConsoleWindow extends Component implements IMinecraft {
    private static final float WIDTH = 340.0F;
    private static final float HEIGHT = 220.0F;
    private static final float HEADER_HEIGHT = 26.0F;
    private static final float INPUT_HEIGHT = 17.0F;
    private static final float PADDING = 8.0F;
    private static final float BUTTON_ICON_SIZE = 8.0F;
    private static final float TEXT_SIZE = 6.0F;
    private static final float LINE_HEIGHT = 9.5F;
    private static final float ENTRY_PADDING = 6.0F;
    private static final float ENTRY_RADIUS = 4.0F;
    private static final float ENTRY_GAP = 3.0F;
    private static final int ERROR_COLOR = ColorUtil.rgb(255, 96, 96);
    private static final int WARN_COLOR = ColorUtil.rgb(250, 200, 90);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private final String scriptName;
    private final int stackIndex;
    private final Animation openAnimation = new Animation();
    private final Animation clearHoverAnimation = new Animation();
    private final Animation closeHoverAnimation = new Animation();
    private final SmoothScrollbar scroll = new SmoothScrollbar();
    private final TextFieldComponent inputField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "scripts.console.input", false, false);
    private final List<String> history = new ArrayList<>();
    private int historyIndex;
    private boolean open;
    private boolean positioned;
    private boolean dragging;
    private boolean dragMoved;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean focused;
    private boolean followTail = true;
    private long lastRevision = -1L;
    private List<ScriptConsoleWindow.Row> rows = List.of();
    private float contentHeight;
    private int errorCount;
    private long seenErrorCount;
    private ScriptConsole.Entry selectionAnchor;
    private ScriptConsole.Entry selectionCaret;
    private boolean selecting;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private float listY;
    private float listHeight;
    private float inputX;
    private float inputY;
    private float inputWidth;
    private float clearButtonX;
    private float closeButtonX;
    private float headerButtonY;

    public ScriptConsoleWindow(String string, int i) {
        this.scriptName = string;
        this.stackIndex = i;
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean isShowing() {
        return this.open || this.openAnimation.get() > 0.001;
    }

    public boolean hasFocus() {
        return this.open && this.focused;
    }

    public boolean isMouseOverWindow(double d, double d2) {
        return this.open && this.isHovered(d, d2, this.position.x, this.position.y, 340.0, 220.0);
    }

    public boolean hasUnreadErrors() {
        return ScriptConsole.getErrorCount() > this.seenErrorCount;
    }

    public void toggle() {
        if (this.open) {
            this.close();
        } else {
            this.open();
        }
    }

    public void open() {
        this.open = true;
        this.dragging = false;
        this.followTail = true;
        if (!this.positioned) {
            float f = this.stackIndex * 18.0F;
            this.position
                .set(Math.max(0.0F, GuiScale.getScaledWidth() - 340.0F - 12.0F - f), Math.max(0.0F, GuiScale.getScaledHeight() - 220.0F - 12.0F - f));
            this.positioned = true;
        }

        this.openAnimation.animate(1.0, 0.3, Easings.QUART_OUT);
    }

    @Override
    public void close() {
        if (this.open) {
            this.open = false;
            this.unfocus();
            this.dragging = false;
            this.selecting = false;
            this.openAnimation.animate(0.0, 0.22, Easings.QUAD_IN);
        }
    }

    public void unfocus() {
        this.focused = false;
        this.inputField.setFocused(false);
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        this.openAnimation.update();
        float f2 = (float)Math.min(this.openAnimation.get(), 1.0);
        if (!(f2 <= 0.001F) && Fonts.INTER_MEDIUM != null) {
            if (this.open) {
                this.seenErrorCount = ScriptConsole.getErrorCount();
            }

            float f3 = this.position.x;
            float f4 = this.position.y;
            this.size.set(340.0F, 220.0F);
            float f5 = f2 * this.getAlpha();
            RenderUtil renderUtil = new RenderUtil(drawContext, f3, f4, 340.0F, 220.0F);
            renderUtil.blur(f5, Radius.all(9.0F), RenderUtil.BlurQuality.LOW, true);
            renderUtil.rect(Theme.withAlpha(Theme.getBackground(), 0.55F), f5, Radius.all(9.0F));
            new RenderUtil(drawContext, f3, f4 + 26.0F, 340.0F, 0.5F).rect(Theme.withAlpha(Theme.getOutline(), 0.4F * f5));
            this.inputX = f3 + 8.0F;
            this.inputWidth = 324.0F;
            this.inputY = f4 + 220.0F - 8.0F - 17.0F;
            this.listY = f4 + 26.0F + 4.0F;
            this.listHeight = this.inputY - 5.0F - this.listY;
            this.rebuildRows();
            this.updateSelection();
            this.renderHeader(drawContext, i, i2, f3, f4, f5);
            this.renderEntries(drawContext, i, i2, f3, f5);
            this.renderInput(drawContext, f5);
        }
    }

    private void renderHeader(DrawContext drawContext, int i, int i2, float f, float f2, float f3) {
        float f4 = f2 + 13.0F - 12.0F / 2.0F;
        new RenderUtil(drawContext, f + 8.0F, f4, 12.0F, 12.0F)
            .rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.16F), f3), Radius.smoothAll(4.0F))
            .icon(Icon.RECTANGLE_LIST, 12.0F / 2.0F - 3.5F, 12.0F / 2.0F - 3.5F, 7.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f3));
        String string = this.getTitle();
        float f5 = 8.0F + 12.0F + 5.0F;
        float f6 = Fonts.INTER_SEMIBOLD.getWidth(string, 7.0F);
        String string2 = String.format(Language.translate("scripts.console.count", "Записей: %s · ошибок: %s"), this.rows.size(), this.errorCount);
        new RenderUtil(drawContext, f, f2, 340.0F, 26.0F)
            .textMiddleLeft(Fonts.INTER_SEMIBOLD, string, f5, 0.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f3), 7.0F)
            .textMiddleLeft(
                Fonts.INTER_REGULAR,
                string2,
                f5 + f6 + 6.0F,
                0.5F,
                ColorUtil.multiplyAlpha(Theme.withAlpha(this.errorCount > 0 ? ERROR_COLOR : Theme.getSecondaryText(), 0.7F), f3),
                5.5F
            );
        this.headerButtonY = f2 + 13.0F - 4.0F;
        this.closeButtonX = f + 340.0F - 8.0F - 8.0F;
        this.clearButtonX = this.closeButtonX - 8.0F - 8.0F;
        this.renderHeaderButton(drawContext, Icon.TRASH, this.clearButtonX, this.clearHoverAnimation, i, i2, f3);
        this.renderHeaderButton(drawContext, Icon.CLOSE, this.closeButtonX, this.closeHoverAnimation, i, i2, f3);
    }

    private String getTitle() {
        if (this.scriptName == null) {
            return Language.translate("scripts.console.title", "Консоль скриптов");
        } else {
            Script script = this.findScript();
            String string = Language.translate("scripts.console.title.script", "Консоль: %s");
            Object[] objects = new Object[1];
            if (script == null) {
                objects[0] = this.scriptName;
                return String.format(string, objects);
            } else {
                objects[0] = script.getTitle();
                return String.format(string, objects);
            }
        }
    }

    private void renderHeaderButton(DrawContext drawContext, Icon icon2, float f, Animation animation, int i, int i2, float f2) {
        animation.update();
        animation.animate(this.isHovered(i, i2, f - 3.0F, this.headerButtonY - 3.0F, 14.0, 14.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f3 = animation.get();
        if (f3 > 0.001F) {
            GuiRenderUtil.drawHighlight(drawContext, f - 3.0F, this.headerButtonY - 3.0F, 14.0F, 14.0F, f2 * f3, Radius.all(4.0F));
        }

        new RenderUtil(drawContext, f, this.headerButtonY, 8.0F, 8.0F)
            .icon(icon2, 0.0F, 0.0F, 8.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.6F + 0.35F * f3), f2));
    }

    private void renderEntries(DrawContext drawContext, int i, int i2, float f, float f2) {
        if (this.rows.isEmpty()) {
            new RenderUtil(drawContext, f, this.listY, 340.0F, this.listHeight)
                .textCenter(
                    Fonts.INTER_REGULAR,
                    Language.translate("scripts.console.empty", "Здесь появится вывод print() и ошибки скриптов"),
                    0.0F,
                    0.0F,
                    ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.5F), f2),
                    6.0F
                );
        } else {
            if (this.followTail) {
                this.scroll.setScroll(Math.min(0.0F, this.listHeight - this.contentHeight));
            }

            this.scroll.update(this.contentHeight, this.listHeight);
            this.scroll.updateHover(i, i2);
            float f3 = getTimeColumnWidth();
            boolean flag = this.isHovered(i, i2, f, this.listY, 340.0, this.listHeight);
            if (ScissorStack.push(drawContext, f, this.listY, 340.0F, this.listHeight)) {
                try {
                    float f4 = this.listY + this.scroll.getScroll();
                    int i3 = 0;

                    for (int i4 = this.rows.size(); i3 < i4; i3++) {
                        ScriptConsoleWindow.Row row = this.rows.get(i3);
                        if (f4 + row.height() >= this.listY && f4 <= this.listY + this.listHeight) {
                            boolean flag2 = flag && !this.selecting && this.isHovered(i, i2, f + 8.0F, f4, 324.0, row.height());
                            this.renderEntry(drawContext, row, f, f4, f3, i3 >= this.selectionStart && i3 <= this.selectionEnd, flag2, f2);
                        }

                        f4 += row.height() + 3.0F;
                    }
                } finally {
                    ScissorStack.pop(drawContext);
                }
            }

            this.scroll.render(drawContext, f + 340.0F - 4.0F, this.listY, this.listHeight, this.contentHeight, f2);
        }
    }

    private void renderEntry(DrawContext drawContext, ScriptConsoleWindow.Row row, float f, float f2, float f3, boolean flag, boolean flag2, float f4) {
        ScriptConsole.Entry entry2 = row.entry();
        String string = entry2.level();
        boolean flag3 = "error".equals(string);
        boolean flag4 = "warn".equals(string);
        float f5 = f + 8.0F;
        int i = flag3 ? Theme.withAlpha(ERROR_COLOR, 0.12F) : (flag4 ? Theme.withAlpha(WARN_COLOR, 0.12F) : Theme.withAlpha(Theme.getTextColor(), 0.035F));
        RenderUtil renderUtil = new RenderUtil(drawContext, f5, f2, 324.0F, row.height()).rect(ColorUtil.multiplyAlpha(i, f4), Radius.all(4.0F));
        if (flag) {
            renderUtil.rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.22F), f4), Radius.all(4.0F));
        } else if (flag2) {
            GuiRenderUtil.drawHighlight(drawContext, f5, f2, 324.0F, row.height(), f4, Radius.all(4.0F));
        }

        float f6 = f5 + 6.0F;
        float f7 = 324.0F - 12.0F;
        float f8 = this.getScriptLabelWidth(entry2);
        int i2 = ColorUtil.multiplyAlpha(getLevelColor(string), f4);
        List list = row.lines();
        int i3 = 0;

        for (int i4 = list.size(); i3 < i4; i3++) {
            RenderUtil renderUtil2 = new RenderUtil(drawContext, f6, f2 + 4.0F + i3 * 9.5F, f7, 9.5F);
            if (i3 == 0) {
                renderUtil2.textMiddleLeft(
                    Fonts.INTER_MEDIUM, row.time(), 0.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.5F), f4), 6.0F
                );
                if (f8 > 0.0F) {
                    renderUtil2.textMiddleLeft(
                        Fonts.INTER_MEDIUM, entry2.script(), f3, 0.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.85F), f4), 6.0F
                    );
                }
            }

            renderUtil2.textMiddleLeft(Fonts.INTER_MEDIUM, (String)list.get(i3), i3 == 0 ? f3 + f8 : f3, 0.0F, i2, 6.0F);
        }
    }

    private static int getLevelColor(String string) {
        byte b = -1;
        switch (string.hashCode()) {
            case -934426595:
                if (string.equals("result")) {
                    b = 3;
                }
                break;
            case 3641990:
                if (string.equals("warn")) {
                    return Theme.withAlpha(WARN_COLOR, 0.9F);
                }
                break;
            case 96784904:
                if (string.equals("error")) {
                    return Theme.withAlpha(ERROR_COLOR, 0.9F);
                }
                break;
            case 100358090:
                if (string.equals("input")) {
                    return Theme.withAlpha(Theme.getPrimary(), 0.9F);
                }
        }

        switch (b) {
            case 0:
                return Theme.withAlpha(ERROR_COLOR, 0.9F);
            case 1:
                return Theme.withAlpha(WARN_COLOR, 0.9F);
            case 2:
                return Theme.withAlpha(Theme.getPrimary(), 0.9F);
            case 3:
                return Theme.withAlpha(Theme.getSecondaryText(), 0.85F);
            default:
                return Theme.withAlpha(Theme.getTextColor(), 0.9F);
        }
    }

    private void renderInput(DrawContext drawContext, float f) {
        GuiRenderUtil.drawOutlined(drawContext, this.inputX, this.inputY, this.inputWidth, 17.0F, f * 0.5F, 5.0F);
        float f2 = this.inputX + 6.0F;
        new RenderUtil(drawContext, f2, this.inputY + 8.5F - 3.5F, 7.0F, 7.0F)
            .icon(
                Icon.CARET_RIGHT,
                0.0F,
                0.0F,
                7.0F,
                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), this.inputField.isFocused() ? 0.95F : 0.55F), f)
            );
        float f3 = f2 + 11.0F;
        float f4 = this.inputX + this.inputWidth - 6.0F - f3;
        if (this.isInputLocked()) {
            new RenderUtil(drawContext, f3, this.inputY, f4, 17.0F)
                .textMiddleLeft(
                    Fonts.INTER_MEDIUM,
                    Language.translate("scripts.console.input.locked", "Ввод закрыт: скрипт собран в .ss"),
                    0.0F,
                    0.0F,
                    ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.4F), f),
                    6.0F
                );
        } else {
            this.inputField.render(drawContext, new Vector2f(f3, this.inputY + 8.5F - 3.0F), ColorUtil.multiplyAlpha(Theme.getTextColor(), f), f4);
        }
    }

    private static float getTimeColumnWidth() {
        return Fonts.INTER_MEDIUM.getWidth("00:00:00", 6.0F) + 7.0F;
    }

    private float getScriptLabelWidth(ScriptConsole.Entry entry) {
        return this.scriptName != null ? 0.0F : Fonts.INTER_MEDIUM.getWidth(entry.script(), 6.0F) + 6.0F;
    }

    private void rebuildRows() {
        long l = ScriptConsole.getRevision();
        if (l != this.lastRevision) {
            this.lastRevision = l;
            IdentityHashMap identityHashMap = new IdentityHashMap();

            for (ScriptConsoleWindow.Row row : this.rows) {
                identityHashMap.put(row.entry(), row);
            }

            float f = 312.0F - getTimeColumnWidth();
            ArrayList arrayList = new ArrayList();
            float f2 = 0.0F;
            int i = 0;

            for (ScriptConsole.Entry entry2 : ScriptConsole.getEntries(this.scriptName, Integer.MAX_VALUE)) {
                ScriptConsoleWindow.Row row2 = (ScriptConsoleWindow.Row)identityHashMap.get(entry2);
                if (row2 == null) {
                    List list = wrapText(formatEntryText(entry2), f - this.getScriptLabelWidth(entry2), f);
                    row2 = new ScriptConsoleWindow.Row(entry2, TIME_FORMAT.format(Instant.ofEpochMilli(entry2.time())), list, list.size() * 9.5F + 8.0F);
                }

                arrayList.add(row2);
                f2 += row2.height() + 3.0F;
                if ("error".equals(entry2.level())) {
                    i++;
                }
            }

            this.rows = arrayList;
            this.errorCount = i;
            this.contentHeight = Math.max(0.0F, f2 - 3.0F);
        }
    }

    private static String formatEntryText(ScriptConsole.Entry entry) {
        String string = entry.level();
        byte b = -1;
        switch (string.hashCode()) {
            case -934426595:
                if (string.equals("result")) {
                    b = 1;
                }
                break;
            case 100358090:
                if (string.equals("input")) {
                    return "> " + entry.text();
                }
        }

        switch (b) {
            case 0:
                return "> " + entry.text();
            case 1:
                return "< " + entry.text();
            default:
                return entry.text();
        }
    }

    private static List<String> wrapText(String string, float f, float f2) {
        ArrayList arrayList = new ArrayList();

        for (String string2 : string.split("\n", -1)) {
            StringBuilder builder = new StringBuilder();
            boolean flag = false;

            for (String string3 : string2.replace("\t", "    ").split(" ", -1)) {
                String string4 = flag ? builder + " " + string3 : string3;
                if (Fonts.INTER_MEDIUM.getWidth(string4, 6.0F) <= getLineWidth(arrayList, f, f2)) {
                    builder.setLength(0);
                    builder.append(string4);
                    flag = true;
                } else {
                    if (flag) {
                        arrayList.add(builder.toString());
                        builder.setLength(0);
                    }

                    int i = 0;

                    for (int i2 = string3.length(); i < i2; i++) {
                        char c = string3.charAt(i);
                        if (!builder.isEmpty() && Fonts.INTER_MEDIUM.getWidth(builder.toString() + c, 6.0F) > getLineWidth(arrayList, f, f2)) {
                            arrayList.add(builder.toString());
                            builder.setLength(0);
                        }

                        builder.append(c);
                    }

                    flag = !builder.isEmpty();
                }
            }

            arrayList.add(builder.toString());
        }

        return arrayList;
    }

    private static float getLineWidth(List<String> strings, float f, float f2) {
        return strings.isEmpty() ? f : f2;
    }

    private Script findScript() {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        return scriptManager != null && this.scriptName != null ? scriptManager.findScript(this.scriptName) : null;
    }

    private boolean isInputLocked() {
        Script script = this.findScript();
        return script != null && script.isPackaged();
    }

    private void submitInput() {
        String string = this.inputField.getText().trim();
        if (!string.isEmpty()) {
            this.inputField.setText("").setCursor(0);
            if (this.history.isEmpty() || !this.history.getLast().equals(string)) {
                this.history.add(string);
            }

            if (this.history.size() > 50) {
                this.history.removeFirst();
            }

            label31: {
                this.historyIndex = this.history.size();
                this.followTail = true;
                ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
                Script script;
                if (this.scriptName == null) {
                    if (scriptManager == null) {
                        break label31;
                    }

                    script = scriptManager.getConsoleScript();
                } else {
                    script = this.findScript();
                }

                Script script2 = script;
                if (script2 != null) {
                    script2.runConsoleInput(string);
                    return;
                }
            }

            String string2 = this.scriptName == null ? "console" : this.scriptName;
            ScriptConsole.log(string2, "input", string);
            ScriptConsole.error(string2, Language.translate("scripts.console.not_found", "Скрипт не найден"));
        }
    }

    private void navigateHistory(int i) {
        if (!this.history.isEmpty()) {
            this.historyIndex = Math.clamp(this.historyIndex + i, 0, this.history.size());
            String string = this.historyIndex < this.history.size() ? this.history.get(this.historyIndex) : "";
            this.inputField.setText(string).setCursor(string.length());
        }
    }

    private void updateSelection() {
        this.selectionStart = this.selectionEnd = -1;
        if (this.selectionAnchor != null && this.selectionCaret != null) {
            int i = this.indexOfEntry(this.selectionAnchor);
            int i2 = this.indexOfEntry(this.selectionCaret);
            if (i < 0 && i2 < 0) {
                this.clearSelection();
            } else {
                int i3 = Math.max(i, 0);
                int i4 = Math.max(i2, 0);
                this.selectionStart = Math.min(i3, i4);
                this.selectionEnd = Math.max(i3, i4);
            }
        }
    }

    private int indexOfEntry(ScriptConsole.Entry entry2) {
        int i = 0;

        for (int i2 = this.rows.size(); i < i2; i++) {
            if (this.rows.get(i).entry() == entry2) {
                return i;
            }
        }

        return -1;
    }

    private void clearSelection() {
        this.selectionAnchor = this.selectionCaret = null;
        this.selectionStart = this.selectionEnd = -1;
    }

    private void selectRow(int i, boolean flag) {
        ScriptConsole.Entry entry2 = this.rows.get(i).entry();
        if (!flag || this.selectionAnchor == null) {
            this.selectionAnchor = entry2;
        }

        this.selectionCaret = entry2;
        this.updateSelection();
    }

    private int rowIndexAtY(double d, boolean flag) {
        if (this.rows.isEmpty()) {
            return -1;
        }

        float f = this.listY + this.scroll.getScroll();
        if (d < f) {
            return flag ? 0 : -1;
        }

        int i = 0;

        for (int i2 = this.rows.size(); i < i2; i++) {
            float f2 = f + this.rows.get(i).height() + 3.0F;
            if (d < f2) {
                return i;
            }

            f = f2;
        }

        return flag ? this.rows.size() - 1 : -1;
    }

    private int rowIndexAt(double d, double d2) {
        if (!this.isHovered(d, d2, this.position.x + 8.0F, this.listY, 324.0, this.listHeight)) {
            return -1;
        }

        float f = this.listY + this.scroll.getScroll();
        int i = 0;

        for (int i2 = this.rows.size(); i < i2; i++) {
            float f2 = this.rows.get(i).height();
            if (d2 < f) {
                return -1;
            }

            if (d2 <= f + f2) {
                return i;
            }

            f += f2 + 3.0F;
        }

        return -1;
    }

    private void scrollToRow(int i) {
        float f = 0.0F;

        for (int i2 = 0; i2 < i; i2++) {
            f += this.rows.get(i2).height() + 3.0F;
        }

        float f2 = f + this.rows.get(i).height();
        float f3 = this.scroll.getTargetScroll();
        if (f + f3 < 0.0F) {
            this.scroll.setScroll(-f);
            this.followTail = this.isAtBottom();
        } else {
            if (f2 + f3 > this.listHeight) {
                this.scroll.setScroll(this.listHeight - f2);
            }

            this.followTail = this.isAtBottom();
        }
    }

    private void copySelection() {
        if (this.selectionStart >= 0) {
            StringBuilder builder = new StringBuilder();

            for (int i = this.selectionStart; i <= this.selectionEnd; i++) {
                ScriptConsoleWindow.Row row = this.rows.get(i);
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }

                builder.append('[').append(row.time()).append("] ").append(row.entry().script()).append(": ").append(formatEntryText(row.entry()));
            }

            mc.keyboard.setClipboard(builder.toString());
            NotificationUtil.info(
                NotificationSources.scripts(),
                String.format(Language.translate("scripts.console.copied", "Скопировано: %s"), this.selectionEnd - this.selectionStart + 1)
            );
        }
    }

    private void clearConsole() {
        if (this.scriptName == null) {
            ScriptConsole.clear();
        } else {
            ScriptConsole.clearScript(this.scriptName);
        }

        this.clearSelection();
        this.scroll.reset();
        this.followTail = true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        double d = click.x();
        double d2 = click.y();
        if (!this.isMouseOverWindow(d, d2)) {
            this.unfocus();
            return false;
        }

        this.focused = true;
        if (!this.isLeftButton(click.button())) {
            return true;
        }

        if (this.isHovered(d, d2, this.closeButtonX - 3.0F, this.headerButtonY - 3.0F, 14.0, 14.0)) {
            this.close();
            return true;
        }

        if (this.isHovered(d, d2, this.clearButtonX - 3.0F, this.headerButtonY - 3.0F, 14.0, 14.0)) {
            this.clearConsole();
            return true;
        }

        if (this.isHovered(d, d2, this.inputX, this.inputY, this.inputWidth, 17.0)) {
            if (this.isInputLocked()) {
                return true;
            }

            this.inputField.mouseClicked(d, d2, click.button());
            this.inputField.setFocused(true);
            return true;
        } else {
            this.inputField.setFocused(false);
            if (this.scroll.mouseClicked(d, d2)) {
                this.followTail = false;
                return true;
            }

            int i = this.rowIndexAt(d, d2);
            if (i >= 0) {
                this.selectRow(i, (click.modifiers() & 1) != 0);
                if (flag) {
                    this.copySelection();
                    return true;
                } else {
                    this.selecting = true;
                    return true;
                }
            } else {
                this.dragging = true;
                this.dragMoved = false;
                this.dragOffsetX = (float)d - this.position.x;
                this.dragOffsetY = (float)d2 - this.position.y;
                return true;
            }
        }
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.dragging) {
            this.dragMoved = true;
            this.position
                .set(
                    Math.clamp((float)click.x() - this.dragOffsetX, 0.0F, Math.max(0.0F, GuiScale.getScaledWidth() - 340.0F)),
                    Math.clamp((float)click.y() - this.dragOffsetY, 0.0F, Math.max(0.0F, GuiScale.getScaledHeight() - 220.0F))
                );
            return true;
        }

        if (this.selecting) {
            if (click.y() < this.listY) {
                this.scroll.onScroll(0.3);
            } else if (click.y() > this.listY + this.listHeight) {
                this.scroll.onScroll(-0.3);
            }

            int i = this.rowIndexAtY(click.y(), true);
            if (i >= 0) {
                this.selectRow(i, true);
            }

            this.followTail = this.isAtBottom();
            return true;
        } else {
            if (!this.scroll.mouseDragged(click.x(), click.y())) {
                return false;
            }

            this.followTail = this.isAtBottom();
            return true;
        }
    }

    @Override
    public boolean onMouseRelease(Click click) {
        if (this.dragging && !this.dragMoved) {
            this.clearSelection();
        }

        this.dragging = false;
        this.selecting = false;
        this.scroll.mouseReleased();
        return false;
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (!this.isMouseOverWindow(d, d2)) {
            return false;
        }

        this.scroll.onScroll(d4);
        this.followTail = this.isAtBottom();
        return true;
    }

    private boolean isAtBottom() {
        return this.scroll.getTargetScroll() <= Math.min(0.0F, this.listHeight - this.contentHeight) + 0.5F;
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        label124: {
            label123: {
                label136: {
                    if (this.open && this.focused) {
                        int i = keyInput.key();
                        if (this.inputField.isFocused()) {
                            switch (i) {
                                case 256:
                                    this.inputField.setFocused(false);
                                    return true;
                                case 257:
                                case 335:
                                    this.submitInput();
                                    return true;
                                case 264:
                                    this.navigateHistory(1);
                                    return true;
                                case 265:
                                    this.navigateHistory(-1);
                                    return true;
                                default:
                                    this.inputField.keyPressed(i);
                                    return true;
                            }
                        }

                        boolean flag = (keyInput.modifiers() & 2) != 0;
                        boolean flag2 = (keyInput.modifiers() & 1) != 0;
                        if (flag && i == 65) {
                            if (this.rows.isEmpty()) {
                                return true;
                            }

                            this.selectionAnchor = this.rows.getFirst().entry();
                            this.selectionCaret = this.rows.getLast().entry();
                            this.updateSelection();
                            return true;
                        }

                        if (flag && i == 67) {
                            this.copySelection();
                            return true;
                        }

                        label127: {
                            label128: {
                                if (flag) {
                                    switch (i) {
                                        case 76:
                                        case 261:
                                            break label124;
                                        case 256:
                                            break;
                                        case 264:
                                        case 265:
                                            break label128;
                                        case 266:
                                        case 267:
                                            break label127;
                                        case 268:
                                            break label123;
                                        case 269:
                                            break label136;
                                        default:
                                            return false;
                                    }
                                } else {
                                    switch (i) {
                                        case 256:
                                            break;
                                        case 261:
                                            break label124;
                                        case 264:
                                        case 265:
                                            break label128;
                                        case 266:
                                        case 267:
                                            break label127;
                                        case 268:
                                            break label123;
                                        case 269:
                                            break label136;
                                        default:
                                            return false;
                                    }
                                }

                                if (this.selectionStart >= 0) {
                                    this.clearSelection();
                                    return true;
                                }

                                switch (i) {
                                    case 264:
                                    case 265:
                                        break;
                                    case 266:
                                    case 267:
                                        break label127;
                                    case 268:
                                        break label123;
                                    case 269:
                                        break label136;
                                    default:
                                        return false;
                                }
                            }

                            if (!this.rows.isEmpty()) {
                                int i2 = this.selectionStart < 0 ? this.rows.size() : this.indexOfEntry(this.selectionCaret);
                                int i3 = Math.clamp(i2 + (i == 265 ? -1 : 1), 0, this.rows.size() - 1);
                                this.selectRow(i3, flag2);
                                this.scrollToRow(i3);
                                return true;
                            }

                            switch (i) {
                                case 266:
                                case 267:
                                    break;
                                case 268:
                                    break label123;
                                case 269:
                                    break label136;
                                default:
                                    return false;
                            }
                        }

                        this.scroll.onScroll((i == 266 ? 1.0 : -1.0) * this.listHeight / 22.0);
                        this.followTail = this.isAtBottom();
                        return true;
                    }

                    return false;
                }

                this.followTail = true;
                return true;
            }

            this.scroll.setScroll(0.0F);
            this.followTail = this.isAtBottom();
            return true;
        }

        this.clearConsole();
        return true;
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        return false;
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        if (this.open && this.focused && !this.isInputLocked()) {
            this.inputField.setFocused(true);
            this.inputField.charTyped(charInput.codepoint());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public void resize(int i, int i2) {
        this.init(i, i2);
    }

    @Override
    public void init(int i, int i2) {
        if (this.positioned) {
            this.position
                .set(
                    Math.clamp(this.position.x, 0.0F, Math.max(0.0F, GuiScale.getScaledWidth() - 340.0F)),
                    Math.clamp(this.position.y, 0.0F, Math.max(0.0F, GuiScale.getScaledHeight() - 220.0F))
                );
        }
    }

    @Override
    public void removed() {
        this.dragging = false;
        this.selecting = false;
        this.inputField.setFocused(false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    record Row(ScriptConsole.Entry entry, String time, List<String> lines, float height) {
    }
}
