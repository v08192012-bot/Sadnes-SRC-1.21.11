package tech.sadnes.client.feature.impl.render.hand;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.config.ConfigManager;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseScrollEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;

@FeatureInfo(name = "Hand Offset", displayName = "feature.impl.render.hand.handoffset", category = Category.VISUALS, subcategory = Subcategory.HAND)
public class HandOffset extends Feature {
    private final NumberSetting mainX = (NumberSetting)new NumberSetting("handoffset.mainX", 0.0F, -1.0F, 1.0F, 0.002F).setDisplayName("Main hand X").setHidden(true);
    private final NumberSetting mainY = (NumberSetting)new NumberSetting("handoffset.mainY", 0.0F, -1.0F, 1.0F, 0.002F).setDisplayName("Main hand Y").setHidden(true);
    private final NumberSetting mainZ = (NumberSetting)new NumberSetting("handoffset.mainZ", 0.0F, -2.5F, 2.5F, 0.005F).setDisplayName("Main hand Z").setHidden(true);
    private final NumberSetting mainSize = (NumberSetting)new NumberSetting("handoffset.mainSize", 1.0F, 0.3F, 1.5F, 0.05F)
        .setDisplayName("Main hand size")
        .setHidden(true);
    private final NumberSetting offX = (NumberSetting)new NumberSetting("handoffset.offX", 0.0F, -1.0F, 1.0F, 0.002F).setDisplayName("Off hand X").setHidden(true);
    private final NumberSetting offY = (NumberSetting)new NumberSetting("handoffset.offY", 0.0F, -1.0F, 1.0F, 0.002F).setDisplayName("Off hand Y").setHidden(true);
    private final NumberSetting offZ = (NumberSetting)new NumberSetting("handoffset.offZ", 0.0F, -2.5F, 2.5F, 0.005F).setDisplayName("Off hand Z").setHidden(true);
    private final NumberSetting offSize = (NumberSetting)new NumberSetting("handoffset.offSize", 1.0F, 0.3F, 1.5F, 0.05F)
        .setDisplayName("Off hand size")
        .setHidden(true);
    private final BooleanSetting tutorialSeen = (BooleanSetting)new BooleanSetting("handoffset.tutorialSeen", false).setDisplayName("Tutorial seen").setHidden(true);
    private final ModeSetting swing = new ModeSetting("handoffset.swing", SwingAnimations.MODES);
    private final NumberSetting swingSpeed = new NumberSetting("handoffset.swingSpeed", 1.0F, 0.5F, 2.0F, 0.05F).setVisibleWhen(() -> this.swing.isNot("Off"));
    private final NumberSetting swingStrength = new NumberSetting("handoffset.swingStrength", 1.0F, 0.1F, 1.5F, 0.05F).setVisibleWhen(() -> this.swing.isNot("Off"));
    private Hand currentHand = Hand.MAIN_HAND;
    private ItemStack currentStack = ItemStack.EMPTY;
    private final HandOffsetEditor editor = new HandOffsetEditor(this);
    private final HandOffset.SmoothedOffset mainOffset = new HandOffset.SmoothedOffset();
    private final HandOffset.SmoothedOffset offOffset = new HandOffset.SmoothedOffset();
    private final HandOffset.HandHitbox mainHitbox = new HandOffset.HandHitbox();
    private final HandOffset.HandHitbox offHitbox = new HandOffset.HandHitbox();
    private final Matrix4f cameraRotation = new Matrix4f();
    private final Quaternionf tempQuaternion = new Quaternionf();
    private static volatile Hand capturingHand;
    private final long[] lastRenderTimes = new long[Hand.values().length];
    private final Matrix4f capturedMatrix = new Matrix4f();
    private boolean matrixCaptured;

