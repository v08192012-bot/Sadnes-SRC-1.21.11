package tech.sadnes.client.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum Subcategory {
    NONE(null, ""),
    AURA(Category.COMBAT, "Attack"),
    COMBAT_WEAPONS(Category.COMBAT, "Weapons"),
    COMBAT_TWEAKS(Category.COMBAT, "Tweaks"),
    COMBAT_INVENTORY(Category.COMBAT, "Supplies"),
    SPEED(Category.MOVEMENT, "Speed"),
    MOVEMENT_AIR(Category.MOVEMENT, "Air"),
    MOVEMENT_ELYTRA(Category.MOVEMENT, "Elytra"),
    MOVEMENT_TERRAIN(Category.MOVEMENT, "Terrain"),
    MOVEMENT_ASSIST(Category.MOVEMENT, "Assist"),
    INTERACT(Category.PLAYER, "Interact"),
    PLAYER_ITEMS(Category.PLAYER, "Items"),
    PLAYER_INVENTORY(Category.PLAYER, "Inventory"),
    PLAYER_TWEAKS(Category.PLAYER, "Tweaks"),
    ENTITIES(Category.VISUALS, "Players"),
    HAND(Category.VISUALS, "Hand"),
    WORLD(Category.VISUALS, "Environment"),
    HIGHLIGHTS(Category.VISUALS, "Highlights"),
    CAMERA(Category.VISUALS, "Camera"),
    HUD_HELPERS(Category.VISUALS, "Helpers"),
    MISC_SERVER(Category.MISC, "Server"),
    MISC_SOCIAL(Category.MISC, "Social"),
    MISC_CHAT(Category.MISC, "Chat"),
    MISC_SAFETY(Category.MISC, "Safety"),
    MISC_CLIENT(Category.MISC, "Client"),
    HUD_STATUS(Category.INTERFACE, "Status"),
    HUD_INFORMATION(Category.INTERFACE, "System"),
    HUD_INVENTORY(Category.INTERFACE, "Inventory"),
    HUD_BINDS(Category.INTERFACE, "Binds"),
    HUD_MISC(Category.INTERFACE, "Misc");

    private final Category category;
    private final String displayName;
    private static final Map<Category, List<Subcategory>> BY_CATEGORY = buildIndex();

    private static Map<Category, List<Subcategory>> buildIndex() {
        EnumMap enumMap = new EnumMap<>(Category.class);

        for (Category category2 : Category.values()) {
            enumMap.put(category2, new ArrayList());
        }

        for (Subcategory subcategory : values()) {
            if (subcategory.category != null) {
                ((List)enumMap.get(subcategory.category)).add(subcategory);
            }
        }

        for (Category category3 : Category.values()) {
            enumMap.put(category3, Collections.unmodifiableList((List)enumMap.get(category3)));
        }

        return Collections.unmodifiableMap(enumMap);
    }

    public static List<Subcategory> getByCategory(Category category2) {
        return BY_CATEGORY.getOrDefault(category2, List.of());
    }

    public Category getCategory() {
        return this.category;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    Subcategory(Category category2, String string) {
        this.category = category2;
        this.displayName = string;
    }
}
