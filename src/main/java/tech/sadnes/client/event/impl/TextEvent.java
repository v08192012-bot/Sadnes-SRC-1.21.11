package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.Event;

public class TextEvent extends Event {
    private String text;

    public void replace(String string, String string2) {
        if (this.text != null && !this.text.isEmpty() && string != null && !string.isEmpty()) {
            if (this.text.contains(string)) {
                this.text = this.text.replace(string, string2);
            }
        }
    }

    public String getText() {
        return this.text;
    }

    public void setText(String string) {
        this.text = string;
    }

    public TextEvent(String string) {
        this.text = string;
    }
}
