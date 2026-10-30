package tech.sadnes.client.feature.impl.movement;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.block.BlockState;
import net.minecraft.block.SignBlock;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.item.SignItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.HudPostRenderEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.event.impl.PreRotationEvent;
import tech.sadnes.client.event.impl.ScreenMouseScrollEvent;
import tech.sadnes.client.event.impl.SendMovementPacketsEvent;
import tech.sadnes.client.event.impl.SetScreenEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.MoveUtil;
import tech.sadnes.client.util.rotation.RotationUtil;

@FeatureInfo(
    name = "Flight",
    description = "feature.impl.movement.flight",
    displayName = "feature.impl.movement.flight",
    category = Category.MOVEMENT,
    subcategory = Subcategory.MOVEMENT_AIR
)
public final class Flight extends Feature {
    private final ModeSetting flightMode = new ModeSetting("flight.flightMode", new String[]{"motion", "funtime", "Grim", "damager"}).setValue("motion");
    private final NumberSetting x = new NumberSetting("flight.x", 0.5F, 0.1F, 7.0F, 0.01F).setVisibleWhen(() -> this.flightMode.is("motion"));
    private final NumberSetting y = new NumberSetting("flight.y", 0.5F, 0.1F, 7.0F, 0.01F).setVisibleWhen(() -> this.flightMode.is("motion"));
    private final BooleanSetting airStop = new BooleanSetting("flight.airStop", true).setVisibleWhen(() -> this.flightMode.is("motion"));
    private final BooleanSetting damagerNotify = new BooleanSetting("flight.damagerNotify", true).setVisibleWhen(() -> this.flightMode.is("damager"));
    private final DamagerFlightHelper damagerHelper = new DamagerFlightHelper(this.getDisplayName(), this.damagerNotify);
    boolean boosting = false;
    private final StopWatch useTimer = new StopWatch();
    private final Queue<Flight.DelayedPacket> delayedPackets = new ConcurrentLinkedQueue<>();
    private BlockPos placePos;
    private BlockPos pendingPlacePos;
    private float placeYaw;
    private float placePitch;
    private Vec3d savedPosition;
    private float savedYaw;
    private float savedPitch;
    private double baseY;
    int bucketSlot = -1;
    int signSlot = -1;

    public Flight() {
        this.flightMode.setOnChange(() -> {
            if (!this.flightMode.is("Grim")) {
                this.releasePackets(0L);
            }
        });
    }

