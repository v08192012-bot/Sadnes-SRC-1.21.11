package tech.sadnes.client.ui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;

public interface ScreenElement {
    void init(int i, int i2);

    void resize(int i, int i2);

    void render(DrawContext drawContext, int i, int i2, float f);

    void mouseMoved(double d, double d2);

    boolean mouseClicked(Click click, boolean flag);

    boolean onMouseRelease(Click click);

    boolean mouseDragged(Click click, double d, double d2);

    boolean mouseScrolled(double d, double d2, double d3, double d4);

    boolean onKeyPress(KeyInput keyInput);

    boolean keyReleased(KeyInput keyInput);

    boolean onCharType(CharInput charInput);

    boolean shouldPause();

    boolean shouldCloseOnEsc();

    void close();

    void removed();

    default void init(MinecraftClient minecraftClient, int i, int i2) {
        this.init(i, i2);
    }

    default void resizeToWindow() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        this.resize(minecraftClient.getWindow().getScaledWidth(), minecraftClient.getWindow().getScaledHeight());
    }

    default boolean mouseClicked(double d, double d2, int i) {
        return this.mouseClicked(new Click(d, d2, new MouseInput(i, 0)), false);
    }

    default boolean mouseReleased(double d, double d2, int i) {
        return this.onMouseRelease(new Click(d, d2, new MouseInput(i, 0)));
    }

    default boolean mouseDragged(double d, double d2, int i, double d3, double d4) {
        return this.mouseDragged(new Click(d, d2, new MouseInput(i, 0)), d3, d4);
    }

    default boolean mouseScrolled(double d, double d2, double d3) {
        return this.mouseScrolled(d, d2, 0.0, d3);
    }

    default boolean keyPressed(int i, int i2, int i3) {
        return this.onKeyPress(new KeyInput(i, i2, i3));
    }

    default boolean keyReleased(int i, int i2, int i3) {
        return this.keyReleased(new KeyInput(i, i2, i3));
    }

    default boolean charTyped(char c, int i) {
        return this.onCharType(new CharInput(c, i));
    }

    default boolean closesOnEscape() {
        return this.shouldCloseOnEsc();
    }

    default void onClose() {
        this.close();
    }
}
