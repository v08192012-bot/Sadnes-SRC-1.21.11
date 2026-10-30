package tech.sadnes.client.ui.hub;

import java.util.Locale;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.texture.Icon;

public enum HubCategory {
    AUCTION(Icon.PIGGY_BANK, "Auctions"),
    EVENTS(Icon.CLOCK_HOUR_4, "Events"),
    SCRIPTS(Icon.CODE, "Scripts"),
    PARTY(Icon.USER_GROUP, "Party");

    private final Icon icon;
    private final String defaultName;

    public String getDisplayName() {
        return Language.translate(this.getTranslationKey(), this.defaultName);
    }

    public String getDescription() {
        return Language.translate(this.getTranslationKey() + ".desc", "");
    }

    private String getTranslationKey() {
        return "category." + this.name().toLowerCase(Locale.ROOT);
    }

    public Icon getIcon() {
        return this.icon;
    }

    public String getDefaultName() {
        return this.defaultName;
    }

    HubCategory(Icon icon2, String string) {
        this.icon = icon2;
        this.defaultName = string;
    }
}
