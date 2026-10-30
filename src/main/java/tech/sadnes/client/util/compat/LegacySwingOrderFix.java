package tech.sadnes.client.util.compat;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public final class LegacySwingOrderFix {
    private static final LegacySwingOrderFix.AttackDetector ATTACK_DETECTOR = new LegacySwingOrderFix.AttackDetector();
    private static boolean sendingSwing;
    private static boolean swingSent;
    private static long swingTimeNanos;

    private LegacySwingOrderFix() {
    }

    public static boolean shouldSwingFirst(Packet<?> packet) {
        if (!sendingSwing && packet instanceof PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
            ATTACK_DETECTOR.attack = false;
            playerInteractEntityC2SPacket.handle(ATTACK_DETECTOR);
            return ATTACK_DETECTOR.attack && ProtocolUtil.isLegacyServer();
        } else {
            return false;
        }
    }

    public static Packet<?> createSwingPacket() {
        sendingSwing = true;
        return new HandSwingC2SPacket(Hand.MAIN_HAND);
    }

    public static void onSwingSent() {
        sendingSwing = false;
        swingSent = true;
        swingTimeNanos = System.nanoTime();
    }

    public static boolean isDuplicateSwing(Packet<?> packet) {
        if (swingSent && !sendingSwing && packet instanceof HandSwingC2SPacket handSwingC2SPacket) {
            swingSent = false;
            boolean flag = System.nanoTime() - swingTimeNanos < 20000000L;
            return flag && handSwingC2SPacket.getHand() == Hand.MAIN_HAND;
        } else {
            return false;
        }
    }

    static final class AttackDetector implements Handler {
        boolean attack;

        @Override
        public void interact(Hand hand) {
        }

        @Override
        public void interactAt(Hand hand, Vec3d vec3d) {
        }

        @Override
        public void attack() {
            this.attack = true;
        }
    }
}
