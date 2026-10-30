package tech.sadnes.client.render.particle;


public final class ParticlePhysicsSettings {
    private ParticlePhysicsSettings.MotionMode motionMode = ParticlePhysicsSettings.MotionMode.NONE;
    private ParticlePhysicsSettings.CollisionMode collisionMode = ParticlePhysicsSettings.CollisionMode.BOUNCE;
    private boolean separation;
    private float gravity;
    private float speed = 1.0F;
    private float turnSpeed = 1.0F;
    private float minSize = 0.1F;
    private float maxSize = 0.5F;
    private boolean shrinkWithFade;
    private float attractDelay;
    private float attractSpeed;

    public ParticlePhysicsSettings.MotionMode getMotionMode() {
        return this.motionMode;
    }

    public ParticlePhysicsSettings.CollisionMode getCollisionMode() {
        return this.collisionMode;
    }

    public boolean isSeparation() {
        return this.separation;
    }

    public float getGravity() {
        return this.gravity;
    }

    public float getSpeed() {
        return this.speed;
    }

    public float getTurnSpeed() {
        return this.turnSpeed;
    }

    public float getMinSize() {
        return this.minSize;
    }

    public float getMaxSize() {
        return this.maxSize;
    }

    public boolean isShrinkWithFade() {
        return this.shrinkWithFade;
    }

    public float getAttractDelay() {
        return this.attractDelay;
    }

    public float getAttractSpeed() {
        return this.attractSpeed;
    }

    public void setMotionMode(ParticlePhysicsSettings.MotionMode motionMode2) {
        this.motionMode = motionMode2;
    }

    public void setCollisionMode(ParticlePhysicsSettings.CollisionMode collisionMode2) {
        this.collisionMode = collisionMode2;
    }

    public void setSeparation(boolean flag) {
        this.separation = flag;
    }

    public void setGravity(float f) {
        this.gravity = f;
    }

    public void setSpeed(float f) {
        this.speed = f;
    }

    public void setTurnSpeed(float f) {
        this.turnSpeed = f;
    }

    public void setMinSize(float f) {
        this.minSize = f;
    }

    public void setMaxSize(float f) {
        this.maxSize = f;
    }

    public void setShrinkWithFade(boolean flag) {
        this.shrinkWithFade = flag;
    }

    public void setAttractDelay(float f) {
        this.attractDelay = f;
    }

    public void setAttractSpeed(float f) {
        this.attractSpeed = f;
    }

    public enum CollisionMode {
        IGNORE,
        BOUNCE,
        STICK,
        DIE,
        SLIDE_SLOW;
    }

    public enum MotionMode {
        NONE,
        SWIRL_LEFT,
        SWIRL_RIGHT,
        SWIRL_RANDOM,
        TO_SOURCE,
        FROM_SOURCE,
        ALTERNATE;

        public float getTurnMultiplier() {
            switch (this) {
                case TO_SOURCE:
                case FROM_SOURCE:
                case ALTERNATE:
                    return 2.0F;
                default:
                    return 1.0F;
            }
        }
    }
}
