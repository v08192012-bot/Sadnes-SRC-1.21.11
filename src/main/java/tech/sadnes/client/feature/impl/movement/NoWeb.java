package tech.sadnes.client.feature.impl.movement;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.MoveUtil;

@FeatureInfo(
    name = "NoWeb",
    description = "feature.impl.movement.noweb",
    displayName = "feature.impl.movement.noweb",
    category = Category.MOVEMENT,
    subcategory = Subcategory.MOVEMENT_TERRAIN
)
public final class NoWeb extends Feature {
    private final ModeSetting mode = new ModeSetting("noweb.mode", new String[]{"none", "Grim", "Matrix", "ReallyWorld"});
    private final BooleanSetting normalizedMotion = new BooleanSetting("noweb.normalizedMotion", false)
        .setVisibleWhen(() -> !this.mode.is("Grim") && !this.mode.is("Matrix") ? false : true);

    public static NoWeb getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(NoWeb.class);
    }

    public static boolean shouldApplyWebSlowdown() {
        NoWeb noWeb = getInstance();
        return ServerManager.isInCobweb() && (noWeb == null || !noWeb.isIgnoringWeb());
    }

    public boolean isIgnoringWeb() {
        return this.isEnabled() && (this.mode.is("none") || this.mode.is("ReallyWorld"));
    }

    public void breakWeb(BlockPos blockPos) {
        if (this.isEnabled() && this.mode.is("ReallyWorld") && mc.player != null && mc.interactionManager != null) {
            BlockPos blockPos2 = blockPos.toImmutable();
            PacketSender.sendSequenced(i -> new PlayerActionC2SPacket(Action.STOP_DESTROY_BLOCK, blockPos2, Direction.UP, i));
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player != null && MoveUtil.isInCobwebMultiplier()) {
            boolean flag = mc.options.jumpKey.isPressed();
            boolean flag2 = mc.options.sneakKey.isPressed();
            if (this.mode.is("Grim")) {
                double[] doubles = MoveUtil.getMotionForInput(0.64);
                mc.player.addVelocity(doubles[0], 0.0, doubles[1]);
                Vec3d vec3d = mc.player.getVelocity();
                double d = this.normalizedMotion.getValue() ? vec3d.y : (flag ? 1.2 : (flag2 ? -2.0 : 0.0));
                mc.player.setVelocity(vec3d.x, d, vec3d.z);
            } else {
                if (this.mode.is("Matrix")) {
                    double[] doubles2 = MoveUtil.getMotionForInput(flag2 ? 0.32 : 0.42);
                    mc.player.addVelocity(doubles2[0], 0.0, doubles2[1]);
                    Vec3d vec3d2 = mc.player.getVelocity();
                    double d2 = this.normalizedMotion.getValue() ? vec3d2.y : (flag ? 1.2 : (flag2 ? -1.2 : 0.0));
                    mc.player.setVelocity(vec3d2.x, d2, vec3d2.z);
                }
            }
        }
    }
}
