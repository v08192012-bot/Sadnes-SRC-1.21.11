package tech.sadnes.client.feature.impl.render;

import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.NumberSetting;

@FeatureInfo(
    name = "MotionBlur",
    description = "feature.impl.render.motionblur",
    displayName = "feature.impl.render.motionblur",
    category = Category.VISUALS,
    subcategory = Subcategory.CAMERA
)
public final class MotionBlur extends Feature {
    private final NumberSetting strength = new NumberSetting("motionblur.strength", 1.0F, 0.1F, 3.0F, 0.1F);

    public static MotionBlur getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(MotionBlur.class);
    }

    public NumberSetting getStrength() {
        return this.strength;
    }
}
