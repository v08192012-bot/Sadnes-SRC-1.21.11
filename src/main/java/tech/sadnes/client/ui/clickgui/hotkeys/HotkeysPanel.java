package tech.sadnes.client.ui.clickgui.hotkeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.Bind;
import tech.sadnes.client.feature.BindMode;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.CategoryDescriptor;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureBind;
import tech.sadnes.client.feature.Module;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.ScriptCategory;
import tech.sadnes.client.script.ScriptFeature;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.setting.ValueSetting;
import tech.sadnes.client.ui.clickgui.component.BindPopupComponent;
import tech.sadnes.client.ui.clickgui.component.Component;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class HotkeysPanel extends Component {
    private static final float PADDING = 8.0F;
    private static final float HEADER_HEIGHT = 10.0F;
    private static final float FONT_SIZE = 5.5F;
    private static final float CLOSE_SIZE = 10.0F;
    private static final float SEARCH_HEIGHT = 14.0F;
    private static final float GAP = 4.0F;
    private static final float CATEGORY_HEIGHT = 14.0F;
    private static final float MODULE_SPACING = 3.0F;
    private static final float CATEGORY_LABEL_HEIGHT = 12.0F;
    private static final float TEXT_INSET = 2.0F;
    private static final float MODULE_INDENT = 7.0F;
    private static final float BIND_ROW_HEIGHT = 18.0F;
    private static final float BIND_ROW_SPACING = 3.0F;
    private static final float BINDS_COLUMN_WIDTH = 170.0F;
    private static final float RESET_BUTTON_HEIGHT = 15.0F;
    private static final float FOOTER_GAP = 4.0F;
    private final Animation openAnimation = new Animation();
    private final Animation bindsAnimation = new Animation();
    private final SmoothScrollbar modulesScroll = new SmoothScrollbar();
    private final SmoothScrollbar bindsScroll = new SmoothScrollbar();
    private final TextFieldComponent searchField = new TextFieldComponent(Fonts.INTER_MEDIUM, 5.5F, "hotkeys.panel.search", false, false);
    private final List<Module> searchResults = new ArrayList<>();
    private final List<HotkeysPanel.ModuleRow> moduleRows = new ArrayList<>();
    private final List<HotkeysPanel.CategoryRow> categoryRows = new ArrayList<>();
    private final List<HotkeysPanel.BindRow> bindRows = new ArrayList<>();
    private final Map<Module, Animation> moduleHoverAnimations = new IdentityHashMap<>();
    private final Map<Module, Animation> moduleSelectAnimations = new IdentityHashMap<>();
    private final Map<Object, Animation> bindHoverAnimations = new IdentityHashMap<>();
    private final Map<CategoryDescriptor, Animation> categoryExpandAnimations = new HashMap<>();
    private final Set<CategoryDescriptor> collapsedCategories = new HashSet<>();
    private BindPopupComponent bindPopup;
    private boolean open;
    private boolean closing;
    private boolean boundOnlyFilter;
    private Module selectedModule;
    private float modulesX;
    private float modulesWidth;
    private float bindsX;
    private float contentY;
    private float contentHeight;
    private float modulesListY;
    private float modulesListHeight;
    private float closeX;
    private float closeY;
    private float resetAllX;
    private float resetAllY;
    private float resetAllWidth;
    private float resetAllHeight;
    private float resetModuleX;
    private float resetModuleY;
    private float resetModuleSize;
    private float filterX;
    private float filterY;
    private float filterSize;

    public boolean isOpen() {
        return this.open && !this.closing;
    }

    public boolean isVisible() {
        return this.openAnimation.get() > 0.001;
    }

    public boolean isListeningForKey() {
        return this.bindPopup != null && this.bindPopup.isListeningForKey();
    }

    public float getOpenProgress() {
        return (float)Math.min(this.openAnimation.get(), 1.0);
    }

    public boolean isPanelHovered(double d, double d2) {
        return this.isVisible() && this.isHovered(d, d2, this.position.x, this.position.y, this.size.x, this.size.y);
    }

    public void open() {
        this.open = true;
        this.closing = false;
        if (this.bindPopup != null) {
            this.bindPopup.resetState();
        }

        this.searchField.setText("");
        this.searchField.setFocused(true);
        this.updateSearchResults();
        this.bindsAnimation.forceValue(0.0);
        this.bindsAnimation.animate(1.0, 0.3, Easings.QUAD_OUT);
        this.openAnimation.animate(1.0, 0.35, Easings.QUART_OUT);
    }

    @Override
    public void close() {
        if (this.open && !this.closing) {
            if (this.bindPopup != null) {
                this.bindPopup.close();
            }

            this.closing = true;
            this.searchField.setFocused(false);
            this.openAnimation.animate(0.0, 0.22, Easings.QUAD_IN);
        }
    }

    public void reset() {
        if (this.bindPopup != null) {
            this.bindPopup.resetState();
            this.bindPopup = null;
        }

        this.open = false;
        this.closing = false;
        this.selectedModule = null;
        this.searchField.setText("");
        this.searchField.setFocused(false);
        this.searchResults.clear();
        this.moduleRows.clear();
        this.categoryRows.clear();
        this.bindRows.clear();
        this.moduleHoverAnimations.clear();
        this.moduleSelectAnimations.clear();
        this.bindHoverAnimations.clear();
        this.openAnimation.forceValue(0.0);
    }

    private void selectModule(Module module) {
        this.selectedModule = module;
        if (this.bindPopup != null) {
            this.bindPopup.close();
        }

        this.bindsScroll.reset();
        this.bindHoverAnimations.clear();
        this.bindsAnimation.forceValue(0.0);
        this.bindsAnimation.animate(1.0, 0.3, Easings.QUAD_OUT);
    }

    private void updateSearchResults() {
        String string = this.searchField.getText().trim().toLowerCase(Locale.ROOT);
        this.searchResults.clear();
        ArrayList<Module> modules = new ArrayList<>(Sadnes.getInstance().getFeatureManager().values());
        modules.addAll(getScripts());

        for (Module module : modules) {
            if (!module.isHidden() && (string.isEmpty() || module.getDisplayName().toLowerCase(Locale.ROOT).contains(string))) {
                this.searchResults.add(module);
            }
        }

        if (this.selectedModule == null && !this.searchResults.isEmpty()) {
            this.selectModule(this.searchResults.get(0));
        }
    }

    private static List<ScriptFeature> getScripts() {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        return scriptManager == null ? List.of() : scriptManager.getActiveHudElements();
    }

    private static List<ScriptCategory> getScriptCategories() {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        return scriptManager == null ? List.of() : scriptManager.getActiveHudStyles();
    }

    private static boolean hasAnyBinds(Module module) {
        if (module instanceof Feature && !((Feature)module).getBinds().isEmpty()) {
            return true;
        }

        for (Setting setting : module.getSettings()) {
            if (setting.hasBindsRecursive()) {
                return true;
            }
        }

        return false;
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.openAnimation.update();
        this.bindsAnimation.update();
        if (this.closing && this.openAnimation.isFinished() && this.openAnimation.get() <= 0.001) {
            this.open = false;
            this.closing = false;
        }

        if (this.open && !(this.openAnimation.get() <= 0.001)) {
            this.updateSearchResults();
            this.position.set(f2, f3);
            this.size.set(f4, f5);
            float f6 = this.getOpenProgress();
            float f7 = f6 * this.getAlpha();
            RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, f4, f5);
            renderUtil.blur(f7, Radius.all(8.0F), RenderUtil.BlurQuality.LOW, true);
            renderUtil.rect(Theme.withAlpha(Theme.getBackground(), 0.4F), f7, Radius.all(8.0F));
            float f8 = f3 + 22.0F;
            this.contentY = f8 + 10.0F + 4.0F;
            float f9 = f3 + f5 - 8.0F;
            this.contentHeight = f9 - this.contentY;
            this.bindsX = f2 + f4 - 8.0F - 170.0F;
            this.modulesX = f2 + 8.0F;
            this.modulesWidth = this.bindsX - 8.0F - this.modulesX;
            this.modulesListY = this.contentY + 14.0F + 4.0F;
            this.modulesListHeight = f9 - 15.0F - 4.0F - this.modulesListY;
            this.closeX = f2 + f4 - 5.0F - 22.0F + 6.0F;
            this.closeY = f3 + 4.0F + 6.0F;
            this.renderHeader(drawContext, i, i2, f8, f7);
            this.renderSearchField(drawContext, f7);
            this.renderModuleList(drawContext, i, i2, f7);
            this.renderBindList(drawContext, i, i2, f7);
            this.renderResetAllButton(drawContext, i, i2, f9, f7);
            if (this.bindPopup != null) {
                this.bindPopup.render(drawContext, i, i2, f);
            }
        }
    }

    private void renderHeader(DrawContext drawContext, int i, int i2, float f, float f2) {
        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.65F), f2);
        new RenderUtil(drawContext, this.modulesX + 2.0F, f, this.modulesWidth, 10.0F)
            .textMiddleLeft(Fonts.INTER_SEMIBOLD, Language.translate("hotkeys.panel.modules", "Modules"), 0.0F, 0.0F, i3, 5.5F);
        new RenderUtil(drawContext, this.bindsX + 2.0F, f, 154.0F, 10.0F)
            .textMiddleLeft(Fonts.INTER_SEMIBOLD, Language.translate("hotkeys.panel.binds", "Hot Keys"), 0.0F, 0.0F, i3, 5.5F);
        this.filterSize = 8.0F;
        this.filterX = this.modulesX + this.modulesWidth - this.filterSize - 2.0F;
        this.filterY = f + 5.0F - this.filterSize / 2.0F;
        boolean flag = this.isHovered(i, i2, this.filterX - 3.0F, this.filterY - 3.0F, this.filterSize + 6.0F, this.filterSize + 6.0F);
        if (flag) {
            GuiRenderUtil.drawHighlight(drawContext, this.filterX - 3.0F, this.filterY - 3.0F, this.filterSize + 6.0F, this.filterSize + 6.0F, f2, Radius.all(4.0F));
        }

        int i4 = this.boundOnlyFilter
            ? ColorUtil.multiplyAlpha(Theme.getPrimary(), f2)
            : ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), flag ? 0.9F : 0.55F), f2);
        new RenderUtil(drawContext, this.filterX, this.filterY, this.filterSize, this.filterSize).icon(Icon.FILTER, 0.0F, 0.0F, this.filterSize, i4);
        this.resetModuleSize = 8.0F;
        this.resetModuleX = this.bindsX + 170.0F - this.resetModuleSize - 2.0F;
        this.resetModuleY = f + 5.0F - this.resetModuleSize / 2.0F;
        if (this.selectedModule != null) {
            boolean flag2 = this.isHovered(
                i, i2, this.resetModuleX - 3.0F, this.resetModuleY - 3.0F, this.resetModuleSize + 6.0F, this.resetModuleSize + 6.0F
            );
            int i5 = ColorUtil.multiplyAlpha(Theme.withAlpha(ColorUtil.rgb(255, 120, 120), flag2 ? 1.0F : 0.6F), f2);
            new RenderUtil(drawContext, this.resetModuleX, this.resetModuleY, this.resetModuleSize, this.resetModuleSize)
                .icon(Icon.REFRESH, 0.0F, 0.0F, this.resetModuleSize, i5);
        }

        new RenderUtil(drawContext, this.closeX, this.closeY, 10.0F, 10.0F).icon(Icon.CLOSE, 0.0F, 0.0F, 10.0F, Theme.withAlpha(Theme.getSecondaryText(), f2));
    }

    private void renderResetAllButton(DrawContext drawContext, int i, int i2, float f, float f2) {
        this.resetAllX = this.modulesX;
        this.resetAllY = f - 15.0F;
        this.resetAllWidth = this.modulesWidth;
        this.resetAllHeight = 15.0F;
        boolean flag = this.isHovered(i, i2, this.resetAllX, this.resetAllY, this.resetAllWidth, this.resetAllHeight);
        GuiRenderUtil.drawOutlined(drawContext, this.resetAllX, this.resetAllY, this.resetAllWidth, this.resetAllHeight, f2 * 0.5F, 5.0F);
        if (flag) {
            GuiRenderUtil.drawHighlight(drawContext, this.resetAllX, this.resetAllY, this.resetAllWidth, this.resetAllHeight, f2 * 0.6F, Radius.all(5.0F));
        }

        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(ColorUtil.rgb(255, 86, 86), flag ? 1.0F : 0.85F), f2);
        String string = Language.translate("hotkeys.panel.resetAll", "Reset all");
        float f3 = Fonts.INTER_MEDIUM.getWidth(string, 5.5F);
        float f4 = this.resetAllX + this.resetAllWidth / 2.0F - (8.0F + 4.0F + f3) / 2.0F;
        new RenderUtil(drawContext, f4, this.resetAllY + this.resetAllHeight / 2.0F - 8.0F / 2.0F, 8.0F, 8.0F).icon(Icon.REFRESH, 0.0F, 0.0F, 8.0F, i3);
        new RenderUtil(drawContext, f4 + 8.0F + 4.0F, this.resetAllY, f3 + 2.0F, this.resetAllHeight)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, i3, 5.5F);
    }

    private void renderSearchField(DrawContext drawContext, float f) {
        GuiRenderUtil.drawOutlined(drawContext, this.modulesX, this.contentY, this.modulesWidth, 14.0F, f * 0.5F, 4.6666665F);
        new RenderUtil(drawContext, this.modulesX + 4.0F, this.contentY + 7.0F - 8.0F / 2.0F, 8.0F, 8.0F)
            .icon(Icon.SEARCH, 0.0F, 0.0F, 8.0F, Theme.withAlpha(Theme.getSecondaryText(), f * 0.7F));
        if (!this.closing) {
            this.searchField.setFocused(true);
        }

        float f2 = this.modulesX + 4.0F + 8.0F + 4.0F;
        this.searchField
            .render(
                drawContext,
                new Vector2f(f2, this.contentY + 7.0F - 2.75F),
                ColorUtil.multiplyAlpha(Theme.getTextColor(), f),
                this.modulesWidth - (f2 - this.modulesX) - 4.0F
            );
    }

    private void renderModuleList(DrawContext drawContext, int i, int i2, float f) {
        this.moduleRows.clear();
        this.categoryRows.clear();
        String string = this.searchField.getText().trim().toLowerCase(Locale.ROOT);
        boolean flag = !string.isEmpty();
        ArrayList<CategoryDescriptor> categoryDescriptors = new ArrayList<>(List.of(Category.values()));
        categoryDescriptors.addAll(getScriptCategories());
        LinkedHashMap<CategoryDescriptor, List<Module>> linkedHashMap = new LinkedHashMap<>();
        float f2 = 0.0F;

        for (CategoryDescriptor categoryDescriptor : categoryDescriptors) {
            List<Module> modules = this.getFilteredModules(categoryDescriptor, string);
            if (!modules.isEmpty()) {
                linkedHashMap.put(categoryDescriptor, modules);
                f2 += 14.0F;
                if (flag || !this.collapsedCategories.contains(categoryDescriptor)) {
                    f2 += modules.size() * 17.0F;
                }
            }
        }

        this.modulesScroll.updateHover(i, i2);
        this.modulesScroll.update(f2, this.modulesListHeight);
        float f3 = this.modulesScroll.getScroll();
        boolean flag2 = ScissorStack.push(drawContext, this.modulesX, this.modulesListY, this.modulesWidth + 4.0F, this.modulesListHeight);
        float f4 = this.modulesListY + f3;

        for (Entry<CategoryDescriptor, List<Module>> entry : linkedHashMap.entrySet()) {
            CategoryDescriptor categoryDescriptor2 = (CategoryDescriptor)entry.getKey();
            boolean flag3;
            if (!flag) {
                flag3 = !this.collapsedCategories.contains(categoryDescriptor2);
            } else {
                flag3 = true;
            }

            this.renderCategoryHeader(drawContext, categoryDescriptor2, this.modulesX, f4, this.modulesWidth, i, i2, f, flag3);
            this.categoryRows.add(new HotkeysPanel.CategoryRow(categoryDescriptor2, this.modulesX, f4, this.modulesWidth, 12.0F));
            f4 += 14.0F;
            if (flag3) {
                for (Module module : entry.getValue()) {
                    HotkeysPanel.ModuleRow moduleRow = new HotkeysPanel.ModuleRow(module, this.modulesX + 7.0F, f4, this.modulesWidth - 7.0F, 14.0F);
                    this.renderModuleRow(drawContext, moduleRow, i, i2, f);
                    this.moduleRows.add(moduleRow);
                    f4 += 17.0F;
                }
            }
        }

        if (flag2) {
            ScissorStack.pop(drawContext);
        }

        this.modulesScroll.render(drawContext, this.modulesX + this.modulesWidth + 1.0F, this.modulesListY, this.modulesListHeight, f2, f);
    }

    private void renderCategoryHeader(
        DrawContext drawContext, CategoryDescriptor categoryDescriptor, float f, float f2, float f3, int i, int i2, float f4, boolean flag
    ) {
        boolean flag2 = this.isHovered(i, i2, f, f2, f3, 12.0);
        Animation animation = this.categoryExpandAnimations.computeIfAbsent(categoryDescriptor, categoryDescriptor2 -> {
            Animation animation2 = new Animation();
            if (flag) {
                animation2.forceValue(1.0);
                return animation2;
            } else {
                animation2.forceValue(0.0);
                return animation2;
            }
        });
        animation.update();
        animation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), flag2 ? 0.9F : 0.6F), f4);
        RenderUtil.drawChevron(drawContext, f + 2.0F, f2 + 6.0F - 6.0F / 2.0F, 6.0F, 90.0F * animation.get(), i3);
        new RenderUtil(drawContext, f + 2.0F + 6.0F + 3.0F, f2, f3 - 6.0F - 5.0F, 12.0F)
            .textMiddleLeft(Fonts.INTER_SEMIBOLD, categoryDescriptor.getName(), 0.0F, 0.0F, i3, 5.5F);
    }

    private List<Module> getFilteredModules(CategoryDescriptor categoryDescriptor, String string) {
        ArrayList<Module> modules = new ArrayList<>();
        if (categoryDescriptor instanceof Category category) {
            modules.addAll(Sadnes.getInstance().getFeatureManager().getByCategory(category));
        }

        for (ScriptFeature scriptFeature : getScripts()) {
            if (scriptFeature.getCategory() == categoryDescriptor) {
                modules.add(scriptFeature);
            }
        }

        ArrayList arrayList = new ArrayList();

        for (Module module : modules) {
            if (!module.isHidden()
                && (!this.boundOnlyFilter || hasAnyBinds(module))
                && (string.isEmpty() || module.getDisplayName().toLowerCase(Locale.ROOT).contains(string))) {
                arrayList.add(module);
            }
        }

        return arrayList;
    }

    private void toggleCategoryCollapsed(CategoryDescriptor categoryDescriptor) {
        if (!this.collapsedCategories.add(categoryDescriptor)) {
            this.collapsedCategories.remove(categoryDescriptor);
        }
    }

    private void renderModuleRow(DrawContext drawContext, HotkeysPanel.ModuleRow moduleRow, int i, int i2, float f) {
        boolean flag = this.isHovered(i, i2, moduleRow.x, moduleRow.y, moduleRow.width, moduleRow.height);
        boolean flag2 = moduleRow.module == this.selectedModule;
        Animation animation = this.moduleHoverAnimations.computeIfAbsent(moduleRow.module, module2 -> new Animation());
        Animation animation2 = this.moduleSelectAnimations.computeIfAbsent(moduleRow.module, module3 -> new Animation());
        animation.update();
        animation2.update();
        animation.animate(flag ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        animation2.animate(flag2 ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        float f2 = animation.get();
        float f3 = animation2.get();
        float f4 = Math.max(f3, f2 * 0.5F);
        GuiRenderUtil.drawHighlight(drawContext, moduleRow.x, moduleRow.y, moduleRow.width, moduleRow.height, f * f4, Radius.all(5.0F));
        int i3 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), f3), f);
        int i4 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), Math.max(f3, f2 * 0.5F)), f);
        new RenderUtil(drawContext, moduleRow.x + 5.0F, moduleRow.y + moduleRow.height / 2.0F - 4.0F, 8.0F, 8.0F).icon(moduleRow.module.getCategory().getIcon(), 0.0F, 0.0F, 8.0F, i3);
        new RenderUtil(drawContext, moduleRow.x + 15.0F, moduleRow.y, moduleRow.width - 15.0F, moduleRow.height)
            .textMiddleLeft(Fonts.INTER_MEDIUM, moduleRow.module.getDisplayName(), 0.0F, 0.0F, i4, 5.5F);
    }

    private void renderBindList(DrawContext drawContext, int i, int i2, float f) {
        this.bindRows.clear();
        List<HotkeysPanel.BindRow> bindRows2 = this.selectedModule == null ? List.of() : this.collectBindRows(this.selectedModule);
        float f2 = this.bindsAnimation.get();
        float f3 = f * f2;
        float f4 = bindRows2.size() * 21.0F;
        this.bindsScroll.updateHover(i, i2);
        this.bindsScroll.update(f4, this.contentHeight);
        float f5 = this.bindsScroll.getScroll();
        boolean flag = ScissorStack.push(drawContext, this.bindsX, this.contentY, 174.0F, this.contentHeight);
        float f6 = this.contentY + f5 + (1.0F - f2) * 8.0F;

        for (HotkeysPanel.BindRow bindRow : bindRows2) {
            bindRow.x = this.bindsX;
            bindRow.y = f6;
            bindRow.width = 170.0F;
            bindRow.height = 18.0F;
            this.renderBindRow(drawContext, bindRow, i, i2, f3);
            this.bindRows.add(bindRow);
            f6 += 21.0F;
        }

        if (flag) {
            ScissorStack.pop(drawContext);
        }

        this.bindsScroll.render(drawContext, this.bindsX + 170.0F + 1.0F, this.contentY, this.contentHeight, f4, f);
        if (bindRows2.isEmpty()) {
            new RenderUtil(drawContext, this.bindsX, this.contentY, 170.0F, this.contentHeight)
                .textCenter(
                    Fonts.INTER_MEDIUM,
                    Language.translate("hotkeys.panel.empty", "No hot keys"),
                    ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f * 0.7F),
                    5.5F,
                    false
                );
        }
    }

    private void renderBindRow(DrawContext drawContext, HotkeysPanel.BindRow bindRow, int i, int i2, float f) {
        boolean flag = bindRow.bind != null;
        bindRow.modeWidth = 0.0F;
        bindRow.removeWidth = 0.0F;
        boolean flag2 = this.isHovered(i, i2, bindRow.x, bindRow.y, bindRow.width, bindRow.height);
        Object object = flag ? bindRow.bind : (bindRow.setting != null ? bindRow.setting : this.selectedModule);
        Animation animation = this.bindHoverAnimations.computeIfAbsent(object, object2 -> new Animation());
        animation.update();
        animation.animate(flag2 ? 1.0 : 0.0, 0.16, Easings.QUAD_OUT);
        float f2 = animation.get();
        if (f2 > 0.001F) {
            GuiRenderUtil.drawHighlight(drawContext, bindRow.x, bindRow.y, bindRow.width, bindRow.height, f * f2 * 0.6F, Radius.all(5.0F));
        }

        float f3 = bindRow.x + bindRow.width;
        if (bindRow.removeAction != null) {
            bindRow.removeWidth = 12.0F;
            bindRow.removeHeight = bindRow.height;
            bindRow.removeX = f3 - 12.0F;
            bindRow.removeY = bindRow.y;
            boolean flag3 = this.isHovered(i, i2, bindRow.removeX, bindRow.removeY, bindRow.removeWidth, bindRow.removeHeight);
            int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(ColorUtil.rgb(255, 94, 94), flag3 ? 1.0F : 0.55F), f);
            new RenderUtil(drawContext, bindRow.removeX + 12.0F / 2.0F - 4.0F, bindRow.y + bindRow.height / 2.0F - 4.0F, 8.0F, 8.0F).icon(Icon.TRASH, 0.0F, 0.0F, 8.0F, i3);
            f3 = bindRow.removeX - 2.0F;
        }

        boolean flag4 = this.bindPopup != null && this.bindPopup.isListeningForKey() && this.bindPopup.getSelectedBind() == bindRow.bind;
        String string = flag4 ? SettingWidgets.getWaitingText() : (flag ? bindRow.bind.getDisplayString() : Language.translate("setting.bind.none", "N/A"));
        float f4 = Fonts.INTER_MEDIUM.getWidth(string, 5.5F);
        bindRow.keyWidth = Math.max(26.0F, f4 + 10.0F);
        bindRow.keyHeight = 11.0F;
        bindRow.keyX = f3 - 4.0F - bindRow.keyWidth;
        bindRow.keyY = bindRow.y + bindRow.height / 2.0F - bindRow.keyHeight / 2.0F;
        if (flag4) {
            SettingWidgets.drawWaitingBind(drawContext, bindRow.keyX, bindRow.keyY, bindRow.keyWidth, bindRow.keyHeight, f);
        } else {
            SettingWidgets.drawBindBox(drawContext, bindRow.keyX, bindRow.keyY, bindRow.keyWidth, bindRow.keyHeight, string, flag ? 1.0F : 0.0F, f);
        }

        float f5 = bindRow.keyX;
        if (flag) {
            String string2 = bindRow.bind.getMode().getDisplayName();
            float f6 = Fonts.INTER_MEDIUM.getWidth(string2, 5.5F);
            bindRow.modeWidth = f6 + 10.0F;
            bindRow.modeHeight = 11.0F;
            bindRow.modeX = f5 - 4.0F - bindRow.modeWidth;
            bindRow.modeY = bindRow.y + bindRow.height / 2.0F - bindRow.modeHeight / 2.0F;
            boolean flag5 = this.isHovered(i, i2, bindRow.modeX, bindRow.modeY, bindRow.modeWidth, bindRow.modeHeight);
            GuiRenderUtil.drawSurface(drawContext, bindRow.modeX, bindRow.modeY, bindRow.modeWidth, bindRow.modeHeight, f, 3.0F)
                .textCenter(Fonts.INTER_MEDIUM, string2, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f * (flag5 ? 1.0F : 0.75F)), 5.5F, false);
            f5 = bindRow.modeX;
        }

        int i4 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f);
        float f7 = f5 - 4.0F - (bindRow.x + 4.0F);
        new RenderUtil(drawContext, bindRow.x + 4.0F, bindRow.y, f7, bindRow.height).textMiddleLeft(Fonts.INTER_MEDIUM, bindRow.label, 0.0F, 0.0F, i4, 5.5F);
    }

    private List<HotkeysPanel.BindRow> collectBindRows(Module module) {
        ArrayList arrayList = new ArrayList();
        BooleanSetting booleanSetting = null;
        if (module instanceof Feature feature) {
            List<FeatureBind> featureBinds = feature.getBinds();
            if (featureBinds.isEmpty()) {
                arrayList.add(new HotkeysPanel.BindRow(null, null, feature.getDisplayName(), () -> this.createFeatureBind(feature), null));
            } else {
                for (FeatureBind featureBind : featureBinds) {
                    arrayList.add(new HotkeysPanel.BindRow(featureBind, null, feature.getDisplayName(), null, () -> feature.removeBind(featureBind)));
                }
            }
        } else if (module instanceof ScriptFeature scriptFeature) {
            booleanSetting = scriptFeature.getEnabledSetting();
            this.collectScriptBindRows(scriptFeature, arrayList);
        }

        for (Setting<?> setting : module.getSettings()) {
            if (setting != booleanSetting) {
                this.collectSettingBindRows(setting, arrayList);

                for (Setting<?> setting2 : setting.getAllChildren()) {
                    this.collectSettingBindRows(setting2, arrayList);
                }
            }
        }

        return arrayList;
    }

    private void collectScriptBindRows(ScriptFeature scriptFeature, List<HotkeysPanel.BindRow> bindRows2) {
        BooleanSetting booleanSetting = scriptFeature.getEnabledSetting();
        List<ValueBind<Boolean>> valueBinds = booleanSetting.getBinds();
        if (valueBinds.isEmpty()) {
            bindRows2.add(new HotkeysPanel.BindRow(null, booleanSetting, scriptFeature.getDisplayName(), () -> this.createSettingBind(booleanSetting), null));
        } else {
            for (ValueBind<Boolean> valueBind : valueBinds) {
                bindRows2.add(new HotkeysPanel.BindRow(valueBind, booleanSetting, scriptFeature.getDisplayName(), null, () -> this.removeSettingBind(booleanSetting, valueBind)));
            }
        }
    }

    private void collectSettingBindRows(Setting<?> setting, List<HotkeysPanel.BindRow> bindRows2) {
        if (setting instanceof ValueSetting valueSetting) {
            String string = Language.translate(setting.getName());
            List<ValueBind<?>> valueBinds = valueSetting.getBinds();
            if (valueBinds.isEmpty()) {
                if (setting instanceof KeybindSetting) {
                    bindRows2.add(new HotkeysPanel.BindRow(null, valueSetting, string, () -> this.createSettingBind(valueSetting), null));
                    return;
                }
            } else {
                for (ValueBind<?> valueBind : valueBinds) {
                    bindRows2.add(new HotkeysPanel.BindRow(valueBind, valueSetting, string, null, () -> this.removeSettingBind(valueSetting, valueBind)));
                }
            }
        }
    }

    private Bind createSettingBind(ValueSetting<?> valueSetting) {
        ValueSetting<Object> valueSetting2 = (ValueSetting<Object>)valueSetting;
        return valueSetting2.createBind(valueSetting2.getValue());
    }

    private Bind createFeatureBind(Feature feature) {
        FeatureBind featureBind = new FeatureBind(-1, BindMode.TOGGLE, true, true);
        feature.addBind(featureBind);
        return featureBind;
    }

    private void removeSettingBind(ValueSetting<?> valueSetting, ValueBind<?> valueBind) {
        ((ValueSetting<Object>)valueSetting).removeBind((ValueBind<Object>)valueBind);
    }

    private void resetAllBinds() {
        if (this.bindPopup != null) {
            this.bindPopup.close();
        }

        for (Feature feature : Sadnes.getInstance().getFeatureManager().values()) {
            this.resetModuleBinds(feature);
        }

        for (ScriptFeature scriptFeature : getScripts()) {
            this.resetModuleBinds(scriptFeature);
        }
    }

    private void resetModuleBinds(Module module) {
        if (module != null) {
            if (this.bindPopup != null) {
                this.bindPopup.close();
            }

            if (module instanceof Feature) {
                ((Feature)module).clearBinds();
            }

            for (Setting<?> setting : module.getSettings()) {
                this.clearSettingBinds(setting);

                for (Setting<?> setting2 : setting.getAllChildren()) {
                    this.clearSettingBinds(setting2);
                }
            }
        }
    }

    private void clearSettingBinds(Setting<?> setting) {
        if (setting instanceof ValueSetting) {
            ((ValueSetting)setting).clearBinds();
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (this.bindPopup == null || !this.bindPopup.isOpen() && !this.bindPopup.isListeningForKey()) {
            if (this.bindPopup != null && this.bindPopup.isMouseOver(click.x(), click.y())) {
                return this.bindPopup.mouseClicked(click, flag);
            }

            if (this.modulesScroll.mouseClicked(click.x(), click.y())) {
                return true;
            }

            if (this.bindsScroll.mouseClicked(click.x(), click.y())) {
                return true;
            }

            if (this.isLeftButton(click.button()) && this.isHovered(click.x(), click.y(), this.closeX - 2.0F, this.closeY - 2.0F, 14.0, 14.0)) {
                this.close();
                return true;
            }

            if (this.isLeftButton(click.button())
                && this.isHovered(click.x(), click.y(), this.filterX - 3.0F, this.filterY - 3.0F, this.filterSize + 6.0F, this.filterSize + 6.0F)) {
                if (!this.boundOnlyFilter) {
                    this.boundOnlyFilter = true;
                    return true;
                } else {
                    this.boundOnlyFilter = false;
                    return true;
                }
            } else {
                if (this.isLeftButton(click.button())
                    && this.isHovered(click.x(), click.y(), this.resetAllX, this.resetAllY, this.resetAllWidth, this.resetAllHeight)) {
                    this.resetAllBinds();
                    return true;
                }

                if (this.isLeftButton(click.button())
                    && this.selectedModule != null
                    && this.isHovered(
                        click.x(), click.y(), this.resetModuleX - 3.0F, this.resetModuleY - 3.0F, this.resetModuleSize + 6.0F, this.resetModuleSize + 6.0F
                    )) {
                    this.resetModuleBinds(this.selectedModule);
                    return true;
                }

                if (this.isHovered(click.x(), click.y(), this.modulesX, this.modulesListY, this.modulesWidth, this.modulesListHeight)) {
                    for (HotkeysPanel.CategoryRow categoryRow : this.categoryRows) {
                        if (this.isHovered(click.x(), click.y(), categoryRow.x, categoryRow.y, categoryRow.width, categoryRow.height)) {
                            this.toggleCategoryCollapsed(categoryRow.category);
                            return true;
                        }
                    }

                    for (HotkeysPanel.ModuleRow moduleRow : this.moduleRows) {
                        if (this.isHovered(click.x(), click.y(), moduleRow.x, moduleRow.y, moduleRow.width, moduleRow.height)) {
                            if (this.selectedModule != moduleRow.module) {
                                this.selectModule(moduleRow.module);
                            }
                            break;
                        }
                    }
                } else {
                    if (!this.isHovered(click.x(), click.y(), this.bindsX, this.contentY, 170.0, this.contentHeight)) {
                        return this.isPanelHovered(click.x(), click.y());
                    }

                    for (HotkeysPanel.BindRow bindRow : this.bindRows) {
                        if (bindRow.removeAction != null && this.isHovered(click.x(), click.y(), bindRow.removeX, bindRow.removeY, bindRow.removeWidth, bindRow.removeHeight)) {
                            if (this.bindPopup != null && this.bindPopup.getSelectedBind() == bindRow.bind) {
                                this.bindPopup.close();
                            }

                            bindRow.removeAction.run();
                            return true;
                        }

                        if (this.isHovered(click.x(), click.y(), bindRow.keyX, bindRow.keyY, bindRow.keyWidth, bindRow.keyHeight)) {
                            if (bindRow.bind == null) {
                                bindRow.bind = bindRow.bindFactory.get();
                            }

                            Module module2 = this.selectedModule;
                            this.bindPopup = new BindPopupComponent(module2 instanceof Feature ? (Feature)module2 : null);
                            this.bindPopup.setAllHotKeysAction(() -> this.bindPopup.close());
                            if (bindRow.setting != null) {
                                this.bindPopup.openForSetting(bindRow.setting, click.x(), click.y());
                                return true;
                            }

                            this.bindPopup.openForFeature(click.x(), click.y());
                            return true;
                        }

                        if (bindRow.bind != null && this.isHovered(click.x(), click.y(), bindRow.modeX, bindRow.modeY, bindRow.modeWidth, bindRow.modeHeight)) {
                            Bind bind2 = bindRow.bind;
                            if (bindRow.bind.getMode() == BindMode.HOLD) {
                                bind2.setMode(BindMode.TOGGLE);
                                return true;
                            }

                            bind2.setMode(BindMode.HOLD);
                            return true;
                        }
                    }
                }

                return true;
            }
        } else {
            this.bindPopup.mouseClicked(click, flag);
            return true;
        }
    }

    @Override
    public boolean onMouseRelease(Click click) {
        if (this.bindPopup != null) {
            this.bindPopup.onMouseRelease(click);
        }

        this.modulesScroll.mouseReleased();
        this.bindsScroll.mouseReleased();
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.bindPopup != null && this.bindPopup.isOpen() && this.bindPopup.mouseDragged(click, d, d2)) {
            return true;
        } else {
            return this.modulesScroll.mouseDragged(click.x(), click.y()) ? true : this.bindsScroll.mouseDragged(click.x(), click.y());
        }
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (this.bindPopup != null && this.bindPopup.isOpen()) {
            this.bindPopup.mouseScrolled(d, d2, d3, d4);
            return true;
        } else if (this.isHovered(d, d2, this.bindsX, this.contentY, 170.0, this.contentHeight)) {
            this.bindsScroll.onScroll(d4);
            return true;
        } else if (this.isHovered(d, d2, this.modulesX, this.contentY, this.modulesWidth, this.contentHeight)) {
            this.modulesScroll.onScroll(d4);
            return true;
        } else {
            return this.isPanelHovered(d, d2);
        }
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (this.bindPopup != null && this.bindPopup.isOpen() && this.bindPopup.onKeyPress(keyInput)) {
            return true;
        }

        int i = keyInput.key();
        switch (i) {
            case 256:
                this.close();
                return true;
            case 260:
            case 344:
                return false;
            default:
                this.searchField.keyPressed(i);
                return true;
        }
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        return this.bindPopup != null && this.bindPopup.isOpen() && this.bindPopup.keyReleased(keyInput);
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        if (this.bindPopup != null && this.bindPopup.isOpen() && this.bindPopup.onCharType(charInput)) {
            return true;
        }

        this.searchField.charTyped((char)charInput.codepoint());
        return true;
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public void resize(int i, int i2) {
        this.reset();
    }

    @Override
    public void init(int i, int i2) {
    }

    @Override
    public void removed() {
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    static final class BindRow {
        Bind bind;
        final ValueSetting<?> setting;
        final String label;
        final Supplier<Bind> bindFactory;
        final Runnable removeAction;
        float x;
        float y;
        float width;
        float height;
        float keyX;
        float keyY;
        float keyWidth;
        float keyHeight;
        float modeX;
        float modeY;
        float modeWidth;
        float modeHeight;
        float removeX;
        float removeY;
        float removeWidth;
        float removeHeight;

        BindRow(Bind bind2, ValueSetting<?> valueSetting, String string, Supplier<Bind> supplier, Runnable runnable) {
            this.bind = bind2;
            this.setting = valueSetting;
            this.label = string;
            this.bindFactory = supplier;
            this.removeAction = runnable;
        }
    }

    static final class CategoryRow {
        final CategoryDescriptor category;
        final float x;
        final float y;
        final float width;
        final float height;

        CategoryRow(CategoryDescriptor categoryDescriptor, float f, float f2, float f3, float f4) {
            this.category = categoryDescriptor;
            this.x = f;
            this.y = f2;
            this.width = f3;
            this.height = f4;
        }
    }

    static final class ModuleRow {
        final Module module;
        final float x;
        final float y;
        final float width;
        final float height;

        ModuleRow(Module module2, float f, float f2, float f3, float f4) {
            this.module = module2;
            this.x = f;
            this.y = f2;
            this.width = f3;
            this.height = f4;
        }
    }
}
