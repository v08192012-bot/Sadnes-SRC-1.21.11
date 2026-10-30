package tech.sadnes.client.party;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatScreen;
import tech.sadnes.api.mixin.accessor.ChatScreenAccessor;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.network.SadnesSocketClient;
import tech.sadnes.client.network.packet.PartyInviteReceivedPacket;
import tech.sadnes.client.network.packet.PartyLooksPacket;
import tech.sadnes.client.network.packet.PartyMember;
import tech.sadnes.client.network.packet.PartyResultPacket;
import tech.sadnes.client.network.packet.PartyStatePacket;
import tech.sadnes.client.network.packet.PlayerLook;
import tech.sadnes.client.network.packet.PlayerState;
import tech.sadnes.client.network.packet.UserInfo;
import tech.sadnes.client.network.packet.UserSearchResultPacket;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.StopWatch;
import ua.mintantileak.profile.Profile;

public final class PartyManager {
    private static final long SEARCH_REFRESH_MS = 3000L;
    private final SadnesSocketClient connection;
    private final PartyStateCache stateCache;
    private volatile PartyStatePacket party = PartyStatePacket.empty();
    private volatile PartyInviteReceivedPacket pendingInvite;
    private volatile List<UserInfo> searchResults = List.of();
    private volatile Map<Integer, PlayerLook> memberLooks = Map.of();
    private volatile PlayerLook currentLook = PlayerLook.NONE;
    private final Map<String, String> avatarUrls = new ConcurrentHashMap<>();
    private final StopWatch searchTimer = new StopWatch();
    private String lastSearchQuery = "";

    public PartyManager(SadnesSocketClient sadnesSocketClient, PartyStateCache partyStateCache) {
        this.connection = sadnesSocketClient;
        this.stateCache = partyStateCache;
        sadnesSocketClient.setListener1381(this::onPartyState);
        sadnesSocketClient.setListener1380(this::onPartyResult);
        sadnesSocketClient.setListener1371(this::onInviteReceived);
        sadnesSocketClient.setListener1377(this::onPartyLooks);
        sadnesSocketClient.setListener1367(this::onUserSearchResult);
    }

    public void createParty() {
        this.connection.sendPacket1368();
    }

    public void invite(String string) {
        this.connection.sendPacket1370(string);
    }

    public void join(String string) {
        this.connection.sendPacket1372(string);
    }

    public void leave() {
        this.connection.sendPacket1374();
    }

    public void kick(int i) {
        this.connection.sendPacket1373(i);
    }

    public void disband() {
        this.connection.sendPacket1369();
    }

    public boolean inParty() {
        return this.party.isInParty();
    }

    public PlayerLook getLook(int i) {
        return this.memberLooks.getOrDefault(i, PlayerLook.NONE);
    }

    public void setLook(PlayerLook playerLook) {
        if (!playerLook.equals(this.currentLook)) {
            this.currentLook = playerLook;
            this.connection.sendPacket1376(playerLook);
        }
    }

    public String getPartyName() {
        return this.party.getCode();
    }

    public int getLeaderUid() {
        return this.party.getLeaderUid();
    }

    public boolean isLeader() {
        if (this.party.isInParty()) {
            int i = this.party.getLeaderUid();
            int i2 = Profile.getUid();
            return i == i2;
        } else {
            return false;
        }
    }

    public List<PartyMember> getMembers() {
        return this.party.getMembers();
    }

    public List<PartyMemberView> getMemberViews() {
        ArrayList arrayList = new ArrayList();
        Iterator iterator2 = this.party.getMembers().iterator();
        if (!iterator2.hasNext()) {
            return arrayList;
        }

        do {
            PartyMember partyMember = (PartyMember)iterator2.next();
            PartyStateCache partyStateCache = this.stateCache;
            String string = partyMember.username();
            PlayerState playerState = partyStateCache.getByLogin(string);
            PartyMemberView partyMemberView = new PartyMemberView(partyMember, playerState);
            arrayList.add(partyMemberView);
        } while (iterator2.hasNext());

        return arrayList;
    }

