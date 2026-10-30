package tech.sadnes.client.server.auction;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

public class AuctionLoreUtil {
    private static final String LATIN_LOOKALIKES = "aecopxyk";
    private static final String CYRILLIC_LOOKALIKES = "аесорхук";
    private static final Pattern DECORATION_PATTERN = Pattern.compile("(?i)^ *(?:xxx|qq) *| *(?:xxx|qq) *$");
    private static final Pattern FUNTIME_SELLER = Pattern.compile("Продавец:\\s*(\\S+)");
    private static final Pattern FUNTIME_EXPIRY = Pattern.compile("Истекает через:\\s*(\\d+)ч\\.\\s*(\\d+)мин\\.\\s*(\\d+)сек\\.");
    private static final Pattern HOLYWORLD_PRICE = Pattern.compile("Цена:\\s*([\\d ]+)¤");
    private static final Pattern HOLYWORLD_UNIT_PRICE = Pattern.compile("Цена за 1 ед\\.:\\s*([\\d ]+)¤");
    private static final Pattern HOLYWORLD_SELLER = Pattern.compile("Продавец:\\s*(\\S+)");
    private static final Pattern HOLYWORLD_EXPIRY = Pattern.compile("Истекает:\\s*(\\d+)\\s*ч\\.\\s*(\\d+)\\s*мин\\.\\s*(\\d+)\\s*сек\\.");
    private static final Pattern SPOOKYTIME_PRICE = Pattern.compile("\\$\\s*Цена:\\s*\\$([0-9,]+)");

    private static String findLoreMatch(ItemStack itemStack, Pattern pattern) {
        LoreComponent loreComponent = itemStack.getComponents().get(DataComponentTypes.LORE);
        if (loreComponent == null) {
            return null;
        }

        for (Text text : loreComponent.lines()) {
            Matcher matcher2 = pattern.matcher(text.getString());
            if (matcher2.find()) {
                return matcher2.group(0);
            }
        }

        return null;
    }

    private static Matcher matchLore(ItemStack itemStack, Pattern pattern) {
        LoreComponent loreComponent = itemStack.getComponents().get(DataComponentTypes.LORE);
        if (loreComponent == null) {
            return null;
        }

        for (Text text : loreComponent.lines()) {
            Matcher matcher2 = pattern.matcher(text.getString());
            if (matcher2.find()) {
                return matcher2;
            }
        }

        return null;
    }

    public static boolean isAuctionInfoLine(String string) {
        String string2 = normalizeLookalikes(string);
        return string2.contains("цен") || string2.contains("продавец") || string2.contains("истекает") || string2.contains("осталось");
    }

    public static String normalizeLookalikes(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(string2.length());
        int i = string2.length();

        for (int i2 = 0; i2 < i; i2++) {
            char c = string2.charAt(i2);
            int i3 = "aecopxyk".indexOf(c);
            builder.append(i3 < 0 ? c : "аесорхук".charAt(i3));
        }

        return builder.toString();
    }

    public static String buildItemSignature(ItemStack itemStack) {
        StringBuilder builder = new StringBuilder(cleanName(itemStack.getName().getString())).append('|');
        TreeMap<String, Integer> treeMap = new TreeMap<>();
        collectEnchantments(itemStack.get(DataComponentTypes.ENCHANTMENTS), treeMap);
        collectEnchantments(itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS), treeMap);

        for (Entry<String, Integer> entry : treeMap.entrySet()) {
            builder.append((String)entry.getKey()).append(':').append(entry.getValue()).append('|');
        }

        LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
        if (loreComponent == null) {
            return builder.toString();
        }

        for (Text text : loreComponent.lines()) {
            String string = normalizeLookalikes(text.getString()).strip();
            if (!string.isBlank() && !isAuctionInfoLine(string) && !isHintLine(string)) {
                builder.append(string).append('|');
            }
        }

        return builder.toString();
    }

    private static void collectEnchantments(ItemEnchantmentsComponent itemEnchantmentsComponent, Map<String, Integer> map2) {
        if (itemEnchantmentsComponent != null && !itemEnchantmentsComponent.isEmpty()) {
            for (RegistryEntry<net.minecraft.enchantment.Enchantment> registryEntry : itemEnchantmentsComponent.getEnchantments()) {
                String string = registryEntry.getKey().map(registryKey -> registryKey.getValue().toString()).orElse(registryEntry.getIdAsString());
                map2.put(string, itemEnchantmentsComponent.getLevel(registryEntry));
            }
        }
    }

    private static boolean isHintLine(String string) {
        return string.contains("нажмите") || string.contains("лкм") || string.contains("пкм");
    }

    public static String cleanName(String string) {
        return stripSymbols(DECORATION_PATTERN.matcher(string).replaceAll(""));
    }

    public static String stripSymbols(String string) {
        return string.replaceAll("[^\\p{L}\\p{N} \\-]", "").trim().replaceAll("\\s{2,}", " ");
    }

    public static String formatNumber(long l) {
        return String.format(Locale.US, "%,d", l);
    }

    public static String getFunTimeSeller(ItemStack itemStack) {
        Matcher matcher = matchLore(itemStack, FUNTIME_SELLER);
        return matcher != null ? matcher.group(1) : null;
    }

    public static int getFunTimeExpirySeconds(ItemStack itemStack) {
        Matcher matcher = matchLore(itemStack, FUNTIME_EXPIRY);
        if (matcher == null) {
            return -1;
        }

        int i = Integer.parseInt(matcher.group(1));
        int i2 = Integer.parseInt(matcher.group(2));
        int i3 = Integer.parseInt(matcher.group(3));
        return i * 3600 + i2 * 60 + i3;
    }

    public static int getHolyWorldPrice(ItemStack itemStack) {
        LoreComponent loreComponent = itemStack.getComponents().get(DataComponentTypes.LORE);
        if (loreComponent == null) {
            return -1;
        }

        for (Text text : loreComponent.lines()) {
            String string = text.getString();
            if (!string.contains("за 1 ед.")) {
                Matcher matcher2 = HOLYWORLD_PRICE.matcher(string);
                if (matcher2.find()) {
                    try {
                        return Integer.parseInt(matcher2.group(1).replace(" ", ""));
                    } catch (Exception exception) {
                    }
                }
            }
        }

        return -1;
    }

    public static int getHolyWorldUnitPrice(ItemStack itemStack) {
        Matcher matcher = matchLore(itemStack, HOLYWORLD_UNIT_PRICE);
        if (matcher == null) {
            return -1;
        }

        try {
            return Integer.parseInt(matcher.group(1).replace(" ", ""));
        } catch (Exception exception) {
            return -1;
        }
    }

    public static String getHolyWorldSeller(ItemStack itemStack) {
        Matcher matcher = matchLore(itemStack, HOLYWORLD_SELLER);
        return matcher != null ? matcher.group(1) : null;
    }

    public static int getHolyWorldExpirySeconds(ItemStack itemStack) {
        Matcher matcher = matchLore(itemStack, HOLYWORLD_EXPIRY);
        if (matcher == null) {
            return -1;
        }

        int i = Integer.parseInt(matcher.group(1));
        int i2 = Integer.parseInt(matcher.group(2));
        int i3 = Integer.parseInt(matcher.group(3));
        return i * 3600 + i2 * 60 + i3;
    }

    public static int getSpookyTimePrice(ItemStack itemStack) {
        Matcher matcher = matchLore(itemStack, SPOOKYTIME_PRICE);
        if (matcher == null) {
            return -1;
        }

        try {
            return Integer.parseInt(matcher.group(1).replace(",", ""));
        } catch (Exception exception) {
            return -1;
        }
    }
}
