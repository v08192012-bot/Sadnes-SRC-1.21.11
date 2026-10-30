package tech.sadnes.client.util.network;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.minecraft.network.packet.Packet;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.util.IMinecraft;

public final class PacketQueue implements IMinecraft {
    private static final Deque<Runnable> TASKS = new ConcurrentLinkedDeque<>();
    private static final Deque<Runnable> DELAYED_TASKS = new ConcurrentLinkedDeque<>();

    public PacketQueue() {
        Sadnes.getEventBus().subscribeStatic(PacketQueue.class);
    }

    public static void sendPacket(Packet<?> packet) {
        TASKS.add(() -> mc.player.networkHandler.sendPacket(packet));
    }

    public static void queue(Runnable runnable) {
        TASKS.add(runnable);
    }

    public static void queueDelayed(Runnable runnable) {
        DELAYED_TASKS.add(runnable);
    }

    public static boolean hasDelayed() {
        return !DELAYED_TASKS.isEmpty();
    }

    public static void flush() {
        if (!TASKS.isEmpty() || !DELAYED_TASKS.isEmpty()) {
            if (mc.player != null && mc.player.networkHandler != null) {
                Runnable runnable;
                while ((runnable = TASKS.poll()) != null) {
                    runnable.run();
                }

                Runnable runnable2 = DELAYED_TASKS.poll();
                if (runnable2 != null) {
                    runnable2.run();
                }
            } else {
                TASKS.clear();
                DELAYED_TASKS.clear();
            }
        }
    }
}
