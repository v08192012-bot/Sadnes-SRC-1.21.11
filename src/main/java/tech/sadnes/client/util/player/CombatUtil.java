package tech.sadnes.client.util.player;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Vector4f;
import tech.sadnes.api.mixin.accessor.PlayerInventoryAccessor;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.feature.component.WindowClickTrackerComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.AutoTotem;
import tech.sadnes.client.feature.impl.combat.Criticals;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.feature.impl.movement.Airstuck;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.compat.ProtocolUtil;
import tech.sadnes.client.util.math.Lerp;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.rotation.SensitivityUtil;

public final class CombatUtil implements IMinecraft {
    public static final Random RANDOM = new Random();
    public static final StopWatch STOP_WATCH = new StopWatch();

    private CombatUtil() {
    }

    public static Vec3d clampToBox(Vec3d vec3d, Box box) {
        return new Vec3d(
            MathHelper.clamp(vec3d.x, box.minX, box.maxX), MathHelper.clamp(vec3d.y, box.minY, box.maxY), MathHelper.clamp(vec3d.z, box.minZ, box.maxZ)
        );
    }

    public static Vec3d applyWobble(Vec3d vec3d) {
        long l = System.currentTimeMillis();
        double d = Math.sin(l / 200.0) * 0.05 + Math.cos(l / 300.0) * 0.025;
        double d2 = Math.cos(l / 250.0) * 0.05 + Math.sin(l / 150.0) * 0.025;
        double d3 = Math.sin(l / 220.0) * 0.05;
        return vec3d.add(d, d2, d3);
    }

    public static Vec3d applyJitter(Vec3d vec3d) {
        Vec3d vec3d2 = new Vec3d(RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextGaussian()).multiply(0.015);
        return vec3d.add(vec3d2);
    }

    public static Vec3d getClosestPoint(LivingEntity livingEntity) {
        if (mc.player != null && livingEntity != null) {
            Vec3d vec3d = mc.player.getEyePos();
            return clampToBox(vec3d, livingEntity.getBoundingBox());
        } else {
            return Vec3d.ZERO;
        }
    }

    public static Vec3d getClosestPoint(Vec3d vec3d, Entity entity) {
        return clampToBox(vec3d, entity.getBoundingBox());
    }

    public static Vec3d getClosestPointOffset(Entity entity) {
        if (mc.player == null) {
            return Vec3d.ZERO;
        }

        Vec3d vec3d = mc.player.getEyePos();
        return getClosestPoint(vec3d, entity).subtract(vec3d);
    }

    public static double getDistanceToBox(Entity entity) {
        return getClosestPointOffset(entity).length();
    }

    public static double getDistanceToBox(LivingEntity livingEntity) {
        return getClosestPointOffset(livingEntity).length();
    }

    public static double getDistance(Entity entity) {
        return getDistanceToBox(entity);
    }

    public static double getDistance(LivingEntity livingEntity) {
        return getDistanceToBox(livingEntity);
    }

    public static double getRayDistance(Entity entity) {
        if (mc.player != null) {
            float f = mc.player.getYaw();
            float f2 = mc.player.getPitch();
            long l = Double.doubleToRawLongBits(getRayDistance(entity, f, f2));
            return Double.longBitsToDouble(l);
        } else {
            return Double.MAX_VALUE;
        }
    }

    public static double getRayDistance(Entity entity, float f, float f2) {
        if (entity != null && mc.player != null) {
            AttackAura attackAura = AttackAura.getInstance();
            if (attackAura != null) {
                attackAura.getAttackRange();
            } else {
                double d = 3.0;
            }

            if (attackAura != null) {
                double d2 = attackAura.getPreRangeSetting().getValue().floatValue();
            } else {
                double d3 = 0.0;
            }

            Vec3d vec3d = mc.player.getEyePos();
            Vec3d vec3d2 = RaycastUtil.getDirection(f, f2);
            Vec3d vec3d3 = vec3d.add(vec3d2.multiply(100.0));
            return entity.getBoundingBox()
                .expand(entity.getTargetingMargin())
                .raycast(vec3d, vec3d3)
                .map(vec3d::distanceTo)
                .orElseGet(() -> getClosestPoint(vec3d, entity).distanceTo(vec3d));
        } else {
            return Double.MAX_VALUE;
        }
    }

