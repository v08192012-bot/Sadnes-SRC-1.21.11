package tech.sadnes.client.feature.impl.player;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import tech.sadnes.api.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import tech.sadnes.api.mixin.client.MinecraftClientAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.ui.notification.NotificationSources;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.RaycastUtil;

@FeatureInfo(
    name = "ClickAction",
    description = "feature.impl.player.clickaction",
    displayName = "feature.impl.player.clickaction",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_ITEMS
)
public class ClickAction extends Feature {
    private final KeybindSetting pearlBind = new KeybindSetting("clickaction.pearlBind");
    private final KeybindSetting expBind = new KeybindSetting("clickaction.expBind");
    private final KeybindSetting crossbowBind = new KeybindSetting("clickaction.crossbowBind");
    private final KeybindSetting spearBind = new KeybindSetting("clickaction.spearBind");
    private final KeybindSetting friendBind = new KeybindSetting("clickaction.friendBind");
    private final KeybindSetting windBind = new KeybindSetting("clickaction.windBind");
    private final BooleanSetting boostJump = new BooleanSetting("clickaction.boostJump", false).setVisibleWhen(this.windBind::hasBindsRecursive);
    private final BooleanSetting checkJump = new BooleanSetting("clickaction.checkJump", false).setVisibleWhen(this.windBind::hasBindsRecursive);
    private final BooleanSetting windPause = new BooleanSetting("clickaction.windPause", false).setVisibleWhen(this.windBind::hasBindsRecursive);
    private final BooleanSetting syncHead = new BooleanSetting("clickaction.syncHead", false).setVisibleWhen(this.pearlBind::hasBindsRecursive);
    private final StopWatch actionTimer = new StopWatch();
    private final StopWatch useTimer = new StopWatch();
    private final StopWatch swapBackTimer = new StopWatch();
    private int swapBackStage = -1;
    private boolean actionPending;
    private Runnable pendingAction;
    private final ClickAction.ItemBind[] itemBinds = new ClickAction.ItemBind[]{
        new ClickAction.ItemBind(Items.ENDER_PEARL, this.pearlBind),
        new ClickAction.ItemBind(Items.EXPERIENCE_BOTTLE, this.expBind),
        new ClickAction.ItemBind(Items.CROSSBOW, this.crossbowBind),
        new ClickAction.ItemBind(Items.WIND_CHARGE, this.windBind)
    };

    public static ClickAction getInstance() {
        return Sadnes.getInstance().getFeatureManager() == null ? null : Sadnes.getInstance().getFeatureManager().getFeature(ClickAction.class);
    }

    public boolean isUsingItem() {
        boolean flag = this.isEnabled();
        StopWatch stopWatch = this.useTimer;
        return flag ? !stopWatch.hasPassed(250.0) : false;
    }

    public List<ClickAction.ItemBind> getItemBinds() {
        return List.of(this.itemBinds);
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        this.runPendingAction();
        this.processSwapBack();
    }

    @Override
    public void onEnable() {
        this.resetState();
        super.onEnable();
    }

    private void processSwapBack() {
        label27: {
            if (this.swapBackStage >= 0) {
                boolean flag = ServerManager.isNullWorld();
                int i = this.swapBackStage;
                StopWatch stopWatch = this.swapBackTimer;
                if (flag) {
                    break label27;
                }

                if (++i == 2) {
                    this.swapBackStage = i;
                    stopWatch.reset();
                    Slot slot = InventoryUtil.findSlotReverse(Items.WIND_CHARGE);
                    if (slot != null) {
                        Item item = Items.WIND_CHARGE;
                        KeybindSetting keybindSetting = this.windBind;
                        ClickAction.ItemBind itemBind = new ClickAction.ItemBind(item, keybindSetting);
                        this.useItemFromSlot(itemBind, slot);
                        return;
                    }
                } else {
                    if (i > 2) {
                        this.swapBackStage = i;
                        boolean flag2 = ThrowComponent.getInstance().isIdle();
                        i = this.swapBackStage;
                        if (flag2) {
                            boolean flag3 = stopWatch.hasPassed(150.0);
                            i = this.swapBackStage;
                            if (flag3) {
                                break label27;
                            }
                        }
                    }

                    this.swapBackStage = i;
                    if (stopWatch.hasPassed(1500.0)) {
                        break label27;
                    }
                }
            }

            return;
        }

        this.resetState();
    }

