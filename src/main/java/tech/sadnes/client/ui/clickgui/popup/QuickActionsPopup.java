package tech.sadnes.client.ui.clickgui.popup;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.impl.misc.QuickInteract;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.ui.IRenderable;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class QuickActionsPopup implements IRenderable {
    private static final float WIDTH = 112.0F;
    private static final float HEADER_HEIGHT = 24.0F;
    private static final float TITLE_SIZE = 7.0F;
    private static final float PADDING = 8.0F;
    private static final float BOTTOM_PADDING = 2.0F;
    private final Animation openAnimation = new Animation();
    private final Map<QuickInteract.QuickAction, Animation> hoverAnimations = new EnumMap<>(QuickInteract.QuickAction.class);
    private List<QuickInteract.QuickAction> actions = List.of();
    private float x;
    private float y;
    private boolean visible;

    public void render(DrawContext drawContext, String string, List<QuickInteract.QuickAction> quickActions, float f, float f2, double d, double d2) {
        if (quickActions.isEmpty()) {
            this.hide();
        } else {
            this.actions = quickActions;
            float f3 = 24.0F + 20.0F * quickActions.size() + 2.0F;
            this.x = f - 56.0F;
            this.y = f2 - f3 / 2.0F;
            this.visible = true;
            this.openAnimation.update();
            this.openAnimation.animate(1.0, 0.3, Easings.BACK_OUT);
            float f4 = this.openAnimation.get();
            float f5 = Math.min(f4, 1.0F);
            MatrixUtil.push(drawContext);
            MatrixUtil.scale(drawContext, f, f2, 0.9F + 0.1F * f4);
            GuiRenderUtil.drawPopup(drawContext, this.x, this.y, 112.0F, f3, f5, 8.0F);
            new RenderUtil(drawContext, this.x, this.y, 112.0F, 24.0F)
                .textMiddleLeft(Fonts.INTER_SEMIBOLD, string, 8.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f5), 7.0F);
            GuiRenderUtil.drawOverlay(drawContext, this.x + 8.0F, MathUtil.roundToStep(this.y + 24.0F, 0.5F), 96.0F, 0.5F, f5);
            float f6 = this.y + 24.0F;
            int i = quickActions.size();

            for (int i2 = 0; i2 < i; i2++) {
                QuickInteract.QuickAction quickAction = (QuickInteract.QuickAction)quickActions.get(i2);
                if (i2 > 0) {
                    GuiRenderUtil.drawOverlay(drawContext, this.x + 8.0F, MathUtil.roundToStep(f6, 0.5F), 96.0F, 0.5F, f5);
                }

                Animation animation = this.hoverAnimations.computeIfAbsent(quickAction, quickAction2 -> new Animation());
                animation.update();
                animation.animate(this.isHovered(d, d2, this.x, f6, 112.0, 20.0) ? 1.0 : 0.0, 0.16, Easings.QUAD_OUT);
                PopupMenuItemRenderer.renderItem(drawContext, this.x, f6, 112.0F, quickAction.getIcon(), quickAction.getDisplayName(), animation.get(), false, f5);
                f6 += 20.0F;
            }

            MatrixUtil.pop(drawContext);
        }
    }

    public void hide() {
        this.visible = false;
        this.actions = List.of();
        this.openAnimation.forceValue(0.0);
        this.hoverAnimations.clear();
    }

    public QuickInteract.QuickAction getHoveredAction(double d, double d2) {
        if (!this.visible) {
            return null;
        }

        float f = this.y + 24.0F;
        int i = this.actions.size();

        for (int i2 = 0; i2 < i; i2++) {
            if (this.isHovered(d, d2, this.x, f, 112.0, 20.0)) {
                return this.actions.get(i2);
            }

            f += 20.0F;
        }

        return null;
    }
}
