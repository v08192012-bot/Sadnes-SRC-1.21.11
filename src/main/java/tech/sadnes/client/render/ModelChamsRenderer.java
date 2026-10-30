package tech.sadnes.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderSetup.Texture;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState.ShadowPiece;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tech.sadnes.api.mixin.accessor.RenderLayerAccessor;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.render.shader.BlurRectUniformStorage;
import tech.sadnes.client.render.shader.BlurRectUniforms;
import tech.sadnes.client.render.shader.ChamsUniformStorage;
import tech.sadnes.client.render.shader.ChamsUniforms;
import tech.sadnes.client.render.shader.FrameBufferPool;
import tech.sadnes.client.render.shader.FullscreenPass;
import tech.sadnes.client.render.shader.GlStateSnapshot;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.render.shader.ScriptFxUniformStorage;
import tech.sadnes.client.render.shader.ScriptFxUniforms;
import tech.sadnes.client.render.shader.ScriptShader;
import tech.sadnes.client.render.texture.ChamsNoiseTexture;
import tech.sadnes.client.util.color.ColorUtil;

public enum ModelChamsRenderer {
    INSTANCE;

    private static final long START_TIME = System.nanoTime();
    static final Identifier WHITE_TEXTURE = Identifier.of(ClientInfo.MOD_ID, "textures/misc/white.png");
    private ModelChamsRenderer.RenderContext worldContext;
    private ModelChamsRenderer.RenderContext previewContext;
    private SimpleFramebuffer preSceneTarget;
    private SimpleFramebuffer previewScratch;
    private boolean preSceneRequested;
    private boolean preSceneCaptured;
    private final CameraRenderState cameraState = new CameraRenderState();
    private final List<ShadowPiece> savedFeatures = new ArrayList<>();

