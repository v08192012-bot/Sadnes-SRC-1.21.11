package tech.sadnes.client.feature.impl.combat.aura.rotation;

import net.minecraft.entity.LivingEntity;
import org.joml.Vector4f;
import tech.sadnes.client.feature.TaskHandler;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.player.CombatUtil;

public final class NormalRotation extends TaskHandler {
    public NormalRotation(AttackAura attackAura) {
        super(attackAura);
    }

    @Override
    public void update(LivingEntity livingEntity) {
        Vector4f vector4f = CombatUtil.getRotationsTo(livingEntity, (float)this.feature.getAttackRange(), false);
        RotationComponent.rotateTo(new Rotation(vector4f.x, vector4f.y), 360.0F, 10.0F, 1, 5);
    }
}
