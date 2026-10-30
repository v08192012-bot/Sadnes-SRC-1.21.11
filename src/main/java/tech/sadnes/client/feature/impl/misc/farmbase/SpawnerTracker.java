package tech.sadnes.client.feature.impl.misc.farmbase;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.spawner.MobSpawnerEntry;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import tech.sadnes.api.mixin.accessor.MobSpawnerLogicAccessor;

final class SpawnerTracker {
    private final Map<BlockPos, TrackedSpawner> spawners = new HashMap<>();
    private final Set<BlockPos> seenSpawnerPositions = new HashSet<>();

    Collection<TrackedSpawner> getSpawners() {
        return Collections.unmodifiableCollection(this.spawners.values());
    }

    @Nullable
    TrackedSpawner getSpawner(BlockPos blockPos) {
        return this.spawners.get(blockPos);
    }

    void clear() {
        this.spawners.clear();
    }

    void scanChunks(ClientWorld clientWorld, ChunkPos chunkPos, int i) {
        ClientChunkManager clientChunkManager = clientWorld.getChunkManager();
        this.seenSpawnerPositions.clear();

        for (int i2 = chunkPos.x - i; i2 <= chunkPos.x + i; i2++) {
            for (int i3 = chunkPos.z - i; i3 <= chunkPos.z + i; i3++) {
                WorldChunk worldChunk = clientChunkManager.getChunk(i2, i3, ChunkStatus.FULL, false);
                if (worldChunk != null) {
                    for (BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
                        if (blockEntity instanceof MobSpawnerBlockEntity mobSpawnerBlockEntity) {
                            TrackedSpawner trackedSpawner = this.readSpawner(clientWorld, mobSpawnerBlockEntity);
                            if (trackedSpawner != null) {
                                this.seenSpawnerPositions.add(trackedSpawner.getPos());
                            }
                        }
                    }
                }
            }
        }

        this.spawners.values().removeIf(trackedSpawner2 -> {
            int i4 = ChunkSectionPos.getSectionCoord(trackedSpawner2.getPos().getX());
            int i5 = ChunkSectionPos.getSectionCoord(trackedSpawner2.getPos().getZ());
            boolean flag;
            if (Math.abs(i4 - chunkPos.x) <= i && Math.abs(i5 - chunkPos.z) <= i) {
                flag = clientChunkManager.isChunkLoaded(i4, i5);
            } else {
                flag = false;
            }

            if (trackedSpawner2.isHidden()) {
                return !trackedSpawner2.isConfirmed() && !flag;
            } else {
                return flag ? !this.seenSpawnerPositions.contains(trackedSpawner2.getPos()) : trackedSpawner2.isIgnored();
            }
        });
    }

    @Nullable
    TrackedSpawner refreshSpawnerAt(ClientWorld clientWorld, BlockPos blockPos) {
        return clientWorld.getBlockEntity(blockPos) instanceof MobSpawnerBlockEntity mobSpawnerBlockEntity ? this.readSpawner(clientWorld, mobSpawnerBlockEntity) : null;
    }

    void updateSpawnerRanges(BlockPos blockPos, @Nullable EntityType<?> entityType, int i, int i2) {
        TrackedSpawner trackedSpawner = this.getOrCreateSpawner(blockPos, entityType);
        trackedSpawner.setRanges(i, i2);
    }

    TrackedSpawner getOrCreateWithDefaults(BlockPos blockPos, @Nullable EntityType<?> entityType) {
        TrackedSpawner trackedSpawner = this.getOrCreateSpawner(blockPos, entityType);
        if (trackedSpawner.getSpawnRange() == 0) {
            trackedSpawner.setRanges(4, 16);
        }

        return trackedSpawner;
    }

    private TrackedSpawner getOrCreateSpawner(BlockPos blockPos, @Nullable EntityType<?> entityType) {
        TrackedSpawner trackedSpawner = this.spawners.get(blockPos);
        boolean flag = trackedSpawner != null && (!trackedSpawner.isHidden() || entityType == null || trackedSpawner.getEntityType() == entityType);
        if (flag) {
            return trackedSpawner;
        }

        TrackedSpawner trackedSpawner2 = TrackedSpawner.hidden(blockPos, entityType);
        if (trackedSpawner != null) {
            trackedSpawner2.setLastNotifyTime(trackedSpawner.getLastNotifyTime());
        }

        this.spawners.put(blockPos, trackedSpawner2);
        return trackedSpawner2;
    }

    @Nullable
    TrackedSpawner findSpawnerForEntity(EntityType<?> entityType, double d, double d2, double d3) {
        TrackedSpawner trackedSpawner = null;
        double d4 = Double.MAX_VALUE;
        boolean flag = false;

        for (TrackedSpawner trackedSpawner2 : this.spawners.values()) {
            if (trackedSpawner2.canSpawnAt(d, d2, d3)) {
                boolean flag2 = trackedSpawner2.getEntityType() == entityType;
                double d5 = trackedSpawner2.getBox().getCenter().squaredDistanceTo(d, d2, d3);
                boolean flag3 = flag2 != flag ? flag2 : d5 < d4;
                if (flag3) {
                    trackedSpawner = trackedSpawner2;
                    d4 = d5;
                    flag = flag2;
                }
            }
        }

        return trackedSpawner;
    }

