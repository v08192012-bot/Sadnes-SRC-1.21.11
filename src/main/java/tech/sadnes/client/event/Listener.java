package tech.sadnes.client.event;

public interface Listener {
    void invoke(Object object);

    Class<?> getEventType();

    int getPriority();

    boolean isStatic();
}
