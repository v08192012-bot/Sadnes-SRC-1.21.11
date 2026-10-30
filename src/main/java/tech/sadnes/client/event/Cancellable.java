package tech.sadnes.client.event;

public interface Cancellable {
    void setCancelled(boolean flag);

    boolean isCancelled();

    default void cancel() {
        this.setCancelled(true);
    }
}
