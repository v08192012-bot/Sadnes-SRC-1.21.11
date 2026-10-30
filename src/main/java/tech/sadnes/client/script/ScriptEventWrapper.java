package tech.sadnes.client.script;

import java.util.Locale;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.Vec3d;
import tech.sadnes.client.event.Cancellable;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.BlockCollisionEvent;
import tech.sadnes.client.event.impl.BlockUpdateEvent;
import tech.sadnes.client.event.impl.CameraRotationEvent;
import tech.sadnes.client.event.impl.CloseScreenEvent;
import tech.sadnes.client.event.impl.CloudRenderEvent;
import tech.sadnes.client.event.impl.EntityBoundingBoxEvent;
import tech.sadnes.client.event.impl.EntityRaycastEvent;
import tech.sadnes.client.event.impl.EntityRemoveEvent;
import tech.sadnes.client.event.impl.FireworkBoostEvent;
import tech.sadnes.client.event.impl.FogColorEvent;
import tech.sadnes.client.event.impl.FogDistanceEvent;
import tech.sadnes.client.event.impl.HandledScreenRenderEvent;
import tech.sadnes.client.event.impl.ItemPickupEvent;
import tech.sadnes.client.event.impl.MouseLookEvent;
import tech.sadnes.client.event.impl.MoveEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.PlayerInputPacketEvent;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.Render3DEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.ScreenMouseScrollEvent;
import tech.sadnes.client.event.impl.SetScreenEvent;
import tech.sadnes.client.event.impl.SkyColorEvent;
import tech.sadnes.client.event.impl.SkyRenderEvent;
import tech.sadnes.client.event.impl.SlotClickEvent;
import tech.sadnes.client.event.impl.SlotTooltipEvent;
import tech.sadnes.client.event.impl.SlowdownEvent;
import tech.sadnes.client.event.impl.SprintEvent;
import tech.sadnes.client.event.impl.SprintResetEvent;
import tech.sadnes.client.event.impl.StopUsingItemEvent;
import tech.sadnes.client.event.impl.SyncedOptionsEvent;
import tech.sadnes.client.event.impl.TargetingMarginEvent;
import tech.sadnes.client.event.impl.TextEvent;
import tech.sadnes.client.event.impl.UseItemEvent;
import tech.sadnes.client.event.impl.UsingItemChangeEvent;
import tech.sadnes.client.event.impl.WeatherEvent;
import tech.sadnes.client.event.impl.WorldTimeEvent;
import tech.sadnes.client.script.api.ScriptConvertUtil;
import tech.sadnes.client.script.api.ScriptPacketConverter;
import tech.sadnes.client.script.api.ScriptPlayerApi;
import tech.sadnes.client.util.player.MoveUtil;
import tech.sadnes.libs.rhino.NativeObject;

public final class ScriptEventWrapper {
    private ScriptEventWrapper() {
    }

