package tech.sadnes.client.render.texture;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.render.font.SdfGenerator;
import tech.sadnes.client.util.crypto.AssetCrypto;

public final class PlainLogoTexture {
    private static final SVGLoader LOADER = new SVGLoader();
    private static final int SIZE = 256;
    private static final int DOWNSAMPLE = 4;
    private static final int RENDER_SIZE = 1024;
    private static final float PADDING_RATIO = 0.09375F;
    private static final int PADDING = Math.round(96.0F);
    private static final Identifier SVG_ID = Identifier.of(ClientInfo.MOD_ID, "logo/logo.svg");
    private static final Identifier TEXTURE_ID = Identifier.of(ClientInfo.MOD_ID, "dynamic/logo_plain");
    private static Identifier texture;
    private static int textureSize = 256;

    private PlainLogoTexture() {
    }

    public static Identifier getTexture() {
        if (texture == null) {
            texture = load();
        }

        return texture;
    }

    public static int getTextureSize() {
        return textureSize;
    }

    public static void release() {
        if (texture != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(texture);
            texture = null;
        }
    }

    private static Identifier load() {
        try {
            InputStream inputStream = PlainLogoTexture.class.getResourceAsStream("/assets/" + SVG_ID.getNamespace() + "/" + SVG_ID.getPath());

            byte[] bytes;
            try {
                if (inputStream == null) {
                    Sadnes.getLogger().warn("Plain logo SVG resource not found: {}", SVG_ID);
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
                Sadnes.getLogger().warn("Failed to parse plain logo SVG: {}", SVG_ID);
                return null;
            } else {
                BufferedImage bufferedImage = new BufferedImage(1024, 1024, 2);
                Graphics2D graphics2D = bufferedImage.createGraphics();
                graphics2D.setColor(Color.WHITE);
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                float f = 1024.0F - PADDING * 2.0F;
                svgDocument.render(null, graphics2D, new ViewBox(PADDING, PADDING, f, f));
                graphics2D.dispose();
                BufferedImage bufferedImage2 = SdfGenerator.downsample(bufferedImage, 4);
                NativeImage nativeImage = SdfGenerator.toNativeImage(bufferedImage2);
                textureSize = bufferedImage2.getWidth();
                NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(() -> ClientInfo.MOD_ID + "_logo_plain", nativeImage);
                MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE_ID, nativeImageBackedTexture);
                return TEXTURE_ID;
            }
        } catch (Exception exception) {
            Sadnes.getLogger().error("Failed to load plain logo {}", SVG_ID, exception);
            return null;
        }
    }
}
