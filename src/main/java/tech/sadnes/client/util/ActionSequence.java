package tech.sadnes.client.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import tech.sadnes.client.util.function.Action;
import tech.sadnes.client.util.math.StopWatch;

public class ActionSequence {
    private final StopWatch timer = new StopWatch();
    private final List<ActionSequence.DelayedAction> delayedActions = new CopyOnWriteArrayList<>();
    private final List<ActionSequence.TickAction> tickActions = new CopyOnWriteArrayList<>();
    private int delayedIndex;
    private int tickIndex;
    private boolean paused;
    private ActionSequence.RepeatPolicy repeatPolicy = new ActionSequence.RepeatTimes(1);

    public ActionSequence() {
        this.clear();
    }

    public ActionSequence addDelayed(int i, Action action) {
        return this.addDelayed(i, action, () -> true, 0);
    }

    public ActionSequence addDelayed(int i, Action action, BooleanSupplier booleanSupplier) {
        return this.addDelayed(i, action, booleanSupplier, 0);
    }

    public ActionSequence addDelayed(int i, Action action, int i2) {
        return this.addDelayed(i, action, () -> true, i2);
    }

    public ActionSequence addDelayed(int i, Action action, BooleanSupplier booleanSupplier, int i2) {
        this.delayedActions.add(new ActionSequence.DelayedAction(i, action, booleanSupplier, i2));
        this.delayedActions.sort(Comparator.reverseOrder());
        return this;
    }

    public ActionSequence addTicked(int i, Action action) {
        return this.addTicked(i, action, () -> true, 0);
    }

    public ActionSequence addTicked(int i, Action action, BooleanSupplier booleanSupplier) {
        return this.addTicked(i, action, booleanSupplier, 0);
    }

    public ActionSequence addTicked(int i, Action action, int i2) {
        return this.addTicked(i, action, () -> true, i2);
    }

    public ActionSequence addTicked(int i, Action action, BooleanSupplier booleanSupplier, int i2) {
        this.tickActions.add(new ActionSequence.TickAction(i, action, booleanSupplier, i2));
        this.tickActions.sort(Comparator.reverseOrder());
        return this;
    }

    public void resetTimer() {
        this.timer.reset();
    }

    public void resetIndices() {
        this.delayedIndex = 0;
        this.tickIndex = 0;
    }

    public ActionSequence clear() {
        this.delayedActions.clear();
        this.tickActions.clear();
        this.resetTimer();
        this.resetIndices();
        this.paused = false;
        return this;
    }

    public void tick() {
        if ((!this.delayedActions.isEmpty() || !this.tickActions.isEmpty()) && !this.paused) {
            if (this.delayedIndex < this.delayedActions.size()) {
                ActionSequence.DelayedAction delayedAction = this.delayedActions.get(this.delayedIndex);
                if (delayedAction.condition.getAsBoolean() && this.timer.hasPassed((double)delayedAction.delay)) {
                    delayedAction.action.perform();
                    this.delayedIndex++;
                    this.resetTimer();
                    if (this.repeatPolicy.shouldRestart(this.delayedIndex, this.delayedActions.size())) {
                        this.resetIndices();
                        this.repeatPolicy.onRestart();
                    }
                }
            }

            if (this.tickIndex < this.tickActions.size()) {
                ActionSequence.TickAction tickAction = this.tickActions.get(this.tickIndex);
                if (tickAction.condition.getAsBoolean() && tickAction.ticks <= 0) {
                    tickAction.action.perform();
                    this.tickIndex++;
                    this.resetTimer();
                    if (this.repeatPolicy.shouldRestart(this.tickIndex, this.tickActions.size())) {
                        this.resetIndices();
                        this.repeatPolicy.onRestart();
                    }
                } else {
                    tickAction.ticks--;
                }
            }

            this.delayedIndex = Math.min(this.delayedIndex, this.delayedActions.size());
            this.tickIndex = Math.min(this.tickIndex, this.tickActions.size());
        }
    }

    public ActionSequence setRepeatPolicy(ActionSequence.RepeatPolicy repeatPolicy2) {
        this.repeatPolicy = repeatPolicy2;
        return this;
    }

    public boolean isFinished() {
        return this.delayedIndex >= this.delayedActions.size() && this.tickIndex >= this.tickActions.size() && !this.paused && this.repeatPolicy.isDone();
    }

    public StopWatch getTimer() {
        return this.timer;
    }

    public List<ActionSequence.DelayedAction> getDelayedActions() {
        return new ArrayList<>(this.delayedActions);
    }

    public List<ActionSequence.TickAction> getTickActions() {
        return new ArrayList<>(this.tickActions);
    }

    public int getDelayedIndex() {
        return this.delayedIndex;
    }

    public int getTickIndex() {
        return this.tickIndex;
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void setPaused(boolean flag) {
        this.paused = flag;
    }

    public ActionSequence.RepeatPolicy getRepeatPolicy() {
        return this.repeatPolicy;
    }

    public static final class DelayedAction implements Comparable<ActionSequence.DelayedAction> {
        int delay;
        Action action;
        BooleanSupplier condition;
        private int priority;

        public DelayedAction(int i, Action action2, BooleanSupplier booleanSupplier, int i2) {
            this.delay = i;
            this.action = action2;
            this.condition = booleanSupplier;
            this.priority = i2;
        }

        public int getDelay() {
            return this.delay;
        }

        public Action getAction() {
            return this.action;
        }

        public BooleanSupplier getCondition() {
            return this.condition;
        }

        public int getPriority() {
            return this.priority;
        }

        public int compareTo(ActionSequence.DelayedAction delayedAction) {
            return Integer.compare(this.priority, delayedAction.priority);
        }
    }

    public static class RepeatForever implements ActionSequence.RepeatPolicy {
        @Override
        public boolean shouldRestart(int i, int i2) {
            return i >= i2;
        }

        @Override
        public void onRestart() {
        }

        @Override
        public boolean isDone() {
            return false;
        }
    }

    public interface RepeatPolicy {
        boolean shouldRestart(int i, int i2);

        void onRestart();

        boolean isDone();
    }

    public static class RepeatTimes implements ActionSequence.RepeatPolicy {
        private final int maxRepeats;
        private int repeats;

        public RepeatTimes(int i) {
            this.maxRepeats = Math.max(0, i - 1);
        }

        @Override
        public boolean shouldRestart(int i, int i2) {
            return i >= i2 && this.repeats < this.maxRepeats;
        }

        @Override
        public void onRestart() {
            this.repeats++;
        }

        @Override
        public boolean isDone() {
            return this.repeats >= this.maxRepeats;
        }
    }

    public static final class TickAction implements Comparable<ActionSequence.TickAction> {
        int ticks;
        Action action;
        BooleanSupplier condition;
        private int priority;

        public TickAction(int i, Action action2, BooleanSupplier booleanSupplier, int i2) {
            this.ticks = i;
            this.action = action2;
            this.condition = booleanSupplier;
            this.priority = i2;
        }

        public int getTicks() {
            return this.ticks;
        }

        public Action getAction() {
            return this.action;
        }

        public BooleanSupplier getCondition() {
            return this.condition;
        }

        public int getPriority() {
            return this.priority;
        }

        public int compareTo(ActionSequence.TickAction tickAction) {
            return Integer.compare(this.priority, tickAction.priority);
        }
    }
}
