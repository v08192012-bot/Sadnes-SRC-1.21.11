package tech.sadnes.client.feature.impl.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.world.GameMode;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.HudFeature;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.VanishDetectorComponent;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.staff.StaffManager;
import tech.sadnes.client.ui.hud.HudEntry;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import ua.mintantileak.profile.Profile;
import ua.mintantileak.profile.Role;

@FeatureInfo(
    name = "StaffList",
    description = "feature.impl.hud.stafflist",
    displayName = "feature.impl.hud.stafflist",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_INFORMATION
)
public class StaffList extends HudFeature<StaffList.StaffEntry> {
    private static final float ICON_SIZE = 8.0F;
    private static final Radius HEAD_RADIUS = Radius.all(2.0F);
    private static final long UPDATE_INTERVAL_MS = 250L;
    private static final float MAX_NAME_WIDTH = 64.0F;
    private static final List<String> STAFF_PREFIX_KEYWORDS = List.of(
        "staff",
        "helper",
        "хелпер",
        "модер",
        "moder",
        "админ",
        "admin",
        "developer",
        "ютуб",
        "youtube",
        "yt",
        "ют",
        "помощник",
        "сотрудник",
        "заместитель",
        "куратор",
        "стажер",
        "стажёр"
    );
    private final MultiSetting show = new MultiSetting("stafflist.show").addOption("online", true).addOption("vanish", true).addOption("spectator", true);
    private final Map<String, StaffList.StaffEntry> entries = new HashMap<>();
    private long lastUpdate;

    public StaffList() {
        super(10.0F, 120.0F);
        this.show.getOption("online").setIcon(Icon.USER);
        this.show.getOption("vanish").setIcon(Icon.GHOST);
        this.show.getOption("spectator").setIcon(Icon.EYE);
    }

    @Override
    public String getTitle() {
        return "Staff";
    }

    @Override
    public Icon getIcon() {
        return Icon.USERS;
    }

    @Override
    public void collectEntries(List<StaffList.StaffEntry> staffEntries) {
        long l = System.currentTimeMillis();
        if (l - this.lastUpdate >= 250L) {
            this.updateStaff();
            this.lastUpdate = l;
        }

        for (StaffList.StaffEntry staffEntry : this.entries.values()) {
            if (staffEntry.active || staffEntry.animation.get() > 0.001) {
                staffEntries.add(staffEntry);
            }
        }

        this.entries.entrySet().removeIf(entry -> !entry.getValue().active && entry.getValue().animation.get() <= 0.001 && entry.getValue().animation.isFinished());
    }

