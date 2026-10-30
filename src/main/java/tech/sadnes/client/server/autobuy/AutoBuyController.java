package tech.sadnes.client.server.autobuy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.HandledScreenRenderEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.feature.impl.misc.AutoSell;
import tech.sadnes.client.feature.impl.misc.Test;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.AuctionUtil;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.server.FunTimeAnarchies;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.auction.AuctionLoreUtil;
import tech.sadnes.client.server.auction.AuctionMarketLog;
import tech.sadnes.client.server.auction.AuctionPriceParser;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.rotation.RotationAnimator;

public final class AutoBuyController extends FeatureComponent {
    private static final long UPDATE_TIMEOUT_MS = 600L;
    private static final long CLOSED_GRACE_MS = 1500L;
    private static final int MAX_SCAN_PAGES = 10;
    private static final int MAX_TRACKED_LOTS = 100;
    private static final long HOP_TIMEOUT_MS = 30000L;
    private static final long BUY_TIMEOUT_MS = 3000L;
    private static final long VERIFY_TIMEOUT_MS = 12000L;
    private static final long PARSE_INTERVAL_MS = 300000L;
    private static final long FUNTIME_PARSE_INTERVAL_MS = 1500000L;
    private static final long PARTNER_PRICES_TIMEOUT_MS = 600000L;
    private static final long PARTNER_PRICES_POLL_MS = 5000L;
    private static final long KEEP_ALIVE_INTERVAL_MS = 6000L;
    private static final int RECOVER_SLOT = 53;
    private static final int MAX_SLOT_SEARCH_ATTEMPTS = 15;
    private static final long SELL_INTERVAL_MS = 120000L;
    private static final long SELL_STEP_DELAY_MS = 300L;
    private static final long SELL_SETTLE_DELAY_MS = 200L;
    private static final long LOTS_CHECK_INTERVAL_MS = 60000L;
    private static final long SELL_TIMEOUT_MS = 20000L;
    private static final int LOTS_SLOT = 46;
    private static final long OPEN_RETRY_DELAY_MS = 3000L;
    private static final int MAX_OPEN_ATTEMPTS = 4;
    private static final long HUB_REJOIN_DELAY_MS = 3000L;
    private static final long HOP_OPEN_DELAY_MS = 12000L;
    private final StopWatch updateTimer = new StopWatch();
    private final StopWatch cooldownTimer = new StopWatch();
    private final StopWatch auctionClosedTimer = new StopWatch();
    private final StopWatch hopTimer = new StopWatch();
    private final StopWatch openTimer = new StopWatch();
    private final StopWatch hubTimer = new StopWatch();
    private final StopWatch buyTimer = new StopWatch();
    private final StopWatch verifyTimer = new StopWatch();
    private final StopWatch lookAroundTimer = new StopWatch();
    private final StopWatch keepAliveTimer = new StopWatch();
    private final StopWatch parseTimer = new StopWatch();
    private final StopWatch recoverTimer = new StopWatch();
    private final StopWatch sellTimer = new StopWatch();
    private final StopWatch sellStepTimer = new StopWatch();
    private final StopWatch lotsCheckTimer = new StopWatch();
    private final StopWatch lotsStepTimer = new StopWatch();
    private final StopWatch partnerPollTimer = new StopWatch();
    private long lookAroundDelay = 2000L;
    private final AuctionPriceParser priceReader = new AuctionPriceParser();
    private final Set<String> rejectedLots = new HashSet<>();
    private final Set<String> unaffordableLots = new HashSet<>();
    private CompletableFuture<Boolean> verification;
    private AutoBuyItem targetItem;
    private String targetSeller;
    private int targetSlot;
    private int targetPrice;
    private volatile boolean buyResponded;
    private volatile boolean buySucceeded;
    private int recoverStage;
    private int recoverAttempts;
    private long recoverDelay;
    private volatile boolean recoverPending;
    private final List<AutoBuyController.ResaleEntry> resaleQueue = new ArrayList<>();
    private AutoBuyController.ResaleEntry currentSale;
    private int sellStage;
    private volatile boolean sellBlocked;
    private int lotsStage;
    private int slotSearchAttempts;
    private long lotsDelay;
    private boolean awaitingPartnerPrices;
    private int scanCount;
    private int pageFlips;
    private int maxPageFlips = 1;
    private int anarchyNumber = -1;
    private int openAttempts;
    private long firstOpenDelay = 12000L;
    private volatile boolean hopConfirmed;
    private volatile boolean running;
    private volatile AutoBuyController.State state = AutoBuyController.State.COOLDOWN;
    private volatile long cooldown;
    private volatile long lastResponseTime;

    public static AutoBuyController getInstance() {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        return componentManager == null ? null : componentManager.getComponent(AutoBuyController.class);
    }

