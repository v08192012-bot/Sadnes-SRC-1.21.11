package tech.sadnes.client.ui.drag;

import java.util.ArrayList;
import java.util.List;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.setting.DragSetting;

public final class DragComponent extends FeatureComponent {
    private static final List<DragSetting> PENDING_SETTINGS = new ArrayList<>();
    private final DragManager dragHandler = new DragManager();

    public DragComponent() {
        for (DragSetting dragSetting : PENDING_SETTINGS) {
            this.dragHandler.register(dragSetting);
        }

        PENDING_SETTINGS.clear();
    }

    public static DragComponent getInstance() {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        return componentManager == null ? null : componentManager.getComponent(DragComponent.class);
    }

    public static void register(DragSetting dragSetting) {
        DragComponent dragComponent = getInstance();
        if (dragComponent != null) {
            dragComponent.dragHandler.register(dragSetting);
        } else {
            if (dragSetting != null && !PENDING_SETTINGS.contains(dragSetting)) {
                PENDING_SETTINGS.add(dragSetting);
            }
        }
    }

    public static void unregister(DragSetting dragSetting) {
        DragComponent dragComponent = getInstance();
        if (dragComponent != null) {
            dragComponent.dragHandler.unregister(dragSetting);
        }

        PENDING_SETTINGS.remove(dragSetting);
    }

    public DragManager getDragHandler() {
        return this.dragHandler;
    }

    @EventHandler
    public void onRender(Render2DEvent render2DEvent) {
        int i = (int)render2DEvent.getMouseX();
        int i2 = (int)render2DEvent.getMouseY();
        this.dragHandler.update(render2DEvent.getMouseX(), render2DEvent.getMouseY());

        for (DragSetting dragSetting : this.dragHandler.getElements()) {
            dragSetting.renderElement(render2DEvent.getContext(), i, i2, render2DEvent.getTickDelta());
        }

        this.dragHandler.render(render2DEvent.getContext());
    }

    @EventHandler
    public void onMouse(ScreenKeyEvent screenKeyEvent) {
        if (screenKeyEvent.isPress()) {
            this.dragHandler.mouseClicked(screenKeyEvent.getMouseX(), screenKeyEvent.getMouseY(), screenKeyEvent.getKey());
        }

        if (screenKeyEvent.isRelease()) {
            this.dragHandler.mouseReleased(screenKeyEvent.getKey());
        }
    }
}
