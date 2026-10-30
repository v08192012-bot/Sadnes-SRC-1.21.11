package tech.sadnes.client.server.autobuy;

import java.util.ArrayList;
import java.util.Locale;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.DonateItem;
import tech.sadnes.client.server.DonateItemRegistry;
import tech.sadnes.client.server.ServerType;
import tech.sadnes.client.server.SpecialPotion;
import tech.sadnes.client.util.player.ItemPredicate;

public class DonateAutoBuyItem extends AutoBuyItem {
    private final DonateItem donateItem;

    public DonateAutoBuyItem(DonateItem donateItem2, int i) {
        super(donateItem2.getItem(), i);
        this.donateItem = donateItem2;
    }

    @Override
    public String getName() {
        return "server:" + this.donateItem.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean isAvailableOn(ServerType serverType) {
        return serverType != null && this.donateItem.getServer().getApiName().equals(serverType.getApiName());
    }

    @Override
    public String getId() {
        return this.donateItem.getDisplayName();
    }

    @Override
    public ItemStack createStack() {
        return this.donateItem.createStack();
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            SpecialPotion specialPotion = this.donateItem.getPotionType();
            if (specialPotion != null) {
                return this.matchesPotion(itemStack, specialPotion);
            }

            DonateItemRegistry.Matchers matchers = DonateItemRegistry.getMatchers(this.donateItem.getServer().resolve(), this.donateItem);
            if (matchers == null) {
                return false;
            }

            ItemPredicate itemPredicate = matchers.deep() != null ? matchers.deep() : matchers.strict();
            return itemPredicate != null && itemPredicate.test(itemStack);
        } else {
            return false;
        }
    }

    private boolean matchesPotion(ItemStack itemStack, SpecialPotion specialPotion) {
        PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContentsComponent == null) {
            return false;
        }

        Integer integer = potionContentsComponent.customColor().orElse(null);
        if (integer != null && integer.equals(specialPotion.getColor())) {
            ArrayList arrayList = new ArrayList();

            for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
                arrayList.add(statusEffectInstance);
            }

            return SpecialPotion.matchesEffects(arrayList, specialPotion.getEffects());
        } else {
            return false;
        }
    }

    public DonateItem getDonateItem() {
        return this.donateItem;
    }
}
