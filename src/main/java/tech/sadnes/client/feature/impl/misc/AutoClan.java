package tech.sadnes.client.feature.impl.misc;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.apache.commons.lang3.RandomStringUtils;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.render.AuctionHelper;
import tech.sadnes.client.friend.FriendManager;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.math.TimerUtil;

@FeatureInfo(
    name = "AutoClan",
    description = "feature.impl.misc.autoclan",
    displayName = "feature.impl.misc.autoclan",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SOCIAL
)
public final class AutoClan extends Feature {
    private final KeybindSetting bindKey = new KeybindSetting("autoclan.bindKey");
    int inviteLimit = -1;
    int balance = -1;
    boolean clanCreated = false;
    boolean nameTaken = false;
    private final List<String> inviteQueue = new ArrayList<>();
    private final List<String> processedPlayers = new ArrayList<>();
    private final TimerUtil inviteTimer = new TimerUtil();
    private int invitesSent = 0;
    private String pendingInvite = null;
    boolean active = false;

    public static AutoClan getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(AutoClan.class);
    }

    @EventHandler
    public void onKey(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.isPress() && this.bindKey.matchesKey(screenMouseClickEvent.getButton())) {
            this.start();
            this.active = true;
        }
    }

    @EventHandler
    public void onMouse(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.isPress() && this.bindKey.matchesKey(screenKeyEvent.getKey())) {
            this.start();
            this.active = true;
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.active) {
            if (!ServerManager.isFunTime() && !ServerManager.isSpookyTime()) {
                ChatUtil.print(this.getName() + " работает только на Funtime/Spookytime");
                this.setEnabled(false);
            } else {
                if (this.clanCreated && !this.nameTaken) {
                    if (this.inviteLimit == -1) {
                        return;
                    }

                    if (this.invitesSent >= this.inviteLimit) {
                        this.active = false;
                        return;
                    }

                    if (!this.inviteTimer.isReached()) {
                        return;
                    }

                    if (this.inviteQueue.isEmpty()) {
                        this.collectFriends();
                    }

                    if (this.inviteQueue.isEmpty()) {
                        this.active = false;
                        return;
                    }

                    String string = this.inviteQueue.removeFirst();
                    this.processedPlayers.add(string);
                    this.pendingInvite = string;
                    mc.player.networkHandler.sendChatCommand("clan invite " + string);
                    this.inviteTimer.setDelay(250L);
                } else if (this.balance >= 10000 && this.inviteLimit != -1) {
                    if (mc.player.age % 10 != 0) {
                        return;
                    }

                    mc.player.networkHandler.sendChatCommand("clan create " + RandomStringUtils.randomAlphanumeric(3, 5));
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (!(packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket)) {
            Packet packet = packetEvent.getPacket();
            if (packet instanceof ChatMessageC2SPacket) {
                ;
            }
        } else {
            String string = gameMessageS2CPacket.content().getString().toLowerCase();
            if (string.contains("вы успешно создали клан") || string.contains("ты уже состоишь в клане")) {
                this.clanCreated = true;
                this.nameTaken = false;
            } else if (string.contains("клан с таким названием уже существует")) {
                this.nameTaken = true;
            }

            if (this.pendingInvite != null) {
                if (string.contains("вы отправили приглашение в клан игроку")) {
                    this.invitesSent++;
                    this.processedPlayers.add(this.pendingInvite);
                    this.pendingInvite = null;
                    return;
                }

                if (string.contains("игрок") && string.contains("не в сети")) {
                    this.processedPlayers.add(this.pendingInvite);
                    this.pendingInvite = null;
                    this.collectFriends();
                    return;
                }

                if (string.contains("игрок состоит в другом клане")) {
                    this.processedPlayers.add(this.pendingInvite);
                    this.pendingInvite = null;
                    this.collectFriends();
                    return;
                }

                if (string.contains("в клане больше игроков быть не может")) {
                    this.active = false;
                    this.balance = -1;
                    this.inviteLimit = -1;
                    this.clanCreated = false;
                    this.nameTaken = false;
                    this.inviteQueue.clear();
                    this.processedPlayers.clear();
                    this.invitesSent = 0;
                    this.pendingInvite = null;
                }
            }
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.clanCreated = false;
        this.nameTaken = false;
        this.inviteQueue.clear();
        this.processedPlayers.clear();
        this.invitesSent = 0;
        this.pendingInvite = null;
        this.start();
    }

    private void start() {
        this.balance = AuctionHelper.getInstance().getBalance();
        int i = Integer.toString(Math.abs(ServerManager.getAnarchyNumber())).charAt(0) - '0';
        this.inviteLimit = i == 1 ? -1 : (i > 1 && i < 9 ? i - 1 : i);
        this.collectFriends();
        if (this.inviteQueue.isEmpty()) {
            this.inviteLimit = -1;
        }
    }

    private void collectFriends() {
        this.inviteQueue.clear();
        if (mc.player != null && mc.getNetworkHandler() != null) {
            FriendManager friendManager = Sadnes.getInstance().getFriendManager();

            for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getPlayerList()) {
                String string = playerListEntry.getProfile().name();
                if (!string.equalsIgnoreCase(mc.player.getName().getString()) && friendManager.isFriend(string) && !this.processedPlayers.contains(string)) {
                    this.inviteQueue.add(string);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (!ServerManager.isFunTime() && !ServerManager.isSpookyTime()) {
            ChatUtil.print(this.getName() + " работает только на Funtime/Spookytime");
            this.setEnabled(false);
        } else {
            this.clanCreated = false;
            this.nameTaken = false;
            this.inviteQueue.clear();
            this.processedPlayers.clear();
            this.invitesSent = 0;
            this.pendingInvite = null;
            this.inviteLimit = -1;
            this.balance = -1;
            this.active = false;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.clanCreated = false;
        this.nameTaken = false;
        this.inviteQueue.clear();
        this.processedPlayers.clear();
        this.invitesSent = 0;
        this.pendingInvite = null;
        this.inviteLimit = -1;
        this.balance = -1;
        this.active = false;
    }
}
