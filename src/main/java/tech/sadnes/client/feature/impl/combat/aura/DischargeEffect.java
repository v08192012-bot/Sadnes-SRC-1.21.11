package tech.sadnes.client.feature.impl.combat.aura;

import net.minecraft.entity.LivingEntity;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.render.shader.DischargeRenderer;
import tech.sadnes.client.util.EntityModelBoundsUtil;
import tech.sadnes.client.util.color.ColorUtil;

final class DischargeEffect {
    private final Animation visibilityAnimation = new Animation();
    private long lastFrameNanos;
    private LivingEntity target;
    private float rotation;
    private int trackedEntityId = -1;
    private int lastHurtTime;
    private int deadEntityId = -1;
    private float pulse;

    void render(RenderEvent renderEvent, AttackAura attackAura, LivingEntity livingEntity) {
        long l = System.nanoTime();
        float f = this.lastFrameNanos == 0L ? 0.016666668F : Math.min((float)(l - this.lastFrameNanos) / 1.0E9F, 0.05F);
        this.lastFrameNanos = l;
        this.visibilityAnimation.update();
        this.visibilityAnimation.animate(livingEntity == null ? 0.0 : 1.0, 0.35F, Easings.LINEAR);
        if (livingEntity != null && livingEntity != this.target) {
            this.target = livingEntity;
            this.rotation = 0.0F;
            this.pulse = 0.0F;
        } else if (livingEntity == null && this.visibilityAnimation.get() <= 0.0F) {
            this.target = null;
            this.trackedEntityId = -1;
        }

        if (this.target != null && !this.target.isRemoved()) {
            int i = this.target.hurtTime;
            if (this.trackedEntityId != this.target.getId()) {
                this.trackedEntityId = this.target.getId();
                this.lastHurtTime = i;
            }

            if (i > this.lastHurtTime) {
                this.pulse = 1.0F;
            }

            this.lastHurtTime = i;
            if (this.target.isDead() && this.deadEntityId != this.target.getId()) {
                this.deadEntityId = this.target.getId();
                this.pulse = 1.0F;
            }

            this.rotation = (this.rotation + f * (float) (Math.PI * 5.0 / 6.0) * (1.0F + 2.0F * this.pulse)) % (float) (Math.PI * 2);
            this.pulse = Math.max(0.0F, this.pulse - f * 3.2F);
            float f2 = this.visibilityAnimation.get();
            if (!(f2 <= 0.01F)) {
                float f3 = renderEvent.getTickDelta();
                EntityModelBoundsUtil.Bounds bounds = EntityModelBoundsUtil.computeBounds(this.target, f3);
                float f4 = bounds == null ? 0.0F : bounds.bottom();
                float f5 = bounds == null ? this.target.getHeight() : bounds.top();
                float f6 = Math.min(0.12F, (f5 - f4) * 0.2F);
                DischargeRenderer.INSTANCE
                    .render(
                        renderEvent.getMatrices(),
                        renderEvent.getCamera(),
                        f3,
                        this.target,
                        ColorUtil.multiplyAlpha(attackAura.getEspColorValue(), f2),
                        this.pulse,
                        f4 + f6,
                        f5 - f6,
                        this.rotation
                    );
            }
        }
    }

    void reset() {
        this.visibilityAnimation.forceValue(0.0);
        this.target = null;
        this.trackedEntityId = -1;
        this.pulse = 0.0F;
        DischargeRenderer.INSTANCE.reset();
    }
}
