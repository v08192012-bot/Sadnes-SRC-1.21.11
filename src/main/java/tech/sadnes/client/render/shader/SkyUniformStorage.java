package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gl.DynamicUniformStorage;

public final class SkyUniformStorage {
    private static final int CAPACITY = 16;
    private static DynamicUniformStorage<SkyUniforms> storage;

    private SkyUniformStorage() {
    }

    public static void init() {
        if (storage == null) {
            String string = "SadnesSky";
            storage = new DynamicUniformStorage<>(string, 144, 16);
        }
    }

    public static GpuBufferSlice write(SkyUniforms skyUniforms) {
        if (storage == null) {
            init();
        }

        return storage.write(skyUniforms);
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
