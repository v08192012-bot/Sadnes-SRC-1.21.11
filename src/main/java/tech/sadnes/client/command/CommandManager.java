package tech.sadnes.client.command;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import tech.sadnes.client.command.impl.BindCommand;
import tech.sadnes.client.command.impl.BoxEspCommand;
import tech.sadnes.client.command.impl.BuildCommand;
import tech.sadnes.client.command.impl.ConfigCommand;
import tech.sadnes.client.command.impl.FastConnectCommand;
import tech.sadnes.client.command.impl.FriendCommand;
import tech.sadnes.client.command.impl.GpsCommand;
import tech.sadnes.client.command.impl.HClipCommand;
import tech.sadnes.client.command.impl.LayoutCommand;
import tech.sadnes.client.command.impl.LoginCommand;
import tech.sadnes.client.command.impl.MacroCommand;
import tech.sadnes.client.command.impl.NukerCommand;
import tech.sadnes.client.command.impl.PartyCommand;
import tech.sadnes.client.command.impl.PrefixCommand;
import tech.sadnes.client.command.impl.ReconnectCommand;
import tech.sadnes.client.command.impl.StaffCommand;
import tech.sadnes.client.command.impl.TeleportCommand;
import tech.sadnes.client.command.impl.ToggleCommand;
import tech.sadnes.client.command.impl.VClipCommand;
import tech.sadnes.client.command.impl.WayCommand;
import tech.sadnes.client.util.chat.ChatUtil;

public final class CommandManager {
    public static final String PREFIX = ".";
    private final Map<String, Command> commands = new LinkedHashMap<>();

    public void registerDefaults() {
        Command[] commands2 = new Command[20];
        BindCommand bindCommand = new BindCommand();
        Array.set(commands2, 0, bindCommand);
        FriendCommand friendCommand = new FriendCommand();
        Array.set(commands2, 1, friendCommand);
        ConfigCommand configCommand = new ConfigCommand();
        Array.set(commands2, 2, configCommand);
        GpsCommand gpsCommand = new GpsCommand();
        Array.set(commands2, 3, gpsCommand);
        WayCommand wayCommand = new WayCommand();
        Array.set(commands2, 4, wayCommand);
        StaffCommand staffCommand = new StaffCommand();
        Array.set(commands2, 5, staffCommand);
        BoxEspCommand boxEspCommand = new BoxEspCommand();
        Array.set(commands2, 6, boxEspCommand);
        NukerCommand nukerCommand = new NukerCommand();
        Array.set(commands2, 7, nukerCommand);
        TeleportCommand teleportCommand = new TeleportCommand();
        Array.set(commands2, 8, teleportCommand);
        VClipCommand vClipCommand = new VClipCommand();
        Array.set(commands2, 9, vClipCommand);
        HClipCommand hClipCommand = new HClipCommand();
        Array.set(commands2, 10, hClipCommand);
        LoginCommand loginCommand = new LoginCommand();
        Array.set(commands2, 11, loginCommand);
        FastConnectCommand fastConnectCommand = new FastConnectCommand();
        Array.set(commands2, 12, fastConnectCommand);
        PartyCommand partyCommand = new PartyCommand();
        Array.set(commands2, 13, partyCommand);
        ReconnectCommand reconnectCommand = new ReconnectCommand();
        Array.set(commands2, 14, reconnectCommand);
        MacroCommand macroCommand = new MacroCommand();
        Array.set(commands2, 15, macroCommand);
        ToggleCommand toggleCommand = new ToggleCommand();
        Array.set(commands2, 16, toggleCommand);
        LayoutCommand layoutCommand = new LayoutCommand();
        Array.set(commands2, 17, layoutCommand);
        BuildCommand buildCommand = new BuildCommand();
        Array.set(commands2, 18, buildCommand);
        PrefixCommand prefixCommand = new PrefixCommand();
        Array.set(commands2, 19, prefixCommand);
        this.register(commands2);
    }

