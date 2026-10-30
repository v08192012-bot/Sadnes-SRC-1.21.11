package tech.sadnes.client.ui.menu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import tech.sadnes.client.render.LogoGlowRenderer;

final class LogoLiquidEffect {
    private final float[] rippleX = new float[12];
    private final float[] rippleY = new float[12];
    private final float[] rippleStrength = new float[12];
    private final float[] rippleAge = new float[12];
    private final float[] rippleDirX = new float[12];
    private final float[] rippleDirY = new float[12];
    private int nextRippleIndex;
    private final Matrix3x2f transform = new Matrix3x2f();
    private final Matrix3x2f inverseTransform = new Matrix3x2f();
    private final LogoGlowRenderer.GlowData uniforms = new LogoGlowRenderer.GlowData();
    private float energy;
    private float stretch;
    private float phase;
    private float time;
    private float idleBlend = 1.0F;
    private float lastMouseX;
    private float lastMouseY;
    private boolean initialized;
    private float velocityX;
    private float velocityY;
    private float speedFactor;
    private float directionX = 1.0F;
    private float directionY;
    private float trailDistance;
    private float wobbleX;
    private float wobbleXVelocity;
    private float wobbleY;
    private float wobbleYVelocity;
    private float wobbleRotation;
    private float wobbleRotationVelocity;

    void reset() {
        this.initialized = false;
        this.velocityX = this.velocityY = this.speedFactor = this.trailDistance = 0.0F;
        this.stretch = this.energy = 0.0F;
        this.idleBlend = 1.0F;
        this.wobbleX = this.wobbleXVelocity = this.wobbleY = this.wobbleYVelocity = this.wobbleRotation = this.wobbleRotationVelocity = 0.0F;
        int i = this.rippleStrength.length;

        for (int i2 = 0; i2 < i; i2++) {
            this.rippleStrength[i2] = 0.0F;
        }
    }

    void addEnergy(float f) {
        this.energy = Math.min(1.0F, this.energy + f * 0.055F);
    }

    void update(float f, float f2, float f3, float f4, float f5, boolean flag) {
        this.energy *= 0.935F;
        if (this.energy < 0.002F) {
            this.energy = 0.0F;
        }

        this.time += 0.016666668F;
        this.idleBlend = this.idleBlend + ((flag ? 1.0F : 0.0F) - this.idleBlend) * 0.06F;
        float f6 = f - this.lastMouseX;
        float f7 = f2 - this.lastMouseY;
        this.lastMouseX = f;
        this.lastMouseY = f2;
        boolean flag2 = !this.initialized || f6 * f6 + f7 * f7 > 22500.0F;
        this.initialized = true;
        if (flag2) {
            f7 = 0.0F;
            f6 = 0.0F;
        }

        this.velocityX = this.velocityX + (f6 / 0.016666668F - this.velocityX) * 0.19F;
        this.velocityY = this.velocityY + (f7 / 0.016666668F - this.velocityY) * 0.19F;
        float f8 = (float)Math.sqrt(this.velocityX * this.velocityX + this.velocityY * this.velocityY);
        this.speedFactor = Math.min(1.0F, f8 / 900.0F);
        if (f8 > 1.0F) {
            this.directionX = this.velocityX / f8;
            this.directionY = this.velocityY / f8;
        }

        boolean flag3 = false;
        int i = this.rippleStrength.length;

        for (int i2 = 0; i2 < i; i2++) {
            if (!(this.rippleStrength[i2] <= 0.0F)) {
                this.rippleAge[i2] = this.rippleAge[i2] + 0.016666668F / 1.1F;
                if (this.rippleAge[i2] >= 1.0F) {
                    this.rippleStrength[i2] = 0.0F;
                }

                float f9 = this.rippleX[i2] - f;
                float f10 = this.rippleY[i2] - f2;
                if (this.rippleAge[i2] < 0.36F && f9 * f9 + f10 * f10 < 64.0F) {
                    flag3 = true;
                }
            }
        }

        boolean flag4 = this.speedFactor > 0.05F;
        this.trailDistance = flag4 ? this.trailDistance + (float)Math.sqrt(f6 * f6 + f7 * f7) : 0.0F;
        float f11 = f - f3;
        float f12 = f2 - f4;
        boolean flag5 = f11 * f11 + f12 * f12 <= f5 * f5 * 0.13F;
        if (this.trailDistance >= 8.0F && flag5) {
            this.trailDistance = 0.0F;
            if (!flag3) {
                this.spawnRipple(f, f2);
            }
        }

        float f13 = Math.max(0.0F, (this.speedFactor - 0.04F) / 0.96F);
        float f14 = Math.min(1.0F, f13 * (float)Math.sqrt(f13) * 1.1F);
        this.stretch = f14 > this.stretch ? this.stretch + (f14 - this.stretch) * 0.25F : Math.max(f14, this.stretch - 0.016666668F / 1.2F);
        this.phase = this.phase + 0.016666668F * (1.0F + 1.8F * this.stretch);
        if (this.phase > 16384.0F) {
            this.phase -= 16384.0F;
        }

        this.updateWobble(0.016666668F);
    }

