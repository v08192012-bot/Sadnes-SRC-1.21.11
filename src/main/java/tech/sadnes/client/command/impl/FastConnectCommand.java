package tech.sadnes.client.command.impl;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerInfo.ServerType;
import net.minecraft.text.Text;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.account.Account;
import tech.sadnes.client.account.AccountManager;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.util.NickGeneratorUtil;
import tech.sadnes.client.util.chat.ChatUtil;

public final class FastConnectCommand extends Command {
    private static final List<FastConnectCommand.Server> SERVERS = List.of(
        new FastConnectCommand.Server("funtime", "ft", "neo.funtime.sh"),
        new FastConnectCommand.Server("holyworld", "hw", "mc.holyworld.me"),
        new FastConnectCommand.Server("reallyworld", "rw", "mc.reallyworld.ru"),
        new FastConnectCommand.Server("spookytime", "sp", "tcpshield.spookytime.net")
    );

    public FastConnectCommand() {
        super("fastconnect");
    }

    @Override
    public List<String> getAliases() {
        return List.of("fc");
    }

    @Override
    public void execute(String[] strings) {
        if (strings.length < 1) {
            ChatUtil.print(Language.translate("command.fastconnect.usage"));
        } else {
            String string = strings[0].trim();
            FastConnectCommand.Server server = this.findServer(string);
            if (server == null) {
                server = this.parseAddress(string);
            }

            if (server == null) {
                ChatUtil.printError(String.format(Language.translate("command.fastconnect.unknown"), string));
            } else if (strings.length < 2 || this.applyNick(strings[1].trim())) {
                ServerAddress serverAddress = ServerAddress.parse(server.address());
                ServerInfo serverInfo = new ServerInfo(server.name(), server.address(), ServerType.OTHER);
                ClientPlayNetworkHandler clientPlayNetworkHandler = mc.getNetworkHandler();
                if (clientPlayNetworkHandler != null) {
                    clientPlayNetworkHandler.getConnection().disconnect(Text.literal(String.format(Language.translate("command.fastconnect.reconnect"), server.name())));
                }

                ChatUtil.print(Language.translate("command.fastconnect.connecting"), server.name());
                mc.execute(() -> ConnectScreen.connect(new TitleScreen(), mc, serverAddress, serverInfo, false, null));
            }
        }
    }

    private boolean applyNick(String string) {
        if (string.isEmpty()) {
            ChatUtil.printError(Language.translate("command.fastconnect.nick.empty"));
            return false;
        }

        if (string.equalsIgnoreCase("random")) {
            string = NickGeneratorUtil.generate();
        }

        AccountManager accountManager = Sadnes.getInstance().getAccountManager();
        Account account = this.findOrCreateAccount(accountManager, string);
        if (!accountManager.apply(account)) {
            ChatUtil.printError(String.format(Language.translate("command.fastconnect.nick.failed"), string));
            return false;
        } else {
            ChatUtil.print(Language.translate("command.fastconnect.nick.applied"), string);
            return true;
        }
    }

    private Account findOrCreateAccount(AccountManager accountManager, String string) {
        for (Account account : accountManager.getAccounts()) {
            if (account.getType() == Account.Type.OFFLINE && account.getUsername().equalsIgnoreCase(string)) {
                return account;
            }
        }

        return accountManager.addOffline(string);
    }

    private FastConnectCommand.Server findServer(String string) {
        for (FastConnectCommand.Server server : SERVERS) {
            if (server.name().equalsIgnoreCase(string) || server.alias().equalsIgnoreCase(string)) {
                return server;
            }
        }

        return null;
    }

    private FastConnectCommand.Server parseAddress(String string) {
        String string2 = string;
        int i = string.indexOf(58);
        if (i >= 0) {
            string2 = string.substring(0, i);
            String string3 = string.substring(i + 1);
            if (string3.isEmpty()) {
                return null;
            }

            int i2 = 0;

            for (int i3 = string3.length(); i2 < i3; i2++) {
                if (!Character.isDigit(string3.charAt(i2))) {
                    return null;
                }
            }
        }

        if (string2.indexOf(46) < 0) {
            return null;
        }

        int i4 = 0;

        for (int i5 = string2.length(); i4 < i5; i4++) {
            char c = string2.charAt(i4);
            if (!Character.isLetterOrDigit(c)) {
                switch (c) {
                    case '-':
                    case '.':
                    case '_':
                        break;
                    default:
                        return null;
                }
            }
        }

        return new FastConnectCommand.Server(string2, "", string);
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length == 1) {
            ArrayList arrayList = new ArrayList();

            for (FastConnectCommand.Server server : SERVERS) {
                arrayList.add(server.name());
            }

            return CommandManager.filterByPrefix(arrayList, strings[0]);
        } else {
            if (strings.length != 2) {
                return List.of();
            }

            ArrayList arrayList2 = new ArrayList();
            arrayList2.add("random");

            for (Account account : Sadnes.getInstance().getAccountManager().getAccounts()) {
                arrayList2.add(account.getUsername());
            }

            return CommandManager.filterByPrefix(arrayList2, strings[1]);
        }
    }

    record Server(String name, String alias, String address) {
    }
}
