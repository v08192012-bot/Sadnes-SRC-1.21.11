package tech.sadnes.client.feature.impl.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;

final class TargetUsingItemIndicator {
    static final float ICON_SIZE = 8.0F;
    private final Animation animation = new Animation();
    private LivingEntity lastEntity;
    private ItemStack displayedItem = ItemStack.EMPTY;

    void render(DrawContext drawContext, LivingEntity livingEntity, float f, float f2, float f3) {
        if (livingEntity != this.lastEntity) {
            this.animation.forceValue(0.0);
            this.displayedItem = ItemStack.EMPTY;
            this.lastEntity = livingEntity;
        }

        boolean flag;
        if (livingEntity.isUsingItem()) {
            flag = isEatOrDrink(livingEntity.getActiveItem());
        } else {
            flag = false;
        }

        if (flag) {
            this.displayedItem = livingEntity.getActiveItem();
        }

        this.animation.update();
        this.animation.animate(flag ? 1.0 : 0.0, 0.25, Easings.QUAD_OUT);
        float f4 = this.animation.get();
        if (!(f4 <= 0.01F) && !this.displayedItem.isEmpty()) {
            double d = System.currentTimeMillis() * 0.0157;
            float f5 = f - 4.0F;
            float f6 = f2 - 4.0F - (float)Math.abs(Math.cos(d)) * 2.5F;
            float f7 = (float)Math.sin(d) * 9.0F;
            MatrixUtil.push(drawContext);
            MatrixUtil.rotate(drawContext, f5 + 4.0F, f6 + 4.0F, f7);
            MatrixUtil.scale(drawContext, f5, f6, 0.5F);
            GuiRenderUtil.drawItem(drawContext, this.displayedItem, f5, f6, f4 * f3);
            MatrixUtil.pop(drawContext);
        } else {
            if (!flag) {
                this.displayedItem = ItemStack.EMPTY;
            }
        }
    }

    static boolean isEatOrDrink(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }

        UseAction useAction = itemStack.getUseAction();
        return useAction == UseAction.EAT || useAction == UseAction.DRINK;
    }
}
