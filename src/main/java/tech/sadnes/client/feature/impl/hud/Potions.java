package tech.sadnes.client.feature.impl.hud;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.HudFeature;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.DonateItem;
import tech.sadnes.client.server.SpecialPotion;
import tech.sadnes.client.ui.hud.HudEntry;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

@FeatureInfo(
    name = "Potions",
    description = "feature.impl.hud.potions",
    displayName = "feature.impl.hud.potions",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_STATUS
)
public class Potions extends HudFeature<Potions.PotionEntry> {
    private static final Set<RegistryEntry<StatusEffect>> PRIORITY_EFFECTS = Set.of(
        StatusEffects.STRENGTH,
        StatusEffects.SPEED,
        StatusEffects.FIRE_RESISTANCE,
        StatusEffects.RESISTANCE,
        StatusEffects.ABSORPTION,
        StatusEffects.REGENERATION
    );
    private static final int NEGATIVE_COLOR = ColorUtil.rgb(255, 80, 80);
    private static final float ICON_SIZE = 8.0F;
    private static final List<DonateItem> EFFECT_SERVER_ITEMS = Arrays.stream(DonateItem.values()).filter(donateItem -> donateItem.getPotionType() != null).toList();
    private final Map<Object, Potions.PotionEntry> entries = new IdentityHashMap<>();
    private final Set<RegistryEntry<StatusEffect>> groupedEffects = Collections.newSetFromMap(new IdentityHashMap<>());

    public Potions() {
        super(10.0F, 48.0F);
    }

    @Override
    public String getTitle() {
        return "Potions";
    }

    @Override
    public Icon getIcon() {
        return Icon.PILL;
    }

    @Override
    public void collectEntries(List<Potions.PotionEntry> potionEntries) {
        this.entries.values().forEach(potionEntry -> potionEntry.active = false);
        this.groupedEffects.clear();
        if (mc.player != null) {
            for (DonateItem donateItem : EFFECT_SERVER_ITEMS) {
                this.collectServerItem(donateItem);
            }

            for (StatusEffectInstance statusEffectInstance : mc.player.getStatusEffects()) {
                RegistryEntry registryEntry = statusEffectInstance.getEffectType();
                if (!this.groupedEffects.contains(registryEntry)) {
                    Potions.PotionEntry potionEntry2 = this.entries.get(registryEntry);
                    if (potionEntry2 == null) {
                        this.entries.put(registryEntry, potionEntry2 = new Potions.PotionEntry(registryEntry));
                    }

                    int i = statusEffectInstance.getAmplifier();
                    if (i != potionEntry2.amplifier) {
                        potionEntry2.amplifier = i;
                        potionEntry2.levelText = i > 0 ? " " + (i + 1) : "";
                    }

                    potionEntry2.durationText = formatDuration(statusEffectInstance);
                    potionEntry2.active = true;
                }
            }
        }

        for (Potions.PotionEntry potionEntry3 : this.entries.values()) {
            if (potionEntry3.active || potionEntry3.animation.get() > 0.001) {
                potionEntries.add(potionEntry3);
            }
        }

        this.entries.entrySet().removeIf(entry -> !entry.getValue().active && entry.getValue().animation.get() <= 0.001 && entry.getValue().animation.isFinished());
    }

    private void collectServerItem(DonateItem donateItem) {
        List<SpecialPotion.Effect> effects = donateItem.getPotionType().getEffects();
        StatusEffectInstance statusEffectInstance = null;
        int i = Integer.MAX_VALUE;

        for (SpecialPotion.Effect effect2 : effects) {
            if (!effect2.effect().value().isInstant()) {
                StatusEffectInstance statusEffectInstance2 = mc.player.getStatusEffect(effect2.effect());
                if (statusEffectInstance2 == null || statusEffectInstance2.getAmplifier() != effect2.amplifier()) {
                    return;
                }

                int i2 = statusEffectInstance2.isInfinite() ? Integer.MAX_VALUE : statusEffectInstance2.getDuration();
                if (statusEffectInstance == null || i2 < i) {
                    statusEffectInstance = statusEffectInstance2;
                    i = i2;
                }
            }
        }

        if (statusEffectInstance != null) {
            for (SpecialPotion.Effect effect3 : effects) {
                this.groupedEffects.add(effect3.effect());
            }

            Potions.PotionEntry potionEntry = this.entries.get(donateItem);
            if (potionEntry == null) {
                this.entries.put(donateItem, potionEntry = new Potions.PotionEntry(donateItem));
            }

            potionEntry.durationText = formatDuration(statusEffectInstance);
            potionEntry.active = true;
        }
    }

    @Override
    public void sortEntries(List<Potions.PotionEntry> potionEntries) {
        potionEntries.sort(
            Comparator.<Potions.PotionEntry, Integer>comparing(potionEntry -> potionEntry.priority)
                .thenComparing(Comparator.<Potions.PotionEntry>comparingDouble(potionEntry2 -> this.getNameWidth(potionEntry2)).reversed())
        );
    }

    private float getNameWidth(Potions.PotionEntry potionEntry) {
        return BODY_FONT.getWidth(potionEntry.name, 6.0F) + BODY_FONT.getWidth(potionEntry.levelText, 6.0F);
    }

