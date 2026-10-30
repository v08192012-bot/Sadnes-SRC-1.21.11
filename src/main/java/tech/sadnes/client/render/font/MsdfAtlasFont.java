package tech.sadnes.client.render.font;

import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.render.GuiBoundsUtil;
import tech.sadnes.client.render.HorizontalFadeMask;
import tech.sadnes.client.util.ResourceUtil;
import tech.sadnes.client.util.color.ColorUtil;

public final class MsdfAtlasFont {
    private static final float EMOJI_SCALE = 1.2F;
    private static final float EMOJI_Y_OFFSET = 0.2F;
    private static final float EMOJI_X_OFFSET = 0.05F;
    private static final float EMOJI_SPACING = 0.1F;
    private final Identifier texture;
    private final MsdfFontData.Atlas atlas;
    private final MsdfFontData.Metrics metrics;
    private final Int2ObjectOpenHashMap<MsdfGlyph> glyphs;
    private final Long2FloatOpenHashMap kerning;

    MsdfAtlasFont(Identifier identifier, MsdfFontData.Atlas atlas2, MsdfFontData.Metrics metrics2, Int2ObjectOpenHashMap<MsdfGlyph> int2ObjectOpenHashMap, Long2FloatOpenHashMap long2FloatOpenHashMap) {
        this.texture = identifier;
        this.atlas = atlas2;
        this.metrics = metrics2;
        this.glyphs = int2ObjectOpenHashMap;
        this.kerning = long2FloatOpenHashMap;
    }

    public float drawText(
        DrawContext drawContext, float f, String string, float f2, float f3, float f4, float f5, int i, float f6, Matrix3x2fc matrix3x2fc, boolean flag
    ) {
        if (drawContext != null && string != null && !string.isEmpty()) {
            TextureSetup textureSetup = this.getTextureSetup();
            if (textureSetup == null) {
                return f3;
            }

            float f7 = Math.max(0.0F, Math.min(1.0F, f6));
            int i2 = ColorUtil.setAlpha(i, Math.round(ColorUtil.alpha(i) * f7));
            int i3 = string.length();
            float[] floats = new float[i3 * 8];
            int[] ints = new int[i3];
            int i4 = 0;
            float f8 = Float.MAX_VALUE;
            float f9 = Float.MAX_VALUE;
            float f10 = -Float.MAX_VALUE;
            float f11 = -Float.MAX_VALUE;
            float f12 = f4 - 0.125F * f;
            EmojiRenderer emojiRenderer = EmojiRenderer.getInstance();
            Matrix3x2f matrix3x2f = null;
            ScreenRect screenRect = null;
            boolean flag2 = HorizontalFadeMask.isActive();
            int i5 = i2;
            int i6 = 0;
            int i7 = 0;

            while (i7 < i3) {
                char c = string.charAt(i7);
                if (c == 167 && i7 + 1 < i3 && isFormattingCode(string.charAt(i7 + 1))) {
                    i7 += 2;
                } else {
                    if (c == '$' && i7 + 1 < i3 && string.charAt(i7 + 1) == '{') {
                        long l = parseColorTag(string, i7, i2, f7);
                        if (l >= 0L) {
                            i5 = (int)l;
                            i7 = (int)(l >>> 32);
                            i6 = 0;
                            continue;
                        }
                    }

                    int i8 = string.codePointAt(i7);
                    long l2 = emojiRenderer.matchEmoji(string, i7, i8);
                    if (l2 != -1L) {
                        float f13 = f * 1.2F;
                        float f14 = f3 + f * 0.05F;
                        float f15 = f12 + f * 0.2F;
                        if (flag) {
                            if (matrix3x2f == null) {
                                matrix3x2f = new Matrix3x2f(matrix3x2fc);
                                screenRect = GuiBoundsUtil.getScissor(drawContext);
                            }

                            int i9 = (int)l2;
                            int i10 = ColorUtil.setAlpha(ColorUtil.WHITE, ColorUtil.alpha(i5));
                            if (flag2) {
                                i10 = HorizontalFadeMask.applyAlpha(i10, f14 + f13 * 0.5F);
                            }

                            emojiRenderer.drawSprite(drawContext, matrix3x2f, screenRect, f14, f15 - f13, f14 + f13, f15, i9 >>> 16, i9 & 65535, i10);
                        }

                        f3 += f * 0.15F + f13;
                        i6 = 0;
                        i7 += (int)(l2 >>> 32);
                    } else {
                        MsdfGlyph msdfGlyph = this.glyphs.get(i8);
                        if (msdfGlyph != null) {
                            if (i6 != 0) {
                                f3 += this.getKerning(i6, i8) * f;
                            }

                            float f16 = msdfGlyph.getWidth() * f;
                            float f17 = msdfGlyph.getHeight() * f;
                            if (f16 > 0.0F && f17 > 0.0F) {
                                float f18 = f3 + msdfGlyph.getLeft() * f;
                                float f19 = f4 - (msdfGlyph.getTop() + 0.125F) * f;
                                int i11 = i4 * 8;
                                floats[i11] = f18;
                                floats[i11 + 1] = f19;
                                floats[i11 + 2] = f18 + f16;
                                floats[i11 + 3] = f19 + f17;
                                floats[i11 + 4] = msdfGlyph.getU0();
                                floats[i11 + 5] = msdfGlyph.getV0();
                                floats[i11 + 6] = msdfGlyph.getU1();
                                floats[i11 + 7] = msdfGlyph.getV1();
                                ints[i4] = flag2 ? HorizontalFadeMask.applyAlpha(i5, f18 + f16 * 0.5F) : i5;
                                i4++;
                                f8 = Math.min(f8, f18);
                                f9 = Math.min(f9, f19);
                                f10 = Math.max(f10, f18 + f16);
                                f11 = Math.max(f11, f19 + f17);
                            }

                            f3 += msdfGlyph.getAdvance() * f + f5 + f2;
                            i6 = i8;
                        }

                        i7 += Character.charCount(i8);
                    }
                }
            }

            if (i4 > 0) {
                MsdfGlyphBatch.submit(
                    drawContext,
                    textureSetup,
                    matrix3x2fc,
                    floats,
                    ints,
                    i4,
                    f8,
                    f9,
                    f10,
                    f11,
                    this.atlas.getWidth(),
                    this.atlas.getHeight(),
                    this.atlas.getDistanceRange()
                );
                return f3;
            }
        }

        return f3;
    }

