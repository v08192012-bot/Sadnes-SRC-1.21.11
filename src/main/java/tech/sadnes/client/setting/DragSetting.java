package tech.sadnes.client.setting;

import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.render.font.Fonts;

public final class DragSetting extends Setting<Vector2f> {
    public final Vector2f position2 = new Vector2f();
    private final Vector2f targetPosition = new Vector2f();
    public final Vector2f size2 = new Vector2f(1.0F, 1.0F);
    private Supplier<Boolean> draggableSupplier = () -> true;
    private Supplier<Boolean> interactiveSupplier = () -> false;
    private Supplier<Boolean> renderCondition = this::isDraggable;
    private Supplier<Float> defaultXSupplier = () -> 0.0F;
    private Supplier<Float> defaultYSupplier = () -> 0.0F;
    private boolean positionInitialized;
    private SettingHolder owner;
    private boolean onTop;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private float visibility = 1.0F;
    private DragSetting.Renderer renderer = (drawContext, i, i2, f) -> {};
    private Supplier<Runnable> postRenderSupplier = () -> () -> {};
    private final Animation visibilityAnimation = new Animation();
    private final Animation dragAnimation = new Animation();
    private final Animation anchorAnimation = new Animation();
    private final Animation hoverAnimation2 = new Animation();
    private float layoutOffsetX;
    private float layoutOffsetY;
    private long shakeStartTime;
    private static final long SHAKE_DURATION_MS = 240L;
    private DragSetting anchorTarget;
    private int anchorTargetX = -1;
    private int anchorSelfX = -1;
    private float anchorOffsetX;
    private int anchorTargetY = -1;
    private int anchorSelfY = -1;
    private float anchorOffsetY;
    private int screenGridX = -1;
    private int screenSelfAnchorX = -1;
    private int screenGridY = -1;
    private int screenSelfAnchorY = -1;
    private DrawContext lastContext;
    private int lastMouseX;
    private int lastMouseY;
    private float lastDelta;

    public DragSetting(String string) {
        this(string, 0.0F, 0.0F);
    }

    public DragSetting(String string, float f, float f2) {
        super(string, new Vector2f(f, f2));
        this.setPosition(f, f2);
        this.positionInitialized = true;
    }

    public DragSetting(String string, Supplier<Float> supplier, Supplier<Float> supplier2) {
        super(string, new Vector2f());
        this.defaultXSupplier = supplier != null ? supplier : () -> 0.0F;
        this.defaultYSupplier = supplier2 != null ? supplier2 : () -> 0.0F;
        this.resetToDefault();
    }

    public DragSetting setValue(Vector2f vector2f) {
        if (vector2f == null) {
            return this;
        }

        this.setPosition(vector2f.x, vector2f.y);
        return this;
    }

    public DragSetting setVisibleWhen(Supplier<Boolean> supplier) {
        super.setVisibleWhen(supplier);
        return this;
    }

    public DragSetting setOnChange(Runnable runnable) {
        super.setOnChange(runnable);
        return this;
    }

    public DragSetting setOnVisibilityChange(Runnable runnable) {
        super.setOnVisibilityChange(runnable);
        return this;
    }

    public Vector2f getPosition() {
        return new Vector2f(this.position2);
    }

    public Vector2f getTargetPosition() {
        return new Vector2f(this.targetPosition);
    }

    public Vector2f getSize() {
        return new Vector2f(this.size2);
    }

    public DragSetting setSize(float f, float f2) {
        this.size2.set(Math.max(f, 1.0F), Math.max(f2, 1.0F));
        return this;
    }

    public DragSetting setSize(Vector2f vector2f) {
        if (vector2f != null) {
            this.setSize(vector2f.x, vector2f.y);
        }

        return this;
    }

    public boolean isDraggable() {
        return this.draggableSupplier.get();
    }

    public DragSetting setDraggable(Supplier<Boolean> supplier) {
        if (supplier != null) {
            this.draggableSupplier = supplier;
            return this;
        } else {
            this.draggableSupplier = () -> true;
            return this;
        }
    }

    public DragSetting setInteractive(Supplier<Boolean> supplier) {
        if (supplier != null) {
            this.interactiveSupplier = supplier;
            return this;
        } else {
            this.interactiveSupplier = () -> false;
            return this;
        }
    }

    public boolean isRendered() {
        return this.isVisible() && this.visibility > 0.001F && this.renderCondition.get();
    }

    public DragSetting setRenderCondition(Supplier<Boolean> supplier) {
        if (supplier != null) {
            this.renderCondition = supplier;
            return this;
        } else {
            this.renderCondition = this::isDraggable;
            return this;
        }
    }

