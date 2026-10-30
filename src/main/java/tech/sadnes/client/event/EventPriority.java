package tech.sadnes.client.event;

public final class EventPriority {
    public static final int HIGHEST = 500;
    public static final int HIGH = 250;
    public static final int NORMAL = 0;
    public static final int LOW = -250;
    public static final int LOWEST = -500;

    private EventPriority() {
    }
}
