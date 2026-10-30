package tech.sadnes.client.script.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.ui.clickgui.component.ScriptConsoleWindow;

public final class ScriptWindowManager {
    private final ScriptConsoleWindow mainWindow = new ScriptConsoleWindow(null, 0);
    private final Map<String, ScriptConsoleWindow> windowsByName = new HashMap<>();
    private final List<ScriptConsoleWindow> windows = new ArrayList<>(List.of(this.mainWindow));
    private ScriptConsoleWindow activeWindow;

    public ScriptConsoleWindow getMainWindow() {
        return this.mainWindow;
    }

    public void openWindow(Script script) {
        ScriptConsoleWindow scriptConsoleWindow = this.windowsByName.computeIfAbsent(script.getDisplayName(), string -> {
            ScriptConsoleWindow scriptConsoleWindow2 = new ScriptConsoleWindow(string, this.windowsByName.size() + 1);
            this.windows.add(scriptConsoleWindow2);
            return scriptConsoleWindow2;
        });
        scriptConsoleWindow.toggle();
        if (scriptConsoleWindow.isOpen()) {
            this.bringToFront(scriptConsoleWindow);
        }
    }

    public boolean isWindowOpen(Script script) {
        ScriptConsoleWindow scriptConsoleWindow = this.windowsByName.get(script.getDisplayName());
        return scriptConsoleWindow != null && scriptConsoleWindow.isOpen();
    }

    public void renderWindows(DrawContext drawContext, int i, int i2, float f) {
        ScriptConsoleWindow scriptConsoleWindow = this.getWindowAt(i, i2);

        for (ScriptConsoleWindow scriptConsoleWindow2 : this.windows) {
            if (scriptConsoleWindow2.isShowing()) {
                ScriptConsoleWindow scriptConsoleWindow3;
                DrawContext drawContext2;
                int i3;
                int i4;
                if (this.activeWindow != null) {
                    if (scriptConsoleWindow2 == this.activeWindow) {
                        scriptConsoleWindow3 = scriptConsoleWindow2;
                        drawContext2 = drawContext;
                        i3 = i;
                        i4 = i2;
                    } else {
                        scriptConsoleWindow3 = scriptConsoleWindow2;
                        drawContext2 = drawContext;
                        i3 = Integer.MIN_VALUE;
                        i4 = Integer.MIN_VALUE;
                    }
                } else if (scriptConsoleWindow2 == scriptConsoleWindow) {
                    scriptConsoleWindow3 = scriptConsoleWindow2;
                    drawContext2 = drawContext;
                    i3 = i;
                    i4 = i2;
                } else {
                    scriptConsoleWindow3 = scriptConsoleWindow2;
                    drawContext2 = drawContext;
                    i3 = Integer.MIN_VALUE;
                    i4 = Integer.MIN_VALUE;
                }

                scriptConsoleWindow3.render(drawContext2, i3, i4, f);
            }
        }
    }

    public boolean isMouseOverWindow(double d, double d2) {
        return this.activeWindow != null || this.getWindowAt(d, d2) != null;
    }

    public boolean onMouseClick(Click click, boolean flag) {
        ScriptConsoleWindow scriptConsoleWindow = this.getWindowAt(click.x(), click.y());

        for (ScriptConsoleWindow scriptConsoleWindow2 : this.windows) {
            if (scriptConsoleWindow2 != scriptConsoleWindow) {
                scriptConsoleWindow2.unfocus();
            }
        }

        if (scriptConsoleWindow == null) {
            return false;
        }

        this.bringToFront(scriptConsoleWindow);
        this.activeWindow = scriptConsoleWindow;
        scriptConsoleWindow.mouseClicked(click, flag);
        return true;
    }

    public boolean onMouseDrag(Click click, double d, double d2) {
        if (this.activeWindow == null) {
            return false;
        }

        this.activeWindow.mouseDragged(click, d, d2);
        return true;
    }

    public boolean onMouseRelease(Click click) {
        if (this.activeWindow == null) {
            return false;
        }

        this.activeWindow.onMouseRelease(click);
        this.activeWindow = null;
        return true;
    }

    public boolean onMouseScroll(double d, double d2, double d3, double d4) {
        ScriptConsoleWindow scriptConsoleWindow = this.getWindowAt(d, d2);
        return scriptConsoleWindow != null && scriptConsoleWindow.mouseScrolled(d, d2, d3, d4);
    }

    public boolean onKeyPress(KeyInput keyInput) {
        ScriptConsoleWindow scriptConsoleWindow = this.getFocusedWindow();
        return scriptConsoleWindow != null && scriptConsoleWindow.onKeyPress(keyInput);
    }

    public boolean onCharTyped(CharInput charInput) {
        ScriptConsoleWindow scriptConsoleWindow = this.getFocusedWindow();
        return scriptConsoleWindow != null && scriptConsoleWindow.onCharType(charInput);
    }

    public void onResize(int i, int i2) {
        Iterator iterator2 = this.windows.iterator();

        while (iterator2.hasNext()) {
            ((ScriptConsoleWindow)iterator2.next()).resize(i, i2);
        }
    }

    public void closeAll() {
        this.activeWindow = null;
        Iterator iterator2 = this.windows.iterator();

        while (iterator2.hasNext()) {
            ((ScriptConsoleWindow)iterator2.next()).removed();
        }
    }

    private ScriptConsoleWindow getWindowAt(double d, double d2) {
        for (int i = this.windows.size() - 1; i >= 0; i--) {
            ScriptConsoleWindow scriptConsoleWindow = this.windows.get(i);
            if (scriptConsoleWindow.isMouseOverWindow(d, d2)) {
                return scriptConsoleWindow;
            }
        }

        return null;
    }

    private ScriptConsoleWindow getFocusedWindow() {
        for (ScriptConsoleWindow scriptConsoleWindow : this.windows) {
            if (scriptConsoleWindow.hasFocus()) {
                return scriptConsoleWindow;
            }
        }

        return null;
    }

    private void bringToFront(ScriptConsoleWindow scriptConsoleWindow) {
        this.windows.remove(scriptConsoleWindow);
        this.windows.add(scriptConsoleWindow);
    }
}
