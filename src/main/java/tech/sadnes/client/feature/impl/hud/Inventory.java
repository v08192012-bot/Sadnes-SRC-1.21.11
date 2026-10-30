package tech.sadnes.client.feature.impl.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.DragSetting;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.drag.DragComponent;
import tech.sadnes.client.ui.hud.Draggable;
import tech.sadnes.client.ui.hud.HudPanelUtil;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.player.InventoryUtil;

@FeatureInfo(
    name = "Inventory",
    description = "feature.impl.hud.inventory",
    displayName = "feature.impl.hud.inventory",
    category = Category.INTERFACE,
    subcategory = Subcategory.HUD_INVENTORY
)
public class Inventory extends Feature implements Draggable {
    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final float PADDING2 = 8.0F;
    private static final float SLOT_GAP = 1.5F;
    private static final float SLOT_SIZE = 11.0F;
    private static final float HOTBAR_GAP = 1.5F;
    private static final float SEPARATOR_OFFSET = 1.5F;
    private static final float CORNER_RADIUS = 3.0F;
    private static final float SLOT_STEP = 12.5F;
    private static final float MIN_WIDTH = 111.0F;
    private static final float MIN_HEIGHT = 40.0F;
    private static final double SWAP_DURATION = 0.22;
    private final DragSetting draggable;
    private final BooleanSetting dynamicHeader = new BooleanSetting("hud.dynamicHeader", true);
    private final BooleanSetting showHotbar = new BooleanSetting("inventory.showHotbar", true);
    private final BooleanSetting showCount = new BooleanSetting("inventory.showCount", true);
    private final Animation showAnimation = new Animation();
    private int mouseX;
    private int mouseY;
    private float gridX;
    private float gridY;
    private int rowCount;
    private int draggedSlot = -1;
    private ItemStack draggedStack = ItemStack.EMPTY;
    private float dragX;
    private float dragY;
    private long lastFrameNanos;
    private final Animation swapAnimation = new Animation();
    private boolean swapAnimating;
    private int swapFromSlot = -1;
    private int swapToSlot = -1;
    private ItemStack swapFromStack = ItemStack.EMPTY;
    private ItemStack swapToStack = ItemStack.EMPTY;
    private float swapStartX;
    private float swapStartY;
    private final Object[] slotCacheKeys = new Object[36];
    private final Object dragCacheKey = new Object();
    private final Object swapFromCacheKey = new Object();
    private final Object swapToCacheKey = new Object();

    public Inventory() {
        for (int i = 0; i < this.slotCacheKeys.length; i++) {
            this.slotCacheKeys[i] = new Object();
        }

        this.draggable = new DragSetting(this.getInfo().name(), 10.0F, 50.0F);
        this.draggable.setSize(111.0F, 15.0F + this.getGridHeight(true));
        this.draggable.setDraggable(() -> this.isEnabled() && this.draggedSlot < 0 ? true : false);
        this.draggable.setInteractive(this::isHoveringItem);
        this.draggable.onInput((drawContext, i2, i3, f) -> {
            this.mouseX = i2;
            this.mouseY = i3;
            this.render(drawContext);
        });
        this.draggable.onInput(this);
        this.draggable.setRenderCondition(this::isEnabled);
        DragComponent.register(this.draggable);
    }

    public DragSetting getDraggable() {
        return this.draggable;
    }

    @Override
    public void render(DrawContext drawContext) {
        if (this.isEnabled() && mc.player != null) {
            if (this.draggedSlot >= 0 && !this.isChatOpen()) {
                this.cancelDrag();
            }

            this.showAnimation.update();
            this.showAnimation.animate(1.0, 0.2, Easings.QUART_OUT);
            float f = this.showAnimation.get();
            if (!(f <= 0.001F)) {
                boolean flag = this.showHotbar.getValue();
                int i = 3 + (flag ? 1 : 0);
                float f2 = this.getGridHeight(flag);
                String string = this.getInfo().name();
                float f3 = HudPanelUtil.getHeaderWidth(8.0F, HEADER_FONT.getWidth(string, 6.0F), true, true);
                float f4 = this.dynamicHeader.getValue() ? Math.max(f3, 111.0F) : f3;
                float f5 = Math.max(f4, 111.0F);
                this.draggable.setSize(f5, 15.0F + f2);
                float f6 = this.draggable.position2.x;
                float f7 = this.draggable.position2.y;
                HudPanelUtil.drawHeader(drawContext, f6, f7, f4, f, string, Icon.PIGGY_BANK, BODY_FONT, HEADER_FONT, 6.0F);
                float f8 = f6 + (f5 - 111.0F) / 2.0F;
                float f9 = f7 + 13.0F + 2.0F;
                this.gridX = f8;
                this.gridY = f9;
                this.rowCount = i;
                this.drawSlots(drawContext, f8, f9, i, f);
                this.drawSwapAnimation(drawContext, f);
                this.drawDraggedItem(drawContext, f);
            }
        }
    }

