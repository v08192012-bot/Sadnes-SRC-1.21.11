package tech.sadnes.client.event.impl;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import tech.sadnes.client.event.Event;

public class BlockUpdateEvent extends Event {
    private static final BlockUpdateEvent UPDATE_INSTANCE = new BlockUpdateEvent(BlockUpdateEvent.Type.UPDATE);
    private BlockState state;
    private BlockPos pos;
    private final BlockUpdateEvent.Type type;

    public BlockUpdateEvent(BlockUpdateEvent.Type type2) {
        this.type = type2;
    }

    public void fire(BlockPos blockPos, BlockState blockState) {
        BlockPos blockPos2 = this.pos;
        BlockState blockState2 = this.state;
        this.pos = blockPos;
        this.state = blockState;
        this.post();
        this.pos = blockPos2;
        this.state = blockState2;
    }

    public static void postUpdate(BlockPos blockPos, BlockState blockState) {
        UPDATE_INSTANCE.fire(blockPos, blockState);
    }

    public BlockState getState() {
        return this.state;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public BlockUpdateEvent.Type getType() {
        return this.type;
    }

    public enum Type {
        LOAD,
        UNLOAD,
        UPDATE;
    }
}
