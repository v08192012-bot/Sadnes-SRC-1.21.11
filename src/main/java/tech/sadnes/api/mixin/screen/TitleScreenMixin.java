package tech.sadnes.api.mixin.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.ui.menu.MainMenuScreen;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void sadnes$redirectToMainMenu(CallbackInfo callbackInfo) {
        MainMenuScreen mainMenuScreen = Sadnes.getInstance().getMainMenu();
        if (mainMenuScreen != null) {
            MinecraftClient.getInstance().setScreen(mainMenuScreen);
            callbackInfo.cancel();
        }
    }
}
