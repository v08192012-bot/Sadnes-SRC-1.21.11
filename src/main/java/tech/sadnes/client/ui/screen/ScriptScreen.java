package tech.sadnes.client.ui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptPanel;
import tech.sadnes.client.util.IMinecraft;

public final class ScriptScreen extends Screen implements IMinecraft {
    private final Script script;
    private final ScriptPanel scriptUi;
    private final Animation openAnimation = new Animation();
    private Screen parentScreen;
    private boolean closing;

    public ScriptScreen(Script script2, ScriptPanel scriptPanel) {
        super(Text.literal(script2.getDisplayName()));
        this.script = script2;
        this.scriptUi = scriptPanel;
    }

    public static void closeFor(Script script2) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient.currentScreen instanceof ScriptScreen scriptScreen && scriptScreen.script == script2) {
            scriptScreen.returnToParent();
        }
    }

    public boolean isActive() {
        return mc.currentScreen == this && !this.closing;
    }

    public void open() {
        if (mc.currentScreen == this) {
            this.closing = false;
            this.openAnimation.animate(1.0, 0.3, Easings.QUART_OUT);
        } else {
            this.parentScreen = mc.currentScreen;
            mc.setScreen(this);
        }
    }

    public void toggle() {
        if (this.isActive()) {
            this.close();
        } else {
            this.open();
        }
    }

    @Override
    public void close() {
        if (!this.closing) {
            this.closing = true;
            this.openAnimation.animate(0.0, 0.2, Easings.QUAD_IN);
        }
    }

    private void returnToParent() {
        Screen screen = this.parentScreen;
        this.parentScreen = null;
        mc.setScreen(screen);
    }

    @Override
    public void onDisplayed() {
        super.onDisplayed();
        this.closing = false;
        this.openAnimation.forceValue(0.0);
        this.openAnimation.animate(1.0, 0.3, Easings.QUART_OUT);
        this.scriptUi.call("open");
    }

    @Override
    public void removed() {
        this.scriptUi.call("close");
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        this.openAnimation.update();
        if (!this.script.isActive() || this.closing && this.openAnimation.isFinished() && this.openAnimation.get() <= 0.001F) {
            this.returnToParent();
        } else {
            float f2 = Math.min(this.openAnimation.get(), 1.0F);
            this.scriptUi
                .render(
                    drawContext,
                    f2,
                    ScriptPanel.renderArgs(
                        0.0F, 0.0F, GuiScale.getScaledWidth(), GuiScale.getScaledHeight(), GuiScale.toScaled(i), GuiScale.toScaled(i2), f2, f
                    )
                );
        }
    }

    private Click scaleMouseInput(Click click) {
        return new Click(GuiScale.toScaled(click.x()), GuiScale.toScaled(click.y()), click.buttonInfo());
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        Click click2 = this.scaleMouseInput(click);
        return this.scriptUi.callBoolean("click", ScriptPanel.mouseArgs(click2.x(), click2.y(), click2.button(), 0.0, 0.0));
    }

    @Override
    public boolean mouseReleased(Click click) {
        Click click2 = this.scaleMouseInput(click);
        return this.scriptUi.callBoolean("release", ScriptPanel.mouseArgs(click2.x(), click2.y(), click2.button(), 0.0, 0.0));
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        Click click2 = this.scaleMouseInput(click);
        return this.scriptUi.callBoolean("drag", ScriptPanel.mouseArgs(click2.x(), click2.y(), click2.button(), GuiScale.toScaled(d), GuiScale.toScaled(d2)));
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        return this.scriptUi.callBoolean("scroll", ScriptPanel.scrollArgs(GuiScale.toScaled(d), GuiScale.toScaled(d2), d4, d3));
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (this.scriptUi.callBoolean("key", ScriptPanel.keyArgs(keyInput.key(), keyInput.scancode(), keyInput.modifiers(), true))) {
            return true;
        } else if (keyInput.key() == 256 && this.scriptUi.getBoolean("escClose", true)) {
            this.close();
            return true;
        } else {
            return super.keyPressed(keyInput);
        }
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        return this.scriptUi.callBoolean("key", ScriptPanel.keyArgs(keyInput.key(), keyInput.scancode(), keyInput.modifiers(), false));
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        return this.scriptUi.callBoolean("char", ScriptPanel.charArgs(charInput.codepoint(), charInput.modifiers()));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return this.scriptUi.getBoolean("pause", false);
    }
}
