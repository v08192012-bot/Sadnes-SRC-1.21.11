package tech.sadnes.client.feature.impl.movement;

import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.feature.component.MovementPauseComponent;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.IMinecraft;
import tech.sadnes.client.util.TickQueue;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.MovementSimulator;
import tech.sadnes.client.util.rotation.RotationUtil;

final class DamagerFlightHelper implements IMinecraft {
    private final String featureName;
    private final BooleanSetting notifyEnabled;
    private final TickQueue actionQueue = new TickQueue();
    private final StopWatch placeTimer = new StopWatch();
    private final AtomicLong lastPlaceTime = new AtomicLong();
    private boolean slotSwapped;

    void tick() {
        if ("FunTime".equals(ServerManager.getServerType())) {
            BlockPos blockPos = mc.player.getBlockPos().down();
            Box box = new Box(blockPos).contract(0.01);
            if (mc.world.getBlockState(blockPos).getOutlineShape(mc.world, BlockPos.ORIGIN).isEmpty()
                && !mc.player.getBoundingBox().intersects(box)
                && !mc.player.getBoundingBox().contains(blockPos.getX(), blockPos.getY(), blockPos.getZ())
                && mc.world.getOtherEntities(null, box).isEmpty()) {
                if (!MovementSimulator.isRecentlyReset()) {
                    boolean flag = BlockPos.ofFloored(MovementSimulator.simulate(2).getPosition()).getY() <= blockPos.getY();
                    boolean flag2;
                    if (this.actionQueue.isEmpty()) {
                        flag2 = MovementPauseComponent.hasPendingTasks();
                    } else {
                        flag2 = false;
                    }

                    if (flag && flag2 && this.placeTimer.hasPassedAndReset(2000.0)) {
                        int i = InventoryUtil.findHotbarIndex(Items.JIGSAW);
                        if (i != -1) {
                            Vec3d vec3d = blockPos.toCenterPos().add(0.0, 0.5, 0.0);
                            RotationComponent.rotate(new Rotation(RotationUtil.getRotationsTo(vec3d)), 999.0F, 999.0F, 999.0F, 999.0F, 2, 12, false);
                            this.actionQueue.clear().then(() -> {
                                this.slotSwapped = true;
                                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
                                this.sendPlace(blockPos, vec3d);
                                mc.world.setBlockState(blockPos, Blocks.JIGSAW.getDefaultState());
                                this.sendPlace(blockPos, vec3d);
                                this.lastPlaceTime.set(System.currentTimeMillis());
                            }).then(this::restoreSelectedSlot);
                            return;
                        }

                        if (this.notifyEnabled.getValue()) {
                            NotificationUtil.notify(
                                NotificationType.WARNING,
                                this.featureName,
                                Items.JIGSAW.getDefaultStack(),
                                Language.translate("flight.damager.missing", "Нет блока-дамагера в быстром доступе")
                            );
                            return;
                        }
                    }
                }
            }
        }
    }

    void processQueue() {
        this.actionQueue.tick().resetIfEmpty();
    }

    void onPacket(PacketEvent packetEvent) {
        if (packetEvent.isReceive() && packetEvent.getPacket() instanceof OpenScreenS2CPacket openScreenS2CPacket) {
            if (openScreenS2CPacket.getName().getString().toLowerCase().contains("дамагер")) {
                long l = this.lastPlaceTime.getAndSet(0L);
                if (l != 0L && System.currentTimeMillis() - l <= 2000L) {
                    int i = openScreenS2CPacket.getSyncId();
                    ClientPlayNetworkHandler clientPlayNetworkHandler = mc.getNetworkHandler();
                    packetEvent.cancel();
                    mc.execute(() -> this.closeDamagerScreen(i, clientPlayNetworkHandler));
                }
            }
        }
    }

    void reset() {
        this.actionQueue.clear();
        this.placeTimer.setElapsed(2000L);
        this.slotSwapped = false;
        this.lastPlaceTime.set(0L);
    }

    void cancel() {
        this.actionQueue.clear();
        this.restoreSelectedSlot();
        this.lastPlaceTime.set(0L);
    }

    private void closeDamagerScreen(int i, ClientPlayNetworkHandler clientPlayNetworkHandler) {
        if (mc.player != null && mc.getNetworkHandler() == clientPlayNetworkHandler) {
            MovementPauseComponent.queue(() -> {
                if (mc.player != null && mc.getNetworkHandler() == clientPlayNetworkHandler) {
                    InventoryUtil.sendClickPacket(i, 14, 0, SlotActionType.QUICK_MOVE, Items.BARRIER.getDefaultStack());
                    clientPlayNetworkHandler.sendPacket(new CloseHandledScreenC2SPacket(i));
                    this.placeTimer.setElapsed(2000L);
                }
            });
        }
    }

    private void restoreSelectedSlot() {
        if (this.slotSwapped && mc.player != null) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(InventoryUtil.getSelectedSlot()));
            this.slotSwapped = false;
        }
    }

    private void sendPlace(BlockPos blockPos, Vec3d vec3d) {
        BlockHitResult blockHitResult = new BlockHitResult(vec3d, Direction.UP, blockPos, false);
        PacketSender.sendSequenced(i -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHitResult, i));
    }

    public DamagerFlightHelper(String string, BooleanSetting booleanSetting) {
        this.featureName = string;
        this.notifyEnabled = booleanSetting;
    }
}
