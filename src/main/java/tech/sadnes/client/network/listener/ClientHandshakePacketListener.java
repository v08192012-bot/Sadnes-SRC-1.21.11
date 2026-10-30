package tech.sadnes.client.network.listener;

import tech.sadnes.client.network.packet.HandshakeResultPacket;

public interface ClientHandshakePacketListener extends ClientPacketListener {
    void onHandshakeResult(HandshakeResultPacket handshakeResultPacket);
}