    public void register(Command... commands2) {
        int i = Array.getLength(commands2);
        int i2 = 0;
        if (0 < i) {
            int i3;
            do {
                Object object = Array.get(commands2, i2);
                Map map = this.commands;
                String string = ((Command)object).getName();
                Locale locale = Locale.US;
                String string2 = string.toLowerCase(locale);
                map.put(string2, object);
                Iterator iterator2 = ((Command)object).getAliases().iterator();
                int i4 = i2 + 1;
                int i5 = i2 & -2 ^ -309318783;
                int i6 = i5 * i5;
                int i7 = (2 - i6) * i6;
                int i8 = (2 - i7) * i7;
                int i9 = (2 - i8) * i8;
                int i10 = (2 - i9) * i9;
                int i11 = 2 - i10;
                i3 = i10 * i4 * i11;

                while (iterator2.hasNext()) {
                    String string3 = (String)iterator2.next();
                    Locale locale2 = Locale.US;
                    String string4 = string3.toLowerCase(locale2);
                    map.put(string4, object);
                }

                i2 = i3;
            } while (i3 < i);
        }
    }

    public void unregister(String string) {
        Locale locale = Locale.US;
        String string2 = string.toLowerCase(locale);
        Command command2 = this.commands.get(string2);
        Map<String, Command> map = this.commands;
        if (command2 != null) {
            Set<Map.Entry<String, Command>> entries = map.entrySet();
            Predicate<Map.Entry<String, Command>> predicate =entry -> entry.getValue() == command2;
            entries.removeIf(predicate);
        }
    }

    public boolean handleMessage(String string) {
        if (string != null && string.startsWith(".")) {
            addToChatHistory(string);
            String string2 = string.substring(".".length()).trim();
            if (string2.isEmpty()) {
                ChatUtil.print("Команды: " + String.join(", ", this.getCommandNames()));
                return true;
            }

            String[] strings = string2.split("\\s+");
            Command command = this.commands.get(strings[0].toLowerCase(Locale.US));
            if (command == null) {
                ChatUtil.printError("Команда не найдена: " + strings[0]);
                ChatUtil.print("Команды: " + String.join(", ", this.getCommandNames()));
                return true;
            }

            try {
                command.execute(Arrays.copyOfRange(strings, 1, strings.length));
            } catch (Exception exception) {
                ChatUtil.printError(exception.getMessage());
            }

            return true;
        } else {
            return false;
        }
    }

    public CommandManager.Suggestions getSuggestions(String string, int i) {
        if (string != null && string.startsWith(".")) {
            if (i < 0 || i > string.length()) {
                i = string.length();
            }

            String string2 = string.substring(0, i);
            String string3 = string2.substring(".".length());
            if (string3.isEmpty()) {
                return new CommandManager.Suggestions(".".length(), i, this.getCommandNames());
            }

            boolean flag = string3.endsWith(" ");
            String string4 = string3.trim();
            String[] strings = string4.isEmpty() ? new String[0] : string4.split("\\s+");
            if (strings.length == 0) {
                return new CommandManager.Suggestions(".".length(), i, this.getCommandNames());
            }

            int i2 = Math.max(".".length(), string2.lastIndexOf(32) + 1);
            if (strings.length == 1 && !flag) {
                Command command = this.commands.get(strings[0].toLowerCase(Locale.US));
                return command != null ? null : new CommandManager.Suggestions(i2, i, dedupe(filterByPrefix(this.getCommandNames(), strings[0])));
            }

            Command command2 = this.commands.get(strings[0].toLowerCase(Locale.US));
            if (command2 == null) {
                return null;
            }

            String[] strings2 = flag ? Arrays.copyOfRange(strings, 1, strings.length + 1) : Arrays.copyOfRange(strings, 1, strings.length);
            if (flag) {
                strings2[strings2.length - 1] = "";
            }

            return new CommandManager.Suggestions(i2, i, dedupe(command2.complete(strings2)));
        } else {
            return null;
        }
    }

    public List<String> getCommandNames() {
        return new ArrayList<>(this.commands.keySet());
    }

    private static void addToChatHistory(String string) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.inGameHud != null) {
            minecraftClient.inGameHud.getChatHud().addToMessageHistory(string);
        }
    }

    public static List<String> filterByPrefix(List<String> strings, String string) {
        ArrayList arrayList = new ArrayList();
        String string2 = string == null ? "" : string.toLowerCase(Locale.US);

        for (String string3 : strings) {
            if (string3.toLowerCase(Locale.US).startsWith(string2)) {
                arrayList.add(string3);
            }
        }

        return arrayList;
    }

    private static List<String> dedupe(List<String> strings) {
        ArrayList<String> strings2 = new ArrayList<>();

        for (String string : strings) {
            if (string != null && !string.isBlank() && strings2.stream().noneMatch(string::equalsIgnoreCase)) {
                strings2.add(string);
            }
        }

        return strings2;
    }

    public record Suggestions(int start, int end, List<String> values) {
    }
}
