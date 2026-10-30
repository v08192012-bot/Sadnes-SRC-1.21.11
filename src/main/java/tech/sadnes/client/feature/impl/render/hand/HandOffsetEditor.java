package tech.sadnes.client.feature.impl.render.hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tech.sadnes.api.mixin.accessor.ChatHudAccessor;
import tech.sadnes.api.mixin.accessor.GameRendererAccessor;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseScrollEvent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.render.shader.GlowRenderer;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.ui.IInteractable;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

final class HandOffsetEditor implements IInteractable, IMinecraft {
    private static final float HAND_BASE_X = 0.56F;
    private static final float HAND_BASE_Y = -0.52F;
    private static final float HAND_BASE_Z = -0.72F;
    private static final float GRIP_TOP_X = 0.21F;
    private static final float GRIP_TOP_Y = 0.02F;
    private static final float GRIP_BOTTOM_X = 0.26F;
    private static final float GRIP_BOTTOM_Y = 0.45F;
    private static final float HIT_RADIUS_FACTOR = 0.16F;
    private static final float MENU_PADDING = 8.0F;
    private static final float POPUP_RADIUS = 8.0F;
    private static final float CLOSE_ICON_SIZE = 8.0F;
    private static final float MENU_HEADER_HEIGHT = 24.0F;
    private static final float TITLE_FONT_SIZE = 7.0F;
    private static final float CHECK_ICON_SIZE = 8.0F;
    private static final float CLOSE_BUTTON_SIZE = 24.0F;
    private static final float ROW_HEIGHT = 10.0F;
    private static final float SLIDER_RADIUS = 1.5F;
    private static final float OPTION_TEXT_OFFSET = 14.0F;
    private static final float HIGHLIGHT_HEIGHT = 8.0F;
    private static final float SECTION_GAP = 4.0F;
    private static final float SLIDER_HEIGHT = 3.0F;
    private static final float TUTORIAL_WIDTH = 216.0F;
    private static final float TUTORIAL_PADDING = 16.0F;
    private static final float TUTORIAL_TITLE_HEIGHT = 20.0F;
    private static final float TUTORIAL_ROW_HEIGHT = 18.0F;
    private static final float TUTORIAL_KEY_HEIGHT = 14.0F;
    private static final float TUTORIAL_ICON_SIZE = 11.0F;
    private final HandOffset handOffset;
    private final List<String> swingOptions = new ArrayList<>();
    private final Animation mainHandHover = new Animation();
    private final Animation offHandHover = new Animation();
    private final Animation menuAnimation = new Animation();
    private final List<Animation> rowAnimations = new ArrayList<>();
    private final Animation tutorialAnimation = new Animation();
    private final Vector3f rayDirection = new Vector3f();
    private final Vector3f[] corners = new Vector3f[16];
    private final Vector3f sweepStart = new Vector3f();
    private final Vector3f sweepEnd = new Vector3f();
    private final Vector4f sweepBounds = new Vector4f();
    private final float[] screenX = new float[16];
    private final float[] screenY = new float[16];
    private Hand sweepHand;
    private long sweepStartTime;
    private Hand hoveredHand;
    private Hand draggedHand;
    private float dragOffsetX;
    private float dragOffsetY;
    private Hand menuHand;
    private Hand shownMenuHand;
    private Hand queuedMenuHand;
    private float queuedMenuX;
    private float queuedMenuY;
    private float menuX;
    private float menuY;
    private float menuWidth;
    private float menuHeight;
    private float menuScale = 1.0F;
    private float swingListY;
    private float sizeSliderY;
    private float resetButtonY;
    private boolean draggingSize;

    HandOffsetEditor(HandOffset handOffset2) {
        this.handOffset = handOffset2;
        int i = 0;

        for (int i2 = this.corners.length; i < i2; i++) {
            this.corners[i] = new Vector3f();
        }
    }

    float getHoverProgress() {
        return Math.max(this.mainHandHover.get(), this.offHandHover.get());
    }

    boolean isActive() {
        return mc.player != null && mc.currentScreen instanceof ChatScreen;
    }

    void onKey(ScreenKeyEvent screenKeyEvent) {
        if (!this.isActive()) {
            this.reset();
        } else if (this.isTutorialVisible()) {
            if (screenKeyEvent.isPress()) {
                this.handOffset.markTutorialSeen();
            }
        } else {
            double d = screenKeyEvent.getMouseX();
            double d2 = screenKeyEvent.getMouseY();
            if (screenKeyEvent.isRelease()) {
                this.draggedHand = null;
                this.draggingSize = false;
            } else if (screenKeyEvent.isPress()) {
                if (this.menuHand != null && this.isInsideMenu(d, d2)) {
                    this.handleMenuClick(d, d2, screenKeyEvent.getKey());
                } else {
                    Hand hand = this.pickHand(d, d2);
                    if (hand == null) {
                        if (this.menuHand != null) {
                            this.closeMenu();
                        }
                    } else if (screenKeyEvent.getKey() == 1) {
                        if (this.menuHand == hand) {
                            this.closeMenu();
                        } else if (this.shownMenuHand != null) {
                            this.closeMenu();
                            this.queuedMenuHand = hand;
                            this.queuedMenuX = (float)d;
                            this.queuedMenuY = (float)d2;
                        } else {
                            this.openMenu(hand, (float)d, (float)d2);
                        }
                    } else if (screenKeyEvent.getKey() == 0) {
                        Vector2f vector2f = this.screenToHandOffset(hand, d, d2);
                        if (vector2f != null) {
                            this.draggedHand = hand;
                            this.dragOffsetX = this.handOffset.getXSetting(hand).getValue() - vector2f.x;
                            this.dragOffsetY = this.handOffset.getYSetting(hand).getValue() - vector2f.y;
                        }
                    }
                }
            }
        }
    }

