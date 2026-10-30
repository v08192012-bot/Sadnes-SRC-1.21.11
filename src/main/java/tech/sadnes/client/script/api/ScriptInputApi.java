package tech.sadnes.client.script.api;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import org.lwjgl.glfw.GLFW;
import tech.sadnes.api.mixin.accessor.KeyBindingAccessor;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptInputApi {
    private static final Map<KeyBinding, ScriptInputApi.PressedKey> PRESSED_KEYS = new HashMap<>();

    private ScriptInputApi() {
    }

    public static void install(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "press", objects -> {
            KeyBinding keyBinding = getKeyBinding(ScriptValues.argString(objects, 0));
            if (keyBinding == null) {
                throw Context.reportRuntimeError("input.press: unknown key " + ScriptValues.argString(objects, 0));
            }

            KeyBindingAccessor keyBindingAccessor = (KeyBindingAccessor)keyBinding;
            keyBindingAccessor.sadnes$setTimesPressed(keyBindingAccessor.sadnes$getTimesPressed() + 1);
            keyBinding.setPressed(true);
            PRESSED_KEYS.put(keyBinding, new ScriptInputApi.PressedKey(script, Math.max(1, ScriptValues.argInt(objects, 1, 1))));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "hold", objects2 -> {
            KeyBinding keyBinding2 = getKeyBinding(ScriptValues.argString(objects2, 0));
            if (keyBinding2 == null) {
                throw Context.reportRuntimeError("input.hold: unknown key " + ScriptValues.argString(objects2, 0));
            }

            keyBinding2.setPressed(true);
            PRESSED_KEYS.put(keyBinding2, new ScriptInputApi.PressedKey(script, Math.max(1, ScriptValues.argInt(objects2, 1, 1)) + 1));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "release", objects3 -> {
            KeyBinding keyBinding3 = getKeyBinding(ScriptValues.argString(objects3, 0));
            if (keyBinding3 != null && PRESSED_KEYS.remove(keyBinding3) != null) {
                restoreKey(keyBinding3);
            }

            return null;
        });
        ScriptValues.putFunction(nativeObject, "physical", objects4 -> {
            KeyBinding keyBinding4 = getKeyBinding(ScriptValues.argString(objects4, 0));
            if (keyBinding4 == null) {
                throw Context.reportRuntimeError("input.physical: unknown key " + ScriptValues.argString(objects4, 0));
            } else {
                return isPhysicallyDown(keyBinding4);
            }
        });
        ScriptValues.putFunction(nativeObject, "isDown", objects5 -> {
            KeyBinding keyBinding5 = getKeyBinding(ScriptValues.argString(objects5, 0));
            return keyBinding5 != null && keyBinding5.isPressed() ? true : false;
        });
        ScriptValues.putFunction(nativeObject, "keyDown", objects6 -> InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), ScriptValues.argInt(objects6, 0, -1)));
        ScriptValues.putFunction(
            nativeObject,
            "mouseDown",
            objects7 -> GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), ScriptValues.argInt(objects7, 0, 0)) == 1 ? true : false
        );
        scriptableObject.put("input", scriptableObject, nativeObject);
    }

    public static void tick() {
        Iterator iterator2 = PRESSED_KEYS.entrySet().iterator();

        while (iterator2.hasNext()) {
            Entry entry = (Entry)iterator2.next();
            if (--((ScriptInputApi.PressedKey)entry.getValue()).ticksLeft <= 0) {
                iterator2.remove();
                restoreKey((KeyBinding)entry.getKey());
            }
        }
    }

    public static void releaseAll(Script script2) {
        Iterator iterator2 = PRESSED_KEYS.entrySet().iterator();

        while (iterator2.hasNext()) {
            Entry entry = (Entry)iterator2.next();
            if (((ScriptInputApi.PressedKey)entry.getValue()).script == script2) {
                iterator2.remove();
                restoreKey((KeyBinding)entry.getKey());
            }
        }
    }

    private static void restoreKey(KeyBinding keyBinding) {
        keyBinding.setPressed(isPhysicallyDown(keyBinding));
    }

    private static boolean isPhysicallyDown(KeyBinding keyBinding) {
        Key key = ((KeyBindingAccessor)keyBinding).sadnes$getBoundKey();
        long l = MinecraftClient.getInstance().getWindow().getHandle();
        switch (key.getCategory()) {
            case KEYSYM:
                return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), key.getCode());
            case MOUSE:
                if (GLFW.glfwGetMouseButton(l, key.getCode()) == 1) {
                    return true;
                }

                return false;
            default:
                return false;
        }
    }

    private static KeyBinding getKeyBinding(String string) {
        GameOptions gameOptions = MinecraftClient.getInstance().options;
        String string2 = string.toLowerCase(Locale.ROOT);
        byte b = -1;
        switch (string2.hashCode()) {
            case -2020599460:
                if (string2.equals("inventory")) {
                    return gameOptions.inventoryKey;
                }
                break;
            case -1407259064:
                if (string2.equals("attack")) {
                    return gameOptions.attackKey;
                }
                break;
            case -895679974:
                if (string2.equals("sprint")) {
                    return gameOptions.sprintKey;
                }
                break;
            case -677145915:
                if (string2.equals("forward")) {
                    return gameOptions.forwardKey;
                }
                break;
            case 116103:
                if (string2.equals("use")) {
                    return gameOptions.useKey;
                }
                break;
            case 3015911:
                if (string2.equals("back")) {
                    return gameOptions.backKey;
                }
                break;
            case 3092207:
                if (string2.equals("drop")) {
                    return gameOptions.dropKey;
                }
                break;
            case 3273774:
                if (string2.equals("jump")) {
                    return gameOptions.jumpKey;
                }
                break;
            case 3317767:
                if (string2.equals("left")) {
                    return gameOptions.leftKey;
                }
                break;
            case 3440673:
                if (string2.equals("pick")) {
                    b = 12;
                }
                break;
            case 3543443:
                if (string2.equals("swap")) {
                    return gameOptions.swapHandsKey;
                }
                break;
            case 108511772:
                if (string2.equals("right")) {
                    return gameOptions.rightKey;
                }
                break;
            case 109582100:
                if (string2.equals("sneak")) {
                    return gameOptions.sneakKey;
                }
        }

        switch (b) {
            case 0:
                return gameOptions.forwardKey;
            case 1:
                return gameOptions.backKey;
            case 2:
                return gameOptions.leftKey;
            case 3:
                return gameOptions.rightKey;
            case 4:
                return gameOptions.jumpKey;
            case 5:
                return gameOptions.sneakKey;
            case 6:
                return gameOptions.sprintKey;
            case 7:
                return gameOptions.attackKey;
            case 8:
                return gameOptions.useKey;
            case 9:
                return gameOptions.dropKey;
            case 10:
                return gameOptions.swapHandsKey;
            case 11:
                return gameOptions.inventoryKey;
            case 12:
                return gameOptions.pickItemKey;
            default:
                return null;
        }
    }

    static final class PressedKey {
        final Script script;
        int ticksLeft;

        PressedKey(Script script2, int i) {
            this.script = script2;
            this.ticksLeft = i;
        }
    }
}
