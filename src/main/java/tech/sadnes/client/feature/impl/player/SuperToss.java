package tech.sadnes.client.feature.impl.player;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PreRotationEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.util.math.Rotation;

@FeatureInfo(
    name = "SuperToss",
    description = "feature.impl.player.supertoss",
    displayName = "feature.impl.player.supertoss",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_ITEMS
)
public final class SuperToss extends Feature {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private final MultiSetting items = new MultiSetting(
        "supertoss.items",
        BooleanSetting.of("pearl", true),
        BooleanSetting.of("snowball", true),
        BooleanSetting.of("egg", true),
        BooleanSetting.of("potion", true),
        BooleanSetting.of("windcharge", true)
    );

    public static SuperToss getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(SuperToss.class);
    }

    @EventHandler
    public void onRotationUpdate(PreRotationEvent preRotationEvent) {
        if (mc.player != null) {
            if (mc.currentScreen != null) {
                return;
            }

            if (mc.options.useKey.isPressed()) {
                float f = this.getCorrectedYaw();
                if (!Float.isNaN(f)) {
                    float f2 = Rotation.getCameraPitch();
                    RotationComponent.rotateTo(new Rotation(f, f2), 360.0F, 360.0F, 1, 10);
                    return;
                }
            }
        }
    }

    public float getCorrectedYaw() {
        if (this.isEnabled() && mc.player != null) {
            SuperToss.Projectile projectile = this.getEnabledProjectile(mc.player.getMainHandStack().getItem());
            if (projectile == null) {
                projectile = this.getEnabledProjectile(mc.player.getOffHandStack().getItem());
            }

            if (projectile == null) {
                return Float.NaN;
            }

            float f = Rotation.getCameraYaw();
            float f2 = this.computeYawOffset(f, Rotation.getCameraPitch(), projectile);
            return Math.abs(f2) < 0.05F ? Float.NaN : f + f2;
        } else {
            return Float.NaN;
        }
    }

    private float computeYawOffset(float f, float f2, SuperToss.Projectile projectile) {
        double d = Math.cos(f2 * (float) (Math.PI / 180.0));
        double d2 = Math.sin((f2 + projectile.roll()) * (float) (Math.PI / 180.0));
        double d3 = Math.sqrt(d * d + d2 * d2);
        if (d3 <= 1.0E-6) {
            return 0.0F;
        }

        double d4 = projectile.speed() * d / d3;
        if (d4 <= 1.0E-4) {
            return 0.0F;
        }

        Vec3d vec3d = mc.player.getMovement();
        double d5 = vec3d.x * Math.cos(f * (float) (Math.PI / 180.0)) + vec3d.z * Math.sin(f * (float) (Math.PI / 180.0));
        double d6 = MathHelper.clamp(d5 / d4, -1.0, 1.0);
        return (float)(Math.asin(d6) / (float) (Math.PI / 180.0));
    }

    private SuperToss.Projectile getEnabledProjectile(Item item) {
        SuperToss.Projectile projectile = getProjectile(item);
        return projectile != null && this.items.has(projectile.id()) ? projectile : null;
    }

    private static SuperToss.Projectile getProjectile(Item item) {
        if (item == Items.ENDER_PEARL) {
            return new SuperToss.Projectile("pearl", 1.5F, 0.0F);
        } else if (item == Items.SNOWBALL) {
            return new SuperToss.Projectile("snowball", 1.5F, 0.0F);
        } else if (item == Items.EGG || item == Items.BLUE_EGG || item == Items.BROWN_EGG) {
            return new SuperToss.Projectile("egg", 1.5F, 0.0F);
        } else if (item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            return new SuperToss.Projectile("potion", 0.5F, -20.0F);
        } else {
            return item == Items.WIND_CHARGE ? new SuperToss.Projectile("windcharge", 1.5F, 0.0F) : null;
        }
    }

    record Projectile(String id, float speed, float roll) {
    }
}
