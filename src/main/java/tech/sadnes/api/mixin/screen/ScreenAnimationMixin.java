package tech.sadnes.api.mixin.screen;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.animation.ContainerAnimations;

@Mixin(Screen.class)
public abstract class ScreenAnimationMixin {
    @Unique
    private boolean sadnes$suspended;
    @Unique
    private boolean sadnes$subtitlesSuspended;

    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void sadnes$beginShift(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        ContainerAnimations.INSTANCE.beginRender(drawContext, (Screen)(Object)this);
    }

    @Inject(
        method = "renderWithTooltip",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = Shift.AFTER)
    )
    private void sadnes$endShift(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        ContainerAnimations.INSTANCE.endRender(drawContext);
    }

    @Inject(method = "renderInGameBackground", at = @At("HEAD"))
    private void sadnes$suspendGradient(DrawContext drawContext, CallbackInfo callbackInfo) {
        this.sadnes$suspended = ContainerAnimations.INSTANCE.popIfPushed(drawContext);
    }

    @Inject(method = "renderInGameBackground", at = @At("TAIL"))
    private void sadnes$resumeGradient(DrawContext drawContext, CallbackInfo callbackInfo) {
        if (this.sadnes$suspended) {
            this.sadnes$suspended = false;
            ContainerAnimations.INSTANCE.applyTranslation(drawContext);
        }
    }

    @Inject(method = "renderDarkening(Lnet/minecraft/client/gui/DrawContext;IIII)V", at = @At("HEAD"))
    private void sadnes$suspendDarkening(DrawContext drawContext, int i, int i2, int i3, int i4, CallbackInfo callbackInfo) {
        this.sadnes$suspended = ContainerAnimations.INSTANCE.popIfPushed(drawContext);
    }

    @Inject(method = "renderDarkening(Lnet/minecraft/client/gui/DrawContext;IIII)V", at = @At("TAIL"))
    private void sadnes$resumeDarkening(DrawContext drawContext, int i, int i2, int i3, int i4, CallbackInfo callbackInfo) {
        if (this.sadnes$suspended) {
            this.sadnes$suspended = false;
            ContainerAnimations.INSTANCE.applyTranslation(drawContext);
        }
    }

    @Inject(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderDeferredSubtitles()V"))
    private void sadnes$suspendSubtitles(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        this.sadnes$subtitlesSuspended = ContainerAnimations.INSTANCE.popIfPushed(drawContext);
    }

    @Inject(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderDeferredSubtitles()V", shift = Shift.AFTER)
    )
    private void sadnes$resumeSubtitles(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        if (this.sadnes$subtitlesSuspended) {
            this.sadnes$subtitlesSuspended = false;
            ContainerAnimations.INSTANCE.applyTranslation(drawContext);
        }
    }

    @ModifyArg(
        method = "renderInGameBackground",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fillGradient(IIIIII)V"),
        index = 4
    )
    private int sadnes$fadeBackdropTop(int i) {
        return ContainerAnimations.INSTANCE.applyFade(i, (Screen)(Object)this);
    }

    @ModifyArg(
        method = "renderInGameBackground",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fillGradient(IIIIII)V"),
        index = 5
    )
    private int sadnes$fadeBackdropBottom(int i) {
        return ContainerAnimations.INSTANCE.applyFade(i, (Screen)(Object)this);
    }

    @Redirect(
        method = "renderBackgroundTexture",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V"
        )
    )
    private static void sadnes$fadeDarkening(
        DrawContext drawContext, RenderPipeline renderPipeline, Identifier identifier, int i, int i2, float f, float f2, int i3, int i4, int i5, int i6
    ) {
        drawContext.drawTexture(renderPipeline, identifier, i, i2, f, f2, i3, i4, i5, i6, ContainerAnimations.INSTANCE.getFadeColor());
    }
}
