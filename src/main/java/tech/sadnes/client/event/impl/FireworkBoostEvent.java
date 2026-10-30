package tech.sadnes.client.event.impl;

import net.minecraft.entity.LivingEntity;
import tech.sadnes.client.event.Event;

public final class FireworkBoostEvent extends Event {
    private final LivingEntity entity;
    private double horizontalSpeed;
    private double verticalSpeed;
    private boolean modified;

    public FireworkBoostEvent(LivingEntity livingEntity, double d, double d2) {
        this.entity = livingEntity;
        this.horizontalSpeed = d;
        this.verticalSpeed = d2;
    }

    public void setSpeed(double d, double d2) {
        this.horizontalSpeed = d;
        this.verticalSpeed = d2;
        this.modified = true;
    }

    public LivingEntity getEntity() {
        return this.entity;
    }

    public double getHorizontalSpeed() {
        return this.horizontalSpeed;
    }

    public double getVerticalSpeed() {
        return this.verticalSpeed;
    }

    public boolean isModified() {
        return this.modified;
    }
}
