package tech.sadnes.client.render.shader;

import java.nio.ByteBuffer;
import net.minecraft.client.gl.DynamicUniformStorage.Uploadable;
import org.joml.Matrix4fc;

public record SkyUniforms(
    float time,
    int variant,
    float brightness,
    float opacity,
    int quality,
    float red,
    float green,
    float blue,
    Matrix4fc previousViewProjection,
    float scaleX,
    float scaleY,
    float offsetX,
    float offsetY,
    int jitterX,
    int jitterY,
    float originTop,
    float historyValid,
    float pixelScale
) implements Uploadable {
    public static final int BLOCK_SIZE = 144;

    @Override
    public void write(ByteBuffer byteBuffer) {
        byteBuffer.putFloat(this.time);
        byteBuffer.putInt(this.variant);
        byteBuffer.putFloat(this.brightness);
        byteBuffer.putFloat(this.opacity);
        byteBuffer.putInt(this.quality);
        byteBuffer.putFloat(this.red);
        byteBuffer.putFloat(this.green);
        byteBuffer.putFloat(this.blue);

        for (float f : this.previousViewProjection.get(new float[16])) {
            byteBuffer.putFloat(f);
        }

        byteBuffer.putFloat(this.scaleX).putFloat(this.scaleY).putFloat(this.offsetX).putFloat(this.offsetY);
        byteBuffer.putInt(this.jitterX).putInt(this.jitterY).putFloat(this.originTop).putFloat(this.historyValid);
        byteBuffer.putFloat(this.pixelScale).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
    }
}
