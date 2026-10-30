package tech.sadnes.client.feature.impl.player;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import tech.sadnes.api.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import tech.sadnes.api.mixin.accessor.PlayerInventoryAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "AutoTool",
    description = "feature.impl.player.autotool",
    displayName = "feature.impl.player.autotool",
    category = Category.PLAYER,
    subcategory = Subcategory.INTERACT
)
public final class AutoTool extends Feature {
    private int previousSlot = -1;
    private int swappedInventorySlot = -1;

    public static AutoTool getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(AutoTool.class);
    }

    @Override
    public void onEnable() {
        if (mc.player != null && mc.interactionManager != null) {
            ClientPlayerEntity clientPlayerEntity = mc.player;
            this.restoreSlot(clientPlayerEntity);
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.isEnabled()) {
            ClientPlayerEntity clientPlayerEntity = mc.player;
            if (clientPlayerEntity != null) {
                if (mc.world != null) {
                    if (mc.interactionManager != null) {
                        ScreenHandler screenHandler = clientPlayerEntity.currentScreenHandler;
                        PlayerScreenHandler playerScreenHandler2 = clientPlayerEntity.playerScreenHandler;
                        label125:
                        if (screenHandler == playerScreenHandler2) {
                            BlockPos blockPos;
                            label142: {
                                Nuker nuker = Nuker.getInstance();
                                if (nuker != null) {
                                    blockPos = nuker.getBreakingPos();
                                    if (blockPos != null) {
                                        break label142;
                                    }
                                }

                                if (!mc.options.attackKey.isPressed()) {
                                    break label125;
                                }

                                if (mc.crosshairTarget == null) {
                                    return;
                                }

                                Type type = mc.crosshairTarget.getType();
                                Type type2 = Type.BLOCK;
                                if (type != type2) {
                                    return;
                                }

                                blockPos = ((BlockHitResult)mc.crosshairTarget).getBlockPos();
                            }

                            BlockState blockState = mc.world.getBlockState(blockPos);
                            int i = this.swappedInventorySlot;
                            int i2 = this.previousSlot;
                            long l = 0L;
                            int i3 = 1065353216;
                            int i4 = i2;
                            int i5 = -1;

                            int i6;
                            int i7;
                            int i8;
                            while (true) {
                                int i9;
                                label151: {
                                    this.swappedInventorySlot = i;
                                    this.previousSlot = i4;
                                    PlayerInventory playerInventory = clientPlayerEntity.getInventory();
                                    int i10 = (int)l;
                                    ItemStack itemStack = playerInventory.getStack(i10);
                                    float f = this.getBreakSpeed(itemStack, blockState);
                                    i6 = this.swappedInventorySlot;
                                    i7 = this.previousSlot;
                                    float f2 = Float.intBitsToFloat(i3);
                                    boolean flag = !(f2 >= f);
                                    if (!Float.isNaN(f) && !Float.isNaN(f2) && f <= f2) {
                                        i9 = i3;
                                        if (flag) {
                                            i8 = i10;
                                            if (++l == 36L) {
                                                break;
                                            }
                                            break label151;
                                        }
                                    } else {
                                        i9 = Float.floatToRawIntBits(f);
                                        if (flag) {
                                            i8 = i10;
                                            if (++l == 36L) {
                                                break;
                                            }
                                            break label151;
                                        }
                                    }

                                    i8 = i5;
                                    if (++l == 36L) {
                                        break;
                                    }
                                }

                                i4 = i7;
                                i3 = i9;
                                i = i6;
                                i5 = i8;
                            }

                            if (i8 != -1) {
                                this.swappedInventorySlot = i6;
                                this.previousSlot = i7;
                                int i11 = ((PlayerInventoryAccessor)clientPlayerEntity.getInventory()).sadnes$getSelectedSlot();
                                int i12 = this.swappedInventorySlot;
                                int i13 = this.previousSlot;
                                if (i8 == i11) {
                                    return;
                                }

                                if (i8 > 8) {
                                    if (i12 == i8) {
                                        return;
                                    }

                                    label89: {
                                        if (i13 != -1) {
                                            PlayerInventoryAccessor playerInventoryAccessor = (PlayerInventoryAccessor)clientPlayerEntity.getInventory();
                                            int i14 = this.previousSlot;
                                            playerInventoryAccessor.sadnes$setSelectedSlot(i14);
                                            ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot();
                                            int i15 = this.swappedInventorySlot;
                                            i13 = -1;
                                            if (i15 == -1) {
                                                i13 = -1;
                                                break label89;
                                            }
                                        } else if (i12 == -1) {
                                            break label89;
                                        }

                                        this.previousSlot = i13;
                                        this.swapBackInventorySlot(clientPlayerEntity);
                                        i13 = this.previousSlot;
                                    }

                                    this.previousSlot = i13;
                                    Slot slot = clientPlayerEntity.currentScreenHandler.getSlot(i8);
                                    Hand hand = Hand.MAIN_HAND;
                                    InventoryUtil.swapToHand(slot, hand);
                                    this.swappedInventorySlot = i8;
                                    return;
                                }

                                label98: {
                                    int i16;
                                    if (i12 != -1) {
                                        this.swapBackInventorySlot(clientPlayerEntity);
                                        int i17 = this.previousSlot;
                                        i16 = i17;
                                        if (i17 == -1) {
                                            break label98;
                                        }
                                    } else {
                                        i16 = i13;
                                        if (i13 == -1) {
                                            break label98;
                                        }
                                    }

                                    i11 = i16;
                                }

                                this.previousSlot = i11;
                                ((PlayerInventoryAccessor)clientPlayerEntity.getInventory()).sadnes$setSelectedSlot(i8);
                                ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot();
                                return;
                            }

                            this.swappedInventorySlot = i6;
                            this.previousSlot = i7;
                            return;
                        }

                        this.restoreSlot(clientPlayerEntity);
                    }
                }
            }
        }
    }

    public float getBestBreakSpeed(BlockState blockState) {
        if (mc.player == null) {
            return 1.0F;
        }

        long l = 0L;
        float f = 1.0F;

        while (true) {
            PlayerInventory playerInventory = mc.player.getInventory();
            int i = (int)l;
            ItemStack itemStack = playerInventory.getStack(i);
            float f2 = this.getBreakSpeed(itemStack, blockState);
            float f3 = Math.max(f, f2);
            if (++l == 36L) {
                return f3;
            }

            f = f3;
        }
    }

    private float getBreakSpeed(ItemStack itemStack, BlockState blockState) {
        float f;
        if (!itemStack.isEmpty()) {
            f = itemStack.getMiningSpeedMultiplier(blockState);
            boolean flag;
            if (blockState.getBlock() instanceof LeavesBlock) {
                flag = false;
            } else {
                TagKey tagKey = BlockTags.LEAVES;
                flag = !blockState.isIn(tagKey);
            }

            Item item = Items.NETHERITE_SWORD;
            if (itemStack.isOf(item)) {
                if (flag) {
                    return f;
                }

                return Math.max(f, 15.0F);
            }

            Item item2 = Items.DIAMOND_SWORD;
            if (itemStack.isOf(item2)) {
                if (flag) {
                    return f;
                }

                return Math.max(f, 15.0F);
            }

            Item item3 = Items.GOLDEN_SWORD;
            if (itemStack.isOf(item3)) {
                if (flag) {
                    return f;
                }

                return Math.max(f, 15.0F);
            }

            Item item4 = Items.IRON_SWORD;
            if (itemStack.isOf(item4)) {
                if (flag) {
                    return f;
                }

                return Math.max(f, 15.0F);
            }

            Item item5 = Items.STONE_SWORD;
            if (itemStack.isOf(item5)) {
                if (flag) {
                    return f;
                }

                return Math.max(f, 15.0F);
            }

            Item item6 = Items.WOODEN_SWORD;
            if (itemStack.isOf(item6) && !flag) {
                return Math.max(f, 15.0F);
            }
        } else {
            f = 0.0F;
        }

        return f;
    }

    private void restoreSlot(ClientPlayerEntity clientPlayerEntity) {
        int i;
        label16: {
            int i2 = this.previousSlot;
            int i3 = this.swappedInventorySlot;
            if (i2 != -1) {
                PlayerInventoryAccessor playerInventoryAccessor = (PlayerInventoryAccessor)clientPlayerEntity.getInventory();
                int i4 = this.previousSlot;
                playerInventoryAccessor.sadnes$setSelectedSlot(i4);
                ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot();
                int i5 = this.swappedInventorySlot;
                i = -1;
                if (i5 != -1) {
                    break label16;
                }

                i = -1;
            } else {
                i = i2;
                if (i3 != -1) {
                    break label16;
                }
            }

            this.previousSlot = i;
            return;
        }

        this.previousSlot = i;
        this.swapBackInventorySlot(clientPlayerEntity);
    }

    private void swapBackInventorySlot(ClientPlayerEntity clientPlayerEntity) {
        ScreenHandler screenHandler = clientPlayerEntity.currentScreenHandler;
        int i = this.swappedInventorySlot;
        Slot slot = screenHandler.getSlot(i);
        Hand hand = Hand.MAIN_HAND;
        InventoryUtil.swapToHand(slot, hand);
        this.swappedInventorySlot = -1;
    }
}
