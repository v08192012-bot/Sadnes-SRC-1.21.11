package tech.sadnes.client.feature.impl.misc;

import net.minecraft.client.network.ServerInfo;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.network.DiscordRpc;
import tech.sadnes.client.network.RichPresence;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.util.math.StopWatch;
import ua.mintantileak.profile.Profile;

@FeatureInfo(
    name = "DiscordRPC",
    description = "feature.impl.misc.discordrpc",
    displayName = "feature.impl.misc.discordrpc",
    category = Category.MISC,
    subcategory = Subcategory.MISC_CLIENT
)
public final class DiscordRPC extends Feature {
    private static final long APPLICATION_ID = 1510393319971356752L;
    private static final String LARGE_IMAGE_URL = "https://s13.gifyu.com/images/bImw0.gif";
    private final BooleanSetting showServer = new BooleanSetting("discordrpc.showServer", true);
    private final BooleanSetting showSession = new BooleanSetting("discordrpc.showSession", true);
    private final StopWatch updateTimer = new StopWatch();
    private DiscordRpc rpcClient;
    private long startTimestamp;

    @Override
    public void onDisable() {
        this.startTimestamp = System.currentTimeMillis() / 1000L;
        this.rpcClient = new DiscordRpc(1510393319971356752L);
        this.rpcClient.setPresence(this.buildMenuPresence());
        this.rpcClient.setOverridePresence(this.buildCurrentPresence());
        this.rpcClient.start();
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (this.rpcClient != null) {
            this.rpcClient.stop();
            this.rpcClient = null;
        }
    }

    @EventHandler
    public void onUpdate(WorldTickEvent worldTickEvent) {
        if (this.rpcClient != null && this.updateTimer.hasPassedAndReset(2000.0)) {
            this.rpcClient.setPresence(this.buildMenuPresence());
            this.rpcClient.setOverridePresence(this.buildCurrentPresence());
        }
    }

    private RichPresence buildCurrentPresence() {
        if (ServerManager.isNullWorld()) {
            return this.buildMenuPresence();
        }

        ServerInfo serverInfo = mc.getCurrentServerEntry();
        String string = Profile.getUsername() != null ? Profile.getUsername() : "Unknown";
        if (serverInfo == null) {
            return this.buildPresence("Singleplayer", "User: " + string);
        }

        String string2 = this.showServer.getValue() ? "Playing on " + this.getServerName(serverInfo) : "Playing online";
        String string3 = "User: " + string;
        return this.buildPresence(string2, string3);
    }

    private RichPresence buildMenuPresence() {
        String string = Profile.getUsername() != null ? Profile.getUsername() : "Unknown";
        return this.buildPresence("Main menu", "User: " + string);
    }

    private RichPresence buildPresence(String string, String string2) {
        String string3 = Profile.getRole() != null ? Profile.getRole().name() : "User";
        String string4 = Profile.getUsername() != null ? Profile.getUsername() : "Unknown";
        String string5 = Profile.getSubscriptionEndDate() != null ? Profile.getSubscriptionEndDate() : "lifetime";
        RichPresence.RichPresenceBuilder richPresenceBuilder = RichPresence.builder()
            .details(string)
            .state(string2)
            .largeImage("https://s13.gifyu.com/images/bImw0.gif")
            .largeText("Sadnes 1.21.11 • " + string3)
            .button(new RichPresence.Button(ClientInfo.WEBSITE, ClientInfo.BACKEND_URL));
        String string6 = Profile.getAvatarUrl();
        if (string6 != null && !string6.isEmpty()) {
            richPresenceBuilder.smallImage(string6).smallText("User: " + string4 + " • until " + string5);
        }

        if (this.showSession.getValue()) {
            richPresenceBuilder.startTimestamp(this.startTimestamp);
        }

        return richPresenceBuilder.build();
    }

    private String getServerName(ServerInfo serverInfo) {
        if (ServerManager.isFunTime()) {
            return "FunTime";
        } else if (ServerManager.isHolyWorld()) {
            return "HolyWorld";
        } else if (ServerManager.isSpookyTime()) {
            return "SpookyTime";
        } else if (ServerManager.isReallyWorld()) {
            return "ReallyWorld";
        } else {
            return serverInfo.address != null ? serverInfo.address : "Multiplayer";
        }
    }

    public static DiscordRPC getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(DiscordRPC.class);
    }
}
