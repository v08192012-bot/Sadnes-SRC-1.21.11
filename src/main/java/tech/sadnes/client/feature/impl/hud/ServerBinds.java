package tech.sadnes.client.feature.impl.hud;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import tech.sadnes.api.mixin.accessor.ItemCooldownManagerAccessor;
import tech.sadnes.api.mixin.accessor.ItemCooldownManagerEntryAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.Bind;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.HudFeature;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.misc.ItemHelper;
import tech.sadnes.client.feature.impl.misc.ServerAssist;
import tech.sadnes.client.feature.impl.movement.ElytraSwap;
import tech.sadnes.client.feature.impl.player.ClickAction;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.DonateItem;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.ui.hud.HudEntry;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "ServerBinds",
    description = "feature.impl.hud.serverbinds",
    displayName = "feature.impl.hud.serverbinds",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_BINDS
)
public class ServerBinds extends HudFeature<ServerBinds.ServerBindEntry> {
    private static final float ICON_SIZE = 8.0F;
    public final ModeSetting orientation = (ModeSetting)((Setting<?>)new ModeSetting("serverbinds.orientation", "Vertical", new String[]{"Vertical", "Horizontal"})).setDisplayName("Orientation");
    public final BooleanSetting singleLine = (BooleanSetting)new BooleanSetting("serverbinds.singleLine", false)
        .setVisibleWhen(() -> "Horizontal".equals(this.orientation.getValue()))
        .setDisplayName("Single line");
    public final BooleanSetting showHeader = new BooleanSetting("hud.showHeader", true);
    private final Map<Bind, ServerBinds.ServerBindEntry> entryMap = new IdentityHashMap<>();

    public ServerBinds() {
        super(10.0F, 120.0F);
    }

    @Override
    public boolean isHeaderHidden() {
        return !this.showHeader.getValue();
    }

    @Override
    public boolean isCompact() {
        return "Horizontal".equals(this.orientation.getValue());
    }

    @Override
    public float getEntryHeight() {
        return Boolean.TRUE.equals(this.singleLine.getValue()) ? Float.MAX_VALUE : 150.0F;
    }

    @Override
    public String getTitle() {
        return "Server Binds";
    }

    @Override
    public Icon getIcon() {
        return Icon.PACKAGE;
    }

