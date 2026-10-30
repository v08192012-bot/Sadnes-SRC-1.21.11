package tech.sadnes.client.ui.screen.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.SimpleOption;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.OptionWidget;
import tech.sadnes.client.util.OptionsUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class CycleOptionWidget<E> implements IRenderable, OptionWidget, DropdownWidget {
    private final SimpleOption<E> option;
    private final E[] values;
    private final Function<E, String> nameFormatter;
    private final Animation hoverAnimation = new Animation();
    private float x;
    private float y;
    private float width;
    private float height;
    private float chevronRotation;

    public CycleOptionWidget(SimpleOption<E> simpleOption, E[] values2, Function<E, String> function) {
        this.option = simpleOption;
        this.values = (E[])values2;
        this.nameFormatter = function;
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
        Radius radius = Radius.all(6.0F);
        GuiRenderUtil.drawSurface(drawContext, f, f2, f3, f4, f5, 6.0F);
        if (f6 > 0.001F) {
            new RenderUtil(drawContext, f, f2, f3, f4).rect(Theme.withAlpha(Theme.getPrimary(), 0.18F * f6 * f5), radius);
        }

        int i3 = Theme.withAlpha(ColorUtil.interpolate(Theme.getTextColor(), ColorUtil.WHITE, f6), f5);
        new RenderUtil(drawContext, f + 8.0F, f2, f3 - 8.0F - 12.0F, f4)
            .textMiddleLeft(Fonts.INTER_MEDIUM, this.nameFormatter.apply(this.option.getValue()), 0.0F, 0.0F, i3, 6.5F);
        float f7 = f + f3 - 11.0F;
        float f8 = f2 + (f4 - 5.5F) / 2.0F;
        boolean flag2 = Math.abs(this.chevronRotation) > 0.001F;
        if (flag2) {
            MatrixUtil.push(drawContext);
            MatrixUtil.rotate(drawContext, f7 + 5.5F / 2.0F, f8 + 5.5F / 2.0F, 90.0F * this.chevronRotation);
        }

        new RenderUtil(drawContext, f, f2, f3, f4)
            .icon(Icon.CHEVRON_RIGHT, f3 - 11.0F, (f4 - 5.5F) / 2.0F, 5.5F, Theme.withAlpha(Theme.getSecondaryText(), f5));
        if (flag2) {
            MatrixUtil.pop(drawContext);
        }
    }

    @Override
    public void setChevronRotation(float f) {
        this.chevronRotation = f;
    }

    @Override
    public boolean mouseClicked(double d, double d2) {
        return false;
    }

    @Override
    public List<String> getOptionNames() {
        ArrayList arrayList = new ArrayList(this.values.length);

        for (Object object : this.values) {
            arrayList.add(this.nameFormatter.apply((E)object));
        }

        return arrayList;
    }

    @Override
    public int getSelectedIndex() {
        Object object = this.option.getValue();

        for (int i = 0; i < this.values.length; i++) {
            if (this.values[i].equals(object)) {
                return i;
            }
        }

        return 0;
    }

    @Override
    public void selectIndex(int i) {
        if (i >= 0 && i < this.values.length) {
            this.option.setValue(this.values[i]);
        }
    }

    @Override
    public float getX() {
        return this.x;
    }

    @Override
    public float getY() {
        return this.y;
    }

    @Override
    public float getWidth() {
        return this.width;
    }

    @Override
    public float getHeight() {
        return this.height;
    }

    @Override
    public boolean isHeaderHovered(double d, double d2) {
        return this.isHovered(d, d2, this.x, this.y, this.width, this.height);
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
        return 100.0F;
    }

    @Override
    public void reset() {
        OptionsUtil.resetToDefault(this.option);
    }

    @Override
    public boolean isFocused() {
        return !OptionsUtil.isDefault(this.option);
    }

    @Override
    public boolean keyPressed(int i) {
        Object object = this.option.getValue();
        int i2 = 0;

        for (int i3 = 0; i3 < this.values.length; i3++) {
            if (this.values[i3].equals(object)) {
                i2 = i3;
                break;
            }
        }

        int i4 = this.values.length;
        int i5 = i < 0 ? (i2 - 1 + i4) % i4 : (i2 + 1) % i4;
        this.option.setValue(this.values[i5]);
        return true;
    }

    @Override
    public String getText() {
        Object object = this.option.getValue();
        return object instanceof Enum ? ((Enum)object).name() : String.valueOf(object);
    }

    @Override
    public void setText(String string) {
        for (Object object : this.values) {
            String string2 = object instanceof Enum ? ((Enum)object).name() : String.valueOf(object);
            if (string2.equals(string)) {
                this.option.setValue((E)object);
                return;
            }
        }
    }
}
