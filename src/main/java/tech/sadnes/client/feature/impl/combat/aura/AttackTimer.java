package tech.sadnes.client.feature.impl.combat.aura;

import net.minecraft.entity.attribute.EntityAttributes;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.CombatUtil;

public final class AttackTimer implements IMinecraft {
    private static final StopWatch timer = new StopWatch();
    private static long attackOffset = -150L;
    private static int attackCount;
    private static int attackThreshold = 3;

    private AttackTimer() {
    }

    private static void incrementAttackCount() {
        attackCount++;
    }

    public static boolean isThresholdReached() {
        return attackCount >= attackThreshold;
    }

    private static void resetAttackCount() {
        if (attackCount > 0) {
            attackCount = 0;
            if (ServerManager.isFunTime()) {
                attackThreshold = CombatUtil.randomInt(9, 15);
                return;
            }

            attackThreshold = CombatUtil.randomInt(2, 5);
        }
    }

    public static boolean isFastMode() {
        return false;
    }

    public static long getCooldownMs() {
        if (mc.player == null) {
            return 450L;
        } else {
            AttackAura attackAura = AttackAura.getInstance();
            if (attackAura != null && !attackAura.getAttackModeSetting().is("1.9+") && attackAura.isEnabled()) {
                return 45L;
            } else if (ServerManager.isSpookyTime()) {
                return computeSpookyTimeCooldown();
            } else {
                return isFastMode() ? AttackDelayPattern.getCurrentDelay() / 4L : AttackDelayPattern.getCurrentDelay();
            }
        }
    }

    private static long computeSpookyTimeCooldown() {
        double d = mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED);
        long l = (long)(1.0 / d * 1000.0 * (1.0F - Math.min(0.2F, 1.0F)));
        if (d == 4.0 || d == 4.4000000059604645 || d == 4.800000011920929) {
            l = 450L;
        }

        long l2 = Math.max(l, 512L);
        if (isThresholdReached()) {
            l2 += 100L;
        }

        return l2;
    }

    public static boolean hasCooldownPassed(long l) {
        return mc.player == null ? false : timer.hasPassed((double)(getCooldownMs() + l));
    }

    public static boolean isReady() {
        return hasCooldownPassed(0L);
    }

    public static boolean hasElapsed(long l) {
        return timer.hasPassed((double)l);
    }

    public static float getCooldownProgress() {
        return mc.player == null ? 0.0F : Math.min((float)timer.getElapsed() / (float)getCooldownMs(), 1.0F);
    }

    public static float getElapsedMs() {
        return (float)timer.getElapsed();
    }

    public static void onAttack() {
        timer.reset();
        AttackDelayPattern.advance();
        attackOffset = ServerManager.isAresMine() ? CombatUtil.randomInt(2, 6) * -50L : -150L;
        if (isThresholdReached()) {
            resetAttackCount();
        } else {
            incrementAttackCount();
        }
    }

    public static StopWatch getTimer() {
        return timer;
    }

    public static long getAttackOffset() {
        return attackOffset;
    }

    public static int getAttackCount() {
        return attackCount;
    }

    public static int getAttackThreshold() {
        return attackThreshold;
    }
}
