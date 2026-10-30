package tech.sadnes.client.server.autobuy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.BundleItemSelectedC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.HandledScreenRenderEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.AuctionUtil;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.InventoryUtil;

public final class KitCollector extends FeatureComponent {
    private static final long SETTLE_TIMEOUT_MS = 3000L;
    private static final long BUY_COOLDOWN_MS = 3000L;
    private static final int SHULKER_SIZE = 27;
    private final StopWatch buyTimer = new StopWatch();
    private final StopWatch settleTimer = new StopWatch();
    private final StopWatch actionTimer = new StopWatch();
    private final StopWatch shulkerOpenTimer = new StopWatch();
    private final StopWatch clickTimer = new StopWatch();
    private long clickDelay = 200L;
    private boolean shulkerOpen;
    private int openedShulkerSlot = -1;
    private final Set<Integer> checkedShulkerSlots = new HashSet<>();
    private int failedShulkerOpens;
    private boolean inventoryOpened;
    private volatile boolean running;
    private boolean buying;
    private boolean settling;
    private int lastFromSlot = -1;
    private int lastToSlot = -1;
    private final StopWatch lastMoveTimer = new StopWatch();
    private final Map<AutoBuyItem, Integer> buyFailures = new IdentityHashMap<>();
    private boolean partial;

    public static KitCollector getInstance() {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        return componentManager == null ? null : componentManager.getComponent(KitCollector.class);
    }

    public static Text getRunButtonText() {
        KitCollector kitCollector = getInstance();
        boolean flag;
        if (kitCollector != null) {
            flag = kitCollector.isRunning();
        } else {
            flag = false;
        }

        return flag
            ? Text.literal(Language.translate("collector.run.button.stop", "Стоп сборки"))
            : Text.literal(Language.translate("collector.run.button.start", "Собрать"));
    }

    public void toggle() {
        if (this.running) {
            this.stop(Language.translate("collector.run.stopped", "Сборка остановлена"));
        } else {
            this.start();
        }
    }

    public void start() {
        if (!this.running) {
            CollectorManager collectorManager = Sadnes.getInstance().getCollectorManager();
            if (collectorManager != null && mc.player != null) {
                if (!collectorManager.getServer().isCurrent()) {
                    NotificationUtil.warning(NotificationSources.autoBuy(), Language.translate("collector.run.wrongserver", "Зайдите на сервер раскладки"));
                } else {
                    this.running = true;
                    this.buying = false;
                    this.shulkerOpen = false;
                    this.failedShulkerOpens = 0;
                    this.settling = false;
                    this.inventoryOpened = false;
                    this.buyFailures.clear();
                    this.checkedShulkerSlots.clear();
                    this.openedShulkerSlot = -1;
                    AuctionUtil.closeScreen();
                    this.buyTimer.reset();
                    NotificationUtil.info(NotificationSources.autoBuy(), Language.translate("collector.run.start", "Собираю раскладку"));
                    int i = collectorManager.countUnavailable();
                    if (i > 0) {
                        NotificationUtil.warning(
                            NotificationSources.autoBuy(), Language.translate("collector.run.foreign", "Предметов другого сервера в раскладке") + ": " + i
                        );
                    }
                }
            }
        }
    }

