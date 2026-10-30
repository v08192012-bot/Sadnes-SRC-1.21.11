package tech.sadnes.client.script.api;

import java.util.ArrayList;
import java.util.Locale;
import tech.sadnes.client.Sadnes;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptEvents;
import tech.sadnes.client.script.ScriptManager;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class EventsApi {
    private EventsApi() {
    }

    public static void install(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(
            nativeObject, "on", objects -> subscribe(script, ScriptValues.argString(objects, 0), ScriptValues.argFunction(objects, 1), false, parsePriority(objects, 2))
        );
        ScriptValues.putFunction(
            nativeObject, "once", objects2 -> subscribe(script, ScriptValues.argString(objects2, 0), ScriptValues.argFunction(objects2, 1), true, parsePriority(objects2, 2))
        );
        ScriptValues.putFunction(nativeObject, "off", objects3 -> script.removeListener(ScriptValues.argInt(objects3, 0, -1)));

        for (String string : ScriptEvents.getEventClasses().keySet()) {
            ScriptValues.putFunction(nativeObject, string, objects4 -> subscribe(script, string, ScriptValues.argFunction(objects4, 0), false, parsePriority(objects4, 1)));
        }

        for (String string2 : ScriptEvents.CUSTOM_EVENTS) {
            ScriptValues.putFunction(nativeObject, string2, objects5 -> subscribe(script, string2, ScriptValues.argFunction(objects5, 0), false, parsePriority(objects5, 1)));
        }

        ScriptValues.putFunction(nativeObject, "names", objects6 -> {
            ArrayList arrayList = new ArrayList<>(ScriptEvents.getEventClasses().keySet());
            arrayList.addAll(ScriptEvents.CUSTOM_EVENTS);
            return ScriptValues.toArray(arrayList);
        });
        scriptableObject.put("events", scriptableObject, nativeObject);
    }

    private static Object subscribe(Script script, String string, Function function, boolean flag, int i) {
        if (!ScriptEvents.isKnownEvent(string)) {
            throw Context.reportRuntimeError("unknown event '" + string + "'");
        }

        if (function == null) {
            throw Context.reportRuntimeError("events." + string + ": function expected");
        }

        if ((!ScriptEvents.getEventClasses().containsKey(string) || ScriptEvents.HOT_EVENTS.contains(string)) && i != 0) {
            throw Context.reportRuntimeError("events." + string + ": priority is only for client bus events");
        }

        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null) {
            scriptManager.registerBusEvent(string, i);
        }

        return script.addListener(ScriptManager.eventKey(string, i), function, flag);
    }

    private static int parsePriority(Object[] objects, int i) {
        if (!ScriptValues.hasArg(objects, i)) {
            return 0;
        }

        Object object = objects[i];
        if (object instanceof Number) {
            return ((Number)object).intValue();
        }

        String string = Context.toString(objects[i]).toLowerCase(Locale.ROOT);
        byte b = -1;
        switch (string.hashCode()) {
            case -1096862286:
                if (string.equals("lowest")) {
                    b = 5;
                }
                break;
            case -1078030475:
                if (string.equals("medium")) {
                    return 0;
                }
                break;
            case -1039745817:
                if (string.equals("normal")) {
                    return 0;
                }
                break;
            case 107348:
                if (string.equals("low")) {
                    return -250;
                }
                break;
            case 3202466:
                if (string.equals("high")) {
                    return 250;
                }
                break;
            case 915484836:
                if (string.equals("highest")) {
                    return 500;
                }
        }

        switch (b) {
            case 0:
                return 500;
            case 1:
                return 250;
            case 2:
            case 3:
                return 0;
            case 4:
                return -250;
            case 5:
                return -500;
            default:
                throw Context.reportRuntimeError("events: unknown priority '" + string + "'");
        }
    }
}
