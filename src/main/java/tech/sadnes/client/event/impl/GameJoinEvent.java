package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.Event;

public class GameJoinEvent extends Event {
    private static final GameJoinEvent INSTANCE = new GameJoinEvent();

    public static GameJoinEvent getInstance() {
        return INSTANCE;
    }
}
