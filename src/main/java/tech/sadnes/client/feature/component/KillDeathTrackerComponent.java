package tech.sadnes.client.feature.component;

import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.config.KdStats;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.EntityRemoveEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.FeatureComponent;

public final class KillDeathTrackerComponent extends FeatureComponent {
    private PlayerEntity lastAttacked;
    private boolean dead;

    public static KillDeathTrackerComponent getInstance() {
        return Sadnes.getInstance().getComponentManager().getComponent(KillDeathTrackerComponent.class);
    }

    @EventHandler
    public void onAttack(AttackEntityEvent attackEntityEvent) {
        if (attackEntityEvent.getTarget() instanceof PlayerEntity playerEntity) {
            this.lastAttacked = playerEntity;
        }
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent entityRemoveEvent) {
        if (mc.player != null && this.lastAttacked != null && entityRemoveEvent.getEntity() == this.lastAttacked) {
            if (entityRemoveEvent.getReason() == RemovalReason.KILLED || this.lastAttacked.getHealth() <= 0.0F || this.lastAttacked.isDead()) {
                String string = this.getServerAddress();
                if (string != null) {
                    KdStats.addKill(string);
                }

                this.lastAttacked = null;
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && packetEvent.getPacket() instanceof HealthUpdateS2CPacket healthUpdateS2CPacket) {
            if (healthUpdateS2CPacket.getHealth() > 0.0F) {
                this.dead = false;
            } else if (!this.dead) {
                this.dead = true;
                String string = this.getServerAddress();
                if (string != null) {
                    KdStats.addDeath(string);
                }

                this.lastAttacked = null;
            }
        }
    }

    public String getKdRatioText() {
        String string = this.getServerAddress();
        if (string == null) {
            return "0.0";
        }

        double d = KdStats.getKdRatio(string);
        return String.format("%.2f", d).replace(',', '.');
    }

    private String getServerAddress() {
        if (mc.isInSingleplayer()) {
            return null;
        }

        ServerInfo serverInfo = mc.getCurrentServerEntry();
        return serverInfo != null ? serverInfo.address : null;
    }
}
