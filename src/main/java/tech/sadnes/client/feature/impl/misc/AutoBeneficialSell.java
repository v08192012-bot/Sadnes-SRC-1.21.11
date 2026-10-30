package tech.sadnes.client.feature.impl.misc;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.sadnes.api.mixin.accessor.PlayerListHudAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.CloseScreenEvent;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.MenuTickEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.SlotClickEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.feature.component.MovementPauseComponent;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.TradeBook;
import tech.sadnes.client.server.donut.DonutPriceUtil;
import tech.sadnes.client.server.donut.OrderDeliveryHelper;
import tech.sadnes.client.server.donut.QuickAction;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.chat.ChatMessageUtil;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.network.AutoReconnector;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "AutoBeneficialSell",
    description = "feature.impl.misc.autobeneficialsell",
    displayName = "feature.impl.misc.autobeneficialsell",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SERVER
)
public final class AutoBeneficialSell extends Feature {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBeneficialSell.class);
    private static final List<AutoBeneficialSell.TradeGood> GOODS = List.of(
        new AutoBeneficialSell.TradeGood("elytra", Items.ELYTRA, "elytra"),
        new AutoBeneficialSell.TradeGood("anchor", Items.RESPAWN_ANCHOR, "anchor"),
        new AutoBeneficialSell.TradeGood("crystal", Items.END_CRYSTAL, "crystal")
    );
    private static final Pattern PURCHASE_PATTERN = Pattern.compile(
        "^(?:\\p{L}+\\s+){1,4}(\\d{1,4})\\s+(.+?)\\s+\\p{L}+\\s+\\$\\s*[0-9][0-9.,]*\\s*[KMBT]?\\.?$", 2
    );
    private static final Pattern MONEY_GAIN_PATTERN = Pattern.compile("^\\+?\\s*\\$\\s*[0-9][0-9.,]*\\s*[KMBT]?$", 2);
    private final MultiSetting items = new MultiSetting(
        "autobeneficialsell.items", BooleanSetting.of("elytra", true), BooleanSetting.of("anchor", true), BooleanSetting.of("crystal", true)
    );
    private final NumberSetting discount = new NumberSetting("autobeneficialsell.discount", 1.1F, 0.3F, 20.0F, 0.1F, Unit.PERCENT);
    private final NumberSetting profit = new NumberSetting("autobeneficialsell.profit", 1.0F, 0.0F, 20.0F, 0.1F, Unit.PERCENT);
    private final NumberSetting period = new NumberSetting("autobeneficialsell.period", 1.0F, 0.5F, 12.0F, 0.5F, Unit.HOUR);
    private final NumberSetting interval = new NumberSetting("autobeneficialsell.interval", 20.0F, 5.0F, 120.0F, 5.0F, Unit.SECOND);
    private final NumberSetting lots = new NumberSetting("autobeneficialsell.lots", 1.0F, 1.0F, 5.0F, 1.0F);
    private final BooleanSetting notifyOnly = new BooleanSetting("autobeneficialsell.notifyOnly", false);
    private final GroupSetting rejoin = new GroupSetting("autobeneficialsell.rejoin", true);
    private final NumberSetting rejoinAfter = new NumberSetting(this.rejoin, "autobeneficialsell.rejoinAfter", 45.0F, 30.0F, 180.0F, 5.0F, Unit.SECOND);
    private final TradeBook priceTracker = new TradeBook();
    private final AutoReconnector rejoinHelper = new AutoReconnector();
    private final Map<Item, AutoBeneficialSell.ShopOffer> bestOffers = new HashMap<>();
    private final Map<Item, Long> averagePrices = new HashMap<>();
    private final Set<Item> checkedItems = new HashSet<>();
    private final Map<Item, Long> buyCooldowns = new HashMap<>();
    private final Map<Item, Long> sellCooldowns = new HashMap<>();
    private final Map<Item, Long> skipNotifyTimes = new HashMap<>();
    private final Map<Item, Integer> lastInventoryCounts = new HashMap<>();
    private final Set<String> shownMessages = new HashSet<>();
    private final StopWatch stateTimer = new StopWatch();
    private final StopWatch clickTimer = new StopWatch();
    private final StopWatch combatTimer = new StopWatch();
    private final StopWatch dangerTimer = new StopWatch();
    private final StopWatch inventorySyncTimer = new StopWatch();
    private AutoBeneficialSell.State state = AutoBeneficialSell.State.IDLE;
    private GenericContainerScreenHandler container;
    private OrderDeliveryHelper sellWindow;
    private boolean selfClicking;
    private boolean sellMode;
    private long nextRunTime;
    private int openFailures;
    private long manualWindowUntil;
    private long silentSince;
    private long lastRejoinTime;
    private boolean serverReplied;
    private final Set<QuickAction> pendingCommands = EnumSet.noneOf(QuickAction.class);
    private AutoBeneficialSell.TradeGood buyGood;
    private AutoBeneficialSell.ShopOffer buyOffer;
    private AutoBeneficialSell.Purchase purchase;
    private AutoBeneficialSell.TradeGood sellGood;
    private TradeBook.Lot sellLot;
    private int sellCount;
    private int countBefore;
    private long quotedSum;
    private long receivedSum;
    private boolean quoteSeen;
    private int returnAttempts;
    private boolean halted;

    @Override
    public void onDisable() {
        super.onDisable();
        this.state = AutoBeneficialSell.State.IDLE;
        this.nextRunTime = 0L;
        this.openFailures = 0;
        this.manualWindowUntil = 0L;
        this.silentSince = 0L;
        this.pendingCommands.clear();
        this.lastRejoinTime = 0L;
        this.halted = false;
        this.buyCooldowns.clear();
        this.sellCooldowns.clear();
        this.skipNotifyTimes.clear();
        this.lastInventoryCounts.clear();
        this.shownMessages.clear();
        this.inventorySyncTimer.reset();
    }

    @Override
    public void onEnable() {
        boolean flag = this.container != null && mc.player != null && mc.player.currentScreenHandler == this.container;
        if (flag && this.isInSellWindow()) {
            this.sellWindow.takeBack(this.returnAttempts);
        } else if (flag) {
            this.closeContainer();
        }

        if (this.state == AutoBeneficialSell.State.SELLING && mc.player != null) {
            this.finishSale();
        } else if (this.state != AutoBeneficialSell.State.IDLE) {
            this.reset(0L);
        }

        this.rejoinHelper.cancel();
        this.priceTracker.save();
        super.onEnable();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        if (this.state != AutoBeneficialSell.State.IDLE) {
            this.reset(0L);
        }

        this.lastInventoryCounts.clear();
        this.inventorySyncTimer.reset();
        String string = this.rejoinHelper.consumeReason();
        if (string != null) {
            ChatUtil.print(string);
        }
    }

    @EventHandler
    public void onDisconnect(MenuTickEvent menuTickEvent) {
        this.rejoinHelper.tick();
    }

    @EventHandler
    public void onUpdate(WorldTickEvent worldTickEvent) {
        if (!ServerManager.isNullWorld() && ServerManager.isDonut()) {
            if (InventoryUtil.isMovementActive()) {
                this.combatTimer.reset();
            }

            if (this.isInDanger()) {
                this.dangerTimer.reset();
            }

            switch (this.state) {
                case IDLE:
                    this.tickIdle();
                    this.priceTracker.tick();
                    return;
                case STOPPING:
                    this.tickStopping();
                    this.priceTracker.tick();
                    return;
                case OPENING:
                    this.tickOpening();
                    this.priceTracker.tick();
                    return;
                case SHOP:
                    this.tickShop();
                    this.priceTracker.tick();
                    return;
                case BUYING:
                    this.tickBuying();
                    this.priceTracker.tick();
                    return;
                case DEPOSITING:
                    this.tickDepositing();
                    this.priceTracker.tick();
                    return;
                case QUOTING:
                    this.tickQuoting();
                    this.priceTracker.tick();
                    return;
                case RETURNING:
                    this.tickReturning();
                    this.priceTracker.tick();
                    return;
                case SELLING:
                    this.tickSelling();
                default:
                    this.priceTracker.tick();
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        Packet packet = packetEvent.getPacket();
        if (packetEvent.isSend()) {
            boolean flag = this.state == AutoBeneficialSell.State.IDLE
                && packet instanceof CommandExecutionC2SPacket
                && QuickAction.byId(((CommandExecutionC2SPacket)packet).command()) != null;
            if (flag) {
                this.manualWindowUntil = 0L;
            }

            boolean flag2;
            label56: {
                boolean flag3;
                if (!(packet instanceof CloseHandledScreenC2SPacket) && !(packet instanceof ClickSlotC2SPacket)) {
                    if (packet instanceof PlayerInteractEntityC2SPacket) {
                        flag2 = this.isInSellWindow();
                        break label56;
                    }

                    flag3 = false;
                } else {
                    flag3 = true;
                }

                flag2 = flag3;
            }

            if (flag2 && this.isContainerOpen() && !this.selfClicking) {
                packetEvent.cancel();
            }
        } else if (packetEvent.isReceive() && packet instanceof GameMessageS2CPacket gameMessageS2CPacket && !gameMessageS2CPacket.overlay()) {
            String string = ChatMessageUtil.getPlainText(gameMessageS2CPacket);
            mc.execute(() -> this.handleChatMessage(string));
            return;
        }
    }

    @EventHandler
    public void onCloseScreen(SlotClickEvent slotClickEvent) {
        if (this.isContainerOpen() && !this.selfClicking && slotClickEvent.getSyncId() == this.container.syncId) {
            slotClickEvent.cancel();
        }
    }

    @EventHandler
    public void onServerCloseScreen(CloseScreenEvent closeScreenEvent) {
        if (this.isContainerOpen() && !this.selfClicking) {
            if (!this.isInSellWindow()) {
                this.container = null;
            } else {
                closeScreenEvent.cancel();
                if (this.state == AutoBeneficialSell.State.QUOTING) {
                    this.setState(AutoBeneficialSell.State.RETURNING);
                }
            }
        }
    }

    private void handleChatMessage(String string) {
        if (this.state == AutoBeneficialSell.State.OPENING && ChatMessageUtil.isErrorMessage(string)) {
            this.serverReplied = true;
        }

        boolean flag = this.isInSellWindow() || this.state == AutoBeneficialSell.State.SELLING;
        if (flag && this.receivedSum < 0L && MONEY_GAIN_PATTERN.matcher(string).matches()) {
            this.receivedSum = DonutPriceUtil.parsePrice(string);
        }

        if (this.state == AutoBeneficialSell.State.BUYING && this.purchase == null) {
            Matcher matcher2 = PURCHASE_PATTERN.matcher(string);
            if (matcher2.matches()) {
                int i = Integer.parseInt(matcher2.group(1));
                long l = DonutPriceUtil.parsePrice(string);
                boolean flag2 = matcher2.group(2).toLowerCase(Locale.ROOT).contains(this.buyGood.chatName());
                if (i == this.buyOffer.count() && Math.abs(l - this.buyOffer.price()) <= this.buyOffer.price() / 4L || flag2) {
                    this.purchase = new AutoBeneficialSell.Purchase(i, l);
                }
            }
        }
    }

    private void tickIdle() {
        if (!this.handleManualWindow()) {
            this.syncInventoryChanges();
            if (this.canStart()) {
                this.sellMode = false;
                MovementPauseComponent.pause(this);
                this.setState(AutoBeneficialSell.State.STOPPING);
            }
        }
    }

    private boolean handleManualWindow() {
        if (System.currentTimeMillis() < this.manualWindowUntil
            && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler
            && this.isServerWindow()) {
            this.container = (GenericContainerScreenHandler)mc.player.currentScreenHandler;
            boolean flag;
            if (!this.sellMode) {
                flag = this.scanShop();
            } else {
                flag = false;
            }

            boolean flag2;
            label42: {
                if (this.sellMode && OrderDeliveryHelper.isOrderScreen(this.container)) {
                    flag2 = true;
                } else {
                    flag2 = false;
                    if (!flag) {
                        break label42;
                    }
                }

                this.onWindowOpened();
            }

            if (flag) {
                MovementPauseComponent.pause(this);
                this.setState(AutoBeneficialSell.State.SHOP);
                return true;
            }

            if (flag2) {
                this.closeContainer();
                this.sellMode = false;
            }

            this.container = null;
            return flag2;
        } else {
            return false;
        }
    }

    private void onWindowOpened() {
        if (this.silentSince != 0L) {
            this.manualWindowUntil = System.currentTimeMillis() + 15000L;
        }

        this.silentSince = 0L;
    }

    private void syncInventoryChanges() {
        if (this.inventorySyncTimer.hasPassed(5000.0)) {
            ItemStack itemStack = mc.player.currentScreenHandler.getCursorStack();

            for (AutoBeneficialSell.TradeGood tradeGood : GOODS) {
                int i = this.countItem(tradeGood.item()) + (TradeBook.isPlainItem(itemStack, tradeGood.item()) ? itemStack.getCount() : 0);
                int i2 = this.priceTracker.getOwnedCount(tradeGood.item());
                Integer integer = this.lastInventoryCounts.put(tradeGood.item(), i);
                int i3 = integer == null ? i2 - i : integer - i;
                if (i3 > 0 && i2 > 0) {
                    this.priceTracker.consumeLots(tradeGood.item(), i3);
                }
            }
        }
    }

    private boolean canStart() {
        boolean flag;
        if (this.isScreenFree()
            && mc.player.currentScreenHandler == mc.player.playerScreenHandler
            && mc.player.currentScreenHandler.getCursorStack().isEmpty()
            && mc.player.isOnGround()
            && !mc.player.isUsingItem()
            && !mc.player.isSneaking()
            && MovementPauseComponent.hasPendingTasks()
            && !MovementPauseComponent.isPaused()
            && !GuiMoveComponent.isActive()) {
            flag = this.isOtherFeatureBusy();
        } else {
            flag = true;
        }

        boolean flag2;
        if (this.combatTimer.hasPassed(1000.0)) {
            flag2 = this.dangerTimer.hasPassed(3000.0);
        } else {
            flag2 = false;
        }

        return !flag && flag2 && this.items.hasAnySelected() && System.currentTimeMillis() >= this.nextRunTime;
    }

    private boolean isScreenFree() {
        return mc.currentScreen == null || mc.currentScreen instanceof GameMenuScreen && !mc.isWindowFocused();
    }

    private boolean isServerWindow() {
        if (mc.currentScreen == null) {
            return false;
        }

        Text text = mc.currentScreen.getTitle();
        return text != null
                && text.getContent() instanceof TranslatableTextContent translatableTextContent
                && (translatableTextContent.getKey().startsWith("container.") || translatableTextContent.getKey().startsWith("entity."))
            ? false
            : text != null;
    }

    private boolean isOtherFeatureBusy() {
        AutoLeave autoLeave = Sadnes.getInstance().getFeatureManager().getFeature(AutoLeave.class);
        return autoLeave != null && autoLeave.shouldLeaveHost();
    }

    private boolean isInDanger() {
        AttackAura attackAura = AttackAura.getInstance();
        return attackAura != null && attackAura.isEnabled() && attackAura.getTarget() != null ? true : mc.player.hurtTime > 0 || this.isOtherFeatureBusy();
    }

    private void tickStopping() {
        if (this.isScreenFree() && !this.isOtherFeatureBusy()) {
            RotationComponent rotationComponent = RotationComponent.getInstance();
            if (InventoryUtil.isMovementActive() || rotationComponent != null && rotationComponent.isActive()) {
                if (this.stateTimer.hasPassed(2500.0)) {
                    this.reset(3000L);
                }
            } else if (!this.sellMode || this.stateTimer.hasPassed(1200.0)) {
                this.serverReplied = false;
                QuickAction quickAction = this.currentCommand();
                if (this.pendingCommands.remove(quickAction) && quickAction.run()) {
                    this.setState(AutoBeneficialSell.State.OPENING);
                } else {
                    mc.player.networkHandler.sendChatCommand(quickAction.getId());
                    this.pendingCommands.add(quickAction);
                    this.setState(AutoBeneficialSell.State.OPENING);
                }
            }
        } else {
            this.reset(0L);
        }
    }

    private QuickAction currentCommand() {
        return this.sellMode ? QuickAction.SELL : QuickAction.SHOP;
    }

    private void tickOpening() {
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler && this.isServerWindow()) {
            this.container = genericContainerScreenHandler;
            this.pendingCommands.remove(this.currentCommand());
            this.onWindowOpened();
            if (this.sellMode) {
                this.setState(AutoBeneficialSell.State.DEPOSITING);
            } else {
                this.setState(AutoBeneficialSell.State.SHOP);
            }
        } else if (this.stateTimer.hasPassed((double)Math.max(1000, ServerManager.getPing() * 3))) {
            long l = System.currentTimeMillis();
            this.manualWindowUntil = l + 15000L;
            if (this.serverReplied) {
                this.pendingCommands.remove(this.currentCommand());
                this.silentSince = 0L;
                this.handleFailure("autobeneficialsell.notify.noOpen", "Не удалось открыть окно сервера — повторю позже");
            } else {
                if (this.silentSince == 0L) {
                    this.silentSince = l;
                }

                this.openFailures++;
                long l2 = (l - this.silentSince) / 1000L;
                long l3 = (long)(this.rejoinAfter.getValue() * 1000.0F);
                boolean flag;
                if (this.rejoin.isEnabled() && this.lastRejoinTime < this.silentSince && this.openFailures >= 3 && l - this.silentSince >= l3) {
                    flag = this.dangerTimer.hasPassed((double)l3);
                } else {
                    flag = false;
                }

                LOGGER.warn("AutoBeneficialSell: /{} got no window and no reply, silent for {} s", this.sellMode ? "sell" : "shop", l2);
                this.reset(5000L);
                if (!flag) {
                    this.manualWindowUntil = this.nextRunTime + 15000L;
                    if (this.openFailures == 3) {
                        this.sendNotification(NotificationType.WARNING, "autobeneficialsell.notify.noOpen", "Не удалось открыть окно сервера — повторю позже");
                    }
                } else {
                    this.lastRejoinTime = l;
                    this.openFailures = 0;
                    this.rejoinHelper
                        .disconnect(
                            this.getDisplayName()
                                + ": "
                                + String.format(
                                    Language.translate("autobeneficialsell.notify.rejoin", "Окно сервера не открывалось %d с — перезаход на сервер"), l2
                                )
                        );
                }
            }
        }
    }

    private void handleFailure(String string, String string2) {
        this.openFailures++;
        if (this.openFailures < 3) {
            this.reset(5000L);
        } else {
            this.openFailures = 0;
            this.sendNotification(NotificationType.WARNING, string, string2);
            this.reset(300000L);
        }
    }

    private void tickShop() {
        if (mc.player.currentScreenHandler != this.container) {
            this.reset(0L);
        } else if (this.isInDanger()) {
            this.closeContainer();
            this.reset(0L);
        } else if (!this.scanShop()) {
            if (this.stateTimer.hasPassed((double)(ServerManager.getPing() + 600))) {
                this.dumpWindow("no priced goods in /shop");
                this.closeContainer();
                this.handleFailure("autobeneficialsell.notify.noGoods", "В /shop не видно выбранных предметов с ценой");
            }
        } else {
            this.openFailures = 0;
            this.updateAveragePrices();
            if (this.notifyOnly.getValue() && this.shownMessages.add("hintsOnly")) {
                this.notifyAndPrint(
                    NotificationType.WARNING, "autobeneficialsell.notify.hintsOnly", "Включено «Только подсказки»: модуль не покупает и не продаёт"
                );
            }

            AutoBeneficialSell.TradeGood tradeGood = this.pickGoodToBuy();
            if (tradeGood == null) {
                this.closeContainer();
                this.startSellingOrFinish();
            } else {
                this.buyGood = tradeGood;
                this.buyOffer = this.bestOffers.get(tradeGood.item());
                this.countBefore = this.countItem(tradeGood.item());
                this.purchase = null;
                this.performSlotClick(this.buyOffer.slot(), 0, SlotActionType.PICKUP);
                this.setState(AutoBeneficialSell.State.BUYING);
            }
        }
    }

    private boolean scanShop() {
        this.bestOffers.clear();
        int i = this.container.getRows() * 9;

        for (int i2 = 0; i2 < i; i2++) {
            ItemStack itemStack = this.container.getSlot(i2).getStack();
            AutoBeneficialSell.TradeGood tradeGood = this.findGood(itemStack);
            long l = tradeGood == null ? -1L : DonutPriceUtil.getItemPrice(itemStack);
            if (l > 0L) {
                AutoBeneficialSell.ShopOffer shopOffer = new AutoBeneficialSell.ShopOffer(i2, itemStack.getCount(), l);
                AutoBeneficialSell.ShopOffer shopOffer2 = this.bestOffers.get(tradeGood.item());
                if (shopOffer2 == null || shopOffer.unit() < shopOffer2.unit()) {
                    this.bestOffers.put(tradeGood.item(), shopOffer);
                }
            }
        }

        return !this.bestOffers.isEmpty();
    }

    private AutoBeneficialSell.TradeGood findGood(ItemStack itemStack) {
        for (AutoBeneficialSell.TradeGood tradeGood : GOODS) {
            if (itemStack.isOf(tradeGood.item()) && this.items.isSelected(tradeGood.option())) {
                return tradeGood;
            }
        }

        return null;
    }

    private void updateAveragePrices() {
        long l = (long)(this.period.getValue() * 3600000.0F);

        for (Entry entry : this.bestOffers.entrySet()) {
            Item item = (Item)entry.getKey();
            if (!this.averagePrices.containsKey(item)) {
                long l2 = this.priceTracker.getAveragePrice(item, l);
                this.averagePrices.put(item, l2);
                this.priceTracker.recordPrice(item, Math.round(((AutoBeneficialSell.ShopOffer)entry.getValue()).unit()));
                this.reportPriceStatus(item, (AutoBeneficialSell.ShopOffer)entry.getValue(), l2);
            }
        }
    }

    private void reportPriceStatus(Item item, AutoBeneficialSell.ShopOffer shopOffer, long l) {
        if (l <= 0L) {
            if (this.shownMessages.add("warmup")) {
                this.sendNotification(NotificationType.INFO, "autobeneficialsell.notify.warmup", "Коплю историю цен: это займёт около пяти минут");
            }
        } else if (this.shownMessages.add("status:" + item + this.discount.getValue())) {
            this.printChat(
                "autobeneficialsell.status.waiting",
                "%s ×%d: сейчас %s, средняя %s, жду цену не выше %s",
                item.getName().getString(),
                shopOffer.count(),
                DonutPriceUtil.formatPrice(shopOffer.price()),
                DonutPriceUtil.formatPrice(l * shopOffer.count()),
                DonutPriceUtil.formatPrice(this.getTargetPrice(shopOffer, l))
            );
        }
    }

    private long getTargetPrice(AutoBeneficialSell.ShopOffer shopOffer, long l) {
        long l2 = Math.round(l * (1.0 - this.discount.getValue().floatValue() / 100.0) * shopOffer.count());
        long l3 = DonutPriceUtil.getPriceStep(l2);
        return l3 == 0L ? l2 : (l2 - l3) / (l3 * 2L) * l3 * 2L;
    }

    private AutoBeneficialSell.TradeGood pickGoodToBuy() {
        long l = System.currentTimeMillis();

        for (AutoBeneficialSell.TradeGood tradeGood : GOODS) {
            Item item2 = tradeGood.item();
            AutoBeneficialSell.ShopOffer shopOffer = this.bestOffers.get(item2);
            if (shopOffer != null && this.checkedItems.add(item2) && l >= this.buyCooldowns.getOrDefault(item2, 0L)) {
                long l2 = this.averagePrices.get(item2);
                boolean flag = l2 > 0L && shopOffer.price() <= this.getTargetPrice(shopOffer, l2);
                if ((this.notifyOnly.getValue() || this.priceTracker.getLots(item2).size() < this.lots.getValue().intValue()) && flag) {
                    String string = this.getBuyObstacle(item2, shopOffer);
                    if (string == null) {
                        return tradeGood;
                    }

                    if (this.shownMessages.add("skipped:" + item2 + string)) {
                        this.printChat(
                            "autobeneficialsell.status.skipped",
                            "%s ×%d за %s при средней %s — выгодно, но не покупаю: %s",
                            this.getGoodName(tradeGood),
                            shopOffer.count(),
                            DonutPriceUtil.formatPrice(shopOffer.price()),
                            DonutPriceUtil.formatPrice(l2 * shopOffer.count()),
                            string
                        );
                    }

                    if (l - this.skipNotifyTimes.getOrDefault(item2, 0L) >= 60000L) {
                        this.skipNotifyTimes.put(item2, l);
                        this.sendNotification(
                            this.notifyOnly.getValue() ? NotificationType.INFO : NotificationType.WARNING,
                            "autobeneficialsell.notify.skipped",
                            "%s: не куплю за %s — %s",
                            this.getGoodName(tradeGood),
                            DonutPriceUtil.formatPrice(shopOffer.price()),
                            string
                        );
                    }
                }
            }
        }

        return null;
    }

    private String getBuyObstacle(Item item, AutoBeneficialSell.ShopOffer shopOffer) {
        if (this.notifyOnly.getValue()) {
            return Language.translate("autobeneficialsell.obstacle.hintsOnly", "включены «Только подсказки»");
        }

        if (this.getFreeSpace(item) < shopOffer.count()) {
            return Language.translate("autobeneficialsell.obstacle.noSpace", "нет места в инвентаре");
        }

        long l = this.getBalance();
        return l >= 0L && l + DonutPriceUtil.getPriceStep(l) < shopOffer.price()
            ? Language.translate("autobeneficialsell.obstacle.noMoney", "не хватает денег")
            : null;
    }

    private int getFreeSpace(Item item) {
        PlayerInventory playerInventory = mc.player.getInventory();
        int i = 0;

        for (int i2 = 0; i2 < 36; i2++) {
            ItemStack itemStack = playerInventory.getStack(i2);
            if (itemStack.isEmpty()) {
                i += item.getMaxCount();
            } else if (TradeBook.isPlainItem(itemStack, item)) {
                i += itemStack.getMaxCount() - itemStack.getCount();
            }
        }

        return i;
    }

    private long getBalance() {
        Text text = ((PlayerListHudAccessor)mc.inGameHud.getPlayerListHud()).sadnes$getFooter();
        return text == null ? -1L : DonutPriceUtil.parsePrice(Formatting.strip(text.getString()));
    }

    private void tickBuying() {
        Item item2 = this.buyGood.item();
        int i = ServerManager.getPing();
        int i2 = this.countItem(item2) - this.countBefore;
        if (mc.player.currentScreenHandler == this.container && this.isInDanger()) {
            this.closeContainer();
        }

        if (this.purchase == null && i2 > 0 && this.clickTimer.hasPassed((double)(i + 500))) {
            this.purchase = new AutoBeneficialSell.Purchase(i2, this.buyOffer.price());
        }

        if (this.purchase != null && i2 > 0 || this.clickTimer.hasPassed((double)(i * 2 + 2500))) {
            if (this.purchase == null) {
                this.buyCooldowns.merge(item2, System.currentTimeMillis() + 60000L, Math::max);
            } else {
                this.recordPurchase();
            }

            this.buyGood = null;
            if (mc.player.currentScreenHandler == this.container) {
                this.setState(AutoBeneficialSell.State.SHOP);
            } else {
                this.reset(0L);
            }
        }
    }

    private void recordPurchase() {
        Item item2 = this.buyGood.item();
        long l = Math.max(this.purchase.cost(), Math.round(this.buyOffer.unit() * this.purchase.count()));
        this.priceTracker.addLot(item2, this.purchase.count(), l);
        this.notifyAndPrint(
            NotificationType.SUCCESS,
            "autobeneficialsell.notify.bought",
            "Куплено: %s ×%d за %s",
            this.getGoodName(this.buyGood),
            this.purchase.count(),
            DonutPriceUtil.formatPrice(l)
        );
        boolean flag = this.purchase.count() == this.buyOffer.count() && Math.abs(this.purchase.cost() - this.buyOffer.price()) <= this.buyOffer.price() / 4L;
        if (!flag) {
            this.buyCooldowns.put(item2, System.currentTimeMillis() + 300000L);
            LOGGER.warn(
                "AutoBeneficialSell: /shop showed {} x{} for {}, bought x{} for {}",
                item2,
                this.buyOffer.count(),
                this.buyOffer.price(),
                this.purchase.count(),
                this.purchase.cost()
            );
            if (mc.player.currentScreenHandler == this.container) {
                this.dumpWindow("purchase differs from /shop offer");
            }

            this.notifyAndPrint(
                NotificationType.WARNING,
                "autobeneficialsell.notify.mismatch",
                "%s: покупка прошла не как на витрине — пять минут его не беру",
                this.getGoodName(this.buyGood)
            );
        }
    }

    private void startSellingOrFinish() {
        if (!this.notifyOnly.getValue() && !this.halted && this.pickLotToSell()) {
            this.sellMode = true;
            this.setState(AutoBeneficialSell.State.STOPPING);
        } else {
            this.reset(0L);
        }
    }

    private boolean pickLotToSell() {
        long l = System.currentTimeMillis();

        for (AutoBeneficialSell.TradeGood tradeGood : GOODS) {
            TradeBook.Lot lot = this.priceTracker.getLots(tradeGood.item()).stream().min(Comparator.comparingDouble(TradeBook.Lot::unitCost)).orElse(null);
            if (lot != null) {
                int i = Math.min(lot.count(), this.countItem(tradeGood.item()));
                if (i != 0 && l >= this.sellCooldowns.getOrDefault(tradeGood.item(), 0L)) {
                    this.sellGood = tradeGood;
                    this.sellLot = lot;
                    this.sellCount = i;
                    return true;
                }
            }
        }

        return false;
    }

    private void tickDepositing() {
        if (mc.player.currentScreenHandler != this.container) {
            this.reset(1000L);
        } else if (!OrderDeliveryHelper.isOrderScreen(this.container)) {
            if (this.stateTimer.hasPassed((double)(ServerManager.getPing() + 600))) {
                if (OrderDeliveryHelper.isContainerEmpty(this.container)) {
                    this.closeContainer();
                }

                this.reset(10000L);
            }
        } else if (this.stateTimer.hasPassed(250.0)) {
            this.sellCount = Math.min(this.sellLot.count(), this.countItem(this.sellGood.item()));
            if (this.sellCount != 0 && !this.isInDanger()) {
                this.countBefore = this.countItem(this.sellGood.item());
                this.quotedSum = -1L;
                this.receivedSum = -1L;
                this.quoteSeen = false;
                this.returnAttempts = 0;
                this.sellWindow = new OrderDeliveryHelper(this.container, this.sellGood.item(), this::performSlotClick);
                this.sellWindow.deliver(this.sellCount);
                boolean flag;
                if (this.sellWindow.countDelivered() == this.sellCount) {
                    flag = true;
                } else {
                    flag = false;
                    this.sellCooldowns.put(this.sellGood.item(), System.currentTimeMillis() + 300000L);
                }

                if (flag) {
                    this.setState(AutoBeneficialSell.State.QUOTING);
                } else {
                    this.setState(AutoBeneficialSell.State.RETURNING);
                }
            } else {
                this.closeContainer();
                this.reset(0L);
            }
        }
    }

    private void tickQuoting() {
        if (mc.player.currentScreenHandler != this.container) {
            this.container = null;
            this.setState(AutoBeneficialSell.State.SELLING);
        } else if (this.isInDanger()) {
            this.setState(AutoBeneficialSell.State.RETURNING);
        } else {
            int i = ServerManager.getPing();
            if (this.clickTimer.hasPassed((double)(i + 150))) {
                long l = this.sellWindow.findChangedPrice();
                if (l >= 0L || this.clickTimer.hasPassed((double)(i * 2 + 1500))) {
                    this.quoteSeen = l > 0L;
                    long l2 = Math.round(this.sellLot.unitCost() * this.sellCount);
                    long l3 = Math.max(l2 + 1L, Math.round(l2 * (1.0 + this.profit.getValue().floatValue() / 100.0)));
                    boolean flag = this.quoteSeen && l <= l2 * 2L && this.sellWindow.countDelivered() == this.sellCount;
                    if (flag && l >= l3) {
                        this.quotedSum = l;
                        this.closeContainer();
                        this.setState(AutoBeneficialSell.State.SELLING);
                    } else {
                        double d = flag ? (double)l3 / l - 1.0 : 1.0;
                        this.sellCooldowns.put(this.sellGood.item(), System.currentTimeMillis() + (d <= 0.01 ? 60000 : (d <= 0.03 ? 300000 : 900000)));
                        if (!flag) {
                            this.dumpWindow("no trusted sum on /sell buttons");
                            this.sendNotification(
                                NotificationType.WARNING,
                                "autobeneficialsell.notify.noQuote",
                                "%s не продаю: в окне /sell не видно суммы",
                                this.getGoodName(this.sellGood)
                            );
                            this.setState(AutoBeneficialSell.State.RETURNING);
                        } else {
                            if (this.shownMessages.add("offer:" + this.sellGood.item() + l)) {
                                this.printChat(
                                    "autobeneficialsell.status.lowOffer",
                                    "В /sell за %s ×%d дают %s, нужно от %s — жду цену получше",
                                    this.getGoodName(this.sellGood),
                                    this.sellCount,
                                    DonutPriceUtil.formatPrice(l),
                                    DonutPriceUtil.formatPrice(l3)
                                );
                            }

                            this.setState(AutoBeneficialSell.State.RETURNING);
                        }
                    }
                }
            }
        }
    }

    private void tickReturning() {
        if (mc.player.currentScreenHandler != this.container) {
            this.container = null;
            this.setState(AutoBeneficialSell.State.SELLING);
        } else {
            int i = ServerManager.getPing();
            if (this.sellWindow.findChangedPrice() > 0L) {
                this.quoteSeen = true;
            }

            if (this.returnAttempts <= 0 || this.clickTimer.hasPassed((double)(i + 250))) {
                boolean flag = this.sellWindow.countDelivered() == 0
                    && this.container.getCursorStack().isEmpty()
                    && this.countItem(this.sellGood.item()) >= this.countBefore;
                boolean flag2 = this.quoteSeen ? this.sellWindow.findChangedPrice() < 0L : this.clickTimer.hasPassed((double)(i * 2 + 1000));
                if (this.returnAttempts > 0 && flag && flag2) {
                    this.closeContainer();
                    this.reset(0L);
                } else if (this.returnAttempts <= 0 || !flag || this.clickTimer.hasPassed((double)(i * 2 + 1500))) {
                    this.sellWindow.takeBack(this.returnAttempts);
                    this.returnAttempts++;
                    if (this.returnAttempts == 5) {
                        MovementPauseComponent.resume(this);
                        this.notifyAndPrint(
                            NotificationType.WARNING,
                            "autobeneficialsell.notify.stuck",
                            "Не получается забрать %s из окна /sell — пробую снова",
                            this.getGoodName(this.sellGood)
                        );
                    }
                }
            }
        }
    }

    private void tickSelling() {
        boolean flag;
        if (this.receivedSum < 0L) {
            flag = this.stateTimer.hasPassed((double)(ServerManager.getPing() * 2 + 1500));
        } else {
            flag = true;
        }

        if (flag) {
            this.finishSale();
        }
    }

    private void finishSale() {
        if (this.countItem(this.sellGood.item()) <= this.countBefore - this.sellCount) {
            long l = Math.round(this.sellLot.unitCost() * this.sellCount);
            long l2 = this.receivedSum >= 0L ? this.receivedSum : this.quotedSum;
            this.priceTracker.removeLot(this.sellLot);
            this.sellCooldowns.remove(this.sellGood.item());
            if (l2 < 0L) {
                this.notifyAndPrint(
                    NotificationType.WARNING,
                    "autobeneficialsell.notify.soldBlind",
                    "%s продан по цене сервера: окно /sell закрылось само",
                    this.getGoodName(this.sellGood)
                );
            } else {
                this.notifyAndPrint(
                    l2 >= l ? NotificationType.SUCCESS : NotificationType.WARNING,
                    "autobeneficialsell.notify.sold",
                    "Продано: %s ×%d за %s, прибыль %s",
                    this.getGoodName(this.sellGood),
                    this.sellCount,
                    DonutPriceUtil.formatPrice(l2),
                    DonutPriceUtil.formatPrice(l2 - l)
                );
            }

            this.halted = l2 < l;
            if (this.halted) {
                this.notifyAndPrint(
                    NotificationType.WARNING, "autobeneficialsell.notify.halted", "Продажи остановлены: сделка прошла не по плану. Включите модуль заново"
                );
            }

            this.reset(0L);
        } else {
            this.reset(0L);
        }
    }

    private int countItem(Item item) {
        PlayerInventory playerInventory = mc.player.getInventory();
        int i = 0;

        for (int i2 = 0; i2 < 36; i2++) {
            if (TradeBook.isPlainItem(playerInventory.getStack(i2), item)) {
                i += playerInventory.getStack(i2).getCount();
            }
        }

        return i;
    }

    private String getGoodName(AutoBeneficialSell.TradeGood tradeGood) {
        return tradeGood.item().getName().getString();
    }

    private boolean isContainerOpen() {
        return this.container != null && mc.player != null && mc.player.currentScreenHandler == this.container;
    }

    private boolean isInSellWindow() {
        return this.state == AutoBeneficialSell.State.QUOTING || this.state == AutoBeneficialSell.State.RETURNING;
    }

    private void performSlotClick(int i, int i2, SlotActionType slotActionType) {
        this.selfClicking = true;

        try {
            mc.interactionManager.clickSlot(this.container.syncId, i, i2, slotActionType, mc.player);
        } finally {
            this.selfClicking = false;
        }

        this.clickTimer.reset();
    }

    private void closeContainer() {
        this.selfClicking = true;

        try {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(this.container.syncId));
        } finally {
            this.selfClicking = false;
        }

        mc.player.closeScreen();
        this.container = null;
    }

    private void sendNotification(NotificationType notificationType, String string, String string2, Object... objects) {
        NotificationUtil.notifyKeyed(notificationType, this.getDisplayName(), String.format(Language.translate(string, string2), objects));
    }

    private void printChat(String string, String string2, Object... objects) {
        ChatUtil.print(this.getDisplayName() + ": " + String.format(Language.translate(string, string2), objects));
    }

    private void notifyAndPrint(NotificationType notificationType, String string, String string2, Object... objects) {
        this.sendNotification(notificationType, string, string2, objects);
        this.printChat(string, string2, objects);
    }

    private void dumpWindow(String string) {
        if (this.shownMessages.add(string)) {
            StringBuilder builder = new StringBuilder();
            int i = this.container.getRows() * 9;

            for (int i2 = 0; i2 < i; i2++) {
                ItemStack itemStack = this.container.getSlot(i2).getStack();
                if (!itemStack.isEmpty()) {
                    builder.append("\n  ")
                        .append(i2)
                        .append(": ")
                        .append(itemStack.getCount())
                        .append("x ")
                        .append(Registries.ITEM.getId(itemStack.getItem()))
                        .append(" \"")
                        .append(itemStack.getName().getString())
                        .append('"');
                    LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
                    if (loreComponent != null) {
                        for (Text text : loreComponent.lines()) {
                            builder.append(" | ").append(text.getString());
                        }
                    }
                }
            }

            String string2 = mc.currentScreen == null ? "" : mc.currentScreen.getTitle().getString();
            LOGGER.warn("AutoBeneficialSell: {}, window \"{}\":{}", string, string2, builder);
        }
    }

    private void reset(long l) {
        MovementPauseComponent.resume(this);
        this.container = null;
        this.sellWindow = null;
        this.buyGood = null;
        this.sellGood = null;
        this.sellLot = null;
        this.bestOffers.clear();
        this.averagePrices.clear();
        this.checkedItems.clear();
        this.lastInventoryCounts.clear();
        this.nextRunTime = System.currentTimeMillis() + Math.max(l, (long)(this.interval.getValue() * 1000.0F));
        this.setState(AutoBeneficialSell.State.IDLE);
    }

    private void setState(AutoBeneficialSell.State state2) {
        this.state = state2;
        this.stateTimer.reset();
    }

    record Purchase(int count, long cost) {
    }

    record ShopOffer(int slot, int count, long price) {
        double unit() {
            return (double)this.price / this.count;
        }
    }

    enum State {
        IDLE,
        STOPPING,
        OPENING,
        SHOP,
        BUYING,
        DEPOSITING,
        QUOTING,
        RETURNING,
        SELLING;
    }

    record TradeGood(String option, Item item, String chatName) {
    }
}
