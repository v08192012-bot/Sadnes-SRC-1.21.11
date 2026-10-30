package tech.sadnes.client.feature.impl.misc.farmbase;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.block.AbstractChestBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

final class StorageTracker {
    private final List<StorageEntry> entries = new ArrayList<>();
    private final List<StorageEntry> entriesView = Collections.unmodifiableList(this.entries);
    private final Set<BlockPos> openedPositions = new HashSet<>();
    private final Map<BlockPos, StorageType> rememberedStorages = new HashMap<>();
    private final Map<BlockPos, StorageType> pendingVerifications = new HashMap<>();
    private final LongOpenHashSet processedDoubleChests = new LongOpenHashSet();
    private int verifiedCount;
    private int missingCount;
    @Nullable
    private BlockPos pendingOpenPos;
    @Nullable
    private BlockPos pendingOpenNeighborPos;
    private int pendingOpenTick;

    List<StorageEntry> getEntries() {
        return this.entriesView;
    }

    boolean isOpened(BlockPos blockPos) {
        return this.openedPositions.contains(blockPos);
    }

    int getMissingCount() {
        return this.missingCount;
    }

    int getVerifiedCount() {
        return this.verifiedCount;
    }

    void reset() {
        this.clearEntries();
        this.openedPositions.clear();
        this.rememberedStorages.clear();
        this.pendingVerifications.clear();
        this.verifiedCount = 0;
        this.missingCount = 0;
        this.pendingOpenPos = null;
        this.pendingOpenNeighborPos = null;
    }

    void clearEntries() {
        this.entries.clear();
        this.processedDoubleChests.clear();
        this.pendingVerifications.clear();
    }

    void addPendingVerification(BlockPos blockPos, StorageType storageType) {
        this.pendingVerifications.put(blockPos, storageType);
    }

    void remember(BlockPos blockPos, StorageType storageType) {
        this.rememberedStorages.put(blockPos.toImmutable(), storageType);
    }

    void onInteract(ClientWorld clientWorld, BlockPos blockPos, int i) {
        this.pendingOpenPos = blockPos.toImmutable();
        this.pendingOpenNeighborPos = getDoubleChestNeighbor(clientWorld.getBlockState(blockPos), blockPos);
        this.pendingOpenTick = i;
    }

    void onScreenOpened(@Nullable StorageType storageType, int i) {
        if (this.pendingOpenPos != null && i - this.pendingOpenTick <= 20) {
            this.openedPositions.add(this.pendingOpenPos);
            if (this.pendingOpenNeighborPos != null) {
                this.openedPositions.add(this.pendingOpenNeighborPos);
            }

            if (storageType != null) {
                this.remember(this.pendingOpenPos, storageType);
            }
        }

        this.pendingOpenPos = null;
        this.pendingOpenNeighborPos = null;
    }

