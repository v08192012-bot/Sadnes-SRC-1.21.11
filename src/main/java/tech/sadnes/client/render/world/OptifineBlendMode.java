package tech.sadnes.client.render.world;

import org.joml.Vector4f;

final class OptifineBlendMode {
    static final int ALPHA = 0;
    static final int ADD = 1;
    static final int SUBTRACT = 2;
    static final int MULTIPLY = 3;
    static final int DODGE = 4;
    static final int BURN = 5;
    static final int SCREEN = 6;
    static final int OVERLAY = 7;
    static final int REPLACE = 8;
    static final int COUNT = 9;

    private OptifineBlendMode() {
    }

    static int parse(String string) {
        if (string == null) {
            return 1;
        }

        String string2 = string.toLowerCase().trim();
        byte b = -1;
        switch (string2.hashCode()) {
            case -2060248300:
                if (string2.equals("subtract")) {
                    return 2;
                }
                break;
            case -1091287984:
                if (string2.equals("overlay")) {
                    return 7;
                }
                break;
            case -907689876:
                if (string2.equals("screen")) {
                    return 6;
                }
                break;
            case 96417:
                if (string2.equals("add")) {
                    return 1;
                }
                break;
            case 3035599:
                if (string2.equals("burn")) {
                    return 5;
                }
                break;
            case 92909918:
                if (string2.equals("alpha")) {
                    return 0;
                }
                break;
            case 95758295:
                if (string2.equals("dodge")) {
                    return 4;
                }
                break;
            case 653829668:
                if (string2.equals("multiply")) {
                    return 3;
                }
                break;
            case 1094496948:
                if (string2.equals("replace")) {
                    b = 8;
                }
        }

        switch (b) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            case 8:
                return 8;
            default:
                return 1;
        }
    }

    static Vector4f getColorMultiplier(int i, float f) {
        switch (i) {
            case 2:
            case 4:
            case 5:
            case 6:
            case 7:
                return new Vector4f(f, f, f, 1.0F);
            case 3:
                return new Vector4f(f, f, f, f);
            default:
                return new Vector4f(1.0F, 1.0F, 1.0F, f);
        }
    }
}