    void onScroll(ScreenMouseScrollEvent screenMouseScrollEvent) {
        if (this.isActive() && !this.isTutorialVisible() && screenMouseScrollEvent.getVerticalAmount() != 0.0) {
            Hand hand = this.menuHand != null && this.isInsideMenu(screenMouseScrollEvent.getMouseX(), screenMouseScrollEvent.getMouseY())
                ? this.menuHand
                : this.pickHand(screenMouseScrollEvent.getMouseX(), screenMouseScrollEvent.getMouseY());
            if (hand != null) {
                NumberSetting numberSetting = this.handOffset.getSizeSetting(hand);
                numberSetting.setValue(Float.valueOf(numberSetting.getValue() + (float)Math.signum(screenMouseScrollEvent.getVerticalAmount()) * numberSetting.getStep()));
                this.handOffset.saveConfig();
                screenMouseScrollEvent.setCancelled(true);
            }
        }
    }

    void onMouseMove(double d, double d2) {
        if (!this.isActive()) {
            this.reset();
        } else {
            if (this.draggedHand != null) {
                Vector2f vector2f = this.screenToHandOffset(this.draggedHand, d, d2);
                if (vector2f != null) {
                    this.handOffset.getXSetting(this.draggedHand).setValue(Float.valueOf(vector2f.x + this.dragOffsetX));
                    this.handOffset.getYSetting(this.draggedHand).setValue(Float.valueOf(vector2f.y + this.dragOffsetY));
                    this.handOffset.saveConfig();
                }
            }

            if (this.draggingSize && this.menuHand != null) {
                this.updateSizeFromMouse(d);
            }

            if (this.isTutorialVisible() || this.menuHand != null && this.isInsideMenu(d, d2)) {
                this.hoveredHand = null;
            } else {
                this.hoveredHand = this.pickHand(d, d2);
            }
        }
    }

    void render(DrawContext drawContext, float f, float f2) {
        this.mainHandHover.update();
        this.offHandHover.update();
        this.mainHandHover.animate(this.isHandHighlighted(Hand.MAIN_HAND) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        this.offHandHover.animate(this.isHandHighlighted(Hand.OFF_HAND) ? 1.0 : 0.0, 0.15, Easings.QUAD_OUT);
        this.renderHandLabel(drawContext, f, f2);
        this.menuAnimation.update();
        if (this.menuHand != null) {
            this.menuAnimation.animate(1.0, 0.3, Easings.BACK_OUT);
        } else {
            this.menuAnimation.animate(0.0, 0.2, Easings.QUAD_IN);
        }

        float f3 = this.menuAnimation.get();
        if (this.menuHand == null && f3 <= 0.001F) {
            this.shownMenuHand = null;
            if (this.queuedMenuHand != null) {
                this.openMenu(this.queuedMenuHand, this.queuedMenuX, this.queuedMenuY);
                this.queuedMenuHand = null;
            }
        }

        if (this.shownMenuHand != null && f3 > 0.001F) {
            this.renderMenu(drawContext, f, f2, f3);
        }

        this.tutorialAnimation.update();
        if (this.isTutorialVisible()) {
            this.tutorialAnimation.animate(1.0, 0.3, Easings.BACK_OUT);
        } else {
            this.tutorialAnimation.animate(0.0, 0.2, Easings.QUAD_IN);
        }

        if (this.tutorialAnimation.get() > 0.01F) {
            this.renderTutorial(drawContext, this.tutorialAnimation.get());
        }
    }

    private boolean isTutorialVisible() {
        return !this.handOffset.isTutorialSeen();
    }

    private void renderTutorial(DrawContext drawContext, float f) {
        float f2 = Math.min(f, 1.0F);
        float f3 = GuiScale.getScaledWidth();
        float f4 = GuiScale.getScaledHeight();
        new RenderUtil(drawContext, 0.0F, 0.0F, f3, f4)
            .blur(f2, Radius.zero(), RenderUtil.BlurQuality.MIDDLE, true)
            .rect(Theme.withAlpha(Theme.getBackground(), f2 * 0.5F));
        String string = Language.translate("handoffset.tutorial.title", "Редактор рук");
        String string2 = Language.translate("handoffset.tutorial.continue", "Нажмите, чтобы продолжить");
        String[] strings = new String[]{
            Language.translate("handoffset.tutorial.lmb", "ЛКМ"),
            Language.translate("handoffset.tutorial.rmb", "ПКМ"),
            Language.translate("handoffset.tutorial.wheel", "Колесо")
        };
        String[] strings2 = new String[]{
            Language.translate("handoffset.tutorial.drag", "Перетащить руку с предметом"),
            Language.translate("handoffset.tutorial.menu", "Анимация удара и размер"),
            Language.translate("handoffset.tutorial.size", "Размер руки")
        };
        float f5 = 0.0F;

        for (String string3 : strings) {
            f5 = Math.max(f5, Fonts.INTER_SEMIBOLD.getWidth(string3, 6.5F));
        }

        float f6 = (float)Math.ceil(f5 + 14.0F);
        float f7 = Math.round((f3 - 216.0F) * 0.5F);
        float f8 = Math.round((f4 - 129.0F) * 0.5F);
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f7 + 108.0F, f8 + 129.0F / 2.0F, 0.9F + 0.1F * f);
        GuiRenderUtil.drawPopup(drawContext, f7, f8, 216.0F, 129.0F, f2, 8.0F);
        float f9 = Fonts.INTER_SEMIBOLD.getWidth(string, 9.5F);
        float f10 = f7 + (216.0F - (17.0F + f9)) / 2.0F;
        float f11 = f8 + 16.0F;
        new RenderUtil(drawContext, f10, f11 + 4.5F, 11.0F, 11.0F).icon(Icon.HAND_POINTER, 0.0F, 0.0F, 11.0F, Theme.withAlpha(Theme.getPrimary(), f2));
        new RenderUtil(drawContext, f10 + 11.0F + 6.0F, f11, f9, 20.0F)
            .textMiddleLeft(Fonts.INTER_SEMIBOLD, string, Theme.withAlpha(Theme.getTextColor(), f2), 9.5F);
        GuiRenderUtil.drawOverlay(drawContext, f7 + 16.0F, MathUtil.roundToStep(f11 + 20.0F + 4.0F, 0.5F), 184.0F, 0.5F, f2);
        float f12 = f11 + 20.0F + 8.0F;

        for (int i = 0; i < 3; i++) {
            this.renderTutorialRow(drawContext, f, f2, i, f7 + 16.0F, f6, 184.0F, f12 + i * 18.0F, strings[i], strings2[i]);
        }

        new RenderUtil(drawContext, f7, f12 + 54.0F + 6.0F, 216.0F, 9.0F)
            .textCenter(Fonts.INTER_MEDIUM, string2, Theme.withAlpha(Theme.getSecondaryText(), f2 * 0.6F), 6.0F);
        MatrixUtil.pop(drawContext);
    }

