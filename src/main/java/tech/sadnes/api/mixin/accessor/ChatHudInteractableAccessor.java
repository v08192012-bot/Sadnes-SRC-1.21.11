package tech.sadnes.api.mixin.accessor;

import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net/minecraft/client/gui/hud/ChatHud$Interactable")
public interface ChatHudInteractableAccessor {
    @Accessor("untransformedOffset")
    Vector2f sadnes$getUntransformedOffset();
}
