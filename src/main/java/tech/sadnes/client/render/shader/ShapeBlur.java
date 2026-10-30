package tech.sadnes.client.render.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.Window;
import net.minecraft.util.Util;
import tech.sadnes.client.util.math.FixedRateLimiter;
import tech.sadnes.client.util.math.StopWatch;

public enum ShapeBlur {
    INSTANCE;

    private static final int LEVEL_COUNT = 3;
    public static int downscale = 2;
    private static final BlurLevel[] LEVELS = new BlurLevel[]{new BlurLevel(1.1F, 2), new BlurLevel(1.0F, 3), new BlurLevel(1.4F, 4)};
    private static final long FRAME_INTERVAL_60_NS = 16666666L;
    private static final long FRAME_INTERVAL_144_NS = 6944444L;
    private final SimpleFramebuffer[] levelTargets = new SimpleFramebuffer[3];
    private final StopWatch[] levelUsageTimers = Stream.generate(StopWatch::new).limit(3L).toArray(StopWatch[]::new);
    private final boolean[] levelActive = new boolean[3];
    private SimpleFramebuffer[] mipTargets = new SimpleFramebuffer[0];
    private final List<SimpleFramebuffer> regionTargets = new ArrayList<>();
    private final List<GpuTextureView> regionViews = new ArrayList<>();
    private final List<Integer> regionLevels = new ArrayList<>();
    private final List<float[]> regionBounds = new ArrayList<>();
    private final List<ShapeBlur.RegionSnapshot> regionSnapshots = new ArrayList<>();
    private int regionCount = 0;
    private SimpleFramebuffer clampTarget;
    private final FixedRateLimiter limiter = new FixedRateLimiter();

    public void reset() {
        for (int i = 0; i < 3; i++) {
            deleteTarget(this.levelTargets[i]);
            this.levelTargets[i] = null;
            this.levelActive[i] = false;
        }

        SimpleFramebuffer[] simpleFramebuffers = this.mipTargets;
        int i2 = simpleFramebuffers.length;

        for (int i3 = 0; i3 < i2; i3++) {
            deleteTarget(simpleFramebuffers[i3]);
        }

        this.mipTargets = new SimpleFramebuffer[0];
        deleteTarget(this.clampTarget);
        this.clampTarget = null;
        Iterator iterator2 = this.regionTargets.iterator();

        while (iterator2.hasNext()) {
            deleteTarget((SimpleFramebuffer)iterator2.next());
        }

        this.regionTargets.clear();
        this.regionViews.clear();
        this.regionLevels.clear();
        this.regionBounds.clear();
        this.regionSnapshots.clear();
        this.regionCount = 0;
    }

    public void beginFrame() {
        this.regionCount = 0;
    }

    public void update() {
        this.limiter.run(60, this::updateBlurLevels);
    }