    private TextureSetup getTextureSetup() {
        GpuTextureView gpuTextureView = ResourceUtil.getTextureView(this.texture);
        return gpuTextureView == null ? null : TextureSetup.of(gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
    }

    public float getWidth(String string, float f, float f2) {
        if (string != null && !string.isEmpty()) {
            float f3 = 0.0F;
            float f4 = 0.0F;
            int i = 0;
            int i2 = string.length();
            EmojiRenderer emojiRenderer = EmojiRenderer.getInstance();
            int i3 = 0;

            while (i3 < i2) {
                char c = string.charAt(i3);
                if (c == 167 && i3 + 1 < i2 && isFormattingCode(string.charAt(i3 + 1))) {
                    i3 += 2;
                } else {
                    if (c == '$' && i3 + 1 < i2 && string.charAt(i3 + 1) == '{') {
                        long l = parseColorTag(string, i3, 0, 1.0F);
                        if (l >= 0L) {
                            i3 = (int)(l >>> 32);
                            continue;
                        }
                    }

                    int i4 = string.codePointAt(i3);
                    long l2 = emojiRenderer.matchEmoji(string, i3, i4);
                    if (l2 != -1L) {
                        float f5 = f * 1.2F;
                        f4 = Math.max(f4, f3 + f * 0.05F + f5);
                        f3 += f * 0.15F + f5;
                        i = 0;
                        i3 += (int)(l2 >>> 32);
                    } else {
                        MsdfGlyph msdfGlyph = this.glyphs.get(i4);
                        if (msdfGlyph != null) {
                            if (i != 0) {
                                f3 += this.getKerning(i, i4) * f;
                            }

                            f4 = Math.max(f4, f3 + (msdfGlyph.getLeft() + msdfGlyph.getWidth()) * f);
                            f3 += msdfGlyph.getAdvance() * f + f2;
                            i = i4;
                        }

                        i3 += Character.charCount(i4);
                    }
                }
            }

            float f6 = Math.max(f3 - f2, 0.0F);
            return Math.max(f6, f4);
        } else {
            return 0.0F;
        }
    }

    public float getWidth(String string, float f) {
        return this.getWidth(string, f, 0.0F);
    }

    public float getLineHeight(float f) {
        return this.metrics.getLineHeight() * f;
    }

    public float getHeight(String string, float f) {
        float f2 = this.metrics.getLineHeight() * f;
        if (string != null && !string.isEmpty()) {
            int i = string.length();
            EmojiRenderer emojiRenderer = EmojiRenderer.getInstance();
            int i2 = 0;

            while (i2 < i) {
                char c = string.charAt(i2);
                if (c == 167 && i2 + 1 < i && isFormattingCode(string.charAt(i2 + 1))) {
                    i2 += 2;
                } else {
                    int i3 = string.codePointAt(i2);
                    long l = emojiRenderer.matchEmoji(string, i2, i3);
                    if (l != -1L) {
                        f2 = Math.max(f2, f * 1.2F);
                        i2 += (int)(l >>> 32);
                    } else {
                        MsdfGlyph msdfGlyph = this.glyphs.get(i3);
                        if (msdfGlyph != null) {
                            f2 = Math.max(f2, msdfGlyph.getScaledHeight(f));
                        }

                        i2 += Character.charCount(i3);
                    }
                }
            }
        }

        return f2;
    }

    public MsdfFontData.Metrics getMetrics() {
        return this.metrics;
    }

    public void destroy() {
        ResourceUtil.destroyTexture(this.texture);
    }

    public static MsdfAtlasFont.Builder builder() {
        return new MsdfAtlasFont.Builder();
    }

    private static boolean isFormattingCode(char c) {
        char c2 = Character.toLowerCase(c);
        return c2 >= '0' && c2 <= '9' || c2 >= 'a' && c2 <= 'f' || c2 >= 'k' && c2 <= 'o' || c2 == 'r';
    }

    private static long parseColorTag(String string, int i, int i2, float f) {
        int i3 = i + 2;
        if (string.regionMatches(true, i3, "reset}", 0, 6)) {
            return pack(i3 + 6, i2);
        }

        boolean flag;
        if (string.regionMatches(true, i3, "rgba(", 0, 5)) {
            flag = true;
            i3 += 5;
        } else {
            if (!string.regionMatches(true, i3, "rgb(", 0, 4)) {
                return -1L;
            }

            flag = false;
            i3 += 4;
        }

        long l = parseNumber(string, i3);
        if (l >= 0L) {
            int i4;
            if (!charAtEquals(string, i4 = (int)(l >>> 32), ',')) {
                return -1L;
            }

            long l2 = parseNumber(string, i4 + 1);
            if (l2 >= 0L) {
                int i5;
                if (!charAtEquals(string, i5 = (int)(l2 >>> 32), ',')) {
                    return -1L;
                }

                long l3 = parseNumber(string, i5 + 1);
                if (l3 < 0L) {
                    return -1L;
                }

                int i6 = (int)(l3 >>> 32);
                int i7 = ColorUtil.alpha(i2);
                if (flag && charAtEquals(string, i6, ',')) {
                    long l4 = parseNumber(string, i6 + 1);
                    if (l4 < 0L) {
                        return -1L;
                    }

                    i6 = (int)(l4 >>> 32);
                    i7 = Math.round((int)l4 * f);
                }

                if (charAtEquals(string, i6, ')')) {
                    if (!charAtEquals(string, i6 + 1, '}')) {
                        return -1L;
                    }

                    return pack(i6 + 2, ColorUtil.rgba((int)l, (int)l2, (int)l3, i7));
                }
            }
        }

        return -1L;
    }

    private static long parseNumber(String string, int i) {
        int i2 = 0;

        int i3;
        for (i3 = 0; i < string.length() && i3 < 3; i3++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                break;
            }

            i2 = i2 * 10 + (c - '0');
            i++;
        }

        return i3 == 0 ? -1L : pack(i, i2);
    }

