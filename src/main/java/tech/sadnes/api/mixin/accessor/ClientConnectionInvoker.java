package tech.sadnes.api.mixin.accessor;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientConnection.class)
public interface ClientConnectionInvoker {
    @Invoker("sendInternal")
    void sadnes$sendInternal(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flag);
}
