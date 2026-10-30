package tech.sadnes.client.feature.impl.combat.aura;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.feature.impl.combat.AimAssist;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.AutoCrystal;
import tech.sadnes.client.feature.impl.combat.AutoMace;
import tech.sadnes.client.render.MeshDrawUtil;
import tech.sadnes.client.render.ParticleBlurRenderer;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.ShapePointData;
import tech.sadnes.client.render.shader.EntityWarpRenderer;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.script.ScriptAuraHooks;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.RaycastUtil;

public final class TargetEspRenderer extends FeatureComponent {
    private final Animation alphaAnimation = new Animation();
    private final Animation sizeAnimation = new Animation();
    private LivingEntity target;
    private float circleTime;
    private long circleLastNanos;
    private final Animation oldCircleHeightAnimation = new Animation();
    private final Animation oldCircleBandAnimation = new Animation();
    private float oldCircleBandTarget;
    private float ghostsTime;
    private long ghostsLastNanos;
    private float crystalTime;
    private long crystalLastNanos;
    private final int[] crystalAlphaRamp = new int[65];
    private final Vector3f tempNormal = new Vector3f();
    private float lightningTime;
    private long lightningLastNanos;
    private float infinityTime;
    private long infinityLastNanos;
    private final float[] cornerX = new float[8];
    private final float[] cornerY = new float[8];
    private final float[] cornerRadius = new float[8];
    private final float[] pathX = new float[256];
    private final float[] pathY = new float[256];
    private final float[] pathNormalX = new float[256];
    private final float[] pathNormalY = new float[256];
    private final float[] pathIntensity = new float[256];
    private final int[] cornerPathStart = new int[8];
    private final float[] segmentDirX = new float[8];
    private final float[] segmentDirY = new float[8];
    private final int[] segmentCount = new int[8];
    private int infinityPointCount;
    private final int[] tubeAlphaRamp = new int[65];
    private final Quaternionf billboardRotation = new Quaternionf();
    private final Vector3f billboardRight = new Vector3f();
    private final Vector3f billboardUp = new Vector3f();
    private final Quaternionf quadRotation = new Quaternionf();
    private final Vector3f quadCorner0 = new Vector3f();
    private final Vector3f quadCorner1 = new Vector3f();
    private final Vector3f quadCorner2 = new Vector3f();
    private final Vector3f quadCorner3 = new Vector3f();
    private TargetEspRenderer.SmokePuff[] smokePuffs;
    private int smokePuffCount;
    private int smokeSpawnCounter;
    private float smokeSpawnAccumulator;
    private int smokeTargetId = -1;
    private final float[] shatterPathX = new float[256];
    private final float[] shatterPathY = new float[256];
    private final float[] shatterNormalX = new float[256];
    private final float[] shatterNormalY = new float[256];
    private final TargetEspRenderer.Shard[] shards = new TargetEspRenderer.Shard[]{new TargetEspRenderer.Shard(), new TargetEspRenderer.Shard()};
    private int shatteredTargetId = -1;
    private float shatterTime = 99.0F;
    private long shatterLastNanos;
    private int shatterColor;
    private double shatterX;
    private double shatterY;
    private double shatterZ;
    private double shatterGroundY;
    private final Vector3f shatterRight = new Vector3f();
    private float sunlightTime;
    private long sunlightLastNanos;
    private TargetEspRenderer.Ember[] embers;
    private int emberCount;
    private int emberSpawnCounter;
    private float emberSpawnAccumulator;
    private int emberTargetId = -1;
    private final Animation sunlightAnimation = new Animation();
    private double sunlightX;
    private double sunlightY;
    private double sunlightZ;
    private float sunlightScale;
    private int sunlightColor = -1;
    private float pulseStrength;
    private int lastTargetHurtTime;
    private int pulseTargetId = -1;
    private int lastPlayerHurtTime;
    private final DischargeEffect dischargeRenderer = new DischargeEffect();
    private static final float CRYSTAL_BEVEL = 0.11F;
    private static final int SUBDIVISIONS = 2;
    private static final int CORNER_SUBDIVISIONS = 1;
    private static final float CRYSTAL_HEIGHT = 1.5F;
    private static final Vector3f LIGHT_DIRECTION = new Vector3f(0.42F, 0.86F, 0.29F).normalize();
    private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final float[] CRYSTAL_MESH = buildCrystalMesh();

    @EventHandler(priority = -500)
    public void onRender3D(RenderEvent renderEvent) {
        if (renderEvent.isWorld()) {
            AttackAura attackAura = AttackAura.getInstance();
            if (attackAura != null) {
                LivingEntity livingEntity = this.findTarget();
                if (livingEntity != null) {
                    this.target = livingEntity;
                }

                this.alphaAnimation.update();
                this.sizeAnimation.update();
                this.alphaAnimation.animate(livingEntity == null ? 0.0 : 255.0, 0.15F, Easings.LINEAR);
                this.sizeAnimation
                    .animate(livingEntity != null && this.target != null ? 0.7 - Math.sin(this.target.hurtTime * 0.03490658503988659) : 1.4, 0.3F, Easings.SINE_OUT);
                String string = attackAura.getActiveEspStyle();
                boolean flag = string.equalsIgnoreCase("infinity");
                boolean flag2 = string.equalsIgnoreCase("sunlight");
                boolean flag3 = string.equalsIgnoreCase("discharge");
                this.sunlightAnimation.update();
                if (flag2) {
                    this.sunlightAnimation.animate(livingEntity == null ? 0.0 : 1.0, 0.5, Easings.LINEAR);
                } else {
                    this.sunlightAnimation.forceValue(0.0);
                }

                if (flag && this.target != null && this.target.isDead() && this.shatteredTargetId != this.target.getId() && this.infinityPointCount > 0) {
                    this.startShatter(renderEvent, attackAura);
                }

                if (this.alphaAnimation.get() <= 0.0F) {
                    this.target = null;
                }

                if (flag && this.shatterTime < 1.8F) {
                    this.renderShatter(renderEvent);
                }

                if (flag2) {
                    this.renderSunlight(renderEvent, attackAura);
                }

                if (flag3) {
                    this.dischargeRenderer.render(renderEvent, attackAura, livingEntity);
                } else {
                    this.dischargeRenderer.reset();
                }

                if (this.target != null && !string.isEmpty()) {
                    if (ScriptAuraHooks.hasEspMode(string)) {
                        ScriptAuraHooks.renderEsp(
                            string, this.target, renderEvent.getMatrices(), renderEvent.getTickDelta(), attackAura.getEspColorValue(), this.alphaAnimation.get() / 255.0F
                        );
                    } else {
                        if (string.equalsIgnoreCase("diamond")) {
                            this.renderDiamond(renderEvent, attackAura);
                        }

                        if (string.equalsIgnoreCase("circle")) {
                            this.renderCircle(renderEvent, attackAura);
                        }

                        if (string.equalsIgnoreCase("old_circle")) {
                            this.renderOldCircle(renderEvent, attackAura);
                        }

                        if (string.equalsIgnoreCase("ghosts")) {
                            this.renderGhosts(renderEvent, attackAura);
                        }

                        if (string.equalsIgnoreCase("crystal")) {
                            this.renderCrystal(renderEvent, attackAura);
                        }

                        if (string.equalsIgnoreCase("lightning")) {
                            this.renderLightning(renderEvent, attackAura);
                        }

                        if (string.equalsIgnoreCase("infinity")) {
                            this.renderInfinity(renderEvent, attackAura);
                        }
                    }
                }
            }
        }
    }

    private LivingEntity findTarget() {
        AttackAura attackAura = AttackAura.getInstance();
        AimAssist aimAssist = AimAssist.getInstance();
        AutoMace autoMace = AutoMace.getInstance();
        AutoCrystal autoCrystal = AutoCrystal.getInstance();
        LivingEntity livingEntity = attackAura.enabled() ? attackAura.getCurrentTarget() : null;
        LivingEntity livingEntity2 = aimAssist.enabled() ? aimAssist.getTarget() : null;
        LivingEntity livingEntity3 = autoMace.enabled() ? autoMace.getTarget() : null;
        LivingEntity livingEntity4 = autoCrystal.getActiveTarget();
        if (livingEntity != null) {
            return livingEntity;
        }

        if (livingEntity3 != null && autoMace.isActive()) {
            return livingEntity3;
        }

        if (livingEntity4 != null) {
            return livingEntity4;
        }

        LivingEntity livingEntity5 = ScriptAuraHooks.getTarget();
        return livingEntity5 != null ? livingEntity5 : livingEntity2;
    }

    private void renderDiamond(RenderEvent renderEvent, AttackAura attackAura) {
        if (!(this.alphaAnimation.get() <= 2.0F)) {
            float f = renderEvent.getTickDelta();
            double d = MathHelper.lerp(f, this.target.lastX, this.target.getX());
            double d2 = MathHelper.lerp(f, this.target.lastY, this.target.getY()) + this.target.getHeight() / 1.8F;
            double d3 = MathHelper.lerp(f, this.target.lastZ, this.target.getZ());
            Vec3d vec3d = renderEvent.getCamera().getCameraPos();
            float f2 = this.alphaAnimation.get() / 255.0F;
            float f3 = (float)Math.sin(this.target.hurtTime * (Math.PI / 10));
            float f4 = (float)Easings.BACK_OUT.ease(f3);
            int[] ints = attackAura.getEspFadeColors();
            int i = ColorUtil.interpolate(ColorUtil.multiplyAlpha(attackAura.getEspColorValue(), f2), ColorUtil.rgbWithAlphaf(255, 0, 0, f2), f4 / 2.0F);
            float f5 = this.sizeAnimation.get() * 2.0F;
            float f6 = (float)(System.currentTimeMillis() / 8.0 % 360.0);
            float f7 = (float)(d - vec3d.x);
            float f8 = (float)(d2 - vec3d.y);
            float f9 = (float)(d3 - vec3d.z);
            if (attackAura.getEspColorMode().is("fade")) {
                this.drawGradientQuad(
                    renderEvent.getMatrices(),
                    Texture.TARGET.getIdentifier(),
                    Pipelines.AURA_BLOOM,
                    renderEvent.getCamera().getRotation(),
                    f7,
                    f8,
                    f9,
                    f5,
                    f6,
                    ColorUtil.multiplyAlpha(ints[0], f2),
                    ColorUtil.multiplyAlpha(ints[1], f2),
                    ColorUtil.multiplyAlpha(ints[2], f2),
                    ColorUtil.multiplyAlpha(ints[3], f2)
                );
            } else {
                this.drawTexturedQuad(
                    renderEvent.getMatrices(),
                    Texture.TARGET.getIdentifier(),
                    Pipelines.AURA_BLOOM,
                    renderEvent.getCamera().getRotation(),
                    f7,
                    f8,
                    f9,
                    f5,
                    f6,
                    i
                );
            }

            if (attackAura.getEspColorMode().is("fade")) {
                this.drawGradientQuad(
                    renderEvent.getMatrices(),
                    Texture.TARGET.getIdentifier(),
                    Pipelines.AURA_BLOOM,
                    renderEvent.getCamera().getRotation(),
                    f7,
                    f8,
                    f9,
                    f5,
                    f6,
                    ColorUtil.multiplyAlpha(ints[0], f2),
                    ColorUtil.multiplyAlpha(ints[1], f2),
                    ColorUtil.multiplyAlpha(ints[2], f2),
                    ColorUtil.multiplyAlpha(ints[3], f2)
                );
            } else {
                this.drawTexturedQuad(
                    renderEvent.getMatrices(),
                    Texture.TARGET.getIdentifier(),
                    Pipelines.AURA_BLOOM,
                    renderEvent.getCamera().getRotation(),
                    f7,
                    f8,
                    f9,
                    f5,
                    f6,
                    i
                );
            }
        }
    }

    private void renderBloomLayers(RenderEvent renderEvent, float f, float f2, float f3, float f4, float f5, int i) {
        float f6 = (float)System.nanoTime() / 1.0E9F;

        for (int i2 = 0; i2 < 10; i2++) {
            float f7 = 1.0F + MathHelper.sin((f6 + i2 * 0.13F) * 2.4F) * 0.08F;
            float f8 = f4 * (1.25F + i2 * 0.08F) * f7;
            int i3 = ColorUtil.multiplyAlpha(i, 0.28F - i2 * 0.018F);
            this.drawTexturedQuad(
                renderEvent.getMatrices(),
                Texture.BLOOM.getIdentifier(),
                Pipelines.AURA_BLOOM,
                renderEvent.getCamera().getRotation(),
                f,
                f2,
                f3,
                f8,
                f5 + i2 * 36.0F,
                i3
            );
        }
    }

