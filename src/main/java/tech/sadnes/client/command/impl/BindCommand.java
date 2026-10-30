package tech.sadnes.client.command.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.feature.BindMode;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureBind;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.chat.ChatUtil;

public final class BindCommand extends Command {
    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "list", "clear");

    public BindCommand() {
        super("bind");
    }

    @Override
    public void execute(String[] strings) {
        String string = strings.length > 0 ? strings[0].toLowerCase(Locale.US) : "list";
        if (string.equals("add")) {
            if (strings.length < 4) {
                ChatUtil.print("Используй: .bind add ModuleName Key BindType (Key напр. F или Ctrl+F)");
            } else {
                Feature feature = this.findFeature(strings[1]);
                if (feature == null) {
                    ChatUtil.print("Модуль не найден: " + strings[1]);
                } else {
                    BindCommand.KeyCombo keyCombo = this.parseKeyCombo(strings[2]);
                    if (keyCombo == null) {
                        ChatUtil.print("Клавиша не найдена: " + strings[2]);
                    } else {
                        BindMode bindMode = this.parseBindType(strings[3]);
                        if (bindMode == null) {
                            ChatUtil.print("Тип бинда: Toggle или Hold");
                        } else {
                            feature.addBind(keyCombo.key(), bindMode, keyCombo.modifiers());
                            this.saveConfig();
                            ChatUtil.print(
                                "Бинд добавлен: "
                                    + feature.getDisplayName()
                                    + " -> "
                                    + Key.getComboName(keyCombo.modifiers(), keyCombo.key())
                                    + " "
                                    + bindMode.getDisplayName()
                            );
                        }
                    }
                }
            }
        } else if (string.equals("remove")) {
            if (strings.length < 2) {
                ChatUtil.print("Используй: .bind remove ModuleName");
            } else {
                Feature feature2 = this.findFeature(strings[1]);
                if (feature2 == null) {
                    ChatUtil.print("Модуль не найден: " + strings[1]);
                } else {
                    feature2.clearBinds();
                    this.saveConfig();
                    ChatUtil.print("Бинды модуля " + feature2.getDisplayName() + " удалены.");
                }
            }
        } else if (string.equals("clear")) {
            Iterator iterator2 = Sadnes.getInstance().getFeatureManager().values().iterator();

            while (iterator2.hasNext()) {
                ((Feature)iterator2.next()).clearBinds();
            }

            this.saveConfig();
            ChatUtil.print("Все бинды модулей удалены.");
        } else if (!string.equals("list")) {
            ChatUtil.print("Используй: .bind add/remove/list/clear");
        } else {
            ArrayList arrayList = new ArrayList();

            for (Feature feature3 : Sadnes.getInstance().getFeatureManager().values()) {
                for (FeatureBind featureBind : feature3.getBinds()) {
                    arrayList.add(feature3.getDisplayName() + " " + featureBind.getDisplayString() + " " + featureBind.getMode().getDisplayName());
                }
            }

            if (arrayList.isEmpty()) {
                ChatUtil.print("Биндов нет.");
            } else {
                ChatUtil.print("Бинды: " + String.join(", ", arrayList));
            }
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(SUBCOMMANDS, "") : CommandManager.filterByPrefix(SUBCOMMANDS, strings[0]);
        }

        if (strings[0].equalsIgnoreCase("add") || strings[0].equalsIgnoreCase("remove")) {
            if (strings.length == 2) {
                return CommandManager.filterByPrefix(this.getFeatureNames(), strings[1]);
            }

            if (strings[0].equalsIgnoreCase("add") && strings.length == 3) {
                return CommandManager.filterByPrefix(this.getKeyNames(), strings[2]);
            }

            if (strings[0].equalsIgnoreCase("add") && strings.length == 4) {
                return CommandManager.filterByPrefix(List.of("Toggle", "Hold"), strings[3]);
            }
        }

        return List.of();
    }

    private Feature findFeature(String string) {
        for (Feature feature : Sadnes.getInstance().getFeatureManager().values()) {
            if (feature.getDisplayName().equalsIgnoreCase(string)) {
                return feature;
            }
        }

        return null;
    }

    private BindCommand.KeyCombo parseKeyCombo(String string) {
        String[] strings = string.split("\\+");
        if (strings.length == 0) {
            return null;
        }

        int i = Key.getCode(strings[strings.length - 1].trim());
        if (i == -1) {
            return null;
        }

        int[] ints = new int[strings.length - 1];

        for (int i2 = 0; i2 < strings.length - 1; i2++) {
            int i3 = this.parseModifier(strings[i2].trim());
            if (i3 == -1) {
                return null;
            }

            ints[i2] = i3;
        }

        return new BindCommand.KeyCombo(i, ints);
    }

    private int parseModifier(String string) {
        String string2 = string.toLowerCase(Locale.US);
        byte b = -1;
        switch (string2.hashCode()) {
            case -1093611626:
                if (string2.equals("lshift")) {
                    return 340;
                }
                break;
            case -1093217649:
                if (string2.equals("lsuper")) {
                    b = 11;
                }
                break;
            case -921836720:
                if (string2.equals("rshift")) {
                    return 344;
                }
                break;
            case 96681:
                if (string2.equals("alt")) {
                    return 342;
                }
                break;
            case 117724:
                if (string2.equals("win")) {
                    return 343;
                }
                break;
            case 3064427:
                if (string2.equals("ctrl")) {
                    return 341;
                }
                break;
            case 3314109:
                if (string2.equals("lalt")) {
                    return 342;
                }
                break;
            case 102804695:
                if (string2.equals("lctrl")) {
                    return 341;
                }
                break;
            case 109407362:
                if (string2.equals("shift")) {
                    return 340;
                }
                break;
            case 109801339:
                if (string2.equals("super")) {
                    return 343;
                }
                break;
            case 196498289:
                if (string2.equals("lcontrol")) {
                    return 341;
                }
                break;
            case 951543133:
                if (string2.equals("control")) {
                    return 341;
                }
        }

        switch (b) {
            case 0:
            case 1:
            case 2:
            case 3:
                return 341;
            case 4:
            case 5:
                return 340;
            case 6:
                return 344;
            case 7:
            case 8:
                return 342;
            case 9:
            case 10:
            case 11:
                return 343;
            default:
                return -1;
        }
    }

    private BindMode parseBindType(String string) {
        for (BindMode bindMode : BindMode.values()) {
            if (bindMode.name().equalsIgnoreCase(string) || bindMode.getDisplayName().equalsIgnoreCase(string)) {
                return bindMode;
            }
        }

        return null;
    }

    private List<String> getFeatureNames() {
        ArrayList arrayList = new ArrayList();

        for (Feature feature : Sadnes.getInstance().getFeatureManager().values()) {
            arrayList.add(feature.getDisplayName());
        }

        return arrayList;
    }

    private List<String> getKeyNames() {
        ArrayList arrayList = new ArrayList();

        for (Key key : Key.values()) {
            if (key.getKeyCode() != -1) {
                arrayList.add(key.getName());
            }
        }

        return arrayList;
    }

    private void saveConfig() {
        if (Sadnes.getInstance().getConfigManager() != null) {
            Sadnes.getInstance().getConfigManager().saveActive();
        }
    }

    record KeyCombo(int key, int[] modifiers) {
    }
}
