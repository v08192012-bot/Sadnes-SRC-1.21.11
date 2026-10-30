package tech.sadnes.client.server.autobuy;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class VanillaEnchantRequirement extends EnchantRequirement {
    private final RegistryKey<Enchantment> enchantment;

    public VanillaEnchantRequirement(RegistryKey<Enchantment> registryKey, int i) {
        super(i);
        this.enchantment = registryKey;
    }

    @Override
    public String getKey() {
        return this.enchantment.getValue().toString();
    }

    @Override
    public String getDisplayName() {
        Identifier identifier = this.enchantment.getValue();
        return Text.translatable("enchantment." + identifier.getNamespace() + "." + identifier.getPath()).getString();
    }

    @Override
    protected int getLevel(ItemStack itemStack) {
        int i = this.getLevelIn(itemStack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
        return i > 0 ? i : this.getLevelIn(itemStack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
    }

    private int getLevelIn(ItemEnchantmentsComponent itemEnchantmentsComponent) {
        for (RegistryEntry registryEntry : itemEnchantmentsComponent.getEnchantments()) {
            if (registryEntry.matchesKey(this.enchantment)) {
                return itemEnchantmentsComponent.getLevel(registryEntry);
            }
        }

        return 0;
    }

    public RegistryKey<Enchantment> getEnchantment() {
        return this.enchantment;
    }
}
