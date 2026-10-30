package tech.sadnes.client.util.network;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PickItemFromBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PickItemFromEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;

public final class TickPacketOrderTracker {
    private static volatile boolean movementSent;
    private static volatile boolean teleportConfirmed;
    private static volatile boolean actionSent;

    private TickPacketOrderTracker() {
    }

    public static void onPacketSend(Packet<?> packet) {
        if (packet instanceof TeleportConfirmC2SPacket) {
            teleportConfirmed = true;
        } else if (packet instanceof PlayerMoveC2SPacket) {
            if (!teleportConfirmed) {
                actionSent = false;
                movementSent = true;
            }

            teleportConfirmed = false;
        } else if (packet instanceof ClientTickEndC2SPacket) {
            if (!movementSent) {
                actionSent = false;
            }

            movementSent = false;
        } else {
            if (isActionPacket(packet)) {
                actionSent = true;
            }
        }
    }

    public static boolean isTickClean() {
        return !actionSent && !movementSent;
    }

    private static boolean isActionPacket(Packet<?> packet) {
        return packet instanceof PlayerActionC2SPacket
            ? ((PlayerActionC2SPacket)packet).getAction() != Action.SWAP_ITEM_WITH_OFFHAND
            : packet instanceof PlayerInteractEntityC2SPacket
                || packet instanceof PlayerInteractBlockC2SPacket
                || packet instanceof PlayerInteractItemC2SPacket
                || packet instanceof PickItemFromBlockC2SPacket
                || packet instanceof PickItemFromEntityC2SPacket
                || packet instanceof ClientCommandC2SPacket;
    }
}
