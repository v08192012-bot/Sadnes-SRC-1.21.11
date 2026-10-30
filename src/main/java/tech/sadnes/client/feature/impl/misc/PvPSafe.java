package tech.sadnes.client.feature.impl.misc;

import java.util.Locale;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.TimerUtil;

@FeatureInfo(
    name = "PvPSafe",
    description = "feature.impl.misc.pvpsafe",
    displayName = "feature.impl.misc.pvpsafe",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SAFETY
)
public final class PvPSafe extends Feature {
    private final MultiSetting protections = new MultiSetting("pvpsafe.protections", BooleanSetting.of("button", true), BooleanSetting.of("command", true));
    private final TimerUtil notifyTimer = new TimerUtil();

    public static PvPSafe getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(PvPSafe.class);
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isSend() && this.protections.has("command") && ServerManager.isInPvp()) {
            String string = this.extractCommand(packetEvent.getPacket());
            if (string != null && this.isBlockedCommand(string)) {
                packetEvent.cancel();
                if (this.notifyTimer.isReached()) {
                    NotificationUtil.notify(NotificationType.INFO, this.getDisplayName(), Language.translate("pvpsafe.blocked"));
                    this.notifyTimer.setDelay(1500L);
                }
            }
        }
    }

    public boolean shouldBlockButton() {
        return this.isEnabled() && this.protections.has("button") && ServerManager.isInPvp();
    }

    private String extractCommand(Packet<?> packet) {
        if (packet instanceof CommandExecutionC2SPacket) {
            return ((CommandExecutionC2SPacket)packet).command();
        } else {
            return packet instanceof ChatCommandSignedC2SPacket ? ((ChatCommandSignedC2SPacket)packet).command() : null;
        }
    }

    private boolean isBlockedCommand(String string) {
        String string2 = string.trim().toLowerCase(Locale.ROOT);
        int i = string2.indexOf(32);
        if (i != -1) {
            string2 = string2.substring(0, i);
        }

        return string2.equals("hub") || string2.equals("limbo") || string2.matches("an\\d*");
    }
}
