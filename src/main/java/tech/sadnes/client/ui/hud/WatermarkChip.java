package tech.sadnes.client.ui.hud;

import java.util.Locale;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public abstract class WatermarkChip implements IMinecraft {
    private static final float FONT_SIZE = 6.0F;
    public static final float SHAKE_AMPLITUDE = 2.5F;
    private static final float TEXT_GAP = 2.0F;
    private IntSupplier iconColorSupplier = () -> ColorUtil.WHITE;
    private IntSupplier warningColorSupplier = () -> ColorUtil.rgb(255, 90, 90);
    public final Animation visibilityAnimation = new Animation();
    public final Animation xAnimation = new Animation();
    public final Animation warningAnimation = new Animation();
    public final Animation widthAnimation = new Animation();
    public boolean xInitialized;
    public boolean widthInitialized;
    private static final long SHAKE_DURATION = 360L;
    private long shakeStartTime;
    private final String id;
    private final BooleanSetting setting;

    public WatermarkChip(String string, BooleanSetting booleanSetting) {
        this.id = string;
        this.setting = booleanSetting;
    }

    public String getId() {
        return this.id;
    }

    public boolean isVisible() {
        return this.setting.getValue() && (this.isActive() || isChatOrGuiOpen());
    }

    protected boolean isActive() {
        return true;
    }

    public static boolean isChatOrGuiOpen() {
        return mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof ClickGuiScreen;
    }

    public BooleanSetting getSetting() {
        return this.setting;
    }

    public void setColors(IntSupplier intSupplier, IntSupplier intSupplier2) {
        this.iconColorSupplier = intSupplier;
        this.warningColorSupplier = intSupplier2;
    }

    public void shake() {
        this.shakeStartTime = System.currentTimeMillis();
    }

    public float getShakeOffset() {
        if (this.shakeStartTime == 0L) {
            return 0.0F;
        } else {
            long l = System.currentTimeMillis() - this.shakeStartTime;
            if (l >= 360L) {
                this.shakeStartTime = 0L;
                return 0.0F;
            } else {
                float f = (float)l / 1000.0F;
                float f2 = 1.0F - (float)l / 360.0F;
                return (float)Math.sin(f * 70.0) * 2.5F * f2;
            }
        }
    }

    public abstract Icon getIcon();

    public String getDisplayText() {
        String string = this.getLabel();
        String string2 = this.getValueText();
        boolean flag;
        if (string != null) {
            flag = !string.isEmpty();
        } else {
            flag = false;
        }

        boolean flag2;
        if (string2 != null) {
            flag2 = !string2.isEmpty();
        } else {
            flag2 = false;
        }

        if (flag && flag2) {
            return string + " " + string2;
        } else if (flag) {
            return string;
        } else {
            return flag2 ? string2 : "";
        }
    }

    protected String getLabel() {
        return "";
    }

    protected String getValueText() {
        return "";
    }

    private String getUpperValueText() {
        String string = this.getValueText();
        return string != null && !string.isEmpty() ? string.toUpperCase(Locale.ROOT) : "";
    }

    public float getWarningLevel() {
        return 0.0F;
    }

    protected float getIconSize() {
        return 7.0F;
    }

    protected float getPadding() {
        return 0.0F;
    }

    public final float getWidth() {
        return MathUtil.roundToStep(this.widthAnimation.get(), 1.0F);
    }

    public final float getTargetWidth() {
        return this.getPadding() * 2.0F + this.getContentWidth();
    }

    private float getContentWidth() {
        float f = this.getIconSize();
        String string = this.getLabel();
        String string2 = this.getUpperValueText();
        boolean flag;
        if (string != null) {
            flag = !string.isEmpty();
        } else {
            flag = false;
        }

        boolean flag2;
        float f2;
        if (!string2.isEmpty()) {
            flag2 = true;
            f2 = 0.0F;
        } else {
            flag2 = false;
            if (!flag) {
                String string3 = this.getDisplayText();
                if (string3 != null && !string3.isEmpty()) {
                    return f + 2.5F + Fonts.INTER_MEDIUM.getWidth(string3, 6.0F);
                }

                return f;
            }

            f2 = 0.0F;
        }

        if (flag) {
            f2 = 0.0F + Fonts.INTER_MEDIUM.getWidth(string, 6.0F);
        }

        if (flag2) {
            if (flag) {
                f2 += 2.0F;
            }

            f2 += Fonts.INTER_MEDIUM.getWidth(string2, 6.0F);
        }

        return f2 <= 0.0F ? f : f + 2.5F + f2;
    }

    public final void render(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = MathUtil.roundToStep(f, 0.5F);
        float f6 = this.getIconSize();
        float f7 = this.getWidth() - this.getPadding() * 2.0F;
        float f8 = Math.max(0.0F, (f7 - this.getContentWidth()) / 2.0F);
        float f9 = MathUtil.roundToStep(f5 + this.getPadding() + f8, 0.5F);
        float f10 = f2 + f3 / 2.0F - f6 / 2.0F;
        String string = this.getLabel();
        String string2 = this.getUpperValueText();
        boolean flag;
        if (string != null) {
            flag = !string.isEmpty();
        } else {
            flag = false;
        }

        boolean flag2 = !string2.isEmpty();
        String string3 = !flag && !flag2 ? this.getDisplayText() : null;
        MsdfFont msdfFont = Fonts.INTER_MEDIUM;
        float f11 = f2 + f3 / 2.0F - 3.0F;
        int i = this.blendWarningColor(Theme.getTextColor(), f4 * 1.0F);
        if (string3 != null && !string3.isEmpty()) {
            this.renderIcon(drawContext, f9, f10, f6, f4);
            float f12 = MathUtil.roundToStep(f9 + f6 + 2.5F, 0.5F);
            msdfFont.draw(drawContext, string3, f12, f11, i, 6.0F);
        } else {
            float f13 = flag2 ? 2.0F : 0.0F;
            this.renderIcon(drawContext, f9, f10, f6, f4);
            float f14 = MathUtil.roundToStep(f9 + f6 + 2.5F, 0.5F);
            if (flag) {
                msdfFont.draw(drawContext, string, f14, f11, i, 6.0F);
                f14 = MathUtil.roundToStep(f14 + msdfFont.getWidth(string, 6.0F) + f13, 0.5F);
            }

            if (flag2) {
                msdfFont.draw(drawContext, string2, f14, f11, i, 6.0F);
            }
        }
    }

    private int blendWarningColor(int i, float f) {
        return ColorUtil.setAlphaf(ColorUtil.interpolate(i, this.warningColorSupplier.getAsInt(), this.warningAnimation.get()), f);
    }

    protected int getIconColor() {
        return 0;
    }

    public void renderIcon(DrawContext drawContext, float f, float f2, float f3, float f4) {
        new RenderUtil(drawContext, f, f2, f3, f3).icon(this.getIcon(), 0.0F, 0.0F, f3, this.resolveIconColor(f4));
    }

    protected int resolveIconColor(float f) {
        int i = this.getIconColor();
        if (i == 0) {
            i = this.iconColorSupplier.getAsInt();
        }

        return this.blendWarningColor(i, f);
    }
}
