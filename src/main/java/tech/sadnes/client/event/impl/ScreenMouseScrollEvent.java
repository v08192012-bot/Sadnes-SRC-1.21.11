package tech.sadnes.client.event.impl;

import net.minecraft.client.gui.screen.Screen;
import tech.sadnes.client.event.CancellableEvent;

public final class ScreenMouseScrollEvent extends CancellableEvent {
    private final double mouseX;
    private final double mouseY;
    private final double horizontalAmount;
    private final double verticalAmount;
    private final Screen screen;

    public double getMouseX() {
        return this.mouseX;
    }

    public double getMouseY() {
        return this.mouseY;
    }

    public double getHorizontalAmount() {
        return this.horizontalAmount;
    }

    public double getVerticalAmount() {
        return this.verticalAmount;
    }

    public Screen getScreen() {
        return this.screen;
    }

    public ScreenMouseScrollEvent(double d, double d2, double d3, double d4, Screen screen2) {
        this.mouseX = d;
        this.mouseY = d2;
        this.horizontalAmount = d3;
        this.verticalAmount = d4;
        this.screen = screen2;
    }
}
