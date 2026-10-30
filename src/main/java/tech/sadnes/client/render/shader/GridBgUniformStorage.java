package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gl.DynamicUniformStorage;

public final class GridBgUniformStorage {
    private static final int CAPACITY = 16;
    private static DynamicUniformStorage<GridBgUniforms> storage;

    private GridBgUniformStorage() {
    }

    public static void init() {
        if (storage == null) {
            String string = "GridBg";
            storage = new DynamicUniformStorage<>(string, 48, 16);
        }
    }

    public static GpuBufferSlice write(GridBgUniforms gridBgUniforms) {
        if (storage == null) {
            init();
        }

        return storage.write(gridBgUniforms);
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
