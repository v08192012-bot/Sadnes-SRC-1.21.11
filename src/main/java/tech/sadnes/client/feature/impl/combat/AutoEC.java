package tech.sadnes.client.feature.impl.combat;

import java.util.function.Predicate;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.combat.CrystalPlacementUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.player.AimPointUtil;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.rotation.RotationUtil;

@FeatureInfo(
    name = "AutoEC",
    description = "feature.impl.combat.autoec",
    displayName = "feature.impl.combat.autoec",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_INVENTORY
)
public final class AutoEC extends Feature {
    private final MultiSetting triggers = new MultiSetting("autoec.triggers", BooleanSetting.of("bind", true), BooleanSetting.of("totems", false))
        .requireAtLeastOne();
    private final KeybindSetting key = new KeybindSetting("autoec.key").setVisibleWhen(() -> this.triggers.has("bind"));
    private final NumberSetting totems = new NumberSetting("autoec.totems", 1.0F, 0.0F, 4.0F, 1.0F).setVisibleWhen(() -> this.triggers.has("totems"));
    private AutoEC.Stage stage = AutoEC.Stage.IDLE;
    private int stageTicks;
    private BlockHitResult placeHit;
    private BlockPos chestPos;
    private int chestSourceSlot = -1;
    private int hotbarSlot;
    private boolean totemTriggered;
    private boolean armorStripped;
    private int armorToStrip;
    private boolean awaitingPlacement;

    @Override
    public void onEnable() {
        super.onEnable();
        this.reset();
        this.totemTriggered = false;
    }

