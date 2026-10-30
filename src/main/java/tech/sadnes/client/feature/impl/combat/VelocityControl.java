package tech.sadnes.client.feature.impl.combat;

import java.util.Locale;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.network.DeferredPacketQueue;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.MoveUtil;

@FeatureInfo(
    name = "VelocityControl",
    description = "feature.impl.combat.velocitycontrol",
    displayName = "feature.impl.combat.velocitycontrol",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_TWEAKS
)
public final class VelocityControl extends Feature {
    private static final int MAX_TICKS = 20;
    private final MultiSetting cancel = new MultiSetting(
        "velocitycontrol.cancel",
        BooleanSetting.of("from_hits", true),
        BooleanSetting.of("from_explosions", false),
        BooleanSetting.of("from_wind_charge", false)
    );
    private final ModeSetting cancelType = new ModeSetting("velocitycontrol.canceltype", new String[]{"normal", "riding_spoof", "grim_standing", "jump_reset"})
        .setVisibleWhen(() -> this.cancel.has("from_hits"));
    private final NumberSetting standingDelay = new NumberSetting("velocitycontrol.standingDelay", 0.0F, 0.0F, 10.0F, 1.0F)
        .setVisibleWhen(() -> this.cancel.has("from_hits") && this.cancelType.is("grim_standing") ? true : false);
    private final NumberSetting pauseOnFlag = new NumberSetting("velocitycontrol.pauseOnFlag", 2.0F, 0.0F, 10.0F, 0.5F)
        .setVisibleWhen(() -> this.cancel.has("from_hits") && this.cancelType.is("grim_standing") ? true : false);
    private volatile boolean damageReceived;
    private volatile boolean delayingPackets;
    private volatile boolean waitingToPlace;
    private volatile boolean awaitingPong;
    private volatile boolean placing;
    private volatile boolean interactedThisTick;
    private volatile boolean pendingJump;
    private volatile boolean jumpResetActive;
    private boolean leftGround;
    private volatile Vec3d knockbackDirection;
    private Vec3d knockbackOrigin;
    private int jumpResetTicks;
    private volatile int placeDelayTicks;
    private volatile int blockUpdateTicks = -1;
    private volatile int pongTicks = -1;
    private volatile BlockPos placedPos;
    private volatile long flagPauseUntil;
    private int placingTicks;
    private BlockHitResult pendingPlaceHit;

