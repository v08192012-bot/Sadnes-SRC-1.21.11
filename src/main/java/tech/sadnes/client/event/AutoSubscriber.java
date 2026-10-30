package tech.sadnes.client.event;

import tech.sadnes.client.Sadnes;

public abstract class AutoSubscriber {
    protected AutoSubscriber() {
        Sadnes.getEventBus().subscribe(this);
    }
}
