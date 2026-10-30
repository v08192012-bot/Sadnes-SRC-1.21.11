package tech.sadnes.client.command.impl;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.account.Account;
import tech.sadnes.client.account.AccountManager;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.util.NickGeneratorUtil;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.network.LastServerInfo;

public final class LoginCommand extends Command {
    public LoginCommand() {
        super("login");
    }

    @Override
    public List<String> getAliases() {
        return List.of("l");
    }

    @Override
    public void execute(String[] strings) {
        if (strings.length < 1) {
            ChatUtil.print("Используй: .login NICKNAME | .login random");
        } else {
            String string = strings[0].trim();
            if (string.isEmpty()) {
                ChatUtil.printError("Ник не может быть пустым.");
            } else {
                if (string.equalsIgnoreCase("random")) {
                    string = NickGeneratorUtil.generate();
                }

                ServerInfo serverInfo = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry() : LastServerInfo.getServerInfo();
                ServerAddress serverAddress = serverInfo != null ? ServerAddress.parse(serverInfo.address) : LastServerInfo.getAddress();
                if (serverInfo != null && serverAddress != null) {
                    AccountManager accountManager = Sadnes.getInstance().getAccountManager();
                    Account account = this.findOrCreateAccount(accountManager, string);
                    if (!accountManager.apply(account)) {
                        ChatUtil.printError("Не удалось применить аккаунт " + string + ".");
                    } else {
                        ClientPlayNetworkHandler clientPlayNetworkHandler = mc.getNetworkHandler();
                        if (clientPlayNetworkHandler != null) {
                            clientPlayNetworkHandler.getConnection().disconnect(Text.literal("Смена ника: " + string));
                        }

                        ChatUtil.print("Перезаход под ником " + string + "...");
                        mc.execute(() -> ConnectScreen.connect(new TitleScreen(), mc, serverAddress, serverInfo, false, null));
                    }
                } else {
                    ChatUtil.printError("Не на сервере — сменить ник нельзя.");
                }
            }
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

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length != 1) {
            return List.of();
        }

        ArrayList arrayList = new ArrayList();
        arrayList.add("random");

        for (Account account : Sadnes.getInstance().getAccountManager().getAccounts()) {
            arrayList.add(account.getUsername());
        }

        return CommandManager.filterByPrefix(arrayList, strings[0]);
    }
}
