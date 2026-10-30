package tech.sadnes.client.util.math;


public class StopWatch {
    private long time;

    public StopWatch() {
        this.reset();
    }

    public boolean hasPassed(double d) {
        return System.currentTimeMillis() - d >= this.time;
    }

    public boolean hasPassedAndReset(double d) {
        boolean flag = this.hasPassed(d);
        if (flag) {
            this.reset();
        }

        return flag;
    }

    public boolean hasPassedSelf() {
        return this.hasPassed(this.time);
    }

    public boolean hasPassed(long l) {
        return this.hasPassed((double)l);
    }

    public void reset() {
        this.time = System.currentTimeMillis();
    }

    public long getElapsed() {
        return System.currentTimeMillis() - this.time;
    }

    public boolean isStarted() {
        return this.time < System.currentTimeMillis();
    }

    public void setElapsed(long l) {
        this.time = System.currentTimeMillis() - l;
    }

    public void delay(long l) {
        this.time = System.currentTimeMillis() + l;
    }

    public boolean isElapsed(float f) {
        return this.isElapsed((long)f);
    }

    public boolean isElapsed(long l) {
        return System.currentTimeMillis() - this.time > l;
    }

    public long getTime() {
        return this.time;
    }
}
