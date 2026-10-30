package tech.sadnes.client.command.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.Util;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.config.ConfigManager;
import tech.sadnes.client.util.chat.ChatUtil;

public final class ConfigCommand extends Command {
    private static final List<String> SUBCOMMANDS = List.of("save", "delete", "list", "load", "dir");

    public ConfigCommand() {
        super("config");
    }

    @Override
    public void execute(String[] strings) {
        ConfigManager configManager = Sadnes.getInstance().getConfigManager();
        if (configManager == null) {
            ChatUtil.print("ConfigManager не найден.");
        } else {
            String string = strings.length > 0 ? strings[0].toLowerCase(Locale.US) : "list";
            if (string.equals("save")) {
                if (strings.length < 2 || this.isInvalidName(strings[1])) {
                    ChatUtil.print("Используй: .config save NAME");
                } else if (configManager.save(strings[1])) {
                    ChatUtil.print("Конфиг сохранен: " + strings[1]);
                } else {
                    ChatUtil.print("Не удалось сохранить конфиг: " + strings[1]);
                }
            } else if (string.equals("delete")) {
                if (strings.length < 2 || this.isInvalidName(strings[1])) {
                    ChatUtil.print("Используй: .config delete NAME");
                } else if (!configManager.exists(strings[1])) {
                    ChatUtil.print("Конфиг не найден: " + strings[1]);
                } else if (configManager.delete(strings[1])) {
                    ChatUtil.print("Конфиг удален: " + strings[1]);
                } else {
                    ChatUtil.print("Не удалось удалить конфиг: " + strings[1]);
                }
            } else if (string.equals("load")) {
                if (strings.length < 2 || this.isInvalidName(strings[1])) {
                    ChatUtil.print("Используй: .config load NAME");
                } else if (configManager.load(strings[1])) {
                    ChatUtil.print("Конфиг загружен: " + strings[1]);
                } else {
                    ChatUtil.print("Конфиг не найден: " + strings[1]);
                }
            } else if (string.equals("list")) {
                List list = configManager.listConfigs();
                if (list.isEmpty()) {
                    ChatUtil.print("Конфигов нет.");
                } else {
                    ChatUtil.print("Конфиги: " + String.join(", ", list));
                }
            } else if (string.equals("dir")) {
                Path path = configManager.getDirectory();
                if (path == null) {
                    ChatUtil.print("Папка конфигов недоступна.");
                } else {
                    Util.getOperatingSystem().open(path);
                    ChatUtil.print("Папка конфигов: " + path);
                }
            } else {
                ChatUtil.print("Используй: .config save/delete/list/load/dir");
            }
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(SUBCOMMANDS, "") : CommandManager.filterByPrefix(SUBCOMMANDS, strings[0]);
        }

        if (!strings[0].equalsIgnoreCase("load") && !strings[0].equalsIgnoreCase("delete") && !strings[0].equalsIgnoreCase("save")) {
            return List.of();
        }

        ConfigManager configManager = Sadnes.getInstance().getConfigManager();
        return configManager == null ? List.of() : CommandManager.filterByPrefix(configManager.listConfigs(), strings[1]);
    }

    private boolean isInvalidName(String string) {
        if (string != null && !string.isBlank() && !string.contains("..")) {
            int i = 0;

            while (i < string.length()) {
                char c = string.charAt(i);
                switch (c) {
                    case '"':
                    case '*':
                    case '/':
                    case ':':
                    case '<':
                    case '>':
                    case '?':
                    case '\\':
                        return true;
                    case '|':
                        return true;
                    default:
                        i++;
                }
            }

            return false;
        } else {
            return true;
        }
    }
}
