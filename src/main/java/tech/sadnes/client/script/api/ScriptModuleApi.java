package tech.sadnes.client.script.api;

import tech.sadnes.client.feature.CategoryDescriptor;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptCategory;
import tech.sadnes.client.script.ScriptFeature;
import tech.sadnes.client.script.ScriptModule;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.libs.rhino.BaseFunction;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.Scriptable;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptModuleApi {
    private ScriptModuleApi() {
    }

    public static void install(ScriptableObject scriptableObject, final Script script) {
        scriptableObject.put("Module", scriptableObject, new BaseFunction() {
            @Override
            public Object call(Context context, Scriptable scriptable, Scriptable scriptable2, Object[] objects) {
                return ScriptModuleApi.createModule(script, objects);
            }

            @Override
            public Scriptable construct(Context context2, Scriptable scriptable3, Object[] objects2) {
                return ScriptModuleApi.createModule(script, objects2);
            }

            @Override
            public String getFunctionName() {
                return "Module";
            }
        });
        scriptableObject.put("category", scriptableObject, ScriptValues.function("category", objects3 -> wrapCategory(ScriptValues.argString(objects3, 0, "Scripts"))));
    }

    static NativeObject createModule(Script script, Object[] objects) {
        ScriptModule scriptModule = script.getOrCreateModule(ScriptValues.argString(objects, 0, "Module"), ScriptValues.argBoolean(objects, 1, false));
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "name", scriptModule.getDisplayName());
        ScriptValues.putFunction(nativeObject, "enabled", objects2 -> scriptModule.isEnabled());
        ScriptValues.putFunction(nativeObject, "isEnabled", objects3 -> scriptModule.isEnabled());
        ScriptValues.putFunction(nativeObject, "setEnabled", objects4 -> {
            scriptModule.setEnabled(ScriptValues.argBoolean(objects4, 0, false));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "toggle", objects5 -> {
            scriptModule.toggle();
            return null;
        });
        ScriptValues.putFunction(nativeObject, "description", objects6 -> {
            scriptModule.setDescription(ScriptValues.argString(objects6, 0));
            return nativeObject;
        });
        ScriptValues.putFunction(nativeObject, "onEnabled", objects7 -> {
            scriptModule.setOnEnable(ScriptValues.argFunction(objects7, 0));
            return nativeObject;
        });
        ScriptValues.putFunction(nativeObject, "onDisabled", objects8 -> {
            scriptModule.setOnDisable(ScriptValues.argFunction(objects8, 0));
            return nativeObject;
        });
        ScriptValues.putFunction(
            nativeObject,
            "feature",
            objects9 -> wrapFeature(
                script,
                scriptModule.getOrCreateHudElement(
                    ScriptValues.argString(objects9, 0, "Feature"), ScriptValues.argString(objects9, 1, "Misc"), ScriptValues.argString(objects9, 2, "")
                )
            )
        );
        ScriptSettingsApi.installFactories(nativeObject, script, scriptModule::addSetting);
        return nativeObject;
    }

    private static NativeObject wrapFeature(Script script, ScriptFeature scriptFeature) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "name", scriptFeature.getDisplayName());
        ScriptValues.putValue(nativeObject, "category", scriptFeature.getCategory().getName());
        ScriptValues.putValue(nativeObject, "subcategory", scriptFeature.getSubcategoryName());
        ScriptValues.putFunction(nativeObject, "enabled", objects -> scriptFeature.isEnabled());
        ScriptValues.putFunction(nativeObject, "isEnabled", objects2 -> scriptFeature.isEnabled());
        ScriptValues.putFunction(nativeObject, "setEnabled", objects3 -> {
            scriptFeature.setEnabled(ScriptValues.argBoolean(objects3, 0, false));
            return null;
        });
        ScriptValues.putFunction(nativeObject, "toggle", objects4 -> {
            scriptFeature.toggle();
            return null;
        });
        ScriptValues.putFunction(nativeObject, "description", objects5 -> {
            scriptFeature.setDescriptionKey(ScriptValues.argString(objects5, 0));
            return nativeObject;
        });
        ScriptValues.putFunction(nativeObject, "onEnabled", objects6 -> {
            scriptFeature.setOnEnable(ScriptValues.argFunction(objects6, 0));
            return nativeObject;
        });
        ScriptValues.putFunction(nativeObject, "onDisabled", objects7 -> {
            scriptFeature.setOnDisable(ScriptValues.argFunction(objects7, 0));
            return nativeObject;
        });
        ScriptValues.putFunction(
            nativeObject,
            "panel",
            objects8 -> ScriptValues.hasArg(objects8, 0) ? ScriptScreenApi.createPanel(script, scriptFeature, objects8[0]) : ScriptScreenApi.createPanel(script, scriptFeature, null)
        );
        ScriptSettingsApi.installFactories(nativeObject, script, scriptFeature::addSetting);
        return nativeObject;
    }

    private static NativeObject wrapCategory(String string) {
        CategoryDescriptor categoryDescriptor = ScriptCategory.resolve(string);
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "name", categoryDescriptor.getName());
        ScriptValues.putValue(nativeObject, "custom", categoryDescriptor instanceof ScriptCategory);
        ScriptValues.putFunction(nativeObject, "icon", objects -> {
            if (categoryDescriptor instanceof ScriptCategory) {
                ((ScriptCategory)categoryDescriptor).setIcon(ScriptValues.argString(objects, 0));
            }

            return nativeObject;
        });
        ScriptValues.putFunction(nativeObject, "description", objects2 -> {
            if (categoryDescriptor instanceof ScriptCategory) {
                ((ScriptCategory)categoryDescriptor).setDescription(ScriptValues.argString(objects2, 0));
            }

            return nativeObject;
        });
        return nativeObject;
    }
}
