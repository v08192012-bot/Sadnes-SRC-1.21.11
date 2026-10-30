package tech.sadnes.client.event.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;
import tech.sadnes.client.event.CancellableEvent;

public class SlotTooltipEvent extends CancellableEvent {
    private final DrawContext context;
    private final Slot focusedSlot;
    private final int mouseX;
    private final int mouseY;

    public DrawContext getContext() {
        return this.context;
    }

    public Slot getFocusedSlot() {
        return this.focusedSlot;
    }

    public int getMouseX() {
        return this.mouseX;
    }

    public int getMouseY() {
        return this.mouseY;
    }

    public SlotTooltipEvent(DrawContext drawContext, Slot slot, int i, int i2) {
        this.context = drawContext;
        this.focusedSlot = slot;
        this.mouseX = i;
        this.mouseY = i2;
    }
}
