package tech.sadnes.api.mixin.render;

import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.render.GuiScale;

@Mixin(Window.class)
public abstract class WindowScaleMixin {
    @Inject(method = "getScaledWidth", at = @At("HEAD"), cancellable = true)
    private void sadnes$scaledWidth(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        if (GuiScale.isActive()) {
            callbackInfoReturnable.setReturnValue(GuiScale.getScaledWidthInt());
        }
    }

    @Inject(method = "getScaledHeight", at = @At("HEAD"), cancellable = true)
    private void sadnes$scaledHeight(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        if (GuiScale.isActive()) {
            callbackInfoReturnable.setReturnValue(GuiScale.getScaledHeightInt());
        }
    }
}
