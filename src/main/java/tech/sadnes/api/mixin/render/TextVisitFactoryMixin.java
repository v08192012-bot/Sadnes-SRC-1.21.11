package tech.sadnes.api.mixin.render;

import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.impl.TextEvent;
import tech.sadnes.client.feature.impl.misc.StreamerMode;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
    @ModifyArg(
        method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
            ordinal = 0
        ),
        index = 0
    )
    private static String sadnes$adjustText(String string) {
        TextEvent textEvent = new TextEvent(string);
        Sadnes.getEventBus().post(textEvent);
        return textEvent.getText();
    }

    @Inject(method = "visitFormatted(Lnet/minecraft/text/StringVisitable;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", at = @At("HEAD"), cancellable = true)
    private static void sadnes$replaceBrand(StringVisitable stringVisitable, Style style, CharacterVisitor characterVisitor, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        Boolean flag = StreamerMode.visitReplacedStyled(stringVisitable, style, characterVisitor);
        if (flag != null) {
            callbackInfoReturnable.setReturnValue(flag);
        }
    }
}
