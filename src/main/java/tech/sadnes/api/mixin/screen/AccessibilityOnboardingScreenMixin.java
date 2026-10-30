package tech.sadnes.api.mixin.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.ui.menu.MainMenuScreen;

@Mixin(AccessibilityOnboardingScreen.class)
public abstract class AccessibilityOnboardingScreenMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void sadnes$skipOnboarding(CallbackInfo callbackInfo) {
        MainMenuScreen mainMenuScreen = Sadnes.getInstance().getMainMenu();
        if (mainMenuScreen != null) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            minecraftClient.options.setAccessibilityOnboarded();
            minecraftClient.setScreen(mainMenuScreen);
            callbackInfo.cancel();
        }
    }
}
