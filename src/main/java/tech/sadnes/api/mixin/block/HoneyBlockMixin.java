package tech.sadnes.api.mixin.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.HoneyBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.movement.BlockNormalizer;

@Mixin(HoneyBlock.class)
public abstract class HoneyBlockMixin {
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void sadnes$normalizeHoney(BlockState blockState, World world, BlockPos blockPos, Entity entity, EntityCollisionHandler entityCollisionHandler, boolean flag, CallbackInfo callbackInfo) {
        BlockNormalizer blockNormalizer = BlockNormalizer.getInstance();
        if (blockNormalizer != null && blockNormalizer.shouldNormalize("honey") && entity == MinecraftClient.getInstance().player) {
            callbackInfo.cancel();
        }
    }
}