    private void renderCircle(RenderEvent renderEvent, AttackAura attackAura) {
        if (!(this.alphaAnimation.get() <= 2.0F)) {
            long l = System.nanoTime();
            float f = this.circleLastNanos == 0L ? 0.016666668F : Math.min((float)(l - this.circleLastNanos) / 1.0E9F, 0.05F);
            this.circleLastNanos = l;
            this.circleTime += f;
            float f2 = this.circleTime * 2.4F;
            float f3 = 0.5F + 0.5F * (float)Math.sin(f2);
            float f4 = -((float)Math.cos(f2));
            float f5 = this.circleTime * 1.6F;
            float f6 = renderEvent.getTickDelta();
            Vec3d vec3d = renderEvent.getCamera().getCameraPos();
            double d = MathHelper.lerp(f6, this.target.lastX, this.target.getX());
            double d2 = MathHelper.lerp(f6, this.target.lastZ, this.target.getZ());
            double d3 = MathHelper.lerp(f6, this.target.lastY, this.target.getY()) + (this.target.getHeight() + 0.1F) * f3;
            float f7 = this.alphaAnimation.get() / 255.0F;
            float f8 = this.target.getWidth() * 1.1F + 1.0F * (1.0F - f7);
            float f9 = Math.max(0.0F, this.target.hurtTime - f6);
            float f10 = (float)Math.sin(f9 * (Math.PI / 10)) * 0.5F;
            int i = ColorUtil.interpolate(ColorUtil.multiplyAlpha(attackAura.getEspColorValue(), f7), ColorUtil.rgbWithAlphaf(255, 0, 0, f7), f10);
            ArrayList arrayList = new ArrayList();

            for (byte b = 0; b < 360; b += 6) {
                double d4 = Math.toRadians(b);
                arrayList.add(new Vec3d(d + Math.cos(d4) * f8, d3, d2 + Math.sin(d4) * f8));
            }

            float f11 = (float)(d - vec3d.x);
            float f12 = (float)(d3 - vec3d.y);
            float f13 = (float)(d2 - vec3d.z);
            float f14 = 0.03F + 0.06F * (1.0F - f7);
            float f15 = ProjectionUtil.getScaledSize(f14, Math.sqrt(f11 * f11 + f12 * f12 + f13 * f13));

            try (
                BufferAllocator bufferAllocator = new BufferAllocator(8192);
                BufferAllocator bufferAllocator2 = new BufferAllocator(16384);
            ) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
                Render3DUtil.emitLines(bufferBuilder, renderEvent.getMatrices().peek(), arrayList, i, 6.0F);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                this.buildCircleSkirt(bufferBuilder2, renderEvent.getMatrices().peek().getPositionMatrix(), f11, f12, f13, f8, i, f7, f4, f5);
                BuiltBuffer builtBuffer2 = bufferBuilder2.endNullable();
                if (builtBuffer != null || builtBuffer2 != null) {
                    ParticleBlurRenderer.INSTANCE.render(f15, f7, () -> {
                        if (builtBuffer != null) {
                            MeshDrawUtil.drawLabeled(Pipelines.AA_LINE, builtBuffer, "sadnes aura ring glow");
                        }

                        if (builtBuffer2 != null) {
                            MeshDrawUtil.drawLabeled(Pipelines.AURA_COLOR_BLOOM, builtBuffer2, "sadnes aura skirt glow");
                        }
                    });
                }
            }
        }
    }

    private void buildCircleSkirt(
        BufferBuilder bufferBuilder, Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, float f5, float f6, float f7
    ) {
        int i2 = ColorUtil.multiplyAlpha(i, 0.28F * f5);
        int i3 = ColorUtil.multiplyAlpha(i, 0.03F * f5);

        for (byte b = 0; b < 360; b += 3) {
            double d = Math.toRadians(b);
            double d2 = Math.toRadians(b + 3);
            float f8 = f + (float)Math.cos(d) * f4;
            float f9 = f3 + (float)Math.sin(d) * f4;
            float f10 = f + (float)Math.cos(d2) * f4;
            float f11 = f3 + (float)Math.sin(d2) * f4;
            float f12 = (0.45F + 0.12F * (float)Math.sin(Math.toRadians(b) * 3.0 - f7)) * f6;
            float f13 = (0.45F + 0.12F * (float)Math.sin(Math.toRadians(b + 3) * 3.0 - f7)) * f6;
            bufferBuilder.vertex(matrix4f, f8, f2, f9).color(i2);
            bufferBuilder.vertex(matrix4f, f10, f2, f11).color(i2);
            bufferBuilder.vertex(matrix4f, f10, f2 + f13, f11).color(i3);
            bufferBuilder.vertex(matrix4f, f8, f2 + f12, f9).color(i3);
        }
    }

    private void renderOldCircle(RenderEvent renderEvent, AttackAura attackAura) {
        this.oldCircleHeightAnimation.update();
        this.oldCircleBandAnimation.update();
        if (!(this.alphaAnimation.get() <= 2.0F)) {
            if (this.oldCircleHeightAnimation.get() == 0.0F && this.oldCircleHeightAnimation.isFinished()) {
                this.oldCircleHeightAnimation.animate(1.0, 0.67F, Easings.SINE_OUT);
            } else if (this.oldCircleHeightAnimation.get() == 1.0F && this.oldCircleHeightAnimation.isFinished()) {
                this.oldCircleHeightAnimation.animate(0.0, 0.67F, Easings.SINE_OUT);
            }

            if (this.oldCircleHeightAnimation.get() > 0.9F) {
                this.oldCircleBandTarget = 0.5F;
            } else if (this.oldCircleHeightAnimation.get() < 0.1F) {
                this.oldCircleBandTarget = -0.5F;
            }

            this.oldCircleBandAnimation.animate(this.oldCircleBandTarget, 0.35F, Easings.SINE_OUT);
            float f = renderEvent.getTickDelta();
            Vec3d vec3d = renderEvent.getCamera().getCameraPos();
            double d = MathHelper.lerp(f, this.target.lastX, this.target.getX());
            double d2 = MathHelper.lerp(f, this.target.lastY, this.target.getY());
            double d3 = MathHelper.lerp(f, this.target.lastZ, this.target.getZ());
            float f2 = this.oldCircleHeightAnimation.get();
            float f3 = this.oldCircleBandAnimation.get();
            float f4 = this.alphaAnimation.get() / 255.0F;
            float f5 = this.target.getHeight() + 0.1F;
            float f6 = this.target.getWidth() * 1.1F;
            int i = attackAura.getEspColorValue();
            int i2 = ColorUtil.multiplyAlpha(i, 0.28431374F * f4);
            int i3 = ColorUtil.multiplyAlpha(i, 0.01F * f4);
            int i4 = ColorUtil.multiplyAlpha(i, f4);
            float f7 = (float)(d - vec3d.x);
            float f8 = (float)(d2 - vec3d.y + f5 * f2);
            float f9 = (float)(d3 - vec3d.z);
            Matrix4f matrix4f = renderEvent.getMatrices().peek().getPositionMatrix();

            try (
                BufferAllocator bufferAllocator = new BufferAllocator(32768);
                BufferAllocator bufferAllocator2 = new BufferAllocator(8192);
            ) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                this.buildOldCircleBand(bufferBuilder, matrix4f, f7, f8, f9, f6, f3, i2, i3);
                this.buildOldCircleBand(bufferBuilder, matrix4f, f7, f8, f9, f6, f3, i2, i3);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer != null) {
                    MeshDrawUtil.drawLabeled(Pipelines.AURA_COLOR_BLOOM, builtBuffer, "sadnes aura old circle band");
                }

                ArrayList arrayList = new ArrayList();

                for (int i5 = 0; i5 <= 360; i5++) {
                    double d4 = Math.toRadians(i5);
                    arrayList.add(new Vec3d(d + Math.cos(d4) * f6, d2 + f5 * f2, d3 + Math.sin(d4) * f6));
                }

                BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
                Render3DUtil.emitLines(bufferBuilder2, renderEvent.getMatrices().peek(), arrayList, i4, 2.0F);
                BuiltBuffer builtBuffer2 = bufferBuilder2.endNullable();
                if (builtBuffer2 != null) {
                    MeshDrawUtil.drawLabeled(Pipelines.AA_LINE, builtBuffer2, "sadnes aura old circle ring");
                }
            }
        }
    }

    private void buildOldCircleBand(BufferBuilder bufferBuilder, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2) {
        for (int i3 = 0; i3 < 360; i3++) {
            double d = Math.toRadians(i3);
            double d2 = Math.toRadians(i3 + 1);
            float f6 = f + (float)Math.cos(d) * f4;
            float f7 = f3 + (float)Math.sin(d) * f4;
            float f8 = f + (float)Math.cos(d2) * f4;
            float f9 = f3 + (float)Math.sin(d2) * f4;
            float f10 = ((float)(Math.cos(i3 * 0.7F) * Math.cos(i3 * 0.2F)) * 0.1F + 1.0F) * f5;
            float f11 = ((float)(Math.cos((i3 + 1) * 0.7F) * Math.cos((i3 + 1) * 0.2F)) * 0.1F + 1.0F) * f5;
            bufferBuilder.vertex(matrix4f, f6, f2, f7).color(i);
            bufferBuilder.vertex(matrix4f, f8, f2, f9).color(i);
            bufferBuilder.vertex(matrix4f, f8, f2 + f11, f9).color(i2);
            bufferBuilder.vertex(matrix4f, f6, f2 + f10, f7).color(i2);
        }
    }

    private void renderGhosts(RenderEvent renderEvent, AttackAura attackAura) {
        if (!(this.alphaAnimation.get() < 0.1F)) {
            long l = System.nanoTime();
            float f = this.ghostsLastNanos == 0L ? 0.016666668F : Math.min((float)(l - this.ghostsLastNanos) / 1.0E9F, 0.05F);
            this.ghostsLastNanos = l;
            this.ghostsTime += f * 125.0F;
            float f2 = renderEvent.getTickDelta();
            Vec3d vec3d = renderEvent.getCamera().getCameraPos();
            double d = MathHelper.lerp(f2, this.target.lastX, this.target.getX()) - vec3d.x;
            double d2 = MathHelper.lerp(f2, this.target.lastY, this.target.getY()) - vec3d.y;
            double d3 = MathHelper.lerp(f2, this.target.lastZ, this.target.getZ()) - vec3d.z;
            float f3 = this.alphaAnimation.get() / 255.0F;
            float f4 = (float)Math.sin(this.target.hurtTime * (Math.PI / 10));
            int i = ColorUtil.interpolate(ColorUtil.multiplyAlpha(attackAura.getEspColorValue(), f3), ColorUtil.rgbWithAlphaf(255, 0, 0, f3), f4 / 3.0F);
            Quaternionf quaternionf = renderEvent.getCamera().getRotation();

            try (
                BufferAllocator bufferAllocator = new BufferAllocator(4096);
                BufferAllocator bufferAllocator2 = new BufferAllocator(4096);
            ) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                this.buildGhosts(bufferBuilder, renderEvent.getMatrices(), quaternionf, d, d2, d3, ColorUtil.setAlpha(i, 255), 2.2F);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer != null) {
                    float f5 = ProjectionUtil.getScaledSize(0.06F, Math.sqrt(d * d + d2 * d2 + d3 * d3));
                    EntityWarpRenderer.INSTANCE
                        .render(
                            f3 * 0.03F,
                            f5,
                            i,
                            () -> MeshDrawUtil.drawLabeledTextured(Pipelines.AURA_BLOOM, builtBuffer, "sadnes aura ghosts warp mask", Texture.BLOOM.getIdentifier())
                        );
                }

                BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                this.buildGhosts(bufferBuilder2, renderEvent.getMatrices(), quaternionf, d, d2, d3, i, 1.0F);
                BuiltBuffer builtBuffer2 = bufferBuilder2.endNullable();
                if (builtBuffer2 != null) {
                    MeshDrawUtil.drawLabeledTextured(Pipelines.AURA_BLOOM, builtBuffer2, "sadnes aura ghosts", Texture.BLOOM.getIdentifier());
                }
            }
        }
    }

    private void buildGhosts(BufferBuilder bufferBuilder, MatrixStack matrixStack, Quaternionf quaternionf, double d, double d2, double d3, int i, float f) {
        for (byte b = 0; b < 9; b += 3) {
            int i2 = b * b;

            for (int i3 = 0; i3 < 12; i3++) {
                float f2 = this.ghostsTime * 0.055F + i3 * 0.1F;
                float f3 = (0.25F + i3 * 0.025F) * f;
                float f4 = (float)(d + 0.7F * MathHelper.sin(f2 + i2));
                float f5 = (float)(d2 + 0.5 + 0.3F * MathHelper.sin(this.ghostsTime * 0.055F + i3 * 0.2F) + 0.2F * b - 0.1F);
                float f6 = (float)(d3 + 0.7F * MathHelper.cos(f2 - i2));
                this.buildGradientQuad(matrixStack, bufferBuilder, quaternionf, f4, f5, f6, f3, 0.0F, i, i, i, i);
            }
        }
    }

    private void renderCrystal(RenderEvent renderEvent, AttackAura attackAura) {
        if (!(this.alphaAnimation.get() <= 2.0F)) {
            long l = System.nanoTime();
            float f = this.crystalLastNanos == 0L ? 0.016666668F : Math.min((float)(l - this.crystalLastNanos) / 1.0E9F, 0.05F);
            this.crystalLastNanos = l;
            this.crystalTime += f;
            float f2 = renderEvent.getTickDelta();
            Vec3d vec3d = renderEvent.getCamera().getCameraPos();
            float f3 = (float)(MathHelper.lerp(f2, this.target.lastX, this.target.getX()) - vec3d.x);
            float f4 = (float)(MathHelper.lerp(f2, this.target.lastY, this.target.getY()) - vec3d.y);
            float f5 = (float)(MathHelper.lerp(f2, this.target.lastZ, this.target.getZ()) - vec3d.z);
            float f6 = this.alphaAnimation.get() / 255.0F;
            float f7 = Math.max(0.0F, this.target.hurtTime - f2);
            float f8 = (float)Math.sin(f7 * (Math.PI / 10)) * 0.5F;
            int i = ColorUtil.interpolate(ColorUtil.multiplyAlpha(attackAura.getEspColorValue(), f6), ColorUtil.rgbWithAlphaf(255, 0, 0, f6), f8);
            float f9 = this.target.getHeight();
            float f10 = this.target.getWidth() * 1.5F * (1.2F - 0.5F * f6) * (1.0F - f8 * 0.3F);
            float f11 = this.crystalTime * 90.0F;
            int i2 = ColorUtil.multiplyAlpha(i, 1.0F);
            if (this.crystalAlphaRamp[64] != i2) {
                int i3 = ColorUtil.red(i2);
                int i4 = ColorUtil.green(i2);
                int i5 = ColorUtil.blue(i2);
                int i6 = ColorUtil.alpha(i2);

                for (int i7 = 0; i7 < 65; i7++) {
                    this.crystalAlphaRamp[i7] = ColorUtil.rgba(i3, i4, i5, i7 * i6 / 64);
                }
            }

            String string = attackAura.getEspDepthMode();
            boolean flag = string.equalsIgnoreCase("always")
                || !string.equalsIgnoreCase("never") && RaycastUtil.canSee(vec3d, CombatUtil.getClosestPoint(this.target));
            RenderPipeline renderPipeline = flag ? Pipelines.CRYSTAL : Pipelines.CRYSTAL_XRAY;
            RenderPipeline renderPipeline2 = flag ? Pipelines.AURA_BLOOM_DEPTH : Pipelines.AURA_BLOOM;
            Quaternionf quaternionf = renderEvent.getCamera().getRotation();

            try (BufferAllocator bufferAllocator = new BufferAllocator(4096)) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                this.buildCrystalGlow(bufferBuilder, renderEvent.getMatrices(), quaternionf, f3, f4, f5, f10, f11, f9, ColorUtil.setAlpha(i, 255), 1.3F);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer != null) {
                    float f12 = ProjectionUtil.getScaledSize(0.06F, Math.sqrt(f3 * f3 + f4 * f4 + f5 * f5));
                    EntityWarpRenderer.INSTANCE
                        .render(
                            f6 * 0.03F,
                            f12,
                            i,
                            () -> MeshDrawUtil.drawLabeledTextured(renderPipeline2, builtBuffer, "sadnes aura crystal warp mask", Texture.BLOOM.getIdentifier())
                        );
                }
            }

            Matrix4f matrix4f = renderEvent.getMatrices().peek().getPositionMatrix();

            try (BufferAllocator bufferAllocator2 = new BufferAllocator(262144)) {
                BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

                for (byte b = 0; b < 360; b += 12) {
                    float f13 = (float)Math.toRadians(b + f11);
                    float f14 = (float)Math.sin(f13) * f10;
                    float f15 = (float)Math.cos(f13) * f10;
                    float f16 = crystalHeight(b, f9);
                    float f17 = f9 * 0.5F - f16;
                    Quaternionf quaternionf2 = new Quaternionf().rotationTo(UP, new Vector3f(-f14, f17 * 0.5F, -f15).normalize());
                    Matrix4f matrix4f2 = new Matrix4f(matrix4f).translate(f3 + f14, f4 + f16, f5 + f15).rotate(quaternionf2).scale(0.07F);
                    this.emitCrystalMesh(bufferBuilder2, matrix4f2, quaternionf2, f3 + f14, f4 + f16, f5 + f15);
                }

                BuiltBuffer builtBuffer2 = bufferBuilder2.endNullable();
                if (builtBuffer2 != null) {
                    MeshDrawUtil.drawLabeled(renderPipeline, builtBuffer2, "sadnes aura crystal");
                }
            }

            try (BufferAllocator bufferAllocator3 = new BufferAllocator(4096)) {
                BufferBuilder bufferBuilder3 = new BufferBuilder(bufferAllocator3, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                this.buildCrystalGlow(bufferBuilder3, renderEvent.getMatrices(), quaternionf, f3, f4, f5, f10, f11, f9, ColorUtil.multiplyAlpha(i, 0.15F), 1.0F);
                BuiltBuffer builtBuffer3 = bufferBuilder3.endNullable();
                if (builtBuffer3 != null) {
                    MeshDrawUtil.drawLabeledTextured(renderPipeline2, builtBuffer3, "sadnes aura crystal bloom", Texture.BLOOM.getIdentifier());
                }
            }
        }
    }

    private void buildCrystalGlow(
        BufferBuilder bufferBuilder, MatrixStack matrixStack, Quaternionf quaternionf, float f, float f2, float f3, float f4, float f5, float f6, int i, float f7
    ) {
        for (byte b = 0; b < 360; b += 12) {
            float f8 = (float)Math.toRadians(b + f5);
            float f9 = (float)Math.sin(f8) * f4;
            float f10 = (float)Math.cos(f8) * f4;
            float f11 = crystalHeight(b, f6);
            this.buildGradientQuad(matrixStack, bufferBuilder, quaternionf, f + f9, f2 + f11, f3 + f10, f7, 0.0F, i, i, i, i);
        }
    }

    private static float crystalHeight(int i, float f) {
        float f2 = Math.abs((float)Math.sin(i));
        return 0.1F + f * f2 * f2;
    }

    private void emitCrystalMesh(BufferBuilder bufferBuilder, Matrix4f matrix4f, Quaternionf quaternionf, float f, float f2, float f3) {
        float f4 = Math.max(MathHelper.sqrt(f * f + f2 * f2 + f3 * f3), 1.0E-4F);
        float f5 = f / f4;
        float f6 = f2 / f4;
        float f7 = f3 / f4;
        float[] floats = CRYSTAL_MESH;
        byte b = 0;

        for (int i = floats.length; b < i; b += 6) {
            this.tempNormal.set(floats[b + 3], floats[b + 4], floats[b + 5]).rotate(quaternionf);
            float f8 = 1.0F - Math.abs(this.tempNormal.x * f5 + this.tempNormal.y * f6 + this.tempNormal.z * f7);
            float f9 = Math.max(0.0F, this.tempNormal.dot(LIGHT_DIRECTION));
            float f10 = MathHelper.clamp(0.18F + 0.45F * f9 * f9 + 0.62F * f8 * f8, 0.0F, 1.0F);
            bufferBuilder.vertex(matrix4f, floats[b], floats[b + 1], floats[b + 2]).color(this.crystalAlphaRamp[(int)(f10 * 64.0F)]);
        }
    }

    private static float[] buildCrystalMesh() {
        float f = 1.0F - 0.11F;
        float f2 = 1.5F - 0.11F;
        Vector3f[] vector3fs = new Vector3f[]{
            new Vector3f(f, 0.0F, 0.0F),
            new Vector3f(-f, 0.0F, 0.0F),
            new Vector3f(0.0F, f2, 0.0F),
            new Vector3f(0.0F, -f2, 0.0F),
            new Vector3f(0.0F, 0.0F, f),
            new Vector3f(0.0F, 0.0F, -f)
        };
        int[][] intses = new int[][]{{2, 0, 4}, {2, 4, 1}, {2, 1, 5}, {2, 5, 0}, {3, 4, 0}, {3, 1, 4}, {3, 5, 1}, {3, 0, 5}};
        Vector3f[] vector3fs2 = new Vector3f[intses.length];
        int i = 0;

        for (int i2 = intses.length; i < i2; i++) {
            Vector3f vector3f = vector3fs[intses[i][0]];
            Vector3f vector3f2 = vector3fs[intses[i][1]];
            Vector3f vector3f3 = vector3fs[intses[i][2]];
            Vector3f vector3f4 = new Vector3f(vector3f2).sub(vector3f).cross(new Vector3f(vector3f3).sub(vector3f)).normalize();
            if (vector3f4.dot(vector3f) + vector3f4.dot(vector3f2) + vector3f4.dot(vector3f3) < 0.0F) {
                vector3f4.negate();
            }

            vector3fs2[i] = vector3f4;
        }

        int[][] intses2 = new int[vector3fs.length][vector3fs.length];
        List<int[]> intses3 = new ArrayList<>();
        int i3 = 0;

        for (int i4 = intses.length; i3 < i4; i3++) {
            for (int i5 = 0; i5 < 3; i5++) {
                int i6 = intses[i3][i5];
                int i7 = intses[i3][(i5 + 1) % 3];
                int i8 = Math.min(i6, i7);
                int i9 = Math.max(i6, i7);
                if (intses2[i8][i9] == 0) {
                    intses2[i8][i9] = i3 + 1;
                } else {
                    intses3.add(new int[]{i8, i9, intses2[i8][i9] - 1, i3});
                }
            }
        }

        List<List<Vector3f>> vector3fses = new ArrayList<>();
        int i10 = 0;

        for (int i11 = vector3fs.length; i10 < i11; i10++) {
            Vector3f vector3f5 = new Vector3f(vector3fs[i10]).normalize();
            Vector3f vector3f6 = (Math.abs(vector3f5.y) < 0.9F ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(1.0F, 0.0F, 0.0F)).cross(vector3f5).normalize();
            Vector3f vector3f7 = new Vector3f(vector3f5).cross(vector3f6);
            List<Vector3f> vector3fs3 = new ArrayList<>();
            int i12 = 0;

            for (int i13 = intses.length; i12 < i13; i12++) {
                for (int i14 = 0; i14 < 3; i14++) {
                    if (intses[i12][i14] == i10) {
                        vector3fs3.add(vector3fs2[i12]);
                    }
                }
            }

            vector3fs3.sort(Comparator.comparingDouble((Vector3f vector3f8) -> Math.atan2(vector3f8.dot(vector3f7), vector3f8.dot(vector3f6))));
            vector3fses.add(vector3fs3);
        }

        int i15 = 0;

        for (List<Vector3f> vector3fs4 : vector3fses) {
            i15 += vector3fs4.size() << 2;
        }

        int i16 = intses.length + intses3.size() * 2 * 2 + i15;
        float[] floats = new float[i16 * 3 * 6];
        int i17 = 0;
        int i18 = 0;

        for (int i19 = intses.length; i18 < i19; i18++) {
            Vector3f vector3f9 = vector3fs2[i18];

            for (int i20 = 0; i20 < 3; i20++) {
                i17 = putVertex(floats, i17, vector3fs[intses[i18][i20]], vector3f9);
            }
        }

        for (int[] ints : intses3) {
            Vector3f vector3f10 = vector3fs[ints[0]];
            Vector3f vector3f11 = vector3fs[ints[1]];
            Vector3f vector3f12 = vector3fs2[ints[2]];
            Vector3f vector3f13 = vector3fs2[ints[3]];

            for (int i21 = 0; i21 < 2; i21++) {
                Vector3f vector3f14 = new Vector3f(vector3f12).lerp(vector3f13, i21 / 2.0F).normalize();
                Vector3f vector3f15 = new Vector3f(vector3f12).lerp(vector3f13, (i21 + 1) / 2.0F).normalize();
                int i22 = putVertex(floats, i17, vector3f10, vector3f14);
                int i23 = putVertex(floats, i22, vector3f11, vector3f14);
                int i24 = putVertex(floats, i23, vector3f11, vector3f15);
                int i25 = putVertex(floats, i24, vector3f11, vector3f15);
                int i26 = putVertex(floats, i25, vector3f10, vector3f15);
                i17 = putVertex(floats, i26, vector3f10, vector3f14);
            }
        }

        int i27 = 0;

        for (int i28 = vector3fs.length; i27 < i28; i27++) {
            Vector3f vector3f16 = new Vector3f(vector3fs[i27]).normalize();
            List list = (List)vector3fses.get(i27);
            int i29 = 0;

            for (int i30 = list.size(); i29 < i30; i29++) {
                i17 = emitSubdividedCorner(floats, i17, vector3fs[i27], vector3f16, (Vector3f)list.get(i29), (Vector3f)list.get((i29 + 1) % i30), 1);
            }
        }

        if (i17 != floats.length) {
            throw new IllegalStateException("crystal mesh: " + i17 + " != " + floats.length);
        } else {
            return floats;
        }
    }

    private static int emitSubdividedCorner(float[] floats, int i, Vector3f vector3f, Vector3f vector3f2, Vector3f vector3f3, Vector3f vector3f4, int i2) {
        if (i2 == 0) {
            int i3 = putVertex(floats, i, vector3f, vector3f2);
            int i4 = putVertex(floats, i3, vector3f, vector3f3);
            return putVertex(floats, i4, vector3f, vector3f4);
        } else {
            Vector3f vector3f5 = new Vector3f(vector3f2).add(vector3f3).normalize();
            Vector3f vector3f6 = new Vector3f(vector3f3).add(vector3f4).normalize();
            Vector3f vector3f7 = new Vector3f(vector3f4).add(vector3f2).normalize();
            int i5 = emitSubdividedCorner(floats, i, vector3f, vector3f2, vector3f5, vector3f7, i2 - 1);
            int i6 = emitSubdividedCorner(floats, i5, vector3f, vector3f5, vector3f3, vector3f6, i2 - 1);
            int i7 = emitSubdividedCorner(floats, i6, vector3f, vector3f7, vector3f6, vector3f4, i2 - 1);
            return emitSubdividedCorner(floats, i7, vector3f, vector3f5, vector3f6, vector3f7, i2 - 1);
        }
    }

    private static int putVertex(float[] floats, int i, Vector3f vector3f, Vector3f vector3f2) {
        floats[i++] = vector3f.x + vector3f2.x * 0.11F;
        floats[i++] = vector3f.y + vector3f2.y * 0.11F;
        floats[i++] = vector3f.z + vector3f2.z * 0.11F;
        floats[i++] = vector3f2.x;
        floats[i++] = vector3f2.y;
        floats[i++] = vector3f2.z;
        return i;
    }

    private void renderLightning(RenderEvent renderEvent, AttackAura attackAura) {
        if (!(this.alphaAnimation.get() <= 2.0F)) {
            long l = System.nanoTime();
            float f = this.lightningLastNanos == 0L ? 0.016666668F : Math.min((float)(l - this.lightningLastNanos) / 1.0E9F, 0.05F);
            this.lightningLastNanos = l;
            this.lightningTime += f;
            float f2 = renderEvent.getTickDelta();
            Vec3d vec3d = renderEvent.getCamera().getCameraPos();
            float f3 = (float)(MathHelper.lerp(f2, this.target.lastX, this.target.getX()) - vec3d.x);
            float f4 = (float)(MathHelper.lerp(f2, this.target.lastY, this.target.getY()) - vec3d.y);
            float f5 = (float)(MathHelper.lerp(f2, this.target.lastZ, this.target.getZ()) - vec3d.z);
            float f6 = this.target.getHeight();
            float f7 = this.target.getWidth() * 0.75F + 0.16F;
            float f8 = this.alphaAnimation.get() / 255.0F;
            float f9 = Math.max(0.0F, this.target.hurtTime - f2);
            float f10 = (float)Math.sin(f9 * (Math.PI / 10)) * 0.5F;
            int i = attackAura.getEspColorValue();
            int i2 = ColorUtil.interpolate(i, ColorUtil.rgba(255, 0, 0, ColorUtil.alpha(i)), f10);
            String string = attackAura.getEspDepthMode();
            boolean flag = string.equalsIgnoreCase("always")
                || !string.equalsIgnoreCase("never") && RaycastUtil.canSee(vec3d, CombatUtil.getClosestPoint(this.target));
            RenderPipeline renderPipeline = flag ? Pipelines.LIGHTNING_DEPTH : Pipelines.LIGHTNING;
            float f11 = f4 + f6 * 0.5F;
            float f12 = ProjectionUtil.getScaledSize(0.085F, Math.sqrt(f3 * f3 + f11 * f11 + f5 * f5));

            try (BufferAllocator bufferAllocator = new BufferAllocator(8192)) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
                this.buildLightningBolts(bufferBuilder, renderEvent.getMatrices().peek(), f3, f4, f6, f5, f7, i2, f8, 10.0F, 1.0F);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer != null) {
                    ParticleBlurRenderer.INSTANCE.render(f12, f8, () -> MeshDrawUtil.drawLabeled(renderPipeline, builtBuffer, "sadnes aura lightning bloom"));
                }
            }

            try (
                BufferAllocator bufferAllocator2 = new BufferAllocator(8192);
                BufferAllocator bufferAllocator3 = new BufferAllocator(8192);
            ) {
                BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
                this.buildLightningBolts(bufferBuilder2, renderEvent.getMatrices().peek(), f3, f4, f6, f5, f7, i2, f8, 22.0F, 0.4F);
                BuiltBuffer builtBuffer2 = bufferBuilder2.endNullable();
                if (builtBuffer2 != null) {
                    MeshDrawUtil.drawLabeled(renderPipeline, builtBuffer2, "sadnes aura lightning halo");
                }

                BufferBuilder bufferBuilder3 = new BufferBuilder(bufferAllocator3, DrawMode.LINES, VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH);
                this.buildLightningBolts(bufferBuilder3, renderEvent.getMatrices().peek(), f3, f4, f6, f5, f7, i2, f8, 9.0F, 1.0F);
                BuiltBuffer builtBuffer3 = bufferBuilder3.endNullable();
                if (builtBuffer3 != null) {
                    MeshDrawUtil.drawLabeled(renderPipeline, builtBuffer3, "sadnes aura lightning core");
                }
            }
        }
    }

    private void buildLightningBolts(
        BufferBuilder bufferBuilder, Entry entry, float f, float f2, float f3, float f4, float f5, int i, float f6, float f7, float f8
    ) {
        float f9 = this.lightningTime * 6.0F;
        int i2 = (int)f9;
        float f10 = f9 - i2;

        for (int i3 = 0; i3 < 3; i3++) {
            float f11 = this.lightningTime * 1.3F + (float)i3 / 3;
            int i4 = (int)Math.floor(f11);
            float f12 = f11 - i4;
            float f13 = MathHelper.clamp(f12 / 0.08F, 0.0F, 1.0F);
            float f14 = 1.0F - MathHelper.clamp((f12 - 0.42F) / 0.3F, 0.0F, 1.0F);
            float f15 = MathHelper.clamp(0.55F + 0.45F * smoothNoise(i3, 900, 0, i2, f10), 0.0F, 1.0F);
            float f16 = f13 * f14 * f15;
            if (!(f16 <= 0.01F)) {
                float f17 = f6 * f16 * f8;
                float f18 = this.lightningTime * 0.9F;
                float f19 = f3 * 0.16F * (float)Math.sin(this.lightningTime * 1.7F + i3 * 2.1F);
                float f20 = hashNoise(i3, 11, i4) * (float) Math.PI;
                float f21 = 0.6F + 0.5F * Math.abs(hashNoise(i3, 12, i4));
                float f22 = hashNoise(i3, 13, i4) >= 0.0F ? 1.0F : -1.0F;
                float f23 = f2 + f3 * (0.85F + 0.05F * hashNoise(i3, 14, i4));
                float f24 = f3 * 0.67F;
                float f25 = f5 * (1.0F + 0.12F * hashNoise(i3, 15, i4));
                float f26 = 0.0F;
                float f27 = 0.0F;
                float f28 = 0.0F;

                for (int i5 = 0; i5 <= 3; i5++) {
                    float f29 = (float)i5 / 3;
                    float f30 = f20 + f18 + f22 * f21 * (float) (Math.PI * 2) * f29;
                    float f31 = f + (float)Math.cos(f30) * f25;
                    float f32 = f4 + (float)Math.sin(f30) * f25;
                    float f33 = f23 - f24 * f29 + f19;
                    if (i5 > 0) {
                        int i6 = (i3 * 131 + i5) * 17 + i4 * 101 | 1;
                        this.buildBoltSegment(bufferBuilder, entry, f26, f27, f28, f31, f33, f32, 4, f7, f17, f17, i, i6, 0, i2, f10);
                    }

                    f26 = f31;
                    f27 = f33;
                    f28 = f32;
                }
            }
        }
    }

    private void buildBoltSegment(
        BufferBuilder bufferBuilder,
        Entry entry,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        int i,
        float f7,
        float f8,
        float f9,
        int i2,
        int i3,
        int i4,
        int i5,
        float f10
    ) {
        float f11 = f4 - f;
        float f12 = f5 - f2;
        float f13 = f6 - f3;
        float f14 = (float)Math.sqrt(f11 * f11 + f12 * f12 + f13 * f13);
        if (i > 0 && !(f14 < 1.0E-4F)) {
            float f15 = f11 / f14;
            float f16 = f12 / f14;
            float f17 = f13 / f14;
            float f18 = Math.abs(f16) < 0.9F ? 0.0F : 1.0F;
            float f19 = Math.abs(f16) < 0.9F ? 1.0F : 0.0F;
            float f20 = -f17 * f19;
            float f21 = f17 * f18;
            float f22 = f15 * f19 - f16 * f18;
            float f23 = (float)Math.sqrt(f20 * f20 + f21 * f21 + f22 * f22);
            if (f23 < 1.0E-5F) {
                f23 = 1.0F;
            }

            float f24 = f20 / f23;
            float f25 = f21 / f23;
            float f26 = f22 / f23;
            float f27 = f16 * f26 - f17 * f25;
            float f28 = f17 * f24 - f15 * f26;
            float f29 = f15 * f25 - f16 * f24;
            float f30 = smoothNoise(i3, 0, 0, i5, f10);
            float f31 = smoothNoise(i3, 1, 0, i5, f10);
            float f32 = f14 * 0.3F;
            float f33 = (f + f4) * 0.5F + (f24 * f30 + f27 * f31) * f32;
            float f34 = (f2 + f5) * 0.5F + (f25 * f30 + f28 * f31) * f32;
            float f35 = (f3 + f6) * 0.5F + (f26 * f30 + f29 * f31) * f32;
            float f36 = (f8 + f9) * 0.5F;
            this.buildBoltSegment(bufferBuilder, entry, f, f2, f3, f33, f34, f35, i - 1, f7, f8, f36, i2, i3 * 2 + 7, i4, i5, f10);
            this.buildBoltSegment(bufferBuilder, entry, f33, f34, f35, f4, f5, f6, i - 1, f7, f36, f9, i2, i3 * 2 + 9, i4, i5, f10);
            if (i4 < 2 && i >= 2 && smoothNoise(i3, 2, 0, i5, f10) > 0.45F) {
                float f37 = smoothNoise(i3, 3, 0, i5, f10);
                float f38 = smoothNoise(i3, 4, 0, i5, f10);
                float f39 = f15 * 0.4F + f24 * f37 + f27 * f38;
                float f40 = f16 * 0.4F + f25 * f37 + f28 * f38;
                float f41 = f17 * 0.4F + f26 * f37 + f29 * f38;
                float f42 = (float)Math.sqrt(f39 * f39 + f40 * f40 + f41 * f41);
                if (f42 < 1.0E-5F) {
                    f42 = 1.0F;
                }

                float f43 = f14 * 0.75F;
                this.buildBoltSegment(
                    bufferBuilder,
                    entry,
                    f33,
                    f34,
                    f35,
                    f33 + f39 / f42 * f43,
                    f34 + f40 / f42 * f43,
                    f35 + f41 / f42 * f43,
                    Math.min(i - 1, 3),
                    f7 * 0.55F,
                    f36 * 0.7F,
                    0.0F,
                    i2,
                    i3 * 3 + 13,
                    i4 + 1,
                    i5,
                    f10
                );
            }
        } else {
            this.emitLine(bufferBuilder, entry, f, f2, f3, f4, f5, f6, ColorUtil.multiplyAlpha(i2, f8), ColorUtil.multiplyAlpha(i2, f9), f7);
        }
    }

    private void emitLine(
        BufferBuilder bufferBuilder, Entry entry, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7
    ) {
        float f8 = f4 - f;
        float f9 = f5 - f2;
        float f10 = f6 - f3;
        float f11 = (float)Math.sqrt(f8 * f8 + f9 * f9 + f10 * f10);
        if (!(f11 < 1.0E-5F)) {
            float f12 = 1.0F / f11;
            float f13 = f8 * f12;
            float f14 = f9 * f12;
            float f15 = f10 * f12;
            bufferBuilder.vertex(entry, f, f2, f3).color(i).normal(entry, f13, f14, f15).lineWidth(f7);
            bufferBuilder.vertex(entry, f4, f5, f6).color(i2).normal(entry, f13, f14, f15).lineWidth(f7);
        }
    }

    private static float smoothNoise(int i, int i2, int i3, int i4, float f) {
        float f2 = hashNoise(i, i2, i4 * 4 + i3);
        float f3 = hashNoise(i, i2, (i4 + 1) * 4 + i3);
        float f4 = f * f * (3.0F - 2.0F * f);
        return f2 + (f3 - f2) * f4;
    }

    private static float hashNoise(int i, int i2, int i3) {
        int i4 = i * 374761393 + i2 * 668265263 + i3 * 1274126177;
        int i5 = (i4 ^ i4 >>> 13) * 1274126177;
        int i6 = i5 ^ i5 >>> 16;
        return (i6 & 65535) / 32768.0F - 1.0F;
    }

    private void renderInfinity(RenderEvent renderEvent, AttackAura attackAura) {
        if (!(this.alphaAnimation.get() <= 2.0F)) {
            long l = System.nanoTime();
            float f = this.infinityLastNanos == 0L ? 0.016666668F : (float)(l - this.infinityLastNanos) / 1.0E9F;
            float f2 = Math.min(f, 0.05F);
            this.infinityLastNanos = l;
            this.infinityTime += f2;
            if (f > 0.25F) {
                this.smokePuffCount = 0;
                this.smokeSpawnAccumulator = 0.0F;
            }

            if (!(this.target.getHeight() <= 0.0F)) {
                float f3 = renderEvent.getTickDelta();
                Vec3d vec3d = renderEvent.getCamera().getCameraPos();
                double d = MathHelper.lerp(f3, this.target.lastX, this.target.getX());
                double d2 = MathHelper.lerp(f3, this.target.lastY, this.target.getY()) + this.target.getHeight() * 0.62F;
                double d3 = MathHelper.lerp(f3, this.target.lastZ, this.target.getZ());
                float f4 = (float)(d - vec3d.x);
                float f5 = (float)(d2 - vec3d.y);
                float f6 = (float)(d3 - vec3d.z);
                float f7 = this.alphaAnimation.get() / 255.0F;
                float f8 = Math.max(0.0F, this.target.hurtTime - f3);
                float f9 = (float)Math.sin(f8 * (Math.PI / 10)) * 0.5F;
                int i = attackAura.getEspColorValue();
                int i2 = ColorUtil.interpolate(i, ColorUtil.rgba(255, 0, 0, ColorUtil.alpha(i)), f9);
                if (this.tubeAlphaRamp[64] != i2) {
                    int i3 = ColorUtil.red(i2);
                    int i4 = ColorUtil.green(i2);
                    int i5 = ColorUtil.blue(i2);
                    int i6 = ColorUtil.alpha(i2);

                    for (int i7 = 0; i7 < 65; i7++) {
                        this.tubeAlphaRamp[i7] = ColorUtil.rgba(i3, i4, i5, i7 * i6 / 64);
                    }
                }

                float f10 = this.infinityTime * 13.0F;
                int i8 = (int)f10;
                float f11 = smoothNoise(0, 77, 0, i8, f10 - i8);
                float f12 = (0.9F + 0.07F * MathHelper.sin(this.infinityTime * 7.3F) + 0.03F * MathHelper.sin(this.infinityTime * 17.1F))
                    * (1.0F - 0.28F * MathHelper.clamp((f11 - 0.82F) / 0.18F, 0.0F, 1.0F));
                float f13 = this.target.getHeight()
                    * 0.26F
                    * (0.74F + 0.26F * f7)
                    * (1.0F + f9 * 0.28F)
                    * (1.0F + 0.035F * MathHelper.sin(this.infinityTime * 2.1F));
                float f14 = f13 * 0.125F;
                float f15 = Math.max(f13 * 0.15F, f14 * 1.05F);
                float f16 = f15 * 0.3F;
                float f17 = f13 * 0.78F;
                this.cornerX[0] = -f17;
                this.cornerY[0] = f13;
                this.cornerX[1] = f17;
                this.cornerY[1] = f13;
                this.cornerX[2] = -f16;
                this.cornerY[2] = 0.0F;
                this.cornerX[3] = f17;
                this.cornerY[3] = -f13;
                this.cornerX[4] = -f17;
                this.cornerY[4] = -f13;
                this.cornerX[5] = f16;
                this.cornerY[5] = 0.0F;
                int i9 = this.buildRoundedPath(6, f13 * 0.09F, f15);
                this.computePathNormals(i9, f14, f13 * 0.16F, this.infinityTime * 0.17F % 1.0F);
                this.infinityPointCount = i9;
                this.billboardRotation.set(renderEvent.getCamera().getRotation()).rotateZ(0.11F * MathHelper.sin(this.infinityTime * 0.62F));
                this.billboardRight.set(1.0F, 0.0F, 0.0F).rotate(this.billboardRotation);
                this.billboardUp.set(0.0F, 1.0F, 0.0F).rotate(this.billboardRotation);
                this.updateSmoke(f2, d, d2, d3, f13, f7);
                float f18 = f7 * f12 * 0.8F;
                Matrix4f matrix4f = renderEvent.getMatrices().peek().getPositionMatrix();
                double d4 = Math.sqrt(f4 * f4 + f5 * f5 + f6 * f6);
                float f19 = Math.min(0.045F + 0.03F * (1.0F - f7) + 0.02F * f9, 0.06F);
                float f20 = ProjectionUtil.getScaledSize(f19, d4);

                try (
                    BufferAllocator bufferAllocator = new BufferAllocator(16384);
                    BufferAllocator bufferAllocator2 = new BufferAllocator(16384);
                ) {
                    BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    this.buildTube(bufferBuilder, matrix4f, i9, f4, f5, f6, 0.0F, f18);
                    BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                    BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    this.buildSmoke(bufferBuilder2, matrix4f, renderEvent.getCamera().getRotation(), vec3d, ColorUtil.darken(i2, 0.9F), f7);
                    BuiltBuffer builtBuffer2 = bufferBuilder2.endNullable();
                    if (builtBuffer != null || builtBuffer2 != null) {
                        ParticleBlurRenderer.INSTANCE
                            .render(
                                f20,
                                f7 * 0.7F,
                                () -> {
                                    if (builtBuffer != null) {
                                        MeshDrawUtil.drawLabeled(Pipelines.NEON_TUBE, builtBuffer, "sadnes aura infinity wash");
                                    }

                                    if (builtBuffer2 != null) {
                                        MeshDrawUtil.drawLabeledTextured(
                                            Pipelines.PARTICLE_TEXTURE, builtBuffer2, "sadnes aura infinity smoke", Texture.BLOOM.getIdentifier()
                                        );
                                    }
                                }
                            );
                    }
                }

                try (BufferAllocator bufferAllocator3 = new BufferAllocator(16384)) {
                    BufferBuilder bufferBuilder3 = new BufferBuilder(bufferAllocator3, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    this.buildTube(bufferBuilder3, matrix4f, i9, f4, f5, f6, 1.0F, f18);
                    BuiltBuffer builtBuffer3 = bufferBuilder3.endNullable();
                    if (builtBuffer3 != null) {
                        MeshDrawUtil.drawLabeled(Pipelines.NEON_TUBE, builtBuffer3, "sadnes aura infinity tube");
                    }
                }
            }
        }
    }

    private int buildRoundedPath(int i, float f, float f2) {
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = (i2 + i - 1) % i;
            int i4 = (i2 + 1) % i;
            float f3 = this.cornerX[i3] - this.cornerX[i2];
            float f4 = this.cornerY[i3] - this.cornerY[i2];
            float f5 = this.cornerX[i4] - this.cornerX[i2];
            float f6 = this.cornerY[i4] - this.cornerY[i2];
            float f7 = (float)Math.sqrt(f3 * f3 + f4 * f4);
            float f8 = (float)Math.sqrt(f5 * f5 + f6 * f6);
            float f9 = (float)Math.acos(MathHelper.clamp((f3 * f5 + f4 * f6) / (f7 * f8), -1.0F, 1.0F)) * 0.5F;
            this.cornerRadius[i2] = Math.min(f2 / (float)Math.tan(f9), 0.45F * Math.min(f7, f8));
        }

        int i5 = this.pathX.length;
        int i6 = 0;

        for (int i7 = 0; i7 < i; i7++) {
            this.cornerPathStart[i7] = i6;
            int i8 = (i7 + i - 1) % i;
            int i9 = (i7 + 1) % i;
            float f10 = this.cornerX[i7];
            float f11 = this.cornerY[i7];
            float f12 = this.cornerX[i8] - f10;
            float f13 = this.cornerY[i8] - f11;
            float f14 = this.cornerX[i9] - f10;
            float f15 = this.cornerY[i9] - f11;
            float f16 = (float)Math.sqrt(f12 * f12 + f13 * f13);
            float f17 = (float)Math.sqrt(f14 * f14 + f15 * f15);
            float f18 = f12 / f16;
            float f19 = f13 / f16;
            float f20 = f14 / f17;
            float f21 = f15 / f17;
            float f22 = this.cornerRadius[i7];
            float f23 = (float)Math.acos(MathHelper.clamp(f18 * f20 + f19 * f21, -1.0F, 1.0F)) * 0.5F;
            float f24 = Math.min(f22 * (float)Math.tan(f23), f2);
            float f25 = f18 + f20;
            float f26 = f19 + f21;
            float f27 = Math.max((float)Math.sqrt(f25 * f25 + f26 * f26), 1.0E-4F);
            float f28 = f24 / (float)Math.sin(f23);
            float f29 = f10 + f25 / f27 * f28;
            float f30 = f11 + f26 / f27 * f28;
            float f31 = (float)Math.atan2(f11 + f19 * f22 - f30, f10 + f18 * f22 - f29);
            float f32 = (float)Math.atan2(f11 + f21 * f22 - f30, f10 + f20 * f22 - f29) - f31;
            if (f32 > Math.PI) {
                f32 -= (float) (Math.PI * 2);
            }

            if (f32 < -Math.PI) {
                f32 += (float) (Math.PI * 2);
            }

            int i10 = Math.max(2, (int)Math.ceil(Math.abs(f32) / 0.22F));

            for (int i11 = 0; i11 <= i10 && i6 < i5; i11++) {
                float f33 = f31 + f32 * i11 / i10;
                this.pathX[i6] = f29 + (float)Math.cos(f33) * f24;
                this.pathY[i6] = f30 + (float)Math.sin(f33) * f24;
                i6++;
            }

            float f34 = f10 + f20 * f22;
            float f35 = f11 + f21 * f22;
            float f36 = this.cornerX[i9] - f20 * this.cornerRadius[i9];
            float f37 = this.cornerY[i9] - f21 * this.cornerRadius[i9];
            float f38 = f36 - f34;
            float f39 = f37 - f35;
            int i12 = Math.max(1, (int)Math.ceil(Math.sqrt(f38 * f38 + f39 * f39) / f));

            for (int i13 = 1; i13 < i12 && i6 < i5; i13++) {
                this.pathX[i6] = f34 + f38 * i13 / i12;
                this.pathY[i6] = f35 + f39 * i13 / i12;
                i6++;
            }
        }

        return i6;
    }

    private void computePathNormals(int i, float f, float f2, float f3) {
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = i2 == 0 ? i - 1 : i2 - 1;
            int i4 = i2 + 1 == i ? 0 : i2 + 1;
            float f4 = this.pathX[i2] - this.pathX[i3];
            float f5 = this.pathY[i2] - this.pathY[i3];
            float f6 = this.pathX[i4] - this.pathX[i2];
            float f7 = this.pathY[i4] - this.pathY[i2];
            float f8 = Math.max((float)Math.sqrt(f4 * f4 + f5 * f5), 1.0E-5F);
            float f9 = Math.max((float)Math.sqrt(f6 * f6 + f7 * f7), 1.0E-5F);
            float f10 = -f5 / f8;
            float f11 = f4 / f8;
            float f12 = -f7 / f9;
            float f13 = f6 / f9;
            float f14 = f10 + f12;
            float f15 = f11 + f13;
            float f16 = Math.max((float)Math.sqrt(f14 * f14 + f15 * f15), 1.0E-5F);
            float f17 = f14 / f16;
            float f18 = f15 / f16;
            float f19 = Math.min(1.0F / Math.max(f17 * f12 + f18 * f13, 0.35F), 2.5F);
            float f20 = (float)i2 / i;
            float f21 = f * (1.0F + 0.055F * MathHelper.sin(f20 * 43.98F)) * f19;
            this.pathNormalX[i2] = f17 * f21;
            this.pathNormalY[i2] = f18 * f21;
            float f22 = (float)Math.sqrt(this.pathX[i2] * this.pathX[i2] + this.pathY[i2] * this.pathY[i2]);
            float f23 = MathHelper.clamp(f22 / f2, 0.0F, 1.0F);
            float f24 = f20 - f3;
            float f25 = f24 - (float)Math.floor(f24 + 0.5F);
            this.pathIntensity[i2] = (0.55F + 0.45F * f23 * f23 * (3.0F - 2.0F * f23)) * (1.0F + 0.22F * (float)Math.exp(-f25 * f25 / 0.0028F));
        }
    }

    private void buildTube(BufferBuilder bufferBuilder, Matrix4f matrix4f, int i, float f, float f2, float f3, float f4, float f5) {
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = i2 + 1 == i ? 0 : i2 + 1;
            int i4 = this.tubeAlphaRamp[MathHelper.clamp(Math.round(this.pathIntensity[i2] * f5 * 64.0F), 0, 64)];
            int i5 = this.tubeAlphaRamp[MathHelper.clamp(Math.round(this.pathIntensity[i3] * f5 * 64.0F), 0, 64)];
            this.emitBillboardVertex(
                bufferBuilder, matrix4f, f, f2, f3, this.pathX[i2] + this.pathNormalX[i2], this.pathY[i2] + this.pathNormalY[i2], 1.0F, f4, i4
            );
            this.emitBillboardVertex(
                bufferBuilder, matrix4f, f, f2, f3, this.pathX[i2] - this.pathNormalX[i2], this.pathY[i2] - this.pathNormalY[i2], -1.0F, f4, i4
            );
            this.emitBillboardVertex(
                bufferBuilder, matrix4f, f, f2, f3, this.pathX[i3] - this.pathNormalX[i3], this.pathY[i3] - this.pathNormalY[i3], -1.0F, f4, i5
            );
            this.emitBillboardVertex(
                bufferBuilder, matrix4f, f, f2, f3, this.pathX[i3] + this.pathNormalX[i3], this.pathY[i3] + this.pathNormalY[i3], 1.0F, f4, i5
            );
        }
    }

    private void emitBillboardVertex(
        BufferBuilder bufferBuilder, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, int i
    ) {
        bufferBuilder.vertex(
                matrix4f,
                f + this.billboardRight.x * f4 + this.billboardUp.x * f5,
                f2 + this.billboardRight.y * f4 + this.billboardUp.y * f5,
                f3 + this.billboardRight.z * f4 + this.billboardUp.z * f5
            )
            .texture(f6, f7)
            .color(i);
    }

    private void updateSmoke(float f, double d, double d2, double d3, float f2, float f3) {
        if (this.smokePuffs == null) {
            this.smokePuffs = new TargetEspRenderer.SmokePuff[40];
            int i = 0;

            for (int i2 = this.smokePuffs.length; i < i2; i++) {
                this.smokePuffs[i] = new TargetEspRenderer.SmokePuff();
            }
        }

        if (this.smokeTargetId != this.target.getId() || f3 < 0.02F) {
            this.smokePuffCount = 0;
            this.smokeSpawnAccumulator = 0.0F;
            this.smokeTargetId = this.target.getId();
        }

        float f4 = this.infinityTime;
        float f5 = Math.max(0.0F, 1.0F - 1.5F * f);
        int i3 = 0;

        while (i3 < this.smokePuffCount) {
            TargetEspRenderer.SmokePuff smokePuff = this.smokePuffs[i3];
            smokePuff.age += f;
            if (smokePuff.age >= smokePuff.lifetime) {
                this.smokePuffCount--;
                this.smokePuffs[i3] = this.smokePuffs[this.smokePuffCount];
                this.smokePuffs[this.smokePuffCount] = smokePuff;
            } else {
                smokePuff.velocityY += 0.34F * f;
                float f6 = (float)(Math.sin(smokePuff.y * 3.1 + f4 * 0.7 + smokePuff.phase) * Math.cos(smokePuff.z * 2.3 - f4 * 0.5));
                float f7 = (float)(Math.cos(smokePuff.x * 2.7 - f4 * 0.6 + smokePuff.phase) * Math.sin(smokePuff.y * 3.4 + f4 * 0.4));
                smokePuff.velocityX = (smokePuff.velocityX + f6 * 0.26F * f) * f5;
                smokePuff.velocityY *= f5;
                smokePuff.velocityZ = (smokePuff.velocityZ + f7 * 0.26F * f) * f5;
                smokePuff.x = smokePuff.x + smokePuff.velocityX * f;
                smokePuff.y = smokePuff.y + smokePuff.velocityY * f;
                smokePuff.z = smokePuff.z + smokePuff.velocityZ * f;
                smokePuff.size = smokePuff.size + smokePuff.growth * f;
                smokePuff.rotation = smokePuff.rotation + smokePuff.rotationSpeed * f;
                i3++;
            }
        }

        this.smokeSpawnAccumulator = Math.min(this.smokeSpawnAccumulator + f * 12.0F * f3, 4.0F);

        while (this.smokeSpawnAccumulator >= 1.0F && this.smokePuffCount < this.smokePuffs.length) {
            this.smokeSpawnAccumulator--;
            TargetEspRenderer.SmokePuff smokePuff2 = this.smokePuffs[this.smokePuffCount++];
            int i4 = ++this.smokeSpawnCounter;
            smokePuff2.x = d + hashNoise(i4, 1, 0) * f2 * 1.25F;
            smokePuff2.y = d2 - f2 * (0.35F + 0.35F * Math.abs(hashNoise(i4, 2, 0)));
            smokePuff2.z = d3 + hashNoise(i4, 3, 0) * f2 * 1.25F;
            smokePuff2.velocityX = hashNoise(i4, 4, 0) * 0.07F;
            smokePuff2.velocityY = 0.05F + 0.05F * Math.abs(hashNoise(i4, 5, 0));
            smokePuff2.velocityZ = hashNoise(i4, 6, 0) * 0.07F;
            smokePuff2.age = 0.0F;
            smokePuff2.lifetime = 2.4F + 1.2F * Math.abs(hashNoise(i4, 7, 0));
            smokePuff2.size = f2 * (0.24F + 0.14F * Math.abs(hashNoise(i4, 8, 0)));
            smokePuff2.growth = smokePuff2.size * 0.45F;
            smokePuff2.rotation = hashNoise(i4, 9, 0) * 3.14159F;
            smokePuff2.rotationSpeed = hashNoise(i4, 10, 0) * 0.45F;
            smokePuff2.stretch = 1.0F + 0.35F * hashNoise(i4, 11, 0);
            smokePuff2.phase = hashNoise(i4, 12, 0) * 3.14159F;
        }
    }

    private void buildSmoke(BufferBuilder bufferBuilder, Matrix4f matrix4f, Quaternionf quaternionf, Vec3d vec3d, int i, float f) {
        for (int i2 = 0; i2 < this.smokePuffCount; i2++) {
            TargetEspRenderer.SmokePuff smokePuff = this.smokePuffs[i2];
            if (ProjectionUtil.isVisible(smokePuff.x, smokePuff.y, smokePuff.z)) {
                float f2 = smokePuff.age / smokePuff.lifetime;
                float f3 = 1.0F - MathHelper.clamp((f2 - 0.42F) / 0.58F, 0.0F, 1.0F);
                float f4 = Math.min(f2 / 0.16F, 1.0F) * f3 * f3 * f * 0.09F;
                if (!(f4 < 0.004F)) {
                    int i3 = ColorUtil.multiplyAlpha(i, f4);
                    float f5 = (float)(smokePuff.x - vec3d.x);
                    float f6 = (float)(smokePuff.y - vec3d.y);
                    float f7 = (float)(smokePuff.z - vec3d.z);

                    for (int i4 = 0; i4 < 3; i4++) {
                        float f8 = smokePuff.rotation + i4 * 2.094F;
                        float f9 = smokePuff.size * (0.78F + 0.18F * i4);
                        this.emitRotatedQuad(
                            bufferBuilder,
                            matrix4f,
                            quaternionf,
                            f5,
                            f6,
                            f7,
                            MathHelper.cos(f8) * smokePuff.size * 0.3F,
                            MathHelper.sin(f8) * smokePuff.size * 0.3F,
                            f9 * smokePuff.stretch,
                            f9 / smokePuff.stretch,
                            f8 * 0.6F,
                            i3
                        );
                    }
                }
            }
        }
    }

    private void renderSunlight(RenderEvent renderEvent, AttackAura attackAura) {
        float f = this.sunlightAnimation.get();
        if (!(f <= 0.002F)) {
            boolean flag;
            if (this.target != null) {
                flag = true;
            } else {
                flag = false;
                if (this.sunlightColor == -1) {
                    return;
                }
            }

            long l = System.nanoTime();
            float f2 = this.sunlightLastNanos == 0L ? 0.016666668F : (float)(l - this.sunlightLastNanos) / 1.0E9F;
            float f3 = Math.min(f2, 0.05F);
            this.sunlightLastNanos = l;
            this.sunlightTime += f3;
            if (f2 > 0.25F) {
                this.emberCount = 0;
                this.emberSpawnAccumulator = 0.0F;
            }

            float f4 = renderEvent.getTickDelta();
            Vec3d vec3d = renderEvent.getCamera().getCameraPos();
            float f5 = 0.0F;
            if (flag) {
                if (this.target.getHeight() <= 0.0F) {
                    return;
                }

                this.sunlightX = MathHelper.lerp(f4, this.target.lastX, this.target.getX());
                this.sunlightY = MathHelper.lerp(f4, this.target.lastY, this.target.getY()) + this.target.getHeight() * 0.62F;
                this.sunlightZ = MathHelper.lerp(f4, this.target.lastZ, this.target.getZ());
                float f6 = Math.max(0.0F, this.target.hurtTime - f4);
                f5 = (float)Math.sin(f6 * (Math.PI / 10)) * 0.5F;
                int i = attackAura.getEspColorValue();
                this.sunlightColor = ColorUtil.interpolate(i, ColorUtil.rgba(255, 0, 0, ColorUtil.alpha(i)), f5);
                this.sunlightScale = this.target.getHeight() * 0.1733F;
                int i2 = this.target.hurtTime;
                if (this.pulseTargetId != this.target.getId()) {
                    this.pulseTargetId = this.target.getId();
                    this.lastTargetHurtTime = i2;
                }

                if (i2 > this.lastTargetHurtTime) {
                    this.pulseStrength = 1.0F;
                }

                this.lastTargetHurtTime = i2;
            }

            if (mc.player != null) {
                int i3 = mc.player.handSwingTicks;
                if (i3 < this.lastPlayerHurtTime) {
                    this.pulseStrength = 1.0F;
                }

                this.lastPlayerHurtTime = i3;
            }

            this.pulseStrength = Math.max(0.0F, this.pulseStrength - f3 * 1.7F);
            float f7 = 1.0F - this.pulseStrength;
            float f8 = this.pulseStrength <= 0.0F ? 0.0F : MathHelper.sin((float)Math.pow(f7, 0.55) * (float) Math.PI);
            double d = this.sunlightX;
            double d2 = this.sunlightY;
            double d3 = this.sunlightZ;
            float f9 = (float)(d - vec3d.x);
            float f10 = (float)(d2 - vec3d.y);
            float f11 = (float)(d3 - vec3d.z);
            float f12 = (float)Math.sqrt(f9 * f9 + f10 * f10 + f11 * f11);
            if (f8 > 0.001F && f12 > 1.0E-4F) {
                float f13 = f8 * 0.22F / f12;
                f9 += f9 * f13;
                f10 += f10 * f13;
                f11 += f11 * f13;
            }

            int i4 = this.sunlightColor;
            if (this.tubeAlphaRamp[64] != i4) {
                int i5 = ColorUtil.red(i4);
                int i6 = ColorUtil.green(i4);
                int i7 = ColorUtil.blue(i4);
                int i8 = ColorUtil.alpha(i4);

                for (int i9 = 0; i9 < 65; i9++) {
                    this.tubeAlphaRamp[i9] = ColorUtil.rgba(i5, i6, i7, i9 * i8 / 64);
                }
            }

            float f14 = 0.9F + 0.07F * MathHelper.sin(this.sunlightTime * 7.3F) + 0.03F * MathHelper.sin(this.sunlightTime * 17.1F);
            float f15 = this.sunlightScale
                * (0.74F + 0.26F * f)
                * (1.0F + f5 * 0.28F)
                * (1.0F + 0.035F * MathHelper.sin(this.sunlightTime * 2.1F))
                * (1.0F - 0.08F * f8);
            float f16 = f15 * 0.095F;
            int i10 = ShapePointData.getPointCount();
            float f17 = f15 * (1.0F + 0.17F * f8);
            float f18 = f15 * (1.0F - 0.15F * f8);

            for (int i11 = 0; i11 < i10; i11++) {
                this.pathX[i11] = ShapePointData.POINTS[i11 * 2] * f17;
                this.pathY[i11] = ShapePointData.POINTS[i11 * 2 + 1] * f18;
            }

            this.computePathNormals(i10, f16, f15 * 0.16F, this.sunlightTime * 0.17F % 1.0F);
            this.applyDisperse(i10, 8, f, f15 * 0.6F);
            this.billboardRotation.set(renderEvent.getCamera().getRotation()).rotateZ(0.07F * MathHelper.sin(this.sunlightTime * 0.62F));
            this.billboardRight.set(1.0F, 0.0F, 0.0F).rotate(this.billboardRotation);
            this.billboardUp.set(0.0F, 1.0F, 0.0F).rotate(this.billboardRotation);
            this.updateEmbers(f3, d, d2, d3, f15, flag ? f : 0.0F);
            float f19 = f * f14 * 0.8F;
            Matrix4f matrix4f = renderEvent.getMatrices().peek().getPositionMatrix();
            double d4 = Math.sqrt(f9 * f9 + f10 * f10 + f11 * f11);
            float f20 = ProjectionUtil.getScaledSize(Math.min(0.045F + 0.03F * (1.0F - f) + 0.02F * f5, 0.06F), d4);
            Quaternionf quaternionf = renderEvent.getCamera().getRotation();

            try (
                BufferAllocator bufferAllocator = new BufferAllocator(16384);
                BufferAllocator bufferAllocator2 = new BufferAllocator(32768);
            ) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                this.buildTube(bufferBuilder, matrix4f, i10, f9, f10, f11, 0.0F, f19);
                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                this.buildEmbers(bufferBuilder2, matrix4f, quaternionf, vec3d, i4, f, 2.6F);
                BuiltBuffer builtBuffer2 = bufferBuilder2.endNullable();
                if (builtBuffer != null || builtBuffer2 != null) {
                    ParticleBlurRenderer.INSTANCE.render(f20, f * 0.7F, () -> {
                        if (builtBuffer != null) {
                            MeshDrawUtil.drawLabeled(Pipelines.NEON_TUBE, builtBuffer, "sadnes aura sunlight wash");
                        }

                        if (builtBuffer2 != null) {
                            MeshDrawUtil.drawLabeledTextured(Pipelines.AURA_BLOOM, builtBuffer2, "sadnes aura sunlight ember glow", Texture.BLOOM.getIdentifier());
                        }
                    });
                }
            }

            try (
                BufferAllocator bufferAllocator3 = new BufferAllocator(16384);
                BufferAllocator bufferAllocator4 = new BufferAllocator(32768);
            ) {
                BufferBuilder bufferBuilder3 = new BufferBuilder(bufferAllocator3, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                this.buildTube(bufferBuilder3, matrix4f, i10, f9, f10, f11, 1.0F, f19);
                BuiltBuffer builtBuffer3 = bufferBuilder3.endNullable();
                if (builtBuffer3 != null) {
                    MeshDrawUtil.drawLabeled(Pipelines.NEON_TUBE, builtBuffer3, "sadnes aura sunlight tube");
                }

                BufferBuilder bufferBuilder4 = new BufferBuilder(bufferAllocator4, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                this.buildEmbers(bufferBuilder4, matrix4f, quaternionf, vec3d, i4, f, 1.0F);
                BuiltBuffer builtBuffer4 = bufferBuilder4.endNullable();
                if (builtBuffer4 != null) {
                    MeshDrawUtil.drawLabeledTextured(Pipelines.AURA_BLOOM, builtBuffer4, "sadnes aura sunlight embers", Texture.BLOOM.getIdentifier());
                }
            }

            this.renderPulseWarp(renderEvent);
        }
    }

    private void renderPulseWarp(RenderEvent renderEvent) {
        if (!(this.pulseStrength <= 0.02F) && mc.world != null) {
            Box box = new Box(
                this.sunlightX - 4.0, this.sunlightY - 4.0, this.sunlightZ - 4.0, this.sunlightX + 4.0, this.sunlightY + 4.0, this.sunlightZ + 4.0
            );
            ArrayList arrayList = new ArrayList();

            for (Entity entity2 : mc.world.getEntitiesByClass(LivingEntity.class, box, livingEntity -> !livingEntity.isSpectator() && livingEntity != mc.player)) {
                arrayList.add(entity2);
            }

            if (!arrayList.isEmpty()) {
                EntityWarpRenderer.INSTANCE
                    .renderEntities(renderEvent.getMatrices(), renderEvent.getCamera(), renderEvent.getTickDelta(), arrayList, this.pulseStrength * 0.022F, 22.0F, this.sunlightColor);
            }
        }
    }

    private void applyDisperse(int i, int i2, float f, float f2) {
        float f3 = 1.0F / (1.0F + 0.55F * Math.max(i2 - 1, 1));

        for (int i3 = 0; i3 < i2; i3++) {
            this.segmentDirX[i3] = 0.0F;
            this.segmentDirY[i3] = 0.0F;
            this.segmentCount[i3] = 0;
        }

        for (int i4 = 0; i4 < i; i4++) {
            int i5 = Math.min(i4 * i2 / i, i2 - 1);
            this.segmentDirX[i5] = this.segmentDirX[i5] + this.pathX[i4];
            this.segmentDirY[i5] = this.segmentDirY[i5] + this.pathY[i4];
            this.segmentCount[i5]++;
        }

        for (int i6 = 0; i6 < i2; i6++) {
            int i7 = Math.max(this.segmentCount[i6], 1);
            float f4 = this.segmentDirX[i6] / i7;
            float f5 = this.segmentDirY[i6] / i7;
            float f6 = Math.max((float)Math.sqrt(f4 * f4 + f5 * f5), 1.0E-4F);
            this.segmentDirX[i6] = f4 / f6;
            this.segmentDirY[i6] = f5 / f6;
        }

        for (int i8 = 0; i8 < i; i8++) {
            int i9 = Math.min(i8 * i2 / i, i2 - 1);
            float f7 = MathHelper.clamp((f - i9 * 0.55F * f3) / f3, 0.0F, 1.0F);
            float f8 = f7 * f7 * (3.0F - 2.0F * f7);
            float f9 = (1.0F - f8) * f2;
            this.pathX[i8] = this.pathX[i8] + this.segmentDirX[i9] * f9;
            this.pathY[i8] = this.pathY[i8] + this.segmentDirY[i9] * f9;
            this.pathIntensity[i8] = this.pathIntensity[i8] * f8;
        }
    }

    private void updateEmbers(float f, double d, double d2, double d3, float f2, float f3) {
        if (this.embers == null) {
            this.embers = new TargetEspRenderer.Ember[160];
            int i = 0;

            for (int i2 = this.embers.length; i < i2; i++) {
                this.embers[i] = new TargetEspRenderer.Ember();
            }
        }

        if (this.target != null && this.emberTargetId != this.target.getId()) {
            this.emberCount = 0;
            this.emberSpawnAccumulator = 0.0F;
            this.emberTargetId = this.target.getId();
        }

        float f4 = this.sunlightTime;
        float f5 = Math.max(0.0F, 1.0F - 1.1F * f);
        int i3 = 0;

        while (i3 < this.emberCount) {
            TargetEspRenderer.Ember ember = this.embers[i3];
            ember.age += f;
            if (ember.age >= ember.lifetime) {
                this.emberCount--;
                this.embers[i3] = this.embers[this.emberCount];
                this.embers[this.emberCount] = ember;
            } else {
                ember.velocityY += 0.42F * f;
                float f6 = (float)(Math.sin(ember.y * 3.7 + f4 * 0.9 + ember.phase) * Math.cos(ember.z * 2.9 - f4 * 0.6));
                float f7 = (float)(Math.cos(ember.x * 3.1 - f4 * 0.7 + ember.phase) * Math.sin(ember.y * 4.1 + f4 * 0.5));
                ember.velocityX = (ember.velocityX + f6 * 0.34F * f) * f5;
                ember.velocityY *= f5;
                ember.velocityZ = (ember.velocityZ + f7 * 0.34F * f) * f5;
                ember.x = ember.x + ember.velocityX * f;
                ember.y = ember.y + ember.velocityY * f;
                ember.z = ember.z + ember.velocityZ * f;
                i3++;
            }
        }

        this.emberSpawnAccumulator = Math.min(this.emberSpawnAccumulator + f * 58.0F * f3, 5.0F);

        while (this.emberSpawnAccumulator >= 1.0F && this.emberCount < this.embers.length) {
            this.emberSpawnAccumulator--;
            TargetEspRenderer.Ember ember2 = this.embers[this.emberCount++];
            int i4 = ++this.emberSpawnCounter;
            ember2.x = d + hashNoise(i4, 1, 0) * f2 * 1.4F;
            ember2.y = d2 + hashNoise(i4, 2, 0) * f2 * 0.9F - f2 * 0.25F;
            ember2.z = d3 + hashNoise(i4, 3, 0) * f2 * 1.4F;
            ember2.velocityX = hashNoise(i4, 4, 0) * 0.11F;
            ember2.velocityY = 0.06F + 0.08F * Math.abs(hashNoise(i4, 5, 0));
            ember2.velocityZ = hashNoise(i4, 6, 0) * 0.11F;
            ember2.age = 0.0F;
            ember2.lifetime = 1.5F + 1.3F * Math.abs(hashNoise(i4, 7, 0));
            ember2.size = f2 * (0.026F + 0.024F * Math.abs(hashNoise(i4, 8, 0)));
            ember2.phase = hashNoise(i4, 9, 0) * 3.14159F;
            ember2.flickerSpeed = 7.0F + 6.0F * Math.abs(hashNoise(i4, 10, 0));
        }
    }

    private void buildEmbers(BufferBuilder bufferBuilder, Matrix4f matrix4f, Quaternionf quaternionf, Vec3d vec3d, int i, float f, float f2) {
        for (int i2 = 0; i2 < this.emberCount; i2++) {
            TargetEspRenderer.Ember ember = this.embers[i2];
            if (ProjectionUtil.isVisible(ember.x, ember.y, ember.z)) {
                float f3 = ember.age / ember.lifetime;
                float f4 = 1.0F - f3;
                float f5 = Math.min(f3 / 0.1F, 1.0F) * f4 * f4 * f4;
                float f6 = 0.55F + 0.45F * MathHelper.sin(this.sunlightTime * ember.flickerSpeed + ember.phase);
                float f7 = MathHelper.clamp(f5 * f6 * f * 0.85F * f2, 0.0F, 1.0F);
                if (!(f7 < 0.004F)) {
                    this.emitRotatedQuad(
                        bufferBuilder,
                        matrix4f,
                        quaternionf,
                        (float)(ember.x - vec3d.x),
                        (float)(ember.y - vec3d.y),
                        (float)(ember.z - vec3d.z),
                        0.0F,
                        0.0F,
                        ember.size * f2,
                        ember.size * f2,
                        ember.phase,
                        ColorUtil.multiplyAlpha(i, f7)
                    );
                }
            }
        }
    }

    private void startShatter(RenderEvent renderEvent, AttackAura attackAura) {
        int i = this.infinityPointCount;
        int i2 = this.cornerPathStart[2];
        int i3 = (this.cornerPathStart[5] - i2 + i) % i;

        for (int i4 = 0; i4 < i; i4++) {
            int i5 = (i2 + i4) % i;
            this.shatterPathX[i4] = this.pathX[i5];
            this.shatterPathY[i4] = this.pathY[i5];
            this.shatterNormalX[i4] = this.pathNormalX[i5];
            this.shatterNormalY[i4] = this.pathNormalY[i5];
        }

        float f = renderEvent.getTickDelta();
        this.shatterX = MathHelper.lerp(f, this.target.lastX, this.target.getX());
        this.shatterY = MathHelper.lerp(f, this.target.lastY, this.target.getY()) + this.target.getHeight() * 0.62F;
        this.shatterZ = MathHelper.lerp(f, this.target.lastZ, this.target.getZ());
        this.shatterColor = attackAura.getEspColorValue();
        this.shatteredTargetId = this.target.getId();
        this.shatterTime = 0.0F;
        this.shatterLastNanos = 0L;
        Vec3d vec3d = new Vec3d(this.shatterX, this.shatterY, this.shatterZ);
        BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(vec3d, vec3d.add(0.0, -24.0, 0.0), ShapeType.COLLIDER, FluidHandling.NONE, this.target));
        this.shatterGroundY = blockHitResult.getType() == Type.MISS ? this.shatterY - 24.0 : blockHitResult.getPos().y;
        this.shatterRight.set(1.0F, 0.0F, 0.0F).rotate(renderEvent.getCamera().getRotation());
        this.initShard(this.shards[0], 0, i3, -1.0F, -0.15F, -2.4F);
        this.initShard(this.shards[1], i3, i, 1.0F, 0.75F, 2.9F);
    }

    private void initShard(TargetEspRenderer.Shard shard, int i, int i2, float f, float f2, float f3) {
        shard.startIndex = i;
        shard.endIndex = i2;
        float f4 = 0.0F;
        float f5 = 0.0F;

        for (int i3 = i; i3 < i2; i3++) {
            f4 += this.shatterPathX[i3];
            f5 += this.shatterPathY[i3];
        }

        int i4 = Math.max(1, i2 - i);
        shard.centerX = f4 / i4;
        shard.centerY = f5 / i4;
        shard.offsetX = 0.0F;
        shard.offsetY = 0.0F;
        shard.offsetZ = 0.0F;
        shard.velocityX = this.shatterRight.x * f * 0.9F;
        shard.velocityY = this.shatterRight.y * f * 0.9F + f2;
        shard.velocityZ = this.shatterRight.z * f * 0.9F;
        shard.angle = 0.0F;
        shard.angularVelocity = f3;
        shard.grounded = false;
    }

    private void renderShatter(RenderEvent renderEvent) {
        long l = System.nanoTime();
        float f = this.shatterLastNanos == 0L ? 0.016666668F : Math.min((float)(l - this.shatterLastNanos) / 1.0E9F, 0.05F);
        this.shatterLastNanos = l;
        this.shatterTime += f;
        if (!(this.shatterTime >= 1.8F)) {
            TargetEspRenderer.Shard[] shards2 = this.shards;

            for (TargetEspRenderer.Shard shard : shards2) {
                shard.velocityY -= 7.0F * f;
                shard.offsetX = shard.offsetX + shard.velocityX * f;
                shard.offsetY = shard.offsetY + shard.velocityY * f;
                shard.offsetZ = shard.offsetZ + shard.velocityZ * f;
                shard.angle = shard.angle + shard.angularVelocity * f;
                float f2 = MathHelper.cos(shard.angle);
                float f3 = MathHelper.sin(shard.angle);
                float f4 = Float.MAX_VALUE;

                for (int i = shard.startIndex; i < shard.endIndex; i++) {
                    float f5 = this.shatterPathX[i] - shard.centerX;
                    float f6 = this.shatterPathY[i] - shard.centerY;
                    f4 = Math.min(f4, f5 * f3 + f6 * f2 + shard.centerY);
                }

                double d = this.shatterY + shard.offsetY + f4;
                if (d < this.shatterGroundY) {
                    shard.grounded = true;
                    shard.offsetY = shard.offsetY + (float)(this.shatterGroundY - d);
                    if (shard.velocityY < -1.2F) {
                        shard.velocityY = -shard.velocityY * 0.34F;
                        shard.angularVelocity *= -0.4F;
                    } else {
                        shard.velocityY = 0.0F;
                    }
                }

                if (shard.grounded) {
                    float f7 = Math.max(0.0F, 1.0F - 7.0F * f);
                    shard.velocityX *= f7;
                    shard.velocityZ *= f7;
                    shard.angularVelocity *= f7;
                }
            }

            float f8 = this.shatterTime / 1.8F;
            float f9 = 1.0F + 1.6F * (float)Math.exp(-this.shatterTime * 9.0F);
            float f10 = (1.0F - f8 * f8) * f9;
            float f11 = Math.max(0.0F, 1.0F - f8 * 1.6F);
            int i2 = ColorUtil.multiplyAlpha(this.shatterColor, MathHelper.clamp(f10 * 0.8F, 0.0F, 1.0F));
            Vec3d vec3d = renderEvent.getCamera().getCameraPos();
            this.billboardRotation.set(renderEvent.getCamera().getRotation());
            this.billboardRight.set(1.0F, 0.0F, 0.0F).rotate(this.billboardRotation);
            this.billboardUp.set(0.0F, 1.0F, 0.0F).rotate(this.billboardRotation);
            Matrix4f matrix4f = renderEvent.getMatrices().peek().getPositionMatrix();
            float f12 = (float)(this.shatterX - vec3d.x);
            float f13 = (float)(this.shatterY - vec3d.y);
            float f14 = (float)(this.shatterZ - vec3d.z);
            float f15 = ProjectionUtil.getScaledSize(0.05F, Math.sqrt(f12 * f12 + f13 * f13 + f14 * f14));
            float f16 = MathHelper.clamp(f10 * 0.7F, 0.0F, 1.0F);

            try (BufferAllocator bufferAllocator = new BufferAllocator(16384)) {
                BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                for (TargetEspRenderer.Shard shard2 : shards2) {
                    this.buildShard(bufferBuilder, matrix4f, shard2, f12, f13, f14, 0.0F, i2);
                }

                BuiltBuffer builtBuffer = bufferBuilder.endNullable();
                if (builtBuffer != null) {
                    ParticleBlurRenderer.INSTANCE
                        .render(f15, f16, () -> MeshDrawUtil.drawLabeled(Pipelines.NEON_TUBE_DEPTH, builtBuffer, "sadnes aura infinity shard wash"));
                }
            }

            try (BufferAllocator bufferAllocator2 = new BufferAllocator(16384)) {
                BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                for (TargetEspRenderer.Shard shard3 : shards2) {
                    this.buildShard(bufferBuilder2, matrix4f, shard3, f12, f13, f14, f11, i2);
                }

                BuiltBuffer builtBuffer2 = bufferBuilder2.endNullable();
                if (builtBuffer2 != null) {
                    MeshDrawUtil.drawLabeled(Pipelines.NEON_TUBE_DEPTH, builtBuffer2, "sadnes aura infinity shard tube");
                }
            }
        }
    }

    private void buildShard(BufferBuilder bufferBuilder, Matrix4f matrix4f, TargetEspRenderer.Shard shard, float f, float f2, float f3, float f4, int i) {
        float f5 = MathHelper.cos(shard.angle);
        float f6 = MathHelper.sin(shard.angle);
        float f7 = f + shard.offsetX;
        float f8 = f2 + shard.offsetY;
        float f9 = f3 + shard.offsetZ;

        for (int i2 = shard.startIndex; i2 + 1 < shard.endIndex; i2++) {
            this.emitShardVertex(bufferBuilder, matrix4f, f7, f8, f9, shard, f5, f6, i2, 1.0F, f4, i);
            this.emitShardVertex(bufferBuilder, matrix4f, f7, f8, f9, shard, f5, f6, i2, -1.0F, f4, i);
            this.emitShardVertex(bufferBuilder, matrix4f, f7, f8, f9, shard, f5, f6, i2 + 1, -1.0F, f4, i);
            this.emitShardVertex(bufferBuilder, matrix4f, f7, f8, f9, shard, f5, f6, i2 + 1, 1.0F, f4, i);
        }
    }

    private void emitShardVertex(
        BufferBuilder bufferBuilder,
        Matrix4f matrix4f,
        float f,
        float f2,
        float f3,
        TargetEspRenderer.Shard shard,
        float f4,
        float f5,
        int i,
        float f6,
        float f7,
        int i2
    ) {
        float f8 = this.shatterPathX[i] + this.shatterNormalX[i] * f6 - shard.centerX;
        float f9 = this.shatterPathY[i] + this.shatterNormalY[i] * f6 - shard.centerY;
        this.emitBillboardVertex(
            bufferBuilder, matrix4f, f, f2, f3, f8 * f4 - f9 * f5 + shard.centerX, f8 * f5 + f9 * f4 + shard.centerY, f6, f7, i2
        );
    }

    private void emitRotatedQuad(
        BufferBuilder bufferBuilder,
        Matrix4f matrix4f,
        Quaternionf quaternionf,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        float f7,
        float f8,
        int i
    ) {
        this.quadRotation.set(quaternionf).rotateZ(f8);
        this.quadCorner0.set(f4 - f6, f5 - f7, 0.0F).rotate(this.quadRotation).add(f, f2, f3);
        this.quadCorner1.set(f4 - f6, f5 + f7, 0.0F).rotate(this.quadRotation).add(f, f2, f3);
        this.quadCorner2.set(f4 + f6, f5 + f7, 0.0F).rotate(this.quadRotation).add(f, f2, f3);
        this.quadCorner3.set(f4 + f6, f5 - f7, 0.0F).rotate(this.quadRotation).add(f, f2, f3);
        bufferBuilder.vertex(matrix4f, this.quadCorner0.x, this.quadCorner0.y, this.quadCorner0.z).texture(0.0F, 1.0F).color(i);
        bufferBuilder.vertex(matrix4f, this.quadCorner1.x, this.quadCorner1.y, this.quadCorner1.z).texture(0.0F, 0.0F).color(i);
        bufferBuilder.vertex(matrix4f, this.quadCorner2.x, this.quadCorner2.y, this.quadCorner2.z).texture(1.0F, 0.0F).color(i);
        bufferBuilder.vertex(matrix4f, this.quadCorner3.x, this.quadCorner3.y, this.quadCorner3.z).texture(1.0F, 1.0F).color(i);
    }

    private void drawTexturedQuad(
        MatrixStack matrixStack, Identifier identifier, RenderPipeline renderPipeline, Quaternionf quaternionf, float f, float f2, float f3, float f4, float f5, int i
    ) {
        try (BufferAllocator bufferAllocator = new BufferAllocator(512)) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            this.buildGradientQuad(matrixStack, bufferBuilder, quaternionf, f, f2, f3, f4, f5, i, i, i, i);
            BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer == null) {
                return;
            }

            this.drawMesh(identifier, renderPipeline, builtBuffer);
        }
    }

    private void drawGradientQuad(
        MatrixStack matrixStack,
        Identifier identifier,
        RenderPipeline renderPipeline,
        Quaternionf quaternionf,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        int i,
        int i2,
        int i3,
        int i4
    ) {
        try (BufferAllocator bufferAllocator = new BufferAllocator(512)) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            this.buildGradientQuad(matrixStack, bufferBuilder, quaternionf, f, f2, f3, f4, f5, i, i2, i3, i4);
            BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer == null) {
                return;
            }

            this.drawMesh(identifier, renderPipeline, builtBuffer);
        }
    }

    private void buildGradientQuad(
        MatrixStack matrixStack,
        VertexConsumer vertexConsumer,
        Quaternionf quaternionf,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        int i,
        int i2,
        int i3,
        int i4
    ) {
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        Quaternionf quaternionf2 = new Quaternionf(quaternionf).rotateZ((float)Math.toRadians(f5));
        float f6 = f4 / 2.0F;
        Vector3f vector3f = new Vector3f(-f6, -f6, 0.0F).rotate(quaternionf2).add(f, f2, f3);
        Vector3f vector3f2 = new Vector3f(-f6, f6, 0.0F).rotate(quaternionf2).add(f, f2, f3);
        Vector3f vector3f3 = new Vector3f(f6, f6, 0.0F).rotate(quaternionf2).add(f, f2, f3);
        Vector3f vector3f4 = new Vector3f(f6, -f6, 0.0F).rotate(quaternionf2).add(f, f2, f3);
        vertexConsumer.vertex(matrix4f, vector3f.x, vector3f.y, vector3f.z).texture(0.0F, 1.0F).color(i);
        vertexConsumer.vertex(matrix4f, vector3f2.x, vector3f2.y, vector3f2.z).texture(0.0F, 0.0F).color(i2);
        vertexConsumer.vertex(matrix4f, vector3f3.x, vector3f3.y, vector3f3.z).texture(1.0F, 0.0F).color(i3);
        vertexConsumer.vertex(matrix4f, vector3f4.x, vector3f4.y, vector3f4.z).texture(1.0F, 1.0F).color(i4);
    }

    private void drawMesh(Identifier identifier, RenderPipeline renderPipeline, BuiltBuffer builtBuffer) {
        MeshDrawUtil.drawLabeledTextured(renderPipeline, builtBuffer, "sadnes aura setup draw", identifier);
    }

    private void buildQuad(MatrixStack matrixStack, VertexConsumer vertexConsumer, Quaternionf quaternionf, float f, float f2, float f3, float f4, float f5, int i) {
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        Quaternionf quaternionf2 = new Quaternionf(quaternionf).rotateZ((float)Math.toRadians(f5));
        float f6 = f4 / 2.0F;
        Vector3f vector3f = new Vector3f(-f6, -f6, 0.0F).rotate(quaternionf2).add(f, f2, f3);
        Vector3f vector3f2 = new Vector3f(-f6, f6, 0.0F).rotate(quaternionf2).add(f, f2, f3);
        Vector3f vector3f3 = new Vector3f(f6, f6, 0.0F).rotate(quaternionf2).add(f, f2, f3);
        Vector3f vector3f4 = new Vector3f(f6, -f6, 0.0F).rotate(quaternionf2).add(f, f2, f3);
        vertexConsumer.vertex(matrix4f, vector3f.x, vector3f.y, vector3f.z).texture(0.0F, 1.0F).color(i);
        vertexConsumer.vertex(matrix4f, vector3f2.x, vector3f2.y, vector3f2.z).texture(0.0F, 0.0F).color(i);
        vertexConsumer.vertex(matrix4f, vector3f3.x, vector3f3.y, vector3f3.z).texture(1.0F, 0.0F).color(i);
        vertexConsumer.vertex(matrix4f, vector3f4.x, vector3f4.y, vector3f4.z).texture(1.0F, 1.0F).color(i);
    }

    static final class Ember {
        double x;
        double y;
        double z;
        float velocityX;
        float velocityY;
        float velocityZ;
        float age;
        float lifetime;
        float size;
        float phase;
        float flickerSpeed;
    }

    static final class Shard {
        int startIndex;
        int endIndex;
        float centerX;
        float centerY;
        float offsetX;
        float offsetY;
        float offsetZ;
        float velocityX;
        float velocityY;
        float velocityZ;
        float angle;
        float angularVelocity;
        boolean grounded;
    }

    static final class SmokePuff {
        double x;
        double y;
        double z;
        float velocityX;
        float velocityY;
        float velocityZ;
        float age;
        float lifetime;
        float size;
        float growth;
        float rotation;
        float rotationSpeed;
        float stretch;
        float phase;
    }
}
