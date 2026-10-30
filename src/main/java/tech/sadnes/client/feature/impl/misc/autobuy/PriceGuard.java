package tech.sadnes.client.feature.impl.misc.autobuy;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.item.Item;

public final class PriceGuard {
    private final Map<Item, PriceGuard.PriceEntry> prices = new HashMap<>();
    private long maxPrice;
    private float tolerance;

    public void configure(long l, float f) {
        this.maxPrice = l;
        this.tolerance = f;
    }

    public void recordPrice(Item item, double d) {
        PriceGuard.PriceEntry priceEntry = this.prices.get(item);
        if (priceEntry == null || System.currentTimeMillis() - priceEntry.seenAt() >= 1800000L) {
            this.prices.put(item, new PriceGuard.PriceEntry(d, System.currentTimeMillis()));
        }
    }

    public boolean isAcceptable(Item item, double d) {
        if (this.maxPrice > 0L && d > this.maxPrice) {
            return false;
        }

        PriceGuard.PriceEntry priceEntry = this.prices.get(item);
        return priceEntry == null || d <= priceEntry.unit() * (1.0F + this.tolerance);
    }

    public long getMaxPrice() {
        return this.maxPrice;
    }

    record PriceEntry(double unit, long seenAt) {
    }
}
