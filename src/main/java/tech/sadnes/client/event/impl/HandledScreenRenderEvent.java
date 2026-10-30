package tech.sadnes.client.event.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;
import tech.sadnes.client.event.Event;

public class HandledScreenRenderEvent extends Event {
    private final DrawContext context;
    private final Slot focusedSlot;
    private final int backgroundWidth;
    private final int backgroundHeight;

    public DrawContext getContext() {
        return this.context;
    }

    public Slot getFocusedSlot() {
        return this.focusedSlot;
    }

    public int getBackgroundWidth() {
        return this.backgroundWidth;
    }

    public int getBackgroundHeight() {
        return this.backgroundHeight;
    }

    public HandledScreenRenderEvent(DrawContext drawContext, Slot slot, int i, int i2) {
        this.context = drawContext;
        this.focusedSlot = slot;
        this.backgroundWidth = i;
        this.backgroundHeight = i2;
    }
}
