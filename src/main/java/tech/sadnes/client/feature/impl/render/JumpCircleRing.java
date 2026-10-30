package tech.sadnes.client.feature.impl.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShape;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.math.MathUtil;

final class JumpCircleRing implements IMinecraft {
    private static final double GRAVITY = 0.04;
    private static final float DAMPING = 0.98F;
    private static final int MIN_ITERATIONS = 6;
    private static final double MAX_VELOCITY = 0.35;
    private static final double RELAX_FACTOR = 0.35;
    private static final double ANCHOR_PULL = 0.35;
    private static final int MAX_STEP_DOWN = 2;
    private static final double PROJECT_STEP = 0.08;
    private static final Mutable MUTABLE_POS = new Mutable();
    private final JumpCircleRing.ClothPoint[] points;
    private final double[] relaxedY;
    private final int iterations;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private int age;
    private int prevAge;

    JumpCircleRing(float[] floats, double d, double d2, double d3, float f, float f2, double d4) {
        this.centerX = d;
        this.centerY = d2;
        this.centerZ = d3;
        float f3 = f2 * (float) (Math.PI / 180.0);
        float f4 = MathHelper.sin(f3);
        float f5 = MathHelper.cos(f3);
        float f6 = -f4;
        float f7 = -f5;
        float f8 = -f4;
        int i = floats.length / 2;
        this.points = new JumpCircleRing.ClothPoint[i];
        this.relaxedY = new double[i];
        float f9 = i / 56.0F;
        this.iterations = Math.min(24, Math.max(6, Math.round(6.0F * f9 * f9)));
        double[] doubles = new double[3];

        for (int i2 = 0; i2 < i; i2++) {
            float f10 = floats[i2 * 2] * f;
            float f11 = floats[i2 * 2 + 1] * f;
            double d5 = f7 * f10 + f6 * f11;
            double d6 = f8 * f10 + f5 * f11;
            double d7 = Math.sqrt(d5 * d5 + d6 * d6);
            this.projectOnTerrain(d5, d6, d7, doubles);
            JumpCircleRing.ClothPoint clothPoint = new JumpCircleRing.ClothPoint(doubles[0], doubles[1] + d4, doubles[2]);
            clothPoint.floorY = doubles[1];
            if (clothPoint.y < clothPoint.floorY || d4 <= 0.0) {
                clothPoint.y = clothPoint.prevY = clothPoint.renderY = clothPoint.floorY;
            }

            clothPoint.saveAnchor();
            this.points[i2] = clothPoint;
        }

        this.smoothShape();
    }

    private void smoothShape() {
        int i = this.points.length;
        if (i >= 3) {
            double[] doubles = new double[i];
            double[] doubles2 = new double[i];

            for (int i2 = 0; i2 < 8; i2++) {
                for (int i3 = 0; i3 < i; i3++) {
                    JumpCircleRing.ClothPoint clothPoint = this.points[(i3 + i - 1) % i];
                    JumpCircleRing.ClothPoint clothPoint2 = this.points[(i3 + 1) % i];
                    doubles[i3] = (clothPoint.x + clothPoint2.x) * 0.5;
                    this.relaxedY[i3] = (clothPoint.y + clothPoint2.y) * 0.5;
                    doubles2[i3] = (clothPoint.z + clothPoint2.z) * 0.5;
                }

                for (int i4 = 0; i4 < i; i4++) {
                    JumpCircleRing.ClothPoint clothPoint3 = this.points[i4];
                    clothPoint3.x = clothPoint3.x + (doubles[i4] - clothPoint3.x) * 0.4;
                    clothPoint3.y = clothPoint3.y + (this.relaxedY[i4] - clothPoint3.y) * 0.4;
                    clothPoint3.z = clothPoint3.z + (doubles2[i4] - clothPoint3.z) * 0.4;
                    clothPoint3.prevX = clothPoint3.renderX = clothPoint3.x;
                    clothPoint3.prevY = clothPoint3.renderY = clothPoint3.y;
                    clothPoint3.prevZ = clothPoint3.renderZ = clothPoint3.z;
                    clothPoint3.floorY = Math.min(clothPoint3.floorY, clothPoint3.y);
                }
            }

            JumpCircleRing.ClothPoint[] clothPoints = this.points;
            int i5 = clothPoints.length;

            for (int i6 = 0; i6 < i5; i6++) {
                clothPoints[i6].saveAnchor();
            }
        }
    }

