package tech.sadnes.client.ui.screen.settings;

import java.util.List;

public interface DropdownWidget {
    List<String> getOptionNames();

    int getSelectedIndex();

    void selectIndex(int i);

    float getX();

    float getY();

    float getWidth();

    float getHeight();

    boolean isHeaderHovered(double d, double d2);

    default void setChevronRotation(float f) {
    }
}
