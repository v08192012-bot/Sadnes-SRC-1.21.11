package tech.sadnes.client.feature.impl.movement;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import tech.sadnes.api.mixin.accessor.LivingEntityAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.HotbarSelectEvent;
import tech.sadnes.client.event.impl.JumpEvent;
import tech.sadnes.client.event.impl.MoveEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.PlayerInputPacketEvent;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.event.impl.PostMotionEvent;
import tech.sadnes.client.event.impl.PostPhysicsEvent;
import tech.sadnes.client.event.impl.SendMovementPacketsEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.feature.component.MovementPauseComponent;
import tech.sadnes.client.feature.component.NetworkStatsComponent;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.PixelRenderUtil;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.ButtonSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.TickQueue;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.math.TimerUtil;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.MoveUtil;
import tech.sadnes.client.util.player.MovementSimulator;
import tech.sadnes.client.util.rotation.RotationUtil;

@FeatureInfo(
    name = "Spider",
    description = "feature.impl.movement.spider",
    displayName = "feature.impl.movement.spider",
    category = Category.MOVEMENT,
    subcategory = Subcategory.MOVEMENT_TERRAIN
)
public final class Spider extends Feature {
    private final ModeSetting mode = new ModeSetting("spider.mode", new String[]{"Old polar", "Matrix", "Matrix blocks", "Ladders", "Funtime", "Сфера", "Blockspoof", "Test"})
        .setValue("Old polar");
    private final NumberSetting maxJumps = new NumberSetting("spider.maxJumps", 3.0F, 2.0F, 8.0F, 1.0F).setVisibleWhen(this::isFuntimeMode);
    private final boolean debug = false;
    private final Setting<Runnable> selectItem = new ButtonSetting("spider.selectItem", "spider.selectItem.button", this::startItemSelection)
        .setVisibleWhen(() -> this.mode.is("Test"));
    private final Setting<String> item = new TextSetting("spider.item").setHidden(true);
    private final StopWatch offhandSwapTimer = new StopWatch();
    private final StopWatch sphereTimer = new StopWatch();
    private final StopWatch blockspoofTimer = new StopWatch();
    private final TimerUtil slotSwitchTimer = new TimerUtil();
    private int previousSlot = -1;
    private final Spider.DelayedAction placeAction = new Spider.DelayedAction();
    private final Spider.DelayedAction jumpAction = new Spider.DelayedAction();
    private final Spider.DelayedAction swapAction = new Spider.DelayedAction();
    private final Spider.DelayedAction bootsSwapAction = new Spider.DelayedAction();
    private final TickQueue testPlaceTask = new TickQueue();
    private final TickQueue testJumpTask = new TickQueue();
    private final TickQueue testSwapTask = new TickQueue();
    private final TickQueue bootsSwapTask = new TickQueue();
    private final StopWatch jumpTimer = new StopWatch();
    private final StopWatch notifyTimer = new StopWatch();
    private int jumpCount;
    private boolean selectingItem;
    private Spider.PlacementTarget currentTarget;
    private Integer originalMainHandSlot;
    private Integer originalOffhandSlot;
    private BlockHitResult pendingHit;
    private boolean pendingInteract;

    public static Spider getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(Spider.class);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        int i = this.previousSlot;
        ModeSetting modeSetting = this.mode;
        Integer integer = this.originalMainHandSlot;
        Integer integer2 = this.originalOffhandSlot;
        Spider.DelayedAction delayedAction = this.bootsSwapAction;
        boolean flag;
        BlockHitResult blockHitResult;
        Integer integer3;
        if (i != -1) {
            this.pendingHit = null;
            this.pendingInteract = false;
            boolean flag2 = modeSetting.is("Matrix blocks");
            BlockHitResult blockHitResult2 = this.pendingHit;
            flag = this.pendingInteract;
            Integer integer4 = this.originalMainHandSlot;
            Integer integer5 = this.originalOffhandSlot;
            if (flag2) {
                boolean flag3 = ServerManager.isNullWorld();
                BlockHitResult blockHitResult3 = this.pendingHit;
                flag = this.pendingInteract;
                Integer integer6 = this.originalMainHandSlot;
                Integer integer7 = this.originalOffhandSlot;
                if (flag3) {
                    integer = integer6;
                    blockHitResult = blockHitResult3;
                    integer3 = integer7;
                    i = -1;
                } else {
                    PlayerInventory playerInventory = mc.player.getInventory();
                    int i2 = this.previousSlot;
                    playerInventory.setSelectedSlot(i2);
                    BlockHitResult blockHitResult4 = this.pendingHit;
                    flag = this.pendingInteract;
                    Integer integer8 = this.originalMainHandSlot;
                    Integer integer9 = this.originalOffhandSlot;
                    blockHitResult = blockHitResult4;
                    integer = integer8;
                    integer3 = integer9;
                    i = -1;
                }
            } else {
                integer = integer4;
                blockHitResult = blockHitResult2;
                integer3 = integer5;
                i = -1;
            }
        } else {
            flag = false;
            blockHitResult = null;
            integer3 = integer2;
        }

        if (integer != null) {
            this.pendingHit = blockHitResult;
            this.pendingInteract = flag;
            this.previousSlot = i;
            int i3 = integer;
            Hand hand = Hand.MAIN_HAND;
            InventoryUtil.swapToHand(i3, hand);
            BlockHitResult blockHitResult5 = this.pendingHit;
            flag = this.pendingInteract;
            i = this.previousSlot;
            Integer integer10 = this.originalOffhandSlot;
            blockHitResult = blockHitResult5;
            integer = null;
            integer3 = integer10;
        }

        if (integer3 != null) {
            this.pendingHit = blockHitResult;
            this.pendingInteract = flag;
            this.previousSlot = i;
            this.originalMainHandSlot = integer;
            int i4 = integer3;
            Hand hand2 = Hand.OFF_HAND;
            InventoryUtil.swapToHand(i4, hand2);
            blockHitResult = this.pendingHit;
            flag = this.pendingInteract;
            i = this.previousSlot;
            Integer integer11 = this.originalMainHandSlot;
            integer = integer11;
            integer3 = null;
        }

        this.pendingHit = blockHitResult;
        this.pendingInteract = flag;
        this.previousSlot = i;
        this.originalMainHandSlot = integer;
        this.originalOffhandSlot = integer3;
        delayedAction.runPending().reset();
        this.swapAction.runPending();
        boolean flag4 = modeSetting.is("Test");
        if (flag4) {
            boolean flag5 = ServerManager.isNullWorld();
            TickQueue tickQueue = this.bootsSwapTask;
            if (!flag5) {
                tickQueue.tick().clear();
                MovementPauseComponent.setDelay(2);
                this.testSwapTask.tick();
            }
        }

