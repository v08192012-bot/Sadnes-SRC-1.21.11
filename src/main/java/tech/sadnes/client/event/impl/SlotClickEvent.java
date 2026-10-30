package tech.sadnes.client.event.impl;

import net.minecraft.screen.slot.SlotActionType;
import tech.sadnes.client.event.CancellableEvent;

public class SlotClickEvent extends CancellableEvent {
    private int syncId;
    private int slotId;
    private int button;
    private SlotActionType actionType;

    public int getSyncId() {
        return this.syncId;
    }

    public int getSlotId() {
        return this.slotId;
    }

    public int getButton() {
        return this.button;
    }

    public SlotActionType getActionType() {
        return this.actionType;
    }

    public void setSyncId(int i) {
        this.syncId = i;
    }

    public void setSlotId(int i) {
        this.slotId = i;
    }

    public void setButton(int i) {
        this.button = i;
    }

    public void setActionType(SlotActionType slotActionType) {
        this.actionType = slotActionType;
    }

    public SlotClickEvent(int i, int i2, int i3, SlotActionType slotActionType) {
        this.syncId = i;
        this.slotId = i2;
        this.button = i3;
        this.actionType = slotActionType;
    }
}
