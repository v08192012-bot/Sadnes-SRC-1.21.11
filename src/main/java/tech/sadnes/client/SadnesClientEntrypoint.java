package tech.sadnes.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import tech.sadnes.client.cosmetic.CosmeticFeatureRenderer;
import tech.sadnes.client.render.EntityPreviewRenderer;
import tech.sadnes.client.render.world.FoliageSwayModel;

public final class SadnesClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        this.disableDevelopmentMode();
        this.registerSpecialGuiElements();
        this.registerFeatureRenderers();
        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, FoliageSwayModel::wrap));
        Sadnes.getInstance().initialize();
    }

    private void disableDevelopmentMode() {
        SharedConstants.isDevelopment = false;
    }

    private void registerFeatureRenderers() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, livingEntityRenderer, registrationHelper, context) -> {
            if (livingEntityRenderer instanceof PlayerEntityRenderer playerEntityRenderer) {
                registrationHelper.register(new CosmeticFeatureRenderer(playerEntityRenderer));
            }
        });
    }

    private void registerSpecialGuiElements() {
        SpecialGuiElementRegistry.register(context -> new EntityPreviewRenderer(context.vertexConsumers(), context.client().getEntityRenderDispatcher()));
    }
}
