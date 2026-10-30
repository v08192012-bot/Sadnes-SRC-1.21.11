package tech.sadnes.client.feature.impl.combat.aura.rotation;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult.Type;
import tech.sadnes.api.mixin.client.MinecraftClientAccessor;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.RaycastUtil;

public final class SnapAttackHelper implements IMinecraft {
    private final AttackAura aura;
    private int activeTicks;
    private int idleTicks;
    private boolean checkLookToggle;
    private int activeBudget = -1;
    private int usedTicks;

    public SnapAttackHelper(AttackAura attackAura) {
        this.aura = attackAura;
    }

    public void tryAttack(LivingEntity livingEntity) {
        this.activeTicks++;
        if (livingEntity != null && this.aura.getHitRandom().getValue()) {
            boolean flag;
            label63: {
                boolean flag2;
                if (this.activeTicks >= 2) {
                    if (this.activeTicks != 4) {
                        flag = CombatUtil.randomBoolean();
                        break label63;
                    }

                    flag2 = true;
                } else {
                    flag2 = false;
                }

                flag = flag2;
            }

            boolean flag3 = this.idleTicks >= 33 && (getEffectiveHealth(livingEntity) > 6.0F || this.idleTicks > 43);
            if ((!this.checkLookToggle || !RaycastUtil.isLookingAtSafe(livingEntity, mc.player.getYaw(), mc.player.getPitch(), 3.0, false)) && flag && flag3) {
                boolean flag4 = mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.ENTITY;
                ((MinecraftClientAccessor)mc).sadnes$invokeDoAttack();
                if (flag4) {
                    AttackTimer.onAttack();
                }

                if (CombatUtil.randomBoolean()) {
                    if (!this.checkLookToggle) {
                        this.checkLookToggle = true;
                        this.idleTicks = (int)CombatUtil.randomLerp(-10.0F, 10.0F);
                        return;
                    }

                    this.checkLookToggle = false;
                }

                this.idleTicks = (int)CombatUtil.randomLerp(-10.0F, 10.0F);
                return;
            }
        }
    }

    public void onStop() {
        this.activeTicks = 0;
        this.idleTicks++;
        this.activeBudget = -1;
    }

    public boolean isActive() {
        if (this.aura.getHitRandom().getValue() && mc.player.isOnGround() && this.aura.isTargetReachable()) {
            if (this.activeBudget < 0) {
                this.activeBudget = CombatUtil.randomFloat() < 0.2F ? 1 : 0;
                this.usedTicks = 0;
            }

            if (this.usedTicks >= this.activeBudget) {
                return false;
            }

            this.usedTicks++;
            return true;
        } else {
            return false;
        }
    }

    public void reset() {
        this.activeBudget = -1;
    }

    private static float getEffectiveHealth(LivingEntity livingEntity) {
        float f = livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
        return livingEntity instanceof PlayerEntity ? ServerManager.getScoreboardHealth((PlayerEntity)livingEntity, f) : f;
    }
}
