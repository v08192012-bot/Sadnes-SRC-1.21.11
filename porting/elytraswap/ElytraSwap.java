package tech.sadnes.client.feature.impl.movement;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.ActionSequence;
import tech.sadnes.client.util.ScreenUtil;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.function.Action;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "ElytraSwap",
    description = "feature.impl.misc.elytraswap",
    displayName = "feature.impl.misc.elytraswap",
    category = Category.MOVEMENT,
    subcategory = Subcategory.MOVEMENT_ELYTRA
)
public final class ElytraSwap extends Feature {
    public final KeybindSetting swap = new KeybindSetting("elytraswap.swap");
    public final KeybindSetting use = new KeybindSetting("elytraswap.use");
    private final BooleanSetting autoFly = new BooleanSetting("elytraswap.autoFly");
    private final TextSetting flyCommands = new TextSetting("elytraswap.flyCommands", "fly; speed 10", "elytraswap.flyCommands.hint")
        .setVisibleWhen(this.autoFly::getValue);
    private final BooleanSetting oldSwap = new BooleanSetting("elytraswap.oldSwap", false);
    private final BooleanSetting chestSwap = new BooleanSetting("elytraswap.chestSwap", false);
    private static final ElytraSwap.SwapTiming DEFAULT_TIMING = new ElytraSwap.SwapTiming(2, 2, 2);
    private static final ElytraSwap.SwapTiming FUNTIME_TIMING = new ElytraSwap.SwapTiming(50, 50, 50);
    private static final ElytraSwap.SwapTiming SPOOKYTIME_TIMING = new ElytraSwap.SwapTiming(50, 50, 150);
    private static final ElytraSwap.SwapTiming HOLY_TIMING = new ElytraSwap.SwapTiming(50, 50, 50);
    private ElytraSwap.SwapTiming timing = DEFAULT_TIMING;
    private final ActionSequence actionSequence = new ActionSequence();
    private final StopWatch flyTimer = new StopWatch();
    private boolean flyCommandsPending;

    public static ElytraSwap getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(ElytraSwap.class);
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        boolean flag;
        BooleanSetting booleanSetting;
        ElytraSwap.SwapTiming swapTiming;
        label99: {
            ElytraSwap.SwapTiming swapTiming2;
            label102: {
                ActionSequence actionSequence2 = this.actionSequence;
                actionSequence2.tick();
                boolean flag2 = ServerManager.isServer("funtime");
                ElytraSwap.SwapTiming swapTiming3 = FUNTIME_TIMING;
                flag = this.flyCommandsPending;
                booleanSetting = this.chestSwap;
                if (flag2) {
                    if (!flag) {
                        swapTiming = swapTiming3;
                        break label99;
                    }

                    swapTiming = swapTiming3;
                } else {
                    boolean flag3 = ServerManager.isLocalProxy();
                    swapTiming2 = FUNTIME_TIMING;
                    flag = this.flyCommandsPending;
                    if (!flag3) {
                        boolean flag4 = ServerManager.isServer("holy");
                        swapTiming2 = SPOOKYTIME_TIMING;
                        flag = this.flyCommandsPending;
                        if (!flag4) {
                            boolean flag5 = ServerManager.isServer("spookytime");
                            swapTiming = HOLY_TIMING;
                            flag = this.flyCommandsPending;
                            if (flag5) {
                                if (!flag) {
                                    break label99;
                                }
                            } else {
                                boolean flag6 = ServerManager.isServer("dildo");
                                ElytraSwap.SwapTiming swapTiming4 = HOLY_TIMING;
                                ElytraSwap.SwapTiming swapTiming5 = DEFAULT_TIMING;
                                flag = this.flyCommandsPending;
                                if (flag6) {
                                    if (!flag) {
                                        swapTiming = swapTiming4;
                                        break label99;
                                    }

                                    swapTiming = swapTiming4;
                                } else {
                                    if (!flag) {
                                        swapTiming = swapTiming5;
                                        break label99;
                                    }

                                    swapTiming = swapTiming5;
                                }
                            }
                        } else {
                            if (!flag) {
                                break label102;
                            }

                            swapTiming = swapTiming2;
                        }
                    } else {
                        if (!flag) {
                            break label102;
                        }

                        swapTiming = swapTiming2;
                    }
                }

                this.timing = swapTiming;
                boolean flag7 = actionSequence2.isFinished();
                boolean flag8;
                if (flag7) {
                    ThrowComponent throwComponent = ThrowComponent.getInstance();
                    boolean flag9 = throwComponent.isIdle();
                    flag8 = !flag9;
                } else {
                    flag8 = true;
                }

                MinecraftClient minecraftClient = mc;
                ClientPlayerEntity clientPlayerEntity = minecraftClient.player;
                EquipmentSlot equipmentSlot = EquipmentSlot.CHEST;
                ItemStack itemStack = clientPlayerEntity.getEquippedStack(equipmentSlot);
                Item item = Items.ELYTRA;
                boolean flag10 = itemStack.isOf(item);
                boolean flag11 = this.flyTimer.hasPassed(3000.0);
                ElytraSwap.SwapTiming swapTiming6 = this.timing;
                flag = this.flyCommandsPending;
                if (flag11) {
                    flag = false;
                    swapTiming = swapTiming6;
                } else if (flag10 && !flag8) {
                    this.flyCommandsPending = false;
                    this.runFlyCommands();
                    ElytraSwap.SwapTiming swapTiming7 = this.timing;
                    flag = this.flyCommandsPending;
                    swapTiming = swapTiming7;
                } else {
                    swapTiming = swapTiming6;
                }
                break label99;
            }

            swapTiming = swapTiming2;
        }