    private void drawSlots(DrawContext drawContext, float f, float f2, int i, float f3) {
        PlayerInventory playerInventory = mc.player.getInventory();

        for (int i2 = 0; i2 < i; i2++) {
            float f4 = f2 + this.getRowOffset(i2);
            HudPanelUtil.drawSmallPanel(drawContext, f, f4, 111.0F, 11.0F, f3);
            this.drawSeparators(drawContext, f, f4, f3);

            for (int i3 = 0; i3 < 9; i3++) {
                float f5 = f + i3 * 12.5F;
                int i4 = this.toInventoryIndex(i2, i3);
                ItemStack itemStack = playerInventory.getStack(i4);
                if (!itemStack.isEmpty() && i4 != this.draggedSlot && (!this.swapAnimating || i4 != this.swapFromSlot && i4 != this.swapToSlot)) {
                    this.drawItem(drawContext, this.slotCacheKeys[i4], itemStack, f5, f4, f3);
                }
            }
        }
    }

    private void drawDraggedItem(DrawContext drawContext, float f) {
        if (this.draggedSlot >= 0 && !this.draggedStack.isEmpty()) {
            float f2 = this.mouseX - 5.5F;
            float f3 = this.mouseY - 5.5F;
            float f4 = 1.0F - (float)Math.exp(-40.0F * this.getFrameDelta());
            this.dragX = MathUtil.lerp(this.dragX, f2, f4);
            this.dragY = MathUtil.lerp(this.dragY, f3, f4);
            this.drawItem(drawContext, this.dragCacheKey, this.draggedStack, this.dragX, this.dragY, f);
        }
    }

    private void drawSwapAnimation(DrawContext drawContext, float f) {
        if (this.swapAnimating) {
            this.swapAnimation.update();
            float f2 = this.swapAnimation.get();
            float[] floats = this.getSlotPosition(this.swapToSlot);
            this.drawItem(
                drawContext,
                this.swapFromCacheKey,
                this.swapFromStack,
                MathUtil.lerp(this.swapStartX, floats[0], f2),
                MathUtil.lerp(this.swapStartY, floats[1], f2),
                f
            );
            if (!this.swapToStack.isEmpty()) {
                float[] floats2 = this.getSlotPosition(this.swapFromSlot);
                this.drawItem(drawContext, this.swapToCacheKey, this.swapToStack, MathUtil.lerp(floats[0], floats2[0], f2), MathUtil.lerp(floats[1], floats2[1], f2), f);
            }

            if (this.swapAnimation.isFinished()) {
                this.swapAnimating = false;
            }
        }
    }

    private void drawItem(DrawContext drawContext, Object object, ItemStack itemStack, float f, float f2, float f3) {
        float f4 = MathUtil.roundToStep(f + 1.5F, 0.5F);
        float f5 = MathUtil.roundToStep(f2 + 1.5F, 0.5F);
        MatrixUtil.push(drawContext);
        MatrixUtil.scale(drawContext, f4, f5, 0.5F);
        GuiRenderUtil.drawCachedItem(drawContext, object, itemStack, f4, f5, f3);
        if (this.showCount.getValue() && itemStack.getCount() != 1) {
            String string = Integer.toString(itemStack.getCount());
            drawContext.drawText(
                mc.textRenderer, string, (int)f4 + 17 - mc.textRenderer.getWidth(string), (int)f5 + 9, ColorUtil.setAlphaf(ColorUtil.WHITE, f3), true
            );
        }

        MatrixUtil.pop(drawContext);
    }

    private void drawSeparators(DrawContext drawContext, float f, float f2, float f3) {
        float f4 = f2 + 5.5F;

        for (int i = 0; i < 8; i++) {
            float f5 = f + i * 12.5F + 11.0F + 0.75F - 0.25F;
            HudPanelUtil.drawSeparator(drawContext, f5, f4, f3);
        }
    }

