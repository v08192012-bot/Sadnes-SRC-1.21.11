package tech.sadnes.client.feature.impl.render.esp;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class ChamsElement extends EspElement {
    public final BooleanSetting model = this.addSetting(new BooleanSetting("Model", false));
    public final ModeSetting mode = this.addSetting(new ModeSetting("Mode", new String[]{"Cosmic", "Flat", "Glass", "Blur", "Ghost", "Outline"}));
    public final ModeSetting colorMode = this.addSetting(new ModeSetting("Color mode", new String[]{"Client", "Static"}));
    public final ColorSetting color = this.addSetting(new ColorSetting("Color", -5218320)).setVisibleWhen(() -> this.colorMode.is("Static"));
    public final NumberSetting intensity = this.addSetting(new NumberSetting("Intensity", 1.0F, 0.0F, 1.0F, 0.05F));
    public final NumberSetting speed = this.addSetting(new NumberSetting("Speed", 1.0F, 0.1F, 3.0F, 0.1F)).setVisibleWhen(() -> this.mode.is("Cosmic"));
    public final NumberSetting scale = this.addSetting(new NumberSetting("Scale", 1.0F, 0.5F, 4.0F, 0.1F)).setVisibleWhen(() -> this.mode.isNot("Flat"));

    public ChamsElement(EspTargetType espTargetType) {
        super("Chams", false, espTargetType);
    }

    public int getColor() {
        int i = this.colorMode.is("Client") ? ColorUtil.fade(10, 0, Theme.getPrimary(), ColorUtil.darken(Theme.getPrimary(), 0.25F)) : this.color.getValue();
        return ColorUtil.multiplyAlpha(i, this.intensity.getValue());
    }

    public int getModeId() {
        if (this.mode.is("Glass")) {
            return 1;
        } else if (this.mode.is("Blur")) {
            return 2;
        } else if (this.mode.is("Cosmic")) {
            return 3;
        } else if (this.mode.is("Ghost")) {
            return 4;
        } else {
            return this.mode.is("Outline") ? 5 : 0;
        }
    }

    @Override
    public void renderForEntity(
        DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7, float f8
    ) {
        this.size2.set(0.0F, 0.0F);
    }
}
