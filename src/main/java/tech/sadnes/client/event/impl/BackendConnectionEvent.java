package tech.sadnes.client.event.impl;

public final class BackendConnectionEvent {
    private final boolean connected;
    private final String error;

    public BackendConnectionEvent(boolean flag, String string) {
        this.connected = flag;
        this.error = string;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public String getError() {
        return this.error;
    }
}
