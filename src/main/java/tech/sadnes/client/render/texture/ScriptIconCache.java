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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.render.font.SdfGenerator;
import tech.sadnes.client.util.ResourceUtil;

public final class ScriptIconCache {
    private static final SVGLoader LOADER = new SVGLoader();
    private static final Map<String, ScriptIconCache.Entry> CACHE = new ConcurrentHashMap<>();

    private ScriptIconCache() {
    }

    public static TextureSetup getTextureSetup(Path path) {
        Identifier identifier = loadTexture(path);
        if (identifier == null) {
            return null;
        }

        GpuTextureView gpuTextureView = ResourceUtil.getTextureView(identifier);
        return gpuTextureView == null ? null : TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
    }

    private static Identifier loadTexture(Path path) {
        if (path != null && Files.isRegularFile(path)) {
            String string = path.toAbsolutePath().normalize().toString();
            long l = 0L;

            try {
                l = Files.getLastModifiedTime(path).toMillis();
            } catch (Exception exception) {
            }

            ScriptIconCache.Entry entry = CACHE.get(string);
            if (entry != null && entry.modified() == l) {
                return entry.texture();
            }

            try (InputStream inputStream = Files.newInputStream(path)) {
                SVGDocument svgDocument = LOADER.load(inputStream, null, LoaderContext.createDefault());
                if (svgDocument == null) {
                    return null;
                }

                NativeImage nativeImage = SdfGenerator.toNativeImage(rasterize(svgDocument));
                Identifier identifier = Identifier.of(ClientInfo.MOD_ID, "dynamic/script_icon/" + Integer.toHexString(string.hashCode()) + "_" + l);
                NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(() -> identifier.toString(), nativeImage);
                MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, nativeImageBackedTexture);
                if (entry != null) {
                    MinecraftClient.getInstance().getTextureManager().destroyTexture(entry.texture());
                }

                CACHE.put(string, new ScriptIconCache.Entry(identifier, l));
                return identifier;
            } catch (Exception exception2) {
                Sadnes.getLogger().warn("[ScriptIcon] {}: {}", path.getFileName(), exception2.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    private static BufferedImage rasterize(SVGDocument svgDocument) {
        int i = (int)SvgIconCache.getTextureSize();
        BufferedImage bufferedImage = new BufferedImage(i, i, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setColor(Color.WHITE);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        float f = i - 10 * 2.0F;
        svgDocument.render(null, graphics2D, new ViewBox(10, 10, f, f));
        graphics2D.dispose();
        return SdfGenerator.generateSdf(bufferedImage, SvgIconCache.getSdfSpread());
    }

    record Entry(Identifier texture, long modified) {
    }
}
