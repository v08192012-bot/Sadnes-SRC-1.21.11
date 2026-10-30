package tech.sadnes.api.mixin.screen;

import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.event.impl.DeathScreenEvent;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void sadnes$onDeath(CallbackInfo callbackInfo) {
        new DeathScreenEvent().post();
    }
}
