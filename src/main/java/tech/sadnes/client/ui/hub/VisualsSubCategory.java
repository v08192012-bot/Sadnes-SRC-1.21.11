package tech.sadnes.client.ui.hub;

import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.texture.Icon;

public enum VisualsSubCategory implements SubCategory {
    PLAYER(Icon.USERS, "category.visuals.player", "Player"),
    WORLD(Icon.GLOBE, "category.visuals.world", "World");

    private final Icon icon;
    private final String translationKey;
    private final String defaultName;

    public static VisualsSubCategory fromSubcategory(Subcategory subcategory) {
        return subcategory != Subcategory.ENTITIES && subcategory != Subcategory.HAND ? WORLD : PLAYER;
    }

    @Override
    public String getDisplayName() {
        return Language.translate(this.translationKey, this.defaultName);
    }

    VisualsSubCategory(Icon icon2, String string, String string2) {
        this.icon = icon2;
        this.translationKey = string;
        this.defaultName = string2;
    }

    @Override
    public Icon getIcon() {
        return this.icon;
    }
}
