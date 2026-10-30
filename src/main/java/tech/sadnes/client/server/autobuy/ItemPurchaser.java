package tech.sadnes.client.server.autobuy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.HandledScreenRenderEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.AuctionUtil;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.auction.AuctionLoreUtil;
import tech.sadnes.client.server.auction.AuctionPriceParser;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.StopWatch;

public final class ItemPurchaser extends FeatureComponent {
    private static final long CLOSE_DELAY_MS = 500L;
    private static final long WINDOW_TIMEOUT_MS = 4000L;
    private static final long PAGE_SETTLE_MS = 2000L;
    private static final int MAX_PAGE_FLIPS = 4;
    private static final long SETTLE_DELAY_MS = 1000L;
    private static final long CONFIRM_TIMEOUT_MS = 3000L;
    private final AuctionPriceParser priceReader = new AuctionPriceParser();
    private final StopWatch stateTimer = new StopWatch();
    private final List<Integer> seenPrices = new ArrayList<>();
    private volatile ItemPurchaser.State state = ItemPurchaser.State.IDLE;
    private volatile boolean windowUpdated;
    private AutoBuyItem item;
    private volatile ItemPurchaser.BuyResponse buyResponse = ItemPurchaser.BuyResponse.NONE;
    private int selectedSlot = -1;
    private int selectedCount;
    private int selectedUnitPrice;
    private int marketPrice;
    private int retries;
    private int remaining;
    private int maxUnitPrice;
    private IntSupplier remainingSupplier;
    private int boughtCount;
    private int searchProgress;
    private int pagesLeft;
    private int refreshesLeft;
    private int closeAttempts;
    private ItemPurchaser.Result lastResult;

    public static ItemPurchaser getInstance() {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        return componentManager == null ? null : componentManager.getComponent(ItemPurchaser.class);
    }

    public boolean isBusy() {
        return this.state != ItemPurchaser.State.IDLE;
    }

    public void buy(AutoBuyItem autoBuyItem) {
        this.buy(autoBuyItem, 0);
    }

    public void buy(AutoBuyItem autoBuyItem, int i) {
        this.buy(autoBuyItem, i, 0);
    }

    public void buy(AutoBuyItem autoBuyItem, int i, int i2) {
        this.buy(autoBuyItem, i, i2, null);
    }

    public void buy(AutoBuyItem autoBuyItem, int i, int i2, IntSupplier intSupplier) {
        if (autoBuyItem != null && mc.getNetworkHandler() != null) {
            if (this.state != ItemPurchaser.State.IDLE) {
                NotificationUtil.warning(
                    NotificationSources.autoBuy(), Language.translate("autobuy.buyitem.busy", "Покупка уже идёт") + ": " + this.item.getId()
                );
            } else {
                AutoBuyController autoBuyController = AutoBuyController.getInstance();
                if (autoBuyController != null && autoBuyController.isRunning()) {
                    autoBuyController.stop(Language.translate("autobuy.buyitem.pause", "Покупка предмета: автопокупка приостановлена"));
                }

                AutoBuyPriceScanner autoBuyPriceScanner = AutoBuyPriceScanner.getInstance();
                if (autoBuyPriceScanner != null && autoBuyPriceScanner.isRunning()) {
                    autoBuyPriceScanner.stop(Language.translate("autobuy.buyitem.pause", "Покупка предмета: автопокупка приостановлена"));
                }

                this.item = autoBuyItem;
                this.remaining = i;
                this.remainingSupplier = intSupplier;
                this.maxUnitPrice = i2;
                this.boughtCount = 0;
                this.searchProgress = 0;
                this.lastResult = null;
                this.pagesLeft = 4;
                this.refreshesLeft = 1;
                this.closeAttempts = 0;
                this.retries = 0;
                this.selectedSlot = -1;
                this.buyResponse = ItemPurchaser.BuyResponse.NONE;
                this.seenPrices.clear();
                AuctionUtil.closeScreen();
                this.stateTimer.reset();
                this.state = ItemPurchaser.State.CLOSING;
                NotificationUtil.info(NotificationSources.autoBuy(), Language.translate("autobuy.buyitem.start", "Ищу на аукционе") + ": " + autoBuyItem.getId());
            }
        }
    }

