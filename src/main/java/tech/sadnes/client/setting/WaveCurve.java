package tech.sadnes.client.setting;

import java.util.Arrays;

public final class WaveCurve {
    public static final int MIN_POINTS = 1;
    public static final int MAX_POINTS = 48;
    private static final float MAX_POSITION = 0.999F;
    private final float[] positions = new float[48];
    private final float[] values = new float[48];
    private int pointCount;

    private WaveCurve() {
    }

    public static WaveCurve createDefault() {
        WaveCurve waveCurve = new WaveCurve();
        waveCurve.addPoint(0.5F, 0.0F);
        return waveCurve;
    }

    public float getMaxAmplitude() {
        float f = 0.0F;

        for (int i = 0; i < this.pointCount; i++) {
            f = Math.max(f, Math.abs(this.values[i]));
        }

        return f;
    }

    public int getPointCount() {
        return this.pointCount;
    }

    public float getPosition(int i) {
        return this.positions[i];
    }

    public float getPointValue(int i) {
        return this.values[i];
    }

    public int setPoint(int i, float f, float f2) {
        this.positions[i] = clamp(f, 0.0F, 0.999F);
        this.values[i] = clamp(f2, -1.0F, 1.0F);

        while (i > 0 && this.positions[i - 1] > this.positions[i]) {
            this.swapPoints(i--, i);
        }

        while (i < this.pointCount - 1 && this.positions[i + 1] < this.positions[i]) {
            this.swapPoints(i++, i);
        }

        return i;
    }

    public int addPoint(float f, float f2) {
        if (this.pointCount >= 48) {
            return -1;
        }

        this.positions[this.pointCount] = 1.0F;
        this.pointCount++;
        return this.setPoint(this.pointCount - 1, f, f2);
    }

    public boolean removePoint(int i) {
        if (this.pointCount <= 1) {
            return false;
        }

        System.arraycopy(this.positions, i + 1, this.positions, i, this.pointCount - i - 1);
        System.arraycopy(this.values, i + 1, this.values, i, this.pointCount - i - 1);
        this.pointCount--;
        return true;
    }

    public float sample(float f) {
        float f2 = f - (float)Math.floor(f);
        int i = this.pointCount - 1;

        for (int i2 = 0; i2 < this.pointCount; i2++) {
            if (this.positions[i2] <= f2) {
                i = i2;
            }
        }

        int i3 = (i + 1) % this.pointCount;
        float f3 = wrap(this.positions[i3] - this.positions[i]);
        if (f3 < 1.0E-4F) {
            return this.values[i];
        }

        float f4 = wrap(f2 - this.positions[i]) / f3;
        float f5 = f4 * f4;
        float f6 = f5 * f4;
        float f7 = (2.0F * f6 - 3.0F * f5 + 1.0F) * this.values[i]
            + (f6 - 2.0F * f5 + f4) * this.getTangent(i) * f3
            + (-2.0F * f6 + 3.0F * f5) * this.values[i3]
            + (f6 - f5) * this.getTangent(i3) * f3;
        return clamp(f7, -1.0F, 1.0F);
    }

    public void copyFrom(WaveCurve waveCurve) {
        this.pointCount = waveCurve.pointCount;
        System.arraycopy(waveCurve.positions, 0, this.positions, 0, this.pointCount);
        System.arraycopy(waveCurve.values, 0, this.values, 0, this.pointCount);
    }

    public WaveCurve copy() {
        WaveCurve waveCurve = new WaveCurve();
        waveCurve.copyFrom(this);
        return waveCurve;
    }

    public float[] toArray() {
        float[] floats = new float[this.pointCount * 2];

        for (int i = 0; i < this.pointCount; i++) {
            floats[i * 2] = this.positions[i];
            floats[i * 2 + 1] = this.values[i];
        }

        return floats;
    }

    public static WaveCurve fromArray(float[] floats) {
        if (floats != null && floats.length >= 2) {
            WaveCurve waveCurve = new WaveCurve();
            int i = Math.min(floats.length / 2, 48);

            for (int i2 = 0; i2 < i; i2++) {
                waveCurve.addPoint(floats[i2 * 2], floats[i2 * 2 + 1]);
            }

            return waveCurve;
        } else {
            return createDefault();
        }
    }

    private float getTangent(int i) {
        int i2 = (i - 1 + this.pointCount) % this.pointCount;
        int i3 = (i + 1) % this.pointCount;
        float f = Math.max(wrap(this.positions[i3] - this.positions[i2]), 1.0E-4F);
        return (this.values[i3] - this.values[i2]) / f;
    }

    private void swapPoints(int i, int i2) {
        float f = this.positions[i];
        float f2 = this.values[i];
        this.positions[i] = this.positions[i2];
        this.values[i] = this.values[i2];
        this.positions[i2] = f;
        this.values[i2] = f2;
    }

    private static float wrap(float f) {
        return f < 0.0F ? f + 1.0F : f;
    }

    private static float clamp(float f, float f2, float f3) {
        return f < f2 ? f2 : Math.min(f, f3);
    }

    @Override
    public boolean equals(Object object) {
        return this == object ? true : object instanceof WaveCurve waveCurve && Arrays.equals(this.toArray(), waveCurve.toArray());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.toArray());
    }
}
