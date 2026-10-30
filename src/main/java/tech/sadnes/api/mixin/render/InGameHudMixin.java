package tech.sadnes.api.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Collection;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.impl.hud.Hotbar;
import tech.sadnes.client.feature.impl.hud.Watermark;
import tech.sadnes.client.feature.impl.misc.FreeCam;
import tech.sadnes.client.feature.impl.misc.StreamerMode;
import tech.sadnes.client.feature.impl.render.Crosshair;
import tech.sadnes.client.feature.impl.render.Removals;
import tech.sadnes.client.render.FoodSaturationOverlay;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.ui.RadialMenu;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Unique
    private float sadnes$hudLift;

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
    private void sadnes$removeScoreboard(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        if (Removals.isRemoved("scoreboard")) {
            callbackInfo.cancel();
        }
    }

    @Redirect(
        method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/scoreboard/Scoreboard;getScoreboardEntries(Lnet/minecraft/scoreboard/ScoreboardObjective;)Ljava/util/Collection;"
        )
    )
    private Collection<ScoreboardEntry> sadnes$filterScoreboard(Scoreboard scoreboard, ScoreboardObjective scoreboardObjective) {
        return StreamerMode.filterScoreboardEntries(scoreboard, scoreboard.getScoreboardEntries(scoreboardObjective));
    }

    @Redirect(
        method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/ScoreboardObjective;getDisplayName()Lnet/minecraft/text/Text;")
    )
    private Text sadnes$scoreboardTitle(ScoreboardObjective scoreboardObjective) {
        return StreamerMode.replaceScoreboardTitle(scoreboardObjective.getDisplayName());
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void sadnes$customHotbar(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        if (Hotbar.isActive()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void sadnes$customStatusBars(DrawContext drawContext, CallbackInfo callbackInfo) {
        if (Hotbar.isStatusShown()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderFood", at = @At("RETURN"))
    private void sadnes$renderSaturation(DrawContext drawContext, PlayerEntity playerEntity, int i, int i2, CallbackInfo callbackInfo) {
        if (FoodSaturationOverlay.shouldRender(playerEntity)) {
            FoodSaturationOverlay.render(drawContext, playerEntity, i, i2);
        }
    }

    @WrapOperation(
        method = "renderStatusBars",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/InGameHud;renderAirBubbles(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/entity/player/PlayerEntity;III)V"
        )
    )
    private void sadnes$liftAirAboveSaturation(InGameHud inGameHud, DrawContext drawContext, PlayerEntity playerEntity, int i, int i2, int i3, Operation<Void> operation) {
        boolean flag;
        if (i == 0) {
            flag = FoodSaturationOverlay.shouldRender(playerEntity);
        } else {
            flag = false;
        }

        operation.call(inGameHud, drawContext, playerEntity, i, flag ? i2 - 10 : i2, i3);
    }

    @Inject(method = "renderMountHealth", at = @At("HEAD"), cancellable = true)
    private void sadnes$customMountHealth(DrawContext drawContext, CallbackInfo callbackInfo) {
        if (Hotbar.isStatusShown()) {
            callbackInfo.cancel();
        }
    }

    @Redirect(
        method = "renderMainHud",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/bar/Bar;drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V"
        )
    )
    private void sadnes$customExperienceLevel(DrawContext drawContext, TextRenderer textRenderer, int i) {
        if (!Hotbar.isStatusShown()) {
            Bar.drawExperienceLevel(drawContext, textRenderer, i);
        }
    }

    @Inject(method = "renderMainHud", at = @At("HEAD"))
    private void sadnes$liftBottomHud(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        this.sadnes$hudLift = GuiScale.applyScale(Watermark.getBottomOffset());
        if (!(this.sadnes$hudLift <= 0.0F)) {
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(0.0F, -this.sadnes$hudLift);
        }
    }

    @Inject(method = "renderMainHud", at = @At("RETURN"))
    private void sadnes$restoreBottomHud(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        if (!(this.sadnes$hudLift <= 0.0F)) {
            drawContext.getMatrices().popMatrix();
            this.sadnes$hudLift = 0.0F;
        }
    }

    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    private void sadnes$removeNauseaOverlay(DrawContext drawContext, float f, CallbackInfo callbackInfo) {
        if (Removals.isRemoved("bad_effects")) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void sadnes$hideStatusEffectOverlay(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        callbackInfo.cancel();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void sadnes$hideCrosshairFreecam(DrawContext drawContext, RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
        FreeCam freeCam = FreeCam.getInstance();
        if (freeCam != null && freeCam.isEnabled()) {
            callbackInfo.cancel();
        } else if (RadialMenu.isAnyOpen()) {
            callbackInfo.cancel();
        } else {
            if (Crosshair.isActive()) {
                Crosshair.renderCrosshair(drawContext);
                callbackInfo.cancel();
            }
        }
    }
}
