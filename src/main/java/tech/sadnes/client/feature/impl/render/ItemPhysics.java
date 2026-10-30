package tech.sadnes.client.feature.impl.render;

import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;

@FeatureInfo(
    name = "ItemPhysics",
    description = "feature.impl.render.itemphysics",
    displayName = "feature.impl.render.itemphysics",
    category = Category.VISUALS,
    subcategory = Subcategory.WORLD
)
public final class ItemPhysics extends Feature {
    private final NumberSetting spinSpeed = new NumberSetting("itemphysics.spinSpeed", 100.0F, 25.0F, 300.0F, 25.0F, Unit.PERCENT);

    public static ItemPhysics getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(ItemPhysics.class);
    }

    public static boolean isActive() {
        ItemPhysics itemPhysics = getInstance();
        return itemPhysics != null && itemPhysics.isEnabled();
    }

    public float getSpinSpeed() {
        return 20.0F * this.spinSpeed.getValue() / 100.0F;
    }
}
