package tech.sadnes.client.event.impl;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import tech.sadnes.client.event.Event;

public final class Render3DEvent extends Event {
    private final WorldRenderContext worldContext;
    private final MatrixStack matrices;
    private final Camera camera;
    private final float tickDelta;

    public WorldRenderContext getWorldContext() {
        return this.worldContext;
    }

    public MatrixStack getMatrices() {
        return this.matrices;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public float getTickDelta() {
        return this.tickDelta;
    }

    public Render3DEvent(WorldRenderContext worldRenderContext, MatrixStack matrixStack, Camera camera2, float f) {
        this.worldContext = worldRenderContext;
        this.matrices = matrixStack;
        this.camera = camera2;
        this.tickDelta = f;
    }
}
