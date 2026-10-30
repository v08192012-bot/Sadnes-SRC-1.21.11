package tech.sadnes.api.mixin.render;

import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import tech.sadnes.client.event.impl.FogDistanceEvent;
import tech.sadnes.client.feature.impl.render.Removals;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "getCameraSubmersionType", at = @At("RETURN"), cancellable = true)
    private void sadnes$removeLiquidFog(Camera camera, CallbackInfoReturnable<CameraSubmersionType> callbackInfoReturnable) {
        CameraSubmersionType cameraSubmersionType = (CameraSubmersionType)callbackInfoReturnable.getReturnValue();
        boolean flag;
        if (cameraSubmersionType == CameraSubmersionType.LAVA) {
            flag = Removals.isRemoved("lava");
        } else {
            flag = false;
        }

        if (cameraSubmersionType == CameraSubmersionType.WATER && Removals.isRemoved("water")) {
            callbackInfoReturnable.setReturnValue(CameraSubmersionType.ATMOSPHERIC);
        } else if (flag) {
            callbackInfoReturnable.setReturnValue(CameraSubmersionType.ATMOSPHERIC);
        }
    }

    @ModifyArgs(
        method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V")
    )
    private void sadnes$ambienceFogDistanceArgs(Args args, Camera camera, int i, RenderTickCounter renderTickCounter, float f, ClientWorld clientWorld) {
        if (Removals.isRemoved("fog")) {
            for (int i2 = 3; i2 <= 8; i2++) {
                args.set(i2, Float.MAX_VALUE);
            }
        }

        FogDistanceEvent fogDistanceEvent = new FogDistanceEvent(
            clientWorld, camera, args.<Float>get(3), args.<Float>get(4), args.<Float>get(5), args.<Float>get(6), args.<Float>get(7), args.<Float>get(8)
        );
        fogDistanceEvent.post();
        args.set(3, fogDistanceEvent.getEnvironmentalStart());
        args.set(4, fogDistanceEvent.getRenderDistanceStart());
        args.set(5, fogDistanceEvent.getEnvironmentalEnd());
        args.set(6, fogDistanceEvent.getRenderDistanceEnd());
        args.set(7, fogDistanceEvent.getSkyEnd());
        args.set(8, fogDistanceEvent.getCloudEnd());
    }
}
