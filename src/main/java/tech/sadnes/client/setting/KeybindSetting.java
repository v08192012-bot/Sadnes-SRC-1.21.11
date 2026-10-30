package tech.sadnes.client.setting;

import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.BindMode;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.util.Key;

public final class KeybindSetting extends ValueSetting<Integer> {
    public static final int NONE = -1;
    private String category;

    public KeybindSetting(String string, int i) {
        super(string, i);
    }

    public KeybindSetting(String string) {
        this(string, -1);
    }

    public KeybindSetting(GroupSetting groupSetting, String string, int i) {
        this(string, i);
        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public KeybindSetting(GroupSetting groupSetting, String string) {
        this(groupSetting, string, -1);
    }

    public KeybindSetting setValue(Integer integer) {
        super.setValue(integer);
        return this;
    }

    public KeybindSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public KeybindSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public KeybindSetting setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    public KeybindSetting withCategory(String string) {
        this.category = string;
        return this;
    }

    public KeybindSetting addBind(int i, BindMode bindMode) {
        super.mouseDragged(i, bindMode, i);
        return this;
    }

    public KeybindSetting addBind(ValueBind<Integer> valueBind) {
        super.addBind(valueBind);
        return this;
    }

    protected Integer sanitizeValue(Integer integer) {
        return integer == null ? -1 : integer;
    }

    public boolean matchesKey(int i) {
        if (i == -1) {
            return false;
        }

        if (i == this.getDefaultValue()) {
            return true;
        }

        for (ValueBind valueBind : this.getBinds()) {
            if (valueBind.matches(i)) {
                return true;
            }
        }

        return false;
    }

    public boolean isBound() {
        if (this.getDefaultValue() != -1) {
            return true;
        }

        for (ValueBind valueBind : this.getBinds()) {
            if (valueBind.getKey() != -1) {
                return true;
            }
        }

        return false;
    }

    public boolean isPressed() {
        if (MinecraftClient.getInstance().currentScreen == null && Key.isPressed(this.getDefaultValue())) {
            return true;
        }

        for (ValueBind valueBind : this.getBinds()) {
            if (this.isBindPressed(valueBind)) {
                return true;
            }
        }

        return false;
    }

    public boolean isBindPressed(ValueBind<Integer> valueBind) {
        if (valueBind.getKey() == -1) {
            return false;
        } else {
            return valueBind.getMode() == BindMode.TOGGLE ? this.isToggleBindActive(valueBind) : this.isBindActive(valueBind);
        }
    }

    @Override
    public float getBindRowHeight() {
        return 0.0F;
    }

    @Override
    public String formatBindValue(ValueBind<Integer> valueBind) {
        return valueBind != null ? valueBind.getDisplayString() : Key.getDisplayName(-1);
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        boolean flag = this.isUnbound();
        this.valueAnimation.update();
        this.valueAnimation.animate(flag ? 0.0 : 1.0, 0.3F, Easings.QUAD_OUT);
        float f2 = this.size.x / 2.0F;
        String string = SettingWidgets.ellipsize(this.getDisplayText(), f2 - 10.0F);
        float f3 = Math.min(f2, this.getBoxWidth(string));
        float f4 = flag ? 9.0F : 10.0F;
        float f5 = this.position.x + this.size.x - f3;
        float f6 = this.position.y + this.size.y / 2.0F - f4 / 2.0F;
        this.drawLabel(
            drawContext, this.position.x, this.position.y, this.size.x, this.size.y, this.size.x - f3 - 6.0F, this.isHovered(i, i2, this.position, this.size)
        );
        if (flag) {
            SettingWidgets.drawSurfaceLabel(drawContext, f5, f6, f3, f4, string, 0.35F, this.getAlpha());
        } else {
            SettingWidgets.drawBindBox(drawContext, f5, f6, f3, f4, string, this.valueAnimation.get(), this.getAlpha());
        }
    }

    private boolean isUnbound() {
        return !this.isBound();
    }

    private String getDisplayText() {
        int i = this.getBinds().size();
        if (this.getDefaultValue() != -1) {
            return i > 0 ? Key.getDisplayName(this.getDefaultValue()) + " +" + i : Key.getDisplayName(this.getDefaultValue());
        } else if (i > 0) {
            String string = this.getBinds().get(0).getDisplayString();
            return i > 1 ? string + " +" + (i - 1) : string;
        } else {
            return Language.translate("setting.bind.none", "N/A");
        }
    }

    private float getBoxWidth(String string) {
        float f = Fonts.INTER_MEDIUM.getWidth(string, 5.5F);
        return this.isUnbound() ? Math.max(14.0F, f + 6.0F) : Math.max(22.0F, f + 10.0F);
    }

    @Override
    protected float getControlWidth() {
        return this.getBoxWidth(this.getDisplayText());
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        return false;
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        return false;
    }

    public String getCategory() {
        return this.category;
    }
}
