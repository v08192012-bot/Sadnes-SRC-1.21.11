package tech.sadnes.api.mixin.compat;

import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tech.sadnes.client.util.compat.ViaPacketDeduplicator;

@Pseudo
@Mixin(targets = "com/viaversion/viaversion/protocols/v1_21to1_21_2/rewriter/EntityPacketRewriter1_21_2", remap = false)
public class ViaEntityPacketRewriterMixin {
    @Redirect(
        method = "lambda$registerPackets$13",
        at = @At(value = "INVOKE", target = "Lcom/viaversion/viaversion/api/protocol/packet/PacketWrapper;sendToServer(Ljava/lang/Class;)V", ordinal = 1),
        remap = false
    )
    private void sadnes$throttleLegacySteer(PacketWrapper packetWrapper, Class<? extends Protocol> clazz) {
        float f = packetWrapper.get(Types.FLOAT, 0);
        float f2 = packetWrapper.get(Types.FLOAT, 1);
        byte b = packetWrapper.get(Types.BYTE, 0);
        if (ViaPacketDeduplicator.shouldSend(packetWrapper.user(), f, f2, b)) {
            packetWrapper.sendToServer(clazz);
        }
    }
}
