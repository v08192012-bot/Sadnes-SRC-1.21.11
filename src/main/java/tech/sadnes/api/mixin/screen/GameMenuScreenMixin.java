package tech.sadnes.api.mixin.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.feature.component.AutoReconnectComponent;
import tech.sadnes.client.feature.impl.misc.PvPSafe;
import tech.sadnes.client.lang.Language;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    @Shadow
    private ButtonWidget exitButton;
    @Unique
    private ButtonWidget sadnes$reconnectButton;
    @Unique
    private boolean sadnes$blocked;

    protected GameMenuScreenMixin(Text text) {
        super(text);
    }

    @Inject(method = "initWidgets", at = @At("TAIL"))
    private void sadnes$addReconnectButton(CallbackInfo callbackInfo) {
        ServerInfo serverInfo = this.client.getCurrentServerEntry();
        if (serverInfo != null && !serverInfo.isRealm() && !this.client.isInSingleplayer()) {
            this.exitButton.setWidth(98);
            this.sadnes$reconnectButton = this.addDrawableChild(
                ButtonWidget.builder(Text.literal(Language.translate("screen.pause.reconnect", "Перезайти")), buttonWidget -> {
                    buttonWidget.active = false;
                    AutoReconnectComponent.getInstance().reconnect();
                }).dimensions(this.exitButton.getX() + 106, this.exitButton.getY(), 98, 20).build()
            );
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void sadnes$blockLeaveButton(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        if (this.exitButton != null) {
            PvPSafe pvPSafe = PvPSafe.getInstance();
            boolean flag;
            if (pvPSafe != null && pvPSafe.shouldBlockButton()) {
                flag = true;
            } else {
                flag = false;
                if (!this.sadnes$blocked) {
                    return;
                }
            }

            this.exitButton.active = !flag;
            if (this.sadnes$reconnectButton != null) {
                if (!flag) {
                    this.sadnes$reconnectButton.active = true;
                    this.sadnes$blocked = flag;
                    return;
                }

                this.sadnes$reconnectButton.active = false;
            }

            this.sadnes$blocked = flag;
        }
    }
}
