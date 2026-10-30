package tech.sadnes.client.util.network;

import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.text.Text;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.math.StopWatch;

public final class AutoReconnector implements IMinecraft {
    private final StopWatch retryTimer = new StopWatch();
    private String reason;
    private int attempts;

    public void disconnect(String string) {
        this.reason = string;
        this.attempts = 0;
        this.retryTimer.reset();
        mc.getNetworkHandler().getConnection().disconnect(Text.literal(string));
    }

    public void tick() {
        if (this.reason != null) {
            Screen screen = mc.currentScreen;
            if (screen instanceof TitleScreen || screen instanceof MultiplayerScreen) {
                this.cancel();
            }

            if (this.reason != null && screen instanceof DisconnectedScreen) {
                if (this.retryTimer.hasPassed(this.attempts == 0 ? 2000.0 : 15000.0)) {
                    if (this.attempts == 10) {
                        this.cancel();
                    } else {
                        this.attempts++;
                        this.retryTimer.reset();
                        ConnectScreen.connect(new TitleScreen(), mc, LastServerInfo.getAddress(), LastServerInfo.getServerInfo(), false, null);
                    }
                }
            }
        }
    }

    public String consumeReason() {
        String string = this.reason;
        this.cancel();
        return string;
    }

    public void cancel() {
        this.reason = null;
    }
}
