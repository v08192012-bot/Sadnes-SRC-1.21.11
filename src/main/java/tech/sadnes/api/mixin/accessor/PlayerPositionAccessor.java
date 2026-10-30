package tech.sadnes.api.mixin.accessor;

import net.minecraft.entity.EntityPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPosition.class)
public interface PlayerPositionAccessor {
    @Mutable
    @Accessor("yaw")
    void sadnes$setYaw(float f);

    @Mutable
    @Accessor("pitch")
    void sadnes$setPitch(float f);
}
