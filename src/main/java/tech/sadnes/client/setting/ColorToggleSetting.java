package tech.sadnes.client.setting;

import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public class ColorToggleSetting extends ColorSetting {
    private static final float SWATCH_GAP = 4.0F;
    private boolean enabled;

    public ColorToggleSetting(String string, boolean flag, int i) {
        super(string, i);
        this.enabled = flag;
        this.valueAnimation.forceValue(flag ? 1.0 : 0.0);
    }

    public ColorToggleSetting(GroupSetting groupSetting, String string, boolean flag, int i) {
        this(string, flag, i);
        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public ColorToggleSetting setEnabled(boolean flag) {
        if (this.enabled == flag) {
            return this;
        }

        this.enabled = flag;
        this.notifyChange();
        return this;
    }

    public ColorToggleSetting toggle() {
        return !this.enabled ? this.setEnabled(true) : this.setEnabled(false);
    }

    public ColorToggleSetting setValue(Integer integer) {
        super.setValue(integer);
        return this;
    }

    public ColorToggleSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public ColorToggleSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public ColorToggleSetting setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        if (this.editingBind != null) {
            super.render(drawContext, i, i2, f);
        } else {
            float f2 = this.position.x;
            float f3 = this.position.y;
            float f4 = this.size.x;
            float f5 = this.size.y;
            this.valueAnimation.update();
            this.valueAnimation.animate(this.enabled ? 1.0 : 0.0, 0.5, Easings.EXPO_OUT);
            float f6 = this.valueAnimation.get();
            float f7 = f2 + f4 - 14.0F;
            float f8 = f3 + f5 / 2.0F - 4.5F;
            float f9 = f7 - 4.0F - 10.0F;
            float f10 = f3 + f5 / 2.0F - 5.0F;
            int i3 = ColorUtil.multiplyAlpha(ColorUtil.interpolate(Theme.withAlpha(Theme.getTextColor(), 0.5F), Theme.getTextColor(), f6), this.getAlpha());
            new RenderUtil(drawContext, f2, f3, f4, f5)
                .scrollingTextMiddle(
                    Fonts.INTER_MEDIUM,
                    Language.translate(this.getName()),
                    0.0F,
                    0.0F,
                    f9 - f2 - 6.0F,
                    i3,
                    6.0F,
                    this.isHovered(i, i2, this.position, this.size)
                );
            SettingWidgets.drawColorSwatch(drawContext, f9, f10, 10.0F, 10.0F, this.getValue(), this.getAlpha());
            SettingWidgets.drawDefaultSwitch(drawContext, f7, f8, f6, this.getAlpha());
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (this.editingBind != null) {
            return super.mouseClicked(click, flag);
        } else if (!this.isLeftButton(click.button()) && !this.isRightButton(click.button())) {
            return false;
        } else if (this.isPickerOpen()) {
            return this.handlePickerClick(click);
        } else {
            float f = this.position.x;
            float f2 = this.position.y;
            float f3 = this.size.x;
            float f4 = this.size.y;
            if (!this.isHovered(click.x(), click.y(), f, f2, f3, f4)) {
                return false;
            } else {
                float f5 = f + f3 - 14.0F - 4.0F - 10.0F;
                float f6 = f2 + f4 / 2.0F - 5.0F;
                boolean flag2 = this.isHovered(click.x(), click.y(), f5 - 1.0F, f6 - 1.0F, 12.0, 12.0);
                if (!this.isRightButton(click.button()) && !flag2) {
                    this.toggle();
                    return true;
                } else {
                    this.openPicker();
                    return true;
                }
            }
        }
    }

    @Override
    protected float getControlWidth() {
        return 28.0F;
    }
}
