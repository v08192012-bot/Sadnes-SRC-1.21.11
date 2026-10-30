package tech.sadnes.client.ui.clickgui.component;

import java.util.Locale;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.impl.render.esp.EspTargetType;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public class EspCategoryTab extends Component {
    private final EspTargetType category;

    public EspCategoryTab(EspTargetType espTargetType) {
        this.category = espTargetType;
    }

    @Override
    public void init(int i, int i2) {
    }

    @Override
    public void resize(int i, int i2) {
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        this.stateAnimation.update();
        this.hoverAnimation.update();
        boolean flag = this.getClickGui().getEspEditorPanel().getCurrentCategory() == this.category;
        this.stateAnimation.animate(flag ? 1.0 : 0.0, 0.25, flag ? Easings.QUART_OUT : Easings.QUAD_IN);
        boolean flag2 = this.isHovered(i, i2, this.position.x, this.position.y, this.size.x, this.size.y);
        this.hoverAnimation.animate(flag2 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f2 = 0.5F + 0.5F * this.stateAnimation.get();
        float f3 = (1.0F - this.stateAnimation.get()) * 0.2F * this.hoverAnimation.get();
        float f4 = f2 + f3;
        int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f4 * this.getAlpha());
        String string = this.getLabel();
        float f5 = this.getFontSize();
        float f6 = Fonts.INTER_MEDIUM.getWidth(string, f5);
        float f7 = (this.size.x - f6) / 2.0F;
        new RenderUtil(drawContext, this.position.x, this.position.y, this.size.x, this.size.y).textMiddleLeft(Fonts.INTER_MEDIUM, string, f7, 0.0F, i3, f5);
    }

    public String getLabel() {
        return Language.translate("esp.category." + this.category.name().toLowerCase(Locale.ROOT), this.category.getDisplayName());
    }

    public float getFontSize() {
        return 5.5F + 1.0F * this.stateAnimation.get();
    }

    public float getLabelWidth() {
        return Fonts.INTER_MEDIUM.getWidth(this.getLabel(), this.getFontSize());
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (!this.isLeftButton(click.button())) {
            return false;
        }

        if (!this.isHovered(click.x(), click.y(), this.position.x, this.position.y, this.size.x, this.size.y)) {
            return false;
        }

        this.getClickGui().getEspEditorPanel().setCategory(this.category);
        return true;
    }

    @Override
    public boolean onMouseRelease(Click click) {
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        return false;
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        return false;
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        return false;
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public void removed() {
    }

    public EspTargetType getCategory() {
        return this.category;
    }
}
