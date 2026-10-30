package tech.sadnes.api.mixin.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.util.network.LastServerInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Shadow
    @Final
    private DirectionalLayoutWidget grid;
    @Shadow
    @Final
    private Screen parent;

    protected DisconnectedScreenMixin(Text text) {
        super(text);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sadnes$addReconnectButton(CallbackInfo callbackInfo) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ServerInfo serverInfo = minecraftClient.getCurrentServerEntry() != null ? minecraftClient.getCurrentServerEntry() : LastServerInfo.getServerInfo();
        if (serverInfo != null) {
            ServerAddress serverAddress = ServerAddress.parse(serverInfo.address);
            if (serverAddress != null) {
                ButtonWidget buttonWidget = ButtonWidget.builder(Text.literal(Language.translate("screen.disconnected.reconnect", "Переподключиться")), buttonWidget2 -> {
                    ServerInfo serverInfo2 = minecraftClient.getCurrentServerEntry() != null ? minecraftClient.getCurrentServerEntry() : LastServerInfo.getServerInfo();
                    ServerAddress serverAddress2 = serverInfo2 != null ? ServerAddress.parse(serverInfo2.address) : LastServerInfo.getAddress();
                    if (serverInfo2 != null && serverAddress2 != null) {
                        ConnectScreen.connect(this.parent, minecraftClient, serverAddress2, serverInfo2, false, null);
                    }
                }).width(200).build();
                this.grid.add(buttonWidget);
                this.grid.refreshPositions();
                this.addDrawableChild(buttonWidget);
            }
        }
    }
}
