package tech.sadnes.client.cosmetic;

import java.util.function.Function;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderSetup.OutlineMode;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public record Cosmetic(
    String id,
    CosmeticType type,
    String name,
    Cosmetic.Anchor anchor,
    float offsetX,
    float offsetY,
    float offsetZ,
    float scale,
    CosmeticModel model,
    Identifier texture,
    Identifier preview,
    boolean smooth
) {
    private static final Function<Identifier, RenderLayer> DEPTH_SORTED_LAYERS = Util.memoize(
        identifier -> RenderLayer.of(
            "sadnes_cosmetic",
            RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT)
                .texture("Sampler0", identifier)
                .useLightmap()
                .useOverlay()
                .crumbling()
                .translucent()
                .outlineMode(OutlineMode.AFFECTS_OUTLINE)
                .build()
        )
    );

    public RenderLayer layer() {
        return DEPTH_SORTED_LAYERS.apply(this.texture);
    }

    public boolean isOnHead() {
        return this.anchor == Cosmetic.Anchor.HEAD || this.anchor == Cosmetic.Anchor.HEAD_TOP;
    }

    public float heightAboveHeadPivot() {
        return this.offsetY - this.anchor.getYOffset() - this.model.getMinY() * this.scale;
    }

    public static float headLift(float f) {
        return Float.isNaN(f) ? 0.0F : f - 0.5F;
    }

    public enum Anchor {
        HEAD(0.0F),
        HEAD_TOP(-0.5F),
        BODY(0.3F),
        ROOT(0.3F),
        HAND(0.0F);

        private final float yOffset;

        public float getYOffset() {
            return this.yOffset;
        }

        Anchor(float f) {
            this.yOffset = f;
        }
    }
}
