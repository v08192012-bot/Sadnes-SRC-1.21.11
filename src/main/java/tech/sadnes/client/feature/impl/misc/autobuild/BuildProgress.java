package tech.sadnes.client.feature.impl.misc.autobuild;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.chunk.WorldChunk;

public final class BuildProgress {
    static final long NO_CELL = Long.MIN_VALUE;
    private static final int[] SPHERE_OFFSETS = buildSphereOffsets();
    private final FilteredSchematic schematic;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final int widthX;
    private final int heightY;
    private final int lengthZ;
    private final BitSet matched;
    private final Mutable mutablePos = new Mutable();
    private final LongArrayList mismatchedNearby = new LongArrayList();
    private final LongList mismatchedNearbyView = LongLists.unmodifiable(this.mismatchedNearby);
    private final LongOpenHashSet unsupported = new LongOpenHashSet();
    private final LongOpenHashSet postponed = new LongOpenHashSet();
    private final Long2IntOpenHashMap failCounts = new Long2IntOpenHashMap();
    private Reference2IntOpenHashMap<Item> missingItems = new Reference2IntOpenHashMap<>();
    private Reference2IntOpenHashMap<Item> pendingMissingItems = new Reference2IntOpenHashMap<>();
    private int scanIndex;
    private int scanTotal;
    private int scanRemaining;
    private int scanManual;
    private int tickCounter;
    private int total;
    private int remaining;
    private int manual;
    private boolean scanned;
    private int obstacleCount;

    public BuildProgress(FilteredSchematic filteredSchematic, Vec3i vec3i) {
        this.schematic = filteredSchematic;
        this.originX = vec3i.getX();
        this.originY = vec3i.getY();
        this.originZ = vec3i.getZ();
        this.widthX = filteredSchematic.widthX();
        this.heightY = filteredSchematic.heightY();
        this.lengthZ = filteredSchematic.lengthZ();
        this.matched = new BitSet(this.widthX * this.heightY * this.lengthZ);
    }

    public void tick(ClientWorld clientWorld, BlockPos blockPos) {
        if (this.tickCounter++ % 10 == 0) {
            this.scanNearby(clientWorld, blockPos);
        }

        int i = this.widthX * this.lengthZ;

        for (int i2 = this.scanned ? 8192 : 65536; i2 > 0 && this.scanIndex < i; i2 -= this.heightY) {
            this.scanColumn(clientWorld, this.scanIndex / this.lengthZ, this.scanIndex % this.lengthZ);
            this.scanIndex++;
        }

        if (this.scanIndex >= i) {
            this.finishScan();
        }
    }

    public void restartScan() {
        this.pendingMissingItems.clear();
        this.scanTotal = this.scanRemaining = this.scanManual = 0;
        this.scanIndex = 0;
        this.scanned = false;
    }

    public int getPercent() {
        return this.total == 0 ? 100 : (int)((this.total - this.remaining) * 100L / this.total);
    }

    public List<BuildProgress.MaterialCount> getMaterials(List<ItemStack> itemStacks, int i) {
        Reference2IntOpenHashMap reference2IntOpenHashMap = new Reference2IntOpenHashMap();

        for (int i2 = 0; i2 < i; i2++) {
            reference2IntOpenHashMap.addTo(((ItemStack)itemStacks.get(i2)).getItem(), ((ItemStack)itemStacks.get(i2)).getCount());
        }

        ArrayList arrayList = new ArrayList(this.missingItems.size());

        for (Entry entry : this.missingItems.reference2IntEntrySet()) {
            arrayList.add(new BuildProgress.MaterialCount((Item)entry.getKey(), entry.getIntValue(), reference2IntOpenHashMap.getInt(entry.getKey())));
        }

        arrayList.sort(Comparator.comparingInt(BuildProgress.MaterialCount::missing).reversed());
        return arrayList;
    }

