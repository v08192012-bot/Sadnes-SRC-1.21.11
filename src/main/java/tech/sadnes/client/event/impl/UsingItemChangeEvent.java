package tech.sadnes.client.event.impl;

import net.minecraft.entity.LivingEntity;
import tech.sadnes.client.event.Event;

public class UsingItemChangeEvent extends Event {
    private final LivingEntity entity;
    private final boolean usingItem;

    public UsingItemChangeEvent(LivingEntity livingEntity, boolean flag) {
        this.entity = livingEntity;
        this.usingItem = flag;
    }

    public LivingEntity getEntity() {
        return this.entity;
    }

    public boolean isUsingItem() {
        return this.usingItem;
    }
}
