package tech.sadnes.api.mixin.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.AnimatedIcon;
import tech.sadnes.client.render.texture.AnimatedIconManager;
import tech.sadnes.client.render.texture.MenuBackgroundTexture;
import tech.sadnes.client.ui.menu.MenuBackgroundRenderer;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow
    @Final
    private boolean reloading;
    @Shadow
    private long reloadStartTime;
    @Shadow
    private long reloadCompleteTime;
    private static final int BOOT_BG = -15398388;
    private static final long FADE_OUT_MS = 650L;
    private static final long LOGO_IN_MS = 480L;
    @Unique
    private static boolean sadnes$pipelinesWarm = false;
    @Unique
    private long sadnes$startedAt = -1L;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void sadnes$renderBranded(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        callbackInfo.cancel();
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        long l = Util.getMeasuringTimeMs();
        if (this.reloading && this.reloadStartTime == -1L) {
            this.reloadStartTime = l;
        }

        float f2 = 1.0F;
        if (this.reloadCompleteTime > -1L) {
            float f3 = clamp01((float)(l - this.reloadCompleteTime) / 650.0F);
            f2 = 1.0F - smoothstep(f3);
            if (f2 <= 0.0F) {
                if (minecraftClient.currentScreen != null) {
                    minecraftClient.currentScreen.renderWithTooltip(drawContext, i, i2, f);
                }

                minecraftClient.setOverlay(null);
                sadnes$pipelinesWarm = true;
                return;
            }
        }

        if (this.sadnes$startedAt < 0L) {
            this.sadnes$startedAt = System.currentTimeMillis();
        }

        if (sadnes$pipelinesWarm) {
            if (this.reloading) {
                this.sadnes$renderRichBrand(drawContext, f2);
            } else {
                this.sadnes$renderRichBrand(drawContext, 0.0F);
            }
        } else {
            this.sadnes$renderSafeBrand(drawContext, minecraftClient);
        }
    }

    @Unique
    private void sadnes$renderRichBrand(DrawContext drawContext, float f) {
        Sadnes.getInstance().saveAll();
        if (Sadnes.getInstance().isShuttingDown()) {
            GuiScale.push(drawContext);

            try {
                MenuBackgroundRenderer.renderBackground(drawContext, 1.0F);
                if (f > 0.004F) {
                    this.sadnes$renderReloadLogo(drawContext, f);
                }
            } finally {
                GuiScale.pop(drawContext);
            }
        }
    }

    @Unique
    private void sadnes$renderReloadLogo(DrawContext drawContext, float f) {
        float f2 = GuiScale.getScaledWidth();
        float f3 = GuiScale.getScaledHeight();
        long l = System.currentTimeMillis();
        long l2 = Math.max(0L, l - this.sadnes$startedAt);
        float f4 = clamp01((float)l2 / 480.0F);
        float f5 = 1.0F + 2.2F * (float)Math.pow(f4 - 1.0F, 3.0) + 1.2F * (float)Math.pow(f4 - 1.0F, 2.0);
        float f6 = 1.0F + 0.022F * (float)Math.sin((float)(l % 2600L) / 2600.0F * Math.PI * 2.0);
        float f7 = (0.6F + 0.4F * f5) * f6;
        float f8 = f * smoothstep(clamp01((float)l2 / 336.0F));
        float f9 = Math.min(f2, f3) * 0.22F;
        float f10 = f2 / 2.0F;
        float f11 = f3 / 2.0F;
        int i = Theme.getPrimary();
        int i2 = ColorUtil.interpolate(i, ColorUtil.WHITE, 0.45F);
        int i3 = ColorUtil.darken(i, 0.7F);
        int i4 = ColorUtil.interpolate(i, ColorUtil.WHITE, 0.85F);
        float f12 = (float)(l % 3200L) / 3200.0F * 1.6F - 0.3F;
        float f13 = Math.max(0.0F, 1.0F - Math.abs(f12) / 0.55F);
        float f14 = Math.max(0.0F, 1.0F - Math.abs(1.0F - f12) / 0.55F);
        int i5 = Theme.withAlpha(ColorUtil.interpolate(i2, i4, f13 * f13), f8);
        int i6 = Theme.withAlpha(ColorUtil.interpolate(i3, i4, f14 * f14), f8);
        float f15 = f9 * f7;
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f10, f11, f7, f7);
        new RenderUtil(drawContext, f10 - f9 / 2.0F, f11 - f9 / 2.0F, f9, f9).spinner(0.0F, 0.0F, f9, f9, i5, i6, 0.0F, 0.0F, 0.0F);
        MatrixUtil.pop(drawContext);
        if (Fonts.INTER_MEDIUM != null) {
            float f16 = 0.45F + 0.25F * (float)Math.sin((float)(l % 1800L) / 1800.0F * Math.PI * 2.0);
            new RenderUtil(drawContext, 0.0F, f11 + f15 * 0.62F, f2, 9.0F)
                .textTopCenter(
                    Fonts.INTER_MEDIUM, Language.translate("splash.loading", "Загрузка ресурсов…"), Theme.withAlpha(Theme.getTextColor(), f16 * f8), 7.0F
                );
        }
    }

    @Unique
    private void sadnes$renderSafeBrand(DrawContext drawContext, MinecraftClient minecraftClient) {
        int i = minecraftClient.getWindow().getScaledWidth();
        int i2 = minecraftClient.getWindow().getScaledHeight();
        Identifier identifier = MenuBackgroundTexture.getTexture();
        if (identifier != null) {
            int i3 = MenuBackgroundTexture.getWidth();
            int i4 = MenuBackgroundTexture.getHeight();
            float f = (float)i3 / i4;
            float f2;
            float f3;
            if ((float)i / i2 > f) {
                f2 = i;
                f3 = i / f;
            } else {
                f3 = i2;
                f2 = i2 * f;
            }

            int i5 = (int)((i - f2) / 2.0F);
            int i6 = (int)((i2 - f3) / 2.0F);
            drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, identifier, i5, i6, 0.0F, 0.0F, (int)f2, (int)f3, i3, i4, i3, i4, ColorUtil.WHITE);
        } else {
            drawContext.fill(0, 0, i, i2, -15398388);
        }

        Identifier identifier2 = AnimatedIconManager.getTexture(AnimatedIcon.BG);
        if (identifier2 != null) {
            int i7 = AnimatedIcon.BG.getColumns();
            int i8 = AnimatedIcon.BG.getRows();
            int i9 = AnimatedIconManager.getCurrentFrame(AnimatedIcon.BG);
            int i10 = i9 % i7;
            int i11 = i9 / i7;
            float f4;
            float f5;
            if ((float)i / i2 > 1.7777778F) {
                f4 = i;
                f5 = i / 1.7777778F;
            } else {
                f5 = i2;
                f4 = i2 * 1.7777778F;
            }

            int i12 = (int)((i - f4) / 2.0F);
            int i13 = (int)((i2 - f5) / 2.0F);
            drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, identifier2, i12, i13, i10, i11, (int)f4, (int)f5, 1, 1, i7, i8, ColorUtil.WHITE);
        }

        drawContext.fill(0, 0, i, i2, Theme.withAlpha(ColorUtil.BLACK, 0.45F));
    }

    @Unique
    private static float clamp01(float f) {
        return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
    }

    @Unique
    private static float smoothstep(float f) {
        return f * f * (3.0F - 2.0F * f);
    }
}
