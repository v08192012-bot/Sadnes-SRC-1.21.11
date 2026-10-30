package tech.sadnes.client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tech.sadnes.client.feature.impl.render.MotionBlur;
import tech.sadnes.client.render.shader.AmbienceRenderer;
import tech.sadnes.client.render.shader.BlurRectUniformStorage;
import tech.sadnes.client.render.shader.BlurRectUniforms;
import tech.sadnes.client.render.shader.FullscreenPass;
import tech.sadnes.client.render.shader.GlStateSnapshot;
import tech.sadnes.client.render.shader.MotionBlurUniformStorage;
import tech.sadnes.client.render.shader.MotionBlurUniforms;
import tech.sadnes.client.render.shader.Pipelines;

public enum MotionBlurRenderer {
    INSTANCE;

    private static final boolean ZERO_TO_ONE_DEPTH;
    private final Matrix4f projection = new Matrix4f();
    private final Vector3f velocity = new Vector3f();
    private SimpleFramebuffer captureTarget;
    private long lastTime;
    private ClientWorld lastWorld;
    private Vec3d lastPos;
    private float lastYaw;
    private float lastPitch;
    private float yawSpeed;
    private float pitchSpeed;

    public void setProjection(Matrix4f matrix4f) {
        this.projection.set(matrix4f);
    }

