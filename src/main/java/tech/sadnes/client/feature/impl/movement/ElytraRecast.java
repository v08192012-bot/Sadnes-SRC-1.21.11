package tech.sadnes.client.feature.impl.movement;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.ModeSetting;

@FeatureInfo(
    name = "ElytraRecast",
    description = "feature.impl.movement.elytrarecast",
    displayName = "feature.impl.movement.elytrarecast",
    category = Category.MOVEMENT,
    subcategory = Subcategory.MOVEMENT_ELYTRA
)
public final class ElytraRecast extends Feature {
    private final ModeSetting mode = new ModeSetting("elytrarecast.mode", new String[]{"rage", "legit"}).setValue("legit");
    private int ticks;

    public static ElytraRecast getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(ElytraRecast.class);
    }

    @EventHandler
    public void onInput(MoveInputEvent moveInputEvent) {
        if (mc.player != null && mc.world != null) {
            EntityPose entityPose = EntityPose.STANDING;
            Vec3d vec3d = mc.player.getEntityPos().add(0.0, 0.4F, 0.0);
            boolean flag = this.canFitPose(entityPose, vec3d);
            ModeSetting modeSetting = this.mode;
            if (flag) {
                if (modeSetting.is("legit")) {
                    if (mc.player.input.playerInput.jump()) {
                        if (mc.player.isGliding()) {
                            this.ticks = 0;
                            return;
                        }

                        if (moveInputEvent.isMoving()) {
                            ClientPlayerEntity clientPlayerEntity = mc.player;
                            EquipmentSlot equipmentSlot = EquipmentSlot.CHEST;
                            ItemStack itemStack = clientPlayerEntity.getEquippedStack(equipmentSlot);
                            Item item = Items.ELYTRA;
                            if (itemStack.isOf(item)) {
                                int i = itemStack.getDamage();
                                if (itemStack.getMaxDamage() - 1 > i) {
                                    if (!mc.player.getAbilities().flying) {
                                        if (!mc.player.hasVehicle()) {
                                            if (!mc.player.isTouchingWater()) {
                                                if (!mc.player.isInLava()) {
                                                    ClientPlayerEntity clientPlayerEntity2 = mc.player;
                                                    RegistryEntry registryEntry = StatusEffects.LEVITATION;
                                                    if (!clientPlayerEntity2.hasStatusEffect(registryEntry)) {
                                                        boolean flag2 = mc.player.isOnGround();
                                                        int i2 = this.ticks;
                                                        if (flag2) {
                                                            this.ticks = 0;
                                                            moveInputEvent.setJump(true);
                                                            return;
                                                        }

                                                        int i3 = (int)(Integer.toUnsignedLong(i2) + 1L);
                                                        this.ticks = i3;
                                                        if (i3 < 2) {
                                                            moveInputEvent.setJump(false);
                                                            return;
                                                        }

                                                        int i4 = i2 & 1;
                                                        if (i4 == 0) {
                                                            moveInputEvent.setJump(false);
                                                            return;
                                                        }

                                                        moveInputEvent.setJump(true);
                                                        return;
                                                    }

                                                    this.ticks = 0;
                                                    return;
                                                }

                                                this.ticks = 0;
                                                return;
                                            }

                                            this.ticks = 0;
                                            return;
                                        }

                                        this.ticks = 0;
                                        return;
                                    }

                                    this.ticks = 0;
                                    return;
                                }

                                this.ticks = 0;
                                return;
                            }

                            this.ticks = 0;
                            return;
                        }

                        this.ticks = 0;
                        return;
                    }
                } else {
                    if (mc.player.isGliding()) {
                        return;
                    }

                    ClientPlayerEntity clientPlayerEntity3 = mc.player;
                    EquipmentSlot equipmentSlot2 = EquipmentSlot.CHEST;
                    ItemStack itemStack2 = clientPlayerEntity3.getEquippedStack(equipmentSlot2);
                    Item item2 = Items.ELYTRA;
                    if (itemStack2.isOf(item2)) {
                        int i5 = itemStack2.getDamage();
                        if (itemStack2.getMaxDamage() - 1 > i5) {
                            mc.player.startGliding();
                            ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
                            ClientPlayerEntity clientPlayerEntity4 = mc.player;
                            Mode mode2 = Mode.START_FALL_FLYING;
                            ClientCommandC2SPacket clientCommandC2SPacket = new ClientCommandC2SPacket(clientPlayerEntity4, mode2);
                            clientPlayNetworkHandler.sendPacket(clientCommandC2SPacket);
                            return;
                        }

                        return;
                    }
                }
            } else if (mc.player.isGliding() && mc.player.isOnGround()) {
                mc.player.stopGliding();
                return;
            }
        }
    }

    public boolean canFitPose(EntityPose entityPose, Vec3d vec3d) {
        World world = mc.player.getEntityWorld();
        ClientPlayerEntity clientPlayerEntity = mc.player;
        Box box = mc.player.getDimensions(entityPose).getBoxAt(vec3d).contract(1.0E-7);
        return world.isSpaceEmpty(clientPlayerEntity, box);
    }
}
