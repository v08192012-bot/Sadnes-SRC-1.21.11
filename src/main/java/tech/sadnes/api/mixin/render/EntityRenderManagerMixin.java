package tech.sadnes.api.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.feature.impl.render.Esp;
import tech.sadnes.client.feature.impl.render.Removals;
import tech.sadnes.client.render.EntityStateCache;

@Mixin(EntityRenderManager.class)
public abstract class EntityRenderManagerMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void sadnes$hideEntityRender(Entity entity, Frustum frustum, double d, double d2, double d3, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (Removals.isEntityHidden(entity)) {
            callbackInfoReturnable.setReturnValue(false);
        } else {
            Esp esp = Esp.getInstance();
            if (esp != null && esp.isChamsTarget(entity)) {
                callbackInfoReturnable.setReturnValue(false);
            }
        }
    }

    @ModifyReturnValue(method = "getSquaredDistanceToCamera", at = @At("RETURN"))
    private double sadnes$previewEntityAtCamera(double d, Entity entity) {
        return EntityStateCache.isCurrent(entity) ? 0.0 : d;
    }
}
