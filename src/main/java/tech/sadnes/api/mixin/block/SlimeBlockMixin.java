package tech.sadnes.api.mixin.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.SlimeBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.event.impl.SlimeBounceEvent;
import tech.sadnes.client.feature.impl.movement.BlockNormalizer;

@Mixin(SlimeBlock.class)
public abstract class SlimeBlockMixin {
    @Inject(method = "onSteppedOn", at = @At("HEAD"), cancellable = true)
    private void sadnes$normalizeSlime(World world, BlockPos blockPos, BlockState blockState, Entity entity, CallbackInfo callbackInfo) {
        BlockNormalizer blockNormalizer = BlockNormalizer.getInstance();
        if (blockNormalizer != null && blockNormalizer.shouldNormalize("slime") && entity == MinecraftClient.getInstance().player) {
            callbackInfo.cancel();
        }
    }

    @Redirect(method = "onEntityLand", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;bypassesLandingEffects()Z"))
    private boolean sadnes$slimeUnbounce(Entity entity) {
        if (entity != MinecraftClient.getInstance().player) {
            return entity.bypassesLandingEffects();
        }

        SlimeBounceEvent slimeBounceEvent = new SlimeBounceEvent();
        slimeBounceEvent.post();
        return slimeBounceEvent.isCancelled() || entity.bypassesLandingEffects();
    }
}
