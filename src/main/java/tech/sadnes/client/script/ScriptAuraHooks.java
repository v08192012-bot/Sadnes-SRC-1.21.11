package tech.sadnes.client.script;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.component.RotationComponent;
import tech.sadnes.client.feature.impl.combat.AttackAura;
import tech.sadnes.client.feature.impl.combat.aura.AttackTimer;
import tech.sadnes.client.feature.impl.hud.Watermark;
import tech.sadnes.client.script.api.ScriptPlayerApi;
import tech.sadnes.client.util.math.Rotation;
import tech.sadnes.client.util.player.CombatUtil;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.Scriptable;
import tech.sadnes.libs.rhino.Undefined;

public final class ScriptAuraHooks {
    private static final Map<String, ScriptAuraHooks.Hook> ROTATION_MODES = new LinkedHashMap<>();
    private static final Map<String, ScriptAuraHooks.Hook> ESP_MODES = new LinkedHashMap<>();
    private static final List<ScriptAuraHooks.Hook> RANGE_HOOKS = new ArrayList<>();
    private static final List<ScriptAuraHooks.Hook> HIT_MOMENT_HOOKS = new ArrayList<>();
    private static boolean inRangeHook;
    private static Script targetScript;
    private static LivingEntity scriptTarget;
    private static int targetSetTick;

    private ScriptAuraHooks() {
    }

    public static void setTarget(Script script, LivingEntity livingEntity) {
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
        targetScript = livingEntity != null && clientPlayerEntity != null ? script : null;
        scriptTarget = targetScript == null ? null : livingEntity;
        if (clientPlayerEntity == null) {
            targetSetTick = 0;
        } else {
            targetSetTick = clientPlayerEntity.age;
        }
    }

    public static LivingEntity getTarget() {
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
        if (clientPlayerEntity != null && scriptTarget != null && targetScript.isActive() && scriptTarget.isAlive()) {
            int i = clientPlayerEntity.age - targetSetTick;
            return i >= 0 && i <= 2 ? scriptTarget : null;
        } else {
            return null;
        }
    }

    public static void registerRotation(Script script, String string, Function function) {
        if (string != null && !string.isEmpty() && function != null) {
            ROTATION_MODES.put(string, new ScriptAuraHooks.Hook(script, function));
            refreshAura();
        } else {
            throw Context.reportRuntimeError("aura.rotation(name, fn)");
        }
    }

    public static void registerEsp(Script script, String string, Function function) {
        if (string != null && !string.isEmpty() && function != null) {
            ESP_MODES.put(string, new ScriptAuraHooks.Hook(script, function));
            refreshAura();
        } else {
            throw Context.reportRuntimeError("aura.esp(name, fn)");
        }
    }

    public static void addRangeHook(Script script, Function function) {
        if (function == null) {
            throw Context.reportRuntimeError("aura.rangeHook(fn)");
        }

        RANGE_HOOKS.add(new ScriptAuraHooks.Hook(script, function));
    }

    public static void addHitMomentHook(Script script, Function function) {
        if (function == null) {
            throw Context.reportRuntimeError("aura.hitMomentHook(fn)");
        }

        HIT_MOMENT_HOOKS.add(new ScriptAuraHooks.Hook(script, function));
    }

    public static void unregisterAll(Script script2) {
        if (targetScript == script2) {
            setTarget(script2, null);
        }

        RANGE_HOOKS.removeIf(hook -> hook.script() == script2);
        HIT_MOMENT_HOOKS.removeIf(hook2 -> hook2.script() == script2);
        boolean flag = ROTATION_MODES.values().removeIf(hook3 -> hook3.script() == script2);
        boolean flag2 = flag | ESP_MODES.values().removeIf(hook4 -> hook4.script() == script2);
        if (flag2) {
            refreshAura();
        }

        Watermark watermark = Sadnes.getInstance().getFeatureManager().getFeature(Watermark.class);
        if (watermark != null) {
            watermark.removeScriptChips(script2);
        }

        ScriptEspRegistry.unregisterAll(script2);
        ScriptTargetHudStyles.unregisterAll(script2);
    }

    public static List<String> getRotationModes() {
        return collectActiveNames(ROTATION_MODES);
    }

    public static List<String> getEspModes() {
        return collectActiveNames(ESP_MODES);
    }

    private static List<String> collectActiveNames(Map<String, ScriptAuraHooks.Hook> map) {
        ArrayList arrayList = new ArrayList();

        for (Entry entry : map.entrySet()) {
            if (((ScriptAuraHooks.Hook)entry.getValue()).script().isActive()) {
                arrayList.add((String)entry.getKey());
            }
        }

        return arrayList;
    }

    public static boolean hasRotationMode(String string) {
        ScriptAuraHooks.Hook hook = ROTATION_MODES.get(string);
        return hook != null && hook.script().isActive();
    }

    public static boolean hasEspMode(String string) {
        ScriptAuraHooks.Hook hook = ESP_MODES.get(string);
        return hook != null && hook.script().isActive();
    }

    public static boolean hasRegistrations(Script script2) {
        for (ScriptAuraHooks.Hook hook : ROTATION_MODES.values()) {
            if (hook.script() == script2) {
                return true;
            }
        }

        for (ScriptAuraHooks.Hook hook2 : ESP_MODES.values()) {
            if (hook2.script() == script2) {
                return true;
            }
        }

        return ScriptTargetHudStyles.hasStyles(script2);
    }

    public static void refreshAll() {
        refreshAura();
        ScriptEspRegistry.refresh();
        ScriptTargetHudStyles.refreshTargetHud();
    }

