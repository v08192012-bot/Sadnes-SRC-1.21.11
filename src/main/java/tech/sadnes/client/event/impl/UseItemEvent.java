package tech.sadnes.client.event.impl;

import net.minecraft.util.Hand;
import tech.sadnes.client.event.CancellableEvent;

public final class UseItemEvent extends CancellableEvent {
    private final UseItemEvent.Stage stage;
    private final Hand hand;

    public UseItemEvent(UseItemEvent.Stage stage2) {
        this(stage2, null);
    }

    public UseItemEvent(UseItemEvent.Stage stage2, Hand hand2) {
        this.stage = stage2;
        this.hand = hand2;
    }

    public UseItemEvent.Stage getStage() {
        return this.stage;
    }

    public Hand getHand() {
        return this.hand;
    }

    public enum Stage {
        START,
        PRE,
        POST,
        ON;
    }
}
