package tech.sadnes.client.setting.curve;

public enum CurvePreset {
    MAGNET("curve.preset.magnet", 1.0F, 0.12F, 0.72F, 0.4F, 0.28F, 0.02F),
    LINEAR("curve.preset.linear", 1.0F, 0.333F, 0.667F, 0.667F, 0.333F, 0.0F),
    BELL("curve.preset.bell", 0.08F, 0.3F, 1.0F, 0.7F, 1.0F, 0.06F),
    SCURVE("curve.preset.scurve", 1.0F, 0.28F, 0.96F, 0.72F, 0.04F, 0.0F);

    private final String translationKey;
    private final float startY;
    private final float control1X;
    private final float control1Y;
    private final float control2X;
    private final float control2Y;
    private final float endY;

    CurvePreset(String string, float f, float f2, float f3, float f4, float f5, float f6) {
        this.translationKey = string;
        this.startY = f;
        this.control1X = f2;
        this.control1Y = f3;
        this.control2X = f4;
        this.control2Y = f5;
        this.endY = f6;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public BezierCurve createCurve() {
        return new BezierCurve(this.startY, this.control1X, this.control1Y, this.control2X, this.control2Y, this.endY);
    }
}
