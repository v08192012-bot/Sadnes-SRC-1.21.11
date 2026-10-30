package tech.sadnes.client.script.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPosition;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.api.mixin.accessor.EntityStatusS2CPacketAccessor;
import tech.sadnes.api.mixin.accessor.PlayerInteractEntityC2SPacketAccessor;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.libs.rhino.NativeObject;

public final class ScriptPacketConverter {
    private ScriptPacketConverter() {
    }

    public static NativeObject toScriptObject(Packet<?> packet) {
        Objects.requireNonNull(packet);
        switch (packet) {
            case PlayerPositionLookS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                EntityPosition change = p.change();
                ScriptValues.putValue(obj, "teleportId", p.teleportId());
                ScriptValues.putValue(obj, "x", change.position().x);
                ScriptValues.putValue(obj, "y", change.position().y);
                ScriptValues.putValue(obj, "z", change.position().z);
                ScriptValues.putValue(obj, "yaw", change.yaw());
                ScriptValues.putValue(obj, "pitch", change.pitch());
                ScriptValues.putValue(obj, "velocity", ScriptConvertUtil.wrapVec(change.deltaMovement()));
                List<String> relative = new ArrayList<>();
                for (PositionFlag flag : p.relatives()) {
                    relative.add(flag.name().toLowerCase(Locale.ROOT));
                }
                ScriptValues.putValue(obj, "relative", ScriptValues.toArray(relative));
                return obj;
            }
            case EntityVelocityUpdateS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                putEntity(obj, p.getEntityId());
                ScriptValues.putValue(obj, "velocity", ScriptConvertUtil.wrapVec(p.getVelocity()));
                return obj;
            }
            case ExplosionS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "x", p.center().x);
                ScriptValues.putValue(obj, "y", p.center().y);
                ScriptValues.putValue(obj, "z", p.center().z);
                p.playerKnockback().ifPresent(knockback -> ScriptValues.putValue(obj, "knockback", ScriptConvertUtil.wrapVec(knockback)));
                return obj;
            }
            case HealthUpdateS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "health", p.getHealth());
                ScriptValues.putValue(obj, "food", p.getFood());
                ScriptValues.putValue(obj, "saturation", p.getSaturation());
                return obj;
            }
            case EntityStatusS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                putEntity(obj, ((EntityStatusS2CPacketAccessor) (Object) p).sadnes$getEntityId());
                ScriptValues.putValue(obj, "status", (int) p.getStatus());
                return obj;
            }
            case EntityStatusEffectS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                putEntity(obj, p.getEntityId());
                ScriptValues.putValue(obj, "effect", effectId(p.getEffectId()));
                ScriptValues.putValue(obj, "amplifier", p.getAmplifier());
                ScriptValues.putValue(obj, "level", p.getAmplifier() + 1);
                ScriptValues.putValue(obj, "duration", p.getDuration());
                return obj;
            }
            case RemoveEntityStatusEffectS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "entityId", p.entityId());
                ScriptValues.putValue(obj, "effect", effectId(p.effect()));
                return obj;
            }
            case UpdateSelectedSlotS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "slot", p.slot());
                return obj;
            }
            case BlockUpdateS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                BlockPos pos = p.getPos();
                ScriptValues.putValue(obj, "x", pos.getX());
                ScriptValues.putValue(obj, "y", pos.getY());
                ScriptValues.putValue(obj, "z", pos.getZ());
                ScriptValues.putValue(obj, "block", Registries.BLOCK.getId(p.getState().getBlock()).toString());
                return obj;
            }
            case GameMessageS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "text", p.content().getString());
                ScriptValues.putValue(obj, "overlay", p.overlay());
                return obj;
            }
            case OverlayMessageS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "text", p.text().getString());
                return obj;
            }
            case TitleS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "text", p.text().getString());
                return obj;
            }
            case SubtitleS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "text", p.text().getString());
                return obj;
            }
            case EntityDamageS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                putEntity(obj, p.entityId());
                ScriptValues.putValue(obj, "source", effectId(p.sourceType()));
                return obj;
            }
            case ScreenHandlerSlotUpdateS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "syncId", p.getSyncId());
                ScriptValues.putValue(obj, "slot", p.getSlot());
                ScriptValues.putValue(obj, "item", ScriptConvertUtil.wrapStack(p.getStack()));
                return obj;
            }
            case InventoryS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "syncId", p.syncId());
                ScriptValues.putValue(obj, "size", p.contents().size());
                return obj;
            }
            case OpenScreenS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "syncId", p.getSyncId());
                ScriptValues.putValue(obj, "title", p.getName().getString());
                return obj;
            }
            case CloseScreenS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "syncId", p.getSyncId());
                return obj;
            }
            case EntityAnimationS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                putEntity(obj, p.getEntityId());
                ScriptValues.putValue(obj, "animation", p.getAnimationId());
                return obj;
            }
            case PlaySoundS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "sound", effectId(p.getSound()));
                ScriptValues.putValue(obj, "x", p.getX());
                ScriptValues.putValue(obj, "y", p.getY());
                ScriptValues.putValue(obj, "z", p.getZ());
                ScriptValues.putValue(obj, "volume", p.getVolume());
                ScriptValues.putValue(obj, "pitch", p.getPitch());
                return obj;
            }
            case EntitySpawnS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "entityId", p.getEntityId());
                ScriptValues.putValue(obj, "type", Registries.ENTITY_TYPE.getId(p.getEntityType()).toString());
                ScriptValues.putValue(obj, "x", p.getX());
                ScriptValues.putValue(obj, "y", p.getY());
                ScriptValues.putValue(obj, "z", p.getZ());
                return obj;
            }
            case EntitiesDestroyS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "ids", ScriptValues.toArray(p.getEntityIds()));
                return obj;
            }
            case DeathMessageS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "playerId", p.playerId());
                ScriptValues.putValue(obj, "text", p.message().getString());
                return obj;
            }
            case CommonPingS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "id", p.getParameter());
                return obj;
            }
            case KeepAliveS2CPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "id", p.getId());
                return obj;
            }
            case PlayerMoveC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ClientPlayerEntity player = MinecraftClient.getInstance().player;
                double currentX = player == null ? 0.0 : player.getX();
                double currentY = player == null ? 0.0 : player.getY();
                double currentZ = player == null ? 0.0 : player.getZ();
                float currentYaw = player == null ? 0.0F : player.getYaw();
                float currentPitch = player == null ? 0.0F : player.getPitch();
                ScriptValues.putValue(obj, "x", p.getX(currentX));
                ScriptValues.putValue(obj, "y", p.getY(currentY));
                ScriptValues.putValue(obj, "z", p.getZ(currentZ));
                ScriptValues.putValue(obj, "yaw", p.getYaw(currentYaw));
                ScriptValues.putValue(obj, "pitch", p.getPitch(currentPitch));
                ScriptValues.putValue(obj, "onGround", p.isOnGround());
                ScriptValues.putValue(obj, "horizontalCollision", p.horizontalCollision());
                ScriptValues.putValue(obj, "changesPosition", p.changesPosition());
                ScriptValues.putValue(obj, "changesLook", p.changesLook());
                return obj;
            }
            case UpdateSelectedSlotC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "slot", p.getSelectedSlot());
                return obj;
            }
            case PlayerInteractBlockC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                BlockHitResult hit = p.getBlockHitResult();
                BlockPos pos = hit.getBlockPos();
                ScriptValues.putValue(obj, "x", pos.getX());
                ScriptValues.putValue(obj, "y", pos.getY());
                ScriptValues.putValue(obj, "z", pos.getZ());
                ScriptValues.putValue(obj, "side", hit.getSide().name().toLowerCase(Locale.ROOT));
                ScriptValues.putValue(obj, "hand", ScriptConvertUtil.handName(p.getHand()));
                ScriptValues.putValue(obj, "hit", ScriptConvertUtil.wrapVec(hit.getPos()));
                ScriptValues.putValue(obj, "sequence", p.getSequence());
                return obj;
            }
            case PlayerInteractItemC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "hand", ScriptConvertUtil.handName(p.getHand()));
                ScriptValues.putValue(obj, "yaw", p.getYaw());
                ScriptValues.putValue(obj, "pitch", p.getPitch());
                ScriptValues.putValue(obj, "sequence", p.getSequence());
                return obj;
            }
            case PlayerActionC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                BlockPos pos = p.getPos();
                ScriptValues.putValue(obj, "action", p.getAction().name().toLowerCase(Locale.ROOT));
                ScriptValues.putValue(obj, "x", pos.getX());
                ScriptValues.putValue(obj, "y", pos.getY());
                ScriptValues.putValue(obj, "z", pos.getZ());
                ScriptValues.putValue(obj, "side", p.getDirection().name().toLowerCase(Locale.ROOT));
                ScriptValues.putValue(obj, "sequence", p.getSequence());
                return obj;
            }
            case HandSwingC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "hand", ScriptConvertUtil.handName(p.getHand()));
                return obj;
            }
            case PlayerInteractEntityC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                putEntity(obj, ((PlayerInteractEntityC2SPacketAccessor) (Object) p).sadnes$getEntityId());
                String[] type = new String[]{"attack"};
                String[] hand = new String[]{null};
                p.handle(new PlayerInteractEntityC2SPacket.Handler() {
                    @Override
                    public void interact(Hand interactHand) {
                        type[0] = "interact";
                        hand[0] = ScriptConvertUtil.handName(interactHand);
                    }

                    @Override
                    public void interactAt(Hand interactHand, Vec3d pos) {
                        type[0] = "interact_at";
                        hand[0] = ScriptConvertUtil.handName(interactHand);
                    }

                    @Override
                    public void attack() {
                        type[0] = "attack";
                    }
                });
                ScriptValues.putValue(obj, "type", type[0]);
                if (hand[0] != null) {
                    ScriptValues.putValue(obj, "hand", hand[0]);
                }
                return obj;
            }
            case ClickSlotC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "syncId", p.syncId());
                ScriptValues.putValue(obj, "slot", Integer.valueOf(p.slot()));
                ScriptValues.putValue(obj, "button", Integer.valueOf(p.button()));
                ScriptValues.putValue(obj, "action", p.actionType().name().toLowerCase(Locale.ROOT));
                return obj;
            }
            case ClientCommandC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "mode", p.getMode().name().toLowerCase(Locale.ROOT));
                return obj;
            }
            case ChatMessageC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "text", p.chatMessage());
                return obj;
            }
            case CommandExecutionC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "command", p.command());
                return obj;
            }
            case PlayerInputC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                PlayerInput input = p.input();
                ScriptValues.putValue(obj, "forward", input.forward());
                ScriptValues.putValue(obj, "backward", input.backward());
                ScriptValues.putValue(obj, "left", input.left());
                ScriptValues.putValue(obj, "right", input.right());
                ScriptValues.putValue(obj, "jump", input.jump());
                ScriptValues.putValue(obj, "sneak", input.sneak());
                ScriptValues.putValue(obj, "sprint", input.sprint());
                return obj;
            }
            case CloseHandledScreenC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "syncId", p.getSyncId());
                return obj;
            }
            case TeleportConfirmC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "teleportId", p.getTeleportId());
                return obj;
            }
            case CommonPongC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "id", p.getParameter());
                return obj;
            }
            case KeepAliveC2SPacket p -> {
                NativeObject obj = ScriptValues.newObject();
                ScriptValues.putValue(obj, "id", p.getId());
                return obj;
            }
            default -> {
                return null;
            }
        }
    }

    private static void putEntity(NativeObject nativeObject, int i) {
        ScriptValues.putValue(nativeObject, "entityId", i);
        ClientWorld clientWorld = MinecraftClient.getInstance().world;
        if (clientWorld != null) {
            Entity entity = clientWorld.getEntityById(i);
            if (entity != null) {
                ScriptValues.putValue(nativeObject, "entity", ScriptPlayerApi.wrapEntity(entity));
            }
        }
    }

    private static String effectId(RegistryEntry<?> registryEntry) {
        return registryEntry.getKey().map(registryKey -> registryKey.getValue().toString()).orElse("");
    }
}
