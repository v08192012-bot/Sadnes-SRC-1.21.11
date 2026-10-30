package tech.sadnes.client.script.editor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.ScriptTextureLoader;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class FilePreview implements IInteractable {
    private static final int HEX_ROWS = 12;
    private static final int HEX_BYTES_PER_ROW = 16;
    private Path path;
    private String extension = "";
    private long size;
    private FileTime lastModified;
    private byte[] headerBytes = new byte[0];
    private float zoom = 1.0F;
    private float panX;
    private float panY;

    public void open(Path path2) {
        this.path = path2;
        this.zoom = 1.0F;
        this.panX = 0.0F;
        this.panY = 0.0F;
        this.extension = getExtension(path2);
        this.size = 0L;
        this.lastModified = null;
        this.headerBytes = new byte[0];

        try {
            this.size = Files.size(path2);
            this.lastModified = Files.getLastModifiedTime(path2);
            if (!this.isImage()) {
                try (InputStream inputStream = Files.newInputStream(path2)) {
                    this.headerBytes = inputStream.readNBytes(192);
                }
            }
        } catch (IOException ioException) {
        }
    }

    public boolean isImage() {
        String string = this.extension;
        byte b = -1;
        switch (string.hashCode()) {
            case 97669:
                if (string.equals("bmp")) {
                    return true;
                }
                break;
            case 105441:
                if (string.equals("jpg")) {
                    return true;
                }
                break;
            case 111145:
                if (string.equals("png")) {
                    return true;
                }
                break;
            case 114766:
                if (string.equals("tga")) {
                    b = 4;
                }
                break;
            case 3268712:
                if (string.equals("jpeg")) {
                    return true;
                }
        }

        switch (b) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
    }

    public void zoomBy(float f) {
        this.zoom = Math.clamp(this.zoom + f, 0.2F, 8.0F);
    }

    public void panBy(float f, float f2) {
        this.panX += f;
        this.panY += f2;
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        if (this.path != null) {
            if (!this.isImage() || !this.renderImage(drawContext, f, f2, f3, f4, f5)) {
                this.renderInfoCard(drawContext, f, f2, f3, f4, f5);
            }
        }
    }

    private boolean renderImage(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        ScriptTextureLoader.ScriptTexture scriptTexture = ScriptTextureLoader.load(this.path.toString());
        if (scriptTexture != null && scriptTexture.width() > 0 && ScriptTextureLoader.bind(this.path.toString())) {
            float f6 = f3 - 48.0F;
            float f7 = f4 - 56.0F;
            float f8 = Math.min(f6 / scriptTexture.width(), f7 / scriptTexture.height()) * this.zoom;
            float f9 = Math.max(1.0F, scriptTexture.width() * f8);
            float f10 = Math.max(1.0F, scriptTexture.height() * f8);
            float f11 = Math.max(0.0F, (f9 - f6) / 2.0F) + 30.0F;
            float f12 = Math.max(0.0F, (f10 - f7) / 2.0F) + 30.0F;
            this.panX = Math.clamp(this.panX, -f11, f11);
            this.panY = Math.clamp(this.panY, -f12, f12);
            float f13 = f + f3 / 2.0F - f9 / 2.0F + this.panX;
            float f14 = f2 + f4 / 2.0F - f10 / 2.0F - 6.0F + this.panY;
            if (ScissorStack.push(drawContext, f, f2, f3, f4)) {
                try {
                    GuiRenderUtil.drawOutlined(drawContext, f13 - 2.0F, f14 - 2.0F, f9 + 4.0F, f10 + 4.0F, f5 * 0.5F, 4.0F);
                    ScriptTextureLoader.bind(this.path.toString());
                    new RenderUtil(drawContext, f13, f14, f9, f10).frostedGlass(f5, Radius.zero());
                } finally {
                    ScissorStack.pop(drawContext);
                }
            }

            String string = scriptTexture.width() + "×" + scriptTexture.height() + "   " + formatSize(this.size) + "   " + Math.round(f8 * 100.0F) + "%";
            new RenderUtil(drawContext, f, f2 + f4 - 16.0F, f3, 12.0F)
                .textCenter(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.8F), f5), 6.0F);
            return true;
        } else {
            return false;
        }
    }

    private void renderInfoCard(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        float f6 = Math.min(f3 - 20.0F, 340.0F);
        float f7 = Math.min(f4 - 20.0F, 200.0F);
        float f8 = f + f3 / 2.0F - f6 / 2.0F;
        float f9 = f2 + f4 / 2.0F - f7 / 2.0F;
        GuiRenderUtil.drawOutlined(drawContext, f8, f9, f6, f7, f5 * 0.75F, 8.0F);
        int i = ColorUtil.multiplyAlpha(Theme.getTextColor(), f5);
        int i2 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.7F), f5);
        new RenderUtil(drawContext, f8, f9, f6, f7)
            .icon(this.getFileIcon(), f6 / 2.0F - 9.0F, 14.0F, 18.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f5))
            .textTopCenter(Fonts.INTER_MEDIUM, this.path.getFileName().toString(), 0.0F, 38.0F, i, 7.0F)
            .textTopCenter(Fonts.INTER_REGULAR, this.getInfoLine(), 0.0F, 49.0F, i2, 5.5F);
        if (this.headerBytes.length != 0) {
            float f10 = f9 + 64.0F;
            int i3 = Math.min(12, (this.headerBytes.length + 16 - 1) / 16);
            StringBuilder builder = new StringBuilder(64);

            for (int i4 = 0; i4 < i3; i4++) {
                builder.setLength(0);
                int i5 = i4 * 16;
                int i6 = Math.min(this.headerBytes.length, i5 + 16);

                for (int i7 = i5; i7 < i6; i7++) {
                    builder.append(String.format("%02X ", this.headerBytes[i7]));
                }

                builder.append("  ");

                for (int i8 = i5; i8 < i6; i8++) {
                    char c = (char)(this.headerBytes[i8] & 0xFF);
                    builder.append(c >= ' ' && c < 127 ? c : '.');
                }

                Fonts.INTER_REGULAR
                    .draw(
                        drawContext,
                        builder.toString(),
                        f8 + 10.0F,
                        f10 + i4 * 8.0F,
                        ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.55F), f5),
                        5.0F
                    );
            }
        }
    }

    private Icon getFileIcon() {
        String string = this.extension;
        byte b = -1;
        switch (string.hashCode()) {
            case 3669:
                if (string.equals("sh")) {
                    return Icon.RUN;
                }
                break;
            case 3676:
                if (string.equals("so")) {
                    return Icon.MICROCHIP;
                }
                break;
            case 97301:
                if (string.equals("bat")) {
                    return Icon.RUN;
                }
                break;
            case 98618:
                if (string.equals("cmd")) {
                    return Icon.RUN;
                }
                break;
            case 99556:
                if (string.equals("dll")) {
                    return Icon.MICROCHIP;
                }
                break;
            case 100882:
                if (string.equals("exe")) {
                    return Icon.RUN;
                }
                break;
            case 104987:
                if (string.equals("jar")) {
                    return Icon.PACKAGE;
                }
                break;
            case 108272:
                if (string.equals("mp3")) {
                    return Icon.MUSIC;
                }
                break;
            case 108273:
                if (string.equals("mp4")) {
                    return Icon.PLAY;
                }
                break;
            case 108308:
                if (string.equals("mov")) {
                    return Icon.PLAY;
                }
                break;
            case 109967:
                if (string.equals("ogg")) {
                    return Icon.MUSIC;
                }
                break;
            case 110369:
                if (string.equals("otf")) {
                    b = 18;
                }
                break;
            case 112675:
                if (string.equals("rar")) {
                    return Icon.PACKAGE;
                }
                break;
            case 115174:
                if (string.equals("ttf")) {
                    return Icon.FONT;
                }
                break;
            case 117484:
                if (string.equals("wav")) {
                    return Icon.MUSIC;
                }
                break;
            case 120609:
                if (string.equals("zip")) {
                    return Icon.PACKAGE;
                }
                break;
            case 3145576:
                if (string.equals("flac")) {
                    return Icon.MUSIC;
                }
                break;
            case 3645337:
                if (string.equals("webm")) {
                    return Icon.PLAY;
                }
                break;
            case 96063952:
                if (string.equals("dylib")) {
                    return Icon.MICROCHIP;
                }
        }

        switch (b) {
            case 0:
            case 1:
            case 2:
                return Icon.MICROCHIP;
            case 3:
            case 4:
            case 5:
            case 6:
                return Icon.RUN;
            case 7:
            case 8:
            case 9:
            case 10:
                return Icon.MUSIC;
            case 11:
            case 12:
            case 13:
                return Icon.PACKAGE;
            case 14:
            case 15:
            case 16:
                return Icon.PLAY;
            case 17:
            case 18:
                return Icon.FONT;
            default:
                return Icon.RECTANGLE_LIST;
        }
    }

    private String getInfoLine() {
        String string = this.extension.isEmpty() ? "file" : this.extension.toUpperCase(Locale.ROOT);
        String string2 = this.lastModified == null ? "" : "   " + this.lastModified.toInstant().toString().substring(0, 16).replace('T', ' ');
        return string + "   " + formatSize(this.size) + string2;
    }

    public static String formatSize(long l) {
        if (l < 1024L) {
            return l + " B";
        } else {
            return l < 1048576L ? String.format(Locale.ROOT, "%.1f KB", l / 1024.0) : String.format(Locale.ROOT, "%.1f MB", l / 1048576.0);
        }
    }

    public static String getExtension(Path path2) {
        String string = path2.getFileName().toString();
        int i = string.lastIndexOf(46);
        return i < 0 ? "" : string.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean isTextFile(Path path2) {
        String string = getExtension(path2);
        byte b = -1;
        switch (string.hashCode()) {
            case -1079613285:
                if (string.equals("mcmeta")) {
                    return true;
                }
                break;
            case -926053069:
                if (string.equals("properties")) {
                    return true;
                }
                break;
            case 0:
                if (string.equals("")) {
                    return true;
                }
                break;
            case 3401:
                if (string.equals("js")) {
                    return true;
                }
                break;
            case 3479:
                if (string.equals("md")) {
                    return true;
                }
                break;
            case 3676:
                if (string.equals("so")) {
                    return false;
                }
                break;
            case 97543:
                if (string.equals("bin")) {
                    b = 41;
                }
                break;
            case 97669:
                if (string.equals("bmp")) {
                    return false;
                }
                break;
            case 98404:
                if (string.equals("cfg")) {
                    return true;
                }
                break;
            case 98822:
                if (string.equals("csv")) {
                    return true;
                }
                break;
            case 99556:
                if (string.equals("dll")) {
                    return false;
                }
                break;
            case 100882:
                if (string.equals("exe")) {
                    return false;
                }
                break;
            case 101691:
                if (string.equals("fsh")) {
                    return true;
                }
                break;
            case 102340:
                if (string.equals("gif")) {
                    return false;
                }
                break;
            case 104420:
                if (string.equals("ini")) {
                    return true;
                }
                break;
            case 104987:
                if (string.equals("jar")) {
                    return false;
                }
                break;
            case 105441:
                if (string.equals("jpg")) {
                    return false;
                }
                break;
            case 107332:
                if (string.equals("log")) {
                    return true;
                }
                break;
            case 108272:
                if (string.equals("mp3")) {
                    return false;
                }
                break;
            case 108273:
                if (string.equals("mp4")) {
                    return false;
                }
                break;
            case 109967:
                if (string.equals("ogg")) {
                    return false;
                }
                break;
            case 110369:
                if (string.equals("otf")) {
                    return false;
                }
                break;
            case 111145:
                if (string.equals("png")) {
                    return false;
                }
                break;
            case 114766:
                if (string.equals("tga")) {
                    return false;
                }
                break;
            case 115174:
                if (string.equals("ttf")) {
                    return false;
                }
                break;
            case 115312:
                if (string.equals("txt")) {
                    return true;
                }
                break;
            case 117067:
                if (string.equals("vsh")) {
                    return true;
                }
                break;
            case 117484:
                if (string.equals("wav")) {
                    return false;
                }
                break;
            case 118807:
                if (string.equals("xml")) {
                    return true;
                }
                break;
            case 119768:
                if (string.equals("yml")) {
                    return true;
                }
                break;
            case 120609:
                if (string.equals("zip")) {
                    return false;
                }
                break;
            case 3059492:
                if (string.equals("conf")) {
                    return true;
                }
                break;
            case 3175934:
                if (string.equals("glsl")) {
                    return true;
                }
                break;
            case 3213227:
                if (string.equals("html")) {
                    return true;
                }
                break;
            case 3268712:
                if (string.equals("jpeg")) {
                    return false;
                }
                break;
            case 3271912:
                if (string.equals("json")) {
                    return true;
                }
                break;
            case 3314158:
                if (string.equals("lang")) {
                    return true;
                }
                break;
            case 3347973:
                if (string.equals("meta")) {
                    return true;
                }
                break;
            case 3565914:
                if (string.equals("toml")) {
                    return true;
                }
                break;
            case 3701415:
                if (string.equals("yaml")) {
                    return true;
                }
                break;
            case 94742904:
                if (string.equals("class")) {
                    return false;
                }
                break;
            case 96063952:
                if (string.equals("dylib")) {
                    return false;
                }
        }

        switch (b) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
                return true;
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
                return false;
            default:
                try (InputStream inputStream = Files.newInputStream(path2)) {
                    for (byte b2 : inputStream.readNBytes(8192)) {
                        if (b2 == 0) {
                            return false;
                        }
                    }

                    return true;
                } catch (IOException ioException) {
                    return false;
                }
        }
    }
}
