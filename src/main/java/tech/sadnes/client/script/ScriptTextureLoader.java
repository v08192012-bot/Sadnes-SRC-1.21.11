package tech.sadnes.client.script;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.render.GuiRenderUtil;

public final class ScriptTextureLoader {
    private static final Map<String, ScriptTextureLoader.ScriptTexture> CACHE = new ConcurrentHashMap<>();

    private ScriptTextureLoader() {
    }

    public static boolean bind(String string) {
        ScriptTextureLoader.ScriptTexture scriptTexture = load(string);
        if (scriptTexture != null && scriptTexture.identifier() != null) {
            GuiRenderUtil.bindTexture(scriptTexture.identifier());
            return GuiRenderUtil.getBoundTexture() != null;
        } else {
            return false;
        }
    }

    public static ScriptTextureLoader.ScriptTexture load(String string) {
        if (string != null && !string.trim().isEmpty()) {
            String string2 = string.trim();
            if (string2.contains(":") && !string2.contains("\\") && string2.indexOf(58) > 1) {
                try {
                    Identifier identifier = Identifier.of(string2);
                    AbstractTexture abstractTexture = MinecraftClient.getInstance().getTextureManager().getTexture(identifier);
                    if (abstractTexture != null) {
                        return new ScriptTextureLoader.ScriptTexture(identifier, 0, 0, 0, 0, 0, 60.0F, 0L);
                    }
                } catch (Exception exception) {
                }
            }

            Path path = resolvePath(string2);
            if (path != null && Files.isRegularFile(path)) {
                String string3 = path.toAbsolutePath().normalize().toString();
                long l = 0L;

                try {
                    l = Files.getLastModifiedTime(path).toMillis();
                } catch (Exception exception2) {
                }

                ScriptTextureLoader.ScriptTexture scriptTexture = CACHE.get(string3);
                if (scriptTexture != null && scriptTexture.lastModified() == l) {
                    return scriptTexture;
                }

                try {
                    byte[] bytes = Files.readAllBytes(path);
                    NativeImage nativeImage = decode(string2, bytes);
                    if (nativeImage == null) {
                        return null;
                    }

                    String string4 = "sadnes_js_tex_" + Integer.toHexString(string3.hashCode());
                    Identifier identifier2 = Identifier.of(ClientInfo.MOD_ID, "dynamic/scripts/" + string4);
                    NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(() -> ClientInfo.MOD_ID + "_" + string4, nativeImage);
                    MinecraftClient.getInstance().getTextureManager().registerTexture(identifier2, nativeImageBackedTexture);
                    int i = 0;
                    int i2 = 0;
                    int i3 = 0;
                    float f = 30.0F;
                    int i4 = path.toString().lastIndexOf(46);
                    if (i4 > 0) {
                        Path path2 = Path.of(path.toString().substring(0, i4) + ".meta");
                        Script script = Script.current();
                        if (script != null) {
                            Path path3 = script.getAssetsRoot().toAbsolutePath().normalize();
                            path2 = ScriptPathValidator.resolveSafe(path3, path3.relativize(path2.toAbsolutePath().normalize()).toString());
                        }

                        if (Files.exists(path2)) {
                            try {
                                for (String string5 : Files.readAllLines(path2)) {
                                    String string6 = string5.trim();
                                    if (string6.startsWith("cols=")) {
                                        i = Integer.parseInt(string6.substring(5).trim());
                                    } else if (string6.startsWith("rows=")) {
                                        i2 = Integer.parseInt(string6.substring(5).trim());
                                    } else if (string6.startsWith("frames=")) {
                                        i3 = Integer.parseInt(string6.substring(7).trim());
                                    } else if (string6.startsWith("fps=")) {
                                        f = Float.parseFloat(string6.substring(4).trim());
                                    }
                                }
                            } catch (Exception exception3) {
                            }
                        }
                    }

                    ScriptTextureLoader.ScriptTexture scriptTexture2 = new ScriptTextureLoader.ScriptTexture(
                        identifier2, nativeImage.getWidth(), nativeImage.getHeight(), i, i2, i3, f, l
                    );
                    CACHE.put(string3, scriptTexture2);
                    return scriptTexture2;
                } catch (Exception exception4) {
                    Sadnes.getLogger().warn("[ScriptTexture] Не удалось загрузить {}: {}", string2, exception4.getMessage());
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static Path resolvePath(String string) {
        Script script = Script.current();
        if (script != null) {
            Path path = ScriptPathValidator.resolveSafe(script.getAssetsDir(), string);
            return Files.exists(path) ? path : ScriptPathValidator.resolveSafe(script.getAssetsRoot(), string);
        }

        Path path2 = Path.of(string);
        if (path2.isAbsolute()) {
            return path2;
        }

        Path path3 = MinecraftClient.getInstance().runDirectory.toPath();
        Path path4 = path3.resolve(ClientInfo.MOD_ID).resolve(string);
        if (Files.exists(path4)) {
            return path4;
        }

        Path path5 = path3.resolve(string);
        return Files.exists(path5) ? path5 : path2;
    }

    private static NativeImage decode(String string, byte[] bytes) {
        try {
            return isPng(bytes) ? NativeImage.read(new ByteArrayInputStream(bytes)) : decodeWithStb(string, bytes);
        } catch (Exception exception) {
            return decodeWithStb(string, bytes);
        }
    }

    private static boolean isPng(byte[] bytes) {
        return bytes.length >= 8
            && (bytes[0] & 255) == 137
            && bytes[1] == 80
            && bytes[2] == 78
            && bytes[3] == 71
            && (bytes[4] & 255) == 13
            && (bytes[5] & 255) == 10
            && (bytes[6] & 255) == 26
            && (bytes[7] & 255) == 10;
    }

    private static NativeImage decodeWithStb(String string, byte[] bytes) {
        ByteBuffer byteBuffer = MemoryUtil.memAlloc(bytes.length);

        NativeImage nativeImage;
        try {
            byteBuffer.put(bytes).flip();

            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                IntBuffer intBuffer = memoryStack.mallocInt(1);
                IntBuffer intBuffer2 = memoryStack.mallocInt(1);
                IntBuffer intBuffer3 = memoryStack.mallocInt(1);
                ByteBuffer byteBuffer2 = STBImage.stbi_load_from_memory(byteBuffer, intBuffer, intBuffer2, intBuffer3, 4);
                if (byteBuffer2 == null) {
                    Sadnes.getLogger().warn("[ScriptTexture] STB не смог декодировать {}: {}", string, STBImage.stbi_failure_reason());
                    return null;
                }

                try {
                    int i = intBuffer.get(0);
                    int i2 = intBuffer2.get(0);
                    nativeImage = new NativeImage(i, i2, false);

                    for (int i3 = 0; i3 < i2; i3++) {
                        for (int i4 = 0; i4 < i; i4++) {
                            int i5 = (i4 + i3 * i) * 4;
                            int i6 = byteBuffer2.get(i5) & 255;
                            int i7 = byteBuffer2.get(i5 + 1) & 255;
                            int i8 = byteBuffer2.get(i5 + 2) & 255;
                            int i9 = byteBuffer2.get(i5 + 3) & 255;
                            nativeImage.setColorArgb(i4, i3, i9 << 24 | i6 << 16 | i7 << 8 | i8);
                        }
                    }
                } finally {
                    STBImage.stbi_image_free(byteBuffer2);
                }
            }
        } finally {
            MemoryUtil.memFree(byteBuffer);
        }

        return nativeImage;
    }

    public record ScriptTexture(Identifier identifier, int width, int height, int cols, int rows, int frames, float fps, long lastModified) {
    }
}
