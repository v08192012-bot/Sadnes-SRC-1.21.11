package tech.sadnes.client.feature.impl.render.esp;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.impl.render.Esp;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.render.font.Fonts;
import tech.sadnes.client.render.font.MsdfFont;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

public final class HeldItemsElement extends EspElement {
    private static final float ITEM_SIZE = 16.0F;
    private static final float ITEM_RENDER_SIZE = 8.0F;
    private static final float PADDING = 2.0F;
    private static final float ROW_GAP = 2.0F;
    private static final float ICON_GAP = 2.0F;
    private static final float DIVIDER_WIDTH = 0.5F;
    private static final float DIVIDER_HEIGHT = 3.5F;
    private static final float SEGMENT_GAP = 4.5F;
    private static final Radius DIVIDER_RADIUS = Radius.all(0.25F);
    public final BooleanSetting showCount = this.addSetting(new BooleanSetting("Show count", true));
    private final TextSetting layout = this.createLayoutSetting(EspAnchor.BOTTOM);
    private final List<ItemStack> heldItems = new ArrayList<>(2);
    private final Animation visibilityAnimation2 = new Animation();
    private float rowHeight;
    private int itemCount;
    private float[] rowWidths = new float[2];
    private List<ItemStack> displayedItems;

    public HeldItemsElement(EspTargetType espTargetType) {
        super("Held items", true, espTargetType);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public boolean isAttachable() {
        return true;
    }

    @Override
    public boolean isVisible() {
        this.visibilityAnimation2.update();
        this.visibilityAnimation2.animate(this.isActive() ? 1.0 : 0.0, 0.22, Easings.QUART_OUT);
        return this.isActive() || this.visibilityAnimation2.get() > 0.001F;
    }

    @Override
    public void renderForEntity(
        DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, float f7, float f8
    ) {
        if (this.measured && this.itemCount != 0) {
            float f9 = f8 * this.visibilityAnimation2.get();
            if (!(f9 <= 0.001F)) {
                this.renderRowBackgrounds(drawContext, f9);
                this.renderRowContents(drawContext, f9);
            }
        }
    }

    @Override
    public void measure(Entity entity, boolean flag) {
        this.measured = false;
        this.itemCount = 0;
        MsdfFont msdfFont = Fonts.INTER_MEDIUM;
        if (msdfFont != null && entity instanceof PlayerEntity playerEntity) {
            List list = this.collectHeldItems(playerEntity);
            if (list.isEmpty()) {
                if (!flag) {
                    return;
                }

                list = List.of(new ItemStack(Items.DIAMOND_SWORD));
            }

            float f = this.getFontSize();
            float f2 = f + 4.0F;
            int i = list.size();
            if (this.rowWidths.length < i) {
                this.rowWidths = new float[i];
            }

            float f3 = 0.0F;

            for (int i2 = 0; i2 < i; i2++) {
                float f4 = 16.5F + msdfFont.getTextWidth(((ItemStack)list.get(i2)).getName(), f);
                this.rowWidths[i2] = f4;
                if (f4 > f3) {
                    f3 = f4;
                }
            }

            this.width = f3;
            this.height = i * f2 + (i - 1) * 2.0F;
            this.rowHeight = f2;
            this.itemCount = i;
            this.displayedItems = list;
            this.measured = true;
        }
    }

    @Override
    public void renderBackground(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
        if (this.measured && this.itemCount != 0) {
            this.renderRowBackgrounds(drawContext, 1.0F);
        }
    }

    @Override
    public void renderOverlay(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
        if (this.measured && this.itemCount != 0) {
            this.renderRowContents(drawContext, 1.0F);
        }
    }

    private List<ItemStack> collectHeldItems(PlayerEntity playerEntity) {
        this.heldItems.clear();
        ItemStack itemStack = playerEntity.getMainHandStack();
        if (!itemStack.isEmpty()) {
            this.heldItems.add(itemStack);
        }

        ItemStack itemStack2 = playerEntity.getOffHandStack();
        if (!itemStack2.isEmpty()) {
            this.heldItems.add(itemStack2);
        }

        return this.heldItems;
    }

    private float getFontSize() {
        return 6.0F;
    }

    private void renderRowBackgrounds(DrawContext drawContext, float f) {
        float f2 = this.getLayoutX();
        float f3 = this.getLayoutY();
        boolean flag = this.isFriendTarget();
        float f4 = f * Esp.getInstance().backgroundAlpha.getValue();
        int i = ColorUtil.setAlphaf(Theme.getSecondaryText(), f4 * 0.3F);

        for (int i2 = 0; i2 < this.itemCount; i2++) {
            float f5 = this.rowWidths[i2];
            float f6 = f2 + (this.width - f5) / 2.0F;
            float f7 = f3 + i2 * (this.rowHeight + 2.0F);
            GuiRenderUtil.drawPanel(drawContext, f6, f7, f5, this.rowHeight, f4, 3.0F, flag);
            float f8 = f6 + 2.0F + 8.0F + 2.0F;
            new RenderUtil(drawContext, f8, f7 + (this.rowHeight - 3.5F) / 2.0F, 0.5F, 3.5F).rect(i, DIVIDER_RADIUS);
        }
    }

    private void renderRowContents(DrawContext drawContext, float f) {
        MsdfFont msdfFont = Fonts.INTER_MEDIUM;
        if (msdfFont != null) {
            float f2 = this.getFontSize();
            float f3 = this.getLayoutX();
            float f4 = this.getLayoutY();
            int i = ColorUtil.multiplyAlpha(Theme.getTextColor(), f);

            for (int i2 = 0; i2 < this.itemCount; i2++) {
                ItemStack itemStack = this.displayedItems.get(i2);
                float f5 = this.rowWidths[i2];
                float f6 = f3 + (this.width - f5) / 2.0F;
                float f7 = f4 + i2 * (this.rowHeight + 2.0F);
                new RenderUtil(drawContext, f6, f7, f5, this.rowHeight).textTopLeft(msdfFont, itemStack.getName(), 14.5F, (this.rowHeight - f2) / 2.0F, i, f2);
                this.renderItem(drawContext, itemStack, f6 + 2.0F, f7 + (this.rowHeight - 8.0F) / 2.0F, f);
            }
        }
    }

    private void renderItem(DrawContext drawContext, ItemStack itemStack, float f, float f2, float f3) {
        float f4 = MathUtil.roundToStep(f, 0.5F);
        float f5 = MathUtil.roundToStep(f2, 0.5F);
        MatrixUtil.push(drawContext);
        MatrixUtil.translate(drawContext, f4, f5);
        MatrixUtil.scale(drawContext, 0.0F, 0.0F, 0.5F);
        GuiRenderUtil.drawCachedItem(drawContext, itemStack, itemStack, 0.0F, 0.0F, f3);
        if (this.showCount.getValue() && f3 > 0.99F) {
            drawContext.drawStackOverlay(MinecraftClient.getInstance().textRenderer, prepareDisplayStack(itemStack), 0, 0);
        }

        MatrixUtil.pop(drawContext);
    }
}
