package tech.sadnes.client.server.autobuy;

import java.util.List;
import java.util.Optional;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import tech.sadnes.client.server.AutoBuyItem;

public final class CustomColorPotionItem extends AutoBuyItem {
    private final String id;
    private final int color;

    public CustomColorPotionItem(int i, String string, int i2) {
        super(Items.TIPPED_ARROW, i);
        this.id = string;
        this.color = i2;
    }

    @Override
    public String getName() {
        return super.getName() + "|" + this.id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public ItemStack createStack() {
        ItemStack itemStack = new ItemStack(Items.TIPPED_ARROW);
        itemStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(this.color), List.of(), Optional.empty()));
        return itemStack;
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        if (!super.matches(itemStack)) {
            return false;
        }

        PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContentsComponent == null) {
            return false;
        }

        Integer integer = potionContentsComponent.customColor().orElse(null);
        return integer != null && integer == this.color;
    }
}
