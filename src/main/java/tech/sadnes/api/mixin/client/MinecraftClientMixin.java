package tech.sadnes.api.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.animation.ContainerAnimations;
import tech.sadnes.client.event.impl.HotbarSelectEvent;
import tech.sadnes.client.event.impl.PreRotationEvent;
import tech.sadnes.client.event.impl.SetScreenEvent;
import tech.sadnes.client.feature.impl.combat.AimBot;
import tech.sadnes.client.feature.impl.misc.AutoGamble;
import tech.sadnes.client.feature.impl.player.NoDelay;
import tech.sadnes.client.feature.impl.player.Nuker;
import tech.sadnes.client.render.RenderHooks;
import tech.sadnes.client.ui.RadialMenu;
import tech.sadnes.client.util.DarkTitleBar;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Final
    private Window window;

    @Inject(method = "tick", at = @At("HEAD"))
    private void sadnes$onClientTick(CallbackInfo callbackInfo) {
        RenderHooks.onClientTick((MinecraftClient)(Object)this);
    }

    @Inject(
        method = "handleInputEvents",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"),
        cancellable = true
    )
    private void sadnes$onHotbarKey(CallbackInfo callbackInfo) {
        HotbarSelectEvent hotbarSelectEvent = new HotbarSelectEvent();
        hotbarSelectEvent.post();
        if (hotbarSelectEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;updateCrosshairTarget(F)V", shift = Shift.BEFORE))
    private void sadnes$preInputRotation(CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = (MinecraftClient)(Object)this;
        if (minecraftClient.player != null && minecraftClient.world != null) {
            PreRotationEvent.getInstance().post();
        }
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setVsync(Z)V", shift = Shift.BEFORE))
    private void sadnes$setDarkMode(CallbackInfo callbackInfo) {
        DarkTitleBar.enableDarkTitleBar(this.window.getHandle());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sadnes$updateTitle(CallbackInfo callbackInfo) {
        this.window.setTitle(ClientInfo.VERSION_STRING);
    }

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void sadnes$getWindowTitle(CallbackInfoReturnable<String> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(ClientInfo.VERSION_STRING);
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void sadnes$keepModuleBreaking(boolean flag, CallbackInfo callbackInfo) {
        AutoGamble autoGamble = AutoGamble.getInstance();
        Nuker nuker = Nuker.getInstance();
        boolean flag2;
        if (autoGamble != null) {
            flag2 = autoGamble.isBreaking();
        } else {
            flag2 = false;
        }

        if (nuker != null && nuker.isBreaking()) {
            if (!flag) {
                callbackInfo.cancel();
                return;
            }
        } else if (!flag) {
            if (!flag2) {
                return;
            }

            callbackInfo.cancel();
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void sadnes$suppressAutoGambleUse(CallbackInfo callbackInfo) {
        AutoGamble autoGamble = AutoGamble.getInstance();
        if (autoGamble != null && autoGamble.isEating()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void sadnes$suppressFastExpUse(CallbackInfo callbackInfo) {
        NoDelay noDelay = NoDelay.getInstance();
        if (noDelay != null && noDelay.shouldFastExp()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void sadnes$suppressAimBotUse(CallbackInfo callbackInfo) {
        AimBot aimBot = AimBot.getInstance();
        if (aimBot != null && aimBot.isFiring()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    public void setScreenHook(Screen screen, CallbackInfo callbackInfo) {
        SetScreenEvent setScreenEvent = new SetScreenEvent(screen);
        setScreenEvent.post();
        if (setScreenEvent.isCancelled() && setScreenEvent.getScreen() == screen) {
            callbackInfo.cancel();
        }
    }

    @Inject(
        method = "setScreen",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", opcode = 181)
    )
    private void sadnes$onScreenChanged(Screen screen, CallbackInfo callbackInfo) {
        ContainerAnimations.INSTANCE.onScreenChange(((MinecraftClient)(Object)this).currentScreen, screen);
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void sadnes$suppressAttackInSwapWheel(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (RadialMenu.isAnyOpen()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void sadnes$suppressUseInSwapWheel(CallbackInfo callbackInfo) {
        if (RadialMenu.isAnyOpen()) {
            callbackInfo.cancel();
        }
    }
}
