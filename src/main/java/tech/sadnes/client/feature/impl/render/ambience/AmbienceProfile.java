package tech.sadnes.client.feature.impl.render.ambience;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.world.World;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.ColorToggleSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.Unit;

public final class AmbienceProfile {
    private final AmbienceProfile.AmbienceWorld world;
    private final BooleanSetting added;
    private final BooleanSetting custom;
    public final NumberSetting time;
    public final ModeSetting shaderQuality;
    public final GroupSetting atmosphere;
    public final GroupSetting sky;
    public final ColorSetting skyColor;
    public final ModeSetting skyTexture;
    public final ModeSetting skyShader;
    public final NumberSetting skyShaderOpacity;
    public final NumberSetting skyBrightness;
    public final BooleanSetting resourcePackSky;
    public final GroupSetting clouds;
    public final ModeSetting cloudType;
    public final NumberSetting cloudCoverage;
    public final NumberSetting cloudDistance;
    public final ColorSetting cloudColor;
    public final GroupSetting fog;
    public final ModeSetting fogMode;
    public final ColorSetting fogColor;
    public final NumberSetting fogDistance;
    public final NumberSetting fogStart;
    public final NumberSetting fogDensity;
    public final NumberSetting fogHeight;
    public final NumberSetting fogThickness;
    public final NumberSetting fogNoise;
    public final NumberSetting fogSpeed;
    public final NumberSetting fogLightShafts;
    public final GroupSetting weather;
    public final ModeSetting weatherType;
    public final NumberSetting precipitationDensity;
    public final BooleanSetting rainRipples;
    public final NumberSetting snowCover;
    public final NumberSetting lightningFrequency;
    public final GroupSetting wind;
    public final NumberSetting windStrength;
    public final NumberSetting windDirection;
    public final NumberSetting windStreaks;
    public final BooleanSetting windFoliage;
    public final GroupSetting lighting;
    public final ColorToggleSetting blockLight;
    public final ColorToggleSetting worldTint;
    public final GroupSetting colorGrade;
    public final NumberSetting saturation;
    public final NumberSetting contrast;
    public final NumberSetting brightness;
    public final NumberSetting temperature;
    public final GroupSetting distanceBlur;
    public final NumberSetting blurDistance;
    public final NumberSetting blurStrength;
    public final GroupSetting bloom;
    public final NumberSetting bloomIntensity;
    public final NumberSetting bloomThreshold;
    public final GroupSetting reflections;
    private final List<Setting<?>> rootSettings;
    private final List<Setting<?>> allSettings;

    public static AmbienceProfile createGlobal() {
        return new AmbienceProfile(null);
    }

    public static AmbienceProfile createForWorld(AmbienceProfile.AmbienceWorld ambienceWorld) {
        return new AmbienceProfile(ambienceWorld);
    }