    @EventHandler
    public void onClientUpdate(HandledScreenRenderEvent handledScreenRenderEvent) {
        this.update();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        this.update();
    }

    private void update() {
        switch (this.state) {
            case IDLE:
            default:
                break;
            case CLOSING:
                if (this.stateTimer.isElapsed(500L)) {
                    this.openCategory();
                    return;
                }
                break;
            case SEARCH:
                this.tickSearch();
                return;
            case PAGE:
                this.tickPage();
                return;
            case WAIT:
                if (this.stateTimer.isElapsed(2000L)) {
                    this.selectBestLot();
                    return;
                }
                break;
            case CONFIRM:
                this.tickConfirm();
                return;
            case SETTLE:
                if (this.stateTimer.isElapsed(1000L)) {
                    this.continueAfterPurchase();
                }
        }
    }

    private void openCategory() {
        if (AuctionUtil.isHandledScreenOpen()) {
            AuctionUtil.closeScreen();
            this.stateTimer.reset();
            if (++this.closeAttempts > 10) {
                this.fail(Language.translate("autobuy.buyitem.noclose", "Окно не закрывается"));
            }
        } else {
            this.windowUpdated = false;
            this.stateTimer.reset();
            this.state = ItemPurchaser.State.SEARCH;
            AuctionUtil.searchAuction(this.item.getCategoryName());
        }
    }

    private void tickSearch() {
        if (this.buyResponse == ItemPurchaser.BuyResponse.SLOW) {
            if (this.stateTimer.isElapsed(1000L)) {
                this.buyResponse = ItemPurchaser.BuyResponse.NONE;
                if (++this.retries > 3) {
                    this.fail(Language.translate("autobuy.buyitem.nowindow", "Аукцион не открылся"));
                } else {
                    this.openCategory();
                }
            }
        } else if (this.windowUpdated && AuctionUtil.isAuctionOpen()) {
            this.stateTimer.reset();
            this.state = ItemPurchaser.State.WAIT;
        } else {
            if (this.stateTimer.isElapsed(4000L)) {
                this.fail(Language.translate("autobuy.buyitem.nowindow", "Аукцион не открылся"));
            }
        }
    }

    private void tickPage() {
        if (this.windowUpdated && AuctionUtil.isAuctionOpen()) {
            this.stateTimer.reset();
            this.state = ItemPurchaser.State.WAIT;
        } else {
            if (this.stateTimer.isElapsed(4000L)) {
                this.fail(Language.translate("autobuy.buyitem.nowindow", "Аукцион не открылся"));
            }
        }
    }

    private void clickPageSlot(int i) {
        AuctionUtil.leftClickSlot(i);
        this.windowUpdated = false;
        this.stateTimer.reset();
        this.state = ItemPurchaser.State.PAGE;
    }

