package tech.sadnes.client.feature.impl.render;

import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.ModeSetting;

@FeatureInfo(
    name = "FullBright",
    description = "feature.impl.render.fullbright",
    displayName = "feature.impl.render.fullbright",
    category = Category.VISUALS,
    subcategory = Subcategory.WORLD
)
public final class FullBright extends Feature {
    private final ModeSetting mode = new ModeSetting("fullbright.mode", new String[]{"gamma", "night_vision"});

    public static FullBright getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(FullBright.class);
    }

    public static boolean isGamma() {
        FullBright fullBright = getInstance();
        return fullBright != null && fullBright.isEnabled() && fullBright.mode.is("gamma");
    }

    public static boolean isNightVision() {
        FullBright fullBright = getInstance();
        return fullBright != null && fullBright.isEnabled() && fullBright.mode.is("night_vision");
    }

    public ModeSetting getMode() {
        return this.mode;
    }
}
