package tech.sadnes.client.feature.component;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.ScreenMouseScrollEvent;
import tech.sadnes.client.feature.ComponentManager;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureComponent;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.clickgui.component.SettingsPopupComponent;
import tech.sadnes.client.ui.drag.DragComponent;

public final class HudSettingsPopupComponent extends FeatureComponent {
    private final SettingsPopupComponent popup = new SettingsPopupComponent();
    private boolean initialized;
    private int heldButton = -1;
    private double lastMouseX;
    private double lastMouseY;

    public static HudSettingsPopupComponent getInstance() {
        ComponentManager componentManager = Sadnes.getInstance().getComponentManager();
        return componentManager == null ? null : componentManager.getComponent(HudSettingsPopupComponent.class);
    }

    private boolean isChatOpen() {
        return mc.currentScreen instanceof ChatScreen;
    }

    public boolean isOpen() {
        return this.popup.isOpen();
    }

    public boolean isVisible() {
        return this.popup.isShowing();
    }

    public boolean isHovered(double d, double d2) {
        return this.popup.isShowing() && (this.popup.isMouseOverPanel(d, d2) || this.popup.isMouseOverAny(d, d2));
    }

    public static boolean isAnyPopupHovered(double d, double d2) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen clickGuiScreen) {
            SettingsPopupComponent settingsPopupComponent = clickGuiScreen.getSettingsPopup();
            if (settingsPopupComponent != null && settingsPopupComponent.isShowing() && (settingsPopupComponent.isMouseOverPanel(d, d2) || settingsPopupComponent.isMouseOverAny(d, d2))) {
                return true;
            }
        }

        HudSettingsPopupComponent hudSettingsPopupComponent = getInstance();
        return hudSettingsPopupComponent != null && hudSettingsPopupComponent.isHovered(d, d2);
    }

    private void ensureInitialized() {
        if (!this.initialized) {
            this.popup.resize((int)GuiScale.getScaledWidth(), (int)GuiScale.getScaledHeight());
            this.initialized = true;
        }
    }

    private void close() {
        this.popup.close();
        this.heldButton = -1;
    }

    private void forceClose() {
        this.popup.forceClose();
        this.heldButton = -1;
    }

    private boolean tryOpenFor(double d, double d2) {
        DragComponent dragComponent = DragComponent.getInstance();
        if (dragComponent == null) {
            return false;
        }

        DragSetting dragSetting = dragComponent.getDragHandler().getElementAt(d, d2);
        if (dragSetting != null && dragSetting.getOwner() instanceof Feature feature) {
            this.ensureInitialized();
            if (this.popup.isOpen() && this.popup.getHolder() == feature) {
                this.close();
                return true;
            } else {
                this.popup.openSide(feature, dragSetting.position2.x, dragSetting.position2.y, dragSetting.size2.x, (int)GuiScale.getScaledWidth(), (int)GuiScale.getScaledHeight());
                return true;
            }
        } else {
            return false;
        }
    }

    public void render(DrawContext drawContext, int i, int i2, float f) {
        if (this.isChatOpen() && (this.popup.isShowing() || this.popup.isOpen())) {
            ClickGuiScreen clickGuiScreen = Sadnes.getInstance().getClickGui();
            if (clickGuiScreen != null) {
                clickGuiScreen.getOpenAnimation().forceValue(1.0);
            }

            this.popup.render(drawContext, i, i2, f);
            this.popup.renderOverlays(drawContext, i, i2, f);
        }
    }

    @EventHandler
    public void onMouseMove(Render2DEvent render2DEvent) {
        if (this.isChatOpen()) {
            double d = render2DEvent.getMouseX();
            double d2 = render2DEvent.getMouseY();
            if (this.popup.isOpen() && this.heldButton == 0) {
                this.popup.mouseDragged(createClick(d, d2, 0), d - this.lastMouseX, d2 - this.lastMouseY);
            }

            this.lastMouseX = d;
            this.lastMouseY = d2;
        } else {
            if (this.popup.isOpen() || this.popup.isShowing()) {
                this.forceClose();
            }
        }
    }

    @EventHandler
    public void onMouseButton(ScreenKeyEvent screenKeyEvent) {
        if (!this.isChatOpen()) {
            if (this.popup.isOpen()) {
                this.forceClose();
            }
        } else {
            double d = screenKeyEvent.getMouseX();
            double d2 = screenKeyEvent.getMouseY();
            if (screenKeyEvent.isPress()) {
                if (screenKeyEvent.getKey() != 1 || !this.tryOpenFor(d, d2)) {
                    if (this.popup.isOpen()) {
                        this.heldButton = screenKeyEvent.getKey();
                        this.lastMouseX = d;
                        this.lastMouseY = d2;
                        if (!this.isHovered(d, d2) && !this.popup.hasOpenOverlay()) {
                            this.close();
                        } else {
                            this.popup.mouseClicked(createClick(d, d2, screenKeyEvent.getKey()), false);
                        }
                    }
                }
            } else {
                if (screenKeyEvent.isRelease()) {
                    if (this.popup.isOpen()) {
                        this.popup.onMouseRelease(createClick(d, d2, screenKeyEvent.getKey()));
                    }

                    this.heldButton = -1;
                }
            }
        }
    }

    @EventHandler
    public void onMouseScroll(ScreenMouseScrollEvent screenMouseScrollEvent) {
        if (this.isChatOpen() && this.popup.isOpen()) {
            if (this.isHovered(screenMouseScrollEvent.getMouseX(), screenMouseScrollEvent.getMouseY()) || this.popup.hasOpenOverlay()) {
                this.popup.mouseScrolled(screenMouseScrollEvent.getMouseX(), screenMouseScrollEvent.getMouseY(), screenMouseScrollEvent.getHorizontalAmount(), screenMouseScrollEvent.getVerticalAmount());
                screenMouseScrollEvent.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onKey(ScreenMouseClickEvent screenMouseClickEvent) {
        if (this.isChatOpen() && this.popup.isOpen()) {
            KeyInput keyInput = new KeyInput(screenMouseClickEvent.getButton(), screenMouseClickEvent.getMouseX(), screenMouseClickEvent.getMouseY());
            if (screenMouseClickEvent.isPress()) {
                if (screenMouseClickEvent.getButton() == 256) {
                    this.close();
                } else {
                    this.popup.onKeyPress(keyInput);
                }
            } else {
                if (screenMouseClickEvent.isRelease()) {
                    this.popup.keyReleased(keyInput);
                }
            }
        }
    }

    private static Click createClick(double d, double d2, int i) {
        return new Click(d, d2, new MouseInput(i, 0));
    }
}
