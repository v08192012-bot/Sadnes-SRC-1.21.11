package tech.sadnes.api.mixin.entity;

import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.event.impl.EntityRaycastEvent;

@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {
    @Inject(method = "raycast", at = @At("RETURN"), cancellable = true)
    private static void sadnes$entityTraceRaycast(CallbackInfoReturnable<EntityHitResult> callbackInfoReturnable) {
        EntityHitResult entityHitResult = (EntityHitResult)callbackInfoReturnable.getReturnValue();
        if (entityHitResult != null && entityHitResult.getEntity() != null) {
            EntityRaycastEvent entityRaycastEvent = new EntityRaycastEvent(entityHitResult.getEntity());
            entityRaycastEvent.post();
            if (entityRaycastEvent.isCancelled()) {
                callbackInfoReturnable.setReturnValue(null);
            }
        }
    }

    @Inject(method = "getEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Lnet/minecraft/util/hit/EntityHitResult;", at = @At("RETURN"), cancellable = true)
    private static void sadnes$entityTraceGetCollision(CallbackInfoReturnable<EntityHitResult> callbackInfoReturnable) {
        EntityHitResult entityHitResult = (EntityHitResult)callbackInfoReturnable.getReturnValue();
        if (entityHitResult != null && entityHitResult.getEntity() != null) {
            EntityRaycastEvent entityRaycastEvent = new EntityRaycastEvent(entityHitResult.getEntity());
            entityRaycastEvent.post();
            if (entityRaycastEvent.isCancelled()) {
                callbackInfoReturnable.setReturnValue(null);
            }
        }
    }
}
