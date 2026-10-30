package tech.sadnes.client.feature.impl.hud;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.HudFeature;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.ui.hud.HudEntry;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

@FeatureInfo(
    name = "Armor",
    description = "feature.impl.hud.armor",
    displayName = "feature.impl.hud.armor",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_INVENTORY
)
public class Armor extends HudFeature<Armor.ArmorEntry> {
    private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };
    private static final float ICON_SIZE = 8.0F;
    private final ModeSetting orientation = new ModeSetting("armor.orientation", new String[]{"vertical", "horizontal"});
    private final BooleanSetting percent = new BooleanSetting("armor.percent", false);
    private final BooleanSetting chipsOnly = new BooleanSetting("armor.chipsOnly", false).setVisibleWhen(() -> this.orientation.is("vertical"));
    private final BooleanSetting hands = new BooleanSetting("armor.hands", true);
    private final Map<EquipmentSlot, Armor.ArmorEntry> entryMap = new EnumMap<>(EquipmentSlot.class);

    public Armor() {
        super(10.0F, 130.0F);
    }

    @Override
    public boolean isCompact() {
        return this.orientation.is("horizontal");
    }

    @Override
    public float getEntryHeight() {
        return Float.MAX_VALUE;
    }

    @Override
    public String getTitle() {
        return "Armor";
    }

    @Override
    public Icon getIcon() {
        return Icon.SHIELD;
    }

    @Override
    public void collectEntries(List<Armor.ArmorEntry> armorEntries) {
        this.entryMap.values().forEach(armorEntry -> armorEntry.active = false);
        if (mc.player != null) {
            for (int i = 0; i < SLOTS.length; i++) {
                int i2 = i;
                EquipmentSlot equipmentSlot = SLOTS[i];
                if (equipmentSlot.getType() != Type.HAND || this.hands.getValue()) {
                    ItemStack itemStack = mc.player.getEquippedStack(equipmentSlot);
                    if (!itemStack.isEmpty()) {
                        this.updateEntry(this.entryMap.computeIfAbsent(equipmentSlot, equipmentSlot2 -> new Armor.ArmorEntry(i2)), itemStack);
                    }
                }
            }
        }

        for (Armor.ArmorEntry armorEntry2 : this.entryMap.values()) {
            if (armorEntry2.active || armorEntry2.animation.get() > 0.001) {
                armorEntries.add(armorEntry2);
            }
        }

        this.entryMap
            .entrySet()
            .removeIf(entry -> !entry.getValue().active && entry.getValue().animation.get() <= 0.001 && entry.getValue().animation.isFinished());
    }

    @Override
    public void sortEntries(List<Armor.ArmorEntry> armorEntries) {
        armorEntries.sort(Comparator.comparingInt(armorEntry -> armorEntry.order));
    }

    @Override
    protected float getEntryWidth(Armor.ArmorEntry armorEntry) {
        return 8.0F;
    }

    @Override
    protected float getEntryValueWidth(Armor.ArmorEntry armorEntry) {
        return BODY_FONT.getWidth(armorEntry.durability, 6.0F);
    }

    @Override
    protected void renderEntryIcon(DrawContext drawContext, float f, float f2, float f3, float f4, Armor.ArmorEntry armorEntry) {
        if (!armorEntry.stack.isEmpty()) {
            float f5 = MathUtil.roundToStep(f, 0.5F);
            float f6 = MathUtil.roundToStep(f2 + f3 / 2.0F - 4.0F, 0.5F);
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f5, f6, 0.5F);
            GuiRenderUtil.drawCachedItem(drawContext, armorEntry, armorEntry.stack, f5, f6, f4);
            MatrixUtil.pop(drawContext);
        }
    }

    @Override
    protected void renderEntryContent(DrawContext drawContext, float f, float f2, float f3, float f4, Armor.ArmorEntry armorEntry) {
        float f5 = BODY_FONT.getWidth(armorEntry.durability, 6.0F);
        new RenderUtil(drawContext, f, f2, f5, f3).textMiddleLeft(BODY_FONT, armorEntry.durability, 0.0F, 0.0F, this.applyAlpha(Theme.getTextColor(), f4), 6.0F);
    }

    @Override
    protected String getEntryText(Armor.ArmorEntry armorEntry) {
        return armorEntry.name;
    }

    @Override
    protected Text getEntryLabel(Armor.ArmorEntry armorEntry) {
        return armorEntry.nameText;
    }

    @Override
    public boolean isVisible() {
        return !this.chipsOnly.getValue();
    }

    private void updateEntry(Armor.ArmorEntry armorEntry, ItemStack itemStack) {
        if (armorEntry.stack != itemStack) {
            armorEntry.stack = itemStack;
            armorEntry.nameText = stripItalic(itemStack.getName());
            armorEntry.name = armorEntry.nameText.getString();
        }

        armorEntry.durability = this.getDurabilityText(itemStack);
        armorEntry.active = true;
    }

    private static Text stripItalic(Text text) {
        MutableText mutableText = text.copyContentOnly().setStyle(text.getStyle().withObfuscated(false));

        for (Text text2 : text.getSiblings()) {
            mutableText.append(stripItalic(text2));
        }

        return mutableText;
    }

    private String getDurabilityText(ItemStack itemStack) {
        if (!itemStack.isDamageable()) {
            return "";
        }

        int i = itemStack.getMaxDamage();
        int i2 = i - itemStack.getDamage();
        return this.percent.getValue() ? Math.round(i2 * 100.0F / i) + "%" : Integer.toString(i2);
    }

    public static Armor getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(Armor.class);
    }

    public static class ArmorEntry extends HudEntry {
        public final int order;
        public ItemStack stack = ItemStack.EMPTY;
        public Text nameText;
        public String name = "";
        public String durability = "";

        public ArmorEntry(int i) {
            this.order = i;
        }
    }
}
