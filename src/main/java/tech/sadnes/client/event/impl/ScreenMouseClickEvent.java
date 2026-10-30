package tech.sadnes.client.event.impl;

import net.minecraft.client.gui.screen.Screen;
import tech.sadnes.client.event.Event;

public final class ScreenMouseClickEvent extends Event {
    private final int button;
    private final int mouseX;
    private final int mouseY;
    private final int action;
    private final Screen screen;

    public boolean isButton(int i) {
        return this.button == i;
    }

    public boolean isPress() {
        return this.action == 1;
    }

    public boolean isRelease() {
        return this.action == 0;
    }

    public int getButton() {
        return this.button;
    }

    public int getMouseX() {
        return this.mouseX;
    }

    public int getMouseY() {
        return this.mouseY;
    }

    public int getAction() {
        return this.action;
    }

    public Screen getScreen() {
        return this.screen;
    }

    public ScreenMouseClickEvent(int i, int i2, int i3, int i4, Screen screen2) {
        this.button = i;
        this.mouseX = i2;
        this.mouseY = i3;
        this.action = i4;
        this.screen = screen2;
    }
}
