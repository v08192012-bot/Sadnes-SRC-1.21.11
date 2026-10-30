package tech.sadnes.client.event.impl;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.util.Identifier;
import tech.sadnes.client.event.CancellableEvent;

public final class SkyRenderEvent extends CancellableEvent {
    private FrameGraphBuilder frameGraphBuilder;
    private Camera camera;
    private GpuBufferSlice fogBuffer;
    private Identifier texture;
    private int color;
    private float alpha;
    private float rotation;
    private float tickDelta;
    private int gradientMode;
    private int segments;
    private float brightness;

    public FrameGraphBuilder getFrameGraphBuilder() {
        return this.frameGraphBuilder;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public GpuBufferSlice getFogBuffer() {
        return this.fogBuffer;
    }

    public Identifier getTexture() {
        return this.texture;
    }

    public int getColor() {
        return this.color;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public float getRotation() {
        return this.rotation;
    }

    public float getTickDelta() {
        return this.tickDelta;
    }

    public int getGradientMode() {
        return this.gradientMode;
    }

    public int getSegments() {
        return this.segments;
    }

    public float getBrightness() {
        return this.brightness;
    }

    public void setFrameGraphBuilder(FrameGraphBuilder frameGraphBuilder2) {
        this.frameGraphBuilder = frameGraphBuilder2;
    }

    public void setCamera(Camera camera2) {
        this.camera = camera2;
    }

    public void setFogBuffer(GpuBufferSlice gpuBufferSlice) {
        this.fogBuffer = gpuBufferSlice;
    }

    public void setTexture(Identifier identifier) {
        this.texture = identifier;
    }

    public void setColor(int i) {
        this.color = i;
    }

    public void setAlpha(float f) {
        this.alpha = f;
    }

    public void setRotation(float f) {
        this.rotation = f;
    }

    public void setTickDelta(float f) {
        this.tickDelta = f;
    }

    public void setGradientMode(int i) {
        this.gradientMode = i;
    }

    public void setSegments(int i) {
        this.segments = i;
    }

    public void setBrightness(float f) {
        this.brightness = f;
    }

    public SkyRenderEvent(
        FrameGraphBuilder frameGraphBuilder2,
        Camera camera2,
        GpuBufferSlice gpuBufferSlice,
        Identifier identifier,
        int i,
        float f,
        float f2,
        float f3,
        int i2,
        int i3,
        float f4
    ) {
        this.frameGraphBuilder = frameGraphBuilder2;
        this.camera = camera2;
        this.fogBuffer = gpuBufferSlice;
        this.texture = identifier;
        this.color = i;
        this.alpha = f;
        this.rotation = f2;
        this.tickDelta = f3;
        this.gradientMode = i2;
        this.segments = i3;
        this.brightness = f4;
    }
}
