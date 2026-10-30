package tech.sadnes.api.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tech.sadnes.client.event.impl.CloudRenderEvent;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererCloudMixin {
    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getCloudRenderModeValue()Lnet/minecraft/client/option/CloudRenderMode;")
    )
    private CloudRenderMode sadnes$enableMinecraftClouds(GameOptions gameOptions, Operation<CloudRenderMode> operation) {
        CloudRenderMode cloudRenderMode = (CloudRenderMode)operation.call(gameOptions);
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.isCloudsEnabled() && cloudRenderMode == CloudRenderMode.OFF) {
            gameOptions.getCloudRenderMode().setValue(CloudRenderMode.FANCY);
            return CloudRenderMode.FANCY;
        } else {
            return cloudRenderMode;
        }
    }

    @WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderClouds(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/option/CloudRenderMode;Lnet/minecraft/util/math/Vec3d;JFIF)V"
        )
    )
    private void sadnes$ambienceClouds(
        WorldRenderer worldRenderer, FrameGraphBuilder frameGraphBuilder, CloudRenderMode cloudRenderMode, Vec3d vec3d, long l, float f, int i, float f2, Operation<Void> operation
    ) {
        CloudRenderEvent cloudRenderEvent = new CloudRenderEvent(i, f2, 1.0F);
        cloudRenderEvent.post();
        if (!cloudRenderEvent.isCancelled()) {
            double d = ((double)l + f) * cloudRenderEvent.getSpeed();
            long l2 = (long)Math.floor(d);
            operation.call(worldRenderer, frameGraphBuilder, cloudRenderMode, vec3d, l2, (float)(d - l2), cloudRenderEvent.getColor(), cloudRenderEvent.getHeight());
        }
    }
}