    private void projectOnTerrain(double d, double d2, double d3, double[] doubles) {
        double d4 = Math.sqrt(d * d + d2 * d2);
        double d5 = this.centerX;
        double d6 = this.centerZ;
        double d7 = this.findSurfaceY(d5, d6, this.centerY + 1.0);
        if (!(d4 < 1.0E-6) && !(d3 < 1.0E-6)) {
            double d8 = d / d4 * 0.08;
            double d9 = d2 / d4 * 0.08;
            double d10 = 0.0;
            int i = 0;

            for (int i2 = (int)(d3 / 0.08) * 4 + 64; d10 < d3 && i < i2; i++) {
                double d11 = d5 + d8;
                double d12 = d6 + d9;
                double d13 = this.findSurfaceY(d11, d12, d7 + 1.0);
                double d14 = d13 - d7;
                if (d14 > 0.08) {
                    d7 += 0.08;
                    d10 += 0.08;
                } else {
                    d5 = d11;
                    d6 = d12;
                    d7 = d13;
                    d10 += 0.08;
                }
            }

            doubles[0] = d5;
            doubles[1] = d7;
            doubles[2] = d6;
        } else {
            doubles[0] = d5;
            doubles[1] = d7;
            doubles[2] = d6;
        }
    }

    private double findSurfaceY(double d, double d2, double d3) {
        double d4 = this.findFloorY(d, d3, d2);
        return Double.isNaN(d4) ? d3 - 2.0 : d4;
    }

    void tick() {
        this.prevAge = this.age++;
    }

    boolean isExpired(int i) {
        return this.age >= i;
    }

    Vec3d getCenter() {
        return new Vec3d(this.centerX, this.centerY, this.centerZ);
    }

    float getProgress(int i, float f) {
        float f2 = MathUtil.lerp((float)this.prevAge, (float)this.age, f);
        return Math.min(1.0F, f2 / i);
    }

    float getDrawProgress(int i, float f) {
        float f2 = MathUtil.lerp((float)this.prevAge, (float)this.age, f);
        float f3 = Math.min(1.0F, f2 / (i * 0.25F));
        return (float)Easings.CUBIC_OUT.ease(f3);
    }

    float getFade(int i, float f) {
        float f2 = MathUtil.lerp((float)this.prevAge, (float)this.age, f);
        float f3 = Math.min(1.0F, f2 / i);
        float f4 = (f3 > 0.5F ? 1.0F - f3 : f3) * 2.0F;
        return (float)Easings.QUINT_OUT.ease(f4);
    }

    void simulate(double d, double d2) {
        JumpCircleRing.ClothPoint[] clothPoints = this.points;
        int i = clothPoints.length;

        for (int i2 = 0; i2 < i; i2++) {
            clothPoints[i2].storeRenderPos();
        }

        this.updateFloors();
        int i3 = clothPoints.length;

        for (int i4 = 0; i4 < i3; i4++) {
            clothPoints[i4].integrate(0.04 * d);
        }

        for (int i5 = 0; i5 < this.iterations; i5++) {
            this.relaxHeights();
        }

        double d3 = Math.min(1.0, 0.35 * d2);
        int i6 = clothPoints.length;

        for (int i7 = 0; i7 < i6; i7++) {
            clothPoints[i7].pullToAnchor(d3);
        }
    }

    List<Vec3d> getRenderPoints(float f, double d) {
        int i = this.points.length;
        ArrayList arrayList = new ArrayList(i);

        for (int i2 = 0; i2 < i; i2++) {
            JumpCircleRing.ClothPoint clothPoint = this.points[i2];
            arrayList.add(
                new Vec3d(
                    MathUtil.lerp(clothPoint.renderX, clothPoint.x, f), MathUtil.lerp(clothPoint.renderY, clothPoint.y, f) + d, MathUtil.lerp(clothPoint.renderZ, clothPoint.z, f)
                )
            );
        }

        return arrayList;
    }

    private void relaxHeights() {
        int i = this.points.length;

        for (int i2 = 0; i2 < i; i2++) {
            JumpCircleRing.ClothPoint clothPoint = this.points[i2];
            if (clothPoint.isOnFloor()) {
                this.relaxedY[i2] = clothPoint.y;
            } else {
                JumpCircleRing.ClothPoint clothPoint2 = this.points[(i2 + i - 1) % i];
                JumpCircleRing.ClothPoint clothPoint3 = this.points[(i2 + 1) % i];
                this.relaxedY[i2] = clothPoint.y + ((clothPoint2.y + clothPoint3.y) * 0.5 - clothPoint.y) * 0.35;
            }
        }

        for (int i3 = 0; i3 < i; i3++) {
            JumpCircleRing.ClothPoint clothPoint4 = this.points[i3];
            if (!clothPoint4.isOnFloor()) {
                clothPoint4.y = Math.max(this.relaxedY[i3], clothPoint4.floorY);
            }
        }
    }

    private void updateFloors() {
        if (mc.world != null) {
            for (JumpCircleRing.ClothPoint clothPoint : this.points) {
                if (!this.isOnSurface(clothPoint.x, clothPoint.floorY, clothPoint.z)) {
                    double d = this.findFloorY(clothPoint.x, clothPoint.y, clothPoint.z);
                    clothPoint.floorY = Double.isNaN(d) ? clothPoint.y - 2.0 : d;
                }
            }
        }
    }

