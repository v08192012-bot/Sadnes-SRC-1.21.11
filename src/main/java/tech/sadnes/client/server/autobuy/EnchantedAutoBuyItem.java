package tech.sadnes.client.server.autobuy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import tech.sadnes.client.server.AutoBuyItem;

public class EnchantedAutoBuyItem extends AutoBuyItem {
    private final List<EnchantRequirement> variants;
    private List<EnchantRequirement> fixedRequirements = List.of();

    public EnchantedAutoBuyItem(Item item, int i, EnchantRequirement... enchantRequirements) {
        super(item, i);
        this.variants = List.of(enchantRequirements);
    }

    public EnchantedAutoBuyItem withFixedRequirements(EnchantRequirement... enchantRequirements) {
        this.fixedRequirements = List.of(enchantRequirements);
        return this;
    }

    @Override
    public List<EnchantRequirement> getVariants() {
        return this.variants;
    }

    public EnchantRequirement findVariant(String string) {
        int i = this.variants.size();

        for (int i2 = 0; i2 < i; i2++) {
            EnchantRequirement enchantRequirement = this.variants.get(i2);
            if (enchantRequirement.getKey().equals(string)) {
                return enchantRequirement;
            }
        }

        return null;
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        if (!super.matches(itemStack)) {
            return false;
        }

        int i = this.fixedRequirements.size();

        for (int i2 = 0; i2 < i; i2++) {
            if (!this.fixedRequirements.get(i2).test(itemStack)) {
                return false;
            }
        }

        int i3 = this.variants.size();

        for (int i4 = 0; i4 < i3; i4++) {
            if (!this.variants.get(i4).test(itemStack)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void save(JsonObject jsonObject) {
        super.save(jsonObject);
        JsonObject jsonObject2 = new JsonObject();

        for (EnchantRequirement enchantRequirement : this.variants) {
            if (enchantRequirement.isRequired()) {
                jsonObject2.addProperty(enchantRequirement.getKey(), enchantRequirement.getMinLevel());
            } else if (enchantRequirement.isExcluded()) {
                jsonObject2.addProperty(enchantRequirement.getKey(), 0);
            } else if (enchantRequirement.getDefaultMode() != EnchantRequirement.Mode.ANY) {
                jsonObject2.addProperty(enchantRequirement.getKey(), -1);
            }
        }

        if (jsonObject2.size() > 0) {
            jsonObject.add("enchants", jsonObject2);
        }
    }

    @Override
    public void load(JsonObject jsonObject) {
        super.load(jsonObject);
        Iterator iterator2 = this.variants.iterator();

        while (iterator2.hasNext()) {
            ((EnchantRequirement)iterator2.next()).resetMode();
        }

        if (jsonObject.has("enchants")) {
            for (Entry entry : jsonObject.getAsJsonObject("enchants").entrySet()) {
                EnchantRequirement enchantRequirement = this.findVariant((String)entry.getKey());
                if (enchantRequirement != null) {
                    int i = ((JsonElement)entry.getValue()).getAsInt();
                    if (i < 0) {
                        enchantRequirement.setMode(EnchantRequirement.Mode.ANY);
                    } else if (i == 0) {
                        enchantRequirement.setMode(EnchantRequirement.Mode.EXCLUDE);
                    } else {
                        enchantRequirement.setMode(EnchantRequirement.Mode.REQUIRE);
                        enchantRequirement.setMinLevel(i);
                    }
                }
            }
        }
    }
}