    public void stop(String string) {
        if (this.running) {
            this.running = false;
            this.shulkerOpen = false;
            this.settling = false;
            if (this.inventoryOpened) {
                AuctionUtil.stopPathing();
                this.inventoryOpened = false;
            }

            if (string != null) {
                NotificationUtil.info(NotificationSources.autoBuy(), string);
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
        if (this.running) {
            CollectorManager collectorManager = Sadnes.getInstance().getCollectorManager();
            if (collectorManager == null || mc.player == null) {
                this.running = false;
            } else if (collectorManager.getServer().isCurrent()) {
                if (this.inventoryOpened) {
                    this.tickInventoryClose();
                } else {
                    boolean flag = this.actionTimer.isElapsed(500L);
                    if (!flag || !this.handleShulker(collectorManager)) {
                        boolean flag2;
                        label110: {
                            boolean flag3;
                            if (flag && !AuctionUtil.isHandledScreenOpen()) {
                                if (!this.arrangeSlot(collectorManager)) {
                                    flag2 = this.fillBundles(collectorManager);
                                    break label110;
                                }

                                flag3 = true;
                            } else {
                                flag3 = false;
                            }

                            flag2 = flag3;
                        }

                        if (AuctionUtil.isAutoBuyBusy()) {
                            this.buying = true;
                            this.buyTimer.reset();
                            this.actionTimer.reset();
                        } else if (this.buying) {
                            this.buying = false;
                            this.recordPurchaseResult();
                            AuctionUtil.closeScreen();
                            this.settling = true;
                            this.settleTimer.reset();
                        } else {
                            if (this.settling) {
                                if (this.settleTimer.isElapsed(3000L)) {
                                    this.settling = false;
                                } else {
                                    if (AuctionUtil.isHandledScreenOpen()) {
                                        AuctionUtil.closeScreen();
                                        return;
                                    }

                                    if (!flag || flag2) {
                                        return;
                                    }

                                    this.settling = false;
                                    if (this.tryOpenInventory()) {
                                        return;
                                    }
                                }
                            }

                            if (this.buyTimer.hasPassed(3000.0)) {
                                KitCollector.Purchase purchase = this.findNextPurchase(collectorManager);
                                if (purchase != null) {
                                    int i = collectorManager.hasPrice(purchase.item()) ? collectorManager.getPrice(purchase.item()) : 0;
                                    AuctionUtil.buy(purchase.item(), purchase.need(), i, () -> this.getMissingCount(collectorManager, purchase.item()));
                                } else if (!flag2 && this.findShulkerToOpen(collectorManager, this.getRequiredItems(collectorManager)) < 0) {
                                    if (this.partial) {
                                        this.stop(Language.translate("collector.run.partial", "Часть предметов не нашлась"));
                                    } else {
                                        this.stop(Language.translate("collector.run.done", "Раскладка собрана"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean tryOpenInventory() {
        if (mc.player != null && AuctionUtil.wanderNear()) {
            this.inventoryOpened = true;
            this.settleTimer.reset();
            return true;
        } else {
            return false;
        }
    }

    private void tickInventoryClose() {
        if (this.settleTimer.isElapsed(3000L)) {
            AuctionUtil.stopPathing();
            this.inventoryOpened = false;
        }
    }

    private Map<AutoBuyItem, Integer> getRequiredItems(CollectorManager collectorManager) {
        Map<AutoBuyItem, Integer> map = this.getRequiredSlotItems(collectorManager);

        for (int i = 0; i < 41; i++) {
            for (CollectorManager.BundleEntry bundleEntry : collectorManager.getSlot(i).getBundleEntries()) {
                if (!collectorManager.isUnavailable(bundleEntry.getItem())) {
                    map.merge(bundleEntry.getItem(), bundleEntry.getCount(), Integer::sum);
                }
            }
        }

        return map;
    }

    private Map<AutoBuyItem, Integer> getRequiredSlotItems(CollectorManager collectorManager) {
        LinkedHashMap<AutoBuyItem, Integer> linkedHashMap = new LinkedHashMap<>();

        for (int i = 0; i < 41; i++) {
            CollectorManager.CollectorSlot collectorSlot = collectorManager.getSlot(i);
            if (!collectorSlot.isEmpty() && !collectorManager.isUnavailable(collectorSlot.getItem())) {
                linkedHashMap.merge(collectorSlot.getItem(), collectorSlot.getCount(), Integer::sum);
            }
        }

        return linkedHashMap;
    }

    private KitCollector.Purchase findNextPurchase(CollectorManager collectorManager) {
        Map<AutoBuyItem, Integer> map = this.getRequiredItems(collectorManager);
        if (!this.hasEnoughBundles(map)) {
            map = this.getRequiredSlotItems(collectorManager);
        }

        ArrayList<KitCollector.Purchase> purchases = new ArrayList<>();
        this.partial = false;

        for (Entry<AutoBuyItem, Integer> entry : map.entrySet()) {
            int i = (Integer)entry.getValue() - this.countOwned((AutoBuyItem)entry.getKey());
            if (i > 0) {
                if (this.buyFailures.getOrDefault(entry.getKey(), 0) >= 3) {
                    this.partial = true;
                } else {
                    purchases.add(new KitCollector.Purchase((AutoBuyItem)entry.getKey(), i));
                }
            }
        }

        if (purchases.isEmpty()) {
            return null;
        }

        purchases.sort(Comparator.comparingInt(purchase -> this.getPurchasePriority(purchase.item())));
        return (KitCollector.Purchase)purchases.get(0);
    }

    private int getPurchasePriority(AutoBuyItem autoBuyItem) {
        if (CollectorManager.isEquippable(autoBuyItem.getItem())) {
            return 0;
        } else if (autoBuyItem.getSource() == AutoBuyItem.Source.SPHERE) {
            return 1;
        } else {
            return autoBuyItem.getItem().getRegistryEntry().isIn(ItemTags.SWORDS) ? 2 : 3;
        }
    }

    private int getMissingCount(CollectorManager collectorManager, AutoBuyItem autoBuyItem) {
        Integer integer = this.getRequiredItems(collectorManager).get(autoBuyItem);
        return integer == null ? 0 : integer - this.countOwned(autoBuyItem);
    }

    private void recordPurchaseResult() {
        ItemPurchaser itemPurchaser = ItemPurchaser.getInstance();
        ItemPurchaser.Result result = itemPurchaser == null ? null : itemPurchaser.getLastResult();
        if (result != null) {
            if (result.bought()) {
                this.buyFailures.remove(result.item());
            } else {
                this.buyFailures.merge(result.item(), 1, Integer::sum);
            }
        }
    }

    private boolean hasEnoughBundles(Map<AutoBuyItem, Integer> map) {
        for (Entry entry : map.entrySet()) {
            if (((AutoBuyItem)entry.getKey()).getItem() == Items.BUNDLE && this.countOwned((AutoBuyItem)entry.getKey()) < (Integer)entry.getValue()) {
                return false;
            }
        }

        return true;
    }

    private int countOwned(AutoBuyItem autoBuyItem) {
        int i = this.countInInventory(autoBuyItem);
        PlayerInventory playerInventory = mc.player.getInventory();

        for (int i2 = 0; i2 < 36; i2++) {
            ItemStack itemStack = playerInventory.getStack(i2);
            if (!itemStack.isEmpty()) {
                if (AuctionUtil.isSecondTagItem(itemStack)) {
                    i += this.countInBundle(itemStack, autoBuyItem);
                } else {
                    ContainerComponent containerComponent = AuctionUtil.isFirstTagItem(itemStack) ? itemStack.get(DataComponentTypes.CONTAINER) : null;
                    if (containerComponent != null) {
                        for (ItemStack itemStack2 : containerComponent.iterateNonEmpty()) {
                            if (this.matchesItem(autoBuyItem, itemStack2)) {
                                i += itemStack2.getCount();
                            }
                        }
                    }
                }
            }
        }

        return i;
    }

    private int countInInventory(AutoBuyItem autoBuyItem) {
        return InventoryUtil.countMatching(itemStack -> this.matchesItem(autoBuyItem, itemStack));
    }

    private boolean matchesItem(AutoBuyItem autoBuyItem, ItemStack itemStack) {
        if (!autoBuyItem.matches(itemStack)) {
            return false;
        }

        if (autoBuyItem.isCustom()) {
            return true;
        }

        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        return autoBuyManager == null || !autoBuyManager.hasCustomMatch(itemStack, autoBuyItem);
    }

    private boolean handleShulker(CollectorManager collectorManager) {
        if (AuctionUtil.isContainerOpen()) {
            return this.shulkerOpen && this.tickShulker(collectorManager);
        }

        if (this.shulkerOpen && !this.shulkerOpenTimer.isElapsed(1500L)) {
            return true;
        }

        if (this.shulkerOpen) {
            this.failedShulkerOpens++;
        }

        this.shulkerOpen = false;
        if (AuctionUtil.isHandledScreenOpen() || AuctionUtil.isAutoBuyBusy()) {
            return false;
        }

        if (!this.clickTimer.isElapsed(this.clickDelay)) {
            return false;
        }

        Map<AutoBuyItem, Integer> map = this.getRequiredItems(collectorManager);
        if (this.extractFromBundles(collectorManager, map)) {
            return true;
        }

        if (!this.shulkerOpenTimer.isElapsed(5000L)) {
            return false;
        }

        int i = this.findShulkerToOpen(collectorManager, map);
        if (i < 0) {
            return false;
        }

        this.shulkerOpenTimer.reset();
        this.shulkerOpen = true;
        this.openedShulkerSlot = i;
        AuctionUtil.rightClickSlot(i);
        return true;
    }

    private void closeShulker() {
        if (this.openedShulkerSlot >= 0) {
            this.checkedShulkerSlots.add(this.openedShulkerSlot);
        }

        this.openedShulkerSlot = -1;
        this.shulkerOpen = false;
        AuctionUtil.closeScreen();
    }

    private boolean tickShulker(CollectorManager collectorManager) {
        this.failedShulkerOpens = 0;
        if (this.shulkerOpenTimer.isElapsed(10000L)) {
            this.closeShulker();
            return true;
        }

        if (!this.clickTimer.isElapsed(this.clickDelay)) {
            return true;
        }

        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        int i = screenHandler.slots.size();
        int i2 = i - 36;
        Map map = this.getRequiredItems(collectorManager);
        if (this.findEmptySlot(screenHandler, i2, i - 1) >= 0) {
            for (int i3 = 0; i3 < i2; i3++) {
                ItemStack itemStack = screenHandler.getSlot(i3).getStack();
                if (!itemStack.isEmpty() && this.isNeeded(collectorManager, map, itemStack)) {
                    this.scheduleClick();
                    InventoryUtil.clickSlot(i3, 0, SlotActionType.QUICK_MOVE);
                    return true;
                }
            }
        }

        if (this.depositExcess(collectorManager, map, screenHandler, i2)) {
            return true;
        }

        this.closeShulker();
        return true;
    }

    private boolean depositExcess(CollectorManager collectorManager, Map<AutoBuyItem, Integer> map, ScreenHandler screenHandler, int i) {
        int i2 = this.findEmptySlot(screenHandler, 0, i - 1);
        if (i2 < 0) {
            return false;
        }

        int i3 = screenHandler.slots.size();

        for (int i4 = i; i4 < i3; i4++) {
            ItemStack itemStack = screenHandler.getSlot(i4).getStack();
            if (!itemStack.isEmpty() && !AuctionUtil.isFirstTagItem(itemStack)) {
                AutoBuyItem autoBuyItem = this.findMatchingItem(map, itemStack);
                if (autoBuyItem != null) {
                    int i5 = this.countInInventory(autoBuyItem) + this.countInBundles(collectorManager, autoBuyItem) - (Integer)map.get(autoBuyItem);
                    if (i5 > 0) {
                        int i6 = Math.min(i5, itemStack.getCount());
                        int i7 = itemStack.getCount() - i6;
                        if (i7 == 0) {
                            this.scheduleClick();
                            InventoryUtil.clickSlot(i4, 0, SlotActionType.QUICK_MOVE);
                            return true;
                        }

                        if (Math.min(i7, i6) <= 16) {
                            this.scheduleClick();
                            InventoryUtil.clickSlot(i4, 0, SlotActionType.PICKUP);
                            if (i7 <= i6) {
                                for (int i8 = 0; i8 < i7; i8++) {
                                    InventoryUtil.clickSlot(i4, 1, SlotActionType.PICKUP);
                                }

                                InventoryUtil.clickSlot(i2, 0, SlotActionType.PICKUP);
                                return true;
                            }

                            for (int i9 = 0; i9 < i6; i9++) {
                                InventoryUtil.clickSlot(i2, 1, SlotActionType.PICKUP);
                            }

                            InventoryUtil.clickSlot(i4, 0, SlotActionType.PICKUP);
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean extractFromBundles(CollectorManager collectorManager, Map<AutoBuyItem, Integer> map) {
        PlayerInventory playerInventory = mc.player.getInventory();

        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = playerInventory.getStack(i);
            if (AuctionUtil.isSecondTagItem(itemStack)) {
                BundleContentsComponent bundleContentsComponent = itemStack.get(DataComponentTypes.BUNDLE_CONTENTS);
                if (bundleContentsComponent != null) {
                    CollectorManager.CollectorSlot collectorSlot = collectorManager.getSlot(this.inventoryToLayoutIndex(i));
                    int i2 = bundleContentsComponent.size();

                    for (int i3 = 0; i3 < i2; i3++) {
                        ItemStack itemStack2 = bundleContentsComponent.get(i3);
                        if (!itemStack2.isEmpty() && this.isNeeded(collectorManager, map, itemStack2)) {
                            AutoBuyItem autoBuyItem = this.findMatchingItem(map, itemStack2);
                            int i4 = collectorSlot.isBundle() ? this.countBundleRequirement(collectorSlot, autoBuyItem) : 0;
                            if (itemStack2.getCount() <= this.countInBundle(itemStack, autoBuyItem) - i4) {
                                int i5 = this.findEmptySlot(mc.player.currentScreenHandler, 9, 44);
                                if (i5 < 0) {
                                    return false;
                                }

                                this.scheduleClick();
                                if (i < 9) {
                                    this.extractFromBundle(i + 36, i5, i3);
                                    return true;
                                }

                                this.extractFromBundle(i, i5, i3);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private void extractFromBundle(int i, int i2, int i3) {
        BundleItem.setSelectedStackIndex(this.getContainerStack(i), i3);
        mc.player.networkHandler.sendPacket(new BundleItemSelectedC2SPacket(i, i3));
        InventoryUtil.clickSlot(i, 1, SlotActionType.PICKUP);
        InventoryUtil.clickSlot(i2, 0, SlotActionType.PICKUP);
    }

    private int findShulkerToOpen(CollectorManager collectorManager, Map<AutoBuyItem, Integer> map) {
        if (this.failedShulkerOpens >= 3) {
            return -1;
        }

        boolean flag = this.hasExcessItems(collectorManager, map);
        boolean flag2 = this.hasMissingItems(map);
        ScreenHandler screenHandler = mc.player.currentScreenHandler;

        for (int i = 9; i <= 44; i++) {
            ItemStack itemStack = screenHandler.getSlot(i).getStack();
            if (AuctionUtil.isFirstTagItem(itemStack)) {
                ContainerComponent containerComponent = itemStack.get(DataComponentTypes.CONTAINER);
                int i2 = 0;
                if (containerComponent != null) {
                    for (ItemStack itemStack2 : containerComponent.iterateNonEmpty()) {
                        i2++;
                        if (this.isNeeded(collectorManager, map, itemStack2)) {
                            return i;
                        }
                    }
                }

                if (flag && i2 < 27) {
                    return i;
                }

                if (flag2 && !this.checkedShulkerSlots.contains(i)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private boolean hasMissingItems(Map<AutoBuyItem, Integer> map) {
        for (Entry entry : map.entrySet()) {
            if ((Integer)entry.getValue() > this.countOwned((AutoBuyItem)entry.getKey())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasExcessItems(CollectorManager collectorManager, Map<AutoBuyItem, Integer> map) {
        for (Entry entry : map.entrySet()) {
            AutoBuyItem autoBuyItem = (AutoBuyItem)entry.getKey();
            if (this.countInInventory(autoBuyItem) + this.countInBundles(collectorManager, autoBuyItem) > (Integer)entry.getValue()) {
                return true;
            }
        }

        return false;
    }

    private boolean isNeeded(CollectorManager collectorManager, Map<AutoBuyItem, Integer> map, ItemStack itemStack) {
        AutoBuyItem autoBuyItem = this.findMatchingItem(map, itemStack);
        return autoBuyItem != null && (Integer)map.get(autoBuyItem) > this.countInInventory(autoBuyItem) + this.countInBundles(collectorManager, autoBuyItem);
    }

    private AutoBuyItem findMatchingItem(Map<AutoBuyItem, Integer> map, ItemStack itemStack) {
        for (AutoBuyItem autoBuyItem : map.keySet()) {
            if (this.matchesItem(autoBuyItem, itemStack)) {
                return autoBuyItem;
            }
        }

        return null;
    }

    private int countInBundles(CollectorManager collectorManager, AutoBuyItem autoBuyItem) {
        int i = 0;

        for (int i2 = 0; i2 < 41; i2++) {
            CollectorManager.CollectorSlot collectorSlot = collectorManager.getSlot(i2);
            int i3 = collectorSlot.isBundle() ? this.layoutToInventoryIndex(i2) : -1;
            if (i3 >= 0) {
                int i4 = this.countBundleRequirement(collectorSlot, autoBuyItem);
                if (i4 != 0) {
                    i += Math.min(i4, this.countInBundle(mc.player.getInventory().getStack(i3), autoBuyItem));
                }
            }
        }

        return i;
    }

    private int countBundleRequirement(CollectorManager.CollectorSlot collectorSlot, AutoBuyItem autoBuyItem) {
        int i = 0;

        for (CollectorManager.BundleEntry bundleEntry : collectorSlot.getBundleEntries()) {
            if (bundleEntry.getItem() == autoBuyItem) {
                i += bundleEntry.getCount();
            }
        }

        return i;
    }

    private int findEmptySlot(ScreenHandler screenHandler, int i, int i2) {
        for (int i3 = i; i3 <= i2; i3++) {
            if (screenHandler.getSlot(i3).getStack().isEmpty()) {
                return i3;
            }
        }

        return -1;
    }

    private void scheduleClick() {
        this.clickTimer.reset();
        this.clickDelay = ThreadLocalRandom.current().nextInt(140, 321);
    }

    private boolean arrangeSlot(CollectorManager collectorManager) {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        if (!screenHandler.getCursorStack().isEmpty()) {
            int i = this.findEmptySlot(screenHandler, 9, 44);
            if (i >= 0 && !this.isRecentMove(i, i)) {
                InventoryUtil.clickSlot(i, 0, SlotActionType.PICKUP);
                this.recordMove(i, i);
                return true;
            }
        } else {
            Set set = this.getSatisfiedSlots(collectorManager);

            for (int i2 = 0; i2 < 41; i2++) {
                CollectorManager.CollectorSlot collectorSlot = collectorManager.getSlot(i2);
                if (!collectorSlot.isEmpty()) {
                    int i3 = this.layoutToContainerSlot(i2);
                    if (!set.contains(i3)) {
                        ItemStack itemStack = this.getContainerStack(i3);
                        boolean flag = this.matchesItem(collectorSlot.getItem(), itemStack);
                        int i4 = flag ? this.findMergeableStack(itemStack, set, i3) : this.findItemSlot(collectorSlot.getItem(), set, i3);
                        if (i4 >= 0 && !this.isRecentMove(i4, i3)) {
                            if (flag) {
                                this.mergeStacks(i4, i3);
                                this.recordMove(i4, i3);
                                return true;
                            }

                            this.swapIntoSlot(i4, i3, i2);
                            this.recordMove(i4, i3);
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void mergeStacks(int i, int i2) {
        InventoryUtil.clickSlot(i, 0, SlotActionType.PICKUP);
        InventoryUtil.clickSlot(i2, 0, SlotActionType.PICKUP);
        InventoryUtil.clickSlot(i, 0, SlotActionType.PICKUP);
    }

    private boolean fillBundles(CollectorManager collectorManager) {
        Set set = this.getSatisfiedSlots(collectorManager);

        for (int i = 0; i < 41; i++) {
            CollectorManager.CollectorSlot collectorSlot = collectorManager.getSlot(i);
            if (collectorSlot.isBundle() && !collectorSlot.getBundleEntries().isEmpty()) {
                int i2 = this.layoutToContainerSlot(i);
                ItemStack itemStack = this.getContainerStack(i2);
                if (AuctionUtil.isSecondTagItem(itemStack)) {
                    LinkedHashMap<AutoBuyItem, Integer> linkedHashMap = new LinkedHashMap<>();

                    for (CollectorManager.BundleEntry bundleEntry : collectorSlot.getBundleEntries()) {
                        linkedHashMap.merge(bundleEntry.getItem(), bundleEntry.getCount(), Integer::sum);
                    }

                    for (Entry<AutoBuyItem, Integer> entry : linkedHashMap.entrySet()) {
                        if (this.countInBundle(itemStack, (AutoBuyItem)entry.getKey()) < (Integer)entry.getValue()) {
                            int i3 = this.findItemSlot((AutoBuyItem)entry.getKey(), set, i2);
                            if (i3 >= 0 && !this.isRecentMove(i3, i2)) {
                                InventoryUtil.clickSlot(i3, 0, SlotActionType.PICKUP);
                                InventoryUtil.clickSlot(i2, 0, SlotActionType.PICKUP);
                                this.recordMove(i3, i2);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private int countInBundle(ItemStack itemStack, AutoBuyItem autoBuyItem) {
        BundleContentsComponent bundleContentsComponent = itemStack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundleContentsComponent == null) {
            return 0;
        }

        int i = 0;

        for (ItemStack itemStack2 : bundleContentsComponent.iterate()) {
            if (this.matchesItem(autoBuyItem, itemStack2)) {
                i += itemStack2.getCount();
            }
        }

        return i;
    }

    private int layoutToContainerSlot(int i) {
        if (i == 40) {
            return 45;
        } else if (i >= 36) {
            return 5 + (i - 36);
        } else {
            return i >= 27 ? 36 + (i - 27) : 9 + i;
        }
    }

    private int layoutToInventoryIndex(int i) {
        if (i >= 36) {
            return -1;
        } else {
            return i >= 27 ? i - 27 : i + 9;
        }
    }

    private int inventoryToLayoutIndex(int i) {
        return i < 9 ? 27 + i : i - 9;
    }

    private Set<Integer> getSatisfiedSlots(CollectorManager collectorManager) {
        HashSet hashSet = new HashSet();

        for (int i = 0; i < 41; i++) {
            CollectorManager.CollectorSlot collectorSlot = collectorManager.getSlot(i);
            if (!collectorSlot.isEmpty()) {
                int i2 = this.layoutToContainerSlot(i);
                ItemStack itemStack = this.getContainerStack(i2);
                if (this.matchesItem(collectorSlot.getItem(), itemStack) && (itemStack.getCount() >= collectorSlot.getCount() || this.findMergeableStack(itemStack, hashSet, i2) < 0)) {
                    hashSet.add(i2);
                }
            }
        }

        return hashSet;
    }

    private int findMergeableStack(ItemStack itemStack, Set<Integer> integers, int i) {
        if (itemStack.getCount() >= itemStack.getMaxCount()) {
            return -1;
        }

        ScreenHandler screenHandler = mc.player.currentScreenHandler;

        for (int i2 = 5; i2 <= 45; i2++) {
            if (i2 != i && !integers.contains(i2) && ItemStack.areItemsAndComponentsEqual(screenHandler.getSlot(i2).getStack(), itemStack)) {
                return i2;
            }
        }

        return -1;
    }

    private int findItemSlot(AutoBuyItem autoBuyItem, Set<Integer> integers, int i) {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;

        for (int i2 = 5; i2 <= 45; i2++) {
            if (i2 != i && !integers.contains(i2)) {
                ItemStack itemStack = screenHandler.getSlot(i2).getStack();
                if (!itemStack.isEmpty() && this.matchesItem(autoBuyItem, itemStack)) {
                    return i2;
                }
            }
        }

        return -1;
    }

    private ItemStack getContainerStack(int i) {
        return mc.player.currentScreenHandler.getSlot(i).getStack();
    }

    private boolean isRecentMove(int i, int i2) {
        return i == this.lastFromSlot && i2 == this.lastToSlot && !this.lastMoveTimer.isElapsed(1000L);
    }

    private void recordMove(int i, int i2) {
        this.lastFromSlot = i;
        this.lastToSlot = i2;
        this.lastMoveTimer.reset();
    }

    private void swapIntoSlot(int i, int i2, int i3) {
        if (i3 == 40) {
            InventoryUtil.clickSlot(i, 40, SlotActionType.SWAP);
        } else if (i3 >= 27 && i3 < 36) {
            InventoryUtil.clickSlot(i, i3 - 27, SlotActionType.SWAP);
        } else if (i >= 36 && i <= 44) {
            InventoryUtil.clickSlot(i2, i - 36, SlotActionType.SWAP);
        } else {
            InventoryUtil.clickSlot(i, 0, SlotActionType.SWAP);
            InventoryUtil.clickSlot(i2, 0, SlotActionType.SWAP);
            InventoryUtil.clickSlot(i, 0, SlotActionType.SWAP);
        }
    }

    public StopWatch getBuyTimer() {
        return this.buyTimer;
    }

    public StopWatch getSettleTimer() {
        return this.settleTimer;
    }

    public StopWatch getActionTimer() {
        return this.actionTimer;
    }

    public StopWatch getShulkerOpenTimer() {
        return this.shulkerOpenTimer;
    }

    public StopWatch getClickTimer() {
        return this.clickTimer;
    }

    public long getClickDelay() {
        return this.clickDelay;
    }

    public boolean isShulkerOpen() {
        return this.shulkerOpen;
    }

    public int getOpenedShulkerSlot() {
        return this.openedShulkerSlot;
    }

    public Set<Integer> getCheckedShulkerSlots() {
        return this.checkedShulkerSlots;
    }

    public int getFailedShulkerOpens() {
        return this.failedShulkerOpens;
    }

    public boolean isInventoryOpened() {
        return this.inventoryOpened;
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean isBuying() {
        return this.buying;
    }

    public boolean isSettling() {
        return this.settling;
    }

    public int getLastFromSlot() {
        return this.lastFromSlot;
    }

    public int getLastToSlot() {
        return this.lastToSlot;
    }

    public StopWatch getLastMoveTimer() {
        return this.lastMoveTimer;
    }

    public Map<AutoBuyItem, Integer> getBuyFailures() {
        return this.buyFailures;
    }

    public boolean isPartial() {
        return this.partial;
    }

    record Purchase(AutoBuyItem item, int need) {
    }
}
