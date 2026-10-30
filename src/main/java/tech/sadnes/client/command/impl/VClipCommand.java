package tech.sadnes.client.command.impl;

import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.command.CommandManager;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.player.ClipUtil;

public final class VClipCommand extends Command {
    private static final List<String> SUBCOMMANDS = List.of("up", "down");

    public VClipCommand() {
        super("vclip");
    }

    @Override
    public void execute(String[] strings) {
        if (mc.player != null) {
            if (strings.length == 0) {
                ChatUtil.print("Используй: .vclip up | down | <число>");
            } else {
                String string = strings[0].toLowerCase(Locale.US);
                double d = mc.player.getX();
                double d2 = mc.player.getZ();
                if (!string.equals("up") && !string.equals("down")) {
                    double d3 = ClipUtil.parseDouble(strings[0]);
                    ClipUtil.teleport(d, mc.player.getY() + d3, d2);
                    ChatUtil.print(String.format("Вертикальный клип на %.1f блоков.", d3));
                } else {
                    boolean flag = string.equals("up");
                    OptionalInt optionalInt = ClipUtil.findSurfaceY(d, mc.player.getY(), d2, flag);
                    if (optionalInt.isEmpty()) {
                        ChatUtil.printError("Поверхность " + (flag ? "сверху" : "снизу") + " не найдена.");
                    } else {
                        ClipUtil.teleport(d, optionalInt.getAsInt(), d2);
                        ChatUtil.print("Клип на поверхность " + (flag ? "сверху" : "снизу") + " (Y " + optionalInt.getAsInt() + ").");
                    }
                }
            }
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        if (strings.length <= 1) {
            return strings.length == 0 ? CommandManager.filterByPrefix(SUBCOMMANDS, "") : CommandManager.filterByPrefix(SUBCOMMANDS, strings[0]);
        } else {
            return List.of();
        }
    }
}
