package tech.sadnes.client.ui.clickgui.component;

import net.minecraft.client.gui.DrawContext;

public interface OptionWidget {
    void renderWidget(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2, float f5);

    boolean mouseClicked(double d, double d2);

    boolean mouseReleased(double d, double d2);

    void update();

    float getWidgetHeight();

    default void reset() {
    }

    default boolean isFocused() {
        return false;
    }

    default boolean keyPressed(int i) {
        return false;
    }

    default String getText() {
        return null;
    }

    default void setText(String string) {
    }
}
