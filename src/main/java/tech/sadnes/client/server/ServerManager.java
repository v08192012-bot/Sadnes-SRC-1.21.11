package tech.sadnes.client.server;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import tech.sadnes.api.mixin.accessor.BossBarHudAccessor;
import tech.sadnes.api.mixin.accessor.PlayerListHudAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.component.NetworkStatsComponent;
import tech.sadnes.client.util.IMinecraft;

public final class ServerManager implements IMinecraft {
    private static final Pattern LOBBY_PATTERN = Pattern.compile("(\\p{L}[\\p{L}0-9 .()\\-]*?)\\s*#\\s*(\\d+)");
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[A-zА-я0-9_]{3,16}$");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d{1,4})\\s*(?:sec|s|сек|с)");
    private static String healthObjective;

    private ServerManager() {
    }

    public static String getDimension() {
        return mc.world.getRegistryKey().getValue().getPath();
    }

    public static boolean isInvalidName(String string) {
        return string == null || !NICKNAME_PATTERN.matcher(string).matches();
    }

    public static boolean isNullWorld() {
        return mc.player == null || mc.world == null;
    }

    public static int getPing() {
        NetworkStatsComponent networkStatsComponent = Sadnes.getInstance().getComponentManager().getComponent(NetworkStatsComponent.class);
        if (networkStatsComponent != null && networkStatsComponent.getPing() > 0) {
            return networkStatsComponent.getPing();
        } else if (mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            return playerListEntry == null ? 0 : Math.max(playerListEntry.getLatency(), 0);
        } else {
            return 0;
        }
    }

    public static boolean isFunTime() {
        return isServer("funtime") || isLocalProxy();
    }

    public static int getFunTimeAnarchy() {
        if (isFunTime() && mc.inGameHud != null) {
            Text text = ((PlayerListHudAccessor)mc.inGameHud.getPlayerListHud()).sadnes$getHeader();
            if (text == null) {
                return -1;
            }

            String[] strings = text.getString().split("-");
            if (strings.length < 2) {
                return -1;
            }

            try {
                return Integer.parseInt(strings[1]);
            } catch (NumberFormatException numberFormatException) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public static boolean isDonut() {
        return isServer("donut");
    }

    public static boolean isSpookyArena() {
        return mc.world.getRegistryKey().getValue().getPath().contains("arenas") && isSpookyTime();
    }

    public static boolean isHolyWorld() {
        return isServer("holy");
    }

    public static boolean isSpookyTime() {
        return isServer("spookytime") || isServer("dildo");
    }

    public static boolean isReallyWorld() {
        return isServer("reallyworld");
    }

    public static boolean isAresMine() {
        return isServer("aresmine");
    }

    public static boolean isServer(String string) {
        return mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null
            ? mc.getCurrentServerEntry().address.toLowerCase().contains(string.toLowerCase())
            : false;
    }

    public static boolean isLocalProxy() {
        if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null) {
            String string = mc.getCurrentServerEntry().address;
            return string.equals("0.0.0.0") || string.startsWith("0.0.0.0:");
        } else {
            return false;
        }
    }

    public static boolean isInHub() {
        if (mc.world == null) {
            return false;
        } else {
            return !isFunTime() && !isHolyWorld() && !isSpookyTime() && !isReallyWorld() ? false : isHubScoreboard() || isHubTabList();
        }
    }

    private static boolean isHubScoreboard() {
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (scoreboardObjective == null) {
            return false;
        }

        if (containsHub(scoreboardObjective.getDisplayName().getString())) {
            return true;
        }

        for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(scoreboardObjective)) {
            String string = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
            if (containsHub(string)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isHubTabList() {
        if (mc.inGameHud == null) {
            return false;
        }

        PlayerListHud playerListHud = mc.inGameHud.getPlayerListHud();
        if (playerListHud == null) {
            return false;
        }

        Text text = ((PlayerListHudAccessor)playerListHud).sadnes$getHeader();
        Text text2 = ((PlayerListHudAccessor)playerListHud).sadnes$getFooter();
        return text != null && containsHub(text.getString()) || text2 != null && containsHub(text2.getString());
    }

    private static boolean containsHub(String string) {
        if (string != null && !string.isEmpty()) {
            String string2 = string.toLowerCase();
            return string2.contains("хаб") || string2.contains("hub") || string2.contains("лобби") || string2.contains("lobby");
        } else {
            return false;
        }
    }

    public static boolean isWardenActive() {
        if (mc.world == null || mc.inGameHud == null || mc.inGameHud.getBossBarHud() == null) {
            return false;
        } else {
            return !isFunTime() && !isHolyWorld() && !isSpookyTime() && !isReallyWorld()
                ? false
                : ((BossBarHudAccessor)mc.inGameHud.getBossBarHud())
                    .sadnes$getBossBars()
                    .values()
                    .stream()
                    .map(clientBossBar -> clientBossBar.getName().getString())
                    .filter(string -> string != null && !string.isEmpty())
                    .map(String::toLowerCase)
                    .anyMatch(string2 -> string2.contains("вардена") || string2.contains("варден") || string2.contains("warden"));
        }
    }

    public static String getServerType() {
        if (!isNullWorld() && mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null && mc.getNetworkHandler().getBrand() != null) {
            String string = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
            String string2 = mc.getNetworkHandler().getBrand().toLowerCase();
            if (string2.contains("botfilter") || isLocalProxy()) {
                return "FunTime";
            }

            if (string2.contains("§6spooky§ccore")) {
                return "SpookyTime";
            }

            if (string.contains("funtime") || string.contains("skytime") || string.contains("space-times") || string.contains("funsky")) {
                return "CopyTime";
            }

            if (string2.contains("holyworld") || string2.contains("vk.com/idwok")) {
                return "HolyWorld";
            }

            if (string.contains("reallyworld")) {
                return "ReallyWorld";
            }
        }

        return "Vanilla";
    }

    public static String getServerName() {
        if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null) {
            String string = getServerType();
            return "Vanilla".equals(string) ? mc.getCurrentServerEntry().address : string;
        } else {
            return "singleplayer";
        }
    }

    public static boolean isInPvp() {
        return mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null
            ? ((BossBarHudAccessor)mc.inGameHud.getBossBarHud())
                .sadnes$getBossBars()
                .values()
                .stream()
                .map(clientBossBar -> clientBossBar.getName().getString())
                .filter(string -> string != null && !string.isEmpty())
                .map(String::toLowerCase)
                .anyMatch(string2 -> string2.contains("pvp") || string2.contains("пвп"))
            : false;
    }

    public static int getPvpTime() {
        if (!isInPvp()) {
            return -1;
        }

        int i = 0;

        for (ClientBossBar clientBossBar : ((BossBarHudAccessor)mc.inGameHud.getBossBarHud()).sadnes$getBossBars().values()) {
            String string = clientBossBar.getName().getString();
            if (string != null) {
                String string2 = string.toLowerCase();
                if (string2.contains("pvp") || string2.contains("пвп")) {
                    i = parseSeconds(string);
                    break;
                }
            }
        }

        return Math.max(1, i);
    }

    private static int parseSeconds(String string) {
        Matcher matcher2 = TIME_PATTERN.matcher(string);
        if (matcher2.find()) {
            int i = Integer.parseInt(matcher2.group(1));
            int i2 = Integer.parseInt(matcher2.group(2));
            return matcher2.group(3) != null ? i * 3600 + i2 * 60 + Integer.parseInt(matcher2.group(3)) : i * 60 + i2;
        } else {
            Matcher matcher3 = SECONDS_PATTERN.matcher(string.toLowerCase());
            return matcher3.find() ? Integer.parseInt(matcher3.group(1)) : 0;
        }
    }

    public static boolean isIntersecting(LivingEntity livingEntity) {
        return isIntersecting(livingEntity, 0.0F);
    }

    public static boolean isIntersecting(LivingEntity livingEntity, float f) {
        if (!isNullWorld() && livingEntity != null) {
            Box box = mc.player.getBoundingBox();
            Box box2 = livingEntity.getBoundingBox().expand(f, 0.0, f);
            return box.intersects(box2);
        } else {
            return false;
        }
    }

    public static boolean isInCobweb() {
        return isInBlock(Blocks.COBWEB);
    }

    public static boolean isInBerryBush() {
        return isInBlock(Blocks.SWEET_BERRY_BUSH);
    }

    private static boolean isInBlock(Block block) {
        if (isNullWorld()) {
            return false;
        }

        Box box = mc.player.getBoundingBox();
        BlockPos blockPos = mc.player.getBlockPos();

        for (BlockPos blockPos2 : getNearbyPositions(blockPos)) {
            if (mc.world.getBlockState(blockPos2).isOf(block) && box.intersects(new Box(blockPos2))) {
                return true;
            }
        }

        return false;
    }

    public static float getHealth(PlayerEntity playerEntity) {
        if (playerEntity == null) {
            return 0.0F;
        }

        float f = playerEntity.getMaxHealth();
        float f2 = playerEntity.getHealth();
        float f3 = getScoreboardHealth(playerEntity, f2);
        return !playerEntity.isAlive() || !Float.isInfinite(f3) && !Float.isNaN(f3) && f3 != 2.1474836E9F ? f3 : f;
    }

    public static float getScoreboardHealth(PlayerEntity playerEntity, float f) {
        if (mc.world != null && playerEntity != null) {
            Scoreboard scoreboard = mc.world.getScoreboard();
            if (healthObjective != null) {
                ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective(healthObjective);
                if (scoreboardObjective != null) {
                    ReadableScoreboardScore readableScoreboardScore = scoreboard.getScore(playerEntity, scoreboardObjective);
                    if (readableScoreboardScore != null) {
                        return readableScoreboardScore.getScore();
                    }
                }
            }

            for (ScoreboardObjective scoreboardObjective2 : scoreboard.getObjectives()) {
                String string = scoreboardObjective2.getDisplayName().getString();
                if (string.contains("Здоровья") || string.contains("❤")) {
                    ReadableScoreboardScore readableScoreboardScore2 = scoreboard.getScore(playerEntity, scoreboardObjective2);
                    if (readableScoreboardScore2 != null) {
                        healthObjective = scoreboardObjective2.getName();
                        return readableScoreboardScore2.getScore();
                    }
                }
            }

            return playerEntity.isInvisible() ? playerEntity.getMaxHealth() : f;
        } else {
            return f;
        }
    }

    public static long getCoins() {
        if (mc.world == null) {
            return -1L;
        }

        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (scoreboardObjective == null) {
            return -1L;
        }

        for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(scoreboardObjective)) {
            String string = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
            int i = string.indexOf("Монет");
            if (i != -1) {
                String string2 = string.substring(i).replaceAll("\\D+", "");
                if (!string2.isEmpty()) {
                    try {
                        return Long.parseLong(string2);
                    } catch (NumberFormatException numberFormatException) {
                        return -1L;
                    }
                }
            }
        }

        return -1L;
    }

    public static int getAnarchyNumber() {
        if (mc.world == null) {
            return -1;
        }

        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (scoreboardObjective == null) {
            return -1;
        }

        String string = scoreboardObjective.getDisplayName().getString();
        if (isFunTime() || isSpookyTime()) {
            String[] strings = string.split("-");
            if (strings.length > 1) {
                String string2 = strings[1].trim();
                if (!string2.isEmpty() && string2.matches("\\d+")) {
                    return Integer.parseInt(string2);
                }
            }
        } else if (isReallyWorld()) {
            String string3 = string.replaceAll("\\D+", "");
            if (!string3.isEmpty()) {
                return Integer.parseInt(string3);
            }
        } else if (isHolyWorld()) {
            ServerManager.Lobby lobby = getHolyLobby();
            if (lobby != null) {
                return lobby.number();
            }
        }

        return -1;
    }

    public static ServerManager.Lobby getHolyLobby() {
        if (mc.world != null && isHolyWorld()) {
            Scoreboard scoreboard = mc.world.getScoreboard();
            ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (scoreboardObjective == null) {
                return null;
            }

            for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(scoreboardObjective)) {
                String string = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
                if (!string.isEmpty()) {
                    Matcher matcher2 = LOBBY_PATTERN.matcher(string);
                    if (matcher2.find()) {
                        return new ServerManager.Lobby(matcher2.group(1).trim(), Integer.parseInt(matcher2.group(2)));
                    }
                }
            }
        }

        return null;
    }

    public static String getHolyMode() {
        ServerManager.Lobby lobby = getHolyLobby();
        return lobby == null ? null : lobby.mode();
    }

    public static String getLiteModeName(int i) {
        if (i <= 12) {
            return "СолоЛайт";
        } else if (i <= 26) {
            return "ДуоЛайт";
        } else {
            return i <= 39 ? "ТриоЛайт" : "КланЛайт";
        }
    }

    private static List<BlockPos> getNearbyPositions(BlockPos blockPos) {
        ArrayList arrayList = new ArrayList();

        for (int i = blockPos.getX() - 2; i <= blockPos.getX() + 2; i++) {
            for (int i2 = blockPos.getY() - 1; i2 <= blockPos.getY() + 4; i2++) {
                for (int i3 = blockPos.getZ() - 2; i3 <= blockPos.getZ() + 2; i3++) {
                    arrayList.add(new BlockPos(i, i2, i3));
                }
            }
        }

        return arrayList;
    }

    public record Lobby(String mode, int number) {
    }
}
