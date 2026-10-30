package tech.sadnes.client.script.api;

import java.util.Locale;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.FluidHandling;
import tech.sadnes.api.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import tech.sadnes.api.mixin.client.MinecraftClientAccessor;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.client.util.network.PacketQueue;
import tech.sadnes.client.util.player.MovementSimulator;
import tech.sadnes.client.util.player.RaycastUtil;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.NativeObject;

public final class ScriptInteractionApi {
    private ScriptInteractionApi() {
    }

    public static void install(NativeObject nativeObject, NativeObject nativeObject2) {
        installBlocks(nativeObject);
        installEntities(nativeObject);
        installItems(nativeObject);
        installRaycast(nativeObject2);
        ScriptValues.putFunction(nativeObject, "predict", objects -> {
            if (mc().player == null) {
                return null;
            }

            MovementSimulator movementSimulator = MovementSimulator.simulate(MathHelper.clamp(ScriptValues.argInt(objects, 0, 1), 1, 40));
            NativeObject nativeObject3 = ScriptConvertUtil.wrapVec(movementSimulator.getPosition());
            ScriptValues.putValue(nativeObject3, "onGround", movementSimulator.isOnGround());
            ScriptValues.putValue(nativeObject3, "box", ScriptConvertUtil.wrapBox(movementSimulator.getBoundingBox()));
            return nativeObject3;
        });
    }

    private static void installBlocks(NativeObject nativeObject) {
        ScriptValues.putFunction(nativeObject, "place", objects -> {
            ClientPlayerEntity clientPlayerEntity = mc().player;
            if (clientPlayerEntity != null && mc().interactionManager != null && ScriptValues.hasArg(objects, 0)) {
                BlockHitResult blockHitResult = parseHitResult(objects[0]);
                if (blockHitResult != null) {
                    if (!clientPlayerEntity.canInteractWithBlockAt(blockHitResult.getBlockPos(), 0.0)) {
                        return false;
                    }

                    Hand hand = ScriptValues.hasArg(objects, 1) ? ScriptConvertUtil.parseHand(ScriptValues.argString(objects, 1)) : getPlaceHand(clientPlayerEntity);
                    PacketQueue.queue(() -> {
                        ActionResult actionResult = mc().interactionManager.interactBlock(mc().player, hand, blockHitResult);
                        if (actionResult instanceof Success && ((Success)actionResult).swingSource() == SwingSource.CLIENT) {
                            mc().player.swingHand(hand);
                        }
                    });
                    return true;
                }
            }

            return false;
        });
        ScriptValues.putFunction(
            nativeObject,
            "breakBlock",
            objects2 -> {
                ClientPlayerEntity clientPlayerEntity2 = mc().player;
                if (clientPlayerEntity2 != null && mc().world != null && mc().interactionManager != null) {
                    BlockPos blockPos = BlockPos.ofFloored(
                        ScriptValues.argDouble(objects2, 0, 0.0), ScriptValues.argDouble(objects2, 1, 0.0), ScriptValues.argDouble(objects2, 2, 0.0)
                    );
                    if (!mc().world.getBlockState(blockPos).isAir()) {
                        if (!clientPlayerEntity2.canInteractWithBlockAt(blockPos, 0.0)) {
                            return false;
                        }

                        Direction direction = ScriptValues.hasArg(objects2, 3)
                            ? parseDirection(ScriptValues.argString(objects2, 3))
                            : getFacingDirection(blockPos, clientPlayerEntity2.getEyePos());
                        PacketQueue.queue(() -> {
                            if (mc().interactionManager.updateBlockBreakingProgress(blockPos, direction)) {
                                mc().world.spawnBlockBreakingParticle(blockPos, direction);
                                mc().player.swingHand(Hand.MAIN_HAND);
                            }
                        });
                        return true;
                    }
                }

                return false;
            }
        );
        ScriptValues.putFunction(nativeObject, "stopBreaking", objects3 -> {
            if (mc().interactionManager != null) {
                PacketQueue.queue(() -> mc().interactionManager.cancelBlockBreaking());
            }

            return null;
        });
        ScriptValues.putFunction(nativeObject, "breaking", objects4 -> mc().interactionManager != null && mc().interactionManager.isBreakingBlock() ? true : false);
        ScriptValues.putFunction(
            nativeObject,
            "breakProgress",
            objects5 -> mc().interactionManager == null
                ? 0.0F
                : ((ClientPlayerInteractionManagerAccessor)mc().interactionManager).sadnes$getCurrentBreakingProgress()
        );
    }

