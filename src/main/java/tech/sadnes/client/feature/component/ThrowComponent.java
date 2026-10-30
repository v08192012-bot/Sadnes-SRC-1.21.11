package tech.sadnes.client.feature.component;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.BundleItemSelectedC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import tech.sadnes.api.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import tech.sadnes.api.mixin.accessor.PlayerInventoryAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PlayerInputPacketEvent;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.event.impl.PostMotionEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.feature.impl.combat.AutoTotem;
import tech.sadnes.client.feature.impl.movement.GuiWalk;
import tech.sadnes.client.feature.impl.player.ClickAction;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.ActionSequence;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.compat.ProtocolUtil;
import tech.sadnes.client.util.function.Action;
import tech.sadnes.client.util.math.TimerUtil;
import tech.sadnes.client.util.network.PacketQueue;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.network.TickPacketOrderTracker;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.MoveUtil;

public final class ThrowComponent extends FeatureComponent {
    private static final int HOTBAR_SIZE = 9;
    private static final int ROTATION_HOLD_MS = 400;
    public boolean blockMovement;
    private boolean swapping;
    static float funTimeDelay = 2.0F;
    private static final ThrowComponent.SwapTiming DEFAULT_TIMING = new ThrowComponent.SwapTiming(2, 2, 2, 2);
    private static final ThrowComponent.SwapTiming FUNTIME_TIMING = new ThrowComponent.SwapTiming(
        (int)funTimeDelay, (int)funTimeDelay, (int)funTimeDelay, (int)funTimeDelay
    );
    private static final ThrowComponent.SwapTiming SLOW_TIMING = new ThrowComponent.SwapTiming(50, 50, 150, 50);
    private static final ThrowComponent.SwapTiming SPOOKYTIME_TIMING = new ThrowComponent.SwapTiming(50, 50, 50, 50);
    private static final ThrowComponent.SwapTiming FUNSKY_TIMING = new ThrowComponent.SwapTiming(45, 111, 55, 111);
    private ThrowComponent.SwapTiming timing = DEFAULT_TIMING;
    public static TimerUtil rotationTimer = new TimerUtil();
    ActionSequence secondaryScheduler = new ActionSequence();
    ActionSequence scheduler = new ActionSequence();
    private final Deque<Runnable> actionQueue = new ArrayDeque<>();
    private Runnable pendingAction;
    private boolean awaitingInventoryClose;
    private Runnable deferredAction;
    private boolean silentSwapActive;
    private int suppressInputTicks;
    private boolean rotating;

    public static ThrowComponent getInstance() {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        return componentManager.getComponent(ThrowComponent.class);
    }

    public boolean isIdle() {
        return this.scheduler.isFinished()
            && this.actionQueue.isEmpty()
            && this.pendingAction == null
            && !this.awaitingInventoryClose
            && this.deferredAction == null;
    }

    public boolean isRotatingBusy() {
        return this.rotating && !this.isIdle();
    }

    public boolean isDonutBusy() {
        return ServerManager.isDonut() && (this.silentSwapActive || !this.isIdle());
    }

    @EventHandler
    public void onPostTick(PlayerTickEvent playerTickEvent) {
        this.silentSwapActive = false;
        if (this.deferredAction != null) {
            if (mc.player == null) {
                this.deferredAction = null;
                this.suppressInputTicks = 0;
            } else {
                AutoTotem autoTotem = AutoTotem.getInstance();
                if (!ServerManager.isDonut() || !autoTotem.isSwapping() && TickPacketOrderTracker.isTickClean()) {
                    Runnable runnable = this.deferredAction;
                    this.deferredAction = null;
                    if (mc.player != null) {
                        runnable.run();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player == null) {
            this.actionQueue.clear();
            this.pendingAction = null;
            this.deferredAction = null;
            this.scheduler.clear();
            this.swapping = false;
        } else {
            if (this.pendingAction != null && !this.awaitingInventoryClose) {
                Runnable runnable = this.pendingAction;
                this.pendingAction = null;
                runnable.run();
            }

            if (this.pendingAction == null && !this.actionQueue.isEmpty()) {
                this.pendingAction = this.actionQueue.poll();
                this.awaitingInventoryClose = true;
                PacketQueue.queue(() -> mc.player.setSprinting(false));
            }

            this.scheduler.tick();
            this.secondaryScheduler.tick();
            if (this.scheduler.isFinished() && this.deferredAction == null) {
                this.swapping = false;
            }

            if (this.isIdle()) {
                this.rotating = false;
            }

            if (ServerManager.isFunTime()) {
                this.timing = FUNTIME_TIMING;
            } else if (ServerManager.isServer("funsky")) {
                this.timing = FUNSKY_TIMING;
            } else if (ServerManager.isHolyWorld() || ServerManager.isServer("space-times")) {
                this.timing = new ThrowComponent.SwapTiming(
                    CombatUtil.randomInt(25, 60), CombatUtil.randomInt(25, 60), CombatUtil.randomInt(70, 200), CombatUtil.randomInt(60, 70)
                );
            } else if (ServerManager.isSpookyTime()) {
                this.timing = SPOOKYTIME_TIMING;
            } else {
                this.timing = DEFAULT_TIMING;
            }
        }
    }

    @EventHandler(priority = -500)
    private void onMovementInput(MoveInputEvent moveInputEvent) {
        if (this.suppressInputTicks > 0) {
            this.suppressInputTicks--;
            moveInputEvent.setCancelled(true);
        } else if (this.awaitingInventoryClose) {
            moveInputEvent.setCancelled(true);
        } else {
            if (this.blockMovement) {
                moveInputEvent.setStrafe(0.0F);
                moveInputEvent.setForward(0.0F);
                moveInputEvent.setJump(false);
                moveInputEvent.setSneak(false);
            }

            if (!rotationTimer.isReached()) {
                moveInputEvent.setJump(true);
            }
        }
    }

    @EventHandler
    private void onInputUpdate(PlayerInputPacketEvent playerInputPacketEvent) {
        Runnable runnable = this.pendingAction;
        boolean flag = this.awaitingInventoryClose;
        if (runnable != null && flag) {
            PlayerInput playerInput = PlayerInput.DEFAULT;
            playerInputPacketEvent.setInput(playerInput);
            Runnable runnable2 = this.pendingAction;
            playerInputPacketEvent.addCallback(runnable2);
            this.pendingAction = null;
        }
    }

    @EventHandler
    private void onInventoryClosed(PostMotionEvent postMotionEvent) {
        this.awaitingInventoryClose = false;
    }

    public void queueHotbarSwapUse(int i, boolean flag, Hand hand, Runnable runnable) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        ActionSequence actionSequence = this.scheduler;
        if (clientPlayerEntity != null) {
            boolean flag2 = actionSequence.isFinished();
            Runnable runnable2 = this.pendingAction;
            Deque deque = this.actionQueue;
            if (flag2 && runnable2 == null && deque.isEmpty()) {
                int i2;
                if (Hand.OFF_HAND != hand) {
                    i2 = this.getSelectedSlot();
                } else {
                    i2 = 40;
                }

                Runnable runnable3 = () -> this.swapSlotWithHotbar(i, i2);
                deque.add(runnable3);
                Runnable runnable4 = () -> {
                    if (flag) {
                        rotationTimer.setDelay(400L);
                    }

                    runnable.run();
                };
                deque.add(runnable4);
                Runnable runnable5 = () -> this.swapSlotWithHotbar(i, i2);
                deque.add(runnable5);
                Runnable runnable6 = this::closeInventoryScreen;
                deque.add(runnable6);
                return;
            }
        }
    }

    public void queueInventorySwapUse(int i, int i2, int i3, boolean flag, Hand hand, Runnable runnable) {
        int i4;
        Deque deque;
        label92: {
            Deque deque2;
            label93: {
                Deque deque3;
                label94: {
                    ClientPlayerEntity clientPlayerEntity = mc.player;
                    ActionSequence actionSequence = this.scheduler;
                    Deque deque4;
                    if (i3 >= 0) {
                        if (clientPlayerEntity == null) {
                            return;
                        }

                        boolean flag2 = actionSequence.isFinished();
                        Runnable runnable2 = this.pendingAction;
                        Deque deque5 = this.actionQueue;
                        if (!flag2 || runnable2 != null || !deque5.isEmpty()) {
                            return;
                        }

                        if (Hand.OFF_HAND != hand) {
                            i4 = this.getSelectedSlot();
                            boolean flag3 = ProtocolUtil.isOlderThan("26.2");
                            deque = deque5;
                            if (flag3) {
                                break label92;
                            }

                            Runnable runnable3 = () -> PacketSender.sendSilent(new BundleItemSelectedC2SPacket(i, i3));
                            deque5.add(runnable3);
                            deque2 = deque5;
                            break label93;
                        }

                        boolean flag4 = ProtocolUtil.isOlderThan("26.2");
                        deque3 = deque5;
                        if (flag4) {
                            break label94;
                        }

                        Runnable runnable4 = () -> PacketSender.sendSilent(new BundleItemSelectedC2SPacket(i, i3));
                        deque5.add(runnable4);
                        deque4 = deque5;
                    } else {
                        if (clientPlayerEntity == null) {
                            return;
                        }

                        boolean flag5 = actionSequence.isFinished();
                        Runnable runnable5 = this.pendingAction;
                        Deque deque6 = this.actionQueue;
                        if (!flag5) {
                            return;
                        }

                        if (runnable5 != null) {
                            return;
                        }

                        if (!deque6.isEmpty()) {
                            return;
                        }

                        if (Hand.OFF_HAND != hand) {
                            i4 = this.getSelectedSlot();
                            boolean flag6 = ProtocolUtil.isOlderThan("26.2");
                            deque = deque6;
                            if (flag6) {
                                break label92;
                            }

                            deque2 = deque6;
                            break label93;
                        }

                        boolean flag7 = ProtocolUtil.isOlderThan("26.2");
                        deque3 = deque6;
                        if (flag7) {
                            break label94;
                        }

                        deque4 = deque6;
                    }

                    Runnable runnable6 = () -> this.clickSlot(i, 1, SlotActionType.PICKUP);
                    deque4.add(runnable6);
                    Deque deque7 = this.actionQueue;
                    Runnable runnable7 = () -> this.clickSlot(i2, 0, SlotActionType.PICKUP);
                    deque7.add(runnable7);
                    Deque deque8 = this.actionQueue;
                    Runnable runnable8 = () -> this.clickSlot(i2, 40, SlotActionType.SWAP);
                    deque8.add(runnable8);
                    Deque deque9 = this.actionQueue;
                    Runnable runnable9 = () -> {
                        if (flag) {
                            rotationTimer.setDelay(400L);
                        }

                        runnable.run();
                    };
                    deque9.add(runnable9);
                    Deque deque10 = this.actionQueue;
                    Runnable runnable10 = () -> this.clickSlot(i2, 40, SlotActionType.SWAP);
                    deque10.add(runnable10);
                    Deque deque11 = this.actionQueue;
                    Runnable runnable11 = () -> {
                        if (!mc.player.currentScreenHandler.getSlot(i2).getStack().isEmpty()) {
                            this.clickSlot(i2, 0, SlotActionType.PICKUP);
                        }
                    };
                    deque11.add(runnable11);
                    Deque deque12 = this.actionQueue;
                    Runnable runnable12 = () -> {
                        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                            this.clickSlot(i, 0, SlotActionType.PICKUP);
                        }
                    };
                    deque12.add(runnable12);
                    Deque deque13 = this.actionQueue;
                    Runnable runnable13 = this::closeInventoryScreen;
                    deque13.add(runnable13);
                    return;
                }

                Runnable runnable14 = () -> {
                    if (i3 >= 0) {
                        PacketSender.sendSilent(new BundleItemSelectedC2SPacket(i, i3));
                    }

                    this.clickSlot(i, 1, SlotActionType.PICKUP);
                    this.clickSlot(i2, 0, SlotActionType.PICKUP);
                    this.swapSlotWithHotbar(i2, 40);
                };
                deque3.add(runnable14);
                Deque deque14 = this.actionQueue;
                Runnable runnable15 = () -> {
                    if (flag) {
                        rotationTimer.setDelay(400L);
                    }

                    runnable.run();
                };
                deque14.add(runnable15);
                Deque deque15 = this.actionQueue;
                Runnable runnable16 = () -> {
                    boolean flag8 = this.getStackCount(i, i3) > 1;
                    this.swapSlotWithHotbar(i2, 40);
                    this.closeInventoryScreen();
                    if (flag8) {
                        this.clickSlot(i2, 0, SlotActionType.PICKUP);
                        this.clickSlot(i, 0, SlotActionType.PICKUP);
                    }
                };
                deque15.add(runnable16);
                return;
            }

            Runnable runnable17 = () -> this.clickSlot(i, 1, SlotActionType.PICKUP);
            deque2.add(runnable17);
            Deque deque16 = this.actionQueue;
            Runnable runnable18 = () -> this.clickSlot(i2, 0, SlotActionType.PICKUP);
            deque16.add(runnable18);
            Deque deque17 = this.actionQueue;
            Runnable runnable19 = () -> this.clickSlot(i2, i4, SlotActionType.SWAP);
            deque17.add(runnable19);
            Deque deque18 = this.actionQueue;
            Runnable runnable20 = () -> {
                if (flag) {
                    rotationTimer.setDelay(400L);
                }

                runnable.run();
            };
            deque18.add(runnable20);
            Deque deque19 = this.actionQueue;
            Runnable runnable21 = () -> this.clickSlot(i2, i4, SlotActionType.SWAP);
            deque19.add(runnable21);
            Deque deque20 = this.actionQueue;
            Runnable runnable22 = () -> {
                if (!mc.player.currentScreenHandler.getSlot(i2).getStack().isEmpty()) {
                    this.clickSlot(i2, 0, SlotActionType.PICKUP);
                }
            };
            deque20.add(runnable22);
            Deque deque21 = this.actionQueue;
            Runnable runnable23 = () -> {
                if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                    this.clickSlot(i, 0, SlotActionType.PICKUP);
                }
            };
            deque21.add(runnable23);
            Deque deque22 = this.actionQueue;
            Runnable runnable24 = this::closeInventoryScreen;
            deque22.add(runnable24);
            return;
        }

        Runnable runnable25 = () -> {
            if (i3 >= 0) {
                PacketSender.sendSilent(new BundleItemSelectedC2SPacket(i, i3));
            }

            this.clickSlot(i, 1, SlotActionType.PICKUP);
            this.clickSlot(i2, 0, SlotActionType.PICKUP);
            this.swapSlotWithHotbar(i2, i4);
        };
        deque.add(runnable25);
        Deque deque23 = this.actionQueue;
        Runnable runnable26 = () -> {
            if (flag) {
                rotationTimer.setDelay(400L);
            }

            runnable.run();
        };
        deque23.add(runnable26);
        Deque deque24 = this.actionQueue;
        Runnable runnable27 = () -> {
            boolean flag9 = this.getStackCount(i, i3) > 1;
            this.swapSlotWithHotbar(i2, i4);
            this.closeInventoryScreen();
            if (flag9) {
                this.clickSlot(i2, 0, SlotActionType.PICKUP);
                this.clickSlot(i, 0, SlotActionType.PICKUP);
            }
        };
        deque24.add(runnable27);
    }

