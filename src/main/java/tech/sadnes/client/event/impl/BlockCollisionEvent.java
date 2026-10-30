package tech.sadnes.client.event.impl;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import tech.sadnes.client.event.CancellableEvent;

public final class BlockCollisionEvent extends CancellableEvent {
    private static final BlockCollisionEvent INSTANCE = new BlockCollisionEvent();
    private BlockPos pos;
    private BlockState state;

    private BlockCollisionEvent() {
    }

    public static BlockState fire(BlockPos blockPos, BlockState blockState) {
        BlockCollisionEvent blockCollisionEvent = INSTANCE;
        BlockPos blockPos2 = blockCollisionEvent.pos;
        BlockState blockState2 = blockCollisionEvent.state;
        boolean flag = blockCollisionEvent.isCancelled();
        blockCollisionEvent.pos = blockPos;
        blockCollisionEvent.state = blockState;
        blockCollisionEvent.post();
        BlockState blockState3 = blockCollisionEvent.isCancelled() ? Blocks.AIR.getDefaultState() : blockCollisionEvent.state;
        blockCollisionEvent.pos = blockPos2;
        blockCollisionEvent.state = blockState2;
        blockCollisionEvent.setCancelled(flag);
        return blockState3;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public BlockState getState() {
        return this.state;
    }

    public BlockCollisionEvent setState(BlockState blockState) {
        this.state = blockState;
        return this;
    }
}
