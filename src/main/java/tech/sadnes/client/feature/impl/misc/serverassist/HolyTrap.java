package tech.sadnes.client.feature.impl.misc.serverassist;

import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.util.math.StopWatch;

public final class HolyTrap {
    private final String nameKey;
    private final String nameFallback;
    private final Item item;
    private final Box box;
    private final StopWatch timer = new StopWatch();
    private final long duration;

    public HolyTrap(String string, String string2, Item item2, double d, double d2, double d3, double d4, long l) {
        this.nameKey = string;
        this.nameFallback = string2;
        this.item = item2;
        double d5 = d4 / 2.0;
        this.box = new Box(d - d5, d2 - d5, d3 - d5, d + d5, d2 + d5, d3 + d5);
        this.duration = l;
    }

    public String getDisplayName() {
        return Language.translate(this.nameKey, this.nameFallback);
    }

    public String getNameKey() {
        return this.nameKey;
    }

    public String getNameFallback() {
        return this.nameFallback;
    }

    public Item getItem() {
        return this.item;
    }

    public Box getBox() {
        return this.box;
    }

    public StopWatch getTimer() {
        return this.timer;
    }

    public long getDuration() {
        return this.duration;
    }
}
