package tech.sadnes.client.ui.clickgui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.compress.utils.Lists;
import org.joml.Matrix3x2fStack;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.CategoryDescriptor;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.Module;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.hud.ClickGui;
import tech.sadnes.client.feature.impl.misc.QuickInteract;
import tech.sadnes.client.feature.impl.misc.ServerAssist;
import tech.sadnes.client.feature.impl.render.Cosmetics;
import tech.sadnes.client.feature.impl.render.Esp;
import tech.sadnes.client.feature.impl.render.ambience.Ambience;
import tech.sadnes.client.figura.FiguraAvatarManager;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.VerticalFadeMask;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.AnimatedIcon;
import tech.sadnes.client.render.texture.AvatarTextureLoader;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.script.ScriptCategory;
import tech.sadnes.client.script.ScriptFeature;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.script.editor.ScriptWindowManager;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.SettingHolder;
import tech.sadnes.client.setting.ValueSetting;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.component.BindPopupComponent;
import tech.sadnes.client.ui.clickgui.component.ModuleRowComponent;
import tech.sadnes.client.ui.clickgui.component.NeuroPanel;
import tech.sadnes.client.ui.clickgui.component.OptionRowGroup;
import tech.sadnes.client.ui.clickgui.component.ProfilePopup;
import tech.sadnes.client.ui.clickgui.component.SettingsPopupComponent;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;
import tech.sadnes.client.ui.clickgui.config.ConfigSelectorComponent;
import tech.sadnes.client.ui.clickgui.hotkeys.HotkeysPanel;
import tech.sadnes.client.ui.clickgui.page.AutoBuyPage;
import tech.sadnes.client.ui.clickgui.page.CollectorPage;
import tech.sadnes.client.ui.clickgui.page.EventsPage;
import tech.sadnes.client.ui.clickgui.page.PartyPage;
import tech.sadnes.client.ui.clickgui.panel.AmbiencePanel;
import tech.sadnes.client.ui.clickgui.panel.ChatPanel;
import tech.sadnes.client.ui.clickgui.panel.CosmeticsPanel;
import tech.sadnes.client.ui.clickgui.panel.ServerAssistPanel;
import tech.sadnes.client.ui.clickgui.script.ScriptEditor;
import tech.sadnes.client.ui.clickgui.script.ScriptPanelOverlay;
import tech.sadnes.client.ui.clickgui.script.ScriptsPage;
import tech.sadnes.client.ui.clickgui.search.SearchPanel;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.ui.hub.HubCategory;
import tech.sadnes.client.ui.hub.UtilitiesSubCategory;
import tech.sadnes.client.ui.hub.VisualsSubCategory;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.SubscriptionUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.StopWatch;
import ua.mintantileak.profile.Profile;

public final class ClickGuiScreen extends Screen implements IInteractable, IRenderable, ModuleRowComponent.RowActionListener, IMinecraft {
    private static final float COLUMN_GAP = 12.0F;
    private static final float PADDING = 4.0F;
    private static final float SPACING = 8.0F;
    private static final float SLIDE_OFFSET = 30.0F;
    private static final float FADE_HEIGHT = 18.0F;
    private static final float MIN_VISIBLE_SIZE = 100.0F;
    private final Set<Integer> heldKeys = new HashSet<>();
    private final Animation openAnimation = new Animation();
    private final Animation categorySwitchAnimation = new Animation();
    private float lastScale;
    private boolean closing;
    private final List<ModuleRowComponent> rows = Lists.newArrayList();
    private final Map<Module, ModuleRowComponent> rowsBySource = new IdentityHashMap<>();
    private final Map<CategoryDescriptor, List<OptionRowGroup>> groupsByCategory = new HashMap<>();
    private OptionRowGroup espGroup = new OptionRowGroup(null);
    private List<OptionRowGroup> visibleGroups = List.of();
    private final SmoothScrollbar scrollbar = new SmoothScrollbar();
    private final Map<CategoryDescriptor, Float> scrollByCategory = new HashMap<>();
    private final ConfigSelectorComponent configSelector = new ConfigSelectorComponent();
    private final CategorySidebar sidebar = new CategorySidebar(this);
    private final EspEditorPanel espEditorPanel = new EspEditorPanel(this);
    private final SearchPanel searchPanel = new SearchPanel(this);
    private final SettingsPopupComponent settingsPopup = new SettingsPopupComponent();
    private final AmbiencePanel ambiencePanel = new AmbiencePanel();
    private final HotkeysPanel hotkeysPanel = new HotkeysPanel();
    private final CosmeticsPanel cosmeticsPanel = new CosmeticsPanel();
    private final ServerAssistPanel serverAssistPanel = new ServerAssistPanel();
    private final NeuroPanel neuroPanel = new NeuroPanel();
    private final ScriptPanelOverlay hudStylePanel = new ScriptPanelOverlay();
    private final AutoBuyPage autoBuyPage = new AutoBuyPage(this);
    private final CollectorPage collectorPage = new CollectorPage(this);
    private final EventsPage eventsPage = new EventsPage(this);
    private final PartyPage partyPage = new PartyPage(this);
    private final ScriptEditor scriptEditor = new ScriptEditor(this);
    private final ScriptWindowManager scriptWindowManager = new ScriptWindowManager();
    private final StopWatch reloadTimer = new StopWatch();
    private final Animation reloadAnimation = new Animation();
    private boolean reloading;
    private final ScriptsPage scriptsPage = new ScriptsPage(this, script2 -> this.scriptEditor.openScript(script2));
    private final ProfilePopup profilePopup = new ProfilePopup();
    private final ChatPanel profileMenu = new ChatPanel();
    private float profileX;
    private float profileY;
    private float profileWidth;
    private float profileHeight;
    private BindPopupComponent bindPopup;
    private Object bindTarget;
    private static final float LOGO_HITBOX_SIZE = 24.0F;
    private static final float LOGO_MARGIN = 8.0F;
    private static final float LOGO_PRESS_SHRINK = 0.2F;
    private static final float LOGO_PRESS_ROTATION = (float)Math.toRadians(-7.0);
    private static final long LOGO_DWELL_MS = 1100L;
    private final Animation logoSpinAnimation = new Animation();
    private final Animation logoSlideAnimation = new Animation();
    private final Animation logoPressAnimation = new Animation();
    private ClickGuiScreen.LogoAnimState logoAnimState = ClickGuiScreen.LogoAnimState.IDLE;
    private long logoDwellStart;
    private boolean logoSecondPass;
    private float logoX;
    private float logoY;
    private final IntroOverlay introOverlay = new IntroOverlay();
    private float contentX;
    private float contentY;
    private float contentWidth;
    private float contentHeight;
    private float contentTotalHeight;
    private float espEditorX;
    private float espEditorY;
    private float espEditorWidth;
    private float espEditorHeight;
    private float x;
    private float y;
    private float width = 380.0F;
    private float height = 290.0F;
    private boolean positioned;
    private boolean dragging;
    private ClickGuiScreen.BlurMode blurMode = ClickGuiScreen.BlurMode.NONE;
    private float dragOffsetX;
    private float dragOffsetY;
    private Screen returnScreen;

    private void captureHeldKeys() {
        this.heldKeys.clear();

        for (Key key : Key.values()) {
            int i = key.getKeyCode();
            if (i > 0 && Key.isPressed(i)) {
                this.heldKeys.add(i);
                int i2 = Key.getModifier(i);
                if (i2 != Key.KEY_NONE.getKeyCode()) {
                    this.heldKeys.add(i2);
                }
            }
        }
    }

    @Override
    public void renderBackground(DrawContext drawContext, int i, int i2, float f) {
    }

    private boolean isCloseKey(int i) {
        switch (i) {
            case 256:
            case 260:
            case 261:
            case 344:
                return true;
            default:
                Feature feature = Sadnes.getInstance().getFeatureManager().get(ClickGui.class);
                return feature != null && feature.isBoundTo(i);
        }
    }

    public ClickGuiScreen() {
        super(Text.literal("Sadnes ClickGUI"));
    }

    public void ensureRows() {
        if (this.rows.isEmpty()) {
            Sadnes.getInstance().getFeatureManager().sequencedValues().stream().sorted(Comparator.<Feature, Integer>comparing(feature2 -> {
                Subcategory subcategory = feature2.getSubcategory();
                return subcategory == null ? Integer.MAX_VALUE : subcategory.ordinal();
            }).thenComparing(feature3 -> feature3.getInfo().name(), String.CASE_INSENSITIVE_ORDER)).forEach(feature4 -> {
                if (!feature4.isHidden() && feature4.isAccessible()) {
                    ModuleRowComponent moduleRowComponent = new ModuleRowComponent(feature4, this);
                    this.rows.add(moduleRowComponent);
                    this.rowsBySource.put(feature4, moduleRowComponent);
                }
            });
            ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
            if (scriptManager != null) {
                for (ScriptFeature scriptFeature : scriptManager.getActiveHudElements()) {
                    ModuleRowComponent moduleRowComponent2 = new ModuleRowComponent(scriptFeature, this);
                    this.rows.add(moduleRowComponent2);
                    this.rowsBySource.put(scriptFeature, moduleRowComponent2);
                }

                this.sidebar.setScriptCategories(scriptManager.getActiveHudStyles());
            }

            this.rebuildGroups();
        }
    }

    public void rebuildRows() {
        if (this.settingsPopup.isOpen() && this.settingsPopup.getHolder() instanceof ScriptFeature) {
            this.settingsPopup.close();
        }

        this.rows.clear();
        this.rowsBySource.clear();
        this.ensureRows();
        Iterator iterator2 = this.rows.iterator();

        while (iterator2.hasNext()) {
            ((ModuleRowComponent)iterator2.next()).reset();
        }

        Sadnes.getInstance().getFeatureManager().refreshSearchIndex();
    }

