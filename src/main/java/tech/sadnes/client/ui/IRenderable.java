package tech.sadnes.client.ui;

import org.joml.Vector2f;

public interface IRenderable {
    default boolean isHovered(double d, double d2, double d3, double d4, double d5, double d6) {
        return d >= d3 && d <= d3 + d5 && d2 >= d4 && d2 <= d4 + d6;
    }

    default boolean isHovered(double d, double d2, Vector2f vector2f, Vector2f vector2f2) {
        return this.isHovered(d, d2, vector2f.x, vector2f.y, vector2f2.x, vector2f2.y);
    }

    default boolean isInside(double d, double d2, double d3, double d4, double d5, double d6, double d7, double d8) {
        return d >= d5 && d + d3 <= d5 + d7 && d2 >= d6 && d2 + d4 <= d6 + d8;
    }

    default boolean isInside(Vector2f vector2f, Vector2f vector2f2, Vector2f vector2f3, Vector2f vector2f4) {
        return this.isInside(vector2f.x, vector2f.y, vector2f2.x, vector2f2.y, vector2f3.x, vector2f3.y, vector2f4.x, vector2f4.y);
    }

    default boolean isLeftButton(int i) {
        return i == 0;
    }

    default boolean isRightButton(int i) {
        return i == 1;
    }

    default boolean isMiddleButton(int i) {
        return i == 2;
    }
}
