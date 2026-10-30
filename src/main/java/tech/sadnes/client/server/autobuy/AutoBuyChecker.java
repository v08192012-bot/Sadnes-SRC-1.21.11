package tech.sadnes.client.server.autobuy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.AuctionUtil;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.server.FunTimeAnarchies;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.auction.AuctionLoreUtil;
import tech.sadnes.client.server.auction.AuctionPriceParser;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.rotation.RotationAnimator;

public final class AutoBuyChecker extends FeatureComponent {
    private static final long CLOSE_DELAY_MS = 500L;
    private static final long SEARCH_TIMEOUT_MS = 3000L;
    private static final long READ_DELAY_MS = 300L;
    private static final long LINK_RETRY_MS = 3000L;
    private static final int MAX_CLOSE_ATTEMPTS = 3;
    private static final long HUB_DETECT_MS = 3000L;
    private static final long HOP_COOLDOWN_MS = 15000L;
    private static final long WANDER_INTERVAL_MS = 10000L;
    private static final int WANDER_MIN_DISTANCE = 8;
    private static final int WANDER_MAX_DISTANCE = 13;
    private static final double ARRIVE_DISTANCE = 2.0;
    private static final long WANDER_TIMEOUT_MS = 20000L;
    private final AuctionPriceParser priceParser = new AuctionPriceParser();
    private final StopWatch stateTimer = new StopWatch();
    private final StopWatch linkRetryTimer = new StopWatch();
    private final StopWatch wanderCooldownTimer = new StopWatch();
    private final StopWatch hopTimer = new StopWatch();
    private final StopWatch hubTimer = new StopWatch();
    private final StopWatch lastHopTimer = new StopWatch();
    private final AtomicReference<AutoBuyChecker.PendingCheck> pendingCheck = new AtomicReference<>();
    final AtomicReference<List<String>> pendingScan = new AtomicReference<>();
    private volatile AutoBuyChecker.State state = AutoBuyChecker.State.IDLE;
    private volatile boolean searchOpened;
    private volatile boolean hopRequested;
    private volatile boolean serverFull;
    private AutoBuyChecker.PendingCheck currentCheck;
    private int closeAttempts;
    private int targetAnarchy = -1;
    private Vec3d wanderOrigin;
    private int wanderX;
    private int wanderZ;
    private boolean returning;
    private final StopWatch wanderTimer = new StopWatch();

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        this.update();
        this.updateWander();
    }

    @EventHandler
    public void onHandledScreenRender(HandledScreenRenderEvent handledScreenRenderEvent) {
        this.update();
    }

    private void update() {
        if (!this.isCheckMode()) {
            if (AutoBuyLink.isHosting()) {
                AutoBuyLink.stopServer();
                this.finishCheck(false);
            }
        } else if (AutoBuyLink.isHosting() || this.tryStartLink()) {
            this.processPendingScan();
            if (this.serverFull) {
                this.serverFull = false;
                FunTimeAnarchies.markFull(this.targetAnarchy);
                this.hopRequested = true;
            }

            this.checkHub();
            if (this.hopRequested && this.state == AutoBuyChecker.State.IDLE) {
                this.hopAnarchy();
            } else {
                switch (this.state) {
                    case IDLE:
                        this.pollCheck();
                        return;
                    case CLOSING:
                        if (this.stateTimer.isElapsed(500L)) {
                            this.openSearch();
                            return;
                        }
                        break;
                    case SEARCH:
                        this.waitForSearch();
                        return;
                    case READ:
                        if (this.stateTimer.isElapsed(300L)) {
                            this.readResults();
                        }
                }
            }
        }
    }

    private boolean tryStartLink() {
        if (!this.linkRetryTimer.isElapsed(3000L)) {
            return false;
        }

        this.linkRetryTimer.reset();
        AutoBuyLink.startServer(this::verifyOnClient, this::requestServerHop, new AutoBuyLink.Handler() {
            @Override
            public void onScan(List<String> strings) {
                AutoBuyChecker.this.pendingScan.set(strings);
            }

            @Override
            public Map<String, Integer> getPrices() {
                AutoBuyPriceScanner autoBuyPriceScanner = AutoBuyPriceScanner.getInstance();
                return autoBuyPriceScanner == null ? Map.of() : autoBuyPriceScanner.getFinishedPrices();
            }
        });
        if (!AutoBuyLink.isHosting()) {
            return false;
        }

        NotificationUtil.info(NotificationSources.autoBuy(), Language.translate("autobuy.check.ready", "Сессия проверки: жду покупающую"));
        return true;
    }

    private void processPendingScan() {
        List<String> strings = this.pendingScan.getAndSet(null);
        if (strings != null && !strings.isEmpty()) {
            AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
            AutoBuyPriceScanner autoBuyPriceScanner = AutoBuyPriceScanner.getInstance();
            if (autoBuyManager != null && autoBuyPriceScanner != null && !autoBuyPriceScanner.isRunning()) {
                ArrayList arrayList = new ArrayList();

                for (String string : strings) {
                    AutoBuyItem autoBuyItem = autoBuyManager.getByName(string);
                    if (autoBuyItem != null) {
                        arrayList.add(autoBuyItem);
                    }
                }

                this.stopWander();
                autoBuyPriceScanner.start(arrayList);
            }
        }
    }

    private Boolean verifyOnClient(AutoBuyLink.VerifyRequest verifyRequest) {
        CompletableFuture completableFuture = new CompletableFuture();
        this.pendingCheck.set(new AutoBuyChecker.PendingCheck(verifyRequest, completableFuture));

        try {
            return (Boolean)completableFuture.get(8L, TimeUnit.SECONDS);
        } catch (Exception exception) {
            return false;
        }
    }

    private void pollCheck() {
        AutoBuyChecker.PendingCheck pendingCheck2 = this.pendingCheck.getAndSet(null);
        if (pendingCheck2 != null) {
            this.currentCheck = pendingCheck2;
            AuctionUtil.stopPathing();
            this.closeAttempts = 0;
            this.openSearch();
        }
    }

    private void openSearch() {
        if (AuctionUtil.isHandledScreenOpen()) {
            if (!this.tryCloseScreen()) {
                this.finishCheck(false);
            }
        } else {
            AutoBuyItem autoBuyItem = this.getCheckedItem();
            if (autoBuyItem != null && mc.getNetworkHandler() != null) {
                this.searchOpened = false;
                this.stateTimer.reset();
                this.state = AutoBuyChecker.State.SEARCH;
                AuctionUtil.searchAuction(autoBuyItem.getCategoryName());
            } else {
                this.finishCheck(false);
            }
        }
    }

    private boolean tryCloseScreen() {
        if (mc.player != null && ++this.closeAttempts <= 3) {
            mc.player.closeHandledScreen();
            this.stateTimer.reset();
            this.state = AutoBuyChecker.State.CLOSING;
            return true;
        } else {
            return false;
        }
    }

    private void waitForSearch() {
        if (this.searchOpened) {
            this.stateTimer.reset();
            this.state = AutoBuyChecker.State.READ;
        } else {
            if (this.stateTimer.isElapsed(3000L)) {
                this.finishCheck(false);
            }
        }
    }

    private void readResults() {
        AutoBuyItem autoBuyItem = this.getCheckedItem();
        if (mc.player == null) {
            this.finishCheck(false);
        } else {
            ScreenHandler screenHandler = mc.player.currentScreenHandler;
            if (autoBuyItem != null && screenHandler != null && AuctionUtil.isAuctionOpen() && screenHandler.slots.size() >= 45) {
                this.finishCheck(this.hasMatchingLot(screenHandler, autoBuyItem));
            } else {
                this.finishCheck(false);
            }
        }
    }

    private boolean hasMatchingLot(ScreenHandler screenHandler, AutoBuyItem autoBuyItem) {
        int i = this.currentCheck.request().price();
        String string = this.currentCheck.request().seller();

        for (int i2 = 0; i2 < 45; i2++) {
            ItemStack itemStack = screenHandler.getSlot(i2).getStack();
            if (!itemStack.isEmpty()
                && autoBuyItem.matchesComponents(itemStack)
                && this.priceParser.getPrice(itemStack) == i
                && string.equals(AuctionLoreUtil.getFunTimeSeller(itemStack))) {
                return true;
            }
        }

        return false;
    }

    private void finishCheck(boolean flag) {
        AutoBuyChecker.PendingCheck pendingCheck2 = this.currentCheck;
        this.currentCheck = null;
        this.state = AutoBuyChecker.State.IDLE;
        this.wanderCooldownTimer.reset();
        if (pendingCheck2 != null) {
            pendingCheck2.answer().complete(flag);
        }

        if (mc.player != null && AuctionUtil.isHandledScreenOpen()) {
            mc.player.closeHandledScreen();
        }
    }

    private void requestServerHop() {
        this.hopRequested = true;
        this.hopTimer.reset();
    }

    private void checkHub() {
        if (!ServerManager.isInHub()) {
            this.hubTimer.reset();
        } else {
            if (this.hubTimer.isElapsed(3000L) && this.lastHopTimer.isElapsed(15000L)) {
                this.hopRequested = true;
            }
        }
    }

    private void hopAnarchy() {
        this.stopWander();
        if (AuctionUtil.isHandledScreenOpen()) {
            if (mc.player != null) {
                mc.player.closeHandledScreen();
            }

            this.hopTimer.reset();
        } else if (this.hopTimer.isElapsed(500L)) {
            this.hopRequested = false;
            int i = FunTimeAnarchies.pickRandomAnarchy(ServerManager.getAnarchyNumber());
            if (i >= 0 && mc.getNetworkHandler() != null) {
                boolean flag = ServerManager.isInHub();
                this.targetAnarchy = i;
                this.serverFull = false;
                this.lastHopTimer.reset();
                mc.getNetworkHandler().sendChatCommand("an" + i);
                NotificationUtil.info(
                    NotificationSources.autoBuy(),
                    (
                            flag
                                ? Language.translate("autobuy.hub", "Мы в хабе, захожу на анархию")
                                : Language.translate("autobuy.check.hop", "Покупающая сменила анархию, иду следом")
                        )
                        + " "
                        + i
                );
            }
        }
    }

    private void updateWander() {
        if (!this.isCheckMode() || mc.player == null) {
            this.stopWander();
        } else if (this.state == AutoBuyChecker.State.IDLE && !this.hopRequested && mc.currentScreen == null) {
            if (this.wanderOrigin == null) {
                if (this.wanderCooldownTimer.isElapsed(10000L)) {
                    this.startWander();
                }
            } else {
                this.continueWander();
            }
        } else {
            if (this.wanderOrigin != null) {
                AuctionUtil.stopPathing();
            }
        }
    }

    private void startWander() {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        this.wanderOrigin = mc.player.getEntityPos();
        this.returning = false;
        this.wanderX = (int)this.wanderOrigin.x + randomSign(threadLocalRandom) * threadLocalRandom.nextInt(8, 14);
        this.wanderZ = (int)this.wanderOrigin.z + randomSign(threadLocalRandom) * threadLocalRandom.nextInt(8, 14);
        if (!AuctionUtil.pathTo(this.wanderX, this.wanderZ)) {
            this.stopWander();
        } else {
            this.wanderTimer.reset();
            RotationAnimator.animate(randomSign(threadLocalRandom) * threadLocalRandom.nextInt(15, 46), threadLocalRandom.nextInt(-10, 11), threadLocalRandom.nextInt(10, 21));
        }
    }

    private void continueWander() {
        if (!this.hasReachedWanderTarget() && !this.wanderTimer.isElapsed(20000L)) {
            if (!AuctionUtil.isPathing()) {
                AuctionUtil.pathTo(this.wanderX, this.wanderZ);
            }
        } else if (this.returning) {
            this.stopWander();
        } else {
            this.returning = true;
            this.wanderX = (int)this.wanderOrigin.x;
            this.wanderZ = (int)this.wanderOrigin.z;
            this.wanderTimer.reset();
            AuctionUtil.pathTo(this.wanderX, this.wanderZ);
        }
    }

    private boolean hasReachedWanderTarget() {
        double d = mc.player.getX() - this.wanderX;
        double d2 = mc.player.getZ() - this.wanderZ;
        return d * d + d2 * d2 <= 4.0;
    }

    private void stopWander() {
        if (this.wanderOrigin != null) {
            this.wanderOrigin = null;
            AuctionUtil.stopPathing();
            this.wanderCooldownTimer.reset();
        }
    }

    private static int randomSign(ThreadLocalRandom threadLocalRandom) {
        return threadLocalRandom.nextBoolean() ? 1 : -1;
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()) {
            if (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
                if (FunTimeAnarchies.isServerFullMessage(gameMessageS2CPacket.content().getString())) {
                    this.serverFull = true;
                }
            } else {
                if (this.state != AutoBuyChecker.State.SEARCH) {
                    return;
                }

                Packet packet = packetEvent.getPacket();
                if (packet instanceof InventoryS2CPacket && ((InventoryS2CPacket)packet).syncId() != 0) {
                    this.searchOpened = true;
                    return;
                }

                if (packet instanceof OpenScreenS2CPacket) {
                    this.searchOpened = true;
                    return;
                }
            }
        }
    }

    private boolean isCheckMode() {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        return autoBuyManager != null && autoBuyManager.getMode() == AutoBuyManager.Mode.CHECK;
    }

    private AutoBuyItem getCheckedItem() {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        return autoBuyManager != null && this.currentCheck != null ? autoBuyManager.getByName(this.currentCheck.request().entry()) : null;
    }

    record PendingCheck(AutoBuyLink.VerifyRequest request, CompletableFuture<Boolean> answer) {
    }

    enum State {
        IDLE,
        CLOSING,
        SEARCH,
        READ;
    }
}
