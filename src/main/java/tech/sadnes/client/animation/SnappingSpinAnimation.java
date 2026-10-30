package tech.sadnes.client.animation;

import net.minecraft.util.math.MathHelper;

public final class SnappingSpinAnimation {
    private float angle;
    private float prevAngle;
    private int snapTicks = 4;
    private int prevSnapTicks = 4;

    public void tick(boolean flag, float f) {
        this.prevAngle = this.angle;
        this.prevSnapTicks = this.snapTicks;
        if (flag) {
            this.snapTicks = Math.min(this.snapTicks + 1, 4);
        } else {
            this.snapTicks = 0;
            this.angle = MathHelper.wrapDegrees(this.angle + f);
        }
    }

    public float getAngle(float f, boolean flag) {
        float f2 = MathHelper.lerpAngleDegrees(f, this.prevAngle, this.angle);
        float f3 = MathHelper.clamp(MathHelper.lerp(f, this.prevSnapTicks, this.snapTicks) / 4.0F, 0.0F, 1.0F);
        float f4 = flag ? Math.round((f2 - 90.0F) / 180.0F) * 180.0F + 90.0F : Math.round(f2 / 90.0F) * 90.0F;
        float f5 = f3 * f3 * (3.0F - 2.0F * f3);
        return MathHelper.lerp(f5, f2, f4);
    }
}
