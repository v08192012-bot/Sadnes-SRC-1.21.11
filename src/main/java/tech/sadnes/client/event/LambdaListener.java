package tech.sadnes.client.event;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class LambdaListener implements Listener {
    private static final LambdaListener.LookupStrategy LOOKUP_STRATEGY;
    private static final Map<Method, MethodHandle> HANDLE_CACHE = new ConcurrentHashMap<>();
    private final Class<?> eventType;
    private final boolean staticHandler;
    private final int priority;
    private final Consumer<Object> consumer;
    public final Class<?> ownerClass;

    public LambdaListener(LambdaListener.LambdaFactory lambdaFactory, Class<?> clazz, Object object, Method method) {
        this.eventType = method.getParameters()[0].getType();
        this.staticHandler = Modifier.isStatic(method.getModifiers());
        this.priority = method.getAnnotation(EventHandler.class).priority();
        this.ownerClass = clazz;

        try {
            this.consumer = this.staticHandler
                ? (Consumer)createStaticFactory(lambdaFactory, clazz, method).invoke()
                : (Consumer)createInstanceFactory(lambdaFactory, clazz, method).invoke((Object)object);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private static MethodHandle createStaticFactory(LambdaListener.LambdaFactory lambdaFactory, Class<?> clazz, Method method) throws Throwable {
        String string = method.getName();
        Lookup lookup2 = LOOKUP_STRATEGY.lookup(lambdaFactory, clazz);
        MethodType methodType2 = MethodType.methodType(void.class, method.getParameters()[0].getType());
        MethodHandle methodHandle = lookup2.findStatic(clazz, string, methodType2);
        MethodType methodType3 = MethodType.methodType(Consumer.class);
        return LambdaMetafactory.metafactory(lookup2, "accept", methodType3, MethodType.methodType(void.class, Object.class), methodHandle, methodType2).getTarget();
    }

    private static MethodHandle createInstanceFactory(LambdaListener.LambdaFactory lambdaFactory, Class<?> clazz, Method method) throws Throwable {
        MethodHandle methodHandle = HANDLE_CACHE.get(method);
        if (methodHandle != null) {
            return methodHandle;
        }

        String string = method.getName();
        Lookup lookup2 = LOOKUP_STRATEGY.lookup(lambdaFactory, clazz);
        MethodType methodType2 = MethodType.methodType(void.class, method.getParameters()[0].getType());
        MethodHandle methodHandle2 = lookup2.findVirtual(clazz, string, methodType2);
        MethodType methodType3 = MethodType.methodType(Consumer.class, clazz);
        MethodHandle methodHandle3 = LambdaMetafactory.metafactory(lookup2, "accept", methodType3, MethodType.methodType(void.class, Object.class), methodHandle2, methodType2).getTarget();
        HANDLE_CACHE.put(method, methodHandle3);
        return methodHandle3;
    }

    @Override
    public void invoke(Object object) {
        this.consumer.accept(object);
    }

    @Override
    public Class<?> getEventType() {
        return this.eventType;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public boolean isStatic() {
        return this.staticHandler;
    }

    public static boolean hasPrivateLookup() {
        return LOOKUP_STRATEGY.privateAccess;
    }

    static {
        LambdaListener.LookupStrategy lookupStrategy = null;

        try {
            try {
                final Method method = MethodHandles.class.getDeclaredMethod("privateLookupIn", Class.class, Lookup.class);
                lookupStrategy = new LambdaListener.LookupStrategy(true) {
                    @Override
                    public Lookup lookup(LambdaListener.LambdaFactory lambdaFactory, Class<?> clazz) throws InvocationTargetException, IllegalAccessException {
                        return lambdaFactory.create(method, clazz);
                    }
                };
            } catch (Exception exception) {
                if (exception instanceof SecurityException securityException) {
                    throw securityException;
                }

                try {
                    final Constructor constructor = Lookup.class.getDeclaredConstructor(Class.class);
                    constructor.setAccessible(true);
                    lookupStrategy = new LambdaListener.LookupStrategy(false) {
                        @Override
                        public Lookup lookup(LambdaListener.LambdaFactory lambdaFactory2, Class<?> clazz2) throws InstantiationException, IllegalAccessException, InvocationTargetException {
                            return (Lookup)constructor.newInstance(clazz2);
                        }
                    };
                } catch (NoSuchMethodException noSuchMethodException) {
                }
            }
        } catch (SecurityException securityException2) {
        }

        if (lookupStrategy == null) {
            lookupStrategy = new LambdaListener.LookupStrategy(false) {
                @Override
                public Lookup lookup(LambdaListener.LambdaFactory lambdaFactory3, Class<?> clazz3) {
                    return MethodHandles.publicLookup().in(clazz3);
                }
            };
        }

        LOOKUP_STRATEGY = lookupStrategy;
    }

    public interface LambdaFactory {
        Lookup create(Method method, Class<?> clazz) throws InvocationTargetException, IllegalAccessException;
    }

    abstract static class LookupStrategy {
        public final boolean privateAccess;

        LookupStrategy(boolean flag) {
            this.privateAccess = flag;
        }

        public abstract Lookup lookup(LambdaListener.LambdaFactory lambdaFactory, Class<?> clazz) throws InvocationTargetException, IllegalAccessException, InstantiationException;
    }
}
