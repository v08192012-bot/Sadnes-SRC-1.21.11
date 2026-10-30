package tech.sadnes.client.render.font;

import java.awt.image.BufferedImage;
import net.minecraft.client.texture.NativeImage;

public final class SdfGenerator {
    private static final float INFINITY = 1.0E20F;

    private SdfGenerator() {
    }

    public static BufferedImage generateSdf(BufferedImage bufferedImage, float f) {
        int i = bufferedImage.getWidth();
        int i2 = bufferedImage.getHeight();
        int i3 = i * i2;
        float[] floats = new float[i3];
        float[] floats2 = new float[i3];
        int i4 = 0;

        for (int i5 = 0; i5 < i2; i5++) {
            for (int i6 = 0; i6 < i; i6++) {
                int i7 = bufferedImage.getRGB(i6, i5) >>> 24;
                boolean flag;
                float[] floats3;
                int i8;
                float f2;
                if (i7 >= 127) {
                    flag = true;
                    floats3 = floats;
                    i8 = i4;
                    f2 = 0.0F;
                } else {
                    flag = false;
                    floats3 = floats;
                    i8 = i4;
                    f2 = 1.0E20F;
                }

                floats3[i8] = f2;
                floats2[i4] = flag ? 1.0E20F : 0.0F;
                i4++;
            }
        }

        distanceTransform2d(floats, i, i2);
        distanceTransform2d(floats2, i, i2);
        BufferedImage bufferedImage2 = new BufferedImage(i, i2, 2);
        float f3 = 1.0F / (2.0F * f);
        int i9 = 0;

        for (int i10 = 0; i10 < i2; i10++) {
            for (int i11 = 0; i11 < i; i11++) {
                float f4 = (float)Math.sqrt(floats2[i9]);
                float f5 = (float)Math.sqrt(floats[i9]);
                float f6 = f4 - f5;
                float f7 = clamp(0.5F + f6 * f3, 0.0F, 1.0F);
                int i12 = Math.round(f7 * 255.0F);
                bufferedImage2.setRGB(i11, i10, i12 << 24 | 16777215);
                i9++;
            }
        }

        return bufferedImage2;
    }

    public static BufferedImage downsample(BufferedImage bufferedImage, int i) {
        if (i <= 1) {
            return bufferedImage;
        }

        int i2 = bufferedImage.getWidth() / i;
        int i3 = bufferedImage.getHeight() / i;
        int i4 = i * i;
        BufferedImage bufferedImage2 = new BufferedImage(i2, i3, 2);

        for (int i5 = 0; i5 < i3; i5++) {
            for (int i6 = 0; i6 < i2; i6++) {
                int i7 = 0;

                for (int i8 = 0; i8 < i; i8++) {
                    for (int i9 = 0; i9 < i; i9++) {
                        i7 += bufferedImage.getRGB(i6 * i + i9, i5 * i + i8) >>> 24;
                    }
                }

                int i10 = i7 / i4;
                bufferedImage2.setRGB(i6, i5, i10 << 24 | 16777215);
            }
        }

        return bufferedImage2;
    }

    public static NativeImage toNativeImage(BufferedImage bufferedImage) {
        int i = bufferedImage.getWidth();
        int i2 = bufferedImage.getHeight();
        NativeImage nativeImage = new NativeImage(i, i2, false);

        for (int i3 = 0; i3 < i2; i3++) {
            for (int i4 = 0; i4 < i; i4++) {
                nativeImage.setColorArgb(i4, i3, bufferedImage.getRGB(i4, i3));
            }
        }

        return nativeImage;
    }

    private static void distanceTransform2d(float[] floats, int i, int i2) {
        int i3 = Math.max(i, i2);
        float[] floats2 = new float[i3];
        int[] ints = new int[i3];
        float[] floats3 = new float[i3 + 1];
        float[] floats4 = new float[floats.length];

        for (int i4 = 0; i4 < i; i4++) {
            for (int i5 = 0; i5 < i2; i5++) {
                floats2[i5] = floats[i5 * i + i4];
            }

            distanceTransform1d(floats2, i2, ints, floats3);

            for (int i6 = 0; i6 < i2; i6++) {
                floats4[i6 * i + i4] = floats2[i6];
            }
        }

        for (int i7 = 0; i7 < i2; i7++) {
            int i8 = i7 * i;

            for (int i9 = 0; i9 < i; i9++) {
                floats2[i9] = floats4[i8 + i9];
            }

            distanceTransform1d(floats2, i, ints, floats3);

            for (int i10 = 0; i10 < i; i10++) {
                floats[i8 + i10] = floats2[i10];
            }
        }
    }

    private static void distanceTransform1d(float[] floats, int i, int[] ints, float[] floats2) {
        int i2 = 0;
        ints[0] = 0;
        floats2[0] = -1.0E20F;
        floats2[1] = 1.0E20F;

        for (int i3 = 1; i3 < i; i3++) {
            float f = intersect(floats, i3, ints[i2]);

            while (f <= floats2[i2]) {
                f = intersect(floats, i3, ints[--i2]);
            }

            ints[++i2] = i3;
            floats2[i2] = f;
            floats2[i2 + 1] = 1.0E20F;
        }

        int i4 = 0;

        for (int i5 = 0; i5 < i; i5++) {
            while (floats2[i4 + 1] < i5) {
                i4++;
            }

            float f2 = i5 - ints[i4];
            floats[i5] = f2 * f2 + floats[ints[i4]];
        }
    }

    private static float intersect(float[] floats, int i, int i2) {
        float f = floats[i] + i * i;
        float f2 = floats[i2] + i2 * i2;
        return (f - f2) / (2.0F * (i - i2));
    }

    private static float clamp(float f, float f2, float f3) {
        if (f < f2) {
            return f2;
        } else {
            return f > f3 ? f3 : f;
        }
    }
}
