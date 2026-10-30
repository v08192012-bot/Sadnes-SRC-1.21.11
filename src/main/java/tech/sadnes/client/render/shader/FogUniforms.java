package tech.sadnes.client.render.shader;

import java.nio.ByteBuffer;
import net.minecraft.client.gl.DynamicUniformStorage.Uploadable;

public record FogUniforms(
    float layerDensity,
    float layerStart,
    float layerBaseY,
    float layerFalloff,
    float hazeShare,
    float hazeFalloff,
    float noiseShare,
    float marchDistance,
    float driftX,
    float driftY,
    float driftZ,
    float noiseCamX,
    float noiseCamY,
    float noiseCamZ,
    float sunShare,
    float ambientShare,
    float shaftShare,
    float mapOriginX,
    float mapOriginZ,
    float mapSize,
    float flashX,
    float flashY,
    float flashZ,
    float flashPower,
    float flashHeight,
    int volumeMode,
    int volumeQuality
) implements Uploadable {
    public static final int BLOCK_SIZE = 128;

    @Override
    public void write(ByteBuffer byteBuffer) {
        byteBuffer.putFloat(this.layerDensity);
        byteBuffer.putFloat(this.layerStart);
        byteBuffer.putFloat(this.layerBaseY);
        byteBuffer.putFloat(this.layerFalloff);
        byteBuffer.putFloat(this.hazeShare);
        byteBuffer.putFloat(this.hazeFalloff);
        byteBuffer.putFloat(this.noiseShare);
        byteBuffer.putFloat(this.marchDistance);
        byteBuffer.putFloat(this.driftX);
        byteBuffer.putFloat(this.driftY);
        byteBuffer.putFloat(this.driftZ);
        byteBuffer.putFloat(this.noiseCamX);
        byteBuffer.putFloat(this.noiseCamY);
        byteBuffer.putFloat(this.noiseCamZ);
        byteBuffer.putFloat(this.sunShare);
        byteBuffer.putFloat(this.ambientShare);
        byteBuffer.putFloat(this.shaftShare);
        byteBuffer.putFloat(this.mapOriginX);
        byteBuffer.putFloat(this.mapOriginZ);
        byteBuffer.putFloat(this.mapSize);
        byteBuffer.putFloat(this.flashX);
        byteBuffer.putFloat(this.flashY);
        byteBuffer.putFloat(this.flashZ);
        byteBuffer.putFloat(this.flashPower);
        byteBuffer.putFloat(this.flashHeight);
        byteBuffer.putInt(this.volumeMode);
        byteBuffer.putInt(this.volumeQuality);
        byteBuffer.putFloat(0.0F);
        byteBuffer.putFloat(0.0F);
        byteBuffer.putFloat(0.0F);
        byteBuffer.putFloat(0.0F);
        byteBuffer.putFloat(0.0F);
    }
}
