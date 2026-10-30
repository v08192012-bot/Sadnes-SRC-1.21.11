package tech.sadnes.client.setting;

public enum Unit {
    PERCENT("%"),
    MILLISECOND("ms"),
    HOUR("h"),
    SECOND("s"),
    PIXEL("px"),
    COUNT("x"),
    METER("m"),
    KILOMETER("km"),
    RADIAN("c"),
    DEGREE("°"),
    DEGREE_PER_SECOND("°/s"),
    HP("hp"),
    EMPTY("");

    private final String suffix;

    Unit(String string) {
        this.suffix = string;
    }

    public String getSuffix() {
        return this.suffix;
    }
}
