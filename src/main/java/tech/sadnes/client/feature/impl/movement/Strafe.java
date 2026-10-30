package tech.sadnes.client.feature.impl.movement;

import java.lang.reflect.Array;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.SendMovementPacketsEvent;
import tech.sadnes.client.event.impl.SprintEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.util.player.MoveUtil;

@FeatureInfo(
    name = "Strafe",
    description = "feature.impl.movement.strafe",
    displayName = "feature.impl.movement.strafe",
    category = Category.MOVEMENT,
    subcategory = Subcategory.SPEED
)
public final class Strafe extends Feature {
    private double speed;
    private double friction = 0.91;
    private double savedOptionValue;

    @Override
    public void onDisable() {
        super.onDisable();
        this.speed = 0.0;
        double d = mc.options.getFovEffectScale().getValue();
        this.savedOptionValue = d;
        SimpleOption simpleOption = mc.options.getFovEffectScale();
        Double doubleValue = 0.0;
        simpleOption.setValue(doubleValue);
    }

    @Override
    public void onEnable() {
        if (mc.options != null) {
            SimpleOption simpleOption = mc.options.getFovEffectScale();
            Double doubleValue = this.savedOptionValue;
            simpleOption.setValue(doubleValue);
        }

        super.onEnable();
    }

    @EventHandler
    public void onMove(MoveEvent moveEvent) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        double d = this.speed;
        if (clientPlayerEntity != null) {
            if (this.canStrafe()) {
                if (MoveUtil.isMoving()) {
                    double d2 = moveEvent.getY();
                    double[] doubles = MoveUtil.getMotionForInput(this.calculateSpeed(d2));
                    double d3 = Array.getDouble(doubles, 0);
                    moveEvent.setX(d3);
                    double d4 = Array.getDouble(doubles, 1);
                    moveEvent.setZ(d4);
                    moveEvent.setCancelled(true);
                    double d5 = Double.longBitsToDouble(Double.doubleToRawLongBits(this.speed));
                    this.speed = d5;
                } else {
                    this.speed = 0.0;
                    moveEvent.setX(0.0);
                    moveEvent.setZ(0.0);
                    moveEvent.setCancelled(true);
                    double d6 = Double.longBitsToDouble(Double.doubleToRawLongBits(this.speed));
                    this.speed = d6;
                }
            } else {
                this.speed = 0.0;
            }
        } else {
            long l = Double.doubleToRawLongBits(d);
            double d7 = Double.longBitsToDouble(l);
            this.speed = d7;
        }
    }

    @EventHandler
    public void onPostMove(SendMovementPacketsEvent sendMovementPacketsEvent) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        double d = this.speed;
        if (clientPlayerEntity != null) {
            double d2 = mc.player.getX();
            double d3 = mc.player.lastX;
            double d4 = mc.player.getZ();
            double d5 = mc.player.lastZ;
            double d6 = ExactMath.subtract(d2, d3);
            double d7 = ExactMath.subtract(d4, d5);
            double d8 = Math.hypot(d6, d7);
            double d9 = this.friction;
            double d10 = Double.longBitsToDouble(Double.doubleToRawLongBits(ExactMath.multiply(d8, d9)));
            this.speed = d10;
        } else {
            long l = Double.doubleToRawLongBits(d);
            double d11 = Double.longBitsToDouble(l);
            this.speed = d11;
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        boolean flag = packetEvent.isReceive();
        double d = this.speed;
        if (flag) {
            boolean flag2 = packetEvent.getPacket() instanceof PlayerPositionLookS2CPacket;
            double d2 = this.speed;
            if (flag2) {
                this.speed = 0.0;
            } else {
                double d3 = Double.longBitsToDouble(Double.doubleToRawLongBits(d2));
                this.speed = d3;
            }
        } else {
            long l = Double.doubleToRawLongBits(d);
            double d4 = Double.longBitsToDouble(l);
            this.speed = d4;
        }
    }

    @EventHandler
    public void onSprint(SprintEvent sprintEvent) {
        SprintEvent.Stage stage = sprintEvent.getStage();
        SprintEvent.Stage stage2 = SprintEvent.Stage.AFSPRINT;
        if (stage == stage2 && mc.player != null && this.canStrafe() && MoveUtil.isMoving()) {
            sprintEvent.setSprinting(true);
        }
    }

    private double calculateSpeed(double d) {
        float f;
        float f2;
        long l;
        int i;
        label44: {
            double d2;
            label47: {
                f = this.getBaseMoveSpeed();
                double d3 = mc.player.getX();
                double d4 = mc.player.getY();
                double d5 = mc.player.getZ();
                double d6 = ExactMath.subtract(d4, d);
                BlockPos blockPos = BlockPos.ofFloored(d3, d6, d5);
                f2 = mc.world.getBlockState(blockPos).getBlock().getSlipperiness();
                ClientPlayerEntity clientPlayerEntity = mc.player;
                RegistryEntry registryEntry = StatusEffects.JUMP_BOOST;
                boolean flag = clientPlayerEntity.hasStatusEffect(registryEntry);
                double d7 = this.speed;
                double d8 = this.friction;
                long l2;
                long l3;
                if (flag) {
                    boolean flag2 = mc.player.isUsingItem();
                    double d9 = this.speed;
                    double d10 = this.friction;
                    if (flag2) {
                        d2 = d10;
                        i = 1063339950;
                        break label47;
                    }

                    l2 = Double.doubleToRawLongBits(d10);
                    l3 = Double.doubleToRawLongBits(d9);
                } else {
                    l2 = Double.doubleToRawLongBits(d8);
                    l3 = Double.doubleToRawLongBits(d7);
                }

                if (!(Double.longBitsToDouble(l3) > 0.32)) {
                    l = l2;
                    i = 1063843267;
                    break label44;
                }

                double d11 = Double.longBitsToDouble(l2);
                this.friction = d11;
                boolean flag3 = mc.player.isUsingItem();
                d2 = this.friction;
                if (!flag3) {
                    l = Double.doubleToRawLongBits(d2);
                    i = 1063843267;
                    break label44;
                }

                i = 1063339950;
            }

            l = Double.doubleToRawLongBits(d2);
        }

        double d12 = Double.longBitsToDouble(l);
        this.friction = d12;
        boolean flag4 = mc.player.isOnGround();
        double d13 = this.friction;
        float f3;
        if (flag4) {
            f3 = ExactMath.multiply(f2, 0.91F);
        } else {
            f3 = Float.intBitsToFloat(i);
        }

        this.friction = d13;
        double d14 = f3;
        double d15 = Math.pow(d14, 3.0);
        boolean flag5 = mc.player.isOnGround();
        double d16 = this.speed;
        double d17 = this.friction;
        long l4;
        if (flag5) {
            float f4 = (float)ExactMath.divide(0.1631F, d15);
            float f5 = ExactMath.multiply(f, f4);
            if (d > 0.0) {
                f5 = ExactMath.add(f5, 0.2F);
            }

            l4 = Double.doubleToRawLongBits(f5);
        } else {
            l4 = 4583007095862853632L;
        }

        this.friction = d17;
        double d18 = ExactMath.add(Double.longBitsToDouble(l4), d16);
        double d19 = Math.max(0.25, d18);
        if ((byte)(mc.player.age & 0xFF & 1) != 0) {
            this.friction = d14;
            return ExactMath.subtract(d19, 0.002);
        } else {
            this.friction = d14;
            return ExactMath.subtract(d19, 0.001);
        }
    }

    private float getBaseMoveSpeed() {
        boolean flag = mc.player.isSprinting();
        mc.player.setSprinting(false);
        float f = mc.player.getMovementSpeed();
        mc.player.setSprinting(flag);
        return ExactMath.multiply(f, 1.3F);
    }

    private boolean canStrafe() {
        if (!mc.player.isSneaking()) {
            if (!mc.player.isInLava()) {
                return !mc.player.isSubmergedInWater() ? !mc.player.getAbilities().flying : false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
