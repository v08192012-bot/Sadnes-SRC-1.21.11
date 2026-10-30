package tech.sadnes.client.script;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.render.shader.GlStateSnapshot;
import tech.sadnes.client.render.shader.ScriptShader;

public final class ScriptShaders {
    private static final String SHADER_PREFIX = "script_fx/";
    private static final Identifier FX_VERTEX_SHADER = Identifier.of(ClientInfo.MOD_ID, "core/world/script_fx");
    private static final Identifier CHAMS_VERTEX_SHADER = Identifier.of(ClientInfo.MOD_ID, "core/chams/color");
    private static final Identifier GLOW_VERTEX_SHADER = Identifier.of(ClientInfo.MOD_ID, "core/glow/composite");
    private static final Map<Identifier, String> SHADER_SOURCES = new ConcurrentHashMap<>();
    private static final Map<String, ScriptShaders.CachedShader> CACHE = new ConcurrentHashMap<>();

    private ScriptShaders() {
    }

    public static ScriptShader getShader(ScriptShaders.ShaderKind shaderKind, Script script, Path path) {
        if (path != null && Files.isRegularFile(path)) {
            String string = path.toAbsolutePath().normalize().toString();
            String string2 = shaderKind + ":" + string;
            long l = 0L;

            try {
                l = Files.getLastModifiedTime(path).toMillis();
            } catch (IOException ioException) {
            }

            ScriptShaders.CachedShader cachedShader = CACHE.get(string2);
            if (cachedShader != null && cachedShader.modified() == l) {
                return cachedShader.shader();
            }

            String string3;
            try {
                string3 = Files.readString(path);
            } catch (IOException ioException2) {
                Sadnes.getLogger().warn("[ScriptShader] {}: {}", path.getFileName(), ioException2.getMessage());
                return null;
            }

            String string4 = Integer.toHexString(string.hashCode()) + "_" + l;
            Identifier identifier = Identifier.of(ClientInfo.MOD_ID, "script_fx/" + string4);
            SHADER_SOURCES.put(identifier, string3);
            Identifier identifier2 = Identifier.of(ClientInfo.MOD_ID, "pipeline/script_" + shaderKind.name().toLowerCase(Locale.ROOT) + "/" + string4);

            RenderPipeline renderPipeline = switch (shaderKind) {
                case FX -> createFxPipeline(identifier2, identifier);
                case CHAMS -> createChamsPipeline(identifier2, identifier);
                case GLOW -> createGlowPipeline(identifier2, identifier);
            };

            boolean flag = switch (shaderKind) {
                case FX -> false;
                case CHAMS -> declaresSampler(string3, "Sampler1") || declaresSampler(string3, "Sampler2");
                case GLOW -> declaresSampler(string3, "Sampler2");
            };
            if (RenderSystem.tryGetDevice() == null) {
                return new ScriptShader(renderPipeline, flag);
            }

            ScriptShader scriptShader = precompile(renderPipeline, script, path, string3) ? new ScriptShader(renderPipeline, flag) : null;
            CACHE.put(string2, new ScriptShaders.CachedShader(scriptShader, l));
            return scriptShader;
        } else {
            return null;
        }
    }

    private static boolean declaresSampler(String string, String string2) {
        return Pattern.compile("sampler2D\\s+" + string2 + "\\b").matcher(string).find();
    }

    private static RenderPipeline createFxPipeline(Identifier identifier, Identifier identifier2) {
        return RenderPipeline.builder()
            .withLocation(identifier)
            .withVertexShader(FX_VERTEX_SHADER)
            .withFragmentShader(identifier2)
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("SadnesScriptFx", UniformType.UNIFORM_BUFFER)
            .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
            .build();
    }

    private static RenderPipeline createChamsPipeline(Identifier identifier, Identifier identifier2) {
        return RenderPipeline.builder()
            .withLocation(identifier)
            .withVertexShader(CHAMS_VERTEX_SHADER)
            .withFragmentShader(identifier2)
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withSampler("Sampler2")
            .withSampler("Sampler3")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("SadnesScriptFx", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
            .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
            .build();
    }

    private static RenderPipeline createGlowPipeline(Identifier identifier, Identifier identifier2) {
        return RenderPipeline.builder()
            .withLocation(identifier)
            .withVertexShader(GLOW_VERTEX_SHADER)
            .withFragmentShader(identifier2)
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withSampler("Sampler2")
            .withUniform("SadnesScriptFx", UniformType.UNIFORM_BUFFER)
            .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
            .build();
    }

    private static boolean precompile(RenderPipeline renderPipeline, Script script, Path path, String string) {
        String string2;
        try {
            if (RenderSystem.getDevice().precompilePipeline(renderPipeline, null).isValid()) {
                return true;
            }

            String string3 = GlStateSnapshot.isOpenGl() ? getCompileError(string) : null;
            string2 = string3 != null ? string3 : "не связался с вершинником клиента — сверь in-переменные и uniform-блоки с шаблоном";
        } catch (RuntimeException exception) {
            string2 = String.valueOf(exception.getMessage());
        }

        String string4 = "шейдер " + path.getFileName() + ": " + string2;
        Sadnes.getLogger().warn("[ScriptShader] {}", string4);
        if (script != null) {
            ScriptConsole.error(script.getDisplayName(), string4);
        }

        return false;
    }

    private static String getCompileError(String string) {
        int i = GlStateManager.glCreateShader(35632);

        try {
            GlStateManager.glShaderSource(i, string);
            GlStateManager.glCompileShader(i);
            if (GlStateManager.glGetShaderi(i, 35713) == 0) {
                return GlStateManager.glGetShaderInfoLog(i, 32768).trim();
            }
        } finally {
            GlStateManager.glDeleteShader(i);
        }

        return null;
    }

    public static String getShaderSource(Identifier identifier, ShaderType shaderType) {
        return identifier != null && shaderType == ShaderType.FRAGMENT ? SHADER_SOURCES.get(identifier) : null;
    }

    record CachedShader(ScriptShader shader, long modified) {
    }

    public enum ShaderKind {
        FX,
        CHAMS,
        GLOW;
    }
}
