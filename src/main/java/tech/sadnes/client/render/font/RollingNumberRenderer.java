package tech.sadnes.client.render.font;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.util.color.ColorUtil;

public final class RollingNumberRenderer {
    private static final String[] DIGITS = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private final double[] digitValues = new double[10];
    private long lastUpdateTime;
    private boolean initialized;

    public void reset() {
        this.initialized = false;
    }

    public void update(long l) {
        long l2 = System.nanoTime();
        double d = Math.min(0.05, (l2 - this.lastUpdateTime) / 1.0E9);
        this.lastUpdateTime = l2;
        long l3 = Math.clamp(l, 0L, 9999999999L);
        long l4 = 1L;
        int i = this.digitValues.length;

        for (int i2 = 0; i2 < i; l4 *= 10L) {
            long l5 = l3 / l4;
            if (!this.initialized) {
                this.digitValues[i2] = l5;
            } else {
                this.digitValues[i2] = this.digitValues[i2] + 10.0 * (long)((l5 - this.digitValues[i2]) / 10.0);
                double d2 = l5 - this.digitValues[i2];
                double d3 = MathHelper.clamp(d2 * (1.0 - Math.exp(-d / 0.07)), -20.0 * d, 20.0 * d);
                this.digitValues[i2] = Math.abs(d2 - d3) < 0.002 ? l5 : this.digitValues[i2] + d3;
            }

            i2++;
        }

        this.initialized = true;
    }

    public float getWidth(MsdfFont msdfFont, float f) {
        float f2 = 0.0F;
        int i = this.digitValues.length;

        for (int i2 = 0; i2 < i; i2++) {
            f2 += this.getDigitVisibility(i2);
        }

        return f2 * getMaxDigitWidth(msdfFont, f);
    }

    public void render(DrawContext drawContext, MsdfFont msdfFont, float f, float f2, float f3, int i) {
        float f4 = getMaxDigitWidth(msdfFont, f3);
        float f5 = msdfFont.getHeight(f3);
        float f6 = f + this.getWidth(msdfFont, f3);
        int i2 = this.digitValues.length;

        for (int i3 = 0; i3 < i2; i3++) {
            float f7 = this.getDigitVisibility(i3);
            if (!(f7 <= 0.0F)) {
                long l = (long)Math.floor(this.digitValues[i3]);
                float f8 = (float)(this.digitValues[i3] - l);
                int i4 = 0;

                while (i4 < 2) {
                    long l2 = l + i4;
                    float f9 = (i4 - f8) * f5;
                    float f10 = Math.abs(f9) / f5;
                    if (i3 > 0 && l2 == 0L) {
                        i4++;
                    } else {
                        if (!(f10 >= 1.0F)) {
                            String string = DIGITS[(int)(l2 % 10L)];
                            float f11 = 1.0F - f10 * f10 * (3.0F - 2.0F * f10);
                            float f12 = f6 - f4 + (f4 - msdfFont.getWidth(string, f3)) * 0.5F;
                            msdfFont.draw(drawContext, string, f12, f2 + f9, ColorUtil.multiplyAlpha(i, f11), f3);
                        }

                        i4++;
                    }
                }

                f6 -= f4 * f7;
            }
        }
    }

    private float getDigitVisibility(int i) {
        return i == 0 ? 1.0F : (float)MathHelper.clamp(this.digitValues[i], 0.0, 1.0);
    }

    private static float getMaxDigitWidth(MsdfFont msdfFont, float f) {
        float f2 = 0.0F;

        for (String string : DIGITS) {
            float f3 = msdfFont.getWidth(string, f);
            if (f3 > f2) {
                f2 = f3;
            }
        }

        return f2;
    }
}
