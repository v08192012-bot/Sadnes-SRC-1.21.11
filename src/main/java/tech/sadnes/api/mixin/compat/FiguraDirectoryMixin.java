package tech.sadnes.api.mixin.compat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;
import org.figuramc.figura.FiguraMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.ClientInfo;

@Mixin(FiguraMod.class)
public class FiguraDirectoryMixin {
    @Inject(method = "getFiguraDirectory", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sadnes$redirectDirectory(CallbackInfoReturnable<Path> callbackInfoReturnable) {
        Path path = MinecraftClient.getInstance().runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("figura");

        try {
            Files.createDirectories(path);
        } catch (IOException ioException) {
        }

        callbackInfoReturnable.setReturnValue(path);
    }
}
