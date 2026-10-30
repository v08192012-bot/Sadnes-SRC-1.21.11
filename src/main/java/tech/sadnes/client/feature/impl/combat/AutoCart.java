package tech.sadnes.client.feature.impl.combat;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.api.mixin.accessor.PlayerInventoryAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.util.ActionSequence;
import tech.sadnes.client.util.chat.ChatUtil;
import tech.sadnes.client.util.function.Action;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "AutoCart",
    description = "feature.impl.combat.autocart",
    displayName = "feature.impl.combat.autocart",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_WEAPONS
)
public final class AutoCart extends Feature {
    private final KeybindSetting key = new KeybindSetting("autocart.key");
    private final ActionSequence actionQueue = new ActionSequence();
    private Vec3d aimTarget;
    private boolean holdingUseKey;
    private boolean placing;

    public static AutoCart getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(AutoCart.class);
    }

    public boolean isPlacing() {
        return this.placing;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.reset();
    }

    @EventHandler
    public void onKeyPress(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.isPress()) {
            Screen screen = screenMouseClickEvent.getScreen();
            KeybindSetting keybindSetting = this.key;
            if (screen == null) {
                int i = screenMouseClickEvent.getButton();
                if (keybindSetting.matchesKey(i)) {
                    this.startSequence();
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onMousePress(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.isPress()) {
            Screen screen = screenKeyEvent.getScreen();
            KeybindSetting keybindSetting = this.key;
            if (screen == null) {
                int i = screenKeyEvent.getKey();
                if (keybindSetting.matchesKey(i)) {
                    this.startSequence();
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        boolean flag = ServerManager.isNullWorld();
        Vec3d vec3d = this.aimTarget;
        ActionSequence actionSequence = this.actionQueue;
        if (flag) {
            this.reset();
        } else if (vec3d != null) {
            this.aimAt(vec3d);
            actionSequence.tick();
        } else {
            actionSequence.tick();
        }
    }

    private void startSequence() {
        boolean flag = ServerManager.isNullWorld();
        ActionSequence actionSequence = this.actionQueue;
        if (!flag && actionSequence.isFinished()) {
            BlockHitResult blockHitResult = this.findPlacementHit();
            if (blockHitResult != null) {
                int i = this.findRailSlot();
                int i2 = InventoryUtil.findHotbarIndex(Items.TNT_MINECART);
                int i3 = this.findFlameBowSlot();
                if (i != -1 && i2 != -1 && i3 != -1) {
                    ClientPlayerEntity clientPlayerEntity = mc.player;
                    ItemStack itemStack = mc.player.getInventory().getStack(i3);
                    if (clientPlayerEntity.getProjectileType(itemStack).isEmpty()) {
                        String string = Language.translate("autocart.needArrows", "AutoCart: РЅРµС‚ СЃС‚СЂРµР»");
                        Object[] objects = new Object[0];
                        ChatUtil.print(string, objects);
                    } else {
                        BlockPos blockPos = blockHitResult.getBlockPos();
                        Direction direction = blockHitResult.getSide();
                        BlockPos blockPos2 = blockPos.offset(direction);
                        Vec3d vec3d = Vec3d.ofCenter(blockPos2, -0.4375);
                        Direction direction2 = Direction.UP;
                        BlockHitResult blockHitResult2 = new BlockHitResult(vec3d, direction2, blockPos2, false);
                        Vec3d vec3d2 = Vec3d.ofCenter(blockPos2, -0.15);
                        int i4 = this.getSelectedSlot();
                        Vec3d vec3d3 = blockHitResult.getPos();
                        this.aimTarget = vec3d3;
                        ActionSequence actionSequence2 = actionSequence.clear();
                        Action action = () -> {
                            this.setSelectedSlot(i);
                            this.interactBlock(blockHitResult);
                        };
                        ActionSequence actionSequence3 = actionSequence2.addTicked(1, action);
                        Action action2 = () -> this.aimTarget = vec3d2;
                        ActionSequence actionSequence4 = actionSequence3.addTicked(0, action2);
                        Action action3 = () -> {
                            this.setSelectedSlot(i2);
                            this.interactBlock(blockHitResult2);
                        };
                        ActionSequence actionSequence5 = actionSequence4.addTicked(1, action3);
                        Action action4 = () -> this.setSelectedSlot(i3);
                        ActionSequence actionSequence6 = actionSequence5.addTicked(0, action4);
                        Action action5 = () -> {
                            mc.options.useKey.setPressed(true);
                            this.holdingUseKey = true;
                        };
                        ActionSequence actionSequence7 = actionSequence6.addTicked(0, action5);
                        Action action6 = this::releaseUseKey;
                        ActionSequence actionSequence8 = actionSequence7.addTicked(20, action6);
                        Action action7 = () -> {
                            this.setSelectedSlot(i4);
                            this.aimTarget = null;
                        };
                        actionSequence8.addTicked(2, action7);
                    }
                } else {
                    String string2 = Language.translate(
                        "autocart.needItems", "AutoCart: РІ С…РѕС‚Р±Р°СЂРµ РЅСѓР¶РЅС‹ СЂРµР»СЊСЃР°, РўРќРў-РІР°РіРѕРЅРµС‚РєР° Рё Р»СѓРє СЃ В«РћРіРѕРЅСЊВ»"
                    );
                    Object[] objects2 = new Object[0];
                    ChatUtil.print(string2, objects2);
                }
            } else {
                String string3 = Language.translate("autocart.noSpot", "AutoCart: РЅРµРєСѓРґР° РїРѕСЃС‚Р°РІРёС‚СЊ СЂРµР»СЊСЃСѓ");
                Object[] objects3 = new Object[0];
                ChatUtil.print(string3, objects3);
            }
        }
    }

    private BlockHitResult findPlacementHit() {
        if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
            Type type = blockHitResult.getType();
            Type type2 = Type.BLOCK;
            if (type == type2) {
                BlockPos blockPos = blockHitResult.getBlockPos();
                Direction direction = blockHitResult.getSide();
                BlockPos blockPos2 = blockPos.offset(direction);
                if (this.canPlaceRailAt(blockPos2)) {
                    return blockHitResult;
                }
            }
        }

        BlockPos blockPos3 = mc.player.getBlockPos();
        if (this.canPlaceRailAt(blockPos3)) {
            Vec3d vec3d = Vec3d.ofCenter(blockPos3, -0.5);
            Direction direction2 = Direction.UP;
            BlockPos blockPos4 = blockPos3.down();
            return new BlockHitResult(vec3d, direction2, blockPos4, false);
        } else {
            return null;
        }
    }

    private boolean canPlaceRailAt(BlockPos blockPos) {
        if (mc.world.getBlockState(blockPos).isReplaceable()) {
            ClientWorld clientWorld = mc.world;
            BlockPos blockPos2 = blockPos.down();
            return Block.hasTopRim(clientWorld, blockPos2);
        } else {
            return false;
        }
    }

    private void interactBlock(BlockHitResult blockHitResult) {
        boolean flag;
        label79: {
            label81: {
                MinecraftClient minecraftClient;
                try {
                    this.placing = true;
                    minecraftClient = mc;
                } catch (Throwable throwable) {
                    break label81;
                }

                ClientPlayerInteractionManager clientPlayerInteractionManager;
                try {
                    clientPlayerInteractionManager = minecraftClient.interactionManager;
                } catch (Throwable throwable2) {
                    break label81;
                }

                MinecraftClient minecraftClient2;
                try {
                    minecraftClient2 = mc;
                } catch (Throwable throwable3) {
                    break label81;
                }

                ClientPlayerEntity clientPlayerEntity;
                try {
                    clientPlayerEntity = minecraftClient2.player;
                } catch (Throwable throwable4) {
                    break label81;
                }

                Hand hand;
                try {
                    hand = Hand.MAIN_HAND;
                } catch (Throwable throwable5) {
                    break label81;
                }

                ActionResult actionResult;
                try {
                    actionResult = clientPlayerInteractionManager.interactBlock(clientPlayerEntity, hand, blockHitResult);
                } catch (Throwable throwable6) {
                    break label81;
                }

                boolean flag2;
                try {
                    flag2 = actionResult.isAccepted();
                } catch (Throwable throwable7) {
                    break label81;
                }

                if (!flag2) {
                    flag = false;
                    break label79;
                }

                MinecraftClient minecraftClient3;
                try {
                    minecraftClient3 = mc;
                } catch (Throwable throwable8) {
                    break label81;
                }

                ClientPlayerEntity clientPlayerEntity2;
                try {
                    clientPlayerEntity2 = minecraftClient3.player;
                } catch (Throwable throwable9) {
                    break label81;
                }

                Hand hand2;
                try {
                    hand2 = Hand.MAIN_HAND;
                } catch (Throwable throwable10) {
                    break label81;
                }

                try {
                    clientPlayerEntity2.swingHand(hand2);
                } catch (Throwable throwable11) {
                    break label81;
                }

                flag = false;
                break label79;
            }

            this.placing = false;
            flag = this.placing;
        }

        this.placing = flag;
    }

    private void aimAt(Vec3d vec3d) {
        Vec3d vec3d2 = mc.player.getEyePos();
        double d = vec3d.x;
        double d2 = vec3d2.x;
        double d3 = vec3d.y;
        double d4 = vec3d2.y;
        double d5 = vec3d.z;
        double d6 = vec3d2.z;
        double d7 = ExactMath.subtract(d5, d6);
        double d8 = ExactMath.multiply(d7, d7);
        double d9 = ExactMath.subtract(d, d2);
        double d10 = ExactMath.multiply(d9, d9);
        double d11 = Math.sqrt(ExactMath.add(d8, d10));
        double d12 = MathHelper.atan2(d7, d9);
        double d13 = MathHelper.atan2(ExactMath.subtract(d3, d4), d11);
        float f = MathHelper.clamp((float)ExactMath.divide(ExactMath.multiply(180.0, d13), (float) -Math.PI), -89.0F, 89.0F);
        float f2 = ExactMath.add((float)ExactMath.divide(ExactMath.multiply(d12, 180.0), (float) Math.PI), -90.0F);
        RotationComponent.rotateTo(new Rotation(f2, f), 180.0F, 90.0F, 2, 10);
    }

    private void releaseUseKey() {
        if (this.holdingUseKey) {
            mc.options.useKey.setPressed(false);
            this.holdingUseKey = false;
        }
    }

    private void reset() {
        this.releaseUseKey();
        ActionSequence actionSequence = this.actionQueue;
        this.aimTarget = null;
        actionSequence.clear();
    }

    private int findRailSlot() {
        long l = 0L;

        while (true) {
            PlayerInventory playerInventory = mc.player.getInventory();
            int i = (int)l;
            ItemStack itemStack = playerInventory.getStack(i);
            TagKey tagKey = ItemTags.RAILS;
            if (itemStack.isIn(tagKey)) {
                break;
            }

            if (++l == 9L) {
                l = 4294967295L;
                break;
            }
        }

        return (int)l;
    }

    private int findFlameBowSlot() {
        long l = 0L;

        while (true) {
            PlayerInventory playerInventory = mc.player.getInventory();
            int i = (int)l;
            ItemStack itemStack = playerInventory.getStack(i);
            if (this.isFlameBow(itemStack)) {
                break;
            }

            if (++l == 9L) {
                l = 4294967295L;
                break;
            }
        }

        return (int)l;
    }

    private boolean isFlameBow(ItemStack itemStack) {
        Item item = Items.BOW;
        if (itemStack.isOf(item)) {
            if (itemStack.hasEnchantments()) {
                DynamicRegistryManager dynamicRegistryManager = mc.world.getRegistryManager();
                RegistryKey registryKey = RegistryKeys.ENCHANTMENT;
                Registry registry = dynamicRegistryManager.getOrThrow(registryKey);
                RegistryKey registryKey2 = Enchantments.FLAME;
                return EnchantmentHelper.getLevel(registry.getOrThrow(registryKey2), itemStack) > 0;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void setSelectedSlot(int i) {
        ((PlayerInventoryAccessor)mc.player.getInventory()).sadnes$setSelectedSlot(i);
    }

    private int getSelectedSlot() {
        return ((PlayerInventoryAccessor)mc.player.getInventory()).sadnes$getSelectedSlot();
    }
}
