package tech.sadnes.client.server.donut;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class DonutPriceUtil {
    private static final Pattern PRICE_PATTERN = Pattern.compile("\\$\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([KMBT](?![A-Za-z]))?", 2);

    private DonutPriceUtil() {
    }

    public static long parsePrice(String string) {
        Matcher matcher2 = PRICE_PATTERN.matcher(string);
        if (!matcher2.find()) {
            return -1L;
        }

        double d = Double.parseDouble(matcher2.group(1).replace(",", ""));
        String string2 = matcher2.group(2) == null ? "" : matcher2.group(2).toUpperCase(Locale.ROOT);
        byte b = -1;
        switch (string2.hashCode()) {
            case 66:
                if (string2.equals("B")) {
                    return Math.round(d * 1000000000L);
                }
                break;
            case 75:
                if (string2.equals("K")) {
                    return Math.round(d * 1000L);
                }
                break;
            case 77:
                if (string2.equals("M")) {
                    return Math.round(d * 1000000L);
                }
                break;
            case 84:
                if (string2.equals("T")) {
                    b = 3;
                }
        }

        switch (b) {
            case 0:
                return Math.round(d * 1000L);
            case 1:
                return Math.round(d * 1000000L);
            case 2:
                return Math.round(d * 1000000000L);
            case 3:
                return Math.round(d * 1000000000000L);
            default:
                return Math.round(d * 1L);
        }
    }

    public static long getItemPrice(ItemStack itemStack) {
        long l = parsePrice(itemStack.getName().getString());
        LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
        if (l < 0L && loreComponent != null) {
            for (Text text : loreComponent.lines()) {
                long l2 = parsePrice(text.getString());
                if (l2 >= 0L) {
                    return l2;
                }
            }

            return -1L;
        } else {
            return l;
        }
    }

    public static long getPriceStep(long l) {
        long l2 = getUnit(l);
        if (l2 == 1L) {
            return 0L;
        } else {
            return l >= l2 * 100L ? l2 / 2L : l2 / 20L;
        }
    }

    public static String formatPrice(long l) {
        long l2 = getUnit(Math.abs(l));
        double d = (double)Math.abs(l) / l2;
        String string = String.format(Locale.ROOT, !(d >= 100.0) && l2 != 1L ? "%.1f" : "%.0f", d);
        if (string.endsWith(".0")) {
            string = string.substring(0, string.length() - 2);
        }

        String string2 = l2 == 1000000000L ? "B" : (l2 == 1000000L ? "M" : (l2 == 1000L ? "K" : ""));
        return (l < 0L ? "-$" : "$") + string + string2;
    }

    private static long getUnit(long l) {
        if (l >= 1000000000L) {
            return 1000000000L;
        } else if (l >= 1000000L) {
            return 1000000L;
        } else {
            return l >= 1000L ? 1000L : 1L;
        }
    }
}
