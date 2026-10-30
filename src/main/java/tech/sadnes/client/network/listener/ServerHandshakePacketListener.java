package tech.sadnes.client.network.listener;

import tech.sadnes.client.network.packet.HandshakePacket;

public interface ServerHandshakePacketListener extends ServerPacketListener {
    void onHandshake(HandshakePacket handshakePacket);
}
