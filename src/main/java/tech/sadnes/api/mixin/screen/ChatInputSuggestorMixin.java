package tech.sadnes.api.mixin.screen;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.api.mixin.accessor.ChatInputSuggestorAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.party.PartyCommandCompletion;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {
    @Shadow
    @Final
    private TextFieldWidget textField;

    @Shadow
    public abstract void clearWindow();

    @Shadow
    public abstract void show(boolean flag);

    @Shadow
    public abstract void setWindowActive(boolean flag);

    @Inject(method = "refresh", at = @At("TAIL"))
    private void sadnes$commandPopup(CallbackInfo callbackInfo) {
        CommandManager commandManager = Sadnes.getInstance().getCommandManager();
        if (commandManager != null) {
            String string = this.textField.getText();
            if (string != null) {
                if (string.startsWith("/")) {
                    this.sadnes$partyLogins(string);
                } else if (string.startsWith(".")) {
                    CommandManager.Suggestions suggestions = commandManager.getSuggestions(string, this.textField.getCursor());
                    if (suggestions != null && !suggestions.values().isEmpty()) {
                        this.sadnes$showSuggestions(suggestions.start(), suggestions.end(), suggestions.values());
                    } else {
                        this.clearWindow();
                    }
                }
            }
        }
    }

    @Unique
    private void sadnes$partyLogins(String string) {
        PartyCommandCompletion.Suggestion suggestion = PartyCommandCompletion.suggest(string, Math.min(this.textField.getCursor(), string.length()));
        if (suggestion != null) {
            this.sadnes$showSuggestions(suggestion.start(), suggestion.end(), suggestion.logins());
        }
    }

    @Unique
    private void sadnes$showSuggestions(int i, int i2, List<String> strings) {
        ArrayList arrayList = new ArrayList();
        StringRange stringRange = StringRange.between(i, i2);

        for (String string : strings) {
            arrayList.add(new Suggestion(stringRange, string));
        }

        this.setWindowActive(true);
        ((ChatInputSuggestorAccessor)this).sadnes$setPendingSuggestions(CompletableFuture.completedFuture(new Suggestions(stringRange, arrayList)));
        this.show(false);
    }
}
