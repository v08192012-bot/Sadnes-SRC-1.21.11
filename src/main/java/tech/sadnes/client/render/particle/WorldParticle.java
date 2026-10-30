package tech.sadnes.client.render.particle;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.math.MathUtil;

public class WorldParticle implements IMinecraft {
    private static final float MAX_VELOCITY = 5.0F;
    private static final Mutable MUTABLE_POS = new Mutable();
    private final Texture texture;
    private final int color;
    private final ParticlePhysicsSettings settings;
    private final Supplier<Vec3d> sourceSupplier;
    private final int maxAge;
    private final float sizeFactor;
    private final int spinDirection;
    private final boolean verticalOnly;
    private double x;
    private double y;
    private double z;
    private double prevX;
    private double prevY;
    private double prevZ;
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private float yaw;
    private float pitch;
    private float rotation;
    private float prevRotation;
    private int age;
    private int prevAge;
    private boolean dead;
    private boolean grounded;

    public WorldParticle(
        Texture texture2, Vec3d vec3d, Supplier<Vec3d> supplier, float f, float f2, float f3, boolean flag, long l, int i, ParticlePhysicsSettings particlePhysicsSettings
    ) {
        this.texture = texture2;
        this.x = vec3d.x;
        this.y = vec3d.y;
        this.z = vec3d.z;
        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;
        this.sourceSupplier = supplier;
        this.yaw = f2;
        this.pitch = f3;
        this.verticalOnly = flag;
        this.color = i;
        this.settings = particlePhysicsSettings;
        this.maxAge = Math.max(1, (int)((float)l / 50.0F * MathUtil.randomBetween(0.9F, 1.1F)));
        this.sizeFactor = MathUtil.randomBetween(0.0F, 1.0F);
        this.spinDirection = MathUtil.randomBetween(0.0F, 1.0F) < 0.5F ? -1 : 1;
        this.rotation = MathUtil.randomBetween(-15.0F, 15.0F);
        this.prevRotation = this.rotation;
        float f4 = f <= 0.0F ? -f : MathUtil.randomBetween(f / 1.5F, f * 1.5F);
        double[] doubles = rotationToVelocity(f2, f3, f4);
        this.velocityX = flag ? 0.0 : doubles[0];
        this.velocityY = doubles[1];
        this.velocityZ = flag ? 0.0 : doubles[2];
    }

    public void tick(List<WorldParticle> worldParticles) {
        this.prevAge = this.age++;
        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;
        this.prevRotation = this.rotation;
        if (this.settings.getAttractDelay() > 0.0F) {
            this.tickAttraction();
        } else {
            float f = (float)Math.sqrt(this.velocityX * this.velocityX + this.velocityY * this.velocityY + this.velocityZ * this.velocityZ);
            this.rotation = this.rotation - 60.0F * this.spinDirection * f;
            ParticlePhysicsSettings.MotionMode motionMode = this.resolveMotionMode();
            float f2 = f / 5.0F * motionMode.getTurnMultiplier() * this.settings.getTurnSpeed();
            float f3 = f2 * 180.0F;
            float f4 = f2 * 90.0F;
            boolean flag = false;
            if (this.settings.isSeparation()) {
                float[] floats = this.findSeparationRotation(worldParticles);
                if (floats != null) {
                    this.yaw = floats[0];
                    this.pitch = floats[1];
                    flag = true;
                }
            }

            if (!flag) {
                this.applyMotionMode(motionMode, f3, f4);
            }

            double[] doubles = rotationToVelocity(this.yaw, this.pitch, f);
            this.velocityX = doubles[0];
            this.velocityY = doubles[1];
            this.velocityZ = doubles[2];
            if (this.verticalOnly && motionMode == ParticlePhysicsSettings.MotionMode.NONE) {
                this.velocityX = 0.0;
                this.velocityZ = 0.0;
            }

            float f5 = this.settings.getSpeed() * (flag ? 0.8F : 1.0F);
            this.velocityX *= f5;
            this.velocityY *= f5;
            this.velocityZ *= f5;
            float f6 = this.settings.getGravity();
            this.velocityY -= f6 / 17.5F;
            if (f6 > 0.0F && this.velocityY > 0.0) {
                this.velocityY /= 1.0F + f6 * 0.05F;
            }

            boolean flag2 = this.handleCollision(f6);
            this.velocityX = MathHelper.clamp(this.velocityX, -5.0, 5.0);
            this.velocityY = MathHelper.clamp(this.velocityY, -5.0, 5.0);
            this.velocityZ = MathHelper.clamp(this.velocityZ, -5.0, 5.0);
            if (!flag2) {
                float[] floats2 = velocityToRotation(this.velocityX, this.velocityY, this.velocityZ);
                this.yaw = floats2[0];
                this.pitch = floats2[1];
                this.x = this.x + this.velocityX;
                this.y = this.y + this.velocityY;
                this.z = this.z + this.velocityZ;
            }
        }
    }

