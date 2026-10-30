package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gl.DynamicUniformStorage;

public final class RoundedUniformStorage {
    private static final int CAPACITY = 256;
    private static DynamicUniformStorage<RoundedUniforms> storage;

    private RoundedUniformStorage() {
    }

    public static void init() {
        if (storage == null) {
            String string = "SadnesRounded";
            storage = new DynamicUniformStorage<>(string, 64, 256);
        }
    }

    public static GpuBufferSlice write(RoundedUniforms roundedUniforms) {
        if (storage == null) {
            throw new IllegalStateException("SadnesRoundedUniforms not initialized");
        } else {
            return storage.write(roundedUniforms);
        }
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
