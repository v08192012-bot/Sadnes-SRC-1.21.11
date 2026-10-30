package tech.sadnes.client.script.api;

import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptBindings {
    private ScriptBindings() {
    }

    public static void installAll(ScriptableObject scriptableObject, Script script) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "name", script.getDisplayName());
        ScriptValues.putValue(nativeObject, "dir", script.getAssetsRoot().toString());
        ScriptValues.putValue(nativeObject, "platform", AssetsApi.getOsName());
        scriptableObject.put("script", scriptableObject, nativeObject);
        scriptableObject.put("include", scriptableObject, ScriptValues.function("include", objects -> {
            script.include(ScriptValues.argString(objects, 0));
            return null;
        }));
        ScriptGlobalsApi.install(scriptableObject, script);
        AssetsApi.install(scriptableObject, script);
        ScriptAuraApi.install(scriptableObject, script);
        ScriptPlayerApi.install(scriptableObject, script);
        ScriptRotationApi.install(scriptableObject);
        ScriptInputApi.install(scriptableObject, script);
        ScriptContainerApi.install(scriptableObject);
        ScriptRenderApi.install(scriptableObject, script);
        ScriptRender3DApi.install(scriptableObject, script);
        EventsApi.install(scriptableObject, script);
        ScriptModuleApi.install(scriptableObject, script);
        ScriptScreenApi.install(scriptableObject, script);
        ScriptWatermarkApi.install(scriptableObject, script);
        ScriptEspApi.install(scriptableObject, script);
        ScriptTargetHudApi.install(scriptableObject, script);
        ScriptSettingsApi.install(scriptableObject, script);
    }
}