    private static void refreshAura() {
        AttackAura attackAura = AttackAura.getInstance();
        if (attackAura != null) {
            attackAura.refreshModes();
        }
    }

    public static void applyRotation(String string, LivingEntity livingEntity, double d) {
        ScriptAuraHooks.Hook hook = ROTATION_MODES.get(string);
        if (hook != null && hook.script().isActive() && livingEntity != null) {
            NativeObject nativeObject = ScriptValues.newObject();
            ScriptValues.putValue(nativeObject, "target", ScriptPlayerApi.wrapEntity(livingEntity));
            ScriptValues.putValue(nativeObject, "yaw", getPlayerYaw());
            ScriptValues.putValue(nativeObject, "pitch", getPlayerPitch());
            ScriptValues.putValue(nativeObject, "distance", CombatUtil.getDistance(livingEntity));
            ScriptValues.putValue(nativeObject, "range", d);
            ScriptValues.putValue(nativeObject, "cooldown", AttackTimer.getCooldownProgress());
            Object object = hook.script().call(hook.fn(), nativeObject);
            if (object instanceof Scriptable scriptable && !Undefined.isUndefined(object)) {
                float f = (float)getNumber(scriptable, "yaw", getPlayerYaw());
                float f2 = (float)getNumber(scriptable, "pitch", getPlayerPitch());
                float f3 = (float)getNumber(scriptable, "yawSpeed", 180.0);
                float f4 = (float)getNumber(scriptable, "pitchSpeed", 180.0);
                float f5 = (float)getNumber(scriptable, "returnSpeed", 12.0);
                float f6 = (float)getNumber(scriptable, "pitchReturnSpeed", f5);
                RotationComponent.rotate(new Rotation(f, Math.clamp(f2, -89.0F, 89.0F)), f3, f4, f5, f6, 1, 4, false);
            }
        }
    }

    public static void renderEsp(String string, LivingEntity livingEntity, MatrixStack matrixStack, float f, int i, float f2) {
        ScriptAuraHooks.Hook hook = ESP_MODES.get(string);
        if (hook != null && hook.script().isActive() && livingEntity != null && matrixStack != null) {
            NativeObject nativeObject = ScriptValues.newObject();
            ScriptValues.putValue(nativeObject, "target", ScriptPlayerApi.wrapEntity(livingEntity));
            ScriptValues.putValue(nativeObject, "color", i);
            ScriptValues.putValue(nativeObject, "alpha", f2);
            ScriptValues.putValue(nativeObject, "partial", f);
            ScriptValues.putValue(nativeObject, "x", lerp(f, livingEntity.lastX, livingEntity.getX()));
            ScriptValues.putValue(nativeObject, "y", lerp(f, livingEntity.lastY, livingEntity.getY()));
            ScriptValues.putValue(nativeObject, "z", lerp(f, livingEntity.lastZ, livingEntity.getZ()));
            ScriptValues.putValue(nativeObject, "width", livingEntity.getWidth());
            ScriptValues.putValue(nativeObject, "height", livingEntity.getHeight());
            ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
            if (scriptManager != null) {
                scriptManager.withMatrices(matrixStack, f, () -> hook.script().call(hook.fn(), nativeObject));
            }
        }
    }

    public static double modifyRange(String string, double d) {
        if (!RANGE_HOOKS.isEmpty() && !inRangeHook && MinecraftClient.getInstance().isOnThread()) {
            NativeObject nativeObject = ScriptValues.newObject();
            ScriptValues.putValue(nativeObject, "source", string);
            ScriptValues.putValue(nativeObject, "range", d);
            inRangeHook = true;

            try {
                for (ScriptAuraHooks.Hook hook : new ArrayList<>(RANGE_HOOKS)) {
                    if (hook.script().isActive()) {
                        Object object = hook.script().call(hook.fn(), nativeObject);
                        if (object instanceof Number) {
                            double d2 = ((Number)object).doubleValue();
                            if (!Double.isNaN(d2) && d2 > 0.0) {
                                return d2;
                            }
                        }
                    }
                }

                return d;
            } finally {
                inRangeHook = false;
            }
        } else {
            return d;
        }
    }

    public static Boolean checkHitMoment(boolean flag, boolean flag2) {
        if (!HIT_MOMENT_HOOKS.isEmpty() && MinecraftClient.getInstance().isOnThread()) {
            NativeObject nativeObject = ScriptValues.newObject();
            ScriptValues.putValue(nativeObject, "canCrit", flag);
            ScriptValues.putValue(nativeObject, "noRoomToJump", flag2);
            Boolean flag3 = null;

            for (ScriptAuraHooks.Hook hook : new ArrayList<>(HIT_MOMENT_HOOKS)) {
                if (hook.script().isActive() && hook.script().call(hook.fn(), nativeObject) instanceof Boolean flag4) {
                    if (flag4) {
                        return true;
                    }

                    flag3 = false;
                }
            }

            return flag3;
        } else {
            return null;
        }
    }

    private static double getNumber(Scriptable scriptable, String string, double d) {
        Object object = scriptable.get(string, scriptable);
        if (object != null && object != Scriptable.NOT_FOUND && !Undefined.isUndefined(object)) {
            double d2 = Context.toNumber(object);
            return Double.isNaN(d2) ? d : d2;
        } else {
            return d;
        }
    }

    private static double lerp(float f, double d, double d2) {
        return d + (d2 - d) * f;
    }

    private static float getPlayerYaw() {
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
        return clientPlayerEntity == null ? 0.0F : clientPlayerEntity.getYaw();
    }

    private static float getPlayerPitch() {
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
        return clientPlayerEntity == null ? 0.0F : clientPlayerEntity.getPitch();
    }

    record Hook(Script script, Function fn) {
    }
}
