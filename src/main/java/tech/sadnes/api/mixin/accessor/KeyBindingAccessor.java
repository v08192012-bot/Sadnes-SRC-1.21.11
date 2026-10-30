package tech.sadnes.api.mixin.accessor;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Key;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
    @Accessor("boundKey")
    Key sadnes$getBoundKey();

    @Accessor("timesPressed")
    int sadnes$getTimesPressed();

    @Accessor("timesPressed")
    void sadnes$setTimesPressed(int i);
}
