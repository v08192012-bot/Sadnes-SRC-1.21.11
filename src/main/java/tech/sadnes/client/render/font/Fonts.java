package tech.sadnes.client.render.font;

import net.minecraft.client.MinecraftClient;
import tech.sadnes.client.Sadnes;

public final class Fonts {
    public static MsdfFont INTER_BOLD;
    public static MsdfFont INTER_MEDIUM;
    public static MsdfFont INTER_REGULAR;
    public static MsdfFont INTER_SEMIBOLD;
    public static MsdfFont NUNITO;

    public static void load() {
        Sadnes.getLogger().info("Loading fonts");
        if (INTER_BOLD == null) {
            INTER_BOLD = new MsdfFont("inter_bold");
            INTER_MEDIUM = new MsdfFont("inter_medium");
            INTER_SEMIBOLD = new MsdfFont("inter_semibold");
            INTER_REGULAR = new MsdfFont("inter_regular");
            NUNITO = new MsdfFont("nunito");
            EmojiRenderer.getInstance().load();
        } else {
            INTER_BOLD.reload("inter_bold");
            INTER_MEDIUM.reload("inter_medium");
            INTER_SEMIBOLD.reload("inter_semibold");
            INTER_REGULAR.reload("inter_regular");
            NUNITO.reload("nunito");
            EmojiRenderer.getInstance().load();
        }
    }

    public static void closeAll() {
        closeFont(INTER_BOLD);
        closeFont(INTER_MEDIUM);
        closeFont(INTER_SEMIBOLD);
        closeFont(INTER_REGULAR);
        closeFont(NUNITO);
    }

    private static void closeFont(MsdfFont msdfFont) {
        if (msdfFont != null) {
            msdfFont.close();
        }
    }

    public static MsdfFont getDefault() {
        return INTER_MEDIUM;
    }

    public static float getWidth(String string, float f) {
        if (string != null && !string.isEmpty()) {
            MsdfFont msdfFont = getDefault();
            return msdfFont != null ? msdfFont.getWidth(string, f) : MinecraftClient.getInstance().textRenderer.getWidth(string);
        } else {
            return 0.0F;
        }
    }
}
