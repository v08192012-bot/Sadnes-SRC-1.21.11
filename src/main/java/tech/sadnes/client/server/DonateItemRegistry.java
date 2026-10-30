package tech.sadnes.client.server;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.screen.slot.Slot;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.ItemPredicate;

public final class DonateItemRegistry {
    private static final Map<ServerType, Map<DonateItem, DonateItemRegistry.Matchers>> REGISTRY = new EnumMap<>(ServerType.class);

    private DonateItemRegistry() {
    }

    public static DonateItemRegistry.Matchers getMatchers(ServerType serverType, DonateItem donateItem) {
        Map map = REGISTRY.get(serverType);
        return map == null ? null : (DonateItemRegistry.Matchers)map.get(donateItem);
    }

    public static Slot findSlot(ServerType serverType, DonateItem donateItem) {
        DonateItemRegistry.Matchers matchers = getMatchers(serverType, donateItem);
        if (matchers == null) {
            return null;
        }

        if (serverType.isCurrent()) {
            if (matchers.deep() != null) {
                Slot slot = InventoryUtil.findSlotMatching(matchers.deep());
                if (slot != null) {
                    return slot;
                }
            }

            if (matchers.strict() != null) {
                Slot slot2 = InventoryUtil.findSlotMatching(matchers.strict());
                if (slot2 != null) {
                    return slot2;
                }
            }

            return null;
        } else {
            if (matchers.strict() != null) {
                Slot slot3 = InventoryUtil.findSlotMatching(matchers.strict());
                if (slot3 != null) {
                    return slot3;
                }
            }

            return InventoryUtil.findSlotMatching(matchers.fallback());
        }
    }

    public static int countItems(ServerType serverType, DonateItem donateItem) {
        DonateItemRegistry.Matchers matchers = getMatchers(serverType, donateItem);
        if (matchers == null) {
            return 0;
        }

        if (serverType.isCurrent() && matchers.deep() != null) {
            return InventoryUtil.countMatching(matchers.deep());
        }

        if (matchers.strict() != null) {
            int i = InventoryUtil.countMatching(matchers.strict());
            if (i > 0) {
                return i;
            }
        }

        return matchers.fallback() == null ? 0 : InventoryUtil.countMatching(matchers.fallback());
    }

    public static InventoryUtil.BundleSlot getDeepInfo(ServerType serverType, DonateItem donateItem) {
        DonateItemRegistry.Matchers matchers = getMatchers(serverType, donateItem);
        return matchers != null && matchers.deep() != null ? InventoryUtil.findInBundle(matchers.deep()) : null;
    }

    private static void registerByTags(DonateItem donateItem, String string, String string2) {
        register(ServerType.FUNTIME, donateItem, createMatchers(donateItem, string));
        register(ServerType.SPOOKYTIME, donateItem, createMatchers(donateItem, string2));
    }

    private static void registerPotion(DonateItem donateItem, String string) {
        DonateItemRegistry.Matchers matchers = createPotionMatchers(donateItem, string, donateItem.getPotionType());
        register(ServerType.FUNTIME, donateItem, matchers);
        register(ServerType.SPOOKYTIME, donateItem, matchers);
    }

    private static DonateItemRegistry.Matchers createMatchers(DonateItem donateItem, String string) {
        ItemPredicate itemPredicate = ItemPredicate.nbtContains("don-item", string);
        ItemPredicate itemPredicate2 = ItemPredicate.ofItem(donateItem.getItem())
            .and(
                ItemPredicate.anyOf(
                    itemPredicate,
                    ItemPredicate.nbtContainsIgnoreCase(string),
                    ItemPredicate.nameOrLoreContains(string),
                    ItemPredicate.nameOrLoreContains(donateItem.getDisplayName())
                )
            );
        return new DonateItemRegistry.Matchers(itemPredicate2, itemPredicate, ItemPredicate.ofItem(donateItem.getItem()));
    }

