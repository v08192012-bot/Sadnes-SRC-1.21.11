package tech.sadnes.api.mixin.network;

import java.util.ArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.script.ScriptManager;

@Mixin(targets = "net/minecraft/network/PacketApplyBatcher$Entry")
public abstract class PacketApplyBatcherEntryMixin {
    @Shadow
    @Final
    private PacketListener listener;
    @Shadow
    @Final
    private Packet<?> packet;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void sadnes$scriptPacket(CallbackInfo callbackInfo) {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null && MinecraftClient.getInstance().isOnThread()) {
            if (this.packet instanceof BundleS2CPacket bundleS2CPacket) {
                ArrayList arrayList = new ArrayList();
                boolean flag = false;

                for (Packet packet2 : bundleS2CPacket.getPackets()) {
                    if (scriptManager.handleIncomingPacket(packet2)) {
                        flag = true;
                    } else {
                        arrayList.add(packet2);
                    }
                }

                if (flag) {
                    callbackInfo.cancel();
                    if (this.listener instanceof ClientPlayPacketListener clientPlayPacketListener && this.listener.accepts(this.packet)) {
                        try {
                            new BundleS2CPacket(arrayList).apply(clientPlayPacketListener);
                        } catch (Exception exception) {
                            this.listener.onPacketException(this.packet, exception);
                        }
                    }
                }
            } else {
                if (scriptManager.handleIncomingPacket(this.packet)) {
                    callbackInfo.cancel();
                }
            }
        }
    }
}
