package tech.sadnes.api.mixin.accessor;

import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInventory.class)
public interface PlayerInventoryAccessor {
    @Accessor("selectedSlot")
    int sadnes$getSelectedSlot();

    @Accessor("selectedSlot")
    void sadnes$setSelectedSlot(int i);
}
