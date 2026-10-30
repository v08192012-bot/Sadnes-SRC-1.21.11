package tech.sadnes.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

public class EntityHolder {
    public Entity entity;
    private final MinecraftClient client = MinecraftClient.getInstance();

    public EntityHolder(Entity entity2) {
        this.entity = entity2;
    }

    public void update() {
    }
}
