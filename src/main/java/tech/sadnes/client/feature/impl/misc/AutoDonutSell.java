package tech.sadnes.client.feature.impl.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.CloseScreenEvent;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.SetScreenEvent;
import tech.sadnes.client.event.impl.SlotClickEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.feature.component.MovementPauseComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.donut.QuickAction;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.chat.ChatMessageUtil;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.ServerInputTracker;

@FeatureInfo(
    name = "AutoDonutSell",
    description = "feature.impl.misc.autodonutsell",
    displayName = "feature.impl.misc.autodonutsell",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SERVER
)
public final class AutoDonutSell extends Feature {
    private final BooleanSetting anyPickup = new BooleanSetting("autodonutsell.anyPickup", false);
    private final NumberSetting freeSlots = new NumberSetting("autodonutsell.freeSlots", 3.0F, 1.0F, 9.0F, 1.0F);
    private final List<AutoDonutSell.DeathPosition> deathPositions = new ArrayList<>();
    private final Map<Integer, ItemStack> trackedItems = new HashMap<>();
    private final List<AutoDonutSell.PickupRecord> pickups = new ArrayList<>();
    private final Set<Integer> lootSlots = new HashSet<>();
    private final ItemStack[] inventorySnapshot = new ItemStack[36];
    private final StopWatch pickupTimer = new StopWatch();
    private final StopWatch damageTimer = new StopWatch();
    private AutoDonutSell.State state = AutoDonutSell.State.IDLE;
    private final StopWatch stateTimer = new StopWatch();
    private final StopWatch clickTimer = new StopWatch();
    private GenericContainerScreenHandler container;
    private final List<ItemStack> parkedTotems = new ArrayList<>();
    private final Set<Integer> parkedSlots = new HashSet<>();
    private int ownTotemsInWindow;
    private int soldCount;
    private int returnAttempts;
    private int openFailures;
    private int emptyRuns;
    private long nextRunTime;
    private long lastNoTotemNotify;
    private boolean selfClicking;
    private boolean holdingRotation;

    public boolean isBusy() {
        return this.state != AutoDonutSell.State.IDLE;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.state = AutoDonutSell.State.IDLE;
        this.openFailures = 0;
        this.emptyRuns = 0;
        this.nextRunTime = 0L;
    }

    @Override
    public void onEnable() {
        boolean flag = this.container != null && mc.player != null && mc.player.currentScreenHandler == this.container;
        if (flag) {
            this.reclaimOwnTotems();
            int i = this.countParkedTotems();
            int i2 = 0;

            while (i2 < i && this.retrieveTotem()) {
                i2++;
            }

            this.closeAndFinish();
        } else if (this.state != AutoDonutSell.State.IDLE) {
            this.reset(0L);
        }

        this.deathPositions.clear();
        this.trackedItems.clear();
        this.pickups.clear();
        this.lootSlots.clear();
        Arrays.fill(this.inventorySnapshot, null);
        super.onEnable();
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.deathPositions.clear();
        this.trackedItems.clear();
        this.pickups.clear();
        if (this.state != AutoDonutSell.State.IDLE) {
            this.reset(0L);
        }
    }

