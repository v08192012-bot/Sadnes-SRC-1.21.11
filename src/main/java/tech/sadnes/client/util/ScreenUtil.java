package tech.sadnes.client.util;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;

public final class ScreenUtil {
    private ScreenUtil() {
    }

    public static boolean allowsMovement(Screen screen) {
        if (screen == null) {
            return true;
        }

        if (screen instanceof ChatScreen || screen instanceof SignEditScreen || screen instanceof BookEditScreen) {
            return false;
        }

        if (!(screen instanceof HandledScreen)) {
            return false;
        }

        Element element = screen.getFocused();
        return !(element instanceof TextFieldWidget) || !((TextFieldWidget)element).isActive();
    }
}