        this.jumpCount = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        boolean flag = ServerManager.isNullWorld();
        ModeSetting modeSetting = this.mode;
        if (!flag) {
            if (modeSetting.is("Matrix blocks")) {
                this.restorePreviousSlot();
            }

            if (this.isFuntimeMode()) {
                ClientPlayerEntity clientPlayerEntity = mc.player;
                RegistryEntry registryEntry = StatusEffects.JUMP_BOOST;
                StatusEffectInstance statusEffectInstance = clientPlayerEntity.getStatusEffect(registryEntry);
                if (statusEffectInstance != null && statusEffectInstance.getDuration() >= 201) {
                    return;
                }

                if (modeSetting.is("Test")) {
                    Stream stream2 = mc.player.currentScreenHandler.slots.stream();
                    Predicate<Slot> predicate = Spider::isJumpBoots;
                    Slot slot = (Slot)stream2.filter(predicate).findFirst().orElse(null);
                    TickQueue tickQueue = this.bootsSwapTask;
                    if (slot != null && tickQueue.isEmpty()) {
                        Runnable runnable = () -> InventoryUtil.moveToHotbar(slot, 8, true);
                        tickQueue.then(runnable);
                        InventoryUtil.moveToHotbar(slot, 8, true);
                        return;
                    }
                } else {
                    Stream stream3 = InventoryUtil.getPlayerSlots().stream();
                    Predicate<Slot> predicate2 = Spider::isJumpBoots;
                    Slot slot2 = (Slot)stream3.filter(predicate2).findFirst().orElse(null);
                    Spider.DelayedAction delayedAction = this.bootsSwapAction;
                    if (slot2 != null && delayedAction.isIdle()) {
                        Runnable runnable2 = () -> InventoryUtil.swapSlots(slot2.id, 8);
                        delayedAction.schedule(runnable2);
                        InventoryUtil.swapSlots(slot2.id, 8);
                        return;
                    }
                }
            }
        }
    }

    private void restorePreviousSlot() {
        if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
            int i = -1;
            long l = 0L;

            while (true) {
                PlayerInventory playerInventory = mc.player.getInventory();
                int i2 = (int)l;
                boolean flag = playerInventory.getStack(i2).getItem() instanceof BlockItem;
                TimerUtil timerUtil = this.slotSwitchTimer;
                if (!flag) {
                    if (++l != 9L) {
                        continue;
                    }

                    if (i == -1) {
                        break;
                    }
                } else {
                    i = i2;
                    if (++l != 9L) {
                        continue;
                    }

                    if (i2 == -1) {
                        break;
                    }
                }

                timerUtil.setDelay(20L);
                int i3 = InventoryUtil.getSelectedSlot();
                this.previousSlot = i3;
                mc.player.getInventory().setSelectedSlot(i);
                return;
            }
        }
    }

    @EventHandler
    public void onSendPackets(SendMovementPacketsEvent sendMovementPacketsEvent) {
        if (this.mode.is("Matrix blocks")) {
            if (ServerManager.isNullWorld()) {
                return;
            }

            if ((byte)(mc.player.age & 0xFF & 1) == 0 && mc.player.horizontalCollision) {
                boolean flag = MoveUtil.isMoving();
                TimerUtil timerUtil = this.slotSwitchTimer;
                if (flag && timerUtil.isReached()) {
                    if (mc.player.getMainHandStack().getItem() instanceof BlockItem) {
                        double d = mc.player.fallDistance;
                        int i = !Double.isNaN(d) ? (d == 0.0 ? 0 : 1) : 0;
                        if (!Double.isNaN(d)) {
                            if (i != 0) {
                                return;
                            }

                            if (mc.player.hurtTime == 0) {
                                double d2 = mc.player.getX();
                                double d3 = mc.player.getY();
                                double d4 = mc.player.getZ();
                                double d5 = ExactMath.add(d3, 2.0);
                                BlockPos blockPos = BlockPos.ofFloored(d2, d5, d4);
                                Direction direction = this.findSolidNeighborDirection(blockPos);
                                if (direction != null) {
                                    BlockPos blockPos2 = blockPos.offset(direction);
                                    Direction direction2 = direction.getOpposite();
                                    Vec3d vec3d = blockPos2.toCenterPos();
                                    Vec3d vec3d2 = Vec3d.of(direction2.getVector()).multiply(0.5);
                                    Vec3d vec3d3 = vec3d.add(vec3d2);
                                    PacketSender.sendSequenced(
                                        sequence -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(vec3d3, direction2, blockPos2, false), sequence)
                                    );
                                    ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
                                    Hand hand = Hand.MAIN_HAND;
                                    HandSwingC2SPacket handSwingC2SPacket = new HandSwingC2SPacket(hand);
                                    clientPlayNetworkHandler.sendPacket(handSwingC2SPacket);
                                    PlayerInput playerInput2 = mc.player.input.playerInput;
                                    if (!playerInput2.sneak()) {
                                        ClientPlayNetworkHandler clientPlayNetworkHandler2 = mc.player.networkHandler;
                                        boolean flag2 = playerInput2.forward();
                                        boolean flag3 = playerInput2.backward();
                                        boolean flag4 = playerInput2.left();
                                        boolean flag5 = playerInput2.right();
                                        boolean flag6 = playerInput2.jump();
                                        boolean flag7 = playerInput2.sprint();
                                        PlayerInput playerInput3 = new PlayerInput(flag2, flag3, flag4, flag5, flag6, true, flag7);
                                        PlayerInputC2SPacket playerInputC2SPacket = new PlayerInputC2SPacket(playerInput3);
                                        clientPlayNetworkHandler2.sendPacket(playerInputC2SPacket);
                                        ClientPlayNetworkHandler clientPlayNetworkHandler3 = mc.player.networkHandler;
                                        PlayerInputC2SPacket playerInputC2SPacket2 = new PlayerInputC2SPacket(playerInput2);
                                        clientPlayNetworkHandler3.sendPacket(playerInputC2SPacket2);
                                        mc.player.setOnGround(true);
                                        mc.player.jump();
                                        return;
                                    }
                                }

                                mc.player.setOnGround(true);
                                mc.player.jump();
                                return;
                            }

                            return;
                        }
                    }

                    return;
                }
            }

            if (mc.player.getMainHandStack().getItem() instanceof BlockItem) {
                double d6 = mc.player.fallDistance;
                int i2 = !Double.isNaN(d6) ? (d6 == 0.0 ? 0 : 1) : 0;
                if (!Double.isNaN(d6)) {
                    if (i2 != 0) {
                        return;
                    }

                    return;
                }
            }
        }
    }

    private Direction findSolidNeighborDirection(BlockPos blockPos) {
        Direction[] directions = Direction.values();
        int i = Array.getLength(directions);
        if (i <= 0) {
            return null;
        }

        int i2 = i & 2147483647;
        long l = 0L;

        do {
            int i3 = (int)l;
            Object object = Array.get(directions, i3);
            BlockPos blockPos2 = blockPos.offset((Direction)object);
            if (!mc.world.isAir(blockPos2) && !mc.world.getBlockState(blockPos2).isReplaceable()) {
                return (Direction)object;
            }
        } while (Integer.toUnsignedLong(i2) != ++l);

        return null;
    }

    private static boolean isJumpBoots(Slot slot2) {
        ItemStack itemStack = slot2.getStack();
        EquippableComponent equippableComponent = itemStack.get(DataComponentTypes.EQUIPPABLE);
        return equippableComponent != null
            && equippableComponent.slot() == EquipmentSlot.FEET
            && itemStack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).toString().contains("Попрыгун");
    }

    @EventHandler
    public void onRotation(MoveInputEvent moveInputEvent) {
        ModeSetting modeSetting = this.mode;
        boolean flag = modeSetting.is("Blockspoof");
        StopWatch stopWatch = this.blockspoofTimer;
        if (flag && !stopWatch.hasPassed(100.0)) {
            moveInputEvent.setJump(true);
        }

        boolean flag2 = modeSetting.is("Test");
        StopWatch stopWatch2 = this.jumpTimer;
        if (flag2 && !stopWatch2.hasPassed(100.0)) {
            moveInputEvent.setJump(true);
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!ServerManager.isNullWorld()) {
            ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
            ModeSetting modeSetting = this.mode;
            if (clientPlayerInteractionManager != null) {
                if (modeSetting.is("Funtime")) {
                    this.handleFuntime();
                } else if (modeSetting.is("Blockspoof")) {
                    this.handleBlockspoof();
                } else if (modeSetting.is("РЎС„РµСЂР°")) {
                    this.placeSphereTarget();
                    if (mc.player.horizontalCollision) {
                        if (mc.player.isTouchingWater()) {
                            this.currentTarget = null;
                        } else {
                            this.aimSphereTarget();
                        }
                    } else {
                        this.currentTarget = null;
                    }
                } else {
                    this.currentTarget = null;
                }
            }
        }
    }

    private void handleBlockspoof() {
        BlockHitResult blockHitResult = this.pendingHit;
        boolean flag;
        BlockHitResult blockHitResult2;
        if (blockHitResult != null) {
            this.pendingHit = null;
            BlockPos blockPos = blockHitResult.getBlockPos();
            Direction direction = blockHitResult.getSide();
            BlockPos blockPos2 = blockPos.offset(direction);
            boolean flag2 = mc.world.getBlockState(blockPos2).isReplaceable();
            BlockHitResult blockHitResult3 = this.pendingHit;
            flag = this.pendingInteract;
            if (flag2) {
                ItemStack itemStack = mc.player.getOffHandStack();
                Item item2 = Items.PLAYER_HEAD;
                boolean flag3 = itemStack.isOf(item2);
                BlockHitResult blockHitResult4 = this.pendingHit;
                flag = this.pendingInteract;
                if (flag3) {
                    sendStopDestroyPacket();
                    ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
                    ClientPlayerEntity clientPlayerEntity = mc.player;
                    Hand hand = Hand.MAIN_HAND;
                    clientPlayerInteractionManager.interactBlock(clientPlayerEntity, hand, blockHitResult);
                    ClientPlayerEntity clientPlayerEntity2 = mc.player;
                    Hand hand2 = Hand.MAIN_HAND;
                    clientPlayerEntity2.swingHand(hand2);
                    this.blockspoofTimer.reset();
                    BlockHitResult blockHitResult5 = this.pendingHit;
                    flag = true;
                    blockHitResult2 = blockHitResult5;
                } else {
                    blockHitResult2 = blockHitResult4;
                }
            } else {
                blockHitResult2 = blockHitResult3;
            }
        } else {
            boolean flag4 = mc.options.jumpKey.isPressed();
            BlockHitResult blockHitResult6 = this.pendingHit;
            flag = this.pendingInteract;
            if (flag4) {
                boolean flag5 = mc.player.horizontalCollision;
                BlockHitResult blockHitResult7 = this.pendingHit;
                flag = this.pendingInteract;
                if (flag5) {
                    boolean flag6 = mc.player.isTouchingWater();
                    BlockHitResult blockHitResult8 = this.pendingHit;
                    StopWatch stopWatch = this.blockspoofTimer;
                    flag = this.pendingInteract;
                    if (flag6) {
                        blockHitResult2 = blockHitResult8;
                    } else {
                        boolean flag7 = stopWatch.hasPassed(100.0);
                        BlockHitResult blockHitResult9 = this.pendingHit;
                        flag = this.pendingInteract;
                        if (flag7) {
                            ItemStack itemStack2 = mc.player.getOffHandStack();
                            Item item3 = Items.PLAYER_HEAD;
                            if (itemStack2.isOf(item3)) {
                                Spider.PlacementTarget placementTarget = this.findLadderTarget();
                                BlockHitResult blockHitResult10 = this.pendingHit;
                                flag = this.pendingInteract;
                                if (placementTarget != null) {
                                    Vec2f vec2f = RotationUtil.getRotationsTo(placementTarget.hit());
                                    float f = vec2f.x;
                                    float f2 = vec2f.y;
                                    RotationComponent.rotateTo(new Rotation(f, f2), 999.0F, 999.0F, 2, 5);
                                    Vec3d vec3d = placementTarget.hit();
                                    Direction direction2 = placementTarget.side();
                                    BlockPos blockPos3 = placementTarget.pos();
                                    BlockHitResult blockHitResult11 = new BlockHitResult(vec3d, direction2, blockPos3, false);
                                    flag = this.pendingInteract;
                                    blockHitResult2 = blockHitResult11;
                                } else {
                                    blockHitResult2 = blockHitResult10;
                                }
                            } else {
                                Slot slot = InventoryUtil.findSlotReverse(Items.PLAYER_HEAD);
                                BlockHitResult blockHitResult12 = this.pendingHit;
                                flag = this.pendingInteract;
                                StopWatch stopWatch2 = this.offhandSwapTimer;
                                if (slot != null) {
                                    boolean flag8 = stopWatch2.hasPassed(400.0);
                                    BlockHitResult blockHitResult13 = this.pendingHit;
                                    flag = this.pendingInteract;
                                    Integer integer = this.originalOffhandSlot;
                                    if (flag8) {
                                        Integer integer2;
                                        if (integer == null) {
                                            integer2 = slot.id;
                                        } else {
                                            integer2 = integer;
                                        }

                                        this.originalOffhandSlot = integer2;
                                        int i = slot.id;
                                        Hand hand3 = Hand.OFF_HAND;
                                        InventoryUtil.swapToHand(i, hand3);
                                        stopWatch2.reset();
                                        BlockHitResult blockHitResult14 = this.pendingHit;
                                        flag = this.pendingInteract;
                                        blockHitResult2 = blockHitResult14;
                                    } else {
                                        blockHitResult2 = blockHitResult13;
                                    }
                                } else {
                                    blockHitResult2 = blockHitResult12;
                                }
                            }
                        } else {
                            blockHitResult2 = blockHitResult9;
                        }
                    }
                } else {
                    blockHitResult2 = blockHitResult7;
                }
            } else {
                blockHitResult2 = blockHitResult6;
            }
        }

        this.pendingHit = blockHitResult2;
        this.pendingInteract = flag;
    }

    private static void sendStopDestroyPacket() {
        Action action = Action.SWAP_ITEM_WITH_OFFHAND;
        BlockPos blockPos = BlockPos.ORIGIN;
        Direction direction = Direction.DOWN;
        PacketSender.sendSilent(new PlayerActionC2SPacket(action, blockPos, direction));
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        boolean flag = this.mode.is("Blockspoof");
        StopWatch stopWatch = this.blockspoofTimer;
        if (!flag || stopWatch.hasPassed(300.0)) {
            boolean flag2 = this.isFuntimeMode();
            StopWatch stopWatch2 = this.jumpTimer;
            if (!flag2 || stopWatch2.hasPassed(300.0)) {
                return;
            }
        }

        if (packetEvent.isReceive() && mc.player != null && packetEvent.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket screenHandlerSlotUpdateS2CPacket) {
            int i = screenHandlerSlotUpdateS2CPacket.getSlot();
            if (mc.player.getInventory().getSelectedSlot() + 36 == i) {
                ItemStack itemStack = mc.player.getOffHandStack();
                Item item2 = screenHandlerSlotUpdateS2CPacket.getStack().getItem();
                if (itemStack.isOf(item2)) {
                    packetEvent.cancel();
                    return;
                }
            }
        }
    }

    private boolean isFuntimeMode() {
        return this.mode.is("Funtime") || this.mode.is("Test");
    }

    private void handleFuntime() {
        Box box = mc.player.getBoundingBox().contract(1.0E-5);
        if (this.canClimbFuntime(box)) {
            Slot slot = mc.player.playerScreenHandler.getSlot(45);
            Slot slot2 = InventoryUtil.findSlotReverse(Items.GRANITE);
            StopWatch stopWatch = this.notifyTimer;
            if (slot2 != null) {
                ItemStack itemStack = mc.player.getMainHandStack();
                Item item2 = Items.GRANITE;
                if (!itemStack.isOf(item2)) {
                    this.swapToMainHand(slot2, slot);
                    return;
                }

                Box box2 = getFeetBox(box);
                List list = this.collectWallPlacements(box2);
                if (!list.isEmpty()) {
                    BlockHitResult blockHitResult = (BlockHitResult)list.getFirst();
                    BooleanSupplier booleanSupplier = Spider::isServerTpsHigh;
                    this.placeAndJump(blockHitResult, booleanSupplier);
                    return;
                }
            } else if (stopWatch.hasPassed(2000.0)) {
                NotificationType notificationType = NotificationType.WARNING;
                String string = this.getDisplayName();
                ItemStack itemStack2 = Items.GRANITE.getDefaultStack();
                String string2 = Items.GRANITE.getName().getString();
                NotificationUtil.notify(notificationType, string, itemStack2, string2);
                stopWatch.reset();
            }

            boolean flag = GuiMoveComponent.isActive();
            Spider.DelayedAction delayedAction = this.swapAction;
            if (!flag) {
                delayedAction.runPending();
                return;
            }
        }
    }

    private boolean canClimbFuntime(Box box) {
        double d = mc.player.getVelocity().y;
        float f = this.maxJumps.getValue();
        float f2 = this.maxJumps.getMax();
        boolean flag = ServerManager.isServer("funtime");
        StopWatch stopWatch = this.jumpTimer;
        if (!flag && !ServerManager.isLocalProxy()) {
            return false;
        }

        if (stopWatch.hasPassed(100.0)) {
            if (!(f >= f2)) {
                Predicate<BlockPos> predicate = blockPos -> !mc.world.getBlockState(blockPos).isAir();
                boolean flag2 = this.anyBlockMatches(box, predicate);
                Spider.DelayedAction delayedAction = this.bootsSwapAction;
                if (flag2) {
                    return false;
                }

                boolean flag3 = delayedAction.isIdle();
                NumberSetting numberSetting = this.maxJumps;
                int i = this.jumpCount;
                double d2 = ExactMath.multiply(ExactMath.add(d, -0.08), 0.98);
                if (!flag3) {
                    ClientPlayerEntity clientPlayerEntity = mc.player;
                    RegistryEntry registryEntry = StatusEffects.JUMP_BOOST;
                    boolean flag4 = clientPlayerEntity.hasStatusEffect(registryEntry);
                    i = this.jumpCount;
                    if (!flag4) {
                        return false;
                    }
                }

                if (0.0 >= d2) {
                    int i2 = numberSetting.getValue().intValue() - 1;
                    return i < i2;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private void swapToMainHand(Slot slot, Slot slot2) {
        if (!GuiMoveComponent.isActive() && this.swapAction.isIdle() && this.swapAction.hasElapsed(400L)) {
            Runnable runnable = slot.id != slot2.id && slot2.getStack().isOf(Items.PLAYER_HEAD) ? () -> InventoryUtil.clickSwap(slot.id, 40) : () -> {};
            Runnable runnable2 = () -> InventoryUtil.clickSwap(slot.id, InventoryUtil.getSelectedSlot());
            runnable2.run();
            runnable.run();
            this.swapAction.schedule(() -> {
                runnable.run();
                runnable2.run();
                this.swapAction.reset();
            });
        }
    }

    private void placeAndJump(BlockHitResult blockHitResult, BooleanSupplier booleanSupplier) {
        Vec2f vec2f = RotationUtil.getRotationsTo(blockHitResult.getPos());
        float f = vec2f.x;
        float f2 = vec2f.y;
        RotationComponent.rotateTo(new Rotation(f, f2), 999.0F, 999.0F, 2, 5);
        Spider.DelayedAction delayedAction = this.placeAction.reset();
        Runnable runnable = () -> {
            if (booleanSupplier.getAsBoolean()) {
                sendStopDestroyPacket();
            }

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
            mc.player.swingHand(Hand.MAIN_HAND);
            this.jumpAction.reset().schedule(() -> {
                if (booleanSupplier.getAsBoolean()) {
                    sendStopDestroyPacket();
                }

                Vec3d vec3d = mc.player.getVelocity();
                mc.player.setVelocity(vec3d.x, ((LivingEntityAccessor)mc.player).sadnes$invokeGetJumpVelocity(), vec3d.z);
                mc.player.setOnGround(true);
            });
        };
        delayedAction.schedule(runnable);
        this.jumpTimer.reset();
        boolean flag = mc.player.isOnGround();
        int i = this.jumpCount;
        if (!flag) {
            i++;
        }

        this.jumpCount = i;
    }

    @EventHandler
    public void onTestUpdate(WorldTickEvent worldTickEvent) {
        if (this.mode.is("Test")) {
            this.handleTestMode();
        }
    }

    private void handleTestMode() {
        MovementSimulator movementSimulator = MovementSimulator.simulate(1);
        if (!MovementSimulator.isRecentlyReset() || !movementSimulator.hasMoved()) {
            Box box = movementSimulator.getBoundingBox().contract(1.0E-5);
            Box box2 = mc.player.getBoundingBox().contract(1.0E-5);
            boolean flag = this.maxJumps.getValue() >= this.maxJumps.getMax();
            if (ServerManager.isFunTime() && this.jumpTimer.hasPassed(100.0) && (flag || this.canClimbTest(box2, movementSimulator))) {
                Item item2 = this.getSelectedItem();
                if (item2 != Items.AIR || this.selectingItem) {
                    Slot slot2 = mc.player.playerScreenHandler.getSlot(45);
                    Slot slot3 = mc.player
                        .currentScreenHandler
                        .slots
                        .stream()
                        .filter(slot4 -> slot4.getStack().isOf(item2))
                        .max(Comparator.comparing(slot2::equals))
                        .orElse(null);
                    if (slot3 == null) {
                        if (NotificationUtil.isSuppressed()) {
                            NotificationUtil.notify(NotificationType.WARNING, this.getDisplayName(), item2.getDefaultStack(), item2.getName());
                        }
                    } else {
                        ScreenHandler screenHandler = mc.player.currentScreenHandler;
                        int i = screenHandler.slots.size() - (screenHandler.equals(mc.player.playerScreenHandler) ? 10 : 9) + mc.player.getInventory().getSelectedSlot();
                        if (i < 0) {
                            return;
                        }

                        boolean flag2 = screenHandler.slots.get(i).getStack().isOf(item2);
                        Box box3 = getHeadBox(box);
                        boolean flag3 = !this.collectInteractTargets(box3).isEmpty() || this.findFallbackTarget(box3) != null;
                        if (flag3 && !flag2) {
                            if (canSwapItems() && this.testSwapTask.isEmpty() && this.testSwapTask.hasPassed(400L)) {
                                Runnable runnable = slot2 != slot3 && slot2.getStack().isOf(Items.PLAYER_HEAD)
                                    ? () -> InventoryUtil.clickSlotLocal(slot3.id, 40, SlotActionType.SWAP)
                                    : () -> {};
                                Runnable runnable2 = () -> InventoryUtil.clickSlotLocal(slot3.id, mc.player.getInventory().getSelectedSlot(), SlotActionType.SWAP);
                                MovementPauseComponent.queue(() -> {
                                    runnable2.run();
                                    runnable.run();
                                });
                                this.testSwapTask.then(() -> MovementPauseComponent.queue(() -> {
                                    runnable.run();
                                    runnable2.run();
                                    InventoryUtil.sendRandomClick();
                                    this.testSwapTask.clear();
                                }));
                            }

                            return;
                        }

                        List list = this.collectInteractTargets(getHeadBox(box2));
                        if (!list.isEmpty()) {
                            BlockHitResult blockHitResult = (BlockHitResult)list.getFirst();
                            if (blockHitResult == null) {
                                return;
                            }

                            Vec2f vec2f = RotationUtil.getRotationsTo(blockHitResult.getPos());
                            RotationComponent.rotateTo(new Rotation(vec2f.x, vec2f.y), 999.0F, 999.0F, 0, 5);
                            this.testPlaceTask.clear().then(() -> {
                                if (isHighAnarchy()) {
                                    sendStopDestroyPacket();
                                }

                                PacketSender.sendSequenced(i2 -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, i2));
                                this.testJumpTask.clear().then(() -> {
                                    if (isHighAnarchy()) {
                                        sendStopDestroyPacket();
                                    }

                                    Vec3d vec3d = mc.player.getVelocity();
                                    mc.player.setVelocity(vec3d.x, ((LivingEntityAccessor)mc.player).sadnes$invokeGetJumpVelocity(), vec3d.z);
                                    mc.player.setOnGround(true);
                                });
                            });
                            this.jumpTimer.reset();
                            if (!mc.player.isOnGround()) {
                                this.jumpCount++;
                                return;
                            }

                            return;
                        }
                    }
                } else if (NotificationUtil.isSuppressed()) {
                    NotificationUtil.warning(this.getDisplayName(), Language.translate("spider.notify.selectItem", "Select a climbing item in Spider settings"));
                }

                if (canSwapItems()) {
                    this.testSwapTask.tick();
                }
            }
        }
    }

    private boolean canClimbTest(Box box, MovementSimulator movementSimulator) {
        Predicate<BlockPos> predicate = blockPos -> !mc.world.getBlockState(blockPos).isAir();
        boolean flag = this.anyBlockMatches(box, predicate);
        TickQueue tickQueue = this.bootsSwapTask;
        if (flag) {
            return false;
        }

        if (!tickQueue.isEmpty()) {
            ClientPlayerEntity clientPlayerEntity = mc.player;
            RegistryEntry registryEntry = StatusEffects.JUMP_BOOST;
            if (!clientPlayerEntity.hasStatusEffect(registryEntry)) {
                return false;
            }
        }

        MovementSimulator movementSimulator2 = MovementSimulator.simulate(2);
        if (MovementSimulator.isRecentlyReset() && movementSimulator2.hasMoved()) {
            return false;
        } else {
            double d = movementSimulator2.getPosition().y;
            double d2 = movementSimulator.getPosition().y;
            int i = this.jumpCount;
            NumberSetting numberSetting = this.maxJumps;
            if (!(d > d2)) {
                int i2 = numberSetting.getValue().intValue() - 1;
                return i < i2;
            } else {
                return false;
            }
        }
    }

    private static boolean canSwapItems() {
        return MovementPauseComponent.hasPendingTasks() && !GuiMoveComponent.isActive();
    }

    private static boolean isHighAnarchy() {
        return ServerManager.getFunTimeAnarchy() > 100;
    }

    @EventHandler
    public void onPostTick(PostPhysicsEvent postPhysicsEvent) {
        if (this.mode.is("Test")) {
            if (mc.player.isOnGround()) {
                this.jumpCount = 0;
            }

            this.testJumpTask.tick();
        }
    }

    @EventHandler
    public void onSlotChange(HotbarSelectEvent hotbarSelectEvent) {
        boolean flag;
        if (this.mode.is("Matrix blocks") && mc.player != null) {
            flag = mc.player.getMainHandStack().getItem() instanceof BlockItem;
        } else {
            flag = false;
        }

        if (this.mode.is("Test") && !this.testSwapTask.isEmpty() || flag) {
            hotbarSelectEvent.cancel();
        }
    }

    @EventHandler
    public void onPreUpdate(PlayerTickEvent playerTickEvent) {
        this.placeAction.runPending();
        this.testPlaceTask.tick();
    }

    @EventHandler
    public void onPostUpdate(PostMotionEvent postMotionEvent) {
        if (!this.mode.is("Test")) {
            if (mc.player != null && mc.player.isOnGround()) {
                this.jumpCount = 0;
            }

            this.jumpAction.runPending();
        }
    }

    @EventHandler
    public void onInput(PlayerInputPacketEvent playerInputPacketEvent) {
        if (!this.mode.is("Test") && !this.jumpTimer.hasPassed(100.0)) {
            playerInputPacketEvent.setJump(true);
        }
    }

    @EventHandler
    public void onJump(JumpEvent jumpEvent) {
        if (!this.jumpTimer.hasPassed(100.0)) {
            jumpEvent.setCancelled(true);
        } else {
            this.jumpCount++;
        }
    }

    private List<BlockHitResult> collectWallPlacements(Box box) {
        ArrayList arrayList = new ArrayList();
        Iterator iterator2 = BlockPos.iterate(box).iterator();
        if (iterator2.hasNext()) {
            do {
                BlockPos blockPos = (BlockPos)iterator2.next();
                Direction[] directions = Direction.values();
                int i = Array.getLength(directions);
                if (i > 0) {
                    int i2 = i & 2147483647;
                    long l = 0L;

                    do {
                        int i3 = (int)l;
                        Object object = Array.get(directions, i3);
                        if (((Direction)object).getOffsetY() == 0) {
                            BlockPos blockPos2 = blockPos.offset((Direction)object);
                            if (this.isSolidBlock(blockPos2)) {
                                BlockPos blockPos3 = blockPos.offset((Direction)object);
                                Direction direction = ((Direction)object).getOpposite();
                                Vec3d vec3d = getRandomizedFaceHit(blockPos3, direction);
                                BlockHitResult blockHitResult = new BlockHitResult(vec3d, direction, blockPos3, false);
                                arrayList.add(blockHitResult);
                            }
                        }
                    } while (Integer.toUnsignedLong(i2) != ++l);
                }
            } while (iterator2.hasNext());
        }

        return arrayList;
    }

    private List<BlockHitResult> collectInteractTargets(Box box) {
        ArrayList arrayList = new ArrayList();
        Iterator iterator2 = BlockPos.iterate(box).iterator();
        if (iterator2.hasNext()) {
            do {
                BlockPos blockPos = (BlockPos)iterator2.next();
                Direction[] directions = Direction.values();
                int i = Array.getLength(directions);
                if (i > 0) {
                    int i2 = i & 2147483647;
                    long l = 0L;

                    do {
                        int i3 = (int)l;
                        Object object = Array.get(directions, i3);
                        if (((Direction)object).getOffsetY() == 0) {
                            BlockPos blockPos2 = blockPos.offset((Direction)object);
                            if (this.isInteractableReplaceable(blockPos2)) {
                                BlockHitResult blockHitResult = this.raycastFace(blockPos, (Direction)object);
                                arrayList.add(blockHitResult);
                            }
                        }
                    } while (Integer.toUnsignedLong(i2) != ++l);
                }
            } while (iterator2.hasNext());
        }

        return arrayList;
    }

    private BlockHitResult findFallbackTarget(Box box) {
        return null;
    }

    private BlockHitResult raycastFace(BlockPos blockPos, Direction direction) {
        BlockPos blockPos2 = blockPos.offset(direction);
        Vec3d vec3d = mc.player.getEyePos();
        Vec3d vec3d2 = Vec3d.of(direction.getVector()).multiply(1.0E-12);
        Box box = new Box(blockPos);
        Box box2 = new Box(blockPos2);
        Box box3 = box.union(box2);
        double d = Math.abs(direction.getOffsetX());
        double d2 = Math.max(0.05, d);
        double d3 = Math.abs(direction.getOffsetY());
        double d4 = Math.max(0.05, d3);
        double d5 = Math.abs(direction.getOffsetZ());
        double d6 = Math.max(0.05, d5);
        int i = (int)Double.doubleToRawLongBits(d2);
        int i2 = (int)(Double.doubleToRawLongBits(d2) >>> 32) ^ Integer.MIN_VALUE;
        double d7 = Double.longBitsToDouble(Integer.toUnsignedLong(i) | (long)i2 << 32);
        int i3 = (int)Double.doubleToRawLongBits(d4);
        int i4 = (int)(Double.doubleToRawLongBits(d4) >>> 32) ^ Integer.MIN_VALUE;
        double d8 = Double.longBitsToDouble(Integer.toUnsignedLong(i3) | (long)i4 << 32);
        double d9 = Double.longBitsToDouble(Long.MIN_VALUE ^ Double.doubleToRawLongBits(d6));
        Box box4 = box3.expand(d7, d8, d9).offset(vec3d2);
        double d10 = vec3d.x;
        double d11 = box4.minX;
        double d12 = box4.maxX;
        double d13 = MathHelper.clamp(d10, d11, d12);
        double d14 = vec3d.y;
        double d15 = box4.minY;
        double d16 = box4.maxY;
        double d17 = MathHelper.clamp(d14, d15, d16);
        double d18 = vec3d.z;
        double d19 = box4.minZ;
        double d20 = box4.maxZ;
        double d21 = MathHelper.clamp(d18, d19, d20);
        Vec3d vec3d3 = new Vec3d(d13, d17, d21);
        BlockHitResult blockHitResult = Box.raycast(List.of(Box.from(Vec3d.ZERO)), vec3d, vec3d3, blockPos2);
        if (blockHitResult != null) {
            Direction direction2 = blockHitResult.getSide().getOpposite();
            if (direction.equals(direction2)) {
                double d22 = vec3d.distanceTo(vec3d3);
                if (!(4.45F > d22)) {
                    return null;
                }

                Vec3d vec3d4 = blockHitResult.getPos().add(vec3d2);
                Direction direction3 = blockHitResult.getSide();
                BlockPos blockPos3 = blockHitResult.getBlockPos();
                boolean flag = blockHitResult.isInsideBlock();
                return new BlockHitResult(vec3d4, direction3, blockPos3, flag);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean isInteractableReplaceable(BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        if (!blockState.isReplaceable()) {
            ClientWorld clientWorld = mc.world;
            ClientPlayerEntity clientPlayerEntity = mc.player;
            Vec3d vec3d = BlockPos.ORIGIN.toCenterPos();
            Direction direction = Direction.SOUTH;
            BlockPos blockPos2 = BlockPos.ORIGIN;
            BlockHitResult blockHitResult = new BlockHitResult(vec3d, direction, blockPos2, false);
            if (!blockState.onUse(clientWorld, clientPlayerEntity, blockHitResult).isAccepted()) {
                ClientWorld clientWorld2 = mc.world;
                BlockPos blockPos3 = BlockPos.ORIGIN;
                return !blockState.getOutlineShape(clientWorld2, blockPos3).isEmpty();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isSolidBlock(BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        return blockState.isSolid() && !blockState.getOutlineShape(mc.world, BlockPos.ORIGIN).isEmpty();
    }

    private static Box getFeetBox(Box box) {
        return box.withMinY(box.minY - 1.0).withMaxY(box.minY - 0.5);
    }

    private static Box getHeadBox(Box box) {
        return box.withMaxY(box.minY + 1.0).withMinY(box.minY + 0.5);
    }

    private static boolean isServerTpsHigh() {
        NetworkStatsComponent networkStatsComponent = Sadnes.getInstance().getComponentManager().getComponent(NetworkStatsComponent.class);
        return networkStatsComponent != null && networkStatsComponent.getTps() > 100.0;
    }

    private Item getSelectedItem() {
        Identifier identifier = Identifier.tryParse(this.item.getValue());
        return identifier == null ? Items.AIR : Registries.ITEM.get(identifier);
    }

    private void startItemSelection() {
        if (mc.player != null) {
            this.selectingItem = true;
            mc.setScreen(new InventoryScreen(mc.player));
        }
    }

    public void renderSlotHighlights(DrawContext drawContext) {
        if (this.selectingItem && mc.player != null) {
            Item item2 = this.getSelectedItem();
            int i = Theme.getPrimary();

            for (Slot slot : mc.player.currentScreenHandler.slots) {
                if (slot.hasStack() && slot.getStack().isOf(item2)) {
                    PixelRenderUtil.drawRect(drawContext, slot.x, slot.y, 16.0F, 16.0F, i);
                }
            }
        }
    }

    public boolean handleSlotClick(int i, int i2, SlotActionType slotActionType) {
        if (this.selectingItem && mc.player != null && mc.currentScreen instanceof InventoryScreen) {
            if (i >= 0 && i2 == 0 && slotActionType == SlotActionType.PICKUP) {
                ItemStack itemStack = mc.player.currentScreenHandler.getSlot(i).getStack();
                if (!itemStack.isEmpty()) {
                    ((Setting<Object>) (Setting<?>) this.item).setValue((Object) Registries.ITEM.getId(itemStack.getItem()).toString());
                    mc.setScreen(null);
                    this.selectingItem = false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private void placeSphereTarget() {
        Spider.PlacementTarget placementTarget = this.currentTarget;
        if (placementTarget != null) {
            this.currentTarget = null;
            ItemStack itemStack = mc.player.getOffHandStack();
            Item item2 = Items.PLAYER_HEAD;
            if (itemStack.isOf(item2)) {
                ClientWorld clientWorld = mc.world;
                BlockPos blockPos = placementTarget.target();
                if (clientWorld.getBlockState(blockPos).isReplaceable()) {
                    BlockPos blockPos2 = placementTarget.target();
                    Direction direction = placementTarget.side();
                    if (this.canPlaceLadder(blockPos2, direction)) {
                        Vec2f vec2f = RotationUtil.getRotationsTo(placementTarget.hit());
                        float f = mc.player.getYaw();
                        float f2 = vec2f.x;
                        float f3 = Math.abs(MathHelper.wrapDegrees(ExactMath.subtract(f, f2)));
                        if (6.0F > f3) {
                            float f4 = mc.player.getPitch();
                            float f5 = vec2f.y;
                            float f6 = Math.abs(ExactMath.subtract(f4, f5));
                            if (6.0F > f6) {
                                ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
                                ClientPlayerEntity clientPlayerEntity = mc.player;
                                Hand hand = Hand.OFF_HAND;
                                Vec3d vec3d = placementTarget.hit();
                                Direction direction2 = placementTarget.side();
                                BlockPos blockPos3 = placementTarget.pos();
                                BlockHitResult blockHitResult = new BlockHitResult(vec3d, direction2, blockPos3, false);
                                clientPlayerInteractionManager.interactBlock(clientPlayerEntity, hand, blockHitResult);
                                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                                Hand hand2 = Hand.OFF_HAND;
                                clientPlayerEntity2.swingHand(hand2);
                                this.sphereTimer.reset();
                            }
                        }
                    }
                }
            }
        } else {
            this.currentTarget = null;
        }
    }

    @EventHandler
    public void onMove(MoveEvent moveEvent) {
        if (!ServerManager.isNullWorld()) {
            ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
            ModeSetting modeSetting = this.mode;
            if (clientPlayerInteractionManager != null) {
                boolean flag = modeSetting.is("Blockspoof");
                boolean flag2 = this.pendingInteract;
                if (flag) {
                    if (flag2) {
                        this.pendingInteract = false;
                        sendStopDestroyPacket();
                        moveEvent.setOnGround(true);
                        mc.player.setOnGround(true);
                        Vec3d vec3d = mc.player.getVelocity();
                        ClientPlayerEntity clientPlayerEntity = mc.player;
                        double d = vec3d.x;
                        float f = ((LivingEntityAccessor)mc.player).sadnes$invokeGetJumpVelocity();
                        double d2 = vec3d.z;
                        double d3 = f;
                        clientPlayerEntity.setVelocity(d, d3, d2);
                        return;
                    }
                } else {
                    if (modeSetting.is("Ladders")) {
                        this.handleMove(moveEvent);
                        return;
                    }

                    if (mc.options.jumpKey.isPressed() && mc.player.horizontalCollision) {
                        if (modeSetting.is("Old polar")) {
                            Box box = mc.player.getBoundingBox().expand(-0.001);
                            double d4 = box.minX;
                            double d5 = box.minY;
                            double d6 = box.minZ;
                            double d7 = box.maxX;
                            double d8 = box.minY;
                            double d9 = box.maxZ;
                            double d10 = ExactMath.add(d8, 0.5);
                            Box box2 = new Box(d4, d5, d6, d7, d10, d9);
                            Predicate<BlockPos> predicate = this::hasCollision;
                            if (this.anyBlockMatches(box2, predicate)) {
                                double d11 = box.minX;
                                double d12 = box.minY;
                                double d13 = box.minZ;
                                double d14 = box.maxX;
                                double d15 = box.maxY;
                                double d16 = box.maxZ;
                                double d17 = ExactMath.add(d12, 1.0);
                                Box box3 = new Box(d11, d17, d13, d14, d15, d16);
                                Predicate<BlockPos> predicate2 = this::hasCollision;
                                if (this.anyBlockMatches(box3, predicate2)) {
                                    moveEvent.setOnGround(true);
                                    mc.player.setOnGround(true);
                                    ClientPlayerEntity clientPlayerEntity2 = mc.player;
                                    double d18 = mc.player.getVelocity().x;
                                    double d19 = mc.player.getVelocity().z;
                                    clientPlayerEntity2.setVelocity(d18, 0.42F, d19);
                                    return;
                                }

                                moveEvent.setOnGround(true);
                                mc.player.setOnGround(true);
                                mc.player.jump();
                                return;
                            }
                        } else {
                            boolean flag3 = modeSetting.is("Matrix");
                            StopWatch stopWatch = this.offhandSwapTimer;
                            if (flag3 && stopWatch.hasPassedAndReset(140.0)) {
                                mc.player.getHorizontalFacing();
                                ClientWorld clientWorld = mc.world;
                                Vec3d vec3d2 = mc.player.getCameraPosVec(1.0F);
                                Vec3d vec3d3 = mc.player.getCameraPosVec(1.0F);
                                float f2 = mc.player.getPitch();
                                float f3 = mc.player.getYaw();
                                Vec3d vec3d4 = Vec3d.fromPolar(f2, f3).multiply(3.0);
                                Vec3d vec3d5 = vec3d3.add(vec3d4);
                                ShapeType shapeType = ShapeType.OUTLINE;
                                FluidHandling fluidHandling = FluidHandling.SOURCE_ONLY;
                                ClientPlayerEntity clientPlayerEntity3 = mc.player;
                                RaycastContext raycastContext = new RaycastContext(vec3d2, vec3d5, shapeType, fluidHandling, clientPlayerEntity3);
                                BlockHitResult blockHitResult = clientWorld.raycast(raycastContext);
                                Type type = blockHitResult.getType();
                                Type type2 = Type.BLOCK;
                                if (type == type2) {
                                    ClientPlayerInteractionManager clientPlayerInteractionManager2 = mc.interactionManager;
                                    ClientPlayerEntity clientPlayerEntity4 = mc.player;
                                    Hand hand = Hand.MAIN_HAND;
                                    clientPlayerInteractionManager2.interactItem(clientPlayerEntity4, hand);
                                    ClientPlayerInteractionManager clientPlayerInteractionManager3 = mc.interactionManager;
                                    ClientPlayerEntity clientPlayerEntity5 = mc.player;
                                    Hand hand2 = Hand.MAIN_HAND;
                                    Vec3d vec3d6 = blockHitResult.getPos();
                                    Direction direction = blockHitResult.getSide();
                                    BlockPos blockPos = blockHitResult.getBlockPos();
                                    boolean flag4 = blockHitResult.isInsideBlock();
                                    BlockHitResult blockHitResult2 = new BlockHitResult(vec3d6, direction, blockPos, flag4);
                                    clientPlayerInteractionManager3.interactBlock(clientPlayerEntity5, hand2, blockHitResult2);
                                    ClientPlayerEntity clientPlayerEntity6 = mc.player;
                                    Hand hand3 = Hand.MAIN_HAND;
                                    clientPlayerEntity6.swingHand(hand3);
                                    ClientPlayerEntity clientPlayerEntity7 = mc.player;
                                    double d20 = mc.player.getVelocity().x;
                                    double d21 = mc.player.getVelocity().z;
                                    clientPlayerEntity7.setVelocity(d20, 0.42, d21);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleMove(MoveEvent moveEvent) {
        Slot slot = InventoryUtil.findSlotReverse(Items.LADDER);
        if (slot != null) {
            ItemStack itemStack = mc.player.getMainHandStack();
            Item item2 = Items.LADDER;
            if (!itemStack.isOf(item2)) {
                Integer integer = slot.id;
                this.originalMainHandSlot = integer;
                int i = slot.id;
                Hand hand = Hand.MAIN_HAND;
                InventoryUtil.swapToHand(i, hand);
                return;
            }

            if (mc.options.jumpKey.isPressed()) {
                Direction direction = mc.player.getHorizontalFacing();
                double d = mc.player.getX();
                double d2 = mc.player.getY();
                double d3 = mc.player.getZ();
                double d4 = ExactMath.add(d2, 1.0);
                Vec3d vec3d = new Vec3d(d, d4, d3);
                Vec3d vec3d2 = Vec3d.of(direction.getVector()).multiply(0.5);
                Vec3d vec3d3 = vec3d.add(vec3d2);
                ClientWorld clientWorld = mc.world;
                ShapeType shapeType = ShapeType.COLLIDER;
                FluidHandling fluidHandling = FluidHandling.NONE;
                ClientPlayerEntity clientPlayerEntity = mc.player;
                RaycastContext raycastContext = new RaycastContext(vec3d, vec3d3, shapeType, fluidHandling, clientPlayerEntity);
                BlockHitResult blockHitResult = clientWorld.raycast(raycastContext);
                Type type = blockHitResult.getType();
                Type type2 = Type.BLOCK;
                if (type != type2) {
                    return;
                }

                Vec3d vec3d4 = mc.player.getEyePos();
                Vec3d vec3d5 = blockHitResult.getPos();
                float f = RotationUtil.getRotations(vec3d4, vec3d5).x;
                RotationComponent.rotate(new Rotation(f, -59.0F), 60.0F, 60.0F, 10.0F, 10.0F, 2, 10, false);
                if (!mc.player.horizontalCollision) {
                    ClientWorld clientWorld2 = mc.world;
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    Direction direction2 = blockHitResult.getSide();
                    BlockPos blockPos2 = blockPos.offset(direction2);
                    if (clientWorld2.isAir(blockPos2)) {
                        ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
                        ClientPlayerEntity clientPlayerEntity2 = mc.player;
                        Hand hand2 = Hand.MAIN_HAND;
                        clientPlayerInteractionManager.interactBlock(clientPlayerEntity2, hand2, blockHitResult);
                        ClientPlayerEntity clientPlayerEntity3 = mc.player;
                        Hand hand3 = Hand.MAIN_HAND;
                        clientPlayerEntity3.swingHand(hand3);
                        this.offhandSwapTimer.reset();
                    }
                }

                ClientWorld clientWorld3 = mc.world;
                double d5 = mc.player.getX();
                double d6 = mc.player.getBoundingBox().minY;
                double d7 = mc.player.getBoundingBox().maxY;
                double d8 = mc.player.getZ();
                double d9 = ExactMath.multiply(ExactMath.add(d6, d7), 0.5);
                BlockPos blockPos3 = BlockPos.ofFloored(d5, d9, d8);
                BlockState blockState = clientWorld3.getBlockState(blockPos3);
                Block block = Blocks.LADDER;
                if (blockState.isOf(block)) {
                    moveEvent.setOnGround(true);
                    mc.player.setOnGround(true);
                    moveEvent.setY(0.6);
                    moveEvent.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void aimSphereTarget() {
        ItemStack itemStack = mc.player.getOffHandStack();
        Item item2 = Items.PLAYER_HEAD;
        if (itemStack.isOf(item2)) {
            boolean flag = mc.player.isOnGround();
            StopWatch stopWatch = this.sphereTimer;
            if (flag) {
                return;
            }

            if (stopWatch.hasPassed(50.0)) {
                Spider.PlacementTarget placementTarget = this.findLadderTarget();
                if (placementTarget != null) {
                    Vec2f vec2f = RotationUtil.getRotationsTo(placementTarget.hit());
                    float f = vec2f.x;
                    float f2 = vec2f.y;
                    RotationComponent.rotateTo(new Rotation(f, f2), 999.0F, 999.0F, 2, 5);
                    this.currentTarget = placementTarget;
                    return;
                }

                return;
            }
        }
    }

    private Spider.PlacementTarget findLadderTarget() {
        double d = mc.player.getVelocity().y;
        if (!(d >= 0.0)) {
            double d2 = mc.player.getY();
            double d3 = mc.player.getX();
            double d4 = mc.player.getZ();
            double d5 = ExactMath.add(ExactMath.add(ExactMath.add(d, d2), -0.75), -0.001);
            BlockPos blockPos = BlockPos.ofFloored(d3, d5, d4);
            Spider.PlacementTarget placementTarget = this.findLadderTargetAt(blockPos);
            if (placementTarget == null) {
                BlockPos blockPos2 = blockPos.down();
                return this.findLadderTargetAt(blockPos2);
            } else {
                return placementTarget;
            }
        } else {
            return null;
        }
    }

    private Spider.PlacementTarget findLadderTargetAt(BlockPos blockPos) {
        if (!mc.world.getBlockState(blockPos).isReplaceable()) {
            return null;
        }

        Iterator iterator2 = net.minecraft.util.math.Direction.Type.HORIZONTAL.iterator();
        if (!iterator2.hasNext()) {
            return null;
        }

        do {
            Direction direction = (Direction)iterator2.next();
            BlockPos blockPos2 = blockPos.offset(direction);
            if (mc.world.getBlockState(blockPos2).isSolid()) {
                Direction direction2 = direction.getOpposite();
                if (this.canPlaceLadder(blockPos, direction2)) {
                    Vec3d vec3d = getRandomizedFaceHit(blockPos2, direction2);
                    if (!(mc.player.getEyePos().distanceTo(vec3d) > 4.4)) {
                        return new Spider.PlacementTarget(blockPos2, direction2, vec3d, blockPos);
                    }
                }
            }
        } while (iterator2.hasNext());

        return null;
    }

    private boolean canPlaceLadder(BlockPos blockPos, Direction direction) {
        BlockState blockState = Blocks.PLAYER_WALL_HEAD.getDefaultState();
        EnumProperty enumProperty = HorizontalFacingBlock.FACING;
        BlockState blockState2 = blockState.with(enumProperty, direction);
        ClientWorld clientWorld = mc.world;
        ShapeContext shapeContext = ShapeContext.absent();
        return clientWorld.canPlace(blockState2, blockPos, shapeContext);
    }

    private static Vec3d getRandomizedFaceHit(BlockPos blockPos, Direction direction) {
        Vec3d vec3d = blockPos.toCenterPos().add(Vec3d.of(direction.getVector()).multiply(0.5));
        double d = direction.getOffsetX() == 0 ? MathUtil.randomBetween(-0.22, 0.22) : 0.0;
        double d2 = direction.getOffsetY() == 0 ? MathUtil.randomBetween(-0.22, 0.22) : 0.0;
        if (direction.getOffsetZ() == 0) {
            double d3 = MathUtil.randomBetween(-0.22, 0.22);
            return vec3d.add(d, d2, d3);
        } else {
            return vec3d.add(d, d2, 0.0);
        }
    }

    private boolean anyBlockMatches(Box box, Predicate<BlockPos> predicate) {
        return BlockPos.stream(box).anyMatch(predicate);
    }

    private boolean hasCollision(BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        ClientWorld clientWorld = mc.world;
        return !blockState.getCollisionShape(clientWorld, blockPos).isEmpty();
    }

    public void setSelectingItem(boolean flag) {
        this.selectingItem = flag;
    }

    static final class DelayedAction {
        private final StopWatch timer = new StopWatch();
        private Runnable action;

        void schedule(Runnable runnable) {
            this.action = runnable;
        }

        Spider.DelayedAction runPending() {
            Runnable runnable = this.action;
            this.action = null;
            if (runnable != null) {
                runnable.run();
            }

            return this;
        }

        Spider.DelayedAction reset() {
            this.action = null;
            this.timer.reset();
            return this;
        }

        boolean isIdle() {
            return this.action == null;
        }

        boolean hasElapsed(long l) {
            return this.timer.hasPassed((double)l);
        }
    }

    record PlacementTarget(BlockPos pos, Direction side, Vec3d hit, BlockPos target) {
    }
}
