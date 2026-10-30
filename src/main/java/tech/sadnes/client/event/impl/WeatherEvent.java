package tech.sadnes.client.event.impl;

import net.minecraft.world.World;
import tech.sadnes.client.event.Event;

public final class WeatherEvent extends Event {
    private World world;
    private WeatherEvent.Type type;
    private float gradient;

    public World getWorld() {
        return this.world;
    }

    public WeatherEvent.Type getType() {
        return this.type;
    }

    public float getGradient() {
        return this.gradient;
    }

    public void setWorld(World world2) {
        this.world = world2;
    }

    public void setType(WeatherEvent.Type type2) {
        this.type = type2;
    }

    public void setGradient(float f) {
        this.gradient = f;
    }

    public WeatherEvent(World world2, WeatherEvent.Type type2, float f) {
        this.world = world2;
        this.type = type2;
        this.gradient = f;
    }

    public enum Type {
        RAIN,
        THUNDER;
    }
}
