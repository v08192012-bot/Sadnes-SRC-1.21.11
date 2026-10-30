package tech.sadnes.client.server.auction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.server.AuctionUtil;

public final class SafeBuyLimitTracker {
    private final IntSupplier overpayPercent;
    private final AuctionPriceLimits limitStore = new AuctionPriceLimits();
    private final AuctionPriceParser priceParser = new AuctionPriceParser();
    private String itemKey;
    private String displayName;
    private int version;
    private long sliderMax;
    private long marketPrice;
    private boolean previewing;
    private long previewLimit;
    private long limit;
    private boolean scanned;
    private String pendingQuery;
    private int pendingSyncId;
    private long pendingExpiry;
    private long lastUpdateTime;

    public void setPendingSearch(String string, int i) {
        this.pendingQuery = string;
        this.pendingSyncId = i;
        long l = System.currentTimeMillis();
        if (i == 0) {
            this.pendingExpiry = l + 10000L;
        } else {
            this.pendingExpiry = l + 2000L;
        }
    }

    public void touch() {
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void onContainerUpdate(int i, List<Slot> slots) {
        long l = System.currentTimeMillis();
        if (this.pendingQuery != null && l > this.pendingExpiry) {
            this.pendingQuery = null;
        }

        if (this.pendingQuery != null && i != this.pendingSyncId) {
            String string = this.pendingQuery;
            this.pendingQuery = null;
            this.setItem(string);
            this.lastUpdateTime = l;
            this.scanPrices(slots);
        } else {
            if (this.pendingQuery == null && this.itemKey != null && l - this.lastUpdateTime > 5000L) {
                this.setItem("");
            }

            this.lastUpdateTime = l;
            this.scanPrices(slots);
        }
    }

    public void clear() {
        this.pendingQuery = null;
        this.setItem("");
    }

    private void setItem(String string) {
        this.commitPreview();
        String string2 = AuctionLoreUtil.stripSymbols(string);
        this.itemKey = string2.isEmpty() ? null : string2.toLowerCase(Locale.ROOT);
        this.displayName = string2.isEmpty() ? null : Character.toUpperCase(string2.charAt(0)) + string2.substring(1);
        this.limit = this.itemKey == null ? 0L : this.limitStore.getLimit(this.itemKey);
        this.sliderMax = 0L;
        this.marketPrice = 0L;
        this.scanned = false;
        this.version++;
    }

    public boolean hasData() {
        return this.itemKey != null && this.sliderMax > 0L;
    }

    public long getEffectiveLimit() {
        if (this.itemKey == null) {
            return 0L;
        } else {
            return this.previewing ? this.previewLimit : this.limit;
        }
    }

    public boolean isOverLimit(ItemStack itemStack) {
        long l = this.getEffectiveLimit();
        if (l <= 0L) {
            return false;
        }

        int i = this.priceParser.getPrice(itemStack);
        return i <= 0 ? false : i * 100L > l * itemStack.getCount() * (100L + this.overpayPercent.getAsInt());
    }

    private void scanPrices(List<Slot> slots) {
        if (this.itemKey != null && !this.scanned) {
            ArrayList<Integer> integers = new ArrayList<>();
            int i = Math.min(45, slots.size() - 36);

            for (int i2 = 0; i2 < i; i2++) {
                ItemStack itemStack = ((Slot)slots.get(i2)).getStack();
                int i3 = this.priceParser.getPrice(itemStack);
                if (i3 > 0) {
                    integers.add(i3 / itemStack.getCount());
                }
            }

            this.marketPrice = AuctionUtil.averageLowestPrices(integers);
            long l = Math.max(this.limit, this.marketPrice);
            long l2 = integers.isEmpty() ? 0L : Collections.max(integers).intValue();
            this.sliderMax = Math.max(l * 5L / 2L, Math.min(l2, l * 4L));
            if (!integers.isEmpty()) {
                this.scanned = true;
            } else {
                this.scanned = false;
            }
        }
    }

    public float getLimitFraction(long l) {
        return l > 0L && this.sliderMax > 0L ? (float)Math.min(1.0, (double)l / this.sliderMax) : 1.0F;
    }

    public long fractionToLimit(double d) {
        if (d >= 1.0) {
            return 0L;
        }

        long l = roundToStep(Math.round(MathHelper.clamp(d, 0.0, 1.0) * this.sliderMax));
        return l >= this.sliderMax ? 0L : Math.max(1L, l);
    }

    public void setPreviewLimit(long l) {
        this.previewing = true;
        this.previewLimit = l;
    }

    public void commitPreview() {
        if (this.previewing) {
            this.previewing = false;
            this.setLimit(this.previewLimit);
        }
    }

    public void stepLimit(boolean flag) {
        long l = this.limit > 0L ? this.limit : this.sliderMax;
        long l2 = flag ? l + stepFor(l) : l - stepFor(l - 1L);
        if (l2 >= this.sliderMax) {
            this.setLimit(0L);
        } else {
            this.setLimit(Math.max(1L, roundToStep(l2)));
        }
    }

    private void setLimit(long l) {
        this.limit = l;
        this.limitStore.setLimit(this.itemKey, l);
    }

    private static long roundToStep(long l) {
        long l2 = stepFor(l);
        return Math.round((double)l / l2) * l2;
    }

    private static long stepFor(long l) {
        long l2 = 1L;

        while (l / l2 >= 100L) {
            l2 *= 10L;
        }

        return l2;
    }

    public SafeBuyLimitTracker(IntSupplier intSupplier) {
        this.overpayPercent = intSupplier;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getVersion() {
        return this.version;
    }

    public long getMarketPrice() {
        return this.marketPrice;
    }

    public boolean isPreviewing() {
        return this.previewing;
    }
}
