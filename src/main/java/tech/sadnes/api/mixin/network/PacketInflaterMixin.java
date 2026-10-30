package tech.sadnes.api.mixin.network;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import java.util.zip.Inflater;
import net.minecraft.network.handler.PacketInflater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PacketInflater.class)
public class PacketInflaterMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("sadnes/PacketInflater");
    @Shadow
    @Final
    private Inflater inflater;

    @WrapMethod(method = "decode")
    private void sadnes$dropCorruptPacket(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> objects, Operation<Void> operation) {
        try {
            operation.call(channelHandlerContext, byteBuf, objects);
        } catch (Exception exception) {
            LOGGER.warn("Dropped corrupt compressed packet: {}", exception.toString());
            this.inflater.reset();
            byteBuf.skipBytes(byteBuf.readableBytes());
        }
    }
}
