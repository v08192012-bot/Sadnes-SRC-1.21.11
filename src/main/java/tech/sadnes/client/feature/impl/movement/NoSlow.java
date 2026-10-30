package tech.sadnes.client.feature.impl.movement;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.entity.data.DataTracker.SerializedEntry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.SlowdownEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.event.impl.UseItemEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.util.ActionSequence;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.compat.ProtocolUtil;
import tech.sadnes.client.util.network.PacketQueue;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "NoSlow",
    description = "feature.impl.movement.noslow",
    displayName = "feature.impl.movement.noslow",
    category = Category.MOVEMENT,
    subcategory = Subcategory.SPEED
)
public class NoSlow extends Feature {
    public final MultiSetting modes = new MultiSetting(
        "noslow.modes", BooleanSetting.of("food", true), BooleanSetting.of("sneak", false), BooleanSetting.of("crawl", false)
    );
    private final ModeSetting eatMode = new ModeSetting(
            "noslow.eatMode", new String[]{"none", "Grim v3", "Grim Swap", "Grim Metadata", "HolyWorld", "FunTime Арбалет", "FT Арбалет"}
        )
        .setVisibleWhen(() -> this.modes.has("food"));
    private final ModeSetting sneakMode = new ModeSetting("noslow.sneakin", new String[]{"none", "Polar 1.16"}).setVisibleWhen(() -> this.modes.has("sneak"));
    protected final BooleanSetting allowFall = new BooleanSetting("noslow.allowFall", false).setVisibleWhen(() -> this.modes.has("sneak"));
    ActionSequence unusedScheduler = new ActionSequence();
    private int grimTickCounter = 0;
    private final Queue<CommonPongC2SPacket> swapDelayedPongs = new ConcurrentLinkedQueue<>();
    private volatile int swapStage;
    private int swapStageTicks;
    private int swapOriginalSlot = -1;
    private boolean swapSentSwapHands;
    private boolean swapInternalUse;
    private final Queue<CommonPongC2SPacket> metadataDelayedPongs = new ConcurrentLinkedQueue<>();
    private volatile int metadataStage;
    private int metadataStageTicks;
    private boolean metadataMovementSent;
    private boolean metadataInternalAction;
    private volatile int metadataPingId = Integer.MIN_VALUE;
    boolean crossbowSwapped = false;
    boolean crossbowSwapping = false;
    int crossbowSlot = -1;
    ActionSequence crossbowScheduler = new ActionSequence();

