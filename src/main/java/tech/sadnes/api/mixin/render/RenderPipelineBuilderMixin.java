package tech.sadnes.api.mixin.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import java.util.Optional;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.render.world.FoliageSwayManager;

@Mixin(Builder.class)
public abstract class RenderPipelineBuilderMixin {
    @Shadow
    private Optional<Identifier> vertexShader;

    @Shadow
    public abstract Builder withUniform(String string, UniformType uniformType);

    @Inject(method = "build", at = @At("HEAD"))
    private void sadnes$foliageUniform(CallbackInfoReturnable<RenderPipeline> callbackInfoReturnable) {
        if (FoliageSwayManager.isTerrainShader(this.vertexShader)) {
            this.withUniform("SadnesFoliage", UniformType.UNIFORM_BUFFER);
        }
    }
}
