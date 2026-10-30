package tech.sadnes.client.util.math;

public final class TimerUtil {
    private long time = System.currentTimeMillis();

    public long getTime() {
        return this.time;
    }

    public void reset() {
        this.time = System.currentTimeMillis();
    }

    public boolean hasPassed(long l) {
        return System.currentTimeMillis() - this.time > l;
    }

    public void setDelay(long l) {
        this.time = System.currentTimeMillis() + l;
    }

    public void setTime(long l) {
        this.time = l;
    }

    public boolean hasPassed(double d) {
        return System.currentTimeMillis() - d >= this.time;
    }

    public long getElapsed() {
        return System.currentTimeMillis() - this.time;
    }

    public boolean isNotReached() {
        return System.currentTimeMillis() - this.time <= 0L;
    }

    public boolean isReached() {
        return this.time < System.currentTimeMillis();
    }
}
