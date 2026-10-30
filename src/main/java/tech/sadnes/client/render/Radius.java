package tech.sadnes.client.render;

import java.util.Map;
import java.util.WeakHashMap;

public final class Radius {
    private static final Map<String, Radius> CACHE = new WeakHashMap<>();
    private static final Radius ZERO = all(0.0F);
    private final float topLeft;
    private final float topRight;
    private final float bottomRight;
    private final float bottomLeft;
    private final boolean smooth;

    private Radius(float f, float f2, float f3, float f4, boolean flag) {
        this.topLeft = f;
        this.topRight = f2;
        this.bottomRight = f3;
        this.bottomLeft = f4;
        this.smooth = flag;
    }

    public static Radius zero() {
        return ZERO;
    }

    public static Radius of(float f, float f2, float f3, float f4, boolean flag) {
        String string = f + "_" + f2 + "_" + f3 + "_" + f4 + "_" + flag;
        return CACHE.computeIfAbsent(string, string2 -> new Radius(f, f2, f3, f4, flag));
    }

    public static Radius of(float f, float f2, float f3, float f4) {
        return of(f, f2, f3, f4, false);
    }

    public static Radius all(float f) {
        return of(f, f, f, f, false);
    }

    public static Radius copyOf(Radius radius) {
        return of(radius.topLeft, radius.topRight, radius.bottomRight, radius.bottomLeft, radius.smooth);
    }

    public static Radius smoothAll(float f) {
        return of(f, f, f, f, true);
    }

    public static Radius smooth(float f, float f2, float f3, float f4) {
        return of(f, f2, f3, f4, true);
    }

    public Radius doubled() {
        return of(this.topLeft * 2.0F, this.topRight * 2.0F, this.bottomRight * 2.0F, this.bottomLeft * 2.0F, true);
    }

    public Radius expand(float f) {
        return of(this.topLeft + f, this.topRight + f, this.bottomRight + f, this.bottomLeft + f, this.smooth);
    }

    public Radius shrink(float f) {
        return of(this.topLeft - f, this.topRight - f, this.bottomRight - f, this.bottomLeft - f, this.smooth);
    }

    public float getMax() {
        return Math.max(Math.max(this.topLeft, this.topRight), Math.max(this.bottomRight, this.bottomLeft));
    }

    public float getTopLeft() {
        return this.topLeft;
    }

    public float getTopRight() {
        return this.topRight;
    }

    public float getBottomRight() {
        return this.bottomRight;
    }

    public float getBottomLeft() {
        return this.bottomLeft;
    }

    public boolean isSmooth() {
        return this.smooth;
    }
}
