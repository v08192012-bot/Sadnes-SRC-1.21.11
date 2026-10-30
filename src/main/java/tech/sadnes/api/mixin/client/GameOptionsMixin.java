package tech.sadnes.api.mixin.client;

import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.InactivityFpsLimit;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.event.impl.SyncedOptionsEvent;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
    @Inject(method = "method_61969", at = @At("HEAD"), cancellable = true)
    private static void sadnes$neverTooltip(InactivityFpsLimit inactivityFpsLimit, CallbackInfoReturnable<Tooltip> callbackInfoReturnable) {
        if (inactivityFpsLimit.asString().equals("never")) {
            callbackInfoReturnable.setReturnValue(Tooltip.of(Text.translatable("options.inactivityFpsLimit.never.tooltip")));
        }
    }

    @Inject(method = "getSyncedOptions", at = @At("RETURN"), cancellable = true)
    private void sadnes$spoofClientOptions(CallbackInfoReturnable<SyncedClientOptions> callbackInfoReturnable) {
        SyncedOptionsEvent syncedOptionsEvent = new SyncedOptionsEvent((SyncedClientOptions)callbackInfoReturnable.getReturnValue());
        syncedOptionsEvent.post();
        callbackInfoReturnable.setReturnValue(syncedOptionsEvent.toOptions());
    }
}