    private void tickAttraction() {
        float f = Math.min((float)this.age / this.maxAge, 1.0F);
        float f2 = this.settings.getAttractDelay();
        if (!(f < f2)) {
            float f3 = (f - f2) / Math.max(1.0E-4F, 1.0F - f2);
            Vec3d vec3d = this.sourceSupplier.get();
            double d = this.x - vec3d.x;
            double d2 = this.y - vec3d.y;
            double d3 = this.z - vec3d.z;
            double d4 = Math.sqrt(d * d + d2 * d2 + d3 * d3);
            if (!(d4 < 1.0E-4)) {
                double d5 = this.settings.getAttractSpeed() * f3;
                this.x += d / d4 * d5;
                this.y += d2 / d4 * d5;
                this.z += d3 / d4 * d5;
            }
        }
    }

    public boolean isExpired() {
        return this.dead || this.age >= this.maxAge;
    }

    public float getProgress(float f) {
        return Math.min(MathUtil.lerp((float)this.prevAge, (float)this.age, f) / this.maxAge, 1.0F);
    }

    public int getFadedColor(float f) {
        float f2 = (float)Easings.QUINT_OUT.ease((f > 0.5F ? 1.0F - f : f) * 2.0F);
        return ColorUtil.multiplyAlpha(this.color, f2);
    }

    public float getSize() {
        return MathUtil.lerp(this.settings.getMinSize(), this.settings.getMaxSize(), this.sizeFactor);
    }

    public boolean shouldShrinkWithFade() {
        return this.settings.isShrinkWithFade();
    }

    public double getLerpedX(float f) {
        return MathUtil.lerp(this.prevX, this.x, f);
    }

    public double getLerpedY(float f) {
        return MathUtil.lerp(this.prevY, this.y, f);
    }

    public double getLerpedZ(float f) {
        return MathUtil.lerp(this.prevZ, this.z, f);
    }

    public float getLerpedRotation(float f) {
        return MathUtil.lerp(this.prevRotation, this.rotation, f);
    }

    private ParticlePhysicsSettings.MotionMode resolveMotionMode() {
        ParticlePhysicsSettings.MotionMode motionMode = this.settings.getMotionMode();
        if (motionMode != ParticlePhysicsSettings.MotionMode.ALTERNATE) {
            return motionMode;
        } else {
            return this.age < this.maxAge / 2.0F ? ParticlePhysicsSettings.MotionMode.TO_SOURCE : ParticlePhysicsSettings.MotionMode.FROM_SOURCE;
        }
    }

    private void applyMotionMode(ParticlePhysicsSettings.MotionMode motionMode, float f, float f2) {
        if (motionMode != ParticlePhysicsSettings.MotionMode.NONE) {
            float f3 = this.yaw;
            float f4 = this.pitch + f2 / 5.0F;
            switch (motionMode) {
                case SWIRL_LEFT:
                    f3 = this.yaw - f;
                    break;
                case SWIRL_RIGHT:
                    f3 = this.yaw + f;
                    break;
                case SWIRL_RANDOM:
                    f3 = this.yaw + f * this.spinDirection;
                    break;
                case TO_SOURCE:
                case FROM_SOURCE:
                    Vec3d vec3d = this.sourceSupplier.get();
                    float[] floats = motionMode == ParticlePhysicsSettings.MotionMode.TO_SOURCE
                        ? getRotationTowards(this.x, this.y, this.z, vec3d.x, vec3d.y, vec3d.z)
                        : getRotationTowards(vec3d.x, vec3d.y, vec3d.z, this.x, this.y, this.z);
                    f3 = floats[0];
                    f4 = floats[1];
            }

            this.yaw = approachAngle(this.yaw, f3, f);
            this.pitch = approachAngle(this.pitch, f4, f2);
        }
    }

    private float[] findSeparationRotation(List<WorldParticle> worldParticles) {
        double d = this.getSize() / 6.0F;
        if (d <= 0.0) {
            return null;
        }

        int i = 0;

        for (WorldParticle worldParticle : worldParticles) {
            if (++i > 60) {
                return null;
            }

            if (worldParticle != this) {
                double d2 = worldParticle.x - this.x;
                double d3 = worldParticle.y - this.y;
                double d4 = worldParticle.z - this.z;
                boolean flag = d2 * d2 + d3 * d3 + d4 * d4 < d;
                if (flag) {
                    return getRotationTowards(worldParticle.x, worldParticle.y, worldParticle.z, this.x, this.y, this.z);
                }
            }
        }

        return null;
    }

