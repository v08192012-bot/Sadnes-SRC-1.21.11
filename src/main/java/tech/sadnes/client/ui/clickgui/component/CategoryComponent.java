package tech.sadnes.client.ui.clickgui.component;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.CategoryDescriptor;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.ui.clickgui.CategorySidebar;
import tech.sadnes.client.util.color.Theme;

public class CategoryComponent extends Component {
    private final CategoryDescriptor category;
    private final HoverTooltip tooltip = new HoverTooltip();

    public CategoryComponent(CategoryDescriptor categoryDescriptor) {
        this.category = categoryDescriptor;
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
        CategorySidebar categorySidebar = this.getClickGui().getSidebar();
        boolean flag = categorySidebar.getSelectedHubCategory() == null && categorySidebar.getSelectedCategory() == this.category;
        boolean flag2 = this.isHovered(i, i2, this.position.x, this.position.y, this.size.x, this.size.y);
        this.stateAnimation.animate(flag ? 1.0 : 0.0, 0.5, flag ? Easings.QUART_OUT : Easings.QUAD_IN);
        this.hoverAnimation.animate(flag2 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f2 = Math.max(this.stateAnimation.get(), this.hoverAnimation.get() * 0.5F);
        GuiRenderUtil.drawHighlight(drawContext, this.position.x, this.position.y, this.size.x, this.size.y, f2 * this.getAlpha(), Radius.all(6.0F));
        int i3 = Theme.withAlpha(Theme.getPrimary(), (0.6F + 0.4F * this.stateAnimation.get()) * this.getAlpha());
        int i4 = this.getPrimaryColor(0.5F + 0.5F * this.stateAnimation.get());
        new RenderUtil(drawContext, this.position.x, this.position.y, this.size.x, this.size.y)
            .icon(this.category.getIcon(), 4.0F, 4.5F, 8.0F, i3)
            .textTopLeft(Fonts.INTER_MEDIUM, this.category.getName(), 16.0F, 5.5F, i4, 6.5F);
        if (flag2) {
            this.tooltip.markHovered();
        }

        this.tooltip.update(this.category.getDescription());
    }

    public void renderTooltip(DrawContext drawContext) {
        this.tooltip.render(drawContext, this.position.x, this.position.y, this.size.x, this.size.y, HoverTooltip.Side.LEFT, this.getAlpha());
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (this.isHovered(click.x(), click.y(), this.position.x, this.position.y, this.size.x, this.size.y)) {
            this.getClickGui().getSidebar().selectCategory(this.category);
        }

        return false;
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

    public CategoryDescriptor getCategory() {
        return this.category;
    }

    public HoverTooltip getTooltip() {
        return this.tooltip;
    }
}