    public static NoSlow getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(NoSlow.class);
    }

    public boolean shouldAllowFall() {
        return this.isEnabled() && this.allowFall.getValue() && this.modes.has("sneak");
    }

    @EventHandler
    public void onItemUseSlowdown(UseItemEvent useItemEvent) {
        if (this.modes.has("food")) {
            if (!this.eatMode.is("Grim Metadata")
                || mc.player.getActiveItem().getUseAction() != UseAction.BOW && mc.player.getActiveItem().getUseAction() != UseAction.CROSSBOW) {
                if (useItemEvent.getStage() == UseItemEvent.Stage.ON) {
                    if (this.eatMode.is("none")) {
                        useItemEvent.cancel();
                    }

                    if (this.eatMode.is("Grim v3")) {
                        if (mc.player.input.playerInput.forward()) {
                            mc.player.setSprinting(true);
                        }

                        this.grimTickCounter++;
                        if (this.grimTickCounter >= 2) {
                            useItemEvent.cancel();
                            this.grimTickCounter = 0;
                        }
                    } else if (this.eatMode.is("Grim Swap") && this.swapStage == 4) {
                        this.applySprint();
                        useItemEvent.cancel();
                    } else if (this.eatMode.is("Grim Metadata") && this.metadataStage == 7) {
                        this.applySprint();
                        useItemEvent.cancel();
                    } else if (this.eatMode.is("HolyWorld")) {
                        if (mc.player.getActiveHand() != Hand.MAIN_HAND) {
                            return;
                        }

                        mc.player.setSprinting(true);
                        if (mc.player.getItemUseTime() <= 1) {
                            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.DROP_ALL_ITEMS, new BlockPos(0, 0, 0), Direction.WEST));
                        }

                        useItemEvent.cancel();
                    } else if (this.eatMode.is("FunTime Арбалет")) {
                        if (mc.player.getStackInHand(mc.player.getActiveHand()).getItem() == Items.CROSSBOW) {
                            if (mc.player.input.playerInput.forward()) {
                                mc.player.setSprinting(true);
                            }

                            this.grimTickCounter++;
                            if (this.grimTickCounter >= 2) {
                                useItemEvent.cancel();
                                this.grimTickCounter = 0;
                            }
                        }
                    } else if (this.eatMode.is("FT Арбалет")) {
                        if (mc.player.getItemUseTime() <= 4 && mc.player.getOffHandStack().getItem() == Items.CROSSBOW) {
                            PacketSender.sendSequenced(i -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, i, mc.player.getYaw(), mc.player.getPitch()));
                        }

                        mc.player.setSprinting(true);
                        useItemEvent.cancel();
                    }
                }

                if (useItemEvent.getStage() == UseItemEvent.Stage.START) {
                    if (this.eatMode.is("Grim Metadata")) {
                        if (this.metadataInternalAction) {
                            return;
                        }

                        if (this.metadataStage != 0) {
                            useItemEvent.cancel();
                            return;
                        }

                        if (useItemEvent.getHand() != Hand.MAIN_HAND) {
                            return;
                        }

                        UseAction useAction = mc.player.getMainHandStack().getUseAction();
                        if (useAction != UseAction.EAT && useAction != UseAction.DRINK) {
                            return;
                        }

                        this.metadataStage = 1;
                        this.metadataStageTicks = 0;
                        this.metadataMovementSent = false;
                        return;
                    }

                    if (this.eatMode.is("Grim Swap")) {
                        if (this.swapInternalUse) {
                            return;
                        }

                        if (this.swapStage != 0) {
                            useItemEvent.cancel();
                            return;
                        }

                        if (useItemEvent.getHand() == Hand.MAIN_HAND
                            && (
                                mc.player.getOffHandStack().isEmpty()
                                    || mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
                                    || mc.player.getOffHandStack().isOf(Items.PLAYER_HEAD)
                            )) {
                            UseAction useAction2 = mc.player.getMainHandStack().getUseAction();
                            if (useAction2 != UseAction.EAT && useAction2 != UseAction.DRINK) {
                                return;
                            }

                            this.swapOriginalSlot = mc.player.getInventory().getSelectedSlot();
                            this.swapStageTicks = 0;
                            this.swapStage = 1;
                            useItemEvent.cancel();
                            return;
                        }

                        return;
                    }

                    if (!this.eatMode.is("FT Арбалет")) {
                        return;
                    }

                    if (this.crossbowSwapping) {
                        useItemEvent.cancel();
                        return;
                    }

                    if (useItemEvent.getHand() != Hand.MAIN_HAND || this.crossbowSwapped) {
                        return;
                    }

                    UseAction useAction3 = mc.player.getStackInHand(Hand.MAIN_HAND).getUseAction();
                    if (useAction3 != UseAction.EAT && useAction3 != UseAction.DRINK) {
                        return;
                    }

                    if (mc.player.getOffHandStack().getItem() == Items.CROSSBOW) {
                        return;
                    }

                    Slot slot = InventoryUtil.findMainInventorySlot(Items.CROSSBOW);
                    if (slot == null) {
                        ChatUtil.print(Language.translate("noslow.ft.nocrossbow", "Нет арбалета у тебя брат"));
                        return;
                    }

                    this.crossbowSlot = slot.id;
                    useItemEvent.cancel();
                }
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && packetEvent.getPacket() instanceof EntityTrackerUpdateS2CPacket entityTrackerUpdateS2CPacket && mc.player != null && entityTrackerUpdateS2CPacket.id() == mc.player.getId()) {
            for (SerializedEntry serializedEntry : entityTrackerUpdateS2CPacket.trackedValues()) {
                if (serializedEntry.id() == 8 && serializedEntry.value() instanceof Byte byteValue) {
                    boolean flag = (byteValue & 1) != 0;
                    if (this.metadataStage == 2 && !flag) {
                        this.metadataStage = 3;
                        this.metadataStageTicks = 0;
                    } else if (this.metadataStage == 5 && flag) {
                        this.metadataStage = 6;
                        this.metadataStageTicks = 0;
                        this.metadataPingId = Integer.MIN_VALUE;
                    }
                    break;
                }
            }
        }

        if (packetEvent.isReceive() && this.metadataStage == 6 && this.metadataPingId == Integer.MIN_VALUE && packetEvent.getPacket() instanceof CommonPingS2CPacket commonPingS2CPacket) {
            this.metadataPingId = commonPingS2CPacket.getParameter();
        }

        if (packetEvent.isSend() && packetEvent.getPacket() instanceof PlayerMoveC2SPacket && this.metadataStage >= 1 && this.metadataStage <= 4) {
            this.metadataMovementSent = true;
        }

        if (packetEvent.isSend() && packetEvent.getPacket() instanceof CommonPongC2SPacket commonPongC2SPacket) {
            if (this.metadataStage == 3) {
                this.metadataStage = 4;
                this.metadataStageTicks = 0;
            } else {
                if (this.metadataStage == 6) {
                    if (commonPongC2SPacket.getParameter() != this.metadataPingId) {
                        return;
                    }

                    this.metadataStage = 7;
                    this.metadataStageTicks = 0;
                    packetEvent.cancel();
                    this.metadataDelayedPongs.add(commonPongC2SPacket);
                    return;
                }

                if (this.metadataStage == 7) {
                    packetEvent.cancel();
                    this.metadataDelayedPongs.add(commonPongC2SPacket);
                    return;
                }
            }
        }

        if (packetEvent.isSend()) {
            Packet packet = packetEvent.getPacket();
            if (packet instanceof PlayerActionC2SPacket
                && ((PlayerActionC2SPacket)packet).getAction() == Action.RELEASE_USE_ITEM
                && this.metadataStage != 0
                && !this.metadataInternalAction) {
                this.resetGrimMetadata();
            }
        }

        if (packetEvent.isSend() && packetEvent.getPacket() instanceof CommonPongC2SPacket commonPongC2SPacket2 && this.swapStage != 0) {
            packetEvent.cancel();
            this.swapDelayedPongs.add(commonPongC2SPacket2);
            if (this.swapStage == 1) {
                this.swapStage = 2;
                this.swapStageTicks = 0;
                PacketQueue.queue(() -> {
                    if (this.swapStage == 2 && mc.player != null) {
                        this.swapSentSwapHands = true;
                        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                    }
                });
            }

            CommonPongC2SPacket commonPongC2SPacket3;
            if (this.swapStage == 0) {
                while ((commonPongC2SPacket3 = this.swapDelayedPongs.poll()) != null) {
                    PacketSender.sendSilent(commonPongC2SPacket3);
                }
            }
        } else if (packetEvent.isReceive() && this.swapStage == 2 && packetEvent.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket screenHandlerSlotUpdateS2CPacket && screenHandlerSlotUpdateS2CPacket.getSlot() == 45) {
            UseAction useAction = screenHandlerSlotUpdateS2CPacket.getStack().getUseAction();
            if (useAction == UseAction.EAT || useAction == UseAction.DRINK) {
                this.swapStage = 3;
                this.swapStageTicks = 0;
                return;
            }
        } else if (packetEvent.isSend()) {
            if (this.swapStage != 4) {
                return;
            }

            Packet packet2 = packetEvent.getPacket();
            if (packet2 instanceof PlayerActionC2SPacket && ((PlayerActionC2SPacket)packet2).getAction() == Action.RELEASE_USE_ITEM) {
                this.resetGrimSwap();
                return;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.metadataStage != 0) {
            if (!this.eatMode.is("Grim Metadata") || mc.player == null || mc.interactionManager == null) {
                this.resetGrimMetadata();
                return;
            }

            this.metadataStageTicks++;
            if (this.metadataStage < 7 && !mc.options.useKey.isPressed()) {
                this.resetGrimMetadata();
                return;
            }

            if (this.metadataStage == 1 && this.metadataMovementSent) {
                this.metadataStage = 2;
                this.metadataStageTicks = 0;
                this.metadataMovementSent = false;
                PacketQueue.queue(() -> {
                    if (this.metadataStage == 2 && mc.player != null) {
                        this.metadataInternalAction = true;

                        try {
                            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
                        } finally {
                            this.metadataInternalAction = false;
                        }
                    }
                });
            } else if (this.metadataStage == 4 && this.metadataMovementSent) {
                this.metadataInternalAction = true;

                try {
                    this.metadataPingId = Integer.MIN_VALUE;
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                } finally {
                    this.metadataInternalAction = false;
                }

                if (mc.player.isUsingItem()) {
                    this.metadataStage = 5;
                    this.metadataStageTicks = 0;
                }
            } else if (this.metadataStage >= 7 && !mc.player.isUsingItem()) {
                this.resetGrimMetadata();
                return;
            }

            if (this.metadataStageTicks > (this.metadataStage == 7 ? 80 : 40)) {
                this.resetGrimMetadata();
                return;
            }
        }

        if (this.swapStage != 0) {
            if (!this.eatMode.is("Grim Swap") || mc.player == null || mc.interactionManager == null) {
                this.resetGrimSwap();
                return;
            }

            this.swapStageTicks++;
            if (this.swapStage < 4 && !mc.options.useKey.isPressed()) {
                this.resetGrimSwap();
                return;
            }

            if (this.swapStage == 3) {
                UseAction useAction = mc.player.getOffHandStack().getUseAction();
                if (useAction == UseAction.EAT || useAction == UseAction.DRINK) {
                    this.swapInternalUse = true;

                    try {
                        mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                    } finally {
                        this.swapInternalUse = false;
                    }

                    if (mc.player.isUsingItem()) {
                        this.swapStage = 4;
                        this.swapStageTicks = 0;
                    }
                }
            } else if (this.swapStage == 4 && !mc.player.isUsingItem()) {
                this.resetGrimSwap();
                return;
            }

            if (this.swapStageTicks > (this.swapStage == 4 ? 80 : 40)) {
                this.resetGrimSwap();
                return;
            }
        }

        this.crossbowScheduler.tick();
        if (!this.crossbowSwapping) {
            if (this.crossbowSwapped && !mc.player.isUsingItem()) {
                this.swapCrossbowToOffhand(this.crossbowSlot, () -> {
                    this.crossbowSwapped = false;
                    this.crossbowSlot = -1;
                });
            } else {
                if (!this.crossbowSwapped && this.crossbowSlot != -1) {
                    this.crossbowSwapped = true;
                    this.swapCrossbowToOffhand(this.crossbowSlot, () -> mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND));
                }
            }
        }
    }

    private void swapCrossbowToOffhand(int i, Runnable runnable) {
        this.crossbowSwapping = true;
        GuiMoveComponent.forceActive = true;
        this.crossbowScheduler
            .clear()
            .addTicked(0, () -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 40, SlotActionType.SWAP, mc.player))
            .addTicked(0, () -> {
                GuiMoveComponent.forceActive = false;
                this.crossbowSwapping = false;
                runnable.run();
            });
    }

    private void resetGrimSwap() {
        this.swapStage = 0;
        this.swapStageTicks = 0;
        if (mc.getNetworkHandler() == null) {
            this.swapDelayedPongs.clear();
        } else {
            CommonPongC2SPacket commonPongC2SPacket;
            while ((commonPongC2SPacket = this.swapDelayedPongs.poll()) != null) {
                PacketSender.sendSilent(commonPongC2SPacket);
            }

            if (this.swapSentSwapHands && this.swapOriginalSlot >= 0) {
                InventoryUtil.swapBack(this.swapOriginalSlot);
            }
        }

        this.swapSentSwapHands = false;
        this.swapOriginalSlot = -1;
        this.swapInternalUse = false;
    }

    private void resetGrimMetadata() {
        this.metadataStage = 0;
        this.metadataStageTicks = 0;
        this.metadataMovementSent = false;
        this.metadataInternalAction = false;
        this.metadataPingId = Integer.MIN_VALUE;
        CommonPongC2SPacket commonPongC2SPacket;
        if (mc.getNetworkHandler() != null) {
            while ((commonPongC2SPacket = this.metadataDelayedPongs.poll()) != null) {
                PacketSender.sendSilent(commonPongC2SPacket);
            }
        } else {
            this.metadataDelayedPongs.clear();
        }
    }

    @Override
    public void onEnable() {
        this.resetGrimSwap();
        this.resetGrimMetadata();
        super.onEnable();
        this.crossbowScheduler.clear();
        GuiMoveComponent.forceActive = false;
        this.crossbowSwapped = false;
        this.crossbowSwapping = false;
        this.crossbowSlot = -1;
    }

    @EventHandler
    public void onSneakSlowdown(SlowdownEvent slowdownEvent) {
        if (this.modes.has("sneak") && mc.player.isSneaking()) {
            this.applySprint();
            if (this.sneakMode.is("none")) {
                slowdownEvent.setSneakSlowdown(false);
            }

            if (this.sneakMode.is("Polar 1.16") && ProtocolUtil.isOlderThan("1.17")) {
                mc.player
                    .networkHandler
                    .sendPacket(
                        new PlayerInputC2SPacket(
                            new PlayerInput(
                                mc.options.forwardKey.isPressed(),
                                mc.options.backKey.isPressed(),
                                mc.options.leftKey.isPressed(),
                                mc.options.rightKey.isPressed(),
                                mc.options.jumpKey.isPressed(),
                                true,
                                mc.player.isSprinting()
                            )
                        )
                    );
                mc.player
                    .networkHandler
                    .sendPacket(
                        new PlayerInputC2SPacket(
                            new PlayerInput(
                                mc.options.forwardKey.isPressed(),
                                mc.options.backKey.isPressed(),
                                mc.options.leftKey.isPressed(),
                                mc.options.rightKey.isPressed(),
                                mc.options.jumpKey.isPressed(),
                                false,
                                mc.player.isSprinting()
                            )
                        )
                    );
                slowdownEvent.setSneakSlowdown(false);
            }
        }

        if (this.modes.has("crawl") && mc.player.isCrawling()) {
            slowdownEvent.setCrawlSlowdown(false);
        }
    }

    public void applySprint() {
        if (!mc.player.isSneaking()
            && !mc.player.horizontalCollision
            && mc.player.getHungerManager().getFoodLevel() > 6
            && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) {
            mc.player.setSprinting(true);
        }
    }
}
