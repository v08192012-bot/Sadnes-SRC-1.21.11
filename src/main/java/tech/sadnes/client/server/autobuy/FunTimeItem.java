package tech.sadnes.client.server.autobuy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import tech.sadnes.client.util.player.ItemPredicate;

public class FunTimeItem extends EnchantedAutoBuyItem {
    private final String displayName;
    private final List<String> loreLines;
    private final List<ItemPredicate> lorePredicates;
    private RegistryKey<ArmorTrimMaterial> trimPattern;
    private RegistryKey<ArmorTrimPattern> trimMaterial;
    private boolean trimApplied;

    public FunTimeItem(Item item, int i, String string, String[] strings, EnchantRequirement... enchantRequirements) {
        super(item, i, enchantRequirements);
        this.displayName = string;
        this.loreLines = List.of(strings);
        ArrayList arrayList = new ArrayList(this.loreLines.size());

        for (String string2 : this.loreLines) {
            arrayList.add(ItemPredicate.loreContains(string2));
        }

        this.lorePredicates = arrayList;
    }

    public FunTimeItem withSkullTexture(String string) {
        super.withSkullTexture(string);
        return this;
    }

    public FunTimeItem withFixedRequirements(EnchantRequirement... enchantRequirements) {
        super.withFixedRequirements(enchantRequirements);
        return this;
    }

    public FunTimeItem withTrim(RegistryKey<ArmorTrimMaterial> registryKey, RegistryKey<ArmorTrimPattern> registryKey2) {
        this.trimPattern = registryKey;
        this.trimMaterial = registryKey2;
        return this;
    }

    @Override
    public ItemStack createStack() {
        ItemStack itemStack = super.createStack();
        this.applyTrim(itemStack);
        return itemStack;
    }

    @Override
    public boolean isStrictMatch() {
        return this.trimPattern != null && !this.trimApplied && MinecraftClient.getInstance().world != null;
    }

    private void applyTrim(ItemStack itemStack) {
        this.trimApplied = false;
        if (this.trimPattern != null && this.trimMaterial != null) {
            ClientWorld clientWorld = MinecraftClient.getInstance().world;
            if (clientWorld != null) {
                DynamicRegistryManager dynamicRegistryManager = clientWorld.getRegistryManager();
                RegistryEntry registryEntry = dynamicRegistryManager.getOrThrow(RegistryKeys.TRIM_MATERIAL).getOptional(this.trimPattern).orElse(null);
                RegistryEntry registryEntry2 = dynamicRegistryManager.getOrThrow(RegistryKeys.TRIM_PATTERN).getOptional(this.trimMaterial).orElse(null);
                if (registryEntry != null && registryEntry2 != null) {
                    itemStack.set(DataComponentTypes.TRIM, new ArmorTrim(registryEntry, registryEntry2));
                    this.trimApplied = true;
                }
            }
        }
    }

    @Override
    public String getName() {
        return super.getName() + "|" + this.displayName;
    }

    @Override
    public String getId() {
        return this.displayName;
    }

    @Override
    public String getCategoryName() {
        return this.displayName.replaceAll("\\s*\\([^)]*\\)$", "");
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        if (!super.matches(itemStack)) {
            return false;
        }

        int i = this.lorePredicates.size();

        for (int i2 = 0; i2 < i; i2++) {
            if (!this.lorePredicates.get(i2).test(itemStack)) {
                return false;
            }
        }

        return true;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public List<String> getLoreLines() {
        return this.loreLines;
    }
}
