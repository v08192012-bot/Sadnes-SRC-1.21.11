package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.Event;

public final class PostMotionEvent extends Event {
    private static final PostMotionEvent INSTANCE = new PostMotionEvent();

    public static PostMotionEvent getInstance() {
        return INSTANCE;
    }
}
