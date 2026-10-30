package tech.sadnes.client.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.Side;
import tech.sadnes.client.render.VerticalFadeMask;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.ui.clickgui.component.SmoothScrollbar;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class GroupSetting extends ValueSetting<Boolean> {
    private static final float GEAR_GAP = 5.0F;
    private static final float GEAR_SIZE = 7.5F;
    private static final float GEAR_HITBOX_SIZE = 10.0F;
    private static final float MAX_POPUP_WIDTH = 132.0F;
    private static final float MIN_POPUP_WIDTH = 90.0F;
    private static final float POPUP_PADDING = 4.0F;
    private static final float CONTENT_PADDING_X = 5.0F;
    private static final float CONTENT_PADDING_Y = 4.0F;
    private static final float ROW_HEIGHT = 15.0F;
    private static final float POPUP_RADIUS = 10.0F;
    private static final int MAX_VISIBLE_ROWS = 10;
    private static final float LABEL_FONT_SIZE = 6.0F;
    private static final float NESTED_PADDING_X = 4.0F;
    private static final float NESTED_PADDING_Y = 2.0F;
    private static final float NESTED_BOTTOM_GAP = 6.0F;
    private static final float NESTED_GAP = 4.0F;
    private static final float MAX_NESTED_POPUP_HEIGHT = 280.0F;
    private static final float DISABLED_CHILD_ALPHA = 0.45F;
    private static final float FADE_EDGE_SIZE = 14.0F;
    private final List<Setting<?>> children = new ArrayList<>();
    private final Animation popupAnimation = new Animation();
    private final Animation gearHoverAnimation = new Animation();
    private final SmoothScrollbar scroller = new SmoothScrollbar();
    private final Animation popupWidthAnimation = new Animation();
    private final Map<ValueBind<Boolean>, Animation> bindToggleAnimations = new IdentityHashMap<>();
    private boolean popupOpen;
    private boolean nested;
    private float popupX;
    private float popupY;
    private float popupHeight;
    private float popupWidth = 132.0F;

    public GroupSetting(String string) {
        this(string, false);
    }

    public GroupSetting(String string, boolean flag) {
        super(string, flag);
        this.setValue(flag);
    }

    public GroupSetting onInput(Setting<?>... settings) {
        if (settings == null) {
            return this;
        }

        for (Setting setting : settings) {
            if (setting != null && setting != this && !this.children.contains(setting)) {
                setting.setChild(true);
                if (setting instanceof GroupSetting) {
                    ((GroupSetting)setting).nested = true;
                }

                this.children.add(setting);
            }
        }

        return this;
    }

    public GroupSetting setValue(Boolean flag) {
        super.setValue(flag);
        return this;
    }

    public GroupSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public GroupSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public GroupSetting setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    public GroupSetting setIcon(Icon icon) {
        super.setIcon(icon);
        return this;
    }

    protected Boolean sanitizeValue(Boolean flag) {
        return Boolean.TRUE.equals(flag);
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.getValue());
    }

    public GroupSetting toggle() {
        return !Boolean.TRUE.equals(this.getDefaultValue()) ? this.setValue(true) : this.setValue(false);
    }

    public boolean isNested() {
        return this.nested;
    }

    public Setting<?> findChild(String string) {
        if (string == null) {
            return null;
        }

        for (Setting setting : this.children) {
            if (string.equalsIgnoreCase(setting.getName())) {
                return setting;
            }
        }

        return null;
    }

    public List<Setting<?>> getChildList() {
        return Collections.unmodifiableList(this.children);
    }

    @Override
    protected List<Setting<?>> getChildren() {
        return Collections.unmodifiableList(this.children);
    }

    @Override
    public String formatBindValue(ValueBind<Boolean> valueBind) {
        boolean flag = valueBind != null ? Boolean.TRUE.equals(valueBind.getValue()) : this.isEnabled();
        return flag ? "On" : "Off";
    }

    public ValueBind<Boolean> createBind(Boolean flag) {
        return !Boolean.TRUE.equals(this.getDefaultValue()) ? super.createBind(true) : super.createBind(false);
    }

    private void renderBindToggle(DrawContext drawContext) {
        ValueBind valueBind = this.editingBind;
        float f = this.position.x;
        float f2 = this.position.y;
        float f3 = this.size.x;
        float f4 = this.size.y;
        boolean flag = Boolean.TRUE.equals(valueBind.getValue());
        this.bindToggleAnimations.keySet().removeIf(valueBind2 -> !this.getBinds().contains(valueBind2));
        Animation animation = this.bindToggleAnimations.computeIfAbsent(valueBind, valueBind3 -> {
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
        animation.animate(flag ? 1.0 : 0.0, 0.5, Easings.EXPO_OUT);
        new RenderUtil(drawContext, f, f2, f3, f4)
            .icon(Icon.CHECK, 3.0F, f4 / 2.0F - 4.25F, 8.5F, this.getSecondaryColor())
            .textMiddleLeft(Fonts.INTER_MEDIUM, "Value", 17.0F, 0.0F, this.getSecondaryColor(), 6.5F);
        float f5 = f + f3 - 14.0F - 4.0F;
        float f6 = f2 + f4 / 2.0F - 4.5F;
        SettingWidgets.drawDefaultSwitch(drawContext, f5, f6, animation.get(), this.getAlpha());
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        if (this.editingBind != null) {
            this.renderBindToggle(drawContext);
        } else if (this.nested) {
            this.renderNested(drawContext, i, i2, f);
        } else {
            float f2 = this.position.x;
            float f3 = this.position.y;
            float f4 = this.size.x;
            float f5 = this.size.y;
            this.valueAnimation.update();
            this.valueAnimation.animate(this.isEnabled() ? 1.0 : 0.0, 0.5, Easings.EXPO_OUT);
            float f6 = this.valueAnimation.get();
            float f7 = f2 + f4 - 14.0F;
            float f8 = f3 + f5 / 2.0F - 4.5F;
            SettingWidgets.drawDefaultSwitch(drawContext, f7, f8, f6, this.getAlpha());
            boolean flag = this.hasVisibleChildren();
            float f9 = f7;
            if (flag) {
                float f10 = f7 - 5.0F - 7.5F;
                f9 = f10;
                this.gearHoverAnimation.update();
                this.gearHoverAnimation.animate(this.isGearHovered(i, i2) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
                float f11 = this.gearHoverAnimation.get();
                GuiRenderUtil.drawHighlight(drawContext, f10 - 1.25F, f3 + f5 / 2.0F - 5.0F, 10.0F, 10.0F, this.getAlpha() * f11, Radius.all(3.3333333F));
                int i3 = Theme.withAlpha(Theme.getPrimary(), (0.65F + 0.35F * f11) * this.getAlpha());
                float f12 = f3 + f5 / 2.0F - 3.75F;
                new RenderUtil(drawContext, f10, f12, 7.5F, 7.5F).icon(Icon.SETTINGS, 0.0F, 0.0F, 7.5F, i3);
            }

            float f13 = Math.max(0.0F, f9 - 5.0F - f2);
            this.drawTitle(drawContext, f2, f3, f13, f5, f6, this.isHovered(i, i2, this.position, this.size));
        }
    }

    private void drawTitle(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, boolean flag) {
        float f6 = this.drawIcon(drawContext, f, f2, f4, 6.0F);
        int i = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.withAlpha(Theme.getTextColor(), 0.5F), Theme.getTextColor(), f5), this.getAlpha());
        new RenderUtil(drawContext, f, f2, f3, f4)
            .scrollingTextMiddle(Fonts.INTER_MEDIUM, Language.translate(this.getName()), f6, 0.0F, Math.max(0.0F, f3 - f6), i, 6.0F, flag);
    }

    private void renderNested(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.position.x;
        float f3 = this.position.y;
        float f4 = this.size.x;
        float f5 = this.size.y;
        this.valueAnimation.update();
        this.valueAnimation.animate(this.isEnabled() ? 1.0 : 0.0, 0.5, Easings.EXPO_OUT);
        float f6 = this.valueAnimation.get();
        float f7 = this.getAlpha();
        Radius radius = Radius.smoothAll(12.0F);
        RenderUtil renderUtil = new RenderUtil(drawContext, f2, f3, f4, f5);
        renderUtil.rect(ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.09F), 0.35F * f7, radius);
        renderUtil.expand(Side.ALL, -0.5F);
        renderUtil.shadow(0.5F, ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.5F), 0.06F * f7, radius);
        renderUtil.expand(Side.ALL, 0.5F);
        float f8 = f2 + 4.0F;
        float f9 = f4 - 8.0F;
        float f10 = f3 + 2.0F;
        float f11 = f8 + f9 - 14.0F;
        float f12 = f10 + 7.5F - 4.5F;
        SettingWidgets.drawDefaultSwitch(drawContext, f11, f12, f6, f7);
        this.drawTitle(drawContext, f8, f10, f11 - 5.0F - f8, 15.0F, f6, this.isHovered(i, i2, f8, f10, f9, 15.0));
        float f13 = 0.45F + 0.55F * f6;
        float f14 = f10 + 15.0F;

        for (Setting setting : this.children) {
            setting.alphaMultiplier = super.alphaMultiplier * f13;
            setting.alphaAnimation.update();
            setting.alphaAnimation.animate(setting.isVisible() ? 1.0 : 0.0, 0.2F, Easings.EXPO_OUT);
            float f15 = setting.alphaAnimation.get();
            if (!(f15 <= 0.001F)) {
                float f16 = setting.resolveHeight(15.0F);
                setting.position.set(f8, f14);
                setting.size.set(f9, f16);
                setting.render(drawContext, i, i2, f);
                f14 += f16 * f15;
            }
        }
    }

    @Override
    public float resolveHeight(float f) {
        if (!this.nested) {
            return f;
        }

        float f2 = 4.0F + f;

        for (Setting setting : this.children) {
            f2 += setting.resolveHeight(f) * setting.alphaAnimation.get();
        }

        return f2;
    }

    private boolean isHeaderHovered(double d, double d2) {
        return this.isHovered(d, d2, this.position.x + 4.0F, this.position.y + 2.0F, this.size.x - 8.0F, 15.0);
    }

    private boolean mouseClickedNested(Click click) {
        boolean flag = this.isLeftButton(click.button());
        if (!flag && !this.isRightButton(click.button())) {
            return false;
        }

        List<Setting<?>> settings = this.getVisibleChildren();

        for (Setting setting : settings) {
            if (setting.isOverlayOpen() && setting.mouseClicked(click, false)) {
                return true;
            }
        }

        for (Setting setting2 : settings) {
            boolean flag2 = setting2.isOverlayOpen();
            if (setting2.mouseClicked(click, false)) {
                if (!flag2 && setting2.isOverlayOpen()) {
                    this.closeOtherOverlays(setting2);
                }

                return true;
            }
        }

        if (flag && this.isHeaderHovered(click.x(), click.y())) {
            this.toggle();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        ValueBind valueBind = this.editingBind;
        if (valueBind != null) {
            if (!this.isLeftButton(click.button())) {
                return false;
            } else if (!this.isHovered(click.x(), click.y(), this.position.x, this.position.y, this.size.x, this.size.y)) {
                return false;
            } else if (!Boolean.TRUE.equals(valueBind.getValue())) {
                this.setBindValue(valueBind, true);
                return true;
            } else {
                this.setBindValue(valueBind, false);
                return true;
            }
        } else {
            if (this.nested) {
                return this.mouseClickedNested(click);
            }

            boolean flag2 = this.isLeftButton(click.button());
            boolean flag3 = this.isRightButton(click.button());
            if (!flag2 && !flag3) {
                return false;
            }

            if (!this.popupOpen) {
                boolean flag4 = this.hasVisibleChildren();
                if (flag2 && flag4 && this.isGearHovered(click.x(), click.y())) {
                    this.openPopup();
                    return true;
                } else if (flag3 && flag4 && this.isRowHovered(click.x(), click.y())) {
                    this.openPopup();
                    return true;
                } else if (flag2 && this.isRowHovered(click.x(), click.y())) {
                    this.toggle();
                    return true;
                } else {
                    return false;
                }
            } else {
                List<Setting<?>> settings = this.getVisibleChildren();

                for (Setting setting : settings) {
                    if (setting.isOverlayOpen() && setting.mouseClicked(click, false)) {
                        return true;
                    }
                }

                if (!this.isPopupHovered(click.x(), click.y())) {
                    if (this.isOverChildOverlay(click.x(), click.y())) {
                        return true;
                    } else if (flag2 && this.isGearHovered(click.x(), click.y())) {
                        this.closePopup();
                        return true;
                    } else if (flag2 && this.isRowHovered(click.x(), click.y())) {
                        this.toggle();
                        return true;
                    } else {
                        this.closePopup();
                        return false;
                    }
                } else {
                    for (Setting setting2 : settings) {
                        boolean flag5 = setting2.isOverlayOpen();
                        if (setting2.mouseClicked(click, false)) {
                            if (!flag5 && setting2.isOverlayOpen()) {
                                this.closeOtherOverlays(setting2);
                            }
                            break;
                        }
                    }

                    return true;
                }
            }
        }
    }

    @Override
    public boolean onMouseRelease(Click click) {
        this.scroller.mouseReleased();
        Iterator iterator2 = this.children.iterator();

        while (iterator2.hasNext()) {
            ((Setting)iterator2.next()).onMouseRelease(click);
        }

        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (!this.popupOpen && !this.nested) {
            return false;
        }

        if (!this.nested && this.scroller.isDragging() && this.scroller.mouseDragged(click.x(), click.y())) {
            return true;
        }

        for (Setting setting : this.getVisibleChildren()) {
            if (setting.mouseDragged(click, d, d2)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (this.nested) {
            for (Setting setting : this.getVisibleChildren()) {
                if (setting.mouseScrolled(d, d2, d3, d4)) {
                    return true;
                }
            }
        } else {
            if (!this.popupOpen) {
                return false;
            }

            for (Setting setting2 : this.getVisibleChildren()) {
                if (setting2 instanceof WaveSetting && ((WaveSetting)setting2).mouseScrolled(d, d2, d3, d4)) {
                    return true;
                }
            }

            if (this.closeChildOverlays()) {
                return true;
            }

            if (this.isPopupHovered(d, d2)) {
                this.scroller.onScroll(d4);
                return true;
            }

            for (Setting setting3 : this.getVisibleChildren()) {
                if (setting3.mouseScrolled(d, d2, d3, d4)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (!this.popupOpen && !this.nested) {
            return false;
        }

        if (!this.nested && keyInput.key() == 256) {
            for (Setting setting : this.getVisibleChildren()) {
                if (setting.isOverlayOpen()) {
                    setting.onKeyPress(keyInput);
                    return true;
                }
            }

            this.closePopup();
            return true;
        } else {
            for (Setting setting2 : this.getVisibleChildren()) {
                if (setting2.onKeyPress(keyInput)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean onCharType(CharInput charInput) {
        if (!this.popupOpen && !this.nested) {
            return false;
        }

        for (Setting setting : this.getVisibleChildren()) {
            if (setting.onCharType(charInput)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void renderOverlay(DrawContext drawContext, int i, int i2, float f) {
        if (this.nested) {
            for (Setting setting2 : this.children) {
                if (!(setting2.alphaAnimation.get() <= 0.001F)) {
                    setting2.alphaMultiplier = super.alphaMultiplier;
                    setting2.renderOverlay(drawContext, i, i2, f);
                }
            }
        } else {
            this.popupAnimation.update();
            if (this.popupOpen) {
                this.popupAnimation.animate(1.0, 0.3F, Easings.BACK_OUT);
            } else {
                this.popupAnimation.animate(0.0, 0.2F, Easings.QUAD_IN);
            }

            float f2 = this.popupAnimation.get();
            if (!(f2 <= 0.001F)) {
                if (this.hasRenderableChildren()) {
                    this.updatePopupBounds();
                    this.popupX = GuiRenderUtil.clampToScreenX(this.popupX, this.popupWidth);
                    this.popupY = GuiRenderUtil.clampToScreenY(this.popupY, this.popupHeight);
                    float f3 = this.getAlpha() * Math.min(f2, 1.0F);
                    float f4 = 0.85F + 0.15F * f2;
                    int i3 = ScissorStack.depth();
                    MatrixUtil.push(drawContext);

                    try {
                        MatrixUtil.scale(drawContext, this.popupX + this.popupWidth / 2.0F, this.popupY + this.popupHeight / 2.0F, f4);
                        GuiRenderUtil.drawPopup(drawContext, this.popupX, this.popupY, this.popupWidth, this.popupHeight, f3, 10.0F);
                        boolean flag = !this.hasNestedChild();
                        float f5 = this.popupX + 4.0F;
                        float f6 = this.popupY + 4.0F;
                        float f7 = this.popupWidth - 8.0F;
                        float f8 = this.popupHeight - 8.0F;
                        if (flag) {
                            this.drawContentBackground(drawContext, f5, f6, f7, f8, f3);
                        }

                        float f9 = f5 + 5.0F;
                        float f10 = f6 + 4.0F;
                        float f11 = f7 - 5.0F * 2.0F - this.getScrollbarWidth();
                        float f12 = this.getVisibleContentHeight();
                        float f13 = this.getContentHeight();
                        this.scroller.update(f13, f12);
                        this.scroller.updateHover(i, i2);
                        if (ScissorStack.push(drawContext, f5, f10, f7, f12)) {
                            try {
                                float f14 = this.scroller.getScroll();
                                this.applyEdgeFade(f10, f12, f13, f14);

                                for (Setting setting3 : this.children) {
                                    setting3.alphaAnimation.update();
                                    setting3.alphaAnimation.animate(setting3.isVisible() ? 1.0 : 0.0, 0.2F, Easings.EXPO_OUT);
                                }

                                this.layoutChildren(f10 + f14, (setting4, f15, f16) -> {
                                    float f17 = this.isScrollable(f13, f12) ? this.computeEdgeFadeAlpha(f15, f16, f10, f12) : 1.0F;
                                    setting4.alphaMultiplier = f3 * f17;
                                    setting4.position.set(f9, f15);
                                    setting4.size.set(f11, f16);
                                    if (f15 + f16 >= f10 && f15 <= f10 + f12) {
                                        setting4.render(drawContext, i, i2, f);
                                    }
                                });
                            } finally {
                                VerticalFadeMask.end();
                                ScissorStack.popTo(drawContext, i3);
                            }
                        }
                    } finally {
                        MatrixUtil.pop(drawContext);
                        ScissorStack.popTo(drawContext, i3);
                    }

                    for (Setting setting5 : this.children) {
                        if (!(setting5.alphaAnimation.get() <= 0.001F)) {
                            setting5.renderOverlay(drawContext, i, i2, f);
                        }
                    }

                    ScissorStack.popTo(drawContext, i3);
                }
            }
        }
    }

    @Override
    public boolean isOverlayOpen() {
        if (this.nested) {
            for (Setting setting : this.children) {
                if (setting.isOverlayOpen()) {
                    return true;
                }
            }

            return false;
        } else {
            return this.popupOpen || this.popupAnimation.get() > 0.001F;
        }
    }

    public boolean isOverOverlay(double d, double d2) {
        if (this.nested) {
            return this.isOverChildOverlay(d, d2);
        } else {
            return !this.popupOpen && this.popupAnimation.get() <= 0.001F ? false : this.isPopupHovered(d, d2) || this.isOverChildOverlay(d, d2);
        }
    }

    public KeybindSetting findHoveredBindSetting(double d, double d2) {
        if (this.nested || this.popupOpen && this.isPopupHovered(d, d2)) {
            for (Setting setting : this.getVisibleChildren()) {
                if (setting instanceof GroupSetting groupSetting) {
                    KeybindSetting keybindSetting = groupSetting.findHoveredBindSetting(d, d2);
                    if (keybindSetting != null) {
                        return keybindSetting;
                    }
                } else if (setting instanceof KeybindSetting keybindSetting2 && this.isHovered(d, d2, setting.position, setting.size)) {
                    return keybindSetting2;
                }
            }
        }

        return null;
    }

    public ValueSetting<?> findHoveredValueSetting(double d, double d2) {
        if (this.nested || this.popupOpen && this.isPopupHovered(d, d2)) {
            for (Setting setting : this.getVisibleChildren()) {
                if (setting instanceof GroupSetting groupSetting) {
                    ValueSetting valueSetting = groupSetting.findHoveredValueSetting(d, d2);
                    if (valueSetting != null) {
                        return valueSetting;
                    }
                } else if (!(setting instanceof KeybindSetting)
                    && !(setting instanceof ColorSetting)
                    && !(setting instanceof ModeSetting)
                    && !(setting instanceof MultiSetting)
                    && setting instanceof ValueSetting valueSetting2
                    && this.isHovered(d, d2, setting.position, setting.size)) {
                    return valueSetting2;
                }
            }
        }

        return null;
    }

    public void closePopupImmediately() {
        if (this.nested) {
            this.closeChildOverlays();
        } else if (this.popupOpen || !(this.popupAnimation.get() <= 0.001F)) {
            this.popupOpen = false;
            this.popupAnimation.forceValue(0.0);
            Iterator iterator2 = this.children.iterator();

            while (iterator2.hasNext()) {
                ((Setting)iterator2.next()).close();
            }
        }
    }

    @Override
    public void resize(int i, int i2) {
        this.popupOpen = false;
        this.popupAnimation.forceValue(0.0);
        this.scroller.reset();
        Iterator iterator2 = this.children.iterator();

        while (iterator2.hasNext()) {
            ((Setting)iterator2.next()).resize(i, i2);
        }
    }

    @Override
    public void removed() {
        this.popupOpen = false;
        this.popupAnimation.forceValue(0.0);
        Iterator iterator2 = this.children.iterator();

        while (iterator2.hasNext()) {
            ((Setting)iterator2.next()).removed();
        }
    }

    @Override
    public void close() {
        this.closePopup();
    }

    private void openPopup() {
        for (Setting setting : this.children) {
            setting.resize(0, 0);
            setting.alphaAnimation.forceValue(setting.isVisible() ? 1.0 : 0.0);
            if (setting instanceof GroupSetting) {
                ((GroupSetting)setting).snapChildAlphas();
            }
        }

        this.popupWidthAnimation.forceValue(this.computePopupWidth());
        this.updatePopupBounds();
        this.scroller.reset();
        this.popupOpen = true;
    }

    private void snapChildAlphas() {
        for (Setting setting : this.children) {
            setting.alphaAnimation.forceValue(setting.isVisible() ? 1.0 : 0.0);
        }
    }

    private float computePopupWidth() {
        float f = 0.0F;

        for (Setting setting : this.children) {
            if (setting.isVisible()) {
                f = Math.max(f, setting.getPreferredWidth());
            }
        }

        float f2 = 18.0F + this.getScrollbarWidth();
        return Math.max(90.0F, Math.min(132.0F, f + f2));
    }

    private float getScrollbarWidth() {
        return 0.0F;
    }

    private void closePopup() {
        this.popupOpen = false;
        Iterator iterator2 = this.children.iterator();

        while (iterator2.hasNext()) {
            ((Setting)iterator2.next()).close();
        }
    }

    private void updatePopupBounds() {
        this.popupWidthAnimation.update();
        this.popupWidthAnimation.animate(this.computePopupWidth(), 0.2, Easings.EXPO_OUT);
        this.popupWidth = this.popupWidthAnimation.get();
        float f = this.hasNestedChild() ? 4.0F : 8.0F;
        this.popupHeight = f * 2.0F + this.getVisibleContentHeight();
        this.popupX = this.position.x + this.size.x - 3.0F;
        this.popupY = this.position.y;
    }

    private float layoutChildren(float f, GroupSetting.ChildVisitor childVisitor) {
        float f2 = f;
        Setting setting = null;
        float f3 = 0.0F;
        Setting setting2 = null;

        for (Setting setting3 : this.children) {
            float f4 = setting3.alphaAnimation.get();
            if (!(f4 <= 0.001F)) {
                if (setting != null && (isNestedGroup(setting) || isNestedGroup(setting3))) {
                    f2 += 4.0F * Math.min(f3, f4);
                }

                float f5 = setting3.resolveHeight(15.0F);
                if (childVisitor != null) {
                    childVisitor.visit(setting3, f2, f5);
                }

                f2 += f5 * f4;
                setting = setting3;
                f3 = f4;
                setting2 = setting3;
            }
        }

        if (setting2 != null && isNestedGroup(setting2)) {
            f2 += 6.0F;
        }

        return f2 - f;
    }

    private static boolean isNestedGroup(Setting<?> setting) {
        return setting instanceof GroupSetting && ((GroupSetting)setting).nested;
    }

    private boolean hasNestedChild() {
        for (Setting setting : this.children) {
            if (isNestedGroup(setting)) {
                return true;
            }
        }

        return false;
    }

    private float getVisibleContentHeight() {
        return this.hasNestedChild() ? Math.min(this.getContentHeight(), 280.0F) : Math.min(this.getContentHeight(), 150.0F);
    }

    private float getContentHeight() {
        return this.layoutChildren(0.0F, null);
    }

    private boolean isScrollable(float f, float f2) {
        return f > f2 + 0.5F;
    }

    private void applyEdgeFade(float f, float f2, float f3, float f4) {
        float f5 = f3 - f2;
        if (!(f5 <= 0.5F) && !(f2 <= 28.0F)) {
            float f6 = Math.min(1.0F, Math.max(0.0F, -f4 / 14.0F));
            float f7 = Math.min(1.0F, Math.max(0.0F, (f5 + f4) / 14.0F));
            if (!(f6 <= 0.001F) || !(f7 <= 0.001F)) {
                float f8 = f + f2;
                VerticalFadeMask.begin(f, f + 14.0F, f6, f8 - 14.0F, f8, f7);
            }
        }
    }

    private float computeEdgeFadeAlpha(float f, float f2, float f3, float f4) {
        if (!(f4 <= 0.0F) && !(f2 <= 0.0F)) {
            float f5 = Math.max(f, f3);
            float f6 = Math.min(f + f2, f3 + f4);
            if (f5 >= f6) {
                return 0.0F;
            }

            float f7 = (f5 + f6) * 0.5F;
            return VerticalFadeMask.getAlphaFactor(f7);
        } else {
            return 0.0F;
        }
    }

    private void drawContentBackground(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        Radius radius = Radius.all(6.6666665F);
        RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, f3, f4);
        renderUtil.rect(ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.05F), 0.3F * f5, radius);
        renderUtil.expand(Side.ALL, -0.5F);
        renderUtil.shadow(0.5F, ColorUtil.interpolate(ColorUtil.BLACK, ColorUtil.WHITE, 0.5F), 0.05F * f5, radius);
        renderUtil.expand(Side.ALL, 0.5F);
    }

    private boolean closeChildOverlays() {
        boolean flag = false;

        for (Setting setting : this.children) {
            if (setting.isOverlayOpen()) {
                if (setting instanceof ModeSetting) {
                    ((ModeSetting)setting).closeDropdown();
                    flag = true;
                } else if (setting instanceof MultiSetting) {
                    ((MultiSetting)setting).closeDropdown();
                    flag = true;
                } else if (setting instanceof ColorSetting) {
                    ((ColorSetting)setting).closeImmediately();
                    flag = true;
                } else if (setting instanceof WaveSetting) {
                    ((WaveSetting)setting).closeEditor();
                    flag = true;
                } else if (setting instanceof GroupSetting) {
                    ((GroupSetting)setting).closePopupImmediately();
                    flag = true;
                } else {
                    setting.close();
                    flag = true;
                }
            }
        }

        return flag;
    }

    private void closeOtherOverlays(Setting<?> setting) {
        for (Setting setting2 : this.children) {
            if (setting2 != setting) {
                if (setting2 instanceof ModeSetting) {
                    ((ModeSetting)setting2).closeDropdown();
                } else if (setting2 instanceof MultiSetting) {
                    ((MultiSetting)setting2).closeDropdown();
                } else if (setting2 instanceof WaveSetting) {
                    ((WaveSetting)setting2).closeEditor();
                } else if (setting2 instanceof GroupSetting) {
                    ((GroupSetting)setting2).closeOtherOverlays(setting);
                }
            }
        }
    }

    private boolean isOverChildOverlay(double d, double d2) {
        for (Setting setting : this.getVisibleChildren()) {
            if (setting instanceof ModeSetting && ((ModeSetting)setting).isOverDropdown(d, d2)) {
                return true;
            }

            if (setting instanceof MultiSetting && ((MultiSetting)setting).isOverDropdown(d, d2)) {
                return true;
            }

            if (setting instanceof ColorSetting && ((ColorSetting)setting).isMouseOverPicker(d, d2)) {
                return true;
            }

            if (setting instanceof TextSetting && ((TextSetting)setting).isOverEditor(d, d2)) {
                return true;
            }

            if (setting instanceof WaveSetting && ((WaveSetting)setting).isOverEditor(d, d2)) {
                return true;
            }

            if (setting instanceof GroupSetting && ((GroupSetting)setting).isOverOverlay(d, d2)) {
                return true;
            }
        }

        return false;
    }

    private boolean isPopupHovered(double d, double d2) {
        return this.popupHeight <= 0.0F ? false : this.isHovered(d, d2, this.popupX, this.popupY, this.popupWidth, this.popupHeight);
    }

    private boolean isGearHovered(double d, double d2) {
        if (!this.hasVisibleChildren()) {
            return false;
        }

        float f = this.position.x + this.size.x - 14.0F;
        float f2 = f - 5.0F - 7.5F;
        return this.isHovered(d, d2, f2 - 1.25F, this.position.y, 10.0, this.size.y);
    }

    private boolean isRowHovered(double d, double d2) {
        return this.isHovered(d, d2, this.position.x, this.position.y, this.size.x, this.size.y);
    }

    private boolean hasVisibleChildren() {
        for (Setting setting : this.children) {
            if (setting.isVisible()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasRenderableChildren() {
        for (Setting setting : this.children) {
            if (setting.isVisible() || setting.alphaAnimation.get() > 0.001F) {
                return true;
            }
        }

        return false;
    }

    private List<Setting<?>> getVisibleChildren() {
        ArrayList arrayList = new ArrayList(this.children.size());

        for (Setting setting : this.children) {
            if (setting.isVisible()) {
                arrayList.add(setting);
            }
        }

        return arrayList;
    }

    @Override
    public float getPreferredWidth() {
        if (!this.nested) {
            return super.getPreferredWidth();
        }

        float f = this.getLabelWidth() + 6.0F + 14.0F;

        for (Setting setting : this.children) {
            if (setting.isVisible()) {
                f = Math.max(f, setting.getPreferredWidth());
            }
        }

        return f + 8.0F;
    }

    @Override
    protected float getControlWidth() {
        if (this.nested) {
            return 14.0F;
        } else {
            return this.hasVisibleChildren() ? 14.0F + 12.5F : 14.0F + 0.0F;
        }
    }

    @FunctionalInterface
    interface ChildVisitor {
        void visit(Setting<?> setting, float f, float f2);
    }
}
