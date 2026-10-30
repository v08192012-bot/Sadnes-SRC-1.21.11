package tech.sadnes.client.feature.impl.misc.autobuild;

import java.util.List;
import java.util.Set;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;

public final class BuildBlockUtil {
    private static final Set<Block> IGNORED_BLOCKS = Set.of(
        Blocks.NETHER_PORTAL,
        Blocks.END_PORTAL,
        Blocks.END_GATEWAY,
        Blocks.MOVING_PISTON,
        Blocks.PISTON_HEAD,
        Blocks.BUBBLE_COLUMN,
        Blocks.FROSTED_ICE,
        Blocks.BAMBOO,
        Blocks.KELP,
        Blocks.SEAGRASS,
        Blocks.VINE,
        Blocks.LILY_PAD,
        Blocks.FROGSPAWN,
        Blocks.FARMLAND,
        Blocks.DIRT_PATH
    );
    private static final List<Property<?>> DIRECTIONAL_PROPERTIES = List.of(
        Properties.FACING,
        Properties.HORIZONTAL_FACING,
        Properties.HOPPER_FACING,
        Properties.ROTATION,
        Properties.BLOCK_HALF,
        Properties.BLOCK_FACE,
        Properties.ORIENTATION,
        Properties.DOOR_HINGE,
        Properties.BED_PART,
        Properties.VERTICAL_DIRECTION
    );

    public static Item getItem(BlockState blockState) {
        Item item = blockState.getBlock().asItem();
        ClientWorld clientWorld = MinecraftClient.getInstance().world;
        return item == Items.AIR && clientWorld != null ? blockState.getPickStack(clientWorld, BlockPos.ORIGIN, false).getItem() : item;
    }

    public static boolean isPlaceable(BlockState blockState) {
        Block block = blockState.getBlock();
        if (!blockState.isAir() && !(block instanceof FluidBlock) && !(block instanceof AbstractFireBlock) && !IGNORED_BLOCKS.contains(block)) {
            boolean flag = blockState.contains(Properties.DOUBLE_BLOCK_HALF) && blockState.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
            if (blockState.contains(Properties.BED_PART) && blockState.get(Properties.BED_PART) == BedPart.HEAD) {
                return false;
            }

            if (!flag) {
                Item item = block.asItem();
                if (item instanceof BlockItem && ((BlockItem)item).getBlock() == block) {
                    return true;
                }

                return false;
            }
        }

        return false;
    }

    public static boolean isDirectional(BlockState blockState) {
        boolean flag = blockState.contains(Properties.SLAB_TYPE) && blockState.get(Properties.SLAB_TYPE) != SlabType.BOTTOM;
        if (flag) {
            return true;
        }

        for (Property property : DIRECTIONAL_PROPERTIES) {
            if (blockState.contains(property)) {
                return true;
            }
        }

        return false;
    }

    public static BuildBlockUtil.BlockKind getKind(BlockState blockState) {
        return BuildBlockUtil.KindCache.KINDS[Block.getRawIdFromState(blockState)];
    }

    static BuildBlockUtil.BlockKind classify(BlockState blockState) {
        if (blockState == null || blockState.isAir()) {
            return BuildBlockUtil.BlockKind.AIR;
        }

        if (!isPlaceable(blockState)) {
            return BuildBlockUtil.BlockKind.MANUAL;
        }

        if (isDirectional(blockState)) {
            return BuildBlockUtil.BlockKind.DIRECTIONAL;
        }

        boolean flag;
        if (!blockState.contains(Properties.AXIS)) {
            flag = blockState.contains(Properties.HORIZONTAL_AXIS);
        } else {
            flag = true;
        }

        return flag ? BuildBlockUtil.BlockKind.AXIS : BuildBlockUtil.BlockKind.BLOCK;
    }

    private BuildBlockUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public enum BlockKind {
        AIR,
        BLOCK,
        AXIS,
        DIRECTIONAL,
        MANUAL;
    }

    static final class KindCache {
        static final BuildBlockUtil.BlockKind[] KINDS = buildKinds();

        private KindCache() {
        }

        private static BuildBlockUtil.BlockKind[] buildKinds() {
            int i = Block.STATE_IDS.size();
            BuildBlockUtil.BlockKind[] blockKinds = new BuildBlockUtil.BlockKind[i];

            for (int i2 = 0; i2 < i; i2++) {
                blockKinds[i2] = BuildBlockUtil.classify(Block.STATE_IDS.get(i2));
            }

            return blockKinds;
        }
    }
}
