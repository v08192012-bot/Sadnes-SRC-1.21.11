package tech.sadnes.client.feature.impl.movement;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.util.network.PacketSender;

@FeatureInfo(
    name = "BedrockClip",
    description = "feature.impl.movement.bedrockclip",
    displayName = "feature.impl.movement.bedrockclip",
    category = Category.MOVEMENT,
    subcategory = Subcategory.MOVEMENT_TERRAIN
)
public final class BedrockClip extends Feature {
    public static boolean active;
    private Entity target;

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        boolean flag = ServerManager.isNullWorld();
        Entity entity = this.target;
        if (!flag) {
            boolean flag2;
            if (entity != null) {
                Vec3d vec3d = entity.getVelocity();
                Vec3d vec3d2 = Vec3d.ZERO;
                flag2 = !vec3d.equals(vec3d2);
            } else {
                flag2 = false;
            }

            active = flag2;
            Entity entity2 = mc.player.getControllingVehicle();
            boolean flag3 = entity2 instanceof AbstractBoatEntity;
            Entity entity3 = this.target;
            if (flag3) {
                AbstractBoatEntity abstractBoatEntity = (AbstractBoatEntity)entity2;
                Vec3d vec3d3 = mc.player.getEntityPos();
                float f = abstractBoatEntity.getYaw();
                float f2 = abstractBoatEntity.getPitch();
                PacketSender.sendSilent(new VehicleMoveC2SPacket(vec3d3, f, f2, false));
                this.target = abstractBoatEntity;
                mc.options.sneakKey.setPressed(true);
                mc.options.rightKey.setPressed(true);
                return;
            }

            if (entity3 == null) {
                return;
            }

            ClientPlayerEntity clientPlayerEntity = mc.player;
            Entity entity4 = this.target;
            if (!(clientPlayerEntity.distanceTo(entity4) >= 6.0F)) {
                mc.options.sneakKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                Entity entity5 = this.target;
                EntityHitResult entityHitResult = new EntityHitResult(entity5);
                Hand hand = Hand.MAIN_HAND;
                clientPlayerInteractionManager.interactEntityAtLocation(clientPlayerEntity2, entity5, entityHitResult, hand);
                ClientPlayerInteractionManager clientPlayerInteractionManager2 = mc.interactionManager;
                ClientPlayerEntity clientPlayerEntity3 = mc.player;
                Entity entity6 = this.target;
                Hand hand2 = Hand.MAIN_HAND;
                clientPlayerInteractionManager2.interactEntity(clientPlayerEntity3, entity6, hand2);
                return;
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        active = false;
        this.target = null;
        mc.options.sneakKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
    }
}
