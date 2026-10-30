package tech.sadnes.client.network;

import tech.sadnes.client.network.listener.PacketListener;

public interface Packet<H extends PacketListener> {
    void write(PacketBuffer packetBuffer);

    void handle(H value);
}
