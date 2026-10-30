package tech.sadnes.client.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.item.KeyedItemRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import tech.sadnes.api.mixin.accessor.DrawContextAccessor;
import tech.sadnes.client.animation.Easing;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.impl.render.Esp;
import tech.sadnes.client.render.texture.MipmapTextureLoader;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.util.ResourceUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class GuiRenderUtil {
    private static final Deque<GuiRenderUtil.GlState> STATE_STACK = new ArrayDeque<>();
    private static final int[] SCISSOR_BOX = new int[4];
    private static TextureSetup boundTexture;
    private static float itemAlpha = 1.0F;
    private static final int ENABLED_COLOR = ColorUtil.fromHex("2ECC71");
    private static final float ENABLED_MIX = 0.15F;
    private static final long ITEM_CACHE_NANOS = 250000000L;
    private static final Map<Object, GuiRenderUtil.ItemCacheEntry> ITEM_CACHE = new IdentityHashMap<>();
    private static long lastCacheCleanup;

    private GuiRenderUtil() {
    }

    public static float getItemAlpha() {
        return itemAlpha;
    }

    public static void drawPanel(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, boolean flag) {
        int i;
        Radius radius;
        RenderUtil renderUtil;
        label28: {
            int i2 = Theme.getBase();
            i = flag ? ColorUtil.interpolate(i2, ENABLED_COLOR, 0.15F) : i2;
            radius = Radius.all(f6);
            renderUtil = new RenderUtil(drawContext, f, f2, f3, f4);
            renderUtil.expand(Side.ALL, 0.5F);
            Esp esp = Esp.getInstance();
            String string = esp.performanceMode.getValue();
            byte b = -1;
            switch (string.hashCode()) {
                case -1924829944:
                    if (string.equals("balanced")) {
                        break label28;
                    }
                    break;
                case 651215103:
                    if (string.equals("quality")) {
                        b = 2;
                    }
                    break;
                case 844740128:
                    if (string.equals("maximum")) {
                        renderUtil.rect(i, f5, Radius.all(0.0F));
                        return;
                    }
            }

            switch (b) {
                case 0:
                    renderUtil.rect(i, f5, Radius.all(0.0F));
                    return;
                case 1:
                    break;
                case 2:
                    renderUtil.blurGlowBorder(f5, i, 0.75F, ColorUtil.BLACK, 4.0F, f5 * 0.5F, Easings.QUAD_IN, radius, RenderUtil.BlurQuality.LOW, false);
                default:
                    return;
            }
        }

        renderUtil.outline(f6, ColorUtil.BLACK, f5 * 0.4F, radius);
        renderUtil.rect(i, f5, radius.shrink(0.5F));
    }

    public static void drawItem(DrawContext drawContext, ItemStack itemStack, float f, float f2, float f3) {
        float f4 = MathUtil.roundToStep(f, 0.5F);
        float f5 = MathUtil.roundToStep(f2, 0.5F);
        Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
        if (!(f3 <= 0.0F)) {
            if (f3 >= 1.0F) {
                matrix3x2fStack.translate(f4, f5);
                drawContext.drawItem(itemStack, 0, 0);
                matrix3x2fStack.translate(-f4, -f5);
            } else {
                itemAlpha = f3;

                try {
                    matrix3x2fStack.translate(f4, f5);
                    drawContext.drawItem(itemStack, 0, 0);
                    matrix3x2fStack.translate(-f4, -f5);
                } finally {
                    itemAlpha = 1.0F;
                }
            }
        }
    }

    public static void drawCachedItem(DrawContext drawContext, Object object, ItemStack itemStack, float f, float f2, float f3) {
        float f4 = MathUtil.roundToStep(f, 0.5F);
        float f5 = MathUtil.roundToStep(f2, 0.5F);
        if (drawContext != null && object != null && itemStack != null && !itemStack.isEmpty() && !(f3 <= 0.0F)) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            long l = Util.getMeasuringTimeNano();
            GuiRenderUtil.ItemCacheEntry itemCacheEntry = ITEM_CACHE.computeIfAbsent(object, object2 -> new GuiRenderUtil.ItemCacheEntry());
            itemCacheEntry.lastAccess = System.currentTimeMillis();
            if (itemCacheEntry.renderState == null || !ItemStack.areItemsAndComponentsEqual(itemCacheEntry.stack, itemStack) || l - itemCacheEntry.updateTime >= 250000000L) {
                KeyedItemRenderState keyedItemRenderState = new KeyedItemRenderState();
                minecraftClient.getItemModelManager().clearAndUpdate(keyedItemRenderState, itemStack, ItemDisplayContext.GUI, minecraftClient.world, minecraftClient.player, 0);
                itemCacheEntry.renderState = keyedItemRenderState;
                itemCacheEntry.stack = itemStack.copy();
                itemCacheEntry.updateTime = l;
            }

            GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
            itemAlpha = f3;

            try {
                Matrix3x2f matrix3x2f = new Matrix3x2f(drawContext.getMatrices());
                matrix3x2f.translate(f4, f5);
                guiRenderState.addItem(new ItemGuiElementRenderState("sadnes:item", matrix3x2f, itemCacheEntry.renderState, 0, 0, drawContext.scissorStack.peekLast()));
            } finally {
                itemAlpha = 1.0F;
            }

            cleanupItemCache();
        }
    }

    private static void cleanupItemCache() {
        long l = System.currentTimeMillis();
        if (ITEM_CACHE.size() > 64 && l - lastCacheCleanup >= 2000L) {
            lastCacheCleanup = l;
            ITEM_CACHE.values().removeIf(itemCacheEntry -> l - itemCacheEntry.lastAccess > 4000L);
        }
    }

    public static void bindTexture(int i) {
        boundTexture = null;
        GlStateManager._activeTexture(33984);
        GlStateManager._bindTexture(i);
    }

    public static void bindTexture(Texture texture) {
        if (texture != null) {
            boundTexture = createTextureSetup(texture);
            bindTexture(texture.getGlId());
            boundTexture = createTextureSetup(texture);
        }
    }

    public static void bindTexture(Identifier identifier) {
        if (identifier != null) {
            AbstractTexture abstractTexture = MinecraftClient.getInstance().getTextureManager().getTexture(identifier);
            GpuTextureView gpuTextureView = ResourceUtil.getView(abstractTexture);
            if (gpuTextureView == null) {
                boundTexture = null;
            } else {
                if (abstractTexture.getGlTexture() instanceof GlTexture glTexture && !glTexture.isClosed()) {
                    bindTexture(glTexture.getGlId());
                }

                boolean flag = gpuTextureView.mipLevels() > 1;
                boundTexture = TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR, flag));
            }
        }
    }

    public static TextureSetup getBoundTexture() {
        return boundTexture;
    }

    public static TextureSetup createTextureSetup(Texture texture) {
        if (texture == null) {
            return null;
        }

        GpuTextureView gpuTextureView = ResourceUtil.getView(MipmapTextureLoader.getTexture(texture.getIdentifier()));
        if (gpuTextureView != null) {
            return TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR, true));
        }

        GpuTextureView gpuTextureView2 = ResourceUtil.getTextureView(texture.getIdentifier());
        return gpuTextureView2 == null ? null : TextureSetup.of(gpuTextureView2, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
    }

    public static RenderUtil rect(DrawContext drawContext, float f, float f2, float f3, float f4) {
        return new RenderUtil(drawContext, f, f2, f3, f4);
    }

    public static RenderUtil window(DrawContext drawContext, float f, float f2, float f3, float f4) {
        return window(drawContext, f, f2, f3, f4, 1.0F, 4.0F);
    }

    public static RenderUtil window(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        return window(drawContext, RenderUtil.BlurQuality.LOW, false, f, f2, f3, f4, f5, f6);
    }

    public static RenderUtil window(
        DrawContext drawContext, RenderUtil.BlurQuality blurQuality, boolean flag, float f, float f2, float f3, float f4, float f5, float f6
    ) {
        return window(drawContext, blurQuality, flag, f, f2, f3, f4, f5, Radius.all(f6));
    }

    public static RenderUtil window(
        DrawContext drawContext, RenderUtil.BlurQuality blurQuality, boolean flag, float f, float f2, float f3, float f4, float f5, Radius radius
    ) {
        return window(drawContext, blurQuality, flag, f, f2, f3, f4, f5, radius, radius.getMax() * 2.0F, Theme.getShadowStrength(f5), Easings.QUAD_IN);
    }

    public static RenderUtil window(
        DrawContext drawContext,
        RenderUtil.BlurQuality blurQuality,
        boolean flag,
        float f,
        float f2,
        float f3,
        float f4,
        float f5,
        Radius radius,
        float f6,
        float f7,
        Easing easing
    ) {
        RenderUtil renderUtil = new RenderUtil(
            drawContext, MathUtil.roundToStep(f, 0.5F), MathUtil.roundToStep(f2, 0.5F), MathUtil.roundToStep(f3, 0.5F), MathUtil.roundToStep(f4, 0.5F)
        );
        renderUtil.blurGlowBorder(f5, Theme.getBase(), 0.68F, Theme.getNearBackground(), f6, f7, easing, radius, blurQuality, flag);
        return renderUtil;
    }

    public static RenderUtil drawSurface(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, f4);
        Radius radius = Radius.all(f6);
        renderUtil.rect(Theme.getSurface(), (float)(Math.pow(f5, 3.0) * 0.7F), radius);
        return renderUtil;
    }

    public static RenderUtil drawOutlined(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, f4);
        return applyOutline(renderUtil, f5, f6);
    }

    public static RenderUtil applyOutline(RenderUtil renderUtil, float f, float f2) {
        Radius radius = Radius.smoothAll(f2 * 2.0F);
        int i = Theme.getSurfaceDark();
        int i2 = Theme.getOutline();
        renderUtil.rect(i, (float)Math.pow(f, 3.0) * 0.95F, radius);
        renderUtil.expand(Side.ALL, -0.5F);
        renderUtil.shadow(0.5F, i2, (float)Math.pow(f, 3.0) * 0.5F, radius);
        renderUtil.expand(Side.ALL, 0.5F);
        return renderUtil;
    }

    public static void drawOverlay(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        int i = Theme.withAlpha(Theme.getOutline(), (float)(Math.pow(f5, 3.0) * 0.3F));
        new RenderUtil(drawContext, f, f2, f3, f4).snapped().glow(i);
    }

    public static float clampToScreenX(float f, float f2) {
        float f3 = MinecraftClient.getInstance().getWindow().getScaledWidth();
        if (f + f2 > f3 - 4.0F) {
            f = f3 - 4.0F - f2;
        }

        if (f < 4.0F) {
            f = 4.0F;
        }

        return f;
    }

    public static float clampToScreenY(float f, float f2) {
        float f3 = MinecraftClient.getInstance().getWindow().getScaledHeight();
        if (f + f2 > f3 - 4.0F) {
            f = f3 - 4.0F - f2;
        }

        if (f < 4.0F) {
            f = 4.0F;
        }

        return f;
    }

    public static float placeBeside(float f, float f2, float f3, float f4) {
        float f5 = MinecraftClient.getInstance().getWindow().getScaledWidth();
        float f6 = f2 + f4;
        if (f6 + f3 <= f5 - 4.0F) {
            return f6;
        }

        float f7 = f - f4 - f3;
        return f7 >= 4.0F ? f7 : clampToScreenX(f6, f3);
    }

    public static RenderUtil drawPopup(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        RenderUtil renderUtil = window(drawContext, RenderUtil.BlurQuality.LOW, true, f, f2, f3, f4, f5, f6);
        applyOutline(renderUtil, f5 * 0.7F, f6);
        return renderUtil;
    }

    public static void drawHighlight(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, Radius radius) {
        if (!(f5 <= 0.001F)) {
            RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, f4);
            int i = Theme.withAlpha(Theme.getWhite(), 0.08F * f5);
            if (radius.isSmooth()) {
                renderUtil.rect(i, radius);
            } else {
                renderUtil.rect(i, radius.doubled());
            }
        }
    }

    public static void drawFadeLine(DrawContext drawContext, float f, float f2, float f3, float f4, int i, boolean flag) {
        if (!(f3 <= 0.0F) && !(f4 <= 0.0F)) {
            float f5 = f3 / 2.0F;
            float f6 = f3 - f5;
            int i2 = ColorUtil.setAlpha(i, 0);
            if (flag) {
                PixelRenderUtil.drawVerticalGradient(drawContext, f, f2, f4, f5, i2, i);
                PixelRenderUtil.drawVerticalGradient(drawContext, f, f2 + f5, f4, f6, i, i2);
            } else {
                PixelRenderUtil.drawHorizontalGradient(drawContext, f, f2, f5, f4, i2, i);
                PixelRenderUtil.drawHorizontalGradient(drawContext, f + f5, f2, f6, f4, i, i2);
            }
        }
    }

    public static void drawSprite(DrawContext drawContext, Sprite sprite, float f, float f2, float f3, float f4, float f5) {
        if (drawContext != null && sprite != null && !(f3 <= 0.0F) && !(f4 <= 0.0F) && !(f5 <= 0.001F)) {
            Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
            matrix3x2fStack.pushMatrix();
            matrix3x2fStack.translate(f, f2);
            drawContext.drawSpriteStretched(
                RenderPipelines.GUI_TEXTURED,
                sprite,
                0,
                0,
                Math.max(1, Math.round(f3)),
                Math.max(1, Math.round(f4)),
                ColorUtil.setAlphaf(ColorUtil.WHITE, f5)
            );
            matrix3x2fStack.popMatrix();
        }
    }

    public static void drawEntity(DrawContext drawContext, int i, int i2, int i3, int i4, float f, float f2, float f3, Entity entity, float f4) {
        drawEntity(drawContext, i, i2, i3, i4, f, f2, f3, entity, f4, 0.0F);
    }

    public static void drawEntity(
        DrawContext drawContext, int i, int i2, int i3, int i4, float f, float f2, float f3, Entity entity, float f4, float f5
    ) {
        drawEntity(drawContext, i, i2, i3, i4, f, f2, f3, entity, f4, f5, null, 0, 0.0F, 0, 0, 0.0F, 0.0F);
    }

    public static void drawEntity(
        DrawContext drawContext,
        int i,
        int i2,
        int i3,
        int i4,
        float f,
        float f2,
        float f3,
        Entity entity,
        float f4,
        float f5,
        Consumer<MatrixStack> consumer,
        int i5,
        float f6,
        int i6,
        int i7,
        float f7,
        float f8
    ) {
        drawEntity(drawContext, i, i2, i3, i4, f, f2, f3, entity, f4, f5, consumer, i5, f6, i6, i7, f7, f8, false);
    }

    public static void drawEntity(
        DrawContext drawContext,
        int i,
        int i2,
        int i3,
        int i4,
        float f,
        float f2,
        float f3,
        Entity entity,
        float f4,
        float f5,
        Consumer<MatrixStack> consumer,
        int i5,
        float f6,
        int i6,
        int i7,
        float f7,
        float f8,
        boolean flag
    ) {
        if (drawContext != null && entity != null && !(f4 <= 0.01F) && !(f <= 0.01F)) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            EntityRenderManager entityRenderManager = minecraftClient.getEntityRenderDispatcher();
            EntityRenderer entityRenderer = entityRenderManager.getRenderer(entity);
            if (entityRenderer != null) {
                EntityRenderState entityRenderState = EntityStateCache.createRenderState(entityRenderer, entity);
                entityRenderState.light = 15728880;
                entityRenderState.shadowPieces.clear();
                entityRenderState.outlineColor = 0;
                entityRenderState.displayName = null;
                boolean flag2 = entityRenderState instanceof LivingEntityRenderState;
                if (entityRenderState instanceof LivingEntityRenderState livingEntityRenderState) {
                    livingEntityRenderState.bodyYaw = 180.0F + f2;
                    livingEntityRenderState.relativeHeadYaw = 0.0F;
                    livingEntityRenderState.pitch = 0.0F;
                    livingEntityRenderState.limbSwingAnimationProgress = 0.0F;
                    livingEntityRenderState.limbSwingAmplitude = 0.0F;
                    livingEntityRenderState.width = livingEntityRenderState.width / livingEntityRenderState.baseScale;
                    livingEntityRenderState.height = livingEntityRenderState.height / livingEntityRenderState.baseScale;
                    livingEntityRenderState.baseScale = 1.0F;
                }

                float f9 = flag2 ? entityRenderState.height : 0.65F;
                Quaternionf quaternionf = new Quaternionf().rotateX(f3 * (float) (Math.PI / 180.0));
                Quaternionf quaternionf2 = new Quaternionf().rotateZ((float) Math.PI);
                if (!flag2) {
                    quaternionf2.rotateY((180.0F + f2) * (float) (Math.PI / 180.0));
                }

                quaternionf2.mul(quaternionf);
                Vector3f vector3f = quaternionf2.transform(new Vector3f(0.0F, -f9 / 2.0F, 0.0F));
                vector3f.y += f5 / f;
                ScreenRect screenRect = drawContext.scissorStack.peekLast();
                if (screenRect != null && (screenRect.width() <= 0 || screenRect.height() <= 0)) {
                    screenRect = null;
                }

                Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
                Vector2f vector2f = matrix3x2fStack.transformPosition(i, i2, new Vector2f());
                Vector2f vector2f2 = matrix3x2fStack.transformPosition(i3, i4, new Vector2f());
                int i8 = Math.round(Math.min(vector2f.x, vector2f2.x));
                int i9 = Math.round(Math.min(vector2f.y, vector2f2.y));
                int i10 = Math.round(Math.max(vector2f.x, vector2f2.x));
                int i11 = Math.round(Math.max(vector2f.y, vector2f2.y));
                float f10 = matrix3x2fStack.m00() > 0.0F ? matrix3x2fStack.m00() : 1.0F;
                GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
                guiRenderState.addSpecialElement(
                    new EntityPreviewState(
                        entityRenderState,
                        vector3f,
                        quaternionf2,
                        quaternionf,
                        consumer,
                        f4,
                        i8,
                        i9,
                        i10,
                        i11,
                        f * f10,
                        i5,
                        f6,
                        i6,
                        i7,
                        f7,
                        f8,
                        flag,
                        screenRect
                    )
                );
            }
        }
    }

    record GlState(
        boolean blendEnabled,
        boolean cullEnabled,
        boolean depthEnabled,
        int srcRgb,
        int dstRgb,
        int srcAlpha,
        int dstAlpha,
        boolean scissorEnabled,
        int scissorX,
        int scissorY,
        int scissorWidth,
        int scissorHeight
    ) {
    }

    static final class ItemCacheEntry {
        KeyedItemRenderState renderState;
        ItemStack stack = ItemStack.EMPTY;
        long updateTime;
        long lastAccess;
    }
}
