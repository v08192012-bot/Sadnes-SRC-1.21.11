package tech.sadnes.api.mixin.text;

import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.feature.impl.misc.ClientSpoof;
import tech.sadnes.client.feature.impl.misc.StreamerMode;

@Mixin(MutableText.class)
public class MutableTextMixin {
    @Inject(method = "asOrderedText", at = @At("HEAD"), cancellable = true)
    private void sadnes$replaceBrandOrdered(CallbackInfoReturnable<OrderedText> callbackInfoReturnable) {
        OrderedText orderedText = StreamerMode.replaceOrderedText((MutableText)(Object)this, "ordered");
        if (orderedText == null) {
            orderedText = ClientSpoof.replaceNamesOrdered((MutableText)(Object)this);
        }

        if (orderedText != null) {
            callbackInfoReturnable.setReturnValue(orderedText);
        }
    }
}
