package tech.sadnes.client.ui.drag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.component.HudSettingsPopupComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.math.MathUtil;

public final class DragManager implements IMinecraft {
    private static final float SCREEN_MARGIN = 5.0F;
    private static final float SNAP_DISTANCE = 9.0F;
    private static final float NEIGHBOR_RANGE = 10.0F;
    private static final float ADJACENT_GAP = 3.0F;
    private static final float FAST_LERP_SPEED = 60.0F;
    private static final float LERP_SPEED = 14.0F;
    private static final float GUIDE_LERP_SPEED = 22.0F;
    private static final float SETTLE_EPSILON = 0.05F;
    private static final int MAX_CHAIN_DEPTH = 16;
    private final List<DragSetting> elements = new ArrayList<>();
    private DragSetting dragging;
    private final Vector2f dragOffset = new Vector2f();
    private final Animation verticalGuideAnimation = new Animation();
    private final Animation horizontalGuideAnimation = new Animation();
    private float snapGuideX = Float.NaN;
    private float snapGuideY = Float.NaN;
    private float renderGuideX = Float.NaN;
    private float renderGuideY = Float.NaN;
    private long lastFrameNanos;
    private int lastScreenWidth;
    private int lastScreenHeight;
    private DragSetting snapTargetX;
    private DragSetting snapTargetY;
    private int targetAnchorX = -1;
    private int selfAnchorX = -1;
    private int targetAnchorY = -1;
    private int selfAnchorY = -1;
    private int screenGridX = -1;
    private int screenSelfAnchorX = -1;
    private int screenGridY = -1;
    private int screenSelfAnchorY = -1;
    private boolean freeMove;
    private final Animation hintAnimation = new Animation();
    private final Animation snapXAnimation = new Animation();
    private final Animation snapYAnimation = new Animation();
    private float lastSnapX = Float.NaN;
    private float lastSnapY = Float.NaN;
    private final Map<DragSetting, Animation> highlightAnimations = new IdentityHashMap<>();
    private final Comparator<DragSetting> depthComparator = Comparator.<DragSetting>comparingInt(dragSetting -> dragSetting.isOnTop() ? 1 : 0)
        .thenComparingInt(this::getAnchorDepth);
    private final Predicate<DragSetting> removedPredicate = dragSetting -> !this.elements.contains(dragSetting);

    public List<DragSetting> getElements() {
        return this.elements;
    }

    public DragSetting getDragging() {
        return this.dragging;
    }

    public void register(DragSetting dragSetting) {
        if (dragSetting != null && !this.elements.contains(dragSetting)) {
            this.elements.add(dragSetting);
        }
    }

    public void unregister(DragSetting dragSetting) {
        if (dragSetting != null) {
            this.elements.remove(dragSetting);
            if (this.dragging == dragSetting) {
                this.dragging = null;
            }

            for (DragSetting dragSetting2 : this.elements) {
                if (dragSetting2.getAnchorTarget() == dragSetting) {
                    dragSetting2.clearAnchor();
                }
            }
        }
    }

    public void clear() {
        for (DragSetting dragSetting : this.elements) {
            dragSetting.clearAnchor();
            dragSetting.clearScreenAnchor();
        }

        this.elements.clear();
        this.dragging = null;
    }

    public static boolean isEditScreen(Screen screen) {
        return screen instanceof ChatScreen || screen instanceof ClickGuiScreen;
    }

    private boolean isEditing() {
        return isEditScreen(mc.currentScreen);
    }

    private boolean isOverClickGui(double d, double d2) {
        if (mc.currentScreen instanceof ClickGuiScreen clickGuiScreen) {
            float f = clickGuiScreen.getX();
            float f2 = clickGuiScreen.getY();
            return d >= f && d <= f + clickGuiScreen.getWidth() && d2 >= f2 && d2 <= f2 + clickGuiScreen.getHeight();
        } else {
            return false;
        }
    }

    private boolean isOverSettingsPopup(double d, double d2) {
        return HudSettingsPopupComponent.isAnyPopupHovered(d, d2);
    }

    private boolean isOverClickGuiOverlay(double d, double d2) {
        return mc.currentScreen instanceof ClickGuiScreen clickGuiScreen ? clickGuiScreen.getProfilePopup().isVisible() && clickGuiScreen.getProfilePopup().isHoveringAny(d, d2) : false;
    }

