package tech.sadnes.client.script.api;

import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptTargetHudStyles;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.Scriptable;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptTargetHudApi {
    private ScriptTargetHudApi() {
    }

    public static void install(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "style", objects -> {
            String string = ScriptValues.argString(objects, 0);
            if (ScriptValues.hasArg(objects, 1)) {
                Object object = objects[1];
                if (object instanceof Scriptable) {
                    ScriptTargetHudStyles.register(script, string, (Scriptable)object);
                    return null;
                }
            }

            ScriptTargetHudStyles.register(script, string, null);
            return null;
        });
        scriptableObject.put("targethud", scriptableObject, nativeObject);
    }
}
