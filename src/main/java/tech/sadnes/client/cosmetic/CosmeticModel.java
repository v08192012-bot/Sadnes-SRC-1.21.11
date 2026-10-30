package tech.sadnes.client.cosmetic;

import java.util.Map;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CosmeticModel {
    static final int QUAD_STRIDE = 23;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private final CosmeticModel.Bone[] bones;
    private final Map<CosmeticModel.Pose, CosmeticAnimation> animations;
    private final int frameCount;
    private final float frameTime;
    private final float frameHeight;
    private final float minY;
    private final Matrix4f[] poseMatrices;
    private final Matrix3f[] normalMatrices;
    private final float[] sampleBuffer = new float[3];
    private final Vector3f positionBuffer = new Vector3f();
    private final Vector3f normalBuffer = new Vector3f();

    CosmeticModel(CosmeticModel.Bone[] bones2, Map<CosmeticModel.Pose, CosmeticAnimation> map, int i, float f, float f2) {
        this.bones = bones2;
        this.animations = map;
        this.frameCount = i;
        this.frameTime = f;
        this.frameHeight = f2;
        this.poseMatrices = new Matrix4f[bones2.length];
        this.normalMatrices = new Matrix3f[bones2.length];
        int i2 = bones2.length;

        for (int i3 = 0; i3 < i2; i3++) {
            this.poseMatrices[i3] = new Matrix4f();
            this.normalMatrices[i3] = new Matrix3f();
        }

        this.minY = this.computeMinY();
    }

    private float computeMinY() {
        Matrix4f[] matrix4fs = new Matrix4f[this.bones.length];
        Vector3f vector3f = new Vector3f();
        float f = Float.MAX_VALUE;
        int i = this.bones.length;

        for (int i2 = 0; i2 < i; i2++) {
            CosmeticModel.Bone bone = this.bones[i2];
            matrix4fs[i2] = new Matrix4f(bone.parent() < 0 ? new Matrix4f() : matrix4fs[bone.parent()])
                .translate(bone.pivotX(), bone.pivotY(), bone.pivotZ())
                .rotateZYX(bone.roll(), bone.yaw(), bone.pitch())
                .translate(-bone.pivotX(), -bone.pivotY(), -bone.pivotZ());
            float[] floats = bone.quads();
            int i3 = floats.length;

            for (byte b = 0; b < i3; b += 23) {
                int i4 = b + 3;

                for (int i5 = b + 23; i4 < i5; i4 += 5) {
                    matrix4fs[i2].transformPosition(floats[i4], floats[i4 + 1], floats[i4 + 2], vector3f);
                    f = Math.min(f, vector3f.y);
                }
            }
        }

        return f == Float.MAX_VALUE ? 0.0F : f;
    }

    int getQuadCount() {
        int i = 0;

        for (CosmeticModel.Bone bone : this.bones) {
            i += bone.quads().length / 23;
        }

        return i;
    }

    public void render(Entry entry, VertexConsumer vertexConsumer, CosmeticModel.Pose pose, double d, int i, int i2, int i3) {
        CosmeticAnimation cosmeticAnimation = this.animations.getOrDefault(pose, this.animations.get(CosmeticModel.Pose.IDLE));
        float f = cosmeticAnimation == null ? 0.0F : cosmeticAnimation.getTime(d);
        float f2 = this.frameCount > 1 ? (int)(d * 20.0 / this.frameTime) % this.frameCount * this.frameHeight : 0.0F;
        int i4 = this.bones.length;

        for (int i5 = 0; i5 < i4; i5++) {
            CosmeticModel.Bone bone = this.bones[i5];
            boolean flag = bone.parent() < 0;
            Matrix4f matrix4f = this.poseMatrices[i5].set(flag ? entry.getPositionMatrix() : this.poseMatrices[bone.parent()]);
            Matrix3f matrix3f = this.normalMatrices[i5].set(flag ? entry.getNormalMatrix() : this.normalMatrices[bone.parent()]);
            float f3 = bone.pivotX();
            float f4 = bone.pivotY();
            float f5 = bone.pivotZ();
            float f6 = bone.pitch();
            float f7 = bone.yaw();
            float f8 = bone.roll();
            CosmeticAnimation.Track track = null;
            if (cosmeticAnimation != null) {
                CosmeticAnimation.Track track2 = cosmeticAnimation.getRotationTrack(i5);
                if (track2 != null) {
                    track2.sample(f, this.sampleBuffer);
                    f6 += this.sampleBuffer[0] * (float) (Math.PI / 180.0);
                    f7 += this.sampleBuffer[1] * (float) (Math.PI / 180.0);
                    f8 += this.sampleBuffer[2] * (float) (Math.PI / 180.0);
                }

                CosmeticAnimation.Track track3 = cosmeticAnimation.getPositionTrack(i5);
                if (track3 != null) {
                    track3.sample(f, this.sampleBuffer);
                    f3 += this.sampleBuffer[0] / 16.0F;
                    f4 -= this.sampleBuffer[1] / 16.0F;
                    f5 += this.sampleBuffer[2] / 16.0F;
                }

                track = cosmeticAnimation.getScaleTrack(i5);
            }

            matrix4f.translate(f3, f4, f5);
            if (f6 != 0.0F || f7 != 0.0F || f8 != 0.0F) {
                matrix4f.rotateZYX(f8, f7, f6);
                matrix3f.rotateZYX(f8, f7, f6);
            }

            if (track != null) {
                track.sample(f, this.sampleBuffer);
                matrix4f.scale(this.sampleBuffer[0], this.sampleBuffer[1], this.sampleBuffer[2]);
            }

            matrix4f.translate(-bone.pivotX(), -bone.pivotY(), -bone.pivotZ());
            float[] floats = bone.quads();
            int i6 = floats.length;

            for (byte b = 0; b < i6; b += 23) {
                matrix3f.transform(floats[b], floats[b + 1], floats[b + 2], this.normalBuffer);
                int i7 = b + 3;

                for (int i8 = b + 23; i7 < i8; i7 += 5) {
                    matrix4f.transformPosition(floats[i7], floats[i7 + 1], floats[i7 + 2], this.positionBuffer);
                    vertexConsumer.vertex(
                        this.positionBuffer.x,
                        this.positionBuffer.y,
                        this.positionBuffer.z,
                        i3,
                        floats[i7 + 3],
                        floats[i7 + 4] + f2,
                        i2,
                        i,
                        this.normalBuffer.x,
                        this.normalBuffer.y,
                        this.normalBuffer.z
                    );
                }
            }
        }
    }

    public float getMinY() {
        return this.minY;
    }

    record Bone(int parent, float pivotX, float pivotY, float pivotZ, float pitch, float yaw, float roll, float[] quads) {
    }

    public enum Pose {
        IDLE,
        MOVE,
        SNEAK,
        SWIM,
        GLIDE;
    }
}
