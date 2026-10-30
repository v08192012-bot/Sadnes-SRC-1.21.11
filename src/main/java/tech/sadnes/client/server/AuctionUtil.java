package tech.sadnes.client.server;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.pathing.goals.GoalXZ;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.impl.misc.AutoSell;
import tech.sadnes.client.feature.impl.movement.Baritone;
import tech.sadnes.client.feature.impl.render.AuctionHelper;
import tech.sadnes.client.server.auction.AuctionLoreUtil;
import tech.sadnes.client.server.autobuy.AutoBuyPriceScanner;
import tech.sadnes.client.server.autobuy.ItemPurchaser;
import tech.sadnes.client.server.autobuy.ShulkerStash;
import tech.sadnes.client.util.IMinecraft;

public final class AuctionUtil implements IMinecraft {
    public static final int AUCTION_ITEM_SLOTS = 45;
    public static final int REFRESH_SLOT = 49;
    public static final int NEXT_PAGE_SLOT = 51;
    private static final int PRICE_SAMPLE_SIZE = 3;

    public static boolean isAuctionOpen() {
        Screen screen = mc.currentScreen;
        return screen instanceof GenericContainerScreen && AuctionHelper.isAuctionScreen((GenericContainerScreen)screen);
    }

    public static boolean isHandledScreenOpen() {
        return mc.currentScreen instanceof HandledScreen;
    }

    public static boolean isContainerOpen() {
        boolean flag;
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            flag = mc.currentScreen instanceof ShulkerBoxScreen;
        } else {
            flag = true;
        }

        return flag && !isAuctionOpen();
    }

    public static boolean isFirstTagItem(ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty() && itemStack.isIn(ItemTags.SHULKER_BOXES);
    }

    public static boolean isSecondTagItem(ItemStack itemStack) {
        return itemStack != null && !itemStack.isEmpty() && itemStack.isIn(ItemTags.BUNDLES);
    }

    public static void closeScreen() {
        if (mc.player != null && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
        }
    }

    public static void searchAuction(String string) {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendChatCommand("ah search " + AuctionLoreUtil.stripSymbols(string));
        }
    }

    public static void leftClickSlot(int i) {
        clickSlot(i, SlotActionType.PICKUP, 0);
    }

    public static void quickMoveSlot(int i) {
        clickSlot(i, SlotActionType.QUICK_MOVE, 0);
    }

    public static void rightClickSlot(int i) {
        clickSlot(i, SlotActionType.PICKUP, 1);
    }

    public static int getNextPageSlot() {
        if (mc.player == null) {
            return -1;
        } else {
            ScreenHandler screenHandler = mc.player.currentScreenHandler;
            if (screenHandler == null || screenHandler.slots.size() <= 51) {
                return -1;
            } else {
                return screenHandler.getSlot(51).getStack().isEmpty() ? -1 : 51;
            }
        }
    }

    private static void clickSlot(int i, SlotActionType slotActionType, int i2) {
        if (mc.player != null) {
            ScreenHandler screenHandler = mc.player.currentScreenHandler;
            if (screenHandler != null && mc.interactionManager != null && i >= 0) {
                if (i < screenHandler.slots.size()) {
                    ItemStack itemStack = screenHandler.getSlot(i).getStack().copy();
                    ItemStack itemStack2 = screenHandler.getCursorStack().copy();
                    mc.interactionManager.clickSlot(screenHandler.syncId, i, i2, slotActionType, mc.player);
                    if (slotActionType == SlotActionType.PICKUP) {
                        screenHandler.getSlot(i).setStackNoCallbacks(itemStack);
                        screenHandler.setCursorStack(itemStack2);
                    }
                }
            }
        }
    }

    public static int averageLowestPrices(List<Integer> integers) {
        if (integers.isEmpty()) {
            return 0;
        }

        ArrayList arrayList = new ArrayList(integers);
        Collections.sort(arrayList);
        int i = Math.min(3, arrayList.size());
        long l = 0L;

        for (int i2 = 0; i2 < i; i2++) {
            l += ((Integer)arrayList.get(i2)).intValue();
        }

        return (int)(l / i);
    }

    public static boolean wanderFar() {
        if (mc.player == null) {
            return false;
        }

        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        return pathTo((int)mc.player.getX() + randomSign(threadLocalRandom) * threadLocalRandom.nextInt(14, 41), (int)mc.player.getZ() + randomSign(threadLocalRandom) * threadLocalRandom.nextInt(14, 41));
    }

    public static boolean wanderNear() {
        if (mc.player == null) {
            return false;
        }

        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        return pathTo((int)mc.player.getX() + randomSign(threadLocalRandom) * threadLocalRandom.nextInt(4, 8), (int)mc.player.getZ() + randomSign(threadLocalRandom) * threadLocalRandom.nextInt(4, 8));
    }

    public static boolean pathTo(int i, int i2) {
        Baritone baritone = Baritone.getInstance();
        if (baritone == null) {
            return false;
        }

        if (!baritone.isEnabled()) {
            baritone.setEnabled(true);
        }

        IBaritone iBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (iBaritone == null) {
            return false;
        }

        iBaritone.getCustomGoalProcess().setGoalAndPath(new GoalXZ(i, i2));
        return true;
    }

    public static boolean isPathing() {
        IBaritone iBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (iBaritone == null) {
            return false;
        }

        IPathingBehavior iPathingBehavior = iBaritone.getPathingBehavior();
        return iPathingBehavior.isPathing() || iPathingBehavior.hasPath();
    }

    public static void stopPathing() {
        IBaritone iBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        if (iBaritone != null) {
            iBaritone.getPathingBehavior().cancelEverything();
        }
    }

    private static int randomSign(ThreadLocalRandom threadLocalRandom) {
        return threadLocalRandom.nextBoolean() ? 1 : -1;
    }

    public static boolean isAutoBuyBusy() {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        if (componentManager != null) {
            AutoBuyPriceScanner autoBuyPriceScanner = componentManager.getComponent(AutoBuyPriceScanner.class);
            if (autoBuyPriceScanner != null && autoBuyPriceScanner.isRunning()) {
                return true;
            }

            ItemPurchaser itemPurchaser = componentManager.getComponent(ItemPurchaser.class);
            if (itemPurchaser != null && itemPurchaser.isBusy()) {
                return true;
            }

            ShulkerStash shulkerStash = componentManager.getComponent(ShulkerStash.class);
            if (shulkerStash != null && shulkerStash.isActive()) {
                return true;
            }
        }

        AutoSell autoSell = AutoSell.getInstance();
        return autoSell != null && autoSell.isBusy();
    }

    public static void buy(AutoBuyItem autoBuyItem) {
        buy(autoBuyItem, 0);
    }

    public static void buy(AutoBuyItem autoBuyItem, int i) {
        buy(autoBuyItem, i, 0);
    }

    public static void buy(AutoBuyItem autoBuyItem, int i, int i2) {
        buy(autoBuyItem, i, i2, null);
    }

    public static void buy(AutoBuyItem autoBuyItem, int i, int i2, IntSupplier intSupplier) {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        if (componentManager != null) {
            ItemPurchaser itemPurchaser = componentManager.getComponent(ItemPurchaser.class);
            if (itemPurchaser != null) {
                itemPurchaser.buy(autoBuyItem, i, i2, intSupplier);
            }
        }
    }

    private AuctionUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
