package tech.sadnes.client.command.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.network.PlayerListEntry;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.friend.FriendManager;
import tech.sadnes.client.util.chat.ChatUtil;

public final class FriendCommand extends Command {
    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "clear", "list");

    public FriendCommand() {
        super("friend");
    }

    @Override
    public void execute(String[] strings) {
        FriendManager friendManager = Sadnes.getInstance().getFriendManager();
        String string = strings.length > 0 ? strings[0].toLowerCase(Locale.US) : "list";
        if (string.equals("add")) {
            if (strings.length < 2) {
                ChatUtil.print("Используй: .friend add nick");
            } else if (friendManager.isFriend(strings[1])) {
                ChatUtil.print(strings[1] + " уже есть в друзьях.");
            } else {
                friendManager.addFriend(strings[1]);
                ChatUtil.print(strings[1] + " добавлен в друзья.");
            }
        } else if (string.equals("remove")) {
            if (strings.length < 2) {
                ChatUtil.print("Используй: .friend remove nick");
            } else if (!friendManager.isFriend(strings[1])) {
                ChatUtil.print(strings[1] + " не найден в друзьях.");
            } else {
                friendManager.removeFriend(strings[1]);
                ChatUtil.print(strings[1] + " удален из друзей.");
            }
        } else if (string.equals("clear")) {
            friendManager.clearFriends();
            ChatUtil.print("Список друзей очищен.");
        } else if (string.equals("list")) {
            if (friendManager.isEmpty()) {
                ChatUtil.print("Список друзей пуст.");
            } else {
                ChatUtil.print("Друзья: " + String.join(", ", friendManager));
            }
        } else {
            ChatUtil.print("Используй: .friend add/remove/clear/list");
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(SUBCOMMANDS, "") : CommandManager.filterByPrefix(SUBCOMMANDS, strings[0]);
        } else if (strings[0].equalsIgnoreCase("add") && strings.length == 2) {
            return CommandManager.filterByPrefix(this.getTabPlayerNames(), strings[1]);
        } else {
            return strings[0].equalsIgnoreCase("remove") ? CommandManager.filterByPrefix(Sadnes.getInstance().getFriendManager(), strings[1]) : List.of();
        }
    }

    private List<String> getTabPlayerNames() {
        ArrayList arrayList = new ArrayList();
        if (mc.getNetworkHandler() == null) {
            return arrayList;
        }

        for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getListedPlayerListEntries()) {
            arrayList.add(playerListEntry.getProfile().name());
        }

        return arrayList;
    }
}
