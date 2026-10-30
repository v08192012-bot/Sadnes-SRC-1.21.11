package tech.sadnes.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;
import org.lwjgl.glfw.GLFW;

public final class KeyUtil {
    private KeyUtil() {
    }

    public static boolean isKeyBindingPressed(KeyBinding keyBinding) {
        return isKeyPressed(keyBinding.getDefaultKey().getCategory(), keyBinding.getDefaultKey().getCode());
    }

    public static boolean isKeyPressed(Type type, int i) {
        if (i == -1) {
            return false;
        } else {
            long l = MinecraftClient.getInstance().getWindow().getHandle();
            if (type == Type.KEYSYM) {
                return GLFW.glfwGetKey(l, i) == 1;
            } else {
                return type == Type.MOUSE ? GLFW.glfwGetMouseButton(l, i) == 1 : false;
            }
        }
    }
}
