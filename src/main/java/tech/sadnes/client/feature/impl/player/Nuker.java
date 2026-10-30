package tech.sadnes.client.feature.impl.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PreRotationEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.rotation.RotationUtil;

@FeatureInfo(
    name = "Nuker",
    description = "feature.impl.player.nuker",
    displayName = "feature.impl.player.nuker",
    category = Category.PLAYER,
    subcategory = Subcategory.INTERACT
)
public final class Nuker extends Feature {
    private static final double[][] HIT_OFFSETS = new double[][]{
        {0.5, 0.5, 0.5}, {0.5, 0.9, 0.5}, {0.5, 0.1, 0.5}, {0.9, 0.5, 0.5}, {0.1, 0.5, 0.5}, {0.5, 0.5, 0.9}, {0.5, 0.5, 0.1}
    };
    private final NumberSetting distance = new NumberSetting("nuker.distance", 4.0F, 1.0F, 6.0F, 0.5F);
    private final BooleanSetting skipHidden = new BooleanSetting("nuker.skipHidden", true);
    private final BooleanSetting rotate = new BooleanSetting("nuker.rotate", false);
    private final BooleanSetting whitelist = new BooleanSetting("nuker.whitelist", false);
    private final TextSetting whiteBlocks = new TextSetting("nuker.whiteBlocks", "").setVisibleWhen(() -> false);
    private final TextSetting blackBlocks = new TextSetting("nuker.blackBlocks", "").setVisibleWhen(() -> false);
    private BlockPos currentTarget;
    private int searchCooldown;

