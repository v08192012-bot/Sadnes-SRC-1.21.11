package tech.sadnes.client.util.math;

import java.util.HashMap;
import java.util.Map;

public class Duration {
    private static final Map<String, Duration> CACHE = new HashMap<>();
    public float value;

    public static Duration zero() {
        return ofMillis(0.0F);
    }

    private Duration() {
    }

    private Duration(float f) {
        this.value = f;
    }

    public static Duration ofSeconds(float f) {
        return ofMillis(f * 1000.0F);
    }

    public static Duration ofMillis(float f) {
        String string = String.valueOf(f);
        if (CACHE.containsKey(string)) {
            return CACHE.get(string);
        }

        Duration duration = new Duration(f);
        CACHE.put(string, duration);
        return duration;
    }

    public Duration subtract(float f) {
        this.value -= f;
        return this;
    }

    public Duration add(float f) {
        this.value += f;
        return this;
    }

    public Duration multiply(float f) {
        this.value *= f;
        return this;
    }

    public Duration set(float f) {
        this.value = f;
        return this;
    }

    public float getValue() {
        return this.value;
    }
}
