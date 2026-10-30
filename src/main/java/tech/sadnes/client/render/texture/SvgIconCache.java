package tech.sadnes.client.render.texture;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.render.font.SdfGenerator;
import tech.sadnes.client.util.ResourceUtil;
import tech.sadnes.client.util.crypto.AssetCrypto;

public final class SvgIconCache {
    private static final Map<Icon, SvgIconCache.Entry> CACHE = new HashMap<>();
    private static final SVGLoader SVG_LOADER = new SVGLoader();
    private static final int TEXTURE_SIZE = 128;
    private static final int PADDING = 10;
    private static final float SDF_SPREAD = 12.0F;

    private SvgIconCache() {
    }

    public static int getTextureId(Icon icon) {
        if (icon == null) {
            return 0;
        }

        SvgIconCache.Entry entry = CACHE.get(icon);
        if (entry != null) {
            return entry.textureId;
        }

        SvgIconCache.Entry entry2 = loadIcon(icon);
        if (entry2 == null) {
            return 0;
        }

        CACHE.put(icon, entry2);
        return entry2.textureId;
    }

    public static TextureSetup getTextureSetup(Icon icon) {
        if (icon == null) {
            return null;
        }

        SvgIconCache.Entry entry = CACHE.get(icon);
        if (entry == null) {
            SvgIconCache.Entry entry2 = loadIcon(icon);
            if (entry2 == null) {
                return null;
            }

            CACHE.put(icon, entry2);
            entry = entry2;
        }

        GpuTextureView gpuTextureView = ResourceUtil.getTextureView(entry.texture);
        return gpuTextureView == null ? null : TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
    }

    public static void warmup() {
        for (Icon icon : Icon.values()) {
            try {
                if (!CACHE.containsKey(icon)) {
                    SvgIconCache.Entry entry = loadIcon(icon);
                    if (entry != null) {
                        CACHE.put(icon, entry);
                    }
                }
            } catch (Exception exception) {
                Sadnes.getLogger().debug("Icon warmup skipped for {}: {}", icon.name(), exception.getMessage());
            }
        }
    }

    public static void clear() {
        int i = CACHE.size();
        if (i != 0) {
            for (SvgIconCache.Entry entry : CACHE.values()) {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(entry.texture);
            }

            CACHE.clear();
        }
    }

    public static float getSdfSpread() {
        return 12.0F;
    }

    public static float getTextureSize() {
        return 128.0F;
    }

    private static SvgIconCache.Entry loadIcon(Icon icon) {
        Identifier identifier = icon.getIdentifier();

        try {
            InputStream inputStream = SvgIconCache.class.getResourceAsStream("/assets/" + identifier.getNamespace() + "/" + identifier.getPath());

            byte[] bytes;
            try {
                if (inputStream == null) {
                    Sadnes.getLogger().warn("SVG icon resource not found: {}", identifier);
                    return null;
                }

                bytes = AssetCrypto.decrypt(inputStream.readAllBytes());
            } catch (Throwable throwable) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }

                throw throwable;
            }

            inputStream.close();
            SVGDocument svgDocument = SVG_LOADER.load(new ByteArrayInputStream(bytes), null, LoaderContext.createDefault());
            if (svgDocument == null) {
                Sadnes.getLogger().warn("Failed to parse SVG icon: {}", identifier);
                return null;
            } else {
                BufferedImage bufferedImage = rasterize(svgDocument);
                NativeImage nativeImage = SdfGenerator.toNativeImage(bufferedImage);
                String string = icon.name().toLowerCase() + "_sdf";
                Identifier identifier2 = Identifier.of(ClientInfo.MOD_ID, "dynamic/icon/" + string);
                NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(() -> ClientInfo.MOD_ID + "_svg_icon_" + string, nativeImage);
                MinecraftClient.getInstance().getTextureManager().registerTexture(identifier2, nativeImageBackedTexture);
                return new SvgIconCache.Entry(identifier2, ResourceUtil.getGlId(identifier2));
            }
        } catch (Exception exception) {
            Sadnes.getLogger().error("Failed to load svg icon {}", identifier, exception);
            return null;
        }
    }

    private static BufferedImage rasterize(SVGDocument svgDocument) {
        BufferedImage bufferedImage = new BufferedImage(128, 128, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setColor(Color.WHITE);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        svgDocument.render(null, graphics2D, new ViewBox(10.0F, 10.0F, 108.0F, 108.0F));
        graphics2D.dispose();
        return SdfGenerator.generateSdf(bufferedImage, 12.0F);
    }

    record Entry(Identifier texture, int textureId) {
    }
}