    @Override
    public void collectEntries(List<ServerBinds.ServerBindEntry> serverBindEntries) {
        this.entryMap.values().forEach(serverBindEntry -> serverBindEntry.active = false);
        ServerAssist serverAssist = Sadnes.getInstance().getFeatureManager().getFeature(ServerAssist.class);
        if (serverAssist != null) {
            serverAssist.getItemBinds().forEach((donateItem, keybindSetting) -> {
                if (serverAssist.getServer().is(donateItem.getServer().getApiName())) {
                    keybindSetting.getBinds().stream().filter(Bind::isVisible).forEach(valueBind -> {
                        ServerBinds.ServerBindEntry serverBindEntry2 = this.entryMap.get(valueBind);
                        if (serverBindEntry2 == null) {
                            serverBindEntry2 = new ServerBinds.ServerBindEntry(valueBind);
                            this.entryMap.put(valueBind, serverBindEntry2);
                        }

                        this.updateDonateEntry(serverBindEntry2, donateItem, serverAssist);
                    });
                }
            });
        }

        ClickAction clickAction = ClickAction.getInstance();
        if (clickAction != null) {
            for (ClickAction.ItemBind itemBind : clickAction.getItemBinds()) {
                itemBind.bind().getBinds().stream().filter(Bind::isVisible).forEach(valueBind2 -> {
                    ServerBinds.ServerBindEntry serverBindEntry3 = this.entryMap.get(valueBind2);
                    if (serverBindEntry3 == null) {
                        serverBindEntry3 = new ServerBinds.ServerBindEntry(valueBind2);
                        this.entryMap.put(valueBind2, serverBindEntry3);
                    }

                    this.updateItemEntry(serverBindEntry3, itemBind.item());
                });
            }
        }

        ItemHelper itemHelper = ItemHelper.getInstance();
        if (itemHelper != null && itemHelper.isEnabled()) {
            for (ItemHelper.ItemBindInfo itemBindInfo : itemHelper.getBindInfos()) {
                itemBindInfo.bind().getBinds().stream().filter(Bind::isVisible).forEach(valueBind3 -> {
                    ServerBinds.ServerBindEntry serverBindEntry4 = this.entryMap.get(valueBind3);
                    if (serverBindEntry4 == null) {
                        serverBindEntry4 = new ServerBinds.ServerBindEntry(valueBind3);
                        this.entryMap.put(valueBind3, serverBindEntry4);
                    }

                    this.updateHelperEntry(serverBindEntry4, itemBindInfo);
                });
            }
        }

        ElytraSwap elytraSwap = ElytraSwap.getInstance();
        if (elytraSwap != null && elytraSwap.isEnabled()) {
            for (Bind bind2 : Stream.concat(elytraSwap.swap.getBinds().stream(), elytraSwap.use.getBinds().stream()).toList()) {
                if (bind2.isVisible()) {
                    ServerBinds.ServerBindEntry serverBindEntry5 = this.entryMap.get(bind2);
                    if (serverBindEntry5 == null) {
                        serverBindEntry5 = new ServerBinds.ServerBindEntry(bind2);
                        this.entryMap.put(bind2, serverBindEntry5);
                    }

                    boolean flag = elytraSwap.use.getBinds().contains(bind2);
                    Slot slot = flag ? InventoryUtil.findSlotReverse(Items.FIREWORK_ROCKET) : ElytraSwap.findElytraSlot();
                    serverBindEntry5.name = flag ? "Firework" : "Elytra Swap";
                    serverBindEntry5.stack = (slot != null ? slot.getStack() : new ItemStack(flag ? Items.FIREWORK_ROCKET : Items.ELYTRA)).copy();
                    serverBindEntry5.count = flag
                        ? InventoryUtil.countItem(Items.FIREWORK_ROCKET)
                        : (slot != null ? slot.getStack().getCount() : InventoryUtil.countItem(Items.ELYTRA));
                    this.updateCooldown(serverBindEntry5, slot);
                    serverBindEntry5.active = true;
                }
            }
        }

        for (ServerBinds.ServerBindEntry serverBindEntry6 : this.entryMap.values()) {
            if (serverBindEntry6.active || serverBindEntry6.animation.get() > 0.001) {
                serverBindEntries.add(serverBindEntry6);
            }
        }

        this.entryMap
            .entrySet()
            .removeIf(entry -> !entry.getValue().active && entry.getValue().animation.get() <= 0.001 && entry.getValue().animation.isFinished());

        for (ServerBinds.ServerBindEntry serverBindEntry7 : serverBindEntries) {
            serverBindEntry7.cooldownAnimation.update();
            serverBindEntry7.cooldownAnimation.animate(serverBindEntry7.onCooldown ? 1.0 : 0.0, 0.35, Easings.QUART_OUT);
        }
    }

    @Override
    public void sortEntries(List<ServerBinds.ServerBindEntry> serverBindEntries) {
        serverBindEntries.sort(Comparator.<ServerBinds.ServerBindEntry>comparingInt(serverBindEntry -> serverBindEntry.onCooldown ? 1 : 0).thenComparingDouble(serverBindEntry2 -> -this.getSortWidth(serverBindEntry2)));
    }

    private float getSortWidth(ServerBinds.ServerBindEntry serverBindEntry) {
        return this.isInverted() ? BODY_FONT.getWidth(serverBindEntry.name, 6.0F) : 8.0F + BODY_FONT.getWidth(serverBindEntry.bind.getDisplayString(), 6.0F);
    }

    protected float getIconWidth(ServerBinds.ServerBindEntry serverBindEntry) {
        return 8.0F;
    }

