package tech.sadnes.client.render;

import java.util.function.Consumer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record EntityPreviewState(
    EntityRenderState renderState,
    Vector3f translation,
    Quaternionf rotation,
    @Nullable Quaternionf overrideCameraAngle,
    @Nullable Consumer<MatrixStack> overlay,
    float alpha,
    int x1,
    int y1,
    int x2,
    int y2,
    float scale,
    int glowTint,
    float glowRadius,
    int chamsTint,
    int chamsStyle,
    float chamsSpeed,
    float chamsScale,
    boolean hideModel,
    @Nullable ScreenRect scissorArea,
    @Nullable ScreenRect bounds
) implements SpecialGuiElementRenderState {
    public EntityPreviewState(
        EntityRenderState entityRenderState,
        Vector3f vector3f,
        Quaternionf quaternionf,
        @Nullable Quaternionf quaternionf2,
        @Nullable Consumer<MatrixStack> consumer,
        float f,
        int i,
        int i2,
        int i3,
        int i4,
        float f2,
        int i5,
        float f3,
        int i6,
        int i7,
        float f4,
        float f5,
        boolean flag,
        @Nullable ScreenRect screenRect
    ) {
        this(
            entityRenderState,
            vector3f,
            quaternionf,
            quaternionf2,
            consumer,
            f,
            i,
            i2,
            i3,
            i4,
            f2,
            i5,
            f3,
            i6,
            i7,
            f4,
            f5,
            flag,
            screenRect,
            SpecialGuiElementRenderState.createBounds(i, i2, i3, i4, screenRect)
        );
    }

    public EntityPreviewState(
        EntityRenderState entityRenderState,
        Vector3f vector3f,
        Quaternionf quaternionf,
        @Nullable Quaternionf quaternionf2,
        @Nullable Consumer<MatrixStack> consumer,
        float f,
        int i,
        int i2,
        int i3,
        int i4,
        float f2,
        int i5,
        float f3,
        int i6,
        int i7,
        float f4,
        float f5,
        @Nullable ScreenRect screenRect
    ) {
        this(entityRenderState, vector3f, quaternionf, quaternionf2, consumer, f, i, i2, i3, i4, f2, i5, f3, i6, i7, f4, f5, false, screenRect);
    }
}
