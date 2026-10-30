package tech.sadnes.client.feature.component;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easing;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.MouseLookEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PreRotationEvent;
import tech.sadnes.client.event.impl.Render3DEvent;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.render.FreeLook;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.TimerUtil;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.HumanizedRotation;
import tech.sadnes.client.util.player.MoveUtil;
import tech.sadnes.client.util.rotation.RotationUtil;
import tech.sadnes.client.util.rotation.SensitivityUtil;

public final class RotationComponent extends FeatureComponent {
    private RotationComponent.State state = RotationComponent.State.IDLE;
    private final Animation rotationAnimation = new Animation();
    private float aimYawSpeed;
    private float aimPitchSpeed;
    private float resetYawSpeed;
    private float resetPitchSpeed;
    private int priority;
    private int aimTicks;
    private int idleTicks;
    private Rotation targetRotation;
    private final TimerUtil speedTimer = new TimerUtil();
    private final TimerUtil resetTimer = new TimerUtil();
    private boolean jitterRotation;
    private boolean holyResetStarted;
    private boolean smoothJitter;
    private float jitterYaw;
    private float jitterPitch;
    private Easing easing = Easings.LINEAR;
    private RotationComponent.RotationMode mode = RotationComponent.RotationMode.DEFAULT;
    private boolean correctMovement = true;
    private RotationComponent.RotationSequence sequence;
    private final HumanizedRotation humanizedRotation = new HumanizedRotation();
    private Entity humanTarget;
    private Entity lastHumanTarget;
    private long lastFrameNanos;
    private float currentYawSpeed;

    public static RotationComponent getInstance() {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        return componentManager == null ? null : componentManager.getComponent(RotationComponent.class);
    }

