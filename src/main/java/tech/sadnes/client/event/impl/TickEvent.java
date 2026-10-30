package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.Event;

public final class TickEvent extends Event {
    private static final TickEvent INSTANCE = new TickEvent();

    public static TickEvent getInstance() {
        return INSTANCE;
    }
}
