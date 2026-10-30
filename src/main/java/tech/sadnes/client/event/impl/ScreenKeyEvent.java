package tech.sadnes.client.event.impl;

import net.minecraft.client.gui.screen.Screen;
import tech.sadnes.client.event.Event;

public final class ScreenKeyEvent extends Event {
    private final int key;
    private final int action;
    private final int modifiers;
    private final Screen screen;
    private final double mouseX;
    private final double mouseY;

    public boolean isKey(int i) {
        return this.key == i;
    }

    public boolean isPress() {
        return this.action == 1;
    }

    public boolean isRelease() {
        return this.action == 0;
    }

    public int getKey() {
        return this.key;
    }

    public int getAction() {
        return this.action;
    }

    public int getModifiers() {
        return this.modifiers;
    }

    public Screen getScreen() {
        return this.screen;
    }

    public double getMouseX() {
        return this.mouseX;
    }

    public double getMouseY() {
        return this.mouseY;
    }

    public ScreenKeyEvent(int i, int i2, int i3, Screen screen2, double d, double d2) {
        this.key = i;
        this.action = i2;
        this.modifiers = i3;
        this.screen = screen2;
        this.mouseX = d;
        this.mouseY = d2;
    }
}
