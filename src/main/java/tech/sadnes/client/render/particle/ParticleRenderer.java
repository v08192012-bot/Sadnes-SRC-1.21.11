package tech.sadnes.client.render.particle;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tech.sadnes.client.render.MeshDrawUtil;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.util.color.ColorUtil;

public final class ParticleRenderer {
    private static final int MAX_PARTICLES = 4096;
    private static final int BYTES_PER_PARTICLE = 96;
    private final List<WorldParticle> particles = new ArrayList<>();
    private final Map<Texture, List<WorldParticle>> particlesByTexture = new EnumMap<>(Texture.class);
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f corner1 = new Vector3f();
    private final Vector3f corner2 = new Vector3f();
    private final Vector3f corner3 = new Vector3f();
    private final Vector3f corner4 = new Vector3f();
    private float trailLength;
    private boolean trailOnly;
    public static final int TRAIL_SEGMENTS = 8;

    public static float getTrailFade(int i) {
        float f = 1.0F - i / 9.0F;
        return f * f * 0.75F;
    }

    public void addParticle(WorldParticle worldParticle) {
        if (this.particles.size() >= 4096) {
            this.particles.remove(0);
        }

        this.particles.add(worldParticle);
    }

    public void tick() {
        if (!this.particles.isEmpty()) {
            this.particles.removeIf(WorldParticle::isExpired);

            for (WorldParticle worldParticle : this.particles) {
                worldParticle.tick(this.particles);
            }
        }
    }

    public void clear() {
        this.particles.clear();
    }

    public boolean isEmpty() {
        return this.particles.isEmpty();
    }

    public void render(MatrixStack matrixStack, Camera camera, float f, boolean flag) {
        this.renderInternal(matrixStack, camera, f, flag, 0.0F, false);
    }

    public void renderTrails(MatrixStack matrixStack, Camera camera, float f, boolean flag, float f2) {
        if (f2 > 0.0F) {
            this.renderInternal(matrixStack, camera, f, flag, f2, true);
        }
    }

    private void renderInternal(MatrixStack matrixStack, Camera camera, float f, boolean flag, float f2, boolean flag2) {
        if (!this.particles.isEmpty()) {
            this.trailLength = f2;
            this.trailOnly = flag2;
            Iterator iterator2 = this.particlesByTexture.values().iterator();

            while (iterator2.hasNext()) {
                ((List)iterator2.next()).clear();
            }

            for (WorldParticle worldParticle : this.particles) {
                if (ProjectionUtil.isVisible(worldParticle.getLerpedX(f), worldParticle.getLerpedY(f), worldParticle.getLerpedZ(f))) {
                    this.particlesByTexture.computeIfAbsent(worldParticle.getTexture(), texture2 -> new ArrayList<>()).add(worldParticle);
                }
            }

            Vec3d vec3d = camera.getCameraPos();
            Quaternionf quaternionf = camera.getRotation();
            Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
            RenderPipeline renderPipeline = flag ? Pipelines.PARTICLE_BLOOM : Pipelines.PARTICLE_TEXTURE;

            for (Entry entry : this.particlesByTexture.entrySet()) {
                List list = (List)entry.getValue();
                if (!list.isEmpty()) {
                    this.renderBatch((Texture)entry.getKey(), list, matrix4f, quaternionf, vec3d, f, renderPipeline);
                }
            }
        }
    }

