package tech.sadnes.api.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import tech.sadnes.client.feature.impl.render.Removals;

@Mixin(AbstractBlockState.class)
public abstract class RemovalsBlockStateMixin {
    @Shadow
    public abstract Block getBlock();

    @ModifyReturnValue(method = "getRenderType", at = @At("RETURN"))
    private BlockRenderType sadnes$hideRemovedBlock(BlockRenderType blockRenderType) {
        return Removals.isBlockHidden(this.getBlock()) ? BlockRenderType.INVISIBLE : blockRenderType;
    }
}
