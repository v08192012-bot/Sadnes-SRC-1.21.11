package tech.sadnes.client.feature.impl.misc.autowarden;

import java.util.Set;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BannerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SmithingTemplateItem;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;

public final class WardenItems {
    private static final Set<Item> VALUABLE_ITEMS = Set.of(
        Items.BLAZE_ROD,
        Items.ENCHANTED_BOOK,
        Items.TRIDENT,
        Items.NAME_TAG,
        Items.SCULK,
        Items.SCULK_SENSOR,
        Items.ENDER_CHEST,
        Items.REINFORCED_DEEPSLATE,
        Items.PUFFERFISH,
        Items.HONEY_BOTTLE,
        Items.FERMENTED_SPIDER_EYE,
        Items.ANVIL,
        Items.COOKED_PORKCHOP
    );
    private static final Set<Item> MEDIUM_ITEMS = Set.of(
        Items.CHORUS_FRUIT,
        Items.DISC_FRAGMENT_5,
        Items.NAUTILUS_SHELL,
        Items.BOOKSHELF,
        Items.COOKED_MUTTON,
        Items.SKELETON_SPAWN_EGG,
        Items.CREEPER_SPAWN_EGG,
        Items.ZOMBIE_SPAWN_EGG,
        Items.VINDICATOR_SPAWN_EGG,
        Items.PIGLIN_SPAWN_EGG,
        Items.FIRE_CHARGE,
        Items.LEATHER,
        Items.SHULKER_SHELL,
        Items.EXPERIENCE_BOTTLE,
        Items.WITHER_ROSE,
        Items.EMERALD,
        Items.SUGAR,
        Items.GHAST_TEAR,
        Items.DRAGON_BREATH,
        Items.VEX_SPAWN_EGG,
        Items.ENDERMITE_SPAWN_EGG,
        Items.CAT_SPAWN_EGG,
        Items.ENCHANTING_TABLE,
        Items.DIAMOND_HELMET,
        Items.DIAMOND_CHESTPLATE,
        Items.DIAMOND_LEGGINGS,
        Items.DIAMOND_BOOTS
    );
    private static final Set<Item> MEDIUM_FOODS = Set.of(
        Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT, Items.COD, Items.SALMON, Items.POTATO
    );
    private static final Set<Item> EXCLUDED_FOODS = Set.of(Items.CHORUS_FRUIT, Items.PUFFERFISH, Items.DRIED_KELP);

    private WardenItems() {
    }

    public static boolean isWorthLooting(ItemStack itemStack, String string) {
        return string.equals("low") ? false : isHighValue(itemStack) || string.equals("high") && isMediumValue(itemStack);
    }

    public static boolean isInvisibilityPotion(ItemStack itemStack) {
        RegistryEntry registryEntry = itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT).potion().orElse(null);
        return registryEntry != null && (registryEntry.equals(Potions.INVISIBILITY) || registryEntry.equals(Potions.LONG_INVISIBILITY));
    }

    public static boolean isSpeedPotion(ItemStack itemStack) {
        if (!itemStack.isOf(Items.POTION)) {
            return false;
        }

        for (StatusEffectInstance statusEffectInstance : itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT).getEffects()) {
            if (statusEffectInstance.getEffectType().equals(StatusEffects.SPEED)) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasEnchantedTrim(ItemStack itemStack) {
        EquippableComponent equippableComponent = itemStack.get(DataComponentTypes.EQUIPPABLE);
        return equippableComponent != null && equippableComponent.slot().isArmorSlot();
    }

    public static int getFoodPriority(ItemStack itemStack) {
        if (!itemStack.contains(DataComponentTypes.FOOD) || EXCLUDED_FOODS.contains(itemStack.getItem()) || itemStack.contains(DataComponentTypes.CUSTOM_NAME)) {
            return -1;
        } else if (itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            return 3;
        } else if (itemStack.isOf(Items.GOLDEN_APPLE)) {
            return 2;
        } else {
            return MEDIUM_FOODS.contains(itemStack.getItem()) ? 1 : 0;
        }
    }

    private static boolean isHighValue(ItemStack itemStack) {
        Item item = itemStack.getItem();
        boolean flag;
        if (item instanceof SmithingTemplateItem) {
            flag = !itemStack.isOf(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        } else {
            flag = false;
        }

        return itemStack.isIn(ItemTags.SHOVELS) || itemStack.isIn(ItemTags.AXES) || item instanceof BannerItem || flag || VALUABLE_ITEMS.contains(item);
    }

    private static boolean isMediumValue(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return item instanceof ArrowItem
            || itemStack.isIn(ItemTags.PICKAXES)
            || itemStack.isIn(ItemTags.AXES)
            || MEDIUM_ITEMS.contains(item)
            || itemStack.contains(DataComponentTypes.JUKEBOX_PLAYABLE)
            || hasFtid(itemStack, "potion-popper");
    }

    private static boolean hasFtid(ItemStack itemStack, String string) {
        NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
        return nbtComponent != null && string.equals(nbtComponent.copyNbt().getCompoundOrEmpty("PublicBukkitValues").getString("minecraft:ftid", ""));
    }
}
