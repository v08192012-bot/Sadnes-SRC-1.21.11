package tech.sadnes.client.feature.impl.player;

import java.lang.reflect.Array;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import tech.sadnes.api.mixin.accessor.LivingEntityAccessor;
import tech.sadnes.api.mixin.client.MinecraftClientAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.MultiSetting;

@FeatureInfo(
    name = "NoDelay",
    description = "feature.impl.player.nodelay",
    displayName = "feature.impl.player.nodelay",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_TWEAKS
)
public final class NoDelay extends Feature {
    private final MultiSetting delayRemovals = new MultiSetting(
        "nodelay.delayremovals", BooleanSetting.of("jumps", false), BooleanSetting.of("right_click", false), BooleanSetting.of("fast_exp", false)
    );
    private final GroupSetting onlyWithGlass = new GroupSetting("nodelay.onlywithglass", false)
        .setVisibleWhen(() -> this.delayRemovals.getOption("right_click").getValue());
    private final BooleanSetting jumpBypass = new BooleanSetting("nodelay.jumpBypass", false).setVisibleWhen(() -> this.delayRemovals.getOption("jumps").getValue());
    private final MultiSetting stopper = new MultiSetting(
            "nodelay.stopper", BooleanSetting.of("inPvP", false), BooleanSetting.of("checkCooldown", false), BooleanSetting.of("gearRepaired", false)
        )
        .setVisibleWhen(() -> this.delayRemovals.getOption("fast_exp").getValue());

