package tech.sadnes.client.util.rotation;

import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PreRotationEvent;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.FeatureComponent;

public final class RotationAnimator extends FeatureComponent {
    private float startYaw;
    private float startPitch;
    private float yawDelta;
    private float pitchDelta;
    private int progressTicks;
    private int durationTicks;

    public static void animate(float f, float f2, int i) {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        if (componentManager != null) {
            RotationAnimator rotationAnimator = componentManager.getComponent(RotationAnimator.class);
            if (rotationAnimator != null) {
                rotationAnimator.begin(f, f2, i);
            }
        }
    }

    private void begin(float f, float f2, int i) {
        if (mc.player != null && i > 0) {
            this.startYaw = mc.player.getYaw();
            this.startPitch = mc.player.getPitch();
            this.yawDelta = f;
            this.pitchDelta = f2;
            this.progressTicks = 0;
            this.durationTicks = i;
        }
    }

    @EventHandler
    public void onUpdate(PreRotationEvent preRotationEvent) {
        if (this.durationTicks > 0 && mc.player != null) {
            this.progressTicks++;
            float f = 0.5F - 0.5F * MathHelper.cos((float) Math.PI * this.progressTicks / this.durationTicks);
            mc.player.setYaw(this.startYaw + this.yawDelta * f);
            mc.player.setPitch(MathHelper.clamp(this.startPitch + this.pitchDelta * f, -90.0F, 90.0F));
            if (this.progressTicks >= this.durationTicks) {
                this.durationTicks = 0;
            }
        }
    }
}
