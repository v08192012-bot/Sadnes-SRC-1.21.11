package tech.sadnes.client.feature.impl.render.esp;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class GlowElement extends EspElement {
    public final ModeSetting colorMode = this.addSetting(new ModeSetting("Color mode", new String[]{"Client", "Static"}));
    public final ColorSetting color = this.addSetting(new ColorSetting("Color", -11141121)).setVisibleWhen(() -> this.colorMode.is("Static"));
    public final NumberSetting intensity = this.addSetting(new NumberSetting("Intensity", 1.0F, 0.0F, 1.0F, 0.05F));
    public final NumberSetting radius = this.addSetting(new NumberSetting("Radius", 4.0F, 1.0F, 12.0F, 0.5F));

    public GlowElement(EspTargetType espTargetType) {
        super("Glow", false, espTargetType);
    }

    public int getColor() {
        int i = this.colorMode.is("Client") ? ColorUtil.fade(10, 0, Theme.getPrimary(), ColorUtil.darken(Theme.getPrimary(), 0.25F)) : this.color.getValue();
        return ColorUtil.multiplyAlpha(i, this.intensity.getValue());
    }

    @Override
    public void renderForEntity(
        DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7, float f8
    ) {
        this.size2.set(0.0F, 0.0F);
    }
}
