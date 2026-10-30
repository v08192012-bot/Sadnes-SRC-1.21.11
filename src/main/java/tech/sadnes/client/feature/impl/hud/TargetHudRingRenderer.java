package tech.sadnes.client.feature.impl.hud;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.impl.misc.ClientSpoof;
import tech.sadnes.client.render.GuiParticle;
import tech.sadnes.client.render.GuiParticleSystem;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.StopWatch;

final class TargetHudRingRenderer {
    static final float WIDTH = 114.0F;
    static final float HEIGHT = 28.0F;
    private static final float INNER_PADDING = 8.0F;
    private static final float AVATAR_SIZE = 20.0F;
    private static final float RING_SIZE = 18.0F;
    private static final float PADDING = 4.0F;
    private static final float MARGIN = 5.0F;
    private static final float RING_THICKNESS = 1.75F;
    private static final float TEXT_GAP = 5.0F;
    private static final float FONT_SIZE = 6.5F;
    private static final float LINE_GAP = 2.5F;
    private static final float ITEM_SIZE = 8.0F;
    private static final float SLOT_RADIUS = 1.5F;
    private static final float NAME_OFFSET_Y = 5.5F;
    private static final float EMPTY_ICON_SIZE = 3.5F;
    private static final float SLOT_INSET = 0.5F;
    private static final float HEALTH_FONT_SIZE = 6.5F;
    private static final float CENTER_OFFSET_Y = 12.0F;
    private static final EquipmentSlot[] ALL_SLOTS = new EquipmentSlot[]{
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };
    private static final EquipmentSlot[] HAND_SLOTS = new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    private static final float AVATAR_RADIUS = 8.0F;
    private static final Radius AVATAR_ROUNDING = Radius.all(8.0F);
    private static final Radius SLOT_ROUNDING = Radius.all(1.5F);
    private static final long RESET_TIMEOUT_MS = 300L;
    private static final long DAMAGE_PARTICLE_COOLDOWN_MS = 500L;
    private static final long BOUNCE_COOLDOWN_MS = 250L;
    private final BooleanSupplier particlesEnabled;
    private final BooleanSupplier showArmor;
    private final TargetUsingItemIndicator avatarOverlay;
    private final GuiParticleSystem particleSystem = new GuiParticleSystem();
    private final Animation healthAnimation = new Animation();
    private final Animation trailAnimation = new Animation();
    private final Animation absorptionAnimation = new Animation();
    private final Animation hitFlashAnimation = new Animation();
    private final StopWatch lastRenderTimer = new StopWatch();
    private final StopWatch damageParticleTimer = new StopWatch();
    private final StopWatch bounceTimer = new StopWatch();
    private LivingEntity lastTarget;
    private float lastHealth;
    private float lastAbsorption;
    private boolean trailActive;
    private float particleAccumulator;
    private float ringCenterX;
    private float ringCenterY;
    private int ringColor;
    private int lastDisplayedHealth = -1;

