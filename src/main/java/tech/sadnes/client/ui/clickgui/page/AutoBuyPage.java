package tech.sadnes.client.ui.clickgui.page;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.animation.StaggeredAppearAnimation;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.VerticalFadeMask;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.server.FunTimeAnarchies;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.autobuy.AutoBuyController;
import tech.sadnes.client.server.autobuy.EnchantRequirement;
import tech.sadnes.client.server.autobuy.PurchaseHistory;
import tech.sadnes.client.server.autobuy.ShulkerStash;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class AutoBuyPage implements IRenderable {
    private static final float SIDEBAR_WIDTH = 116.0F;
    private static final AutoBuyManager.Mode[] SPOOKYTIME_MODES = new AutoBuyManager.Mode[]{AutoBuyManager.Mode.BUY, AutoBuyManager.Mode.OFF};
    private static final AutoBuyManager.Mode[] DEFAULT_MODES = new AutoBuyManager.Mode[]{
        AutoBuyManager.Mode.BUY, AutoBuyManager.Mode.CHECK, AutoBuyManager.Mode.OFF
    };
    private static final float BUTTON_HEIGHT = 15.0F;
    private static final float SEARCH_HEIGHT = 15.0F;
    private static final float ITEM_ROW_HEIGHT = 20.0F;
    private static final float ROW_GAP = 2.0F;
    private static final float FADE_OFFSET = 6.0F;
    private static final float HEADER_HEIGHT = 24.0F;
    private static final float HISTORY_ROW_HEIGHT = 22.0F;
    private static final float OPTION_ROW_HEIGHT = 14.0F;
    private static final float SLIDER_WIDTH = 30.0F;
    private static final float SLIDER_HEIGHT = 3.0F;
    private static final float GROUP_GAP = 9.0F;
    private static final float TOGGLE_WIDTH = 20.0F;
    private static final float SECTION_LABEL_HEIGHT = 12.0F;
    private static final float FADE_HEIGHT = 14.0F;
    private static final float SELECTOR_INSET = 1.5F;
    private static final float PRICE_BOX_HEIGHT = 10.0F;
    private static final float BADGE_WIDTH = 22.0F;
    private static final float ANARCHY_CELL_WIDTH = 30.0F;
    private static final float ANARCHY_CELL_HEIGHT = 14.0F;
    private static final float SCROLLBAR_SPACE = 7.0F;
    private static final float GAP = 4.0F;
    private final ClickGuiScreen screen;
    private final SmoothScrollbar itemListScroll = new SmoothScrollbar();
    private final SmoothScrollbar detailsScroll = new SmoothScrollbar();
    private final SmoothScrollbar historyScroll = new SmoothScrollbar();
    private final SmoothScrollbar stashScroll = new SmoothScrollbar();
    private final TextFieldComponent searchField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "autobuy.page.search", false, false);
    private final TextFieldComponent historySearchField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "autobuy.page.search", false, false);
    private final TextFieldComponent priceField = new TextFieldComponent(Fonts.INTER_MEDIUM, 5.5F, "autobuy.page.price", false, true);
    private final Map<AutoBuyItem, Animation> itemHoverAnimations = new IdentityHashMap<>();
    private final Map<AutoBuyItem, Animation> itemSelectAnimations = new IdentityHashMap<>();
    private final Map<AutoBuyItem, Animation> itemEnabledAnimations = new IdentityHashMap<>();
    private final Map<EnchantRequirement, Animation> optionStateAnimations = new IdentityHashMap<>();
    private final Map<EnchantRequirement, Animation> optionHoverAnimations = new IdentityHashMap<>();
    private final List<AutoBuyPage.ItemBounds> itemBounds = new ArrayList<>();
    private final List<AutoBuyPage.OptionBounds> optionBounds = new ArrayList<>();
    private final List<AutoBuyItem> filteredItems = new ArrayList<>();
    private final Animation searchHoverAnimation = new Animation();
    private final Animation priceHoverAnimation = new Animation();
    private final Animation historyButtonAnimation = new Animation();
    private final Animation stashButtonAnimation = new Animation();
    private final Animation stashRunAnimation = new Animation();
    private final Animation stashHereAnimation = new Animation();
    private final Animation historySearchAnimation = new Animation();
    private final Animation filterButtonAnimation = new Animation();
    private final List<PurchaseHistory.Entry> filteredHistory = new ArrayList<>();
    private AutoBuyPage.HistoryFilter historyFilter = AutoBuyPage.HistoryFilter.ALL;
    private AutoBuyPage.HistoryFilter cachedFilter;
    private String cachedQuery = "";
    private int cachedHistorySize = -1;
    private final Animation modeSelectorAnimation = new Animation();
    private final Animation contentFadeAnimation = new Animation();
    private final StaggeredAppearAnimation staggerAnimation = new StaggeredAppearAnimation();
    private final Animation selectedEnabledAnimation = new Animation();
    private final List<AutoBuyPage.AnarchyBounds> anarchyBounds = new ArrayList<>();
    private final Map<Integer, Animation> anarchyHoverAnimations = new HashMap<>();
    private AutoBuyItem selectedItem;
    private boolean historyOpen;
    private boolean stashOpen;
    private boolean editingPrice;
    private String lastSearchQuery = "";
    private EnchantRequirement draggingOption;
    private float sliderX;
    private final Animation sliderDragAnimation = new Animation();
    private float listX;
    private float listY;
    private float listWidth;
    private float listHeight;
    private float modeX;
    private float modeY;
    private float modeWidth;
    private float historyButtonX;
    private float toolbarY;
    private float historyButtonWidth;
    private float stashButtonX;
    private float stashButtonWidth;
    private float stashRunX;
    private float stashRowY;
    private float stashRunWidth;
    private float stashHereX;
    private float stashHereWidth;
    private float historySearchX;
    private float historySearchWidth;
    private float filterButtonX;
    private float filterButtonWidth;
    private float searchX;
    private float searchY;
    private float searchWidth;
    private float contentX;
    private float contentY;
    private float contentWidth;
    private float contentHeight;
    private float innerX;
    private float innerWidth;
    private float toggleX;
    private float toggleY;
    private float priceBoxX;
    private float priceBoxY;
    private float priceBoxWidth;
    private float priceRowY;

    public AutoBuyPage(ClickGuiScreen clickGuiScreen) {
        this.screen = clickGuiScreen;
    }

    public void onClose() {
        this.staggerAnimation.reset();
        this.commitPriceEdit();
        this.searchField.setFocused(false);
        this.priceField.setFocused(false);
        this.historySearchField.setFocused(false);
        if (this.selectedItem != null) {
            Iterator iterator2 = this.selectedItem.getSettings().iterator();

            while (iterator2.hasNext()) {
                ((Setting)iterator2.next()).close();
            }
        }
    }

    private Setting<?> getTypingSetting() {
        if (this.selectedItem == null) {
            return null;
        }

        for (Setting setting : this.selectedItem.getSettings()) {
            if (setting.isTyping()) {
                return setting;
            }
        }

        return null;
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.staggerAnimation.update();
        float f6 = this.getPageAlpha();
        float f7 = f3 + 15.0F + 4.0F;
        this.modeX = f2;
        this.modeY = f3 + this.staggerAnimation.getOffset(0);
        this.modeWidth = 116.0F;
        this.searchX = f2;
        this.searchY = f7 + this.staggerAnimation.getOffset(1);
        this.searchWidth = 116.0F;
        this.listX = f2;
        this.listY = f7 + 15.0F + 4.0F;
        this.listWidth = 116.0F;
        this.listHeight = f3 + f5 - this.listY;
        this.contentX = f2 + 116.0F + 12.0F;
        this.contentY = f7;
        this.contentWidth = f2 + f4 - this.contentX;
        this.contentHeight = f3 + f5 - this.contentY;
        this.innerX = this.contentX;
        this.innerWidth = this.contentWidth - 7.0F;
        this.toolbarY = f3 + this.staggerAnimation.getOffset(1);
        this.historyButtonX = this.innerX;
        this.stashButtonWidth = 15.0F;
        this.stashButtonX = this.innerX + this.innerWidth - this.stashButtonWidth;
        this.historyButtonWidth = this.stashButtonX - 4.0F - this.historyButtonX;
        this.renderModeSelector(drawContext, f6 * this.staggerAnimation.getProgress(0));
        this.renderSearchField(drawContext, i, i2, f6 * this.staggerAnimation.getProgress(1));
        this.renderHistoryButton(drawContext, i, i2, f6 * this.staggerAnimation.getProgress(1));
        this.renderStashButton(drawContext, i, i2, f6 * this.staggerAnimation.getProgress(1));
        this.refreshFilteredItems();
        if (this.selectedItem == null && !this.filteredItems.isEmpty()) {
            this.selectItem(this.filteredItems.get(0));
        }

        GuiRenderUtil.drawOverlay(drawContext, MathUtil.roundToStep(f2 + 116.0F + 6.0F, 0.5F), f3, 0.5F, f5, f6 * this.staggerAnimation.getProgress(0));
        this.renderItemList(drawContext, i, i2, f6);
        float f8 = f6 * this.staggerAnimation.getProgress(2);
        if (this.stashOpen) {
            this.renderStashView(drawContext, i, i2, f8);
        } else if (this.historyOpen) {
            this.renderHistoryView(drawContext, i, i2, f8);
        } else {
            this.renderItemDetails(drawContext, i, i2, f, f8);
        }
    }

    private void renderModeSelector(DrawContext drawContext, float f) {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager != null && !(this.modeWidth <= 0.0F)) {
            AutoBuyManager.Mode[] modes = this.getAvailableModes();
            int i = modes.length;
            this.modeSelectorAnimation.update();
            this.modeSelectorAnimation.animate(indexOfMode(modes, autoBuyManager.getMode()), 0.3, Easings.QUAD_OUT);
            float f2 = this.modeWidth / i;
            float f3 = Math.min(Math.max(this.modeSelectorAnimation.get(), 0.0F), i - 1.0F);
            GuiRenderUtil.drawOutlined(drawContext, this.modeX, this.modeY, this.modeWidth, 15.0F, f * 0.7F, 5.0F);
            GuiRenderUtil.drawHighlight(drawContext, this.modeX + 1.5F + f2 * f3, this.modeY + 1.5F, f2 - 3.0F, 12.0F, f * 2.0F, Radius.all(4.0F));

            for (int i2 = 0; i2 < i; i2++) {
                float f4 = Math.max(0.0F, 1.0F - Math.abs(f3 - i2));
                this.renderModeLabel(drawContext, getModeLabel(modes[i2]), this.modeX + f2 * i2, f2, f4, f);
            }
        }
    }

    private AutoBuyManager.Mode[] getAvailableModes() {
        return ServerManager.isSpookyTime() ? SPOOKYTIME_MODES : DEFAULT_MODES;
    }

    private static int indexOfMode(AutoBuyManager.Mode[] modes, AutoBuyManager.Mode mode) {
        int i = 0;

        for (int i2 = modes.length; i < i2; i++) {
            if (modes[i] == mode) {
                return i;
            }
        }

        return 0;
    }

    private static String getModeLabel(AutoBuyManager.Mode mode) {
        switch (mode) {
            case BUY:
                return Language.translate("autobuy.page.mode.buy", "Покупка");
            case CHECK:
                return Language.translate("autobuy.page.mode.check", "Проверка");
            case OFF:
                return Language.translate("autobuy.page.mode.off", "Выкл.");
            default:
                throw new MatchException(null, null);
        }
    }

    private void renderModeLabel(DrawContext drawContext, String string, float f, float f2, float f3, float f4) {
        int i = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.45F + 0.55F * f3), f4);
        new RenderUtil(drawContext, f, this.modeY, f2, 15.0F).textCenter(Fonts.INTER_MEDIUM, string, i, 6.0F);
    }

    private void renderSearchField(DrawContext drawContext, int i, int i2, float f) {
        this.searchHoverAnimation.update();
        boolean flag;
        if (!this.isHovered(i, i2, this.searchX, this.searchY, this.searchWidth, 15.0)) {
            flag = this.searchField.isFocused();
        } else {
            flag = true;
        }

        this.searchHoverAnimation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        GuiRenderUtil.drawOutlined(drawContext, this.searchX, this.searchY, this.searchWidth, 15.0F, f * (0.7F + 0.2F * this.searchHoverAnimation.get()), 5.0F)
            .icon(Icon.SEARCH, 3.5F, 3.5F, 8.0F, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f));
        this.searchField
            .render(
                drawContext,
                new Vector2f(this.searchX + 14.0F, this.searchY + 7.5F - 3.0F),
                ColorUtil.multiplyAlpha(Theme.getTextColor(), f),
                this.searchWidth - 17.0F
            );
        if (!this.searchField.getText().equals(this.lastSearchQuery)) {
            this.lastSearchQuery = this.searchField.getText();
            this.itemListScroll.reset();
        }
    }

    private void renderHistoryButton(DrawContext drawContext, int i, int i2, float f) {
        this.historyButtonAnimation.update();
        boolean flag = this.isHovered(i, i2, this.historyButtonX, this.toolbarY, this.historyButtonWidth, 15.0);
        this.historyButtonAnimation.animate(!flag && !this.historyOpen ? 0.0 : 1.0, 0.25, Easings.QUAD_OUT);
        float f2 = (float)Math.min(this.historyButtonAnimation.get(), 1.0);
        GuiRenderUtil.drawOutlined(drawContext, this.historyButtonX, this.toolbarY, this.historyButtonWidth, 15.0F, f * (0.7F + 0.2F * f2), 5.0F);
        int i3 = ColorUtil.multiplyAlpha(this.historyOpen ? Theme.getPrimary() : Theme.withAlpha(Theme.getTextColor(), 0.55F + 0.35F * f2), f);
        new RenderUtil(drawContext, this.historyButtonX, this.toolbarY, this.historyButtonWidth, 15.0F)
            .icon(Icon.RECTANGLE_LIST, 5.0F, 3.5F, 8.0F, i3)
            .textMiddleLeft(Fonts.INTER_MEDIUM, Language.translate("autobuy.page.history", "Журнал"), 16.0F, 0.0F, i3, 6.0F);
    }

    private void renderStashButton(DrawContext drawContext, int i, int i2, float f) {
        this.stashButtonAnimation.update();
        boolean flag = this.isHovered(i, i2, this.stashButtonX, this.toolbarY, this.stashButtonWidth, 15.0);
        this.stashButtonAnimation.animate(!flag && !this.stashOpen ? 0.0 : 1.0, 0.25, Easings.QUAD_OUT);
        float f2 = (float)Math.min(this.stashButtonAnimation.get(), 1.0);
        GuiRenderUtil.drawOutlined(drawContext, this.stashButtonX, this.toolbarY, this.stashButtonWidth, 15.0F, f * (0.7F + 0.2F * f2), 5.0F);
        int i3 = ColorUtil.multiplyAlpha(this.stashOpen ? Theme.getPrimary() : Theme.withAlpha(Theme.getTextColor(), 0.55F + 0.35F * f2), f);
        new RenderUtil(drawContext, this.stashButtonX, this.toolbarY, this.stashButtonWidth, 15.0F)
            .icon(Icon.LOCATION_DOT, this.stashButtonWidth / 2.0F - 4.0F, 3.5F, 8.0F, i3);
    }

    private void renderStashView(DrawContext drawContext, int i, int i2, float f) {
        this.optionBounds.clear();
        this.anarchyBounds.clear();
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager != null) {
            float f2 = this.getContentFade();
            float f3 = f * f2;
            float f4 = (1.0F - f2) * 6.0F;
            this.renderStashControls(drawContext, i, i2, autoBuyManager, f3);
            float f5 = this.contentY + 15.0F + 4.0F;
            float f6 = this.contentY + this.contentHeight - f5;
            if (!(f6 <= 0.0F)) {
                List list = FunTimeAnarchies.getRanges();
                int i3 = Math.max(1, (int)((this.innerWidth + 3.0F) / 33.0F));
                float f7 = this.computeAnarchyGridHeight(list, i3);
                this.stashScroll.update(f7, f6);
                this.stashScroll.updateHover(i, i2);
                if (f7 > f6) {
                    this.stashScroll.render(drawContext, this.contentX + this.contentWidth - 2.5F, f5, f6, f7, f3);
                }

                if (ScissorStack.push(drawContext, this.contentX - 2.0F, f5, this.contentWidth + 4.0F, f6)) {
                    try {
                        this.applyScrollFade(this.stashScroll, f5, f6, f7);
                        float f8 = f5 + this.stashScroll.getScroll() + f4;
                        int i4 = list.size();

                        for (int i5 = 0; i5 < i4; i5++) {
                            int[] ints = (int[])list.get(i5);
                            int i6 = 0;

                            for (int i7 = ints[0]; i7 <= ints[1]; i6++) {
                                float f9 = this.innerX + i6 % i3 * 33.0F;
                                float f10 = f8 + i6 / i3 * 17.0F;
                                if (!(f10 + 14.0F < f5) && !(f10 > f5 + f6)) {
                                    this.renderAnarchyCell(drawContext, i7, f9, f10, i, i2, autoBuyManager, f3);
                                }

                                i7++;
                            }

                            f8 += this.ceilDiv(i6, i3) * 17.0F;
                            if (i5 != i4 - 1) {
                                float f11 = MathUtil.roundToStep(f8 + 4.5F, 0.5F);
                                if (f11 >= f5 && f11 <= f5 + f6) {
                                    GuiRenderUtil.drawOverlay(drawContext, this.innerX, f11, this.innerWidth, 0.5F, f3);
                                }

                                f8 += 9.0F;
                            }
                        }
                    } finally {
                        VerticalFadeMask.end();
                        ScissorStack.pop(drawContext);
                    }
                }
            }
        }
    }

    private float computeAnarchyGridHeight(List<int[]> intses, int i) {
        float f = 0.0F;
        int i2 = intses.size();

        for (int i3 = 0; i3 < i2; i3++) {
            int[] ints = (int[])intses.get(i3);
            f += this.ceilDiv(ints[1] - ints[0] + 1, i) * 17.0F;
            if (i3 < i2 - 1) {
                f += 9.0F;
            }
        }

        return f;
    }

    private int ceilDiv(int i, int i2) {
        return (i + i2 - 1) / i2;
    }

    private void renderAnarchyCell(DrawContext drawContext, int i, float f, float f2, int i2, int i3, AutoBuyManager autoBuyManager, float f3) {
        this.anarchyBounds.add(new AutoBuyPage.AnarchyBounds(i, f, f2, 30.0F, 14.0F));
        boolean flag = autoBuyManager.getStashAnarchy() == i;
        Animation animation = getAnimation(this.anarchyHoverAnimations, i);
        animation.update();
        animation.animate(this.isHovered(i2, i3, f, f2, 30.0, 14.0) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f4 = (float)Math.min(animation.get(), 1.0);
        GuiRenderUtil.drawOutlined(drawContext, f, f2, 30.0F, 14.0F, f3 * (flag ? 0.95F : 0.55F + 0.3F * f4), 3.0F);
        int i4 = ColorUtil.multiplyAlpha(
            flag
                ? Theme.getPrimary()
                : (
                    i == ServerManager.getAnarchyNumber()
                        ? Theme.withAlpha(Theme.getPrimary(), 0.55F + 0.35F * f4)
                        : Theme.withAlpha(Theme.getTextColor(), 0.55F + 0.4F * f4)
                ),
            f3
        );
        new RenderUtil(drawContext, f, f2, 30.0F, 14.0F).textCenter(Fonts.INTER_MEDIUM, String.valueOf(i), i4, 6.0F);
    }

    private void renderStashControls(DrawContext drawContext, int i, int i2, AutoBuyManager autoBuyManager, float f) {
        this.stashRunX = this.innerX;
        this.stashRowY = this.contentY;
        this.stashHereWidth = 64.0F;
        this.stashHereX = this.innerX + this.innerWidth - this.stashHereWidth;
        this.stashRunWidth = this.innerWidth - this.stashHereWidth - 4.0F;
        this.renderStashHereButton(drawContext, i, i2, autoBuyManager, f);
        this.stashRunAnimation.update();
        boolean flag = this.isHovered(i, i2, this.stashRunX, this.stashRowY, this.stashRunWidth, 15.0);
        this.stashRunAnimation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        float f2 = (float)Math.min(this.stashRunAnimation.get(), 1.0);
        GuiRenderUtil.drawOutlined(drawContext, this.stashRunX, this.stashRowY, this.stashRunWidth, 15.0F, f * (0.7F + 0.2F * f2), 5.0F);
        ShulkerStash shulkerStash = ShulkerStash.getInstance();
        boolean flag2;
        if (shulkerStash != null) {
            flag2 = shulkerStash.isActive();
        } else {
            flag2 = false;
        }

        int i3 = autoBuyManager.getStashAnarchy();
        String string = flag2
            ? Language.translate("autobuy.page.stash.busy", "Вывоз идёт")
            : (
                i3 > 0
                    ? Language.translate("autobuy.page.stash.run", "Вывезти на") + " " + i3
                    : Language.translate("autobuy.page.stash.pick", "Выберите анархию")
            );
        int i4 = ColorUtil.multiplyAlpha(
            i3 > 0 ? Theme.withAlpha(Theme.getTextColor(), 0.6F + 0.35F * f2) : Theme.withAlpha(Theme.getSecondaryText(), 0.5F), f
        );
        new RenderUtil(drawContext, this.stashRunX, this.stashRowY, this.stashRunWidth, 15.0F).textCenter(Fonts.INTER_MEDIUM, string, i4, 6.0F);
    }

    private void renderStashHereButton(DrawContext drawContext, int i, int i2, AutoBuyManager autoBuyManager, float f) {
        int i3 = ServerManager.getAnarchyNumber();
        boolean flag = i3 > 0 && autoBuyManager.getStashAnarchy() != i3;
        this.stashHereAnimation.update();
        this.stashHereAnimation
            .animate(flag && this.isHovered(i, i2, this.stashHereX, this.stashRowY, this.stashHereWidth, 15.0) ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        float f2 = (float)Math.min(this.stashHereAnimation.get(), 1.0);
        GuiRenderUtil.drawOutlined(drawContext, this.stashHereX, this.stashRowY, this.stashHereWidth, 15.0F, f * (0.6F + 0.25F * f2), 5.0F);
        int i4 = ColorUtil.multiplyAlpha(
            i3 > 0 ? Theme.withAlpha(Theme.getTextColor(), 0.55F + 0.4F * f2) : Theme.withAlpha(Theme.getSecondaryText(), 0.35F), f
        );
        String string = Language.translate("autobuy.page.stash.here", "Текущая") + (i3 > 0 ? " " + i3 : "");
        new RenderUtil(drawContext, this.stashHereX, this.stashRowY, this.stashHereWidth, 15.0F).textCenter(Fonts.INTER_MEDIUM, string, i4, 6.0F);
    }

    private void renderHistoryView(DrawContext drawContext, int i, int i2, float f) {
        this.optionBounds.clear();
        float f2 = this.getContentFade();
        float f3 = f * f2;
        float f4 = (1.0F - f2) * 6.0F;
        this.renderHistoryToolbar(drawContext, i, i2, f3);
        float f5 = this.contentY + 15.0F + 4.0F;
        float f6 = this.contentY + this.contentHeight - f5;
        if (!(f6 <= 0.0F)) {
            List list = this.getFilteredHistory();
            if (list.isEmpty()) {
                boolean flag = this.historyFilter != AutoBuyPage.HistoryFilter.ALL || !this.historySearchField.getText().isBlank();
                new RenderUtil(drawContext, this.innerX, f5 + f4, this.innerWidth, f6)
                    .icon(
                        Icon.RECTANGLE_LIST, this.innerWidth / 2.0F - 9.0F, f6 / 2.0F - 16.0F, 18.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.3F * f3)
                    )
                    .textCenter(
                        Fonts.INTER_MEDIUM,
                        flag ? Language.translate("autobuy.page.empty", "Ничего не найдено") : Language.translate("autobuy.page.history.empty", "Пока пусто"),
                        0.0F,
                        8.0F,
                        Theme.withAlpha(Theme.getSecondaryText(), 0.5F * f3),
                        6.5F
                    );
            } else {
                int i3 = list.size();
                float f7 = i3 * 22.0F;
                this.historyScroll.update(f7, f6);
                this.historyScroll.updateHover(i, i2);
                if (f7 > f6) {
                    this.historyScroll.render(drawContext, this.contentX + this.contentWidth - 2.5F, f5, f6, f7, f3);
                }

                if (ScissorStack.push(drawContext, this.contentX - 2.0F, f5, this.contentWidth + 4.0F, f6)) {
                    try {
                        this.applyScrollFade(this.historyScroll, f5, f6, f7);
                        float f8 = f5 + this.historyScroll.getScroll() + f4;

                        for (int i4 = 0; i4 < i3; i4++) {
                            if (f8 + 22.0F >= f5 && f8 <= f5 + f6) {
                                this.renderHistoryEntry(drawContext, (PurchaseHistory.Entry)list.get(i4), this.innerX, f8, this.innerWidth, i, i2, f3);
                            }

                            f8 += 22.0F;
                        }
                    } finally {
                        VerticalFadeMask.end();
                        ScissorStack.pop(drawContext);
                    }
                }
            }
        }
    }

    private void renderHistoryToolbar(DrawContext drawContext, int i, int i2, float f) {
        this.filterButtonWidth = 54.0F;
        this.historySearchX = this.innerX;
        this.historySearchWidth = this.innerWidth - this.filterButtonWidth - 4.0F;
        this.filterButtonX = this.innerX + this.innerWidth - this.filterButtonWidth;
        this.historySearchAnimation.update();
        boolean flag;
        if (!this.isHovered(i, i2, this.historySearchX, this.contentY, this.historySearchWidth, 15.0)) {
            flag = this.historySearchField.isFocused();
        } else {
            flag = true;
        }

        this.historySearchAnimation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        GuiRenderUtil.drawOutlined(
                drawContext, this.historySearchX, this.contentY, this.historySearchWidth, 15.0F, f * (0.7F + 0.2F * this.historySearchAnimation.get()), 5.0F
            )
            .icon(Icon.SEARCH, 3.5F, 3.5F, 8.0F, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f));
        this.historySearchField
            .render(
                drawContext,
                new Vector2f(this.historySearchX + 14.0F, this.contentY + 7.5F - 3.0F),
                ColorUtil.multiplyAlpha(Theme.getTextColor(), f),
                this.historySearchWidth - 17.0F
            );
        this.filterButtonAnimation.update();
        boolean flag2 = this.isHovered(i, i2, this.filterButtonX, this.contentY, this.filterButtonWidth, 15.0);
        this.filterButtonAnimation.animate(flag2 ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        float f2 = this.filterButtonAnimation.get();
        int i3 = ColorUtil.multiplyAlpha(
            this.historyFilter == AutoBuyPage.HistoryFilter.ALL ? Theme.withAlpha(Theme.getTextColor(), 0.55F + 0.35F * f2) : Theme.getPrimary(), f
        );
        GuiRenderUtil.drawOutlined(drawContext, this.filterButtonX, this.contentY, this.filterButtonWidth, 15.0F, f * (0.7F + 0.2F * f2), 5.0F)
            .icon(this.historyFilter.getIcon(), 4.0F, 3.5F, 8.0F, i3)
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, this.historyFilter.getLabel(), 14.0F, 0.0F, this.filterButtonWidth - 17.0F, i3, 5.5F, flag2);
    }

    private List<PurchaseHistory.Entry> getFilteredHistory() {
        List<PurchaseHistory.Entry> entries = PurchaseHistory.getEntries();
        String string = this.historySearchField.getText().trim().toLowerCase(Locale.ROOT);
        boolean flag;
        if (this.historyFilter == this.cachedFilter && string.equals(this.cachedQuery)) {
            flag = false;
            if (entries.size() == this.cachedHistorySize) {
                return this.filteredHistory;
            }
        } else {
            flag = true;
        }

        if (flag) {
            this.historyScroll.reset();
        }

        this.cachedFilter = this.historyFilter;
        this.cachedQuery = string;
        this.cachedHistorySize = entries.size();
        this.filteredHistory.clear();

        for (PurchaseHistory.Entry entry : entries) {
            if (this.historyFilter.accepts(entry.result()) && (string.isEmpty() || matchesQuery(entry, string))) {
                this.filteredHistory.add(entry);
            }
        }

        if (this.historyFilter == AutoBuyPage.HistoryFilter.PRICE) {
            this.filteredHistory.sort(Comparator.comparingInt(PurchaseHistory.Entry::price).reversed());
        }

        return this.filteredHistory;
    }

    private static boolean matchesQuery(PurchaseHistory.Entry entry, String string) {
        return entry.item().getId().toLowerCase(Locale.ROOT).contains(string) || entry.seller().toLowerCase(Locale.ROOT).contains(string);
    }

    private void renderHistoryEntry(DrawContext drawContext, PurchaseHistory.Entry entry, float f, float f2, float f3, int i, int i2, float f4) {
        boolean flag = entry.result() == PurchaseHistory.Result.BOUGHT;
        boolean flag2 = this.isHovered(i, i2, f, f2, f3, 22.0);
        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), flag ? 0.95F : 0.6F), f4);
        int i4 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.55F), f4);
        int i5 = flag ? ColorUtil.multiplyAlpha(Theme.getPrimary(), f4) : i4;
        GuiRenderUtil.drawItem(
            drawContext, entry.item().getDisplayStack(), f + 1.0F, f2 + 11.0F - 8.0F, f4 * VerticalFadeMask.getAlphaFactor(f2 + 11.0F) * (flag ? 1.0F : 0.6F)
        );
        String string = formatPrice(entry.price());
        float f5 = Fonts.INTER_MEDIUM.getWidth(string, 6.5F);
        String string2 = getResultLabel(entry.result()) + " · " + entry.seller() + " · " + entry.time();
        new RenderUtil(drawContext, f, f2, f3, 22.0F)
            .scrollingTextTop(Fonts.INTER_MEDIUM, entry.item().getId(), 21.0F, 3.5F, f3 - 27.0F - f5, i3, 6.5F, flag2)
            .textTopRight(Fonts.INTER_MEDIUM, string, 1.0F, 3.5F, i5, 6.5F)
            .scrollingTextBottom(Fonts.INTER_REGULAR, string2, 21.0F, 3.5F, f3 - 23.0F, i4, 6.0F, flag2);
    }

    private static String getResultLabel(PurchaseHistory.Result result) {
        switch (result) {
            case BOUGHT:
                return Language.translate("autobuy.bought", "Куплено");
            case LOST:
                return Language.translate("autobuy.buy.failed", "Лот не достался");
            case UNVERIFIED:
                return Language.translate("autobuy.verify.gone", "Лот не подтверждён");
            case NO_MONEY:
                return Language.translate("autobuy.buyitem.nomoney", "Не хватает денег");
            default:
                throw new MatchException(null, null);
        }
    }

    private void renderItemList(DrawContext drawContext, int i, int i2, float f) {
        this.itemBounds.clear();
        if (!(this.listHeight <= 0.0F)) {
            if (this.filteredItems.isEmpty()) {
                new RenderUtil(drawContext, this.listX, this.listY, this.listWidth, this.listHeight)
                    .textCenter(
                        Fonts.INTER_MEDIUM,
                        Language.translate("autobuy.page.empty", "Ничего не найдено"),
                        Theme.withAlpha(Theme.getSecondaryText(), 0.5F * f),
                        6.0F
                    );
            } else {
                int i3 = this.filteredItems.size();
                float f2 = i3 * 22.0F - 2.0F;
                this.itemListScroll.update(f2, this.listHeight);
                this.itemListScroll.updateHover(i, i2);
                if (f2 > this.listHeight) {
                    this.itemListScroll.render(drawContext, this.listX + this.listWidth - 2.5F, this.listY, this.listHeight, f2, f);
                }

                float f3 = this.listWidth - 7.0F;
                boolean flag = ScissorStack.push(drawContext, this.listX - 2.0F, this.listY, this.listWidth + 4.0F, this.listHeight);
                if (flag) {
                    try {
                        this.applyScrollFade(this.itemListScroll, this.listY, this.listHeight, f2);
                        float f4 = this.listY + this.itemListScroll.getScroll();

                        for (int i4 = 0; i4 < i3; i4++) {
                            AutoBuyItem autoBuyItem = this.filteredItems.get(i4);
                            float f5 = f4 + this.staggerAnimation.getOffset(2 + i4);
                            if (f5 + 20.0F >= this.listY && f5 <= this.listY + this.listHeight) {
                                this.renderItemRow(drawContext, autoBuyItem, this.listX, f5, f3, i, i2, f * this.staggerAnimation.getProgress(2 + i4));
                            }

                            this.itemBounds.add(new AutoBuyPage.ItemBounds(autoBuyItem, this.listX, f5, f3, 20.0F));
                            f4 += 22.0F;
                        }
                    } finally {
                        VerticalFadeMask.end();
                        ScissorStack.pop(drawContext);
                    }
                }
            }
        }
    }

    private void renderItemRow(DrawContext drawContext, AutoBuyItem autoBuyItem, float f, float f2, float f3, int i, int i2, float f4) {
        boolean flag = this.isHovered(i, i2, f, f2, f3, 20.0);
        Animation animation = getAnimation(this.itemHoverAnimations, autoBuyItem);
        animation.update();
        animation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        boolean flag2 = autoBuyItem == this.selectedItem;
        Animation animation2 = getAnimation(this.itemSelectAnimations, autoBuyItem);
        animation2.update();
        animation2.animate(flag2 ? 1.0 : 0.0, 0.4, flag2 ? Easings.QUART_OUT : Easings.QUAD_IN);
        float f5 = Math.max(animation2.get(), animation.get() * 0.5F);
        GuiRenderUtil.drawHighlight(drawContext, f - 2.0F, f2, f3 + 4.0F, 20.0F, f4 * f5, Radius.all(6.0F));
        GuiRenderUtil.drawItem(drawContext, autoBuyItem.getDisplayStack(), f + 2.0F, f2 + 10.0F - 8.0F, f4 * VerticalFadeMask.getAlphaFactor(f2 + 10.0F));
        Animation animation3 = getAnimation(this.itemEnabledAnimations, autoBuyItem);
        animation3.update();
        animation3.animate(autoBuyItem.isEnabled() ? 1.0 : 0.0, 0.4, Easings.EXPO_OUT);
        float f6 = (float)Math.min(animation3.get(), 1.0);
        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.55F + 0.45F * f6), f4);
        int i4 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.4F + 0.35F * f6), f4);
        new RenderUtil(drawContext, f, f2, f3, 20.0F)
            .scrollingTextTop(Fonts.INTER_MEDIUM, autoBuyItem.getId(), 21.0F, 3.0F, f3 - 31.0F, i3, 6.5F, flag)
            .textBottomLeft(Fonts.INTER_REGULAR, formatPrice(autoBuyItem.getPrice()), 21.0F, 3.5F, i4, 6.0F);
        if (f6 > 0.01F) {
            new RenderUtil(drawContext, f + f3 - 7.5F, f2 + 10.0F - 1.5F, 3.0F, 3.0F)
                .rect(ColorUtil.multiplyAlpha(Theme.getPrimary(), f4 * f6), Radius.all(1.5F));
        }
    }

    private void renderItemDetails(DrawContext drawContext, int i, int i2, float f, float f2) {
        this.optionBounds.clear();
        if (!(this.contentHeight <= 0.0F)) {
            if (this.selectedItem == null) {
                this.renderNoSelection(drawContext, f2);
            } else {
                float f3 = this.getContentFade();
                float f4 = f2 * f3;
                float f5 = (1.0F - f3) * 6.0F;
                AutoBuyItem autoBuyItem = this.selectedItem;
                this.selectedEnabledAnimation.update();
                this.selectedEnabledAnimation.animate(autoBuyItem.isEnabled() ? 1.0 : 0.0, 0.4, Easings.EXPO_OUT);
                this.sliderDragAnimation.update();
                this.sliderDragAnimation.animate(this.draggingOption != null ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
                float f6 = this.innerWidth;
                float f7 = this.contentY + 4.0F + f5;
                this.toggleX = this.innerX + f6 - 14.0F;
                this.toggleY = f7 + 12.0F - 4.5F;
                GuiRenderUtil.drawItem(drawContext, autoBuyItem.getDisplayStack(), this.innerX, f7 + 12.0F - 8.0F, f4);
                new RenderUtil(drawContext, this.innerX, f7, f6, 24.0F)
                    .scrollingTextMiddle(
                        Fonts.INTER_SEMIBOLD,
                        autoBuyItem.getId(),
                        21.0F,
                        0.0F,
                        this.toggleX - this.innerX - 25.0F,
                        ColorUtil.multiplyAlpha(Theme.getTextColor(), f4),
                        7.0F,
                        this.isHovered(i, i2, this.innerX, f7, f6, 24.0)
                    );
                SettingWidgets.drawDefaultSwitch(drawContext, this.toggleX, this.toggleY, (float)Math.min(this.selectedEnabledAnimation.get(), 1.0), f4);
                GuiRenderUtil.drawOverlay(drawContext, this.innerX, MathUtil.roundToStep(f7 + 24.0F, 0.5F), f6, 0.5F, f4);
                float f8 = this.contentY + 4.0F + 24.0F + 4.0F;
                float f9 = this.contentY + this.contentHeight - 4.0F - f8;
                if (!(f9 <= 0.0F)) {
                    List<EnchantRequirement> enchantRequirements = autoBuyItem.getVariants();
                    float f10 = 20.0F + this.getSettingsHeight(autoBuyItem);
                    if (!enchantRequirements.isEmpty()) {
                        f10 += 12.0F + enchantRequirements.size() * 15.5F;
                    }

                    this.detailsScroll.update(f10, f9);
                    this.detailsScroll.updateHover(i, i2);
                    if (f10 > f9) {
                        this.detailsScroll.render(drawContext, this.contentX + this.contentWidth - 2.5F, f8, f9, f10, f2);
                    }

                    boolean flag = ScissorStack.push(drawContext, this.contentX - 2.0F, f8, this.contentWidth + 4.0F, f9);
                    if (flag) {
                        try {
                            this.applyScrollFade(this.detailsScroll, f8, f9, f10);
                            float f11 = f8 + this.detailsScroll.getScroll() + f5;
                            float f12 = this.renderPriceRow(drawContext, autoBuyItem, this.innerX, f11, f6, i, i2, f4);
                            float f13 = this.renderItemSettings(
                                drawContext, autoBuyItem, this.innerX, f12, f6, i, i2, f3 * this.staggerAnimation.getProgress(2), f
                            );
                            if (!enchantRequirements.isEmpty()) {
                                new RenderUtil(drawContext, this.innerX, f13, f6, 12.0F)
                                    .textBottomLeft(
                                        Fonts.INTER_REGULAR,
                                        Language.translate("autobuy.page.enchants", "Зачарования"),
                                        0.0F,
                                        2.0F,
                                        ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.65F), f4),
                                        6.5F
                                    );
                                float f14 = f13 + 12.0F;

                                for (EnchantRequirement enchantRequirement : enchantRequirements) {
                                    this.renderOptionRow(drawContext, enchantRequirement, this.innerX, f14, f6, i, i2, f4);
                                    this.optionBounds.add(new AutoBuyPage.OptionBounds(enchantRequirement, this.innerX, f14, f6, 14.0F));
                                    f14 += 15.5F;
                                }
                            }
                        } finally {
                            VerticalFadeMask.end();
                            ScissorStack.pop(drawContext);
                        }
                    }
                }
            }
        }
    }

    private void renderNoSelection(DrawContext drawContext, float f) {
        new RenderUtil(drawContext, this.contentX, this.contentY, this.contentWidth, this.contentHeight)
            .icon(
                Icon.CART_SHOPPING,
                this.contentWidth / 2.0F - 9.0F,
                this.contentHeight / 2.0F - 16.0F,
                18.0F,
                Theme.withAlpha(Theme.getSecondaryText(), 0.3F * f)
            )
            .textCenter(
                Fonts.INTER_MEDIUM,
                Language.translate("autobuy.page.select", "Выберите предмет"),
                0.0F,
                8.0F,
                Theme.withAlpha(Theme.getSecondaryText(), 0.5F * f),
                6.5F
            );
    }

    private float renderPriceRow(DrawContext drawContext, AutoBuyItem autoBuyItem, float f, float f2, float f3, int i, int i2, float f4) {
        String string = formatPrice(autoBuyItem.getPrice());
        this.priceRowY = f2;
        this.priceBoxWidth = Fonts.INTER_MEDIUM.getWidth(string, 5.5F) + 8.0F;
        if (this.editingPrice) {
            this.priceBoxWidth = Math.max(this.priceBoxWidth, Fonts.INTER_MEDIUM.getWidth(this.priceField.getText(), 5.5F) + 8.0F);
        }

        this.priceBoxX = f + f3 - this.priceBoxWidth;
        this.priceBoxY = f2 + 10.0F - 5.0F;
        this.priceHoverAnimation.update();
        this.priceHoverAnimation.animate(!this.editingPrice && !this.isHovered(i, i2, f, f2, f3, 20.0) ? 0.0 : 1.0, 0.25, Easings.QUAD_OUT);
        float f5 = (float)Math.min(this.priceHoverAnimation.get(), 1.0);
        GuiRenderUtil.drawHighlight(drawContext, f - 2.0F, f2, f3 + 4.0F, 20.0F, f4 * 0.4F * f5, Radius.all(5.0F));
        new RenderUtil(drawContext, f, f2, f3, 20.0F)
            .scrollingTextMiddle(
                Fonts.INTER_MEDIUM,
                Language.translate("autobuy.page.maxprice", "Макс. цена"),
                0.0F,
                0.0F,
                this.priceBoxX - f - 6.0F,
                ColorUtil.multiplyAlpha(Theme.getTextColor(), f4),
                6.0F
            );
        if (this.editingPrice) {
            GuiRenderUtil.drawSurface(drawContext, this.priceBoxX, this.priceBoxY, this.priceBoxWidth, 10.0F, f4, 3.0F);
            this.priceField
                .render(
                    drawContext,
                    new Vector2f(this.priceBoxX + 4.0F, this.priceBoxY + 5.0F - 2.75F),
                    ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f4),
                    this.priceBoxWidth - 8.0F
                );
            return f2 + 20.0F;
        } else {
            SettingWidgets.drawValueBox(drawContext, this.priceBoxX, this.priceBoxY, this.priceBoxWidth, 10.0F, string, f4);
            return f2 + 20.0F;
        }
    }

    private float renderItemSettings(DrawContext drawContext, AutoBuyItem autoBuyItem, float f, float f2, float f3, int i, int i2, float f4, float f5) {
        for (Setting setting : autoBuyItem.getSettings()) {
            setting.alphaMultiplier = f4;
            setting.alphaAnimation.update();
            setting.alphaAnimation.animate(setting.isVisible() ? 1.0 : 0.0, 0.2F, Easings.EXPO_OUT);
            float f6 = setting.resolveHeight(20.0F);
            setting.position.set(f, f2);
            setting.size.set(f3, f6);
            setting.render(drawContext, i, i2, f5);
            f2 += f6 * setting.alphaAnimation.get();
        }

        return f2;
    }

    private float getSettingsHeight(AutoBuyItem autoBuyItem) {
        float f = 0.0F;

        for (Setting setting : autoBuyItem.getSettings()) {
            f += setting.resolveHeight(20.0F) * setting.alphaAnimation.get();
        }

        return f;
    }

    private void renderOptionRow(DrawContext drawContext, EnchantRequirement enchantRequirement, float f, float f2, float f3, int i, int i2, float f4) {
        boolean flag = this.isHovered(i, i2, f, f2, f3, 14.0);
        boolean flag2 = enchantRequirement.isExcluded();
        Animation animation = getAnimation(this.optionStateAnimations, enchantRequirement);
        animation.update();
        animation.animate(enchantRequirement.getMode() == EnchantRequirement.Mode.ANY ? 0.0 : 1.0, 0.4, Easings.EXPO_OUT);
        Animation animation2 = getAnimation(this.optionHoverAnimations, enchantRequirement);
        animation2.update();
        animation2.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        GuiRenderUtil.drawHighlight(drawContext, f - 2.0F, f2, f3 + 4.0F, 14.0F, f4 * 0.4F * animation2.get(), Radius.all(5.0F));
        float f5 = this.getOptionToggleX(f, f3);
        float f6 = this.getOptionBadgeX(f, f3);
        boolean flag3 = this.hasLevelSlider(enchantRequirement);
        float f7 = flag3 ? this.getOptionSliderX(f, f3) : f6;
        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.45F + 0.4F * animation.get()), f4);
        new RenderUtil(drawContext, f, f2, f3, 14.0F)
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, enchantRequirement.getDisplayName(), 1.0F, 0.0F, f7 - f - 6.0F, i3, 6.0F, flag);
        if (flag3) {
            float f8 = (float)(enchantRequirement.getMinLevel() - 1) / (enchantRequirement.getMaxLevel() - 1);
            float f9 = enchantRequirement == this.draggingOption ? (float)Math.min(this.sliderDragAnimation.get(), 1.0) : 0.0F;
            SettingWidgets.drawSlider(
                drawContext, this.getOptionSliderX(f, f3), f2 + 7.0F - 1.5F, 30.0F, 3.0F, f8, 6.0F, f9, f4 * (0.4F + 0.6F * animation.get())
            );
        }

        float f10 = f2 + 7.0F - 5.0F;
        float f11 = f4 * (0.35F + 0.45F * animation.get());
        GuiRenderUtil.drawSurface(drawContext, f6, f10, 22.0F, 10.0F, f11, 3.0F);
        String string = flag2 ? Language.translate("autobuy.page.without", "нет") : String.valueOf(enchantRequirement.getMinLevel());
        int i4 = flag2 ? Theme.withAlpha(Theme.getPrimary(), f4) : ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f4);
        new RenderUtil(drawContext, f6, f10, 22.0F, 10.0F).textCenter(Fonts.INTER_MEDIUM, string, i4, 6.0F);
        SettingWidgets.drawDefaultSwitch(drawContext, f5, f2 + 7.0F - 4.5F, animation.get(), f4);
    }

    private float getOptionToggleX(float f, float f2) {
        return f + f2 - 14.0F;
    }

    private float getOptionBadgeX(float f, float f2) {
        return this.getOptionToggleX(f, f2) - 6.0F - 22.0F;
    }

    private float getOptionSliderX(float f, float f2) {
        return this.getOptionBadgeX(f, f2) - 6.0F - 30.0F;
    }

    private boolean hasLevelSlider(EnchantRequirement enchantRequirement) {
        return enchantRequirement.getMaxLevel() > 1 && !enchantRequirement.isExcluded();
    }

    private float getContentFade() {
        this.contentFadeAnimation.update();
        return (float)Math.min(this.contentFadeAnimation.get(), 1.0);
    }

    private void restartContentFade() {
        this.contentFadeAnimation.forceValue(0.0);
        this.contentFadeAnimation.animate(1.0, 0.3, Easings.QUAD_OUT);
    }

    private void applyScrollFade(SmoothScrollbar smoothScrollbar, float f, float f2, float f3) {
        float f4 = f3 - f2;
        if (!(f4 <= 0.5F) && !(f2 <= 28.0F)) {
            float f5 = smoothScrollbar.getScroll();
            float f6 = Math.min(Math.max(-f5 / 14.0F, 0.0F), 1.0F);
            float f7 = Math.min(Math.max((f4 + f5) / 14.0F, 0.0F), 1.0F);
            if (!(f6 <= 0.001F) || !(f7 <= 0.001F)) {
                VerticalFadeMask.begin(f, f + 14.0F, f6, f + f2 - 14.0F, f + f2, f7);
            }
        }
    }

    public boolean mouseClicked(Click click, boolean flag) {
        double d = click.x();
        double d2 = click.y();
        if (this.modeWidth > 0.0F && this.isHovered(d, d2, this.modeX, this.modeY, this.modeWidth, 15.0)) {
            this.commitPriceEdit();
            this.searchField.setFocused(false);
            AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
            if (autoBuyManager != null) {
                this.selectModeAt(autoBuyManager, d);
            }
        } else {
            if (this.isHovered(d, d2, this.stashButtonX, this.toolbarY, this.stashButtonWidth, 15.0)) {
                this.commitPriceEdit();
                this.searchField.setFocused(false);
                this.stashOpen = !this.stashOpen;
                if (this.stashOpen) {
                    this.historyOpen = false;
                }

                this.restartContentFade();
                return true;
            }

            if (this.stashOpen && this.handleStashClick(d, d2)) {
                return true;
            }

            if (this.isHovered(d, d2, this.historyButtonX, this.toolbarY, this.historyButtonWidth, 15.0)) {
                this.commitPriceEdit();
                this.searchField.setFocused(false);
                this.historyOpen = !this.historyOpen;
                if (this.historyOpen) {
                    this.stashOpen = false;
                }

                this.historySearchField.setFocused(false);
                this.historyScroll.reset();
                this.restartContentFade();
                return true;
            }

            if (this.isHovered(d, d2, this.searchX, this.searchY, this.searchWidth, 15.0)) {
                this.commitPriceEdit();
                this.searchField.setFocused(true);
                this.searchField.mouseClicked(d, d2, click.button());
                return true;
            }

            this.searchField.setFocused(false);
            if (!this.itemListScroll.mouseClicked(d, d2) && !this.detailsScroll.mouseClicked(d, d2)) {
                if (this.historyScroll.mouseClicked(d, d2)) {
                    return true;
                }

                if (this.isHovered(d, d2, this.listX, this.listY, this.listWidth, this.listHeight)) {
                    int i = this.itemBounds.size();

                    for (int i2 = 0; i2 < i; i2++) {
                        AutoBuyPage.ItemBounds itemBounds2 = this.itemBounds.get(i2);
                        if (this.isHovered(d, d2, itemBounds2.x(), itemBounds2.y(), itemBounds2.w(), itemBounds2.h())) {
                            this.commitPriceEdit();
                            if (this.isRightButton(click.button())) {
                                AutoBuyItem autoBuyItem = itemBounds2.item();
                                if (!itemBounds2.item().isEnabled()) {
                                    autoBuyItem.setEnabled(true);
                                    this.saveConfig();
                                    return true;
                                }

                                autoBuyItem.setEnabled(false);
                                this.saveConfig();
                                return true;
                            }

                            if (this.historyOpen) {
                                this.restartContentFade();
                            }

                            this.historyOpen = false;
                            this.selectItem(itemBounds2.item());
                            return true;
                        }
                    }

                    this.commitPriceEdit();
                    return true;
                }

                if (this.isHovered(d, d2, this.contentX, this.contentY, this.contentWidth, this.contentHeight)) {
                    if (this.stashOpen) {
                        return true;
                    }

                    if (this.historyOpen) {
                        this.handleHistoryClick(click, d, d2);
                        return true;
                    }

                    this.handleDetailsClick(click, flag, d, d2);
                    return true;
                }

                this.commitPriceEdit();
                return false;
            }
        }

        return true;
    }

    private void selectModeAt(AutoBuyManager autoBuyManager, double d) {
        AutoBuyManager.Mode[] modes = this.getAvailableModes();
        int i = (int)((d - this.modeX) / (this.modeWidth / modes.length));
        AutoBuyManager.Mode mode = modes[Math.min(Math.max(i, 0), modes.length - 1)];
        autoBuyManager.setMode(mode);
        if (mode == AutoBuyManager.Mode.OFF) {
            AutoBuyController autoBuyController = AutoBuyController.getInstance();
            if (autoBuyController != null && autoBuyController.isRunning()) {
                autoBuyController.stop(Language.translate("autobuy.stop.manual", "Выключена"));
            }
        }
    }

    private void handleHistoryClick(Click click, double d, double d2) {
        if (this.isHovered(d, d2, this.historySearchX, this.contentY, this.historySearchWidth, 15.0)) {
            this.historySearchField.setFocused(true);
            this.historySearchField.mouseClicked(d, d2, click.button());
        } else {
            this.historySearchField.setFocused(false);
            if (this.isHovered(d, d2, this.filterButtonX, this.contentY, this.filterButtonWidth, 15.0)) {
                this.historyFilter = this.historyFilter.next();
            }
        }
    }

    private void handleDetailsClick(Click click, boolean flag, double d, double d2) {
        if (this.selectedItem != null) {
            if (this.isHovered(d, d2, this.toggleX - 3.0F, this.toggleY - 3.0F, 20.0, 15.0)) {
                this.commitPriceEdit();
                AutoBuyItem autoBuyItem = this.selectedItem;
                if (!this.selectedItem.isEnabled()) {
                    autoBuyItem.setEnabled(true);
                    this.saveConfig();
                } else {
                    autoBuyItem.setEnabled(false);
                    this.saveConfig();
                }
            } else if (this.isHovered(d, d2, this.innerX, this.priceRowY, this.innerWidth, 20.0)) {
                this.startPriceEdit();
            } else {
                for (Setting setting : this.selectedItem.getSettings()) {
                    if (!(setting.alphaAnimation.get() <= 0.001F) && setting.mouseClicked(click, flag)) {
                        this.commitPriceEdit();
                        this.saveConfig();
                        return;
                    }
                }

                int i = this.optionBounds.size();

                for (int i2 = 0; i2 < i; i2++) {
                    AutoBuyPage.OptionBounds optionBounds2 = this.optionBounds.get(i2);
                    if (this.isHovered(d, d2, optionBounds2.x(), optionBounds2.y(), optionBounds2.w(), optionBounds2.h())) {
                        this.handleOptionClick(click, d, d2, optionBounds2);
                        return;
                    }
                }

                this.commitPriceEdit();
            }
        }
    }

    private void handleOptionClick(Click click, double d, double d2, AutoBuyPage.OptionBounds optionBounds2) {
        this.commitPriceEdit();
        EnchantRequirement enchantRequirement = optionBounds2.option();
        boolean flag = this.isRightButton(click.button());
        if (!flag && this.hasLevelSlider(enchantRequirement)) {
            float f = this.getOptionSliderX(optionBounds2.x(), optionBounds2.w());
            if (this.isHovered(d, d2, f - 3.0F, optionBounds2.y(), 36.0, optionBounds2.h())) {
                this.draggingOption = enchantRequirement;
                this.sliderX = f;
                enchantRequirement.setMode(EnchantRequirement.Mode.REQUIRE);
                this.updateSliderDrag(d);
                return;
            }
        }

        if (flag) {
            if (enchantRequirement.isExcluded()) {
                enchantRequirement.setMode(EnchantRequirement.Mode.ANY);
                this.saveConfig();
            } else {
                enchantRequirement.setMode(EnchantRequirement.Mode.EXCLUDE);
                this.saveConfig();
            }
        } else if (enchantRequirement.isRequired()) {
            enchantRequirement.setMode(EnchantRequirement.Mode.ANY);
            this.saveConfig();
        } else {
            enchantRequirement.setMode(EnchantRequirement.Mode.REQUIRE);
            this.saveConfig();
        }
    }

    private void updateSliderDrag(double d) {
        if (this.draggingOption != null) {
            float f = Math.min(Math.max((float)(d - this.sliderX) / 30.0F, 0.0F), 1.0F);
            this.draggingOption.setMinLevel(1 + Math.round(f * (this.draggingOption.getMaxLevel() - 1)));
        }
    }

    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (this.isHovered(d, d2, this.listX, this.listY, this.listWidth, this.listHeight)) {
            this.itemListScroll.onScroll(d4);
            return true;
        }

        if (this.stashOpen) {
            this.stashScroll.onScroll(d4);
            return true;
        }

        if (this.historyOpen) {
            if (!this.isHovered(d, d2, this.contentX, this.contentY, this.contentWidth, this.contentHeight)) {
                return false;
            }

            this.historyScroll.onScroll(d4);
            return true;
        } else if (this.isHovered(d, d2, this.contentX, this.contentY + 24.0F, this.contentWidth, this.contentHeight - 24.0F)) {
            this.detailsScroll.onScroll(d4);
            return true;
        } else {
            return false;
        }
    }

    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.draggingOption != null) {
            this.updateSliderDrag(click.x());
            return true;
        }

        if (this.selectedItem != null) {
            for (Setting setting : this.selectedItem.getSettings()) {
                if (setting.mouseDragged(click, d, d2)) {
                    return true;
                }
            }
        }

        return this.itemListScroll.mouseDragged(click.x(), click.y())
            || this.detailsScroll.mouseDragged(click.x(), click.y())
            || this.historyScroll.mouseDragged(click.x(), click.y());
    }

    public void mouseReleased(Click click) {
        if (this.draggingOption != null) {
            this.draggingOption = null;
            this.saveConfig();
        }

        if (this.selectedItem != null) {
            for (Setting setting : this.selectedItem.getSettings()) {
                if (setting.onMouseRelease(click)) {
                    this.saveConfig();
                }
            }
        }

        this.itemListScroll.mouseReleased();
        this.detailsScroll.mouseReleased();
        this.historyScroll.mouseReleased();
    }

    public boolean keyPressed(KeyInput keyInput) {
        Setting setting = this.getTypingSetting();
        if (setting != null) {
            boolean flag = setting.onKeyPress(keyInput);
            if (!setting.isTyping()) {
                this.saveConfig();
            }

            return flag;
        } else if (this.historySearchField.isFocused()) {
            if (keyInput.key() == 256) {
                this.historySearchField.setFocused(false);
                return true;
            } else {
                this.historySearchField.keyPressed(keyInput.key());
                return true;
            }
        } else if (this.editingPrice) {
            if (keyInput.key() == 257 || keyInput.key() == 335) {
                this.commitPriceEdit();
                return true;
            } else if (keyInput.key() == 256) {
                this.cancelPriceEdit();
                return true;
            } else {
                this.priceField.keyPressed(keyInput.key());
                return true;
            }
        } else if (!this.searchField.isFocused()) {
            return false;
        } else if (keyInput.key() == 256) {
            this.searchField.setFocused(false);
            return true;
        } else {
            this.searchField.keyPressed(keyInput.key());
            return true;
        }
    }

    public boolean charTyped(CharInput charInput) {
        Setting setting = this.getTypingSetting();
        if (setting != null) {
            return setting.onCharType(charInput);
        }

        if (this.historySearchField.isFocused()) {
            this.historySearchField.charTyped(charInput.codepoint());
            return true;
        }

        if (this.editingPrice) {
            this.priceField.charTyped(charInput.codepoint());
            return true;
        }

        if (!this.searchField.isFocused()) {
            return false;
        }

        this.searchField.charTyped(charInput.codepoint());
        return true;
    }

    private void selectItem(AutoBuyItem autoBuyItem) {
        if (this.selectedItem != autoBuyItem) {
            this.commitPriceEdit();
            this.selectedItem = autoBuyItem;
            Animation animation = this.selectedEnabledAnimation;
            if (autoBuyItem.isEnabled()) {
                animation.forceValue(1.0);
                this.detailsScroll.reset();
                this.restartContentFade();
            } else {
                animation.forceValue(0.0);
                this.detailsScroll.reset();
                this.restartContentFade();
            }
        }
    }

    private void startPriceEdit() {
        if (!this.editingPrice && this.selectedItem != null) {
            this.editingPrice = true;
            this.priceField.setText(String.valueOf(this.selectedItem.getPrice()));
            this.priceField.setCursor(this.priceField.getText().length());
            this.priceField.clearSelection();
            this.priceField.setFocused(true);
        }
    }

    private void commitPriceEdit() {
        if (this.editingPrice) {
            try {
                int i = Integer.parseInt(this.priceField.getText().trim());
                if (i > 0 && this.selectedItem != null) {
                    this.selectedItem.setPrice(i);
                    this.saveConfig();
                }
            } catch (NumberFormatException numberFormatException) {
            }

            this.cancelPriceEdit();
        }
    }

    private void cancelPriceEdit() {
        this.editingPrice = false;
        this.priceField.setFocused(false);
        this.priceField.setText("");
    }

    private void refreshFilteredItems() {
        this.filteredItems.clear();
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager != null) {
            String string = this.searchField.getText().trim().toLowerCase(Locale.ROOT);

            for (AutoBuyItem autoBuyItem : autoBuyManager.getItemsForCurrentServer()) {
                if (string.isEmpty() || autoBuyItem.getId().toLowerCase(Locale.ROOT).contains(string)) {
                    this.filteredItems.add(autoBuyItem);
                }
            }

            this.filteredItems.sort(Comparator.comparingInt(autoBuyItem2 -> autoBuyItem2.getSource().ordinal()));
        }
    }

    private boolean handleStashClick(double d, double d2) {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager == null) {
            return false;
        }

        int i = ServerManager.getAnarchyNumber();
        if (i > 0 && this.isHovered(d, d2, this.stashHereX, this.stashRowY, this.stashHereWidth, 15.0)) {
            autoBuyManager.setStashAnarchy(i);
            this.saveConfig();
            return true;
        }

        if (this.isHovered(d, d2, this.stashRunX, this.stashRowY, this.stashRunWidth, 15.0)) {
            ShulkerStash shulkerStash = ShulkerStash.getInstance();
            if (shulkerStash != null) {
                shulkerStash.start();
            }

            return true;
        } else {
            int i2 = this.anarchyBounds.size();

            for (int i3 = 0; i3 < i2; i3++) {
                AutoBuyPage.AnarchyBounds anarchyBounds2 = this.anarchyBounds.get(i3);
                if (this.isHovered(d, d2, anarchyBounds2.x(), anarchyBounds2.y(), anarchyBounds2.w(), anarchyBounds2.h())) {
                    if (autoBuyManager.getStashAnarchy() == anarchyBounds2.anarchy()) {
                        autoBuyManager.setStashAnarchy(0);
                        this.saveConfig();
                        return true;
                    }

                    autoBuyManager.setStashAnarchy(anarchyBounds2.anarchy());
                    this.saveConfig();
                    return true;
                }
            }

            return false;
        }
    }

    private void saveConfig() {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager != null) {
            autoBuyManager.save();
        }
    }

    private static <K> Animation getAnimation(Map<K, Animation> map, K value) {
        return map.computeIfAbsent(value, value2 -> new Animation());
    }

    private static String formatPrice(int i) {
        return String.format(Locale.US, "%,d", i).replace(',', ' ');
    }

    private float getPageAlpha() {
        return (float)Math.pow(this.screen.getOpenAnimation().get(), 3.0);
    }

    record AnarchyBounds(int anarchy, float x, float y, float w, float h) {
    }

    enum HistoryFilter {
        ALL(Icon.FILTER, "autobuy.page.filter.all", "Все"),
        BOUGHT(Icon.CIRCLE_CHECK, "autobuy.page.filter.bought", "Куплено"),
        MISSED(Icon.CIRCLE_XMARK, "autobuy.page.filter.missed", "Мимо"),
        PRICE(Icon.SORT_DESC, "autobuy.page.filter.price", "Цена");

        private final Icon icon;
        private final String translationKey;
        private final String fallback;

        AutoBuyPage.HistoryFilter next() {
            AutoBuyPage.HistoryFilter[] historyFilters = values();
            return historyFilters[(this.ordinal() + 1) % historyFilters.length];
        }

        String getLabel() {
            return Language.translate(this.translationKey, this.fallback);
        }

        boolean accepts(PurchaseHistory.Result result) {
            switch (this) {
                case BOUGHT:
                    if (result == PurchaseHistory.Result.BOUGHT) {
                        return true;
                    }
                    break;
                case MISSED:
                    if (result != PurchaseHistory.Result.BOUGHT) {
                        return true;
                    }
                    break;
                default:
                    return true;
            }

            return false;
        }

        public Icon getIcon() {
            return this.icon;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }

        public String getFallback() {
            return this.fallback;
        }

        HistoryFilter(Icon icon2, String string, String string2) {
            this.icon = icon2;
            this.translationKey = string;
            this.fallback = string2;
        }
    }

    record ItemBounds(AutoBuyItem item, float x, float y, float w, float h) {
    }

    record OptionBounds(EnchantRequirement option, float x, float y, float w, float h) {
    }
}
