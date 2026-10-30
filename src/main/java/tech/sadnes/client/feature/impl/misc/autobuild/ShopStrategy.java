package tech.sadnes.client.feature.impl.misc.autobuild;

import net.minecraft.dialog.type.Dialog;
import net.minecraft.item.Item;
import tech.sadnes.client.feature.impl.misc.autobuy.PriceGuard;

public interface ShopStrategy {
    boolean isReady();

    void start(Item item, int i, PriceGuard priceGuard);

    boolean isBusy();

    boolean wasSuccessful();

    default boolean shouldAbortAll() {
        return false;
    }

    default void tick() {
    }

    default void onChatMessage(String string) {
    }

    default void onDialog(Dialog dialog) {
    }

    void stop();
}