    private void updateBlurLevels() {
        Framebuffer framebuffer = getMainFramebuffer();
        if (framebuffer != null) {
            boolean flag = false;

            for (int i = 0; i < 3; i++) {
                this.levelActive[i] = !this.levelUsageTimers[i].hasPassed(250.0);
                flag |= this.levelActive[i];
            }

            if (flag) {
                Window window = MinecraftClient.getInstance().getWindow();
                int i2 = Math.max(1, window.getFramebufferWidth() / downscale);
                int i3 = Math.max(1, window.getFramebufferHeight() / downscale);
                GpuTextureView gpuTextureView = framebuffer.getColorAttachmentView();

                try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
                    for (int i4 = 0; i4 < 3; i4++) {
                        if (this.levelActive[i4]) {
                            this.levelTargets[i4] = ensureTarget(this.levelTargets[i4], "sadnes_shape_blur_l" + i4, i2, i3);
                            this.blur(gpuTextureView, true, getPassCount(i4), LEVELS[i4].offset(), LEVELS[i4].offset() * downscale, this.levelTargets[i4]);
                        }
                    }
                }
            }
        }
    }

    private static int getPassCount(int i) {
        int i2 = 31 - Integer.numberOfLeadingZeros(Math.max(1, downscale));
        return Math.max(1, LEVELS[i].depth() + 1 - i2);
    }

    private void blur(GpuTextureView gpuTextureView, boolean flag, int i, float f, float f2, SimpleFramebuffer simpleFramebuffer) {
        this.ensureMipTargets(i, simpleFramebuffer.textureWidth, simpleFramebuffer.textureHeight);
        GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        GpuTextureView gpuTextureView2 = gpuTextureView;

        for (int i2 = 0; i2 < i; i2++) {
            if (i2 == 0 && flag) {
                FullscreenPass.blitUnflipped(Pipelines.BLUR_DOWN, this.mipTargets[i2], gpuTextureView2, gpuSampler, f2);
            } else {
                FullscreenPass.blit(Pipelines.BLUR_DOWN, this.mipTargets[i2], gpuTextureView2, gpuSampler, i2 == 0 ? f2 : f);
            }

            gpuTextureView2 = this.mipTargets[i2].getColorAttachmentView();
        }

        for (int i3 = i - 2; i3 >= 0; i3--) {
            FullscreenPass.blit(Pipelines.BLUR_UP, this.mipTargets[i3], this.mipTargets[i3 + 1].getColorAttachmentView(), gpuSampler, f);
        }

        FullscreenPass.blit(Pipelines.BLUR_UP, simpleFramebuffer, this.mipTargets[0].getColorAttachmentView(), gpuSampler, f);
    }

    private void ensureMipTargets(int i, int i2, int i3) {
        if (this.mipTargets.length < i) {
            this.mipTargets = Arrays.copyOf(this.mipTargets, i);
        }

        for (int i4 = 0; i4 < i; i4++) {
            this.mipTargets[i4] = ensureTarget(
                this.mipTargets[i4], "sadnes_shape_blur_mip" + i4, Math.max(1, i2 >> i4 + 1), Math.max(1, i3 >> i4 + 1)
            );
        }
    }

    public int allocateRegion(int i, float f, float f2, float f3, float f4, float f5) {
        Window window = MinecraftClient.getInstance().getWindow();
        int i2 = Math.max(1, window.getFramebufferWidth() / downscale);
        int i3 = Math.max(1, window.getFramebufferHeight() / downscale);
        int i4 = this.regionCount++;

        while (this.regionTargets.size() <= i4) {
            this.regionTargets.add(new SimpleFramebuffer("sadnes_shape_blur_2d_res_" + this.regionTargets.size(), i2, i3, false));
            this.regionViews.add(null);
            this.regionLevels.add(0);
            this.regionBounds.add(null);
            this.regionSnapshots.add(null);
        }

        SimpleFramebuffer simpleFramebuffer = this.regionTargets.get(i4);
        if (simpleFramebuffer.textureWidth != i2 || simpleFramebuffer.textureHeight != i3) {
            simpleFramebuffer.resize(i2, i3);
            this.regionSnapshots.set(i4, null);
        }

        this.regionViews.set(i4, simpleFramebuffer.getColorAttachmentView());
        this.regionLevels.set(i4, clampLevel(i));
        this.regionBounds.set(i4, new float[]{f, f2, f3, f4, f5});
        return i4;
    }

    private TextureSetup getRegionTexture(int i) {
        if (i >= 0 && i < this.regionViews.size()) {
            GpuTextureView gpuTextureView = this.regionViews.get(i);
            if (gpuTextureView != null) {
                if (gpuTextureView.isClosed()) {
                    return null;
                }

                return TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            }
        }

        return null;
    }

    public int findRegion(GpuTextureView gpuTextureView) {
        if (gpuTextureView == null) {
            return -1;
        }

        for (int i = 0; i < this.regionViews.size(); i++) {
            if (this.regionViews.get(i) == gpuTextureView) {
                return i;
            }
        }

        return -1;
    }

    public boolean needsRegionUpdate(int i) {
        return i >= 0 && i < this.regionTargets.size() ? this.isRegionStale(i, Util.getMeasuringTimeNano()) : false;
    }

    private boolean isRegionStale(int i, long l) {
        ShapeBlur.RegionSnapshot regionSnapshot = this.regionSnapshots.get(i);
        if (regionSnapshot == null) {
            return true;
        } else {
            float[] floats = this.regionBounds.get(i);
            if (regionSnapshot.level() == this.regionLevels.get(i) && !boundsChangedSignificantly(regionSnapshot.bounds(), floats)) {
                boolean flag = !Arrays.equals(regionSnapshot.bounds(), floats);
                return l - regionSnapshot.timeNs() >= (flag ? 6944444L : 16666666L);
            } else {
                return true;
            }
        }
    }

    private static boolean boundsChangedSignificantly(float[] floats, float[] floats2) {
        if (floats != null && floats2 != null) {
            float f = Math.max(0.0F, Math.min(floats[2], floats2[2]) - Math.max(floats[0], floats2[0]));
            float f2 = Math.max(0.0F, Math.min(floats[3], floats2[3]) - Math.max(floats[1], floats2[1]));
            float f3 = f * f2;
            float f4 = Math.max(0.0F, floats[2] - floats[0]) * Math.max(0.0F, floats[3] - floats[1]);
            float f5 = Math.max(0.0F, floats2[2] - floats2[0]) * Math.max(0.0F, floats2[3] - floats2[1]);
            float f6 = f4 + f5 - f3;
            return f6 > 1.0E-9F && f3 / f6 < 0.75F;
        } else {
            return floats != floats2;
        }
    }

    public void renderRegion(int i, Framebuffer framebuffer) {
        if (i >= 0 && i < this.regionTargets.size()) {
            if (framebuffer != null && framebuffer.getColorAttachment() != null && framebuffer.getColorAttachmentView() != null && !framebuffer.getColorAttachmentView().isClosed()) {
                SimpleFramebuffer simpleFramebuffer = this.regionTargets.get(i);
                if (simpleFramebuffer != null) {
                    long l = Util.getMeasuringTimeNano();
                    if (this.isRegionStale(i, l)) {
                        float[] floats = this.regionBounds.get(i);
                        int i2 = this.regionLevels.get(i);
                        this.blurRegion(framebuffer, simpleFramebuffer, floats, i2);
                        this.regionSnapshots.set(i, new ShapeBlur.RegionSnapshot(l, i2, floats));
                    }
                }
            }
        }
    }

    private void blurRegion(Framebuffer framebuffer, SimpleFramebuffer simpleFramebuffer, float[] floats, int i) {
        BlurLevel blurLevel = LEVELS[i];
        GpuTextureView gpuTextureView = framebuffer.getColorAttachmentView();
        boolean flag = true;
        float f = blurLevel.offset() * downscale;

        try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
            if (floats != null && floats[2] > floats[0] && floats[3] > floats[1]) {
                this.clampTarget = ensureTarget(this.clampTarget, "sadnes_shape_blur_clamp", simpleFramebuffer.textureWidth, simpleFramebuffer.textureHeight);
                GpuBufferSlice gpuBufferSlice = BlurRectUniformStorage.write(new BlurRectUniforms(floats[0], floats[1], floats[2], floats[3], floats[4]));
                FullscreenPass.blitWithUniformsUnflipped(
                    Pipelines.CLAMP_COPY, this.clampTarget, gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR), gpuBufferSlice
                );
                gpuTextureView = this.clampTarget.getColorAttachmentView();
                flag = false;
                f = blurLevel.offset();
            }

            this.blur(gpuTextureView, flag, getPassCount(i), blurLevel.offset(), f, simpleFramebuffer);
        }
    }

    public TextureSetup getBlurTexture(int i, boolean flag, float f, float f2, float f3, float f4, float f5) {
        int i2 = clampLevel(i);
        if (flag) {
            int i3 = this.allocateRegion(i2, f, f2, f3, f4, f5);
            TextureSetup textureSetup = this.getRegionTexture(i3);
            if (textureSetup != null) {
                return textureSetup;
            }
        }

        this.levelUsageTimers[i2].reset();
        if (!this.levelActive[i2]) {
            return null;
        }

        SimpleFramebuffer simpleFramebuffer = this.levelTargets[i2];
        return simpleFramebuffer != null && simpleFramebuffer.getColorAttachmentView() != null && !simpleFramebuffer.getColorAttachmentView().isClosed()
            ? TextureSetup.of(simpleFramebuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR))
            : null;
    }

    private static int clampLevel(int i) {
        if (i < 0) {
            return 0;
        } else {
            return i >= 3 ? 2 : i;
        }
    }

    private static Framebuffer getMainFramebuffer() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null) {
            return null;
        }

        Framebuffer framebuffer = minecraftClient.getFramebuffer();
        if (framebuffer != null && framebuffer.getColorAttachment() != null) {
            if (framebuffer.getColorAttachment().getFormat() != TextureFormat.RGBA8) {
                return null;
            }

            if (framebuffer.getColorAttachmentView() != null) {
                if (framebuffer.getColorAttachmentView().isClosed()) {
                    return null;
                }

                return framebuffer;
            }
        }

        return null;
    }

    private static SimpleFramebuffer ensureTarget(SimpleFramebuffer simpleFramebuffer, String string, int i, int i2) {
        if (simpleFramebuffer == null) {
            return new SimpleFramebuffer(string, i, i2, false);
        }

        if (simpleFramebuffer.textureWidth != i || simpleFramebuffer.textureHeight != i2) {
            simpleFramebuffer.resize(i, i2);
        }

        return simpleFramebuffer;
    }

    private static void deleteTarget(SimpleFramebuffer simpleFramebuffer) {
        if (simpleFramebuffer != null) {
            simpleFramebuffer.delete();
        }
    }

    record RegionSnapshot(long timeNs, int level, float[] bounds) {
    }
}
