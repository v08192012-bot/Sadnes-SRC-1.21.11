package tech.sadnes.client.render.font;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import tech.sadnes.api.mixin.accessor.DrawContextAccessor;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.render.GuiBoundsUtil;
import tech.sadnes.client.util.ResourceUtil;

public final class EmojiRenderer {
    private static final EmojiRenderer INSTANCE = new EmojiRenderer();
    private static final int SHEET_SIZE = 2108;
    private static final int CELL_STRIDE = 34;
    private static final int CELL_SIZE = 32;
    private static final int CELL_PADDING = 1;
    private static final int MAX_SEQUENCE_LENGTH = 16;
    private static final Identifier SHEET_TEXTURE = Identifier.of(ClientInfo.MOD_ID, "emoji_apple_sheet");
    private static final Identifier DATA_FILE = Identifier.of(ClientInfo.MOD_ID, "fonts/emoji.json");
    private static final Identifier SHEET_IMAGE = Identifier.of(ClientInfo.MOD_ID, "fonts/emoji_apple_32.png");
    private static final Identifier PRESENTATION_FILE = Identifier.of(ClientInfo.MOD_ID, "fonts/emoji_presentation.txt");
    private final Object2IntOpenHashMap<String> sheetPositions = new Object2IntOpenHashMap<>();
    private final IntOpenHashSet leadingCodepoints = new IntOpenHashSet();
    private int[] presentationRanges = new int[0];
    private int maxSequenceLength = 1;
    private GpuSampler sampler;
    private boolean loaded;

    private EmojiRenderer() {
        this.sheetPositions.defaultReturnValue(-1);
    }

    public static EmojiRenderer getInstance() {
        return INSTANCE;
    }

    public void load() {
        if (!this.loaded) {
            EmojiRenderer.EmojiData[] emojiDatas = ResourceUtil.readJson(DATA_FILE, EmojiRenderer.EmojiData[].class);
            if (emojiDatas == null) {
                throw new RuntimeException("Failed to read emoji data file: " + DATA_FILE);
            }

            for (EmojiRenderer.EmojiData emojiData : emojiDatas) {
                this.registerEmoji(emojiData.unified, emojiData.hasImage, emojiData.sheetX, emojiData.sheetY);
                if (emojiData.skinVariations != null) {
                    for (EmojiRenderer.SkinVariation skinVariation : emojiData.skinVariations.values()) {
                        this.registerEmoji(skinVariation.unified, skinVariation.hasImage, skinVariation.sheetX, skinVariation.sheetY);
                    }
                }
            }

            this.loadPresentationRanges();
            NativeImage nativeImage = ResourceUtil.readImage(SHEET_IMAGE);
            NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(SHEET_IMAGE::toString, nativeImage);
            MinecraftClient.getInstance().getTextureManager().registerTexture(SHEET_TEXTURE, nativeImageBackedTexture);
            this.loaded = true;
        }
    }

    public void reload() {
        this.sheetPositions.clear();
        this.leadingCodepoints.clear();
        this.maxSequenceLength = 1;
        this.loaded = false;
        this.load();
    }

    private void registerEmoji(String string, boolean flag, int i, int i2) {
        if (flag && string != null) {
            String[] strings = string.split("-");
            int i3 = strings.length;
            StringBuilder builder = new StringBuilder(i3 * 5);
            int i4 = 0;

            for (int i5 = 0; i5 < i3; i5++) {
                int i6 = Integer.parseInt(strings[i5], 16);
                if (i5 == 0) {
                    i4 = i6;
                } else {
                    builder.append('-');
                }

                builder.append(Integer.toHexString(i6));
            }

            this.sheetPositions.put(builder.toString(), i << 16 | i2 & 65535);
            this.leadingCodepoints.add(i4);
            if (i3 > this.maxSequenceLength) {
                this.maxSequenceLength = Math.min(i3, 16);
            }
        }
    }

    private void loadPresentationRanges() {
        String string = ResourceUtil.readText(PRESENTATION_FILE);
        if (!string.isEmpty()) {
            String[] strings = string.split("\n");
            int[] ints = new int[strings.length * 2];
            int i = 0;

            for (String string2 : strings) {
                String string3 = string2.trim();
                int i2 = string3.indexOf(45);
                if (i2 > 0) {
                    ints[i++] = Integer.parseInt(string3.substring(0, i2), 16);
                    ints[i++] = Integer.parseInt(string3.substring(i2 + 1), 16);
                }
            }

            if (i == ints.length) {
                this.presentationRanges = ints;
            } else {
                this.presentationRanges = Arrays.copyOf(ints, i);
            }
        }
    }

    public boolean isEmoji(String string) {
        this.load();
        return this.getSheetPosition(string) >= 0;
    }

    public boolean drawEmoji(DrawContext drawContext, String string, float f, float f2, float f3, int i) {
        this.load();
        int i2 = this.getSheetPosition(string);
        if (i2 < 0) {
            return false;
        }

        Matrix3x2f matrix3x2f = new Matrix3x2f(drawContext.getMatrices());
        this.drawSprite(drawContext, matrix3x2f, GuiBoundsUtil.getScissor(drawContext), f, f2, f + f3, f2 + f3, i2 >>> 16, i2 & 65535, i);
        return true;
    }

    public boolean isEmojiStart(int i) {
        if (!this.loaded) {
            this.load();
        }

        return this.leadingCodepoints.contains(i);
    }

