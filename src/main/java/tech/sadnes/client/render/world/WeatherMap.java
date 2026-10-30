package tech.sadnes.client.render.world;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.biome.Biome.Precipitation;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.ResourceUtil;
import tech.sadnes.client.util.math.StopWatch;

final class WeatherMap {
    static final int SIZE = 96;
    static final int RAIN = 1;
    static final int SNOW = 2;
    private static final int HALF_SIZE = 48;
    private static final int HEIGHT_OFFSET = 32768;
    private static final int WATER_FLAG = 4;
    private static final int CELL_SIZE = 4;
    private static final int CELLS = 24;
    private static final int MOVE_THRESHOLD = 8;
    private static final long REFRESH_INTERVAL = 500L;
    private final StopWatch refreshTimer = new StopWatch();
    private final Mutable mutablePos = new Mutable();
    private final int[] cellPrecipitation = new int[576];
    private NativeImageBackedTexture texture;
    private RegistryKey<World> dimension;
    private Precipitation forcedPrecipitation;
    private int originX;
    private int originY;
    private int originZ;
    private boolean hasRain;
    private boolean hasSnow;
    private int centerPrecipitation;

    void update(ClientWorld clientWorld, Vec3d vec3d, Precipitation precipitation) {
        int i = MathHelper.floor(vec3d.x);
        int i2 = MathHelper.floor(vec3d.y);
        int i3 = MathHelper.floor(vec3d.z);
        boolean flag = Math.abs(i - this.originX - 48) > 8 || Math.abs(i3 - this.originZ - 48) > 8 || Math.abs(i2 - this.originY) > 8;
        if (this.texture == null || flag || clientWorld.getRegistryKey() != this.dimension || precipitation != this.forcedPrecipitation || this.refreshTimer.hasPassed(500.0)) {
            this.refreshTimer.reset();
            this.dimension = clientWorld.getRegistryKey();
            this.forcedPrecipitation = precipitation;
            this.originX = i - 48;
            this.originY = i2;
            this.originZ = i3 - 48;
            this.rebuild(clientWorld, precipitation);
        }
    }

    private void rebuild(ClientWorld clientWorld, Precipitation precipitation) {
        if (this.texture == null) {
            this.texture = new NativeImageBackedTexture(() -> ClientInfo.MOD_ID + "_weather_map", 96, 96, false);
        }

        int i = clientWorld.getSeaLevel();

        for (int i2 = 0; i2 < 24; i2++) {
            for (int i3 = 0; i3 < 24; i3++) {
                this.cellPrecipitation[i2 * 24 + i3] = this.samplePrecipitation(clientWorld, precipitation, i, this.originX + i3 * 4 + 2, this.originZ + i2 * 4 + 2);
            }
        }

        this.centerPrecipitation = this.cellPrecipitation[300];
        NativeImage nativeImage = this.texture.getImage();
        this.hasRain = false;
        this.hasSnow = false;

        for (int i4 = 0; i4 < 96; i4++) {
            for (int i5 = 0; i5 < 96; i5++) {
                int i6 = this.originX + i5;
                int i7 = this.originZ + i4;
                int i8 = this.cellPrecipitation[i4 / 4 * 24 + i5 / 4];
                int i9 = clientWorld.getTopY(Type.MOTION_BLOCKING, i6, i7);
                int i10;
                int i11;
                byte b;
                if (i8 != 0 && clientWorld.getFluidState(this.mutablePos.set(i6, i9 - 1, i7)).isIn(FluidTags.WATER)) {
                    i10 = i9 + 32768;
                    i11 = i8;
                    b = 4;
                } else {
                    i10 = i9 + 32768;
                    i11 = i8;
                    b = 0;
                }

                int i12 = i11 | b;
                nativeImage.setColorArgb(i5, i4, 0xFF000000 | (i10 >> 8 & 0xFF) << 16 | (i10 & 0xFF) << 8 | i12);
                this.hasRain |= i8 == 1;
                this.hasSnow |= i8 == 2;
            }
        }

        this.texture.upload();
    }

    private int samplePrecipitation(ClientWorld clientWorld, Precipitation precipitation, int i, int i2, int i3) {
        if (!clientWorld.getChunkManager().isChunkLoaded(ChunkSectionPos.getSectionCoord(i2), ChunkSectionPos.getSectionCoord(i3))) {
            return 0;
        }

        Precipitation precipitation2 = precipitation != null ? precipitation : clientWorld.getBiome(this.mutablePos.set(i2, this.originY, i3)).value().getPrecipitation(this.mutablePos, i);
        switch (precipitation2) {
            case RAIN:
                return 1;
            case SNOW:
                return 2;
            default:
                return 0;
        }
    }

    GpuTextureView getTextureView() {
        return this.texture == null ? null : ResourceUtil.getView(this.texture);
    }

    int getOriginX() {
        return this.originX;
    }

    int getOriginZ() {
        return this.originZ;
    }

    boolean hasRain() {
        return this.hasRain;
    }

    boolean hasSnow() {
        return this.hasSnow;
    }

    int getCenterPrecipitation() {
        return this.centerPrecipitation;
    }

    void close() {
        if (this.texture != null) {
            this.texture.close();
        }

        this.texture = null;
        this.dimension = null;
    }
}
