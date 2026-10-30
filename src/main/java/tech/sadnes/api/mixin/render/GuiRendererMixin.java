package tech.sadnes.api.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.texture.TextureSetup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.sadnes.api.duck.GuiItemAlphaHolder;
import tech.sadnes.client.render.shader.Pipelines;
import tech.sadnes.client.render.shader.ShapeBlur;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Shadow
    @Final
    private List<?> preparations;
    @Unique
    private final IntArrayList sadnes$blurMarkers = new IntArrayList();
    @Unique
    private final IntArrayList sadnes$blurMarkerSlots = new IntArrayList();

    @Invoker("render")
    abstract void sadnes$renderRange(
        Supplier<String> supplier, Framebuffer framebuffer, GpuBufferSlice gpuBufferSlice, GpuBufferSlice gpuBufferSlice2, GpuBuffer gpuBuffer, IndexType indexType, int i, int i2
    );

    @Inject(method = "prepare", at = @At("HEAD"))
    private void sadnes$resetMarkers(CallbackInfo callbackInfo) {
        this.sadnes$blurMarkers.clear();
        this.sadnes$blurMarkerSlots.clear();
    }

    @Inject(method = "endBuffer", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0))
    private void sadnes$trackMarker(BufferBuilder bufferBuilder, RenderPipeline renderPipeline, TextureSetup textureSetup, ScreenRect screenRect, CallbackInfo callbackInfo) {
        if (renderPipeline == Pipelines.SHAPE_TEXTURE_GUI_2D_BLUR || renderPipeline == Pipelines.GLASS_2D_BLUR) {
            int i = textureSetup == null ? -1 : ShapeBlur.INSTANCE.findRegion(textureSetup.texure0());
            this.sadnes$blurMarkers.add(this.preparations.size());
            this.sadnes$blurMarkerSlots.add(i);
        }
    }

    @Redirect(
        method = "renderPreparedDraws",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Ljava/util/function/Supplier;Lnet/minecraft/client/gl/Framebuffer;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;II)V"
        )
    )
    private void sadnes$splitInclude2DBlur(
        GuiRenderer guiRenderer, Supplier<String> supplier, Framebuffer framebuffer, GpuBufferSlice gpuBufferSlice, GpuBufferSlice gpuBufferSlice2, GpuBuffer gpuBuffer, IndexType indexType, int i, int i2
    ) {
        if (!this.sadnes$blurMarkers.isEmpty() && i < i2) {
            int i3 = i;

            for (int i4 = 0; i4 < this.sadnes$blurMarkers.size(); i4++) {
                int i5 = this.sadnes$blurMarkers.getInt(i4);
                if (i5 >= i && i5 < i2) {
                    int i6 = this.sadnes$blurMarkerSlots.getInt(i4);
                    if (i6 >= 0 && ShapeBlur.INSTANCE.needsRegionUpdate(i6)) {
                        if (i5 > i3) {
                            this.sadnes$renderRange(supplier, framebuffer, gpuBufferSlice, gpuBufferSlice2, gpuBuffer, indexType, i3, i5);
                        }

                        ShapeBlur.INSTANCE.renderRegion(i6, MinecraftClient.getInstance().getFramebuffer());
                        i3 = i5;
                    }
                }
            }

            if (i3 < i2) {
                this.sadnes$renderRange(supplier, framebuffer, gpuBufferSlice, gpuBufferSlice2, gpuBuffer, indexType, i3, i2);
            }
        } else {
            this.sadnes$renderRange(supplier, framebuffer, gpuBufferSlice, gpuBufferSlice2, gpuBuffer, indexType, i, i2);
        }
    }

    @ModifyArg(
        method = "prepareItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/state/TexturedQuadGuiElementRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/texture/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/ScreenRect;Lnet/minecraft/client/gui/ScreenRect;)V"
        ),
        index = 11
    )
    private int sadnes$fadeItemTint(int i, @Local(argsOnly = true) ItemGuiElementRenderState itemGuiElementRenderState) {
        float f = ((GuiItemAlphaHolder)(Object)itemGuiElementRenderState).sadnes$guiItemAlpha();
        if (f >= 1.0F) {
            return i;
        }

        int i2 = Math.round(f * 255.0F);
        return i2 << 24 | i2 << 16 | i2 << 8 | i2;
    }
}
