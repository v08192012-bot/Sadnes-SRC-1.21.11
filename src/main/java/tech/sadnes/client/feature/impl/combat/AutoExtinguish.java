package tech.sadnes.client.feature.impl.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.AimPointUtil;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.RaycastUtil;
import tech.sadnes.client.util.rotation.RotationUtil;

@FeatureInfo(
    name = "AutoExtinguish",
    description = "feature.impl.combat.autoextinguish",
    displayName = "feature.impl.combat.autoextinguish",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_TWEAKS
)
public final class AutoExtinguish extends Feature {
    private final NumberSetting duration = new NumberSetting("autoextinguish.duration", 3.0F, 1.0F, 10.0F, 0.5F, Unit.SECOND);
    private final BooleanSetting aim = new BooleanSetting("autoextinguish.aim", false);
    private final NumberSetting batch = new NumberSetting("autoextinguish.batch", 3.0F, 1.0F, 10.0F, 1.0F).setVisibleWhen(() -> !this.aim.getValue() ? true : false);
    private final BooleanSetting throughWalls = new BooleanSetting("autoextinguish.throughWalls", false);
    private final StopWatch timer = new StopWatch();
    private Vec3d deathPos;

    @Override
    public void onEnable() {
        this.deathPos = null;
        super.onEnable();
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && packetEvent.getPacket() instanceof EntityStatusS2CPacket entityStatusS2CPacket && entityStatusS2CPacket.getStatus() == 3) {
            mc.execute(() -> this.handleEntityStatus(entityStatusS2CPacket));
        }
    }

    private void handleEntityStatus(EntityStatusS2CPacket entityStatusS2CPacket) {
        if (!ServerManager.isNullWorld()) {
            if (entityStatusS2CPacket.getEntity(mc.world) instanceof PlayerEntity playerEntity && playerEntity != mc.player) {
                double d = mc.player.getBlockInteractionRange() + 6.0;
                if (!(playerEntity.squaredDistanceTo(mc.player) > d * d)) {
                    this.deathPos = playerEntity.getEntityPos();
                    this.timer.reset();
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.deathPos != null) {
            if (ServerManager.isNullWorld()) {
                return;
            }

            ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
            StopWatch stopWatch = this.timer;
            NumberSetting numberSetting = this.duration;
            if (clientPlayerInteractionManager == null) {
                return;
            }

            double d = ExactMath.multiply(numberSetting.getValue().floatValue(), 1000.0);
            if (stopWatch.hasPassed(d)) {
                this.deathPos = null;
                return;
            }

            if (mc.player.isAlive() && !mc.player.isSpectator()) {
                float f = (float)mc.player.getBlockInteractionRange();
                List list = this.findFireBlocks(f);
                if (this.aim.getValue()) {
                    this.extinguishAimed(list, f);
                    return;
                }

                this.extinguishBatch(list, f);
                return;
            }
        }
    }

    private List<BlockPos> findFireBlocks(float f) {
        Vec3d vec3d = mc.player.getEyePos();
        int i = MathHelper.ceil(f);
        ArrayList<BlockPos> blockPoses = new ArrayList<>();

        for (BlockPos blockPos : BlockPos.iterateOutwards(BlockPos.ofFloored(vec3d), i, i, i)) {
            if (mc.world.getBlockState(blockPos).isIn(BlockTags.FIRE) && !(vec3d.squaredDistanceTo(CombatUtil.clampToBox(vec3d, new Box(blockPos))) > f * f)) {
                blockPoses.add(blockPos.toImmutable());
            }
        }

        blockPoses.sort(Comparator.comparingDouble(blockPos2 -> blockPos2.toBottomCenterPos().squaredDistanceTo(this.deathPos)));
        return blockPoses;
    }

    private void extinguishBatch(List<BlockPos> blockPoses, float f) {
        int i = this.batch.getValue().intValue();

        for (BlockPos blockPos : blockPoses) {
            if (i == 0) {
                return;
            }

            BlockHitResult blockHitResult = AimPointUtil.getBlockHit(blockPos, f, this.throughWalls.getValue());
            if (blockHitResult != null) {
                this.breakFire(blockPos, blockHitResult.getSide());
                i--;
            }
        }
    }

    private void extinguishAimed(List<BlockPos> blockPoses, float f) {
        if (!mc.player.isUsingItem()) {
            for (BlockPos blockPos : blockPoses) {
                Vec2f vec2f = this.getRotationTo(blockPos, f);
                if (vec2f != null) {
                    RotationComponent.rotateTo(new Rotation(vec2f.x, vec2f.y), 180.0F, 90.0F, 2, 10);
                    BlockHitResult blockHitResult = this.raycastFire(blockPos, mc.player.getYaw(), mc.player.getPitch(), f);
                    if (blockHitResult != null) {
                        this.breakFire(blockHitResult.getBlockPos(), blockHitResult.getSide());
                    }
                    break;
                }
            }
        }
    }

    private Vec2f getRotationTo(BlockPos blockPos, float f) {
        BlockHitResult blockHitResult = AimPointUtil.getBlockHit(blockPos, f, this.throughWalls.getValue());
        if (blockHitResult == null) {
            return null;
        }

        Axis axis = blockHitResult.getSide().getAxis();
        Vec3d vec3d = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos).getBoundingBox().offset(blockPos).getCenter();
        Vec3d vec3d2 = blockHitResult.getPos().lerp(vec3d.withAxis(axis, blockHitResult.getPos().getComponentAlongAxis(axis)), 0.5);

        for (Vec3d vec3d3 : List.of(vec3d2, blockHitResult.getPos())) {
            Vec2f vec2f = RotationUtil.getRotations(mc.player.getEyePos(), vec3d3);
            if (this.raycastFire(blockPos, vec2f.x, vec2f.y, f) != null) {
                return vec2f;
            }
        }

        return null;
    }

    private BlockHitResult raycastFire(BlockPos blockPos, float f, float f2, float f3) {
        if (this.throughWalls.getValue()) {
            return AimPointUtil.raycastBlock(blockPos, f, f2, f3);
        } else {
            return RaycastUtil.raycastBlocks(f, f2, f3, false) instanceof BlockHitResult blockHitResult
                    && blockHitResult.getType() == Type.BLOCK
                    && mc.world.getBlockState(blockHitResult.getBlockPos()).isIn(BlockTags.FIRE)
                ? blockHitResult
                : null;
        }
    }

    private void breakFire(BlockPos blockPos, Direction direction) {
        PacketSender.sendSequenced(i -> {
            mc.interactionManager.breakBlock(blockPos);
            return new PlayerActionC2SPacket(Action.START_DESTROY_BLOCK, blockPos, direction, i);
        });
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