    private static DonateItemRegistry.Matchers createPotionMatchers(DonateItem donateItem, String string, SpecialPotion specialPotion) {
        ItemPredicate itemPredicate = ItemPredicate.nbtContains("don-item", string);
        ItemPredicate itemPredicate2 = ItemPredicate.anyPotion()
            .and(
                ItemPredicate.anyOf(
                    ItemPredicate.ofPotionType(specialPotion),
                    itemPredicate,
                    ItemPredicate.nbtContainsIgnoreCase(string),
                    ItemPredicate.nameOrLoreContains(string),
                    ItemPredicate.nameOrLoreContains(donateItem.getDisplayName())
                )
            );
        return new DonateItemRegistry.Matchers(itemPredicate2, itemPredicate, ItemPredicate.nameContains(donateItem.getDisplayName()));
    }

    private static void registerWithTag(ServerType serverType, DonateItem donateItem, String string) {
        ItemPredicate itemPredicate = ItemPredicate.ofItem(donateItem.getItem())
            .and(
                ItemPredicate.anyOf(
                    ItemPredicate.nbtContainsIgnoreCase(string), ItemPredicate.nameOrLoreContains(string), ItemPredicate.nameOrLoreContains(donateItem.getDisplayName())
                )
            );
        register(serverType, donateItem, new DonateItemRegistry.Matchers(itemPredicate, null, ItemPredicate.ofItem(donateItem.getItem())));
    }

    private static void registerByItem(ServerType serverType, DonateItem donateItem) {
        register(serverType, donateItem, new DonateItemRegistry.Matchers(null, null, ItemPredicate.ofItem(donateItem.getItem())));
    }

    private static void register(ServerType serverType, DonateItem donateItem, DonateItemRegistry.Matchers matchers) {
        REGISTRY.computeIfAbsent(serverType, serverType2 -> new EnumMap<>(DonateItem.class)).put(donateItem, matchers);
    }

    static {
        registerByTags(DonateItem.DESORIENTATION, "desorientation", "effect-item-diz");
        registerByTags(DonateItem.DUST, "sheerdust", "effect-item-dust");
        registerByTags(DonateItem.BALL, "fireball", "effect-item-smerch");
        registerByTags(DonateItem.AURA, "godsaura", "effect-item-god");
        registerByTags(DonateItem.SNOWBALL, "freezeball", "effect-item-snowball");
        registerByTags(DonateItem.PLAST, "stratum", "schematic-item-plast");
        registerByTags(DonateItem.TRAP, "trap", "schematic-item-trap");
        registerPotion(DonateItem.POPPER, "potion-popper");
        registerPotion(DonateItem.HOLY_WATER, "potion-holy-water");
        registerPotion(DonateItem.RAGE, "potion-rage");
        registerPotion(DonateItem.PALADIN, "potion-paladin");
        registerPotion(DonateItem.ASSASSIN, "potion-assassin");
        registerPotion(DonateItem.RADIATION, "potion-radiation");
        registerPotion(DonateItem.SLEEP, "potion-drowsiness");
        registerByItem(ServerType.FUNTIME, DonateItem.BUFF);
        registerByItem(ServerType.SPOOKYTIME, DonateItem.BUFF);
        registerWithTag(ServerType.HOLYWORLD, DonateItem.ALTERNATIVE_TRAP, "ALTERNATIVE_TRAP");
        registerWithTag(ServerType.HOLYWORLD, DonateItem.EXPLOSIVE_TRAP, "EXPLOSIVE_TRAP");
        registerWithTag(ServerType.HOLYWORLD, DonateItem.EXPLOSIVE_STUFF, "ExplosiveStuff");
        registerWithTag(ServerType.HOLYWORLD, DonateItem.STUN_STAR, "STUN_STAR");
        registerWithTag(ServerType.HOLYWORLD, DonateItem.SNOW_LUMP, "SnowBall");
        registerWithTag(ServerType.HOLYWORLD, DonateItem.JAKE_LUMINAIRE, "JakeLuminaire");
        registerWithTag(ServerType.REALLYWORLD, DonateItem.ANTIFLY, "antiFly");
    }

    public record Matchers(ItemPredicate deep, ItemPredicate strict, ItemPredicate fallback) {
    }
}
