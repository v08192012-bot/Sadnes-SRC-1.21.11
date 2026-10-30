package tech.sadnes.client.setting;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.BindMode;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.StatusIconUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public class BooleanSetting extends ValueSetting<Boolean> {
    private final Map<ValueBind<Boolean>, Animation> bindAnimations = new IdentityHashMap<>();

    public BooleanSetting(String string, boolean flag) {
        super(string, flag);
        this.setValue(flag);
    }

    public BooleanSetting(String string) {
        this(string, false);
    }

    public BooleanSetting(GroupSetting groupSetting, String string, boolean flag) {
        this(string, flag);
        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public BooleanSetting(GroupSetting groupSetting, String string) {
        this(groupSetting, string, false);
    }

    public static BooleanSetting of(String string, boolean flag) {
        return new BooleanSetting(string, flag);
    }

    public static BooleanSetting of(String string) {
        return new BooleanSetting(string, false);
    }

    public BooleanSetting setValue(Boolean flag) {
        super.setValue(flag);
        return this;
    }

    public BooleanSetting setIcon(Icon icon) {
        super.setIcon(icon);
        return this;
    }

    public BooleanSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public BooleanSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public BooleanSetting setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.getDefaultValue());
    }

    public BooleanSetting toggle() {
        return !this.isEnabled() ? this.setValue(true) : this.setValue(false);
    }

    public BooleanSetting bind(int i, BindMode bindMode) {
        if (!this.isEnabled()) {
            super.mouseDragged(i, bindMode, true);
            return this;
        } else {
            super.mouseDragged(i, bindMode, false);
            return this;
        }
    }

    public BooleanSetting bind(int i, BindMode bindMode, boolean flag) {
        super.mouseDragged(i, bindMode, flag);
        return this;
    }

    public BooleanSetting addBind(ValueBind<Boolean> valueBind) {
        super.addBind(valueBind);
        return this;
    }

    public ValueBind<Boolean> createBind(Boolean flag) {
        return !this.isEnabled() ? super.createBind(true) : super.createBind(false);
    }

    protected Boolean sanitizeValue(Boolean flag) {
        return Boolean.TRUE.equals(flag);
    }

    @Override
    public String formatBindValue(ValueBind<Boolean> valueBind) {
        return this.isBindEnabled(valueBind) ? "On" : "Off";
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        ValueBind valueBind = this.editingBind;
        float f2 = this.position.x;
        float f3 = this.position.y;
        float f4 = this.size.x;
        float f5 = this.size.y;
        boolean flag = valueBind != null;
        boolean flag2 = flag ? this.isBindEnabled(valueBind) : Boolean.TRUE.equals(this.getValue());
        float f6;
        if (flag) {
            this.cleanupBindAnimations();
            f6 = this.animateBind(valueBind, flag2);
        } else {
            this.valueAnimation.update();
            this.valueAnimation.animate(flag2 ? 1.0 : 0.0, 0.5, Easings.EXPO_OUT);
            f6 = this.valueAnimation.get();
        }

        if (flag) {
            new RenderUtil(drawContext, f2, f3, f4, f5)
                .icon(Icon.CHECK, 3.0F, f5 / 2.0F - 4.25F, 8.5F, this.getSecondaryColor())
                .textMiddleLeft(Fonts.INTER_MEDIUM, "Value", 17.0F, 0.0F, this.getSecondaryColor(), 6.5F);
        } else {
            float f7 = this.drawIcon(drawContext, f2, f3, f5, 6.0F);
            boolean flag3 = this.hasSettingInfo();
            boolean flag4 = this.isDependencyActive();
            float f8 = f7 + StatusIconUtil.drawIcons(drawContext, f2 + f7, f3, f5, 6.0F, this.getAlpha(), flag3, flag4, this.getDependencyStatus());
            int i3 = ColorUtil.multiplyAlpha(
                ColorUtil.interpolate(Theme.withAlpha(Theme.getTextColor(), 0.5F), Theme.getTextColor(), f6), this.getAlpha()
            );
            int i4 = StatusIconUtil.getNameColor(i3, this.getAlpha(), flag3, flag4, this.getDependencyStatus());
            new RenderUtil(drawContext, f2, f3, f4, f5)
                .scrollingTextMiddle(
                    Fonts.INTER_MEDIUM,
                    Language.translate(this.getName()),
                    f8,
                    0.0F,
                    f4 - this.getControlWidth() - 6.0F - f8,
                    i4,
                    6.0F,
                    this.isHovered(i, i2, this.position, this.size)
                );
        }

        float f9 = f2 + f4 - this.getControlWidth();
        if (flag) {
            this.drawSwitch(drawContext, f9 - 4.0F, f3, f5, f6);
        } else {
            this.drawSwitch(drawContext, f9 - 0.0F, f3, f5, f6);
        }
    }

    @Override
    protected float getControlWidth() {
        return 14.0F;
    }

    protected void drawSwitch(DrawContext drawContext, float f, float f2, float f3, float f4) {
        SettingWidgets.drawDefaultSwitch(drawContext, f, f2 + f3 / 2.0F - 4.5F, f4, this.getAlpha());
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        ValueBind valueBind = this.editingBind;
        if (!this.isLeftButton(click.button())) {
            return false;
        }

        if (!this.isHovered(click.x(), click.y(), this.position.x, this.position.y, this.size.x, this.size.y)) {
            return false;
        }

        if (valueBind != null) {
            if (!this.isBindEnabled(valueBind)) {
                this.setBindEnabled(valueBind, true);
                return true;
            } else {
                this.setBindEnabled(valueBind, false);
                return true;
            }
        } else {
            this.toggle();
            return true;
        }
    }

    private boolean isBindEnabled(ValueBind<Boolean> valueBind) {
        return valueBind != null && Boolean.TRUE.equals(valueBind.getValue());
    }

    private float animateBind(ValueBind<Boolean> valueBind, boolean flag) {
        if (valueBind == null) {
            return flag ? 1.0F : 0.0F;
        } else {
            Animation animation = this.bindAnimations.computeIfAbsent(valueBind, valueBind2 -> {
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
            if (flag) {
                animation.animate(1.0, 0.5, Easings.EXPO_OUT);
                return animation.get();
            } else {
                animation.animate(0.0, 0.5, Easings.EXPO_OUT);
                return animation.get();
            }
        }
    }

    private void cleanupBindAnimations() {
        this.bindAnimations.keySet().removeIf(valueBind -> !this.getBinds().contains(valueBind));
    }

    private void setBindEnabled(ValueBind<Boolean> valueBind, boolean flag) {
        if (valueBind != null) {
            super.setBindValue(valueBind, flag);
        }
    }
}
