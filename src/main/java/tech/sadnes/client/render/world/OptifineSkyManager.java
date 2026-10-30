package tech.sadnes.client.render.world;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;
import tech.sadnes.client.feature.impl.render.ambience.AmbienceProfile;

public final class OptifineSkyManager {
    private static final String[] SKY_PATHS = new String[]{"optifine/sky/world", "mcpatcher/sky/world"};
    private static final int MAX_WORLDS = 10;
    private static final int MAX_MISSING = 10;
    private static final Map<Integer, List<OptifineSkyLayer>> LAYERS = new HashMap<>();
    private static boolean loaded;

    private OptifineSkyManager() {
    }

    public static void reload() {
        LAYERS.clear();
        loaded = false;
    }

    public static boolean hasLayers(World world) {
        ensureLoaded();
        List list = LAYERS.get(getWorldIndex(world));
        return list != null && !list.isEmpty();
    }

    public static boolean shouldRender(Camera camera) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.world == null) {
            return false;
        }

        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.isEnabled()) {
            AmbienceProfile ambienceProfile = ambience.getActiveProfile();
            if (ambienceProfile.isSkyEnabled()) {
                if (!ambienceProfile.resourcePackSky.getValue()) {
                    return false;
                }

                if (hasLayers(minecraftClient.world) && !isVisionObstructed(camera)) {
                    return true;
                }

                return false;
            }
        }

        return false;
    }

    public static void render() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ClientWorld clientWorld = minecraftClient.world;
        if (clientWorld != null) {
            List<OptifineSkyLayer> optifineSkyLayers = LAYERS.get(getWorldIndex(clientWorld));
            if (optifineSkyLayers != null && !optifineSkyLayers.isEmpty()) {
                float f = minecraftClient.getRenderTickCounter().getTickProgress(false);
                long l = clientWorld.getTimeOfDay();
                int i = (int)(l % 24000L);
                float f2 = clientWorld.getRainGradient(f);
                float f3 = clientWorld.getThunderGradient(f);
                if (f2 > 0.0F) {
                    f3 /= f2;
                }

                float f4 = getCelestialAngle(l, f);
                ArrayList arrayList = new ArrayList(optifineSkyLayers.size());
                float[] floats = new float[optifineSkyLayers.size()];
                int i2 = 0;

                for (OptifineSkyLayer optifineSkyLayer : optifineSkyLayers) {
                    if (optifineSkyLayer.isActive(i)) {
                        float f5 = optifineSkyLayer.getBrightness(i, f2, f3);
                        if (!(f5 < 1.0E-4F)) {
                            arrayList.add(optifineSkyLayer);
                            floats[i2++] = f5;
                        }
                    }
                }

                if (!arrayList.isEmpty()) {
                    OptifineSkyRenderer.render(arrayList, floats, f4, clientWorld);
                }
            }
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();

            for (int i = 0; i < 10; i++) {
                List list = loadLayers(resourceManager, SKY_PATHS[0] + i);
                if (list.isEmpty()) {
                    list = loadLayers(resourceManager, SKY_PATHS[1] + i);
                }

                if (!list.isEmpty()) {
                    LAYERS.put(i, list);
                }
            }
        }
    }

    private static List<OptifineSkyLayer> loadLayers(ResourceManager resourceManager, String string) {
        ArrayList arrayList = new ArrayList();
        int i = 0;

        for (int i2 = 0; i <= 10; i2++) {
            Identifier identifier = parseIdentifier(string + "/sky" + i2 + ".properties");
            Optional optional = identifier == null ? Optional.empty() : resourceManager.getResource(identifier);
            if (optional.isEmpty()) {
                i++;
            } else {
                i = 0;
                OptifineSkyLayer optifineSkyLayer = loadLayer(resourceManager, (Resource)optional.get(), string, i2);
                if (optifineSkyLayer != null) {
                    arrayList.add(optifineSkyLayer);
                }
            }
        }

        return arrayList;
    }

    private static OptifineSkyLayer loadLayer(ResourceManager resourceManager, Resource resource, String string, int i) {
        Properties properties = new Properties();

        try (InputStream inputStream = resource.getInputStream()) {
            properties.load(inputStream);
        } catch (Exception exception) {
            return null;
        }

        OptifineSkyLayer optifineSkyLayer = new OptifineSkyLayer(properties, i + ".png");
        if (!optifineSkyLayer.isValid()) {
            return null;
        } else {
            Identifier identifier = parseIdentifier(ensurePngExtension(resolvePath(optifineSkyLayer.source, string)));
            if (identifier != null && !resourceManager.getResource(identifier).isEmpty()) {
                optifineSkyLayer.texture = identifier;
                return optifineSkyLayer;
            } else {
                return null;
            }
        }
    }

    private static String resolvePath(String string, String string2) {
        if (string.startsWith("assets/minecraft/")) {
            return string.substring("assets/minecraft/".length());
        } else if (string.startsWith("./")) {
            return string2 + "/" + string.substring(2);
        } else if (string.startsWith("/")) {
            return string.substring(1);
        } else {
            return !string.contains("/") ? string2 + "/" + string : string;
        }
    }

    private static String ensurePngExtension(String string) {
        return string.endsWith(".png") ? string : string + ".png";
    }

    private static Identifier parseIdentifier(String string) {
        try {
            return Identifier.ofVanilla(string);
        } catch (Exception exception) {
            return null;
        }
    }

    private static int getWorldIndex(World world) {
        RegistryKey registryKey = world.getRegistryKey();
        if (registryKey == World.OVERWORLD) {
            return 0;
        } else {
            return registryKey == World.END ? 1 : -1;
        }
    }

    private static float getCelestialAngle(long l, float f) {
        double d = ((double)(l % 24000L) + f) / 24000.0 - 0.25;
        double d2 = d - Math.floor(d);
        double d3 = 0.5 - Math.cos(d2 * Math.PI) / 2.0;
        return (float)((d2 * 2.0 + d3) / 3.0);
    }

    private static boolean isVisionObstructed(Camera camera) {
        CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
        return cameraSubmersionType != CameraSubmersionType.POWDER_SNOW && cameraSubmersionType != CameraSubmersionType.LAVA
            ? camera.getFocusedEntity() instanceof LivingEntity livingEntity
                && (livingEntity.hasStatusEffect(StatusEffects.BLINDNESS) || livingEntity.hasStatusEffect(StatusEffects.DARKNESS))
            : true;
    }
}
