package tech.sadnes.api.mixin.render;

import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl.ItemCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.api.duck.ItemAlphaHolder;
import tech.sadnes.client.feature.impl.render.TransparentPlayers;

@Mixin(ItemCommand.class)
public abstract class ItemCommandMixin implements ItemAlphaHolder {
    @Unique
    private float sadnes$alpha = 1.0F;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sadnes$captureAlpha(CallbackInfo callbackInfo) {
        this.sadnes$alpha = TransparentPlayers.getRenderAlpha();
    }

    @Override
    public float sadnes$itemAlpha() {
        return this.sadnes$alpha;
    }
}
