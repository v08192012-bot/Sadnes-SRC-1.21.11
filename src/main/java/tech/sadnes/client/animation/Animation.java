package tech.sadnes.client.animation;

import tech.sadnes.client.util.math.Duration;

public class Animation {
    private static final double EPSILON = 1.0E-6;
    private long startTime;
    private boolean running;
    private double duration;
    private double fromValue;
    private double toValue;
    private double value;
    private double prevValue;
    private Easing easing = Easings.LINEAR;
    private boolean reversed = false;
    private Runnable onFinish;

    public Animation animate(double d, double d2) {
        return this.animate(d, d2, Easings.LINEAR, false);
    }

    public Animation animate(double d, double d2, Easing easing2) {
        return this.animate(d, d2, easing2, false);
    }

    public Animation animate(double d, double d2, boolean flag) {
        return this.animate(d, d2, Easings.LINEAR, flag);
    }

    public Animation animate(double d, double d2, Easing easing2, boolean flag) {
        if (this.isAlreadyTarget(d)) {
            return this;
        }

        if (!this.shouldSkip(flag, d)) {
            this.setEasing(easing2)
                .setDuration(d2 * 1000.0)
                .setStartTime(System.currentTimeMillis())
                .setFromValue(this.getValue())
                .setToValue(d)
                .setRunning(true);
        }

        return this;
    }

    public Animation animate(double d, Duration duration2) {
        return this.animate(d, duration2, Easings.LINEAR, false);
    }

    public Animation animate(double d, Duration duration2, Easing easing2) {
        return this.animate(d, duration2, easing2, false);
    }

    public Animation animate(double d, Duration duration2, boolean flag) {
        return this.animate(d, duration2, Easings.LINEAR, flag);
    }

    public Animation animate(double d, Duration duration2, Easing easing2, boolean flag) {
        if (this.isAlreadyTarget(d)) {
            return this;
        }

        if (!this.shouldSkip(flag, d)) {
            this.setEasing(easing2)
                .setDuration(duration2.getValue())
                .setStartTime(System.currentTimeMillis())
                .setFromValue(this.getValue())
                .setToValue(d)
                .setRunning(true);
        }

        return this;
    }

    public boolean update() {
        this.setPrevValue(this.getValue());
        if (!this.isRunning()) {
            return false;
        }

        boolean flag = this.isAnimating();
        if (flag) {
            this.setValue(this.interpolate(this.getFromValue(), this.getToValue(), this.getEasing().ease(this.getProgress())));
            return flag;
        }

        this.setStartTime(0L).setRunning(false);
        this.setValue(this.getToValue());
        if (this.onFinish != null) {
            this.onFinish.run();
            this.onFinish = null;
        }

        return flag;
    }

    public boolean isAnimating() {
        return !this.isFinished();
    }

    public boolean isFinished() {
        return !this.isRunning() || this.getProgress() >= 1.0;
    }

    public double getProgress() {
        return this.isRunning() && !(this.getDuration() <= 0.0) ? (System.currentTimeMillis() - this.getStartTime()) / this.getDuration() : 1.0;
    }

    public boolean shouldSkip(boolean flag, double d) {
        return flag && (this.approxEquals(d, this.getToValue()) || this.approxEquals(d, this.getValue()));
    }

    public double interpolate(double d, double d2, double d3) {
        return d + (d2 - d) * d3;
    }

    public Animation setStartTime(long l) {
        this.startTime = l;
        return this;
    }

    public Animation setDuration(double d) {
        this.duration = d;
        return this;
    }

    public Animation setFromValue(double d) {
        this.fromValue = d;
        return this;
    }

    public Animation setToValue(double d) {
        this.toValue = d;
        return this;
    }

    public Animation setValue(double d) {
        this.value = d;
        return this;
    }

    public Animation setPrevValue(double d) {
        this.prevValue = d;
        return this;
    }

    public Animation setEasing(Easing easing2) {
        this.easing = easing2;
        return this;
    }

    public Animation setReversed(boolean flag) {
        this.reversed = flag;
        return this;
    }

    public Animation setOnFinish(Runnable runnable) {
        this.onFinish = runnable;
        return this;
    }

    public float get() {
        return (float)this.getValue();
    }

    public float getPrev() {
        return (float)this.getPrevValue();
    }

    public void forceValue(double d) {
        this.animate(d, 0.0);
        this.update();
        this.setValue(d);
    }

    private boolean isAlreadyTarget(double d) {
        return this.isRunning() ? this.approxEquals(d, this.getToValue()) : this.approxEquals(d, this.getValue());
    }

    private boolean approxEquals(double d, double d2) {
        return Math.abs(d - d2) <= 1.0E-6;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public boolean isRunning() {
        return this.running;
    }

    public double getDuration() {
        return this.duration;
    }

    public double getFromValue() {
        return this.fromValue;
    }

    public double getToValue() {
        return this.toValue;
    }

    public double getValue() {
        return this.value;
    }

    public double getPrevValue() {
        return this.prevValue;
    }

    public Easing getEasing() {
        return this.easing;
    }

    public boolean isReversed() {
        return this.reversed;
    }

    public Runnable getOnFinish() {
        return this.onFinish;
    }

    public void setRunning(boolean flag) {
        this.running = flag;
    }

    public void setFinishCallback(Runnable runnable) {
        this.onFinish = runnable;
    }
}
