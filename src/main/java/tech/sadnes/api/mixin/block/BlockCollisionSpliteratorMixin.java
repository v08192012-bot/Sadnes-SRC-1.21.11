package tech.sadnes.api.mixin.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.BlockView;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.impl.BlockCollisionEvent;

@Mixin(BlockCollisionSpliterator.class)
public abstract class BlockCollisionSpliteratorMixin {
    @Redirect(
        method = "computeNext",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/BlockView;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;")
    )
    private BlockState sadnes$onBlockCollision(BlockView blockView, BlockPos blockPos) {
        BlockState blockState = blockView.getBlockState(blockPos);
        if (!Sadnes.getEventBus().hasListeners(BlockCollisionEvent.class)) {
            return blockState;
        } else {
            return blockView instanceof WorldChunk && ((WorldChunk)blockView).getWorld().isClient() ? BlockCollisionEvent.fire(blockPos, blockState) : blockState;
        }
    }
}
