package tech.sadnes.client.feature.impl.hud;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import tech.sadnes.api.mixin.accessor.ItemCooldownManagerAccessor;
import tech.sadnes.api.mixin.accessor.ItemCooldownManagerEntryAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.HudFeature;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.hud.HudEntry;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

@FeatureInfo(
    name = "Cooldowns",
    description = "feature.impl.hud.cooldowns",
    displayName = "feature.impl.hud.cooldowns",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_STATUS
)
public class Cooldowns extends HudFeature<Cooldowns.Entry> {
    private static final float ICON_SIZE = 8.0F;
    private final Map<Identifier, Cooldowns.Entry> entries = new HashMap<>();
    private final Set<Identifier> seenIds = new HashSet<>();

    public Cooldowns() {
        super(10.0F, 90.0F);
    }

    @Override
    public String getTitle() {
        return "Cooldowns";
    }

    @Override
    public Icon getIcon() {
        return Icon.CLOCK_HOUR_4;
    }

    @Override
    public void collectEntries(List<Cooldowns.Entry> entries2) {
        this.entries.values().forEach(entry -> entry.active = false);
        if (mc.player != null) {
            ItemCooldownManager itemCooldownManager = mc.player.getItemCooldownManager();
            ItemCooldownManagerAccessor itemCooldownManagerAccessor = (ItemCooldownManagerAccessor)itemCooldownManager;
            Map map = itemCooldownManagerAccessor.sadnes$getEntries();
            int i = itemCooldownManagerAccessor.sadnes$getTick();
            this.seenIds.clear();
            PlayerInventory playerInventory = mc.player.getInventory();
            int i2 = playerInventory.size();

            for (int i3 = 0; i3 < i2; i3++) {
                ItemStack itemStack = playerInventory.getStack(i3);
                if (!itemStack.isEmpty() && itemCooldownManager.isCoolingDown(itemStack)) {
                    Identifier identifier = itemCooldownManager.getGroup(itemStack);
                    if (this.seenIds.add(identifier)) {
                        this.updateEntry(this.entries.computeIfAbsent(identifier, Cooldowns.Entry::new), itemStack, map.get(identifier), i);
                    }
                }
            }

            ItemStack itemStack2 = mc.player.getOffHandStack();
            if (!itemStack2.isEmpty() && itemCooldownManager.isCoolingDown(itemStack2)) {
                Identifier identifier2 = itemCooldownManager.getGroup(itemStack2);
                if (this.seenIds.add(identifier2)) {
                    this.updateEntry(this.entries.computeIfAbsent(identifier2, Cooldowns.Entry::new), itemStack2, map.get(identifier2), i);
                }
            }
        }

        for (Cooldowns.Entry entry2 : this.entries.values()) {
            if (entry2.active || entry2.animation.get() > 0.001) {
                entries2.add(entry2);
            }
        }

        this.entries.entrySet().removeIf(entry3 -> !entry3.getValue().active && entry3.getValue().animation.get() <= 0.001 && entry3.getValue().animation.isFinished());
    }

    @Override
    public void sortEntries(List<Cooldowns.Entry> entries2) {
        entries2.sort(Comparator.<Cooldowns.Entry>comparingDouble(entry -> BODY_FONT.getWidth(entry.name, 6.0F)).reversed());
    }

    @Override
    protected float getEntryWidth(Cooldowns.Entry entry) {
        return 8.0F;
    }

    @Override
    protected float getEntryValueWidth(Cooldowns.Entry entry) {
        return BODY_FONT.getWidth(entry.timeText, 6.0F);
    }

    @Override
    protected void renderEntryIcon(DrawContext drawContext, float f, float f2, float f3, float f4, Cooldowns.Entry entry) {
        if (entry.stack != null && !entry.stack.isEmpty()) {
            float f5 = MathUtil.roundToStep(f, 0.5F);
            float f6 = MathUtil.roundToStep(f2 + f3 / 2.0F - 4.0F, 0.5F);
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f5, f6, 0.5F);
            GuiRenderUtil.drawCachedItem(drawContext, entry, entry.stack, f5, f6, f4);
            MatrixUtil.pop(drawContext);
        }
    }

    @Override
    protected void renderEntryContent(DrawContext drawContext, float f, float f2, float f3, float f4, Cooldowns.Entry entry) {
        float f5 = BODY_FONT.getWidth(entry.timeText, 6.0F);
        new RenderUtil(drawContext, f, f2, f5, f3).textMiddleLeft(BODY_FONT, entry.timeText, 0.0F, 0.0F, this.applyAlpha(Theme.getTextColor(), f4), 6.0F);
    }

    @Override
    protected String getEntryText(Cooldowns.Entry entry) {
        return entry.name;
    }

    @Override
    protected Text getEntryLabel(Cooldowns.Entry entry) {
        return entry.nameText;
    }

    private void updateEntry(Cooldowns.Entry entry, ItemStack itemStack, Object object, int i) {
        if (object != null) {
            int i2 = Math.max(0, ((ItemCooldownManagerEntryAccessor)object).sadnes$getEndTick() - i);
            int i3 = Math.max(0, Math.round(i2 / 2.0F));
            if (i3 != entry.lastTenths) {
                entry.lastTenths = i3;
                StringBuilder builder = new StringBuilder(6);
                builder.append(i3 / 10).append('.').append(i3 % 10).append('s');
                entry.timeText = builder.toString();
            }

            if (entry.stack == null || entry.stack.getItem() != itemStack.getItem()) {
                entry.stack = itemStack.copy();
                entry.nameText = itemStack.getName();
                entry.name = entry.nameText.getString();
            }

            entry.active = true;
        }
    }

    public static Cooldowns getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(Cooldowns.class);
    }

    public static class Entry extends HudEntry {
        public String name = "";
        public Text nameText;
        public String timeText = "";
        public int lastTenths = -1;
        public final Identifier id;
        public ItemStack stack;

        public Entry(Identifier identifier) {
            this.id = identifier;
        }
    }
}
