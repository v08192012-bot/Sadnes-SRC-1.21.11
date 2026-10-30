package tech.sadnes.client.script.api;

import java.util.ArrayList;
import java.util.Locale;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.feature.impl.hud.Watermark;
import tech.sadnes.client.render.texture.Icon;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.client.ui.hud.ScriptWatermarkChip;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.Scriptable;
import tech.sadnes.libs.rhino.ScriptableObject;
import tech.sadnes.libs.rhino.Undefined;

public final class ScriptWatermarkApi {
    private ScriptWatermarkApi() {
    }

    public static void install(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "icons", objects -> {
            ArrayList arrayList = new ArrayList();

            for (Icon icon : Icon.values()) {
                arrayList.add(icon.name().toLowerCase(Locale.ROOT));
            }

            return ScriptValues.toArray(arrayList);
        });
        ScriptValues.putFunction(nativeObject, "chip", objects2 -> {
            String string = ScriptValues.argString(objects2, 0);
            if (ScriptValues.hasArg(objects2, 1)) {
                Object object = objects2[1];
                if (object instanceof Scriptable) {
                    return createChip(script, string, (Scriptable)object);
                }
            }

            return createChip(script, string, null);
        });
        scriptableObject.put("watermark", scriptableObject, nativeObject);
    }

    private static NativeObject createChip(Script script, String string, Scriptable scriptable) {
        String string2 = string == null ? "" : string.replace(',', ' ').trim();
        if (string2.isEmpty()) {
            throw Context.reportRuntimeError("watermark.chip(name, {icon, value, unit, badness})");
        }

        Object object = getProperty(scriptable, "enabled");
        ScriptWatermarkChip scriptWatermarkChip = getWatermark().addScriptChip(script, string2, Icon.INFO_CIRCLE, !(object instanceof Boolean) || (Boolean)object);
        applyIcon(scriptWatermarkChip, script, getStringProperty(scriptable, "icon"));
        scriptWatermarkChip.setLabel(getProperty(scriptable, "value"));
        scriptWatermarkChip.setValueText(getProperty(scriptable, "unit"));
        Object object2 = getProperty(scriptable, "badness");
        scriptWatermarkChip.setWarningFunction(object2 instanceof Function ? (Function)object2 : null);
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "name", string2);
        ScriptValues.putFunction(nativeObject, "icon", objects -> {
            applyIcon(scriptWatermarkChip, script, ScriptValues.argString(objects, 0));
            return nativeObject;
        });
        ScriptValues.putFunction(nativeObject, "value", objects2 -> {
            if (ScriptValues.hasArg(objects2, 0)) {
                scriptWatermarkChip.setLabel(objects2[0]);
                return nativeObject;
            } else {
                scriptWatermarkChip.setLabel(null);
                return nativeObject;
            }
        });
        ScriptValues.putFunction(nativeObject, "unit", objects3 -> {
            if (ScriptValues.hasArg(objects3, 0)) {
                scriptWatermarkChip.setValueText(objects3[0]);
                return nativeObject;
            } else {
                scriptWatermarkChip.setValueText(null);
                return nativeObject;
            }
        });
        ScriptValues.putFunction(nativeObject, "badness", objects4 -> {
            scriptWatermarkChip.setWarningFunction(ScriptValues.argFunction(objects4, 0));
            return nativeObject;
        });
        ScriptValues.putFunction(nativeObject, "remove", objects5 -> {
            getWatermark().removeScriptChip(string2);
            return null;
        });
        return nativeObject;
    }

    private static void applyIcon(ScriptWatermarkChip scriptWatermarkChip, Script script, String string) {
        if (string != null && !string.isEmpty()) {
            if (string.toLowerCase(Locale.ROOT).endsWith(".svg")) {
                scriptWatermarkChip.setIconPath(ScriptRenderApi.resolvePath(script, string));
            } else {
                Icon icon = ScriptRenderApi.findIcon(string);
                if (icon == null) {
                    throw Context.reportRuntimeError("watermark.chip: unknown icon '" + string + "', see watermark.icons()");
                }

                scriptWatermarkChip.setIcon(icon);
            }
        }
    }

    private static Watermark getWatermark() {
        Watermark watermark = Sadnes.getInstance().getFeatureManager().getFeature(Watermark.class);
        if (watermark == null) {
            throw Context.reportRuntimeError("watermark is not available");
        } else {
            return watermark;
        }
    }

    private static Object getProperty(Scriptable scriptable, String string) {
        if (scriptable == null) {
            return null;
        }

        Object object = ScriptableObject.getProperty(scriptable, string);
        return object != Scriptable.NOT_FOUND && !Undefined.isUndefined(object) ? object : null;
    }

    private static String getStringProperty(Scriptable scriptable, String string) {
        Object object = getProperty(scriptable, string);
        return object == null ? null : Context.toString(object);
    }
}