    private void renderTutorialRow(
        DrawContext drawContext, float f, float f2, int i, float f3, float f4, float f5, float f6, String string, String string2
    ) {
        float f7 = Math.max(0.0F, Math.min(1.0F, (f - 0.18F * i) / 0.55F));
        if (!(f7 <= 0.001F)) {
            float f8 = f2 * f7;
            float f9 = (1.0F - f7) * 8.0F;
            float f10 = f6 + 2.0F;
            Radius radius = Radius.smoothAll(7.0F);
            new RenderUtil(drawContext, f3 + f9, f10, f4, 14.0F)
                .snapped()
                .rect(Theme.withAlpha(Theme.getPrimary(), f8 * 0.16F), radius)
                .shadow(0.5F, Theme.getPrimary(), f8 * 0.4F, radius)
                .textCenter(Fonts.INTER_SEMIBOLD, string, Theme.withAlpha(Theme.getPrimary(), f8), 6.5F);
            float f11 = f4 + 9.0F;
            new RenderUtil(drawContext, f3 + f9 + f11, f6, f5 - f11, 18.0F)
                .textMiddleLeft(Fonts.INTER_MEDIUM, string2, Theme.withAlpha(Theme.getTextColor(), f8 * 0.9F), 7.0F);
        }
    }

    private boolean isHandHighlighted(Hand hand) {
        return this.hoveredHand == hand || this.draggedHand == hand || this.menuHand == hand;
    }

    private void renderHandLabel(DrawContext drawContext, float f, float f2) {
        float f3 = this.mainHandHover.get();
        float f4 = this.offHandHover.get();
        float f5 = Math.max(f3, f4);
        if (!(f5 <= 0.01F)) {
            Hand hand = f3 >= f4 ? Hand.MAIN_HAND : Hand.OFF_HAND;
            Vector2f vector2f = this.getHandLabelAnchor(hand);
            float f6 = vector2f != null ? vector2f.x : f;
            float f7 = vector2f != null ? vector2f.y - 20.0F : f2 - 26.0F;
            float f8 = Math.max(85.0F, Math.min(f6, GuiScale.getScaledWidth() - 85.0F));
            float f9 = Math.max(2.0F, Math.min(f7, GuiScale.getScaledHeight() - 22.0F));
            String string = Language.translate(
                hand == Hand.MAIN_HAND ? "handoffset.editor.main" : "handoffset.editor.off", hand == Hand.MAIN_HAND ? "Основная рука" : "Вторая рука"
            );
            String string2 = Language.translate("handoffset.editor.hint", "ЛКМ — двигать, ПКМ — меню, колесо — размер");
            new RenderUtil(drawContext, f8 - 85.0F, f9, 170.0F, 8.0F)
                .textTopCenter(Fonts.INTER_SEMIBOLD, string, Theme.withAlpha(Theme.getTextColor(), f5), 6.5F);
            new RenderUtil(drawContext, f8 - 85.0F, f9 + 9.0F, 170.0F, 8.0F)
                .textTopCenter(Fonts.INTER_MEDIUM, string2, Theme.withAlpha(Theme.getSecondaryText(), f5 * 0.75F), 5.5F);
        }
    }

