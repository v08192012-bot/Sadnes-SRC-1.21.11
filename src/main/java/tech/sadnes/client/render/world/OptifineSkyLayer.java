package tech.sadnes.client.render.world;

import java.util.Properties;
import net.minecraft.util.Identifier;

final class OptifineSkyLayer {
    final String source;
    Identifier texture;
    final int blend;
    final boolean rotate;
    final float speed;
    final float[] axis;
    private final int startFadeIn;
    private final int endFadeIn;
    private int startFadeOut;
    private final int endFadeOut;
    private final boolean weatherClear;
    private final boolean weatherRain;
    private final boolean weatherThunder;
    private static final float[] DEFAULT_AXIS = new float[]{1.0F, 0.0F, 0.0F};

    OptifineSkyLayer(Properties properties, String string) {
        this.source = properties.getProperty("source", string);
        this.startFadeIn = parseTime(properties.getProperty("startFadeIn"));
        this.endFadeIn = parseTime(properties.getProperty("endFadeIn"));
        this.startFadeOut = parseTime(properties.getProperty("startFadeOut"));
        this.endFadeOut = parseTime(properties.getProperty("endFadeOut"));
        this.blend = OptifineBlendMode.parse(properties.getProperty("blend"));
        this.rotate = parseBoolean(properties.getProperty("rotate"), true);
        this.speed = parseFloat(properties.getProperty("speed"), 1.0F);
        this.axis = parseAxis(properties.getProperty("axis"));
        String string2 = properties.getProperty("weather", "clear");
        this.weatherClear = string2.contains("clear");
        this.weatherRain = string2.contains("rain");
        this.weatherThunder = string2.contains("thunder");
    }

    boolean isValid() {
        if (this.source == null) {
            return false;
        }

        if (this.startFadeIn >= 0 && this.endFadeIn >= 0 && this.endFadeOut >= 0) {
            int i = normalizeTime(this.endFadeIn - this.startFadeIn);
            if (this.startFadeOut < 0) {
                this.startFadeOut = normalizeTime(this.endFadeOut - i);
                if (isBetween(this.startFadeOut, this.startFadeIn, this.endFadeIn)) {
                    this.startFadeOut = this.endFadeIn;
                }
            }

            int i2 = normalizeTime(this.endFadeIn - this.startFadeIn)
                + normalizeTime(this.startFadeOut - this.endFadeIn)
                + normalizeTime(this.endFadeOut - this.startFadeOut)
                + normalizeTime(this.startFadeIn - this.endFadeOut);
            return i2 == 24000 && this.speed >= 0.0F;
        } else {
            return false;
        }
    }

    boolean isActive(int i) {
        return !isBetween(i, this.endFadeOut, this.startFadeIn);
    }

    float getBrightness(int i, float f, float f2) {
        float f3 = this.getFadeBrightness(i) * this.getWeatherBrightness(f, f2);
        return Math.max(0.0F, Math.min(1.0F, f3));
    }

    private float getFadeBrightness(int i) {
        if (isBetween(i, this.startFadeIn, this.endFadeIn)) {
            int i2 = normalizeTime(this.endFadeIn - this.startFadeIn);
            return i2 == 0 ? 1.0F : (float)normalizeTime(i - this.startFadeIn) / i2;
        } else if (isBetween(i, this.endFadeIn, this.startFadeOut)) {
            return 1.0F;
        } else if (isBetween(i, this.startFadeOut, this.endFadeOut)) {
            int i3 = normalizeTime(this.endFadeOut - this.startFadeOut);
            return i3 == 0 ? 0.0F : 1.0F - (float)normalizeTime(i - this.startFadeOut) / i3;
        } else {
            return 0.0F;
        }
    }

    private float getWeatherBrightness(float f, float f2) {
        float f3 = 1.0F - f;
        float f4 = f - f2;
        float f5 = 0.0F;
        if (this.weatherClear) {
            f5 = 0.0F + f3;
        }

        if (this.weatherRain) {
            f5 += f4;
        }

        if (this.weatherThunder) {
            f5 += f2;
        }

        return Math.max(0.0F, Math.min(1.0F, f5));
    }

    private static int parseTime(String string) {
        if (string == null) {
            return -1;
        }

        String[] strings = string.split(":");
        if (strings.length != 2) {
            return -1;
        }

        int i = parseInt(strings[0]);
        int i2 = parseInt(strings[1]);
        if (i >= 0 && i <= 23 && i2 >= 0 && i2 <= 59) {
            i -= 6;
            if (i < 0) {
                i += 24;
            }

            return i * 1000 + (int)(i2 / 60.0 * 1000.0);
        } else {
            return -1;
        }
    }

    private static int normalizeTime(int i) {
        while (i >= 24000) {
            i -= 24000;
        }

        while (i < 0) {
            i += 24000;
        }

        return i;
    }

    private static boolean isBetween(int i, int i2, int i3) {
        return i2 <= i3 ? i >= i2 && i <= i3 : i >= i2 || i <= i3;
    }

    private static boolean parseBoolean(String string, boolean flag) {
        if (string == null) {
            return flag;
        } else if (string.equalsIgnoreCase("true")) {
            return true;
        } else {
            return string.equalsIgnoreCase("false") ? false : flag;
        }
    }

    private static float parseFloat(String string, float f) {
        if (string == null) {
            return f;
        }

        try {
            return Float.parseFloat(string.trim());
        } catch (NumberFormatException numberFormatException) {
            return f;
        }
    }

    private static int parseInt(String string) {
        try {
            return Integer.parseInt(string.trim());
        } catch (NumberFormatException numberFormatException) {
            return -1;
        }
    }

    private static float[] parseAxis(String string) {
        if (string == null) {
            return DEFAULT_AXIS;
        } else {
            String[] strings = string.trim().split("\\s+");
            if (strings.length != 3) {
                return DEFAULT_AXIS;
            } else {
                float f = parseFloat(strings[0], Float.MIN_VALUE);
                float f2 = parseFloat(strings[1], Float.MIN_VALUE);
                float f3 = parseFloat(strings[2], Float.MIN_VALUE);
                if (f != Float.MIN_VALUE && f2 != Float.MIN_VALUE && f3 != Float.MIN_VALUE) {
                    return f * f + f2 * f2 + f3 * f3 < 1.0E-5F ? DEFAULT_AXIS : new float[]{f3, f2, -f};
                } else {
                    return DEFAULT_AXIS;
                }
            }
        }
    }
}
