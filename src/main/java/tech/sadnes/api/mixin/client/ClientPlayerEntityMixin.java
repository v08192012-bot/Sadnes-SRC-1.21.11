package tech.sadnes.api.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Iterator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.api.duck.TicksOnGroundAccessor;
import tech.sadnes.api.mixin.accessor.ClientPlayerEntityAccessor;
import tech.sadnes.api.mixin.accessor.InputAccessor;
import tech.sadnes.client.event.impl.CloseScreenEvent;
import tech.sadnes.client.event.impl.MoveEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PlayerInputPacketEvent;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.event.impl.PostMotionEvent;
import tech.sadnes.client.event.impl.PostPhysicsEvent;
import tech.sadnes.client.event.impl.SendMovementPacketsEvent;
import tech.sadnes.client.event.impl.SlowdownEvent;
import tech.sadnes.client.event.impl.SprintEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.event.impl.UseItemEvent;
import tech.sadnes.client.feature.component.FreeLookComponent;
import tech.sadnes.client.feature.component.MovementPauseComponent;
import tech.sadnes.client.feature.impl.misc.LockSlot;
import tech.sadnes.client.feature.impl.movement.NoPush;
import tech.sadnes.client.feature.impl.movement.NoSlow;
import tech.sadnes.client.feature.impl.movement.Spider;
import tech.sadnes.client.util.compat.ProtocolUtil;
import tech.sadnes.client.util.network.PacketQueue;
import tech.sadnes.client.util.player.MovementSimulator;
import tech.sadnes.client.util.player.ServerInputTracker;
import tech.sadnes.client.util.player.ServerRotationTracker;
import tech.sadnes.client.util.player.SprintUtil;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin implements TicksOnGroundAccessor {
    @Shadow
    public float renderYaw;
    @Shadow
    public float renderPitch;
    @Shadow
    public float lastRenderYaw;
    @Shadow
    public float lastRenderPitch;
    @Shadow
    public Input input;
    @Unique
    private static boolean sadnes$inCrosshairRaytrace;
    @Unique
    private boolean sadnes$spoofOnGround;
    @Unique
    private boolean sadnes$onGroundValue;
    @Unique
    private int sadnes$ticksOnGround;

    @Inject(method = "getCrosshairTarget(FLnet/minecraft/entity/Entity;)Lnet/minecraft/util/hit/HitResult;", at = @At("HEAD"))
    private void sadnes$crosshairRaytraceHead(float f, Entity entity, CallbackInfoReturnable<HitResult> callbackInfoReturnable) {
        sadnes$inCrosshairRaytrace = true;
    }

    @Inject(method = "getCrosshairTarget(FLnet/minecraft/entity/Entity;)Lnet/minecraft/util/hit/HitResult;", at = @At("RETURN"))
    private void sadnes$crosshairRaytraceTail(float f, Entity entity, CallbackInfoReturnable<HitResult> callbackInfoReturnable) {
        sadnes$inCrosshairRaytrace = false;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sadnes$onPlayerSpawn(
        MinecraftClient minecraftClient,
        ClientWorld clientWorld,
        ClientPlayNetworkHandler clientPlayNetworkHandler,
        StatHandler statHandler,
        ClientRecipeBook clientRecipeBook,
        PlayerInput playerInput,
        boolean flag,
        CallbackInfo callbackInfo
    ) {
        MovementSimulator.resetTicks();
        MovementPauseComponent.clearPause();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void sadnes$playerTickFreeze(CallbackInfo callbackInfo) {
        PlayerTickEvent playerTickEvent = new PlayerTickEvent();
        playerTickEvent.post();
        if (playerTickEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void sadnes$resyncHeldInput(CallbackInfo callbackInfo) {
        ServerInputTracker.restoreLastInput();
    }

    @WrapWithCondition(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V")
    )
    private boolean sadnes$skipInputWhileHeld(ClientPlayNetworkHandler clientPlayNetworkHandler, Packet<?> packet) {
        return !(packet instanceof PlayerInputC2SPacket) || !ServerInputTracker.isInputSentManually();
    }

    @WrapOperation(method = "tickRiding", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;setInputs(ZZZZ)V"))
    private void sadnes$steerBoatAsServerSees(AbstractBoatEntity abstractBoatEntity, boolean flag, boolean flag2, boolean flag3, boolean flag4, Operation<Void> operation) {
        if (!ServerInputTracker.isInputSentManually()) {
            operation.call(abstractBoatEntity, flag, flag2, flag3, flag4);
        } else {
            PlayerInput playerInput = ServerInputTracker.getLastInput();
            operation.call(abstractBoatEntity, playerInput.left(), playerInput.right(), playerInput.forward(), playerInput.backward());
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Cooldown;tick()V", shift = Shift.AFTER))
    private void sadnes$onUpdate(CallbackInfo callbackInfo) {
        TickEvent.getInstance().post();
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick()V", shift = Shift.AFTER))
    private void sadnes$foreignMoveInput(CallbackInfo callbackInfo) {
        if (!(this.input instanceof KeyboardInput)) {
            PlayerInput playerInput2 = this.input.playerInput;
            float f = sadnes$axis(playerInput2.forward(), playerInput2.backward());
            float f2 = sadnes$axis(playerInput2.left(), playerInput2.right());
            Vec2f vec2f = new Vec2f(f2, f);
            ((InputAccessor)this.input).sadnes$setMovementVector(ProtocolUtil.isOlderThan("1.21.5") ? vec2f : vec2f.normalize());
            MoveInputEvent moveInputEvent = new MoveInputEvent(f, f2, playerInput2.jump(), playerInput2.sneak(), 0.3);
            moveInputEvent.post();
            if (moveInputEvent.isCancelled()) {
                this.input.playerInput = new PlayerInput(false, false, false, false, false, false, playerInput2.sprint());
                ((InputAccessor)this.input).sadnes$setMovementVector(Vec2f.ZERO);
            }
        }
    }

    @Unique
    private static float sadnes$axis(boolean flag, boolean flag2) {
        if (flag == flag2) {
            return 0.0F;
        } else {
            return flag ? 1.0F : -1.0F;
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift = Shift.AFTER))
    private void sadnes$onPostPhysics(CallbackInfo callbackInfo) {
        new PostPhysicsEvent().post();
    }

    @Inject(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V", shift = Shift.AFTER)
    )
    private void sadnes$onPostMotion(CallbackInfo callbackInfo) {
        PostMotionEvent.getInstance().post();
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void sadnes$flushPreFlying(CallbackInfo callbackInfo) {
        PacketQueue.flush();
        SendMovementPacketsEvent sendMovementPacketsEvent = new SendMovementPacketsEvent();
        sendMovementPacketsEvent.post();
        ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity)(Object)this;
        boolean flag = this.sadnes$spoofOnGround ? this.sadnes$onGroundValue : clientPlayerEntity.isOnGround();
        if (flag) {
            this.sadnes$ticksOnGround++;
        } else {
            this.sadnes$ticksOnGround = 0;
        }

        if (sendMovementPacketsEvent.isCancelled()) {
            for (Packet packet : sendMovementPacketsEvent.getPackets()) {
                clientPlayerEntity.networkHandler.sendPacket(packet);
            }

            callbackInfo.cancel();
        }
    }

    @Override
    public int sadnes$getTicksOnGround() {
        return this.sadnes$ticksOnGround;
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3d sadnes$onMove(Vec3d vec3d) {
        ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity)(Object)this;
        boolean flag = clientPlayerEntity.isOnGround();
        MoveEvent moveEvent = new MoveEvent(vec3d.x, vec3d.y, vec3d.z, flag);
        moveEvent.post();
        this.sadnes$onGroundValue = moveEvent.isOnGround();
        this.sadnes$spoofOnGround = moveEvent.isOnGround() != flag;
        return moveEvent.isCancelled() ? new Vec3d(moveEvent.getX(), moveEvent.getY(), moveEvent.getZ()) : vec3d;
    }

    @Redirect(
        method = "sendMovementPackets",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V")
    )
    private void sadnes$onPlayerInputPacket(ClientPlayNetworkHandler clientPlayNetworkHandler, Packet<?> packet) {
        if (!(packet instanceof PlayerInputC2SPacket playerInputC2SPacket)) {
            clientPlayNetworkHandler.sendPacket(packet);
        } else {
            PlayerInputPacketEvent playerInputPacketEvent = new PlayerInputPacketEvent(playerInputC2SPacket.input());
            playerInputPacketEvent.post();
            if (!playerInputPacketEvent.isCancelled()) {
                PlayerInput playerInput = playerInputPacketEvent.getInput();
                clientPlayNetworkHandler.sendPacket(playerInput.equals(playerInputC2SPacket.input()) ? packet : new PlayerInputC2SPacket(playerInput));
            }

            for (Packet packet2 : playerInputPacketEvent.getExtraPackets()) {
                clientPlayNetworkHandler.sendPacket(packet2);
            }

            Iterator iterator2 = playerInputPacketEvent.getCallbacks().iterator();

            while (iterator2.hasNext()) {
                ((Runnable)iterator2.next()).run();
            }
        }
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isOnGround()Z"))
    private boolean sadnes$spoofPacketOnGround(ClientPlayerEntity clientPlayerEntity) {
        return this.sadnes$spoofOnGround ? this.sadnes$onGroundValue : clientPlayerEntity.isOnGround();
    }

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void sadnes$lockSlotDrop(boolean flag, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        LockSlot lockSlot = LockSlot.getInstance();
        if (lockSlot != null && lockSlot.isSelectedSlotLocked()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "closeHandledScreen", at = @At("HEAD"), cancellable = true)
    private void sadnes$onCloseHandledScreen(CallbackInfo callbackInfo) {
        Spider spider = Spider.getInstance();
        if (spider != null) {
            spider.setSelectingItem(false);
        }

        CloseScreenEvent closeScreenEvent = new CloseScreenEvent(MinecraftClient.getInstance().currentScreen);
        closeScreenEvent.post();
        if (closeScreenEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSprinting()Z", ordinal = 0))
    private void sadnes$beforeSprintStopChecks(CallbackInfo callbackInfo) {
        ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity)(Object)this;
        SprintEvent sprintEvent = new SprintEvent(SprintEvent.Stage.NOSPRINT, false);
        sprintEvent.post();
        if (sprintEvent.isModified()) {
            this.sadnes$setSprintState(clientPlayerEntity, sprintEvent.isSprinting());
        }
    }

    @Inject(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerAbilities;allowFlying:Z", ordinal = 0))
    private void sadnes$afterSprintStopChecks(CallbackInfo callbackInfo) {
        ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity)(Object)this;
        SprintEvent sprintEvent = new SprintEvent(SprintEvent.Stage.AFSPRINT, false);
        sprintEvent.post();
        if (sprintEvent.isModified()) {
            this.sadnes$setSprintState(clientPlayerEntity, sprintEvent.isSprinting());
        }

        if (clientPlayerEntity.isSprinting() && SprintUtil.shouldStopSprint(clientPlayerEntity)) {
            this.sadnes$setSprintState(clientPlayerEntity, false);
        }
    }

    private void sadnes$setSprintState(ClientPlayerEntity clientPlayerEntity, boolean flag) {
        clientPlayerEntity.setSprinting(flag);
        ((ClientPlayerEntityAccessor)clientPlayerEntity).sadnes$invokeSendSprintingPacket();
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void sadnes$noBlockPush(double d, double d2, CallbackInfo callbackInfo) {
        if (NoPush.getInstance() != null && NoPush.getInstance().isEnabled() && NoPush.getInstance().pushes.has("blocks")) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "tickMovementInput", at = @At("RETURN"))
    private void sadnes$fixArmRotation(CallbackInfo callbackInfo) {
        if (FreeLookComponent.isActive()) {
            float f = FreeLookComponent.getYaw();
            float f2 = FreeLookComponent.getPitch();
            this.renderPitch = this.lastRenderPitch + (f2 - this.lastRenderPitch) * 0.5F;
            this.renderYaw = this.lastRenderYaw + (f - this.lastRenderYaw) * 0.5F;
        }
    }

    @Inject(method = "getPitch", at = @At("HEAD"), cancellable = true)
    private void sadnes$cameraRenderPitch(float f, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        if (ServerRotationTracker.isActive() && MinecraftClient.getInstance().player == (Object)this) {
            callbackInfoReturnable.setReturnValue(ServerRotationTracker.getServerPitch());
        } else if (!sadnes$inCrosshairRaytrace) {
            if (FreeLookComponent.isActive() && MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {
                callbackInfoReturnable.setReturnValue(FreeLookComponent.getPitch());
            }
        }
    }

    @Inject(method = "getYaw", at = @At("HEAD"), cancellable = true)
    private void sadnes$cameraRenderYaw(float f, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        if (ServerRotationTracker.isActive() && MinecraftClient.getInstance().player == (Object)this) {
            callbackInfoReturnable.setReturnValue(ServerRotationTracker.getServerYaw());
        } else if (!sadnes$inCrosshairRaytrace) {
            if (FreeLookComponent.isActive() && MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {
                callbackInfoReturnable.setReturnValue(FreeLookComponent.getYaw());
            }
        }
    }

    @Inject(method = "shouldSlowDown", at = @At("HEAD"), cancellable = true)
    private void sadnes$shouldSlowDownHook(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (NoSlow.getInstance().isEnabled()) {
            ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity)(Object)this;
            SlowdownEvent slowdownEvent = new SlowdownEvent();
            slowdownEvent.post();
            if (clientPlayerEntity.isCrawling() && NoSlow.getInstance().modes.has("crawl") && clientPlayerEntity.isInSwimmingPose() && !clientPlayerEntity.isTouchingWater()) {
                callbackInfoReturnable.setReturnValue(slowdownEvent.isCrawlSlowdown());
            } else {
                if (clientPlayerEntity.isSneaking() && NoSlow.getInstance().modes.has("sneak")) {
                    callbackInfoReturnable.setReturnValue(slowdownEvent.isSneakSlowdown());
                }
            }
        }
    }

    @Inject(method = "getActiveItemSpeedMultiplier", at = @At("HEAD"), cancellable = true)
    private void sadnes$activeItemSpeedMultiplier(CallbackInfoReturnable<Float> callbackInfoReturnable) {
        ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity)(Object)this;
        if (clientPlayerEntity.isUsingItem()) {
            UseItemEvent useItemEvent = new UseItemEvent(UseItemEvent.Stage.ON);
            useItemEvent.post();
            if (useItemEvent.isCancelled()) {
                callbackInfoReturnable.setReturnValue(1.0F);
            }
        }
    }
}
