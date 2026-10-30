package tech.sadnes.client.feature.impl.movement;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.api.mixin.accessor.KeyBindingAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.CloseScreenEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.PlayerInputPacketEvent;
import tech.sadnes.client.event.impl.PostMotionEvent;
import tech.sadnes.client.event.impl.SlotClickEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.event.impl.UseItemEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.feature.impl.misc.AutoDonutSell;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.util.compat.ProtocolUtil;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.network.PacketQueue;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.MoveUtil;

@FeatureInfo(name = "GuiWalk", description = "feature.impl.movement.guiwalk", category = Category.MOVEMENT, subcategory = Subcategory.MOVEMENT_ASSIST)
public class GuiWalk extends Feature {
    public static final List<GuiWalk.QueuedClick> QUEUED_CLICKS = new CopyOnWriteArrayList<>();
    private final BooleanSetting bypass = new BooleanSetting("guiwalk.bypass", false);
    private final BooleanSetting allScreens = new BooleanSetting("guiwalk.allscreens", false);
    private boolean replayingClicks;
    private int lastSlot = -1;
    private boolean groundReady;
    private boolean slowdownApplied;
    private final List<ItemStack> slotSnapshot = new ArrayList<>();
    private ItemStack cursorSnapshot = ItemStack.EMPTY;
    private final Deque<ClickSlotC2SPacket> delayedClickPackets = new ArrayDeque<>();
    private ClickSlotC2SPacket pendingClickPacket;
    private boolean sendingDelayedClick;
    private int closeStage;
    private final StopWatch screenOpenTimer = new StopWatch();
    private volatile long lastClickTime;

    public boolean isStrictServer() {
        return ServerManager.isFunTime() || ServerManager.isDonut() || ServerManager.isServer("thunderhack");
    }

    public boolean isBypassSupported() {
        return !ProtocolUtil.isOlderThan("1.21.5") && !this.isStrictServer();
    }

    public boolean isBypassActive() {
        return this.isEnabled() && this.bypass.getValue() && this.isBypassSupported();
    }

