package tech.sadnes.client.script.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import tech.sadnes.client.feature.impl.render.EspElement;
import tech.sadnes.client.feature.impl.render.esp.EspAnchor;
import tech.sadnes.client.feature.impl.render.esp.EspTargetType;
import tech.sadnes.client.feature.impl.render.esp.ScriptEspElement;
import tech.sadnes.client.feature.impl.render.esp.ScriptShaderElement;
import tech.sadnes.client.feature.impl.render.esp.ScriptedEspElement;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptEspRegistry;
import tech.sadnes.client.script.ScriptShaders;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.NativeArray;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.Scriptable;
import tech.sadnes.libs.rhino.ScriptableObject;
import tech.sadnes.libs.rhino.Undefined;

public final class ScriptEspApi {
    private ScriptEspApi() {
    }

    public static void install(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "categories", objects -> {
            ArrayList arrayList = new ArrayList();

            for (EspTargetType espTargetType : EspTargetType.values()) {
                arrayList.add(espTargetType.name().toLowerCase(Locale.ROOT));
            }

            return ScriptValues.toArray(arrayList);
        });
        ScriptValues.putFunction(nativeObject, "element", objects2 -> createElement(script, ScriptValues.argString(objects2, 0), optionsArg(objects2)));
        ScriptValues.putFunction(
            nativeObject, "chams", objects3 -> createShaderElement(script, ScriptShaders.ShaderKind.CHAMS, ScriptValues.argString(objects3, 0), optionsArg(objects3))
        );
        ScriptValues.putFunction(
            nativeObject, "glow", objects4 -> createShaderElement(script, ScriptShaders.ShaderKind.GLOW, ScriptValues.argString(objects4, 0), optionsArg(objects4))
        );
        NativeObject nativeObject2 = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject2, "segment", objects5 -> createNametagSegment(script, ScriptValues.argString(objects5, 0), optionsArg(objects5)));
        ScriptValues.putFunction(nativeObject2, "background", objects6 -> {
            ScriptEspRegistry.setNametagHook(script, ScriptValues.argFunction(objects6, 0));
            return null;
        });
        ScriptValues.putValue(nativeObject, "nametag", nativeObject2);
        scriptableObject.put("esp", scriptableObject, nativeObject);
    }

    private static NativeObject createElement(Script script, String string, Scriptable scriptable) {
        String string2 = string == null ? "" : string.trim();
        Object object = getOption(scriptable, "size");
        Object object2 = getOption(scriptable, "render");
        Function function = object2 instanceof Function ? (Function)object2 : null;
        if (!string2.isEmpty() && object != null && function != null) {
            String string3 = getStringOption(scriptable, "replaces");
            EspElement espElement = string3 != null && !string3.isBlank() ? ScriptEspRegistry.findBuiltinElement(string3) : null;
            ScriptedEspElement.Definition definition = new ScriptedEspElement.Definition(
                script,
                string2,
                getBooleanOption(scriptable, "enabled", true),
                parseAnchor(getStringOption(scriptable, "anchor")),
                parseIcon(getStringOption(scriptable, "icon")),
                object,
                function,
                espElement == null ? null : espElement.getDisplayName(),
                espElement != null && espElement.usesItems()
            );
            List list = ScriptEspRegistry.registerElements(
                script, string2, parseCategories(scriptable, EnumSet.allOf(EspTargetType.class)), espTargetType -> new ScriptedEspElement(definition, espTargetType)
            );
            return wrapElementHandle(script, string2, list);
        } else {
            throw Context.reportRuntimeError("esp.element(name, {size, render[, replaces, categories, anchor, icon, enabled]})");
        }
    }

    private static NativeObject createShaderElement(Script script, ScriptShaders.ShaderKind shaderKind, String string, Scriptable scriptable) {
        String string2 = string == null ? "" : string.trim();
        String string3 = shaderKind == ScriptShaders.ShaderKind.CHAMS ? "esp.chams" : "esp.glow";
        String string4 = getStringOption(scriptable, "shader");
        if (!string2.isEmpty() && string4 != null && !string4.isBlank()) {
            Path path = ScriptRender3DApi.resolvePath(script, string4);
            if (!Files.isRegularFile(path)) {
                throw Context.reportRuntimeError(string3 + ": нет файла assets/" + string4);
            }

            String string5 = getStringOption(scriptable, "replaces");
            EspElement espElement = string5 != null && !string5.isBlank() ? ScriptEspRegistry.findBuiltinElement(string5) : null;
            Icon icon = parseIcon(getStringOption(scriptable, "icon"));
            if (icon == null) {
                icon = shaderKind == ScriptShaders.ShaderKind.CHAMS ? Icon.PALETTE : Icon.LIGHTBULB;
            }

            ScriptShaderElement.Definition definition = new ScriptShaderElement.Definition(
                script,
                string2,
                shaderKind,
                path,
                getBooleanOption(scriptable, "enabled", true),
                icon,
                espElement == null ? null : espElement.getDisplayName(),
                getOption(scriptable, "color"),
                getOption(scriptable, "params"),
                getOption(scriptable, "radius"),
                getBooleanOption(scriptable, "model", false)
            );
            List list = ScriptEspRegistry.registerElements(
                script, string2, parseCategories(scriptable, EnumSet.allOf(EspTargetType.class)), espTargetType -> new ScriptShaderElement(definition, espTargetType)
            );
            return wrapElementHandle(script, string2, list);
        } else {
            throw Context.reportRuntimeError(
                string3
                    + "(name, {shader[, color, params, "
                    + (shaderKind == ScriptShaders.ShaderKind.CHAMS ? "model" : "radius")
                    + ", replaces, categories, icon, enabled]})"
            );
        }
    }

    private static NativeObject wrapElementHandle(Script script, String string, List<? extends ScriptEspElement> list) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "name", string);
        ScriptSettingsApi.installFactories(nativeObject, script, setting2 -> ScriptEspRegistry.registerSharedSetting(script, string, list, setting2));
        ScriptValues.putFunction(nativeObject, "remove", objects -> {
            ScriptEspRegistry.unregisterElement(script, string);
            return null;
        });
        return nativeObject;
    }

    private static NativeObject createNametagSegment(Script script, String string, Scriptable scriptable) {
        String string2 = string == null ? "" : string.trim();
        Object object = getOption(scriptable, "text");
        Icon icon = parseIcon(getStringOption(scriptable, "icon"));
        Object object2 = getOption(scriptable, "width");
        Function function = object2 instanceof Function ? (Function)object2 : null;
        Object object3 = getOption(scriptable, "draw");
        Function function2 = object3 instanceof Function ? (Function)object3 : null;
        boolean flag = object != null || icon != null || function2 != null && function != null;
        if (!string2.isEmpty() && flag) {
            Set set = parseCategories(scriptable, EnumSet.complementOf(EnumSet.of(EspTargetType.ITEM)));
            ScriptEspRegistry.registerSegment(
                new ScriptEspRegistry.NametagSegment(
                    script,
                    string2,
                    set,
                    icon,
                    getOption(scriptable, "iconColor"),
                    getBooleanOption(scriptable, "iconAfter", false),
                    object,
                    getOption(scriptable, "color"),
                    function,
                    function2
                ),
                getBooleanOption(scriptable, "enabled", true)
            );
            NativeObject nativeObject = ScriptValues.newObject();
            ScriptValues.putValue(nativeObject, "id", string2);
            ScriptValues.putFunction(nativeObject, "remove", objects -> {
                ScriptEspRegistry.unregisterSegment(script, string2);
                return null;
            });
            return nativeObject;
        } else {
            throw Context.reportRuntimeError("esp.nametag.segment(id, {text, icon, color, iconColor, iconAfter} | {width, draw})");
        }
    }

    private static Set<EspTargetType> parseCategories(Scriptable scriptable, Set<EspTargetType> espTargetTypes) {
        Object object = getOption(scriptable, "categories");
        if (object == null) {
            return espTargetTypes;
        }

        ArrayList arrayList = new ArrayList();
        if (object instanceof NativeArray nativeArray) {
            arrayList.addAll(nativeArray);
        } else {
            arrayList.add(object);
        }

        EnumSet enumSet = EnumSet.noneOf(EspTargetType.class);

        for (Object object2 : arrayList) {
            String string = Context.toString(object2).trim();
            if (string.equalsIgnoreCase("players")) {
                for (EspTargetType espTargetType : EspTargetType.values()) {
                    if (espTargetType.isPlayerType()) {
                        enumSet.add(espTargetType);
                    }
                }
            } else {
                enumSet.add(parseCategory(string));
            }
        }

        if (enumSet.isEmpty()) {
            throw Context.reportRuntimeError("esp: пустой список categories");
        } else {
            return enumSet;
        }
    }

    private static EspTargetType parseCategory(String string) {
        for (EspTargetType espTargetType : EspTargetType.values()) {
            if (espTargetType.name().equalsIgnoreCase(string)) {
                return espTargetType;
            }
        }

        throw Context.reportRuntimeError("esp: неизвестная категория '" + string + "', см. esp.categories() или \"players\"");
    }

    private static EspAnchor parseAnchor(String string) {
        if (string != null && !string.isEmpty()) {
            String string2 = string.replace("_", "");

            for (EspAnchor espAnchor : EspAnchor.values()) {
                if (espAnchor.name().replace("_", "").equalsIgnoreCase(string2)) {
                    return espAnchor;
                }
            }

            throw Context.reportRuntimeError("esp: неизвестный anchor '" + string + "' (top, bottom, left, right, center, top_left…)");
        } else {
            return EspAnchor.TOP;
        }
    }

    private static Icon parseIcon(String string) {
        if (string != null && !string.isEmpty()) {
            Icon icon = ScriptRenderApi.findIcon(string);
            if (icon == null) {
                throw Context.reportRuntimeError("esp: неизвестная иконка '" + string + "', см. watermark.icons()");
            } else {
                return icon;
            }
        } else {
            return null;
        }
    }

    private static Scriptable optionsArg(Object[] objects) {
        if (ScriptValues.hasArg(objects, 1)) {
            Object object = objects[1];
            if (object instanceof Scriptable) {
                return (Scriptable)object;
            }
        }

        return null;
    }

    private static Object getOption(Scriptable scriptable, String string) {
        if (scriptable == null) {
            return null;
        }

        Object object = ScriptableObject.getProperty(scriptable, string);
        return object != Scriptable.NOT_FOUND && !Undefined.isUndefined(object) ? object : null;
    }

    private static String getStringOption(Scriptable scriptable, String string) {
        Object object = getOption(scriptable, string);
        return object == null ? null : Context.toString(object);
    }

    private static boolean getBooleanOption(Scriptable scriptable, String string, boolean flag) {
        Object object = getOption(scriptable, string);
        return object == null ? flag : Context.toBoolean(object);
    }
}
