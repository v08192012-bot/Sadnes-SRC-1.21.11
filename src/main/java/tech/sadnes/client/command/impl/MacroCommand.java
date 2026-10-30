package tech.sadnes.client.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.macro.MacroManager;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.chat.ChatUtil;

public final class MacroCommand extends Command {
    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "clear", "list");

    public MacroCommand() {
        super("macro");
    }

    @Override
    public void execute(String[] strings) {
        MacroManager macroManager = Sadnes.getInstance().getMacroManager();
        String string = strings.length > 0 ? strings[0].toLowerCase(Locale.US) : "list";
        byte b = -1;
        switch (string.hashCode()) {
            case -934610812:
                if (string.equals("remove")) {
                    this.remove(macroManager, strings);
                    return;
                }
                break;
            case 96417:
                if (string.equals("add")) {
                    this.add(macroManager, strings);
                    return;
                }
                break;
            case 3322014:
                if (string.equals("list")) {
                    b = 3;
                }
                break;
            case 94746189:
                if (string.equals("clear")) {
                    macroManager.clearMacros();
                    ChatUtil.print(Language.translate("command.macro.cleared"));
                    return;
                }
        }

        switch (b) {
            case 0:
                this.add(macroManager, strings);
                return;
            case 1:
                this.remove(macroManager, strings);
                return;
            case 2:
                macroManager.clearMacros();
                ChatUtil.print(Language.translate("command.macro.cleared"));
                return;
            case 3:
                this.list(macroManager);
                return;
            default:
                ChatUtil.print(Language.translate("command.macro.usage"));
        }
    }

    private void add(MacroManager macroManager, String[] strings) {
        if (strings.length < 3) {
            ChatUtil.print(Language.translate("command.macro.add.usage"));
        } else {
            int i = Key.getCode(strings[1]);
            if (i == Key.KEY_NONE.getKeyCode()) {
                ChatUtil.print(Language.translate("command.macro.badkey"), strings[1]);
            } else {
                String string = String.join(" ", Arrays.copyOfRange(strings, 2, strings.length)).trim();
                if (string.isEmpty()) {
                    ChatUtil.print(Language.translate("command.macro.add.usage"));
                } else {
                    macroManager.addMacro(i, string);
                    ChatUtil.print(Language.translate("command.macro.added"), Key.getDisplayName(i), string);
                }
            }
        }
    }

    private void remove(MacroManager macroManager, String[] strings) {
        if (strings.length < 2) {
            ChatUtil.print(Language.translate("command.macro.remove.usage"));
        } else {
            int i = Key.getCode(strings[1]);
            if (i == Key.KEY_NONE.getKeyCode()) {
                ChatUtil.print(Language.translate("command.macro.badkey"), strings[1]);
            } else if (macroManager.removeMacro(i)) {
                ChatUtil.print(Language.translate("command.macro.removed"), Key.getDisplayName(i));
            } else {
                ChatUtil.print(Language.translate("command.macro.notfound"), Key.getDisplayName(i));
            }
        }
    }

    private void list(MacroManager macroManager) {
        if (macroManager.isEmpty()) {
            ChatUtil.print(Language.translate("command.macro.list.empty"));
        } else {
            StringBuilder builder = new StringBuilder();

            for (Entry entry : macroManager.getMacros().entrySet()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }

                builder.append(Key.getDisplayName((Integer)entry.getKey())).append(" → ").append((String)entry.getValue());
            }

            ChatUtil.print(Language.translate("command.macro.list"), builder.toString());
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(SUBCOMMANDS, "") : CommandManager.filterByPrefix(SUBCOMMANDS, strings[0]);
        } else {
            String string = strings[0].toLowerCase(Locale.US);
            if (strings.length == 2 && string.equals("add")) {
                return CommandManager.filterByPrefix(this.getKeyNames(), strings[1]);
            } else {
                return strings.length == 2 && string.equals("remove") ? CommandManager.filterByPrefix(this.getBoundKeyNames(), strings[1]) : List.of();
            }
        }
    }

    private List<String> getKeyNames() {
        ArrayList arrayList = new ArrayList();

        for (Key key : Key.values()) {
            if (key != Key.KEY_NONE) {
                arrayList.add(key.getName());
            }
        }

        return arrayList;
    }

    private List<String> getBoundKeyNames() {
        ArrayList arrayList = new ArrayList();

        for (int i : Sadnes.getInstance().getMacroManager().getMacros().keySet()) {
            arrayList.add(Key.getDisplayName(i));
        }

        return arrayList;
    }
}
