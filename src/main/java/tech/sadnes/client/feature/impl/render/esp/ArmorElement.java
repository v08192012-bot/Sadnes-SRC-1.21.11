package tech.sadnes.client.feature.impl.render.esp;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import tech.sadnes.client.animation.Animation;
import tech.sadnes.client.animation.Easings;
import tech.sadnes.client.feature.impl.render.Esp;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.render.GuiRenderUtil;
import tech.sadnes.client.render.MatrixUtil;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.util.math.MathUtil;

public final class ArmorElement extends EspElement {
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final float ITEM_SIZE = 16.0F;
    private static final float ICON_SIZE = 8.0F;
    private static final float PADDING = 1.0F;
    private static final float SPACING = 1.0F;
    public final BooleanSetting showDurability = this.addSetting(new BooleanSetting("Show durability", true));
    private final TextSetting layout = this.createLayoutSetting(EspAnchor.TOP, "Nametag", 0.5F, 0.0F);
    private final List<ItemStack> items = new ArrayList<>(6);
    private final Animation visibilityAnimation2 = new Animation();

    public ArmorElement(EspTargetType espTargetType) {
        super("Armor", true, espTargetType);
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
        if (this.measured) {
            float f9 = this.visibilityAnimation2.get() * f8;
            if (!(f9 <= 0.001F)) {
                this.renderPanel(drawContext, f9);
                this.renderItems(drawContext, f9);
            }
        }
    }

    @Override
    public void measure(Entity entity, boolean flag) {
        this.measured = false;
        if (entity instanceof PlayerEntity playerEntity) {
            int i = this.collectItems(playerEntity).size();
            if (i != 0) {
                float f = i * 8.0F + (i - 1) * 1.0F;
                this.width = f + 2.0F;
                this.height = 10.0F;
                this.measured = true;
            }
        }
    }

    @Override
    public void renderBackground(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
        if (this.measured) {
            this.renderPanel(drawContext, 1.0F);
        }
    }

    @Override
    public void renderOverlay(DrawContext drawContext, Entity entity, float f, float f2, float f3, float f4, float f5) {
        if (this.measured) {
            this.renderItems(drawContext, 1.0F);
        }
    }

    private void renderPanel(DrawContext drawContext, float f) {
        float f2 = f * Esp.getInstance().backgroundAlpha.getValue();
        GuiRenderUtil.drawPanel(drawContext, this.getLayoutX(), this.getLayoutY(), this.width, this.height, f2, 3.0F, this.isFriendTarget());
    }

    private void renderItems(DrawContext drawContext, float f) {
        float f2 = this.getLayoutX() + 1.0F;
        float f3 = this.getLayoutY() + 1.0F;
        int i = this.items.size();

        for (int i2 = 0; i2 < i; i2++) {
            this.renderItem(drawContext, this.items.get(i2), f2 + i2 * 9.0F, f3, f);
        }
    }

    private void renderItem(DrawContext drawContext, ItemStack itemStack, float f, float f2, float f3) {
        float f4 = MathUtil.roundToStep(f, 0.5F);
        float f5 = MathUtil.roundToStep(f2, 0.5F);
        MatrixUtil.push(drawContext);
        MatrixUtil.translate(drawContext, f4, f5);
        MatrixUtil.scale(drawContext, 0.5F, 0.5F, 0.5F);
        GuiRenderUtil.drawCachedItem(drawContext, itemStack, itemStack, 0.0F, 0.0F, f3);
        if (this.showDurability.getValue() && f3 > 0.99F) {
            drawContext.drawStackOverlay(MinecraftClient.getInstance().textRenderer, prepareDisplayStack(itemStack), 0, 0);
        }

        MatrixUtil.pop(drawContext);
    }

    private boolean isHeldItemsElementActive() {
        Esp esp = Esp.getInstance();
        if (esp == null) {
            return false;
        }

        for (EspElement espElement : esp.getElements(this.getTarget())) {
            if (espElement instanceof HeldItemsElement && ((HeldItemsElement)espElement).isActive()) {
                return true;
            }
        }

        return false;
    }

    private List<ItemStack> collectItems(PlayerEntity playerEntity) {
        this.items.clear();
        boolean flag = !this.isHeldItemsElementActive();
        if (flag) {
            ItemStack itemStack = playerEntity.getOffHandStack();
            if (!itemStack.isEmpty()) {
                this.items.add(itemStack);
            }
        }

        for (EquipmentSlot equipmentSlot : ARMOR_SLOTS) {
            ItemStack itemStack2 = playerEntity.getEquippedStack(equipmentSlot);
            if (!itemStack2.isEmpty()) {
                this.items.add(itemStack2);
            }
        }

        if (flag) {
            ItemStack itemStack3 = playerEntity.getMainHandStack();
            if (!itemStack3.isEmpty()) {
                this.items.add(itemStack3);
            }
        }

        return this.items;
    }
}
