package tech.sadnes.client.script.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.rotation.RotationUtil;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptRotationApi {
    private static Rotation lastRotation;

    private ScriptRotationApi() {
    }

    public static void install(ScriptableObject scriptableObject) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "set", objects -> {
            float f = ScriptValues.argFloat(objects, 0);
            float f2 = ScriptValues.argFloat(objects, 1);
            if (ScriptValues.hasArg(objects, 2)) {
                setRotation(f, f2, objects[2]);
                return null;
            } else {
                setRotation(f, f2, null);
                return null;
            }
        });
        ScriptValues.putFunction(
            nativeObject,
            "lookAt",
            objects2 -> {
                float[] floats = RotationUtil.getRotationArrayTo(
                    new Vec3d(ScriptValues.argDouble(objects2, 0, 0.0), ScriptValues.argDouble(objects2, 1, 0.0), ScriptValues.argDouble(objects2, 2, 0.0))
                );
                setRotation(floats[0], floats[1], ScriptValues.hasArg(objects2, 3) ? objects2[3] : null);
                return wrapRotation(floats[0], floats[1]);
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "angleTo",
            objects3 -> {
                float[] floats2 = RotationUtil.getRotationArrayTo(
                    new Vec3d(ScriptValues.argDouble(objects3, 0, 0.0), ScriptValues.argDouble(objects3, 1, 0.0), ScriptValues.argDouble(objects3, 2, 0.0))
                );
                return wrapRotation(floats2[0], floats2[1]);
            }
        );
        ScriptValues.putFunction(nativeObject, "server", objects4 -> {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            return minecraftClient.player == null ? null : wrapRotation(minecraftClient.player.getYaw(), minecraftClient.player.getPitch());
        });
        ScriptValues.putFunction(
            nativeObject, "camera", objects5 -> MinecraftClient.getInstance().player == null ? null : wrapRotation(Rotation.getCameraYaw(), Rotation.getCameraPitch())
        );
        ScriptValues.putFunction(
            nativeObject,
            "delta",
            objects6 -> {
                MinecraftClient minecraftClient2 = MinecraftClient.getInstance();
                return minecraftClient2.player == null
                    ? 0.0F
                    : new Rotation(minecraftClient2.player).distanceTo(new Rotation(ScriptValues.argFloat(objects6, 0), ScriptValues.argFloat(objects6, 1)));
            }
        );
        ScriptValues.putFunction(nativeObject, "active", objects7 -> {
            RotationComponent rotationComponent = RotationComponent.getInstance();
            return rotationComponent != null && rotationComponent.isActive() ? true : false;
        });
        ScriptValues.putFunction(nativeObject, "stop", objects8 -> {
            RotationComponent rotationComponent2 = RotationComponent.getInstance();
            boolean flag = rotationComponent2 != null && lastRotation != null && rotationComponent2.getTargetRotation() == lastRotation;
            if (flag && rotationComponent2.getState() == RotationComponent.State.AIM) {
                rotationComponent2.setState(RotationComponent.State.RESET);
            }

            return null;
        });
        scriptableObject.put("rotation", scriptableObject, nativeObject);
    }

    private static void setRotation(float f, float f2, Object object) {
        if (MinecraftClient.getInstance().player != null) {
            float f3 = (float)ScriptValues.getNumber(object, "speed", 180.0);
            float f4 = (float)ScriptValues.getNumber(object, "returnSpeed", 12.0);
            Rotation rotation = new Rotation(f, Math.clamp(f2, -90.0F, 90.0F));
            RotationComponent.rotate(
                rotation,
                f3,
                (float)ScriptValues.getNumber(object, "pitchSpeed", f3),
                f4,
                (float)ScriptValues.getNumber(object, "pitchReturnSpeed", f4),
                (int)ScriptValues.getNumber(object, "timeout", 1.0),
                (int)ScriptValues.getNumber(object, "priority", 3.0),
                false
            );
            lastRotation = rotation;
        }
    }

    private static NativeObject wrapRotation(float f, float f2) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "yaw", f);
        ScriptValues.putValue(nativeObject, "pitch", f2);
        return nativeObject;
    }
}
