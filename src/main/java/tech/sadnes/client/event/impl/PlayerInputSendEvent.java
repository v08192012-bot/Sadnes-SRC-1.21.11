package tech.sadnes.client.event.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.PlayerInput;
import tech.sadnes.client.event.CancellableEvent;

public final class PlayerInputSendEvent extends CancellableEvent {
    private PlayerInput input;
    private final List<Packet<?>> extraPackets = new ArrayList<>();
    private final List<Runnable> callbacks = new ArrayList<>();

    public PlayerInputSendEvent(PlayerInput playerInput) {
        this.input = playerInput;
    }

    public void addPacket(Packet<?> packet) {
        if (packet != null) {
            this.extraPackets.add(packet);
        }
    }

    public void addInputPacket(PlayerInput playerInput) {
        if (playerInput != null) {
            this.extraPackets.add(new PlayerInputC2SPacket(playerInput));
        }
    }

    public void addCallback(Runnable runnable) {
        if (runnable != null) {
            this.callbacks.add(runnable);
        }
    }

    public List<Packet<?>> getExtraPackets() {
        return Collections.unmodifiableList(this.extraPackets);
    }

    public List<Runnable> getCallbacks() {
        return Collections.unmodifiableList(this.callbacks);
    }

    public void setJump(boolean flag) {
        this.input = new PlayerInput(
            this.input.forward(), this.input.backward(), this.input.left(), this.input.right(), flag, this.input.sneak(), this.input.sprint()
        );
    }

    public void setSneak(boolean flag) {
        this.input = new PlayerInput(
            this.input.forward(), this.input.backward(), this.input.left(), this.input.right(), this.input.jump(), flag, this.input.sprint()
        );
    }

    public void setSprint(boolean flag) {
        this.input = new PlayerInput(
            this.input.forward(), this.input.backward(), this.input.left(), this.input.right(), this.input.jump(), this.input.sneak(), flag
        );
    }

    public PlayerInput getInput() {
        return this.input;
    }

    public void setInput(PlayerInput playerInput) {
        this.input = playerInput;
    }
}
