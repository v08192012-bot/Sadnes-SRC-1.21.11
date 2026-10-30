package tech.sadnes.client.script;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class ScriptConsole {
    public static final String LEVEL_PRINT = "print";
    public static final String LEVEL_WARN = "warn";
    public static final String LEVEL_ERROR = "error";
    public static final String LEVEL_INPUT = "input";
    public static final String LEVEL_RESULT = "result";
    private static final int MAX_ENTRIES = 1000;
    private static final ArrayDeque<ScriptConsole.Entry> ENTRIES = new ArrayDeque<>();
    private static volatile long revision;
    private static volatile long errorCount;

    private ScriptConsole() {
    }

    public static void print(String string, String string2) {
        log(string, "print", string2);
    }

    public static void error(String string, String string2) {
        log(string, "error", string2);
    }

    public static synchronized void log(String string, String string2, String string3) {
        ENTRIES.addLast(new ScriptConsole.Entry(System.currentTimeMillis(), string, string2, string3 == null ? "" : string3));
        if (ENTRIES.size() > 1000) {
            ENTRIES.removeFirst();
        }

        if ("error".equals(string2)) {
            errorCount++;
        }

        revision++;
    }

    public static synchronized void clear() {
        ENTRIES.clear();
        revision++;
    }

    public static synchronized void clearScript(String string) {
        ENTRIES.removeIf(entry -> entry.script().equals(string));
        revision++;
    }

    public static long getRevision() {
        return revision;
    }

    public static long getErrorCount() {
        return errorCount;
    }

    public static synchronized List<ScriptConsole.Entry> getEntries(String string, int i) {
        ArrayList arrayList = new ArrayList();

        for (ScriptConsole.Entry entry : ENTRIES) {
            if (string == null || entry.script().equals(string)) {
                arrayList.add(entry);
            }
        }

        return arrayList.size() > i ? new ArrayList<>(arrayList.subList(arrayList.size() - i, arrayList.size())) : arrayList;
    }

    public record Entry(long time, String script, String level, String text) {
    }
}
