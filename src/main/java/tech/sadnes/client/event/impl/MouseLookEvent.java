package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.CancellableEvent;

public final class MouseLookEvent extends CancellableEvent {
    private double deltaX;
    private double deltaY;

    public double getDeltaX() {
        return this.deltaX;
    }

    public void setDeltaX(double d) {
        this.deltaX = d;
    }

    public double getDeltaY() {
        return this.deltaY;
    }

    public void setDeltaY(double d) {
        this.deltaY = d;
    }

    public MouseLookEvent(double d, double d2) {
        this.deltaX = d;
        this.deltaY = d2;
    }
}
