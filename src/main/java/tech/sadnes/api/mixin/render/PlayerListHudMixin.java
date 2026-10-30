package tech.sadnes.api.mixin.render;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tech.sadnes.client.feature.impl.misc.StreamerMode;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @ModifyVariable(method = "setHeader", at = @At("HEAD"), argsOnly = true)
    private Text sadnes$replaceHeader(Text text) {
        return StreamerMode.replaceInText(text);
    }

    @ModifyVariable(method = "setFooter", at = @At("HEAD"), argsOnly = true)
    private Text sadnes$replaceFooter(Text text) {
        return StreamerMode.replaceInText(text);
    }
}
