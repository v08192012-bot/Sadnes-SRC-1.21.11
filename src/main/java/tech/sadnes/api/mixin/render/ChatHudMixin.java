package tech.sadnes.api.mixin.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.hud.ChatHud.Backend;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.api.mixin.accessor.ChatHudInteractableAccessor;
import tech.sadnes.client.animation.HudAnimations;
import tech.sadnes.client.feature.impl.misc.ChatHelper;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @ModifyConstant(method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V", constant = @Constant(intValue = 100))
    private int sadnes$messageLimit(int i) {
        return ChatHelper.isInfiniteEnabled() ? Integer.MAX_VALUE : i;
    }

    @ModifyConstant(method = "addVisibleMessage", constant = @Constant(intValue = 100))
    private int sadnes$visibleLimit(int i) {
        return ChatHelper.isInfiniteEnabled() ? Integer.MAX_VALUE : i;
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("TAIL"))
    private void sadnes$startChatAnimation(Text text, MessageSignatureData messageSignatureData, MessageIndicator messageIndicator, CallbackInfo callbackInfo) {
        HudAnimations.INSTANCE.onChatMessage();
    }

    @Inject(
        method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;updatePose(Ljava/util/function/Consumer;)V", shift = Shift.AFTER)
    )
    private void sadnes$offsetChatLines(Backend backend, int i, int i2, boolean flag, CallbackInfo callbackInfo) {
        float f = HudAnimations.INSTANCE.getChatMessageOffset();
        if (f != 0.0F) {
            backend.updatePose(matrix3x2f -> matrix3x2f.translate(0.0F, f));
        }
    }

    @Redirect(method = "method_75802", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;fill(IIIII)V"))
    private static void sadnes$lineBackground(
        Backend backend,
        int i,
        int i2,
        int i3,
        int i4,
        int i5,
        int i6,
        int i7,
        Backend backend2,
        int i8,
        float f,
        Visible visible,
        int i9,
        float f2
    ) {
        int i10 = i3;
        if (ChatHelper.isCompactEnabled()) {
            i10 = Math.min(i3, visible.getWidth(MinecraftClient.getInstance().textRenderer) + 4);
        }

        if (backend instanceof ChatHudInteractableAccessor chatHudInteractableAccessor) {
            Vector2f vector2f = chatHudInteractableAccessor.sadnes$getUntransformedOffset();
            boolean flag = vector2f.x >= i && vector2f.x <= i10 && vector2f.y >= i2 && vector2f.y <= i4;
            if (flag) {
                ChatHelper.setHoveredLine(i9);
            }
        }

        if (ChatHelper.isMentionsEnabled() && ChatHelper.isMention(visible.content(), i9)) {
            int i11 = ColorUtil.setAlpha(Theme.getPrimary(), ColorUtil.alpha(i5));
            int i12 = Math.min(i10, i + 60);

            for (int i13 = i; i13 < i12; i13 += 4) {
                float f3 = (float)(i13 - i) / (i12 - i);
                backend.fill(i13, i2, Math.min(i13 + 4, i12), i4, ColorUtil.interpolate(i11, i5, f3));
            }

            if (i12 < i10) {
                backend.fill(i12, i2, i10, i4, i5);
            }
        } else {
            backend.fill(i, i2, i10, i4, i5);
        }
    }
}
