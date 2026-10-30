package tech.sadnes.client.feature.impl.movement;

import java.util.Optional;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import tech.sadnes.api.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import tech.sadnes.api.mixin.accessor.LivingEntityAccessor;
import tech.sadnes.api.mixin.client.MinecraftClientAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.SendMovementPacketsEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.TimerUtil;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.MoveUtil;

@FeatureInfo(
    name = "Speed",
    description = "feature.impl.movement.speed",
    displayName = "feature.impl.movement.speed",
    category = Category.MOVEMENT,
    subcategory = Subcategory.SPEED
)
public final class Speed extends Feature {
    private final ModeSetting mode = new ModeSetting("speed.mode", new String[]{"ncp", "matrix_jb", "grim_entity", "grim_entity2", "FunTime Elytra", "spear_boost"});
    private final BooleanSetting useTimer = new BooleanSetting("speed.useTimer", false)
        .setVisibleWhen(() -> !this.mode.is("ncp") && !this.mode.is("matrix_jb") ? false : true);
    private final NumberSetting timerSpeed = new NumberSetting("speed.timerSpeed", 1.088F, 1.0F, 2.0F, 0.001F, Unit.EMPTY)
        .setVisibleWhen(() -> this.mode.is("matrix_jb") && this.useTimer.getValue() ? true : false);
    private final BooleanSetting armorStands = new BooleanSetting("speed.armorStands", false).setVisibleWhen(() -> this.mode.is("grim_entity2"));
    private final MultiSetting pause = new MultiSetting("speed.pause", BooleanSetting.of("liquids", false), BooleanSetting.of("sneaking", false))
        .setVisibleWhen(() -> !this.mode.is("FunTime Elytra") ? true : false);
    private final BooleanSetting speedboost = (BooleanSetting)new BooleanSetting("speed.speedboost", false)
        .setVisibleWhen(() -> !this.mode.is("FunTime Elytra") && !this.mode.is("spear_boost") ? false : true)
        .setDisplayName("Ускорение");
    private double moveSpeed;
    private int ncpStage;
    private int ncpTickCounter;
    private float lastForwardInput;
    private long lastLagbackTime;
    boolean elytraSneakTriggered = false;
    TimerUtil elytraTimer = new TimerUtil();

