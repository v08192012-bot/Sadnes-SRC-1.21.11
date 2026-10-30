package tech.sadnes.client.ui.clickgui.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.neuro.NeuroAim;
import tech.sadnes.client.neuro.NeuroDebug;
import tech.sadnes.client.neuro.NeuroStorage;
import tech.sadnes.client.neuro.NeuroTrainer;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.setting.SettingWidgets;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.StopWatch;

public final class NeuroPanel extends Component {
    private static final float PADDING = 8.0F;
    private static final float HEADER_HEIGHT = 10.0F;
    private static final float HEADER_GAP = 10.0F;
    private static final float SIDEBAR_WIDTH = 80.0F;
    private static final float SIDEBAR_GAP = 8.0F;
    private static final float TAB_HEIGHT = 15.0F;
    private static final float SLIDER_HEIGHT = 3.0F;
    private static final float FIELD_HEIGHT = 16.0F;
    private static final float ROW_HEIGHT = 15.0F;
    private static final float TAB_SPACING = 18.0F;
    private static final float CLOSE_BUTTON_SIZE = 16.0F;
    private static final float ROW_GAP = 2.0F;
    private static final float CORNER_RADIUS = 6.0F;
    private static final int DOT_SIZE = 4;
    private static final int DANGER_COLOR = ColorUtil.rgb(255, 94, 94);
    private final Animation openAnimation = new Animation();
    private final Animation tabSwitchAnimation = new Animation();
    private final Animation closeHoverAnimation = new Animation();
    private final Animation recordingAnimation = new Animation();
    private final Animation recordHoverAnimation = new Animation();
    private final Animation eraseHoverAnimation = new Animation();
    private final Animation startHoverAnimation = new Animation();
    private final Animation cancelHoverAnimation = new Animation();
    private final Animation epochsDragAnimation = new Animation();
    private final Map<String, Animation> hoverAnimations = new HashMap<>();
    private final Map<String, Animation> selectAnimations = new HashMap<>();
    private final SmoothScrollbar datasetScroll = new SmoothScrollbar();
    private final SmoothScrollbar modelsScroll = new SmoothScrollbar();
    private final TextFieldComponent datasetNameField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "neuro.dataset.placeholder", false, false);
    private final TextFieldComponent modelNameField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "neuro.model.placeholder", false, false);
    private final TextFieldComponent renameField = new TextFieldComponent(Fonts.INTER_MEDIUM, 6.0F, "neuro.model.placeholder", false, false);
    private boolean open;
    private boolean closing;
    private NeuroPanel.Tab currentTab = NeuroPanel.Tab.RECORD;
    private String renamingName;
    private boolean renamingModel;
    private boolean renameFieldWasFocused;
    private boolean datasetFieldWasFocused;
    private boolean modelFieldWasFocused;
    private float epochs = 700.0F;
    private boolean draggingEpochs;
    private float epochsSliderX;
    private float epochsSliderWidth;
    private String selectedDataset;
    private String statusMessage;
    private boolean statusError;
    private boolean wasTraining;
    private boolean confirmClearModels;
    private boolean confirmErase;
    private final StopWatch clearConfirmTimer = new StopWatch();
    private final StopWatch eraseConfirmTimer = new StopWatch();
    private List<String> datasets = new ArrayList<>();
    private List<Integer> datasetSampleCounts = new ArrayList<>();
    private List<String> models = new ArrayList<>();
    private float displayedProgress;
    private String cachedSampleDataset;
    private int cachedSampleCount;
    private float contentX;
    private float contentY;
    private float contentWidth;
    private float contentHeight;
    private float closeButtonX;
    private float closeButtonY;
    private float tabsY;
    private float datasetListY;
    private float datasetListHeight;
    private float modelsListY;
    private float modelsListHeight;
    private final List<float[]> clickAreas = new ArrayList<>();
    private final List<Runnable> clickActions = new ArrayList<>();
    private final List<NeuroPanel.RenameTarget> renameTargets = new ArrayList<>();

    private NeuroAim getNeuro() {
        return NeuroAim.INSTANCE;
    }

    private NeuroStorage getDatasets() {
        return this.getNeuro().getStorage();
    }

    private NeuroTrainer getTrainer() {
        return this.getNeuro().getTrainer();
    }

    public NeuroPanel() {
        if (this.datasetNameField.getText().isEmpty()) {
            this.datasetNameField.setText("style");
        }
    }

    public boolean isOpen() {
        return this.open && !this.closing;
    }

    public boolean isVisible() {
        return this.openAnimation.get() > 0.001;
    }

    public boolean isMouseOver(double d, double d2) {
        return this.isVisible() && this.isHovered(d, d2, this.position.x, this.position.y, this.size.x, this.size.y);
    }

    public void openPanel() {
        this.open = true;
        this.closing = false;
        this.statusMessage = null;
        this.renamingName = null;
        this.draggingEpochs = false;
        this.confirmClearModels = false;
        this.confirmErase = false;
        this.invalidateSampleCount();
        this.refreshLists();
        this.hoverAnimations.clear();
        this.selectAnimations.clear();
        this.datasetScroll.reset();
        this.modelsScroll.reset();
        this.tabSwitchAnimation.forceValue(1.0);
        this.openAnimation.animate(1.0, 0.35, Easings.QUART_OUT);
    }

    @Override
    public void close() {
        if (this.open && !this.closing) {
            this.closing = true;
            this.unfocusFields();
            this.openAnimation.animate(0.0, 0.22, Easings.QUAD_IN);
        }
    }

    public void closeImmediately() {
        this.open = false;
        this.closing = false;
        this.unfocusFields();
        this.openAnimation.forceValue(0.0);
    }

    private void unfocusFields() {
        this.datasetNameField.setFocused(false);
        this.modelNameField.setFocused(false);
        this.renameField.setFocused(false);
        this.renamingName = null;
    }

    private void switchTab(NeuroPanel.Tab tab) {
        if (this.currentTab != tab) {
            this.currentTab = tab;
            this.statusMessage = null;
            this.unfocusFields();
            this.datasetScroll.reset();
            this.modelsScroll.reset();
            this.refreshLists();
            this.tabSwitchAnimation.forceValue(0.0);
            this.tabSwitchAnimation.animate(1.0, 0.3, Easings.QUAD_OUT);
        }
    }

    private void refreshLists() {
        this.datasets = this.getDatasets().listDatasets();
        this.datasetSampleCounts = new ArrayList<>();

        for (String string : this.datasets) {
            this.datasetSampleCounts.add(this.getDatasets().countDatasetSamples(string));
        }

        this.models = this.getDatasets().listModels();
        if (this.selectedDataset != null && !this.datasets.contains(this.selectedDataset)) {
            this.selectedDataset = null;
        }

        if (this.selectedDataset == null && !this.datasets.isEmpty()) {
            this.selectedDataset = this.datasets.get(0);
        }

        this.invalidateSampleCount();
    }

    private String sanitizeName(String string) {
        String string2 = string.replaceAll("[^a-zA-Z0-9_-]", "");
        return string2.isEmpty() ? "style" : string2;
    }

    private int getCurrentSampleCount() {
        String string = this.sanitizeName(this.datasetNameField.getText());
        if (!string.equals(this.cachedSampleDataset)) {
            this.cachedSampleDataset = string;
            this.cachedSampleCount = this.getDatasets().countDatasetSamples(string);
        }

        return this.cachedSampleCount;
    }

    private void invalidateSampleCount() {
        this.cachedSampleDataset = null;
    }

    public void renderPanel(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4, float f5) {
        this.openAnimation.update();
        this.tabSwitchAnimation.update();
        if (this.closing && this.openAnimation.isFinished() && this.openAnimation.get() <= 0.001) {
            this.open = false;
            this.closing = false;
        }

        if (this.open && !(this.openAnimation.get() <= 0.001)) {
            this.position.set(f2, f3);
            this.size.set(f4, f5);
            this.updateTrainingStatus();
            this.updateFieldFocus();
            if (this.confirmClearModels && this.clearConfirmTimer.hasPassed(3000.0)) {
                this.confirmClearModels = false;
            }

            if (this.confirmErase && this.eraseConfirmTimer.hasPassed(3000.0)) {
                this.confirmErase = false;
            }

            if (this.currentTab == NeuroPanel.Tab.DEBUG && !NeuroDebug.hasAccess()) {
                this.currentTab = NeuroPanel.Tab.RECORD;
            }

            float f6 = (float)Math.min(this.openAnimation.get(), 1.0);
            float f7 = f6 * this.getAlpha();
            RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, f4, f5);
            renderUtil.blur(f7, Radius.smoothAll(20.0F), RenderUtil.BlurQuality.LOW, true);
            renderUtil.rect(Theme.withAlpha(Theme.getBackground(), 0.4F), f7, Radius.smoothAll(20.0F));
            float f8 = f3 + 6.0F;
            this.closeButtonX = f2 + f4 - 5.0F - 22.0F + 6.0F;
            this.closeButtonY = f3 + 4.0F + 6.0F;
            this.tabsY = f8 + 10.0F + 6.0F;
            this.contentX = f2 + 8.0F + 80.0F + 8.0F;
            this.contentY = this.tabsY;
            this.contentWidth = f2 + f4 - 8.0F - this.contentX;
            this.contentHeight = f3 + f5 - 8.0F - this.contentY;
            this.clickAreas.clear();
            this.clickActions.clear();
            this.renameTargets.clear();
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f2 + f4 / 2.0F, f3 + f5 / 2.0F, 0.96F + 0.04F * f6);
            this.renderHeader(drawContext, f2, f8, i, i2, f7);
            this.renderSidebar(drawContext, f2, f3, f5, i, i2, f7);
            float f9 = (float)Math.min(this.tabSwitchAnimation.get(), 1.0);
            float f10 = (1.0F - f9) * 8.0F;
            float f11 = f7 * f9;
            switch (this.currentTab) {
                case RECORD:
                    this.renderRecordTab(drawContext, i, i2, this.contentX, this.contentY + f10, this.contentWidth, f11);
                    MatrixUtil.pop(drawContext);
                    return;
                case LEARN:
                    this.renderLearnTab(drawContext, i, i2, this.contentX, this.contentY + f10, this.contentWidth, f11);
                    MatrixUtil.pop(drawContext);
                    return;
                case MODELS:
                    this.renderModelsTab(drawContext, i, i2, this.contentX, this.contentY + f10, this.contentWidth, f11);
                    MatrixUtil.pop(drawContext);
                    return;
                case DEBUG:
                    this.renderDebugTab(drawContext, i, i2, this.contentX, this.contentY + f10, this.contentWidth, f11);
                default:
                    MatrixUtil.pop(drawContext);
            }
        }
    }

    private void updateTrainingStatus() {
        if (this.getTrainer().isTraining()) {
            this.wasTraining = true;
        } else if (this.wasTraining) {
            this.wasTraining = false;
            this.refreshLists();
            if (this.getTrainer().getError() != null) {
                this.statusMessage = this.getTrainer().getError();
                this.statusError = true;
            } else {
                if (this.getTrainer().getProgress() >= 1.0F) {
                    this.statusMessage = "neuro.status.trained";
                    this.statusError = false;
                }
            }
        }
    }

    private void updateFieldFocus() {
        boolean flag = this.datasetNameField.isFocused();
        if (this.datasetFieldWasFocused && !flag) {
            this.datasetNameField.setText(this.sanitizeName(this.datasetNameField.getText()));
            this.invalidateSampleCount();
        }

        this.datasetFieldWasFocused = flag;
        boolean flag2 = this.modelNameField.isFocused();
        if (this.modelFieldWasFocused && !flag2 && !this.modelNameField.getText().isEmpty()) {
            this.modelNameField.setText(this.sanitizeName(this.modelNameField.getText()));
        }

        this.modelFieldWasFocused = flag2;
        boolean flag3 = this.renameField.isFocused();
        if (this.renameFieldWasFocused && !flag3) {
            this.applyRename();
        }

        this.renameFieldWasFocused = flag3;
    }

    private void renderHeader(DrawContext drawContext, float f, float f2, int i, int i2, float f3) {
        int i3 = ColorUtil.multiplyAlpha(Theme.getPrimary(), f3);
        new RenderUtil(drawContext, f + 8.0F, f2, 80.0F + this.contentWidth - 20.0F, 10.0F)
            .icon(Icon.MICROCHIP, 2.0F, 1.5F, 7.0F, i3)
            .textMiddleLeft(Fonts.INTER_MEDIUM, Language.translate("neuro.title", "Нейро-наводка"), 12.0F, 0.0F, i3, 6.0F);
        float f4 = this.closeButtonX - 3.0F;
        float f5 = this.closeButtonY - 3.0F;
        this.closeHoverAnimation.update();
        this.closeHoverAnimation.animate(this.isHovered(i, i2, f4, f5, 16.0F, 16.0F) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f6 = this.closeHoverAnimation.get();
        GuiRenderUtil.drawHighlight(drawContext, f4, f5, 16.0F, 16.0F, f3 * f6, Radius.all(16.0F / 3.0F));
        new RenderUtil(drawContext, f4, f5, 16.0F, 16.0F)
            .icon(Icon.CLOSE, 16.0F / 2.0F - 5.0F, 16.0F / 2.0F - 5.0F, 10.0F, Theme.withAlpha(Theme.getSecondaryText(), (0.55F + 0.4F * f6) * f3));
        this.addClickArea(f4, f5, 16.0F, 16.0F, this::close);
    }

    private void renderSidebar(DrawContext drawContext, float f, float f2, float f3, int i, int i2, float f4) {
        float f5 = f + 8.0F;
        int i3 = NeuroDebug.hasAccess() ? NeuroPanel.Tab.VALUES.length : NeuroPanel.Tab.VALUES.length - 1;

        for (int i4 = 0; i4 < i3; i4++) {
            NeuroPanel.Tab tab = NeuroPanel.Tab.VALUES[i4];
            float f6 = this.tabsY + i4 * 18.0F;
            boolean flag = tab == this.currentTab;
            Animation animation = this.getAnimation(this.selectAnimations, "tab:" + tab.name());
            animation.update();
            animation.animate(flag ? 1.0 : 0.0, 0.5, flag ? Easings.QUART_OUT : Easings.QUAD_IN);
            Animation animation2 = this.getAnimation(this.hoverAnimations, "tab:" + tab.name());
            animation2.update();
            animation2.animate(this.isHovered(i, i2, f5, f6, 80.0, 15.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
            float f7 = animation.get();
            float f8 = Math.max(f7, animation2.get() * 0.5F);
            GuiRenderUtil.drawHighlight(drawContext, f5, f6, 80.0F, 15.0F, f8 * f4, Radius.all(6.0F));
            int i5 = Theme.withAlpha(Theme.getPrimary(), (0.6F + 0.4F * f7) * f4);
            int i6 = ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getTextColor(), 0.5F + 0.5F * f7), f4);
            new RenderUtil(drawContext, f5, f6, 80.0F, 15.0F)
                .icon(tab.icon, 4.0F, 3.5F, 8.0F, i5)
                .textMiddleLeft(Fonts.INTER_MEDIUM, this.truncate(tab.getDisplayName(), 60.0F, 6.5F), 16.0F, 0.0F, i6, 6.5F);
            this.addClickArea(f5, f6, 80.0F, 15.0F, () -> this.switchTab(tab));
        }

        boolean flag2 = this.getNeuro().getModelName() != null;
        String string = flag2 ? this.getNeuro().getModelName() : Language.translate("neuro.sidebar.noModel", "модель не выбрана");
        int i7 = flag2 ? Theme.getPrimary() : Theme.getSecondaryText();
        float f9 = f2 + f3 - 16.0F;
        new RenderUtil(drawContext, f5 + 2.0F, f9 + 2.5F, 4.0F, 4.0F).rect(Theme.withAlpha(i7, f4), Radius.all(2.0F));
        new RenderUtil(drawContext, f5 + 10.0F, f9, 70.0F, 9.0F)
            .textMiddleLeft(
                Fonts.INTER_MEDIUM,
                this.truncate(string, 68.0F, 5.5F),
                0.0F,
                0.0F,
                ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.9F), f4),
                5.5F
            );
    }

    private void renderRecordTab(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4) {
        this.renderSectionLabel(drawContext, f, f2, f3, Language.translate("neuro.trainer.dataset", "Датасет"), f4);
        float f5 = f2 + 12.0F;
        this.renderTextField(drawContext, this.datasetNameField, f, f5, 92.0F, f4);
        this.addClickArea(f, f5, 92.0F, 16.0F, () -> this.focusField(this.datasetNameField));
        int i3 = this.getCurrentSampleCount();
        String string = i3 + " " + Language.translate("neuro.unit.samples", "сэмплов");
        new RenderUtil(drawContext, f + 92.0F + 6.0F, f5, f3 - 92.0F - 6.0F, 16.0F)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, Theme.withAlpha(i3 >= 15000 ? Theme.getPrimary() : Theme.getSecondaryText(), f4), 6.0F);
        if (i3 > 0) {
            String string2 = this.confirmErase ? Language.translate("neuro.confirm", "Точно?") : Language.translate("neuro.trainer.erase", "Стереть");
            float f6 = Fonts.INTER_MEDIUM.getWidth(string2, 5.5F) + 14.0F;
            float f7 = f + f3 - f6;
            this.eraseHoverAnimation.update();
            this.eraseHoverAnimation.animate(this.isHovered(i, i2, f7, f5, f6, 16.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
            float f8 = this.eraseHoverAnimation.get();
            int i4 = this.confirmErase ? DANGER_COLOR : Theme.getSecondaryText();
            GuiRenderUtil.drawOutlined(drawContext, f7, f5, f6, 16.0F, f4 * (0.6F + 0.25F * f8), 5.0F)
                .textCenter(
                    Fonts.INTER_MEDIUM, string2, Theme.withAlpha(i4, (0.7F + 0.3F * Math.max(f8, this.confirmErase ? 1.0F : 0.0F)) * f4), 5.5F, false
                );
            this.addClickArea(f7, f5, f6, 16.0F, this::onEraseDatasetClicked);
        }

        boolean flag = this.getNeuro().getRecorder().isRecording();
        this.recordingAnimation.update();
        this.recordingAnimation.animate(flag ? 1.0 : 0.0, 0.3, Easings.EXPO_OUT);
        float f9 = (float)Math.min(this.recordingAnimation.get(), 1.0);
        float f10 = f5 + 16.0F + 8.0F;
        this.recordHoverAnimation.update();
        this.recordHoverAnimation.animate(this.isHovered(i, i2, f, f10, 112.0F, 16.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        GuiRenderUtil.drawOutlined(drawContext, f, f10, 112.0F, 16.0F, f4 * (0.6F + 0.25F * this.recordHoverAnimation.get()), 5.0F);
        if (f9 > 0.001F) {
            new RenderUtil(drawContext, f, f10, 112.0F, 16.0F).rect(Theme.withAlpha(Theme.getPrimary(), 0.15F * f9 * f4), Radius.all(5.0F));
        }

        int i5 = ColorUtil.interpolate(Theme.getSecondaryText(), DANGER_COLOR, f9);
        new RenderUtil(drawContext, f + 6.0F, f10 + 8.0F - 2.0F, 4.0F, 4.0F).rect(Theme.withAlpha(i5, f4), Radius.all(2.0F));
        new RenderUtil(drawContext, f + 14.0F, f10, 112.0F - 14.0F, 16.0F)
            .textMiddleLeft(
                Fonts.INTER_MEDIUM,
                Language.translate("neuro.trainer.combatRec", "Запись реального боя"),
                0.0F,
                0.0F,
                ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), f9), f4),
                6.0F
            );
        this.addClickArea(f, f10, 112.0F, 16.0F, this::onToggleRecordingClicked);
        if (flag && this.getNeuro().getRecorder().getDataset() != null) {
            new RenderUtil(drawContext, f + 112.0F + 8.0F, f10, f3 - 112.0F - 8.0F, 16.0F)
                .textMiddleLeft(
                    Fonts.INTER_MEDIUM,
                    String.valueOf(this.getNeuro().getRecorder().getDataset().getSampleCount()),
                    0.0F,
                    0.0F,
                    Theme.withAlpha(Theme.getPrimary(), f4),
                    6.0F
                );
        }

        String string3 = flag
            ? Language.translate("neuro.trainer.recBlock", "Пока идёт запись, Trained не водит — целься вручную. Активация модели остановит запись.")
            : Language.translate("neuro.trainer.hint", "Играй с той же сенсой и DPI, что и в бою — модель выучит твой стиль. Цель: 15–30 тыс. сэмплов.");
        new RenderUtil(drawContext, f, f10 + 16.0F + 8.0F, f3, 24.0F)
            .scrollingTextTop(Fonts.INTER_REGULAR, string3, 0.0F, 0.0F, f3, Theme.withAlpha(Theme.getSecondaryText(), 0.65F * f4), 5.5F);
    }

    private void renderLearnTab(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4) {
        this.renderSectionLabel(drawContext, f, f2, f3, Language.translate("neuro.learn.datasets", "Датасеты"), f4);
        this.datasetListY = f2 + 12.0F;
        this.datasetListHeight = 66.0F;
        if (this.datasets.isEmpty()) {
            new RenderUtil(drawContext, f, this.datasetListY + 2.0F, f3, 20.0F)
                .scrollingTextTop(
                    Fonts.INTER_REGULAR,
                    Language.translate("neuro.learn.noDatasets", "Пусто. Сначала запиши живой бой."),
                    0.0F,
                    0.0F,
                    f3,
                    Theme.withAlpha(Theme.getSecondaryText(), 0.6F * f4),
                    5.5F
                );
        } else {
            this.renderDatasetList(drawContext, i, i2, f, f3, f4);
        }

        float f5 = this.datasetListY + this.datasetListHeight + 8.0F;
        this.renderSectionLabel(drawContext, f, f5, f3, Language.translate("neuro.learn.modelName", "Имя модели"), f4);
        float f6 = f5 + 12.0F;
        this.renderTextField(drawContext, this.modelNameField, f, f6, f3, f4);
        this.addClickArea(f, f6, f3, 16.0F, () -> this.focusField(this.modelNameField));
        float f7 = f6 + 16.0F + 8.0F;
        this.renderSectionLabel(drawContext, f, f7, f3, Language.translate("neuro.learn.epochs", "Эпохи"), f4);
        new RenderUtil(drawContext, f, f7, f3, 9.0F)
            .textTopRight(Fonts.INTER_MEDIUM, String.valueOf((int)this.epochs), 0.0F, 0.0F, Theme.withAlpha(Theme.getPrimary(), f4), 6.0F);
        float f8 = f7 + 13.0F;
        this.epochsSliderX = f;
        this.epochsSliderWidth = f3;
        this.epochsDragAnimation.update();
        this.epochsDragAnimation.animate(this.draggingEpochs ? 1.0 : 0.0, 0.2, Easings.QUAD_OUT);
        SettingWidgets.drawSlider(
            drawContext, f, f8, f3, 3.0F, (this.epochs - 100.0F) / 1400.0F, 6.0F, (float)Math.min(this.epochsDragAnimation.get(), 1.0), f4
        );
        this.addClickArea(f - 3.0F, f8 - 5.0F, f3 + 6.0F, 13.0F, () -> this.draggingEpochs = true);
        new RenderUtil(drawContext, f, f8 + 8.0F, f3, 8.0F)
            .textTopLeft(
                Fonts.INTER_REGULAR,
                Language.translate("neuro.learn.epochsHint", "600–800 — рабочая точка"),
                0.0F,
                0.0F,
                Theme.withAlpha(Theme.getSecondaryText(), 0.6F * f4),
                5.5F
            );
        float f9 = f8 + 24.0F;
        if (!this.getTrainer().isTraining()) {
            this.renderStartButton(drawContext, i, i2, f, f9, f3, f4);
        } else {
            this.renderTrainingProgress(drawContext, i, i2, f, f9, f3, f4);
        }

        if (this.statusMessage != null) {
            RenderUtil renderUtil = new RenderUtil(drawContext, f, this.contentY + this.contentHeight - 16.0F, f3, 16.0F);
            MsdfFont msdfFont = Fonts.INTER_MEDIUM;
            String string = Language.translate(this.statusMessage);
            if (this.statusError) {
                renderUtil.scrollingTextTop(msdfFont, string, 0.0F, 0.0F, f3, Theme.withAlpha(DANGER_COLOR, f4), 5.5F);
                return;
            }

            renderUtil.scrollingTextTop(msdfFont, string, 0.0F, 0.0F, f3, Theme.withAlpha(Theme.getPrimary(), f4), 5.5F);
        }
    }

    private void renderDatasetList(DrawContext drawContext, int i, int i2, float f, float f2, float f3) {
        int i3 = this.datasets.size();
        float f4 = i3 * 17.0F - 2.0F;
        float f5 = f2 - 6.0F;
        this.datasetScroll.update(f4, this.datasetListHeight);
        this.datasetScroll.updateHover(i, i2);
        if (f4 > this.datasetListHeight) {
            this.datasetScroll.render(drawContext, f + f2 - 2.5F, this.datasetListY, this.datasetListHeight, f4, f3);
        }

        boolean flag = ScissorStack.push(drawContext, f - 2.0F, this.datasetListY, f2 + 4.0F, this.datasetListHeight);
        if (flag) {
            try {
                float f6 = this.datasetListY + this.datasetScroll.getScroll();

                for (int i4 = 0; i4 < i3; i4++) {
                    String string = this.datasets.get(i4);
                    if (f6 + 15.0F >= this.datasetListY && f6 <= this.datasetListY + this.datasetListHeight) {
                        this.renderDatasetEntry(drawContext, i, i2, string, this.datasetSampleCounts.get(i4), f, f6, f5, f3);
                        this.addClippedClickArea(this.datasetListY, this.datasetListHeight, f, f6, f5, 15.0F, () -> {
                            this.selectedDataset = string;
                            if (this.modelNameField.getText().isEmpty()) {
                                this.modelNameField.setText(string);
                            }
                        });
                    }

                    f6 += 17.0F;
                }
            } finally {
                ScissorStack.pop(drawContext);
            }
        }
    }

    private void renderDatasetEntry(DrawContext drawContext, int i, int i2, String string, int i3, float f, float f2, float f3, float f4) {
        boolean flag = string.equals(this.selectedDataset);
        Animation animation = this.getAnimation(this.selectAnimations, "ds:" + string);
        animation.update();
        animation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        Animation animation2 = this.getAnimation(this.hoverAnimations, "ds:" + string);
        animation2.update();
        animation2.animate(this.isHovered(i, i2, f, f2, f3, 15.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f5 = animation.get();
        GuiRenderUtil.drawOutlined(drawContext, f, f2, f3, 15.0F, f4 * (0.35F + 0.2F * animation2.get() + 0.3F * f5), 5.0F);
        if (f5 > 0.001F) {
            new RenderUtil(drawContext, f, f2, f3, 15.0F).rect(Theme.withAlpha(Theme.getPrimary(), 0.12F * f5 * f4), Radius.all(5.0F));
        }

        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, 15.0F)
            .textMiddleLeft(
                Fonts.INTER_MEDIUM,
                this.truncate(string, f3 - 40.0F, 6.0F),
                6.0F,
                0.0F,
                ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), f5), f4),
                6.0F
            );
        MsdfFont msdfFont = Fonts.INTER_MEDIUM;
        String string2 = formatCount(i3);
        if (flag) {
            renderUtil.textMiddleRight(msdfFont, string2, 5.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getPrimary(), 0.8F), f4), 5.5F);
        } else {
            renderUtil.textMiddleRight(msdfFont, string2, 5.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.8F), f4), 5.5F);
        }
    }

    private void renderStartButton(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4) {
        boolean flag = this.selectedDataset != null;
        this.startHoverAnimation.update();
        this.startHoverAnimation.animate(flag && this.isHovered(i, i2, f, f2, f3, 18.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f5 = this.startHoverAnimation.get();
        int i3 = flag
            ? ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getPrimary(), ColorUtil.brighten(Theme.getPrimary(), 0.92F), f5), f4)
            : ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSurface(), 0.5F), f4);
        new RenderUtil(drawContext, f, f2, f3, 18.0F)
            .rect(i3, Radius.all(6.0F))
            .textCenter(
                Fonts.INTER_SEMIBOLD,
                Language.translate("neuro.learn.start", "Начать обучение"),
                flag ? ColorUtil.multiplyAlpha(Theme.getTextColor(), f4) : ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.5F), f4),
                6.5F,
                false
            );
        if (flag) {
            this.addClickArea(f, f2, f3, 18.0F, this::onStartTrainingClicked);
        }
    }

    private void renderTrainingProgress(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4) {
        this.displayedProgress = this.displayedProgress + (this.getTrainer().getProgress() - this.displayedProgress) * 0.12F;
        String string = Math.round(this.displayedProgress * 100.0F)
            + "%   ·   loss "
            + String.format("%.3f", this.getTrainer().getLoss())
            + "   ·   ~"
            + formatDuration(this.getTrainer().getEtaMillis());
        new RenderUtil(drawContext, f, f2, f3, 9.0F)
            .textTopLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f4), 6.0F);
        float f5 = f2 + 12.0F;
        new RenderUtil(drawContext, f, f5, f3, 3.0F).rect(ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSurface(), 0.8F), f4), Radius.all(1.5F));
        new RenderUtil(drawContext, f, f5, f3 * MathHelper.clamp(this.displayedProgress, 0.0F, 1.0F), 3.0F)
            .rect(Theme.withAlpha(Theme.getPrimary(), f4), Radius.all(1.5F));
        float f6 = f5 + 9.0F;
        this.cancelHoverAnimation.update();
        this.cancelHoverAnimation.animate(this.isHovered(i, i2, f, f6, 64.0, 15.0) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        GuiRenderUtil.drawOutlined(drawContext, f, f6, 64.0F, 15.0F, f4 * (0.6F + 0.25F * this.cancelHoverAnimation.get()), 5.0F)
            .textCenter(
                Fonts.INTER_MEDIUM, Language.translate("neuro.learn.cancel", "Отменить"), ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f4), 6.0F, false
            );
        this.addClickArea(f, f6, 64.0F, 15.0F, this.getTrainer()::cancel);
    }

    private void renderModelsTab(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4) {
        this.renderSectionLabel(drawContext, f, f2, f3, Language.translate("neuro.models.title", "Модели"), f4);
        if (!this.models.isEmpty()) {
            String string = this.confirmClearModels ? Language.translate("neuro.confirm", "Точно?") : Language.translate("neuro.models.clearAll", "Очистить всё");
            float f5 = Fonts.INTER_MEDIUM.getWidth(string, 5.5F) + 8.0F;
            boolean flag = this.isHovered(i, i2, f + f3 - f5, f2 - 2.0F, f5, 12.0);
            int i3 = this.confirmClearModels ? DANGER_COLOR : Theme.getSecondaryText();
            new RenderUtil(drawContext, f + f3 - f5, f2 - 2.0F, f5, 12.0F)
                .textMiddleRight(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, Theme.withAlpha(i3, (!flag && !this.confirmClearModels ? 0.6F : 1.0F) * f4), 5.5F);
            this.addClickArea(f + f3 - f5, f2 - 2.0F, f5, 12.0F, this::onClearModelsClicked);
        }

        this.modelsListY = f2 + 12.0F;
        this.modelsListHeight = this.contentY + this.contentHeight - this.modelsListY;
        float f6 = f3 - 6.0F;
        float f7 = this.getModelsContentHeight();
        this.modelsScroll.update(f7, this.modelsListHeight);
        this.modelsScroll.updateHover(i, i2);
        if (f7 > this.modelsListHeight) {
            this.modelsScroll.render(drawContext, f + f3 - 2.5F, this.modelsListY, this.modelsListHeight, f7, f4);
        }

        boolean flag2 = ScissorStack.push(drawContext, f - 2.0F, this.modelsListY, f3 + 4.0F, this.modelsListHeight);
        if (flag2) {
            try {
                float f8 = this.modelsListY + this.modelsScroll.getScroll();
                if (this.models.isEmpty()) {
                    new RenderUtil(drawContext, f, f8 + 2.0F, f3, 16.0F)
                        .scrollingTextTop(
                            Fonts.INTER_REGULAR,
                            Language.translate("neuro.models.empty", "Моделей нет — обучи первую во вкладке «Обучение»."),
                            0.0F,
                            0.0F,
                            f3,
                            Theme.withAlpha(Theme.getSecondaryText(), 0.6F * f4),
                            5.5F
                        );
                    f8 += 22.0F;
                }

                for (String string2 : this.models) {
                    if (this.isInModelsView(f8, 18.0F)) {
                        this.renderModelEntry(drawContext, i, i2, string2, f, f8, f6, f4);
                    }

                    f8 += 20.0F;
                }

                float f9 = f8 + 6.0F;
                if (this.isInModelsView(f9, 9.0F)) {
                    this.renderSectionLabel(drawContext, f, f9, f6, Language.translate("neuro.models.datasets", "Датасеты"), f4);
                }

                float f10 = f9 + 12.0F;
                int i4 = this.datasets.size();

                for (int i5 = 0; i5 < i4; i5++) {
                    if (this.isInModelsView(f10, 16.0F)) {
                        this.renderDatasetRow(drawContext, i, i2, this.datasets.get(i5), this.datasetSampleCounts.get(i5), f, f10, f6, f4);
                    }

                    f10 += 18.0F;
                }
            } finally {
                ScissorStack.pop(drawContext);
            }
        }
    }

    private float getModelsContentHeight() {
        float f = this.models.isEmpty() ? 22.0F : this.models.size() * 20.0F;
        return f + 18.0F + this.datasets.size() * 18.0F;
    }

    private boolean isInModelsView(float f, float f2) {
        return f + f2 >= this.modelsListY && f <= this.modelsListY + this.modelsListHeight;
    }

    private void renderModelEntry(DrawContext drawContext, int i, int i2, String string, float f, float f2, float f3, float f4) {
        boolean flag = string.equals(this.getNeuro().getModelName());
        Animation animation = this.getAnimation(this.selectAnimations, "md:" + string);
        animation.update();
        animation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        Animation animation2 = this.getAnimation(this.hoverAnimations, "md:" + string);
        animation2.update();
        boolean flag2 = this.isHovered(i, i2, f, f2, f3, 18.0);
        animation2.animate(flag2 ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        float f5 = animation.get();
        GuiRenderUtil.drawOutlined(drawContext, f, f2, f3, 18.0F, f4 * (0.35F + 0.2F * animation2.get() + 0.3F * f5), 6.0F);
        if (f5 > 0.001F) {
            new RenderUtil(drawContext, f, f2, f3, 18.0F).rect(Theme.withAlpha(Theme.getPrimary(), 0.12F * f5 * f4), Radius.all(6.0F));
            new RenderUtil(drawContext, f + 6.0F, f2 + 9.0F - 2.0F, 4.0F, 4.0F).rect(Theme.withAlpha(Theme.getPrimary(), f5 * f4), Radius.all(2.0F));
        }

        boolean flag3 = this.isRenaming(string, true);
        float f6 = f + 6.0F + 8.0F * f5;
        if (flag3) {
            this.renderRenameField(drawContext, f6, f2 + 2.0F, 100.0F, 14.0F, f4);
        } else {
            new RenderUtil(drawContext, f6, f2, f3 - 60.0F, 18.0F)
                .textMiddleLeft(
                    Fonts.INTER_MEDIUM,
                    this.truncate(string, f3 - 66.0F, 6.0F),
                    0.0F,
                    0.0F,
                    ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), f5), f4),
                    6.0F
                );
            this.addRenameTarget(f, f2, f3 - 24.0F, 18.0F, string, true);
        }

        boolean flag4 = this.isHovered(i, i2, f + f3 - 20.0F, f2 + 2.0F, 14.0, 14.0);
        if (!flag3 && (flag || flag2 && !flag4)) {
            new RenderUtil(drawContext, f, f2, f3 - 26.0F, 18.0F)
                .textMiddleRight(
                    Fonts.INTER_MEDIUM,
                    flag ? Language.translate("neuro.models.active", "активна") : Language.translate("neuro.models.use", "включить"),
                    0.0F,
                    0.0F,
                    Theme.withAlpha(flag ? Theme.getPrimary() : Theme.getSecondaryText(), 0.85F * f4),
                    5.5F
                );
        }

        new RenderUtil(drawContext, f + f3 - 20.0F, f2 + 2.0F, 14.0F, 14.0F)
            .icon(Icon.TRASH, 3.0F, 3.0F, 8.0F, Theme.withAlpha(flag4 ? DANGER_COLOR : Theme.getSecondaryText(), (flag4 ? 1.0F : 0.5F) * f4));
        this.addClippedClickArea(this.modelsListY, this.modelsListHeight, f + f3 - 20.0F, f2 + 2.0F, 14.0F, 14.0F, () -> {
            this.getNeuro().deleteModel(string);
            this.refreshLists();
        });
        if (!flag3) {
            this.addClippedClickArea(this.modelsListY, this.modelsListHeight, f, f2, f3 - 24.0F, 18.0F, () -> {
                if (flag) {
                    this.getNeuro().unloadModel();
                } else {
                    if (!this.getNeuro().loadModel(string)) {
                        this.statusMessage = "neuro.error.modelLoad";
                        this.statusError = true;
                    }
                }
            });
        }
    }

    private void renderDatasetRow(DrawContext drawContext, int i, int i2, String string, int i3, float f, float f2, float f3, float f4) {
        GuiRenderUtil.drawOutlined(drawContext, f, f2, f3, 16.0F, f4 * 0.35F, 5.0F);
        if (this.isRenaming(string, false)) {
            this.renderRenameField(drawContext, f + 5.0F, f2 + 1.5F, 100.0F, 13.0F, f4);
        } else {
            new RenderUtil(drawContext, f, f2, f3 - 60.0F, 16.0F)
                .textMiddleLeft(
                    Fonts.INTER_MEDIUM, this.truncate(string, f3 - 66.0F, 6.0F), 6.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f4), 6.0F
                );
            this.addRenameTarget(f, f2, f3 - 24.0F, 16.0F, string, false);
        }

        new RenderUtil(drawContext, f, f2, f3 - 26.0F, 16.0F)
            .textMiddleRight(
                Fonts.INTER_MEDIUM, formatCount(i3), 0.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.withAlpha(Theme.getSecondaryText(), 0.7F), f4), 5.5F
            );
        boolean flag = this.isHovered(i, i2, f + f3 - 20.0F, f2 + 1.0F, 14.0, 14.0);
        new RenderUtil(drawContext, f + f3 - 20.0F, f2 + 1.0F, 14.0F, 14.0F)
            .icon(Icon.TRASH, 3.0F, 3.0F, 8.0F, Theme.withAlpha(flag ? DANGER_COLOR : Theme.getSecondaryText(), (flag ? 1.0F : 0.5F) * f4));
        this.addClippedClickArea(this.modelsListY, this.modelsListHeight, f + f3 - 20.0F, f2 + 1.0F, 14.0F, 14.0F, () -> {
            this.getDatasets().deleteDataset(string);
            this.refreshLists();
        });
    }

    private void renderDebugTab(DrawContext drawContext, int i, int i2, float f, float f2, float f3, float f4) {
        NeuroDebug neuroDebug = this.getNeuro().getDebug();
        float f5 = f3 / 2.0F;
        this.renderCheckbox(drawContext, f, f2, Language.translate("neuro.debug.optimalPoint", "Оптимальный поинт"), neuroDebug.isShowAimPoint(), f4, () -> {
            if (!neuroDebug.isShowAimPoint()) {
                neuroDebug.setShowAimPoint(true);
            } else {
                neuroDebug.setShowAimPoint(false);
            }
        });
        this.renderCheckbox(drawContext, f + f5, f2, Language.translate("neuro.debug.trail", "Трейл прицела"), neuroDebug.isShowTrail(), f4, () -> {
            if (!neuroDebug.isShowTrail()) {
                neuroDebug.setShowTrail(true);
            } else {
                neuroDebug.setShowTrail(false);
            }
        });
        float f6 = f2 + 13.0F;
        this.renderCheckbox(drawContext, f, f6, Language.translate("neuro.debug.highlight", "Подсветка цели"), neuroDebug.isShowHitbox(), f4, () -> {
            if (!neuroDebug.isShowHitbox()) {
                neuroDebug.setShowHitbox(true);
            } else {
                neuroDebug.setShowHitbox(false);
            }
        });
        this.renderCheckbox(drawContext, f + f5, f6, Language.translate("neuro.debug.hud", "Оверлей в игре"), neuroDebug.isShowDebugHud(), f4, () -> {
            if (!neuroDebug.isShowDebugHud()) {
                neuroDebug.setShowDebugHud(true);
            } else {
                neuroDebug.setShowDebugHud(false);
            }
        });
        float f7 = f2 + 26.0F;
        this.renderCheckbox(drawContext, f, f7, Language.translate("neuro.debug.fileLog", "Лог в файл"), neuroDebug.isCsvLogging(), f4, () -> {
            if (!neuroDebug.isCsvLogging()) {
                neuroDebug.setCsvLogging(true);
            } else {
                neuroDebug.setCsvLogging(false);
            }
        });
        if (neuroDebug.getSampleCount() == 0) {
            new RenderUtil(drawContext, f, f7 + 18.0F, f3, 20.0F)
                .scrollingTextTop(
                    Fonts.INTER_REGULAR,
                    Language.translate("neuro.debug.noData", "Нет данных — войди в бой с активной моделью."),
                    0.0F,
                    0.0F,
                    f3,
                    Theme.withAlpha(Theme.getSecondaryText(), 0.65F * f4),
                    5.5F
                );
        } else {
            new RenderUtil(drawContext, f, f7 + 13.0F, f3, 8.0F)
                .textTopLeft(Fonts.INTER_MEDIUM, neuroDebug.buildSummaryLine(), 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), 0.85F * f4), 5.0F);
            float f8 = f7 + 24.0F;
            neuroDebug.drawGraph(drawContext, f, f8, f3, 36.0F, Language.translate("neuro.debug.deltaYaw", "Дельта yaw"), 0, 2, 4, 21, 3.0F, 180, f4);
            float f9 = f8 + 51.0F;
            neuroDebug.drawGraph(drawContext, f, f9, f3, 36.0F, Language.translate("neuro.debug.deltaPitch", "Дельта pitch"), 1, 3, 5, 42, 3.0F, 180, f4);
            float f10 = f9 + 51.0F;
            neuroDebug.drawGraph(drawContext, f, f10, f3, 36.0F, Language.translate("neuro.debug.error", "Ошибка наводки"), 6, 7, -1, 0, 10.0F, 180, f4);
            float f11 = f10 + 51.0F;
            int i3 = Theme.withAlpha(Theme.getSecondaryText(), 0.85F * f4);
            new RenderUtil(drawContext, f, f11, f3, 8.0F).textTopLeft(Fonts.INTER_MEDIUM, neuroDebug.buildStatsLine(), 0.0F, 0.0F, i3, 5.0F);
            new RenderUtil(drawContext, f, f11 + 8.0F, f3, 8.0F).textTopLeft(Fonts.INTER_MEDIUM, neuroDebug.formatStats("yaw", 2), 0.0F, 0.0F, i3, 5.0F);
            new RenderUtil(drawContext, f, f11 + 16.0F, f3, 8.0F).textTopLeft(Fonts.INTER_MEDIUM, neuroDebug.formatStats("pitch", 3), 0.0F, 0.0F, i3, 5.0F);
        }
    }

    private void renderCheckbox(DrawContext drawContext, float f, float f2, String string, boolean flag, float f3, Runnable runnable) {
        SettingWidgets.drawCheckbox(drawContext, f, f2, 8.0F, flag ? 1.0F : 0.0F, f3);
        float f4 = Fonts.INTER_MEDIUM.getWidth(string, 6.0F);
        new RenderUtil(drawContext, f + 12.0F, f2, f4, 8.0F)
            .textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, Theme.withAlpha(flag ? Theme.getTextColor() : Theme.getSecondaryText(), f3), 6.0F);
        this.addClickArea(f - 2.0F, f2 - 3.0F, 14.0F + f4, 14.0F, runnable);
    }

    private void renderSectionLabel(DrawContext drawContext, float f, float f2, float f3, String string, float f4) {
        new RenderUtil(drawContext, f, f2, f3, 9.0F).textTopLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, Theme.withAlpha(Theme.getSecondaryText(), f4), 6.0F);
    }

    private void renderTextField(DrawContext drawContext, TextFieldComponent textFieldComponent, float f, float f2, float f3, float f4) {
        GuiRenderUtil.drawOutlined(drawContext, f, f2, f3, 16.0F, f4 * (textFieldComponent.isFocused() ? 0.9F : 0.7F), 5.0F);
        textFieldComponent.render(drawContext, new Vector2f(f + 6.0F, f2 + 8.0F - 3.0F), ColorUtil.multiplyAlpha(Theme.getTextColor(), f4), f3 - 12.0F);
    }

    private void focusField(TextFieldComponent textFieldComponent) {
        if (!textFieldComponent.isFocused()) {
            textFieldComponent.setFocused(true);
            textFieldComponent.setCursor(textFieldComponent.getText().length());
        }
    }

    private boolean isRenaming(String string, boolean flag) {
        return this.renameField.isFocused() && string.equals(this.renamingName) && this.renamingModel == flag;
    }

    private void renderRenameField(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        GuiRenderUtil.drawOutlined(drawContext, f, f2, f3, f4, f5 * 0.95F, 4.0F).shadow(0.5F, Theme.withAlpha(Theme.getPrimary(), f5), Radius.all(4.0F));
        this.renameField
            .render(drawContext, new Vector2f(f + 5.0F, f2 + (f4 - 6.0F) / 2.0F), ColorUtil.multiplyAlpha(Theme.getTextColor(), f5), f3 - 10.0F);
    }

    private void startRename(String string, boolean flag) {
        this.renamingName = string;
        this.renamingModel = flag;
        this.renameField.setText(string);
        this.renameField.setFocused(true);
        this.renameField.selectAll();
    }

    private void applyRename() {
        if (this.renamingName != null) {
            String string = this.renamingName;
            this.renamingName = null;
            String string2 = this.sanitizeName(this.renameField.getText());
            if (!string2.equals(string)) {
                if (this.renamingModel) {
                    this.getNeuro().renameModel(string, string2);
                    this.refreshLists();
                } else {
                    if (!this.getNeuro().getRecorder().isRecordingDataset(string)
                        && this.getDatasets().renameDataset(string, string2)
                        && string.equals(this.selectedDataset)) {
                        this.selectedDataset = string2;
                    }

                    this.refreshLists();
                }
            }
        }
    }

    private void onClearModelsClicked() {
        if (!this.confirmClearModels) {
            this.confirmClearModels = true;
            this.clearConfirmTimer.reset();
        } else {
            this.confirmClearModels = false;

            for (String string : new ArrayList<>(this.models)) {
                this.getNeuro().deleteModel(string);
            }

            this.refreshLists();
        }
    }

    private void onEraseDatasetClicked() {
        if (!this.confirmErase) {
            this.confirmErase = true;
            this.eraseConfirmTimer.reset();
        } else {
            this.confirmErase = false;
            String string = this.sanitizeName(this.datasetNameField.getText());
            if (this.getNeuro().getRecorder().isRecordingDataset(string)) {
                this.getNeuro().getRecorder().cancelRecording();
            }

            this.getDatasets().deleteDataset(string);
            this.refreshLists();
        }
    }

    private void onToggleRecordingClicked() {
        if (this.getNeuro().getRecorder().isRecording()) {
            this.getNeuro().getRecorder().stopRecording();
            this.invalidateSampleCount();
        } else {
            this.getNeuro().getRecorder().startRecording(this.sanitizeName(this.datasetNameField.getText()));
        }
    }

    private void onStartTrainingClicked() {
        if (this.selectedDataset != null && !this.getTrainer().isTraining()) {
            String string = this.modelNameField.getText().isEmpty() ? this.selectedDataset : this.sanitizeName(this.modelNameField.getText());
            String string2 = this.selectedDataset;
            this.statusMessage = null;
            this.displayedProgress = 0.0F;
            this.getTrainer().startTraining(string2, (int)this.epochs, string, mixtureDensityNetwork -> this.getNeuro().setModel(mixtureDensityNetwork, string));
        }
    }

    private void addClickArea(float f, float f2, float f3, float f4, Runnable runnable) {
        this.clickAreas.add(new float[]{f, f2, f3, f4});
        this.clickActions.add(runnable);
    }

    private void addClippedClickArea(float f, float f2, float f3, float f4, float f5, float f6, Runnable runnable) {
        float f7 = Math.max(f4, f);
        float f8 = Math.min(f4 + f6, f + f2);
        if (f8 - f7 > 1.0F) {
            this.addClickArea(f3, f7, f5, f8 - f7, runnable);
        }
    }

    private void addRenameTarget(float f, float f2, float f3, float f4, String string, boolean flag) {
        float f5 = Math.max(f2, this.modelsListY);
        float f6 = Math.min(f2 + f4, this.modelsListY + this.modelsListHeight);
        if (f6 - f5 > 1.0F) {
            this.renameTargets.add(new NeuroPanel.RenameTarget(f, f5, f3, f6 - f5, string, flag));
        }
    }

    private Animation getAnimation(Map<String, Animation> map, String string) {
        return map.computeIfAbsent(string, string2 -> new Animation());
    }

    private static String formatCount(int i) {
        return i >= 1000 ? String.format("%.1fk", i / 1000.0F) : String.valueOf(i);
    }

    private static String formatDuration(long l) {
        long l2 = Math.max(0L, l) / 1000L;
        return l2 < 60L ? l2 + "с" : l2 / 60L + "м " + l2 % 60L + "с";
    }

    private String truncate(String string, float f, float f2) {
        if (Fonts.INTER_MEDIUM.getWidth(string, f2) <= f) {
            return string;
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < string.length() && !(Fonts.INTER_MEDIUM.getWidth(builder.toString() + string.charAt(i) + "…", f2) > f); i++) {
            builder.append(string.charAt(i));
        }

        return builder + "…";
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (!this.open) {
            return false;
        }

        double d = click.x();
        double d2 = click.y();
        if (this.currentTab == NeuroPanel.Tab.RECORD) {
            this.datasetNameField.mouseClicked(d, d2, click.button());
        }

        if (this.currentTab == NeuroPanel.Tab.LEARN) {
            this.modelNameField.mouseClicked(d, d2, click.button());
        }

        if (this.renamingName != null) {
            this.renameField.mouseClicked(d, d2, click.button());
        }

        if (!this.isLeftButton(click.button())) {
            return this.isMouseOver(d, d2);
        }

        if (flag) {
            for (NeuroPanel.RenameTarget renameTarget : this.renameTargets) {
                if (this.isHovered(d, d2, renameTarget.x(), renameTarget.y(), renameTarget.w(), renameTarget.h())) {
                    this.startRename(renameTarget.name(), renameTarget.model());
                    return true;
                }
            }
        }

        if (this.currentTab == NeuroPanel.Tab.LEARN && this.datasetScroll.mouseClicked(d, d2)) {
            return true;
        }

        if (this.currentTab == NeuroPanel.Tab.MODELS && this.modelsScroll.mouseClicked(d, d2)) {
            return true;
        }

        int i = this.clickAreas.size();

        for (int i2 = 0; i2 < i; i2++) {
            float[] floats = this.clickAreas.get(i2);
            if (this.isHovered(d, d2, floats[0], floats[1], floats[2], floats[3])) {
                this.clickActions.get(i2).run();
                if (this.draggingEpochs) {
                    this.updateEpochsFromMouse(d);
                }

                return true;
            }
        }

        return this.isMouseOver(d, d2);
    }

    @Override
    public boolean onMouseRelease(Click click) {
        this.draggingEpochs = false;
        this.datasetScroll.mouseReleased();
        this.modelsScroll.mouseReleased();
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.draggingEpochs) {
            this.updateEpochsFromMouse(click.x());
            return true;
        } else {
            return this.datasetScroll.isDragging() && this.datasetScroll.mouseDragged(click.x(), click.y())
                ? true
                : this.modelsScroll.isDragging() && this.modelsScroll.mouseDragged(click.x(), click.y());
        }
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (!this.open) {
            return false;
        } else if (this.currentTab == NeuroPanel.Tab.LEARN
            && this.isHovered(d, d2, this.contentX, this.datasetListY, this.contentWidth, this.datasetListHeight)) {
            this.datasetScroll.onScroll(d4);
            return true;
        } else if (this.currentTab == NeuroPanel.Tab.MODELS
            && this.isHovered(d, d2, this.contentX, this.modelsListY, this.contentWidth, this.modelsListHeight)) {
            this.modelsScroll.onScroll(d4);
            return true;
        } else {
            return this.isMouseOver(d, d2);
        }
    }

    private void updateEpochsFromMouse(double d) {
        float f = MathHelper.clamp((float)(d - this.epochsSliderX) / this.epochsSliderWidth, 0.0F, 1.0F);
        this.epochs = Math.round((100.0F + f * 1400.0F) / 50.0F) * 50.0F;
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (!this.open) {
            return false;
        }

        int i = keyInput.key();
        if (i == 256) {
            if (this.renameField.isFocused()) {
                this.renamingName = null;
                this.renameField.setFocused(false);
                return true;
            } else if (!this.datasetNameField.isFocused() && !this.modelNameField.isFocused()) {
                this.close();
                return true;
            } else {
                this.datasetNameField.setFocused(false);
                this.modelNameField.setFocused(false);
                return true;
            }
        } else if (this.renameField.isFocused()) {
            this.renameField.keyPressed(i);
            return true;
        } else if (this.datasetNameField.isFocused()) {
            this.datasetNameField.keyPressed(i);
            return true;
        } else if (this.modelNameField.isFocused()) {
            this.modelNameField.keyPressed(i);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        return false;
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        if (!this.open) {
            return false;
        } else if (this.renameField.isFocused()) {
            this.renameField.charTyped(charInput.codepoint());
            return true;
        } else if (this.datasetNameField.isFocused()) {
            this.datasetNameField.charTyped(charInput.codepoint());
            return true;
        } else if (this.modelNameField.isFocused()) {
            this.modelNameField.charTyped(charInput.codepoint());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void resize(int i, int i2) {
        this.closeImmediately();
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

    record RenameTarget(float x, float y, float w, float h, String name, boolean model) {
    }

    enum Tab {
        RECORD(Icon.SWORDS, "neuro.tab.record", "Запись"),
        LEARN(Icon.CHART_LINE, "neuro.tab.learn", "Обучение"),
        MODELS(Icon.MEMORY, "neuro.tab.models", "Модели"),
        DEBUG(Icon.BUG, "neuro.tab.debug", "Дебаг");

        static final NeuroPanel.Tab[] VALUES = values();
        final Icon icon;
        private final String translationKey;
        private final String fallbackName;

        Tab(Icon icon2, String string, String string2) {
            this.icon = icon2;
            this.translationKey = string;
            this.fallbackName = string2;
        }

        String getDisplayName() {
            return Language.translate(this.translationKey, this.fallbackName);
        }
    }
}
