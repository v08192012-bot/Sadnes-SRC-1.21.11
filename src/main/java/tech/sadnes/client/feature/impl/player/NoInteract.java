package tech.sadnes.client.feature.impl.player;

import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;

@FeatureInfo(
    name = "NoInteract",
    description = "feature.impl.player.nointeract",
    displayName = "feature.impl.player.nointeract",
    category = Category.PLAYER,
    subcategory = Subcategory.INTERACT
)
public final class NoInteract extends Feature {
    public final MultiSetting interactions = new MultiSetting(
        "nointeract.interactions", BooleanSetting.of("blocks", true), BooleanSetting.of("entities", false)
    );
    public final BooleanSetting onlyWhenAura = new BooleanSetting("nointeract.onlyWhenAura", true);

    public static NoInteract getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(NoInteract.class);
    }

    public boolean shouldBlockBlocks() {
        boolean flag = this.isActive();
        MultiSetting multiSetting = this.interactions;
        return flag && multiSetting.has("blocks");
    }

    public boolean shouldBlockEntities() {
        boolean flag = this.isActive();
        MultiSetting multiSetting = this.interactions;
        return flag && multiSetting.has("entities");
    }

    private boolean isActive() {
        boolean flag = this.isEnabled();
        BooleanSetting booleanSetting = this.onlyWhenAura;
        if (flag) {
            if (booleanSetting.getValue()) {
                AttackAura attackAura = AttackAura.getInstance();
                return attackAura != null ? attackAura.isEnabled() : false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }
}
