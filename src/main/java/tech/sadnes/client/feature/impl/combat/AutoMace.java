package tech.sadnes.client.feature.impl.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import tech.sadnes.api.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.HudPostRenderEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PreRotationEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.component.TargetComponent;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.ActionSequence;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.client.util.player.FallUtil;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.MoveUtil;
import tech.sadnes.client.util.player.RaycastUtil;
import tech.sadnes.client.util.rotation.SensitivityUtil;

@FeatureInfo(
    name = "AutoMace",
    description = "feature.impl.combat.automace",
    displayName = "feature.impl.combat.automace",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_WEAPONS
)
public final class AutoMace extends Feature {
    private final NumberSetting fall = new NumberSetting("aura.autoMace.fall", 1.5F, 1.5F, 6.0F, 0.1F);
    private final BooleanSetting swap = new BooleanSetting("aura.autoMace.swap", true);
    private final BooleanSetting swapBack = new BooleanSetting("aura.autoMace.swapBack", true).setVisibleWhen(this.swap::getValue);
    private final ModeSetting swapMode = new ModeSetting("aura.autoMace.swapMode", new String[]{"default", "silent"});
    private final ModeSetting forceShootMode = new ModeSetting("aura.autoMace.forceShootMode", new String[]{"packet", "normal", "none"});
    private final BooleanSetting throughWalls = new BooleanSetting("aura.autoMace.throughWalls", true);
    private final BooleanSetting move = new BooleanSetting("aura.autoMace.move", true);
    private LivingEntity target;
    private float attackTickThreshold;
    private int previousSlot = -1;
    private boolean swapped = false;
    private boolean silentSwapped = false;
    private int silentSlot = -1;
    private Runnable pendingAction;
    private final ActionSequence actionSequence = new ActionSequence();
    private boolean unusedFlag = false;
    private boolean timerPrimed = false;