    private void resetState() {
        if (this.swapBackStage >= 0) {
            this.swapBackStage = -1;
            ThrowComponent.getInstance().setBlockMovement(false);
        }
    }

    private void runPendingAction() {
        if (ServerManager.isNullWorld()) {
            this.pendingAction = null;
        } else {
            ThrowComponent throwComponent = ThrowComponent.getInstance();
            Runnable runnable = this.pendingAction;
            if (this.actionPending) {
                Screen screen = mc.currentScreen;
                Runnable runnable2 = this.pendingAction;
                if (screen == null) {
                    if (runnable2 != null) {
                        ItemStack itemStack = mc.player.getMainHandStack();
                        Item item = Items.EXPERIENCE_BOTTLE;
                        if (itemStack.isOf(item)) {
                            if (RotationComponent.getInstance() != null) {
                                float f = mc.player.getYaw();
                                RotationComponent.rotateTo(new Rotation(f, 80.0F), 160.0F, 20.0F, 1, 6);
                            }

                            throwComponent.useHeldItem(null);
                            return;
                        }

                        if (throwComponent.isIdle()) {
                            if (InventoryUtil.findSlotReverse(Items.EXPERIENCE_BOTTLE) != null) {
                                Item item2 = Items.EXPERIENCE_BOTTLE;
                                throwComponent.switchToItem(item2);
                                return;
                            }

                            return;
                        }

                        return;
                    }

                    if (throwComponent.isIdle()) {
                        Item item3 = Items.EXPERIENCE_BOTTLE;
                        Runnable runnable3 = throwComponent.switchToItem(item3);
                        if (runnable3 != null) {
                            this.pendingAction = runnable3;
                            return;
                        }

                        return;
                    }

                    return;
                }

                if (runnable2 == null) {
                    return;
                }
            } else if (runnable == null) {
                return;
            }

            boolean flag = throwComponent.isIdle();
            Runnable runnable4 = this.pendingAction;
            if (flag) {
                runnable4.run();
                this.pendingAction = null;
            }
        }
    }

    @EventHandler
    public void onKey(ScreenMouseClickEvent screenMouseClickEvent) {
        KeybindSetting keybindSetting = this.expBind;
        int i = screenMouseClickEvent.getButton();
        boolean flag = keybindSetting.matchesKey(i);
        boolean flag2 = this.actionPending;
        ClickAction clickAction;
        boolean flag3;
        if (flag) {
            if (!screenMouseClickEvent.isRelease()) {
                if (screenMouseClickEvent.getScreen() != null) {
                    clickAction = this;
                    flag3 = false;
                } else {
                    clickAction = this;
                    flag3 = true;
                }
            } else {
                clickAction = this;
                flag3 = false;
            }
        } else {
            clickAction = this;
            flag3 = flag2;
        }

        clickAction.actionPending = flag3;
        if (screenMouseClickEvent.isPress() && screenMouseClickEvent.getScreen() == null) {
            int i2 = screenMouseClickEvent.getButton();
            this.handleBindPress(i2);
        }
    }

    @EventHandler
    public void onMouseButton(ScreenKeyEvent screenKeyEvent) {
        KeybindSetting keybindSetting = this.expBind;
        int i = screenKeyEvent.getKey();
        boolean flag = keybindSetting.matchesKey(i);
        boolean flag2 = this.actionPending;
        ClickAction clickAction;
        boolean flag3;
        if (flag) {
            if (!screenKeyEvent.isRelease()) {
                if (screenKeyEvent.getScreen() != null) {
                    clickAction = this;
                    flag3 = false;
                } else {
                    clickAction = this;
                    flag3 = true;
                }
            } else {
                clickAction = this;
                flag3 = false;
            }
        } else {
            clickAction = this;
            flag3 = flag2;
        }

        clickAction.actionPending = flag3;
        if (screenKeyEvent.isPress() && screenKeyEvent.getScreen() == null) {
            int i2 = screenKeyEvent.getKey();
            this.handleBindPress(i2);
        }
    }

