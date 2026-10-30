package tech.sadnes.client.ui.clickgui.page;

import java.util.ArrayList;
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
import net.minecraft.item.Items;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.animation.StaggeredAppearAnimation;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.VerticalFadeMask;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.server.AutoBuyItem;
import tech.sadnes.client.server.AutoBuyManager;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.ServerType;
import tech.sadnes.client.server.autobuy.CollectorManager;
import tech.sadnes.client.server.autobuy.EnchantRequirement;
import tech.sadnes.client.server.autobuy.KitCollector;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.ui.clickgui.component.TextFieldComponent;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.math.StopWatch;

public final class CollectorPage implements IRenderable, IMinecraft {
    private static final float ITEM_LIST_WIDTH = 104.0F;
    private static final float HEADER_HEIGHT = 15.0F;
    private static final float ITEM_ROW_HEIGHT = 18.0F;
    private static final float ITEM_ROW_GAP = 2.0F;
    private static final float PRESETS_GAP = 6.0F;
    private static final float SLOT_GAP = 2.0F;
    private static final float CONTENT_TOP_GAP = 4.0F;
    private static final float SECTION_GAP = 4.0F;
    private static final float PRESET_ROW_HEIGHT = 18.0F;
    private static final float PRESET_ROW_GAP = 3.0F;
    private static final float PRESETS_HEADER_HEIGHT = 12.0F;
    private static final float POPUP_WIDTH = 112.0F;
    private static final float POPUP_PADDING = 4.5F;
    private static final float POPUP_INNER_PADDING = 8.0F;
    private static final float POPUP_ROW_HEIGHT = 14.0F;
    private static final float COUNT_SLIDER_WIDTH = 34.0F;
    private static final float COUNT_FIELD_WIDTH = 20.0F;
    private static final float FIELD_HEIGHT = 10.0F;
    private static final float PRICE_FIELD_WIDTH = 52.0F;
    private static final float MIN_SLOT_SIZE = 14.0F;
    private static final float SLOT_COLUMNS = 9.0F;
    private static final long DOUBLE_CLICK_MS = 350L;
    private static final int DANGER_COLOR = ColorUtil.rgb(255, 94, 94);
    private static final float HINTS_WIDTH = 152.0F;
    private static final float HINT_LINE_HEIGHT = 11.0F;
    private static final String[][] HINTS = new String[][]{
        {"collector.hint.take", "ЛКМ по предмету — взять в руку"},
        {"collector.hint.place", "ЛКМ по слоту — положить или перенести"},
        {"collector.hint.clone", "Клик колесом — копия предмета"},
        {"collector.hint.count", "Прокрутка над слотом — количество"},
        {"collector.hint.settings", "ПКМ по слоту — настройки предмета"},
        {"collector.hint.remove", "Двойной ПКМ или перенос в список — убрать"},
        {"collector.hint.clear", "Корзина дважды — очистить раскладку"}
    };
    private final ClickGuiScreen parentScreen;
    private final SmoothScrollbar itemListScroll = new SmoothScrollbar();
    private final SmoothScrollbar presetScroll = new SmoothScrollbar();
    private final SmoothScrollbar popupScroll = new SmoothScrollbar();
    private final TextFieldComponent searchField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "autobuy.page.search", false, false);
    private final TextFieldComponent priceField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "autobuy.page.price", false, true);
    private final TextFieldComponent countField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "collector.page.count", false, true);
    private final TextFieldComponent presetNameField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "collector.preset.name", false, false);
    private final Map<AutoBuyItem, Animation> itemHoverAnimations = new IdentityHashMap<>();
    private final Map<Integer, Animation> slotHoverAnimations = new HashMap<>();
    private final Map<String, Animation> presetHoverAnimations = new HashMap<>();
    private final Map<String, Animation> presetDeleteHoverAnimations = new HashMap<>();
    private final StaggeredAppearAnimation appearAnimator = new StaggeredAppearAnimation();
    private final Animation savePresetHoverAnimation = new Animation();
    private final Animation addPresetHoverAnimation = new Animation();
    private final Animation searchHoverAnimation = new Animation();
    private final Animation dropZoneAnimation = new Animation();
    private final Animation dropZoneHoverAnimation = new Animation();
    private final Animation serverButtonHoverAnimation = new Animation();
    private final Animation serverSwitchAnimation = new Animation();
    private final Animation clearHoverAnimation = new Animation();
    private final Animation toggleHoverAnimation = new Animation();
    private final Animation infoHoverAnimation = new Animation();
    private final Animation hintsAnimation = new Animation();
    private final StopWatch clearConfirmTimer = new StopWatch();
    private boolean clearPending;
    private final Animation removeHoverAnimation = new Animation();
    private final Animation draggedItemAnimation = new Animation();
    private final Animation countSliderAnimation = new Animation();
    private final Animation slotPopupAnimation = new Animation();
    private final List<CollectorPage.ItemRow> itemRows = new ArrayList<>();
    private final List<CollectorPage.PresetRow> presetRows = new ArrayList<>();
    private AutoBuyItem draggedItem;
    private int draggedCount;
    private int dragSourceSlot = -1;
    private List<CollectorManager.BundleEntry> draggedContents = List.of();
    private int dragSourceContainerSlot = -1;
    private float dragStartX;
    private float dragStartY;
    private final List<CollectorPage.FlyingItem> flyingItems = new ArrayList<>();
    private final List<CollectorPage.ContentCell> contentCells = new ArrayList<>();
    private final List<CollectorPage.EnchantRow> enchantRows = new ArrayList<>();
    private final Map<EnchantRequirement, Animation> enchantAnimations = new IdentityHashMap<>();
    private final Animation[] contentHoverAnimations = new Animation[4];
    private int selectedSlot = -1;
    private int popupSlot = -1;
    private CollectorManager.BundleEntry selectedContent;
    private boolean editingPrice;
    private boolean editingCount;
    private boolean draggingCountSlider;
    private boolean creatingPreset;
    private boolean overwritePreset;
    private boolean serverSwitchFlipped;
    private boolean toggleLocked;
    private String previousServerName;
    private String lastSearchQuery = "";
    private final StopWatch rightClickTimer = new StopWatch();
    private int lastRightClickedSlot = -1;
    private CollectorManager.BundleEntry lastRightClickedContent;
    private float panelX;
    private float panelY;
    private float panelWidth;
    private float panelHeight;
    private float itemListX;
    private float itemListY;
    private float itemListHeight;
    private float gridX;
    private float gridY;
    private float slotSize;
    private float presetsX;
    private float presetsWidth;
    private float presetsHeaderY;
    private float presetsListY;
    private float presetsListHeight;
    private float addPresetY;
    private float addPresetButtonX;
    private float serverButtonX;
    private float serverButtonWidth;
    private float clearButtonX;
    private float toggleButtonX;
    private float infoButtonX;
    private float popupX;
    private float popupY;
    private float popupHeight;
    private float countSliderX;
    private float countSliderY;
    private float countFieldX;
    private float countFieldY;
    private float priceFieldX;
    private float priceFieldY;
    private float priceRowY;
    private float discountRowY = -1.0F;
    private float removeRowY;

    public CollectorPage(ClickGuiScreen clickGuiScreen) {
        this.parentScreen = clickGuiScreen;

        for (int i = 0; i < 4; i++) {
            this.contentHoverAnimations[i] = new Animation();
        }
    }

    public void onOpen() {
        this.appearAnimator.reset();
        this.commitFieldEdits();
        this.cancelPresetCreation();
        this.searchField.setFocused(false);
        CollectorManager collectorManager = this.getCollector();
        if (collectorManager != null) {
            this.cancelDrag(collectorManager);
        }

        this.draggedItem = null;
        this.dragSourceSlot = -1;
        this.selectedSlot = -1;
        this.popupSlot = -1;
        this.selectedContent = null;
        this.slotPopupAnimation.forceValue(0.0);
        this.draggingCountSlider = false;
    }

    public void render(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        CollectorManager collectorManager = this.getCollector();
        if (collectorManager != null) {
            this.appearAnimator.update();
            float f6 = this.getOpenProgress();
            this.panelX = f2;
            this.panelY = f3;
            this.panelWidth = f4;
            this.panelHeight = f5;
            if (this.selectedSlot >= 0 && collectorManager.getSlot(this.selectedSlot).isEmpty()) {
                this.closeSlotPopup();
            }

            float f7 = f4 - 104.0F - 12.0F;
            this.itemListX = f2 + f4 - 104.0F;
            this.itemListY = f3 + 15.0F + 4.0F;
            this.itemListHeight = f3 + f5 - this.itemListY;
            this.slotSize = Math.clamp((f7 - 16.0F) / 9.0F, 14.0F, 20.0F);
            float f8 = 9.0F * this.slotSize + 16.0F;
            this.gridX = f2 + (f7 - f8) / 2.0F;
            this.gridY = f3 + 15.0F + 4.0F;
            float f9 = this.getSlotY(27) + this.slotSize;
            this.presetsX = this.gridX;
            this.presetsWidth = f8;
            this.presetsHeaderY = f9 + 6.0F;
            this.presetsListY = this.presetsHeaderY + 12.0F;
            this.presetsListHeight = f3 + f5 - this.presetsListY;
            GuiRenderUtil.drawOverlay(drawContext, MathUtil.roundToStep(f2 + f7 + 6.0F, 0.5F), f3, 0.5F, f5, f6 * this.appearAnimator.getProgress(0));
            this.renderSearchField(drawContext, i, i2, f6 * this.appearAnimator.getProgress(0));
            this.renderServerSelector(drawContext, collectorManager, i, i2, f6 * this.appearAnimator.getProgress(0));
            this.renderItemList(drawContext, collectorManager, i, i2, f6);
            this.renderDropZone(drawContext, i, i2, f6);
            this.renderSlots(drawContext, collectorManager, i, i2, f6);
            this.renderPresets(drawContext, collectorManager, i, i2, f6);
            this.renderSlotPopup(drawContext, collectorManager, i, i2, f6);
            this.renderHints(drawContext, f6);
            this.renderFlyingItems(drawContext, f6);
            if (this.draggedItem != null) {
                this.draggedItemAnimation.update();
                this.draggedItemAnimation.animate(1.0, 0.25, Easings.QUAD_OUT);
                GuiRenderUtil.drawItem(
                    drawContext, this.draggedItem.getDisplayStack(), i - 8.0F, i2 - 8.0F, f6 * 0.85F * (float)Math.min(this.draggedItemAnimation.get(), 1.0)
                );
            } else {
                this.draggedItemAnimation.forceValue(0.0);
            }
        }
    }

    private void renderSearchField(DrawContext drawContext, int i, int i2, float f) {
        this.searchHoverAnimation.update();
        boolean flag;
        if (!this.isHovered(i, i2, this.itemListX, this.panelY, 104.0, 15.0)) {
            flag = this.searchField.isFocused();
        } else {
            flag = true;
        }

        this.searchHoverAnimation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        GuiRenderUtil.drawOutlined(drawContext, this.itemListX, this.panelY, 104.0F, 15.0F, f * (0.7F + 0.2F * this.searchHoverAnimation.get()), 5.0F)
            .icon(Icon.SEARCH, 3.5F, 3.5F, 8.0F, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f));
        this.searchField
            .render(drawContext, new Vector2f(this.itemListX + 14.0F, this.panelY + 7.5F - 3.0F), ColorUtil.multiplyAlpha(Theme.getTextColor(), f), 87.0F);
        if (!this.searchField.getText().equals(this.lastSearchQuery)) {
            this.lastSearchQuery = this.searchField.getText();
            this.itemListScroll.reset();
        }
    }

    private void renderServerSelector(DrawContext drawContext, CollectorManager collectorManager, int i, int i2, float f) {
        this.renderToolbarButtons(drawContext, i, i2, f);
        this.serverButtonX = this.gridX;
        this.serverButtonWidth = this.infoButtonX - this.serverButtonX - 4.0F;
        this.serverButtonHoverAnimation.update();
        this.serverButtonHoverAnimation
            .animate(this.isHovered(i, i2, this.serverButtonX, this.panelY, this.serverButtonWidth, 15.0) ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        float f2 = (float)Math.min(this.serverButtonHoverAnimation.get(), 1.0);
        this.serverSwitchAnimation.update();
        float f3 = (float)Math.clamp(this.serverSwitchAnimation.get(), 0.0, 1.0);
        float f4 = this.serverSwitchFlipped ? f3 : 1.0F - f3;
        GuiRenderUtil.drawOutlined(drawContext, this.serverButtonX, this.panelY, this.serverButtonWidth, 15.0F, f * (0.7F + 0.2F * f2), 5.0F)
            .icon(Icon.SERVER, 4.0F, 3.5F, 8.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.7F + 0.3F * f2), f));
        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.7F + 0.3F * f2), f);
        float f5 = this.serverButtonX + 15.0F;
        float f6 = this.serverButtonWidth - 28.0F;
        String string = getServerDisplayName(collectorManager.getServer());
        if (ScissorStack.push(drawContext, this.serverButtonX, this.panelY, this.serverButtonWidth, 15.0F)) {
            try {
                if (f4 < 0.999F && this.previousServerName != null) {
                    this.drawServerName(drawContext, this.previousServerName, f5, this.panelY - 15.0F * f4, f6, ColorUtil.multiplyAlpha(i3, 1.0F - f4));
                    this.drawServerName(drawContext, string, f5, this.panelY + 15.0F * (1.0F - f4), f6, ColorUtil.multiplyAlpha(i3, f4));
                } else {
                    this.drawServerName(drawContext, string, f5, this.panelY, f6, i3);
                }
            } finally {
                ScissorStack.pop(drawContext);
            }
        }

        RenderUtil.drawChevron(
            drawContext,
            this.serverButtonX + this.serverButtonWidth - 11.0F,
            this.panelY + 7.5F - 4.5F,
            9.0F,
            90.0F + 180.0F * f3,
            ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f)
        );
    }

    private void drawServerName(DrawContext drawContext, String string, float f, float f2, float f3, int i) {
        new RenderUtil(drawContext, f, f2, f3, 15.0F).textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, i, 6.0F);
    }

    private void cycleServer(CollectorManager collectorManager) {
        List list = CollectorManager.SUPPORTED_SERVERS;
        ServerType serverType = (ServerType)list.get((list.indexOf(collectorManager.getServer()) + 1) % list.size());
        KitCollector kitCollector = KitCollector.getInstance();
        if (kitCollector != null) {
            kitCollector.stop(null);
        }

        this.previousServerName = getServerDisplayName(collectorManager.getServer());
        collectorManager.setServer(serverType);
        this.serverSwitchFlipped = !this.serverSwitchFlipped;
        if (this.serverSwitchFlipped) {
            this.serverSwitchAnimation.animate(1.0, 0.28, Easings.QUART_OUT);
        } else {
            this.serverSwitchAnimation.animate(0.0, 0.28, Easings.QUART_OUT);
        }
    }

    private void renderToolbarButtons(DrawContext drawContext, int i, int i2, float f) {
        this.clearButtonX = this.gridX + 9.0F * this.slotSize + 16.0F - 15.0F;
        if (this.clearPending && this.clearConfirmTimer.hasPassed(2500.0)) {
            this.clearPending = false;
        }

        this.clearHoverAnimation.update();
        this.clearHoverAnimation.animate(this.isHovered(i, i2, this.clearButtonX, this.panelY, 15.0, 15.0) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f2 = (float)Math.min(this.clearHoverAnimation.get(), 1.0);
        int i3 = ColorUtil.multiplyAlpha(this.clearPending ? DANGER_COLOR : Theme.withAlpha(Theme.getSecondaryText(), 0.55F + 0.4F * f2), f);
        GuiRenderUtil.drawOutlined(drawContext, this.clearButtonX, this.panelY, 15.0F, 15.0F, f * (0.7F + 0.2F * f2), 5.0F)
            .icon(Icon.TRASH, 3.5F, 3.5F, 8.0F, i3);
        this.toggleButtonX = this.clearButtonX - 15.0F - 4.0F;
        KitCollector kitCollector = KitCollector.getInstance();
        boolean flag;
        if (kitCollector != null) {
            flag = kitCollector.isRunning();
        } else {
            flag = false;
        }

        this.toggleLocked = !flag && ServerManager.isInPvp();
        this.toggleHoverAnimation.update();
        this.toggleHoverAnimation
            .animate(this.toggleLocked || !this.isHovered(i, i2, this.toggleButtonX, this.panelY, 15.0, 15.0) && !flag ? 0.0 : 1.0, 0.2, Easings.QUAD_OUT);
        float f3 = (float)Math.min(this.toggleHoverAnimation.get(), 1.0);
        int i4 = ColorUtil.multiplyAlpha(
            flag ? Theme.getPrimary() : Theme.withAlpha(Theme.getSecondaryText(), this.toggleLocked ? 0.25F : 0.55F + 0.4F * f3), f
        );
        GuiRenderUtil.drawOutlined(drawContext, this.toggleButtonX, this.panelY, 15.0F, 15.0F, f * (0.7F + 0.2F * f3), 5.0F)
            .icon(flag ? Icon.PAUSE : Icon.PLAY, 3.5F, 3.5F, 8.0F, i4);
        this.infoButtonX = this.toggleButtonX - 15.0F - 4.0F;
        this.infoHoverAnimation.update();
        this.infoHoverAnimation.animate(this.isHovered(i, i2, this.infoButtonX, this.panelY, 15.0, 15.0) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f4 = (float)Math.min(this.infoHoverAnimation.get(), 1.0);
        GuiRenderUtil.drawOutlined(drawContext, this.infoButtonX, this.panelY, 15.0F, 15.0F, f * (0.7F + 0.2F * f4), 5.0F)
            .icon(Icon.INFO_CIRCLE, 3.5F, 3.5F, 8.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.55F + 0.4F * f4), f));
    }

    private void renderHints(DrawContext drawContext, float f) {
        this.hintsAnimation.update();
        this.hintsAnimation.animate(this.infoHoverAnimation.get() > 0.5F ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        float f2 = (float)Math.min(this.hintsAnimation.get(), 1.0);
        if (!(f2 <= 0.001F)) {
            float f3 = 9.0F + HINTS.length * 11.0F;
            float f4 = Math.max(this.panelX, this.clearButtonX + 15.0F - 152.0F);
            float f5 = this.panelY + 15.0F + 3.0F;
            float f6 = f * f2;
            MatrixUtil.push(drawContext);
            MatrixUtil.translate(drawContext, 0.0F, (1.0F - f2) * 6.0F);
            GuiRenderUtil.drawPopup(drawContext, f4, f5, 152.0F, f3, f6, 8.0F);
            float f7 = f5 + 4.5F;
            int i = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.8F), f6);

            for (String[] strings : HINTS) {
                new RenderUtil(drawContext, f4 + 8.0F, f7, 136.0F, 11.0F)
                    .textMiddleLeft(Fonts.INTER_MEDIUM, Language.translate(strings[0], strings[1]), 0.0F, 0.0F, i, 5.5F);
                f7 += 11.0F;
            }

            MatrixUtil.pop(drawContext);
        }
    }

    private static String getServerDisplayName(ServerType serverType) {
        switch (serverType) {
            case SPOOKYTIME:
                return "SpookyTime";
            case HOLYWORLD:
                return "HolyWorld";
            default:
                return "FunTime";
        }
    }

    private void renderItemList(DrawContext drawContext, CollectorManager collectorManager, int i, int i2, float f) {
        this.itemRows.clear();
        if (!(this.itemListHeight <= 0.0F)) {
            List list = this.getFilteredItems(collectorManager);
            if (list.isEmpty()) {
                new RenderUtil(drawContext, this.itemListX, this.itemListY, 104.0F, this.itemListHeight)
                    .textCenter(
                        Fonts.INTER_MEDIUM,
                        Language.translate("autobuy.page.empty", "Ничего не найдено"),
                        Theme.withAlpha(Theme.getSecondaryText(), 0.5F * f),
                        6.0F
                    );
            } else {
                int i3 = list.size();
                float f2 = i3 * 20.0F - 2.0F;
                this.itemListScroll.update(f2, this.itemListHeight);
                this.itemListScroll.updateHover(i, i2);
                if (f2 > this.itemListHeight) {
                    this.itemListScroll.render(drawContext, this.itemListX + 104.0F - 2.5F, this.itemListY, this.itemListHeight, f2, f);
                }

                if (ScissorStack.push(drawContext, this.itemListX - 2.0F, this.itemListY, 108.0F, this.itemListHeight)) {
                    try {
                        this.applyScrollFade(this.itemListScroll, this.itemListY, this.itemListHeight, f2);
                        float f3 = this.itemListY + this.itemListScroll.getScroll();

                        for (int i4 = 0; i4 < i3; i4++) {
                            AutoBuyItem autoBuyItem = (AutoBuyItem)list.get(i4);
                            float f4 = f3 + this.appearAnimator.getOffset(1 + i4);
                            if (f4 + 18.0F >= this.itemListY && f4 <= this.itemListY + this.itemListHeight) {
                                this.renderItemRow(drawContext, autoBuyItem, f4, 95.0F, i, i2, f * this.appearAnimator.getProgress(1 + i4));
                            }

                            this.itemRows.add(new CollectorPage.ItemRow(autoBuyItem, f4));
                            f3 += 20.0F;
                        }
                    } finally {
                        VerticalFadeMask.end();
                        ScissorStack.pop(drawContext);
                    }
                }
            }
        }
    }

    private void renderDropZone(DrawContext drawContext, int i, int i2, float f) {
        this.dropZoneAnimation.update();
        this.dropZoneAnimation
            .animate(this.draggedItem == null || this.dragSourceSlot < 0 && this.dragSourceContainerSlot < 0 ? 0.0 : 1.0, 0.2, Easings.QUAD_OUT);
        float f2 = (float)Math.min(this.dropZoneAnimation.get(), 1.0);
        if (!(f2 <= 0.001F)) {
            float f3 = f * f2;
            GuiRenderUtil.drawPopup(drawContext, this.itemListX - 3.0F, this.panelY, 110.0F, this.panelHeight, f3, 8.0F);
            boolean flag = this.isHovered(i, i2, this.itemListX - 3.0F, this.panelY, 110.0, this.panelHeight);
            this.dropZoneHoverAnimation.update();
            this.dropZoneHoverAnimation.animate(flag ? 1.0 : 0.0, 0.3, flag ? Easings.BACK_OUT : Easings.QUAD_OUT);
            float f4 = (float)Math.max(this.dropZoneHoverAnimation.get(), 0.0);
            int i3 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.withAlpha(Theme.getSecondaryText(), 0.6F), DANGER_COLOR, Math.min(f4, 1.0F)), f3);
            float f5 = 22.0F + 6.0F * f4;
            float f6 = this.panelY + this.panelHeight / 2.0F;
            new RenderUtil(drawContext, this.itemListX, f6 - 12.0F - f5 / 2.0F, 104.0F, f5).icon(Icon.TRASH, 52.0F - f5 / 2.0F, 0.0F, f5, i3);
            new RenderUtil(drawContext, this.itemListX, f6 + 6.0F, 104.0F, 10.0F)
                .textCenter(Fonts.INTER_MEDIUM, Language.translate("collector.page.drop.remove", "Отпустите, чтобы убрать"), i3, 6.0F);
        }
    }

    private List<AutoBuyItem> getFilteredItems(CollectorManager collectorManager) {
        List<AutoBuyItem> autoBuyItems = collectorManager.getAvailableItems();
        String string = this.searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (string.isEmpty()) {
            return autoBuyItems;
        }

        ArrayList arrayList = new ArrayList();

        for (AutoBuyItem autoBuyItem : autoBuyItems) {
            if (autoBuyItem.getId().toLowerCase(Locale.ROOT).contains(string)) {
                arrayList.add(autoBuyItem);
            }
        }

        return arrayList;
    }

    private void renderItemRow(DrawContext drawContext, AutoBuyItem autoBuyItem, float f, float f2, int i, int i2, float f3) {
        boolean flag = this.isHovered(i, i2, this.itemListX, f, f2, 18.0);
        Animation animation = this.itemHoverAnimations.computeIfAbsent(autoBuyItem, autoBuyItem2 -> new Animation());
        animation.update();
        animation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        float f4 = Math.max(autoBuyItem == this.draggedItem && this.dragSourceSlot < 0 ? 1.0F : 0.0F, animation.get() * 0.5F);
        GuiRenderUtil.drawHighlight(drawContext, this.itemListX - 2.0F, f, f2 + 4.0F, 18.0F, f3 * f4, Radius.all(6.0F));
        GuiRenderUtil.drawItem(drawContext, autoBuyItem.getDisplayStack(), this.itemListX + 2.0F, f + 9.0F - 8.0F, f3 * VerticalFadeMask.getAlphaFactor(f + 9.0F));
        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), autoBuyItem == this.draggedItem && this.dragSourceSlot < 0 ? 1.0F : 0.7F), f3);
        new RenderUtil(drawContext, this.itemListX, f, f2, 18.0F)
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, autoBuyItem.getId(), 21.0F, 0.0F, f2 - 23.0F, i3, 6.5F, flag);
    }

    private void renderSlots(DrawContext drawContext, CollectorManager collectorManager, int i, int i2, float f) {
        for (int i3 = 0; i3 < 41; i3++) {
            float f2 = this.getSlotX(i3);
            float f3 = this.getSlotY(i3);
            float f4 = f * this.appearAnimator.getProgress(i3 >= 36 ? 1 : i3 / 9 + 2);
            CollectorManager.CollectorSlot collectorSlot = collectorManager.getSlot(i3);
            boolean flag = this.isHovered(i, i2, f2, f3, this.slotSize, this.slotSize);
            Animation animation = this.slotHoverAnimations.computeIfAbsent(i3, integer -> new Animation());
            animation.update();
            animation.animate(!flag && i3 != this.selectedSlot ? 0.0 : 1.0, 0.2, Easings.QUAD_OUT);
            float f5 = (float)Math.min(animation.get(), 1.0);
            GuiRenderUtil.drawOutlined(drawContext, f2, f3, this.slotSize, this.slotSize, f4 * (0.5F + 0.25F * f5), 4.0F);
            float f6 = f2 + this.slotSize / 2.0F - 8.0F;
            float f7 = f3 + this.slotSize / 2.0F - 8.0F;
            if (!collectorSlot.isEmpty()) {
                float f8 = f4 * this.getSlotItemAlpha(i3);
                GuiRenderUtil.drawItem(drawContext, collectorSlot.getItem().getDisplayStack(), f6, f7, f8);
                if (collectorSlot.getCount() > 1) {
                    String string = String.valueOf(collectorSlot.getCount());
                    drawContext.drawText(
                        mc.textRenderer,
                        string,
                        (int)f6 + 17 - mc.textRenderer.getWidth(string),
                        (int)f7 + 9,
                        ColorUtil.setAlphaf(ColorUtil.WHITE, f8),
                        true
                    );
                }
            } else if (this.draggedItem != null && flag && collectorManager.canPlace(i3, this.draggedItem)) {
                GuiRenderUtil.drawItem(drawContext, this.draggedItem.getDisplayStack(), f6, f7, f4 * 0.35F);
            } else {
                new RenderUtil(drawContext, f2, f3, this.slotSize, this.slotSize)
                    .icon(
                        Icon.PLUS,
                        this.slotSize / 2.0F - 3.5F,
                        this.slotSize / 2.0F - 3.5F,
                        7.0F,
                        Theme.withAlpha(Theme.getSecondaryText(), (0.18F + 0.2F * f5) * f4)
                    );
            }
        }
    }

    private float getSlotItemAlpha(int i) {
        int i2 = this.flyingItems.size();

        for (int i3 = 0; i3 < i2; i3++) {
            CollectorPage.FlyingItem flyingItem = this.flyingItems.get(i3);
            if (flyingItem.targetSlot == i) {
                float f = (float)Math.min(flyingItem.progressAnimation.get(), 1.0);
                return f * f;
            }
        }

        return 1.0F;
    }

    private void renderFlyingItems(DrawContext drawContext, float f) {
        Iterator iterator2 = this.flyingItems.iterator();

        while (iterator2.hasNext()) {
            CollectorPage.FlyingItem flyingItem = (CollectorPage.FlyingItem)iterator2.next();
            flyingItem.progressAnimation.update();
            float f2 = (float)Math.min(flyingItem.progressAnimation.get(), 1.0);
            if (flyingItem.progressAnimation.isFinished() && f2 >= 0.999F) {
                iterator2.remove();
            } else {
                float f3 = flyingItem.startX + (flyingItem.endX - flyingItem.startX) * f2;
                float f4 = flyingItem.startY + (flyingItem.endY - flyingItem.startY) * f2;
                GuiRenderUtil.drawItem(drawContext, flyingItem.item.getDisplayStack(), f3 - 8.0F, f4 - 8.0F, f * (flyingItem.targetSlot >= 0 ? 1.0F : 1.0F - f2));
            }
        }
    }

    private void renderPresets(DrawContext drawContext, CollectorManager collectorManager, int i, int i2, float f) {
        this.presetRows.clear();
        if (!(this.presetsListHeight <= 18.0F)) {
            float f2 = f * this.appearAnimator.getProgress(6);
            int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.65F), f2);
            RenderUtil renderUtil = new RenderUtil(drawContext, this.presetsX + 2.0F, this.presetsHeaderY, this.presetsWidth - 4.0F, 10.0F);
            renderUtil.textMiddleLeft(Fonts.INTER_SEMIBOLD, Language.translate("collector.page.presets", "Пресеты"), 0.0F, 0.0F, i3, 5.5F);
            long l = collectorManager.getTotalCost();
            if (l > 0L) {
                renderUtil.textMiddleRight(
                    Fonts.INTER_MEDIUM,
                    Language.translate("collector.page.total", "Итого:") + " " + formatNumber(l),
                    0.0F,
                    0.0F,
                    ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.7F), f2),
                    6.0F
                );
            }

            List list = collectorManager.getPresets();
            int i4 = list.size() + 1;
            float f3 = i4 * 21.0F - 3.0F;
            this.presetScroll.update(f3, this.presetsListHeight);
            this.presetScroll.updateHover(i, i2);
            if (f3 > this.presetsListHeight) {
                this.presetScroll.render(drawContext, this.presetsX + this.presetsWidth + 2.0F, this.presetsListY, this.presetsListHeight, f3, f2);
            }

            if (ScissorStack.push(drawContext, this.presetsX - 2.0F, this.presetsListY, this.presetsWidth + 4.0F, this.presetsListHeight)) {
                try {
                    this.applyScrollFade(this.presetScroll, this.presetsListY, this.presetsListHeight, f3);
                    float f4 = this.presetsListY + this.presetScroll.getScroll();
                    String string = collectorManager.getActivePresetName();
                    int i5 = list.size();

                    for (int i6 = 0; i6 < i5; i6++) {
                        CollectorManager.Preset preset = (CollectorManager.Preset)list.get(i6);
                        Animation animation = this.presetHoverAnimations.computeIfAbsent(preset.name(), string2 -> new Animation());
                        Animation animation2 = this.presetDeleteHoverAnimations.computeIfAbsent(preset.name(), string3 -> new Animation());
                        boolean flag = preset.name().equals(string);
                        this.renderPresetRow(
                            drawContext, Icon.DEVICE_FLOPPY, preset.name(), f4, animation, animation2, flag, i, i2, f * this.appearAnimator.getProgress(6 + i6)
                        );
                        this.presetRows.add(new CollectorPage.PresetRow(preset, f4));
                        f4 += 21.0F;
                    }

                    this.addPresetY = f4;
                    this.renderAddPresetRow(drawContext, i, i2, f * this.appearAnimator.getProgress(6 + i5));
                } finally {
                    VerticalFadeMask.end();
                    ScissorStack.pop(drawContext);
                }
            }
        }
    }

    private void renderPresetRow(
        DrawContext drawContext, Icon icon2, String string, float f, Animation animation, Animation animation2, boolean flag, int i, int i2, float f2
    ) {
        boolean flag2 = this.isHovered(i, i2, this.presetsX, f, this.presetsWidth, 18.0);
        animation.update();
        animation.animate(flag2 ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f3 = (float)Math.min(animation.get(), 1.0);
        float f4 = f2 * VerticalFadeMask.getAlphaFactor(f + 9.0F);
        GuiRenderUtil.drawOutlined(drawContext, this.presetsX, f, this.presetsWidth, 18.0F, f4 * (0.4F + 0.3F * f3 + (flag ? 0.3F : 0.0F)), 6.0F);
        int i3 = flag
            ? ColorUtil.multiplyAlpha(Theme.getPrimary(), f4)
            : ColorUtil.multiplyAlpha(Theme.withAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), f3), 0.8F), f4);
        int i4 = ColorUtil.multiplyAlpha(
            Theme.withAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), flag ? 1.0F : f3), 0.9F), f4
        );
        float f5 = animation2 != null ? 16.0F : 4.0F;
        new RenderUtil(drawContext, this.presetsX, f, this.presetsWidth, 18.0F)
            .icon(icon2, 5.0F, 5.0F, 8.0F, i3)
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, string, 17.0F, 0.0F, this.presetsWidth - 17.0F - f5, i4, 6.0F, flag2);
        if (animation2 != null) {
            float f6 = this.presetsX + this.presetsWidth - 14.0F;
            float f7 = f + 9.0F - 6.0F;
            boolean flag3;
            if (flag2) {
                flag3 = this.isHovered(i, i2, f6, f7, 12.0, 12.0);
            } else {
                flag3 = false;
            }

            animation2.update();
            animation2.animate(flag3 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
            float f8 = (float)Math.min(animation2.get(), 1.0);
            int i5 = ColorUtil.multiplyAlpha(f8 > 0.5F ? DANGER_COLOR : Theme.withAlpha(Theme.getSecondaryText(), 0.4F + 0.5F * f3), f4);
            new RenderUtil(drawContext, f6 + 2.0F, f7 + 2.0F, 8.0F, 8.0F).icon(Icon.TRASH, 0.0F, 0.0F, 8.0F, i5);
        }
    }

    private void renderAddPresetRow(DrawContext drawContext, int i, int i2, float f) {
        this.addPresetButtonX = this.presetsX + this.presetsWidth - 18.0F;
        float f2 = this.presetsWidth - 18.0F - 3.0F;
        float f3 = f * VerticalFadeMask.getAlphaFactor(this.addPresetY + 9.0F);
        if (this.creatingPreset) {
            GuiRenderUtil.drawOutlined(drawContext, this.presetsX, this.addPresetY, f2, 18.0F, f3 * 0.7F, 6.0F)
                .icon(Icon.CHECK, 5.0F, 4.75F, 8.5F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f3));
            this.presetNameField
                .render(
                    drawContext, new Vector2f(this.presetsX + 17.0F, this.addPresetY + 9.0F - 3.0F), ColorUtil.multiplyAlpha(Theme.getTextColor(), f3), f2 - 21.0F
                );
        } else {
            this.savePresetHoverAnimation.update();
            this.savePresetHoverAnimation.animate(this.isHovered(i, i2, this.presetsX, this.addPresetY, f2, 18.0) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
            float f4 = (float)Math.min(this.savePresetHoverAnimation.get(), 1.0);
            int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.5F + 0.4F * f4), f3);
            GuiRenderUtil.drawOutlined(drawContext, this.presetsX, this.addPresetY, f2, 18.0F, f3 * (0.35F + 0.25F * f4), 6.0F)
                .icon(Icon.DEVICE_FLOPPY, 5.0F, 5.0F, 8.0F, i3)
                .textMiddleLeft(Fonts.INTER_MEDIUM, Language.translate("collector.preset.add", "Сохранить раскладку"), 17.0F, 0.0F, i3, 6.0F);
        }

        this.addPresetHoverAnimation.update();
        this.addPresetHoverAnimation
            .animate(!this.creatingPreset && this.isHovered(i, i2, this.addPresetButtonX, this.addPresetY, 18.0, 18.0) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        float f5 = (float)Math.min(this.addPresetHoverAnimation.get(), 1.0);
        int i4 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.withAlpha(Theme.getTextColor(), 0.5F), Theme.getPrimary(), f5), f3);
        GuiRenderUtil.drawOutlined(drawContext, this.addPresetButtonX, this.addPresetY, 18.0F, 18.0F, f3 * (0.35F + 0.25F * f5), 6.0F)
            .icon(Icon.PLUS, 5.0F, 5.0F, 8.0F, i4);
    }

    private void renderSlotPopup(DrawContext drawContext, CollectorManager collectorManager, int i, int i2, float f) {
        this.contentCells.clear();
        this.enchantRows.clear();
        this.slotPopupAnimation.update();
        this.slotPopupAnimation.animate(this.selectedSlot >= 0 ? 1.0 : 0.0, this.selectedSlot >= 0 ? 0.3 : 0.18, Easings.QUAD_OUT);
        float f2 = (float)Math.min(this.slotPopupAnimation.get(), 1.0);
        if (this.selectedSlot >= 0) {
            this.popupSlot = this.selectedSlot;
        }

        if (f2 <= 0.001F) {
            this.popupSlot = -1;
            this.selectedContent = null;
        } else {
            int i3 = this.popupSlot;
            if (i3 >= 0) {
                CollectorManager.CollectorSlot collectorSlot = collectorManager.getSlot(i3);
                if (!collectorSlot.isEmpty()) {
                    CollectorManager.BundleEntry bundleEntry = this.selectedContent;
                    if (bundleEntry != null && (!collectorSlot.isBundle() || !collectorSlot.getBundleEntries().contains(bundleEntry))) {
                        this.selectedContent = null;
                        bundleEntry = null;
                    }

                    AutoBuyItem autoBuyItem = bundleEntry != null ? bundleEntry.getItem() : collectorSlot.getItem();
                    int i4 = bundleEntry != null ? bundleEntry.getCount() : collectorSlot.getCount();
                    int i5 = autoBuyItem.getMaxCount();
                    boolean flag = i5 > 1;
                    boolean flag2;
                    if (bundleEntry == null) {
                        flag2 = collectorSlot.isBundle();
                    } else {
                        flag2 = false;
                    }

                    List list = flag2 ? collectorSlot.getBundleEntries() : List.of();
                    float f3 = flag2 ? 15.5F + getContentCellSize(96.0F) : 0.0F;
                    List list2 = autoBuyItem.getVariants();
                    float f4 = list2.isEmpty() ? 0.0F : 13.5F + list2.size() * 14.0F;
                    NumberSetting numberSetting = autoBuyItem.getDiscountSetting();
                    float f5 = numberSetting == null ? 0.0F : 14.0F;
                    float f6 = 56.5F + f5 + f4 + f3 + 3.5F + 14.0F;
                    this.popupHeight = Math.min(f6, this.panelHeight);
                    float f7 = this.getSlotX(i3);
                    float f8 = this.getSlotY(i3);
                    this.popupX = f7 + this.slotSize + 6.0F;
                    if (this.popupX + 112.0F > this.panelX + this.panelWidth) {
                        this.popupX = f7 - 112.0F - 6.0F;
                    }

                    this.popupY = Math.clamp(
                        f8 + this.slotSize / 2.0F - this.popupHeight / 2.0F, this.panelY, this.panelY + this.panelHeight - this.popupHeight
                    );
                    float f9 = f * f2;
                    this.popupScroll.update(f6, this.popupHeight);
                    this.popupScroll.updateHover(i, i2);
                    MatrixUtil.push(drawContext);
                    MatrixUtil.translate(drawContext, 0.0F, (1.0F - f2) * 6.0F);
                    GuiRenderUtil.drawPopup(drawContext, this.popupX, this.popupY, 112.0F, this.popupHeight, f9, 8.0F);
                    if (f6 > this.popupHeight) {
                        this.popupScroll.render(drawContext, this.popupX + 112.0F - 2.5F, this.popupY, this.popupHeight, f6, f9);
                    }

                    boolean flag3 = ScissorStack.push(drawContext, this.popupX, this.popupY, 112.0F, this.popupHeight);
                    this.applyScrollFade(this.popupScroll, this.popupY, this.popupHeight, f6);
                    float f10 = 112.0F - 8.0F * 2.0F;
                    float f11 = this.popupY + 4.5F + this.popupScroll.getScroll();
                    GuiRenderUtil.drawItem(drawContext, autoBuyItem.getDisplayStack(), this.popupX + 8.0F, f11, f9);
                    new RenderUtil(drawContext, this.popupX + 8.0F + 18.0F, f11, f10 - 18.0F, 16.0F)
                        .scrollingTextMiddle(
                            Fonts.INTER_SEMIBOLD,
                            autoBuyItem.getId(),
                            0.0F,
                            0.0F,
                            f10 - 18.0F,
                            ColorUtil.multiplyAlpha(Theme.getTextColor(), f9),
                            6.5F,
                            this.isHovered(i, i2, this.popupX, f11, 112.0, 16.0)
                        );
                    float f12 = f11 + 16.0F;
                    GuiRenderUtil.drawOverlay(drawContext, this.popupX + 8.0F + 4.0F, MathUtil.roundToStep(f12 + 1.5F, 0.5F), f10 - 8.0F, 0.5F, f9);
                    float f13 = f12 + 3.5F;
                    this.countSliderAnimation.update();
                    this.countSliderAnimation.animate(this.draggingCountSlider ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
                    float f14 = flag ? 1.0F : 0.4F;
                    this.countFieldX = this.popupX + 112.0F - 8.0F - 20.0F;
                    this.countFieldY = f13 + 7.0F - 5.0F;
                    this.countSliderX = this.countFieldX - 6.0F - 34.0F;
                    this.countSliderY = f13 + 7.0F - 1.5F;
                    new RenderUtil(drawContext, this.popupX + 8.0F, f13, f10, 14.0F)
                        .textMiddleLeft(
                            Fonts.INTER_MEDIUM,
                            Language.translate("collector.page.count", "Кол-во"),
                            0.0F,
                            0.0F,
                            ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.8F * f14), f9),
                            6.0F
                        );
                    SettingWidgets.drawSurfaceLabel(
                        drawContext, this.countFieldX, this.countFieldY, 20.0F, 10.0F, this.editingCount ? "" : String.valueOf(i4), 0.35F, f9 * f14
                    );
                    if (this.editingCount) {
                        this.countField
                            .render(
                                drawContext,
                                new Vector2f(this.countFieldX + 4.0F, this.countFieldY + 5.0F - 3.0F),
                                ColorUtil.multiplyAlpha(Theme.getTextColor(), f9),
                                12.0F
                            );
                    }

                    if (flag) {
                        float f15 = (float)(i4 - 1) / (i5 - 1);
                        SettingWidgets.drawSlider(
                            drawContext, this.countSliderX, this.countSliderY, 34.0F, 3.0F, f15, 6.0F, (float)Math.min(this.countSliderAnimation.get(), 1.0), f9
                        );
                    }

                    float f16 = f13 + 14.0F;
                    this.priceRowY = f16;
                    this.priceFieldX = this.popupX + 112.0F - 8.0F - 52.0F;
                    this.priceFieldY = f16 + 7.0F - 5.0F;
                    new RenderUtil(drawContext, this.popupX + 8.0F, f16, f10, 14.0F)
                        .textMiddleLeft(
                            Fonts.INTER_MEDIUM,
                            Language.translate("autobuy.page.maxprice", "Макс. цена"),
                            0.0F,
                            0.0F,
                            ColorUtil.multiplyAlpha(Theme.getTextColor(), f9),
                            6.0F
                        );
                    GuiRenderUtil.drawSurface(drawContext, this.priceFieldX, this.priceFieldY, 52.0F, 10.0F, f9 * 0.95F, 3.0F);
                    RenderUtil renderUtil = new RenderUtil(drawContext, this.priceFieldX, this.priceFieldY, 52.0F, 10.0F);
                    if (this.editingPrice) {
                        this.priceField
                            .render(
                                drawContext,
                                new Vector2f(this.priceFieldX + 4.0F, this.priceFieldY + 5.0F - 3.0F),
                                ColorUtil.multiplyAlpha(Theme.getTextColor(), f9),
                                44.0F
                            );
                    } else if (collectorManager.hasPrice(autoBuyItem)) {
                        renderUtil.textCenter(Fonts.INTER_MEDIUM, formatNumber(collectorManager.getPrice(autoBuyItem)), ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f9), 6.0F);
                    } else {
                        int i6 = ColorUtil.multiplyAlpha(Theme.getPrimary(), f9);
                        renderUtil.icon(Icon.PLUS, 5.0F, 2.0F, 6.0F, i6)
                            .textMiddleLeft(Fonts.INTER_MEDIUM, Language.translate("collector.page.setprice", "Добавить"), 14.0F, 0.0F, i6, 6.0F);
                    }

                    float f17 = f16 + 14.0F;
                    this.discountRowY = -1.0F;
                    if (numberSetting != null) {
                        this.discountRowY = f17;
                        boolean flag4 = this.isHovered(i, i2, this.popupX + 8.0F, f17, f10, 14.0);
                        if (flag4) {
                            GuiRenderUtil.drawHighlight(drawContext, this.popupX + 8.0F - 4.0F, f17, f10 + 8.0F, 14.0F, f9 * 0.35F, Radius.all(4.6666665F));
                        }

                        new RenderUtil(drawContext, this.popupX + 8.0F, f17, f10, 14.0F)
                            .textMiddleLeft(
                                Fonts.INTER_MEDIUM, Language.translate(numberSetting.getName()), 0.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f9), 6.0F
                            );
                        SettingWidgets.drawSurfaceLabel(
                            drawContext,
                            this.popupX + 112.0F - 8.0F - 20.0F,
                            f17 + 7.0F - 5.0F,
                            20.0F,
                            10.0F,
                            String.valueOf((int)numberSetting.getValue().floatValue()),
                            0.35F,
                            f9
                        );
                        f17 += 14.0F;
                    }

                    if (!list2.isEmpty()) {
                        f17 = this.renderEnchantOptions(drawContext, list2, f17, f10, i, i2, f9);
                    }

                    if (flag2) {
                        f17 = this.renderContainerContents(drawContext, list, f17, f10, i, i2, f9);
                    }

                    GuiRenderUtil.drawOverlay(drawContext, this.popupX + 8.0F + 4.0F, MathUtil.roundToStep(f17 + 1.5F, 0.5F), f10 - 8.0F, 0.5F, f9);
                    float f18 = f17 + 3.5F;
                    this.removeRowY = f18;
                    this.removeHoverAnimation.update();
                    this.removeHoverAnimation.animate(this.isHovered(i, i2, this.popupX + 8.0F, f18, f10, 14.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
                    float f19 = (float)Math.min(this.removeHoverAnimation.get(), 1.0);
                    if (f19 > 0.001F) {
                        GuiRenderUtil.drawHighlight(drawContext, this.popupX + 8.0F - 4.0F, f18, f10 + 8.0F, 14.0F, f9 * f19 * 0.8F, Radius.all(4.6666665F));
                    }

                    int i7 = Theme.withAlpha(DANGER_COLOR, f9);
                    new RenderUtil(drawContext, this.popupX + 8.0F, f18, f10, 14.0F)
                        .textMiddleLeft(Fonts.INTER_MEDIUM, Language.translate("collector.page.remove", "Убрать"), 0.0F, 0.0F, i7, 6.0F)
                        .icon(Icon.TRASH, f10 - 8.5F, 2.75F, 8.5F, i7);
                    VerticalFadeMask.end();
                    if (flag3) {
                        ScissorStack.pop(drawContext);
                    }

                    MatrixUtil.pop(drawContext);
                }
            }
        }
    }

    private float renderEnchantOptions(DrawContext drawContext, List<EnchantRequirement> enchantRequirements, float f, float f2, int i, int i2, float f3) {
        GuiRenderUtil.drawOverlay(drawContext, this.popupX + 8.0F + 4.0F, MathUtil.roundToStep(f + 1.5F, 0.5F), f2 - 8.0F, 0.5F, f3);
        float f4 = f + 3.5F;
        new RenderUtil(drawContext, this.popupX + 8.0F, f4, f2, 10.0F)
            .textMiddleLeft(
                Fonts.INTER_SEMIBOLD,
                Language.translate("autobuy.page.enchants", "Зачарования"),
                0.0F,
                0.0F,
                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.65F), f3),
                5.5F
            );
        float f5 = f4 + 10.0F;
        float f6 = this.popupX + 8.0F + f2 - 14.0F;
        float f7 = f6 - 5.0F - 20.0F;

        for (EnchantRequirement enchantRequirement : enchantRequirements) {
            boolean flag = this.isHovered(i, i2, this.popupX + 8.0F, f5, f2, 14.0);
            Animation animation = this.enchantAnimations.computeIfAbsent(enchantRequirement, enchantRequirement2 -> new Animation());
            animation.update();
            animation.animate(enchantRequirement.getMode() == EnchantRequirement.Mode.ANY ? 0.0 : 1.0, 0.3, Easings.EXPO_OUT);
            float f8 = (float)Math.clamp(animation.get(), 0.0, 1.0);
            if (flag) {
                GuiRenderUtil.drawHighlight(drawContext, this.popupX + 8.0F - 4.0F, f5, f2 + 8.0F, 14.0F, f3 * 0.35F, Radius.all(4.6666665F));
            }

            new RenderUtil(drawContext, this.popupX + 8.0F, f5, f2, 14.0F)
                .scrollingTextMiddle(
                    Fonts.INTER_MEDIUM,
                    enchantRequirement.getDisplayName(),
                    0.0F,
                    0.0F,
                    f7 - this.popupX - 8.0F - 4.0F,
                    ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.45F + 0.4F * f8), f3),
                    6.0F,
                    flag
                );
            boolean flag2 = enchantRequirement.isExcluded();
            float f9 = f5 + 7.0F - 5.0F;
            GuiRenderUtil.drawSurface(drawContext, f7, f9, 20.0F, 10.0F, f3 * (0.35F + 0.45F * f8), 3.0F);
            new RenderUtil(drawContext, f7, f9, 20.0F, 10.0F)
                .textCenter(
                    Fonts.INTER_MEDIUM,
                    flag2 ? Language.translate("autobuy.page.without", "нет") : String.valueOf(enchantRequirement.getMinLevel()),
                    flag2 ? Theme.withAlpha(Theme.getPrimary(), f3) : ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f3),
                    6.0F
                );
            SettingWidgets.drawDefaultSwitch(drawContext, f6, f5 + 7.0F - 4.5F, f8, f3);
            this.enchantRows.add(new CollectorPage.EnchantRow(enchantRequirement, f5, f7));
            f5 += 14.0F;
        }

        return f5;
    }

    private float renderContainerContents(DrawContext drawContext, List<CollectorManager.BundleEntry> bundleEntries, float f, float f2, int i, int i2, float f3) {
        GuiRenderUtil.drawOverlay(drawContext, this.popupX + 8.0F + 4.0F, MathUtil.roundToStep(f + 1.5F, 0.5F), f2 - 8.0F, 0.5F, f3);
        float f4 = f + 3.5F;
        int i3 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.65F), f3);
        new RenderUtil(drawContext, this.popupX + 8.0F, f4, f2, 10.0F)
            .textMiddleLeft(Fonts.INTER_SEMIBOLD, Language.translate("collector.page.contents", "Содержимое"), 0.0F, 0.0F, i3, 5.5F)
            .textMiddleRight(Fonts.INTER_MEDIUM, bundleEntries.size() + "/4", 0.0F, 0.0F, i3, 5.5F);
        float f5 = f4 + 12.0F;
        float f6 = getContentCellSize(f2);
        int i4 = bundleEntries.size();

        for (int i5 = 0; i5 < 4; i5++) {
            float f7 = this.popupX + 8.0F + i5 * (f6 + 2.0F);
            CollectorManager.BundleEntry bundleEntry = i5 < i4 ? (CollectorManager.BundleEntry)bundleEntries.get(i5) : null;
            Animation animation = this.contentHoverAnimations[i5];
            animation.update();
            animation.animate(this.isHovered(i, i2, f7, f5, f6, f6) ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
            float f8 = (float)Math.min(animation.get(), 1.0);
            GuiRenderUtil.drawOutlined(drawContext, f7, f5, f6, f6, f3 * (0.5F + 0.25F * f8), 4.0F);
            if (bundleEntry == null) {
                new RenderUtil(drawContext, f7, f5, f6, f6)
                    .icon(Icon.PLUS, f6 / 2.0F - 3.5F, f6 / 2.0F - 3.5F, 7.0F, Theme.withAlpha(Theme.getSecondaryText(), (0.18F + 0.2F * f8) * f3));
            } else {
                this.contentCells.add(new CollectorPage.ContentCell(bundleEntry, f7, f5, f6));
                float f9 = f7 + f6 / 2.0F - 8.0F;
                float f10 = f5 + f6 / 2.0F - 8.0F;
                GuiRenderUtil.drawItem(drawContext, bundleEntry.getItem().getDisplayStack(), f9, f10, f3);
                if (bundleEntry.getCount() > 1) {
                    String string = String.valueOf(bundleEntry.getCount());
                    drawContext.drawText(
                        mc.textRenderer,
                        string,
                        (int)f9 + 17 - mc.textRenderer.getWidth(string),
                        (int)f10 + 9,
                        ColorUtil.setAlphaf(ColorUtil.WHITE, f3),
                        true
                    );
                }
            }
        }

        return f5 + f6;
    }

    private static float getContentCellSize(float f) {
        return (f - 6.0F) / 4.0F;
    }

    private float getSlotX(int i) {
        int i2 = i >= 36 ? (i == 40 ? 8 : i - 36) : i % 9;
        return this.gridX + i2 * (this.slotSize + 2.0F);
    }

    private float getSlotY(int i) {
        if (i >= 36) {
            return this.gridY;
        }

        int i2 = i / 9;
        float f = this.gridY + this.slotSize + 4.0F + i2 * (this.slotSize + 2.0F);
        return i2 == 3 ? f + 4.0F : f;
    }

    private int getSlotAt(double d, double d2) {
        for (int i = 0; i < 41; i++) {
            if (this.isHovered(d, d2, this.getSlotX(i), this.getSlotY(i), this.slotSize, this.slotSize)) {
                return i;
            }
        }

        return -1;
    }

    private void applyScrollFade(SmoothScrollbar smoothScrollbar, float f, float f2, float f3) {
        float f4 = f3 - f2;
        if (!(f4 <= 0.5F) && !(f2 <= 28.0F)) {
            float f5 = smoothScrollbar.getScroll();
            float f6 = Math.clamp(-f5 / 14.0F, 0.0F, 1.0F);
            float f7 = Math.clamp((f4 + f5) / 14.0F, 0.0F, 1.0F);
            if (!(f6 <= 0.001F) || !(f7 <= 0.001F)) {
                VerticalFadeMask.begin(f, f + 14.0F, f6, f + f2 - 14.0F, f + f2, f7);
            }
        }
    }

    public boolean mouseClicked(Click click, boolean flag) {
        CollectorManager collectorManager = this.getCollector();
        if (collectorManager == null) {
            return false;
        }

        double d = click.x();
        double d2 = click.y();
        if (this.draggedItem != null && this.isLeftButton(click.button())) {
            this.dropDraggedItem(collectorManager, d, d2);
            return true;
        }

        if (this.selectedSlot >= 0 && !this.isHovered(d, d2, this.popupX, this.popupY, 112.0, this.popupHeight)) {
            this.closeSlotPopup();
        }

        if (this.isLeftButton(click.button()) && this.isHovered(d, d2, this.toggleButtonX, this.panelY, 15.0, 15.0)) {
            this.commitFieldEdits();
            if (this.toggleLocked) {
                return true;
            }

            KitCollector kitCollector = KitCollector.getInstance();
            if (kitCollector != null) {
                kitCollector.toggle();
            }
        } else {
            if (this.isLeftButton(click.button()) && this.isHovered(d, d2, this.clearButtonX, this.panelY, 15.0, 15.0)) {
                this.commitFieldEdits();
                if (this.clearPending && !this.clearConfirmTimer.hasPassed(2500.0)) {
                    this.closeSlotPopup();
                    collectorManager.clearLayout();
                    this.clearPending = false;
                    return true;
                }

                this.clearPending = true;
                this.clearConfirmTimer.reset();
                return true;
            }

            if (this.isLeftButton(click.button()) && this.isHovered(d, d2, this.serverButtonX, this.panelY, this.serverButtonWidth, 15.0)) {
                this.commitFieldEdits();
                this.cycleServer(collectorManager);
                return true;
            }

            if (this.selectedSlot >= 0 && this.isHovered(d, d2, this.popupX, this.popupY, 112.0, this.popupHeight)) {
                if (!this.popupScroll.mouseClicked(d, d2)) {
                    this.handlePopupClick(collectorManager, click, d, d2);
                    return true;
                }
            } else {
                if (this.isHovered(d, d2, this.itemListX, this.panelY, 104.0, 15.0)) {
                    this.commitFieldEdits();
                    this.searchField.setFocused(true);
                    this.searchField.mouseClicked(d, d2, click.button());
                    return true;
                }

                this.searchField.setFocused(false);
                if (!this.itemListScroll.mouseClicked(d, d2)) {
                    if (this.presetScroll.mouseClicked(d, d2)) {
                        return true;
                    }

                    if (this.isHovered(d, d2, this.itemListX, this.itemListY, 104.0, this.itemListHeight)) {
                        this.commitFieldEdits();

                        for (CollectorPage.ItemRow itemRow : this.itemRows) {
                            if (this.isHovered(d, d2, this.itemListX, itemRow.y(), 95.0, 18.0)) {
                                if (this.isLeftButton(click.button())) {
                                    this.startDrag(collectorManager, itemRow.item(), itemRow.item().getMaxCount(), -1, d, d2);
                                    return true;
                                }
                                break;
                            }
                        }
                    } else {
                        int i = this.getSlotAt(d, d2);
                        if (i < 0) {
                            if (this.handlePresetsClick(collectorManager, click, d, d2)) {
                                return true;
                            }

                            this.commitFieldEdits();
                            this.cancelPresetCreation();
                            return false;
                        }

                        this.commitFieldEdits();
                        this.cancelPresetCreation();
                        if (this.isRightButton(click.button())) {
                            if (!collectorManager.getSlot(i).isEmpty()) {
                                if (this.lastRightClickedSlot == i && !this.rightClickTimer.hasPassed(350.0)) {
                                    collectorManager.clearSlot(i);
                                    if (this.selectedSlot == i) {
                                        this.closeSlotPopup();
                                    }

                                    this.lastRightClickedSlot = -1;
                                    return true;
                                }

                                this.lastRightClickedSlot = i;
                                this.lastRightClickedContent = null;
                                this.rightClickTimer.reset();
                                this.openSlotPopup(i);
                                return true;
                            }
                        } else {
                            CollectorManager.CollectorSlot collectorSlot = collectorManager.getSlot(i);
                            if (this.isMiddleButton(click.button()) && !collectorSlot.isEmpty()) {
                                this.startDrag(collectorManager, collectorSlot.getItem(), collectorSlot.getCount(), -1, d, d2);
                                return true;
                            }

                            if (this.isLeftButton(click.button()) && !collectorSlot.isEmpty()) {
                                this.startDrag(collectorManager, collectorSlot.getItem(), collectorSlot.getCount(), i, d, d2);
                                this.draggedContents = List.copyOf(collectorSlot.getBundleEntries());
                                collectorManager.clearSlot(i);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    private void startDrag(CollectorManager collectorManager, AutoBuyItem autoBuyItem, int i, int i2, double d, double d2) {
        this.cancelDrag(collectorManager);
        this.commitFieldEdits();
        this.cancelPresetCreation();
        this.draggedItem = autoBuyItem;
        this.draggedCount = i;
        this.dragSourceSlot = i2;
        this.draggedContents = List.of();
        this.dragSourceContainerSlot = -1;
        this.dragStartX = (float)d;
        this.dragStartY = (float)d2;
    }

    private void dropDraggedItem(CollectorManager collectorManager, double d, double d2) {
        if (this.selectedSlot < 0
            || !this.isHovered(d, d2, this.popupX, this.popupY, 112.0, this.popupHeight)
            || !this.tryInsertIntoContainer(collectorManager, this.selectedSlot, d, d2)) {
            if ((this.dragSourceSlot >= 0 || this.dragSourceContainerSlot >= 0)
                && this.isHovered(d, d2, this.itemListX - 3.0F, this.panelY, 110.0, this.panelHeight)) {
                this.flyingItems.add(new CollectorPage.FlyingItem(this.draggedItem, (float)d, (float)d2, (float)d, (float)d2, -1));
                this.clearDrag();
            } else {
                int i = this.getSlotAt(d, d2);
                if (i >= 0 && collectorManager.getSlot(i).isBundle() && this.draggedItem.getItem() != Items.BUNDLE) {
                    if (this.tryInsertIntoContainer(collectorManager, i, d, d2)) {
                        return;
                    }
                } else if (i >= 0 && collectorManager.canPlace(i, this.draggedItem)) {
                    CollectorManager.CollectorSlot collectorSlot = collectorManager.getSlot(i);
                    if (this.dragSourceContainerSlot < 0) {
                        if (this.dragSourceSlot < 0) {
                            collectorManager.placeItem(i, this.draggedItem, this.draggedCount);
                            this.flyingItems
                                .add(
                                    new CollectorPage.FlyingItem(
                                        this.draggedItem, (float)d, (float)d2, this.getSlotCenterX(i), this.getSlotCenterY(i), i
                                    )
                                );
                            this.clearDrag();
                            return;
                        }

                        AutoBuyItem autoBuyItem = collectorSlot.getItem();
                        int i2 = collectorSlot.getCount();
                        List list = List.copyOf(collectorSlot.getBundleEntries());
                        collectorManager.placeItem(i, this.draggedItem, this.draggedCount);
                        if (!this.draggedContents.isEmpty()) {
                            collectorManager.setBundleEntries(i, this.draggedContents);
                        }

                        this.flyingItems
                            .add(
                                new CollectorPage.FlyingItem(
                                    this.draggedItem, (float)d, (float)d2, this.getSlotCenterX(i), this.getSlotCenterY(i), i
                                )
                            );
                        if (autoBuyItem != null && collectorManager.getSlot(this.dragSourceSlot).isEmpty() && collectorManager.canPlace(this.dragSourceSlot, autoBuyItem)) {
                            collectorManager.placeItem(this.dragSourceSlot, autoBuyItem, i2);
                            if (!list.isEmpty()) {
                                collectorManager.setBundleEntries(this.dragSourceSlot, list);
                            }

                            this.flyingItems
                                .add(
                                    new CollectorPage.FlyingItem(
                                        autoBuyItem,
                                        this.getSlotCenterX(i),
                                        this.getSlotCenterY(i),
                                        this.getSlotCenterX(this.dragSourceSlot),
                                        this.getSlotCenterY(this.dragSourceSlot),
                                        this.dragSourceSlot
                                    )
                                );
                        }

                        this.clearDrag();
                        return;
                    }

                    if (collectorSlot.isEmpty()) {
                        collectorManager.placeItem(i, this.draggedItem, this.draggedCount);
                        this.flyingItems
                            .add(
                                new CollectorPage.FlyingItem(
                                    this.draggedItem, (float)d, (float)d2, this.getSlotCenterX(i), this.getSlotCenterY(i), i
                                )
                            );
                        this.clearDrag();
                        return;
                    }
                }

                if (this.dragSourceSlot >= 0 && collectorManager.getSlot(this.dragSourceSlot).isEmpty() && collectorManager.canPlace(this.dragSourceSlot, this.draggedItem)) {
                    collectorManager.placeItem(this.dragSourceSlot, this.draggedItem, this.draggedCount);
                    if (!this.draggedContents.isEmpty()) {
                        collectorManager.setBundleEntries(this.dragSourceSlot, this.draggedContents);
                    }

                    this.flyingItems
                        .add(
                            new CollectorPage.FlyingItem(
                                this.draggedItem,
                                (float)d,
                                (float)d2,
                                this.getSlotCenterX(this.dragSourceSlot),
                                this.getSlotCenterY(this.dragSourceSlot),
                                this.dragSourceSlot
                            )
                        );
                    this.clearDrag();
                } else if (this.dragSourceContainerSlot >= 0 && collectorManager.getSlot(this.dragSourceContainerSlot).isBundle()) {
                    collectorManager.addToBundle(this.dragSourceContainerSlot, this.draggedItem, this.draggedCount);
                    this.flyingItems
                        .add(
                            new CollectorPage.FlyingItem(
                                this.draggedItem,
                                (float)d,
                                (float)d2,
                                this.getSlotCenterX(this.dragSourceContainerSlot),
                                this.getSlotCenterY(this.dragSourceContainerSlot),
                                -1
                            )
                        );
                    this.clearDrag();
                } else {
                    if (this.dragSourceSlot < 0) {
                        this.flyingItems.add(new CollectorPage.FlyingItem(this.draggedItem, (float)d, (float)d2, this.dragStartX, this.dragStartY, -1));
                    }

                    this.clearDrag();
                }
            }
        }
    }

    private boolean tryInsertIntoContainer(CollectorManager collectorManager, int i, double d, double d2) {
        if (collectorManager.getSlot(i).isBundle() && this.draggedItem.getItem() != Items.BUNDLE) {
            int i2 = collectorManager.addToBundle(i, this.draggedItem, this.draggedCount);
            if (i2 <= 0) {
                return false;
            }

            this.flyingItems
                .add(new CollectorPage.FlyingItem(this.draggedItem, (float)d, (float)d2, this.getSlotCenterX(i), this.getSlotCenterY(i), -1));
            int i3 = this.draggedCount - i2;
            if (i3 > 0) {
                if (this.dragSourceSlot >= 0 && collectorManager.getSlot(this.dragSourceSlot).isEmpty() && collectorManager.canPlace(this.dragSourceSlot, this.draggedItem)) {
                    collectorManager.placeItem(this.dragSourceSlot, this.draggedItem, i3);
                    this.clearDrag();
                    return true;
                }

                if (this.dragSourceContainerSlot >= 0 && this.dragSourceContainerSlot != i && collectorManager.getSlot(this.dragSourceContainerSlot).isBundle()) {
                    collectorManager.addToBundle(this.dragSourceContainerSlot, this.draggedItem, i3);
                }
            }

            this.clearDrag();
            return true;
        } else {
            return false;
        }
    }

    private void clearDrag() {
        this.draggedItem = null;
        this.dragSourceSlot = -1;
        this.draggedContents = List.of();
        this.dragSourceContainerSlot = -1;
    }

    private float getSlotCenterX(int i) {
        return this.getSlotX(i) + this.slotSize / 2.0F;
    }

    private float getSlotCenterY(int i) {
        return this.getSlotY(i) + this.slotSize / 2.0F;
    }

    private boolean handlePopupClick(CollectorManager collectorManager, Click click, double d, double d2) {
        CollectorManager.CollectorSlot collectorSlot = collectorManager.getSlot(this.selectedSlot);
        if (collectorSlot.isEmpty()) {
            return false;
        }

        AutoBuyItem autoBuyItem = this.selectedContent != null ? this.selectedContent.getItem() : collectorSlot.getItem();
        boolean flag = autoBuyItem.getMaxCount() > 1;
        float f = 112.0F - 8.0F * 2.0F;
        NumberSetting numberSetting = autoBuyItem.getDiscountSetting();
        if (numberSetting != null && this.discountRowY >= 0.0F && this.isHovered(d, d2, this.popupX + 8.0F, this.discountRowY, f, 14.0)) {
            float f2 = numberSetting.getValue() + 1.0F;
            if (f2 > numberSetting.getMax()) {
                numberSetting.setValue(Float.valueOf(numberSetting.getMin()));
                this.saveAutoBuyConfig();
                return true;
            } else {
                numberSetting.setValue(Float.valueOf(f2));
                this.saveAutoBuyConfig();
                return true;
            }
        } else {
            for (CollectorPage.EnchantRow enchantRow : this.enchantRows) {
                if (this.isHovered(d, d2, this.popupX + 8.0F, enchantRow.y(), f, 14.0)) {
                    EnchantRequirement enchantRequirement = enchantRow.option();
                    if (this.isRightButton(click.button())) {
                        if (enchantRequirement.isExcluded()) {
                            enchantRequirement.setMode(EnchantRequirement.Mode.ANY);
                            this.saveAutoBuyConfig();
                            return true;
                        }

                        enchantRequirement.setMode(EnchantRequirement.Mode.EXCLUDE);
                        this.saveAutoBuyConfig();
                        return true;
                    }

                    if (enchantRequirement.isRequired()) {
                        enchantRequirement.setMode(EnchantRequirement.Mode.ANY);
                        this.saveAutoBuyConfig();
                        return true;
                    }

                    enchantRequirement.setMode(EnchantRequirement.Mode.REQUIRE);
                    this.saveAutoBuyConfig();
                    return true;
                }
            }

            for (CollectorPage.ContentCell contentCell : this.contentCells) {
                if (this.isHovered(d, d2, contentCell.x(), contentCell.y(), contentCell.size(), contentCell.size())) {
                    if (this.isRightButton(click.button())) {
                        this.commitFieldEdits();
                        if (this.lastRightClickedContent == contentCell.entry() && !this.rightClickTimer.hasPassed(350.0)) {
                            collectorManager.removeFromBundle(this.selectedSlot, contentCell.entry());
                            if (this.selectedContent == contentCell.entry()) {
                                this.selectedContent = null;
                            }

                            this.lastRightClickedContent = null;
                            return true;
                        }

                        this.lastRightClickedContent = contentCell.entry();
                        this.lastRightClickedSlot = -1;
                        this.rightClickTimer.reset();
                        this.selectedContent = contentCell.entry();
                        return true;
                    }

                    if (this.isLeftButton(click.button())) {
                        int i = this.selectedSlot;
                        CollectorManager.BundleEntry bundleEntry = contentCell.entry();
                        this.startDrag(collectorManager, bundleEntry.getItem(), bundleEntry.getCount(), -1, d, d2);
                        this.dragSourceContainerSlot = i;
                        collectorManager.removeFromBundle(i, bundleEntry);
                        if (this.selectedContent == bundleEntry) {
                            this.selectedContent = null;
                        }
                    }

                    return true;
                }
            }

            if (this.isHovered(d, d2, this.countFieldX, this.countFieldY, 20.0, 10.0)) {
                if (this.selectedContent != null) {
                    this.startCountEdit(this.selectedContent.getCount());
                    return true;
                } else {
                    this.startCountEdit(collectorSlot.getCount());
                    return true;
                }
            } else {
                if (flag && this.isHovered(d, d2, this.countSliderX - 3.0F, this.countSliderY - 5.0F, 40.0, 13.0)) {
                    this.commitFieldEdits();
                    this.draggingCountSlider = true;
                    this.updateCountFromSlider(d);
                    return true;
                }

                if (this.isHovered(d, d2, this.priceFieldX, this.priceRowY, 52.0, 14.0)) {
                    this.startPriceEdit(collectorManager, autoBuyItem);
                    return true;
                }

                if (this.isHovered(d, d2, this.popupX + 8.0F, this.removeRowY, f, 14.0)) {
                    this.commitFieldEdits();
                    if (this.selectedContent != null) {
                        collectorManager.removeFromBundle(this.selectedSlot, this.selectedContent);
                        this.selectedContent = null;
                        return true;
                    } else {
                        collectorManager.clearSlot(this.selectedSlot);
                        this.closeSlotPopup();
                        return true;
                    }
                } else {
                    return this.isHovered(d, d2, this.popupX, this.popupY, 112.0, this.popupHeight);
                }
            }
        }
    }

    private boolean handlePresetsClick(CollectorManager collectorManager, Click click, double d, double d2) {
        if (!this.isHovered(d, d2, this.presetsX - 2.0F, this.presetsListY, this.presetsWidth + 6.0F, this.presetsListHeight)) {
            return false;
        }

        this.commitFieldEdits();
        if (this.creatingPreset) {
            if (this.isHovered(d, d2, this.presetsX, this.addPresetY, 16.0, 18.0)) {
                this.confirmPresetCreation(collectorManager);
                return true;
            }

            if (this.isHovered(d, d2, this.presetsX, this.addPresetY, this.presetsWidth - 18.0F - 3.0F, 18.0)) {
                this.presetNameField.mouseClicked(d, d2, click.button());
                this.presetNameField.setFocused(true);
                return true;
            }

            this.cancelPresetCreation();
        } else {
            for (CollectorPage.PresetRow presetRow : this.presetRows) {
                if (this.isHovered(d, d2, this.presetsX, presetRow.y(), this.presetsWidth, 18.0)) {
                    float f = this.presetsX + this.presetsWidth - 14.0F;
                    if (this.isHovered(d, d2, f, presetRow.y() + 9.0F - 6.0F, 12.0, 12.0)) {
                        collectorManager.deletePreset(presetRow.preset());
                        return true;
                    }

                    this.cancelDrag(collectorManager);
                    this.closeSlotPopup();
                    collectorManager.applyPreset(presetRow.preset());
                    return true;
                }
            }

            if (this.isHovered(d, d2, this.addPresetButtonX, this.addPresetY, 18.0, 18.0)) {
                this.overwritePreset = true;
                this.startPresetCreation();
                return true;
            }

            if (this.isHovered(d, d2, this.presetsX, this.addPresetY, this.presetsWidth - 18.0F - 3.0F, 18.0)) {
                this.startPresetCreation();
                return true;
            }
        }

        return true;
    }

    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.draggingCountSlider) {
            this.updateCountFromSlider(click.x());
            return true;
        } else {
            return this.itemListScroll.mouseDragged(click.x(), click.y())
                || this.presetScroll.mouseDragged(click.x(), click.y())
                || this.popupScroll.mouseDragged(click.x(), click.y());
        }
    }

    public void mouseReleased(Click click) {
        if (this.draggingCountSlider) {
            this.draggingCountSlider = false;
            CollectorManager collectorManager = this.getCollector();
            if (collectorManager != null) {
                collectorManager.save();
            }
        }

        CollectorManager collectorManager2 = this.getCollector();
        if (collectorManager2 != null && this.draggedItem != null && (this.isLeftButton(click.button()) || this.isMiddleButton(click.button()))) {
            this.dropDraggedItem(collectorManager2, click.x(), click.y());
        }

        this.itemListScroll.mouseReleased();
        this.presetScroll.mouseReleased();
        this.popupScroll.mouseReleased();
    }

    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        CollectorManager collectorManager = this.getCollector();
        if (collectorManager == null) {
            return false;
        }

        if (this.selectedSlot >= 0 && this.isHovered(d, d2, this.popupX, this.popupY, 112.0, this.popupHeight)) {
            AutoBuyItem autoBuyItem = this.selectedContent != null ? this.selectedContent.getItem() : collectorManager.getSlot(this.selectedSlot).getItem();
            NumberSetting numberSetting = autoBuyItem == null ? null : autoBuyItem.getDiscountSetting();
            if (numberSetting != null && this.discountRowY >= 0.0F && this.isHovered(d, d2, this.popupX + 8.0F, this.discountRowY, 96.0, 14.0)) {
                float f = numberSetting.getValue();
                if (d4 > 0.0) {
                    numberSetting.setValue(Float.valueOf(f + 1.0F));
                    this.saveAutoBuyConfig();
                    return true;
                } else {
                    numberSetting.setValue(Float.valueOf(f + -1.0F));
                    this.saveAutoBuyConfig();
                    return true;
                }
            } else {
                for (CollectorPage.EnchantRow enchantRow : this.enchantRows) {
                    if (this.isHovered(d, d2, enchantRow.chipX(), enchantRow.y(), 20.0, 14.0)) {
                        EnchantRequirement enchantRequirement = enchantRow.option();
                        if (enchantRequirement.getMaxLevel() > 1) {
                            enchantRequirement.setMinLevel(enchantRequirement.getMinLevel() + (d4 > 0.0 ? 1 : -1));
                            enchantRequirement.setMode(EnchantRequirement.Mode.REQUIRE);
                            this.saveAutoBuyConfig();
                        }

                        return true;
                    }
                }

                for (CollectorPage.ContentCell contentCell : this.contentCells) {
                    if (this.isHovered(d, d2, contentCell.x(), contentCell.y(), contentCell.size(), contentCell.size())) {
                        CollectorManager.BundleEntry bundleEntry = contentCell.entry();
                        int i = contentCell.entry().getCount();
                        if (d4 > 0.0) {
                            collectorManager.setBundleEntryCount(bundleEntry, i + 1);
                            collectorManager.save();
                            return true;
                        }

                        collectorManager.setBundleEntryCount(bundleEntry, i + -1);
                        collectorManager.save();
                        return true;
                    }
                }

                this.popupScroll.onScroll(d4);
                return true;
            }
        } else {
            int i2 = this.getSlotAt(d, d2);
            if (i2 >= 0 && !collectorManager.getSlot(i2).isEmpty()) {
                int i3 = collectorManager.getSlot(i2).getCount();
                if (d4 > 0.0) {
                    collectorManager.setSlotCount(i2, i3 + 1);
                    collectorManager.save();
                    return true;
                } else {
                    collectorManager.setSlotCount(i2, i3 + -1);
                    collectorManager.save();
                    return true;
                }
            } else if (this.isHovered(d, d2, this.itemListX, this.itemListY, 104.0, this.itemListHeight)) {
                this.itemListScroll.onScroll(d4);
                return true;
            } else if (this.isHovered(d, d2, this.presetsX - 2.0F, this.presetsListY, this.presetsWidth + 6.0F, this.presetsListHeight)) {
                this.presetScroll.onScroll(d4);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean keyPressed(KeyInput keyInput) {
        if (this.editingCount) {
            if (keyInput.key() == 257 || keyInput.key() == 335) {
                this.commitCountEdit();
                return true;
            } else if (keyInput.key() == 256) {
                this.cancelCountEdit();
                return true;
            } else {
                this.countField.keyPressed(keyInput.key());
                return true;
            }
        } else if (this.editingPrice) {
            if (keyInput.key() == 257 || keyInput.key() == 335) {
                this.commitFieldEdits();
                return true;
            } else if (keyInput.key() == 256) {
                this.cancelPriceEdit();
                return true;
            } else {
                this.priceField.keyPressed(keyInput.key());
                return true;
            }
        } else {
            if (this.creatingPreset) {
                if (keyInput.key() != 257 && keyInput.key() != 335) {
                    if (keyInput.key() == 256) {
                        this.cancelPresetCreation();
                        return true;
                    }

                    this.presetNameField.keyPressed(keyInput.key());
                    return true;
                }

                CollectorManager collectorManager = this.getCollector();
                if (collectorManager != null) {
                    this.confirmPresetCreation(collectorManager);
                }
            } else {
                if (this.searchField.isFocused()) {
                    if (keyInput.key() == 256) {
                        this.searchField.setFocused(false);
                        return true;
                    }

                    this.searchField.keyPressed(keyInput.key());
                    return true;
                }

                if (keyInput.key() == 256) {
                    if (this.selectedSlot >= 0) {
                        this.closeSlotPopup();
                        return true;
                    }

                    if (this.draggedItem != null) {
                        CollectorManager collectorManager2 = this.getCollector();
                        if (collectorManager2 != null) {
                            this.cancelDrag(collectorManager2);
                            return true;
                        }

                        return true;
                    }
                }

                return false;
            }

            return true;
        }
    }

    public boolean charTyped(CharInput charInput) {
        if (this.editingCount) {
            this.countField.charTyped(charInput.codepoint());
            return true;
        } else if (this.editingPrice) {
            this.priceField.charTyped(charInput.codepoint());
            return true;
        } else if (this.creatingPreset) {
            this.presetNameField.charTyped(charInput.codepoint());
            return true;
        } else if (this.searchField.isFocused()) {
            this.searchField.charTyped(charInput.codepoint());
            return true;
        } else {
            return false;
        }
    }

    private void cancelDrag(CollectorManager collectorManager) {
        if (this.draggedItem != null) {
            if (this.dragSourceSlot >= 0 && collectorManager.getSlot(this.dragSourceSlot).isEmpty() && collectorManager.canPlace(this.dragSourceSlot, this.draggedItem)) {
                collectorManager.placeItem(this.dragSourceSlot, this.draggedItem, this.draggedCount);
                if (!this.draggedContents.isEmpty()) {
                    collectorManager.setBundleEntries(this.dragSourceSlot, this.draggedContents);
                }
            } else if (this.dragSourceContainerSlot >= 0 && collectorManager.getSlot(this.dragSourceContainerSlot).isBundle()) {
                collectorManager.addToBundle(this.dragSourceContainerSlot, this.draggedItem, this.draggedCount);
            }
        }

        this.draggedItem = null;
        this.dragSourceSlot = -1;
        this.draggedContents = List.of();
        this.dragSourceContainerSlot = -1;
    }

    private void openSlotPopup(int i) {
        this.commitFieldEdits();
        this.selectedSlot = i;
        this.selectedContent = null;
        this.popupScroll.reset();
    }

    private void closeSlotPopup() {
        this.commitFieldEdits();
        this.draggingCountSlider = false;
        this.selectedSlot = -1;
    }

    private void startPresetCreation() {
        this.creatingPreset = true;
        this.presetNameField.setText("");
        this.presetNameField.setFocused(true);
    }

    private void confirmPresetCreation(CollectorManager collectorManager) {
        String string = this.presetNameField.getText().trim();
        if (!string.isEmpty()) {
            if (this.overwritePreset) {
                collectorManager.clearLayout();
            }

            collectorManager.savePreset(string);
        }

        this.cancelPresetCreation();
    }

    private void cancelPresetCreation() {
        this.creatingPreset = false;
        this.overwritePreset = false;
        this.presetNameField.setFocused(false);
        this.presetNameField.setText("");
    }

    private void saveAutoBuyConfig() {
        AutoBuyManager autoBuyManager = Sadnes.getInstance().getAutoBuyManager();
        if (autoBuyManager != null) {
            autoBuyManager.save();
        }
    }

    private void updateCountFromSlider(double d) {
        CollectorManager collectorManager = this.getCollector();
        if (collectorManager != null && this.selectedSlot >= 0) {
            CollectorManager.CollectorSlot collectorSlot = collectorManager.getSlot(this.selectedSlot);
            if (!collectorSlot.isEmpty()) {
                AutoBuyItem autoBuyItem = this.selectedContent != null ? this.selectedContent.getItem() : collectorSlot.getItem();
                int i = autoBuyItem.getMaxCount();
                float f = Math.clamp((float)(d - this.countSliderX) / 34.0F, 0.0F, 1.0F);
                int i2 = 1 + Math.round(f * (i - 1));
                if (this.selectedContent != null) {
                    collectorManager.setBundleEntryCount(this.selectedContent, i2);
                } else {
                    collectorManager.setSlotCount(this.selectedSlot, i2);
                }
            }
        }
    }

    private void startPriceEdit(CollectorManager collectorManager, AutoBuyItem autoBuyItem) {
        if (!this.editingPrice) {
            this.editingPrice = true;
            this.priceField.setText(collectorManager.hasPrice(autoBuyItem) ? String.valueOf(collectorManager.getPrice(autoBuyItem)) : "");
            this.priceField.setCursor(this.priceField.getText().length());
            this.priceField.clearSelection();
            this.priceField.setFocused(true);
        }
    }

    private void commitFieldEdits() {
        this.commitPriceEdit();
        this.commitCountEdit();
    }

    private void commitPriceEdit() {
        if (this.editingPrice) {
            CollectorManager collectorManager = this.getCollector();

            try {
                int i = Integer.parseInt(this.priceField.getText().trim());
                if (i > 0 && collectorManager != null && this.selectedSlot >= 0 && !collectorManager.getSlot(this.selectedSlot).isEmpty()) {
                    collectorManager.setPrice(this.selectedContent != null ? this.selectedContent.getItem() : collectorManager.getSlot(this.selectedSlot).getItem(), i);
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

    private void startCountEdit(int i) {
        if (!this.editingCount) {
            this.commitPriceEdit();
            this.editingCount = true;
            this.countField.setText(String.valueOf(i));
            this.countField.setCursor(this.countField.getText().length());
            this.countField.clearSelection();
            this.countField.setFocused(true);
        }
    }

    private void commitCountEdit() {
        if (this.editingCount) {
            CollectorManager collectorManager = this.getCollector();

            try {
                int i = Integer.parseInt(this.countField.getText().trim());
                if (collectorManager != null && this.selectedSlot >= 0 && !collectorManager.getSlot(this.selectedSlot).isEmpty()) {
                    if (this.selectedContent != null) {
                        collectorManager.setBundleEntryCount(this.selectedContent, i);
                    } else {
                        collectorManager.setSlotCount(this.selectedSlot, i);
                    }

                    collectorManager.save();
                }
            } catch (NumberFormatException numberFormatException) {
            }

            this.cancelCountEdit();
        }
    }

    private void cancelCountEdit() {
        this.editingCount = false;
        this.countField.setFocused(false);
        this.countField.setText("");
    }

    private CollectorManager getCollector() {
        return Sadnes.getInstance().getCollectorManager();
    }

    private static String formatNumber(long l) {
        return String.format(Locale.US, "%,d", l).replace(',', ' ');
    }

    private float getOpenProgress() {
        return (float)Math.pow(this.parentScreen.getOpenAnimation().get(), 3.0);
    }

    record ContentCell(CollectorManager.BundleEntry entry, float x, float y, float size) {
    }

    record EnchantRow(EnchantRequirement option, float y, float chipX) {
    }

    static final class FlyingItem {
        final AutoBuyItem item;
        final float startX;
        final float startY;
        final float endX;
        final float endY;
        final int targetSlot;
        final Animation progressAnimation = new Animation();

        FlyingItem(AutoBuyItem autoBuyItem, float f, float f2, float f3, float f4, int i) {
            this.item = autoBuyItem;
            this.startX = f;
            this.startY = f2;
            this.endX = f3;
            this.endY = f4;
            this.targetSlot = i;
            this.progressAnimation.animate(1.0, 0.2, Easings.QUART_OUT);
        }
    }

    record ItemRow(AutoBuyItem item, float y) {
    }

    record PresetRow(CollectorManager.Preset preset, float y) {
    }
}