    private static boolean charAtEquals(String string, int i, char c) {
        return i < string.length() && string.charAt(i) == c;
    }

    private static long pack(int i, int i2) {
        return (long)i << 32 | i2 & 4294967295L;
    }

    private float getKerning(int i, int i2) {
        return this.kerning.get((long)i << 32 | i2 & 4294967295L);
    }

    public static final class Builder {
        private static final String FONTS_PATH = "fonts/";
        private Identifier dataId;
        private Identifier atlasId;
        private Path dataPath;
        private Path atlasPath;

        Builder() {
        }

        public MsdfAtlasFont.Builder data(String string) {
            this.dataId = Identifier.of(ClientInfo.MOD_ID, "fonts/" + string);
            return this;
        }

        public MsdfAtlasFont.Builder atlas(String string) {
            this.atlasId = Identifier.of(ClientInfo.MOD_ID, "fonts/" + string);
            return this;
        }

        public MsdfAtlasFont.Builder files(Path path, Path path2) {
            this.atlasPath = path;
            this.dataPath = path2;
            return this;
        }

        public MsdfAtlasFont build() {
            MsdfFontData msdfFontData = this.dataPath != null ? this.readDataFile() : ResourceUtil.readJson(this.dataId, MsdfFontData.class);
            if (msdfFontData == null) {
                throw new RuntimeException("Failed to read font data file: " + (this.dataPath != null ? this.dataPath : this.dataId));
            }

            float f = msdfFontData.getAtlas().getWidth();
            float f2 = msdfFontData.getAtlas().getHeight();
            Int2ObjectOpenHashMap int2ObjectOpenHashMap = new Int2ObjectOpenHashMap(msdfFontData.getGlyphs().size());

            for (MsdfFontData.Glyph glyph : msdfFontData.getGlyphs()) {
                int2ObjectOpenHashMap.put(glyph.getUnicode(), new MsdfGlyph(glyph, f, f2));
            }

            Long2FloatOpenHashMap long2FloatOpenHashMap = new Long2FloatOpenHashMap(msdfFontData.getKerning() != null ? msdfFontData.getKerning().size() : 0);
            long2FloatOpenHashMap.defaultReturnValue(0.0F);
            if (msdfFontData.getKerning() != null) {
                for (MsdfFontData.KerningPair kerningPair : msdfFontData.getKerning()) {
                    long2FloatOpenHashMap.put((long)kerningPair.getUnicode1() << 32 | kerningPair.getUnicode2() & 4294967295L, kerningPair.getAdvance());
                }
            }

            String string = this.atlasPath != null ? this.atlasPath.getFileName().toString() : this.atlasId.getPath();
            NativeImage nativeImage = this.atlasPath != null ? this.readAtlasFile() : ResourceUtil.readImage(this.atlasId);
            NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(() -> string, nativeImage);
            Identifier identifier = Identifier.of(ClientInfo.MOD_ID, "dynamic/font/" + Integer.toHexString(string.hashCode()) + "_" + System.nanoTime());
            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, nativeImageBackedTexture);
            return new MsdfAtlasFont(identifier, msdfFontData.getAtlas(), msdfFontData.getMetrics(), int2ObjectOpenHashMap, long2FloatOpenHashMap);
        }

        private MsdfFontData readDataFile() {
            try {
                return new Gson().fromJson(Files.readString(this.dataPath), MsdfFontData.class);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to read font data file: " + this.dataPath, exception);
            }
        }

        private NativeImage readAtlasFile() {
            try (InputStream inputStream = Files.newInputStream(this.atlasPath)) {
                return NativeImage.read(inputStream);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to read font atlas: " + this.atlasPath, exception);
            }
        }
    }
}
