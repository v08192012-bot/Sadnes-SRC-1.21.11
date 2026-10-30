package tech.sadnes.client.feature.impl.misc.autobuild;

import baritone.api.schematic.ISchematic;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.util.math.BlockPos;

public final class FilteredSchematic implements ISchematic {
    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private final ISchematic delegate;
    private volatile boolean clearAir;
    private volatile boolean ignoreDirection;
    private volatile boolean ignoreAxis;
    private volatile boolean keepExisting;
    private volatile boolean clearMode;
    private volatile LongSet skippedPositions = LongSets.EMPTY_SET;
    private volatile Set<BlockState> skippedStates = Set.of();
    private volatile LongSet postponedPositions = LongSets.EMPTY_SET;

    public BlockState getTargetState(int i, int i2, int i3) {
        boolean flag = i >= 0 && i < this.delegate.widthX() && i2 >= 0 && i2 < this.delegate.heightY() && i3 >= 0 && i3 < this.delegate.lengthZ();
        return flag && this.delegate.inSchematic(i, i2, i3, AIR) ? this.delegate.desiredState(i, i2, i3, AIR, null) : null;
    }

    public boolean isBuildable(BlockState blockState) {
        switch (BuildBlockUtil.getKind(blockState)) {
            case AIR:
                return this.clearAir;
            case MANUAL:
                break;
            case DIRECTIONAL:
                if (!this.ignoreDirection) {
                    return true;
                }
                break;
            case AXIS:
                if (this.ignoreAxis && this.ignoreDirection) {
                    break;
                }

                return true;
            case BLOCK:
                return true;
            default:
                throw new MatchException(null, null);
        }

        return false;
    }

    public boolean shouldBuild(int i, int i2, int i3, BlockState blockState) {
        return this.isBuildable(blockState) && !this.skippedStates.contains(blockState) && !this.skippedPositions.contains(BlockPos.asLong(i, i2, i3));
    }

    public boolean needsClearing(BlockState blockState, BlockState blockState2) {
        if (!blockState2.isAir()) {
            if (blockState2.getBlock() instanceof FluidBlock) {
                return false;
            }

            if (blockState2.getBlock() != blockState.getBlock()) {
                if (!blockState.isAir() && !blockState2.isReplaceable() && this.keepExisting) {
                    return false;
                }

                return true;
            }
        }

        return false;
    }

    public void skipPositions(LongCollection longCollection) {
        LongOpenHashSet longOpenHashSet = new LongOpenHashSet(this.skippedPositions);
        longOpenHashSet.addAll(longCollection);
        this.skippedPositions = longOpenHashSet;
    }

    public void skipStates(Collection<BlockState> blockStates) {
        HashSet hashSet = new HashSet<>(this.skippedStates);
        hashSet.addAll(blockStates);
        this.skippedStates = hashSet;
    }

    public boolean isSkippedState(BlockState blockState) {
        return this.skippedStates.contains(blockState);
    }

    public boolean isPostponed(long l) {
        return this.postponedPositions.contains(l);
    }

    @Override
    public boolean inSchematic(int i, int i2, int i3, BlockState blockState) {
        BlockState blockState2 = this.getTargetState(i, i2, i3);
        boolean flag;
        if (!this.clearMode) {
            flag = this.postponedPositions.contains(BlockPos.asLong(i, i2, i3));
        } else {
            flag = false;
        }

        return blockState2 != null && this.shouldBuild(i, i2, i3, blockState2) && !flag;
    }

    @Override
    public BlockState desiredState(int i, int i2, int i3, BlockState blockState, List<BlockState> blockStates) {
        BlockState blockState2 = this.delegate.desiredState(i, i2, i3, blockState, blockStates);
        if (blockState == null) {
            return blockState2;
        }

        boolean flag;
        if (blockState2.isAir()) {
            flag = blockState.getBlock() instanceof FluidBlock;
        } else {
            flag = false;
        }

        boolean flag2;
        if (this.keepExisting && !blockState2.isAir()) {
            flag2 = !blockState.isReplaceable();
        } else {
            flag2 = false;
        }

        if (!blockState2.isAir() && blockState.getBlock() == blockState2.getBlock()) {
            return blockState;
        } else if (!flag && !flag2) {
            boolean flag3 = this.needsClearing(blockState2, blockState);
            BlockState blockState3 = flag3 ? AIR : blockState;
            BlockState blockState4 = flag3 ? blockState : blockState2;
            return this.clearMode ? blockState3 : blockState4;
        } else {
            return blockState;
        }
    }

    @Override
    public void reset() {
        this.delegate.reset();
    }

    @Override
    public int widthX() {
        return this.delegate.widthX();
    }

    @Override
    public int heightY() {
        return this.delegate.heightY();
    }

    @Override
    public int lengthZ() {
        return this.delegate.lengthZ();
    }

    public boolean isClearAir() {
        return this.clearAir;
    }

    public boolean isIgnoreDirection() {
        return this.ignoreDirection;
    }

    public boolean isIgnoreAxis() {
        return this.ignoreAxis;
    }

    public boolean isKeepExisting() {
        return this.keepExisting;
    }

    public boolean isClearMode() {
        return this.clearMode;
    }

    public FilteredSchematic setClearAir(boolean flag) {
        this.clearAir = flag;
        return this;
    }

    public FilteredSchematic setIgnoreDirection(boolean flag) {
        this.ignoreDirection = flag;
        return this;
    }

    public FilteredSchematic setIgnoreAxis(boolean flag) {
        this.ignoreAxis = flag;
        return this;
    }

    public FilteredSchematic setKeepExisting(boolean flag) {
        this.keepExisting = flag;
        return this;
    }

    public FilteredSchematic setClearMode(boolean flag) {
        this.clearMode = flag;
        return this;
    }

    public FilteredSchematic setPostponedPositions(LongSet longSet) {
        this.postponedPositions = longSet;
        return this;
    }

    public FilteredSchematic(ISchematic iSchematic) {
        this.delegate = iSchematic;
    }
}
