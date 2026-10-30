package tech.sadnes.client.event.impl;

import net.minecraft.client.gui.screen.Screen;
import tech.sadnes.client.event.CancellableEvent;

public class SetScreenEvent extends CancellableEvent {
    public Screen screen;

    public Screen getScreen() {
        return this.screen;
    }

    public void setScreen(Screen screen2) {
        this.screen = screen2;
    }

    public SetScreenEvent(Screen screen2) {
        this.screen = screen2;
    }
}