    public void render(GameRenderer gameRenderer) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        MotionBlur motionBlur = MotionBlur.getInstance();
        if (motionBlur != null && motionBlur.isEnabled() && minecraftClient.player != null && minecraftClient.world != null) {
            Framebuffer framebuffer = minecraftClient.getFramebuffer();
            if (AmbienceRenderer.isFramebufferUsable(framebuffer)) {
                Camera camera = gameRenderer.getCamera();
                this.updateVelocity(camera, minecraftClient.world);
                float f = motionBlur.getStrength().getValue() / 60.0F;
                boolean flag = Math.abs(this.yawSpeed) + Math.abs(this.pitchSpeed) > 0.5F;
                if (this.velocity.length() > 0.5F || flag) {
                    GpuBufferSlice gpuBufferSlice = MotionBlurUniformStorage.write(this.buildUniforms(camera, f));
                    this.captureTarget = ensureTarget(this.captureTarget, framebuffer.textureWidth, framebuffer.textureHeight);
                    GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                    GpuSampler gpuSampler2 = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);

                    try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
                        GpuBufferSlice gpuBufferSlice2 = BlurRectUniformStorage.write(new BlurRectUniforms(0.0F, 0.0F, 1.0F, 1.0F, 0.0F));
                        FullscreenPass.blitWithUniforms(Pipelines.CLAMP_COPY, this.captureTarget, framebuffer.getColorAttachmentView(), gpuSampler2, gpuBufferSlice2);
                        FullscreenPass.drawMotionBlur(
                            Pipelines.MOTION_BLUR, framebuffer, this.captureTarget.getColorAttachmentView(), framebuffer.getDepthAttachmentView(), gpuSampler, gpuSampler2, gpuBufferSlice
                        );
                    }
                }
            }
        } else {
            this.reset();
        }
    }

    public void reset() {
        if (this.captureTarget != null) {
            this.captureTarget.delete();
            this.captureTarget = null;
        }

        this.lastTime = 0L;
        this.lastWorld = null;
        this.lastPos = null;
        this.yawSpeed = 0.0F;
        this.pitchSpeed = 0.0F;
        this.velocity.zero();
    }

    private void updateVelocity(Camera camera, ClientWorld clientWorld) {
        long l = System.nanoTime();
        float f = this.lastTime == 0L ? 0.0F : (float)(l - this.lastTime) / 1.0E9F;
        this.lastTime = l;
        Vec3d vec3d = camera.getCameraPos();
        float f2 = camera.getYaw();
        float f3 = camera.getPitch();
        boolean flag = this.lastPos != null && clientWorld == this.lastWorld && f > 0.0F && f < 0.2F && vec3d.squaredDistanceTo(this.lastPos) < 100.0;
        if (flag) {
            float f4 = 1.0F - (float)Math.exp(-f / 0.05F);
            this.yawSpeed = MathHelper.lerp(f4, this.yawSpeed, MathHelper.wrapDegrees(f2 - this.lastYaw) / f);
            this.pitchSpeed = MathHelper.lerp(f4, this.pitchSpeed, (f3 - this.lastPitch) / f);
            Vector3f vector3f = new Vector3f((float)(vec3d.x - this.lastPos.x), (float)(vec3d.y - this.lastPos.y), (float)(vec3d.z - this.lastPos.z)).div(f);
            this.velocity.lerp(vector3f, f4);
        } else {
            this.yawSpeed = 0.0F;
            this.pitchSpeed = 0.0F;
            this.velocity.zero();
        }

        this.lastWorld = clientWorld;
        this.lastPos = vec3d;
        this.lastYaw = f2;
        this.lastPitch = f3;
    }

    private MotionBlurUniforms buildUniforms(Camera camera, float f) {
        Matrix4f matrix4f;
        Matrix4f matrix4f2;
        Vector3f vector3f;
        Vector3f vector3f2;
        Entity entity;
        boolean flag;
        label31: {
            float f2 = camera.getYaw() - this.yawSpeed * f;
            float f3 = MathHelper.clamp(camera.getPitch() - this.pitchSpeed * f, -90.0F, 90.0F);
            matrix4f = viewRotation(camera.getYaw(), camera.getPitch()).invert();
            Vector3f vector3f3 = new Vector3f(this.velocity).mul(f * 0.02F);
            matrix4f2 = viewRotation(f2, f3).translate(vector3f3).mul(matrix4f);
            vector3f = new Vector3f(Float.MAX_VALUE);
            vector3f2 = new Vector3f(-Float.MAX_VALUE);
            entity = camera.getFocusedEntity();
            boolean flag2;
            if (entity != null) {
                if (!camera.isThirdPerson()) {
                    flag = entity.hasVehicle();
                    break label31;
                }

                flag2 = true;
            } else {
                flag2 = false;
            }

            flag = flag2;
        }

        if (flag) {
            Vec3d vec3d = camera.getCameraPos();
            float f4 = camera.getLastTickProgress();

            for (Entity entity2 : new Entity[]{entity, entity.getRootVehicle()}) {
                Vec3d vec3d2 = entity2.getLerpedPos(f4).subtract(vec3d);
                float f5 = entity2.getWidth() * 0.5F + 0.4F;
                vector3f.min(new Vector3f((float)vec3d2.x - f5, (float)vec3d2.y - 0.2F, (float)vec3d2.z - f5));
                vector3f2.max(new Vector3f((float)vec3d2.x + f5, (float)vec3d2.y + entity2.getHeight() + 0.4F, (float)vec3d2.z + f5));
            }
        }

        return new MotionBlurUniforms(
            this.projection,
            new Matrix4f(this.projection).invert(),
            matrix4f2,
            matrix4f,
            vector3f,
            vector3f2,
            flag,
            GlStateSnapshot.isOpenGl() ? 0.0F : 1.0F,
            0.25F,
            ZERO_TO_ONE_DEPTH
        );
    }

    private static Matrix4f viewRotation(float f, float f2) {
        Quaternionf quaternionf = new Quaternionf().rotationYXZ((float) Math.PI - f * (float) (Math.PI / 180.0), -f2 * (float) (Math.PI / 180.0), 0.0F);
        return new Matrix4f().rotation(quaternionf.conjugate());
    }

    private static SimpleFramebuffer ensureTarget(SimpleFramebuffer simpleFramebuffer, int i, int i2) {
        int i3 = Math.max(1, i);
        int i4 = Math.max(1, i2);
        if (simpleFramebuffer == null) {
            return new SimpleFramebuffer("sadnes_motion_blur_capture", i3, i4, false);
        }

        if (simpleFramebuffer.textureWidth != i3 || simpleFramebuffer.textureHeight != i4) {
            simpleFramebuffer.resize(i3, i4);
        }

        return simpleFramebuffer;
    }

    static {
        if (new Matrix4f().perspective(1.0F, 1.0F, 1.0F, 2.0F).m22() > -2.5F) {
            ZERO_TO_ONE_DEPTH = true;
        } else {
            ZERO_TO_ONE_DEPTH = false;
        }
    }
}
