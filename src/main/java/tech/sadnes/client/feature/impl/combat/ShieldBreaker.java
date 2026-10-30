package tech.sadnes.client.feature.impl.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.api.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import tech.sadnes.api.mixin.accessor.PlayerInventoryAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "ShieldBreaker",
    description = "feature.impl.combat.shieldbreaker",
    displayName = "feature.impl.combat.shieldbreaker",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_WEAPONS
)
public final class ShieldBreaker extends Feature {
    private LivingEntity pendingTarget;
    private boolean pendingWithMace;
    private boolean waitTick;
    private boolean attacking;

    public static ShieldBreaker getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(ShieldBreaker.class);
    }

    public int getAxeSlotFor(LivingEntity livingEntity) {
        if (this.isEnabled() && this.pendingTarget == null && livingEntity.isBlocking()) {
            Vec3d vec3d = mc.player.getEntityPos().subtract(livingEntity.getEntityPos());
            boolean flag = vec3d.dotProduct(livingEntity.getRotationVector(0.0F, livingEntity.getHeadYaw())) >= 0.0;
            Slot slot = flag ? this.findAxeSlot() : null;
            return slot == null ? -1 : slot.getIndex();
        } else {
            return -1;
        }
    }

    @EventHandler
    public void onAttack(AttackEntityEvent attackEntityEvent) {
        if (!this.attacking && mc.player != null && attackEntityEvent.getTarget() instanceof LivingEntity livingEntity) {
            if (attackEntityEvent.getStage() == AttackEntityEvent.Stage.PRE) {
                this.handlePreAttack(attackEntityEvent, livingEntity);
            } else if (this.pendingTarget == null && livingEntity.isBlocking() && !this.isHoldingMace()) {
                if (!(mc.player.getMainHandStack().getItem() instanceof AxeItem)) {
                    int i = this.getAxeHotbarSlot();
                    if (i == -1) {
                        this.scheduleAxeAttack(livingEntity);
                    } else {
                        if (ThrowComponent.getInstance().isIdle()) {
                            this.schedulePending(livingEntity, false);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.pendingTarget != null) {
            if (mc.player == null) {
                this.pendingTarget = null;
            } else if (this.waitTick) {
                this.waitTick = false;
            } else {
                LivingEntity livingEntity = this.pendingTarget;
                boolean flag = this.pendingWithMace;
                this.pendingTarget = null;
                if (livingEntity.isAlive()) {
                    if (flag) {
                        this.attacking = true;

                        try {
                            mc.interactionManager.attackEntity(mc.player, livingEntity);
                            mc.player.swingHand(Hand.MAIN_HAND);
                        } finally {
                            this.attacking = false;
                        }
                    } else {
                        int i = this.getAxeHotbarSlot();
                        if (i != -1) {
                            this.attackWithSlot(livingEntity, i, true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.pendingTarget = null;
        this.waitTick = false;
        this.attacking = false;
    }

    private void handlePreAttack(AttackEntityEvent attackEntityEvent, LivingEntity livingEntity) {
        if (this.pendingTarget != null) {
            attackEntityEvent.setCancelled(true);
        } else if (this.isHoldingMace() && livingEntity.isBlocking()) {
            int i = this.getAxeHotbarSlot();
            if (i != -1 && ThrowComponent.getInstance().isIdle()) {
                attackEntityEvent.setCancelled(true);
                this.attackWithSlot(livingEntity, i, false);
                this.schedulePending(livingEntity, true);
            }
        }
    }

    private void attackWithSlot(LivingEntity livingEntity, int i, boolean flag) {
        PlayerInventoryAccessor playerInventoryAccessor = (PlayerInventoryAccessor)mc.player.getInventory();
        int i2 = playerInventoryAccessor.sadnes$getSelectedSlot();
        playerInventoryAccessor.sadnes$setSelectedSlot(i);
        ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot();
        PacketSender.sendSilent(PlayerInteractEntityC2SPacket.attack(livingEntity, mc.player.isSneaking()));
        if (flag) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        playerInventoryAccessor.sadnes$setSelectedSlot(i2);
    }

    private void scheduleAxeAttack(LivingEntity livingEntity) {
        Slot slot = this.findAxeSlot();
        if (slot != null) {
            ThrowComponent.getInstance().scheduleHotbarSwapUse(slot.id, false, Hand.MAIN_HAND, () -> {
                PacketSender.sendSilent(PlayerInteractEntityC2SPacket.attack(livingEntity, mc.player.isSneaking()));
                mc.player.swingHand(Hand.MAIN_HAND);
            });
        }
    }

    private int getAxeHotbarSlot() {
        Slot slot = this.findAxeSlot();
        return slot == null ? -1 : InventoryUtil.toHotbarIndex(slot.id);
    }

    private Slot findAxeSlot() {
        Slot slot = InventoryUtil.findPlayerSlotFromInventory(AxeItem.class);
        return slot != null ? slot : InventoryUtil.findPlayerSlotByItemOrClass(AxeItem.class);
    }

    private boolean isHoldingMace() {
        return mc.player.getMainHandStack().isOf(Items.MACE);
    }

    private void schedulePending(LivingEntity livingEntity, boolean flag) {
        this.pendingTarget = livingEntity;
        this.pendingWithMace = flag;
        this.waitTick = true;
    }
}