    void render(DrawContext drawContext, LivingEntity livingEntity, float f, float f2, float f3, float f4, float f5, float f6) {
        float f7 = livingEntity.isAlive() ? f3 / f4 : 0.0F;
        float f8 = f5 > 0.01F && !ServerManager.isFunTime() ? f5 / f4 : 0.0F;
        float f9 = f2 + 14.0F;
        float f10 = f + 4.0F + 10.0F;
        this.ringCenterX = f + 114.0F - 5.0F - 9.0F;
        this.ringCenterY = f9;
        float f11 = MathHelper.clamp((0.5F - Math.max(this.healthAnimation.get(), f7)) / 0.25F, 0.0F, 1.0F);
        int i = ColorUtil.interpolate(Theme.getPrimary(), TargetHud.LOW_HEALTH_COLOR, f11);
        this.ringColor = i;
        this.updateHealth(livingEntity, f7, f8, i);
        HudPanelUtil.drawPanel(drawContext, f, f2, 114.0F, 28.0F, f6, 8.0F);
        this.renderRing(drawContext, i, f6);
        this.renderAvatar(drawContext, livingEntity, f10, f9, f6);
        this.avatarOverlay.render(drawContext, livingEntity, f10 + 9.2F, f9 + 9.2F, f6);
        int i2 = livingEntity.isAlive() ? Math.max(1, Math.round(f3)) : 0;
        if (i2 != this.lastDisplayedHealth) {
            if (this.bounceTimer.isElapsed(250L)) {
                this.bounceTimer.reset();
            }

            this.lastDisplayedHealth = i2;
        }

        float f12 = (float)this.bounceTimer.getElapsed() / 1000.0F;
        float f13 = f12 < 1.2F ? (float)(Math.exp(-7.0 * f12) * Math.cos(18.0 * f12)) : 0.0F;
        String string = Integer.toString(i2);
        float f14 = Math.min(6.5F, 78.0F / Fonts.INTER_SEMIBOLD.getWidth(string, 6.5F));
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, this.ringCenterX, this.ringCenterY, 1.0F + 0.35F * f13, 1.0F - 0.28F * f13);
        new RenderUtil(drawContext, this.ringCenterX - 9.0F, this.ringCenterY - 9.0F, 18.0F, 18.0F)
            .textCenter(Fonts.INTER_SEMIBOLD, string, ColorUtil.setAlphaf(Theme.getTextColor(), f6), f14);
        MatrixUtil.pop(drawContext);
        float f15 = f10 + 10.0F + 5.0F;
        float f16 = this.ringCenterX - 9.0F - 5.0F - f15;
        String string2 = ClientSpoof.replaceNames(livingEntity.getName()).getString();
        new RenderUtil(drawContext, f15, f2, f16, 28.0F)
            .scrollingTextTop(Fonts.INTER_SEMIBOLD, string2, 0.0F, 5.5F, f16, ColorUtil.setAlphaf(Theme.getTextColor(), f6), 6.5F);
        this.renderEquipment(drawContext, livingEntity, f15, f2 + 5.5F + 6.5F + 2.5F, f6);
        this.renderParticles(drawContext);
    }

    void onHit() {
        this.hitFlashAnimation.forceValue(1.0);
        this.hitFlashAnimation.animate(0.0, 0.5, Easings.QUART_OUT);
        this.damageParticleTimer.reset();
        this.bounceTimer.reset();
        float f = MathHelper.clamp(this.healthAnimation.get(), 0.0F, 1.0F);
        if (this.particlesEnabled.getAsBoolean() && !(f <= 0.0F)) {
            float f2 = Math.min(f, 0.06F);
            int i = this.ringColor;
            this.particleSystem
                .spawn(
                    12,
                    () -> this.createRingParticle(
                        f - MathUtil.randomBetween(0.0F, f2),
                        ColorUtil.interpolate(i, Theme.getWhite(), MathUtil.randomBetween(0.1F, 0.6F)),
                        MathUtil.randomBetween(20.0F, 50.0F),
                        MathUtil.randomBetween(-14.0F, 14.0F),
                        (long)MathUtil.randomBetween(500.0F, 900.0F)
                    )
                );
        }
    }

    void renderParticles(DrawContext drawContext) {
        if (!this.particleSystem.isEmpty()) {
            this.particleSystem.applyForce(this.ringCenterX, this.ringCenterY);
            this.particleSystem.update();
            this.particleSystem.render(drawContext, 1.0F);
        }
    }

    private void updateHealth(LivingEntity livingEntity, float f, float f2, int i) {
        boolean flag = this.particlesEnabled.getAsBoolean();
        boolean flag2 = this.lastRenderTimer.isElapsed(300L);
        this.lastRenderTimer.reset();
        if (flag2) {
            this.healthAnimation.forceValue(0.0);
            this.absorptionAnimation.forceValue(0.0);
            this.bounceTimer.reset();
        }

        if (flag2 || livingEntity != this.lastTarget) {
            this.lastTarget = livingEntity;
            this.lastHealth = f;
            this.lastAbsorption = f2;
            this.trailActive = false;
        }

        if (f < this.lastHealth - 5.0E-4F) {
            boolean flag3 = f <= 0.0F;
            if (flag && (flag3 || this.damageParticleTimer.isElapsed(500L))) {
                this.spawnDamageParticles(this.lastHealth, f, i, flag3);
            }

            if (!this.trailActive) {
                this.trailAnimation.forceValue(this.lastHealth);
            }

            this.trailActive = true;
        }

        if (flag && f2 < this.lastAbsorption - 5.0E-4F) {
            this.spawnDamageParticles(1.0F - f2, 1.0F - this.lastAbsorption, TargetHud.ABSORPTION_COLOR, false);
        }

        this.lastHealth = f;
        this.lastAbsorption = f2;
        this.healthAnimation.update();
        this.healthAnimation.animate(f, 0.45, Easings.EXPO_OUT);
        this.absorptionAnimation.update();
        this.absorptionAnimation.animate(f2, 0.45, Easings.EXPO_OUT);
        this.hitFlashAnimation.update();
        if (this.trailActive) {
            float f3 = this.trailAnimation.get();
            this.trailAnimation.update();
            this.trailAnimation.animate(f, 0.35 + (this.trailAnimation.get() - f) * 0.9, Easings.CUBIC_OUT);
            float f4 = this.trailAnimation.get();
            if (f4 <= f + 5.0E-4F) {
                this.trailActive = false;
            } else {
                float f5 = f3 - f4;
                if (flag && !(f5 <= 0.0F)) {
                    this.particleAccumulator = Math.min(this.particleAccumulator + f5 * 120.0F, 6.0F);
                    int i2 = (int)this.particleAccumulator;
                    this.particleAccumulator -= i2;
                    int i3 = ColorUtil.interpolate(i, Theme.getWhite(), 0.35F);
                    this.particleSystem
                        .spawn(
                            i2,
                            () -> this.createRingParticle(
                                f4,
                                i3,
                                MathUtil.randomBetween(8.0F, 24.0F),
                                MathUtil.randomBetween(-12.0F, 2.0F),
                                (long)MathUtil.randomBetween(400.0F, 800.0F)
                            )
                        );
                }
            }
        }
    }

    private void spawnDamageParticles(float f, float f2, int i, boolean flag) {
        float f3 = f - f2;
        int i2 = Math.min(flag ? 32 : 18, 3 + Math.round(f3 * (flag ? 64.0F : 40.0F)));
        float f4 = flag ? 75.0F : 48.0F;
        float f5 = flag ? 1300.0F : 950.0F;
        this.particleSystem
            .spawn(
                i2,
                () -> this.createRingParticle(
                    MathUtil.randomBetween(f2, f),
                    ColorUtil.interpolate(i, Theme.getWhite(), MathUtil.randomBetween(0.1F, 0.6F)),
                    MathUtil.randomBetween(18.0F, f4),
                    MathUtil.randomBetween(-14.0F, 14.0F),
                    (long)MathUtil.randomBetween(500.0F, f5)
                )
            );
    }

    private GuiParticle createRingParticle(float f, int i, float f2, float f3, long l) {
        double d = f * Math.PI * 2.0;
        float f4 = (float)Math.sin(d);
        float f5 = (float)(-Math.cos(d));
        float f6 = 8.125F + MathUtil.randomBetween(-0.6F, 0.6F);
        return GuiParticle.builder()
            .texture(Texture.BLOOM)
            .color(i)
            .position(this.ringCenterX, this.ringCenterY)
            .offsetX(f4 * f6)
            .offsetY(f5 * f6)
            .motionX(f4 * f2 - f5 * f3)
            .motionY(f5 * f2 + f4 * f3)
            .size(MathUtil.randomBetween(0.8F, 2.0F))
            .lifetime(l)
            .fadeIn(40L)
            .fadeOut(l * 2L / 3L)
            .gravity(45.0F)
            .friction(1.4F)
            .build();
    }

    private void renderRing(DrawContext drawContext, int i, float f) {
        float f2 = this.ringCenterX - 9.0F;
        float f3 = this.ringCenterY - 9.0F;
        RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, 18.0F, 18.0F).circle(1.75F, ColorUtil.setAlphaf(Theme.getSecondaryText(), f * 0.2F));
        float f4 = this.hitFlashAnimation.get();
        float f5 = 1.75F + 0.7F * f4;
        float f6 = 360.0F * MathHelper.clamp(this.healthAnimation.get(), 0.0F, 1.0F);
        float f7 = this.trailActive ? 360.0F * MathHelper.clamp(this.trailAnimation.get(), 0.0F, 1.0F) : 0.0F;
        if (f7 - f6 > 0.5F) {
            renderUtil.arc(0.0F, f7, f5, ColorUtil.setAlphaf(ColorUtil.interpolate(i, Theme.getWhite(), 0.5F), f * 0.55F));
        }

        if (f6 > 0.5F) {
            renderUtil.arc(0.0F, f6, f5, ColorUtil.interpolate(i, Theme.getWhite(), 0.45F * f4), f);
        }

        float f8 = 360.0F * MathHelper.clamp(this.absorptionAnimation.get(), 0.0F, 1.0F);
        if (f8 > 0.5F) {
            renderUtil.arc(360.0F - f8, f8, f5, TargetHud.ABSORPTION_COLOR, f);
        }
    }

    private void renderAvatar(DrawContext drawContext, LivingEntity livingEntity, float f, float f2, float f3) {
        float f4 = f - 10.0F;
        float f5 = f2 - 10.0F;
        TextureSetup textureSetup = TargetHud.getSkinTextureSetup(livingEntity);
        if (textureSetup != null) {
            TargetHud.drawPlayerFace(drawContext, textureSetup, f4, f5, 20.0F, 8.0F, f3);
        } else {
            new RenderUtil(drawContext, f4, f5, 20.0F, 20.0F)
                .rect(ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getBackground(), Theme.getWhite(), 0.15F), f3 * 0.6F), AVATAR_ROUNDING)
                .icon(Icon.USER, 5.0F, 5.0F, 10.0F, ColorUtil.setAlphaf(Theme.getSecondaryText(), f3));
        }
    }

    private void renderEquipment(DrawContext drawContext, LivingEntity livingEntity, float f, float f2, float f3) {
        float f4 = MathUtil.roundToStep(f, 0.5F);
        float f5 = MathUtil.roundToStep(f2, 0.5F);
        int i = ColorUtil.setAlphaf(Theme.getSecondaryText(), f3 * 0.45F);
        int i2 = ColorUtil.setAlphaf(Theme.getSecondaryText(), f3 * 0.07F);

        for (EquipmentSlot equipmentSlot : this.showArmor.getAsBoolean() ? ALL_SLOTS : HAND_SLOTS) {
            ItemStack itemStack = livingEntity.getEquippedStack(equipmentSlot);
            if (itemStack.isEmpty()) {
                new RenderUtil(drawContext, f4 + 0.5F, f5 + 0.5F, 7.0F, 7.0F).shadow(0.5F, i2, SLOT_ROUNDING);
                new RenderUtil(drawContext, f4 + 2.25F, f5 + 2.25F, 3.5F, 3.5F).icon(Icon.CLOSE, 0.0F, 0.0F, 3.5F, i);
            } else {
                TargetHud.drawItemStack(drawContext, itemStack, f4, f5, 8.0F, f3);
            }

            f4 += 9.5F;
        }
    }

    public TargetHudRingRenderer(BooleanSupplier booleanSupplier, BooleanSupplier booleanSupplier2, TargetUsingItemIndicator targetUsingItemIndicator) {
        this.particlesEnabled = booleanSupplier;
        this.showArmor = booleanSupplier2;
        this.avatarOverlay = targetUsingItemIndicator;
    }
}
