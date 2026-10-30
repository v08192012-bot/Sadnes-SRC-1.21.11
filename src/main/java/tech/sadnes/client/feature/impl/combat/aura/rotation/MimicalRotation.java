package tech.sadnes.client.feature.impl.combat.aura.rotation;

import net.minecraft.entity.LivingEntity;
import tech.sadnes.client.feature.TaskHandler;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.util.player.LegitAimController;

public final class MimicalRotation extends TaskHandler {
    private final LegitAimController mimicModel = new LegitAimController();
    private int minTicks;
    private int maxTicks;

    public MimicalRotation(AttackAura attackAura) {
        super(attackAura);
    }

    @Override
    public void update(LivingEntity livingEntity) {
        this.minTicks = 8;
        this.maxTicks = 14;
        this.applyMimic(livingEntity);
    }

    @Override
    public void onStart() {
        int i;
        label29: {
            LivingEntity livingEntity = this.feature.getLastTarget();
            int i2 = this.minTicks;
            int i3 = this.maxTicks;
            if (livingEntity != null) {
                if (i2 > 0) {
                    boolean flag = livingEntity.isAlive();
                    int i4 = this.minTicks;
                    i = this.maxTicks;
                    if (flag) {
                        int i5 = i4 + -1;
                        int i6 = 1 * i5;
                        this.minTicks = i6;
                        this.applyMimic(livingEntity);
                        return;
                    }

                    if (i > 0) {
                        break label29;
                    }
                } else {
                    i = i3;
                    if (i3 > 0) {
                        break label29;
                    }
                }
            } else if (i2 > 0) {
                i = i3;
                if (i3 > 0) {
                    break label29;
                }
            } else {
                i = i3;
                if (i3 > 0) {
                    break label29;
                }
            }

            return;
        }

        int i7 = i & -2 ^ -577298081;
        int i8 = i7 * i7;
        int i9 = (2 - i8) * i8;
        int i10 = (2 - i9) * i9;
        int i11 = (2 - i10) * i10;
        int i12 = (2 - i11) * i11;
        int i13 = i + -1;
        int i14 = i12 * i13;
        int i15 = 2 - i12;
        int i16 = i14 * i15;
        this.maxTicks = i16;
        this.applyMimic(null);
    }

    @Override
    public void restart() {
        if (mc.player != null) {
            this.applyMimic(null);
        }
    }

    @Override
    public void reset() {
        this.minTicks = 0;
        this.maxTicks = 0;
        this.mimicModel.reset();
    }

    private void applyMimic(LivingEntity livingEntity) {
        LegitAimController legitAimController = this.mimicModel;
        float f = (float)this.feature.getAttackRange();
        float[] floats = legitAimController.computeRotationDelta(livingEntity, f);
        this.applyRotation(floats);
    }
}
