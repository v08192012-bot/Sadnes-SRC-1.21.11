package tech.sadnes.client.event.impl;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import tech.sadnes.client.event.Event;

public final class RenderEvent extends Event {
    private final RenderEvent.Type type;
    private final WorldRenderContext worldContext;
    private final MatrixStack matrices;
    private final Camera camera;
    private final DrawContext context;
    private final RenderTickCounter tickCounter;
    private final double mouseX;
    private final double mouseY;
    private final float tickDelta;

    public static RenderEvent ofWorld(WorldRenderContext worldRenderContext, MatrixStack matrixStack, Camera camera2, float f) {
        return new RenderEvent(RenderEvent.Type.THREE_D, worldRenderContext, matrixStack, camera2, null, null, 0.0, 0.0, f);
    }

    public static RenderEvent ofHudPost(DrawContext drawContext, RenderTickCounter renderTickCounter, float f, double d, double d2) {
        return new RenderEvent(RenderEvent.Type.TWO_D_POST, null, null, null, drawContext, renderTickCounter, d, d2, f);
    }

    public static RenderEvent ofHudPre(DrawContext drawContext, RenderTickCounter renderTickCounter, float f, double d, double d2) {
        return new RenderEvent(RenderEvent.Type.TWO_D_PRE, null, null, null, drawContext, renderTickCounter, d, d2, f);
    }

    public boolean isWorld() {
        return this.type == RenderEvent.Type.THREE_D;
    }

    public boolean isHudPost() {
        return this.type == RenderEvent.Type.TWO_D_POST;
    }

    public boolean isHudPre() {
        return this.type == RenderEvent.Type.TWO_D_PRE;
    }

    public RenderEvent.Type getType() {
        return this.type;
    }

    public WorldRenderContext getWorldContext() {
        return this.worldContext;
    }

    public MatrixStack getMatrices() {
        return this.matrices;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public DrawContext getContext() {
        return this.context;
    }

    public RenderTickCounter getTickCounter() {
        return this.tickCounter;
    }

    public double getMouseX() {
        return this.mouseX;
    }

    public double getMouseY() {
        return this.mouseY;
    }

    public float getTickDelta() {
        return this.tickDelta;
    }

    private RenderEvent(
        RenderEvent.Type type2,
        WorldRenderContext worldRenderContext,
        MatrixStack matrixStack,
        Camera camera2,
        DrawContext drawContext,
        RenderTickCounter renderTickCounter,
        double d,
        double d2,
        float f
    ) {
        this.type = type2;
        this.worldContext = worldRenderContext;
        this.matrices = matrixStack;
        this.camera = camera2;
        this.context = drawContext;
        this.tickCounter = renderTickCounter;
        this.mouseX = d;
        this.mouseY = d2;
        this.tickDelta = f;
    }

    public enum Type {
        THREE_D,
        TWO_D_PRE,
        TWO_D_POST;
    }
}
