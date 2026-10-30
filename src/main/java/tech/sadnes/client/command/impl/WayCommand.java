package tech.sadnes.client.command.impl;

import java.util.List;
import java.util.Locale;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.ui.hud.WaypointRenderer;
import tech.sadnes.client.util.chat.ChatUtil;

public final class WayCommand extends Command {
    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "clear", "list");

    public WayCommand() {
        super("way");
    }

    @Override
    public void execute(String[] strings) {
        String string = strings.length > 0 ? strings[0].toLowerCase(Locale.US) : "list";
        if (string.equals("add")) {
            if (strings.length < 5) {
                ChatUtil.print("Используй: .way add NAME X Y Z");
            } else {
                String string2 = strings[1];

                try {
                    double d = Double.parseDouble(strings[2]);
                    double d2 = Double.parseDouble(strings[3]);
                    double d3 = Double.parseDouble(strings[4]);
                    if (d % 1.0 == 0.0) {
                        d += 0.5;
                    }

                    if (d3 % 1.0 == 0.0) {
                        d3 += 0.5;
                    }

                    WaypointRenderer.add(string2, d, d2, d3);
                    ChatUtil.print("Метка " + string2 + " добавлена (" + (int)d + " " + (int)d2 + " " + (int)d3 + ")");
                } catch (NumberFormatException numberFormatException) {
                    ChatUtil.print("Координаты должны быть числами.");
                }
            }
        } else if (string.equals("remove")) {
            if (strings.length < 2) {
                ChatUtil.print("Используй: .way remove NAME");
            } else {
                if (WaypointRenderer.remove(strings[1])) {
                    ChatUtil.print("Метка " + strings[1] + " удалена.");
                } else {
                    ChatUtil.print("Метка " + strings[1] + " не найдена.");
                }
            }
        } else if (string.equals("clear")) {
            WaypointRenderer.clear();
            ChatUtil.print("Все метки удалены.");
        } else if (string.equals("list")) {
            List list = WaypointRenderer.getNames();
            if (list.isEmpty()) {
                ChatUtil.print("Меток нет.");
            } else {
                ChatUtil.print("Метки: " + String.join(", ", list));
            }
        } else {
            ChatUtil.print("Используй: .way add/remove/clear/list");
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(SUBCOMMANDS, "") : CommandManager.filterByPrefix(SUBCOMMANDS, strings[0]);
        } else {
            return strings[0].equalsIgnoreCase("remove") && strings.length == 2 ? CommandManager.filterByPrefix(WaypointRenderer.getNames(), strings[1]) : List.of();
        }
    }
}
