package tech.sadnes.api.mixin.accessor;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InGameHud.class)
public interface InGameHudAccessor {
    @Accessor("overlayMessage")
    Text sadnes$getOverlayMessage();

    @Accessor("overlayRemaining")
    int sadnes$getOverlayRemaining();
}
