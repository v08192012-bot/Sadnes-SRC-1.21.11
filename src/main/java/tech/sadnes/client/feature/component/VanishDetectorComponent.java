package tech.sadnes.client.feature.component;

import com.mojang.brigadier.suggestion.Suggestion;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Team;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.feature.impl.hud.StaffList;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.chat.ChatNameUtil;

public final class VanishDetectorComponent extends FeatureComponent {
    private final ConcurrentLinkedQueue<VanishDetectorComponent.PendingEvent> pendingEvents = new ConcurrentLinkedQueue<>();
    private final Set<Integer> pendingSuggestionIds = ConcurrentHashMap.newKeySet();
    private final Map<Long, Long> interactedBlocks = new ConcurrentHashMap<>();
    private volatile List<String> suggestedNames;
    private volatile long lastInteractionMs;
    private final Map<UUID, String> knownNames = new HashMap<>();
    private final Map<UUID, Long> firstSeenMs = new HashMap<>();
    private final Set<String> staffNames = new HashSet<>();
    private final Map<String, VanishDetectorComponent.StaffEntry> detectedStaff = new HashMap<>();
    private final Deque<VanishDetectorComponent.PendingRemoval> pendingRemovals = new ArrayDeque<>();
    private final Map<UUID, Long> leaveGraceUntil = new HashMap<>();
    private final Map<UUID, Long> lastStaffActivity = new HashMap<>();
    private final Map<UUID, Long> lastGamemodeChange = new HashMap<>();
    private final Deque<VanishDetectorComponent.ChatEntry> recentChat = new ArrayDeque<>();
    private final Deque<VanishDetectorComponent.Evidence> evidence = new ArrayDeque<>();
    private final Map<String, Long> evidenceCooldowns = new HashMap<>();
    private final Deque<VanishDetectorComponent.Explosion> explosions = new ArrayDeque<>();
    private final Map<Long, Long> openDoors = new HashMap<>();
    private final Map<String, Deque<Long>> particleBursts = new HashMap<>();
    private boolean leaveMessageSeen;
    private boolean active;
    private int tickCounter;
    private int nextSuggestionId = 30000;
    private long lastScreenOpenMs;
    private long lastAlertMs;
    private long alertUntilMs;
    private String alertName = "";

    public static VanishDetectorComponent getInstance() {
        return Sadnes.getInstance().getComponentManager().getComponent(VanishDetectorComponent.class);
    }

    public VanishDetectorComponent.VanishState getVanishState(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        boolean flag;
        if (System.currentTimeMillis() < this.alertUntilMs) {
            flag = this.alertName.equalsIgnoreCase(string);
        } else {
            flag = false;
        }

        VanishDetectorComponent.StaffEntry staffEntry = this.detectedStaff.get(string2);
        UUID uuid = staffEntry != null && staffEntry.getUuid() != null ? staffEntry.getUuid() : this.resolveUuid(string);
        boolean flag2;
        if (staffEntry != null) {
            flag2 = this.isInWorld(uuid, string);
        } else {
            flag2 = false;
        }

        if (flag || this.changedGamemodeRecently(uuid) || flag2) {
            return VanishDetectorComponent.VanishState.WATCHING;
        } else {
            return staffEntry != null ? VanishDetectorComponent.VanishState.HIDDEN : VanishDetectorComponent.VanishState.NONE;
        }
    }

    public List<String> getVanishedNames() {
        ArrayList arrayList = new ArrayList();

        for (VanishDetectorComponent.StaffEntry staffEntry : this.detectedStaff.values()) {
            arrayList.add(staffEntry.getName());
        }

        boolean flag;
        if (System.currentTimeMillis() < this.alertUntilMs) {
            flag = !this.alertName.isBlank();
        } else {
            flag = false;
        }

        if (flag && !this.detectedStaff.containsKey(this.alertName.toLowerCase(Locale.ROOT))) {
            arrayList.add(this.alertName);
        }

        return arrayList;
    }

    public boolean isStaff(String string) {
        if (string != null && !string.isBlank()) {
            if (this.staffNames.contains(string.toLowerCase(Locale.ROOT))) {
                return true;
            }

            Team team = mc.world == null ? null : mc.world.getScoreboard().getScoreHolderTeam(string);
            return team != null && hasStaffPrefix(team.getPrefix().getString());
        } else {
            return false;
        }
    }

    private boolean shouldRun() {
        StaffList staffList = StaffList.getInstance();
        return mc.player != null && mc.world != null && mc.getNetworkHandler() != null && ServerManager.isDonut() && staffList != null && staffList.isEnabled();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.reset();
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (this.shouldRun()) {
            Packet packet = packetEvent.getPacket();
            if (packetEvent.isSend()) {
                this.handleOutgoing(packet);
            } else if (packet instanceof CommandSuggestionsS2CPacket commandSuggestionsS2CPacket && this.pendingSuggestionIds.remove(commandSuggestionsS2CPacket.id())) {
                ArrayList arrayList = new ArrayList();

                for (Suggestion suggestion : commandSuggestionsS2CPacket.getSuggestions().getList()) {
                    String string = suggestion.getText();
                    if (string != null && !string.isBlank()) {
                        arrayList.add(string.trim());
                    }
                }

                this.suggestedNames = arrayList;
                packetEvent.cancel();
            } else {
                this.handleIncoming(packet);
            }
        }
    }

