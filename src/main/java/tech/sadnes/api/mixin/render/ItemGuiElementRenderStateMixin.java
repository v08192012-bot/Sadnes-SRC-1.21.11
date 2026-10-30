package tech.sadnes.api.mixin.render;

import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.api.duck.GuiItemAlphaHolder;
import tech.sadnes.client.render.GuiRenderUtil;

@Mixin(ItemGuiElementRenderState.class)
public abstract class ItemGuiElementRenderStateMixin implements GuiItemAlphaHolder {
    @Unique
    private float sadnes$alpha = 1.0F;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sadnes$captureAlpha(CallbackInfo callbackInfo) {
        this.sadnes$alpha = GuiRenderUtil.getItemAlpha();
    }

    @Override
    public float sadnes$guiItemAlpha() {
        return this.sadnes$alpha;
    }
}
