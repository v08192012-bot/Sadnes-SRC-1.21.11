package tech.sadnes.client.feature.impl.player;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.deobf.ExactMath;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.util.math.TimerUtil;

@FeatureInfo(
    name = "KTLeave",
    description = "feature.impl.player.ktleave",
    displayName = "feature.impl.player.ktleave",
    category = Category.MISC,
    subcategory = Subcategory.MISC_SAFETY
)
public final class KTLeave extends Feature {
    private final ModeSetting mode = new ModeSetting("ktleave.mode", new String[]{"kick", "damage_spoof"});
    private final BooleanSetting additionalBypass = new BooleanSetting("ktleave.additionalBypass", false).setVisibleWhen(() -> this.mode.is("damage_spoof"));
    private final TimerUtil leaveTimer = new TimerUtil();
    private boolean leaving;

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        ModeSetting modeSetting = this.mode;
        if (clientPlayerEntity != null) {
            boolean flag = modeSetting.is("damage_spoof");
            BooleanSetting booleanSetting = this.additionalBypass;
            TimerUtil timerUtil = this.leaveTimer;
            if (flag) {
                if (booleanSetting.getValue()) {
                    double d = mc.player.getX();
                    double d2 = mc.player.getY();
                    double d3 = mc.player.getZ();
                    boolean flag2 = this.leaving;
                    double d4 = ExactMath.add(d2, 0.096);
                    int i = 350;

                    while (true) {
                        this.leaving = flag2;
                        ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
                        PositionAndOnGround positionAndOnGround = new PositionAndOnGround(d, d4, d3, false, false);
                        clientPlayNetworkHandler.sendPacket(positionAndOnGround);
                        ClientPlayNetworkHandler clientPlayNetworkHandler2 = mc.player.networkHandler;
                        PositionAndOnGround positionAndOnGround2 = new PositionAndOnGround(d, d2, d3, false, false);
                        clientPlayNetworkHandler2.sendPacket(positionAndOnGround2);
                        flag2 = this.leaving;
                        int i2 = i - 1;
                        if (i - 1 == 0) {
                            ClientPlayNetworkHandler clientPlayNetworkHandler3 = mc.player.networkHandler;
                            PositionAndOnGround positionAndOnGround3 = new PositionAndOnGround(d, d2, d3, true, false);
                            clientPlayNetworkHandler3.sendPacket(positionAndOnGround3);
                            break;
                        }

                        i = i2;
                    }
                }

                ClientPlayNetworkHandler clientPlayNetworkHandler4 = mc.player.networkHandler;
                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                boolean flag3 = mc.player.isSneaking();
                PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket = PlayerInteractEntityC2SPacket.attack(clientPlayerEntity2, flag3);
                clientPlayNetworkHandler4.sendPacket(playerInteractEntityC2SPacket);
                this.setEnabled(false);
                return;
            }

            boolean flag4 = timerUtil.isReached();
            boolean flag5 = this.leaving;
            if (!flag4) {
                this.leaving = true;
                Sadnes.guiScale = 20.0F;
                return;
            }

            if (!flag5) {
                return;
            }
        }

        this.setEnabled(false);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.leaveTimer.setDelay(450L);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (this.leaving) {
            Sadnes.guiScale = 1.0F;
        }

        this.leaving = false;
    }
}
