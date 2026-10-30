package tech.sadnes.client.render.font;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.VerticalFadeMask;
import tech.sadnes.client.util.color.ColorTagUtil;
import tech.sadnes.client.util.color.ColorUtil;

public final class MsdfFont {
    private static final float EPSILON = 1.0E-4F;
    private MsdfAtlasFont renderer;
    private final Map<String, MsdfFont.ScrollState> scrollStates = new HashMap<>();
    private long lastCleanup;

    public MsdfFont(String string) {
        this.renderer = MsdfAtlasFont.builder().atlas(string + ".png").data(string + ".json").build();
    }

    public MsdfFont(Path path, Path path2) {
        this.renderer = MsdfAtlasFont.builder().files(path, path2).build();
    }

    public void reload(String string) {
        if (this.renderer != null) {
            this.renderer.destroy();
        }

        this.renderer = MsdfAtlasFont.builder().atlas(string + ".png").data(string + ".json").build();
    }

    public void drawScrolling(
        DrawContext drawContext, String string, float f, float f2, float f3, int i, float f4, float f5, boolean flag, Matrix3x2fc matrix3x2fc
    ) {
        if (string != null && !string.isEmpty() && !(f3 <= 0.0F)) {
            float f6 = this.getWidth(string, f4);
            float f7 = f4 * 1.5F;
            float f8 = f2 - f4 / 4.0F;
            if (f6 <= f3) {
                this.scrollStates.remove(string);
                boolean flag2 = ScissorStack.push(drawContext, f - 1.0F, f8, f3 + 2.0F, f7, matrix3x2fc);
                if (flag2) {
                    try {
                        this.draw(drawContext, string, f, f2, i, f4, matrix3x2fc);
                    } finally {
                        ScissorStack.pop(drawContext);
                    }
                }
            } else {
                float f9 = f6 + 20.0F;
                MsdfFont.ScrollState scrollState = this.updateScroll(string, f9, flag);
                float f10 = scrollState.offset * f9;
                float f11 = f - f10;
                float f12 = Math.min(Math.max(f5, 0.0F), f3 / 2.0F);
                float f13 = f + f3 - f12;
                boolean flag3 = ScissorStack.push(drawContext, f, f8, Math.max(0.0F, f13 - f), f7, matrix3x2fc);
                if (flag3) {
                    try {
                        this.draw(drawContext, string, f11, f2, i, f4, matrix3x2fc);
                        this.draw(drawContext, string, f11 + f9, f2, i, f4, matrix3x2fc);
                    } finally {
                        ScissorStack.pop(drawContext);
                    }
                }

                MsdfFont.DrawCallback drawCallback = f14 -> {
                    this.drawWithAlpha(drawContext, string, f11, f2, i, f4, f14, matrix3x2fc);
                    this.drawWithAlpha(drawContext, string, f11 + f9, f2, i, f4, f14, matrix3x2fc);
                };
                if (f12 > 0.0F) {
                    this.drawFadeEdge(drawContext, f13, f12, f8, f7, matrix3x2fc, true, drawCallback);
                }
            }
        }
    }

    private void drawFadeEdge(DrawContext drawContext, float f, float f2, float f3, float f4, Matrix3x2fc matrix3x2fc, boolean flag, MsdfFont.DrawCallback drawCallback) {
        if (!this.isAxisAligned(matrix3x2fc)) {
            boolean flag2 = ScissorStack.push(drawContext, f, f3, f2, f4, matrix3x2fc);
            if (flag2) {
                try {
                    drawCallback.draw(1.0F);
                } finally {
                    ScissorStack.pop(drawContext);
                }
            }
        } else {
            int i = (int)Math.ceil(this.transformX(matrix3x2fc, f, f3));
            int i2 = (int)Math.ceil(this.transformX(matrix3x2fc, f + f2, f3));
            int i3 = (int)Math.floor(this.transformY(matrix3x2fc, f, f3));
            int i4 = (int)Math.ceil(this.transformY(matrix3x2fc, f, f3 + f4));
            int i5 = Math.max(1, i2 - i);

            for (int i6 = 0; i6 < i5; i6++) {
                float f5 = i5 <= 1 ? 1.0F : (float)i6 / (i5 - 1);
                if (this.pushScissor(drawContext, i + i6, i3, 1, i4 - i3)) {
                    try {
                        drawCallback.draw(flag ? 1.0F - f5 : f5);
                    } finally {
                        this.popScissor(drawContext);
                    }
                }
            }
        }
    }

    private MsdfFont.ScrollState updateScroll(String string, float f, boolean flag) {
        long l = System.nanoTime();
        MsdfFont.ScrollState scrollState = this.scrollStates.computeIfAbsent(string, string2 -> new MsdfFont.ScrollState());
        float f2 = scrollState.lastNanos == 0L ? 0.0F : Math.min(0.05F, (float)(l - scrollState.lastNanos) / 1.0E9F);
        scrollState.lastNanos = l;
        scrollState.lastAccess = System.currentTimeMillis();
        float f3 = f2 * 20.0F / f;
        if (flag) {
            scrollState.scrolling = true;
            scrollState.direction = 0;
            scrollState.offset += f3;
            if (scrollState.offset >= 1.0F) {
                scrollState.offset--;
            }
        } else if (scrollState.scrolling) {
            if (scrollState.direction == 0) {
                scrollState.direction = scrollState.offset < 0.5F ? -1 : 1;
            }

            if (scrollState.direction > 0) {
                scrollState.offset += f3;
                if (scrollState.offset >= 1.0F) {
                    scrollState.offset = 0.0F;
                    scrollState.scrolling = false;
                    scrollState.direction = 0;
                }
            } else {
                scrollState.offset -= f3 * 2.5F;
                if (scrollState.offset <= 0.0F) {
                    scrollState.offset = 0.0F;
                    scrollState.scrolling = false;
                    scrollState.direction = 0;
                }
            }
        }

        scrollState.fade = scrollState.fade + ((scrollState.scrolling ? 1.0F : 0.0F) - scrollState.fade) * Math.min(1.0F, f2 * 8.0F);
        this.cleanupScrollStates(scrollState.lastAccess);
        return scrollState;
    }

