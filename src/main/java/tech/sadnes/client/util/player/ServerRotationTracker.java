package tech.sadnes.client.util.player;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public final class ServerRotationTracker {
    private static float serverYaw;
    private static float serverPitch;
    private static boolean hasRotation;
    private static boolean active;

    private ServerRotationTracker() {
    }

    public static void onPacketSend(Packet<?> packet) {
        if (packet instanceof PlayerMoveC2SPacket playerMoveC2SPacket && playerMoveC2SPacket.changesLook()) {
            serverYaw = playerMoveC2SPacket.getYaw(serverYaw);
            serverPitch = playerMoveC2SPacket.getPitch(serverPitch);
            hasRotation = true;
        }
    }

    public static void onPacketReceive(Packet<?> packet) {
        if (packet instanceof PlayerPositionLookS2CPacket playerPositionLookS2CPacket) {
            serverYaw = playerPositionLookS2CPacket.change().yaw();
            serverPitch = playerPositionLookS2CPacket.change().pitch();
            hasRotation = true;
        }
    }

    public static void enable() {
        active = true;
    }

    public static void disable() {
        active = false;
    }

    public static boolean isActive() {
        return active && hasRotation;
    }

    public static float getServerYaw() {
        return serverYaw;
    }

    public static float getServerPitch() {
        return serverPitch;
    }
}
