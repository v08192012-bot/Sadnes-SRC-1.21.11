package tech.sadnes.client.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import tech.sadnes.client.lang.Language;

public enum Key {
    KEY_SPACE("SPACE", 32),
    KEY_APOSTROPHE("'", 39),
    KEY_COMMA(",", 44),
    KEY_MINUS("-", 45),
    KEY_PERIOD("PERIOD", 46),
    KEY_SLASH("/", 47),
    KEY_0("0", 48),
    KEY_1("1", 49),
    KEY_2("2", 50),
    KEY_3("3", 51),
    KEY_4("4", 52),
    KEY_5("5", 53),
    KEY_6("6", 54),
    KEY_7("7", 55),
    KEY_8("8", 56),
    KEY_9("9", 57),
    KEY_SEMICOLON("SEMICOLON", 59),
    KEY_EQUAL("=", 61),
    KEY_A("A", 65),
    KEY_B("B", 66),
    KEY_C("C", 67),
    KEY_D("D", 68),
    KEY_E("E", 69),
    KEY_F("F", 70),
    KEY_G("G", 71),
    KEY_H("H", 72),
    KEY_I("I", 73),
    KEY_J("J", 74),
    KEY_K("K", 75),
    KEY_L("L", 76),
    KEY_M("M", 77),
    KEY_N("N", 78),
    KEY_O("O", 79),
    KEY_P("P", 80),
    KEY_Q("Q", 81),
    KEY_R("R", 82),
    KEY_S("S", 83),
    KEY_T("T", 84),
    KEY_U("U", 85),
    KEY_V("V", 86),
    KEY_W("W", 87),
    KEY_X("X", 88),
    KEY_Y("Y", 89),
    KEY_Z("Z", 90),
    KEY_LEFT_BRACKET("LBRACKET", 91),
    KEY_BACKSLASH("BACKSLASH", 92),
    KEY_RIGHT_BRACKET("RBRACKET", 93),
    KEY_GRAVE("~", 96),
    KEY_WORLD_1("WORLD1", 161),
    KEY_WORLD_2("WORLD2", 162),
    KEY_ESCAPE("ESC", 256),
    KEY_ENTER("ENTER", 257),
    KEY_TAB("TAB", 258),
    KEY_BACKSPACE("BACKSPACE", 259),
    KEY_INSERT("INSERT", 260),
    KEY_DELETE("DELETE", 261),
    KEY_RIGHT("→", 262),
    KEY_LEFT("←", 263),
    KEY_DOWN("↓", 264),
    KEY_UP("↑", 265),
    KEY_PAGE_UP("PAGEUP", 266),
    KEY_PAGE_DOWN("PAGEDOWN", 267),
    KEY_HOME("HOME", 268),
    KEY_END("END", 269),
    KEY_CAPS_LOCK("CAPS", 280),
    KEY_SCROLL_LOCK("SCROLLLOCK", 281),
    KEY_NUM_LOCK("NUMLOCK", 282),
    KEY_PRINT_SCREEN("PRINTSCREEN", 283),
    KEY_PAUSE("PAUSE", 284),
    KEY_F1("F1", 290),
    KEY_F2("F2", 291),
    KEY_F3("F3", 292),
    KEY_F4("F4", 293),
    KEY_F5("F5", 294),
    KEY_F6("F6", 295),
    KEY_F7("F7", 296),
    KEY_F8("F8", 297),
    KEY_F9("F9", 298),
    KEY_F10("F10", 299),
    KEY_F11("F11", 300),
    KEY_F12("F12", 301),
    KEY_F13("F13", 302),
    KEY_F14("F14", 303),
    KEY_F15("F15", 304),
    KEY_F16("F16", 305),
    KEY_F17("F17", 306),
    KEY_F18("F18", 307),
    KEY_F19("F19", 308),
    KEY_F20("F20", 309),
    KEY_F21("F21", 310),
    KEY_F22("F22", 311),
    KEY_F23("F23", 312),
    KEY_F24("F24", 313),
    KEY_F25("F25", 314),
    KEY_KP_0("NUM0", 320),
    KEY_KP_1("NUM1", 321),
    KEY_KP_2("NUM2", 322),
    KEY_KP_3("NUM3", 323),
    KEY_KP_4("NUM4", 324),
    KEY_KP_5("NUM5", 325),
    KEY_KP_6("NUM6", 326),
    KEY_KP_7("NUM7", 327),
    KEY_KP_8("NUM8", 328),
    KEY_KP_9("NUM9", 329),
    KEY_KP_DECIMAL("KPDECIMAL", 330),
    KEY_KP_DIVIDE("KPDIVIDE", 331),
    KEY_KP_MULTIPLY("KPMULTIPLY", 332),
    KEY_KP_SUBTRACT("KPSUBTRACT", 333),
    KEY_KP_ADD("KPADD", 334),
    KEY_KP_ENTER("KPENTER", 335),
    KEY_KP_EQUAL("KPEQUAL", 336),
    KEY_LEFT_SHIFT("LSHIFT", 340),
    KEY_LEFT_CONTROL("LCONTROL", 341),
    KEY_LEFT_ALT("LALT", 342),
    KEY_LEFT_SUPER("LSUPER", 343),
    KEY_RIGHT_SHIFT("RSHIFT", 344),
    KEY_RIGHT_CONTROL("RCONTROL", 345),
    KEY_RIGHT_ALT("RALT", 346),
    KEY_RIGHT_SUPER("RSUPER", 347),
    KEY_MENU("MENU", 348),
    KEY_LAST("LAST", 348),
    MOUSE_1("M1", 0),
    MOUSE_2("M2", 1),
    MOUSE_3("M3", 2),
    MOUSE_4("M4", 3),
    MOUSE_5("M5", 4),
    MOUSE_6("M6", 5),
    MOUSE_7("M7", 6),
    MOUSE_8("M8", 7),
    MOUSE_LAST("MOUSELAST", 7),
    MOUSE_LEFT("MOUSELEFT", 0),
    MOUSE_RIGHT("MOUSERIGHT", 1),
    MOUSE_MIDDLE("MOUSEMIDDLE", 2),
    KEY_NONE("NONE", -1);

