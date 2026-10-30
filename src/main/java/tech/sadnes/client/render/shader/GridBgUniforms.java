package tech.sadnes.client.render.shader;

import java.nio.ByteBuffer;
import net.minecraft.client.gl.DynamicUniformStorage.Uploadable;

public record GridBgUniforms(
    float time,
    float aspect,
    float mouseX,
    float mouseY,
    float accentR,
    float accentG,
    float accentB,
    float alpha,
    float baseR,
    float baseG,
    float baseB,
    float light
) implements Uploadable {
    public static final int BLOCK_SIZE = 48;

    @Override
    public void write(ByteBuffer byteBuffer) {
        byteBuffer.putFloat(this.time);
        byteBuffer.putFloat(this.aspect);
        byteBuffer.putFloat(this.mouseX);
        byteBuffer.putFloat(this.mouseY);
        byteBuffer.putFloat(this.accentR);
        byteBuffer.putFloat(this.accentG);
        byteBuffer.putFloat(this.accentB);
        byteBuffer.putFloat(this.alpha);
        byteBuffer.putFloat(this.baseR);
        byteBuffer.putFloat(this.baseG);
        byteBuffer.putFloat(this.baseB);
        byteBuffer.putFloat(this.light);
    }
}
