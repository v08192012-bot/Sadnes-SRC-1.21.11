package tech.sadnes.api.mixin.input;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import tech.sadnes.client.event.impl.MouseLookEvent;

@Mixin(Mouse.class)
public abstract class MouseLookMixin {
    @ModifyArgs(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void sadnes$onChangeLookDirection(Args args) {
        double d = args.<Double>get(0);
        double d2 = args.<Double>get(1);
        MouseLookEvent mouseLookEvent = new MouseLookEvent(d, d2);
        mouseLookEvent.post();
        if (mouseLookEvent.isCancelled()) {
            args.set(0, 0.0);
            args.set(1, 0.0);
        } else {
            args.set(0, mouseLookEvent.getDeltaX());
            args.set(1, mouseLookEvent.getDeltaY());
        }
    }
}
