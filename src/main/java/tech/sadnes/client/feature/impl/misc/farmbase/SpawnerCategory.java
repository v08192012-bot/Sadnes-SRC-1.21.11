package tech.sadnes.client.feature.impl.misc.farmbase;

import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import tech.sadnes.client.render.texture.Icon;

enum SpawnerCategory {
    HOSTILE("hostile", Icon.SKULL),
    PASSIVE("passive", Icon.HEART),
    EMPTY("empty", Icon.CUBE),
    DAMAGER("damager", Icon.BOLT);

    private final String key;
    private final Icon icon;

    static SpawnerCategory fromEntityType(@Nullable EntityType<?> entityType) {
        if (entityType == null) {
            return EMPTY;
        } else {
            return entityType.getSpawnGroup().isPeaceful() ? PASSIVE : HOSTILE;
        }
    }

    public String getKey() {
        return this.key;
    }

    public Icon getIcon() {
        return this.icon;
    }

    SpawnerCategory(String string, Icon icon2) {
        this.key = string;
        this.icon = icon2;
    }
}