    @EventHandler
    public void onUpdate(WorldTickEvent worldTickEvent) {
        if (!ServerManager.isNullWorld() && ServerManager.isDonut()) {
            this.trackLootItems();
            this.detectLootSlots();
            switch (this.state) {
                case IDLE:
                    this.tickIdle();
                    this.snapshotInventory();
                    return;
                case CALLING:
                    this.tickCalling();
                    this.snapshotInventory();
                    return;
                case OPENING:
                    this.tickOpening();
                    this.snapshotInventory();
                    return;
                case SELLING:
                    this.tickSelling();
                    this.snapshotInventory();
                    return;
                case RETURNING:
                    this.tickReturning();
                default:
                    this.snapshotInventory();
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        Packet packet = packetEvent.getPacket();
        if (packetEvent.isSend()) {
            boolean flag;
            if (!(packet instanceof CloseHandledScreenC2SPacket)) {
                flag = packet instanceof ClickSlotC2SPacket;
            } else {
                flag = true;
            }

            boolean flag2;
            label76: {
                boolean flag3;
                if (flag) {
                    if (this.container == null) {
                        flag2 = this.isAwaitingWindow();
                        break label76;
                    }

                    flag3 = true;
                } else {
                    flag3 = false;
                }

                flag2 = flag3;
            }

            boolean flag4;
            if (packet instanceof PlayerInteractEntityC2SPacket) {
                flag4 = !this.parkedSlots.isEmpty();
            } else {
                flag4 = false;
            }

            if (flag4 && !this.selfClicking) {
                this.damageTimer.reset();
            }

            if ((flag2 || flag4) && !this.selfClicking) {
                packetEvent.cancel();
            }
        } else {
            boolean flag5;
            if (this.state == AutoDonutSell.State.OPENING && packet instanceof GameMessageS2CPacket gameMessageS2CPacket && !gameMessageS2CPacket.overlay()) {
                flag5 = ChatMessageUtil.isErrorMessage(ChatMessageUtil.getPlainText(gameMessageS2CPacket));
            } else {
                flag5 = false;
            }

            boolean flag6;
            if ((!(packet instanceof EntityStatusS2CPacket) || ((EntityStatusS2CPacket)packet).getStatus() != 3)
                && !flag5
                && !(packet instanceof EntityDamageS2CPacket)) {
                flag6 = packet instanceof ItemPickupAnimationS2CPacket;
            } else {
                flag6 = true;
            }

            if (packetEvent.isReceive() && flag6) {
                mc.execute(() -> this.handlePacket(packet));
                return;
            }
        }
    }

    @EventHandler
    public void onCloseScreen(SlotClickEvent slotClickEvent) {
        boolean flag;
        if (this.container == null) {
            flag = this.isAwaitingWindow();
        } else {
            flag = true;
        }

        if (flag && !this.selfClicking) {
            slotClickEvent.cancel();
        }
    }

    @EventHandler(priority = 500)
    public void onOpenScreen(SetScreenEvent setScreenEvent) {
        boolean flag;
        if (this.container != null) {
            flag = setScreenEvent.getScreen() instanceof InventoryScreen;
        } else {
            flag = false;
        }

        if (flag) {
            setScreenEvent.cancel();
            if (this.state == AutoDonutSell.State.SELLING) {
                this.setState(AutoDonutSell.State.RETURNING);
            }
        } else if (this.state == AutoDonutSell.State.OPENING && mc.player != null && setScreenEvent.getScreen() instanceof GenericContainerScreen genericContainerScreen) {
            GenericContainerScreenHandler genericContainerScreenHandler = genericContainerScreen.getScreenHandler();
            if (mc.player.currentScreenHandler != genericContainerScreenHandler) {
                return;
            }

            setScreenEvent.cancel();
            this.container = genericContainerScreenHandler;
            this.openFailures = 0;
            this.ownTotemsInWindow = 0;
            this.soldCount = 0;
            this.returnAttempts = 0;
            this.setState(AutoDonutSell.State.SELLING);
            return;
        }
    }

    @EventHandler
    public void onServerCloseScreen(CloseScreenEvent closeScreenEvent) {
        if (this.container != null && !this.selfClicking) {
            closeScreenEvent.cancel();
            if (this.state == AutoDonutSell.State.SELLING) {
                this.setState(AutoDonutSell.State.RETURNING);
            }
        }
    }

    private void handlePacket(Packet<?> packet) {
        if (!ServerManager.isNullWorld() && ServerManager.isDonut()) {
            if (packet instanceof GameMessageS2CPacket) {
                if (this.state == AutoDonutSell.State.OPENING) {
                    this.handleOpenFailure();
                    return;
                }
            } else {
                if (packet instanceof ItemPickupAnimationS2CPacket itemPickupAnimationS2CPacket) {
                    ItemStack itemStack = this.trackedItems.get(itemPickupAnimationS2CPacket.getEntityId());
                    if (itemStack != null && itemPickupAnimationS2CPacket.getCollectorEntityId() == mc.player.getId()) {
                        this.pickupTimer.reset();
                        long l = System.currentTimeMillis();
                        int i = this.pickups.size();

                        for (int i2 = 0; i2 < i; i2++) {
                            AutoDonutSell.PickupRecord pickupRecord = this.pickups.get(i2);
                            if (ItemStack.areItemsAndComponentsEqual(pickupRecord.item(), itemStack)) {
                                this.pickups.set(i2, new AutoDonutSell.PickupRecord(itemStack, pickupRecord.count() + itemPickupAnimationS2CPacket.getStackAmount(), l));
                                return;
                            }
                        }

                        this.pickups.add(new AutoDonutSell.PickupRecord(itemStack, itemPickupAnimationS2CPacket.getStackAmount(), l));
                        return;
                    }

                    return;
                }

                if (packet instanceof EntityDamageS2CPacket entityDamageS2CPacket) {
                    boolean flag;
                    if (entityDamageS2CPacket.sourceCauseId() == -1 && entityDamageS2CPacket.sourceDirectId() == -1) {
                        flag = entityDamageS2CPacket.sourcePosition().isPresent();
                    } else {
                        flag = true;
                    }

                    if (entityDamageS2CPacket.entityId() == mc.player.getId() && flag) {
                        this.damageTimer.reset();
                    }

                    return;
                }

                if (packet instanceof EntityStatusS2CPacket entityStatusS2CPacket) {
                    if (!(entityStatusS2CPacket.getEntity(mc.world) instanceof PlayerEntity playerEntity) || playerEntity == mc.player) {
                        return;
                    }

                    boolean flag2 = Sadnes.getInstance().getFriendManager().isFriend(playerEntity.getGameProfile().name());
                    if (flag2 || playerEntity.squaredDistanceTo(mc.player) > 1024.0) {
                        return;
                    }

                    this.deathPositions.add(new AutoDonutSell.DeathPosition(playerEntity.getEntityPos(), System.currentTimeMillis()));
                }
            }
        }
    }

    private void trackLootItems() {
        long l = System.currentTimeMillis();
        this.deathPositions.removeIf(deathPosition -> l - deathPosition.time() > 1500L);
        this.pickups.removeIf(pickupRecord -> pickupRecord.count() <= 0 || l - pickupRecord.time() > 3000L);
        this.trackedItems.keySet().removeIf(integer -> mc.world.getEntityById(integer) == null);
        if (!this.deathPositions.isEmpty() || this.anyPickup.getValue()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof ItemEntity itemEntity
                    && !this.trackedItems.containsKey(itemEntity.getId())
                    && (this.anyPickup.getValue() && itemEntity.squaredDistanceTo(mc.player) < 64.0 || this.isNearDeath(itemEntity, l))) {
                    this.trackedItems.put(itemEntity.getId(), itemEntity.getStack().copyWithCount(1));
                }
            }
        }
    }

