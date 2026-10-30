package tech.sadnes.client.ui.clickgui.component;

import net.minecraft.client.gui.DrawContext;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.screen.ScreenElement;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public abstract class Component implements IInteractable, IRenderable, ScreenElement {
    public Vector2f position = new Vector2f();
    public Vector2f size = new Vector2f();
    public final Animation hoverAnimation = new Animation();
    public final Animation stateAnimation = new Animation();

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        this.hoverAnimation.update();
        this.stateAnimation.update();
    }

    public float getAlpha() {
        return (float)Math.pow(this.getClickGui().getOpenAnimation().get(), 3.0);
    }

    public int applyAlpha(int i) {
        return ColorUtil.multiplyAlpha(i, this.getAlpha());
    }

    public int getPrimaryColor() {
        return this.getPrimaryColor(1.0F);
    }

    public int getPrimaryColor(float f) {
        return ColorUtil.multiplyAlpha(Theme.getTextColor(), f * this.getAlpha());
    }

    public int getSecondaryColor() {
        return this.getSecondaryColor(1.0F);
    }

    public int getSecondaryColor(float f) {
        return ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f * this.getAlpha());
    }

    public ClickGuiScreen getClickGui() {
        return Sadnes.getInstance().getClickGui();
    }
}
