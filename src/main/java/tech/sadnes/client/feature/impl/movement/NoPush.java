package tech.sadnes.client.feature.impl.movement;

import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;

@FeatureInfo(
    name = "NoPush",
    description = "feature.impl.movement.nopush",
    displayName = "feature.impl.movement.nopush",
    category = Category.MOVEMENT,
    subcategory = Subcategory.MOVEMENT_TERRAIN
)
public final class NoPush extends Feature {
    public final MultiSetting pushes = new MultiSetting(
        "nopush.pushes",
        BooleanSetting.of("blocks", true),
        BooleanSetting.of("mechanisms", false),
        BooleanSetting.of("water", false),
        BooleanSetting.of("bubbles", false),
        BooleanSetting.of("entities", true),
        BooleanSetting.of("fishing_rod", true),
        BooleanSetting.of("world_border", false)
    );

    public static NoPush getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(NoPush.class);
    }
}