    public long matchEmoji(String string, int i, int i2) {
        if (!this.loaded) {
            this.load();
        }

        if (!this.leadingCodepoints.contains(i2)) {
            return -1L;
        }

        int i3 = string.length();
        int i4 = i + Character.charCount(i2);
        int i5 = i4 < i3 ? string.codePointAt(i4) : -1;
        if (!isModifier(i5)) {
            if (!this.isPresentationDefault(i2)) {
                return -1L;
            }

            int i6 = this.sheetPositions.getInt(Integer.toHexString(i2));
            return i6 == -1 ? -1L : pack(Character.charCount(i2), i6);
        } else {
            int[] ints = new int[this.maxSequenceLength];
            int i7 = 0;
            int i8 = i;

            while (i8 < i3 && i7 < this.maxSequenceLength) {
                int i9 = string.codePointAt(i8);
                ints[i7++] = i9;
                i8 += Character.charCount(i9);
            }

            for (int i10 = i7; i10 >= 1; i10--) {
                String string2 = joinCodepoints(ints, i10);
                int i11 = this.sheetPositions.getInt(string2);
                if (i11 != -1 && (i10 != 1 || this.isPresentationDefault(i2))) {
                    int i12 = 0;

                    for (int i13 = 0; i13 < i10; i13++) {
                        i12 += Character.charCount(ints[i13]);
                    }

                    return pack(i12, i11);
                }
            }

            return -1L;
        }
    }

    public void drawSprite(DrawContext drawContext, Matrix3x2f matrix3x2f, ScreenRect screenRect, float f, float f2, float f3, float f4, int i, int i2, int i3) {
        this.load();
        GpuTextureView gpuTextureView = ResourceUtil.getTextureView(SHEET_TEXTURE);
        if (gpuTextureView != null) {
            TextureSetup textureSetup = TextureSetup.of(gpuTextureView, this.getSampler());
            float f5 = (i * 34 + 1) / 2108.0F;
            float f6 = (i * 34 + 1 + 32) / 2108.0F;
            float f7 = (i2 * 34 + 1) / 2108.0F;
            float f8 = (i2 * 34 + 1 + 32) / 2108.0F;
            GuiRenderState guiRenderState = ((DrawContextAccessor)drawContext).sadnes$getState();
            guiRenderState.addSimpleElement(
                new TexturedQuadGuiElementRenderState(
                    RenderPipelines.GUI_TEXTURED,
                    textureSetup,
                    matrix3x2f,
                    Math.round(f),
                    Math.round(f2),
                    Math.round(f3),
                    Math.round(f4),
                    f5,
                    f6,
                    f7,
                    f8,
                    i3,
                    screenRect
                )
            );
        }
    }

    private GpuSampler getSampler() {
        if (this.sampler == null) {
            this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        }

        return this.sampler;
    }

    private int getSheetPosition(String string) {
        String string2 = toUnifiedKey(string);
        if (string2 == null) {
            return -1;
        } else {
            int i = this.sheetPositions.getInt(string2);
            if (i != -1) {
                return i;
            } else {
                return string2.endsWith("-fe0f") ? this.sheetPositions.getInt(string2.substring(0, string2.length() - 5)) : this.sheetPositions.getInt(string2 + "-fe0f");
            }
        }
    }

    private boolean isPresentationDefault(int i) {
        int[] ints = this.presentationRanges;
        int i2 = 0;
        int i3 = ints.length / 2 - 1;

        while (i2 <= i3) {
            int i4 = i2 + i3 >>> 1;
            int i5 = ints[i4 * 2];
            int i6 = ints[i4 * 2 + 1];
            if (i < i5) {
                i3 = i4 - 1;
            } else {
                if (i <= i6) {
                    return true;
                }

                i2 = i4 + 1;
            }
        }

        return false;
    }

    private static boolean isModifier(int i) {
        return i == 65039
            || i == 8205
            || i == 8419
            || i >= 127995 && i <= 127999
            || i >= 127462 && i <= 127487
            || i >= 917536 && i <= 917631;
    }

    private static long pack(int i, int i2) {
        return (long)i << 32 | i2 & 4294967295L;
    }

    private static String joinCodepoints(int[] ints, int i) {
        StringBuilder builder = new StringBuilder(i * 5);

        for (int i2 = 0; i2 < i; i2++) {
            if (i2 > 0) {
                builder.append('-');
            }

            builder.append(Integer.toHexString(ints[i2]));
        }

        return builder.toString();
    }

    private static String toUnifiedKey(String string) {
        if (string != null && !string.isEmpty()) {
            int i = string.length();
            StringBuilder builder = new StringBuilder(i * 3);
            int i2 = 0;
            boolean flag = true;

            while (i2 < i) {
                int i3 = string.codePointAt(i2);
                if (!flag) {
                    builder.append('-');
                }

                builder.append(Integer.toHexString(i3));
                flag = false;
                i2 += Character.charCount(i3);
            }

            return builder.toString();
        } else {
            return null;
        }
    }

    static final class EmojiData {
        @SerializedName("unified")
        String unified;
        @SerializedName("sheet_x")
        int sheetX;
        @SerializedName("sheet_y")
        int sheetY;
        @SerializedName("has_img_apple")
        boolean hasImage;
        @SerializedName("skin_variations")
        Map<String, EmojiRenderer.SkinVariation> skinVariations;

        private EmojiData() {
        }
    }

    static final class SkinVariation {
        @SerializedName("unified")
        String unified;
        @SerializedName("sheet_x")
        int sheetX;
        @SerializedName("sheet_y")
        int sheetY;
        @SerializedName("has_img_apple")
        boolean hasImage;

        private SkinVariation() {
        }
    }
}
