package tech.sadnes.client.feature.impl.misc.farmbase;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

final class StorageEntry {
    private final BlockPos pos;
    private final Box box;
    private final StorageType type;
    private final boolean packetOnly;

    private StorageEntry(BlockPos blockPos, Box box2, StorageType storageType, boolean flag) {
        this.pos = blockPos;
        this.box = box2;
        this.type = storageType;
        this.packetOnly = flag;
    }

    static StorageEntry of(BlockPos blockPos, Box box2, StorageType storageType) {
        return new StorageEntry(blockPos, box2, storageType, false);
    }

    static StorageEntry fromPacket(BlockPos blockPos, StorageType storageType) {
        return new StorageEntry(blockPos, new Box(blockPos), storageType, true);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Box getBox() {
        return this.box;
    }

    public StorageType getType() {
        return this.type;
    }

    public boolean isPacketOnly() {
        return this.packetOnly;
    }
}
