package tech.sadnes.client.event.impl;

import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import tech.sadnes.client.event.Event;

public final class FogColorEvent extends Event {
    private ClientWorld world;
    private Camera camera;
    private int viewDistance;
    private float skyDarkness;
    private int color;

    public ClientWorld getWorld() {
        return this.world;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public float getSkyDarkness() {
        return this.skyDarkness;
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

    public void setViewDistance(int i) {
        this.viewDistance = i;
    }

    public void setSkyDarkness(float f) {
        this.skyDarkness = f;
    }

    public void setColor(int i) {
        this.color = i;
    }

    public FogColorEvent(ClientWorld clientWorld, Camera camera2, int i, float f, int i2) {
        this.world = clientWorld;
        this.camera = camera2;
        this.viewDistance = i;
        this.skyDarkness = f;
        this.color = i2;
    }
}
