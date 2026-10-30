package tech.sadnes.client.ui.clickgui.component;

import java.util.Iterator;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.CategoryDescriptor;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.Module;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.VerticalFadeMask;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptModule;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.SettingHolder;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.setting.ValueSetting;
import tech.sadnes.client.setting.WaveSetting;
import tech.sadnes.client.setting.curve.CurveSetting;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class SettingsPopupComponent extends Component {
    private static final float HEADER_HEIGHT = 24.0F;
    private static final float MODULE_HEADER_HEIGHT = 25.0F;
    private static final float SETTING_HEIGHT = 20.0F;
    private static final float PADDING = 8.0F;
    private static final float SETTING_GAP = 0.0F;
    private static final float TITLE_FONT_SIZE = 7.0F;
    private static final float CLOSE_ICON_SIZE = 8.0F;
    private static final float DEFAULT_MAX_WIDTH = 150.0F;
    private static final float DEFAULT_MIN_WIDTH = 100.0F;
    private static final float SIDE_MIN_WIDTH = 90.0F;
    private static final float SCREEN_MARGIN = 4.0F;
    private static final float BOTTOM_PADDING = 2.0F;
    private static final float CORNER_RADIUS = 8.0F;
    private static final float MAX_CONTENT_HEIGHT = 300.0F;
    private static final float FADE_HEIGHT = 14.0F;
    private final Animation openAnimation = new Animation();
    private final Animation widthAnimation = new Animation();
    private final SmoothScrollbar scroll = new SmoothScrollbar();
    private float minWidth = 100.0F;
    private float maxWidth = 150.0F;
    private SettingHolder holder;
    private BindPopupComponent bindPopup;
    private float popupHeight;
    private boolean closing;
    private SettingHolder pendingHolder;
    private float pendingX;
    private float pendingY;
    private float pendingWidth;
    private int pendingScreenWidth;
    private int pendingScreenHeight;
    private float pendingMinWidth;
    private int screenWidth = Integer.MAX_VALUE;
    private int screenHeight = Integer.MAX_VALUE;
    private float currentWidth = 150.0F;

    @Override
    public void resize(int i, int i2) {
        this.openAnimation.forceValue(0.0);
        this.holder = null;
        this.bindPopup = null;
        this.closing = false;
        this.pendingHolder = null;
        this.screenWidth = i;
        this.screenHeight = i2;
    }

    @Override
    public void init(int i, int i2) {
        this.screenWidth = i;
        this.screenHeight = i2;
    }

    public boolean isOpen() {
        return this.holder != null;
    }

    public boolean isShowing() {
        return this.openAnimation.get() > 0.001;
    }

    public Feature getFeature() {
        SettingHolder settingHolder = this.holder;
        return settingHolder instanceof Feature ? (Feature)settingHolder : null;
    }

    public void open(SettingHolder settingHolder, float f, float f2, float f3, int i, int i2) {
        this.open(settingHolder, f, f2, f3, i, i2, 0.0F);
    }

    public void openSide(SettingHolder settingHolder, float f, float f2, float f3, int i, int i2) {
        this.open(settingHolder, f, f2, f3, i, i2, 90.0F);
    }

    public void open(SettingHolder settingHolder, float f, float f2, float f3, int i, int i2, float f4) {
        if (settingHolder != null) {
            if (this.holder != settingHolder || this.closing) {
                if (this.isShowing() && this.holder != null && this.holder != settingHolder) {
                    this.pendingHolder = settingHolder;
                    this.pendingX = f;
                    this.pendingY = f2;
                    this.pendingWidth = f3;
                    this.pendingScreenWidth = i;
                    this.pendingScreenHeight = i2;
                    this.pendingMinWidth = f4;
                    this.close();
                } else {
                    this.closeSettingOverlays();
                    this.holder = settingHolder;
                    this.bindPopup = settingHolder instanceof Feature feature
                        ? new BindPopupComponent(feature)
                        : (!(settingHolder instanceof ScriptModule) && !(settingHolder instanceof Script) ? null : new BindPopupComponent(null));
                    if (this.bindPopup != null) {
                        this.bindPopup.setAllHotKeysAction(() -> this.getClickGui().openHotkeysPanel());
                    }

                    this.closing = false;
                    this.screenWidth = i;
                    this.screenHeight = i2;

                    for (Setting setting : settingHolder.getSettings()) {
                        if (!this.isHiddenSetting(setting)) {
                            setting.resize(0, 0);
                            setting.alphaAnimation.forceValue(setting.isVisible() ? 1.0 : 0.0);
                        }
                    }

                    boolean flag;
                    float f5;
                    SettingsPopupComponent settingsPopupComponent;
                    if (f4 > 0.0F) {
                        flag = true;
                        settingsPopupComponent = this;
                        f5 = f4;
                    } else {
                        flag = false;
                        settingsPopupComponent = this;
                        f5 = 100.0F;
                    }

                    settingsPopupComponent.minWidth = f5;
                    this.maxWidth = !flag && !(f3 <= 10.0F) ? f3 + 15.0F : 150.0F;
                    this.currentWidth = this.computeWidth();
                    this.widthAnimation.forceValue(this.currentWidth);
                    this.scroll.reset();
                    this.size.x = this.currentWidth;
                    this.size.y = this.computeHeight();
                    this.positionNear(f, f2, f3);
                    this.openAnimation.animate(1.0, 0.3, Easings.BACK_OUT);
                }
            }
        }
    }

    public void openAtCurrentPosition(SettingHolder settingHolder) {
        this.open(settingHolder, this.position.x, this.position.y, 0.0F, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    private void positionNear(float f, float f2, float f3) {
        float f4 = f + f3 - 6.0F;
        float f5 = f2 - 15.0F;
        this.position.set(this.clampToScreen(f4, this.size.x, this.screenWidth), this.clampToScreen(f5, this.size.y, this.screenHeight));
    }

    @Override
    public void close() {
        if (this.holder != null) {
            this.closeSettingOverlays();
            this.closing = true;
            this.openAnimation.animate(0.0, 0.2, Easings.QUAD_IN);
        }
    }

    public void forceClose() {
        this.closeSettingOverlays();
        this.holder = null;
        this.bindPopup = null;
        this.closing = false;
        this.pendingHolder = null;
        this.openAnimation.forceValue(0.0);
    }

    private void closeSettingOverlays() {
        if (this.bindPopup != null) {
            this.bindPopup.close();
        }

        if (this.holder != null) {
            Iterator iterator2 = this.holder.getSettings().iterator();

            while (iterator2.hasNext()) {
                ((Setting)iterator2.next()).close();
            }
        }
    }

    public boolean isMouseOverPanel(double d, double d2) {
        return this.isShowing() && this.isHovered(d, d2, this.position.x, this.position.y, this.size.x, this.size.y);
    }

    public boolean isMouseOverAny(double d, double d2) {
        if (this.isShowing() && this.holder != null) {
            if (this.isMouseOverPanel(d, d2)) {
                return true;
            }

            if (this.bindPopup != null && this.bindPopup.isMouseOver(d, d2)) {
                return true;
            }

            for (Setting setting : this.holder.getSettings()) {
                if (!(setting.alphaAnimation.get() <= 0.001F)) {
                    if (setting instanceof ColorSetting && ((ColorSetting)setting).isMouseOverPicker(d, d2)) {
                        return true;
                    }

                    if (setting instanceof ModeSetting && ((ModeSetting)setting).isOverDropdown(d, d2)) {
                        return true;
                    }

                    if (setting instanceof MultiSetting && ((MultiSetting)setting).isOverDropdown(d, d2)) {
                        return true;
                    }

                    if (setting instanceof TextSetting && ((TextSetting)setting).isOverEditor(d, d2)) {
                        return true;
                    }

                    if (setting instanceof GroupSetting && ((GroupSetting)setting).isOverOverlay(d, d2)) {
                        return true;
                    }

                    if (setting instanceof CurveSetting && ((CurveSetting)setting).isCapturingMouse(d, d2)) {
                        return true;
                    }

                    if (setting instanceof WaveSetting && ((WaveSetting)setting).isOverEditor(d, d2)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean hasOpenOverlay() {
        if (this.bindPopup != null && this.bindPopup.isOpen()) {
            return true;
        }

        if (this.holder == null) {
            return false;
        }

        for (Setting setting : this.holder.getSettings()) {
            if (!this.isHiddenSetting(setting) && setting.isOverlayOpen()) {
                return true;
            }
        }

        return false;
    }

    private boolean isHiddenSetting(Setting<?> setting) {
        return setting instanceof DragSetting || !this.holder.isSettingVisible(setting);
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        super.render(drawContext, i, i2, f);
        this.openAnimation.update();
        if (this.closing && this.openAnimation.isFinished() && this.openAnimation.get() <= 0.001) {
            this.holder = null;
            this.bindPopup = null;
            this.closing = false;
            if (this.pendingHolder != null) {
                SettingHolder settingHolder = this.pendingHolder;
                float f2 = this.pendingX;
                float f3 = this.pendingY;
                float f4 = this.pendingWidth;
                int i3 = this.pendingScreenWidth;
                int i4 = this.pendingScreenHeight;
                float f5 = this.pendingMinWidth;
                this.pendingHolder = null;
                this.pendingMinWidth = 0.0F;
                this.open(settingHolder, f2, f3, f4, i3, i4, f5);
            }
        } else if (this.holder != null) {
            float f6 = this.openAnimation.get();
            float f7 = Math.min(f6, 1.0F) * this.getAlpha();
            this.widthAnimation.update();
            this.widthAnimation.animate(this.computeWidth(), 0.2, Easings.EXPO_OUT);
            this.currentWidth = this.widthAnimation.get();
            this.position.x = this.clampToScreen(this.position.x, this.currentWidth, this.screenWidth);
            this.popupHeight = this.computeHeight();
            this.size.y = this.popupHeight;
            this.size.x = this.currentWidth;
            float f8 = this.position.x;
            float f9 = this.position.y;
            float f10 = this.size.x;
            float f11 = this.size.y;
            float f12 = 0.9F + 0.1F * f6;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f8 + f10 / 2.0F, f9 + f11 / 2.0F, f12);
            float f13 = Math.min(f6, 1.0F);
            this.renderPanel(drawContext, f8, f9, f10, f11, f7);
            this.renderHeader(drawContext, f8, f9, f10, f7, i, i2);
            this.renderSettings(drawContext, f8, f9, f10, f11, f7, f13, i, i2, f);
            MatrixUtil.pop(drawContext);
        }
    }

    public void renderOverlays(DrawContext drawContext, int i, int i2, float f) {
        if (this.holder != null) {
            this.openAnimation.get();
            this.getAlpha();

            for (Setting setting : this.holder.getSettings()) {
                if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F)) {
                    if (!(setting instanceof ColorSetting) && !(setting instanceof ModeSetting) && !(setting instanceof MultiSetting) && !(setting instanceof TextSetting)) {
                        setting.renderOverlay(drawContext, i, i2, f);
                    } else {
                        setting.renderOverlay(drawContext, i, i2, f);
                    }
                }
            }

            if (this.bindPopup != null) {
                this.bindPopup.render(drawContext, i, i2, f);
            }
        }
    }

    private float computeHeight() {
        return this.holder == null ? 24.0F : this.getHeaderHeight() + Math.min(this.computeContentHeight(), 300.0F) + 2.0F;
    }

    private float computeContentHeight() {
        float f = 0.0F;
        int i = 0;

        for (Setting setting : this.holder.getSettings()) {
            if (!this.isHiddenSetting(setting)) {
                float f2 = setting.alphaAnimation.get();
                f += (setting.resolveHeight(20.0F) + 0.0F) * f2;
                if (setting.isVisible()) {
                    i++;
                }
            }
        }

        return i == 0 ? f + 20.0F : f;
    }

    private float computeWidth() {
        float f = 8.0F + this.computeHeaderWidth() + 4.0F + 24.0F;

        for (Setting setting : this.holder.getSettings()) {
            if (!this.isHiddenSetting(setting) && setting.isVisible()) {
                f = Math.max(f, setting.getPreferredWidth() + 16.0F);
            }
        }

        return Math.max(this.minWidth, Math.min(this.maxWidth, f));
    }

    private float computeHeaderWidth() {
        float f = Fonts.INTER_SEMIBOLD.getWidth(this.holder.getDisplayName(), 7.0F);
        if (this.holder instanceof Module module) {
            float f2 = 8.0F + Fonts.INTER_MEDIUM.getWidth(module.getCategory().getName(), 5.5F);
            String string = module.getSubcategoryName();
            if (!string.isEmpty()) {
                f2 += 8.0F + Fonts.INTER_MEDIUM.getWidth(string, 5.5F);
            }

            return Math.max(f, f2);
        } else {
            return f;
        }
    }

    private float getHeaderHeight() {
        return this.holder instanceof Module ? 25.0F : 24.0F;
    }

    private void renderPanel(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        GuiRenderUtil.drawPopup(drawContext, f, f2, f3, f4, f5, 8.0F);
    }

    private void renderHeader(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        float f5 = this.getHeaderHeight();
        int i3 = ColorUtil.multiplyAlpha(Theme.getTextColor(), f4);
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, f5);
        if (this.holder instanceof Module module) {
            renderUtil.textTopLeft(Fonts.INTER_SEMIBOLD, this.holder.getDisplayName(), 8.0F, 5.8F, i3, 7.0F);
            this.renderCategoryPath(drawContext, module, f + 8.0F, f2 + 13.7F, f4);
        } else {
            renderUtil.textMiddleLeft(Fonts.INTER_SEMIBOLD, this.holder.getDisplayName(), 8.0F, 0.0F, i3, 7.0F);
        }

        float f6 = f + f3 - 8.0F - 8.0F + 2.0F;
        float f7 = f2 + f5 / 2.0F - 4.0F;
        boolean flag = this.isHovered(i, i2, f + f3 - 24.0F, f2, 24.0, f5);
        int i4 = ColorUtil.multiplyAlpha(flag ? Theme.getTextColor() : Theme.getSecondaryText(), f4);
        new RenderUtil(drawContext, f6, f7, 8.0F, 8.0F).icon(Icon.CLOSE, 0.0F, 0.0F, 8.0F, i4);
        GuiRenderUtil.drawOverlay(drawContext, f + 8.0F, MathUtil.roundToStep(f2 + f5, 0.5F), f3 - 16.0F, 0.5F, f4);
    }

    private void renderCategoryPath(DrawContext drawContext, Module module, float f, float f2, float f3) {
        CategoryDescriptor categoryDescriptor = module.getCategory();
        int i = ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f3);
        new RenderUtil(drawContext, f - 0.5F, f2 - 0.35F, 6.0F, 6.0F).icon(categoryDescriptor.getIcon(), 0.0F, 0.0F, 6.0F, i);
        float f4 = f + 5.5F + 2.5F;
        new RenderUtil(drawContext, f4, f2, 0.0F, 5.5F).textTopLeft(Fonts.INTER_MEDIUM, categoryDescriptor.getName(), i, 5.5F);
        String string = module.getSubcategoryName();
        if (!string.isEmpty()) {
            float f5 = f4 + Fonts.INTER_MEDIUM.getWidth(categoryDescriptor.getName(), 5.5F) + 2.0F;
            new RenderUtil(drawContext, f5, f2 + 0.75F, 4.0F, 4.0F).icon(Icon.CHEVRON_RIGHT, 0.0F, 0.0F, 4.0F, ColorUtil.multiplyAlpha(i, 0.6F));
            new RenderUtil(drawContext, f5 + 6.0F, f2, 0.0F, 5.5F).textTopLeft(Fonts.INTER_MEDIUM, string, i, 5.5F);
        }
    }

    private void renderSettings(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7) {
        float f8 = f2 + this.getHeaderHeight();
        float f9 = f4 - this.getHeaderHeight();
        if (!(f9 <= 0.0F)) {
            float f10 = this.computeContentHeight();
            this.scroll.update(f10, f9);
            float f11 = this.scroll.getScroll();
            if (ScissorStack.push(drawContext, f, f8, f3, f9)) {
                try {
                    this.beginFadeMask(f8, f9, f10, f11);
                    float f12 = f3 - 16.0F;
                    float f13 = f8 + f11;
                    boolean flag = true;

                    for (Setting setting : this.holder.getSettings()) {
                        if (!this.isHiddenSetting(setting)) {
                            setting.alphaMultiplier = f6;
                            setting.alphaAnimation.update();
                            setting.alphaAnimation.animate(setting.isVisible() ? 1.0 : 0.0, 0.2F, Easings.EXPO_OUT);
                            float f14 = setting.alphaAnimation.get();
                            float f15 = setting.resolveHeight(20.0F);
                            float f16 = (f15 + 0.0F) * f14;
                            if (f16 > 0.01F) {
                                if (!flag) {
                                    GuiRenderUtil.drawOverlay(drawContext, f + 8.0F, MathUtil.roundToStep(f13, 0.5F), f3 - 16.0F, 0.5F, f5 * f14);
                                }

                                flag = false;
                            }

                            setting.position.set(f + 8.0F, f13);
                            setting.size.set(f12, f15);
                            setting.render(drawContext, i, i2, f7);
                            f13 += f16;
                        }
                    }
                } finally {
                    VerticalFadeMask.end();
                    ScissorStack.pop(drawContext);
                }
            }
        }
    }

    private void beginFadeMask(float f, float f2, float f3, float f4) {
        float f5 = f3 - f2;
        if (!(f5 <= 0.5F)) {
            float f6 = Math.min(1.0F, Math.max(0.0F, -f4 / 14.0F));
            float f7 = Math.min(1.0F, Math.max(0.0F, (f5 + f4) / 14.0F));
            float f8 = f + f2;
            VerticalFadeMask.begin(f, f + 14.0F, f6, f8 - 14.0F, f8, f7);
        }
    }

    private boolean isOverContent(double d, double d2) {
        return this.isHovered(d, d2, this.position.x, this.position.y + this.getHeaderHeight(), this.size.x, this.size.y - this.getHeaderHeight());
    }

    @Override
    public void mouseMoved(double d, double d2) {
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (this.holder == null) {
            return false;
        }

        if (this.isHovered(click.x(), click.y(), this.position.x + this.size.x - 24.0F, this.position.y, 24.0, this.getHeaderHeight())) {
            this.close();
            return true;
        }

        if (this.bindPopup == null || !this.bindPopup.isOpen() && !this.bindPopup.isListeningForKey()) {
            if (this.bindPopup != null && this.bindPopup.isMouseOver(click.x(), click.y())) {
                return this.bindPopup.mouseClicked(click, flag);
            }

            Iterator iterator2 = this.holder.getSettings().iterator();

            while (true) {
                if (!iterator2.hasNext()) {
                    if (!this.isOverContent(click.x(), click.y())) {
                        return false;
                    }

                    if (this.bindPopup != null && (this.isLeftButton(click.button()) || this.isRightButton(click.button()) || this.isMiddleButton(click.button()))
                        )
                     {
                        KeybindSetting keybindSetting = this.findHoveredKeybind(click.x(), click.y());
                        if (keybindSetting != null) {
                            this.bindPopup.openForSetting(keybindSetting, click.x(), click.y());
                            return true;
                        }
                    }

                    if (this.bindPopup != null && (this.isRightButton(click.button()) || this.isMiddleButton(click.button()))) {
                        ValueSetting valueSetting = this.findHoveredValueSetting(click.x(), click.y());
                        if (valueSetting != null) {
                            this.bindPopup.openForSetting(valueSetting, click.x(), click.y());
                            return true;
                        }
                    }

                    for (Setting setting : this.holder.getSettings()) {
                        if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F)) {
                            boolean flag2 = setting.isOverlayOpen();
                            if (setting.mouseClicked(click, flag)) {
                                if (!flag2 && setting.isOverlayOpen()) {
                                    this.closeOtherOverlays(setting);
                                    return true;
                                }

                                return true;
                            }
                        }
                    }

                    return false;
                }

                Setting setting2 = (Setting)iterator2.next();
                if (!this.isHiddenSetting(setting2) && !(setting2.alphaAnimation.get() <= 0.001F) && setting2.isOverlayOpen()) {
                    if ((this.isRightButton(click.button()) || this.isMiddleButton(click.button())) && setting2 instanceof MultiSetting multiSetting) {
                        BooleanSetting booleanSetting = multiSetting.getHoveredOption(click.x(), click.y());
                        if (booleanSetting != null) {
                            if (this.bindPopup != null) {
                                this.bindPopup.openForSetting(booleanSetting, click.x(), click.y());
                            }
                            break;
                        }
                    }

                    if ((this.isRightButton(click.button()) || this.isMiddleButton(click.button())) && setting2 instanceof ModeSetting modeSetting) {
                        BooleanSetting booleanSetting2 = modeSetting.getHoveredToggle(click.x(), click.y());
                        if (booleanSetting2 != null) {
                            if (this.bindPopup != null) {
                                this.bindPopup.openForSetting(booleanSetting2, click.x(), click.y());
                                return true;
                            }
                            break;
                        }
                    }

                    if (setting2 instanceof GroupSetting groupSetting) {
                        KeybindSetting keybindSetting2 = groupSetting.findHoveredBindSetting(click.x(), click.y());
                        if (keybindSetting2 != null) {
                            if (this.bindPopup != null) {
                                this.bindPopup.openForSetting(keybindSetting2, click.x(), click.y());
                                return true;
                            }
                            break;
                        }

                        if (this.isRightButton(click.button()) || this.isMiddleButton(click.button())) {
                            ValueSetting valueSetting2 = groupSetting.findHoveredValueSetting(click.x(), click.y());
                            if (valueSetting2 != null) {
                                if (this.bindPopup != null) {
                                    this.bindPopup.openForSetting(valueSetting2, click.x(), click.y());
                                    return true;
                                }
                                break;
                            }
                        }
                    }

                    if (setting2.mouseClicked(click, flag)) {
                        return true;
                    }
                }
            }

            return true;
        } else {
            this.bindPopup.mouseClicked(click, flag);
            return true;
        }
    }

    private void closeOtherOverlays(Setting<?> setting) {
        if (this.holder != null) {
            for (Setting setting2 : this.holder.getSettings()) {
                if (setting2 != setting) {
                    if (setting2 instanceof MultiSetting) {
                        ((MultiSetting)setting2).closeDropdown();
                    } else if (setting2 instanceof ModeSetting) {
                        ((ModeSetting)setting2).closeDropdown();
                    } else if (setting2 instanceof GroupSetting) {
                        ((GroupSetting)setting2).closePopupImmediately();
                    } else if (setting2 instanceof CurveSetting) {
                        ((CurveSetting)setting2).resetPopup();
                    } else if (setting2 instanceof WaveSetting) {
                        ((WaveSetting)setting2).closeEditor();
                    }
                }
            }
        }
    }

    private KeybindSetting findHoveredKeybind(double d, double d2) {
        for (Setting setting : this.holder.getSettings()) {
            if (!(setting.alphaAnimation.get() <= 0.001F) && setting instanceof KeybindSetting keybindSetting && this.isHovered(d, d2, setting.position, setting.size)) {
                return keybindSetting;
            }
        }

        return null;
    }

    private ValueSetting<?> findHoveredValueSetting(double d, double d2) {
        for (Setting setting : this.holder.getSettings()) {
            if (!(setting.alphaAnimation.get() <= 0.001F)
                && !(setting instanceof KeybindSetting)
                && !(setting instanceof ColorSetting)
                && !(setting instanceof ModeSetting)
                && !(setting instanceof MultiSetting)
                && setting instanceof ValueSetting valueSetting
                && this.isHovered(d, d2, setting.position, setting.size)) {
                return valueSetting;
            }
        }

        return null;
    }

    @Override
    public boolean onMouseRelease(Click click) {
        if (this.holder == null) {
            return false;
        }

        if (this.bindPopup != null) {
            this.bindPopup.onMouseRelease(click);
        }

        for (Setting setting : this.holder.getSettings()) {
            if (!this.isHiddenSetting(setting)) {
                setting.onMouseRelease(click);
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.holder == null) {
            return false;
        }

        if (this.bindPopup != null && this.bindPopup.isOpen()) {
            this.bindPopup.mouseDragged(click, d, d2);
            return true;
        }

        if (this.bindPopup != null && this.bindPopup.mouseDragged(click, d, d2)) {
            return true;
        }

        for (Setting setting : this.holder.getSettings()) {
            if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F) && setting.mouseDragged(click, d, d2)) {
                return true;
            }
        }

        return false;
    }

    private float clampToScreen(float f, float f2, int i) {
        if (i > 0 && i != Integer.MAX_VALUE) {
            float f3 = Math.max(4.0F, i - 4.0F - f2);
            return f3 < 4.0F ? 4.0F : Math.max(4.0F, Math.min(f, f3));
        } else {
            return f;
        }
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (this.holder == null) {
            return false;
        }

        if (this.bindPopup != null && this.bindPopup.isOpen()) {
            this.bindPopup.mouseScrolled(d, d2, d3, d4);
            return true;
        }

        for (Setting setting : this.holder.getSettings()) {
            if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F) && setting.mouseScrolled(d, d2, d3, d4)) {
                return true;
            }
        }

        if (!this.isMouseOverPanel(d, d2)) {
            return false;
        }

        this.scroll.onScroll(d4);
        return true;
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (this.holder == null) {
            return false;
        }

        if (this.bindPopup != null && this.bindPopup.isOpen()) {
            this.bindPopup.onKeyPress(keyInput);
            return true;
        }

        if (this.bindPopup != null && this.bindPopup.onKeyPress(keyInput)) {
            return true;
        }

        for (Setting setting : this.holder.getSettings()) {
            if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F) && setting.onKeyPress(keyInput)) {
                return true;
            }
        }

        if (keyInput.key() == 256) {
            this.close();
            return true;
        } else {
            return false;
        }
    }

    public boolean isListeningForKey() {
        return this.bindPopup != null && this.bindPopup.isListeningForKey();
    }

    public boolean handleBindKeyRelease(KeyInput keyInput) {
        return this.bindPopup != null && this.bindPopup.isOpen() && this.bindPopup.keyReleased(keyInput);
    }

    @Override
    public boolean keyReleased(KeyInput keyInput) {
        if (this.holder == null) {
            return false;
        }

        if (this.bindPopup != null && this.bindPopup.isOpen()) {
            return true;
        }

        for (Setting setting : this.holder.getSettings()) {
            if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F)) {
                setting.keyReleased(keyInput);
            }
        }

        return false;
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        if (this.holder == null) {
            return false;
        }

        if (this.bindPopup != null && this.bindPopup.isOpen()) {
            return this.bindPopup.onCharType(charInput);
        }

        for (Setting setting : this.holder.getSettings()) {
            if (!this.isHiddenSetting(setting) && !(setting.alphaAnimation.get() <= 0.001F) && setting.onCharType(charInput)) {
                return true;
            }
        }

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
    public void removed() {
    }

    public Animation getOpenAnimation() {
        return this.openAnimation;
    }

    public Animation getWidthAnimation() {
        return this.widthAnimation;
    }

    public SmoothScrollbar getScroll() {
        return this.scroll;
    }

    public float getMinWidth() {
        return this.minWidth;
    }

    public float getMaxWidth() {
        return this.maxWidth;
    }

    public SettingHolder getHolder() {
        return this.holder;
    }

    public BindPopupComponent getBindPopup() {
        return this.bindPopup;
    }

    public float getPopupHeight() {
        return this.popupHeight;
    }

    public boolean isClosing() {
        return this.closing;
    }

    public SettingHolder getPendingHolder() {
        return this.pendingHolder;
    }

    public float getPendingX() {
        return this.pendingX;
    }

    public float getPendingY() {
        return this.pendingY;
    }

    public float getPendingWidth() {
        return this.pendingWidth;
    }

    public int getPendingScreenWidth() {
        return this.pendingScreenWidth;
    }

    public int getPendingScreenHeight() {
        return this.pendingScreenHeight;
    }

    public float getPendingMinWidth() {
        return this.pendingMinWidth;
    }

    public int getScreenWidth() {
        return this.screenWidth;
    }

    public int getScreenHeight() {
        return this.screenHeight;
    }

    public float getCurrentWidth() {
        return this.currentWidth;
    }
}
