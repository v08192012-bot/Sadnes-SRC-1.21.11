package tech.sadnes.client.util.player;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;

public final class BubbleColumnUtil {
    private static final Mutable MUTABLE_POS = new Mutable();
    private static int lastAge = -1;

    private BubbleColumnUtil() {
    }

    public static void applyBubbleColumns(Entity entity) {
        if (entity.age != lastAge) {
            lastAge = entity.age;
            World world = entity.getEntityWorld();
            Box box = entity.getBoundingBox().contract(1.0E-5);
            int i = MathHelper.floor(box.minX);
            int i2 = MathHelper.floor(box.minY);
            int i3 = MathHelper.floor(box.minZ);
            int i4 = MathHelper.floor(box.maxX);
            int i5 = MathHelper.floor(box.maxY);
            int i6 = MathHelper.floor(box.maxZ);

            for (int i7 = i; i7 <= i4; i7++) {
                for (int i8 = i2; i8 <= i5; i8++) {
                    for (int i9 = i3; i9 <= i6; i9++) {
                        BlockState blockState = world.getBlockState(MUTABLE_POS.set(i7, i8, i9));
                        if (blockState.isOf(Blocks.BUBBLE_COLUMN)) {
                            boolean flag = blockState.get(BubbleColumnBlock.DRAG);
                            boolean flag2 = world.getBlockState(MUTABLE_POS.set(i7, i8 + 1, i9)).isAir();
                            Vec3d vec3d = entity.getVelocity();
                            double d = flag2
                                ? (flag ? Math.max(-0.9, vec3d.y - 0.03) : Math.min(1.8, vec3d.y + 0.1))
                                : (flag ? Math.max(-0.3, vec3d.y - 0.03) : Math.min(0.7, vec3d.y + 0.06));
                            entity.setVelocity(vec3d.x, d, vec3d.z);
                            entity.fallDistance = 0.0;
                        }
                    }
                }
            }
        }
    }
}
