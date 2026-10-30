package tech.sadnes.client.feature.impl.combat;

import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.Hand;
import tech.sadnes.api.mixin.accessor.PlayerInteractEntityC2SPacketAccessor;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.event.EventHandler;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.feature.Category;
import tech.sadnes.client.feature.Feature;
import tech.sadnes.client.feature.FeatureInfo;
import tech.sadnes.client.feature.FeatureManager;
import tech.sadnes.client.feature.Subcategory;
import tech.sadnes.client.setting.ModeSetting;
import tech.sadnes.client.util.math.MathUtil;
import tech.sadnes.client.util.network.PacketSender;

@FeatureInfo(
    name = "Criticals",
    description = "feature.impl.combat.criticals",
    displayName = "feature.impl.combat.criticals",
    category = Category.COMBAT,
    subcategory = Subcategory.COMBAT_TWEAKS
)
public final class Criticals extends Feature {
    private final ModeSetting type = new ModeSetting("criticals.type", new String[]{"Grim 1.17+ Air", "NCP+", "Macespoof", "Post", "legit"});

    public static Criticals getInstance() {
        FeatureManager featureManager = Sadnes.getInstance().getFeatureManager();
        return featureManager.getFeature(Criticals.class);
    }

    public static boolean isPostMode() {
        Criticals criticals = getInstance();
        boolean flag;
        if (!mc.player.getMainHandStack().isOf(Items.MACE)) {
            flag = mc.player.getOffHandStack().isOf(Items.MACE);
        } else {
            flag = true;
        }

        return criticals != null && criticals.isEnabled() && criticals.type.is("Post") && !flag;
    }

    @EventHandler
    public void onAttack(AttackEntityEvent attackEntityEvent) {
        label83: {
            label84: {
                label69: {
                    String string = this.type.getValue();
                    byte b = -1;
                    switch (string.hashCode()) {
                        case 2390608:
                            if (string.equals("NCP+")) {
                                break label84;
                            }
                            break;
                        case 102851513:
                            if (string.equals("legit")) {
                                b = 3;
                            }
                            break;
                        case 483584947:
                            if (string.equals("Macespoof")) {
                                break label83;
                            }
                            break;
                        case 2100270369:
                            if (string.equals("Grim 1.17+ Air")) {
                                break label69;
                            }
                    }

                    switch (b) {
                        case 0:
                            break;
                        case 1:
                            break label84;
                        case 2:
                            break label83;
                        case 3:
                            if (attackEntityEvent.getStage() != AttackEntityEvent.Stage.POST) {
                                return;
                            }

                            if (attackEntityEvent.getTarget() instanceof BoatEntity boatEntity) {
                                for (int i = 0; i < 15; i++) {
                                    PacketSender.sendSilent(new HandSwingC2SPacket(Hand.MAIN_HAND));
                                    PacketSender.sendSilent(PlayerInteractEntityC2SPacketAccessor.sadnes$attack(boatEntity, mc.player.isSneaking()));
                                }
                            }

                            return;
                        default:
                            return;
                    }
                }

                if (attackEntityEvent.getStage() != AttackEntityEvent.Stage.PRE) {
                    return;
                }

                if (!mc.player.isOnGround() && mc.player.fallDistance == 0.0) {
                    mc.player
                        .networkHandler
                        .sendPacket(
                            new Full(
                                mc.player.getX(),
                                mc.player.getY() - (mc.player.fallDistance = MathUtil.random(1.0E-5F, 1.0E-4F)),
                                mc.player.getZ(),
                                mc.player.getYaw(),
                                mc.player.getPitch(),
                                mc.player.isOnGround(),
                                mc.player.horizontalCollision
                            )
                        );
                    return;
                }

                return;
            }

            if (attackEntityEvent.getStage() != AttackEntityEvent.Stage.PRE) {
                return;
            }

            if (mc.player.isOnGround()) {
                mc.player
                    .networkHandler
                    .sendPacket(
                        new Full(
                            mc.player.getX(),
                            mc.player.getY() + 0.0625,
                            mc.player.getZ(),
                            mc.player.getYaw(),
                            mc.player.getPitch(),
                            false,
                            mc.player.horizontalCollision
                        )
                    );
                mc.player
                    .networkHandler
                    .sendPacket(
                        new Full(
                            mc.player.getX(),
                            mc.player.getY(),
                            mc.player.getZ(),
                            mc.player.getYaw(),
                            mc.player.getPitch(),
                            false,
                            mc.player.horizontalCollision
                        )
                    );
                return;
            }

            return;
        }

        if (attackEntityEvent.getStage() == AttackEntityEvent.Stage.PRE) {
            if (mc.player.getMainHandStack().getItem() == Items.MACE && mc.player.fallDistance < 9.0) {
                mc.player
                    .networkHandler
                    .sendPacket(
                        new Full(
                            mc.player.getX(),
                            mc.player.getY() + 9.0,
                            mc.player.getZ(),
                            mc.player.getYaw(),
                            mc.player.getPitch(),
                            false,
                            mc.player.horizontalCollision
                        )
                    );
                mc.player
                    .networkHandler
                    .sendPacket(
                        new Full(
                            mc.player.getX(),
                            mc.player.getY(),
                            mc.player.getZ(),
                            mc.player.getYaw(),
                            mc.player.getPitch(),
                            false,
                            mc.player.horizontalCollision
                        )
                    );
            }
        }
    }

    public ModeSetting getType() {
        return this.type;
    }
}
