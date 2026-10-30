package tech.sadnes.client.feature.impl.misc.autobuild;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import tech.sadnes.client.feature.impl.misc.autobuy.AutoBuyPurchaseHandler;
import tech.sadnes.client.feature.impl.misc.autobuy.PriceGuard;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.math.StopWatch;

public final class MaterialShopper implements IMinecraft {
    private final ShopStrategy defaultStrategy = new AutoBuyPurchaseHandler();
    private final ShopStrategy donutStrategy = new DonutShopStrategy(new ShopDialogHelper());
    private final Queue<MaterialShopper.Purchase> queue = new ArrayDeque<>();
    private final Map<Item, Long> failedAt = new HashMap<>();
    private final StopWatch lastAttemptTimer = new StopWatch();
    private final PriceGuard priceTracker = new PriceGuard();
    private ShopStrategy strategy = this.defaultStrategy;
    private Item currentItem;
    private int delayTicks;
    private volatile boolean active;
    private int boughtCount;
    private int failedCount;

    public static int countItem(Item item) {
        int i = 0;

        for (ItemStack itemStack : mc.player.getInventory().getMainStacks()) {
            if (itemStack.isOf(item)) {
                i += itemStack.getCount();
            }
        }

        return i;
    }

    public boolean isSupported() {
        return ServerManager.isDonut() || ServerManager.isFunTime() || ServerManager.isSpookyTime() || ServerManager.isHolyWorld();
    }

    public boolean canRetry() {
        return this.lastAttemptTimer.isElapsed(60000L);
    }

    public boolean start(List<BuildProgress.MaterialCount> materialCounts) {
        this.queue.clear();
        long l = System.currentTimeMillis();

        for (BuildProgress.MaterialCount materialCount : materialCounts) {
            boolean flag = l - this.failedAt.getOrDefault(materialCount.item(), 0L) < 300000L;
            if (materialCount.need() > countItem(materialCount.item()) && this.getFreeSpace(materialCount.item()) > 0 && !flag) {
                this.queue.add(new MaterialShopper.Purchase(materialCount.item(), materialCount.need()));
            }
        }

        this.lastAttemptTimer.reset();
        if (this.queue.isEmpty()) {
            return false;
        }

        this.strategy = ServerManager.isDonut() ? this.donutStrategy : this.defaultStrategy;
        this.active = true;
        this.boughtCount = this.failedCount = 0;
        this.currentItem = null;
        this.delayTicks = 10;
        return true;
    }

    public boolean tick(long l, float f) {
        if (!this.active) {
            return false;
        }

        this.priceTracker.configure(l, f);
        this.strategy.tick();
        if (this.delayTicks > 0) {
            this.delayTicks--;
            return false;
        }

        if (this.currentItem != null) {
            if (!this.strategy.isBusy()) {
                this.finishItem();
            }

            return false;
        } else {
            if (!this.strategy.isReady()) {
                return false;
            }

            MaterialShopper.Purchase purchase = this.queue.poll();
            if (purchase == null) {
                return this.finish();
            }

            Item item2 = purchase.item();
            int i = Math.min(purchase.need() - countItem(item2), this.getFreeSpace(item2));
            if (i <= 0) {
                return false;
            }

            this.currentItem = item2;
            this.strategy.start(item2, i, this.priceTracker);
            return false;
        }
    }

    public void onChatMessage(String string) {
        if (this.active) {
            this.strategy.onChatMessage(string);
        }
    }

    public void onDialog(Dialog dialog) {
        if (this.active) {
            this.strategy.onDialog(dialog);
        }
    }

    public void stop() {
        if (this.active) {
            this.strategy.stop();
        }

        this.queue.clear();
        this.currentItem = null;
        this.active = false;
    }

    private void finishItem() {
        if (this.strategy.wasSuccessful()) {
            this.boughtCount++;
        } else {
            this.failedCount++;
            this.failedAt.put(this.currentItem, System.currentTimeMillis());
        }

        this.currentItem = null;
        if (this.strategy.shouldAbortAll()) {
            this.queue.clear();
        }

        this.delayTicks = 10;
    }

    private boolean finish() {
        this.strategy.stop();
        this.active = false;
        this.lastAttemptTimer.reset();
        return true;
    }

    private int getFreeSpace(Item item) {
        int i = 0;
        int i2 = 0;

        for (ItemStack itemStack : mc.player.getInventory().getMainStacks()) {
            if (itemStack.isEmpty()) {
                i++;
            } else if (itemStack.isOf(item)) {
                i2 += itemStack.getMaxCount() - itemStack.getCount();
            }
        }

        return i2 + Math.max(i - 2, 0) * item.getMaxCount();
    }

    public boolean isActive() {
        return this.active;
    }

    public int getBoughtCount() {
        return this.boughtCount;
    }

    public int getFailedCount() {
        return this.failedCount;
    }

    record Purchase(Item item, int need) {
    }
}
