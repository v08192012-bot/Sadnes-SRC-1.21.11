package tech.sadnes.client.feature.impl.misc;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.util.color.ColorUtil;

@FeatureInfo(
    name = "AncientESP",
    description = "feature.impl.misc.ancientesp",
    displayName = "feature.impl.misc.ancientesp",
    category = Category.VISUALS,
    subcategory = Subcategory.HIGHLIGHTS
)
public final class AncientESP extends Feature {
    private final ColorSetting color = new ColorSetting("ancientesp.color", -2069973);
    private final Set<BlockPos> debrisPositions = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        super.onEnable();
        this.debrisPositions.clear();
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!event.isReceive()) {
            return;
        }

        switch (event.getPacket()) {
            case ChunkDeltaUpdateS2CPacket packet -> packet.visitUpdates(this::updateDebrisPosition);
            case BlockUpdateS2CPacket packet -> this.updateDebrisPosition(packet.getPos(), packet.getState());
            default -> {
            }
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.debrisPositions.clear();
    }

    @EventHandler
    public void onRender3D(RenderEvent renderEvent) {
        if (renderEvent.isWorld() && mc.world != null && !this.debrisPositions.isEmpty()) {
            int i = this.color.getValue();
            int i2 = ColorUtil.multiplyAlpha(i, 0.25F);

            for (BlockPos blockPos : this.debrisPositions) {
                if (mc.world.getBlockState(blockPos).isOf(Blocks.ANCIENT_DEBRIS)) {
                    Render3DUtil.drawBox(renderEvent.getMatrices(), new Box(blockPos), i, i2);
                } else {
                    this.debrisPositions.remove(blockPos);
                }
            }
        }
    }

    private void updateDebrisPosition(BlockPos blockPos, BlockState blockState) {
        if (blockState.isOf(Blocks.ANCIENT_DEBRIS)) {
            this.debrisPositions.add(blockPos.toImmutable());
        } else {
            this.debrisPositions.remove(blockPos);
        }
    }
}
