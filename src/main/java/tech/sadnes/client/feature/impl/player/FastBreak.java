package tech.sadnes.client.feature.impl.player;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import tech.sadnes.api.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;

@FeatureInfo(
    name = "FastBreak",
    description = "feature.impl.player.fastbreak",
    displayName = "feature.impl.player.fastbreak",
    category = Category.PLAYER,
    subcategory = Subcategory.INTERACT
)
public class FastBreak extends Feature {
    private final NumberSetting threshold = new NumberSetting("fastbreak.threshold", 0.5F, 0.1F, 1.0F, 0.05F, Unit.EMPTY);
    private final BooleanSetting legacyGrimBypass = new BooleanSetting("fastbreak.legacyGrimBypass", false);
    public BlockPos breakingPos;
    public Direction breakingDirection;
    public boolean brokeEarly;

    public static FastBreak getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(FastBreak.class);
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (this.isEnabled() && mc.player != null) {
            ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
            BlockPos blockPos = this.breakingPos;
            Direction direction = this.breakingDirection;
            boolean flag = this.brokeEarly;
            if (clientPlayerInteractionManager != null && blockPos != null && direction != null && !flag) {
                float f = ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).sadnes$getCurrentBreakingProgress();
                float f2 = this.threshold.getValue();
                if (f >= f2) {
                    ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
                    Action action = Action.STOP_DESTROY_BLOCK;
                    BlockPos blockPos2 = this.breakingPos;
                    Direction direction2 = this.breakingDirection;
                    PlayerActionC2SPacket playerActionC2SPacket = new PlayerActionC2SPacket(action, blockPos2, direction2);
                    clientPlayNetworkHandler.sendPacket(playerActionC2SPacket);
                    ClientPlayNetworkHandler clientPlayNetworkHandler2 = mc.player.networkHandler;
                    Action action2 = Action.ABORT_DESTROY_BLOCK;
                    BlockPos blockPos3 = this.breakingPos;
                    Direction direction3 = this.breakingDirection;
                    PlayerActionC2SPacket playerActionC2SPacket2 = new PlayerActionC2SPacket(action2, blockPos3, direction3);
                    clientPlayNetworkHandler2.sendPacket(playerActionC2SPacket2);
                    this.brokeEarly = true;
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (this.isEnabled() && this.legacyGrimBypass.getValue() && packetEvent.isSend() && mc.player != null) {
            if (packetEvent.getPacket() instanceof PlayerActionC2SPacket playerActionC2SPacket && playerActionC2SPacket.getAction() == Action.STOP_DESTROY_BLOCK) {
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.ABORT_DESTROY_BLOCK, playerActionC2SPacket.getPos().up(500), playerActionC2SPacket.getDirection()));
            }
        }
    }
}
