package tech.sadnes.client.feature.impl.combat.aura.rotation;

import net.minecraft.entity.LivingEntity;
import tech.sadnes.client.feature.TaskHandler;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;

public final class TestRotation extends TaskHandler {
    public TestRotation(AttackAura attackAura) {
        super(attackAura);
    }

    @Override
    public void update(LivingEntity livingEntity) {
        RotationComponent.rotateHuman(livingEntity, 2, 5);
    }
}
