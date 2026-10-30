package tech.sadnes.client.animation;


public final class Easings {
    public static final double BACK_C1 = 1.70158;
    public static final double BACK_C2 = 2.5949095;
    public static final double BACK_C3 = 2.70158;
    public static final double ELASTIC_C4 = Math.PI * 2.0 / 3.0;
    public static final double ELASTIC_C5 = Math.PI * 4.0 / 9.0;
    public static final Easing LINEAR = d -> d;
    public static final Easing QUAD_IN = polyIn(2);
    public static final Easing QUAD_OUT = polyOut(2);
    public static final Easing QUAD_IN_OUT = powInOut(2.0);
    public static final Easing CUBIC_IN = polyIn(3);
    public static final Easing CUBIC_OUT = polyOut(3);
    public static final Easing CUBIC_IN_OUT = powInOut(3.0);
    public static final Easing QUART_IN = polyIn(4);
    public static final Easing QUART_OUT = polyOut(4);
    public static final Easing QUART_IN_OUT = powInOut(4.0);
    public static final Easing QUINT_IN = polyIn(5);
    public static final Easing QUINT_OUT = polyOut(5);
    public static final Easing QUINT_IN_OUT = powInOut(5.0);
    public static final Easing SINE_IN = d -> 1.0 - Math.cos(d * Math.PI / 2.0);
    public static final Easing SINE_OUT = d -> Math.sin(d * Math.PI / 2.0);
    public static final Easing SINE_IN_OUT = d -> -(Math.cos(Math.PI * d) - 1.0) / 2.0;
    public static final Easing CIRC_IN = d -> 1.0 - Math.sqrt(1.0 - Math.pow(d, 2.0));
    public static final Easing CIRC_OUT = d -> Math.sqrt(1.0 - Math.pow(d - 1.0, 2.0));
    public static final Easing CIRC_IN_OUT = d -> d < 0.5
        ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * d, 2.0))) / 2.0
        : (Math.sqrt(1.0 - Math.pow(-2.0 * d + 2.0, 2.0)) + 1.0) / 2.0;
    public static final Easing ELASTIC_IN = d -> d != 0.0 && d != 1.0
        ? Math.pow(-2.0, 10.0 * d - 10.0) * Math.sin((d * 10.0 - 10.75) * (Math.PI * 2.0 / 3.0))
        : d;
    public static final Easing ELASTIC_OUT = d -> d != 0.0 && d != 1.0
        ? Math.pow(2.0, -10.0 * d) * Math.sin((d * 10.0 - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0
        : d;
    public static final Easing ELASTIC_IN_OUT = d -> {
        if (d == 0.0 || d == 1.0) {
            return d;
        } else {
            return d < 0.5
                ? -(Math.pow(2.0, 20.0 * d - 10.0) * Math.sin((20.0 * d - 11.125) * (Math.PI * 4.0 / 9.0))) / 2.0
                : Math.pow(2.0, -20.0 * d + 10.0) * Math.sin((20.0 * d - 11.125) * (Math.PI * 4.0 / 9.0)) / 2.0 + 1.0;
        }
    };
    public static final Easing EXPO_IN = d -> d != 0.0 ? Math.pow(2.0, 10.0 * d - 10.0) : d;
    public static final Easing EXPO_OUT = d -> d != 1.0 ? 1.0 - Math.pow(2.0, -10.0 * d) : d;
    public static final Easing EXPO_IN_OUT = d -> {
        if (d == 0.0 || d == 1.0) {
            return d;
        } else {
            return d < 0.5 ? Math.pow(2.0, 20.0 * d - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * d + 10.0)) / 2.0;
        }
    };
    public static final Easing BACK_IN = d -> 2.70158 * Math.pow(d, 3.0) - 1.70158 * Math.pow(d, 2.0);
    public static final Easing BACK_OUT = d -> 1.0 + 2.70158 * Math.pow(d - 1.0, 3.0) + 1.70158 * Math.pow(d - 1.0, 2.0);
    public static final Easing BACK_IN_OUT = d -> d < 0.5
        ? Math.pow(2.0 * d, 2.0) * (7.189819 * d - 2.5949095) / 2.0
        : (Math.pow(2.0 * d - 2.0, 2.0) * (3.5949095 * (d * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0;
    public static final Easing BOUNCE_OUT = d -> {
        if (d < 1.0 / 2.75) {
            return 7.5625 * Math.pow(d, 2.0);
        } else if (d < 2.0 / 2.75) {
            return 7.5625 * Math.pow(d - 1.5 / 2.75, 2.0) + 0.75;
        } else {
            return d < 2.5 / 2.75 ? 7.5625 * Math.pow(d - 2.25 / 2.75, 2.0) + 0.9375 : 7.5625 * Math.pow(d - 2.625 / 2.75, 2.0) + 0.984375;
        }
    };
    public static final Easing BOUNCE_IN = d -> 1.0 - BOUNCE_OUT.ease(1.0 - d);
    public static final Easing BOUNCE_IN_OUT = d -> d < 0.5
        ? (1.0 - BOUNCE_OUT.ease(1.0 - 2.0 * d)) / 2.0
        : (1.0 + BOUNCE_OUT.ease(2.0 * d - 1.0)) / 2.0;

    public static Easing powIn(double d) {
        return d2 -> Math.pow(d2, d);
    }

    public static Easing polyIn(int i) {
        return powIn(i);
    }

    public static Easing powOut(double d) {
        return d2 -> 1.0 - Math.pow(1.0 - d2, d);
    }

    public static Easing polyOut(int i) {
        return powOut(i);
    }

    public static Easing powInOut(double d) {
        return d2 -> d2 < 0.5 ? Math.pow(2.0, d - 1.0) * Math.pow(d2, d) : 1.0 - Math.pow(-2.0 * d2 + 2.0, d) / 2.0;
    }

    private Easings() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
