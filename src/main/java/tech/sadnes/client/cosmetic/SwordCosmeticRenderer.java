package tech.sadnes.client.cosmetic;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;

public final class SwordCosmeticRenderer {
    private static Cosmetic current;
    private static boolean rendered;

    private SwordCosmeticRenderer() {
    }

    public static void prepare(Cosmetic[] cosmetics, ItemStack itemStack) {
        Cosmetic cosmetic = cosmetics == null ? null : cosmetics[CosmeticType.SWORD.ordinal()];
        if (cosmetic != null && itemStack.isIn(ItemTags.SWORDS)) {
            current = cosmetic;
            rendered = false;
        } else {
            current = null;
            rendered = false;
        }
    }

    public static void clear() {
        current = null;
    }

    public static boolean render(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i) {
        Cosmetic cosmetic = current;
        if (cosmetic == null) {
            return false;
        }

        if (rendered) {
            return true;
        }

        rendered = true;
        double d = Util.getMeasuringTimeMs() / 1000.0;
        float f = 0.72F * cosmetic.scale();
        matrixStack.push();
        matrixStack.translate(0.25F, 0.25F, 0.5F);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45.0F));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        matrixStack.scale(f, f, f);
        matrixStack.translate(cosmetic.offsetX(), cosmetic.offsetY(), cosmetic.offsetZ());
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        orderedRenderCommandQueue.submitCustom(
            matrixStack, cosmetic.layer(), (entry, vertexConsumer) -> cosmetic.model().render(entry, vertexConsumer, CosmeticModel.Pose.IDLE, d, i, OverlayTexture.DEFAULT_UV, -1)
        );
        matrixStack.pop();
        return true;
    }
}
