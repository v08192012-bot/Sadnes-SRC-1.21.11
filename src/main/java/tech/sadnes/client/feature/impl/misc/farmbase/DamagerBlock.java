package tech.sadnes.client.feature.impl.misc.farmbase;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

final class DamagerBlock {
    private final BlockPos pos;
    private final Box box;
    private long lastKillTime;

    DamagerBlock(BlockPos blockPos, long l) {
        this.pos = blockPos;
        this.box = new Box(blockPos);
        this.lastKillTime = l;
    }

    void setLastKillTime(long l) {
        this.lastKillTime = l;
    }

    boolean isActive(long l) {
        return l - this.lastKillTime < 1000L;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Box getBox() {
        return this.box;
    }

    public long getLastKillTime() {
        return this.lastKillTime;
    }
}
