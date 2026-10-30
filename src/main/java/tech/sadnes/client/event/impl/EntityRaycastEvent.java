package tech.sadnes.client.event.impl;

import net.minecraft.entity.Entity;
import tech.sadnes.client.event.CancellableEvent;

public final class EntityRaycastEvent extends CancellableEvent {
    private final Entity entity;

    public Entity getEntity() {
        return this.entity;
    }

    public EntityRaycastEvent(Entity entity2) {
        this.entity = entity2;
    }
}
