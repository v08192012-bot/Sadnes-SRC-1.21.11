package tech.sadnes.client.ui.clickgui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.CategoryDescriptor;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.VerticalFadeMask;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.script.ScriptCategory;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.CategoryComponent;
import tech.sadnes.client.ui.clickgui.component.HoverTooltip;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.hub.HubCategory;
import tech.sadnes.client.ui.hub.SubCategory;
import tech.sadnes.client.ui.hub.UtilitiesSubCategory;
import tech.sadnes.client.ui.hub.VisualsSubCategory;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class CategorySidebar implements IRenderable {
    private static final float BUTTON_SPACING = 3.0F;
    private static final float SECTION_HEADER_HEIGHT = 11.0F;
    private static final float FADE_HEIGHT = 14.0F;
    private static final float SUB_ENTRY_HEIGHT = 14.0F;
    private final SmoothScrollbar scroll = new SmoothScrollbar();
    private float contentHeight;
    private float viewY;
    private float viewHeight;
    private final ClickGuiScreen clickGui;
    private final List<CategoryComponent> categoryComponents = new ArrayList<>();
    private final List<CategoryComponent> scriptCategoryComponents = new ArrayList<>();
    private final List<CategorySidebar.CategoryButton> hubButtons = new ArrayList<>();
    private final CategorySidebar.SubCategoryList visualsSubCategories = new CategorySidebar.SubCategoryList(VisualsSubCategory.values());
    private final CategorySidebar.SubCategoryList utilitiesSubCategories = new CategorySidebar.SubCategoryList(UtilitiesSubCategory.values());
    private CategoryDescriptor selectedCategory = Category.COMBAT;
    HubCategory selectedHubCategory;
    private VisualsSubCategory selectedVisualsSubCategory = VisualsSubCategory.PLAYER;
    private UtilitiesSubCategory selectedUtilitiesSubCategory = UtilitiesSubCategory.AUTOBUY;

    public CategorySidebar(ClickGuiScreen clickGuiScreen) {
        this.clickGui = clickGuiScreen;

        for (Category category : Category.values()) {
            this.categoryComponents.add(new CategoryComponent(category));
        }

        for (HubCategory hubCategory : HubCategory.values()) {
            this.hubButtons.add(new CategorySidebar.CategoryButton(hubCategory));
        }
    }

    public void selectCategory(CategoryDescriptor categoryDescriptor) {
        if (categoryDescriptor != null && (categoryDescriptor != this.selectedCategory || this.selectedHubCategory != null)) {
            CategoryDescriptor categoryDescriptor2 = this.selectedCategory;
            this.selectedCategory = categoryDescriptor;
            this.selectedHubCategory = null;
            this.clickGui.onCategoryChanged(categoryDescriptor2, categoryDescriptor);
        }
    }

    public void setScriptCategories(List<ScriptCategory> scriptCategories) {
        this.scriptCategoryComponents.clear();

        for (ScriptCategory scriptCategory : scriptCategories) {
            this.scriptCategoryComponents.add(new CategoryComponent(scriptCategory));
        }

        if (this.selectedCategory instanceof ScriptCategory && !scriptCategories.contains(this.selectedCategory)) {
            this.selectedCategory = Category.COMBAT;
        }
    }

    public void selectHubCategory(HubCategory hubCategory) {
        if (hubCategory != null && hubCategory != this.selectedHubCategory) {
            this.selectedHubCategory = hubCategory;
            this.clickGui.onHubCategoryChanged();
        }
    }

    public void selectVisualsSubCategory(VisualsSubCategory visualsSubCategory) {
        if (visualsSubCategory != this.selectedVisualsSubCategory) {
            this.selectedVisualsSubCategory = visualsSubCategory;
            this.clickGui.resetScroll();
        }
    }

    public void selectUtilitiesSubCategory(UtilitiesSubCategory utilitiesSubCategory) {
        if (utilitiesSubCategory != this.selectedUtilitiesSubCategory) {
            this.selectedUtilitiesSubCategory = utilitiesSubCategory;
            this.clickGui.onHubCategoryChanged();
        }
    }

    public void resize(int i, int i2) {
        Iterator iterator2 = this.categoryComponents.iterator();

        while (iterator2.hasNext()) {
            ((CategoryComponent)iterator2.next()).resize(i, i2);
        }
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5, float f6) {
        this.visualsSubCategories.setExpanded(this.selectedCategory == Category.VISUALS && this.selectedHubCategory == null);
        this.utilitiesSubCategories.setExpanded(this.selectedHubCategory == HubCategory.AUCTION);
        float f7 = this.getAlpha();
        int i3 = Theme.withAlpha(Theme.getSecondaryText(), 0.65F * f7);
        this.scroll.update(this.contentHeight, f6);
        this.viewY = f3;
        this.viewHeight = f6;
        this.beginFadeMask(f3, f6);
        float f8 = f3 + this.scroll.getScroll();
        float f9 = this.renderSectionHeader(drawContext, Language.translate("category.section.main", "Main"), f2, f8, f4, i3);

        for (CategoryComponent categoryComponent : this.categoryComponents) {
            categoryComponent.position.set(f2, f8 + f9);
            categoryComponent.size.set(f4, f5);
            categoryComponent.render(drawContext, i, i2, f);
            f9 += f5 + 3.0F;
            if (categoryComponent.getCategory() == Category.VISUALS) {
                f9 += this.visualsSubCategories.render(drawContext, i, i2, f2, f8 + f9, f4, f7, this.selectedVisualsSubCategory.ordinal());
            }
        }

        if (!this.scriptCategoryComponents.isEmpty()) {
            f9 += this.renderSectionHeader(drawContext, Language.translate("category.section.scripts", "Scripts"), f2, f8 + f9, f4, i3);

            for (CategoryComponent categoryComponent2 : this.scriptCategoryComponents) {
                categoryComponent2.position.set(f2, f8 + f9);
                categoryComponent2.size.set(f4, f5);
                categoryComponent2.render(drawContext, i, i2, f);
                f9 += f5 + 3.0F;
            }
        }

        float f10 = f9 + this.renderSectionHeader(drawContext, Language.translate("category.section.misc", "Misc"), f2, f8 + f9, f4, i3);

        for (CategorySidebar.CategoryButton categoryButton : this.hubButtons) {
            categoryButton.render(drawContext, i, i2, f2, f8 + f10, f4, f5, f7);
            f10 += f5 + 3.0F;
            if (categoryButton.category == HubCategory.AUCTION) {
                f10 += this.utilitiesSubCategories.render(drawContext, i, i2, f2, f8 + f10, f4, f7, this.selectedUtilitiesSubCategory.ordinal());
            }
        }

        this.contentHeight = f10;
        VerticalFadeMask.end();
    }

    private void beginFadeMask(float f, float f2) {
        float f3 = this.contentHeight - f2;
        if (!(f3 <= 0.5F) && !(f2 <= 28.0F)) {
            float f4 = this.scroll.getScroll();
            float f5 = Math.min(1.0F, Math.max(0.0F, -f4 / 14.0F));
            float f6 = Math.min(1.0F, Math.max(0.0F, (f3 + f4) / 14.0F));
            if (!(f5 <= 0.001F) || !(f6 <= 0.001F)) {
                VerticalFadeMask.begin(f, f + 14.0F, f5, f + f2 - 14.0F, f + f2, f6);
            }
        }
    }

    public boolean mouseScrolled(double d, double d2, double d3) {
        if (this.contentHeight <= this.viewHeight) {
            return false;
        }

        this.scroll.onScroll(d3);
        return true;
    }

    public void mouseReleased() {
        this.scroll.mouseReleased();
    }

    public void renderTooltips(DrawContext drawContext) {
        Iterator iterator2 = this.categoryComponents.iterator();

        while (iterator2.hasNext()) {
            ((CategoryComponent)iterator2.next()).renderTooltip(drawContext);
        }

        Iterator iterator3 = this.scriptCategoryComponents.iterator();

        while (iterator3.hasNext()) {
            ((CategoryComponent)iterator3.next()).renderTooltip(drawContext);
        }

        Iterator iterator4 = this.hubButtons.iterator();

        while (iterator4.hasNext()) {
            ((CategorySidebar.CategoryButton)iterator4.next()).renderTooltip(drawContext);
        }
    }

    private float renderSectionHeader(DrawContext drawContext, String string, float f, float f2, float f3, int i) {
        new RenderUtil(drawContext, f, f2 + 2.0F, f3, 7.0F).textBottomLeft(Fonts.INTER_REGULAR, string, 2.0F, 0.0F, i, 6.5F);
        return 11.0F;
    }

    public boolean mouseClicked(Click click, boolean flag) {
        int i = this.visualsSubCategories.getHoveredIndex(click.x(), click.y());
        if (i >= 0) {
            this.selectVisualsSubCategory(VisualsSubCategory.values()[i]);
            return true;
        }

        int i2 = this.utilitiesSubCategories.getHoveredIndex(click.x(), click.y());
        if (i2 >= 0) {
            this.selectUtilitiesSubCategory(UtilitiesSubCategory.values()[i2]);
            return true;
        }

        for (CategoryComponent categoryComponent : this.categoryComponents) {
            if (categoryComponent.mouseClicked(click, flag)) {
                return true;
            }
        }

        for (CategoryComponent categoryComponent2 : this.scriptCategoryComponents) {
            if (categoryComponent2.mouseClicked(click, flag)) {
                return true;
            }
        }

        for (CategorySidebar.CategoryButton categoryButton : this.hubButtons) {
            if (categoryButton.mouseClicked(click)) {
                return true;
            }
        }

        return false;
    }

    public boolean isMouseOver(double d, double d2) {
        for (CategoryComponent categoryComponent : this.categoryComponents) {
            if (this.isHovered(d, d2, categoryComponent.position, categoryComponent.size)) {
                return true;
            }
        }

        for (CategoryComponent categoryComponent2 : this.scriptCategoryComponents) {
            if (this.isHovered(d, d2, categoryComponent2.position, categoryComponent2.size)) {
                return true;
            }
        }

        for (CategorySidebar.CategoryButton categoryButton : this.hubButtons) {
            if (categoryButton.isHovered(d, d2)) {
                return true;
            }
        }

        return this.visualsSubCategories.getHoveredIndex(d, d2) >= 0 || this.utilitiesSubCategories.getHoveredIndex(d, d2) >= 0;
    }

    float getAlpha() {
        return (float)Math.pow(this.clickGui.getOpenAnimation().get(), 3.0);
    }

    public CategoryDescriptor getSelectedCategory() {
        return this.selectedCategory;
    }

    public HubCategory getSelectedHubCategory() {
        return this.selectedHubCategory;
    }

    public VisualsSubCategory getSelectedVisualsSubCategory() {
        return this.selectedVisualsSubCategory;
    }

    public UtilitiesSubCategory getSelectedUtilitiesSubCategory() {
        return this.selectedUtilitiesSubCategory;
    }

    final class CategoryButton {
        final HubCategory category;
        private final HoverTooltip tooltip = new HoverTooltip();
        private final Animation selectAnimation = new Animation();
        private final Animation hoverAnimation = new Animation();
        private float x;
        private float y;
        private float width;
        private float height;

        CategoryButton(HubCategory hubCategory) {
            this.category = hubCategory;
        }

        void render(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
            this.x = f;
            this.y = f2;
            this.width = f3;
            this.height = f4;
            boolean flag = CategorySidebar.this.selectedHubCategory == this.category;
            boolean flag2 = this.isHovered(i, i2);
            this.selectAnimation.update();
            this.selectAnimation.animate(flag ? 1.0 : 0.0, 0.5, flag ? Easings.QUART_OUT : Easings.QUAD_IN);
            this.hoverAnimation.update();
            this.hoverAnimation.animate(flag2 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
            float f6 = Math.max(this.selectAnimation.get(), this.hoverAnimation.get() * 0.5F);
            GuiRenderUtil.drawHighlight(drawContext, f, f2, f3, f4, f6 * f5, Radius.all(6.0F));
            int i3 = Theme.withAlpha(Theme.getPrimary(), (0.6F + 0.4F * this.selectAnimation.get()) * f5);
            int i4 = ColorUtil.multiplyAlpha(Theme.getTextColor(), (0.5F + 0.5F * this.selectAnimation.get()) * f5);
            new RenderUtil(drawContext, f, f2, f3, f4)
                .icon(this.category.getIcon(), 4.0F, 4.5F, 8.0F, i3)
                .textTopLeft(Fonts.INTER_MEDIUM, this.category.getDisplayName(), 16.0F, 5.5F, i4, 6.5F);
            if (flag2) {
                this.tooltip.markHovered();
            }

            this.tooltip.update(this.category.getDescription());
        }

        void renderTooltip(DrawContext drawContext) {
            this.tooltip.render(drawContext, this.x, this.y, this.width, this.height, HoverTooltip.Side.LEFT, CategorySidebar.this.getAlpha());
        }

        boolean mouseClicked(Click click) {
            if (!this.isHovered(click.x(), click.y())) {
                return false;
            }

            CategorySidebar.this.selectHubCategory(this.category);
            return true;
        }

        boolean isHovered(double d, double d2) {
            return CategorySidebar.this.isHovered(d, d2, this.x, this.y, this.width, this.height);
        }
    }

    final class SubCategoryList {
        private final SubCategory[] entries;
        private final Animation expandAnimation = new Animation();
        private final Animation[] selectAnimations;
        private final Animation[] hoverAnimations;
        private final float[] entryY;
        private float x;
        private float width;

        SubCategoryList(SubCategory[] subCategories) {
            this.entries = subCategories;
            this.selectAnimations = new Animation[subCategories.length];
            this.hoverAnimations = new Animation[subCategories.length];
            this.entryY = new float[subCategories.length];
            int i = 0;

            for (int i2 = subCategories.length; i < i2; i++) {
                this.selectAnimations[i] = new Animation();
                this.hoverAnimations[i] = new Animation();
            }
        }

        void setExpanded(boolean flag) {
            this.expandAnimation.update();
            double d = flag ? 1.0 : 0.0;
            if (flag) {
                this.expandAnimation.animate(d, 0.3F, Easings.QUART_OUT);
            } else {
                this.expandAnimation.animate(d, 0.3F, Easings.QUAD_IN);
            }
        }

        float render(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, int i3) {
            float f5 = this.expandAnimation.get();
            if (f5 <= 0.001F) {
                return 0.0F;
            }

            this.x = f + 10.0F;
            this.width = f3 - 10.0F;
            float f6 = 17.0F * f5;
            float f7 = f5 * f4;
            int i4 = 0;

            for (int i5 = this.entries.length; i4 < i5; i4++) {
                this.entryY[i4] = f2 + f6 * i4;
                this.renderEntry(drawContext, i, i2, i4, f7, i4 == i3);
            }

            return f6 * this.entries.length;
        }

        private void renderEntry(DrawContext drawContext, int i, int i2, int i3, float f, boolean flag) {
            Animation animation = this.selectAnimations[i3];
            Animation animation2 = this.hoverAnimations[i3];
            float f2 = this.entryY[i3];
            animation.update();
            animation.animate(flag ? 1.0 : 0.0, 0.3F, flag ? Easings.QUART_OUT : Easings.QUAD_IN);
            animation2.update();
            animation2.animate(CategorySidebar.this.isHovered(i, i2, this.x, f2, this.width, 14.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
            float f3 = Math.max(animation.get(), animation2.get() * 0.5F);
            GuiRenderUtil.drawHighlight(drawContext, this.x, f2, this.width, 14.0F, f3 * f, Radius.all(5.0F));
            SubCategory subCategory = this.entries[i3];
            int i4 = Theme.withAlpha(Theme.getPrimary(), (0.6F + 0.4F * animation.get()) * f);
            new RenderUtil(drawContext, this.x, f2, this.width, 14.0F)
                .icon(subCategory.getIcon(), 3.0F, 3.0F, 8.0F, i4)
                .textTopLeft(Fonts.INTER_MEDIUM, subCategory.getDisplayName(), 14.0F, 4.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f), 6.5F);
        }

        int getHoveredIndex(double d, double d2) {
            if (this.expandAnimation.get() <= 0.1F) {
                return -1;
            }

            int i = 0;

            for (int i2 = this.entries.length; i < i2; i++) {
                if (CategorySidebar.this.isHovered(d, d2, this.x, this.entryY[i], this.width, 14.0)) {
                    return i;
                }
            }

            return -1;
        }
    }
}
