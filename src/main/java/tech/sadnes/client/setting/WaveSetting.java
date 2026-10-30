package tech.sadnes.client.setting;

import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.ScissorStack;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.StopWatch;

public final class WaveSetting extends Setting<WaveCurve> implements IMinecraft {
    private static final float POPUP_WIDTH = 200.0F;
    private static final float POPUP_PADDING = 8.0F;
    private static final float GRAPH_HEIGHT = 92.0F;
    private static final float POINT_HIT_SIZE = 9.0F;
    private static final float POPUP_RADIUS = 10.0F;
    private static final float RESET_BUTTON_WIDTH = 60.0F;
    private static final float RESET_BUTTON_HEIGHT = 12.0F;
    private static final float PREVIEW_PADDING = 3.0F;
    private static final float POINT_RADIUS = 2.6F;
    private static final float LABEL_FONT_SIZE = 4.5F;
    private static final float PREVIEW_WIDTH = 30.0F;
    private static final float PREVIEW_HEIGHT = 11.0F;
    private static final float ZOOM_FACTOR = 1.25F;
    private static final float MIN_ZOOM = 0.0025F;
    private static final long DOUBLE_CLICK_MS = 300L;
    private static final float DOUBLE_CLICK_DISTANCE = 3.0F;
    private final WaveSetting.Axis axis;
    private final Animation popupAnimation = new Animation();
    private final Animation[] pointHoverAnimations = new Animation[48];
    private boolean editorOpen;
    private int draggedPoint = -1;
    private boolean panning;
    private double lastPanX;
    private double lastPanY;
    private final StopWatch clickTimer = new StopWatch();
    private double lastClickX;
    private double lastClickY;
    private float popupX;
    private float popupY;
    private float viewX = 0.0F;
    private float viewWidth = 1.0F;
    private float viewY = 0.0F;
    private float viewHeight = 1.0F;

    public WaveSetting(GroupSetting groupSetting, String string, WaveSetting.Axis axis2) {
        super(string, WaveCurve.createDefault());

        for (int i = 0; i < this.pointHoverAnimations.length; i++) {
            this.pointHoverAnimations[i] = new Animation();
        }

        this.axis = axis2;
        if (groupSetting != null) {
            groupSetting.onInput(this);
        }
    }

    public WaveSetting setValue(WaveCurve waveCurve) {
        super.setValue(waveCurve);
        return this;
    }

    public WaveSetting setDisplayName(String string) {
        super.setDisplayName(string);
        return this;
    }