    public void toggle() {
        if (this.running) {
            this.stop(Language.translate("autobuy.stop.manual", "Выключена"));
        } else {
            this.start();
        }
    }

    public void start() {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (!this.isScanMode() && autoBuyManager != null && autoBuyManager.getMode() == AutoBuyManager.Mode.OFF) {
            NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("autobuy.mode.off", "Выключена в этой сессии"));
        } else if (!this.isSpookyTime() && !this.isScanMode() && autoBuyManager != null && autoBuyManager.getMode() == AutoBuyManager.Mode.CHECK) {
            NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("autobuy.mode.check", "Эта сессия проверяющая — покупает вторая"));
        } else if (!ServerManager.isFunTime() && !ServerManager.isSpookyTime()) {
            NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("autobuy.stop.server", "Работает только на FunTime"));
        } else if (!this.isAuctionOpen()) {
            NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("autobuy.stop.noauction", "Откройте аукцион"));
        } else if (!this.isSpookyTime() && !this.isScanMode() && !AutoBuyLink.ping()) {
            NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("autobuy.link.down", "Проверяющая сессия не подключена"));
        } else {
            this.lastResponseTime = 0L;
            this.parseTimer.reset();
            this.sellTimer.reset();
            this.lotsCheckTimer.reset();
            this.resetCooldown();
            this.running = true;
            NotificationUtil.info(NotificationSources.autoBuy(), Language.translate("autobuy.start", "Запущена"));
        }
    }

    public void stop(String string) {
        if (this.running) {
            this.running = false;
            mc.options.forwardKey.setPressed(false);
            this.state = AutoBuyController.State.COOLDOWN;
            this.anarchyNumber = -1;
            AuctionUtil.stopPathing();
            NotificationUtil.info(NotificationSources.autoBuy(), string);
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
        if (this.running) {
            if (this.state == AutoBuyController.State.HOPPING && this.hopConfirmed) {
                this.hopConfirmed = false;
                FunTimeAnarchies.markFull(this.anarchyNumber);
                this.hopToFreeAnarchy();
            } else if (ServerManager.isInHub()) {
                if (this.isSpookyTime()) {
                    this.stop(Language.translate("autobuy.stop.closed", "Аукцион закрыт — автопокупка остановлена"));
                } else {
                    if (this.hubTimer.isElapsed(3000L)) {
                        this.leaveHub();
                    }
                }
            } else {
                this.hubTimer.reset();
                if (this.state == AutoBuyController.State.HOPPING) {
                    this.tickHopping();
                } else if (this.state == AutoBuyController.State.OPENING) {
                    this.tickOpening();
                } else if (this.state == AutoBuyController.State.SELLING) {
                    this.tickSelling();
                } else if (this.state == AutoBuyController.State.LOTS) {
                    this.tickLots();
                } else if (this.state == AutoBuyController.State.RECOVER) {
                    this.tickRecover();
                } else if (this.state == AutoBuyController.State.BUYING) {
                    this.tickBuying();
                } else if (this.state == AutoBuyController.State.VERIFYING) {
                    this.tickVerifying();
                } else if (!this.isAuctionOpen()) {
                    this.onAuctionClosed();
                } else {
                    this.auctionClosedTimer.reset();
                    this.lookAround();
                    if (!this.tryStartParsing()) {
                        if (!this.tryStartSelling() && !this.tryStartLotsCheck()) {
                            if (this.state == AutoBuyController.State.WAIT_UPDATE) {
                                if (this.updateTimer.isElapsed(600L)) {
                                    if (this.isSpookyTime()) {
                                        this.resetCooldown();
                                    } else {
                                        this.hopOnTimeout();
                                    }
                                }
                            } else if (this.cooldownTimer.isElapsed(this.cooldown)) {
                                if (this.isScanMode()) {
                                    this.scanAuction();
                                } else if (!this.tryBuyCheapLot()) {
                                    this.refreshAuction();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isSpookyTime() {
        return ServerManager.isSpookyTime();
    }

    private boolean isScanMode() {
        Test test = Test.getInstance();
        return test != null && test.isEnabled() && test.getLogAuctionAssortment().getValue();
    }

    private boolean tryBuyCheapLot() {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (mc.player == null) {
            return false;
        }

        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        if (autoBuyManager != null && screenHandler != null && screenHandler.slots.size() >= 45) {
            long l = ServerManager.getCoins();

            for (int i = 0; i < 45; i++) {
                ItemStack itemStack = screenHandler.getSlot(i).getStack();
                if (!itemStack.isEmpty()) {
                    int i2 = this.priceReader.getPrice(itemStack);
                    if (i2 > 0) {
                        int i3 = i2 / Math.max(1, itemStack.getCount());
                        AutoBuyItem autoBuyItem = this.findItem(autoBuyManager, itemStack, i3);
                        if (autoBuyItem != null) {
                            String string = AuctionLoreUtil.getFunTimeSeller(itemStack);
                            if (!this.isOwnLot(string)) {
                                if (l >= 0L && i2 > l) {
                                    if (this.unaffordableLots.add(this.lotKey(autoBuyItem, i2, string))) {
                                        if (this.unaffordableLots.size() > 500) {
                                            this.unaffordableLots.clear();
                                        }

                                        PurchaseHistory.add(autoBuyItem, i2, string, PurchaseHistory.Result.NO_MONEY);
                                    }
                                } else {
                                    if (this.isSpookyTime()) {
                                        this.targetItem = autoBuyItem;
                                        this.targetPrice = i2;
                                        this.targetSeller = string;
                                        this.buyLot(screenHandler, i, autoBuyItem, i2);
                                        return true;
                                    }

                                    if (string != null && !this.rejectedLots.contains(this.lotKey(autoBuyItem, i2, string))) {
                                        this.requestVerification(autoBuyItem, i, i2, string);
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private void lookAround() {
        if (this.isSpookyTime() && this.lookAroundTimer.isElapsed(this.lookAroundDelay)) {
            ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
            this.lookAroundTimer.reset();
            this.lookAroundDelay = threadLocalRandom.nextInt(1800, 5200);
            RotationAnimator.animate(randomSign(threadLocalRandom) * threadLocalRandom.nextFloat(2.0F, 11.0F), threadLocalRandom.nextFloat(-4.0F, 4.0F), threadLocalRandom.nextInt(8, 16));
        }
    }

    private boolean tryStartParsing() {
        long l = ServerManager.isFunTime() ? 1500000L : 300000L;
        if (!this.isScanMode() && this.parseTimer.isElapsed(l) && !AuctionUtil.isAutoBuyBusy()) {
            AutoBuyPriceScanner autoBuyPriceScanner = AutoBuyPriceScanner.getInstance();
            AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
            if (autoBuyPriceScanner != null && autoBuyManager != null) {
                if (autoBuyPriceScanner.isRunning()) {
                    return false;
                }

                ArrayList<AutoBuyItem> autoBuyItems = new ArrayList<>();

                for (AutoBuyItem autoBuyItem : autoBuyManager.getItemsForCurrentServer()) {
                    if (autoBuyItem.isEnabled() && autoBuyItem.getAutoParse().getValue()) {
                        autoBuyItems.add(autoBuyItem);
                    }
                }

                if (autoBuyItems.isEmpty()) {
                    this.parseTimer.reset();
                    return false;
                }

                this.parseTimer.reset();
                ArrayList<AutoBuyItem> autoBuyItems2 = autoBuyItems;
                if (!this.isSpookyTime() && autoBuyItems.size() > 1 && AutoBuyLink.ping()) {
                    int i = autoBuyItems.size() / 2;
                    autoBuyItems2 = new ArrayList<>(autoBuyItems.subList(0, i));
                    ArrayList arrayList = new ArrayList();

                    for (AutoBuyItem autoBuyItem2 : autoBuyItems.subList(i, autoBuyItems.size())) {
                        arrayList.add(autoBuyItem2.getName());
                    }

                    AutoBuyLink.sendScan(arrayList);
                    this.awaitingPartnerPrices = true;
                    this.partnerPollTimer.reset();
                    this.verifyTimer.reset();
                }

                autoBuyPriceScanner.start(autoBuyItems2);
                return true;
            }
        }

        return false;
    }

    @EventHandler
    public void onPartnerPricesTick(TickEvent tickEvent) {
        if (this.awaitingPartnerPrices) {
            AutoBuyPriceScanner autoBuyPriceScanner = AutoBuyPriceScanner.getInstance();
            if (autoBuyPriceScanner == null || !autoBuyPriceScanner.isRunning()) {
                if (this.partnerPollTimer.isElapsed(5000L)) {
                    this.partnerPollTimer.reset();
                    int i = AutoBuyPriceScanner.applyPrices(AutoBuyLink.requestPrices());
                    if (i > 0) {
                        this.awaitingPartnerPrices = false;
                        NotificationUtil.success(
                            NotificationSources.autoBuy(), Language.translate("autobuy.scan.partner", "Цены соседней сессии приняты") + ": " + i
                        );
                    } else {
                        if (this.verifyTimer.isElapsed(600000L)) {
                            this.awaitingPartnerPrices = false;
                        }
                    }
                }
            }
        }
    }

    private AutoBuyItem findItem(AutoBuyManager autoBuyManager, ItemStack itemStack, int i) {
        AutoBuyItem autoBuyItem = autoBuyManager.findEnabledMatch(itemStack);
        return autoBuyItem != null && i <= autoBuyItem.getPrice() ? autoBuyItem : this.findItemInContainer(autoBuyManager, itemStack, i);
    }

    private AutoBuyItem findItemInContainer(AutoBuyManager autoBuyManager, ItemStack itemStack, int i) {
        ContainerComponent containerComponent = itemStack.get(DataComponentTypes.CONTAINER);
        if (containerComponent == null) {
            return null;
        }

        long l = 0L;
        AutoBuyItem autoBuyItem = null;

        for (ItemStack itemStack2 : containerComponent.iterateNonEmpty()) {
            AutoBuyItem autoBuyItem2 = autoBuyManager.findEnabledMatch(itemStack2);
            if (autoBuyItem2 != null) {
                l += (long)autoBuyItem2.getPrice() * Math.max(1, itemStack2.getCount());
                if (autoBuyItem == null || autoBuyItem2.getPrice() > autoBuyItem.getPrice()) {
                    autoBuyItem = autoBuyItem2;
                }
            }
        }

        return l >= i ? autoBuyItem : null;
    }

    private void scanAuction() {
        if (this.scanCount < 3) {
            this.scanCount++;
            int i = AuctionMarketLog.scanContainer(mc.player.currentScreenHandler);
            if (i > 0) {
                ChatUtil.print("Аукцион: +" + i + " / " + AuctionMarketLog.getItemCount());
            }

            if (this.scanCount < 3) {
                this.refreshAuction();
                return;
            }
        }

        int i2 = this.pageFlips < this.maxPageFlips ? this.getNextPageSlot() : -1;
        if (i2 < 0) {
            this.scanCount = 0;
            this.pageFlips = 0;
            this.maxPageFlips = ThreadLocalRandom.current().nextInt(1, 3);
            this.refreshAuction();
        } else {
            this.pageFlips++;
            this.clickSlotAndWait(i2);
        }
    }

    private int getNextPageSlot() {
        return AuctionUtil.getNextPageSlot();
    }

    private void requestVerification(AutoBuyItem autoBuyItem, int i, int i2, String string) {
        this.targetItem = autoBuyItem;
        this.targetSlot = i;
        this.targetPrice = i2;
        this.targetSeller = string;
        this.verification = AutoBuyLink.requestVerification(new AutoBuyLink.VerifyRequest(autoBuyItem.getName(), i2, string));
        this.verifyTimer.reset();
        this.state = AutoBuyController.State.VERIFYING;
    }

    private void tickVerifying() {
        if (!this.verification.isDone()) {
            if (this.verifyTimer.isElapsed(12000L)) {
                this.onPartnerLost();
            }
        } else {
            Boolean flag = this.verification.getNow(null);
            if (flag == null) {
                this.onPartnerLost();
            } else if (flag) {
                if (this.isTargetStillPresent()) {
                    this.buyLot(mc.player.currentScreenHandler, this.targetSlot, this.targetItem, this.targetPrice);
                } else {
                    this.resetCooldown();
                }
            } else {
                NotificationUtil.warning(
                    NotificationSources.autoBuy(), Language.translate("autobuy.verify.gone", "Лот не подтверждён") + ": " + this.targetItem.getId()
                );
                PurchaseHistory.add(this.targetItem, this.targetPrice, this.targetSeller, PurchaseHistory.Result.UNVERIFIED);
                if (this.rejectedLots.size() > 500) {
                    this.rejectedLots.clear();
                }

                this.rejectedLots.add(this.lotKey(this.targetItem, this.targetPrice, this.targetSeller));
                this.resetCooldown();
            }
        }
    }

    private boolean isTargetStillPresent() {
        if (mc.player == null) {
            return false;
        } else {
            ScreenHandler screenHandler = mc.player.currentScreenHandler;
            if (screenHandler != null && screenHandler.slots.size() >= 45 && this.isAuctionOpen()) {
                ItemStack itemStack = screenHandler.getSlot(this.targetSlot).getStack();
                return !itemStack.isEmpty()
                    && this.priceReader.getPrice(itemStack) == this.targetPrice
                    && this.targetSeller.equals(AuctionLoreUtil.getFunTimeSeller(itemStack));
            } else {
                return false;
            }
        }
    }

    private void onPartnerLost() {
        this.stop(Language.translate("autobuy.link.lost", "Проверяющая сессия не отвечает — автопокупка остановлена"));
    }

    private boolean isOwnLot(String string) {
        return string != null && mc.player != null && string.equalsIgnoreCase(mc.player.getGameProfile().name());
    }

    private String lotKey(AutoBuyItem autoBuyItem, int i, String string) {
        return autoBuyItem.getName() + "|" + i + "|" + string;
    }

    private void buyLot(ScreenHandler screenHandler, int i, AutoBuyItem autoBuyItem, int i2) {
        this.buyResponded = false;
        this.buyTimer.reset();
        this.lookAroundTimer.reset();
        this.state = AutoBuyController.State.BUYING;
        AuctionUtil.quickMoveSlot(i);
        NotificationUtil.info(NotificationSources.autoBuy(), Language.translate("autobuy.buying", "Покупаю") + " " + autoBuyItem.getId() + " — " + i2);
    }

    private void tickBuying() {
        if (this.buyResponded || this.buyTimer.isElapsed(3000L)) {
            this.buyResponded = false;
            PurchaseHistory.add(
                this.targetItem, this.targetPrice, this.targetSeller, this.buySucceeded ? PurchaseHistory.Result.BOUGHT : PurchaseHistory.Result.LOST
            );
            if (this.buySucceeded) {
                this.buySucceeded = false;
                if (this.isSpookyTime() && this.targetItem != null) {
                    this.resaleQueue.add(new AutoBuyController.ResaleEntry(this.targetItem, this.targetPrice));
                }

                ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
                RotationAnimator.animate(randomSign(threadLocalRandom) * threadLocalRandom.nextInt(70, 91), threadLocalRandom.nextInt(-8, 9), threadLocalRandom.nextInt(12, 19));
            }

            if (this.recoverPending) {
                this.recoverPending = false;
                this.startRecover();
            } else if (!this.isAuctionOpen()) {
                this.onAuctionClosed();
            } else {
                this.resetCooldown();
            }
        }
    }

    private void startRecover() {
        this.recoverStage = 1;
        this.recoverAttempts = 0;
        this.recoverDelay = this.randomDelay();
        this.recoverTimer.reset();
        this.state = AutoBuyController.State.RECOVER;
        AuctionUtil.leftClickSlot(53);
    }

    private void tickRecover() {
        if (this.recoverTimer.isElapsed(this.recoverDelay)) {
            this.recoverTimer.reset();
            this.recoverDelay = this.randomDelay();
            this.slotSearchAttempts = this.recoverAttempts;
            this.clickReturnButton();
            this.recoverAttempts = this.slotSearchAttempts;
        }
    }

    private boolean tryStartSelling() {
        if (!this.isSpookyTime() || this.resaleQueue.isEmpty() || !this.sellTimer.isElapsed(120000L) || AuctionUtil.isAutoBuyBusy()) {
            return false;
        }

        if (AutoSell.getInstance() == null) {
            return false;
        }

        this.sellTimer.reset();
        this.sellBlocked = false;
        this.sellStage = 1;
        this.sellStepTimer.reset();
        this.state = AutoBuyController.State.SELLING;
        AuctionUtil.closeScreen();
        return true;
    }

    private void tickSelling() {
        AutoSell autoSell = AutoSell.getInstance();
        if (autoSell != null && !this.sellBlocked) {
            switch (this.sellStage) {
                case 1:
                    if (AuctionUtil.isHandledScreenOpen()) {
                        AuctionUtil.closeScreen();
                        this.sellStepTimer.reset();
                        return;
                    } else {
                        if (!this.sellStepTimer.isElapsed(200L)) {
                            return;
                        }

                        this.sellStage = 2;
                        return;
                    }
                case 2:
                    if (this.resaleQueue.isEmpty()) {
                        this.finishSelling();
                        return;
                    } else {
                        int i = this.findInventorySlot(this.resaleQueue.get(0).entry());
                        if (i < 0) {
                            this.resaleQueue.remove(0);
                            return;
                        }

                        InventoryUtil.swapToHand(mc.player.currentScreenHandler.getSlot(i), Hand.MAIN_HAND);
                        this.sellStepTimer.reset();
                        this.sellStage = 3;
                        return;
                    }
                case 3:
                    if (!this.sellStepTimer.isElapsed(300L)) {
                        return;
                    }

                    if (!autoSell.isEnabled()) {
                        autoSell.setEnabled(true);
                    }

                    this.currentSale = this.resaleQueue.remove(0);
                    autoSell.startSell(this.currentSale.price());
                    this.sellStepTimer.reset();
                    this.sellStage = 4;
                    return;
                case 4:
                    if (autoSell.isBusy() && !this.sellStepTimer.isElapsed(20000L)) {
                        return;
                    }

                    if (this.currentSale != null && autoSell.isPriceBelowMinimum()) {
                        this.resaleQueue.add(this.currentSale);
                    }

                    this.currentSale = null;
                    this.sellStepTimer.reset();
                    this.sellStage = 5;
                    return;
                default:
                    if (this.sellStepTimer.isElapsed(200L)) {
                        if (this.resaleQueue.isEmpty()) {
                            this.finishSelling();
                        } else {
                            this.sellStage = 2;
                        }
                    }
            }
        } else {
            this.finishSelling();
        }
    }

    private void finishSelling() {
        this.sellStage = 0;
        this.lotsCheckTimer.reset();
        this.openAttempts = 0;
        this.firstOpenDelay = 3000L;
        this.openTimer.reset();
        this.state = AutoBuyController.State.OPENING;
    }

    private int findInventorySlot(AutoBuyItem autoBuyItem) {
        ScreenHandler screenHandler = mc.player == null ? null : mc.player.currentScreenHandler;
        if (screenHandler == null) {
            return -1;
        }

        int i = screenHandler.slots.size();

        for (int i2 = 0; i2 < i; i2++) {
            Slot slot = screenHandler.getSlot(i2);
            if (slot.inventory == mc.player.getInventory() && slot.getIndex() < 36 && autoBuyItem.matches(slot.getStack())) {
                return i2;
            }
        }

        return -1;
    }

    private boolean tryStartLotsCheck() {
        if (this.isSpookyTime() && this.lotsCheckTimer.isElapsed(60000L) && !AuctionUtil.isAutoBuyBusy()) {
            this.lotsCheckTimer.reset();
            this.lotsStage = 1;
            this.slotSearchAttempts = 0;
            this.lotsDelay = this.randomDelay();
            this.lotsStepTimer.reset();
            this.state = AutoBuyController.State.LOTS;
            AuctionUtil.leftClickSlot(46);
            return true;
        } else {
            return false;
        }
    }

    private void tickLots() {
        if (this.lotsStepTimer.isElapsed(this.lotsDelay)) {
            this.lotsStepTimer.reset();
            this.lotsDelay = this.randomDelay();
            if (this.lotsStage == 1) {
                int i = this.findContainerSlot(Items.CLOCK);
                if (i >= 0) {
                    AuctionUtil.leftClickSlot(i);
                    this.lotsStage = 2;
                    this.slotSearchAttempts = 0;
                } else if (++this.slotSearchAttempts >= 15) {
                    this.lotsStage = 2;
                    this.slotSearchAttempts = 0;
                }
            } else {
                this.clickReturnButton();
            }
        }
    }

    private void clickReturnButton() {
        int i = this.findReturnSlot();
        if (i >= 0) {
            AuctionUtil.leftClickSlot(i);
            this.lotsStage = 0;
            this.recoverStage = 0;
            this.resetCooldown();
        } else if (++this.slotSearchAttempts >= 15) {
            this.lotsStage = 0;
            this.recoverStage = 0;
            AuctionUtil.closeScreen();
            this.openAttempts = 0;
            this.firstOpenDelay = 3000L;
            this.openTimer.reset();
            this.state = AutoBuyController.State.OPENING;
        }
    }

    private int findReturnSlot() {
        return this.findContainerSlot(Items.SPECTRAL_ARROW);
    }

    private int findContainerSlot(Item item) {
        ScreenHandler screenHandler = mc.player == null ? null : mc.player.currentScreenHandler;
        if (screenHandler == null) {
            return -1;
        }

        int i = screenHandler.slots.size() - 36;

        for (int i2 = 0; i2 < i; i2++) {
            if (screenHandler.getSlot(i2).getStack().isOf(item)) {
                return i2;
            }
        }

        return -1;
    }

    private void hopOnTimeout() {
        long l = this.updateTimer.getElapsed();
        FunTimeAnarchies.markFull(ServerManager.getAnarchyNumber());
        int i = this.hopToFreeAnarchy();
        if (i > 0) {
            NotificationUtil.info(
                NotificationSources.autoBuy(), Language.translate("autobuy.hop", "Сервер не отвечает, перехожу на анархию") + " " + i + " (" + l + " мс)"
            );
        }
    }

    private void leaveHub() {
        if (this.anarchyNumber > 0) {
            FunTimeAnarchies.markFull(this.anarchyNumber);
        }

        int i = this.hopToFreeAnarchy();
        if (i > 0) {
            NotificationUtil.info(NotificationSources.autoBuy(), Language.translate("autobuy.hub", "Мы в хабе, захожу на анархию") + " " + i);
        }
    }

    private int hopToFreeAnarchy() {
        int i = FunTimeAnarchies.pickRandomAnarchy(ServerManager.getAnarchyNumber());
        if (i >= 0 && mc.getNetworkHandler() != null) {
            this.anarchyNumber = i;
            this.hopConfirmed = false;
            this.hopTimer.reset();
            this.hubTimer.reset();
            this.state = AutoBuyController.State.HOPPING;
            mc.getNetworkHandler().sendChatCommand("an" + i);
            AutoBuyLink.sendHop();
            return i;
        } else {
            this.stop(Language.translate("autobuy.stop.nofree", "Свободных анархий нет — автопокупка остановлена"));
            return -1;
        }
    }

    private void tickHopping() {
        if (ServerManager.getAnarchyNumber() == this.anarchyNumber) {
            this.openAttempts = 0;
            this.firstOpenDelay = 12000L;
            this.openTimer.reset();
            this.state = AutoBuyController.State.OPENING;
        } else {
            if (this.hopTimer.isElapsed(30000L)) {
                this.stop(Language.translate("autobuy.stop.hopfail", "Не удалось перейти на анархию — автопокупка остановлена"));
            }
        }
    }

    private void tickOpening() {
        if (this.isAuctionOpen()) {
            AuctionUtil.stopPathing();
            this.resetCooldown();
        } else {
            this.sendKeepAlive();
            if (this.openTimer.isElapsed(this.openAttempts == 0 ? this.firstOpenDelay : 3000L)) {
                if (this.openAttempts < 4 && mc.getNetworkHandler() != null) {
                    this.openAttempts++;
                    this.openTimer.reset();
                    mc.getNetworkHandler().sendChatCommand("ah");
                } else {
                    this.stop(Language.translate("autobuy.stop.noauction", "Откройте аукцион"));
                }
            }
        }
    }

    private void sendKeepAlive() {
        if (!this.isSpookyTime() && this.keepAliveTimer.isElapsed(6000L)) {
            this.keepAliveTimer.reset();
            AuctionUtil.wanderFar();
        }
    }

    private void onAuctionClosed() {
        if (this.auctionClosedTimer.isElapsed(1500L)) {
            if (!this.isSpookyTime() && !this.isScanMode()) {
                String string = Language.translate("autobuy.stop.closed", "Аукцион закрыт — автопокупка остановлена");
                if (mc.currentScreen != null) {
                    string = string + " (" + mc.currentScreen.getTitle().getString() + ")";
                }

                this.stop(string);
            } else {
                this.openAttempts = 0;
                this.firstOpenDelay = 3000L;
                this.openTimer.setElapsed(1500L);
                this.state = AutoBuyController.State.OPENING;
            }
        }
    }

    private void refreshAuction() {
        this.clickSlotAndWait(49);
    }

    private void clickSlotAndWait(int i) {
        ScreenHandler screenHandler = mc.player == null ? null : mc.player.currentScreenHandler;
        if (screenHandler != null) {
            this.updateTimer.reset();
            this.state = AutoBuyController.State.WAIT_UPDATE;
            AuctionUtil.leftClickSlot(i);
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (this.running && packetEvent.isReceive()) {
            if (this.isSpookyTime() && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
                String string = gameMessageS2CPacket.content().getString().toLowerCase(Locale.ROOT);
                if (string.contains("не удалось выставить")) {
                    this.sellBlocked = true;
                    this.resaleQueue.clear();
                    NotificationUtil.warning(
                        NotificationSources.autoBuy(), Language.translate("autobuy.sell.full", "Хранилище лотов заполнено — продажа остановлена")
                    );
                    return;
                }
            }

            if (this.state == AutoBuyController.State.HOPPING) {
                Packet packet = packetEvent.getPacket();
                if (packet instanceof GameMessageS2CPacket && FunTimeAnarchies.isServerFullMessage(((GameMessageS2CPacket)packet).content().getString())) {
                    this.hopConfirmed = true;
                    return;
                }
            } else {
                if (this.state != AutoBuyController.State.BUYING) {
                    if (this.state != AutoBuyController.State.WAIT_UPDATE) {
                        return;
                    }

                    Packet packet2 = packetEvent.getPacket();
                    if ((!(packet2 instanceof InventoryS2CPacket) || ((InventoryS2CPacket)packet2).syncId() == 0) && !(packet2 instanceof OpenScreenS2CPacket)) {
                        return;
                    }

                    this.lastResponseTime = this.updateTimer.getElapsed();
                    this.cooldown = this.randomDelay();
                    this.cooldownTimer.reset();
                    this.state = AutoBuyController.State.COOLDOWN;
                    return;
                }

                if (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket2) {
                    this.handleBuyResponse(gameMessageS2CPacket2);
                    return;
                }
            }
        }
    }

    private void handleBuyResponse(GameMessageS2CPacket gameMessageS2CPacket) {
        String string = gameMessageS2CPacket.content().getString().toLowerCase(Locale.ROOT);
        if (string.contains("успешно купили")) {
            NotificationUtil.success(NotificationSources.autoBuy(), Language.translate("autobuy.bought", "Куплено"));
            this.buySucceeded = true;
            this.buyResponded = true;
        } else {
            if (string.contains("уже купили") || string.contains("недостаточно")) {
                NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("autobuy.buy.failed", "Лот не достался"));
                this.recoverPending = this.isSpookyTime();
                this.buyResponded = true;
            }
        }
    }

    private void resetCooldown() {
        this.cooldown = this.randomDelay();
        this.cooldownTimer.reset();
        this.auctionClosedTimer.reset();
        this.scanCount = 0;
        this.pageFlips = 0;
        this.state = AutoBuyController.State.COOLDOWN;
    }

    private long randomDelay() {
        if (this.isScanMode()) {
            return ThreadLocalRandom.current().nextInt(1000, 1501);
        } else {
            return ServerManager.isSpookyTime() ? ThreadLocalRandom.current().nextInt(240, 360) : ThreadLocalRandom.current().nextInt(200, 250);
        }
    }

    private static int randomSign(ThreadLocalRandom threadLocalRandom) {
        return threadLocalRandom.nextBoolean() ? 1 : -1;
    }

    public boolean isAuctionOpen() {
        return AuctionUtil.isAuctionOpen();
    }

    public StopWatch getUpdateTimer() {
        return this.updateTimer;
    }

    public StopWatch getCooldownTimer() {
        return this.cooldownTimer;
    }

    public StopWatch getAuctionClosedTimer() {
        return this.auctionClosedTimer;
    }

    public StopWatch getHopTimer() {
        return this.hopTimer;
    }

    public StopWatch getOpenTimer() {
        return this.openTimer;
    }

    public StopWatch getHubTimer() {
        return this.hubTimer;
    }

    public StopWatch getBuyTimer() {
        return this.buyTimer;
    }

    public StopWatch getVerifyTimer() {
        return this.verifyTimer;
    }

    public StopWatch getLookAroundTimer() {
        return this.lookAroundTimer;
    }

    public StopWatch getKeepAliveTimer() {
        return this.keepAliveTimer;
    }

    public StopWatch getParseTimer() {
        return this.parseTimer;
    }

    public StopWatch getRecoverTimer() {
        return this.recoverTimer;
    }

    public StopWatch getSellTimer() {
        return this.sellTimer;
    }

    public StopWatch getSellStepTimer() {
        return this.sellStepTimer;
    }

    public StopWatch getLotsCheckTimer() {
        return this.lotsCheckTimer;
    }

    public StopWatch getLotsStepTimer() {
        return this.lotsStepTimer;
    }

    public StopWatch getPartnerPollTimer() {
        return this.partnerPollTimer;
    }

    public long getLookAroundDelay() {
        return this.lookAroundDelay;
    }

    public AuctionPriceParser getPriceReader() {
        return this.priceReader;
    }

    public Set<String> getRejectedLots() {
        return this.rejectedLots;
    }

    public Set<String> getUnaffordableLots() {
        return this.unaffordableLots;
    }

    public CompletableFuture<Boolean> getVerification() {
        return this.verification;
    }

    public AutoBuyItem getTargetItem() {
        return this.targetItem;
    }

    public String getTargetSeller() {
        return this.targetSeller;
    }

    public int getTargetSlot() {
        return this.targetSlot;
    }

    public int getTargetPrice() {
        return this.targetPrice;
    }

    public boolean isBuyResponded() {
        return this.buyResponded;
    }

    public boolean isBuySucceeded() {
        return this.buySucceeded;
    }

    public int getRecoverStage() {
        return this.recoverStage;
    }

    public int getRecoverAttempts() {
        return this.recoverAttempts;
    }

    public long getRecoverDelay() {
        return this.recoverDelay;
    }

    public boolean isRecoverPending() {
        return this.recoverPending;
    }

    public List<AutoBuyController.ResaleEntry> getResaleQueue() {
        return this.resaleQueue;
    }

    public AutoBuyController.ResaleEntry getCurrentSale() {
        return this.currentSale;
    }

    public int getSellStage() {
        return this.sellStage;
    }

    public boolean isSellBlocked() {
        return this.sellBlocked;
    }

    public int getLotsStage() {
        return this.lotsStage;
    }

    public int getSlotSearchAttempts() {
        return this.slotSearchAttempts;
    }

    public long getLotsDelay() {
        return this.lotsDelay;
    }

    public boolean isAwaitingPartnerPrices() {
        return this.awaitingPartnerPrices;
    }

    public int getScanCount() {
        return this.scanCount;
    }

    public int getPageFlips() {
        return this.pageFlips;
    }

    public int getMaxPageFlips() {
        return this.maxPageFlips;
    }

    public int getAnarchyNumber() {
        return this.anarchyNumber;
    }

    public int getOpenAttempts() {
        return this.openAttempts;
    }

    public long getFirstOpenDelay() {
        return this.firstOpenDelay;
    }

    public boolean isHopConfirmed() {
        return this.hopConfirmed;
    }

    public boolean isRunning() {
        return this.running;
    }

    public AutoBuyController.State getState() {
        return this.state;
    }

    public long getCooldown() {
        return this.cooldown;
    }

    public long getLastResponseTime() {
        return this.lastResponseTime;
    }

    record ResaleEntry(AutoBuyItem entry, int price) {
    }

    enum State {
        WAIT_UPDATE,
        COOLDOWN,
        HOPPING,
        OPENING,
        BUYING,
        VERIFYING,
        RECOVER,
        SELLING,
        LOTS;
    }
}
