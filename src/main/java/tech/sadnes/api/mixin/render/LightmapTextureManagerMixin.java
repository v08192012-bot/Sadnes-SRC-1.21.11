package tech.sadnes.api.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.feature.impl.render.FullBright;
import tech.sadnes.client.feature.impl.render.Removals;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;
import tech.sadnes.client.render.texture.WhiteLightmap;
import tech.sadnes.client.util.color.ColorUtil;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Inject(method = "getDarkness", at = @At("HEAD"), cancellable = true)
    private void sadnes$removeDarknessLightmap(LivingEntity livingEntity, float f, float f2, CallbackInfoReturnable<Float> callbackInfoReturnable) {
        if (Removals.isRemoved("bad_effects")) {
            callbackInfoReturnable.setReturnValue(0.0F);
        }
    }

    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEffectFadeFactor(Lnet/minecraft/registry/entry/RegistryEntry;F)F"
        )
    )
    private float sadnes$removeDarknessScale(ClientPlayerEntity clientPlayerEntity, RegistryEntry<StatusEffect> registryEntry, float f) {
        return Removals.isRemoved("bad_effects") ? 0.0F : clientPlayerEntity.getEffectFadeFactor(registryEntry, f);
    }

    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float sadnes$fullBrightGamma(float f) {
        return FullBright.isGamma() ? 10.0F : f;
    }

    @ModifyExpressionValue(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z",
            ordinal = 0
        )
    )
    private boolean sadnes$fullBrightNightVision(boolean flag) {
        return flag || FullBright.isNightVision();
    }

    @ModifyArg(
        method = "<init>",
        index = 2,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/MappableRingBuffer;<init>(Ljava/util/function/Supplier;II)V")
    )
    private int sadnes$enlargeLightmapUbo(int i) {
        return i + 32;
    }

    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putVec3(Lorg/joml/Vector3fc;)Lcom/mojang/blaze3d/buffers/Std140Builder;",
            ordinal = 1
        )
    )
    private Std140Builder sadnes$appendAmbienceTints(Std140Builder std140Builder, Vector3fc vector3fc) {
        std140Builder.putVec3(vector3fc);
        Ambience ambience = Ambience.getInstance();
        int i;
        int i2;
        boolean flag;
        if (ambience == null) {
            i = -1;
            i2 = -1;
            flag = false;
        } else {
            i = ambience.getBlockLightColor();
            i2 = ambience.getWorldTintColor();
            flag = i != -1 || i2 != -1;
        }

        Ambience.setLightmapOverridden(flag);
        std140Builder.putVec3(ColorUtil.redf(i), ColorUtil.greenf(i), ColorUtil.bluef(i));
        std140Builder.putVec3(ColorUtil.redf(i2), ColorUtil.greenf(i2), ColorUtil.bluef(i2));
        return std140Builder;
    }

    @ModifyReturnValue(method = "getGlTextureView", at = @At("RETURN"))
    private GpuTextureView sadnes$untintGuiLightmap(GpuTextureView gpuTextureView) {
        return !Ambience.isWorldPhase() && Ambience.isLightmapOverridden() ? WhiteLightmap.getTextureView() : gpuTextureView;
    }
}
