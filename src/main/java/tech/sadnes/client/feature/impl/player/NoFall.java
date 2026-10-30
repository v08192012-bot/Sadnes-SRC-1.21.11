package tech.sadnes.client.feature.impl.player;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import tech.sadnes.api.mixin.accessor.LivingEntityAccessor;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.JumpEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.event.impl.SendMovementPacketsEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.util.player.ServerRotationTracker;

@FeatureInfo(
    name = "NoFall",
    description = "feature.impl.player.nofall",
    displayName = "feature.impl.player.nofall",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_TWEAKS
)
public final class NoFall extends Feature {
    private double lastFallDistance;
    private int spoofTicks = -1;
    private int retryCount;
    private boolean receivedTeleport;
    private boolean receivedVelocity;
    private boolean forceJump;
    private int pendingCancels;
    private boolean sentMovePacket;

    @Override
    public void onEnable() {
        this.reset();
        super.onEnable();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.reset();
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent playerTickEvent) {
        this.forceJump = false;
        if (this.isSpoofing()) {
            ((LivingEntityAccessor)mc.player).sadnes$setJumpingCooldown(0);
            if (this.receivedTeleport && (this.receivedVelocity || ++this.retryCount > 2)) {
                this.forceJump = mc.player.isOnGround();
                this.resetSpoof();
                this.lastFallDistance = mc.player.fallDistance;
                return;
            }

            if (++this.spoofTicks > 12) {
                this.resetSpoof();
            }
        }

        this.lastFallDistance = mc.player.fallDistance;
    }

    @EventHandler
    public void onInput(MoveInputEvent moveInputEvent) {
        if (this.forceJump) {
            moveInputEvent.setJump(true);
        }
    }

    @EventHandler
    public void onMove(JumpEvent jumpEvent) {
        if (this.isSpoofing()) {
            jumpEvent.cancel();
        }
    }

    @EventHandler
    public void onSendMovement(SendMovementPacketsEvent sendMovementPacketsEvent) {
        if (!sendMovementPacketsEvent.isCancelled()) {
            if (this.isSpoofing()) {
                sendMovementPacketsEvent.cancel();
            } else if (this.shouldSpoofGround(mc.player)) {
                if (this.hasNearbyProjectiles(mc.player)) {
                    sendMovementPacketsEvent.addPackets(this.createGroundPacket(mc.player.getY() + 0.25));
                    this.startSpoof();
                } else {
                    sendMovementPacketsEvent.addPackets(
                        new OnGroundOnly(true, mc.player.horizontalCollision), ClientTickEndC2SPacket.INSTANCE, this.createGroundPacket(mc.player.getY())
                    );
                    this.pendingCancels = Math.min(4, this.pendingCancels + 1);
                    this.startSpoof();
                }
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()) {
            if (packetEvent.getPacket() instanceof PlayerPositionLookS2CPacket) {
                mc.execute(this::handleTeleport);
            } else {
                if (packetEvent.getPacket() instanceof EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket) {
                    mc.execute(() -> this.handleVelocity(entityVelocityUpdateS2CPacket.getEntityId()));
                }
            }
        } else if (packetEvent.isSend()) {
            if (packetEvent.getPacket() instanceof PlayerMoveC2SPacket) {
                this.sentMovePacket = true;
            } else {
                if (packetEvent.getPacket() instanceof ClientTickEndC2SPacket) {
                    if (!this.sentMovePacket && this.pendingCancels > 0) {
                        this.pendingCancels--;
                        packetEvent.cancel();
                    }

                    this.sentMovePacket = false;
                }
            }
        }
    }

    private void handleTeleport() {
        if (this.isSpoofing()) {
            this.receivedTeleport = true;
            this.receivedVelocity = false;
        }
    }

    private void handleVelocity(int i) {
        if (this.receivedTeleport && mc.player != null && i == mc.player.getId()) {
            this.receivedVelocity = true;
        }
    }

    private boolean shouldSpoofGround(ClientPlayerEntity clientPlayerEntity) {
        boolean flag;
        if (clientPlayerEntity.isOnGround() && !clientPlayerEntity.hasVehicle() && !clientPlayerEntity.isGliding() && !clientPlayerEntity.isTouchingWater() && !clientPlayerEntity.isInLava() && !clientPlayerEntity.getAbilities().flying) {
            flag = clientPlayerEntity.getAbilities().invulnerable;
        } else {
            flag = true;
        }

        return flag ? false : this.lastFallDistance > clientPlayerEntity.getAttributeValue(EntityAttributes.SAFE_FALL_DISTANCE) && clientPlayerEntity.lastY - clientPlayerEntity.getY() > 0.05;
    }

    private boolean hasNearbyProjectiles(ClientPlayerEntity clientPlayerEntity) {
        return !mc.world
            .getOtherEntities(
                clientPlayerEntity,
                clientPlayerEntity.getBoundingBox().expand(5.5),
                entity2 -> entity2 instanceof AbstractBoatEntity || entity2 instanceof ShulkerEntity || entity2 instanceof HappyGhastEntity
            )
            .isEmpty();
    }

    private PlayerMoveC2SPacket createGroundPacket(double d) {
        float f = mc.player.getYaw();
        float f2 = mc.player.getPitch();
        boolean flag = mc.player.horizontalCollision;
        boolean flag2 = f != ServerRotationTracker.getServerYaw() || f2 != ServerRotationTracker.getServerPitch();
        return flag2
            ? new Full(mc.player.getX(), d, mc.player.getZ(), f, f2, true, flag)
            : new PositionAndOnGround(mc.player.getX(), d, mc.player.getZ(), true, flag);
    }

    private boolean isSpoofing() {
        return this.spoofTicks >= 0;
    }

    private void startSpoof() {
        this.resetSpoof();
        this.spoofTicks = 0;
    }

    private void resetSpoof() {
        this.spoofTicks = -1;
        this.retryCount = 0;
        this.receivedTeleport = false;
        this.receivedVelocity = false;
    }

    private void reset() {
        this.resetSpoof();
        this.forceJump = false;
        this.pendingCancels = 0;
    }
}
