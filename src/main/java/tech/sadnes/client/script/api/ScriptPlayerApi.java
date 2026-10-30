package tech.sadnes.client.script.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.component.GuiMoveComponent;
import tech.sadnes.client.feature.component.ThrowComponent;
import tech.sadnes.client.render.ProjectionUtil;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.client.server.DonateItem;
import tech.sadnes.client.server.DonateItemRegistry;
import tech.sadnes.client.util.network.PacketQueue;
import tech.sadnes.client.util.network.PacketSender;
import tech.sadnes.client.util.player.InventoryUtil;
import tech.sadnes.client.util.player.MoveUtil;
import tech.sadnes.client.util.world.HoleFinder;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptPlayerApi {
    private static final Object ENTITY_KEY = new Object();

    private ScriptPlayerApi() {
    }

    public static void install(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = installPlayer(scriptableObject);
        NativeObject nativeObject2 = installWorld(scriptableObject);
        ScriptInteractionApi.install(nativeObject, nativeObject2);
        installMove(scriptableObject);
    }

    private static void installMove(ScriptableObject scriptableObject) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "speed", objects -> MoveUtil.getHorizontalSpeed());
        ScriptValues.putFunction(nativeObject, "isMoving", objects2 -> MoveUtil.isMoving());
        ScriptValues.putFunction(nativeObject, "setSpeed", objects3 -> {
            if (mc().player == null) {
                return null;
            } else if (ScriptValues.hasArg(objects3, 1)) {
                MoveUtil.setMotionWithYaw(ScriptValues.argDouble(objects3, 0, 0.0), ScriptValues.argFloat(objects3, 1, mc().player.getYaw()));
                return null;
            } else {
                MoveUtil.setMotion(ScriptValues.argDouble(objects3, 0, 0.0));
                return null;
            }
        });
        ScriptValues.putFunction(nativeObject, "velocity", objects4 -> {
            if (mc().player == null) {
                return null;
            }

            mc().player.setVelocity(ScriptValues.argDouble(objects4, 0, 0.0), ScriptValues.argDouble(objects4, 1, 0.0), ScriptValues.argDouble(objects4, 2, 0.0));
            return null;
        });
        ScriptValues.putFunction(
            nativeObject,
            "add",
            objects5 -> {
                ClientPlayerEntity clientPlayerEntity = mc().player;
                if (clientPlayerEntity == null) {
                    return null;
                }

                Vec3d vec3d = clientPlayerEntity.getVelocity();
                clientPlayerEntity.setVelocity(
                    vec3d.x + ScriptValues.argDouble(objects5, 0, 0.0),
                    vec3d.y + ScriptValues.argDouble(objects5, 1, 0.0),
                    vec3d.z + ScriptValues.argDouble(objects5, 2, 0.0)
                );
                return null;
            }
        );
        ScriptValues.putFunction(nativeObject, "stop", objects6 -> {
            MoveUtil.stopHorizontal();
            return null;
        });
        ScriptValues.putFunction(nativeObject, "jump", objects7 -> {
            ClientPlayerEntity clientPlayerEntity2 = mc().player;
            if (clientPlayerEntity2 != null && (clientPlayerEntity2.isOnGround() || ScriptValues.argBoolean(objects7, 0, false))) {
                clientPlayerEntity2.jump();
            }

            return null;
        });
        ScriptValues.putFunction(nativeObject, "holdJump", objects8 -> {
            ThrowComponent.rotationTimer.setDelay((long)ScriptValues.argDouble(objects8, 0, 0.0));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "escapeFrom", objects9 -> {
            Vec3d vec3d2 = MoveUtil.findEscapeDirection(unwrapEntity(ScriptValues.hasArg(objects9, 0) ? objects9[0] : null));
            return vec3d2 == null ? null : vec(vec3d2.x, 0.0, vec3d2.z);
        });
        ScriptValues.putFunction(
            nativeObject,
            "forward",
            objects10 -> {
                double[] doubles = MoveUtil.getMotionForYaw(
                    ScriptValues.argDouble(objects10, 0, 0.0),
                    ScriptValues.hasArg(objects10, 1) ? ScriptValues.argFloat(objects10, 1, 0.0F) : (mc().player == null ? 0.0F : mc().player.getYaw())
                );
                return vec(doubles[0], 0.0, doubles[1]);
            }
        );
        scriptableObject.put("move", scriptableObject, nativeObject);
    }

    private static NativeObject installPlayer(ScriptableObject scriptableObject) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "exists", objects -> mc().player != null ? true : false);
        ScriptValues.putFunction(nativeObject, "x", objects2 -> mc().player == null ? 0.0 : mc().player.getX());
        ScriptValues.putFunction(nativeObject, "y", objects3 -> mc().player == null ? 0.0 : mc().player.getY());
        ScriptValues.putFunction(nativeObject, "z", objects4 -> mc().player == null ? 0.0 : mc().player.getZ());
        ScriptValues.putFunction(nativeObject, "yaw", objects5 -> mc().player == null ? 0.0F : mc().player.getYaw());
        ScriptValues.putFunction(nativeObject, "pitch", objects6 -> mc().player == null ? 0.0F : mc().player.getPitch());
        ScriptValues.putFunction(nativeObject, "health", objects7 -> mc().player == null ? 0.0F : mc().player.getHealth());
        ScriptValues.putFunction(nativeObject, "maxHealth", objects8 -> mc().player == null ? 0.0F : mc().player.getMaxHealth());
        ScriptValues.putFunction(nativeObject, "hunger", objects9 -> mc().player == null ? 0 : mc().player.getHungerManager().getFoodLevel());
        ScriptValues.putFunction(nativeObject, "onGround", objects10 -> mc().player != null && mc().player.isOnGround() ? true : false);
        ScriptValues.putFunction(nativeObject, "sprinting", objects11 -> mc().player != null && mc().player.isSprinting() ? true : false);
        ScriptValues.putFunction(nativeObject, "sneaking", objects12 -> mc().player != null && mc().player.isSneaking() ? true : false);
        ScriptValues.putFunction(nativeObject, "speed", objects13 -> {
            ClientPlayerEntity clientPlayerEntity = mc().player;
            if (clientPlayerEntity == null) {
                return 0.0;
            }

            Vec3d vec3d = clientPlayerEntity.getVelocity();
            return Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
        });
        ScriptValues.putFunction(nativeObject, "velocity", objects14 -> {
            ClientPlayerEntity clientPlayerEntity2 = mc().player;
            return clientPlayerEntity2 == null ? null : vec(clientPlayerEntity2.getVelocity().x, clientPlayerEntity2.getVelocity().y, clientPlayerEntity2.getVelocity().z);
        });
        ScriptValues.putFunction(
            nativeObject, "heldItem", objects15 -> mc().player == null ? "" : Registries.ITEM.getId(mc().player.getMainHandStack().getItem()).toString()
        );
        ScriptValues.putFunction(
            nativeObject, "offhandItem", objects16 -> mc().player == null ? "" : Registries.ITEM.getId(mc().player.getOffHandStack().getItem()).toString()
        );
        ScriptValues.putFunction(nativeObject, "wrap", objects17 -> mc().player == null ? null : wrapEntity(mc().player));
        ScriptValues.putFunction(nativeObject, "renderPos", objects18 -> mc().player == null ? null : getRenderPos(mc().player));
        ScriptValues.putFunction(nativeObject, "pos", objects19 -> mc().player == null ? null : ScriptConvertUtil.wrapVec(mc().player.getEntityPos()));
        ScriptValues.putFunction(nativeObject, "eyeY", objects20 -> mc().player == null ? 0.0 : mc().player.getEyeY());
        ScriptValues.putFunction(nativeObject, "box", objects21 -> mc().player == null ? null : ScriptConvertUtil.wrapBox(mc().player.getBoundingBox()));
        ScriptValues.putFunction(nativeObject, "fallDistance", objects22 -> mc().player == null ? 0.0 : mc().player.fallDistance);
        ScriptValues.putFunction(nativeObject, "hurtTime", objects23 -> mc().player == null ? 0 : mc().player.hurtTime);
        ScriptValues.putFunction(nativeObject, "age", objects24 -> mc().player == null ? 0 : mc().player.age);
        ScriptValues.putFunction(nativeObject, "movementInput", objects25 -> {
            ClientPlayerEntity clientPlayerEntity3 = mc().player;
            if (clientPlayerEntity3 != null && clientPlayerEntity3.input != null) {
                NativeObject nativeObject2 = ScriptValues.newObject();
                ScriptValues.putValue(nativeObject2, "forward", clientPlayerEntity3.input.getMovementInput().y);
                ScriptValues.putValue(nativeObject2, "strafe", clientPlayerEntity3.input.getMovementInput().x);
                ScriptValues.putValue(nativeObject2, "jump", clientPlayerEntity3.input.playerInput.jump());
                ScriptValues.putValue(nativeObject2, "sneak", clientPlayerEntity3.input.playerInput.sneak());
                ScriptValues.putValue(nativeObject2, "sprint", clientPlayerEntity3.input.playerInput.sprint());
                return nativeObject2;
            } else {
                return null;
            }
        });
        ScriptValues.putFunction(nativeObject, "setOnGround", objects26 -> {
            if (mc().player != null) {
                mc().player.setOnGround(ScriptValues.argBoolean(objects26, 0, true));
            }

            return null;
        });
        ScriptValues.putFunction(nativeObject, "startGliding", objects27 -> {
            ClientPlayerEntity clientPlayerEntity4 = mc().player;
            if (clientPlayerEntity4 == null) {
                return false;
            }

            clientPlayerEntity4.networkHandler.sendPacket(new ClientCommandC2SPacket(clientPlayerEntity4, Mode.START_FALL_FLYING));
            clientPlayerEntity4.startGliding();
            return true;
        });
        ScriptValues.putFunction(nativeObject, "air", objects28 -> mc().player == null ? 0 : mc().player.getAir());
        ScriptValues.putFunction(nativeObject, "maxAir", objects29 -> mc().player == null ? 0 : mc().player.getMaxAir());
        ScriptValues.putFunction(nativeObject, "armor", objects30 -> mc().player == null ? 0 : mc().player.getArmor());
        ScriptValues.putFunction(nativeObject, "absorption", objects31 -> mc().player == null ? 0.0F : mc().player.getAbsorptionAmount());
        ScriptValues.putFunction(nativeObject, "saturation", objects32 -> mc().player == null ? 0.0F : mc().player.getHungerManager().getSaturationLevel());
        ScriptValues.putFunction(nativeObject, "experience", objects33 -> mc().player == null ? 0 : mc().player.experienceLevel);
        ScriptValues.putFunction(nativeObject, "experienceProgress", objects34 -> mc().player == null ? 0.0F : mc().player.experienceProgress);
        ScriptValues.putFunction(nativeObject, "horizontalCollision", objects35 -> mc().player != null && mc().player.horizontalCollision ? true : false);
        ScriptValues.putFunction(nativeObject, "verticalCollision", objects36 -> mc().player != null && mc().player.verticalCollision ? true : false);
        ScriptValues.putFunction(nativeObject, "gliding", objects37 -> mc().player != null && mc().player.isGliding() ? true : false);
        ScriptValues.putFunction(nativeObject, "swimming", objects38 -> mc().player != null && mc().player.isSwimming() ? true : false);
        ScriptValues.putFunction(nativeObject, "inWater", objects39 -> mc().player != null && mc().player.isTouchingWater() ? true : false);
        ScriptValues.putFunction(nativeObject, "inLava", objects40 -> mc().player != null && mc().player.isInLava() ? true : false);
        ScriptValues.putFunction(nativeObject, "onFire", objects41 -> mc().player != null && mc().player.isOnFire() ? true : false);
        ScriptValues.putFunction(nativeObject, "invisible", objects42 -> mc().player != null && mc().player.isInvisible() ? true : false);
        ScriptValues.putFunction(nativeObject, "usingItem", objects43 -> mc().player != null && mc().player.isUsingItem() ? true : false);
        ScriptValues.putFunction(nativeObject, "activeHand", objects44 -> mc().player == null ? "main" : ScriptConvertUtil.handName(mc().player.getActiveHand()));
        ScriptValues.putFunction(nativeObject, "itemUseTime", objects45 -> mc().player == null ? 0 : mc().player.getItemUseTimeLeft());
        ScriptValues.putFunction(nativeObject, "handSwing", objects46 -> mc().player == null ? 0 : mc().player.handSwingTicks);
        ScriptValues.putFunction(nativeObject, "attackCooldown", objects47 -> mc().player == null ? 1.0F : mc().player.getAttackCooldownProgress(0.0F));
        ScriptValues.putFunction(nativeObject, "riding", objects48 -> mc().player != null && mc().player.hasVehicle() ? true : false);
        ScriptValues.putFunction(
            nativeObject, "vehicle", objects49 -> mc().player != null && mc().player.getVehicle() != null ? wrapEntity(mc().player.getVehicle()) : null
        );
        ScriptValues.putFunction(nativeObject, "creative", objects50 -> mc().player != null && mc().player.getAbilities().creativeMode ? true : false);
        ScriptValues.putFunction(nativeObject, "flying", objects51 -> mc().player != null && mc().player.getAbilities().flying ? true : false);
        ScriptValues.putFunction(nativeObject, "setFlying", objects52 -> {
            if (mc().player == null) {
                return null;
            }

            mc().player.getAbilities().flying = ScriptValues.argBoolean(objects52, 0, false);
            return null;
        });
        ScriptValues.putFunction(nativeObject, "mainHand", objects53 -> mc().player == null ? null : ScriptConvertUtil.wrapStack(mc().player.getMainHandStack()));
        ScriptValues.putFunction(nativeObject, "offHand", objects54 -> mc().player == null ? null : ScriptConvertUtil.wrapStack(mc().player.getOffHandStack()));
        ScriptValues.putFunction(nativeObject, "equipment", objects55 -> mc().player == null ? null : ScriptConvertUtil.wrapEquipment(mc().player));
        ScriptValues.putFunction(nativeObject, "effects", objects56 -> mc().player == null ? ScriptValues.toArray(List.of()) : ScriptConvertUtil.wrapEffects(mc().player));
        ScriptValues.putFunction(nativeObject, "effect", objects57 -> {
            if (mc().player == null) {
                return null;
            }

            String string = ScriptValues.argString(objects57, 0);

            for (StatusEffectInstance statusEffectInstance : mc().player.getStatusEffects()) {
                NativeObject nativeObject3 = ScriptConvertUtil.wrapEffect(statusEffectInstance);
                if (nativeObject3 != null && string.equals(String.valueOf(nativeObject3.get("id", nativeObject3)))) {
                    return nativeObject3;
                }
            }

            return null;
        });
        ScriptValues.putFunction(nativeObject, "lookingAt", objects58 -> ScriptConvertUtil.wrapHitResult(mc().crosshairTarget));
        installInventory(nativeObject);
        ScriptValues.putFunction(nativeObject, "useItem", objects59 -> {
            ClientPlayerEntity clientPlayerEntity5 = mc().player;
            if (clientPlayerEntity5 == null) {
                return false;
            }

            Hand hand = "off".equalsIgnoreCase(ScriptValues.argString(objects59, 0, "main")) ? Hand.OFF_HAND : Hand.MAIN_HAND;
            float f = clientPlayerEntity5.getYaw();
            float f2 = clientPlayerEntity5.getPitch();
            PacketQueue.queue(() -> PacketSender.sendSequenced(i -> new PlayerInteractItemC2SPacket(hand, i, f, f2)));
            return true;
        });
        ScriptValues.putFunction(nativeObject, "swing", objects60 -> {
            ClientPlayerEntity clientPlayerEntity6 = mc().player;
            if (clientPlayerEntity6 == null) {
                return null;
            } else if ("off".equalsIgnoreCase(ScriptValues.argString(objects60, 0, "main"))) {
                clientPlayerEntity6.swingHand(Hand.OFF_HAND);
                return null;
            } else {
                clientPlayerEntity6.swingHand(Hand.MAIN_HAND);
                return null;
            }
        });
        ScriptValues.putFunction(nativeObject, "slot", objects61 -> {
            ClientPlayerEntity clientPlayerEntity7 = mc().player;
            if (clientPlayerEntity7 == null) {
                return -1;
            }

            if (!ScriptValues.hasArg(objects61, 0)) {
                return clientPlayerEntity7.getInventory().getSelectedSlot();
            }

            int i2 = Math.clamp(ScriptValues.argInt(objects61, 0, 0), 0, 8);
            InventoryUtil.setSelectedSlot(i2);
            return i2;
        });
        ScriptValues.putFunction(nativeObject, "dropHeld", objects62 -> {
            ClientPlayerEntity clientPlayerEntity8 = mc().player;
            return clientPlayerEntity8 != null && !clientPlayerEntity8.isSpectator() && clientPlayerEntity8.dropSelectedItem(ScriptValues.argBoolean(objects62, 0, false)) ? true : false;
        });
        scriptableObject.put("player", scriptableObject, nativeObject);
        return nativeObject;
    }

    private static void installInventory(NativeObject nativeObject) {
        NativeObject nativeObject2 = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject2, "size", objects -> mc().player == null ? 0 : mc().player.getInventory().size());
        ScriptValues.putFunction(nativeObject2, "selectedSlot", objects2 -> mc().player == null ? 0 : mc().player.getInventory().getSelectedSlot());
        ScriptValues.putFunction(
            nativeObject2,
            "slot",
            objects3 -> {
                ClientPlayerEntity clientPlayerEntity = mc().player;
                if (clientPlayerEntity == null) {
                    return null;
                }

                int i = ScriptValues.argInt(objects3, 0, 0);
                return i >= 0 && i < clientPlayerEntity.getInventory().size()
                    ? ScriptConvertUtil.wrapStack(clientPlayerEntity.getInventory().getStack(i))
                    : ScriptConvertUtil.wrapStack(ItemStack.EMPTY);
            }
        );
        ScriptValues.putFunction(nativeObject2, "all", objects4 -> {
            ArrayList arrayList = new ArrayList();
            ClientPlayerEntity clientPlayerEntity2 = mc().player;
            if (clientPlayerEntity2 != null) {
                int i2 = 0;

                for (int i3 = clientPlayerEntity2.getInventory().size(); i2 < i3; i2++) {
                    arrayList.add(ScriptConvertUtil.wrapStack(clientPlayerEntity2.getInventory().getStack(i2)));
                }
            }

            return ScriptValues.toArray(arrayList);
        });
        ScriptValues.putFunction(nativeObject2, "hotbar", objects5 -> {
            ArrayList arrayList2 = new ArrayList();
            ClientPlayerEntity clientPlayerEntity3 = mc().player;
            if (clientPlayerEntity3 != null) {
                for (int i4 = 0; i4 < 9; i4++) {
                    arrayList2.add(ScriptConvertUtil.wrapStack(clientPlayerEntity3.getInventory().getStack(i4)));
                }
            }

            return ScriptValues.toArray(arrayList2);
        });
        ScriptValues.putFunction(nativeObject2, "armor", objects6 -> mc().player == null ? null : ScriptConvertUtil.wrapEquipment(mc().player).get("armor", null));
        ScriptValues.putFunction(nativeObject2, "find", objects7 -> {
            ClientPlayerEntity clientPlayerEntity4 = mc().player;
            if (clientPlayerEntity4 == null) {
                return -1;
            }

            String string = ScriptValues.argString(objects7, 0);
            int i5 = 0;

            for (int i6 = clientPlayerEntity4.getInventory().size(); i5 < i6; i5++) {
                ItemStack itemStack = clientPlayerEntity4.getInventory().getStack(i5);
                if (!itemStack.isEmpty() && Registries.ITEM.getId(itemStack.getItem()).toString().equals(string)) {
                    return i5;
                }
            }

            return -1;
        });
        ScriptValues.putFunction(nativeObject2, "count", objects8 -> {
            ClientPlayerEntity clientPlayerEntity5 = mc().player;
            if (clientPlayerEntity5 == null) {
                return 0;
            }

            String string2 = ScriptValues.argString(objects8, 0);
            int i7 = 0;
            int i8 = 0;

            for (int i9 = clientPlayerEntity5.getInventory().size(); i8 < i9; i8++) {
                ItemStack itemStack2 = clientPlayerEntity5.getInventory().getStack(i8);
                if (!itemStack2.isEmpty() && Registries.ITEM.getId(itemStack2.getItem()).toString().equals(string2)) {
                    i7 += itemStack2.getCount();
                }
            }

            return i7;
        });
        ScriptValues.putFunction(
            nativeObject2,
            "click",
            objects9 -> {
                ClientPlayerEntity clientPlayerEntity6 = mc().player;
                if (clientPlayerEntity6 != null && mc().interactionManager != null) {
                    mc()
                        .interactionManager
                        .clickSlot(
                            clientPlayerEntity6.currentScreenHandler.syncId,
                            ScriptValues.argInt(objects9, 0, 0),
                            ScriptValues.argInt(objects9, 1, 0),
                            parseSlotAction(ScriptValues.argString(objects9, 2, "PICKUP")),
                            clientPlayerEntity6
                        );
                    return true;
                } else {
                    return false;
                }
            }
        );
        ScriptValues.putFunction(
            nativeObject2,
            "swap",
            objects10 -> {
                ClientPlayerEntity clientPlayerEntity7 = mc().player;
                if (clientPlayerEntity7 != null && mc().interactionManager != null) {
                    mc()
                        .interactionManager
                        .clickSlot(
                            clientPlayerEntity7.currentScreenHandler.syncId, ScriptValues.argInt(objects10, 0, 0), ScriptValues.argInt(objects10, 1, 0), SlotActionType.SWAP, clientPlayerEntity7
                        );
                    return true;
                } else {
                    return false;
                }
            }
        );
        ScriptValues.putFunction(nativeObject2, "drop", objects11 -> {
            ClientPlayerEntity clientPlayerEntity8 = mc().player;
            if (clientPlayerEntity8 != null && mc().interactionManager != null) {
                ClientPlayerInteractionManager clientPlayerInteractionManager = mc().interactionManager;
                int i10 = clientPlayerEntity8.currentScreenHandler.syncId;
                int i11 = ScriptValues.argInt(objects11, 0, 0);
                if (ScriptValues.argBoolean(objects11, 1, false)) {
                    clientPlayerInteractionManager.clickSlot(i10, i11, 1, SlotActionType.THROW, clientPlayerEntity8);
                    return true;
                } else {
                    clientPlayerInteractionManager.clickSlot(i10, i11, 0, SlotActionType.THROW, clientPlayerEntity8);
                    return true;
                }
            } else {
                return false;
            }
        });
        ScriptValues.putFunction(nativeObject2, "swapHand", objects12 -> {
            if (mc().player == null) {
                return false;
            }

            InventoryUtil.swapToHand(ScriptValues.argInt(objects12, 0, -1), ScriptConvertUtil.parseHand(ScriptValues.argString(objects12, 1, "main")));
            return true;
        });
        ScriptValues.putFunction(nativeObject2, "busy", objects13 -> GuiMoveComponent.isActive());
        ScriptValues.putFunction(nativeObject2, "findServerItem", objects14 -> {
            DonateItem donateItem = findServerItem(ScriptValues.argString(objects14, 0));
            if (mc().player != null && donateItem != null) {
                Slot slot = DonateItemRegistry.findSlot(donateItem.getServer().resolve(), donateItem);
                return slot == null ? -1 : slot.id;
            } else {
                return -1;
            }
        });
        ScriptValues.putFunction(nativeObject2, "throwItem", objects15 -> {
            ClientPlayerEntity clientPlayerEntity9 = mc().player;
            ThrowComponent throwComponent = ThrowComponent.getInstance();
            int i12 = ScriptValues.argInt(objects15, 0, -1);
            if (clientPlayerEntity9 != null && throwComponent != null && i12 >= 0 && i12 < clientPlayerEntity9.currentScreenHandler.slots.size()) {
                Slot slot2 = clientPlayerEntity9.currentScreenHandler.getSlot(i12);
                if (ScriptValues.hasArg(objects15, 2)) {
                    throwComponent.useSlotWithRotation(slot2, new Vector2f(ScriptValues.argFloat(objects15, 1, 0.0F), ScriptValues.argFloat(objects15, 2, 0.0F)));
                    return true;
                } else {
                    throwComponent.useSlot(slot2);
                    return true;
                }
            } else {
                return false;
            }
        });
        ScriptValues.putFunction(nativeObject2, "throwing", objects16 -> {
            ThrowComponent throwComponent2 = ThrowComponent.getInstance();
            return throwComponent2 != null && !throwComponent2.isIdle() ? true : false;
        });
        ScriptValues.putFunction(nativeObject2, "close", objects17 -> {
            ClientPlayerEntity clientPlayerEntity10 = mc().player;
            if (clientPlayerEntity10 != null) {
                clientPlayerEntity10.closeHandledScreen();
            }

            return null;
        });
        ScriptValues.putValue(nativeObject, "inventory", nativeObject2);
    }

    private static DonateItem findServerItem(String string) {
        for (DonateItem donateItem : DonateItem.values()) {
            if (donateItem.name().equalsIgnoreCase(string)) {
                return donateItem;
            }
        }

        return null;
    }

    private static SlotActionType parseSlotAction(String string) {
        try {
            return SlotActionType.valueOf(string.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            return SlotActionType.PICKUP;
        }
    }

    private static NativeObject installWorld(ScriptableObject scriptableObject) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "exists", objects -> mc().world != null ? true : false);
        ScriptValues.putFunction(nativeObject, "time", objects2 -> mc().world == null ? 0L : mc().world.getTimeOfDay());
        ScriptValues.putFunction(nativeObject, "dimension", objects3 -> mc().world == null ? "" : mc().world.getRegistryKey().getValue().toString());
        ScriptValues.putFunction(nativeObject, "players", objects4 -> {
            ArrayList arrayList = new ArrayList();
            if (mc().world != null) {
                for (PlayerEntity playerEntity : mc().world.getPlayers()) {
                    if (playerEntity != mc().player) {
                        arrayList.add(wrapEntity(playerEntity));
                    }
                }
            }

            return ScriptValues.toArray(arrayList);
        });
        ScriptValues.putFunction(nativeObject, "entities", objects5 -> {
            ArrayList arrayList2 = new ArrayList();
            if (mc().world != null) {
                for (Entity entity : mc().world.getEntities()) {
                    if (entity != mc().player && entity instanceof LivingEntity) {
                        arrayList2.add(wrapEntity(entity));
                    }
                }
            }

            return ScriptValues.toArray(arrayList2);
        });
        ScriptValues.putFunction(
            nativeObject,
            "blockAt",
            objects6 -> {
                if (mc().world == null) {
                    return "";
                }

                BlockPos blockPos = BlockPos.ofFloored(
                    ScriptValues.argDouble(objects6, 0, 0.0), ScriptValues.argDouble(objects6, 1, 0.0), ScriptValues.argDouble(objects6, 2, 0.0)
                );
                return Registries.BLOCK.getId(mc().world.getBlockState(blockPos).getBlock()).toString();
            }
        );
        ScriptValues.putFunction(
            nativeObject,
            "block",
            objects7 -> {
                if (mc().world == null) {
                    return null;
                }

                BlockPos blockPos2 = BlockPos.ofFloored(
                    ScriptValues.argDouble(objects7, 0, 0.0), ScriptValues.argDouble(objects7, 1, 0.0), ScriptValues.argDouble(objects7, 2, 0.0)
                );
                return ScriptConvertUtil.wrapBlock(blockPos2, mc().world.getBlockState(blockPos2));
            }
        );
        ScriptValues.putFunction(nativeObject, "bottomY", objects8 -> mc().world == null ? 0 : mc().world.getBottomY());
        ScriptValues.putFunction(nativeObject, "raining", objects9 -> mc().world != null && mc().world.isRaining() ? true : false);
        ScriptValues.putFunction(nativeObject, "thundering", objects10 -> mc().world != null && mc().world.isThundering() ? true : false);
        ScriptValues.putFunction(nativeObject, "rainGradient", objects11 -> mc().world == null ? 0.0F : mc().world.getRainGradient(1.0F));
        ScriptValues.putFunction(nativeObject, "day", objects12 -> mc().world == null ? 0L : mc().world.getTimeOfDay() / 24000L);
        ScriptValues.putFunction(nativeObject, "difficulty", objects13 -> mc().world == null ? "" : mc().world.getDifficulty().getName());
        ScriptValues.putFunction(nativeObject, "entityById", objects14 -> {
            if (mc().world == null) {
                return null;
            }

            Entity entity2 = mc().world.getEntityById(ScriptValues.argInt(objects14, 0, -1));
            return entity2 == null ? null : wrapEntity(entity2);
        });
        ScriptValues.putFunction(nativeObject, "findBlocks", objects15 -> {
            ArrayList arrayList3 = new ArrayList();
            if (mc().world != null && mc().player != null) {
                Identifier identifier = Identifier.tryParse(ScriptValues.argString(objects15, 0));
                if (identifier == null) {
                    return ScriptValues.toArray(arrayList3);
                }

                Block block = Registries.BLOCK.get(identifier);
                int i = MathHelper.clamp(ScriptValues.argInt(objects15, 1, 16), 1, 32);
                BlockPos blockPos3 = mc().player.getBlockPos();

                for (BlockPos blockPos4 : BlockPos.iterate(blockPos3.add(-i, -i, -i), blockPos3.add(i, i, i))) {
                    if (mc().world.getBlockState(blockPos4).getBlock() == block) {
                        arrayList3.add(vec(blockPos4.getX(), blockPos4.getY(), blockPos4.getZ()));
                    }
                }
            }

            return ScriptValues.toArray(arrayList3);
        });
        ScriptValues.putFunction(nativeObject, "holes", objects16 -> {
            ArrayList arrayList4 = new ArrayList();
            if (mc().world != null && mc().player != null) {
                int i2 = MathHelper.clamp(ScriptValues.argInt(objects16, 0, 8), 1, 16);
                BlockPos blockPos5 = mc().player.getBlockPos();

                for (BlockPos blockPos6 : HoleFinder.findHoles(blockPos5.add(-i2, -i2, -i2), blockPos5.add(i2, i2, i2))) {
                    arrayList4.add(vec(blockPos6.getX(), blockPos6.getY(), blockPos6.getZ()));
                }
            }

            return ScriptValues.toArray(arrayList4);
        });
        scriptableObject.put("world", scriptableObject, nativeObject);
        return nativeObject;
    }

    public static NativeObject wrapEntity(Entity entity) {
        NativeObject nativeObject = ScriptValues.newObject();
        nativeObject.associateValue(ENTITY_KEY, entity);
        ScriptValues.putFunction(nativeObject, "name", objects -> entity.getName().getString());
        ScriptValues.putFunction(nativeObject, "type", objects2 -> Registries.ENTITY_TYPE.getId(entity.getType()).toString());
        ScriptValues.putFunction(nativeObject, "id", objects3 -> entity.getId());
        ScriptValues.putFunction(nativeObject, "x", objects4 -> entity.getX());
        ScriptValues.putFunction(nativeObject, "y", objects5 -> entity.getY());
        ScriptValues.putFunction(nativeObject, "z", objects6 -> entity.getZ());
        ScriptValues.putFunction(nativeObject, "yaw", objects7 -> entity.getYaw());
        ScriptValues.putFunction(nativeObject, "pitch", objects8 -> entity.getPitch());
        ScriptValues.putFunction(nativeObject, "alive", objects9 -> entity.isAlive());
        ScriptValues.putFunction(nativeObject, "isPlayer", objects10 -> entity instanceof PlayerEntity);
        ScriptValues.putFunction(nativeObject, "isLiving", objects11 -> entity instanceof LivingEntity);
        ScriptValues.putFunction(nativeObject, "prevPos", objects12 -> ScriptConvertUtil.vec(entity.lastX, entity.lastY, entity.lastZ));
        ScriptValues.putFunction(nativeObject, "health", objects13 -> entity instanceof LivingEntity ? ((LivingEntity)entity).getHealth() : 0.0F);
        ScriptValues.putFunction(nativeObject, "maxHealth", objects14 -> entity instanceof LivingEntity ? ((LivingEntity)entity).getMaxHealth() : 0.0F);
        ScriptValues.putFunction(nativeObject, "hurtTime", objects15 -> entity instanceof LivingEntity ? ((LivingEntity)entity).hurtTime : 0);
        ScriptValues.putFunction(nativeObject, "maxHurtTime", objects16 -> entity instanceof LivingEntity ? ((LivingEntity)entity).maxHurtTime : 0);
        ScriptValues.putFunction(nativeObject, "distance", objects17 -> mc().player == null ? 0.0 : (double)entity.distanceTo(mc().player));
        ScriptValues.putFunction(nativeObject, "renderPos", objects18 -> getRenderPos(entity));
        ScriptValues.putFunction(nativeObject, "pos", objects19 -> ScriptConvertUtil.wrapVec(entity.getEntityPos()));
        ScriptValues.putFunction(nativeObject, "velocity", objects20 -> ScriptConvertUtil.wrapVec(entity.getVelocity()));
        ScriptValues.putFunction(nativeObject, "eyeY", objects21 -> entity.getEyeY());
        ScriptValues.putFunction(nativeObject, "width", objects22 -> entity.getWidth());
        ScriptValues.putFunction(nativeObject, "height", objects23 -> entity.getHeight());
        ScriptValues.putFunction(nativeObject, "fallDistance", objects24 -> entity.fallDistance);
        ScriptValues.putFunction(nativeObject, "onGround", objects25 -> entity.isOnGround());
        ScriptValues.putFunction(nativeObject, "sprinting", objects26 -> entity.isSprinting());
        ScriptValues.putFunction(nativeObject, "sneaking", objects27 -> entity.isSneaking());
        ScriptValues.putFunction(nativeObject, "invisible", objects28 -> entity.isInvisible());
        ScriptValues.putFunction(nativeObject, "onFire", objects29 -> entity.isOnFire());
        ScriptValues.putFunction(nativeObject, "inWater", objects30 -> entity.isTouchingWater());
        ScriptValues.putFunction(nativeObject, "gliding", objects31 -> entity instanceof LivingEntity && ((LivingEntity)entity).isGliding() ? true : false);
        ScriptValues.putFunction(nativeObject, "usingItem", objects32 -> entity instanceof LivingEntity && ((LivingEntity)entity).isUsingItem() ? true : false);
        ScriptValues.putFunction(nativeObject, "armor", objects33 -> entity instanceof LivingEntity ? ((LivingEntity)entity).getArmor() : 0);
        ScriptValues.putFunction(nativeObject, "absorption", objects34 -> entity instanceof LivingEntity ? ((LivingEntity)entity).getAbsorptionAmount() : 0.0F);
        ScriptValues.putFunction(nativeObject, "mainHand", objects35 -> entity instanceof LivingEntity livingEntity ? ScriptConvertUtil.wrapStack(livingEntity.getMainHandStack()) : null);
        ScriptValues.putFunction(nativeObject, "offHand", objects36 -> entity instanceof LivingEntity livingEntity2 ? ScriptConvertUtil.wrapStack(livingEntity2.getOffHandStack()) : null);
        ScriptValues.putFunction(nativeObject, "equipment", objects37 -> entity instanceof LivingEntity ? ScriptConvertUtil.wrapEquipment((LivingEntity)entity) : null);
        ScriptValues.putFunction(
            nativeObject, "effects", objects38 -> entity instanceof LivingEntity livingEntity3 ? ScriptConvertUtil.wrapEffects(livingEntity3) : ScriptValues.toArray(List.of())
        );
        ScriptValues.putFunction(nativeObject, "canSee", objects39 -> mc().player != null && mc().player.canSee(entity) ? true : false);
        ScriptValues.putFunction(nativeObject, "box", objects40 -> {
            Box box = entity.getBoundingBox();
            NativeObject nativeObject2 = ScriptValues.newObject();
            ScriptValues.putValue(nativeObject2, "x1", box.minX);
            ScriptValues.putValue(nativeObject2, "y1", box.minY);
            ScriptValues.putValue(nativeObject2, "z1", box.minZ);
            ScriptValues.putValue(nativeObject2, "x2", box.maxX);
            ScriptValues.putValue(nativeObject2, "y2", box.maxY);
            ScriptValues.putValue(nativeObject2, "z2", box.maxZ);
            return nativeObject2;
        });
        return nativeObject;
    }

    public static Entity unwrapEntity(Object object) {
        if (object instanceof NativeObject nativeObject) {
            Object object2 = nativeObject.getAssociatedValue(ENTITY_KEY);
            return object2 instanceof Entity ? (Entity)object2 : null;
        } else {
            return null;
        }
    }

    private static NativeObject getRenderPos(Entity entity) {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        float f = scriptManager == null ? 1.0F : scriptManager.getTickDelta();
        Vec3d vec3d = ProjectionUtil.getInterpolatedPos(entity, f);
        return vec(vec3d.x, vec3d.y, vec3d.z);
    }

    private static NativeObject vec(double d, double d2, double d3) {
        return ScriptConvertUtil.vec(d, d2, d3);
    }

    private static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }
}
