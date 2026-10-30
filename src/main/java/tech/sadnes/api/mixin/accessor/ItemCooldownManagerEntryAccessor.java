package tech.sadnes.api.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net/minecraft/entity/player/ItemCooldownManager$Entry")
public interface ItemCooldownManagerEntryAccessor {
    @Accessor("startTick")
    int sadnes$getStartTick();

    @Accessor("endTick")
    int sadnes$getEndTick();
}