    private void rebuildGroups() {
        this.groupsByCategory.clear();
        this.espGroup = new OptionRowGroup(null);

        for (ModuleRowComponent moduleRowComponent : this.rows) {
            if (moduleRowComponent.getModule() instanceof Esp) {
                this.espGroup.addRow(moduleRowComponent);
            }
        }

        for (ModuleRowComponent moduleRowComponent2 : this.rows) {
            if (moduleRowComponent2.getModule() instanceof Cosmetics) {
                this.espGroup.addRow(moduleRowComponent2);
            }
        }

        for (Category category : Category.values()) {
            ArrayList<OptionRowGroup> optionRowGroups = new ArrayList<>();
            IdentityHashMap identityHashMap = new IdentityHashMap();

            for (Subcategory subcategory : Subcategory.getByCategory(category)) {
                OptionRowGroup optionRowGroup = new OptionRowGroup(subcategory);
                identityHashMap.put(subcategory, optionRowGroup);
                optionRowGroups.add(optionRowGroup);
            }

            LinkedHashMap<String, OptionRowGroup> linkedHashMap = new LinkedHashMap<>();
            OptionRowGroup optionRowGroup2 = new OptionRowGroup(null);

            for (ModuleRowComponent moduleRowComponent3 : this.rows) {
                Module module = moduleRowComponent3.getModule();
                if (module.getCategory() == category && !(module instanceof Esp) && !(module instanceof Cosmetics)) {
                    Subcategory subcategory2 = module.getSubcategory();
                    String string = module.getSubcategoryName();
                    if (subcategory2 != null && subcategory2 != Subcategory.NONE) {
                        ((OptionRowGroup)identityHashMap.get(subcategory2)).addRow(moduleRowComponent3);
                    } else if (string.isEmpty()) {
                        optionRowGroup2.addRow(moduleRowComponent3);
                    } else {
                        linkedHashMap.computeIfAbsent(string, string2 -> new OptionRowGroup(null, string2)).addRow(moduleRowComponent3);
                    }
                }
            }

            optionRowGroups.addAll(linkedHashMap.values());
            optionRowGroups.removeIf(optionRowGroup3 -> optionRowGroup3.getRows().isEmpty());
            if (!optionRowGroup2.getRows().isEmpty()) {
                optionRowGroups.add(optionRowGroup2);
            }

            this.groupsByCategory.put(category, optionRowGroups);
        }

        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null) {
            for (ScriptCategory scriptCategory : scriptManager.getActiveHudStyles()) {
                LinkedHashMap<String, OptionRowGroup> linkedHashMap2 = new LinkedHashMap<>();
                OptionRowGroup optionRowGroup4 = new OptionRowGroup(null);

                for (ModuleRowComponent moduleRowComponent4 : this.rows) {
                    Module module2 = moduleRowComponent4.getModule();
                    if (module2.getCategory() == scriptCategory) {
                        String string3 = module2.getSubcategoryName();
                        if (string3.isEmpty()) {
                            optionRowGroup4.addRow(moduleRowComponent4);
                        } else {
                            linkedHashMap2.computeIfAbsent(string3, string4 -> new OptionRowGroup(null, string4)).addRow(moduleRowComponent4);
                        }
                    }
                }

                ArrayList<OptionRowGroup> optionRowGroups2 = new ArrayList<>(linkedHashMap2.values());
                if (!optionRowGroup4.getRows().isEmpty()) {
                    optionRowGroups2.add(optionRowGroup4);
                }

                this.groupsByCategory.put(scriptCategory, optionRowGroups2);
            }
        }
    }

    public void onCategoryChanged(CategoryDescriptor categoryDescriptor, CategoryDescriptor categoryDescriptor2) {
        if (this.searchPanel.isOpen()) {
            this.searchPanel.setOpen(false);
        }

        this.autoBuyPage.onClose();
        this.collectorPage.onOpen();
        this.scrollByCategory.put(categoryDescriptor, this.scrollbar.getTargetScroll());
        this.scrollbar.setScroll(this.scrollByCategory.getOrDefault(categoryDescriptor2, 0.0F));
        if (this.settingsPopup.isOpen()) {
            SettingHolder settingHolder = this.settingsPopup.getHolder();
            if (settingHolder instanceof Module && ((Module)settingHolder).getCategory() != categoryDescriptor2) {
                this.settingsPopup.close();
            }
        }

        this.closeSidePanels();
        this.playCategorySwitchAnimation();
    }

    public void onHubCategoryChanged() {
        if (this.searchPanel.isOpen()) {
            this.searchPanel.setOpen(false);
        }

        this.autoBuyPage.onClose();
        this.collectorPage.onOpen();
        this.eventsPage.onOpen();
        this.partyPage.onOpen();
        this.scriptsPage.reset();
        this.scriptEditor.close();
        this.settingsPopup.close();
        this.closeSidePanels();
        this.playCategorySwitchAnimation();
    }

    private void closeSidePanels() {
        this.scriptEditor.close();
        this.ambiencePanel.close();
        this.hudStylePanel.close();
        this.hotkeysPanel.close();
        this.cosmeticsPanel.close();
        this.serverAssistPanel.close();
        this.neuroPanel.close();
        this.closeBindPopup();
    }

    public void resetScroll() {
        this.scrollbar.setScroll(0.0F);
        this.playCategorySwitchAnimation();
    }

    public void toggleProfileMenu() {
        if (this.profileMenu.isOpen()) {
            this.profileMenu.close();
        } else {
            this.profileMenu.open();
            this.profilePopup.close();
        }
    }

    private void playCategorySwitchAnimation() {
        this.categorySwitchAnimation.forceValue(0.0);
        this.categorySwitchAnimation.animate(1.0, 0.34, Easings.QUAD_OUT);
    }

    @Override
    public void init() {
        this.captureHeldKeys();
        this.ensureRows();
        this.closing = false;
        this.openAnimation.setFromValue(0.0);
        this.openAnimation.animate(1.0, 0.3F, Easings.QUART_OUT);
        this.categorySwitchAnimation.forceValue(1.0);
        float f = GuiScale.getScaledWidth();
        float f2 = GuiScale.getScaledHeight();
        if (!this.positioned) {
            this.x = (f - this.width) / 2.0F;
            this.y = (f2 - this.height) / 2.0F;
            this.positioned = true;
        }

        this.logoSpinAnimation.forceValue(1.0);
        this.logoSlideAnimation.forceValue(0.0);
        this.logoPressAnimation.forceValue(0.0);
        this.logoAnimState = ClickGuiScreen.LogoAnimState.IDLE;
        Iterator iterator2 = this.rows.iterator();

        while (iterator2.hasNext()) {
            ((ModuleRowComponent)iterator2.next()).reset();
        }

        int i = (int)f;
        int i2 = (int)f2;
        this.sidebar.resize(i, i2);
        this.configSelector.resize(i, i2);
        this.searchPanel.init(i, i2);
        this.settingsPopup.resize(i, i2);
        this.ambiencePanel.resize(i, i2);
        this.hudStylePanel.resize(i, i2);
        this.hotkeysPanel.resize(i, i2);
        this.cosmeticsPanel.resize(i, i2);
        this.serverAssistPanel.resize(i, i2);
        this.neuroPanel.resize(i, i2);
        this.profilePopup.resize(i, i2);
        this.profileMenu.resize(i, i2);
        this.scriptWindowManager.onResize(i, i2);
        if (this.bindPopup != null) {
            this.bindPopup.resetState();
        }

        this.bindPopup = null;
        this.bindTarget = null;
    }

    @Override
    public void onDisplayed() {
        super.onDisplayed();
        this.captureHeldKeys();
        if (this.markIntroSeen()) {
            this.introOverlay.start();
        }
    }

    private boolean markIntroSeen() {
        Path path = mc.runDirectory.toPath().resolve(ClientInfo.MOD_ID).resolve("intro_seen");
        if (Files.exists(path)) {
            return false;
        }

        try {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        } catch (IOException ioException) {
        }

        return true;
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        this.openAnimation.update();
        this.categorySwitchAnimation.update();
        this.searchPanel.tick();
        this.tickClose();
        this.tickLogoAnimation();
        Iterator iterator2 = this.rows.iterator();

        while (iterator2.hasNext()) {
            ((ModuleRowComponent)iterator2.next()).update();
        }

        float f2 = GuiScale.getEffectiveScale();
        int i3 = (int)GuiScale.toScaled(i);
        int i4 = (int)GuiScale.toScaled(i2);
        if (this.introOverlay.isActive()) {
            i3 = Integer.MIN_VALUE;
            i4 = Integer.MIN_VALUE;
        }

        int i5 = i3;
        int i6 = i4;
        if (this.isSidePanelVisible()) {
            i3 = Integer.MIN_VALUE;
            i4 = Integer.MIN_VALUE;
        }

        int i7 = i5;
        int i8 = i6;
        if (this.scriptWindowManager.isMouseOverWindow(i7, i8)) {
            i5 = Integer.MIN_VALUE;
            i3 = Integer.MIN_VALUE;
            i6 = Integer.MIN_VALUE;
            i4 = Integer.MIN_VALUE;
        }

        if (this.lastScale > 0.0F && Math.abs(f2 - this.lastScale) > 1.0E-6F) {
            float f3 = this.lastScale / f2;
            this.x = (this.x + this.width / 2.0F) * f3 - this.width / 2.0F;
            this.y = (this.y + this.height / 2.0F) * f3 - this.height / 2.0F;
        }

        this.lastScale = f2;
        this.clampPosition();
        boolean flag = this.searchPanel.isOpen();
        float f4 = 1.0F - this.searchPanel.getOpenProgress();
        float f5 = 2.0F + 18.0F + 2.0F;
        float f6 = this.x + 90.0F + 4.0F * 2.0F;
        float f7 = this.y + 4.0F;
        float f8 = this.x + this.width - 4.0F - 3.0F;
        float f9 = f8 - f6;
        float f10 = this.y + (4.0F + f5 + 4.0F) / 2.0F - 14.0F / 2.0F;
        this.renderScriptScreenBackground(drawContext);
        this.renderPanelBackground(drawContext, 90.0F);
        if (this.reloading && this.reloadTimer.hasPassed(500.0)) {
            this.reloading = false;
        }

        this.reloadAnimation.update();
        this.reloadAnimation.animate(this.reloading ? 1.0 : 0.0, this.reloading ? 0.25 : 0.45, this.reloading ? Easings.QUAD_OUT : Easings.EXPO_OUT);
        this.renderLogo(drawContext, 90.0F, 4.0F, f5, 18.0F);
        this.renderProfile(drawContext, 90.0F, 4.0F, f5 + 2.0F, 18.0F);
        float f11 = this.y + 4.0F + f5;
        boolean flag2 = ScissorStack.push(drawContext, this.x, f11, 90.0F, this.profileY - f11);

        try {
            float f12 = this.y + 4.0F + f5 + 4.0F - 1.0F;
            this.sidebar.render(drawContext, i3, i4, f, this.x + 4.0F, f12, 90.0F - 4.0F * 2.0F, 17.0F, this.profileY - f12);
        } finally {
            if (flag2) {
                ScissorStack.pop(drawContext);
            }
        }

        this.sidebar.renderTooltips(drawContext);
        float f13 = this.y + 4.0F + 18.0F + 4.0F + 4.0F;
        float f14 = this.height - (f13 - this.y) - 4.0F - 3.0F;
        float f15 = this.x + 90.0F + 6.0F;
        float f16 = this.width - 90.0F - 8.0F - 4.0F;
        int i9;
        int i10;
        if (this.isHovered(i3, i4, this.contentX, this.contentY, this.contentWidth + 6.0F, this.contentHeight)
            && !(this.searchPanel.getOpenProgress() > 0.001F)
            && !this.isSidePanelVisible()
            && (!this.settingsPopup.isShowing() || !this.settingsPopup.isMouseOverPanel(i3, i4) && !this.settingsPopup.isMouseOverAny(i3, i4))
            && (!this.isBindPopupOpen() || !this.bindPopup.isMouseOver(i3, i4))
            && (!this.profilePopup.isVisible() || !this.profilePopup.isHoveringAny(i3, i4))
            && (!this.profileMenu.isOpen() || !this.profileMenu.isMouseOver(i3, i4))
            && (!this.configSelector.isPopupVisible() || !this.configSelector.isMouseOver(i3, i4))) {
            i9 = i3;
            i10 = i4;
        } else {
            i9 = Integer.MIN_VALUE;
            i10 = Integer.MIN_VALUE;
        }

        int i11 = i10;
        this.renderContent(drawContext, i9, i11, f, f15, f13, f16, f14);
        this.configSelector.setAlphaFactor(f4);
        float f17 = this.height * 2.0F / 3.0F - f5;
        if (flag) {
            this.setBlurMode(ClickGuiScreen.BlurMode.ONLY_MODULE);
        } else if (this.searchPanel.getOpenProgress() <= 0.0F) {
            this.setBlurMode(ClickGuiScreen.BlurMode.NONE);
        }

        this.renderBlur(drawContext, this.blurMode, this.searchPanel.getOpenProgress(), 90.0F);
        this.renderSearchBar(drawContext, f6, f7, f9, f5, f17);
        if (flag) {
            this.searchPanel.renderResults(drawContext, i3, i4, f);
            this.searchPanel.renderOverlays(drawContext, i3, i4, f);
        }

        this.renderRowOverlays(drawContext, this.visibleGroups);
        if (this.isEspEditorVisible() && this.espGroup != null) {
            Iterator iterator3 = this.espGroup.getRows().iterator();

            while (iterator3.hasNext()) {
                ((ModuleRowComponent)iterator3.next()).renderTooltip(drawContext);
            }
        }

        if (f4 > 0.001F) {
            this.renderConfigSelector(drawContext, i3, i4, f, f6 + 4.0F, f10, 100.0F, 14.0F);
        }

        this.renderSettingsPopup(drawContext, i3, i4, f);
        this.renderBindPopup(drawContext, i3, i4, f);
        this.renderProfilePopup(drawContext, i3, i4, f);
        if (this.ambiencePanel.isShowing() || this.ambiencePanel.isOpen()) {
            this.ambiencePanel.render(drawContext, i5, i6, f, this.x + 90.0F, this.y, this.width - 90.0F, this.height);
            this.ambiencePanel.renderOverlays(drawContext, i5, i6, f);
        }

        if (this.hudStylePanel.isVisible() || this.hudStylePanel.isOpen()) {
            this.hudStylePanel.render(drawContext, i5, i6, f, this.x + 90.0F, this.y, this.width - 90.0F, this.height);
        }

        if (this.cosmeticsPanel.isVisible() || this.cosmeticsPanel.isOpen()) {
            this.cosmeticsPanel.render(drawContext, i5, i6, f, this.x + 90.0F, this.y, this.width - 90.0F, this.height);
            this.cosmeticsPanel.renderOverlay(drawContext, i5, i6, f);
        }

        if (this.serverAssistPanel.isVisible() || this.serverAssistPanel.isOpen()) {
            this.serverAssistPanel.render(drawContext, i5, i6, f, this.x + 90.0F, this.y, this.width - 90.0F, this.height);
            this.serverAssistPanel.renderOverlay(drawContext, i5, i6, f);
        }

        if (this.neuroPanel.isVisible() || this.neuroPanel.isOpen()) {
            this.neuroPanel.renderPanel(drawContext, i5, i6, f, this.x + 90.0F, this.y, this.width - 90.0F, this.height);
        }

        if (this.hotkeysPanel.isVisible() || this.hotkeysPanel.isOpen()) {
            this.hotkeysPanel.render(drawContext, i5, i6, f, this.x + 90.0F, this.y, this.width - 90.0F, this.height);
        }

        if (this.profileMenu.isOpen() || this.profileMenu.isVisible()) {
            this.profileMenu.render(drawContext, i3, i4, f);
        }

        if (this.scriptEditor.isVisible()) {
            this.scriptEditor.render(drawContext, i5, i6, f);
        }

        this.scriptWindowManager.renderWindows(drawContext, i7, i8, f);
        this.renderReloadOverlay(drawContext);
        float f18 = this.x + 4.0F + 90.0F / 2.0F - 18.0F / 4.0F;
        float f19 = this.y + 4.0F + f5 / 2.0F;
        this.introOverlay.render(drawContext, this.x, this.y, this.width, this.height, f18, f19, 18.0F, this.openAnimation.get());
    }

    private void renderBlur(DrawContext drawContext, ClickGuiScreen.BlurMode blurMode2, float f, float f2) {
        switch (blurMode2) {
            case FULL:
                new RenderUtil(drawContext, this.x, this.y, this.width, this.height)
                    .blur(this.openAnimation.get() * f, Radius.all(8.0F), RenderUtil.BlurQuality.LOW, true);
                return;
            case ONLY_MODULE:
                new RenderUtil(drawContext, this.x + f2, this.y, this.width - f2, this.height)
                    .blur(this.openAnimation.get() * f, Radius.all(8.0F), RenderUtil.BlurQuality.LOW, true);
        }
    }

    public void reload() {
        this.reloading = true;
        this.reloadTimer.reset();
        this.settingsPopup.close();
        this.hudStylePanel.clear();
        this.rebuildRows();
    }

    private void renderReloadOverlay(DrawContext drawContext) {
        float f = this.reloadAnimation.get();
        if (!(f <= 0.001F)) {
            float f2 = (float)Math.pow(this.openAnimation.get(), 2.0) * f;
            new RenderUtil(drawContext, this.x, this.y, this.width, this.height)
                .blur(f2, Radius.smoothAll(20.0F), RenderUtil.BlurQuality.MAX, true)
                .rect(ColorUtil.multiplyAlpha(Theme.getBackground(), 0.35F * f2), Radius.smoothAll(20.0F));
            float f3 = this.x + this.width / 2.0F;
            float f4 = this.y + this.height / 2.0F;
            long l = this.reloadTimer.getElapsed();
            float f5 = 1.0F - Math.min(1.0F, (float)l / 500.0F);
            float f6 = 360.0F * (1.0F - f5 * f5 * f5);
            float f7 = (float)(l % 1400L) / 1400.0F * 360.0F;
            RenderUtil renderUtil = new RenderUtil(drawContext, f3 - 26.0F, f4 - 26.0F, 26.0F * 2.0F, 26.0F * 2.0F).circle(2.5F, Theme.getPrimary(), 0.15F * f2);
            if (f6 > 0.5F) {
                renderUtil.arc(f7, f6, 2.5F, Theme.getPrimary(), f2);
            }

            new RenderUtil(drawContext, f3 - 22.0F / 2.0F, f4 - 22.0F / 2.0F, 22.0F, 22.0F)
                .spriteAtProgress(AnimatedIcon.MAIN, 0.0F, 0.0F, 22.0F, 22.0F, Theme.withAlpha(Theme.getPrimary(), f2), 0.0F);
        }
    }

    private void renderPanelBackground(DrawContext drawContext, float f) {
        float f2 = this.openAnimation.get();
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager == null || !scriptManager.renderClickGuiBackground(drawContext, this.x, this.y, this.width, this.height, 20.0F, f, f2)) {
            int i = Theme.getBase();
            int i2 = Theme.getNearBackground();
            float f3 = Theme.getShadowStrength(f2);
            new RenderUtil(drawContext, this.x, this.y, this.width, this.height)
                .blurGlowBorder(f2, i, 0.68F, i2, 30.0F, f3, Easings.QUAD_IN, Radius.smoothAll(20.0F), RenderUtil.BlurQuality.MIDDLE, true);
            GuiRenderUtil.drawOutlined(drawContext, this.x + f, this.y, this.width - f, this.height, f2 * 0.6F, 10.0F);
        }
    }

    private void renderLogo(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = (float)Math.pow(this.openAnimation.get(), 3.0);
        float f6 = this.x + f2;
        float f7 = this.y + f2 + f3 / 2.0F - f4 / 2.0F;
        float f8 = f6 + f / 2.0F - f4 / 4.0F;
        float f9 = f8 - f4 / 2.0F;
        float f10 = this.logoSpinAnimation.get();
        float f11 = this.logoSlideAnimation.get();
        float f12 = this.logoPressAnimation.get();
        float f13 = this.x + 8.0F;
        float f14 = this.logoSecondPass ? 2.0F * f9 - f13 : f13;
        float f15 = (f14 - f9) * f11;
        this.logoX = f8 - 12.0F;
        this.logoY = f7 + f4 / 2.0F - 12.0F;
        GuiRenderUtil.drawOverlay(
            drawContext, this.x + f2 * 2.0F, MathUtil.roundToStep(this.y + f2 + f3, 0.5F), f - f2 * 4.0F, 0.5F, this.openAnimation.get()
        );
        boolean flag = f12 > 0.001F;
        if (flag) {
            float f16 = f9 + f15 + f4 / 2.0F;
            float f17 = f7 + f4 / 2.0F;
            float f18 = 1.0F - 0.2F * f12;
            float f19 = LOGO_PRESS_ROTATION * f12;
            Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
            matrix3x2fStack.pushMatrix();
            matrix3x2fStack.translate(f16, f17);
            matrix3x2fStack.rotate(f19);
            matrix3x2fStack.scale(f18, f18);
            matrix3x2fStack.translate(-f16, -f17);
        }

        if (!this.introOverlay.isActive()) {
            new RenderUtil(drawContext, f9 + f15, f7, f4, f4)
                .spriteAtProgress(AnimatedIcon.MAIN, -f4 / 2.0F, -f4 / 2.0F, f4 * 2.0F, f4 * 2.0F, Theme.withAlpha(Theme.getHighlight(), f5), f10);
        }

        if (flag) {
            drawContext.getMatrices().popMatrix();
        }

        if (f11 > 0.001F) {
            int i = Theme.withAlpha(Theme.getTextColor(), f11 * f5);
            float f20 = f7 + f4 / 2.0F - 4.0F;
            if (this.logoSecondPass) {
                String string = "Sadnes";
                float f21 = Fonts.INTER_MEDIUM == null ? 0.0F : Fonts.INTER_MEDIUM.getWidth(string, 8.0F);
                float f22 = f9 + f15 - f4 * 0.5F;
                new RenderUtil(drawContext, f22 - 2.0F - f21, f20, f21, 8.0F).textTopLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, i, 8.0F);
                return;
            }

            float f23 = f9 + f15 + f4 * 1.5F;
            new RenderUtil(drawContext, f23 + 2.0F, f20, 40.0F, 8.0F).textTopLeft(Fonts.INTER_MEDIUM, "1.21.11", 0.0F, 0.0F, i, 8.0F);
        }
    }

    private void renderProfile(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = this.x + f2 + 4.0F;
        this.profileX = this.x + f2;
        this.profileY = this.y + this.height - f3 - f2;
        this.profileWidth = f - f2 * 2.0F;
        this.profileHeight = f3;
        GuiRenderUtil.drawOverlay(drawContext, this.x + f2 * 2.0F, MathUtil.roundToStep(this.profileY, 0.5F), f - f2 * 4.0F, 0.5F, this.openAnimation.get());
        RenderUtil.drawChevron(
            drawContext,
            this.profileX + this.profileWidth - f2 - 8.0F,
            this.profileY + f3 / 2.0F - 8.0F / 2.0F,
            8.0F,
            180.0F * this.profilePopup.getOpenProgress(),
            ColorUtil.multiplyAlpha(Theme.getTextColor(), this.openAnimation.get())
        );
        float f6 = this.y + this.height - f3 - f2 + f3 / 2.0F - f4 / 2.0F;
        Identifier identifier = AvatarTextureLoader.getTexture(Profile.getAvatarUrl());
        if (identifier != null) {
            GuiRenderUtil.bindTexture(identifier);
        } else {
            GuiRenderUtil.bindTexture(Texture.AVATAR_DEFAULT);
        }

        if (GuiRenderUtil.getBoundTexture() != null) {
            new RenderUtil(drawContext, f5, f6, f4, f4).frostedGlass(this.openAnimation.get(), Radius.all(f4 / 2.0F));
        }

        float f7 = f4 / 2.0F - (7.0F + 1.0F + 6.0F) / 2.0F;
        float f8 = f4 + 4.0F;
        float f9 = this.profileX + this.profileWidth - f2 - 8.0F - (f5 + f8) - 4.0F;
        new RenderUtil(drawContext, f5, f6, f4, f4)
            .scrollingTextTop(
                Fonts.INTER_MEDIUM, Profile.getUsername(), f8, f7, f9, Theme.withAlpha(Theme.getSecondaryText(), this.openAnimation.get()), 7.0F
            )
            .scrollingTextTop(
                Fonts.INTER_MEDIUM,
                SubscriptionUtil.getSubscriptionText(),
                f8,
                f7 + 7.0F + 1.0F,
                f9,
                Theme.withAlpha(Theme.getSecondaryText(), 0.75F * this.openAnimation.get()),
                6.0F
            );
    }

    private void renderSearchBar(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        this.searchPanel.renderSearchBar(drawContext, f, f2, f3, f4, f5);
    }

    private void renderSettingsPopup(DrawContext drawContext, int i, int i2, float f) {
        if (this.settingsPopup.isShowing() || this.settingsPopup.isOpen()) {
            this.settingsPopup.render(drawContext, i, i2, f);
            this.settingsPopup.renderOverlays(drawContext, i, i2, f);
        }
    }

    private void renderBindPopup(DrawContext drawContext, int i, int i2, float f) {
        if (this.bindPopup != null) {
            this.bindPopup.render(drawContext, i, i2, f);
            if (!this.bindPopup.isOpen() && !this.bindPopup.isListeningForKey() && this.bindPopup.getOpenAnimation().get() <= 0.001F) {
                this.bindPopup = null;
                this.bindTarget = null;
            }
        }
    }

    private void renderProfilePopup(DrawContext drawContext, int i, int i2, float f) {
        this.profilePopup
            .renderPopup(
                drawContext,
                i,
                i2,
                f,
                this.profileX + this.profileWidth,
                this.profileX,
                this.profileY + this.profileHeight,
                (int)GuiScale.getScaledWidth(),
                (int)GuiScale.getScaledHeight()
            );
    }

    private void renderContent(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.contentX = f2;
        this.contentY = f3;
        this.contentWidth = f4;
        this.contentHeight = Math.max(0.0F, f5);
        HubCategory hubCategory = this.sidebar.getSelectedHubCategory();
        if (hubCategory != null) {
            this.visibleGroups = List.of();
            this.contentTotalHeight = 0.0F;
            this.renderHubPage(drawContext, hubCategory, i, i2, f, f2, f3, f4, f5);
        } else {
            CategoryDescriptor categoryDescriptor = this.sidebar.getSelectedCategory();
            VisualsSubCategory visualsSubCategory = this.sidebar.getSelectedVisualsSubCategory();
            List<OptionRowGroup> optionRowGroups = this.groupsByCategory.getOrDefault(categoryDescriptor, List.of());
            List list;
            if (categoryDescriptor == Category.VISUALS) {
                list = new ArrayList();

                for (OptionRowGroup optionRowGroup : optionRowGroups) {
                    if (VisualsSubCategory.fromSubcategory(optionRowGroup.getSource()) == visualsSubCategory) {
                        list.add(optionRowGroup);
                    }
                }
            } else {
                list = optionRowGroups;
            }

            float f6 = (f4 - 8.0F) / 2.0F;
            boolean flag = categoryDescriptor == Category.VISUALS && visualsSubCategory == VisualsSubCategory.PLAYER;
            if (flag) {
                this.renderEspLayout(drawContext, i, i2, f, f2, f3, f4, f5, f6, list);
            } else {
                float[] floats = new float[]{0.0F, 0.0F};
                int[] ints = new int[list.size()];

                for (int i3 = 0; i3 < list.size(); i3++) {
                    int i4 = floats[0] <= floats[1] ? 0 : 1;
                    ints[i3] = i4;
                    floats[i4] += ((OptionRowGroup)list.get(i3)).getHeight() + 12.0F;
                }

                this.contentTotalHeight = Math.max(floats[0], floats[1]);
                this.scrollbar.update(this.contentTotalHeight, this.contentHeight);
                this.scrollbar.updateHover(i, i2);
                this.scrollToPendingFeature(list, ints, f3);
                this.scrollbar.render(drawContext, this.contentX + this.contentWidth + 1.5F, this.contentY, this.contentHeight, this.contentTotalHeight, 0.0F);
                float f7 = this.scrollbar.getScroll();
                boolean flag2 = ScissorStack.push(drawContext, this.contentX - 2.0F, this.contentY - 2.0F, this.contentWidth + 4.0F, this.contentHeight + 4.0F);
                if (flag2) {
                    try {
                        this.beginScrollFade(f7);
                        float f8 = (1.0F - this.categorySwitchAnimation.get()) * 30.0F;
                        float[] floats2 = new float[]{f3 + f7 + f8, f3 + f7 + f8};

                        for (int i5 = 0; i5 < list.size(); i5++) {
                            OptionRowGroup optionRowGroup2 = (OptionRowGroup)list.get(i5);
                            int i6 = ints[i5];
                            float f9 = f2 + i6 * (f6 + 8.0F);
                            float f10 = floats2[i6];
                            if (f10 + optionRowGroup2.getHeight() >= this.contentY && f10 <= this.contentY + this.contentHeight) {
                                optionRowGroup2.render(drawContext, f9, f10, f6, i, i2, f);
                            }

                            floats2[i6] += optionRowGroup2.getHeight() + 12.0F;
                        }
                    } finally {
                        VerticalFadeMask.end();
                        ScissorStack.pop(drawContext);
                    }
                }

                this.visibleGroups = list;
                this.handlePendingNavigation();
            }
        }
    }

    private void handlePendingNavigation() {
        if (this.searchPanel.hasPendingNavigation() && !this.searchPanel.isOpen()) {
            SearchPanel.NavigationRequest navigationRequest = this.searchPanel.getPendingNavigation();
            if (navigationRequest.feature() != null && navigationRequest.feature().getCategory() == this.sidebar.getSelectedCategory()) {
                this.searchPanel.consumePendingNavigation();
                ModuleRowComponent moduleRowComponent = this.getRow(navigationRequest.feature());
                if (moduleRowComponent != null) {
                    moduleRowComponent.flash();
                }
            }
        }
    }

    private void scrollToPendingFeature(List<OptionRowGroup> optionRowGroups, int[] ints, float f) {
        SearchPanel.NavigationRequest navigationRequest = this.searchPanel.getPendingNavigation();
        if (navigationRequest != null && navigationRequest.feature() != null && navigationRequest.feature().getCategory() == this.sidebar.getSelectedCategory()) {
            float f2 = this.findRowOffset(optionRowGroups, ints, navigationRequest.feature(), f);
            if (!Float.isNaN(f2)) {
                float f3 = Math.min(0.0F, this.contentHeight - this.contentTotalHeight);
                this.scrollbar.setScroll(this.clamp(f + 8.0F - f2, f3, 0.0F));
            }
        }
    }

    private float findRowOffset(List<OptionRowGroup> optionRowGroups, int[] ints, Module module, float f) {
        float[] floats = new float[]{f, f};

        for (int i = 0; i < optionRowGroups.size(); i++) {
            OptionRowGroup optionRowGroup = (OptionRowGroup)optionRowGroups.get(i);
            int i2 = ints[i];
            float f2 = floats[i2];

            for (int i3 = 0; i3 < optionRowGroup.getRows().size(); i3++) {
                ModuleRowComponent moduleRowComponent = optionRowGroup.getRows().get(i3);
                if (moduleRowComponent.getModule() == module) {
                    return f2 + optionRowGroup.getTitleHeight() + i3 * 22.0F;
                }
            }

            floats[i2] += optionRowGroup.getHeight() + 12.0F;
        }

        return Float.NaN;
    }

    private void renderHubPage(DrawContext drawContext, HubCategory hubCategory, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        if (hubCategory == HubCategory.SCRIPTS) {
            this.scriptsPage.render(drawContext, i, i2, f, f2, f3, f4, f5);
        } else if (hubCategory == HubCategory.AUCTION) {
            switch (this.sidebar.getSelectedUtilitiesSubCategory()) {
                case AUTOBUY:
                    this.autoBuyPage.render(drawContext, i, i2, f, f2, f3, f4, f5);
                    return;
                case COLLECTOR:
                    this.collectorPage.render(drawContext, i, i2, f, f2, f3, f4, f5);
            }
        } else if (hubCategory == HubCategory.EVENTS) {
            this.eventsPage.render(drawContext, i, i2, f, f2, f3, f4, f5);
        } else if (hubCategory == HubCategory.PARTY) {
            this.partyPage.renderPage(drawContext, i, i2, f, f2, f3, f4, f5);
        } else {
            float f6 = (float)Math.pow(this.openAnimation.get(), 3.0);
            new RenderUtil(drawContext, f2, f3, f4, f5)
                .icon(hubCategory.getIcon(), f4 / 2.0F - 9.0F, f5 / 2.0F - 24.0F, 18.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.3F * f6))
                .textCenter(Fonts.INTER_MEDIUM, hubCategory.getDisplayName(), 0.0F, 0.0F, Theme.withAlpha(Theme.getTextColor(), 0.7F * f6), 8.0F)
                .textCenter(
                    Fonts.INTER_REGULAR,
                    Language.translate("clickgui.page.soon", "Coming soon"),
                    0.0F,
                    11.0F,
                    Theme.withAlpha(Theme.getSecondaryText(), 0.5F * f6),
                    6.5F
                );
        }
    }

    private void renderRowOverlays(DrawContext drawContext, List<OptionRowGroup> optionRowGroups) {
        for (OptionRowGroup optionRowGroup : optionRowGroups) {
            for (ModuleRowComponent moduleRowComponent : optionRowGroup.getRows()) {
                moduleRowComponent.renderTooltip(drawContext);
            }
        }
    }

    private boolean isEspEditorVisible() {
        return this.sidebar.getSelectedHubCategory() == null
            && this.sidebar.getSelectedCategory() == Category.VISUALS
            && this.sidebar.getSelectedVisualsSubCategory() == VisualsSubCategory.PLAYER;
    }

    private void renderEspLayout(
        DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5, float f6, List<OptionRowGroup> optionRowGroups
    ) {
        float f7 = 0.0F;

        for (OptionRowGroup optionRowGroup : optionRowGroups) {
            f7 += optionRowGroup.getHeight() + 12.0F;
        }

        if (!optionRowGroups.isEmpty()) {
            f7 -= 12.0F;
        }

        this.contentTotalHeight = f7;
        this.scrollbar.update(this.contentTotalHeight, this.contentHeight);
        this.scrollbar.updateHover(i, i2);
        this.scrollToPendingFeature(optionRowGroups, new int[optionRowGroups.size()], f3);
        this.espEditorX = f2 + f6 + 8.0F;
        this.espEditorY = f3;
        this.espEditorWidth = f4 - f6 - 8.0F;
        float f8 = this.espGroup.getRows().isEmpty() ? 0.0F : this.espGroup.getHeight() + 4.0F;
        this.espEditorHeight = Math.max(0.0F, f5 - f8);
        this.espEditorPanel.render(drawContext, this.espEditorX, this.espEditorY, this.espEditorWidth, this.espEditorHeight, i, i2, f);
        if (f8 > 0.0F) {
            this.espGroup.render(drawContext, this.espEditorX, this.espEditorY + this.espEditorHeight + 4.0F, this.espEditorWidth, i, i2, f);
        }

        if (this.contentTotalHeight > this.contentHeight) {
            this.scrollbar.render(drawContext, f2 + f6 + 1.5F, f3, this.contentHeight, this.contentTotalHeight, 0.0F);
        }

        float f9 = this.scrollbar.getScroll();
        boolean flag = ScissorStack.push(drawContext, f2 - 2.0F, f3 - 2.0F, f6 + 4.0F, f5 + 4.0F);
        if (flag) {
            try {
                this.beginScrollFade(f9);
                float f10 = f3 + f9 + (1.0F - this.categorySwitchAnimation.get()) * 30.0F;

                for (OptionRowGroup optionRowGroup2 : optionRowGroups) {
                    if (f10 + optionRowGroup2.getHeight() >= f3 && f10 <= f3 + f5) {
                        optionRowGroup2.render(drawContext, f2, f10, f6, i, i2, f);
                    }

                    f10 += optionRowGroup2.getHeight() + 12.0F;
                }
            } finally {
                VerticalFadeMask.end();
                ScissorStack.pop(drawContext);
            }
        }

        this.visibleGroups = optionRowGroups;
        this.handlePendingNavigation();
    }

    private void beginScrollFade(float f) {
        float f2 = this.contentTotalHeight - this.contentHeight;
        if (!(f2 <= 0.5F) && !(this.contentHeight <= 36.0F)) {
            float f3 = this.clamp(-f / 18.0F, 0.0F, 1.0F);
            float f4 = this.clamp((f2 + f) / 18.0F, 0.0F, 1.0F);
            if (!(f3 <= 0.001F) || !(f4 <= 0.001F)) {
                float f5 = this.contentY;
                float f6 = this.contentY + this.contentHeight;
                VerticalFadeMask.begin(f5, f5 + 18.0F, f3, f6 - 18.0F, f6, f4);
            }
        }
    }

    private void renderConfigSelector(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.configSelector.position.set(f2, f3);
        this.configSelector.size.set(f4, f5);
        this.configSelector.render(drawContext, i, i2, f);
    }

    @Override
    public void onToggle(Module module) {
        module.toggle();
    }

    @Override
    public void onOpenSettings(Module module, ModuleRowComponent moduleRowComponent) {
        if (module instanceof ScriptFeature scriptFeature && scriptFeature.hasCustomPanel()) {
            if (this.hudStylePanel.isShowing(scriptFeature)) {
                this.hudStylePanel.close();
            } else {
                this.openHudStyle(scriptFeature);
            }
        } else if (module instanceof Ambience ambience) {
            if (this.ambiencePanel.isOpen()) {
                this.ambiencePanel.close();
            } else {
                this.settingsPopup.close();
                this.closeBindPopup();
                this.cosmeticsPanel.close();
                this.serverAssistPanel.close();
                this.neuroPanel.close();
                this.ambiencePanel.open(ambience);
            }
        } else if (module instanceof Cosmetics cosmetics) {
            if (this.cosmeticsPanel.isOpen()) {
                this.cosmeticsPanel.close();
            } else {
                this.settingsPopup.close();
                this.ambiencePanel.close();
                this.serverAssistPanel.close();
                this.neuroPanel.close();
                this.closeBindPopup();
                this.cosmeticsPanel.open(cosmetics);
            }
        } else if (module instanceof ServerAssist serverAssist) {
            if (this.serverAssistPanel.isOpen()) {
                this.serverAssistPanel.close();
            } else {
                this.settingsPopup.close();
                this.ambiencePanel.close();
                this.cosmeticsPanel.close();
                this.neuroPanel.close();
                this.closeBindPopup();
                this.serverAssistPanel.open(serverAssist);
            }
        } else if (this.settingsPopup.isOpen() && this.settingsPopup.getHolder() == module) {
            this.settingsPopup.close();
        } else {
            this.settingsPopup.open(module, moduleRowComponent.getLeft(), moduleRowComponent.getTop(), moduleRowComponent.getRowWidth(), (int)GuiScale.getScaledWidth(), (int)GuiScale.getScaledHeight());
        }
    }

    @Override
    public void onBind(Module module, double d, double d2) {
        if (module instanceof Feature feature) {
            this.openFeatureBind(feature, d, d2);
        } else {
            if (module instanceof ScriptFeature scriptFeature) {
                this.openSettingBind(scriptFeature.getEnabledSetting(), d, d2);
            }
        }
    }

    public void closeSettingsPopup() {
        if (this.settingsPopup.isOpen()) {
            this.settingsPopup.close();
        }
    }

    public void toggleSettingsPopup(SettingHolder settingHolder, float f, float f2) {
        if (settingHolder != null) {
            if (this.settingsPopup.isOpen() && this.settingsPopup.getHolder() == settingHolder) {
                this.settingsPopup.close();
            } else {
                this.settingsPopup.open(settingHolder, f + 6.0F, f2 + 15.0F, 0.0F, (int)GuiScale.getScaledWidth(), (int)GuiScale.getScaledHeight());
            }
        }
    }

    public void toggleSettingsPopupAt(SettingHolder settingHolder, float f, float f2, float f3) {
        if (settingHolder != null) {
            if (this.settingsPopup.isOpen() && this.settingsPopup.getHolder() == settingHolder) {
                this.settingsPopup.close();
            } else {
                this.settingsPopup.open(settingHolder, f, f2, f3, (int)GuiScale.getScaledWidth(), (int)GuiScale.getScaledHeight());
            }
        }
    }

    private void openFeatureBind(Feature feature, double d, double d2) {
        if (this.bindTarget != feature) {
            if (this.bindPopup != null) {
                this.bindPopup.resetState();
            }

            this.bindPopup = new BindPopupComponent(feature);
            this.bindPopup.setAllHotKeysAction(this::openHotkeysPanel);
            this.bindPopup.position.set(this.x, this.y);
            this.bindPopup.size.set(this.width, this.height);
            this.bindTarget = feature;
        }

        this.bindPopup.position.set(this.x, this.y);
        this.bindPopup.size.set(this.width, this.height);
        this.bindPopup.openForFeature(d, d2);
    }

    public void openSettingBind(ValueSetting<?> valueSetting, double d, double d2) {
        if (valueSetting != null) {
            if (this.bindTarget != valueSetting) {
                if (this.bindPopup != null) {
                    this.bindPopup.resetState();
                }

                this.bindPopup = new BindPopupComponent(null);
                this.bindPopup.setAllHotKeysAction(this::openHotkeysPanel);
                this.bindTarget = valueSetting;
            }

            this.bindPopup.position.set(this.x, this.y);
            this.bindPopup.size.set(this.width, this.height);
            this.bindPopup.openForSetting(valueSetting, d, d2);
        }
    }

    public boolean isBindPopupActive() {
        return this.isBindPopupOpen();
    }

    private void closeBindPopup() {
        if (this.bindPopup != null) {
            this.bindPopup.close();
        }
    }

    public void openHotkeysPanel() {
        if (this.searchPanel.isOpen()) {
            this.searchPanel.setOpen(false);
        }

        this.configSelector.close();
        this.settingsPopup.close();
        this.ambiencePanel.close();
        this.cosmeticsPanel.close();
        this.serverAssistPanel.close();
        this.neuroPanel.close();
        this.profilePopup.close();
        this.closeBindPopup();
        this.hotkeysPanel.open();
    }

    public void toggleNeuroPanel() {
        if (this.neuroPanel.isOpen()) {
            this.neuroPanel.close();
        } else {
            this.settingsPopup.close();
            this.ambiencePanel.close();
            this.cosmeticsPanel.close();
            this.serverAssistPanel.close();
            this.closeBindPopup();
            this.neuroPanel.openPanel();
        }
    }

    public void openHudStyle(ScriptFeature scriptFeature) {
        if (mc.currentScreen != this) {
            mc.setScreen(this);
        }

        if (!this.hudStylePanel.isShowing(scriptFeature)) {
            this.settingsPopup.close();
            this.closeSidePanels();
            this.hudStylePanel.open(scriptFeature);
        }
    }

    public void closeHudStyle(ScriptFeature scriptFeature) {
        if (this.hudStylePanel.isShowing(scriptFeature)) {
            this.hudStylePanel.close();
        }
    }

    public boolean isHudStyleOpen(ScriptFeature scriptFeature) {
        return this.hudStylePanel.isShowing(scriptFeature);
    }

    private boolean isBindPopupOpen() {
        return this.bindPopup != null && (this.bindPopup.isOpen() || this.bindPopup.isListeningForKey());
    }

    private boolean isSidePanelVisible() {
        return this.ambiencePanel.isShowing()
            || this.cosmeticsPanel.isVisible()
            || this.serverAssistPanel.isVisible()
            || this.neuroPanel.isVisible()
            || this.hotkeysPanel.isVisible()
            || this.scriptEditor.isVisible()
            || this.hudStylePanel.isVisible();
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (this.closing) {
            return false;
        }

        if (this.introOverlay.isActive() && keyInput.key() != 256) {
            this.introOverlay.tryContinue();
            return true;
        }

        QuickInteract quickInteract = QuickInteract.getInstance();
        if (quickInteract != null && quickInteract.isEnabled() && quickInteract.keyPressed(keyInput)) {
            return true;
        }

        if (this.scriptWindowManager.onKeyPress(keyInput)) {
            return true;
        }

        if (this.scriptEditor.isOpen() && this.scriptEditor.keyPressed(keyInput)) {
            return true;
        }

        boolean flag = this.searchPanel.isOpen();
        if ((this.bindPopup == null || !this.bindPopup.isListeningForKey())
            && (!this.settingsPopup.isOpen() || !this.settingsPopup.isListeningForKey())
            && !this.hotkeysPanel.isListeningForKey()
            && keyInput.key() == 70
            && (keyInput.modifiers() & 2) != 0) {
            this.toggleSearch();
            return true;
        }

        if (flag && keyInput.key() == 256) {
            this.closeSearch();
            return true;
        }

        if (flag && this.searchPanel.keyPressed(keyInput)) {
            return true;
        }

        if (this.isBindPopupOpen()) {
            if (this.bindPopup.onKeyPress(keyInput)) {
                return true;
            }

            if (keyInput.key() == 256) {
                this.closeBindPopup();
                return true;
            }
        }

        if (this.ambiencePanel.isOpen() && this.ambiencePanel.onKeyPress(keyInput)) {
            return true;
        } else if (this.hudStylePanel.isOpen() && this.hudStylePanel.onKeyPress(keyInput)) {
            return true;
        } else if (this.hotkeysPanel.isOpen() && this.hotkeysPanel.onKeyPress(keyInput)) {
            return true;
        } else if (this.cosmeticsPanel.isOpen() && this.cosmeticsPanel.onKeyPress(keyInput)) {
            return true;
        } else if (this.serverAssistPanel.isOpen() && this.serverAssistPanel.onKeyPress(keyInput)) {
            return true;
        } else if (this.neuroPanel.isOpen() && this.neuroPanel.onKeyPress(keyInput)) {
            return true;
        } else if (this.profileMenu.isOpen() && this.profileMenu.onKeyPress(keyInput)) {
            return true;
        } else if (this.settingsPopup.isOpen() && this.settingsPopup.onKeyPress(keyInput)) {
            return true;
        } else if (this.profilePopup.isVisible() && this.profilePopup.onKeyPress(keyInput)) {
            return true;
        } else if (!flag && this.espEditorPanel.keyPressed(keyInput)) {
            return true;
        } else if (!flag && this.configSelector.onKeyPress(keyInput)) {
            return true;
        } else if (this.isAutoBuyPage() && this.autoBuyPage.keyPressed(keyInput)) {
            return true;
        } else if (this.isCollectorPage() && this.collectorPage.keyPressed(keyInput)) {
            return true;
        } else if (this.isPartyPage() && this.partyPage.handleKeyPress(keyInput)) {
            return true;
        } else {
            return this.isCloseKey(keyInput.key()) && !this.heldKeys.contains(keyInput.key()) && !this.heldKeys.contains(Key.getModifier(keyInput.key()))
                ? this.shouldCloseOnEsc()
                : false;
        }
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        this.heldKeys.remove(keyInput.key());
        this.heldKeys.remove(Key.getModifier(keyInput.key()));
        if (this.bindPopup != null && this.bindPopup.keyReleased(keyInput)) {
            return true;
        } else if (this.hotkeysPanel.keyReleased(keyInput)) {
            return true;
        } else {
            return this.settingsPopup.handleBindKeyRelease(keyInput) ? true : this.serverAssistPanel.isOpen() && this.serverAssistPanel.keyReleased(keyInput);
        }
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (this.closing) {
            return true;
        } else if (this.scriptWindowManager.onCharTyped(charInput)) {
            return true;
        } else if (this.scriptEditor.isOpen() && this.scriptEditor.charTyped(charInput)) {
            return true;
        } else {
            boolean flag = this.searchPanel.isOpen();
            if (flag && this.searchPanel.charTyped(charInput)) {
                return true;
            } else if (this.isBindPopupOpen() && this.bindPopup.onCharType(charInput)) {
                return true;
            } else if (this.ambiencePanel.isOpen() && this.ambiencePanel.onCharType(charInput)) {
                return true;
            } else if (this.hudStylePanel.isOpen() && this.hudStylePanel.onCharType(charInput)) {
                return true;
            } else if (this.hotkeysPanel.isOpen() && this.hotkeysPanel.onCharType(charInput)) {
                return true;
            } else if (this.cosmeticsPanel.isOpen() && this.cosmeticsPanel.onCharType(charInput)) {
                return true;
            } else if (this.serverAssistPanel.isOpen() && this.serverAssistPanel.onCharType(charInput)) {
                return true;
            } else if (this.neuroPanel.isOpen() && this.neuroPanel.onCharType(charInput)) {
                return true;
            } else if (this.profileMenu.isOpen() && this.profileMenu.onCharType(charInput)) {
                return true;
            } else if (this.settingsPopup.isOpen() && this.settingsPopup.onCharType(charInput)) {
                return true;
            } else if (this.profilePopup.isVisible() && this.profilePopup.onCharType(charInput)) {
                return true;
            } else if (!flag && this.configSelector.onCharType(charInput)) {
                return true;
            } else if (this.isAutoBuyPage() && this.autoBuyPage.charTyped(charInput)) {
                return true;
            } else {
                return this.isCollectorPage() && this.collectorPage.charTyped(charInput) ? true : this.isPartyPage() && this.partyPage.handleCharTyped(charInput);
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (this.closing) {
            return true;
        }

        Click click2 = this.toScaledClick(click);
        if (this.introOverlay.isActive()) {
            this.introOverlay.skip();
            return true;
        }

        QuickInteract quickInteract = QuickInteract.getInstance();
        if (quickInteract != null && quickInteract.isEnabled() && quickInteract.mouseClicked(click2, this.isHovered(click2.x(), click2.y(), this.x, this.y, this.width, this.height))) {
            return true;
        }

        if (this.scriptWindowManager.onMouseClick(click2, flag)) {
            return true;
        }

        if (this.scriptEditor.isOpen()) {
            return this.scriptEditor.mouseClicked(click2, flag);
        }

        if (this.reloading) {
            return true;
        }

        boolean flag2 = this.searchPanel.isOpen();
        if (this.ambiencePanel.isOpen()) {
            if (this.ambiencePanel.mouseClicked(click2, flag)) {
                return true;
            }

            this.ambiencePanel.close();
            return true;
        } else if (this.hudStylePanel.isOpen()) {
            if (this.hudStylePanel.mouseClicked(click2, flag)) {
                return true;
            }

            this.hudStylePanel.close();
            return true;
        } else if (this.cosmeticsPanel.isOpen()) {
            if (this.cosmeticsPanel.mouseClicked(click2, flag)) {
                return true;
            }

            this.cosmeticsPanel.close();
            return true;
        } else if (this.serverAssistPanel.isOpen()) {
            if (this.serverAssistPanel.mouseClicked(click2, flag)) {
                return true;
            }

            this.serverAssistPanel.close();
            return true;
        } else if (this.neuroPanel.isOpen()) {
            if (this.neuroPanel.mouseClicked(click2, flag)) {
                return true;
            }

            this.neuroPanel.close();
            return true;
        } else if (this.hotkeysPanel.isOpen()) {
            if (this.hotkeysPanel.mouseClicked(click2, flag)) {
                return true;
            }

            this.hotkeysPanel.close();
            return true;
        } else {
            if (this.isSidePanelVisible()) {
                return true;
            }

            if (this.profileMenu.isOpen() && this.profileMenu.mouseClicked(click2, flag)) {
                return true;
            }

            if (this.isBindPopupOpen()) {
                if (flag && this.isMiddleButton(click2.button())) {
                    this.bindPopup.addBindAndListen();
                    return true;
                } else if (!this.bindPopup.isMouseOver(click2.x(), click2.y()) && !this.bindPopup.isListeningForKey()) {
                    this.closeBindPopup();
                    return true;
                } else {
                    this.bindPopup.mouseClicked(click2, flag);
                    return true;
                }
            } else {
                if (this.profilePopup.isVisible()
                    && (this.profilePopup.isHoveringAny(click2.x(), click2.y()) || this.profilePopup.isAnyOverlayOpen())
                    && this.profilePopup.mouseClicked(click2, flag)) {
                    return true;
                }

                if (this.isLeftButton(click2.button()) && this.isOverProfile(click2.x(), click2.y())) {
                    boolean flag3 = !this.profilePopup.isOpen();
                    this.profilePopup.toggle();
                    if (flag3) {
                        this.configSelector.close();
                        this.settingsPopup.close();
                        this.ambiencePanel.close();
                        this.cosmeticsPanel.close();
                        this.serverAssistPanel.close();
                        this.neuroPanel.close();
                        this.closeBindPopup();
                    }
                } else {
                    if (this.profilePopup.isOpen() && !this.profilePopup.isHoveringAny(click2.x(), click2.y())) {
                        this.profilePopup.close();
                        return true;
                    }

                    if (this.searchPanel.mouseClicked(click2, flag)) {
                        if (this.searchPanel.isOpen() != flag2) {
                            this.onSearchOpened();
                            return true;
                        }
                    } else {
                        if (this.settingsPopup.isOpen()) {
                            boolean flag4 = this.settingsPopup.isMouseOverPanel(click2.x(), click2.y());
                            boolean flag5 = this.settingsPopup.isMouseOverAny(click2.x(), click2.y());
                            if (flag4 || flag5) {
                                this.settingsPopup.mouseClicked(click2, flag);
                                return true;
                            }

                            if (this.settingsPopup.hasOpenOverlay()) {
                                this.settingsPopup.mouseClicked(click2, flag);
                                return true;
                            }
                        }

                        if (this.isRightButton(click2.button()) && !this.isHovered(click2.x(), click2.y(), this.x, this.y, this.width, this.height)) {
                            DragComponent dragComponent = DragComponent.getInstance();
                            if (dragComponent != null) {
                                DragSetting dragSetting = dragComponent.getDragHandler().getElementAt(click2.x(), click2.y());
                                if (dragSetting != null && dragSetting.getOwner() instanceof Feature feature) {
                                    if (this.settingsPopup.isOpen() && this.settingsPopup.getHolder() == feature) {
                                        this.settingsPopup.close();
                                        return true;
                                    }

                                    this.ambiencePanel.close();
                                    this.cosmeticsPanel.close();
                                    this.serverAssistPanel.close();
                                    this.neuroPanel.close();
                                    this.closeBindPopup();
                                    this.settingsPopup
                                        .openSide(
                                            feature,
                                            dragSetting.position2.x,
                                            dragSetting.position2.y,
                                            dragSetting.size2.x,
                                            (int)GuiScale.getScaledWidth(),
                                            (int)GuiScale.getScaledHeight()
                                        );
                                    return true;
                                }
                            }
                        }

                        if (!flag2 && this.configSelector.mouseClicked(click2, flag)) {
                            return true;
                        }

                        if (flag2 || !this.isOverEspEditor(click2.x(), click2.y())) {
                            if (!flag2) {
                                this.espEditorPanel.closeMenu();
                            }

                            if (!flag2 && this.isAutoBuyPage() && this.autoBuyPage.mouseClicked(click2, flag)) {
                                return true;
                            }

                            if (!flag2 && this.isCollectorPage() && this.collectorPage.mouseClicked(click2, flag)) {
                                return true;
                            }

                            if (!flag2 && this.isEventsPage() && this.eventsPage.mouseClicked(click2, flag)) {
                                return true;
                            }

                            if (!flag2 && this.isPartyPage() && this.partyPage.handleMouseClick(click2, flag)) {
                                return true;
                            }

                            if (!flag2 && this.isScriptsPage() && this.scriptsPage.mouseClicked(click2, flag)) {
                                return true;
                            }

                            if (this.sidebar.mouseClicked(click2, flag)) {
                                return true;
                            }

                            if (!flag2 && this.isLeftButton(click2.button()) && this.scrollbar.mouseClicked(click2.x(), click2.y())) {
                                return true;
                            }

                            if (!flag2 && this.isEspEditorVisible() && this.espGroup != null && this.espGroup.mouseClicked(click2, flag)) {
                                return true;
                            }

                            if (!flag2 && this.isOverContent(click2.x(), click2.y())) {
                                CategoryDescriptor categoryDescriptor = this.sidebar.getSelectedCategory();
                                VisualsSubCategory visualsSubCategory = this.sidebar.getSelectedVisualsSubCategory();

                                for (OptionRowGroup optionRowGroup : this.groupsByCategory.getOrDefault(categoryDescriptor, List.of())) {
                                    if ((categoryDescriptor != Category.VISUALS || VisualsSubCategory.fromSubcategory(optionRowGroup.getSource()) == visualsSubCategory)
                                        && optionRowGroup.mouseClicked(click2, flag)) {
                                        return true;
                                    }
                                }
                            }

                            if (this.settingsPopup.isOpen()
                                && !this.settingsPopup.isMouseOverPanel(click2.x(), click2.y())
                                && !this.settingsPopup.isMouseOverAny(click2.x(), click2.y())) {
                                this.settingsPopup.close();
                                return true;
                            }

                            if (this.isLeftButton(click2.button())
                                && this.logoAnimState == ClickGuiScreen.LogoAnimState.IDLE
                                && this.isHovered(click2.x(), click2.y(), this.logoX, this.logoY, 24.0, 24.0)) {
                                this.startLogoAnimation();
                                return true;
                            }

                            if (this.isLeftButton(click2.button()) && this.isOverDragArea(click2.x(), click2.y())) {
                                this.dragging = true;
                                this.dragOffsetX = (float)click2.x() - this.x;
                                this.dragOffsetY = (float)click2.y() - this.y;
                                return true;
                            }

                            return false;
                        }

                        this.espEditorPanel.mouseClicked(click2, flag);
                    }
                }

                return true;
            }
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        Click click2 = this.toScaledClick(click);
        if (this.scriptWindowManager.onMouseRelease(click2)) {
            return true;
        }

        if (this.isBindPopupOpen()) {
            this.bindPopup.onMouseRelease(click2);
        }

        if (this.ambiencePanel.isOpen()) {
            this.ambiencePanel.onMouseRelease(click2);
        }

        if (this.hudStylePanel.isOpen()) {
            this.hudStylePanel.onMouseRelease(click2);
        }

        if (this.hotkeysPanel.isOpen()) {
            this.hotkeysPanel.onMouseRelease(click2);
        }

        if (this.cosmeticsPanel.isOpen()) {
            this.cosmeticsPanel.onMouseRelease(click2);
        }

        if (this.serverAssistPanel.isOpen()) {
            this.serverAssistPanel.onMouseRelease(click2);
        }

        if (this.neuroPanel.isOpen()) {
            this.neuroPanel.onMouseRelease(click2);
        }

        if (this.profileMenu.isOpen()) {
            this.profileMenu.onMouseRelease(click2);
        }

        if (this.settingsPopup.isOpen()) {
            this.settingsPopup.onMouseRelease(click2);
        }

        if (this.profilePopup.isVisible()) {
            this.profilePopup.onMouseRelease(click2);
        }

        if (this.scriptEditor.isOpen()) {
            this.scriptEditor.mouseReleased(click2);
        }

        this.autoBuyPage.mouseReleased(click2);
        this.collectorPage.mouseReleased(click2);
        this.eventsPage.mouseReleased(click2);
        this.partyPage.handleMouseRelease(click2);
        this.scriptsPage.mouseReleased(click2);
        this.scrollbar.mouseReleased();
        this.sidebar.mouseReleased();
        this.dragging = false;
        if (this.searchPanel.isOpen()) {
            this.searchPanel.mouseReleased(click2);
        }

        return !this.searchPanel.isOpen() && this.configSelector.onMouseRelease(click2) ? true : !this.searchPanel.isOpen() && this.espEditorPanel.mouseReleased(click2);
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.closing) {
            return true;
        } else if (this.introOverlay.isActive()) {
            return true;
        } else {
            Click click2 = this.toScaledClick(click);
            double d3 = GuiScale.toScaled(d);
            double d4 = GuiScale.toScaled(d2);
            if (this.scriptWindowManager.onMouseDrag(click2, d3, d4)) {
                return true;
            } else if (this.scriptEditor.isOpen()) {
                this.scriptEditor.mouseDragged(click2, d3, d4);
                return true;
            } else if (this.profileMenu.isOpen() && this.profileMenu.mouseDragged(click2, d3, d4)) {
                return true;
            } else if (this.ambiencePanel.isOpen()) {
                this.ambiencePanel.mouseDragged(click2, d3, d4);
                return true;
            } else if (this.hudStylePanel.isOpen()) {
                this.hudStylePanel.mouseDragged(click2, d3, d4);
                return true;
            } else if (this.cosmeticsPanel.isOpen()) {
                this.cosmeticsPanel.mouseDragged(click2, d3, d4);
                return true;
            } else if (this.serverAssistPanel.isOpen()) {
                this.serverAssistPanel.mouseDragged(click2, d3, d4);
                return true;
            } else if (this.neuroPanel.isOpen()) {
                this.neuroPanel.mouseDragged(click2, d3, d4);
                return true;
            } else if (this.hotkeysPanel.isOpen()) {
                this.hotkeysPanel.mouseDragged(click2, d3, d4);
                return true;
            } else if (this.isBindPopupOpen()) {
                this.bindPopup.mouseDragged(click2, d3, d4);
                return true;
            } else if (this.settingsPopup.isOpen() && this.settingsPopup.mouseDragged(click2, d3, d4)) {
                return true;
            } else if (this.profilePopup.isVisible() && this.profilePopup.mouseDragged(click2, d3, d4)) {
                return true;
            } else if (this.searchPanel.isOpen() && this.searchPanel.mouseDragged(click2, d3, d4)) {
                return true;
            } else if (this.scrollbar.isDragging() && this.scrollbar.mouseDragged(click2.x(), click2.y())) {
                return true;
            } else if (this.isAutoBuyPage() && this.autoBuyPage.mouseDragged(click2, d3, d4)) {
                return true;
            } else if (this.isCollectorPage() && this.collectorPage.mouseDragged(click2, d3, d4)) {
                return true;
            } else if (this.isEventsPage() && this.eventsPage.mouseDragged(click2, d3, d4)) {
                return true;
            } else if (this.dragging && this.isLeftButton(click2.button())) {
                this.x = (float)click2.x() - this.dragOffsetX;
                this.y = (float)click2.y() - this.dragOffsetY;
                this.clampPosition();
                return true;
            } else {
                boolean flag = this.searchPanel.isOpen();
                return !flag && this.configSelector.mouseDragged(click2, d3, d4) ? true : !flag && this.espEditorPanel.mouseDragged(click2, d3, d4);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (this.closing) {
            return true;
        }

        if (this.introOverlay.isActive()) {
            return true;
        }

        double d5 = GuiScale.toScaled(d);
        double d6 = GuiScale.toScaled(d2);
        if (this.isBindPopupOpen()) {
            this.bindPopup.mouseScrolled(d5, d6, d3, d4);
            return true;
        }

        if (this.scriptWindowManager.onMouseScroll(d5, d6, d3, d4)) {
            return true;
        }

        if (this.scriptEditor.isOpen()) {
            this.scriptEditor.mouseScrolled(d5, d6, d3, d4);
            return true;
        }

        if (this.reloading) {
            return true;
        }

        if (this.ambiencePanel.isOpen()) {
            this.ambiencePanel.mouseScrolled(d5, d6, d3, d4);
            return true;
        }

        if (this.hudStylePanel.isOpen()) {
            this.hudStylePanel.mouseScrolled(d5, d6, d3, d4);
            return true;
        }

        if (this.hotkeysPanel.isOpen()) {
            this.hotkeysPanel.mouseScrolled(d5, d6, d3, d4);
            return true;
        }

        if (this.cosmeticsPanel.isOpen()) {
            this.cosmeticsPanel.mouseScrolled(d5, d6, d3, d4);
            return true;
        }

        if (this.serverAssistPanel.isOpen()) {
            this.serverAssistPanel.mouseScrolled(d5, d6, d3, d4);
            return true;
        }

        if (this.neuroPanel.isOpen()) {
            this.neuroPanel.mouseScrolled(d5, d6, d3, d4);
            return true;
        }

        if (this.isSidePanelVisible()) {
            return true;
        }

        if (this.profileMenu.isOpen() && this.profileMenu.isMouseOver(d5, d6)) {
            return this.profileMenu.mouseScrolled(d5, d6, d3, d4);
        }

        if (!this.settingsPopup.isOpen()
            || !this.settingsPopup.isMouseOverPanel(d5, d6) && !this.settingsPopup.isMouseOverAny(d5, d6) && !this.settingsPopup.hasOpenOverlay()) {
            if (this.profilePopup.isVisible() && this.profilePopup.isHoveringPopup(d5, d6)) {
                return this.profilePopup.mouseScrolled(d5, d6, d3, d4);
            } else {
                boolean flag = this.searchPanel.isOpen();
                if (!flag && this.configSelector.mouseScrolled(d5, d6, d3, d4)) {
                    return true;
                } else if (this.searchPanel.mouseScrolled(d5, d6, d3, d4)) {
                    return true;
                } else if (!flag && this.isOverEspEditor(d5, d6)) {
                    return this.espEditorPanel.mouseScrolled(d5, d6, d3, d4);
                } else if (!flag && this.isAutoBuyPage() && this.autoBuyPage.mouseScrolled(d5, d6, d3, d4)) {
                    return true;
                } else if (!flag && this.isCollectorPage() && this.collectorPage.mouseScrolled(d5, d6, d3, d4)) {
                    return true;
                } else if (!flag && this.isEventsPage() && this.eventsPage.mouseScrolled(d5, d6, d3, d4)) {
                    return true;
                } else if (!flag && this.isPartyPage() && this.partyPage.handleMouseScroll(d5, d6, d3, d4)) {
                    return true;
                } else if (!flag && this.isScriptsPage() && this.scriptsPage.mouseScrolled(d5, d6, d3, d4)) {
                    return true;
                } else if (!flag && this.isHovered(d5, d6, this.x, this.y, 90.0, this.height) && this.sidebar.mouseScrolled(d5, d6, d4)) {
                    return true;
                } else if (!flag
                    && this.sidebar.getSelectedHubCategory() == null
                    && this.isHovered(d5, d6, this.contentX, this.contentY, this.contentWidth + 6.0F, this.contentHeight)) {
                    this.scrollbar.onScroll(d4);
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return this.settingsPopup.mouseScrolled(d5, d6, d3, d4);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (!this.closing && this.openAnimation.getValue() > 0.0) {
            this.openAnimation.animate(0.0, 0.2, Easings.QUAD_IN);
            this.closing = true;
        }

        return false;
    }

    public ClickGuiScreen withReturnScreen(Screen screen) {
        this.returnScreen = screen;
        return this;
    }

    @Override
    public void close() {
        if (this.returnScreen != null) {
            Screen screen = this.returnScreen;
            this.returnScreen = null;
            mc.setScreen(screen);
        } else if (mc.world == null) {
            mc.setScreen(Sadnes.getInstance().getMainMenu());
        } else {
            super.close();
        }
    }

    @Override
    public void removed() {
        this.autoBuyPage.onClose();
        this.collectorPage.onOpen();
        FiguraAvatarManager.clearAllPreviews();
        Sadnes.getInstance().getConfigManager().saveActive();
        this.introOverlay.stop();
        this.profileMenu.reset();
        this.scriptWindowManager.closeAll();
        TextFieldComponent.unfocusAll();
    }

    private void tickClose() {
        if (this.closing && this.openAnimation.isFinished()) {
            this.close();
            this.closing = false;
        }
    }

    private void tickLogoAnimation() {
        this.logoSpinAnimation.update();
        this.logoSlideAnimation.update();
        this.logoPressAnimation.update();
        if (this.logoAnimState != ClickGuiScreen.LogoAnimState.IDLE && this.logoPressAnimation.isFinished() && this.logoPressAnimation.getValue() > 0.5) {
            this.logoPressAnimation.animate(0.0, 0.22, Easings.BACK_OUT);
        }

        switch (this.logoAnimState) {
            case PLAYING:
                if (this.logoSpinAnimation.isFinished()) {
                    this.logoSlideAnimation.animate(1.0, 0.55, Easings.BACK_OUT);
                    this.logoAnimState = ClickGuiScreen.LogoAnimState.SLIDING_IN;
                    return;
                }
                break;
            case SLIDING_IN:
                if (this.logoSlideAnimation.isFinished()) {
                    this.logoAnimState = ClickGuiScreen.LogoAnimState.DWELL;
                    this.logoDwellStart = System.currentTimeMillis();
                    return;
                }
                break;
            case DWELL:
                if (System.currentTimeMillis() - this.logoDwellStart >= 1100L) {
                    this.logoSlideAnimation.animate(0.0, 0.5, Easings.BACK_OUT);
                    this.logoAnimState = ClickGuiScreen.LogoAnimState.SLIDING_OUT;
                    return;
                }
                break;
            case SLIDING_OUT:
                if (this.logoSlideAnimation.isFinished()) {
                    if (!this.logoSecondPass) {
                        this.logoSecondPass = true;
                        this.logoSlideAnimation.animate(1.0, 0.55, Easings.BACK_OUT);
                        this.logoAnimState = ClickGuiScreen.LogoAnimState.SLIDING_IN;
                        return;
                    }

                    this.logoSecondPass = false;
                    this.logoAnimState = ClickGuiScreen.LogoAnimState.IDLE;
                }
        }
    }

    private void startLogoAnimation() {
        if (this.logoAnimState == ClickGuiScreen.LogoAnimState.IDLE) {
            this.logoSpinAnimation.setValue(0.0);
            this.logoSpinAnimation.animate(1.0, 1.4, Easings.QUART_OUT);
            this.logoPressAnimation.setValue(0.0);
            this.logoPressAnimation.animate(1.0, 0.12, Easings.QUART_OUT);
            this.logoSecondPass = false;
            this.logoAnimState = ClickGuiScreen.LogoAnimState.PLAYING;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private boolean isOverDragArea(double d, double d2) {
        boolean flag = this.searchPanel.isOpen();
        if (!this.isHovered(d, d2, this.x, this.y, this.width, this.height)) {
            return false;
        } else if (this.isHovered(d, d2, this.logoX, this.logoY, 24.0, 24.0)) {
            return false;
        } else if (this.isOverProfile(d, d2)) {
            return false;
        } else if (!flag && this.configSelector.isMouseOver(d, d2)) {
            return false;
        } else if (this.sidebar.isMouseOver(d, d2)) {
            return false;
        } else {
            return flag ? true : !this.isOverContent(d, d2);
        }
    }

    private boolean isOverContent(double d, double d2) {
        return this.sidebar.getSelectedHubCategory() == null && this.isHovered(d, d2, this.contentX, this.contentY, this.contentWidth, this.contentHeight);
    }

    private boolean isOverProfile(double d, double d2) {
        return this.isHovered(d, d2, this.profileX, this.profileY, this.profileWidth, this.profileHeight);
    }

    private boolean isEventsPage() {
        return this.sidebar.getSelectedHubCategory() == HubCategory.EVENTS;
    }

    private boolean isPartyPage() {
        return this.sidebar.getSelectedHubCategory() == HubCategory.PARTY;
    }

    private boolean isScriptsPage() {
        return this.sidebar.getSelectedHubCategory() == HubCategory.SCRIPTS;
    }

    public static boolean isTypingText() {
        if (TextFieldComponent.isAnyFocused()) {
            return true;
        }

        ClickGuiScreen clickGuiScreen = Sadnes.getInstance().getClickGui();
        return clickGuiScreen != null && clickGuiScreen.scriptEditor.isOpen();
    }

    private boolean isAutoBuyPage() {
        return this.sidebar.getSelectedHubCategory() == HubCategory.AUCTION && this.sidebar.getSelectedUtilitiesSubCategory() == UtilitiesSubCategory.AUTOBUY;
    }

    private boolean isCollectorPage() {
        return this.sidebar.getSelectedHubCategory() == HubCategory.AUCTION && this.sidebar.getSelectedUtilitiesSubCategory() == UtilitiesSubCategory.COLLECTOR;
    }

    private boolean isOverEspEditor(double d, double d2) {
        return this.sidebar.getSelectedHubCategory() == null
            && this.sidebar.getSelectedCategory() == Category.VISUALS
            && this.sidebar.getSelectedVisualsSubCategory() == VisualsSubCategory.PLAYER
            && this.espEditorWidth > 0.0F
            && this.espEditorHeight > 0.0F
            && this.isHovered(d, d2, this.espEditorX, this.espEditorY, this.espEditorWidth, this.espEditorHeight);
    }

    private float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    private void clampPosition() {
        float f = Math.min(100.0F, Math.min(this.width, this.height));
        this.x = this.clamp(this.x, f - this.width, Math.max(0.0F, GuiScale.getScaledWidth() - f));
        this.y = this.clamp(this.y, 0.0F, Math.max(0.0F, GuiScale.getScaledHeight() - f));
    }

    public ProfilePopup getProfilePopup() {
        return this.profilePopup;
    }

    private Click toScaledClick(Click click) {
        return new Click(GuiScale.toScaled(click.x()), GuiScale.toScaled(click.y()), click.buttonInfo());
    }

    private void toggleSearch() {
        boolean flag = this.searchPanel.isOpen();
        this.searchPanel.setOpen(!flag);
        if (!flag) {
            this.onSearchOpened();
        }
    }

    private void closeSearch() {
        this.searchPanel.setOpen(false);
    }

    private void onSearchOpened() {
        if (this.searchPanel.isOpen()) {
            this.dragging = false;
            this.configSelector.close();
            this.settingsPopup.close();
            this.ambiencePanel.close();
            this.hudStylePanel.close();
            this.hotkeysPanel.close();
            this.cosmeticsPanel.close();
            this.serverAssistPanel.close();
            this.neuroPanel.close();
            this.closeBindPopup();
        }
    }

    private void renderScriptScreenBackground(DrawContext drawContext) {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null) {
            scriptManager.renderClickGuiScreenBackground(drawContext, GuiScale.getScaledWidth(), GuiScale.getScaledHeight(), this.openAnimation.get());
        }
    }

    public ModuleRowComponent getRow(Module module) {
        return this.rowsBySource.get(module);
    }

    public Set<Integer> getHeldKeys() {
        return this.heldKeys;
    }

    public Animation getOpenAnimation() {
        return this.openAnimation;
    }

    public Animation getCategorySwitchAnimation() {
        return this.categorySwitchAnimation;
    }

    public float getLastScale() {
        return this.lastScale;
    }

    public boolean isClosing() {
        return this.closing;
    }

    public List<ModuleRowComponent> getRows() {
        return this.rows;
    }

    public Map<Module, ModuleRowComponent> getRowsBySource() {
        return this.rowsBySource;
    }

    public Map<CategoryDescriptor, List<OptionRowGroup>> getGroupsByCategory() {
        return this.groupsByCategory;
    }

    public OptionRowGroup getEspGroup() {
        return this.espGroup;
    }

    public List<OptionRowGroup> getVisibleGroups() {
        return this.visibleGroups;
    }

    public SmoothScrollbar getScrollbar() {
        return this.scrollbar;
    }

    public Map<CategoryDescriptor, Float> getScrollByCategory() {
        return this.scrollByCategory;
    }

    public ConfigSelectorComponent getConfigSelector() {
        return this.configSelector;
    }

    public CategorySidebar getSidebar() {
        return this.sidebar;
    }

    public EspEditorPanel getEspEditorPanel() {
        return this.espEditorPanel;
    }

    public SearchPanel getSearchPanel() {
        return this.searchPanel;
    }

    public SettingsPopupComponent getSettingsPopup() {
        return this.settingsPopup;
    }

    public AmbiencePanel getAmbiencePanel() {
        return this.ambiencePanel;
    }

    public HotkeysPanel getHotkeysPanel() {
        return this.hotkeysPanel;
    }

    public CosmeticsPanel getCosmeticsPanel() {
        return this.cosmeticsPanel;
    }

    public ServerAssistPanel getServerAssistPanel() {
        return this.serverAssistPanel;
    }

    public NeuroPanel getNeuroPanel() {
        return this.neuroPanel;
    }

    public ScriptPanelOverlay getHudStylePanel() {
        return this.hudStylePanel;
    }

    public AutoBuyPage getAutoBuyPage() {
        return this.autoBuyPage;
    }

    public CollectorPage getCollectorPage() {
        return this.collectorPage;
    }

    public EventsPage getEventsPage() {
        return this.eventsPage;
    }

    public PartyPage getPartyPage() {
        return this.partyPage;
    }

    public ScriptEditor getScriptEditor() {
        return this.scriptEditor;
    }

    public ScriptWindowManager getScriptWindowManager() {
        return this.scriptWindowManager;
    }

    public StopWatch getReloadTimer() {
        return this.reloadTimer;
    }

    public Animation getReloadAnimation() {
        return this.reloadAnimation;
    }

    public boolean isReloading() {
        return this.reloading;
    }

    public ScriptsPage getScriptsPage() {
        return this.scriptsPage;
    }

    public ChatPanel getProfileMenu() {
        return this.profileMenu;
    }

    public float getProfileX() {
        return this.profileX;
    }

    public float getProfileY() {
        return this.profileY;
    }

    public float getProfileWidth() {
        return this.profileWidth;
    }

    public float getProfileHeight() {
        return this.profileHeight;
    }

    public BindPopupComponent getBindPopup() {
        return this.bindPopup;
    }

    public Object getBindTarget() {
        return this.bindTarget;
    }

    public Animation getLogoSpinAnimation() {
        return this.logoSpinAnimation;
    }

    public Animation getLogoSlideAnimation() {
        return this.logoSlideAnimation;
    }

    public Animation getLogoPressAnimation() {
        return this.logoPressAnimation;
    }

    public ClickGuiScreen.LogoAnimState getLogoAnimState() {
        return this.logoAnimState;
    }

    public long getLogoDwellStart() {
        return this.logoDwellStart;
    }

    public boolean isLogoSecondPass() {
        return this.logoSecondPass;
    }

    public float getLogoX() {
        return this.logoX;
    }

    public float getLogoY() {
        return this.logoY;
    }

    public IntroOverlay getIntroOverlay() {
        return this.introOverlay;
    }

    public float getContentX() {
        return this.contentX;
    }

    public float getContentY() {
        return this.contentY;
    }

    public float getContentWidth() {
        return this.contentWidth;
    }

    public float getContentHeight() {
        return this.contentHeight;
    }

    public float getContentTotalHeight() {
        return this.contentTotalHeight;
    }

    public float getEspEditorX() {
        return this.espEditorX;
    }

    public float getEspEditorY() {
        return this.espEditorY;
    }

    public float getEspEditorWidth() {
        return this.espEditorWidth;
    }

    public float getEspEditorHeight() {
        return this.espEditorHeight;
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

    public float getHeight() {
        return this.height;
    }

    public boolean isPositioned() {
        return this.positioned;
    }

    public boolean isDragging2() {
        return this.dragging;
    }

    public float getDragOffsetX() {
        return this.dragOffsetX;
    }

    public float getDragOffsetY() {
        return this.dragOffsetY;
    }

    public Screen getReturnScreen() {
        return this.returnScreen;
    }

    public ClickGuiScreen.BlurMode getBlurMode() {
        return this.blurMode;
    }

    public void setBlurMode(ClickGuiScreen.BlurMode blurMode2) {
        this.blurMode = blurMode2;
    }

    public enum BlurMode {
        FULL,
        ONLY_MODULE,
        NONE;
    }

    enum LogoAnimState {
        IDLE,
        PLAYING,
        SLIDING_IN,
        DWELL,
        SLIDING_OUT;
    }
}
