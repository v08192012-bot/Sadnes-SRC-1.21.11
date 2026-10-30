package tech.sadnes.client.render.shader;

import java.nio.ByteBuffer;
import net.minecraft.client.gl.DynamicUniformStorage.Uploadable;

public record ChamsUniforms(float time, float speed, float scale, float type) implements Uploadable {
    public static final int BLOCK_SIZE = 16;

    @Override
    public void write(ByteBuffer byteBuffer) {
        byteBuffer.putFloat(this.time);
        byteBuffer.putFloat(this.speed);
        byteBuffer.putFloat(this.scale);
        byteBuffer.putFloat(this.type);
    }
}
