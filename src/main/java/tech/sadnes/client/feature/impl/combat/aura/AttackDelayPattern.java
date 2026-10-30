package tech.sadnes.client.feature.impl.combat.aura;

import java.lang.ref.WeakReference;
import java.util.SplittableRandom;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.util.IMinecraft;

public final class AttackDelayPattern implements IMinecraft {
    private static final int PATTERN_SIZE = 100;
    private static final double MIN_DELAY = 460.0;
    private static final double MAX_DELAY = 650.0;
    private static SplittableRandom random;
    private static WeakReference<Object> connectionRef = new WeakReference<>(null);
    private static long seed;
    private static double baseDelay;
    private static double concentration;
    private static double outlierChance;
    private static double driftAmplitude;
    private static double driftDecay;
    private static double smoothing;
    private static double drift;
    private static double lastDelay = -1.0;
    private static final long[] PATTERN = new long[100];
    private static int index;

    private AttackDelayPattern() {
    }

    public static long getCurrentDelay() {
        ensureSeeded();
        return PATTERN[index];
    }

    public static void advance() {
        ensureSeeded();
        if (++index >= 100) {
            regenerate();
        }
    }

    private static void ensureSeeded() {
        ClientPlayNetworkHandler clientPlayNetworkHandler = mc.getNetworkHandler();
        if (random == null || connectionRef.get() != clientPlayNetworkHandler) {
            connectionRef = new WeakReference<>(clientPlayNetworkHandler);
            String string = mc.getSession() != null ? mc.getSession().getUsername() : "";
            seed = string.hashCode() * -7046029254386353131L ^ System.currentTimeMillis();
            random = new SplittableRandom(seed);
            baseDelay = 505.0 + random.nextDouble() * 50.0;
            concentration = 3.0 + random.nextDouble() * 6.0;
            outlierChance = 0.03 + random.nextDouble() * 0.07;
            driftAmplitude = 8.0 + random.nextDouble() * 12.0;
            driftDecay = 0.06 + random.nextDouble() * 0.06;
            smoothing = 0.15 + random.nextDouble() * 0.2;
            drift = 0.0;
            lastDelay = -1.0;
            regenerate();
        }
    }

    private static void regenerate() {
        for (int i = 0; i < 100; i++) {
            drift = MathHelper.clamp(drift - driftDecay * drift + random.nextGaussian() * driftAmplitude * 0.5, -driftAmplitude, driftAmplitude);
            double d = baseDelay + drift;
            double d2;
            if (random.nextDouble() < outlierChance) {
                d2 = 482.0 + random.nextDouble() * 16.0;
            } else {
                double d3 = MathHelper.clamp((d - 460.0) / 190.0, 0.08, 0.92);
                d2 = 460.0 + 190.0 * sampleBeta(1.0 + d3 * concentration, 1.0 + (1.0 - d3) * concentration);
                if (lastDelay >= 0.0) {
                    d2 = MathHelper.clamp(d2 + smoothing * (lastDelay - d), 460.0, 650.0);
                }
            }

            lastDelay = d2;
            PATTERN[i] = Math.round(d2);
        }

        index = 0;
    }

    private static double sampleBeta(double d, double d2) {
        double d3 = sampleGamma(d);
        return d3 / (d3 + sampleGamma(d2));
    }

    private static double sampleGamma(double d) {
        if (d < 1.0) {
            return sampleGamma(d + 1.0) * Math.pow(random.nextDouble(), 1.0 / d);
        }

        double d2 = d - 0.3333333333333333;
        double d3 = 1.0 / Math.sqrt(9.0 * d2);

        while (true) {
            double d4 = random.nextGaussian();
            double d5 = 1.0 + d3 * d4;
            if (!(d5 <= 0.0)) {
                double d6 = d5 * d5 * d5;
                double d7 = random.nextDouble();
                if (d7 < 1.0 - 0.0331 * d4 * d4 * d4 * d4) {
                    return d2 * d6;
                }

                if (Math.log(d7) < 0.5 * d4 * d4 + d2 * (1.0 - d6 + Math.log(d6))) {
                    return d2 * d6;
                }
            }
        }
    }

    public static String getPreview() {
        ensureSeeded();
        StringBuilder builder = new StringBuilder();
        int i = index;

        for (int i2 = Math.min(index + 5, 100); i < i2; i++) {
            builder.append(PATTERN[i]).append(' ');
        }

        return builder.toString().trim();
    }

    public static long getSeed() {
        return seed;
    }
}
