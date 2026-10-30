package tech.sadnes.client.render;

import java.util.ArrayDeque;
import java.util.Deque;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.EndMain;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.impl.HudPostRenderEvent;
import tech.sadnes.client.event.impl.MenuTickEvent;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.Render3DEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.render.shader.ShapeBlur;
import tech.sadnes.client.util.math.FixedRateLimiter;
import tech.sadnes.client.util.player.MovementSimulator;

public final class RenderHooks {
    private static boolean initialized;
    private static boolean screenRendering;
    private static boolean overlayRendering;
    private static DrawContext screenContext;
    private static final Deque<RenderHooks.DeferredTask> deferredTasks = new ArrayDeque<>();
    private static final FixedRateLimiter tickThrottle = new FixedRateLimiter();

    private RenderHooks() {
    }

    public static void init() {
        if (!initialized) {
            initialized = true;
            Event event = WorldRenderEvents.END_MAIN;
            EndMain endMain = RenderHooks::onWorldRenderEnd;
            event.register(endMain);
        }
    }

    public static void onWorldRenderEnd(WorldRenderContext worldRenderContext) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (initialized && minecraftClient != null && minecraftClient.player != null && minecraftClient.world != null && worldRenderContext != null) {
            Camera camera = minecraftClient.gameRenderer.getCamera();
            new Render3DEvent(worldRenderContext, worldRenderContext.matrices(), camera, camera.getLastTickProgress()).post();
            RenderEvent.ofWorld(worldRenderContext, worldRenderContext.matrices(), camera, camera.getLastTickProgress()).post();
        }
    }

    public static void onClientTick(MinecraftClient minecraftClient) {
        if (initialized && minecraftClient != null) {
            if (minecraftClient.player != null && minecraftClient.world != null) {
                MovementSimulator.incrementTicks();
                new WorldTickEvent().post();
            } else {
                new MenuTickEvent().post();
            }
        }
    }

    public static void beginScreenRender(DrawContext drawContext) {
        if (initialized) {
            Sadnes.getInstance().saveAll();
            ScissorStack.clear(null);
            deferredTasks.clear();
            screenContext = drawContext;
            screenRendering = true;
            GuiScale.push(drawContext);
        }
    }

    public static void endScreenRender() {
        if (screenContext != null) {
            GuiScale.pop(screenContext);
        }

        ScissorStack.clear(null);
        screenRendering = false;
    }

    public static boolean isRendering() {
        return screenRendering || overlayRendering;
    }

    public static void defer(Runnable runnable) {
        defer(runnable, false);
    }

    public static void defer(Runnable runnable, boolean flag) {
        if (initialized && runnable != null) {
            RenderHooks.DeferredTask deferredTask = new RenderHooks.DeferredTask(runnable, ScissorStack.current(), flag);
            if (screenRendering) {
                deferredTasks.addLast(deferredTask);
            }
        }
    }

    public static void clearDeferred() {
        deferredTasks.clear();
    }

    public static void reset() {
        ScissorStack.clear(null);
        deferredTasks.clear();
        screenContext = null;
        screenRendering = false;
        overlayRendering = false;
        GuiScale.reset();
    }

    public static void renderPreOverlay(DrawContext drawContext, RenderTickCounter renderTickCounter) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (initialized && minecraftClient != null && minecraftClient.player != null && minecraftClient.world != null && drawContext != null && renderTickCounter != null) {
            Window window = minecraftClient.getWindow();
            double d = window.getWidth() == 0 ? 0.0 : minecraftClient.mouse.getX() * window.getScaledWidth() / window.getWidth();
            double d2 = window.getHeight() == 0 ? 0.0 : minecraftClient.mouse.getY() * window.getScaledHeight() / window.getHeight();
            double d3 = GuiScale.toScaled(d);
            double d4 = GuiScale.toScaled(d2);
            float f = renderTickCounter.getTickProgress(false);
            ScissorStack.clear(null);
            overlayRendering = true;
            GuiScale.push(drawContext);

            try {
                Sadnes.getInstance().saveAll();
                RenderEvent.ofHudPre(drawContext, renderTickCounter, f, d3, d4).post();
            } finally {
                GuiScale.pop(drawContext);
                ScissorStack.clear(null);
                overlayRendering = false;
            }
        }
    }

    public static void renderOverlay(DrawContext drawContext, RenderTickCounter renderTickCounter) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (initialized && minecraftClient != null && minecraftClient.player != null && minecraftClient.world != null && drawContext != null && renderTickCounter != null) {
            Window window = minecraftClient.getWindow();
            double d = window.getWidth() == 0 ? 0.0 : minecraftClient.mouse.getX() * window.getScaledWidth() / window.getWidth();
            double d2 = window.getHeight() == 0 ? 0.0 : minecraftClient.mouse.getY() * window.getScaledHeight() / window.getHeight();
            double d3 = GuiScale.toScaled(d);
            double d4 = GuiScale.toScaled(d2);
            float f = renderTickCounter.getTickProgress(false);
            tickThrottle.run(60, () -> new HudPostRenderEvent().post());
            ScissorStack.clear(null);
            overlayRendering = true;
            GuiScale.push(drawContext);

            try {
                Sadnes.getInstance().saveAll();
                RenderEvent.ofHudPost(drawContext, renderTickCounter, f, d3, d4).post();
                new Render2DEvent(drawContext, renderTickCounter, f, d3, d4).post();
            } finally {
                GuiScale.pop(drawContext);
                ScissorStack.clear(null);
                overlayRendering = false;
            }
        }
    }

    public static void flushFrame() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (initialized && minecraftClient != null && minecraftClient.player != null && minecraftClient.world != null && !minecraftClient.skipGameRender) {
            Sadnes.getInstance().saveAll();
            ShapeBlur.INSTANCE.update();
            ShapeBlur.INSTANCE.beginFrame();
        } else {
            ShapeBlur.INSTANCE.beginFrame();
        }
    }

    record DeferredTask(Runnable runnable, ScissorStack.Bounds scissorSnapshot, boolean shapeBlurTask) {
    }
}