    private void handleBindPress(int i) {
        boolean flag = this.friendBind.matchesKey(i);
        KeybindSetting keybindSetting = this.spearBind;
        if (flag) {
            this.toggleFriendOnTarget();
        } else {
            boolean flag2 = keybindSetting.matchesKey(i);
            ClickAction.ItemBind[] itemBinds2 = this.itemBinds;
            if (flag2) {
                this.useSpear();
            } else {
                int i2 = Array.getLength(itemBinds2);
                if (i2 > 0) {
                    int i3 = i2 & 2147483647;
                    long l = 0L;

                    do {
                        int i4 = (int)l;
                        Object object = Array.get(itemBinds2, i4);
                        Item item2 = ((ClickAction.ItemBind)object).item;
                        Item item3 = Items.EXPERIENCE_BOTTLE;
                        if (item2 != item3 && ((ClickAction.ItemBind)object).bind.matchesKey(i)) {
                            Slot slot = InventoryUtil.findSlotReverse(((ClickAction.ItemBind)object).item);
                            if (slot != null) {
                                this.useItemFromSlot((ClickAction.ItemBind)object, slot);
                                return;
                            }

                            NotificationType notificationType = NotificationType.WARNING;
                            String string = this.getDisplayName();
                            ItemStack itemStack = ((ClickAction.ItemBind)object).item.asItem().getDefaultStack();
                            String string2 = Language.translate("clickaction.noItem", "РџСЂРµРґРјРµС‚Р° %s РЅРµС‚ РІ РёРЅРІРµРЅС‚Р°СЂРµ!");
                            Object[] objects = new Object[1];
                            String string3 = ((ClickAction.ItemBind)object).item.asItem().getName().getString();
                            Array.set(objects, 0, string3);
                            String string4 = String.format(string2, objects);
                            NotificationUtil.notify(notificationType, string, itemStack, string4);
                            return;
                        }
                    } while (Integer.toUnsignedLong(i3) != ++l);
                }
            }
        }
    }

