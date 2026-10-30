package tech.sadnes.client.feature.impl.misc.autobuy;

import net.minecraft.item.Item;
import tech.sadnes.client.feature.impl.misc.autobuild.MaterialShopper;
import tech.sadnes.client.feature.impl.misc.autobuild.ShopStrategy;
import tech.sadnes.client.server.AuctionUtil;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.autobuy.ItemPurchaser;

public final class AutoBuyPurchaseHandler implements ShopStrategy {
    private AutoBuyItem currentItem;

    @Override
    public boolean isReady() {
        return !AuctionUtil.isAutoBuyBusy();
    }

    @Override
    public void start(Item item, int i, PriceGuard priceGuard) {
        ItemPurchaser itemPurchaser = ItemPurchaser.getInstance();
        int i2 = MaterialShopper.countItem(item) + i;
        this.currentItem = new AutoBuyItem(item, 0).markCustom();
        if (itemPurchaser != null) {
            itemPurchaser.buy(this.currentItem, i, (int)Math.min(priceGuard.getMaxPrice(), 2147483647L), () -> Math.max(i2 - MaterialShopper.countItem(item), 0));
        }
    }

    @Override
    public boolean isBusy() {
        ItemPurchaser itemPurchaser = ItemPurchaser.getInstance();
        return itemPurchaser != null && itemPurchaser.isBusy();
    }

    @Override
    public boolean wasSuccessful() {
        ItemPurchaser itemPurchaser = ItemPurchaser.getInstance();
        ItemPurchaser.Result result = itemPurchaser == null ? null : itemPurchaser.getLastResult();
        return result != null && result.item() == this.currentItem && result.bought();
    }

    @Override
    public void stop() {
        if (!this.isBusy() && AuctionUtil.isHandledScreenOpen()) {
            AuctionUtil.closeScreen();
        }
    }
}