    public PartyMember findMember(String string) {
        for (PartyMember partyMember : this.party.getMembers()) {
            if (partyMember.username().equalsIgnoreCase(string)) {
                return partyMember;
            }
        }

        return null;
    }

    public String getPendingInviter() {
        PartyInviteReceivedPacket partyInviteReceivedPacket = this.pendingInvite;
        return partyInviteReceivedPacket == null ? null : partyInviteReceivedPacket.getInviter();
    }

    public boolean acceptInvite() {
        PartyInviteReceivedPacket partyInviteReceivedPacket = this.pendingInvite;
        if (partyInviteReceivedPacket == null) {
            return false;
        }

        this.pendingInvite = null;
        this.connection.sendPacket1372(partyInviteReceivedPacket.getCode());
        return true;
    }

    public List<String> suggestInvitees(String string) {
        String string2 = string == null ? "" : string.toLowerCase(Locale.US);
        if (!string2.equals(this.lastSearchQuery) || this.searchTimer.hasPassed(3000.0)) {
            this.lastSearchQuery = string2;
            this.searchTimer.reset();
            this.connection.sendText(string2);
        }

        String string3 = Profile.getUsername();
        ArrayList arrayList = new ArrayList();

        for (UserInfo userInfo : this.searchResults) {
            String string4 = userInfo.login();
            if (string4.regionMatches(true, 0, string2, 0, string2.length()) && !string4.equalsIgnoreCase(string3) && this.findMember(string4) == null) {
                arrayList.add(string4);
            }
        }

        return arrayList;
    }

    public String getMemberServer(String string) {
        for (PartyMemberView partyMemberView : this.getMemberViews()) {
            if (partyMemberView.username().equalsIgnoreCase(string)) {
                if (partyMemberView.hasState()) {
                    return partyMemberView.state().getUsername();
                }

                return "";
            }
        }

        return "";
    }

    public List<String> suggestMembers(String string) {
        String string2 = string == null ? "" : string;
        ArrayList arrayList = new ArrayList();

        for (PartyMember partyMember : this.party.getMembers()) {
            if (partyMember.username().regionMatches(true, 0, string2, 0, string2.length())) {
                arrayList.add(partyMember.username());
            }
        }

        return arrayList;
    }

    public void refreshSearch() {
        this.searchTimer.reset();
        this.connection.sendText(this.lastSearchQuery);
    }

    public String getAvatarUrl(String string) {
        if (string == null) {
            return "";
        }

        if (string.equalsIgnoreCase(Profile.getUsername())) {
            return Profile.getAvatarUrl();
        }

        String string2 = this.avatarUrls.get(string.toLowerCase(Locale.US));
        return string2 == null ? "" : string2;
    }

    public void resync() {
        this.connection.sendPacket1376(this.currentLook);
        this.connection.sendPacket1382();
    }

