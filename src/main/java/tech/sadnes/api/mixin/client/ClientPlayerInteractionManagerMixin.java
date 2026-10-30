package tech.sadnes.api.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.SlotClickEvent;
import tech.sadnes.client.event.impl.StopUsingItemEvent;
import tech.sadnes.client.event.impl.UseItemEvent;
import tech.sadnes.client.feature.impl.combat.AutoCart;
import tech.sadnes.client.feature.impl.misc.AutoBuild;
import tech.sadnes.client.feature.impl.misc.ServerAssist;
import tech.sadnes.client.feature.impl.movement.Spider;
import tech.sadnes.client.feature.impl.player.ClickAction;
import tech.sadnes.client.feature.impl.player.FastBreak;
import tech.sadnes.client.feature.impl.player.NoInteract;
import tech.sadnes.client.feature.impl.player.OpenWalls;
import tech.sadnes.client.util.network.PacketSender;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void sadnes$fastBreakAttack(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        FastBreak fastBreak = FastBreak.getInstance();
        if (fastBreak != null) {
            if (!blockPos.equals(fastBreak.breakingPos) || direction != fastBreak.breakingDirection) {
                fastBreak.brokeEarly = false;
            }

            fastBreak.breakingPos = blockPos;
            fastBreak.breakingDirection = direction;
        }
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void sadnes$fastBreakUpdate(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        FastBreak fastBreak = FastBreak.getInstance();
        if (fastBreak != null) {
            if (!blockPos.equals(fastBreak.breakingPos) || direction != fastBreak.breakingDirection) {
                fastBreak.brokeEarly = false;
            }

            fastBreak.breakingPos = blockPos;
            fastBreak.breakingDirection = direction;
        }
    }

    @Inject(method = "cancelBlockBreaking", at = @At("HEAD"))
    private void sadnes$fastBreakCancel(CallbackInfo callbackInfo) {
        FastBreak fastBreak = FastBreak.getInstance();
        if (fastBreak != null) {
            fastBreak.breakingPos = null;
            fastBreak.breakingDirection = null;
            fastBreak.brokeEarly = false;
        }
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void sadnes$noInteract(ClientPlayerEntity clientPlayerEntity, Hand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<ActionResult> callbackInfoReturnable) {
        AutoCart autoCart = AutoCart.getInstance();
        if (autoCart == null || !autoCart.isPlacing()) {
            AutoBuild autoBuild = AutoBuild.getInstance();
            if (autoBuild == null || !autoBuild.isBuilding()) {
                NoInteract noInteract = NoInteract.getInstance();
                if (noInteract != null && noInteract.shouldBlockBlocks()) {
                    callbackInfoReturnable.setReturnValue(ActionResult.PASS);
                } else {
                    OpenWalls openWalls = OpenWalls.getInstance();
                    if (openWalls != null && openWalls.hasTarget()) {
                        callbackInfoReturnable.setReturnValue(ActionResult.PASS);
                    } else {
                        ServerAssist serverAssist = ServerAssist.getInstance();
                        if (serverAssist != null && serverAssist.isUsingItem()) {
                            callbackInfoReturnable.setReturnValue(ActionResult.FAIL);
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "pickItemFromEntity", at = @At("HEAD"), cancellable = true)
    private void sadnes$suppressPick(Entity entity, boolean flag, CallbackInfo callbackInfo) {
        ClickAction clickAction = ClickAction.getInstance();
        if (clickAction != null && clickAction.isUsingItem()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void sadnes$noInteractEntity(PlayerEntity playerEntity, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> callbackInfoReturnable) {
        NoInteract noInteract = NoInteract.getInstance();
        if (noInteract != null && noInteract.shouldBlockEntities()) {
            callbackInfoReturnable.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"), cancellable = true)
    private void sadnes$noInteractEntityAtLocation(PlayerEntity playerEntity, Entity entity, EntityHitResult entityHitResult, Hand hand, CallbackInfoReturnable<ActionResult> callbackInfoReturnable) {
        NoInteract noInteract = NoInteract.getInstance();
        if (noInteract != null && noInteract.shouldBlockEntities()) {
            callbackInfoReturnable.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void sadnes$clickSlot(int i, int i2, int i3, SlotActionType slotActionType, PlayerEntity playerEntity, CallbackInfo callbackInfo) {
        Spider spider = Spider.getInstance();
        if (spider != null && spider.handleSlotClick(i2, i3, slotActionType)) {
            callbackInfo.cancel();
        } else {
            SlotClickEvent slotClickEvent = new SlotClickEvent(i, i2, i3, slotActionType);
            slotClickEvent.post();
            if (slotClickEvent.isCancelled()) {
                callbackInfo.cancel();
            }
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void sadnes$usingItemStart(PlayerEntity playerEntity, Hand hand, CallbackInfoReturnable<ActionResult> callbackInfoReturnable) {
        UseItemEvent useItemEvent = new UseItemEvent(UseItemEvent.Stage.START, hand);
        useItemEvent.post();
        if (useItemEvent.isCancelled()) {
            callbackInfoReturnable.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "stopUsingItem", at = @At("HEAD"), cancellable = true)
    private void sadnes$stopUsingItem(PlayerEntity playerEntity, CallbackInfo callbackInfo) {
        StopUsingItemEvent stopUsingItemEvent = new StopUsingItemEvent(playerEntity.getActiveItem());
        stopUsingItemEvent.post();
        if (stopUsingItemEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void sadnes$attackEntityPre(PlayerEntity playerEntity, Entity entity, CallbackInfo callbackInfo, @Share("weaponSlot") LocalIntRef localIntRef) {
        AttackEntityEvent attackEntityEvent = new AttackEntityEvent(playerEntity, entity, AttackEntityEvent.Stage.PRE);
        attackEntityEvent.post();
        localIntRef.set(attackEntityEvent.getWeaponSlot());
        if (attackEntityEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @WrapOperation(
        method = "attackEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V")
    )
    private void sadnes$attackWithWeapon(
        ClientPlayNetworkHandler clientPlayNetworkHandler, Packet<?> packet, Operation<Void> operation, PlayerEntity playerEntity, Entity entity, @Share("weaponSlot") LocalIntRef localIntRef
    ) {
        int i = localIntRef.get();
        int i2 = playerEntity.getInventory().getSelectedSlot();
        if (i >= 0 && i < 9 && i != i2) {
            PacketSender.sendSilent(new UpdateSelectedSlotC2SPacket(i));

            try {
                PacketSender.sendSilent(packet);
            } finally {
                PacketSender.sendSilent(new UpdateSelectedSlotC2SPacket(i2));
            }
        } else {
            operation.call(clientPlayNetworkHandler, packet);
        }
    }

    @Inject(method = "attackEntity", at = @At("RETURN"))
    private void sadnes$attackEntityPost(PlayerEntity playerEntity, Entity entity, CallbackInfo callbackInfo) {
        new AttackEntityEvent(playerEntity, entity, AttackEntityEvent.Stage.POST).post();
    }
}
