package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.CancellableEvent;

public final class CloudRenderEvent extends CancellableEvent {
    private int color;
    private float height;
    private float speed;

    public int getColor() {
        return this.color;
    }

    public float getHeight() {
        return this.height;
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setColor(int i) {
        this.color = i;
    }

    public void setHeight(float f) {
        this.height = f;
    }

    public void setSpeed(float f) {
        this.speed = f;
    }

    public CloudRenderEvent(int i, float f, float f2) {
        this.color = i;
        this.height = f;
        this.speed = f2;
    }
}
