package tech.sadnes.client.event.impl;

import net.minecraft.entity.Entity;
import tech.sadnes.client.event.Event;

public class ItemPickupEvent extends Event {
    private final Entity collector;
    private final Entity item;
    private final int amount;

    public Entity getCollector() {
        return this.collector;
    }

    public Entity getItem() {
        return this.item;
    }

    public int getAmount() {
        return this.amount;
    }

    public ItemPickupEvent(Entity entity, Entity entity2, int i) {
        this.collector = entity;
        this.item = entity2;
        this.amount = i;
    }
}
