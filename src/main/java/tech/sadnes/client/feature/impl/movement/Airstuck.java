package tech.sadnes.client.feature.impl.movement;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.HudPostRenderEvent;
import tech.sadnes.client.event.impl.MoveEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.math.TimerUtil;
import tech.sadnes.client.util.network.PacketSender;

@FeatureInfo(
    name = "Airstuck",
    description = "feature.impl.movement.airstuck",
    displayName = "feature.impl.movement.airstuck",
    category = Category.MOVEMENT,
    subcategory = Subcategory.MOVEMENT_AIR
)
public class Airstuck extends Feature {
    TimerUtil lagTimer = new TimerUtil();
    private final StopWatch autoDisableTimer = new StopWatch();
    private final StopWatch spookyTimer = new StopWatch();
    private final ModeSetting mode = new ModeSetting("airstuck.mode", new String[]{"default", "funtime", "spookytime"});
    private final BooleanSetting alternativeMode = new BooleanSetting("airstuck.alternativeMode", false).setVisibleWhen(() -> this.mode.is("default"));
    private final BooleanSetting waitMoment = new BooleanSetting("airstuck.waitMoment", false);
    private final BooleanSetting autoDisable = new BooleanSetting("airstuck.autoDisable", false).setVisibleWhen(() -> !this.mode.is("spookytime") ? true : false);
    private final NumberSetting autoDisableTime = new NumberSetting("airstuck.autoDisableTime", 5.0F, 1.0F, 30.0F, 1.0F, Unit.SECOND)
        .setVisibleWhen(() -> !this.mode.is("spookytime") && this.autoDisable.getValue() ? true : false);
    private final CopyOnWriteArrayList<Packet<?>> bufferedPackets = new CopyOnWriteArrayList<>();
    Vec3d savedVelocity = new Vec3d(0.0, 0.0, 0.0);
    Vec3d savedPosition = new Vec3d(0.0, 0.0, 0.0);
    private float savedYaw;
    private float savedPitch;
    private float savedBodyYaw;
    private boolean stateSaved;
    private boolean releasing;

