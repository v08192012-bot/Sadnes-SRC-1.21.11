package tech.sadnes.client.util.rotation;

import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import tech.sadnes.client.util.IMinecraft;

public final class SensitivityUtil implements IMinecraft {
    private SensitivityUtil() {
    }

    public static float snapDelta(float f) {
        return getStepCount(f) * getGcd();
    }

    public static float getGcd() {
        return (float)(getSensitivityFactor() * 0.15);
    }

    public static float getSensitivityFactor() {
        double d = mc.options.getMouseSensitivity().getValue();
        return (float)(Math.pow(d * 0.6F + 0.2F, 3.0) * 8.0);
    }

    public static float getStepCount(float f) {
        float f2 = getGcd();
        return f2 == 0.0F ? f : Math.round(f / f2);
    }

    public static float snapAngle(float f) {
        float f2 = getGcd();
        if (f2 == 0.0F) {
            return f;
        }

        float f3 = MathHelper.wrapDegrees(f);
        return f3 - f3 % f2;
    }

    public static Vector2f snapRotation(float f, float f2) {
        return new Vector2f(snapAngle(f), snapAngle(f2));
    }
}
