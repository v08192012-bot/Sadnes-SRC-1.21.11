package tech.sadnes.client.event.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import tech.sadnes.client.event.Event;

public final class Render2DEvent extends Event {
    private final DrawContext context;
    private final RenderTickCounter tickCounter;
    private final float tickDelta;
    private final double mouseX;
    private final double mouseY;

    public DrawContext getContext() {
        return this.context;
    }

    public RenderTickCounter getTickCounter() {
        return this.tickCounter;
    }

    public float getTickDelta() {
        return this.tickDelta;
    }

    public double getMouseX() {
        return this.mouseX;
    }

    public double getMouseY() {
        return this.mouseY;
    }

    public Render2DEvent(DrawContext drawContext, RenderTickCounter renderTickCounter, float f, double d, double d2) {
        this.context = drawContext;
        this.tickCounter = renderTickCounter;
        this.tickDelta = f;
        this.mouseX = d;
        this.mouseY = d2;
    }
}
