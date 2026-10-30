package tech.sadnes.client.feature.component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import tech.sadnes.api.mixin.accessor.PlayerListHudAccessor;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.math.StopWatch;

public class NetworkStatsComponent extends FeatureComponent {
    private static final int PING_TRANSACTION_ID = 1337;
    private static final long PING_INTERVAL_MS = 250L;
    private static final long PING_TIMEOUT_MS = 3000L;
    private static final long LAG_THRESHOLD_MS = 1500L;
    private static final int MAX_PING_SAMPLES = 20;
    private static final int MAX_TICK_SAMPLES = 6;
    private static final Pattern FOOTER_PING_PATTERN = Pattern.compile("(\\d{1,5})\\s*ms\\b", 2);
    private final StopWatch pingTimer = new StopWatch();
    private final StopWatch lastPacketTimer = new StopWatch();
    private final StopWatch lastWorldPacketTimer = new StopWatch();
    private long pingSentAt;
    private boolean awaitingPing;
    private boolean hasPing;
    private final Deque<NetworkStatsComponent.PingSample> pingSamples = new ArrayDeque<>(20);
    private final Deque<NetworkStatsComponent.TickSample> tickSamples = new ArrayDeque<>(6);
    private int ping;
    private int jitter;
    private double variance;
    private double packetLoss;
    private volatile double tps = 20.0;

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player != null && mc.world != null) {
            if (this.awaitingPing && System.currentTimeMillis() - this.pingSentAt > 3000L) {
                this.awaitingPing = false;
                this.addSample(new NetworkStatsComponent.PingSample(-1, false));
            }

            if (this.pingTimer.isElapsed(250L) && !this.awaitingPing) {
                this.pingTimer.reset();
                if (ServerManager.isDonut()) {
                    int i = this.readServerPing();
                    if (i > 0) {
                        this.recordPing(i);
                    }

                    return;
                }

                if (mc.getNetworkHandler() != null) {
                    this.pingSentAt = System.currentTimeMillis();
                    this.awaitingPing = true;
                }
            }
        } else {
            this.pingSamples.clear();
            this.awaitingPing = false;
            this.hasPing = false;
            this.ping = 0;
            this.jitter = 0;
            this.variance = 0.0;
            this.packetLoss = 0.0;
            this.lastWorldPacketTimer.reset();
            this.resetTps();
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()) {
            this.lastPacketTimer.reset();
            if (this.isWorldUpdatePacket(packetEvent.getPacket())) {
                this.lastWorldPacketTimer.reset();
            }

            if (packetEvent.getPacket() instanceof WorldTimeUpdateS2CPacket worldTimeUpdateS2CPacket) {
                this.recordServerTick(worldTimeUpdateS2CPacket.time(), System.nanoTime());
            }

            Packet packet = packetEvent.getPacket();
            if (packet instanceof CommandSuggestionsS2CPacket && ((CommandSuggestionsS2CPacket)packet).id() == 1337) {
                if (this.awaitingPing) {
                    this.awaitingPing = false;
                    this.recordPing(Math.clamp(System.currentTimeMillis() - this.pingSentAt, 0, 10000));
                }

                this.lastWorldPacketTimer.reset();
                packetEvent.cancel();
            }
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.resetTps();
    }

    public long getTimeSinceLastPacket() {
        return this.lastPacketTimer.getElapsed();
    }

    public long getTimeSinceWorldPacket() {
        return this.lastWorldPacketTimer.getElapsed();
    }

    public boolean isLagging() {
        return mc.player != null && mc.world != null ? this.lastWorldPacketTimer.getElapsed() > 1500L : false;
    }

    private boolean isWorldUpdatePacket(Packet<?> packet) {
        return packet instanceof WorldTimeUpdateS2CPacket
            || packet instanceof EntityS2CPacket
            || packet instanceof EntityPositionSyncS2CPacket
            || packet instanceof EntityVelocityUpdateS2CPacket
            || packet instanceof BundleS2CPacket
            || packet instanceof KeepAliveS2CPacket
            || packet instanceof CommonPingS2CPacket;
    }

    private int readServerPing() {
        Text text = ((PlayerListHudAccessor)mc.inGameHud.getPlayerListHud()).sadnes$getFooter();
        Matcher matcher2 = FOOTER_PING_PATTERN.matcher(text == null ? "" : Formatting.strip(text.getString()));
        if (matcher2.find()) {
            return Integer.parseInt(matcher2.group(1));
        }

        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return playerListEntry == null ? 0 : playerListEntry.getLatency();
    }

    private void recordPing(int i) {
        this.jitter = this.hasPing ? Math.abs(i - this.ping) : 0;
        this.ping = i;
        this.hasPing = true;
        this.addSample(new NetworkStatsComponent.PingSample(i, true));
    }

    private void addSample(NetworkStatsComponent.PingSample pingSample) {
        if (this.pingSamples.size() >= 20) {
            this.pingSamples.pollFirst();
        }

        this.pingSamples.addLast(pingSample);
        this.recalculateStats();
    }

    private void recalculateStats() {
        int i = 0;
        int i2 = 0;
        int i3 = 0;

        for (NetworkStatsComponent.PingSample pingSample : this.pingSamples) {
            if (!pingSample.success()) {
                i++;
            } else {
                i2 += pingSample.ping();
                i3++;
            }
        }

        this.packetLoss = this.pingSamples.isEmpty() ? 0.0 : (double)i / this.pingSamples.size() * 100.0;
        if (i3 < 2) {
            this.variance = 0.0;
        } else {
            double d = (double)i2 / i3;
            double d2 = 0.0;

            for (NetworkStatsComponent.PingSample pingSample2 : this.pingSamples) {
                if (pingSample2.success()) {
                    d2 += Math.pow(pingSample2.ping() - d, 2.0);
                }
            }

            this.variance = Math.sqrt(d2 / i3);
        }
    }

    private synchronized void recordServerTick(long l, long l2) {
        NetworkStatsComponent.TickSample tickSample = this.tickSamples.peekLast();
        if (tickSample != null && l <= tickSample.serverTick()) {
            this.tickSamples.clear();
        }

        this.tickSamples.addLast(new NetworkStatsComponent.TickSample(l, l2));
        if (this.tickSamples.size() > 6) {
            this.tickSamples.pollFirst();
        }

        if (this.tickSamples.size() >= 2) {
            NetworkStatsComponent.TickSample tickSample2 = this.tickSamples.peekFirst();
            NetworkStatsComponent.TickSample tickSample3 = this.tickSamples.peekLast();
            long l3 = tickSample3.receivedNanos() - tickSample2.receivedNanos();
            long l4 = tickSample3.serverTick() - tickSample2.serverTick();
            if (l3 > 0L && l4 > 0L) {
                this.tps = Math.clamp(l4 * 1.0E9 / l3, 0.0, 20.0);
            }
        }
    }

    private synchronized void resetTps() {
        this.tickSamples.clear();
        this.tps = 20.0;
    }

    public int getPing() {
        return this.ping;
    }

    public int getJitter() {
        return this.jitter;
    }

    public double getVariance() {
        return this.variance;
    }

    public double getPacketLoss() {
        return this.packetLoss;
    }

    public double getTps() {
        return this.tps;
    }

    record PingSample(int ping, boolean success) {
    }

    record TickSample(long serverTick, long receivedNanos) {
    }
}
