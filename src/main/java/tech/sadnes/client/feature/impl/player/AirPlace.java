package tech.sadnes.client.feature.impl.player;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.color.Theme;

@FeatureInfo(
    name = "AirPlace",
    description = "feature.impl.player.airplace",
    displayName = "feature.impl.player.airplace",
    category = Category.PLAYER,
    subcategory = Subcategory.INTERACT
)
public final class AirPlace extends Feature {
    private final NumberSetting range = new NumberSetting("airplace.range", 5.0F, 1.0F, 6.0F, 0.5F);
    private int placeCooldown;

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        boolean flag = ServerManager.isNullWorld();
        int i = this.placeCooldown;
        if (!flag) {
            if (i > 0) {
                i--;
            }

            this.placeCooldown = i;
            Entity entity = mc.getCameraEntity();
            double d = this.range.getValue().floatValue();
            if (entity.raycast(d, 0.0F, false) instanceof BlockHitResult blockHitResult) {
                boolean flag2 = mc.player.getMainHandStack().getItem() instanceof BlockItem;
                boolean flag3 = mc.player.getOffHandStack().getItem() instanceof BlockItem;
                boolean flag4 = mc.options.useKey.isPressed();
                int i2 = this.placeCooldown;
                if (flag4 && (flag3 | flag2) && i2 <= 0) {
                    if (flag2) {
                        Hand hand = Hand.MAIN_HAND;
                        ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
                        ClientPlayerEntity clientPlayerEntity = mc.player;
                        clientPlayerInteractionManager.interactBlock(clientPlayerEntity, hand, blockHitResult);
                        mc.player.swingHand(hand);
                        this.placeCooldown = 4;
                        return;
                    }

                    Hand hand2 = Hand.OFF_HAND;
                    ClientPlayerInteractionManager clientPlayerInteractionManager2 = mc.interactionManager;
                    ClientPlayerEntity clientPlayerEntity2 = mc.player;
                    clientPlayerInteractionManager2.interactBlock(clientPlayerEntity2, hand2, blockHitResult);
                    mc.player.swingHand(hand2);
                    this.placeCooldown = 4;
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onRender3D(RenderEvent renderEvent) {
        if (renderEvent.isWorld() && !ServerManager.isNullWorld()) {
            boolean flag = mc.player.getMainHandStack().getItem() instanceof BlockItem;
            if (mc.player.getOffHandStack().getItem() instanceof BlockItem | flag) {
                Entity entity = mc.getCameraEntity();
                double d = this.range.getValue().floatValue();
                if (entity.raycast(d, 0.0F, false) instanceof BlockHitResult blockHitResult) {
                    int i = Theme.getPrimary();
                    MatrixStack matrixStack = renderEvent.getMatrices();
                    BlockPos blockPos = blockHitResult.getBlockPos();
                    Box box = new Box(blockPos);
                    int i2 = MathHelper.clamp(Math.round(ExactMath.multiply(i >>> 24, 0.25F)), 0, 255);
                    int i3 = MathHelper.clamp(i >>> 16, 0, 255);
                    int i4 = MathHelper.clamp(i >>> 8, 0, 255);
                    int i5 = MathHelper.clamp(i, 0, 255);
                    int i6 = i4 << 8;
                    int i7 = i3 << 16;
                    int i8 = i2 << 24;
                    int i9 = i7 | i8;
                    int i10 = i6 | i9 | i5;
                    Render3DUtil.drawGlowBox(matrixStack, box, i, i10);
                    return;
                }
            }
        }
    }
}
