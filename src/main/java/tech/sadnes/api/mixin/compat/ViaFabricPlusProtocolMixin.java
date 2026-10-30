package tech.sadnes.api.mixin.compat;

import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com/viaversion/viafabricplus/protocoltranslator/protocol/ViaFabricPlusProtocol", remap = false)
public class ViaFabricPlusProtocolMixin {
    @Inject(method = "lambda$registerPackets$0", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sadnes$skipFabricWorkaroundOnNativeVersion(PacketWrapper packetWrapper, CallbackInfo callbackInfo) {
        ProtocolInfo protocolInfo = packetWrapper.user().getProtocolInfo();
        if (protocolInfo.serverProtocolVersion().equals(protocolInfo.protocolVersion())) {
            callbackInfo.cancel();
        }
    }
}
