package tech.sadnes.client.feature.impl.combat.aura.rotation;

import net.minecraft.entity.LivingEntity;
import tech.sadnes.client.feature.TaskHandler;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.neuro.NeuroAim;

public final class TrainedRotation extends TaskHandler {
    public TrainedRotation(AttackAura attackAura) {
        super(attackAura);
    }

    @Override
    public void update(LivingEntity livingEntity) {
        NeuroAim neuroAim = NeuroAim.INSTANCE;
        float f = (float)this.feature.getAttackRange();
        float[] floats = neuroAim.computeRotation(livingEntity, f);
        this.applyRotation(floats);
    }

    @Override
    public void onStart() {
        cancelRotation();
    }
}
