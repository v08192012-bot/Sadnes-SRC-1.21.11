package tech.sadnes.client.script;

import tech.sadnes.libs.rhino.Context;
import tech.sadnes.libs.rhino.ContextFactory;
import tech.sadnes.libs.rhino.EvaluatorException;
import tech.sadnes.libs.rhino.Scriptable;
import tech.sadnes.libs.rhino.ScriptableObject;
import tech.sadnes.libs.rhino.WrapFactory;

public final class ScriptRuntimeSandbox {
    private static final long TIME_BUDGET_NS = 250000000L;
    static final Object DEADLINE_KEY = new Object();
    private static final Object DEPTH_KEY = new Object();
    static final WrapFactory WRAP_FACTORY = new WrapFactory() {
        @Override
        public Object wrap(Context context, Scriptable scriptable, Object object, Class<?> clazz) {
            return ScriptValues.toScript(object);
        }

        @Override
        public Scriptable wrapAsJavaObject(Context context2, Scriptable scriptable2, Object object2, Class<?> clazz2) {
            throw Context.reportRuntimeError("Java access is disabled in scripts");
        }

        @Override
        public Scriptable wrapNewObject(Context context3, Scriptable scriptable3, Object object3) {
            throw Context.reportRuntimeError("Java access is disabled in scripts");
        }

        @Override
        public Scriptable wrapJavaClass(Context context4, Scriptable scriptable4, Class<?> clazz3) {
            throw Context.reportRuntimeError("Java access is disabled in scripts");
        }
    };
    static final ContextFactory CONTEXT_FACTORY = new ContextFactory() {
        @Override
        public Context makeContext() {
            Context context = super.makeContext();
            context.setLanguageVersion(200);
            context.setInterpretedMode(true);
            context.setInstructionObserverThreshold(100000);
            context.setClassShutter(string -> false);
            context.setWrapFactory(ScriptRuntimeSandbox.WRAP_FACTORY);
            return context;
        }

        @Override
        public void observeInstructionCount(Context context2, int i) {
            if (context2.getThreadLocal(ScriptRuntimeSandbox.DEADLINE_KEY) instanceof Long longValue && System.nanoTime() > longValue) {
                throw new ScriptRuntimeSandbox.ScriptTimeoutError();
            }
        }
    };

    private ScriptRuntimeSandbox() {
    }

    public static Context enter() {
        Context context = CONTEXT_FACTORY.enterContext();
        if (context.getFactory() != CONTEXT_FACTORY) {
            Context.exit();
            throw new IllegalStateException("cannot run a script in a foreign Rhino context");
        }

        Object object = context.getThreadLocal(DEPTH_KEY);
        int i = object instanceof Integer ? (Integer)object : 0;
        if (i == 0) {
            context.putThreadLocal(DEADLINE_KEY, System.nanoTime() + 250000000L);
        }

        context.putThreadLocal(DEPTH_KEY, i + 1);
        return context;
    }

    public static void exit() {
        Context context = Context.getCurrentContext();
        int i = (Integer)context.getThreadLocal(DEPTH_KEY) - 1;
        if (i == 0) {
            context.removeThreadLocal(DEADLINE_KEY);
            context.removeThreadLocal(DEPTH_KEY);
            Context.exit();
        } else {
            context.putThreadLocal(DEPTH_KEY, i);
            Context.exit();
        }
    }

    public static ScriptableObject sharedScope() {
        return ScriptRuntimeSandbox.SharedScopeHolder.SCOPE;
    }

    public static ScriptableObject newScope(Context context) {
        return context.initSafeStandardObjects();
    }

    public static ScriptRuntimeSandbox.SyntaxError checkSyntax(String string, String string2) {
        Context context = enter();

        try {
            context.compileString(string, string2, 1, null);
        } catch (EvaluatorException evaluatorException) {
            return new ScriptRuntimeSandbox.SyntaxError(evaluatorException.details(), Math.max(1, evaluatorException.lineNumber()), Math.max(0, evaluatorException.columnNumber()));
        } catch (Exception exception) {
            return new ScriptRuntimeSandbox.SyntaxError(String.valueOf(exception.getMessage()), 1, 0);
        } finally {
            exit();
        }

        return null;
    }

    public static final class ScriptTimeoutError extends Error {
        ScriptTimeoutError() {
            super("script exceeded time budget (endless loop?)");
        }
    }

    static final class SharedScopeHolder {
        static final ScriptableObject SCOPE = createScope();

        private SharedScopeHolder() {
        }

        private static ScriptableObject createScope() {
            Context context = ScriptRuntimeSandbox.CONTEXT_FACTORY.enterContext();

            try {
                return context.initSafeStandardObjects(null, true);
            } finally {
                Context.exit();
            }
        }
    }

    public record SyntaxError(String message, int line, int column) {
    }
}
