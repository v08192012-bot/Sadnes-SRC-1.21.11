package tech.sadnes.client.util.math;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import tech.sadnes.client.util.Callback;

public final class FixedRateTicker {
    private long lastTime = Util.getMeasuringTimeNano();
    private int pendingTicks;
    private final boolean limitByFps;
    private int rate;
    private long intervalNanos;

    public FixedRateTicker(boolean flag) {
        this.limitByFps = flag;
        this.pendingTicks = 0;
        this.rate = 0;
        this.intervalNanos = 0L;
    }

    public void tick(int i, Callback... callbacks) {
        int i2 = Math.max(1, i);
        if (this.rate != i2) {
            this.intervalNanos = 1000000000L / i2;
            this.rate = i2;
        }

        long l = Util.getMeasuringTimeNano();
        long l2 = l - this.lastTime;
        this.pendingTicks = this.pendingTicks + (int)(l2 / this.intervalNanos);
        this.lastTime = this.lastTime + this.pendingTicks * this.intervalNanos;
        int i3 = this.limitByFps ? Math.min(this.rate, Math.max(1, MinecraftClient.getInstance().getCurrentFps())) : this.rate;

        for (this.pendingTicks = Math.min(this.pendingTicks, i3); this.pendingTicks > 0; this.pendingTicks--) {
            for (Callback callback : callbacks) {
                callback.run();
            }
        }
    }

    public boolean isDue(int i) {
        int i2 = Math.max(1, i);
        long l = 1000000000L / i2;
        long l2 = Util.getMeasuringTimeNano();
        return l2 - this.lastTime >= l;
    }
}
