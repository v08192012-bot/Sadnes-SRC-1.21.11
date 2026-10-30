package tech.sadnes.client.event.impl;

import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import tech.sadnes.client.event.Event;

public final class SkyColorEvent extends Event {
    private ClientWorld world;
    private Camera camera;
    private float tickDelta;
    private int color;

    public ClientWorld getWorld() {
        return this.world;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public float getTickDelta() {
        return this.tickDelta;
    }

    public int getColor() {
        return this.color;
    }

    public void setWorld(ClientWorld clientWorld) {
        this.world = clientWorld;
    }

    public void setCamera(Camera camera2) {
        this.camera = camera2;
    }

    public void setTickDelta(float f) {
        this.tickDelta = f;
    }

    public void setColor(int i) {
        this.color = i;
    }

    public SkyColorEvent(ClientWorld clientWorld, Camera camera2, float f, int i) {
        this.world = clientWorld;
        this.camera = camera2;
        this.tickDelta = f;
        this.color = i;
    }
}