    @EventHandler
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (this.isActive() && mc.player != null) {
            AttackAura attackAura = AttackAura.getInstance();
            if (attackAura != null && attackAura.isEnabled() && attackAura.getCurrentTarget() != null) {
                if (attackAura.getMoveCorrectionMode().is("subtle")) {
                    MoveUtil.fixMovement(moveInputEvent, getMovementYaw());
                    return;
                }
            } else if (!isFreeLookEnabled() && !isOverrideActive()) {
                MoveUtil.fixMovement(moveInputEvent, getMovementYaw());
            }
        }
    }

    private static float getMovementYaw() {
        return FreeLookComponent.isActive() ? MathHelper.wrapDegrees(FreeLookComponent.getYaw()) : MathHelper.wrapDegrees(Rotation.getCameraYaw());
    }

    public void tickReset() {
        if (this.sequence != null) {
            RotationComponent.SequenceStep sequenceStep = this.sequence.next();
            if (sequenceStep == null) {
                this.stop();
                return;
            }

            this.stepRotation(sequenceStep.rotation(), sequenceStep.yawSpeed(), sequenceStep.pitchSpeed());
        } else {
            float f = FreeLookComponent.getYaw();
            float f2 = FreeLookComponent.getPitch();
            Rotation rotation2 = new Rotation(f, f2);
            if (this.mode == RotationComponent.RotationMode.HUMAN) {
                return;
            }

            if (this.mode == RotationComponent.RotationMode.EASED) {
                if (this.stepEased(rotation2, this.resetYawSpeed * 1.0F, this.resetPitchSpeed * 1.0F, this.easing)) {
                    this.stop();
                    return;
                }
            } else if (this.smoothJitter) {
                float f3 = CombatUtil.randomFloat(2.0F, 2.4F) * 5.0F;
                boolean flag = this.stepRotation(rotation2.randomize(0.5F), f3, f3);
                if (flag && this.getJitterFade() <= 0.0F) {
                    this.stop();
                    return;
                }
            } else if (ServerManager.isHolyWorld()) {
                if (!this.holyResetStarted) {
                    this.resetTimer.setDelay(2500L);
                    this.holyResetStarted = true;
                }

                if (this.stepRotation(rotation2, this.resetYawSpeed * 1.0F, this.resetPitchSpeed * 1.0F)) {
                    this.stop();
                    return;
                }
            } else if (!ServerManager.isSpookyTime() && !ServerManager.isFunTime()) {
                if (this.stepRotation(rotation2, this.resetYawSpeed * 1.0F, this.resetPitchSpeed * 1.0F)) {
                    this.stop();
                    return;
                }
            } else {
                float f4 = CombatUtil.randomFloat(2.0F, 2.4F) * 5.0F;
                if (this.stepRotation(rotation2.randomize(0.5F), f4, f4)) {
                    this.stop();
                    return;
                }
            }
        }
    }

    private float getJitterFade() {
        long l = this.resetTimer.getElapsed();
        if (l <= 1000L) {
            return 1.0F;
        }

        if (l >= 6000L) {
            return 0.0F;
        }

        float f = (float)(l - 1000L) / 5000.0F;
        return 1.0F - f * f * (3.0F - 2.0F * f);
    }

    private void applyJitter() {
        this.jitterYaw = 0.0F;
        this.jitterPitch = 0.0F;
        if (this.smoothJitter) {
            float f = this.state == RotationComponent.State.RESET ? this.getJitterFade() : 1.0F;
            if (!(f <= 0.0F)) {
                AttackAura attackAura = AttackAura.getInstance();
                if (attackAura != null) {
                    double[] doubles = attackAura.computeAimJitter();
                    this.jitterYaw = SensitivityUtil.snapDelta((float)(doubles[0] * f));
                    this.jitterPitch = SensitivityUtil.snapDelta((float)(doubles[1] * f));
                    mc.player.setYaw(mc.player.getYaw() + this.jitterYaw);
                    mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + this.jitterPitch, -90.0F, 90.0F));
                }
            }
        }
    }

    @EventHandler
    public void onPreRotation(PreRotationEvent preRotationEvent) {
        if (this.state == RotationComponent.State.AIM && this.idleTicks > this.aimTicks) {
            this.state = RotationComponent.State.RESET;
            if (this.smoothJitter) {
                this.resetTimer.reset();
            }
        }

        if (this.state == RotationComponent.State.RESET) {
            this.tickReset();
        }

        this.idleTicks++;
    }

    public static void rotate(Rotation rotation, float f, float f2, float f3, float f4, int i, int i2, boolean flag) {
        RotationComponent rotationComponent = getInstance();
        if (rotationComponent != null && rotation != null) {
            if (rotationComponent.priority <= i2) {
                if (rotationComponent.state == RotationComponent.State.IDLE && !flag) {
                    FreeLookComponent.setActive(true);
                }

                rotationComponent.jitterRotation = false;
                rotationComponent.smoothJitter = false;
                rotationComponent.mode = RotationComponent.RotationMode.DEFAULT;
                rotationComponent.easing = Easings.LINEAR;
                rotationComponent.correctMovement = true;
                rotationComponent.aimYawSpeed = f;
                rotationComponent.aimPitchSpeed = f2;
                rotationComponent.resetYawSpeed = f3;
                rotationComponent.resetPitchSpeed = f4;
                rotationComponent.aimTicks = i;
                rotationComponent.priority = i2;
                rotationComponent.state = RotationComponent.State.AIM;
                rotationComponent.sequence = null;
                rotationComponent.targetRotation = rotation;
                rotationComponent.stepRotation(rotation, f, f2);
            }
        }
    }

    public static void rotateSequence(
        Rotation rotation, float f, float f2, float f3, float f4, int i, int i2, boolean flag, RotationComponent.RotationSequence rotationSequence
    ) {
        RotationComponent rotationComponent = getInstance();
        if (rotationComponent != null && rotation != null && rotationComponent.priority <= i2) {
            rotate(rotation, f, f2, f3, f4, i, i2, flag);
            rotationComponent.sequence = rotationSequence;
        }
    }

    public static void rotateSmooth(Rotation rotation, float f, float f2, float f3, float f4, int i, int i2) {
        RotationComponent rotationComponent = getInstance();
        if (rotationComponent != null && rotation != null) {
            if (rotationComponent.priority <= i2) {
                if (rotationComponent.state == RotationComponent.State.IDLE) {
                    FreeLookComponent.setActive(true);
                }

                rotationComponent.jitterRotation = false;
                rotationComponent.smoothJitter = true;
                rotationComponent.mode = RotationComponent.RotationMode.DEFAULT;
                rotationComponent.easing = Easings.LINEAR;
                rotationComponent.correctMovement = true;
                rotationComponent.aimYawSpeed = f;
                rotationComponent.aimPitchSpeed = f2;
                rotationComponent.resetYawSpeed = f3;
                rotationComponent.resetPitchSpeed = f4;
                rotationComponent.aimTicks = i;
                rotationComponent.priority = i2;
                rotationComponent.state = RotationComponent.State.AIM;
                rotationComponent.sequence = null;
                rotationComponent.targetRotation = rotation;
                rotationComponent.stepRotation(rotation, f, f2);
            }
        }
    }

    public static void rotateJitter(Rotation rotation, boolean flag, float f, float f2, float f3, float f4, int i, int i2, boolean flag2) {
        RotationComponent rotationComponent = getInstance();
        if (rotationComponent != null && rotation != null) {
            if (rotationComponent.priority <= i2) {
                if (rotationComponent.state == RotationComponent.State.IDLE && !flag2) {
                    FreeLookComponent.setActive(true);
                }

                rotationComponent.jitterRotation = true;
                rotationComponent.smoothJitter = false;
                rotationComponent.mode = RotationComponent.RotationMode.DEFAULT;
                rotationComponent.easing = Easings.LINEAR;
                rotationComponent.correctMovement = true;
                rotationComponent.aimYawSpeed = f;
                rotationComponent.aimPitchSpeed = f2;
                rotationComponent.resetYawSpeed = f3;
                rotationComponent.resetPitchSpeed = f4;
                rotationComponent.aimTicks = i;
                rotationComponent.priority = i2;
                rotationComponent.state = RotationComponent.State.AIM;
                rotationComponent.sequence = null;
                rotationComponent.targetRotation = rotation;
                rotationComponent.stepJitter(rotation, flag, f, f2);
            }
        }
    }

    public static void rotateEased(Rotation rotation, float f, float f2, float f3, float f4, int i, int i2, boolean flag, Easing easing2) {
        rotateEased(rotation, f, f2, f3, f4, i, i2, flag, easing2, true);
    }

    public static void rotateEased(Rotation rotation, float f, float f2, float f3, float f4, int i, int i2, boolean flag, Easing easing2, boolean flag2) {
        RotationComponent rotationComponent = getInstance();
        if (rotationComponent != null && rotation != null) {
            if (rotationComponent.priority <= i2) {
                if (rotationComponent.state == RotationComponent.State.IDLE && !flag) {
                    FreeLookComponent.setActive(true);
                }

                rotationComponent.jitterRotation = false;
                rotationComponent.smoothJitter = false;
                rotationComponent.mode = RotationComponent.RotationMode.EASED;
                rotationComponent.easing = easing2 == null ? Easings.LINEAR : easing2;
                rotationComponent.correctMovement = flag2;
                rotationComponent.aimYawSpeed = f;
                rotationComponent.aimPitchSpeed = f2;
                rotationComponent.resetYawSpeed = f3;
                rotationComponent.resetPitchSpeed = f4;
                rotationComponent.aimTicks = i;
                rotationComponent.priority = i2;
                rotationComponent.state = RotationComponent.State.AIM;
                rotationComponent.sequence = null;
                rotationComponent.targetRotation = rotation;
                rotationComponent.stepEased(rotation, f, f2, rotationComponent.easing);
            }
        }
    }

    public static void rotateHuman(Entity entity, int i, int i2) {
        RotationComponent rotationComponent = getInstance();
        if (rotationComponent != null && entity != null) {
            if (rotationComponent.priority <= i2) {
                if (rotationComponent.state == RotationComponent.State.IDLE) {
                    FreeLookComponent.setActive(true);
                }

                rotationComponent.jitterRotation = false;
                rotationComponent.smoothJitter = false;
                rotationComponent.mode = RotationComponent.RotationMode.HUMAN;
                rotationComponent.aimTicks = i;
                rotationComponent.priority = i2;
                rotationComponent.state = RotationComponent.State.AIM;
                rotationComponent.sequence = null;
                rotationComponent.humanTarget = entity;
                rotationComponent.idleTicks = 0;
            }
        }
    }

    @EventHandler(priority = 250)
    public void onMouseLook(MouseLookEvent mouseLookEvent) {
        if (this.mode == RotationComponent.RotationMode.HUMAN && this.isActive()) {
            this.humanizedRotation.addMouseDelta((float)mouseLookEvent.getDeltaX() * 0.15F, (float)mouseLookEvent.getDeltaY() * 0.15F);
        }
    }

    @EventHandler
    public void onAttack(AttackEntityEvent attackEntityEvent) {
        boolean flag = attackEntityEvent.getStage() == AttackEntityEvent.Stage.POST
            && this.mode == RotationComponent.RotationMode.HUMAN
            && this.state == RotationComponent.State.AIM
            && attackEntityEvent.getTarget() == this.humanTarget;
        if (flag) {
            this.humanizedRotation.triggerFlick();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent render3DEvent) {
        long l = System.nanoTime();
        float f = this.lastFrameNanos == 0L ? 0.0F : Math.min((float)(l - this.lastFrameNanos) / 1.0E9F, 0.05F);
        this.lastFrameNanos = l;
        if (this.mode == RotationComponent.RotationMode.HUMAN && this.isActive() && mc.player != null) {
            boolean flag;
            if (this.state == RotationComponent.State.AIM && this.humanTarget != null) {
                flag = this.humanTarget.isAlive();
            } else {
                flag = false;
            }

            Entity entity = flag ? this.humanTarget : null;
            if (entity != this.lastHumanTarget) {
                this.humanizedRotation.resetVelocity();
            }

            this.lastHumanTarget = entity;
            Rotation rotation = new Rotation(mc.player);
            Rotation rotation2;
            float f2;
            float f3;
            if (flag) {
                Vec3d vec3d = mc.player.getCameraPosVec(render3DEvent.getTickDelta());
                Vec3d vec3d2 = this.humanizedRotation.getAimPoint(this.humanTarget, render3DEvent.getTickDelta(), f);
                float[] floats = RotationUtil.getRotationArray(vec3d, vec3d2);
                rotation2 = new Rotation(floats[0], floats[1]);
                double d = Math.max(vec3d.distanceTo(vec3d2), 0.5);
                float f4 = (float)Math.toDegrees(Math.atan2(this.humanTarget.getBoundingBox().getLengthX() * 0.5, d));
                float f5 = (float)Math.toDegrees(Math.atan2(this.humanTarget.getBoundingBox().getLengthY() * 0.5, d));
                f2 = MathHelper.clamp(f4 * 0.75F, 0.6F, 7.0F);
                f3 = MathHelper.clamp(f5 * 0.45F, 1.0F, 9.0F);
            } else {
                rotation2 = new Rotation(FreeLookComponent.getYaw(), FreeLookComponent.getPitch());
                f2 = 0.4F;
                f3 = 0.4F;
                if (rotation.distanceTo(rotation2) < 1.0F && !this.humanizedRotation.isSaccadeActive()) {
                    this.stop();
                    return;
                }
            }

            float[] floats2 = this.humanizedRotation.computeRotationDelta(rotation, rotation2, f2, f3, f);
            mc.player.setYaw(mc.player.getYaw() + floats2[0]);
            mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + floats2[1], -90.0F, 90.0F));
        }
    }

    public static void rotateTo(Rotation rotation, float f, float f2, int i, int i2) {
        rotate(rotation, f, f, f2, f2, i, i2, false);
    }

    public static void rotateTo(Rotation rotation, float f, float f2, int i, int i2, Easing easing2) {
        rotateEased(rotation, f, f, f2, f2, i, i2, false, easing2);
    }

    public boolean stepRotation(Rotation rotation, float f, float f2) {
        if (this.jitterYaw != 0.0F || this.jitterPitch != 0.0F) {
            mc.player.setYaw(mc.player.getYaw() - this.jitterYaw);
            mc.player.setPitch(mc.player.getPitch() - this.jitterPitch);
        }

        Rotation rotation2 = new Rotation(mc.player);
        float f3 = MathHelper.wrapDegrees(rotation.getYaw() - rotation2.getYaw());
        float f4 = rotation.getPitch() - rotation2.getPitch();
        if (!this.smoothJitter && !this.resetTimer.isReached() && this.state == RotationComponent.State.RESET && shouldWobble()) {
            f3 += (float)(Math.cos(System.currentTimeMillis() / 45.0) * 4.0 + CombatUtil.RANDOM.nextGaussian());
            f4 += (float)(Math.cos(System.currentTimeMillis() / 45.0) * 3.0 + CombatUtil.RANDOM.nextGaussian());
        }

        float f5 = Math.min(Math.abs(f3), f + MathUtil.randomBetween(0.0F, 3.0F));
        float f6 = Math.min(Math.abs(f4), f2 + MathUtil.randomBetween(0.0F, 3.0F));
        float f7 = SensitivityUtil.snapDelta(MathHelper.clamp(f3, -f5, f5));
        float f8 = SensitivityUtil.snapDelta(MathHelper.clamp(f4, -f6, f6));
        mc.player.setYaw(mc.player.getYaw() + f7);
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + f8, -90.0F, 90.0F));
        if (new Rotation(mc.player).distanceTo(rotation) < 1.0F) {
            this.applyJitter();
            this.idleTicks = 0;
            return true;
        } else {
            this.applyJitter();
            this.idleTicks = 0;
            return false;
        }
    }

    public boolean stepEased(Rotation rotation, float f, float f2, Easing easing2) {
        Easing easing3 = easing2 == null ? Easings.LINEAR : easing2;
        mc.player.setYaw(this.easeAngle(mc.player.getYaw(), rotation.getYaw(), f, easing3));
        mc.player.setPitch(MathHelper.clamp(this.easeAngle(mc.player.getPitch(), rotation.getPitch(), f2, easing3), -90.0F, 90.0F));
        this.idleTicks = 0;
        return new Rotation(mc.player).distanceTo(rotation) < 1.0F;
    }

    private float easeAngle(float f, double d, float f2, Easing easing2) {
        float f3 = (float)MathHelper.wrapDegrees(d - f);
        float f4 = (float)MathHelper.clamp(easing2.ease(MathHelper.clamp(1.0F / Math.max(f2, 0.001F), 0.0F, 1.0F)), 0.0, 1.0);
        float f5 = Math.abs(f3) * f4 * Math.signum(f3);
        if (Math.abs(f5) > Math.abs(f3)) {
            f5 = f3;
        }

        if (f5 == 0.0F && f3 != 0.0F) {
            f5 = 0.01F * Math.signum(f3);
        }

        return f + SensitivityUtil.snapDelta(f5);
    }

    public boolean stepJitter(Rotation rotation, boolean flag, float f, float f2) {
        Rotation rotation2 = new Rotation(mc.player);
        AttackAura attackAura = AttackAura.getInstance();
        double d = 0.0;
        double d2 = 0.0;
        if (attackAura != null) {
            double[] doubles = attackAura.computeAimJitter();
            d = doubles[0];
            d2 = doubles[1];
        }

        if (!this.speedTimer.isReached()) {
            this.currentYawSpeed = CombatUtil.randomFloat(1.0F, 3.0F);
        } else {
            this.currentYawSpeed = f;
        }

        if (this.resetTimer.isReached() && this.state == RotationComponent.State.RESET) {
            d2 = 0.0;
            d = 0.0;
        }

        if (flag) {
            d = CombatUtil.randomDouble(-4.0, 4.0);
            d2 = CombatUtil.randomDouble(-4.0, 4.0);
        }

        float f3 = (float)(MathHelper.wrapDegrees(rotation.getYaw() - rotation2.getYaw()) + d);
        float f4 = (float)(rotation.getPitch() - rotation2.getPitch() + d2);
        float f5 = Math.min(Math.abs(f3), this.currentYawSpeed + MathUtil.randomBetween(0.0F, 2.0F));
        float f6 = Math.min(Math.abs(f4), f2 + MathUtil.randomBetween(0.0F, 2.0F));
        float f7 = SensitivityUtil.snapDelta(MathHelper.clamp(f3, -f5, f5));
        float f8 = SensitivityUtil.snapDelta(MathHelper.clamp(f4, -f6, f6));
        mc.player.setYaw(mc.player.getYaw() + f7);
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + f8, -90.0F, 90.0F));
        this.idleTicks = 0;
        return new Rotation(mc.player).distanceTo(rotation) < 1.0F;
    }

    public void stop() {
        this.state = RotationComponent.State.IDLE;
        this.priority = 0;
        this.targetRotation = null;
        this.holyResetStarted = false;
        this.smoothJitter = false;
        this.jitterYaw = 0.0F;
        this.jitterPitch = 0.0F;
        this.mode = RotationComponent.RotationMode.DEFAULT;
        this.easing = Easings.LINEAR;
        this.correctMovement = true;
        this.humanTarget = null;
        this.lastHumanTarget = null;
        this.humanizedRotation.reset();
        this.sequence = null;
        FreeLookComponent.setActive(false);
    }

    public boolean isActive() {
        return this.state != RotationComponent.State.IDLE;
    }

    private static boolean isFreeLookEnabled() {
        FreeLook freeLook = FreeLook.getInstance();
        return freeLook != null && freeLook.isEnabled();
    }

    private static boolean isOverrideActive() {
        return false;
    }

    private static boolean shouldWobble() {
        FreeLook freeLook = FreeLook.getInstance();
        return freeLook != null && freeLook.isEnabled() ? false : freeLook == null || freeLook.getReturnTimer().isStarted();
    }

    public RotationComponent.State getState() {
        return this.state;
    }

    public Animation getRotationAnimation() {
        return this.rotationAnimation;
    }

    public float getAimYawSpeed() {
        return this.aimYawSpeed;
    }

    public float getAimPitchSpeed() {
        return this.aimPitchSpeed;
    }

    public float getResetYawSpeed() {
        return this.resetYawSpeed;
    }

    public float getResetPitchSpeed() {
        return this.resetPitchSpeed;
    }

    public int getPriority() {
        return this.priority;
    }

    public int getAimTicks() {
        return this.aimTicks;
    }

    public int getIdleTicks() {
        return this.idleTicks;
    }

    public Rotation getTargetRotation() {
        return this.targetRotation;
    }

    public TimerUtil getSpeedTimer() {
        return this.speedTimer;
    }

    public TimerUtil getResetTimer() {
        return this.resetTimer;
    }

    public boolean isJitterRotation() {
        return this.jitterRotation;
    }

    public boolean isHolyResetStarted() {
        return this.holyResetStarted;
    }

    public boolean isSmoothJitter() {
        return this.smoothJitter;
    }

    public float getJitterYaw() {
        return this.jitterYaw;
    }

    public float getJitterPitch() {
        return this.jitterPitch;
    }

    public Easing getEasing() {
        return this.easing;
    }

    public RotationComponent.RotationMode getMode() {
        return this.mode;
    }

    public boolean isCorrectMovement() {
        return this.correctMovement;
    }

    public RotationComponent.RotationSequence getSequence() {
        return this.sequence;
    }

    public float getCurrentYawSpeed() {
        return this.currentYawSpeed;
    }

    public RotationComponent setState(RotationComponent.State state2) {
        this.state = state2;
        return this;
    }

    public RotationComponent setAimYawSpeed(float f) {
        this.aimYawSpeed = f;
        return this;
    }

    public RotationComponent setAimPitchSpeed(float f) {
        this.aimPitchSpeed = f;
        return this;
    }

    public RotationComponent setResetYawSpeed(float f) {
        this.resetYawSpeed = f;
        return this;
    }

    public RotationComponent setResetPitchSpeed(float f) {
        this.resetPitchSpeed = f;
        return this;
    }

    public RotationComponent setPriority(int i) {
        this.priority = i;
        return this;
    }

    public RotationComponent setAimTicks(int i) {
        this.aimTicks = i;
        return this;
    }

    public RotationComponent setIdleTicks(int i) {
        this.idleTicks = i;
        return this;
    }

    public RotationComponent setTargetRotation(Rotation rotation) {
        this.targetRotation = rotation;
        return this;
    }

    public RotationComponent setJitterRotation(boolean flag) {
        this.jitterRotation = flag;
        return this;
    }

    public RotationComponent setHolyResetStarted(boolean flag) {
        this.holyResetStarted = flag;
        return this;
    }

    public RotationComponent setSmoothJitter(boolean flag) {
        this.smoothJitter = flag;
        return this;
    }

    public RotationComponent setJitterYaw(float f) {
        this.jitterYaw = f;
        return this;
    }

    public RotationComponent setJitterPitch(float f) {
        this.jitterPitch = f;
        return this;
    }

    public RotationComponent setEasing(Easing easing2) {
        this.easing = easing2;
        return this;
    }

    public RotationComponent setMode(RotationComponent.RotationMode rotationMode) {
        this.mode = rotationMode;
        return this;
    }

    public RotationComponent setCorrectMovement(boolean flag) {
        this.correctMovement = flag;
        return this;
    }

    public RotationComponent setSequence(RotationComponent.RotationSequence rotationSequence) {
        this.sequence = rotationSequence;
        return this;
    }

    public RotationComponent setCurrentYawSpeed(float f) {
        this.currentYawSpeed = f;
        return this;
    }

    public enum RotationMode {
        DEFAULT,
        EASED,
        HUMAN;
    }

    @FunctionalInterface
    public interface RotationSequence {
        RotationComponent.SequenceStep next();
    }

    public record SequenceStep(Rotation rotation, float yawSpeed, float pitchSpeed) {
    }

    public enum State {
        AIM,
        RESET,
        IDLE;
    }
}
