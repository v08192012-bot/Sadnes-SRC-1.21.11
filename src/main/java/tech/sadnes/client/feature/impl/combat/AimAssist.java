package tech.sadnes.client.feature.impl.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.HudPostRenderEvent;
import tech.sadnes.client.event.impl.Render3DEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.TargetComponent;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.RangeSetting;
import tech.sadnes.client.setting.curve.CurvePreset;
import tech.sadnes.client.setting.curve.CurveSetting;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.RaycastUtil;
import tech.sadnes.client.util.rotation.SensitivityUtil;

@FeatureInfo(
    name = "AimAssist",
    description = "feature.impl.combat.aimassist",
    displayName = "feature.impl.combat.aimassist",
    category = Category.COMBAT,
    subcategory = Subcategory.AURA
)
public final class AimAssist extends Feature {
    private final ModeSetting mode = new ModeSetting("aimassist.mode", new String[]{"normal", "adaptive"}).setValue("normal");
    private final MultiSetting targets = new MultiSetting(
        "aimassist.targets",
        BooleanSetting.of("players", true),
        BooleanSetting.of("invisible", true),
        BooleanSetting.of("naked", true),
        BooleanSetting.of("teammates", false),
        BooleanSetting.of("animals", false),
        BooleanSetting.of("mobs", false)
    );
    private final ModeSetting sortMode = new ModeSetting("aimassist.sortMode", new String[]{"adaptive", "distance", "health", "aim"}).setValue("aim");
    private final NumberSetting adaptiveStrength = new NumberSetting("aimassist.adaptiveStrength", 4.0F, 1.0F, 10.0F, 0.5F)
        .setVisibleWhen(() -> this.mode.is("adaptive"));
    private final NumberSetting range = new NumberSetting("aimassist.range", 4.0F, 2.0F, 6.0F, 0.1F);
    private final NumberSetting fov = new NumberSetting("aimassist.fov", 90.0F, 10.0F, 180.0F, 1.0F);
    private final RangeSetting speed = new RangeSetting("aimassist.speed", 0.8F, 1.2F, 0.1F, 3.0F, 0.05F);
    private final CurveSetting curveYaw = new CurveSetting("aimassist.curveYaw", CurvePreset.MAGNET.createCurve());
    private final CurveSetting curvePitch = new CurveSetting("aimassist.curvePitch", CurvePreset.MAGNET.createCurve())
        .setVisibleWhen(() -> this.pitchAssist.getValue());
    private final GroupSetting arc = new GroupSetting("aimassist.arc", true);
    private final CurveSetting arcCurve = new CurveSetting(this.arc, "aimassist.arcCurve", CurvePreset.BELL.createCurve());
    private final NumberSetting arcHeight = new NumberSetting(this.arc, "aimassist.arcHeight", 1.5F, 0.0F, 4.0F, 0.1F);
    private final NumberSetting arcLength = new NumberSetting(this.arc, "aimassist.arcLength", 40.0F, 10.0F, 120.0F, 1.0F);
    private final BooleanSetting throughWalls = new BooleanSetting("aimassist.throughWalls", true);
    private final BooleanSetting pitchAssist = new BooleanSetting("aimassist.pitchAssist", true);
    private final Random random = new Random();
    private final StopWatch frameTimer = new StopWatch();
    private LivingEntity target;
    private LivingEntity lastTarget;
    private float arcProgress;
    private float arcDirection = 1.0F;
    private float lastYaw;
    private float lastPitch;
    private float arcSign;
    private float lastArcOffset;