    private void useItemFromSlot(ClickAction.ItemBind itemBind, Slot slot) {
        Item item2 = itemBind.item;
        Item item3 = Items.WIND_CHARGE;
        BooleanSetting booleanSetting = this.checkJump;
        if (item2 == item3 && booleanSetting.getValue()) {
            if (!mc.player.isGliding()) {
                double d = mc.player.fallDistance;
                int i = this.swapBackStage;
                int i2 = !Double.isNaN(d) ? (d == 0.0 ? 0 : 1) : 0;
                if (Double.isNaN(d) || i2 != 0) {
                    this.swapBackStage = i;
                    NotificationType notificationType = NotificationType.WARNING;
                    String string = this.getDisplayName();
                    ItemStack itemStack = Items.WIND_CHARGE.getDefaultStack();
                    String string2 = Language.translate(
                        "clickaction.windFalling", "Р’С‹ РІ РїР°РґРµРЅРёРё вЂ” Р·Р°СЂСЏРґ РІРµС‚СЂР° РїСЂРѕРїР°РґС‘С‚ РІРїСѓСЃС‚СѓСЋ"
                    );
                    NotificationUtil.notify(notificationType, string, itemStack, string2);
                    return;
                }

                this.swapBackStage = i;
                if (this.canBoostJump()) {
                    NotificationType notificationType2 = NotificationType.WARNING;
                    String string3 = this.getDisplayName();
                    ItemStack itemStack2 = Items.WIND_CHARGE.getDefaultStack();
                    String string4 = Language.translate(
                        "clickaction.windNoGround", "РџРѕРґ РІР°РјРё РЅРµС‚ РѕРїРѕСЂС‹ вЂ” Р·Р°СЂСЏРґ РІРµС‚СЂР° СѓР»РµС‚РёС‚ РІРЅРёР·"
                    );
                    NotificationUtil.notify(notificationType2, string3, itemStack2, string4);
                    return;
                }
            }

            if (this.isInsideBlock()) {
                NotificationType notificationType3 = NotificationType.WARNING;
                String string5 = this.getDisplayName();
                ItemStack itemStack3 = Items.WIND_CHARGE.getDefaultStack();
                String string6 = Language.translate("clickaction.windBlocked", "РќР°Рґ РІР°РјРё Р±Р»РѕРєРё вЂ” Р·Р°СЂСЏРґ РІРµС‚СЂР° РЅРµ РїРѕРґР±СЂРѕСЃРёС‚");
                NotificationUtil.notify(notificationType3, string5, itemStack3, string6);
                return;
            }
        }

        Item item4 = itemBind.item;
        Item item5 = Items.WIND_CHARGE;
        BooleanSetting booleanSetting2 = this.windPause;
        int i3 = this.swapBackStage;
        StopWatch stopWatch = this.useTimer;
        if (item4 == item5) {
            boolean flag = booleanSetting2.getValue();
            i3 = this.swapBackStage;
            StopWatch stopWatch2 = this.swapBackTimer;
            if (flag) {
                if (i3 < 0) {
                    this.swapBackStage = 0;
                    stopWatch2.reset();
                    ThrowComponent.getInstance().setBlockMovement(true);
                    return;
                }

                if (Integer.compareUnsigned(i3, 2) < 0) {
                    this.swapBackStage = i3;
                    return;
                }
            }
        }

        this.swapBackStage = i3;
        stopWatch.reset();
        ThrowComponent throwComponent = ThrowComponent.getInstance();
        Item item6 = itemBind.item;
        Item item7 = Items.WIND_CHARGE;
        if (item6 != item7) {
            Hand hand;
            label96: {
                Hand hand2 = mc.player.getActiveHand();
                Hand hand3 = Hand.MAIN_HAND;
                Hand hand4;
                BooleanSetting booleanSetting3;
                if (hand2 == hand3) {
                    if (mc.player.getItemUseTime() == 0) {
                        Hand hand5 = Hand.MAIN_HAND;
                        Item item8 = itemBind.item;
                        Item item9 = Items.ENDER_PEARL;
                        BooleanSetting booleanSetting4 = this.syncHead;
                        if (item8 != item9) {
                            hand = hand5;
                            break label96;
                        }

                        hand4 = hand5;
                        booleanSetting3 = booleanSetting4;
                    } else {
                        Hand hand6 = Hand.OFF_HAND;
                        Item item10 = itemBind.item;
                        Item item11 = Items.ENDER_PEARL;
                        BooleanSetting booleanSetting5 = this.syncHead;
                        if (item10 != item11) {
                            hand = hand6;
                            break label96;
                        }

                        hand4 = hand6;
                        booleanSetting3 = booleanSetting5;
                    }
                } else {
                    Hand hand7 = Hand.MAIN_HAND;
                    Item item12 = itemBind.item;
                    Item item13 = Items.ENDER_PEARL;
                    BooleanSetting booleanSetting6 = this.syncHead;
                    if (item12 != item13) {
                        hand = hand7;
                        break label96;
                    }

                    hand4 = hand7;
                    booleanSetting3 = booleanSetting6;
                }

                if (booleanSetting3.getValue()) {
                    float f = Rotation.getCameraYaw();
                    float f2 = Rotation.getCameraPitch();
                    RotationComponent.rotateTo(new Rotation(f, f2), 999.0F, 999.0F, 2, 6);
                }

                throwComponent.useSlotRotating(slot, hand4);
                return;
            }

            throwComponent.useSlotFull(slot, hand, null);
        } else if (!ServerManager.isServer("spookytime") && !ServerManager.isServer("dildo")) {
            if (RotationComponent.getInstance() != null) {
                float f3 = mc.player.getYaw();
                RotationComponent.rotateTo(new Rotation(f3, 90.0F), 999.0F, 999.0F, 2, 6);
            }

            float f4 = mc.player.getYaw();
            float f5 = mc.player.getPitch();
            Vector2f vector2f = new Vector2f(f4, f5);
            throwComponent.useSlotWithRotation(slot, vector2f);
        } else {
            float f6 = Rotation.getCameraYaw();
            Vector2f vector2f2 = new Vector2f(f6, 89.0F);
            throwComponent.useSlotWithRotation(slot, vector2f2);
        }
    }

