package tech.sadnes.client.feature.impl.render.ambience;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome.Precipitation;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.CloudRenderEvent;
import tech.sadnes.client.event.impl.FogColorEvent;
import tech.sadnes.client.event.impl.FogDistanceEvent;
import tech.sadnes.client.event.impl.SkyColorEvent;
import tech.sadnes.client.event.impl.SkyRenderEvent;
import tech.sadnes.client.event.impl.WeatherEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.event.impl.WorldTimeEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.render.Removals;
import tech.sadnes.client.render.shader.AmbienceRenderer;
import tech.sadnes.client.render.world.AmbienceLightningRenderer;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.util.color.ColorUtil;

@FeatureInfo(
    name = "Ambience",
    description = "feature.impl.render.ambience",
    displayName = "feature.impl.render.ambience",
    category = Category.VISUALS,
    subcategory = Subcategory.WORLD
)
public final class Ambience extends Feature {
    private static final int LIGHTNING_FOG_COLOR = ColorUtil.rgb(204, 204, 255);
    private static final int SNOW_FOG_COLOR = ColorUtil.rgb(214, 222, 232);
    private final AmbienceProfile globalProfile = AmbienceProfile.createGlobal();
    private final Map<AmbienceProfile.AmbienceWorld, AmbienceProfile> worldProfiles = this.createWorldProfiles();
    private final List<Setting<?>> allSettings = this.collectSettings();
    private AmbienceProfile.AmbienceWorld cachedWorld;
    private long lastWorldCheck;
    static boolean lightmapOverridden;
    static boolean worldPhase;

