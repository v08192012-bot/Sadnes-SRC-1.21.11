package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.CancellableEvent;

public final class SprintResetEvent extends CancellableEvent {
    private final SprintResetEvent.Reason reason;

    public SprintResetEvent.Reason getReason() {
        return this.reason;
    }

    public SprintResetEvent(SprintResetEvent.Reason reason2) {
        this.reason = reason2;
    }

    public enum Reason {
        ATTACK,
        HUNGER,
        LEGACY;
    }
}
