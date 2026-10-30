package tech.sadnes.client.feature.impl.combat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MouseLookEvent;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.SlotClickEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.ui.RadialMenu;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "AutoSwap",
    description = "feature.impl.combat.autoswap",
    displayName = "feature.impl.combat.autoswap",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_INVENTORY
)
public class AutoSwap extends Feature {
    private static final int MAX_WHEEL_ITEMS = 5;
    private final ModeSetting mode = new ModeSetting("autoswap.mode", new String[]{"wheel", "normal", "auto", "manual"});
    private final KeybindSetting key = new KeybindSetting("autoswap.key").setVisibleWhen(this::isWheelOrNormalMode);
    private final BooleanSetting notifications = new BooleanSetting("autoswap.notify", true);
    private final ModeSetting smartArmorItem = new ModeSetting("autoswap.smartArmorItem", getSphereItemIds())
        .setItemIconProvider(AutoSwap::getSphereIcon)
        .setVisibleWhen(() -> this.mode.is("manual"));
    private final ModeSetting smartDamageItem = new ModeSetting("autoswap.smartDamageItem", getSphereItemIds())
        .setItemIconProvider(AutoSwap::getSphereIcon)
        .setVisibleWhen(() -> this.mode.is("manual"));
    private final ModeSetting swapType1 = new ModeSetting("autoswap.swapType1", new String[]{"totem", "shield", "gapple", "ball"}).setVisibleWhen(() -> this.mode.is("normal"));
    private final ModeSetting swapType2 = new ModeSetting("autoswap.swapType2", new String[]{"gapple", "shield", "totem", "ball"}).setVisibleWhen(() -> this.mode.is("normal"));
    private final BooleanSetting ignoreUnenchanted = new BooleanSetting("autoswap.ignoreUnenchanted", true).setVisibleWhen(() -> this.mode.is("normal"));
    private final KeybindSetting extraKey = new KeybindSetting("autoswap.extraKey").setVisibleWhen(() -> this.mode.is("normal"));
    private final ModeSetting extraItem = new ModeSetting("autoswap.extraItem", getSphereItemIds())
        .setItemIconProvider(AutoSwap::getSphereIcon)
        .setVisibleWhen(() -> this.mode.is("normal") && isBindSet(this.extraKey) ? true : false);
    private final List<AutoSwap.WheelEntry> wheelEntries = new ArrayList<>();
    private boolean editingWheel;
    private boolean ignoreNextMouseRelease;
    private final StopWatch wheelOpenTimer = new StopWatch();
    private final StopWatch smartSwapTimer = new StopWatch();
    private final RadialMenu wheel = new RadialMenu()
        .withIcon(Icon.PLUS)
        .withDefaultLabel(
            () -> this.wheelEntries.isEmpty()
                ? Language.translate("autoswap.wheel.empty", "Добавить / убрать")
                : Language.translate("autoswap.wheel.add", "Добавить / убрать")
        )
        .withHint(() -> Language.translate("autoswap.wheel.hint", "В сторону — свап · в центр — предметы"));
    private boolean wheelHeld;
    private int lastWheelIndex = -1;
    private int previousWheelIndex = -1;

    private static boolean isBindSet(KeybindSetting keybindSetting) {
        return keybindSetting.getDefaultValue() != -1 || !keybindSetting.getBinds().isEmpty();
    }

    private boolean isWheelOrNormalMode() {
        return this.mode.is("wheel") || this.mode.is("normal");
    }

    private boolean isSmartMode() {
        return this.mode.is("auto") || this.mode.is("manual");
    }

