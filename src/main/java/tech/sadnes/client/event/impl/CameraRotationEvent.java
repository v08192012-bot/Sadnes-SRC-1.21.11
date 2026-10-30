package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.Event;

public final class CameraRotationEvent extends Event {
    private float yaw;
    private float pitch;
    private final float tickProgress;

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public float getTickProgress() {
        return this.tickProgress;
    }

    public void setYaw(float f) {
        this.yaw = f;
    }

    public void setPitch(float f) {
        this.pitch = f;
    }

    public CameraRotationEvent(float f, float f2, float f3) {
        this.yaw = f;
        this.pitch = f2;
        this.tickProgress = f3;
    }
}
