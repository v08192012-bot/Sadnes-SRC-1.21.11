package tech.sadnes.client.setting;

import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;

public class CheckboxSetting extends BooleanSetting {
    public CheckboxSetting(String string, boolean flag) {
        super(string, flag);
    }

    public CheckboxSetting(String string) {
        this(string, false);
    }

    public CheckboxSetting(GroupSetting groupSetting, String string, boolean flag) {
        super(groupSetting, string, flag);
    }

    public CheckboxSetting(GroupSetting groupSetting, String string) {
        this(groupSetting, string, false);
    }

    public static CheckboxSetting of(String string, boolean flag) {
        return new CheckboxSetting(string, flag);
    }

    public static CheckboxSetting of(String string) {
        return new CheckboxSetting(string, false);
    }

    public CheckboxSetting setValue(Boolean flag) {
        super.setValue(flag);
        return this;
    }

    public CheckboxSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public CheckboxSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public CheckboxSetting setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    @Override
    protected float getControlWidth() {
        return 8.0F;
    }

    @Override
    protected void drawSwitch(DrawContext drawContext, float f, float f2, float f3, float f4) {
        SettingWidgets.drawCheckbox(drawContext, f, f2 + f3 / 2.0F - 4.0F, 8.0F, f4, this.getAlpha());
    }
}
