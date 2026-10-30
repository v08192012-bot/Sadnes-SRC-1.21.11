package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.CancellableEvent;

public final class MoveInputEvent extends CancellableEvent {
    private float forward;
    private float strafe;
    private boolean jump;
    private boolean sneak;
    private double sneakFactor;

    public boolean isMoving() {
        return this.forward != 0.0F || this.strafe != 0.0F;
    }

    public float getForward() {
        return this.forward;
    }

    public float getStrafe() {
        return this.strafe;
    }

    public boolean isJump() {
        return this.jump;
    }

    public boolean isSneak() {
        return this.sneak;
    }

    public double getSneakFactor() {
        return this.sneakFactor;
    }

    public void setForward(float f) {
        this.forward = f;
    }

    public void setStrafe(float f) {
        this.strafe = f;
    }

    public void setJump(boolean flag) {
        this.jump = flag;
    }

    public void setSneak(boolean flag) {
        this.sneak = flag;
    }

    public void setSneakFactor(double d) {
        this.sneakFactor = d;
    }

    public MoveInputEvent(float f, float f2, boolean flag, boolean flag2, double d) {
        this.forward = f;
        this.strafe = f2;
        this.jump = flag;
        this.sneak = flag2;
        this.sneakFactor = d;
    }
}