    private void onPartyState(PartyStatePacket partyStatePacket) {
        PartyStatePacket partyStatePacket2 = this.party;
        this.party = partyStatePacket;
        ArrayList arrayList;
        PartyStateCache partyStateCache;
        if (partyStatePacket.isInParty()) {
            int i = partyStatePacket.getMembers().size();
            arrayList = new ArrayList(i);
            Iterator iterator2 = partyStatePacket.getMembers().iterator();

            while (true) {
                boolean flag = iterator2.hasNext();
                partyStateCache = this.stateCache;
                if (!flag) {
                    break;
                }

                String string = ((PartyMember)iterator2.next()).username();
                arrayList.add(string);
            }
        } else {
            Map map = Map.of();
            this.memberLooks = map;
            int i2 = partyStatePacket.getMembers().size();
            arrayList = new ArrayList(i2);
            Iterator iterator3 = partyStatePacket.getMembers().iterator();

            while (true) {
                boolean flag2 = iterator3.hasNext();
                partyStateCache = this.stateCache;
                if (!flag2) {
                    break;
                }

                String string2 = ((PartyMember)iterator3.next()).username();
                arrayList.add(string2);
            }
        }

        partyStateCache.setSubscribed(arrayList);
        this.connection.sendTargets(arrayList);
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient != null) {
            Runnable runnable = () -> this.announcePartyChange(partyStatePacket2, partyStatePacket);
            minecraftClient.execute(runnable);
        }
    }

    private void onPartyLooks(PartyLooksPacket partyLooksPacket) {
        HashMap hashMap = new HashMap();

        for (PartyLooksPacket.Entry entry : partyLooksPacket.getLooks()) {
            hashMap.put(entry.uid(), entry.look());
        }

        this.memberLooks = hashMap;
    }

    private void announcePartyChange(PartyStatePacket partyStatePacket, PartyStatePacket partyStatePacket2) {
        if (partyStatePacket2.isInParty()) {
            StringBuilder builder = new StringBuilder();

            for (PartyMember partyMember : partyStatePacket2.getMembers()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }

                String string = partyMember.username();
                builder.append(string);
                int i = partyMember.uid();
                int i2 = partyStatePacket2.getLeaderUid();
                if (i == i2) {
                    StringBuilder builder2 = builder.append(' ');
                    String string2 = Language.translate("command.party.tag.leader");
                    builder2.append(string2);
                }
            }

            String string3 = Language.translate("command.party.announce.roster");
            Object[] objects = new Object[2];
            String string4 = partyStatePacket2.getCode();
            Array.set(objects, 0, string4);
            String string5 = builder.toString();
            Array.set(objects, 1, string5);
            ChatUtil.print(string3, objects);
        } else if (partyStatePacket.isInParty()) {
            String string6 = Language.translate("command.party.announce.left");
            Object[] objects2 = new Object[0];
            ChatUtil.print(string6, objects2);
        }
    }

    private void onInviteReceived(PartyInviteReceivedPacket partyInviteReceivedPacket) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient != null) {
            Runnable runnable = () -> {
                if (!PartyPrefs.isIgnored(partyInviteReceivedPacket.getInviter())) {
                    this.pendingInvite = partyInviteReceivedPacket;
                    NotificationUtil.info(NotificationSources.party(), String.format(Language.translate("command.party.invite.got"), partyInviteReceivedPacket.getInviter()));
                    ChatUtil.print(Language.translate("command.party.invite.accept"), partyInviteReceivedPacket.getInviter());
                }
            };
            minecraftClient.execute(runnable);
        }
    }

    private void onUserSearchResult(UserSearchResultPacket userSearchResultPacket) {
        for (UserInfo userInfo : userSearchResultPacket.getUsers()) {
            if (!userInfo.avatarUrl().isEmpty()) {
                this.avatarUrls.put(userInfo.login().toLowerCase(Locale.US), userInfo.avatarUrl());
            }
        }

        if (!userSearchResultPacket.isLookup()) {
            this.searchResults = userSearchResultPacket.getUsers();
        }

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient != null) {
            minecraftClient.execute(PartyManager::refreshChatSuggestions);
        }
    }

    private static void refreshChatSuggestions() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient != null && minecraftClient.currentScreen instanceof ChatScreen chatScreen) {
            ChatInputSuggestor chatInputSuggestor = ((ChatScreenAccessor)chatScreen).sadnes$getSuggestor();
            if (chatInputSuggestor != null) {
                chatInputSuggestor.refresh();
            }
        }
    }

    private void onPartyResult(PartyResultPacket partyResultPacket) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient != null && !partyResultPacket.getMessage().isBlank()) {
            Runnable runnable = () -> {
                if (partyResultPacket.isSuccess()) {
                    NotificationUtil.info(NotificationSources.party(), partyResultPacket.getMessage());
                } else {
                    NotificationUtil.warning(NotificationSources.party(), partyResultPacket.getMessage());
                }
            };
            minecraftClient.execute(runnable);
        }
    }
}
