package tech.sadnes.client.feature.impl.combat;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PreRotationEvent;
import tech.sadnes.client.event.impl.StopUsingItemEvent;
import tech.sadnes.client.event.impl.UseItemEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.player.CombatUtil;

@FeatureInfo(
    name = "AimBot",
    description = "feature.impl.combat.aimbot",
    displayName = "feature.impl.combat.aimbot",
    category = Category.COMBAT,
    subcategory = Subcategory.AURA
)
public final class AimBot extends Feature {
    private static final double ARROW_DRAG = 0.99;
    private static final double ARROW_GRAVITY = 0.05;
    private static final int MAX_SIM_TICKS = 400;
    private static final double PLAYER_GRAVITY = 0.08;
    private static final double PLAYER_DRAG = 0.98;
    private static final int HISTORY_SIZE = 8;
    private static final double MAX_ACCEL_OFFSET = 1.5;
    private static final double RESIDUAL_PENALTY = 8.0;
    private static final double RESIDUAL_TOLERANCE = 0.15;
    private final MultiSetting weapons = new MultiSetting(
        "aimbot.weapons", BooleanSetting.of("bow", true), BooleanSetting.of("crossbow", true), BooleanSetting.of("trident", true)
    );
    private final NumberSetting distance = new NumberSetting("aimbot.distance", 20.0F, 3.0F, 60.0F, 1.0F);
    private final NumberSetting fov = new NumberSetting("aimbot.fov", 60.0F, 10.0F, 180.0F, 1.0F);
    private final NumberSetting extrapolation = new NumberSetting("aimbot.extrapolation", 0.0F, 0.0F, 10.0F, 1.0F);
    private final BooleanSetting autoFire = new BooleanSetting("aimbot.autoFire", false);
    private boolean pendingRelease;
    private boolean releasing;
    private Hand pendingCrossbowHand;
    private boolean firingCrossbow;
    private boolean firedThisTick;
    private final Map<UUID, ArrayDeque<double[]>> positionHistory = new HashMap<>();
    private int lastRecordedAge = -1;

