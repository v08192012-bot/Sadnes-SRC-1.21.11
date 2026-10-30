package tech.sadnes.client.event.impl;

import net.minecraft.client.gui.screen.Screen;
import tech.sadnes.client.event.CancellableEvent;

public class CloseScreenEvent extends CancellableEvent {
    private final Screen screen;

    public CloseScreenEvent(Screen screen2) {
        this.screen = screen2;
    }

    public Screen getScreen() {
        return this.screen;
    }
}
