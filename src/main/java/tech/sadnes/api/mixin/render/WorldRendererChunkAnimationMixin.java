package tech.sadnes.api.mixin.render;

import net.minecraft.client.gl.DynamicUniforms.ChunkSectionsValue;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tech.sadnes.client.animation.ChunkAnimations;

@Mixin(value = WorldRenderer.class, priority = 1001)
public abstract class WorldRendererChunkAnimationMixin {
    @Redirect(
        method = "renderBlockLayers",
        at = @At(value = "NEW", target = "(Lorg/joml/Matrix4fc;IIIFII)Lnet/minecraft/client/gl/DynamicUniforms$ChunkSectionsValue;"),
        require = 0
    )
    private ChunkSectionsValue sadnes$animateChunkSection(Matrix4fc matrix4fc, int i, int i2, int i3, float f, int i4, int i5) {
        Matrix4fc matrix4fc2 = ChunkAnimations.INSTANCE.transform(matrix4fc, i, i2, i3);
        return new ChunkSectionsValue(matrix4fc2, i, i2, i3, f, i4, i5);
    }
}