    protected float getEntryValueWidth(ServerBinds.ServerBindEntry serverBindEntry) {
        float f = serverBindEntry.cooldownAnimation.get();
        float f2 = BODY_FONT.getWidth(serverBindEntry.bind.getDisplayString(), 6.0F);
        float f3 = serverBindEntry.cooldownText.isEmpty() ? 0.0F : BODY_FONT.getWidth(serverBindEntry.cooldownText, 6.0F);
        return f2 * (1.0F - f) + f3 * f;
    }

    protected void drawIcon(DrawContext drawContext, float f, float f2, float f3, float f4, ServerBinds.ServerBindEntry serverBindEntry) {
        if (serverBindEntry.stack != null && !serverBindEntry.stack.isEmpty()) {
            float f5 = MathUtil.roundToStep(f, 0.5F);
            float f6 = MathUtil.roundToStep(f2 + f3 / 2.0F - 4.0F, 0.5F);
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f5, f6, 0.5F);
            GuiRenderUtil.drawCachedItem(drawContext, serverBindEntry, serverBindEntry.stack, f5, f6, f4);
            MatrixUtil.pop(drawContext);
        }

        if (serverBindEntry.count > 0) {
            String string = Integer.toString(serverBindEntry.count);
            float f7 = BODY_FONT.getWidth(string, 5.0F);
            new RenderUtil(drawContext, f, f2, 8.0F, f3)
                .textTopLeft(
                    BODY_FONT,
                    string,
                    8.0F - f7,
                    f3 / 2.0F + 4.0F - 5.0F,
                    this.applyAlpha(ColorUtil.interpolate(Theme.getTextColor(), ColorUtil.BLACK, 0.1F), f4),
                    5.0F,
                    true
                );
        }
    }

    protected void drawValue(DrawContext drawContext, float f, float f2, float f3, float f4, ServerBinds.ServerBindEntry serverBindEntry) {
        float f5 = serverBindEntry.cooldownAnimation.get();
        if (f5 < 0.999F) {
            String string = serverBindEntry.bind.getDisplayString();
            new RenderUtil(drawContext, f, f2, BODY_FONT.getWidth(string, 6.0F), f3)
                .textMiddleLeft(BODY_FONT, string, 0.0F, 0.0F, this.applyAlpha(Theme.getTextColor(), f4 * (1.0F - f5)), 6.0F);
        }

        if (f5 > 0.001F && !serverBindEntry.cooldownText.isEmpty()) {
            int i = ColorUtil.interpolate(ColorUtil.GREEN, ColorUtil.RED, serverBindEntry.cooldownProgress);
            new RenderUtil(drawContext, f, f2, BODY_FONT.getWidth(serverBindEntry.cooldownText, 6.0F), f3)
                .textMiddleLeft(BODY_FONT, serverBindEntry.cooldownText, 0.0F, 0.0F, this.applyAlpha(i, f4 * f5), 6.0F);
        }
    }

    protected String getEntryName(ServerBinds.ServerBindEntry serverBindEntry) {
        return serverBindEntry.name;
    }

    @Override
    protected String getEntryText(ServerBinds.ServerBindEntry serverBindEntry) {
        return this.getEntryName(serverBindEntry);
    }

    @Override
    protected float getEntryWidth(ServerBinds.ServerBindEntry serverBindEntry) {
        return this.getIconWidth(serverBindEntry);
    }

    @Override
    protected void renderEntryIcon(DrawContext drawContext, float f, float f2, float f3, float f4, ServerBinds.ServerBindEntry serverBindEntry) {
        this.drawIcon(drawContext, f, f2, f3, f4, serverBindEntry);
    }

    @Override
    protected void renderEntryContent(DrawContext drawContext, float f, float f2, float f3, float f4, ServerBinds.ServerBindEntry serverBindEntry) {
        this.drawValue(drawContext, f, f2, f3, f4, serverBindEntry);
    }
    private void updateDonateEntry(ServerBinds.ServerBindEntry serverBindEntry, DonateItem donateItem, ServerAssist serverAssist) {
        serverBindEntry.name = donateItem.getDisplayName();
        Slot slot = serverAssist.findItemSlot(donateItem);
        serverBindEntry.stack = slot != null ? slot.getStack().copy() : donateItem.createStack();
        int i = serverAssist.getItemCount(donateItem);
        serverBindEntry.count = i <= 0 && slot != null ? slot.getStack().getCount() : i;
        this.updateCooldown(serverBindEntry, slot);
        serverBindEntry.active = true;
    }

    private void updateItemEntry(ServerBinds.ServerBindEntry serverBindEntry, Item item) {
        serverBindEntry.name = item.getName().getString();
        Slot slot = InventoryUtil.findSlotReverse(item);
        serverBindEntry.stack = (slot != null ? slot.getStack() : new ItemStack(item)).copy();
        serverBindEntry.count = InventoryUtil.countItem(item);
        this.updateCooldown(serverBindEntry, slot);
        serverBindEntry.active = true;
    }

    private void updateHelperEntry(ServerBinds.ServerBindEntry serverBindEntry, ItemHelper.ItemBindInfo itemBindInfo) {
        Slot slot = itemBindInfo.potion() ? InventoryUtil.findPotionSlotByEntry(itemBindInfo.effect()) : InventoryUtil.findSlotReverse(itemBindInfo.item());
        serverBindEntry.stack = (slot != null ? slot.getStack() : new ItemStack(itemBindInfo.item())).copy();
        serverBindEntry.name = serverBindEntry.stack.getName().getString();
        serverBindEntry.count = itemBindInfo.potion() ? (slot != null ? slot.getStack().getCount() : 0) : InventoryUtil.countItem(itemBindInfo.item());
        this.updateCooldown(serverBindEntry, slot);
        serverBindEntry.active = true;
    }

    private void updateCooldown(ServerBinds.ServerBindEntry serverBindEntry, Slot slot) {
        serverBindEntry.onCooldown = false;
        if (slot != null && mc.player != null) {
            ItemStack itemStack = slot.getStack();
            ItemCooldownManager itemCooldownManager = mc.player.getItemCooldownManager();
            if (!itemStack.isEmpty() && itemCooldownManager.isCoolingDown(itemStack)) {
                ItemCooldownManagerAccessor itemCooldownManagerAccessor = (ItemCooldownManagerAccessor)itemCooldownManager;
                Object object = itemCooldownManagerAccessor.sadnes$getEntries().get(itemCooldownManager.getGroup(itemStack));
                if (object != null) {
                    int i = ((ItemCooldownManagerEntryAccessor)object).sadnes$getEndTick();
                    int i2 = ((ItemCooldownManagerEntryAccessor)object).sadnes$getStartTick();
                    int i3 = Math.max(0, i - itemCooldownManagerAccessor.sadnes$getTick());
                    int i4 = Math.max(1, i - i2);
                    serverBindEntry.cooldownProgress = Math.min(1.0F, (float)i3 / i4);
                    int i5 = Math.max(0, Math.round(i3 / 2.0F));
                    StringBuilder builder = new StringBuilder(6);
                    builder.append(i5 / 10).append('.').append(i5 % 10).append('s');
                    serverBindEntry.cooldownText = builder.toString();
                    serverBindEntry.onCooldown = true;
                }
            }
        }
    }

    public static ServerBinds getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(ServerBinds.class);
    }

    public static class ServerBindEntry extends HudEntry {
        public String name = "";
        public final Bind bind;
        public ItemStack stack;
        public int count;
        public boolean onCooldown;
        public float cooldownProgress;
        public String cooldownText = "";
        public final Animation cooldownAnimation = new Animation();

        public ServerBindEntry(Bind bind2) {
            this.bind = bind2;
        }
    }
}
