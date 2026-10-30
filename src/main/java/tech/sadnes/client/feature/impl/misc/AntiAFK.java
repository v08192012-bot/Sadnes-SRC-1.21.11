package tech.sadnes.client.feature.impl.misc;

import net.minecraft.client.option.InactivityFpsLimiter;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Util;
import tech.sadnes.api.mixin.accessor.InactivityFpsLimiterAccessor;
import tech.sadnes.api.mixin.client.MinecraftClientAccessor;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.util.ActionSequence;
import tech.sadnes.client.util.math.TimerUtil;

@FeatureInfo(name = "AntiAFK", displayName = "feature.impl.misc.antiafk", category = Category.MISC, subcategory = Subcategory.MISC_SERVER)
public final class AntiAFK extends Feature {
    private final ModeSetting afkBypassMode = new ModeSetting("antiafk.afkBypassMode", new String[]{"funtime", "default"}).setValue("default");
    private final NumberSetting afkSeconds = new NumberSetting("antiafk.afkSeconds", 60.0F, 5.0F, 600.0F, 1.0F, Unit.SECOND);
    TimerUtil actionTimer = new TimerUtil();
    private final ActionSequence scheduler = new ActionSequence();

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!ServerManager.isNullWorld()) {
            this.scheduler.tick();
            if (this.isAfk() && this.actionTimer.hasPassed(10000L)) {
                this.actionTimer.reset();
                if (mc.currentScreen != null) {
                    mc.player.closeHandledScreen();
                }

                if (this.afkBypassMode.is("funtime")) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 0, 0, SlotActionType.PICKUP, mc.player);
                    mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
                } else {
                    if (this.afkBypassMode.is("default")) {
                        if (mc.player.isOnGround()) {
                            ThrowComponent.rotationTimer.setDelay(42L);
                        }

                        ((MinecraftClientAccessor)mc).sadnes$invokeDoAttack();
                        mc.player.getInventory().setSelectedSlot((mc.player.getInventory().getSelectedSlot() + 1) % 9);
                        this.scheduler
                            .clear()
                            .addTicked(0, () -> mc.player.getInventory().setSelectedSlot((mc.player.getInventory().getSelectedSlot() + 8) % 9));
                    }
                }
            }
        }
    }

    public boolean isAfk() {
        InactivityFpsLimiter inactivityFpsLimiter = mc.getInactivityFpsLimiter();
        if (inactivityFpsLimiter == null) {
            return false;
        }

        long l = ((InactivityFpsLimiterAccessor)inactivityFpsLimiter).sadnes$getLastInputTime();
        return Util.getMeasuringTimeMs() - l > (long)(this.afkSeconds.getValue() * 1000.0F);
    }
}
