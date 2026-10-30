package tech.sadnes.client.util.math;

import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.joml.Vector2f;
import tech.sadnes.client.animation.PerspectiveAnimation;
import tech.sadnes.client.util.IMinecraft;

public class Rotation implements IMinecraft {
    private static float lastPlayerYaw;
    private static float lastPlayerPitch;
    private static boolean trackingInitialized;
    private float yaw;
    private float pitch;

    public Rotation() {
    }

    public Rotation(float f, float f2) {
        this.yaw = f;
        this.pitch = f2;
    }

    public Rotation(Entity entity) {
        this(entity.getYaw(), entity.getPitch());
    }

    public Rotation(Vector2f vector2f) {
        this(vector2f.x, vector2f.y);
    }

    public Rotation(Vec2f vec2f) {
        this(vec2f.x, vec2f.y);
    }

    public float distanceTo(Rotation rotation) {
        float f = MathHelper.wrapDegrees(rotation.yaw - this.yaw);
        float f2 = rotation.pitch - this.pitch;
        return (float)Math.hypot(Math.abs(f), Math.abs(f2));
    }

    public double wrappedDistanceTo(Rotation rotation) {
        double d = MathHelper.wrapDegrees(rotation.yaw - this.yaw);
        double d2 = MathHelper.wrapDegrees(rotation.pitch - this.pitch);
        return Math.hypot(d, d2);
    }

    public static Vector2f getCameraRotation() {
        return new Vector2f(getCameraYaw(), getCameraPitch());
    }

    public static float getCameraYaw() {
        PerspectiveAnimation perspectiveAnimation = PerspectiveAnimation.INSTANCE;
        if (perspectiveAnimation.hasCameraRotation()) {
            return MathHelper.wrapDegrees(perspectiveAnimation.getCameraYaw());
        }

        float f = mc.gameRenderer.getCamera().getYaw();
        return mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT ? MathHelper.wrapDegrees(f + 180) : MathHelper.wrapDegrees(f + 0);
    }

    public static float getCameraPitch() {
        PerspectiveAnimation perspectiveAnimation = PerspectiveAnimation.INSTANCE;
        if (perspectiveAnimation.hasCameraRotation()) {
            return perspectiveAnimation.getCameraPitch();
        }

        float f = mc.gameRenderer.getCamera().getPitch();
        return mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT ? f * -1 : f * 1;
    }

    public Rotation add(float f, float f2) {
        return new Rotation(this.yaw + f, this.pitch + f2);
    }

    public Rotation add(Vector2f vector2f) {
        return this.add(vector2f.x, vector2f.y);
    }

    public Rotation randomize(float f) {
        return this.randomize(f, f);
    }

    public Rotation randomize(float f, float f2) {
        float f3 = (float)(Math.random() * f * 2.0 - f);
        float f4 = (float)(Math.random() * f2 * 2.0 - f2);
        return new Rotation(this.yaw + f3, this.pitch + f4);
    }

    public Rotation multiply(float f, float f2) {
        return new Rotation(this.yaw * f, this.pitch * f2);
    }

    public static boolean hasPlayerRotated() {
        if (mc.player == null) {
            trackingInitialized = false;
            return false;
        } else {
            float f = mc.player.getYaw();
            float f2 = mc.player.getPitch();
            if (!trackingInitialized) {
                lastPlayerYaw = f;
                lastPlayerPitch = f2;
                trackingInitialized = true;
                return false;
            } else if (!(Math.abs(f - lastPlayerYaw) > 0.001F) && !(Math.abs(f2 - lastPlayerPitch) > 0.001F)) {
                lastPlayerYaw = f;
                lastPlayerPitch = f2;
                return false;
            } else {
                lastPlayerYaw = f;
                lastPlayerPitch = f2;
                return true;
            }
        }
    }

    public void setYaw(float f) {
        this.yaw = f;
    }

    public void setPitch(float f) {
        this.pitch = f;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }
}
