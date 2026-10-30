package tech.sadnes.client.render;

import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

public final class MatrixUtil {
    public static void push(DrawContext drawContext) {
        drawContext.getMatrices().pushMatrix();
    }

    public static void pop(DrawContext drawContext) {
        drawContext.getMatrices().popMatrix();
    }

    public static void rotate(DrawContext drawContext, float f, float f2, float f3) {
        Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
        matrix3x2fStack.translate(f, f2);
        matrix3x2fStack.rotate((float)Math.toRadians(f3));
        matrix3x2fStack.translate(-f, -f2);
    }

    public static void scale(DrawContext drawContext, float f, float f2, float f3, float f4) {
        Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
        matrix3x2fStack.translate(f, f2);
        matrix3x2fStack.scale(f3, f4);
        matrix3x2fStack.translate(-f, -f2);
    }

    public static void translate(DrawContext drawContext, float f, float f2) {
        Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
        matrix3x2fStack.translate(f, f2);
    }

    public static void scale(DrawContext drawContext, float f, float f2, float f3) {
        scale(drawContext, f, f2, f3, f3);
    }
}
