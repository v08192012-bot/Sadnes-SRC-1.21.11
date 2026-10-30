package tech.sadnes.client.feature.impl.movement;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import org.lwjgl.glfw.GLFW;
import tech.sadnes.api.mixin.accessor.ClientPlayerEntityAccessor;
import tech.sadnes.api.mixin.accessor.KeyBindingAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.SprintEvent;
import tech.sadnes.client.event.impl.SprintResetEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.BooleanSetting;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.SprintUtil;

@FeatureInfo(
    name = "Sprint",
    description = "feature.impl.movement.sprint",
    displayName = "feature.impl.movement.sprint",
    category = Category.MOVEMENT,
    subcategory = Subcategory.SPEED
)
public class Sprint extends Feature {
    private final BooleanSetting allowHungry = new BooleanSetting("sprint.allowHungry", false);
    private final BooleanSetting keepSprint = new BooleanSetting("sprint.keepSprint", false);
    private final BooleanSetting anticheatBypass = new BooleanSetting("sprint.anticheatBypass", false);
    private final ModeSetting keepSprintMode = new ModeSetting("sprint.keepSprintMode", new String[]{"default", "grim", "grim_hard"}).setVisibleWhen(this.keepSprint::getValue);
    private boolean needsKeyRestore;

    public static Sprint getInstance() {
        return (Sprint)Sadnes.getInstance().getFeatureManager().get(Sprint.class);
    }

    public boolean isKeepSprint() {
        return this.keepSprint.getValue();
    }

    public boolean isGrimMode() {
        return this.keepSprintMode.is("grim");
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        ClientPlayerEntity clientPlayerEntity = mc.player;
        BooleanSetting booleanSetting = this.anticheatBypass;
        if (clientPlayerEntity != null) {
            boolean flag = booleanSetting.getValue();
            boolean flag2 = this.needsKeyRestore;
            if (flag) {
                mc.options.sprintKey.setPressed(true);
                this.needsKeyRestore = true;
                return;
            }

            if (flag2) {
                this.restoreSprintKey();
                return;
            }
        }
    }

    @EventHandler
    public void onMotion(AttackEntityEvent attackEntityEvent) {
        AttackEntityEvent.Stage stage = attackEntityEvent.getStage();
        AttackEntityEvent.Stage stage2 = AttackEntityEvent.Stage.PRE;
        if (stage == stage2) {
            ClientPlayerEntity clientPlayerEntity = mc.player;
            BooleanSetting booleanSetting = this.keepSprint;
            if (clientPlayerEntity != null) {
                boolean flag = booleanSetting.getValue();
                ModeSetting modeSetting = this.keepSprintMode;
                if (flag && modeSetting.is("grim_hard")) {
                    int i = InventoryUtil.getSelectedSlot();
                    ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player.networkHandler;
                    int i2 = (i < 0 ? i + 7 : i) & -8;
                    int i3 = 0 - i2 + i + 1;
                    UpdateSelectedSlotC2SPacket updateSelectedSlotC2SPacket = new UpdateSelectedSlotC2SPacket(i3);
                    clientPlayNetworkHandler.sendPacket(updateSelectedSlotC2SPacket);
                    ClientPlayNetworkHandler clientPlayNetworkHandler2 = mc.player.networkHandler;
                    UpdateSelectedSlotC2SPacket updateSelectedSlotC2SPacket2 = new UpdateSelectedSlotC2SPacket(i);
                    clientPlayNetworkHandler2.sendPacket(updateSelectedSlotC2SPacket2);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onSprint(SprintEvent sprintEvent) {
        SprintEvent.Stage stage = sprintEvent.getStage();
        SprintEvent.Stage stage2 = SprintEvent.Stage.AFSPRINT;
        if (stage == stage2) {
            ClientPlayerEntity clientPlayerEntity = mc.player;
            BooleanSetting booleanSetting = this.anticheatBypass;
            if (clientPlayerEntity != null && !booleanSetting.getValue()) {
                ClientPlayerEntityAccessor clientPlayerEntityAccessor = (ClientPlayerEntityAccessor)mc.player;
                if (clientPlayerEntityAccessor.sadnes$invokeCanStartSprinting() && SprintUtil.canSprint(mc.player)) {
                    if (!clientPlayerEntityAccessor.sadnes$invokeShouldStopSprinting() && !SprintUtil.shouldStopSprint(mc.player)) {
                        mc.player.isSprinting();
                        sprintEvent.setSprinting(true);
                        return;
                    }
                } else if (!clientPlayerEntityAccessor.sadnes$invokeShouldStopSprinting() && !SprintUtil.shouldStopSprint(mc.player) && mc.player.isSprinting()) {
                    sprintEvent.setSprinting(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onSprintReset(SprintResetEvent sprintResetEvent) {
        SprintResetEvent.Reason reason = sprintResetEvent.getReason();
        SprintResetEvent.Reason reason2 = SprintResetEvent.Reason.ATTACK;
        BooleanSetting booleanSetting = this.keepSprint;
        if (reason == reason2 && booleanSetting.getValue()) {
            sprintResetEvent.cancel();
        }

        SprintResetEvent.Reason reason3 = sprintResetEvent.getReason();
        SprintResetEvent.Reason reason4 = SprintResetEvent.Reason.HUNGER;
        BooleanSetting booleanSetting2 = this.allowHungry;
        if (reason3 == reason4 && booleanSetting2.getValue()) {
            sprintResetEvent.cancel();
        }
    }

    @Override
    public void onEnable() {
        if (this.needsKeyRestore) {
            this.restoreSprintKey();
        }

        super.onEnable();
    }

    private void restoreSprintKey() {
        Key key = ((KeyBindingAccessor)mc.options.sprintKey).sadnes$getBoundKey();

        boolean flag = switch (key.getCategory()) {
            case KEYSYM -> InputUtil.isKeyPressed(mc.getWindow(), key.getCode());
            case MOUSE -> GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key.getCode()) == 1;
            default -> false;
        };
        mc.options.sprintKey.setPressed(flag);
        this.needsKeyRestore = false;
    }
}
