package tech.sadnes.api.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.api.duck.TumbleHolder;
import tech.sadnes.api.duck.TumbleState;
import tech.sadnes.client.feature.impl.render.ItemPhysics;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void sadnes$captureTumble(ItemEntity itemEntity, ItemEntityRenderState itemEntityRenderState, float f, CallbackInfo callbackInfo) {
        TumbleState tumbleState = (TumbleState)itemEntityRenderState;
        boolean flag;
        if (ItemPhysics.isActive()) {
            flag = !itemEntityRenderState.itemRenderState.isEmpty();
        } else {
            flag = false;
        }

        tumbleState.sadnes$setTumbling(flag);
        if (flag) {
            boolean flag2 = sadnes$isFlat(itemEntityRenderState.itemRenderState.getModelBoundingBox());
            tumbleState.sadnes$setTumblePitch(((TumbleHolder)itemEntity).sadnes$tumble().getAngle(f, flag2));
        }
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private void sadnes$placeTumbled(
        MatrixStack matrixStack, float f, float f2, float f3, Operation<Void> operation, @Local(argsOnly = true) ItemEntityRenderState itemEntityRenderState, @Local Box box
    ) {
        TumbleState tumbleState = (TumbleState)itemEntityRenderState;
        if (!tumbleState.sadnes$tumbling()) {
            operation.call(matrixStack, f, f2, f3);
        } else {
            float f4 = tumbleState.sadnes$tumblePitch() * (float) (Math.PI / 180.0);
            float f5 = (float)box.getLengthZ();
            if (sadnes$isFlat(box)) {
                f5 *= 1.0F + 1.5F * Math.max(itemEntityRenderState.renderedAmount - 1, 0);
            }

            float f6 = (float)box.getLengthY() * Math.abs(MathHelper.cos(f4)) + f5 * Math.abs(MathHelper.sin(f4));
            Vec3d vec3d = box.getCenter();
            matrixStack.translate(0.0F, f6 / 2.0F + 0.02F, 0.0F);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation(itemEntityRenderState.uniqueOffset));
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotation(f4));
            matrixStack.translate((float)(-vec3d.x), (float)(-vec3d.y), (float)(-vec3d.z));
        }
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionfc;)V"))
    private void sadnes$skipVanillaSpin(MatrixStack matrixStack, Quaternionfc quaternionfc, Operation<Void> operation, @Local(argsOnly = true) ItemEntityRenderState itemEntityRenderState) {
        if (!((TumbleState)itemEntityRenderState).sadnes$tumbling()) {
            operation.call(matrixStack, quaternionfc);
        }
    }

    @Unique
    private static boolean sadnes$isFlat(Box box) {
        return box.getLengthZ() <= 0.0625;
    }
}
