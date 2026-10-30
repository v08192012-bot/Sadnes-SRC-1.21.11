package tech.sadnes.client.command.impl;

import java.util.List;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.feature.component.AutoReconnectComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.chat.NotificationUtil;

public final class ReconnectCommand extends Command {
    public ReconnectCommand() {
        super("rct");
    }

    @Override
    public void execute(String[] strings) {
        if (ServerManager.isInPvp()) {
            NotificationUtil.warning(NotificationSources.reconnect(), Language.translate("command.rct.in_pvp", "Вы в режиме PvP."));
        } else if (!ServerManager.isFunTime() && !ServerManager.isSpookyTime() && !ServerManager.isReallyWorld() && !ServerManager.isHolyWorld()) {
            NotificationUtil.warning(
                NotificationSources.reconnect(), Language.translate("command.rct.unsupported", "Реконнект не поддерживается на этом сервере.")
            );
        } else {
            int i;
            if (strings.length > 0) {
                try {
                    i = Integer.parseInt(strings[0]);
                } catch (NumberFormatException numberFormatException) {
                    NotificationUtil.error(NotificationSources.reconnect(), Language.translate("command.rct.not_number", "Номер анархии должен быть числом."));
                    return;
                }
            } else {
                i = ServerManager.getAnarchyNumber();
            }

            AutoReconnectComponent autoReconnectComponent = AutoReconnectComponent.getInstance();
            if (!autoReconnectComponent.start(i)) {
                NotificationUtil.warning(NotificationSources.reconnect(), Language.translate("command.rct.unknown_anarchy", "Не удалось определить анархию."));
            } else {
                if (ServerManager.isReallyWorld()) {
                    NotificationUtil.info(
                        NotificationSources.reconnect(), String.format(Language.translate("command.rct.join_grief", "Перезахожу на %s гриф."), i)
                    );
                } else if (ServerManager.isHolyWorld()) {
                    NotificationUtil.info(
                        NotificationSources.reconnect(),
                        String.format(Language.translate("command.rct.join_holy", "Перезахожу на %s %s."), autoReconnectComponent.getHolyMode(), i)
                    );
                } else {
                    NotificationUtil.info(
                        NotificationSources.reconnect(), String.format(Language.translate("command.rct.join_anarchy", "Реконнект на анархию %s..."), i)
                    );
                }
            }
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        return List.of();
    }
}
