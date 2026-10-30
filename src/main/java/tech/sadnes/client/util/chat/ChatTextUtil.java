package tech.sadnes.client.util.chat;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public final class ChatTextUtil {
    public static MutableText createBoldColored(String string) {
        MutableText mutableText = Text.literal("");

        for (int i = 0; i < string.length(); i++) {
            mutableText.append(Text.literal(String.valueOf(string.charAt(i))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(15022389)).withBold(Boolean.TRUE)));
        }

        return mutableText;
    }

    private ChatTextUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