    private final String name;
    private final int code;
    private static final Map<Integer, Key> BY_CODE = new HashMap<>();
    private static final Map<String, Key> BY_NAME = new HashMap<>();

    @Override
    public String toString() {
        return this.name;
    }

    public static String getDisplayName(int i) {
        switch (i) {
            case 340:
                return "LShift";
            case 341:
                return "LCtrl";
            case 342:
                return "LAlt";
            case 343:
                return "LWin";
            case 344:
                return "RShift";
            case 345:
                return "RCtrl";
            case 346:
                return "RAlt";
            case 347:
                return "RWin";
            default:
                Key key = BY_CODE.getOrDefault(i, KEY_NONE);
                if (key == KEY_NONE) {
                    return Language.translate("setting.bind.none", "N/A");
                } else {
                    String string = key.name.toLowerCase();
                    return Character.toUpperCase(string.charAt(0)) + string.substring(1);
                }
        }
    }

    public static int getCode(String string) {
        if (string.equalsIgnoreCase("shift")) {
            return 340;
        }

        Key key = BY_NAME.getOrDefault(string.toLowerCase(), KEY_NONE);
        return key.code;
    }

    public static Key fromCode(int i) {
        return BY_CODE.getOrDefault(i, KEY_NONE);
    }

    public static boolean isModifier(int i) {
        return getModifier(i) != KEY_NONE.code;
    }

    public static boolean matches(int i, int i2) {
        if (i == i2) {
            return true;
        }

        int i3 = getModifier(i);
        return i3 != KEY_NONE.code && i3 == getModifier(i2);
    }

    public static int getModifier(int i) {
        switch (i) {
            case 340:
            case 341:
            case 342:
            case 343:
            case 344:
            case 345:
            case 346:
            case 347:
                return i;
            default:
                return -1;
        }
    }

    public static boolean isPressed(int i) {
        if (i < 0) {
            return false;
        } else {
            long l = getWindowHandle();
            if (l == 0L) {
                return false;
            } else {
                return i <= 7 ? GLFW.glfwGetMouseButton(l, i) == 1 : GLFW.glfwGetKey(l, i) == 1;
            }
        }
    }

    public static String getModifierName(int i) {
        return getDisplayName(i);
    }

    public static String getComboName(int[] ints, int i) {
        if (ints != null && ints.length != 0) {
            StringBuilder builder = new StringBuilder();

            for (int i2 : ints) {
                builder.append(getModifierName(i2)).append(" + ");
            }

            return builder.append(getDisplayName(i)).toString();
        } else {
            return getDisplayName(i);
        }
    }

    private static long getWindowHandle() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        return minecraftClient != null && minecraftClient.getWindow() != null ? minecraftClient.getWindow().getHandle() : 0L;
    }

    public boolean is(int i) {
        return i == this.getKeyCode();
    }

    public String getName() {
        return this.name;
    }

    public int getKeyCode() {
        return this.code;
    }

    Key(String string, int i) {
        this.name = string;
        this.code = i;
    }

    static {
        for (Key key : values()) {
            BY_CODE.put(key.code, key);
            BY_NAME.put(key.name.toLowerCase(), key);
        }
    }
}
