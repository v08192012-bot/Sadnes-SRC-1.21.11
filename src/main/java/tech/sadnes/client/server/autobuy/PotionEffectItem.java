package tech.sadnes.client.server.autobuy;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Setting;

public final class PotionEffectItem extends AutoBuyItem {
    private final String id;
    private final List<RegistryEntry<StatusEffect>> effects;
    private final NumberSetting potionLevel = new NumberSetting("autobuy.setting.potionlevel", 1.0F, 1.0F, 3.0F, 1.0F);

    public PotionEffectItem(int i, String string, List<RegistryEntry<StatusEffect>> registryEntries) {
        super(Items.POTION, i);
        this.id = string;
        this.effects = List.copyOf(registryEntries);
    }

    @Override
    public NumberSetting getDiscountSetting() {
        return this.potionLevel;
    }

    @Override
    public List<Setting<?>> getSettings() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(this.potionLevel);
        arrayList.addAll(super.getSettings());
        return arrayList;
    }

    @Override
    public void save(JsonObject jsonObject) {
        super.save(jsonObject);
        jsonObject.addProperty("potionLevel", this.potionLevel.getValue());
    }

    @Override
    public void load(JsonObject jsonObject) {
        super.load(jsonObject);
        if (jsonObject.has("potionLevel")) {
            this.potionLevel.setValue(Float.valueOf(jsonObject.get("potionLevel").getAsFloat()));
        }
    }

    @Override
    public String getName() {
        StringBuilder builder = new StringBuilder(super.getName());

        for (RegistryEntry registryEntry : this.effects) {
            builder.append('#').append(registryEntry.getIdAsString());
        }

        return builder.toString();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public int getMaxCount() {
        return 64;
    }

    @Override
    public String getCategoryName() {
        int i = this.id.indexOf(" + ");
        return i > 0 ? this.id.substring(0, i) : this.id;
    }

    @Override
    public ItemStack createStack() {
        ItemStack itemStack = new ItemStack(Items.POTION);
        ArrayList arrayList = new ArrayList();

        for (RegistryEntry registryEntry : this.effects) {
            arrayList.add(new StatusEffectInstance(registryEntry, 1800, 0));
        }

        itemStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.empty(), arrayList, Optional.empty()));
        return itemStack;
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        if (itemStack != null && itemStack.isOf(Items.POTION)) {
            PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
            if (potionContentsComponent == null) {
                return false;
            }

            int i = (int)this.potionLevel.getValue().floatValue();
            ArrayList<RegistryEntry<StatusEffect>> registryEntries = new ArrayList<>();

            for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
                if (statusEffectInstance.getAmplifier() + 1 < i) {
                    return false;
                }

                registryEntries.add(statusEffectInstance.getEffectType());
            }

            if (registryEntries.size() != this.effects.size()) {
                return false;
            }

            ArrayList arrayList = new ArrayList<>(this.effects);

            for (RegistryEntry registryEntry : registryEntries) {
                if (!arrayList.remove(registryEntry)) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
