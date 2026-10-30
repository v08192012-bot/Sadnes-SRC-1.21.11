package tech.sadnes.client.feature.component;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import org.lwjgl.glfw.GLFW;
import tech.sadnes.api.mixin.accessor.KeyBindingAccessor;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.TickQueue;
import tech.sadnes.client.util.player.MovementSimulator;

public final class MovementPauseComponent extends FeatureComponent {
    private static final TickQueue TASK_QUEUE = new TickQueue();
    private static final Set<Object> PAUSE_HOLDERS = Collections.newSetFromMap(new IdentityHashMap<>());

    public static boolean hasPendingTasks() {
        return TASK_QUEUE.isEmpty();
    }

    public static boolean isPaused() {
        return !PAUSE_HOLDERS.isEmpty();
    }

    public static void clearPause() {
        PAUSE_HOLDERS.clear();
    }

    public static void setDelay(int i) {
        TASK_QUEUE.delay(i);
    }

    public static void queue(Runnable runnable) {
        if (ServerManager.isHolyWorld()) {
            TASK_QUEUE.then(() -> {
                runnable.run();
                resumeMovement();
            });
        } else {
            if (ServerManager.isSpookyTime()) {
                MovementSimulator movementSimulator = MovementSimulator.simulate(2);
                boolean flag;
                if (!movementSimulator.isOnGround()) {
                    flag = !movementSimulator.isTouchingWater();
                } else {
                    flag = false;
                }

                if (flag) {
                    TASK_QUEUE.then(MovementPauseComponent::pauseMovement).then(runnable).then(MovementPauseComponent::resumeMovement);
                    return;
                }
            }

            TASK_QUEUE.then(MovementPauseComponent::pauseMovement).then(() -> {
                runnable.run();
                resumeMovement();
            });
        }
    }

    public static void pause(Object object) {
        PAUSE_HOLDERS.add(object);
        Iterator iterator2 = getMovementKeys().iterator();

        while (iterator2.hasNext()) {
            ((KeyBinding)iterator2.next()).setPressed(false);
        }
    }

    public static void resume(Object object) {
        PAUSE_HOLDERS.remove(object);
        if (PAUSE_HOLDERS.isEmpty()) {
            for (KeyBinding keyBinding : getMovementKeys()) {
                keyBinding.setPressed(isPhysicallyPressed(keyBinding));
            }
        }
    }

    public static void closeAndResume(Object object) {
        if (mc.player != null) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        }

        resume(object);
    }

    private static boolean isPhysicallyPressed(KeyBinding keyBinding) {
        if (keyBinding.isUnbound()) {
            return false;
        }

        Key key = ((KeyBindingAccessor)keyBinding).sadnes$getBoundKey();
        switch (key.getCategory()) {
            case KEYSYM:
                return InputUtil.isKeyPressed(mc.getWindow(), key.getCode());
            case MOUSE:
                if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key.getCode()) == 1) {
                    return true;
                }

                return false;
            default:
                return false;
        }
    }

    @EventHandler(priority = -250)
    public void onUpdate(PlayerTickEvent playerTickEvent) {
        TASK_QUEUE.tick();
        TASK_QUEUE.resetIfEmpty();
    }

    @EventHandler(priority = -500)
    public void onMovementInput(MoveInputEvent moveInputEvent) {
        if (!PAUSE_HOLDERS.isEmpty()) {
            moveInputEvent.setCancelled(true);
        }
    }

    private static void pauseMovement() {
        pause(TASK_QUEUE);
    }

    private static void resumeMovement() {
        closeAndResume(TASK_QUEUE);
    }

    private static List<KeyBinding> getMovementKeys() {
        boolean flag = mc.options.getSprintToggled().getValue();
        return flag
            ? List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey)
            : List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey, mc.options.sprintKey);
    }
}
