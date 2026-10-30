package tech.sadnes.client.feature.impl.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.EntityHitResult;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.PerspectiveAnimation;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ButtonSetting;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.ColorToggleSetting;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.ui.RadialMenu;
import tech.sadnes.client.ui.screen.CrosshairEditorScreen;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

@FeatureInfo(
    name = "Crosshair",
    description = "feature.impl.render.crosshair",
    displayName = "feature.impl.render.crosshair",
    category = Category.VISUALS,
    subcategory = Subcategory.HUD_HELPERS
)
public final class Crosshair extends Feature {
    public static final int GRID_SIZE = 15;
    private final ModeSetting mode = new ModeSetting("crosshair.mode", new String[]{"dot", "cross", "custom"});
    private final NumberSetting size = new NumberSetting("crosshair.size", 3.0F, 0.5F, 15.0F, 0.5F).setVisibleWhen(() -> this.mode.is("dot"));
    private final NumberSetting gap = new NumberSetting("crosshair.gap", 3.0F, 0.0F, 15.0F, 0.5F).setVisibleWhen(() -> this.mode.is("cross"));
    private final NumberSetting length = new NumberSetting("crosshair.length", 5.0F, 1.0F, 20.0F, 0.5F).setVisibleWhen(() -> this.mode.is("cross"));
    private final NumberSetting thickness = new NumberSetting("crosshair.thickness", 1.5F, 0.5F, 5.0F, 0.1F).setVisibleWhen(() -> this.mode.is("cross"));
    private final BooleanSetting tStyle = new BooleanSetting("crosshair.tStyle", false).setVisibleWhen(() -> this.mode.is("cross"));
    private final NumberSetting customScale = new NumberSetting("crosshair.customScale", 2.0F, 1.0F, 6.0F, 1.0F).setVisibleWhen(() -> this.mode.is("custom"));
    private final Setting<Runnable> edit = new ButtonSetting("crosshair.edit", () -> mc.setScreen(new CrosshairEditorScreen(this)))
        .setVisibleWhen(() -> this.mode.is("custom"));
    private final TextSetting customData = new TextSetting("crosshair.customData", "").setVisibleWhen(() -> false);
    private final GroupSetting customColor = new GroupSetting("crosshair.customColor", false);
    private final ModeSetting colorMode = new ModeSetting(this.customColor, "crosshair.colorMode", new String[]{"solid", "rainbow", "gradient"});
    private final ColorSetting color = new ColorSetting(this.customColor, "crosshair.color", -1)
        .setVisibleWhen(() -> !this.colorMode.is("rainbow") ? true : false);
    private final ColorSetting secondColor = new ColorSetting(this.customColor, "crosshair.secondColor", -12417035)
        .setVisibleWhen(() -> this.colorMode.is("gradient"));
    private final NumberSetting rainbowSpeed = new NumberSetting(this.customColor, "crosshair.rainbowSpeed", 10.0F, 1.0F, 30.0F, 1.0F)
        .setVisibleWhen(() -> this.colorMode.is("rainbow"));
    private final ColorToggleSetting targetColor = new ColorToggleSetting(this.customColor, "crosshair.targetColor", false, -43691);
    private final GroupSetting effects = new GroupSetting("crosshair.effects", true);
    private final BooleanSetting centerDot = new BooleanSetting(this.effects, "crosshair.centerDot", true).setVisibleWhen(() -> this.mode.is("cross"));
    private final NumberSetting dotSize = new NumberSetting(this.effects, "crosshair.dotSize", 1.5F, 0.5F, 5.0F, 0.1F)
        .setVisibleWhen(() -> this.mode.is("cross") && this.centerDot.getValue() ? true : false);
    private final BooleanSetting outline = new BooleanSetting(this.effects, "crosshair.outline", true);
    private final NumberSetting outlineWidth = new NumberSetting(this.effects, "crosshair.outlineWidth", 1.0F, 0.5F, 3.0F, 0.1F)
        .setVisibleWhen(() -> this.outline.getValue());
    private final BooleanSetting shadow = new BooleanSetting(this.effects, "crosshair.shadow", false);
    private final NumberSetting shadowRadius = new NumberSetting(this.effects, "crosshair.shadowRadius", 4.0F, 1.0F, 10.0F, 0.5F)
        .setVisibleWhen(() -> this.shadow.getValue());
    private final NumberSetting glowStrength = new NumberSetting(this.effects, "crosshair.glowStrength", 0.6F, 0.1F, 1.0F, 0.05F)
        .setVisibleWhen(() -> this.shadow.getValue());
    private final BooleanSetting dynamic = new BooleanSetting(this.effects, "crosshair.dynamic", false).setVisibleWhen(() -> this.mode.is("cross"));
    private final NumberSetting dynamicAmount = new NumberSetting(this.effects, "crosshair.dynamicAmount", 4.0F, 1.0F, 15.0F, 0.5F)
        .setVisibleWhen(() -> this.mode.is("cross") && this.dynamic.getValue() ? true : false);
    private final BooleanSetting pulse = new BooleanSetting(this.effects, "crosshair.pulse", false);
    private final NumberSetting pulseSpeed = new NumberSetting(this.effects, "crosshair.pulseSpeed", 2.0F, 0.5F, 8.0F, 0.1F)
        .setVisibleWhen(() -> this.pulse.getValue());
    private final NumberSetting pulseAmount = new NumberSetting(this.effects, "crosshair.pulseAmount", 0.15F, 0.01F, 0.5F, 0.01F)
        .setVisibleWhen(() -> this.pulse.getValue());
    private final BooleanSetting rotate = new BooleanSetting(this.effects, "crosshair.rotate", false).setVisibleWhen(() -> !this.mode.is("dot") ? true : false);
    private final NumberSetting rotateSpeed = new NumberSetting(this.effects, "crosshair.rotateSpeed", 1.0F, 0.1F, 5.0F, 0.1F)
        .setVisibleWhen(() -> !this.mode.is("dot") && this.rotate.getValue() ? true : false);
    private boolean[] cachedPixels;
    private String cachedData;

