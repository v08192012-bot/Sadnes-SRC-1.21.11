package tech.sadnes.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.math.MathUtil;

public final class GuiParticleSystem implements IMinecraft {
    private static final float BLOOM_SCALE = 6.0F;
    private static final float BLOOM_ALPHA = 0.12F;
    private static final float SPRITE_SCALE = 1.6F;
    private static final float COLLISION_SCALE = 1.2F;
    private static final int COLLISION_ITERATIONS = 2;
    private static final float FRICTION = 0.2F;
    private static final float BOUNCE = 0.35F;
    private final List<GuiParticle> particles = new ArrayList<>();

    public void spawn(int i, Supplier<GuiParticle> supplier) {
        for (int i2 = 0; i2 < i; i2++) {
            this.particles.add((GuiParticle)supplier.get());
        }
    }

    public void update() {
        if (!this.particles.isEmpty()) {
            this.particles.removeIf(GuiParticle::isDead);
            float f = MathUtil.getFrameTime();

            for (GuiParticle guiParticle : this.particles) {
                guiParticle.update(f);
            }
        }
    }

    public void applyForce(float f, float f2) {
        for (GuiParticle guiParticle : this.particles) {
            guiParticle.setOrigin(f, f2);
        }
    }

    public void updateInBounds(float f, float f2, float f3, float f4) {
        if (!this.particles.isEmpty()) {
            this.particles.removeIf(GuiParticle::isDead);
            float f5 = MathUtil.getFrameTime();

            for (GuiParticle guiParticle : this.particles) {
                guiParticle.update(f5);
            }

            for (int i = 0; i < 2; i++) {
                this.resolveCollisions();

                for (GuiParticle guiParticle2 : this.particles) {
                    this.constrainToBounds(guiParticle2, f, f2, f3, f4);
                }
            }
        }
    }

    private void resolveCollisions() {
        int i = this.particles.size();

        for (int i2 = 0; i2 < i; i2++) {
            GuiParticle guiParticle = this.particles.get(i2);
            float f = guiParticle.getSize() * 1.2F * 0.5F;

            for (int i3 = i2 + 1; i3 < i; i3++) {
                GuiParticle guiParticle2 = this.particles.get(i3);
                float f2 = f + guiParticle2.getSize() * 1.2F * 0.5F;
                float f3 = guiParticle2.getX() - guiParticle.getX();
                float f4 = guiParticle2.getY() - guiParticle.getY();
                float f5 = f3 * f3 + f4 * f4;
                if (!(f5 >= f2 * f2) && !(f5 < 1.0E-4F)) {
                    float f6 = (float)Math.sqrt(f5);
                    float f7 = f3 / f6;
                    float f8 = f4 / f6;
                    float f9 = (f2 - f6) * 0.5F;
                    guiParticle.move(-f7 * f9, -f8 * f9);
                    guiParticle2.move(f7 * f9, f8 * f9);
                    float f10 = (guiParticle.getMotionX() - guiParticle2.getMotionX()) * f7 + (guiParticle.getMotionY() - guiParticle2.getMotionY()) * f8;
                    if (!(f10 <= 0.0F)) {
                        float f11 = f10 * 1.2F * 0.5F;
                        guiParticle.setMotion(guiParticle.getMotionX() - f11 * f7, guiParticle.getMotionY() - f11 * f8);
                        guiParticle2.setMotion(guiParticle2.getMotionX() + f11 * f7, guiParticle2.getMotionY() + f11 * f8);
                    }
                }
            }
        }
    }

    private void constrainToBounds(GuiParticle guiParticle, float f, float f2, float f3, float f4) {
        float f5 = guiParticle.getSize() * 1.2F * 0.5F;
        if (guiParticle.getX() - f5 < f) {
            guiParticle.setX(f + f5);
            if (guiParticle.getMotionX() < 0.0F) {
                guiParticle.setMotion(-guiParticle.getMotionX() * 0.35F, guiParticle.getMotionY());
            }
        } else if (guiParticle.getX() + f5 > f3) {
            guiParticle.setX(f3 - f5);
            if (guiParticle.getMotionX() > 0.0F) {
                guiParticle.setMotion(-guiParticle.getMotionX() * 0.35F, guiParticle.getMotionY());
            }
        }

        if (guiParticle.getY() - f5 < f2) {
            guiParticle.setY(f2 + f5);
            if (guiParticle.getMotionY() < 0.0F) {
                guiParticle.setMotion(guiParticle.getMotionX(), -guiParticle.getMotionY() * 0.35F);
                return;
            }
        } else if (guiParticle.getY() + f5 > f4) {
            guiParticle.setY(f4 - f5);
            if (guiParticle.getMotionY() > 0.0F) {
                guiParticle.setMotion(guiParticle.getMotionX(), -guiParticle.getMotionY() * 0.35F);
            }
        }
    }

    public void render(DrawContext drawContext, float f) {
        if (!this.particles.isEmpty() && !(f <= 0.004F)) {
            TextureSetup textureSetup = this.getTexture(Texture.BLOOM);
            if (textureSetup != null) {
                for (GuiParticle guiParticle : this.particles) {
                    float f2 = guiParticle.getAlpha() * f * 0.12F;
                    if (!(f2 <= 0.004F)) {
                        float f3 = guiParticle.getSize() * 6.0F;
                        this.drawSprite(drawContext, textureSetup, new Matrix3x2f(drawContext.getMatrices()), guiParticle.getX(), guiParticle.getY(), f3, f2, guiParticle.getColor());
                    }
                }
            }

            for (GuiParticle guiParticle2 : this.particles) {
                float f4 = guiParticle2.getAlpha() * f;
                if (!(f4 <= 0.004F)) {
                    TextureSetup textureSetup2 = this.getTexture(guiParticle2.getTexture());
                    if (textureSetup2 != null) {
                        Matrix3x2f matrix3x2f = new Matrix3x2f(drawContext.getMatrices());
                        if (guiParticle2.getRotation() != 0.0F) {
                            matrix3x2f.translate(guiParticle2.getX(), guiParticle2.getY()).rotate((float)Math.toRadians(guiParticle2.getRotation())).translate(-guiParticle2.getX(), -guiParticle2.getY());
                        }

                        this.drawSprite(drawContext, textureSetup2, matrix3x2f, guiParticle2.getX(), guiParticle2.getY(), guiParticle2.getSize() * 1.6F, f4, guiParticle2.getColor());
                    }
                }
            }
        }
    }

    public void clear() {
        this.particles.clear();
    }

    public boolean isEmpty() {
        return this.particles.isEmpty();
    }

    private void drawSprite(DrawContext drawContext, TextureSetup textureSetup, Matrix3x2f matrix3x2f, float f, float f2, float f3, float f4, int i) {
        TexturedShapeRenderer.drawBloom(drawContext, textureSetup, matrix3x2f, f - f3 / 2.0F, f2 - f3 / 2.0F, f3, f3, f4, i);
    }

    private TextureSetup getTexture(Texture texture) {
        if (texture == null) {
            return null;
        }

        AbstractTexture abstractTexture = mc.getTextureManager().getTexture(texture.getIdentifier());
        if (abstractTexture == null) {
            return null;
        }

        GpuTextureView gpuTextureView = abstractTexture.getGlTextureView();
        return gpuTextureView != null && !gpuTextureView.isClosed() ? TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR)) : null;
    }
}