    public static GuiWalk getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(GuiWalk.class);
    }

    public static boolean isDonutServer() {
        return ServerManager.isDonut();
    }

    public void onPacketSent(Packet<?> packet) {
        if (packet instanceof ClickSlotC2SPacket && ((ClickSlotC2SPacket)packet).syncId() == 0 && ServerManager.isServer("donut")) {
            long l = System.currentTimeMillis();
            this.lastClickTime = l;
        }
    }

    public boolean shouldDelayPacket(Packet<?> packet) {
        boolean flag;
        if (packet instanceof ScreenHandlerSlotUpdateS2CPacket && ((ScreenHandlerSlotUpdateS2CPacket)packet).getSyncId() == 0) {
            flag = false;
        } else if (packet instanceof InventoryS2CPacket && ((InventoryS2CPacket)packet).syncId() == 0) {
            flag = false;
        } else {
            flag = !(packet instanceof SetPlayerInventoryS2CPacket);
        }

        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        PlayerScreenHandler playerScreenHandler2 = mc.player.playerScreenHandler;
        if (!flag) {
            if (screenHandler == playerScreenHandler2) {
                if (ServerManager.isServer("donut")) {
                    boolean flag2;
                    if (this.isEnabled()) {
                        flag2 = mc.currentScreen instanceof InventoryScreen;
                    } else {
                        flag2 = false;
                    }

                    long l = System.currentTimeMillis();
                    long l2 = this.lastClickTime;
                    int i = ServerManager.getPing();
                    long l3 = l - l2;
                    long l4 = i + 100;
                    return l3 >= l4 ? false | flag2 : true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        boolean flag = this.sendingDelayedClick;
        ClickSlotC2SPacket clickSlotC2SPacket = this.pendingClickPacket;
        boolean flag2 = this.slowdownApplied;
        boolean flag3 = this.groundReady;
        boolean flag4;
        boolean flag5;
        ClickSlotC2SPacket clickSlotC2SPacket2;
        if (clientPlayerEntity != null) {
            boolean flag6;
            label201: {
                Deque deque;
                label200: {
                    boolean flag7;
                    ClickSlotC2SPacket clickSlotC2SPacket3;
                    label205: {
                        boolean flag8 = mc.player.hasVehicle();
                        boolean flag9 = this.sendingDelayedClick;
                        ClickSlotC2SPacket clickSlotC2SPacket4 = this.pendingClickPacket;
                        deque = this.delayedClickPackets;
                        boolean flag10 = this.slowdownApplied;
                        if (flag8) {
                            flag9 = false;
                            if (clickSlotC2SPacket4 == null) {
                                flag7 = false;
                                clickSlotC2SPacket3 = clickSlotC2SPacket4;
                                break label205;
                            }
                        } else {
                            if (clickSlotC2SPacket4 == null) {
                                flag7 = flag9;
                                clickSlotC2SPacket3 = clickSlotC2SPacket4;
                                break label205;
                            }

                            if (flag9) {
                                flag6 = flag10;
                                if (!flag10 || clickSlotC2SPacket4 != null) {
                                    break label201;
                                }
                                break label200;
                            }
                        }

                        this.sendingDelayedClick = flag9;
                        PacketSender.sendSilent(clickSlotC2SPacket4);
                        boolean flag11 = deque.isEmpty();
                        flag7 = this.sendingDelayedClick;
                        if (flag11) {
                            boolean flag12 = mc.player.currentScreenHandler.getCursorStack().isEmpty();
                            flag7 = this.sendingDelayedClick;
                            if (flag12) {
                                ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
                                CloseHandledScreenC2SPacket closeHandledScreenC2SPacket = new CloseHandledScreenC2SPacket(0);
                                clientPlayNetworkHandler.sendPacket(closeHandledScreenC2SPacket);
                                flag7 = this.sendingDelayedClick;
                            }
                        }

                        clickSlotC2SPacket3 = null;
                    }

                    this.sendingDelayedClick = flag7;
                    this.pendingClickPacket = clickSlotC2SPacket3;
                    boolean flag13 = deque.isEmpty();
                    ClickSlotC2SPacket clickSlotC2SPacket5 = this.pendingClickPacket;
                    boolean flag14 = this.slowdownApplied;
                    if (!flag13) {
                        ClickSlotC2SPacket clickSlotC2SPacket6 = (ClickSlotC2SPacket)deque.poll();
                        this.sendingDelayedClick = true;
                        this.pendingClickPacket = clickSlotC2SPacket6;
                        PacketQueue.queue(() -> mc.player.setSprinting(false));
                        ClickSlotC2SPacket clickSlotC2SPacket7 = this.pendingClickPacket;
                        boolean flag15 = this.slowdownApplied;
                        flag6 = flag15;
                        if (!flag15 || clickSlotC2SPacket7 != null) {
                            break label201;
                        }
                    } else {
                        flag6 = flag14;
                        if (!flag14 || clickSlotC2SPacket5 != null) {
                            break label201;
                        }
                    }
                }

                boolean flag16 = deque.isEmpty();
                boolean flag17 = this.slowdownApplied;
                if (flag16) {
                    ScreenHandler screenHandler = mc.player.currentScreenHandler;
                    PlayerScreenHandler playerScreenHandler2 = mc.player.playerScreenHandler;
                    boolean flag18 = this.slowdownApplied;
                    if (screenHandler == playerScreenHandler2) {
                        boolean flag19 = mc.player.currentScreenHandler.getCursorStack().isEmpty();
                        boolean flag20 = this.slowdownApplied;
                        if (flag19) {
                            ClientPlayNetworkHandler clientPlayNetworkHandler2 = mc.player.networkHandler;
                            CloseHandledScreenC2SPacket closeHandledScreenC2SPacket2 = new CloseHandledScreenC2SPacket(0);
                            clientPlayNetworkHandler2.sendPacket(closeHandledScreenC2SPacket2);
                            flag6 = false;
                        } else {
                            flag6 = flag20;
                        }
                    } else {
                        flag6 = flag18;
                    }
                } else {
                    flag6 = flag17;
                }
            }

            this.slowdownApplied = flag6;
            Screen screen = mc.currentScreen;
            flag = this.sendingDelayedClick;
            ClickSlotC2SPacket clickSlotC2SPacket8 = this.pendingClickPacket;
            boolean flag21 = this.slowdownApplied;
            boolean flag22 = this.groundReady;
            if (screen != null) {
                boolean flag23 = mc.player.isGliding();
                flag = this.sendingDelayedClick;
                clickSlotC2SPacket2 = this.pendingClickPacket;
                boolean flag24 = this.slowdownApplied;
                boolean flag25 = this.groundReady;
                if (!flag23) {
                    boolean flag26 = mc.currentScreen instanceof ChatScreen;
                    flag = this.sendingDelayedClick;
                    clickSlotC2SPacket2 = this.pendingClickPacket;
                    boolean flag27 = this.slowdownApplied;
                    boolean flag28 = this.groundReady;
                    if (!flag26) {
                        boolean flag29 = mc.currentScreen instanceof BookEditScreen;
                        flag = this.sendingDelayedClick;
                        clickSlotC2SPacket2 = this.pendingClickPacket;
                        boolean flag30 = this.slowdownApplied;
                        boolean flag31 = this.groundReady;
                        if (!flag29) {
                            boolean flag32 = mc.currentScreen instanceof SignEditScreen;
                            flag = this.sendingDelayedClick;
                            clickSlotC2SPacket2 = this.pendingClickPacket;
                            boolean flag33 = this.slowdownApplied;
                            boolean flag34 = this.groundReady;
                            if (flag32) {
                                flag4 = flag34;
                                flag5 = flag33;
                            } else {
                                Screen screen2 = mc.currentScreen;
                                boolean flag35 = this.isAllowedScreen(screen2);
                                flag = this.sendingDelayedClick;
                                clickSlotC2SPacket2 = this.pendingClickPacket;
                                boolean flag36 = this.slowdownApplied;
                                boolean flag37 = this.groundReady;
                                label175:
                                if (flag35) {
                                    boolean flag38;
                                    label173: {
                                        boolean flag39 = mc.currentScreen instanceof InventoryScreen;
                                        boolean flag40 = this.groundReady;
                                        if (!flag39) {
                                            boolean flag41 = mc.currentScreen instanceof ClickGuiScreen;
                                            boolean flag42 = this.groundReady;
                                            label171:
                                            if (!flag41) {
                                                if (!ServerManager.isServer("funtime") && !ServerManager.isLocalProxy()) {
                                                    boolean flag43 = ServerManager.isServer("holy");
                                                    boolean flag44 = this.groundReady;
                                                    if (!flag43) {
                                                        flag38 = flag44;
                                                        if (!flag44) {
                                                            break label173;
                                                        }
                                                        break label171;
                                                    }
                                                }

                                                boolean flag45 = ServerManager.isInHub();
                                                flag = this.sendingDelayedClick;
                                                clickSlotC2SPacket2 = this.pendingClickPacket;
                                                boolean flag46 = this.slowdownApplied;
                                                boolean flag47 = this.groundReady;
                                                if (!flag45) {
                                                    flag4 = flag47;
                                                    flag5 = flag46;
                                                    break label175;
                                                }

                                                flag38 = flag47;
                                                if (!flag47) {
                                                    break label173;
                                                }
                                            } else {
                                                flag38 = flag42;
                                                if (!flag42) {
                                                    break label173;
                                                }
                                            }
                                        } else {
                                            flag38 = flag40;
                                            if (!flag40) {
                                                break label173;
                                            }
                                        }

                                        boolean flag48 = this.isBypassSupported();
                                        boolean flag49 = this.groundReady;
                                        if (flag48) {
                                            flag38 = flag49;
                                        } else {
                                            boolean flag50 = mc.currentScreen instanceof InventoryScreen;
                                            boolean flag51 = this.groundReady;
                                            label158:
                                            if (flag50) {
                                                if (!MoveUtil.isMoving()) {
                                                    boolean flag52 = mc.options.jumpKey.isPressed();
                                                    boolean flag53 = this.groundReady;
                                                    if (!flag52) {
                                                        flag38 = flag53;
                                                        break label158;
                                                    }
                                                }

                                                boolean flag54 = mc.player.currentScreenHandler.getCursorStack().isEmpty();
                                                boolean flag55 = this.groundReady;
                                                if (flag54) {
                                                    ClientPlayNetworkHandler clientPlayNetworkHandler3 = mc.player.networkHandler;
                                                    CloseHandledScreenC2SPacket closeHandledScreenC2SPacket3 = new CloseHandledScreenC2SPacket(0);
                                                    clientPlayNetworkHandler3.sendPacket(closeHandledScreenC2SPacket3);
                                                    flag38 = false;
                                                } else {
                                                    flag38 = flag55;
                                                }
                                            } else {
                                                flag38 = flag51;
                                            }
                                        }
                                    }

                                    this.groundReady = flag38;
                                    KeyBinding[] keyBindings = new KeyBinding[6];
                                    KeyBinding keyBinding = mc.options.forwardKey;
                                    Array.set(keyBindings, 0, keyBinding);
                                    KeyBinding keyBinding2 = mc.options.backKey;
                                    Array.set(keyBindings, 1, keyBinding2);
                                    KeyBinding keyBinding3 = mc.options.leftKey;
                                    Array.set(keyBindings, 2, keyBinding3);
                                    KeyBinding keyBinding4 = mc.options.rightKey;
                                    Array.set(keyBindings, 3, keyBinding4);
                                    KeyBinding keyBinding5 = mc.options.jumpKey;
                                    Array.set(keyBindings, 4, keyBinding5);
                                    KeyBinding keyBinding6 = mc.options.sprintKey;
                                    Array.set(keyBindings, 5, keyBinding6);
                                    if (!ClickGuiScreen.isTypingText()) {
                                        Screen screen3 = mc.currentScreen;
                                        if (!this.hasFocusedTextInput(screen3)) {
                                            int i = Array.getLength(keyBindings);
                                            flag = this.sendingDelayedClick;
                                            ClickSlotC2SPacket clickSlotC2SPacket9 = this.pendingClickPacket;
                                            boolean flag56 = this.slowdownApplied;
                                            boolean flag57 = this.groundReady;
                                            if (i > 0) {
                                                int i2 = i & 2147483647;
                                                flag4 = flag57;
                                                long l = 0L;
                                                flag5 = flag56;

                                                do {
                                                    this.sendingDelayedClick = flag;
                                                    this.pendingClickPacket = clickSlotC2SPacket9;
                                                    this.slowdownApplied = flag5;
                                                    this.groundReady = flag4;
                                                    int i3 = (int)l;
                                                    Object object = Array.get(keyBindings, i3);
                                                    Key key = ((KeyBindingAccessor)object).sadnes$getBoundKey();
                                                    Type type = key.getCategory();
                                                    Type type2 = Type.KEYSYM;
                                                    flag = this.sendingDelayedClick;
                                                    ClickSlotC2SPacket clickSlotC2SPacket10 = this.pendingClickPacket;
                                                    boolean flag58 = this.slowdownApplied;
                                                    boolean flag59 = this.groundReady;
                                                    if (type == type2) {
                                                        Window window = mc.getWindow();
                                                        int i4 = key.getCode();
                                                        boolean flag60 = InputUtil.isKeyPressed(window, i4);
                                                        ((KeyBinding)object).setPressed(flag60);
                                                        boolean flag61 = this.sendingDelayedClick;
                                                        clickSlotC2SPacket9 = this.pendingClickPacket;
                                                        boolean flag62 = this.slowdownApplied;
                                                        flag4 = this.groundReady;
                                                        flag = flag61;
                                                        flag5 = flag62;
                                                    } else {
                                                        flag4 = flag59;
                                                        flag5 = flag58;
                                                        clickSlotC2SPacket9 = clickSlotC2SPacket10;
                                                    }

                                                    l += 1L;
                                                } while (Integer.toUnsignedLong(i2) != l);

                                                clickSlotC2SPacket2 = clickSlotC2SPacket9;
                                            } else {
                                                flag4 = flag57;
                                                flag5 = flag56;
                                                clickSlotC2SPacket2 = clickSlotC2SPacket9;
                                            }
                                            break label175;
                                        }
                                    }

                                    int i5 = Array.getLength(keyBindings);
                                    flag = this.sendingDelayedClick;
                                    ClickSlotC2SPacket clickSlotC2SPacket11 = this.pendingClickPacket;
                                    boolean flag63 = this.slowdownApplied;
                                    boolean flag64 = this.groundReady;
                                    if (i5 > 0) {
                                        int i6 = i5 & 2147483647;
                                        flag4 = flag64;
                                        long l2 = 0L;
                                        flag5 = flag63;

                                        do {
                                            this.sendingDelayedClick = flag;
                                            this.pendingClickPacket = clickSlotC2SPacket11;
                                            this.slowdownApplied = flag5;
                                            this.groundReady = flag4;
                                            int i7 = (int)l2;
                                            Object object2 = Array.get(keyBindings, i7);
                                            Type type3 = ((KeyBindingAccessor)object2).sadnes$getBoundKey().getCategory();
                                            Type type4 = Type.KEYSYM;
                                            flag = this.sendingDelayedClick;
                                            ClickSlotC2SPacket clickSlotC2SPacket12 = this.pendingClickPacket;
                                            boolean flag65 = this.slowdownApplied;
                                            boolean flag66 = this.groundReady;
                                            if (type3 == type4) {
                                                ((KeyBinding)object2).setPressed(false);
                                                boolean flag67 = this.sendingDelayedClick;
                                                clickSlotC2SPacket11 = this.pendingClickPacket;
                                                boolean flag68 = this.slowdownApplied;
                                                flag4 = this.groundReady;
                                                flag = flag67;
                                                flag5 = flag68;
                                            } else {
                                                flag4 = flag66;
                                                flag5 = flag65;
                                                clickSlotC2SPacket11 = clickSlotC2SPacket12;
                                            }

                                            l2 += 1L;
                                        } while (Integer.toUnsignedLong(i6) != l2);

                                        clickSlotC2SPacket2 = clickSlotC2SPacket11;
                                    } else {
                                        flag4 = flag64;
                                        flag5 = flag63;
                                        clickSlotC2SPacket2 = clickSlotC2SPacket11;
                                    }
                                } else {
                                    flag4 = flag37;
                                    flag5 = flag36;
                                }
                            }
                        } else {
                            flag4 = flag31;
                            flag5 = flag30;
                        }
                    } else {
                        flag4 = flag28;
                        flag5 = flag27;
                    }
                } else {
                    flag4 = flag25;
                    flag5 = flag24;
                }
            } else {
                flag4 = flag22;
                flag5 = flag21;
                clickSlotC2SPacket2 = clickSlotC2SPacket8;
            }
        } else {
            flag4 = flag3;
            flag5 = flag2;
            clickSlotC2SPacket2 = clickSlotC2SPacket;
        }

        this.sendingDelayedClick = flag;
        this.pendingClickPacket = clickSlotC2SPacket2;
        this.slowdownApplied = flag5;
        this.groundReady = flag4;
    }

    @EventHandler
    public void onInput(SlotClickEvent slotClickEvent) {
        BooleanSetting booleanSetting = this.bypass;
        Object object = booleanSetting.getValue();
        Boolean flag = (Boolean)object;
        boolean flag2 = flag;
        if (flag2) {
            boolean flag3 = this.isBypassSupported();
            boolean flag4 = this.replayingClicks;
            if (!flag3) {
                if (!flag4) {
                    boolean flag5 = GuiMoveComponent.isExecutingAction();
                    if (!flag5) {
                        MinecraftClient minecraftClient = mc;
                        Screen screen = minecraftClient.currentScreen;
                        boolean flag6 = screen instanceof InventoryScreen;
                        if (flag6) {
                            boolean flag7 = MoveUtil.isMoving();
                            boolean flag8 = this.groundReady;
                            boolean flag9 = this.slowdownApplied;
                            boolean flag10;
                            boolean flag11;
                            if (!flag7) {
                                MinecraftClient minecraftClient2 = mc;
                                GameOptions gameOptions = minecraftClient2.options;
                                KeyBinding keyBinding = gameOptions.jumpKey;
                                boolean flag12 = keyBinding.isPressed();
                                boolean flag13 = this.groundReady;
                                boolean flag14 = this.slowdownApplied;
                                if (flag12) {
                                    flag10 = flag14;
                                    flag11 = flag13;
                                } else {
                                    MinecraftClient minecraftClient3 = mc;
                                    ClientPlayerEntity clientPlayerEntity = minecraftClient3.player;
                                    Vec3d vec3d = clientPlayerEntity.getVelocity();
                                    double d = vec3d.y;
                                    boolean flag15 = this.groundReady;
                                    boolean flag16 = this.slowdownApplied;
                                    int i = !Double.isNaN(d) ? (d == -0.0784000015258789 ? 0 : 1) : 0;
                                    if (!Double.isNaN(d)) {
                                        if (i == 0) {
                                            this.groundReady = true;
                                            this.slowdownApplied = flag16;
                                            boolean flag17 = this.isStrictServer();
                                            if (flag17) {
                                                this.slowdownApplied = true;
                                                return;
                                            }

                                            return;
                                        }

                                        flag10 = flag16;
                                        flag11 = flag15;
                                    } else {
                                        flag10 = flag16;
                                        flag11 = flag15;
                                    }
                                }
                            } else {
                                flag10 = flag9;
                                flag11 = flag8;
                            }

                            this.groundReady = flag11;
                            this.slowdownApplied = flag10;
                            boolean flag18 = this.isStrictServer();
                            List list = QUEUED_CLICKS;
                            if (flag18) {
                                slotClickEvent.cancel();
                                int i2 = slotClickEvent.getSyncId();
                                int i3 = slotClickEvent.getSlotId();
                                int i4 = slotClickEvent.getButton();
                                SlotActionType slotActionType = slotClickEvent.getActionType();
                                Runnable runnable = () -> {
                                    if (mc.player != null && mc.interactionManager != null) {
                                        mc.interactionManager.clickSlot(i2, i3, i4, slotActionType, mc.player);
                                    }
                                };
                                GuiMoveComponent.scheduleAction(runnable, 0.95F, 0.95F);
                            } else {
                                boolean flag19 = list.isEmpty();
                                List list2 = QUEUED_CLICKS;
                                if (flag19) {
                                    this.snapshotInventory();
                                    list2 = QUEUED_CLICKS;
                                }

                                int i5 = slotClickEvent.getSyncId();
                                int i6 = slotClickEvent.getSlotId();
                                int i7 = slotClickEvent.getButton();
                                SlotActionType slotActionType2 = slotClickEvent.getActionType();
                                GuiWalk.QueuedClick queuedClick = new GuiWalk.QueuedClick(i5, i6, i7, slotActionType2);
                                list2.add(queuedClick);
                                slotClickEvent.cancel();
                                MinecraftClient minecraftClient4 = mc;
                                ClientPlayerEntity clientPlayerEntity2 = minecraftClient4.player;
                                ScreenHandler screenHandler = clientPlayerEntity2.currentScreenHandler;
                                int i8 = slotClickEvent.getSlotId();
                                int i9 = slotClickEvent.getButton();
                                SlotActionType slotActionType3 = slotClickEvent.getActionType();
                                MinecraftClient minecraftClient5 = mc;
                                ClientPlayerEntity clientPlayerEntity3 = minecraftClient5.player;
                                screenHandler.onSlotClick(i8, i9, slotActionType3, clientPlayerEntity3);
                            }
                        }
                    }
                }
            }
        }
    }

    private void snapshotInventory() {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        this.slotSnapshot.clear();
        Iterator iterator2 = screenHandler.slots.iterator();
        if (iterator2.hasNext()) {
            do {
                Slot slot = (Slot)iterator2.next();
                List list = this.slotSnapshot;
                ItemStack itemStack = slot.getStack().copy();
                list.add(itemStack);
            } while (iterator2.hasNext());
        }

        ItemStack itemStack2 = screenHandler.getCursorStack().copy();
        this.cursorSnapshot = itemStack2;
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        BooleanSetting booleanSetting = this.bypass;
        if (clientPlayerEntity != null) {
            if (booleanSetting.getValue()) {
                Packet packet = packetEvent.getPacket();
                boolean flag = packet instanceof CloseHandledScreenC2SPacket;
                boolean flag2 = this.groundReady;
                boolean flag3 = this.slowdownApplied;
                boolean flag4;
                boolean flag5;
                if (flag) {
                    flag4 = false;
                    flag5 = false;
                } else {
                    flag4 = flag2;
                    flag5 = flag3;
                }

                this.groundReady = flag4;
                this.slowdownApplied = flag5;
                if (mc.currentScreen instanceof InventoryScreen) {
                    boolean flag6 = this.isBypassSupported();
                    boolean flag7 = this.groundReady;
                    boolean flag8 = this.slowdownApplied;
                    boolean flag9;
                    boolean flag10;
                    if (flag6) {
                        boolean flag11 = packet instanceof ClickSlotC2SPacket;
                        boolean flag12 = this.groundReady;
                        boolean flag13 = this.slowdownApplied;
                        label71:
                        if (flag11) {
                            ClickSlotC2SPacket clickSlotC2SPacket = (ClickSlotC2SPacket)packet;
                            boolean flag14 = MoveUtil.isMoving();
                            boolean flag15 = this.groundReady;
                            boolean flag16 = this.slowdownApplied;
                            boolean flag17;
                            boolean flag18;
                            if (flag14) {
                                flag17 = flag15;
                                flag18 = flag16;
                            } else {
                                double d = mc.player.getVelocity().y;
                                flag9 = this.groundReady;
                                boolean flag19 = this.slowdownApplied;
                                int i = !Double.isNaN(d) ? (d == -0.0784000015258789 ? 0 : 1) : 0;
                                if (!Double.isNaN(d) && i == 0) {
                                    flag10 = true;
                                    break label71;
                                }

                                this.groundReady = flag9;
                                this.slowdownApplied = flag19;
                                double d2 = mc.player.getVelocity().y;
                                boolean flag20 = this.groundReady;
                                boolean flag21 = this.slowdownApplied;
                                int i2 = !Double.isNaN(d2) ? (d2 == -0.005 ? 0 : 1) : 0;
                                if (!Double.isNaN(d2)) {
                                    if (i2 == 0) {
                                        flag9 = flag20;
                                        flag10 = true;
                                        break label71;
                                    }

                                    flag17 = flag20;
                                    flag18 = flag21;
                                } else {
                                    flag17 = flag20;
                                    flag18 = flag21;
                                }
                            }

                            this.groundReady = flag17;
                            this.slowdownApplied = flag18;
                            packetEvent.cancel();
                            this.delayedClickPackets.add(clickSlotC2SPacket);
                            boolean flag22 = this.groundReady;
                            boolean flag23 = this.slowdownApplied;
                            flag9 = flag22;
                            flag10 = flag23;
                        } else {
                            flag9 = flag12;
                            flag10 = flag13;
                        }
                    } else {
                        flag9 = flag7;
                        flag10 = flag8;
                    }

                    this.groundReady = flag9;
                    this.slowdownApplied = flag10;
                    if (packet instanceof CloseScreenS2CPacket) {
                        if (!this.isBypassSupported()) {
                            if (!ServerManager.isServer("holy")) {
                                packetEvent.cancel();
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = -250)
    public void onPostTick(MoveInputEvent moveInputEvent) {
        int i;
        boolean flag;
        label78: {
            boolean flag2 = ServerManager.isServer("donut");
            int i2 = this.closeStage;
            if (flag2) {
                boolean flag3 = mc.player.currentScreenHandler.getCursorStack().isEmpty();
                i2 = this.closeStage;
                if (!flag3) {
                    i = 5;
                    flag = false;
                    break label78;
                }

                if (i2 != 2) {
                    i = i2;
                    flag = true;
                    break label78;
                }
            } else if (i2 != 2) {
                i = i2;
                flag = true;
                break label78;
            }

            ScreenHandler screenHandler = mc.player.currentScreenHandler;
            PlayerScreenHandler playerScreenHandler2 = mc.player.playerScreenHandler;
            int i3 = this.closeStage;
            if (screenHandler == playerScreenHandler2) {
                ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
                CloseHandledScreenC2SPacket closeHandledScreenC2SPacket = new CloseHandledScreenC2SPacket(0);
                clientPlayNetworkHandler.sendPacket(closeHandledScreenC2SPacket);
                i = this.closeStage;
                flag = true;
            } else {
                i = i3;
                flag = true;
            }
        }

        this.closeStage = i;
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        String string = "tech.sadnes.client.feature.impl.misc.AutoDonutSell";

        Class clazz;
        try {
            clazz = Class.forName(string, false, GuiWalk.class.getClassLoader());
        } catch (ClassNotFoundException classNotFoundException) {
            throw new ExceptionInInitializerError(classNotFoundException);
        }

        int i4;
        label69: {
            int i5;
            label68: {
                Class clazz2 = clazz;
                AutoDonutSell autoDonutSell = (AutoDonutSell) featureManager.getFeature(clazz2);
                i5 = this.closeStage;
                boolean flag4 = this.sendingDelayedClick;
                if (autoDonutSell != null) {
                    boolean flag5 = autoDonutSell.isEnabled();
                    i5 = this.closeStage;
                    boolean flag6 = this.sendingDelayedClick;
                    if (flag5) {
                        boolean flag7 = autoDonutSell.isBusy();
                        i5 = this.closeStage;
                        boolean flag8 = this.sendingDelayedClick;
                        if (!flag7 && !flag8 && i5 <= 0) {
                            break label68;
                        }
                    } else if (!flag6 && i5 <= 0) {
                        break label68;
                    }
                } else if (!flag4 && i5 <= 0) {
                    break label68;
                }

                moveInputEvent.setCancelled(true);
                int i6 = this.closeStage;
                if (flag) {
                    if (i6 > 0) {
                        i4 = i6 - 1;
                    } else {
                        i4 = i6;
                    }
                } else {
                    i4 = i6;
                }
                break label69;
            }

            if (flag) {
                if (i5 > 0) {
                    i4 = i5 - 1;
                } else {
                    i4 = i5;
                }
            } else {
                i4 = i5;
            }
        }

        this.closeStage = i4;
    }

    @EventHandler
    public void onMovementTick(UseItemEvent useItemEvent) {
        boolean flag = ServerManager.isServer("donut");
        StopWatch stopWatch = this.screenOpenTimer;
        if (flag) {
            if (stopWatch.hasPassed(200.0)) {
                useItemEvent.getStage();
            } else {
                UseItemEvent.Stage stage = useItemEvent.getStage();
                UseItemEvent.Stage stage2 = UseItemEvent.Stage.START;
                if (stage == stage2) {
                    useItemEvent.cancel();
                }
            }
        } else {
            useItemEvent.getStage();
        }
    }

    @EventHandler
    public void onSendPackets(PlayerInputPacketEvent playerInputPacketEvent) {
        ClickSlotC2SPacket clickSlotC2SPacket = this.pendingClickPacket;
        boolean flag = this.sendingDelayedClick;
        if (clickSlotC2SPacket != null && flag) {
            PlayerInput playerInput = PlayerInput.DEFAULT;
            playerInputPacketEvent.setInput(playerInput);
            ClickSlotC2SPacket clickSlotC2SPacket2 = this.pendingClickPacket;
            playerInputPacketEvent.addPacket(clickSlotC2SPacket2);
            if (!this.delayedClickPackets.isEmpty()) {
                this.pendingClickPacket = null;
            } else if (mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                CloseHandledScreenC2SPacket closeHandledScreenC2SPacket = new CloseHandledScreenC2SPacket(0);
                playerInputPacketEvent.addPacket(closeHandledScreenC2SPacket);
                this.pendingClickPacket = null;
            } else {
                this.pendingClickPacket = null;
            }
        }
    }

    @EventHandler
    public void onWorldChange(PostMotionEvent postMotionEvent) {
        this.sendingDelayedClick = false;
    }

    @EventHandler
    public void onScreenClose(CloseScreenEvent closeScreenEvent) {
        boolean flag = ServerManager.isServer("donut");
        if (flag) {
            Screen screen = closeScreenEvent.getScreen();
            boolean flag2 = screen instanceof InventoryScreen;
            StopWatch stopWatch = this.screenOpenTimer;
            if (flag2) {
                stopWatch.reset();
            }
        }

        MinecraftClient minecraftClient = mc;
        ClientPlayerEntity clientPlayerEntity = minecraftClient.player;
        BooleanSetting booleanSetting = this.bypass;
        if (clientPlayerEntity != null) {
            Object object = booleanSetting.getValue();
            Boolean flag3 = (Boolean)object;
            boolean flag4 = flag3;
            if (flag4) {
                Screen screen2 = closeScreenEvent.getScreen();
                boolean flag5 = screen2 instanceof InventoryScreen;
                if (flag5) {
                    boolean flag6 = this.isBypassSupported();
                    if (flag6) {
                        closeScreenEvent.cancel();
                        return;
                    }

                    boolean flag7 = ServerManager.isServer("funtime");
                    List list = QUEUED_CLICKS;
                    float f;
                    if (flag7) {
                        f = 0.95F;
                    } else {
                        boolean flag8 = ServerManager.isLocalProxy();
                        list = QUEUED_CLICKS;
                        if (!flag8) {
                            boolean flag9 = ServerManager.isServer("funsky");
                            list = QUEUED_CLICKS;
                            if (flag9) {
                                int i = CombatUtil.randomInt(2, 4);
                                list = QUEUED_CLICKS;
                                f = i;
                            } else {
                                f = 2.0F;
                            }
                        } else {
                            f = 0.95F;
                        }
                    }

                    boolean flag10 = list.isEmpty();
                    if (!flag10) {
                        closeScreenEvent.cancel();
                        Runnable runnable = this::restoreSlotsAfterClose;
                        GuiMoveComponent.scheduleAction(runnable, f, f);
                        return;
                    }

                    boolean flag11 = MoveUtil.isMoving();
                    if (flag11) {
                        closeScreenEvent.cancel();
                        return;
                    }
                }
            }
        }
    }

    private void restoreSlotsAfterClose() {
        MinecraftClient minecraftClient = mc;
        ClientPlayerEntity clientPlayerEntity = minecraftClient.player;
        if (clientPlayerEntity != null) {
            MinecraftClient minecraftClient2 = mc;
            ClientPlayerEntity clientPlayerEntity2 = minecraftClient2.player;
            ScreenHandler screenHandler = clientPlayerEntity2.currentScreenHandler;
            List list = this.slotSnapshot;
            int i = list.size();
            boolean flag = QUEUED_CLICKS.isEmpty();
            if (!flag) {
                DefaultedList defaultedList = screenHandler.slots;
                int i2 = defaultedList.size();
                ItemStack itemStack = this.cursorSnapshot;
                if (i == i2) {
                    int i3 = 0;
                    if (0 < i) {
                        do {
                            DefaultedList defaultedList2 = screenHandler.slots;
                            Object object = defaultedList2.get(i3);
                            Slot slot = (Slot)object;
                            Object object2 = list.get(i3);
                            ItemStack itemStack2 = (ItemStack)object2;
                            slot.setStack(itemStack2);
                            itemStack = this.cursorSnapshot;
                            int i4 = i3 + 1;
                            int i5 = i3 & -2 ^ 2132951987;
                            int i6 = i5 * i5;
                            int i7 = (2 - i6) * i6;
                            int i8 = (2 - i7) * i7;
                            int i9 = (2 - i8) * i8;
                            int i10 = (2 - i9) * i9;
                            int i11 = 2 - i10;
                            i3 = i10 * i4 * i11;
                        } while (i3 < i);
                    }

                    screenHandler.setCursorStack(itemStack);
                }
            }

            boolean flag2 = ServerManager.isServer("funsky");
            List list2 = List.copyOf(QUEUED_CLICKS);
            Iterator iterator2 = list2.iterator();

            while (true) {
                boolean flag3 = iterator2.hasNext();
                List list3 = QUEUED_CLICKS;
                if (!flag3) {
                    this.replayingClicks = false;
                    list3.clear();
                    list.clear();
                    ItemStack itemStack3 = ItemStack.EMPTY;
                    this.cursorSnapshot = itemStack3;
                    return;
                }

                Object object3 = iterator2.next();
                GuiWalk.QueuedClick queuedClick = (GuiWalk.QueuedClick)object3;
                Runnable runnable = () -> {
                    if (mc.player != null && mc.interactionManager != null) {
                        this.replayingClicks = true;

                        try {
                            mc.interactionManager.clickSlot(queuedClick.windowId(), queuedClick.slotId(), queuedClick.button(), queuedClick.actionType(), mc.player);
                        } finally {
                            this.replayingClicks = false;
                        }
                    }
                };
                PacketQueue.queueDelayed(runnable);
                if (flag2) {
                    Runnable runnable2 = () -> {};
                    PacketQueue.queueDelayed(runnable2);
                    Runnable runnable3 = () -> {};
                    PacketQueue.queueDelayed(runnable3);
                }
            }
        }
    }

    private boolean isAllowedScreen(Screen screen) {
        boolean flag = screen instanceof ClickGuiScreen;
        BooleanSetting booleanSetting = this.allScreens;
        if (!flag) {
            if (!booleanSetting.getValue()) {
                return !(screen instanceof InventoryScreen) ? !(screen instanceof HandledScreen) : true;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    private boolean hasFocusedTextInput(Screen screen) {
        Iterator iterator2 = screen.children().iterator();
        if (iterator2.hasNext()) {
            do {
                Element element = (Element)iterator2.next();
                if (element instanceof TextFieldWidget && ((TextFieldWidget)element).isActive()) {
                    return true;
                }

                if (element instanceof EditBoxWidget editBoxWidget && editBoxWidget.visible && editBoxWidget.isFocused()) {
                    return true;
                }
            } while (iterator2.hasNext());

            return false;
        } else {
            return false;
        }
    }

    public void clearQueuedClicks() {
        QUEUED_CLICKS.clear();
        this.slotSnapshot.clear();
        ItemStack itemStack = ItemStack.EMPTY;
        this.cursorSnapshot = itemStack;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        QUEUED_CLICKS.clear();
        Deque deque = this.delayedClickPackets;
        this.lastSlot = -1;
        this.groundReady = false;
        this.slowdownApplied = false;
        deque.clear();
        this.pendingClickPacket = null;
        this.sendingDelayedClick = false;
        this.closeStage = 0;
    }

    public BooleanSetting getBypass() {
        return this.bypass;
    }

    public BooleanSetting getAllScreens() {
        return this.allScreens;
    }

    public boolean isReplayingClicks() {
        return this.replayingClicks;
    }

    public int getLastSlot() {
        return this.lastSlot;
    }

    public boolean isGroundReady() {
        return this.groundReady;
    }

    public boolean isSlowdownApplied() {
        return this.slowdownApplied;
    }

    public List<ItemStack> getSlotSnapshot() {
        return this.slotSnapshot;
    }

    public ItemStack getCursorSnapshot() {
        return this.cursorSnapshot;
    }

    public Deque<ClickSlotC2SPacket> getDelayedClickPackets() {
        return this.delayedClickPackets;
    }

    public ClickSlotC2SPacket getPendingClickPacket() {
        return this.pendingClickPacket;
    }

    public boolean isSendingDelayedClick() {
        return this.sendingDelayedClick;
    }

    public int getCloseStage() {
        return this.closeStage;
    }

    public StopWatch getScreenOpenTimer() {
        return this.screenOpenTimer;
    }

    public record QueuedClick(int windowId, int slotId, int button, SlotActionType actionType) {
    }
}