    private static void installEntities(NativeObject nativeObject) {
        ScriptValues.putFunction(nativeObject, "attack", objects -> {
            Entity entity = ScriptPlayerApi.unwrapEntity(ScriptValues.hasArg(objects, 0) ? objects[0] : null);
            ClientPlayerEntity clientPlayerEntity = mc().player;
            if (entity != null && clientPlayerEntity != null && clientPlayerEntity.canInteractWithEntity(entity, 0.0)) {
                PacketQueue.queue(() -> {
                    mc().interactionManager.attackEntity(mc().player, entity);
                    mc().player.swingHand(Hand.MAIN_HAND);
                });
                return true;
            } else {
                return false;
            }
        });
        ScriptValues.putFunction(nativeObject, "interact", objects2 -> {
            Entity entity2 = ScriptPlayerApi.unwrapEntity(ScriptValues.hasArg(objects2, 0) ? objects2[0] : null);
            ClientPlayerEntity clientPlayerEntity2 = mc().player;
            if (entity2 != null && clientPlayerEntity2 != null && clientPlayerEntity2.canInteractWithEntity(entity2, 0.0)) {
                Hand hand = ScriptConvertUtil.parseHand(ScriptValues.argString(objects2, 1, "main"));
                PacketQueue.queue(() -> {
                    EntityHitResult entityHitResult = new EntityHitResult(entity2, getHitPos(mc().player, entity2.getBoundingBox()));
                    ActionResult actionResult = mc().interactionManager.interactEntityAtLocation(mc().player, entity2, entityHitResult, hand);
                    if (!actionResult.isAccepted()) {
                        actionResult = mc().interactionManager.interactEntity(mc().player, entity2, hand);
                    }

                    if (actionResult instanceof Success && ((Success)actionResult).swingSource() == SwingSource.CLIENT) {
                        mc().player.swingHand(hand);
                    }
                });
                return true;
            } else {
                return false;
            }
        });
    }

    private static void installItems(NativeObject nativeObject) {
        ScriptValues.putFunction(nativeObject, "interactItem", objects -> {
            if (mc().player != null && mc().interactionManager != null) {
                Hand hand = ScriptConvertUtil.parseHand(ScriptValues.argString(objects, 0, "main"));
                PacketQueue.queue(() -> mc().interactionManager.interactItem(mc().player, hand));
                return true;
            } else {
                return false;
            }
        });
        ScriptValues.putFunction(nativeObject, "rightClick", objects2 -> {
            if (mc().player != null && mc().interactionManager != null) {
                PacketQueue.queue(() -> ((MinecraftClientAccessor)mc()).sadnes$invokeDoItemUse());
                return true;
            } else {
                return false;
            }
        });
    }

