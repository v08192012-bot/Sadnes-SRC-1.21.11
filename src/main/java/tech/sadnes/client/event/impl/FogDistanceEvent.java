package tech.sadnes.client.event.impl;

import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import tech.sadnes.client.event.Event;

public final class FogDistanceEvent extends Event {
    private ClientWorld world;
    private Camera camera;
    private float environmentalStart;
    private float renderDistanceStart;
    private float environmentalEnd;
    private float renderDistanceEnd;
    private float skyEnd;
    private float cloudEnd;

    public ClientWorld getWorld() {
        return this.world;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public float getEnvironmentalStart() {
        return this.environmentalStart;
    }

    public float getRenderDistanceStart() {
        return this.renderDistanceStart;
    }

    public float getEnvironmentalEnd() {
        return this.environmentalEnd;
    }

    public float getRenderDistanceEnd() {
        return this.renderDistanceEnd;
    }

    public float getSkyEnd() {
        return this.skyEnd;
    }

    public float getCloudEnd() {
        return this.cloudEnd;
    }

    public void setWorld(ClientWorld clientWorld) {
        this.world = clientWorld;
    }

    public void setCamera(Camera camera2) {
        this.camera = camera2;
    }

    public void setEnvironmentalStart(float f) {
        this.environmentalStart = f;
    }

    public void setRenderDistanceStart(float f) {
        this.renderDistanceStart = f;
    }

    public void setEnvironmentalEnd(float f) {
        this.environmentalEnd = f;
    }

    public void setRenderDistanceEnd(float f) {
        this.renderDistanceEnd = f;
    }

    public void setSkyEnd(float f) {
        this.skyEnd = f;
    }

    public void setCloudEnd(float f) {
        this.cloudEnd = f;
    }

    public FogDistanceEvent(ClientWorld clientWorld, Camera camera2, float f, float f2, float f3, float f4, float f5, float f6) {
        this.world = clientWorld;
        this.camera = camera2;
        this.environmentalStart = f;
        this.renderDistanceStart = f2;
        this.environmentalEnd = f3;
        this.renderDistanceEnd = f4;
        this.skyEnd = f5;
        this.cloudEnd = f6;
    }
}
