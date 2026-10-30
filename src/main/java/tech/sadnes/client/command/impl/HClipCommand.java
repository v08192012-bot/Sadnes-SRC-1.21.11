package tech.sadnes.client.command.impl;

import java.util.List;
import tech.sadnes.client.command.Command;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.player.ClipUtil;

public final class HClipCommand extends Command {
    public HClipCommand() {
        super("hclip");
    }

    @Override
    public void execute(String[] strings) {
        if (mc.player != null) {
            if (strings.length == 0) {
                ChatUtil.print("Используй: .hclip <число>");
            } else {
                double d = ClipUtil.parseDouble(strings[0]);
                double d2 = Math.toRadians(mc.player.getYaw());
                double d3 = -Math.sin(d2) * d;
                double d4 = Math.cos(d2) * d;
                ClipUtil.teleport(mc.player.getX() + d3, mc.player.getY(), mc.player.getZ() + d4);
                ChatUtil.print(String.format("Горизонтальный клип на %.1f блоков.", d));
            }
        }
    }

    @Override
    public List<String> complete(String[] strings) {
        return List.of();
    }
}
