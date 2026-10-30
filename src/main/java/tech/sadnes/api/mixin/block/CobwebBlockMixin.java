package tech.sadnes.api.mixin.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.movement.NoWeb;

@Mixin(CobwebBlock.class)
public abstract class CobwebBlockMixin {
    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void sadnes$noWeb(BlockState blockState, World world, BlockPos blockPos, Entity entity, EntityCollisionHandler entityCollisionHandler, boolean flag, CallbackInfo callbackInfo) {
        NoWeb noWeb = NoWeb.getInstance();
        if (noWeb != null && noWeb.isIgnoringWeb() && entity == MinecraftClient.getInstance().player) {
            callbackInfo.cancel();
            noWeb.breakWeb(blockPos);
        }
    }
}
