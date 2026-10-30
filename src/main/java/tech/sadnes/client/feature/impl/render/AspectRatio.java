package tech.sadnes.client.feature.impl.render;

import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;

@FeatureInfo(
    name = "AspectRatio",
    description = "feature.impl.render.aspectratio",
    displayName = "feature.impl.render.aspectratio",
    category = Category.VISUALS,
    subcategory = Subcategory.CAMERA
)
public final class AspectRatio extends Feature {
    private final ModeSetting ratio = new ModeSetting("aspectratio.ratio", new String[]{"4:3", "5:4", "16:10", "16:9", "21:9", "1:1", "custom"});
    private final NumberSetting customWidth = new NumberSetting("aspectratio.customWidth", 1.78F, 0.5F, 4.0F, 0.01F, Unit.EMPTY)
        .setVisibleWhen(() -> this.ratio.is("custom"));

    public static AspectRatio getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(AspectRatio.class);
    }

    public static float getAspectRatio(float f) {
        AspectRatio aspectRatio = getInstance();
        return aspectRatio != null && aspectRatio.isEnabled() ? aspectRatio.getRatio() : f;
    }

    private float getRatio() {
        String string = this.ratio.getValue();
        byte b = -1;
        switch (string.hashCode()) {
            case -1349088399:
                if (string.equals("custom")) {
                    b = 5;
                }
                break;
            case 48936:
                if (string.equals("1:1")) {
                    return 1.0F;
                }
                break;
            case 52783:
                if (string.equals("5:4")) {
                    return 1.25F;
                }
                break;
            case 1513508:
                if (string.equals("16:9")) {
                    return 1.7777778F;
                }
                break;
            case 1538494:
                if (string.equals("21:9")) {
                    return 2.3333333F;
                }
                break;
            case 46918548:
                if (string.equals("16:10")) {
                    return 1.6F;
                }
        }

        switch (b) {
            case 0:
                return 1.25F;
            case 1:
                return 1.6F;
            case 2:
                return 1.7777778F;
            case 3:
                return 2.3333333F;
            case 4:
                return 1.0F;
            case 5:
                return this.customWidth.getValue();
            default:
                return 1.3333334F;
        }
    }
}
