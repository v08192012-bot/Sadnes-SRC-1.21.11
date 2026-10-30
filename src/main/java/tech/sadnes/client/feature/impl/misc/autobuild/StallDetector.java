package tech.sadnes.client.feature.impl.misc.autobuild;

import baritone.api.BaritoneAPI;
import java.util.List;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.util.IMinecraft;

public final class StallDetector implements IMinecraft {
    private boolean materialsShortage;
    private boolean materialsMissingNearby;
    private boolean awaitingResume;
    private long shortageInventoryHash;
    private int pausedTicks;
    private int resumeAttempts;
    private int bestRemaining;
    private Vec3d lastPlayerPos;
    private int idleTicks;
    private int lastCount;
    private long lastStuckPos = Long.MIN_VALUE;
    private int stuckPosRepeats;
    private long obstaclePos = Long.MIN_VALUE;
    private int obstacleScore;
    private int heldInGoalTicks;
    private int cutOffTicks;
    private long lastInventoryHash;
    private int unsupportedCount;
    private long stubbornPos = Long.MIN_VALUE;

    public void reset() {
        this.materialsShortage = this.materialsMissingNearby = this.awaitingResume = false;
        this.pausedTicks = this.resumeAttempts = this.idleTicks = 0;
        this.bestRemaining = Integer.MAX_VALUE;
        this.lastPlayerPos = null;
        this.lastStuckPos = this.obstaclePos = this.stubbornPos = Long.MIN_VALUE;
        this.stuckPosRepeats = this.obstacleScore = this.heldInGoalTicks = this.cutOffTicks = 0;
    }

    public boolean isMaterialsShort() {
        return this.materialsShortage || this.materialsMissingNearby;
    }

    public void resetIdle() {
        this.idleTicks = this.stuckPosRepeats = 0;
    }

    public StallDetector.MovementStall checkMovement(boolean flag, boolean flag2) {
        long l = this.hashInventory();
        boolean flag3 = l == this.lastInventoryHash;
        this.lastInventoryHash = l;
        this.heldInGoalTicks = flag && flag3 ? this.heldInGoalTicks + 1 : 0;
        this.cutOffTicks = flag2 && flag3 ? this.cutOffTicks + 1 : 0;
        boolean flag4 = this.heldInGoalTicks >= 40;
        boolean flag5 = this.cutOffTicks >= 100;
        if (flag4 || flag5) {
            this.heldInGoalTicks = this.cutOffTicks = 0;
        }

        if (flag4) {
            return StallDetector.MovementStall.HELD_IN_GOAL;
        } else {
            return flag5 ? StallDetector.MovementStall.CUT_OFF : StallDetector.MovementStall.NONE;
        }
    }

    public void requestResume() {
        this.awaitingResume = true;
    }

    public StallDetector.Action checkPaused(BuildProgress buildProgress, StallDetector.PauseReason pauseReason, boolean flag) {
        if (!buildProgress.isScanned()) {
            return StallDetector.Action.NONE;
        }

        if (pauseReason == StallDetector.PauseReason.SHORTAGE) {
            long l = this.hashInventory();
            boolean flag2 = l != this.shortageInventoryHash;
            StallDetector.Action action = !this.materialsShortage
                ? StallDetector.Action.MATERIALS
                : (flag2 ? StallDetector.Action.RESUME : StallDetector.Action.NONE);
            this.materialsShortage = true;
            this.shortageInventoryHash = l;
            return action;
        }

        this.materialsShortage = false;
        if (pauseReason == StallDetector.PauseReason.UNPLACEABLE) {
            this.awaitingResume = !flag;
            return flag ? StallDetector.Action.ORIENTATION : StallDetector.Action.UNPLACEABLE;
        }

        if (this.awaitingResume) {
            this.awaitingResume = false;
            return StallDetector.Action.RESUME;
        }

        if (buildProgress.getRemaining() < this.bestRemaining) {
            this.bestRemaining = buildProgress.getRemaining();
            this.resumeAttempts = 0;
        }

        if (++this.pausedTicks >= 100) {
            this.pausedTicks = 0;
            boolean flag3 = ++this.resumeAttempts > 6;
            return flag3 ? StallDetector.Action.ABANDON : StallDetector.Action.RESUME;
        } else {
            return StallDetector.Action.NONE;
        }
    }

