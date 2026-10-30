package tech.sadnes.client.ui.hud;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;

public interface Draggable {
    MsdfFont HEADER_FONT = Fonts.INTER_SEMIBOLD;
    MsdfFont BODY_FONT = Fonts.INTER_MEDIUM;
    float PADDING = 6.0F;
    float SPACING = 6.0F;
    float HEADER_HEIGHT = 7.0F;

    void render(DrawContext drawContext);
}
