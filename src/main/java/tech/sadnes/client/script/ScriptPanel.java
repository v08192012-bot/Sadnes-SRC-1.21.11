package tech.sadnes.client.script;

import net.minecraft.client.gui.DrawContext;
import tech.sadnes.client.Sadnes;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.Scriptable;
import tech.sadnes.libs.rhino.ScriptableObject;
import tech.sadnes.libs.rhino.Undefined;

public final class ScriptPanel {
    private final Script script;
    private final Scriptable handlers;

    public ScriptPanel(Script script2, Object object) {
        if (object instanceof Scriptable scriptable && !(object instanceof Function)) {
            this.script = script2;
            this.handlers = scriptable;
        } else {
            throw Context.reportRuntimeError("ожидается объект вида {render: function (ui) {...}, click: ...}");
        }
    }

    public Object call(String string, Object... objects) {
        return ScriptableObject.getProperty(this.handlers, string) instanceof Function function ? this.script.callWithThis(this.handlers, function, objects) : null;
    }

    public boolean callBoolean(String string, Object... objects) {
        Object object = this.call(string, objects);
        return object != null && !Undefined.isUndefined(object) && Context.toBoolean(object);
    }

    public boolean getBoolean(String string, boolean flag) {
        Object object = ScriptableObject.getProperty(this.handlers, string);
        return object != Scriptable.NOT_FOUND && !Undefined.isUndefined(object) ? Context.toBoolean(object) : flag;
    }

    public void render(DrawContext drawContext, float f, NativeObject nativeObject) {
        ScriptManager scriptManager = Sadnes.getInstance().getScriptManager();
        if (scriptManager != null) {
            scriptManager.withDrawContext(drawContext, f, () -> this.call("render", nativeObject));
        }
    }

    public static NativeObject renderArgs(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "x", f);
        ScriptValues.putValue(nativeObject, "y", f2);
        ScriptValues.putValue(nativeObject, "w", f3);
        ScriptValues.putValue(nativeObject, "h", f4);
        ScriptValues.putValue(nativeObject, "mouseX", f5);
        ScriptValues.putValue(nativeObject, "mouseY", f6);
        ScriptValues.putValue(nativeObject, "alpha", f7);
        ScriptValues.putValue(nativeObject, "partial", f8);
        return nativeObject;
    }

    public static NativeObject mouseArgs(double d, double d2, int i, double d3, double d4) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "x", d);
        ScriptValues.putValue(nativeObject, "y", d2);
        ScriptValues.putValue(nativeObject, "button", i);
        ScriptValues.putValue(nativeObject, "dx", d3);
        ScriptValues.putValue(nativeObject, "dy", d4);
        return nativeObject;
    }

    public static NativeObject scrollArgs(double d, double d2, double d3, double d4) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "x", d);
        ScriptValues.putValue(nativeObject, "y", d2);
        ScriptValues.putValue(nativeObject, "vertical", d3);
        ScriptValues.putValue(nativeObject, "horizontal", d4);
        return nativeObject;
    }

    public static NativeObject keyArgs(int i, int i2, int i3, boolean flag) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "key", i);
        ScriptValues.putValue(nativeObject, "scanCode", i2);
        ScriptValues.putValue(nativeObject, "modifiers", i3);
        ScriptValues.putValue(nativeObject, "press", flag);
        return nativeObject;
    }

    public static NativeObject charArgs(int i, int i2) {
        NativeObject nativeObject = ScriptValues.newObject();
        ScriptValues.putValue(nativeObject, "char", new String(Character.toChars(i)));
        ScriptValues.putValue(nativeObject, "code", i);
        ScriptValues.putValue(nativeObject, "modifiers", i2);
        return nativeObject;
    }
}
