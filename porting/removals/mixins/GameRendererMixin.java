package tech.sadnes.api.mixin.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.feature.impl.render.AspectRatio;
import tech.sadnes.client.feature.impl.render.CameraTweaks;
import tech.sadnes.client.feature.impl.render.FullBright;
import tech.sadnes.client.feature.impl.render.Removals;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;
import tech.sadnes.client.render.MotionBlurRenderer;
import tech.sadnes.client.render.RenderHooks;
import tech.sadnes.client.render.shader.AmbienceRenderer;
import tech.sadnes.client.render.shader.ShaderSkyRenderer;
import tech.sadnes.client.render.world.FoliageSwayManager;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"
        )
    )
    private void sadnes$render2D(InGameHud inGameHud, DrawContext drawContext, RenderTickCounter renderTickCounter) {
        RenderHooks.renderPreOverlay(drawContext, renderTickCounter);
        RenderHooks.renderOverlay(drawContext, renderTickCounter);
        inGameHud.render(drawContext, renderTickCounter);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;clear()V"))
    private void sadnes$updateBlurBeforeGui(RenderTickCounter renderTickCounter, boolean flag, CallbackInfo callbackInfo) {
        RenderHooks.flushFrame();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void sadnes$resetRenderState(RenderTickCounter renderTickCounter, boolean flag, CallbackInfo callbackInfo) {
        RenderHooks.reset();
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void sadnes$removeHurtCamera(MatrixStack matrixStack, float f, CallbackInfo callbackInfo) {
        if (Removals.isRemoved("shake")) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void sadnes$cameraTweaksZoom(Camera camera, float f, boolean flag, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        CameraTweaks cameraTweaks = CameraTweaks.getInstance();
        if (cameraTweaks != null) {
            callbackInfoReturnable.setReturnValue(cameraTweaks.applyZoom(callbackInfoReturnable.getReturnValueF()));
        }
    }

    @Redirect(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEffectFadeFactor(Lnet/minecraft/registry/entry/RegistryEntry;F)F"
        )
    )
    private float sadnes$removeNauseaRender(ClientPlayerEntity clientPlayerEntity, RegistryEntry<StatusEffect> registryEntry, float f) {
        return Removals.isRemoved("bad_effects") ? 0.0F : clientPlayerEntity.getEffectFadeFactor(registryEntry, f);
    }

    @ModifyArg(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/RawProjectionMatrix;set(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
        )
    )
    private Matrix4f sadnes$ambienceCaptureProjection(Matrix4f matrix4f) {
        AmbienceRenderer.setSkyParams(matrix4f.m00(), matrix4f.m11());
        MotionBlurRenderer.INSTANCE.setProjection(matrix4f);
        ShaderSkyRenderer.setProjectionMatrix(matrix4f);
        return matrix4f;
    }

    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/systems/ProjectionType;)V",
            ordinal = 1
        )
    )
    private void sadnes$ambiencePostFx(RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        AmbienceRenderer.INSTANCE.renderPostEffects((GameRenderer)(Object)this);
    }

    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/systems/ProjectionType;)V",
            ordinal = 1
        )
    )
    private void sadnes$motionBlur(RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        MotionBlurRenderer.INSTANCE.render((GameRenderer)(Object)this);
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void sadnes$ambienceWorldPhaseBegin(RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        Ambience.setWorldPhase(true);
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void sadnes$foliageWind(RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        FoliageSwayManager.INSTANCE.update();
    }

    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void sadnes$ambienceWorldPhaseEnd(RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        Ambience.setWorldPhase(false);
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEffectFadeFactor(Lnet/minecraft/registry/entry/RegistryEntry;F)F"
        )
    )
    private float sadnes$removeNauseaTick(ClientPlayerEntity clientPlayerEntity, RegistryEntry<StatusEffect> registryEntry, float f) {
        return Removals.isRemoved("bad_effects") ? 0.0F : clientPlayerEntity.getEffectFadeFactor(registryEntry, f);
    }

    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true)
    private static void sadnes$fullBrightNightVisionStrength(LivingEntity livingEntity, float f, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        if (FullBright.isNightVision()) {
            callbackInfoReturnable.setReturnValue(1.0F);
        }
    }

    @ModifyArg(method = "getBasicProjectionMatrix", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"), index = 1)
    private float sadnes$aspectRatio(float f) {
        return AspectRatio.getAspectRatio(f);
    }
}