    public WaveSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public float getOffset(float f) {
        return this.axis == WaveSetting.Axis.YAW
            ? -90.0F * this.getValue().sample(0.5F + f / 360.0F)
            : 180.0F * this.getValue().sample(0.5F - f / 180.0F);
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
        float f8 = 1.0F / Math.max(this.getValue().getMaxAmplitude(), 0.001F);
        float f9 = 30.0F - 3.0F * 2.0F;
        float f10 = 11.0F - 3.0F * 2.0F;
        int i3 = ColorUtil.multiplyAlpha(Theme.getPrimary(), this.getAlpha());
        float f11 = 0.0F;
        float f12 = 0.0F;

        for (int i4 = 0; i4 <= 20; i4++) {
            float f13 = i4 / 20.0F;
            float f14 = this.getValue().sample(f13) * f8;
            float f15 = f6 + 3.0F + f13 * f9;
            float f16 = f7 + 3.0F + (1.0F - f14) * 0.5F * f10;
            if (i4 > 0) {
                this.drawLine(drawContext, f11, f12, f15, f16, 1.0F, i3);
            }

            f11 = f15;
            f12 = f16;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean flag) {
        if (this.editorOpen) {
            return this.mouseClickedEditor(click);
        } else if (this.isLeftButton(click.button()) && this.isHovered(click.x(), click.y(), this.position.x, this.position.y, this.size.x, this.size.y)) {
            this.editorOpen = true;
            return true;
        } else {
            return false;
        }
    }

    private boolean mouseClickedEditor(Click click) {
        double d = click.x();
        double d2 = click.y();
        int i = this.getHoveredPoint(d, d2);
        if (this.isRightButton(click.button())) {
            if (i >= 0 && this.getValue().removePoint(i)) {
                this.notifyChange();
            }

            return this.isPopupHovered(d, d2);
        } else {
            if (!this.isLeftButton(click.button())) {
                return false;
            }

            if (i >= 0) {
                this.draggedPoint = i;
                return true;
            }

            if (!this.isGraphHovered(d, d2)) {
                if (this.isHovered(d, d2, this.getResetButtonX(), this.getResetButtonY(), 60.0, 12.0)) {
                    this.resetCurve();
                    return true;
                }

                if (this.isPopupHovered(d, d2)) {
                    return true;
                }

                this.close();
                return true;
            } else {
                boolean flag = this.clickTimer.getElapsed() < 300L && Math.abs(d - this.lastClickX) < 3.0 && Math.abs(d2 - this.lastClickY) < 3.0;
                this.clickTimer.reset();
                this.lastClickX = d;
                this.lastClickY = d2;
                if (flag) {
                    this.draggedPoint = this.getValue().addPoint(this.getPointPosition(d, d2), this.getPointValue(d, d2));
                    if (this.draggedPoint >= 0) {
                        this.notifyChange();
                    }

                    return true;
                } else {
                    this.panning = true;
                    this.lastPanX = d;
                    this.lastPanY = d2;
                    return true;
                }
            }
        }
    }

    private void resetCurve() {
        this.getValue().copyFrom(WaveCurve.createDefault());
        this.viewX = 0.0F;
        this.viewY = 0.0F;
        this.viewWidth = 1.0F;
        this.viewHeight = 1.0F;
        this.notifyChange();
    }

    @Override
    public boolean mouseDragged(Click click, double d, double d2) {
        if (!this.editorOpen || !this.isLeftButton(click.button())) {
            return false;
        }

        if (this.panning) {
            this.viewX = clamp(this.viewX - (float)((click.x() - this.lastPanX) / this.getGraphWidth()) * this.viewWidth, 0.0F, 1.0F - this.viewWidth);
            this.viewY = clamp(this.viewY + (float)((click.y() - this.lastPanY) / 92.0) * this.viewHeight, 0.0F, 1.0F - this.viewHeight);
            this.lastPanX = click.x();
            this.lastPanY = click.y();
            return true;
        }

        if (this.draggedPoint < 0) {
            return false;
        }

        this.draggedPoint = this.getValue().setPoint(this.draggedPoint, this.getPointPosition(click.x(), click.y()), this.getPointValue(click.x(), click.y()));
        this.notifyChange();
        return true;
    }

    @Override
    public boolean onMouseRelease(Click click) {
        this.draggedPoint = -1;
        this.panning = false;
        return this.editorOpen;
    }

    @Override
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (this.editorOpen && this.isGraphHovered(d, d2) && d4 != 0.0) {
            float f = d4 > 0.0 ? 0.8F : 1.25F;
            float f2 = (float)((d - this.getGraphX()) / this.getGraphWidth());
            float f3 = 1.0F - (float)((d2 - this.getGraphY()) / 92.0);
            boolean flag = isShiftDown();
            if (!flag || this.axis == WaveSetting.Axis.PITCH) {
                float f4 = clamp(this.viewWidth * f, 0.0025F, 1.0F);
                this.viewX = clamp(this.viewX + f2 * (this.viewWidth - f4), 0.0F, 1.0F - f4);
                this.viewWidth = f4;
            }

            if (!flag || this.axis == WaveSetting.Axis.YAW) {
                float f5 = clamp(this.viewHeight * f, 0.0025F, 1.0F);
                this.viewY = clamp(this.viewY + f3 * (this.viewHeight - f5), 0.0F, 1.0F - f5);
                this.viewHeight = f5;
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onKeyPress(KeyInput keyInput) {
        if (this.editorOpen && keyInput.key() == 256) {
            this.close();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() {
        this.editorOpen = false;
        this.draggedPoint = -1;
        this.panning = false;
    }

    public void closeEditor() {
        this.close();
        this.popupAnimation.forceValue(0.0);
    }

    @Override
    public boolean isOverlayOpen() {
        return this.isEditorVisible();
    }

    public boolean isOverEditor(double d, double d2) {
        return this.isEditorVisible() && (this.draggedPoint >= 0 || this.panning || this.isPopupHovered(d, d2));
    }

    @Override
    public void renderOverlay(DrawContext drawContext, int i, int i2, float f) {
        this.popupAnimation.update();
        if (this.editorOpen) {
            this.popupAnimation.animate(1.0, 0.3F, Easings.BACK_OUT);
        } else {
            this.popupAnimation.animate(0.0, 0.2F, Easings.QUAD_IN);
        }

        float f2 = this.popupAnimation.get();
        if (!(f2 <= 0.001F)) {
            float f3 = this.getPopupHeight();
            this.popupX = GuiRenderUtil.placeBeside(this.position.x, this.position.x + this.size.x, 200.0F, 4.0F);
            this.popupY = GuiRenderUtil.clampToScreenY(this.position.y + this.size.y / 2.0F - f3 / 2.0F, f3);
            float f4 = this.getAlpha() * Math.min(f2, 1.0F);
            float f5 = 0.85F + 0.15F * f2;
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, this.popupX + 100.0F, this.popupY + f3 / 2.0F, f5);
            GuiRenderUtil.drawPopup(drawContext, this.popupX, this.popupY, 200.0F, f3, f4, 10.0F);
            float f6 = this.getGraphX();
            float f7 = this.getGraphY();
            float f8 = this.getGraphWidth();
            GuiRenderUtil.drawSurface(drawContext, f6, f7, f8, 92.0F, f4, 4.0F);
            ScissorStack.push(drawContext, f6, f7, f8, 92.0F);
            this.drawGrid(drawContext, f6, f7, f8, f4);
            this.drawCurve(drawContext, f4);
            int i3 = this.drawPoints(drawContext, f4, i, i2);
            ScissorStack.pop(drawContext);
            this.drawLabels(drawContext, f6, f7, f8, f4, i3, i, i2);
            MatrixUtil.pop(drawContext);
        }
    }

    private void drawGrid(DrawContext drawContext, float f, float f2, float f3, float f4) {
        int i = ColorUtil.multiplyAlpha(Theme.getOutline(), 0.35F * f4);

        for (int i2 = 1; i2 < 4; i2++) {
            new RenderUtil(drawContext, f + f3 * i2 / 4.0F, f2, 0.5F, 92.0F).rect(i);
            new RenderUtil(drawContext, f, f2 + 92.0F * i2 / 4.0F, f3, 0.5F).rect(i);
        }

        int i3 = ColorUtil.multiplyAlpha(Theme.getMutedText(), 0.7F * f4);
        new RenderUtil(drawContext, this.toScreenX(0.5F), f2, 0.6F, 92.0F).rect(i3);
        new RenderUtil(drawContext, f, this.toScreenY(0.5F), f3, 0.6F).rect(i3);
    }

    private void drawCurve(DrawContext drawContext, float f) {
        int i = ColorUtil.multiplyAlpha(Theme.getPrimary(), f);
        float f2 = 0.0F;
        float f3 = 0.0F;

        for (int i2 = 0; i2 <= 96; i2++) {
            float f4 = this.axis == WaveSetting.Axis.YAW ? this.viewX + this.viewWidth * i2 / 96 : this.viewY + this.viewHeight * i2 / 96;
            float f5 = (this.getValue().sample(f4) + 1.0F) * 0.5F;
            float f6 = this.toScreenX(this.axis == WaveSetting.Axis.YAW ? f4 : f5);
            float f7 = this.toScreenY(this.axis == WaveSetting.Axis.YAW ? f5 : f4);
            if (i2 > 0) {
                this.drawLine(drawContext, f2, f3, f6, f7, 2.0F, i);
            }

            f2 = f6;
            f3 = f7;
        }
    }

    private int drawPoints(DrawContext drawContext, float f, int i, int i2) {
        WaveCurve waveCurve = this.getValue();
        int i3 = waveCurve.getPointCount();
        int i4 = this.draggedPoint;

        for (int i5 = 0; i5 < i3; i5++) {
            float f2 = this.getPointScreenX(i5);
            float f3 = this.getPointScreenY(i5);
            boolean flag;
            if (this.draggedPoint != i5) {
                flag = this.isHovered(i, i2, f2 - 4.5F, f3 - 4.5F, 9.0, 9.0);
            } else {
                flag = true;
            }

            if (flag && i4 < 0) {
                i4 = i5;
            }

            this.pointHoverAnimations[i5].update();
            this.pointHoverAnimations[i5].animate(flag ? 1.0 : 0.0, 0.15F, Easings.QUAD_OUT);
            float f4 = 2.6F + this.pointHoverAnimations[i5].get() * 1.2F;
            new RenderUtil(drawContext, f2 - f4, f3 - f4, f4 * 2.0F, f4 * 2.0F)
                .rect(ColorUtil.multiplyAlpha(Theme.getSurface(), f), Radius.all(f4));
            new RenderUtil(drawContext, f2 - f4, f3 - f4, f4 * 2.0F, f4 * 2.0F)
                .shadow(0.7F, ColorUtil.multiplyAlpha(Theme.getPrimary(), f), Radius.all(f4));
        }

        return i4;
    }

    private void drawLabels(DrawContext drawContext, float f, float f2, float f3, float f4, int i, int i2, int i3) {
        int i4 = ColorUtil.multiplyAlpha(Theme.getSecondaryText(), f4 * 0.7F);
        new RenderUtil(drawContext, f + 2.0F, f2 + 1.0F, f3, 9.0F)
            .textMiddleLeft(Fonts.INTER_MEDIUM, formatDegrees(toPitchDegrees(this.viewY + this.viewHeight)), i4, 4.5F);
        new RenderUtil(drawContext, f + 2.0F, f2 + 92.0F - 9.0F - 1.0F, f3, 9.0F)
            .textMiddleLeft(Fonts.INTER_MEDIUM, formatDegrees(toPitchDegrees(this.viewY)), i4, 4.5F);
        float f5 = f2 + 92.0F + 3.0F;
        new RenderUtil(drawContext, f, f5, f3, 9.0F).textMiddleLeft(Fonts.INTER_MEDIUM, formatDegrees(toYawDegrees(this.viewX)), i4, 4.5F);
        new RenderUtil(drawContext, f, f5, f3, 9.0F)
            .textMiddleRight(Fonts.INTER_MEDIUM, formatDegrees(toYawDegrees(this.viewX + this.viewWidth)), 0.0F, 0.0F, i4, 4.5F, false);
        float f6 = -1.0F;
        float f7 = -1.0F;
        if (i >= 0) {
            f6 = this.fromScreenX(this.getPointScreenX(i));
            f7 = this.fromScreenY(this.getPointScreenY(i));
        } else if (this.isGraphHovered(i2, i3)) {
            f6 = this.fromScreenX(i2);
            f7 = this.fromScreenY(i3);
        }

        if (f6 >= 0.0F) {
            String string = String.format(
                Language.translate("wave.readout", "yaw %s  pitch %s"), formatDegrees(toYawDegrees(f6)), formatDegrees(toPitchDegrees(f7))
            );
            new RenderUtil(drawContext, f, f5, f3, 9.0F)
                .textCenter(Fonts.INTER_MEDIUM, string, ColorUtil.multiplyAlpha(Theme.getTextColor(), f4), 4.5F, false);
        }

        new RenderUtil(drawContext, f, f5 + 9.0F + 3.0F, f3, 10.0F)
            .textCenter(Fonts.INTER_MEDIUM, Language.translate("wave.hint", "2×ЛКМ — точка, ПКМ — удалить, тянуть — двигать, колесо — зум"), i4, 5.0F, false);
        float f8 = this.getResetButtonX();
        float f9 = this.getResetButtonY();
        GuiRenderUtil.drawSurface(drawContext, f8, f9, 60.0F, 12.0F, f4, 3.0F);
        boolean flag = this.isHovered(i2, i3, f8, f9, 60.0, 12.0);
        if (flag) {
            new RenderUtil(drawContext, f8, f9, 60.0F, 12.0F).rect(ColorUtil.multiplyAlpha(Theme.getWhite(), 0.07F * f4), Radius.all(3.0F));
        }

        int i5 = flag ? Theme.getTextColor() : Theme.getSecondaryText();
        new RenderUtil(drawContext, f8, f9, 60.0F, 12.0F)
            .textCenter(Fonts.INTER_MEDIUM, Language.translate("wave.reset", "Сбросить"), ColorUtil.multiplyAlpha(i5, f4), 5.0F, false);
    }

    private void drawLine(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, int i) {
        float f6 = f3 - f;
        float f7 = f4 - f2;
        float f8 = (float)Math.sqrt(f6 * f6 + f7 * f7) + f5 * 0.5F;
        float f9 = (float)Math.toDegrees(Math.atan2(f7, f6));
        MatrixUtil.push(drawContext);
        MatrixUtil.rotate(drawContext, f, f2, f9);
        new RenderUtil(drawContext, f, f2 - f5 / 2.0F, f8, f5).rect(i);
        MatrixUtil.pop(drawContext);
    }

    private static float toYawDegrees(float f) {
        return -180.0F + 360.0F * f;
    }

    private static float toPitchDegrees(float f) {
        return 90.0F - 180.0F * f;
    }

    private static String formatDegrees(float f) {
        return String.format(Locale.ROOT, "%.1f°", f);
    }

    private float toScreenX(float f) {
        return this.getGraphX() + (f - this.viewX) / this.viewWidth * this.getGraphWidth();
    }

    private float toScreenY(float f) {
        return this.getGraphY() + (1.0F - (f - this.viewY) / this.viewHeight) * 92.0F;
    }

    private float fromScreenX(double d) {
        return this.viewX + (float)((d - this.getGraphX()) / this.getGraphWidth()) * this.viewWidth;
    }

    private float fromScreenY(double d) {
        return this.viewY + (1.0F - (float)((d - this.getGraphY()) / 92.0)) * this.viewHeight;
    }

    private float getPointScreenX(int i) {
        WaveCurve waveCurve = this.getValue();
        return this.axis == WaveSetting.Axis.YAW ? this.toScreenX(waveCurve.getPosition(i)) : this.toScreenX((waveCurve.getPointValue(i) + 1.0F) * 0.5F);
    }

    private float getPointScreenY(int i) {
        WaveCurve waveCurve = this.getValue();
        return this.axis == WaveSetting.Axis.YAW ? this.toScreenY((waveCurve.getPointValue(i) + 1.0F) * 0.5F) : this.toScreenY(waveCurve.getPosition(i));
    }

    private float getPointPosition(double d, double d2) {
        return this.axis == WaveSetting.Axis.YAW ? this.fromScreenX(d) : this.fromScreenY(d2);
    }

    private float getPointValue(double d, double d2) {
        return this.axis == WaveSetting.Axis.YAW ? this.fromScreenY(d2) * 2.0F - 1.0F : this.fromScreenX(d) * 2.0F - 1.0F;
    }

    private float getPopupHeight() {
        return 148.0F;
    }

    private float getResetButtonX() {
        return this.popupX + 100.0F - 30.0F;
    }

    private float getResetButtonY() {
        return this.getGraphY() + 92.0F + 3.0F + 9.0F + 3.0F + 10.0F + 3.0F;
    }

    private float getGraphX() {
        return this.popupX + 8.0F;
    }

    private float getGraphY() {
        return this.popupY + 8.0F;
    }

    private float getGraphWidth() {
        return 184.0F;
    }

    private boolean isGraphHovered(double d, double d2) {
        return this.isHovered(d, d2, this.getGraphX(), this.getGraphY(), this.getGraphWidth(), 92.0);
    }

    private int getHoveredPoint(double d, double d2) {
        if (!this.isGraphHovered(d, d2)) {
            return -1;
        }

        int i = this.getValue().getPointCount();

        for (int i2 = 0; i2 < i; i2++) {
            float f = this.getPointScreenX(i2);
            float f2 = this.getPointScreenY(i2);
            if (this.isHovered(d, d2, f - 4.5F, f2 - 4.5F, 9.0, 9.0)) {
                return i2;
            }
        }

        return -1;
    }

    private boolean isPopupHovered(double d, double d2) {
        return this.isHovered(d, d2, this.popupX, this.popupY, 200.0, this.getPopupHeight());
    }

    private boolean isEditorVisible() {
        return this.editorOpen || this.popupAnimation.get() > 0.001F;
    }

    private static boolean isShiftDown() {
        return InputUtil.isKeyPressed(mc.getWindow(), 340) || InputUtil.isKeyPressed(mc.getWindow(), 344);
    }

    private static float clamp(float f, float f2, float f3) {
        return f < f2 ? f2 : Math.min(f, f3);
    }

    @Override
    protected float getControlWidth() {
        return 30.0F;
    }

    public enum Axis {
        YAW,
        PITCH;
    }
}
