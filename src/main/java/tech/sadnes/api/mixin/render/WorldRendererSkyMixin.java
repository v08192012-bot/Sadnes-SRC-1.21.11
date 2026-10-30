package tech.sadnes.api.mixin.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.FramePass;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.event.impl.SkyRenderEvent;
import tech.sadnes.client.render.CustomSkyRenderer;
import tech.sadnes.client.render.world.OptifineSkyManager;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererSkyMixin {
    @Shadow
    @Final
    private DefaultFramebufferSet framebufferSet;

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void sadnes$customSky(FrameGraphBuilder frameGraphBuilder, Camera camera, GpuBufferSlice gpuBufferSlice, CallbackInfo callbackInfo) {
        float f = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
        SkyRenderEvent skyRenderEvent = new SkyRenderEvent(frameGraphBuilder, camera, gpuBufferSlice, null, -1, 1.0F, 0.0F, f, 0, 2, 1.0F);
        skyRenderEvent.post();
        if (skyRenderEvent.isCancelled()) {
            boolean flag;
            if (skyRenderEvent.getGradientMode() > 0) {
                flag = true;
            } else {
                flag = false;
                if (skyRenderEvent.getTexture() == null) {
                    return;
                }
            }

            if (flag) {
                CustomSkyRenderer.queueShaderSky(skyRenderEvent.getGradientMode(), skyRenderEvent.getColor(), skyRenderEvent.getAlpha(), skyRenderEvent.getBrightness(), skyRenderEvent.getSegments());
            }

            if (skyRenderEvent.getTexture() != null) {
                FramePass framePass = frameGraphBuilder.createPass(ClientInfo.MOD_ID + "-custom-sky");
                this.framebufferSet.mainFramebuffer = framePass.transfer(this.framebufferSet.mainFramebuffer);
                framePass.setRenderer(() -> {
                    RenderSystem.setShaderFog(gpuBufferSlice);
                    CustomSkyRenderer.renderTextured(skyRenderEvent.getTexture(), skyRenderEvent.getColor(), skyRenderEvent.getAlpha(), skyRenderEvent.getRotation());
                });
            }

            callbackInfo.cancel();
        }
    }

    @Inject(method = "pushEntityRenders", at = @At("HEAD"))
    private void sadnes$deferredSky(CallbackInfo callbackInfo) {
        CustomSkyRenderer.flushShaderSky();
    }

    @Inject(method = "renderSky", at = @At("RETURN"))
    private void sadnes$optifineSky(FrameGraphBuilder frameGraphBuilder, Camera camera, GpuBufferSlice gpuBufferSlice, CallbackInfo callbackInfo) {
        if (OptifineSkyManager.shouldRender(camera)) {
            FramePass framePass = frameGraphBuilder.createPass(ClientInfo.MOD_ID + "-optifine-sky");
            this.framebufferSet.mainFramebuffer = framePass.transfer(this.framebufferSet.mainFramebuffer);
            framePass.setRenderer(() -> {
                RenderSystem.setShaderFog(gpuBufferSlice);
                OptifineSkyManager.render();
            });
        }
    }
}
