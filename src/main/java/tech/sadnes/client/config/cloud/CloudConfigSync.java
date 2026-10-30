package tech.sadnes.client.config.cloud;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import tech.sadnes.client.network.SadnesSocketClient;
import tech.sadnes.client.network.packet.CloudConfigEntry;
import tech.sadnes.client.network.packet.ConfigKind;
import tech.sadnes.client.network.packet.ShareCodesPacket;
import tech.sadnes.client.network.packet.ShareResultPacket;

public final class CloudConfigSync {
    private final ConfigKind kind;
    private final SadnesSocketClient socketClient;
    private final Supplier<String> exporter;
    private final Consumer<String> importer;
    private volatile List<CloudConfigEntry> entries = List.of();
    private volatile String activeId = "";

    public CloudConfigSync(ConfigKind configKind, SadnesSocketClient sadnesSocketClient, Supplier<String> supplier, Consumer<String> consumer) {
        this.kind = configKind;
        this.socketClient = sadnesSocketClient;
        this.exporter = supplier;
        this.importer = consumer;
    }

    public ConfigKind getKind() {
        return this.kind;
    }

    public boolean isConnected() {
        return this.socketClient.isConnected();
    }

    public List<CloudConfigEntry> getEntries() {
        return this.entries;
    }

    public String getActiveId() {
        return this.activeId;
    }

    public CompletableFuture<Void> refresh() {
        return this.socketClient.upload(this.kind).thenAccept(configListPacket -> {
            this.entries = configListPacket.getEntries();
            this.activeId = configListPacket.getActiveId();
        });
    }

    public CompletableFuture<String> create(String string, CloudConfigMeta cloudConfigMeta) {
        return this.socketClient
            .uploadFile("", this.kind, string, cloudConfigMeta.toBytes(), this.exportBytes())
            .thenCompose(string2 -> this.refresh().thenApply(void2 -> string2));
    }

    public CompletableFuture<Void> update(String string, String string2, CloudConfigMeta cloudConfigMeta) {
        return this.socketClient.uploadFile(string, this.kind, string2, cloudConfigMeta.toBytes(), this.exportBytes()).thenCompose(string3 -> this.refresh());
    }

    public CompletableFuture<Void> load(String string) {
        return this.socketClient.download(string).thenCompose(bytes -> {
            this.applyConfig(new String(bytes, StandardCharsets.UTF_8));
            if (this.isShared(string)) {
                return CompletableFuture.completedFuture(null);
            }

            this.activeId = string;
            return this.socketClient.requestAction1354(string);
        }).thenCompose(void2 -> this.refresh());
    }

    public CompletableFuture<Void> delete(String string) {
        return this.socketClient.requestAction1342(string).thenCompose(void2 -> this.refresh());
    }

    public CompletableFuture<Void> setActive(String string) {
        this.activeId = string;
        return this.socketClient.requestAction1354(string).thenCompose(void2 -> this.refresh());
    }

    public CompletableFuture<ShareResultPacket> lookupShare(String string, boolean flag) {
        return this.socketClient.lookup(string, flag);
    }

    public CompletableFuture<String> importShared(String string) {
        return this.socketClient.requestAction1340(string).thenCompose(string2 -> this.refresh().thenApply(void2 -> string2));
    }

    public CompletableFuture<ShareCodesPacket> getShareCodes(String string) {
        return this.socketClient.requestInfo(string);
    }

    public CompletableFuture<Void> revokeShare(String string) {
        return this.socketClient.requestAction1349(string);
    }

    public boolean isShared(String string) {
        for (CloudConfigEntry cloudConfigEntry : this.entries) {
            if (cloudConfigEntry.id().equals(string)) {
                return cloudConfigEntry.shared();
            }
        }

        return false;
    }

    public String getEntryName(String string) {
        for (CloudConfigEntry cloudConfigEntry : this.entries) {
            if (cloudConfigEntry.id().equals(string)) {
                return cloudConfigEntry.name();
            }
        }

        return "";
    }

    public boolean hasEntry(String string) {
        for (CloudConfigEntry cloudConfigEntry : this.entries) {
            if (cloudConfigEntry.id().equals(string)) {
                return true;
            }
        }

        return false;
    }

    private byte[] exportBytes() {
        return this.exporter.get().getBytes(StandardCharsets.UTF_8);
    }

    private void applyConfig(String string) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient != null) {
            minecraftClient.execute(() -> this.importer.accept(string));
        }
    }
}
