package tech.sadnes.api.mixin.render;

import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuDevice;
import java.util.List;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gl.ShaderSourceGetter;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tech.sadnes.client.ClientInfo;

@Mixin(ShaderLoader.class)
public class ShaderLoaderMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientInfo.MOD_ID);

    @Redirect(
        method = "apply",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuDevice;precompilePipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gl/ShaderSourceGetter;)Lcom/mojang/blaze3d/pipeline/CompiledRenderPipeline;"
        )
    )
    private CompiledRenderPipeline sadnes$survivePipelineCrash(GpuDevice gpuDevice, RenderPipeline renderPipeline, ShaderSourceGetter shaderSourceGetter) {
        try {
            return gpuDevice.precompilePipeline(renderPipeline, shaderSourceGetter);
        } catch (RuntimeException exception) {
            if (!ClientInfo.MOD_ID.equals(renderPipeline.getLocation().getNamespace())) {
                throw exception;
            }

            LOGGER.error("Shader pipeline '{}' crashed the compiler on this backend; skipping it so the client stays usable.", renderPipeline.getLocation(), exception);
            return () -> false;
        }
    }

    @Redirect(method = "apply", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean sadnes$skipOwnFailedPipeline(List<Object> objects, Object object) {
        if (object instanceof Identifier identifier && ClientInfo.MOD_ID.equals(identifier.getNamespace())) {
            LOGGER.error("Shader pipeline '{}' failed to compile on this GPU/driver; skipping it so the client stays usable (this effect won't render).", identifier);
            return false;
        } else {
            return objects.add(object);
        }
    }
}
