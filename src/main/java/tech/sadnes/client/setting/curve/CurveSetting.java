package tech.sadnes.client.setting.curve;

import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.BezierCurveRenderer;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.setting.GroupSetting;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class CurveSetting extends Setting<BezierCurve> {
    private static final float POPUP_WIDTH = 152.0F;
    private static final float POPUP_PADDING = 8.0F;
    private static final float GRAPH_HEIGHT = 86.0F;
    private static final float PRESET_HEIGHT = 13.0F;
    private static final float HINT_HEIGHT = 10.0F;
    private static final float SECTION_GAP = 6.0F;
    private static final float PRESET_GAP = 4.0F;
    private static final float HANDLE_RADIUS = 2.6F;
    private static final float PREVIEW_WIDTH = 30.0F;
    private static final float PREVIEW_HEIGHT = 11.0F;
    private static final float SNAP_THRESHOLD = 0.06F;
    private static final float EXACT_THRESHOLD = 0.008F;
    private static final CurvePreset[] PRESETS = CurvePreset.values();
    private static final BezierCurve[] PRESET_CURVES = new BezierCurve[PRESETS.length];
    private final Animation openAnimation = new Animation();
    private final Animation snapAnimation = new Animation();
    private final Animation morphAnimation = new Animation();
    private final Animation[] handleAnimations = new Animation[]{new Animation(), new Animation(), new Animation(), new Animation()};
    private final Animation[] presetAnimations = new Animation[PRESETS.length];
    private final float[] points = new float[8];
    private final float[] snapPoints = new float[8];
    private final BezierCurve morphFrom = CurvePreset.MAGNET.createCurve();
    private BezierCurve morphTo;
    private boolean open;
    private boolean morphing;
    private int draggingHandle = -1;
    private CurvePreset snapPreset;
    private float popupX;
    private float popupY;

    public CurveSetting(String string) {
        this(null, string, CurvePreset.MAGNET.createCurve());
    }

    public CurveSetting(String string, BezierCurve bezierCurve) {
        this(null, string, bezierCurve);
    }

    public CurveSetting(GroupSetting groupSetting, String string, BezierCurve bezierCurve) {
        super(string, bezierCurve != null ? bezierCurve : CurvePreset.MAGNET.createCurve());

        for (int i = 0; i < PRESETS.length; i++) {
            this.presetAnimations[i] = new Animation();
        }

        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public CurveSetting setValue(BezierCurve bezierCurve) {
        super.setValue(bezierCurve);
        return this;
    }

    public CurveSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public CurveSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public CurveSetting setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.position.x;
        float f3 = this.position.y;
        float f4 = this.size.x;
        float f5 = this.size.y;
        float f6 = f2 + f4 - 30.0F;
        float f7 = f3 + f5 / 2.0F - 5.5F;
        this.drawLabel(drawContext, f2, f3, f4, f5, f4 - 30.0F - 6.0F, this.isHovered(i, i2, this.position, this.size));
        GuiRenderUtil.drawSurface(drawContext, f6, f7, 30.0F, 11.0F, this.getAlpha(), 3.0F);
        this.getValue().toScreenPoints(this.points);
        BezierCurveRenderer.drawCurve(
            drawContext,
            f6 + 3.0F,
            f7 + 3.0F,
            30.0F - 3.0F * 2.0F,
            11.0F - 3.0F * 2.0F,
            this.points,
            ColorUtil.multiplyAlpha(Theme.getPrimary(), this.getAlpha()),
            this.getAlpha(),
            1.2F,
            0.0F,
            0.0F
        );
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (!this.isLeftButton(click.button())) {
            return false;
        } else if (this.open) {
            return this.handlePopupClick(click);
        } else if (this.isHovered(click.x(), click.y(), this.position.x, this.position.y, this.size.x, this.size.y)) {
            this.open = true;
            this.updateSnapPreset();
            return true;
        } else {
            return false;
        }
    }

    private boolean handlePopupClick(Click click) {
        double d = click.x();
        double d2 = click.y();
        int i = this.getHandleAt(d, d2);
        if (i >= 0) {
            this.draggingHandle = i;
            this.morphing = false;
            return true;
        }

        int i2 = this.getPresetAt(d, d2);
        if (i2 >= 0) {
            this.morphTo(PRESET_CURVES[i2].copy());
            return true;
        }

        if (this.isOverPopup(d, d2)) {
            return true;
        }

        this.close();
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (this.open && this.draggingHandle >= 0 && this.isLeftButton(click.button())) {
            this.dragHandle(this.draggingHandle, click.x(), click.y());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onMouseRelease(Click click) {
        this.draggingHandle = -1;
        return this.open;
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (!this.open) {
            return false;
        }

        int i = keyInput.key();
        boolean flag;
        switch (i) {
            case 256:
                this.close();
                return true;
            case 257:
            case 258:
            case 335:
                flag = true;
                break;
            default:
                flag = false;
        }

        boolean flag2 = flag;
        if (flag2 && this.snapPreset != null && !this.morphing) {
            this.morphTo(PRESET_CURVES[this.snapPreset.ordinal()].copy());
            return true;
        } else {
            return false;
        }
    }

    private void dragHandle(int i, double d, double d2) {
        float f = clamp01((float)((d - this.getGraphX()) / this.getGraphWidth()));
        float f2 = clamp01((float)((d2 - this.getGraphY()) / 86.0));
        float f3 = 1.0F - f2;
        BezierCurve bezierCurve = this.getValue();
        switch (i) {
            case 0:
                bezierCurve.setStartY(f3);
                this.notifyChange();
                this.updateSnapPreset();
                return;
            case 1:
                bezierCurve.setControl1(f, f3);
                this.notifyChange();
                this.updateSnapPreset();
                return;
            case 2:
                bezierCurve.setControl2(f, f3);
                this.notifyChange();
                this.updateSnapPreset();
                return;
            case 3:
                bezierCurve.setEndY(f3);
            default:
                this.notifyChange();
                this.updateSnapPreset();
        }
    }

    private void morphTo(BezierCurve bezierCurve) {
        this.morphFrom.copyFrom(this.getValue());
        this.morphTo = bezierCurve;
        this.morphing = true;
        this.morphAnimation.forceValue(0.0);
        this.morphAnimation.animate(1.0, 0.4, Easings.BACK_OUT);
        this.snapPreset = null;
    }

    private void updateSnapPreset() {
        if (!this.morphing) {
            BezierCurve bezierCurve = this.getValue();
            CurvePreset curvePreset = null;
            float f = 0.06F;

            for (int i = 0; i < PRESETS.length; i++) {
                float f2 = bezierCurve.maxDifference(PRESET_CURVES[i]);
                if (f2 < f) {
                    f = f2;
                    curvePreset = PRESETS[i];
                }
            }

            if (curvePreset != null && f > 0.008F) {
                this.snapPreset = curvePreset;
            } else {
                this.snapPreset = null;
            }
        }
    }

    @Override
    public void close() {
        this.open = false;
        this.draggingHandle = -1;
        this.snapPreset = null;
    }

    public void resetPopup() {
        this.close();
        this.openAnimation.forceValue(0.0);
        this.snapAnimation.forceValue(0.0);
    }

    @Override
    public boolean isOverlayOpen() {
        return this.isPopupVisible();
    }

    public boolean isCapturingMouse(double d, double d2) {
        return this.isPopupVisible() && (this.draggingHandle >= 0 || this.isOverPopup(d, d2));
    }

    @Override
    public void renderOverlay(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.getOpenProgress();
        if (!(f2 <= 0.001F)) {
            if (this.morphing) {
                this.updateMorph();
            }

            this.renderPopup(drawContext, i, i2, f2);
        }
    }

    private void updateMorph() {
        this.morphAnimation.update();
        this.getValue().lerp(this.morphFrom, this.morphTo, this.morphAnimation.get());
        if (this.morphAnimation.isFinished()) {
            this.getValue().copyFrom(this.morphTo);
            this.morphing = false;
            this.notifyChange();
            this.updateSnapPreset();
        }
    }

    private void renderPopup(DrawContext drawContext, int i, int i2, float f) {
        float f2 = this.getPopupHeight();
        this.popupX = GuiRenderUtil.placeBeside(this.position.x, this.position.x + this.size.x, 152.0F, 4.0F);
        this.popupY = GuiRenderUtil.clampToScreenY(this.position.y + this.size.y / 2.0F - f2 / 2.0F, f2);
        float f3 = this.getAlpha() * Math.min(f, 1.0F);
        float f4 = 0.85F + 0.15F * f;
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, this.popupX + 76.0F, this.popupY + f2 / 2.0F, f4);
        GuiRenderUtil.drawPopup(drawContext, this.popupX, this.popupY, 152.0F, f2, f3, 10.0F);
        float f5 = this.getGraphX();
        float f6 = this.getGraphY();
        float f7 = this.getGraphWidth();
        this.renderGrid(drawContext, f5, f6, f7, f3);
        this.renderCurves(drawContext, f5, f6, f7, f3);
        this.renderHandles(drawContext, f5, f6, f7, f3, i, i2);
        this.renderPresets(drawContext, f3, i, i2);
        this.renderHint(drawContext, f3);
        MatrixUtil.pop(drawContext);
    }

    private void renderGrid(DrawContext drawContext, float f, float f2, float f3, float f4) {
        GuiRenderUtil.drawSurface(drawContext, f, f2, f3, 86.0F, f4, 4.0F);
        int i = ColorUtil.multiplyAlpha(Theme.getOutline(), 0.35F * f4);

        for (int i2 = 1; i2 < 5; i2++) {
            float f5 = f + f3 * i2 / 5.0F;
            new RenderUtil(drawContext, f5, f2 + 2.0F, 0.5F, 82.0F).rect(i);
        }

        for (int i3 = 1; i3 < 4; i3++) {
            float f6 = f2 + 86.0F * i3 / 4.0F;
            new RenderUtil(drawContext, f + 2.0F, f6, f3 - 4.0F, 0.5F).rect(i);
        }
    }

    private void renderCurves(DrawContext drawContext, float f, float f2, float f3, float f4) {
        float f5 = this.getSnapProgress();
        if (f5 > 0.001F && this.snapPreset != null) {
            PRESET_CURVES[this.snapPreset.ordinal()].toScreenPoints(this.snapPoints);
            int i = ColorUtil.interpolate(Theme.getTextColor(), Theme.getBackground(), 0.35F);
            BezierCurveRenderer.drawCurve(
                drawContext, f, f2, f3, 86.0F, this.snapPoints, ColorUtil.multiplyAlpha(i, f4 * f5 * 0.75F), f4 * f5 * 0.75F, 1.3F, 0.0F, 0.0F
            );
        }

        this.getValue().toScreenPoints(this.points);
        float f6 = (float)(System.currentTimeMillis() % 3000L) / 3000.0F;
        BezierCurveRenderer.drawCurve(drawContext, f, f2, f3, 86.0F, this.points, ColorUtil.multiplyAlpha(Theme.getPrimary(), f4), f4, 2.0F, 2.5F, f6);
    }

    private void renderHandles(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2) {
        int i3 = ColorUtil.multiplyAlpha(Theme.getMutedText(), 0.6F * f4);
        this.drawLine(drawContext, f + this.points[0] * f3, f2 + this.points[1] * 86.0F, f + this.points[2] * f3, f2 + this.points[3] * 86.0F, i3);
        this.drawLine(drawContext, f + this.points[6] * f3, f2 + this.points[7] * 86.0F, f + this.points[4] * f3, f2 + this.points[5] * 86.0F, i3);

        for (int i4 = 0; i4 < 4; i4++) {
            float f5 = f + this.points[i4 * 2] * f3;
            float f6 = f2 + this.points[i4 * 2 + 1] * 86.0F;
            boolean flag;
            if (this.draggingHandle != i4) {
                flag = this.isHovered(i, i2, f5 - 4.5F, f6 - 4.5F, 9.0, 9.0);
            } else {
                flag = true;
            }

            this.handleAnimations[i4].update();
            this.handleAnimations[i4].animate(flag ? 1.0 : 0.0, 0.15F, Easings.QUAD_OUT);
            float f7 = 2.6F + this.handleAnimations[i4].get() * 1.2F;
            boolean flag2 = i4 == 0 || i4 == 3;
            int i5 = flag2 ? Theme.getPrimary() : Theme.getSurface();
            new RenderUtil(drawContext, f5 - f7, f6 - f7, f7 * 2.0F, f7 * 2.0F).rect(ColorUtil.multiplyAlpha(i5, f4), Radius.all(f7));
            new RenderUtil(drawContext, f5 - f7, f6 - f7, f7 * 2.0F, f7 * 2.0F)
                .shadow(0.7F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f4), Radius.all(f7));
        }
    }

    private void renderPresets(DrawContext drawContext, float f, int i, int i2) {
        float f2 = this.getGraphY() + 86.0F + 6.0F;
        float f3 = this.getGraphX();
        float f4 = this.getGraphWidth();
        float f5 = (f4 - 4.0F * (PRESETS.length - 1)) / PRESETS.length;

        for (int i3 = 0; i3 < PRESETS.length; i3++) {
            float f6 = f3 + i3 * (f5 + 4.0F);
            boolean flag = this.isHovered(i, i2, f6, f2, f5, 13.0);
            this.presetAnimations[i3].update();
            this.presetAnimations[i3].animate(flag ? 1.0 : 0.0, 0.18F, Easings.QUAD_OUT);
            GuiRenderUtil.drawSurface(drawContext, f6, f2, f5, 13.0F, f, 3.0F);
            if (this.presetAnimations[i3].get() > 0.001F) {
                new RenderUtil(drawContext, f6, f2, f5, 13.0F)
                    .rect(ColorUtil.multiplyAlpha(Theme.getWhite(), 0.07F * this.presetAnimations[i3].get() * f), Radius.all(3.0F));
            }

            if (this.snapPreset == PRESETS[i3]) {
                new RenderUtil(drawContext, f6, f2, f5, 13.0F)
                    .shadow(0.7F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f * this.getSnapProgress()), Radius.all(3.0F));
            }

            int i4 = ColorUtil.interpolate(Theme.getSecondaryText(), Theme.getTextColor(), this.presetAnimations[i3].get());
            new RenderUtil(drawContext, f6, f2, f5, 13.0F)
                .textCenter(Fonts.INTER_MEDIUM, Language.translate(PRESETS[i3].getTranslationKey()), ColorUtil.multiplyAlpha(i4, f), 5.0F, false);
        }
    }

    private void renderHint(DrawContext drawContext, float f) {
        float f2 = this.getGraphY() + 86.0F + 6.0F + 13.0F + 4.0F;
        String string;
        float f3;
        if (this.snapPreset != null) {
            string = String.format(Language.translate("curve.snapHint", "Enter → %s"), Language.translate(this.snapPreset.getTranslationKey()));
            f3 = f;
        } else {
            string = Language.translate("curve.dragHint", "Перетащи узлы");
            f3 = f * 0.7F;
        }

        new RenderUtil(drawContext, this.getGraphX(), f2, this.getGraphWidth(), 10.0F)
            .textCenter(Fonts.INTER_MEDIUM, string, ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f3), 5.0F, false);
    }

    private void drawLine(DrawContext drawContext, float f, float f2, float f3, float f4, int i) {
        float f5 = f3 - f;
        float f6 = f4 - f2;
        float f7 = (float)Math.sqrt(f5 * f5 + f6 * f6);
        if (!(f7 < 0.01F)) {
            float f8 = (float)Math.toDegrees(Math.atan2(f6, f5));
            MatrixUtil.push(drawContext);
            MatrixUtil.rotate(drawContext, f, f2, f8);
            new RenderUtil(drawContext, f, f2 - 0.4F, f7, 0.8F).rect(i);
            MatrixUtil.pop(drawContext);
        }
    }

    private float getOpenProgress() {
        this.openAnimation.update();
        if (this.open) {
            this.openAnimation.animate(1.0, 0.3F, Easings.BACK_OUT);
            return this.openAnimation.get();
        } else {
            this.openAnimation.animate(0.0, 0.2F, Easings.QUAD_IN);
            return this.openAnimation.get();
        }
    }

    private float getSnapProgress() {
        this.snapAnimation.update();
        if (this.snapPreset != null) {
            this.snapAnimation.animate(1.0, 0.25, Easings.QUAD_OUT);
            return this.snapAnimation.get();
        } else {
            this.snapAnimation.animate(0.0, 0.25, Easings.QUAD_OUT);
            return this.snapAnimation.get();
        }
    }

    private float getPopupHeight() {
        return 135.0F;
    }

    private float getGraphX() {
        return this.popupX + 8.0F;
    }

    private float getGraphY() {
        return this.popupY + 8.0F;
    }

    private float getGraphWidth() {
        return 136.0F;
    }

    private int getHandleAt(double d, double d2) {
        this.getValue().toScreenPoints(this.points);
        float f = this.getGraphX();
        float f2 = this.getGraphY();
        float f3 = this.getGraphWidth();

        for (int i = 0; i < 4; i++) {
            float f4 = f + this.points[i * 2] * f3;
            float f5 = f2 + this.points[i * 2 + 1] * 86.0F;
            if (this.isHovered(d, d2, f4 - 4.5F, f5 - 4.5F, 9.0, 9.0)) {
                return i;
            }
        }

        return -1;
    }

    private int getPresetAt(double d, double d2) {
        float f = this.getGraphY() + 86.0F + 6.0F;
        float f2 = this.getGraphX();
        float f3 = this.getGraphWidth();
        float f4 = (f3 - 4.0F * (PRESETS.length - 1)) / PRESETS.length;

        for (int i = 0; i < PRESETS.length; i++) {
            float f5 = f2 + i * (f4 + 4.0F);
            if (this.isHovered(d, d2, f5, f, f4, 13.0)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isOverPopup(double d, double d2) {
        return this.isHovered(d, d2, this.popupX, this.popupY, 152.0, this.getPopupHeight());
    }

    private boolean isPopupVisible() {
        return this.open || this.openAnimation.get() > 0.001F;
    }

    private static float clamp01(float f) {
        if (f < 0.0F) {
            return 0.0F;
        } else {
            return f > 1.0F ? 1.0F : f;
        }
    }

    @Override
    public float getControlWidth() {
        return 30.0F;
    }

    static {
        for (int i = 0; i < PRESETS.length; i++) {
            PRESET_CURVES[i] = PRESETS[i].createCurve();
        }
    }
}