    public DragSetting getElementAt(double d, double d2) {
        for (int i = this.elements.size() - 1; i >= 0; i--) {
            DragSetting dragSetting = this.elements.get(i);
            if (dragSetting.getOwner() != null && dragSetting.isRendered() && this.isInside(dragSetting, d, d2)) {
                return dragSetting;
            }
        }

        return null;
    }

    public boolean isHovered(double d, double d2) {
        if (this.dragging != null) {
            return true;
        }

        if (!this.isOverClickGui(d, d2) && !this.isOverSettingsPopup(d, d2) && !this.isOverClickGuiOverlay(d, d2)) {
            for (int i = this.elements.size() - 1; i >= 0; i--) {
                DragSetting dragSetting = this.elements.get(i);
                if (dragSetting.isRendered() && this.isInside(dragSetting, d, d2)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public void mouseClicked(double d, double d2, int i) {
        if (this.isEditing() && i == 0) {
            if (this.isOverClickGui(d, d2)) {
                return;
            }

            if (this.isOverSettingsPopup(d, d2)) {
                return;
            }

            if (this.isOverClickGuiOverlay(d, d2)) {
                return;
            }

            for (int i2 = this.elements.size() - 1; i2 >= 0; i2--) {
                DragSetting dragSetting = this.elements.get(i2);
                if (dragSetting.isRendered() && dragSetting.isDraggable() && this.isInside(dragSetting, d, d2)) {
                    if (dragSetting.isInteractive()) {
                        return;
                    }

                    this.dragging = dragSetting;
                    dragSetting.clearAnchor();
                    dragSetting.clearScreenAnchor();
                    this.dragOffset.set((float)(dragSetting.position2.x - d), (float)(dragSetting.position2.y - d2));
                    this.bringToFront(dragSetting);
                    dragSetting.startDrag(d, d2);
                    return;
                }
            }
        }
    }

    public void mouseReleased(int i) {
        if (i == 0 && this.dragging != null) {
            if (!this.isAltHeld()) {
                this.applyAnchor(this.dragging);
            }

            this.dragging.commitTargetPosition();
            this.dragging.stopDrag();
            this.dragging = null;
            this.snapGuideX = Float.NaN;
            this.snapGuideY = Float.NaN;
            this.snapTargetX = null;
            this.snapTargetY = null;
            this.lastSnapX = Float.NaN;
            this.lastSnapY = Float.NaN;
        }
    }

    private boolean isAltHeld() {
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), 342) == 1;
    }

    private void applyAnchor(DragSetting dragSetting) {
        DragSetting dragSetting2 = this.snapTargetX != null ? this.snapTargetX : this.snapTargetY;
        if (dragSetting2 == null || dragSetting2 == dragSetting) {
            boolean flag = this.screenGridX >= 0;
            boolean flag2 = this.screenGridY >= 0;
            if (flag || flag2) {
                dragSetting.setScreenAnchor(
                    flag ? this.screenGridX : -1, flag ? this.screenSelfAnchorX : -1, flag2 ? this.screenGridY : -1, flag2 ? this.screenSelfAnchorY : -1
                );
                dragSetting.getAnchorAnimation().forceValue(1.0);
                dragSetting.getAnchorAnimation().animate(0.0, 0.6, Easings.EXPO_OUT);
            }
        } else if (this.isAncestorOf(dragSetting, dragSetting2)) {
            dragSetting.startShake();
        } else {
            int i = this.snapTargetX == dragSetting2 ? this.targetAnchorX : -1;
            int i2 = this.snapTargetX == dragSetting2 ? this.selfAnchorX : -1;
            int i3 = this.snapTargetY == dragSetting2 ? this.targetAnchorY : -1;
            int i4 = this.snapTargetY == dragSetting2 ? this.selfAnchorY : -1;
            float f = i < 0 ? dragSetting.getTargetPosition().x - dragSetting2.getTargetPosition().x : 0.0F;
            float f2 = i3 < 0 ? dragSetting.getTargetPosition().y - dragSetting2.getTargetPosition().y : 0.0F;
            dragSetting.setAnchor(dragSetting2, i, i2, f, i3, i4, f2);
            dragSetting.getAnchorAnimation().forceValue(1.0);
            dragSetting.getAnchorAnimation().animate(0.0, 0.6, Easings.EXPO_OUT);
        }
    }

    public void update(double d, double d2) {
        int i = mc.getWindow().getScaledWidth();
        int i2 = mc.getWindow().getScaledHeight();
        if (this.lastScreenWidth > 0 && (i != this.lastScreenWidth || i2 != this.lastScreenHeight)) {
            this.rescale(i, i2);
        }

        this.lastScreenWidth = i;
        this.lastScreenHeight = i2;
        this.freeMove = this.isAltHeld();
        if (this.dragging != null) {
            if (this.isEditing() && this.dragging.isDraggable()) {
                float f = (float)d + this.dragOffset.x;
                float f2 = (float)d2 + this.dragOffset.y;
                float f3 = Math.max(5.0F, i - this.dragging.size2.x - 5.0F);
                float f4 = Math.max(5.0F, i2 - this.dragging.size2.y - 5.0F);
                float f5 = MathUtil.clamp(5.0F, f3, f);
                float f6 = MathUtil.clamp(5.0F, f4, f2);
                if (this.freeMove) {
                    this.snapGuideX = Float.NaN;
                    this.snapGuideY = Float.NaN;
                    this.snapTargetX = null;
                    this.snapTargetY = null;
                    this.screenGridX = -1;
                    this.screenGridY = -1;
                    this.lastSnapX = Float.NaN;
                    this.lastSnapY = Float.NaN;
                    this.dragging.setTargetPosition(f5, f6);
                } else {
                    Vector2f vector2f = this.computeSnap(this.dragging, f5, f6, i, i2);
                    boolean flag = !Float.isNaN(this.snapGuideX);
                    boolean flag2 = !Float.isNaN(this.snapGuideY);
                    float f7;
                    if (!flag) {
                        this.snapXAnimation.forceValue(f5);
                        this.lastSnapX = Float.NaN;
                        f7 = f5;
                    } else {
                        if (Float.isNaN(this.lastSnapX) || Math.abs(this.lastSnapX - vector2f.x) > 0.5F) {
                            this.snapXAnimation.forceValue(f5);
                            this.snapXAnimation.animate(vector2f.x, 0.3, Easings.BACK_OUT);
                        }

                        this.lastSnapX = vector2f.x;
                        this.snapXAnimation.update();
                        f7 = this.snapXAnimation.get();
                    }

                    float f8;
                    if (!flag2) {
                        this.snapYAnimation.forceValue(f6);
                        this.lastSnapY = Float.NaN;
                        f8 = f6;
                    } else {
                        if (Float.isNaN(this.lastSnapY) || Math.abs(this.lastSnapY - vector2f.y) > 0.5F) {
                            this.snapYAnimation.forceValue(f6);
                            this.snapYAnimation.animate(vector2f.y, 0.3, Easings.BACK_OUT);
                        }

                        this.lastSnapY = vector2f.y;
                        this.snapYAnimation.update();
                        f8 = this.snapYAnimation.get();
                    }

                    this.dragging.setTargetPosition(f7, f8);
                }
            } else {
                if (!this.freeMove) {
                    this.applyAnchor(this.dragging);
                }

                this.dragging.commitTargetPosition();
                this.dragging.stopDrag();
                this.dragging = null;
                this.snapGuideX = Float.NaN;
                this.snapGuideY = Float.NaN;
                this.snapTargetX = null;
                this.snapTargetY = null;
                this.lastSnapX = Float.NaN;
                this.lastSnapY = Float.NaN;
            }
        } else {
            this.snapGuideX = Float.NaN;
            this.snapGuideY = Float.NaN;
            this.snapTargetX = null;
            this.snapTargetY = null;
            this.lastSnapX = Float.NaN;
            this.lastSnapY = Float.NaN;
        }

        float f9 = this.getDeltaTime();
        boolean flag3;
        if (this.dragging != null) {
            flag3 = !Float.isNaN(this.snapGuideX);
        } else {
            flag3 = false;
        }

        boolean flag4;
        if (this.dragging != null) {
            flag4 = !Float.isNaN(this.snapGuideY);
        } else {
            flag4 = false;
        }

        float f10 = 1.0F - (float)Math.exp(-22.0F * f9);
        if (flag3) {
            this.renderGuideX = Float.isNaN(this.renderGuideX) ? this.snapGuideX : MathUtil.lerp(this.renderGuideX, this.snapGuideX, f10);
        }

        if (flag4) {
            this.renderGuideY = Float.isNaN(this.renderGuideY) ? this.snapGuideY : MathUtil.lerp(this.renderGuideY, this.snapGuideY, f10);
        }

        this.verticalGuideAnimation.update();
        this.verticalGuideAnimation.animate(flag3 ? 1.0 : 0.0, 0.22, flag3 ? Easings.QUART_OUT : Easings.EXPO_OUT);
        this.horizontalGuideAnimation.update();
        this.horizontalGuideAnimation.animate(flag4 ? 1.0 : 0.0, 0.22, flag4 ? Easings.QUART_OUT : Easings.EXPO_OUT);
        if (!flag3 && this.verticalGuideAnimation.get() < 0.001F) {
            this.renderGuideX = Float.NaN;
        }

        if (!flag4 && this.horizontalGuideAnimation.get() < 0.001F) {
            this.renderGuideY = Float.NaN;
        }

        this.hintAnimation.update();
        this.hintAnimation.animate(this.dragging != null ? 1.0 : 0.0, 0.22, Easings.EXPO_OUT);

        for (DragSetting dragSetting : this.elements) {
            Animation animation = this.highlightAnimations.computeIfAbsent(dragSetting, dragSetting2 -> new Animation());
            boolean flag5 = this.dragging != null && dragSetting != this.dragging && (dragSetting == this.snapTargetX || dragSetting == this.snapTargetY);
            animation.update();
            animation.animate(flag5 ? 1.0 : 0.0, 0.2, Easings.EXPO_OUT);
        }

        this.highlightAnimations.keySet().removeIf(this.removedPredicate);
        this.elements.sort(this.depthComparator);
        DragSetting dragSetting3 = null;
        if (this.dragging == null && this.isEditing() && !this.isOverClickGui(d, d2)) {
            for (int i3 = this.elements.size() - 1; i3 >= 0; i3--) {
                DragSetting dragSetting4 = this.elements.get(i3);
                if (dragSetting4.isRendered() && dragSetting4.isDraggable() && this.isInside(dragSetting4, d, d2)) {
                    dragSetting3 = dragSetting4;
                    break;
                }
            }
        }

        for (DragSetting dragSetting5 : this.elements) {
            Animation animation2 = dragSetting5.getHoverAnimation();
            animation2.update();
            animation2.animate(dragSetting5 == dragSetting3 ? 1.0 : 0.0, 0.2, Easings.EXPO_OUT);
        }

        for (DragSetting dragSetting6 : this.elements) {
            DragSetting dragSetting7 = dragSetting6.getAnchorTarget();
            if (dragSetting7 != null && !this.elements.contains(dragSetting7)) {
                dragSetting6.clearAnchor();
            }
        }

        for (DragSetting dragSetting8 : this.elements) {
            if (!dragSetting8.isPositionInitialized()) {
                dragSetting8.resetToDefault();
            }

            DragSetting dragSetting9;
            boolean flag6;
            label219: {
                dragSetting9 = dragSetting8.getAnchorTarget();
                boolean flag7;
                if (dragSetting8 != this.dragging) {
                    if (dragSetting9 == null) {
                        flag6 = dragSetting8.isScreenAnchored();
                        break label219;
                    }

                    flag7 = true;
                } else {
                    flag7 = false;
                }

                flag6 = flag7;
            }

            if (dragSetting9 != null && dragSetting8 != this.dragging) {
                Vector2f vector2f2 = this.computeAnchoredPosition(dragSetting8, dragSetting9);
                dragSetting8.setTargetPosition(vector2f2.x, vector2f2.y);
            } else if (dragSetting8.isScreenAnchored() && dragSetting8 != this.dragging) {
                Vector2f vector2f3 = this.computeScreenAnchoredPosition(dragSetting8, i, i2);
                dragSetting8.setTargetPosition(vector2f3.x, vector2f3.y);
            }

            float f11 = Math.max(5.0F, i - dragSetting8.size2.x - 5.0F);
            float f12 = Math.max(5.0F, i2 - dragSetting8.size2.y - 5.0F);
            Vector2f vector2f4 = dragSetting8.getTargetPosition();
            float f13 = MathUtil.clamp(5.0F, f11, vector2f4.x);
            float f14 = MathUtil.clamp(5.0F, f12, vector2f4.y);
            float f15;
            float f16;
            if (flag6) {
                f15 = f13;
                f16 = f14;
            } else {
                float f17 = this.isFollowingDragged(dragSetting8) ? 60.0F : 14.0F;
                float f18 = 1.0F - (float)Math.exp(-f17 * f9);
                f15 = MathUtil.lerp(dragSetting8.position2.x, f13, f18);
                f16 = MathUtil.lerp(dragSetting8.position2.y, f14, f18);
                if (Math.abs(f15 - f13) < 0.05F && Math.abs(f16 - f14) < 0.05F) {
                    f15 = f13;
                    f16 = f14;
                }
            }

            float f19 = MathUtil.clamp(5.0F, f11, f15);
            float f20 = MathUtil.clamp(5.0F, f12, f16);
            float f21 = MathUtil.roundToStep(f19, 0.5F);
            float f22 = MathUtil.roundToStep(f20, 0.5F);
            dragSetting8.setRenderPosition(f21, f22);
            Animation animation3 = dragSetting8.getVisibilityAnimation();
            animation3.update();
            animation3.animate(dragSetting8.isVisible() ? 1.0 : 0.0, 0.25, dragSetting8.isVisible() ? Easings.BACK_OUT : Easings.EXPO_OUT, true);
            dragSetting8.setVisibility(animation3.get());
            Animation animation4 = dragSetting8.getDragAnimation();
            animation4.update();
            animation4.animate(dragSetting8 == this.dragging ? 1.0 : 0.0, 0.25, Easings.EXPO_OUT, true);
            dragSetting8.getAnchorAnimation().update();
        }
    }

    private void rescale(int i, int i2) {
        float f = (float)i / this.lastScreenWidth;
        float f2 = (float)i2 / this.lastScreenHeight;

        for (DragSetting dragSetting : this.elements) {
            if (dragSetting != this.dragging) {
                Vector2f vector2f = dragSetting.getTargetPosition();
                dragSetting.setPosition((vector2f.x + dragSetting.size2.x * 0.5F) * f - dragSetting.size2.x * 0.5F, (vector2f.y + dragSetting.size2.y * 0.5F) * f2 - dragSetting.size2.y * 0.5F);
            }
        }
    }

    public void render(DrawContext drawContext) {
        this.renderHighlights(drawContext);
        this.renderAnchorLinks(drawContext);
        float f = this.verticalGuideAnimation.get();
        float f2 = this.horizontalGuideAnimation.get();
        int i = mc.getWindow().getScaledWidth();
        int i2 = mc.getWindow().getScaledHeight();
        Radius radius = Radius.all(0.0F);
        int i3 = ColorUtil.gray(170);
        if (f > 0.001F && !Float.isNaN(this.renderGuideX)) {
            int i4 = ColorUtil.setAlpha(i3, (int)(90.0F * f));
            new RenderUtil(drawContext, this.renderGuideX - 0.5F, 0.0F, 1.0F, i2).rect(i4, radius);
        }

        if (f2 > 0.001F && !Float.isNaN(this.renderGuideY)) {
            int i5 = ColorUtil.setAlpha(i3, (int)(90.0F * f2));
            new RenderUtil(drawContext, 0.0F, this.renderGuideY - 0.5F, i, 1.0F).rect(i5, radius);
        }

        this.renderHint(drawContext, i2);
    }

    private void renderHighlights(DrawContext drawContext) {
        float f = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
        float f2 = 0.78F + 0.22F * (float)Math.sin(f * 3.4F);
        int i = ColorUtil.rgb(210, 235, 255);
        int i2 = ColorUtil.rgb(110, 175, 255);
        int i3 = ColorUtil.rgb(255, 230, 200);
        int i4 = ColorUtil.rgb(255, 170, 90);

        for (Entry entry : this.highlightAnimations.entrySet()) {
            float f3 = ((Animation)entry.getValue()).get();
            if (!(f3 < 0.005F)) {
                float f4 = (float)Easings.EXPO_OUT.ease(f3);
                this.renderGlowOutline(drawContext, (DragSetting)entry.getKey(), 14.0F, f4 * f2, i, i2);
            }
        }

        if (this.dragging != null) {
            float f5 = (float)this.dragging.getDragAnimation().getValue();
            if (f5 > 0.005F) {
                this.renderGlowOutline(drawContext, this.dragging, 14.0F, f5 * f2, i3, i4);
            }
        }
    }

    private void renderGlowOutline(DrawContext drawContext, DragSetting dragSetting, float f, float f2, int i, int i2) {
        float f3 = dragSetting.position2.x;
        float f4 = dragSetting.position2.y;
        float f5 = dragSetting.size2.x;
        float f6 = dragSetting.size2.y;
        Radius radius = Radius.all(6.0F);
        new RenderUtil(drawContext, f3, f4, f5, f6).outline(f, ColorUtil.setAlpha(i2, 20), f2, radius);
        new RenderUtil(drawContext, f3, f4, f5, f6).outline(f * 0.5F, ColorUtil.setAlpha(i, 20), f2, radius);
    }

    private void renderHint(DrawContext drawContext, int i) {
        float f = this.hintAnimation.get();
        if (!(f < 0.005F) && this.dragging != null) {
            MsdfFont msdfFont = Fonts.INTER_MEDIUM;
            String string = this.freeMove ? "drag.freeMove" : "drag.holdAlt";
            String string2 = this.freeMove ? "Free move" : "Hold LALT to move freely";
            String string3 = Language.translate(string, string2);
            float f2 = msdfFont.getWidth(string3, 5.5F);
            float f3 = this.dragging.position2.x + this.dragging.size2.x * 0.5F - f2 * 0.5F;
            float f4 = this.dragging.position2.y + this.dragging.size2.y + 5.0F;
            if (f4 + 5.5F + 2.0F > i) {
                f4 = this.dragging.position2.y - 5.5F - 5.0F;
            }

            int i2 = ColorUtil.setAlpha(ColorUtil.gray(220), (int)(220.0F * f));
            msdfFont.draw(drawContext, string3, f3, f4, i2, 5.5F);
        }
    }

    private void renderAnchorLinks(DrawContext drawContext) {
        boolean flag = this.isEditing();
        Radius radius = Radius.all(0.0F);
        int i = ColorUtil.gray(220);

        for (DragSetting dragSetting : this.elements) {
            DragSetting dragSetting2 = dragSetting.getAnchorTarget();
            if (dragSetting2 != null && dragSetting.isRendered() && dragSetting2.isRendered()) {
                float f = dragSetting.getAnchorAnimation().get();
                float f2 = flag ? 0.28F : 0.0F;
                float f3 = Math.min(1.0F, f2 + f);
                if (!(f3 < 0.005F)) {
                    int i2 = ColorUtil.setAlpha(i, (int)(230.0F * f3));
                    int i3 = ColorUtil.setAlpha(i, (int)(170.0F * f3));
                    float f4 = 0.6F + f * 1.2F;
                    int i4 = dragSetting.getAnchorTargetX();
                    int i5 = dragSetting.getAnchorSelfX();
                    int i6 = dragSetting.getAnchorTargetY();
                    int i7 = dragSetting.getAnchorSelfY();
                    if (i4 >= 0) {
                        float f5 = dragSetting2.position2.x + dragSetting2.size2.x * i4 * 0.5F;
                        float f6 = dragSetting.position2.x + dragSetting.size2.x * i5 * 0.5F;
                        this.renderLinkBar(drawContext, f5 - f4 * 0.5F, dragSetting2.position2.y, f4, dragSetting2.size2.y, 5.0F, i3, i2, radius);
                        this.renderLinkBar(drawContext, f6 - f4 * 0.5F, dragSetting.position2.y, f4, dragSetting.size2.y, 5.0F, i3, i2, radius);
                    }

                    if (i6 >= 0) {
                        float f7 = dragSetting2.position2.y + dragSetting2.size2.y * i6 * 0.5F;
                        float f8 = dragSetting.position2.y + dragSetting.size2.y * i7 * 0.5F;
                        this.renderLinkBar(drawContext, dragSetting2.position2.x, f7 - f4 * 0.5F, dragSetting2.size2.x, f4, 5.0F, i3, i2, radius);
                        this.renderLinkBar(drawContext, dragSetting.position2.x, f8 - f4 * 0.5F, dragSetting.size2.x, f4, 5.0F, i3, i2, radius);
                    }
                }
            }
        }
    }

    private void renderLinkBar(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, int i, int i2, Radius radius) {
        new RenderUtil(drawContext, f, f2, f3, f4).outline(f5, i).rect(i2, radius);
    }

    private boolean isOppositeEdges(int i, int i2) {
        return i == 2 && i2 == 0 || i == 0 && i2 == 2;
    }

    private Vector2f computeAnchoredPosition(DragSetting dragSetting, DragSetting dragSetting2) {
        float f = dragSetting2.position2.x;
        float f2 = dragSetting2.position2.y;
        float f3 = dragSetting2.size2.x;
        float f4 = dragSetting2.size2.y;
        float f5 = dragSetting.size2.x;
        float f6 = dragSetting.size2.y;
        int i = dragSetting.getAnchorTargetX();
        int i2 = dragSetting.getAnchorSelfX();
        int i3 = dragSetting.getAnchorTargetY();
        int i4 = dragSetting.getAnchorSelfY();
        float f7 = i >= 0 ? f + f3 * i * 0.5F - f5 * i2 * 0.5F + this.edgeGap(i, i2) : f + dragSetting.getAnchorOffsetX();
        float f8 = i3 >= 0 ? f2 + f4 * i3 * 0.5F - f6 * i4 * 0.5F + this.edgeGap(i3, i4) : f2 + dragSetting.getAnchorOffsetY();
        return new Vector2f(f7, f8);
    }

    private Vector2f computeScreenAnchoredPosition(DragSetting dragSetting, int i, int i2) {
        Vector2f vector2f = dragSetting.getTargetPosition();
        float f = vector2f.x;
        float f2 = vector2f.y;
        if (dragSetting.getScreenGridX() >= 0) {
            f = gridPosition(dragSetting.getScreenGridX(), i) - dragSetting.size2.x * dragSetting.getScreenSelfAnchorX() * 0.5F;
        }

        if (dragSetting.getScreenGridY() >= 0) {
            f2 = gridPosition(dragSetting.getScreenGridY(), i2) - dragSetting.size2.y * dragSetting.getScreenSelfAnchorY() * 0.5F;
        }

        return new Vector2f(f, f2);
    }

    private static float gridPosition(int i, float f) {
        if (i <= 0) {
            return 5.0F;
        } else {
            return i >= 8 ? f - 5.0F : f * (i * 0.125F);
        }
    }

    private float edgeGap(int i, int i2) {
        if (!this.isOppositeEdges(i, i2)) {
            return 0.0F;
        } else {
            return i == 2 ? 3.0F : -3.0F;
        }
    }

    private int getAnchorDepth(DragSetting dragSetting) {
        int i = 0;

        for (DragSetting dragSetting2 = dragSetting.getAnchorTarget(); dragSetting2 != null && i < 16; dragSetting2 = dragSetting2.getAnchorTarget()) {
            i++;
        }

        return i;
    }

    private boolean isAncestorOf(DragSetting dragSetting, DragSetting dragSetting2) {
        DragSetting dragSetting3 = dragSetting2;

        for (int i = 0; dragSetting3 != null && i < 16; i++) {
            if (dragSetting3 == dragSetting) {
                return true;
            }

            dragSetting3 = dragSetting3.getAnchorTarget();
        }

        return false;
    }

    private boolean isAnchoredTo(DragSetting dragSetting, DragSetting dragSetting2) {
        DragSetting dragSetting3 = dragSetting.getAnchorTarget();

        for (int i = 0; dragSetting3 != null && i < 16; i++) {
            if (dragSetting3 == dragSetting2) {
                return true;
            }

            dragSetting3 = dragSetting3.getAnchorTarget();
        }

        return false;
    }

    private boolean isFollowingDragged(DragSetting dragSetting) {
        if (this.dragging == null) {
            return false;
        }

        DragSetting dragSetting2 = dragSetting;

        for (int i = 0; dragSetting2 != null && i < 16; i++) {
            if (dragSetting2 == this.dragging) {
                return true;
            }

            dragSetting2 = dragSetting2.getAnchorTarget();
        }

        return false;
    }

    private Vector2f computeSnap(DragSetting dragSetting, float f, float f2, int i, int i2) {
        float f3 = dragSetting.size2.x;
        float f4 = dragSetting.size2.y;
        float[] floats = new float[]{0.0F, f3 * 0.5F, f3};
        float[] floats2 = new float[]{0.0F, f4 * 0.5F, f4};
        float[] floats3 = new float[9];
        float[] floats4 = new float[9];

        for (int i3 = 0; i3 < 9; i3++) {
            floats3[i3] = gridPosition(i3, i);
            floats4[i3] = gridPosition(i3, i2);
        }

        float f5 = 9.0F;
        float f6 = 9.0F;
        float f7 = f;
        float f8 = f2;
        float f9 = Float.NaN;
        float f10 = Float.NaN;
        DragSetting dragSetting2 = null;
        DragSetting dragSetting3 = null;
        int i4 = -1;
        int i5 = -1;
        int i6 = -1;
        int i7 = -1;
        int i8 = -1;
        int i9 = -1;
        int i10 = -1;
        int i11 = -1;

        for (int i12 = 0; i12 < 3; i12++) {
            float f11 = f + floats[i12];
            float f12 = f2 + floats2[i12];

            for (int i13 = 0; i13 < floats3.length; i13++) {
                float f13 = Math.abs(f11 - floats3[i13]);
                if (f13 < f5) {
                    f5 = f13;
                    f7 = floats3[i13] - floats[i12];
                    f9 = floats3[i13];
                    dragSetting2 = null;
                    i4 = -1;
                    i5 = -1;
                    i8 = i13;
                    i9 = i12;
                }
            }

            for (int i14 = 0; i14 < floats4.length; i14++) {
                float f14 = Math.abs(f12 - floats4[i14]);
                if (f14 < f6) {
                    f6 = f14;
                    f8 = floats4[i14] - floats2[i12];
                    f10 = floats4[i14];
                    dragSetting3 = null;
                    i6 = -1;
                    i7 = -1;
                    i10 = i14;
                    i11 = i12;
                }
            }
        }

        for (DragSetting dragSetting4 : this.elements) {
            if (dragSetting4 != dragSetting && dragSetting4.isRendered() && dragSetting4.isDraggable() && !this.isAnchoredTo(dragSetting4, dragSetting)) {
                float f15 = dragSetting4.position2.x;
                float f16 = dragSetting4.position2.y;
                float f17 = dragSetting4.size2.x;
                float f18 = dragSetting4.size2.y;
                float f19 = Math.min(f + f3, f15 + f17) - Math.max(f, f15);
                float f20 = Math.min(f2 + f4, f16 + f18) - Math.max(f2, f16);
                if (!(f19 > 0.0F) || !(f20 > 0.0F)) {
                    boolean flag = this.isWithinRange(f2, f2 + f4, f16, f16 + f18, 10.0F);
                    boolean flag2 = this.isWithinRange(f, f + f3, f15, f15 + f17, 10.0F);
                    if (flag2 || flag) {
                        float[] floats5 = new float[]{f15, f15 + f17 * 0.5F, f15 + f17};
                        float[] floats6 = new float[]{f16, f16 + f18 * 0.5F, f16 + f18};

                        for (int i15 = 0; i15 < 3; i15++) {
                            float f21 = f + floats[i15];
                            float f22 = f2 + floats2[i15];

                            for (int i16 = 0; i16 < 3; i16++) {
                                if (flag) {
                                    float f23 = floats5[i16];
                                    float f24 = Math.abs(f21 - f23);
                                    if (f24 < f5) {
                                        f5 = f24;
                                        f7 = f23 - floats[i15] + this.edgeGap(i16, i15);
                                        f9 = f23;
                                        dragSetting2 = dragSetting4;
                                        i4 = i16;
                                        i5 = i15;
                                        i8 = -1;
                                    }
                                }

                                if (flag2) {
                                    float f25 = floats6[i16];
                                    float f26 = Math.abs(f22 - f25);
                                    if (f26 < f6) {
                                        f6 = f26;
                                        f8 = f25 - floats2[i15] + this.edgeGap(i16, i15);
                                        f10 = f25;
                                        dragSetting3 = dragSetting4;
                                        i6 = i16;
                                        i7 = i15;
                                        i10 = -1;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        this.snapGuideX = f9;
        this.snapGuideY = f10;
        this.snapTargetX = dragSetting2;
        this.snapTargetY = dragSetting3;
        this.targetAnchorX = i4;
        this.selfAnchorX = i5;
        this.targetAnchorY = i6;
        this.selfAnchorY = i7;
        this.screenGridX = i8;
        this.screenSelfAnchorX = i9;
        this.screenGridY = i10;
        this.screenSelfAnchorY = i11;
        return new Vector2f(f7, f8);
    }

    private boolean isWithinRange(float f, float f2, float f3, float f4, float f5) {
        if (f2 < f3) {
            return f3 - f2 <= f5;
        } else {
            return f4 < f ? f - f4 <= f5 : true;
        }
    }

    private float getDeltaTime() {
        long l = System.nanoTime();
        if (this.lastFrameNanos == 0L) {
            this.lastFrameNanos = l;
            return 0.016666668F;
        } else {
            float f = (float)(l - this.lastFrameNanos) / 1.0E9F;
            this.lastFrameNanos = l;
            if (f <= 0.0F) {
                return 0.004166667F;
            } else {
                return f > 0.1F ? 0.1F : f;
            }
        }
    }

    private void bringToFront(DragSetting dragSetting) {
        if (!this.elements.isEmpty() && this.elements.contains(dragSetting)) {
            if (this.elements.get(this.elements.size() - 1) != dragSetting) {
                this.elements.remove(dragSetting);
                this.elements.add(dragSetting);
            }
        }
    }

    private boolean isInside(DragSetting dragSetting, double d, double d2) {
        return d >= dragSetting.position2.x && d <= dragSetting.position2.x + dragSetting.size2.x && d2 >= dragSetting.position2.y && d2 <= dragSetting.position2.y + dragSetting.size2.y;
    }
}
