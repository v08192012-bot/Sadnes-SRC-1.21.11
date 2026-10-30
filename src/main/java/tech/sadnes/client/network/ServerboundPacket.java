package tech.sadnes.client.network;

import tech.sadnes.client.network.listener.ServerPacketListener;

public interface ServerboundPacket<H extends ServerPacketListener> extends Packet<H> {
}
