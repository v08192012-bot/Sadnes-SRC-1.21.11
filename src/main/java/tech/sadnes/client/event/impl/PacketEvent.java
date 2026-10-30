package tech.sadnes.client.event.impl;

import net.minecraft.network.packet.Packet;
import tech.sadnes.client.event.CancellableEvent;

public class PacketEvent extends CancellableEvent {
    private final Packet<?> packet;
    private final PacketEvent.Direction direction;

    public boolean isSend() {
        return this.direction == PacketEvent.Direction.SEND;
    }

    public boolean isGhost() {
        return this.direction == PacketEvent.Direction.GHOST;
    }

    public boolean isReceive() {
        return this.direction == PacketEvent.Direction.RECEIVE;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public PacketEvent.Direction getDirection() {
        return this.direction;
    }

    public PacketEvent(Packet<?> packet2, PacketEvent.Direction direction2) {
        this.packet = packet2;
        this.direction = direction2;
    }

    public enum Direction {
        SEND,
        RECEIVE,
        GHOST;
    }
}
