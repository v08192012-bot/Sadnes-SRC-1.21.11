package tech.sadnes.client.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.impl.render.Esp;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.feature.impl.render.esp.EspTargetType;
import tech.sadnes.client.feature.impl.render.esp.NametagElement;
import tech.sadnes.client.feature.impl.render.esp.ScriptEspElement;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.api.ScriptConvertUtil;
import tech.sadnes.client.script.api.ScriptPlayerApi;
import tech.sadnes.client.setting.Setting;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.NativeArray;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.Scriptable;
import tech.sadnes.libs.rhino.ScriptableObject;
import tech.sadnes.libs.rhino.Undefined;

public final class ScriptEspRegistry {
    private static final List<ScriptEspElement> ELEMENTS = new ArrayList<>();
    private static final Map<String, ScriptEspRegistry.NametagSegment> SEGMENTS = new LinkedHashMap<>();
    private static List<ScriptEspRegistry.NametagSegment> activeSegments = List.of();
    private static ScriptEspRegistry.RenderHook nametagHook;
    private static final Map<String, Boolean> SEGMENT_STATES = new HashMap<>();
    private static final Map<Entity, NativeObject> ENTITY_CACHE = new LinkedHashMap<Entity, NativeObject>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Entry<Entity, NativeObject> entry) {
            return this.size() > 256;
        }
    };

    private ScriptEspRegistry() {
    }

    public static <T extends ScriptEspElement> List<T> registerElements(Script script, String string, Set<EspTargetType> espTargetTypes, Function<EspTargetType, T> function) {
        for (EspElement espElement : requireEsp().elements) {
            if (espElement.getDisplayName().equalsIgnoreCase(string)) {
                throw Context.reportRuntimeError("esp: '" + string + "' — встроенный элемент");
            }
        }

        ELEMENTS.removeIf(scriptEspElement -> scriptEspElement.getScript() == script && scriptEspElement.getDisplayName().equals(string));

        for (ScriptEspElement scriptEspElement2 : ELEMENTS) {
            if (scriptEspElement2.getDisplayName().equalsIgnoreCase(string)) {
                throw Context.reportRuntimeError("esp: '" + string + "' уже занят скриптом " + scriptEspElement2.getScript().getDisplayName());
            }
        }

        ArrayList arrayList = new ArrayList(espTargetTypes.size());

        for (EspTargetType espTargetType : espTargetTypes) {
            ScriptEspElement scriptEspElement3 = (ScriptEspElement)function.apply(espTargetType);

            for (Setting setting : scriptEspElement3.getSettings()) {
                script.addSetting(setting);
            }

            arrayList.add(scriptEspElement3);
        }

        ELEMENTS.addAll(arrayList);
        refresh();
        return arrayList;
    }

    public static void registerSharedSetting(Script script, String string, List<? extends ScriptEspElement> list, Setting<?> setting) {
        setting.setHidden(true);
        setting.setDisplayName("esp " + string + " " + setting.getDisplayName());
        script.addSetting(setting);
        Iterator iterator2 = list.iterator();

        while (iterator2.hasNext()) {
            ((ScriptEspElement)iterator2.next()).registerSetting(setting);
        }
    }

    public static EspElement findBuiltinElement(String string) {
        String string2 = normalizeName(string);
        ArrayList arrayList = new ArrayList();

        for (EspElement espElement : requireEsp().elements) {
            if (normalizeName(espElement.getDisplayName()).equals(string2)) {
                return espElement;
            }

            if (!arrayList.contains(espElement.getDisplayName())) {
                arrayList.add(espElement.getDisplayName());
            }
        }

        throw Context.reportRuntimeError("esp.element: replaces '" + string + "' — нет такого встроенного элемента: " + String.join(", ", arrayList));
    }

    private static String normalizeName(String string) {
        return string.replace(" ", "").replace("_", "").toLowerCase(Locale.ROOT);
    }

    public static void unregisterElement(Script script, String string) {
        if (ELEMENTS.removeIf(scriptEspElement -> scriptEspElement.getScript() == script && scriptEspElement.getDisplayName().equals(string))) {
            refresh();
        }
    }

    public static void registerSegment(ScriptEspRegistry.NametagSegment nametagSegment, boolean flag) {
        String string = nametagSegment.id();
        if (NametagElement.isBuiltinElement(string)) {
            throw Context.reportRuntimeError("esp.nametag.segment: '" + string + "' — встроенный сегмент");
        }

        ScriptEspRegistry.NametagSegment nametagSegment2 = SEGMENTS.get(string);
        if (nametagSegment2 != null && nametagSegment2.script() != nametagSegment.script()) {
            throw Context.reportRuntimeError("esp.nametag.segment: '" + string + "' уже занят скриптом " + nametagSegment2.script().getDisplayName());
        }

        if (nametagSegment2 != null) {
            detachSegment(nametagSegment2);
        }

        SEGMENTS.put(string, nametagSegment);
        Esp esp = requireEsp();
        Script script2 = nametagSegment.script();

        for (EspTargetType espTargetType : nametagSegment.categories()) {
            NametagElement nametagElement = esp.getNametagElements().get(espTargetType);
            if (nametagElement != null) {
                nametagElement.addElementOption(string, SEGMENT_STATES.getOrDefault(segmentStateKey(espTargetType, string), flag), script2::isActive);
            }
        }

        refresh();
    }

    public static void unregisterSegment(Script script2, String string) {
        ScriptEspRegistry.NametagSegment nametagSegment = SEGMENTS.get(string);
        if (nametagSegment != null && nametagSegment.script() == script2) {
            detachSegment(nametagSegment);
            SEGMENTS.remove(string);
            refresh();
        }
    }

    public static void setNametagHook(Script script2, tech.sadnes.libs.rhino.Function function) {
        if (function != null) {
            nametagHook = new ScriptEspRegistry.RenderHook(script2, function);
        } else {
            if (nametagHook != null && nametagHook.script() == script2) {
                nametagHook = null;
            }
        }
    }

    public static void unregisterAll(Script script2) {
        boolean flag = ELEMENTS.removeIf(scriptEspElement -> scriptEspElement.getScript() == script2);
        Iterator iterator2 = SEGMENTS.values().iterator();

        while (iterator2.hasNext()) {
            ScriptEspRegistry.NametagSegment nametagSegment = (ScriptEspRegistry.NametagSegment)iterator2.next();
            if (nametagSegment.script() == script2) {
                detachSegment(nametagSegment);
                iterator2.remove();
                flag = true;
            }
        }

        if (nametagHook != null && nametagHook.script() == script2) {
            nametagHook = null;
        }

        if (flag) {
            refresh();
        }
    }

    public static void refresh() {
        ArrayList arrayList = new ArrayList();

        for (ScriptEspRegistry.NametagSegment nametagSegment : SEGMENTS.values()) {
            if (nametagSegment.script().isActive()) {
                arrayList.add(nametagSegment);
            }
        }

        activeSegments = List.copyOf(arrayList);
        Esp esp = Esp.getInstance();
        if (esp != null) {
            ArrayList arrayList2 = new ArrayList();

            for (ScriptEspElement scriptEspElement : ELEMENTS) {
                if (scriptEspElement.getScript().isActive()) {
                    arrayList2.add(scriptEspElement);
                }
            }

            esp.applyOverrides(arrayList2);
        }
    }

    public static List<ScriptEspRegistry.NametagSegment> getActiveSegments() {
        return activeSegments;
    }

    private static void detachSegment(ScriptEspRegistry.NametagSegment nametagSegment) {
        Esp esp = Esp.getInstance();
        if (esp != null) {
            for (EspTargetType espTargetType : nametagSegment.categories()) {
                NametagElement nametagElement = esp.getNametagElements().get(espTargetType);
                if (nametagElement != null) {
                    Boolean flag = nametagElement.removeElementOption(nametagSegment.id());
                    if (flag != null) {
                        SEGMENT_STATES.put(segmentStateKey(espTargetType, nametagSegment.id()), flag);
                    }
                }
            }
        }
    }

    private static String segmentStateKey(EspTargetType espTargetType, String string) {
        return espTargetType.name() + ":" + string;
    }

    private static Esp requireEsp() {
        Esp esp = Esp.getInstance();
        if (esp == null) {
            throw Context.reportRuntimeError("esp is not available");
        } else {
            return esp;
        }
    }

    public static NativeObject createContext(Entity entity, EspTargetType espTargetType, boolean flag) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "entity", entity == null ? null : wrapEntityCached(entity));
        ScriptValues.putValue(nativeObject, "category", espTargetType.name().toLowerCase(Locale.ROOT));
        ScriptValues.putValue(nativeObject, "preview", flag);
        return nativeObject;
    }

    public static void putBounds(NativeObject nativeObject, float f, float f2, float f3, float f4, float f5) {
        ScriptValues.putValue(nativeObject, "x", f);
        ScriptValues.putValue(nativeObject, "y", f2);
        ScriptValues.putValue(nativeObject, "w", f3);
        ScriptValues.putValue(nativeObject, "h", f4);
        ScriptValues.putValue(nativeObject, "alpha", f5);
    }

    public static void putItems(NativeObject nativeObject, List<ItemStack> itemStacks) {
        ArrayList arrayList = new ArrayList(itemStacks.size());

        for (ItemStack itemStack : itemStacks) {
            arrayList.add(ScriptConvertUtil.wrapStack(itemStack));
        }

        ScriptValues.putValue(nativeObject, "items", ScriptValues.toArray(arrayList));
    }

    public static void putBox(NativeObject nativeObject, float f, float f2, float f3, float f4) {
        NativeObject nativeObject2 = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject2, "x", f);
        ScriptValues.putValue(nativeObject2, "y", f2);
        ScriptValues.putValue(nativeObject2, "w", f3);
        ScriptValues.putValue(nativeObject2, "h", f4);
        ScriptValues.putValue(nativeObject, "box", nativeObject2);
    }

    public static void invokeDraw(Script script, DrawContext drawContext, float f, tech.sadnes.libs.rhino.Function function, NativeObject nativeObject) {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null && script.isActive()) {
            scriptManager.withDrawContext(drawContext, f, () -> script.call(function, nativeObject));
        }
    }

    public static boolean renderNametagHook(
        DrawContext drawContext, Entity entity, EspTargetType espTargetType, boolean flag, float f, float f2, float f3, float f4, float f5, boolean flag2
    ) {
        ScriptEspRegistry.RenderHook renderHook = nametagHook;
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (renderHook != null && scriptManager != null && renderHook.script().isActive()) {
            NativeObject nativeObject = createContext(entity, espTargetType, flag);
            putBounds(nativeObject, f, f2, f3, f4, f5);
            ScriptValues.putValue(nativeObject, "friend", flag2);
            Object[] objects = new Object[1];
            scriptManager.withDrawContext(drawContext, f5, () -> objects[0] = renderHook.script().call(renderHook.fn(), nativeObject));
            return renderHook.script().isActive() && !Boolean.FALSE.equals(objects[0]);
        } else {
            return false;
        }
    }

    public static String resolveString(Script script, Object object, NativeObject nativeObject) {
        Object object2 = resolveValue(script, object, nativeObject);
        return object2 == null ? null : Context.toString(object2);
    }

    public static int resolveInt(Script script, Object object, NativeObject nativeObject, int i) {
        Object object2 = resolveValue(script, object, nativeObject);
        return object2 == null ? i : ScriptValues.argIntOrDefault(new Object[]{object2}, 0, i);
    }

    public static float resolveFloat(Script script, Object object, NativeObject nativeObject, float f) {
        Object object2 = resolveValue(script, object, nativeObject);
        if (object2 == null) {
            return f;
        }

        double d = Context.toNumber(object2);
        return Double.isNaN(d) ? f : (float)d;
    }

    public static void resolveFloats(Script script, Object object, NativeObject nativeObject, float[] floats) {
        Arrays.fill(floats, 0.0F);
        if (resolveValue(script, object, nativeObject) instanceof NativeArray nativeArray) {
            int i = Math.min(nativeArray.size(), floats.length);

            for (int i2 = 0; i2 < i; i2++) {
                double d = Context.toNumber(nativeArray.get(i2));
                if (!Double.isNaN(d)) {
                    floats[i2] = (float)d;
                }
            }
        }
    }

    public static float getFloatProperty(Scriptable scriptable, String string, float f) {
        Object object = ScriptableObject.getProperty(scriptable, string);
        if (object != Scriptable.NOT_FOUND && object != null && !Undefined.isUndefined(object)) {
            double d = Context.toNumber(object);
            return Double.isNaN(d) ? f : (float)d;
        } else {
            return f;
        }
    }

    private static Object resolveValue(Script script, Object object, NativeObject nativeObject) {
        Object object2 = object instanceof tech.sadnes.libs.rhino.Function function ? script.call(function, nativeObject) : object;
        return object2 != null && !Undefined.isUndefined(object2) ? object2 : null;
    }

    private static NativeObject wrapEntityCached(Entity entity) {
        return ENTITY_CACHE.computeIfAbsent(entity, ScriptPlayerApi::wrapEntity);
    }

    public record NametagSegment(
        Script script,
        String id,
        Set<EspTargetType> categories,
        Icon icon,
        Object iconColor,
        boolean iconAfter,
        Object text,
        Object color,
        tech.sadnes.libs.rhino.Function width,
        tech.sadnes.libs.rhino.Function draw
    ) {
        public boolean shows(EspTargetType espTargetType) {
            return this.categories.contains(espTargetType);
        }
    }

    record RenderHook(Script script, tech.sadnes.libs.rhino.Function fn) {
    }
}
