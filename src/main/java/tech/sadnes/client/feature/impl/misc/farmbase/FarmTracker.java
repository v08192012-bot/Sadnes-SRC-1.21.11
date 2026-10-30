package tech.sadnes.client.feature.impl.misc.farmbase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.Nullable;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.ui.hud.WaypointRenderer;

final class FarmTracker {
    private final List<FarmTracker.Farm> farms = new ArrayList<>();
    private int nextId = 1;

    void reset() {
        this.removeWaypoints();
        this.farms.clear();
        this.nextId = 1;
    }

    List<FarmTracker.Farm> update(
        Collection<TrackedSpawner> trackedSpawners, Predicate<TrackedSpawner> predicate, Collection<DamagerBlock> damagerBlocks, Predicate<DamagerBlock> predicate2, ChunkPos chunkPos, int i
    ) {
        Iterator iterator2 = this.farms.iterator();

        while (iterator2.hasNext()) {
            ((FarmTracker.Farm)iterator2.next()).reset();
        }

        ArrayList arrayList = new ArrayList();

        for (TrackedSpawner trackedSpawner : trackedSpawners) {
            if (!predicate.test(trackedSpawner)) {
                this.markFiltered(trackedSpawner.getPos());
            } else {
                FarmTracker.Farm farm = this.getOrCreateFarm(trackedSpawner.getPos(), arrayList);
                farm.spawnerCounts.merge(trackedSpawner.getDisplayName(), 1, Integer::sum);
                if (trackedSpawner.isHidden()) {
                    farm.hiddenCount++;
                }
            }
        }

        for (DamagerBlock damagerBlock : damagerBlocks) {
            if (predicate2.test(damagerBlock)) {
                this.getOrCreateFarm(damagerBlock.getPos(), arrayList).damagerCount++;
            } else {
                this.markFiltered(damagerBlock.getPos());
            }
        }

        this.farms.removeIf(farm2 -> {
            int i2 = ChunkSectionPos.getSectionCoord(farm2.pos.getX());
            int i3 = ChunkSectionPos.getSectionCoord(farm2.pos.getZ());
            boolean flag = Math.abs(i2 - chunkPos.x) > i || Math.abs(i3 - chunkPos.z) > i;
            if (flag && farm2.isEmpty() && !farm2.filtered) {
                if (farm2.waypointName != null) {
                    WaypointRenderer.remove(farm2.waypointName);
                }

                return true;
            } else {
                return false;
            }
        });
        return arrayList;
    }

    void syncWaypoints() {
        for (FarmTracker.Farm farm : this.farms) {
            String string = farm.isEmpty() ? null : farm.getWaypointName();
            if (!Objects.equals(string, farm.waypointName)) {
                if (string == null) {
                    WaypointRenderer.remove(farm.waypointName);
                } else {
                    boolean flag;
                    if (farm.waypointName != null) {
                        flag = WaypointRenderer.rename(farm.waypointName, string);
                    } else {
                        flag = false;
                    }

                    BlockPos blockPos = farm.pos;
                    if (!flag) {
                        WaypointRenderer.add(string, blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                    }
                }

                farm.waypointName = string;
            }
        }
    }

    void removeWaypoints() {
        for (FarmTracker.Farm farm : this.farms) {
            if (farm.waypointName != null) {
                WaypointRenderer.remove(farm.waypointName);
                farm.waypointName = null;
            }
        }
    }

    private void markFiltered(BlockPos blockPos) {
        FarmTracker.Farm farm = this.findFarm(blockPos);
        if (farm != null) {
            farm.filtered = true;
        }
    }

    private FarmTracker.Farm getOrCreateFarm(BlockPos blockPos, List<FarmTracker.Farm> farms2) {
        FarmTracker.Farm farm = this.findFarm(blockPos);
        if (farm != null) {
            return farm;
        }

        FarmTracker.Farm farm2 = new FarmTracker.Farm(this.nextId++, blockPos);
        this.farms.add(farm2);
        farms2.add(farm2);
        return farm2;
    }

    @Nullable
    private FarmTracker.Farm findFarm(BlockPos blockPos) {
        for (FarmTracker.Farm farm : this.farms) {
            if (farm.pos.getSquaredDistance(blockPos) <= 1024.0) {
                return farm;
            }
        }

        return null;
    }

    static final class Farm {
        private final int id;
        final BlockPos pos;
        final Map<String, Integer> spawnerCounts = new TreeMap<>();
        int damagerCount;
        int hiddenCount;
        boolean filtered;
        @Nullable
        String waypointName;

        Farm(int i, BlockPos blockPos) {
            this.id = i;
            this.pos = blockPos;
        }

        void reset() {
            this.spawnerCounts.clear();
            this.damagerCount = 0;
            this.hiddenCount = 0;
            this.filtered = false;
        }

        boolean isEmpty() {
            return this.spawnerCounts.isEmpty() && this.damagerCount == 0;
        }

        String describe() {
            ArrayList arrayList = new ArrayList<>(this.spawnerCounts.entrySet());
            arrayList.sort(Entry.comparingByValue(Comparator.reverseOrder()));
            StringBuilder builder = new StringBuilder();
            int i = arrayList.size();
            int i2 = Math.min(i, 2);

            for (int i3 = 0; i3 < i2; i3++) {
                Entry entry = (Entry)arrayList.get(i3);
                if (i3 > 0) {
                    builder.append(", ");
                }

                builder.append((String)entry.getKey());
                if ((Integer)entry.getValue() > 1) {
                    builder.append(" ×").append(entry.getValue());
                }
            }

            if (i > 2) {
                builder.append(" +").append(i - 2);
            }

            if (this.damagerCount > 0) {
                if (!builder.isEmpty()) {
                    builder.append(" · ");
                }

                builder.append(Language.translate("farmbase.damager", "Блок-дамагер"));
            }

            if (this.hiddenCount > 0) {
                builder.append(" · ").append(Language.translate("farmbase.hiddenMark", "скрытая"));
            }

            return builder.toString();
        }

        String getWaypointName() {
            return String.format(Language.translate("farmbase.waypoint", "Ферма #%d · %s"), this.id, this.describe());
        }

        public int getId() {
            return this.id;
        }

        public BlockPos getPos() {
            return this.pos;
        }
    }
}