    public static Crosshair getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(Crosshair.class);
    }

    public static boolean isActive() {
        Crosshair crosshair = getInstance();
        return crosshair != null && crosshair.isEnabled() && mc.player != null;
    }

    public static void renderCrosshair(DrawContext drawContext) {
        Crosshair crosshair = getInstance();
        if (crosshair != null) {
            crosshair.render(drawContext);
        }
    }

    public boolean[] getPixels() {
        String string = this.customData.getValue();
        if (this.cachedPixels == null || !string.equals(this.cachedData)) {
            this.cachedPixels = decodePixels(string);
            this.cachedData = string;
        }

        return this.cachedPixels;
    }

    public void setPixels(boolean[] booleans) {
        StringBuilder builder = new StringBuilder(225);

        for (boolean flag : booleans) {
            builder.append((char)(flag ? '1' : '0'));
        }

        String string = builder.toString();
        this.customData.setValue(string);
        this.cachedPixels = (boolean[])booleans.clone();
        this.cachedData = string;
    }

    private static boolean[] decodePixels(String string) {
        boolean[] booleans = new boolean[225];
        if (string == null) {
            return booleans;
        }

        int i = Math.min(string.length(), booleans.length);

        for (int i2 = 0; i2 < i; i2++) {
            booleans[i2] = string.charAt(i2) == '1';
        }

        return booleans;
    }

    private void render(DrawContext drawContext) {
        if (mc.player != null) {
            if (!PerspectiveAnimation.INSTANCE.isThirdPerson()) {
                if (!RadialMenu.isAnyOpen()) {
                    float f = mc.getWindow().getScaledWidth() / 2.0F;
                    float f2 = mc.getWindow().getScaledHeight() / 2.0F;
                    boolean flag;
                    if (this.customColor.isEnabled() && this.targetColor.isEnabled()) {
                        flag = mc.crosshairTarget instanceof EntityHitResult;
                    } else {
                        flag = false;
                    }

                    int i = flag ? this.targetColor.getValue() : this.getColor();
                    int i2 = ColorUtil.rgba(0, 0, 0, 200);
                    boolean flag2;
                    if (this.effects.isEnabled() && this.rotate.getValue()) {
                        flag2 = !this.mode.is("dot");
                    } else {
                        flag2 = false;
                    }

                    boolean flag3;
                    label116: {
                        if (this.effects.isEnabled() && this.pulse.getValue()) {
                            flag3 = true;
                        } else {
                            flag3 = false;
                            if (!flag2) {
                                break label116;
                            }
                        }

                        MatrixUtil.push(drawContext);
                        if (flag3) {
                            float f3 = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
                            float f4 = 1.0F + (float)Math.sin(f3 * this.pulseSpeed.getValue() * Math.PI) * this.pulseAmount.getValue();
                            MatrixUtil.scale(drawContext, f, f2, f4);
                        }

                        if (flag2) {
                            MatrixUtil.rotate(drawContext, f, f2, (float)(System.currentTimeMillis() * 0.1 * this.rotateSpeed.getValue().floatValue() % 360.0));
                        }
                    }

                    label83: {
                        label82: {
                            label81: {
                                String string = this.mode.getValue();
                                byte b = -1;
                                switch (string.hashCode()) {
                                    case -1349088399:
                                        if (string.equals("custom")) {
                                            b = 2;
                                        }
                                        break;
                                    case 99657:
                                        if (string.equals("dot")) {
                                            break label82;
                                        }
                                        break;
                                    case 94935104:
                                        if (string.equals("cross")) {
                                            break label81;
                                        }
                                }

                                switch (b) {
                                    case 0:
                                        break label82;
                                    case 1:
                                        break;
                                    case 2:
                                        this.drawCustom(drawContext, f, f2, i, i2);
                                    default:
                                        break label83;
                                }
                            }

                            this.drawCross(drawContext, f, f2, i, i2);
                            break label83;
                        }

                        this.drawDot(drawContext, f, f2, this.size.getValue(), i, i2);
                    }

                    if (this.effects.isEnabled() && this.centerDot.getValue() && this.mode.is("cross")) {
                        this.drawDot(drawContext, f, f2, this.dotSize.getValue(), i, i2);
                    }

                    if (flag2 || flag3) {
                        MatrixUtil.pop(drawContext);
                    }
                }
            }
        }
    }

    private void drawDot(DrawContext drawContext, float f, float f2, float f3, int i, int i2) {
        float f4 = f3 / 2.0F;
        if (this.isShadowEnabled()) {
            this.drawShadow(drawContext, f - f4, f2 - f4, f3, f3, Radius.all(f4), i);
        }

        if (this.isOutlineEnabled()) {
            float f5 = this.outlineWidth.getValue();
            new RenderUtil(drawContext, f - f4 - f5, f2 - f4 - f5, f3 + f5 * 2.0F, f3 + f5 * 2.0F).rect(i2, Radius.all(f4 + f5));
        }

        new RenderUtil(drawContext, f - f4, f2 - f4, f3, f3).rect(i, Radius.all(f4));
    }

    private void drawCross(DrawContext drawContext, float f, float f2, int i, int i2) {
        float f3 = Math.max(1.0F, Math.round(this.thickness.getValue()));
        float f4 = Math.max(1.0F, Math.round(this.length.getValue()));
        float f5 = Math.max(0.0F, this.gap.getValue() + this.getDynamicGap());
        float f6 = f3 / 2.0F;
        float f7 = Math.round(this.outlineWidth.getValue() * 2.0F) / 2.0F;
        float[][] floatses = this.tStyle.getValue()
            ? new float[][]{{f - f6, f2 + f5, f3, f4}, {f - f5 - f4, f2 - f6, f4, f3}, {f + f5, f2 - f6, f4, f3}}
            : new float[][]{
                {f - f6, f2 - f5 - f4, f3, f4},
                {f - f6, f2 + f5, f3, f4},
                {f - f5 - f4, f2 - f6, f4, f3},
                {f + f5, f2 - f6, f4, f3}
            };
        if (this.isShadowEnabled()) {
            for (float[] floats : floatses) {
                this.drawShadow(drawContext, floats[0], floats[1], floats[2], floats[3], Radius.zero(), i);
            }
        }

        if (this.isOutlineEnabled()) {
            for (float[] floats2 : floatses) {
                this.drawRect(drawContext, floats2[0] - f7, floats2[1] - f7, floats2[2] + f7 * 2.0F, floats2[3] + f7 * 2.0F, i2);
            }
        }

        for (float[] floats3 : floatses) {
            this.drawRect(drawContext, floats3[0], floats3[1], floats3[2], floats3[3], i);
        }
    }

    private void drawCustom(DrawContext drawContext, float f, float f2, int i, int i2) {
        boolean[] booleans = this.getPixels();
        int i3 = Math.max(1, Math.round(this.customScale.getValue()));
        int i4 = Math.round(f - 15 * i3 / 2.0F);
        int i5 = Math.round(f2 - 15 * i3 / 2.0F);
        boolean flag = this.isOutlineEnabled();
        boolean flag2 = this.isShadowEnabled();
        float f3 = this.outlineWidth.getValue();
        float[] floats = new float[16];
        float[] floats2 = new float[16];

        for (int i6 = 0; i6 <= 15; i6++) {
            floats[i6] = i4 + i6 * i3;
            floats2[i6] = i5 + i6 * i3;
        }

        List list = mergeRects(booleans);
        int i7 = list.size();
        if (flag2 || flag) {
            for (int i8 = 0; i8 < i7; i8++) {
                int[] ints = (int[])list.get(i8);
                float f4 = floats[ints[0]];
                float f5 = floats[ints[0] + ints[2]] - f4;
                float f6 = floats2[ints[1]];
                float f7 = floats2[ints[1] + ints[3]] - f6;
                if (flag2) {
                    this.drawShadow(drawContext, f4, f6, f5, f7, Radius.zero(), i);
                }

                if (flag) {
                    this.drawRect(drawContext, f4 - f3, f6 - f3, f5 + f3 * 2.0F, f7 + f3 * 2.0F, i2);
                }
            }
        }

        for (int i9 = 0; i9 < i7; i9++) {
            int[] ints2 = (int[])list.get(i9);
            float f8 = floats[ints2[0]];
            float f9 = floats[ints2[0] + ints2[2]] - f8;
            float f10 = floats2[ints2[1]];
            float f11 = floats2[ints2[1] + ints2[3]] - f10;
            this.drawRect(drawContext, f8, f10, f9, f11, i);
        }
    }

    private static List<int[]> mergeRects(boolean[] booleans) {
        ArrayList arrayList = new ArrayList();
        boolean[] booleans2 = new boolean[225];

        for (int i = 0; i < 15; i++) {
            for (int i2 = 0; i2 < 15; i2++) {
                int i3 = i * 15 + i2;
                if (booleans[i3] && !booleans2[i3]) {
                    int i4 = 1;

                    while (i2 + i4 < 15 && booleans[i * 15 + i2 + i4] && !booleans2[i * 15 + i2 + i4]) {
                        i4++;
                    }

                    int i5 = 1;

                    int i6;
                    label67:
                    while (true) {
                        if (i + i5 >= 15) {
                            i6 = i;
                            break;
                        }

                        for (int i7 = 0; i7 < i4; i7++) {
                            int i8 = (i + i5) * 15 + i2 + i7;
                            if (!booleans[i8] || booleans2[i8]) {
                                i6 = i;
                                break label67;
                            }
                        }

                        i5++;
                    }

                    while (i6 < i + i5) {
                        for (int i9 = i2; i9 < i2 + i4; i9++) {
                            booleans2[i6 * 15 + i9] = true;
                        }

                        i6++;
                    }

                    arrayList.add(new int[]{i2, i, i4, i5});
                }
            }
        }

        return arrayList;
    }

    private void drawShadow(DrawContext drawContext, float f, float f2, float f3, float f4, Radius radius, int i) {
        float f5 = this.shadowRadius.getValue();
        float f6 = this.glowStrength.getValue();
        new RenderUtil(drawContext, f, f2, f3, f4).border(f5 * 1.6F, i, 0.35F * f6, radius);
        new RenderUtil(drawContext, f, f2, f3, f4).border(f5 * 0.7F, i, 0.8F * f6, radius);
    }

    private int getColor() {
        if (!this.customColor.isEnabled()) {
            return Theme.getPrimary();
        }

        String string = this.colorMode.getValue();
        byte b = -1;
        switch (string.hashCode()) {
            case 89650992:
                if (string.equals("gradient")) {
                    b = 1;
                }
                break;
            case 973576630:
                if (string.equals("rainbow")) {
                    return ColorUtil.rainbow(this.rainbowSpeed.getValue().intValue(), 0, 0.7F, 1.0F, 1.0F);
                }
        }

        switch (b) {
            case 0:
                return ColorUtil.rainbow(this.rainbowSpeed.getValue().intValue(), 0, 0.7F, 1.0F, 1.0F);
            case 1:
                float f = (float)(Math.sin(System.currentTimeMillis() / 2000.0) * 0.5 + 0.5);
                return ColorUtil.interpolate(this.color.getValue(), this.secondColor.getValue(), f);
            default:
                return this.color.getValue();
        }
    }

    private float getDynamicGap() {
        if (this.effects.isEnabled() && this.dynamic.getValue() && mc.player != null) {
            float f = mc.player.getAttackCooldownProgress(1.5F);
            return f >= 1.0F ? 0.0F : this.dynamicAmount.getValue() * (1.0F - f);
        } else {
            return 0.0F;
        }
    }

    private boolean isOutlineEnabled() {
        return this.effects.isEnabled() && this.outline.getValue();
    }

    private boolean isShadowEnabled() {
        return this.effects.isEnabled() && this.shadow.getValue();
    }

    private void drawRect(DrawContext drawContext, float f, float f2, float f3, float f4, int i) {
        new RenderUtil(drawContext, f, f2, f3, f4).rect(i);
    }
}
