package tech.sadnes.client.ui.clickgui;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiParticle;
import tech.sadnes.client.render.GuiParticleSystem;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.texture.AnimatedIcon;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.StopWatch;

public final class IntroOverlay {
    private static final float ICON_SIZE = 80.0F;
    private static final float TITLE_FONT_SIZE = 15.0F;
    private static final float HINT_FONT_SIZE = 9.0F;
    private static final long PLAY_DURATION_MS = 1400L;
    private static final long HOLD_DURATION_MS = 2200L;
    private static final float FLY_DURATION = 0.9F;
    private static final float OUTRO_DURATION = 0.45F;
    private static final float PARTICLES_PER_SECOND = 100.0F;
    private static final long MIN_PROMPT_MS = 300L;
    private final GuiParticleSystem particles = new GuiParticleSystem();
    private final Animation promptAnimation = new Animation();
    private final Animation vanishAnimation = new Animation();
    private final Animation iconScaleAnimation = new Animation();
    private final Animation blurAnimation = new Animation();
    private final Animation iconProgressAnimation = new Animation();
    private final Animation flyAnimation = new Animation();
    private final StopWatch stageTimer = new StopWatch();
    private float particleAccumulator;
    private IntroOverlay.Stage stage = IntroOverlay.Stage.IDLE;
    private String welcomeText = "";
    private String continueText = "";

    public boolean isActive() {
        return this.stage != IntroOverlay.Stage.IDLE;
    }

    public void start() {
        this.stage = IntroOverlay.Stage.PROMPT;
        this.welcomeText = Language.translate("intro.welcome", "Welcome to Sadnes");
        this.continueText = Language.translate("intro.continue", "Press to continue");
        this.particles.clear();
        this.stageTimer.reset();
        this.particleAccumulator = 0.0F;
        this.promptAnimation.forceValue(0.0);
        this.promptAnimation.animate(1.0, 0.5, Easings.QUART_OUT);
        this.vanishAnimation.forceValue(0.0);
        this.iconScaleAnimation.forceValue(0.0);
        this.blurAnimation.forceValue(0.0);
        this.blurAnimation.animate(1.0, 0.4, Easings.QUAD_OUT);
        this.iconProgressAnimation.forceValue(0.0);
        this.flyAnimation.forceValue(0.0);
    }

    public void stop() {
        this.stage = IntroOverlay.Stage.IDLE;
        this.particles.clear();
    }

    public void skip() {
        if (this.stage == IntroOverlay.Stage.PROMPT) {
            this.beginVanish();
        } else {
            this.stop();
        }
    }

    public void tryContinue() {
        if (this.stage == IntroOverlay.Stage.PROMPT) {
            if (this.stageTimer.hasPassed(300.0)) {
                this.beginVanish();
                return;
            }
        } else {
            this.stop();
        }
    }

    private void beginVanish() {
        this.stage = IntroOverlay.Stage.VANISH;
        this.stageTimer.reset();
        this.vanishAnimation.animate(1.0, 0.35, Easings.QUAD_IN);
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        if (this.stage != IntroOverlay.Stage.IDLE) {
            float f9 = f + f3 / 2.0F;
            float f10 = f2 + f4 / 2.0F;
            float f11 = this.flyAnimation.get();
            float f12 = lerp(f9, f5, f11);
            float f13 = lerp(f10, f6, f11);
            float f14 = lerp(80.0F, f7, f11) * (0.55F + 0.45F * this.iconScaleAnimation.get());
            this.updateStage(f12, f13);
            if (!(f8 <= 0.004F)) {
                new RenderUtil(drawContext, f, f2, f3, f4).blur(this.blurAnimation.get() * f8, Radius.smoothAll(20.0F), RenderUtil.BlurQuality.LOW, true);
                this.renderPrompt(drawContext, f9, f10, f8);
                this.particles.render(drawContext, f8);
                float f15 = f8 * Math.min(1.0F, this.iconScaleAnimation.get() * 1.4F);
                if (f15 > 0.004F) {
                    this.renderIcon(drawContext, f12, f13, f14, f15);
                }
            }
        }
    }

