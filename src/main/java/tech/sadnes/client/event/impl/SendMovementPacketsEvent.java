package tech.sadnes.client.event.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.packet.Packet;
import tech.sadnes.client.event.CancellableEvent;

public final class SendMovementPacketsEvent extends CancellableEvent {
    private final List<Packet<?>> packets = new ArrayList<>();

    public void addPackets(Packet<?>... packets2) {
        this.setCancelled(true);
        this.packets.clear();
        Collections.addAll(this.packets, packets2);
    }

    public List<Packet<?>> getPackets() {
        return Collections.unmodifiableList(this.packets);
    }
}