    public static Nuker getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(Nuker.class);
    }

    public boolean isBreaking() {
        return this.isEnabled() && this.currentTarget != null;
    }

    public BlockPos getBreakingPos() {
        return this.isBreaking() ? this.currentTarget : null;
    }

    public List<String> getBlockList(Nuker.BlockListType blockListType) {
        String string = this.getListSetting(blockListType).getValue();
        ArrayList arrayList = new ArrayList();
        if (string != null && !string.isBlank()) {
            for (String string2 : string.split(",")) {
                if (!string2.isBlank()) {
                    arrayList.add(string2.trim());
                }
            }
        }

        return arrayList;
    }

    public boolean addBlock(Nuker.BlockListType blockListType, String string) {
        List list = this.getBlockList(blockListType);
        if (list.contains(string)) {
            return false;
        }

        list.add(string);
        this.getListSetting(blockListType).setValue(String.join(",", list));
        return true;
    }

    public boolean removeBlock(Nuker.BlockListType blockListType, String string) {
        List list = this.getBlockList(blockListType);
        if (!list.remove(string)) {
            return false;
        }

        this.getListSetting(blockListType).setValue(String.join(",", list));
        return true;
    }

    public void clearBlocks(Nuker.BlockListType blockListType) {
        this.getListSetting(blockListType).setValue("");
    }

    private TextSetting getListSetting(Nuker.BlockListType blockListType) {
        return blockListType == Nuker.BlockListType.WHITE ? this.whiteBlocks : this.blackBlocks;
    }

    @Override
    public void onDisable() {
        this.currentTarget = null;
        this.searchCooldown = 0;
        super.onDisable();
    }

    @Override
    public void onEnable() {
        this.resetBreaking();
        super.onEnable();
    }

    @EventHandler
    public void onRotationUpdate(PreRotationEvent preRotationEvent) {
        if (this.rotate.getValue() && this.currentTarget != null && mc.player != null && mc.currentScreen == null) {
            Vec3d vec3d = mc.player.getEyePos();
            Vec3d vec3d2 = Vec3d.ofCenter(this.currentTarget).add(Vec3d.of(this.getHitSide(vec3d, this.currentTarget).getVector()).multiply(0.5));
            float[] floats = RotationUtil.getRotationArray(vec3d, vec3d2);
            RotationComponent.rotateTo(new Rotation(floats[0], MathHelper.clamp(floats[1], -90.0F, 90.0F)), 180.0F, 90.0F, 2, 10);
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            this.currentTarget = null;
        } else if (mc.currentScreen != null) {
            this.resetBreaking();
        } else {
            Vec3d vec3d = mc.player.getEyePos();
            double d = this.distance.getValue().floatValue();
            if (this.currentTarget != null) {
                if (mc.world.getBlockState(this.currentTarget).isAir()) {
                    this.currentTarget = null;
                } else {
                    if (this.isInRange(vec3d, this.currentTarget, d) && this.isBreakable(this.currentTarget)) {
                        mc.interactionManager.updateBlockBreakingProgress(this.currentTarget, this.getHitSide(vec3d, this.currentTarget));
                        mc.player.swingHand(Hand.MAIN_HAND);
                        return;
                    }

                    this.resetBreaking();
                }
            }

            if (this.searchCooldown > 0) {
                this.searchCooldown--;
            } else {
                BlockPos blockPos = this.findTarget(vec3d, d);
                if (blockPos == null) {
                    this.searchCooldown = 3;
                } else {
                    this.currentTarget = blockPos;
                    if (!this.rotate.getValue()) {
                        mc.interactionManager.attackBlock(blockPos, this.getHitSide(vec3d, blockPos));
                        mc.player.swingHand(Hand.MAIN_HAND);
                        if (mc.world.getBlockState(blockPos).isAir()) {
                            this.currentTarget = null;
                        }
                    }
                }
            }
        }
    }

    private BlockPos findTarget(Vec3d vec3d, double d) {
        Box box = mc.player.getBoundingBox().expand(d);
        BlockPos blockPos = BlockPos.ofFloored(box.minX, box.minY, box.minZ);
        BlockPos blockPos2 = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ);
        AutoTool autoTool = AutoTool.getInstance();
        boolean flag;
        if (autoTool != null) {
            flag = autoTool.isEnabled();
        } else {
            flag = false;
        }

        HashSet hashSet = new HashSet<>(this.getBlockList(Nuker.BlockListType.WHITE));
        HashSet hashSet2 = new HashSet<>(this.getBlockList(Nuker.BlockListType.BLACK));
        boolean flag2;
        if (this.whitelist.getValue()) {
            flag2 = !hashSet.isEmpty();
        } else {
            flag2 = false;
        }

        ArrayList<Nuker.BlockCandidate> blockCandidates = new ArrayList<>();

        for (BlockPos blockPos3 : BlockPos.iterate(blockPos, blockPos2)) {
            if (this.isInRange(vec3d, blockPos3, d) && this.isBreakable(blockPos3)) {
                String string = Registries.BLOCK.getId(mc.world.getBlockState(blockPos3).getBlock()).toString();
                if (!hashSet2.contains(string) && (!flag2 || hashSet.contains(string))) {
                    BlockPos blockPos4 = blockPos3.toImmutable();
                    blockCandidates.add(new Nuker.BlockCandidate(blockPos4, this.getBreakSpeed(blockPos4, flag ? autoTool : null), vec3d.squaredDistanceTo(Vec3d.ofCenter(blockPos4))));
                }
            }
        }

        blockCandidates.sort(Comparator.comparingDouble(Nuker.BlockCandidate::speed).reversed().thenComparingDouble(Nuker.BlockCandidate::distance));
        long l = System.nanoTime();

        for (Nuker.BlockCandidate blockCandidate : blockCandidates) {
            if (!this.skipHidden.getValue() || this.isVisible(vec3d, blockCandidate.pos())) {
                return blockCandidate.pos();
            }

            if (System.nanoTime() - l > 60000000L) {
                break;
            }
        }

        return null;
    }

    private float getBreakSpeed(BlockPos blockPos, AutoTool autoTool) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        float f = blockState.getHardness(mc.world, blockPos);
        if (f == 0.0F) {
            return Float.MAX_VALUE;
        } else {
            return autoTool != null ? autoTool.getBestBreakSpeed(blockState) / f : blockState.calcBlockBreakingDelta(mc.player, mc.world, blockPos);
        }
    }

    private boolean isBreakable(BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);
        return blockPos.getY() < mc.player.getBlockY()
            ? false
            : !blockState.isAir() && !(blockState.getBlock() instanceof FluidBlock) && blockState.getHardness(mc.world, blockPos) >= 0.0F;
    }

    private boolean isInRange(Vec3d vec3d, BlockPos blockPos, double d) {
        double d2 = MathHelper.clamp(vec3d.x, blockPos.getX(), blockPos.getX() + 1.0);
        double d3 = MathHelper.clamp(vec3d.y, blockPos.getY(), blockPos.getY() + 1.0);
        double d4 = MathHelper.clamp(vec3d.z, blockPos.getZ(), blockPos.getZ() + 1.0);
        return vec3d.squaredDistanceTo(d2, d3, d4) <= d * d;
    }

    private boolean isVisible(Vec3d vec3d, BlockPos blockPos) {
        for (double[] doubles : HIT_OFFSETS) {
            Vec3d vec3d2 = new Vec3d(blockPos.getX() + doubles[0], blockPos.getY() + doubles[1], blockPos.getZ() + doubles[2]);
            BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
            if (blockHitResult.getType() == Type.MISS || blockHitResult.getBlockPos().equals(blockPos)) {
                return true;
            }
        }

        return false;
    }

    private Direction getHitSide(Vec3d vec3d, BlockPos blockPos) {
        return Direction.getFacing(vec3d.subtract(Vec3d.ofCenter(blockPos)));
    }

    private void resetBreaking() {
        if (this.currentTarget != null) {
            if (mc.interactionManager != null) {
                mc.interactionManager.cancelBlockBreaking();
            }

            this.currentTarget = null;
        }
    }

    record BlockCandidate(BlockPos pos, float speed, double distance) {
    }

    public enum BlockListType {
        WHITE,
        BLACK;
    }
}
