package tech.sadnes.client.ui.hub;

import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.texture.Icon;

public enum UtilitiesSubCategory implements SubCategory {
    AUTOBUY(Icon.CART_SHOPPING, "category.utilities.autobuy", "AutoBuy"),
    COLLECTOR(Icon.PACKAGE, "category.utilities.collector", "Collector");

    private final Icon icon;
    private final String translationKey;
    private final String defaultName;

    @Override
    public String getDisplayName() {
        return Language.translate(this.translationKey, this.defaultName);
    }

    UtilitiesSubCategory(Icon icon2, String string, String string2) {
        this.icon = icon2;
        this.translationKey = string;
        this.defaultName = string2;
    }

    @Override
    public Icon getIcon() {
        return this.icon;
    }
}
