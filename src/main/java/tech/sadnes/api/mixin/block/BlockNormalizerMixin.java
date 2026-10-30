package tech.sadnes.api.mixin.block;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.feature.impl.movement.BlockNormalizer;

@Mixin(Block.class)
public abstract class BlockNormalizerMixin {
    @Inject(method = "getSlipperiness", at = @At("RETURN"), cancellable = true)
    private void sadnes$normalizeIce(CallbackInfoReturnable<Float> callbackInfoReturnable) {
        BlockNormalizer blockNormalizer = BlockNormalizer.getInstance();
        if (blockNormalizer != null && blockNormalizer.shouldNormalize("ice") && callbackInfoReturnable.getReturnValueF() > 0.6F) {
            callbackInfoReturnable.setReturnValue(0.6F);
        }
    }

    @Inject(method = "getVelocityMultiplier", at = @At("RETURN"), cancellable = true)
    private void sadnes$normalizeVelocity(CallbackInfoReturnable<Float> callbackInfoReturnable) {
        BlockNormalizer blockNormalizer = BlockNormalizer.getInstance();
        if (blockNormalizer != null) {
            if (blockNormalizer.shouldNormalize("soul_sand") && (Object)this == Blocks.SOUL_SAND) {
                callbackInfoReturnable.setReturnValue(1.0F);
            } else {
                if (blockNormalizer.shouldNormalize("honey") && (Object)this == Blocks.HONEY_BLOCK) {
                    callbackInfoReturnable.setReturnValue(1.0F);
                }
            }
        }
    }

    @Inject(method = "getJumpVelocityMultiplier", at = @At("RETURN"), cancellable = true)
    private void sadnes$normalizeJump(CallbackInfoReturnable<Float> callbackInfoReturnable) {
        BlockNormalizer blockNormalizer = BlockNormalizer.getInstance();
        if (blockNormalizer != null) {
            if (blockNormalizer.shouldNormalize("soul_sand") && (Object)this == Blocks.SOUL_SAND) {
                callbackInfoReturnable.setReturnValue(1.0F);
            } else {
                if (blockNormalizer.shouldNormalize("honey") && (Object)this == Blocks.HONEY_BLOCK) {
                    callbackInfoReturnable.setReturnValue(1.0F);
                }
            }
        }
    }
}