    public boolean hasMissingNearby(List<ItemStack> itemStacks, int i, ClientPlayerEntity clientPlayerEntity, double d) {
        BlockPos blockPos = clientPlayerEntity.getBlockPos();
        Vec3d vec3d = clientPlayerEntity.getEyePos();
        ReferenceOpenHashSet referenceOpenHashSet = new ReferenceOpenHashSet();

        for (int i2 = 0; i2 < i; i2++) {
            referenceOpenHashSet.add(((ItemStack)itemStacks.get(i2)).getItem());
        }

        int i3 = this.mismatchedNearby.size();

        for (int i4 = 0; i4 < i3; i4++) {
            long l = this.mismatchedNearby.getLong(i4);
            int i5 = BlockPos.unpackLongX(l);
            int i6 = BlockPos.unpackLongY(l);
            int i7 = BlockPos.unpackLongZ(l);
            int i8 = i6 - blockPos.getY();
            boolean flag = Math.abs(i5 - blockPos.getX()) <= 5 && Math.abs(i7 - blockPos.getZ()) <= 5 && i8 >= -5 && i8 <= 1;
            if (flag && isInReach(vec3d, d, i5, i6, i7)) {
                int i9 = i5 - this.originX;
                int i10 = i6 - this.originY;
                int i11 = i7 - this.originZ;
                BlockState blockState = this.schematic.getTargetState(i9, i10, i11);
                boolean flag2;
                if (blockState != null && this.schematic.shouldBuild(i9, i10, i11, blockState)) {
                    flag2 = !this.schematic.isPostponed(BlockPos.asLong(i9, i10, i11));
                } else {
                    flag2 = false;
                }

                if (flag2 && !referenceOpenHashSet.contains(BuildBlockUtil.getItem(blockState))) {
                    return true;
                }
            }
        }

        return false;
    }

    public int pruneUnsupported(ClientWorld clientWorld) {
        LongArrayList longArrayList = new LongArrayList();
        LongIterator longIterator = this.unsupported.iterator();

        while (longIterator.hasNext()) {
            long l = longIterator.nextLong();
            int i = BlockPos.unpackLongX(l);
            int i2 = BlockPos.unpackLongY(l);
            int i3 = BlockPos.unpackLongZ(l);
            this.mutablePos.set(this.originX + i, this.originY + i2, this.originZ + i3);
            boolean flag = clientWorld.getChunkManager().getWorldChunk(this.mutablePos.getX() >> 4, this.mutablePos.getZ() >> 4) != null;
            BlockState blockState = this.schematic.getTargetState(i, i2, i3);
            boolean flag2 = !this.matched.get((i * this.lengthZ + i3) * this.heightY + i2);
            boolean flag3;
            if (blockState != null && this.schematic.shouldBuild(i, i2, i3, blockState) && flag2) {
                flag3 = !blockState.canPlaceAt(clientWorld, this.mutablePos);
            } else {
                flag3 = false;
            }

            if (!flag) {
                longArrayList.add(l);
            } else if (!flag3) {
                longIterator.remove();
            }
        }

        LongOpenHashSet longOpenHashSet = new LongOpenHashSet(this.unsupported);

        for (long l2 : this.unsupported) {
            if (this.hasUnmatchedNeighbor(l2)) {
                longArrayList.add(l2);
            }
        }

        while (!longArrayList.isEmpty()) {
            long l3 = longArrayList.removeLong(longArrayList.size() - 1);
            if (longOpenHashSet.remove(l3)) {
                for (Direction direction : Direction.values()) {
                    long l4 = BlockPos.offset(l3, direction);
                    if (longOpenHashSet.contains(l4)) {
                        longArrayList.add(l4);
                    }
                }
            }
        }

        if (!longOpenHashSet.isEmpty()) {
            this.schematic.skipPositions(longOpenHashSet);
        }

        this.unsupported.removeAll(longOpenHashSet);
        this.syncIgnored();
        return longOpenHashSet.size();
    }

    private boolean hasUnmatchedNeighbor(long l) {
        int i = BlockPos.unpackLongX(l);
        int i2 = BlockPos.unpackLongY(l);
        int i3 = BlockPos.unpackLongZ(l);

        for (Direction direction : Direction.values()) {
            int i4 = i + direction.getOffsetX();
            int i5 = i2 + direction.getOffsetY();
            int i6 = i3 + direction.getOffsetZ();
            BlockState blockState = this.schematic.getTargetState(i4, i5, i6);
            boolean flag;
            if (blockState != null && !blockState.isAir()) {
                flag = this.schematic.shouldBuild(i4, i5, i6, blockState);
            } else {
                flag = false;
            }

            boolean flag2;
            if (flag && !this.matched.get((i4 * this.lengthZ + i6) * this.heightY + i5)) {
                flag2 = !this.unsupported.contains(BlockPos.asLong(i4, i5, i6));
            } else {
                flag2 = false;
            }

            if (flag2) {
                return true;
            }
        }

        return false;
    }

    public void postpone(LongList longList) {
        this.postponed.addAll(longList);
        this.syncIgnored();
    }

