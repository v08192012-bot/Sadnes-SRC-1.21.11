package tech.sadnes.client.event.impl;

import net.minecraft.item.ItemStack;
import tech.sadnes.client.event.CancellableEvent;

public final class StopUsingItemEvent extends CancellableEvent {
    private final ItemStack stack;

    public StopUsingItemEvent(ItemStack itemStack) {
        this.stack = itemStack;
    }

    public ItemStack getStack() {
        return this.stack;
    }
}