    @Nullable
    private TrackedSpawner readSpawner(ClientWorld clientWorld, MobSpawnerBlockEntity mobSpawnerBlockEntity) {
        BlockPos blockPos = mobSpawnerBlockEntity.getPos();
        MobSpawnerLogicAccessor mobSpawnerLogicAccessor = (MobSpawnerLogicAccessor)mobSpawnerBlockEntity.getLogic();
        MobSpawnerEntry mobSpawnerEntry = mobSpawnerLogicAccessor.sadnes$getSpawnEntry();
        EntityType entityType = mobSpawnerEntry == null ? null : mobSpawnerEntry.getNbt().getString("id").flatMap(EntityType::get).orElse(null);
        TrackedSpawner trackedSpawner = this.spawners.get(blockPos);
        if (trackedSpawner == null || trackedSpawner.isHidden() || trackedSpawner.getEntityType() != entityType) {
            int i = entityType == EntityType.SILVERFISH ? 7 : 2;
            if (!isAreaLoaded(clientWorld, blockPos, i)) {
                return trackedSpawner;
            }

            long l = trackedSpawner == null ? 0L : trackedSpawner.getLastNotifyTime();
            trackedSpawner = TrackedSpawner.of(blockPos, entityType, hasFarmStructure(clientWorld, blockPos, entityType));
            trackedSpawner.setLastNotifyTime(l);
            this.spawners.put(blockPos, trackedSpawner);
        }

        trackedSpawner.setRanges(mobSpawnerLogicAccessor.sadnes$getSpawnRange(), mobSpawnerLogicAccessor.sadnes$getRequiredPlayerRange());
        return trackedSpawner;
    }

    private static boolean isAreaLoaded(ClientWorld clientWorld, BlockPos blockPos, int i) {
        ClientChunkManager clientChunkManager = clientWorld.getChunkManager();
        int i2 = ChunkSectionPos.getSectionCoord(blockPos.getX() - i);
        int i3 = ChunkSectionPos.getSectionCoord(blockPos.getX() + i);
        int i4 = ChunkSectionPos.getSectionCoord(blockPos.getZ() - i);
        int i5 = ChunkSectionPos.getSectionCoord(blockPos.getZ() + i);
        return clientChunkManager.isChunkLoaded(i2, i4) && clientChunkManager.isChunkLoaded(i2, i5) && clientChunkManager.isChunkLoaded(i3, i4) && clientChunkManager.isChunkLoaded(i3, i5);
    }

    private static boolean hasFarmStructure(ClientWorld clientWorld, BlockPos blockPos, @Nullable EntityType<?> entityType) {
        if (entityType == null) {
            return false;
        }

        if (entityType != EntityType.ZOMBIE && entityType != EntityType.SKELETON && entityType != EntityType.SPIDER) {
            if (entityType == EntityType.CAVE_SPIDER) {
                if (countMatchingBlocks(clientWorld, blockPos, 2, 1, blockState -> blockState.isOf(Blocks.COBWEB)) > 0) {
                    return true;
                }
            } else if (entityType == EntityType.BLAZE) {
                if (countMatchingBlocks(
                        clientWorld,
                        blockPos,
                        1,
                        1,
                        blockState2 -> blockState2.isOf(Blocks.NETHER_BRICKS) || blockState2.isOf(Blocks.NETHER_BRICK_FENCE) || blockState2.isOf(Blocks.NETHER_BRICK_STAIRS)
                    )
                    > 0) {
                    return true;
                }
            } else if (entityType == EntityType.SILVERFISH) {
                if (countMatchingBlocks(clientWorld, blockPos, 7, 2, blockState3 -> blockState3.isOf(Blocks.END_PORTAL_FRAME)) > 0) {
                    return true;
                }

                if (countMatchingBlocks(
                        clientWorld,
                        blockPos,
                        2,
                        1,
                        blockState4 -> blockState4.isIn(BlockTags.STONE_BRICKS) || blockState4.isOf(Blocks.STONE_BRICK_STAIRS) || blockState4.isOf(Blocks.STONE_BRICK_SLAB)
                    )
                    > 0) {
                    return true;
                }
            } else {
                if (entityType != EntityType.MAGMA_CUBE) {
                    return false;
                }

                boolean flag = clientWorld.getBlockState(blockPos.up()).isOf(Blocks.IRON_CHAIN);
                if (flag
                    && countMatchingBlocks(
                            clientWorld,
                            blockPos.up(2),
                            1,
                            0,
                            blockState5 -> blockState5.isOf(Blocks.POLISHED_BLACKSTONE_BRICKS)
                                || blockState5.isOf(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS)
                                || blockState5.isOf(Blocks.GILDED_BLACKSTONE)
                                || blockState5.isOf(Blocks.BLACKSTONE)
                        )
                        >= 3) {
                    return true;
                }
            }

            return false;
        } else {
            int i = countMatchingBlocks(clientWorld, blockPos.down(), 1, 0, blockState6 -> blockState6.isOf(Blocks.MOSSY_COBBLESTONE));
            int i2 = countMatchingBlocks(clientWorld, blockPos.down(), 1, 0, blockState7 -> blockState7.isOf(Blocks.COBBLESTONE));
            return i > 0 && i + i2 >= 5;
        }
    }

    private static int countMatchingBlocks(ClientWorld clientWorld, BlockPos blockPos, int i, int i2, Predicate<BlockState> predicate) {
        int i3 = 0;

        for (BlockPos blockPos2 : BlockPos.iterate(blockPos.add(-i, -i2, -i), blockPos.add(i, i2, i))) {
            if (predicate.test(clientWorld.getBlockState(blockPos2))) {
                i3++;
            }
        }

        return i3;
    }
}
