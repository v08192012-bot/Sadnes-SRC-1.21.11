package tech.sadnes.client.feature.impl.render;

import net.minecraft.client.option.Perspective;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.FreeLookComponent;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.util.math.StopWatch;

@FeatureInfo(
    name = "FreeLook",
    description = "feature.impl.render.freelook",
    displayName = "feature.impl.render.freelook",
    category = Category.VISUALS,
    subcategory = Subcategory.CAMERA
)
public final class FreeLook extends Feature {
    private final ModeSetting mode = new ModeSetting("freelook.mode", new String[]{"camera", "head"});
    private final StopWatch returnTimer = new StopWatch();
    private Perspective savedPerspective = Perspective.FIRST_PERSON;

    public static FreeLook getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(FreeLook.class);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.options != null) {
            this.savedPerspective = mc.options.getPerspective();
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }

        FreeLookComponent.setHeadActive(true);
        this.returnTimer.reset();
    }

    @Override
    public void onEnable() {
        if (this.mode.is("head") && mc.player != null) {
            FreeLookComponent.setYaw(mc.player.getYaw());
            FreeLookComponent.setPitch(mc.player.getPitch());
        }

        FreeLookComponent.setHeadActive(false);
        if (mc.options != null) {
            mc.options.setPerspective(this.savedPerspective);
        }

        this.returnTimer.delay(400L);
        super.onEnable();
    }

    public ModeSetting getMode() {
        return this.mode;
    }

    public StopWatch getReturnTimer() {
        return this.returnTimer;
    }

    public Perspective getSavedPerspective() {
        return this.savedPerspective;
    }
}
