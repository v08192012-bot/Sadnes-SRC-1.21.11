package tech.sadnes.client.animation;


public enum EasingMode {
    LINEAR(Easings.LINEAR),
    QUAD_OUT(Easings.QUAD_OUT),
    CUBIC_OUT(Easings.CUBIC_OUT),
    QUART_OUT(Easings.QUART_OUT),
    QUINT_OUT(Easings.QUINT_OUT),
    SINE_OUT(Easings.SINE_OUT),
    CIRC_OUT(Easings.CIRC_OUT),
    ELASTIC_OUT(Easings.ELASTIC_OUT),
    EXPO_OUT(Easings.EXPO_OUT),
    BACK_OUT(Easings.BACK_OUT),
    BOUNCE_OUT(Easings.BOUNCE_OUT);

    private final Easing easing;

    @Override
    public String toString() {
        String string = this.name().toLowerCase();
        String[] strings = string.split("_");
        StringBuilder builder = new StringBuilder();

        for (String string2 : strings) {
            builder.append(Character.toUpperCase(string2.charAt(0))).append(string2.substring(1)).append(" ");
        }

        return builder.toString().trim();
    }

    public Easing getEasing() {
        return this.easing;
    }

    EasingMode(Easing easing2) {
        this.easing = easing2;
    }
}
