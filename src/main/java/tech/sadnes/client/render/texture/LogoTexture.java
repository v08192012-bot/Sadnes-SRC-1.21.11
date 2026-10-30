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

public final class LogoTexture {
    private static final SVGLoader LOADER = new SVGLoader();
    private static final int SIZE = 512;
    private static final int DOWNSAMPLE = 3;
    private static final int RENDER_SIZE = 1536;
    private static final int PADDING = 144;
    private static final float SDF_SPREAD = 144.0F;
    private static final Identifier SVG_ID = Identifier.of(ClientInfo.MOD_ID, "logo/logo.svg");
    private static final Identifier TEXTURE_ID = Identifier.of(ClientInfo.MOD_ID, "dynamic/logo");
    private static Identifier texture;

    private LogoTexture() {
    }

    public static TextureSetup getTextureSetup() {
        if (texture == null) {
            texture = load();
        }

        if (texture == null) {
            return null;
        }

        GpuTextureView gpuTextureView = ResourceUtil.getTextureView(texture);
        return gpuTextureView == null ? null : TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
    }

    public static void warmup() {
        if (texture == null) {
            try {
                texture = load();
            } catch (Exception exception) {
                Sadnes.getLogger().debug("Logo warmup skipped: {}", exception.getMessage());
            }
        }
    }

    public static void release() {
        if (texture != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(texture);
            texture = null;
        }
    }

    private static Identifier load() {
        try {
            InputStream inputStream = LogoTexture.class.getResourceAsStream("/assets/" + SVG_ID.getNamespace() + "/" + SVG_ID.getPath());

            byte[] bytes;
            try {
                if (inputStream == null) {
                    Sadnes.getLogger().warn("Logo SVG resource not found: {}", SVG_ID);
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
            SVGDocument svgDocument = LOADER.load(new ByteArrayInputStream(bytes), null, LoaderContext.createDefault());
            if (svgDocument == null) {
                Sadnes.getLogger().warn("Failed to parse logo SVG: {}", SVG_ID);
                return null;
            } else {
                NativeImage nativeImage = SdfGenerator.toNativeImage(rasterize(svgDocument));
                NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(() -> ClientInfo.MOD_ID + "_logo", nativeImage);
                MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE_ID, nativeImageBackedTexture);
                return TEXTURE_ID;
            }
        } catch (Exception exception) {
            Sadnes.getLogger().error("Failed to load logo {}", SVG_ID, exception);
            return null;
        }
    }

    private static BufferedImage rasterize(SVGDocument svgDocument) {
        BufferedImage bufferedImage = new BufferedImage(1536, 1536, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setColor(Color.WHITE);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        svgDocument.render(null, graphics2D, new ViewBox(144.0F, 144.0F, 1248.0F, 1248.0F));
        graphics2D.dispose();
        return SdfGenerator.downsample(SdfGenerator.generateSdf(bufferedImage, 144.0F), 3);
    }
}
