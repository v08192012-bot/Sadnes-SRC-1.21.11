package tech.sadnes.client.feature.impl.combat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import tech.sadnes.api.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import tech.sadnes.api.mixin.client.MinecraftClientAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.network.TickPacketOrderTracker;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "AutoTotem",
    description = "feature.impl.combat.autototem",
    displayName = "feature.impl.combat.autototem",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_INVENTORY
)
public class AutoTotem extends Feature {
    private final NumberSetting health = new NumberSetting("autototem.health", 4.0F, 2.0F, 20.0F, 1.0F);
    private final NumberSetting healthElytra = new NumberSetting("autototem.healthElytra", 4.0F, 4.0F, 20.0F, 1.0F);
    private final MultiSetting checks = new MultiSetting(
        "autototem.checks",
        BooleanSetting.of("golden_hearts", true),
        BooleanSetting.of("crystals", false),
        BooleanSetting.of("anchor", false),
        BooleanSetting.of("fall", true)
    );
    private final MultiSetting options = new MultiSetting(
        "autototem.options",
        BooleanSetting.of("swap_back", true),
        BooleanSetting.of("no_ball_switch", false),
        BooleanSetting.of("save_enchanted", true),
        BooleanSetting.of("check_cooldown", true),
        BooleanSetting.of("no_eat_switch", false)
    );
    private final BooleanSetting talisman = new BooleanSetting("autototem.talisman", false);
    private final ModeSetting talismanType = new ModeSetting("autototem.talismanType", new String[]{"Талисман Крушителя", "Талисман Карателя", "Талисман Ярости", "Талисман Раздора", "Талисман Демона", "Талисман Вихря", "Талисман Тирана", "Талисман Мрака"})
        .setVisibleWhen(this.talisman::getValue);
    private final GroupSetting mace = new GroupSetting("autototem.mace", true);
    private final NumberSetting distMace = new NumberSetting(this.mace, "autototem.distMace", 6.0F, 4.0F, 16.0F, 1.0F);
    private final NumberSetting distMaceY = new NumberSetting(this.mace, "autototem.distMaceY", 12.0F, 4.0F, 32.0F, 1.0F);
    private final NumberSetting hpMace = new NumberSetting(this.mace, "autototem.hpMace", 12.0F, 5.0F, 20.0F, 1.0F);
    private final BooleanSetting maceWalls = new BooleanSetting(this.mace, "autototem.maceWalls", true);
    private final BooleanSetting macePvp = new BooleanSetting(this.mace, "autototem.macePvp", false);
    private final GroupSetting tnt = new GroupSetting("autototem.tnt", false);
    private final NumberSetting distTNT = new NumberSetting(this.tnt, "autototem.distTNT", 10.0F, 4.0F, 128.0F, 1.0F);
    private final GroupSetting trident = new GroupSetting("autototem.trident", false);
    private final NumberSetting distTrident = new NumberSetting(this.trident, "autototem.distTrident", 10.0F, 4.0F, 128.0F, 1.0F);
    private final Map<UUID, Double> peakYByPlayer = new HashMap<>();
    private final Set<UUID> airbornePlayers = new HashSet<>();
    private int lastSourceSlot;
    private int swapBackSlot = -1;
    private int cancelMoveTicks;
    private long lastTotemPopTime;
    private long lastSwapTime;
    private boolean swapInProgress;

