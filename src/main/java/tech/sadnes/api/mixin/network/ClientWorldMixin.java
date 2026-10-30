package tech.sadnes.api.mixin.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.impl.BlockUpdateEvent;
import tech.sadnes.client.event.impl.EntityRemoveEvent;
import tech.sadnes.client.feature.impl.render.Removals;
import tech.sadnes.client.render.EntityStateCache;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(method = "updateListeners", at = @At("TAIL"))
    private void sadnes$onBlockUpdate(BlockPos blockPos, BlockState blockState, BlockState blockState2, int i, CallbackInfo callbackInfo) {
        if (Sadnes.getEventBus().hasListeners(BlockUpdateEvent.class)) {
            BlockUpdateEvent.postUpdate(blockPos, blockState2);
        }
    }

    @WrapOperation(
        method = "updateListeners",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;updateBlock(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;I)V"
        )
    )
    private void sadnes$skipHiddenBlockRebuild(
        WorldRenderer worldRenderer, BlockView blockView, BlockPos blockPos, BlockState blockState, BlockState blockState2, int i, Operation<Void> operation
    ) {
        if (!Removals.shouldCullFace(blockState, blockState2)) {
            operation.call(worldRenderer, blockView, blockPos, blockState, blockState2, i);
        }
    }

    @Inject(method = "scheduleBlockRerenderIfNeeded", at = @At("HEAD"), cancellable = true)
    private void sadnes$skipHiddenBlockRerender(BlockPos blockPos, BlockState blockState, BlockState blockState2, CallbackInfo callbackInfo) {
        if (Removals.shouldCullFace(blockState, blockState2)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
    private void sadnes$removeBlockBreakParticles(BlockPos blockPos, BlockState blockState, CallbackInfo callbackInfo) {
        if (Removals.isRemoved("block_particles")) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "spawnBlockBreakingParticle", at = @At("HEAD"), cancellable = true)
    private void sadnes$removeBlockBreakingParticles(BlockPos blockPos, Direction direction, CallbackInfo callbackInfo) {
        if (Removals.isRemoved("block_particles")) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "addBlockParticleEffects", at = @At("HEAD"), cancellable = true)
    private void sadnes$removeExplosionBlockParticles(CallbackInfo callbackInfo) {
        if (Removals.isRemoved("explosion_particles")) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;ZZDDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void sadnes$removeSculkParticles(
        ParticleEffect particleEffect, boolean flag, boolean flag2, double d, double d2, double d3, double d4, double d5, double d6, CallbackInfo callbackInfo
    ) {
        if (Removals.isParticleRemoved(particleEffect)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "getEntityById", at = @At("RETURN"), cancellable = true)
    private void sadnes$previewEntityById(int i, CallbackInfoReturnable<Entity> callbackInfoReturnable) {
        if (callbackInfoReturnable.getReturnValue() == null) {
            Entity entity = EntityStateCache.getEntity(i);
            if (entity != null) {
                callbackInfoReturnable.setReturnValue(entity);
            }
        }
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void sadnes$onEntityRemove(int i, RemovalReason removalReason, CallbackInfo callbackInfo) {
        ClientWorld clientWorld = (ClientWorld)(Object)this;
        Entity entity = clientWorld.getEntityById(i);
        if (entity != null) {
            Sadnes.getEventBus().post(new EntityRemoveEvent(entity, i, removalReason));
        }
    }
}