    public static Airstuck getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(Airstuck.class);
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.lagTimer.setDelay(67L);
        this.flushPackets();
        this.toggle();
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (mc.player != null && mc.world != null) {
            if (!this.waitMoment.getValue() || mc.player == null || !(mc.player.fallDistance < 0.1)) {
                if (!this.mode.is("spookytime") || !this.releasing) {
                    if (this.mode.is("funtime")) {
                        if (packetEvent.isSend()) {
                            this.bufferedPackets.add(packetEvent.getPacket());
                            packetEvent.cancel();
                            return;
                        }
                    } else if (packetEvent.getPacket() instanceof PlayerMoveC2SPacket) {
                        packetEvent.cancel();
                    }
                }
            }
        } else {
            if (this.enabled()) {
                this.toggle();
            }
        }
    }

    @EventHandler
    public void onUpdate(HudPostRenderEvent hudPostRenderEvent) {
        if (!this.lagTimer.isReached()) {
            this.setEnabled(false);
        }

        if (this.autoDisable.getValue() && !this.mode.is("spookytime") && this.autoDisableTimer.isElapsed((long)(this.autoDisableTime.getValue() * 1000.0F))) {
            this.setEnabled(false);
        }

        if (this.mode.is("spookytime")) {
            if (!this.releasing) {
                if (this.spookyTimer.isElapsed(2500L)) {
                    this.flushPackets();
                    this.stateSaved = false;
                    if (this.savedPosition != null && mc.player != null) {
                        mc.player.setMovement(mc.player.isOnGround(), this.savedPosition);
                    }

                    if (this.savedVelocity != null && mc.player != null) {
                        mc.player.setVelocity(this.savedVelocity);
                    }

                    this.releasing = true;
                    this.spookyTimer.reset();
                    return;
                }
            } else if (this.spookyTimer.isElapsed(150L)) {
                this.releasing = false;
                this.spookyTimer.reset();
            }
        }
    }

    @EventHandler
    public void onMove(MoveEvent moveEvent) {
        if (mc.player != null) {
            if (!this.waitMoment.getValue() || !(mc.player.fallDistance < 0.1)) {
                if (!this.mode.is("spookytime") || !this.releasing) {
                    this.saveState();
                    moveEvent.setX(0.0);
                    moveEvent.setY(0.0);
                    moveEvent.setZ(0.0);
                    moveEvent.setCancelled(true);
                    if (this.mode.is("default") && this.alternativeMode.getValue() && mc.player.age % 10 == 0) {
                        mc.player.networkHandler.sendPacket(new OnGroundOnly(mc.player.isOnGround(), mc.player.horizontalCollision));
                    }

                    mc.player.setVelocity(Vec3d.ZERO);
                }
            }
        }
    }

    public boolean isStateSaved() {
        return this.stateSaved;
    }

    public float getSavedPitch() {
        return this.savedPitch;
    }

    public float getSavedYaw() {
        return this.savedYaw;
    }

    public float getSavedBodyYaw() {
        return this.savedBodyYaw;
    }

    private void saveState() {
        boolean flag = this.stateSaved;
        float f = this.savedBodyYaw;
        float f2;
        if (!flag) {
            this.stateSaved = true;
            Vec3d vec3d = mc.player.getMovement();
            this.savedPosition = vec3d;
            ClientPlayerEntity clientPlayerEntity = mc.player;
            boolean flag2 = mc.player.isOnGround();
            Vec3d vec3d2 = Vec3d.ZERO;
            clientPlayerEntity.setMovement(flag2, vec3d2);
            double d = mc.player.getVelocity().x;
            double d2 = mc.player.getVelocity().y;
            double d3 = mc.player.getVelocity().z;
            Vec3d vec3d3 = new Vec3d(d, d2, d3);
            this.savedVelocity = vec3d3;
            ClientPlayerEntity clientPlayerEntity2 = mc.player;
            Vec3d vec3d4 = Vec3d.ZERO;
            clientPlayerEntity2.setVelocity(vec3d4);
            float f3 = mc.player.getYaw();
            this.savedYaw = f3;
            float f4 = mc.player.getPitch();
            this.savedPitch = f4;
            f2 = mc.player.bodyYaw;
        } else {
            f2 = f;
        }

        this.savedBodyYaw = f2;
    }

    private void flushPackets() {
        ClientPlayNetworkHandler clientPlayNetworkHandler = mc.getNetworkHandler();
        CopyOnWriteArrayList copyOnWriteArrayList = this.bufferedPackets;
        if (clientPlayNetworkHandler == null) {
            copyOnWriteArrayList.clear();
        } else {
            Iterator iterator2 = copyOnWriteArrayList.iterator();
            if (!iterator2.hasNext()) {
                copyOnWriteArrayList.clear();
            } else {
                do {
                    PacketSender.sendSilent((Packet<?>)iterator2.next());
                } while (iterator2.hasNext());

                copyOnWriteArrayList.clear();
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        CopyOnWriteArrayList copyOnWriteArrayList = this.bufferedPackets;
        this.stateSaved = false;
        this.releasing = false;
        copyOnWriteArrayList.clear();
        this.autoDisableTimer.reset();
        this.spookyTimer.reset();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.stateSaved = false;
        this.releasing = false;
        this.flushPackets();
        boolean flag = this.stateSaved;
        boolean flag2 = this.releasing;
        Vec3d vec3d = this.savedPosition;
        Vec3d vec3d2 = this.savedVelocity;
        long l;
        boolean flag3;
        boolean flag4;
        if (vec3d != null) {
            ((Long)(Object)vec3d2).longValue();
            this.stateSaved = flag;
            this.releasing = flag2;
            ClientPlayerEntity clientPlayerEntity = mc.player;
            boolean flag5 = this.stateSaved;
            boolean flag6 = this.releasing;
            Vec3d vec3d3 = this.savedVelocity;
            if (clientPlayerEntity != null) {
                ((Long)(Object)vec3d3).longValue();
                this.stateSaved = flag5;
                this.releasing = flag6;
                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                boolean flag7 = mc.player.isOnGround();
                Vec3d vec3d4 = this.savedPosition;
                clientPlayerEntity2.setMovement(flag7, vec3d4);
                boolean flag8 = this.stateSaved;
                boolean flag9 = this.releasing;
                l = ((Long)(Object)this.savedVelocity).longValue();
                flag3 = flag8;
                flag4 = flag9;
            } else {
                l = ((Long)(Object)vec3d3).longValue();
                flag3 = flag5;
                flag4 = flag6;
            }
        } else {
            l = ((Long)(Object)vec3d2).longValue();
            flag3 = flag;
            flag4 = flag2;
        }

        if (l != 0L) {
            this.stateSaved = flag3;
            this.releasing = flag4;
            ClientPlayerEntity clientPlayerEntity3 = mc.player;
            boolean flag10 = this.stateSaved;
            boolean flag11 = this.releasing;
            Vec3d vec3d5 = this.savedVelocity;
            if (clientPlayerEntity3 == null) {
                ((Long)(Object)vec3d5).longValue();
                this.stateSaved = flag10;
                this.releasing = flag11;
                return;
            }

            ((Long)(Object)vec3d5).longValue();
            this.stateSaved = flag10;
            this.releasing = flag11;
            ClientPlayerEntity clientPlayerEntity4 = mc.player;
            Vec3d vec3d6 = this.savedVelocity;
            clientPlayerEntity4.setVelocity(vec3d6);
            boolean flag12 = this.stateSaved;
            boolean flag13 = this.releasing;
            ((Long)(Object)this.savedVelocity).longValue();
            flag3 = flag12;
            flag4 = flag13;
        }

        this.stateSaved = flag3;
        this.releasing = flag4;
    }
}