    public static NoDelay getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(NoDelay.class);
    }

    public boolean isJumpDelayRemoved() {
        return this.isEnabled() && this.delayRemovals.getOption("jumps").getValue();
    }

    @EventHandler
    public void onInput(MoveInputEvent moveInputEvent) {
        boolean flag = ServerManager.isServer("donut");
        MultiSetting multiSetting = this.delayRemovals;
        if (flag) {
            boolean flag2 = multiSetting.getOption("jumps").getValue();
            BooleanSetting booleanSetting = this.jumpBypass;
            if (flag2 && booleanSetting.getValue() && mc.player.input.playerInput.jump()) {
                ClientPlayerEntity clientPlayerEntity = mc.player;
                EquipmentSlot equipmentSlot = EquipmentSlot.CHEST;
                Item item = clientPlayerEntity.getEquippedStack(equipmentSlot).getItem();
                Item item2 = Items.ELYTRA;
                if (item != item2 && !mc.player.getAbilities().allowFlying && !mc.player.isTouchingWater() && !mc.player.isInLava() && !mc.player.isClimbing()) {
                    int i = ~mc.player.age & 1;
                    if (i == 0) {
                        moveInputEvent.setJump(false);
                        return;
                    }

                    moveInputEvent.setJump(true);
                    return;
                }
            }
        } else if (this.shouldRemoveJumpDelay() && moveInputEvent.isJump() && !mc.player.isOnGround()) {
            moveInputEvent.setJump(false);
            return;
        }
    }

    private boolean shouldRemoveJumpDelay() {
        boolean flag = this.delayRemovals.getOption("jumps").getValue();
        BooleanSetting booleanSetting = this.jumpBypass;
        if (flag) {
            if (booleanSetting.getValue()) {
                ClientPlayerEntity clientPlayerEntity = mc.player;
                EquipmentSlot equipmentSlot = EquipmentSlot.CHEST;
                Item item = clientPlayerEntity.getEquippedStack(equipmentSlot).getItem();
                Item item2 = Items.ELYTRA;
                if (item != item2) {
                    if (!mc.player.getAbilities().allowFlying) {
                        if (!mc.player.isTouchingWater()) {
                            if (!mc.player.isInLava()) {
                                if (!mc.player.isClimbing()) {
                                    return !mc.player.inPowderSnow ? !mc.player.hasVehicle() : false;
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
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        MultiSetting multiSetting = this.delayRemovals;
        if (multiSetting.getOption("jumps").getValue() && (ServerManager.isServer("donut") || !this.shouldRemoveJumpDelay())) {
            ((LivingEntityAccessor)mc.player).sadnes$setJumpingCooldown(0);
        }

        ItemStack itemStack = mc.player.getMainHandStack();
        ItemStack itemStack2 = mc.player.getOffHandStack();
        Item item = Items.EXPERIENCE_BOTTLE;
        byte b;
        if (itemStack.isOf(item)) {
            if (Hand.MAIN_HAND == null) {
                b = 0;
            } else {
                b = 1;
            }
        } else {
            Item item2 = Items.EXPERIENCE_BOTTLE;
            if (itemStack2.isOf(item2)) {
                if (Hand.OFF_HAND == null) {
                    b = 0;
                } else {
                    b = 1;
                }
            } else {
                b = 0;
            }
        }

        boolean flag;
        if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
            ClientWorld clientWorld = mc.world;
            BlockPos blockPos = blockHitResult.getBlockPos();
            FluidState fluidState = clientWorld.getFluidState(blockPos);
            FlowableFluid flowableFluid = Fluids.WATER;
            flag = fluidState.isOf(flowableFluid);
        } else {
            flag = false;
        }

        ItemStack itemStack3 = mc.player.getMainHandStack();
        Item item3 = Items.GLASS_BOTTLE;
        boolean flag2;
        if (!itemStack3.isOf(item3)) {
            ItemStack itemStack4 = mc.player.getOffHandStack();
            Item item4 = Items.GLASS_BOTTLE;
            flag2 = itemStack4.isOf(item4);
        } else {
            flag2 = true;
        }

        boolean flag3 = multiSetting.getOption("right_click").getValue();
        GroupSetting groupSetting = this.onlyWithGlass;
        if (flag3 && (0 | b) == 0) {
            if (groupSetting.getValue() && !(flag & flag2)) {
                return;
            }

            ((MinecraftClientAccessor)mc).sadnes$setItemUseCooldown(0);
        }

        Hand hand = this.getUsingHand();
        if (hand != null) {
            ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
            ClientPlayerEntity clientPlayerEntity = mc.player;
            clientPlayerInteractionManager.interactItem(clientPlayerEntity, hand);
        }
    }

    private Hand getUsingHand() {
        if (mc.player != null) {
            if (mc.options.useKey.isPressed()) {
                Boolean flag = Boolean.TRUE;
                Object object = this.delayRemovals.getOption("fast_exp").getValue();
                if (flag.equals(object)) {
                    ItemStack itemStack = mc.player.getMainHandStack();
                    ItemStack itemStack2 = mc.player.getOffHandStack();
                    Item item = Items.EXPERIENCE_BOTTLE;
                    Hand hand;
                    MultiSetting multiSetting;
                    if (itemStack.isOf(item)) {
                        Hand hand2 = Hand.MAIN_HAND;
                        MultiSetting multiSetting2 = this.stopper;
                        if (hand2 == null) {
                            return null;
                        }

                        hand = hand2;
                        multiSetting = multiSetting2;
                    } else {
                        Item item2 = Items.EXPERIENCE_BOTTLE;
                        if (!itemStack2.isOf(item2)) {
                            return null;
                        }

                        Hand hand3 = Hand.OFF_HAND;
                        MultiSetting multiSetting3 = this.stopper;
                        if (hand3 == null) {
                            return null;
                        }

                        hand = hand3;
                        multiSetting = multiSetting3;
                    }

                    if (multiSetting.isSelected("inPvP") && ServerManager.isInPvp()) {
                        return null;
                    }

                    Hand hand4 = Hand.MAIN_HAND;
                    boolean flag2 = this.stopper.isSelected("checkCooldown");
                    MultiSetting multiSetting4 = this.stopper;
                    MultiSetting multiSetting5;
                    if (flag2) {
                        ItemCooldownManager itemCooldownManager = mc.player.getItemCooldownManager();
                        ItemStack itemStack3 = hand4 != hand ? itemStack2 : itemStack;
                        boolean flag3 = itemCooldownManager.isCoolingDown(itemStack3);
                        multiSetting5 = this.stopper;
                        if (flag3) {
                            return null;
                        }
                    } else {
                        multiSetting5 = multiSetting4;
                    }

                    if (multiSetting5.isSelected("gearRepaired")) {
                        return this.canFastUse(hand) ? hand : null;
                    } else {
                        return hand;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean canFastUse(Hand hand) {
        boolean flag;
        label76: {
            Hand hand2 = Hand.MAIN_HAND;
            if (hand2 != hand) {
                if (hand2 == hand && hand2 == hand) {
                    ItemStack itemStack = mc.player.getMainHandStack();
                    flag = this.isGearDamaged(itemStack);
                    break label76;
                }

                ItemStack itemStack2 = mc.player.getMainHandStack();
                if (!this.isGearDamaged(itemStack2)) {
                    flag = false;
                    break label76;
                }
            } else {
                ItemStack itemStack3 = mc.player.getOffHandStack();
                if (!this.isGearDamaged(itemStack3)) {
                    flag = false;
                    break label76;
                }
            }

            flag = true;
        }

        boolean flag2;
        if (flag) {
            flag2 = true;
        } else {
            EquipmentSlot[] equipmentSlots = new EquipmentSlot[4];
            EquipmentSlot equipmentSlot = EquipmentSlot.HEAD;
            Array.set(equipmentSlots, 0, equipmentSlot);
            EquipmentSlot equipmentSlot2 = EquipmentSlot.CHEST;
            Array.set(equipmentSlots, 1, equipmentSlot2);
            EquipmentSlot equipmentSlot3 = EquipmentSlot.LEGS;
            Array.set(equipmentSlots, 2, equipmentSlot3);
            EquipmentSlot equipmentSlot4 = EquipmentSlot.FEET;
            Array.set(equipmentSlots, 3, equipmentSlot4);
            int i = Array.getLength(equipmentSlots);
            if (i <= 0) {
                flag2 = false;
            } else {
                int i2 = i & 2147483647;
                long l = 0L;

                while (true) {
                    int i3 = (int)l;
                    Object object = Array.get(equipmentSlots, i3);
                    ItemStack itemStack4 = mc.player.getEquippedStack((EquipmentSlot)object);
                    if (this.isGearDamaged(itemStack4)) {
                        flag2 = true;
                        break;
                    }

                    if (Integer.toUnsignedLong(i2) == ++l) {
                        flag2 = false;
                        break;
                    }
                }
            }
        }

        return flag2;
    }

    private boolean isGearDamaged(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        } else {
            return itemStack.isDamageable() ? itemStack.getDamage() > 0 : false;
        }
    }

    public boolean shouldFastExp() {
        if (this.isEnabled()) {
            Hand hand = this.getUsingHand();
            Hand hand2 = Hand.OFF_HAND;
            return hand == hand2;
        } else {
            return false;
        }
    }
}
