package tech.sadnes.client.cosmetic;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import tech.sadnes.client.feature.impl.render.Cosmetics;
import tech.sadnes.client.feature.impl.render.TransparentPlayers;
import tech.sadnes.client.figura.FiguraAvatarManager;
import tech.sadnes.client.util.color.ColorUtil;

public final class CosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> featureRendererContext) {
        super(featureRendererContext);
    }

    public void render(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, PlayerEntityRenderState playerEntityRenderState, float f, float f2) {
        Cosmetic[] cosmetics = playerEntityRenderState.getData(Cosmetics.COSMETICS_KEY);
        if (!playerEntityRenderState.invisible || !playerEntityRenderState.invisibleToPlayer) {
            if (cosmetics != null) {
                CosmeticModel.Pose pose = playerEntityRenderState.isGliding
                    ? CosmeticModel.Pose.GLIDE
                    : (
                        playerEntityRenderState.isSwimming
                            ? CosmeticModel.Pose.SWIM
                            : (
                                playerEntityRenderState.isInSneakingPose
                                    ? CosmeticModel.Pose.SNEAK
                                    : (playerEntityRenderState.limbSwingAmplitude > 0.15F ? CosmeticModel.Pose.MOVE : CosmeticModel.Pose.IDLE)
                            )
                    );
                double d = Util.getMeasuringTimeMs() / 1000.0;
                int i2 = LivingEntityRenderer.getOverlay(playerEntityRenderState, 0.0F);
                int i3 = ColorUtil.multiplyAlpha(-1, TransparentPlayers.getRenderAlpha());
                PlayerEntityModel playerEntityModel = this.getContextModel();
                float f3 = Cosmetic.headLift(FiguraAvatarManager.getHeadHeight(playerEntityRenderState));

                for (Cosmetic cosmetic : cosmetics) {
                    if (cosmetic != null && cosmetic.anchor() != Cosmetic.Anchor.HAND) {
                        matrixStack.push();
                        switch (cosmetic.anchor()) {
                            case HEAD:
                            case HEAD_TOP:
                                playerEntityModel.head.applyTransform(matrixStack);
                                break;
                            case BODY:
                                playerEntityModel.body.applyTransform(matrixStack);
                            case ROOT:
                            case HAND:
                        }

                        float f4 = cosmetic.isOnHead() ? f3 : 0.0F;
                        matrixStack.translate(cosmetic.offsetX(), cosmetic.anchor().getYOffset() - cosmetic.offsetY() - f4, cosmetic.offsetZ());
                        matrixStack.scale(cosmetic.scale(), cosmetic.scale(), cosmetic.scale());
                        orderedRenderCommandQueue.submitCustom(matrixStack, cosmetic.layer(), (entry, vertexConsumer) -> cosmetic.model().render(entry, vertexConsumer, pose, d, i, i2, i3));
                        matrixStack.pop();
                    }
                }
            }
        }
    }
}
