package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.Event;

public final class PreRotationEvent extends Event {
    private static final PreRotationEvent INSTANCE = new PreRotationEvent();

    public static PreRotationEvent getInstance() {
        return INSTANCE;
    }
}
