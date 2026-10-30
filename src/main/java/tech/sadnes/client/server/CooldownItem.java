package tech.sadnes.client.server;

import java.util.function.Predicate;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;

public enum CooldownItem {
    GOLDEN_APPLE(Items.GOLDEN_APPLE, 30000L, 10000L),
    ENCHANTED_GOLDEN_APPLE(Items.ENCHANTED_GOLDEN_APPLE, 150000L, 300000L),
    HEALING_POTION(Items.POTION, 20000L, 10000L, itemStack -> hasEffect(itemStack, StatusEffects.INSTANT_HEALTH)) {
        @Override
        protected ItemStack createIcon() {
            return PotionContentsComponent.createStack(Items.POTION, Potions.HEALING);
        }
    };

    private static final CooldownItem[] VALUES = values();
    private final Item item;
    private final long cooldownMs;
    private final long holyWorldCooldownMs;
    private final Predicate<ItemStack> matcher;
    private ItemStack icon;

    CooldownItem(Item item2, long l, long l2) {
        this(item2, l, l2, null);
    }

    CooldownItem(Item item2, long l, long l2, Predicate<ItemStack> predicate) {
        this.item = item2;
        this.cooldownMs = l;
        this.holyWorldCooldownMs = l2;
        this.matcher = predicate;
    }

    public static CooldownItem fromStack(ItemStack itemStack) {
        if (itemStack != null && !itemStack.isEmpty()) {
            for (CooldownItem cooldownItem : VALUES) {
                if (cooldownItem.matches(itemStack)) {
                    return cooldownItem;
                }
            }
        }

        return null;
    }

    public boolean matches(ItemStack itemStack) {
        return itemStack.isOf(this.item) && (this.matcher == null || this.matcher.test(itemStack));
    }

    public ItemStack getIcon() {
        if (this.icon == null) {
            this.icon = this.createIcon();
        }

        return this.icon;
    }

    protected ItemStack createIcon() {
        return this.item.getDefaultStack();
    }

    private static boolean hasEffect(ItemStack itemStack, RegistryEntry<StatusEffect> registryEntry) {
        PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContentsComponent == null) {
            return false;
        }

        for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
            if (statusEffectInstance.getEffectType().equals(registryEntry)) {
                return true;
            }
        }

        return false;
    }

    public Item getItem() {
        return this.item;
    }

    public long getCooldownMs() {
        return this.cooldownMs;
    }

    public long getHolyWorldCooldownMs() {
        return this.holyWorldCooldownMs;
    }
}
