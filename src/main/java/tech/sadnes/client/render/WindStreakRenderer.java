package tech.sadnes.client.render;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap.Type;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.util.color.ColorUtil;

public final class WindStreakRenderer {
    private static final int MAX_STREAKS = 56;
    private static final int SEGMENTS = 24;
    private static final float TWO_PI = (float) (Math.PI * 2);
    private static final double SPAWN_RADIUS = 22.0;
    private static final float FADE_DISTANCE = 26.0F;
    private static final float STREAK_LENGTH = 0.45F;
    private static final float ANGLE_SPREAD = 0.3F;
    private static final float MIN_WIDTH = 8.0F;
    private static final float WIDTH_SCALE = 0.011111111F;
    private static final float MAX_LINE_WIDTH = 16.0F;
    private static final float TRAVEL_SPEED = 4.0F;
    private final WindStreakRenderer.Streak[] streaks = new WindStreakRenderer.Streak[56];
    private final float[] points = new float[75];
    private final Random random = new Random();
    private final BufferAllocator allocator = new BufferAllocator(65536);

    public WindStreakRenderer() {
        for (int i = 0; i < 56; i++) {
            this.streaks[i] = new WindStreakRenderer.Streak(3.5 + this.random.nextDouble() * 2.5, this.random.nextDouble());
        }
    }

    public void render(ClientWorld clientWorld, Vec3d vec3d, double d, float f, float f2, float f3, float f4, int i) {
        int i2 = Math.round(56.0F * f4 * (0.4F + 0.6F * f3));
        if (i2 > 0 && !(f3 <= 0.0F)) {
            BufferBuilder bufferBuilder = new BufferBuilder(this.allocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);

            for (int i3 = 0; i3 < i2; i3++) {
                WindStreakRenderer.Streak streak = this.streaks[i3];
                double d2 = d / streak.period + streak.phase;
                long l = (long)Math.floor(d2);
                if (streak.cycle != l) {
                    this.respawn(streak, l, clientWorld, vec3d, f, f2);
                }

                if (streak.visible) {
                    float f5 = (float)(d2 - l);
                    float f6 = f3 * 4.0F * f5 * (float)streak.period;
                    this.renderStreak(bufferBuilder, streak, f5, f6, vec3d, f3, i);
                }
            }

            BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer != null) {
                float f7 = Math.min(MinecraftClient.getInstance().getWindow().getFramebufferHeight() * 0.011111111F, 16.0F);
                BuiltBuffer builtBuffer2 = builtBuffer;

                try {
                    ParticleBlurRenderer.INSTANCE.render(f7, 0.0F, () -> MeshDrawUtil.drawLabeled(Pipelines.WIND_STREAK, builtBuffer, "sadnes ambience wind"));
                } catch (Throwable throwable) {
                    if (builtBuffer2 != null) {
                        try {
                            builtBuffer2.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }

                    throw throwable;
                }

                if (builtBuffer != null) {
                    builtBuffer.close();
                }
            }
        }
    }

    private void respawn(WindStreakRenderer.Streak streak, long l, ClientWorld clientWorld, Vec3d vec3d, float f, float f2) {
        streak.cycle = l;
        double d = this.random.nextDouble() * Math.PI * 2.0;
        double d2 = Math.sqrt(this.random.nextDouble()) * 22.0;
        streak.x = vec3d.x + Math.cos(d) * d2;
        streak.z = vec3d.z + Math.sin(d) * d2;
        int i = MathHelper.floor(streak.x);
        int i2 = MathHelper.floor(streak.z);
        boolean flag = clientWorld.getChunkManager().isChunkLoaded(ChunkSectionPos.getSectionCoord(i), ChunkSectionPos.getSectionCoord(i2));
        int i3 = clientWorld.getTopY(Type.MOTION_BLOCKING, i, i2);
        streak.visible = flag && i3 <= vec3d.y + 6.0;
        streak.y = i3 + 0.4 + this.random.nextDouble() * 5.6;
        float f3 = (this.random.nextFloat() - 0.5F) * 0.3F;
        float f4 = MathHelper.cos(f3);
        float f5 = MathHelper.sin(f3);
        streak.dirX = f * f4 - f2 * f5;
        streak.dirZ = f * f5 + f2 * f4;
        streak.length = 8.0F + this.random.nextFloat() * 8.0F;
        streak.waveAmplitude = 0.1F + this.random.nextFloat() * 0.25F;
        streak.waveFrequency = 0.3F + this.random.nextFloat() * 0.4F;
        streak.wavePhase = this.random.nextFloat() * (float) (Math.PI * 2);
        streak.verticalAmplitude = 0.05F + this.random.nextFloat() * 0.15F;
        streak.alpha = 0.75F + this.random.nextFloat() * 0.25F;
    }

