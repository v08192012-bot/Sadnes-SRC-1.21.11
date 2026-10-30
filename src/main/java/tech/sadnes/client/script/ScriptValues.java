package tech.sadnes.client.script;

import java.util.List;
import tech.sadnes.libs.rhino.BaseFunction;
import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.Function;
import tech.sadnes.libs.rhino.NativeArray;
import tech.sadnes.libs.rhino.NativeJSON;
import tech.sadnes.libs.rhino.NativeJavaPackage;
import tech.sadnes.libs.rhino.NativeObject;
import tech.sadnes.libs.rhino.RhinoException;
import tech.sadnes.libs.rhino.ScriptRuntime;
import tech.sadnes.libs.rhino.Scriptable;
import tech.sadnes.libs.rhino.ScriptableObject;
import tech.sadnes.libs.rhino.Undefined;
import tech.sadnes.libs.rhino.Wrapper;
import tech.sadnes.libs.rhino.TopLevel.Builtins;

public final class ScriptValues {
    private ScriptValues() {
    }

    public static BaseFunction function(final String string, final ScriptValues.HostFunction hostFunction) {
        BaseFunction baseFunction = new BaseFunction() {
            @Override
            public Object call(Context context, Scriptable scriptable, Scriptable scriptable2, Object[] objects) {
                try {
                    return ScriptValues.toScript(hostFunction.call(objects));
                } catch (RhinoException rhinoException) {
                    throw rhinoException;
                } catch (RuntimeException exception) {
                    throw Context.reportRuntimeError(string + ": " + exception.getMessage());
                }
            }

            @Override
            public String getFunctionName() {
                return string;
            }
        };
        ScriptRuntime.setFunctionProtoAndParent(baseFunction, ScriptRuntimeSandbox.sharedScope());
        return baseFunction;
    }

    public static NativeObject newObject() {
        NativeObject nativeObject = new NativeObject();
        ScriptRuntime.setBuiltinProtoAndParent(nativeObject, ScriptRuntimeSandbox.sharedScope(), Builtins.Object);
        return nativeObject;
    }

    public static void putValue(NativeObject nativeObject, String string, Object object) {
        nativeObject.put(string, nativeObject, toScript(object));
    }

    public static void putFunction(NativeObject nativeObject, String string, ScriptValues.HostFunction hostFunction) {
        nativeObject.put(string, nativeObject, function(string, hostFunction));
    }

    public static Object toScript(Object object) {
        if (object == null) {
            return Undefined.instance;
        } else if (object instanceof Wrapper || object instanceof NativeJavaPackage) {
            throw Context.reportRuntimeError("Java objects are disabled in scripts");
        } else if (object instanceof Scriptable || object instanceof Undefined || object instanceof Boolean) {
            return object;
        } else if (object instanceof CharSequence) {
            return ((CharSequence)object).toString();
        } else if (object instanceof Character) {
            return ((Character)object).toString();
        } else if (object instanceof Number) {
            return ((Number)object).doubleValue();
        } else {
            throw Context.reportRuntimeError("Unsupported host value in script API");
        }
    }

    public static NativeArray toArray(List<?> list) {
        int i = list.size();
        Object[] objects = new Object[i];

        for (int i2 = 0; i2 < i; i2++) {
            objects[i2] = toScript(list.get(i2));
        }

        return toArray(objects);
    }

    public static NativeArray toArray(Object[] objects) {
        Object[] objects2 = (Object[])objects.clone();

        for (int i = 0; i < objects2.length; i++) {
            objects2[i] = toScript(objects2[i]);
        }

        NativeArray nativeArray = new NativeArray(objects2);
        ScriptRuntime.setBuiltinProtoAndParent(nativeArray, ScriptRuntimeSandbox.sharedScope(), Builtins.Array);
        return nativeArray;
    }

    public static boolean hasArg(Object[] objects, int i) {
        return i < objects.length && objects[i] != null && !Undefined.isUndefined(objects[i]);
    }

    public static double argDouble(Object[] objects, int i, double d) {
        return hasArg(objects, i) ? Context.toNumber(objects[i]) : d;
    }

    public static float argFloat(Object[] objects, int i, float f) {
        return (float)argDouble(objects, i, f);
    }

    public static float argFloat(Object[] objects, int i) {
        return argFloat(objects, i, 0.0F);
    }

    public static int argInt(Object[] objects, int i, int i2) {
        return hasArg(objects, i) ? (int)((long)Context.toNumber(objects[i])) : i2;
    }

    public static int argIntOrDefault(Object[] objects, int i, int i2) {
        return hasArg(objects, i) ? (int)((long)Context.toNumber(objects[i])) : i2;
    }

    public static boolean argBoolean(Object[] objects, int i, boolean flag) {
        return hasArg(objects, i) ? Context.toBoolean(objects[i]) : flag;
    }

    public static String argString(Object[] objects, int i, String string) {
        return hasArg(objects, i) ? Context.toString(objects[i]) : string;
    }

    public static String argString(Object[] objects, int i) {
        return argString(objects, i, "");
    }

    public static Function argFunction(Object[] objects, int i) {
        if (hasArg(objects, i)) {
            Object object = objects[i];
            if (object instanceof Function) {
                return (Function)object;
            }
        }

        return null;
    }

    public static String stringify(Object object) {
        if (object instanceof Scriptable scriptable && !(scriptable instanceof Function) && ("Object".equals(scriptable.getClassName()) || "Array".equals(scriptable.getClassName()))) {
            try {
                Object object2 = NativeJSON.stringify(Context.getCurrentContext(), ScriptRuntimeSandbox.sharedScope(), scriptable, null, null);
                if (object2 instanceof CharSequence) {
                    return ((CharSequence)object2).toString();
                }
            } catch (RuntimeException exception) {
            }
        }

        return Context.toString(object);
    }

    public static Object getProperty(Object object, String string) {
        if (object instanceof Scriptable scriptable) {
            Object object2 = ScriptableObject.getProperty(scriptable, string);
            return object2 != Scriptable.NOT_FOUND && !Undefined.isUndefined(object2) ? object2 : null;
        } else {
            return null;
        }
    }

    public static double getNumber(Object object, String string, double d) {
        Object object2 = getProperty(object, string);
        if (object2 == null) {
            return d;
        }

        double d2 = Context.toNumber(object2);
        return Double.isNaN(d2) ? d : d2;
    }

    public static String[] argStringArray(Object[] objects, int i) {
        if (hasArg(objects, i) && objects[i] instanceof NativeArray nativeArray) {
            int i2 = (int)nativeArray.getLength();
            String[] strings = new String[i2];

            for (int i3 = 0; i3 < i2; i3++) {
                strings[i3] = Context.toString(nativeArray.get(i3, nativeArray));
            }

            return strings;
        } else {
            return new String[0];
        }
    }

    @FunctionalInterface
    public interface HostFunction {
        Object call(Object[] objects);
    }
}
