package tech.sadnes.api.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlimeBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import tech.sadnes.api.mixin.accessor.LivingEntityAccessor;
import tech.sadnes.client.event.impl.SlimeBounceEvent;
import tech.sadnes.client.util.compat.ProtocolUtil;

@Mixin(ClientPlayerEntity.class)
public abstract class ViaClientPlayerMovementMixin {
    @WrapOperation(
        method = "move",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
        )
    )
    private void sadnes$apply26_2Restitution(ClientPlayerEntity clientPlayerEntity, MovementType movementType, Vec3d vec3d, Operation<Void> operation) {
        ClientPlayerEntity clientPlayerEntity2 = (ClientPlayerEntity)(Object)this;
        if (ProtocolUtil.isOlderThan("26.2")) {
            operation.call(clientPlayerEntity, movementType, vec3d);
        } else {
            Vec3d vec3d2 = clientPlayerEntity2.getVelocity();
            double d = ((LivingEntityAccessor)clientPlayerEntity2).sadnes$invokeGetEffectiveGravity();
            operation.call(clientPlayerEntity, movementType, vec3d);
            if (clientPlayerEntity2.verticalCollision && clientPlayerEntity2.isOnGround() && vec3d.y != 0.0 && !(vec3d2.y >= 0.0)) {
                BlockState blockState = clientPlayerEntity2.getEntityWorld().getBlockState(clientPlayerEntity2.getLandingPos());
                Block block = blockState.getBlock();
                double d2 = block instanceof SlimeBlock ? 1.0 : (block instanceof BedBlock ? 0.75 : 0.0);
                if (d2 != 0.0) {
                    boolean flag;
                    if (block instanceof SlimeBlock) {
                        flag = sadnes$slimeBounceCancelled();
                    } else {
                        flag = false;
                    }

                    if (!clientPlayerEntity2.bypassesLandingEffects() && !flag && !(-vec3d2.y < d)) {
                        double d3 = vec3d.y / vec3d2.y;
                        double d4 = d3 * d;
                        double d5 = MathHelper.lerp(d3, 1.0, 0.98);
                        double d6 = (d4 - vec3d2.y) * d5 * d2;
                        Vec3d vec3d3 = clientPlayerEntity2.getVelocity();
                        clientPlayerEntity2.setVelocity(vec3d3.x, d6, vec3d3.z);
                    } else {
                        Vec3d vec3d4 = clientPlayerEntity2.getVelocity();
                        clientPlayerEntity2.setVelocity(vec3d4.x, 0.0, vec3d4.z);
                    }
                }
            }
        }
    }

    @Unique
    private static boolean sadnes$slimeBounceCancelled() {
        SlimeBounceEvent slimeBounceEvent = new SlimeBounceEvent();
        slimeBounceEvent.post();
        return slimeBounceEvent.isCancelled();
    }
}
