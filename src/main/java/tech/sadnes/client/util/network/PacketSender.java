package tech.sadnes.client.util.network;

import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.packet.Packet;
import tech.sadnes.api.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import tech.sadnes.client.util.IMinecraft;

public class PacketSender {
    public static void sendSilent(Packet<?> packet) {
        IMinecraft.mc.getNetworkHandler().getConnection().send(packet, null);
    }

    public static void sendSequenced(SequencedPacketCreator sequencedPacketCreator) {
        ((ClientPlayerInteractionManagerAccessor)IMinecraft.mc.interactionManager).invokeSendSequencedPacket(IMinecraft.mc.world, sequencedPacketCreator);
    }
}