    public void render(MatrixStack matrixStack, Camera camera, float f, List<ModelChamsRenderer.Request> requests) {
        boolean flag = false;
        boolean flag2 = false;
        boolean flag3 = false;
        int i = requests.size();

        for (int i2 = 0; i2 < i; i2++) {
            ModelChamsRenderer.Request request = (ModelChamsRenderer.Request)requests.get(i2);
            if (request.isVisible()) {
                flag = true;
                if (request.needsScene()) {
                    flag2 = true;
                    if (!request.renderModel()) {
                        flag3 = true;
                    }
                }
            }
        }

        if (flag) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (this.worldContext == null) {
                this.worldContext = new ModelChamsRenderer.RenderContext();
            }

            EntityRenderManager entityRenderManager = minecraftClient.getEntityRenderDispatcher();
            Vec3d vec3d = camera.getCameraPos();
            this.cameraState.orientation = camera.getRotation();

            for (int i3 = 0; i3 < i; i3++) {
                ModelChamsRenderer.Request request2 = (ModelChamsRenderer.Request)requests.get(i3);
                if (request2.isVisible()) {
                    for (Entity entity : request2.entities()) {
                        EntityRenderer entityRenderer = entityRenderManager.getRenderer(entity);
                        if (entityRenderer != null) {
                            EntityRenderState entityRenderState = entityRenderer.getAndUpdateRenderState(entity, f);
                            this.savedFeatures.clear();
                            this.savedFeatures.addAll(entityRenderState.shadowPieces);
                            Text text = entityRenderState.displayName;
                            List list = entityRenderState.leashDatas;
                            int i4 = entityRenderState.outlineColor;
                            boolean flag4 = entityRenderState.onFire;
                            entityRenderState.displayName = null;
                            entityRenderState.outlineColor = 0;
                            entityRenderState.onFire = false;
                            entityRenderState.leashDatas = null;
                            entityRenderState.shadowPieces.clear();
                            Vec3d vec3d2 = ProjectionUtil.getInterpolatedPos(entity, f);
                            this.worldContext
                                .consumers
                                .pushTransform(i3)
                                .translation((float)(vec3d.x - vec3d2.x), (float)(vec3d.y - vec3d2.y), (float)(vec3d.z - vec3d2.z));
                            entityRenderManager.render(entityRenderState, this.cameraState, vec3d2.x - vec3d.x, vec3d2.y - vec3d.y, vec3d2.z - vec3d.z, matrixStack, this.worldContext.commandQueue);
                            this.worldContext.dispatcher.render();
                            entityRenderState.shadowPieces.addAll(this.savedFeatures);
                            entityRenderState.displayName = text;
                            entityRenderState.outlineColor = i4;
                            entityRenderState.onFire = flag4;
                            entityRenderState.leashDatas = list;
                        }
                    }
                }
            }

            this.savedFeatures.clear();
            Framebuffer framebuffer = minecraftClient.getFramebuffer();
            int i5 = Math.max(1, framebuffer.textureWidth);
            int i6 = Math.max(1, framebuffer.textureHeight);
            GpuTexture gpuTexture = null;
            GpuTextureView gpuTextureView = null;
            this.preSceneRequested = flag2;
            if (flag2) {
                boolean flag5 = flag3
                    && this.preSceneCaptured
                    && this.preSceneTarget != null
                    && this.preSceneTarget.textureWidth == i5
                    && this.preSceneTarget.textureHeight == i6;
                if (flag5) {
                    gpuTextureView = this.preSceneTarget.getColorAttachmentView();
                } else {
                    gpuTexture = framebuffer.getColorAttachment();
                }
            }

            this.preSceneCaptured = false;
            this.worldContext.consumers.draw(framebuffer.getColorAttachmentView(), i5, i6, gpuTexture, null, gpuTextureView, requests);
        }
    }

    public void renderPreview(
        EntityRenderState entityRenderState, CameraRenderState cameraRenderState, MatrixStack matrixStack, GpuTexture gpuTexture, GpuTextureView gpuTextureView, int i, int i2, float f, float f2
    ) {
        if (i >>> 24 != 0 && gpuTexture != null && gpuTextureView != null) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (this.previewContext == null) {
                this.previewContext = new ModelChamsRenderer.RenderContext();
            }

            List list = entityRenderState.leashDatas;
            boolean flag = entityRenderState.onFire;
            entityRenderState.onFire = false;
            entityRenderState.leashDatas = null;
            this.previewContext.consumers.pushTransform(0).set(matrixStack.peek().getPositionMatrix()).invert();
            minecraftClient.getEntityRenderDispatcher().render(entityRenderState, cameraRenderState, 0.0, 0.0, 0.0, matrixStack, this.previewContext.commandQueue);
            this.previewContext.dispatcher.render();
            entityRenderState.onFire = flag;
            entityRenderState.leashDatas = list;
            int i3 = gpuTexture.getWidth(0);
            int i4 = gpuTexture.getHeight(0);
            if (GlStateSnapshot.isOpenGl()) {
                this.previewContext
                    .consumers
                    .draw(gpuTextureView, i3, i4, null, gpuTextureView, null, List.of(new ModelChamsRenderer.Request(List.of(), i, i2, f, f2)));
            } else {
                if (this.previewScratch == null) {
                    this.previewScratch = new SimpleFramebuffer("sadnes_chams_preview_scratch", i3, i4, false);
                } else if (this.previewScratch.textureWidth != i3 || this.previewScratch.textureHeight != i4) {
                    this.previewScratch.resize(i3, i4);
                }

                RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.previewScratch.getColorAttachment(), 0);
                this.previewContext
                    .consumers
                    .draw(
                        this.previewScratch.getColorAttachmentView(),
                        i3,
                        i4,
                        null,
                        gpuTextureView,
                        null,
                        List.of(new ModelChamsRenderer.Request(List.of(), i, i2, f, f2))
                    );
                FullscreenPass.composite(
                    Pipelines.CHAMS_PREVIEW_BLIT,
                    gpuTextureView,
                    this.previewScratch.getColorAttachmentView(),
                    RenderSystem.getSamplerCache().get(FilterMode.NEAREST),
                    ColorUtil.WHITE
                );
            }
        }
    }

    public void capturePreScene() {
        if (this.preSceneRequested) {
            this.preSceneRequested = false;
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
            int i = Math.max(1, framebuffer.textureWidth);
            int i2 = Math.max(1, framebuffer.textureHeight);
            if (this.preSceneTarget == null) {
                this.preSceneTarget = new SimpleFramebuffer("sadnes_chams_prescene", i, i2, false);
            } else if (this.preSceneTarget.textureWidth != i || this.preSceneTarget.textureHeight != i2) {
                this.preSceneTarget.resize(i, i2);
            }

            RenderSystem.getDevice()
                .createCommandEncoder()
                .copyTextureToTexture(framebuffer.getColorAttachment(), this.preSceneTarget.getColorAttachment(), 0, 0, 0, 0, 0, i, i2);
            this.preSceneCaptured = true;
        }
    }

    static float getTime() {
        return (float)(System.nanoTime() - START_TIME) / 1.0E9F;
    }

    static final class CapturingConsumers extends Immediate {
        private static final VertexConsumer NOOP_CONSUMER = new VertexConsumer() {
            @Override
            public VertexConsumer vertex(float f, float f2, float f3) {
                return this;
            }

            @Override
            public VertexConsumer color(int i, int i2, int i3, int i4) {
                return this;
            }

            @Override
            public VertexConsumer color(int i5) {
                return this;
            }

            @Override
            public VertexConsumer texture(float f4, float f5) {
                return this;
            }

            @Override
            public VertexConsumer overlay(int i6, int i7) {
                return this;
            }

            @Override
            public VertexConsumer light(int i8, int i9) {
                return this;
            }

            @Override
            public VertexConsumer normal(float f6, float f7, float f8) {
                return this;
            }

            @Override
            public VertexConsumer lineWidth(float f9) {
                return this;
            }
        };
        private final Map<ModelChamsRenderer.CapturingConsumers.BatchKey, BufferBuilder> builders = new LinkedHashMap<>();
        private final List<BufferAllocator> allocators = new ArrayList<>();
        private int allocatorCount = 0;
        private final List<Matrix4f> transforms = new ArrayList<>();
        private int transformCount = 0;
        private final IntArrayList transformRequests = new IntArrayList();
        private int currentTransform = -1;
        private final List<ModelChamsRenderer.CapturingConsumers.DrawCall> drawCalls = new ArrayList<>();
        private final Vector4f colorModulator = new Vector4f();
        private final Vector3f modelOffset = new Vector3f();
        private SimpleFramebuffer sceneTarget;
        private final List<SimpleFramebuffer> frostTargets = new ArrayList<>();
        private SimpleFramebuffer frostScratch;
        private ByteBuffer outlineBuffer;

        CapturingConsumers() {
            super(new BufferAllocator(256), Object2ObjectSortedMaps.emptyMap());
        }

        Matrix4f pushTransform(int i) {
            Matrix4f matrix4f;
            if (this.transformCount < this.transforms.size()) {
                matrix4f = this.transforms.get(this.transformCount);
            } else {
                matrix4f = new Matrix4f();
                this.transforms.add(matrix4f);
            }

            this.transformCount++;
            this.transformRequests.add(i);
            this.currentTransform = this.transformCount - 1;
            return matrix4f;
        }

        @Override
        public VertexConsumer getBuffer(RenderLayer renderLayer) {
            if (this.currentTransform < 0) {
                return NOOP_CONSUMER;
            }

            if (!VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL.equals(renderLayer.getVertexFormat())) {
                return NOOP_CONSUMER;
            }

            Texture texture = ((RenderLayerAccessor)renderLayer).sadnes$getRenderSetup().resolveTextures().get("Sampler0");
            ModelChamsRenderer.CapturingConsumers.BatchKey batchKey = new ModelChamsRenderer.CapturingConsumers.BatchKey(
                texture != null ? texture : getWhiteSampler(), this.currentTransform
            );
            return this.builders.computeIfAbsent(batchKey, batchKey2 -> {
                BufferAllocator bufferAllocator;
                if (this.allocatorCount < this.allocators.size()) {
                    bufferAllocator = this.allocators.get(this.allocatorCount);
                } else {
                    bufferAllocator = new BufferAllocator(65536);
                    this.allocators.add(bufferAllocator);
                }

                this.allocatorCount++;
                return new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            });
        }

        private static Texture getWhiteSampler() {
            AbstractTexture abstractTexture = MinecraftClient.getInstance().getTextureManager().getTexture(ModelChamsRenderer.WHITE_TEXTURE);
            return new Texture(abstractTexture.getGlTextureView(), abstractTexture.getSampler());
        }

        void draw(GpuTextureView gpuTextureView, int i, int i2, GpuTexture gpuTexture, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, List<ModelChamsRenderer.Request> requests) {
            if (this.builders.isEmpty()) {
                this.reset();
            } else {
                if (this.sceneTarget == null) {
                    this.sceneTarget = new SimpleFramebuffer("sadnes_chams_scene", i, i2, true);
                } else if (this.sceneTarget.textureWidth != i || this.sceneTarget.textureHeight != i2) {
                    this.sceneTarget.resize(i, i2);
                }

                CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                commandEncoder.clearDepthTexture(this.sceneTarget.getDepthAttachment(), 1.0);
                GpuTextureView gpuTextureView4 = gpuTextureView3;
                if (gpuTextureView3 == null && gpuTexture != null) {
                    commandEncoder.copyTextureToTexture(gpuTexture, this.sceneTarget.getColorAttachment(), 0, 0, 0, 0, 0, i, i2);
                    gpuTextureView4 = this.sceneTarget.getColorAttachmentView();
                }

                float f = ModelChamsRenderer.getTime();
                int i3 = requests.size();
                GpuBufferSlice[] gpuBufferSlices = new GpuBufferSlice[i3];

                for (int i4 = 0; i4 < i3; i4++) {
                    ModelChamsRenderer.Request request = (ModelChamsRenderer.Request)requests.get(i4);
                    gpuBufferSlices[i4] = request.shader() != null
                        ? ScriptFxUniformStorage.write(request.fx())
                        : ChamsUniformStorage.write(new ChamsUniforms(f, request.speed(), request.scale(), request.style()));
                }

                Matrix4f matrix4f = RenderSystem.getModelViewMatrix();
                int i5 = this.transformCount;
                GpuBufferSlice[] gpuBufferSlices2 = new GpuBufferSlice[i5];

                for (int i6 = 0; i6 < i5; i6++) {
                    int i7 = ((ModelChamsRenderer.Request)requests.get(this.transformRequests.getInt(i6))).tint();
                    this.colorModulator.set(ColorUtil.redf(i7), ColorUtil.greenf(i7), ColorUtil.bluef(i7), ColorUtil.alphaf(i7));
                    gpuBufferSlices2[i6] = RenderSystem.getDynamicUniforms().write(matrix4f, this.colorModulator, this.modelOffset, this.transforms.get(i6));
                }

                boolean flag = false;

                for (Entry entry : this.builders.entrySet()) {
                    BuiltBuffer builtBuffer = ((BufferBuilder)entry.getValue()).endNullable();
                    if (builtBuffer != null) {
                        GpuBuffer gpuBuffer = null;
                        ModelChamsRenderer.Request request2 = (ModelChamsRenderer.Request)requests.get(
                            this.transformRequests.getInt(((ModelChamsRenderer.CapturingConsumers.BatchKey)entry.getKey()).entity())
                        );
                        if (request2.style() == 5) {
                            gpuBuffer = this.buildOutlineBuffer(builtBuffer);
                            flag = true;
                        }

                        GpuBuffer gpuBuffer2 = FrameBufferPool.createBuffer(() -> "sadnes model chams vbo", builtBuffer.getBuffer());
                        this.drawCalls
                            .add(
                                new ModelChamsRenderer.CapturingConsumers.DrawCall(
                                    (ModelChamsRenderer.CapturingConsumers.BatchKey)entry.getKey(), builtBuffer, gpuBuffer2, gpuBuffer, builtBuffer.getDrawParameters().indexCount()
                                )
                            );
                    }
                }

                GpuTextureView gpuTextureView5 = this.sceneTarget.getDepthAttachmentView();
                GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);

                try (FullscreenPass.RenderStateGuard renderStateGuard = FullscreenPass.guardState()) {
                    if (gpuTextureView4 == null && gpuTextureView2 != null) {
                        FullscreenPass.blitWithUniformsUnflipped(
                            Pipelines.CLAMP_COPY,
                            this.sceneTarget,
                            gpuTextureView2,
                            gpuSampler,
                            BlurRectUniformStorage.write(new BlurRectUniforms(0.0F, 0.0F, 1.0F, 1.0F, 0.0F))
                        );
                        gpuTextureView4 = this.sceneTarget.getColorAttachmentView();
                    }

                    if (gpuTextureView4 == null) {
                        gpuTextureView4 = this.sceneTarget.getColorAttachmentView();
                    }

                    GpuTextureView[] gpuTextureViews = new GpuTextureView[i3];
                    boolean[] booleans = new boolean[i3];
                    int i8 = this.drawCalls.size();

                    for (int i9 = 0; i9 < i8; i9++) {
                        booleans[this.transformRequests.getInt(this.drawCalls.get(i9).key().entity())] = true;
                    }

                    int i10 = 0;

                    for (int i11 = 0; i11 < i3; i11++) {
                        if (booleans[i11]) {
                            gpuTextureViews[i11] = ((ModelChamsRenderer.Request)requests.get(i11)).style() == 2
                                ? this.blurFrost(gpuTextureView4, i, i2, ((ModelChamsRenderer.Request)requests.get(i11)).scale(), gpuSampler, i10++)
                                : gpuTextureView4;
                        }
                    }

                    this.renderChamsPass(Pipelines.ENTITY_CHAMS_DEPTH, gpuTextureView, gpuTextureView5, gpuTextureView4, gpuSampler, false, shapeIndexBuffer, requests, gpuBufferSlices, gpuTextureViews, gpuBufferSlices2);
                    this.renderChamsPass(Pipelines.ENTITY_CHAMS, gpuTextureView, gpuTextureView5, gpuTextureView4, gpuSampler, true, shapeIndexBuffer, requests, gpuBufferSlices, gpuTextureViews, gpuBufferSlices2);
                    if (flag) {
                        this.renderOutlinePass(gpuTextureView, gpuTextureView5, shapeIndexBuffer, gpuBufferSlices, gpuBufferSlices2);
                    }
                } finally {
                    int i12 = this.drawCalls.size();

                    for (int i13 = 0; i13 < i12; i13++) {
                        this.drawCalls.get(i13).built().close();
                    }

                    this.reset();
                }
            }
        }

        private void reset() {
            this.drawCalls.clear();
            this.builders.clear();
            this.allocatorCount = 0;
            this.transformCount = 0;
            this.transformRequests.clear();
            this.currentTransform = -1;
        }

        private GpuTextureView blurFrost(GpuTextureView gpuTextureView, int i, int i2, float f, GpuSampler gpuSampler, int i3) {
            int i4 = Math.max(1, i / 4);
            int i5 = Math.max(1, i2 / 4);

            while (this.frostTargets.size() <= i3) {
                this.frostTargets.add(null);
            }

            SimpleFramebuffer simpleFramebuffer = ensureTarget(this.frostTargets.get(i3), "sadnes_chams_frost_" + i3, i4, i5);
            this.frostTargets.set(i3, simpleFramebuffer);
            this.frostScratch = ensureTarget(this.frostScratch, "sadnes_chams_frost_scratch", i4, i5);
            float f2 = 2.0F * f;
            GpuTextureView gpuTextureView2 = gpuTextureView;

            for (int i6 = 0; i6 < 2; i6++) {
                FullscreenPass.blitUnflipped(Pipelines.BLUR_DOWN, this.frostScratch, gpuTextureView2, gpuSampler, i6 == 0 ? f2 : f2 / 4.0F);
                FullscreenPass.blitUnflipped(Pipelines.BLUR_UP, simpleFramebuffer, this.frostScratch.getColorAttachmentView(), gpuSampler, f2 / 4.0F);
                gpuTextureView2 = simpleFramebuffer.getColorAttachmentView();
            }

            return simpleFramebuffer.getColorAttachmentView();
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

        private void renderOutlinePass(GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, ShapeIndexBuffer shapeIndexBuffer, GpuBufferSlice[] gpuBufferSlices, GpuBufferSlice[] gpuBufferSlices2) {
            int i = this.drawCalls.size();
            int i2 = 0;

            for (int i3 = 0; i3 < i; i3++) {
                ModelChamsRenderer.CapturingConsumers.DrawCall drawCall = this.drawCalls.get(i3);
                if (drawCall.outlineVbo() != null) {
                    i2 = Math.max(i2, drawCall.indexCount());
                }
            }

            GpuBuffer gpuBuffer = shapeIndexBuffer.getIndexBuffer(i2);

            try (RenderPass renderPass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "sadnes model chams outline", gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
                renderPass.setPipeline(Pipelines.ENTITY_CHAMS_OUTLINE);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setIndexBuffer(gpuBuffer, shapeIndexBuffer.getIndexType());
                int i4 = -1;

                for (int i5 = 0; i5 < i; i5++) {
                    ModelChamsRenderer.CapturingConsumers.DrawCall drawCall2 = this.drawCalls.get(i5);
                    if (drawCall2.outlineVbo() != null) {
                        int i6 = this.transformRequests.getInt(drawCall2.key().entity());
                        if (i6 != i4) {
                            renderPass.setUniform("SadnesChams", gpuBufferSlices[i6]);
                            i4 = i6;
                        }

                        renderPass.setUniform("DynamicTransforms", gpuBufferSlices2[drawCall2.key().entity()]);
                        renderPass.bindTexture("Sampler0", drawCall2.key().texture().textureView(), drawCall2.key().texture().sampler());
                        renderPass.setVertexBuffer(0, drawCall2.outlineVbo());
                        renderPass.drawIndexed(0, 0, drawCall2.indexCount(), 1);
                    }
                }
            }
        }

        private GpuBuffer buildOutlineBuffer(BuiltBuffer builtBuffer) {
            ByteBuffer byteBuffer = builtBuffer.getBuffer().duplicate().order(ByteOrder.nativeOrder());
            byteBuffer.rewind();
            int i = byteBuffer.remaining();
            if (this.outlineBuffer == null || this.outlineBuffer.capacity() < i) {
                this.outlineBuffer = ByteBuffer.allocateDirect(Integer.highestOneBit(Math.max(i - 1, 1)) * 2).order(ByteOrder.nativeOrder());
            }

            this.outlineBuffer.clear();
            this.outlineBuffer.put(byteBuffer);
            this.outlineBuffer.flip();
            int i2 = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL.getVertexSize();
            int i3 = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL.getOffset(VertexFormatElement.UV2);

            for (int i4 = 0; i4 + i2 * 4 <= i; i4 += i2 * 4) {
                for (int i5 = 0; i5 < 4; i5++) {
                    int i6 = i4 + i5 * i2 + i3;
                    this.outlineBuffer.putShort(i6, (short)(i5 != 1 && i5 != 2 ? 0 : 1));
                    this.outlineBuffer.putShort(i6 + 2, (short)(i5 >= 2 ? 1 : 0));
                }
            }

            return FrameBufferPool.createBuffer(() -> "sadnes chams outline vbo", this.outlineBuffer);
        }

        private void renderChamsPass(
            RenderPipeline renderPipeline,
            GpuTextureView gpuTextureView,
            GpuTextureView gpuTextureView2,
            GpuTextureView gpuTextureView3,
            GpuSampler gpuSampler,
            boolean flag,
            ShapeIndexBuffer shapeIndexBuffer,
            List<ModelChamsRenderer.Request> requests,
            GpuBufferSlice[] gpuBufferSlices,
            GpuTextureView[] gpuTextureViews,
            GpuBufferSlice[] gpuBufferSlices2
        ) {
            int i = this.drawCalls.size();
            int i2 = 0;

            for (int i3 = 0; i3 < i; i3++) {
                i2 = Math.max(i2, this.drawCalls.get(i3).indexCount());
            }

            GpuBuffer gpuBuffer = shapeIndexBuffer.getIndexBuffer(i2);

            try (RenderPass renderPass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "sadnes model chams", gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
                renderPass.setPipeline(renderPipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setIndexBuffer(gpuBuffer, shapeIndexBuffer.getIndexType());
                if (flag) {
                    renderPass.bindTexture("Sampler1", gpuTextureView3, gpuSampler);
                    GpuTextureView gpuTextureView4 = ChamsNoiseTexture.getTexture();
                    renderPass.bindTexture("Sampler3", gpuTextureView4 != null ? gpuTextureView4 : gpuTextureView3, RenderSystem.getSamplerCache().getRepeated(FilterMode.LINEAR));
                }

                int i4 = -1;

                for (int i5 = 0; i5 < i; i5++) {
                    ModelChamsRenderer.CapturingConsumers.DrawCall drawCall = this.drawCalls.get(i5);
                    int i6 = this.transformRequests.getInt(drawCall.key().entity());
                    if (flag && i6 != i4) {
                        ScriptShader scriptShader = ((ModelChamsRenderer.Request)requests.get(i6)).shader();
                        renderPass.setPipeline(scriptShader != null ? scriptShader.pipeline() : renderPipeline);
                        renderPass.setUniform(scriptShader != null ? "SadnesScriptFx" : "SadnesChams", gpuBufferSlices[i6]);
                        renderPass.bindTexture("Sampler2", gpuTextureViews[i6], gpuSampler);
                        i4 = i6;
                    }

                    renderPass.setUniform("DynamicTransforms", gpuBufferSlices2[drawCall.key().entity()]);
                    renderPass.bindTexture("Sampler0", drawCall.key().texture().textureView(), drawCall.key().texture().sampler());
                    renderPass.setVertexBuffer(0, drawCall.vbo());
                    renderPass.drawIndexed(0, 0, drawCall.indexCount(), 1);
                }
            }
        }

        record BatchKey(Texture texture, int entity) {
        }

        record DrawCall(ModelChamsRenderer.CapturingConsumers.BatchKey key, BuiltBuffer built, GpuBuffer vbo, GpuBuffer outlineVbo, int indexCount) {
        }
    }

    static final class RenderContext {
        final OrderedRenderCommandQueueImpl commandQueue = new OrderedRenderCommandQueueImpl();
        final ModelChamsRenderer.CapturingConsumers consumers = new ModelChamsRenderer.CapturingConsumers();
        final RenderDispatcher dispatcher;

        RenderContext() {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            this.dispatcher = new RenderDispatcher(
                this.commandQueue,
                minecraftClient.getBlockRenderManager(),
                this.consumers,
                minecraftClient.getAtlasManager(),
                minecraftClient.getBufferBuilders().getOutlineVertexConsumers(),
                minecraftClient.getBufferBuilders().getEffectVertexConsumers(),
                minecraftClient.textRenderer
            );
        }
    }

    public record Request(List<Entity> entities, int tint, int style, float speed, float scale, boolean renderModel, ScriptShader shader, ScriptFxUniforms fx) {
        public Request(List<Entity> entities2, int i, int i2, float f, float f2, boolean flag) {
            this(entities2, i, i2, f, f2, flag, null, null);
        }

        public Request(List<Entity> entities2, int i, int i2, float f, float f2) {
            this(entities2, i, i2, f, f2, false);
        }

        public static ModelChamsRenderer.Request script(List<Entity> entities2, int i, boolean flag, ScriptShader scriptShader, ScriptFxUniforms scriptFxUniforms) {
            return new ModelChamsRenderer.Request(entities2, i, 0, 0.0F, 1.0F, flag, scriptShader, scriptFxUniforms);
        }

        boolean needsScene() {
            return this.shader != null
                ? this.shader.needsScene()
                : this.style == 1 || this.style == 2 || this.style == 4 || this.style == 5 || this.tint >>> 24 < 255;
        }

        boolean isVisible() {
            return !this.entities.isEmpty() && (this.shader != null || this.tint >>> 24 != 0);
        }
    }
}