    void update(ClientWorld clientWorld, ChunkPos chunkPos, int i, Vec3d vec3d, double d, Predicate<StorageType> predicate) {
        this.entries.clear();
        this.processedDoubleChests.clear();
        this.verifyPending(clientWorld);
        double d2 = d * d;
        ClientChunkManager clientChunkManager = clientWorld.getChunkManager();

        for (int i2 = chunkPos.x - i; i2 <= chunkPos.x + i; i2++) {
            for (int i3 = chunkPos.z - i; i3 <= chunkPos.z + i; i3++) {
                WorldChunk worldChunk = clientChunkManager.getChunk(i2, i3, ChunkStatus.FULL, false);
                if (worldChunk != null) {
                    for (BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
                        StorageType storageType = StorageType.fromBlockEntity(blockEntity);
                        if (storageType != null && predicate.test(storageType)) {
                            BlockPos blockPos = blockEntity.getPos();
                            if (!(vec3d.squaredDistanceTo(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > d2)) {
                                Box box = this.computeBox(clientWorld, blockPos);
                                if (box != null) {
                                    this.entries.add(StorageEntry.of(blockPos, box, storageType));
                                }
                            }
                        }
                    }
                }
            }
        }

        this.addRememberedEntries(clientWorld, vec3d, d2, predicate);
        this.entries.sort(Comparator.comparingDouble(storageEntry -> vec3d.squaredDistanceTo(storageEntry.getBox().getCenter())));
        double d3 = d2 * 4.0;
        this.openedPositions.removeIf(blockPos2 -> vec3d.squaredDistanceTo(blockPos2.getX() + 0.5, blockPos2.getY() + 0.5, blockPos2.getZ() + 0.5) > d3);
    }

    private void verifyPending(ClientWorld clientWorld) {
        if (!this.pendingVerifications.isEmpty()) {
            ClientChunkManager clientChunkManager = clientWorld.getChunkManager();

            for (Entry entry : this.pendingVerifications.entrySet()) {
                BlockPos blockPos = (BlockPos)entry.getKey();
                int i = ChunkSectionPos.getSectionCoord(blockPos.getX());
                int i2 = ChunkSectionPos.getSectionCoord(blockPos.getZ());
                if (clientChunkManager.isChunkLoaded(i, i2)) {
                    BlockEntity blockEntity = clientWorld.getBlockEntity(blockPos);
                    if (blockEntity != null && StorageType.fromBlockEntity(blockEntity) == entry.getValue()) {
                        this.verifiedCount++;
                    } else {
                        this.missingCount++;
                        this.rememberedStorages.put(blockPos, (StorageType)entry.getValue());
                    }
                }
            }

            this.pendingVerifications.clear();
        }
    }

    private void addRememberedEntries(ClientWorld clientWorld, Vec3d vec3d, double d, Predicate<StorageType> predicate) {
        double d2 = d * 4.0;
        this.rememberedStorages.entrySet().removeIf(entry -> {
            BlockPos blockPos = entry.getKey();
            return vec3d.squaredDistanceTo(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > d2;
        });

        for (Entry entry2 : this.rememberedStorages.entrySet()) {
            BlockPos blockPos2 = (BlockPos)entry2.getKey();
            StorageType storageType = (StorageType)entry2.getValue();
            if (predicate.test(storageType) && !(vec3d.squaredDistanceTo(blockPos2.getX() + 0.5, blockPos2.getY() + 0.5, blockPos2.getZ() + 0.5) > d)) {
                BlockEntity blockEntity = clientWorld.getBlockEntity(blockPos2);
                if (blockEntity == null || StorageType.fromBlockEntity(blockEntity) == null) {
                    this.entries.add(StorageEntry.fromPacket(blockPos2, storageType));
                }
            }
        }
    }

    @Nullable
    private Box computeBox(ClientWorld clientWorld, BlockPos blockPos) {
        BlockState blockState = clientWorld.getBlockState(blockPos);
        if (!(blockState.getBlock() instanceof AbstractChestBlock)) {
            return new Box(blockPos);
        }

        Box box = new Box(blockPos);
        BlockPos blockPos2 = getDoubleChestNeighbor(blockState, blockPos);
        if (blockPos2 != null) {
            if (!this.processedDoubleChests.add(blockPos.asLong())) {
                return null;
            }

            this.processedDoubleChests.add(blockPos2.asLong());
            box = box.union(new Box(blockPos2));
        }

        return new Box(box.minX + 0.0625, box.minY, box.minZ + 0.0625, box.maxX - 0.0625, box.maxY - 0.0625 * 2.0, box.maxZ - 0.0625);
    }

    @Nullable
    private static BlockPos getDoubleChestNeighbor(BlockState blockState, BlockPos blockPos) {
        if (!blockState.contains(ChestBlock.CHEST_TYPE)) {
            return null;
        } else {
            return blockState.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE ? null : blockPos.offset(ChestBlock.getFacing(blockState));
        }
    }
}
