package tech.sadnes.client.render.font;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public final class MsdfFontData {
    @SerializedName("atlas")
    private MsdfFontData.Atlas atlas;
    @SerializedName("metrics")
    private MsdfFontData.Metrics metrics;
    @SerializedName("glyphs")
    private List<MsdfFontData.Glyph> glyphs;
    @SerializedName("kerning")
    private List<MsdfFontData.KerningPair> kerning;

    public MsdfFontData.Atlas getAtlas() {
        return this.atlas;
    }

    public MsdfFontData.Metrics getMetrics() {
        return this.metrics;
    }

    public List<MsdfFontData.Glyph> getGlyphs() {
        return this.glyphs;
    }

    public List<MsdfFontData.KerningPair> getKerning() {
        return this.kerning;
    }

    public static final class Atlas {
        @SerializedName("distanceRange")
        private float distanceRange;
        @SerializedName("width")
        private float width;
        @SerializedName("height")
        private float height;

        public float getDistanceRange() {
            return this.distanceRange;
        }

        public float getWidth() {
            return this.width;
        }

        public float getHeight() {
            return this.height;
        }
    }

    public static final class Bounds {
        @SerializedName("left")
        private float left;
        @SerializedName("top")
        private float top;
        @SerializedName("right")
        private float right;
        @SerializedName("bottom")
        private float bottom;

        public float getLeft() {
            return this.left;
        }

        public float getTop() {
            return this.top;
        }

        public float getRight() {
            return this.right;
        }

        public float getBottom() {
            return this.bottom;
        }
    }

    public static final class Glyph {
        @SerializedName("unicode")
        private int unicode;
        @SerializedName("advance")
        private float advance;
        @SerializedName("planeBounds")
        private MsdfFontData.Bounds planeBounds;
        @SerializedName("atlasBounds")
        private MsdfFontData.Bounds atlasBounds;

        public int getUnicode() {
            return this.unicode;
        }

        public float getAdvance() {
            return this.advance;
        }

        public MsdfFontData.Bounds getPlaneBounds() {
            return this.planeBounds;
        }

        public MsdfFontData.Bounds getAtlasBounds() {
            return this.atlasBounds;
        }
    }

    public static final class KerningPair {
        @SerializedName("unicode1")
        private int unicode1;
        @SerializedName("unicode2")
        private int unicode2;
        @SerializedName("advance")
        private float advance;

        public int getUnicode1() {
            return this.unicode1;
        }

        public int getUnicode2() {
            return this.unicode2;
        }

        public float getAdvance() {
            return this.advance;
        }
    }

    public static final class Metrics {
        @SerializedName("lineHeight")
        private float lineHeight;
        @SerializedName("ascender")
        private float ascender;
        @SerializedName("descender")
        private float descender;

        public float getBaselineHeight() {
            return this.lineHeight + this.descender;
        }

        public float getLineHeight() {
            return this.lineHeight;
        }

        public float getAscender() {
            return this.ascender;
        }

        public float getDescender() {
            return this.descender;
        }
    }
}
