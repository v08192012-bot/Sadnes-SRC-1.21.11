package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gl.DynamicUniformStorage;

public final class ScriptFxUniformStorage {
    private static final int CAPACITY = 64;
    private static DynamicUniformStorage<ScriptFxUniforms> storage;

    private ScriptFxUniformStorage() {
    }

    public static void init() {
        if (storage == null) {
            storage = new DynamicUniformStorage<>("SadnesScriptFx", 32, 64);
        }
    }

    public static GpuBufferSlice write(ScriptFxUniforms scriptFxUniforms) {
        if (storage == null) {
            init();
        }

        return storage.write(scriptFxUniforms);
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