        this.timing = swapTiming;
        this.flyCommandsPending = flag;
        Object object = booleanSetting.getValue();
        Boolean flag12 = (Boolean)object;
        boolean flag13 = flag12;
        ElytraSwap.SwapTiming swapTiming8 = this.timing;
        boolean flag14 = this.flyCommandsPending;
        if (flag13) {
            boolean flag15 = ServerManager.isServer("funtime");
            boolean flag16;
            if (flag15) {
                flag16 = false;
            } else {
                boolean flag17 = ServerManager.isLocalProxy();
                if (!flag17) {
                    boolean flag18 = ServerManager.isServer("spookytime");
                    if (!flag18) {
                        boolean flag19 = ServerManager.isServer("dildo");
                        flag16 = !flag19;
                    } else {
                        flag16 = false;
                    }
                } else {
                    flag16 = false;
                }
            }

            boolean flag20 = ServerManager.isInPvp();
            swapTiming8 = this.timing;
            flag14 = this.flyCommandsPending;
            if (flag20) {
                MinecraftClient minecraftClient2 = mc;
                ClientPlayerEntity clientPlayerEntity2 = minecraftClient2.player;
                EquipmentSlot equipmentSlot2 = EquipmentSlot.CHEST;
                ItemStack itemStack2 = clientPlayerEntity2.getEquippedStack(equipmentSlot2);
                Item item2 = itemStack2.getItem();
                Item item3 = Items.ELYTRA;
                if (item2 != item3) {
                    return;
                }

                if (flag16) {
                    return;
                }

                MinecraftClient minecraftClient3 = mc;
                ClientPlayerEntity clientPlayerEntity3 = minecraftClient3.player;
                int i = clientPlayerEntity3.age;
                swapTiming8 = this.timing;
                flag14 = this.flyCommandsPending;
                if (Integer.compareUnsigned(i * -286331153 + 143165576, 286331153) < 0) {
                    this.timing = swapTiming8;
                    this.flyCommandsPending = flag14;
                    this.swapElytra();
                    return;
                }
            }
        }

