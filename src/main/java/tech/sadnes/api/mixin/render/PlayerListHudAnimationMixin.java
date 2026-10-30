package tech.sadnes.api.mixin.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.animation.HudAnimations;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudAnimationMixin {
    @Shadow
    private boolean visible;
    @Unique
    private boolean sadnes$transformed;

    @Inject(method = "setVisible", at = @At("HEAD"))
    private void sadnes$trackVisibility(boolean flag, CallbackInfo callbackInfo) {
        if (this.visible != flag) {
            HudAnimations.INSTANCE.setTabListVisible(flag);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void sadnes$pushAnimation(DrawContext drawContext, int i, Scoreboard scoreboard, ScoreboardObjective scoreboardObjective, CallbackInfo callbackInfo) {
        this.sadnes$transformed = HudAnimations.INSTANCE.beginTabListTransform(drawContext, i);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void sadnes$popAnimation(DrawContext drawContext, int i, Scoreboard scoreboard, ScoreboardObjective scoreboardObjective, CallbackInfo callbackInfo) {
        if (this.sadnes$transformed) {
            this.sadnes$transformed = false;
            HudAnimations.INSTANCE.endTabListTransform(drawContext);
        }
    }
}
