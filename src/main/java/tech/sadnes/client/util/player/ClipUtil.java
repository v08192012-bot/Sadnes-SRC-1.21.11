package tech.sadnes.client.util.player;

import java.util.OptionalInt;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.util.IMinecraft;

public final class ClipUtil implements IMinecraft {
    private static final int MAX_SEARCH_DISTANCE = 384;

    private ClipUtil() {
    }

    public static void teleport(double d, double d2, double d3) {
        if (mc.player != null && mc.player.networkHandler != null) {
            boolean flag = mc.player.horizontalCollision;
            sendPositionBurst(d, d2, d3, flag);
            sendRotationPacket(flag);
            sendPositionBurst(d, d2, d3, flag);
            sendRotationPacket(flag);
            sendPositionBurst(d, d2, d3, flag);
        }
    }

    private static void sendPositionBurst(double d, double d2, double d3, boolean flag) {
        for (int i = 0; i <= 20; i++) {
            mc.player.networkHandler.sendPacket(new PositionAndOnGround(d, d2, d3, true, flag));
        }
    }

    private static void sendRotationPacket(boolean flag) {
        mc.player.networkHandler.sendPacket(new Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), -180.0F, 0.0F, false, flag));
    }

    public static OptionalInt findSurfaceY(double d, double d2, double d3, boolean flag) {
        if (mc.world != null && mc.player != null) {
            int i = MathHelper.floor(d2);
            int i2 = flag ? 1 : -1;

            for (int i3 = 1; i3 <= 384; i3++) {
                int i4 = i + i2 * i3;
                if (canStandAt(d, i4, d3)) {
                    return OptionalInt.of(i4);
                }
            }
        }

        return OptionalInt.empty();
    }

    private static boolean canStandAt(double d, int i, double d2) {
        Box box = mc.player.getBoundingBox().offset(d - mc.player.getX(), i - mc.player.getY(), d2 - mc.player.getZ());
        if (!mc.world.isSpaceEmpty(mc.player, box)) {
            return false;
        }

        BlockPos blockPos = BlockPos.ofFloored(d, i - 1, d2);
        return !mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
    }

    public static double parseDouble(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException(string + " не является числом.");
        }
    }
}
