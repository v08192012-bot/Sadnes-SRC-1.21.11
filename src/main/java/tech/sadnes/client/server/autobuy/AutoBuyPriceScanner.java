package tech.sadnes.client.server.autobuy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.Vec3d;
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

public final class AutoBuyPriceScanner extends FeatureComponent {
    private static final int MAX_EXTRA_PAGES = 2;
    private static final long SEARCH_TIMEOUT_MS = 4000L;
    private static final long UPDATE_TIMEOUT_MS = 1500L;
    private static final int MIN_COOLDOWN_MS = 270;
    private static final int MAX_COOLDOWN_MS = 500;
    private static final int MAX_SEARCH_ATTEMPTS = 5;
    private static final double WALK_DISTANCE = 5.0;
    private static final long WALK_TIMEOUT_MS = 8000L;
    private static final int MIN_SAMPLES = 3;
    private final AuctionPriceParser priceParser = new AuctionPriceParser();
    private final StopWatch timer = new StopWatch();
    private final List<AutoBuyItem> queue = new ArrayList<>();
    private final List<Integer> samples = new ArrayList<>();
    private final Set<String> seenLots = new HashSet<>();
    private final Map<String, Integer> scannedPrices = new LinkedHashMap<>();
    private volatile boolean finished;
    private volatile boolean running;
    private volatile AutoBuyPriceScanner.State state = AutoBuyPriceScanner.State.SEARCH;
    private volatile boolean updateReceived;
    private volatile boolean unknownItem;
    private long cooldownMs;
    private boolean resumeAutoBuy;
    private Vec3d walkStart;
    private AutoBuyItem currentItem;
    private String searchQuery = "";
    private int pagesRead;
    private int pageIndex;
    private int searchAttempts;
    private int totalItems;
    private int processedItems;

    public static AutoBuyPriceScanner getInstance() {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        return componentManager == null ? null : componentManager.getComponent(AutoBuyPriceScanner.class);
    }

    public void toggle() {
        if (this.running) {
            this.stop(Language.translate("autobuy.scan.stop", "Сбор цен остановлен"));
        } else {
            this.startEnabled();
        }
    }