    public static AutoMace getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(AutoMace.class);
    }

    public boolean isActive() {
        return this.isEnabled() && this.target != null && (this.swapped || mc.player != null && mc.player.getMainHandStack().isOf(Items.MACE));
    }

    @Override
    public void onEnable() {
        this.swapBack();
        this.runPendingAction();
        this.target = null;
        super.onEnable();
    }

    @EventHandler
    public void onPreRotation(PreRotationEvent preRotationEvent) {
        if (this.silentSwapped && InventoryUtil.getSelectedSlot() != this.previousSlot) {
            if (this.silentSlot != -1 && InventoryUtil.getSelectedSlot() == this.silentSlot && mc.interactionManager != null) {
                ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$setLastSelectedSlot(this.silentSlot);
            }

            this.silentSlot = -1;
            this.silentSwapped = false;
            this.swapped = false;
            this.previousSlot = -1;
            this.pendingAction = null;
        }

        this.runPendingAction();
    }

    private void runPendingAction() {
        Runnable runnable = this.pendingAction;
        this.pendingAction = null;
        if (runnable != null) {
            runnable.run();
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player != null && mc.world != null) {
            this.actionSequence.tick();
            if (FallUtil.isFallDamageExempt()) {
                this.swapBack();
                this.target = null;
            } else {
                boolean flag = mc.player.fallDistance >= this.fall.getValue().floatValue()
                    || FallUtil.getDistanceToGround(mc.player) >= this.fall.getValue().floatValue();
                if (flag) {
                    this.updateTarget();
                    if (this.target != null) {
                        this.swapToMace();
                        this.attackTarget();
                    } else {
                        this.swapBack();
                    }
                } else {
                    this.swapBack();
                    this.target = null;
                }
            }
        }
    }

    @EventHandler
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (this.target != null && this.isActive() && this.move.getValue()) {
            moveInputEvent.setForward(1.0F);
            MoveUtil.moveTowardsTarget(moveInputEvent, this.target);
        }
    }

    @EventHandler
    public void onHudPostRender(HudPostRenderEvent hudPostRenderEvent) {
    }

    private void updateTarget() {
        if (this.target == null || !this.target.isAlive() || !this.isInRange(this.target)) {
            this.target = null;
            AttackAura attackAura = AttackAura.getInstance();
            if (attackAura != null) {
                if (attackAura.target != null) {
                    this.target = attackAura.target;
                } else {
                    LivingEntity livingEntity = TargetComponent.getCurrentTarget();
                    if (livingEntity != null
                        && livingEntity.isAlive()
                        && TargetComponent.isValidTarget(livingEntity, attackAura.getTargets(), this.throughWalls.getValue())
                        && this.isInRange(livingEntity)) {
                        this.target = livingEntity;
                    } else {
                        for (LivingEntity livingEntity2 : TargetComponent.LIVING_ENTITIES) {
                            if (TargetComponent.isValidTarget(livingEntity2, attackAura.getTargets(), this.throughWalls.getValue()) && this.isInRange(livingEntity2)) {
                                this.target = livingEntity2;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isInRange(LivingEntity livingEntity) {
        AttackAura attackAura = AttackAura.getInstance();
        double d = attackAura != null ? attackAura.getAttackRange() : 3.0;
        Box box = livingEntity.getBoundingBox();
        double d2 = mc.player.getX() - MathHelper.clamp(mc.player.getX(), box.minX, box.maxX);
        double d3 = mc.player.getZ() - MathHelper.clamp(mc.player.getZ(), box.minZ, box.maxZ);
        double d4 = d2 * d2 + d3 * d3;
        if (d4 > d * d) {
            return false;
        }

        double d5 = mc.player.getY() - livingEntity.getY();
        return d5 >= -1.0;
    }

    private void swapToMace() {
        if (this.swap.getValue()) {
            if (mc.player.getMainHandStack().isOf(Items.MACE)) {
                if (!this.swapped && !this.timerPrimed) {
                    AttackTimer.getTimer().setElapsed(2000L);
                    this.timerPrimed = true;
                }

                this.swapped = true;
            } else {
                Slot slot = InventoryUtil.findSlotReverse(Items.MACE);
                if (slot != null && !this.swapped) {
                    int i = InventoryUtil.toHotbarIndex(slot.id);
                    if (i != -1) {
                        this.previousSlot = InventoryUtil.getSelectedSlot();
                        if (this.swapMode.is("default")) {
                            InventoryUtil.setSelectedSlot(i);
                        } else {
                            this.pendingAction = () -> {
                                if (mc.player != null) {
                                    if (InventoryUtil.getSelectedSlot() != i) {
                                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
                                        this.silentSlot = i;
                                    }

                                    this.silentSwapped = true;
                                }
                            };
                        }

                        this.swapped = true;
                        AttackTimer.getTimer().setElapsed(2000L);
                        this.timerPrimed = true;
                    } else {
                        this.previousSlot = slot.id;
                        InventoryUtil.swapToHand(slot, Hand.MAIN_HAND);
                        this.swapped = true;
                        AttackTimer.getTimer().setElapsed(2000L);
                        this.timerPrimed = true;
                    }
                }
            }
        }
    }

    private void swapBack() {
        if (this.actionSequence.isFinished()) {
            if (this.swapped) {
                if (this.silentSwapped || this.silentSlot != -1 || this.pendingAction != null) {
                    this.pendingAction = this.silentSlot != -1 ? () -> {
                        if (mc.player != null && this.silentSlot != -1) {
                            int i = InventoryUtil.getSelectedSlot();
                            if (i != this.silentSlot) {
                                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
                            }

                            if (mc.interactionManager != null) {
                                ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$setLastSelectedSlot(i);
                            }

                            this.silentSlot = -1;
                        }
                    } : null;
                    this.silentSwapped = false;
                    this.previousSlot = -1;
                    this.swapped = false;
                    return;
                }

                if (this.swapBack.getValue() && this.previousSlot != -1) {
                    if (this.previousSlot >= 9) {
                        InventoryUtil.swapToHand(this.previousSlot, Hand.MAIN_HAND);
                        this.previousSlot = -1;
                        this.swapped = false;
                        return;
                    }

                    InventoryUtil.setSelectedSlot(this.previousSlot);
                }

                this.previousSlot = -1;
                this.swapped = false;
            }
        }
    }

    private void attackTarget() {
        if (this.swapMode.is("default")) {
            if (!mc.player.getMainHandStack().isOf(Items.MACE)) {
                return;
            }
        } else if (!this.silentSwapped && !mc.player.getMainHandStack().isOf(Items.MACE)) {
            return;
        }

        if (this.target != null && this.target.isAlive() && this.isInRange(this.target)) {
            if (AttackTimer.isReady()) {
                AttackAura attackAura = AttackAura.getInstance();
                int i = FallUtil.getTicksToGround(mc.player);
                boolean flag = mc.player.fallDistance >= this.fall.getValue().floatValue();
                double d = MathHelper.clamp(mc.player.getEyePos().y, this.target.getBoundingBox().minY, this.target.getBoundingBox().maxY);
                double d2 = mc.player.getEyePos().y - d;
                boolean flag2 = d2 < 1.0 && d2 >= 0.0;
                if (flag2 && flag) {
                    this.attackTickThreshold = i + 1;
                } else {
                    this.attackTickThreshold = 3.0F;
                }

                if (mc.player.fallDistance > 0.0) {
                    boolean flag3 = RaycastUtil.rayIntersectsEntity(MathHelper.wrapDegrees(mc.player.getYaw()), mc.player.getPitch(), 1000.0, this.target);
                    boolean flag4 = this.forceShootMode.is("normal");
                    if (flag4 && i <= this.attackTickThreshold && !flag3) {
                        Vector4f vector4f = CombatUtil.getRotationsTo(this.target, (float)attackAura.getAttackRange(), false);
                        RotationComponent.rotateTo(
                            new Rotation(vector4f.x, vector4f.y).randomize(2.0F),
                            CombatUtil.randomFloat(170.0F, 180.0F),
                            CombatUtil.randomFloat(170.0F, 180.0F),
                            1,
                            1001
                        );
                    }

                    if (i <= this.attackTickThreshold - 1.0F) {
                        if (mc.player.getItemCooldownManager().isCoolingDown(Items.MACE.getDefaultStack())) {
                            return;
                        }

                        Runnable runnable = this.createRotationPacketAction(this.forceShootMode.is("packet") && !flag3);
                        CombatUtil.attack(
                            this.target,
                            () -> {},
                            () -> {},
                            runnable,
                            () -> {},
                            Hand.MAIN_HAND,
                            this.forceShootMode.is("packet"),
                            attackAura.getAttackRange(),
                            attackAura.getThroughWallsSetting().getValue()
                        );
                        this.timerPrimed = false;
                    }
                }
            }
        }
    }

    private Runnable createRotationPacketAction(boolean flag) {
        return !flag ? () -> {} : () -> {
            Vector4f vector4f = CombatUtil.getRotationsTo(this.target);
            float f = SensitivityUtil.snapDelta(mc.player.getYaw() + vector4f.z + CombatUtil.randomFloat(-0.02F, 0.02F));
            float f2 = SensitivityUtil.snapDelta(mc.player.getPitch() + vector4f.w + CombatUtil.randomFloat(-0.02F, 0.02F));
            PacketSender.sendSilent(new Full(mc.player.getEntityPos(), f, f2, mc.player.isOnGround(), mc.player.horizontalCollision));
        };
    }

    public LivingEntity getTarget() {
        return this.target;
    }
}
