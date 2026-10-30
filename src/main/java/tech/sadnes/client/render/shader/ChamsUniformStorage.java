package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gl.DynamicUniformStorage;

public final class ChamsUniformStorage {
    private static final int CAPACITY = 64;
    private static DynamicUniformStorage<ChamsUniforms> storage;

    private ChamsUniformStorage() {
    }

    public static void init() {
        if (storage == null) {
            String string = "SadnesChams";
            storage = new DynamicUniformStorage<>(string, 16, 64);
        }
    }

    public static GpuBufferSlice write(ChamsUniforms chamsUniforms) {
        if (storage == null) {
            init();
        }

        return storage.write(chamsUniforms);
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
