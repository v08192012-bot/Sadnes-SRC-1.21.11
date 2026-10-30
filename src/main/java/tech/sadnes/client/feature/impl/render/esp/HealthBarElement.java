package tech.sadnes.client.feature.impl.render.esp;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class HealthBarElement extends EspElement {
    public final ModeSetting mode = this.addSetting(new ModeSetting("Mode", new String[]{"Health", "Client", "Custom"}));
    public final ColorSetting topColor = this.addSetting(new ColorSetting("Top color", ColorUtil.GREEN)).setVisibleWhen(() -> this.mode.is("Custom"));
    public final ColorSetting bottomColor = this.addSetting(new ColorSetting("Bottom color", ColorUtil.darken(ColorUtil.GREEN, 0.25F)))
        .setVisibleWhen(() -> this.mode.is("Custom"));
    private final ModeSetting position = this.addHiddenSetting(
        new ModeSetting("Position", EspAnchor.namesOf(EspAnchor.LEFT, EspAnchor.RIGHT, EspAnchor.TOP, EspAnchor.BOTTOM))
    );

    public HealthBarElement(EspTargetType espTargetType) {
        super("HealthBar", true, espTargetType);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public EspAnchor[] getAllowedSides() {
        return new EspAnchor[]{EspAnchor.LEFT, EspAnchor.RIGHT, EspAnchor.TOP, EspAnchor.BOTTOM};
    }

    @Override
    public EspAnchor getSide() {
        return EspAnchor.fromName(this.position.getValue(), EspAnchor.LEFT);
    }

    @Override
    public void setSide(EspAnchor espAnchor) {
        this.position.setValue(espAnchor.getDisplayName());
    }

    @Override
    public void resetLayout() {
        this.position.setValue(EspAnchor.LEFT.getDisplayName());
    }

    @Override
    public void renderForEntity(
        DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7, float f8
    ) {
        if (!(f8 <= 0.001F) && !(f6 <= 0.0F)) {
            this.drawBar(drawContext, f3, f4, f3 + f5, f4 + f6, 1.0F, f8);
        } else {
            this.size2.set(0.0F, 0.0F);
        }
    }

    @Override
    public void renderBackground(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
        if (entity instanceof LivingEntity livingEntity) {
            float f6 = this.getHealthPercent(livingEntity);
            this.drawBar(drawContext, f, f2, f3, f4, f6, 1.0F);
        }
    }

    private void drawBar(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        int i = ColorUtil.multiplyAlpha(this.getTopColor(), f6);
        int i2 = ColorUtil.multiplyAlpha(this.getBottomColor(), f6);
        int i3 = ColorUtil.multiplyAlpha(ColorUtil.BLACK, f6);
        EspAnchor espAnchor = this.getSide();
        if (espAnchor.isHorizontal()) {
            float f7 = f4 - f2;
            if (f7 <= 0.0F) {
                this.size2.set(0.0F, 0.0F);
            } else {
                float f8 = espAnchor == EspAnchor.LEFT ? f - 2.0F : f3 + 0.5F;
                new RenderUtil(drawContext, f8, f2, 1.5F, f7).rect(i3);
                float f9 = MathHelper.clamp((f7 - 1.0F) * f5, 0.0F, f7 - 1.0F);
                float f10 = f2 + MathHelper.clamp((f7 - 1.0F) * (1.0F - f5), 0.0F, f7 - 1.0F) + 0.5F;
                new RenderUtil(drawContext, f8 + 0.5F, f10, 0.5F, f9).rectGradientX(i, i2);
                this.position2.set(f8, f2);
                this.size2.set(1.5F, f7);
            }
        } else {
            float f11 = f3 - f;
            if (f11 <= 0.0F) {
                this.size2.set(0.0F, 0.0F);
            } else {
                float f12 = espAnchor == EspAnchor.TOP ? f2 - 2.0F : f4 + 0.5F;
                new RenderUtil(drawContext, f, f12, f11, 1.5F).rect(i3);
                float f13 = MathHelper.clamp((f11 - 1.0F) * f5, 0.0F, f11 - 1.0F);
                new RenderUtil(drawContext, f + 0.5F, f12 + 0.5F, f13, 0.5F).rectGradientY(i, i2);
                this.position2.set(f, f12);
                this.size2.set(f11, 1.5F);
            }
        }
    }

    private float getHealthPercent(LivingEntity livingEntity) {
        float f = this.getHealth(livingEntity);
        float f2 = livingEntity.getMaxHealth();
        return f2 <= 0.0F ? 0.0F : MathHelper.clamp(f / f2, 0.0F, 1.0F);
    }

    private float getHealth(LivingEntity livingEntity) {
        return livingEntity instanceof PlayerEntity ? ServerManager.getHealth((PlayerEntity)livingEntity) : livingEntity.getHealth();
    }

    private int getTopColor() {
        String string = this.mode.getValue();
        byte b = -1;
        switch (string.hashCode()) {
            case -2137395588:
                if (string.equals("Health")) {
                    return ColorUtil.GREEN;
                }
                break;
            case 2021122027:
                if (string.equals("Client")) {
                    return Theme.getPrimary();
                }
                break;
            case 2029746065:
                if (string.equals("Custom")) {
                    b = 2;
                }
        }

        switch (b) {
            case 0:
                return ColorUtil.GREEN;
            case 1:
                return Theme.getPrimary();
            case 2:
                return this.topColor.getValue();
            default:
                return ColorUtil.WHITE;
        }
    }

    private int getBottomColor() {
        String string = this.mode.getValue();
        byte b = -1;
        switch (string.hashCode()) {
            case -2137395588:
                if (string.equals("Health")) {
                    return ColorUtil.RED;
                }
                break;
            case 2021122027:
                if (string.equals("Client")) {
                    return ColorUtil.darken(Theme.getPrimary(), 0.25F);
                }
                break;
            case 2029746065:
                if (string.equals("Custom")) {
                    b = 2;
                }
        }

        switch (b) {
            case 0:
                return ColorUtil.RED;
            case 1:
                return ColorUtil.darken(Theme.getPrimary(), 0.25F);
            case 2:
                return this.bottomColor.getValue();
            default:
                return ColorUtil.WHITE;
        }
    }
}
