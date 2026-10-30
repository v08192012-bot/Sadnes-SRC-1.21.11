package tech.sadnes.client.feature.impl.combat.aura.rotation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.feature.TaskHandler;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.player.CombatUtil;

public final class SnapsRotation extends TaskHandler {
    public SnapsRotation(AttackAura attackAura) {
        super(attackAura);
    }

    @Override
    public void update(LivingEntity livingEntity) {
        double d = CombatUtil.getDistance(livingEntity);
        if (this.feature.getRange().getValue().floatValue() >= d && this.feature.canAttack()) {
            float f = (float)this.feature.getAttackRange();
            Vector4f vector4f = CombatUtil.getRotationsTo(livingEntity, f, false);
            float f2 = vector4f.x;
            float f3 = CombatUtil.randomLerp(-2.0F, 2.0F);
            float f4 = vector4f.y;
            float f5 = CombatUtil.randomLerp(-2.0F, 2.0F);
            float f6 = MathHelper.clamp(ExactMath.add(f4, f5), -89.0F, 89.0F);
            String string = "holy";
            boolean flag = ServerManager.isServer(string);
            boolean flag2 = ServerManager.isServer(string);
            float f7 = ExactMath.add(f2, f3);
            Rotation rotation = new Rotation(f7, f6);
            float f8 = Float.intBitsToFloat((int)(flag ? 1123024896L : 1135869952L));
            float f9 = Float.intBitsToFloat((int)(flag2 ? 1123024896L : 1135869952L));
            RotationComponent.rotate(rotation, f8, f8, f9, f9, 1, 5, false);
        }
    }
}
