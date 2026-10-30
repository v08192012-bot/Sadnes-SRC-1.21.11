package tech.sadnes.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Easing;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.shader.GlStateSnapshot;
import tech.sadnes.client.render.shader.ShapeBlur;
import tech.sadnes.client.render.texture.AnimatedIcon;
import tech.sadnes.client.render.texture.AnimatedIconManager;
import tech.sadnes.client.render.texture.EasingLutTexture;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.LogoTexture;
import tech.sadnes.client.render.texture.SvgIconCache;
import tech.sadnes.client.ui.clickgui.component.SliderDrop;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.math.MathUtil;

public final class RenderUtil {
    private final DrawContext context;
    private float x;
    private float y;
    private float width;
    private float height;

    public RenderUtil(DrawContext drawContext, float f, float f2, float f3, float f4) {
        this.context = drawContext;
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
    }

    public RenderUtil snapped() {
        return new RenderUtil(
            this.context,
            MathUtil.roundToStep(this.x, 0.5F),
            MathUtil.roundToStep(this.y, 0.5F),
            MathUtil.roundToStep(this.width, 0.5F),
            MathUtil.roundToStep(this.height, 0.5F)
        );
    }

    public DrawContext getContext() {
        return this.context;
    }

    public float getX() {
        return this.x;
    }

    public RenderUtil setX(float f) {
        this.x = f;
        return this;
    }

    public float getY() {
        return this.y;
    }

    public RenderUtil setY(float f) {
        this.y = f;
        return this;
    }

    public float getWidth() {
        return this.width;
    }

    public RenderUtil setWidth(float f) {
        this.width = f;
        return this;
    }

    public float getHeight() {
        return this.height;
    }

    public RenderUtil setHeight(float f) {
        this.height = f;
        return this;
    }

    public RenderUtil setPosition(float f, float f2) {
        this.x = f;
        this.y = f2;
        return this;
    }

    public RenderUtil translate(float f, float f2) {
        this.x += f;
        this.y += f2;
        return this;
    }

    public RenderUtil translateBack(float f, float f2) {
        this.x -= f;
        this.y -= f2;
        return this;
    }

    public RenderUtil expand(Side side, float f) {
        side.expand(this, f);
        return this;
    }

    public RenderUtil rect(float f, int i, int i2, int i3, int i4, float f2, Radius radius) {
        return this.rectInternal(f, f2, 0.0F, 0.0F, i, i2, i3, i4, radius, 0);
    }

    public RenderUtil rect(float f, int i, int i2, int i3, int i4, Radius radius) {
        return this.rect(f, i, i2, i3, i4, 1.0F, radius);
    }

    public RenderUtil rect(int i, int i2, int i3, int i4, float f, Radius radius) {
        return this.rect(0.0F, i, i2, i3, i4, f, radius);
    }

    public RenderUtil rect(int i, int i2, int i3, int i4, Radius radius) {
        return this.rect(0.0F, i, i2, i3, i4, 1.0F, radius);
    }

    public RenderUtil rect(int i, float f, Radius radius) {
        return this.rect(0.0F, i, i, i, i, f, radius);
    }

    public RenderUtil rect(int i, Radius radius) {
        return this.rect(0.0F, i, i, i, i, 1.0F, radius);
    }

    public RenderUtil rect(int i) {
        return this.rect(0.0F, i, i, i, i, 1.0F, Radius.zero());
    }

    public RenderUtil rectGradientX(int i, int i2) {
        return this.rect(0.0F, i2, i, i2, i, 1.0F, Radius.zero());
    }

    public RenderUtil rectGradientY(int i, int i2) {
        return this.rect(0.0F, i2, i2, i, i, 1.0F, Radius.zero());
    }

    public RenderUtil shadow(float f, float f2, int i, int i2, int i3, int i4, float f3, Radius radius) {
        return this.rectInternal(f, f3, 0.0F, f2, i, i2, i3, i4, radius, 4);
    }

    public RenderUtil shadow(float f, float f2, int i, int i2, int i3, int i4, Radius radius) {
        return this.shadow(f, f2, i, i2, i3, i4, 1.0F, radius);
    }

    public RenderUtil shadow(float f, int i, int i2, int i3, int i4, float f2, Radius radius) {
        return this.shadow(f, f, i, i2, i3, i4, f2, radius);
    }

    public RenderUtil shadow(float f, int i, int i2, int i3, int i4, Radius radius) {
        return this.shadow(f, f, i, i2, i3, i4, 1.0F, radius);
    }

    public RenderUtil shadow(float f, int i, Radius radius) {
        return this.shadow(f, f, i, i, i, i, 1.0F, radius);
    }

    public RenderUtil shadow(float f, int i, float f2, Radius radius) {
        return this.shadow(f, f, i, i, i, i, f2, radius);
    }

    public RenderUtil glow(int i, float f, Radius radius) {
        this.drawRectShape(f, 0.0F, Math.max(this.width, this.height), i, i, i, i, radius, 4);
        return this;
    }

    public RenderUtil glow(int i, Radius radius) {
        return this.glow(i, 1.0F, radius);
    }

    public RenderUtil glow(int i) {
        return this.glow(i, 1.0F, Radius.zero());
    }

    public RenderUtil arc(float f, float f2, float f3, int i, float f4, float f5, float f6) {
        ArcRenderer.drawArc(this.context, this.context.getMatrices(), this.x, this.y, this.width, this.height, f, f2, f3, f4, i, f5, f6);
        return this;
    }

    public RenderUtil arc(float f, float f2, float f3, int i, float f4) {
        return this.arc(f, f2, f3, i, f4, 0.0F, 0.0F);
    }

    public RenderUtil arc(float f, float f2, float f3, int i) {
        return this.arc(f, f2, f3, i, 1.0F, 0.0F, 0.0F);
    }

    public RenderUtil arcCapped(float f, float f2, float f3, int i, float f4) {
        ArcRenderer.drawArc(this.context, this.context.getMatrices(), this.x, this.y, this.width, this.height, f, f2, f3, f4, i, 0.0F, 0.0F, true);
        return this;
    }

    public RenderUtil circle(float f, int i, float f2) {
        return this.arc(0.0F, 360.0F, f, i, f2, 0.0F, 0.0F);
    }

    public RenderUtil circle(float f, int i) {
        return this.arc(0.0F, 360.0F, f, i, 1.0F, 0.0F, 0.0F);
    }

    public RenderUtil slime(
        int i, float f, float f2, float f3, float f4, float f5, float f6, float f7, boolean flag, float f8, Radius radius
    ) {
        SlimeShapeRenderer.Style style = flag ? SlimeShapeRenderer.Style.OVERLAY : SlimeShapeRenderer.Style.SLIME;
        return this.fluidEffect(style, i, f, f2, f3, f4, f5, f6, f7, f8, radius);
    }

