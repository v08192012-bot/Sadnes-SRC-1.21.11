package tech.sadnes.client.event.impl;

import tech.sadnes.client.event.Event;

public class SlowdownEvent extends Event {
    private boolean crawlSlowdown = true;
    private boolean sneakSlowdown = true;

    public boolean isCrawlSlowdown() {
        return this.crawlSlowdown;
    }

    public void setCrawlSlowdown(boolean flag) {
        this.crawlSlowdown = flag;
    }

    public boolean isSneakSlowdown() {
        return this.sneakSlowdown;
    }

    public void setSneakSlowdown(boolean flag) {
        this.sneakSlowdown = flag;
    }
}