    private boolean isInsideBlock() {
        if (!ServerManager.isNullWorld()) {
            Box box = mc.player.getBoundingBox();
            double d = box.minX;
            double d2 = box.maxY;
            double d3 = box.minZ;
            double d4 = box.maxX;
            double d5 = box.maxY;
            double d6 = box.maxZ;
            double d7 = ExactMath.add(d5, 5.0);
            Box box2 = new Box(d, d2, d3, d4, d7, d6).contract(1.0E-7);
            ClientWorld clientWorld = mc.world;
            ClientPlayerEntity clientPlayerEntity = mc.player;
            return !clientWorld.isSpaceEmpty(clientPlayerEntity, box2);
        } else {
            return false;
        }
    }

    private boolean canBoostJump() {
        if (!ServerManager.isNullWorld()) {
            Vec3d vec3d = mc.player.getEntityPos().add(0.0, 0.05, 0.0);
            Vec3d vec3d2 = vec3d.add(0.0, -2.0, 0.0);
            ClientWorld clientWorld = mc.world;
            ShapeType shapeType = ShapeType.COLLIDER;
            FluidHandling fluidHandling = FluidHandling.NONE;
            ClientPlayerEntity clientPlayerEntity = mc.player;
            RaycastContext raycastContext = new RaycastContext(vec3d, vec3d2, shapeType, fluidHandling, clientPlayerEntity);
            Type type = clientWorld.raycast(raycastContext).getType();
            Type type2 = Type.MISS;
            return type == type2;
        } else {
            return false;
        }
    }

    public void useSpear() {
        if (!ServerManager.isNullWorld()) {
            if (mc.world != null && mc.interactionManager != null && !mc.player.isUsingItem() && !mc.options.useKey.isPressed()) {
                int i = Key.MOUSE_RIGHT.getKeyCode();
                if (i >= 0) {
                    MinecraftClient minecraftClient = MinecraftClient.getInstance();
                    if (minecraftClient != null && minecraftClient.getWindow() != null) {
                        long l = minecraftClient.getWindow().getHandle();
                        if (l != 0L) {
                            if (Integer.compareUnsigned(i, 8) < 0) {
                                int i2 = i & 7;
                                if (GLFW.glfwGetMouseButton(l, i2) == 1) {
                                    return;
                                }
                            } else {
                                int i3 = i & 2147483647;
                                if (GLFW.glfwGetKey(l, i3) == 1) {
                                    return;
                                }
                            }
                        }
                    }
                }

                DynamicRegistryManager dynamicRegistryManager = mc.world.getRegistryManager();
                RegistryKey registryKey = RegistryKeys.ENCHANTMENT;
                Registry registry = dynamicRegistryManager.getOrThrow(registryKey);
                RegistryKey registryKey2 = Enchantments.LUNGE;
                Optional optional = registry.getOptional(registryKey2);
                if (!optional.isEmpty()) {
                    RegistryEntry registryEntry = (RegistryEntry)optional.get();
                    PlayerInventory playerInventory = mc.player.getInventory();
                    long l2 = 0L;

                    while (true) {
                        int i4 = (int)l2;
                        ItemStack itemStack = playerInventory.getStack(i4);
                        if (!itemStack.isEmpty()) {
                            TagKey tagKey = ItemTags.SPEARS;
                            if (itemStack.isIn(tagKey)) {
                                if (EnchantmentHelper.getLevel(registryEntry, itemStack) != 0) {
                                    int i5 = playerInventory.getSelectedSlot();
                                    if ((int)l2 != i5) {
                                        int i6 = (int)l2;
                                        playerInventory.setSelectedSlot(i6);
                                    }

                                    ((MinecraftClientAccessor)mc).sadnes$invokeDoAttack();
                                    if ((int)l2 != i5) {
                                        playerInventory.setSelectedSlot(i5);
                                        ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$invokeSyncSelectedSlot();
                                        return;
                                    }

                                    return;
                                }

                                if (++l2 == 9L) {
                                    break;
                                }
                            } else {
                                if (++l2 != 9L) {
                                    continue;
                                }
                                break;
                            }
                        } else {
                            if (++l2 != 9L) {
                                continue;
                            }
                            break;
                        }
                    }

                    NotificationType notificationType = NotificationType.WARNING;
                    String string = this.getDisplayName();
                    ItemStack itemStack2 = Items.IRON_SPEAR.getDefaultStack();
                    String string2 = Language.translate("clickaction.noSpear", "РљРѕРїСЊСЏ СЃ Р±СѓСЃС‚РѕРј РЅРµС‚ РІ С…РѕС‚Р±Р°СЂРµ!");
                    NotificationUtil.notify(notificationType, string, itemStack2, string2);
                    return;
                }
            }
        }
    }