    public RenderUtil water(int i, float f, float f2, float f3, float f4, float f5, Radius radius) {
        return this.fluidEffect(SlimeShapeRenderer.Style.WATER, i, f, f2, f3, f4, 0.0F, 0.5F, 0.0F, f5, radius);
    }

    private RenderUtil fluidEffect(
        SlimeShapeRenderer.Style style, int i, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, Radius radius
    ) {
        TextureSetup textureSetup = LogoTexture.getTextureSetup();
        if (textureSetup == null) {
            return this;
        }

        SlimeShapeRenderer.drawSlime(
            this.context,
            textureSetup,
            this.context.getMatrices(),
            this.x,
            this.y,
            this.width,
            this.height,
            radius,
            style,
            i,
            f,
            f2,
            f3,
            f4,
            f5,
            f6,
            f7,
            f8
        );
        return this;
    }

    public RenderUtil animatedEffect(int i, float f, SliderDrop sliderDrop, float f2) {
        LiquidSliderRenderer.draw(this.context, this.context.getMatrices(), this.x, this.y, this.width, this.height, i, f, sliderDrop, f2);
        return this;
    }

    public RenderUtil progressRing(float f, float f2, int i, float f3, float f4, float f5) {
        Arrow3dRenderer.drawArrow(this.context, this.context.getMatrices(), this.x, this.y, this.width, this.height, f, f2, i, f3, f4, f5);
        return this;
    }

    public RenderUtil progressRing(float f, float f2, int i, float f3) {
        return this.progressRing(f, f2, i, f3, 0.0F, 0.0F);
    }

    public RenderUtil outline(float f, int i, int i2, int i3, int i4, float f2, Radius radius) {
        return this.rectInternal(f, f2, f, 0.0F, i, i2, i3, i4, radius, 1);
    }

    public RenderUtil outline(float f, int i, int i2, int i3, int i4, Radius radius) {
        return this.outline(f, i, i2, i3, i4, 1.0F, radius);
    }

    public RenderUtil outline(float f, int i, int i2, int i3, int i4, float f2) {
        return this.outline(f, i, i2, i3, i4, f2, Radius.all(f));
    }

    public RenderUtil outline(float f, int i, int i2, int i3, int i4) {
        return this.outline(f, i, i2, i3, i4, 1.0F, Radius.all(f));
    }

    public RenderUtil outline(float f, int i, float f2, Radius radius) {
        return this.outline(f, i, i, i, i, f2, radius);
    }

    public RenderUtil outline(float f, int i) {
        return this.outline(f, i, i, i, i, 1.0F, Radius.all(f));
    }

    public RenderUtil outline(float f, int i, Radius radius) {
        return this.outline(f, i, i, i, i, 1.0F, radius);
    }

    public RenderUtil outline(float f, int i, float f2) {
        return this.outline(f, i, i, i, i, f2, Radius.all(f));
    }

    public RenderUtil border(float f, int i, int i2, int i3, int i4, float f2, Radius radius) {
        return this.rectInternal(f, f2, f, 0.0F, i, i2, i3, i4, radius, 3);
    }

    public RenderUtil border(float f, int i, int i2, int i3, int i4, Radius radius) {
        return this.border(f, i, i2, i3, i4, 1.0F, radius);
    }

    public RenderUtil border(float f, int i, int i2, int i3, int i4, float f2) {
        return this.border(f, i, i2, i3, i4, f2, Radius.all(f));
    }

    public RenderUtil border(float f, int i, int i2, int i3, int i4) {
        return this.border(f, i, i2, i3, i4, 1.0F, Radius.all(f));
    }

    public RenderUtil border(float f, int i, float f2, Radius radius) {
        return this.border(f, i, i, i, i, f2, radius);
    }

    public RenderUtil border(float f, int i) {
        return this.border(f, i, i, i, i, 1.0F, Radius.all(f));
    }

    public RenderUtil border(float f, int i, Radius radius) {
        return this.border(f, i, i, i, i, 1.0F, radius);
    }

    public RenderUtil border(float f, int i, float f2) {
        return this.border(f, i, i, i, i, f2, Radius.all(f));
    }

    public RenderUtil frostedGlass(float f, float f2, float f3, float f4, float f5, Radius radius) {
        TextureSetup textureSetup = GuiRenderUtil.getBoundTexture();
        if (textureSetup != null) {
            this.texturedInternal(f, ColorUtil.WHITE, f5, f2, f3, f4, radius, textureSetup, 2, false);
        }

        return this;
    }

    public RenderUtil frostedGlass(float f, float f2, float f3, Radius radius) {
        return this.frostedGlass(0.0F, 0.0F, f, f2, f3, radius);
    }

    public RenderUtil frostedGlass(float f, Radius radius) {
        return this.frostedGlass(0.0F, 0.0F, -1.0F, 1.0F, f, radius);
    }

    public RenderUtil frostedGlass(Radius radius) {
        return this.frostedGlass(0.0F, 0.0F, -0.5F, 0.5F, 1.0F, radius);
    }

    public RenderUtil frostedGlass() {
        return this.frostedGlass(0.0F, 0.0F, -0.5F, 0.5F, 1.0F, Radius.zero());
    }

    public RenderUtil tintedGlass(int i, float f, Radius radius) {
        TextureSetup textureSetup = GuiRenderUtil.getBoundTexture();
        if (textureSetup != null) {
            this.texturedInternal(0.0F, i, f, 0.0F, -1.0F, 1.0F, radius, textureSetup, 2, false);
        }

        return this;
    }

    public RenderUtil glassGradient(float f, float f2, float f3, Radius radius) {
        TextureSetup textureSetup = GuiRenderUtil.getBoundTexture();
        if (textureSetup != null) {
            int i = ColorUtil.setAlpha(ColorUtil.WHITE, Math.max(0, Math.min(255, Math.round(f * 255.0F))));
            int i2 = ColorUtil.setAlpha(ColorUtil.WHITE, Math.max(0, Math.min(255, Math.round(f2 * 255.0F))));
            TexturedShapeRenderer.drawTextureRegion(
                this.context,
                textureSetup,
                this.context.getMatrices(),
                this.x,
                this.y,
                this.width,
                this.height,
                0.0F,
                0.0F,
                1.0F,
                1.0F,
                radius,
                0.0F,
                -1.0F,
                1.0F,
                f3,
                i,
                i2,
                2,
                false
            );
        }

        return this;
    }