    public static NativeObject wrapEvent(Object event) {
        NativeObject object = ScriptValues.newObject();
        if (event instanceof Cancellable cancellable) {
            ScriptValues.putFunction(object, "cancel", args -> {
                cancellable.setCancelled(ScriptValues.argBoolean(args, 0, true));
                return null;
            });
            ScriptValues.putFunction(object, "cancelled", args -> cancellable.isCancelled());
        }

        switch (event) {
            case MoveEvent e -> {
                ScriptValues.putValue(object, "x", e.getX());
                ScriptValues.putValue(object, "y", e.getY());
                ScriptValues.putValue(object, "z", e.getZ());
                ScriptValues.putValue(object, "onGround", e.isOnGround());
                ScriptValues.putFunction(object, "setX", args -> {
                    e.setX(ScriptValues.argDouble(args, 0, e.getX()));
                    return null;
                });
                ScriptValues.putFunction(object, "setY", args -> {
                    e.setY(ScriptValues.argDouble(args, 0, e.getY()));
                    return null;
                });
                ScriptValues.putFunction(object, "setZ", args -> {
                    e.setZ(ScriptValues.argDouble(args, 0, e.getZ()));
                    return null;
                });
                ScriptValues.putFunction(object, "setOnGround", args -> {
                    e.setOnGround(ScriptValues.argBoolean(args, 0, e.isOnGround()));
                    return null;
                });
                return object;
            }
            case MoveInputEvent e -> {
                ScriptValues.putValue(object, "forward", e.getForward());
                ScriptValues.putValue(object, "strafe", e.getStrafe());
                ScriptValues.putValue(object, "jump", e.isJump());
                ScriptValues.putValue(object, "sneaking", e.isSneak());
                ScriptValues.putValue(object, "sneakSlow", e.getSneakFactor());
                ScriptValues.putValue(object, "moving", e.isMoving());
                ScriptValues.putFunction(object, "setForward", args -> {
                    e.setForward(ScriptValues.argFloat(args, 0, e.getForward()));
                    return null;
                });
                ScriptValues.putFunction(object, "setStrafe", args -> {
                    e.setStrafe(ScriptValues.argFloat(args, 0, e.getStrafe()));
                    return null;
                });
                ScriptValues.putFunction(object, "setJump", args -> {
                    e.setJump(ScriptValues.argBoolean(args, 0, e.isJump()));
                    return null;
                });
                ScriptValues.putFunction(object, "setSneaking", args -> {
                    e.setSneak(ScriptValues.argBoolean(args, 0, e.isSneak()));
                    return null;
                });
                ScriptValues.putFunction(object, "setSneakSlow", args -> {
                    e.setSneakFactor(ScriptValues.argDouble(args, 0, e.getSneakFactor()));
                    return null;
                });
                ScriptValues.putFunction(object, "steer", args -> {
                    MoveUtil.steerToDirection(e, new Vec3d(ScriptValues.argDouble(args, 0, 0.0), 0.0, ScriptValues.argDouble(args, 1, 0.0)));
                    return null;
                });
                return object;
            }
            case PlayerInputPacketEvent e -> {
                ScriptValues.putFunction(object, "jump", args -> {
                    e.setJump(ScriptValues.argBoolean(args, 0, true));
                    return null;
                });
                ScriptValues.putFunction(object, "sneak", args -> {
                    e.setSneak(ScriptValues.argBoolean(args, 0, true));
                    return null;
                });
                ScriptValues.putFunction(object, "sprint", args -> {
                    e.setSprint(ScriptValues.argBoolean(args, 0, true));
                    return null;
                });
                return object;
            }
            case CameraRotationEvent e -> {
                ScriptValues.putValue(object, "yaw", e.getYaw());
                ScriptValues.putValue(object, "pitch", e.getPitch());
                ScriptValues.putValue(object, "partial", e.getTickProgress());
                ScriptValues.putFunction(object, "setYaw", args -> {
                    e.setYaw(ScriptValues.argFloat(args, 0, e.getYaw()));
                    return null;
                });
                ScriptValues.putFunction(object, "setPitch", args -> {
                    e.setPitch(ScriptValues.argFloat(args, 0, e.getPitch()));
                    return null;
                });
                return object;
            }
            case MouseLookEvent e -> {
                ScriptValues.putValue(object, "yaw", e.getDeltaX());
                ScriptValues.putValue(object, "pitch", e.getDeltaY());
                ScriptValues.putFunction(object, "setYaw", args -> {
                    e.setDeltaX(ScriptValues.argDouble(args, 0, e.getDeltaX()));
                    return null;
                });
                ScriptValues.putFunction(object, "setPitch", args -> {
                    e.setDeltaY(ScriptValues.argDouble(args, 0, e.getDeltaY()));
                    return null;
                });
                return object;
            }
            case AttackEntityEvent e -> {
                ScriptValues.putValue(object, "target", e.getTarget() == null ? null : ScriptPlayerApi.wrapEntity(e.getTarget()));
                ScriptValues.putValue(object, "post", e.getStage() == AttackEntityEvent.Stage.POST);
                ScriptValues.putValue(object, "stage", e.getStage().name().toLowerCase(Locale.ROOT));
                return object;
            }
            case EntityRaycastEvent e -> {
                if (e.getEntity() == null) {
                    ScriptValues.putValue(object, "entity", null);
                    return object;
                }
                ScriptValues.putValue(object, "entity", ScriptPlayerApi.wrapEntity(e.getEntity()));
                return object;
            }
            case TargetingMarginEvent e -> {
                ScriptValues.putValue(object, "entity", ScriptPlayerApi.wrapEntity(e.getEntity()));
                ScriptValues.putValue(object, "size", e.getMargin());
                ScriptValues.putFunction(object, "setSize", args -> {
                    e.setMargin(ScriptValues.argFloat(args, 0, e.getMargin()));
                    return null;
                });
                return object;
            }
            case UsingItemChangeEvent e -> {
                ScriptValues.putValue(object, "entity", ScriptPlayerApi.wrapEntity(e.getEntity()));
                ScriptValues.putValue(object, "using", e.isUsingItem());
                return object;
            }
            case EntityRemoveEvent e -> {
                ScriptValues.putValue(object, "entity", e.getEntity() == null ? null : ScriptPlayerApi.wrapEntity(e.getEntity()));
                ScriptValues.putValue(object, "id", e.getEntityId());
                if (e.getReason() == null) {
                    ScriptValues.putValue(object, "reason", "");
                    return object;
                }
                ScriptValues.putValue(object, "reason", e.getReason().name().toLowerCase(Locale.ROOT));
                return object;
            }
            case ItemPickupEvent e -> {
                ScriptValues.putValue(object, "collector", e.getCollector() == null ? null : ScriptPlayerApi.wrapEntity(e.getCollector()));
                ScriptValues.putValue(object, "item", e.getItem() == null ? null : ScriptPlayerApi.wrapEntity(e.getItem()));
                ScriptValues.putValue(object, "count", e.getAmount());
                return object;
            }
            case EntityBoundingBoxEvent e -> {
                ScriptValues.putValue(object, "entity", ScriptPlayerApi.wrapEntity(e.getEntity()));
                ScriptValues.putValue(object, "box", ScriptConvertUtil.wrapBox(e.getBox()));
                return object;
            }
            case BlockUpdateEvent e -> {
                ScriptValues.putValue(object, "block", ScriptConvertUtil.wrapBlock(e.getPos(), e.getState()));
                ScriptValues.putValue(object, "type", e.getType().name().toLowerCase(Locale.ROOT));
                return object;
            }
            case BlockCollisionEvent e -> {
                ScriptValues.putValue(object, "block", ScriptConvertUtil.wrapBlock(e.getPos(), e.getState()));
                return object;
            }
            case SlotClickEvent e -> {
                ScriptValues.putValue(object, "windowId", e.getSyncId());
                ScriptValues.putValue(object, "slot", e.getSlotId());
                ScriptValues.putValue(object, "button", e.getButton());
                ScriptValues.putValue(object, "action", String.valueOf(e.getActionType()).toLowerCase(Locale.ROOT));
                return object;
            }
            case UseItemEvent e -> {
                ScriptValues.putValue(object, "type", e.getStage().name().toLowerCase(Locale.ROOT));
                if (e.getHand() == null) {
                    ScriptValues.putValue(object, "hand", "main");
                    return object;
                }
                ScriptValues.putValue(object, "hand", ScriptConvertUtil.handName(e.getHand()));
                return object;
            }
            case StopUsingItemEvent e -> {
                ScriptValues.putValue(object, "item", ScriptConvertUtil.wrapStack(e.getStack()));
                return object;
            }
            case PacketEvent e -> {
                ScriptValues.putValue(object, "name", packetName(e.getPacket()));
                ScriptValues.putValue(object, "send", e.isSend());
                ScriptValues.putValue(object, "receive", e.isReceive());
                ScriptValues.putValue(object, "ghost", e.isGhost());
                ScriptValues.putFunction(object, "data", args -> ScriptPacketConverter.toScriptObject(e.getPacket()));
                return object;
            }
            case ScreenMouseClickEvent e -> {
                ScriptValues.putValue(object, "key", e.getButton());
                ScriptValues.putValue(object, "scanCode", e.getMouseX());
                ScriptValues.putValue(object, "modifiers", e.getMouseY());
                ScriptValues.putValue(object, "press", e.isPress());
                ScriptValues.putValue(object, "release", e.isRelease());
                if (e.getScreen() != null) {
                    ScriptValues.putValue(object, "screenOpen", true);
                    return object;
                }
                ScriptValues.putValue(object, "screenOpen", false);
                return object;
            }
            case ScreenKeyEvent e -> {
                ScriptValues.putValue(object, "button", e.getKey());
                ScriptValues.putValue(object, "modifiers", e.getModifiers());
                ScriptValues.putValue(object, "press", e.isPress());
                ScriptValues.putValue(object, "release", e.isRelease());
                ScriptValues.putValue(object, "x", e.getMouseX());
                ScriptValues.putValue(object, "y", e.getMouseY());
                if (e.getScreen() != null) {
                    ScriptValues.putValue(object, "screenOpen", true);
                    return object;
                }
                ScriptValues.putValue(object, "screenOpen", false);
                return object;
            }
            case ScreenMouseScrollEvent e -> {
                ScriptValues.putValue(object, "vertical", e.getVerticalAmount());
                ScriptValues.putValue(object, "horizontal", e.getHorizontalAmount());
                ScriptValues.putValue(object, "x", e.getMouseX());
                ScriptValues.putValue(object, "y", e.getMouseY());
                if (e.getScreen() != null) {
                    ScriptValues.putValue(object, "screenOpen", true);
                    return object;
                }
                ScriptValues.putValue(object, "screenOpen", false);
                return object;
            }
            case SprintEvent e -> {
                ScriptValues.putValue(object, "stage", e.getStage().name().toLowerCase(Locale.ROOT));
                ScriptValues.putValue(object, "forced", e.isSprinting());
                ScriptValues.putFunction(object, "setForced", args -> {
                    e.setSprinting(ScriptValues.argBoolean(args, 0, true));
                    return null;
                });
                return object;
            }
            case SprintResetEvent e -> {
                ScriptValues.putValue(object, "stage", e.getReason().name().toLowerCase(Locale.ROOT));
                return object;
            }
            case FireworkBoostEvent e -> {
                ScriptValues.putValue(object, "shooter", e.getEntity() == null ? null : ScriptPlayerApi.wrapEntity(e.getEntity()));
                ScriptValues.putValue(object, "multiplierXZ", e.getHorizontalSpeed());
                ScriptValues.putValue(object, "multiplierY", e.getVerticalSpeed());
                ScriptValues.putFunction(object, "setMultipliers", args -> {
                    e.setSpeed(ScriptValues.argDouble(args, 0, e.getHorizontalSpeed()), ScriptValues.argDouble(args, 1, e.getVerticalSpeed()));
                    return null;
                });
                return object;
            }
            case SlowdownEvent e -> {
                ScriptValues.putValue(object, "slowCrawling", e.isCrawlSlowdown());
                ScriptValues.putValue(object, "slowSneaking", e.isSneakSlowdown());
                ScriptValues.putFunction(object, "setSlowCrawling", args -> {
                    e.setCrawlSlowdown(ScriptValues.argBoolean(args, 0, true));
                    return null;
                });
                ScriptValues.putFunction(object, "setSlowSneaking", args -> {
                    e.setSneakSlowdown(ScriptValues.argBoolean(args, 0, true));
                    return null;
                });
                return object;
            }
            case SyncedOptionsEvent e -> {
                ScriptValues.putValue(object, "language", e.getLanguage());
                ScriptValues.putValue(object, "viewDistance", e.getViewDistance());
                ScriptValues.putValue(object, "modelParts", e.getPlayerModelParts());
                ScriptValues.putFunction(object, "setLanguage", args -> {
                    e.setLanguage(ScriptValues.argString(args, 0, e.getLanguage()));
                    return null;
                });
                ScriptValues.putFunction(object, "setViewDistance", args -> {
                    e.setViewDistance(ScriptValues.argInt(args, 0, e.getViewDistance()));
                    return null;
                });
                return object;
            }
            case HandledScreenRenderEvent e -> {
                ScriptValues.putValue(object, "width", e.getBackgroundWidth());
                ScriptValues.putValue(object, "height", e.getBackgroundHeight());
                if (e.getFocusedSlot() == null) {
                    ScriptValues.putValue(object, "slot", -1);
                    return object;
                }
                ScriptValues.putValue(object, "slot", e.getFocusedSlot().id);
                return object;
            }
            case SlotTooltipEvent e -> {
                ScriptValues.putValue(object, "x", e.getMouseX());
                ScriptValues.putValue(object, "y", e.getMouseY());
                if (e.getFocusedSlot() == null) {
                    ScriptValues.putValue(object, "slot", -1);
                    return object;
                }
                ScriptValues.putValue(object, "slot", e.getFocusedSlot().id);
                return object;
            }
            case CloseScreenEvent e -> {
                ScriptValues.putValue(object, "screen", ScriptConvertUtil.getScreenName(e.getScreen()));
                return object;
            }
            case SetScreenEvent e -> {
                ScriptValues.putValue(object, "screen", ScriptConvertUtil.getScreenName(e.screen));
                return object;
            }
            case TextEvent e -> {
                ScriptValues.putValue(object, "text", e.getText());
                ScriptValues.putFunction(object, "setText", args -> {
                    e.setText(ScriptValues.argString(args, 0, e.getText()));
                    return null;
                });
                ScriptValues.putFunction(object, "replace", args -> {
                    e.replace(ScriptValues.argString(args, 0), ScriptValues.argString(args, 1));
                    return null;
                });
                return object;
            }
            case Render2DEvent e -> {
                ScriptValues.putValue(object, "partial", e.getTickDelta());
                ScriptValues.putValue(object, "mouseX", e.getMouseX());
                ScriptValues.putValue(object, "mouseY", e.getMouseY());
                return object;
            }
            case Render3DEvent e -> {
                ScriptValues.putValue(object, "partial", e.getTickDelta());
                return object;
            }
            case RenderEvent e -> {
                ScriptValues.putValue(object, "stage", e.getType().name().toLowerCase(Locale.ROOT));
                ScriptValues.putValue(object, "partial", e.getTickDelta());
                return object;
            }
            case WorldTimeEvent e -> {
                ScriptValues.putValue(object, "timeOfDay", e.getTime());
                ScriptValues.putFunction(object, "setTimeOfDay", args -> {
                    e.setTime((long) ScriptValues.argDouble(args, 0, e.getTime()));
                    return null;
                });
                return object;
            }
            case WeatherEvent e -> {
                ScriptValues.putValue(object, "type", e.getType().name().toLowerCase(Locale.ROOT));
                ScriptValues.putValue(object, "gradient", e.getGradient());
                ScriptValues.putFunction(object, "setGradient", args -> {
                    e.setGradient(ScriptValues.argFloat(args, 0, e.getGradient()));
                    return null;
                });
                return object;
            }
            case SkyColorEvent e -> {
                ScriptValues.putValue(object, "color", e.getColor());
                ScriptValues.putValue(object, "partial", e.getTickDelta());
                ScriptValues.putFunction(object, "setColor", args -> {
                    e.setColor(ScriptValues.argIntOrDefault(args, 0, e.getColor()));
                    return null;
                });
                return object;
            }
            case FogColorEvent e -> {
                ScriptValues.putValue(object, "color", e.getColor());
                ScriptValues.putValue(object, "viewDistance", e.getViewDistance());
                ScriptValues.putValue(object, "skyDarkness", e.getSkyDarkness());
                ScriptValues.putFunction(object, "setColor", args -> {
                    e.setColor(ScriptValues.argIntOrDefault(args, 0, e.getColor()));
                    return null;
                });
                return object;
            }
            case FogDistanceEvent e -> {
                ScriptValues.putValue(object, "start", e.getEnvironmentalStart());
                ScriptValues.putValue(object, "end", e.getRenderDistanceStart());
                ScriptValues.putValue(object, "renderStart", e.getEnvironmentalEnd());
                ScriptValues.putValue(object, "renderEnd", e.getRenderDistanceEnd());
                ScriptValues.putFunction(object, "setRange", args -> {
                    e.setEnvironmentalStart(ScriptValues.argFloat(args, 0, e.getEnvironmentalStart()));
                    e.setRenderDistanceStart(ScriptValues.argFloat(args, 1, e.getRenderDistanceStart()));
                    return null;
                });
                return object;
            }
            case CloudRenderEvent e -> {
                ScriptValues.putValue(object, "color", e.getColor());
                ScriptValues.putValue(object, "height", e.getHeight());
                ScriptValues.putValue(object, "speed", e.getSpeed());
                ScriptValues.putFunction(object, "setColor", args -> {
                    e.setColor(ScriptValues.argIntOrDefault(args, 0, e.getColor()));
                    return null;
                });
                ScriptValues.putFunction(object, "setHeight", args -> {
                    e.setHeight(ScriptValues.argFloat(args, 0, e.getHeight()));
                    return null;
                });
                ScriptValues.putFunction(object, "setSpeed", args -> {
                    e.setSpeed(ScriptValues.argFloat(args, 0, e.getSpeed()));
                    return null;
                });
                return object;
            }
            case SkyRenderEvent e -> {
                ScriptValues.putValue(object, "color", e.getColor());
                ScriptValues.putValue(object, "brightness", e.getAlpha());
                ScriptValues.putValue(object, "rotation", e.getRotation());
                ScriptValues.putValue(object, "partial", e.getTickDelta());
                return object;
            }
            default -> {
                return object;
            }
        }
    }

    private static String packetName(Packet<?> packet) {
        try {
            return packet.getPacketType().id().getPath();
        } catch (Exception exception) {
            return "unknown";
        }
    }
}
