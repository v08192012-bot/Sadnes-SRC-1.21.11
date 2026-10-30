package tech.sadnes.client.feature.impl.combat.aura.rotation;

import java.lang.reflect.Array;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.animation.Easing;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.feature.TaskHandler;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.RaycastUtil;
import tech.sadnes.client.util.rotation.RotationUtil;
import tech.sadnes.client.util.rotation.SensitivityUtil;

public final class LegitRotation extends TaskHandler {
    private final StopWatch frameTimer = new StopWatch();
    private float yawVelocity;
    private float pitchVelocity;

    public LegitRotation(AttackAura attackAura) {
        super(attackAura);
    }

    @Override
    public TaskHandler.Phase getPhase() {
        return TaskHandler.Phase.RENDER;
    }

    @Override
    public void update(LivingEntity livingEntity) {
        RotationComponent rotationComponent = RotationComponent.getInstance();
        StopWatch stopWatch = this.frameTimer;
        if (rotationComponent != null && rotationComponent.isActive()) {
            this.reset();
        } else {
            float f = Math.min(ExactMath.divide((float)stopWatch.getElapsed(), 1000.0F), 0.1F);
            stopWatch.reset();
            float f2 = mc.player.getYaw();
            float f3 = mc.player.getPitch();
            Vec3d vec3d = mc.player.getEyePos();
            Vec3d vec3d2 = RaycastUtil.getDirection(f2, f3);
            Box box = livingEntity.getBoundingBox();
            double d = Math.max(mc.player.distanceTo(livingEntity), 0.1F);
            Vec3d vec3d3 = vec3d2.multiply(d);
            Vec3d vec3d4 = vec3d.add(vec3d3);
            Box box2 = box.expand(-0.1);
            Vec3d vec3d5 = CombatUtil.clampToBox(vec3d4, box2);
            float[] floats = RotationUtil.getRotationArray(vec3d, vec3d5);
            float f4 = MathHelper.wrapDegrees(ExactMath.subtract(Array.getFloat(floats, 0), f2));
            float f5 = Array.getFloat(floats, 1);
            float f6 = CombatUtil.randomLerp(0.55F, 0.6F);
            float f7 = CombatUtil.randomLerp(0.15F, 0.26F);
            Easing easing = Easings.CIRC_OUT;
            double d2 = ExactMath.divide(Math.abs(f4), 180.0F);
            double d3 = easing.ease(d2);
            double d4 = MathHelper.clamp(ExactMath.multiply(ExactMath.multiply(ExactMath.multiply(f6, f), 60.0F), d3), 0.0, 1.0);
            Easing easing2 = Easings.QUART_IN_OUT;
            float f8 = ExactMath.subtract(f5, f3);
            double d5 = ExactMath.divide(Math.abs(f8), 90.0F);
            double d6 = easing2.ease(d5);
            double d7 = MathHelper.clamp(ExactMath.multiply(ExactMath.multiply(ExactMath.multiply(f7, f), 60.0F), d6), 0.0, 1.0);
            float f9 = this.feature.getLegitSpeed().getValue();
            float f10 = (float)ExactMath.multiply(f4, d4);
            double d8 = f10;
            float f11 = (float)ExactMath.multiply(f8, d7);
            double d9 = f11;
            double d10 = Math.hypot(d8, d9);
            float f12 = CombatUtil.randomLerp(0.1F, 0.15F);
            float f13 = CombatUtil.randomLerp(0.1F, 0.15F);
            float f14 = CombatUtil.randomLerp(-0.02F, 0.02F);
            float f15 = this.yawVelocity;
            float f16 = CombatUtil.randomLerp(-0.02F, 0.02F);
            float f17 = this.pitchVelocity;
            float f18 = SensitivityUtil.getSensitivityFactor();
            float f19 = this.yawVelocity;
            float f20 = this.pitchVelocity;
            float f21 = ExactMath.multiply(f, f9);
            float f22 = (float)d10;
            boolean flag = !(f22 > f21);
            float f23 = ExactMath.divide(f21, f22);
            float f24 = ExactMath.multiply(f23, f10);
            float f25 = ExactMath.multiply(f23, f11);
            boolean flag2;
            int i;
            if (flag) {
                flag2 = true;
                i = Float.floatToRawIntBits(f10);
            } else {
                flag2 = false;
                i = Float.floatToRawIntBits(f24);
            }

            int i2 = i;
            float f26 = Float.intBitsToFloat(flag2 ? Float.floatToRawIntBits(f11) : Float.floatToRawIntBits(f25));
            float f27 = ExactMath.multiply(f26, f12);
            float f28 = Float.intBitsToFloat(i2);
            float f29 = ExactMath.multiply(f28, f13);
            float f30 = ExactMath.add(f27, f28);
            float f31 = ExactMath.add(f29, f26);
            float f32 = ExactMath.add(f14, f30);
            float f33 = ExactMath.add(f16, f31);
            float f34 = ExactMath.add(f15, f32);
            float f35 = ExactMath.add(f17, f33);
            float f36 = (float)ExactMath.multiply(f18, 0.15);
            int i3 = !Float.isNaN(f36) ? (f36 == 0.0F ? 0 : 1) : 0;
            float f37;
            float f38;
            if (!Float.isNaN(f36) && i3 == 0) {
                f37 = f20;
                f38 = f34;
            } else {
                this.yawVelocity = f19;
                this.pitchVelocity = f20;
                int i4 = Math.round(ExactMath.divide(f34, f36));
                f19 = this.yawVelocity;
                f37 = this.pitchVelocity;
                f38 = i4;
            }

            this.yawVelocity = f19;
            this.pitchVelocity = f37;
            float f39 = SensitivityUtil.getSensitivityFactor();
            float f40 = SensitivityUtil.getSensitivityFactor();
            float f41 = this.yawVelocity;
            float f42 = this.pitchVelocity;
            float f43 = ExactMath.multiply((float)ExactMath.multiply(f39, 0.15), f38);
            float f44 = (float)ExactMath.multiply(f40, 0.15);
            int i5 = !Float.isNaN(f44) ? (f44 == 0.0F ? 0 : 1) : 0;
            float f45;
            float f46;
            float f47;
            if (!Float.isNaN(f44) && i5 == 0) {
                f45 = f35;
                f46 = f41;
                f47 = f42;
            } else {
                this.yawVelocity = f41;
                this.pitchVelocity = f42;
                int i6 = Math.round(ExactMath.divide(f35, f44));
                float f48 = this.yawVelocity;
                f47 = this.pitchVelocity;
                float f49 = i6;
                f46 = f48;
                f45 = f49;
            }

            this.yawVelocity = f46;
            this.pitchVelocity = f47;
            float f50 = SensitivityUtil.getSensitivityFactor();
            float f51 = ExactMath.subtract(f34, f43);
            this.yawVelocity = f51;
            float f52 = ExactMath.multiply((float)ExactMath.multiply(f50, 0.15), f45);
            float f53 = ExactMath.subtract(f35, f52);
            this.pitchVelocity = f53;
            ClientPlayerEntity clientPlayerEntity = mc.player;
            float f54 = ExactMath.add(f2, f43);
            clientPlayerEntity.setYaw(f54);
            ClientPlayerEntity clientPlayerEntity2 = mc.player;
            float f55 = MathHelper.clamp(ExactMath.add(f52, f3), -89.0F, 89.0F);
            clientPlayerEntity2.setPitch(f55);
        }
    }

    @Override
    public void reset() {
        this.frameTimer.reset();
        this.yawVelocity = 0.0F;
        this.pitchVelocity = 0.0F;
    }
}