    private void handleOutgoing(Packet<?> packet) {
        if (packet instanceof PlayerInteractBlockC2SPacket playerInteractBlockC2SPacket) {
            this.lastInteractionMs = System.currentTimeMillis();
            BlockPos blockPos = playerInteractBlockC2SPacket.getBlockHitResult().getBlockPos();
            BlockPos blockPos2 = blockPos.offset(playerInteractBlockC2SPacket.getBlockHitResult().getSide());
            long l = System.currentTimeMillis() + 6000L;
            Identifier identifier = Registries.ITEM.getId(mc.player.getStackInHand(playerInteractBlockC2SPacket.getHand()).getItem());
            String string = identifier.getPath();
            this.markInteractedBlock(blockPos, string, l);
            this.markInteractedBlock(blockPos2, string, l);
        } else {
            boolean flag;
            if (!(packet instanceof PlayerActionC2SPacket) && !(packet instanceof PlayerInteractItemC2SPacket) && !(packet instanceof PlayerInteractEntityC2SPacket)) {
                flag = packet instanceof HandSwingC2SPacket;
            } else {
                flag = true;
            }

            if (flag) {
                this.lastInteractionMs = System.currentTimeMillis();
            }
        }
    }

    private void markInteractedBlock(BlockPos blockPos, String string, long l) {
        this.interactedBlocks.put(blockPos.asLong(), l);
        boolean flag;
        if ((!string.endsWith("_door") || string.endsWith("trapdoor"))
            && !string.contains("sunflower")
            && !string.contains("lilac")
            && !string.contains("rose_bush")
            && !string.contains("peony")
            && !string.contains("tall_grass")
            && !string.contains("large_fern")) {
            flag = string.contains("pitcher_plant");
        } else {
            flag = true;
        }

        if (flag) {
            this.interactedBlocks.put(blockPos.up().asLong(), l);
            this.interactedBlocks.put(blockPos.down().asLong(), l);
        }

        if (string.endsWith("_bed")) {
            this.interactedBlocks.put(blockPos.add(1, 0, 0).asLong(), l);
            this.interactedBlocks.put(blockPos.add(-1, 0, 0).asLong(), l);
            this.interactedBlocks.put(blockPos.add(0, 0, 1).asLong(), l);
            this.interactedBlocks.put(blockPos.add(0, 0, -1).asLong(), l);
        }
    }

    private void handleIncoming(Packet<?> packet) {
        if (packet instanceof BundleS2CPacket bundleS2CPacket) {
            for (Packet packet2 : bundleS2CPacket.getPackets()) {
                this.handleIncoming(packet2);
            }
        } else if (packet instanceof PlayerRemoveS2CPacket playerRemoveS2CPacket) {
            if (playerRemoveS2CPacket.profileIds().size() < 4) {
                for (UUID uuid : playerRemoveS2CPacket.profileIds()) {
                    this.pendingEvents.offer(VanishDetectorComponent.PendingEvent.of(VanishDetectorComponent.EventKind.TAB_REMOVE, uuid));
                }
            }
        } else if (packet instanceof PlayerListS2CPacket playerListS2CPacket && playerListS2CPacket.getActions().contains(Action.UPDATE_LISTED)) {
            List<Entry> entries = new ArrayList<>();

            for (Entry entry : playerListS2CPacket.getEntries()) {
                if (!entry.listed()) {
                    entries.add(entry);
                }
            }

            int i = entries.size();
            if (i > 0 && i < 4) {
                for (Entry entry2 : entries) {
                    this.pendingEvents.offer(VanishDetectorComponent.PendingEvent.of(VanishDetectorComponent.EventKind.TAB_HIDE, entry2.profileId()));
                }
            }
        } else if (packet instanceof PlayerListS2CPacket playerListS2CPacket2 && playerListS2CPacket2.getActions().contains(Action.UPDATE_GAME_MODE)) {
            if (playerListS2CPacket2.getActions().contains(Action.ADD_PLAYER)) {
                return;
            }

            for (Entry entry3 : playerListS2CPacket2.getEntries()) {
                if (entry3.gameMode() != null) {
                    this.pendingEvents
                        .offer(
                            new VanishDetectorComponent.PendingEvent(
                                VanishDetectorComponent.EventKind.GAMEMODE, entry3.profileId(), -1, Vec3d.ZERO, entry3.gameMode().asString()
                            )
                        );
                }
            }
        } else if (packet instanceof GameMessageS2CPacket gameMessageS2CPacket) {
            String string = gameMessageS2CPacket.content().getString();
            if (!string.isBlank()) {
                this.pendingEvents.offer(new VanishDetectorComponent.PendingEvent(VanishDetectorComponent.EventKind.SYSTEM_CHAT, null, -1, Vec3d.ZERO, string));
            }

            String string2 = extractLeftPlayerName(gameMessageS2CPacket.content());
            if (!string2.isBlank()) {
                this.pendingEvents.offer(new VanishDetectorComponent.PendingEvent(VanishDetectorComponent.EventKind.PLAYER_LEFT, null, -1, Vec3d.ZERO, string2));
                return;
            }
        } else {
            if (packet instanceof EntityTrackerUpdateS2CPacket entityTrackerUpdateS2CPacket) {
                this.pendingEvents
                    .offer(new VanishDetectorComponent.PendingEvent(VanishDetectorComponent.EventKind.METADATA, null, entityTrackerUpdateS2CPacket.id(), Vec3d.ZERO, ""));
                return;
            }

            if (packet instanceof ExplosionS2CPacket explosionS2CPacket) {
                this.pendingEvents
                    .offer(
                        new VanishDetectorComponent.PendingEvent(
                            VanishDetectorComponent.EventKind.EXPLOSION, null, -1, explosionS2CPacket.center(), Float.toString(explosionS2CPacket.radius())
                        )
                    );
                return;
            }

            if (packet instanceof PlaySoundFromEntityS2CPacket playSoundFromEntityS2CPacket) {
                this.pendingEvents
                    .offer(
                        new VanishDetectorComponent.PendingEvent(
                            VanishDetectorComponent.EventKind.ENTITY_SOUND, null, playSoundFromEntityS2CPacket.getEntityId(), Vec3d.ZERO, playSoundFromEntityS2CPacket.getSound().value().id().toString()
                        )
                    );
                return;
            }

            if (packet instanceof ParticleS2CPacket particleS2CPacket) {
                Identifier identifier = Registries.PARTICLE_TYPE.getId(particleS2CPacket.getParameters().getType());
                Vec3d vec3d = new Vec3d(particleS2CPacket.getX(), particleS2CPacket.getY(), particleS2CPacket.getZ());
                this.pendingEvents
                    .offer(
                        new VanishDetectorComponent.PendingEvent(
                            VanishDetectorComponent.EventKind.PARTICLE, null, -1, vec3d, identifier == null ? "" : identifier.toString()
                        )
                    );
                return;
            }

            if (packet instanceof PlaySoundS2CPacket playSoundS2CPacket) {
                Vec3d vec3d2 = new Vec3d(playSoundS2CPacket.getX(), playSoundS2CPacket.getY(), playSoundS2CPacket.getZ());
                this.pendingEvents
                    .offer(
                        new VanishDetectorComponent.PendingEvent(
                            VanishDetectorComponent.EventKind.POSITIONAL_SOUND, null, -1, vec3d2, playSoundS2CPacket.getSound().value().id().toString()
                        )
                    );
                return;
            }
        }
    }

