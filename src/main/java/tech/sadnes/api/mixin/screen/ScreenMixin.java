package tech.sadnes.api.mixin.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.component.HudSettingsPopupComponent;
import tech.sadnes.client.feature.impl.misc.QuickInteract;
import tech.sadnes.client.feature.impl.render.hand.HandOffset;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.RenderHooks;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.menu.MainMenuScreen;
import tech.sadnes.client.ui.screen.LanguageScreen;
import tech.sadnes.client.ui.screen.ScreenElement;
import tech.sadnes.client.ui.screen.ScriptScreen;
import tech.sadnes.client.ui.screen.settings.SettingsScreen;

@Mixin(Screen.class)
public abstract class ScreenMixin implements ScreenElement {
    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void sadnes$beginScreenRender(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        if ((Object)this instanceof ClickGuiScreen
            || (Object)this instanceof MainMenuScreen
            || (Object)this instanceof SettingsScreen
            || (Object)this instanceof LanguageScreen
            || (Object)this instanceof ScriptScreen) {
            RenderHooks.beginScreenRender(drawContext);
        }
    }

    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void sadnes$endScreenRender(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        if ((Object)this instanceof ClickGuiScreen
            || (Object)this instanceof MainMenuScreen
            || (Object)this instanceof SettingsScreen
            || (Object)this instanceof LanguageScreen
            || (Object)this instanceof ScriptScreen) {
            RenderHooks.endScreenRender();
        }
    }

    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void sadnes$renderHudSettingsOverChat(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        if ((Object)this instanceof ChatScreen) {
            HudSettingsPopupComponent hudSettingsPopupComponent = HudSettingsPopupComponent.getInstance();
            if (hudSettingsPopupComponent != null && (hudSettingsPopupComponent.isOpen() || hudSettingsPopupComponent.isVisible())) {
                int i3 = (int)GuiScale.toScaled(i);
                int i4 = (int)GuiScale.toScaled(i2);
                GuiScale.push(drawContext);

                try {
                    hudSettingsPopupComponent.render(drawContext, i3, i4, f);
                } finally {
                    GuiScale.pop(drawContext);
                }
            }
        }
    }

    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void sadnes$renderHandEditor(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        if ((Object)this instanceof ChatScreen) {
            HandOffset handOffset = HandOffset.getInstance();
            if (handOffset != null && handOffset.isEditorActive()) {
                float f2 = GuiScale.toScaled(i);
                float f3 = GuiScale.toScaled(i2);
                GuiScale.push(drawContext);

                try {
                    handOffset.renderEditor(drawContext, f2, f3);
                } finally {
                    GuiScale.pop(drawContext);
                }
            }
        }
    }

    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void sadnes$renderQuickInteract(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        if ((Object)this instanceof ChatScreen || (Object)this instanceof ClickGuiScreen) {
            QuickInteract quickInteract = QuickInteract.getInstance();
            if (quickInteract != null && quickInteract.isEnabled() && quickInteract.isInteracting()) {
                int i3 = (int)GuiScale.toScaled(i);
                int i4 = (int)GuiScale.toScaled(i2);
                GuiScale.push(drawContext);

                try {
                    quickInteract.renderInChat(drawContext, i3, i4, f);
                } finally {
                    GuiScale.pop(drawContext);
                }
            }
        }
    }

    @Inject(method = "updateNarrator", at = @At("HEAD"), cancellable = true)
    private void sadnes$disableNarration(CallbackInfo callbackInfo) {
        callbackInfo.cancel();
    }
}