    private void cleanupScrollStates(long l) {
        if (this.scrollStates.size() > 16 && l - this.lastCleanup >= 2000L) {
            this.lastCleanup = l;
            this.scrollStates.entrySet().removeIf(entry -> l - entry.getValue().lastAccess > 4000L);
        }
    }

    private boolean isAxisAligned(Matrix3x2fc matrix3x2fc) {
        return matrix3x2fc != null && Math.abs(matrix3x2fc.m01()) <= 1.0E-4F && Math.abs(matrix3x2fc.m10()) <= 1.0E-4F && matrix3x2fc.m00() > 1.0E-4F && matrix3x2fc.m11() > 1.0E-4F;
    }

    private boolean pushScissor(DrawContext drawContext, int i, int i2, int i3, int i4) {
        if (drawContext != null && i3 > 0 && i4 > 0) {
            ScreenRect screenRect = new ScreenRect(i, i2, i3, i4);
            ScreenRect screenRect2 = drawContext.scissorStack.peekLast();
            ScreenRect screenRect3 = screenRect2 == null ? screenRect : screenRect.intersection(screenRect2);
            if (screenRect3 != null && screenRect3.width() > 0) {
                if (screenRect3.height() <= 0) {
                    return false;
                }

                drawContext.scissorStack.push(screenRect3);
                return true;
            }
        }

        return false;
    }

    private void popScissor(DrawContext drawContext) {
        if (drawContext != null && drawContext.scissorStack.peekLast() != null) {
            drawContext.disableScissor();
        }
    }

    private float transformX(Matrix3x2fc matrix3x2fc, float f, float f2) {
        return matrix3x2fc.m00() * f + matrix3x2fc.m10() * f2 + matrix3x2fc.m20();
    }

    private float transformY(Matrix3x2fc matrix3x2fc, float f, float f2) {
        return matrix3x2fc.m01() * f + matrix3x2fc.m11() * f2 + matrix3x2fc.m21();
    }

    public void draw(DrawContext drawContext, String string, float f, float f2, int i, float f3) {
        this.draw(drawContext, string, f, f2, i, f3, drawContext.getMatrices());
    }

    public void draw(DrawContext drawContext, String string, float f, float f2, int i, float f3, Matrix3x2fc matrix3x2fc) {
        this.drawInternal(drawContext, string, f, f2, i, f3, 1.0F, matrix3x2fc, true);
    }

    public void draw(DrawContext drawContext, String string, float f, float f2, int i, float f3, Matrix3x2fc matrix3x2fc, boolean flag) {
        this.drawInternal(drawContext, string, f, f2, i, f3, 1.0F, matrix3x2fc, flag);
    }

    private void drawWithAlpha(DrawContext drawContext, String string, float f, float f2, int i, float f3, float f4, Matrix3x2fc matrix3x2fc) {
        this.drawInternal(drawContext, string, f, f2, i, f3, f4, matrix3x2fc, true);
    }

    private void drawInternal(DrawContext drawContext, String string, float f, float f2, int i, float f3, float f4, Matrix3x2fc matrix3x2fc, boolean flag) {
        if (drawContext != null && string != null && !string.isEmpty()) {
            if (VerticalFadeMask.isActive()) {
                f4 *= VerticalFadeMask.getAlphaFactor(f2 + f3 * 0.5F);
            }

            Matrix3x2fc matrix3x2fc2 = matrix3x2fc != null ? matrix3x2fc : new Matrix3x2f(drawContext.getMatrices());
            this.renderer.drawText(drawContext, f3, string, 0.0F, f, f2 + this.renderer.getMetrics().getBaselineHeight() * f3, 0.0F, i, f4, matrix3x2fc2, flag);
        }
    }

    public void close() {
        this.renderer.destroy();
    }

    public float getWidth(String string, float f) {
        return this.renderer.getWidth(string, f);
    }

    public float getWidth(String string, float f, float f2) {
        return this.renderer.getWidth(string, f, f2);
    }

    public float getTextWidth(Text text2, float f) {
        float[] floats = new float[]{0.0F};
        text2.visit((style, string) -> {
            if (!string.isEmpty()) {
                floats[0] += this.renderer.getWidth(string, f);
            }

            return Optional.empty();
        }, Style.EMPTY);
        return floats[0];
    }

    public String toColoredString(Text text2, int i) {
        StringBuilder builder = new StringBuilder();
        text2.visit((style, string) -> {
            if (string.isEmpty()) {
                return Optional.empty();
            }

            TextColor textColor = style.getColor();
            int i2 = textColor != null ? ColorUtil.setAlphaf(textColor.getRgb() | 0xFF000000, ColorUtil.alphaf(i)) : i;
            ColorTagUtil.appendColorTag(builder, i2).append(string);
            return Optional.empty();
        }, Style.EMPTY);
        return builder.toString();
    }

    public float getHeight(float f) {
        return this.renderer.getLineHeight(f);
    }

    @FunctionalInterface
    interface DrawCallback {
        void draw(float f);
    }

    static final class ScrollState {
        float offset;
        boolean scrolling;
        int direction;
        float fade;
        long lastNanos;
        long lastAccess;
    }
}