    public void renderSingle(
        MatrixStack matrixStack, Camera camera, Texture texture, double d, double d2, double d3, float f, float f2, int i, boolean flag
    ) {
        if (ProjectionUtil.isVisible(d, d2, d3)) {
            Vec3d vec3d = camera.getCameraPos();

            try (BufferAllocator bufferAllocator = new BufferAllocator(352)) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                this.putQuad(
                    bufferBuilder,
                    matrixStack.peek().getPositionMatrix(),
                    camera.getRotation(),
                    (float)(d - vec3d.x),
                    (float)(d2 - vec3d.y),
                    (float)(d3 - vec3d.z),
                    f,
                    f2,
                    i
                );
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer != null) {
                    RenderPipeline renderPipeline = flag ? Pipelines.PARTICLE_BLOOM : Pipelines.PARTICLE_TEXTURE;
                    MeshDrawUtil.drawLabeledTextured(renderPipeline, builtBuffer, "sadnes particles", texture.getIdentifier());
                }
            }
        }
    }

    private void renderBatch(Texture texture, List<WorldParticle> worldParticles, Matrix4f matrix4f, Quaternionf quaternionf, Vec3d vec3d, float f, RenderPipeline renderPipeline) {
        int i = this.getTrailSegmentCount();
        int i2 = this.trailOnly ? 1 : 0;

        try (BufferAllocator bufferAllocator = new BufferAllocator(worldParticles.size() * (i + 1) * 96 + 256)) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (WorldParticle worldParticle : worldParticles) {
                int i3 = worldParticle.getFadedColor(worldParticle.getProgress(f));
                if (ColorUtil.alpha(i3) > 1) {
                    float f2 = worldParticle.getSize();
                    if (worldParticle.shouldShrinkWithFade()) {
                        f2 *= ColorUtil.alphaf(i3);
                    }

                    if (!(f2 <= 0.001F)) {
                        for (int i4 = i; i4 >= i2; i4--) {
                            float f3 = i4 == 0 ? 1.0F : getTrailFade(i4);
                            int i5 = ColorUtil.multiplyAlpha(i3, f3);
                            if (ColorUtil.alpha(i5) > 1) {
                                float f4 = f - this.trailLength * i4 / 8.0F;
                                float f5 = f2 * (0.45F + 0.55F * f3);
                                float f6 = (float)(worldParticle.getLerpedX(f4) - vec3d.x);
                                float f7 = (float)(worldParticle.getLerpedY(f4) + f5 / 5.0F - vec3d.y);
                                float f8 = (float)(worldParticle.getLerpedZ(f4) - vec3d.z);
                                this.putQuad(bufferBuilder, matrix4f, quaternionf, f6, f7, f8, f5, worldParticle.getLerpedRotation(f4), i5);
                            }
                        }
                    }
                }
            }

            BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer != null) {
                MeshDrawUtil.drawLabeledTextured(renderPipeline, builtBuffer, "sadnes particles", texture.getIdentifier());
            }
        }
    }

    private int getTrailSegmentCount() {
        return this.trailLength > 0.0F ? 8 : 0;
    }

    private void putQuad(BufferBuilder bufferBuilder, Matrix4f matrix4f, Quaternionf quaternionf, float f, float f2, float f3, float f4, float f5, int i) {
        this.rotation.set(quaternionf).rotateZ((float)Math.toRadians(f5));
        float f6 = f4 / 2.0F;
        this.corner1.set(-f6, -f6, 0.0F).rotate(this.rotation).add(f, f2, f3);
        this.corner2.set(-f6, f6, 0.0F).rotate(this.rotation).add(f, f2, f3);
        this.corner3.set(f6, f6, 0.0F).rotate(this.rotation).add(f, f2, f3);
        this.corner4.set(f6, -f6, 0.0F).rotate(this.rotation).add(f, f2, f3);
        bufferBuilder.vertex(matrix4f, this.corner1.x, this.corner1.y, this.corner1.z).texture(0.0F, 1.0F).color(i);
        bufferBuilder.vertex(matrix4f, this.corner2.x, this.corner2.y, this.corner2.z).texture(0.0F, 0.0F).color(i);
        bufferBuilder.vertex(matrix4f, this.corner3.x, this.corner3.y, this.corner3.z).texture(1.0F, 0.0F).color(i);
        bufferBuilder.vertex(matrix4f, this.corner4.x, this.corner4.y, this.corner4.z).texture(1.0F, 1.0F).color(i);
    }
}
