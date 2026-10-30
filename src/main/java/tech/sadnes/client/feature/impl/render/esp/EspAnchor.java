package tech.sadnes.client.feature.impl.render.esp;

public enum EspAnchor {
    TOP("Top", 0.5F, 0.0F, 0.5F, 1.0F, 0, -1),
    BOTTOM("Bottom", 0.5F, 1.0F, 0.5F, 0.0F, 0, 1),
    LEFT("Left", 0.0F, 0.5F, 1.0F, 0.5F, -1, 0),
    RIGHT("Right", 1.0F, 0.5F, 0.0F, 0.5F, 1, 0),
    CENTER("Center", 0.5F, 0.5F, 0.5F, 0.5F, 0, 0),
    TOP_LEFT("TopLeft", 0.0F, 0.0F, 1.0F, 1.0F, -1, -1),
    TOP_RIGHT("TopRight", 1.0F, 0.0F, 0.0F, 1.0F, 1, -1),
    BOTTOM_LEFT("BottomLeft", 0.0F, 1.0F, 1.0F, 0.0F, -1, 1),
    BOTTOM_RIGHT("BottomRight", 1.0F, 1.0F, 0.0F, 0.0F, 1, 1);

    private final String displayName;
    private final float anchorX;
    private final float anchorY;
    private final float pivotX;
    private final float pivotY;
    private final int offsetX;
    private final int offsetY;

    EspAnchor(String string, float f, float f2, float f3, float f4, int i, int i2) {
        this.displayName = string;
        this.anchorX = f;
        this.anchorY = f2;
        this.pivotX = f3;
        this.pivotY = f4;
        this.offsetX = i;
        this.offsetY = i2;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public float getAnchorX() {
        return this.anchorX;
    }

    public float getAnchorY() {
        return this.anchorY;
    }

    public float getPivotX() {
        return this.pivotX;
    }

    public float getPivotY() {
        return this.pivotY;
    }

    public int getOffsetX() {
        return this.offsetX;
    }

    public int getOffsetY() {
        return this.offsetY;
    }

    public float resolveX(float f, float f2) {
        return f + (f2 - f) * this.anchorX;
    }

    public float resolveY(float f, float f2) {
        return f + (f2 - f) * this.anchorY;
    }

    public boolean isHorizontal() {
        return this == LEFT || this == RIGHT;
    }

    public boolean isVertical() {
        return this == TOP || this == BOTTOM;
    }

    public static EspAnchor fromName(String string, EspAnchor espAnchor) {
        if (string != null) {
            for (EspAnchor espAnchor2 : values()) {
                if (espAnchor2.displayName.equalsIgnoreCase(string)) {
                    return espAnchor2;
                }
            }
        }

        return espAnchor;
    }

    public static String[] namesOf(EspAnchor... espAnchors) {
        String[] strings = new String[espAnchors.length];

        for (int i = 0; i < espAnchors.length; i++) {
            strings[i] = espAnchors[i].displayName;
        }

        return strings;
    }
}
