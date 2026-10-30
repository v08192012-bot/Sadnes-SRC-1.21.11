package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gl.DynamicUniformStorage;

public final class WeatherUniformStorage {
    private static final int CAPACITY = 8;
    private static DynamicUniformStorage<WeatherUniforms> storage;

    private WeatherUniformStorage() {
    }

    public static void init() {
        if (storage == null) {
            storage = new DynamicUniformStorage<>("SadnesWeather", 144, 8);
        }
    }

    public static GpuBufferSlice write(WeatherUniforms weatherUniforms) {
        if (storage == null) {
            init();
        }

        return storage.write(weatherUniforms);
    }

    public static void clear() {
        if (storage != null) {
            storage.clear();
        }
    }

    public static void close() {
        if (storage != null) {
            storage.close();
            storage = null;
        }
    }
}
