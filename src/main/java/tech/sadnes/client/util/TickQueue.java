package tech.sadnes.client.util;

import java.util.ArrayList;
import java.util.List;
import tech.sadnes.client.util.math.StopWatch;

public final class TickQueue {
    private final List<TickQueue.ScheduledTask> tasks = new ArrayList<>();
    private final StopWatch timer = new StopWatch();
    private int currentTick;
    private int scheduledTick;

    public boolean isEmpty() {
        return this.tasks.isEmpty();
    }

    public boolean hasPassed(long l) {
        return this.timer.hasPassed((double)l);
    }

    public TickQueue then(Runnable runnable) {
        if (!this.tasks.isEmpty()) {
            this.scheduledTick++;
        }

        this.tasks.add(new TickQueue.ScheduledTask(this.scheduledTick, runnable));
        return this;
    }

    public TickQueue delay(int i) {
        this.scheduledTick += i;
        return this;
    }

    public TickQueue clear() {
        this.currentTick = 0;
        this.scheduledTick = 0;
        this.tasks.clear();
        return this;
    }

    public void resetIfEmpty() {
        if (this.tasks.isEmpty()) {
            this.clear();
        }
    }

    public TickQueue tick() {
        if (this.tasks.isEmpty()) {
            return this;
        }

        int i = this.currentTick;
        this.tasks.removeIf(scheduledTask -> {
            if (i < scheduledTask.tick()) {
                return false;
            }

            scheduledTask.action().run();
            return true;
        });
        this.currentTick++;
        this.timer.reset();
        return this;
    }

    record ScheduledTask(int tick, Runnable action) {
    }
}
