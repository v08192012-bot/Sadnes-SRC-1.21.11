package tech.sadnes.api.mixin.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.HudAnimations;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.feature.impl.misc.ChatHelper;
import tech.sadnes.client.party.PartyCommandCompletion;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Unique
    private long sadnes$openedAt;
    @Unique
    private boolean sadnes$shifted;

    @Inject(method = "render", at = @At("HEAD"))
    private void sadnes$pushOpenAnimation(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        if (this.sadnes$openedAt == 0L) {
            this.sadnes$openedAt = System.currentTimeMillis();
        }

        float f2 = HudAnimations.INSTANCE.getChatOpenOffset(this.sadnes$openedAt);
        if (f2 != 0.0F) {
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(0.0F, f2);
            this.sadnes$shifted = true;
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void sadnes$popOpenAnimation(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        if (this.sadnes$shifted) {
            this.sadnes$shifted = false;
            drawContext.getMatrices().popMatrix();
        }
    }

    @ModifyVariable(method = "sendMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String sadnes$resolvePartyLogins(String string) {
        return PartyCommandCompletion.resolveMemberName(string);
    }

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void sadnes$executeCommand(String string, boolean flag, CallbackInfo callbackInfo) {
        CommandManager commandManager = Sadnes.getInstance().getCommandManager();
        if (commandManager != null && commandManager.handleMessage(string.trim())) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void sadnes$copyMessage(Click click, boolean flag, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (ChatHelper.isCopyEnabled() && ChatHelper.isDoubleClick()) {
            String string = ChatHelper.getHoveredLineText();
            if (string != null) {
                MinecraftClient.getInstance().keyboard.setClipboard(string);
                callbackInfoReturnable.setReturnValue(true);
            }
        }
    }
}