    public static Flight getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(Flight.class);
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.flightMode.is("motion")) {
            boolean flag = mc.player.input.playerInput.jump();
            boolean flag2 = mc.player.input.playerInput.sneak();
            boolean flag3 = MoveUtil.isMoving();
            if (this.airStop.getValue() && !flag3 && !flag && !flag2) {
                mc.player.setVelocity(0.0, 0.0, 0.0);
            } else {
                double d = flag
                    ? this.y.getValue().floatValue()
                    : (flag2 ? -this.y.getValue() : (this.airStop.getValue() ? 0.0 : mc.player.getVelocity().getY()));
                mc.player.setVelocity(mc.player.getVelocity().getX(), d, mc.player.getVelocity().getZ());
                if (flag3) {
                    MoveUtil.setMotion(this.x.getValue().floatValue());
                }
            }
        } else {
            if (this.flightMode.is("funtime")) {
                if (InventoryUtil.countInContainer(Items.WATER_BUCKET) < 1) {
                    ChatUtil.print(Language.translate("flight.noWaterOrSigns", "Закончились таблички или нет ведра воды!"));
                    this.setEnabled(false);
                }

                this.boosting = mc.player.input.playerInput.jump()
                    && mc.world.getBlockState(mc.player.getBlockPos().up()).getBlock() instanceof SignBlock
                    && this.flightMode.is("funtime")
                    && mc.player.fallDistance == 0.0;
                RotationComponent.rotate(new Rotation(mc.player.getYaw(), 90.0F), 999.0F, 999.0F, 999.0F, 999.0F, 2, 10, false);
                if (!(mc.player.getOffHandStack().getItem() instanceof SignItem) && InventoryUtil.findPlayerSlotByClass(SignItem.class) != null) {
                    mc.interactionManager.clickSlot(0, InventoryUtil.findPlayerSlotByClass(SignItem.class).id, 40, SlotActionType.SWAP, mc.player);
                }

                if (this.useTimer.isStarted() && mc.player.input.playerInput.jump()) {
                    BlockHitResult blockHitResult = mc.world
                        .raycast(
                            new RaycastContext(
                                mc.player.getCameraPosVec(1.0F),
                                mc.player.getCameraPosVec(1.0F).add(Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw()).multiply(3.0)),
                                ShapeType.OUTLINE,
                                FluidHandling.SOURCE_ONLY,
                                mc.player
                            )
                        );
                    if (mc.player.getOffHandStack().getItem() instanceof SignItem) {
                        mc.interactionManager
                            .interactBlock(mc.player, Hand.OFF_HAND, new BlockHitResult(blockHitResult.getPos(), blockHitResult.getSide(), blockHitResult.getBlockPos(), true));
                    }

                    if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET) {
                        mc.interactionManager
                            .interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(blockHitResult.getPos(), blockHitResult.getSide(), blockHitResult.getBlockPos(), true));
                        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    }

                    if (this.boosting) {
                        mc.player.setVelocity(mc.player.getVelocity().x, 0.42F, mc.player.getVelocity().z);
                    }

                    this.useTimer.delay(67L);
                    return;
                }
            } else {
                if (this.flightMode.is("Grim")) {
                    int i = this.findBlockSlot();
                    if (i != -1 && this.savedPosition != null) {
                        if (mc.options.jumpKey.isPressed()) {
                            if (mc.player.fallDistance == 0.0) {
                                return;
                            }

                            Vec3d vec3d = this.getSavedEyePos();
                            BlockHitResult blockHitResult2 = mc.world
                                .raycast(
                                    new RaycastContext(
                                        vec3d,
                                        vec3d.add(Vec3d.fromPolar(90.0F, this.savedYaw).multiply(4.0)),
                                        ShapeType.OUTLINE,
                                        FluidHandling.SOURCE_ONLY,
                                        mc.player
                                    )
                                );
                            if (blockHitResult2.getType() != Type.BLOCK) {
                                return;
                            }

                            this.placeBlock(i, new BlockHitResult(blockHitResult2.getPos(), blockHitResult2.getSide(), blockHitResult2.getBlockPos(), blockHitResult2.isInsideBlock()));
                            mc.player.fallDistance = 0.0;
                            return;
                        }

                        if (this.pendingPlacePos != null && mc.world.getBlockState(this.pendingPlacePos).isAir()) {
                            BlockHitResult blockHitResult3 = this.raycastFrom(this.getSavedEyePos(), this.savedYaw, this.savedPitch, this.pendingPlacePos);
                            if (blockHitResult3 == null) {
                                return;
                            }

                            this.placeBlock(i, blockHitResult3);
                            if (this.pendingPlacePos.equals(this.placePos)) {
                                this.placePos = null;
                            }

                            this.pendingPlacePos = null;
                            return;
                        }

                        return;
                    }

                    return;
                }

                if (this.flightMode.is("Funtime Elytra")) {
                    boolean flag4 = InputUtil.isKeyPressed(mc.getWindow(), mc.options.sneakKey.getDefaultKey().getCode());
                    int i2 = InventoryUtil.findHotbarIndex(Items.ELYTRA);
                    if (mc.player.getInventory().getSelectedSlot() != i2) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i2));
                    }

                    PacketSender.sendSequenced(i3 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, i3, mc.player.getYaw(), mc.player.getPitch()));
                    mc.player.startGliding();
                    mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
                    PacketSender.sendSequenced(i4 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, i4, mc.player.getYaw(), mc.player.getPitch()));
                    if (mc.player.getInventory().getSelectedSlot() != i2) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPreUpdate(WorldTickEvent worldTickEvent) {
        if (this.flightMode.is("damager")) {
            this.damagerHelper.tick();
        }
    }

    @EventHandler
    public void onPostUpdate(PlayerTickEvent playerTickEvent) {
        this.damagerHelper.processQueue();
    }

    @EventHandler
    public void onDamagerPacket(PacketEvent packetEvent) {
        this.damagerHelper.onPacket(packetEvent);
    }

    @EventHandler
    public void onCancellableEvent(ScreenMouseScrollEvent screenMouseScrollEvent) {
        if (mc.player != null) {
            if (this.flightMode.is("funtime")) {
                screenMouseScrollEvent.cancel();
            }
        }
    }

    @EventHandler
    public void onUpdate(HudPostRenderEvent hudPostRenderEvent) {
        if (mc.currentScreen instanceof SignEditScreen && this.flightMode.is("funtime")) {
            mc.player.closeHandledScreen();
        }
    }

    @EventHandler
    public void onScreenOpen(SetScreenEvent setScreenEvent) {
        if (setScreenEvent.getScreen() instanceof SignEditScreen && this.flightMode.is("funtime")) {
            setScreenEvent.cancel();
        }
    }

    @EventHandler
    public void onInput(MoveInputEvent moveInputEvent) {
        if (this.flightMode.is("funtime")) {
            if (this.boosting) {
                moveInputEvent.setSneak(true);
                return;
            }
        } else if (this.flightMode.is("Grim")) {
            if (moveInputEvent.isMoving()) {
                moveInputEvent.setJump(false);
                moveInputEvent.setSneak(false);
            }

            if (!mc.player.isOnGround()) {
                moveInputEvent.setForward(0.0F);
                moveInputEvent.setStrafe(0.0F);
                return;
            }
        } else if (this.flightMode.is("Funtime Elytra")) {
            mc.player.isGliding();
        }
    }

    @EventHandler
    public void onGrimPacket(PacketEvent packetEvent) {
        if (this.flightMode.is("Grim") && packetEvent.isSend()) {
            if (mc.player != null && mc.world != null) {
                this.delayedPackets.add(new Flight.DelayedPacket(packetEvent.getPacket(), System.currentTimeMillis()));
                packetEvent.cancel();
            }
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.delayedPackets.clear();
        this.placePos = null;
        this.pendingPlacePos = null;
        this.savedPosition = null;
    }

    @EventHandler
    public void onPostMotion(SendMovementPacketsEvent sendMovementPacketsEvent) {
        if (this.flightMode.is("Grim")) {
            this.releasePackets(1000L);
            if (this.placePos != null && !mc.options.jumpKey.isPressed()) {
                Float floatValue = this.findPitchForYaw(this.placePos, mc.player.getYaw());
                if (floatValue != null) {
                    mc.player.setPitch(floatValue);
                    this.placePitch = floatValue;
                }
            }

            this.savedPosition = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            this.savedYaw = mc.player.getYaw();
            this.savedPitch = mc.player.getPitch();
            this.pendingPlacePos = this.placePos;
        }
    }

    private Vec3d getSavedEyePos() {
        return this.savedPosition.add(0.0, mc.player.getEyePos().y - mc.player.getY(), 0.0);
    }

    @EventHandler
    public void onPreMotion(PreRotationEvent preRotationEvent) {
        if (this.flightMode.is("Grim") && mc.player != null) {
            if (this.findBlockSlot() != -1) {
                if (mc.options.jumpKey.isPressed()) {
                    this.placePos = null;
                    RotationComponent.rotate(new Rotation(mc.player.getYaw(), 90.0F), 999.0F, 999.0F, 999.0F, 999.0F, 2, 10, false);
                } else {
                    if (mc.player.isOnGround()) {
                        this.baseY = Math.floor(mc.player.getY() - 1.0);
                    }

                    this.placePos = this.findPlacePos();
                    this.computePlaceRotation(this.placePos);
                    RotationComponent.rotate(new Rotation(this.placeYaw, this.placePitch), 999.0F, 999.0F, 999.0F, 999.0F, 3, 10, false);
                }
            }
        }
    }

    private void releasePackets(long l) {
        long l2 = System.currentTimeMillis();
        if (mc.getNetworkHandler() == null) {
            this.delayedPackets.clear();
        } else {
            for (Flight.DelayedPacket delayedPacket : this.delayedPackets) {
                if (l2 - delayedPacket.time() < l) {
                    return;
                }

                PacketSender.sendSilent(delayedPacket.packet());
                this.delayedPackets.remove(delayedPacket);
            }
        }
    }

    private BlockPos findPlacePos() {
        BlockPos blockPos = this.getPlacePosAt(mc.player.getX(), mc.player.getZ());
        if (blockPos != null) {
            return blockPos;
        }

        Vec3d vec3d = mc.player.getVelocity();
        return this.getPlacePosAt(mc.player.getX() + vec3d.x * 3.0, mc.player.getZ() + vec3d.z * 3.0);
    }

    private BlockPos getPlacePosAt(double d, double d2) {
        double d3 = mc.player.getY() - this.baseY;
        double d4 = d3 >= 0.0 && d3 <= 3.0 ? this.baseY : Math.floor(mc.player.getY()) - 1.0;
        BlockPos blockPos = BlockPos.ofFloored(d, d4, d2);
        if (!mc.world.getBlockState(blockPos).isAir()) {
            return null;
        } else if (mc.player.getBoundingBox().intersects(new Box(blockPos))) {
            return null;
        } else {
            return this.findPlaceTarget(blockPos) == null ? null : blockPos;
        }
    }

    private Flight.PlaceTarget findPlaceTarget(BlockPos blockPos) {
        for (Direction direction : Direction.values()) {
            BlockPos blockPos2 = blockPos.offset(direction);
            BlockState blockState = mc.world.getBlockState(blockPos2);
            if (blockState.isSolidBlock(mc.world, blockPos2) && blockState.getFluidState().isEmpty()) {
                return new Flight.PlaceTarget(blockPos2, direction.getOpposite());
            }
        }

        return null;
    }

    private void computePlaceRotation(BlockPos blockPos) {
        float f = MathHelper.wrapDegrees(Rotation.getCameraYaw() + 180.0F);
        this.placeYaw = f;
        if (blockPos != null) {
            for (float f2 = 0.0F; f2 <= 30.0F; f2 += 10.0F) {
                for (byte b = 1; b >= -1; b -= 2) {
                    float f3 = MathHelper.wrapDegrees(f + f2 * b);
                    Float floatValue = this.findPitchForYaw(blockPos, f3);
                    if (floatValue != null) {
                        this.placeYaw = f3;
                        this.placePitch = floatValue;
                        return;
                    }

                    if (f2 == 0.0F) {
                        break;
                    }
                }
            }
        }
    }

    private Float findPitchForYaw(BlockPos blockPos, float f) {
        Vec3d vec3d = mc.player.getEyePos();
        Float floatValue = null;
        float f2 = Float.MAX_VALUE;

        for (Direction direction : Direction.values()) {
            BlockPos blockPos2 = blockPos.offset(direction);
            BlockState blockState = mc.world.getBlockState(blockPos2);
            if (blockState.isSolidBlock(mc.world, blockPos2) && blockState.getFluidState().isEmpty()) {
                Direction direction2 = direction.getOpposite();

                for (float f3 = 0.2F; f3 <= 0.8F; f3 += 0.2F) {
                    Vec3d vec3d2 = this.getFacePoint(blockPos2, direction2, 0.5F, f3);
                    if (!(vec3d.squaredDistanceTo(vec3d2) > 20.25)) {
                        float f4 = RotationUtil.getRotationsTo(vec3d2).y;
                        float f5 = Math.abs(f4 - this.placePitch);
                        if (!(f5 >= f2) && this.raycastBlock(blockPos, f, f4) != null) {
                            f2 = f5;
                            floatValue = f4;
                        }
                    }
                }
            }
        }

        return floatValue;
    }

    private Vec3d getFacePoint(BlockPos blockPos, Direction direction, float f, float f2) {
        double d = blockPos.getX() + (direction.getOffsetX() == 0 ? f : (direction.getOffsetX() > 0 ? 1.0 : 0.0));
        double d2 = blockPos.getY() + (direction.getOffsetY() == 0 ? f2 : (direction.getOffsetY() > 0 ? 1.0 : 0.0));
        double d3 = blockPos.getZ() + (direction.getOffsetZ() == 0 ? (direction.getOffsetX() == 0 ? f2 : f) : (direction.getOffsetZ() > 0 ? 1.0 : 0.0));
        return new Vec3d(d, d2, d3);
    }

    private BlockHitResult raycastBlock(BlockPos blockPos, float f, float f2) {
        return this.raycastFrom(mc.player.getEyePos(), f, f2, blockPos);
    }

    private BlockHitResult raycastFrom(Vec3d vec3d, float f, float f2, BlockPos blockPos) {
        Vec3d vec3d2 = vec3d.add(Vec3d.fromPolar(f2, f).multiply(4.0));
        BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(vec3d, vec3d2, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
        if (!(blockHitResult instanceof BlockHitResult) || blockHitResult.getType() != Type.BLOCK) {
            return null;
        } else {
            return !blockHitResult.getBlockPos().offset(blockHitResult.getSide()).equals(blockPos) ? null : blockHitResult;
        }
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem) {
                return i;
            }
        }

        ChatUtil.print(Language.translate("flight.needBlocks", "Для работы этого режима нужны блоки в хотбаре!"));
        this.setEnabled(false);
        return -1;
    }

    private void placeBlock(int i, BlockHitResult blockHitResult) {
        int i2 = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(i);
        if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult).isAccepted()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        mc.player.getInventory().setSelectedSlot(i2);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.damagerHelper.reset();
        if (mc.player != null) {
            this.placeYaw = mc.player.getYaw();
            this.placePitch = 70.0F;
            this.baseY = Math.floor(mc.player.getY() - 1.0);
        }

        this.placePos = null;
        if (this.flightMode.is("funtime")) {
            if (InventoryUtil.findPlayerSlotFromInventory(Items.WATER_BUCKET) == null) {
                ChatUtil.print(Language.translate("flight.noWater", "У тебя нет ведра воды"));
                this.setEnabled(false);
            } else {
                this.bucketSlot = Objects.requireNonNull(InventoryUtil.findPlayerSlotFromInventory(Items.WATER_BUCKET)).id;
                InventoryUtil.swapToHand(this.bucketSlot, Hand.MAIN_HAND);
            }

            if (InventoryUtil.findPlayerSlotFromInventory(SignItem.class) == null) {
                ChatUtil.print(Language.translate("flight.noSigns", "У тебя нет табличек, для работы флая возьми табличек"));
                return;
            }

            this.signSlot = Objects.requireNonNull(InventoryUtil.findPlayerSlotFromInventory(SignItem.class)).id;
            InventoryUtil.swapToHand(this.signSlot, Hand.OFF_HAND);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.damagerHelper.cancel();
        this.releasePackets(0L);
        this.placePos = null;
        this.pendingPlacePos = null;
        this.savedPosition = null;
        if (this.flightMode.is("funtime")) {
            if (this.bucketSlot != -1) {
                InventoryUtil.swapToHand(this.bucketSlot, Hand.MAIN_HAND);
                this.bucketSlot = -1;
            }

            if (this.signSlot != -1) {
                InventoryUtil.swapToHand(this.signSlot, Hand.OFF_HAND);
                this.signSlot = -1;
            }

            if (mc.currentScreen instanceof SignEditScreen) {
                mc.player.closeHandledScreen();
            }
        }
    }

    public ModeSetting getFlightMode() {
        return this.flightMode;
    }

    record DelayedPacket(Packet<?> packet, long time) {
    }

    record PlaceTarget(BlockPos pos, Direction face) {
    }
}
