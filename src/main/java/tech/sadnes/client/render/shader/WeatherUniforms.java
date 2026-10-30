package tech.sadnes.client.render.shader;

import java.nio.ByteBuffer;
import net.minecraft.client.gl.DynamicUniformStorage.Uploadable;

public record WeatherUniforms(
    float boxSize,
    float innerFade,
    float originAX,
    float originAY,
    float originAZ,
    float originBX,
    float originBY,
    float originBZ,
    float originCX,
    float originCY,
    float originCZ,
    float fallX,
    float fallY,
    float fallZ,
    float streakLength,
    float particleSize,
    float opacity,
    float time,
    float mapOriginX,
    float mapOriginZ,
    float mapCamY,
    float mapSize,
    float lightR,
    float lightG,
    float lightB,
    float flash,
    float fogStart,
    float fogDensity,
    float ripples,
    float snowCover,
    float sway,
    int defaultType,
    int quality
) implements Uploadable {
    public static final int BLOCK_SIZE = 144;

    @Override
    public void write(ByteBuffer byteBuffer) {
        byteBuffer.putFloat(this.boxSize);
        byteBuffer.putFloat(this.innerFade);
        byteBuffer.putFloat(this.originAX);
        byteBuffer.putFloat(this.originAY);
        byteBuffer.putFloat(this.originAZ);
        byteBuffer.putFloat(this.originBX);
        byteBuffer.putFloat(this.originBY);
        byteBuffer.putFloat(this.originBZ);
        byteBuffer.putFloat(this.originCX);
        byteBuffer.putFloat(this.originCY);
        byteBuffer.putFloat(this.originCZ);
        byteBuffer.putFloat(this.fallX);
        byteBuffer.putFloat(this.fallY);
        byteBuffer.putFloat(this.fallZ);
        byteBuffer.putFloat(this.streakLength);
        byteBuffer.putFloat(this.particleSize);
        byteBuffer.putFloat(this.opacity);
        byteBuffer.putFloat(this.time);
        byteBuffer.putFloat(this.mapOriginX);
        byteBuffer.putFloat(this.mapOriginZ);
        byteBuffer.putFloat(this.mapCamY);
        byteBuffer.putFloat(this.mapSize);
        byteBuffer.putFloat(this.lightR);
        byteBuffer.putFloat(this.lightG);
        byteBuffer.putFloat(this.lightB);
        byteBuffer.putFloat(this.flash);
        byteBuffer.putFloat(this.fogStart);
        byteBuffer.putFloat(this.fogDensity);
        byteBuffer.putFloat(this.ripples);
        byteBuffer.putFloat(this.snowCover);
        byteBuffer.putFloat(this.sway);
        byteBuffer.putInt(this.defaultType);
        byteBuffer.putInt(this.quality);
        byteBuffer.putFloat(0.0F);
        byteBuffer.putFloat(0.0F);
        byteBuffer.putFloat(0.0F);
    }
}
