package tech.sadnes.client.feature.impl.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.DyeColor;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.SlotTooltipEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.ValueBind;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.GuiScale;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.render.Radius;
import tech.sadnes.client.render.RenderUtil;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.KeybindSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.util.Key;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;
import tech.sadnes.client.util.math.MathUtil;

@FeatureInfo(
    name = "Previews",
    description = "feature.impl.render.previews",
    displayName = "feature.impl.render.previews",
    category = Category.VISUALS,
    subcategory = Subcategory.WORLD
)
public final class Previews extends Feature {
    private final KeybindSetting button = new KeybindSetting("previews.button");
    private final MultiSetting types = new MultiSetting("previews.types", BooleanSetting.of("bundle", true), BooleanSetting.of("shulker", true));
    private final BooleanSetting inWorld = new BooleanSetting("previews.inWorld", true);
    private final BooleanSetting inContainers = new BooleanSetting("previews.inContainers", true);

    private boolean isButtonPressed() {
        if (Key.isPressed(this.button.getDefaultValue())) {
            return true;
        }

        for (ValueBind valueBind : this.button.getBinds()) {
            if (Key.isPressed(valueBind.getKey())) {
                return true;
            }
        }

        return false;
    }

    @EventHandler
    public void onSlotRender(SlotTooltipEvent slotTooltipEvent) {
        if (this.isButtonPressed() && this.inContainers.getValue() && this.types.has("shulker")) {
            Slot slot = slotTooltipEvent.getFocusedSlot();
            if (slot != null && slot.hasStack()) {
                ItemStack itemStack = slot.getStack();
                if (this.isShulkerBox(itemStack)) {
                    List list = this.getContents(itemStack);
                    if (list != null) {
                        this.renderContents(slotTooltipEvent.getContext(), list, slotTooltipEvent.getMouseX() + 12.0F, slotTooltipEvent.getMouseY() - 12.0F, this.getItemColor(itemStack));
                        slotTooltipEvent.cancel();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (renderEvent.isHudPre() && this.inWorld.getValue()) {
            if (mc.player != null && mc.world != null) {
                for (Previews.Preview preview : this.collectItemPreviews(renderEvent.getTickDelta())) {
                    this.renderWorldPreview(renderEvent.getContext(), preview, true);
                }

                if (this.isButtonPressed()) {
                    Previews.Preview preview2 = this.getLookedContainerPreview();
                    if (preview2 != null) {
                        this.renderWorldPreview(renderEvent.getContext(), preview2, false);
                    }
                }
            }
        }
    }

    private void renderWorldPreview(DrawContext drawContext, Previews.Preview preview, boolean flag) {
        Vector2f vector2f = ProjectionUtil.worldToScreen(preview.pos());
        if (vector2f != null) {
            if (!flag || !(vector2f.x < 0.0F) && !(vector2f.y < 0.0F) && !(vector2f.x > GuiScale.getScaledWidthInt()) && !(vector2f.y > GuiScale.getScaledHeightInt())) {
                int i = preview.contents().size();
                int i2 = Math.min(i, 9);
                int i3 = Math.max(1, (int)Math.ceil(i / 9.0));
                float f = i2 * 18.0F + 8.0F;
                float f2 = i3 * 18.0F + 8.0F;
                float f3 = GuiRenderUtil.clampToScreenX(vector2f.x - f / 2.0F, f);
                float f4 = GuiRenderUtil.clampToScreenY(vector2f.y - f2 - 6.0F, f2);
                MatrixUtil.push(drawContext);
                MatrixUtil.scale(drawContext, vector2f.x, vector2f.y, 0.65F);
                this.renderContents(drawContext, preview.contents(), f3 + 4.0F, f4 + 4.0F, preview.color());
                MatrixUtil.pop(drawContext);
            }
        }
    }

    private Previews.Preview getLookedContainerPreview() {
        if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            if (mc.world.getBlockEntity(blockPos) instanceof Inventory inventory) {
                Block block = mc.world.getBlockState(blockPos).getBlock();
                if (block instanceof ShulkerBoxBlock && !this.types.has("shulker")) {
                    return null;
                }

                int i = inventory.size();
                ArrayList arrayList = new ArrayList(i);

                for (int i2 = 0; i2 < i; i2++) {
                    ItemStack itemStack = inventory.getStack(i2);
                    if (!itemStack.isEmpty()) {
                        arrayList.add(itemStack);
                    }
                }

                if (arrayList.isEmpty()) {
                    return null;
                }

                return new Previews.Preview(arrayList, Vec3d.ofCenter(blockPos).add(0.0, 0.6, 0.0), this.getBlockColor(block));
            }
        }

        return null;
    }

    private List<Previews.Preview> collectItemPreviews(float f) {
        ArrayList arrayList = new ArrayList();
        Box box = mc.player.getBoundingBox().expand(32.0);

        for (ItemEntity itemEntity : mc.world.getEntitiesByClass(ItemEntity.class, box, itemEntity2 -> true)) {
            ItemStack itemStack = itemEntity.getStack();
            List list = this.getContents(itemStack);
            if (list != null) {
                arrayList.add(new Previews.Preview(list, ProjectionUtil.getInterpolatedPos(itemEntity, f).add(0.0, 0.5, 0.0), this.getItemColor(itemStack)));
            }
        }

        return arrayList;
    }

    private void renderContents(DrawContext drawContext, List<ItemStack> itemStacks, float f, float f2, int i) {
        int i2 = itemStacks.size();
        int i3 = Math.min(i2, 9);
        int i4 = Math.max(1, (int)Math.ceil(i2 / 9.0));
        float f3 = MathUtil.roundToStep(f - 4.0F, 0.5F);
        float f4 = MathUtil.roundToStep(f2 - 4.0F, 0.5F);
        float f5 = MathUtil.roundToStep(i3 * 18.0F + 8.0F, 0.5F);
        float f6 = MathUtil.roundToStep(i4 * 18.0F + 8.0F, 0.5F);
        Radius radius = Radius.all(5.0F);
        GuiRenderUtil.window(drawContext, f3, f4, f5, f6, 1.0F, 5.0F);
        new RenderUtil(drawContext, f3, f4, f5, f6).rect(ColorUtil.setAlphaf(i, 0.42F), radius);

        for (int i5 = 0; i5 < i2; i5++) {
            ItemStack itemStack = (ItemStack)itemStacks.get(i5);
            if (!itemStack.isEmpty()) {
                float f7 = f + i5 % 9 * 18.0F;
                float f8 = f2 + i5 / 9 * 18.0F;
                float f9 = MathUtil.roundToStep(f7 + 1.0F, 0.5F);
                float f10 = MathUtil.roundToStep(f8 + 1.0F, 0.5F);
                GuiRenderUtil.drawItem(drawContext, itemStack, f9, f10, 1.0F);
                if (itemStack.getCount() > 1) {
                    String string = Integer.toString(itemStack.getCount());
                    drawContext.drawText(
                        mc.textRenderer,
                        string,
                        (int)(f9 + 17.0F - mc.textRenderer.getWidth(string)),
                        (int)(f10 + 9.0F),
                        ColorUtil.setAlphaf(Theme.getTextColor(), 1.0F),
                        true
                    );
                }
            }
        }
    }

    private List<ItemStack> getContents(ItemStack itemStack) {
        if (this.isBundle(itemStack) && this.types.has("bundle")) {
            BundleContentsComponent bundleContentsComponent = itemStack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundleContentsComponent != null && !bundleContentsComponent.isEmpty()) {
                ArrayList arrayList = new ArrayList(bundleContentsComponent.size());

                for (ItemStack itemStack2 : bundleContentsComponent.iterate()) {
                    arrayList.add(itemStack2);
                }

                return arrayList;
            }
        } else if (this.isShulkerBox(itemStack) && this.types.has("shulker")) {
            ContainerComponent containerComponent = itemStack.get(DataComponentTypes.CONTAINER);
            if (containerComponent == null) {
                return null;
            }

            ArrayList arrayList2 = new ArrayList();
            containerComponent.stream().forEach(itemStack3 -> {
                if (!itemStack3.isEmpty()) {
                    arrayList2.add(itemStack3);
                }
            });
            if (arrayList2.isEmpty()) {
                return null;
            }

            return arrayList2;
        }

        return null;
    }

    private boolean isBundle(ItemStack itemStack) {
        return itemStack.getItem() instanceof BundleItem;
    }

    private boolean isShulkerBox(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return item instanceof BlockItem && ((BlockItem)item).getBlock() instanceof ShulkerBoxBlock;
    }

    private int getItemColor(ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (item instanceof BlockItem && ((BlockItem)item).getBlock() instanceof ShulkerBoxBlock shulkerBoxBlock) {
            return this.getDyeColor(shulkerBoxBlock.getColor());
        } else {
            return this.isBundle(itemStack) ? -4688322 : Theme.getPrimary();
        }
    }

    private int getBlockColor(Block block) {
        if (block instanceof ShulkerBoxBlock shulkerBoxBlock) {
            return this.getDyeColor(shulkerBoxBlock.getColor());
        } else if (block instanceof ChestBlock) {
            return -6264787;
        } else {
            return block instanceof BarrelBlock ? -6657228 : Theme.getPrimary();
        }
    }

    private int getDyeColor(DyeColor dyeColor) {
        return dyeColor == null ? -7710070 : 0xFF000000 | dyeColor.getEntityColor();
    }

    record Preview(List<ItemStack> contents, Vec3d pos, int color) {
    }
}