    public static AimBot getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(AimBot.class);
    }

    public boolean isFiring() {
        return this.isEnabled() && this.firedThisTick;
    }

    @EventHandler
    public void onStopUsingItem(StopUsingItemEvent stopUsingItemEvent) {
        if (!this.releasing && !stopUsingItemEvent.isCancelled() && mc.player != null && mc.world != null) {
            float f = this.getProjectileSpeed(stopUsingItemEvent.getStack());
            if (!(f <= 0.0F)) {
                PlayerEntity playerEntity = this.findTarget();
                if (playerEntity != null) {
                    Vector2f vector2f = this.computeAim(playerEntity, f, true);
                    if (vector2f != null) {
                        stopUsingItemEvent.cancel();
                        this.applyRotation(vector2f);
                        this.pendingRelease = true;
                    }
                }
            }
        }
    }

    @Override
    public void onEnable() {
        this.positionHistory.clear();
        this.pendingRelease = false;
        this.releasing = false;
        this.pendingCrossbowHand = null;
        this.firingCrossbow = false;
        this.firedThisTick = false;
        super.onEnable();
    }

    private void recordTargetPosition() {
        if (mc.player.age != this.lastRecordedAge) {
            this.lastRecordedAge = mc.player.age;
            PlayerEntity playerEntity = this.findTarget();
            if (playerEntity != null) {
                UUID uuid = playerEntity.getUuid();
                ArrayDeque arrayDeque = this.positionHistory.computeIfAbsent(uuid, uuid2 -> new ArrayDeque<>());
                arrayDeque.addLast(new double[]{playerEntity.getX(), playerEntity.getBoundingBox().minY, playerEntity.getZ()});

                while (arrayDeque.size() > 8) {
                    arrayDeque.removeFirst();
                }

                this.positionHistory.keySet().removeIf(uuid3 -> !uuid3.equals(uuid));
            }
        }
    }

    private double[] estimateVelocity(PlayerEntity playerEntity) {
        double d = playerEntity.isOnGround() ? 0.0 : playerEntity.getY() - playerEntity.lastY;
        ArrayDeque<double[]> doubleses = this.positionHistory.get(playerEntity.getUuid());
        if (doubleses != null && doubleses.size() >= 4) {
            int i = doubleses.size();
            double[] doubles = new double[i];
            double[] doubles2 = new double[i];
            int i2 = 0;

            for (double[] doubles3 : doubleses) {
                doubles[i2] = doubles3[0];
                doubles2[i2] = doubles3[2];
                i2++;
            }

            double[] doubles4 = this.fitQuadratic(doubles);
            double[] doubles5 = this.fitQuadratic(doubles2);
            return new double[]{doubles4[0], d, doubles5[0], doubles4[1], doubles5[1]};
        } else {
            return new double[]{playerEntity.getX() - playerEntity.lastX, d, playerEntity.getZ() - playerEntity.lastZ, 0.0, 0.0};
        }
    }

    private double[] fitQuadratic(double[] doubles) {
        int i = doubles.length;
        double d = i;
        double d2 = 0.0;
        double d3 = 0.0;
        double d4 = 0.0;
        double d5 = 0.0;
        double d6 = 0.0;
        double d7 = 0.0;
        double d8 = 0.0;

        for (int i2 = 0; i2 < i; i2++) {
            double d9 = i2 - (i - 1);
            double d10 = d9 * d9;
            d2 += d9;
            d3 += d10;
            d4 += d10 * d9;
            d5 += d10 * d10;
            d6 += doubles[i2];
            d7 += d9 * doubles[i2];
            d8 += d10 * doubles[i2];
        }

        double d11 = determinant3(d, d2, d3, d2, d3, d4, d3, d4, d5);
        if (Math.abs(d11) < 1.0E-9) {
            return new double[]{0.0, 0.0};
        }

        double d12 = determinant3(d6, d2, d3, d7, d3, d4, d8, d4, d5) / d11;
        double d13 = determinant3(d, d6, d3, d2, d7, d4, d3, d8, d5) / d11;
        double d14 = determinant3(d, d2, d6, d2, d3, d7, d3, d4, d8) / d11;
        double d15 = 0.0;

        for (int i3 = 0; i3 < i; i3++) {
            double d16 = i3 - (i - 1);
            double d17 = doubles[i3] - (d12 + d13 * d16 + d14 * d16 * d16);
            d15 += d17 * d17;
        }

        double d18 = Math.sqrt(d15 / i);
        double d19 = 1.0 / (1.0 + 8.0 * Math.max(0.0, d18 - 0.15));
        return new double[]{d13, 2.0 * d14 * d19};
    }

    private static double determinant3(double d, double d2, double d3, double d4, double d5, double d6, double d7, double d8, double d9) {
        return d * (d5 * d9 - d6 * d8) - d2 * (d4 * d9 - d6 * d7) + d3 * (d4 * d8 - d5 * d7);
    }

    private static double accelOffset(double d, double d2) {
        double d3 = 0.5 * d * d2 * d2;
        return Math.max(-1.5, Math.min(1.5, d3));
    }

    private static double predictY(double d, double d2, double d3, boolean flag, double d4) {
        if (flag && d3 <= 0.0) {
            return d;
        }

        double d5 = d;
        double d6 = d3;
        int i = (int)Math.floor(d4);

        for (int i2 = 0; i2 < i; i2++) {
            d5 += d6;
            d6 = (d6 - 0.08) * 0.98;
            if (d5 < d2) {
                d5 = d2;
                d6 = 0.0;
            }
        }

        double d7 = d5 + d6 * (d4 - i);
        return Math.max(d7, d2);
    }

    @EventHandler
    public void onTick(PreRotationEvent preRotationEvent) {
        if (mc.player != null && mc.world != null) {
            this.firedThisTick = false;
            this.recordTargetPosition();
            if (this.pendingRelease) {
                this.pendingRelease = false;
                this.releasing = true;
                mc.interactionManager.stopUsingItem(mc.player);
                this.releasing = false;
                this.firedThisTick = true;
            } else if (this.pendingCrossbowHand != null) {
                Hand hand = this.pendingCrossbowHand;
                this.pendingCrossbowHand = null;
                this.firingCrossbow = true;
                mc.interactionManager.interactItem(mc.player, hand);
                this.firingCrossbow = false;
            } else {
                Hand hand2 = this.getLoadedCrossbowHand();
                if (hand2 != null) {
                    if (this.autoFire.getValue()) {
                        PlayerEntity playerEntity = this.findTarget();
                        if (playerEntity != null) {
                            Vector2f vector2f = this.computeAim(playerEntity, 3.15F, false);
                            if (vector2f != null) {
                                this.applyRotation(vector2f);
                                this.pendingCrossbowHand = hand2;
                            }
                        }
                    }
                } else if (this.autoFire.getValue() && mc.player.isUsingItem()) {
                    ItemStack itemStack = mc.player.getActiveItem();
                    float f = this.getProjectileSpeed(itemStack);
                    if (!(f <= 0.0F)) {
                        PlayerEntity playerEntity2 = this.findTarget();
                        if (playerEntity2 != null) {
                            Vector2f vector2f2 = this.computeAim(playerEntity2, f, true);
                            if (vector2f2 != null) {
                                this.applyRotation(vector2f2);
                                if (itemStack.isOf(Items.TRIDENT) || mc.player.getItemUseTime() >= 20) {
                                    this.pendingRelease = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteractItem(UseItemEvent useItemEvent) {
        if (!this.firingCrossbow && useItemEvent.getStage() == UseItemEvent.Stage.START && mc.player != null && mc.world != null) {
            Hand hand = this.getLoadedCrossbowHand();
            if (hand != null && useItemEvent.getHand() == hand) {
                PlayerEntity playerEntity = this.findTarget();
                if (playerEntity != null) {
                    Vector2f vector2f = this.computeAim(playerEntity, 3.15F, false);
                    if (vector2f != null) {
                        useItemEvent.cancel();
                        this.applyRotation(vector2f);
                        this.pendingCrossbowHand = hand;
                    }
                }
            }
        }
    }

    private void applyRotation(Vector2f vector2f) {
        float f = mc.player.getYaw() + MathHelper.wrapDegrees(vector2f.x - mc.player.getYaw()) + CombatUtil.randomFloat(-0.02F, 0.02F);
        float f2 = mc.player.getPitch() + MathHelper.wrapDegrees(vector2f.y - mc.player.getPitch()) + CombatUtil.randomFloat(-0.02F, 0.02F);
        RotationComponent.rotateTo(new Rotation(f, f2), 360.0F, 90.0F, 1, 10);
    }

    private float getProjectileSpeed(ItemStack itemStack) {
        Item item = itemStack.getItem();
        int i = mc.player.getItemUseTime();
        if (item == Items.BOW && this.weapons.has("bow")) {
            float f = (i / 20.0F * i / 20.0F + i / 20.0F * 2.0F) / 3.0F;
            return f < 0.1F ? -1.0F : Math.min(f, 1.0F) * 3.0F;
        } else if (item == Items.TRIDENT && this.weapons.has("trident")) {
            return i >= 10 && EnchantmentHelper.getTridentSpinAttackStrength(itemStack, mc.player) == 0.0F ? 2.5F : -1.0F;
        } else {
            return -1.0F;
        }
    }

    private int getLatency(UUID uuid) {
        if (mc.getNetworkHandler() == null) {
            return 0;
        }

        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(uuid);
        return playerListEntry != null ? Math.max(playerListEntry.getLatency(), 0) : 0;
    }

    private Hand getLoadedCrossbowHand() {
        if (!this.weapons.has("crossbow")) {
            return null;
        }

        ItemStack itemStack = mc.player.getMainHandStack();
        if (itemStack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack)) {
            return Hand.MAIN_HAND;
        }

        ItemStack itemStack2 = mc.player.getOffHandStack();
        return itemStack2.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack2) ? Hand.OFF_HAND : null;
    }

    private PlayerEntity findTarget() {
        return mc.world
            .getPlayers()
            .stream()
            .filter(
                abstractClientPlayerEntity -> abstractClientPlayerEntity != mc.player
                    && abstractClientPlayerEntity.isAlive()
                    && !abstractClientPlayerEntity.isDead()
                    && abstractClientPlayerEntity.getHealth() > 0.0F
                    && !abstractClientPlayerEntity.isSpectator()
                    && !Sadnes.getInstance().getFriendManager().isFriend(abstractClientPlayerEntity.getGameProfile().name())
                    && mc.player.canSee(abstractClientPlayerEntity)
                    && CombatUtil.getCenterRotationDistance(abstractClientPlayerEntity) <= this.fov.getValue().floatValue()
                    && mc.player.distanceTo(abstractClientPlayerEntity) < this.distance.getValue()
            )
            .min(Comparator.comparingDouble(abstractClientPlayerEntity2 -> mc.player.distanceTo(abstractClientPlayerEntity2)))
            .orElse(null);
    }

    private Vector2f computeAim(PlayerEntity playerEntity, float f, boolean flag) {
        Vec3d vec3d = mc.player.getVelocity();
        boolean flag2 = !mc.player.isOnGround();
        double d = vec3d.x;
        double d2 = flag2 ? vec3d.y : 0.0;
        double d3 = vec3d.z;
        double d4 = mc.player.getX() + d;
        double d5 = mc.player.getEyeY() - 0.1 + d2;
        double d6 = mc.player.getZ() + d3;
        double d7 = 0.0;
        double d8 = 0.0;
        double d9 = 0.0;
        if (flag) {
            d7 = d;
            d8 = flag2 ? (d2 - 0.08) * 0.98 : 0.0;
            d9 = d3;
        }

        Box box = playerEntity.getBoundingBox();
        double d10 = box.minY;
        double d11 = (box.maxY - box.minY) / 2.0;
        double d12 = playerEntity.getX();
        double d13 = playerEntity.getZ();
        double[] doubles = this.estimateVelocity(playerEntity);
        double d14 = doubles[0];
        double d15 = doubles[1];
        double d16 = doubles[2];
        double d17 = doubles[3];
        double d18 = doubles[4];
        double d19 = this.getLatency(mc.player.getUuid()) / 50.0 + this.getLatency(playerEntity.getUuid()) / 100.0 + this.extrapolation.getValue().floatValue() + 1.0;
        double d20 = Math.hypot(d12 - d4, d13 - d6) / f;
        double d21 = 0.0;
        double d22 = 0.0;
        double d23 = 0.0;
        double d24 = 0.0;
        double d25 = 0.0;
        double d26 = 0.0;

        for (int i = 0; i < 10; i++) {
            double d27 = Math.ceil(d20) + d19;
            double d28 = d12 + d14 * d27 + accelOffset(d17, d27);
            double d29 = predictY(d10 + d11, d10, d15, playerEntity.isOnGround(), d27);
            double d30 = d13 + d16 * d27 + accelOffset(d18, d27);
            double d31 = d28 - d4;
            double d32 = d30 - d6;
            double d33 = Math.hypot(d31, d32);
            if (d33 < 0.01) {
                return null;
            }

            d22 = d31 / d33;
            d23 = d32 / d33;
            d24 = -d23;
            d25 = d22;
            double d34 = d7 * d22 + d9 * d23;
            d26 = d7 * d24 + d9 * d22;
            double[] doubles2 = this.solvePitch(f, d33, d29 - d5, d34, d26, d8);
            if (doubles2 == null) {
                return null;
            }

            d21 = doubles2[0];
            d20 = doubles2[1];
        }

        double d35 = Math.cos(d21);
        double d36 = f * d35 * (f * d35) - d26 * d26;
        if (d36 < 0.0) {
            return null;
        }

        double d37 = Math.sqrt(d36);
        double d38 = d37 * d22 - d26 * d24;
        double d39 = d37 * d23 - d26 * d25;
        float f2 = (float)Math.toDegrees(Math.atan2(-d38, d39));
        float f3 = MathHelper.clamp((float)Math.toDegrees(-d21), -90.0F, 90.0F);
        return new Vector2f(f2, f3);
    }

    private double[] solvePitch(double d, double d2, double d3, double d4, double d5, double d6) {
        double d7 = Double.NaN;
        double d8 = Double.NaN;

        for (double d9 = -85.0; d9 <= 85.0; d9 += 0.5) {
            double d10 = Math.toRadians(d9);
            double[] doubles = this.simulateProjectile(d10, d, d2, d4, d5, d6);
            if (doubles != null) {
                double d11 = doubles[0] - d3;
                if (!Double.isNaN(d8) && d8 * d11 <= 0.0) {
                    double d12 = d7;
                    double d13 = d10;
                    double d14 = d8;

                    for (int i = 0; i < 40; i++) {
                        double d15 = (d12 + d13) / 2.0;
                        double[] doubles2 = this.simulateProjectile(d15, d, d2, d4, d5, d6);
                        if (doubles2 == null) {
                            d13 = d15;
                        } else {
                            double d16 = doubles2[0] - d3;
                            if (Math.abs(d16) < 1.0E-4) {
                                return new double[]{d15, doubles2[1]};
                            }

                            if (d14 * d16 <= 0.0) {
                                d13 = d15;
                            } else {
                                d12 = d15;
                                d14 = d16;
                            }
                        }
                    }

                    double d17 = (d12 + d13) / 2.0;
                    double[] doubles3 = this.simulateProjectile(d17, d, d2, d4, d5, d6);
                    if (doubles3 == null) {
                        return null;
                    }

                    return new double[]{d17, doubles3[1]};
                }

                d7 = d10;
                d8 = d11;
            }
        }

        return null;
    }

    private double[] simulateProjectile(double d, double d2, double d3, double d4, double d5, double d6) {
        double d7 = Math.cos(d);
        double d8 = d2 * d7 * (d2 * d7) - d5 * d5;
        if (d8 < 0.0) {
            return null;
        }

        double d9 = d4 + Math.sqrt(d8);
        double d10 = d2 * Math.sin(d) + d6;
        if (d9 <= 0.1) {
            return null;
        }

        double d11 = 0.0;
        double d12 = 0.0;

        for (int i = 1; i <= 400; i++) {
            double d13 = d11;
            double d14 = d12;
            d11 += d9;
            d12 += d10;
            d9 *= 0.99;
            d10 = d10 * 0.99 - 0.05;
            if (d11 >= d3) {
                double d15 = (d3 - d13) / (d11 - d13);
                return new double[]{d14 + (d12 - d14) * d15, i - 1 + d15};
            }
        }

        return null;
    }
}
