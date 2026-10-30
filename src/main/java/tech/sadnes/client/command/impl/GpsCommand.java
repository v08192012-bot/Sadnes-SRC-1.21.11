package tech.sadnes.client.command.impl;

import java.util.List;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.ui.hud.GpsIndicator;
import tech.sadnes.client.util.chat.ChatUtil;

public final class GpsCommand extends Command {
    private static final List<String> SUGGESTIONS = List.of("x", "off");

    public GpsCommand() {
        super("gps");
    }

    @Override
    public void execute(String[] strings) {
        if (strings.length == 0) {
            if (mc.player != null) {
                this.setTarget(mc.player.getX(), mc.player.getZ());
            }
        } else if (strings[0].equalsIgnoreCase("off")) {
            GpsIndicator.clear();
            ChatUtil.print(Language.translate("command.gps.off"));
        } else if (strings.length < 2) {
            ChatUtil.print(Language.translate("command.gps.usage"));
        } else {
            try {
                this.setTarget(Double.parseDouble(strings[0]), Double.parseDouble(strings[1]));
            } catch (NumberFormatException numberFormatException) {
                ChatUtil.print(Language.translate("command.gps.notnumber"));
            }
        }
    }

    private void setTarget(double d, double d2) {
        if (d % 1.0 == 0.0) {
            d += 0.5;
        }

        if (d2 % 1.0 == 0.0) {
            d2 += 0.5;
        }

        GpsIndicator.setTarget(d, d2);
        ChatUtil.print(Language.translate("command.gps.set"), (int)d, (int)d2);
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(SUGGESTIONS, "") : CommandManager.filterByPrefix(SUGGESTIONS, strings[0]);
        } else {
            return !strings[0].equalsIgnoreCase("off") && strings.length == 2 ? CommandManager.filterByPrefix(List.of("z"), strings[1]) : List.of();
        }
    }
}
