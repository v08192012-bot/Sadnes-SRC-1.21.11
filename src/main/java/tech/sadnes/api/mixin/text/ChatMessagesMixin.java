package tech.sadnes.api.mixin.text;

import net.minecraft.client.util.ChatMessages;
import net.minecraft.text.StringVisitable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tech.sadnes.client.feature.impl.misc.StreamerMode;

@Mixin(ChatMessages.class)
public class ChatMessagesMixin {
    @ModifyVariable(method = "breakRenderedChatMessageLines", at = @At("HEAD"), argsOnly = true, index = 0)
    private static StringVisitable sadnes$replaceChatText(StringVisitable stringVisitable) {
        return StreamerMode.replaceInVisitable(stringVisitable);
    }
}
