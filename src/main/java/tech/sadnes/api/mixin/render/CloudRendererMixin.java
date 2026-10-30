package tech.sadnes.api.mixin.render;

import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;
import tech.sadnes.client.render.shader.AmbienceRenderer;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void sadnes$volumetricClouds(int i, CloudRenderMode cloudRenderMode, float f, Vec3d vec3d, long l, float f2, CallbackInfo callbackInfo) {
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.isVolumetricClouds()) {
            callbackInfo.cancel();
            AmbienceRenderer.INSTANCE.renderClouds(ambience.getActiveProfile(), i, f, vec3d, l, f2);
        }
    }
}