    private static void installRaycast(NativeObject nativeObject) {
        ScriptValues.putFunction(
            nativeObject,
            "raycast",
            objects -> {
                ClientPlayerEntity clientPlayerEntity = mc().player;
                if (clientPlayerEntity == null) {
                    return null;
                }

                float f = ScriptValues.argFloat(objects, 0, clientPlayerEntity.getYaw());
                float f2 = ScriptValues.argFloat(objects, 1, clientPlayerEntity.getPitch());
                double d = ScriptValues.argDouble(objects, 2, clientPlayerEntity.getBlockInteractionRange());
                return ScriptValues.hasArg(objects, 3)
                    ? ScriptConvertUtil.wrapHitResult(RaycastUtil.raycastBlocks(f, f2, d, parseFluidHandling(objects[3])))
                    : ScriptConvertUtil.wrapHitResult(RaycastUtil.raycastBlocks(f, f2, d, parseFluidHandling(null)));
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "placeTarget",
            objects2 -> findPlaceTarget(
                BlockPos.ofFloored(ScriptValues.argDouble(objects2, 0, 0.0), ScriptValues.argDouble(objects2, 1, 0.0), ScriptValues.argDouble(objects2, 2, 0.0))
            )
        );
    }

    private static NativeObject findPlaceTarget(BlockPos blockPos) {
        ClientPlayerEntity clientPlayerEntity = mc().player;
        if (clientPlayerEntity != null && mc().world != null && mc().world.getBlockState(blockPos).isReplaceable()) {
            if (clientPlayerEntity.getBoundingBox().intersects(new Box(blockPos))) {
                return null;
            }

            Vec3d vec3d = clientPlayerEntity.getEyePos();
            double d = clientPlayerEntity.getBlockInteractionRange();
            BlockPos blockPos2 = null;
            Direction direction = null;
            Vec3d vec3d2 = null;
            double d2 = Double.MAX_VALUE;

            for (Direction direction2 : Direction.values()) {
                BlockPos blockPos3 = blockPos.offset(direction2);
                if (!mc().world.getBlockState(blockPos3).isReplaceable()) {
                    Direction direction3 = direction2.getOpposite();
                    Vec3d vec3d3 = blockPos3.toCenterPos().add(Vec3d.of(direction3.getVector()).multiply(0.5));
                    double d3 = vec3d.distanceTo(vec3d3);
                    if (!(d3 > d)) {
                        double d4 = Vec3d.of(direction3.getVector()).dotProduct(vec3d.subtract(vec3d3)) > 0.0 ? d3 : d3 + 100.0;
                        if (!(d4 >= d2)) {
                            d2 = d4;
                            blockPos2 = blockPos3;
                            direction = direction3;
                            vec3d2 = vec3d3;
                        }
                    }
                }
            }

            if (blockPos2 == null) {
                return null;
            }

            NativeObject nativeObject = ScriptValues.newObject();
            ScriptValues.putValue(nativeObject, "type", "block");
            ScriptValues.putValue(nativeObject, "side", direction.name().toLowerCase(Locale.ROOT));
            ScriptValues.putValue(nativeObject, "pos", ScriptConvertUtil.wrapVec(vec3d2));
            BlockState blockState = mc().world.getBlockState(blockPos2);
            ScriptValues.putValue(nativeObject, "block", ScriptConvertUtil.wrapBlock(blockPos2, blockState));
            ScriptValues.putValue(nativeObject, "target", ScriptConvertUtil.vec(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            return nativeObject;
        } else {
            return null;
        }
    }

    private static BlockHitResult parseHitResult(Object object) {
        Object object2 = ScriptValues.getProperty(object, "side");
        Object object3 = ScriptValues.getProperty(object, "pos");
        Object object4 = ScriptValues.getProperty(object, "block");
        if (object2 != null && object3 != null && object4 != null) {
            Vec3d vec3d = new Vec3d(ScriptValues.getNumber(object3, "x", 0.0), ScriptValues.getNumber(object3, "y", 0.0), ScriptValues.getNumber(object3, "z", 0.0));
            BlockPos blockPos = new BlockPos(
                (int)ScriptValues.getNumber(object4, "x", 0.0), (int)ScriptValues.getNumber(object4, "y", 0.0), (int)ScriptValues.getNumber(object4, "z", 0.0)
            );
            return new BlockHitResult(vec3d, parseDirection(Context.toString(object2)), blockPos, false);
        } else {
            return null;
        }
    }

    private static Hand getPlaceHand(ClientPlayerEntity clientPlayerEntity) {
        boolean flag;
        if (!(clientPlayerEntity.getMainHandStack().getItem() instanceof BlockItem)) {
            flag = clientPlayerEntity.getOffHandStack().getItem() instanceof BlockItem;
        } else {
            flag = false;
        }

        return flag ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    private static Direction parseDirection(String string) {
        Direction direction = Direction.byId(string.toLowerCase(Locale.ROOT));
        return direction == null ? Direction.UP : direction;
    }

    private static Direction getFacingDirection(BlockPos blockPos, Vec3d vec3d) {
        Vec3d vec3d2 = vec3d.subtract(blockPos.toCenterPos());
        return Direction.getFacing(vec3d2.x, vec3d2.y, vec3d2.z);
    }

    private static Vec3d getHitPos(ClientPlayerEntity clientPlayerEntity, Box box) {
        Vec3d vec3d = clientPlayerEntity.getEyePos();
        Vec3d vec3d2 = RaycastUtil.getRotationVector(clientPlayerEntity.getPitch(), clientPlayerEntity.getYaw());
        Vec3d vec3d3 = vec3d.add(vec3d2.multiply(clientPlayerEntity.getEntityInteractionRange()));
        return box.raycast(vec3d, vec3d3).orElse(box.getCenter());
    }

    private static FluidHandling parseFluidHandling(Object object) {
        if (object instanceof CharSequence charSequence && "source".equalsIgnoreCase(charSequence.toString())) {
            return FluidHandling.SOURCE_ONLY;
        } else {
            return object != null && Context.toBoolean(object) ? FluidHandling.ANY : FluidHandling.NONE;
        }
    }

    private static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }
}
