package tech.sadnes.client.feature.component;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.Component;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.CloseScreenEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.util.chat.ChatUtil;

public final class InventoryLayoutComponent extends FeatureComponent {
    private static final long STEP_DELAY_MS = 200L;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sadnes-layout");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> task;
    private final Map<Integer, Integer> attempts = new HashMap<>();

    public static InventoryLayoutComponent getInstance() {
        return Sadnes.getInstance().getComponentManager().getComponent(InventoryLayoutComponent.class);
    }

    @EventHandler
    public void onEvent(CloseScreenEvent closeScreenEvent) {
        if (this.task != null) {
            closeScreenEvent.setCancelled(true);
        }
    }

    public Map<Integer, String> captureLayout() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        if (mc.player == null) {
            return linkedHashMap;
        }

        PlayerScreenHandler playerScreenHandler2 = mc.player.playerScreenHandler;

        for (int i = 5; i <= 45; i++) {
            ItemStack itemStack = playerScreenHandler2.getSlot(i).getStack();
            if (!itemStack.isEmpty()) {
                linkedHashMap.put(i, getStackKey(itemStack));
            }
        }

        return linkedHashMap;
    }

    public void loadLayout(Map<Integer, String> map) {
        if (mc.player != null && mc.interactionManager != null) {
            if (this.task != null) {
                this.task.cancel(false);
                this.task = null;
            }

            LinkedHashMap linkedHashMap = new LinkedHashMap();

            for (Entry entry : map.entrySet()) {
                int i = (Integer)entry.getKey();
                if (i >= 5 && i <= 45) {
                    linkedHashMap.put(i, (String)entry.getValue());
                }
            }

            if (mc.currentScreen != null) {
                mc.player.closeHandledScreen();
            }

            mc.setScreen(new InventoryScreen(mc.player));
            this.attempts.clear();
            this.scheduleStep(linkedHashMap, new HashSet<>(), 0L);
        }
    }

    private void scheduleStep(Map<Integer, String> map, Set<Integer> integers, long l) {
        this.task = this.scheduler.schedule(() -> mc.execute(() -> this.step(map, integers)), l, TimeUnit.MILLISECONDS);
    }

    private void step(Map<Integer, String> map, Set<Integer> integers) {
        if (this.task != null) {
            if (mc.player == null || mc.interactionManager == null) {
                this.stop();
            } else if (!(mc.currentScreen instanceof InventoryScreen)) {
                mc.setScreen(new InventoryScreen(mc.player));
                this.scheduleStep(map, integers, 200L);
            } else if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                this.scheduleStep(map, integers, 200L);
            } else {
                PlayerScreenHandler playerScreenHandler2 = mc.player.playerScreenHandler;
                if (!playerScreenHandler2.getCursorStack().isEmpty()) {
                    int i = this.findEmptySlot(playerScreenHandler2, -1, -1);
                    if (i != -1) {
                        this.clickPickup(playerScreenHandler2.syncId, i);
                    }

                    this.scheduleStep(map, integers, 200L);
                } else {
                    int i2 = -1;

                    for (Entry entry : map.entrySet()) {
                        int i3 = (Integer)entry.getKey();
                        if (!integers.contains(i3) && !this.getSlotKey(playerScreenHandler2, i3).equals(entry.getValue())) {
                            i2 = i3;
                            break;
                        }
                    }

                    if (i2 == -1) {
                        ChatUtil.print("Раскладка загружена.");
                        this.stop();
                    } else {
                        String string = (String)map.get(i2);
                        int i4 = this.findSourceSlot(playerScreenHandler2, map, string, i2);
                        if (i4 != -1 && this.attempts.merge(i2, 1, Integer::sum) <= 6) {
                            this.swapSlots(playerScreenHandler2, i4, i2, integers);
                            this.scheduleStep(map, integers, 200L);
                        } else {
                            integers.add(i2);
                            this.scheduleStep(map, integers, 200L);
                        }
                    }
                }
            }
        }
    }

    private void swapSlots(ScreenHandler screenHandler, int i, int i2, Set<Integer> integers) {
        ItemStack itemStack = screenHandler.getSlot(i).getStack();
        ItemStack itemStack2 = screenHandler.getSlot(i2).getStack();
        boolean flag = itemStack2.isEmpty();
        if (!screenHandler.getSlot(i2).canInsert(itemStack)) {
            integers.add(i2);
        } else if (!flag && !screenHandler.getSlot(i).canInsert(itemStack2)) {
            integers.add(i2);
        } else {
            int i3 = this.findHotbarBuffer(screenHandler, i, i2);
            if (i3 == -1) {
                integers.add(i2);
            } else {
                int i4 = screenHandler.syncId;
                boolean flag2 = !screenHandler.getSlot(36 + i3).hasStack();
                this.clickSwap(i4, i, i3);
                this.clickSwap(i4, i2, i3);
                if (!flag || !flag2) {
                    this.clickSwap(i4, i, i3);
                }
            }
        }
    }

    private int findHotbarBuffer(ScreenHandler screenHandler, int i, int i2) {
        int i3 = -1;

        for (int i4 = 0; i4 < 9; i4++) {
            int i5 = 36 + i4;
            if (i5 != i && i5 != i2) {
                ItemStack itemStack = screenHandler.getSlot(i5).getStack();
                if (itemStack.isEmpty()) {
                    return i4;
                }

                if (i3 == -1 && screenHandler.getSlot(i).canInsert(itemStack) && screenHandler.getSlot(i2).canInsert(itemStack)) {
                    i3 = i4;
                }
            }
        }

        return i3;
    }

    private void clickSwap(int i, int i2, int i3) {
        mc.interactionManager.clickSlot(i, i2, i3, SlotActionType.SWAP, mc.player);
    }

    private int findSourceSlot(ScreenHandler screenHandler, Map<Integer, String> map, String string, int i) {
        for (int i2 = 5; i2 <= 45; i2++) {
            if (i2 != i && this.getSlotKey(screenHandler, i2).equals(string) && !string.equals(map.get(i2))) {
                return i2;
            }
        }

        return -1;
    }

    private int findEmptySlot(ScreenHandler screenHandler, int i, int i2) {
        for (int i3 = 9; i3 <= 44; i3++) {
            if (i3 != i && i3 != i2 && !screenHandler.getSlot(i3).hasStack()) {
                return i3;
            }
        }

        return -1;
    }

    private void clickPickup(int i, int i2) {
        mc.interactionManager.clickSlot(i, i2, 0, SlotActionType.PICKUP, mc.player);
    }

    private String getSlotKey(ScreenHandler screenHandler, int i) {
        return getStackKey(screenHandler.getSlot(i).getStack());
    }

    private static String getStackKey(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return "";
        }

        String string = Registries.ITEM.getId(itemStack.getItem()).toString();
        if (mc.world == null) {
            return string;
        }

        RegistryOps registryOps = RegistryOps.of(NbtOps.INSTANCE, mc.world.getRegistryManager());
        ArrayList arrayList = new ArrayList();

        for (Component component : itemStack.getComponents()) {
            if (component.type() != DataComponentTypes.BUNDLE_CONTENTS) {
                String string2 = encodeComponent(component, registryOps);
                if (string2 != null) {
                    arrayList.add(string2);
                }
            }
        }

        Collections.sort(arrayList);
        return string + arrayList;
    }

    private static <T> String encodeComponent(Component<T> component, RegistryOps<NbtElement> registryOps) {
        Codec codec = component.type().getCodec();
        if (codec == null) {
            return null;
        }

        Identifier identifier = Registries.DATA_COMPONENT_TYPE.getId(component.type());
        return identifier + "=" + codec.encodeStart(registryOps, component.value()).result().map(Object::toString).orElse("");
    }

    private void stop() {
        if (this.task != null) {
            this.task.cancel(false);
        }

        this.task = null;
        if (mc.currentScreen instanceof InventoryScreen) {
            mc.setScreen(null);
        }
    }
}
