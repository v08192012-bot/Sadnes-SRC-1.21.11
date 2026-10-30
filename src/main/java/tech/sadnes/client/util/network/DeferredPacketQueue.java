package tech.sadnes.client.util.network;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;

public final class DeferredPacketQueue {
    private static final Deque<Packet<?>> QUEUE = new ArrayDeque<>();
    private static volatile boolean deferring;

    private DeferredPacketQueue() {
    }

    public static boolean isDeferring() {
        return deferring;
    }

    public static void setDeferring(boolean flag) {
        deferring = flag;
    }

    public static synchronized void enqueue(Packet<?> packet) {
        QUEUE.add(packet);
    }

    public static synchronized void flush() {
        deferring = false;
        ClientPlayNetworkHandler clientPlayNetworkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (clientPlayNetworkHandler == null) {
            QUEUE.clear();
        } else {
            Packet packet;
            while ((packet = QUEUE.poll()) != null) {
                try {
                    packet.apply(clientPlayNetworkHandler);
                } catch (Throwable throwable) {
                }
            }
        }
    }

    public static synchronized void discard() {
        deferring = false;
        QUEUE.clear();
    }
}
