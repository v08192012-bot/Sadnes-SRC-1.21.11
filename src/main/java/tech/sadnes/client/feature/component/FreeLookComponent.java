package tech.sadnes.client.feature.component;

import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.CameraRotationEvent;
import tech.sadnes.client.event.impl.MouseLookEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.feature.impl.misc.FreeCam;
import tech.sadnes.client.feature.impl.render.FreeLook;

public final class FreeLookComponent extends FeatureComponent {
    private static float yaw;
    private static float pitch;
    private static boolean active;
    private static boolean headActive;

    public static boolean isActive() {
        return active || headActive;
    }

    public static void setActive(boolean flag) {
        active = flag;
    }

    public static void setHeadActive(boolean flag) {
        headActive = flag;
    }

    public static void setYaw(float f) {
        yaw = f;
    }

    public static void setPitch(float f) {
        pitch = f;
    }

    @EventHandler
    public void onMouseMove(MouseLookEvent mouseLookEvent) {
        FreeCam freeCam = FreeCam.getInstance();
        if (freeCam == null || !freeCam.isEnabled()) {
            if (!headActive || !FreeLook.getInstance().getMode().is("head")) {
                if (isActive()) {
                    this.applyMouseDelta(mouseLookEvent.getDeltaX(), mouseLookEvent.getDeltaY());
                    mouseLookEvent.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onCameraRotation(CameraRotationEvent cameraRotationEvent) {
        FreeCam freeCam = FreeCam.getInstance();
        if (freeCam == null || !freeCam.isEnabled()) {
            if (isActive()) {
                cameraRotationEvent.setYaw(yaw);
                cameraRotationEvent.setPitch(pitch);
            } else {
                yaw = cameraRotationEvent.getYaw();
                pitch = cameraRotationEvent.getPitch();
            }
        }
    }

    private void applyMouseDelta(double d, double d2) {
        float f = (float)d2 * 0.15F;
        float f2 = (float)d * 0.15F;
        pitch = MathHelper.clamp(pitch + f, -90.0F, 90.0F);
        yaw += f2;
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getPitch() {
        return pitch;
    }
}
