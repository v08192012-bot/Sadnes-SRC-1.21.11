package tech.sadnes.client.event;

public interface IEventBus {
    void setLambdaFactory(LambdaListener.LambdaFactory lambdaFactory);

    boolean hasListeners(Class<?> clazz);

    <T> T post(T value);

    <T extends Cancellable> T postCancellable(T value);

    void subscribe(Object object);

    void subscribeStatic(Class<?> clazz);

    void subscribeListener(Listener listener);

    void unsubscribe(Object object);

    void unsubscribeStatic(Class<?> clazz);

    void unsubscribeListener(Listener listener);
}
