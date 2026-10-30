package tech.sadnes.client.neuro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;

public final class NeuroRecorder {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final NeuroStorage storage;
    private final NeuroDebug debug;
    private NeuroDataset dataset;
    private boolean recording;
    private LivingEntity target;
    private float lastYaw;
    private float lastPitch;
    private float lastDeltaYaw;
    private float lastDeltaPitch;
    private Vec3d lastEyePos;
    private Vec3d lastTargetCenter;
    private int tickIndex;
    private int idleTicks;
    private int farTicks;
    private int lastSavedSampleCount;

    public NeuroRecorder(NeuroStorage neuroStorage, NeuroDebug neuroDebug) {
        this.storage = neuroStorage;
        this.debug = neuroDebug;
    }

    public void startRecording(String string) {
        this.dataset = this.storage.loadDataset(string);
        this.recording = true;
        this.target = null;
        this.lastSavedSampleCount = this.dataset.getSampleCount();
    }

    public void stopRecording() {
        if (this.recording && this.dataset != null) {
            this.recording = false;
            this.endEpisode();
            NeuroDataset neuroDataset = this.dataset;
            this.dataset = null;
            if (neuroDataset.getSampleCount() > 0) {
                this.storage.saveDatasetAsync(neuroDataset.copy());
            }
        }
    }

    public void cancelRecording() {
        this.recording = false;
        this.dataset = null;
        this.target = null;
    }

    public boolean isRecordingDataset(String string) {
        return this.recording && this.dataset != null && this.dataset.getName().equals(string);
    }

    public boolean isCapturing() {
        return this.recording && this.target != null;
    }

    public void tick() {
        if (this.recording && this.dataset != null && mc.player != null) {
            LivingEntity livingEntity;
            boolean flag;
            label81: {
                AttackAura attackAura = AttackAura.getInstance();
                livingEntity = attackAura != null && attackAura.isEnabled() ? attackAura.getCurrentTarget() : null;
                RotationComponent rotationComponent = RotationComponent.getInstance();
                boolean flag2;
                if (mc.currentScreen == null) {
                    if (rotationComponent != null) {
                        flag = !rotationComponent.isActive();
                        break label81;
                    }

                    flag2 = true;
                } else {
                    flag2 = false;
                }

                flag = flag2;
            }

            if (livingEntity != null && livingEntity.isAlive() && flag) {
                float f = mc.player.getYaw();
                float f2 = mc.player.getPitch();
                Vec3d vec3d = mc.player.getEyePos();
                Box box = livingEntity.getBoundingBox();
                Vec3d vec3d2 = box.getCenter();
                if (livingEntity != this.target) {
                    this.endEpisode();
                    this.dataset.beginSession();
                    this.target = livingEntity;
                    this.lastYaw = f;
                    this.lastPitch = f2;
                    this.lastDeltaYaw = 0.0F;
                    this.lastDeltaPitch = 0.0F;
                    this.lastEyePos = vec3d;
                    this.lastTargetCenter = vec3d2;
                    this.tickIndex = 0;
                    this.idleTicks = 0;
                    this.farTicks = 0;
                } else {
                    float f3 = MathHelper.wrapDegrees(f - this.lastYaw);
                    float f4 = f2 - this.lastPitch;
                    boolean flag3 = Math.abs(f3) > 90.0F || Math.abs(f4) > 60.0F;
                    boolean flag4 = Math.abs(f3) < 0.01F && Math.abs(f4) < 0.01F;
                    this.idleTicks = flag4 ? this.idleTicks + 1 : 0;
                    float[] floats = NeuroFeatures.calculateRotation(vec3d, vec3d2);
                    float f5 = (float)Math.hypot(MathHelper.wrapDegrees(floats[0] - f), floats[1] - f2);
                    this.farTicks = f5 > 60.0F ? this.farTicks + 1 : 0;
                    if (!flag3 && this.idleTicks <= 20 && this.farTicks <= 15) {
                        float[] floats2 = NeuroFeatures.buildFeatures(
                            vec3d,
                            this.lastEyePos,
                            vec3d2,
                            this.lastTargetCenter,
                            (box.maxX - box.minX) * 0.5,
                            (box.maxY - box.minY) * 0.5,
                            this.lastYaw,
                            this.lastPitch,
                            this.lastDeltaYaw,
                            this.lastDeltaPitch,
                            this.tickIndex
                        );
                        this.dataset.addSample(floats2, new float[]{f3, f4});
                        float f6 = MathHelper.wrapDegrees(floats[0] - this.lastYaw);
                        float f7 = floats[1] - this.lastPitch;
                        this.debug
                            .record(
                                livingEntity,
                                vec3d,
                                box,
                                vec3d2,
                                this.lastYaw,
                                this.lastPitch,
                                f6,
                                f7,
                                0.0F,
                                0.0F,
                                0.0F,
                                0.0F,
                                f3,
                                f4,
                                f,
                                f2,
                                (float)Math.hypot(f6, f7),
                                (float)vec3d.distanceTo(vec3d2),
                                0.0F,
                                1.0F,
                                0,
                                this.tickIndex == 0
                            );
                        this.lastDeltaYaw = f3;
                        this.lastDeltaPitch = f4;
                        this.lastYaw = f;
                        this.lastPitch = f2;
                        this.lastEyePos = vec3d;
                        this.lastTargetCenter = vec3d2;
                        this.tickIndex++;
                    } else {
                        this.endEpisode();
                    }
                }
            } else {
                this.endEpisode();
            }
        }
    }

    private void endEpisode() {
        if (this.target != null && this.dataset != null) {
            this.dataset.endSession();
            this.target = null;
            this.idleTicks = 0;
            this.farTicks = 0;
            int i = this.dataset.getSampleCount();
            if (i - this.lastSavedSampleCount >= 2000) {
                this.lastSavedSampleCount = i;
                this.storage.saveDatasetAsync(this.dataset.copy());
            }
        }
    }

    public NeuroDataset getDataset() {
        return this.dataset;
    }

    public boolean isRecording() {
        return this.recording;
    }
}
