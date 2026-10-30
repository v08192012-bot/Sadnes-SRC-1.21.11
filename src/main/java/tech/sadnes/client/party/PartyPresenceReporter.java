package tech.sadnes.client.party;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.network.SadnesSocketClient;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.math.StopWatch;

public final class PartyPresenceReporter {
    private static final long IDLE_INTERVAL_MS = 60000L;
    private static final long CHANGED_INTERVAL_MS = 15000L;
    private static final long CHECK_INTERVAL_MS = 1000L;
    private final SadnesSocketClient connection;
    private final StopWatch checkTimer = new StopWatch();
    private final StopWatch sendTimer = new StopWatch();
    private String lastServer = "";
    private List<String> lastFeatures = List.of();

    public PartyPresenceReporter(SadnesSocketClient sadnesSocketClient) {
        this.connection = sadnesSocketClient;
    }

    public void start() {
        this.sendTimer.setElapsed(60000L);
        Sadnes.getEventBus().subscribe(this);
    }

    public void stop() {
        Sadnes.getEventBus().unsubscribe(this);
    }

    @EventHandler
    public void onUpdate(WorldTickEvent worldTickEvent) {
        if (this.connection.isConnected() && this.checkTimer.hasPassed(1000.0)) {
            this.checkTimer.reset();
            String string = currentServer();
            List list = enabledFeatures();
            boolean flag;
            if (string.equals(this.lastServer)) {
                flag = !list.equals(this.lastFeatures);
            } else {
                flag = true;
            }

            if (this.sendTimer.hasPassed(flag ? 15000.0 : 60000.0)) {
                this.connection.sendNamedList(string, list);
                this.lastServer = string;
                this.lastFeatures = list;
                this.sendTimer.reset();
            }
        }
    }

    private static String currentServer() {
        if (IMinecraft.mc.getCurrentServerEntry() != null && IMinecraft.mc.getCurrentServerEntry().address != null) {
            String string = IMinecraft.mc.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);
            return string.length() <= 128 ? string : string.substring(0, 128);
        } else {
            return IMinecraft.mc.world != null ? "singleplayer" : "";
        }
    }

    private static List<String> enabledFeatures() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        if (featureManager == null) {
            return List.of();
        }

        ArrayList arrayList = new ArrayList();

        for (Feature feature : featureManager.values()) {
            if (feature.isEnabled()) {
                arrayList.add(feature.getDisplayName());
                if (arrayList.size() == 512) {
                    break;
                }
            }
        }

        return arrayList;
    }
}
