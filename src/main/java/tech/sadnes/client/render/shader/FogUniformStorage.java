package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gl.DynamicUniformStorage;

public final class FogUniformStorage {
    private static final int CAPACITY = 2;
    private static DynamicUniformStorage<FogUniforms> storage;

    private FogUniformStorage() {
    }

    public static void init() {
        if (storage == null) {
            storage = new DynamicUniformStorage<>("SadnesFog", 128, 2);
        }
    }

    public static GpuBufferSlice write(FogUniforms fogUniforms) {
        if (storage == null) {
            init();
        }

        return storage.write(fogUniforms);
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
