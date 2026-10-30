package tech.sadnes.client.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class ResourceUtil {
    private static final Gson GSON = new Gson();

    private ResourceUtil() {
    }

    public static String readText(Identifier identifier) {
        return readLines(identifier, "\n");
    }

    public static String readLines(Identifier identifier, String string) {
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        Optional optional = resourceManager.getResource(identifier);
        if (optional.isEmpty()) {
            return "";
        }

        try (
            InputStream inputStream = ((Resource)optional.get()).getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        ) {
            return bufferedReader.lines().collect(Collectors.joining(string));
        } catch (IOException ioException) {
            ioException.printStackTrace();
            return "";
        }
    }

    public static <T> T readJson(Identifier identifier, Class<T> clazz) {
        try {
            String string = readText(identifier);
            return string.isEmpty() ? null : GSON.fromJson(string, clazz);
        } catch (JsonSyntaxException jsonSyntaxException) {
            jsonSyntaxException.printStackTrace();
            return null;
        }
    }

    public static NativeImage readImage(Identifier identifier) {
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        Optional optional = resourceManager.getResource(identifier);
        if (optional.isEmpty()) {
            throw new RuntimeException("Unable to find texture resource: " + identifier);
        }

        try (InputStream inputStream = ((Resource)optional.get()).getInputStream()) {
            return NativeImage.read(inputStream);
        } catch (IOException ioException) {
            throw new RuntimeException("Unable to load texture resource: " + identifier, ioException);
        }
    }

    public static int getGlId(Identifier identifier) {
        if (identifier != null && !identifier.getPath().isEmpty()) {
            AbstractTexture abstractTexture = MinecraftClient.getInstance().getTextureManager().getTexture(identifier);
            if (abstractTexture.getGlTexture() instanceof GlTexture glTexture && !glTexture.isClosed()) {
                return glTexture.getGlId();
            } else {
                throw new RuntimeException("Unable to resolve GL texture for resource: " + identifier);
            }
        } else {
            throw new IllegalArgumentException("Invalid texture identifier: " + identifier);
        }
    }

    public static GpuTextureView getTextureView(Identifier identifier) {
        return identifier == null ? null : getView(MinecraftClient.getInstance().getTextureManager().getTexture(identifier));
    }

    public static GpuTextureView getView(AbstractTexture abstractTexture) {
        if (abstractTexture == null) {
            return null;
        }

        try {
            GpuTextureView gpuTextureView = abstractTexture.getGlTextureView();
            return gpuTextureView.isClosed() ? null : gpuTextureView;
        } catch (IllegalStateException illegalStateException) {
            return null;
        }
    }

    public static void destroyTexture(Identifier identifier) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        TextureManager textureManager = minecraftClient.getTextureManager();
        textureManager.destroyTexture(identifier);
    }
}