    private RenderUtil blurInternal(
        float f, int i, float f2, float f3, float f4, float f5, Radius radius, RenderUtil.BlurQuality blurQuality, boolean flag
    ) {
        TextureSetup textureSetup = this.getBlurTexture(f, radius, blurQuality, flag);
        if (textureSetup != null) {
            this.texturedInternal(f, i, f5, f2, f3, f4, radius, textureSetup, 3, flag);
        }

        return this;
    }

    private TextureSetup getBlurTexture(float f, Radius radius, RenderUtil.BlurQuality blurQuality, boolean flag) {
        float f2 = 0.0F;
        float f3 = 0.0F;
        float f4 = 0.0F;
        float f5 = 0.0F;
        float f6 = 0.0F;
        if (flag) {
            float f7 = this.x - f;
            float f8 = this.y - f;
            float f9 = this.width + f * 2.0F;
            float f10 = this.height + f * 2.0F;
            Matrix3x2fStack matrix3x2fStack = this.context.getMatrices();
            Vector2f vector2f = matrix3x2fStack.transformPosition(f7, f8, new Vector2f());
            Vector2f vector2f2 = matrix3x2fStack.transformPosition(f7 + f9, f8 + f10, new Vector2f());
            float f11 = Math.min(vector2f.x, vector2f2.x);
            float f12 = Math.min(vector2f.y, vector2f2.y);
            float f13 = Math.max(vector2f.x, vector2f2.x);
            float f14 = Math.max(vector2f.y, vector2f2.y);
            Window window = MinecraftClient.getInstance().getWindow();
            float f15 = window.getScaleFactor();
            float f16 = f15 <= 0.0F ? window.getScaledWidth() : window.getFramebufferWidth() / f15;
            float f17 = f15 <= 0.0F ? window.getScaledHeight() : window.getFramebufferHeight() / f15;
            f2 = f11 / f16;
            f4 = f13 / f16;
            if (GlStateSnapshot.isOpenGl()) {
                f3 = 1.0F - f14 / f17;
                f5 = 1.0F - f12 / f17;
            } else {
                f3 = f12 / f17;
                f5 = f14 / f17;
            }

            float f18 = Math.max(Math.max(radius.getTopRight(), radius.getBottomLeft()), Math.max(radius.getTopLeft(), radius.getBottomRight()));
            f6 = f18 * f15 * matrix3x2fStack.m00();
        }

        return ShapeBlur.INSTANCE.getBlurTexture(blurQuality.ordinal(), flag, f2, f3, f4, f5, f6);
    }

    public RenderUtil blur(float f, float f2, float f3, float f4, float f5, Radius radius, RenderUtil.BlurQuality blurQuality, boolean flag) {
        return this.blurInternal(f, ColorUtil.WHITE, f2, f3, f4, f5, radius, blurQuality, flag);
    }

    public RenderUtil blur(float f, float f2, float f3, Radius radius, RenderUtil.BlurQuality blurQuality, boolean flag) {
        return this.blur(0.0F, 0.0F, f, f2, f3, radius, blurQuality, flag);
    }

    public RenderUtil blur(float f, Radius radius, RenderUtil.BlurQuality blurQuality, boolean flag) {
        return this.blur(0.0F, 0.0F, -0.5F, 0.5F, f, radius, blurQuality, flag);
    }

    public RenderUtil blur(float f, Radius radius, RenderUtil.BlurQuality blurQuality) {
        return this.blur(0.0F, 0.0F, -0.5F, 0.5F, f, radius, blurQuality, false);
    }

    public RenderUtil blur(float f, Radius radius, boolean flag) {
        return this.blur(0.0F, 0.0F, -0.5F, 0.5F, f, radius, RenderUtil.BlurQuality.MIDDLE, flag);
    }

    public RenderUtil blur(float f, Radius radius) {
        return this.blur(0.0F, 0.0F, -0.5F, 0.5F, f, radius, RenderUtil.BlurQuality.MIDDLE, false);
    }

    public RenderUtil blur(Radius radius, RenderUtil.BlurQuality blurQuality, boolean flag) {
        return this.blur(0.0F, 0.0F, -0.5F, 0.5F, 1.0F, radius, blurQuality, flag);
    }

    public RenderUtil blur(Radius radius, RenderUtil.BlurQuality blurQuality) {
        return this.blur(0.0F, 0.0F, -0.5F, 0.5F, 1.0F, radius, blurQuality, false);
    }

    public RenderUtil blur(Radius radius, boolean flag) {
        return this.blur(0.0F, 0.0F, -0.5F, 0.5F, 1.0F, radius, RenderUtil.BlurQuality.MIDDLE, flag);
    }

    public RenderUtil blur(Radius radius) {
        return this.blur(0.0F, 0.0F, -0.5F, 0.5F, 1.0F, radius, RenderUtil.BlurQuality.MIDDLE, false);
    }

    public RenderUtil blur(boolean flag) {
        return this.blur(0.0F, 0.0F, -0.5F, 0.5F, 1.0F, Radius.zero(), RenderUtil.BlurQuality.MIDDLE, flag);
    }

    public RenderUtil blur() {
        return this.blur(0.0F, 0.0F, -0.5F, 0.5F, 1.0F, Radius.zero(), RenderUtil.BlurQuality.MIDDLE, false);
    }

    public RenderUtil tintedBlur(float f, int i, float f2, Radius radius, RenderUtil.BlurQuality blurQuality, boolean flag) {
        return this.blurInternal(0.0F, i, f2, -0.5F, 0.5F, f, radius, blurQuality, flag);
    }

    public RenderUtil glowBorder(float f, int i, float f2, Easing easing, Radius radius) {
        if (!(f <= 0.0F) && !(f2 <= 5.0E-4F)) {
            TextureSetup textureSetup = EasingLutTexture.getTextureSetup(easing);
            if (textureSetup == null) {
                return this;
            }

            float f3 = this.x;
            float f4 = this.y;
            float f5 = this.width;
            float f6 = this.height;
            this.expand(Side.ALL, f);
            GlassShadowRenderer.draw(
                this.context,
                textureSetup,
                this.context.getMatrices(),
                this.x + 0.175F,
                this.y + 0.175F,
                this.width - 0.25F,
                this.height - 0.25F,
                radius,
                f,
                f2,
                i
            );
            this.x = f3;
            this.y = f4;
            this.width = f5;
            this.height = f6;
            return this;
        } else {
            return this;
        }
    }

