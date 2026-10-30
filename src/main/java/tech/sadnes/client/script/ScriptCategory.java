package tech.sadnes.client.script;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.CategoryDescriptor;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.texture.Icon;

public final class ScriptCategory implements CategoryDescriptor {
    private static final Map<String, ScriptCategory> CUSTOM_CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, Subcategory> SUBCATEGORY_ALIASES = Map.of(
        "aura",
        Subcategory.AURA,
        "inventory",
        Subcategory.COMBAT_INVENTORY,
        "tools",
        Subcategory.PLAYER_ITEMS,
        "utility",
        Subcategory.MISC_SERVER,
        "players",
        Subcategory.MISC_SOCIAL
    );
    private final String name;
    private Icon icon = Icon.CODE;
    private String description = "";

    private ScriptCategory(String string) {
        this.name = string;
    }

    public static CategoryDescriptor resolve(String string) {
        String string2 = string == null ? "" : string.trim();

        for (Category category : Category.values()) {
            if (category.getName().equalsIgnoreCase(string2) || category.name().equalsIgnoreCase(string2)) {
                return category;
            }
        }

        return getOrCreate(string2);
    }

    public static ScriptCategory getOrCreate(String string) {
        String string2 = string != null && !string.isBlank() ? string.trim() : "Scripts";
        return CUSTOM_CATEGORIES.computeIfAbsent(string2.toLowerCase(Locale.ROOT), string3 -> new ScriptCategory(string2));
    }

    public static Subcategory resolveSubcategory(CategoryDescriptor categoryDescriptor, String string) {
        if (categoryDescriptor instanceof Category category && string != null && !string.isBlank()) {
            String string2 = string.trim();

            for (Subcategory subcategory : Subcategory.getByCategory(category)) {
                if (subcategory.getDisplayName().equalsIgnoreCase(string2) || subcategory.name().equalsIgnoreCase(string2)) {
                    return subcategory;
                }
            }

            Subcategory subcategory2 = SUBCATEGORY_ALIASES.get(string2.toLowerCase(Locale.ROOT));
            return subcategory2 != null && subcategory2.getCategory() == category ? subcategory2 : null;
        } else {
            return null;
        }
    }

    public void setIcon(String string) {
        if (string != null && !string.isBlank()) {
            String string2 = string.trim().toLowerCase(Locale.ROOT).replace('_', '-');

            for (Icon icon2 : Icon.values()) {
                if (icon2.getFileName().equals(string2) || icon2.name().toLowerCase(Locale.ROOT).replace('_', '-').equals(string2)) {
                    this.icon = icon2;
                    return;
                }
            }
        }
    }

    public void setDescription(String string) {
        if (string == null) {
            this.description = "";
        } else {
            this.description = string;
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Icon getIcon() {
        return this.icon;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
