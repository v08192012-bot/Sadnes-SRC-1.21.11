package tech.sadnes.client.command.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.config.LayoutStorage;
import tech.sadnes.client.feature.component.InventoryLayoutComponent;
import tech.sadnes.client.util.chat.ChatUtil;

public final class LayoutCommand extends Command {
    private static final List<String> SUBCOMMANDS = List.of("save", "load", "clear", "list");

    public LayoutCommand() {
        super("layout");
    }

    @Override
    public void execute(String[] strings) {
        String string;
        label41: {
            String string2 = strings.length > 0 ? strings[0].toLowerCase(Locale.US) : "list";
            string = strings.length > 1 ? strings[1] : "default";
            byte b = -1;
            switch (string2.hashCode()) {
                case 3322014:
                    if (string2.equals("list")) {
                        b = 3;
                    }
                    break;
                case 3327206:
                    if (string2.equals("load")) {
                        this.load(string);
                        return;
                    }
                    break;
                case 3522941:
                    if (string2.equals("save")) {
                        this.save(string);
                        return;
                    }
                    break;
                case 94746189:
                    if (string2.equals("clear")) {
                        break label41;
                    }
            }

            switch (b) {
                case 0:
                    this.save(string);
                    return;
                case 1:
                    this.load(string);
                    return;
                case 2:
                    break;
                case 3:
                    this.list();
                    return;
                default:
                    ChatUtil.print("Используй: .layout save/load/clear/list [имя]");
                    return;
            }
        }

        if (strings.length > 1) {
            this.clear(string);
        } else {
            this.clear(null);
        }
    }

    private void save(String string) {
        if (mc.player == null) {
            ChatUtil.printError("Нет игрока.");
        } else {
            Map map = InventoryLayoutComponent.getInstance().captureLayout();
            if (map.isEmpty()) {
                ChatUtil.print("Инвентарь пуст — нечего сохранять.");
            } else if (LayoutStorage.save(string, map)) {
                ChatUtil.print("Раскладка '" + string + "' сохранена (" + map.size() + " предметов).");
            } else {
                ChatUtil.printError("Не удалось сохранить раскладку.");
            }
        }
    }

    private void load(String string) {
        if (mc.player != null && mc.interactionManager != null) {
            Map map = LayoutStorage.load(string);
            if (map == null) {
                ChatUtil.printError("Раскладка '" + string + "' не найдена.");
            } else {
                ChatUtil.print("Загружаю раскладку '" + string + "'...");
                InventoryLayoutComponent.getInstance().loadLayout(map);
            }
        } else {
            ChatUtil.printError("Нет игрока.");
        }
    }

    private void clear(String string) {
        if (string == null) {
            LayoutStorage.deleteAll();
            ChatUtil.print("Все раскладки удалены.");
        } else if (LayoutStorage.delete(string)) {
            ChatUtil.print("Раскладка '" + string + "' удалена.");
        } else {
            ChatUtil.printError("Раскладка '" + string + "' не найдена.");
        }
    }

    private void list() {
        List list2 = LayoutStorage.list();
        if (list2.isEmpty()) {
            ChatUtil.print("Сохранённых раскладок нет.");
        } else {
            ChatUtil.print("Раскладки: " + String.join(", ", list2));
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(SUBCOMMANDS, "") : CommandManager.filterByPrefix(SUBCOMMANDS, strings[0]);
        }

        String string = strings[0].toLowerCase(Locale.US);
        return (string.equals("load") || string.equals("clear")) && strings.length == 2 ? CommandManager.filterByPrefix(LayoutStorage.list(), strings[1]) : List.of();
    }
}
