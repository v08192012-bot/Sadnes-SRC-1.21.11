package tech.sadnes.api.mixin.render;

import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.render.chunk.AbstractChunkRenderData;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.animation.ChunkAnimations;

@Mixin(BuiltChunk.class)
public abstract class BuiltChunkAnimationMixin {
    @Shadow
    @Final
    public AtomicReference<AbstractChunkRenderData> currentRenderData;

    @Shadow
    public abstract long getSectionPos();

    @Inject(method = "setCurrentRenderData", at = @At("HEAD"))
    private void sadnes$registerChunkAnimation(AbstractChunkRenderData abstractChunkRenderData, CallbackInfo callbackInfo) {
        if (abstractChunkRenderData != null && abstractChunkRenderData.hasData()) {
            AbstractChunkRenderData abstractChunkRenderData2 = this.currentRenderData.get();
            if (abstractChunkRenderData2 == null || !abstractChunkRenderData2.hasData()) {
                long l = this.getSectionPos();
                ChunkAnimations.INSTANCE
                    .queueChunk(
                        ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackX(l)),
                        ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackY(l)),
                        ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackZ(l))
                    );
            }
        }
    }
}
