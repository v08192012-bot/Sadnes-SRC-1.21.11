package tech.sadnes.client.feature.impl.render.esp;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class BoxElement extends EspElement {
    public static final float BOX_EXPAND = 0.05F;
    public final ModeSetting mode = this.addSetting(new ModeSetting("Mode", new String[]{"Box", "Corners", "3D"}));
    public final ModeSetting colorMode = this.addSetting(new ModeSetting("Color mode", new String[]{"Client", "Static"}));
    public final ColorSetting color = this.addSetting(new ColorSetting("Color", -1)).setVisibleWhen(() -> this.colorMode.is("Static"));
    public final MultiSetting render = this.addSetting(new MultiSetting("Render", BooleanSetting.of("Lines", true), BooleanSetting.of("Fill", true)))
        .setVisibleWhen(() -> this.mode.is("3D"));

    public BoxElement(EspTargetType espTargetType) {
        super("Box", true, espTargetType);
    }

    public boolean isBox3d() {
        return this.isActive() && this.mode.is("3D");
    }

    public int getEntityColor(Entity entity, int i, float f) {
        boolean flag;
        if (entity instanceof PlayerEntity playerEntity) {
            flag = Sadnes.getInstance().getFriendManager().isFriend(playerEntity.getGameProfile().name());
        } else {
            flag = false;
        }

        if (flag) {
            return ColorUtil.multiplyAlpha(ColorUtil.GREEN, f);
        }

        int i2 = this.getBaseColor(i);
        return ColorUtil.multiplyAlpha(i2, f);
    }

    private int getBaseColor(int i) {
        if (this.colorMode.is("Client")) {
            int i2 = Theme.getPrimary();
            return ColorUtil.fade(10, i, i2, ColorUtil.darken(i2, 0.25F));
        } else {
            return this.color.getValue();
        }
    }

    @Override
    public void renderForEntity(
        DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7, float f8
    ) {
        if (!(f8 <= 0.001F) && !(f5 <= 0.0F) && !(f6 <= 0.0F) && !this.mode.is("3D")) {
            int i3 = ColorUtil.multiplyAlpha(this.getBaseColor(0), f8);
            if (this.mode.is("Box")) {
                this.drawBox(drawContext, f3, f4, f5, f6, i3, f8);
            } else if (this.mode.is("Corners")) {
                this.drawCorners(drawContext, f3, f4, f5, f6, i3, f8);
            }

            this.position2.set(f3, f4);
            this.size2.set(f5, f6);
        } else {
            this.size2.set(0.0F, 0.0F);
        }
    }

    @Override
    public void renderBackground(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
        float f6 = f3 - f;
        float f7 = f4 - f2;
        if (!(f6 <= 0.0F) && !(f7 <= 0.0F)) {
            if (!this.mode.is("3D")) {
                int i = this.getEntityColor(entity, 0, 1.0F);
                if (this.mode.is("Box")) {
                    this.drawBox(drawContext, f, f2, f6, f7, i, 1.0F);
                } else {
                    if (this.mode.is("Corners")) {
                        this.drawCorners(drawContext, f, f2, f6, f7, i, 1.0F);
                    }
                }
            }
        }
    }

    private void drawBox(DrawContext drawContext, float f, float f2, float f3, float f4, int i, float f5) {
        int i2 = ColorUtil.multiplyAlpha(ColorUtil.BLACK, f5);
        new RenderUtil(drawContext, f, f2, f3, 1.5F).rect(i2);
        new RenderUtil(drawContext, f, f2, 1.5F, f4).rect(i2);
        new RenderUtil(drawContext, f + f3 - 1.5F, f2, 1.5F, f4).rect(i2);
        new RenderUtil(drawContext, f, f2 + f4 - 1.5F, f3, 1.5F).rect(i2);
        new RenderUtil(drawContext, f + 0.5F, f2 + 0.5F, f3 - 0.5F * 2.0F, 1.5F - 0.5F * 2.0F).rect(i);
        new RenderUtil(drawContext, f + 0.5F, f2 + 0.5F, 1.5F - 0.5F * 2.0F, f4 - 0.5F * 2.0F).rect(i);
        new RenderUtil(drawContext, f + f3 - 1.5F + 0.5F, f2 + 0.5F, 1.5F - 0.5F * 2.0F, f4 - 0.5F * 2.0F).rect(i);
        new RenderUtil(drawContext, f + 0.5F, f2 + f4 - 1.5F + 0.5F, f3 - 0.5F * 2.0F, 1.5F - 0.5F * 2.0F).rect(i);
    }

    private void drawCorners(DrawContext drawContext, float f, float f2, float f3, float f4, int i, float f5) {
        int i2 = ColorUtil.multiplyAlpha(ColorUtil.BLACK, f5);
        float f6 = Math.min(10.0F, f3 / 3.0F);
        new RenderUtil(drawContext, f, f2, f6, 1.5F).rect(i2);
        new RenderUtil(drawContext, f, f2, 1.5F, f6).rect(i2);
        new RenderUtil(drawContext, f + 0.5F, f2 + 0.5F, f6 - 0.5F * 2.0F, 1.5F - 0.5F * 2.0F).rect(i);
        new RenderUtil(drawContext, f + 0.5F, f2 + 0.5F, 1.5F - 0.5F * 2.0F, f6 - 0.5F * 2.0F).rect(i);
        new RenderUtil(drawContext, f + f3 - f6, f2, f6, 1.5F).rect(i2);
        new RenderUtil(drawContext, f + f3 - 1.5F, f2, 1.5F, f6).rect(i2);
        new RenderUtil(drawContext, f + f3 - f6 + 0.5F, f2 + 0.5F, f6 - 0.5F * 2.0F, 1.5F - 0.5F * 2.0F).rect(i);
        new RenderUtil(drawContext, f + f3 - 1.5F + 0.5F, f2 + 0.5F, 1.5F - 0.5F * 2.0F, f6 - 0.5F * 2.0F).rect(i);
        new RenderUtil(drawContext, f + f3 - f6, f2 + f4 - 1.5F, f6, 1.5F).rect(i2);
        new RenderUtil(drawContext, f + f3 - 1.5F, f2 + f4 - f6, 1.5F, f6).rect(i2);
        new RenderUtil(drawContext, f + f3 - f6 + 0.5F, f2 + f4 - 1.5F + 0.5F, f6 - 0.5F * 2.0F, 1.5F - 0.5F * 2.0F).rect(i);
        new RenderUtil(drawContext, f + f3 - 1.5F + 0.5F, f2 + f4 - f6 + 0.5F, 1.5F - 0.5F * 2.0F, f6 - 0.5F * 2.0F).rect(i);
        new RenderUtil(drawContext, f, f2 + f4 - 1.5F, f6, 1.5F).rect(i2);
        new RenderUtil(drawContext, f, f2 + f4 - f6, 1.5F, f6).rect(i2);
        new RenderUtil(drawContext, f + 0.5F, f2 + f4 - 1.5F + 0.5F, f6 - 0.5F * 2.0F, 1.5F - 0.5F * 2.0F).rect(i);
        new RenderUtil(drawContext, f + 0.5F, f2 + f4 - f6 + 0.5F, 1.5F - 0.5F * 2.0F, f6 - 0.5F * 2.0F).rect(i);
    }
}