    public static AutoSwap getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(AutoSwap.class);
    }

    @EventHandler
    public void onKey(ScreenMouseClickEvent screenMouseClickEvent) {
        Screen screen = screenMouseClickEvent.getScreen();
        ModeSetting modeSetting = this.mode;
        if (screen == null) {
            boolean flag = modeSetting.is("normal");
            KeybindSetting keybindSetting = this.extraKey;
            KeybindSetting keybindSetting2 = this.key;
            if (flag) {
                int i = screenMouseClickEvent.getButton();
                if (keybindSetting.matchesKey(i)) {
                    if (screenMouseClickEvent.isPress()) {
                        this.swapExtraItem();
                        return;
                    }

                    return;
                }
            }

            int i2 = screenMouseClickEvent.getButton();
            if (keybindSetting2.matchesKey(i2)) {
                if (modeSetting.is("normal")) {
                    if (screenMouseClickEvent.isPress()) {
                        this.swapBetweenTypes();
                        return;
                    }
                } else if (modeSetting.is("wheel")) {
                    if (screenMouseClickEvent.isPress()) {
                        this.openWheel();
                        return;
                    }

                    if (screenMouseClickEvent.isRelease()) {
                        this.releaseWheel();
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMouseButton(ScreenKeyEvent screenKeyEvent) {
        boolean flag = this.editingWheel;
        boolean flag2 = this.ignoreNextMouseRelease;
        boolean flag3;
        if (flag) {
            boolean flag4 = screenKeyEvent.isKey(0);
            boolean flag5 = this.ignoreNextMouseRelease;
            if (flag4) {
                boolean flag6 = screenKeyEvent.isRelease();
                boolean flag7 = this.ignoreNextMouseRelease;
                if (flag6) {
                    flag3 = false;
                } else {
                    flag3 = flag7;
                }
            } else {
                flag3 = flag5;
            }
        } else {
            flag3 = flag2;
        }

        this.ignoreNextMouseRelease = flag3;
        Screen screen = screenKeyEvent.getScreen();
        RadialMenu radialMenu = this.wheel;
        if (screen == null) {
            if (radialMenu.isOpen() && screenKeyEvent.isPress() && screenKeyEvent.isKey(0)) {
                radialMenu.close();
                this.wheelHeld = false;
                this.openWheelEditor();
            } else {
                boolean flag8 = radialMenu.isOpen();
                ModeSetting modeSetting = this.mode;
                if (flag8 && screenKeyEvent.isPress()) {
                    boolean flag9 = screenKeyEvent.isKey(1);
                    List list = this.wheelEntries;
                    if (flag9) {
                        int i = list.size();
                        int i2 = radialMenu.getSelectedIndex(i);
                        this.removeWheelEntry(i2);
                        return;
                    }
                }

                boolean flag10 = modeSetting.is("normal");
                KeybindSetting keybindSetting = this.extraKey;
                KeybindSetting keybindSetting2 = this.key;
                if (flag10) {
                    int i3 = screenKeyEvent.getKey();
                    if (keybindSetting.matchesKey(i3)) {
                        if (screenKeyEvent.isPress()) {
                            this.swapExtraItem();
                            return;
                        }

                        return;
                    }
                }

                int i4 = screenKeyEvent.getKey();
                boolean flag11 = keybindSetting2.matchesKey(i4);
                ModeSetting modeSetting2 = this.mode;
                if (flag11) {
                    boolean flag12 = modeSetting2.is("normal");
                    ModeSetting modeSetting3 = this.mode;
                    if (flag12) {
                        if (screenKeyEvent.isPress()) {
                            this.swapBetweenTypes();
                        }
                    } else if (modeSetting3.is("wheel")) {
                        if (screenKeyEvent.isPress()) {
                            this.openWheel();
                        } else if (screenKeyEvent.isRelease()) {
                            this.releaseWheel();
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = 250)
    public void onMouseScroll(MouseLookEvent mouseLookEvent) {
        this.wheel.onMouseMove(mouseLookEvent);
    }

    private void openWheel() {
        if (mc.player != null) {
            Screen screen = mc.currentScreen;
            RadialMenu radialMenu = this.wheel;
            if (screen == null) {
                this.wheelHeld = true;
                radialMenu.close();
                StopWatch stopWatch = this.wheelOpenTimer;
                this.editingWheel = false;
                stopWatch.reset();
            }
        }
    }

    private void releaseWheel() {
        boolean flag = this.wheelHeld;
        RadialMenu radialMenu = this.wheel;
        if (flag) {
            this.wheelHeld = false;
            boolean flag2 = radialMenu.isOpen();
            int i = this.wheelEntries.size();
            int i2 = radialMenu.getSelectedIndex(i);
            radialMenu.close();
            if (mc.player != null) {
                if (!flag2) {
                    this.swapToAlternateWheelItem();
                    return;
                }

                if (i2 >= 0) {
                    int i3 = i2 & 2147483647;
                    this.swapToWheelIndex(i3);
                    return;
                }
            }
        }
    }

    private void swapToAlternateWheelItem() {
        int i = this.wheelEntries.size();
        if (i != 0) {
            if (i == 1) {
                this.swapToWheelIndex(0);
            } else {
                int i2 = this.findOffhandWheelIndex();
                int i3 = this.lastWheelIndex;
                int i4 = this.previousWheelIndex;
                if (i3 >= 0) {
                    if (i4 < i) {
                        if (i3 >= i) {
                            if (i2 != 0) {
                                this.swapToWheelIndex(0);
                                return;
                            }

                            this.swapToWheelIndex(1);
                            return;
                        }

                        if (i4 >= 0) {
                            if (i3 == i2) {
                                this.swapToWheelIndex(i4);
                                return;
                            }

                            this.swapToWheelIndex(i3);
                            return;
                        }

                        if (i2 != 0) {
                            this.swapToWheelIndex(0);
                            return;
                        }
                    } else if (i2 != 0) {
                        this.swapToWheelIndex(0);
                        return;
                    }
                } else if (i2 != 0) {
                    this.swapToWheelIndex(0);
                    return;
                }

                this.swapToWheelIndex(1);
            }
        }
    }

    private void swapToWheelIndex(int i) {
        List list = this.wheelEntries;
        if (i >= 0) {
            int i2 = list.size();
            if (i >= i2) {
                return;
            }

            AutoSwap.WheelEntry wheelEntry = (AutoSwap.WheelEntry)list.get(i);
            this.swapToEntry(wheelEntry);
            int i3 = this.lastWheelIndex;
            if (i != i3) {
                this.lastWheelIndex = i;
                this.previousWheelIndex = i3;
                return;
            }
        }
    }

    private void removeWheelEntry(int i) {
        List list = this.wheelEntries;
        if (i >= 0) {
            int i2 = list.size();
            if (i < i2) {
                ItemStack itemStack = ((AutoSwap.WheelEntry)list.remove(i)).stack();
                BooleanSetting booleanSetting = this.notifications;
                this.lastWheelIndex = -1;
                this.previousWheelIndex = -1;
                if (booleanSetting.getValue()) {
                    NotificationType notificationType = NotificationType.INFO;
                    String string = this.getDisplayName();
                    String string2 = Language.translate("autoswap.removed", "РЈР±СЂР°РЅРѕ РёР· РєРѕР»РµСЃР°");
                    NotificationUtil.notify(notificationType, string, itemStack, string2);
                }
            }
        }
    }

    private int findOffhandWheelIndex() {
        ItemStack itemStack = mc.player.getOffHandStack();
        boolean flag = itemStack.isEmpty();
        List list = this.wheelEntries;
        if (flag) {
            return -1;
        }

        if (list.size() > 0) {
            long l = 0L;

            long l2;
            do {
                int i = (int)l;
                if (ItemStack.areItemsAndComponentsEqual(((AutoSwap.WheelEntry)list.get(i)).stack(), itemStack)) {
                    return (int)l;
                }

                l2 = list.size();
            } while (++l < l2);
        }

        if (list.size() <= 0) {
            return -1;
        }

        long l3 = 0L;

        long l4;
        do {
            int i2 = (int)l3;
            Item item = ((AutoSwap.WheelEntry)list.get(i2)).stack().getItem();
            if (itemStack.isOf(item)) {
                return (int)l3;
            }

            l4 = list.size();
        } while (++l3 < l4);

        return -1;
    }

    public void openWheelEditor() {
        if (mc.player != null) {
            this.editingWheel = true;
            this.ignoreNextMouseRelease = true;
            MinecraftClient minecraftClient = mc;
            ClientPlayerEntity clientPlayerEntity = mc.player;
            InventoryScreen inventoryScreen = new InventoryScreen(clientPlayerEntity);
            minecraftClient.setScreen(inventoryScreen);
        }
    }

    @EventHandler
    public void onSlotClick(SlotClickEvent slotClickEvent) {
        if (this.editingWheel && mc.player != null) {
            label48: {
                SlotActionType slotActionType = slotClickEvent.getActionType();
                SlotActionType slotActionType2 = SlotActionType.PICKUP;
                boolean flag = this.ignoreNextMouseRelease;
                if (slotActionType != slotActionType2) {
                    SlotActionType slotActionType3 = SlotActionType.QUICK_MOVE;
                    boolean flag2 = this.ignoreNextMouseRelease;
                    if (slotActionType != slotActionType3) {
                        return;
                    }

                    if (flag2) {
                        break label48;
                    }
                } else if (flag) {
                    break label48;
                }

                ScreenHandler screenHandler = mc.player.currentScreenHandler;
                int i = slotClickEvent.getSlotId();
                if (i >= 0) {
                    int i2 = screenHandler.slots.size();
                    if (i < i2) {
                        int i3 = i & 2147483647;
                        ItemStack itemStack = screenHandler.getSlot(i3).getStack();
                        if (!itemStack.isEmpty()) {
                            this.editingWheel = false;
                            slotClickEvent.cancel();
                            int i4 = this.indexOfWheelEntry(itemStack);
                            List list = this.wheelEntries;
                            if (i4 == -1) {
                                if (list.size() <= 4) {
                                    ItemStack itemStack2 = itemStack.copy();
                                    AutoSwap.WheelEntry wheelEntry = new AutoSwap.WheelEntry(itemStack2);
                                    list.add(wheelEntry);
                                    if (this.notifications.getValue()) {
                                        NotificationType notificationType = NotificationType.SUCCESS;
                                        String string = this.getDisplayName();
                                        ItemStack itemStack3 = itemStack.copy();
                                        String string2 = Language.translate("autoswap.added", "Р”РѕР±Р°РІР»РµРЅРѕ РІ РєРѕР»РµСЃРѕ");
                                        NotificationUtil.notify(notificationType, string, itemStack3, string2);
                                    }
                                }
                            } else {
                                ItemStack itemStack4 = ((AutoSwap.WheelEntry)list.remove(i4)).stack();
                                if (this.notifications.getValue()) {
                                    NotificationType notificationType2 = NotificationType.INFO;
                                    String string3 = this.getDisplayName();
                                    String string4 = Language.translate("autoswap.removed", "РЈР±СЂР°РЅРѕ РёР· РєРѕР»РµСЃР°");
                                    NotificationUtil.notify(notificationType2, string3, itemStack4, string4);
                                }
                            }

                            this.lastWheelIndex = -1;
                            this.previousWheelIndex = -1;
                            MinecraftClient minecraftClient = mc;
                            Runnable runnable = () -> {
                                if (mc.currentScreen instanceof InventoryScreen) {
                                    mc.setScreen(null);
                                }
                            };
                            minecraftClient.execute(runnable);
                        }

                        return;
                    }
                }

                return;
            }

            slotClickEvent.cancel();
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.editingWheel && !(mc.currentScreen instanceof InventoryScreen)) {
            this.editingWheel = false;
            this.handleSmartSwap();
        } else {
            this.handleSmartSwap();
        }
    }

    private void handleSmartSwap() {
        if (this.isSmartMode() && mc.player != null) {
            if (mc.currentScreen != null) {
                return;
            }

            AttackAura attackAura = AttackAura.getInstance();
            if (attackAura != null && attackAura.getCurrentTarget() != null) {
                boolean flag = FunTimeHitSwapHandler.isActive();
                StopWatch stopWatch = this.smartSwapTimer;
                if (!flag && stopWatch.hasPassed(500.0)) {
                    stopWatch.reset();
                    ItemStack itemStack = attackAura.getCurrentTarget().getMainHandStack();
                    boolean flag2 = this.isWeapon(itemStack);
                    boolean flag3 = this.mode.is("manual");
                    ModeSetting modeSetting = this.smartArmorItem;
                    ModeSetting modeSetting2 = this.smartDamageItem;
                    Slot slot;
                    if (flag3) {
                        String string;
                        if (flag2) {
                            string = modeSetting.getValue();
                            ItemStack itemStack2 = mc.player.getOffHandStack();
                            String string2 = this.getAutoBuyId(itemStack2);
                            if (string.equals(string2)) {
                                return;
                            }
                        } else {
                            string = modeSetting2.getValue();
                            ItemStack itemStack3 = mc.player.getOffHandStack();
                            String string3 = this.getAutoBuyId(itemStack3);
                            if (string.equals(string3)) {
                                return;
                            }
                        }

                        Slot slot2 = this.findSlotByAutoBuyId(string);
                        if (slot2 == null) {
                            return;
                        }

                        slot = slot2;
                    } else {
                        Slot slot3;
                        RegistryEntry registryEntry;
                        if (flag2) {
                            RegistryEntry registryEntry2 = EntityAttributes.ARMOR;
                            Slot slot4 = this.findSlotWithBestAttribute(registryEntry2);
                            if (slot4 == null) {
                                return;
                            }

                            slot3 = slot4;
                            registryEntry = registryEntry2;
                        } else {
                            RegistryEntry registryEntry3 = EntityAttributes.ATTACK_DAMAGE;
                            Slot slot5 = this.findSlotWithBestAttribute(registryEntry3);
                            if (slot5 == null) {
                                return;
                            }

                            slot3 = slot5;
                            registryEntry = registryEntry3;
                        }

                        double d = InventoryUtil.getAttributeValue(slot3.getStack(), registryEntry);
                        if (InventoryUtil.getAttributeValue(mc.player.getOffHandStack(), registryEntry) >= d) {
                            return;
                        }

                        slot = slot3;
                    }

                    ItemStack itemStack4 = slot.getStack().copy();
                    Hand hand = Hand.OFF_HAND;
                    InventoryUtil.swapToHand(slot, hand);
                    if (this.notifications.getValue()) {
                        NotificationType notificationType = NotificationType.INFO;
                        String string4 = this.getDisplayName();
                        Text text = itemStack4.getName();
                        NotificationUtil.notify(notificationType, string4, itemStack4, text);
                        return;
                    }
                }
            }
        }
    }

    private boolean isWeapon(ItemStack itemStack) {
        TagKey tagKey = ItemTags.SWORDS;
        if (!itemStack.isIn(tagKey)) {
            TagKey tagKey2 = ItemTags.AXES;
            if (!itemStack.isIn(tagKey2)) {
                Item item = Items.MACE;
                return itemStack.isOf(item);
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    private static String[] getSphereItemIds() {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager == null) {
            return new String[]{""};
        }

        ArrayList arrayList = new ArrayList();

        for (AutoBuyItem autoBuyItem : autoBuyManager.getItems()) {
            if (autoBuyItem.getSource() == AutoBuyItem.Source.SPHERE) {
                String string = autoBuyItem.getId();
                if (!arrayList.contains(string)) {
                    arrayList.add(string);
                }
            }
        }

        return (String[])arrayList.toArray(new String[0]);
    }

    private static ItemStack getSphereIcon(String string) {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager == null) {
            return ItemStack.EMPTY;
        }

        for (AutoBuyItem autoBuyItem : autoBuyManager.getItems()) {
            if (autoBuyItem.getSource() == AutoBuyItem.Source.SPHERE && autoBuyItem.getId().equals(string)) {
                return autoBuyItem.getDisplayStack();
            }
        }

        return ItemStack.EMPTY;
    }

    private String getAutoBuyId(ItemStack itemStack) {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager != null) {
            AutoBuyItem autoBuyItem = autoBuyManager.findMatch(itemStack);
            if (autoBuyItem != null) {
                return autoBuyItem.getId();
            }
        }

        return "";
    }

    private Slot findSlotByAutoBuyId(String string) {
        if (string.isEmpty()) {
            return null;
        }

        Iterator iterator2 = mc.player.currentScreenHandler.slots.iterator();
        if (!iterator2.hasNext()) {
            return null;
        }

        do {
            Slot slot = (Slot)iterator2.next();
            if (this.isPlayerInventorySlot(slot)) {
                ItemStack itemStack = slot.getStack();
                String string2 = this.getAutoBuyId(itemStack);
                if (string.equals(string2)) {
                    return slot;
                }
            }
        } while (iterator2.hasNext());

        return null;
    }

    private Slot findSlotWithBestAttribute(RegistryEntry<EntityAttribute> registryEntry) {
        Iterator iterator2 = mc.player.currentScreenHandler.slots.iterator();
        if (!iterator2.hasNext()) {
            return null;
        }

        Slot slot = null;
        long l = 0L;

        while (true) {
            Slot slot2 = (Slot)iterator2.next();
            if (this.isPlayerInventorySlot(slot2)) {
                double d = InventoryUtil.getAttributeValue(slot2.getStack(), registryEntry);
                if (!(0.0 >= d) && !(Double.longBitsToDouble(l) > d)) {
                    double d2 = Double.longBitsToDouble(l);
                    if (d > d2 || this.pickCheaperSlot(slot, slot2) == slot2) {
                        if (!iterator2.hasNext()) {
                            return slot2;
                        }

                        l = Double.doubleToRawLongBits(d);
                        slot = slot2;
                        continue;
                    }
                }
            }

            if (!iterator2.hasNext()) {
                return slot;
            }
        }
    }

    private void swapToEntry(AutoSwap.WheelEntry wheelEntry) {
        if (mc.player != null && wheelEntry != null) {
            ItemStack itemStack = wheelEntry.stack();
            Slot slot = this.findSlotForStack(itemStack);
            if (slot != null) {
                ItemStack itemStack2 = slot.getStack().copy();
                Hand hand = Hand.OFF_HAND;
                InventoryUtil.swapToHand(slot, hand);
                if (this.notifications.getValue()) {
                    NotificationType notificationType = NotificationType.INFO;
                    String string = this.getDisplayName();
                    Text text = itemStack2.getName();
                    NotificationUtil.notify(notificationType, string, itemStack2, text);
                    return;
                }
            }
        }
    }

    private void swapBetweenTypes() {
        if (mc.player != null) {
            Item item = mc.player.getOffHandStack().getItem();
            String string = this.swapType1.getValue();
            Item item2 = this.getItemForType(string);
            Slot slot = this.findSlotForItem(item2);
            String string2 = this.swapType2.getValue();
            Item item3 = this.getItemForType(string2);
            Slot slot2 = this.findSlotForItem(item3);
            if (slot != null) {
                if (slot2 != null) {
                    Item item4 = slot.getStack().getItem();
                    if (item != item4) {
                        slot2.getStack().getItem();
                    } else {
                        slot = slot2;
                    }
                }
            } else {
                if (slot2 == null) {
                    return;
                }

                slot = slot2;
            }

            ItemStack itemStack = slot.getStack().copy();
            Hand hand = Hand.OFF_HAND;
            InventoryUtil.swapToHand(slot, hand);
            if (this.notifications.getValue()) {
                NotificationType notificationType = NotificationType.INFO;
                String string3 = this.getDisplayName();
                Text text = itemStack.getName();
                NotificationUtil.notify(notificationType, string3, itemStack, text);
                return;
            }
        }
    }

    private void swapExtraItem() {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        ModeSetting modeSetting = this.extraItem;
        if (clientPlayerEntity != null) {
            String string = modeSetting.getValue();
            Slot slot = this.findSlotByAutoBuyId(string);
            if (slot != null) {
                ItemStack itemStack = slot.getStack().copy();
                Hand hand = Hand.OFF_HAND;
                InventoryUtil.swapToHand(slot, hand);
                if (this.notifications.getValue()) {
                    NotificationType notificationType = NotificationType.INFO;
                    String string2 = this.getDisplayName();
                    Text text = itemStack.getName();
                    NotificationUtil.notify(notificationType, string2, itemStack, text);
                    return;
                }
            }
        }
    }

    private Slot findSlotForItem(Item item) {
        if (Items.AIR == item) {
            return null;
        } else {
            Item item2 = Items.TOTEM_OF_UNDYING;
            BooleanSetting booleanSetting = this.ignoreUnenchanted;
            if (item2 == item && booleanSetting.getValue()) {
                Slot slot = this.findSlotForItemFiltered(item, true);
                return slot != null ? slot : this.findSlotForItemFiltered(item, false);
            } else {
                return this.findSlotForItemFiltered(item, false);
            }
        }
    }

    private Slot findSlotForItemFiltered(Item item, boolean flag) {
        boolean flag2 = isBindSet(this.extraKey);
        ModeSetting modeSetting = this.extraItem;
        String string;
        if (flag2) {
            string = modeSetting.getValue();
        } else {
            string = "";
        }

        Iterator iterator2 = mc.player.currentScreenHandler.slots.iterator();
        if (!iterator2.hasNext()) {
            return null;
        }

        Slot slot = null;
        Slot slot2 = null;

        while (true) {
            while (true) {
                Slot slot3 = (Slot)iterator2.next();
                ItemStack itemStack = slot3.getStack();
                if (itemStack.isEmpty() || !this.isPlayerInventorySlot(slot3) || !itemStack.isOf(item)) {
                    break;
                }

                boolean flag3 = false;
                if (flag && !itemStack.hasEnchantments()) {
                    break;
                }

                if (!string.isEmpty()) {
                    String string2 = this.getAutoBuyId(itemStack);
                    if (string.equals(string2)) {
                        break;
                    }
                }

                if ((ServerManager.isServer("funtime") || ServerManager.isLocalProxy())
                    && (Items.TOTEM_OF_UNDYING == item || Items.PLAYER_HEAD == item)
                    && itemStack.getComponents().toString().contains("don-item")) {
                    slot = this.pickCheaperSlot(slot, slot3);
                    if (!iterator2.hasNext()) {
                        if (slot == null) {
                            return slot2;
                        }

                        return slot;
                    }
                } else {
                    slot2 = this.pickCheaperSlot(slot2, slot3);
                    if (!iterator2.hasNext()) {
                        if (slot == null) {
                            return slot2;
                        }

                        return slot;
                    }
                }
            }

            if (!iterator2.hasNext()) {
                if (slot == null) {
                    return slot2;
                }

                return slot;
            }
        }
    }

    private Item getItemForType(String string) {
        switch (string.hashCode()) {
            case -1253135533:
                if (string.equals("gapple")) {
                    return Items.GOLDEN_APPLE;
                }
                break;
            case -903340183:
                if (string.equals("shield")) {
                    return Items.SHIELD;
                }
                break;
            case 3016191:
                if (string.equals("ball")) {
                    return Items.PLAYER_HEAD;
                }
                break;
            case 110549953:
                if (string.equals("totem")) {
                    return Items.TOTEM_OF_UNDYING;
                }
        }

        return Items.AIR;
    }

    @EventHandler
    public void onRender2D(Render2DEvent render2DEvent) {
        Screen screen = mc.currentScreen;
        RadialMenu radialMenu = this.wheel;
        boolean flag = this.wheelHeld;
        if (screen != null) {
            radialMenu.closeImmediately();
        } else {
            if (flag) {
                boolean flag2 = RadialMenu.isAnyOpen();
                StopWatch stopWatch = this.wheelOpenTimer;
                if (!flag2 && stopWatch.hasPassed(100.0)) {
                    radialMenu.open();
                }
            }

            boolean flag3 = radialMenu.isVisible();
            List list = this.wheelEntries;
            if (flag3) {
                int i = list.size();
                ArrayList arrayList = new ArrayList(i);
                Iterator iterator2 = list.iterator();
                if (iterator2.hasNext()) {
                    do {
                        AutoSwap.WheelEntry wheelEntry = (AutoSwap.WheelEntry)iterator2.next();
                        ItemStack itemStack = wheelEntry.stack();
                        String string = wheelEntry.displayName().getString();
                        RadialMenu.Entry entry = new RadialMenu.Entry(itemStack, string);
                        arrayList.add(entry);
                    } while (iterator2.hasNext());
                }

                DrawContext drawContext = render2DEvent.getContext();
                radialMenu.render(drawContext, arrayList);
            }
        }
    }

    private int indexOfWheelEntry(ItemStack itemStack) {
        int i = this.wheelEntries.size();
        List list = this.wheelEntries;
        if (i > 0) {
            long l = 0L;

            long l2;
            do {
                int i2 = (int)l;
                if (ItemStack.areItemsAndComponentsEqual(((AutoSwap.WheelEntry)list.get(i2)).stack(), itemStack)) {
                    return (int)l;
                }

                l2 = list.size();
            } while (++l < l2);

            return -1;
        } else {
            return -1;
        }
    }

    private Slot findSlotForStack(ItemStack itemStack) {
        Slot slot = null;
        Slot slot2 = null;
        Slot slot3 = null;

        for (Slot slot4 : mc.player.currentScreenHandler.slots) {
            ItemStack itemStack2 = slot4.getStack();
            if (!itemStack2.isEmpty() && this.isPlayerInventorySlot(slot4)) {
                if (ItemStack.areItemsAndComponentsEqual(itemStack2, itemStack)) {
                    slot = this.pickCheaperSlot(slot, slot4);
                } else if (itemStack2.isOf(itemStack.getItem()) && this.hasSameName(itemStack2, itemStack)) {
                    slot2 = this.pickCheaperSlot(slot2, slot4);
                } else if (itemStack2.isOf(itemStack.getItem())) {
                    slot3 = this.pickCheaperSlot(slot3, slot4);
                }
            }
        }

        if (slot != null) {
            return slot;
        } else if (slot2 != null) {
            return slot2;
        } else if (itemStack.getComponents().isEmpty()) {
            return slot3;
        } else if (ItemStack.areItemsAndComponentsEqual(mc.player.getOffHandStack(), itemStack)) {
            NotificationUtil.notify(NotificationType.INFO, this.getDisplayName(), itemStack, itemStack.getName().getString() + " уже находится в левой руке");
            return null;
        } else {
            NotificationUtil.notify(NotificationType.WARNING, this.getDisplayName(), itemStack, itemStack.getName().getString() + " не найден");
            return null;
        }
    }

    private boolean isPlayerInventorySlot(Slot slot) {
        Inventory inventory2 = slot.inventory;
        PlayerInventory playerInventory = mc.player.getInventory();
        return inventory2 == playerInventory ? slot.getIndex() < 36 : false;
    }

    private boolean hasSameName(ItemStack itemStack, ItemStack itemStack2) {
        String string = itemStack.getName().getString();
        String string2 = itemStack2.getName().getString();
        return string.equals(string2);
    }

    private Slot pickCheaperSlot(Slot slot, Slot slot2) {
        if (slot != null) {
            int i = this.getSwapCost(slot2);
            int i2 = this.getSwapCost(slot);
            return i < i2 ? slot2 : slot;
        } else {
            return slot2;
        }
    }

    private int getSwapCost(Slot slot) {
        int i = InventoryUtil.toHotbarIndex(slot.id);
        int i2 = mc.player.getInventory().getSelectedSlot();
        if (i != -1) {
            return i == i2 ? 0 : 1;
        } else {
            return i == i2 ? 0 : 2;
        }
    }

    @Override
    public void toggle() {
        super.toggle();
        this.wheel.closeImmediately();
        this.wheelHeld = false;
        this.editingWheel = false;
    }

    public ModeSetting getMode() {
        return this.mode;
    }

    public KeybindSetting getKey() {
        return this.key;
    }

    public BooleanSetting getNotifications() {
        return this.notifications;
    }

    public ModeSetting getSmartArmorItem() {
        return this.smartArmorItem;
    }

    public ModeSetting getSmartDamageItem() {
        return this.smartDamageItem;
    }

    public ModeSetting getSwapType1() {
        return this.swapType1;
    }

    public ModeSetting getSwapType2() {
        return this.swapType2;
    }

    public BooleanSetting getIgnoreUnenchanted() {
        return this.ignoreUnenchanted;
    }

    public KeybindSetting getExtraKey() {
        return this.extraKey;
    }

    public ModeSetting getExtraItem() {
        return this.extraItem;
    }

    public List<AutoSwap.WheelEntry> getWheelEntries() {
        return this.wheelEntries;
    }

    public boolean isEditingWheel() {
        return this.editingWheel;
    }

    public boolean isIgnoreNextMouseRelease() {
        return this.ignoreNextMouseRelease;
    }

    public StopWatch getWheelOpenTimer() {
        return this.wheelOpenTimer;
    }

    public StopWatch getSmartSwapTimer() {
        return this.smartSwapTimer;
    }

    public RadialMenu getWheel() {
        return this.wheel;
    }

    public boolean isWheelHeld() {
        return this.wheelHeld;
    }

    public int getLastWheelIndex() {
        return this.lastWheelIndex;
    }

    public int getPreviousWheelIndex() {
        return this.previousWheelIndex;
    }

    record WheelEntry(ItemStack stack) {
        Text displayName() {
            return this.stack.getName();
        }
    }
}
