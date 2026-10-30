package tech.sadnes.client.network.listener;

import tech.sadnes.client.network.packet.AuthResultPacket;

public interface ClientAuthPacketListener extends ClientPacketListener {
    void onAuthResult(AuthResultPacket authResultPacket);
}
