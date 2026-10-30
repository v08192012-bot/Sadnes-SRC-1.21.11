package tech.sadnes.client.render;

import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.util.math.MathUtil;

public class GuiParticle {
    private final Texture texture;
    private final int color;
    private final long spawnTime;
    private final float size;
    private final long lifetime;
    private final long fadeIn;
    private final long fadeOut;
    private final float gravity;
    private final float friction;
    private final float spin;
    private float originX;
    private float originY;
    private float offsetX;
    private float offsetY;
    private float motionX;
    private float motionY;
    private float rotation;

    public void update(float f) {
        this.motionY = this.motionY + this.gravity * f;
        if (this.friction > 0.0F) {
            float f2 = Math.max(0.0F, 1.0F - this.friction * f);
            this.motionX *= f2;
            this.motionY *= f2;
        }

        this.offsetX = this.offsetX + this.motionX * f;
        this.offsetY = this.offsetY + this.motionY * f;
        this.rotation = this.rotation + this.spin * f;
    }

    public void setOrigin(float f, float f2) {
        this.originX = f;
        this.originY = f2;
    }

    public void setMotion(float f, float f2) {
        this.motionX = f;
        this.motionY = f2;
    }

    public void move(float f, float f2) {
        this.offsetX += f;
        this.offsetY += f2;
    }

    public void setX(float f) {
        this.offsetX = f - this.originX;
    }

    public void setY(float f) {
        this.offsetY = f - this.originY;
    }

    public float getX() {
        return this.originX + this.offsetX;
    }

    public float getY() {
        return this.originY + this.offsetY;
    }

    public float getAlpha() {
        long l = System.currentTimeMillis() - this.spawnTime;
        if (l >= this.lifetime) {
            return 0.0F;
        }

        if (l < this.fadeIn) {
            return (float)Easings.QUAD_OUT.ease((double)l / this.fadeIn);
        }

        long l2 = this.lifetime - l;
        return l2 < this.fadeOut ? (float)Easings.QUAD_OUT.ease((double)l2 / this.fadeOut) : 1.0F;
    }

    public boolean isDead() {
        return System.currentTimeMillis() - this.spawnTime >= this.lifetime;
    }

    static long defaultSpawnTime() {
        return System.currentTimeMillis();
    }

    static float defaultSize() {
        return 3.0F;
    }

    static long defaultLifetime() {
        return 1500L;
    }

    static long defaultFadeIn() {
        return 150L;
    }

    static long defaultFadeOut() {
        return 600L;
    }

    static float defaultGravity() {
        return 0.0F;
    }

    static float defaultFriction() {
        return 0.0F;
    }

    static float defaultSpin() {
        return 0.0F;
    }

    GuiParticle(
        Texture texture2,
        int i,
        long l,
        float f,
        long l2,
        long l3,
        long l4,
        float f2,
        float f3,
        float f4,
        float f5,
        float f6,
        float f7,
        float f8,
        float f9,
        float f10,
        float f11
    ) {
        this.texture = texture2;
        this.color = i;
        this.spawnTime = l;
        this.size = f;
        this.lifetime = l2;
        this.fadeIn = l3;
        this.fadeOut = l4;
        this.gravity = f2;
        this.friction = f3;
        this.spin = f4;
        this.originX = f5;
        this.originY = f6;
        this.offsetX = f7;
        this.offsetY = f8;
        this.motionX = f9;
        this.motionY = f10;
        this.rotation = f11;
    }

    public static GuiParticle.Builder builder() {
        return new GuiParticle.Builder();
    }

    public Texture getTexture() {
        return this.texture;
    }

    public int getColor() {
        return this.color;
    }

    public long getSpawnTime() {
        return this.spawnTime;
    }

    public float getSize() {
        return this.size;
    }

    public long getLifetime() {
        return this.lifetime;
    }

    public long getFadeIn() {
        return this.fadeIn;
    }

    public long getFadeOut() {
        return this.fadeOut;
    }

    public float getGravity() {
        return this.gravity;
    }

    public float getFriction() {
        return this.friction;
    }

    public float getSpin() {
        return this.spin;
    }

    public float getOriginX() {
        return this.originX;
    }

    public float getOriginY() {
        return this.originY;
    }

    public float getOffsetX() {
        return this.offsetX;
    }

    public float getOffsetY() {
        return this.offsetY;
    }

    public float getMotionX() {
        return this.motionX;
    }

    public float getMotionY() {
        return this.motionY;
    }

    public float getRotation() {
        return this.rotation;
    }

    public static class Builder {
        private Texture texture;
        private int color;
        private boolean spawnTimeSet;
        private long spawnTimeValue;
        private boolean sizeSet;
        private float sizeValue;
        private boolean lifetimeSet;
        private long lifetimeValue;
        private boolean fadeInSet;
        private long fadeInValue;
        private boolean fadeOutSet;
        private long fadeOutValue;
        private boolean gravitySet;
        private float gravityValue;
        private boolean frictionSet;
        private float frictionValue;
        private boolean spinSet;
        private float spinValue;
        private float originX;
        private float originY;
        private float offsetX;
        private float offsetY;
        private float motionX;
        private float motionY;
        private float rotation;

