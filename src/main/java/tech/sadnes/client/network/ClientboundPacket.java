package tech.sadnes.client.network;

import tech.sadnes.client.network.listener.ClientPacketListener;

public interface ClientboundPacket<H extends ClientPacketListener> extends Packet<H> {
}
