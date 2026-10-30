package tech.sadnes.client.ui.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.impl.render.Crosshair;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class CrosshairEditorScreen extends Screen implements IMinecraft {
    private static final int GRID_SIZE = 15;
    private static final float PADDING = 10.0F;
    private static final float BUTTON_HEIGHT = 16.0F;
    private static final float BUTTON_GAP = 6.0F;
    private final Crosshair crosshair;
    private final boolean[] pixels;
    private final float[] pixelAnimations = new float[225];
    private boolean drawing;
    private boolean drawValue;
    private boolean mirrorX;
    private boolean mirrorY;
    private boolean clearing;
    private float clearRadius;
    private long lastFrameNanos;

    public CrosshairEditorScreen(Crosshair crosshair2) {
        super(Text.literal("Crosshair Editor"));
        this.crosshair = crosshair2;
        this.pixels = (boolean[])crosshair2.getPixels().clone();

        for (int i = 0; i < this.pixelAnimations.length; i++) {
            this.pixelAnimations[i] = this.pixels[i] ? 1.0F : 0.0F;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        new RenderUtil(drawContext, 0.0F, 0.0F, this.width, this.height).rect(ColorUtil.rgba(0, 0, 0, 150));
        this.updateAnimations();
        float f2 = this.getCellSize();
        float f3 = this.getGridX(f2);
        float f4 = this.getGridY(f2);
        float f5 = 15.0F * f2;
        new RenderUtil(drawContext, f3 - 10.0F, f4 - 10.0F, f5 + 20.0F, f5 + 20.0F + 16.0F + 10.0F)
            .rect(ColorUtil.multiplyAlpha(Theme.getSurfaceDark(), 0.95F), Radius.all(8.0F))
            .shadow(1.0F, ColorUtil.multiplyAlpha(Theme.getMutedText(), 0.6F), Radius.all(8.0F));
        int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), 0.06F);
        int i4 = ColorUtil.multiplyAlpha(Theme.getTextColor(), 0.03F);
        int i5 = Theme.getPrimary();

        for (int i6 = 0; i6 < 15; i6++) {
            for (int i7 = 0; i7 < 15; i7++) {
                float f6 = f3 + i7 * f2;
                float f7 = f4 + i6 * f2;
                int i8 = (i7 + i6 & 1) == 0 ? i3 : i4;
                new RenderUtil(drawContext, f6 + 0.5F, f7 + 0.5F, f2 - 1.0F, f2 - 1.0F).rect(i8);
                float f8 = this.pixelAnimations[i6 * 15 + i7];
                if (!(f8 <= 0.002F)) {
                    float f9 = Math.min(f8, 1.0F);
                    float f10 = Math.max(0.0F, (float)Easings.BACK_OUT.ease(f9));
                    float f11 = f2 * f10;
                    float f12 = (f2 - f11) / 2.0F;
                    new RenderUtil(drawContext, f6 + f12, f7 + f12, f11, f11).rect(ColorUtil.multiplyAlpha(i5, f9));
                }
            }
        }

        int i9 = this.getCellAt(i, i2, f2, f3, f4);
        if (i9 >= 0) {
            float f13 = f3 + i9 % 15 * f2;
            float f14 = f4 + i9 / 15 * f2;
            new RenderUtil(drawContext, f13, f14, f2, f2).shadow(1.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), 0.8F), Radius.zero());
        }

        float[] floats = this.getButtonLayout(f2);
        float f15 = floats[2];
        float f16 = floats[1];
        this.renderButton(drawContext, floats[0], f16, f15, i, i2, Language.translate("crosshair.editor.mirrorX", "Зеркало X"), this.mirrorX);
        this.renderButton(drawContext, floats[0] + f15 + 6.0F, f16, f15, i, i2, Language.translate("crosshair.editor.mirrorY", "Зеркало Y"), this.mirrorY);
        this.renderButton(drawContext, floats[0] + (f15 + 6.0F) * 2.0F, f16, f15, i, i2, Language.translate("crosshair.editor.clear", "Очистить"), false);
        new RenderUtil(drawContext, f3, f4 - 10.0F - 10.0F, f5, 8.0F)
            .textCenter(Fonts.INTER_MEDIUM, Language.translate("crosshair.editor.hint", "ЛКМ — рисовать, ПКМ — стирать"), Theme.getSecondaryText(), 6.5F);
    }

    private void updateAnimations() {
        long l = System.nanoTime();
        float f = this.lastFrameNanos == 0L ? 0.016666668F : Math.min((float)(l - this.lastFrameNanos) / 1.0E9F, 0.05F);
        this.lastFrameNanos = l;
        if (this.clearing) {
            this.clearRadius += f * 26.0F;

            for (int i = 0; i < 15; i++) {
                for (int i2 = 0; i2 < 15; i2++) {
                    if (Math.hypot(i2 - 7.0F, i - 7.0F) <= this.clearRadius) {
                        this.pixels[i * 15 + i2] = false;
                    }
                }
            }

            if (this.clearRadius > 22.5F) {
                this.clearing = false;
            }
        }

        float f2 = 1.0F - (float)Math.exp(-f * 16.0F);

        for (int i3 = 0; i3 < this.pixelAnimations.length; i3++) {
            float f3 = this.pixels[i3] ? 1.0F : 0.0F;
            this.pixelAnimations[i3] = this.pixelAnimations[i3] + (f3 - this.pixelAnimations[i3]) * f2;
            if (Math.abs(this.pixelAnimations[i3] - f3) < 0.001F) {
                this.pixelAnimations[i3] = f3;
            }
        }
    }

    private void renderButton(DrawContext drawContext, float f, float f2, float f3, int i, int i2, String string, boolean flag) {
        boolean flag2 = i >= f && i <= f + f3 && i2 >= f2 && i2 <= f2 + 16.0F;
        int i3 = ColorUtil.multiplyAlpha(flag ? Theme.getPrimary() : Theme.getSurfaceDarkest(), flag2 ? 1.0F : 0.9F);
        new RenderUtil(drawContext, f, f2, f3, 16.0F).rect(i3, Radius.all(4.0F)).textCenter(Fonts.INTER_MEDIUM, string, Theme.getTextColor(), 6.5F);
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        double d = click.x();
        double d2 = click.y();
        float f = this.getCellSize();
        float[] floats = this.getButtonLayout(f);
        float f2 = floats[2];
        float f3 = floats[1];
        if (this.isButtonHovered(d, d2, floats[0], f3, f2)) {
            if (!this.mirrorX) {
                this.mirrorX = true;
                return true;
            } else {
                this.mirrorX = false;
                return true;
            }
        } else if (this.isButtonHovered(d, d2, floats[0] + f2 + 6.0F, f3, f2)) {
            if (!this.mirrorY) {
                this.mirrorY = true;
                return true;
            } else {
                this.mirrorY = false;
                return true;
            }
        } else if (this.isButtonHovered(d, d2, floats[0] + (f2 + 6.0F) * 2.0F, f3, f2)) {
            this.startClear();
            return true;
        } else {
            int i = this.getCellAt(d, d2, f, this.getGridX(f), this.getGridY(f));
            if (i < 0) {
                return super.mouseClicked(click, flag);
            } else {
                this.drawing = true;
                if (click.button() == 0) {
                    this.drawValue = true;
                    this.paintCell(i, this.drawValue);
                    return true;
                } else {
                    this.drawValue = false;
                    this.paintCell(i, this.drawValue);
                    return true;
                }
            }
        }
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (!this.drawing) {
            return super.mouseDragged(click, d, d2);
        }

        float f = this.getCellSize();
        int i = this.getCellAt(click.x(), click.y(), f, this.getGridX(f), this.getGridY(f));
        if (i >= 0) {
            this.paintCell(i, this.drawValue);
        }

        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        this.drawing = false;
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        this.crosshair.setPixels(this.pixels);
        if (Sadnes.getInstance().getConfigManager() != null) {
            Sadnes.getInstance().getConfigManager().saveActive();
        }

        mc.setScreen(Sadnes.getInstance().getClickGui());
    }

    private void startClear() {
        this.clearing = true;
        this.clearRadius = 0.0F;
    }

    private void paintCell(int i, boolean flag) {
        int i2 = i % 15;
        int i3 = i / 15;
        this.setPixel(i2, i3, flag);
        if (this.mirrorX) {
            this.setPixel(14 - i2, i3, flag);
        }

        if (this.mirrorY) {
            this.setPixel(i2, 14 - i3, flag);
        }

        if (this.mirrorX && this.mirrorY) {
            this.setPixel(14 - i2, 14 - i3, flag);
        }
    }

    private void setPixel(int i, int i2, boolean flag) {
        if (i >= 0 && i < 15 && i2 >= 0 && i2 < 15) {
            this.pixels[i2 * 15 + i] = flag;
        }
    }

    private boolean isButtonHovered(double d, double d2, float f, float f2, float f3) {
        return d >= f && d <= f + f3 && d2 >= f2 && d2 <= f2 + 16.0F;
    }

    private float[] getButtonLayout(float f) {
        float f2 = this.getGridX(f);
        float f3 = 15.0F * f;
        float f4 = this.getGridY(f) + f3 + 10.0F;
        float f5 = (f3 - 12.0F) / 3.0F;
        return new float[]{f2, f4, f5};
    }

    private int getCellAt(double d, double d2, float f, float f2, float f3) {
        if (!(d < f2) && !(d2 < f3)) {
            int i = (int)((d - f2) / f);
            int i2 = (int)((d2 - f3) / f);
            if (i >= 0 && i < 15 && i2 >= 0) {
                if (i2 >= 15) {
                    return -1;
                }

                return i2 * 15 + i;
            }
        }

        return -1;
    }

    private float getCellSize() {
        return Math.min(this.width, this.height) * 0.5F / 15.0F;
    }

    private float getGridX(float f) {
        return this.width / 2.0F - 15.0F * f / 2.0F;
    }

    private float getGridY(float f) {
        return this.height / 2.0F - 15.0F * f / 2.0F;
    }
}
