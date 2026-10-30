package tech.sadnes.client.util.chat;

import java.util.regex.Pattern;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Formatting;

public final class ChatMessageUtil {
    private static final Pattern ERROR_PATTERN = Pattern.compile("не можете|подождать|не существует|\\b(?:can(?:['’]|no)t|wait|unknown)\\b|not exist", 66);

    private ChatMessageUtil() {
    }

    public static String getPlainText(GameMessageS2CPacket gameMessageS2CPacket) {
        return Formatting.strip(gameMessageS2CPacket.content().getString()).strip();
    }

    public static boolean isPlayerMessage(String string) {
        return string.startsWith("<") || string.startsWith("⇄");
    }

    public static boolean isErrorMessage(String string) {
        return !isPlayerMessage(string) && ERROR_PATTERN.matcher(string).find();
    }
}
