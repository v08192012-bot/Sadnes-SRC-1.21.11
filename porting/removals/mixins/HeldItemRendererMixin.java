package tech.sadnes.api.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.animation.PerspectiveAnimation;
import tech.sadnes.client.cosmetic.SwordCosmeticRenderer;
import tech.sadnes.client.feature.impl.misc.FreeCam;
import tech.sadnes.client.feature.impl.render.Cosmetics;
import tech.sadnes.client.feature.impl.render.Removals;
import tech.sadnes.client.feature.impl.render.hand.HandGlow;
import tech.sadnes.client.feature.impl.render.hand.HandOffset;
import tech.sadnes.client.render.HandCaptureRenderer;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"), cancellable = true)
    private void sadnes$hideHandInFreeCam(float f, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, ClientPlayerEntity clientPlayerEntity, int i, CallbackInfo callbackInfo) {
        FreeCam freeCam = FreeCam.getInstance();
        if (freeCam != null && freeCam.isEnabled()) {
            callbackInfo.cancel();
        }

        if (PerspectiveAnimation.INSTANCE.isThirdPerson()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"))
    private void sadnes$handGlow(float f, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, ClientPlayerEntity clientPlayerEntity, int i, CallbackInfo callbackInfo) {
        if (!HandCaptureRenderer.isCapturing()) {
            FreeCam freeCam = FreeCam.getInstance();
            if (freeCam == null || !freeCam.isEnabled()) {
                HandGlow handGlow = HandGlow.getInstance();
                if (handGlow != null) {
                    handGlow.renderHandGlow(f, matrixStack, (HeldItemRenderer)(Object)this, clientPlayerEntity, i);
                }

                HandOffset handOffset = HandOffset.getInstance();
                if (handOffset != null) {
                    handOffset.renderEditorHand(f, matrixStack, (HeldItemRenderer)(Object)this, clientPlayerEntity, i);
                }
            }
        }
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void sadnes$handSwing(float f, MatrixStack matrixStack, int i, Arm arm, CallbackInfo callbackInfo) {
        HandOffset handOffset = HandOffset.getInstance();
        if (handOffset != null && handOffset.applySwingAnimation(matrixStack, arm, f)) {
            callbackInfo.cancel();
        }
    }

    @ModifyVariable(method = "swingArm", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float sadnes$ghostSwingProgress(float f) {
        float f2 = HandCaptureRenderer.getCaptureProgress();
        return f2 >= 0.0F ? f2 : f;
    }

    @Inject(method = "applyEquipOffset", at = @At("HEAD"), cancellable = true)
    private void sadnes$skipEquipForSwing(MatrixStack matrixStack, Arm arm, float f, CallbackInfo callbackInfo) {
        HandOffset handOffset = HandOffset.getInstance();
        if (handOffset != null && handOffset.isSwingActive()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void sadnes$captureSingleHand(
        AbstractClientPlayerEntity abstractClientPlayerEntity,
        float f,
        float f2,
        Hand hand,
        float f3,
        ItemStack itemStack,
        float f4,
        MatrixStack matrixStack,
        OrderedRenderCommandQueue orderedRenderCommandQueue,
        int i,
        CallbackInfo callbackInfo
    ) {
        Hand hand2 = HandCaptureRenderer.getCaptureHand();
        if (hand2 != null && hand != hand2) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", shift = Shift.AFTER, target = "Lnet/minecraft/client/util/math/MatrixStack;push()V"))
    private void sadnes$handOffset(
        AbstractClientPlayerEntity abstractClientPlayerEntity,
        float f,
        float f2,
        Hand hand,
        float f3,
        ItemStack itemStack,
        float f4,
        MatrixStack matrixStack,
        OrderedRenderCommandQueue orderedRenderCommandQueue,
        int i,
        CallbackInfo callbackInfo
    ) {
        HandOffset handOffset = HandOffset.getInstance();
        if (handOffset != null) {
            handOffset.applyOffset(matrixStack, hand, itemStack);
            if (abstractClientPlayerEntity instanceof ClientPlayerEntity clientPlayerEntity) {
                handOffset.markHandRendered(hand, itemStack, clientPlayerEntity);
            }
        }
    }

    @Inject(
        method = "renderFirstPersonItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
        )
    )
    private void sadnes$handScale(
        AbstractClientPlayerEntity abstractClientPlayerEntity,
        float f,
        float f2,
        Hand hand,
        float f3,
        ItemStack itemStack,
        float f4,
        MatrixStack matrixStack,
        OrderedRenderCommandQueue orderedRenderCommandQueue,
        int i,
        CallbackInfo callbackInfo
    ) {
        HandOffset handOffset = HandOffset.getInstance();
        if (handOffset != null) {
            handOffset.applyScale(matrixStack, hand, itemStack);
        }
    }

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", at = @At("HEAD"))
    private void sadnes$requestHandItemBounds(
        LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, CallbackInfo callbackInfo
    ) {
        HandOffset handOffset = HandOffset.getInstance();
        if (handOffset != null) {
            handOffset.beginItemCapture(itemDisplayContext, itemStack);
        }
    }

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V", at = @At("RETURN"))
    private void sadnes$finishHandItemBounds(
        LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, CallbackInfo callbackInfo
    ) {
        HandOffset handOffset = HandOffset.getInstance();
        if (handOffset != null) {
            handOffset.endItemCapture();
        }
    }

    @WrapOperation(
        method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/ItemRenderState;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V"
        )
    )
    private void sadnes$cosmeticSword(
        ItemRenderState itemRenderState,
        MatrixStack matrixStack,
        OrderedRenderCommandQueue orderedRenderCommandQueue,
        int i,
        int i2,
        int i3,
        Operation<Void> operation,
        @Local(argsOnly = true) LivingEntity livingEntity,
        @Local(argsOnly = true) ItemStack itemStack
    ) {
        Cosmetics cosmetics = Cosmetics.getInstance();
        boolean flag = cosmetics != null && livingEntity instanceof PlayerEntity;
        SwordCosmeticRenderer.prepare(flag ? cosmetics.getCosmetics((PlayerEntity)livingEntity) : null, itemStack);

        try {
            operation.call(itemRenderState, matrixStack, orderedRenderCommandQueue, i, i2, i3);
        } finally {
            SwordCosmeticRenderer.clear();
        }
    }

    @Redirect(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch(F)F"))
    private float sadnes$removeHandPitchSway(ClientPlayerEntity clientPlayerEntity, float f) {
        return Removals.isRemoved("hand_anim") ? MathHelper.lerp(f, clientPlayerEntity.lastRenderPitch, clientPlayerEntity.renderPitch) : clientPlayerEntity.getPitch(f);
    }

    @Redirect(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw(F)F"))
    private float sadnes$removeHandYawSway(ClientPlayerEntity clientPlayerEntity, float f) {
        return Removals.isRemoved("hand_anim") ? MathHelper.lerp(f, clientPlayerEntity.lastRenderYaw, clientPlayerEntity.renderYaw) : clientPlayerEntity.getYaw(f);
    }

    @Redirect(
        method = "applyEatOrDrinkTransformation",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 0)
    )
    private void sadnes$removeEatBobbing(MatrixStack matrixStack, float f, float f2, float f3) {
        if (!Removals.isRemoved("eat_bob")) {
            matrixStack.translate(f, f2, f3);
        }
    }
}
