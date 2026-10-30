package tech.sadnes.client.feature.impl.combat.aura.rotation;

import java.lang.reflect.Array;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Easing;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.feature.TaskHandler;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.AutoMace;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.player.AimPointUtil;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.RaycastUtil;
import tech.sadnes.client.util.rotation.RotationUtil;

public final class SpookyRotation extends TaskHandler {
    private Vector2f rotation = new Vector2f();

    public SpookyRotation(AttackAura attackAura) {
        super(attackAura);
    }

    @Override
    public void update(LivingEntity livingEntity) {
        AutoMace autoMace = AutoMace.getInstance();
        int i = this.feature.getAttackCount();
        AttackAura attackAura = this.feature;
        boolean flag;
        if (AttackTimer.hasCooldownPassed(-110L)) {
            flag = false;
        } else if (!autoMace.isActive()) {
            int i2 = (int)(i * -1840700269L >>> 32) + i;
            int i3 = i2 >>> 31;
            int i4 = (i2 >> 2) + i3;
            int i5 = i4 * 8;
            if (i4 - i5 + i == 3) {
                flag = false;
            } else {
                flag = true;
            }
        } else {
            flag = false;
        }

        attackAura.setBoostedJitter(flag);
        double d = CombatUtil.getRayDistance(livingEntity);
        Vec3d vec3d = mc.player.getEyePos();
        Vec3d vec3d2 = CombatUtil.getLookVector().multiply(0.1);
        Vec3d vec3d3 = vec3d.add(vec3d2);
        Box box = livingEntity.getBoundingBox().expand(-0.1F);
        Vec3d vec3d4 = CombatUtil.clampToBox(vec3d3, box);
        float[] floats = RotationUtil.getRotationArrayTo(vec3d4);
        float[] floats2;
        if (!(d > 3.1F)) {
            floats2 = floats;
        } else {
            Box box2 = livingEntity.getBoundingBox();
            floats2 = RotationUtil.getRotationArrayTo(CombatUtil.clampToBox(vec3d, box2));
        }

        float[] floats3;
        if (!AimPointUtil.canSee(mc.player, vec3d4)) {
            float f = (float)this.feature.getAttackRange();
            floats3 = RotationUtil.getRotationArrayTo(AimPointUtil.getAimPoint(livingEntity, f, true));
        } else {
            floats3 = floats2;
        }

        float f2;
        boolean flag2;
        Vector2f vector2f;
        float f3;
        float f4;
        label50: {
            if (CombatUtil.randomBoolean()) {
                float f5 = CombatUtil.randomLerp(0.3F, 0.4F);
                float f6 = CombatUtil.randomFloat(0.0F, 1.0F);
                float f7 = CombatUtil.randomLerp(50.0F, 55.0F);
                f2 = MathHelper.lerp(f6, f7, 0.1F);
                float f8 = CombatUtil.randomLerp(0.05F, 0.5F);
                float f9 = MathHelper.wrapDegrees(mc.player.getYaw());
                flag2 = RaycastUtil.isPitchFacing(livingEntity, f9);
                float f10 = this.rotation.y;
                boolean flag3 = RaycastUtil.isYawFacing(livingEntity, f10);
                vector2f = this.rotation;
                if (!flag3) {
                    f3 = f8;
                    f4 = f5;
                    break label50;
                }

                f3 = f8;
                f4 = f5;
            } else {
                float f11 = CombatUtil.randomLerp(0.6F, 0.8F);
                float f12 = CombatUtil.randomFloat(0.0F, 1.0F);
                float f13 = CombatUtil.randomLerp(50.0F, 55.0F);
                f2 = MathHelper.lerp(f12, f13, 0.1F);
                float f14 = CombatUtil.randomLerp(0.05F, 0.5F);
                float f15 = MathHelper.wrapDegrees(mc.player.getYaw());
                flag2 = RaycastUtil.isPitchFacing(livingEntity, f15);
                float f16 = this.rotation.y;
                boolean flag4 = RaycastUtil.isYawFacing(livingEntity, f16);
                vector2f = this.rotation;
                if (!flag4) {
                    f3 = f14;
                    f4 = f11;
                    break label50;
                }

                f3 = f14;
                f4 = f11;
            }

            if (3.1 > d) {
                float f17 = vector2f.y;
                Array.setFloat(floats3, 1, f17);
                vector2f = this.rotation;
            }
        }

        float f18 = ExactMath.multiply(f4, f2);
        if (flag2) {
            f18 = ExactMath.multiply(f18, 0.5F);
        }

        this.rotation = vector2f;
        float f19 = AttackTimer.getCooldownProgress();
        float f20 = Array.getFloat(floats3, 0);
        float f21 = CombatUtil.randomLerp(-0.14F, 0.14F);
        float f22 = ExactMath.add(f20, f21);
        Array.setFloat(floats3, 0, f22);
        float f23 = Array.getFloat(floats3, 1);
        float f24 = CombatUtil.randomLerp(-0.14F, 0.14F);
        float f25 = ExactMath.add(f23, f24);
        Array.setFloat(floats3, 1, f25);
        float f26 = mc.player.getYaw();
        float f27 = MathHelper.wrapDegrees(ExactMath.subtract(Array.getFloat(floats3, 0), f26));
        Easing easing = Easings.BOUNCE_IN_OUT;
        double d2 = ExactMath.add(ExactMath.add(f19, -1.0F), 1.0F);
        float f28 = ExactMath.add(ExactMath.multiply((float)easing.ease(d2), f27), f26);
        float f29 = MathHelper.wrapDegrees(ExactMath.subtract(f28, f26));
        float f30 = CombatUtil.randomLerp(0.005F, 0.04F);
        float f31 = Array.getFloat(floats3, 1);
        float f32 = MathHelper.clamp(ExactMath.add(ExactMath.multiply(f29, f30), f31), -89.0F, 89.0F);
        Vector2f vector2f2 = new Vector2f(f28, f32);
        this.rotation = vector2f2;
        Rotation rotation2 = new Rotation(vector2f2);
        float f33 = CombatUtil.randomLerp(10.0F, 15.0F);
        float f34 = CombatUtil.randomLerp(5.0F, 6.0F);
        RotationComponent.rotate(rotation2, f18, f3, f33, f34, 1, 4, false);
    }

    @Override
    public void reset() {
        if (mc.player != null) {
            this.rotation = new Vector2f(mc.player.getYaw(), mc.player.getPitch());
        }
    }
}
