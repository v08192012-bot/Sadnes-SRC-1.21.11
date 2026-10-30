package tech.sadnes.client.feature.impl.render;

import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.ChunkAnimations;
import tech.sadnes.client.animation.HudAnimations;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;

@FeatureInfo(
    name = "Animations",
    description = "feature.impl.render.animations",
    displayName = "feature.impl.render.animations",
    category = Category.VISUALS,
    subcategory = Subcategory.WORLD
)
public final class Animations extends Feature {
    public final MultiSetting elements = new MultiSetting(
            "animations.elements",
            BooleanSetting.of("containers", true),
            BooleanSetting.of("perspective", true),
            BooleanSetting.of("chat", true),
            BooleanSetting.of("chat_open", true),
            BooleanSetting.of("typing", true),
            BooleanSetting.of("tab", true),
            BooleanSetting.of("chunks", true)
        )
        .setOnChange(this::syncChunkAnimations);

    public static Animations getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(Animations.class);
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        ChunkAnimations.INSTANCE.clear();
    }

    @EventHandler
    public void onRender2D(RenderEvent renderEvent) {
        if (renderEvent.isHudPost()) {
            HudAnimations.INSTANCE.renderClosingTabList(renderEvent.getContext());
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.syncChunkAnimations();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ChunkAnimations.INSTANCE.setEnabled(false);
    }

    public boolean isEnabled(String string) {
        return this.isEnabled() && this.elements.has(string);
    }

    private void syncChunkAnimations() {
        ChunkAnimations chunkAnimations = ChunkAnimations.INSTANCE;
        if (this.isEnabled() && this.elements.has("chunks")) {
            chunkAnimations.setEnabled(true);
        } else {
            chunkAnimations.setEnabled(false);
        }
    }

    public MultiSetting getElements() {
        return this.elements;
    }
}
