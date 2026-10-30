package tech.sadnes.client.feature.impl.misc;

import java.util.Arrays;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.feature.component.MovementPauseComponent;
import tech.sadnes.client.feature.impl.combat.AntiBot;
import tech.sadnes.client.lang.Language;
import tech.sadnes.client.server.ServerManager;
import tech.sadnes.client.server.donut.DonutRegion;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.MultiSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.setting.TextSetting;
import tech.sadnes.client.setting.Unit;
import tech.sadnes.client.ui.notification.NotificationType;
import tech.sadnes.client.util.chat.NotificationUtil;
import tech.sadnes.client.util.math.StopWatch;
import tech.sadnes.client.util.math.TimerUtil;

@FeatureInfo(name = "AutoLeave", displayName = "feature.impl.misc.autoleave", category = Category.MISC, subcategory = Subcategory.MISC_SAFETY)
public final class AutoLeave extends Feature {
    private final ModeSetting leaveMode = new ModeSetting("autoLeave.leaveMode", new String[]{"disconnect", "command"});
    private final TextSetting commandText = new TextSetting("autoLeave.commandtext", "hub", "Введи команду без /")
        .setVisibleWhen(() -> this.leaveMode.is("command"));
    private final MultiSetting reasonsToLeave = new MultiSetting(
        "autoLeave.reasonsToLeave",
        BooleanSetting.of("players_near", false),
        BooleanSetting.of("after_pvp", false),
        BooleanSetting.of("lowest_hp", false),
        BooleanSetting.of("donut_host", false)
    );
    private final NumberSetting health = new NumberSetting("autoLeave.health", 10.0F, 1.0F, 20.0F, 1.0F, Unit.HP)
        .setVisibleWhen(() -> this.reasonsToLeave.getOption("lowest_hp").getValue());
    private final NumberSetting distance = new NumberSetting("autoLeave.distance", 30.0F, 2.0F, 50.0F, 1.0F, Unit.METER)
        .setVisibleWhen(() -> this.reasonsToLeave.getOption("players_near").getValue());
    private final MultiSetting hosts = new MultiSetting(
            "autoLeave.hosts", Arrays.stream(DonutRegion.values()).map(DonutRegion::getDisplayName).toArray(String[]::new)
        )
        .setVisibleWhen(() -> this.reasonsToLeave.getOption("donut_host").getValue());
    boolean wasInPvp = false;
    TimerUtil commandTimer = new TimerUtil();
    private final StopWatch hostCommandStopWatch = new StopWatch();
    private final StopWatch lastCombatStopWatch = new StopWatch();

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player.hurtTime > 0) {
            this.lastCombatStopWatch.reset();
        }

        if (ServerManager.isInPvp()) {
            this.wasInPvp = true;
        } else if (this.reasonsToLeave.getOption("after_pvp").getValue() && this.wasInPvp) {
            this.leave(Language.translate("pvp_ends"), this.commandText.getValue());
            this.wasInPvp = false;
        } else if (this.reasonsToLeave.getOption("lowest_hp").getValue() && mc.player.getHealth() <= this.health.getValue()) {
            this.leave(Language.translate("ur_hp_low") + ": " + (int)mc.player.getHealth(), this.commandText.getValue());
        } else if (this.reasonsToLeave.getOption("players_near").getValue() && this.isPlayerNearby()) {
            this.leave(
                Language.translate("on_distance") + " " + this.distance.getValue().intValue() + " " + Language.translate("finded_player") + "!",
                this.commandText.getValue()
            );
        } else {
            if (this.isOnBlockedHost()) {
                this.leaveHost();
            }
        }
    }

    private boolean isPlayerNearby() {
        float f = this.distance.getValue();
        ClientPlayerEntity clientPlayerEntity = mc.player;
        return clientPlayerEntity == null
            ? false
            : mc.world
                .getPlayers()
                .stream()
                .filter(abstractClientPlayerEntity -> abstractClientPlayerEntity != clientPlayerEntity)
                .filter(LivingEntity::isAlive)
                .filter(abstractClientPlayerEntity2 -> !AntiBot.isBotStatic(abstractClientPlayerEntity2))
                .filter(abstractClientPlayerEntity3 -> !Sadnes.getInstance().getFriendManager().isFriend(abstractClientPlayerEntity3.getGameProfile().name()))
                .anyMatch(abstractClientPlayerEntity4 -> clientPlayerEntity.distanceTo(abstractClientPlayerEntity4) <= f);
    }

    private boolean isOnBlockedHost() {
        boolean flag = mc.player.getX() != 0.0 || mc.player.getZ() != 0.0;
        boolean flag2;
        if (!mc.player.isDead()) {
            flag2 = flag;
        } else {
            flag2 = false;
        }

        return this.reasonsToLeave.getOption("donut_host").getValue()
            && ServerManager.isDonut()
            && flag2
            && this.hosts.isSelected(DonutRegion.getCurrent().getDisplayName());
    }

    private void leaveHost() {
        String string = String.format(
            Language.translate("autoLeave.notify.host", "Вы на хосте %s"),
            Language.translateOption(this.hosts.getName(), DonutRegion.getCurrent().getDisplayName())
        );
        if (this.leaveMode.is("disconnect")) {
            this.leave(string, this.commandText.getValue());
        } else {
            boolean flag = mc.player.currentScreenHandler != mc.player.playerScreenHandler;
            boolean flag2 = !this.hostCommandStopWatch.hasPassed(15000.0);
            if (!flag && !MovementPauseComponent.isPaused() && !flag2) {
                mc.player.networkHandler.sendChatCommand(this.commandText.getValue());
                NotificationUtil.notifyKeyed(NotificationType.INFO, this.getDisplayName(), string);
                this.hostCommandStopWatch.reset();
            }
        }
    }

    public boolean shouldLeaveHost() {
        return this.isEnabled() && this.leaveMode.is("command") && this.isOnBlockedHost();
    }

    @EventHandler(priority = 500)
    public void onAttack(AttackEntityEvent attackEntityEvent) {
        boolean flag;
        if (attackEntityEvent.getStage() == AttackEntityEvent.Stage.PRE) {
            flag = true;
        } else {
            flag = false;
            if (attackEntityEvent.getTarget() instanceof PlayerEntity) {
                this.lastCombatStopWatch.reset();
            }
        }

        boolean flag2 = !this.lastCombatStopWatch.hasPassed(30000.0);
        boolean flag3 = !this.hostCommandStopWatch.hasPassed(2000.0);
        if (flag && !flag2 && flag3) {
            attackEntityEvent.cancel();
        }
    }

    public void leave(String string, String string2) {
        ClientPlayNetworkHandler clientPlayNetworkHandler = mc.getNetworkHandler();
        if (clientPlayNetworkHandler != null) {
            if (this.leaveMode.is("disconnect")) {
                clientPlayNetworkHandler.getConnection().disconnect(Text.literal("AutoLeave: " + string));
                this.toggle();
            } else if (this.commandTimer.isReached()) {
                clientPlayNetworkHandler.sendChatCommand(string2);
                NotificationUtil.notify(NotificationType.INFO, this.getDisplayName(), string);
                this.commandTimer.setDelay(250L);
                this.toggle();
            }
        }
    }
}
