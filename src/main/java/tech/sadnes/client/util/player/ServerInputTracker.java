package tech.sadnes.client.util.player;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket;
import net.minecraft.util.PlayerInput;
import tech.sadnes.api.mixin.accessor.ClientPlayerEntityAccessor;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.compat.ProtocolUtil;

public final class ServerInputTracker implements IMinecraft {
    private static volatile PlayerInput lastInput = PlayerInput.DEFAULT;
    private static volatile boolean serverSprinting;
    private static volatile boolean inputSentThisTick;
    private static volatile boolean inputSentManually;
    private static volatile boolean pendingRestore;

    private ServerInputTracker() {
    }

    public static void sendInput(PlayerInput playerInput) {
        if (mc.player != null) {
            mc.player.networkHandler.sendPacket(new PlayerInputC2SPacket(playerInput));
            inputSentManually = true;
        }
    }

    public static void restoreLastInput() {
        if (pendingRestore && mc.player != null) {
            pendingRestore = false;
            ((ClientPlayerEntityAccessor)mc.player).sadnes$setLastPlayerInput(lastInput);
        }
    }

    public static void onPacketSend(Packet<?> packet) {
        if (packet instanceof ClientTickEndC2SPacket) {
            inputSentThisTick = false;
            if (inputSentManually) {
                pendingRestore = true;
            }

            inputSentManually = false;
        }

        if (packet instanceof PlayerInputC2SPacket playerInputC2SPacket) {
            lastInput = playerInputC2SPacket.input();
            inputSentThisTick = true;
        }

        if (packet instanceof ClientCommandC2SPacket clientCommandC2SPacket) {
            if (clientCommandC2SPacket.getMode() == Mode.START_SPRINTING) {
                serverSprinting = true;
            }

            if (clientCommandC2SPacket.getMode() == Mode.STOP_SPRINTING) {
                serverSprinting = false;
            }
        }
    }

    public static void onPacketReceive(Packet<?> packet) {
        if (packet instanceof AdvancementUpdateS2CPacket && ((AdvancementUpdateS2CPacket)packet).shouldClearCurrent()) {
            lastInput = PlayerInput.DEFAULT;
            serverSprinting = false;
        }
    }

    public static void reset() {
        lastInput = PlayerInput.DEFAULT;
        if (mc.player != null && mc.player.isSprinting()) {
            serverSprinting = true;
        } else {
            serverSprinting = false;
        }
    }

    private static boolean hasMovementInput() {
        PlayerInput playerInput = lastInput;
        boolean flag;
        if (playerInput.sneak()) {
            flag = !ProtocolUtil.isOlderThan("1.21.9");
        } else {
            flag = false;
        }

        return playerInput.forward() || playerInput.backward() || playerInput.left() || playerInput.right() || playerInput.jump() || flag;
    }

    public static boolean isStandingStill() {
        boolean flag;
        if (mc.player != null && mc.player.isSwimming()) {
            flag = mc.player.isOnGround();
        } else {
            flag = false;
        }

        return !hasMovementInput() && (!serverSprinting || flag);
    }

    public static String describeInput() {
        PlayerInput playerInput = lastInput;
        StringBuilder builder = new StringBuilder();
        if (playerInput.forward()) {
            builder.append('W');
        }

        if (playerInput.backward()) {
            builder.append('S');
        }

        if (playerInput.left()) {
            builder.append('A');
        }

        if (playerInput.right()) {
            builder.append('D');
        }

        if (playerInput.jump()) {
            builder.append(" jump");
        }

        if (playerInput.sneak()) {
            builder.append(" sneak");
        }

        if (serverSprinting) {
            builder.append(" sprint");
        }

        return builder.isEmpty() ? "standing" : builder.toString().strip();
    }

    public static PlayerInput getLastInput() {
        return lastInput;
    }

    public static boolean isInputSentThisTick() {
        return inputSentThisTick;
    }

    public static boolean isInputSentManually() {
        return inputSentManually;
    }
}
