package tech.sadnes.client.server.autobuy;

import java.util.Locale;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.ServerType;

public class HolyWorldItem extends AutoBuyItem {
    private final String displayName;
    private final String lowerName;
    private final boolean exactMatch;

    public HolyWorldItem(Item item, int i, String string, boolean flag) {
        super(item, i);
        this.displayName = string;
        this.lowerName = string.toLowerCase(Locale.ROOT);
        this.exactMatch = flag;
        this.setServer(ServerType.HOLYWORLD);
    }

    @Override
    public String getName() {
        return "holy:" + this.displayName;
    }

    @Override
    public String getId() {
        return this.displayName;
    }

    @Override
    public int getMaxCount() {
        return this.getItem().getMaxCount();
    }

    @Override
    public String getCategoryName() {
        int i = this.displayName.indexOf(32);
        return i > 0 ? this.displayName.substring(0, i) : this.displayName;
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        if (!super.matches(itemStack)) {
            return false;
        }

        Text text = itemStack.getName();
        if (text == null) {
            return false;
        }

        String string = text.getString().toLowerCase(Locale.ROOT).trim();
        return this.exactMatch ? string.equals(this.lowerName) : string.contains(this.lowerName);
    }
}
