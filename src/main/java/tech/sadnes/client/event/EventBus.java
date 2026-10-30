package tech.sadnes.client.event;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EventBus implements IEventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger("sadnes/EventBus");
    private final Map<Object, List<Listener>> objectListeners;
    private final Map<Class<?>, List<Listener>> staticListeners;
    private final Map<Class<?>, List<Listener>> listenersByEvent;
    private final Supplier<List<Listener>> listFactory;
    private volatile LambdaListener.LambdaFactory lambdaFactory;
    private final Map<Class<?>, Listener[]> listenerCache = new ConcurrentHashMap<>();
    private static final Listener[] EMPTY = new Listener[0];

    public EventBus(Map<Object, List<Listener>> map, Map<Class<?>, List<Listener>> map2, Map<Class<?>, List<Listener>> map3, Supplier<List<Listener>> supplier) {
        this.objectListeners = map;
        this.staticListeners = map2;
        this.listenersByEvent = map3;
        this.listFactory = supplier;
    }

    public static EventBus createConcurrent() {
        return new EventBus(
            Collections.synchronizedMap(new IdentityHashMap<>()), new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), CopyOnWriteArrayList::new
        );
    }

    public static EventBus createUnsynchronized() {
        return new EventBus(new IdentityHashMap<>(), new IdentityHashMap<>(), new IdentityHashMap<>(), CopyOnWriteArrayList::new);
    }

    @Override
    public void setLambdaFactory(LambdaListener.LambdaFactory lambdaFactory2) {
        this.lambdaFactory = lambdaFactory2;
    }

    @Override
    public boolean hasListeners(Class<?> clazz) {
        Listener[] listeners = this.listenerCache.get(clazz);
        return listeners != null && listeners.length > 0;
    }

    @Override
    public <T> T post(T value) {
        Listener[] listeners = this.listenerCache.get(value.getClass());
        if (listeners == null) {
            return (T)value;
        }

        for (Listener listener : listeners) {
            this.invokeListener(listener, value);
        }

        return (T)value;
    }

    @Override
    public <T extends Cancellable> T postCancellable(T value) {
        Listener[] listeners = this.listenerCache.get(value.getClass());
        if (listeners == null) {
            return (T)value;
        }

        value.setCancelled(false);

        for (Listener listener : listeners) {
            this.invokeListener(listener, value);
            if (value.isCancelled()) {
                return (T)value;
            }
        }

        return (T)value;
    }

    private void invokeListener(Listener listener, Object object) {
        try {
            listener.invoke(object);
        } catch (Exception exception) {
            Class clazz = listener.getEventType();
            String string = clazz instanceof Class ? clazz.getSimpleName() : String.valueOf(clazz);
            LOGGER.error("Listener {} threw {} while handling {}", string, exception.getClass().getSimpleName(), object.getClass().getSimpleName(), exception);
        }
    }

    @Override
    public void subscribe(Object object) {
        if (!this.objectListeners.containsKey(object)) {
            this.addListeners(this.getObjectListeners(object), false);
        }
    }

    @Override
    public void subscribeStatic(Class<?> clazz) {
        this.addListeners(this.getStaticListeners(clazz), true);
    }

    @Override
    public void subscribeListener(Listener listener) {
        this.addListener(listener, true);
    }

    @Override
    public void unsubscribe(Object object) {
        List list = this.objectListeners.get(object);
        if (list != null) {
            this.removeListeners(list);
            this.objectListeners.remove(object);
        }
    }

    @Override
    public void unsubscribeStatic(Class<?> clazz) {
        List list = this.staticListeners.get(clazz);
        if (list != null) {
            this.removeListeners(list);
        }
    }

    @Override
    public synchronized void unsubscribeListener(Listener listener) {
        List list = this.listenersByEvent.get(listener.getEventType());
        if (list != null) {
            ListenerSorter.remove(list, listener);
            this.rebuildCache(listener.getEventType(), list);
        }
    }

    private void addListeners(List<Listener> listeners, boolean flag) {
        for (Listener listener : listeners) {
            this.addListener(listener, flag);
        }
    }

    private synchronized void addListener(Listener listener, boolean flag) {
        List list = this.listenersByEvent.computeIfAbsent(listener.getEventType(), clazz -> this.listFactory.get());
        if (!flag || !ListenerSorter.contains(list, listener)) {
            ListenerSorter.insert(list, listener);
            this.rebuildCache(listener.getEventType(), list);
        }
    }

    private void rebuildCache(Class<?> clazz, List<Listener> listeners) {
        Map map = this.listenerCache;
        if (listeners.isEmpty()) {
            map.put(clazz, EMPTY);
        } else {
            map.put(clazz, listeners.toArray(EMPTY));
        }
    }

    private void removeListeners(List<Listener> listeners) {
        for (Listener listener : listeners) {
            this.unsubscribeListener(listener);
        }
    }

    private List<Listener> getStaticListeners(Class<?> clazz) {
        return this.staticListeners.computeIfAbsent(clazz, clazz2 -> this.collectListeners(clazz, null, true));
    }

    private List<Listener> getObjectListeners(Object object) {
        return this.objectListeners.computeIfAbsent(object, object2 -> this.collectListeners(object.getClass(), object, false));
    }

    private List<Listener> collectListeners(Class<?> clazz, Object object, boolean flag) {
        ArrayList<Listener> listeners = new ArrayList<>();
        this.scanClass(listeners, clazz, object, flag);
        return List.of(listeners.toArray(new Listener[0]));
    }

    private void scanClass(List<Listener> listeners, Class<?> clazz, Object object, boolean flag) {
        while (clazz != null) {
            LambdaListener.LambdaFactory lambdaFactory2 = null;

            for (Method method : clazz.getDeclaredMethods()) {
                if (this.isHandlerMethod(method, flag)) {
                    if (LambdaListener.hasPrivateLookup() && lambdaFactory2 == null) {
                        lambdaFactory2 = this.requireLambdaFactory(clazz);
                    }

                    listeners.add(new LambdaListener(lambdaFactory2, clazz, object, method));
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    private boolean isHandlerMethod(Method method, boolean flag) {
        if (flag && !Modifier.isStatic(method.getModifiers())) {
            return false;
        } else if (!method.isAnnotationPresent(EventHandler.class)) {
            return false;
        } else if (method.getReturnType() != void.class) {
            return false;
        } else {
            return method.getParameterCount() != 1 ? false : !method.getParameters()[0].getType().isPrimitive();
        }
    }

    private LambdaListener.LambdaFactory requireLambdaFactory(Class<?> clazz) {
        LambdaListener.LambdaFactory lambdaFactory2 = this.lambdaFactory;
        if (lambdaFactory2 == null) {
            throw new NoLambdaFactoryException(clazz);
        } else {
            return lambdaFactory2;
        }
    }
}
