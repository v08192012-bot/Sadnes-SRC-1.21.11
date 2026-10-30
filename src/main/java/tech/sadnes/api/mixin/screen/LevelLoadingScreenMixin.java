package tech.sadnes.api.mixin.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.world.ClientChunkLoadProgress;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.world.chunk.ChunkLoadMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.lang.Language;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {
    @Shadow
    private ClientChunkLoadProgress chunkLoadProgress;
    @Unique
    private ButtonWidget sadnes$cancelButton;

    protected LevelLoadingScreenMixin(Text text) {
        super(text);
    }

    @Override
    public void init() {
        super.init();
        if (this.client != null && !this.client.isInSingleplayer()) {
            this.sadnes$cancelButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal(Language.translate("menu.cancel", "Отмена")), buttonWidget -> this.client.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT)
                    )
                    .dimensions(this.width / 2 - 100, this.sadnes$cancelY(), 200, 20)
                    .build()
            );
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void sadnes$repositionCancel(DrawContext drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        if (this.sadnes$cancelButton != null) {
            this.sadnes$cancelButton.setY(this.sadnes$cancelY());
        }
    }

    @Unique
    private int sadnes$cancelY() {
        ChunkLoadMap chunkLoadMap = this.chunkLoadProgress == null ? null : this.chunkLoadProgress.getChunkLoadMap();
        int i = chunkLoadMap == null ? 0 : chunkLoadMap.getRadius() * 2 + 1;
        return this.height / 2 + i + 12;
    }
}
