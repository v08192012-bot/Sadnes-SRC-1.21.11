package tech.sadnes.client.feature.impl.misc;

import net.minecraft.component.ComponentMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.api.mixin.accessor.PlayerInteractEntityC2SPacketAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.EntityRemoveEvent;
import tech.sadnes.client.event.impl.MoveEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.PlayerInputPacketEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.SendMovementPacketsEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.event.impl.UseItemEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.Beta;
import tech.sadnes.client.feature.BetaScope;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Danger;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.AuctionUtil;
import tech.sadnes.client.server.MarkerLevel;
import tech.sadnes.client.server.MarkerServer;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.auction.AuctionDumper;
import tech.sadnes.client.server.auction.AuctionMarketLog;
import tech.sadnes.client.server.autobuy.AutoBuyController;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.network.PacketSender;

@Beta(scope = BetaScope.ONLY_ADMIN)
@Danger(reasons = {MarkerServer.FUNTIME, MarkerServer.HOLY_WORLD}, level = MarkerLevel.CRITICAL)
@FeatureInfo(
    name = "Test",
    description = "feature.impl.misc.test",
    displayName = "feature.impl.misc.test",
    category = Category.MISC,
    subcategory = Subcategory.MISC_CLIENT
)
public class Test extends Feature {
    private final KeybindSetting spoofEntityClickBind = new KeybindSetting("Спуффать клик по энтити");
    private final KeybindSetting printComponentsBind = new KeybindSetting("Вывести компоненты предмета");
    private final BooleanSetting collectFirstAuctionLot = new BooleanSetting("Собирать первый лот аукциона", false);
    private final BooleanSetting logAuctionAssortment = new BooleanSetting("Писать ассортимент аукциона", false);
    private double unusedValue;
    private int recordedEntityId = -1;
    private volatile boolean screenOpened;
    private boolean debugFlag = false;

    public static Test getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(Test.class);
    }

    @EventHandler
    public void onKey(ScreenMouseClickEvent screenMouseClickEvent) {
        if (this.spoofEntityClickBind.matchesKey(screenMouseClickEvent.getButton()) && screenMouseClickEvent.isPress() && mc.player != null) {
            PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket = PlayerInteractEntityC2SPacketAccessor.sadnes$interactAt(mc.player, false, Hand.MAIN_HAND, Vec3d.ZERO);
            PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket2 = PlayerInteractEntityC2SPacketAccessor.sadnes$interact(mc.player, false, Hand.MAIN_HAND);
            ((PlayerInteractEntityC2SPacketAccessor)playerInteractEntityC2SPacket).sadnes$setEntityId(this.recordedEntityId);
            ((PlayerInteractEntityC2SPacketAccessor)playerInteractEntityC2SPacket2).sadnes$setEntityId(this.recordedEntityId);
            PacketSender.sendSilent(playerInteractEntityC2SPacket);
            PacketSender.sendSilent(playerInteractEntityC2SPacket2);
            ChatUtil.print("Произвёл клик по сущности: " + this.recordedEntityId);
        }

        if (this.printComponentsBind.matchesKey(screenMouseClickEvent.getButton()) && screenMouseClickEvent.isPress() && mc.player.getMainHandStack().getItem() != Items.AIR) {
            ComponentMap componentMap = mc.player.getMainHandStack().getComponents();
            if (componentMap == null) {
                return;
            }

            ChatUtil.print(componentMap);
        }
    }

    @EventHandler
    public void onPacketBundle(SendMovementPacketsEvent sendMovementPacketsEvent) {
    }

    private int findHotbarItemSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack.getItem() == Items.CROSSBOW) {
                return i;
            }
        }

        return -1;
    }

    @EventHandler
    public void onMove(MoveEvent moveEvent) {
        ServerManager.isNullWorld();
    }

    @EventHandler
    public void onScreenPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && (this.collectFirstAuctionLot.getValue() || this.logAuctionAssortment.getValue())) {
            Packet packet = packetEvent.getPacket();
            if (packet instanceof InventoryS2CPacket && ((InventoryS2CPacket)packet).syncId() != 0 || packetEvent.getPacket() instanceof OpenScreenS2CPacket) {
                this.screenOpened = true;
                return;
            }
        }
    }

    @EventHandler
    public void onScreenUpdate(WorldTickEvent worldTickEvent) {
        if (this.screenOpened) {
            this.screenOpened = false;
            AutoBuyController autoBuyController = AutoBuyController.getInstance();
            if (autoBuyController == null || !autoBuyController.isRunning()) {
                if (mc.player != null && AuctionUtil.isAuctionOpen()) {
                    if (this.collectFirstAuctionLot.getValue()) {
                        ItemStack itemStack = mc.player.currentScreenHandler.getSlot(0).getStack();
                        String string = AuctionDumper.dumpLot(itemStack, mc.currentScreen.getTitle().getString());
                        if (string != null) {
                            ChatUtil.print("Лот записан: " + string);
                        }
                    }

                    if (this.logAuctionAssortment.getValue()) {
                        int i = AuctionMarketLog.scanContainer(mc.player.currentScreenHandler);
                        if (i > 0) {
                            ChatUtil.print("Аукцион: +" + i + " / " + AuctionMarketLog.getItemCount());
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteractPacket(PacketEvent packetEvent) {
        if (packetEvent.getPacket() instanceof PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
            final boolean[] booleans = new boolean[]{false};
            playerInteractEntityC2SPacket.handle(new Handler() {
                @Override
                public void interact(Hand hand) {
                }

                @Override
                public void interactAt(Hand hand2, Vec3d vec3d) {
                }

                @Override
                public void attack() {
                    booleans[0] = true;
                }
            });
            if (!booleans[0]) {
                this.recordedEntityId = ((PlayerInteractEntityC2SPacketAccessor)playerInteractEntityC2SPacket).sadnes$getEntityId();
                ChatUtil.print("Записал айди сущности: " + this.recordedEntityId);
            }
        }

        if (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
            gameMessageS2CPacket.content().getSiblings().forEach(text2 -> {
                if (text2.getStyle().getClickEvent() != null) {
                    ClickEvent clickEvent = text2.getStyle().getClickEvent();
                    clickEvent.getAction();
                }
            });
        }
    }

    @EventHandler
    public void onRotation(MoveInputEvent moveInputEvent) {
    }

    @EventHandler
    public void onEntityStatus(EntityRemoveEvent entityRemoveEvent) {
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
    }

    @EventHandler
    public void onInputPacket(PlayerInputPacketEvent playerInputPacketEvent) {
    }

    @EventHandler
    public void onSwing(UseItemEvent useItemEvent) {
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Sadnes.guiScale = 1.0F;
        AuctionMarketLog.flush();
    }

    public KeybindSetting getSpoofEntityClickBind() {
        return this.spoofEntityClickBind;
    }

    public KeybindSetting getPrintComponentsBind() {
        return this.printComponentsBind;
    }

    public BooleanSetting getCollectFirstAuctionLot() {
        return this.collectFirstAuctionLot;
    }

    public BooleanSetting getLogAuctionAssortment() {
        return this.logAuctionAssortment;
    }

    public double getUnusedValue() {
        return this.unusedValue;
    }

    public int getRecordedEntityId() {
        return this.recordedEntityId;
    }

    public boolean isScreenOpened() {
        return this.screenOpened;
    }

    public boolean isDebugFlag() {
        return this.debugFlag;
    }
}
