package tech.sadnes.client.feature.impl.player;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.render.world.Render3DUtil;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.ColorSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.math.TimerUtil;
import tech.sadnes.client.util.network.PacketSender;

@FeatureInfo(
    name = "PingSpike",
    description = "feature.impl.player.pingspike",
    displayName = "feature.impl.player.pingspike",
    category = Category.PLAYER,
    subcategory = Subcategory.PLAYER_TWEAKS
)
public final class PingSpike extends Feature {
    private final ModeSetting mode = new ModeSetting("pingspike.mode", new String[]{"leave", "pulse", "toggle"});
    private final NumberSetting delay = new NumberSetting("pingspike.delay", 500.0F, 100.0F, 1000.0F, 10.0F, Unit.MILLISECOND)
        .setVisibleWhen(() -> this.mode.is("pulse"));
    private final ColorSetting color = new ColorSetting("pingspike.color", -50384);
    private final TimerUtil pulseTimer = new TimerUtil();
    private final CopyOnWriteArrayList<Packet<?>> heldPackets = new CopyOnWriteArrayList<>();
    private Vec3d serverPos = Vec3d.ZERO;
    private long startTime;
    private boolean playerNearby;

    public static PingSpike getInstance() {
        return Sadnes.getInstance().getFeatureManager().getFeature(PingSpike.class);
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (mc.player == null || mc.world == null || mc.isInSingleplayer()) {
            this.setEnabled(false);
        } else if (mc.player.isDead()) {
            this.setEnabled(false);
        } else {
            boolean flag = packetEvent.isSend();
            CopyOnWriteArrayList copyOnWriteArrayList = this.heldPackets;
            if (flag) {
                Packet packet = packetEvent.getPacket();
                copyOnWriteArrayList.add(packet);
                packetEvent.cancel();
            }
        }
    }

    @EventHandler
    public void onWorldChange(GameJoinEvent gameJoinEvent) {
        this.flushPackets();
        this.setEnabled(false);
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player != null && mc.world != null) {
            Iterator iterator2 = mc.world.getPlayers().iterator();
            if (iterator2.hasNext()) {
                do {
                    PlayerEntity playerEntity = (PlayerEntity)iterator2.next();
                    ClientPlayerEntity clientPlayerEntity = mc.player;
                    Vec3d vec3d = this.serverPos;
                    boolean flag = this.playerNearby;
                    if (playerEntity != clientPlayerEntity) {
                        double d = playerEntity.squaredDistanceTo(vec3d);
                        flag = this.playerNearby;
                        if (25.0 >= d) {
                            flag = true;
                        }
                    }

                    this.playerNearby = flag;
                } while (iterator2.hasNext());
            }

            long l = System.currentTimeMillis();
            long l2 = this.startTime;
            ModeSetting modeSetting = this.mode;
            if (l - l2 >= 29900L) {
                this.setEnabled(false);
                return;
            }

            boolean flag2 = modeSetting.is("pulse");
            TimerUtil timerUtil = this.pulseTimer;
            NumberSetting numberSetting = this.delay;
            if (flag2) {
                double d2 = numberSetting.getValue().floatValue();
                if (timerUtil.hasPassed(d2) || AttackAura.getInstance().canAttack()) {
                    this.pulse();
                    return;
                }
            } else if (!modeSetting.is("toggle")) {
                int i;
                if (ServerManager.isServer("spookytime")) {
                    i = 1080164352;
                } else if (ServerManager.isServer("dildo")) {
                    i = 1080164352;
                } else {
                    i = 1078067200;
                }

                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                Vec3d vec3d2 = this.serverPos;
                double d3 = clientPlayerEntity2.squaredDistanceTo(vec3d2);
                boolean flag3 = this.playerNearby;
                double d4 = Double.longBitsToDouble(0L | (long)i << 32);
                if (!(d3 >= d4)) {
                    this.playerNearby = flag3;
                    boolean flag4 = timerUtil.hasPassed(4000.0);
                    boolean flag5 = this.playerNearby;
                    if (!flag4 && !flag5) {
                        return;
                    }
                }

                this.playerNearby = false;
                this.pulse();
                return;
            }
        }
    }

    @EventHandler
    public void onRender(RenderEvent renderEvent) {
        if (renderEvent.isWorld() && mc.player != null && mc.world != null) {
            Box box = mc.player.getBoundingBox();
            double d = box.maxX;
            double d2 = box.minX;
            double d3 = box.maxY;
            double d4 = box.minY;
            double d5 = this.serverPos.x;
            double d6 = this.serverPos.y;
            double d7 = this.serverPos.z;
            double d8 = this.serverPos.x;
            double d9 = this.serverPos.y;
            double d10 = this.serverPos.z;
            double d11 = ExactMath.multiply(ExactMath.subtract(d, d2), 0.5);
            double d12 = ExactMath.subtract(d5, d11);
            double d13 = ExactMath.subtract(d7, d11);
            double d14 = ExactMath.add(d8, d11);
            double d15 = ExactMath.add(ExactMath.subtract(d3, d4), d9);
            double d16 = ExactMath.add(d11, d10);
            Box box2 = new Box(d12, d6, d13, d14, d15, d16);
            int i = this.color.getValue();
            MatrixStack matrixStack = renderEvent.getMatrices();
            int i2 = MathHelper.clamp(Math.round(45.9F), 0, 255);
            int i3 = MathHelper.clamp(i >>> 16, 0, 255);
            int i4 = MathHelper.clamp(i >>> 8, 0, 255);
            int i5 = MathHelper.clamp(i, 0, 255);
            int i6 = i4 << 8;
            int i7 = i3 << 16;
            int i8 = i2 << 24;
            int i9 = i7 | i8;
            int i10 = i6 | i9 | i5;
            Render3DUtil.drawGlowBox(matrixStack, box2, i, i10);
        }
    }

    private void pulse() {
        this.flushPackets();
        double d = mc.player.getX();
        double d2 = mc.player.getY();
        double d3 = mc.player.getZ();
        Vec3d vec3d = new Vec3d(d, d2, d3);
        this.serverPos = vec3d;
        long l = System.currentTimeMillis();
        TimerUtil timerUtil = this.pulseTimer;
        this.startTime = l;
        timerUtil.reset();
    }

    private void flushPackets() {
        ClientPlayNetworkHandler clientPlayNetworkHandler = mc.getNetworkHandler();
        CopyOnWriteArrayList copyOnWriteArrayList = this.heldPackets;
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
        long l = System.currentTimeMillis();
        this.startTime = l;
        Vec3d vec3d;
        TimerUtil timerUtil;
        if (mc.player != null) {
            double d = mc.player.getX();
            double d2 = mc.player.getY();
            double d3 = mc.player.getZ();
            Vec3d vec3d2 = new Vec3d(d, d2, d3);
            TimerUtil timerUtil2 = this.pulseTimer;
            vec3d = vec3d2;
            timerUtil = timerUtil2;
        } else {
            Vec3d vec3d3 = Vec3d.ZERO;
            TimerUtil timerUtil3 = this.pulseTimer;
            vec3d = vec3d3;
            timerUtil = timerUtil3;
        }

        this.serverPos = vec3d;
        timerUtil.reset();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.flushPackets();
    }
}
