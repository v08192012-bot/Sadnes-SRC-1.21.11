package tech.sadnes.client.feature;

import java.lang.reflect.Array;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.player.CombatUtil;

public abstract class TaskHandler implements IMinecraft {
    protected final AttackAura feature;

    public abstract void update(LivingEntity livingEntity);

    public TaskHandler.Phase getPhase() {
        return TaskHandler.Phase.FRAME;
    }

    public void onStart() {
    }

    public void onStop() {
    }

    public boolean isActive() {
        return false;
    }

    public void restart() {
        this.onStart();
    }

    public void reset() {
    }

    protected void applyRotation(float[] floats) {
        if (floats != null) {
            float f = mc.player.getYaw();
            float f2 = Array.getFloat(floats, 0);
            float f3 = mc.player.getPitch();
            float f4 = Array.getFloat(floats, 1);
            float f5 = MathHelper.clamp(ExactMath.add(f3, f4), -89.0F, 89.0F);
            float f6 = ExactMath.add(f, f2);
            Rotation rotation = new Rotation(f6, f5);
            float f7 = Math.abs(Array.getFloat(floats, 0));
            float f8 = Math.abs(Array.getFloat(floats, 1));
            float f9 = CombatUtil.randomLerp(10.0F, 15.0F);
            float f10 = CombatUtil.randomLerp(6.0F, 10.0F);
            float f11 = ExactMath.add(f7, 0.1F);
            float f12 = ExactMath.add(f8, 0.1F);
            RotationComponent.rotate(rotation, f11, f12, f9, f10, 2, 5, false);
        }
    }

    protected static void cancelRotation() {
        RotationComponent rotationComponent = RotationComponent.getInstance();
        if (rotationComponent != null && rotationComponent.isActive()) {
            rotationComponent.stop();
        }
    }

    public TaskHandler(AttackAura attackAura) {
        this.feature = attackAura;
    }

    public enum Phase {
        RENDER,
        FRAME,
        TICK;
    }
}
