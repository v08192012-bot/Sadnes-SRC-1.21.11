package tech.sadnes.client.util.chat;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import tech.sadnes.client.util.IMinecraft;

public final class ChatUtil implements IMinecraft {
    public static void print(Object object, Object... objects) {
        if (mc.player != null) {
            if (object == null) {
                print("Object is null");
            } else {
                String string = objects.length == 0 ? String.valueOf(object) : String.format(String.valueOf(object), objects);
                mc.execute(
                    () -> {
                        if (mc.player != null && mc.inGameHud != null) {
                            MutableText mutableText = ChatTextUtil.createBoldColored(" Sadnes client")
                                .append(Text.literal(" → ").setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
                                .append(Text.literal(string).setStyle(Style.EMPTY.withColor(Formatting.RESET)));
                            mc.inGameHud.getChatHud().addMessage(mutableText);
                        }
                    }
                );
            }
        }
    }

    public static void printError(Object object) {
        print("ERROR " + object);
    }

    public static void sendChat(String string) {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendChatCommand(string);
        }
    }
}