    private void toggleFriendOnTarget() {
        if (!ServerManager.isNullWorld()) {
            PlayerEntity playerEntity = this.getTargetedPlayer();
            if (playerEntity != null) {
                String string = Formatting.strip(playerEntity.getGameProfile().name());
                if (string != null) {
                    if (!Sadnes.getInstance().getFriendManager().isFriend(string)) {
                        Sadnes.getInstance().getFriendManager().addFriend(string);
                        String string2 = NotificationSources.friends();
                        String string3 = Language.translate("clickaction.friendAdded", "\"%s\" РґРѕР±Р°РІР»РµРЅ РІ СЃРїРёСЃРѕРє РґСЂСѓР·РµР№.");
                        Object[] objects = new Object[1];
                        Array.set(objects, 0, string);
                        String string4 = String.format(string3, objects);
                        NotificationUtil.success(string2, string4);
                    } else {
                        Sadnes.getInstance().getFriendManager().removeFriend(string);
                        String string5 = NotificationSources.friends();
                        String string6 = Language.translate("clickaction.friendRemoved", "\"%s\" СѓРґР°Р»С‘РЅ РёР· СЃРїРёСЃРєР° РґСЂСѓР·РµР№.");
                        Object[] objects2 = new Object[1];
                        Array.set(objects2, 0, string);
                        String string7 = String.format(string6, objects2);
                        NotificationUtil.warning(string5, string7);
                    }
                }
            }
        }
    }

    private PlayerEntity getTargetedPlayer() {
        Entity entity2 = mc.getCameraEntity();
        if (entity2 != null) {
            if (mc.world != null) {
                if (mc.player != null) {
                    double d = mc.player.getEntityInteractionRange();
                    Vec3d vec3d = entity2.getEyePos();
                    Vec3d vec3d2 = entity2.getRotationVec(1.0F);
                    Vec3d vec3d3 = vec3d2.multiply(d);
                    Vec3d vec3d4 = vec3d.add(vec3d3);
                    Box box = entity2.getBoundingBox();
                    Vec3d vec3d5 = vec3d2.multiply(d);
                    Box box2 = box.stretch(vec3d5).expand(1.0);
                    Predicate<Entity> predicate = entity3 -> entity3 instanceof PlayerEntity && !entity3.isSpectator() && entity3.canHit();
                    EntityHitResult entityHitResult = RaycastUtil.raycastEntities(entity2, vec3d, vec3d4, box2, predicate, d);
                    if (entityHitResult != null) {
                        Entity entity4 = entityHitResult.getEntity();
                        return entity4 instanceof PlayerEntity ? (PlayerEntity)entity4 : null;
                    } else {
                        return null;
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

    public KeybindSetting getPearlBind() {
        return this.pearlBind;
    }

    public KeybindSetting getExpBind() {
        return this.expBind;
    }

    public KeybindSetting getCrossbowBind() {
        return this.crossbowBind;
    }

    public KeybindSetting getSpearBind() {
        return this.spearBind;
    }

    public KeybindSetting getFriendBind() {
        return this.friendBind;
    }

    public KeybindSetting getWindBind() {
        return this.windBind;
    }

    public BooleanSetting getBoostJump() {
        return this.boostJump;
    }

    public BooleanSetting getCheckJump() {
        return this.checkJump;
    }

    public BooleanSetting getWindPause() {
        return this.windPause;
    }

    public BooleanSetting getSyncHead() {
        return this.syncHead;
    }

    public StopWatch getActionTimer() {
        return this.actionTimer;
    }

    public StopWatch getUseTimer() {
        return this.useTimer;
    }

    public StopWatch getSwapBackTimer() {
        return this.swapBackTimer;
    }

    public int getSwapBackStage() {
        return this.swapBackStage;
    }

    public boolean isActionPending() {
        return this.actionPending;
    }

    public Runnable getPendingAction() {
        return this.pendingAction;
    }

    public record ItemBind(Item item, KeybindSetting bind) {
    }
}