    public StallDetector.Action checkBuilding(BuildProgress buildProgress, List<ItemStack> itemStacks, int i, long l, int i2) {
        this.materialsShortage = this.awaitingResume = false;
        if (l != this.lastStuckPos) {
            this.lastStuckPos = l;
            this.stuckPosRepeats = 0;
        } else if (l != Long.MIN_VALUE && ++this.stuckPosRepeats >= i2) {
            this.stuckPosRepeats = 0;
            this.stubbornPos = l;
            return StallDetector.Action.STUBBORN;
        }

        boolean flag = mc.interactionManager.isBreakingBlock();
        if (this.hasProgressed(buildProgress.getMismatchedNearby().size(), flag)) {
            this.materialsMissingNearby = false;
            return StallDetector.Action.NONE;
        }

        if (++this.idleTicks % 300 == 0) {
            this.unsupportedCount = buildProgress.pruneUnsupported(mc.world);
            if (this.unsupportedCount > 0) {
                return StallDetector.Action.UNSUPPORTED;
            }

            double d = BaritoneAPI.getSettings().blockReachDistance.value.floatValue();
            if (buildProgress.hasMissingNearby(itemStacks, i, mc.player, d)) {
                this.idleTicks = 0;
                if (this.materialsMissingNearby) {
                    StallDetector.Action action = StallDetector.Action.NONE;
                    this.materialsMissingNearby = true;
                    return action;
                } else {
                    StallDetector.Action action2 = StallDetector.Action.MATERIALS;
                    this.materialsMissingNearby = true;
                    return action2;
                }
            } else {
                boolean flag2;
                StallDetector stallDetector;
                long l2;
                if (this.idleTicks >= 2400) {
                    flag2 = true;
                    stallDetector = this;
                    l2 = Long.MIN_VALUE;
                } else {
                    flag2 = false;
                    stallDetector = this;
                    l2 = buildProgress.findReplaceableNearby(mc.world, mc.player, d);
                }

                stallDetector.stubbornPos = l2;
                if (this.stubbornPos != Long.MIN_VALUE) {
                    return StallDetector.Action.STUBBORN;
                } else {
                    return flag2 ? StallDetector.Action.ABANDON : StallDetector.Action.NONE;
                }
            }
        } else {
            return StallDetector.Action.NONE;
        }
    }

    public StallDetector.Action checkClearing(BuildProgress buildProgress, long l, boolean flag) {
        this.materialsShortage = this.materialsMissingNearby = this.awaitingResume = false;
        if (this.hasProgressed(buildProgress.getObstacleCount(), false)) {
            this.obstacleScore = 0;
            return StallDetector.Action.NONE;
        }

        if (l != Long.MIN_VALUE) {
            if (this.obstacleScore == 0) {
                this.obstaclePos = l;
            }

            this.obstacleScore = this.obstacleScore + (l == this.obstaclePos ? 1 : -1);
        }

        boolean flag2 = ++this.idleTicks >= 600;
        boolean flag3 = this.idleTicks % 100 == 0;
        if (!flag2) {
            return flag && flag3 ? StallDetector.Action.RESUME : StallDetector.Action.NONE;
        }

        boolean flag4 = this.obstacleScore >= 200;
        this.stubbornPos = flag4 ? this.obstaclePos : Long.MIN_VALUE;
        this.idleTicks = this.obstacleScore = 0;
        return flag4 ? StallDetector.Action.STUBBORN : StallDetector.Action.ABANDON;
    }

    private boolean hasProgressed(int i, boolean flag) {
        Vec3d vec3d = mc.player.getEntityPos();
        if (this.lastPlayerPos != null) {
            boolean flag2 = vec3d.squaredDistanceTo(this.lastPlayerPos) > 2.25;
            boolean flag3 = i < this.lastCount;
            if (i <= this.lastCount + 1 && !flag2 && !flag3 && !flag && mc.currentScreen == null) {
                return false;
            }
        }

        this.lastPlayerPos = vec3d;
        this.lastCount = i;
        this.idleTicks = 0;
        return true;
    }

    private long hashInventory() {
        long l = 0L;

        for (ItemStack itemStack : mc.player.getInventory().getMainStacks()) {
            if (itemStack.getItem() instanceof BlockItem) {
                l = l * 31L + Item.getRawId(itemStack.getItem()) * 131L + itemStack.getCount();
            }
        }

        return l;
    }

    public int getUnsupportedCount() {
        return this.unsupportedCount;
    }

    public long getStubbornPos() {
        return this.stubbornPos;
    }

    public enum Action {
        NONE,
        MATERIALS,
        RESUME,
        ORIENTATION,
        UNPLACEABLE,
        UNSUPPORTED,
        STUBBORN,
        ABANDON;
    }

    public enum MovementStall {
        NONE,
        HELD_IN_GOAL,
        CUT_OFF;
    }

    public enum PauseReason {
        SHORTAGE,
        UNPLACEABLE,
        OTHER;
    }
}