    private AmbienceProfile(AmbienceProfile.AmbienceWorld ambienceWorld) {
        this.world = ambienceWorld;
        boolean flag = ambienceWorld != null;
        String string = flag ? "ambience." + ambienceWorld.getId() + "." : "ambience.";
        this.time = new NumberSetting("ambience.time", 12.0F, 0.0F, 24.0F, 0.1F, Unit.HOUR);
        this.shaderQuality = new ModeSetting("ambience.shaderQuality", "x2", new String[]{"x1", "x2", "x3", "x4"});
        this.sky = new GroupSetting("ambience.sky", false).setIcon(Icon.SUN);
        this.skyColor = new ColorSetting(this.sky, "ambience.skyColor", -7354113);
        this.skyTexture = new ModeSetting(this.sky, "ambience.skyTexture", "off", new String[]{"off", "day", "nebula", "storm", "galaxy", "starry"});
        this.skyShader = new ModeSetting(
            this.sky, "ambience.skyShader", "off", new String[]{"off", "nebula", "starfield", "blackhole", "aurora", "planet", "night", "starfall"}
        );
        this.skyShaderOpacity = new NumberSetting(this.sky, "ambience.skyShaderOpacity", 100.0F, 10.0F, 100.0F, 5.0F, Unit.PERCENT)
            .setVisibleWhen(() -> !this.skyShader.is("off") ? true : false);
        this.skyBrightness = new NumberSetting(this.sky, "ambience.skyBrightness", 1.0F, 0.0F, 2.0F, 0.05F);
        this.resourcePackSky = new BooleanSetting(this.sky, "ambience.resourcePackSky", false);
        this.clouds = new GroupSetting("ambience.clouds", true).setIcon(Icon.CLOUD);
        this.cloudType = new ModeSetting(this.clouds, "ambience.cloudType", "volumetric", new String[]{"volumetric", "vanilla"});
        this.cloudCoverage = new NumberSetting(this.clouds, "ambience.cloudCoverage", 55.0F, 5.0F, 95.0F, 5.0F, Unit.PERCENT)
            .setVisibleWhen(() -> this.cloudType.is("volumetric"));
        this.cloudDistance = new NumberSetting(this.clouds, "ambience.cloudDistance", 2048.0F, 512.0F, 4096.0F, 128.0F, Unit.METER)
            .setVisibleWhen(() -> this.cloudType.is("volumetric"));
        this.cloudColor = new ColorSetting(this.clouds, "ambience.cloudColor", -1);
        this.fog = new GroupSetting("ambience.fog", true).setIcon(Icon.SMOG);
        this.fogMode = new ModeSetting(this.fog, "ambience.fogMode", "distance", new String[]{"distance", "ground", "volumetric"});
        this.fogColor = new ColorSetting(this.fog, "ambience.fogColor", null, false, -4138753);
        this.fogDistance = new NumberSetting(this.fog, "ambience.fogDistance", 128.0F, 8.0F, 512.0F, 1.0F, Unit.METER);
        this.fogStart = new NumberSetting(this.fog, "ambience.fogStart", 50.0F, 0.0F, 90.0F, 5.0F, Unit.PERCENT);
        this.fogDensity = new NumberSetting(this.fog, "ambience.fogDensity", 100.0F, 10.0F, 200.0F, 5.0F, Unit.PERCENT);
        this.fogHeight = new NumberSetting(this.fog, "ambience.fogHeight", 64.0F, -64.0F, 320.0F, 1.0F, Unit.METER)
            .setVisibleWhen(() -> !this.fogMode.is("ground") && !this.fogMode.is("volumetric") ? false : true);
        this.fogThickness = new NumberSetting(this.fog, "ambience.fogThickness", 16.0F, 4.0F, 256.0F, 1.0F, Unit.METER)
            .setVisibleWhen(() -> !this.fogMode.is("ground") && !this.fogMode.is("volumetric") ? false : true);
        this.fogNoise = new NumberSetting(this.fog, "ambience.fogNoise", 50.0F, 0.0F, 100.0F, 5.0F, Unit.PERCENT)
            .setVisibleWhen(() -> this.fogMode.is("volumetric"));
        this.fogSpeed = new NumberSetting(this.fog, "ambience.fogSpeed", 100.0F, 0.0F, 200.0F, 5.0F, Unit.PERCENT)
            .setVisibleWhen(() -> this.fogMode.is("volumetric") && this.fogNoise.getValue() > 0.0F ? true : false);
        this.fogLightShafts = new NumberSetting(this.fog, "ambience.fogLightShafts", 70.0F, 0.0F, 100.0F, 5.0F, Unit.PERCENT);
        this.weather = new GroupSetting("ambience.weather", true);
        this.weatherType = new ModeSetting(this.weather, "ambience.weatherType", "clear", new String[]{"clear", "rain", "thunder", "snow"});
        this.precipitationDensity = new NumberSetting(this.weather, "ambience.precipitationDensity", 70.0F, 10.0F, 100.0F, 5.0F, Unit.PERCENT)
            .setVisibleWhen(() -> this.isWeatherType("rain", "thunder", "snow"));
        this.rainRipples = new BooleanSetting(this.weather, "ambience.rainRipples", true).setVisibleWhen(() -> this.isWeatherType("rain", "thunder"));
        this.snowCover = new NumberSetting(this.weather, "ambience.snowCover", 60.0F, 0.0F, 100.0F, 5.0F, Unit.PERCENT)
            .setVisibleWhen(() -> this.isWeatherType("snow"));
        this.lightningFrequency = new NumberSetting(this.weather, "ambience.lightningFrequency", 50.0F, 0.0F, 100.0F, 5.0F, Unit.PERCENT)
            .setVisibleWhen(() -> this.isWeatherType("thunder"));
        this.wind = new GroupSetting("ambience.wind", false).setIcon(Icon.WIND);
        this.windStrength = new NumberSetting(this.wind, "ambience.windStrength", 50.0F, 0.0F, 100.0F, 5.0F, Unit.PERCENT);
        this.windDirection = new NumberSetting(this.wind, "ambience.windDirection", 45.0F, 0.0F, 345.0F, 15.0F, Unit.DEGREE);
        this.windStreaks = new NumberSetting(this.wind, "ambience.windStreaks", 60.0F, 0.0F, 100.0F, 5.0F, Unit.PERCENT);
        this.windFoliage = new BooleanSetting(this.wind, "ambience.windFoliage", true);
        this.weather.onInput(this.wind);
        this.atmosphere = new GroupSetting("ambience.atmosphere", true).onInput(this.sky, this.clouds, this.fog);
        this.lighting = new GroupSetting("ambience.lighting", true);
        this.blockLight = new ColorToggleSetting(this.lighting, "ambience.blockLight", false, -25277);
        this.worldTint = new ColorToggleSetting(this.lighting, "ambience.worldTint", false, -7359233);
        this.colorGrade = new GroupSetting("ambience.colorGrade", false);
        this.saturation = new NumberSetting(this.colorGrade, "ambience.saturation", 100.0F, 0.0F, 200.0F, 5.0F, Unit.PERCENT);
        this.contrast = new NumberSetting(this.colorGrade, "ambience.contrast", 100.0F, 50.0F, 150.0F, 5.0F, Unit.PERCENT);
        this.brightness = new NumberSetting(this.colorGrade, "ambience.brightness", 100.0F, 50.0F, 150.0F, 5.0F, Unit.PERCENT);
        this.temperature = new NumberSetting(this.colorGrade, "ambience.temperature", 0.0F, -100.0F, 100.0F, 5.0F, Unit.PERCENT);
        this.distanceBlur = new GroupSetting("ambience.distanceBlur", false);
        this.blurDistance = new NumberSetting(this.distanceBlur, "ambience.blurDistance", 64.0F, 8.0F, 256.0F, 1.0F, Unit.METER);
        this.blurStrength = new NumberSetting(this.distanceBlur, "ambience.blurStrength", 3.0F, 1.0F, 4.0F, 1.0F);
        this.bloom = new GroupSetting("ambience.bloom", false);
        this.bloomIntensity = new NumberSetting(this.bloom, "ambience.bloomIntensity", 1.0F, 0.1F, 3.0F, 0.05F);
        this.bloomThreshold = new NumberSetting(this.bloom, "ambience.bloomThreshold", 0.8F, 0.3F, 1.0F, 0.01F);
        this.reflections = new GroupSetting("ambience.reflections", false);
        this.rootSettings = List.of(
            this.time, this.weather, this.shaderQuality, this.atmosphere, this.lighting, this.colorGrade, this.distanceBlur, this.bloom, this.reflections
        );
        Iterator iterator2 = this.rootSettings.iterator();

        while (iterator2.hasNext()) {
            prefixSettingKey((Setting<?>)iterator2.next(), string, flag);
        }

        this.added = flag ? createHiddenFlag("ambience.world.added", string + "added", false) : null;
        this.custom = flag ? createHiddenFlag("ambience.world.custom", string + "custom", true) : null;
        ArrayList arrayList = new ArrayList<>(this.rootSettings);
        if (flag) {
            arrayList.add(this.added);
            arrayList.add(this.custom);
        }

        this.allSettings = Collections.unmodifiableList(arrayList);
    }