    @EventHandler
    public void onScreenKey(ScreenKeyEvent screenKeyEvent) {
        if (this.isEnabled() && screenKeyEvent.isKey(0) && this.isChatOpen() && mc.player != null) {
            float f = (float)screenKeyEvent.getMouseX();
            float f2 = (float)screenKeyEvent.getMouseY();
            if (screenKeyEvent.isPress()) {
                int i = this.getSlotAt(f, f2);
                if (i >= 0) {
                    ItemStack itemStack = mc.player.getInventory().getStack(i);
                    if (!itemStack.isEmpty()) {
                        this.draggedSlot = i;
                        this.draggedStack = itemStack;
                        this.dragX = f - 5.5F;
                        this.dragY = f2 - 5.5F;
                        this.lastFrameNanos = 0L;
                    }
                }
            } else {
                if (screenKeyEvent.isRelease() && this.draggedSlot >= 0) {
                    int i2 = this.getSlotAt(f, f2);
                    if (i2 >= 0 && i2 != this.draggedSlot) {
                        this.startSwapAnimation(this.draggedSlot, i2);
                        this.swapSlots(this.draggedSlot, i2);
                    }

                    this.cancelDrag();
                }
            }
        }
    }

    private void startSwapAnimation(int i, int i2) {
        this.swapFromSlot = i;
        this.swapToSlot = i2;
        this.swapFromStack = this.draggedStack.copy();
        this.swapToStack = mc.player.getInventory().getStack(i2).copy();
        this.swapStartX = this.dragX;
        this.swapStartY = this.dragY;
        this.swapAnimating = true;
        this.swapAnimation.forceValue(0.0);
        this.swapAnimation.animate(1.0, 0.22, Easings.QUART_OUT);
    }

    private void swapSlots(int i, int i2) {
        if (i >= 9 && i2 >= 9) {
            InventoryUtil.swapSlots(this.toScreenSlot(i), this.toScreenSlot(i2));
        } else {
            int i3;
            Inventory inventory;
            int i4;
            if (i < 9) {
                i3 = i;
                inventory = this;
                i4 = i2;
            } else {
                i3 = i2;
                inventory = this;
                i4 = i;
            }

            int i5 = inventory.toScreenSlot(i4);
            GuiMoveComponent.scheduleAction(() -> mc.interactionManager.clickSlot(0, i5, i3, SlotActionType.SWAP, mc.player), 2.0F, 2.0F);
        }
    }

    private void cancelDrag() {
        this.draggedSlot = -1;
        this.draggedStack = ItemStack.EMPTY;
    }

    private boolean isChatOpen() {
        return mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof ClickGuiScreen;
    }

    private boolean isHoveringItem() {
        if (mc.player == null) {
            return false;
        }

        int i = this.getSlotAt(this.mouseX, this.mouseY);
        return i >= 0 && !mc.player.getInventory().getStack(i).isEmpty();
    }

    private float getFrameDelta() {
        long l = System.nanoTime();
        if (this.lastFrameNanos == 0L) {
            this.lastFrameNanos = l;
            return 0.016666668F;
        } else {
            float f = (float)(l - this.lastFrameNanos) / 1.0E9F;
            this.lastFrameNanos = l;
            return MathUtil.clamp(0.004166667F, 0.1F, f);
        }
    }

    private int getSlotAt(float f, float f2) {
        for (int i = 0; i < this.rowCount; i++) {
            float f3 = this.gridY + this.getRowOffset(i);
            if (!(f2 < f3) && !(f2 > f3 + 11.0F)) {
                for (int i2 = 0; i2 < 9; i2++) {
                    float f4 = this.gridX + i2 * 12.5F;
                    if (f >= f4 && f <= f4 + 11.0F) {
                        return this.toInventoryIndex(i, i2);
                    }
                }
            }
        }

        return -1;
    }

    private float[] getSlotPosition(int i) {
        int i2 = i < 9 ? 3 : (i - 9) / 9;
        int i3 = i < 9 ? i : (i - 9) % 9;
        return new float[]{this.gridX + i3 * 12.5F, this.gridY + this.getRowOffset(i2)};
    }

    private boolean rowHasItems(PlayerInventory playerInventory, int i) {
        for (int i2 = 0; i2 < 9; i2++) {
            if (!playerInventory.getStack(this.toInventoryIndex(i, i2)).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private int toInventoryIndex(int i, int i2) {
        return i < 3 ? 9 + i * 9 + i2 : i2;
    }

    private int toScreenSlot(int i) {
        return i < 9 ? i + 36 : i;
    }

    private float getRowOffset(int i) {
        float f = i * 12.5F;
        if (i >= 3) {
            f += 1.5F;
        }

        return f;
    }

    private float getGridHeight(boolean flag) {
        int i = 3 + (flag ? 1 : 0);
        return this.getRowOffset(i - 1) + 11.0F;
    }

    public static Inventory getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(Inventory.class);
    }
}