    public int postponeOrSkip(LongList longList) {
        LongArrayList longArrayList = new LongArrayList();
        int i = longList.size();

        for (int i2 = 0; i2 < i; i2++) {
            long l = longList.getLong(i2);
            boolean flag = this.failCounts.addTo(l, 1) >= 2;
            if (flag) {
                longArrayList.add(l);
            } else {
                this.postponed.add(l);
            }
        }

        if (!longArrayList.isEmpty()) {
            this.schematic.skipPositions(longArrayList);
        }

        this.syncIgnored();
        return longArrayList.size();
    }

    public void clearPostponed() {
        this.postponed.clear();
        this.syncIgnored();
    }

    public int getUnsupportedCount() {
        return this.unsupported.size();
    }

    public int getPostponedCount() {
        return this.postponed.size();
    }

    public long toSolidCell(BlockPos blockPos) {
        BlockState blockState = this.schematic.getTargetState(blockPos.getX() - this.originX, blockPos.getY() - this.originY, blockPos.getZ() - this.originZ);
        return blockState != null && !blockState.isAir() ? this.toCell(blockPos) : Long.MIN_VALUE;
    }

    public long toCell(BlockPos blockPos) {
        int i = blockPos.getX() - this.originX;
        int i2 = blockPos.getY() - this.originY;
        int i3 = blockPos.getZ() - this.originZ;
        BlockState blockState = this.schematic.getTargetState(i, i2, i3);
        return blockState != null && this.schematic.shouldBuild(i, i2, i3, blockState) ? BlockPos.asLong(i, i2, i3) : Long.MIN_VALUE;
    }

    public long findReplaceableNearby(ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, double d) {
        BlockPos blockPos = clientPlayerEntity.getBlockPos();
        Vec3d vec3d = clientPlayerEntity.getEyePos();

        for (int i = -5; i <= 5; i++) {
            for (int i2 = -5; i2 <= 1; i2++) {
                for (int i3 = -5; i3 <= 5; i3++) {
                    this.mutablePos.set(blockPos.getX() + i, blockPos.getY() + i2, blockPos.getZ() + i3);
                    boolean flag;
                    if (isInReach(vec3d, d, this.mutablePos.getX(), this.mutablePos.getY(), this.mutablePos.getZ())) {
                        flag = clientWorld.getBlockState(this.mutablePos).isReplaceable();
                    } else {
                        flag = false;
                    }

                    long l = flag ? this.toSolidCell(this.mutablePos) : Long.MIN_VALUE;
                    boolean flag2;
                    if (l != Long.MIN_VALUE) {
                        flag2 = !this.schematic.isPostponed(l);
                    } else {
                        flag2 = false;
                    }

                    if (flag2) {
                        return l;
                    }
                }
            }
        }

        return Long.MIN_VALUE;
    }

    private static boolean isInReach(Vec3d vec3d, double d, int i, int i2, int i3) {
        return vec3d.squaredDistanceTo(i + 0.5, i2 + 0.5, i3 + 0.5) <= (d + 1.0) * (d + 1.0);
    }

    public void skipCell(long l) {
        this.schematic.skipPositions(LongList.of(l));
    }

    public LongList getMismatchedNearby() {
        return this.mismatchedNearbyView;
    }

    private void scanColumn(ClientWorld clientWorld, int i, int i2) {
        int i3 = this.originX + i;
        int i4 = this.originZ + i2;
        WorldChunk worldChunk = clientWorld.getChunkManager().getWorldChunk(i3 >> 4, i4 >> 4);
        int i5 = (i * this.lengthZ + i2) * this.heightY;

        for (int i6 = 0; i6 < this.heightY; i5++) {
            BlockState blockState = this.schematic.getTargetState(i, i6, i2);
            if (blockState != null) {
                boolean flag = this.schematic.shouldBuild(i, i6, i2, blockState);
                if (!blockState.isAir() || flag) {
                    if (worldChunk != null) {
                        this.matched.set(i5, this.matches(blockState, flag, worldChunk.getBlockState(this.mutablePos.set(i3, this.originY + i6, i4))));
                    }

                    boolean flag2 = this.matched.get(i5);
                    if (!flag) {
                        if (!flag2) {
                            this.scanManual++;
                        }
                    } else {
                        this.scanTotal++;
                        if (!flag2) {
                            this.scanRemaining++;
                            if (!blockState.isAir()) {
                                this.pendingMissingItems.addTo(BuildBlockUtil.getItem(blockState), 1);
                            }
                        }
                    }
                }
            }

            i6++;
        }
    }

    private boolean matches(BlockState blockState, boolean flag, BlockState blockState2) {
        if (blockState.isAir()) {
            return blockState2.isAir() || blockState2.getBlock() instanceof FluidBlock;
        } else {
            return blockState2.getBlock() == blockState.getBlock() ? true : flag && this.schematic.isKeepExisting() && !blockState2.isReplaceable();
        }
    }

