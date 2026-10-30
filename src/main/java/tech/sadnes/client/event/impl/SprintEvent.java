package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.Event;

public final class SprintEvent extends Event {
    private final SprintEvent.Stage stage;
    private boolean sprinting;
    private boolean modified;

    public SprintEvent(SprintEvent.Stage stage2, boolean flag) {
        this.stage = stage2;
        this.sprinting = flag;
    }

    public void setSprinting(boolean flag) {
        this.sprinting = flag;
        this.modified = true;
    }

    public SprintEvent.Stage getStage() {
        return this.stage;
    }

    public boolean isSprinting() {
        return this.sprinting;
    }

    public boolean isModified() {
        return this.modified;
    }

    public enum Stage {
        NOSPRINT,
        AFSPRINT;
    }
}
