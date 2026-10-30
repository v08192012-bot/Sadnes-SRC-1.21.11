package tech.sadnes.client.feature.impl.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Vector4f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.TargetingMarginEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.player.CombatUtil;

@FeatureInfo(
    name = "Hitbox",
    description = "feature.impl.combat.hitbox",
    displayName = "feature.impl.combat.hitbox",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_TWEAKS
)
public final class Hitbox extends Feature {
    private final MultiSetting targets = new MultiSetting("hitbox.targets", BooleanSetting.of("players", true), BooleanSetting.of("mobs", true));
    private final NumberSetting players = new NumberSetting("hitbox.players", 0.3F, 0.0F, 2.0F, 0.05F, Unit.EMPTY).setVisibleWhen(() -> this.targets.has("players"));
    private final NumberSetting mobs = new NumberSetting("hitbox.mobs", 0.3F, 0.0F, 2.0F, 0.05F, Unit.EMPTY).setVisibleWhen(() -> this.targets.has("mobs"));
    private final ModeSetting bypass = new ModeSetting("hitbox.bypass", new String[]{"none", "snap"}).setValue("none");

    public static Hitbox getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(Hitbox.class);
    }

    @EventHandler
    public void onTargetingMargin(TargetingMarginEvent targetingMarginEvent) {
        if (targetingMarginEvent.getEntity() instanceof LivingEntity livingEntity) {
            targetingMarginEvent.setMargin(targetingMarginEvent.getMargin() + this.getExpansion(livingEntity));
        }
    }

    @EventHandler
    public void onAttack(AttackEntityEvent attackEntityEvent) {
        if (!this.bypass.isNot("snap") && attackEntityEvent.getStage() == AttackEntityEvent.Stage.PRE && mc.player != null) {
            if (attackEntityEvent.getTarget() instanceof LivingEntity livingEntity && !(this.getExpansion(livingEntity) <= 0.0F)) {
                Vector4f vector4f = CombatUtil.getRotationsTo(livingEntity, 3.0F, true);
                RotationComponent.rotateTo(new Rotation(vector4f.x, vector4f.y), 180.0F, 180.0F, 0, 3);
            }
        }
    }

    private float getExpansion(LivingEntity livingEntity) {
        if (livingEntity == mc.player) {
            return 0.0F;
        }

        if (livingEntity instanceof PlayerEntity playerEntity) {
            if (Sadnes.getInstance().getFriendManager() != null && Sadnes.getInstance().getFriendManager().isFriend(playerEntity.getGameProfile().name())) {
                return 0.0F;
            }

            if (this.targets.has("players")) {
                return this.players.getValue();
            }
        } else if (livingEntity instanceof MobEntity && this.targets.has("mobs")) {
            return this.mobs.getValue();
        }

        return 0.0F;
    }
}
