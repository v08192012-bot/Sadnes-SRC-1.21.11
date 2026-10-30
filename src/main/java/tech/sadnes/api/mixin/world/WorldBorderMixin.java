package tech.sadnes.api.mixin.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.feature.impl.movement.NoPush;

@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {
    @Inject(method = "canCollide", at = @At("HEAD"), cancellable = true)
    private void sadnes$noWorldBorderCollision(Entity entity, Box box, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (NoPush.getInstance() != null && NoPush.getInstance().isEnabled() && NoPush.getInstance().pushes.has("world_border") && entity == minecraftClient.player) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }
}