    private void updateStaff() {
        Iterator iterator2 = this.entries.values().iterator();

        while (iterator2.hasNext()) {
            ((StaffList.StaffEntry)iterator2.next()).active = false;
        }

        if (mc.world != null && mc.player != null && mc.getNetworkHandler() != null) {
            Scoreboard scoreboard = mc.world.getScoreboard();
            if (ServerManager.isDonut()) {
                this.updateDonutStaff();
                return;
            }

            StaffManager staffManager = Sadnes.getInstance().getStaffManager();
            HashSet hashSet = new HashSet();

            for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getListedPlayerListEntries()) {
                String string = playerListEntry.getProfile().name();
                if (string != null && !string.isEmpty()) {
                    hashSet.add(string);
                    Team team = scoreboard.getScoreHolderTeam(string);
                    String string2 = team != null ? team.getPrefix().getString() : "";
                    String string3 = this.stripFormatting(string2);
                    MutableText mutableText = team != null ? team.getPrefix().copy() : null;
                    if (playerListEntry.getGameMode() == GameMode.SPECTATOR) {
                        if (this.show.has("spectator")) {
                            this.addEntry(string, string3, mutableText, StaffList.StaffStatus.SPECTATE, playerListEntry.getSkinTextures().body().texturePath());
                        }
                    } else if ((!string3.isEmpty() && (this.isStaffPrefix(string3) || string2.contains("YT")) || staffManager.isStaff(string)) && this.show.has("online")) {
                        this.addEntry(string, string3, mutableText, StaffList.StaffStatus.ONLINE, playerListEntry.getSkinTextures().body().texturePath());
                    }
                }
            }

            if (this.show.has("vanish")) {
                boolean flag;
                if (!ServerManager.isHolyWorld()) {
                    flag = ServerManager.isServer("holytime");
                } else {
                    flag = true;
                }

                for (Team team2 : scoreboard.getTeams()) {
                    String string4 = team2.getPrefix().getString();
                    String string5 = this.stripFormatting(string4);
                    if (!string5.isEmpty() && this.isStaffPrefix(string5) && (!flag || team2.getDisplayName().getString().contains("⚡"))) {
                        MutableText mutableText2 = team2.getPrefix().copy();

                        for (String string6 : team2.getPlayerList()) {
                            String string7 = this.stripFormatting(string6);
                            if (!string7.isEmpty() && !hashSet.contains(string7)) {
                                UUID uuid = Uuids.getOfflinePlayerUuid(string7);
                                this.addEntry(string7, string5, mutableText2, StaffList.StaffStatus.VANISH, DefaultSkinHelper.getSkinTextures(uuid).body().texturePath());
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateDonutStaff() {
        for (StaffList.StaffCandidate staffCandidate : this.collectDonutCandidates()) {
            if (staffCandidate.shown()) {
                Team team = mc.world.getScoreboard().getScoreHolderTeam(staffCandidate.name());
                boolean flag;
                if (team != null) {
                    flag = VanishDetectorComponent.hasStaffPrefix(team.getPrefix().getString());
                } else {
                    flag = false;
                }

                String string = flag ? this.stripFormatting(team.getPrefix().getString()) : "★";
                MutableText mutableText = flag ? team.getPrefix().copy() : Text.literal("★");
                PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(staffCandidate.name());
                Identifier identifier = playerListEntry != null
                    ? playerListEntry.getSkinTextures().body().texturePath()
                    : DefaultSkinHelper.getSkinTextures(Uuids.getOfflinePlayerUuid(staffCandidate.name())).body().texturePath();
                StaffList.StaffEntry staffEntry = this.addEntry(staffCandidate.name(), string, mutableText, staffCandidate.status(), identifier);
                int i = VanishDetectorComponent.getInstance().getSuspicionScore(staffCandidate.name());
                if (isAdmin() && i > 0) {
                    staffEntry.statusText = staffEntry.status.label + " " + i;
                }
            }
        }
    }

    private static boolean isAdmin() {
        Role role = Profile.getRole();
        return role != null && role.ordinal() >= Role.ADMIN.ordinal();
    }

    private List<StaffList.StaffCandidate> collectDonutCandidates() {
        HashSet hashSet = new HashSet();

        for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getListedPlayerListEntries()) {
            hashSet.add(playerListEntry.getProfile().name().toLowerCase(Locale.ROOT));
        }

        HashSet hashSet2 = new HashSet();

        for (PlayerEntity playerEntity : mc.world.getPlayers()) {
            if (playerEntity != mc.player) {
                hashSet2.add(playerEntity.getGameProfile().name().toLowerCase(Locale.ROOT));
            }
        }

        VanishDetectorComponent vanishDetectorComponent = VanishDetectorComponent.getInstance();
        HashSet<String> strings = new HashSet<>();

        for (Team team : mc.world.getScoreboard().getTeams()) {
            if (VanishDetectorComponent.hasStaffPrefix(team.getPrefix().getString())) {
                for (String string : team.getPlayerList()) {
                    strings.add(this.stripFormatting(string));
                }
            }
        }

        for (PlayerListEntry playerListEntry2 : mc.getNetworkHandler().getPlayerList()) {
            String string2 = playerListEntry2.getProfile().name();
            if (vanishDetectorComponent.isStaff(string2)) {
                strings.add(string2);
            }
        }

        strings.addAll(vanishDetectorComponent.getVanishedNames());
        String string3 = mc.player.getGameProfile().name();
        ArrayList<StaffList.StaffCandidate> staffCandidates = new ArrayList<>();

        for (String string4 : strings) {
            if (!string4.isEmpty() && !string4.equalsIgnoreCase(string3)) {
                String string5 = string4.toLowerCase(Locale.ROOT);
                PlayerListEntry playerListEntry3 = mc.getNetworkHandler().getPlayerListEntry(string4);
                boolean flag = hashSet.contains(string5);
                boolean flag2 = playerListEntry3 != null && playerListEntry3.getGameMode() == GameMode.SPECTATOR;
                boolean flag3 = hashSet2.contains(string5);
                VanishDetectorComponent.VanishState vanishState = vanishDetectorComponent.getVanishState(string4);
                boolean flag4;
                if (vanishState == VanishDetectorComponent.VanishState.NONE && flag) {
                    flag4 = flag2;
                } else {
                    flag4 = true;
                }

                boolean flag5;
                label67: {
                    boolean flag6;
                    if (vanishState != VanishDetectorComponent.VanishState.WATCHING) {
                        if (flag4) {
                            flag5 = flag3;
                            break label67;
                        }

                        flag6 = false;
                    } else {
                        flag6 = true;
                    }

                    flag5 = flag6;
                }

                StaffList.StaffStatus staffStatus = flag5 ? StaffList.StaffStatus.SPEC : (flag4 ? StaffList.StaffStatus.VANISH : StaffList.StaffStatus.ONLINE);
                String string6 = staffStatus == StaffList.StaffStatus.SPEC ? "spectator" : (staffStatus == StaffList.StaffStatus.VANISH ? "vanish" : "online");
                staffCandidates.add(new StaffList.StaffCandidate(string4, flag, flag2, flag3, vanishState, staffStatus, this.show.has(string6)));
            }
        }

        return staffCandidates;
    }

    private StaffList.StaffEntry addEntry(String string, String string2, Text text, StaffList.StaffStatus staffStatus, Identifier identifier) {
        String string3 = string.toLowerCase(Locale.ROOT);
        StaffList.StaffEntry staffEntry = this.entries.computeIfAbsent(string3, string4 -> new StaffList.StaffEntry(string));
        staffEntry.prefix = string2;
        staffEntry.prefixText = text;
        staffEntry.displayName = string2.isEmpty() ? string : string2 + " " + string;
        staffEntry.displayText = this.buildNameText(staffEntry);
        if (staffEntry.active) {
            return staffEntry;
        }

        staffEntry.status = staffStatus;
        staffEntry.statusText = staffStatus.label;
        staffEntry.skinTexture = identifier;
        staffEntry.active = true;
        return staffEntry;
    }

    private boolean isStaffPrefix(String string) {
        for (String string2 : STAFF_PREFIX_KEYWORDS) {
            if (string.contains(string2)) {
                return true;
            }
        }

        return false;
    }

    private String stripFormatting(String string) {
        if (string == null) {
            return "";
        }

        String string2 = Formatting.strip(string);
        return string2.trim();
    }

    protected float getIconSize(StaffList.StaffEntry staffEntry) {
        return 8.0F;
    }

    protected float getEntryValueWidth(StaffList.StaffEntry staffEntry) {
        return BODY_FONT.getWidth(staffEntry.statusText, 6.0F);
    }

    protected void renderIcon(DrawContext drawContext, float f, float f2, float f3, float f4, StaffList.StaffEntry staffEntry) {
        this.renderUserIcon(drawContext, f, f2 + f3 / 2.0F - 4.0F, 8.0F, f4);
    }

    protected void renderValue(DrawContext drawContext, float f, float f2, float f3, float f4, StaffList.StaffEntry staffEntry) {
        float f5 = BODY_FONT.getWidth(staffEntry.statusText, 6.0F);
        int i = staffEntry.status == StaffList.StaffStatus.SPEC ? Theme.getPrimary() : Theme.getTextColor();
        new RenderUtil(drawContext, f, f2, f5, f3).textMiddleLeft(BODY_FONT, staffEntry.statusText, 0.0F, 0.0F, this.applyAlpha(i, f4), 6.0F);
    }

    protected String getName(StaffList.StaffEntry staffEntry) {
        return staffEntry.displayName;
    }

    protected Text getNameText(StaffList.StaffEntry staffEntry) {
        return staffEntry.displayText != null ? staffEntry.displayText : Text.literal(staffEntry.name);
    }

    @Override
    protected String getEntryText(StaffList.StaffEntry staffEntry) {
        return this.getName(staffEntry);
    }

    @Override
    protected Text getEntryLabel(StaffList.StaffEntry staffEntry) {
        return this.getNameText(staffEntry);
    }

    @Override
    protected float getEntryWidth(StaffList.StaffEntry staffEntry) {
        return this.getIconSize(staffEntry);
    }

    @Override
    protected void renderEntryIcon(DrawContext drawContext, float f, float f2, float f3, float f4, StaffList.StaffEntry staffEntry) {
        this.renderIcon(drawContext, f, f2, f3, f4, staffEntry);
    }

    @Override
    protected void renderEntryContent(DrawContext drawContext, float f, float f2, float f3, float f4, StaffList.StaffEntry staffEntry) {
        this.renderValue(drawContext, f, f2, f3, f4, staffEntry);
    }
    private Text buildNameText(StaffList.StaffEntry staffEntry) {
        if (staffEntry.prefixText == null) {
            return Text.literal(staffEntry.name);
        }

        MutableText mutableText = Text.empty().append(staffEntry.prefixText);
        String string = staffEntry.prefixText.getString();
        if (!string.isEmpty() && !string.endsWith(" ")) {
            mutableText.append(Text.literal(" "));
        }

        return mutableText.append(Text.literal(staffEntry.name));
    }

    private void renderUserIcon(DrawContext drawContext, float f, float f2, float f3, float f4) {
        new RenderUtil(drawContext, f, f2, f3, f3).icon(Icon.USER, 0.0F, 0.0F, f3, ColorUtil.setAlphaf(Theme.getSecondaryText(), f4));
    }

    public static StaffList getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(StaffList.class);
    }

    record StaffCandidate(
        String name,
        boolean listed,
        boolean spectator,
        boolean loaded,
        VanishDetectorComponent.VanishState watched,
        StaffList.StaffStatus status,
        boolean shown
    ) {
    }

    public static class StaffEntry extends HudEntry {
        public final String name;
        public String prefix = "";
        public Text prefixText;
        public String displayName;
        public Text displayText;
        public StaffList.StaffStatus status = StaffList.StaffStatus.ONLINE;
        public String statusText = StaffList.StaffStatus.ONLINE.label;
        public Identifier skinTexture;

        public StaffEntry(String string) {
            this.name = string;
            this.displayName = string;
        }
    }

    public enum StaffStatus {
        ONLINE("Online"),
        VANISH("Vanish"),
        SPECTATE("GM3"),
        SPEC("Spec");

        public final String label;

        StaffStatus(String string) {
            this.label = string;
        }
    }
}