    public RenderUtil blurGlowBorder(
        float f, int i, float f2, int i2, float f3, float f4, Easing easing, Radius radius, RenderUtil.BlurQuality blurQuality, boolean flag
    ) {
        if (!(f3 <= 0.0F) && !(f4 <= 5.0E-4F)) {
            TextureSetup textureSetup = this.getBlurTexture(0.0F, radius, blurQuality, flag);
            TextureSetup textureSetup2 = EasingLutTexture.getTextureSetup(easing);
            if (textureSetup != null && textureSetup.texure0() != null && textureSetup2 != null) {
                TextureSetup textureSetup3 = TextureSetup.of(textureSetup.texure0(), textureSetup.sampler0(), textureSetup2.texure0(), textureSetup2.sampler0());
                float f5 = ColorUtil.alpha(i2) / 255.0F * f4;
                float f6 = this.x;
                float f7 = this.y;
                float f8 = this.width;
                float f9 = this.height;
                this.expand(Side.ALL, f3);
                GlassRenderer.draw(
                    this.context, textureSetup3, this.context.getMatrices(), this.x, this.y, this.width, this.height, radius, f3, i, f, f2, i2, f5, flag
                );
                this.x = f6;
                this.y = f7;
                this.width = f8;
                this.height = f9;
                return this;
            } else {
                return this;
            }
        } else {
            return this.tintedBlur(f, i, f2, radius, blurQuality, flag);
        }
    }

    private RenderUtil textAligned(
        RenderUtil.HorizontalAlign horizontalAlign, RenderUtil.VerticalAlign verticalAlign, MsdfFont msdfFont, String string, float f, float f2, int i, float f3, boolean flag
    ) {
        if (string != null && !string.isEmpty()) {
            float f4 = switch (verticalAlign) {
                case TOP -> this.y + f2;
                case MIDDLE -> this.y + this.height / 2.0F - f3 / 2.0F + f2;
                case BOTTOM -> this.y + this.height - f2 - f3;
            };
            Matrix3x2fStack matrix3x2fStack = this.context.getMatrices();
            switch (horizontalAlign) {
                case LEFT:
                    drawTextLeft(this.context, msdfFont, string, this.x + f, f4, f3, i, flag, matrix3x2fStack);
                    return this;
                case CENTER:
                    drawTextCentered(this.context, msdfFont, string, this.x + this.width / 2.0F + f, f4, f3, i, flag, matrix3x2fStack);
                    return this;
                case RIGHT:
                    drawTextRight(this.context, msdfFont, string, this.x + this.width - f, f4, f3, i, flag, matrix3x2fStack);
                    return this;
            }
        }

        return this;
    }

    public RenderUtil textTopLeft(MsdfFont msdfFont, String string, float f, float f2, int i, float f3, boolean flag) {
        return this.textAligned(RenderUtil.HorizontalAlign.LEFT, RenderUtil.VerticalAlign.TOP, msdfFont, string, f, f2, i, f3, flag);
    }

    public RenderUtil textTopLeft(MsdfFont msdfFont, String string, float f, float f2, int i, float f3) {
        return this.textAligned(RenderUtil.HorizontalAlign.LEFT, RenderUtil.VerticalAlign.TOP, msdfFont, string, f, f2, i, f3, false);
    }

    public RenderUtil textTopLeft(MsdfFont msdfFont, String string, int i, float f) {
        return this.textAligned(RenderUtil.HorizontalAlign.LEFT, RenderUtil.VerticalAlign.TOP, msdfFont, string, 0.0F, 0.0F, i, f, false);
    }

    public RenderUtil textTopLeft(MsdfFont msdfFont, Text text, float f, float f2, int i, float f3) {
        return text == null
            ? this
            : this.textAligned(
                RenderUtil.HorizontalAlign.LEFT, RenderUtil.VerticalAlign.TOP, msdfFont, msdfFont.toColoredString(text, i), f, f2, i, f3, false
            );
    }

    public RenderUtil textTopCenter(MsdfFont msdfFont, String string, float f, float f2, int i, float f3, boolean flag) {
        return this.textAligned(RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.TOP, msdfFont, string, f, f2, i, f3, flag);
    }

    public RenderUtil textTopCenter(MsdfFont msdfFont, String string, float f, float f2, int i, float f3) {
        return this.textAligned(RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.TOP, msdfFont, string, f, f2, i, f3, false);
    }

    public RenderUtil textTopCenter(MsdfFont msdfFont, String string, int i, float f) {
        return this.textAligned(RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.TOP, msdfFont, string, 0.0F, 0.0F, i, f, false);
    }

    public RenderUtil textTopCenter(MsdfFont msdfFont, Text text, float f, float f2, int i, float f3) {
        return text == null
            ? this
            : this.textAligned(
                RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.TOP, msdfFont, msdfFont.toColoredString(text, i), f, f2, i, f3, false
            );
    }

    public RenderUtil textTopRight(MsdfFont msdfFont, String string, float f, float f2, int i, float f3, boolean flag) {
        return this.textAligned(RenderUtil.HorizontalAlign.RIGHT, RenderUtil.VerticalAlign.TOP, msdfFont, string, f, f2, i, f3, flag);
    }

    public RenderUtil textTopRight(MsdfFont msdfFont, String string, float f, float f2, int i, float f3) {
        return this.textAligned(RenderUtil.HorizontalAlign.RIGHT, RenderUtil.VerticalAlign.TOP, msdfFont, string, f, f2, i, f3, false);
    }

    public RenderUtil textTopRight(MsdfFont msdfFont, String string, int i, float f) {
        return this.textAligned(RenderUtil.HorizontalAlign.RIGHT, RenderUtil.VerticalAlign.TOP, msdfFont, string, 0.0F, 0.0F, i, f, false);
    }

    public RenderUtil textTopRight(MsdfFont msdfFont, Text text, float f, float f2, int i, float f3) {
        return text == null
            ? this
            : this.textAligned(
                RenderUtil.HorizontalAlign.RIGHT, RenderUtil.VerticalAlign.TOP, msdfFont, msdfFont.toColoredString(text, i), f, f2, i, f3, false
            );
    }

    public RenderUtil textMiddleLeft(MsdfFont msdfFont, String string, float f, float f2, int i, float f3, boolean flag) {
        return this.textAligned(RenderUtil.HorizontalAlign.LEFT, RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, f, f2, i, f3, flag);
    }

    public RenderUtil textMiddleLeft(MsdfFont msdfFont, String string, float f, float f2, int i, float f3) {
        return this.textAligned(RenderUtil.HorizontalAlign.LEFT, RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, f, f2, i, f3, false);
    }

    public RenderUtil textMiddleLeft(MsdfFont msdfFont, String string, int i, float f) {
        return this.textAligned(RenderUtil.HorizontalAlign.LEFT, RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, 0.0F, 0.0F, i, f, false);
    }

