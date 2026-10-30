package tech.sadnes.api.mixin.network;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.api.mixin.accessor.ClientConnectionInvoker;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.impl.movement.GuiWalk;
import tech.sadnes.client.server.donut.CommandTraceComponent;
import tech.sadnes.client.util.compat.LegacySwingOrderFix;
import tech.sadnes.client.util.compat.ProtocolUtil;
import tech.sadnes.client.util.network.TickPacketOrderTracker;
import tech.sadnes.client.util.player.ServerInputTracker;
import tech.sadnes.client.util.player.ServerRotationTracker;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Unique
    private PlayerInput sadnes$inputSentThisTick;

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void sadnes$onSend(Packet<?> packet, CallbackInfo callbackInfo) {
        PacketEvent packetEvent = new PacketEvent(packet, PacketEvent.Direction.SEND);
        packetEvent.post();
        if (packetEvent.isCancelled()) {
            CommandTraceComponent commandTraceComponent = CommandTraceComponent.getInstance();
            if (commandTraceComponent != null) {
                commandTraceComponent.onPacketCancelled(packet);
            }

            callbackInfo.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"))
    private void sadnes$trackServerRotation(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flag, CallbackInfo callbackInfo) {
        ServerRotationTracker.onPacketSend(packet);
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"))
    private void sadnes$trackTickOrder(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flag, CallbackInfo callbackInfo) {
        TickPacketOrderTracker.onPacketSend(packet);
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"))
    private void sadnes$trackServerInput(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flag, CallbackInfo callbackInfo) {
        ServerInputTracker.onPacketSend(packet);
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void sadnes$translateForeignClick(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flag, CallbackInfo callbackInfo) {
        if (packet instanceof ClickSlotC2SPacket && ProtocolUtil.sendLegacyClickSlot((ClickSlotC2SPacket)packet)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"))
    private void sadnes$holdInventorySync(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flag, CallbackInfo callbackInfo) {
        if (packet instanceof ClickSlotC2SPacket) {
            GuiWalk guiWalk = GuiWalk.getInstance();
            if (guiWalk != null) {
                guiWalk.onPacketSent(packet);
            }
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"))
    private void sadnes$traceSent(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flag, CallbackInfo callbackInfo) {
        CommandTraceComponent commandTraceComponent = CommandTraceComponent.getInstance();
        if (commandTraceComponent != null) {
            commandTraceComponent.onPacketSent(packet);
        }
    }

    @Inject(method = "sendInternal", at = @At("HEAD"), cancellable = true)
    private void sadnes$dropDuplicateSwing(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flag, CallbackInfo callbackInfo) {
        if (packet instanceof ClientTickEndC2SPacket) {
            this.sadnes$inputSentThisTick = null;
        } else if (packet instanceof PlayerInputC2SPacket playerInputC2SPacket) {
            PlayerInput playerInput = playerInputC2SPacket.input();
            if (playerInput.equals(this.sadnes$inputSentThisTick)) {
                callbackInfo.cancel();
                return;
            }

            this.sadnes$inputSentThisTick = playerInput;
        }

        if (LegacySwingOrderFix.isDuplicateSwing(packet)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "sendInternal", at = @At("TAIL"))
    private void sadnes$swingAfterAttack(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flag, CallbackInfo callbackInfo) {
        if (LegacySwingOrderFix.shouldSwingFirst(packet)) {
            ((ClientConnectionInvoker)this).sadnes$sendInternal(LegacySwingOrderFix.createSwingPacket(), null, true);
            LegacySwingOrderFix.onSwingSent();
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void sadnes$onReceive(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo callbackInfo) {
        ServerRotationTracker.onPacketReceive(packet);
        ServerInputTracker.onPacketReceive(packet);
        PacketEvent packetEvent = new PacketEvent(packet, PacketEvent.Direction.RECEIVE);
        packetEvent.post();
        if (packetEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
