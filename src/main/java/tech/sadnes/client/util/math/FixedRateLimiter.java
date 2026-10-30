package tech.sadnes.client.util.math;

import net.minecraft.util.Util;

public final class FixedRateLimiter {
    private long lastTimeNs = Util.getMeasuringTimeNano();
    private int pendingTicks;
    private int rate;
    private long intervalNs;

    public void run(int i, Runnable... runnables) {
        if (this.rate != i) {
            this.intervalNs = 1000000000L / i;
            this.rate = i;
        }

        long l = Util.getMeasuringTimeNano();
        long l2 = l - this.lastTimeNs;
        this.pendingTicks = this.pendingTicks + (int)(l2 / this.intervalNs);
        this.lastTimeNs = this.lastTimeNs + this.pendingTicks * this.intervalNs;

        for (this.pendingTicks = Math.min(this.pendingTicks, 1); this.pendingTicks > 0; this.pendingTicks--) {
            for (Runnable runnable : runnables) {
                runnable.run();
            }
        }
    }
}
