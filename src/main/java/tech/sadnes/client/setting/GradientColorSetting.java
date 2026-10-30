package tech.sadnes.client.setting;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public class GradientColorSetting extends ColorSetting {
    private static final float SWATCH_SIZE2 = 9.0F;
    private static final float SWATCH_GAP = 8.0F;
    private static final float SWATCH_PADDING = 5.0F;
    private final Animation[] selectAnimations = new Animation[]{new Animation(), new Animation()};
    private int secondColor;
    private int selectedIndex;

    public GradientColorSetting(String string, int i, int i2) {
        super(string, null, false, i);
        this.secondColor = i2 | 0xFF000000;
    }

    public int getFirstColor() {
        return super.getValue();
    }

    public int getSecondColor() {
        return this.secondColor;
    }

    public GradientColorSetting setColors(int i, int i2) {
        this.secondColor = i2 | 0xFF000000;
        super.setValue(i);
        return this;
    }

    public Integer getValue() {
        return this.selectedIndex == 0 ? super.getValue() : this.secondColor;
    }

    public GradientColorSetting setValue(Integer integer) {
        if (this.selectedIndex == 0) {
            super.setValue(integer);
            return this;
        } else {
            this.secondColor = this.sanitizeValue(integer);
            this.notifyChange();
            return this;
        }
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        if (this.editingBind != null) {
            super.render(drawContext, i, i2, f);
        } else {
            float f2 = this.position.x;
            float f3 = this.position.y;
            float f4 = this.size.x;
            float f5 = this.size.y;
            this.drawLabel(drawContext, f2, f3, f4, f5, f4 - 10.0F - 6.0F, this.isHovered(i, i2, this.position, this.size));
            float f6 = f2 + f4 - 10.0F + 1.2F;
            float f7 = f3 + f5 / 2.0F - 5.0F + 1.2F;
            new RenderUtil(drawContext, f6, f7, 7.6F, 7.6F)
                .rect(this.getFirstColor(), this.getFirstColor(), this.secondColor, this.secondColor, this.getAlpha(), Radius.all(3.0F));
        }
    }

    @Override
    protected float getExtraHeaderHeight() {
        return 14.0F;
    }

    @Override
    protected void renderExtraHeader(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        float f5 = f + (f3 - 18.0F - 8.0F) / 2.0F;
        Radius radius = Radius.all(3.0F);

        for (int i3 = 0; i3 < 2; i3++) {
            float f6 = f5 + i3 * 17.0F;
            this.selectAnimations[i3].update();
            this.selectAnimations[i3].animate(this.selectedIndex == i3 ? 1.0 : 0.0, 0.2F, Easings.QUAD_OUT);
            int i4 = i3 == 0 ? this.getFirstColor() : this.secondColor;
            new RenderUtil(drawContext, f6, f2, 9.0F, 9.0F)
                .rect(ColorUtil.multiplyAlpha(i4, f4), radius)
                .shadow(0.5F, ColorUtil.multiplyAlpha(Theme.getOutline(), 0.4F * f4), radius);
            float f7 = this.selectAnimations[i3].get();
            if (!(f7 <= 0.001F)) {
                new RenderUtil(drawContext, f6 - 1.0F, f2 - 1.0F, 11.0F, 11.0F)
                    .shadow(1.0F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f7 * f4), Radius.all(4.0F));
            }
        }
    }

    @Override
    protected boolean handleExtraHeaderClick(Click click, float f, float f2, float f3) {
        float f4 = f + (f3 - 18.0F - 8.0F) / 2.0F;

        for (int i = 0; i < 2; i++) {
            float f5 = f4 + i * 17.0F;
            if (this.isHovered(click.x(), click.y(), f5 - 1.0F, f2 - 1.0F, 11.0, 11.0)) {
                if (this.selectedIndex != i) {
                    this.selectedIndex = i;
                    this.syncPickerFromValue();
                }

                return true;
            }
        }

        return false;
    }
}