    public RenderUtil textMiddleLeft(MsdfFont msdfFont, Text text, float f, float f2, int i, float f3) {
        return text == null
            ? this
            : this.textAligned(
                RenderUtil.HorizontalAlign.LEFT, RenderUtil.VerticalAlign.MIDDLE, msdfFont, msdfFont.toColoredString(text, i), f, f2, i, f3, false
            );
    }

    public RenderUtil textCenter(MsdfFont msdfFont, String string, float f, float f2, int i, float f3, boolean flag) {
        return this.textAligned(RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, f, f2, i, f3, flag);
    }

    public RenderUtil textCenter(MsdfFont msdfFont, String string, float f, float f2, int i, float f3) {
        return this.textAligned(RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, f, f2, i, f3, false);
    }

    public RenderUtil textCenter(MsdfFont msdfFont, String string, int i, float f, boolean flag) {
        return this.textAligned(RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, 0.0F, 0.0F, i, f, flag);
    }

    public RenderUtil textCenter(MsdfFont msdfFont, String string, int i, float f) {
        return this.textAligned(RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, 0.0F, 0.0F, i, f, false);
    }

    public RenderUtil textCenter(MsdfFont msdfFont, Text text, float f, float f2, int i, float f3) {
        return text == null
            ? this
            : this.textAligned(
                RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.MIDDLE, msdfFont, msdfFont.toColoredString(text, i), f, f2, i, f3, false
            );
    }

    public RenderUtil textMiddleRight(MsdfFont msdfFont, String string, float f, float f2, int i, float f3, boolean flag) {
        return this.textAligned(RenderUtil.HorizontalAlign.RIGHT, RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, f, f2, i, f3, flag);
    }

    public RenderUtil textMiddleRight(MsdfFont msdfFont, String string, float f, float f2, int i, float f3) {
        return this.textAligned(RenderUtil.HorizontalAlign.RIGHT, RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, f, f2, i, f3, false);
    }

    public RenderUtil textMiddleRight(MsdfFont msdfFont, String string, int i, float f) {
        return this.textAligned(RenderUtil.HorizontalAlign.RIGHT, RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, 0.0F, 0.0F, i, f, false);
    }

    public RenderUtil textMiddleRight(MsdfFont msdfFont, Text text, float f, float f2, int i, float f3) {
        return text == null
            ? this
            : this.textAligned(
                RenderUtil.HorizontalAlign.RIGHT, RenderUtil.VerticalAlign.MIDDLE, msdfFont, msdfFont.toColoredString(text, i), f, f2, i, f3, false
            );
    }

    public RenderUtil textBottomLeft(MsdfFont msdfFont, String string, float f, float f2, int i, float f3, boolean flag) {
        return this.textAligned(RenderUtil.HorizontalAlign.LEFT, RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, f, f2, i, f3, flag);
    }

    public RenderUtil textBottomLeft(MsdfFont msdfFont, String string, float f, float f2, int i, float f3) {
        return this.textAligned(RenderUtil.HorizontalAlign.LEFT, RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, f, f2, i, f3, false);
    }

    public RenderUtil textBottomLeft(MsdfFont msdfFont, String string, int i, float f) {
        return this.textAligned(RenderUtil.HorizontalAlign.LEFT, RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, 0.0F, 0.0F, i, f, false);
    }

    public RenderUtil textBottomLeft(MsdfFont msdfFont, Text text, float f, float f2, int i, float f3) {
        return text == null
            ? this
            : this.textAligned(
                RenderUtil.HorizontalAlign.LEFT, RenderUtil.VerticalAlign.BOTTOM, msdfFont, msdfFont.toColoredString(text, i), f, f2, i, f3, false
            );
    }

    public RenderUtil textBottomCenter(MsdfFont msdfFont, String string, float f, float f2, int i, float f3, boolean flag) {
        return this.textAligned(RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, f, f2, i, f3, flag);
    }

    public RenderUtil textBottomCenter(MsdfFont msdfFont, String string, float f, float f2, int i, float f3) {
        return this.textAligned(RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, f, f2, i, f3, false);
    }

    public RenderUtil textBottomCenter(MsdfFont msdfFont, String string, int i, float f) {
        return this.textAligned(RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, 0.0F, 0.0F, i, f, false);
    }

    public RenderUtil textBottomCenter(MsdfFont msdfFont, Text text, float f, float f2, int i, float f3) {
        return text == null
            ? this
            : this.textAligned(
                RenderUtil.HorizontalAlign.CENTER, RenderUtil.VerticalAlign.BOTTOM, msdfFont, msdfFont.toColoredString(text, i), f, f2, i, f3, false
            );
    }

    public RenderUtil textBottomRight(MsdfFont msdfFont, String string, float f, float f2, int i, float f3, boolean flag) {
        return this.textAligned(RenderUtil.HorizontalAlign.RIGHT, RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, f, f2, i, f3, flag);
    }

    public RenderUtil textBottomRight(MsdfFont msdfFont, String string, float f, float f2, int i, float f3) {
        return this.textAligned(RenderUtil.HorizontalAlign.RIGHT, RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, f, f2, i, f3, false);
    }

    public RenderUtil textBottomRight(MsdfFont msdfFont, String string, int i, float f) {
        return this.textAligned(RenderUtil.HorizontalAlign.RIGHT, RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, 0.0F, 0.0F, i, f, false);
    }

    public RenderUtil textBottomRight(MsdfFont msdfFont, Text text, float f, float f2, int i, float f3) {
        return text == null
            ? this
            : this.textAligned(
                RenderUtil.HorizontalAlign.RIGHT, RenderUtil.VerticalAlign.BOTTOM, msdfFont, msdfFont.toColoredString(text, i), f, f2, i, f3, false
            );
    }

    private RenderUtil scrollingTextAligned(
        RenderUtil.VerticalAlign verticalAlign, MsdfFont msdfFont, String string, float f, float f2, float f3, int i, float f4, float f5, Boolean flag
    ) {
        float f6 = switch (verticalAlign) {
            case TOP -> this.y + f2;
            case MIDDLE -> this.y + this.height / 2.0F - f4 / 2.0F + f2;
            case BOTTOM -> this.y + this.height - f2 - f4;
        };
        boolean flag2 = flag != null ? flag : this.isMouseOver(this.x + f, f6, f3, f4);
        Matrix3x2fStack matrix3x2fStack = this.context.getMatrices();
        drawScrollingText(this.context, msdfFont, string, this.x + f, f6, f3, i, f4, f5, flag2, matrix3x2fStack);
        return this;
    }

