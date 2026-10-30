package tech.sadnes.api.mixin.render;

import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.script.ScriptShaders;

@Mixin(ShaderLoader.class)
public class ShaderLoaderScriptMixin {
    @Inject(method = "getSource", at = @At("HEAD"), cancellable = true)
    private void sadnes$scriptShaderSource(Identifier identifier, ShaderType shaderType, CallbackInfoReturnable<String> callbackInfoReturnable) {
        String string = ScriptShaders.getShaderSource(identifier, shaderType);
        if (string != null) {
            callbackInfoReturnable.setReturnValue(string);
        }
    }
}
