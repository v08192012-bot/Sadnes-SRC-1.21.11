package tech.sadnes.client.command.impl;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.chat.TextDebugUtil;

public final class PrefixCommand extends Command {
    public PrefixCommand() {
        super("prefix");
    }

    @Override
    public void execute(String[] strings) {
        if (mc.world != null && mc.getNetworkHandler() != null) {
            if (strings.length == 0) {
                ChatUtil.print("[prefix] .prefix <nick>");
            } else {
                PlayerListEntry playerListEntry = this.findPlayerEntry(strings[0]);
                if (playerListEntry == null) {
                    ChatUtil.printError("[prefix] " + strings[0] + " not in tab");
                } else {
                    String string = playerListEntry.getProfile().name();
                    Team team = mc.world.getScoreboard().getScoreHolderTeam(string);
                    ChatUtil.print("[prefix] " + string + " team=" + (team == null ? "-" : team.getName()));
                    if (team != null) {
                        ChatUtil.print("[prefix] prefix: " + TextDebugUtil.describeText(team.getPrefix()));
                        ChatUtil.print("[prefix] suffix: " + TextDebugUtil.describeText(team.getSuffix()));
                    }

                    ChatUtil.print("[prefix] tab: " + TextDebugUtil.describeText(playerListEntry.getDisplayName()));
                }
            }
        }
    }

    private PlayerListEntry findPlayerEntry(String string) {
        for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getListedPlayerListEntries()) {
            if (playerListEntry.getProfile().name().equalsIgnoreCase(string)) {
                return playerListEntry;
            }
        }

        return null;
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1 && mc.getNetworkHandler() != null) {
            ArrayList arrayList = new ArrayList();

            for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getListedPlayerListEntries()) {
                arrayList.add(playerListEntry.getProfile().name());
            }

            return strings.length == 0 ? CommandManager.filterByPrefix(arrayList, "") : CommandManager.filterByPrefix(arrayList, strings[0]);
        } else {
            return List.of();
        }
    }
}