    private static String extractLeftPlayerName(Text text) {
        if (text.getContent() instanceof TranslatableTextContent translatableTextContent) {
            if (!"multiplayer.player.left".equals(translatableTextContent.getKey())) {
                return "";
            }

            Object[] objects = translatableTextContent.getArgs();
            if (objects.length != 0) {
                if (objects[0] == null) {
                    return "";
                }

                Object object = objects[0];
                if (object instanceof Text) {
                    return ((Text)object).getString();
                }

                return String.valueOf(objects[0]);
            }
        }

        return "";
    }

    @EventHandler
    public void onClientTick(WorldTickEvent worldTickEvent) {
        if (!this.shouldRun()) {
            if (this.active) {
                this.reset();
            }
        } else {
            this.active = true;
            this.tickCounter++;
            long l = System.currentTimeMillis();
            if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                this.lastScreenOpenMs = l;
            }

            for (int i = 0; i < 512; i++) {
                VanishDetectorComponent.PendingEvent pendingEvent = this.pendingEvents.poll();
                if (pendingEvent == null) {
                    break;
                }

                this.processEvent(pendingEvent);
            }

            while (this.pendingEvents.size() > 4096) {
                this.pendingEvents.poll();
            }

            this.processPendingRemovals();
            this.processSuggestions();
            if (this.tickCounter % 100 == 0) {
                this.requestNameSuggestions();
            }

            if (this.tickCounter % 10 == 0) {
                this.refreshTabList();
                this.updateVisibleStaff();
                this.confirmDetections();
            }

            if (this.tickCounter % 5 == 0) {
                this.scanInvisiblePlayers();
            }

            this.cleanup(l);
        }
    }

    private void processEvent(VanishDetectorComponent.PendingEvent pendingEvent) {
        switch (pendingEvent.kind()) {
            case TAB_REMOVE:
                this.onTabRemove(pendingEvent.profileId());
                return;
            case TAB_HIDE:
                this.onTabHide(pendingEvent.profileId());
                return;
            case PLAYER_LEFT:
                this.leaveMessageSeen = true;
                this.onPlayerLeft(pendingEvent.detail());
                return;
            case SYSTEM_CHAT:
                this.addChatMessage(pendingEvent.detail());
                return;
            case METADATA:
                this.checkEntityMetadata(pendingEvent.entityId());
                return;
            case POSITIONAL_SOUND:
                this.handlePositionalSound(pendingEvent);
                return;
            case ENTITY_SOUND:
                this.handleEntitySound(pendingEvent);
                return;
            case PARTICLE:
                this.handleParticle(pendingEvent);
                return;
            case EXPLOSION:
                this.recordExplosion(pendingEvent);
                return;
            case GAMEMODE:
                this.onGamemodeChange(pendingEvent.profileId(), pendingEvent.detail());
        }
    }

    private void refreshTabList() {
        long l = System.currentTimeMillis();

        for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getListedPlayerListEntries()) {
            UUID uuid = playerListEntry.getProfile().id();
            if (uuid != null && !uuid.equals(mc.player.getUuid())) {
                this.firstSeenMs.putIfAbsent(uuid, l);
                String string = playerListEntry.getProfile().name();
                if (string != null) {
                    this.knownNames.putIfAbsent(uuid, string);
                    Team team = mc.world.getScoreboard().getScoreHolderTeam(string);
                    if (team != null && hasStaffPrefix(team.getPrefix().getString())) {
                        this.staffNames.add(string.toLowerCase(Locale.ROOT));
                    }

                    VanishDetectorComponent.StaffEntry staffEntry = this.detectedStaff.get(string.toLowerCase(Locale.ROOT));
                    boolean flag = staffEntry != null && !staffEntry.isSpectator() && l - staffEntry.getDetectedAt() >= 1000L;
                    if (flag) {
                        this.detectedStaff.remove(string.toLowerCase(Locale.ROOT));
                    }
                }
            }
        }

        if (this.firstSeenMs.size() > 1024 || this.knownNames.size() > 1024) {
            HashSet hashSet = new HashSet();

            for (PlayerListEntry playerListEntry2 : mc.getNetworkHandler().getPlayerList()) {
                hashSet.add(playerListEntry2.getProfile().id());
            }

            this.firstSeenMs.keySet().retainAll(hashSet);
            this.knownNames.keySet().retainAll(hashSet);
        }
    }

    private boolean isEligibleStaff(UUID uuid, String string) {
        Long longValue = uuid == null ? null : this.firstSeenMs.get(uuid);
        boolean flag = longValue != null && System.currentTimeMillis() - longValue >= 1500L;
        return isRealPlayer(uuid, string) && this.isStaff(string) && flag;
    }

    private void onTabRemove(UUID uuid2) {
        if (uuid2 != null && !uuid2.equals(mc.player.getUuid())) {
            String string = this.resolveName(uuid2);
            if (this.isEligibleStaff(uuid2, string) && ChatNameUtil.isValidUsername(string)) {
                this.pendingRemovals.removeIf(pendingRemoval -> pendingRemoval.uuid().equals(uuid2));
                this.pendingRemovals.addLast(new VanishDetectorComponent.PendingRemoval(uuid2, string, this.tickCounter + 20));
            }
        }
    }

    private void onTabHide(UUID uuid) {
        if (uuid != null && !uuid.equals(mc.player.getUuid())) {
            String string = this.resolveName(uuid);
            if (this.isEligibleStaff(uuid, string) && ChatNameUtil.isValidUsername(string)) {
                if (!this.hasRecentLeaveMessage(string)) {
                    this.markVanished(uuid, string, false);
                }
            }
        }
    }

    private String resolveName(UUID uuid) {
        String string = this.knownNames.get(uuid);
        if (string != null && !string.isBlank()) {
            return string;
        }

        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(uuid);
        return playerListEntry != null ? playerListEntry.getProfile().name() : null;
    }

    private UUID resolveUuid(String string) {
        for (java.util.Map.Entry entry : this.knownNames.entrySet()) {
            if (((String)entry.getValue()).equalsIgnoreCase(string)) {
                return (UUID)entry.getKey();
            }
        }

        return null;
    }

    private void onPlayerLeft(String string) {
        long l = System.currentTimeMillis();

        for (java.util.Map.Entry entry : this.knownNames.entrySet()) {
            if (ChatNameUtil.containsName(string, (String)entry.getValue())) {
                UUID uuid2 = (UUID)entry.getKey();
                this.leaveGraceUntil.put(uuid2, l + 5000L);
                this.pendingRemovals.removeIf(pendingRemoval -> pendingRemoval.uuid().equals(uuid2));
                this.detectedStaff.remove(((String)entry.getValue()).toLowerCase(Locale.ROOT));
            }
        }
    }

    private void processPendingRemovals() {
        while (!this.pendingRemovals.isEmpty() && this.pendingRemovals.peekFirst().dueTick() <= this.tickCounter) {
            VanishDetectorComponent.PendingRemoval pendingRemoval = this.pendingRemovals.removeFirst();
            if (mc.getNetworkHandler().getPlayerListEntry(pendingRemoval.uuid()) == null
                && this.leaveGraceUntil.getOrDefault(pendingRemoval.uuid(), 0L) <= System.currentTimeMillis()) {
                if (this.hasRecentLeaveMessage(pendingRemoval.name())) {
                    this.leaveMessageSeen = true;
                } else {
                    PlayerEntity playerEntity = mc.world.getPlayerByUuid(pendingRemoval.uuid());
                    if ((playerEntity == null || playerEntity.isRemoved()) && !this.leaveMessageSeen) {
                        if (this.wasRecentlyActive(pendingRemoval.uuid())) {
                            this.markVanished(pendingRemoval.uuid(), pendingRemoval.name(), false);
                            this.addEvidence(VanishDetectorComponent.EvidenceType.VANISH, pendingRemoval.name(), true, 30);
                        }
                    } else {
                        this.markVanished(pendingRemoval.uuid(), pendingRemoval.name(), false);
                    }
                }
            }
        }
    }

    private void addChatMessage(String string) {
        long l = System.currentTimeMillis();
        this.recentChat.addLast(new VanishDetectorComponent.ChatEntry(string, l));

        while (!this.recentChat.isEmpty() && (l - this.recentChat.peekFirst().atMs() > 8000L || this.recentChat.size() > 64)) {
            this.recentChat.removeFirst();
        }
    }

    private boolean hasRecentLeaveMessage(String string) {
        if (string != null && !string.isBlank()) {
            long l = System.currentTimeMillis();

            for (VanishDetectorComponent.ChatEntry chatEntry : this.recentChat) {
                if (l - chatEntry.atMs() <= 8000L && ChatNameUtil.isLeaveMessage(chatEntry.text(), string)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void requestNameSuggestions() {
        int i = this.nextSuggestionId++;
        if (this.nextSuggestionId > 40000) {
            this.nextSuggestionId = 30000;
        }

        this.pendingSuggestionIds.add(i);

        while (this.pendingSuggestionIds.size() > 8) {
            this.pendingSuggestionIds.remove(this.pendingSuggestionIds.iterator().next());
        }

        mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(i, "minecraft:msg "));
    }

    private void processSuggestions() {
        List<String> strings = this.suggestedNames;
        if (strings != null) {
            this.suggestedNames = null;
            HashSet hashSet = new HashSet();

            for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getPlayerList()) {
                hashSet.add(playerListEntry.getProfile().name().toLowerCase(Locale.ROOT));
            }

            String string = mc.player.getGameProfile().name();
            List<String> strings2 = new ArrayList<>();

            for (String string2 : strings) {
                boolean flag;
                if (this.isStaff(string2) && ChatNameUtil.isValidUsername(string2) && !string2.equalsIgnoreCase(string) && !hashSet.contains(string2.toLowerCase(Locale.ROOT))) {
                    flag = !this.hasRecentLeaveMessage(string2);
                } else {
                    flag = false;
                }

                if (flag) {
                    strings2.add(string2);
                }
            }

            if (!strings2.isEmpty() && strings2.size() <= 3) {
                for (String string3 : strings2) {
                    this.markVanished(this.resolveUuid(string3), string3, false);
                }
            }
        }
    }

    private void onGamemodeChange(UUID uuid, String string) {
        if (uuid != null && !uuid.equals(mc.player.getUuid())) {
            String string2 = this.resolveName(uuid);
            if (isRealPlayer(uuid, string2) && this.isStaff(string2)) {
                long l = System.currentTimeMillis();
                this.lastStaffActivity.put(uuid, l);
                this.lastGamemodeChange.put(uuid, l);
                if (GameMode.SPECTATOR.asString().equals(string)) {
                    if (this.isEligibleStaff(uuid, string2)) {
                        this.markVanished(uuid, string2, true);
                    }
                } else {
                    VanishDetectorComponent.StaffEntry staffEntry = this.detectedStaff.get(string2.toLowerCase(Locale.ROOT));
                    if (staffEntry != null && staffEntry.isSpectator()) {
                        this.detectedStaff.remove(string2.toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
    }

    private void markVanished(UUID uuid, String string, boolean flag) {
        String string2 = string.toLowerCase(Locale.ROOT);
        VanishDetectorComponent.StaffEntry staffEntry = this.detectedStaff.get(string2);
        if (staffEntry == null || staffEntry.isSpectator() != flag) {
            this.detectedStaff
                .put(
                    string2,
                    new VanishDetectorComponent.StaffEntry(uuid, string, flag, System.currentTimeMillis(), this.tickCounter, this.hasPresenceProof(uuid, string))
                );
        }
    }

    private void confirmDetections() {
        for (VanishDetectorComponent.StaffEntry staffEntry : this.detectedStaff.values()) {
            if (!staffEntry.isAlerted() && staffEntry.getDetectedTick() < this.tickCounter) {
                boolean flag;
                label26: {
                    boolean flag2;
                    if (!this.hasPresenceProof(staffEntry.getUuid(), staffEntry.getName())) {
                        if (!staffEntry.isChecked()) {
                            flag = staffEntry.hasInitialProof();
                            break label26;
                        }

                        flag2 = false;
                    } else {
                        flag2 = true;
                    }

                    flag = flag2;
                }

                staffEntry.setChecked(true);
                if (flag) {
                    staffEntry.setAlerted(true);
                    this.triggerAlert(staffEntry.getName());
                }
            }
        }
    }

    private void triggerAlert(String string) {
        long l = System.currentTimeMillis();
        this.lastAlertMs = l;
        this.alertUntilMs = l + 7000L;
        this.alertName = string;
    }

    private boolean hasPresenceProof(UUID uuid, String string) {
        return this.isInWorld(uuid, string) || this.wasRecentlyActive(uuid);
    }

    private boolean wasRecentlyActive(UUID uuid) {
        Long longValue = uuid == null ? null : this.lastStaffActivity.get(uuid);
        return longValue != null && System.currentTimeMillis() - longValue < 600000L;
    }

    private boolean changedGamemodeRecently(UUID uuid) {
        Long longValue = uuid == null ? null : this.lastGamemodeChange.get(uuid);
        return longValue != null && System.currentTimeMillis() - longValue < 10000L;
    }

    private void updateVisibleStaff() {
        long l = System.currentTimeMillis();

        for (PlayerEntity playerEntity : mc.world.getPlayers()) {
            boolean flag;
            if (playerEntity != mc.player && isRealPlayer(playerEntity.getUuid(), playerEntity.getGameProfile().name())) {
                flag = this.isStaff(playerEntity.getGameProfile().name());
            } else {
                flag = false;
            }

            if (flag) {
                this.lastStaffActivity.put(playerEntity.getUuid(), l);
            }
        }
    }

    private static boolean isRealPlayer(UUID uuid, String string) {
        return uuid != null && uuid.version() != 2 && ChatNameUtil.isValidNameToken(string);
    }

    public static boolean hasStaffPrefix(String string) {
        if (string == null) {
            return false;
        }

        int i = string.length();
        int i2 = 0;

        while (i2 < i) {
            int i3 = string.codePointAt(i2);

            boolean flag = switch (i3) {
                case 9733, 9734, 10022, 10023, 10026, 10031, 10032, 11088 -> true;
                default -> false;
            };
            if (flag) {
                return true;
            }

            i2 += Character.charCount(i3);
        }

        return false;
    }

    private boolean isInWorld(UUID uuid, String string) {
        PlayerEntity playerEntity = uuid != null ? mc.world.getPlayerByUuid(uuid) : null;
        if (playerEntity == null) {
            for (PlayerEntity playerEntity2 : mc.world.getPlayers()) {
                if (playerEntity2.getGameProfile().name().equalsIgnoreCase(string)) {
                    playerEntity = playerEntity2;
                }
            }
        }

        return playerEntity != null && playerEntity != mc.player;
    }

    private void scanInvisiblePlayers() {
        for (PlayerEntity playerEntity : mc.world.getPlayers()) {
            this.checkInvisiblePlayer(playerEntity);
        }
    }

    private void checkEntityMetadata(int i) {
        if (mc.world.getEntityById(i) instanceof PlayerEntity playerEntity) {
            this.checkInvisiblePlayer(playerEntity);
        }
    }

    private void checkInvisiblePlayer(PlayerEntity playerEntity) {
        boolean flag;
        if (playerEntity != mc.player && playerEntity.isInvisible() && playerEntity.squaredDistanceTo(mc.player) <= 4096.0) {
            flag = this.isEligibleStaff(playerEntity.getUuid(), playerEntity.getGameProfile().name());
        } else {
            flag = false;
        }

        if (flag) {
            this.addEvidenceThrottled(VanishDetectorComponent.EvidenceType.INVISIBLE, playerEntity.getGameProfile().name(), true, 35, 5000L);
        }
    }

    private void handlePositionalSound(VanishDetectorComponent.PendingEvent pendingEvent) {
        if (isInteractionSound(pendingEvent.detail())) {
            Vec3d vec3d;
            boolean flag;
            label47: {
                vec3d = pendingEvent.pos();
                boolean flag2;
                if (this.isWithinRange(vec3d)
                    && !this.hasVisibleSourceNearby(vec3d)
                    && !this.isNearExplosion(vec3d)
                    && !this.isOpenDoorNearby(vec3d, pendingEvent.detail())
                    && System.currentTimeMillis() - this.lastInteractionMs >= 1000L
                    && !this.isInteractedBlock(vec3d)
                    && !this.isVillagerDoor(pendingEvent.detail(), vec3d)) {
                    if (this.recentlyOpenedScreen()) {
                        flag = !isContainerSound(pendingEvent.detail());
                        break label47;
                    }

                    flag2 = true;
                } else {
                    flag2 = false;
                }

                flag = flag2;
            }

            if (flag) {
                this.addEvidenceThrottled(VanishDetectorComponent.EvidenceType.SOUND, this.describeRelativePosition(vec3d), false, 14, 3000L);
            }
        }
    }

    private void handleEntitySound(VanishDetectorComponent.PendingEvent pendingEvent) {
        if (isInteractionSound(pendingEvent.detail())) {
            if (mc.world.getEntityById(pendingEvent.entityId()) instanceof PlayerEntity playerEntity) {
                boolean flag;
                if (playerEntity != mc.player && playerEntity.isInvisible() && playerEntity.squaredDistanceTo(mc.player) <= 4096.0) {
                    flag = this.isEligibleStaff(playerEntity.getUuid(), playerEntity.getGameProfile().name());
                } else {
                    flag = false;
                }

                if (flag) {
                    this.addEvidenceThrottled(VanishDetectorComponent.EvidenceType.SOUND, playerEntity.getGameProfile().name(), true, 16, 3000L);
                }
            }
        }
    }

    private void handleParticle(VanishDetectorComponent.PendingEvent pendingEvent) {
        if (isSuspiciousParticle(pendingEvent.detail())) {
            Vec3d vec3d = pendingEvent.pos();
            long l = System.currentTimeMillis();
            if (this.isWithinRange(vec3d)
                && !this.hasVisibleSourceNearby(vec3d)
                && !this.isNearExplosion(vec3d)
                && l - this.lastInteractionMs >= 900L
                && !this.isNearSmokeSource(vec3d, pendingEvent.detail())) {
                String string = stripNamespace(pendingEvent.detail());
                boolean flag;
                if (!string.equals("block")) {
                    flag = string.contains("smoke");
                } else {
                    flag = true;
                }

                if (!flag || !(vec3d.squaredDistanceTo(mc.player.getEntityPos()) < 6.25)) {
                    if (!flag || this.isParticleBurst(string, l)) {
                        this.addEvidenceThrottled(VanishDetectorComponent.EvidenceType.PARTICLE, this.describeRelativePosition(vec3d), false, 16, 3000L);
                    }
                }
            }
        }
    }

    private void addEvidenceThrottled(VanishDetectorComponent.EvidenceType evidenceType, String string, boolean flag, int i, long l) {
        long l2 = System.currentTimeMillis();
        String string2 = evidenceType.name() + "|" + string.toLowerCase(Locale.ROOT);
        if (l2 - this.evidenceCooldowns.getOrDefault(string2, 0L) >= l) {
            this.evidenceCooldowns.put(string2, l2);
            this.addEvidence(evidenceType, string, flag, i);
        }
    }

    private void addEvidence(VanishDetectorComponent.EvidenceType evidenceType, String string, boolean flag, int i) {
        long l = System.currentTimeMillis();
        this.evidence.addLast(new VanishDetectorComponent.Evidence(evidenceType, string, flag, i, l));

        while (!this.evidence.isEmpty() && l - this.evidence.peekFirst().timeMs() > 15000L) {
            this.evidence.removeFirst();
        }

        EnumMap<VanishDetectorComponent.EvidenceType, Integer> enumMap = this.collectEvidenceWeights(l);
        VanishDetectorComponent.Evidence evidence2 = null;

        for (VanishDetectorComponent.Evidence evidence3 : this.evidence) {
            if (evidence3.hasProof()) {
                evidence2 = evidence3;
            }
        }

        int i2 = 0;

        for (int i3 : enumMap.values()) {
            i2 += i3;
        }

        boolean flag2 = evidence2 != null && enumMap.size() >= 2 && i2 >= 35 && l - this.lastAlertMs >= 12000L;
        if (flag2) {
            this.triggerAlert(evidence2.subject());
        }
    }

    public int getSuspicionScore(String string) {
        long l = System.currentTimeMillis();
        boolean flag = false;

        for (VanishDetectorComponent.Evidence evidence2 : this.evidence) {
            boolean flag2 = l - evidence2.timeMs() <= 15000L;
            if (flag2 && evidence2.hasProof() && evidence2.subject().equalsIgnoreCase(string)) {
                flag = true;
            }
        }

        if (!flag) {
            return 0;
        }

        int i = 0;

        for (int i2 : this.collectEvidenceWeights(l).values()) {
            i += i2;
        }

        return i;
    }

    private EnumMap<VanishDetectorComponent.EvidenceType, Integer> collectEvidenceWeights(long l) {
        EnumMap<VanishDetectorComponent.EvidenceType, Integer> enumMap = new EnumMap<>(VanishDetectorComponent.EvidenceType.class);

        for (VanishDetectorComponent.Evidence evidence2 : this.evidence) {
            if (l - evidence2.timeMs() <= 15000L) {
                enumMap.merge(evidence2.type(), evidence2.weight(), Math::max);
            }
        }

        return enumMap;
    }

    private void recordExplosion(VanishDetectorComponent.PendingEvent pendingEvent) {
        double d;
        try {
            d = Double.parseDouble(pendingEvent.detail());
        } catch (NumberFormatException numberFormatException) {
            d = 4.0;
        }

        this.explosions.addLast(new VanishDetectorComponent.Explosion(pendingEvent.pos(), Math.max(2.0, d + 4.0), System.currentTimeMillis()));

        while (this.explosions.size() > 8) {
            this.explosions.removeFirst();
        }
    }

    private boolean isParticleBurst(String string, long l) {
        Deque<Long> longValues = this.particleBursts.computeIfAbsent(string, string2 -> new ArrayDeque<>());
        longValues.addLast(l);

        while (!longValues.isEmpty() && l - longValues.peekFirst() > 2000L) {
            longValues.removeFirst();
        }

        if (longValues.size() < 2) {
            return false;
        }

        longValues.clear();
        return true;
    }

    private boolean isWithinRange(Vec3d vec3d) {
        return vec3d.squaredDistanceTo(mc.player.getEntityPos()) <= 4096.0;
    }

    private boolean recentlyOpenedScreen() {
        return System.currentTimeMillis() - this.lastScreenOpenMs < 2500L;
    }

    private boolean isInteractedBlock(Vec3d vec3d) {
        Long longValue = this.interactedBlocks.get(BlockPos.ofFloored(vec3d).asLong());
        return longValue != null && longValue > System.currentTimeMillis();
    }

    private boolean isVillagerDoor(String string, Vec3d vec3d) {
        String string2 = stripNamespace(string);
        if ((!string2.contains("door") || string2.contains("trapdoor")) && !string2.contains("fence_gate")) {
            return false;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity.getType() == EntityType.VILLAGER && entity.getEntityPos().squaredDistanceTo(vec3d) <= 9.0) {
                return true;
            }
        }

        return false;
    }

    private boolean hasVisibleSourceNearby(Vec3d vec3d) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity != mc.player && !(entity.getEntityPos().squaredDistanceTo(vec3d) > 16.0)) {
                if (entity instanceof PlayerEntity && !((PlayerEntity)entity).isInvisible()) {
                    return true;
                }

                if (entity instanceof ProjectileEntity) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isNearExplosion(Vec3d vec3d) {
        for (VanishDetectorComponent.Explosion explosion : this.explosions) {
            if (explosion.center().squaredDistanceTo(vec3d) <= explosion.radius() * explosion.radius()) {
                return true;
            }
        }

        return false;
    }

    private boolean isNearSmokeSource(Vec3d vec3d, String string) {
        if (!stripNamespace(string).contains("smoke")) {
            return false;
        }

        BlockPos blockPos = BlockPos.ofFloored(vec3d);

        for (BlockPos blockPos2 : BlockPos.iterate(blockPos.add(-2, -2, -2), blockPos.add(2, 2, 2))) {
            String string2 = Registries.BLOCK.getId(mc.world.getBlockState(blockPos2).getBlock()).getPath();
            boolean flag;
            if (!string2.contains("campfire")
                && !string2.contains("furnace")
                && !string2.contains("smoker")
                && !string2.contains("torch")
                && !string2.contains("fire")
                && !string2.contains("candle")) {
                flag = string2.contains("respawn_anchor");
            } else {
                flag = true;
            }

            if (flag) {
                return true;
            }
        }

        return false;
    }

    private boolean isOpenDoorNearby(Vec3d vec3d, String string) {
        if (!stripNamespace(string).contains("door")) {
            return false;
        }

        BlockPos blockPos = BlockPos.ofFloored(vec3d);
        long l = System.currentTimeMillis();

        for (int i = -1; i <= 1; i++) {
            BlockPos blockPos2 = blockPos.add(0, i, 0);
            BlockState blockState = mc.world.getBlockState(blockPos2);
            if (blockState.contains(Properties.POWERED) && blockState.get(Properties.POWERED) || mc.world.isReceivingRedstonePower(blockPos2)) {
                this.openDoors.put(blockPos2.asLong(), l + 5000L);
                return true;
            }

            if (this.openDoors.getOrDefault(blockPos2.asLong(), 0L) > l) {
                return true;
            }
        }

        return false;
    }

    private String describeRelativePosition(Vec3d vec3d) {
        Vec3d vec3d2 = mc.player.getEntityPos();
        double d = vec3d.x - vec3d2.x;
        double d2 = vec3d.z - vec3d2.z;
        long l = Math.round(vec3d.distanceTo(vec3d2));
        String string = (d2 < -1.0 ? "N" : (d2 > 1.0 ? "S" : "")) + (d > 1.0 ? "E" : (d < -1.0 ? "W" : ""));
        return string.isEmpty() ? l + "m" : l + "m " + string;
    }

    private static boolean isInteractionSound(String string) {
        String string2 = stripNamespace(string);
        return string2.contains("chest.open")
            || string2.contains("chest.close")
            || string2.contains("chest.locked")
            || string2.contains("barrel.open")
            || string2.contains("barrel.close")
            || string2.contains("shulker_box.open")
            || string2.contains("shulker_box.close")
            || string2.contains("door.open")
            || string2.contains("door.close")
            || string2.contains("fence_gate.open")
            || string2.contains("fence_gate.close")
            || string2.contains("button.click")
            || string2.contains("lever.click");
    }

    private static boolean isSuspiciousParticle(String string) {
        String string2 = stripNamespace(string);
        return string2.contains("crit") || string2.contains("enchanted_hit") || string2.contains("damage_indicator") || string2.contains("smoke") || string2.equals("block");
    }

    private static boolean isContainerSound(String string) {
        String string2 = stripNamespace(string);
        return string2.contains("chest") || string2.contains("barrel") || string2.contains("shulker");
    }

    private static String stripNamespace(String string) {
        if (string == null) {
            return "unknown";
        }

        int i = string.indexOf(58);
        return i >= 0 && i + 1 < string.length() ? string.substring(i + 1).toLowerCase(Locale.ROOT) : string.toLowerCase(Locale.ROOT);
    }

    private void cleanup(long l) {
        this.detectedStaff.values().removeIf(staffEntry -> staffEntry.isSpectator() && this.isOffline(staffEntry.getUuid()));
        this.evidenceCooldowns.values().removeIf(longValue -> l - longValue > 60000L);
        this.interactedBlocks.values().removeIf(longValue2 -> longValue2 <= l);
        this.leaveGraceUntil.values().removeIf(longValue3 -> longValue3 <= l);
        this.lastStaffActivity.values().removeIf(longValue4 -> l - longValue4 >= 600000L);
        this.lastGamemodeChange.values().removeIf(longValue5 -> l - longValue5 >= 10000L);
        this.openDoors.values().removeIf(longValue6 -> longValue6 <= l);
        this.particleBursts.values().removeIf(longValues -> {
            while (!longValues.isEmpty() && l - longValues.peekFirst() > 2000L) {
                longValues.removeFirst();
            }

            return longValues.isEmpty();
        });

        while (!this.explosions.isEmpty() && l - this.explosions.peekFirst().timeMs() > 3000L) {
            this.explosions.removeFirst();
        }

        while (!this.evidence.isEmpty() && l - this.evidence.peekFirst().timeMs() > 15000L) {
            this.evidence.removeFirst();
        }
    }

    private boolean isOffline(UUID uuid) {
        return uuid == null || mc.getNetworkHandler().getPlayerListEntry(uuid) == null;
    }

    private void reset() {
        this.pendingEvents.clear();
        this.pendingSuggestionIds.clear();
        this.interactedBlocks.clear();
        this.suggestedNames = null;
        this.lastInteractionMs = 0L;
        this.knownNames.clear();
        this.firstSeenMs.clear();
        this.staffNames.clear();
        this.detectedStaff.clear();
        this.pendingRemovals.clear();
        this.leaveGraceUntil.clear();
        this.lastStaffActivity.clear();
        this.lastGamemodeChange.clear();
        this.recentChat.clear();
        this.evidence.clear();
        this.evidenceCooldowns.clear();
        this.explosions.clear();
        this.openDoors.clear();
        this.particleBursts.clear();
        this.leaveMessageSeen = false;
        this.active = false;
        this.tickCounter = 0;
        this.nextSuggestionId = 30000;
        this.lastScreenOpenMs = 0L;
        this.lastAlertMs = 0L;
        this.alertUntilMs = 0L;
        this.alertName = "";
    }

    record ChatEntry(String text, long atMs) {
    }

    enum EventKind {
        TAB_REMOVE,
        TAB_HIDE,
        PLAYER_LEFT,
        SYSTEM_CHAT,
        METADATA,
        POSITIONAL_SOUND,
        ENTITY_SOUND,
        PARTICLE,
        EXPLOSION,
        GAMEMODE;
    }

    record Evidence(VanishDetectorComponent.EvidenceType type, String subject, boolean hasProof, int weight, long timeMs) {
    }

    enum EvidenceType {
        VANISH,
        INVISIBLE,
        PARTICLE,
        SOUND;
    }

    record Explosion(Vec3d center, double radius, long timeMs) {
    }

    record PendingEvent(VanishDetectorComponent.EventKind kind, UUID profileId, int entityId, Vec3d pos, String detail) {
        static VanishDetectorComponent.PendingEvent of(VanishDetectorComponent.EventKind eventKind, UUID uuid) {
            return new VanishDetectorComponent.PendingEvent(eventKind, uuid, -1, Vec3d.ZERO, "");
        }
    }

    record PendingRemoval(UUID uuid, String name, int dueTick) {
    }

    static final class StaffEntry {
        private final UUID uuid;
        private final String name;
        private final boolean spectator;
        private final long detectedAt;
        private final int detectedTick;
        private final boolean initialProof;
        private boolean checked;
        private boolean alerted;

        public UUID getUuid() {
            return this.uuid;
        }

        public String getName() {
            return this.name;
        }

        public boolean isSpectator() {
            return this.spectator;
        }

        public long getDetectedAt() {
            return this.detectedAt;
        }

        public int getDetectedTick() {
            return this.detectedTick;
        }

        public boolean hasInitialProof() {
            return this.initialProof;
        }

        public boolean isChecked() {
            return this.checked;
        }

        public boolean isAlerted() {
            return this.alerted;
        }

        public StaffEntry(UUID uuid2, String string, boolean flag, long l, int i, boolean flag2) {
            this.uuid = uuid2;
            this.name = string;
            this.spectator = flag;
            this.detectedAt = l;
            this.detectedTick = i;
            this.initialProof = flag2;
        }

        public VanishDetectorComponent.StaffEntry setChecked(boolean flag) {
            this.checked = flag;
            return this;
        }

        public VanishDetectorComponent.StaffEntry setAlerted(boolean flag) {
            this.alerted = flag;
            return this;
        }
    }

    public enum VanishState {
        NONE,
        HIDDEN,
        WATCHING;
    }
}
