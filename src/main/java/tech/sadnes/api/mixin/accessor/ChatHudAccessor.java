package tech.sadnes.api.mixin.accessor;

import java.util.List;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatHud.class)
public interface ChatHudAccessor {
    @Accessor("visibleMessages")
    List<Visible> sadnes$getVisibleMessages();

    @Accessor("scrolledLines")
    int sadnes$getScrolledLines();
}
