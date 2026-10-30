package tech.sadnes.client.util.chat;

import java.util.Optional;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public final class TextDebugUtil {
    private TextDebugUtil() {
    }

    public static String describeText(Text text2) {
        if (text2 == null) {
            return "-";
        }

        StringBuilder builder = new StringBuilder();
        text2.visit((style, string) -> {
            if (string.isEmpty()) {
                return Optional.empty();
            }

            builder.append('[').append(escapeUnicode(string)).append(" color=").append(style.getColor()).append(" font=").append(style.getFont()).append("] ");
            return Optional.empty();
        }, Style.EMPTY);
        return builder.toString();
    }

    public static String escapeUnicode(String string) {
        if (string == null) {
            return "-";
        }

        StringBuilder builder = new StringBuilder();
        string.codePoints().forEach(i -> {
            boolean flag = i >= 32 && i < 127;
            if (flag) {
                builder.appendCodePoint(i);
            } else {
                builder.append(String.format("\\u%04X", i));
            }
        });
        return builder.toString();
    }
}