    private static void prefixSettingKey(Setting<?> setting, String string, boolean flag) {
        setting.setDisplayName(string + stripPrefix(setting.getName()));
        setting.setHidden(flag);
        if (setting instanceof GroupSetting groupSetting) {
            Iterator iterator2 = groupSetting.getChildList().iterator();

            while (iterator2.hasNext()) {
                prefixSettingKey((Setting<?>)iterator2.next(), string, flag);
            }
        }
    }

    private static BooleanSetting createHiddenFlag(String string, String string2, boolean flag) {
        BooleanSetting booleanSetting = new BooleanSetting(string, flag);
        booleanSetting.setDisplayName(string2).setHidden(true);
        return booleanSetting;
    }

    private static String stripPrefix(String string) {
        return string.startsWith("ambience.") ? string.substring("ambience.".length()) : string;
    }

    public List<Setting<?>> getRootSettings() {
        return this.rootSettings;
    }

    public List<Setting<?>> getAllSettings() {
        return this.allSettings;
    }

    public boolean isAdded() {
        return this.added != null && this.added.getValue();
    }

    public void setAdded(boolean flag) {
        if (this.added != null) {
            this.added.setValue(flag);
        }
    }

    public boolean isCustom() {
        return this.custom != null && this.custom.getValue();
    }

