package tech.sadnes.api.mixin.render;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.tracy.TracyFrameCapturer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.render.shader.AmbiencePostUniformStorage;
import tech.sadnes.client.render.shader.BlurRectUniformStorage;
import tech.sadnes.client.render.shader.ChamsUniformStorage;
import tech.sadnes.client.render.shader.FlameUniformStorage;
import tech.sadnes.client.render.shader.FogUniformStorage;
import tech.sadnes.client.render.shader.FrameBufferPool;
import tech.sadnes.client.render.shader.GridBgUniformStorage;
import tech.sadnes.client.render.shader.MotionBlurUniformStorage;
import tech.sadnes.client.render.shader.RoundedUniformStorage;
import tech.sadnes.client.render.shader.ScriptFxUniformStorage;
import tech.sadnes.client.render.shader.SkyUniformStorage;
import tech.sadnes.client.render.shader.WeatherUniformStorage;
import tech.sadnes.client.render.world.FoliageSwayManager;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
    @Inject(method = "flipFrame", at = @At("HEAD"))
    private static void sadnes$clearCustomUbo(Window window, TracyFrameCapturer tracyFrameCapturer, CallbackInfo callbackInfo) {
        RoundedUniformStorage.clear();
        ChamsUniformStorage.clear();
        FlameUniformStorage.clear();
        ScriptFxUniformStorage.clear();
        MotionBlurUniformStorage.clear();
        AmbiencePostUniformStorage.clear();
        WeatherUniformStorage.clear();
        FogUniformStorage.clear();
        BlurRectUniformStorage.clear();
        GridBgUniformStorage.clear();
        SkyUniformStorage.clear();
        FrameBufferPool.endFrame();
    }

    @Inject(method = "bindDefaultUniforms", at = @At("TAIL"))
    private static void sadnes$foliageUniform(RenderPass renderPass, CallbackInfo callbackInfo) {
        FoliageSwayManager.INSTANCE.bindUniform(renderPass);
    }
}
