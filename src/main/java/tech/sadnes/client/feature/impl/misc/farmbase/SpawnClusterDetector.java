package tech.sadnes.client.feature.impl.misc.farmbase;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

final class SpawnClusterDetector {
    private final List<SpawnClusterDetector.SpawnCluster> clusters = new ArrayList<>();

    void clear() {
        this.clusters.clear();
    }

    @Nullable
    BlockPos recordSpawn(EntityType<?> entityType, Vec3d vec3d, int i) {
        this.clusters.removeIf(spawnCluster -> i - spawnCluster.lastTick > 12000);
        SpawnClusterDetector.SpawnCluster spawnCluster2 = this.findCluster(entityType, vec3d);
        if (spawnCluster2 == null) {
            SpawnClusterDetector.SpawnCluster spawnCluster3 = new SpawnClusterDetector.SpawnCluster(entityType, vec3d, i);
            this.clusters.add(spawnCluster3);
            return null;
        } else {
            spawnCluster2.add(vec3d, i);
            if (!spawnCluster2.reported && spawnCluster2.waves >= 3) {
                spawnCluster2.reported = true;
                return spawnCluster2.getCenter();
            } else {
                return null;
            }
        }
    }

    @Nullable
    private SpawnClusterDetector.SpawnCluster findCluster(EntityType<?> entityType, Vec3d vec3d) {
        for (SpawnClusterDetector.SpawnCluster spawnCluster : this.clusters) {
            if (spawnCluster.type == entityType && spawnCluster.isNear(vec3d)) {
                return spawnCluster;
            }
        }

        return null;
    }

    static final class SpawnCluster {
        final EntityType<?> type;
        private double sumX;
        private double sumY;
        private double sumZ;
        private int samples;
        int lastTick;
        int waves = 1;
        boolean reported;

        SpawnCluster(EntityType<?> entityType, Vec3d vec3d, int i) {
            this.type = entityType;
            this.add(vec3d, i);
        }

        void add(Vec3d vec3d, int i) {
            if (i - this.lastTick >= 40 && this.samples > 0) {
                this.waves++;
            }

            this.sumX = this.sumX + vec3d.x;
            this.sumY = this.sumY + vec3d.y;
            this.sumZ = this.sumZ + vec3d.z;
            this.samples++;
            this.lastTick = i;
        }

        boolean isNear(Vec3d vec3d) {
            return Math.abs(vec3d.x - this.sumX / this.samples) <= 5.0
                && Math.abs(vec3d.z - this.sumZ / this.samples) <= 5.0
                && Math.abs(vec3d.y - this.sumY / this.samples) <= 3.0;
        }

        BlockPos getCenter() {
            return BlockPos.ofFloored(this.sumX / this.samples, this.sumY / this.samples, this.sumZ / this.samples);
        }
    }
}
