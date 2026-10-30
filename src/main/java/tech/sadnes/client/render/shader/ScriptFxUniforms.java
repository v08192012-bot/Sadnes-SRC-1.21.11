package tech.sadnes.client.render.shader;

import java.nio.ByteBuffer;
import net.minecraft.client.gl.DynamicUniformStorage.Uploadable;

public record ScriptFxUniforms(float time, float size, float param0, float param1, float param2, float param3, float pad0, float pad1) implements Uploadable {
    public static final int BLOCK_SIZE = 32;

    @Override
    public void write(ByteBuffer byteBuffer) {
        byteBuffer.putFloat(this.time);
        byteBuffer.putFloat(this.size);
        byteBuffer.putFloat(this.param0);
        byteBuffer.putFloat(this.param1);
        byteBuffer.putFloat(this.param2);
        byteBuffer.putFloat(this.param3);
        byteBuffer.putFloat(this.pad0);
        byteBuffer.putFloat(this.pad1);
    }
}