    public static VelocityControl getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager == null ? null : featureManager.getFeature(VelocityControl.class);
    }

    public boolean cancelExplosions() {
        return this.cancel.has("from_explosions");
    }

    public boolean cancelWindCharge() {
        return this.cancel.has("from_wind_charge");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.resetState();
        DeferredPacketQueue.flush();
    }

    @EventHandler
    public void onPacket(PacketEvent packetEvent) {
        if (mc.player != null) {
            Packet packet = packetEvent.getPacket();
            if (this.cancel.has("from_hits")) {
                if (!this.cancelType.is("grim_standing")) {
                    if (packet instanceof EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket && entityVelocityUpdateS2CPacket.getEntityId() == mc.player.getId()) {
                        if (this.cancelType.is("normal")) {
                            packetEvent.cancel();
                            return;
                        }

                        if (this.cancelType.is("riding_spoof")) {
                            packetEvent.cancel();
                            mc.player
                                .networkHandler
                                .sendPacket(new PositionAndOnGround(mc.player.getX(), -999.0, mc.player.getZ(), true, mc.player.horizontalCollision));
                            return;
                        }

                        if (this.cancelType.is("jump_reset") && mc.player.isOnGround()) {
                            Vec3d vec3d = entityVelocityUpdateS2CPacket.getVelocity();
                            double d = Math.hypot(vec3d.x, vec3d.z);
                            if (d > 1.0E-4) {
                                this.knockbackDirection = new Vec3d(vec3d.x / d, 0.0, vec3d.z / d);
                                this.knockbackOrigin = new Vec3d(mc.player.getX(), 0.0, mc.player.getZ());
                                this.pendingJump = true;
                                this.jumpResetActive = true;
                                this.leftGround = false;
                                this.jumpResetTicks = 0;
                            }
                        }
                    }
                } else {
                    if (packetEvent.isReceive() && packet instanceof GameMessageS2CPacket gameMessageS2CPacket) {
                        String string = gameMessageS2CPacket.content().getString().toLowerCase(Locale.ROOT);
                        if (string.contains("grim") && (string.contains("fail") || string.contains("flag"))) {
                            this.flagPauseUntil = System.currentTimeMillis() + (long)(this.pauseOnFlag.getValue() * 1000.0F);
                        }
                    }

                    if (packetEvent.isSend() && (packet instanceof PlayerInteractEntityC2SPacket || packet instanceof PlayerInteractBlockC2SPacket)) {
                        this.interactedThisTick = true;
                    }

                    if (packetEvent.isSend() && this.placing && (packet instanceof Full || packet instanceof PositionAndOnGround)) {
                        packetEvent.cancel();
                    }

                    if (packetEvent.isSend() && this.awaitingPong && packet instanceof CommonPongC2SPacket) {
                        this.pongTicks = 1;
                    }

                    if (!packetEvent.isCancelled()) {
                        if (packetEvent.isReceive()
                            && packet instanceof BlockUpdateS2CPacket blockUpdateS2CPacket
                            && this.placing
                            && this.placedPos != null
                            && blockUpdateS2CPacket.getPos().equals(this.placedPos)) {
                            this.blockUpdateTicks = 1;
                            this.waitingToPlace = false;
                        } else if (!this.placing && !this.delayingPackets) {
                            if (packetEvent.isReceive()) {
                                if (System.currentTimeMillis() < this.flagPauseUntil) {
                                    if (this.isSelfVelocityPacket(packet)) {
                                        packetEvent.cancel();
                                    }
                                } else if (this.isSelfDamagePacket(packet)) {
                                    this.damageReceived = true;
                                } else {
                                    if (this.damageReceived && this.isSelfVelocityPacket(packet)) {
                                        packetEvent.cancel();
                                        this.delayingPackets = true;
                                        this.damageReceived = false;
                                        this.waitingToPlace = true;
                                        this.placeDelayTicks = Math.round(this.standingDelay.getValue());
                                    }
                                }
                            }
                        } else {
                            if (this.delayingPackets && !this.placing && packetEvent.isReceive()) {
                                DeferredPacketQueue.enqueue(packet);
                                packetEvent.cancel();
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent playerTickEvent) {
        if (mc.player != null && mc.world != null && mc.interactionManager != null) {
            if (this.cancel.has("from_hits") && this.cancelType.is("jump_reset")) {
                if (this.pendingJump && mc.player.isOnGround()) {
                    ThrowComponent.rotationTimer.setDelay(200L);
                    this.pendingJump = false;
                }

                if (this.jumpResetActive) {
                    if (!mc.player.isOnGround()) {
                        this.leftGround = true;
                    }

                    double d = (mc.player.getX() - this.knockbackOrigin.x) * this.knockbackDirection.x
                        + (mc.player.getZ() - this.knockbackOrigin.z) * this.knockbackDirection.z;
                    boolean flag = this.leftGround && d <= 0.0;
                    if (this.leftGround && mc.player.isOnGround()) {
                        this.jumpResetActive = false;
                        this.leftGround = false;
                    } else if (flag || ++this.jumpResetTicks > 20) {
                        this.jumpResetActive = false;
                        this.leftGround = false;
                    }
                }
            } else if (this.cancel.has("from_hits") && this.cancelType.is("grim_standing")) {
                if (this.blockUpdateTicks > 0 && --this.blockUpdateTicks == 0) {
                    this.awaitingPong = true;
                    this.waitingToPlace = false;
                }

                if (this.pongTicks > 0 && --this.pongTicks == 0) {
                    this.placing = false;
                    this.awaitingPong = false;
                }

                if (this.waitingToPlace && !this.interactedThisTick && !mc.player.isUsingItem()) {
                    if (this.placeDelayTicks > 0) {
                        this.placeDelayTicks--;
                    } else {
                        this.pendingPlaceHit = this.findPlaceTarget();
                    }
                }

                if (this.pendingPlaceHit != null) {
                    this.delayingPackets = false;
                    DeferredPacketQueue.flush();
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, this.pendingPlaceHit);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    PacketSender.sendSilent(new LookAndOnGround(mc.player.getYaw(), 90.0F, mc.player.isOnGround(), mc.player.horizontalCollision));
                    this.placedPos = mc.player.getBlockPos();
                    this.placingTicks = 0;
                    this.placing = true;
                    this.pendingPlaceHit = null;
                    this.waitingToPlace = false;
                }

                if (this.placing) {
                    playerTickEvent.setCancelled(true);
                    if (++this.placingTicks > 20) {
                        this.placing = false;
                        this.awaitingPong = false;
                        this.waitingToPlace = false;
                    }
                }

                this.interactedThisTick = false;
            } else {
                this.resetState();
            }
        }
    }

    @EventHandler(priority = 0)
    public void onMovementInput(MoveInputEvent moveInputEvent) {
        if (mc.player != null && this.jumpResetActive) {
            if (this.cancel.has("from_hits") && this.cancelType.is("jump_reset")) {
                double d = Math.toDegrees(Math.atan2(this.knockbackDirection.x, -this.knockbackDirection.z));
                float f = 0.0F;
                float f2 = 0.0F;
                double d2 = Double.MAX_VALUE;

                for (float f3 = -1.0F; f3 <= 1.0F; f3++) {
                    for (float f4 = -1.0F; f4 <= 1.0F; f4++) {
                        if (f3 != 0.0F || f4 != 0.0F) {
                            double d3 = Math.toDegrees(MoveUtil.getDirectionFromInput(mc.player.getYaw(), f3, f4));
                            double d4 = Math.abs(MathHelper.wrapDegrees(d - d3));
                            if (d4 < d2) {
                                d2 = d4;
                                f = f3;
                                f2 = f4;
                            }
                        }
                    }
                }

                moveInputEvent.setForward(f);
                moveInputEvent.setStrafe(f2);
            } else {
                this.jumpResetActive = false;
            }
        }
    }

    private BlockHitResult findPlaceTarget() {
        Vec3d vec3d = mc.player.getEyePos();
        BlockHitResult blockHitResult = mc.world.raycast(new RaycastContext(vec3d, vec3d.add(0.0, -3.0, 0.0), ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
        if (blockHitResult.getType() != Type.BLOCK) {
            return null;
        } else {
            return !blockHitResult.getBlockPos().offset(blockHitResult.getSide()).equals(mc.player.getBlockPos()) ? null : blockHitResult;
        }
    }

    private boolean isSelfVelocityPacket(Packet<?> packet) {
        return packet instanceof EntityVelocityUpdateS2CPacket && ((EntityVelocityUpdateS2CPacket)packet).getEntityId() == mc.player.getId();
    }

    private boolean isSelfDamagePacket(Packet<?> packet) {
        return packet instanceof EntityDamageS2CPacket && ((EntityDamageS2CPacket)packet).entityId() == mc.player.getId();
    }

    private void resetState() {
        this.damageReceived = false;
        this.delayingPackets = false;
        this.waitingToPlace = false;
        this.awaitingPong = false;
        this.placing = false;
        this.interactedThisTick = false;
        this.pendingJump = false;
        this.jumpResetActive = false;
        this.leftGround = false;
        this.placeDelayTicks = 0;
        this.blockUpdateTicks = -1;
        this.pongTicks = -1;
        this.placingTicks = 0;
        this.pendingPlaceHit = null;
        this.placedPos = null;
    }
}