    private Vector2f getHandLabelAnchor(Hand hand) {
        int i = this.handOffset.getHandCorners(hand, this.corners);
        if (i == 0) {
            return null;
        }

        float f = this.getProjectionScale();
        float f2 = this.getAspectRatio();
        if (!(f <= 0.0F) && !(f2 <= 0.0F)) {
            float f3 = GuiScale.getScaledWidth() * 0.5F;
            float f4 = GuiScale.getScaledHeight() * 0.5F;
            float f5 = Float.MAX_VALUE;
            float f6 = Float.MAX_VALUE;
            float f7 = -Float.MAX_VALUE;

            for (int i2 = 0; i2 < i; i2++) {
                Vector3f vector3f = this.corners[i2];
                float f8 = -vector3f.z;
                if (!(f8 < 0.15F)) {
                    float f9 = Math.max(0.0F, Math.min(f3 + f * vector3f.x / (f2 * f8) * f3, GuiScale.getScaledWidth()));
                    float f10 = Math.max(0.0F, Math.min(f4 - f * vector3f.y / f8 * f4, GuiScale.getScaledHeight()));
                    f5 = Math.min(f5, f10);
                    f6 = Math.min(f6, f9);
                    f7 = Math.max(f7, f9);
                }
            }

            return f5 == Float.MAX_VALUE ? null : new Vector2f((f6 + f7) * 0.5F, f5);
        } else {
            return null;
        }
    }

