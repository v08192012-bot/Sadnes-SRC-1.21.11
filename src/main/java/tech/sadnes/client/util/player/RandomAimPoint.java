package tech.sadnes.client.util.player;

import java.util.List;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RandomAimPoint {
    private Vec3d currentPoint = null;
    private Vec3d targetPoint = null;
    private Entity lastEntity;
    private float progress = 1.0F;
    private float speed;

    public RandomAimPoint(float f) {
        this.speed = f;
    }

    public Vec3d getPoint(Entity entity, float f, boolean flag) {
        if (entity != this.lastEntity) {
            flag = true;
        }

        this.lastEntity = entity;
        List list = AimPointUtil.getBandPoints(entity, f);
        if (list.isEmpty()) {
            return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        }

        if (this.currentPoint == null) {
            this.currentPoint = (Vec3d)list.get(new Random().nextInt(list.size()));
            this.targetPoint = this.currentPoint;
        }

        if (flag && this.progress >= 1.0F) {
            this.targetPoint = (Vec3d)list.get(new Random().nextInt(list.size()));
            this.progress = 0.0F;
        }

        if (this.progress < 1.0F) {
            this.progress = this.progress + this.speed;
            if (this.progress > 1.0F) {
                this.progress = 1.0F;
            }

            this.currentPoint = new Vec3d(
                AimPointUtil.lerp(this.currentPoint.x, this.targetPoint.x, this.progress),
                AimPointUtil.lerp(this.currentPoint.y, this.targetPoint.y, this.progress),
                AimPointUtil.lerp(this.currentPoint.z, this.targetPoint.z, this.progress)
            );
        }

        return new Vec3d(
            MathHelper.clamp(this.currentPoint.x, entity.getBoundingBox().minX, entity.getBoundingBox().maxX),
            MathHelper.clamp(this.currentPoint.y, entity.getBoundingBox().minY, entity.getBoundingBox().maxY),
            MathHelper.clamp(this.currentPoint.z, entity.getBoundingBox().minZ, entity.getBoundingBox().maxZ)
        );
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setSpeed(float f) {
        this.speed = f;
    }
}
