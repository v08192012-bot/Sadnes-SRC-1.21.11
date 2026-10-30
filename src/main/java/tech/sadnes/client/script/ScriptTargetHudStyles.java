package tech.sadnes.client.script;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.impl.hud.TargetHud;
import tech.sadnes.client.script.api.ScriptPlayerApi;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.Scriptable;

public final class ScriptTargetHudStyles {
    private static final Map<String, ScriptTargetHudStyles.Style> STYLES = new LinkedHashMap<>();

    private ScriptTargetHudStyles() {
    }

    public static void register(Script script, String string, Scriptable scriptable) {
        float f = (float)ScriptValues.getNumber(scriptable, "width", 0.0);
        float f2 = (float)ScriptValues.getNumber(scriptable, "height", 0.0);
        if (string != null && !string.isEmpty() && f > 0.0F && f2 > 0.0F && getFunction(scriptable, "render") != null) {
            STYLES.put(string, new ScriptTargetHudStyles.Style(script, scriptable, f, f2));
            refreshTargetHud();
        } else {
            throw Context.reportRuntimeError("targethud.style(name, {width, height, render(hud)})");
        }
    }

    public static void unregisterAll(Script script2) {
        if (STYLES.values().removeIf(style -> style.script() == script2)) {
            refreshTargetHud();
        }
    }

    public static boolean hasStyles(Script script2) {
        for (ScriptTargetHudStyles.Style style : STYLES.values()) {
            if (style.script() == script2) {
                return true;
            }
        }

        return false;
    }

    public static void refreshTargetHud() {
        TargetHud targetHud = TargetHud.getInstance();
        if (targetHud != null) {
            targetHud.updateStyles();
        }
    }

    public static List<String> getStyleNames() {
        ArrayList arrayList = new ArrayList();

        for (Entry entry : STYLES.entrySet()) {
            if (((ScriptTargetHudStyles.Style)entry.getValue()).script().isActive()) {
                arrayList.add((String)entry.getKey());
            }
        }

        return arrayList;
    }

    public static boolean isAvailable(String string) {
        ScriptTargetHudStyles.Style style = STYLES.get(string);
        return style != null && style.script().isActive();
    }

    public static float getWidth(String string) {
        return STYLES.get(string).width();
    }

    public static float getHeight(String string) {
        return STYLES.get(string).height();
    }

    public static void render(String string, DrawContext drawContext, ScriptTargetHudStyles.TargetHudContext targetHudContext) {
        ScriptTargetHudStyles.Style style = STYLES.get(string);
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (style != null && style.script().isActive() && scriptManager != null) {
            NativeObject nativeObject = ScriptValues.newObject();
            ScriptValues.putValue(nativeObject, "target", targetHudContext.target() == null ? null : ScriptPlayerApi.wrapEntity(targetHudContext.target()));
            ScriptValues.putValue(nativeObject, "name", targetHudContext.name());
            ScriptValues.putValue(nativeObject, "x", targetHudContext.x());
            ScriptValues.putValue(nativeObject, "y", targetHudContext.y());
            ScriptValues.putValue(nativeObject, "width", style.width());
            ScriptValues.putValue(nativeObject, "height", style.height());
            ScriptValues.putValue(nativeObject, "health", targetHudContext.health());
            ScriptValues.putValue(nativeObject, "maxHealth", targetHudContext.maxHealth());
            ScriptValues.putValue(nativeObject, "absorption", targetHudContext.absorption());
            ScriptValues.putValue(nativeObject, "alpha", targetHudContext.alpha());
            ScriptValues.putValue(nativeObject, "showArmor", targetHudContext.showArmor());
            ScriptValues.putValue(nativeObject, "particles", targetHudContext.particles());
            ScriptValues.putFunction(nativeObject, "eatingBadge", objects -> {
                targetHudContext.badge().draw(ScriptValues.argFloat(objects, 0), ScriptValues.argFloat(objects, 1), ScriptValues.argFloat(objects, 2, targetHudContext.alpha()));
                return null;
            });
            scriptManager.withDrawContext(drawContext, 1.0F, () -> style.script().callWithThis(style.spec(), getFunction(style.spec(), "render"), nativeObject));
        }
    }

    public static void onHit(String string, LivingEntity livingEntity) {
        ScriptTargetHudStyles.Style style = STYLES.get(string);
        if (style != null && style.script().isActive()) {
            Function function = getFunction(style.spec(), "hit");
            if (function != null) {
                style.script().callWithThis(style.spec(), function, ScriptPlayerApi.wrapEntity(livingEntity));
            }
        }
    }

    private static Function getFunction(Scriptable scriptable, String string) {
        Object object = ScriptValues.getProperty(scriptable, string);
        return object instanceof Function ? (Function)object : null;
    }

    @FunctionalInterface
    public interface BadgeRenderer {
        void draw(float f, float f2, float f3);
    }

    record Style(Script script, Scriptable spec, float width, float height) {
    }

    public record TargetHudContext(
        LivingEntity target,
        String name,
        float x,
        float y,
        float health,
        float maxHealth,
        float absorption,
        float alpha,
        boolean showArmor,
        boolean particles,
        ScriptTargetHudStyles.BadgeRenderer badge
    ) {
    }
}
