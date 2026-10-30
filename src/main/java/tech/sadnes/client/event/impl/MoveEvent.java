package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.CancellableEvent;

public final class MoveEvent extends CancellableEvent {
    private double x;
    private double y;
    private double z;
    private boolean onGround;

    public MoveEvent(double d, double d2, double d3, boolean flag) {
        this.x = d;
        this.y = d2;
        this.z = d3;
        this.onGround = flag;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public void setX(double d) {
        this.x = d;
    }

    public void setY(double d) {
        this.y = d;
    }

    public void setZ(double d) {
        this.z = d;
    }

    public void setOnGround(boolean flag) {
        this.onGround = flag;
    }
}
