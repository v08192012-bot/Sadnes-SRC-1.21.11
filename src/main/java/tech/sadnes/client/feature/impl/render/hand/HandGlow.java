package tech.sadnes.client.feature.impl.render.hand;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.shader.GlowRenderer;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

@FeatureInfo(name = "Hand Glow", displayName = "feature.impl.render.hand.handglow", category = Category.VISUALS, subcategory = Subcategory.HAND)
public class HandGlow extends Feature {
    private final ModeSetting glowStyle = (ModeSetting)((Setting<?>)new ModeSetting("handglow.glowStyle", new String[]{"Bloom", "Flame", "Rimlight"})).setDisplayName("Glow style");
    private final BooleanSetting spring = (BooleanSetting)new BooleanSetting("handglow.spring", false)
        .setDisplayName("Spring")
        .setVisibleWhen(() -> this.glowStyle.is("Rimlight"));
    private final BooleanSetting hitDelay = (BooleanSetting)new BooleanSetting("handglow.hitDelay", false)
        .setDisplayName("Hit delay")
        .setVisibleWhen(() -> this.glowStyle.is("Rimlight"));
    private final NumberSetting hitDelayTime = (NumberSetting)new NumberSetting("handglow.hitDelayTime", 0.12F, 0.05F, 0.5F, 0.01F)
        .setDisplayName("Hit delay time")
        .setVisibleWhen(() -> this.glowStyle.is("Rimlight") && this.hitDelay.getValue() ? true : false);
    private final ModeSetting colorMode = (ModeSetting)((Setting<?>)new ModeSetting("handglow.colorMode", new String[]{"Client", "Static", "Fade", "Texture"})).setDisplayName("Color mode");
    private final ColorSetting color = (ColorSetting)new ColorSetting("handglow.color", -11141121).setDisplayName("Color").setVisibleWhen(() -> this.colorMode.is("Static"));
    private final ColorSetting fadeColor1 = (ColorSetting)new ColorSetting("handglow.fadeColor1", -29664)
        .setDisplayName("Fade color 1")
        .setVisibleWhen(() -> this.colorMode.is("Fade"));
    private final ColorSetting fadeColor2 = (ColorSetting)new ColorSetting("handglow.fadeColor2", -57312)
        .setDisplayName("Fade color 2")
        .setVisibleWhen(() -> this.colorMode.is("Fade"));
    private final NumberSetting intensity = (NumberSetting)new NumberSetting("handglow.intensity", 1.0F, 0.0F, 1.0F, 0.05F).setDisplayName("Intensity");
    private final NumberSetting radius = (NumberSetting)new NumberSetting("handglow.radius", 4.0F, 1.0F, 12.0F, 0.5F).setDisplayName("Radius");
    private final NumberSetting flameHeight = (NumberSetting)new NumberSetting("handglow.flameHeight", 0.25F, 0.05F, 0.6F, 0.01F)
        .setDisplayName("Flame height")
        .setVisibleWhen(() -> this.glowStyle.is("Flame"));
    private final NumberSetting flameTrail = (NumberSetting)new NumberSetting("handglow.flameTrail", 0.05F, 0.0F, 1.0F, 0.05F)
        .setDisplayName("Flame trail")
        .setVisibleWhen(() -> this.glowStyle.is("Flame"));

    public static HandGlow getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(HandGlow.class);
    }

    public void renderHandGlow(float f, MatrixStack matrixStack, HeldItemRenderer heldItemRenderer, ClientPlayerEntity clientPlayerEntity, int i) {
        if (this.isEnabled()) {
            HandOffset handOffset = HandOffset.getInstance();
            float f2 = handOffset == null ? 0.0F : handOffset.getEditorProgress();
            if (!(f2 >= 0.999F)) {
                int i2 = ColorUtil.multiplyAlpha(this.getGlowColor(), 1.0F - f2);
                if (this.glowStyle.is("Flame")) {
                    GlowRenderer.INSTANCE
                        .renderHandFlame(
                            matrixStack,
                            f,
                            heldItemRenderer,
                            clientPlayerEntity,
                            i,
                            i2,
                            this.radius.getValue(),
                            1.0F,
                            this.flameHeight.getValue(),
                            0.015F,
                            this.flameTrail.getValue(),
                            this.colorMode.is("Texture")
                        );
                } else if (this.glowStyle.is("Rimlight")) {
                    if (!this.spring.getValue() && !this.hitDelay.getValue()) {
                        GlowRenderer.INSTANCE.renderHandRim(matrixStack, f, heldItemRenderer, clientPlayerEntity, i, i2, this.radius.getValue());
                    } else {
                        GlowRenderer.INSTANCE
                            .renderHandGhost(
                                matrixStack,
                                f,
                                heldItemRenderer,
                                clientPlayerEntity,
                                i,
                                i2,
                                this.radius.getValue(),
                                this.spring.getValue(),
                                this.hitDelay.getValue(),
                                this.hitDelayTime.getValue()
                            );
                    }
                } else {
                    GlowRenderer.INSTANCE.renderHandGlow(matrixStack, f, heldItemRenderer, clientPlayerEntity, i, i2, this.radius.getValue());
                }
            }
        }
    }

    private int getGlowColor() {
        int i;
        label30: {
            label29: {
                label28: {
                    String string = this.colorMode.getValue();
                    byte b = -1;
                    switch (string.hashCode()) {
                        case -1808614770:
                            if (string.equals("Static")) {
                                break label29;
                            }
                            break;
                        case 2181788:
                            if (string.equals("Fade")) {
                                break label28;
                            }
                            break;
                        case 246836475:
                            if (string.equals("Texture")) {
                                b = 2;
                            }
                    }

                    switch (b) {
                        case 0:
                            break label29;
                        case 1:
                            break;
                        case 2:
                            i = -1;
                            break label30;
                        default:
                            i = ColorUtil.fade(10, 0, Theme.getPrimary(), ColorUtil.darken(Theme.getPrimary(), 0.25F));
                            break label30;
                    }
                }

                i = ColorUtil.fade(10, 0, this.fadeColor1.getValue(), this.fadeColor2.getValue());
                break label30;
            }

            i = this.color.getValue();
        }

        int i2 = i;
        return ColorUtil.multiplyAlpha(i2, this.intensity.getValue());
    }
}
