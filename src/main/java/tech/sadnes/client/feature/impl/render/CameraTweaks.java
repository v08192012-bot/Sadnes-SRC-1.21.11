package tech.sadnes.client.feature.impl.render;

import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.ScreenMouseScrollEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.Key;

@FeatureInfo(
    name = "CameraTweaks",
    description = "feature.impl.render.cameratweaks",
    displayName = "feature.impl.render.cameratweaks",
    category = Category.VISUALS,
    subcategory = Subcategory.CAMERA
)
public final class CameraTweaks extends Feature {
    public final BooleanSetting disableCollisions = new BooleanSetting("cameratweaks.disableCollisions", true);
    public final NumberSetting distance = new NumberSetting("cameratweaks.distance", 4.0F, 1.0F, 10.0F, 0.1F).setVisibleWhen(this.disableCollisions::getValue);
    public final KeybindSetting zoomBind = new KeybindSetting("cameratweaks.zoomBind");
    private final Animation zoomAnimation = new Animation();
    private final Animation zoomFovAnimation = new Animation();
    private float zoomAmount = 50.0F;

    public CameraTweaks() {
        this.zoomFovAnimation.forceValue(this.zoomAmount);
    }

    public static CameraTweaks getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(CameraTweaks.class);
    }

    @EventHandler
    public void onMouseScroll(ScreenMouseScrollEvent screenMouseScrollEvent) {
        if (this.isZooming()) {
            this.zoomAmount = Math.max(30.0F, Math.min(90.0F, this.zoomAmount + (float)screenMouseScrollEvent.getVerticalAmount() * 5.0F));
            this.zoomFovAnimation.animate(this.zoomAmount, 0.18, Easings.QUART_OUT);
            screenMouseScrollEvent.setCancelled(true);
        }
    }

    public float applyZoom(float f) {
        this.zoomAnimation.update();
        this.zoomAnimation.animate(this.isZooming() ? 1.0 : 0.0, 0.18, Easings.QUART_OUT);
        this.zoomFovAnimation.update();
        double d = this.zoomAnimation.getValue();
        return d <= 1.0E-4 ? f : (float)Math.max(1.0, f - this.zoomFovAnimation.getValue() * d);
    }

    private boolean isZooming() {
        if (this.isEnabled() && mc.currentScreen == null) {
            if (Key.isPressed(this.zoomBind.getDefaultValue())) {
                return true;
            }

            for (ValueBind valueBind : this.zoomBind.getBinds()) {
                if (Key.isPressed(valueBind.getKey())) {
                    return true;
                }
            }
        }

        return false;
    }

    public BooleanSetting getDisableCollisions() {
        return this.disableCollisions;
    }

    public NumberSetting getDistance() {
        return this.distance;
    }

    public KeybindSetting getZoomBind() {
        return this.zoomBind;
    }

    public Animation getZoomAnimation() {
        return this.zoomAnimation;
    }

    public Animation getZoomFovAnimation() {
        return this.zoomFovAnimation;
    }

    public float getZoomAmount() {
        return this.zoomAmount;
    }
}
