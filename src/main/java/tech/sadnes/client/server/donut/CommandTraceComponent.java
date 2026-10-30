package tech.sadnes.client.server.donut;

import java.util.List;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomClickActionC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.common.ShowDialogS2CPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.chat.ChatMessageUtil;
import tech.sadnes.client.util.player.ServerInputTracker;
import ua.mintantileak.profile.Profile;
import ua.mintantileak.profile.Role;

public final class CommandTraceComponent extends FeatureComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandTraceComponent.class);
    private volatile String pendingCommand;
    private volatile long commandSentTime;
    private String firstServerLine;
    private long inventoryCloseDelay = -1L;
    private int clicksSent;
    private int clicksWhileMoving;
    private int clicksCancelled;
    private List<String> lastMenuButtons = List.of();
    private volatile long lastServerTickTime;

    public static CommandTraceComponent getInstance() {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        return componentManager == null ? null : componentManager.getComponent(CommandTraceComponent.class);
    }

    public void onPacketSent(Packet<?> packet) {
        if (this.isTracedPacket(packet) && this.isTracingEnabled()) {
            if (!mc.isOnThread()) {
                mc.execute(() -> this.onPacketSent(packet));
            } else {
                String string = this.extractCommand(packet);
                String string2 = mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getSimpleName();
                long l = System.currentTimeMillis();
                if (string != null) {
                    if (this.pendingCommand != null) {
                        this.reportUnanswered("before the next command");
                    }

                    LOGGER.info(
                        "CommandTrace: /{} sent | server sees: {} | screen {}, window #{} | server tick {} ms ago",
                        string,
                        ServerInputTracker.describeInput(),
                        string2,
                        mc.player.currentScreenHandler.syncId,
                        l - this.lastServerTickTime
                    );
                    this.pendingCommand = string;
                    this.commandSentTime = l;
                    this.firstServerLine = null;
                    this.inventoryCloseDelay = -1L;
                } else if (packet instanceof CustomClickActionC2SPacket customClickActionC2SPacket) {
                    LOGGER.info(
                        "CommandTrace: menu button sent: {}{} | server sees: {} | screen {}",
                        customClickActionC2SPacket.id(),
                        customClickActionC2SPacket.payload().map(nbtElement -> " " + nbtElement).orElse(""),
                        ServerInputTracker.describeInput(),
                        string2
                    );
                } else if (packet instanceof CloseHandledScreenC2SPacket closeHandledScreenC2SPacket) {
                    LOGGER.info(
                        "CommandTrace: close of window #{} sent | server sees: {} | clicks sent {}, of them on the move {}, cancelled by modules {}",
                        closeHandledScreenC2SPacket.getSyncId(),
                        ServerInputTracker.describeInput(),
                        this.clicksSent,
                        this.clicksWhileMoving,
                        this.clicksCancelled
                    );
                    this.clicksSent = 0;
                    this.clicksWhileMoving = 0;
                    this.clicksCancelled = 0;
                } else {
                    this.clicksSent++;
                    if (!ServerInputTracker.isStandingStill()) {
                        this.clicksWhileMoving++;
                    }
                }
            }
        }
    }

    public void onPacketCancelled(Packet<?> packet) {
        if (this.isTracedPacket(packet) && this.isTracingEnabled()) {
            String string = this.extractCommand(packet);
            if (string != null) {
                LOGGER.info("CommandTrace: /{} cancelled by a module | server sees: {}", string, ServerInputTracker.describeInput());
            } else if (packet instanceof CloseHandledScreenC2SPacket closeHandledScreenC2SPacket) {
                LOGGER.warn("CommandTrace: close of window #{} CANCELLED by a module", closeHandledScreenC2SPacket.getSyncId());
            } else {
                if (packet instanceof ClickSlotC2SPacket) {
                    this.clicksCancelled++;
                }
            }
        }
    }

    private boolean isTracedPacket(Packet<?> packet) {
        return this.extractCommand(packet) != null
            || packet instanceof CustomClickActionC2SPacket
            || packet instanceof CloseHandledScreenC2SPacket
            || packet instanceof ClickSlotC2SPacket;
    }

    private String extractCommand(Packet<?> packet) {
        if (packet instanceof CommandExecutionC2SPacket) {
            return ((CommandExecutionC2SPacket)packet).command();
        } else {
            return packet instanceof ChatCommandSignedC2SPacket ? ((ChatCommandSignedC2SPacket)packet).command() : null;
        }
    }

    @EventHandler(priority = 500)
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()) {
            Packet packet = packetEvent.getPacket();
            if (packet instanceof WorldTimeUpdateS2CPacket) {
                this.lastServerTickTime = System.currentTimeMillis();
            }

            boolean flag;
            label55: {
                boolean flag2;
                if (!(packet instanceof OpenScreenS2CPacket) && !(packet instanceof CloseScreenS2CPacket) && !(packet instanceof ShowDialogS2CPacket)) {
                    if (this.pendingCommand != null) {
                        flag = packet instanceof GameMessageS2CPacket;
                        break label55;
                    }

                    flag2 = false;
                } else {
                    flag2 = true;
                }

                flag = flag2;
            }

            if (flag && this.isTracingEnabled()) {
                long l = System.currentTimeMillis();
                if (packet instanceof OpenScreenS2CPacket openScreenS2CPacket) {
                    String string = String.format("window #%d \"%s\"", openScreenS2CPacket.getSyncId(), openScreenS2CPacket.getName().getString());
                    mc.execute(() -> this.onWindowOpened(string, l));
                } else if (packet instanceof ShowDialogS2CPacket showDialogS2CPacket) {
                    String string2 = String.format("menu \"%s\"", showDialogS2CPacket.dialog().value().common().title().getString());
                    mc.execute(() -> this.onWindowOpened(string2, l));
                } else if (packet instanceof CloseScreenS2CPacket closeScreenS2CPacket) {
                    mc.execute(() -> this.onWindowClosedByServer(closeScreenS2CPacket.getSyncId(), l));
                } else if (packet instanceof GameMessageS2CPacket gameMessageS2CPacket && !gameMessageS2CPacket.overlay()) {
                    String string3 = ChatMessageUtil.getPlainText(gameMessageS2CPacket);
                    if (!ChatMessageUtil.isPlayerMessage(string3)) {
                        mc.execute(() -> {
                            if (this.firstServerLine == null) {
                                this.firstServerLine = string3;
                            }
                        });
                    }
                }
            }
        }
    }

    private void onWindowOpened(String string, long l) {
        if (this.pendingCommand == null) {
            LOGGER.info("CommandTrace: {} opened with no command waiting", string);
        } else {
            LOGGER.info("CommandTrace: /{} -> {} after {} ms", this.pendingCommand, string, l - this.commandSentTime);
            this.pendingCommand = null;
        }
    }

    private void onWindowClosedByServer(int i, long l) {
        LOGGER.info("CommandTrace: window #{} closed by the server", i);
        if (this.pendingCommand != null && this.inventoryCloseDelay < 0L) {
            this.inventoryCloseDelay = l - this.commandSentTime;
        }
    }

    @EventHandler
    public void onTick(WorldTickEvent worldTickEvent) {
        if (this.pendingCommand != null && System.currentTimeMillis() - this.commandSentTime >= 3000L) {
            this.reportUnanswered("in 3 s");
        }
    }

    private void reportUnanswered(String string) {
        if (this.inventoryCloseDelay >= 0L) {
            LOGGER.info("CommandTrace: /{} -> no window, the server closed the inventory after {} ms", this.pendingCommand, this.inventoryCloseDelay);
            this.pendingCommand = null;
        } else if (this.firstServerLine != null) {
            LOGGER.info("CommandTrace: /{} -> no window, first server line: \"{}\"", this.pendingCommand, this.firstServerLine);
            this.pendingCommand = null;
        } else {
            LOGGER.warn(
                "CommandTrace: /{} -> NOTHING {}: no window, no close, no chat line | server tick {} ms ago, ping {} ms",
                this.pendingCommand,
                string,
                System.currentTimeMillis() - this.lastServerTickTime,
                ServerManager.getPing()
            );
            this.pendingCommand = null;
        }
    }

    @EventHandler
    public void onWorldJoin(GameJoinEvent gameJoinEvent) {
        ServerInputTracker.reset();
        this.pendingCommand = null;
        if (this.isTracingEnabled()) {
            LOGGER.info("CommandTrace: joined a world or respawned");
            List<String> strings = QuickActionsUtil.describeActions();
            if (!strings.equals(this.lastMenuButtons)) {
                this.lastMenuButtons = strings;

                for (String string : strings) {
                    LOGGER.info("CommandTrace: menu button {}", string);
                }
            }
        }
    }

    private boolean isTracingEnabled() {
        Role role = Profile.getRole();
        return role != null && role.ordinal() >= Role.ADMIN.ordinal() && mc.player != null && ServerManager.isDonut();
    }
}
