package tech.sadnes.client.feature.component;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.feature.impl.movement.GuiWalk;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.compat.ProtocolUtil;
import tech.sadnes.client.util.network.PacketQueue;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.ServerInputTracker;

public class GuiMoveComponent extends FeatureComponent {
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "GuiMoveService");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicInteger PENDING_TASKS = new AtomicInteger();
    private static volatile boolean executingAction;
    public static boolean forceActive = false;

    public static boolean isActive() {
        return PENDING_TASKS.get() > 0 || PacketQueue.hasDelayed() || forceActive;
    }

    public static boolean isExecutingAction() {
        return executingAction;
    }

    public static void scheduleAction(Runnable runnable, float f, float f2) {
        PENDING_TASKS.incrementAndGet();
        ScheduledExecutorService scheduledExecutorService = EXECUTOR;
        Float floatValue = f;
        Float floatValue2 = f2;
        Runnable runnable2 = () -> {
            try {
                try {
                    Thread.sleep((long)(floatValue * 50.0F));
                    AtomicBoolean atomicBoolean = new AtomicBoolean();

                    for (int i = 0; i < 10 && !atomicBoolean.get(); i++) {
                        boolean flag = i == 9;
                        boolean flag2 = runOnMainThread(() -> {
                            boolean flag3;
                            if (ServerManager.isDonut()) {
                                flag3 = !ServerInputTracker.isStandingStill();
                            } else {
                                flag3 = false;
                            }

                            if (!flag3 || flag) {
                                executeAction(runnable);
                                atomicBoolean.set(true);
                            }
                        });
                        if (!flag2) {
                            return;
                        }
                    }

                    Thread.sleep((long)(floatValue2 * 50.0F));
                    if (mc.player != null) {
                        PacketQueue.queueDelayed(() -> {
                            if (mc.player != null && mc.player.currentScreenHandler != null) {
                                boolean flag4 = mc.player.currentScreenHandler == mc.player.playerScreenHandler;
                                ItemStack itemStack = mc.player.currentScreenHandler.getCursorStack();
                                if (flag4 && itemStack.isEmpty()) {
                                    mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
                                }
                            }
                        });
                    }

                    return;
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                } catch (Throwable throwable) {
                }
            } finally {
                finishTask();
                PacketQueue.queueDelayed(() -> {
                    GuiWalk guiWalk = GuiWalk.getInstance();
                    if (guiWalk != null) {
                        guiWalk.clearQueuedClicks();
                    }
                });
            }
        };
        scheduledExecutorService.submit(runnable2);
    }

    public static void scheduleActions(List<Runnable> runnables, Runnable runnable) {
        List<Runnable> runnables2 = List.copyOf(runnables);
        PENDING_TASKS.incrementAndGet();
        EXECUTOR.submit(() -> {
            try {
                try {
                    Thread.sleep(100L);

                    for (Runnable runnable2 : runnables2) {
                        if (!runOnMainThread(() -> executeAction(runnable2))) {
                            return;
                        }
                    }

                    if (ServerManager.isReallyWorld()) {
                        runOnMainThread(() -> {
                            GuiWalk guiWalk = GuiWalk.getInstance();
                            if (guiWalk != null) {
                                guiWalk.clearQueuedClicks();
                            }
                        });
                    }

                    if (ServerManager.isServer("donut")) {
                        runOnMainThread(() -> {
                            GuiWalk guiWalk2 = GuiWalk.getInstance();
                            if (guiWalk2 != null) {
                                sendFakeClick();
                            }
                        });
                    }

                    if (runnable != null) {
                        runOnMainThread(runnable);
                    }

                    Thread.sleep(100L);
                    return;
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                } catch (Throwable throwable) {
                }
            } finally {
                finishTask();
            }
        });
    }

    public static void sendFakeClick() {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        ItemStack itemStack = Registries.ITEM.get(ThreadLocalRandom.current().nextInt(0, 100)).getDefaultStack();
        ItemStackHash itemStackHash = ItemStackHash.fromItemStack(itemStack, mc.player.networkHandler.getComponentHasher());
        mc.player
            .networkHandler
            .sendPacket(
                new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(), (short)0, (byte)0, SlotActionType.PICKUP_ALL, Int2ObjectMaps.singleton(0, itemStackHash), itemStackHash)
            );
    }

    private static boolean runOnMainThread(Runnable runnable) throws InterruptedException {
        if (mc.player != null && mc.player.networkHandler != null) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            AtomicBoolean atomicBoolean = new AtomicBoolean(true);
            PacketQueue.queueDelayed(() -> {
                if (!atomicBoolean.compareAndSet(true, false)) {
                    countDownLatch.countDown();
                } else {
                    try {
                        runnable.run();
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            });
            return countDownLatch.await(5L, TimeUnit.SECONDS) ? true : !atomicBoolean.compareAndSet(true, false) && countDownLatch.await(1L, TimeUnit.SECONDS);
        } else {
            return false;
        }
    }

    public static void executeAction(Runnable runnable) {
        executingAction = true;

        try {
            if (ServerManager.isSpookyTime() && ProtocolUtil.isOlderThan("26.2")) {
                InventoryUtil.clickSlot(0, 0, SlotActionType.PICKUP);
            }

            runnable.run();
        } finally {
            executingAction = false;
        }
    }

    private static void finishTask() {
        PENDING_TASKS.decrementAndGet();
    }

    @EventHandler(priority = -500)
    private void onMovementInput(MoveInputEvent moveInputEvent) {
        if (isActive()) {
            moveInputEvent.setCancelled(true);
        }
    }

    @EventHandler
    private void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && mc.player != null) {
            GuiWalk guiWalk = GuiWalk.getInstance();
            if (guiWalk != null && guiWalk.shouldDelayPacket(packetEvent.getPacket())) {
                packetEvent.cancel();
            }
        }
    }
}