    @Override
    public void onDisable() {
        super.onDisable();
        this.ncpStage = 1;
        this.ncpTickCounter = 0;
        this.moveSpeed = 0.2873;
        this.lastForwardInput = 0.0F;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Sadnes.guiScale = 1.0F;
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && packetEvent.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.lastLagbackTime = System.currentTimeMillis();
        }
    }

    private void applyBoostRotation() {
        if (mc.player.input.hasForwardMovement() && mc.player.input.playerInput.jump()) {
            RotationComponent.rotate(new Rotation(MathHelper.wrapDegrees(Rotation.getCameraYaw()), 60.0F), 999.0F, 999.0F, 999.0F, 999.0F, 2, 7, false);
        }
    }

    public static void stopFallFlyingIfNeeded() {
        if (mc.player != null && mc.player.isGliding()) {
            if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                Speed speed = Sadnes.getInstance().getFeatureManager().getFeature(Speed.class);
                WaterSpeed waterSpeed = WaterSpeed.getInstance();
                if (speed != null && speed.isEnabled() && speed.mode.is("FunTime Elytra")) {
                    mc.player.stopGliding();
                } else if (waterSpeed != null && waterSpeed.isEnabled()) {
                    mc.player.stopGliding();
                }
            }
        }
    }

    public static boolean shouldPauseElytra() {
        if (mc.player == null) {
            return false;
        }

        boolean flag;
        if (!mc.player.isUsingItem() && (mc.player.getMainHandStack().getUseAction() == UseAction.NONE || !mc.options.useKey.isPressed())) {
            if (mc.player.getOffHandStack().getUseAction() != UseAction.NONE) {
                boolean flag2 = mc.options.useKey.isPressed();
                return findElytraSlot() == -1
                    || flag2
                    || mc.player.isOnGround() && !mc.player.input.playerInput.jump()
                    || !ElytraRecast.getInstance().canFitPose(EntityPose.STANDING, mc.player.getEntityPos());
            }

            flag = false;
        } else {
            flag = true;
        }

        boolean flag3 = flag;
        return findElytraSlot() == -1
            || flag3
            || mc.player.isOnGround() && !mc.player.input.playerInput.jump()
            || !ElytraRecast.getInstance().canFitPose(EntityPose.STANDING, mc.player.getEntityPos());
    }

    public static int findElytraSlot() {
        if (mc.player == null) {
            return -1;
        }

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.ELYTRA)) {
                return i;
            }
        }

        return -1;
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.mode.is("FunTime Elytra")) {
            if (mc.player.isUsingItem()) {
                return;
            }

            if (shouldPauseElytra()) {
                if (this.elytraSneakTriggered) {
                    this.elytraTimer.setDelay(100L);
                }
            } else {
                int i = findElytraSlot();
                if (i == -1) {
                    if (mc.player.age % 40 == 0) {
                        ChatUtil.print("Для этого режима " + this.getName() + " нужны элитры в хотбаре!");
                        return;
                    }
                } else {
                    if (this.speedboost.getValue()) {
                        this.applyBoostRotation();
                    }

                    if (mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                        return;
                    }

                    if (!shouldPauseElytra() && mc.player.isSneaking()) {
                        this.elytraSneakTriggered = true;
                    }

                    if (!mc.world.getBlockState(mc.player.getBlockPos().down(3)).isAir() && mc.player.input.playerInput.sneak()) {
                        this.elytraTimer.setDelay(200L);
                    }

                    int i2 = mc.player.getInventory().getSelectedSlot();
                    if (mc.player.getInventory().getSelectedSlot() != i) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
                    }

                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                    PacketSender.sendSequenced(i3 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, i3, mc.player.getYaw(), mc.player.getPitch()));
                    mc.player.setSprinting(true);
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
                    mc.player.startGliding();
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                    if (mc.player.getInventory().getSelectedSlot() != i) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i2));
                        return;
                    }
                }
            }
        } else if (this.mode.is("spear_boost")) {
            if (!ServerManager.isNullWorld() && mc.world != null) {
                if (mc.interactionManager == null) {
                    return;
                }

                if (!mc.player.isOnGround()) {
                    if (mc.player.isTouchingWater()) {
                        return;
                    }

                    Optional optional = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(Enchantments.LUNGE);
                    if (optional.isEmpty()) {
                        return;
                    }

                    RegistryEntry registryEntry = (RegistryEntry)optional.get();
                    PlayerInventory playerInventory = mc.player.getInventory();
                    int i4 = -1;

                    for (int i5 = 0; i5 < 9; i5++) {
                        ItemStack itemStack = playerInventory.getStack(i5);
                        if (!itemStack.isEmpty() && itemStack.isIn(ItemTags.SPEARS) && EnchantmentHelper.getLevel(registryEntry, itemStack) > 0) {
                            i4 = i5;
                            break;
                        }
                    }

                    if (i4 == -1) {
                        if (mc.player.age % 30 == 0) {
                            NotificationUtil.notify(
                                NotificationType.WARNING,
                                this.getDisplayName(),
                                Items.IRON_SPEAR.getDefaultStack(),
                                Language.translate("speed.nospear", "Копья с рывком нет в хотбаре!")
                            );
                            return;
                        }
                    } else {
                        int i6 = playerInventory.getSelectedSlot();
                        if (i6 != i4) {
                            playerInventory.setSelectedSlot(i4);
                        }

                        if (this.speedboost.getValue()) {
                            RotationComponent.rotate(
                                new Rotation(MathHelper.wrapDegrees(Rotation.getCameraYaw()), 0.0F), 999.0F, 999.0F, 999.0F, 999.0F, 2, 7, false
                            );
                        }

                        ((MinecraftClientAccessor)mc).sadnes$invokeDoAttack();
                        if (i6 != i4) {
                            playerInventory.setSelectedSlot(i6);
                            ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot();
                            return;
                        }
                    }
                }
            }
        } else if (!ServerManager.isNullWorld() && this.mode.is("matrix_jb")) {
            if (this.isPaused()) {
                return;
            }

            boolean flag = false;

            for (VoxelShape voxelShape : mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(0.5, 0.0, 0.5).offset(0.0, -1.0, 0.0))) {
                if (voxelShape != VoxelShapes.empty()) {
                    flag = true;
                    break;
                }
            }

            if (MoveUtil.isMoving() && flag && mc.player.fallDistance <= 0.0) {
                Sadnes.guiScale = 1.0F;
                mc.player.setOnGround(true);
                mc.player.jump();
                return;
            }

            if (mc.player.fallDistance > 0.0 && this.useTimer.getValue()) {
                Sadnes.guiScale = this.timerSpeed.getValue();
                mc.player.addVelocity(0.0, -0.003, 0.0);
                return;
            }
        }
    }

    @EventHandler
    public void onElytraCheck(MoveInputEvent moveInputEvent) {
        if (!this.elytraTimer.isReached()) {
            moveInputEvent.setSneak(false);
        }
    }

    @EventHandler
    public void onMove(MoveEvent moveEvent) {
        if (mc.player != null && this.mode.is("ncp") && !this.isPaused() && !moveEvent.isCancelled()) {
            if (!mc.player.getAbilities().flying && !mc.player.isGliding()) {
                if (mc.player.getHungerManager().getFoodLevel() > 6) {
                    moveEvent.setCancelled(true);
                    if (!MoveUtil.isMoving()) {
                        Sadnes.guiScale = 1.0F;
                        moveEvent.setX(0.0);
                        moveEvent.setZ(0.0);
                    } else {
                        Sadnes.guiScale = this.useTimer.getValue() ? 1.088F : 1.0F;
                        float f = mc.player.input.getMovementInput().y;
                        double d = MoveUtil.getDeltaSpeed();
                        double d2 = f <= 0.0F && this.lastForwardInput > 0.0F ? d * 0.66 : d;
                        if (this.ncpStage == 1 && mc.player.isOnGround() && !mc.player.horizontalCollision) {
                            double d3 = ((LivingEntityAccessor)mc.player).sadnes$invokeGetJumpVelocity();
                            mc.player.setVelocity(mc.player.getVelocity().x, d3, mc.player.getVelocity().z);
                            moveEvent.setY(d3);
                            this.moveSpeed *= 2.149;
                            this.ncpStage = 2;
                        } else if (this.ncpStage == 2) {
                            this.moveSpeed = d2 - 0.66 * (d2 - this.getBaseSpeed());
                            this.ncpStage = 3;
                        } else {
                            boolean flag = mc.world
                                .getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0))
                                .iterator()
                                .hasNext();
                            if (flag || mc.player.verticalCollision) {
                                this.ncpStage = 1;
                            }

                            this.moveSpeed = d2 - d2 / 159.0;
                        }

                        this.moveSpeed = Math.max(this.moveSpeed, this.getBaseSpeed());
                        double d4 = f < 1.0F ? 0.465 : 0.576;
                        double d5 = f < 1.0F ? 0.44 : 0.57;
                        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                            double d6 = 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
                            d4 *= d6;
                            d5 *= d6;
                        }

                        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                            double d7 = 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);
                            d4 /= d7;
                            d5 /= d7;
                        }

                        this.moveSpeed = Math.min(this.moveSpeed, this.ncpTickCounter > 25 ? d4 : d5);
                        if (this.ncpTickCounter++ > 50) {
                            this.ncpTickCounter = 0;
                        }

                        double[] doubles = MoveUtil.getMotionForInput(this.moveSpeed);
                        moveEvent.setX(doubles[0]);
                        moveEvent.setZ(doubles[1]);
                        this.lastForwardInput = f;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTravel(SendMovementPacketsEvent sendMovementPacketsEvent) {
        if (mc.player != null && !this.isPaused() && System.currentTimeMillis() - this.lastLagbackTime >= 1000L) {
            if (this.mode.is("grim_entity")) {
                for (PlayerEntity playerEntity : mc.world.getPlayers()) {
                    if (playerEntity != mc.player && !(mc.player.squaredDistanceTo(playerEntity) > 2.25)) {
                        float f = mc.world.getBlockState(this.getBlockBelow()).getBlock().getSlipperiness();
                        float f2 = mc.player.isOnGround() ? f * 0.91F : 0.91F;
                        float f3 = mc.player.isOnGround() ? f : 0.99F;
                        Vec3d vec3d = mc.player.getVelocity();
                        mc.player.setVelocity(vec3d.x / f2 * f3, vec3d.y, vec3d.z / f2 * f3);
                        break;
                    }
                }
            }

            if (this.mode.is("grim_entity2") && MoveUtil.isMoving()) {
                int i = 0;

                for (Entity entity : mc.world.getEntities()) {
                    if (entity != mc.player
                        && (!(entity instanceof ArmorStandEntity) || this.armorStands.getValue())
                        && (entity instanceof LivingEntity || entity instanceof AbstractBoatEntity)
                        && mc.player.getBoundingBox().expand(1.0).intersects(entity.getBoundingBox())) {
                        i++;
                    }
                }

                if (i == 0) {
                    return;
                }

                double[] doubles = MoveUtil.getMotionForInput(0.08 * i);
                mc.player.addVelocity(doubles[0], 0.0, doubles[1]);
                return;
            }
        }
    }

    private boolean isPaused() {
        return this.pause.has("liquids") && (mc.player.isTouchingWater() || mc.player.isInLava()) || this.pause.has("sneaking") && mc.player.isSneaking();
    }

    private double getBaseSpeed() {
        double d = 0.2873;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            d = 0.2873 * (1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1));
        }

        return d;
    }

    private BlockPos getBlockBelow() {
        return BlockPos.ofFloored(mc.player.getX(), mc.player.getBoundingBox().minY - 0.500001, mc.player.getZ());
    }
}
