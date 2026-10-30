package tech.sadnes.api.mixin.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;
import tech.sadnes.client.render.texture.WhiteLightmap;

@Mixin(RenderSetup.class)
public abstract class RenderSetupMixin {
    @Shadow
    @Final
    RenderPipeline pipeline;

    @Redirect(
        method = "resolveTextures",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/LightmapTextureManager;getGlTextureView()Lcom/mojang/blaze3d/textures/GpuTextureView;"
        )
    )
    private GpuTextureView sadnes$lightmapForDraw(LightmapTextureManager lightmapTextureManager) {
        return (!Ambience.isWorldPhase() || this.sadnes$isTextLayer()) && Ambience.isLightmapOverridden()
            ? WhiteLightmap.getTextureView()
            : lightmapTextureManager.getGlTextureView();
    }

    private boolean sadnes$isTextLayer() {
        String string = this.pipeline.getLocation().getPath();
        int i = string.indexOf("text");
        return i >= 0 && (i + 4 == string.length() || string.charAt(i + 4) == '_');
    }
}
