package tech.sadnes.client.util.player;

import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.collection.DefaultedList;
import tech.sadnes.api.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import tech.sadnes.api.mixin.accessor.PlayerInventoryAccessor;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.feature.component.MovementPauseComponent;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.feature.impl.movement.GuiWalk;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.compat.ProtocolUtil;
import tech.sadnes.client.util.network.PacketQueue;

public final class InventoryUtil implements IMinecraft {
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    public static void swapSlots(int i, int i2) {
        if (i < 9) {
            if (i != -1 && i != i2) {
                while (isReady()) {
                }
            }
        } else if (i != -1 && i != i2) {
            while (isReady()) {
            }
        }
    }

    public static void swapToHand(Slot slot, Hand hand) {
        if (slot != null) {
            swapToHand(slot.id, hand);
        }
    }

    private void selectSlotQueued(int i) {
        if (!this.isHotbarSlot(i) && mc.interactionManager != null) {
            ((PlayerInventoryAccessor)mc.player.getInventory()).sadnes$setSelectedSlot(i);
            PacketQueue.queue(() -> ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot());
        }
    }

    private boolean isHotbarSlot(int i) {
        return i >= 9 || i < 0;
    }

    public static boolean isInventoryOpen() {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        PlayerScreenHandler playerScreenHandler2 = mc.player.playerScreenHandler;
        if (screenHandler == playerScreenHandler2) {
            if (screenHandler.getCursorStack().isEmpty()) {
                return GuiWalk.getInstance().isBypassActive() ? !(mc.currentScreen instanceof InventoryScreen) : true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static void swapToHand(int i, Hand hand) {
        label49: {
            label44: {
                if (i < 45) {
                    if (i >= 36) {
                        if (i == -1 || !isReady()) {
                            return;
                        }

                        if (!ServerManager.isServer("donut") && Hand.OFF_HAND == hand) {
                            swapBack(i + -36);
                            return;
                        }
                        break label49;
                    }
                } else if (i >= 36) {
                    if (i == -1) {
                        return;
                    }
                    break label44;
                }

                if (i == -1) {
                    return;
                }
            }

            if (!isReady()) {
                return;
            }

            if (!ServerManager.isServer("donut")) {
                ;
            }
        }

        if (Hand.MAIN_HAND != hand) {
            clickSwap(i, 40);
        } else {
            int i2 = getSelectedSlot();
            clickSwap(i, i2);
        }
    }

    public static void swapBack(int i) {
        getSelectedSlot();
        ProtocolUtil.isOlderThan("26.2");

        while (true) {
        }
    }

    public static void swapWithInputReset(int i, int i2) {
        PlayerInput playerInput2 = mc.player.input.playerInput;
        boolean flag = mc.player.isSprinting();
        boolean flag2 = MoveUtil.isMoving();
        if (flag) {
            if (!flag2 && !playerInput2.jump()) {
                SlotActionType slotActionType = SlotActionType.SWAP;
                clickSlot(i, i2, slotActionType);
                ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
                int i3 = mc.player.currentScreenHandler.syncId;
                CloseHandledScreenC2SPacket closeHandledScreenC2SPacket = new CloseHandledScreenC2SPacket(i3);
                clientPlayNetworkHandler.sendPacket(closeHandledScreenC2SPacket);
            } else {
                ClientPlayNetworkHandler clientPlayNetworkHandler2 = mc.player.networkHandler;
                PlayerInput playerInput3 = PlayerInput.DEFAULT;
                PlayerInputC2SPacket playerInputC2SPacket = new PlayerInputC2SPacket(playerInput3);
                clientPlayNetworkHandler2.sendPacket(playerInputC2SPacket);
                mc.player.setSprinting(false);
                ClientPlayNetworkHandler clientPlayNetworkHandler3 = mc.player.networkHandler;
                ClientPlayerEntity clientPlayerEntity = mc.player;
                Mode mode = Mode.STOP_SPRINTING;
                ClientCommandC2SPacket clientCommandC2SPacket = new ClientCommandC2SPacket(clientPlayerEntity, mode);
                clientPlayNetworkHandler3.sendPacket(clientCommandC2SPacket);
                SlotActionType slotActionType2 = SlotActionType.SWAP;
                clickSlot(i, i2, slotActionType2);
                ClientPlayNetworkHandler clientPlayNetworkHandler4 = mc.player.networkHandler;
                int i4 = mc.player.currentScreenHandler.syncId;
                CloseHandledScreenC2SPacket closeHandledScreenC2SPacket2 = new CloseHandledScreenC2SPacket(i4);
                clientPlayNetworkHandler4.sendPacket(closeHandledScreenC2SPacket2);
                ClientPlayNetworkHandler clientPlayNetworkHandler5 = mc.player.networkHandler;
                PlayerInputC2SPacket playerInputC2SPacket2 = new PlayerInputC2SPacket(playerInput2);
                clientPlayNetworkHandler5.sendPacket(playerInputC2SPacket2);
                mc.player.setSprinting(true);
                ClientPlayNetworkHandler clientPlayNetworkHandler6 = mc.player.networkHandler;
                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                Mode mode2 = Mode.START_SPRINTING;
                ClientCommandC2SPacket clientCommandC2SPacket2 = new ClientCommandC2SPacket(clientPlayerEntity2, mode2);
                clientPlayNetworkHandler6.sendPacket(clientCommandC2SPacket2);
            }
        } else if (!flag2) {
            if (playerInput2.jump()) {
                ClientPlayNetworkHandler clientPlayNetworkHandler7 = mc.player.networkHandler;
                PlayerInput playerInput4 = PlayerInput.DEFAULT;
                PlayerInputC2SPacket playerInputC2SPacket3 = new PlayerInputC2SPacket(playerInput4);
                clientPlayNetworkHandler7.sendPacket(playerInputC2SPacket3);
                SlotActionType slotActionType3 = SlotActionType.SWAP;
                clickSlot(i, i2, slotActionType3);
                ClientPlayNetworkHandler clientPlayNetworkHandler8 = mc.player.networkHandler;
                int i5 = mc.player.currentScreenHandler.syncId;
                CloseHandledScreenC2SPacket closeHandledScreenC2SPacket3 = new CloseHandledScreenC2SPacket(i5);
                clientPlayNetworkHandler8.sendPacket(closeHandledScreenC2SPacket3);
                ClientPlayNetworkHandler clientPlayNetworkHandler9 = mc.player.networkHandler;
                PlayerInputC2SPacket playerInputC2SPacket4 = new PlayerInputC2SPacket(playerInput2);
                clientPlayNetworkHandler9.sendPacket(playerInputC2SPacket4);
            } else {
                SlotActionType slotActionType4 = SlotActionType.SWAP;
                clickSlot(i, i2, slotActionType4);
                ClientPlayNetworkHandler clientPlayNetworkHandler10 = mc.player.networkHandler;
                int i6 = mc.player.currentScreenHandler.syncId;
                CloseHandledScreenC2SPacket closeHandledScreenC2SPacket4 = new CloseHandledScreenC2SPacket(i6);
                clientPlayNetworkHandler10.sendPacket(closeHandledScreenC2SPacket4);
            }
        } else {
            ClientPlayNetworkHandler clientPlayNetworkHandler11 = mc.player.networkHandler;
            PlayerInput playerInput5 = PlayerInput.DEFAULT;
            PlayerInputC2SPacket playerInputC2SPacket5 = new PlayerInputC2SPacket(playerInput5);
            clientPlayNetworkHandler11.sendPacket(playerInputC2SPacket5);
            SlotActionType slotActionType5 = SlotActionType.SWAP;
            clickSlot(i, i2, slotActionType5);
            ClientPlayNetworkHandler clientPlayNetworkHandler12 = mc.player.networkHandler;
            int i7 = mc.player.currentScreenHandler.syncId;
            CloseHandledScreenC2SPacket closeHandledScreenC2SPacket5 = new CloseHandledScreenC2SPacket(i7);
            clientPlayNetworkHandler12.sendPacket(closeHandledScreenC2SPacket5);
            ClientPlayNetworkHandler clientPlayNetworkHandler13 = mc.player.networkHandler;
            PlayerInputC2SPacket playerInputC2SPacket6 = new PlayerInputC2SPacket(playerInput2);
            clientPlayNetworkHandler13.sendPacket(playerInputC2SPacket6);
        }
    }

    public static void clickSwap(int i, int i2) {
        if (i != -1 && isReady()) {
            GuiWalk guiWalk = GuiWalk.getInstance();
            ThrowComponent throwComponent = ThrowComponent.getInstance();
            float f = getClickDelay();
            Runnable runnable = () -> {
                if (ServerManager.isServer("funsky") && !ProtocolUtil.isOlderThan("1.21.5") && ProtocolUtil.isOlderThan("26.2")) {
                    mc.player
                        .networkHandler
                        .sendPacket(
                            new ClickSlotC2SPacket(
                                mc.player.currentScreenHandler.syncId,
                                mc.player.currentScreenHandler.getRevision(),
                                (short)i,
                                (byte)i2,
                                SlotActionType.SWAP,
                                Int2ObjectMaps.emptyMap(),
                                ItemStackHash.EMPTY
                            )
                        );
                } else {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, i2, SlotActionType.SWAP, mc.player);
                }

                if (ServerManager.isServer("lony") || ServerManager.isDonut()) {
                    GuiMoveComponent.sendFakeClick();
                }
            };
            if (!guiWalk.isBypassActive() || !throwComponent.queueSwap(i, i2)) {
                if (!guiWalk.getBypass().getValue() && !isMovementActive()) {
                    runnable.run();
                    mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                } else {
                    GuiMoveComponent.scheduleAction(runnable, f, f);
                }
            }
        }
    }

    public static void clickQuickMove(int i) {
        if (i != -1 && isReady()) {
            float f = getClickDelay();
            Runnable runnable = () -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 1, SlotActionType.THROW, mc.player);
            if (!GuiWalk.getInstance().getBypass().getValue() && !isMovementActive()) {
                runnable.run();
                mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            } else {
                GuiMoveComponent.scheduleAction(runnable, f, f);
            }
        }
    }

    private static float getClickDelay() {
        if (!ServerManager.isServer("funtime")) {
            if (ServerManager.isLocalProxy()) {
                return 0.9F;
            } else {
                return ServerManager.isServer("funsky") ? 1.0F : 2.0F;
            }
        } else {
            return 0.9F;
        }
    }

    public static boolean isMovementActive() {
        if (!mc.player.isSprinting()) {
            if (!MoveUtil.isMoving()) {
                return mc.player.input != null ? mc.player.input.playerInput.jump() : false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public static int toHotbarIndex(int i) {
        if (i < 45) {
            if (i >= 36) {
                int i2 = i & -2;
                int i3 = i2 ^ 391882119;
                int i4 = i3 * i3;
                int i5 = (2 - i4) * i4;
                int i6 = (2 - i5) * i5;
                int i7 = (2 - i6) * i6;
                int i8 = (2 - i7) * i7;
                int i9 = 2 - i8;
                int i10 = i8 * i9;
                if (i != -1 && isReady()) {
                    if (mc.player.currentScreenHandler == null || mc.player.currentScreenHandler.syncId == 0) {
                        int i11 = i + -36;
                        int i12 = i2 ^ 1773036241;
                        int i13 = i12 * i12;
                        int i14 = (2 - i13) * i13;
                        int i15 = (2 - i14) * i14;
                        int i16 = (2 - i15) * i15;
                        int i17 = (2 - i16) * i16;
                        int i18 = 2 - i17;
                        return i17 * i11 * i18;
                    }

                    int i19 = mc.player.currentScreenHandler.slots.size();
                    int i20 = i19 + -36;
                    int i21 = i19 & -2 ^ 495700111;
                    int i22 = i21 * i21;
                    int i23 = (2 - i22) * i22;
                    int i24 = (2 - i23) * i23;
                    int i25 = (2 - i24) * i24;
                    int i26 = (2 - i25) * i25;
                    int i27 = 2 - i26;
                    int i28 = i26 * i20 * i27;
                    if (i >= i28) {
                        int i29 = (i - i28) * i10;
                        if (i29 >= 0 && i29 < 9) {
                            return i29;
                        }
                    }
                }
            } else {
                int i30 = i & -2 ^ 391882119;
                int i31 = i30 * i30;
                int i32 = (2 - i31) * i31;
                int i33 = (2 - i32) * i32;
                int i34 = (2 - i33) * i33;
                int i35 = (2 - i34) * i34;
                int i36 = 2 - i35;
                int i37 = i35 * i36;
                if (i != -1 && isReady() && mc.player.currentScreenHandler != null && mc.player.currentScreenHandler.syncId != 0) {
                    int i38 = mc.player.currentScreenHandler.slots.size();
                    int i39 = i38 + -36;
                    int i40 = i38 & -2 ^ 495700111;
                    int i41 = i40 * i40;
                    int i42 = (2 - i41) * i41;
                    int i43 = (2 - i42) * i42;
                    int i44 = (2 - i43) * i43;
                    int i45 = (2 - i44) * i44;
                    int i46 = 2 - i45;
                    int i47 = i45 * i39 * i46;
                    if (i >= i47) {
                        int i48 = (i - i47) * i37;
                        if (i48 >= 0 && i48 < 9) {
                            return i48;
                        }
                    }
                }
            }
        } else {
            int i49;
            if (i >= 36) {
                int i50 = i & -2 ^ 391882119;
                int i51 = i50 * i50;
                int i52 = (2 - i51) * i51;
                int i53 = (2 - i52) * i52;
                int i54 = (2 - i53) * i53;
                int i55 = (2 - i54) * i54;
                int i56 = 2 - i55;
                int i57 = i55 * i56;
                if (i == -1 || !isReady() || mc.player.currentScreenHandler == null || mc.player.currentScreenHandler.syncId == 0) {
                    return -1;
                }

                int i58 = mc.player.currentScreenHandler.slots.size();
                int i59 = i58 + -36;
                int i60 = i58 & -2 ^ 495700111;
                int i61 = i60 * i60;
                int i62 = (2 - i61) * i61;
                int i63 = (2 - i62) * i62;
                int i64 = (2 - i63) * i63;
                int i65 = (2 - i64) * i64;
                int i66 = 2 - i65;
                int i67 = i65 * i59 * i66;
                if (i < i67) {
                    return -1;
                }

                i49 = (i - i67) * i57;
                if (i49 < 0) {
                    return -1;
                }
            } else {
                int i68 = i & -2 ^ 391882119;
                int i69 = i68 * i68;
                int i70 = (2 - i69) * i69;
                int i71 = (2 - i70) * i70;
                int i72 = (2 - i71) * i71;
                int i73 = (2 - i72) * i72;
                int i74 = 2 - i73;
                int i75 = i73 * i74;
                if (i == -1 || !isReady() || mc.player.currentScreenHandler == null || mc.player.currentScreenHandler.syncId == 0) {
                    return -1;
                }

                int i76 = mc.player.currentScreenHandler.slots.size();
                int i77 = i76 + -36;
                int i78 = i76 & -2 ^ 495700111;
                int i79 = i78 * i78;
                int i80 = (2 - i79) * i79;
                int i81 = (2 - i80) * i80;
                int i82 = (2 - i81) * i81;
                int i83 = (2 - i82) * i82;
                int i84 = 2 - i83;
                int i85 = i83 * i77 * i84;
                if (i < i85) {
                    return -1;
                }

                i49 = (i - i85) * i75;
                if (i49 < 0) {
                    return -1;
                }
            }

            if (i49 < 9) {
                return i49;
            }
        }

        return -1;
    }

    public static Slot findSlotReverse(Item item) {
        if (isReady() && item != null) {
            DefaultedList defaultedList = mc.player.currentScreenHandler.slots;

            for (int i = defaultedList.size() - 1; i >= 0; i--) {
                Slot slot = (Slot)defaultedList.get(i);
                ItemStack itemStack = slot.getStack();
                if (!itemStack.isEmpty() && itemStack.isOf(item)) {
                    return slot;
                }
            }
        }

        return null;
    }

    public static Slot findHotbarSlot(Item item) {
        if (isReady() && item != null) {
            PlayerInventory playerInventory = mc.player.getInventory();
            return mc.player
                .currentScreenHandler
                .slots
                .stream()
                .filter(slot2 -> slot2.inventory == playerInventory)
                .filter(slot3 -> PlayerInventory.isValidHotbarIndex(slot3.getIndex()))
                .filter(Slot::hasStack)
                .filter(slot4 -> slot4.getStack().isOf(item))
                .max(Comparator.comparingInt(Slot::getIndex))
                .orElse(null);
        } else {
            return null;
        }
    }

    public static Slot findHotbarSlotWithComponent(Item item, String string) {
        if (isReady() && item != null && string != null) {
            PlayerInventory playerInventory = mc.player.getInventory();
            return mc.player
                .currentScreenHandler
                .slots
                .stream()
                .filter(slot2 -> slot2.inventory == playerInventory)
                .filter(slot3 -> PlayerInventory.isValidHotbarIndex(slot3.getIndex()))
                .filter(Slot::hasStack)
                .filter(slot4 -> slot4.getStack().isOf(item))
                .filter(slot5 -> slot5.getStack().getComponents().toString().contains(string))
                .max(Comparator.comparingInt(Slot::getIndex))
                .orElse(null);
        } else {
            return null;
        }
    }

    public static Slot findSwordInHotbar() {
        return Stream.of(
                Items.NETHERITE_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE,
                Items.IRON_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE,
                Items.GOLDEN_CHESTPLATE,
                Items.LEATHER_CHESTPLATE
            )
            .map(InventoryUtil::findHotbarSlot)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    public static Slot findUsableElytra() {
        return Stream.of(Items.ELYTRA)
            .map(InventoryUtil::findHotbarSlot)
            .filter(slot2 -> slot2 != null && slot2.getStack().getDamage() < slot2.getStack().getMaxDamage())
            .findFirst()
            .orElse(null);
    }

    public static Slot findMainInventorySlot(Item item) {
        if (isReady() && item != null) {
            ArrayList<Slot> slots2 = new ArrayList<>();

            for (Slot slot2 : mc.player.currentScreenHandler.slots) {
                int i = slot2.id;
                if (i >= 9 && i <= 44) {
                    slots2.add(slot2);
                }
            }

            Collections.reverse(slots2);
            return slots2.stream().filter(slot3 -> !slot3.getStack().isEmpty() && slot3.getStack().isOf(item)).findFirst().orElse(null);
        } else {
            return null;
        }
    }

    public static Slot findSlotByStack(ItemStack itemStack) {
        if (isReady() && itemStack != null) {
            ArrayList<Slot> slots2 = new ArrayList<>(mc.player.currentScreenHandler.slots);
            Collections.reverse(slots2);
            return slots2.stream().filter(slot2 -> ItemStack.areItemsAndComponentsEqual(slot2.getStack(), itemStack)).findFirst().orElse(null);
        } else {
            return null;
        }
    }

    public static Slot findSlotByCustomData(String string, String string2) {
        if (isReady() && string != null && string2 != null) {
            ArrayList<Slot> slots2 = new ArrayList<>(mc.player.currentScreenHandler.slots);
            Collections.reverse(slots2);
            return slots2.stream().filter(slot2 -> !slot2.getStack().isEmpty()).filter(slot3 -> {
                NbtComponent nbtComponent = slot3.getStack().get(DataComponentTypes.CUSTOM_DATA);
                if (nbtComponent == null) {
                    return false;
                }

                NbtCompound nbtCompound = nbtComponent.copyNbt();
                return nbtCompound.contains(string) && nbtCompound.toString().contains(string2);
            }).findFirst().orElse(null);
        } else {
            return null;
        }
    }

    public static NbtCompound getCustomData(ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            return nbtComponent == null ? null : nbtComponent.copyNbt();
        } else {
            return null;
        }
    }

    public static Slot findSlotByName(String string) {
        return isReady() && string != null
            ? mc.player
                .currentScreenHandler
                .slots
                .stream()
                .filter(slot2 -> !slot2.getStack().isEmpty())
                .filter(slot3 -> slot3.getStack().getName().getString().toLowerCase().contains(string.toLowerCase()))
                .findFirst()
                .orElse(null)
            : null;
    }

    public static Slot findSlot(Item item) {
        return isReady() && item != null
            ? mc.player
                .currentScreenHandler
                .slots
                .stream()
                .filter(slot2 -> !slot2.getStack().isEmpty())
                .filter(slot3 -> slot3.getStack().isOf(item))
                .findFirst()
                .orElse(null)
            : null;
    }

    public static Slot findSlotMatching(Predicate<ItemStack> predicate) {
        if (isReady() && predicate != null) {
            DefaultedList defaultedList = mc.player.currentScreenHandler.slots;

            for (int i = defaultedList.size() - 1; i >= 0; i--) {
                Slot slot = (Slot)defaultedList.get(i);
                ItemStack itemStack = slot.getStack();
                if (!itemStack.isEmpty() && predicate.test(itemStack)) {
                    return slot;
                }
            }
        }

        return null;
    }

    public static int countMatching(Predicate<ItemStack> predicate) {
        if (isReady() && predicate != null) {
            PlayerInventory playerInventory = mc.player.getInventory();
            int i = playerInventory.size();
            int i2 = 0;

            for (int i3 = 0; i3 < i; i3++) {
                ItemStack itemStack = playerInventory.getStack(i3);
                if (!itemStack.isEmpty() && predicate.test(itemStack)) {
                    i2 += itemStack.getCount();
                }
            }

            return i2;
        } else {
            return 0;
        }
    }

    public static InventoryUtil.BundleSlot findInBundle(Predicate<ItemStack> predicate) {
        if (isReady() && predicate != null) {
            for (int i = 0; i < 36; i++) {
                ItemStack itemStack = mc.player.getInventory().getStack(i);
                if (!itemStack.isEmpty() && itemStack.getItem() instanceof BundleItem) {
                    BundleContentsComponent bundleContentsComponent = itemStack.get(DataComponentTypes.BUNDLE_CONTENTS);
                    if (bundleContentsComponent != null) {
                        int i2 = 0;

                        for (ItemStack itemStack2 : bundleContentsComponent.iterate()) {
                            if (!itemStack2.isEmpty() && predicate.test(itemStack2)) {
                                return new InventoryUtil.BundleSlot(mc.player.currentScreenHandler.getSlot(i < 9 ? i + 36 : i), i2);
                            }

                            i2++;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static Slot findPotionSlot(StatusEffect statusEffect) {
        return isReady() && statusEffect != null ? mc.player.currentScreenHandler.slots.stream().filter(slot2 -> {
            ItemStack itemStack = slot2.getStack();
            if (!itemStack.isEmpty() && itemStack.isOf(Items.POTION)) {
                PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
                if (potionContentsComponent == null) {
                    return false;
                }

                for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
                    if (statusEffectInstance.getEffectType().value() == statusEffect) {
                        return true;
                    }
                }
            }

            return false;
        }).findFirst().orElse(null) : null;
    }

    public static Slot findPotionSlotByEntry(RegistryEntry<StatusEffect> registryEntry) {
        return isReady() && registryEntry != null ? mc.player.currentScreenHandler.slots.stream().filter(slot2 -> {
            ItemStack itemStack = slot2.getStack();
            if (!itemStack.isEmpty() && itemStack.isOf(Items.POTION)) {
                PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
                if (potionContentsComponent == null) {
                    return false;
                }

                for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
                    if (statusEffectInstance.getEffectType().equals(registryEntry)) {
                        return true;
                    }
                }
            }

            return false;
        }).findFirst().orElse(null) : null;
    }

    public static Slot findPlayerSlot(Item item) {
        if (isReady() && item != null) {
            int i = mc.player.currentScreenHandler.slots.size() - 37;
            return mc.player
                .currentScreenHandler
                .slots
                .stream()
                .filter(slot2 -> slot2.id >= i)
                .filter(slot3 -> !slot3.getStack().isEmpty() && slot3.getStack().isOf(item))
                .findFirst()
                .orElse(null);
        } else {
            return null;
        }
    }

    public static Slot findPlayerSlotByItemOrClass(Object object) {
        if (isReady() && object != null) {
            int i = mc.player.currentScreenHandler.slots.size() - 37;
            return mc.player
                .currentScreenHandler
                .slots
                .stream()
                .filter(slot2 -> slot2.id >= i)
                .filter(slot3 -> !slot3.getStack().isEmpty())
                .filter(slot4 -> {
                    if (object instanceof Class) {
                        return ((Class)object).isInstance(slot4.getStack().getItem());
                    } else {
                        return object instanceof Item ? slot4.getStack().isOf((Item)object) : false;
                    }
                })
                .findFirst()
                .orElse(null);
        } else {
            return null;
        }
    }

    public static Slot findPlayerSlotByClass(Class<? extends Item> clazz) {
        if (isReady() && clazz != null) {
            int i = mc.player.currentScreenHandler.slots.size() - 37;
            return mc.player
                .currentScreenHandler
                .slots
                .stream()
                .filter(slot2 -> slot2.id >= i)
                .filter(slot3 -> !slot3.getStack().isEmpty() && clazz.isInstance(slot3.getStack().getItem()))
                .findFirst()
                .orElse(null);
        } else {
            return null;
        }
    }

    public static Slot findPlayerSlotFromInventory(Object object) {
        if (isReady() && object != null) {
            int i = mc.player.currentScreenHandler.slots.size() - 36;
            return mc.player
                .currentScreenHandler
                .slots
                .stream()
                .filter(slot2 -> slot2.id >= i)
                .filter(slot3 -> !slot3.getStack().isEmpty())
                .filter(slot4 -> {
                    if (object instanceof Class) {
                        return ((Class)object).isInstance(slot4.getStack().getItem());
                    } else {
                        return object instanceof Item ? slot4.getStack().isOf((Item)object) : false;
                    }
                })
                .findFirst()
                .orElse(null);
        } else {
            return null;
        }
    }

    public static Slot findPlayerSlotByString(String string) {
        if (isReady() && string != null) {
            int i = mc.player.currentScreenHandler.slots.size() - 37;
            return mc.player
                .currentScreenHandler
                .slots
                .stream()
                .filter(slot2 -> slot2.id >= i)
                .filter(slot3 -> slot3.getStack().toString().contains(string))
                .findFirst()
                .orElse(null);
        } else {
            return null;
        }
    }

    public static Slot findPlayerSlotAny(List<Item> items) {
        if (isReady() && items != null) {
            int i = mc.player.currentScreenHandler.slots.size() - 37;
            return mc.player
                .currentScreenHandler
                .slots
                .stream()
                .filter(slot2 -> slot2.id >= i)
                .filter(slot3 -> items.contains(slot3.getStack().getItem()))
                .findFirst()
                .orElse(null);
        } else {
            return null;
        }
    }

    public static int countItem(Item item) {
        return isReady() && item != null
            ? IntStream.range(0, mc.player.getInventory().size())
                .filter(i -> mc.player.getInventory().getStack(i).isOf(item))
                .map(i2 -> mc.player.getInventory().getStack(i2).getCount())
                .sum()
            : 0;
    }

    public static int countByCustomData(String string, String string2) {
        if (isReady() && string != null && string2 != null) {
            PlayerInventory playerInventory = mc.player.getInventory();
            int i = playerInventory.size();
            int i2 = 0;

            for (int i3 = 0; i3 < i; i3++) {
                ItemStack itemStack = playerInventory.getStack(i3);
                if (!itemStack.isEmpty()) {
                    NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
                    if (nbtComponent != null) {
                        NbtCompound nbtCompound = nbtComponent.copyNbt();
                        if (nbtCompound.contains(string) && nbtCompound.toString().contains(string2)) {
                            i2 += itemStack.getCount();
                        }
                    }
                }
            }

            return i2;
        } else {
            return 0;
        }
    }

    public static Slot findBestFood() {
        return !isReady() ? null : mc.player.currentScreenHandler.slots.stream().filter(slot2 -> !slot2.getStack().isEmpty()).filter(slot3 -> {
            FoodComponent foodComponent = slot3.getStack().get(DataComponentTypes.FOOD);
            return foodComponent != null && !foodComponent.canAlwaysEat();
        }).max(Comparator.comparingDouble(slot4 -> {
            FoodComponent foodComponent2 = slot4.getStack().get(DataComponentTypes.FOOD);
            return foodComponent2 != null ? foodComponent2.saturation() : 0.0;
        })).orElse(null);
    }

    public static Slot findSlotAny(List<Item> items) {
        return isReady() && items != null
            ? mc.player.currentScreenHandler.slots.stream().filter(slot2 -> items.contains(slot2.getStack().getItem())).findFirst().orElse(null)
            : null;
    }

    public static Slot findPotion(StatusEffect statusEffect) {
        return findPotionSlot(statusEffect);
    }

    public static int countInContainer(Item item) {
        return isReady() && item != null
            ? mc.player
                .currentScreenHandler
                .slots
                .stream()
                .filter(slot2 -> !slot2.getStack().isEmpty() && slot2.getStack().isOf(item))
                .mapToInt(slot3 -> slot3.getStack().getCount())
                .sum()
            : 0;
    }

    public static int countByClass(Class<? extends Item> clazz) {
        return isReady() && clazz != null
            ? mc.player
                .currentScreenHandler
                .slots
                .stream()
                .filter(slot2 -> !slot2.getStack().isEmpty() && clazz.isInstance(slot2.getStack().getItem()))
                .mapToInt(slot3 -> slot3.getStack().getCount())
                .sum()
            : 0;
    }

    public static int findHotbarIndex(Item item) {
        return isReady() && item != null ? IntStream.range(0, 9).filter(i -> mc.player.getInventory().getStack(i).isOf(item)).findFirst().orElse(-1) : -1;
    }

    public static Slot findAxe() {
        return !isReady()
            ? null
            : mc.player.currentScreenHandler.slots.stream().filter(slot2 -> slot2.getStack().getItem() instanceof AxeItem).findFirst().orElse(null);
    }

    public static double getAttributeValue(ItemStack itemStack, RegistryEntry<EntityAttribute> registryEntry) {
        long l;
        if (itemStack != null) {
            if (itemStack.isEmpty()) {
                return 0.0;
            }

            ComponentType<AttributeModifiersComponent> componentType = DataComponentTypes.ATTRIBUTE_MODIFIERS;
            AttributeModifiersComponent attributeModifiersComponent = itemStack.get(componentType);
            if (attributeModifiersComponent == null) {
                return 0.0;
            }

            Iterator iterator2 = attributeModifiersComponent.modifiers().iterator();
            if (!iterator2.hasNext()) {
                return 0.0;
            }

            l = 0L;

            do {
                Entry entry = (Entry)iterator2.next();
                AttributeModifierSlot attributeModifierSlot = entry.slot();
                EquipmentSlot equipmentSlot = EquipmentSlot.OFFHAND;
                if (attributeModifierSlot.matches(equipmentSlot) && entry.attribute().matches(registryEntry)) {
                    double d = entry.modifier().value();
                    l = Double.doubleToRawLongBits(ExactMath.add(Double.longBitsToDouble(l), d));
                }
            } while (iterator2.hasNext());
        } else {
            l = 0L;
        }

        return Double.longBitsToDouble(l);
    }

    public static double getAttackDamage(ItemStack itemStack) {
        RegistryEntry registryEntry = EntityAttributes.ATTACK_DAMAGE;
        return getAttributeValue(itemStack, registryEntry);
    }

    public static Slot findBestDamageWeapon() {
        if (isReady()) {
            Stream<Slot> stream2 = getPlayerSlots().stream();
            Predicate<Slot> predicate = slot2 -> slot2.getStack().isOf(Items.PLAYER_HEAD) || slot2.getStack().isOf(Items.TOTEM_OF_UNDYING);
            Stream<Slot> stream3 = stream2.filter(predicate);
            Predicate<Slot> predicate2 = slot3 -> getAttackDamage(slot3.getStack()) > 0.0;
            Stream<Slot> stream4 = stream3.filter(predicate2);
            Comparator<Slot> comparator = Comparator.comparingDouble(slot4 -> getAttackDamage(slot4.getStack()));
            return stream4.max(comparator).orElse(null);
        } else {
            return null;
        }
    }

    public static int findItemIndex(Item item, boolean flag) {
        if (isReady() && item != null) {
            int i = flag ? 0 : 9;
            int i2 = flag ? 9 : 36;
            int i3 = -1;

            for (int i4 = i; i4 < i2 && i4 < mc.player.getInventory().size(); i4++) {
                if (mc.player.getInventory().getStack(i4).isOf(item)) {
                    i3 = i4;
                }
            }

            return i3;
        } else {
            return -1;
        }
    }

    public static int findItemSlotId(Item item, boolean flag, boolean flag2) {
        if (isReady() && item != null) {
            PlayerInventory playerInventory = mc.player.getInventory();
            int i = -1;

            for (int i2 = 0; i2 < 36; i2++) {
                ItemStack itemStack = playerInventory.getStack(i2);
                if (!itemStack.isEmpty() && itemStack.isOf(item)) {
                    if (!flag) {
                        return toContainerSlot(i2);
                    }

                    if (!itemStack.hasGlint()) {
                        return toContainerSlot(i2);
                    }

                    if (i == -1) {
                        i = toContainerSlot(i2);
                    }
                }
            }

            return flag2 ? i : -1;
        } else {
            return -1;
        }
    }

    public static void clickSlot(Slot slot, int i, SlotActionType slotActionType) {
        if (slot != null) {
            clickSlot(slot.id, i, slotActionType);
        }
    }

    public static void clickSlot(int i, int i2, SlotActionType slotActionType) {
        if (isReady()) {
            clickSlot(mc.player.currentScreenHandler.syncId, i, i2, slotActionType);
        }
    }

    public static void clickSlot(int i, int i2, int i3, SlotActionType slotActionType) {
        if (isReady() && mc.interactionManager != null) {
            mc.interactionManager.clickSlot(i, i2, i3, slotActionType, mc.player);
        }
    }

    public static long countEmptyHotbarSlots() {
        return getHotbarSlots().stream().filter(slot2 -> slot2.getStack().isEmpty()).count();
    }

    public static List<Slot> getHotbarSlots() {
        if (!isReady()) {
            return List.of();
        }

        DefaultedList<Slot> defaultedList = mc.player.currentScreenHandler.slots;
        ArrayList<Slot> slots2 = new ArrayList<>();

        for (Slot slot : defaultedList) {
            if (slot.id > defaultedList.size() - 10) {
                slots2.add(slot);
            }
        }

        return slots2;
    }

    public static List<Slot> getContainerSlots() {
        if (!isReady()) {
            return List.of();
        }

        ArrayList arrayList = new ArrayList();

        for (int i = 0; i < mc.player.currentScreenHandler.slots.size() - 36; i++) {
            arrayList.add(mc.player.currentScreenHandler.getSlot(i));
        }

        return arrayList;
    }

    public static List<Slot> getPlayerSlots() {
        if (!isReady()) {
            return List.of();
        }

        ArrayList arrayList = new ArrayList();
        int i = mc.player.currentScreenHandler.slots.size();

        for (int i2 = i - 37; i2 < i; i2++) {
            arrayList.add(mc.player.currentScreenHandler.getSlot(i2));
        }

        return arrayList;
    }

    public static void clickSlotLocal(int i, int i2, SlotActionType slotActionType) {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        ClientPlayerEntity clientPlayerEntity = mc.player;
        screenHandler.onSlotClick(i, i2, slotActionType, clientPlayerEntity);
        int i3 = screenHandler.syncId;
        ItemStack itemStack = screenHandler.getCursorStack().copy();
        sendClickPacketInternal(i3, i, i2, slotActionType, itemStack);
    }

    public static void sendRandomClick() {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        DefaultedRegistry<Item> defaultedRegistry = Registries.ITEM;
        float f = (float)ThreadLocalRandom.current().nextDouble(0.0, 101.0);
        boolean flag;
        if (!Float.isNaN(f)) {
            flag = Float.isNaN(f);
        } else {
            flag = true;
        }

        boolean flag2 = !(f > 2.1474835E9F);
        int i = !Float.isNaN(f) && !(f >= 2.1474836E9F) && f >= -2.1474836E9F ? (int)f : Integer.MIN_VALUE;
        int i2 = flag2 ? i : Integer.MAX_VALUE;
        int i3 = flag ? 0 : i2;
        ItemStack itemStack = defaultedRegistry.get(i3).getDefaultStack();
        Stream<Slot> stream2 = screenHandler.slots.stream();
        Predicate<Slot> predicate = slot2 -> !slot2.getStack().isEmpty();
        Stream<Slot> stream3 = stream2.filter(predicate);
        Function<Slot, Integer> function = slot3 -> slot3.id;
        Optional<Integer> optionalInteger = stream3.map(function).findFirst();
        Integer integer = 0;
        int i4 = optionalInteger.orElse(integer);
        int i5 = screenHandler.syncId;
        SlotActionType slotActionType = SlotActionType.PICKUP_ALL;
        sendClickPacketInternal(i5, i4, 0, slotActionType, itemStack);
    }

    public static void moveToHotbar(Slot slot, int i, boolean flag) {
        if (slot != null && slot.id != i && slot.id != -1) {
            int i2 = slot.id;
            int i3 = mc.player.currentScreenHandler.slots.size() - 10;
            boolean flag2 = i2 >= i3 && i3 == 36;
            Runnable runnable = flag2 ? () -> clickSlotLocal(i, i2 - i3, SlotActionType.SWAP) : () -> {
                clickSlotLocal(i2, 8, SlotActionType.SWAP);
                clickSlotLocal(i, 8, SlotActionType.SWAP);
                clickSlotLocal(i2, 8, SlotActionType.SWAP);
            };
            if (flag) {
                MovementPauseComponent.queue(runnable);
            } else {
                runnable.run();
            }
        }
    }

    public static void sendClickPacket(int i, int i2, int i3, SlotActionType slotActionType, ItemStack itemStack) {
        sendClickPacketInternal(i, i2, i3, slotActionType, itemStack);
    }

    private static void sendClickPacketInternal(int i, int i2, int i3, SlotActionType slotActionType, ItemStack itemStack) {
        ItemStackHash itemStackHash = ItemStackHash.fromItemStack(itemStack, mc.getNetworkHandler().getComponentHasher());
        mc.getNetworkHandler()
            .sendPacket(new ClickSlotC2SPacket(i, 0, Shorts.checkedCast(i2), SignedBytes.checkedCast(i3), slotActionType, Int2ObjectMaps.emptyMap(), itemStackHash));
    }

    private static boolean isReady() {
        return mc.player != null && mc.player.currentScreenHandler != null && mc.player.getInventory() != null;
    }

    public static int getSelectedSlot() {
        return ((PlayerInventoryAccessor)mc.player.getInventory()).sadnes$getSelectedSlot();
    }

    public static boolean withHotbarSlot(int i, BooleanSupplier booleanSupplier) {
        if (i >= 0 && i <= 8 && booleanSupplier != null && isReady()) {
            int i2 = getSelectedSlot();
            if (i2 == i) {
                return booleanSupplier.getAsBoolean();
            }

            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));

            try {
                return booleanSupplier.getAsBoolean();
            } finally {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i2));
            }
        } else {
            return false;
        }
    }

    public static void setSelectedSlot(int i) {
        if (i >= 0 && i <= 8 && mc.interactionManager != null) {
            ((PlayerInventoryAccessor)mc.player.getInventory()).sadnes$setSelectedSlot(i);
            ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot();
        }
    }

    private static int toContainerSlot(int i) {
        return i >= 0 && i < 9 ? i + 36 : i;
    }

    private static boolean isOnCooldown(Item item) {
        if (isReady() && item != null) {
            for (int i = 0; i < mc.player.getInventory().size(); i++) {
                ItemStack itemStack = mc.player.getInventory().getStack(i);
                if (!itemStack.isEmpty() && itemStack.isOf(item) && mc.player.getItemCooldownManager().isCoolingDown(itemStack)) {
                    return true;
                }
            }
        }

        return false;
    }

    public record BundleSlot(Slot bundleSlot, int indexInBundle) {
    }
}
