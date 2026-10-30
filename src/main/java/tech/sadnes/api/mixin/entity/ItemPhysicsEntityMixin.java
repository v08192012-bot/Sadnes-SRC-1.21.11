package tech.sadnes.api.mixin.entity;

import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.api.duck.TumbleHolder;
import tech.sadnes.client.animation.SnappingSpinAnimation;
import tech.sadnes.client.feature.impl.render.ItemPhysics;

@Mixin(ItemEntity.class)
public abstract class ItemPhysicsEntityMixin implements TumbleHolder {
    @Unique
    private final SnappingSpinAnimation sadnes$tumble = new SnappingSpinAnimation();

    @Override
    public SnappingSpinAnimation sadnes$tumble() {
        return this.sadnes$tumble;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sadnes$tickTumble(CallbackInfo callbackInfo) {
        ItemEntity itemEntity = (ItemEntity)(Object)this;
        if (itemEntity.getEntityWorld().isClient() && ItemPhysics.isActive()) {
            if (!itemEntity.isOnGround() && !itemEntity.isTouchingWater()) {
                this.sadnes$tumble.tick(false, ItemPhysics.getInstance().getSpinSpeed());
            } else {
                this.sadnes$tumble.tick(true, ItemPhysics.getInstance().getSpinSpeed());
            }
        }
    }
}