    public static Vec3d getAimPoint(LivingEntity livingEntity, float f) {
        if (mc.player != null && livingEntity != null) {
            double d = MathHelper.clamp(mc.player.getEyeY() - livingEntity.getEyeY(), livingEntity.getHeight() / 4.0, livingEntity.getHeight() / 3.0)
                / (mc.player.isGliding() ? 10.0 : (!mc.options.jumpKey.isPressed() && mc.player.isOnGround() ? (livingEntity.isSneaking() ? 0.8F : 0.6F) : 1.0F));
            Vec3d vec3d = new Vec3d(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ()).add(0.0, d, 0.0);
            return vec3d.add(f, f / 2.0F, f);
        } else {
            return Vec3d.ZERO;
        }
    }

    public static Vector4f getRotationsTo(Entity entity) {
        return getRotationsTo(entity, 3.1F, true);
    }

    public static Vector4f getRotationsTo(Entity entity, float f, boolean flag) {
        if (mc.player != null && entity != null) {
            Vec3d vec3d = AimPointUtil.getAimPoint(entity, f, flag);
            Vec3d vec3d2 = vec3d.subtract(mc.player.getEyePos()).normalize();
            float f2 = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec3d2.z, vec3d2.x)) - 90.0);
            float f3 = (float)(-Math.toDegrees(Math.atan2(vec3d2.y, Math.sqrt(vec3d2.x * vec3d2.x + vec3d2.z * vec3d2.z))));
            float f4 = MathHelper.wrapDegrees(f2 - mc.player.getYaw());
            float f5 = f3 - mc.player.getPitch();
            return new Vector4f(f2, f3, f4, f5);
        } else {
            return new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
        }
    }

    public static Vector4f getClosestRotations(LivingEntity livingEntity) {
        if (mc.player != null && livingEntity != null) {
            Vec3d vec3d = getClosestPoint(livingEntity).subtract(mc.player.getEyePos());
            float f = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec3d.z, vec3d.x)) - 90.0);
            float f2 = (float)(-Math.toDegrees(Math.atan2(vec3d.y, Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z))));
            float f3 = MathHelper.wrapDegrees(f - Rotation.getCameraYaw());
            float f4 = f2 - Rotation.getCameraPitch();
            return new Vector4f(f, f2, f3, f4);
        } else {
            return new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
        }
    }

    public static boolean isInAttackRange(Entity entity, double d, boolean flag) {
        if (entity != null) {
            if (mc.player != null) {
                float f = mc.player.getYaw();
                float f2 = mc.player.getPitch();
                AttackAura attackAura = AttackAura.getInstance();
                if (attackAura != null && attackAura.isHitSharedVehicle() && mc.player.hasVehicle()) {
                    Entity entity2 = entity.getRootVehicle();
                    Entity entity3 = mc.player.getRootVehicle();
                    if (entity2 == entity3) {
                        Box box = entity.getBoundingBox();
                        double d2 = entity.getTargetingMargin();
                        Box box2 = box.expand(d2);
                        return RaycastUtil.rayIntersectsBox(entity, box2, f, f2, d, flag);
                    }
                }

                return RaycastUtil.isLookingAtSafe(entity, f, f2, d, flag);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean isLookingAt(LivingEntity livingEntity, double d) {
        if (mc.player != null) {
            if (livingEntity != null) {
                float f = MathHelper.wrapDegrees(mc.player.getYaw());
                float f2 = mc.player.getPitch();
                float f3 = (float)d;
                return RaycastUtil.isLookingAt(livingEntity, f, f2, f3, true);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean canAttack(
        LivingEntity livingEntity, boolean flag, boolean flag2, boolean flag3, boolean flag4, boolean flag5, long l, float f, boolean flag6
    ) {
        if (flag2 && livingEntity != null) {
            double d = getRayDistance(livingEntity);
            double d2 = f;
            if (d > d2) {
                return false;
            }
        }

        if (AttackTimer.hasCooldownPassed(l)) {
            if (!WindowClickTrackerComponent.getInstance().isInteracting()) {
                boolean flag7 = CriticalUtil.isBestMoment(flag4, flag3, flag5);
                boolean flag8 = !flag7;
                boolean flag9;
                if (flag) {
                    if (flag8) {
                        flag9 = flag7;
                    } else {
                        double d3 = f;
                        boolean flag10 = isInAttackRange(livingEntity, d3, flag6);
                        if (flag10) {
                            flag9 = flag7;
                        } else {
                            flag9 = flag10;
                        }
                    }
                } else {
                    flag9 = flag7;
                }

                return flag9;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean canAttack(LivingEntity livingEntity, boolean flag, boolean flag2, boolean flag3, boolean flag4, long l, float f, boolean flag5) {
        return canAttack(livingEntity, flag, true, flag2, flag3, flag4, l, f, flag5);
    }

    public static boolean canAttack(LivingEntity livingEntity, boolean flag, boolean flag2, boolean flag3, boolean flag4, long l, float f) {
        return canAttack(livingEntity, flag, true, flag2, flag3, flag4, l, f, true);
    }

    public static boolean isCooldownReady() {
        return mc.player == null ? false : mc.player.getAttackCooldownProgress(1.5F) >= 0.93F;
    }

    public static long getAttackDelay() {
        return AttackTimer.getCooldownMs();
    }

    public static boolean isAttackReady() {
        return AttackTimer.isReady();
    }

    public static boolean isAttackReady(long l) {
        return AttackTimer.hasCooldownPassed(l);
    }

    public static StopWatch getAttackTimer() {
        return AttackTimer.getTimer();
    }

    public static double getCeilingGap(int i) {
        if (mc.world != null) {
            if (mc.player == null) {
                return 1.0;
            }

            long l;
            label44: {
                Vec3d vec3d = mc.player.getEyePos();
                float f = mc.player.getWidth();
                Vec3d[] vec3ds = new Vec3d[4];
                float f2 = ExactMath.add(ExactMath.multiply(f, 0.5F), -0.01F);
                double d = Float.intBitsToFloat(Integer.MIN_VALUE ^ Float.floatToRawIntBits(f2));
                Vec3d vec3d2 = vec3d.add(d, 0.0, d);
                Array.set(vec3ds, 0, vec3d2);
                double d2 = f2;
                Vec3d vec3d3 = vec3d.add(d2, 0.0, d2);
                Array.set(vec3ds, 1, vec3d3);
                Vec3d vec3d4 = vec3d.add(d2, 0.0, d);
                Array.set(vec3ds, 2, vec3d4);
                Vec3d vec3d5 = vec3d.add(d, 0.0, d2);
                Array.set(vec3ds, 3, vec3d5);
                Iterator iterator2 = Arrays.asList(vec3ds).iterator();
                boolean flag = iterator2.hasNext();
                double d3 = i;
                double d4 = ExactMath.add(d3, d3);
                if (flag) {
                    double d5 = 0 - i;
                    long l2 = 4643176031446892544L;
                    long l3 = -4589168020290535424L;
                    long l4 = Double.doubleToRawLongBits(d4);

                    while (true) {
                        Vec3d vec3d6 = (Vec3d)iterator2.next();
                        ClientWorld clientWorld = mc.world;
                        Vec3d vec3d7 = vec3d6.add(0.0, d5, 0.0);
                        ShapeType shapeType = ShapeType.VISUAL;
                        FluidHandling fluidHandling = FluidHandling.ANY;
                        ClientPlayerEntity clientPlayerEntity = mc.player;
                        RaycastContext raycastContext = new RaycastContext(vec3d6, vec3d7, shapeType, fluidHandling, clientPlayerEntity);
                        BlockHitResult blockHitResult = clientWorld.raycast(raycastContext);
                        ClientWorld clientWorld2 = mc.world;
                        Vec3d vec3d8 = vec3d6.add(0.0, d3, 0.0);
                        ShapeType shapeType2 = ShapeType.VISUAL;
                        FluidHandling fluidHandling2 = FluidHandling.ANY;
                        ClientPlayerEntity clientPlayerEntity2 = mc.player;
                        RaycastContext raycastContext2 = new RaycastContext(vec3d6, vec3d8, shapeType2, fluidHandling2, clientPlayerEntity2);
                        BlockHitResult blockHitResult2 = clientWorld2.raycast(raycastContext2);
                        double d6 = blockHitResult2.getPos().y;
                        if (Double.longBitsToDouble(l2) > d6) {
                            l2 = Double.doubleToRawLongBits(blockHitResult2.getPos().y);
                        }

                        double d7 = blockHitResult.getPos().y;
                        double d8 = Double.longBitsToDouble(l3);
                        if (d7 > d8) {
                            l3 = Double.doubleToRawLongBits(blockHitResult.getPos().y);
                        }

                        boolean flag2 = iterator2.hasNext();
                        double d9 = Double.longBitsToDouble(l2);
                        double d10 = Double.longBitsToDouble(l3);
                        d4 = ExactMath.subtract(d9, d10);
                        double d11 = Double.longBitsToDouble(l4);
                        if (d4 < d11) {
                            if (!flag2) {
                                break;
                            }
                        } else {
                            d4 = d11;
                            if (!flag2) {
                                l = Double.doubleToRawLongBits(d4);
                                break label44;
                            }
                        }

                        l4 = Double.doubleToRawLongBits(d4);
                    }
                }

                l = Double.doubleToRawLongBits(d4);
            }

            double d12 = mc.player.getHeight();
            return Double.longBitsToDouble(Double.doubleToRawLongBits(ExactMath.subtract(Double.longBitsToDouble(l), d12)));
        } else {
            return 1.0;
        }
    }

    public static boolean isUnderCeiling() {
        double d = getCeilingGap(3);
        return d == 1.2F || d == 1.2625F || d == 0.70000005F;
    }

    public static boolean isUnderLowCeiling() {
        return getCeilingGap(2) == 0.20000005F;
    }

    public static boolean attack(LivingEntity livingEntity, Runnable runnable, Runnable runnable2, Runnable runnable3, Runnable runnable4, Hand hand) {
        AttackAura attackAura = AttackAura.getInstance();
        boolean flag;
        if (attackAura != null) {
            flag = attackAura.getRotationMode().is("zaporozhets");
        } else {
            flag = false;
        }

        boolean flag2 = Airstuck.getInstance().enabled();
        boolean flag3;
        if (flag) {
            flag3 = true;
        } else {
            flag3 = flag2;
        }

        double d = attackAura.getAttackRange();
        boolean flag4 = attackAura.getThroughWallsSetting().getValue();
        return !flag3 ? attack(livingEntity, runnable, runnable2, runnable3, runnable4, hand, false, d, flag4) : attack(livingEntity, runnable, runnable2, runnable3, runnable4, hand, true, d, flag4);
    }

    public static boolean attack(
        LivingEntity livingEntity, Runnable runnable, Runnable runnable2, Runnable runnable3, Runnable runnable4, Hand hand, boolean flag, double d, boolean flag2
    ) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        boolean flag3 = runnable2 == null;
        boolean flag4 = runnable4 != null;
        boolean flag5 = runnable3 == null;
        boolean flag6 = flag;
        boolean flag7 = runnable != null;
        boolean flag8 = livingEntity != null;
        if (clientPlayerEntity != null) {
            if (mc.interactionManager == null) {
                return false;
            }

            if (flag8) {
                if (flag7) {
                    runnable.run();
                }

                label87: {
                    label74: {
                        if (isInAttackRange(livingEntity, d, flag2)) {
                            if (flag5) {
                                break label74;
                            }
                        } else {
                            if (!flag6) {
                                break label87;
                            }

                            if (flag5) {
                                break label74;
                            }
                        }

                        runnable3.run();
                    }

                    ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
                    ClientPlayerEntity clientPlayerEntity2 = mc.player;
                    clientPlayerInteractionManager.attackEntity(clientPlayerEntity2, livingEntity);
                    mc.player.swingHand(hand);
                    CriticalUtil.delayInitialized = false;
                    if (flag4) {
                        runnable4.run();
                    }

                    AttackTimer.onAttack();
                }

                if (!flag3) {
                    runnable2.run();
                    return true;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean isCritReady(LivingEntity livingEntity, float f) {
        if (mc.player != null) {
            if (livingEntity != null) {
                long l = AttackTimer.getAttackOffset();
                if (canAttack(livingEntity, false, false, false, false, false, l, f, true)) {
                    if (!mc.player.isOnGround()) {
                        ClientPlayerEntity clientPlayerEntity = mc.player;
                        TagKey tagKey = FluidTags.WATER;
                        if (!clientPlayerEntity.isSubmergedIn(tagKey)) {
                            double d = mc.player.getVelocity().y;
                            return 0.0030162615090425808 >= d;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean isMovingTowards(Entity entity) {
        if (mc.player == null) {
            return false;
        }

        Vec3d vec3d = mc.player.getVelocity();
        Vec3d vec3d2 = new Vec3d(entity.getX(), entity.getY(), entity.getZ()).subtract(new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        return vec3d.x * vec3d2.x + vec3d.z * vec3d2.z > 0.0;
    }

    public static Runnable[] getSprintResetActions(boolean flag) {
        Runnable[] runnables = new Runnable[2];
        Runnable runnable = () -> {};
        Array.set(runnables, 0, runnable);
        Runnable runnable2 = () -> {};
        Array.set(runnables, 1, runnable2);
        ClientPlayerEntity clientPlayerEntity = mc.player;
        if (flag) {
            if (clientPlayerEntity != null && mc.player.networkHandler != null) {
                if (mc.player.isSwimming()) {
                    mc.player.isSprinting();
                    return runnables;
                }

                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                TagKey tagKey = FluidTags.WATER;
                if (clientPlayerEntity2.isSubmergedIn(tagKey)) {
                    mc.player.isSprinting();
                    return runnables;
                }

                ClientPlayerEntity clientPlayerEntity3 = mc.player;
                TagKey tagKey2 = FluidTags.LAVA;
                if (clientPlayerEntity3.isSubmergedIn(tagKey2)) {
                    mc.player.isSprinting();
                    return runnables;
                }

                if (mc.player.isGliding()) {
                    mc.player.isSprinting();
                    return runnables;
                }

                if (mc.player.isSprinting()) {
                    Runnable runnable3 = () -> {
                        mc.player.setSprinting(false);
                        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                    };
                    Array.set(runnables, 0, runnable3);
                    Runnable runnable4 = () -> {
                        mc.player.setSprinting(true);
                        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_SPRINTING));
                    };
                    Array.set(runnables, 1, runnable4);
                    return runnables;
                }
            }
        } else if (clientPlayerEntity != null && mc.player.networkHandler != null) {
            if (mc.player.isSwimming()) {
                mc.player.isSprinting();
                return runnables;
            }

            ClientPlayerEntity clientPlayerEntity4 = mc.player;
            TagKey tagKey3 = FluidTags.WATER;
            if (clientPlayerEntity4.isSubmergedIn(tagKey3)) {
                mc.player.isSprinting();
                return runnables;
            }

            ClientPlayerEntity clientPlayerEntity5 = mc.player;
            TagKey tagKey4 = FluidTags.LAVA;
            if (clientPlayerEntity5.isSubmergedIn(tagKey4)) {
                mc.player.isSprinting();
                return runnables;
            }

            mc.player.isGliding();
            mc.player.isSprinting();
            return runnables;
        }

        return runnables;
    }

    public static double getRotationDistance(LivingEntity livingEntity) {
        Vector4f vector4f = getRotationsTo(livingEntity);
        return Math.sqrt(vector4f.z * vector4f.z + vector4f.w * vector4f.w);
    }

    public static double getLookRotationDistance(LivingEntity livingEntity) {
        Vector4f vector4f = getLookRotations(livingEntity);
        return Math.sqrt(vector4f.z * vector4f.z + vector4f.w * vector4f.w);
    }

    public static double getCenterRotationDistance(LivingEntity livingEntity) {
        if (mc.player != null && livingEntity != null) {
            Vec3d vec3d = new Vec3d(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ()).subtract(mc.player.getEyePos()).normalize();
            float f = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec3d.z, vec3d.x)) - 90.0);
            float f2 = (float)(-Math.toDegrees(Math.atan2(vec3d.y, Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z))));
            float f3 = MathHelper.wrapDegrees(f - Rotation.getCameraYaw());
            float f4 = f2 - Rotation.getCameraPitch();
            return Math.sqrt(f3 * f3 + f4 * f4);
        } else {
            return 0.0;
        }
    }

    public static Vec3d getServerLookVector() {
        float f = (float)Math.toRadians(Rotation.getCameraYaw());
        float f2 = (float)Math.toRadians(Rotation.getCameraPitch());
        double d = -Math.cos(f2) * Math.sin(f);
        double d2 = -Math.sin(f2);
        double d3 = Math.cos(f2) * Math.cos(f);
        return new Vec3d(d, d2, d3);
    }

    public static Vec3d getLookVector() {
        float f = (float)Math.toRadians(MathHelper.wrapDegrees(mc.player.getYaw()));
        float f2 = (float)Math.toRadians(mc.player.getPitch());
        double d = -Math.cos(f2) * Math.sin(f);
        double d2 = -Math.sin(f2);
        double d3 = Math.cos(f2) * Math.cos(f);
        return new Vec3d(d, d2, d3);
    }

    public static Vector4f getLookRotations(LivingEntity livingEntity) {
        if (mc.player != null && livingEntity != null) {
            Vec3d vec3d = mc.player.getEyePos();
            Vec3d vec3d2 = getServerLookVector();
            double d = mc.player.distanceTo(livingEntity);
            Vec3d vec3d3 = vec3d.add(vec3d2.multiply(d));
            Vec3d vec3d4 = clampToBox(vec3d3, livingEntity.getBoundingBox().expand(-0.1F));
            Vec3d vec3d5 = vec3d4.subtract(vec3d);
            float f = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec3d5.z, vec3d5.x)) - 90.0);
            float f2 = (float)(-Math.toDegrees(Math.atan2(vec3d5.y, Math.sqrt(vec3d5.x * vec3d5.x + vec3d5.z * vec3d5.z))));
            float f3 = MathHelper.wrapDegrees(f - Rotation.getCameraYaw());
            float f4 = f2 - Rotation.getCameraPitch();
            return new Vector4f(f, f2, f3, f4);
        } else {
            return new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
        }
    }

    public static double getLookDistance(LivingEntity livingEntity) {
        return getLookRotationDistance(livingEntity);
    }

    public static boolean randomBoolean() {
        return RANDOM.nextInt(2) == 1;
    }

    public static float randomFloat(float f, float f2) {
        return RANDOM.nextFloat(f, f2);
    }

    public static double randomDouble(double d, double d2) {
        return RANDOM.nextDouble(d, d2);
    }

    public static int randomInt(int i, int i2) {
        return RANDOM.nextInt(i, i2);
    }

    public static long randomLong(long l, long l2) {
        return RANDOM.nextLong(l, l2);
    }

    public static float randomFloat() {
        return randomFloat(0.0F, 1.0F);
    }

    public static int randomSign() {
        return randomBoolean() ? 1 : -1;
    }

    public static float randomLerp(float f, float f2) {
        return Lerp.lerp(f2, f, RANDOM.nextFloat());
    }

    public static AttackAura.AttackHooks createSnapAttack(boolean flag, LivingEntity livingEntity, float f, float f2) {
        if (livingEntity != null && flag && mc.player != null) {
            float f3 = mc.player.getYaw();
            float f4 = mc.player.getPitch();
            float f5 = ExactMath.add(MathHelper.wrapDegrees(f3), 180.0F);
            int i;
            if (f5 > 100.0F && 200.0F > f5) {
                i = -1035468800;
            } else if (f5 >= 200.0F) {
                if (240.0F > f5) {
                    i = 1112014848;
                } else {
                    i = 1103626240;
                }
            } else {
                i = 1103626240;
            }

            Float floatValue = f3;
            Float floatValue2 = f4;
            float f6 = floatValue;
            float f7 = floatValue2;
            Runnable runnable = () -> sendRotation(f6 + 0.01F, f7 + 0.01F);
            Float floatValue3 = f;
            float f8 = Float.intBitsToFloat(i);
            Float floatValue4 = ExactMath.add(f3, f8);
            Float floatValue5 = f2;
            Float floatValue6 = Float.intBitsToFloat(Float.floatToRawIntBits(f4) ^ Integer.MIN_VALUE);
            float f9 = floatValue3;
            float f10 = floatValue4;
            float f11 = floatValue5;
            float f12 = floatValue6;
            Runnable runnable2 = () -> {
                for (float f13 = 0.2F; f13 < 1.0F; f13 += 0.2F) {
                    sendRotation(MathHelper.lerp(f13, f9, f10) + randomLerp(-5.0F, 5.0F), MathHelper.lerp(f13, f11, f12) + randomLerp(-5.0F, 5.0F));
                }

                sendRotation(f10 + randomLerp(-0.001F, 0.001F), f12 + randomLerp(-0.001F, 0.001F));
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(livingEntity, false, Hand.MAIN_HAND, Vec3d.ZERO));
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(livingEntity, false, Hand.MAIN_HAND));
            };
            return new AttackAura.AttackHooks(runnable2, runnable);
        } else {
            return AttackAura.AttackHooks.NONE;
        }
    }

    public static AttackAura.AttackHooks createRotationPackets(boolean flag, LivingEntity livingEntity) {
        if (flag) {
            Runnable runnable = () -> PacketSender.sendSilent(
                new Full(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.isOnGround(),
                    mc.player.horizontalCollision
                )
            );
            Runnable runnable2 = () -> {
                Vector4f vector4f = getRotationsTo(livingEntity);
                float f = SensitivityUtil.snapDelta(mc.player.getYaw() + vector4f.z + randomLerp(-0.02F, 0.02F));
                float f2 = SensitivityUtil.snapDelta(mc.player.getPitch() + vector4f.w + randomLerp(-0.02F, 0.02F));
                Criticals criticals = Criticals.getInstance();
                boolean flag2;
                if (criticals != null) {
                    flag2 = criticals.isEnabled();
                } else {
                    flag2 = false;
                }

                float f3 = flag2 && !mc.player.isOnGround() ? 1.0E-6F : 0.0F;
                PacketSender.sendSilent(
                    new Full(mc.player.getX(), mc.player.getY() + f3, mc.player.getZ(), f, f2, mc.player.isOnGround(), mc.player.horizontalCollision)
                );
            };
            return new AttackAura.AttackHooks(runnable2, runnable);
        } else {
            return AttackAura.AttackHooks.NONE;
        }
    }

    public static AttackAura.AttackHooks createShieldBreakSwap(LivingEntity livingEntity, boolean flag) {
        if (flag && livingEntity instanceof PlayerEntity playerEntity) {
            Item item = playerEntity.getMainHandStack().getItem();
            Item item2 = Items.SHIELD;
            if (item != item2) {
                Item item3 = playerEntity.getOffHandStack().getItem();
                Item item4 = Items.SHIELD;
                if (item3 != item4) {
                    return AttackAura.AttackHooks.NONE;
                }
            }

            Slot slot = InventoryUtil.findAxe();
            if (slot != null) {
                int i = InventoryUtil.toHotbarIndex(slot.id);
                if (i != -1) {
                    int i2 = InventoryUtil.getSelectedSlot();
                    if (i != i2) {
                        Runnable runnable = () -> {
                            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i2));
                            ((PlayerInventoryAccessor)mc.player.getInventory()).sadnes$setSelectedSlot(i2);
                        };
                        Runnable runnable2 = () -> {
                            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
                            ((PlayerInventoryAccessor)mc.player.getInventory()).sadnes$setSelectedSlot(i);
                        };
                        return new AttackAura.AttackHooks(runnable2, runnable);
                    }

                    return AttackAura.AttackHooks.NONE;
                }

                return AttackAura.AttackHooks.NONE;
            }
        }

        return AttackAura.AttackHooks.NONE;
    }

    public static AttackAura.AttackHooks createOffhandSwap(boolean flag) {
        boolean flag2 = mc.player.isSprinting();
        boolean flag3 = AutoTotem.getInstance().isInDanger();
        Slot slot = InventoryUtil.findBestDamageWeapon();
        if (!flag2) {
            boolean flag4 = false;
            if (flag && slot != null && !flag3) {
                Runnable runnable = () -> swapToOffhand(slot.id);
                Runnable runnable2 = () -> swapToOffhand(slot.id);
                return new AttackAura.AttackHooks(runnable2, runnable);
            }
        }

        return AttackAura.AttackHooks.NONE;
    }

    public static AttackAura.AttackHooks createMovementStop() {
        PlayerInput playerInput = ServerInputTracker.getLastInput();
        boolean flag;
        if (!playerInput.forward()) {
            if (playerInput.backward()) {
                flag = true;
            } else if (playerInput.left()) {
                flag = true;
            } else {
                flag = playerInput.right();
            }
        } else {
            flag = true;
        }

        if (ServerInputTracker.isInputSentThisTick()) {
            return AttackAura.AttackHooks.NONE;
        } else if (mc.player.getVehicle() instanceof AbstractBoatEntity && flag) {
            boolean flag2 = playerInput.jump();
            boolean flag3 = playerInput.sneak();
            boolean flag4 = playerInput.sprint();
            PlayerInput playerInput2 = new PlayerInput(false, false, false, false, flag2, flag3, flag4);
            Runnable runnable = () -> {};
            Runnable runnable2 = () -> ServerInputTracker.sendInput(playerInput2);
            return new AttackAura.AttackHooks(runnable2, runnable);
        } else {
            return AttackAura.AttackHooks.NONE;
        }
    }

    private static void sendRotation(float f, float f2) {
        mc.player
            .networkHandler
            .sendPacket(new Full(mc.player.getEntityPos(), f, MathHelper.clamp(f2, -90.0F, 90.0F), mc.player.isOnGround(), mc.player.horizontalCollision));
    }

    public static Slot findBestOffhandSlot() {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        PlayerScreenHandler playerScreenHandler2 = mc.player.playerScreenHandler;
        if (screenHandler == playerScreenHandler2) {
            double d = InventoryUtil.getAttackDamage(mc.player.getOffHandStack());
            int i = InventoryUtil.getSelectedSlot();
            Iterator iterator2 = InventoryUtil.getPlayerSlots().iterator();
            long l = Double.doubleToRawLongBits(d);
            Slot slot = null;

            while (iterator2.hasNext()) {
                Slot slot2 = (Slot)iterator2.next();
                int i2 = InventoryUtil.toHotbarIndex(slot2.id);
                if (i != i2) {
                    double d2 = InventoryUtil.getAttackDamage(slot2.getStack());
                    if (!(Double.longBitsToDouble(l) >= d2)) {
                        l = Double.doubleToRawLongBits(d2);
                        slot = slot2;
                    }
                }
            }

            return slot;
        } else {
            return null;
        }
    }

    private static void swapToOffhand(int i) {
        ThrowComponent throwComponent = ThrowComponent.getInstance();
        boolean flag = ProtocolUtil.isOlderThan("26.2");
        if (throwComponent != null && !flag) {
            Hand hand = Hand.OFF_HAND;
            throwComponent.swapToHand(i, hand);
        } else {
            ScreenHandler screenHandler = mc.player.currentScreenHandler;
            PlayerInput playerInput2 = mc.player.input.playerInput;
            PlayerInput playerInput3 = PlayerInput.DEFAULT;
            int i2;
            if (playerInput2.equals(playerInput3)) {
                i2 = 16777216;
            } else {
                i2 = 0;
                ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
                PlayerInput playerInput4 = PlayerInput.DEFAULT;
                PlayerInputC2SPacket playerInputC2SPacket = new PlayerInputC2SPacket(playerInput4);
                clientPlayNetworkHandler.sendPacket(playerInputC2SPacket);
            }

            ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
            int i3 = screenHandler.syncId;
            SlotActionType slotActionType = SlotActionType.SWAP;
            ClientPlayerEntity clientPlayerEntity = mc.player;
            clientPlayerInteractionManager.clickSlot(i3, i, 40, slotActionType, clientPlayerEntity);
            ClientPlayNetworkHandler clientPlayNetworkHandler2 = mc.player.networkHandler;
            int i4 = screenHandler.syncId;
            CloseHandledScreenC2SPacket closeHandledScreenC2SPacket = new CloseHandledScreenC2SPacket(i4);
            clientPlayNetworkHandler2.sendPacket(closeHandledScreenC2SPacket);
            if ((byte)(i2 >>> 24 & 0xFF & 1) == 0) {
                ClientPlayNetworkHandler clientPlayNetworkHandler3 = mc.player.networkHandler;
                PlayerInputC2SPacket playerInputC2SPacket2 = new PlayerInputC2SPacket(playerInput2);
                clientPlayNetworkHandler3.sendPacket(playerInputC2SPacket2);
            }
        }
    }
}
