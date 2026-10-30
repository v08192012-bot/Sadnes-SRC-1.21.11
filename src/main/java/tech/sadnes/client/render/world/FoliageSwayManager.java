package tech.sadnes.client.render.world;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;
import tech.sadnes.client.feature.impl.render.ambience.AmbienceProfile;
import tech.sadnes.client.util.ResourceUtil;

public enum FoliageSwayManager {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(FoliageSwayManager.class);
    public static final String UNIFORM = "SadnesFoliage";
    private static final Identifier TERRAIN_SHADER = Identifier.ofVanilla("core/terrain");
    private static final Identifier SWAY_INCLUDE = Identifier.of(ClientInfo.MOD_ID, "shaders/include/foliage_sway.glsl");
    private static final String SODIUM_VERTEX_SHADER = "blocks/block_layer_opaque.vsh";
    private static final String POSITION_ANCHOR = "vec3 position = _vert_position + translation;";
    private static final String MAIN_ANCHOR = "void main()";
    private static final String WIND_UNIFORM = "u_SadnesFoliageWind";
    private static final String CAMERA_UNIFORM = "u_SadnesFoliageCamera";
    private static final double TIME_WRAP = 1024.0;
    private static final int CAMERA_WRAP = 4096;
    private static final int BUFFER_SIZE = 16;
    private GpuBuffer uniformBuffer;
    private long lastNanos;
    private double time;
    private float windX;
    private float windZ;
    private float strength;

    public static boolean isTerrainShader(Optional<Identifier> optionalIdentifier) {
        return optionalIdentifier.isPresent() && TERRAIN_SHADER.equals(optionalIdentifier.get());
    }

    public void update() {
        long l = System.nanoTime();
        double d = this.lastNanos == 0L ? 0.0 : Math.min((l - this.lastNanos) / 1.0E9, 0.1);
        this.lastNanos = l;
        this.time = (this.time + d) % 1024.0;
        Ambience ambience = Ambience.getInstance();
        AmbienceProfile ambienceProfile = ambience == null ? null : ambience.getActiveProfile();
        boolean flag = ambience != null && ambience.isEnabled() && ambienceProfile.isWindEnabled() && ambienceProfile.windFoliage.getValue();
        this.strength = flag ? ambienceProfile.windStrength.getValue() / 100.0F : 0.0F;
        if (flag) {
            float f = (float)Math.toRadians(ambienceProfile.windDirection.getValue().floatValue());
            this.windX = -MathHelper.sin(f);
            this.windZ = MathHelper.cos(f);
        }

        if (this.uniformBuffer == null || this.uniformBuffer.isClosed()) {
            this.uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Sadnes foliage sway", 136, 16L);
        }

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, 16).putVec4(this.windX, this.windZ, this.strength, (float)this.time).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.uniformBuffer.slice(), byteBuffer);
        }
    }

    public void bindUniform(RenderPass renderPass) {
        if (this.uniformBuffer != null && !this.uniformBuffer.isClosed()) {
            renderPass.setUniform("SadnesFoliage", this.uniformBuffer);
        }
    }

    public void uploadSodiumUniforms() {
        int i = GL11.glGetInteger(35725);
        int i2 = GL20.glGetUniformLocation(i, "u_SadnesFoliageWind");
        if (i2 >= 0) {
            GL20.glUniform4f(i2, this.windX, this.windZ, this.strength, (float)this.time);
            int i3 = GL20.glGetUniformLocation(i, "u_SadnesFoliageCamera");
            if (i3 >= 0) {
                Vec3d vec3d = MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos();
                int i4 = MathHelper.floor(vec3d.x);
                int i5 = MathHelper.floor(vec3d.y);
                int i6 = MathHelper.floor(vec3d.z);
                GL20.glUniform3f(
                    i3,
                    (float)(Math.floorMod(i4, 4096) + (vec3d.x - i4)),
                    (float)(Math.floorMod(i5, 4096) + (vec3d.y - i5)),
                    (float)(Math.floorMod(i6, 4096) + (vec3d.z - i6))
                );
            }
        }
    }

    public static String patchSodiumShader(Identifier identifier, String string) {
        if (string != null && "sodium".equals(identifier.getNamespace()) && identifier.getPath().endsWith("blocks/block_layer_opaque.vsh")) {
            int i = string.indexOf("void main()");
            int i2 = string.indexOf("vec3 position = _vert_position + translation;");
            String string2 = ResourceUtil.readText(SWAY_INCLUDE);
            if (i >= 0 && i2 >= i && !string2.isEmpty()) {
                int i3 = i2 + "vec3 position = _vert_position + translation;".length();
                return string.substring(0, i)
                    + "uniform vec4 u_SadnesFoliageWind;\nuniform vec3 u_SadnesFoliageCamera;\n"
                    + string2
                    + "\n"
                    + string.substring(i, i3)
                    + "\n    position += sadnes_foliageSway(position + u_SadnesFoliageCamera, _vert_color.a, u_SadnesFoliageWind);"
                    + string.substring(i3);
            } else {
                LOGGER.warn("Sodium chunk shader {} has unexpected layout, foliage sway is disabled", identifier);
                return string;
            }
        } else {
            return string;
        }
    }

    public void close() {
        if (this.uniformBuffer != null) {
            this.uniformBuffer.close();
        }

        this.uniformBuffer = null;
    }
}
