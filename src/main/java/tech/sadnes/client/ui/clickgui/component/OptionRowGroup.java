package tech.sadnes.client.ui.clickgui.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class OptionRowGroup implements IInteractable, IRenderable {
    private static final float CORNER_RADIUS = 8.0F;
    private static final float DIVIDER_THICKNESS = 0.5F;
    private static final float DIVIDER_INSET = 8.0F;
    private static final float TITLE_SIZE = 6.5F;
    private static final float TITLE_OFFSET = 2.0F;
    private static final float TITLE_HEIGHT = 8.5F;
    private final Subcategory source;
    private final String title;
    private final List<ModuleRowComponent> rows = new ArrayList<>();
    private float x;
    private float y;
    private float width;

    public OptionRowGroup(Subcategory subcategory) {
        this(subcategory, subcategory == null ? "" : subcategory.getDisplayName());
    }

    public OptionRowGroup(Subcategory subcategory, String string) {
        this.source = subcategory;
        this.title = string == null ? "" : string;
    }

    public void addRow(ModuleRowComponent moduleRowComponent) {
        this.rows.add(moduleRowComponent);
    }

    public float getHeight() {
        return this.getTitleHeight() + this.rows.size() * 22.0F;
    }

    public float getTitleHeight() {
        return this.hasTitle() ? 8.5F : 0.0F;
    }

    private boolean hasTitle() {
        return !this.title.isEmpty();
    }

    public void resetRows() {
        Iterator iterator2 = this.rows.iterator();

        while (iterator2.hasNext()) {
            ((ModuleRowComponent)iterator2.next()).reset();
        }
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, int i, int i2, float f4) {
        this.x = f;
        this.y = f2;
        this.width = f3;
        float f5 = this.getGuiAlpha();
        float f6 = this.getTitleHeight();
        float f7 = f2 + f6;
        float f8 = this.getHeight() - f6;
        if (this.hasTitle()) {
            int i3 = Theme.withAlpha(Theme.getSecondaryText(), 0.65F * f5);
            new RenderUtil(drawContext, f, f2, f3, 6.5F).textTopLeft(Fonts.INTER_MEDIUM, this.title, 2.0F, 0.0F, i3, 6.5F);
        }

        GuiRenderUtil.drawOutlined(drawContext, f, f7, f3, f8, f5 * 0.75F, 8.0F);
        int i4 = this.rows.size();
        int i5 = i4 - 1;

        for (int i6 = 0; i6 < i4; i6++) {
            float f9 = f7 + i6 * 22.0F;
            this.rows.get(i6).render(drawContext, f, f9, f3, i, i2, f4, this.rowRadius(i6 == 0, i6 == i5));
        }

        for (int i7 = 1; i7 < i4; i7++) {
            float f10 = f7 + i7 * 22.0F - 0.25F;
            GuiRenderUtil.drawOverlay(drawContext, f + 8.0F, MathUtil.roundToStep(f10, 0.5F), f3 - 16.0F, 0.5F, f5);
        }
    }

    private Radius rowRadius(boolean flag, boolean flag2) {
        if (flag && flag2) {
            return Radius.all(8.0F);
        } else if (flag) {
            return Radius.of(0.0F, 8.0F, 0.0F, 8.0F);
        } else {
            return flag2 ? Radius.of(8.0F, 0.0F, 8.0F, 0.0F) : Radius.zero();
        }
    }

    public boolean isMouseOver(double d, double d2) {
        float f = this.getTitleHeight();
        return this.isHovered(d, d2, this.x, this.y + f, this.width, this.getHeight() - f);
    }

    public boolean mouseClicked(Click click, boolean flag) {
        if (!this.isMouseOver(click.x(), click.y())) {
            return false;
        }

        for (ModuleRowComponent moduleRowComponent : this.rows) {
            if (moduleRowComponent.handleMouseClick(click, flag)) {
                return true;
            }
        }

        return false;
    }

    private float getGuiAlpha() {
        ClickGuiScreen clickGuiScreen = Sadnes.getInstance().getClickGui();
        return clickGuiScreen == null ? 1.0F : (float)Math.pow(clickGuiScreen.getOpenAnimation().get(), 3.0) * clickGuiScreen.getCategorySwitchAnimation().get();
    }

    public Subcategory getSource() {
        return this.source;
    }

    public String getTitle() {
        return this.title;
    }

    public List<ModuleRowComponent> getRows() {
        return this.rows;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getWidth() {
        return this.width;
    }
}