    protected float getIconSize(Potions.PotionEntry potionEntry) {
        return 8.0F;
    }

    protected float getEntryValueWidth(Potions.PotionEntry potionEntry) {
        return BODY_FONT.getWidth(potionEntry.durationText, 6.0F);
    }

    protected void renderIcon(DrawContext drawContext, float f, float f2, float f3, float f4, Potions.PotionEntry potionEntry) {
        if (potionEntry.item != null) {
            float f5 = MathUtil.roundToStep(f, 0.5F);
            float f6 = MathUtil.roundToStep(f2 + f3 / 2.0F - 4.0F, 0.5F);
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f5, f6, 0.5F);
            GuiRenderUtil.drawCachedItem(drawContext, potionEntry, potionEntry.item, f5, f6, f4);
            MatrixUtil.pop(drawContext);
        } else {
            Identifier identifier = InGameHud.getEffectTexture(potionEntry.effect);
            SpriteAtlasTexture spriteAtlasTexture = mc.getAtlasManager().getAtlasTexture(Atlases.GUI);
            Sprite sprite = spriteAtlasTexture.getSprite(identifier);
            GuiRenderUtil.drawSprite(drawContext, sprite, f, f2 + f3 / 2.0F - 4.0F, 8.0F, 8.0F, f4);
        }
    }

    protected void renderValue(DrawContext drawContext, float f, float f2, float f3, float f4, Potions.PotionEntry potionEntry) {
        float f5 = BODY_FONT.getWidth(potionEntry.durationText, 6.0F);
        new RenderUtil(drawContext, f, f2, f5, f3).textMiddleLeft(BODY_FONT, potionEntry.durationText, 0.0F, 0.0F, this.applyAlpha(Theme.getTextColor(), f4), 6.0F);
    }

    protected String getName(Potions.PotionEntry potionEntry) {
        return potionEntry.name;
    }

    protected String getSuffix(Potions.PotionEntry potionEntry) {
        return potionEntry.levelText;
    }

    protected int getEntryColor(Potions.PotionEntry potionEntry, float f) {
        return potionEntry.priority == 3 ? this.applyAlpha(NEGATIVE_COLOR, f) : this.applyAlpha(Theme.getTextColor(), f);
    }

    @Override
    protected String getEntryText(Potions.PotionEntry potionEntry) {
        return this.getName(potionEntry);
    }

    @Override
    protected String getEntrySuffix(Potions.PotionEntry potionEntry) {
        return this.getSuffix(potionEntry);
    }

    @Override
    protected float getEntryWidth(Potions.PotionEntry potionEntry) {
        return this.getIconSize(potionEntry);
    }

    @Override
    protected void renderEntryIcon(DrawContext drawContext, float f, float f2, float f3, float f4, Potions.PotionEntry potionEntry) {
        this.renderIcon(drawContext, f, f2, f3, f4, potionEntry);
    }

    @Override
    protected void renderEntryContent(DrawContext drawContext, float f, float f2, float f3, float f4, Potions.PotionEntry potionEntry) {
        this.renderValue(drawContext, f, f2, f3, f4, potionEntry);
    }
    private static String formatDuration(StatusEffectInstance statusEffectInstance) {
        if (statusEffectInstance.isInfinite()) {
            return "INF";
        }

        int i = statusEffectInstance.getDuration() / 20;
        int i2 = i / 60;
        int i3 = i % 60;
        StringBuilder builder = new StringBuilder(5);
        if (i2 < 10) {
            builder.append('0');
        }

        builder.append(i2).append(':');
        if (i3 < 10) {
            builder.append('0');
        }

        builder.append(i3);
        return builder.toString();
    }

    static int getEffectPriority(RegistryEntry<StatusEffect> registryEntry) {
        if (PRIORITY_EFFECTS.contains(registryEntry)) {
            return 0;
        } else {
            StatusEffectCategory statusEffectCategory = ((StatusEffect)registryEntry.value()).getCategory();
            if (statusEffectCategory == StatusEffectCategory.BENEFICIAL) {
                return 1;
            } else {
                return statusEffectCategory == StatusEffectCategory.NEUTRAL ? 2 : 3;
            }
        }
    }

    public static Potions getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(Potions.class);
    }

    public static class PotionEntry extends HudEntry {
        public String name = "";
        public String levelText = "";
        public String durationText = "";
        public final RegistryEntry<StatusEffect> effect;
        public final ItemStack item;
        public int priority;
        public int amplifier = -1;

        public PotionEntry(RegistryEntry<StatusEffect> registryEntry) {
            this.effect = registryEntry;
            this.item = null;
            this.name = ((StatusEffect)registryEntry.value()).getName().getString();
            this.priority = Potions.getEffectPriority(registryEntry);
        }

        public PotionEntry(DonateItem donateItem) {
            this.effect = null;
            this.item = donateItem.createStack();
            this.name = Language.translate("serveritem." + donateItem.name().toLowerCase(Locale.ROOT) + ".name", donateItem.getDisplayName());
            this.priority = donateItem.isPositivePotion() ? 0 : 3;
        }
    }
}
