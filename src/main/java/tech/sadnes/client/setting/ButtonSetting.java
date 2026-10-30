package tech.sadnes.client.setting;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class ButtonSetting extends Setting<Runnable> {
    private static final float BUTTON_WIDTH = 28.0F;
    private static final float BUTTON_HEIGHT = 10.0F;
    private final Animation hoverAnimation2 = new Animation();
    private final String buttonLabelKey;

    public ButtonSetting(String string, Runnable runnable) {
        this(string, "lua.button.run", runnable);
    }

    public ButtonSetting(String string, String string2, Runnable runnable) {
        super(string, runnable);
        this.buttonLabelKey = string2;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.position.x;
        float f3 = this.position.y;
        float f4 = this.size.x;
        float f5 = this.size.y;
        boolean flag = this.isHovered(i, i2, this.position, this.size);
        this.hoverAnimation2.update();
        this.hoverAnimation2.animate(flag ? 1.0 : 0.0, 0.25, Easings.EXPO_OUT);
        float f6 = this.hoverAnimation2.get();
        this.drawLabel(drawContext, f2, f3, f4, f5, f4 - 28.0F - 6.0F, flag);
        float f7 = f2 + f4 - 28.0F;
        float f8 = f3 + f5 / 2.0F - 5.0F;
        int i3 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getSurfaceDeep(), Theme.getHighlight(), f6 * 0.45F), this.getAlpha());
        int i4 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), f6), this.getAlpha());
        new RenderUtil(drawContext, f7, f8, 28.0F, 10.0F)
            .rect(i3, Radius.all(4.0F))
            .textCenter(Fonts.INTER_MEDIUM, Language.translate(this.buttonLabelKey, "Run"), i4, 5.5F, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (!this.isLeftButton(click.button())) {
            return false;
        }

        if (!this.isHovered(click.x(), click.y(), this.position, this.size)) {
            return false;
        }

        Runnable runnable = this.getValue();
        if (runnable != null) {
            runnable.run();
        }

        return true;
    }
}