    private boolean isNearDeath(ItemEntity itemEntity, long l) {
        for (AutoDonutSell.DeathPosition deathPosition : this.deathPositions) {
            boolean flag = itemEntity.age <= (l - deathPosition.time()) / 50L + 3L;
            if (flag && itemEntity.squaredDistanceTo(deathPosition.pos()) <= 16.0) {
                return true;
            }
        }

        return false;
    }

    private void detectLootSlots() {
        PlayerInventory playerInventory = mc.player.getInventory();

        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = this.inventorySnapshot[i];
            ItemStack itemStack2 = playerInventory.getStack(i);
            if (itemStack != null) {
                boolean flag;
                if (!itemStack2.isEmpty() && ItemStack.areItemsAndComponentsEqual(itemStack, itemStack2)) {
                    flag = true;
                } else {
                    flag = false;
                    this.lootSlots.remove(i);
                }

                int i2 = flag ? itemStack2.getCount() - itemStack.getCount() : itemStack2.getCount();
                boolean flag2;
                if (i2 > 0) {
                    flag2 = this.consumePickup(itemStack2, i2);
                } else {
                    flag2 = false;
                }

                if (flag2 && !flag) {
                    this.lootSlots.add(i);
                }
            }
        }
    }

    private boolean consumePickup(ItemStack itemStack, int i) {
        int i2 = this.pickups.size();

        for (int i3 = 0; i3 < i2; i3++) {
            AutoDonutSell.PickupRecord pickupRecord = this.pickups.get(i3);
            if (pickupRecord.count() >= i && ItemStack.areItemsAndComponentsEqual(pickupRecord.item(), itemStack)) {
                this.pickups.set(i3, new AutoDonutSell.PickupRecord(pickupRecord.item(), pickupRecord.count() - i, pickupRecord.time()));
                return true;
            }
        }

        return false;
    }

    private void snapshotInventory() {
        PlayerInventory playerInventory = mc.player.getInventory();

        for (int i = 0; i < 36; i++) {
            this.inventorySnapshot[i] = playerInventory.getStack(i).copy();
        }
    }

    private void tickIdle() {
        if (this.canStart() && (!this.lootSlots.isEmpty() || this.hasFreeTotemForBlockedLoot())) {
            boolean flag;
            if (this.openFailures != 1) {
                flag = QuickAction.SELL.run();
            } else {
                flag = false;
            }

            if (flag) {
                this.setState(AutoDonutSell.State.OPENING);
            } else {
                this.setState(AutoDonutSell.State.CALLING);
            }
        }
    }

    private void tickCalling() {
        if (this.tryHoldStill()) {
            mc.player.networkHandler.sendChatCommand(QuickAction.SELL.getId());
            this.releaseHold();
            this.setState(AutoDonutSell.State.OPENING);
        } else {
            if (this.stateTimer.hasPassed(1000.0)) {
                this.reset(1000L);
            }
        }
    }

    private boolean canStart() {
        return mc.currentScreen == null
                && mc.player.currentScreenHandler == mc.player.playerScreenHandler
                && mc.player.currentScreenHandler.getCursorStack().isEmpty()
                && !mc.player.isUsingItem()
                && !mc.player.isSneaking()
                && MovementPauseComponent.hasPendingTasks()
                && !MovementPauseComponent.isPaused()
                && !GuiMoveComponent.isActive()
                && !this.isInDanger()
                && !this.isOtherFeatureBusy()
            ? System.currentTimeMillis() >= this.nextRunTime
            : false;
    }

    private boolean hasFreeTotemForBlockedLoot() {
        if (this.getLootStats().blocked() == 0) {
            return false;
        }

        boolean flag = IntStream.range(9, 36).anyMatch(this::isFreeTotemSlot);
        boolean flag2 = System.currentTimeMillis() - this.lastNoTotemNotify > 15000L;
        if (!flag && flag2) {
            this.lastNoTotemNotify = System.currentTimeMillis();
            NotificationUtil.notifyKeyed(
                NotificationType.WARNING,
                this.getDisplayName(),
                Language.translate("autodonutsell.notify.noTotem", "Инвентарь забит, а свободного тотема нет — лут не подобрать")
            );
        }

        return flag;
    }

    private boolean isAwaitingWindow() {
        return this.state == AutoDonutSell.State.OPENING && !this.stateTimer.hasPassed((double)(ServerManager.getPing() * 2 + 150));
    }

    private void tickOpening() {
        if (this.stateTimer.hasPassed((double)(Math.max(1500, ServerManager.getPing() * 3) + 1500))) {
            this.handleOpenFailure();
        }
    }

    private void handleOpenFailure() {
        this.openFailures++;
        if (this.openFailures < 3) {
            this.reset(1500L);
        } else {
            this.openFailures = 0;
            NotificationUtil.notifyKeyed(
                NotificationType.WARNING, this.getDisplayName(), Language.translate("autodonutsell.notify.noOpen", "Не удалось открыть /sell — повторю позже")
            );
            this.reset(30000L);
        }
    }

    private boolean isOtherFeatureBusy() {
        AutoLeave autoLeave = Sadnes.getInstance().getFeatureManager().getFeature(AutoLeave.class);
        return autoLeave != null && autoLeave.shouldLeaveHost();
    }

    private boolean isInDanger() {
        AttackAura attackAura = AttackAura.getInstance();
        return attackAura != null && attackAura.isEnabled() && attackAura.getTarget() != null ? true : !this.damageTimer.hasPassed(1500.0);
    }

    private AutoDonutSell.LootStats getLootStats() {
        PlayerInventory playerInventory = mc.player.getInventory();
        int i = ServerManager.getPing() / 50 + 3;
        Vec3d vec3d = mc.player.getVelocity().multiply(i, 0.0, i);
        Box box = mc.player.getBoundingBox().expand(1.0, 0.5, 1.0).stretch(vec3d);
        int i2 = 38 - ServerManager.getPing() / 50;
        boolean flag = playerInventory.getEmptySlot() != -1;
        int i3 = 0;
        int i4 = 0;

        for (int i5 : this.trackedItems.keySet()) {
            if (mc.world.getEntityById(i5) instanceof ItemEntity itemEntity && itemEntity.age >= i2) {
                if (itemEntity.squaredDistanceTo(mc.player) < 25.0) {
                    i3++;
                }

                boolean flag2 = !flag && playerInventory.getOccupiedSlotWithRoomForStack(itemEntity.getStack()) == -1;
                if (flag2 && itemEntity.getBoundingBox().intersects(box)) {
                    i4++;
                }
            }
        }

        return new AutoDonutSell.LootStats(i3, i4);
    }

    private void tickSelling() {
        if (mc.player.currentScreenHandler != this.container) {
            if (!this.parkedTotems.isEmpty()) {
                this.notifyTotemLost();
            }

            this.reset(1000L);
        } else {
            AutoDonutSell.LootStats lootStats = this.getLootStats();
            int i = ServerManager.getPing();
            boolean flag = !this.parkedSlots.isEmpty();
            boolean flag2 = this.findWindowSlotFor(ItemStack.EMPTY) == -1;
            boolean flag3;
            if (!this.lootSlots.isEmpty()) {
                flag3 = !flag2;
            } else {
                flag3 = false;
            }

            int i2 = this.freeSlots.getValue().intValue() - this.parkedTotems.size();
            boolean flag4;
            if (i2 > 0 && !flag2) {
                flag4 = IntStream.range(9, 36).anyMatch(this::isFreeTotemSlot);
            } else {
                flag4 = false;
            }

            boolean flag5;
            label147: {
                int i3 = this.countEmptySlots();
                boolean flag6;
                if (lootStats.near() <= i3 || !flag4 && (!flag3 || i3 != 0)) {
                    if (!flag) {
                        flag5 = flag3;
                        break label147;
                    }

                    flag6 = false;
                } else {
                    flag6 = true;
                }

                flag5 = flag6;
            }

            boolean flag7;
            if (!this.isInDanger() && !this.isOtherFeatureBusy()) {
                flag7 = this.stateTimer.hasPassed(flag ? 12000.0 : 1500.0);
            } else {
                flag7 = true;
            }

            boolean flag8;
            if (this.pickupTimer.hasPassed((double)(i + 1200))) {
                flag8 = this.clickTimer.hasPassed((double)(i + 1200));
            } else {
                flag8 = false;
            }

            label146: {
                if (this.clickTimer.hasPassed((double)(i + 75)) && (lootStats.near() == 0 || flag8 || flag2)) {
                    if (!flag5) {
                        this.setState(AutoDonutSell.State.RETURNING);
                        return;
                    }

                    if (flag7) {
                        break label146;
                    }
                } else if (!flag5 ? !flag || flag7 : flag7) {
                    break label146;
                }

                if (!flag5) {
                    this.releaseHold();
                    return;
                }

                if (!this.tryHoldStill()) {
                    return;
                }

                this.soldCount = this.soldCount + this.moveLootToWindow();
                this.parkTotems(Math.min(this.getLootStats().near() - this.countEmptySlots(), i2));
                if (this.parkedSlots.isEmpty()) {
                    this.closeAndFinish();
                    return;
                }

                this.releaseHold();
                return;
            }

            this.setState(AutoDonutSell.State.RETURNING);
        }
    }

    private int countEmptySlots() {
        PlayerInventory playerInventory = mc.player.getInventory();
        return (int)IntStream.range(0, 36).filter(i -> playerInventory.getStack(i).isEmpty()).count();
    }

    private int moveLootToWindow() {
        PlayerInventory playerInventory = mc.player.getInventory();
        int i = 0;

        for (int i2 : List.copyOf(this.lootSlots)) {
            boolean flag = playerInventory.getStack(i2).isOf(Items.TOTEM_OF_UNDYING);
            if (this.findWindowSlotFor(playerInventory.getStack(i2)) != -1) {
                this.clickSlot(this.toWindowSlot(i2), SlotActionType.QUICK_MOVE);
                if (playerInventory.getStack(i2).isEmpty()) {
                    this.lootSlots.remove(i2);
                    if (flag) {
                        this.ownTotemsInWindow++;
                    }

                    i++;
                }
            }
        }

        return i;
    }

    private void reclaimOwnTotems() {
        PlayerInventory playerInventory = mc.player.getInventory();

        for (int i : List.copyOf(this.lootSlots)) {
            if (this.countParkedTotems() <= 0) {
                return;
            }

            if (playerInventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                this.lootSlots.remove(i);
                this.ownTotemsInWindow++;
            }
        }
    }

    private int countParkedTotems() {
        int i = this.container.getRows() * 9;
        int i2 = 0;

        for (int i3 = 0; i3 < i; i3++) {
            if (this.container.getSlot(i3).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                i2++;
            }
        }

        return i2 - this.ownTotemsInWindow;
    }

    private int toWindowSlot(int i) {
        int i2 = this.container.getRows() * 9;
        return i < 9 ? i2 + i + 27 : i2 + (i - 9);
    }

    private void parkTotems(int i) {
        for (int i2 = 9; i2 < 36 && i > 0; i2++) {
            if (this.isFreeTotemSlot(i2) && this.findWindowSlotFor(ItemStack.EMPTY) != -1) {
                this.parkedTotems.add(mc.player.getInventory().getStack(i2).copy());
                this.parkedSlots.add(i2);
                this.clickSlot(this.toWindowSlot(i2), SlotActionType.QUICK_MOVE);
                i--;
            }
        }
    }

    private boolean isFreeTotemSlot(int i) {
        ItemStack itemStack = mc.player.getInventory().getStack(i);
        return itemStack.isOf(Items.TOTEM_OF_UNDYING) && !itemStack.hasGlint() && !itemStack.contains(DataComponentTypes.CUSTOM_NAME) && !this.lootSlots.contains(i);
    }

    private void tickReturning() {
        if (mc.player.currentScreenHandler != this.container) {
            if (!this.parkedTotems.isEmpty()) {
                this.notifyTotemLost();
            }

            this.reset(1000L);
        } else if (this.returnAttempts != 0 && !this.clickTimer.hasPassed((double)(ServerManager.getPing() + 75))) {
            this.releaseHold();
        } else if (this.tryHoldStill() || this.stateTimer.hasPassed(this.parkedSlots.isEmpty() ? 1000.0 : 6000.0)) {
            label58: {
                this.reclaimOwnTotems();
                this.soldCount = this.soldCount + this.moveLootToWindow();
                ItemStack itemStack = this.container.getCursorStack();
                int i = this.countParkedTotems();
                if (itemStack.isEmpty()) {
                    if (i <= 0) {
                        break label58;
                    }

                    if (this.returnAttempts >= 3) {
                        this.closeAndFinish();
                        return;
                    }
                }

                if (!this.stateTimer.hasPassed(10000.0)) {
                    this.returnAttempts++;
                    if (itemStack.isEmpty()) {
                        for (int i2 = 0; i2 < i; i2++) {
                            if (!this.retrieveTotem()) {
                                this.releaseHold();
                                return;
                            }
                        }
                    } else if (!this.placeCursorStack(itemStack)) {
                        this.closeAndFinish();
                        return;
                    }

                    this.releaseHold();
                    return;
                }
            }

            this.closeAndFinish();
        }
    }

    private boolean placeCursorStack(ItemStack itemStack) {
        int i = this.findReturnSlot();
        int i2 = this.findWindowSlotFor(itemStack);
        if (i2 == -1) {
            i2 = this.findTotemInWindow();
        }

        boolean flag = itemStack.isOf(Items.TOTEM_OF_UNDYING);
        int i3 = !flag ? i2 : (i == -1 ? -1 : this.toWindowSlot(i));
        if (i3 != -1) {
            this.clickSlot(i3, SlotActionType.PICKUP);
        }

        return i3 != -1;
    }

    private boolean retrieveTotem() {
        int i = this.findTotemInWindow();
        int i2 = this.findReturnSlot();
        if (i != -1 && i2 != -1) {
            ItemStack itemStack = this.container.getSlot(i).getStack().copy();
            this.clickSlot(i, SlotActionType.PICKUP);
            this.clickSlot(this.toWindowSlot(i2), SlotActionType.PICKUP);
            if (!this.container.getCursorStack().isEmpty()) {
                this.clickSlot(i, SlotActionType.PICKUP);
                this.soldCount++;
            }

            this.lootSlots.remove(i2);
            int i3 = this.parkedTotems.size();

            for (int i4 = 0; i4 < i3; i4++) {
                if (ItemStack.areItemsAndComponentsEqual(this.parkedTotems.get(i4), itemStack)) {
                    this.parkedTotems.remove(i4);
                    return true;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private int findReturnSlot() {
        PlayerInventory playerInventory = mc.player.getInventory();
        int i = -1;

        for (int i2 = 9; i2 < 36; i2++) {
            ItemStack itemStack = playerInventory.getStack(i2);
            if (itemStack.isEmpty()) {
                return i2;
            }

            boolean flag;
            if (!this.lootSlots.contains(i2)) {
                flag = this.parkedSlots.contains(i2);
            } else {
                flag = true;
            }

            if (i == -1 && flag && !itemStack.isOf(Items.TOTEM_OF_UNDYING)) {
                i = i2;
            }
        }

        return i;
    }

    private int findTotemInWindow() {
        int i = this.container.getRows() * 9;
        int i2 = -1;

        for (int i3 = 0; i3 < i; i3++) {
            ItemStack itemStack = this.container.getSlot(i3).getStack();
            if (itemStack.isOf(Items.TOTEM_OF_UNDYING)) {
                boolean flag = this.parkedTotems.stream().anyMatch(itemStack2 -> ItemStack.areItemsAndComponentsEqual(itemStack2, itemStack));
                if (flag) {
                    return i3;
                }

                if (i2 == -1) {
                    i2 = i3;
                }
            }
        }

        return i2;
    }

    private int findWindowSlotFor(ItemStack itemStack) {
        int i = this.container.getRows() * 9;

        for (int i2 = 0; i2 < i; i2++) {
            ItemStack itemStack2 = this.container.getSlot(i2).getStack();
            boolean flag = !itemStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(itemStack2, itemStack) && itemStack2.getCount() < itemStack2.getMaxCount();
            if (itemStack2.isEmpty() || flag) {
                return i2;
            }
        }

        return -1;
    }

    private void clickSlot(int i, SlotActionType slotActionType) {
        this.selfClicking = true;

        try {
            mc.interactionManager.clickSlot(this.container.syncId, i, 0, slotActionType, mc.player);
        } finally {
            this.selfClicking = false;
        }

        this.clickTimer.reset();
    }

    private void closeAndFinish() {
        if (this.countParkedTotems() > 0) {
            this.notifyTotemLost();
        }

        this.selfClicking = true;

        try {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(this.container.syncId));
        } finally {
            this.selfClicking = false;
        }

        mc.player.currentScreenHandler = mc.player.playerScreenHandler;
        this.emptyRuns = this.soldCount > 0 ? 0 : this.emptyRuns + 1;
        this.reset(this.emptyRuns == 0 ? 250L : Math.min(6000, this.emptyRuns * 2000));
    }

    private void notifyTotemLost() {
        NotificationUtil.notifyKeyed(
            NotificationType.WARNING,
            this.getDisplayName(),
            Language.translate("autodonutsell.notify.totemLost", "Не все тотемы удалось вернуть из окна продажи")
        );
    }

    private boolean tryHoldStill() {
        boolean flag;
        if (this.state == AutoDonutSell.State.RETURNING && !this.parkedSlots.isEmpty()) {
            flag = this.stateTimer.hasPassed(2000.0);
        } else {
            flag = false;
        }

        if (mc.player.isSneaking() && !flag) {
            this.releaseHold();
            return false;
        } else {
            MovementPauseComponent.pause(this);
            this.holdingRotation = true;
            return ServerInputTracker.isStandingStill();
        }
    }

    private void releaseHold() {
        if (this.holdingRotation) {
            MovementPauseComponent.resume(this);
            this.holdingRotation = false;
        }
    }

    private void reset(long l) {
        this.releaseHold();
        this.container = null;
        this.parkedTotems.clear();
        this.parkedSlots.clear();
        this.nextRunTime = System.currentTimeMillis() + l;
        this.setState(AutoDonutSell.State.IDLE);
    }

    private void setState(AutoDonutSell.State state2) {
        this.state = state2;
        this.stateTimer.reset();
    }

    record DeathPosition(Vec3d pos, long time) {
    }

    record LootStats(int near, int blocked) {
    }

    record PickupRecord(ItemStack item, int count, long time) {
    }

    enum State {
        IDLE,
        CALLING,
        OPENING,
        SELLING,
        RETURNING;
    }
}
