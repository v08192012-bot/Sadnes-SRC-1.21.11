package tech.sadnes.client.render;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.util.color.Theme;

public final class GlowBorderRenderer {
    private GlowBorderRenderer() {
    }

    public static void drawGlowBorder(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        drawGlowBorder(drawContext, f, f2, f3, f4, f5, f6, Theme.getPrimary());
    }

    public static void drawGlowBorder(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        if (!(f5 <= 0.01F) && !(f6 <= 0.01F)) {
            for (float[] floats : new float[][]{
                {f - 7.0F, f2 - 7.0F, f3 + 2.0F * 7.0F, 7.0F},
                {f - 7.0F, f2 + f4, f3 + 2.0F * 7.0F, 7.0F},
                {f - 7.0F, f2, 7.0F, f4},
                {f + f3, f2, 7.0F, f4}
            }) {
                boolean flag = ScissorStack.push(drawContext, floats[0], floats[1], floats[2], floats[3]);

                try {
                    drawGlow(drawContext, f, f2, f3, f4, f5, f6, 5.0F, i);
                } finally {
                    if (flag) {
                        ScissorStack.pop(drawContext);
                    }
                }
            }
        }
    }

    public static void drawGlow(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        drawGlow(drawContext, f, f2, f3, f4, f5, f6, 5.0F, Theme.getPrimary(), 1.0F);
    }

    public static void drawGlow(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        drawGlow(drawContext, f, f2, f3, f4, f5, f6, f7, Theme.getPrimary(), 1.0F);
    }

    public static void drawGlow(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, float f7, int i) {
        drawGlow(drawContext, f, f2, f3, f4, f5, f6, f7, i, 1.0F);
    }

    public static void drawGlow(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, float f7, int i, float f8) {
        drawGlow(drawContext, f, f2, f3, f4, f5, f6, f7, i, f8, 0.0F, 1.0F);
    }

    public static void drawGlow(
        DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, float f7, int i, float f8, float f9
    ) {
        drawGlow(drawContext, f, f2, f3, f4, f5, f6, f7, i, f8, f9, 1.0F);
    }

    public static void drawGlow(
        DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, float f7, int i, float f8, float f9, float f10
    ) {
        if (!(f5 <= 0.01F) && !(f6 <= 0.01F)) {
            long l = System.currentTimeMillis();
            float f11 = 0.5F + 0.5F * (float)Math.sin(l / 1500.0);
            float f12 = f5 * f6;
            float f13 = (0.6F + 0.4F * f11) * f12 * f10;
            Radius radius = Radius.all(f7);
            new RenderUtil(drawContext, f, f2, f3, f4).border(6.0F * f8, i, 0.1F * f13, radius);
            new RenderUtil(drawContext, f, f2, f3, f4).border(3.0F * f8, i, 0.18F * f13, radius);
            new RenderUtil(drawContext, f, f2, f3, f4).border(1.5F * f8, i, 0.26F * f13, radius);
            float f14 = 3.0F * f8;
            float f15 = Math.max(1.2F * f8, 0.8F);
            float f16 = 2.0F * (f3 + f4);
            float f17 = (float)(l % 2600L) / 2600.0F;
            int i2 = (int)Math.min(f16 / f15, 800.0F);
            float[] floats = new float[2];
            float[] floats2 = new float[2];

            for (int i3 = 0; i3 < i2; i3++) {
                float f18 = (float)i3 / i2;
                float f19 = Math.abs(f18 - f17);
                if (f19 > 0.5F) {
                    f19 = 1.0F - f19;
                }

                float f20 = 1.0F - f19 / 0.5F;
                if (!(f20 <= 0.04F)) {
                    float f21 = f20 * f20;
                    pointOnRoundedRect(f18, f, f2, f3, f4, f7, floats);
                    float f22 = f18 + 1.0F / i2;
                    if (f22 >= 1.0F) {
                        f22--;
                    }

                    pointOnRoundedRect(f22, f, f2, f3, f4, f7, floats2);
                    float f23 = floats2[0] - floats[0];
                    float f24 = floats2[1] - floats[1];
                    float f25 = (float)Math.sqrt(f23 * f23 + f24 * f24);
                    float f26 = floats[0] + (f25 > 1.0E-4F ? f24 / f25 : 0.0F) * f9;
                    float f27 = floats[1] - (f25 > 1.0E-4F ? f23 / f25 : 0.0F) * f9;
                    new RenderUtil(drawContext, f26 - 0.6F, f27 - 0.6F, 0.6F * 2.0F, 0.6F * 2.0F)
                        .border(f14, i, f21 * 0.55F * f12 * f10, Radius.all(0.6F));
                }
            }
        }
    }

    public static void pointOnRoundedRect(float f, float f2, float f3, float f4, float f5, float f6, float[] floats) {
        float f7 = Math.min(f6, Math.min(f4, f5) / 2.0F);
        float f8 = Math.max(f4 - 2.0F * f7, 0.0F);
        float f9 = Math.max(f5 - 2.0F * f7, 0.0F);
        float f10 = (float) (Math.PI / 2) * f7;
        float f11 = 2.0F * f8 + 2.0F * f9 + 4.0F * f10;
        float f12 = f * f11;
        if (f12 <= f8) {
            floats[0] = f2 + f7 + f12;
            floats[1] = f3;
        } else {
            float f13 = f12 - f8;
            if (f13 <= f10) {
                float f14 = -(float) (Math.PI / 2) + f13 / f10 * (float) (Math.PI / 2);
                floats[0] = f2 + f4 - f7 + f7 * (float)Math.cos(f14);
                floats[1] = f3 + f7 + f7 * (float)Math.sin(f14);
            } else {
                float f15 = f13 - f10;
                if (f15 <= f9) {
                    floats[0] = f2 + f4;
                    floats[1] = f3 + f7 + f15;
                } else {
                    float f16 = f15 - f9;
                    if (f16 <= f10) {
                        float f17 = f16 / f10 * (float) (Math.PI / 2);
                        floats[0] = f2 + f4 - f7 + f7 * (float)Math.cos(f17);
                        floats[1] = f3 + f5 - f7 + f7 * (float)Math.sin(f17);
                    } else {
                        float f18 = f16 - f10;
                        if (f18 <= f8) {
                            floats[0] = f2 + f4 - f7 - f18;
                            floats[1] = f3 + f5;
                        } else {
                            float f19 = f18 - f8;
                            if (f19 <= f10) {
                                float f20 = (float) (Math.PI / 2) + f19 / f10 * (float) (Math.PI / 2);
                                floats[0] = f2 + f7 + f7 * (float)Math.cos(f20);
                                floats[1] = f3 + f5 - f7 + f7 * (float)Math.sin(f20);
                            } else {
                                float f21 = f19 - f10;
                                if (f21 <= f9) {
                                    floats[0] = f2;
                                    floats[1] = f3 + f5 - f7 - f21;
                                } else {
                                    float f22 = f21 - f9;
                                    float f23 = (float) Math.PI + f22 / f10 * (float) (Math.PI / 2);
                                    floats[0] = f2 + f7 + f7 * (float)Math.cos(f23);
                                    floats[1] = f3 + f7 + f7 * (float)Math.sin(f23);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
