package tech.sadnes.client.feature.impl.misc.farmbase;

import java.util.Objects;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Language;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

final class TrackedSpawner {
    private final BlockPos pos;
    private final Box box;
    @Nullable
    private final EntityType<?> entityType;
    private final SpawnerCategory category;
    private final boolean ignored;
    private final boolean hidden;
    private int spawnRange;
    private int requiredPlayerRange;
    private boolean confirmed;
    private long lastNotifyTime;
    private String cachedName;
    private Language cachedLanguage;
    private String cachedLocale;

    private TrackedSpawner(BlockPos blockPos, @Nullable EntityType<?> entityType2, boolean flag, boolean flag2) {
        this.pos = blockPos;
        this.box = new Box(blockPos);
        this.entityType = entityType2;
        this.category = SpawnerCategory.fromEntityType(entityType2);
        this.ignored = flag;
        this.hidden = flag2;
    }

    static TrackedSpawner of(BlockPos blockPos, @Nullable EntityType<?> entityType2, boolean flag) {
        return new TrackedSpawner(blockPos, entityType2, flag, false);
    }

    static TrackedSpawner hidden(BlockPos blockPos, @Nullable EntityType<?> entityType2) {
        return new TrackedSpawner(blockPos, entityType2, false, true);
    }

    void confirm() {
        this.confirmed = true;
    }

    void setRanges(int i, int i2) {
        this.spawnRange = MathHelper.clamp(i, 0, 16);
        this.requiredPlayerRange = i2;
    }

    static boolean isInSpawnArea(BlockPos blockPos, int i, double d, double d2, double d3) {
        double d4 = i + 1.0;
        return Math.abs(d - (blockPos.getX() + 0.5)) <= d4 && Math.abs(d3 - (blockPos.getZ() + 0.5)) <= d4 && Math.abs(d2 - blockPos.getY()) <= 2.0;
    }

    boolean canSpawnAt(double d, double d2, double d3) {
        return isInSpawnArea(this.pos, this.spawnRange, d, d2, d3);
    }

    Box getSpawnZone() {
        return new Box(
            this.pos.getX() - this.spawnRange,
            this.pos.getY() - 1,
            this.pos.getZ() - this.spawnRange,
            this.pos.getX() + this.spawnRange + 1.0,
            this.pos.getY() + 2.0,
            this.pos.getZ() + this.spawnRange + 1.0
        );
    }

    String getDisplayName() {
        Language language = Language.getInstance();
        String string = tech.sadnes.client.lang.Language.getLocale();
        boolean flag;
        if (this.cachedName != null && language == this.cachedLanguage) {
            flag = !Objects.equals(string, this.cachedLocale);
        } else {
            flag = true;
        }

        if (flag) {
            this.cachedName = this.entityType == null
                ? tech.sadnes.client.lang.Language.translate("farmbase.emptySpawner", "Пустой спавнер")
                : this.entityType.getName().getString();
            this.cachedLanguage = language;
            this.cachedLocale = string;
        }

        return this.cachedName;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Box getBox() {
        return this.box;
    }

    @Nullable
    public EntityType<?> getEntityType() {
        return this.entityType;
    }

    public SpawnerCategory getCategory() {
        return this.category;
    }

    public boolean isIgnored() {
        return this.ignored;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public int getSpawnRange() {
        return this.spawnRange;
    }

    public int getRequiredPlayerRange() {
        return this.requiredPlayerRange;
    }

    public boolean isConfirmed() {
        return this.confirmed;
    }

    public long getLastNotifyTime() {
        return this.lastNotifyTime;
    }

    public TrackedSpawner setLastNotifyTime(long l) {
        this.lastNotifyTime = l;
        return this;
    }
}
