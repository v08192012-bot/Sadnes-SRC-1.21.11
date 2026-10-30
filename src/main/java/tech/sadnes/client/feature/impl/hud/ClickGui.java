package tech.sadnes.client.feature.impl.hud;

import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;

@FeatureInfo(
    name = "ClickGui",
    description = "feature.impl.hud.clickgui",
    displayName = "feature.impl.hud.clickgui",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_MISC,
    keys = {344, 260},
    autoEnabled = true,
    allowDisable = false
)
public final class ClickGui extends Feature {
    @EventHandler
    public void onMouseClick(ScreenMouseClickEvent screenMouseClickEvent) {
        if (screenMouseClickEvent.isPress() && screenMouseClickEvent.getScreen() == null && this.isBoundTo(screenMouseClickEvent.getButton())) {
            mc.setScreen(Sadnes.getInstance().getClickGui());
        }
    }
}
