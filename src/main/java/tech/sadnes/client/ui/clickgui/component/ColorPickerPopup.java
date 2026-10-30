package tech.sadnes.client.ui.clickgui.component;

import java.util.function.IntConsumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2f;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.RoundedRectRenderer;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.texture.Texture;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

public final class ColorPickerPopup implements IInteractable, IRenderable, IMinecraft {
    private static final float SQUARE_SIZE = 72.0F;
    private static final float BAR_WIDTH = 8.0F;
    private static final float GAP = 5.0F;
    private static final float PADDING = 6.0F;
    private static final float PREVIEW_HEIGHT = 12.0F;
    private static final float WIDTH = 110.0F;
    private static final float HEIGHT = 100.0F;
    private boolean open;
    private float x;
    private float y;
    private float hue;
    private float saturation;
    private float brightness;
    private float alpha = 1.0F;
    private boolean allowAlpha = true;
    private IntConsumer callback;
    private ColorPickerPopup.DragTarget dragTarget = ColorPickerPopup.DragTarget.NONE;

    public boolean isOpen() {
        return this.open;
    }

    public void show(int i, boolean flag, float f, float f2, IntConsumer intConsumer) {
        float[] floats = ColorUtil.toHsb(i);
        this.hue = floats[0];
        this.saturation = floats[1];
        this.brightness = floats[2];
        this.alpha = flag ? ColorUtil.alpha(i) / 255.0F : 1.0F;
        this.allowAlpha = flag;
        this.callback = intConsumer;
        this.x = GuiRenderUtil.clampToScreenX(f, 110.0F);
        this.y = GuiRenderUtil.clampToScreenY(f2, 100.0F);
        this.dragTarget = ColorPickerPopup.DragTarget.NONE;
        this.open = true;
    }

    public void close() {
        this.open = false;
        this.dragTarget = ColorPickerPopup.DragTarget.NONE;
    }

    public int getColor() {
        return ColorUtil.fromHsba(this.hue, this.saturation, this.brightness, this.alpha);
    }

    public boolean isMouseOver(double d, double d2) {
        return this.open && this.isHovered(d, d2, this.x, this.y, 110.0, 100.0);
    }

    public void renderPicker(DrawContext drawContext, int i, int i2, float f) {
        if (this.open) {
            int i3 = this.getColor();
            int i4 = ColorUtil.fromHsb(this.hue, 1.0F, 1.0F);
            float f2 = this.x + 6.0F;
            float f3 = f2 + 72.0F + 5.0F;
            float f4 = f3 + 8.0F + 5.0F;
            float f5 = this.y + 6.0F;
            GuiRenderUtil.drawPopup(drawContext, this.x, this.y, 110.0F, 100.0F, f, 8.0F);
            RoundedRectRenderer.draw(
                drawContext, new Matrix3x2f(drawContext.getMatrices()), f2, f5, 72.0F, 72.0F, Radius.all(5.0F), 0.0F, 0.0F, f, i4, i4, i4, i4, 2
            );
            GuiRenderUtil.bindTexture(Texture.COLOR_HUE);
            new RenderUtil(drawContext, f3, f5, 8.0F, 72.0F).frostedGlass(f, Radius.all(2.0F));
            if (this.allowAlpha) {
                GuiRenderUtil.bindTexture(Texture.COLOR_ALPHA);
                new RenderUtil(drawContext, f4, f5, 8.0F, 72.0F).frostedGlass(f, Radius.all(2.0F));
                new RenderUtil(drawContext, f4, f5, 8.0F, 72.0F)
                    .rect(
                        ColorUtil.setAlpha(i3, 0),
                        ColorUtil.setAlpha(i3, 255),
                        ColorUtil.setAlpha(i3, 0),
                        ColorUtil.setAlpha(i3, 255),
                        f,
                        Radius.all(2.0F)
                    );
            }

            int i5 = ColorUtil.multiplyAlpha(ColorUtil.WHITE, f);
            new RenderUtil(drawContext, f2 + this.saturation * 72.0F - 3.0F, f5 + (1.0F - this.brightness) * 72.0F - 3.0F, 6.0F, 6.0F)
                .rect(i5, Radius.all(3.0F));
            new RenderUtil(drawContext, f3 - 1.0F, f5 + this.hue * 72.0F - 1.0F, 10.0F, 2.0F).rect(i5, Radius.all(1.0F));
            if (this.allowAlpha) {
                new RenderUtil(drawContext, f4 - 1.0F, f5 + (1.0F - this.alpha) * 72.0F - 1.0F, 10.0F, 2.0F).rect(i5, Radius.all(1.0F));
            }

            float f6 = f5 + 72.0F + 4.0F;
            new RenderUtil(drawContext, f2, f6 + 1.0F, 10.0F, 10.0F).rect(ColorUtil.multiplyAlpha(i3, f), Radius.all(2.0F));
            String string = this.allowAlpha ? String.format("0x%08X", i3) : String.format("0x%06X", i3 & 16777215);
            new RenderUtil(drawContext, f2 + 14.0F, f6, 84.0F, 12.0F)
                .textMiddleLeft(Fonts.INTER_MEDIUM, string, 0.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f), 6.5F);
        }
    }

    public boolean onMouseClick(Click click) {
        if (this.open && this.isLeftButton(click.button())) {
            float f = this.x + 6.0F;
            float f2 = f + 72.0F + 5.0F;
            float f3 = f2 + 8.0F + 5.0F;
            float f4 = this.y + 6.0F;
            if (this.isHovered(click.x(), click.y(), f, f4, 72.0, 72.0)) {
                this.dragTarget = ColorPickerPopup.DragTarget.SQUARE;
                this.updateFromMouse(click.x(), click.y());
                return true;
            } else if (this.isHovered(click.x(), click.y(), f2 - 2.0F, f4, 12.0, 72.0)) {
                this.dragTarget = ColorPickerPopup.DragTarget.HUE;
                this.updateFromMouse(click.x(), click.y());
                return true;
            } else if (this.allowAlpha && this.isHovered(click.x(), click.y(), f3 - 2.0F, f4, 12.0, 72.0)) {
                this.dragTarget = ColorPickerPopup.DragTarget.ALPHA;
                this.updateFromMouse(click.x(), click.y());
                return true;
            } else {
                return this.isMouseOver(click.x(), click.y());
            }
        } else {
            return false;
        }
    }

    public boolean onMouseDrag(Click click) {
        if (this.open && this.dragTarget != ColorPickerPopup.DragTarget.NONE) {
            this.updateFromMouse(click.x(), click.y());
            return true;
        } else {
            return false;
        }
    }

    public void onMouseRelease() {
        this.dragTarget = ColorPickerPopup.DragTarget.NONE;
    }

    private void updateFromMouse(double d, double d2) {
        float f = this.x + 6.0F;
        float f2 = this.y + 6.0F;
        switch (this.dragTarget) {
            case SQUARE:
                this.saturation = Math.clamp((float)(d - f) / 72.0F, 0.0F, 1.0F);
                this.brightness = 1.0F - Math.clamp((float)(d2 - f2) / 72.0F, 0.0F, 1.0F);
                break;
            case HUE:
                this.hue = Math.clamp((float)(d2 - f2) / 72.0F, 0.0F, 1.0F);
                break;
            case ALPHA:
                this.alpha = 1.0F - Math.clamp((float)(d2 - f2) / 72.0F, 0.0F, 1.0F);
                break;
            default:
                return;
        }

        if (this.callback != null) {
            this.callback.accept(this.getColor());
        }
    }

    enum DragTarget {
        NONE,
        SQUARE,
        HUE,
        ALPHA;
    }
}
