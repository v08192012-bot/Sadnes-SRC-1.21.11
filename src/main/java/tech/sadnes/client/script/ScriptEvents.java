package tech.sadnes.client.script;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import tech.sadnes.client.event.impl.AttackEntityEvent;
import tech.sadnes.client.event.impl.BlockCollisionEvent;
import tech.sadnes.client.event.impl.BlockUpdateEvent;
import tech.sadnes.client.event.impl.CameraRotationEvent;
import tech.sadnes.client.event.impl.CloseScreenEvent;
import tech.sadnes.client.event.impl.CloudRenderEvent;
import tech.sadnes.client.event.impl.DeathScreenEvent;
import tech.sadnes.client.event.impl.EntityBoundingBoxEvent;
import tech.sadnes.client.event.impl.EntityRaycastEvent;
import tech.sadnes.client.event.impl.EntityRemoveEvent;
import tech.sadnes.client.event.impl.FireworkBoostEvent;
import tech.sadnes.client.event.impl.FogColorEvent;
import tech.sadnes.client.event.impl.FogDistanceEvent;
import tech.sadnes.client.event.impl.GameJoinEvent;
import tech.sadnes.client.event.impl.HandledScreenRenderEvent;
import tech.sadnes.client.event.impl.HudPostRenderEvent;
import tech.sadnes.client.event.impl.ItemPickupEvent;
import tech.sadnes.client.event.impl.JumpEvent;
import tech.sadnes.client.event.impl.MouseLookEvent;
import tech.sadnes.client.event.impl.MoveEvent;
import tech.sadnes.client.event.impl.MoveInputEvent;
import tech.sadnes.client.event.impl.PacketEvent;
import tech.sadnes.client.event.impl.PlayerInputPacketEvent;
import tech.sadnes.client.event.impl.PlayerTickEvent;
import tech.sadnes.client.event.impl.PostMotionEvent;
import tech.sadnes.client.event.impl.PreRotationEvent;
import tech.sadnes.client.event.impl.Render2DEvent;
import tech.sadnes.client.event.impl.Render3DEvent;
import tech.sadnes.client.event.impl.RenderEvent;
import tech.sadnes.client.event.impl.ScreenKeyEvent;
import tech.sadnes.client.event.impl.ScreenMouseClickEvent;
import tech.sadnes.client.event.impl.ScreenMouseScrollEvent;
import tech.sadnes.client.event.impl.SendMovementPacketsEvent;
import tech.sadnes.client.event.impl.SetScreenEvent;
import tech.sadnes.client.event.impl.SkyColorEvent;
import tech.sadnes.client.event.impl.SkyRenderEvent;
import tech.sadnes.client.event.impl.SlimeBounceEvent;
import tech.sadnes.client.event.impl.SlotClickEvent;
import tech.sadnes.client.event.impl.SlotTooltipEvent;
import tech.sadnes.client.event.impl.SlowdownEvent;
import tech.sadnes.client.event.impl.SprintEvent;
import tech.sadnes.client.event.impl.SprintResetEvent;
import tech.sadnes.client.event.impl.StopUsingItemEvent;
import tech.sadnes.client.event.impl.SyncedOptionsEvent;
import tech.sadnes.client.event.impl.TargetingMarginEvent;
import tech.sadnes.client.event.impl.TextEvent;
import tech.sadnes.client.event.impl.TickEvent;
import tech.sadnes.client.event.impl.UseItemEvent;
import tech.sadnes.client.event.impl.UsingItemChangeEvent;
import tech.sadnes.client.event.impl.WeatherEvent;
import tech.sadnes.client.event.impl.WorldTickEvent;
import tech.sadnes.client.event.impl.WorldTimeEvent;

public final class ScriptEvents {
    public static final Set<String> HOT_EVENTS = Set.of("tick", "render2d", "render3d", "packet");
    public static final Set<String> CUSTOM_EVENTS = Set.of(
        "chat",
        "block_interact",
        "health_change",
        "tab_update",
        "enable",
        "disable",
        "clickgui_bg",
        "clickgui_screen_bg",
        "hud_header_bg",
        "hud_cell_bg",
        "editor_bg"
    );
    private static final Map<String, Class<?>> EVENT_CLASSES = new LinkedHashMap<>();

    private static void register(String string, Class<?> clazz) {
        EVENT_CLASSES.put(string, clazz);
    }

    private ScriptEvents() {
    }

    public static Map<String, Class<?>> getEventClasses() {
        return Collections.unmodifiableMap(EVENT_CLASSES);
    }

    public static boolean isKnownEvent(String string) {
        return EVENT_CLASSES.containsKey(string) || CUSTOM_EVENTS.contains(string);
    }

    static {
        register("tick", WorldTickEvent.class);
        register("update", TickEvent.class);
        register("pre_update", PreRotationEvent.class);
        register("post_motion", PostMotionEvent.class);
        register("player_tick", PlayerTickEvent.class);
        register("game_update", HudPostRenderEvent.class);
        register("slow", SlowdownEvent.class);
        register("move", MoveEvent.class);
        register("move_input", MoveInputEvent.class);
        register("move_sync", SendMovementPacketsEvent.class);
        register("player_input", PlayerInputPacketEvent.class);
        register("jump", JumpEvent.class);
        register("forced_sprint", SprintEvent.class);
        register("keep_sprint", SprintResetEvent.class);
        register("slime_bounce", SlimeBounceEvent.class);
        register("firework_boost", FireworkBoostEvent.class);
        register("bounding_box", EntityBoundingBoxEvent.class);
        register("look", MouseLookEvent.class);
        register("rotation", CameraRotationEvent.class);
        register("attack", AttackEntityEvent.class);
        register("entity_trace", EntityRaycastEvent.class);
        register("entity_hitbox", TargetingMarginEvent.class);
        register("death", DeathScreenEvent.class);
        register("click_slot", SlotClickEvent.class);
        register("use_item", UseItemEvent.class);
        register("stop_use", StopUsingItemEvent.class);
        register("entity_use_item", UsingItemChangeEvent.class);
        register("item_pickup", ItemPickupEvent.class);
        register("packet", PacketEvent.class);
        register("world_change", GameJoinEvent.class);
        register("block_update", BlockUpdateEvent.class);
        register("block_collision", BlockCollisionEvent.class);
        register("entity_remove", EntityRemoveEvent.class);
        register("client_options", SyncedOptionsEvent.class);
        register("screen", HandledScreenRenderEvent.class);
        register("screen_close", CloseScreenEvent.class);
        register("set_screen", SetScreenEvent.class);
        register("screen_tooltip", SlotTooltipEvent.class);
        register("key", ScreenMouseClickEvent.class);
        register("mouse", ScreenKeyEvent.class);
        register("scroll", ScreenMouseScrollEvent.class);
        register("render", RenderEvent.class);
        register("render2d", Render2DEvent.class);
        register("render3d", Render3DEvent.class);
        register("text_factory", TextEvent.class);
        register("custom_sky", SkyRenderEvent.class);
        register("ambience_cloud", CloudRenderEvent.class);
        register("ambience_fog_color", FogColorEvent.class);
        register("ambience_fog_distance", FogDistanceEvent.class);
        register("ambience_sky_color", SkyColorEvent.class);
        register("ambience_time", WorldTimeEvent.class);
        register("ambience_weather", WeatherEvent.class);
    }
}
