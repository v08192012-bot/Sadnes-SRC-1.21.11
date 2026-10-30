package tech.sadnes.client.party;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.network.SadnesSocketClient;
import tech.sadnes.client.network.packet.PlayerState;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.math.StopWatch;

public final class PartyStateReporter {
    private static final long TICK_MS = 50L;
    private static final long SNAPSHOT_MAX_AGE_MS = 500L;
    private static final long RESEND_INTERVAL_MS = 2500L;
    private final SadnesSocketClient connection;
    private final ScheduledExecutorService executor;
    private volatile PartyStateReporter.StateSnapshot snapshot;
    private volatile PartyStateReporter.ActivePing activePing;
    private final StopWatch resendTimer = new StopWatch();
    private PlayerState lastSent;
    private boolean reporting;

    public PartyStateReporter(SadnesSocketClient sadnesSocketClient) {
        this.connection = sadnesSocketClient;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sadnes-state-reporter");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        Sadnes.getEventBus().subscribe(this);
        this.executor.scheduleWithFixedDelay(this::reportTick, 50L, 50L, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        Sadnes.getEventBus().unsubscribe(this);
        this.executor.shutdownNow();
    }

    public void setPing(int i, double d, double d2, double d3, long l) {
        this.activePing = new PartyStateReporter.ActivePing(i, d, d2, d3, null, System.currentTimeMillis() + l);
    }

    public void setEntityPing(int i, UUID uuid, double d, double d2, double d3, long l) {
        this.activePing = new PartyStateReporter.ActivePing(i, d, d2, d3, uuid, System.currentTimeMillis() + l);
    }

    public void clearPing() {
        this.activePing = null;
    }

    public PartyStateReporter.ActivePing getActivePing() {
        PartyStateReporter.ActivePing activePing2 = this.activePing;
        return activePing2 != null && System.currentTimeMillis() < activePing2.expiresAtMs() ? activePing2 : null;
    }

    @EventHandler
    public void onTick(PlayerTickEvent playerTickEvent) {
        this.snapshot = new PartyStateReporter.StateSnapshot(this.captureState(), System.currentTimeMillis());
    }

    private PlayerState captureState() {
        if (IMinecraft.mc.player != null && IMinecraft.mc.world != null && IMinecraft.mc.getCurrentServerEntry() != null) {
            PartyStateReporter.ActivePing activePing2 = this.getActivePing();
            UUID uuid = activePing2 != null ? activePing2.entity() : null;
            double d = 0.0;
            double d2 = 0.0;
            double d3 = 0.0;
            if (activePing2 != null) {
                Entity entity2 = uuid != null ? findEntity(uuid) : null;
                d = entity2 != null ? entity2.getX() : activePing2.x();
                d2 = entity2 != null ? entity2.getY() : activePing2.y();
                d3 = entity2 != null ? entity2.getZ() : activePing2.z();
            }

            return new PlayerState(
                IMinecraft.mc.player.getGameProfile().name(),
                ServerManager.getServerName(),
                WorldIdentity.currentWorldId(),
                IMinecraft.mc.player.getX(),
                IMinecraft.mc.player.getY(),
                IMinecraft.mc.player.getZ(),
                itemId(IMinecraft.mc.player.getMainHandStack()),
                itemId(IMinecraft.mc.player.getOffHandStack()),
                List.of(
                    itemId(IMinecraft.mc.player.getEquippedStack(EquipmentSlot.HEAD)),
                    itemId(IMinecraft.mc.player.getEquippedStack(EquipmentSlot.CHEST)),
                    itemId(IMinecraft.mc.player.getEquippedStack(EquipmentSlot.LEGS)),
                    itemId(IMinecraft.mc.player.getEquippedStack(EquipmentSlot.FEET))
                ),
                activePing2 != null,
                activePing2 != null ? activePing2.type() : 0,
                d,
                d2,
                d3,
                uuid
            );
        } else {
            return null;
        }
    }

    private static String itemId(ItemStack itemStack) {
        return itemStack.isEmpty() ? "" : Registries.ITEM.getId(itemStack.getItem()).toString();
    }

    private static Entity findEntity(UUID uuid) {
        if (uuid != null && IMinecraft.mc.world != null) {
            for (Entity entity : IMinecraft.mc.world.getEntities()) {
                if (uuid.equals(entity.getUuid())) {
                    return entity;
                }
            }
        }

        return null;
    }

    private void reportTick() {
        try {
            PartyStateReporter.StateSnapshot stateSnapshot = this.snapshot;
            if (stateSnapshot == null || stateSnapshot.data() == null || System.currentTimeMillis() - stateSnapshot.atMs() > 500L) {
                if (this.reporting) {
                    this.connection.sendRefresh();
                    this.reporting = false;
                    this.lastSent = null;
                }

                return;
            }

            if (stateSnapshot.data().equals(this.lastSent) && !this.resendTimer.hasPassed(2500.0)) {
                return;
            }

            this.connection.publishData(stateSnapshot.data());
            this.lastSent = stateSnapshot.data();
            this.reporting = true;
            this.resendTimer.reset();
        } catch (RuntimeException exception) {
        }
    }

    public record ActivePing(int type, double x, double y, double z, UUID entity, long expiresAtMs) {
    }

    record StateSnapshot(PlayerState data, long atMs) {
    }
}
