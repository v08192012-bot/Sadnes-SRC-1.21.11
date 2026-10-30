package tech.sadnes.api.mixin.accessor;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("jumpingCooldown")
    void sadnes$setJumpingCooldown(int i);

    @Accessor("jumpingCooldown")
    int sadnes$getJumpingCooldown();

    @Accessor("jumping")
    boolean sadnes$isJumping();

    @Invoker("getJumpVelocity")
    float sadnes$invokeGetJumpVelocity();

    @Invoker("getEffectiveGravity")
    double sadnes$invokeGetEffectiveGravity();
}
