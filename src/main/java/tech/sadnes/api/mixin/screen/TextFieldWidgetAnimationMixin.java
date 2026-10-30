package tech.sadnes.api.mixin.screen;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.animation.HudAnimations;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetAnimationMixin {
    @Shadow
    private String text;
    @Shadow
    private int firstCharacterIndex;
    @Shadow
    private int selectionStart;
    @Unique
    private long sadnes$typedAt;
    @Unique
    private int sadnes$typedCursor = -1;
    @Unique
    private int sadnes$typedLength = -1;
    @Unique
    private int sadnes$lengthBeforeWrite;

    @Shadow
    private OrderedText format(String string, int i) {
        throw new AssertionError();
    }

    @Inject(method = "write", at = @At("HEAD"))
    private void sadnes$beforeWrite(String string, CallbackInfo callbackInfo) {
        this.sadnes$lengthBeforeWrite = this.text.length();
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void sadnes$markTyped(String string, CallbackInfo callbackInfo) {
        if (this.text.length() == this.sadnes$lengthBeforeWrite + 1) {
            this.sadnes$typedAt = System.currentTimeMillis();
            this.sadnes$typedCursor = this.selectionStart;
            this.sadnes$typedLength = this.text.length();
        }
    }

    @Redirect(
        method = "renderWidget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V",
            ordinal = 0
        )
    )
    private void sadnes$revealTypedChar(
        DrawContext drawContext, TextRenderer textRenderer, OrderedText orderedText, int i, int i2, int i3, boolean flag, @Local(ordinal = 1) String string
    ) {
        boolean flag2 = this.selectionStart == this.sadnes$typedCursor && this.text.length() == this.sadnes$typedLength;
        boolean flag3 = this.selectionStart - this.firstCharacterIndex == string.length();
        float f = flag2 && flag3 ? HudAnimations.INSTANCE.getTypingProgress(this.sadnes$typedAt) : 1.0F;
        if (!(f >= 1.0F) && !string.isEmpty()) {
            int i4 = string.offsetByCodePoints(string.length(), -1);
            int i5 = i;
            if (i4 > 0) {
                OrderedText orderedText2 = this.format(string.substring(0, i4), this.firstCharacterIndex);
                drawContext.drawText(textRenderer, orderedText2, i, i2, i3, flag);
                i5 = i + textRenderer.getWidth(orderedText2);
            }

            OrderedText orderedText3 = this.format(string.substring(i4), this.firstCharacterIndex + i4);
            Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
            matrix3x2fStack.pushMatrix();
            matrix3x2fStack.translate(0.0F, (1.0F - f) * -3.0F);
            drawContext.drawText(textRenderer, orderedText3, i5, i2, ColorHelper.withAlpha(Math.max(4, (int)(f * 255.0F)), i3), flag);
            matrix3x2fStack.popMatrix();
        } else {
            drawContext.drawText(textRenderer, orderedText, i, i2, i3, flag);
        }
    }
}
