package tech.sadnes.client.feature.impl.render.esp;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.math.MathUtil;

public final class IconLabelStack {
    private static final float ITEM_ICON_SIZE = 16.0F;
    private static final float ICON_TEXT_GAP = 2.0F;
    private static final float LINE_SPACING = 1.0F;
    private final List<IconLabelStack.Line> lines = new ArrayList<>(8);
    private int lineCount;
    private float fontSize;
    private float iconSize;
    private float lineHeight;
    private float width;
    private float height;

    public void begin(float f, float f2) {
        this.lineCount = 0;
        this.fontSize = f;
        this.iconSize = f2;
        this.lineHeight = Math.max(f, f2);
        this.width = 0.0F;
        this.height = 0.0F;
    }

    public void addLine(String string, int i) {
        this.addEntry(string, i, null, null);
    }

    public void addItemLine(String string, int i, ItemStack itemStack) {
        this.addEntry(string, i, itemStack, null);
    }

    public void addSpriteLine(String string, int i, Sprite sprite) {
        this.addEntry(string, i, null, sprite);
    }

    public boolean finish() {
        if (this.lineCount == 0) {
            return false;
        }

        float f = 0.0F;

        for (int i = 0; i < this.lineCount; i++) {
            float f2 = this.lines.get(i).width;
            if (f2 > f) {
                f = f2;
            }
        }

        if (f <= 0.0F) {
            return false;
        }

        this.width = f;
        this.height = this.lineCount * this.lineHeight + (this.lineCount - 1) * 1.0F;
        return true;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    public void render(DrawContext drawContext, float f, float f2, float f3, boolean flag, float f4) {
        MsdfFont msdfFont = Fonts.INTER_MEDIUM;
        if (msdfFont != null && this.lineCount != 0 && !(f4 <= 0.001F)) {
            boolean flag2 = f3 > 0.5F;
            RenderUtil renderUtil = new RenderUtil(drawContext, f, f2, this.width, this.lineHeight);

            for (int i = 0; i < this.lineCount; i++) {
                IconLabelStack.Line line = this.lines.get(i);
                int i2 = flag ? this.lineCount - 1 - i : i;
                float f5 = f + (this.width - line.width) * f3;
                float f6 = f2 + i2 * (this.lineHeight + 1.0F);
                boolean flag3 = line.stack != null || line.sprite != null;
                float f7 = flag3 ? this.iconSize + 2.0F : 0.0F;
                renderUtil.setPosition(f5, f6).setWidth(line.width);
                renderUtil.textMiddleLeft(msdfFont, line.text, flag2 ? 0.0F : f7, 0.0F, ColorUtil.multiplyAlpha(line.color, f4), this.fontSize, true);
                if (flag3) {
                    float f8 = flag2 ? f5 + line.width - this.iconSize : f5;
                    this.renderIcon(drawContext, line, f8, f6 + (this.lineHeight - this.iconSize) / 2.0F, f4);
                }
            }
        }
    }

    private void addEntry(String string, int i, ItemStack itemStack, Sprite sprite2) {
        MsdfFont msdfFont = Fonts.INTER_MEDIUM;
        if (msdfFont != null && string != null && !string.isEmpty()) {
            IconLabelStack.Line line = this.obtainLine();
            line.text = string;
            line.color = i;
            line.stack = itemStack;
            line.sprite = sprite2;
            boolean flag = itemStack != null || sprite2 != null;
            if (flag) {
                line.width = this.iconSize + 2.0F + msdfFont.getWidth(string, this.fontSize);
            } else {
                line.width = 0.0F + msdfFont.getWidth(string, this.fontSize);
            }
        }
    }

    private IconLabelStack.Line obtainLine() {
        if (this.lineCount < this.lines.size()) {
            return this.lines.get(this.lineCount++);
        }

        IconLabelStack.Line line = new IconLabelStack.Line();
        this.lines.add(line);
        this.lineCount++;
        return line;
    }

    private void renderIcon(DrawContext drawContext, IconLabelStack.Line line, float f, float f2, float f3) {
        if (line.sprite != null) {
            GuiRenderUtil.drawSprite(drawContext, line.sprite, f, f2, this.iconSize, this.iconSize, f3);
        } else {
            MatrixUtil.push(drawContext);
            MatrixUtil.translate(drawContext, MathUtil.roundToStep(f, 0.5F), MathUtil.roundToStep(f2, 0.5F));
            MatrixUtil.scale(drawContext, 0.0F, 0.0F, this.iconSize / 16.0F);
            GuiRenderUtil.drawCachedItem(drawContext, line, line.stack, 0.0F, 0.0F, f3);
            MatrixUtil.pop(drawContext);
        }
    }

    static final class Line {
        String text;
        int color;
        ItemStack stack;
        Sprite sprite;
        float width;
    }
}
