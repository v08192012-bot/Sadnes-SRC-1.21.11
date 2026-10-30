package tech.sadnes.api.mixin.render;

import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.render.GuiOverlapUtil;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {
    @Redirect(
        method = "findAndGoToLayerIntersecting",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;anyIntersect(Lnet/minecraft/client/gui/ScreenRect;Ljava/util/List;)Z"
        )
    )
    private boolean sadnes$gridAnyIntersect(GuiRenderState guiRenderState, ScreenRect screenRect, List<? extends GuiElementRenderState> list) {
        return GuiOverlapUtil.overlapsAny(screenRect, list);
    }

    @Inject(method = "clear", at = @At("TAIL"))
    private void sadnes$resetLayeringGrid(CallbackInfo callbackInfo) {
        GuiOverlapUtil.reset();
    }
}
