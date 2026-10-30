package tech.sadnes.client.feature.impl.render;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.api.duck.TransparentAlphaHolder;
import tech.sadnes.api.duck.TransparentSmoothHolder;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.math.Interpolation;

@FeatureInfo(
    name = "TransparentPlayers",
    description = "feature.impl.render.transparentplayers",
    displayName = "feature.impl.render.transparentplayers",
    category = Category.VISUALS,
    subcategory = Subcategory.ENTITIES
)
public final class TransparentPlayers extends Feature {
    private static final ThreadLocal<Float> RENDER_ALPHA = ThreadLocal.withInitial(() -> 1.0F);
    private final NumberSetting distance = new NumberSetting("transparentplayers.distance", 8.0F, 2.0F, 32.0F, 0.5F, Unit.METER);

    public static TransparentPlayers getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(TransparentPlayers.class);
    }

    @Override
    public boolean enabled() {
        return this.isEnabled();
    }

    private float computeTargetAlpha(LivingEntityRenderState livingEntityRenderState) {
        if (this.enabled() && mc.player != null && livingEntityRenderState.entityType == EntityType.PLAYER) {
            double d = this.distance.getValue().floatValue();
            Vec3d vec3d = mc.player.getLerpedPos(mc.getRenderTickCounter().getTickProgress(false));
            double d2 = vec3d.squaredDistanceTo(livingEntityRenderState.x, livingEntityRenderState.y, livingEntityRenderState.z);
            if (d2 > d * d) {
                return 1.0F;
            }

            float f = MathHelper.clamp((float)(Math.sqrt(d2) / d), 0.0F, 1.0F);
            float f2 = f * f * (3.0F - 2.0F * f);
            return MathHelper.clamp(f2 * 0.99F, 0.0F, 0.99F);
        } else {
            return 1.0F;
        }
    }

    public float getSmoothedAlpha(LivingEntity livingEntity, LivingEntityRenderState livingEntityRenderState) {
        if (livingEntity != mc.player && livingEntity instanceof PlayerEntity) {
            float f = this.computeTargetAlpha(livingEntityRenderState);
            if (livingEntity instanceof TransparentSmoothHolder transparentSmoothHolder) {
                float f2 = transparentSmoothHolder.sadnes$transparentSmooth();
                if (f2 < 0.0F) {
                    transparentSmoothHolder.sadnes$setTransparentSmooth(f);
                    return f;
                } else {
                    float f3 = mc.getRenderTickCounter().getDynamicDeltaTicks();
                    float f4 = MathHelper.clamp(1.0F - (float)Math.exp(-0.2F * f3), 0.0F, 1.0F);
                    float f5 = MathHelper.clamp(Interpolation.interpolate(f2, f, f4), 0.0F, 1.0F);
                    transparentSmoothHolder.sadnes$setTransparentSmooth(f5);
                    return f5;
                }
            } else {
                return f;
            }
        } else {
            return 1.0F;
        }
    }

    public float getAlpha(LivingEntityRenderState livingEntityRenderState) {
        return this.enabled() && livingEntityRenderState.entityType == EntityType.PLAYER
            ? MathHelper.clamp(((TransparentAlphaHolder)livingEntityRenderState).sadnes$getTransparentAlpha(), 0.0F, 1.0F)
            : 1.0F;
    }

    public boolean isTransparent(LivingEntityRenderState livingEntityRenderState) {
        return this.enabled() && livingEntityRenderState.entityType == EntityType.PLAYER && this.getAlpha(livingEntityRenderState) < 1.0F;
    }

    public int applyRenderAlpha(int i) {
        return ColorUtil.multiplyAlpha(i, getRenderAlpha());
    }

    public int applyStateAlpha(int i, LivingEntityRenderState livingEntityRenderState) {
        return ColorUtil.setAlphaf(i, this.getAlpha(livingEntityRenderState));
    }

    public static void setRenderAlpha(float f) {
        RENDER_ALPHA.set(MathHelper.clamp(f, 0.0F, 1.0F));
    }

    public static void resetRenderAlpha() {
        RENDER_ALPHA.set(1.0F);
    }

    public static float getRenderAlpha() {
        return RENDER_ALPHA.get();
    }
}
