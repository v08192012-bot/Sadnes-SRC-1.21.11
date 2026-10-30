package tech.sadnes.api.mixin.entity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tech.sadnes.client.event.impl.FireworkBoostEvent;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    private void sadnes$boost(LivingEntity livingEntity, Vec3d vec3d) {
        if (livingEntity != MinecraftClient.getInstance().player) {
            livingEntity.setVelocity(vec3d);
        } else {
            FireworkBoostEvent fireworkBoostEvent = new FireworkBoostEvent(livingEntity, 1.5, 1.5);
            fireworkBoostEvent.post();
            if (!fireworkBoostEvent.isModified()) {
                livingEntity.setVelocity(vec3d);
            } else {
                Vec3d vec3d2 = livingEntity.getRotationVector();
                Vec3d vec3d3 = livingEntity.getVelocity();
                double d = fireworkBoostEvent.getHorizontalSpeed();
                double d2 = fireworkBoostEvent.getVerticalSpeed();
                livingEntity.setVelocity(
                    vec3d3.add(
                        vec3d2.x * 0.1 + (vec3d2.x * d - vec3d3.x) * 0.5,
                        vec3d2.y * 0.1 + (vec3d2.y * d2 - vec3d3.y) * 0.5,
                        vec3d2.z * 0.1 + (vec3d2.z * d - vec3d3.z) * 0.5
                    )
                );
            }
        }
    }
}
