package tech.sadnes.client.feature.impl.misc.autowarden;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.IMinecraft;

public final class WardenChestTracker implements IMinecraft {
    private static final Pattern TIMER_PATTERN = Pattern.compile("(\\d{2}):(\\d{2})");
    private final List<BlockPos> chests = new ArrayList<>();
    private final List<BlockPos> chestsView = Collections.unmodifiableList(this.chests);
    private final Map<Integer, Map<BlockPos, Long>> refillTimes = new HashMap<>();

    public List<BlockPos> getChests() {
        return this.chestsView;
    }

    public void update() {
        this.chests.clear();

        for (int i = -130; i <= -121; i++) {
            for (int i2 = -130; i2 <= -121; i2++) {
                WorldChunk worldChunk = mc.world.getChunkManager().getChunk(i, i2, ChunkStatus.FULL, false);
                if (worldChunk != null) {
                    for (BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
                        BlockPos blockPos = blockEntity.getPos();
                        boolean flag = blockEntity instanceof ChestBlockEntity
                            && !blockEntity.isRemoved()
                            && blockPos.getY() >= -60
                            && blockPos.getY() <= -35
                            && blockPos.getX() >= -2070
                            && blockPos.getX() <= -1921
                            && blockPos.getZ() >= -2076
                            && blockPos.getZ() <= -1929;
                        if (flag) {
                            this.chests.add(blockPos);
                        }
                    }
                }
            }
        }

        long l = System.currentTimeMillis();
        Map map = this.refillTimes.computeIfAbsent(ServerManager.getFunTimeAnarchy(), integer -> new HashMap<>());

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ArmorStandEntity armorStandEntity) {
                long l2 = parseTimer(armorStandEntity);
                if (l2 >= 0L) {
                    BlockPos blockPos2 = this.findChestInColumn(armorStandEntity.getBlockPos());
                    if (blockPos2 != null) {
                        Long longValue = (Long)map.get(blockPos2);
                        boolean flag2 = longValue == null || Math.abs(l2 / 1000L - Math.max(0L, longValue - l) / 1000L) > 5L;
                        if (flag2) {
                            map.put(blockPos2, l + l2);
                        }
                    }
                }
            }
        }

        Iterator<Map<BlockPos, Long>> iterator2 = this.refillTimes.values().iterator();

        while (iterator2.hasNext()) {
            iterator2.next().values().removeIf(longValue2 -> longValue2 <= l);
        }
    }

    public long getRefillTime(BlockPos blockPos) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ArmorStandEntity armorStandEntity && armorStandEntity.getBlockPos().getX() == blockPos.getX() && armorStandEntity.getBlockPos().getZ() == blockPos.getZ()) {
                long l = parseTimer(armorStandEntity);
                if (l >= 0L) {
                    return l;
                }
            }
        }

        Long longValue = this.refillTimes.getOrDefault(ServerManager.getFunTimeAnarchy(), Map.of()).get(blockPos);
        return longValue == null ? -1L : Math.max(0L, longValue - System.currentTimeMillis());
    }

    private BlockPos findChestInColumn(BlockPos blockPos) {
        for (BlockPos blockPos2 : this.chests) {
            if (blockPos.getX() == blockPos2.getX() && blockPos.getZ() == blockPos2.getZ()) {
                return blockPos2;
            }
        }

        return null;
    }

    private static long parseTimer(ArmorStandEntity armorStandEntity) {
        Matcher matcher2 = TIMER_PATTERN.matcher(armorStandEntity.getName().getString());
        return !matcher2.find() ? -1L : (Integer.parseInt(matcher2.group(1)) * 60L + Integer.parseInt(matcher2.group(2))) * 1000L;
    }
}
