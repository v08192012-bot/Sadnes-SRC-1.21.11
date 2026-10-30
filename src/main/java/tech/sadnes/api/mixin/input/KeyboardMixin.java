package tech.sadnes.api.mixin.input;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.api.mixin.client.MinecraftClientAccessor;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"))
    private void sadnes$onKey(long l, int i, KeyInput keyInput, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        new ScreenMouseClickEvent(keyInput.key(), keyInput.scancode(), keyInput.modifiers(), i, ((MinecraftClientAccessor)minecraftClient).sadnes$getCurrentScreen()).post();
    }
}
