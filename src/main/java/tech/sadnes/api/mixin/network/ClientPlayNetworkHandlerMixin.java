package tech.sadnes.api.mixin.network;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.impl.BlockUpdateEvent;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.ItemPickupEvent;
import tech.sadnes.client.feature.impl.combat.VelocityControl;
import tech.sadnes.client.feature.impl.misc.ChatHelper;
import tech.sadnes.client.feature.impl.render.Particles;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void sadnes$onGameJoin(GameJoinS2CPacket gameJoinS2CPacket, CallbackInfo callbackInfo) {
        Sadnes.getEventBus().post(GameJoinEvent.getInstance());
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void sadnes$translateChatMessage(String string, CallbackInfo callbackInfo) {
        if (ChatHelper.handleOutgoingMessage(string)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void sadnes$onChunkData(ChunkDataS2CPacket chunkDataS2CPacket, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world != null) {
            this.sadnes$scanChunk(minecraftClient.world.getChunk(chunkDataS2CPacket.getChunkX(), chunkDataS2CPacket.getChunkZ()), BlockUpdateEvent.Type.LOAD);
        }
    }

    @Inject(method = "onUnloadChunk", at = @At("HEAD"))
    private void sadnes$onUnloadChunk(UnloadChunkS2CPacket unloadChunkS2CPacket, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world != null) {
            this.sadnes$scanChunk(minecraftClient.world.getChunk(unloadChunkS2CPacket.pos().x, unloadChunkS2CPacket.pos().z), BlockUpdateEvent.Type.UNLOAD);
        }
    }

    @Unique
    private void sadnes$scanChunk(WorldChunk worldChunk, BlockUpdateEvent.Type type) {
        if (Sadnes.getEventBus().hasListeners(BlockUpdateEvent.class)) {
            int i = worldChunk.getPos().getStartX();
            int i2 = worldChunk.getPos().getStartZ();
            int i3 = worldChunk.getHighestNonEmptySection();
            int i4 = worldChunk.getBottomY() >> 4;
            boolean flag = type == BlockUpdateEvent.Type.LOAD;
            CompletableFuture.runAsync(() -> {
                BlockState blockState = Blocks.AIR.getDefaultState();
                BlockUpdateEvent blockUpdateEvent = new BlockUpdateEvent(type);
                Mutable mutable = new Mutable();

                for (int i5 = 0; i5 <= i3; i5++) {
                    ChunkSection chunkSection = worldChunk.getSection(i5);
                    if (!flag || !chunkSection.isEmpty()) {
                        for (int i6 = 0; i6 < 16; i6++) {
                            int i7 = i5 + i4 << 4 | i6;

                            for (int i8 = 0; i8 < 16; i8++) {
                                for (int i9 = 0; i9 < 16; i9++) {
                                    BlockState blockState2 = flag ? chunkSection.getBlockState(i8, i6, i9) : blockState;
                                    if (!flag || !blockState2.isAir()) {
                                        blockUpdateEvent.fire(mutable.set(i | i8, i7, i2 | i9), blockState2);
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    @Inject(method = "onPlayerRespawn", at = @At("TAIL"))
    private void sadnes$onPlayerRespawn(PlayerRespawnS2CPacket playerRespawnS2CPacket, CallbackInfo callbackInfo) {
        Sadnes.getEventBus().post(GameJoinEvent.getInstance());
    }

    @Inject(method = "onItemPickupAnimation", at = @At("HEAD"))
    private void sadnes$onItemPickup(ItemPickupAnimationS2CPacket itemPickupAnimationS2CPacket, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world != null) {
            Entity entity = minecraftClient.world.getEntityById(itemPickupAnimationS2CPacket.getEntityId());
            Entity entity2 = minecraftClient.world.getEntityById(itemPickupAnimationS2CPacket.getCollectorEntityId());
            if (entity2 == null) {
                entity2 = minecraftClient.player;
            }

            if (entity != null && entity2 != null) {
                Sadnes.getEventBus().post(new ItemPickupEvent(entity2, entity, itemPickupAnimationS2CPacket.getStackAmount()));
            }
        }
    }

    @Redirect(method = "onExplosion", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private void sadnes$explosionKnockback(Optional<Vec3d> optionalVec3d, Consumer<Vec3d> consumer, ExplosionS2CPacket explosionS2CPacket) {
        VelocityControl velocityControl = VelocityControl.getInstance();
        if (velocityControl != null && velocityControl.isEnabled()) {
            boolean flag = explosionS2CPacket.explosionSound().value() == SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST.value();
            if (flag) {
                if (velocityControl.cancelWindCharge()) {
                    return;
                }
            } else if (velocityControl.cancelExplosions()) {
                return;
            }
        }

        optionalVec3d.ifPresent(consumer);
    }

    @Redirect(
        method = "onEntityStatus",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleManager;addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V"
        )
    )
    private void sadnes$suppressTotemParticles(ParticleManager particleManager, Entity entity, ParticleEffect particleEffect, int i) {
        Particles particles = Particles.getInstance();
        if (particles == null || !particles.isTotemEnabled()) {
            particleManager.addEmitter(entity, particleEffect, i);
        }
    }
}
