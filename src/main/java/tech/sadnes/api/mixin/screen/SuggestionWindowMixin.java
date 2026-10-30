package tech.sadnes.api.mixin.screen;

import net.minecraft.client.gui.screen.ChatInputSuggestor.SuggestionWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.misc.ClientSpoof;

@Mixin(SuggestionWindow.class)
public class SuggestionWindowMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void sadnes$suppressStart(CallbackInfo callbackInfo) {
        ClientSpoof.setBypassed(true);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void sadnes$suppressEnd(CallbackInfo callbackInfo) {
        ClientSpoof.setBypassed(false);
    }
}
