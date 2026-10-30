package tech.sadnes.client.render;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.render.texture.Icon;

public final class IconHoverAnimator {
    private float spinAngle;
    private long lastFrameTime;

    public void render(DrawContext drawContext, Icon icon2, float f, float f2, float f3, int i, float f4) {
        float f5 = f4 < 0.0F ? 0.0F : Math.min(f4, 1.0F);
        long l = System.currentTimeMillis();
        if (this.lastFrameTime == 0L) {
            this.lastFrameTime = l;
        }

        float f6 = Math.min((float)(l - this.lastFrameTime), 100.0F);
        this.lastFrameTime = l;
        float f7 = getSpinSpeed(icon2);
        if (f7 > 0.0F && f5 > 0.01F) {
            this.spinAngle = (this.spinAngle + f6 * f7 * f5) % 360.0F;
        } else if (f7 > 0.0F && this.spinAngle != 0.0F && f5 <= 0.01F) {
            this.spinAngle = 0.0F;
        }

        boolean flag = f5 <= 0.001F && this.spinAngle == 0.0F;
        if (flag) {
            new RenderUtil(drawContext, f, f2, f3, f3).icon(icon2, 0.0F, 0.0F, f3, i);
        } else {
            float f8 = f + f3 / 2.0F;
            float f9 = f2 + f3 / 2.0F;
            float f10 = 0.0F;
            float f11 = 0.0F;
            float f12 = this.spinAngle;
            switch (icon2) {
                case SETTINGS:
                case REFRESH:
                case DICE:
                    f12 += 100.0F * f5;
                    break;
                case PLUS:
                case CLOSE:
                    f12 += 90.0F * f5;
                    break;
                case STAR:
                    f12 += (float)Math.sin(l / 140.0) * 12.0F * f5;
                    break;
                case TRASH:
                    f12 += (float)Math.sin(l / 60.0) * 9.0F * f5;
                    break;
                case CHEVRON_RIGHT:
                    f10 = 1.6F * f5;
                    break;
                default:
                    f11 = -((float)Math.sin(f5 * Math.PI)) * 2.0F;
            }

            MatrixUtil.push(drawContext);
            if (f10 != 0.0F || f11 != 0.0F) {
                MatrixUtil.translate(drawContext, f10, f11);
            }

            if (f12 != 0.0F) {
                MatrixUtil.rotate(drawContext, f8, f9, f12);
            }

            MatrixUtil.scale(drawContext, f8, f9, 1.0F + 0.2F * f5);
            new RenderUtil(drawContext, f, f2, f3, f3).icon(icon2, 0.0F, 0.0F, f3, i);
            MatrixUtil.pop(drawContext);
        }
    }

    private static float getSpinSpeed(Icon icon) {
        switch (icon) {
            case SETTINGS:
            case REFRESH:
                return 0.13F;
            case DICE:
                return 0.22F;
            default:
                return 0.0F;
        }
    }
}
