package tech.sadnes.client.server;

import tech.sadnes.client.util.color.ColorUtil;

public enum MarkerLevel {
    WARNING("marker.level.warning", ColorUtil.fromHex("ff7a33")),
    CRITICAL("marker.level.critical", ColorUtil.fromHex("ff3b30"));

    private final String translationKey;
    private final int color;

    MarkerLevel(String string, int i) {
        this.translationKey = string;
        this.color = i;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public int getColor() {
        return this.color;
    }
}
