package tech.sadnes.client.ui.menu;

import tech.sadnes.client.lang.Language;

public enum GraphicsQuality {
    LOW("menu.graphics.low", "Плохая графика"),
    MEDIUM("menu.graphics.medium", "Хорошая графика"),
    HIGH("menu.graphics.high", "Высокая графика");

    private static GraphicsQuality current = HIGH;
    private final String translationKey;
    private final String fallbackName;

    GraphicsQuality(String string, String string2) {
        this.translationKey = string;
        this.fallbackName = string2;
    }

    public static GraphicsQuality getCurrent() {
        return current;
    }

    public static void setCurrent(GraphicsQuality graphicsQuality) {
        if (graphicsQuality != null) {
            current = graphicsQuality;
        }
    }

    public static void setByName(String string) {
        String string2 = "ULTRA".equals(string) ? HIGH.name() : string;

        for (GraphicsQuality graphicsQuality : values()) {
            if (graphicsQuality.name().equals(string2)) {
                current = graphicsQuality;
                return;
            }
        }
    }

    public static void setByIndex(int i) {
        GraphicsQuality[] graphicsQualities = new GraphicsQuality[]{LOW, MEDIUM, HIGH};
        if (i >= 0 && i < graphicsQualities.length) {
            current = graphicsQualities[i];
        }
    }

    public GraphicsQuality next() {
        GraphicsQuality[] graphicsQualities = values();
        return graphicsQualities[(this.ordinal() + 1) % graphicsQualities.length];
    }

    public boolean isEffectsEnabled() {
        return this != LOW;
    }

    public boolean isHigh() {
        return this == HIGH;
    }

    public String getDisplayName() {
        return Language.translate(this.translationKey, this.fallbackName);
    }
}
