package tech.sadnes.api.mixin.input;

import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.api.mixin.client.MinecraftClientAccessor;
import tech.sadnes.client.event.impl.HotbarSelectEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseScrollEvent;
import tech.sadnes.client.feature.impl.misc.AutoBuild;
import tech.sadnes.client.render.GuiScale;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(
        method = "onMouseScroll",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"),
        cancellable = true
    )
    private void sadnes$onHotbarScroll(long l, double d, double d2, CallbackInfo callbackInfo) {
        HotbarSelectEvent hotbarSelectEvent = new HotbarSelectEvent();
        hotbarSelectEvent.post();
        if (hotbarSelectEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void sadnes$onMouseButton(long l, MouseInput mouseInput, int i, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Window window = minecraftClient.getWindow();
        double d = window.getWidth() == 0 ? 0.0 : minecraftClient.mouse.getX() * window.getScaledWidth() / window.getWidth();
        double d2 = window.getHeight() == 0 ? 0.0 : minecraftClient.mouse.getY() * window.getScaledHeight() / window.getHeight();
        double d3 = GuiScale.toScaled(d);
        double d4 = GuiScale.toScaled(d2);
        new ScreenKeyEvent(mouseInput.button(), i, mouseInput.modifiers(), ((MinecraftClientAccessor)minecraftClient).sadnes$getCurrentScreen(), d3, d4).post();
    }

    @Inject(method = "onFilesDropped", at = @At("HEAD"))
    private void sadnes$onFilesDropped(long l, List<Path> paths, int i, CallbackInfo callbackInfo) {
        AutoBuild.importFiles(paths);
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void sadnes$onMouseScroll(long l, double d, double d2, CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (l == minecraftClient.getWindow().getHandle()) {
            Window window = minecraftClient.getWindow();
            double d3 = window.getWidth() == 0 ? 0.0 : minecraftClient.mouse.getX() * window.getScaledWidth() / window.getWidth();
            double d4 = window.getHeight() == 0 ? 0.0 : minecraftClient.mouse.getY() * window.getScaledHeight() / window.getHeight();
            double d5 = GuiScale.toScaled(d3);
            double d6 = GuiScale.toScaled(d4);
            ScreenMouseScrollEvent screenMouseScrollEvent = new ScreenMouseScrollEvent(d5, d6, d, d2, ((MinecraftClientAccessor)minecraftClient).sadnes$getCurrentScreen());
            screenMouseScrollEvent.post();
            if (screenMouseScrollEvent.isCancelled()) {
                callbackInfo.cancel();
            }
        }
    }
}
