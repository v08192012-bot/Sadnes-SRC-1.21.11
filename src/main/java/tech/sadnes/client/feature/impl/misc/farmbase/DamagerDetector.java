package tech.sadnes.client.feature.impl.misc.farmbase;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

final class DamagerDetector {
    private final Map<Integer, DamagerDetector.SpawnRecord> spawns = new HashMap<>();
    private final List<DamagerDetector.SpawnRecord> recentDeaths = new ArrayList<>();
    private final List<DamagerBlock> damagers = new ArrayList<>();

    Collection<DamagerBlock> getDamagers() {
        return Collections.unmodifiableList(this.damagers);
    }

    void clear() {
        this.spawns.clear();
        this.recentDeaths.clear();
        this.damagers.clear();
    }

    void recordSpawn(int i, Vec3d vec3d, int i2) {
        this.spawns.values().removeIf(spawnRecord -> i2 - spawnRecord.tick() > 40);
        this.spawns.put(i, new DamagerDetector.SpawnRecord(vec3d, i2));
    }

    boolean handleRemoved(IntList intList, int i, long l) {
        boolean flag = false;
        int i2 = intList.size();

        for (int i3 = 0; i3 < i2; i3++) {
            DamagerDetector.SpawnRecord spawnRecord = this.spawns.remove(intList.getInt(i3));
            if (spawnRecord != null && i - spawnRecord.tick() <= 10 && this.registerDeath(spawnRecord, i, l)) {
                flag = true;
            }
        }

        return flag;
    }

    private boolean registerDeath(DamagerDetector.SpawnRecord spawnRecord, int i, long l) {
        for (DamagerBlock damagerBlock : this.damagers) {
            if (!(damagerBlock.getBox().getCenter().squaredDistanceTo(spawnRecord.pos()) > 2.25)) {
                damagerBlock.setLastKillTime(l);
                return false;
            }
        }

        this.recentDeaths.removeIf(spawnRecord2 -> i - spawnRecord2.tick() > 20);
        this.recentDeaths.add(spawnRecord);
        IntOpenHashSet intOpenHashSet = new IntOpenHashSet();

        for (DamagerDetector.SpawnRecord spawnRecord3 : this.recentDeaths) {
            if (spawnRecord3.pos().squaredDistanceTo(spawnRecord.pos()) <= 1.0) {
                intOpenHashSet.add(spawnRecord3.tick());
            }
        }

        if (intOpenHashSet.size() < 3) {
            return false;
        }

        this.recentDeaths.removeIf(spawnRecord4 -> spawnRecord4.pos().squaredDistanceTo(spawnRecord.pos()) <= 1.0);
        this.damagers.add(new DamagerBlock(BlockPos.ofFloored(spawnRecord.pos()), l));
        return true;
    }

    record SpawnRecord(Vec3d pos, int tick) {
    }
}
