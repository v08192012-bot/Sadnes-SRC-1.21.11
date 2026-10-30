package tech.sadnes.client.feature.impl.combat;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPosition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.Render3DEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.color.ColorUtil;
import tech.sadnes.client.util.color.Theme;

@FeatureInfo(
    name = "BackTrack",
    description = "feature.impl.combat.backtrack",
    displayName = "feature.impl.combat.backtrack",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_TWEAKS
)
public class BackTrack extends Feature {
    public final NumberSetting delayMs = new NumberSetting("option.backtrack.delay_ms", 200.0F, 10.0F, 600.0F, 10.0F);
    private Vec3d lastPosition = null;
    private Vec3d serverPosition = null;
    private final List<BackTrack.DelayedPacket> delayedPackets = new CopyOnWriteArrayList<>();

    @Override
    public void onDisable() {
        this.delayedPackets.clear();
        this.lastPosition = null;
        super.onDisable();
    }

    @Override
    public void onEnable() {
        this.releaseAll();
        this.lastPosition = null;
        super.onEnable();
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && mc.world != null && mc.player != null) {
            Packet packet = packetEvent.getPacket();
            if (this.isMovementPacket(packet)) {
                if (this.isTargetPacket(packet)) {
                    if (this.shouldDelay(packet)) {
                        Vec3d vec3d = this.getPacketPosition(packet);
                        if (vec3d != null) {
                            this.serverPosition = vec3d;
                        }

                        this.delayedPackets.add(new BackTrack.DelayedPacket(packet));
                        packetEvent.cancel();
                    }
                }
            }
        }
    }

    private boolean isMovementPacket(Packet<?> packet) {
        return packet instanceof EntityS2CPacket || packet instanceof EntityPositionS2CPacket || packet instanceof EntityPositionSyncS2CPacket;
    }

    private boolean isTargetPacket(Packet<?> packet) {
        if (mc.world == null) {
            return false;
        }

        AttackAura attackAura = AttackAura.getInstance();
        if (attackAura.target == null) {
            return false;
        }

        int i = -1;
        if (packet instanceof EntityS2CPacket entityS2CPacket) {
            Entity entity = entityS2CPacket.getEntity(mc.world);
            if (entity == null) {
                return false;
            }

            i = entity.getId();
        } else if (packet instanceof EntityPositionSyncS2CPacket) {
            i = ((EntityPositionSyncS2CPacket)packet).id();
        } else if (packet instanceof EntityPositionS2CPacket) {
            i = ((EntityPositionS2CPacket)packet).entityId();
        }

        return i == -1 ? false : attackAura.target == mc.world.getEntityById(i);
    }

    private boolean shouldDelay(Packet<?> packet) {
        if (mc.world == null) {
            return false;
        }

        AttackAura attackAura = AttackAura.getInstance();
        Vec3d vec3d = this.getPacketPosition(packet);
        if (vec3d == null) {
            return false;
        }

        if (attackAura.target == null) {
            return false;
        }

        double d = attackAura.getAttackRange();
        double d2 = mc.player.getEntityPos().distanceTo(vec3d);
        double d3 = mc.player.distanceTo(attackAura.target);
        return d3 > d && d2 < d + 1.5;
    }

    private Vec3d getPacketPosition(Packet<?> packet) {
        if (mc.world == null) {
            return null;
        }

        if (packet instanceof EntityPositionS2CPacket entityPositionS2CPacket) {
            Entity entity = mc.world.getEntityById(entityPositionS2CPacket.entityId());
            if (entity == null) {
                return null;
            }

            EntityPosition entityPosition = entityPositionS2CPacket.change();
            Set set = entityPositionS2CPacket.relatives();
            double d = set.contains(PositionFlag.X) ? entity.getX() + entityPosition.position().x : entityPosition.position().x;
            double d2 = set.contains(PositionFlag.Y) ? entity.getY() + entityPosition.position().y : entityPosition.position().y;
            double d3 = set.contains(PositionFlag.Z) ? entity.getZ() + entityPosition.position().z : entityPosition.position().z;
            return new Vec3d(d, d2, d3);
        } else if (packet instanceof EntityPositionSyncS2CPacket entityPositionSyncS2CPacket) {
            return entityPositionSyncS2CPacket.values().position();
        } else if (packet instanceof EntityS2CPacket entityS2CPacket) {
            Entity entity2 = entityS2CPacket.getEntity(mc.world);
            return entity2 == null
                ? null
                : new Vec3d(
                    entity2.lastRenderX + entityS2CPacket.getDeltaX() / 4096.0,
                    entity2.lastRenderY + entityS2CPacket.getDeltaY() / 4096.0,
                    entity2.lastRenderZ + entityS2CPacket.getDeltaZ() / 4096.0
                );
        } else {
            return null;
        }
    }

    private void releaseExpired() {
        if (mc.getNetworkHandler() != null) {
            float f = this.delayMs.getValue();
            long l = System.currentTimeMillis();
            this.delayedPackets.removeIf(delayedPacket -> {
                if ((float)(l - delayedPacket.time) >= f) {
                    this.applyPacket(delayedPacket.packet);
                    return true;
                } else {
                    return false;
                }
            });
        }
    }

    private void releaseAll() {
        for (BackTrack.DelayedPacket delayedPacket : this.delayedPackets) {
            this.applyPacket(delayedPacket.packet);
        }

        this.delayedPackets.clear();
    }

    private <T extends PacketListener> void applyPacket(Packet<T> packet) {
        if (mc.getNetworkHandler() != null) {
            try {
                packet.apply((T)mc.getNetworkHandler());
            } catch (Exception exception) {
            }
        }
    }

    private Box createBox(Entity entity, Vec3d vec3d) {
        if (entity != null && vec3d != null) {
            float f = entity.getWidth();
            float f2 = entity.getHeight();
            float f3 = f / 2.0F;
            return new Box(vec3d.x - f3, vec3d.y, vec3d.z - f3, vec3d.x + f3, vec3d.y + f2, vec3d.z + f3);
        } else {
            return null;
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent render3DEvent) {
        MatrixStack matrixStack = render3DEvent.getMatrices();
        AttackAura attackAura = AttackAura.getInstance();
        LivingEntity livingEntity = attackAura.target;
        if (livingEntity != null && this.serverPosition != null && mc.player != null) {
            double d = attackAura.getAttackRange();
            double d2 = mc.player.getEntityPos().distanceTo(this.serverPosition);
            double d3 = mc.player.distanceTo(livingEntity);
            if (d3 > d && d2 < d + 1.5) {
                Vec3d vec3d = mc.gameRenderer.getCamera().getCameraPos();
                Box box = this.createBox(livingEntity, this.serverPosition);

                try (Render3DUtil.BoxBatch boxBatch = Render3DUtil.glowBoxBatch()) {
                    int i = Theme.getAccent();
                    int i2 = ColorUtil.darken(i, 0.076F);
                    int i3 = ColorUtil.darken(i, 0.5F);
                    Box box2 = box.offset(-vec3d.x, -vec3d.y, -vec3d.z);
                    boxBatch.fill(matrixStack, box2, i2);
                    boxBatch.outline(matrixStack, box2, i3);
                    boxBatch.draw();
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.world != null && mc.player != null) {
            AttackAura attackAura = AttackAura.getInstance();
            LivingEntity livingEntity = attackAura.target;
            if (livingEntity == null) {
                this.releaseAll();
            } else {
                this.releaseExpired();
            }
        }
    }

    static class DelayedPacket {
        final Packet<?> packet;
        final long time;

        DelayedPacket(Packet<?> packet2) {
            this.packet = packet2;
            this.time = System.currentTimeMillis();
        }
    }
}
