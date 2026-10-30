package tech.sadnes.client.render.world;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap.Type;
import tech.sadnes.client.render.MeshDrawUtil;
import tech.sadnes.client.render.ParticleBlurRenderer;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.util.color.ColorUtil;

public enum AmbienceLightningRenderer {
    INSTANCE;

    private static final float TWO_PI = (float) (Math.PI * 2);
    private static final double MIN_DISTANCE = 36.0;
    private static final double MAX_DISTANCE = 220.0;
    private static final double SOUND_SPEED = 120.0;
    private static final int MAIN_BOLT_DEPTH = 7;
    private static final int BRANCH_DEPTH = 5;
    private static final float DISPLACEMENT = 0.16F;
    private static final float FLASH_RISE = 0.025F;
    private static final float FLASH_DECAY = 0.08F;
    static final int BOLT_COLOR = ColorUtil.rgb(150, 165, 255);
    private static final float MIN_FALLOFF = 0.35F;
    private final List<AmbienceLightningRenderer.Bolt> bolts = new ArrayList<>();
    private final Random random = new Random();
    private long nextStrikeTime;

    public void tick(ClientWorld clientWorld, float f) {
        long l = System.nanoTime();
        this.bolts.removeIf(bolt -> bolt.isExpired(l));
        Iterator iterator2 = this.bolts.iterator();

        while (iterator2.hasNext()) {
            ((AmbienceLightningRenderer.Bolt)iterator2.next()).tick(clientWorld, l);
        }

        if (f > 0.0F && clientWorld.getThunderGradient(1.0F) > 0.5F && clientWorld.getRainGradient(1.0F) > 0.2F) {
            if (this.nextStrikeTime == 0L) {
                this.nextStrikeTime = l + this.nextStrikeDelay(f);
            }

            if (l >= this.nextStrikeTime) {
                this.nextStrikeTime = l + this.nextStrikeDelay(f);
                AmbienceLightningRenderer.Bolt bolt2 = this.spawnBolt(clientWorld, l);
                bolt2.tick(clientWorld, l);
                this.bolts.add(bolt2);
            }
        } else {
            this.nextStrikeTime = 0L;
        }
    }

    public float getFlashIntensity(Vec3d vec3d) {
        if (this.bolts.isEmpty()) {
            return 0.0F;
        }

        long l = System.nanoTime();
        float f = 0.0F;

        for (AmbienceLightningRenderer.Bolt bolt : this.bolts) {
            float f2 = 1.0F - 0.65F * smoothstep(60.0F, 220.0F, bolt.distanceTo(vec3d));
            f = Math.max(f, bolt.getIntensity(bolt.getAge(l)) * f2);
        }

        return f;
    }

    public AmbienceLightningRenderer.LightningFlash getBrightestFlash(Vec3d vec3d) {
        if (this.bolts.isEmpty()) {
            return null;
        }

        long l = System.nanoTime();
        AmbienceLightningRenderer.Bolt bolt = null;
        float f = 0.003F;

        for (AmbienceLightningRenderer.Bolt bolt2 : this.bolts) {
            float f2 = bolt2.getIntensity(bolt2.getAge(l));
            if (!(f2 <= f)) {
                f = f2;
                bolt = bolt2;
            }
        }

        return bolt == null
            ? null
            : new AmbienceLightningRenderer.LightningFlash((float)(bolt.x - vec3d.x), (float)(bolt.y - vec3d.y), (float)(bolt.z - vec3d.z), bolt.height, f);
    }

