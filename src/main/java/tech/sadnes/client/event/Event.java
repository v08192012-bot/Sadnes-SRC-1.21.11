package tech.sadnes.client.event;

import tech.sadnes.client.Sadnes;

public class Event {
    public void post() {
        if (this instanceof Cancellable cancellable) {
            Sadnes.getEventBus().postCancellable(cancellable);
        } else {
            Sadnes.getEventBus().post(this);
        }
    }
}