    private boolean handleCollision(float f) {
        ParticlePhysicsSettings.CollisionMode collisionMode = this.settings.getCollisionMode();
        if (collisionMode == ParticlePhysicsSettings.CollisionMode.IGNORE) {
            return false;
        }

        if (collisionMode != ParticlePhysicsSettings.CollisionMode.DIE && collisionMode != ParticlePhysicsSettings.CollisionMode.STICK) {
            boolean flag = collisionMode == ParticlePhysicsSettings.CollisionMode.BOUNCE;
            boolean flag2 = false;
            if (this.grounded || this.collidesY()) {
                if (this.velocityY != 0.0) {
                    this.velocityY = flag ? -this.velocityY / (Math.max(f, 0.0F) + 1.0F) * Math.min(Math.abs(this.velocityY) / 0.2, 1.0) : 0.0;
                    if (flag && this.velocityY > 0.0 && this.velocityY < 0.02) {
                        this.velocityY = 0.0;
                    }
                }

                if (!flag) {
                    this.velocityX *= 0.875F;
                    this.velocityZ *= 0.875F;
                }

                flag2 = true;
            }

            if (this.grounded || this.collidesX()) {
                this.velocityX = flag ? -this.velocityX : 0.0;
                if (!flag) {
                    this.velocityZ *= 0.875F;
                }

                flag2 = true;
            }

            if (this.grounded || this.collidesZ()) {
                this.velocityZ = flag ? -this.velocityZ : 0.0;
                if (!flag) {
                    this.velocityX *= 0.875F;
                }

                flag2 = true;
            }

            if (flag2) {
                double d = this.velocityX * this.velocityX + this.velocityY * this.velocityY + this.velocityZ * this.velocityZ;
                if (d < 6.4E-5) {
                    this.grounded = true;
                    return false;
                }

                this.grounded = false;
            }

            return false;
        } else {
            if (!this.grounded && !this.collidesX() && !this.collidesY() && !this.collidesZ()) {
                return false;
            }

            if (collisionMode == ParticlePhysicsSettings.CollisionMode.DIE) {
                this.dead = true;
            }

            this.velocityX = 0.0;
            this.velocityY = 0.0;
            this.velocityZ = 0.0;
            this.grounded = true;
            return true;
        }
    }

    private boolean collidesX() {
        return this.isSolidAt(this.x + this.velocityX, this.y, this.z);
    }

    private boolean collidesY() {
        return this.isSolidAt(this.x, this.y + this.velocityY, this.z);
    }

    private boolean collidesZ() {
        return this.isSolidAt(this.x, this.y, this.z + this.velocityZ);
    }

    private boolean isSolidAt(double d, double d2, double d3) {
        if (mc.world == null) {
            return false;
        }

        MUTABLE_POS.set(MathHelper.floor(d), MathHelper.floor(d2), MathHelper.floor(d3));
        return !mc.world.getBlockState(MUTABLE_POS).getCollisionShape(mc.world, MUTABLE_POS).isEmpty();
    }

    public static double[] rotationToVelocity(float f, float f2, double d) {
        double d2 = Math.toRadians(f);
        double d3 = Math.toRadians(f2);
        double d4 = Math.cos(d3);
        return new double[]{-d * d4 * Math.sin(d2), -d * Math.sin(d3), d * d4 * Math.cos(d2)};
    }

    public static float[] getRotationTowards(double d, double d2, double d3, double d4, double d5, double d6) {
        double d7 = d4 - d;
        double d8 = d5 - d2;
        double d9 = d6 - d3;
        double d10 = Math.sqrt(d7 * d7 + d9 * d9);
        float f = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(d9, d7)) - 90.0);
        float f2 = (float)(-Math.toDegrees(Math.atan2(d8, d10)));
        return new float[]{f, f2};
    }

    public static float[] velocityToRotation(double d, double d2, double d3) {
        double d4 = Math.sqrt(d * d + d3 * d3);
        float f = 0.0F;
        float f2 = 0.0F;
        if (d4 > 1.0E-6) {
            f = MathHelper.wrapDegrees((float)(Math.toDegrees(Math.atan2(d3, d)) - 90.0));
        }

        if (d4 > 1.0E-6 || Math.abs(d2) > 1.0E-6) {
            f2 = MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(d2, d4))), -90.0F, 90.0F);
        }

        return new float[]{f, f2};
    }

    private static float approachAngle(float f, float f2, float f3) {
        float f4 = MathHelper.wrapDegrees(f2 - f);
        float f5 = Math.copySign(Math.min(Math.abs(f4), f3), f4);
        return MathHelper.wrapDegrees(f + f5);
    }

    public Texture getTexture() {
        return this.texture;
    }
}
