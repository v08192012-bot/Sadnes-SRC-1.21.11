package tech.sadnes.client.ui.screen.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Type;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.OptionWidget;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class KeybindOptionWidget implements IRenderable, OptionWidget {
    public static KeybindOptionWidget listeningWidget;
    private final KeyBinding keyBinding;
    private final Animation hoverAnimation = new Animation();
    private float x;
    private float y;
    private float width;
    private float height;

    public KeybindOptionWidget(KeyBinding keyBinding2) {
        this.keyBinding = keyBinding2;
    }

    private boolean isListening() {
        return listeningWidget == this;
    }

    @Override
    public void renderWidget(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2, float f5) {
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
        this.hoverAnimation.update();
        boolean flag = this.isHovered(i, i2, f, f2, f3, f4);
        this.hoverAnimation.animate(flag ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f6 = this.hoverAnimation.get();
        boolean flag2 = this.isListening();
        Radius radius = Radius.all(6.0F);
        GuiRenderUtil.drawSurface(drawContext, f, f2, f3, f4, f5, 6.0F);
        if (flag2) {
            new RenderUtil(drawContext, f, f2, f3, f4).rect(Theme.withAlpha(Theme.getPrimary(), 0.28F * f5), radius);
        } else if (f6 > 0.001F) {
            new RenderUtil(drawContext, f, f2, f3, f4).rect(Theme.withAlpha(Theme.getPrimary(), 0.18F * f6 * f5), radius);
        }

        String string = flag2 ? "> ? <" : this.keyBinding.getBoundKeyLocalizedText().getString();
        int i3;
        if (flag2) {
            i3 = Theme.withAlpha(ColorUtil.WHITE, f5);
        } else if (this.keyBinding.isUnbound()) {
            i3 = Theme.withAlpha(Theme.getSecondaryText(), 0.7F * f5);
        } else {
            i3 = Theme.withAlpha(ColorUtil.interpolate(Theme.getTextColor(), ColorUtil.WHITE, f6), f5);
        }

        new RenderUtil(drawContext, f, f2, f3, f4).textCenter(Fonts.INTER_MEDIUM, string, i3, 6.5F);
    }

    @Override
    public boolean mouseClicked(double d, double d2) {
        if (this.isHovered(d, d2, this.x, this.y, this.width, this.height)) {
            listeningWidget = this;
            return true;
        } else {
            return false;
        }
    }

    public void onKey(KeyInput keyInput) {
        if (keyInput.key() == 256) {
            this.keyBinding.setBoundKey(InputUtil.UNKNOWN_KEY);
            KeyBinding.updateKeysByCode();
            listeningWidget = null;
        } else {
            this.keyBinding.setBoundKey(InputUtil.fromKeyCode(keyInput));
            KeyBinding.updateKeysByCode();
            listeningWidget = null;
        }
    }

    public void onMouseButton(int i) {
        this.keyBinding.setBoundKey(Type.MOUSE.createFromCode(i));
        KeyBinding.updateKeysByCode();
        listeningWidget = null;
    }

    @Override
    public boolean mouseReleased(double d, double d2) {
        return false;
    }

    @Override
    public void update() {
    }

    @Override
    public float getWidgetHeight() {
        return 84.0F;
    }

    @Override
    public void reset() {
        this.keyBinding.setBoundKey(this.keyBinding.getDefaultKey());
    }

    @Override
    public boolean isFocused() {
        return !this.keyBinding.isDefault();
    }

    @Override
    public boolean keyPressed(int i) {
        if (i == 0) {
            listeningWidget = this;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getText() {
        return this.keyBinding.getBoundKeyTranslationKey();
    }

    @Override
    public void setText(String string) {
        try {
            this.keyBinding.setBoundKey(InputUtil.fromTranslationKey(string));
        } catch (Exception exception) {
        }
    }
}
