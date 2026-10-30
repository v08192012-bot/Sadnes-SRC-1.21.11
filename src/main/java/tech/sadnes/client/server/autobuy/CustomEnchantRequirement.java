package tech.sadnes.client.server.autobuy;

import java.util.Locale;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class CustomEnchantRequirement extends EnchantRequirement {
    private final CustomEnchant enchant;
    private final String lowerName;

    public CustomEnchantRequirement(CustomEnchant customEnchant) {
        super(customEnchant.getMaxLevel());
        this.enchant = customEnchant;
        this.lowerName = customEnchant.getDisplayName().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getKey() {
        return "server:" + this.enchant.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getDisplayName() {
        return this.enchant.getDisplayName();
    }

    @Override
    protected int getLevel(ItemStack itemStack) {
        LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
        if (loreComponent == null) {
            return 0;
        }

        for (Text text : loreComponent.lines()) {
            int i = this.parseLevel(text.getString());
            if (i > 0) {
                return i;
            }
        }

        return 0;
    }

    private int parseLevel(String string) {
        String string2 = string.strip();
        if (!string2.toLowerCase(Locale.ROOT).startsWith(this.lowerName)) {
            return 0;
        }

        String string3 = string2.substring(this.lowerName.length()).strip();
        return string3.isEmpty() ? 1 : parseRoman(string3);
    }

    private static int parseRoman(String string) {
        byte b = 0;
        byte b2 = 0;

        for (int i = string.length() - 1; i >= 0; i--) {
            byte b3;
            switch (Character.toUpperCase(string.charAt(i))) {
                case 'I':
                    b3 = 1;
                    break;
                case 'V':
                    b3 = 5;
                    break;
                case 'X':
                    b3 = 10;
                    break;
                default:
                    return 0;
            }

            b += b3 < b2 ? -b3 : b3;
            b2 = b3;
        }

        return b;
    }

    public CustomEnchant getEnchant() {
        return this.enchant;
    }

    public String getLowerName() {
        return this.lowerName;
    }
}
