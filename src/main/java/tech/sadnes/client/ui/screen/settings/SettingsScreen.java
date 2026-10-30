package tech.sadnes.client.ui.screen.settings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.config.FavoritesPrefs;
import tech.sadnes.client.config.ProfilesPrefs;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.IconHoverAnimator;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.ShimmerText;
import tech.sadnes.client.render.texture.AnimatedIcon;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.menu.MenuBackgroundRenderer;
import tech.sadnes.client.ui.menu.MenuButton;
import tech.sadnes.client.ui.screen.LanguageScreen;
import tech.sadnes.client.ui.screen.options.OptionsCategory;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class SettingsScreen extends Screen implements IRenderable, IMinecraft {
    private static final float CATEGORY_STEP = 18.0F;
    private static final float CATEGORY_GAP = 4.0F;
    private static final float CATEGORY_HEIGHT = 14.0F;
    private static final float SCROLLBAR_GAP = 4.0F;
    private static final int PINNED_CATEGORY_COUNT = 2;
    private static final float SECTION_PADDING = 9.0F;
    private static final float SEARCH_HEIGHT = 16.0F;
    private static final float MIN_WIDTH = 230.0F;
    private static final float MIN_HEIGHT = 170.0F;
    private static final int DANGER_COLOR = -1353883;
    private final Screen parent;
    private final Animation openAnimation = new Animation();
    private final Animation resetDialogAnimation = new Animation();
    private final SmoothScrollbar contentScroll = new SmoothScrollbar().setAutoHide(true);
    private final SmoothScrollbar categoryScroll = new SmoothScrollbar().setAutoHide(true).setMinThumbHeight(10.0F);
    private final List<OptionsCategoryTab> categoryButtons = new ArrayList<>();
    private OptionsCategory selectedCategory = OptionsCategory.ESSENTIALS;
    private List<SettingEntry> visibleEntries = new ArrayList<>();
    private List<SettingEntry> categoryEntries = new ArrayList<>();
    private float[] entryVisibility = new float[0];
    private final List<OptionsCategory> entryCategories = new ArrayList<>();
    private final List<float[]> categoryBadgeBounds = new ArrayList<>();
    private final List<OptionsCategory> categoryBadgeTargets = new ArrayList<>();
    private List<OptionsCategory.Preset> quickToggles = new ArrayList<>();
    private final List<float[]> quickToggleBounds = new ArrayList<>();
    private String searchQuery = "";
    private boolean searchFocused;
    private int selectedIndex = -1;
    private float listY;
    private float listHeight;
    private MenuButton languageButton;
    private MenuButton clickGuiButton;
    private MenuButton resetButton;
    private ResourcePacksPanel resourcePackList;
    private float windowX;
    private float windowY;
    private float windowWidth;
    private float windowHeight;
    private float sidebarX;
    private float sidebarY;
    private float sidebarWidth;
    private float sidebarHeight;
    private float categoryListHeight;
    private float categoryPanelX;
    private float categoryPanelY;
    private float categoryPanelWidth;
    private float categoryPanelHeight;
    private float pinnedCategoriesY;
    private float categoryListY;
    private float contentX;
    private float contentWidth;
    private float searchX;
    private float searchY;
    private float searchWidth;
    private float searchHeight;
    private float contentY;
    private float contentHeight;
    private float contentBottom;
    private float vanillaButtonX;
    private float vanillaButtonY;
    private float vanillaButtonWidth;
    private float vanillaButtonHeight;
    private float closeButtonX;
    private float closeButtonY;
    private float closeButtonSize;
    private final ShimmerText vanillaButtonText = new ShimmerText();
    private final ShimmerText profilesButtonText = new ShimmerText();
    private final Animation vanillaHoverAnimation = new Animation();
    private final Animation profilesHoverAnimation = new Animation();
    private final IconHoverAnimator vanillaIcon = new IconHoverAnimator();
    private final IconHoverAnimator profilesIcon = new IconHoverAnimator();
    private final IconHoverAnimator closeIcon = new IconHoverAnimator();
    private final IconHoverAnimator spareIcon = new IconHoverAnimator();
    private final IconHoverAnimator trashIcon = new IconHoverAnimator();
    private boolean resetDialogOpen;
    private float resetDialogX;
    private float resetDialogY;
    private float resetDialogWidth;
    private float resetDialogHeight;
    private float confirmButtonX;
    private float confirmButtonY;
    private float confirmButtonWidth;
    private float confirmButtonHeight;
    private float cancelButtonX;
    private float cancelButtonY;
    private float cancelButtonWidth;
    private float cancelButtonHeight;
    private boolean profilesDialogOpen;
    private String profileNameInput = "";
    private boolean profileNameFocused;
    private float profilesButtonX;
    private float profilesButtonY;
    private float profilesButtonWidth;
    private float profilesButtonHeight;
    private float profilesDialogX;
    private float profilesDialogY;
    private float profilesDialogWidth;
    private float profilesDialogHeight;
    private final float[] profileNameBounds = new float[4];
    private final float[] saveButtonBounds = new float[4];
    private final List<float[]> profileRowBounds = new ArrayList<>();
    private final List<float[]> profileDeleteBounds = new ArrayList<>();
    private final List<String> profileNames = new ArrayList<>();
    private DropdownWidget activeDropdown;
    private DropdownWidget closingDropdown;
    private final Animation dropdownAnimation = new Animation();
    private final ShimmerText dropdownSelectedText = new ShimmerText();
    private final ShimmerText cancelButtonText = new ShimmerText();
    private final ShimmerText confirmButtonText = new ShimmerText();
    private final Animation searchIconAnimation = new Animation();
    private float dropdownX;
    private float dropdownY;
    private float dropdownWidth;
    private float dropdownHeight;
    private final List<float[]> dropdownOptionBounds = new ArrayList<>();

    public SettingsScreen(Screen screen) {
        super(Text.literal("Settings"));
        this.parent = screen;
    }

    @Override
    public void init() {
        this.openAnimation.forceValue(0.0);
        this.openAnimation.animate(1.0, 0.35, Easings.QUART_OUT);
        this.contentScroll.reset();
        this.categoryScroll.reset();
        if (this.languageButton == null) {
            this.languageButton = new MenuButton(MenuButton.Style.GHOST, Icon.GLOBE, "Выбрать язык", () -> mc.setScreen(new LanguageScreen(this)))
                .withAccentColor(Theme.getPrimary())
                .withChevron(false);
            this.clickGuiButton = new MenuButton(
                    MenuButton.Style.GHOST, Icon.BOLT, "Открыть ClickGui", () -> mc.setScreen(Sadnes.getInstance().getClickGui().withReturnScreen(this))
                )
                .withAccentColor(Theme.getPrimary())
                .withChevron(false);
            this.resetButton = new MenuButton(MenuButton.Style.GHOST, Icon.REFRESH, "Сбросить", this::openResetDialog)
                .withAccentColor(Theme.getPrimary())
                .withChevron(false);
        }

        this.resetDialogOpen = false;
        this.profilesDialogOpen = false;
        this.profileNameFocused = false;
        this.activeDropdown = null;
        this.closingDropdown = null;
        this.dropdownAnimation.forceValue(0.0);
        this.resetDialogAnimation.forceValue(0.0);
        this.initCategoryButtons();
        this.updateLayout();
        this.selectCategory(this.selectedCategory);
    }

    private void initCategoryButtons() {
        this.categoryButtons.clear();

        for (OptionsCategory optionsCategory : OptionsCategory.values()) {
            this.categoryButtons.add(new OptionsCategoryTab(optionsCategory, () -> this.selectCategory(optionsCategory)));
        }
    }

    private void updateLayout() {
        float f = GuiScale.getScaledWidth();
        float f2 = GuiScale.getScaledHeight();
        this.windowWidth = Math.min(Math.max(f * 0.52F, 300.0F), Math.min(430.0F, f - 24.0F));
        this.windowHeight = Math.min(Math.max(f2 * 0.62F, 200.0F), Math.min(250.0F, f2 - 24.0F));
        this.windowX = (f - this.windowWidth) / 2.0F;
        this.windowY = (f2 - this.windowHeight) / 2.0F;
        float f3 = 0.0F;

        for (OptionsCategoryTab optionsCategoryTab : this.categoryButtons) {
            f3 = Math.max(f3, optionsCategoryTab.getPreferredWidth());
        }

        this.sidebarWidth = Math.min(Math.min(Math.max(f3, 90.0F), 150.0F), this.windowWidth * 0.34F);
        this.sidebarX = this.windowX + 12.0F;
        this.sidebarY = this.windowY + 12.0F;
        this.sidebarHeight = this.windowHeight - 12.0F * 2.0F;
        this.pinnedCategoriesY = this.sidebarY + 7.0F;
        float f4 = this.sidebarY + this.sidebarHeight - 16.0F;
        this.resetButton.setBounds(this.sidebarX, f4, this.sidebarWidth, 16.0F);
        this.categoryPanelX = this.sidebarX - 6.0F;
        this.categoryPanelWidth = this.sidebarWidth + 12.0F;
        this.categoryPanelY = this.pinnedCategoriesY + 32.0F + 9.0F;
        this.categoryPanelHeight = f4 - 8.0F - this.categoryPanelY;
        this.categoryListY = this.categoryPanelY + 10.0F;
        this.categoryListHeight = this.categoryPanelHeight - 10.0F * 2.0F;

        for (int i = 0; i < this.categoryButtons.size(); i++) {
            this.categoryButtons.get(i).setBounds(this.sidebarX, this.sidebarY + i * 18.0F, this.sidebarWidth, 14.0F);
        }

        this.contentX = this.sidebarX + this.sidebarWidth + 14.0F;
        this.contentWidth = this.windowX + this.windowWidth - 12.0F - this.contentX;
        this.searchX = this.contentX;
        this.searchY = this.windowY + 12.0F;
        this.searchHeight = 16.0F;
        this.closeButtonSize = 16.0F;
        this.closeButtonX = this.windowX + this.windowWidth - 12.0F - this.closeButtonSize;
        this.closeButtonY = this.searchY;
        this.searchWidth = this.closeButtonX - 8.0F - this.searchX;
        this.contentY = this.searchY + this.searchHeight + 12.0F;
        this.contentBottom = this.windowY + this.windowHeight - 8.0F;
        this.contentHeight = this.windowY + this.windowHeight - 12.0F - this.contentY;
        String string = Language.translate("settings.footer.vanilla", "Ванильные настройки");
        float f5 = Fonts.INTER_MEDIUM != null ? Fonts.INTER_MEDIUM.getWidth(string, 6.0F) : 100.0F;
        this.vanillaButtonWidth = f5 + 28.0F;
        this.vanillaButtonHeight = 14.0F;
        this.vanillaButtonX = f - 10.0F - this.vanillaButtonWidth;
        this.vanillaButtonY = f2 - 10.0F - this.vanillaButtonHeight;
        String string2 = Language.translate("settings.footer.profiles", "Профили");
        float f6 = Fonts.INTER_MEDIUM != null ? Fonts.INTER_MEDIUM.getWidth(string2, 6.0F) : 40.0F;
        this.profilesButtonWidth = f6 + 30.0F;
        this.profilesButtonHeight = this.vanillaButtonHeight;
        this.profilesButtonX = this.vanillaButtonX - 8.0F - this.profilesButtonWidth;
        this.profilesButtonY = this.vanillaButtonY;
    }

    private void selectCategory(OptionsCategory optionsCategory) {
        KeybindOptionWidget.listeningWidget = null;
        this.closeDropdown();
        if (this.selectedCategory == OptionsCategory.RESOURCE_PACKS && this.resourcePackList != null && optionsCategory != OptionsCategory.RESOURCE_PACKS) {
            this.resourcePackList.applyChanges();
            this.resourcePackList.releaseIcons();
            this.resourcePackList = null;
        }

        this.selectedCategory = optionsCategory;
        this.contentScroll.reset();
        if (optionsCategory == OptionsCategory.RESOURCE_PACKS) {
            if (this.resourcePackList == null) {
                this.resourcePackList = new ResourcePacksPanel(mc);
            }

            this.categoryEntries = new ArrayList<>();
            this.quickToggles = optionsCategory.getPresets(mc);
            this.refreshEntries();
        } else {
            this.categoryEntries = optionsCategory.getOptions(mc);
            this.quickToggles = optionsCategory.getPresets(mc);
            this.refreshEntries();
        }
    }

    private boolean isSearching() {
        return this.searchQuery != null && !this.searchQuery.isBlank();
    }

    private void refreshEntries() {
        ArrayList arrayList = new ArrayList();
        this.entryCategories.clear();
        this.selectedIndex = -1;
        if (this.isSearching()) {
            String string = this.searchQuery.toLowerCase(Locale.ROOT);

            for (OptionsCategory optionsCategory : OptionsCategory.values()) {
                if (hasOptionEntries(optionsCategory)) {
                    for (SettingEntry settingEntry : optionsCategory.getOptions(mc)) {
                        if (settingEntry.matchesQuery(string)) {
                            arrayList.add(settingEntry);
                            this.entryCategories.add(optionsCategory);
                        }
                    }
                }
            }
        } else if (this.selectedCategory == OptionsCategory.FAVORITES) {
            Set set = FavoritesPrefs.getAll();

            for (OptionsCategory optionsCategory2 : OptionsCategory.values()) {
                if (hasOptionEntries(optionsCategory2)) {
                    for (SettingEntry settingEntry2 : optionsCategory2.getOptions(mc)) {
                        if (!settingEntry2.isHeader() && set.contains(settingEntry2.getName())) {
                            arrayList.add(settingEntry2);
                            this.entryCategories.add(optionsCategory2);
                        }
                    }
                }
            }
        } else {
            arrayList.addAll(this.categoryEntries);
        }

        this.visibleEntries = arrayList;
    }

    private void openResetDialog() {
        this.resetDialogOpen = true;
        this.closeDropdown();
        this.resetDialogAnimation.forceValue(0.0);
        this.resetDialogAnimation.animate(1.0, 0.22, Easings.QUART_OUT);
    }

    private void openDropdown(DropdownWidget dropdownWidget) {
        if (this.closingDropdown != null && this.closingDropdown != dropdownWidget) {
            this.closingDropdown.setChevronRotation(0.0F);
        }

        this.activeDropdown = dropdownWidget;
        this.closingDropdown = null;
        this.dropdownAnimation.forceValue(0.0);
        this.dropdownAnimation.animate(1.0, 0.22, Easings.QUART_OUT);
    }

    private void closeDropdown() {
        if (this.activeDropdown != null) {
            this.closingDropdown = this.activeDropdown;
            this.activeDropdown = null;
            this.dropdownAnimation.animate(0.0, 0.16, Easings.QUAD_OUT);
        }
    }

    private void resetAllSettings() {
        for (OptionsCategory optionsCategory : OptionsCategory.values()) {
            if (hasOptionEntries(optionsCategory)) {
                Iterator iterator2 = optionsCategory.getOptions(mc).iterator();

                while (iterator2.hasNext()) {
                    ((SettingEntry)iterator2.next()).reset();
                }
            }
        }

        KeyBinding.updateKeysByCode();
        mc.options.write();
        KeybindOptionWidget.listeningWidget = null;
        this.selectCategory(this.selectedCategory);
    }

    private Map<String, String> exportProfile() {
        LinkedHashMap linkedHashMap = new LinkedHashMap();

        for (OptionsCategory optionsCategory : OptionsCategory.values()) {
            if (hasOptionEntries(optionsCategory)) {
                for (SettingEntry settingEntry : optionsCategory.getOptions(mc)) {
                    if (!settingEntry.isHeader()) {
                        String string = settingEntry.getValue();
                        if (string != null) {
                            linkedHashMap.put(optionsCategory.name() + "::" + settingEntry.getName(), string);
                        }
                    }
                }
            }
        }

        return linkedHashMap;
    }

    private void applyProfile(Map<String, String> map) {
        if (map != null) {
            for (OptionsCategory optionsCategory : OptionsCategory.values()) {
                if (hasOptionEntries(optionsCategory)) {
                    for (SettingEntry settingEntry : optionsCategory.getOptions(mc)) {
                        if (!settingEntry.isHeader()) {
                            String string = (String)map.get(optionsCategory.name() + "::" + settingEntry.getName());
                            if (string != null) {
                                settingEntry.setValue(string);
                            }
                        }
                    }
                }
            }

            KeyBinding.updateKeysByCode();
            mc.options.write();
            this.selectCategory(this.selectedCategory);
        }
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        Sadnes.getInstance().saveAll();
        if (Fonts.INTER_MEDIUM != null) {
            int i3 = (int)GuiScale.toScaled(i);
            int i4 = (int)GuiScale.toScaled(i2);
            int i5;
            int i6;
            if (!this.resetDialogOpen && !this.profilesDialogOpen && this.activeDropdown == null) {
                i5 = i3;
                i6 = i4;
            } else {
                i5 = -10000;
                i6 = -10000;
            }

            int i7 = i6;
            this.openAnimation.update();
            float f2 = this.openAnimation.get();
            this.renderWallpaper(drawContext, 1.0F);
            float f3 = GuiScale.getScaledWidth();
            float f4 = GuiScale.getScaledHeight();
            MenuBackgroundRenderer.renderBlurOverlay(drawContext, f3, f4, f2, 1.0F);
            if (!(this.windowWidth < 230.0F) && !(this.windowHeight < 170.0F)) {
                new RenderUtil(drawContext, this.windowX, this.windowY, this.windowWidth, this.windowHeight)
                    .border(74.0F, ColorUtil.BLACK, 0.16F * f2, Radius.all(10.0F));
                new RenderUtil(drawContext, this.windowX, this.windowY, this.windowWidth, this.windowHeight)
                    .border(42.0F, ColorUtil.BLACK, 0.3F * f2, Radius.all(10.0F));
                GuiRenderUtil.window(
                    drawContext, RenderUtil.BlurQuality.MIDDLE, true, this.windowX, this.windowY, this.windowWidth, this.windowHeight, f2 * 0.4F, Radius.all(10.0F)
                );
                new RenderUtil(drawContext, this.windowX, this.windowY, this.windowWidth, this.windowHeight)
                    .rect(Theme.withAlpha(ColorUtil.BLACK, 0.45F * f2), Radius.all(10.0F));
                new RenderUtil(drawContext, this.windowX, this.windowY, this.windowWidth, this.windowHeight)
                    .shadow(0.7F, Theme.withAlpha(ColorUtil.WHITE, 0.05F * f2), Radius.all(10.0F));
                float f5 = (this.sidebarX + this.sidebarWidth + this.contentX) / 2.0F;
                new RenderUtil(drawContext, f5, this.windowY + 14.0F, 1.0F, this.windowHeight - 28.0F)
                    .rect(Theme.withAlpha(ColorUtil.WHITE, 0.07F * f2), Radius.zero());
                this.renderSearchBar(drawContext, f2);
                this.renderCloseButton(drawContext, i5, i7, f2);
                this.renderCategories(drawContext, i5, i7, f2);
                this.resetButton.render(drawContext, i5, i7, f2);
                this.renderContent(drawContext, i5, i7, f2);
                this.renderFooter(drawContext, i5, i7, f2);
                if ((this.activeDropdown != null || this.closingDropdown != null) && !this.resetDialogOpen && !this.profilesDialogOpen) {
                    this.renderDropdown(drawContext, i3, i4, f2);
                }

                if (this.resetDialogOpen) {
                    this.renderResetDialog(drawContext, i3, i4, f2);
                }

                if (this.profilesDialogOpen) {
                    this.renderProfilesDialog(drawContext, i3, i4, f2);
                    return;
                }
            }
        }
    }

    private void renderDropdown(DrawContext drawContext, int i, int i2, float f) {
        this.dropdownOptionBounds.clear();
        DropdownWidget dropdownWidget = this.activeDropdown != null ? this.activeDropdown : this.closingDropdown;
        if (dropdownWidget != null) {
            this.dropdownAnimation.update();
            float f2 = this.dropdownAnimation.get();
            if (this.activeDropdown == null && f2 <= 0.01F) {
                dropdownWidget.setChevronRotation(0.0F);
                this.closingDropdown = null;
            } else {
                List<String> strings = dropdownWidget.getOptionNames();
                int i3 = dropdownWidget.getSelectedIndex();
                float f3 = Math.max(dropdownWidget.getWidth(), 76.0F);

                for (String string : strings) {
                    f3 = Math.max(f3, Fonts.INTER_MEDIUM.getWidth(string, 6.5F) + 9.0F + 18.0F);
                }

                float f4 = strings.size() * 17.0F + Math.max(0, strings.size() - 1) * 3.0F + 3.0F * 2.0F;
                float f5 = dropdownWidget.getX();
                float f6 = dropdownWidget.getY();
                float f7 = dropdownWidget.getHeight();
                float f8 = dropdownWidget.getWidth();
                float f9 = GuiScale.getScaledWidth();
                float f10 = GuiScale.getScaledHeight();
                float f11 = f5 + f8 - f3;
                float f12 = Math.max(2.0F, Math.min(f11, f9 - f3 - 2.0F));
                float f13 = f6 + f7 + 3.0F;
                boolean flag = f13 + f4 > f10 - 4.0F;
                if (flag) {
                    f13 = f6 - 3.0F - f4;
                }

                this.dropdownX = f12;
                this.dropdownY = f13;
                this.dropdownWidth = f3;
                this.dropdownHeight = f4;
                dropdownWidget.setChevronRotation(flag ? -f2 : f2);
                float f14 = f * f2;
                MatrixUtil.push(drawContext);
                MatrixUtil.scale(drawContext, f12 + f3 / 2.0F, flag ? f13 + f4 : f13, 0.94F + 0.06F * f2);
                Radius radius = Radius.all(8.0F);
                new RenderUtil(drawContext, f12, f13, f3, f4).border(20.0F, ColorUtil.BLACK, 0.34F * f14, radius);
                new RenderUtil(drawContext, f12, f13, f3, f4).border(9.0F, ColorUtil.BLACK, 0.3F * f14, radius);
                GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MIDDLE, true, f12, f13, f3, f4, f14 * 0.95F, 8.0F);
                new RenderUtil(drawContext, f12, f13, f3, f4).rect(Theme.withAlpha(ColorUtil.BLACK, 0.31F * f14), radius);
                boolean flag2 = this.activeDropdown != null;

                for (int i4 = 0; i4 < strings.size(); i4++) {
                    float f15 = f13 + 3.0F + i4 * (17.0F + 3.0F);
                    boolean flag3;
                    if (flag2) {
                        flag3 = this.isHovered(i, i2, f12, f15, f3, 17.0F);
                    } else {
                        flag3 = false;
                    }

                    boolean flag4 = i4 == i3;
                    if (flag4) {
                        new RenderUtil(drawContext, f12 + 3.0F, f15, f3 - 3.0F * 2.0F, 17.0F)
                            .rect(Theme.withAlpha(Theme.getPrimary(), (flag3 ? 0.26F : 0.16F) * f14), Radius.all(6.0F));
                    } else if (flag3) {
                        new RenderUtil(drawContext, f12 + 3.0F, f15, f3 - 3.0F * 2.0F, 17.0F)
                            .rect(Theme.withAlpha(ColorUtil.WHITE, 0.07F * f14), Radius.all(6.0F));
                    }

                    int i5 = flag4
                        ? Theme.withAlpha(Theme.getPrimary(), f14)
                        : Theme.withAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), ColorUtil.WHITE, flag3 ? 1.0F : 0.0F), f14);
                    String string2 = (String)strings.get(i4);
                    if (flag4) {
                        string2 = this.dropdownSelectedText.applyThemed(Fonts.INTER_SEMIBOLD, string2, 6.5F, Theme.getPrimary(), i5, 1.0F);
                    }

                    new RenderUtil(drawContext, f12 + 9.0F, f15, f3 - 9.0F - 18.0F, 17.0F)
                        .textMiddleLeft(flag4 ? Fonts.INTER_SEMIBOLD : Fonts.INTER_MEDIUM, string2, 0.0F, 0.0F, i5, 6.5F);
                    if (flag4) {
                        new RenderUtil(drawContext, f12 + f3 - 3.0F - 6.0F - 6.0F, f15 + (17.0F - 6.0F) / 2.0F, 6.0F, 6.0F)
                            .icon(Icon.CHECK, 0.0F, 0.0F, 6.0F, Theme.withAlpha(Theme.getPrimary(), f14));
                    }

                    if (flag2) {
                        this.dropdownOptionBounds.add(new float[]{f12, f15, f3, 17.0F});
                    }
                }

                MatrixUtil.pop(drawContext);
            }
        }
    }

    private void renderResetDialog(DrawContext drawContext, int i, int i2, float f) {
        float f2 = GuiScale.getScaledWidth();
        float f3 = GuiScale.getScaledHeight();
        this.resetDialogAnimation.update();
        float f4 = this.resetDialogAnimation.get();
        float f5 = f * f4;
        new RenderUtil(drawContext, 0.0F, 0.0F, f2, f3).rect(Theme.withAlpha(ColorUtil.BLACK, 0.45F * f5), Radius.zero());
        this.resetDialogWidth = Math.min(f2 * 0.38F, 232.0F);
        this.resetDialogHeight = 104.0F;
        this.resetDialogX = (f2 - this.resetDialogWidth) / 2.0F;
        this.resetDialogY = (f3 - this.resetDialogHeight) / 2.0F;
        Radius radius = Radius.all(12.0F);
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, this.resetDialogX + this.resetDialogWidth / 2.0F, this.resetDialogY + this.resetDialogHeight / 2.0F, 0.94F + 0.06F * f4);
        new RenderUtil(drawContext, this.resetDialogX, this.resetDialogY, this.resetDialogWidth, this.resetDialogHeight)
            .border(26.0F, ColorUtil.BLACK, 0.26F * f5, radius);
        GuiRenderUtil.window(
            drawContext, RenderUtil.BlurQuality.MAX, true, this.resetDialogX, this.resetDialogY, this.resetDialogWidth, this.resetDialogHeight, f5 * 0.9F, radius
        );
        new RenderUtil(drawContext, this.resetDialogX, this.resetDialogY, this.resetDialogWidth, this.resetDialogHeight)
            .rect(Theme.withAlpha(ColorUtil.BLACK, 0.22F * f5), radius);
        new RenderUtil(drawContext, this.resetDialogX, this.resetDialogY + 14.0F, this.resetDialogWidth, 12.0F)
            .textTopCenter(
                Fonts.INTER_SEMIBOLD, Language.translate("settings.reset.title", "Сбросить все настройки?"), Theme.withAlpha(Theme.getTextColor(), f5), 8.5F
            );
        new RenderUtil(drawContext, this.resetDialogX + 12.0F, this.resetDialogY + 30.0F, this.resetDialogWidth - 24.0F, 10.0F)
            .textTopCenter(
                Fonts.INTER_REGULAR,
                Language.translate("settings.reset.subtitle", "Все значения вернутся к стандартным"),
                Theme.withAlpha(Theme.getSecondaryText(), 0.85F * f5),
                6.0F
            );
        float f6 = (this.resetDialogWidth - 14.0F * 2.0F - 8.0F) / 2.0F;
        float f7 = this.resetDialogY + this.resetDialogHeight - 16.0F - 22.0F;
        this.cancelButtonX = this.resetDialogX + 14.0F;
        this.cancelButtonY = f7;
        this.cancelButtonWidth = f6;
        this.cancelButtonHeight = 22.0F;
        this.confirmButtonX = this.cancelButtonX + f6 + 8.0F;
        this.confirmButtonY = f7;
        this.confirmButtonWidth = f6;
        this.confirmButtonHeight = 22.0F;
        this.renderDialogButton(
            drawContext,
            this.cancelButtonX,
            this.cancelButtonY,
            this.cancelButtonWidth,
            this.cancelButtonHeight,
            Language.translate("menu.cancel", "Отмена"),
            Theme.getTextColor(),
            this.isHovered(i, i2, this.cancelButtonX, this.cancelButtonY, this.cancelButtonWidth, this.cancelButtonHeight),
            this.cancelButtonText,
            f5
        );
        this.renderDialogButton(
            drawContext,
            this.confirmButtonX,
            this.confirmButtonY,
            this.confirmButtonWidth,
            this.confirmButtonHeight,
            Language.translate("settings.reset.confirm", "Сбросить"),
            -1353883,
            this.isHovered(i, i2, this.confirmButtonX, this.confirmButtonY, this.confirmButtonWidth, this.confirmButtonHeight),
            this.confirmButtonText,
            f5
        );
        MatrixUtil.pop(drawContext);
    }

    private void renderDialogButton(
        DrawContext drawContext, float f, float f2, float f3, float f4, String string, int i, boolean flag, ShimmerText shimmerText, float f5
    ) {
        new RenderUtil(drawContext, f, f2, f3, f4).rect(Theme.withAlpha(ColorUtil.WHITE, (flag ? 0.11F : 0.06F) * f5), Radius.all(8.0F));
        int i2 = Theme.withAlpha(Theme.getSecondaryText(), f5);
        String string2 = flag ? shimmerText.applyThemed(Fonts.INTER_MEDIUM, string, 6.5F, i, i2, 1.0F) : string;
        new RenderUtil(drawContext, f, f2, f3, f4).textCenter(Fonts.INTER_MEDIUM, string2, i2, 6.5F);
    }

    private void renderProfilesDialog(DrawContext drawContext, int i, int i2, float f) {
        float f2 = GuiScale.getScaledWidth();
        float f3 = GuiScale.getScaledHeight();
        new RenderUtil(drawContext, 0.0F, 0.0F, f2, f3).rect(Theme.withAlpha(ColorUtil.BLACK, 0.3F * f), Radius.zero());
        List list = ProfilesPrefs.getProfileNames();
        int i3 = Math.min(list.size(), 6);
        float f4 = list.isEmpty() ? 16.0F : i3 * 18.0F;
        this.profilesDialogWidth = Math.min(f2 * 0.55F, 320.0F);
        this.profilesDialogHeight = 50.0F + f4 + 12.0F;
        this.profilesDialogX = (f2 - this.profilesDialogWidth) / 2.0F;
        this.profilesDialogY = (f3 - this.profilesDialogHeight) / 2.0F;
        Radius radius = Radius.all(12.0F);
        new RenderUtil(drawContext, this.profilesDialogX, this.profilesDialogY, this.profilesDialogWidth, this.profilesDialogHeight)
            .border(22.0F, ColorUtil.BLACK, 0.22F * f, radius);
        GuiRenderUtil.window(
            drawContext,
            RenderUtil.BlurQuality.MAX,
            true,
            this.profilesDialogX,
            this.profilesDialogY,
            this.profilesDialogWidth,
            this.profilesDialogHeight,
            f * 0.85F,
            radius
        );
        new RenderUtil(drawContext, this.profilesDialogX, this.profilesDialogY, this.profilesDialogWidth, this.profilesDialogHeight)
            .rect(Theme.withAlpha(ColorUtil.BLACK, 0.55F * f), radius);
        new RenderUtil(drawContext, this.profilesDialogX + 12.0F, this.profilesDialogY + 9.0F, this.profilesDialogWidth - 12.0F * 2.0F, 10.0F)
            .textTopLeft(Fonts.INTER_SEMIBOLD, "Профили настроек", 0.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f), 7.5F);
        float f5 = this.profilesDialogY + 26.0F;
        float f6 = this.profilesDialogX + 12.0F;
        float f7 = this.profilesDialogWidth - 12.0F * 2.0F - 62.0F - 6.0F;
        this.profileNameBounds[0] = f6;
        this.profileNameBounds[1] = f5;
        this.profileNameBounds[2] = f7;
        this.profileNameBounds[3] = 18.0F;
        new RenderUtil(drawContext, f6, f5, f7, 18.0F)
            .rect(Theme.withAlpha(ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.05F), 0.85F * f), Radius.all(8.0F));
        boolean flag;
        if (this.profileNameInput.isEmpty()) {
            flag = !this.profileNameFocused;
        } else {
            flag = false;
        }

        String string = flag ? "Имя профиля" : this.profileNameInput + (this.profileNameFocused && isCaretVisible() ? "_" : "");
        int i4 = flag ? Theme.withAlpha(Theme.getSecondaryText(), 0.8F * f) : Theme.withAlpha(Theme.getTextColor(), f);
        new RenderUtil(drawContext, f6 + 6.0F, f5, f7 - 10.0F, 18.0F).textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, i4, 6.5F);
        float f8 = f6 + f7 + 6.0F;
        this.saveButtonBounds[0] = f8;
        this.saveButtonBounds[1] = f5;
        this.saveButtonBounds[2] = 62.0F;
        this.saveButtonBounds[3] = 18.0F;
        boolean flag2 = !this.profileNameInput.isBlank();
        boolean flag3 = this.isHovered(i, i2, f8, f5, 62.0F, 18.0F);
        new RenderUtil(drawContext, f8, f5, 62.0F, 18.0F)
            .rect(Theme.withAlpha(Theme.getPrimary(), (flag2 ? (flag3 ? 0.95F : 0.75F) : 0.25F) * f), Radius.all(8.0F));
        new RenderUtil(drawContext, f8, f5, 62.0F, 18.0F).textCenter(Fonts.INTER_MEDIUM, "Сохранить", Theme.withAlpha(ColorUtil.WHITE, f), 6.5F);
        this.profileRowBounds.clear();
        this.profileDeleteBounds.clear();
        this.profileNames.clear();
        float f9 = this.profilesDialogY + 50.0F;
        if (list.isEmpty()) {
            new RenderUtil(drawContext, f6 + 4.0F, f9, this.profilesDialogWidth - 12.0F * 2.0F, 16.0F)
                .textMiddleLeft(Fonts.INTER_REGULAR, "Пока нет профилей", 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.7F * f), 6.0F);
        } else {
            float f10 = this.profilesDialogWidth - 12.0F * 2.0F;

            for (int i5 = 0; i5 < i3; i5++) {
                String string2 = (String)list.get(i5);
                float f11 = f9 + i5 * 18.0F;
                boolean flag4 = this.isHovered(i, i2, f6, f11, f10 - 18.0F, 16.0);
                if (flag4) {
                    GuiRenderUtil.drawHighlight(drawContext, f6, f11, f10, 16.0F, f, Radius.all(5.0F));
                }

                new RenderUtil(drawContext, f6 + 5.0F, f11, f10 - 24.0F, 16.0F)
                    .textMiddleLeft(Fonts.INTER_MEDIUM, string2, 0.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f), 6.5F);
                float f12 = this.profilesDialogX + this.profilesDialogWidth - 12.0F - 13.0F;
                boolean flag5 = this.isHovered(i, i2, f12 - 2.0F, f11, 17.0, 16.0);
                this.trashIcon
                    .render(
                        drawContext,
                        Icon.TRASH,
                        f12,
                        f11 + 4.5F,
                        7.0F,
                        Theme.withAlpha(flag5 ? -1353883 : Theme.getSecondaryText(), (flag5 ? 1.0F : 0.7F) * f),
                        flag5 ? 1.0F : 0.0F
                    );
                this.profileRowBounds.add(new float[]{f6, f11, f10 - 18.0F, 16.0F});
                this.profileDeleteBounds.add(new float[]{f12 - 2.0F, f11, 17.0F, 16.0F});
                this.profileNames.add(string2);
            }
        }
    }

    private void renderCategories(DrawContext drawContext, int i, int i2, float f) {
        for (int i3 = 0; i3 < 2; i3++) {
            this.categoryButtons.get(i3).setBounds(this.sidebarX, this.pinnedCategoriesY + i3 * 18.0F, this.sidebarWidth, 14.0F);
        }

        for (int i4 = 0; i4 < 2; i4++) {
            OptionsCategoryTab optionsCategoryTab = this.categoryButtons.get(i4);
            optionsCategoryTab.setSelected(optionsCategoryTab.getCategory() == this.selectedCategory);
            optionsCategoryTab.render(drawContext, i, i2, f);
        }

        new RenderUtil(drawContext, this.categoryPanelX, this.categoryPanelY, this.categoryPanelWidth, this.categoryPanelHeight)
            .blur(f, Radius.all(8.0F), RenderUtil.BlurQuality.LOW, true)
            .rect(Theme.withAlpha(ColorUtil.BLACK, 0.3F * f), Radius.all(8.0F))
            .shadow(0.5F, Theme.withAlpha(ColorUtil.WHITE, 0.05F * f), Radius.all(8.0F));
        int i5 = this.categoryButtons.size() - 2;
        float f2 = i5 * 14.0F + Math.max(0, i5 - 1) * 4.0F;
        this.categoryScroll.update(f2, this.categoryListHeight);
        this.categoryScroll.updateHover(i, i2);
        float f3 = this.categoryScroll.getScroll();

        for (int i6 = 2; i6 < this.categoryButtons.size(); i6++) {
            int i7 = i6 - 2;
            this.categoryButtons.get(i6).setBounds(this.sidebarX, this.categoryListY + f3 + i7 * 18.0F, this.sidebarWidth, 14.0F);
        }

        boolean flag = ScissorStack.push(drawContext, this.categoryPanelX, this.categoryListY - 5.0F, this.categoryPanelWidth, this.categoryListHeight + 10.0F);

        try {
            for (int i8 = 2; i8 < this.categoryButtons.size(); i8++) {
                OptionsCategoryTab optionsCategoryTab2 = this.categoryButtons.get(i8);
                optionsCategoryTab2.setSelected(optionsCategoryTab2.getCategory() == this.selectedCategory);
                optionsCategoryTab2.render(drawContext, i, i2, f);
            }
        } finally {
            if (flag) {
                ScissorStack.pop(drawContext);
            }
        }

        this.categoryScroll.render(drawContext, this.categoryPanelX + 2.0F, this.categoryListY, this.categoryListHeight, f2, f);
    }

    private void renderWallpaper(DrawContext drawContext, float f) {
        float f2 = GuiScale.getScaledWidth();
        float f3 = GuiScale.getScaledHeight();
        float f4;
        float f5;
        if (f2 / f3 > 1.7777778F) {
            f4 = f2;
            f5 = f2 / 1.7777778F;
        } else {
            f5 = f3;
            f4 = f3 * 1.7777778F;
        }

        float f6 = (f2 - f4) / 2.0F;
        float f7 = (f3 - f5) / 2.0F;
        GuiRenderUtil.bindTexture(Texture.MENU_BACKGROUND);
        if (GuiRenderUtil.getBoundTexture() != null) {
            new RenderUtil(drawContext, f6, f7, f4, f5).frostedGlass(f, Radius.zero());
        }

        new RenderUtil(drawContext, f6, f7, f4, f5).spriteBlended(AnimatedIcon.BG, 0.0F, 0.0F, f4, f5, Theme.withAlpha(ColorUtil.WHITE, f));
    }

    private void renderCloseButton(DrawContext drawContext, int i, int i2, float f) {
        boolean flag = this.isHovered(i, i2, this.closeButtonX, this.closeButtonY, this.closeButtonSize, this.closeButtonSize);
        if (flag) {
            new RenderUtil(drawContext, this.closeButtonX, this.closeButtonY, this.closeButtonSize, this.closeButtonSize)
                .rect(Theme.withAlpha(ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.12F), 0.7F * f), Radius.all(6.0F));
        }

        float f2 = this.closeButtonSize * 0.46F;
        IconHoverAnimator iconHoverAnimator = this.closeIcon;
        Icon icon = Icon.CLOSE;
        float f3 = this.closeButtonX + (this.closeButtonSize - f2) / 2.0F;
        float f4 = this.closeButtonY + (this.closeButtonSize - f2) / 2.0F;
        int i3 = Theme.withAlpha(flag ? Theme.getTextColor() : Theme.getSecondaryText(), f);
        if (flag) {
            iconHoverAnimator.render(drawContext, icon, f3, f4, f2, i3, 1.0F);
        } else {
            iconHoverAnimator.render(drawContext, icon, f3, f4, f2, i3, 0.0F);
        }
    }

    private void playSearchIconAnimation() {
        float f = this.searchIconAnimation.get();
        if (!this.searchIconAnimation.isAnimating() || !(f > 0.001F) || !(f < 0.999F)) {
            this.searchIconAnimation.forceValue(0.0);
            this.searchIconAnimation.animate(1.0, 0.42, Easings.QUAD_OUT);
        }
    }

    private void renderSearchBar(DrawContext drawContext, float f) {
        new RenderUtil(drawContext, this.searchX, this.searchY, this.searchWidth, this.searchHeight)
            .rect(Theme.withAlpha(ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.05F), 0.85F * f), Radius.all(8.0F));
        this.searchIconAnimation.update();
        float f2 = this.searchIconAnimation.get();
        float f3 = (float)Math.sin(f2 * Math.PI * 2.0) * 14.0F;
        float f4 = (float)Math.sin(f2 * Math.PI) * 0.22F;
        float f5 = this.searchX + 6.0F + 6.0F;
        float f6 = this.searchY + this.searchHeight / 2.0F;
        boolean flag = f2 > 0.001F && f2 < 0.999F;
        if (flag) {
            MatrixUtil.push(drawContext);
            MatrixUtil.rotate(drawContext, f5, f6, f3);
            MatrixUtil.scale(drawContext, f5, f6, 1.0F + f4);
        }

        new RenderUtil(drawContext, this.searchX + 6.0F, this.searchY, 12.0F, this.searchHeight)
            .icon(
                Icon.SEARCH,
                0.0F,
                (this.searchHeight - 9.0F) / 2.0F,
                9.0F,
                Theme.withAlpha(this.searchFocused ? Theme.getTextColor() : Theme.getSecondaryText(), f)
            );
        if (flag) {
            MatrixUtil.pop(drawContext);
        }

        boolean flag2;
        if (this.searchQuery.isEmpty()) {
            flag2 = !this.searchFocused;
        } else {
            flag2 = false;
        }

        String string = flag2 ? "Поиск настройки" : this.searchQuery + (this.searchFocused && isCaretVisible() ? "_" : "");
        int i = flag2 ? Theme.withAlpha(Theme.getSecondaryText(), 0.8F * f) : Theme.withAlpha(Theme.getTextColor(), f);
        new RenderUtil(drawContext, this.searchX + 20.0F, this.searchY, this.searchWidth - 24.0F, this.searchHeight)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, i, 6.5F);
    }

    private static boolean isRedirectCategory(OptionsCategory optionsCategory) {
        return optionsCategory == OptionsCategory.RESOURCE_PACKS || optionsCategory == OptionsCategory.LANGUAGE || optionsCategory == OptionsCategory.FEATURES;
    }

    private static boolean hasOptionEntries(OptionsCategory optionsCategory) {
        return optionsCategory.enabled && !isRedirectCategory(optionsCategory) && optionsCategory != OptionsCategory.ESSENTIALS && optionsCategory != OptionsCategory.FAVORITES;
    }

    private void renderContent(DrawContext drawContext, int i, int i2, float f) {
        this.categoryBadgeBounds.clear();
        this.categoryBadgeTargets.clear();
        if (!this.isSearching() && this.selectedCategory == OptionsCategory.RESOURCE_PACKS) {
            if (this.resourcePackList != null) {
                this.resourcePackList.render(drawContext, this.contentX, this.contentY, this.contentWidth, this.contentHeight, i, i2, f, this.searchQuery);
            }
        } else if (!this.isSearching() && this.selectedCategory == OptionsCategory.LANGUAGE) {
            this.renderRedirectPage(drawContext, i, i2, f, Icon.GLOBE, "Язык игры", "Выбор языка интерфейса Minecraft", this.languageButton);
        } else if (!this.isSearching() && this.selectedCategory == OptionsCategory.FEATURES) {
            this.renderRedirectPage(drawContext, i, i2, f, Icon.BOLT, "Функции клиента", "Модули, ESP и прочее — в ClickGui", this.clickGuiButton);
        } else {
            float f2 = this.renderQuickToggles(drawContext, i, i2, f);
            float f3 = this.contentY + f2;
            float f4 = this.contentHeight - f2;
            this.listY = f3;
            this.listHeight = f4;
            if (this.entryVisibility.length < this.visibleEntries.size()) {
                this.entryVisibility = new float[this.visibleEntries.size()];
            }

            float f5 = 0.0F;
            float f6 = 1.0F;

            for (int i3 = 0; i3 < this.visibleEntries.size(); i3++) {
                SettingEntry settingEntry = this.visibleEntries.get(i3);
                if (settingEntry.isHeader()) {
                    f6 = settingEntry.getExpandProgress();
                    this.entryVisibility[i3] = 1.0F;
                    f5 += 22.0F;
                } else {
                    this.entryVisibility[i3] = f6;
                    f5 += 22.0F * f6;
                }
            }

            this.contentScroll.update(f5, f4);
            this.contentScroll.updateHover(i, i2);
            if (this.visibleEntries.isEmpty()) {
                boolean flag = this.selectedCategory == OptionsCategory.FAVORITES && !this.isSearching();
                String string = flag ? "Нет избранного" : (this.selectedCategory.enabled ? "Ничего не найдено" : "Раздел в разработке");
                new RenderUtil(drawContext, this.contentX, f3, this.contentWidth, f4)
                    .textCenter(Fonts.INTER_MEDIUM, string, Theme.withAlpha(Theme.getSecondaryText(), 0.85F * f), 7.5F);
                if (flag) {
                    new RenderUtil(drawContext, this.contentX, f3 + 14.0F, this.contentWidth, f4)
                        .textCenter(Fonts.INTER_REGULAR, "Отметьте настройки звёздочкой слева", Theme.withAlpha(Theme.getSecondaryText(), 0.55F * f), 6.0F);
                } else if (!this.selectedCategory.enabled) {
                    new RenderUtil(drawContext, this.contentX, f3 + 14.0F, this.contentWidth, f4)
                        .textCenter(Fonts.INTER_REGULAR, "Откройте ванильные настройки ниже", Theme.withAlpha(Theme.getSecondaryText(), 0.55F * f), 6.0F);
                }
            } else {
                boolean flag2 = this.isSearching() || this.selectedCategory == OptionsCategory.FAVORITES;
                boolean flag3 = !this.isSearching();
                boolean flag4 = ScissorStack.push(drawContext, this.contentX, f3, this.contentWidth, f4);

                try {
                    float f7 = this.contentScroll.getScroll();
                    float f8 = this.contentX;
                    float f9 = this.contentWidth - 8.0F;
                    float f10 = f3 + f7;

                    for (int i4 = 0; i4 < this.visibleEntries.size(); i4++) {
                        SettingEntry settingEntry2 = this.visibleEntries.get(i4);
                        boolean flag5 = settingEntry2.isHeader();
                        float f11 = this.entryVisibility[i4];
                        float f12 = 18.0F * (flag5 ? 1.0F : f11);
                        float f13 = f10;
                        f10 += flag5 ? 22.0F : 22.0F * f11;
                        if ((flag5 || !(f11 <= 0.004F)) && !(f13 + f12 < f3) && !(f13 > f3 + f4)) {
                            settingEntry2.setShowFavorite(flag3 && !flag5);
                            float f14 = f * (flag5 ? 1.0F : f11);
                            boolean flag6 = false;
                            if (!flag5 && f11 < 0.999F) {
                                flag6 = ScissorStack.push(drawContext, f8, f13, f9, Math.max(0.1F, f12));
                            }

                            if (flag2 && i4 < this.entryCategories.size()) {
                                this.renderCategoryBadge(drawContext, f8, f13, 13.0F, 18.0F, this.entryCategories.get(i4), f14);
                                this.categoryBadgeBounds.add(new float[]{f8, f13, 13.0F, 18.0F});
                                this.categoryBadgeTargets.add(this.entryCategories.get(i4));
                                settingEntry2.render(drawContext, f8 + 13.0F + 5.0F, f13, f9 - 13.0F - 5.0F, 18.0F, i, i2, f14);
                            } else {
                                settingEntry2.render(drawContext, f8, f13, f9, 18.0F, i, i2, f14);
                            }

                            if (flag6) {
                                ScissorStack.pop(drawContext);
                            }

                            if (i4 == this.selectedIndex) {
                                new RenderUtil(drawContext, f8, f13, f9, 18.0F)
                                    .shadow(1.0F, Theme.withAlpha(Theme.getPrimary(), 0.7F * f), Radius.all(6.0F));
                            }
                        }
                    }
                } finally {
                    if (flag4) {
                        ScissorStack.pop(drawContext);
                    }
                }

                this.contentScroll.render(drawContext, this.contentX + this.contentWidth + 4.0F, f3, f4, f5, f);
            }
        }
    }

    private float renderQuickToggles(DrawContext drawContext, int i, int i2, float f) {
        this.quickToggleBounds.clear();
        if (!this.quickToggles.isEmpty() && !this.isSearching()) {
            float f2 = this.contentY;
            float f3 = this.contentX;

            for (OptionsCategory.Preset preset : this.quickToggles) {
                float f4 = Fonts.INTER_MEDIUM.getWidth(preset.name, 6.0F) + 16.0F;
                if (f3 + f4 > this.contentX + this.contentWidth) {
                    return 16.0F + 8.0F;
                }

                boolean flag = preset.isActive();
                boolean flag2 = this.isHovered(i, i2, f3, f2, f4, 16.0F);
                int i3 = flag ? Theme.withAlpha(Theme.getPrimary(), 0.2F * f) : Theme.withAlpha(ColorUtil.WHITE, (flag2 ? 0.09F : 0.05F) * f);
                new RenderUtil(drawContext, f3, f2, f4, 16.0F).rect(i3, Radius.all(8.0F));
                int i4 = flag ? Theme.withAlpha(Theme.getPrimary(), 0.55F * f) : Theme.withAlpha(ColorUtil.WHITE, (flag2 ? 0.2F : 0.12F) * f);
                new RenderUtil(drawContext, f3, f2, f4, 16.0F).shadow(0.8F, i4, Radius.all(8.0F));
                int i5 = flag
                    ? Theme.withAlpha(Theme.getPrimary(), f)
                    : Theme.withAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), ColorUtil.WHITE, flag2 ? 1.0F : 0.0F), f);
                new RenderUtil(drawContext, f3, f2, f4, 16.0F).textCenter(Fonts.INTER_MEDIUM, preset.name, i5, 6.0F);
                this.quickToggleBounds.add(new float[]{f3, f2, f4, 16.0F});
                f3 += f4 + 6.0F;
            }

            return 16.0F + 8.0F;
        } else {
            return 0.0F;
        }
    }

    private boolean clickQuickToggle(double d, double d2) {
        for (int i = 0; i < this.quickToggleBounds.size() && i < this.quickToggles.size(); i++) {
            float[] floats = this.quickToggleBounds.get(i);
            if (this.isHovered(d, d2, floats[0], floats[1], floats[2], floats[3])) {
                this.quickToggles.get(i).apply();
                mc.options.write();
                this.selectCategory(this.selectedCategory);
                return true;
            }
        }

        return false;
    }

    private void renderCategoryBadge(DrawContext drawContext, float f, float f2, float f3, float f4, OptionsCategory optionsCategory, float f5) {
        float f6 = Math.min(f3, 13.0F);
        float f7 = f2 + (f4 - f6) / 2.0F;
        new RenderUtil(drawContext, f, f7, f6, f6).rect(Theme.withAlpha(Theme.getPrimary(), 0.16F * f5), Radius.all(4.0F));
        float f8 = f6 - 4.0F;
        new RenderUtil(drawContext, f, f7, f6, f6)
            .icon(optionsCategory.icon, (f6 - f8) / 2.0F, (f6 - f8) / 2.0F, f8, Theme.withAlpha(Theme.getPrimary(), 0.95F * f5));
    }

    private void renderRedirectPage(DrawContext drawContext, int i, int i2, float f, Icon icon2, String string, String string2, MenuButton menuButton) {
        float f2 = this.contentX + this.contentWidth / 2.0F;
        float f3 = this.contentY + this.contentHeight / 2.0F;
        new RenderUtil(drawContext, f2 - 14.0F, f3 - 38.0F, 28.0F, 28.0F).icon(icon2, 0.0F, 0.0F, 28.0F, Theme.withAlpha(Theme.getPrimary(), f));
        new RenderUtil(drawContext, this.contentX, f3 + 0.0F, this.contentWidth, 12.0F)
            .textTopCenter(Fonts.INTER_BOLD, string, 0.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), f), 10.0F);
        new RenderUtil(drawContext, this.contentX, f3 + 14.0F, this.contentWidth, 8.0F)
            .textTopCenter(Fonts.INTER_REGULAR, string2, 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.8F * f), 6.5F);
        menuButton.setBounds(f2 - 150.0F / 2.0F, f3 + 30.0F, 150.0F, 18.0F);
        menuButton.render(drawContext, i, i2, f);
    }

    private void renderFooter(DrawContext drawContext, int i, int i2, float f) {
        this.renderFooterButton(
            drawContext,
            this.vanillaButtonX,
            this.vanillaButtonY,
            this.vanillaButtonWidth,
            this.vanillaButtonHeight,
            Icon.SETTINGS,
            Language.translate("settings.footer.vanilla", "Ванильные настройки"),
            this.vanillaButtonText,
            this.vanillaHoverAnimation,
            this.vanillaIcon,
            i,
            i2,
            f
        );
        this.renderFooterButton(
            drawContext,
            this.profilesButtonX,
            this.profilesButtonY,
            this.profilesButtonWidth,
            this.profilesButtonHeight,
            Icon.FOLDER,
            Language.translate("settings.footer.profiles", "Профили"),
            this.profilesButtonText,
            this.profilesHoverAnimation,
            this.profilesIcon,
            i,
            i2,
            f
        );
    }

    private void renderFooterButton(
        DrawContext drawContext,
        float f,
        float f2,
        float f3,
        float f4,
        Icon icon,
        String string,
        ShimmerText shimmerText,
        Animation animation,
        IconHoverAnimator iconHoverAnimator,
        int i,
        int i2,
        float f5
    ) {
        Radius radius = Radius.all(7.0F);
        animation.update();
        animation.animate(this.isHovered(i, i2, f, f2, f3, f4) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f6 = animation.get();
        new RenderUtil(drawContext, f, f2, f3, f4).rect(Theme.withAlpha(ColorUtil.BLACK, 0.72F * f5), radius);
        GuiRenderUtil.window(drawContext, RenderUtil.BlurQuality.MIDDLE, true, f, f2, f3, f4, f5 * 0.55F, 7.0F);
        int i3 = Theme.withAlpha(Theme.getTextColor(), f5);
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, f4);
        iconHoverAnimator.render(drawContext, icon, f + 9.0F, f2 + (f4 - 7.0F) / 2.0F, 7.0F, i3, f6);
        String string2 = shimmerText.applyThemed(Fonts.INTER_MEDIUM, string, 6.0F, Theme.getPrimary(), i3, 1.0F);
        renderUtil.textMiddleLeft(Fonts.INTER_MEDIUM, string2, 9.0F + 7.0F + 5.0F, 0.0F, i3, 6.0F);
    }

    private static boolean isCaretVisible() {
        return System.currentTimeMillis() % 1000L < 500L;
    }

    private Click scaleClick(Click click) {
        return new Click(GuiScale.toScaled(click.x()), GuiScale.toScaled(click.y()), click.buttonInfo());
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        Click click2 = this.scaleClick(click);
        double d = click2.x();
        double d2 = click2.y();
        if (this.resetDialogOpen) {
            if (this.isHovered(d, d2, this.confirmButtonX, this.confirmButtonY, this.confirmButtonWidth, this.confirmButtonHeight)) {
                this.resetDialogOpen = false;
                this.resetAllSettings();
                return true;
            }

            if (this.isHovered(d, d2, this.cancelButtonX, this.cancelButtonY, this.cancelButtonWidth, this.cancelButtonHeight)) {
                this.resetDialogOpen = false;
                return true;
            }

            if (!this.isHovered(d, d2, this.resetDialogX, this.resetDialogY, this.resetDialogWidth, this.resetDialogHeight)) {
                this.resetDialogOpen = false;
            }
        } else if (this.profilesDialogOpen) {
            if (this.isHovered(d, d2, this.profileNameBounds[0], this.profileNameBounds[1], this.profileNameBounds[2], this.profileNameBounds[3])) {
                this.profileNameFocused = true;
                return true;
            }

            this.profileNameFocused = false;
            if (this.isHovered(d, d2, this.saveButtonBounds[0], this.saveButtonBounds[1], this.saveButtonBounds[2], this.saveButtonBounds[3])) {
                if (!this.profileNameInput.isBlank()) {
                    ProfilesPrefs.saveProfile(this.profileNameInput, this.exportProfile());
                    this.profileNameInput = "";
                    return true;
                }
            } else {
                for (int i = 0; i < this.profileDeleteBounds.size(); i++) {
                    float[] floats = this.profileDeleteBounds.get(i);
                    if (this.isHovered(d, d2, floats[0], floats[1], floats[2], floats[3])) {
                        ProfilesPrefs.deleteProfile(this.profileNames.get(i));
                        return true;
                    }
                }

                for (int i2 = 0; i2 < this.profileRowBounds.size(); i2++) {
                    float[] floats2 = this.profileRowBounds.get(i2);
                    if (this.isHovered(d, d2, floats2[0], floats2[1], floats2[2], floats2[3])) {
                        this.applyProfile(ProfilesPrefs.getProfile(this.profileNames.get(i2)));
                        this.profilesDialogOpen = false;
                        return true;
                    }
                }

                if (!this.isHovered(d, d2, this.profilesDialogX, this.profilesDialogY, this.profilesDialogWidth, this.profilesDialogHeight)) {
                    this.profilesDialogOpen = false;
                    return true;
                }
            }
        } else {
            if (this.activeDropdown != null) {
                for (int i3 = 0; i3 < this.dropdownOptionBounds.size(); i3++) {
                    float[] floats3 = this.dropdownOptionBounds.get(i3);
                    if (this.isHovered(d, d2, floats3[0], floats3[1], floats3[2], floats3[3])) {
                        this.activeDropdown.selectIndex(i3);
                        this.closeDropdown();
                        return true;
                    }
                }

                this.closeDropdown();
                return true;
            }

            if (this.closingDropdown != null && this.isHovered(d, d2, this.dropdownX, this.dropdownY, this.dropdownWidth, this.dropdownHeight)) {
                return true;
            }

            if (KeybindOptionWidget.listeningWidget != null) {
                KeybindOptionWidget.listeningWidget.onMouseButton(click2.button());
                return true;
            }

            if (this.isHovered(d, d2, this.closeButtonX, this.closeButtonY, this.closeButtonSize, this.closeButtonSize)) {
                mc.setScreen(this.parent);
                return true;
            }

            boolean flag2 = this.isHovered(d, d2, this.searchX, this.searchY, this.searchWidth, this.searchHeight);
            this.searchFocused = flag2;
            if (flag2) {
                this.playSearchIconAnimation();
                return true;
            }

            if (this.categoryScroll.mouseClicked(d, d2)) {
                return true;
            }

            if (this.resetButton.mouseClicked(d, d2)) {
                return true;
            }

            for (int i4 = 0; i4 < 2; i4++) {
                if (this.categoryButtons.get(i4).mouseClicked(d, d2)) {
                    return true;
                }
            }

            if (this.isHovered(d, d2, this.categoryPanelX, this.categoryPanelY, this.categoryPanelWidth, this.categoryPanelHeight)) {
                for (int i5 = 2; i5 < this.categoryButtons.size(); i5++) {
                    if (this.categoryButtons.get(i5).mouseClicked(d, d2)) {
                        return true;
                    }
                }
            }

            if (this.isHovered(d, d2, this.vanillaButtonX, this.vanillaButtonY, this.vanillaButtonWidth, this.vanillaButtonHeight)) {
                mc.setScreen(new OptionsScreen(this, mc.options));
                return true;
            }

            if (this.isHovered(d, d2, this.profilesButtonX, this.profilesButtonY, this.profilesButtonWidth, this.profilesButtonHeight)) {
                this.profilesDialogOpen = true;
                this.profileNameFocused = false;
                this.profileNameInput = "";
                this.closeDropdown();
                return true;
            }

            if (!this.isSearching()) {
                if (this.selectedCategory == OptionsCategory.RESOURCE_PACKS) {
                    if (this.resourcePackList != null
                        && this.isHovered(d, d2, this.contentX, this.contentY, this.contentWidth + 12.0F, this.contentHeight)
                        && this.resourcePackList.mouseClicked(d, d2)) {
                        return true;
                    }

                    return false;
                }

                if (this.selectedCategory == OptionsCategory.LANGUAGE) {
                    return this.languageButton.mouseClicked(d, d2);
                }

                if (this.selectedCategory == OptionsCategory.FEATURES) {
                    return this.clickGuiButton.mouseClicked(d, d2);
                }
            }

            if (this.clickQuickToggle(d, d2)) {
                return true;
            }

            if (this.isHovered(d, d2, this.contentX, this.contentY, this.contentWidth + 12.0F, this.contentHeight)) {
                for (int i6 = 0; i6 < this.categoryBadgeBounds.size() && i6 < this.categoryBadgeTargets.size(); i6++) {
                    float[] floats4 = this.categoryBadgeBounds.get(i6);
                    if (this.isHovered(d, d2, floats4[0], floats4[1], floats4[2], floats4[3])) {
                        this.searchQuery = "";
                        this.searchFocused = false;
                        this.selectCategory(this.categoryBadgeTargets.get(i6));
                        return true;
                    }
                }

                if (this.contentScroll.mouseClicked(d, d2)) {
                    return true;
                }

                for (SettingEntry settingEntry : this.visibleEntries) {
                    if (!settingEntry.isHeader() && settingEntry.getWidget() instanceof DropdownWidget dropdownWidget && dropdownWidget.isHeaderHovered(d, d2)) {
                        this.openDropdown(dropdownWidget);
                        return true;
                    }

                    if (settingEntry.clickFavorite(d, d2)) {
                        if (this.selectedCategory == OptionsCategory.FAVORITES) {
                            this.refreshEntries();
                            return true;
                        }

                        return true;
                    }

                    if (settingEntry.clickReset(d, d2)) {
                        KeyBinding.updateKeysByCode();
                        mc.options.write();
                        return true;
                    }

                    if (settingEntry.mouseClicked(d, d2)) {
                        if (settingEntry.isHeader()) {
                            this.refreshEntries();
                            return true;
                        }

                        return true;
                    }
                }
            }

            return false;
        }

        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (!this.resetDialogOpen && !this.profilesDialogOpen) {
            if (this.activeDropdown != null) {
                return true;
            }

            Click click2 = this.scaleClick(click);
            double d3 = click2.x();
            double d4 = click2.y();
            if (this.categoryScroll.mouseDragged(d3, d4)) {
                return true;
            }

            if (!this.isSearching() && this.selectedCategory == OptionsCategory.RESOURCE_PACKS) {
                return this.resourcePackList != null && this.resourcePackList.mouseDragged(d3, d4);
            }

            if (this.contentScroll.mouseDragged(d3, d4)) {
                return true;
            }

            for (SettingEntry settingEntry : this.visibleEntries) {
                if (settingEntry.mouseReleased(d3, d4)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (this.resourcePackList != null) {
            this.resourcePackList.mouseReleased();
        }

        this.contentScroll.mouseReleased();
        this.categoryScroll.mouseReleased();

        for (SettingEntry settingEntry : this.visibleEntries) {
            settingEntry.update();
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (!this.resetDialogOpen && !this.profilesDialogOpen) {
            if (this.activeDropdown != null) {
                this.closeDropdown();
                return true;
            }

            double d5 = GuiScale.toScaled(d);
            double d6 = GuiScale.toScaled(d2);
            if (this.isHovered(d5, d6, this.categoryPanelX, this.categoryPanelY, this.categoryPanelWidth, this.categoryPanelHeight)) {
                this.categoryScroll.onScroll(d4);
                return true;
            }

            if (!this.isHovered(d5, d6, this.contentX, this.contentY, this.contentWidth + 12.0F, this.contentHeight)) {
                return super.mouseScrolled(d, d2, d3, d4);
            }

            if (this.isSearching() || this.selectedCategory != OptionsCategory.RESOURCE_PACKS) {
                this.contentScroll.onScroll(d4);
                return true;
            }

            if (this.resourcePackList != null) {
                this.resourcePackList.mouseScrolled(d4);
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int i = keyInput.key();
        if (this.resetDialogOpen) {
            switch (i) {
                case 256:
                    this.resetDialogOpen = false;
                    break;
                case 257:
                case 335:
                    this.resetDialogOpen = false;
                    this.resetAllSettings();
                    return true;
            }
        } else if (this.profilesDialogOpen) {
            if (this.profileNameFocused) {
                switch (i) {
                    case 256:
                        this.profileNameFocused = false;
                        return true;
                    case 257:
                    case 335:
                        if (!this.profileNameInput.isBlank()) {
                            ProfilesPrefs.saveProfile(this.profileNameInput, this.exportProfile());
                            this.profileNameInput = "";
                        }

                        this.profileNameFocused = false;
                        return true;
                    case 259:
                        if (!this.profileNameInput.isEmpty()) {
                            this.profileNameInput = this.profileNameInput.substring(0, this.profileNameInput.length() - 1);
                            return true;
                        }
                }
            } else if (i == 256) {
                this.profilesDialogOpen = false;
                return true;
            }
        } else {
            if (KeybindOptionWidget.listeningWidget != null) {
                KeybindOptionWidget.listeningWidget.onKey(keyInput);
                return true;
            }

            if (this.searchFocused) {
                switch (i) {
                    case 256:
                    case 257:
                    case 335:
                        this.searchFocused = false;
                        return true;
                    case 259:
                        if (!this.searchQuery.isEmpty()) {
                            this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                            this.refreshEntries();
                            return true;
                        }
                }
            } else {
                if (this.activeDropdown == null) {
                    if (this.handleNavigationKey(i)) {
                        return true;
                    }

                    if (i == 256) {
                        mc.setScreen(this.parent);
                        return true;
                    }

                    return super.keyPressed(keyInput);
                }

                if (i == 256) {
                    this.closeDropdown();
                    return true;
                }
            }
        }

        return true;
    }

    private boolean handleNavigationKey(int i) {
        switch (i) {
            case 258:
            case 264:
                this.moveSelection(1);
                return true;
            case 265:
                this.moveSelection(-1);
                return true;
            default:
                if (this.selectedIndex >= 0 && this.selectedIndex < this.visibleEntries.size()) {
                    SettingEntry settingEntry = this.visibleEntries.get(this.selectedIndex);
                    byte b;
                    switch (i) {
                        case 32:
                        case 257:
                        case 335:
                            b = 0;
                            break;
                        case 262:
                            b = 1;
                            break;
                        case 263:
                            b = -1;
                            break;
                        default:
                            return false;
                    }

                    boolean flag = settingEntry.keyPressed(b);
                    if (flag && settingEntry.isHeader()) {
                        this.refreshEntries();
                    }

                    return flag;
                } else {
                    return false;
                }
        }
    }

    private void moveSelection(int i) {
        int i2 = this.visibleEntries.size();
        if (i2 == 0) {
            this.selectedIndex = -1;
        } else {
            int i3 = this.selectedIndex >= 0 ? this.selectedIndex : (i > 0 ? -1 : 0);

            for (int i4 = 0; i4 < i2; i4++) {
                i3 = (i3 + i + i2) % i2;
                if (!this.visibleEntries.get(i3).isHeader()) {
                    this.selectedIndex = i3;
                    this.scrollToSelection();
                    return;
                }
            }

            this.selectedIndex = -1;
        }
    }

    private void scrollToSelection() {
        if (this.selectedIndex >= 0) {
            float f = this.selectedIndex * 22.0F;
            float f2 = f + this.contentScroll.getScroll();
            if (f2 < 0.0F) {
                this.contentScroll.setScroll(-f);
            } else {
                if (f2 + 18.0F > this.listHeight) {
                    this.contentScroll.setScroll(this.listHeight - 18.0F - f);
                }
            }
        }
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (this.profilesDialogOpen) {
            if (this.profileNameFocused) {
                char c = (char)charInput.codepoint();
                if (this.profileNameInput.length() < 24 && c >= ' ') {
                    this.profileNameInput = this.profileNameInput + c;
                }
            }

            return true;
        } else {
            if (KeybindOptionWidget.listeningWidget != null) {
                return true;
            }

            if (this.searchFocused) {
                char c2 = (char)charInput.codepoint();
                if (this.searchQuery.length() < 32 && c2 >= ' ') {
                    this.searchQuery = this.searchQuery + c2;
                    this.refreshEntries();
                    return true;
                } else {
                    return false;
                }
            } else {
                return super.charTyped(charInput);
            }
        }
    }

    @Override
    public void removed() {
        KeybindOptionWidget.listeningWidget = null;
        if (this.resourcePackList != null) {
            this.resourcePackList.applyChanges();
            this.resourcePackList.releaseIcons();
            this.resourcePackList = null;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
