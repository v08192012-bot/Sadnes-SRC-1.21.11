package tech.sadnes.client.util.player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import tech.sadnes.client.server.SpecialPotion;

@FunctionalInterface
public interface ItemPredicate extends Predicate<ItemStack> {
    Set<String> MODIFIER_TAGS = Set.of("modificator", "modifier-item");
    Map<NbtComponent, String> NBT_STRING_CACHE = Collections.synchronizedMap(new LinkedHashMap<NbtComponent, String>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Entry<NbtComponent, String> entry) {
            return this.size() > 256;
        }
    });

    default ItemPredicate and(ItemPredicate itemPredicate) {
        return itemStack -> this.test(itemStack) && itemPredicate.test(itemStack);
    }

    static ItemPredicate anyOf(ItemPredicate... itemPredicates) {
        return itemStack -> {
            for (ItemPredicate itemPredicate : itemPredicates) {
                if (itemPredicate.test(itemStack)) {
                    return true;
                }
            }

            return false;
        };
    }

    static ItemPredicate ofItem(Item item2) {
        return itemStack -> itemStack.isOf(item2);
    }

    static ItemPredicate anyPotion() {
        return itemStack -> itemStack.isOf(Items.POTION) || itemStack.isOf(Items.SPLASH_POTION) || itemStack.isOf(Items.LINGERING_POTION);
    }

    static ItemPredicate ofPotionType(SpecialPotion specialPotion) {
        return itemStack -> SpecialPotion.fromStack(itemStack) == specialPotion;
    }

    static String getNbtString(NbtComponent nbtComponent) {
        String string = NBT_STRING_CACHE.get(nbtComponent);
        if (string == null) {
            string = nbtComponent.copyNbt().toString();
            NBT_STRING_CACHE.put(nbtComponent, string);
        }

        return string;
    }

    static boolean containsIgnoreCase(String string, String string2) {
        int i = string.length() - string2.length();

        for (int i2 = 0; i2 <= i; i2++) {
            if (string.regionMatches(true, i2, string2, 0, string2.length())) {
                return true;
            }
        }

        return false;
    }

    static ItemPredicate nbtContains(String string, String string2) {
        return nbtContains(string, string2, true);
    }

    static ItemPredicate nbtContains(String string, String string2, boolean flag) {
        return itemStack -> {
            NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            if (nbtComponent == null) {
                return false;
            }

            String string3 = getNbtString(nbtComponent);
            if (!string3.contains(string)) {
                return false;
            }

            if (flag) {
                for (String string4 : MODIFIER_TAGS) {
                    if (string3.contains(string4) && !string2.contains(string4)) {
                        return false;
                    }
                }
            }

            return string3.contains(string2);
        };
    }

    static ItemPredicate nbtContainsIgnoreCase(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        return itemStack -> {
            NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            return nbtComponent != null && containsIgnoreCase(getNbtString(nbtComponent), string2);
        };
    }

    static ItemPredicate nameContains(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        return itemStack -> {
            Text text = itemStack.getName();
            return text != null && text.getString().toLowerCase(Locale.ROOT).contains(string2);
        };
    }

    static ItemPredicate loreContains(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        return itemStack -> {
            LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
            if (loreComponent == null) {
                return false;
            }

            for (Text text : loreComponent.lines()) {
                if (text.getString().toLowerCase(Locale.ROOT).contains(string2)) {
                    return true;
                }
            }

            return false;
        };
    }

    static ItemPredicate nameOrLoreContains(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        return itemStack -> {
            Text text = itemStack.getName();
            if (text != null && text.getString().toLowerCase(Locale.ROOT).contains(string2)) {
                return true;
            }

            LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
            if (loreComponent == null) {
                return false;
            }

            for (Text text2 : loreComponent.lines()) {
                if (text2.getString().toLowerCase(Locale.ROOT).contains(string2)) {
                    return true;
                }
            }

            return false;
        };
    }
}