    public static Ambience getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(Ambience.class);
    }

    private Map<AmbienceProfile.AmbienceWorld, AmbienceProfile> createWorldProfiles() {
        EnumMap enumMap = new EnumMap<>(AmbienceProfile.AmbienceWorld.class);

        for (AmbienceProfile.AmbienceWorld ambienceWorld : AmbienceProfile.AmbienceWorld.values()) {
            enumMap.put(ambienceWorld, AmbienceProfile.createForWorld(ambienceWorld));
        }

        return enumMap;
    }

    private List<Setting<?>> collectSettings() {
        ArrayList arrayList = new ArrayList<>(this.globalProfile.getAllSettings());

        for (AmbienceProfile ambienceProfile : this.worldProfiles.values()) {
            arrayList.addAll(ambienceProfile.getAllSettings());
        }

        return arrayList;
    }

    public AmbienceProfile getProfile(AmbienceProfile.AmbienceWorld ambienceWorld) {
        return this.worldProfiles.get(ambienceWorld);
    }

    public AmbienceProfile.AmbienceWorld getCurrentWorld() {
        long l = System.currentTimeMillis();
        if (l - this.lastWorldCheck > 500L) {
            this.lastWorldCheck = l;
            this.cachedWorld = this.detectWorld();
        }

        return this.cachedWorld;
    }

    private AmbienceProfile.AmbienceWorld detectWorld() {
        if (ServerManager.isWardenActive()) {
            return AmbienceProfile.AmbienceWorld.WARDEN_CITY;
        } else {
            return ServerManager.isInHub() ? AmbienceProfile.AmbienceWorld.HUB : AmbienceProfile.AmbienceWorld.fromWorld(mc.world);
        }
    }

    public AmbienceProfile getActiveProfile() {
        AmbienceProfile.AmbienceWorld ambienceWorld = this.getCurrentWorld();
        if (ambienceWorld == null) {
            return this.globalProfile;
        }

        AmbienceProfile ambienceProfile = this.worldProfiles.get(ambienceWorld);
        return ambienceProfile != null && ambienceProfile.isCustomActive() ? ambienceProfile : this.globalProfile;
    }

    public List<Setting<?>> getRootSettings(AmbienceProfile ambienceProfile) {
        return ambienceProfile.getRootSettings();
    }

    public Precipitation getPrecipitation() {
        if (!this.isEnabled()) {
            return null;
        } else {
            AmbienceProfile ambienceProfile = this.getActiveProfile();
            if (!ambienceProfile.weather.isEnabled()) {
                return null;
            } else if (ambienceProfile.weatherType.is("snow")) {
                return Precipitation.SNOW;
            } else {
                return !ambienceProfile.weatherType.is("rain") && !ambienceProfile.weatherType.is("thunder") ? null : Precipitation.RAIN;
            }
        }
    }

    public boolean isSnowing() {
        return this.getPrecipitation() == Precipitation.SNOW;
    }

    public static Precipitation getPrecipitationAt(World world, Vec3d vec3d) {
        Ambience ambience = getInstance();
        if (ambience != null) {
            Precipitation precipitation = ambience.getPrecipitation();
            if (precipitation != null) {
                return precipitation;
            }
        }

        BlockPos blockPos = BlockPos.ofFloored(vec3d);
        return world.getBiome(blockPos).value().getPrecipitation(blockPos, world.getSeaLevel());
    }

    public boolean isWeatherEnabled() {
        return this.isEnabled() && this.getActiveProfile().isWeatherEnabled();
    }

    @EventHandler
    public void onWorldTick(WorldTickEvent worldTickEvent) {
        AmbienceProfile ambienceProfile = this.getActiveProfile();
        if (ambienceProfile.isWeatherEnabled()) {
            float f = ambienceProfile.lightningFrequency.getValue() / 100.0F;
            AmbienceLightningRenderer.INSTANCE.tick(mc.world, f);
        } else {
            AmbienceLightningRenderer.INSTANCE.tick(mc.world, 0.0F);
        }
    }

    @EventHandler
    public void onWorldTime(WorldTimeEvent worldTimeEvent) {
        if (worldTimeEvent.getWorld() instanceof ClientWorld) {
            long l = Math.round(this.getActiveProfile().time.getValue() * 1000.0F);
            if (l > 23999L) {
                l = 23999L;
            }

            worldTimeEvent.setTime(l);
        }
    }

    @EventHandler
    public void onWeather(WeatherEvent weatherEvent) {
        AmbienceProfile ambienceProfile = this.getActiveProfile();
        if (weatherEvent.getWorld() instanceof ClientWorld && ambienceProfile.weather.isEnabled()) {
            boolean flag = weatherEvent.getType() == WeatherEvent.Type.RAIN;
            if (ambienceProfile.weatherType.is("clear")) {
                weatherEvent.setGradient(0.0F);
            }

            if (ambienceProfile.weatherType.is("rain") || ambienceProfile.weatherType.is("snow")) {
                weatherEvent.setGradient(flag ? 1.0F : 0.0F);
            }

            if (ambienceProfile.weatherType.is("thunder")) {
                weatherEvent.setGradient(1.0F);
            }
        }
    }

    private static int getSkyShaderMode(String string) {
        byte b = -1;
        switch (string.hashCode()) {
            case -2141602328:
                if (string.equals("starfield")) {
                    return 2;
                }
                break;
            case -1406381216:
                if (string.equals("aurora")) {
                    return 4;
                }
                break;
            case -1049449665:
                if (string.equals("nebula")) {
                    return 1;
                }
                break;
            case -985763432:
                if (string.equals("planet")) {
                    return 5;
                }
                break;
            case 104817688:
                if (string.equals("night")) {
                    return 6;
                }
                break;
            case 1316381901:
                if (string.equals("starfall")) {
                    b = 6;
                }
                break;
            case 1332899135:
                if (string.equals("blackhole")) {
                    return 3;
                }
        }

        switch (b) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 6;
            case 6:
                return 7;
            default:
                return 0;
        }
    }

    @EventHandler
    public void onSkyColor(SkyColorEvent skyColorEvent) {
        AmbienceProfile ambienceProfile = this.getActiveProfile();
        if (ambienceProfile.isSkyEnabled()) {
            skyColorEvent.setColor(ambienceProfile.skyColor.getValue());
        }
    }

    @EventHandler
    public void onSkyRender(SkyRenderEvent skyRenderEvent) {
        AmbienceProfile ambienceProfile = this.getActiveProfile();
        if (ambienceProfile.isSkyEnabled()) {
            int i = getSkyShaderMode(ambienceProfile.skyShader.getValue());
            if (i > 0) {
                skyRenderEvent.setGradientMode(i);
                skyRenderEvent.setSegments(ambienceProfile.getShaderQualityScale());
                skyRenderEvent.setAlpha(ambienceProfile.skyBrightness.getValue());
                skyRenderEvent.setColor(ambienceProfile.skyColor.getValue());
                skyRenderEvent.setBrightness(ambienceProfile.skyShaderOpacity.getValue() / 100.0F);
                skyRenderEvent.setCancelled(true);
                if (ambienceProfile.skyShaderOpacity.getValue() >= 100.0F) {
                    return;
                }
            }

            if (!ambienceProfile.skyTexture.is("off")) {
                skyRenderEvent.setTexture(Identifier.of(ClientInfo.MOD_ID, "textures/skybox/dramatic/" + ambienceProfile.skyTexture.getValue() + ".png"));
                skyRenderEvent.setAlpha(ambienceProfile.skyBrightness.getValue());
                skyRenderEvent.setRotation(0.0F);
                skyRenderEvent.setColor(ambienceProfile.skyColor.getValue());
                skyRenderEvent.setCancelled(true);
            }
        }
    }

    public boolean isCloudsEnabled() {
        return this.isEnabled() && this.getActiveProfile().isCloudsEnabled();
    }

    public boolean isVolumetricClouds() {
        return this.isEnabled() && this.getActiveProfile().isVolumetricClouds();
    }

    @EventHandler
    public void onCloudRender(CloudRenderEvent cloudRenderEvent) {
        AmbienceProfile ambienceProfile = this.getActiveProfile();
        if (ambienceProfile.atmosphere.isEnabled()) {
            if (!ambienceProfile.clouds.isEnabled()) {
                cloudRenderEvent.setCancelled(true);
            } else {
                int i = ambienceProfile.cloudColor.getValue();
                if (ambienceProfile.cloudType.is("vanilla")) {
                    cloudRenderEvent.setColor(ColorUtil.multiply(cloudRenderEvent.getColor(), i));
                } else {
                    cloudRenderEvent.setColor(i);
                }
            }
        }
    }

    @EventHandler
    public void onFogColor(FogColorEvent fogColorEvent) {
        AmbienceProfile ambienceProfile = this.getActiveProfile();
        if (ambienceProfile.atmosphere.isEnabled()) {
            Vec3d vec3d = fogColorEvent.getCamera().getCameraPos();
            if (ambienceProfile.fog.isEnabled()) {
                fogColorEvent.setColor(applyWeatherToFogColor(ambienceProfile.fogColor.getValue(), fogColorEvent.getWorld(), vec3d));
            } else {
                if (ambienceProfile.sky.isEnabled()) {
                    fogColorEvent.setColor(applyWeatherToFogColor(ambienceProfile.skyColor.getValue(), fogColorEvent.getWorld(), vec3d));
                }
            }
        }
    }

    public static int applyWeatherToFogColor(int i, World world, Vec3d vec3d) {
        if (world == null) {
            return i;
        }

        float f = world.getThunderGradient(1.0F);
        float f2 = Math.max(world.getRainGradient(1.0F) - f, 0.0F);
        float f3 = MathHelper.lerp(f2, 1.0F, 0.5F) * MathHelper.lerp(f, 1.0F, 0.25F);
        float f4 = MathHelper.lerp(f2, 1.0F, 0.6F) * MathHelper.lerp(f, 1.0F, 0.3F);
        int i2 = ColorUtil.rgbaf(ColorUtil.redf(i) * f3, ColorUtil.greenf(i) * f3, ColorUtil.bluef(i) * f4, ColorUtil.alphaf(i));
        boolean flag = getPrecipitationAt(world, vec3d) == Precipitation.SNOW;
        if (flag) {
            i2 = ColorUtil.interpolate(i2, ColorUtil.setAlpha(SNOW_FOG_COLOR, ColorUtil.alpha(i)), 0.55F * world.getRainGradient(1.0F));
        }

        boolean flag2 = MinecraftClient.getInstance().options.getHideLightningFlashes().getValue();
        float f5 = flag2 ? 0.0F : AmbienceLightningRenderer.INSTANCE.getFlashIntensity(vec3d);
        return f5 > 0.0F ? ColorUtil.interpolate(i2, LIGHTNING_FOG_COLOR, 0.35F * f5) : i2;
    }

    @EventHandler
    public void onFogDistance(FogDistanceEvent fogDistanceEvent) {
        AmbienceProfile ambienceProfile = this.getActiveProfile();
        if (ambienceProfile.atmosphere.isEnabled()) {
            if (!Removals.isRemoved("fog") || ambienceProfile.fog.isEnabled()) {
                boolean flag;
                label92: {
                    CameraSubmersionType cameraSubmersionType = fogDistanceEvent.getCamera().getSubmersionType();
                    boolean flag2;
                    if (cameraSubmersionType != CameraSubmersionType.POWDER_SNOW && (cameraSubmersionType != CameraSubmersionType.WATER || Removals.isRemoved("water"))) {
                        if (cameraSubmersionType == CameraSubmersionType.LAVA) {
                            flag = !Removals.isRemoved("lava");
                            break label92;
                        }

                        flag2 = false;
                    } else {
                        flag2 = true;
                    }

                    flag = flag2;
                }

                if (!(
                    fogDistanceEvent.getCamera().getFocusedEntity() instanceof LivingEntity livingEntity
                        && (livingEntity.hasStatusEffect(StatusEffects.BLINDNESS) || livingEntity.hasStatusEffect(StatusEffects.DARKNESS))
                        && !Removals.isRemoved("bad_effects")
                )) {
                    if (!flag) {
                        boolean flag3;
                        if (ambienceProfile.fog.isEnabled()) {
                            flag3 = !AmbienceRenderer.isOpenGl();
                        } else {
                            flag3 = false;
                        }

                        float f = flag3 ? ambienceProfile.getFogStartDistance() : Float.MAX_VALUE;
                        float f2 = flag3 ? f + (ambienceProfile.fogDistance.getValue() - f) * 100.0F / Math.max(ambienceProfile.fogDensity.getValue(), 1.0F) : Float.MAX_VALUE;
                        fogDistanceEvent.setEnvironmentalStart(f);
                        fogDistanceEvent.setRenderDistanceStart(f2);
                        fogDistanceEvent.setEnvironmentalEnd(Float.MAX_VALUE);
                        fogDistanceEvent.setRenderDistanceEnd(Float.MAX_VALUE);
                        if (flag3) {
                            fogDistanceEvent.setSkyEnd(f2);
                        } else {
                            fogDistanceEvent.setSkyEnd(0.9F * Math.min(512.0F, mc.gameRenderer.getFarPlaneDistance()));
                        }
                    }
                }
            }
        }
    }

    public int getBlockLightColor() {
        if (!this.isEnabled()) {
            return -1;
        }

        AmbienceProfile ambienceProfile = this.getActiveProfile();
        return ambienceProfile.lighting.isEnabled() && ambienceProfile.blockLight.isEnabled() ? ambienceProfile.blockLight.getValue() : -1;
    }

    public int getWorldTintColor() {
        if (!this.isEnabled()) {
            return -1;
        }

        AmbienceProfile ambienceProfile = this.getActiveProfile();
        return ambienceProfile.lighting.isEnabled() && ambienceProfile.worldTint.isEnabled() ? ambienceProfile.worldTint.getValue() : -1;
    }

    public static void setLightmapOverridden(boolean flag) {
        lightmapOverridden = flag;
    }

    public static boolean isLightmapOverridden() {
        return lightmapOverridden;
    }

    public static void setWorldPhase(boolean flag) {
        worldPhase = flag;
    }

    public static boolean isWorldPhase() {
        return worldPhase;
    }

    public AmbienceProfile getGlobalProfile() {
        return this.globalProfile;
    }
}