    private void spawnRipple(float f, float f2) {
        int i = this.nextRippleIndex;
        this.nextRippleIndex = (this.nextRippleIndex + 1) % this.rippleX.length;
        this.rippleX[i] = f;
        this.rippleY[i] = f2;
        float f3 = MathHelper.clamp((this.speedFactor - 0.05F) / 0.75F, 0.0F, 1.0F);
        this.rippleStrength[i] = Math.max(f3 * f3 * (3.0F - 2.0F * f3), 0.004F);
        this.rippleAge[i] = 0.0F;
        this.rippleDirX[i] = this.directionX;
        this.rippleDirY[i] = this.directionY;
    }

    private void updateWobble(float f) {
        float f2 = 2.6F * (float)Math.tanh(this.velocityX / 700.0F);
        float f3 = 2.6F * (float)Math.tanh(this.velocityY / 700.0F);
        float f4 = 0.03F * (float)Math.tanh(this.velocityX / 700.0F);
        this.wobbleXVelocity = this.wobbleXVelocity + ((f2 - this.wobbleX) * 40.0F - this.wobbleXVelocity * 7.0F) * f;
        this.wobbleYVelocity = this.wobbleYVelocity + ((f3 - this.wobbleY) * 40.0F - this.wobbleYVelocity * 7.0F) * f;
        this.wobbleRotationVelocity = this.wobbleRotationVelocity + ((f4 - this.wobbleRotation) * 40.0F - this.wobbleRotationVelocity * 7.0F) * f;
        this.wobbleX = this.wobbleX + this.wobbleXVelocity * f;
        this.wobbleY = this.wobbleY + this.wobbleYVelocity * f;
        this.wobbleRotation = this.wobbleRotation + this.wobbleRotationVelocity * f;
    }

    void render(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, float f9) {
        float f10 = 1.2F * MathHelper.sin(this.time * 0.83F) + 0.8F * MathHelper.sin(this.time * 0.37F + 1.1F);
        float f11 = 1.1F * MathHelper.sin(this.time * 0.61F + 0.4F) + 0.7F * MathHelper.sin(this.time * 0.29F);
        float f12 = 0.011F * MathHelper.sin(this.time * 0.47F) + 0.006F * MathHelper.sin(this.time * 0.23F + 2.0F);
        float f13 = (f10 + this.wobbleX) * this.idleBlend / f6;
        float f14 = (f11 + this.wobbleY) * this.idleBlend / f6;
        float f15 = 1.0F + 0.015F * this.idleBlend * MathHelper.sin(this.time * 0.9F);
        this.transform
            .translation(f, f2)
            .scale(f3, f4)
            .rotate((float)Math.toRadians(f5))
            .translate(-f6 / 2.0F, -f6 / 2.0F)
            .scale(f6)
            .translate(0.5F + f13, 0.5F + f14)
            .rotate((f12 + this.wobbleRotation) * this.idleBlend)
            .scale(f15)
            .translate(-0.5F, -0.5F);
        this.transform.invert(this.inverseTransform);
        int i2 = this.rippleX.length;

        for (int i3 = 0; i3 < i2; i3++) {
            float f16 = this.toLocalX(this.rippleX[i3], this.rippleY[i3]);
            float f17 = this.toLocalY(this.rippleX[i3], this.rippleY[i3]);
            boolean flag = (f16 - 0.5F) * (f16 - 0.5F) + (f17 - 0.5F) * (f17 - 0.5F) > 0.9025F;
            if (!(this.rippleStrength[i3] <= 0.0F) && !flag) {
                this.uniforms
                    .setPoint(
                        i3,
                        f16,
                        f17,
                        this.rippleStrength[i3],
                        this.rippleAge[i3],
                        this.toLocalDirX(this.rippleDirX[i3], this.rippleDirY[i3]),
                        this.toLocalDirY(this.rippleDirX[i3], this.rippleDirY[i3])
                    );
            } else {
                this.uniforms.clearPoint(i3);
            }
        }

        this.uniforms
            .setCursor(
                this.toLocalX(f7, f8),
                this.toLocalY(f7, f8),
                this.speedFactor,
                this.toLocalDirX(this.directionX, this.directionY),
                this.toLocalDirY(this.directionX, this.directionY)
            );
        LogoGlowRenderer.draw(drawContext, this.transform, i, f9, this.stretch, this.energy, this.phase, this.uniforms);
    }

    private float toLocalX(float f, float f2) {
        return this.inverseTransform.m00() * f + this.inverseTransform.m10() * f2 + this.inverseTransform.m20();
    }

    private float toLocalY(float f, float f2) {
        return this.inverseTransform.m01() * f + this.inverseTransform.m11() * f2 + this.inverseTransform.m21();
    }

    private float toLocalDirX(float f, float f2) {
        float f3 = this.inverseTransform.m00() * f + this.inverseTransform.m10() * f2;
        float f4 = this.inverseTransform.m01() * f + this.inverseTransform.m11() * f2;
        return f3 / Math.max((float)Math.sqrt(f3 * f3 + f4 * f4), 1.0E-6F);
    }

    private float toLocalDirY(float f, float f2) {
        float f3 = this.inverseTransform.m00() * f + this.inverseTransform.m10() * f2;
        float f4 = this.inverseTransform.m01() * f + this.inverseTransform.m11() * f2;
        return f4 / Math.max((float)Math.sqrt(f3 * f3 + f4 * f4), 1.0E-6F);
    }
}
