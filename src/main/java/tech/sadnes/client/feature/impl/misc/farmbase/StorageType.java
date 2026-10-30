package tech.sadnes.client.feature.impl.misc.farmbase;

import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.block.entity.DecoratedPotBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.screen.ScreenHandlerType;
import org.jetbrains.annotations.Nullable;
import tech.sadnes.client.render.texture.Icon;

enum StorageType {
    CHEST("chest", Icon.BOXES),
    TRAPPED("trapped", Icon.WARNING),
    SHULKER("shulker", Icon.CUBE),
    ENDER("ender", Icon.WAND_SPARKLES),
    OTHER("other", Icon.FLAME);

    private final String key;
    private final Icon icon;

    @Nullable
    static StorageType fromBlock(Block block) {
        if (block instanceof TrappedChestBlock) {
            return TRAPPED;
        } else if (block instanceof ChestBlock) {
            return CHEST;
        } else if (block instanceof EnderChestBlock) {
            return ENDER;
        } else {
            return block instanceof ShulkerBoxBlock ? SHULKER : null;
        }
    }

    @Nullable
    static StorageType fromScreenHandler(ScreenHandlerType<?> screenHandlerType) {
        if (screenHandlerType == ScreenHandlerType.SHULKER_BOX) {
            return SHULKER;
        } else if (screenHandlerType == ScreenHandlerType.GENERIC_9X1
            || screenHandlerType == ScreenHandlerType.GENERIC_9X2
            || screenHandlerType == ScreenHandlerType.GENERIC_9X3
            || screenHandlerType == ScreenHandlerType.GENERIC_9X4
            || screenHandlerType == ScreenHandlerType.GENERIC_9X5
            || screenHandlerType == ScreenHandlerType.GENERIC_9X6) {
            return CHEST;
        } else {
            return screenHandlerType != ScreenHandlerType.GENERIC_3X3
                    && screenHandlerType != ScreenHandlerType.CRAFTER_3X3
                    && screenHandlerType != ScreenHandlerType.FURNACE
                    && screenHandlerType != ScreenHandlerType.BLAST_FURNACE
                    && screenHandlerType != ScreenHandlerType.SMOKER
                    && screenHandlerType != ScreenHandlerType.HOPPER
                    && screenHandlerType != ScreenHandlerType.BREWING_STAND
                ? null
                : OTHER;
        }
    }

    @Nullable
    static StorageType fromBlockEntityType(BlockEntityType<?> blockEntityType) {
        if (blockEntityType == BlockEntityType.TRAPPED_CHEST) {
            return TRAPPED;
        }

        if (blockEntityType == BlockEntityType.CHEST || blockEntityType == BlockEntityType.BARREL) {
            return CHEST;
        }

        if (blockEntityType == BlockEntityType.SHULKER_BOX) {
            return SHULKER;
        }

        if (blockEntityType == BlockEntityType.ENDER_CHEST) {
            return ENDER;
        }

        boolean flag = blockEntityType == BlockEntityType.FURNACE
            || blockEntityType == BlockEntityType.BLAST_FURNACE
            || blockEntityType == BlockEntityType.SMOKER
            || blockEntityType == BlockEntityType.BREWING_STAND
            || blockEntityType == BlockEntityType.CHISELED_BOOKSHELF
            || blockEntityType == BlockEntityType.CRAFTER
            || blockEntityType == BlockEntityType.DISPENSER
            || blockEntityType == BlockEntityType.DROPPER
            || blockEntityType == BlockEntityType.DECORATED_POT
            || blockEntityType == BlockEntityType.HOPPER;
        return flag ? OTHER : null;
    }

    @Nullable
    static StorageType fromBlockEntity(BlockEntity blockEntity) {
        if (blockEntity instanceof TrappedChestBlockEntity) {
            return TRAPPED;
        }

        if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity) {
            return CHEST;
        }

        if (blockEntity instanceof ShulkerBoxBlockEntity) {
            return SHULKER;
        }

        if (blockEntity instanceof EnderChestBlockEntity) {
            return ENDER;
        }

        boolean flag;
        if (!(blockEntity instanceof AbstractFurnaceBlockEntity)
            && !(blockEntity instanceof BrewingStandBlockEntity)
            && !(blockEntity instanceof ChiseledBookshelfBlockEntity)
            && !(blockEntity instanceof CrafterBlockEntity)
            && !(blockEntity instanceof DispenserBlockEntity)
            && !(blockEntity instanceof DecoratedPotBlockEntity)) {
            flag = blockEntity instanceof HopperBlockEntity;
        } else {
            flag = true;
        }

        return flag ? OTHER : null;
    }

    public String getKey() {
        return this.key;
    }

    public Icon getIcon() {
        return this.icon;
    }

    StorageType(String string, Icon icon2) {
        this.key = string;
        this.icon = icon2;
    }
}
