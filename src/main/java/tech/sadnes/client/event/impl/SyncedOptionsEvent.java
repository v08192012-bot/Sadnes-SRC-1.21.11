package tech.sadnes.client.event.impl;

import net.minecraft.network.message.ChatVisibility;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.Arm;
import tech.sadnes.client.event.Event;

public final class SyncedOptionsEvent extends Event {
    private String language;
    private int viewDistance;
    private ChatVisibility chatVisibility;
    private boolean chatColorsEnabled;
    private int playerModelParts;
    private Arm mainArm;
    private boolean filtersText;
    private boolean allowsServerListing;
    private ParticlesMode particleStatus;

    public SyncedOptionsEvent(SyncedClientOptions syncedClientOptions) {
        this.language = syncedClientOptions.language();
        this.viewDistance = syncedClientOptions.viewDistance();
        this.chatVisibility = syncedClientOptions.chatVisibility();
        this.chatColorsEnabled = syncedClientOptions.chatColorsEnabled();
        this.playerModelParts = syncedClientOptions.playerModelParts();
        this.mainArm = syncedClientOptions.mainArm();
        this.filtersText = syncedClientOptions.filtersText();
        this.allowsServerListing = syncedClientOptions.allowsServerListing();
        this.particleStatus = syncedClientOptions.particleStatus();
    }

    public SyncedClientOptions toOptions() {
        return new SyncedClientOptions(
            this.language,
            this.viewDistance,
            this.chatVisibility,
            this.chatColorsEnabled,
            this.playerModelParts,
            this.mainArm,
            this.filtersText,
            this.allowsServerListing,
            this.particleStatus
        );
    }

    public String getLanguage() {
        return this.language;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public ChatVisibility getChatVisibility() {
        return this.chatVisibility;
    }

    public boolean isChatColorsEnabled() {
        return this.chatColorsEnabled;
    }

    public int getPlayerModelParts() {
        return this.playerModelParts;
    }

    public Arm getMainArm() {
        return this.mainArm;
    }

    public boolean isFiltersText() {
        return this.filtersText;
    }

    public boolean isAllowsServerListing() {
        return this.allowsServerListing;
    }

    public ParticlesMode getParticleStatus() {
        return this.particleStatus;
    }

    public void setLanguage(String string) {
        this.language = string;
    }

    public void setViewDistance(int i) {
        this.viewDistance = i;
    }

    public void setChatVisibility(ChatVisibility chatVisibility2) {
        this.chatVisibility = chatVisibility2;
    }

    public void setChatColorsEnabled(boolean flag) {
        this.chatColorsEnabled = flag;
    }

    public void setPlayerModelParts(int i) {
        this.playerModelParts = i;
    }

    public void setMainArm(Arm arm) {
        this.mainArm = arm;
    }

    public void setFiltersText(boolean flag) {
        this.filtersText = flag;
    }

    public void setAllowsServerListing(boolean flag) {
        this.allowsServerListing = flag;
    }

    public void setParticleStatus(ParticlesMode particlesMode) {
        this.particleStatus = particlesMode;
    }
}