    private boolean isOnSurface(double d, double d2, double d3) {
        int i = MathHelper.floor(d);
        int i2 = MathHelper.floor(d3);
        int i3 = MathHelper.floor(d2 - 1.0E-4);
        MUTABLE_POS.set(i, i3, i2);
        VoxelShape voxelShape = mc.world.getBlockState(MUTABLE_POS).getCollisionShape(mc.world, MUTABLE_POS);
        if (voxelShape.isEmpty()) {
            return false;
        }

        double d4 = d - i;
        double d5 = d3 - i2;
        List list = voxelShape.getBoundingBoxes();
        int i4 = list.size();

        for (int i5 = 0; i5 < i4; i5++) {
            Box box = (Box)list.get(i5);
            if (!(d4 < box.minX) && !(d4 > box.maxX) && !(d5 < box.minZ) && !(d5 > box.maxZ) && Math.abs(i3 + box.maxY - d2) < 0.001) {
                return true;
            }
        }

        return false;
    }

    private double findFloorY(double d, double d2, double d3) {
        if (mc.world == null) {
            return Double.NaN;
        }

        int i = MathHelper.floor(d);
        int i2 = MathHelper.floor(d3);
        double d4 = d - i;
        double d5 = d3 - i2;
        int i3 = MathHelper.floor(d2);
        int i4 = i3;

        for (int i5 = i3 - 2; i4 >= i5; i4--) {
            MUTABLE_POS.set(i, i4, i2);
            VoxelShape voxelShape = mc.world.getBlockState(MUTABLE_POS).getCollisionShape(mc.world, MUTABLE_POS);
            if (!voxelShape.isEmpty()) {
                double d6 = Double.NaN;
                List list = voxelShape.getBoundingBoxes();
                int i6 = list.size();

                for (int i7 = 0; i7 < i6; i7++) {
                    Box box = (Box)list.get(i7);
                    if (!(d4 < box.minX) && !(d4 > box.maxX) && !(d5 < box.minZ) && !(d5 > box.maxZ)) {
                        double d7 = i4 + box.maxY;
                        if (!(d7 > d2) && (Double.isNaN(d6) || d7 > d6)) {
                            d6 = d7;
                        }
                    }
                }

                if (!Double.isNaN(d6)) {
                    return d6;
                }
            }
        }

        return Double.NaN;
    }

    Box getBoundingBox() {
        double d = this.points[0].x;
        double d2 = this.points[0].y;
        double d3 = this.points[0].z;
        double d4 = d;
        double d5 = d2;
        double d6 = d3;

        for (JumpCircleRing.ClothPoint clothPoint : this.points) {
            d = Math.min(d, clothPoint.x);
            d4 = Math.max(d4, clothPoint.x);
            d2 = Math.min(d2, clothPoint.y);
            d5 = Math.max(d5, clothPoint.y);
            d3 = Math.min(d3, clothPoint.z);
            d6 = Math.max(d6, clothPoint.z);
        }

        return new Box(d, d2, d3, d4, d5, d6);
    }

    static final class ClothPoint {
        double x;
        double y;
        double z;
        double prevX;
        double prevY;
        double prevZ;
        double renderX;
        double renderY;
        double renderZ;
        double floorY;
        private double anchorX;
        private double anchorZ;

        ClothPoint(double d, double d2, double d3) {
            this.x = this.renderX = this.prevX = d;
            this.y = this.renderY = this.prevY = d2;
            this.z = this.renderZ = this.prevZ = d3;
        }

        boolean isOnFloor() {
            return this.y <= this.floorY + 1.0E-4;
        }

        void saveAnchor() {
            this.anchorX = this.x;
            this.anchorZ = this.z;
        }

        void storeRenderPos() {
            this.renderX = this.x;
            this.renderY = this.y;
            this.renderZ = this.z;
        }

        void integrate(double d) {
            double d2 = (this.x - this.prevX) * 0.98F;
            double d3 = (this.y - this.prevY) * 0.98F;
            double d4 = (this.z - this.prevZ) * 0.98F;
            double d5 = Math.sqrt(d2 * d2 + d3 * d3 + d4 * d4);
            if (d5 > 0.35) {
                double d6 = 0.35 / d5;
                d2 *= d6;
                d3 *= d6;
                d4 *= d6;
            }

            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;
            this.x += d2;
            this.y += d3 - d;
            this.z += d4;
            if (!(this.y > this.floorY)) {
                this.y = this.floorY;
                this.prevY = this.floorY;
            }
        }

        void pullToAnchor(double d) {
            double d2 = (this.anchorX - this.x) * d;
            double d3 = (this.anchorZ - this.z) * d;
            this.x += d2;
            this.prevX += d2;
            this.z += d3;
            this.prevZ += d3;
        }
    }
}