    public void render(Vec3d vec3d, float f, float f2) {
        if (!this.bolts.isEmpty()) {
            long l = System.nanoTime();

            try (
                BufferAllocator bufferAllocator = new BufferAllocator(32768);
                BufferAllocator bufferAllocator2 = new BufferAllocator(32768);
                BufferAllocator bufferAllocator3 = new BufferAllocator(32768);
            ) {
                BufferBuilder bufferBuilder = createLineBuilder(bufferAllocator);
                BufferBuilder bufferBuilder2 = createLineBuilder(bufferAllocator2);
                BufferBuilder bufferBuilder3 = createLineBuilder(bufferAllocator3);
                float f3 = 0.0F;
                float f4 = 0.0F;

                for (AmbienceLightningRenderer.Bolt bolt : this.bolts) {
                    float f5 = bolt.getAge(l);
                    float f6 = bolt.distanceTo(vec3d);
                    float f7 = (float)Math.exp(-Math.min(Math.max(f6 - f, 0.0F) * f2, 3.0F));
                    float f8 = bolt.getIntensity(f5) * Math.max(f7, 0.35F);
                    if (!(f8 <= 0.003F)) {
                        float f9 = ProjectionUtil.getPixelsPerBlock(f6);
                        float f10 = MathHelper.clamp(f9 * 0.35F, 2.2F, 7.0F);
                        float f11 = MathHelper.clamp(f9 * 1.6F, 10.0F, 34.0F);
                        bolt.render(bufferBuilder, vec3d, f5, f8, f10 * 1.4F);
                        bolt.render(bufferBuilder2, vec3d, f5, f8 * 0.45F, f11);
                        bolt.render(bufferBuilder3, vec3d, f5, f8, f10);
                        f3 = Math.max(f3, MathHelper.clamp(f9 * 2.5F, 6.0F, 30.0F));
                        f4 = Math.max(f4, f8);
                    }
                }

                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer != null) {
                    ParticleBlurRenderer.INSTANCE
                        .render(
                            f3, Math.min(f4, 1.0F), () -> MeshDrawUtil.drawLabeled(Pipelines.LIGHTNING_DEPTH, builtBuffer, "sadnes ambience lightning bloom")
                        );
                }

                BuiltBuffer builtBuffer2 = bufferBuilder2.endNullable();
                if (builtBuffer2 != null) {
                    MeshDrawUtil.drawLabeled(Pipelines.LIGHTNING_DEPTH, builtBuffer2, "sadnes ambience lightning halo");
                }

                BuiltBuffer builtBuffer3 = bufferBuilder3.endNullable();
                if (builtBuffer3 != null) {
                    MeshDrawUtil.drawLabeled(Pipelines.LIGHTNING_DEPTH, builtBuffer3, "sadnes ambience lightning core");
                }
            }
        }
    }

    public boolean hasBolts() {
        return !this.bolts.isEmpty();
    }

    public void clear() {
        this.bolts.clear();
        this.nextStrikeTime = 0L;
    }

    private static BufferBuilder createLineBuilder(BufferAllocator bufferAllocator) {
        return new BufferBuilder(bufferAllocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
    }

    private long nextStrikeDelay(float f) {
        double d = MathHelper.lerp(f, 16.0, 2.2) * (0.35 + this.random.nextDouble() * 1.3);
        return (long)(d * 1.0E9);
    }

    private AmbienceLightningRenderer.Bolt spawnBolt(ClientWorld clientWorld, long l) {
        Vec3d vec3d = MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos();
        double d = this.random.nextDouble() * Math.PI * 2.0;
        double d2 = 36.0 + this.random.nextDouble() * 184.0;
        double d3 = vec3d.x + Math.cos(d) * d2;
        double d4 = vec3d.z + Math.sin(d) * d2;
        int i = MathHelper.floor(d3);
        int i2 = MathHelper.floor(d4);
        boolean flag = clientWorld.getChunkManager().isChunkLoaded(ChunkSectionPos.getSectionCoord(i), ChunkSectionPos.getSectionCoord(i2));
        double d5 = flag ? clientWorld.getTopY(Type.MOTION_BLOCKING, i, i2) : Math.min(vec3d.y, clientWorld.getSeaLevel());
        float f = 100.0F + this.random.nextFloat() * 40.0F;
        return new AmbienceLightningRenderer.Bolt(this.random, d3, d5, d4, f, l, (long)(d2 / 120.0 * 1.0E9));
    }

    static float smoothstep(float f, float f2, float f3) {
        float f4 = MathHelper.clamp((f3 - f) / (f2 - f), 0.0F, 1.0F);
        return f4 * f4 * (3.0F - 2.0F * f4);
    }

    static float[] generatePath(Random random2, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        int i2 = 1 << i;
        float[] floats = new float[(i2 + 1) * 3];
        floats[0] = f;
        floats[1] = f2;
        floats[2] = f3;
        floats[i2 * 3] = f4;
        floats[i2 * 3 + 1] = f5;
        floats[i2 * 3 + 2] = f6;
        subdivide(random2, floats, 0, i2);
        return floats;
    }

    private static void subdivide(Random random2, float[] floats, int i, int i2) {
        if (i2 - i >= 2) {
            int i3 = i * 3;
            int i4 = i2 * 3;
            float f = floats[i4] - floats[i3];
            float f2 = floats[i4 + 1] - floats[i3 + 1];
            float f3 = floats[i4 + 2] - floats[i3 + 2];
            float f4 = MathHelper.sqrt(f * f + f2 * f2 + f3 * f3);
            float f5 = MathHelper.sqrt(f * f + f3 * f3);
            float f6 = f5 > 1.0E-4F ? -f3 / f5 : 1.0F;
            float f7 = f5 > 1.0E-4F ? f / f5 : 0.0F;
            float f8 = f2 * f7 / f4;
            float f9 = (f3 * f6 - f * f7) / f4;
            float f10 = -f2 * f6 / f4;
            float f11 = (random2.nextFloat() * 2.0F - 1.0F) * f4 * 0.16F;
            float f12 = (random2.nextFloat() * 2.0F - 1.0F) * f4 * 0.16F;
            int i5 = i + i2 >>> 1;
            int i6 = i5 * 3;
            floats[i6] = floats[i3] + f * 0.5F + f6 * f11 + f8 * f12;
            floats[i6 + 1] = floats[i3 + 1] + f2 * 0.5F + f9 * f12;
            floats[i6 + 2] = floats[i3 + 2] + f3 * 0.5F + f7 * f11 + f10 * f12;
            subdivide(random2, floats, i, i5);
            subdivide(random2, floats, i5, i2);
        }
    }

    static final class Bolt {
        final double x;
        final double y;
        final double z;
        final float height;
        private final float[] mainPath;
        private final List<AmbienceLightningRenderer.Branch> branches = new ArrayList<>();
        private final float[] flashTimes;
        private final float[] flashStrengths;
        private final float duration;
        private final float thunderPitch;
        private final long startTime;
        private final long thunderTime;
        private int flashIndex;
        private boolean thunderPlayed;

        Bolt(Random random2, double d, double d2, double d3, float f, long l, long l2) {
            this.x = d;
            this.y = d2;
            this.z = d3;
            this.height = f;
            this.startTime = l;
            this.thunderTime = l + l2;
            this.thunderPitch = 0.8F + random2.nextFloat() * 0.2F;
            float f2 = (random2.nextFloat() - 0.5F) * 0.3F * f;
            float f3 = (random2.nextFloat() - 0.5F) * 0.3F * f;
            this.mainPath = AmbienceLightningRenderer.generatePath(random2, f2, f, f3, 0.0F, 0.0F, 0.0F, 7);
            this.generateBranches(random2);
            int i = 2 + random2.nextInt(3);
            this.flashTimes = new float[i];
            this.flashStrengths = new float[i];
            this.flashStrengths[0] = 1.0F;

            for (int i2 = 1; i2 < i; i2++) {
                this.flashTimes[i2] = this.flashTimes[i2 - 1] + 0.08F + random2.nextFloat() * 0.22F;
                this.flashStrengths[i2] = 0.55F + random2.nextFloat() * 0.4F;
            }

            this.duration = this.flashTimes[i - 1] + 0.55F;
        }

        private void generateBranches(Random random2) {
            int i = 3 + random2.nextInt(4);

            for (int i2 = 0; i2 < i; i2++) {
                int i3 = (int)(128 * (0.05F + random2.nextFloat() * 0.6F)) * 3;
                float[] floats = generateBranchPath(
                    random2,
                    this.mainPath[i3],
                    this.mainPath[i3 + 1],
                    this.mainPath[i3 + 2],
                    this.mainPath[i3 + 1] * (0.2F + random2.nextFloat() * 0.35F),
                    random2.nextFloat() * (float) (Math.PI * 2),
                    5
                );
                this.branches.add(new AmbienceLightningRenderer.Branch(floats, 0.55F + random2.nextFloat() * 0.25F));
                if (random2.nextBoolean()) {
                    int i4 = (int)(32.0F * (0.25F + random2.nextFloat() * 0.5F)) * 3;
                    float[] floats2 = generateBranchPath(
                        random2,
                        floats[i4],
                        floats[i4 + 1],
                        floats[i4 + 2],
                        this.mainPath[i3 + 1] * (0.08F + random2.nextFloat() * 0.12F),
                        random2.nextFloat() * (float) (Math.PI * 2),
                        4
                    );
                    this.branches.add(new AmbienceLightningRenderer.Branch(floats2, 0.35F + random2.nextFloat() * 0.15F));
                }
            }
        }

        private static float[] generateBranchPath(Random random2, float f, float f2, float f3, float f4, float f5, int i) {
            float f6 = 0.4F + random2.nextFloat() * 0.8F;
            float f7 = MathHelper.cos(f5) * f6;
            float f8 = MathHelper.sin(f5) * f6;
            float f9 = MathHelper.sqrt(f7 * f7 + 1.0F + f8 * f8);
            float f10 = f4 / f9;
            return AmbienceLightningRenderer.generatePath(random2, f, f2, f3, f + f7 * f10, f2 - f10, f3 + f8 * f10, i);
        }

        void tick(ClientWorld clientWorld, long l) {
            float f = this.getAge(l);

            while (this.flashIndex < this.flashTimes.length && f >= this.flashTimes[this.flashIndex]) {
                this.flashIndex++;
                clientWorld.setLightningTicksLeft(2);
            }

            if (!this.thunderPlayed && l >= this.thunderTime) {
                this.thunderPlayed = true;
                clientWorld.playSoundClient(
                    this.x, this.y, this.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 10000.0F, this.thunderPitch, false
                );
            }
        }

        boolean isExpired(long l) {
            return this.thunderPlayed && this.getAge(l) > this.duration;
        }

        float getAge(long l) {
            return (float)(l - this.startTime) / 1.0E9F;
        }

        float distanceTo(Vec3d vec3d) {
            double d = this.x - vec3d.x;
            double d2 = this.y + this.height * 0.4 - vec3d.y;
            double d3 = this.z - vec3d.z;
            return (float)Math.sqrt(d * d + d2 * d2 + d3 * d3);
        }

        float getIntensity(float f) {
            if (!(f < 0.0F) && !(f > this.duration)) {
                float f2 = 0.2F * (float)Math.exp(-f / 0.5F);
                int i = this.flashTimes.length;

                for (int i2 = 0; i2 < i; i2++) {
                    float f3 = f - this.flashTimes[i2];
                    if (f3 < 0.0F) {
                        break;
                    }

                    float f4 = f3 < 0.025F ? f3 / 0.025F : (float)Math.exp(-(f3 - 0.025F) / 0.08F);
                    f2 = Math.max(f2, f4 * this.flashStrengths[i2]);
                }

                return f2 * (1.0F - AmbienceLightningRenderer.smoothstep(this.duration - 0.25F, this.duration, f));
            } else {
                return 0.0F;
            }
        }

        void render(BufferBuilder bufferBuilder, Vec3d vec3d, float f, float f2, float f3) {
            float f4 = (float)(this.x - vec3d.x);
            float f5 = (float)(this.y - vec3d.y);
            float f6 = (float)(this.z - vec3d.z);
            renderPath(bufferBuilder, this.mainPath, f4, f5, f6, f2 * 0.45F, f2, f3);
            float f7 = f2 * (0.35F + 0.65F * (float)Math.exp(-f / 0.3F));

            for (AmbienceLightningRenderer.Branch branch : this.branches) {
                renderPath(bufferBuilder, branch.points(), f4, f5, f6, f7 * branch.alpha(), f7 * branch.alpha() * 0.1F, f3 * 0.6F);
            }
        }

        private static void renderPath(BufferBuilder bufferBuilder, float[] floats, float f, float f2, float f3, float f4, float f5, float f6) {
            int i = floats.length / 3 - 1;

            for (int i2 = 0; i2 < i; i2++) {
                int i3 = i2 * 3;
                int i4 = i3 + 3;
                float f7 = floats[i4] - floats[i3];
                float f8 = floats[i4 + 1] - floats[i3 + 1];
                float f9 = floats[i4 + 2] - floats[i3 + 2];
                float f10 = MathHelper.sqrt(f7 * f7 + f8 * f8 + f9 * f9);
                if (!(f10 < 1.0E-5F)) {
                    float f11 = f7 / f10;
                    float f12 = f8 / f10;
                    float f13 = f9 / f10;
                    int i5 = ColorUtil.setAlphaf(AmbienceLightningRenderer.BOLT_COLOR, MathHelper.lerp((float)i2 / i, f4, f5));
                    int i6 = ColorUtil.setAlphaf(AmbienceLightningRenderer.BOLT_COLOR, MathHelper.lerp((float)(i2 + 1) / i, f4, f5));
                    bufferBuilder.vertex(f + floats[i3], f2 + floats[i3 + 1], f3 + floats[i3 + 2]).color(i5).normal(f11, f12, f13).lineWidth(f6);
                    bufferBuilder.vertex(f + floats[i4], f2 + floats[i4 + 1], f3 + floats[i4 + 2]).color(i6).normal(f11, f12, f13).lineWidth(f6);
                }
            }
        }
    }

    record Branch(float[] points, float alpha) {
    }

    public record LightningFlash(float x, float y, float z, float height, float power) {
    }
}
