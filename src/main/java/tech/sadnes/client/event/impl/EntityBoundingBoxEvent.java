package tech.sadnes.client.event.impl;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import tech.sadnes.client.event.CancellableEvent;

public final class EntityBoundingBoxEvent extends CancellableEvent {
    private static final EntityBoundingBoxEvent INSTANCE = new EntityBoundingBoxEvent();
    private Box box;
    private Entity entity;

    private EntityBoundingBoxEvent() {
    }

    public static Box fire(Box box2, Entity entity2) {
        EntityBoundingBoxEvent entityBoundingBoxEvent = INSTANCE;
        Box box3 = entityBoundingBoxEvent.box;
        Entity entity3 = entityBoundingBoxEvent.entity;
        boolean flag = entityBoundingBoxEvent.isCancelled();
        entityBoundingBoxEvent.box = box2;
        entityBoundingBoxEvent.entity = entity2;
        entityBoundingBoxEvent.post();
        Box box4 = entityBoundingBoxEvent.box != null ? entityBoundingBoxEvent.box : box2;
        if (entityBoundingBoxEvent.isCancelled()) {
            box4 = shrinkToCenter(box4);
        }

        entityBoundingBoxEvent.box = box3;
        entityBoundingBoxEvent.entity = entity3;
        entityBoundingBoxEvent.setCancelled(flag);
        return box4;
    }

    private static Box shrinkToCenter(Box box2) {
        double d = (box2.minX + box2.maxX) * 0.5;
        double d2 = (box2.minY + box2.maxY) * 0.5;
        double d3 = (box2.minZ + box2.maxZ) * 0.5;
        return new Box(d - 0.001, d2 - 0.001, d3 - 0.001, d + 0.001, d2 + 0.001, d3 + 0.001);
    }

    public Box getBox() {
        return this.box;
    }

    public void setBox(Box box2) {
        this.box = box2;
    }

    public Entity getEntity() {
        return this.entity;
    }
}
