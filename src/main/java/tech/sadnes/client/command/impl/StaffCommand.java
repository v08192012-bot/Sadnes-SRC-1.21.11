package tech.sadnes.client.command.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.network.PlayerListEntry;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.staff.StaffManager;
import tech.sadnes.client.util.chat.ChatUtil;

public final class StaffCommand extends Command {
    private static final List<String> SUBCOMMANDS = List.of("add", "addfile", "remove", "clear", "list");

    public StaffCommand() {
        super("staff");
    }

    @Override
    public void execute(String[] strings) {
        StaffManager staffManager = Sadnes.getInstance().getStaffManager();
        String string = strings.length > 0 ? strings[0].toLowerCase(Locale.US) : "list";
        byte b = -1;
        switch (string.hashCode()) {
            case -1148045923:
                if (string.equals("addfile")) {
                    this.addFromFile(staffManager, strings);
                    return;
                }
                break;
            case -934610812:
                if (string.equals("remove")) {
                    this.remove(staffManager, strings);
                    return;
                }
                break;
            case 96417:
                if (string.equals("add")) {
                    this.add(staffManager, strings);
                    return;
                }
                break;
            case 3322014:
                if (string.equals("list")) {
                    b = 4;
                }
                break;
            case 94746189:
                if (string.equals("clear")) {
                    this.clear(staffManager);
                    return;
                }
        }

        switch (b) {
            case 0:
                this.add(staffManager, strings);
                return;
            case 1:
                this.addFromFile(staffManager, strings);
                return;
            case 2:
                this.remove(staffManager, strings);
                return;
            case 3:
                this.clear(staffManager);
                return;
            case 4:
                this.list(staffManager);
                return;
            default:
                ChatUtil.print("Используй: .staff add/addfile/remove/clear/list");
        }
    }

    private void add(StaffManager staffManager, String[] strings) {
        if (strings.length < 2) {
            ChatUtil.print("Используй: .staff add nick");
        } else {
            String string = strings[1];
            if (this.isSelf(string)) {
                ChatUtil.printError("Себя в список модерации добавить нельзя.");
            } else if (staffManager.isStaff(string)) {
                ChatUtil.print(string + " уже в списке модерации.");
            } else {
                staffManager.addStaff(string);
                ChatUtil.print(string + " добавлен в список модерации.");
            }
        }
    }

    private void addFromFile(StaffManager staffManager, String[] strings) {
        if (strings.length < 2) {
            ChatUtil.print("Используй: .staff addfile <путь к файлу>");
        } else {
            Path path = Path.of(strings[1]);
            if (!Files.exists(path)) {
                ChatUtil.printError("Файл не найден: " + path);
            } else {
                int i = 0;

                try {
                    for (String string : Files.readAllLines(path)) {
                        String string2 = string.replace(",", "").replace("\"", "").trim();
                        if (!string2.isEmpty() && !this.isSelf(string2) && !staffManager.isStaff(string2)) {
                            staffManager.addStaff(string2);
                            i++;
                        }
                    }
                } catch (IOException ioException) {
                    ChatUtil.printError("Ошибка чтения файла: " + ioException.getMessage());
                    return;
                }

                ChatUtil.print("Добавлено модераторов из файла: " + i);
            }
        }
    }

    private void remove(StaffManager staffManager, String[] strings) {
        if (strings.length < 2) {
            ChatUtil.print("Используй: .staff remove nick");
        } else {
            String string = strings[1];
            if (!staffManager.isStaff(string)) {
                ChatUtil.print(string + " не найден в списке модерации.");
            } else {
                staffManager.removeStaff(string);
                ChatUtil.print(string + " удалён из списка модерации.");
            }
        }
    }

    private void clear(StaffManager staffManager) {
        if (staffManager.isEmpty()) {
            ChatUtil.print("Список модерации пуст.");
        } else {
            staffManager.clearStaff();
            ChatUtil.print("Список модерации очищен.");
        }
    }

    private void list(StaffManager staffManager) {
        if (staffManager.isEmpty()) {
            ChatUtil.print("Список модерации пуст.");
        } else {
            ChatUtil.print("Модерация: " + String.join(", ", staffManager));
        }
    }

    private boolean isSelf(String string) {
        return mc.player != null && mc.player.getGameProfile().name().equalsIgnoreCase(string);
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(SUBCOMMANDS, "") : CommandManager.filterByPrefix(SUBCOMMANDS, strings[0]);
        } else if (strings[0].equalsIgnoreCase("add") && strings.length == 2) {
            return CommandManager.filterByPrefix(this.getTabPlayerNames(), strings[1]);
        } else {
            return strings[0].equalsIgnoreCase("remove") ? CommandManager.filterByPrefix(Sadnes.getInstance().getStaffManager(), strings[1]) : List.of();
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