        public GuiParticle.Builder position(float f, float f2) {
            this.originX = f;
            this.originY = f2;
            return this;
        }

        public GuiParticle.Builder randomVelocity(float f, float f2) {
            float f3 = (float)Math.toRadians(MathUtil.randomBetween(0.0F, 360.0F));
            float f4 = MathUtil.randomBetween(f, f2);
            this.motionX = (float)Math.cos(f3) * f4;
            this.motionY = (float)Math.sin(f3) * f4;
            return this;
        }

        Builder() {
        }

        public GuiParticle.Builder texture(Texture texture2) {
            this.texture = texture2;
            return this;
        }

        public GuiParticle.Builder color(int i) {
            this.color = i;
            return this;
        }

        public GuiParticle.Builder spawnTime(long l) {
            this.spawnTimeValue = l;
            this.spawnTimeSet = true;
            return this;
        }

        public GuiParticle.Builder size(float f) {
            this.sizeValue = f;
            this.sizeSet = true;
            return this;
        }

        public GuiParticle.Builder lifetime(long l) {
            this.lifetimeValue = l;
            this.lifetimeSet = true;
            return this;
        }

        public GuiParticle.Builder fadeIn(long l) {
            this.fadeInValue = l;
            this.fadeInSet = true;
            return this;
        }

        public GuiParticle.Builder fadeOut(long l) {
            this.fadeOutValue = l;
            this.fadeOutSet = true;
            return this;
        }

        public GuiParticle.Builder gravity(float f) {
            this.gravityValue = f;
            this.gravitySet = true;
            return this;
        }

        public GuiParticle.Builder friction(float f) {
            this.frictionValue = f;
            this.frictionSet = true;
            return this;
        }

        public GuiParticle.Builder spin(float f) {
            this.spinValue = f;
            this.spinSet = true;
            return this;
        }

        public GuiParticle.Builder originX(float f) {
            this.originX = f;
            return this;
        }

        public GuiParticle.Builder originY(float f) {
            this.originY = f;
            return this;
        }

        public GuiParticle.Builder offsetX(float f) {
            this.offsetX = f;
            return this;
        }

        public GuiParticle.Builder offsetY(float f) {
            this.offsetY = f;
            return this;
        }

        public GuiParticle.Builder motionX(float f) {
            this.motionX = f;
            return this;
        }

        public GuiParticle.Builder motionY(float f) {
            this.motionY = f;
            return this;
        }

        public GuiParticle.Builder rotation(float f) {
            this.rotation = f;
            return this;
        }

        public GuiParticle build() {
            long l = this.spawnTimeValue;
            if (!this.spawnTimeSet) {
                l = GuiParticle.defaultSpawnTime();
            }

            float f = this.sizeValue;
            if (!this.sizeSet) {
                f = GuiParticle.defaultSize();
            }

            long l2 = this.lifetimeValue;
            if (!this.lifetimeSet) {
                l2 = GuiParticle.defaultLifetime();
            }

            long l3 = this.fadeInValue;
            if (!this.fadeInSet) {
                l3 = GuiParticle.defaultFadeIn();
            }

            long l4 = this.fadeOutValue;
            if (!this.fadeOutSet) {
                l4 = GuiParticle.defaultFadeOut();
            }

            float f2 = this.gravityValue;
            if (!this.gravitySet) {
                f2 = GuiParticle.defaultGravity();
            }

            float f3 = this.frictionValue;
            if (!this.frictionSet) {
                f3 = GuiParticle.defaultFriction();
            }

            float f4 = this.spinValue;
            if (!this.spinSet) {
                f4 = GuiParticle.defaultSpin();
            }

            return new GuiParticle(
                this.texture,
                this.color,
                l,
                f,
                l2,
                l3,
                l4,
                f2,
                f3,
                f4,
                this.originX,
                this.originY,
                this.offsetX,
                this.offsetY,
                this.motionX,
                this.motionY,
                this.rotation
            );
        }

        @Override
        public String toString() {
            return "Particle2D.Particle2DBuilder(texture="
                + this.texture
                + ", color="
                + this.color
                + ", spawnTime$value="
                + this.spawnTimeValue
                + ", size$value="
                + this.sizeValue
                + ", lifespanMs$value="
                + this.lifetimeValue
                + ", fadeInMs$value="
                + this.fadeInValue
                + ", fadeOutMs$value="
                + this.fadeOutValue
                + ", gravity$value="
                + this.gravityValue
                + ", friction$value="
                + this.frictionValue
                + ", rotationSpeed$value="
                + this.spinValue
                + ", anchorX="
                + this.originX
                + ", anchorY="
                + this.originY
                + ", offsetX="
                + this.offsetX
                + ", offsetY="
                + this.offsetY
                + ", motionX="
                + this.motionX
                + ", motionY="
                + this.motionY
                + ", rotation="
                + this.rotation
                + ")";
        }
    }
}