    public static AutoTotem getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(AutoTotem.class);
    }

    @EventHandler(priority = -500)
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (this.cancelMoveTicks > 0) {
            moveInputEvent.cancel();
            this.cancelMoveTicks--;
        }
    }

    @EventHandler(priority = 500)
    public void onPlayerUpdate(WorldTickEvent worldTickEvent) {
        this.swapInProgress = false;
        if (!ServerManager.isNullWorld()) {
            this.updatePeakHeights();
            this.update();
        }
    }

    public boolean isSwapping() {
        if (!this.isEnabled() || !ServerManager.isDonut() || ServerManager.isNullWorld()) {
            return false;
        } else {
            return this.swapInProgress ? true : this.isInDanger() && !this.isValidTotem(mc.player.getOffHandStack()) && this.findTotemSlot() != -1;
        }
    }

    @EventHandler(priority = 500)
    public void onInventoryUpdate(PlayerTickEvent playerTickEvent) {
        if (ServerManager.isDonut() && !ServerManager.isNullWorld()) {
            this.update();
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive()) {
            Packet packet = packetEvent.getPacket();
            EntityStatusS2CPacket entityStatusS2CPacket = packet instanceof EntityStatusS2CPacket entityStatusS2CPacket2 && entityStatusS2CPacket2.getStatus() == 35 ? entityStatusS2CPacket2 : null;
            if (entityStatusS2CPacket != null || packet instanceof HealthUpdateS2CPacket || packet instanceof ScreenHandlerSlotUpdateS2CPacket || packet instanceof InventoryS2CPacket
                )
             {
                mc.execute(() -> {
                    if (this.isEnabled() && !ServerManager.isNullWorld()) {
                        if (entityStatusS2CPacket != null && entityStatusS2CPacket.getEntity(mc.world) == mc.player) {
                            this.lastTotemPopTime = System.currentTimeMillis();
                        }

                        this.update();
                    }
                });
            }
        }
    }

    private void update() {
        if (ServerManager.isDonut() || System.currentTimeMillis() - this.lastSwapTime >= 300L) {
            boolean flag = this.isInDanger();
            boolean flag2;
            if (ServerManager.isDonut() && flag && !this.isValidTotem(mc.player.getOffHandStack())) {
                flag2 = this.isValidTotem(mc.player.getMainHandStack());
            } else {
                flag2 = false;
            }

            if (flag2 && this.swapHands()) {
                this.lastSwapTime = System.currentTimeMillis();
            } else if (flag && !this.isHoldingTotem() && this.equipTotem()) {
                this.lastSwapTime = System.currentTimeMillis();
            } else {
                if (!flag && this.swapBackSlot != -1) {
                    if (FunTimeHitSwapHandler.isActive()) {
                        return;
                    }

                    ItemStack itemStack = mc.player.getOffHandStack();
                    boolean flag3;
                    if (!itemStack.isEmpty()) {
                        flag3 = itemStack.isOf(Items.TOTEM_OF_UNDYING);
                    } else {
                        flag3 = true;
                    }

                    if (this.options.has("swap_back") && flag3) {
                        if (!mc.player.isUsingItem() && this.swapSlotToOffhand(this.swapBackSlot)) {
                            this.swapBackSlot = -1;
                            return;
                        }
                    } else {
                        this.swapBackSlot = -1;
                    }
                }

                if (ServerManager.isDonut()) {
                    this.refillHotbarTotem();
                }
            }
        }
    }

    private void refillHotbarTotem() {
        if (!mc.player.getInventory().getStack(8).isOf(Items.TOTEM_OF_UNDYING)) {
            int i = this.findInventoryTotem();
            if (i != -1 && !mc.player.isUsingItem() && !GuiMoveComponent.isActive()) {
                int i2 = InventoryUtil.findHotbarIndex(Items.TOTEM_OF_UNDYING);
                if (i2 == -1 || !InventoryUtil.isMovementActive()) {
                    if (this.moveToHotbarSlot(i)) {
                        if (this.lastSourceSlot == 8) {
                            this.lastSourceSlot = i;
                        }
                    }
                }
            }
        }
    }

    private boolean moveToHotbarSlot(int i) {
        if (!InventoryUtil.isInventoryOpen()) {
            return false;
        }

        this.swapInProgress = true;
        if (InventoryUtil.isMovementActive()) {
            this.cancelMoveTicks = 1;
            return false;
        }

        InventoryUtil.clickSlot(i, 8, SlotActionType.SWAP);
        GuiMoveComponent.sendFakeClick();
        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
        if (this.swapBackSlot == 44) {
            this.swapBackSlot = i;
        }

        return true;
    }

    private int findInventoryTotem() {
        PlayerInventory playerInventory = mc.player.getInventory();
        boolean flag = this.options.has("save_enchanted");
        int i = -1;

        for (int i2 = 9; i2 < 36; i2++) {
            ItemStack itemStack = playerInventory.getStack(i2);
            if (itemStack.isOf(Items.TOTEM_OF_UNDYING)) {
                if (!flag || !itemStack.hasGlint()) {
                    return i2;
                }

                if (i == -1) {
                    i = i2;
                }
            }
        }

        return i;
    }

    private boolean equipTotem() {
        if (this.options.has("check_cooldown") && this.isTotemOnCooldown()) {
            return false;
        }

        int i = this.findTotemSlot();
        if (i == -1) {
            return false;
        }

        boolean flag;
        if (this.options.has("no_eat_switch")) {
            flag = mc.player.isUsingItem();
        } else {
            flag = false;
        }

        if (flag) {
            return false;
        }

        boolean flag2 = !mc.player.getOffHandStack().isEmpty();
        if (!this.swapSlotToOffhand(i)) {
            return false;
        }

        if (mc.player.isUsingItem()) {
            mc.interactionManager.stopUsingItem(mc.player);
            ((MinecraftClientAccessor)mc).sadnes$setItemUseCooldown(2);
        }

        boolean flag3;
        if (this.options.has("swap_back") && this.swapBackSlot == -1) {
            flag3 = flag2;
        } else {
            flag3 = false;
        }

        if (flag3) {
            this.swapBackSlot = this.lastSourceSlot < 9 ? this.lastSourceSlot + 36 : this.lastSourceSlot;
        }

        if (ServerManager.isDonut() && this.lastSourceSlot == 8) {
            this.refillHotbarTotem();
        }

        return true;
    }

    private boolean swapHands() {
        this.swapInProgress = true;
        if (!TickPacketOrderTracker.isTickClean()) {
            return false;
        }

        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        ItemStack itemStack = mc.player.getMainHandStack();
        mc.player.setStackInHand(Hand.MAIN_HAND, mc.player.getOffHandStack());
        mc.player.setStackInHand(Hand.OFF_HAND, itemStack);
        return true;
    }

    private boolean swapSlotToOffhand(int i) {
        boolean flag = ServerManager.isDonut();
        if (flag) {
            this.swapInProgress = true;
        }

        if (flag && !TickPacketOrderTracker.isTickClean()) {
            return false;
        }

        int i2 = i >= 36 ? i - 36 : i;
        int i3 = -1;
        if (!flag) {
            InventoryUtil.swapToHand(i, Hand.OFF_HAND);
            if (i >= 36 && i <= 44) {
                i3 = i2;
            }
        } else if (i2 < 9) {
            this.sendOffhandSwap(i2);
            i3 = i2;
        } else {
            if (!this.moveToHotbarSlot(i)) {
                return false;
            }

            this.sendOffhandSwap(8);
            i3 = 8;
        }

        if (i3 != -1) {
            PlayerInventory playerInventory = mc.player.getInventory();
            ItemStack itemStack = playerInventory.getStack(i3);
            playerInventory.setStack(i3, mc.player.getOffHandStack());
            playerInventory.setStack(40, itemStack);
        }

        if (flag && i2 >= 9) {
            this.lastSourceSlot = 8;
            return true;
        } else {
            this.lastSourceSlot = i2;
            return true;
        }
    }

    private void sendOffhandSwap(int i) {
        ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot();
        int i2 = InventoryUtil.getSelectedSlot();
        if (i != i2) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
        }

        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        if (i != i2) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i2));
        }
    }

    private int findTotemSlot() {
        int i = this.talisman.getValue() ? this.findTalismanSlot() : -1;
        return i != -1 ? i : InventoryUtil.findItemSlotId(Items.TOTEM_OF_UNDYING, this.options.has("save_enchanted"), true);
    }

    public boolean isInDanger() {
        if (ServerManager.isNullWorld()) {
            return false;
        }

        boolean flag = mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
        boolean flag2 = this.checks.has("golden_hearts") && System.currentTimeMillis() - this.lastTotemPopTime > 5000L;
        float f = mc.player.getHealth() + (flag2 ? mc.player.getAbsorptionAmount() : 0.0F);
        if (f <= (flag ? this.healthElytra.getValue() : this.health.getValue())) {
            return true;
        }

        boolean flag3 = this.checks.has("fall") && mc.player.fallDistance > 5.0;
        boolean flag4;
        if (this.options.has("no_ball_switch") && !flag3) {
            flag4 = mc.player.getOffHandStack().isOf(Items.PLAYER_HEAD);
        } else {
            flag4 = false;
        }

        return flag4 ? false : this.isFallDangerous() || this.isMaceThreat() || this.isEntityThreat() || this.isAnchorNearby();
    }

    private boolean isFallDangerous() {
        if (!this.checks.has("fall")) {
            return false;
        } else if (mc.player.fallDistance <= 10.0) {
            return false;
        } else {
            return mc.player.isGliding() || mc.player.isTouchingWater() || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                ? false
                : !this.hasSafeLanding();
        }
    }

    private boolean hasSafeLanding() {
        Mutable mutable = mc.player.getBlockPos().mutableCopy();
        int i = mc.world.getBottomY();

        while (mutable.getY() > i) {
            mutable.move(Direction.DOWN);
            BlockState blockState = mc.world.getBlockState(mutable);
            if (!blockState.getFluidState().isEmpty()) {
                return true;
            }

            if (blockState.isOf(Blocks.SLIME_BLOCK)) {
                if (!mc.player.isSneaking()) {
                    return true;
                }

                return false;
            }

            if (blockState.isOf(Blocks.POWDER_SNOW) || blockState.isOf(Blocks.COBWEB) || blockState.isOf(Blocks.SWEET_BERRY_BUSH) || blockState.isOf(Blocks.SCAFFOLDING)) {
                return true;
            }

            if (!blockState.getCollisionShape(mc.world, mutable).isEmpty()) {
                return false;
            }
        }

        return false;
    }

    private boolean isMaceThreat() {
        if (!this.mace.isEnabled()) {
            return false;
        }

        if (this.macePvp.getValue() && !ServerManager.isInPvp()) {
            return false;
        }

        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() > this.hpMace.getValue()) {
            return false;
        }

        float f = this.distMace.getValue();
        float f2 = this.distMaceY.getValue();

        for (PlayerEntity playerEntity : mc.world.getPlayers()) {
            if (playerEntity != mc.player
                && !Sadnes.getInstance().getFriendManager().isFriend(playerEntity.getGameProfile().name())
                && playerEntity.getMainHandStack().isOf(Items.MACE)
                && !(this.getFallDistance(playerEntity) <= 0.5)) {
                double d = playerEntity.getX() - mc.player.getX();
                double d2 = playerEntity.getZ() - mc.player.getZ();
                if (!(d * d + d2 * d2 > f * f)
                    && !(Math.abs(playerEntity.getY() - mc.player.getY()) > f2)
                    && (this.maceWalls.getValue() || this.canSee(playerEntity))) {
                    return true;
                }
            }
        }

        return false;
    }

    private void updatePeakHeights() {
        if (!this.mace.isEnabled()) {
            if (!this.peakYByPlayer.isEmpty()) {
                this.peakYByPlayer.clear();
            }
        } else {
            this.airbornePlayers.clear();

            for (PlayerEntity playerEntity : mc.world.getPlayers()) {
                if (playerEntity != mc.player && !playerEntity.isOnGround() && !playerEntity.isTouchingWater() && !playerEntity.isClimbing()) {
                    UUID uuid = playerEntity.getUuid();
                    this.airbornePlayers.add(uuid);
                    this.peakYByPlayer.merge(uuid, playerEntity.getY(), Math::max);
                }
            }

            this.peakYByPlayer.keySet().retainAll(this.airbornePlayers);
        }
    }

    private double getFallDistance(PlayerEntity playerEntity) {
        return this.peakYByPlayer.getOrDefault(playerEntity.getUuid(), playerEntity.getY()) - playerEntity.getY();
    }

    private boolean canSee(PlayerEntity playerEntity) {
        BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), playerEntity.getEyePos(), ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
        return blockHitResult.getType() == Type.MISS;
    }

    private boolean isEntityThreat() {
        boolean flag = this.checks.has("crystals");
        boolean flag2 = this.tnt.isEnabled();
        boolean flag3 = this.trident.isEnabled();
        if (!flag && !flag2 && !flag3) {
            return false;
        }

        float f = this.distTNT.getValue();
        float f2 = this.distTrident.getValue();

        for (Entity entity : mc.world.getEntities()) {
            if (flag && entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) <= 6.0F) {
                return true;
            }

            if (flag2 && this.isPrimedExplosive(entity) && mc.player.distanceTo(entity) <= f) {
                return true;
            }

            if (flag3 && entity instanceof TridentEntity tridentEntity && !tridentEntity.noClip && !this.isFriendlyTrident(tridentEntity) && mc.player.distanceTo(tridentEntity) <= f2) {
                return true;
            }
        }

        return false;
    }

    private boolean isPrimedExplosive(Entity entity) {
        return entity instanceof TntEntity ? ((TntEntity)entity).getFuse() > 0 : entity instanceof TntMinecartEntity && ((TntMinecartEntity)entity).isPrimed();
    }

    private boolean isFriendlyTrident(TridentEntity tridentEntity) {
        Entity entity = tridentEntity.getOwner();
        if (entity == null) {
            return false;
        } else {
            return entity == mc.player
                ? true
                : entity instanceof PlayerEntity playerEntity && Sadnes.getInstance().getFriendManager().isFriend(playerEntity.getGameProfile().name());
        }
    }

    private boolean isAnchorNearby() {
        if (!this.checks.has("anchor")) {
            return false;
        }

        BlockPos blockPos = mc.player.getBlockPos();

        for (BlockPos blockPos2 : BlockPos.iterate(blockPos.add(-6, -6, -6), blockPos.add(6, 6, 6))) {
            if (mc.world.getBlockState(blockPos2).isOf(Blocks.RESPAWN_ANCHOR)) {
                return true;
            }
        }

        return false;
    }

    private boolean isHoldingTotem() {
        return this.isValidTotem(mc.player.getOffHandStack()) || this.isValidTotem(mc.player.getMainHandStack());
    }

    private boolean isValidTotem(ItemStack itemStack) {
        if (!itemStack.isOf(Items.TOTEM_OF_UNDYING)) {
            return false;
        }

        if (this.talisman.getValue()) {
            if (this.isTalisman(itemStack)) {
                return true;
            }

            if (this.findTalismanSlot() != -1) {
                return false;
            }
        }

        boolean flag;
        if (this.options.has("save_enchanted")) {
            flag = itemStack.hasGlint();
        } else {
            flag = false;
        }

        return !flag || InventoryUtil.findItemSlotId(Items.TOTEM_OF_UNDYING, true, false) == -1;
    }

    private int findTalismanSlot() {
        PlayerInventory playerInventory = mc.player.getInventory();

        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = playerInventory.getStack(i);
            if (itemStack.isOf(Items.TOTEM_OF_UNDYING) && this.isTalisman(itemStack)) {
                if (i < 9) {
                    return i + 36;
                }

                return i;
            }
        }

        return -1;
    }

    private boolean isTalisman(ItemStack itemStack) {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager == null) {
            return false;
        }

        AutoBuyItem autoBuyItem = autoBuyManager.findMatch(itemStack);
        return autoBuyItem != null && autoBuyItem.getId().equals(this.talismanType.getValue());
    }

    private boolean isTotemOnCooldown() {
        int i = mc.player.getInventory().size();

        for (int i2 = 0; i2 < i; i2++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i2);
            if (itemStack.isOf(Items.TOTEM_OF_UNDYING) && mc.player.getItemCooldownManager().isCoolingDown(itemStack)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void toggle() {
        super.toggle();
        this.swapBackSlot = -1;
        this.cancelMoveTicks = 0;
        this.lastTotemPopTime = 0L;
        this.lastSwapTime = 0L;
        this.swapInProgress = false;
        this.peakYByPlayer.clear();
    }

    public NumberSetting getHealth() {
        return this.health;
    }

    public NumberSetting getHealthElytra() {
        return this.healthElytra;
    }

    public MultiSetting getChecks() {
        return this.checks;
    }

    public MultiSetting getOptions() {
        return this.options;
    }

    public BooleanSetting getTalisman() {
        return this.talisman;
    }

    public ModeSetting getTalismanType() {
        return this.talismanType;
    }

    public GroupSetting getMace() {
        return this.mace;
    }

    public NumberSetting getDistMace() {
        return this.distMace;
    }

    public NumberSetting getDistMaceY() {
        return this.distMaceY;
    }

    public NumberSetting getHpMace() {
        return this.hpMace;
    }

    public BooleanSetting getMaceWalls() {
        return this.maceWalls;
    }

    public BooleanSetting getMacePvp() {
        return this.macePvp;
    }

    public GroupSetting getTnt() {
        return this.tnt;
    }

    public NumberSetting getDistTNT() {
        return this.distTNT;
    }

    public GroupSetting getTrident() {
        return this.trident;
    }

    public NumberSetting getDistTrident() {
        return this.distTrident;
    }

    public Map<UUID, Double> getPeakYByPlayer() {
        return this.peakYByPlayer;
    }

    public Set<UUID> getAirbornePlayers() {
        return this.airbornePlayers;
    }

    public int getLastSourceSlot() {
        return this.lastSourceSlot;
    }

    public int getSwapBackSlot() {
        return this.swapBackSlot;
    }

    public int getCancelMoveTicks() {
        return this.cancelMoveTicks;
    }

    public long getLastTotemPopTime() {
        return this.lastTotemPopTime;
    }

    public long getLastSwapTime() {
        return this.lastSwapTime;
    }

    public boolean isSwapInProgress() {
        return this.swapInProgress;
    }
}