    private void updateStage(float f, float f2) {
        this.promptAnimation.update();
        this.vanishAnimation.update();
        this.iconScaleAnimation.update();
        this.blurAnimation.update();
        this.iconProgressAnimation.update();
        this.flyAnimation.update();
        switch (this.stage) {
            case VANISH:
                if (this.vanishAnimation.isFinished()) {
                    this.stage = IntroOverlay.Stage.PLAY;
                    this.stageTimer.reset();
                    this.iconScaleAnimation.animate(1.0, 0.5, Easings.BACK_OUT);
                    this.iconProgressAnimation.animate(1.0, 1.4, Easings.QUART_OUT);
                }
                break;
            case PLAY:
                if (this.iconProgressAnimation.isFinished() && this.stageTimer.hasPassed(1400.0)) {
                    this.stage = IntroOverlay.Stage.HOLD;
                    this.stageTimer.reset();
                }
                break;
            case HOLD:
                if (this.stageTimer.hasPassed(2200.0)) {
                    this.stage = IntroOverlay.Stage.FLY;
                    this.stageTimer.reset();
                    this.flyAnimation.animate(1.0, 0.9F, Easings.EXPO_OUT);
                }
                break;
            case FLY:
                if (this.flyAnimation.isFinished()) {
                    this.stage = IntroOverlay.Stage.OUTRO;
                    this.stageTimer.reset();
                    this.blurAnimation.animate(0.0, 0.45F, Easings.QUAD_IN);
                }
                break;
            case OUTRO:
                if (this.blurAnimation.isFinished() && this.particles.isEmpty()) {
                    this.stage = IntroOverlay.Stage.IDLE;
                }
        }

        if (this.stage == IntroOverlay.Stage.HOLD || this.stage == IntroOverlay.Stage.FLY) {
            this.particleAccumulator = this.particleAccumulator + 100.0F * Math.min(0.05F, MathUtil.getFrameTime());

            while (this.particleAccumulator >= 1.0F) {
                this.particleAccumulator--;
                this.spawnParticle(f, f2);
            }
        }

        this.particles.update();
    }

    private void renderPrompt(DrawContext drawContext, float f, float f2, float f3) {
        float f4 = f3 * this.promptAnimation.get() * (1.0F - this.vanishAnimation.get());
        if (!(f4 <= 0.004F)) {
            MsdfFont msdfFont = Fonts.INTER_SEMIBOLD;
            MsdfFont msdfFont2 = Fonts.INTER_MEDIUM;
            if (msdfFont != null && msdfFont2 != null) {
                float f5 = msdfFont.getWidth(this.welcomeText, 15.0F);
                float f6 = msdfFont2.getWidth(this.continueText, 9.0F);
                float f7 = -8.0F * this.vanishAnimation.get();
                float f8 = f2 - (15.0F + 8.0F + 9.0F) / 2.0F + f7;
                msdfFont.draw(drawContext, this.welcomeText, f - f5 / 2.0F, f8, Theme.withAlpha(Theme.getTextColor(), f4), 15.0F);
                float f9 = 0.5F + 0.5F * (float)Math.sin(this.stageTimer.getElapsed() / 320.0);
                float f10 = f4 * (0.35F + 0.65F * f9);
                msdfFont2.draw(drawContext, this.continueText, f - f6 / 2.0F, f8 + 15.0F + 8.0F, Theme.withAlpha(Theme.getSecondaryText(), f10), 9.0F);
            }
        }
    }

    private void spawnParticle(float f, float f2) {
        float f3 = f + MathUtil.randomBetween(-2.5F, 2.5F);
        float f4 = f2 + MathUtil.randomBetween(-2.5F, 2.5F);
        this.particles
            .spawn(
                1,
                () -> GuiParticle.builder()
                    .position(f3, f4)
                    .randomVelocity(25.0F, 100.0F)
                    .size(MathUtil.randomBetween(1.6F, 3.2F))
                    .lifetime((long)MathUtil.randomBetween(900.0F, 1500.0F))
                    .fadeIn(320L)
                    .fadeOut(520L)
                    .gravity(16.0F)
                    .friction(0.95F)
                    .texture(Texture.BLOOM)
                    .color(Theme.tintWhite(MathUtil.randomBetween(0.25F, 0.92F)))
                    .build()
            );
    }

    private void renderIcon(DrawContext drawContext, float f, float f2, float f3, float f4) {
        int i = Theme.withAlpha(Theme.getHighlight(), f4);
        new RenderUtil(drawContext, f - f3 / 2.0F, f2 - f3 / 2.0F, f3, f3)
            .spriteAtProgress(AnimatedIcon.MAIN, -f3 / 2.0F, -f3 / 2.0F, f3 * 2.0F, f3 * 2.0F, i, this.iconProgressAnimation.get());
    }

    private static float lerp(float f, float f2, float f3) {
        return f + (f2 - f) * f3;
    }

    enum Stage {
        IDLE,
        PROMPT,
        VANISH,
        PLAY,
        HOLD,
        FLY,
        OUTRO;
    }
}
