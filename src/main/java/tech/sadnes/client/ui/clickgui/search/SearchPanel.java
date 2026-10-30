package tech.sadnes.client.ui.clickgui.search;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Module;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.CategorySidebar;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.clickgui.component.ModuleRowComponent;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;
import tech.sadnes.client.ui.hub.VisualsSubCategory;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class SearchPanel implements IInteractable, IRenderable {
    private static final float GROUP_HEADER_HEIGHT = 11.0F;
    private static final float GROUP_SPACING = 6.0F;
    private static final float ROW_HEIGHT = 22.0F;
    private static final float EMPTY_HEIGHT = 22.0F;
    private static final float CLOSE_ICON_SIZE = 10.0F;
    private static final float LINK_ICON_SIZE = 7.5F;
    private static final float LIST_PADDING = 4.0F;
    private static final float LINK_BUTTON_PADDING = 4.0F;
    private static final float LINK_BUTTON_SIZE = 10.0F;
    private static final float CORNER_RADIUS = 6.0F;
    private static final float CLOSE_ICON_MARGIN = 8.0F;
    private static final float MODULE_ROW_HEIGHT = 22.0F;
    private final ClickGuiScreen clickGui;
    private final Animation openAnimation = new Animation();
    private final Vector2f inputPosition = new Vector2f();
    private final TextFieldComponent searchField = new TextFieldComponent(UI_FONT, 6.5F, "search.placeholder", false, false);
    private final SmoothScrollbar scroll = new SmoothScrollbar();
    private final SearchIndex searchIndex = new SearchIndex();
    private final List<SearchPanel.RowBounds> visibleRows = new ArrayList<>();
    private final List<SearchPanel.ResultGroup> groups = new ArrayList<>();
    private final Map<Module, ModuleRowComponent> moduleComponents = new IdentityHashMap<>();
    private final Map<Setting<?>, Animation> linkHoverAnimations = new IdentityHashMap<>();
    private boolean open;
    private float barX;
    private float barY;
    private float barWidth;
    private float barHeight;
    private float closeIconX;
    private float closeIconY;
    private float x;
    private float y;
    private float width;
    private float height;
    private float dropdownHeight;
    private float listX;
    private float listY;
    private float listWidth;
    private float listHeight;
    private float contentHeight;
    private String lastQuery = "";
    private List<SearchResult> results;
    private SearchPanel.NavigationRequest pendingNavigation;

    public SearchPanel(ClickGuiScreen clickGuiScreen) {
        this.clickGui = clickGuiScreen;
    }

    public void init(int i, int i2) {
        this.open = false;
        this.openAnimation.forceValue(0.0);
        this.searchField.setText("");
        this.searchField.setCursor(0);
        this.searchField.setFocused(false);
        this.searchField.setReplaceOnInput(false);
        this.scroll.reset();
        this.results = null;
        this.lastQuery = "";
        this.contentHeight = 0.0F;
        this.pendingNavigation = null;
        this.visibleRows.clear();
        this.groups.clear();
        this.moduleComponents.values().forEach(ModuleRowComponent::reset);
    }

    public void tick() {
        this.openAnimation.update();
        this.openAnimation.animate(this.open ? 1.0 : 0.0, 0.22F, Easings.QUART_OUT);
        if (!this.open && this.openAnimation.get() <= 0.001F) {
            this.contentHeight = 0.0F;
        } else {
            Iterator iterator2 = this.moduleComponents.values().iterator();

            while (iterator2.hasNext()) {
                ((ModuleRowComponent)iterator2.next()).update();
            }

            String string = this.searchField.getText();
            String string2 = string == null ? "" : string;
            if (this.results == null || !string2.equals(this.lastQuery)) {
                this.lastQuery = string2;
                this.results = this.searchIndex.search(string2);
                this.rebuildGroups();
            }

            this.contentHeight = this.computeContentHeight();
        }
    }

    public void renderSearchBar(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
        this.dropdownHeight = Math.min(this.contentHeight + 4.0F, f5);
        float f6 = this.openAnimation.get();
        GuiRenderUtil.drawOverlay(drawContext, f, MathUtil.roundToStep(f2 + f4, 0.5F), f3, 0.5F, this.clickGui.getOpenAnimation().get());
        RenderUtil renderUtil = new RenderUtil(
            drawContext,
            MathUtil.roundToStep(f, 0.5F),
            MathUtil.roundToStep(f2, 0.5F),
            MathUtil.roundToStep(f3, 0.5F),
            MathUtil.roundToStep(f4 + this.dropdownHeight * f6, 0.5F)
        );
        int i = Theme.getBase();
        Theme.getNearBackground();
        float f7 = Theme.getShadowStrength(this.clickGui.getOpenAnimation().get() * f6);
        renderUtil.blurGlowBorder(
            this.clickGui.getOpenAnimation().get() * f6,
            i,
            0.28F,
            0,
            this.clickGui.getOpenAnimation().get() * f6 * 2.0F,
            f7,
            Easings.QUAD_IN,
            Radius.all(f4 / 3.0F),
            RenderUtil.BlurQuality.LOW,
            true
        );
        this.barX = this.lerp(f + f3 - f4, f, f6);
        this.barY = f2;
        this.barWidth = this.lerp(f4, f3, f6);
        this.barHeight = f4;
        new RenderUtil(drawContext, this.barX, this.barY, this.barWidth, this.barHeight)
            .icon(Icon.SEARCH, f4 / 2.0F - 12.0F / 2.0F, this.barHeight / 2.0F - 12.0F / 2.0F, 12.0F, Theme.withAlpha(Theme.getPrimary(), this.getGuiAlpha()));
        if (f6 > 0.001F) {
            this.closeIconX = f + f3 - f4 + (f4 - 10.0F) / 2.0F;
            this.closeIconY = f2 + (f4 - 10.0F) / 2.0F;
            new RenderUtil(drawContext, this.closeIconX, this.closeIconY, 10.0F, 10.0F)
                .icon(Icon.CLOSE, 0.0F, 0.0F, 10.0F, Theme.withAlpha(Theme.getSecondaryText(), this.getGuiAlpha() * f6));
            float f8 = this.barX + f4 + 1.5F;
            float f9 = this.barWidth - f4 - 10.0F - 8.0F;
            if (f9 > 1.0F) {
                this.inputPosition.set(f8, this.barY + this.barHeight / 2.0F - 3.25F);
                this.searchField.render(drawContext, this.inputPosition, Theme.withAlpha(Theme.getTextColor(), this.getGuiAlpha() * f6), f9);
            }

            new RenderUtil(drawContext, f + 6.0F, f2 + f4, f3 - 12.0F, 0.5F)
                .rect(Theme.withAlpha(Theme.getSecondaryText(), 0.12F * this.getGuiAlpha() * f6), Radius.zero());
        }
    }

    public void renderResults(DrawContext drawContext, int i, int i2, float f) {
        this.visibleRows.clear();
        float f2 = this.openAnimation.get();
        if (!(f2 <= 0.001F)) {
            this.listX = this.x + 4.0F;
            this.listWidth = this.width - 8.0F;
            this.listY = this.y + this.height + 2.0F;
            float f3 = this.y + this.height + this.dropdownHeight * f2 - 4.0F;
            this.listHeight = Math.max(0.0F, f3 - this.listY);
            if (!(this.listHeight <= 0.5F)) {
                this.scroll.update(this.contentHeight, this.listHeight);
                this.scroll.updateHover(i, i2);
                float f4 = this.scroll.getScroll();
                this.scroll.render(drawContext, this.listX + this.listWidth + 1.5F, this.listY, this.listHeight, this.contentHeight, 0.0F);
                if (ScissorStack.push(drawContext, this.listX - 2.0F, this.listY - 2.0F, this.listWidth + 4.0F, this.listHeight + 4.0F)) {
                    try {
                        if (this.groups.isEmpty()) {
                            new RenderUtil(drawContext, this.listX, this.listY, this.listWidth, this.listHeight)
                                .textCenter(
                                    Fonts.INTER_MEDIUM,
                                    Language.translate("search.empty"),
                                    Theme.withAlpha(Theme.getSecondaryText(), this.getGuiAlpha()),
                                    6.5F,
                                    false
                                );
                            return;
                        }

                        float f5 = this.listY + f4;

                        for (int i3 = 0; i3 < this.groups.size(); i3++) {
                            if (i3 > 0) {
                                f5 += 6.0F;
                            }

                            SearchPanel.ResultGroup resultGroup = this.groups.get(i3);
                            if (f5 + 11.0F >= this.listY && f5 <= this.listY + this.listHeight) {
                                this.renderGroupHeader(drawContext, resultGroup.name(), this.listX, f5, this.listWidth);
                            }

                            float f6 = f5 + 11.0F;
                            f5 = this.renderGroup(drawContext, resultGroup, this.listX, f6, this.listWidth, i, i2, f);
                        }
                    } finally {
                        ScissorStack.pop(drawContext);
                    }
                }
            }
        }
    }

    private float renderGroup(DrawContext drawContext, SearchPanel.ResultGroup resultGroup, float f, float f2, float f3, int i, int i2, float f4) {
        float f5 = this.getGuiAlpha() * this.openAnimation.get();
        List list = resultGroup.rows();
        int i3 = list.size();
        float f6 = 0.0F;

        for (int i4 = 0; i4 < i3; i4++) {
            f6 += this.getRowHeight((SearchResult)list.get(i4));
        }

        boolean flag = f2 + f6 >= this.listY && f2 <= this.listY + this.listHeight;
        if (flag) {
            GuiRenderUtil.drawOutlined(drawContext, f, f2, f3, f6, f5 * 0.7F, 6.0F);
        }

        float f7 = f2;

        for (int i5 = 0; i5 < i3; i5++) {
            SearchResult searchResult = (SearchResult)list.get(i5);
            float f8 = this.getRowHeight(searchResult);
            if (f7 + f8 >= this.listY && f7 <= this.listY + this.listHeight) {
                Radius radius = this.getRowRadius(i5 == 0, i5 == i3 - 1);
                if (searchResult.isModule()) {
                    this.getModuleComponent(searchResult.feature()).render(drawContext, f, f7, f3, i, i2, f4, radius);
                } else {
                    boolean flag2;
                    if (this.isHovered(i, i2, f, f7, f3, f8)) {
                        flag2 = this.isHovered(i, i2, this.listX, this.listY, this.listWidth, this.listHeight);
                    } else {
                        flag2 = false;
                    }

                    if (flag2) {
                        GuiRenderUtil.drawHighlight(drawContext, f, f7, f3, f8, f5 * 0.5F, radius);
                    }

                    this.renderSettingRow(drawContext, searchResult.setting(), f, f7, f3, f8, i, i2, f4);
                }

                if (i5 > 0) {
                    GuiRenderUtil.drawOverlay(drawContext, f + 8.0F, MathUtil.roundToStep(f7, 0.5F), f3 - 16.0F, 0.5F, f5);
                }

                this.visibleRows.add(new SearchPanel.RowBounds(f, f7, f3, f8, searchResult));
            }

            f7 += f8;
        }

        return f2 + f6;
    }

    private void renderGroupHeader(DrawContext drawContext, String string, float f, float f2, float f3) {
        int i = Theme.withAlpha(Theme.getSecondaryText(), 0.6F * this.getGuiAlpha());
        new RenderUtil(drawContext, f, f2, f3, 11.0F).textMiddleLeft(Fonts.INTER_MEDIUM, string, 4.0F, 0.0F, i, 6.0F);
    }

    private void renderSettingRow(DrawContext drawContext, Setting<?> setting2, float f, float f2, float f3, float f4, int i, int i2, float f5) {
        float f6 = this.getGuiAlpha() * this.openAnimation.get();
        float f7 = f + 4.0F;
        float f8 = f2 + f4 / 2.0F - 5.0F;
        boolean flag;
        if (this.isHovered(i, i2, f7, f8, 10.0, 10.0)) {
            flag = this.isHovered(i, i2, this.listX, this.listY, this.listWidth, this.listHeight);
        } else {
            flag = false;
        }

        Animation animation = this.linkHoverAnimations.computeIfAbsent(setting2, setting3 -> new Animation());
        animation.update();
        animation.animate(flag ? 1.0 : 0.0, 0.15F, Easings.QUAD_OUT);
        float f9 = animation.get();
        GuiRenderUtil.drawHighlight(drawContext, f7, f8, 10.0F, 10.0F, f6 * f9, Radius.all(3.3333333F));
        int i3 = Theme.withAlpha(Theme.getSecondaryText(), (0.55F + 0.4F * f9) * f6);
        new RenderUtil(drawContext, f7 + 1.25F, f2 + f4 / 2.0F - 3.75F, 7.5F, 7.5F).icon(Icon.LINK, 0.0F, 0.0F, 7.5F, i3);
        float f10 = f7 + 10.0F + 4.0F;
        setting2.alphaAnimation.forceValue(1.0);
        setting2.alphaMultiplier = this.openAnimation.get();
        setting2.position.set(f10, f2);
        setting2.size.set(f + f3 - 6.0F - f10, f4);
        setting2.render(drawContext, i, i2, f5);
    }

    private ModuleRowComponent getModuleComponent(Module module) {
        return this.moduleComponents.computeIfAbsent(module, module2 -> {
            ModuleRowComponent moduleRowComponent = new ModuleRowComponent(module2, this.clickGui);
            moduleRowComponent.enableSettingsButton();
            return moduleRowComponent;
        });
    }

    private boolean isLinkHovered(double d, double d2, SearchPanel.RowBounds rowBounds) {
        float f = rowBounds.x() + 4.0F;
        float f2 = rowBounds.y() + rowBounds.height() / 2.0F - 5.0F;
        return this.isHovered(d, d2, f, f2, 10.0, 10.0);
    }

    private Radius getRowRadius(boolean flag, boolean flag2) {
        if (flag && flag2) {
            return Radius.all(6.0F);
        } else if (flag) {
            return Radius.of(0.0F, 6.0F, 0.0F, 6.0F);
        } else {
            return flag2 ? Radius.of(6.0F, 0.0F, 6.0F, 0.0F) : Radius.zero();
        }
    }

    private float getRowHeight(SearchResult searchResult) {
        return searchResult.isModule() ? 22.0F : searchResult.setting().resolveHeight(22.0F);
    }

    private void rebuildGroups() {
        this.groups.clear();
        String string = null;
        ArrayList arrayList = null;

        for (int i = 0; i < this.results.size(); i++) {
            SearchResult searchResult = this.results.get(i);
            String string2 = this.getGroupName(searchResult.feature());
            if (arrayList == null || !string2.equals(string)) {
                arrayList = new ArrayList();
                this.groups.add(new SearchPanel.ResultGroup(string2, arrayList));
                string = string2;
            }

            arrayList.add(searchResult);
        }
    }

    private float computeContentHeight() {
        if (this.groups.isEmpty()) {
            return 22.0F;
        }

        float f = 0.0F;

        for (int i = 0; i < this.groups.size(); i++) {
            if (i > 0) {
                f += 6.0F;
            }

            f += 11.0F;

            for (SearchResult searchResult : this.groups.get(i).rows()) {
                f += this.getRowHeight(searchResult);
            }
        }

        return f;
    }

    private String getGroupName(Module module) {
        String string = module.getCategory().getName();
        String string2 = module.getSubcategoryName();
        return string2.isEmpty() ? string : string + "  ›  " + string2;
    }

    public boolean keyPressed(KeyInput keyInput) {
        if (!this.open) {
            return false;
        }

        if (!this.searchField.isFocused()) {
            for (int i = 0; i < this.visibleRows.size(); i++) {
                SearchResult searchResult = this.visibleRows.get(i).row();
                if (!searchResult.isModule() && searchResult.setting().onKeyPress(keyInput)) {
                    return true;
                }
            }

            this.searchField.setFocused(true);
        }

        this.searchField.keyPressed(keyInput.key());
        return keyInput.key() != 344 && keyInput.key() != 260;
    }

    public boolean charTyped(CharInput charInput) {
        if (!this.open) {
            return false;
        }

        if (!this.searchField.isFocused()) {
            for (int i = 0; i < this.visibleRows.size(); i++) {
                SearchResult searchResult = this.visibleRows.get(i).row();
                if (!searchResult.isModule() && searchResult.setting().onCharType(charInput)) {
                    return true;
                }
            }

            this.searchField.setFocused(true);
        }

        this.searchField.charTyped((char)charInput.codepoint());
        return true;
    }

    public boolean mouseClicked(Click click, boolean flag) {
        if (!this.isLeftButton(click.button())) {
            return false;
        }

        if (this.open && this.isHovered(click.x(), click.y(), this.closeIconX - 2.0F, this.closeIconY - 2.0F, 14.0, 14.0)) {
            this.setOpen(false);
            return true;
        }

        if (this.isHovered(click.x(), click.y(), this.barX, this.barY, this.barWidth, this.barHeight)) {
            if (this.open && !(click.x() <= this.barX + this.barHeight)) {
                this.searchField.mouseClicked(click.x(), click.y(), click.button());
                if (!this.searchField.isFocused()) {
                    this.searchField.setFocused(true);
                }

                this.stopTypingSettings();
                return true;
            } else if (!this.open) {
                this.setOpen(true);
                return true;
            } else {
                this.setOpen(false);
                return true;
            }
        } else {
            if (this.open) {
                for (int i = 0; i < this.visibleRows.size(); i++) {
                    SearchResult searchResult = this.visibleRows.get(i).row();
                    if (!searchResult.isModule() && searchResult.setting().isOverlayOpen() && searchResult.setting().mouseClicked(click, flag)) {
                        return true;
                    }
                }

                if (this.scroll.mouseClicked(click.x(), click.y())) {
                    return true;
                }

                SearchPanel.RowBounds rowBounds = this.findRowAt(click.x(), click.y());
                if (rowBounds != null) {
                    if (rowBounds.row().isModule()) {
                        Module module = rowBounds.row().feature();
                        if (this.getModuleComponent(module).isOverSettingsButton(click.x(), click.y())) {
                            this.navigateToFeature(module);
                            return true;
                        }

                        module.toggle();
                    } else {
                        if (this.isLinkHovered(click.x(), click.y(), rowBounds)) {
                            this.navigateToFeature(rowBounds.row().feature());
                            return true;
                        }

                        rowBounds.row().setting().mouseClicked(click, flag);
                        if (rowBounds.row().setting().isTyping()) {
                            this.searchField.setFocused(false);
                            return true;
                        }
                    }

                    return true;
                }
            }

            if (this.searchField.isFocused()) {
                this.searchField.setFocused(false);
            }

            return false;
        }
    }

    public boolean mouseReleased(Click click) {
        this.scroll.mouseReleased();
        if (!this.open) {
            return false;
        }

        for (int i = 0; i < this.visibleRows.size(); i++) {
            SearchResult searchResult = this.visibleRows.get(i).row();
            if (!searchResult.isModule()) {
                searchResult.setting().onMouseRelease(click);
            }
        }

        return false;
    }

    public boolean mouseDragged(Click click, double d, double d2) {
        if (!this.open) {
            return false;
        }

        if (this.scroll.isDragging() && this.scroll.mouseDragged(click.x(), click.y())) {
            return true;
        }

        for (int i = 0; i < this.visibleRows.size(); i++) {
            SearchResult searchResult = this.visibleRows.get(i).row();
            if (!searchResult.isModule() && searchResult.setting().mouseDragged(click, d, d2)) {
                return true;
            }
        }

        return false;
    }

    public void renderOverlays(DrawContext drawContext, int i, int i2, float f) {
        if (!(this.openAnimation.get() <= 0.001F)) {
            for (int i3 = 0; i3 < this.visibleRows.size(); i3++) {
                SearchResult searchResult = this.visibleRows.get(i3).row();
                if (!searchResult.isModule()) {
                    searchResult.setting().renderOverlay(drawContext, i, i2, f);
                }
            }
        }
    }

    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (this.open && this.isHovered(d, d2, this.listX, this.listY, this.listWidth + 6.0F, this.listHeight)) {
            this.scroll.onScroll(d4);
            return true;
        } else {
            return false;
        }
    }

    public float getOpenProgress() {
        return this.openAnimation.get();
    }

    public void setOpen(boolean flag) {
        if (this.open != flag) {
            this.open = flag;
            if (!flag) {
                this.stopAllTyping();
            }

            this.results = null;
            this.lastQuery = "";
            this.visibleRows.clear();
            this.scroll.reset();
            this.searchField.setFocused(flag);
            this.searchField.setCursor(this.searchField.getText().length());
            this.searchField.setReplaceOnInput(false);
        }
    }

    private void stopAllTyping() {
        if (this.results != null) {
            for (SearchResult searchResult : this.results) {
                if (!searchResult.isModule()) {
                    searchResult.setting().close();
                }
            }
        }
    }

    private void stopTypingSettings() {
        for (int i = 0; i < this.visibleRows.size(); i++) {
            SearchResult searchResult = this.visibleRows.get(i).row();
            if (!searchResult.isModule() && searchResult.setting().isTyping()) {
                searchResult.setting().close();
            }
        }
    }

    public boolean hasPendingNavigation() {
        return this.pendingNavigation != null;
    }

    public SearchPanel.NavigationRequest getPendingNavigation() {
        return this.pendingNavigation;
    }

    public SearchPanel.NavigationRequest consumePendingNavigation() {
        SearchPanel.NavigationRequest navigationRequest = this.pendingNavigation;
        this.pendingNavigation = null;
        return navigationRequest;
    }

    private void navigateToFeature(Module module) {
        this.pendingNavigation = new SearchPanel.NavigationRequest(module);
        CategorySidebar categorySidebar = this.clickGui.getSidebar();
        categorySidebar.selectCategory(module.getCategory());
        if (module.getCategory() == Category.VISUALS) {
            categorySidebar.selectVisualsSubCategory(VisualsSubCategory.fromSubcategory(module.getSubcategory()));
        }

        this.setOpen(false);
    }

    private SearchPanel.RowBounds findRowAt(double d, double d2) {
        if (!this.isHovered(d, d2, this.listX, this.listY, this.listWidth, this.listHeight)) {
            return null;
        }

        for (int i = 0; i < this.visibleRows.size(); i++) {
            SearchPanel.RowBounds rowBounds = this.visibleRows.get(i);
            if (this.isHovered(d, d2, rowBounds.x(), rowBounds.y(), rowBounds.width(), rowBounds.height())) {
                return rowBounds;
            }
        }

        return null;
    }

    private float getGuiAlpha() {
        return (float)Math.pow(this.clickGui.getOpenAnimation().get(), 3.0);
    }

    private float lerp(float f, float f2, float f3) {
        return f + (f2 - f) * f3;
    }

    public boolean isOpen() {
        return this.open;
    }

    public record NavigationRequest(Module feature) {
    }

    record ResultGroup(String name, List<SearchResult> rows) {
    }

    record RowBounds(float x, float y, float width, float height, SearchResult row) {
    }
}