    public boolean queueActions(Runnable... runnables) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        ActionSequence actionSequence = this.scheduler;
        Runnable runnable = this.pendingAction;
        Deque deque = this.actionQueue;
        if (clientPlayerEntity != null) {
            ((Long)(Object)runnable).longValue();
            actionSequence.isFinished();
            Runnable runnable2 = this.pendingAction;
            if (actionSequence.isFinished()) {
                long l = (Long)(Object)runnable2;
                if (l != 0L) {
                    return false;
                } else {
                    deque.isEmpty();
                    Runnable runnable3 = this.pendingAction;
                    if (deque.isEmpty()) {
                        ((Long)(Object)runnable3).longValue();
                        Collections.addAll(deque, runnables);
                        return true;
                    } else {
                        ((Long)(Object)runnable3).longValue();
                        return false;
                    }
                }
            } else {
                ((Long)(Object)runnable2).longValue();
                return false;
            }
        } else {
            ((Long)(Object)runnable).longValue();
            return false;
        }
    }

    public void swapSlotWithHotbar(int i, int i2) {
        if (ProtocolUtil.isOlderThan("26.2") && !ServerManager.isServer("funsky")) {
            SlotActionType slotActionType = SlotActionType.PICKUP;
            this.clickSlot(0, 0, slotActionType);
        }

        SlotActionType slotActionType2 = SlotActionType.SWAP;
        this.clickSlot(i, i2, slotActionType2);
        if (ServerManager.isServer("lony")) {
            GuiMoveComponent.sendFakeClick();
        } else if (ServerManager.isServer("donut")) {
            GuiMoveComponent.sendFakeClick();
        }
    }

    private void clickSlot(int i, int i2, SlotActionType slotActionType) {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
        int i3 = screenHandler.syncId;
        ClientPlayerEntity clientPlayerEntity = mc.player;
        clientPlayerInteractionManager.clickSlot(i3, i, i2, slotActionType, clientPlayerEntity);
    }

    public void useItem(Item item) {
        if (!ServerManager.isServer("holy")) {
            Hand hand = mc.player.getActiveHand();
            Hand hand2 = Hand.MAIN_HAND;
            if (hand != hand2) {
                Hand hand3 = Hand.MAIN_HAND;
                this.useItemInHand(item, hand3);
            } else if (mc.player.getItemUseTime() != 0) {
                Hand hand4 = Hand.OFF_HAND;
                this.useItemInHand(item, hand4);
            } else {
                Hand hand5 = Hand.MAIN_HAND;
                this.useItemInHand(item, hand5);
            }
        } else {
            Hand hand6 = Hand.OFF_HAND;
            this.useItemInHand(item, hand6);
        }
    }

    public void useItemInHand(Item item, Hand hand) {
        Slot slot = InventoryUtil.findSlotReverse(item);
        if (slot != null) {
            this.useSlotFull(slot, hand, null);
        } else {
            Text text = item.getName();
            MutableText mutableText = this.formatMessage("throwutil.noItem", "РџСЂРµРґРјРµС‚Р° %s РЅРµС‚.", text);
            NotificationType notificationType = NotificationType.ERROR;
            String string = NotificationSources.inventory();
            ItemStack itemStack = item.getDefaultStack();
            NotificationUtil.notify(notificationType, string, itemStack, mutableText);
        }
    }

    public void useStack(ItemStack itemStack) {
        Hand hand = Hand.MAIN_HAND;
        this.useStackInHand(itemStack, hand);
    }

    public void useStackInHand(ItemStack itemStack, Hand hand) {
        Slot slot = InventoryUtil.findSlotByStack(itemStack);
        if (slot != null) {
            this.useSlotFull(slot, hand, null);
        } else {
            Text text = itemStack.getName();
            MutableText mutableText = this.formatMessage("throwutil.noItem", "РџСЂРµРґРјРµС‚Р° %s РЅРµС‚.", text);
            NotificationType notificationType = NotificationType.ERROR;
            String string = NotificationSources.inventory();
            NotificationUtil.notify(notificationType, string, itemStack, mutableText);
        }
    }

    public void useSlot(Slot slot) {
        Hand hand = Hand.MAIN_HAND;
        this.useSlotInHand(slot, hand);
    }

    public void useSlotInHand(Slot slot, Hand hand) {
        if (slot != null) {
            this.useSlotFull(slot, hand, null);
        }
    }

    public void useSlotWithRotation(Slot slot, Vector2f vector2f) {
        Hand hand = Hand.MAIN_HAND;
        this.useSlotFull(slot, hand, vector2f);
    }

    public void useSlotRotating(Slot slot, Hand hand) {
        this.rotating = true;
        this.useSlotFull(slot, hand, null);
    }

    public void useSlotFull(Slot slot, Hand hand, Vector2f vector2f) {
        if (slot != null && mc.player != null) {
            boolean flag;
            label105: {
                label112: {
                    if (!mc.player.isUsingItem() && !mc.options.useKey.isPressed()) {
                        int i = Key.MOUSE_RIGHT.getKeyCode();
                        if (i < 0) {
                            break label112;
                        }

                        MinecraftClient minecraftClient = MinecraftClient.getInstance();
                        if (minecraftClient == null) {
                            flag = false;
                            break label105;
                        }

                        if (minecraftClient.getWindow() == null) {
                            flag = false;
                            break label105;
                        }

                        long l = minecraftClient.getWindow().getHandle();
                        if (l == 0L) {
                            flag = false;
                            break label105;
                        }

                        if (Integer.compareUnsigned(i, 7) > 0) {
                            int i2 = i & 2147483647;
                            if (GLFW.glfwGetKey(l, i2) != 1) {
                                break label112;
                            }
                        } else {
                            int i3 = i & 7;
                            if (GLFW.glfwGetMouseButton(l, i3) != 1) {
                                break label112;
                            }
                        }
                    }

                    if (ServerManager.isServer("donut")) {
                        return;
                    }

                    flag = true;
                    break label105;
                }

                flag = false;
            }

            ItemStack itemStack = slot.getStack();
            Item item = Items.FIREWORK_ROCKET;
            if (!itemStack.isOf(item) && mc.player.getItemCooldownManager().isCoolingDown(itemStack)) {
                Text text = itemStack.getName();
                MutableText mutableText = this.formatMessage("throwutil.cooldown", "РџСЂРµРґРјРµС‚ %s РµС‰С‘ РІ РєРґ.", text);
                NotificationType notificationType = NotificationType.ERROR;
                String string = NotificationSources.inventory();
                NotificationUtil.notify(notificationType, string, itemStack, mutableText);
            } else {
                int i4 = slot.id;
                int i5 = InventoryUtil.toHotbarIndex(i4);
                if ((Hand.MAIN_HAND != hand ? ServerManager.isServer("donut") && i5 != -1 && i5 < 9 : i5 != -1 && i5 < 9) && !flag) {
                    this.useHotbarSlotWithRotation(i5, vector2f);
                } else if (ServerManager.isServer("donut")) {
                    this.deferUseMatchingStack(itemStack, vector2f);
                } else if (ServerManager.isServer("spookytime")) {
                    this.useWithLegitSwap(i4, hand, vector2f);
                } else if (ServerManager.isServer("dildo")) {
                    this.useWithLegitSwap(i4, hand, vector2f);
                } else {
                    ItemStack itemStack2 = slot.getStack();
                    boolean flag2 = this.shouldDelayRotation(itemStack2);
                    Hand hand2 = Hand.MAIN_HAND;
                    if (hand == hand2 && this.supportsSilentSwap() && !flag) {
                        this.queueSilentHotbarUse(i4, flag2, vector2f);
                    } else {
                        GuiWalk guiWalk = GuiWalk.getInstance();
                        if (ProtocolUtil.isOlderThan("26.2") && !guiWalk.isBypassActive()) {
                            Runnable runnable = () -> this.interactItem(vector2f, hand);
                            this.scheduleHotbarSwapUse(i4, flag2, hand, runnable);
                        } else {
                            Runnable runnable2 = () -> this.interactItem(vector2f, hand);
                            this.queueHotbarSwapUse(i4, flag2, hand, runnable2);
                        }
                    }
                }
            }
        }
    }

    private void queueSilentHotbarUse(int i, boolean flag, Vector2f vector2f) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        ActionSequence actionSequence = this.scheduler;
        Runnable runnable = this.pendingAction;
        Deque deque = this.actionQueue;
        if (clientPlayerEntity != null) {
            ((Long)(Object)runnable).longValue();
            actionSequence.isFinished();
            Runnable runnable2 = this.pendingAction;
            if (actionSequence.isFinished()) {
                long l = (Long)(Object)runnable2;
                if (l == 0L) {
                    deque.isEmpty();
                    Runnable runnable3 = this.pendingAction;
                    if (deque.isEmpty()) {
                        ((Long)(Object)runnable3).longValue();
                        int i2 = this.getSelectedSlot();
                        int i3;
                        if (i2 < 0) {
                            int i4 = i2 + 7;
                            i3 = i4;
                        } else {
                            i3 = i2;
                        }

                        int i5 = i3 & -8;
                        int i6 = 0 - i5 + i2 + 1;
                        Runnable runnable4 = () -> {
                            this.swapSlotWithHotbar(i, i6);
                            this.closeInventoryScreen();
                            this.setSelectedSlot(i6);
                        };
                        deque.add(runnable4);
                        boolean flag2 = flag;
                        Runnable runnable5 = () -> {
                            if (flag2) {
                                rotationTimer.setDelay(400L);
                            }

                            this.interactItem(vector2f, Hand.MAIN_HAND);
                            this.setSelectedSlot(i2);
                        };
                        deque.add(runnable5);
                        Runnable runnable6 = () -> {
                            this.swapSlotWithHotbar(i, i6);
                            this.closeInventoryScreen();
                        };
                        deque.add(runnable6);
                    } else {
                        ((Long)(Object)runnable3).longValue();
                    }
                }
            } else {
                ((Long)(Object)runnable2).longValue();
            }
        } else {
            ((Long)(Object)runnable).longValue();
        }
    }

    private void queueSilentInventoryUse(int i, int i2, int i3, Vector2f vector2f) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        ActionSequence actionSequence = this.scheduler;
        if (clientPlayerEntity != null) {
            boolean flag = actionSequence.isFinished();
            Runnable runnable = this.pendingAction;
            Deque deque = this.actionQueue;
            if (flag && runnable == null && deque.isEmpty()) {
                int i4 = this.getSelectedSlot();
                int i5 = this.getStackCount(i, i3);
                int i6 = i4 + 7;
                int i7 = (i4 < 0 ? i6 : i4) & -8;
                int i8 = 0 - i7;
                int i9 = i4 + i8 + 1;
                boolean flag2;
                if (i5 >= 2) {
                    flag2 = true;
                } else {
                    flag2 = false;
                }

                Runnable runnable2 = () -> {
                    if (i3 >= 0) {
                        BundleItem.setSelectedStackIndex(mc.player.currentScreenHandler.getSlot(i).getStack(), i3);
                        mc.player.networkHandler.sendPacket(new BundleItemSelectedC2SPacket(i, i3));
                    }

                    this.clickSlot(i, 1, SlotActionType.PICKUP);
                    this.clickSlot(i2, 0, SlotActionType.PICKUP);
                    this.swapSlotWithHotbar(i2, i9);
                    this.closeInventoryScreen();
                    this.setSelectedSlot(i9);
                };
                deque.add(runnable2);
                Runnable runnable3 = () -> {
                    this.interactItem(vector2f, Hand.MAIN_HAND);
                    this.setSelectedSlot(i4);
                };
                deque.add(runnable3);
                Runnable runnable4 = () -> {
                    this.swapSlotWithHotbar(i2, i9);
                    if (flag2) {
                        this.clickSlot(i2, 0, SlotActionType.PICKUP);
                        this.clickSlot(i, 0, SlotActionType.PICKUP);
                    }

                    this.closeInventoryScreen();
                };
                deque.add(runnable4);
                return;
            }
        }
    }

    private boolean supportsSilentSwap() {
        return ServerManager.isServer("funtime") ? true : ServerManager.isLocalProxy();
    }

    public void useHeldItem(Vector2f vector2f) {
        Hand hand = Hand.MAIN_HAND;
        this.useHeldItemInHand(vector2f, hand);
    }

    public void useHeldItemInHand(Vector2f vector2f, Hand hand) {
        if (mc.player != null) {
            ItemStack itemStack;
            if (Hand.OFF_HAND != hand) {
                itemStack = mc.player.getMainHandStack();
            } else {
                itemStack = mc.player.getOffHandStack();
            }

            if (!itemStack.isEmpty()) {
                if (!mc.player.getItemCooldownManager().isCoolingDown(itemStack)) {
                    this.interactItem(vector2f, hand);
                }
            }
        }
    }

    public void useFromSlot(int i, int i2, int i3, Vector2f vector2f) {
        Hand hand = Hand.MAIN_HAND;
        this.useFromSlotInHand(i, i2, i3, hand, vector2f);
    }

    public void useFromSlotInHand(int i, int i2, int i3, Hand hand, Vector2f vector2f) {
        if (this.scheduler.isFinished() && mc.player != null) {
            boolean flag;
            label68: {
                if (!mc.player.isUsingItem() && !mc.options.useKey.isPressed()) {
                    int i4 = Key.MOUSE_RIGHT.getKeyCode();
                    if (i4 < 0) {
                        flag = false;
                        break label68;
                    }

                    MinecraftClient minecraftClient = MinecraftClient.getInstance();
                    if (minecraftClient == null) {
                        flag = false;
                        break label68;
                    }

                    if (minecraftClient.getWindow() == null) {
                        flag = false;
                        break label68;
                    }

                    long l = minecraftClient.getWindow().getHandle();
                    if (l == 0L) {
                        flag = false;
                        break label68;
                    }

                    if (Integer.compareUnsigned(i4, 7) > 0) {
                        int i5 = i4 & 2147483647;
                        if (GLFW.glfwGetKey(l, i5) != 1) {
                            flag = false;
                            break label68;
                        }
                    } else {
                        int i6 = i4 & 7;
                        if (GLFW.glfwGetMouseButton(l, i6) != 1) {
                            flag = false;
                            break label68;
                        }
                    }
                }

                if (ServerManager.isServer("donut")) {
                    return;
                }

                flag = true;
            }

            if (Hand.MAIN_HAND == hand && this.supportsSilentSwap() && !flag) {
                this.queueSilentInventoryUse(i, i2, i3, vector2f);
            } else {
                GuiWalk guiWalk = GuiWalk.getInstance();
                if (ProtocolUtil.isOlderThan("26.2") && !guiWalk.isBypassActive()) {
                    Runnable runnable = () -> this.interactItem(vector2f, hand);
                    this.scheduleInventorySwapUse(i, i2, i3, false, hand, runnable);
                } else {
                    Runnable runnable2 = () -> this.interactItem(vector2f, hand);
                    this.queueInventorySwapUse(i, i2, i3, false, hand, runnable2);
                }
            }
        }
    }

    private void scheduleInventorySwapUse(int i, int i2, int i3, boolean flag, Hand hand, Runnable runnable) {
        int i4 = hand == Hand.OFF_HAND ? 40 : this.getSelectedSlot();
        Runnable runnable2 = () -> {
            if (i3 >= 0) {
                BundleItem.setSelectedStackIndex(mc.player.currentScreenHandler.getSlot(i).getStack(), i3);
                mc.player.networkHandler.sendPacket(new BundleItemSelectedC2SPacket(i, i3));
            }

            mc.interactionManager.clickSlot(0, i, 1, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, i2, 0, SlotActionType.PICKUP, mc.player);
        };
        Runnable runnable3 = () -> this.swapSlotPacket(i2, i4);
        Runnable runnable4 = () -> {
            if (!mc.player.currentScreenHandler.getSlot(i2).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(0, i2, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, i, 0, SlotActionType.PICKUP, mc.player);
            }
        };
        if (flag) {
            this.scheduler
                .addDelayed(0, () -> {
                    this.blockMovement = true;
                    this.swapping = true;
                })
                .addDelayed(this.timing.swapInAt(), runnable2::run)
                .addDelayed(this.timing.swapInAt() * 2, runnable3::run)
                .addDelayed(this.timing.swapInAt() + this.timing.useAt(), runnable::run)
                .addDelayed(0, () -> {}, rotationTimer::isReached)
                .addDelayed(100, runnable3::run)
                .addDelayed(100 + this.timing.swapIn(), runnable4::run)
                .addDelayed(this.timing.swapInAt() * 2 + this.timing.unblockAt(), () -> {
                    this.blockMovement = false;
                    this.swapping = false;
                });
        } else {
            this.scheduler
                .addDelayed(0, () -> {
                    this.blockMovement = true;
                    this.swapping = true;
                })
                .addDelayed(this.timing.swapInAt(), runnable2::run)
                .addDelayed(this.timing.swapInAt() * 2, runnable3::run)
                .addDelayed(this.timing.swapInAt() + this.timing.useAt(), runnable::run)
                .addDelayed(this.timing.swapInAt() + this.timing.swapBackAt(), runnable3::run)
                .addDelayed(this.timing.swapInAt() * 2 + this.timing.swapBackAt(), runnable4::run)
                .addDelayed(this.timing.swapInAt() * 2 + this.timing.unblockAt(), () -> {
                    this.blockMovement = false;
                    this.swapping = false;
                });
        }
    }

    private void performInventorySwapUse(int i, int i2, int i3, boolean flag, Hand hand, Runnable runnable) {
        ScreenHandler screenHandler;
        int i4;
        int i5;
        label183: {
            label208: {
                PlayerInput playerInput2;
                boolean flag2;
                label181: {
                    boolean flag3 = ProtocolUtil.isOlderThan("26.2");
                    if (flag) {
                        if (i3 >= 0) {
                            if (!flag3) {
                                break label208;
                            }

                            screenHandler = mc.player.currentScreenHandler;
                            playerInput2 = mc.player.input.playerInput;
                            boolean flag4 = MoveUtil.isMoving();
                            i4 = (int)((short)(i & 65535));
                            i5 = (int)((short)(i2 & 65535));
                            if (!flag4 && !mc.player.input.playerInput.jump()) {
                                mc.player.isSprinting();
                                int i6;
                                if (Hand.OFF_HAND != hand) {
                                    i6 = this.getSelectedSlot() & 0xFF;
                                } else {
                                    i6 = 40;
                                }

                                boolean flag5;
                                if (this.getStackCount(i, i3) >= 2) {
                                    flag5 = true;
                                } else {
                                    flag5 = false;
                                }

                                PacketSender.sendSilent(new BundleItemSelectedC2SPacket(i, i3));
                                int i7 = screenHandler.syncId;
                                int i8 = screenHandler.getRevision();
                                SlotActionType slotActionType = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i7, i8, (short)i4, (byte)1, slotActionType, int2ObjectMap, itemStackHash));
                                int i9 = screenHandler.syncId;
                                int i10 = screenHandler.getRevision();
                                SlotActionType slotActionType2 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap2 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash2 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i9, i10, (short)i5, (byte)0, slotActionType2, int2ObjectMap2, itemStackHash2));
                                int i11 = screenHandler.syncId;
                                int i12 = screenHandler.getRevision();
                                SlotActionType slotActionType3 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap3 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash3 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i11, i12, (short)0, (byte)0, slotActionType3, int2ObjectMap3, itemStackHash3));
                                int i13 = screenHandler.syncId;
                                int i14 = screenHandler.getRevision();
                                SlotActionType slotActionType4 = SlotActionType.SWAP;
                                Int2ObjectMap int2ObjectMap4 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash4 = ItemStackHash.EMPTY;
                                int i15 = (int)((byte)i6);
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i13, i14, (short)i5, (byte)i15, slotActionType4, int2ObjectMap4, itemStackHash4));
                                this.closeInventoryScreen();
                                rotationTimer.setDelay(400L);
                                runnable.run();
                                int i16 = screenHandler.syncId;
                                int i17 = screenHandler.getRevision();
                                SlotActionType slotActionType5 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap5 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash5 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i16, i17, (short)0, (byte)0, slotActionType5, int2ObjectMap5, itemStackHash5));
                                int i18 = screenHandler.syncId;
                                int i19 = screenHandler.getRevision();
                                SlotActionType slotActionType6 = SlotActionType.SWAP;
                                Int2ObjectMap int2ObjectMap6 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash6 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i18, i19, (short)i5, (byte)i15, slotActionType6, int2ObjectMap6, itemStackHash6));
                                this.closeInventoryScreen();
                                if (flag5) {
                                    break label183;
                                }

                                return;
                            }

                            flag2 = mc.player.isSprinting();
                            int i20;
                            if (Hand.OFF_HAND != hand) {
                                i20 = this.getSelectedSlot() & 0xFF;
                            } else {
                                i20 = 40;
                            }

                            boolean flag6;
                            if (this.getStackCount(i, i3) >= 2) {
                                flag6 = true;
                            } else {
                                flag6 = false;
                            }

                            ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
                            PlayerInput playerInput3 = PlayerInput.DEFAULT;
                            PlayerInputC2SPacket playerInputC2SPacket = new PlayerInputC2SPacket(playerInput3);
                            clientPlayNetworkHandler.sendPacket(playerInputC2SPacket);
                            if (flag2) {
                                mc.player.setSprinting(false);
                                ClientPlayNetworkHandler clientPlayNetworkHandler2 = mc.player.networkHandler;
                                ClientPlayerEntity clientPlayerEntity = mc.player;
                                Mode mode = Mode.STOP_SPRINTING;
                                ClientCommandC2SPacket clientCommandC2SPacket = new ClientCommandC2SPacket(clientPlayerEntity, mode);
                                clientPlayNetworkHandler2.sendPacket(clientCommandC2SPacket);
                            }

                            PacketSender.sendSilent(new BundleItemSelectedC2SPacket(i, i3));
                            int i21 = screenHandler.syncId;
                            int i22 = screenHandler.getRevision();
                            SlotActionType slotActionType7 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap7 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash7 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i21, i22, (short)i4, (byte)1, slotActionType7, int2ObjectMap7, itemStackHash7));
                            int i23 = screenHandler.syncId;
                            int i24 = screenHandler.getRevision();
                            SlotActionType slotActionType8 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap8 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash8 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i23, i24, (short)i5, (byte)0, slotActionType8, int2ObjectMap8, itemStackHash8));
                            int i25 = screenHandler.syncId;
                            int i26 = screenHandler.getRevision();
                            SlotActionType slotActionType9 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap9 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash9 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i25, i26, (short)0, (byte)0, slotActionType9, int2ObjectMap9, itemStackHash9));
                            int i27 = screenHandler.syncId;
                            int i28 = screenHandler.getRevision();
                            SlotActionType slotActionType10 = SlotActionType.SWAP;
                            Int2ObjectMap int2ObjectMap10 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash10 = ItemStackHash.EMPTY;
                            int i29 = (int)((byte)i20);
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i27, i28, (short)i5, (byte)i29, slotActionType10, int2ObjectMap10, itemStackHash10));
                            this.closeInventoryScreen();
                            rotationTimer.setDelay(400L);
                            runnable.run();
                            int i30 = screenHandler.syncId;
                            int i31 = screenHandler.getRevision();
                            SlotActionType slotActionType11 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap11 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash11 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i30, i31, (short)0, (byte)0, slotActionType11, int2ObjectMap11, itemStackHash11));
                            int i32 = screenHandler.syncId;
                            int i33 = screenHandler.getRevision();
                            SlotActionType slotActionType12 = SlotActionType.SWAP;
                            Int2ObjectMap int2ObjectMap12 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash12 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i32, i33, (short)i5, (byte)i29, slotActionType12, int2ObjectMap12, itemStackHash12));
                            this.closeInventoryScreen();
                            if (!flag6) {
                                break label181;
                            }
                        } else {
                            if (!flag3) {
                                break label208;
                            }

                            screenHandler = mc.player.currentScreenHandler;
                            playerInput2 = mc.player.input.playerInput;
                            boolean flag7 = MoveUtil.isMoving();
                            i4 = (int)((short)(i & 65535));
                            i5 = (int)((short)(i2 & 65535));
                            if (!flag7 && !mc.player.input.playerInput.jump()) {
                                mc.player.isSprinting();
                                int i34;
                                if (Hand.OFF_HAND != hand) {
                                    i34 = this.getSelectedSlot() & 0xFF;
                                } else {
                                    i34 = 40;
                                }

                                boolean flag8;
                                if (this.getStackCount(i, i3) >= 2) {
                                    flag8 = true;
                                } else {
                                    flag8 = false;
                                }

                                int i35 = screenHandler.syncId;
                                int i36 = screenHandler.getRevision();
                                SlotActionType slotActionType13 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap13 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash13 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i35, i36, (short)i4, (byte)1, slotActionType13, int2ObjectMap13, itemStackHash13));
                                int i37 = screenHandler.syncId;
                                int i38 = screenHandler.getRevision();
                                SlotActionType slotActionType14 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap14 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash14 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i37, i38, (short)i5, (byte)0, slotActionType14, int2ObjectMap14, itemStackHash14));
                                int i39 = screenHandler.syncId;
                                int i40 = screenHandler.getRevision();
                                SlotActionType slotActionType15 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap15 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash15 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i39, i40, (short)0, (byte)0, slotActionType15, int2ObjectMap15, itemStackHash15));
                                int i41 = screenHandler.syncId;
                                int i42 = screenHandler.getRevision();
                                SlotActionType slotActionType16 = SlotActionType.SWAP;
                                Int2ObjectMap int2ObjectMap16 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash16 = ItemStackHash.EMPTY;
                                int i43 = (int)((byte)i34);
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i41, i42, (short)i5, (byte)i43, slotActionType16, int2ObjectMap16, itemStackHash16));
                                this.closeInventoryScreen();
                                rotationTimer.setDelay(400L);
                                runnable.run();
                                int i44 = screenHandler.syncId;
                                int i45 = screenHandler.getRevision();
                                SlotActionType slotActionType17 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap17 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash17 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i44, i45, (short)0, (byte)0, slotActionType17, int2ObjectMap17, itemStackHash17));
                                int i46 = screenHandler.syncId;
                                int i47 = screenHandler.getRevision();
                                SlotActionType slotActionType18 = SlotActionType.SWAP;
                                Int2ObjectMap int2ObjectMap18 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash18 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i46, i47, (short)i5, (byte)i43, slotActionType18, int2ObjectMap18, itemStackHash18));
                                this.closeInventoryScreen();
                                if (flag8) {
                                    break label183;
                                }

                                return;
                            }

                            flag2 = mc.player.isSprinting();
                            int i48;
                            if (Hand.OFF_HAND != hand) {
                                i48 = this.getSelectedSlot() & 0xFF;
                            } else {
                                i48 = 40;
                            }

                            boolean flag9;
                            if (this.getStackCount(i, i3) >= 2) {
                                flag9 = true;
                            } else {
                                flag9 = false;
                            }

                            ClientPlayNetworkHandler clientPlayNetworkHandler3 = mc.player.networkHandler;
                            PlayerInput playerInput4 = PlayerInput.DEFAULT;
                            PlayerInputC2SPacket playerInputC2SPacket2 = new PlayerInputC2SPacket(playerInput4);
                            clientPlayNetworkHandler3.sendPacket(playerInputC2SPacket2);
                            if (flag2) {
                                mc.player.setSprinting(false);
                                ClientPlayNetworkHandler clientPlayNetworkHandler4 = mc.player.networkHandler;
                                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                                Mode mode2 = Mode.STOP_SPRINTING;
                                ClientCommandC2SPacket clientCommandC2SPacket2 = new ClientCommandC2SPacket(clientPlayerEntity2, mode2);
                                clientPlayNetworkHandler4.sendPacket(clientCommandC2SPacket2);
                            }

                            int i49 = screenHandler.syncId;
                            int i50 = screenHandler.getRevision();
                            SlotActionType slotActionType19 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap19 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash19 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i49, i50, (short)i4, (byte)1, slotActionType19, int2ObjectMap19, itemStackHash19));
                            int i51 = screenHandler.syncId;
                            int i52 = screenHandler.getRevision();
                            SlotActionType slotActionType20 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap20 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash20 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i51, i52, (short)i5, (byte)0, slotActionType20, int2ObjectMap20, itemStackHash20));
                            int i53 = screenHandler.syncId;
                            int i54 = screenHandler.getRevision();
                            SlotActionType slotActionType21 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap21 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash21 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i53, i54, (short)0, (byte)0, slotActionType21, int2ObjectMap21, itemStackHash21));
                            int i55 = screenHandler.syncId;
                            int i56 = screenHandler.getRevision();
                            SlotActionType slotActionType22 = SlotActionType.SWAP;
                            Int2ObjectMap int2ObjectMap22 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash22 = ItemStackHash.EMPTY;
                            int i57 = (int)((byte)i48);
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i55, i56, (short)i5, (byte)i57, slotActionType22, int2ObjectMap22, itemStackHash22));
                            this.closeInventoryScreen();
                            rotationTimer.setDelay(400L);
                            runnable.run();
                            int i58 = screenHandler.syncId;
                            int i59 = screenHandler.getRevision();
                            SlotActionType slotActionType23 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap23 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash23 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i58, i59, (short)0, (byte)0, slotActionType23, int2ObjectMap23, itemStackHash23));
                            int i60 = screenHandler.syncId;
                            int i61 = screenHandler.getRevision();
                            SlotActionType slotActionType24 = SlotActionType.SWAP;
                            Int2ObjectMap int2ObjectMap24 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash24 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i60, i61, (short)i5, (byte)i57, slotActionType24, int2ObjectMap24, itemStackHash24));
                            this.closeInventoryScreen();
                            if (!flag9) {
                                break label181;
                            }
                        }
                    } else {
                        if (i3 >= 0) {
                            if (!flag3) {
                                break label208;
                            }

                            screenHandler = mc.player.currentScreenHandler;
                            playerInput2 = mc.player.input.playerInput;
                            boolean flag10 = MoveUtil.isMoving();
                            i4 = (int)((short)(i & 65535));
                            i5 = (int)((short)(i2 & 65535));
                            if (!flag10 && !mc.player.input.playerInput.jump()) {
                                mc.player.isSprinting();
                                int i62;
                                if (Hand.OFF_HAND != hand) {
                                    i62 = this.getSelectedSlot() & 0xFF;
                                } else {
                                    i62 = 40;
                                }

                                boolean flag11;
                                if (this.getStackCount(i, i3) >= 2) {
                                    flag11 = true;
                                } else {
                                    flag11 = false;
                                }

                                PacketSender.sendSilent(new BundleItemSelectedC2SPacket(i, i3));
                                int i63 = screenHandler.syncId;
                                int i64 = screenHandler.getRevision();
                                SlotActionType slotActionType25 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap25 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash25 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i63, i64, (short)i4, (byte)1, slotActionType25, int2ObjectMap25, itemStackHash25));
                                int i65 = screenHandler.syncId;
                                int i66 = screenHandler.getRevision();
                                SlotActionType slotActionType26 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap26 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash26 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i65, i66, (short)i5, (byte)0, slotActionType26, int2ObjectMap26, itemStackHash26));
                                int i67 = screenHandler.syncId;
                                int i68 = screenHandler.getRevision();
                                SlotActionType slotActionType27 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap27 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash27 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i67, i68, (short)0, (byte)0, slotActionType27, int2ObjectMap27, itemStackHash27));
                                int i69 = screenHandler.syncId;
                                int i70 = screenHandler.getRevision();
                                SlotActionType slotActionType28 = SlotActionType.SWAP;
                                Int2ObjectMap int2ObjectMap28 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash28 = ItemStackHash.EMPTY;
                                int i71 = (int)((byte)i62);
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i69, i70, (short)i5, (byte)i71, slotActionType28, int2ObjectMap28, itemStackHash28));
                                this.closeInventoryScreen();
                                runnable.run();
                                int i72 = screenHandler.syncId;
                                int i73 = screenHandler.getRevision();
                                SlotActionType slotActionType29 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap29 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash29 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i72, i73, (short)0, (byte)0, slotActionType29, int2ObjectMap29, itemStackHash29));
                                int i74 = screenHandler.syncId;
                                int i75 = screenHandler.getRevision();
                                SlotActionType slotActionType30 = SlotActionType.SWAP;
                                Int2ObjectMap int2ObjectMap30 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash30 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i74, i75, (short)i5, (byte)i71, slotActionType30, int2ObjectMap30, itemStackHash30));
                                this.closeInventoryScreen();
                                if (flag11) {
                                    break label183;
                                }

                                return;
                            }

                            flag2 = mc.player.isSprinting();
                            int i76;
                            if (Hand.OFF_HAND != hand) {
                                i76 = this.getSelectedSlot() & 0xFF;
                            } else {
                                i76 = 40;
                            }

                            boolean flag12;
                            if (this.getStackCount(i, i3) >= 2) {
                                flag12 = true;
                            } else {
                                flag12 = false;
                            }

                            ClientPlayNetworkHandler clientPlayNetworkHandler5 = mc.player.networkHandler;
                            PlayerInput playerInput5 = PlayerInput.DEFAULT;
                            PlayerInputC2SPacket playerInputC2SPacket3 = new PlayerInputC2SPacket(playerInput5);
                            clientPlayNetworkHandler5.sendPacket(playerInputC2SPacket3);
                            if (flag2) {
                                mc.player.setSprinting(false);
                                ClientPlayNetworkHandler clientPlayNetworkHandler6 = mc.player.networkHandler;
                                ClientPlayerEntity clientPlayerEntity3 = mc.player;
                                Mode mode3 = Mode.STOP_SPRINTING;
                                ClientCommandC2SPacket clientCommandC2SPacket3 = new ClientCommandC2SPacket(clientPlayerEntity3, mode3);
                                clientPlayNetworkHandler6.sendPacket(clientCommandC2SPacket3);
                            }

                            PacketSender.sendSilent(new BundleItemSelectedC2SPacket(i, i3));
                            int i77 = screenHandler.syncId;
                            int i78 = screenHandler.getRevision();
                            SlotActionType slotActionType31 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap31 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash31 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i77, i78, (short)i4, (byte)1, slotActionType31, int2ObjectMap31, itemStackHash31));
                            int i79 = screenHandler.syncId;
                            int i80 = screenHandler.getRevision();
                            SlotActionType slotActionType32 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap32 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash32 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i79, i80, (short)i5, (byte)0, slotActionType32, int2ObjectMap32, itemStackHash32));
                            int i81 = screenHandler.syncId;
                            int i82 = screenHandler.getRevision();
                            SlotActionType slotActionType33 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap33 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash33 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i81, i82, (short)0, (byte)0, slotActionType33, int2ObjectMap33, itemStackHash33));
                            int i83 = screenHandler.syncId;
                            int i84 = screenHandler.getRevision();
                            SlotActionType slotActionType34 = SlotActionType.SWAP;
                            Int2ObjectMap int2ObjectMap34 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash34 = ItemStackHash.EMPTY;
                            int i85 = (int)((byte)i76);
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i83, i84, (short)i5, (byte)i85, slotActionType34, int2ObjectMap34, itemStackHash34));
                            this.closeInventoryScreen();
                            runnable.run();
                            int i86 = screenHandler.syncId;
                            int i87 = screenHandler.getRevision();
                            SlotActionType slotActionType35 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap35 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash35 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i86, i87, (short)0, (byte)0, slotActionType35, int2ObjectMap35, itemStackHash35));
                            int i88 = screenHandler.syncId;
                            int i89 = screenHandler.getRevision();
                            SlotActionType slotActionType36 = SlotActionType.SWAP;
                            Int2ObjectMap int2ObjectMap36 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash36 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i88, i89, (short)i5, (byte)i85, slotActionType36, int2ObjectMap36, itemStackHash36));
                            this.closeInventoryScreen();
                            if (flag12) {
                                int i90 = screenHandler.syncId;
                                int i91 = screenHandler.getRevision();
                                SlotActionType slotActionType37 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap37 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash37 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i90, i91, (short)i5, (byte)0, slotActionType37, int2ObjectMap37, itemStackHash37));
                                int i92 = screenHandler.syncId;
                                int i93 = screenHandler.getRevision();
                                SlotActionType slotActionType38 = SlotActionType.PICKUP;
                                Int2ObjectMap int2ObjectMap38 = Int2ObjectMaps.emptyMap();
                                ItemStackHash itemStackHash38 = ItemStackHash.EMPTY;
                                PacketSender.sendSilent(new ClickSlotC2SPacket(i92, i93, (short)i4, (byte)0, slotActionType38, int2ObjectMap38, itemStackHash38));
                            }
                            break label181;
                        }

                        if (!flag3) {
                            break label208;
                        }

                        screenHandler = mc.player.currentScreenHandler;
                        playerInput2 = mc.player.input.playerInput;
                        boolean flag13 = MoveUtil.isMoving();
                        i4 = (int)((short)(i & 65535));
                        i5 = (int)((short)(i2 & 65535));
                        if (!flag13 && !mc.player.input.playerInput.jump()) {
                            mc.player.isSprinting();
                            int i94;
                            if (Hand.OFF_HAND != hand) {
                                i94 = this.getSelectedSlot() & 0xFF;
                            } else {
                                i94 = 40;
                            }

                            boolean flag14;
                            if (this.getStackCount(i, i3) >= 2) {
                                flag14 = true;
                            } else {
                                flag14 = false;
                            }

                            int i95 = screenHandler.syncId;
                            int i96 = screenHandler.getRevision();
                            SlotActionType slotActionType39 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap39 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash39 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i95, i96, (short)i4, (byte)1, slotActionType39, int2ObjectMap39, itemStackHash39));
                            int i97 = screenHandler.syncId;
                            int i98 = screenHandler.getRevision();
                            SlotActionType slotActionType40 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap40 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash40 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i97, i98, (short)i5, (byte)0, slotActionType40, int2ObjectMap40, itemStackHash40));
                            int i99 = screenHandler.syncId;
                            int i100 = screenHandler.getRevision();
                            SlotActionType slotActionType41 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap41 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash41 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i99, i100, (short)0, (byte)0, slotActionType41, int2ObjectMap41, itemStackHash41));
                            int i101 = screenHandler.syncId;
                            int i102 = screenHandler.getRevision();
                            SlotActionType slotActionType42 = SlotActionType.SWAP;
                            Int2ObjectMap int2ObjectMap42 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash42 = ItemStackHash.EMPTY;
                            int i103 = (int)((byte)i94);
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i101, i102, (short)i5, (byte)i103, slotActionType42, int2ObjectMap42, itemStackHash42));
                            this.closeInventoryScreen();
                            runnable.run();
                            int i104 = screenHandler.syncId;
                            int i105 = screenHandler.getRevision();
                            SlotActionType slotActionType43 = SlotActionType.PICKUP;
                            Int2ObjectMap int2ObjectMap43 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash43 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i104, i105, (short)0, (byte)0, slotActionType43, int2ObjectMap43, itemStackHash43));
                            int i106 = screenHandler.syncId;
                            int i107 = screenHandler.getRevision();
                            SlotActionType slotActionType44 = SlotActionType.SWAP;
                            Int2ObjectMap int2ObjectMap44 = Int2ObjectMaps.emptyMap();
                            ItemStackHash itemStackHash44 = ItemStackHash.EMPTY;
                            PacketSender.sendSilent(new ClickSlotC2SPacket(i106, i107, (short)i5, (byte)i103, slotActionType44, int2ObjectMap44, itemStackHash44));
                            this.closeInventoryScreen();
                            if (flag14) {
                                break label183;
                            }

                            return;
                        }

                        flag2 = mc.player.isSprinting();
                        int i108;
                        if (Hand.OFF_HAND != hand) {
                            i108 = this.getSelectedSlot() & 0xFF;
                        } else {
                            i108 = 40;
                        }

                        boolean flag15;
                        if (this.getStackCount(i, i3) >= 2) {
                            flag15 = true;
                        } else {
                            flag15 = false;
                        }

                        ClientPlayNetworkHandler clientPlayNetworkHandler7 = mc.player.networkHandler;
                        PlayerInput playerInput6 = PlayerInput.DEFAULT;
                        PlayerInputC2SPacket playerInputC2SPacket4 = new PlayerInputC2SPacket(playerInput6);
                        clientPlayNetworkHandler7.sendPacket(playerInputC2SPacket4);
                        if (flag2) {
                            mc.player.setSprinting(false);
                            ClientPlayNetworkHandler clientPlayNetworkHandler8 = mc.player.networkHandler;
                            ClientPlayerEntity clientPlayerEntity4 = mc.player;
                            Mode mode4 = Mode.STOP_SPRINTING;
                            ClientCommandC2SPacket clientCommandC2SPacket4 = new ClientCommandC2SPacket(clientPlayerEntity4, mode4);
                            clientPlayNetworkHandler8.sendPacket(clientCommandC2SPacket4);
                        }

                        int i109 = screenHandler.syncId;
                        int i110 = screenHandler.getRevision();
                        SlotActionType slotActionType45 = SlotActionType.PICKUP;
                        Int2ObjectMap int2ObjectMap45 = Int2ObjectMaps.emptyMap();
                        ItemStackHash itemStackHash45 = ItemStackHash.EMPTY;
                        PacketSender.sendSilent(new ClickSlotC2SPacket(i109, i110, (short)i4, (byte)1, slotActionType45, int2ObjectMap45, itemStackHash45));
                        int i111 = screenHandler.syncId;
                        int i112 = screenHandler.getRevision();
                        SlotActionType slotActionType46 = SlotActionType.PICKUP;
                        Int2ObjectMap int2ObjectMap46 = Int2ObjectMaps.emptyMap();
                        ItemStackHash itemStackHash46 = ItemStackHash.EMPTY;
                        PacketSender.sendSilent(new ClickSlotC2SPacket(i111, i112, (short)i5, (byte)0, slotActionType46, int2ObjectMap46, itemStackHash46));
                        int i113 = screenHandler.syncId;
                        int i114 = screenHandler.getRevision();
                        SlotActionType slotActionType47 = SlotActionType.PICKUP;
                        Int2ObjectMap int2ObjectMap47 = Int2ObjectMaps.emptyMap();
                        ItemStackHash itemStackHash47 = ItemStackHash.EMPTY;
                        PacketSender.sendSilent(new ClickSlotC2SPacket(i113, i114, (short)0, (byte)0, slotActionType47, int2ObjectMap47, itemStackHash47));
                        int i115 = screenHandler.syncId;
                        int i116 = screenHandler.getRevision();
                        SlotActionType slotActionType48 = SlotActionType.SWAP;
                        Int2ObjectMap int2ObjectMap48 = Int2ObjectMaps.emptyMap();
                        ItemStackHash itemStackHash48 = ItemStackHash.EMPTY;
                        int i117 = (int)((byte)i108);
                        PacketSender.sendSilent(new ClickSlotC2SPacket(i115, i116, (short)i5, (byte)i117, slotActionType48, int2ObjectMap48, itemStackHash48));
                        this.closeInventoryScreen();
                        runnable.run();
                        int i118 = screenHandler.syncId;
                        int i119 = screenHandler.getRevision();
                        SlotActionType slotActionType49 = SlotActionType.PICKUP;
                        Int2ObjectMap int2ObjectMap49 = Int2ObjectMaps.emptyMap();
                        ItemStackHash itemStackHash49 = ItemStackHash.EMPTY;
                        PacketSender.sendSilent(new ClickSlotC2SPacket(i118, i119, (short)0, (byte)0, slotActionType49, int2ObjectMap49, itemStackHash49));
                        int i120 = screenHandler.syncId;
                        int i121 = screenHandler.getRevision();
                        SlotActionType slotActionType50 = SlotActionType.SWAP;
                        Int2ObjectMap int2ObjectMap50 = Int2ObjectMaps.emptyMap();
                        ItemStackHash itemStackHash50 = ItemStackHash.EMPTY;
                        PacketSender.sendSilent(new ClickSlotC2SPacket(i120, i121, (short)i5, (byte)i117, slotActionType50, int2ObjectMap50, itemStackHash50));
                        this.closeInventoryScreen();
                        if (!flag15) {
                            break label181;
                        }
                    }

                    int i122 = screenHandler.syncId;
                    int i123 = screenHandler.getRevision();
                    SlotActionType slotActionType51 = SlotActionType.PICKUP;
                    Int2ObjectMap int2ObjectMap51 = Int2ObjectMaps.emptyMap();
                    ItemStackHash itemStackHash51 = ItemStackHash.EMPTY;
                    PacketSender.sendSilent(new ClickSlotC2SPacket(i122, i123, (short)i5, (byte)0, slotActionType51, int2ObjectMap51, itemStackHash51));
                    int i124 = screenHandler.syncId;
                    int i125 = screenHandler.getRevision();
                    SlotActionType slotActionType52 = SlotActionType.PICKUP;
                    Int2ObjectMap int2ObjectMap52 = Int2ObjectMaps.emptyMap();
                    ItemStackHash itemStackHash52 = ItemStackHash.EMPTY;
                    PacketSender.sendSilent(new ClickSlotC2SPacket(i124, i125, (short)i4, (byte)0, slotActionType52, int2ObjectMap52, itemStackHash52));
                }

                ClientPlayNetworkHandler clientPlayNetworkHandler9 = mc.player.networkHandler;
                PlayerInputC2SPacket playerInputC2SPacket5 = new PlayerInputC2SPacket(playerInput2);
                clientPlayNetworkHandler9.sendPacket(playerInputC2SPacket5);
                if (flag2) {
                    mc.player.setSprinting(true);
                    ClientPlayNetworkHandler clientPlayNetworkHandler10 = mc.player.networkHandler;
                    ClientPlayerEntity clientPlayerEntity5 = mc.player;
                    Mode mode5 = Mode.START_SPRINTING;
                    ClientCommandC2SPacket clientCommandC2SPacket5 = new ClientCommandC2SPacket(clientPlayerEntity5, mode5);
                    clientPlayNetworkHandler10.sendPacket(clientCommandC2SPacket5);
                    return;
                }

                return;
            }

            this.queueInventorySwapUse(i, i2, i3, flag, hand, runnable);
            return;
        }

        int i126 = screenHandler.syncId;
        int i127 = screenHandler.getRevision();
        SlotActionType slotActionType53 = SlotActionType.PICKUP;
        Int2ObjectMap int2ObjectMap53 = Int2ObjectMaps.emptyMap();
        ItemStackHash itemStackHash53 = ItemStackHash.EMPTY;
        PacketSender.sendSilent(new ClickSlotC2SPacket(i126, i127, (short)i5, (byte)0, slotActionType53, int2ObjectMap53, itemStackHash53));
        int i128 = screenHandler.syncId;
        int i129 = screenHandler.getRevision();
        SlotActionType slotActionType54 = SlotActionType.PICKUP;
        Int2ObjectMap int2ObjectMap54 = Int2ObjectMaps.emptyMap();
        ItemStackHash itemStackHash54 = ItemStackHash.EMPTY;
        PacketSender.sendSilent(new ClickSlotC2SPacket(i128, i129, (short)i4, (byte)0, slotActionType54, int2ObjectMap54, itemStackHash54));
    }

    private int getStackCount(int i, int i2) {
        ItemStack itemStack = mc.player.currentScreenHandler.getSlot(i).getStack();
        ComponentType<BundleContentsComponent> componentType = DataComponentTypes.BUNDLE_CONTENTS;
        BundleContentsComponent bundleContentsComponent = itemStack.get(componentType);
        int i3;
        if (i2 < 0) {
            if (bundleContentsComponent == null) {
                return 0;
            }

            i3 = 0;
        } else {
            if (bundleContentsComponent == null) {
                return 0;
            }

            i3 = i2;
        }

        int i4 = bundleContentsComponent.size();
        return i3 >= i4 ? 0 : bundleContentsComponent.get(i3).getCount();
    }

    public void scheduleHotbarSwapUse(int i, boolean flag, Hand hand, Runnable runnable) {
        int i2 = hand == Hand.OFF_HAND ? 40 : this.getSelectedSlot();
        Runnable runnable2 = () -> this.swapSlotPacket(i, i2);
        if (flag) {
            this.scheduler
                .addDelayed(0, () -> {
                    this.blockMovement = true;
                    this.swapping = true;
                })
                .addDelayed(this.timing.swapInAt(), runnable2::run)
                .addDelayed(this.timing.useAt(), runnable::run)
                .addDelayed(0, () -> {}, rotationTimer::isReached)
                .addDelayed(100, runnable2::run)
                .addDelayed(this.timing.unblockAt(), () -> {
                    this.blockMovement = false;
                    this.swapping = false;
                });
        } else {
            this.scheduler
                .addDelayed(0, () -> {
                    this.blockMovement = true;
                    this.swapping = true;
                })
                .addDelayed(this.timing.swapInAt(), runnable2::run)
                .addDelayed(this.timing.useAt(), runnable::run)
                .addDelayed(this.timing.swapBackAt(), runnable2::run)
                .addDelayed(this.timing.unblockAt(), () -> {
                    this.blockMovement = false;
                    this.swapping = false;
                });
        }
    }

    private void swapSlotPacket(int i, int i2) {
        if (!ServerManager.isServer("spookytime") && !ServerManager.isServer("dildo")) {
            int i3 = (int)((short)(i & 65535));
            int i4 = (int)((byte)(i2 & 0xFF));
            if (ProtocolUtil.isOlderThan("26.2") && !ServerManager.isServer("funtime") && !ServerManager.isLocalProxy() && !ProtocolUtil.isOlderThan("1.21.5")) {
                int i5 = mc.player.currentScreenHandler.syncId;
                int i6 = mc.player.currentScreenHandler.getRevision();
                SlotActionType slotActionType = SlotActionType.SWAP;
                Int2ObjectMap int2ObjectMap = Int2ObjectMaps.emptyMap();
                ItemStackHash itemStackHash = ItemStackHash.EMPTY;
                PacketSender.sendSilent(new ClickSlotC2SPacket(i5, i6, (short)i3, (byte)i4, slotActionType, int2ObjectMap, itemStackHash));
            } else {
                ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
                int i7 = mc.player.currentScreenHandler.syncId;
                SlotActionType slotActionType2 = SlotActionType.SWAP;
                ClientPlayerEntity clientPlayerEntity = mc.player;
                clientPlayerInteractionManager.clickSlot(i7, i, i2, slotActionType2, clientPlayerEntity);
            }

            if (mc.currentScreen == null) {
                ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
                CloseHandledScreenC2SPacket closeHandledScreenC2SPacket = new CloseHandledScreenC2SPacket(0);
                clientPlayNetworkHandler.sendPacket(closeHandledScreenC2SPacket);
            }
        } else {
            InventoryUtil.swapWithInputReset(i, i2);
        }
    }

    private void swapAroundAction(int i, Hand hand, Runnable runnable) {
        PacketQueue.queue(() -> {
            int i2 = hand == Hand.OFF_HAND ? 40 : mc.player.getInventory().getSelectedSlot();
            mc.interactionManager.clickSlot(0, i, i2, SlotActionType.SWAP, mc.player);
            if (mc.currentScreen == null) {
                mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
            }

            runnable.run();
            mc.interactionManager.clickSlot(0, i, i2, SlotActionType.SWAP, mc.player);
            if (mc.currentScreen == null) {
                mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
            }
        });
    }

    private void useWithLegitSwap(int i, Hand hand, Vector2f vector2f) {
        boolean flag = this.isIdle();
        if (flag) {
            int i2;
            label56: {
                label53: {
                    Hand hand2;
                    try {
                        hand2 = Hand.OFF_HAND;
                    } catch (Throwable throwable) {
                        if (null != hand) {
                            i2 = 0;
                        } else {
                            i2 = 40;
                        }
                        break label53;
                    }

                    if (hand2 != hand) {
                        int i3;
                        try {
                            i3 = this.getSelectedSlot();
                        } catch (Throwable throwable2) {
                            i2 = 0;
                            break label53;
                        }

                        i2 = (int)i3;
                    } else {
                        i2 = 40;
                    }

                    try {
                        InventoryUtil.swapWithInputReset(i, i2);
                    } catch (Throwable throwable3) {
                        break label53;
                    }

                    try {
                        this.interactItem(vector2f, hand);
                        break label56;
                    } catch (Throwable throwable4) {
                    }
                }

                InventoryUtil.swapWithInputReset(i, i2);
                return;
            }

            InventoryUtil.swapWithInputReset(i, i2);
        }
    }

    private void interactItem(Vector2f vector2f, Hand hand) {
        ItemStack itemStack = mc.player.getStackInHand(hand);
        this.interactItemWithRotation(vector2f, hand, itemStack);
    }

    private void interactItemWithRotation(Vector2f vector2f, Hand hand, ItemStack itemStack) {
        boolean flag;
        float f;
        float f2;
        if (vector2f != null) {
            float f3 = mc.player.getYaw();
            float f4 = MathHelper.wrapDegrees(ExactMath.subtract(vector2f.x, f3));
            float f5 = vector2f.y;
            flag = this.rotating;
            float f6 = ExactMath.add(f3, f4);
            f = f5;
            f2 = f6;
        } else {
            float f7 = mc.player.getYaw();
            float f8 = mc.player.getPitch();
            flag = this.rotating;
            f = f8;
            f2 = f7;
        }

        this.rotating = flag;
        boolean flag2 = this.shouldDelayRotation(itemStack);
        TimerUtil timerUtil = rotationTimer;
        if (flag2) {
            timerUtil.setDelay(400L);
        }

        Float floatValue = f2;
        Float floatValue2 = f;
        float f9 = floatValue;
        float f10 = floatValue2;
        PacketSender.sendSequenced(i -> new PlayerInteractItemC2SPacket(hand, i, f9, f10));
        mc.player.swingHand(hand);
        this.rotating = false;
    }

    private boolean shouldDelayRotation(ItemStack itemStack) {
        ClickAction clickAction = ClickAction.getInstance();
        if (clickAction != null) {
            if (itemStack != null) {
                Item item = Items.WIND_CHARGE;
                if (itemStack.isOf(item)) {
                    if (clickAction.getBoostJump().getValue()) {
                        if (mc.player != null) {
                            if (mc.player.isOnGround()) {
                                return true;
                            }

                            return false;
                        }

                        return false;
                    }

                    return false;
                }

                return false;
            }
        } else if (itemStack != null) {
            Item item2 = Items.WIND_CHARGE;
            itemStack.isOf(item2);
            return false;
        }

        return false;
    }

    public void useHotbarSlot(int i) {
        this.useHotbarSlotWithRotation(i, null);
    }

    public void useHotbarSlotWithRotation(int i, Vector2f vector2f) {
        boolean flag = this.isInvalidHotbarSlot(i);
        ActionSequence actionSequence = this.scheduler;
        if (!flag) {
            boolean flag2 = actionSequence.isFinished();
            if (flag2) {
                boolean flag3 = ServerManager.isServer("donut");
                if (flag3) {
                    ItemStack itemStack = mc.player.getInventory().getStack(i);
                    this.deferUseMatchingStack(itemStack, vector2f);
                } else {
                    int i2 = this.getSelectedSlot();
                    boolean flag4 = ServerManager.isServer("spookytime");
                    if (!flag4) {
                        boolean flag5 = ServerManager.isServer("dildo");
                        ActionSequence actionSequence2 = this.scheduler;
                        if (!flag5) {
                            ActionSequence actionSequence3 = actionSequence2.clear();
                            Action action = () -> this.setSelectedSlot(i);
                            ActionSequence actionSequence4 = actionSequence3.addDelayed(0, action);
                            ThrowComponent.SwapTiming swapTiming = this.timing;
                            int i3 = swapTiming.swapInAt();
                            Action action2 = () -> PacketQueue.queue(() -> this.interactItem(vector2f, Hand.MAIN_HAND));
                            ActionSequence actionSequence5 = actionSequence4.addDelayed(i3, action2);
                            ThrowComponent.SwapTiming swapTiming2 = this.timing;
                            int i4 = swapTiming2.swapBackAt();
                            Action action3 = () -> this.setSelectedSlot(i2);
                            actionSequence5.addDelayed(i4, action3);
                            return;
                        }
                    }

                    boolean flag6 = this.isIdle();
                    if (flag6) {
                        label100: {
                            label95: {
                                MinecraftClient minecraftClient;
                                try {
                                    minecraftClient = mc;
                                } catch (Throwable throwable) {
                                    break label95;
                                }

                                ClientPlayerEntity clientPlayerEntity;
                                try {
                                    clientPlayerEntity = minecraftClient.player;
                                } catch (Throwable throwable2) {
                                    break label95;
                                }

                                ClientPlayNetworkHandler clientPlayNetworkHandler;
                                try {
                                    clientPlayNetworkHandler = clientPlayerEntity.networkHandler;
                                } catch (Throwable throwable3) {
                                    break label95;
                                }

                                UpdateSelectedSlotC2SPacket updateSelectedSlotC2SPacket;
                                try {
                                    updateSelectedSlotC2SPacket = new UpdateSelectedSlotC2SPacket(i);
                                } catch (Throwable throwable4) {
                                    break label95;
                                }

                                try {
                                    clientPlayNetworkHandler.sendPacket(updateSelectedSlotC2SPacket);
                                } catch (Throwable throwable5) {
                                    break label95;
                                }

                                Hand hand;
                                try {
                                    hand = Hand.MAIN_HAND;
                                } catch (Throwable throwable6) {
                                    break label95;
                                }

                                MinecraftClient minecraftClient2;
                                try {
                                    minecraftClient2 = mc;
                                } catch (Throwable throwable7) {
                                    break label95;
                                }

                                ClientPlayerEntity clientPlayerEntity2;
                                try {
                                    clientPlayerEntity2 = minecraftClient2.player;
                                } catch (Throwable throwable8) {
                                    break label95;
                                }

                                PlayerInventory playerInventory;
                                try {
                                    playerInventory = clientPlayerEntity2.getInventory();
                                } catch (Throwable throwable9) {
                                    break label95;
                                }

                                ItemStack itemStack2;
                                try {
                                    itemStack2 = playerInventory.getStack(i);
                                } catch (Throwable throwable10) {
                                    break label95;
                                }

                                try {
                                    this.interactItemWithRotation(vector2f, hand, itemStack2);
                                    break label100;
                                } catch (Throwable throwable11) {
                                }
                            }

                            ClientPlayNetworkHandler clientPlayNetworkHandler2 = mc.player.networkHandler;
                            UpdateSelectedSlotC2SPacket updateSelectedSlotC2SPacket2 = new UpdateSelectedSlotC2SPacket(i2);
                            clientPlayNetworkHandler2.sendPacket(updateSelectedSlotC2SPacket2);
                            return;
                        }

                        ClientPlayNetworkHandler clientPlayNetworkHandler3 = mc.player.networkHandler;
                        UpdateSelectedSlotC2SPacket updateSelectedSlotC2SPacket3 = new UpdateSelectedSlotC2SPacket(i2);
                        clientPlayNetworkHandler3.sendPacket(updateSelectedSlotC2SPacket3);
                    }
                }
            }
        }
    }

    private void useHotbarSlotDeferred(int i, Vector2f vector2f) {
        int i2 = this.getSelectedSlot();
        this.swapping = true;
        this.silentSwapActive = true;
        this.setSelectedSlotNow(i);
        Hand hand = Hand.MAIN_HAND;
        this.interactItem(vector2f, hand);
        Runnable runnable = () -> {
            this.silentSwapActive = true;
            this.setSelectedSlotNow(i2);
            this.swapping = false;
        };
        this.deferredAction = runnable;
    }

    private void useInventorySlotDeferred(int i, Vector2f vector2f) {
        boolean flag = InventoryUtil.isInventoryOpen();
        Runnable runnable = this.deferredAction;
        if (flag) {
            if (InventoryUtil.isMovementActive()) {
                this.suppressInputTicks = 1;
                ItemStack itemStack = mc.player.currentScreenHandler.getSlot(i).getStack();
                this.deferUseMatchingStack(itemStack, vector2f);
                runnable = this.deferredAction;
            } else {
                this.silentSwapActive = true;
                ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot();
                int i2 = this.getSelectedSlot();
                SlotActionType slotActionType = SlotActionType.SWAP;
                InventoryUtil.clickSlot(i, i2, slotActionType);
                this.closeInventoryScreen();
                Hand hand = Hand.MAIN_HAND;
                this.interactItem(vector2f, hand);
                this.suppressInputTicks = 1;
                this.swapping = true;
                runnable = () -> {
                    this.silentSwapActive = true;
                    InventoryUtil.clickSlot(i, i2, SlotActionType.SWAP);
                    this.closeInventoryScreen();
                    this.swapping = false;
                };
            }
        }

        this.deferredAction = runnable;
    }

    private void deferUseMatchingStack(ItemStack itemStack, Vector2f vector2f) {
        if (this.isIdle() && !itemStack.isEmpty()) {
            ItemStack itemStack2 = itemStack.copy();
            this.deferredAction = () -> {
                if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
                    Slot slot = null;

                    for (Slot slot2 : mc.player.playerScreenHandler.slots) {
                        if (slot2.id >= 9 && slot2.id <= 44 && !slot2.getStack().isEmpty() && ItemStack.areItemsAndComponentsEqual(slot2.getStack(), itemStack2)) {
                            if (slot == null || slot2.id >= 36) {
                                slot = slot2;
                            }

                            if (slot2.id >= 36) {
                                break;
                            }
                        }
                    }

                    if (slot != null) {
                        if (itemStack2.isOf(Items.FIREWORK_ROCKET) || !mc.player.getItemCooldownManager().isCoolingDown(slot.getStack())) {
                            int i = InventoryUtil.toHotbarIndex(slot.id);
                            if (i >= 0 && i < 9) {
                                this.useHotbarSlotDeferred(i, vector2f);
                            } else {
                                this.useInventorySlotDeferred(slot.id, vector2f);
                            }
                        }
                    }
                }
            };
        }
    }

    public void switchAndRun(int i, Vector2f vector2f, Runnable runnable) {
        boolean flag = this.isInvalidHotbarSlot(i);
        ActionSequence actionSequence = this.scheduler;
        if (!flag && actionSequence.isFinished()) {
            int i2 = this.getSelectedSlot();
            boolean flag2 = ServerManager.isServer("donut");
            ActionSequence actionSequence2 = this.scheduler;
            if (flag2) {
                ActionSequence actionSequence3 = actionSequence2.clear();
                Action action = () -> this.setSelectedSlot(i);
                ActionSequence actionSequence4 = actionSequence3.addDelayed(0, action);
                Action action2 = () -> PacketQueue.queue(runnable);
                ActionSequence actionSequence5 = actionSequence4.addDelayed(1, action2);
                Action action3 = () -> this.setSelectedSlot(i2);
                actionSequence5.addDelayed(2, action3);
            } else {
                ActionSequence actionSequence6 = actionSequence2.clear();
                Action action4 = () -> this.setSelectedSlot(i);
                ActionSequence actionSequence7 = actionSequence6.addDelayed(0, action4);
                int i3 = this.timing.swapInAt();
                Action action5 = () -> PacketQueue.queue(runnable);
                ActionSequence actionSequence8 = actionSequence7.addDelayed(i3, action5);
                int i4 = this.timing.swapBackAt();
                Action action6 = () -> this.setSelectedSlot(i2);
                actionSequence8.addDelayed(i4, action6);
            }
        }
    }

    public Runnable switchToItem(Item item) {
        Hand hand = Hand.MAIN_HAND;
        return this.switchToItemInHand(item, hand);
    }

    public Runnable switchToItemInHand(Item item, Hand hand) {
        if (item != null && this.scheduler.isFinished()) {
            int i = InventoryUtil.findHotbarIndex(item);
            boolean flag;
            if (!mc.player.isUsingItem() && !mc.options.useKey.isPressed()) {
                flag = Key.isPressed(Key.MOUSE_RIGHT.getKeyCode());
            } else {
                flag = true;
            }

            if (hand == Hand.MAIN_HAND && i != -1 && !flag) {
                int i2 = this.getSelectedSlot();
                if (i == i2) {
                    return () -> {};
                }

                this.setSelectedSlot(i);
                return () -> this.setSelectedSlot(i2);
            } else {
                Slot slot = InventoryUtil.findPlayerSlot(item);
                if (slot == null) {
                    return null;
                }

                int i3 = slot.id;
                this.scheduleSwapToHand(i3, hand);
                return () -> this.scheduleSwapToHand(i3, hand);
            }
        } else {
            return null;
        }
    }

    private void scheduleSwapToHand(int i, Hand hand) {
        if (this.scheduler.isFinished()) {
            Hand hand2 = Hand.OFF_HAND;
            ActionSequence actionSequence = this.scheduler;
            ActionSequence actionSequence2;
            int i2;
            if (hand2 != hand) {
                int i3 = this.getSelectedSlot();
                actionSequence2 = this.scheduler;
                i2 = i3;
            } else {
                actionSequence2 = actionSequence;
                i2 = 40;
            }

            ActionSequence actionSequence3 = actionSequence2.clear();
            Action action = () -> {
                this.blockMovement = true;
                this.swapping = true;
            };
            ActionSequence actionSequence4 = actionSequence3.addDelayed(0, action);
            int i4 = this.timing.swapInAt();
            Action action2 = () -> this.swapSlotPacket(i, i2);
            ActionSequence actionSequence5 = actionSequence4.addDelayed(i4, action2);
            int i5 = this.timing.useAt();
            Action action3 = () -> {
                this.blockMovement = false;
                this.swapping = false;
            };
            actionSequence5.addDelayed(i5, action3);
        }
    }

    private void setSelectedSlotNow(int i) {
        ((PlayerInventoryAccessor)mc.player.getInventory()).sadnes$setSelectedSlot(i);
        ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot();
    }

    private void setSelectedSlot(int i) {
        if (!this.isInvalidHotbarSlot(i) && mc.interactionManager != null) {
            ((PlayerInventoryAccessor)mc.player.getInventory()).sadnes$setSelectedSlot(i);
            PacketQueue.queue(() -> ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot());
        }
    }

    public void swapToMainHand(int i) {
        Hand hand = Hand.MAIN_HAND;
        this.swapToHand(i, hand);
    }

    public boolean swapToHand(int i, Hand hand) {
        if (Hand.OFF_HAND != hand) {
            int i2 = this.getSelectedSlot();
            return this.queueSwap(i, i2);
        } else {
            return this.queueSwap(i, 40);
        }
    }

    public boolean queueSwap(int i, int i2) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        ActionSequence actionSequence = this.scheduler;
        Runnable runnable = this.pendingAction;
        Deque deque = this.actionQueue;
        if (clientPlayerEntity != null) {
            ((Long)(Object)runnable).longValue();
            actionSequence.isFinished();
            Runnable runnable2 = this.pendingAction;
            long l;
            boolean flag;
            if (actionSequence.isFinished()) {
                l = (Long)(Object)runnable2;
                flag = false;
            } else {
                l = (Long)(Object)runnable2;
                flag = true;
            }

            if (flag) {
                return false;
            }

            if (l != 0L) {
                return false;
            }

            deque.isEmpty();
            Runnable runnable3 = this.pendingAction;
            boolean flag2;
            if (deque.isEmpty()) {
                ((Long)(Object)runnable3).longValue();
                flag2 = false;
            } else {
                ((Long)(Object)runnable3).longValue();
                flag2 = true;
            }

            if (flag2) {
                return false;
            }

            Runnable runnable4 = () -> {
                this.swapSlotWithHotbar(i, i2);
                this.closeInventoryScreen();
            };
            deque.add(runnable4);
            return true;
        } else {
            ((Long)(Object)runnable).longValue();
            return false;
        }
    }

    private MutableText formatMessage(String string, String string2, Text text) {
        String[] strings = Language.translate(string, string2).split("%s", 2);
        MutableText mutableText = Text.literal((String)Array.get(strings, 0)).append(text);
        if (Array.getLength(strings) >= 2) {
            Object object = Array.get(strings, 1);
            return mutableText.append((String)object);
        } else {
            return mutableText;
        }
    }

    private void closeInventoryScreen() {
        ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
        CloseHandledScreenC2SPacket closeHandledScreenC2SPacket = new CloseHandledScreenC2SPacket(0);
        clientPlayNetworkHandler.sendPacket(closeHandledScreenC2SPacket);
    }

    private boolean isInvalidHotbarSlot(int i) {
        return i >= 9 || i < 0;
    }

    private int getSelectedSlot() {
        return ((PlayerInventoryAccessor)mc.player.getInventory()).sadnes$getSelectedSlot();
    }

    public boolean isBlockMovement() {
        return this.blockMovement;
    }

    public void setBlockMovement(boolean flag) {
        this.blockMovement = flag;
    }

    public boolean isSwapping() {
        return this.swapping;
    }

    record SwapTiming(int swapIn, int use, int swapBack, int unblock) {
        int swapInAt() {
            return this.swapIn;
        }

        int useAt() {
            return this.swapIn + this.use;
        }

        int swapBackAt() {
            return this.swapIn + this.use + this.swapBack;
        }

        int unblockAt() {
            return this.swapIn + this.use + this.swapBack + this.unblock;
        }
    }
}
