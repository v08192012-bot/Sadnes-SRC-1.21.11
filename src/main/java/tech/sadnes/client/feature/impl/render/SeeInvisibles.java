package tech.sadnes.client.feature.impl.render;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.color.ColorUtil;

@FeatureInfo(
    name = "SeeInvisibles",
    description = "feature.impl.render.seeinvisibles",
    displayName = "feature.impl.render.seeinvisibles",
    category = Category.VISUALS,
    subcategory = Subcategory.ENTITIES
)
public final class SeeInvisibles extends Feature {
    private final NumberSetting alpha = new NumberSetting("seeinvisibles.alpha", 75.0F, 1.0F, 100.0F, 1.0F, Unit.PERCENT);

    public static SeeInvisibles getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(SeeInvisibles.class);
    }

    @Override
    public boolean enabled() {
        return this.isEnabled();
    }

    public boolean shouldRender(LivingEntity livingEntity) {
        return this.enabled() && livingEntity instanceof PlayerEntity && livingEntity.isInvisible();
    }

    public boolean shouldRenderState(LivingEntityRenderState livingEntityRenderState) {
        return this.enabled() && livingEntityRenderState.entityType == EntityType.PLAYER && livingEntityRenderState.invisible;
    }

    public int applyAlpha(int i) {
        return ColorUtil.setAlphaf(i, this.alpha.getValue() / 100.0F);
    }
}
