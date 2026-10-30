package tech.sadnes.client.event.impl;

import net.minecraft.world.World;
import tech.sadnes.client.event.Event;

public final class WorldTimeEvent extends Event {
    private World world;
    private long time;

    public World getWorld() {
        return this.world;
    }

    public long getTime() {
        return this.time;
    }

    public void setWorld(World world2) {
        this.world = world2;
    }

    public void setTime(long l) {
        this.time = l;
    }

    public WorldTimeEvent(World world2, long l) {
        this.world = world2;
        this.time = l;
    }
}
