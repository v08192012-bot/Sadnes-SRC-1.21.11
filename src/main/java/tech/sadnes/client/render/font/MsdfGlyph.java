package tech.sadnes.client.render.font;


public final class MsdfGlyph {
    private final int unicode;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;
    private final float advance;
    private final float top;
    private final float left;
    private final float width;
    private final float height;

    public MsdfGlyph(MsdfFontData.Glyph glyph, float f, float f2) {
        this.unicode = glyph.getUnicode();
        this.advance = glyph.getAdvance();
        MsdfFontData.Bounds bounds = glyph.getAtlasBounds();
        if (bounds != null) {
            this.u0 = bounds.getLeft() / f;
            this.u1 = bounds.getRight() / f;
            this.v0 = 1.0F - bounds.getTop() / f2;
            this.v1 = 1.0F - bounds.getBottom() / f2;
        } else {
            this.u0 = 0.0F;
            this.u1 = 0.0F;
            this.v0 = 0.0F;
            this.v1 = 0.0F;
        }

        MsdfFontData.Bounds bounds2 = glyph.getPlaneBounds();
        if (bounds2 != null) {
            this.left = bounds2.getLeft();
            this.width = bounds2.getRight() - bounds2.getLeft();
            this.height = bounds2.getTop() - bounds2.getBottom();
            this.top = bounds2.getTop();
        } else {
            this.left = 0.0F;
            this.width = 0.0F;
            this.height = 0.0F;
            this.top = 0.0F;
        }
    }

    public float getScaledAdvance(float f) {
        return this.advance * f;
    }

    public float getScaledHeight(float f) {
        return this.height * f;
    }

    public int getUnicode() {
        return this.unicode;
    }

    public float getU0() {
        return this.u0;
    }

    public float getU1() {
        return this.u1;
    }

    public float getV0() {
        return this.v0;
    }

    public float getV1() {
        return this.v1;
    }

    public float getAdvance() {
        return this.advance;
    }

    public float getTop() {
        return this.top;
    }

    public float getLeft() {
        return this.left;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }
}
