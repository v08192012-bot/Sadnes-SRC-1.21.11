package tech.sadnes.client.feature.component;

import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;

public final class InputLockComponent extends FeatureComponent {
    @EventHandler(priority = -500)
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (ClickGuiScreen.isTypingText()) {
            moveInputEvent.setCancelled(true);
        }
    }
}
