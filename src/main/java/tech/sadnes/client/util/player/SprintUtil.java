package tech.sadnes.client.util.player;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import tech.sadnes.client.event.impl.SprintResetEvent;
import tech.sadnes.client.feature.impl.movement.NoSlow;
import tech.sadnes.client.util.compat.ProtocolUtil;

public final class SprintUtil {
    private SprintUtil() {
    }

    public static boolean shouldStopSprint(ClientPlayerEntity clientPlayerEntity) {
        if (clientPlayerEntity != null && ProtocolUtil.isLegacyServer()) {
            SprintResetEvent sprintResetEvent = new SprintResetEvent(SprintResetEvent.Reason.LEGACY);
            sprintResetEvent.post();
            if (sprintResetEvent.isCancelled()) {
                return false;
            }

            if (clientPlayerEntity.isSwimming()) {
                return !clientPlayerEntity.isTouchingWater();
            }

            if (!clientPlayerEntity.isGliding() && !clientPlayerEntity.shouldSlowDown()) {
                if (clientPlayerEntity.hasVehicle()) {
                    Entity entity = clientPlayerEntity.getVehicle();
                    if (entity == null || entity.getType() != EntityType.CAMEL) {
                        return true;
                    }
                } else {
                    NoSlow noSlow = NoSlow.getInstance();
                    if ((noSlow == null || !noSlow.isEnabled() || !noSlow.modes.has("food")) && clientPlayerEntity.isUsingItem() && !clientPlayerEntity.isSubmergedInWater()) {
                        return true;
                    }

                    if (clientPlayerEntity.isTouchingWater() && !clientPlayerEntity.isSubmergedInWater()) {
                        return true;
                    }

                    if (clientPlayerEntity.horizontalCollision && !clientPlayerEntity.collidedSoftly) {
                        return true;
                    }
                }

                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public static boolean canSprint(ClientPlayerEntity clientPlayerEntity) {
        if (clientPlayerEntity == null) {
            return false;
        } else if (!ProtocolUtil.isLegacyServer()) {
            return true;
        } else {
            return clientPlayerEntity.horizontalCollision ? false : clientPlayerEntity.isOnGround() || clientPlayerEntity.isSubmergedInWater();
        }
    }
}