    public static AimAssist getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(AimAssist.class);
    }

    @Override
    public void onEnable() {
        this.target = null;
        this.lastTarget = null;
        this.lastYaw = 0.0F;
        this.lastPitch = 0.0F;
        super.onEnable();
    }

    @EventHandler
    public void onHudPostRender(HudPostRenderEvent hudPostRenderEvent) {
        this.updateTarget();
    }

    @EventHandler
    public void onRender3D(Render3DEvent render3DEvent) {
        LivingEntity livingEntity = this.target;
        if (mc.player != null && mc.world != null && livingEntity != null && livingEntity.isAlive()) {
            float f = Math.min((float)this.frameTimer.getElapsed() / 1000.0F, 0.1F);
            this.frameTimer.reset();
            float f2 = mc.player.getYaw();
            float f3 = mc.player.getPitch();
            Vec3d vec3d = mc.player.getEyePos();
            Vec3d vec3d2 = RaycastUtil.getDirection(f2, f3);
            float f4 = this.range.getValue();
            Box box = livingEntity.getBoundingBox();
            if (box.raycast(vec3d, vec3d.add(vec3d2.multiply(f4))).isPresent()) {
                this.resetArc();
                this.lastYaw = f2;
                this.lastPitch = f3;
            } else {
                Vec3d vec3d3 = vec3d.add(vec3d2.multiply(Math.max(mc.player.distanceTo(livingEntity), 1.0F)));
                Vec3d vec3d4 = CombatUtil.clampToBox(vec3d3, box);
                Vec3d vec3d5 = vec3d4.subtract(vec3d);
                float f5 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec3d5.z, vec3d5.x)) - 90.0);
                float f6 = (float)(-Math.toDegrees(Math.atan2(vec3d5.y, Math.sqrt(vec3d5.x * vec3d5.x + vec3d5.z * vec3d5.z))));
                float f7 = MathHelper.wrapDegrees(f5 - f2);
                float f8 = f6 - f3;
                float f9 = MathHelper.wrapDegrees(f2 - this.lastYaw);
                float f10 = f3 - this.lastPitch;
                float f11 = Math.max(this.fov.getValue(), 1.0F);
                if (Math.hypot(f7, f8) > f11) {
                    this.resetArc();
                    this.lastYaw = f2;
                    this.lastPitch = f3;
                } else {
                    float f12 = this.randomSpeed();
                    float f13 = f * 60.0F;
                    float f14 = clamp01(this.curveYaw.getValue().evaluate(clamp01(Math.abs(f7) / f11)) * f12 * f13);
                    if (this.mode.is("adaptive")) {
                        float f15 = Math.abs(f9);
                        if (Math.signum(f9) != Math.signum(f7)) {
                            f14 = 0.0F;
                        } else {
                            f14 *= Math.min(f15 / this.adaptiveStrength.getValue(), 1.0F);
                        }
                    }

                    float f16 = f7 * f14;
                    float f17 = 0.0F;
                    if (this.pitchAssist.getValue()) {
                        float f18 = clamp01(this.curvePitch.getValue().evaluate(clamp01(Math.abs(f8) / f11)) * f12 * f13);
                        if (this.mode.is("adaptive")) {
                            float f19 = Math.abs(f10);
                            if (Math.signum(f10) != Math.signum(f8)) {
                                f18 = 0.0F;
                            } else {
                                f18 *= Math.min(f19 / this.adaptiveStrength.getValue(), 1.0F);
                            }
                        }

                        f17 = f8 * f18;
                    }

                    if (this.arc.isEnabled()) {
                        float f20 = Math.signum(f16);
                        if (f20 != 0.0F && f20 != this.arcSign) {
                            this.arcProgress = 0.0F;
                            this.lastArcOffset = 0.0F;
                            this.arcDirection = this.random.nextBoolean() ? 1.0F : -1.0F;
                            this.arcSign = f20;
                        }

                        this.arcProgress = this.arcProgress + Math.abs(f16);
                        float f21 = this.arcProgress / this.arcLength.getValue() % 1.0F;
                        float f22 = this.arcHeight.getValue() * this.arcDirection * clamp01(this.arcCurve.getValue().evaluate(f21));
                        f17 += f22 - this.lastArcOffset;
                        this.lastArcOffset = f22;
                    }

                    float f23 = SensitivityUtil.snapDelta(f2 + f16 + CombatUtil.randomFloat(-0.02F, 0.02F));
                    float f24 = SensitivityUtil.snapDelta(f3 + f17 + CombatUtil.randomFloat(-0.02F, 0.02F));
                    mc.player.setYaw(f23);
                    mc.player.setPitch(MathHelper.clamp(f24, -89.0F, 89.0F));
                    this.lastYaw = mc.player.getYaw();
                    this.lastPitch = mc.player.getPitch();
                }
            }
        } else {
            this.frameTimer.reset();
            if (mc.player != null) {
                this.lastYaw = mc.player.getYaw();
                this.lastPitch = mc.player.getPitch();
            }
        }
    }

    private void resetArc() {
        this.arcProgress = 0.0F;
        this.lastArcOffset = 0.0F;
        this.arcSign = 0.0F;
    }

    private float randomSpeed() {
        float[] floats = this.speed.getValue();
        return floats[0] + this.random.nextFloat() * (floats[1] - floats[0]);
    }

    private void updateTarget() {
        if (mc.player != null && mc.world != null) {
            double d = this.range.getValue().floatValue();
            boolean flag = this.throughWalls.getValue();
            if (this.isTargetInCrosshair(d, flag)) {
                this.lastTarget = this.target;
            } else {
                if (this.isValidTarget(this.lastTarget, d, flag)) {
                    this.target = this.lastTarget;
                } else {
                    this.target = this.findTarget(d, flag);
                }

                if (this.target != null) {
                    this.lastTarget = this.target;
                }
            }
        } else {
            this.target = null;
        }
    }

    private LivingEntity findTarget(double d, boolean flag) {
        ArrayList arrayList = new ArrayList();

        for (LivingEntity livingEntity : TargetComponent.LIVING_ENTITIES) {
            if (TargetComponent.isValidTarget(livingEntity, this.targets, flag) && CombatUtil.getDistance(livingEntity) <= d) {
                arrayList.add(livingEntity);
            }
        }

        if (arrayList.isEmpty()) {
            return null;
        }

        arrayList.sort(this.getComparator());
        return (LivingEntity)arrayList.get(0);
    }

    private Comparator<LivingEntity> getComparator() {
        if (this.sortMode.is("distance")) {
            return Comparator.<LivingEntity>comparingDouble(livingEntity -> CombatUtil.getDistance(livingEntity)).thenComparingDouble(this::effectiveHealth);
        } else if (this.sortMode.is("health")) {
            return Comparator.<LivingEntity>comparingDouble(livingEntity2 -> this.effectiveHealth(livingEntity2))
                .thenComparingDouble(livingEntity3 -> CombatUtil.getDistance(livingEntity3));
        } else {
            return this.sortMode.is("adaptive")
                ? Comparator.<LivingEntity>comparingDouble(livingEntity4 -> -livingEntity4.getArmor())
                    .thenComparingDouble(livingEntity5 -> CombatUtil.getCenterRotationDistance(livingEntity5))
                    .thenComparingDouble(livingEntity6 -> CombatUtil.getDistance(livingEntity6))
                    .thenComparingDouble(this::effectiveHealth)
                : Comparator.<LivingEntity>comparingDouble(livingEntity7 -> CombatUtil.getCenterRotationDistance(livingEntity7))
                    .thenComparingDouble(livingEntity8 -> CombatUtil.getDistance(livingEntity8));
        }
    }

    private double effectiveHealth(LivingEntity livingEntity) {
        return livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
    }

    private boolean isValidTarget(LivingEntity livingEntity, double d, boolean flag) {
        return livingEntity != null && livingEntity.isAlive() && CombatUtil.getDistance(livingEntity) <= d && TargetComponent.isValidTarget(livingEntity, this.targets, flag);
    }

    private boolean isTargetInCrosshair(double d, boolean flag) {
        return this.target == null
            ? false
            : this.target.isAlive() && CombatUtil.getRayDistance(this.target) <= d && TargetComponent.isValidTarget(this.target, this.targets, flag);
    }

    private static float clamp01(float f) {
        if (f < 0.0F) {
            return 0.0F;
        } else {
            return f > 1.0F ? 1.0F : f;
        }
    }

    public ModeSetting getMode() {
        return this.mode;
    }

    public MultiSetting getTargets() {
        return this.targets;
    }

    public ModeSetting getSortMode() {
        return this.sortMode;
    }

    public NumberSetting getAdaptiveStrength() {
        return this.adaptiveStrength;
    }

    public NumberSetting getRange() {
        return this.range;
    }

    public NumberSetting getFov() {
        return this.fov;
    }

    public RangeSetting getSpeed() {
        return this.speed;
    }

    public CurveSetting getCurveYaw() {
        return this.curveYaw;
    }

    public CurveSetting getCurvePitch() {
        return this.curvePitch;
    }

    public GroupSetting getArc() {
        return this.arc;
    }

    public CurveSetting getArcCurve() {
        return this.arcCurve;
    }

    public NumberSetting getArcHeight() {
        return this.arcHeight;
    }

    public NumberSetting getArcLength() {
        return this.arcLength;
    }

    public BooleanSetting getThroughWalls() {
        return this.throughWalls;
    }

    public BooleanSetting getPitchAssist() {
        return this.pitchAssist;
    }

    public Random getRandom() {
        return this.random;
    }

    public StopWatch getFrameTimer() {
        return this.frameTimer;
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public LivingEntity getLastTarget() {
        return this.lastTarget;
    }

    public float getArcProgress() {
        return this.arcProgress;
    }

    public float getArcDirection() {
        return this.arcDirection;
    }

    public float getLastYaw() {
        return this.lastYaw;
    }

    public float getLastPitch() {
        return this.lastPitch;
    }

    public float getArcSign() {
        return this.arcSign;
    }

    public float getLastArcOffset() {
        return this.lastArcOffset;
    }
}