    private void selectBestLot() {
        if (mc.player == null) {
            this.fail(Language.translate("autobuy.buyitem.nowindow", "Аукцион не открылся"));
        } else {
            ScreenHandler screenHandler = mc.player.currentScreenHandler;
            if (screenHandler != null && AuctionUtil.isAuctionOpen() && screenHandler.slots.size() >= 45) {
                this.seenPrices.clear();
                AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
                long l = ServerManager.getCoins();
                ItemStack itemStack = this.findLargestOwnedStack();
                int i = this.remaining > 0 ? Math.max(this.remaining / 2, 4) : Integer.MAX_VALUE;
                int i2 = -1;
                int i3 = Integer.MAX_VALUE;
                int i4 = Integer.MAX_VALUE;
                int i5 = 0;

                for (int i6 = 0; i6 < 45; i6++) {
                    ItemStack itemStack2 = screenHandler.getSlot(i6).getStack();
                    if (!itemStack2.isEmpty()) {
                        if (itemStack2.isOf(this.item.getItem())) {
                            this.searchProgress = Math.max(this.searchProgress, 1);
                        }

                        if ((this.remaining > 0 ? this.item.matches(itemStack2) : this.item.matchesComponents(itemStack2))
                            && (autoBuyManager == null || !autoBuyManager.hasCustomMatch(itemStack2, this.item))
                            && (itemStack.isEmpty() || matchesIgnoringDisplay(itemStack2, itemStack))) {
                            this.searchProgress = Math.max(this.searchProgress, 2);
                            int i7 = this.priceReader.getPrice(itemStack2);
                            if (i7 > 0) {
                                this.searchProgress = Math.max(this.searchProgress, 3);
                                int i8 = i7 / Math.max(1, itemStack2.getCount());
                                this.seenPrices.add(i8);
                                if (!this.isOwnLot(AuctionLoreUtil.getFunTimeSeller(itemStack2))) {
                                    this.searchProgress = Math.max(this.searchProgress, 4);
                                    if (this.maxUnitPrice <= 0 || i8 <= this.maxUnitPrice) {
                                        this.searchProgress = Math.max(this.searchProgress, 5);
                                        if (l < 0L || i7 <= l) {
                                            label164: {
                                                int i9 = this.remaining > 0 ? Math.max(0, itemStack2.getCount() - this.remaining) : 0;
                                                boolean flag = i9 <= i;
                                                boolean flag2;
                                                if (i2 >= 0 && i4 <= i) {
                                                    flag2 = true;
                                                    if (i2 < 0) {
                                                        i2 = i6;
                                                        i3 = i8;
                                                        i4 = i9;
                                                        break label164;
                                                    }
                                                } else {
                                                    flag2 = false;
                                                    if (i2 < 0) {
                                                        i2 = i6;
                                                        i3 = i8;
                                                        i4 = i9;
                                                        break label164;
                                                    }
                                                }

                                                label105: {
                                                    boolean flag3;
                                                    if (flag != flag2) {
                                                        flag3 = flag;
                                                    } else {
                                                        if (flag) {
                                                            if (i8 >= i3) {
                                                                continue;
                                                            }

                                                            i2 = i6;
                                                            i3 = i8;
                                                            i4 = i9;
                                                            break label164;
                                                        }

                                                        if (i9 < i4) {
                                                            break label105;
                                                        }

                                                        if (i9 == i4 && i8 < i3) {
                                                            i2 = i6;
                                                            i3 = i8;
                                                            i4 = i9;
                                                            break label164;
                                                        }

                                                        flag3 = false;
                                                    }

                                                    if (!flag3) {
                                                        continue;
                                                    }
                                                }

                                                i2 = i6;
                                                i3 = i8;
                                                i4 = i9;
                                            }

                                            i5 = itemStack2.getCount();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (i2 >= 0) {
                    this.selectedSlot = i2;
                    this.selectedCount = i5;
                    this.selectedUnitPrice = i3;
                    this.marketPrice = AuctionUtil.averageLowestPrices(this.seenPrices);
                    this.buyResponse = ItemPurchaser.BuyResponse.NONE;
                    this.stateTimer.reset();
                    this.state = ItemPurchaser.State.CONFIRM;
                    AuctionUtil.quickMoveSlot(i2);
                } else if (this.pagesLeft > 0 && AuctionUtil.getNextPageSlot() >= 0) {
                    this.pagesLeft--;
                    this.clickPageSlot(51);
                } else if (this.refreshesLeft > 0) {
                    this.refreshesLeft--;
                    this.clickPageSlot(49);
                } else {
                    this.fail(this.getFailureReason());
                }
            } else {
                this.fail(Language.translate("autobuy.buyitem.nowindow", "Аукцион не открылся"));
            }
        }
    }

    private void tickConfirm() {
        switch (this.buyResponse) {
            case OK:
                this.onBought();
                return;
            case SLOW:
                if (!this.stateTimer.isElapsed(1000L)) {
                    return;
                }

                this.buyResponse = ItemPurchaser.BuyResponse.NONE;
                this.stateTimer.reset();
                this.state = ItemPurchaser.State.WAIT;
                return;
            case GONE:
                this.onLotGone();
                return;
            default:
                if (this.stateTimer.isElapsed(3000L)) {
                    if (this.remainingSupplier != null && this.remainingSupplier.getAsInt() < this.remaining) {
                        this.onBought();
                    } else {
                        this.fail(Language.translate("autobuy.buyitem.noreply", "Сервер не ответил на покупку"));
                    }
                }
        }
    }

    private void onBought() {
        this.boughtCount++;
        this.retries = 0;
        this.buyResponse = ItemPurchaser.BuyResponse.NONE;
        NotificationUtil.success(
            NotificationSources.autoBuy(),
            Language.translate("autobuy.buyitem.bought", "Куплено")
                + " "
                + this.item.getId()
                + " — "
                + this.selectedUnitPrice
                + " ("
                + Language.translate("autobuy.scan.market", "рынок")
                + " "
                + this.marketPrice
                + ")"
        );
        this.stateTimer.reset();
        this.state = ItemPurchaser.State.SETTLE;
    }

    private void continueAfterPurchase() {
        int i = this.remaining - this.selectedCount;
        this.remaining = this.remainingSupplier == null ? i : Math.min(i, this.remainingSupplier.getAsInt());
        if (this.remaining <= 0) {
            this.finish();
        } else {
            this.pagesLeft = 4;
            this.refreshesLeft = 1;
            this.clickPageSlot(49);
        }
    }

    private void onLotGone() {
        this.buyResponse = ItemPurchaser.BuyResponse.NONE;
        if (this.remainingSupplier != null && this.remainingSupplier.getAsInt() <= 0) {
            this.finish();
        } else if (++this.retries > 3) {
            this.fail(Language.translate("autobuy.buyitem.taken", "Лот не достался"));
        } else {
            this.pagesLeft = 4;
            this.refreshesLeft = 1;
            this.clickPageSlot(49);
        }
    }

    private String getFailureReason() {
        switch (this.searchProgress) {
            case 0:
                return Language.translate("autobuy.buyitem.nolots", "Лотов нет");
            case 1:
                return Language.translate("autobuy.buyitem.othervariant", "Лоты есть, но другого вида");
            case 2:
                return Language.translate("autobuy.buyitem.noprice", "Цена лота не читается");
            case 3:
                return Language.translate("autobuy.buyitem.ownlots", "На выдаче только свои лоты");
            case 4:
                return Language.translate("autobuy.buyitem.overprice", "Лоты дороже заданной цены");
            default:
                return Language.translate("autobuy.buyitem.nomoney", "Не хватает денег");
        }
    }

    private ItemStack findLargestOwnedStack() {
        if (mc.player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack itemStack = ItemStack.EMPTY;

        for (int i = 0; i < 36; i++) {
            ItemStack itemStack2 = mc.player.getInventory().getStack(i);
            if (!itemStack2.isEmpty() && itemStack2.getMaxCount() > 1 && this.item.matches(itemStack2) && (itemStack.isEmpty() || itemStack2.getCount() > itemStack.getCount())) {
                itemStack = itemStack2;
            }
        }

        return itemStack;
    }

    private static boolean matchesIgnoringDisplay(ItemStack itemStack, ItemStack itemStack2) {
        if (!itemStack.isOf(itemStack2.getItem())) {
            return false;
        }

        ItemStack itemStack3 = itemStack.copy();
        itemStack3.remove(DataComponentTypes.LORE);
        itemStack3.remove(DataComponentTypes.CUSTOM_DATA);
        ItemStack itemStack4 = itemStack2.copy();
        itemStack4.remove(DataComponentTypes.LORE);
        itemStack4.remove(DataComponentTypes.CUSTOM_DATA);
        return ItemStack.areItemsAndComponentsEqual(itemStack3, itemStack4);
    }

    private boolean isOwnLot(String string) {
        return string != null && mc.player != null && string.equalsIgnoreCase(mc.player.getGameProfile().name());
    }

    private void fail(String string) {
        if (this.boughtCount == 0) {
            NotificationUtil.warning(NotificationSources.autoBuy(), this.item.getId() + ": " + string);
        }

        this.finish();
    }

    private void finish() {
        this.lastResult = this.item == null ? null : new ItemPurchaser.Result(this.item, this.boughtCount > 0);
        this.item = null;
        this.remaining = 0;
        this.boughtCount = 0;
        this.state = ItemPurchaser.State.IDLE;
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        label35: {
            if (this.state != ItemPurchaser.State.IDLE && packetEvent.isReceive()) {
                Packet packet = packetEvent.getPacket();
                if (packet instanceof GameMessageS2CPacket gameMessageS2CPacket) {
                    this.handleChatResponse(AuctionLoreUtil.normalizeLookalikes(gameMessageS2CPacket.content().getString()));
                    return;
                }

                if (this.state != ItemPurchaser.State.SEARCH && this.state != ItemPurchaser.State.PAGE) {
                    return;
                }

                if (packet instanceof InventoryS2CPacket && ((InventoryS2CPacket)packet).syncId() != 0) {
                    break label35;
                }

                if (packet instanceof ScreenHandlerSlotUpdateS2CPacket && ((ScreenHandlerSlotUpdateS2CPacket)packet).getSyncId() > 0) {
                    this.windowUpdated = true;
                    return;
                }

                if (packet instanceof OpenScreenS2CPacket) {
                    break label35;
                }
            }

            return;
        }

        this.windowUpdated = true;
    }

    private void handleChatResponse(String string) {
        if (string.contains("успешно купили")) {
            this.buyResponse = ItemPurchaser.BuyResponse.OK;
        } else if (!string.contains("не так быстро") && !string.contains("слишком быстро")) {
            if (string.contains("уже купили") || string.contains("не актуален") || string.contains("недостаточно")) {
                this.buyResponse = ItemPurchaser.BuyResponse.GONE;
            }
        } else {
            this.buyResponse = ItemPurchaser.BuyResponse.SLOW;
        }
    }

    public AuctionPriceParser getPriceReader() {
        return this.priceReader;
    }

    public StopWatch getStateTimer() {
        return this.stateTimer;
    }

    public List<Integer> getSeenPrices() {
        return this.seenPrices;
    }

    public ItemPurchaser.State getState() {
        return this.state;
    }

    public boolean isWindowUpdated() {
        return this.windowUpdated;
    }

    public AutoBuyItem getItem() {
        return this.item;
    }

    public ItemPurchaser.BuyResponse getBuyResponse() {
        return this.buyResponse;
    }

    public int getSelectedSlot() {
        return this.selectedSlot;
    }

    public int getSelectedCount() {
        return this.selectedCount;
    }

    public int getSelectedUnitPrice() {
        return this.selectedUnitPrice;
    }

    public int getMarketPrice() {
        return this.marketPrice;
    }

    public int getRetries() {
        return this.retries;
    }

    public int getRemaining() {
        return this.remaining;
    }

    public int getMaxUnitPrice() {
        return this.maxUnitPrice;
    }

    public IntSupplier getRemainingSupplier() {
        return this.remainingSupplier;
    }

    public int getBoughtCount() {
        return this.boughtCount;
    }

    public int getSearchProgress() {
        return this.searchProgress;
    }

    public int getPagesLeft() {
        return this.pagesLeft;
    }

    public int getRefreshesLeft() {
        return this.refreshesLeft;
    }

    public int getCloseAttempts() {
        return this.closeAttempts;
    }

    public ItemPurchaser.Result getLastResult() {
        return this.lastResult;
    }

    enum BuyResponse {
        NONE,
        OK,
        SLOW,
        GONE;
    }

    public record Result(AutoBuyItem item, boolean bought) {
    }

    enum State {
        IDLE,
        CLOSING,
        SEARCH,
        PAGE,
        WAIT,
        CONFIRM,
        SETTLE;
    }
}
