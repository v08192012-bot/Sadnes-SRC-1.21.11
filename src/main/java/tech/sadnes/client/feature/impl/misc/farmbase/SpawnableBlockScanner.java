package tech.sadnes.client.feature.impl.misc.farmbase;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.LightType;
import org.jetbrains.annotations.Nullable;

final class SpawnableBlockScanner {
    private final List<SpawnableBlockScanner.SpawnQuad> quads = new ArrayList<>();
    private final List<SpawnableBlockScanner.SpawnQuad> quadsView = Collections.unmodifiableList(this.quads);
    private final LongOpenHashSet scanned = new LongOpenHashSet();

    List<SpawnableBlockScanner.SpawnQuad> getQuads() {
        return this.quadsView;
    }

    void clear() {
        this.quads.clear();
        this.scanned.clear();
    }

    void reset() {
        this.clear();
    }

    void scanGeneric(ClientWorld clientWorld, BlockPos blockPos, BlockPos blockPos2, int i) {
        this.scan(clientWorld, blockPos, blockPos2, i, null, true);
    }

    void scanForEntity(ClientWorld clientWorld, BlockPos blockPos, BlockPos blockPos2, int i, @Nullable EntityType<?> entityType) {
        this.scan(clientWorld, blockPos, blockPos2, i, entityType, false);
    }

    private void scan(ClientWorld clientWorld, BlockPos blockPos, BlockPos blockPos2, int i, @Nullable EntityType<?> entityType, boolean flag) {
        Mutable mutable = new Mutable();
        Mutable mutable2 = new Mutable();
        int i2 = blockPos.getY();
        int i3 = blockPos2.getY();
        int i4 = blockPos.getX();
        int i5 = blockPos2.getX();
        int i6 = blockPos.getZ();
        int i7 = blockPos2.getZ();

        for (int i8 = i2; i8 <= i3; i8++) {
            for (int i9 = i4; i9 <= i5; i9++) {
                for (int i10 = i6; i10 <= i7; i10++) {
                    if (this.scanned.add(BlockPos.asLong(i9, i8, i10))) {
                        mutable.set(i9, i8, i10);
                        mutable2.set(i9, i8 - 1, i10);
                        SpawnableBlockScanner.SpawnState spawnState = getSpawnState(clientWorld, mutable, mutable2, i, entityType, flag);
                        if (spawnState != SpawnableBlockScanner.SpawnState.NEVER) {
                            this.quads.add(createQuad(i9, i8, i10, spawnState == SpawnableBlockScanner.SpawnState.POTENTIAL));
                        }
                    }
                }
            }
        }
    }

    private static SpawnableBlockScanner.SpawnQuad createQuad(int i, int i2, int i3, boolean flag) {
        double d = i2 + 0.0075;
        return new SpawnableBlockScanner.SpawnQuad(
            new Vec3d(i, d, i3), new Vec3d(i + 1.0, d, i3 + 1.0), new Vec3d(i + 1.0, d, i3), new Vec3d(i, d, i3 + 1.0), flag
        );
    }

    private static SpawnableBlockScanner.SpawnState getSpawnState(
        ClientWorld clientWorld, BlockPos blockPos, BlockPos blockPos2, int i, @Nullable EntityType<?> entityType, boolean flag
    ) {
        BlockState blockState = clientWorld.getBlockState(blockPos);
        boolean flag2 = blockState.getBlock() instanceof SnowBlock && blockState.get(SnowBlock.LAYERS) == 1;
        if (!blockState.isAir() && !flag2) {
            return SpawnableBlockScanner.SpawnState.NEVER;
        } else if (flag && !isSpawnableFloor(clientWorld.getBlockState(blockPos2))) {
            return SpawnableBlockScanner.SpawnState.NEVER;
        } else if (!flag && !canFit(clientWorld, blockPos, entityType)) {
            return SpawnableBlockScanner.SpawnState.NEVER;
        } else if (clientWorld.getLightLevel(LightType.BLOCK, blockPos) > i) {
            return SpawnableBlockScanner.SpawnState.NEVER;
        } else {
            return clientWorld.getLightLevel(LightType.SKY, blockPos) > i ? SpawnableBlockScanner.SpawnState.POTENTIAL : SpawnableBlockScanner.SpawnState.ALWAYS;
        }
    }

    private static boolean canFit(ClientWorld clientWorld, BlockPos blockPos, @Nullable EntityType<?> entityType) {
        double d = blockPos.getX() + 0.5;
        double d2 = blockPos.getZ() + 0.5;
        return entityType != null ? clientWorld.isSpaceEmpty(entityType.getSpawnBox(d, blockPos.getY(), d2)) : clientWorld.getBlockState(blockPos.up()).isAir();
    }

    private static boolean isSpawnableFloor(BlockState blockState) {
        Block block = blockState.getBlock();
        if (block == Blocks.BEDROCK || block == Blocks.BARRIER || block instanceof TransparentBlock || block instanceof ScaffoldingBlock) {
            return false;
        }

        if (block != Blocks.SOUL_SAND && block != Blocks.MUD) {
            if (block instanceof SlabBlock && blockState.get(SlabBlock.TYPE) == SlabType.TOP) {
                return true;
            } else {
                return block instanceof StairsBlock && blockState.get(StairsBlock.HALF) == BlockHalf.TOP ? true : blockState.isOpaqueFullCube();
            }
        } else {
            return true;
        }
    }

    record SpawnQuad(Vec3d northWest, Vec3d southEast, Vec3d northEast, Vec3d southWest, boolean potential) {
    }

    enum SpawnState {
        NEVER,
        POTENTIAL,
        ALWAYS;
    }
}
