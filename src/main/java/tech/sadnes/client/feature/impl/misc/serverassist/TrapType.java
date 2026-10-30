package tech.sadnes.client.feature.impl.misc.serverassist;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import tech.sadnes.client.lang.Language;

public record TrapType(
    Item item,
    String nameKey,
    String nameFallback,
    TrapEffect effect,
    long time,
    int sizeX,
    int sizeY,
    int sizeZ,
    int centerOffsetX,
    int centerOffsetY,
    int centerOffsetZ,
    TrapType.StructureValidator validator
) {
    public String displayName() {
        return Language.translate(this.nameKey, this.nameFallback);
    }

    public boolean matchesSize(int i, int i2, int i3) {
        return i == this.sizeX && i2 == this.sizeY && i3 == this.sizeZ;
    }

    public BlockPos calculateCenter(int i, int i2, int i3) {
        return new BlockPos(i + this.centerOffsetX, i2 + this.centerOffsetY, i3 + this.centerOffsetZ);
    }

    public static TrapType corner(Item item2, Block block, String string, String string2, TrapEffect trapEffect, long l, int i, int i2) {
        return new TrapType(
            item2,
            string,
            string2,
            trapEffect,
            l,
            i,
            i,
            i,
            i2,
            i2,
            i2,
            (blockMatcher, i3, i4, i5, i6, i7, i8) -> checkAllCorners(blockMatcher, block, i3, i4, i5, i6, i7, i8)
        );
    }

    public static TrapType dual(Item item2, Block block, Block block2, String string, String string2, TrapEffect trapEffect, long l) {
        return new TrapType(
            item2,
            string,
            string2,
            trapEffect,
            l,
            5,
            5,
            5,
            2,
            2,
            2,
            (blockMatcher, i, i2, i3, i4, i5, i6) -> checkDoubleCorners(blockMatcher, block, block2, i, i2, i3, i4, i5, i6)
        );
    }

    public static TrapType plast(
        Item item2, Block block, String string, String string2, TrapEffect trapEffect, long l, int i, int i2, int i3, int i4, int i5, int i6
    ) {
        return new TrapType(
            item2,
            string,
            string2,
            trapEffect,
            l,
            i,
            i2,
            i3,
            i4,
            i5,
            i6,
            (blockMatcher, i7, i8, i9, i10, i11, i12) -> checkAllCorners(blockMatcher, block, i7, i8, i9, i10, i11, i12)
        );
    }

    public static boolean checkAllCorners(TrapType.BlockMatcher blockMatcher, Block block, int i, int i2, int i3, int i4, int i5, int i6) {
        return blockMatcher.isValid(new BlockPos(i, i2, i3), block)
            && blockMatcher.isValid(new BlockPos(i, i2, i6), block)
            && blockMatcher.isValid(new BlockPos(i, i5, i3), block)
            && blockMatcher.isValid(new BlockPos(i, i5, i6), block)
            && blockMatcher.isValid(new BlockPos(i4, i2, i3), block)
            && blockMatcher.isValid(new BlockPos(i4, i2, i6), block)
            && blockMatcher.isValid(new BlockPos(i4, i5, i3), block)
            && blockMatcher.isValid(new BlockPos(i4, i5, i6), block);
    }

    public static boolean checkDoubleCorners(TrapType.BlockMatcher blockMatcher, Block block, Block block2, int i, int i2, int i3, int i4, int i5, int i6) {
        return blockMatcher.isValid(new BlockPos(i, i2, i3), block2)
            && blockMatcher.isValid(new BlockPos(i, i2, i6), block2)
            && blockMatcher.isValid(new BlockPos(i, i5, i3), block)
            && blockMatcher.isValid(new BlockPos(i, i5, i6), block)
            && blockMatcher.isValid(new BlockPos(i4, i2, i3), block2)
            && blockMatcher.isValid(new BlockPos(i4, i2, i6), block2)
            && blockMatcher.isValid(new BlockPos(i4, i5, i3), block)
            && blockMatcher.isValid(new BlockPos(i4, i5, i6), block);
    }

    @FunctionalInterface
    public interface BlockMatcher {
        boolean isValid(BlockPos blockPos, Block block);
    }

    @FunctionalInterface
    public interface StructureValidator {
        boolean check(TrapType.BlockMatcher blockMatcher, int i, int i2, int i3, int i4, int i5, int i6);
    }
}
