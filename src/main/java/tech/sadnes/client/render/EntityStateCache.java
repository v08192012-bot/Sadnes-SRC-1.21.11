package tech.sadnes.client.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;

public final class EntityStateCache {
    private static final Map<Integer, Entity> ENTITIES = new ConcurrentHashMap<>();
    private static Entity currentEntity;

    private EntityStateCache() {
    }

    public static EntityRenderState createRenderState(EntityRenderer entityRenderer, Entity entity) {
        track(entity);
        currentEntity = entity;

        try {
            return entityRenderer.getAndUpdateRenderState(entity, 1.0F);
        } finally {
            currentEntity = null;
        }
    }

    public static boolean isCurrent(Entity entity) {
        return entity == currentEntity;
    }

    public static void track(Entity entity) {
        if (entity != null) {
            ENTITIES.put(entity.getId(), entity);
        }
    }

    public static Entity getEntity(int i) {
        return ENTITIES.get(i);
    }
}