    private boolean isMouseOver(float f, float f2, float f3, float f4) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Window window = minecraftClient.getWindow();
        if (window.getWidth() != 0 && window.getHeight() != 0) {
            double d = window.getScaleFactor();
            double d2 = d <= 0.0 ? window.getScaledWidth() : window.getFramebufferWidth() / d;
            double d3 = d <= 0.0 ? window.getScaledHeight() : window.getFramebufferHeight() / d;
            double d4 = minecraftClient.mouse.getX() * d2 / window.getWidth();
            double d5 = minecraftClient.mouse.getY() * d3 / window.getHeight();
            Matrix3x2fStack matrix3x2fStack = this.context.getMatrices();
            Vector2f vector2f = matrix3x2fStack.transformPosition(f, f2, new Vector2f());
            Vector2f vector2f2 = matrix3x2fStack.transformPosition(f + f3, f2 + f4, new Vector2f());
            return d4 >= Math.min(vector2f.x, vector2f2.x)
                && d4 <= Math.max(vector2f.x, vector2f2.x)
                && d5 >= Math.min(vector2f.y, vector2f2.y)
                && d5 <= Math.max(vector2f.y, vector2f2.y);
        } else {
            return false;
        }
    }

    public RenderUtil scrollingTextTop(MsdfFont msdfFont, String string, float f, float f2, float f3, int i, float f4, boolean flag) {
        return this.scrollingTextAligned(RenderUtil.VerticalAlign.TOP, msdfFont, string, f, f2, f3, i, f4, 12.0F, flag);
    }

    public RenderUtil scrollingTextTop(MsdfFont msdfFont, String string, float f, float f2, float f3, int i, float f4) {
        return this.scrollingTextAligned(RenderUtil.VerticalAlign.TOP, msdfFont, string, f, f2, f3, i, f4, 12.0F, null);
    }

    public RenderUtil scrollingTextTop(MsdfFont msdfFont, String string, float f, float f2, float f3, int i, boolean flag) {
        return this.scrollingTextAligned(RenderUtil.VerticalAlign.TOP, msdfFont, string, f, f2, f3, i, 6.5F, 12.0F, flag);
    }

    public RenderUtil scrollingTextTop(MsdfFont msdfFont, String string, float f, float f2, float f3, int i) {
        return this.scrollingTextAligned(RenderUtil.VerticalAlign.TOP, msdfFont, string, f, f2, f3, i, 6.5F, 12.0F, null);
    }

    public RenderUtil scrollingTextMiddle(MsdfFont msdfFont, String string, float f, float f2, float f3, int i, float f4, boolean flag) {
        return this.scrollingTextAligned(RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, f, f2, f3, i, f4, 12.0F, flag);
    }

    public RenderUtil scrollingTextMiddle(MsdfFont msdfFont, String string, float f, float f2, float f3, int i, float f4) {
        return this.scrollingTextAligned(RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, f, f2, f3, i, f4, 12.0F, null);
    }

    public RenderUtil scrollingTextMiddle(MsdfFont msdfFont, String string, float f, float f2, float f3, int i, boolean flag) {
        return this.scrollingTextAligned(RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, f, f2, f3, i, 6.5F, 12.0F, flag);
    }

    public RenderUtil scrollingTextMiddle(MsdfFont msdfFont, String string, float f, float f2, float f3, int i) {
        return this.scrollingTextAligned(RenderUtil.VerticalAlign.MIDDLE, msdfFont, string, f, f2, f3, i, 6.5F, 12.0F, null);
    }

    public RenderUtil scrollingTextBottom(MsdfFont msdfFont, String string, float f, float f2, float f3, int i, float f4, boolean flag) {
        return this.scrollingTextAligned(RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, f, f2, f3, i, f4, 12.0F, flag);
    }

    public RenderUtil scrollingTextBottom(MsdfFont msdfFont, String string, float f, float f2, float f3, int i, float f4) {
        return this.scrollingTextAligned(RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, f, f2, f3, i, f4, 12.0F, null);
    }

    public RenderUtil scrollingTextBottom(MsdfFont msdfFont, String string, float f, float f2, float f3, int i, boolean flag) {
        return this.scrollingTextAligned(RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, f, f2, f3, i, 6.5F, 12.0F, flag);
    }

    public RenderUtil scrollingTextBottom(MsdfFont msdfFont, String string, float f, float f2, float f3, int i) {
        return this.scrollingTextAligned(RenderUtil.VerticalAlign.BOTTOM, msdfFont, string, f, f2, f3, i, 6.5F, 12.0F, null);
    }

    public RenderUtil icon(Icon icon2, float f, float f2, float f3, int i) {
        return this.icon(icon2, f, f2, f3, f3, i);
    }

    public RenderUtil icon(Icon icon2, float f, float f2, float f3, float f4, int i) {
        if (icon2 != null && !(f3 <= 0.0F) && !(f4 <= 0.0F)) {
            float f5 = this.x + f;
            float f6 = this.y + f2;
            Matrix3x2fStack matrix3x2fStack = this.context.getMatrices();
            drawIconTransformed(this.context, icon2, f5, f6, f3, f4, i, matrix3x2fStack);
            return this;
        } else {
            return this;
        }
    }

    public RenderUtil spinner(float f, float f2, float f3, int i) {
        return this.spinner(f, f2, f3, f3, i, i, 0.0F, 0.0F, 0.0F);
    }

    public RenderUtil spinner(float f, float f2, float f3, int i, int i2) {
        return this.spinner(f, f2, f3, f3, i, i2, 0.0F, 0.0F, 0.0F);
    }

    public RenderUtil spinner(float f, float f2, float f3, float f4, int i, int i2, float f5, float f6, float f7) {
        if (!(f3 <= 0.0F) && !(f4 <= 0.0F)) {
            TextureSetup textureSetup = LogoTexture.getTextureSetup();
            if (textureSetup == null) {
                return this;
            }

            Matrix3x2f matrix3x2f = new Matrix3x2f(this.context.getMatrices());
            LogoRenderer.draw(this.context, textureSetup, matrix3x2f, this.x + f, this.y + f2, f3, f4, i, i2, f5, f6, f7, 1.0F);
            return this;
        } else {
            return this;
        }
    }

    public static void drawChevron(DrawContext drawContext, float f, float f2, float f3, float f4, int i) {
        drawIcon(drawContext, Icon.CHEVRON_RIGHT, f, f2, f3, f4, i);
    }

    public static void drawIcon(DrawContext drawContext, Icon icon2, float f, float f2, float f3, float f4, int i) {
        if (drawContext != null && icon2 != null && !(f3 <= 0.0F)) {
            Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
            drawIconImpl(drawContext, icon2, f, f2, f3, f3, i, matrix3x2fStack, f4);
        }
    }

    public RenderUtil sprite(AnimatedIcon animatedIcon, float f, float f2, float f3, int i) {
        return this.sprite(animatedIcon, f, f2, f3, f3, i);
    }

    public RenderUtil sprite(AnimatedIcon animatedIcon, float f, float f2, float f3, float f4, int i) {
        return animatedIcon == null ? this : this.spriteFrame(animatedIcon, f, f2, f3, f4, i, AnimatedIconManager.getCurrentFrame(animatedIcon));
    }

    public RenderUtil spriteBlended(AnimatedIcon animatedIcon, float f, float f2, float f3, float f4, int i) {
        return this.spriteBlended(animatedIcon, f, f2, f3, f4, i, Radius.zero());
    }

    public RenderUtil spriteBlended(AnimatedIcon animatedIcon, float f, float f2, float f3, float f4, int i, Radius radius) {
        if (animatedIcon != null && animatedIcon.getFrameCount() > 0) {
            int i2 = AnimatedIconManager.getCurrentFrame(animatedIcon);
            int i3 = (i2 + 1) % animatedIcon.getFrameCount();
            float f5 = AnimatedIconManager.getFrameProgress(animatedIcon);
            this.spriteFrame(animatedIcon, f, f2, f3, f4, i, i, i2, radius);
            if (f5 > 0.001F) {
                int i4 = Math.round((i >>> 24 & 0xFF) * f5);
                if (i4 > 0) {
                    int i5 = i & 16777215 | i4 << 24;
                    this.spriteFrame(animatedIcon, f, f2, f3, f4, i5, i5, i3, radius);
                    return this;
                }
            }
        }

        return this;
    }

    public RenderUtil spriteAtProgress(AnimatedIcon animatedIcon, float f, float f2, float f3, float f4, int i, float f5) {
        return this.spriteAtProgress(animatedIcon, f, f2, f3, f4, i, i, f5);
    }

    public RenderUtil spriteAtProgress(AnimatedIcon animatedIcon, float f, float f2, float f3, float f4, int i, int i2, float f5) {
        return animatedIcon == null ? this : this.spriteFrame(animatedIcon, f, f2, f3, f4, i, i2, AnimatedIconManager.getFrameAt(animatedIcon, f5));
    }

    private RenderUtil spriteFrame(AnimatedIcon animatedIcon, float f, float f2, float f3, float f4, int i, int i2) {
        return this.spriteFrame(animatedIcon, f, f2, f3, f4, i, i, i2);
    }

    private RenderUtil spriteFrame(AnimatedIcon animatedIcon, float f, float f2, float f3, float f4, int i, int i2, int i3) {
        return this.spriteFrame(animatedIcon, f, f2, f3, f4, i, i2, i3, Radius.zero());
    }

    private RenderUtil spriteFrame(AnimatedIcon animatedIcon, float f, float f2, float f3, float f4, int i, int i2, int i3, Radius radius) {
        if (!(f3 <= 0.0F) && !(f4 <= 0.0F)) {
            TextureSetup textureSetup = AnimatedIconManager.getTextureSetup(animatedIcon);
            if (textureSetup == null) {
                return this;
            } else {
                int i4 = i3 % animatedIcon.getColumns();
                int i5 = i3 / animatedIcon.getColumns();
                float f5 = 1.0F / animatedIcon.getColumns();
                float f6 = 1.0F / animatedIcon.getRows();
                float f7 = i4 * f5;
                float f8 = i5 * f6;
                float f9 = f7 + f5;
                float f10 = f8 + f6;
                float f11 = this.x + f;
                float f12 = this.y + f2;
                Matrix3x2fStack matrix3x2fStack = this.context.getMatrices();
                float f13 = radius == null
                    ? 0.0F
                    : Math.max(Math.max(radius.getTopRight(), radius.getBottomLeft()), Math.max(radius.getTopLeft(), radius.getBottomRight()));
                if (f13 > 0.001F) {
                    TexturedShapeRenderer.drawOutline(this.context, textureSetup, matrix3x2fStack, f11, f12, f3, f4, f7, f8, f9, f10, f13, 1.0F, i);
                    return this;
                } else {
                    TexturedShapeRenderer.drawTextureRegion(
                        this.context,
                        textureSetup,
                        matrix3x2fStack,
                        f11,
                        f12,
                        f3,
                        f4,
                        f7,
                        f8,
                        f9,
                        f10,
                        Radius.zero(),
                        0.0F,
                        -0.5F,
                        0.5F,
                        1.0F,
                        i,
                        i2,
                        2,
                        false
                    );
                    return this;
                }
            }
        } else {
            return this;
        }
    }

    private static void drawIconRaw(DrawContext drawContext, Icon icon2, float f, float f2, float f3, float f4, int i) {
        drawIconImpl(drawContext, icon2, f, f2, f3, f4, i, drawContext.getMatrices(), 0.0F);
    }

    private static void drawIconTransformed(DrawContext drawContext, Icon icon2, float f, float f2, float f3, float f4, int i, Matrix3x2fc matrix3x2fc) {
        drawIconImpl(drawContext, icon2, f, f2, f3, f4, i, matrix3x2fc, 0.0F);
    }

    private static void drawIconRotated(DrawContext drawContext, Icon icon2, float f, float f2, float f3, float f4, int i, float f5) {
        drawIconImpl(drawContext, icon2, f, f2, f3, f4, i, drawContext.getMatrices(), f5);
    }

    private static void drawIconImpl(DrawContext drawContext, Icon icon2, float f, float f2, float f3, float f4, int i, Matrix3x2fc matrix3x2fc, float f5) {
        TextureSetup textureSetup = SvgIconCache.getTextureSetup(icon2);
        if (textureSetup != null && !(f3 <= 0.0F) && !(f4 <= 0.0F)) {
            Matrix3x2fc matrix3x2fc2 = matrix3x2fc;
            if (f5 != 0.0F) {
                float f6 = f + f3 / 2.0F;
                float f7 = f2 + f4 / 2.0F;
                matrix3x2fc2 = new Matrix3x2f(matrix3x2fc).translate(f6, f7).rotate((float)Math.toRadians(f5)).translate(-f6, -f7);
            }

            TexturedShapeRenderer.drawTexture(drawContext, textureSetup, matrix3x2fc2, f, f2, f3, f4, Radius.zero(), SvgIconCache.getSdfSpread(), -0.4F, 0.4F, 1.0F, i, 6);
        }
    }

    private static void drawTextLeft(DrawContext drawContext, MsdfFont msdfFont, String string, float f, float f2, float f3, int i, boolean flag, Matrix3x2fc matrix3x2fc) {
        if (string != null && !string.isEmpty()) {
            if (msdfFont == null) {
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                drawContext.drawText(minecraftClient.textRenderer, string, Math.round(f), Math.round(f2), i, flag);
            } else if (flag) {
                msdfFont.draw(drawContext, string, f + 0.25F, f2 + 0.25F, ColorUtil.darken(i, 0.25F), f3, matrix3x2fc, false);
                msdfFont.draw(drawContext, string, f, f2, i, f3, matrix3x2fc);
            } else {
                msdfFont.draw(drawContext, string, f, f2, i, f3, matrix3x2fc);
            }
        }
    }

    private static void drawTextCentered(
        DrawContext drawContext, MsdfFont msdfFont, String string, float f, float f2, float f3, int i, boolean flag, Matrix3x2fc matrix3x2fc
    ) {
        if (string != null && !string.isEmpty()) {
            if (msdfFont == null) {
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                int i2 = minecraftClient.textRenderer.getWidth(string);
                drawContext.drawText(minecraftClient.textRenderer, string, Math.round(f - i2 / 2.0F), Math.round(f2), i, flag);
            } else if (flag) {
                float f4 = f - msdfFont.getWidth(string, f3) / 2.0F;
                msdfFont.draw(drawContext, string, f4 + 0.25F, f2 + 0.25F, ColorUtil.darken(i, 0.25F), f3, matrix3x2fc, false);
                msdfFont.draw(drawContext, string, f4, f2, i, f3, matrix3x2fc);
            } else {
                msdfFont.draw(drawContext, string, f - msdfFont.getWidth(string, f3) / 2.0F, f2, i, f3, matrix3x2fc);
            }
        }
    }

    private static void drawTextRight(
        DrawContext drawContext, MsdfFont msdfFont, String string, float f, float f2, float f3, int i, boolean flag, Matrix3x2fc matrix3x2fc
    ) {
        if (string != null && !string.isEmpty()) {
            if (msdfFont == null) {
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                int i2 = minecraftClient.textRenderer.getWidth(string);
                drawContext.drawText(minecraftClient.textRenderer, string, Math.round(f - i2), Math.round(f2), i, flag);
            } else if (flag) {
                float f4 = f - msdfFont.getWidth(string, f3);
                msdfFont.draw(drawContext, string, f4 + 0.25F, f2 + 0.25F, ColorUtil.darken(i, 0.25F), f3, matrix3x2fc, false);
                msdfFont.draw(drawContext, string, f4, f2, i, f3, matrix3x2fc);
            } else {
                msdfFont.draw(drawContext, string, f - msdfFont.getWidth(string, f3), f2, i, f3, matrix3x2fc);
            }
        }
    }

    private static void drawScrollingText(
        DrawContext drawContext, MsdfFont msdfFont, String string, float f, float f2, float f3, int i, float f4, float f5, boolean flag, Matrix3x2fc matrix3x2fc
    ) {
        if (string != null && !string.isEmpty() && !(f3 <= 0.0F)) {
            if (msdfFont == null) {
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                PixelRenderUtil.scissor(
                    drawContext, f, f2, f3, minecraftClient.textRenderer.fontHeight, () -> drawContext.drawText(minecraftClient.textRenderer, string, Math.round(f), Math.round(f2), i, false)
                );
            } else {
                msdfFont.drawScrolling(drawContext, string, f, f2, f3, i, f4, f5, flag, matrix3x2fc);
            }
        }
    }

    public RenderUtil scissor(Runnable runnable) {
        PixelRenderUtil.scissor(this.context, this.x, this.y, this.width, this.height, runnable);
        return this;
    }

    public boolean isInside(double d, double d2) {
        return d >= this.x && d <= this.x + this.width && d2 >= this.y && d2 <= this.y + this.height;
    }

    public RenderUtil copy() {
        RenderUtil renderUtil = new RenderUtil(this.context, 0.0F, 0.0F, 0.0F, 0.0F);
        renderUtil.x = this.x;
        renderUtil.y = this.y;
        renderUtil.width = this.width;
        renderUtil.height = this.height;
        return renderUtil;
    }

    private void drawRectShape(float f, float f2, float f3, int i, int i2, int i3, int i4, Radius radius, int i5) {
        RoundedRectRenderer.draw(
            this.context, this.context.getMatrices(), this.x, this.y, this.width, this.height, radius, f2, f3, f, i4, i3, i, i2, i5
        );
    }

    private void drawTexturedWhite(float f, float f2, float f3, float f4, Radius radius, TextureSetup textureSetup, int i) {
        this.drawTextured(ColorUtil.WHITE, f, f2, f3, f4, radius, textureSetup, i, false);
    }

    private void drawTextured(int i, float f, float f2, float f3, float f4, Radius radius, TextureSetup textureSetup, int i2, boolean flag) {
        TexturedShapeRenderer.drawTexture(
            this.context, textureSetup, this.context.getMatrices(), this.x, this.y, this.width, this.height, radius, f2, f3, f4, f, i, i2, flag
        );
    }

    private RenderUtil rectInternal(float f, float f2, float f3, float f4, int i, int i2, int i3, int i4, Radius radius, int i5) {
        if (f == 0.0F) {
            this.drawRectShape(f2, f3, f4, i, i2, i3, i4, radius, i5);
            return this;
        } else {
            float f5 = this.x;
            float f6 = this.y;
            float f7 = this.width;
            float f8 = this.height;
            this.expand(Side.ALL, f);
            this.drawRectShape(f2, f3, f4, i, i2, i3, i4, radius, i5);
            this.x = f5;
            this.y = f6;
            this.width = f7;
            this.height = f8;
            return this;
        }
    }

    private RenderUtil texturedInternal(
        float f, int i, float f2, float f3, float f4, float f5, Radius radius, TextureSetup textureSetup, int i2, boolean flag
    ) {
        if (f == 0.0F) {
            this.drawTextured(i, f2, f3, f4, f5, radius, textureSetup, i2, flag);
            return this;
        } else {
            float f6 = this.x;
            float f7 = this.y;
            float f8 = this.width;
            float f9 = this.height;
            this.expand(Side.ALL, f);
            this.drawTextured(i, f2, f3, f4, f5, radius, textureSetup, i2, flag);
            this.x = f6;
            this.y = f7;
            this.width = f8;
            this.height = f9;
            return this;
        }
    }

    public enum BlurQuality {
        LOW,
        MIDDLE,
        MAX;
    }

    public enum HorizontalAlign {
        LEFT,
        CENTER,
        RIGHT;
    }

    public enum VerticalAlign {
        TOP,
        MIDDLE,
        BOTTOM;
    }
}