    private void finishScan() {
        Reference2IntOpenHashMap reference2IntOpenHashMap = this.pendingMissingItems;
        this.pendingMissingItems = this.missingItems;
        this.pendingMissingItems.clear();
        this.missingItems = reference2IntOpenHashMap;
        this.total = this.scanTotal;
        this.remaining = this.scanRemaining;
        this.manual = this.scanManual;
        this.scanTotal = this.scanRemaining = this.scanManual = 0;
        this.scanIndex = 0;
        this.scanned = true;
    }

    private void scanNearby(ClientWorld clientWorld, BlockPos blockPos) {
        this.mismatchedNearby.clear();
        this.obstacleCount = 0;
        boolean flag = false;
        int i = blockPos.getX() - this.originX;
        int i2 = blockPos.getY() - this.originY;
        int i3 = blockPos.getZ() - this.originZ;
        int i4 = blockPos.getX() - 12 >> 4;
        int i5 = blockPos.getZ() - 12 >> 4;
        WorldChunk[] worldChunks = new WorldChunk[9];

        for (int i6 = 0; i6 < 9; i6++) {
            worldChunks[i6] = clientWorld.getChunkManager().getWorldChunk(i4 + i6 / 3, i5 + i6 % 3);
        }

        for (int i7 : SPHERE_OFFSETS) {
            int i8 = i + (i7 >> 10) - 12;
            int i9 = i2 + (i7 >> 5 & 31) - 12;
            int i10 = i3 + (i7 & 31) - 12;
            if (i8 >= 0 && i8 < this.widthX && i9 >= 0 && i9 < this.heightY && i10 >= 0 && i10 < this.lengthZ) {
                BlockState blockState = this.schematic.getTargetState(i8, i9, i10);
                if (blockState != null) {
                    boolean flag2 = this.schematic.shouldBuild(i8, i9, i10, blockState);
                    if (!blockState.isAir() || flag2) {
                        int i11 = this.originX + i8;
                        int i12 = this.originZ + i10;
                        WorldChunk worldChunk = worldChunks[((i11 >> 4) - i4) * 3 + (i12 >> 4) - i5];
                        if (worldChunk != null) {
                            BlockState blockState2 = worldChunk.getBlockState(this.mutablePos.set(i11, this.originY + i9, i12));
                            if (flag2 && this.schematic.needsClearing(blockState, blockState2)) {
                                this.obstacleCount++;
                            }

                            boolean flag3;
                            if (!blockState.isAir()) {
                                flag3 = !this.matches(blockState, flag2, blockState2);
                            } else {
                                flag3 = false;
                            }

                            boolean flag4;
                            if (flag3 && flag2) {
                                flag4 = !blockState.canPlaceAt(clientWorld, this.mutablePos);
                            } else {
                                flag4 = false;
                            }

                            long l = BlockPos.asLong(i8, i9, i10);
                            flag |= flag4 ? this.unsupported.add(l) : this.unsupported.remove(l);
                            if (flag3) {
                                this.mismatchedNearby.add(BlockPos.asLong(i11, this.originY + i9, i12));
                            }
                        }
                    }
                }
            }
        }

        if (flag) {
            this.syncIgnored();
        }
    }

    private void syncIgnored() {
        LongOpenHashSet longOpenHashSet = new LongOpenHashSet(this.unsupported);
        longOpenHashSet.addAll(this.postponed);
        this.schematic.setPostponedPositions(longOpenHashSet);
    }

    private static int[] buildSphereOffsets() {
        long[] longs = new long[15625];
        int i = 0;

        for (int i2 = -12; i2 <= 12; i2++) {
            for (int i3 = -12; i3 <= 12; i3++) {
                for (int i4 = -12; i4 <= 12; i4++) {
                    longs[i++] = (long)(i2 * i2 + i3 * i3 + i4 * i4) << 32 | i2 + 12 << 10 | i3 + 12 << 5 | i4 + 12;
                }
            }
        }

        Arrays.sort(longs);
        int[] ints = new int[i];

        for (int i5 = 0; i5 < i; i5++) {
            ints[i5] = (int)longs[i5];
        }

        return ints;
    }

    public int getTotal() {
        return this.total;
    }

    public int getRemaining() {
        return this.remaining;
    }

    public int getManual() {
        return this.manual;
    }

    public boolean isScanned() {
        return this.scanned;
    }

    public int getObstacleCount() {
        return this.obstacleCount;
    }

    public record MaterialCount(Item item, int need, int have) {
        public int missing() {
            return Math.max(this.need - this.have, 0);
        }
    }
}