    private void renderStreak(BufferBuilder bufferBuilder, WindStreakRenderer.Streak streak, float f, float f2, Vec3d vec3d, float f3, int i) {
        float f4 = -0.05F + 1.55F * f - 0.45F;

        for (int i2 = 0; i2 <= 24; i2++) {
            this.computePoint(streak, f4 + 0.45F * i2 / 24.0F, f2, vec3d, i2);
        }

        float f5 = ColorUtil.alphaf(i) * streak.alpha * (0.5F + 0.5F * f3);
        float f6 = 0.0F;
        float f7 = 0.0F;

        for (int i3 = 0; i3 <= 24; i3++) {
            float f8 = f4 + 0.45F * i3 / 24.0F;
            float f9 = i3 / 24.0F;
            float f10 = smoothstep(0.0F, 0.6F, f9) * (1.0F - smoothstep(0.75F, 1.0F, f9));
            float f11 = smoothstep(0.0F, 0.08F, f8) * (1.0F - smoothstep(0.92F, 1.0F, f8));
            int i4 = i3 * 3;
            float f12 = MathHelper.sqrt(
                this.points[i4] * this.points[i4] + this.points[i4 + 1] * this.points[i4 + 1] + this.points[i4 + 2] * this.points[i4 + 2]
            );
            float f13 = smoothstep(1.0F, 2.5F, f12) * (1.0F - smoothstep(18.199999F, 26.0F, f12));
            float f14 = f5 * f10 * f11 * f13;
            float f15 = Math.max(0.3F * ProjectionUtil.getPixelsPerBlock(f12), 8.0F) * (0.5F + 0.5F * f10);
            if (i3 > 0 && (f14 > 0.002F || f6 > 0.002F)) {
                this.addSegment(bufferBuilder, i3 - 1, i3, f6, f14, f7, f15, i);
            }

            f6 = f14;
            f7 = f15;
        }
    }

    private void computePoint(WindStreakRenderer.Streak streak, float f, float f2, Vec3d vec3d, int i) {
        float f3 = (f - 0.5F) * streak.length + f2;
        float f4 = MathHelper.sin((float) (Math.PI * 2) * streak.waveFrequency * f + streak.wavePhase);
        float f5 = streak.waveAmplitude * f4;
        float f6 = streak.verticalAmplitude * f4;
        int i2 = i * 3;
        this.points[i2] = (float)(streak.x - vec3d.x) + streak.dirX * f3 + streak.dirZ * f5;
        this.points[i2 + 1] = (float)(streak.y - vec3d.y) + f6;
        this.points[i2 + 2] = (float)(streak.z - vec3d.z) + streak.dirZ * f3 - streak.dirX * f5;
    }

    private void addSegment(BufferBuilder bufferBuilder, int i, int i2, float f, float f2, float f3, float f4, int i3) {
        int i4 = i * 3;
        int i5 = i2 * 3;
        float f5 = this.points[i5] - this.points[i4];
        float f6 = this.points[i5 + 1] - this.points[i4 + 1];
        float f7 = this.points[i5 + 2] - this.points[i4 + 2];
        float f8 = MathHelper.sqrt(f5 * f5 + f6 * f6 + f7 * f7);
        if (!(f8 < 1.0E-5F)) {
            float f9 = f5 / f8;
            float f10 = f6 / f8;
            float f11 = f7 / f8;
            bufferBuilder.vertex(this.points[i4], this.points[i4 + 1], this.points[i4 + 2])
                .color(ColorUtil.setAlphaf(i3, f))
                .normal(f9, f10, f11)
                .lineWidth(f3);
            bufferBuilder.vertex(this.points[i5], this.points[i5 + 1], this.points[i5 + 2])
                .color(ColorUtil.setAlphaf(i3, f2))
                .normal(f9, f10, f11)
                .lineWidth(f4);
        }
    }

    private static float smoothstep(float f, float f2, float f3) {
        float f4 = MathHelper.clamp((f3 - f) / (f2 - f), 0.0F, 1.0F);
        return f4 * f4 * (3.0F - 2.0F * f4);
    }

    static final class Streak {
        final double period;
        final double phase;
        long cycle = Long.MIN_VALUE;
        boolean visible;
        double x;
        double y;
        double z;
        float dirX;
        float dirZ;
        float length;
        float waveAmplitude;
        float waveFrequency;
        float wavePhase;
        float verticalAmplitude;
        float alpha;

        Streak(double d, double d2) {
            this.period = d;
            this.phase = d2;
        }
    }
}
