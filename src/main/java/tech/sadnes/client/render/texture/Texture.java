package tech.sadnes.client.render.texture;

import net.minecraft.util.Identifier;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.ResourceUtil;

public enum Texture {
    LOGO("other/logo.png"),
    PREMIUM_LOGO("other/premlogo.png"),
    LOGO_WHITE("other/logo_white.png"),
    LOGO_WHITE_GLOW("other/logo_white_glow.png"),
    LOGO_WHITE_GLOW_MAX("other/logo_white_glow_max.png"),
    MENU_BACKGROUND("other/menu_bg.png"),
    ARROW("other/arrow.png"),
    BLOOM("particle/bloom.png"),
    DOLLAR("particle/dollar.png"),
    FLUFF_0("particle/fluff_0.png"),
    FLUFF_1("particle/fluff_1.png"),
    FLUFF_2("particle/fluff_2.png"),
    FLUFF_3("particle/fluff_3.png"),
    FLUFF_4("particle/fluff_4.png"),
    FLUFF_5("particle/fluff_5.png"),
    FLUFF_6("particle/fluff_6.png"),
    FLUFF_7("particle/fluff_7.png"),
    HEART("particle/heart.png"),
    PUMPKIN("particle/pumpkin.png"),
    SADNES("particle/sadnes.png"),
    SNOW("particle/snow.png"),
    STAR("particle/star.png"),
    STAR_FIVE("particle/star_five.png"),
    COLOR_HUE("gui/color/hue.png"),
    COLOR_ALPHA("gui/color/alpha.png"),
    COLOR_SATURATION("gui/color/saturation.png"),
    COLOR_BRIGHTNESS("gui/color/brightness.png"),
    CAPE_DEFAULT("world/capedefault.png"),
    JUMP("world/jump.png"),
    TARGET("world/target.png"),
    AMBIENCE_OVERWORLD("world/ambience/overworld.png"),
    AMBIENCE_NETHER("world/ambience/nether.png"),
    AMBIENCE_END("world/ambience/end.png"),
    AMBIENCE_SPAWN("world/ambience/spawn.png"),
    AMBIENCE_HUB("world/ambience/hub.png"),
    AVATAR_DEFAULT("avatar/default.png");

    private static final String PREFIX = "texture/";
    private final String path;
    private final Identifier identifier;

    Texture(String string) {
        this.path = normalizePath(string);
        this.identifier = Identifier.of(ClientInfo.MOD_ID, "texture/" + this.path);
    }

    public int getGlId() {
        return ResourceUtil.getGlId(this.identifier);
    }

    public void bind() {
        ResourceUtil.destroyTexture(this.identifier);
    }

    private static String normalizePath(String string) {
        String string2 = string.replace('\\', '/');
        return string2.startsWith("texture/") ? string2.substring("texture/".length()) : string2;
    }

    public String getPath() {
        return this.path;
    }

    public Identifier getIdentifier() {
        return this.identifier;
    }
}