    public static HandOffset getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(HandOffset.class);
    }

    public void applyOffset(MatrixStack matrixStack, Hand hand, ItemStack itemStack) {
        this.currentHand = hand;
        this.currentStack = itemStack;
        if (this.isEnabled()) {
            if (hand != Hand.MAIN_HAND || !(itemStack.getItem() instanceof CrossbowItem)) {
                HandOffset.SmoothedOffset smoothedOffset = this.getOffset(hand);
                smoothedOffset.update(
                    this.getXSetting(hand).getValue(),
                    this.getYSetting(hand).getValue(),
                    this.getZSetting(hand).getValue(),
                    this.getSizeSetting(hand).getValue(),
                    0.08F
                );
                matrixStack.translate(smoothedOffset.x, smoothedOffset.y, smoothedOffset.z);
            }
        }
    }

    public void applyScale(MatrixStack matrixStack, Hand hand, ItemStack itemStack) {
        if (this.isEnabled()) {
            if (hand != Hand.MAIN_HAND || !(itemStack.getItem() instanceof CrossbowItem)) {
                float f = this.getOffset(hand).scale;
                matrixStack.scale(f, f, f);
            }
        }
    }

    public boolean isSwingActive() {
        boolean flag;
        if (this.isEnabled()) {
            flag = this.swing.isNot("Off");
        } else {
            flag = false;
        }

        return flag && this.currentHand == Hand.MAIN_HAND && SwingAnimations.isSwingItem(this.currentStack);
    }

    public boolean applySwingAnimation(MatrixStack matrixStack, Arm arm, float f) {
        if (!this.isSwingActive()) {
            return false;
        }

        SwingAnimations.apply(matrixStack, this.swing.getValue(), arm == Arm.RIGHT ? 1 : -1, f, this.swingStrength.getValue(), this.swingSpeed.getValue());
        return true;
    }

    public int adjustSwingDuration(ItemStack itemStack, int i) {
        boolean flag;
        if (this.isEnabled()) {
            flag = this.swing.isNot("Off");
        } else {
            flag = false;
        }

        return flag && SwingAnimations.isSwingItem(itemStack) ? Math.max(1, Math.round(i / this.swingSpeed.getValue())) : i;
    }

    List<String> getSwingModes() {
        return this.swing.getModes();
    }

    String getSwingMode() {
        return this.swing.getValue();
    }

    void setSwingMode(String string) {
        this.swing.setValue(string);
        this.saveConfig();
    }

    public boolean isEditorActive() {
        return this.isEnabled() && this.editor.isActive();
    }

    public void renderEditor(DrawContext drawContext, float f, float f2) {
        this.editor.render(drawContext, f, f2);
    }

    public float getEditorProgress() {
        return this.isEditorActive() ? this.editor.getHoverProgress() : 0.0F;
    }

    public void renderEditorHand(float f, MatrixStack matrixStack, HeldItemRenderer heldItemRenderer, ClientPlayerEntity clientPlayerEntity, int i) {
        if (this.isEditorActive()) {
            this.editor.renderHandSweep(f, matrixStack, heldItemRenderer, clientPlayerEntity, i);
        }
    }

    public void markHandRendered(Hand hand, ItemStack itemStack, ClientPlayerEntity clientPlayerEntity) {
        if (!itemStack.isEmpty() || hand != Hand.OFF_HAND && !clientPlayerEntity.isInvisible()) {
            this.lastRenderTimes[hand.ordinal()] = System.nanoTime();
        }
    }

    boolean wasHandRenderedRecently(Hand hand) {
        long l = this.lastRenderTimes[hand.ordinal()];
        return l != 0L && System.nanoTime() - l < 200000000L;
    }

    public void beginItemCapture(ItemDisplayContext itemDisplayContext, ItemStack itemStack) {
        capturingHand = null;
        ClientPlayerEntity clientPlayerEntity = mc.player;
        if (clientPlayerEntity != null && !itemStack.isEmpty() && this.isEditorActive()) {
            Arm arm = itemDisplayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? Arm.RIGHT : (itemDisplayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? Arm.LEFT : null);
            if (arm != null) {
                capturingHand = arm == clientPlayerEntity.getMainArm() ? Hand.MAIN_HAND : Hand.OFF_HAND;
                this.cameraRotation.rotation(mc.gameRenderer.getCamera().getRotation().conjugate(this.tempQuaternion));
                this.matrixCaptured = false;
                this.getHitbox(capturingHand).reset();
            }
        }
    }

    public static boolean isCapturing() {
        return capturingHand != null;
    }

    public void captureQuads(MatrixStack matrixStack, List<BakedQuad> bakedQuads) {
        Hand hand = capturingHand;
        if (hand != null && bakedQuads != null && !bakedQuads.isEmpty()) {
            this.getHitbox(hand).addQuadsBox(this.cameraRotation, matrixStack.peek().getPositionMatrix(), bakedQuads);
        }
    }

    public void captureMatrix(MatrixStack matrixStack) {
        if (capturingHand != null && !this.matrixCaptured) {
            this.capturedMatrix.set(matrixStack.peek().getPositionMatrix());
            this.matrixCaptured = true;
        }
    }

    public void endItemCapture() {
        Hand hand = capturingHand;
        capturingHand = null;
        if (hand != null) {
            HandOffset.HandHitbox handHitbox = this.getHitbox(hand);
            if (handHitbox.isEmpty() && this.matrixCaptured) {
                handHitbox.addDefaultBox(this.cameraRotation, this.capturedMatrix);
            }

            this.matrixCaptured = false;
            handHitbox.finish();
        }
    }

    float raycastHand(Hand hand, Vector3fc vector3fc) {
        return this.getHitbox(hand).raycast(vector3fc);
    }

    int getHandCorners(Hand hand, Vector3f[] vector3fs) {
        return this.getHitbox(hand).getCorners(vector3fs);
    }

    private HandOffset.HandHitbox getHitbox(Hand hand) {
        return hand == Hand.MAIN_HAND ? this.mainHitbox : this.offHitbox;
    }

    @EventHandler
    public void onMouseMove(Render2DEvent render2DEvent) {
        this.editor.onMouseMove(render2DEvent.getMouseX(), render2DEvent.getMouseY());
    }

    @EventHandler
    public void onMousePress(ScreenKeyEvent screenKeyEvent) {
        this.editor.onKey(screenKeyEvent);
    }

    @EventHandler
    public void onMouseRelease(ScreenMouseScrollEvent screenMouseScrollEvent) {
        this.editor.onScroll(screenMouseScrollEvent);
    }

    NumberSetting getXSetting(Hand hand) {
        return hand == Hand.MAIN_HAND ? this.mainX : this.offX;
    }

    NumberSetting getYSetting(Hand hand) {
        return hand == Hand.MAIN_HAND ? this.mainY : this.offY;
    }

    NumberSetting getZSetting(Hand hand) {
        return hand == Hand.MAIN_HAND ? this.mainZ : this.offZ;
    }

    NumberSetting getSizeSetting(Hand hand) {
        return hand == Hand.MAIN_HAND ? this.mainSize : this.offSize;
    }

    float getSmoothedX(Hand hand) {
        return this.getOffset(hand).x;
    }

    float getSmoothedY(Hand hand) {
        return this.getOffset(hand).y;
    }

    float getSmoothedZ(Hand hand) {
        return this.getOffset(hand).z;
    }

    boolean isTutorialSeen() {
        return this.tutorialSeen.getValue();
    }

    void markTutorialSeen() {
        this.tutorialSeen.setValue(true);
        this.saveConfig();
    }

    void saveConfig() {
        ConfigManager configManager = Sadnes.getInstance().getConfigManager();
        if (configManager != null) {
            configManager.markDirty();
        }
    }

    private HandOffset.SmoothedOffset getOffset(Hand hand) {
        return hand == Hand.MAIN_HAND ? this.mainOffset : this.offOffset;
    }

    static final class HandHitbox {
        private static final int MAX_BOXES = 2;
        private final Matrix4f[] transforms = new Matrix4f[2];
        private final Matrix4f[] inverseTransforms = new Matrix4f[2];
        private final Vector3f[] mins = new Vector3f[2];
        private final Vector3f[] maxs = new Vector3f[2];
        private final Vector3f rayOrigin = new Vector3f();
        private final Vector3f rayDirection = new Vector3f();
        private int boxCount;
        private long capturedAt;

        HandHitbox() {
            for (int i = 0; i < 2; i++) {
                this.transforms[i] = new Matrix4f();
                this.inverseTransforms[i] = new Matrix4f();
                this.mins[i] = new Vector3f();
                this.maxs[i] = new Vector3f();
            }
        }

        void reset() {
            this.boxCount = 0;
        }

        boolean isEmpty() {
            return this.boxCount == 0;
        }

        void addDefaultBox(Matrix4f matrix4f, Matrix4f matrix4f2) {
            this.addBox(matrix4f, matrix4f2, -0.5F, -0.5F, -0.5F, 0.5F, 0.5F, 0.5F);
        }

        void addQuadsBox(Matrix4f matrix4f, Matrix4f matrix4f2, List<BakedQuad> bakedQuads) {
            if (this.boxCount < 2) {
                float f = Float.MAX_VALUE;
                float f2 = Float.MAX_VALUE;
                float f3 = Float.MAX_VALUE;
                float f4 = -Float.MAX_VALUE;
                float f5 = -Float.MAX_VALUE;
                float f6 = -Float.MAX_VALUE;
                int i = bakedQuads.size();

                for (int i2 = 0; i2 < i; i2++) {
                    BakedQuad bakedQuad = (BakedQuad)bakedQuads.get(i2);

                    for (int i3 = 0; i3 < 4; i3++) {
                        Vector3fc vector3fc = bakedQuad.getPosition(i3);
                        f = Math.min(f, vector3fc.x());
                        f2 = Math.min(f2, vector3fc.y());
                        f3 = Math.min(f3, vector3fc.z());
                        f4 = Math.max(f4, vector3fc.x());
                        f5 = Math.max(f5, vector3fc.y());
                        f6 = Math.max(f6, vector3fc.z());
                    }
                }

                if (!(f > f4)) {
                    this.addBox(matrix4f, matrix4f2, f, f2, f3, f4, f5, f6);
                }
            }
        }

        private void addBox(Matrix4f matrix4f, Matrix4f matrix4f2, float f, float f2, float f3, float f4, float f5, float f6) {
            if (this.boxCount < 2) {
                this.mins[this.boxCount].set(f, f2, f3);
                this.maxs[this.boxCount].set(f4, f5, f6);
                this.transforms[this.boxCount].set(matrix4f).mul(matrix4f2);
                this.inverseTransforms[this.boxCount].set(this.transforms[this.boxCount]).invert();
                this.boxCount++;
            }
        }

        void finish() {
            if (this.boxCount > 0) {
                this.capturedAt = System.nanoTime();
            } else {
                this.capturedAt = 0L;
            }
        }

        private boolean isFresh() {
            return this.capturedAt != 0L && System.nanoTime() - this.capturedAt < 200000000L;
        }

        int getCorners(Vector3f[] vector3fs) {
            if (!this.isFresh()) {
                return 0;
            }

            int i = 0;

            for (int i2 = 0; i2 < this.boxCount; i2++) {
                for (int i3 = 0; i3 < 8 && i < vector3fs.length; i3++) {
                    float f = (i3 & 1) == 0 ? this.mins[i2].x : this.maxs[i2].x;
                    float f2 = (i3 & 2) == 0 ? this.mins[i2].y : this.maxs[i2].y;
                    float f3 = (i3 & 4) == 0 ? this.mins[i2].z : this.maxs[i2].z;
                    vector3fs[i++].set(f, f2, f3).mulPosition(this.transforms[i2]);
                }
            }

            return i;
        }

        float raycast(Vector3fc vector3fc) {
            if (!this.isFresh()) {
                return -1.0F;
            }

            float f = -1.0F;

            for (int i = 0; i < this.boxCount; i++) {
                this.rayOrigin.set(0.0F, 0.0F, 0.0F).mulPosition(this.inverseTransforms[i]);
                this.rayDirection.set(vector3fc).mulDirection(this.inverseTransforms[i]);
                float f2 = intersectRayBox(this.rayOrigin, this.rayDirection, this.mins[i], this.maxs[i]);
                if (!(f2 < 0.0F) && (f < 0.0F || f2 < f)) {
                    f = f2;
                }
            }

            return f;
        }

        private static float intersectRayBox(Vector3f vector3f, Vector3f vector3f2, Vector3f vector3f3, Vector3f vector3f4) {
            float f = -Float.MAX_VALUE;
            float f2 = Float.MAX_VALUE;

            for (int i = 0; i < 3; i++) {
                float f3 = vector3f.get(i);
                float f4 = vector3f2.get(i);
                float f5 = vector3f3.get(i);
                float f6 = vector3f4.get(i);
                if (Math.abs(f4) < 1.0E-6F) {
                    if (f3 < f5 || f3 > f6) {
                        return -1.0F;
                    }
                } else {
                    float f7 = (f5 - f3) / f4;
                    float f8 = (f6 - f3) / f4;
                    if (f7 > f8) {
                        float f9 = f7;
                        f7 = f8;
                        f8 = f9;
                    }

                    f = Math.max(f, f7);
                    f2 = Math.min(f2, f8);
                    if (f > f2) {
                        return -1.0F;
                    }
                }
            }

            return f > 0.0F ? f : -1.0F;
        }
    }

    static final class SmoothedOffset {
        float x;
        float y;
        float z;
        float scale = 1.0F;
        private long lastUpdateTime;

        void update(float f, float f2, float f3, float f4, float f5) {
            long l = System.nanoTime();
            boolean flag;
            float f6;
            if (this.lastUpdateTime == 0L) {
                flag = true;
                f6 = 0.0F;
            } else {
                flag = false;
                f6 = Math.min((float)(l - this.lastUpdateTime) / 1.0E9F, 0.25F);
            }

            float f7 = f6;
            this.lastUpdateTime = l;
            float f8 = !flag && !(f5 <= 1.0E-4F) ? 1.0F - (float)Math.exp(-f7 / f5) : 1.0F;
            this.x = this.x + (f - this.x) * f8;
            this.y = this.y + (f2 - this.y) * f8;
            this.z = this.z + (f3 - this.z) * f8;
            this.scale = this.scale + (f4 - this.scale) * f8;
        }
    }
}
