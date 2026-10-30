package tech.sadnes.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tech.sadnes.api.mixin.accessor.LivingEntityRendererInvoker;

public final class EntityModelBoundsUtil {
    public static EntityModelBoundsUtil.Bounds computeBounds(Entity entity, float f) {
        EntityRenderer entityRenderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (entityRenderer instanceof LivingEntityRenderer livingEntityRenderer && entityRenderer.getAndUpdateRenderState(entity, f) instanceof LivingEntityRenderState livingEntityRenderState) {
            EntityModel entityModel = livingEntityRenderer.getModel();
            entityModel.setAngles(livingEntityRenderState);
            MatrixStack matrixStack = new MatrixStack();
            matrixStack.scale(livingEntityRenderState.baseScale, livingEntityRenderState.baseScale, livingEntityRenderState.baseScale);
            LivingEntityRendererInvoker livingEntityRendererInvoker = (LivingEntityRendererInvoker)livingEntityRenderer;
            livingEntityRendererInvoker.sadnes$setupTransforms(livingEntityRenderState, matrixStack, livingEntityRenderState.bodyYaw, livingEntityRenderState.baseScale);
            matrixStack.scale(-1.0F, -1.0F, 1.0F);
            livingEntityRendererInvoker.sadnes$scale(livingEntityRenderState, matrixStack);
            matrixStack.translate(0.0F, -1.501F, 0.0F);
            float[] floats = new float[]{Float.MAX_VALUE, -Float.MAX_VALUE};
            Vector3f vector3f = new Vector3f();
            entityModel.getRootPart().forEachCuboid(matrixStack, (entry, string, i, cuboid) -> {
                Matrix4f matrix4f = entry.getPositionMatrix();

                for (int i2 = 0; i2 < 8; i2++) {
                    float f2 = ((i2 & 1) == 0 ? cuboid.minX : cuboid.maxX) / 16.0F;
                    float f3 = ((i2 & 2) == 0 ? cuboid.minY : cuboid.maxY) / 16.0F;
                    float f4 = ((i2 & 4) == 0 ? cuboid.minZ : cuboid.maxZ) / 16.0F;
                    matrix4f.transformPosition(f2, f3, f4, vector3f);
                    floats[0] = Math.min(floats[0], vector3f.y);
                    floats[1] = Math.max(floats[1], vector3f.y);
                }
            });
            return floats[0] > floats[1] ? null : new EntityModelBoundsUtil.Bounds(floats[0], floats[1]);
        } else {
            return null;
        }
    }

    private EntityModelBoundsUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public record Bounds(float bottom, float top) {
    }
}
