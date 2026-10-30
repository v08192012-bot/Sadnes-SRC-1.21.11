package tech.sadnes.client.party;

import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public enum PartyPingType {
    GO(Icon.LOCATION_CROSSHAIRS),
    DANGER(Icon.SKULL),
    LOOT(Icon.PACKAGE),
    RALLY(Icon.USERS);

    private static final PartyPingType[] VALUES = values();
    private final Icon icon;

    PartyPingType(Icon icon2) {
        this.icon = icon2;
    }

    public int getId() {
        return this.ordinal();
    }

    public Icon getIcon() {
        return this.icon;
    }

    public int getColor() {
        switch (this) {
            case GO:
                return Theme.getAccent();
            case DANGER:
                return ColorUtil.rgb(226, 75, 74);
            case LOOT:
                return ColorUtil.rgb(239, 159, 39);
            case RALLY:
                return ColorUtil.rgb(45, 200, 160);
            default:
                throw new MatchException(null, null);
        }
    }

    public static PartyPingType byId(int i) {
        return i >= 0 && i < VALUES.length ? VALUES[i] : GO;
    }
}