    public void startEnabled() {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager != null) {
            ArrayList arrayList = new ArrayList();

            for (AutoBuyItem autoBuyItem : autoBuyManager.getItemsForCurrentServer()) {
                if (autoBuyItem.isEnabled() && autoBuyItem.getAutoParse().getValue()) {
                    arrayList.add(autoBuyItem);
                }
            }

            this.start(arrayList);
        }
    }

    public void start(List<AutoBuyItem> autoBuyItems) {
        if (!ServerManager.isFunTime() && !ServerManager.isSpookyTime()) {
            NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("autobuy.stop.server", "Работает только на FunTime"));
        } else {
            AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
            if (autoBuyManager != null && mc.getNetworkHandler() != null) {
                if (AuctionUtil.isAutoBuyBusy()) {
                    NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("autobuy.busy", "Аукцион сейчас занят другой задачей"));
                } else {
                    this.queue.clear();
                    this.scannedPrices.clear();
                    this.queue.addAll(autoBuyItems);
                    if (this.queue.isEmpty()) {
                        NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("autobuy.scan.empty", "Нет включённых предметов"));
                    } else {
                        Collections.shuffle(this.queue);
                        AutoBuyController autoBuyController = AutoBuyController.getInstance();
                        this.resumeAutoBuy = autoBuyController != null && autoBuyController.isRunning();
                        if (this.resumeAutoBuy) {
                            autoBuyController.stop(Language.translate("autobuy.scan.pause", "Сбор цен: автопокупка приостановлена"));
                        }

                        this.finished = false;
                        this.totalItems = this.queue.size();
                        this.processedItems = 0;
                        this.currentItem = null;
                        this.running = true;
                        NotificationUtil.info(NotificationSources.autoBuy(), Language.translate("autobuy.scan.start", "Сбор цен") + ": " + this.totalItems);
                        this.closeAndWait();
                    }
                }
            }
        }
    }

    public void stop(String string) {
        if (this.running) {
            this.running = false;
            this.resumeAutoBuy = false;
            AuctionUtil.stopPathing();
            this.walkStart = null;
            this.currentItem = null;
            this.queue.clear();
            NotificationUtil.info(NotificationSources.autoBuy(), string);
        }
    }

    @EventHandler
    public void onHandledScreenRender(HandledScreenRenderEvent handledScreenRenderEvent) {
        this.update();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        this.update();
    }

    private void update() {
        if (this.running) {
            switch (this.state) {
                case ITEM_DELAY:
                    if (this.timer.isElapsed(this.getActionDelay())) {
                        this.closeAndWait();
                        return;
                    }
                    break;
                case CLOSING:
                    if (this.timer.isElapsed(this.getActionDelay())) {
                        this.onClosed();
                        return;
                    }
                    break;
                case WALK:
                    this.updateWalk();
                    return;
                case SEARCH:
                    this.updateSearch();
                    return;
                case COOLDOWN:
                    this.updateCooldown();
                    return;
                case WAIT_UPDATE:
                    this.updateWaitUpdate();
                    return;
                case RESUME:
                    this.updateResume();
            }
        }
    }

    private long getActionDelay() {
        return ServerManager.isSpookyTime() ? 200L : 500L;
    }

    private void closeAndWait() {
        AuctionUtil.closeScreen();
        this.timer.reset();
        this.state = AutoBuyPriceScanner.State.CLOSING;
    }

    private void onClosed() {
        if (AuctionUtil.isHandledScreenOpen()) {
            this.closeAndWait();
        } else if (!ServerManager.isFunTime() || !this.tryWander()) {
            this.searchNextOrRetry();
        }
    }

    private void searchNextOrRetry() {
        if (this.currentItem == null) {
            this.nextItem();
        } else {
            this.searchCurrent();
        }
    }

    private boolean tryWander() {
        if (mc.player != null && AuctionUtil.wanderNear()) {
            this.walkStart = mc.player.getEntityPos();
            this.timer.reset();
            this.state = AutoBuyPriceScanner.State.WALK;
            return true;
        } else {
            return false;
        }
    }

    private void updateWalk() {
        if (mc.player != null && this.walkStart != null) {
            if (mc.player.getEntityPos().distanceTo(this.walkStart) >= 4.0) {
                AuctionUtil.stopPathing();
                this.walkStart = null;
                this.searchNextOrRetry();
                return;
            }

            if (!this.timer.isElapsed(8000L)) {
                return;
            }
        }

        AuctionUtil.stopPathing();
        this.walkStart = null;
        this.searchNextOrRetry();
    }

    private void updateSearch() {
        if (this.unknownItem) {
            this.unknownItem = false;
            NotificationUtil.warning(
                NotificationSources.autoBuy(),
                this.currentItem.getId()
                    + ": "
                    + Language.translate("autobuy.scan.unknown", "сервер не знает такого предмета")
                    + " («"
                    + this.searchQuery
                    + "»)"
            );
            this.finishItem();
        } else if (this.isAuctionOpen()) {
            this.startCooldown();
        } else {
            if (this.timer.isElapsed(4000L)) {
                this.closeAndWait();
            }
        }
    }

    private void updateWaitUpdate() {
        if (this.updateReceived) {
            this.startCooldown();
        } else if (this.timer.isElapsed(1500L)) {
            if (this.samples.size() >= 3) {
                this.finishItem();
            } else {
                this.closeAndWait();
            }
        }
    }

    private void updateCooldown() {
        if (this.timer.isElapsed(this.cooldownMs)) {
            if (!this.isAuctionOpen()) {
                if (this.samples.size() >= 3) {
                    this.finishItem();
                } else {
                    this.closeAndWait();
                }
            } else {
                this.collectPrices();
                if (this.pagesRead == 1 && this.samples.isEmpty()) {
                    this.finishItem();
                } else if (this.pageIndex < this.getMaxPages()) {
                    this.pageIndex++;
                    this.clickSlot(49);
                } else {
                    this.finishItem();
                }
            }
        }
    }

    private int getMaxPages() {
        return ServerManager.isSpookyTime() ? 0 : 2;
    }

    private void startCooldown() {
        this.cooldownMs = ThreadLocalRandom.current().nextInt(270, 501);
        this.timer.reset();
        this.state = AutoBuyPriceScanner.State.COOLDOWN;
    }

    private void clickSlot(int i) {
        ScreenHandler screenHandler = mc.player == null ? null : mc.player.currentScreenHandler;
        if (screenHandler == null) {
            this.finishItem();
        } else {
            this.updateReceived = false;
            this.timer.reset();
            this.state = AutoBuyPriceScanner.State.WAIT_UPDATE;
            AuctionUtil.leftClickSlot(i);
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (this.running && packetEvent.isReceive()) {
            if (!(this.state == AutoBuyPriceScanner.State.SEARCH && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket)) {
                if (this.state != AutoBuyPriceScanner.State.WAIT_UPDATE) {
                    return;
                }

                Packet packet = packetEvent.getPacket();
                if (packet instanceof InventoryS2CPacket && ((InventoryS2CPacket)packet).syncId() != 0) {
                    this.updateReceived = true;
                    return;
                }

                if (!(packet instanceof OpenScreenS2CPacket)) {
                    return;
                }

                this.updateReceived = true;
                return;
            }

            String string = gameMessageS2CPacket.content().getString().toLowerCase(Locale.ROOT);
            if (string.contains("такого предмета") || string.contains("не существует")) {
                this.unknownItem = true;
                return;
            }
        }
    }

    private void nextItem() {
        if (this.queue.isEmpty()) {
            this.finishScan();
        } else {
            this.currentItem = this.queue.remove(0);
            this.searchQuery = this.buildSearchQuery(this.currentItem);
            this.samples.clear();
            this.seenLots.clear();
            this.pagesRead = 0;
            this.pageIndex = 0;
            this.searchAttempts = 0;
            this.searchCurrent();
        }
    }

    private void searchCurrent() {
        this.searchAttempts++;
        if (!this.searchQuery.isEmpty() && mc.getNetworkHandler() != null && this.searchAttempts <= 5) {
            this.updateReceived = false;
            this.unknownItem = false;
            this.timer.reset();
            this.state = AutoBuyPriceScanner.State.SEARCH;
            AuctionUtil.searchAuction(this.searchQuery);
        } else {
            this.finishItem();
        }
    }

    private void finishItem() {
        AutoBuyItem autoBuyItem = this.currentItem;
        this.currentItem = null;
        this.processedItems++;
        this.timer.reset();
        this.state = AutoBuyPriceScanner.State.ITEM_DELAY;
        if (autoBuyItem != null) {
            if (this.samples.size() >= 3) {
                int i = AuctionUtil.averageLowestPrices(this.samples);
                int i2 = Math.max(1, Math.round(i * autoBuyItem.getDiscountedPrice()));
                autoBuyItem.setPrice(i2);
                this.scannedPrices.put(autoBuyItem.getName(), i2);
                AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
                if (autoBuyManager != null) {
                    autoBuyManager.save();
                }

                NotificationUtil.success(
                    NotificationSources.autoBuy(),
                    autoBuyItem.getId() + ": " + i2 + " (" + Language.translate("autobuy.scan.market", "рынок") + " " + i + ", " + this.samples.size() + ")"
                );
            }
        }
    }

    private void finishScan() {
        this.finished = true;
        this.currentItem = null;
        NotificationUtil.success(
            NotificationSources.autoBuy(), Language.translate("autobuy.scan.done", "Сбор цен завершён") + ": " + this.processedItems + "/" + this.totalItems
        );
        if (this.resumeAutoBuy && mc.getNetworkHandler() != null) {
            this.timer.reset();
            this.state = AutoBuyPriceScanner.State.RESUME;
            mc.getNetworkHandler().sendChatCommand("ah");
        } else {
            this.running = false;
        }
    }

    private void updateResume() {
        AutoBuyController autoBuyController = AutoBuyController.getInstance();
        if (AuctionUtil.isAuctionOpen() && autoBuyController != null) {
            this.running = false;
            this.resumeAutoBuy = false;
            autoBuyController.start();
        } else if (this.timer.isElapsed(10000L)) {
            this.running = false;
            this.resumeAutoBuy = false;
        }
    }

    public Map<String, Integer> getFinishedPrices() {
        return this.finished ? new LinkedHashMap<>(this.scannedPrices) : Map.of();
    }

    public static int applyPrices(Map<String, Integer> map) {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager != null && !map.isEmpty()) {
            int i = 0;

            for (Entry entry : map.entrySet()) {
                AutoBuyItem autoBuyItem = autoBuyManager.getByName((String)entry.getKey());
                if (autoBuyItem != null && (Integer)entry.getValue() > 0) {
                    autoBuyItem.setPrice((Integer)entry.getValue());
                    i++;
                }
            }

            if (i > 0) {
                autoBuyManager.save();
            }

            return i;
        } else {
            return 0;
        }
    }

    private void collectPrices() {
        if (mc.player != null) {
            ScreenHandler screenHandler = mc.player.currentScreenHandler;
            if (screenHandler != null && screenHandler.slots.size() >= 45) {
                for (int i = 0; i < 45; i++) {
                    ItemStack itemStack = screenHandler.getSlot(i).getStack();
                    if (!itemStack.isEmpty() && this.currentItem.matches(itemStack)) {
                        this.currentItem.tryMatch(itemStack);
                        int i2 = this.priceParser.getPrice(itemStack);
                        if (i2 > 0 && this.seenLots.add(this.getLotKey(itemStack, i2))) {
                            int i3 = Math.max(1, itemStack.getCount());
                            this.samples.add(i2 / i3);
                        }
                    }
                }

                this.pagesRead++;
            }
        }
    }

    private String getLotKey(ItemStack itemStack, int i) {
        NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
        String string = nbtComponent == null ? "" : nbtComponent.copyNbt().toString();
        return string + "|" + itemStack.getName().getString() + "|" + i + "|" + itemStack.getCount() + "|" + AuctionLoreUtil.getFunTimeSeller(itemStack);
    }

    private boolean isAuctionOpen() {
        return AuctionUtil.isAuctionOpen();
    }

    private String buildSearchQuery(AutoBuyItem autoBuyItem) {
        return AuctionLoreUtil.stripSymbols(autoBuyItem.getCategoryName());
    }

    public AuctionPriceParser getPriceParser() {
        return this.priceParser;
    }

    public StopWatch getTimer() {
        return this.timer;
    }

    public List<AutoBuyItem> getQueue() {
        return this.queue;
    }

    public List<Integer> getSamples() {
        return this.samples;
    }

    public Set<String> getSeenLots() {
        return this.seenLots;
    }

    public Map<String, Integer> getScannedPrices() {
        return this.scannedPrices;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public boolean isRunning() {
        return this.running;
    }

    public AutoBuyPriceScanner.State getState() {
        return this.state;
    }

    public boolean isUpdateReceived() {
        return this.updateReceived;
    }

    public boolean isUnknownItem() {
        return this.unknownItem;
    }

    public long getCooldownMs() {
        return this.cooldownMs;
    }

    public boolean isResumeAutoBuy() {
        return this.resumeAutoBuy;
    }

    public Vec3d getWalkStart() {
        return this.walkStart;
    }

    public AutoBuyItem getCurrentItem() {
        return this.currentItem;
    }

    public String getSearchQuery() {
        return this.searchQuery;
    }

    public int getPagesRead() {
        return this.pagesRead;
    }

    public int getPageIndex() {
        return this.pageIndex;
    }

    public int getSearchAttempts() {
        return this.searchAttempts;
    }

    public int getTotalItems() {
        return this.totalItems;
    }

    public int getProcessedItems() {
        return this.processedItems;
    }

    enum State {
        ITEM_DELAY,
        CLOSING,
        WALK,
        SEARCH,
        COOLDOWN,
        WAIT_UPDATE,
        RESUME;
    }
}
