package tech.sadnes.client.server;

import java.util.Locale;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.texture.Icon;

public enum FunTimeEventType {
    METEOR_RAIN("meteor_rain", "дожд", Icon.METEOR_RAIN),
    METEOR("meteor", "метеор", Icon.METEOR),
    ALTAR_UNDEAD("altarundead", "нежит", Icon.SKULL_CROSSBONES),
    ALTAR("altar", "алтар", Icon.ALTAR),
    MYSTERY_BEACON("myst_beacon", "загадочн", Icon.TOWER),
    BEACON("beacon", "маяк", Icon.BEACON),
    AIRDROP("airdrop", "дроп", Icon.PARACHUTE_BOX),
    DEATH_CHEST("deathchest", "смерт", Icon.BOX_OPEN),
    MYSTIC("mystic", "мистич", Icon.WAND_SPARKLES),
    GEYSER("geyser", "гейзер", Icon.GEYSER),
    HELL("hellm", "резн", Icon.FLAME),
    VOLCANO("volcano", "вулкан", Icon.VOLCANO),
    PORTAL("portal", "портал", Icon.DUNGEON),
    SANTA("santa", "санта", Icon.GIFT),
    VOTING("voting", "голосов", Icon.VOTE);

    private static final FunTimeEventType[] VALUES = values();
    private final String id;
    private final String keyword;
    private final Icon icon;

    public String getDisplayName() {
        return Language.translate("eventtimer.ftevents." + this.id, this.id);
    }

    public static Icon getIconById(String string, Icon icon2) {
        for (FunTimeEventType funTimeEventType : VALUES) {
            if (funTimeEventType.id.equals(string)) {
                return funTimeEventType.icon;
            }
        }

        return icon2;
    }

    public static FunTimeEventType fromMessage(String string) {
        String string2 = string.toLowerCase(Locale.ROOT).replace('ё', 'е');

        for (FunTimeEventType funTimeEventType : VALUES) {
            if (string2.contains(funTimeEventType.keyword)) {
                return funTimeEventType;
            }
        }

        return null;
    }

    FunTimeEventType(String string, String string2, Icon icon2) {
        this.id = string;
        this.keyword = string2;
        this.icon = icon2;
    }

    public Icon getIcon() {
        return this.icon;
    }
}
