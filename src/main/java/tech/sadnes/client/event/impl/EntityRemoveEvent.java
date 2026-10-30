package tech.sadnes.client.event.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import tech.sadnes.client.event.Event;

public class EntityRemoveEvent extends Event {
    private final Entity entity;
    private final int entityId;
    private final RemovalReason reason;

    public EntityRemoveEvent(Entity entity2, int i, RemovalReason removalReason) {
        this.entity = entity2;
        this.entityId = i;
        this.reason = removalReason;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public int getEntityId() {
        return this.entityId;
    }

    public RemovalReason getReason() {
        return this.reason;
    }
}