    void renderHandSweep(float f, MatrixStack matrixStack, HeldItemRenderer heldItemRenderer, ClientPlayerEntity clientPlayerEntity, int i) {
        if (this.isActive() && !this.isTutorialVisible()) {
            float f2 = this.mainHandHover.get();
            float f3 = this.offHandHover.get();
            float f4 = Math.max(f2, f3);
            if (f4 <= 0.02F) {
                this.sweepHand = null;
            } else {
                Hand hand = f2 >= f3 ? Hand.MAIN_HAND : Hand.OFF_HAND;
                if (this.handOffset.wasHandRenderedRecently(hand)) {
                    float f5 = this.getProjectionScale();
                    float f6 = this.getAspectRatio();
                    if (!(f5 <= 0.0F) && !(f6 <= 0.0F)) {
                        if (this.computeSweepLine(hand, f5, f6)) {
                            float f7 = this.sweepEnd.x - this.sweepStart.x;
                            float f8 = this.sweepEnd.y - this.sweepStart.y;
                            float f9 = (float)Math.hypot(f7, f8);
                            if (!(f9 < 0.001F)) {
                                float f10 = Math.min(0.45F, 0.75F / f9);
                                GlowRenderer.INSTANCE
                                    .renderHandSweep(
                                        matrixStack,
                                        f,
                                        heldItemRenderer,
                                        clientPlayerEntity,
                                        i,
                                        hand,
                                        3.5F,
                                        ColorUtil.setAlpha(Theme.getPrimary(), 255),
                                        this.getSweepPhase(hand),
                                        f10,
                                        f7 / (f9 * f9),
                                        f8 / (f9 * f9),
                                        this.sweepStart.x,
                                        this.sweepStart.y,
                                        f6,
                                        f4,
                                        this.sweepBounds.x,
                                        this.sweepBounds.y,
                                        this.sweepBounds.z,
                                        this.sweepBounds.w
                                    );
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean computeSweepLine(Hand hand, float f, float f2) {
        int i = this.getHandSide(hand);
        float f3 = this.handOffset.getSmoothedX(hand) + i * 0.56F;
        float f4 = this.handOffset.getSmoothedY(hand) + -0.52F;
        float f5 = this.handOffset.getSmoothedZ(hand) + -0.72F;
        int i2 = this.handOffset.getHandCorners(hand, this.corners);
        int i3 = -1;
        int i4 = -1;
        float f6 = 0.0F;
        float f7 = Float.MAX_VALUE;
        float f8 = Float.MAX_VALUE;
        float f9 = -Float.MAX_VALUE;
        float f10 = -Float.MAX_VALUE;
        boolean flag = false;

        for (int i5 = 0; i5 < i2; i5++) {
            Vector3f vector3f = this.corners[i5];
            float f11 = -vector3f.z;
            if (f11 < 0.15F) {
                this.screenX[i5] = Float.NaN;
                flag = true;
            } else {
                this.screenX[i5] = (f * vector3f.x / (f2 * f11) + 1.0F) * 0.5F * f2;
                this.screenY[i5] = (f * vector3f.y / f11 + 1.0F) * 0.5F;
                f7 = Math.min(f7, this.screenX[i5]);
                f8 = Math.min(f8, this.screenY[i5]);
                f9 = Math.max(f9, this.screenX[i5]);
                f10 = Math.max(f10, this.screenY[i5]);

                for (int i6 = 0; i6 < i5; i6++) {
                    if (!Float.isNaN(this.screenX[i6])) {
                        float f12 = this.screenX[i5] - this.screenX[i6];
                        float f13 = this.screenY[i5] - this.screenY[i6];
                        float f14 = f12 * f12 + f13 * f13;
                        if (!(f14 <= f6)) {
                            f6 = f14;
                            i3 = i6;
                            i4 = i5;
                        }
                    }
                }
            }
        }

        if (i3 >= 0 && !flag) {
            float f15 = 24.0F / Math.max(1, mc.getWindow().getFramebufferWidth());
            float f16 = 24.0F / Math.max(1, mc.getWindow().getFramebufferHeight());
            this.sweepBounds
                .set(Math.max(0.0F, f7 / f2 - f15), Math.max(0.0F, f8 - f16), Math.min(1.0F, f9 / f2 + f15), Math.min(1.0F, f10 + f16));
        } else {
            this.sweepBounds.set(0.0F, 0.0F, 1.0F, 1.0F);
        }

        if (i3 < 0) {
            return this.computeFallbackSweepLine(f3, f4, f5, i, f, f2);
        }

        int i7;
        int i8;
        if (this.corners[i3].distanceSquared(f3, f4, f5) > this.corners[i4].distanceSquared(f3, f4, f5)) {
            i7 = i4;
            i8 = i3;
        } else {
            i7 = i3;
            i8 = i4;
        }

        int i9 = i8;
        this.sweepStart.set(this.screenX[i7], this.screenY[i7], 0.0F);
        this.sweepEnd.set(this.screenX[i9], this.screenY[i9], 0.0F);
        return true;
    }

    private boolean computeFallbackSweepLine(float f, float f2, float f3, int i, float f4, float f5) {
        float f6 = -f3;
        if (f6 < 0.15F) {
            return false;
        }

        float f7 = f + i * 0.21F;
        float f8 = f2 + 0.02F;
        float f9 = f - i * 0.26F;
        float f10 = f2 + 0.45F;
        this.sweepStart.set((f4 * f7 / (f5 * f6) + 1.0F) * 0.5F * f5, (f4 * f8 / f6 + 1.0F) * 0.5F, 0.0F);
        this.sweepEnd.set((f4 * f9 / (f5 * f6) + 1.0F) * 0.5F * f5, (f4 * f10 / f6 + 1.0F) * 0.5F, 0.0F);
        return true;
    }

    private float getSweepPhase(Hand hand) {
        if (this.sweepHand != hand) {
            this.sweepHand = hand;
            this.sweepStartTime = System.nanoTime();
        }

        float f = (float)(System.nanoTime() - this.sweepStartTime) / 1.0E9F;
        float f2 = f / 5.0F;
        float f3 = f2 - (float)Math.floor(f2);
        float f4 = Math.min(f3 / 0.85F, 1.0F);
        if (f4 < 0.5F) {
            float f5 = 2.0F * f4 * f4;
            return -0.45F + f5 * 1.9F;
        } else {
            float f6 = 1.0F - 2.0F * (1.0F - f4) * (1.0F - f4);
            return -0.45F + f6 * 1.9F;
        }
    }

    private void renderMenu(DrawContext drawContext, float f, float f2, float f3) {
        float f4 = Math.min(f3, 1.0F);
        this.menuScale = 0.9F + 0.1F * f3;
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, this.menuX + this.menuWidth / 2.0F, this.menuY + this.menuHeight / 2.0F, this.menuScale);
        GuiRenderUtil.drawPopup(drawContext, this.menuX, this.menuY, this.menuWidth, this.menuHeight, f4, 8.0F)
            .scissor(() -> this.renderMenuContent(drawContext, f, f2, f4));
        MatrixUtil.pop(drawContext);
    }

    private void renderMenuContent(DrawContext drawContext, float f, float f2, float f3) {
        float f4 = this.menuX + 8.0F;
        float f5 = this.menuWidth - 16.0F;
        String string = Language.translate(
            this.shownMenuHand == Hand.MAIN_HAND ? "handoffset.editor.main" : "handoffset.editor.off",
            this.shownMenuHand == Hand.MAIN_HAND ? "Основная рука" : "Вторая рука"
        );
        new RenderUtil(drawContext, this.menuX, this.menuY, this.menuWidth, 24.0F)
            .textMiddleLeft(Fonts.INTER_SEMIBOLD, string, 8.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.getTextColor(), f3), 7.0F);
        boolean flag = isInside(f, f2, this.menuX + this.menuWidth - 24.0F, this.menuY, 24.0F, 24.0F);
        new RenderUtil(drawContext, this.menuX + this.menuWidth - 8.0F - 8.0F + 2.0F, this.menuY + 12.0F - 4.0F, 8.0F, 8.0F)
            .icon(Icon.CLOSE, 0.0F, 0.0F, 8.0F, ColorUtil.multiplyAlpha(flag ? Theme.getTextColor() : Theme.getSecondaryText(), f3));
        GuiRenderUtil.drawOverlay(drawContext, f4, MathUtil.roundToStep(this.menuY + 24.0F, 0.5F), this.menuWidth - 16.0F, 0.5F, f3);
        new RenderUtil(drawContext, f4, this.swingListY - 10.0F, f5, 10.0F)
            .textMiddleLeft(
                Fonts.INTER_MEDIUM,
                Language.translate("handoffset.editor.swing", "Анимация удара"),
                Theme.withAlpha(Theme.getSecondaryText(), f3 * 0.7F),
                5.5F
            );
        int i = this.getSelectedSwingIndex();
        int i2 = this.swingOptions.size();

        for (int i3 = 0; i3 < i2; i3++) {
            this.renderSwingOption(drawContext, f, f2, f3, i3, f4, f5, this.swingListY + i3 * 11.5F, this.swingOptions.get(i3), i3 == i);
        }

        NumberSetting numberSetting = this.handOffset.getSizeSetting(this.shownMenuHand);
        new RenderUtil(drawContext, f4, this.sizeSliderY, f5, 10.0F)
            .textMiddleLeft(
                Fonts.INTER_MEDIUM, Language.translate("handoffset.editor.size", "Размер"), Theme.withAlpha(Theme.getSecondaryText(), f3 * 0.7F), 5.5F
            )
            .textMiddleRight(Fonts.INTER_MEDIUM, String.format(Locale.ROOT, "%.2f", numberSetting.getValue()), Theme.withAlpha(Theme.getTextColor(), f3), 5.5F);
        float f6 = this.getSliderY();
        float f7 = (numberSetting.getValue() - numberSetting.getMin()) / (numberSetting.getMax() - numberSetting.getMin());
        new RenderUtil(drawContext, f4, f6, f5, 3.0F).rect(Theme.withAlpha(Theme.getWhite(), f3 * 0.12F), Radius.all(1.5F));
        new RenderUtil(drawContext, f4, f6, Math.max(f5 * f7, 3.0F), 3.0F).rect(Theme.withAlpha(Theme.getPrimary(), f3), Radius.all(1.5F));
        this.renderResetButton(drawContext, f, f2, f3, i2, f4, f5, this.resetButtonY, Language.translate("handoffset.editor.reset", "Сбросить руку"));
    }

    private void renderResetButton(DrawContext drawContext, float f, float f2, float f3, int i, float f4, float f5, float f6, String string) {
        boolean flag = isInside(f, f2, f4, f6, f5, 10.0F);
        Animation animation = this.rowAnimations.get(i);
        animation.update();
        animation.animate(flag ? 1.0 : 0.0, 0.16, Easings.QUAD_OUT);
        GuiRenderUtil.drawHighlight(drawContext, f4, f6 + 1.0F, f5, 8.0F, f3 * animation.get(), Radius.all(4.0F));
        RenderUtil renderUtil = new RenderUtil(drawContext, f4, f6, f5, 10.0F);
        MsdfFont msdfFont = Fonts.INTER_MEDIUM;
        int i2 = Theme.getTextColor();
        if (flag) {
            renderUtil.textCenter(msdfFont, string, ColorUtil.setAlphaf(i2, 0.9F * f3), 5.5F);
        } else {
            renderUtil.textCenter(msdfFont, string, ColorUtil.setAlphaf(i2, 0.65F * f3), 5.5F);
        }
    }

    private void renderSwingOption(
        DrawContext drawContext, float f, float f2, float f3, int i, float f4, float f5, float f6, String string, boolean flag
    ) {
        boolean flag2 = isInside(f, f2, f4, f6, f5, 10.0F);
        Animation animation = this.rowAnimations.get(i);
        animation.update();
        animation.animate(flag2 ? 1.0 : 0.0, 0.16, Easings.QUAD_OUT);
        GuiRenderUtil.drawHighlight(drawContext, f4, f6 + 1.0F, f5, 8.0F, f3 * animation.get(), Radius.all(4.0F));
        RenderUtil renderUtil = new RenderUtil(drawContext, f4, f6, f5, 10.0F);
        if (flag) {
            renderUtil.icon(Icon.CHECK, 4.0F, 1.0F, 8.0F, Theme.withAlpha(Theme.getTextColor(), f3));
        }

        int i2 = ColorUtil.setAlphaf(Theme.getTextColor(), (flag ? 1.0F : 0.5F) * f3);
        renderUtil.scrollingTextMiddle(Fonts.INTER_MEDIUM, string, 14.0F, 0.0F, f5 - 14.0F, i2, 5.5F, flag2);
    }

    private void openMenu(Hand hand, float f, float f2) {
        this.swingOptions.clear();

        for (String string : this.handOffset.getSwingModes()) {
            this.swingOptions.add(Language.translateOption("handoffset.swing", string));
        }

        String string2 = Language.translate(
            hand == Hand.MAIN_HAND ? "handoffset.editor.main" : "handoffset.editor.off", hand == Hand.MAIN_HAND ? "Основная рука" : "Вторая рука"
        );
        String string3 = Language.translate("handoffset.editor.reset", "Сбросить руку");
        float f3 = Math.max(Fonts.INTER_SEMIBOLD.getWidth(string2, 7.0F) + 24.0F - 14.0F, Fonts.INTER_MEDIUM.getWidth(string3, 5.5F));
        int i = this.swingOptions.size();

        for (int i2 = 0; i2 < i; i2++) {
            f3 = Math.max(f3, Fonts.INTER_MEDIUM.getWidth(this.swingOptions.get(i2), 5.5F));
        }

        while (this.rowAnimations.size() < i + 1) {
            this.rowAnimations.add(new Animation());
        }

        int i3 = 0;

        for (int i4 = this.rowAnimations.size(); i3 < i4; i3++) {
            this.rowAnimations.get(i3).forceValue(0.0);
        }

        this.menuWidth = 30.0F + f3 + 6.0F;
        float f4 = i * 11.5F;
        this.menuHeight = 35.5F + f4 + 19.0F + 14.0F + 8.0F;
        this.menuX = Math.max(4.0F, Math.min(f, GuiScale.getScaledWidth() - this.menuWidth - 4.0F));
        this.menuY = Math.max(4.0F, Math.min(f2, GuiScale.getScaledHeight() - this.menuHeight - 4.0F));
        this.swingListY = this.menuY + 35.5F;
        this.sizeSliderY = this.swingListY + f4 + 4.0F;
        this.resetButtonY = this.sizeSliderY + 10.0F + 2.0F + 3.0F + 4.0F;
        this.menuHand = hand;
        this.shownMenuHand = hand;
    }

    private float getSliderY() {
        return this.sizeSliderY + 10.0F + 2.0F;
    }

    private void closeMenu() {
        this.menuHand = null;
        this.draggingSize = false;
    }

    private void handleMenuClick(double d, double d2, int i) {
        if (i == 0) {
            double d3 = this.unscale(d, this.menuX + this.menuWidth / 2.0F);
            double d4 = this.unscale(d2, this.menuY + this.menuHeight / 2.0F);
            if (isInside(d3, d4, this.menuX + this.menuWidth - 24.0F, this.menuY, 24.0F, 24.0F)) {
                this.closeMenu();
            } else {
                float f = this.menuX + 8.0F;
                float f2 = this.menuWidth - 16.0F;
                int i2 = this.swingOptions.size();

                for (int i3 = 0; i3 < i2; i3++) {
                    if (isInside(d3, d4, f, this.swingListY + i3 * 11.5F, f2, 10.0F)) {
                        this.selectSwingMode(i3);
                        return;
                    }
                }

                if (isInside(d3, d4, f, this.sizeSliderY, f2, this.getSliderY() + 3.0F - this.sizeSliderY)) {
                    this.draggingSize = true;
                    this.updateSizeFromMouse(d);
                } else if (isInside(d3, d4, f, this.resetButtonY, f2, 10.0F)) {
                    this.handOffset.getXSetting(this.menuHand).setValue(Float.valueOf(0.0F));
                    this.handOffset.getYSetting(this.menuHand).setValue(Float.valueOf(0.0F));
                    this.handOffset.getZSetting(this.menuHand).setValue(Float.valueOf(0.0F));
                    this.handOffset.getSizeSetting(this.menuHand).setValue(Float.valueOf(1.0F));
                    this.handOffset.saveConfig();
                }
            }
        }
    }

    private double unscale(double d, float f) {
        float f2 = this.menuScale <= 0.0F ? 1.0F : this.menuScale;
        return (d - f) / f2 + f;
    }

    private void updateSizeFromMouse(double d) {
        NumberSetting numberSetting = this.handOffset.getSizeSetting(this.menuHand);
        float f = this.menuX + 8.0F;
        float f2 = this.menuWidth - 16.0F;
        double d2 = this.unscale(d, this.menuX + this.menuWidth / 2.0F);
        float f3 = Math.max(0.0F, Math.min(1.0F, (float)(d2 - f) / f2));
        numberSetting.setValue(Float.valueOf(numberSetting.getMin() + f3 * (numberSetting.getMax() - numberSetting.getMin())));
        this.handOffset.saveConfig();
    }

    private int getSelectedSwingIndex() {
        return Math.max(0, this.handOffset.getSwingModes().indexOf(this.handOffset.getSwingMode()));
    }

    private void selectSwingMode(int i) {
        this.handOffset.setSwingMode(this.handOffset.getSwingModes().get(i));
    }

    private boolean isInsideMenu(double d, double d2) {
        return isInside(
            this.unscale(d, this.menuX + this.menuWidth / 2.0F),
            this.unscale(d2, this.menuY + this.menuHeight / 2.0F),
            this.menuX,
            this.menuY,
            this.menuWidth,
            this.menuHeight
        );
    }

    private static boolean isInside(double d, double d2, float f, float f2, float f3, float f4) {
        return d >= f && d <= f + f3 && d2 >= f2 && d2 <= f2 + f4;
    }

    private boolean isOverChat(double d, double d2) {
        if (d2 >= GuiScale.getScaledHeight() - GuiScale.toScaled(16.0)) {
            return true;
        }

        ChatHud chatHud = mc.inGameHud == null ? null : mc.inGameHud.getChatHud();
        if (chatHud == null) {
            return false;
        }

        double d3 = mc.options.getChatScale().getValue();
        if (d3 <= 0.0) {
            return false;
        }

        List list = ((ChatHudAccessor)chatHud).sadnes$getVisibleMessages();
        int i = Math.min(list.size(), chatHud.getVisibleLineCount());
        if (i <= 0) {
            return false;
        }

        double d4 = 9.0 * (mc.options.getChatLineSpacing().getValue() + 1.0);
        float f = GuiScale.toScaled(4.0 + ChatHud.getWidth(mc.options.getChatWidth().getValue()) * d3);
        float f2 = GuiScale.getScaledHeight() - GuiScale.toScaled(40.0);
        float f3 = f2 - GuiScale.toScaled(i * d4 * d3);
        return d <= f && d2 >= f3 && d2 <= f2;
    }

    private Hand pickHand(double d, double d2) {
        if (this.isOverChat(d, d2)) {
            return null;
        }

        DragComponent dragComponent = DragComponent.getInstance();
        if (dragComponent != null && dragComponent.getDragHandler().isHovered(d, d2)) {
            return null;
        }

        float f = this.getProjectionScale();
        float f2 = this.getAspectRatio();
        if (!(f <= 0.0F) && !(f2 <= 0.0F)) {
            float f3 = (float)(d / GuiScale.getScaledWidth()) * 2.0F - 1.0F;
            float f4 = 1.0F - (float)(d2 / GuiScale.getScaledHeight()) * 2.0F;
            this.rayDirection.set(f3 * f2 / f, f4 / f, -1.0F);
            Hand hand = null;
            float f5 = Float.MAX_VALUE;

            for (Hand hand2 : Hand.values()) {
                if (this.handOffset.wasHandRenderedRecently(hand2)) {
                    float f6 = this.handOffset.raycastHand(hand2, this.rayDirection);
                    if (f6 < 0.0F) {
                        f6 = this.fallbackHandDistance(hand2, d, d2, f, f2);
                    }

                    if (!(f6 < 0.0F) && !(f6 >= f5)) {
                        f5 = f6;
                        hand = hand2;
                    }
                }
            }

            return hand;
        } else {
            return null;
        }
    }

    private float fallbackHandDistance(Hand hand, double d, double d2, float f, float f2) {
        float f3 = -(this.handOffset.getSmoothedZ(hand) + -0.72F);
        if (f3 < 0.05F) {
            return -1.0F;
        }

        int i = this.getHandSide(hand);
        float f4 = this.handOffset.getSmoothedX(hand) + i * 0.56F;
        float f5 = this.handOffset.getSmoothedY(hand) + -0.52F;
        float f6 = f / (f2 * f3) * 0.5F * GuiScale.getScaledWidth();
        float f7 = f / f3 * 0.5F * GuiScale.getScaledHeight();
        float f8 = GuiScale.getScaledWidth() * 0.5F + (f4 + i * 0.21F) * f6;
        float f9 = GuiScale.getScaledHeight() * 0.5F - (f5 + 0.02F) * f7;
        float f10 = GuiScale.getScaledWidth() * 0.5F + (f4 - i * 0.26F) * f6;
        float f11 = GuiScale.getScaledHeight() * 0.5F - (f5 + 0.45F) * f7;
        float f12 = f10 - f8;
        float f13 = f11 - f9;
        float f14 = f12 * f12 + f13 * f13;
        if (f14 < 1.0F) {
            return -1.0F;
        }

        float f15 = Math.max(0.0F, Math.min(1.0F, (float)(((d - f8) * f12 + (d2 - f9) * f13) / f14)));
        double d3 = f8 + f12 * f15 - d;
        double d4 = f9 + f13 * f15 - d2;
        float f16 = Math.max((float)Math.sqrt(f14) * 0.16F, 8.0F);
        return d3 * d3 + d4 * d4 <= f16 * f16 ? f3 : -1.0F;
    }

    private Vector2f screenToHandOffset(Hand hand, double d, double d2) {
        float f = -(this.handOffset.getZSetting(hand).getValue() + -0.72F);
        if (f < 0.05F) {
            return null;
        } else {
            float f2 = this.getProjectionScale();
            float f3 = this.getAspectRatio();
            if (!(f2 <= 0.0F) && !(f3 <= 0.0F)) {
                float f4 = (float)(d / GuiScale.getScaledWidth()) * 2.0F - 1.0F;
                float f5 = 1.0F - (float)(d2 / GuiScale.getScaledHeight()) * 2.0F;
                return new Vector2f(f4 * f3 * f / f2 - this.getHandSide(hand) * 0.56F, f5 * f / f2 - -0.52F);
            } else {
                return null;
            }
        }
    }

    private int getHandSide(Hand hand) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        if (clientPlayerEntity == null) {
            return hand == Hand.MAIN_HAND ? 1 : -1;
        }

        Arm arm = hand == Hand.MAIN_HAND ? clientPlayerEntity.getMainArm() : clientPlayerEntity.getMainArm().getOpposite();
        return arm == Arm.RIGHT ? 1 : -1;
    }

    private float getProjectionScale() {
        if (mc.gameRenderer == null) {
            return 0.0F;
        }

        float f = mc.getRenderTickCounter().getTickProgress(false);
        float f2 = ((GameRendererAccessor)mc.gameRenderer).sadnes$getFov(mc.gameRenderer.getCamera(), f, false);
        return !(f2 <= 1.0F) && !(f2 >= 179.0F) ? 1.0F / (float)Math.tan(Math.toRadians(f2) * 0.5) : 0.0F;
    }

    private float getAspectRatio() {
        int i = mc.getWindow().getFramebufferHeight();
        return i == 0 ? 0.0F : (float)mc.getWindow().getFramebufferWidth() / i;
    }

    private void reset() {
        this.draggedHand = null;
        this.hoveredHand = null;
        this.draggingSize = false;
        this.menuHand = null;
        this.shownMenuHand = null;
        this.queuedMenuHand = null;
    }
}
