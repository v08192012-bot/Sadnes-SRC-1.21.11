package tech.sadnes.client.feature.impl.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EntityTypeTags;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;

@FeatureInfo(
    name = "AutoWeapon",
    description = "feature.impl.combat.autoweapon",
    displayName = "feature.impl.combat.autoweapon",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_WEAPONS
)
public final class AutoWeapon extends Feature {
    @EventHandler
    public void onAttack(AttackEntityEvent attackEntityEvent) {
        if (attackEntityEvent.getStage() == AttackEntityEvent.Stage.PRE && mc.player != null && mc.world != null) {
            if (attackEntityEvent.getPlayer() == mc.player && !mc.player.isSpectator()) {
                EntityAttributeInstance entityAttributeInstance = new EntityAttributeInstance(EntityAttributes.ATTACK_DAMAGE, entityAttributeInstance2 -> {});
                entityAttributeInstance.setFrom(mc.player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE));
                mc.player.getMainHandStack().applyAttributeModifiers(EquipmentSlot.MAINHAND, (registryEntry, entityAttributeModifier) -> {
                    if (registryEntry.matches(EntityAttributes.ATTACK_DAMAGE)) {
                        entityAttributeInstance.removeModifier(entityAttributeModifier.id());
                    }
                });
                int i = mc.player.getInventory().getSelectedSlot();
                int i2 = i;
                double d = this.getAttackDamage(mc.player.getMainHandStack(), attackEntityEvent.getTarget(), entityAttributeInstance);

                for (int i3 = 0; i3 < 9; i3++) {
                    ItemStack itemStack = mc.player.getInventory().getStack(i3);
                    if (i3 != i && !itemStack.isEmpty()) {
                        double d2 = this.getAttackDamage(itemStack, attackEntityEvent.getTarget(), entityAttributeInstance);
                        if (!(d2 <= d)) {
                            d = d2;
                            i2 = i3;
                        }
                    }
                }

                if (i2 != i) {
                    attackEntityEvent.setWeaponSlot(i2);
                }
            }
        }
    }

    private double getAttackDamage(ItemStack itemStack, Entity entity2, EntityAttributeInstance entityAttributeInstance) {
        EntityAttributeInstance entityAttributeInstance2 = new EntityAttributeInstance(EntityAttributes.ATTACK_DAMAGE, entityAttributeInstance3 -> {});
        entityAttributeInstance2.setFrom(entityAttributeInstance);
        itemStack.applyAttributeModifiers(EquipmentSlot.MAINHAND, (registryEntry, entityAttributeModifier) -> {
            if (registryEntry.matches(EntityAttributes.ATTACK_DAMAGE)) {
                entityAttributeInstance2.removeModifier(entityAttributeModifier.id());
                entityAttributeInstance2.addTemporaryModifier(entityAttributeModifier);
            }
        });
        double d = entityAttributeInstance2.getValue();
        double d2 = d + itemStack.getItem().getBonusAttackDamage(entity2, (float)d, mc.player.getDamageSources().playerAttack(mc.player));
        return d2 + this.getEnchantmentBonus(itemStack, entity2);
    }

    private double getEnchantmentBonus(ItemStack itemStack, Entity entity) {
        double d = 0.0;
        boolean flag;
        if (itemStack.isOf(Items.MACE)) {
            flag = MaceItem.shouldDealAdditionalDamage(mc.player);
        } else {
            flag = false;
        }

        for (Entry entry : itemStack.getEnchantments().getEnchantmentEntries()) {
            RegistryEntry registryEntry = (RegistryEntry)entry.getKey();
            int i = entry.getIntValue();
            if (registryEntry.matchesKey(Enchantments.SHARPNESS)) {
                d += 0.5 * i + 0.5;
            }

            if (registryEntry.matchesKey(Enchantments.SMITE) && entity.getType().isIn(EntityTypeTags.SENSITIVE_TO_SMITE)) {
                d += 2.5 * i;
            }

            if (registryEntry.matchesKey(Enchantments.BANE_OF_ARTHROPODS) && entity.getType().isIn(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
                d += 2.5 * i;
            }

            if (registryEntry.matchesKey(Enchantments.IMPALING) && entity.getType().isIn(EntityTypeTags.SENSITIVE_TO_IMPALING)) {
                d += 2.5 * i;
            }

            if (registryEntry.matchesKey(Enchantments.DENSITY) && flag) {
                d += 0.5 * i * mc.player.fallDistance;
            }
        }

        return d;
    }
}
