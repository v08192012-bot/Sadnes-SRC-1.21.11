package tech.sadnes.client.ui.screen.settings;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.OptionWidget;
import tech.sadnes.client.util.OptionsUtil;

public final class ToggleOptionWidget implements IRenderable, OptionWidget {
    private final OptionsUtil.BoolOption option;
    private final Animation toggleAnimation = new Animation();
    private float x;
    private float y;
    private float width;
    private float height;

    public ToggleOptionWidget(OptionsUtil.BoolOption boolOption) {
        this.option = boolOption;
    }

    @Override
    public void renderWidget(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2, float f5) {
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
        this.toggleAnimation.update();
        this.toggleAnimation.animate(this.option.get() ? 1.0 : 0.0, 0.5, Easings.EXPO_OUT);
        float f6 = this.toggleAnimation.get();
        float f7 = f + f3 - 14.0F;
        float f8 = f2 + f4 / 2.0F - 4.5F;
        SettingWidgets.drawDefaultSwitch(drawContext, f7, f8, f6, f5);
    }

    @Override
    public boolean mouseClicked(double d, double d2) {
        if (this.isHovered(d, d2, this.x, this.y, this.width, this.height)) {
            OptionsUtil.BoolOption boolOption = this.option;
            if (!this.option.get()) {
                boolOption.set(true);
                return true;
            } else {
                boolOption.set(false);
                return true;
            }
        } else {
            return false;
        }
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
        return 20.0F;
    }

    @Override
    public void reset() {
        this.option.reset();
    }

    @Override
    public boolean isFocused() {
        return !this.option.isDefault();
    }

    @Override
    public boolean keyPressed(int i) {
        if (i > 0) {
            this.option.set(true);
            return true;
        } else if (i < 0) {
            this.option.set(false);
            return true;
        } else {
            OptionsUtil.BoolOption boolOption = this.option;
            if (!this.option.get()) {
                boolOption.set(true);
                return true;
            } else {
                boolOption.set(false);
                return true;
            }
        }
    }

    @Override
    public String getText() {
        return Boolean.toString(this.option.get());
    }

    @Override
    public void setText(String string) {
        this.option.set(Boolean.parseBoolean(string));
    }
}
