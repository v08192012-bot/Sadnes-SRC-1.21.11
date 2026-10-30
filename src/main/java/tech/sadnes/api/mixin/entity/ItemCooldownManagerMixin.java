package tech.sadnes.api.mixin.entity;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.server.CooldownItem;
import tech.sadnes.client.server.HealingPotionCooldown;
import tech.sadnes.client.server.ServerManager;

@Mixin(ItemCooldownManager.class)
public class ItemCooldownManagerMixin {
    @Inject(method = "getGroup", at = @At("RETURN"), cancellable = true)
    private void sadnes$healingPotionGroup(ItemStack itemStack, CallbackInfoReturnable<Identifier> callbackInfoReturnable) {
        if (CooldownItem.HEALING_POTION.matches(itemStack)) {
            if (ServerManager.isFunTime() || ServerManager.isSpookyTime()) {
                callbackInfoReturnable.setReturnValue(HealingPotionCooldown.COOLDOWN_ID);
            }
        }
    }
}