    @EventHandler
    public void onKey(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.isPress() && screenMouseClickEvent.getScreen() == null && this.triggers.has("bind")) {
            if (this.key.matchesKey(screenMouseClickEvent.getButton())) {
                this.start();
            }
        }
    }

    @EventHandler
    public void onMouse(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.isPress() && screenKeyEvent.getScreen() == null && this.triggers.has("bind")) {
            if (this.key.matchesKey(screenKeyEvent.getKey())) {
                this.start();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!ServerManager.isNullWorld() && mc.interactionManager != null) {
            if (this.triggers.has("totems") && InventoryUtil.countItem(Items.TOTEM_OF_UNDYING) <= this.totems.getValue()) {
                if (!this.totemTriggered && this.stage == AutoEC.Stage.IDLE && InventoryUtil.isInventoryOpen()) {
                    this.totemTriggered = true;
                    this.start();
                    return;
                }
            } else {
                this.totemTriggered = false;
            }

            if (this.stage != AutoEC.Stage.IDLE) {
                this.stageTicks++;
                switch (this.stage) {
                    case EQUIP:
                        this.tickEquip();
                        return;
                    case STRIP:
                        this.tickStrip();
                        return;
                    case OPEN:
                        this.tickOpen();
                        return;
                    default:
                        this.tickDeposit();
                }
            }
        } else {
            this.reset();
        }
    }

    private void start() {
        if (!ServerManager.isNullWorld() && this.stage == AutoEC.Stage.IDLE && InventoryUtil.isInventoryOpen() && this.hasItemsToStore()) {
            this.chestPos = this.findNearbyEnderChest();
            if (this.chestPos != null) {
                this.setStage(AutoEC.Stage.OPEN);
                this.tickOpen();
            } else {
                boolean flag = mc.player.getMainHandStack().isOf(Items.ENDER_CHEST);
                Slot slot = flag ? null : InventoryUtil.findPlayerSlot(Items.ENDER_CHEST);
                if (!flag && slot == null) {
                    ChatUtil.print(Language.translate("autoec.noChest", "AutoEC: нет эндер-сундука в инвентаре"));
                } else {
                    CrystalPlacementUtil.PlacePosition placePosition = CrystalPlacementUtil.findNearestPlacePos(Items.ENDER_CHEST, this::hasFreeSpaceAbove);
                    if (placePosition == null) {
                        ChatUtil.print(Language.translate("autoec.noSpot", "AutoEC: рядом некуда поставить эндер-сундук"));
                    } else {
                        this.chestPos = placePosition.pos();
                        this.placeHit = placePosition.hit();
                        this.chestSourceSlot = flag ? -1 : slot.id;
                        this.hotbarSlot = InventoryUtil.getSelectedSlot();
                        InventoryUtil.swapToHand(this.chestSourceSlot, Hand.MAIN_HAND);
                        this.setStage(AutoEC.Stage.EQUIP);
                        this.tickEquip();
                    }
                }
            }
        }
    }

    private BlockPos findNearbyEnderChest() {
        Vec3d vec3d = mc.player.getEyePos();
        BlockPos blockPos = BlockPos.ofFloored(vec3d);
        float f = (float)mc.player.getBlockInteractionRange();
        BlockPos blockPos2 = null;
        double d = 16.0;

        for (BlockPos blockPos3 : BlockPos.iterate(blockPos.add(-4, -4, -4), blockPos.add(4, 4, 4))) {
            double d2 = vec3d.squaredDistanceTo(Vec3d.ofCenter(blockPos3));
            boolean flag;
            if (d2 < d && mc.world.getBlockState(blockPos3).isOf(Blocks.ENDER_CHEST)) {
                flag = this.hasFreeSpaceAbove(blockPos3);
            } else {
                flag = false;
            }

            if (flag && AimPointUtil.getBlockHit(blockPos3, f, false) != null) {
                d = d2;
                blockPos2 = blockPos3.toImmutable();
            }
        }

        return blockPos2;
    }

    private boolean hasFreeSpaceAbove(BlockPos blockPos) {
        BlockPos blockPos2 = blockPos.up();
        return !mc.world.getBlockState(blockPos2).isSolidBlock(mc.world, blockPos2);
    }

    private void tickEquip() {
        boolean flag = mc.player.getMainHandStack().isOf(Items.ENDER_CHEST) && InventoryUtil.getSelectedSlot() == this.hotbarSlot;
        if (flag && !GuiMoveComponent.isActive()) {
            CrystalPlacementUtil.PlacePosition placePosition = this.stageTicks == 0
                ? null
                : CrystalPlacementUtil.findNearestPlacePos(Items.ENDER_CHEST, this::hasFreeSpaceAbove);
            if (this.stageTicks > 0 && placePosition == null) {
                this.abortAndRestoreSlot();
            } else {
                if (placePosition != null) {
                    this.chestPos = placePosition.pos();
                    this.placeHit = placePosition.hit();
                }

                this.rotateTo(this.placeHit.getPos());
                boolean flag2 = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, this.placeHit).isAccepted();
                if (flag2) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                }

                InventoryUtil.clickSwap(this.chestSourceSlot, this.hotbarSlot);
                this.chestSourceSlot = -1;
                if (!flag2) {
                    this.reset();
                } else {
                    this.awaitingPlacement = true;
                    this.setStage(AutoEC.Stage.OPEN);
                }
            }
        } else {
            if (this.stageTicks > 40) {
                this.abortAndRestoreSlot();
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && packetEvent.getPacket() instanceof BlockUpdateS2CPacket blockUpdateS2CPacket) {
            BlockPos blockPos = blockUpdateS2CPacket.getPos().toImmutable();
            boolean flag = blockUpdateS2CPacket.getState().isOf(Blocks.ENDER_CHEST);
            mc.execute(() -> {
                if (this.awaitingPlacement && blockPos.equals(this.chestPos)) {
                    if (flag) {
                        this.awaitingPlacement = false;
                    } else {
                        this.reset();
                    }
                }
            });
        }
    }

    private void tickStrip() {
        if (!GuiMoveComponent.isActive() && InventoryUtil.isInventoryOpen()) {
            PlayerInventory playerInventory = mc.player.getInventory();
            int i = 0;

            for (int i2 = 5; i2 <= 8 && this.armorToStrip > 0; i2++) {
                if (!mc.player.playerScreenHandler.getSlot(i2).getStack().isEmpty()) {
                    while (i < 9 && !playerInventory.getStack(i).isEmpty()) {
                        i++;
                    }

                    if (i == 9) {
                        this.setStage(AutoEC.Stage.OPEN);
                        this.tickOpen();
                        return;
                    }

                    InventoryUtil.clickSwap(i2, i++);
                    this.armorToStrip--;
                }
            }

            this.setStage(AutoEC.Stage.OPEN);
            this.tickOpen();
        } else {
            if (this.stageTicks > 40) {
                this.reset();
            }
        }
    }

    private void tickOpen() {
        boolean flag;
        if (!this.awaitingPlacement
            && mc.world.getBlockState(this.chestPos).isOf(Blocks.ENDER_CHEST)
            && !GuiMoveComponent.isActive()
            && !mc.player.shouldCancelInteraction()) {
            flag = InventoryUtil.isInventoryOpen();
        } else {
            flag = false;
        }

        BlockHitResult blockHitResult = flag ? AimPointUtil.getBlockHit(this.chestPos, (float)mc.player.getBlockInteractionRange(), false) : null;
        if (blockHitResult == null) {
            if (this.stageTicks > 100) {
                this.reset();
            }
        } else {
            this.rotateTo(blockHitResult.getPos());
            if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult).isAccepted()) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }

            this.setStage(AutoEC.Stage.AWAIT_WINDOW);
        }
    }

    private void tickDeposit() {
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
            int i = genericContainerScreenHandler.getRows() * 9;
            int i2 = genericContainerScreenHandler.slots.size();
            this.quickMoveMatching(genericContainerScreenHandler, i, itemStack -> itemStack.isOf(Items.RESPAWN_ANCHOR));
            this.quickMoveMatching(genericContainerScreenHandler, i, itemStack2 -> !itemStack2.isOf(Items.TOTEM_OF_UNDYING));
            this.quickMoveMatching(genericContainerScreenHandler, i, itemStack3 -> true);
            int i3 = mc.player.getOffHandStack().isEmpty() ? -1 : this.findEmptyChestSlot(genericContainerScreenHandler, i);
            if (i3 != -1) {
                mc.interactionManager.clickSlot(genericContainerScreenHandler.syncId, i3, 40, SlotActionType.SWAP, mc.player);
            }

            int i4 = 0;

            for (int i5 = i2 - 9; i5 < i2; i5++) {
                if (genericContainerScreenHandler.getSlot(i5).getStack().isEmpty()) {
                    i4++;
                }
            }

            this.armorToStrip = this.armorStripped ? 0 : Math.min(this.countWornArmor(), Math.min(this.countEmptyChestSlots(genericContainerScreenHandler, i), i4));
            mc.player.closeHandledScreen();
            if (this.armorToStrip == 0) {
                this.reset();
            } else {
                this.armorStripped = true;
                this.setStage(AutoEC.Stage.STRIP);
                this.tickStrip();
            }
        } else {
            if (this.stageTicks > 40) {
                this.reset();
            }
        }
    }

    private void quickMoveMatching(GenericContainerScreenHandler genericContainerScreenHandler, int i, Predicate<ItemStack> predicate) {
        int i2 = genericContainerScreenHandler.slots.size();

        for (int i3 = i; i3 < i2; i3++) {
            ItemStack itemStack = genericContainerScreenHandler.getSlot(i3).getStack();
            if (!itemStack.isEmpty() && predicate.test(itemStack) && this.chestCanAccept(genericContainerScreenHandler, i, itemStack)) {
                mc.interactionManager.clickSlot(genericContainerScreenHandler.syncId, i3, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
        }
    }

    private int findEmptyChestSlot(GenericContainerScreenHandler genericContainerScreenHandler, int i) {
        for (int i2 = 0; i2 < i; i2++) {
            if (genericContainerScreenHandler.getSlot(i2).getStack().isEmpty()) {
                return i2;
            }
        }

        return -1;
    }

    private int countEmptyChestSlots(GenericContainerScreenHandler genericContainerScreenHandler, int i) {
        int i2 = 0;

        for (int i3 = 0; i3 < i; i3++) {
            if (genericContainerScreenHandler.getSlot(i3).getStack().isEmpty()) {
                i2++;
            }
        }

        return i2;
    }

    private boolean hasItemsToStore() {
        if (this.countWornArmor() <= 0 && mc.player.getOffHandStack().isEmpty()) {
            PlayerInventory playerInventory = mc.player.getInventory();

            for (int i = 0; i < 36; i++) {
                ItemStack itemStack = playerInventory.getStack(i);
                if (!itemStack.isEmpty() && !itemStack.isOf(Items.ENDER_CHEST)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    private int countWornArmor() {
        int i = 0;

        for (int i2 = 5; i2 <= 8; i2++) {
            if (!mc.player.playerScreenHandler.getSlot(i2).getStack().isEmpty()) {
                i++;
            }
        }

        return i;
    }

    private boolean chestCanAccept(GenericContainerScreenHandler genericContainerScreenHandler, int i, ItemStack itemStack) {
        for (int i2 = 0; i2 < i; i2++) {
            ItemStack itemStack2 = genericContainerScreenHandler.getSlot(i2).getStack();
            boolean flag = itemStack2.isEmpty() || ItemStack.areItemsAndComponentsEqual(itemStack2, itemStack) && itemStack2.getCount() < itemStack2.getMaxCount();
            if (flag) {
                return true;
            }
        }

        return false;
    }

    private void rotateTo(Vec3d vec3d) {
        Vec2f vec2f = RotationUtil.getRotations(mc.player.getEyePos(), vec3d);
        RotationComponent.rotate(new Rotation(vec2f.x, MathHelper.clamp(vec2f.y, -90.0F, 90.0F)), 360.0F, 360.0F, 360.0F, 360.0F, 2, 990, false);
    }

    private void abortAndRestoreSlot() {
        if (!ServerManager.isNullWorld() && InventoryUtil.isInventoryOpen() && mc.player.getInventory().getStack(this.hotbarSlot).isOf(Items.ENDER_CHEST)) {
            InventoryUtil.clickSwap(this.chestSourceSlot, this.hotbarSlot);
            this.reset();
        } else {
            this.reset();
        }
    }

    private void setStage(AutoEC.Stage stage2) {
        this.stage = stage2;
        this.stageTicks = 0;
    }

    private void reset() {
        this.setStage(AutoEC.Stage.IDLE);
        this.placeHit = null;
        this.chestPos = null;
        this.chestSourceSlot = -1;
        this.armorStripped = false;
        this.awaitingPlacement = false;
    }

    enum Stage {
        IDLE,
        EQUIP,
        STRIP,
        OPEN,
        AWAIT_WINDOW;
    }
}
