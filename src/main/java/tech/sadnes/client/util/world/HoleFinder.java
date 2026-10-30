package tech.sadnes.client.util.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import tech.sadnes.client.util.IMinecraft;

public final class HoleFinder implements IMinecraft {
    private final Long2ObjectOpenHashMap<HoleFinder.Region> regions = new Long2ObjectOpenHashMap<>();
    private final Mutable mutablePos = new Mutable();

    public static List<BlockPos> findHoles(BlockPos blockPos, BlockPos blockPos2) {
        ArrayList arrayList = new ArrayList();
        if (mc.world == null) {
            return arrayList;
        }

        HoleFinder holeFinder = new HoleFinder();

        for (BlockPos blockPos3 : BlockPos.iterate(blockPos, blockPos2)) {
            if (holeFinder.isSafeHole(blockPos3.getX(), blockPos3.getY(), blockPos3.getZ())) {
                arrayList.add(blockPos3.toImmutable());
            }
        }

        return arrayList;
    }

    public static boolean isHole(BlockPos blockPos) {
        return !findHoles(blockPos, blockPos).isEmpty();
    }

    public static List<BlockPos> getRegionHoles(BlockPos blockPos) {
        ArrayList arrayList = new ArrayList();
        if (mc.world == null) {
            return arrayList;
        }

        HoleFinder holeFinder = new HoleFinder();
        HoleFinder.Region region = holeFinder.getRegion(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        if (region == null) {
            return arrayList;
        }

        int i = region.positions.size();

        for (int i2 = 0; i2 < i; i2++) {
            int i3 = BlockPos.unpackLongX(region.positions.getLong(i2));
            int i4 = BlockPos.unpackLongZ(region.positions.getLong(i2));
            if (holeFinder.isSafeHole(i3, region.y, i4)) {
                arrayList.add(new BlockPos(i3, region.y, i4));
            }
        }

        return arrayList;
    }

    public static BlockPos findBestPos(List<BlockPos> blockPoses, BlockPos blockPos) {
        BlockPos blockPos2 = blockPos;
        int i = 0;

        for (BlockPos blockPos3 : blockPoses) {
            int i2 = 0;

            for (BlockPos blockPos4 : blockPoses) {
                if (Math.abs(blockPos4.getX() - blockPos3.getX()) <= 1 && Math.abs(blockPos4.getZ() - blockPos3.getZ()) <= 1) {
                    i2++;
                }
            }

            boolean flag = i2 > i;
            if (i2 == i && blockPos3.getSquaredDistance(blockPos) < blockPos2.getSquaredDistance(blockPos)) {
                i = i2;
                blockPos2 = blockPos3;
            } else if (flag) {
                i = i2;
                blockPos2 = blockPos3;
            }
        }

        return blockPos2;
    }

    private boolean isSafeHole(int i, int i2, int i3) {
        HoleFinder.Region region = this.getRegion(i, i2, i3);
        boolean flag;
        if (region != null && this.isEnclosed(region) && this.isWalled(region)) {
            flag = region.hasOpening;
        } else {
            flag = false;
        }

        return flag && this.isStandable(i, i2, i3);
    }

    private HoleFinder.Region getRegion(int i, int i2, int i3) {
        long l = BlockPos.asLong(i, i2, i3);
        HoleFinder.Region region = this.regions.get(l);
        if (region != null) {
            return region;
        }

        if (this.isEmpty(i, i2, i3) && this.isEmpty(i, i2 + 1, i3) && !this.isEmpty(i, i2 - 1, i3)) {
            HoleFinder.Region region2 = new HoleFinder.Region(i2);
            region2.positions.add(l);

            for (int i4 = 0; region2.valid && i4 < region2.positions.size(); i4++) {
                int i5 = BlockPos.unpackLongX(region2.positions.getLong(i4));
                int i6 = BlockPos.unpackLongZ(region2.positions.getLong(i4));
                region2.valid = region2.positions.size() <= 48
                    && this.expand(region2, i5 + 1, i6)
                    && this.expand(region2, i5 - 1, i6)
                    && this.expand(region2, i5, i6 + 1)
                    && this.expand(region2, i5, i6 - 1);
            }

            int i7 = region2.positions.size();

            for (int i8 = 0; i8 < i7; i8++) {
                this.regions.put(region2.positions.getLong(i8), region2);
            }

            return region2;
        } else {
            return null;
        }
    }

    private boolean expand(HoleFinder.Region region, int i, int i2) {
        long l = BlockPos.asLong(i, region.y, i2);
        if (region.positions.contains(l)) {
            return true;
        }

        if (this.regions.containsKey(l)) {
            return false;
        }

        if (this.isEmpty(i, region.y, i2) && this.isEmpty(i, region.y + 1, i2)) {
            for (int i3 = 0; i3 <= 3; i3++) {
                if (!this.isEmpty(i, region.y - i3 - 1, i2)) {
                    if (i3 == 0) {
                        region.positions.add(l);
                        return true;
                    }

                    region.drops.add(BlockPos.asLong(i, region.y - i3, i2));
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    private boolean isEnclosed(HoleFinder.Region region) {
        if (region.enclosedCache != null) {
            return region.enclosedCache;
        }

        boolean flag = region.valid;
        int i = region.drops.size();

        for (int i2 = 0; flag && i2 < i; i2++) {
            flag = this.isEnclosed(this.getRegionAt(region.drops.getLong(i2)));
        }

        region.enclosedCache = flag;
        return flag;
    }

    private boolean isWalled(HoleFinder.Region region) {
        if (region.walledCache != null) {
            return region.walledCache;
        }

        boolean flag = true;
        int i = region.positions.size();

        for (int i2 = 0; flag && i2 < i; i2++) {
            int i3 = BlockPos.unpackLongX(region.positions.getLong(i2));
            int i4 = BlockPos.unpackLongZ(region.positions.getLong(i2));
            if (this.checkSide(region, i3, i4, 1, 0) && this.checkSide(region, i3, i4, -1, 0) && this.checkSide(region, i3, i4, 0, 1)) {
                flag = this.checkSide(region, i3, i4, 0, -1);
            } else {
                flag = false;
            }
        }

        int i5 = region.drops.size();

        for (int i6 = 0; flag && i6 < i5; i6++) {
            flag = this.isWalled(this.getRegionAt(region.drops.getLong(i6)));
        }

        region.walledCache = flag;
        return flag;
    }

    private boolean checkSide(HoleFinder.Region region, int i, int i2, int i3, int i4) {
        if (!this.isEmpty(i + i3, region.y, i2 + i4)) {
            region.hasOpening = true;
            int i5 = i + i3 * 2;
            int i6 = i2 + i4 * 2;
            if (!this.isEmpty(i5, region.y, i6)) {
                return true;
            }

            for (int i7 = 0; i7 <= 3; i7++) {
                if (!this.isEmpty(i5, region.y - i7 - 1, i6)) {
                    HoleFinder.Region region2 = this.getRegion(i5, region.y - i7, i6);
                    if (region2 != null && this.isEnclosed(region2)) {
                        return true;
                    }

                    return false;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    private HoleFinder.Region getRegionAt(long l) {
        return this.getRegion(BlockPos.unpackLongX(l), BlockPos.unpackLongY(l), BlockPos.unpackLongZ(l));
    }

    private boolean isStandable(int i, int i2, int i3) {
        return this.isEmptySafe(i, i2, i3) && this.isEmptySafe(i, i2 + 1, i3) && !this.isEmpty(i, i2 - 1, i3);
    }

    private boolean isEmptySafe(int i, int i2, int i3) {
        BlockState blockState = mc.world.getBlockState(this.mutablePos.set(i, i2, i3));
        return blockState.getCollisionShape(mc.world, this.mutablePos).isEmpty() && blockState.getFluidState().isEmpty() && !blockState.isIn(BlockTags.FIRE);
    }

    private boolean isEmpty(int i, int i2, int i3) {
        return mc.world.getBlockState(this.mutablePos.set(i, i2, i3)).getCollisionShape(mc.world, this.mutablePos).isEmpty();
    }

    private HoleFinder() {
    }

    static final class Region {
        final int y;
        final LongArrayList positions = new LongArrayList();
        final LongArrayList drops = new LongArrayList();
        boolean valid = true;
        boolean hasOpening;
        Boolean enclosedCache;
        Boolean walledCache;

        public Region(int i) {
            this.y = i;
        }
    }
}
