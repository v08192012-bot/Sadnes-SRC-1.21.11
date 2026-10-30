package tech.sadnes.client.network.listener;

import tech.sadnes.client.network.packet.AuthRequestPacket;

public interface ServerAuthPacketListener extends ServerPacketListener {
    void onAuthRequest(AuthRequestPacket authRequestPacket);
}