        this.timing = swapTiming8;
        this.flyCommandsPending = flag14;
    }

    @EventHandler
    public void onMouseClick(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.isPress() && ScreenUtil.allowsMovement(screenMouseClickEvent.getScreen())) {
            int i = screenMouseClickEvent.getButton();
            this.handleKey(i);
        }
    }

    @EventHandler
    public void onScreenKey(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.isPress() && ScreenUtil.allowsMovement(screenKeyEvent.getScreen())) {
            int i = screenKeyEvent.getKey();
            this.handleKey(i);
        }
    }

    private void handleKey(int i) {
        boolean flag = this.swap.matchesKey(i);
        ActionSequence actionSequence2 = this.actionSequence;
        KeybindSetting keybindSetting = this.use;
        if (flag) {
            if (actionSequence2.isFinished()) {
                this.swapElytra();
                return;
            }
        } else if (keybindSetting.matchesKey(i) && mc.player.isGliding()) {
            if (InventoryUtil.findMainInventorySlot(Items.FIREWORK_ROCKET) != null) {
                ThrowComponent throwComponent = ThrowComponent.getInstance();
                Item item = Items.FIREWORK_ROCKET;
                throwComponent.useItem(item);
                return;
            }

            NotificationType notificationType = NotificationType.WARNING;
            String string = this.getDisplayName();
            ItemStack itemStack = Items.FIREWORK_ROCKET.getDefaultStack();
            String string2 = Language.translate("elytraswap.noFirework", "Р¤РµР№РµСЂРІРµСЂРєРѕРІ РЅРµС‚ РІ РёРЅРІРµРЅС‚Р°СЂРµ!");
            NotificationUtil.notify(notificationType, string, itemStack, string2);
            return;
        }
    }

    private void swapElytra() {
        this.flyCommandsPending = false;
        ClientPlayerEntity clientPlayerEntity = mc.player;
        EquipmentSlot equipmentSlot = EquipmentSlot.CHEST;
        ItemStack itemStack = clientPlayerEntity.getEquippedStack(equipmentSlot);
        Slot slot = findChestplateSlot();
        Item item = Items.ELYTRA;
        if (itemStack.isOf(item)) {
            if (slot == null) {
                this.swapViaFreeSlot();
                return;
            }
        } else if (!itemStack.isEmpty() || slot == null) {
            Slot slot2 = findElytraSlot();
            if (slot2 != null) {
                int i = slot2.id;
                this.swapChestSlot(i);
                boolean flag = this.autoFly.getValue();
                StopWatch stopWatch = this.flyTimer;
                if (flag) {
                    this.flyCommandsPending = true;
                    stopWatch.reset();
                    return;
                }

                return;
            }

            NotificationType notificationType = NotificationType.WARNING;
            String string = this.getDisplayName();
            ItemStack itemStack2 = Items.ELYTRA.getDefaultStack();
            String string2 = Language.translate("elytraswap.noElytra", "Р\u00adР»РёС‚СЂС‹ РЅРµС‚ РІ РёРЅРІРµРЅС‚Р°СЂРµ!");
            NotificationUtil.notify(notificationType, string, itemStack2, string2);
            return;
        }

        int i2 = slot.id;
        this.swapChestSlot(i2);
    }

    private void runFlyCommands() {
        String[] strings = this.flyCommands.getValue().split("[;\n]");
        int i = Array.getLength(strings);
        if (i > 0) {
            int i2 = i & 2147483647;
            long l = 0L;

            do {
                int i3 = (int)l;
                String string = ((String)Array.get(strings, i3)).trim();
                if (string.startsWith("/")) {
                    string = string.substring(1);
                }

                if (!string.isEmpty()) {
                    ChatUtil.sendChat(string);
                }
            } while (Integer.toUnsignedLong(i2) != ++l);
        }
    }

    private void swapViaFreeSlot() {
        Slot slot = this.findEmptySlot();
        if (slot != null) {
            boolean flag = GuiWalk.getInstance().isBypassActive();
            ActionSequence actionSequence2 = this.actionSequence;
            if (flag) {
                ThrowComponent throwComponent = ThrowComponent.getInstance();
                Runnable[] runnables = new Runnable[1];
                Runnable runnable = () -> {
                    InventoryUtil.clickSlot(6, 0, SlotActionType.PICKUP);
                    InventoryUtil.clickSlot(slot.id, 0, SlotActionType.PICKUP);
                    this.closeContainer();
                };
                Array.set(runnables, 0, runnable);
                throwComponent.queueActions(runnables);
            } else {
                ActionSequence actionSequence3 = actionSequence2.clear();
                Action action = () -> ThrowComponent.getInstance().setBlockMovement(true);
                ActionSequence actionSequence4 = actionSequence3.addDelayed(0, action);
                int i = this.timing.firstAt();
                Runnable runnable2 = () -> {
                    InventoryUtil.clickSlot(6, 0, SlotActionType.PICKUP);
                    InventoryUtil.clickSlot(slot.id, 0, SlotActionType.PICKUP);
                    this.closeContainer();
                };
                Action action2 = () -> {
                    runnable2.run();
                    ThrowComponent.getInstance().setBlockMovement(false);
                };
                actionSequence4.addDelayed(i, action2);
            }
        } else {
            NotificationType notificationType = NotificationType.WARNING;
            String string = this.getDisplayName();
            ItemStack itemStack = Items.ELYTRA.getDefaultStack();
            String string2 = Language.translate("elytraswap.noSpace", "РќРµС‚ СЃРІРѕР±РѕРґРЅРѕРіРѕ СЃР»РѕС‚Р° РІ РёРЅРІРµРЅС‚Р°СЂРµ!");
            NotificationUtil.notify(notificationType, string, itemStack, string2);
        }
    }

    private Slot findEmptySlot() {
        for (Slot slot : InventoryUtil.getPlayerSlots()) {
            if (slot.getStack().isEmpty()) {
                return slot;
            }
        }

        return null;
    }

    private void swapChestSlot(int i) {
        if (this.oldSwap.getValue()) {
            if (ServerManager.isHolyWorld()) {
                this.swapLegacy(i);
            } else {
                this.swapDirect(i);
            }
        } else {
            int i2 = InventoryUtil.toHotbarIndex(i);
            if (i2 != -1) {
                ThrowComponent.getInstance().useHotbarSlot(i2);
            } else {
                int i3 = InventoryUtil.getSelectedSlot();
                Runnable runnable = () -> PacketSender.sendSequenced(
                    i4 -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, i4, mc.player.getYaw(), mc.player.getPitch())
                );
                if (GuiWalk.getInstance().isBypassActive()) {
                    ThrowComponent.getInstance().queueActions(() -> this.swapWithHotbar(i, i3), runnable, () -> this.swapWithHotbar(i, i3));
                } else {
                    this.actionSequence
                        .clear()
                        .addDelayed(0, () -> ThrowComponent.getInstance().setBlockMovement(true))
                        .addDelayed(this.timing.firstAt(), () -> {
                            InventoryUtil.clickSlot(i, i3, SlotActionType.SWAP);
                            this.closeContainer();
                        })
                        .addDelayed(this.timing.armorAt(), runnable::run)
                        .addDelayed(this.timing.backAt(), () -> {
                            InventoryUtil.clickSlot(i, i3, SlotActionType.SWAP);
                            this.closeContainer();
                            ThrowComponent.getInstance().setBlockMovement(false);
                        });
                }
            }
        }
    }

    private void swapDirect(int i) {
        Runnable runnable = () -> {
            InventoryUtil.clickSlot(i, 0, SlotActionType.PICKUP);
            InventoryUtil.clickSlot(6, 0, SlotActionType.PICKUP);
            InventoryUtil.clickSlot(i, 0, SlotActionType.PICKUP);
            this.closeContainer();
        };
        if (GuiWalk.getInstance().isBypassActive()) {
            ThrowComponent.getInstance().queueActions(runnable);
        } else {
            this.actionSequence
                .clear()
                .addDelayed(0, () -> ThrowComponent.getInstance().setBlockMovement(true))
                .addDelayed(this.timing.firstAt(), runnable::run)
                .addDelayed(this.timing.backAt(), () -> ThrowComponent.getInstance().setBlockMovement(false));
        }
    }

    private void swapLegacy(int i) {
        boolean flag = GuiWalk.getInstance().isBypassActive();
        int i2 = InventoryUtil.toHotbarIndex(i);
        ActionSequence actionSequence2 = this.actionSequence;
        if (i2 == -1) {
            int i3 = InventoryUtil.getSelectedSlot();
            if (flag) {
                ThrowComponent throwComponent = ThrowComponent.getInstance();
                Runnable[] runnables = new Runnable[3];
                Runnable runnable = () -> this.swapWithHotbar(i, i3);
                Array.set(runnables, 0, runnable);
                Runnable runnable2 = () -> this.swapWithHotbar(6, i3);
                Array.set(runnables, 1, runnable2);
                Runnable runnable3 = () -> this.swapWithHotbar(i, i3);
                Array.set(runnables, 2, runnable3);
                throwComponent.queueActions(runnables);
            } else {
                ActionSequence actionSequence3 = actionSequence2.clear();
                Action action = () -> ThrowComponent.getInstance().setBlockMovement(true);
                ActionSequence actionSequence4 = actionSequence3.addDelayed(0, action);
                int i4 = this.timing.firstAt();
                Action action2 = () -> InventoryUtil.clickSlot(i, i3, SlotActionType.SWAP);
                ActionSequence actionSequence5 = actionSequence4.addDelayed(i4, action2);
                int i5 = this.timing.armorAt();
                Action action3 = () -> InventoryUtil.clickSlot(6, i3, SlotActionType.SWAP);
                ActionSequence actionSequence6 = actionSequence5.addDelayed(i5, action3);
                int i6 = this.timing.backAt();
                Action action4 = () -> {
                    InventoryUtil.clickSlot(i, i3, SlotActionType.SWAP);
                    this.closeContainer();
                    ThrowComponent.getInstance().setBlockMovement(false);
                };
                actionSequence6.addDelayed(i6, action4);
            }
        } else if (flag) {
            ThrowComponent throwComponent2 = ThrowComponent.getInstance();
            Runnable[] runnables2 = new Runnable[1];
            Runnable runnable4 = () -> this.swapWithHotbar(6, i2);
            Array.set(runnables2, 0, runnable4);
            throwComponent2.queueActions(runnables2);
        } else {
            ActionSequence actionSequence7 = actionSequence2.clear();
            Action action5 = () -> ThrowComponent.getInstance().setBlockMovement(true);
            ActionSequence actionSequence8 = actionSequence7.addDelayed(0, action5);
            int i7 = this.timing.firstAt();
            Action action6 = () -> {
                InventoryUtil.clickSlot(6, i2, SlotActionType.SWAP);
                this.closeContainer();
            };
            ActionSequence actionSequence9 = actionSequence8.addDelayed(i7, action6);
            int i8 = this.timing.backAt();
            Action action7 = () -> ThrowComponent.getInstance().setBlockMovement(false);
            actionSequence9.addDelayed(i8, action7);
        }
    }

    private void swapWithHotbar(int i, int i2) {
        ThrowComponent.getInstance().swapSlotWithHotbar(i, i2);
        this.closeContainer();
    }

    private void closeContainer() {
        if (mc.currentScreen == null) {
            ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
            CloseHandledScreenC2SPacket closeHandledScreenC2SPacket = new CloseHandledScreenC2SPacket(0);
            clientPlayNetworkHandler.sendPacket(closeHandledScreenC2SPacket);
        }
    }

    public static Slot findChestplateSlot() {
        return Stream.of(
                Items.NETHERITE_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE,
                Items.IRON_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE,
                Items.GOLDEN_CHESTPLATE,
                Items.LEATHER_CHESTPLATE
            )
            .map(InventoryUtil::findMainInventorySlot)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    public static Slot findElytraSlot() {
        return Stream.of(Items.ELYTRA)
            .map(InventoryUtil::findMainInventorySlot)
            .filter(slot2 -> slot2 != null && slot2.getStack().getDamage() < slot2.getStack().getMaxDamage())
            .findFirst()
            .orElse(null);
    }

    record SwapTiming(int first, int armor, int back) {
        int firstAt() {
            return this.first;
        }

        int armorAt() {
            int i = this.first;
            return this.armor + i;
        }

        int backAt() {
            int i = this.first;
            int i2 = this.armor;
            int i3 = this.back;
            return i2 + i + i3;
        }
    }
}
