package tech.sadnes.client.util.player;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.FluidTags;
import tech.sadnes.api.duck.TicksOnGroundAccessor;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.impl.movement.NoWeb;
import tech.sadnes.client.script.ScriptAuraHooks;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.math.StopWatch;

public final class CriticalUtil implements IMinecraft {
    private static final StopWatch airTimer = new StopWatch();
    private static long aresMineDelay = 90L;
    public static boolean delayInitialized = false;

    public static boolean isHeadBlocked() {
        return mc.world != null && mc.player != null && !mc.world.getBlockState(mc.player.getBlockPos().up(2)).isAir();
    }

    public static double getFallDistance() {
        return mc.player != null && !mc.player.isOnGround() ? mc.player.fallDistance : 0.0;
    }

    public static boolean isBestMoment(boolean flag, boolean flag2, boolean flag3) {
        if (mc.player == null) {
            return false;
        }

        if (hasBadPotion()) {
            return true;
        }

        StatusEffectInstance statusEffectInstance = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
        StatusEffectInstance statusEffectInstance2 = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
        if (statusEffectInstance != null && statusEffectInstance2 != null && statusEffectInstance.getAmplifier() == 9 && statusEffectInstance2.getAmplifier() == 0) {
            return true;
        }

        if (!flag) {
            return true;
        }

        double d = 0.0;
        if (ServerManager.isServer("shard")) {
            d = 0.2;
        }

        double d2 = getFallDistance();
        boolean flag4 = !mc.player.isOnGround() && d2 > d;
        boolean flag5 = CombatUtil.getCeilingGap(2) < 0.1F;
        boolean flag6;
        if (!mc.player.isTouchingWater()) {
            flag6 = mc.player.isInLava();
        } else {
            flag6 = true;
        }

        boolean flag7;
        if (!mc.player.isSubmergedIn(FluidTags.WATER)) {
            flag7 = mc.player.isSubmergedIn(FluidTags.LAVA);
        } else {
            flag7 = true;
        }

        boolean flag8;
        if (!NoWeb.shouldApplyWebSlowdown()) {
            flag8 = ServerManager.isInBerryBush();
        } else {
            flag8 = true;
        }

        boolean flag9;
        if ((mc.player.input.playerInput.jump() || !flag6) && !flag7) {
            flag9 = flag8;
        } else {
            flag9 = true;
        }

        Boolean flag10 = ScriptAuraHooks.checkHitMoment(flag4, flag5);
        if (flag10 != null) {
            if (flag10) {
                return true;
            }
        } else {
            if (flag5) {
                return true;
            }

            if (flag4) {
                if (!isAresMineDelayActive()) {
                    return true;
                }

                return false;
            }
        }

        return flag9
            || !mc.player.input.playerInput.jump() && ((TicksOnGroundAccessor)mc.player).sadnes$getTicksOnGround() > 6 && flag3
            || mc.player.isClimbing()
            || mc.player.isGliding()
            || mc.player.hasVehicle()
            || mc.player.getAbilities().flying
            || mc.player.getY() < -64.0;
    }

    private static boolean isAresMineDelayActive() {
        return ServerManager.isAresMine() && !airTimer.hasPassed((double)aresMineDelay) ? FallUtil.getTicksToGround() > 1 : false;
    }

    public static boolean hasBadPotion() {
        if (mc.player == null) {
            return false;
        }

        for (StatusEffectInstance statusEffectInstance : mc.player.getStatusEffects()) {
            if (statusEffectInstance.getEffectType().value() == StatusEffects.BLINDNESS.value()
                || statusEffectInstance.getEffectType().value() == StatusEffects.LEVITATION.value()
                || statusEffectInstance.getEffectType().value() == StatusEffects.SLOW_FALLING.value()) {
                return true;
            }
        }

        return false;
    }

    @EventHandler
    public static void onTick(TickEvent tickEvent) {
        if (mc.player != null) {
            if (!delayInitialized) {
                aresMineDelay = ThreadLocalRandom.current().nextLong(0L, 150L);
                delayInitialized = true;
            }

            if (getFallDistance() <= 0.0) {
                airTimer.reset();
            }
        }
    }

    private CriticalUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
