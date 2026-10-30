package tech.sadnes.client.event.impl;

import net.minecraft.entity.Entity;
import tech.sadnes.client.event.Event;

public final class TargetingMarginEvent extends Event {
    private final Entity entity;
    private float margin;

    public TargetingMarginEvent(Entity entity2, float f) {
        this.entity = entity2;
        this.margin = f;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public float getMargin() {
        return this.margin;
    }

    public void setMargin(float f) {
        this.margin = f;
    }
}
