package tech.sadnes.api.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import tech.sadnes.client.feature.impl.render.Removals;

@Pseudo
@Mixin(targets = "net/caffeinemc/mods/sodium/client/render/model/AbstractBlockRenderContext", remap = false)
public abstract class SodiumBlockCullingMixin {
    @WrapOperation(
        method = "shouldDrawSide",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;getCullingFace(Lnet/minecraft/util/math/Direction;)Lnet/minecraft/util/shape/VoxelShape;",
            remap = true
        ),
        remap = false,
        require = 0
    )
    private VoxelShape sadnes$noCullingByHiddenBlock(BlockState blockState, Direction direction, Operation<VoxelShape> operation) {
        return Removals.isBlockHidden(blockState.getBlock()) ? VoxelShapes.empty() : (VoxelShape)operation.call(blockState, direction);
    }
}
