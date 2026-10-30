package tech.sadnes.client.feature.impl.movement;

import net.minecraft.client.network.ClientPlayerEntity;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.setting.NumberSetting;
import tech.sadnes.client.util.player.MoveUtil;

@FeatureInfo(
    name = "AbilitiesFly",
    description = "feature.impl.movement.abilitiesfly",
    displayName = "feature.impl.movement.abilitiesfly",
    category = Category.MOVEMENT,
    subcategory = Subcategory.MOVEMENT_AIR
)
public class AbilitiesFly extends Feature {
    private final ModeSetting flightMode = new ModeSetting("abilitiesfly.flightMode", new String[]{"rw", "legit", "custom"}).setValue("legit");
    private final NumberSetting speedX = new NumberSetting("abilitiesfly.speedx", 1.0F, 1.0F, 5.0F, 0.1F).setVisibleWhen(() -> this.flightMode.is("custom"));
    private final NumberSetting speedY = new NumberSetting("abilitiesfly.speedy", 1.0F, 1.0F, 5.0F, 0.1F).setVisibleWhen(() -> this.flightMode.is("custom"));

    public static AbilitiesFly getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(AbilitiesFly.class);
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (mc.player.getAbilities().flying) {
            boolean flag = MoveUtil.isMoving();
            ModeSetting modeSetting = this.flightMode;
            if (!flag) {
                mc.player.setVelocity(0.0, 0.0, 0.0);
                return;
            }

            if (modeSetting.is("rw")) {
                long l;
                if (!mc.options.leftKey.isPressed()) {
                    if (!mc.options.rightKey.isPressed()) {
                        if (mc.options.jumpKey.isPressed()) {
                            l = 4607317526659989504L;
                        } else {
                            l = 4607632778870128640L;
                        }
                    } else {
                        l = 4607317526659989504L;
                    }
                } else {
                    l = 4607317526659989504L;
                }

                long l2;
                if (mc.options.sneakKey.isPressed()) {
                    l2 = -4625196817309499392L;
                } else if (mc.options.jumpKey.isPressed()) {
                    l2 = 4598175219545276416L;
                } else {
                    l2 = 0L;
                }

                MoveUtil.setMotion(Double.longBitsToDouble(l));
                ClientPlayerEntity clientPlayerEntity = mc.player;
                double d = mc.player.getVelocity().x;
                double d2 = mc.player.getVelocity().z;
                double d3 = Double.longBitsToDouble(l2);
                clientPlayerEntity.setVelocity(d, d3, d2);
                return;
            }

            if (modeSetting.is("legit")) {
                long l3;
                if (mc.options.sneakKey.isPressed()) {
                    l3 = -4626097536912850947L;
                } else if (mc.options.jumpKey.isPressed()) {
                    l3 = 4597274499941924861L;
                } else {
                    l3 = 0L;
                }

                MoveUtil.setSpeed(1.010979820548404);
                ClientPlayerEntity clientPlayerEntity2 = mc.player;
                double d4 = mc.player.getVelocity().x;
                double d5 = mc.player.getVelocity().z;
                double d6 = Double.longBitsToDouble(l3);
                clientPlayerEntity2.setVelocity(d4, d6, d5);
                return;
            }

            if (modeSetting.is("custom")) {
                NumberSetting numberSetting;
                long l4;
                label53: {
                    boolean flag2 = mc.options.sneakKey.isPressed();
                    NumberSetting numberSetting2 = this.speedY;
                    int i;
                    if (flag2) {
                        float f = numberSetting2.getValue();
                        numberSetting = this.speedX;
                        i = Integer.MIN_VALUE ^ Float.floatToRawIntBits(f);
                    } else {
                        boolean flag3 = mc.options.jumpKey.isPressed();
                        NumberSetting numberSetting3 = this.speedX;
                        if (!flag3) {
                            numberSetting = numberSetting3;
                            l4 = 0L;
                            break label53;
                        }

                        float f2 = numberSetting2.getValue();
                        numberSetting = numberSetting3;
                        i = Float.floatToRawIntBits(f2);
                    }

                    l4 = Double.doubleToRawLongBits(Float.intBitsToFloat(i));
                }

                MoveUtil.setSpeed(numberSetting.getValue().floatValue());
                ClientPlayerEntity clientPlayerEntity3 = mc.player;
                double d7 = mc.player.getVelocity().x;
                double d8 = mc.player.getVelocity().z;
                double d9 = Double.longBitsToDouble(l4);
                clientPlayerEntity3.setVelocity(d7, d9, d8);
                return;
            }
        }
    }
}
