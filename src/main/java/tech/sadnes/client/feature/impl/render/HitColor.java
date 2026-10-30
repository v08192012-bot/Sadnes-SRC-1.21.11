package tech.sadnes.client.feature.impl.render;

import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.ColorSetting;

@FeatureInfo(
    name = "HitColor",
    description = "feature.impl.render.hitcolor",
    displayName = "feature.impl.render.hitcolor",
    category = Category.VISUALS,
    subcategory = Subcategory.ENTITIES
)
public final class HitColor extends Feature {
    private final ColorSetting color = new ColorSetting("hitcolor.color", -16718337);

    public static HitColor getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(HitColor.class);
    }

    public int getColor() {
        return this.color.getValue();
    }
}
