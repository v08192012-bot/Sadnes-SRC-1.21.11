package tech.sadnes.client.setting.curve;

import java.util.Arrays;

public final class BezierCurve {
    private static final int SAMPLES = 48;
    private float startY;
    private float control1X;
    private float control1Y;
    private float control2X;
    private float control2Y;
    private float endY;
    private final float[] sampleX = new float[49];
    private final float[] sampleY = new float[49];
    private boolean dirty = true;

    public BezierCurve(float f, float f2, float f3, float f4, float f5, float f6) {
        this.startY = clamp01(f);
        this.control1X = clamp01(f2);
        this.control1Y = clamp01(f3);
        this.control2X = clamp01(f4);
        this.control2Y = clamp01(f5);
        this.endY = clamp01(f6);
    }

    public float getStartY() {
        return this.startY;
    }

    public float getControl1X() {
        return this.control1X;
    }

    public float getControl1Y() {
        return this.control1Y;
    }

    public float getControl2X() {
        return this.control2X;
    }

    public float getControl2Y() {
        return this.control2Y;
    }

    public float getEndY() {
        return this.endY;
    }

    public void setStartY(float f) {
        this.startY = clamp01(f);
        this.dirty = true;
    }

    public void setEndY(float f) {
        this.endY = clamp01(f);
        this.dirty = true;
    }

    public void setControl1(float f, float f2) {
        this.control1X = clamp01(f);
        this.control1Y = clamp01(f2);
        this.dirty = true;
    }

    public void setControl2(float f, float f2) {
        this.control2X = clamp01(f);
        this.control2Y = clamp01(f2);
        this.dirty = true;
    }

    public void copyFrom(BezierCurve bezierCurve) {
        this.startY = bezierCurve.startY;
        this.control1X = bezierCurve.control1X;
        this.control1Y = bezierCurve.control1Y;
        this.control2X = bezierCurve.control2X;
        this.control2Y = bezierCurve.control2Y;
        this.endY = bezierCurve.endY;
        this.dirty = true;
    }

    public void lerp(BezierCurve bezierCurve, BezierCurve bezierCurve2, float f) {
        this.startY = clamp01(lerpFloat(bezierCurve.startY, bezierCurve2.startY, f));
        this.control1X = clamp01(lerpFloat(bezierCurve.control1X, bezierCurve2.control1X, f));
        this.control1Y = clamp01(lerpFloat(bezierCurve.control1Y, bezierCurve2.control1Y, f));
        this.control2X = clamp01(lerpFloat(bezierCurve.control2X, bezierCurve2.control2X, f));
        this.control2Y = clamp01(lerpFloat(bezierCurve.control2Y, bezierCurve2.control2Y, f));
        this.endY = clamp01(lerpFloat(bezierCurve.endY, bezierCurve2.endY, f));
        this.dirty = true;
    }

    public float evaluate(float f) {
        if (this.dirty) {
            this.rebuildSamples();
        }

        float f2 = clamp01(f);

        for (int i = 0; i < 48; i++) {
            float f3 = this.sampleX[i];
            float f4 = this.sampleX[i + 1];
            if (f2 >= f3 && f2 <= f4 || f2 <= f3 && f2 >= f4) {
                float f5 = f4 - f3;
                float f6 = Math.abs(f5) < 1.0E-6F ? 0.0F : (f2 - f3) / f5;
                return clamp01(this.sampleY[i] + (this.sampleY[i + 1] - this.sampleY[i]) * f6);
            }
        }

        return clamp01(this.sampleY[48]);
    }

    public float maxDifference(BezierCurve bezierCurve) {
        float f = 0.0F;

        for (int i = 0; i <= 16; i++) {
            float f2 = i / 16.0F;
            f = Math.max(f, Math.abs(this.evaluate(f2) - bezierCurve.evaluate(f2)));
        }

        return f;
    }

    public void toScreenPoints(float[] floats) {
        floats[0] = 0.0F;
        floats[1] = 1.0F - this.startY;
        floats[2] = this.control1X;
        floats[3] = 1.0F - this.control1Y;
        floats[4] = this.control2X;
        floats[5] = 1.0F - this.control2Y;
        floats[6] = 1.0F;
        floats[7] = 1.0F - this.endY;
    }

    public BezierCurve copy() {
        return new BezierCurve(this.startY, this.control1X, this.control1Y, this.control2X, this.control2Y, this.endY);
    }

    public float[] toArray() {
        return new float[]{this.startY, this.control1X, this.control1Y, this.control2X, this.control2Y, this.endY};
    }

    public static BezierCurve fromArray(float[] floats) {
        return floats != null && floats.length >= 6 ? new BezierCurve(floats[0], floats[1], floats[2], floats[3], floats[4], floats[5]) : CurvePreset.MAGNET.createCurve();
    }

    private void rebuildSamples() {
        for (int i = 0; i <= 48; i++) {
            float f = i / 48.0F;
            this.sampleX[i] = cubicBezier(0.0F, this.control1X, this.control2X, 1.0F, f);
            this.sampleY[i] = cubicBezier(this.startY, this.control1Y, this.control2Y, this.endY, f);
        }

        this.dirty = false;
    }

    private static float cubicBezier(float f, float f2, float f3, float f4, float f5) {
        float f6 = 1.0F - f5;
        return f6 * f6 * f6 * f + 3.0F * f6 * f6 * f5 * f2 + 3.0F * f6 * f5 * f5 * f3 + f5 * f5 * f5 * f4;
    }

    private static float lerpFloat(float f, float f2, float f3) {
        return f + (f2 - f) * f3;
    }

    private static float clamp01(float f) {
        if (f < 0.0F) {
            return 0.0F;
        } else {
            return f > 1.0F ? 1.0F : f;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object instanceof BezierCurve bezierCurve
                ? this.startY == bezierCurve.startY
                    && this.control1X == bezierCurve.control1X
                    && this.control1Y == bezierCurve.control1Y
                    && this.control2X == bezierCurve.control2X
                    && this.control2Y == bezierCurve.control2Y
                    && this.endY == bezierCurve.endY
                : false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.toArray());
    }
}
