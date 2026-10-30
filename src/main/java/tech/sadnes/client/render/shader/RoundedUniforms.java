package tech.sadnes.client.render.shader;

import java.nio.ByteBuffer;
import net.minecraft.client.gl.DynamicUniformStorage.Uploadable;
import tech.sadnes.client.render.Radius;

public record RoundedUniforms(
    float radiusLeftBottom,
    float radiusLeftTop,
    float radiusRightBottom,
    float radiusRightTop,
    float rectWidth,
    float rectHeight,
    float smoothness1,
    float smoothness2,
    float guiWidth,
    float guiHeight,
    float value,
    float thickness,
    float alphaMul,
    int type
) implements Uploadable {
    public static final int BLOCK_SIZE = 64;

    public static RoundedUniforms of(
        Radius radius, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int i
    ) {
        return new RoundedUniforms(
            radius.getTopLeft(), radius.getTopRight(), radius.getBottomRight(), radius.getBottomLeft(), f, f2, f3, f4, f5, f6, f7, f8, f9, i
        );
    }

    @Override
    public void write(ByteBuffer byteBuffer) {
        byteBuffer.putFloat(this.radiusLeftBottom);
        byteBuffer.putFloat(this.radiusLeftTop);
        byteBuffer.putFloat(this.radiusRightBottom);
        byteBuffer.putFloat(this.radiusRightTop);
        byteBuffer.putFloat(this.rectWidth);
        byteBuffer.putFloat(this.rectHeight);
        byteBuffer.putFloat(this.smoothness1);
        byteBuffer.putFloat(this.smoothness2);
        byteBuffer.putFloat(this.guiWidth);
        byteBuffer.putFloat(this.guiHeight);
        byteBuffer.putFloat(this.value);
        byteBuffer.putFloat(this.thickness);
        byteBuffer.putFloat(this.alphaMul);
        byteBuffer.putInt(this.type);
        byteBuffer.putFloat(0.0F);
        byteBuffer.putFloat(0.0F);
    }
}
