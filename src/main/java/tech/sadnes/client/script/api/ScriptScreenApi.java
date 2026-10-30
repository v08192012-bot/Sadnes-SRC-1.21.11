package tech.sadnes.client.script.api;

import tech.sadnes.client.Sadnes;
import tech.sadnes.client.script.Script;
import tech.sadnes.client.script.ScriptFeature;
import tech.sadnes.client.script.ScriptPanel;
import tech.sadnes.client.script.ScriptValues;
import tech.sadnes.client.ui.clickgui.ClickGuiScreen;
import tech.sadnes.client.ui.screen.ScriptScreen;
import tech.sadnes.libs.rhino.BaseFunction;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.Scriptable;
import tech.sadnes.libs.rhino.ScriptableObject;

public final class ScriptScreenApi {
    private ScriptScreenApi() {
    }

    public static void install(ScriptableObject scriptableObject, final Script script) {
        scriptableObject.put("Screen", scriptableObject, new BaseFunction() {
            @Override
            public Object call(Context context, Scriptable scriptable, Scriptable scriptable2, Object[] objects) {
                return ScriptScreenApi.createScreen(script, objects);
            }

            @Override
            public Scriptable construct(Context context2, Scriptable scriptable3, Object[] objects2) {
                return ScriptScreenApi.createScreen(script, objects2);
            }

            @Override
            public String getFunctionName() {
                return "Screen";
            }
        });
    }

    static NativeObject createScreen(Script script, Object[] objects) {
        ScriptScreen scriptScreen = new ScriptScreen(script, new ScriptPanel(script, ScriptValues.hasArg(objects, 0) ? objects[0] : null));
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "open", objects2 -> {
            scriptScreen.open();
            return null;
        });
        ScriptValues.putFunction(nativeObject, "close", objects3 -> {
            scriptScreen.close();
            return null;
        });
        ScriptValues.putFunction(nativeObject, "toggle", objects4 -> {
            scriptScreen.toggle();
            return null;
        });
        ScriptValues.putFunction(nativeObject, "isOpen", objects5 -> scriptScreen.isActive());
        return nativeObject;
    }

    static NativeObject createPanel(Script script, ScriptFeature scriptFeature, Object object) {
        scriptFeature.setPanel(new ScriptPanel(script, object));
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putFunction(nativeObject, "open", objects -> {
            getClickGui().openHudStyle(scriptFeature);
            return null;
        });
        ScriptValues.putFunction(nativeObject, "close", objects2 -> {
            getClickGui().closeHudStyle(scriptFeature);
            return null;
        });
        ScriptValues.putFunction(nativeObject, "isOpen", objects3 -> getClickGui().isHudStyleOpen(scriptFeature));
        return nativeObject;
    }

    private static ClickGuiScreen getClickGui() {
        ClickGuiScreen clickGuiScreen = Sadnes.getInstance().getClickGui();
        if (clickGuiScreen == null) {
            throw Context.reportRuntimeError("ClickGui ещё не создан");
        } else {
            return clickGuiScreen;
        }
    }
}
