package tech.sadnes.client.feature.impl.player;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.event.impl.UseItemEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.util.math.StopWatch;

@FeatureInfo(
    name = "OpenWalls",
    description = "feature.impl.player.openwalls",
    displayName = "feature.impl.player.openwalls",
    category = Category.PLAYER,
    subcategory = Subcategory.INTERACT
)
public final class OpenWalls extends Feature {
    private final Set<BlockPos> openedPositions = new HashSet<>();
    private final StopWatch interactTimer = new StopWatch();
    private boolean interacting;

    public static OpenWalls getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(OpenWalls.class);
    }

    public boolean hasTarget() {
        return this.interacting ? false : this.findContainer() != null;
    }

    @EventHandler
    public void onInteract(UseItemEvent useItemEvent) {
        UseItemEvent.Stage stage = useItemEvent.getStage();
        UseItemEvent.Stage stage2 = UseItemEvent.Stage.START;
        if (stage == stage2 && this.findContainer() != null) {
            useItemEvent.cancel();
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.interactTimer.hasPassed(200.0)) {
            BlockPos blockPos = this.findContainer();
            if (blockPos != null) {
                Vec3d vec3d = mc.player.getEyePos();
                double d = vec3d.x;
                int i = blockPos.getX();
                double d2 = vec3d.y;
                int i2 = blockPos.getY();
                double d3 = vec3d.z;
                int i3 = blockPos.getZ();
                double d4 = ExactMath.add(i, 0.5);
                float f = (float)ExactMath.subtract(d, d4);
                double d5 = ExactMath.add(i2, 0.5);
                float f2 = (float)ExactMath.subtract(d2, d5);
                double d6 = ExactMath.add(i3, 0.5);
                float f3 = (float)ExactMath.subtract(d3, d6);
                Direction direction = Direction.getFacing(f, f2, f3);
                int i4 = blockPos.getX();
                int i5 = direction.getOffsetX();
                int i6 = blockPos.getY();
                int i7 = direction.getOffsetY();
                int i8 = blockPos.getZ();
                int i9 = direction.getOffsetZ();
                double d7 = ExactMath.multiply(i5, 0.5);
                double d8 = ExactMath.add(i4, 0.5);
                double d9 = ExactMath.add(d7, d8);
                double d10 = ExactMath.multiply(i7, 0.5);
                double d11 = ExactMath.add(i6, 0.5);
                double d12 = ExactMath.add(d10, d11);
                double d13 = ExactMath.multiply(i9, 0.5);
                double d14 = ExactMath.add(i8, 0.5);
                double d15 = ExactMath.add(d13, d14);
                Vec3d vec3d2 = new Vec3d(d9, d12, d15);
                BlockHitResult blockHitResult = new BlockHitResult(vec3d2, direction, blockPos, false);
                ClientPlayerEntity clientPlayerEntity = mc.player;
                Hand hand = Hand.MAIN_HAND;
                clientPlayerEntity.swingHand(hand);
                this.interacting = true;
                ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                Hand hand2 = Hand.MAIN_HAND;
                clientPlayerInteractionManager.interactBlock(clientPlayerEntity2, hand2, blockHitResult);
                StopWatch stopWatch = this.interactTimer;
                this.interacting = false;
                stopWatch.reset();
                return;
            }
        }
    }

    private BlockPos findContainer() {
        if (this.isEnabled()) {
            if (mc.player == null) {
                return null;
            }

            if (mc.world == null) {
                return null;
            }

            boolean flag;
            if (mc.options.useKey.isPressed()) {
                flag = mc.player.isSneaking();
            } else {
                flag = true;
            }

            boolean flag2;
            if (!mc.player.isUsingItem()) {
                if (mc.currentScreen == null) {
                    flag2 = mc.player.getMainHandStack().getItem() instanceof BlockItem;
                } else {
                    flag2 = true;
                }
            } else {
                flag2 = true;
            }

            NoInteract noInteract = NoInteract.getInstance();
            if (noInteract != null) {
                if (noInteract.shouldBlockBlocks()) {
                    return null;
                }

                if (flag | flag2) {
                    return null;
                }
            } else if (flag | flag2) {
                return null;
            }

            Vec3d vec3d = mc.player.getEyePos();
            Vec3d vec3d2 = new Vec3d(0.0, 0.0, 0.1);
            float f = Float.intBitsToFloat(Float.floatToRawIntBits((float)Math.toRadians(mc.player.getPitch())) + Integer.MIN_VALUE);
            Vec3d vec3d3 = vec3d2.rotateX(f);
            float f2 = Float.intBitsToFloat(Float.floatToRawIntBits((float)Math.toRadians(mc.player.getYaw())) + Integer.MIN_VALUE);
            Vec3d vec3d4 = vec3d3.rotateY(f2);
            this.openedPositions.clear();
            int i = 1;

            while (true) {
                double d = i;
                Vec3d vec3d5 = vec3d4.multiply(d);
                BlockPos blockPos = BlockPos.ofFloored(vec3d.add(vec3d5));
                if (this.openedPositions.add(blockPos)) {
                    BlockEntity blockEntity = mc.world.getBlockEntity(blockPos);
                    if (blockEntity instanceof LockableContainerBlockEntity) {
                        return blockPos;
                    }

                    if (blockEntity instanceof EnderChestBlockEntity) {
                        return blockPos;
                    }

                    if (++i == 50) {
                        break;
                    }
                } else if (++i == 50) {
                    break;
                }
            }
        }

        return null;
    }
}
