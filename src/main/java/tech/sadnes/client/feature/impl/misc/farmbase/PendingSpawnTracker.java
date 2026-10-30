package tech.sadnes.client.feature.impl.misc.farmbase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

final class PendingSpawnTracker {
    private final Map<Integer, PendingSpawnTracker.PendingSpawn> pending = new LinkedHashMap<>();
    private boolean matchedAny;
    private int ignoreUntilTick;

    void clear() {
        this.pending.clear();
        this.matchedAny = false;
    }

    void resetGrace(int i) {
        this.ignoreUntilTick = i + 60;
    }

    boolean isInGrace(int i) {
        return i < this.ignoreUntilTick;
    }

    void add(PendingSpawnTracker.PendingSpawn pendingSpawn) {
        this.pending.put(pendingSpawn.entityId(), pendingSpawn);
    }

    @Nullable
    PendingSpawnTracker.PendingSpawn matchSpawner(TrackedSpawner trackedSpawner) {
        this.matchedAny = true;
        PendingSpawnTracker.PendingSpawn pendingSpawn = null;
        boolean flag = false;

        for (PendingSpawnTracker.PendingSpawn pendingSpawn2 : this.pending.values()) {
            Vec3d vec3d = pendingSpawn2.pos();
            if (trackedSpawner.canSpawnAt(vec3d.x, vec3d.y, vec3d.z)) {
                boolean flag2 = pendingSpawn2.type() == trackedSpawner.getEntityType();
                if (pendingSpawn == null || flag2 || !flag) {
                    pendingSpawn = pendingSpawn2;
                    flag = flag2;
                }
            }
        }

        if (pendingSpawn == null) {
            return null;
        }

        this.pending.remove(pendingSpawn.entityId());
        return new PendingSpawnTracker.PendingSpawn(pendingSpawn.entityId(), pendingSpawn.type(), pendingSpawn.pos(), trackedSpawner, pendingSpawn.tick());
    }

    @Nullable
    EntityType<?> findNearbyType(BlockPos blockPos) {
        EntityType entityType = null;

        for (PendingSpawnTracker.PendingSpawn pendingSpawn : this.pending.values()) {
            Vec3d vec3d = pendingSpawn.pos();
            if (TrackedSpawner.isInSpawnArea(blockPos, 4, vec3d.x, vec3d.y, vec3d.z)) {
                entityType = pendingSpawn.type();
            }
        }

        return entityType;
    }

    @Nullable
    PendingSpawnTracker.PendingSpawn takeExact(int i) {
        PendingSpawnTracker.PendingSpawn pendingSpawn = this.pending.get(i);
        if (pendingSpawn != null && pendingSpawn.exact()) {
            this.matchedAny = true;
            return this.pending.remove(i);
        } else {
            return null;
        }
    }

    List<PendingSpawnTracker.PendingSpawn> collectExpired(int i, Vec3d vec3d) {
        ArrayList arrayList = new ArrayList();
        Iterator iterator2 = this.pending.values().iterator();

        while (iterator2.hasNext()) {
            PendingSpawnTracker.PendingSpawn pendingSpawn = (PendingSpawnTracker.PendingSpawn)iterator2.next();
            if (i - pendingSpawn.tick() >= 10) {
                iterator2.remove();
                TrackedSpawner trackedSpawner = pendingSpawn.source();
                if (trackedSpawner != null) {
                    double d = trackedSpawner.getRequiredPlayerRange() + 1.0;
                    boolean flag = vec3d.squaredDistanceTo(trackedSpawner.getBox().getCenter()) <= d * d;
                    boolean flag2;
                    if (this.matchedAny) {
                        flag2 = trackedSpawner.isHidden();
                    } else {
                        flag2 = true;
                    }

                    boolean flag3;
                    if (flag2 && pendingSpawn.exact() && pendingSpawn.tick() >= this.ignoreUntilTick) {
                        flag3 = flag;
                    } else {
                        flag3 = false;
                    }

                    if (flag3) {
                        arrayList.add(pendingSpawn);
                    }
                }
            }
        }

        return arrayList;
    }

    record PendingSpawn(int entityId, EntityType<?> type, Vec3d pos, @Nullable TrackedSpawner source, int tick) {
        boolean exact() {
            return this.source != null && this.type == this.source.getEntityType();
        }
    }
}
