package tech.sadnes.client.event;

import java.util.List;

public final class ListenerSorter {
    private static final int LINEAR_SEARCH_THRESHOLD = 8;

    private ListenerSorter() {
    }

    public static Listener remove(List<Listener> listeners, Listener listener) {
        int i = listener.getPriority();

        for (int i2 = 0; i2 < listeners.size(); i2++) {
            Listener listener2 = (Listener)listeners.get(i2);
            if (listener2 == listener) {
                return (Listener)listeners.remove(i2);
            }

            if (listener2.getPriority() < i) {
                return null;
            }
        }

        return null;
    }

    public static boolean contains(List<Listener> listeners, Listener listener) {
        int i = listener.getPriority();

        for (Listener listener2 : listeners) {
            if (listener2 == listener) {
                return true;
            }

            if (listener2.getPriority() < i) {
                break;
            }
        }

        return false;
    }

    public static void insert(List<Listener> listeners, Listener listener) {
        if (listeners.size() < 8) {
            int i = linearInsertIndex(listeners, listener);
            listeners.add(i, listener);
        } else {
            int i2 = binaryInsertIndex(listeners, listener);
            listeners.add(i2, listener);
        }
    }

    private static int linearInsertIndex(List<Listener> listeners, Listener listener) {
        int i = listeners.size();

        for (int i2 = 0; i2 < i; i2++) {
            if (listener.getPriority() > ((Listener)listeners.get(i2)).getPriority()) {
                return i2;
            }
        }

        return i;
    }

    private static int binaryInsertIndex(List<Listener> listeners, Listener listener) {
        int i = 0;
        int i2 = listeners.size() - 1;

        while (i <= i2) {
            int i3 = i + (i2 - i) / 2;
            Listener listener2 = (Listener)listeners.get(i3);
            int i4 = Integer.compare(listener.getPriority(), listener2.getPriority());
            if (i4 == 0) {
                return i3;
            }

            if (i4 < 0) {
                i = i3 + 1;
            } else {
                i2 = i3 - 1;
            }
        }

        return i;
    }
}