    public void setCustom(boolean flag) {
        if (this.custom != null) {
            this.custom.setValue(flag);
        }
    }

    public boolean isCustomActive() {
        return this.isAdded() && this.isCustom();
    }

    private boolean isWeatherType(String... strings) {
        for (String string : strings) {
            if (this.weatherType.is(string)) {
                return true;
            }
        }

        return false;
    }

    public boolean isSkyEnabled() {
        return this.atmosphere.isEnabled() && this.sky.isEnabled();
    }

    public boolean isCloudsEnabled() {
        return this.atmosphere.isEnabled() && this.clouds.isEnabled();
    }

    public boolean isVolumetricClouds() {
        return this.isCloudsEnabled() && this.cloudType.is("volumetric");
    }

    public boolean isWeatherEnabled() {
        return this.weather.isEnabled();
    }

    public boolean isWindEnabled() {
        return this.weather.isEnabled() && this.wind.isEnabled();
    }

    public int getFogModeIndex() {
        String string = this.fogMode.getValue();
        byte b = -1;
        switch (string.hashCode()) {
            case -1551614414:
                if (string.equals("volumetric")) {
                    b = 1;
                }
                break;
            case -1237460601:
                if (string.equals("ground")) {
                    return 1;
                }
        }

        switch (b) {
            case 0:
                return 1;
            case 1:
                return 2;
            default:
                return 0;
        }
    }

    public int getShaderQualityScale() {
        String string = this.shaderQuality.getValue();
        byte b = -1;
        switch (string.hashCode()) {
            case 3769:
                if (string.equals("x1")) {
                    return 1;
                }
            case 3770:
            default:
                break;
            case 3771:
                if (string.equals("x3")) {
                    return 3;
                }
                break;
            case 3772:
                if (string.equals("x4")) {
                    b = 2;
                }
        }

        switch (b) {
            case 0:
                return 1;
            case 1:
                return 3;
            case 2:
                return 4;
            default:
                return 2;
        }
    }

    public boolean isFogEnabled() {
        return this.atmosphere.isEnabled() && this.fog.isEnabled();
    }

    public float getFogStartDistance() {
        return this.fogDistance.getValue() * this.fogStart.getValue() / 100.0F;
    }

    public float getFogDensityFactor() {
        return 3.0F * this.fogDensity.getValue() / 100.0F / Math.max(this.fogDistance.getValue() - this.getFogStartDistance(), 1.0F);
    }

    public AmbienceProfile.AmbienceWorld getWorld() {
        return this.world;
    }

    public enum AmbienceWorld {
        OVERWORLD("ambience.world.overworld", Icon.MOUNTAIN_SUN, "overworld", Texture.AMBIENCE_OVERWORLD),
        NETHER("ambience.world.nether", Icon.FLAME, "nether", Texture.AMBIENCE_NETHER),
        END("ambience.world.end", Icon.DRAGON, "end", Texture.AMBIENCE_END),
        SPAWN("ambience.world.spawn", Icon.FLAG, "spawn", Texture.AMBIENCE_SPAWN),
        HUB("ambience.world.hub", Icon.CIRCLE_NODES, "hub", Texture.AMBIENCE_HUB),
        WARDEN_CITY("ambience.world.warden", Icon.SKULL, "warden", null);

        private final String translationKey;
        private final Icon icon;
        private final String id;
        private final Texture preview;

        AmbienceWorld(String string, Icon icon2, String string2, Texture texture) {
            this.translationKey = string;
            this.icon = icon2;
            this.id = string2;
            this.preview = texture;
        }

        public static AmbienceProfile.AmbienceWorld fromWorld(World world2) {
            if (world2 == null) {
                return null;
            } else if (world2.getRegistryKey() == World.OVERWORLD) {
                return OVERWORLD;
            } else if (world2.getRegistryKey() == World.NETHER) {
                return NETHER;
            } else if (world2.getRegistryKey() == World.END) {
                return END;
            } else {
                return world2.getRegistryKey().getValue().getPath().equals("spawn") ? SPAWN : null;
            }
        }

        public String getTranslationKey() {
            return this.translationKey;
        }

        public Icon getIcon() {
            return this.icon;
        }

        public String getId() {
            return this.id;
        }

        public Texture getPreview() {
            return this.preview;
        }
    }
}