    public boolean isInteractive() {
        try {
            return Boolean.TRUE.equals(this.interactiveSupplier.get());
        } catch (Throwable throwable) {
            return false;
        }
    }

    public SettingHolder getOwner() {
        return this.owner;
    }

    public DragSetting onInput(SettingHolder settingHolder) {
        this.owner = settingHolder;
        return this;
    }

    public boolean isOnTop() {
        return this.onTop;
    }

    public DragSetting setOnTop(boolean flag) {
        this.onTop = flag;
        return this;
    }

    public boolean isPositionInitialized() {
        return this.positionInitialized;
    }

    public DragSetting setPositionInitialized(boolean flag) {
        this.positionInitialized = flag;
        return this;
    }

    public DragSetting setDefaultX(Supplier<Float> supplier) {
        if (supplier != null) {
            this.defaultXSupplier = supplier;
            return this;
        } else {
            this.defaultXSupplier = () -> 0.0F;
            return this;
        }
    }

    public DragSetting setDefaultY(Supplier<Float> supplier) {
        if (supplier != null) {
            this.defaultYSupplier = supplier;
            return this;
        } else {
            this.defaultYSupplier = () -> 0.0F;
            return this;
        }
    }

    public DragSetting resetToDefault() {
        this.setPosition(this.defaultXSupplier.get(), this.defaultYSupplier.get());
        this.positionInitialized = true;
        return this;
    }

    public DragSetting setPosition(float f, float f2) {
        this.position2.set(f, f2);
        this.targetPosition.set(f, f2);
        this.setValueInternal(new Vector2f(f, f2));
        return this;
    }

    public DragSetting setTargetPosition(float f, float f2) {
        this.targetPosition.set(f, f2);
        return this;
    }

    public DragSetting setRenderPosition(float f, float f2) {
        this.position2.set(f, f2);
        return this;
    }

    public DragSetting commitTargetPosition() {
        this.setValueInternal(new Vector2f(this.targetPosition.x, this.targetPosition.y));
        return this;
    }

    public boolean isSettled(float f) {
        return Math.abs(this.position2.x - this.targetPosition.x) <= f && Math.abs(this.position2.y - this.targetPosition.y) <= f;
    }

