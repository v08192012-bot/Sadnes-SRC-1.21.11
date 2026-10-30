package tech.sadnes.client.event;

public class CancellableEvent extends Event implements Cancellable {
    private boolean cancelled;

    @Override
    public void setCancelled(boolean flag) {
        this.cancelled = flag;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }
}
