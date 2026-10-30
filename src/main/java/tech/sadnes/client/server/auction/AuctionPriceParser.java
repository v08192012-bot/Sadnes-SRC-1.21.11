package tech.sadnes.client.server.auction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.donut.DonutPriceUtil;

public class AuctionPriceParser {
    private static final Pattern FUNTIME_PRICE = Pattern.compile("Ценa.*?([0-9,]+)", 34);
    private static final Pattern SPOOKYTIME_PRICE = Pattern.compile("\\$\\s*Цена:\\s*\\$([0-9,]+)");
    private static final Pattern HOLYWORLD_PRICE = Pattern.compile("Цена:\\s*([\\d ]+)¤");

    public int getPrice(ItemStack itemStack) {
        if (ServerManager.isDonut()) {
            return (int)Math.min(DonutPriceUtil.getItemPrice(itemStack), 2147483647L);
        }

        Pattern pattern = ServerManager.isFunTime()
            ? FUNTIME_PRICE
            : (ServerManager.isSpookyTime() ? SPOOKYTIME_PRICE : (ServerManager.isHolyWorld() ? HOLYWORLD_PRICE : null));
        if (pattern == null) {
            return -1;
        }

        LoreComponent loreComponent = itemStack.getComponents().get(DataComponentTypes.LORE);
        if (loreComponent == null) {
            return -1;
        }

        for (Text text : loreComponent.lines()) {
            String string = text.getString();
            if (!ServerManager.isHolyWorld() || !string.contains("за 1 ед.")) {
                Matcher matcher2 = pattern.matcher(string);
                if (matcher2.find()) {
                    try {
                        return Integer.parseInt(matcher2.group(1).replace(",", "").replace(" ", ""));
                    } catch (Exception exception) {
                    }
                }
            }
        }

        return -1;
    }
}