    public DragSetting moveBy(float f, float f2) {
        return this.setPosition(this.position2.x + f, this.position2.y + f2);
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public float getVisibility() {
        return this.visibility;
    }

    public Animation getVisibilityAnimation() {
        return this.visibilityAnimation;
    }

    public Animation getDragAnimation() {
        return this.dragAnimation;
    }

    public Animation getAnchorAnimation() {
        return this.anchorAnimation;
    }

    public Animation getHoverAnimation() {
        return this.hoverAnimation2;
    }

    public float getLayoutOffsetX() {
        return this.layoutOffsetX;
    }

    public float getLayoutOffsetY() {
        return this.layoutOffsetY;
    }

    public void setLayoutOffset(float f, float f2) {
        this.layoutOffsetX = f;
        this.layoutOffsetY = f2;
    }

    public void startShake() {
        this.shakeStartTime = System.currentTimeMillis();
    }

    public float getShakeOffset() {
        if (this.shakeStartTime == 0L) {
            return 0.0F;
        } else {
            long l = System.currentTimeMillis() - this.shakeStartTime;
            if (l >= 240L) {
                this.shakeStartTime = 0L;
                return 0.0F;
            } else {
                float f = (float)l / 1000.0F;
                float f2 = 1.0F - (float)l / 240.0F;
                return (float)Math.sin(f * 70.0) * 2.5F * f2;
            }
        }
    }

    public DragSetting getAnchorTarget() {
        return this.anchorTarget;
    }

    public boolean isAnchored() {
        return this.anchorTarget != null;
    }

    public int getAnchorTargetX() {
        return this.anchorTargetX;
    }

    public int getAnchorSelfX() {
        return this.anchorSelfX;
    }

    public float getAnchorOffsetX() {
        return this.anchorOffsetX;
    }

    public int getAnchorTargetY() {
        return this.anchorTargetY;
    }

    public int getAnchorSelfY() {
        return this.anchorSelfY;
    }

    public float getAnchorOffsetY() {
        return this.anchorOffsetY;
    }

    public void clearAnchor() {
        this.anchorTarget = null;
        this.anchorTargetX = -1;
        this.anchorSelfX = -1;
        this.anchorTargetY = -1;
        this.anchorSelfY = -1;
        this.anchorOffsetX = 0.0F;
        this.anchorOffsetY = 0.0F;
    }

    public void setAnchor(DragSetting dragSetting, int i, int i2, float f, int i3, int i4, float f2) {
        this.anchorTarget = dragSetting;
        this.anchorTargetX = i;
        this.anchorSelfX = i2;
        this.anchorOffsetX = f;
        this.anchorTargetY = i3;
        this.anchorSelfY = i4;
        this.anchorOffsetY = f2;
    }

    public int getScreenGridX() {
        return this.screenGridX;
    }

    public int getScreenSelfAnchorX() {
        return this.screenSelfAnchorX;
    }

    public int getScreenGridY() {
        return this.screenGridY;
    }

    public int getScreenSelfAnchorY() {
        return this.screenSelfAnchorY;
    }

    public boolean isScreenAnchored() {
        return this.screenGridX >= 0 || this.screenGridY >= 0;
    }

    public void setScreenAnchor(int i, int i2, int i3, int i4) {
        this.screenGridX = i;
        this.screenSelfAnchorX = i2;
        this.screenGridY = i3;
        this.screenSelfAnchorY = i4;
    }

    public void clearScreenAnchor() {
        this.screenGridX = -1;
        this.screenSelfAnchorX = -1;
        this.screenGridY = -1;
        this.screenSelfAnchorY = -1;
    }

    public DrawContext getLastContext() {
        return this.lastContext;
    }

    public int getLastMouseX() {
        return this.lastMouseX;
    }

    public int getLastMouseY() {
        return this.lastMouseY;
    }

    public float getLastDelta() {
        return this.lastDelta;
    }

    public DragSetting setVisibility(float f) {
        this.visibility = Math.max(0.0F, Math.min(1.0F, f));
        return this;
    }

    public DragSetting onInput(DragSetting.Renderer renderer2) {
        if (renderer2 != null) {
            this.renderer = renderer2;
            return this;
        } else {
            this.renderer = (drawContext, i, i2, f) -> {};
            return this;
        }
    }

    public DragSetting setPostRender(Supplier<Runnable> supplier) {
        if (supplier != null) {
            this.postRenderSupplier = supplier;
            return this;
        } else {
            this.postRenderSupplier = () -> () -> {};
            return this;
        }
    }

    public void renderElement(DrawContext drawContext, int i, int i2, float f) {
        if (!(this.visibility <= 0.001F)) {
            this.lastContext = drawContext;
            this.lastMouseX = i;
            this.lastMouseY = i2;
            this.lastDelta = f;
            float f2 = this.getShakeOffset();
            Matrix3x2fStack matrix3x2fStack = drawContext.getMatrices();
            boolean flag = Math.abs(f2) > 1.0E-4F;
            if (flag) {
                matrix3x2fStack.pushMatrix();
                matrix3x2fStack.translate(f2, 0.0F);
            }

            this.renderer.render(drawContext, i, i2, f);
            Runnable runnable = this.postRenderSupplier.get();
            if (runnable != null) {
                runnable.run();
            }

            if (flag) {
                matrix3x2fStack.popMatrix();
            }
        }
    }

    public boolean startDrag(double d, double d2) {
        if (!this.isDraggable()) {
            return false;
        }

        this.dragging = true;
        this.dragOffsetX = (float)(d - this.position2.x);
        this.dragOffsetY = (float)(d2 - this.position2.y);
        return true;
    }

    public DragSetting dragTo(double d, double d2) {
        if (this.dragging) {
            this.setPosition((float)d - this.dragOffsetX, (float)d2 - this.dragOffsetY);
        }

        return this;
    }

    public DragSetting stopDrag() {
        this.dragging = false;
        return this;
    }

    @Override
    public void render(DrawContext drawContext, int i, int i2, float f) {
        String string = String.format("%.0f %.0f", this.position2.x, this.position2.y);
        float f2 = Math.max(28.0F, Fonts.INTER_MEDIUM.getWidth(string, 5.5F) + 8.0F);
        float f3 = super.position.x + super.size.x - f2;
        float f4 = super.position.y + super.size.y / 2.0F - 10.0F / 2.0F;
        this.drawLabel(
            drawContext,
            super.position.x,
            super.position.y,
            super.size.x,
            super.size.y,
            super.size.x - f2 - 6.0F,
            this.isHovered(i, i2, super.position, super.size)
        );
        SettingWidgets.drawLabelBox(drawContext, f3, f4, f2, 10.0F, string, this.getAlpha());
    }

    @FunctionalInterface
    public interface Renderer {
        void render(DrawContext drawContext, int i, int i2, float f);
    }
}
