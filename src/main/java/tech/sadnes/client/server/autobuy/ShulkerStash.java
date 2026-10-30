package tech.sadnes.client.server.autobuy;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.HandledScreenRenderEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.AuctionUtil;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.rotation.RotationUtil;

public final class ShulkerStash extends FeatureComponent {
    private static final int MIN_MOVE_DELAY_MS = 140;
    private static final int MAX_MOVE_DELAY_MS = 320;
    private static final long BUY_TIMEOUT_MS = 20000L;
    private static final long OPEN_RETRY_MS = 5000L;
    private static final long HOP_TIMEOUT_MS = 30000L;
    private static final long SETTLE_DELAY_MS = 6000L;
    private static final int MAX_PLACE_ATTEMPTS = 5;
    private final StopWatch stateTimer = new StopWatch();
    private final StopWatch moveTimer = new StopWatch();
    private volatile ShulkerStash.State state = ShulkerStash.State.IDLE;
    private long moveDelay;
    private int placeAttempts;
    private boolean resumeAutoBuy;

    public static ShulkerStash getInstance() {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        return componentManager == null ? null : componentManager.getComponent(ShulkerStash.class);
    }

    public boolean isActive() {
        return this.state != ShulkerStash.State.IDLE;
    }

    public void start() {
        if (!this.isActive()) {
            AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
            if (autoBuyManager != null && mc.player != null) {
                if (!ServerManager.isFunTime()) {
                    NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("autobuy.stop.server", "Работает только на FunTime"));
                } else if (AuctionUtil.isAutoBuyBusy()) {
                    NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("autobuy.busy", "Аукцион сейчас занят другой задачей"));
                } else if (autoBuyManager.getStashAnarchy() <= 0) {
                    NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("autobuy.stash.noanarchy", "Выберите анархию для склада"));
                } else {
                    AutoBuyController autoBuyController = AutoBuyController.getInstance();
                    this.resumeAutoBuy = autoBuyController != null && autoBuyController.isRunning();
                    if (this.resumeAutoBuy) {
                        autoBuyController.stop(Language.translate("autobuy.stash.pause", "Вывоз в шалкер: автопокупка приостановлена"));
                    }

                    this.placeAttempts = 0;
                    NotificationUtil.info(NotificationSources.autoBuy(), Language.translate("autobuy.stash.start", "Собираю вещи в шалкер"));
                    if (this.findShulkerSlot() >= 0) {
                        this.setState(ShulkerStash.State.OPEN);
                    } else {
                        this.buyShulker();
                    }
                }
            }
        }
    }

    public void stop(String string) {
        if (this.isActive()) {
            this.state = ShulkerStash.State.IDLE;
            NotificationUtil.info(NotificationSources.autoBuy(), string);
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        this.update();
    }

    @EventHandler
    public void onHandledScreenRender(HandledScreenRenderEvent handledScreenRenderEvent) {
        this.update();
    }

    private void update() {
        if (this.isActive()) {
            if (mc.player == null) {
                this.state = ShulkerStash.State.IDLE;
            } else {
                switch (this.state) {
                    case BUY:
                        this.updateBuy();
                        return;
                    case OPEN:
                        this.updateOpen();
                        return;
                    case FILL:
                        this.updateFill();
                        return;
                    case HOP:
                        this.updateHop();
                        return;
                    case SETTLE:
                        if (this.stateTimer.isElapsed(6000L)) {
                            this.setState(ShulkerStash.State.PLACE);
                            return;
                        }
                        break;
                    case PLACE:
                        this.updatePlace();
                        return;
                    case RESUME:
                        this.updateResume();
                }
            }
        }
    }

    private void setState(ShulkerStash.State state2) {
        this.stateTimer.reset();
        this.state = state2;
    }

    private void buyShulker() {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        AutoBuyItem autoBuyItem = autoBuyManager == null ? null : this.findShulkerEntry(autoBuyManager);
        if (autoBuyItem == null) {
            this.fail(Language.translate("autobuy.stash.noentry", "В каталоге нет записи шалкера"));
        } else {
            this.setState(ShulkerStash.State.BUY);
            AuctionUtil.buy(autoBuyItem);
        }
    }

    private void updateBuy() {
        if (this.findShulkerSlot() >= 0) {
            this.setState(ShulkerStash.State.OPEN);
        } else {
            if (this.stateTimer.isElapsed(20000L)) {
                this.fail(Language.translate("autobuy.stash.nobox", "Не удалось купить шалкер"));
            }
        }
    }

    private void updateOpen() {
        if (AuctionUtil.isContainerOpen()) {
            this.moveTimer.reset();
            this.moveDelay = 0L;
            this.setState(ShulkerStash.State.FILL);
        } else if (this.stateTimer.isElapsed(5000L)) {
            int i = this.findShulkerSlot();
            if (i < 0) {
                this.fail(Language.translate("autobuy.stash.nobox", "Не удалось купить шалкер"));
            } else if (AuctionUtil.isAuctionOpen()) {
                AuctionUtil.closeScreen();
                this.stateTimer.reset();
            } else {
                this.stateTimer.reset();
                AuctionUtil.rightClickSlot(i);
            }
        }
    }

    private void updateFill() {
        if (!AuctionUtil.isContainerOpen()) {
            this.setState(ShulkerStash.State.HOP);
            this.hopToStash();
        } else if (this.moveTimer.isElapsed(this.moveDelay)) {
            ScreenHandler screenHandler = mc.player.currentScreenHandler;
            int i = this.findItemToMove(screenHandler);
            if (i < 0) {
                AuctionUtil.closeScreen();
                this.setState(ShulkerStash.State.HOP);
                this.hopToStash();
            } else {
                this.moveTimer.reset();
                this.moveDelay = ThreadLocalRandom.current().nextInt(140, 321);
                AuctionUtil.quickMoveSlot(i);
            }
        }
    }

    private int findItemToMove(ScreenHandler screenHandler) {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager == null) {
            return -1;
        }

        int i = screenHandler.slots.size();

        for (int i2 = i - 36; i2 < i; i2++) {
            Slot slot = screenHandler.getSlot(i2);
            ItemStack itemStack = slot.getStack();
            if (!itemStack.isEmpty() && !AuctionUtil.isFirstTagItem(itemStack) && autoBuyManager.findEnabledMatch(itemStack) != null) {
                return i2;
            }
        }

        return -1;
    }

    private void hopToStash() {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendChatCommand("an" + autoBuyManager.getStashAnarchy());
        }
    }

    private void updateHop() {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager != null && ServerManager.getAnarchyNumber() == autoBuyManager.getStashAnarchy()) {
            this.setState(ShulkerStash.State.SETTLE);
        } else {
            if (this.stateTimer.isElapsed(30000L)) {
                this.fail(Language.translate("autobuy.stash.hopfail", "Не удалось зайти на анархию склада"));
            }
        }
    }

    private void updatePlace() {
        int i = this.findShulkerSlot();
        if (i < 0) {
            this.finish();
        } else if (AuctionUtil.isHandledScreenOpen()) {
            AuctionUtil.closeScreen();
        } else if (!AuctionUtil.isFirstTagItem(mc.player.getMainHandStack())) {
            InventoryUtil.swapToHand(mc.player.currentScreenHandler.getSlot(i), Hand.MAIN_HAND);
        } else {
            BlockPos blockPos = this.findPlacePos();
            if (blockPos == null) {
                this.onPlaceFailed();
            } else {
                RotationComponent.rotateTo(new Rotation(RotationUtil.getRotationsTo(Vec3d.ofCenter(blockPos).add(0.0, 0.5, 0.0))), 40.0F, 40.0F, 1, 12);
                if (mc.getCameraEntity().raycast(4.5, 0.0F, false) instanceof BlockHitResult blockHitResult) {
                    if (blockHitResult.getBlockPos().equals(blockPos) && blockHitResult.getSide() == Direction.UP) {
                        if (!mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult).isAccepted()) {
                            this.onPlaceFailed();
                        } else {
                            mc.player.swingHand(Hand.MAIN_HAND);
                        }
                    }
                }
            }
        }
    }

    private void onPlaceFailed() {
        if (this.stateTimer.isElapsed(1000L)) {
            this.stateTimer.reset();
            if (++this.placeAttempts > 5) {
                this.fail(Language.translate("autobuy.stash.noplace", "Некуда поставить шалкер"));
            }
        }
    }

    private BlockPos findPlacePos() {
        BlockPos blockPos = mc.player.getBlockPos();

        for (Direction direction : Type.HORIZONTAL) {
            BlockPos blockPos2 = blockPos.offset(direction);
            BlockPos blockPos3 = blockPos2.down();
            if (mc.world.getBlockState(blockPos2).isReplaceable() && mc.world.getBlockState(blockPos3).isSolidBlock(mc.world, blockPos3)) {
                return blockPos3;
            }
        }

        return null;
    }

    private void finish() {
        NotificationUtil.success(NotificationSources.autoBuy(), Language.translate("autobuy.stash.done", "Шалкер поставлен"));
        if (this.resumeAutoBuy && mc.getNetworkHandler() != null) {
            this.setState(ShulkerStash.State.RESUME);
            mc.getNetworkHandler().sendChatCommand("ah");
        } else {
            this.state = ShulkerStash.State.IDLE;
        }
    }

    private void updateResume() {
        AutoBuyController autoBuyController = AutoBuyController.getInstance();
        if (AuctionUtil.isAuctionOpen() && autoBuyController != null) {
            this.state = ShulkerStash.State.IDLE;
            this.resumeAutoBuy = false;
            autoBuyController.start();
        } else {
            if (this.stateTimer.isElapsed(10000L)) {
                this.state = ShulkerStash.State.IDLE;
                this.resumeAutoBuy = false;
            }
        }
    }

    private void fail(String string) {
        this.state = ShulkerStash.State.IDLE;
        NotificationUtil.warning(NotificationSources.autoBuy(), string);
    }

    private int findShulkerSlot() {
        ScreenHandler screenHandler = mc.player == null ? null : mc.player.currentScreenHandler;
        if (screenHandler == null) {
            return -1;
        }

        int i = screenHandler.slots.size();

        for (int i2 = i - 36; i2 < i; i2++) {
            if (AuctionUtil.isFirstTagItem(screenHandler.getSlot(i2).getStack())) {
                return i2;
            }
        }

        return -1;
    }

    private AutoBuyItem findShulkerEntry(AutoBuyManager autoBuyManager) {
        ItemStack itemStack = new ItemStack(Items.SHULKER_BOX);

        for (AutoBuyItem autoBuyItem : autoBuyManager.getItemsForCurrentServer()) {
            if (autoBuyItem.matches(itemStack)) {
                return autoBuyItem;
            }
        }

        return null;
    }

    public StopWatch getStateTimer() {
        return this.stateTimer;
    }

    public StopWatch getMoveTimer() {
        return this.moveTimer;
    }

    public ShulkerStash.State getState() {
        return this.state;
    }

    public long getMoveDelay() {
        return this.moveDelay;
    }

    public int getPlaceAttempts() {
        return this.placeAttempts;
    }

    public boolean isResumeAutoBuy() {
        return this.resumeAutoBuy;
    }

    enum State {
        IDLE,
        BUY,
        OPEN,
        FILL,
        HOP,
        SETTLE,
        PLACE,
        RESUME;
    }
}
