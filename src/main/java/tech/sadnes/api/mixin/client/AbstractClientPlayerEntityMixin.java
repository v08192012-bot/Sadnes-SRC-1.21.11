package tech.sadnes.api.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.feature.impl.misc.StreamerMode;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void sadnes$replaceSkin(CallbackInfoReturnable<SkinTextures> callbackInfoReturnable) {
        SkinTextures skinTextures = StreamerMode.getReplacementSkin((AbstractClientPlayerEntity)(Object)this);
        if (skinTextures != null) {
            callbackInfoReturnable.setReturnValue(skinTextures);
        }
    }
}
